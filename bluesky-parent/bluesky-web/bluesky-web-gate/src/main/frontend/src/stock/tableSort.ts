// 클라이언트 측 표 정렬. <table data-sortable> 의 thead <th> 를 클릭하면 해당 열로 tbody 행을 정렬한다.
// 열 타입(숫자/날짜/텍스트)은 셀 내용으로 자동 감지하므로 th 마다 속성을 달 필요가 없다.
// 상세 페이지의 표(매매·배당·보유 종목)는 한 종목/계좌분이라 작아 서버 왕복 없이 즉시 정렬한다.
// 클릭 위임이라 htmx 로 나중에 삽입된 표에도 동작한다. import/export 없이 classic <script src> 로 로드.
(function () {
	// 셀에 "금액 (증감률%)" 처럼 보조 수치가 붙는 열이 있다. 괄호 뒤까지 숫자로 긁으면 두 수가
	// 이어붙어 정렬 키가 망가지고(실측: "+300,000 (+0.5%)" -> 3000000.5), 괄호 때문에 숫자 판정도
	// 실패해 문자열 정렬로 떨어진다(실측: 오름차순인데 -500,000 / +1,900,000 / +2,000,000 / +300,000).
	// 정렬 기준은 앞의 대표 수치이므로 괄호 앞만 본다.
	// 텍스트 열 정렬에 쓰는 콜레이션 로케일.
	//
	// 같은 앱의 표 정렬 구현이 셋이다 - 이 파일(계좌 상세/종목 상세)과 assetStatus.jte,
	// tabsDividendHistory.jte 의 인라인 정렬기. 뒤의 둘은 'ko' 를 명시하는데 여기만 undefined 라
	// 실행 로케일을 따랐다. 게이트는 영어 번들(gateMessage_en.properties)이 있어 이 차이가 실제로
	// 드러난다 - 실측(같은 종목명 목록):
	//   ko  -> 가나다, 삼성전자, 하이닉스, CJ씨푸드, HD현대중공업, KODEX..., RISE..., TIGER...
	//   en  -> CJ씨푸드, HD현대중공업, KODEX..., RISE..., TIGER..., 가나다, 삼성전자, 하이닉스
	// 즉 영어 로케일 브라우저에서는 자산현황 표와 계좌 상세 표의 종목명 정렬이 서로 뒤집혔다.
	// 표시 언어와 무관하게 종목명 자체가 한국어이므로, 이미 배포된 다수(2/3)와 같은 'ko' 로 맞춘다.
	var TEXT_COLLATION_LOCALE = "ko";

	function leadingNumericText(text: string): string {
		var head = text.split("(")[0];
		return head.replace(/[,\s%₩원주]/g, "");
	}

	function columnType(tbody: HTMLTableSectionElement, idx: number): string {
		var rows = tbody.rows;
		for (var i = 0; i < rows.length; i++) {
			var cell = rows[i].children[idx] as HTMLElement | undefined;
			if (!cell) continue;
			var text = (cell.textContent || "").trim();
			if (!text || text === "-") continue;
			if (/^\d{4}-\d{2}-\d{2}/.test(text)) return "date";
			var stripped = leadingNumericText(text);
			if (/^[+-]?\d+(\.\d+)?$/.test(stripped)) return "num";
			return "text";
		}
		return "text";
	}

	function cellNum(cell: Element | undefined): number {
		var t = leadingNumericText((cell ? cell.textContent : "") || "");
		var n = parseFloat(t.replace(/[^0-9.\-]/g, ""));
		return isNaN(n) ? 0 : n;
	}
	function cellText(cell: Element | undefined): string {
		return ((cell ? cell.textContent : "") || "").trim();
	}

	function sortTable(table: HTMLTableElement, th: HTMLElement) {
		var headRow = th.parentElement;
		if (!headRow) return;
		var ths = Array.prototype.slice.call(headRow.children);
		var idx = ths.indexOf(th);
		if (idx < 0) return;
		var tbody = table.tBodies[0];
		if (!tbody) return;

		var type = columnType(tbody, idx);
		var dir = th.getAttribute("data-sort-dir") === "asc" ? "desc" : "asc";

		ths.forEach(function (h: HTMLElement) {
			h.removeAttribute("data-sort-dir");
			var a = h.querySelector(".sort-arrow");
			if (a) a.textContent = "";
		});
		th.setAttribute("data-sort-dir", dir);
		ths.forEach(function (h: HTMLElement) {
			h.setAttribute("aria-sort", "none");
		});
		th.setAttribute("aria-sort", dir === "asc" ? "ascending" : "descending");
		var arrow = th.querySelector(".sort-arrow") as HTMLElement | null;
		if (!arrow) {
			arrow = document.createElement("span");
			arrow.className = "sort-arrow";
			th.appendChild(arrow);
		}
		arrow.textContent = dir === "asc" ? " ▲" : " ▼";

		var rows = Array.prototype.slice.call(tbody.rows);
		rows.sort(function (a: HTMLTableRowElement, b: HTMLTableRowElement) {
			var ca = a.children[idx];
			var cb = b.children[idx];
			var cmp: number;
			if (type === "num") {
				cmp = cellNum(ca) - cellNum(cb);
			} else {
				cmp = cellText(ca).localeCompare(cellText(cb), TEXT_COLLATION_LOCALE, {
					numeric: true,
				});
			}
			return dir === "asc" ? cmp : -cmp;
		});
		rows.forEach(function (r: HTMLTableRowElement) {
			tbody.appendChild(r);
		});
	}

	// 헤더는 마우스 전용이었다(실측: cursor 는 pointer 인데 focus() 도 안 걸리고 Tab 으로 닿지 않아
	// Enter 로 정렬할 수 없었다). 같은 화면의 다른 표(자산현황/배당)는 th 안에 <button> 을 둬서
	// 키보드로 정렬된다. 여기서는 마크업을 건드리지 않고 tabindex/aria-sort 를 입혀 맞춘다.
	// htmx 로 나중에 삽입된 표도 덮도록 swap 이후에 다시 훑는다.
	function enhanceHeaders() {
		var tables = document.querySelectorAll("table[data-sortable]");
		for (var i = 0; i < tables.length; i++) {
			var head = tables[i].querySelector("thead tr");
			if (!head) continue;
			var cells = head.children;
			for (var j = 0; j < cells.length; j++) {
				var cell = cells[j] as HTMLElement;
				if (cell.tagName !== "TH" || cell.hasAttribute("tabindex")) continue;
				cell.setAttribute("tabindex", "0");
				if (!cell.hasAttribute("aria-sort")) cell.setAttribute("aria-sort", "none");
			}
		}
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", enhanceHeaders);
	} else {
		enhanceHeaders();
	}
	document.addEventListener("htmx:afterSettle", enhanceHeaders);

	document.addEventListener("keydown", function (e) {
		var ev = e as KeyboardEvent;
		if (ev.key !== "Enter" && ev.key !== " ") return;
		var target = ev.target as HTMLElement;
		if (!target || typeof target.closest !== "function") return;
		if (target.closest("a, button, input, label, select, textarea")) return;
		var th = target.closest("th") as HTMLElement | null;
		if (!th) return;
		var table = th.closest("table") as HTMLTableElement | null;
		if (!table || !table.hasAttribute("data-sortable")) return;
		ev.preventDefault();
		sortTable(table, th);
	});

	// 테스트에서 이 규칙들을 직접 부를 수 있게 노출한다(classic script 라 export 를 쓸 수 없다).
	// 다른 주식 스크립트도 같은 방식이다(__dateRangePickerInternals 등).
	(globalThis as any).__tableSortInternals = {
		leadingNumericText: leadingNumericText,
		columnType: columnType,
		cellNum: cellNum,
		cellText: cellText,
		sortTable: sortTable,
		TEXT_COLLATION_LOCALE: TEXT_COLLATION_LOCALE,
	};

	document.addEventListener("click", function (e) {
		var target = e.target as HTMLElement;
		if (!target || typeof target.closest !== "function") return;
		// 셀 안의 링크/버튼 클릭은 정렬로 가로채지 않는다.
		if (target.closest("a, button, input, label, select")) return;
		var th = target.closest("th") as HTMLElement | null;
		if (!th) return;
		var table = th.closest("table") as HTMLTableElement | null;
		if (!table || !table.hasAttribute("data-sortable")) return;
		sortTable(table, th);
	});
})();
