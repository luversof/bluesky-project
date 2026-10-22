// 표 행 선택 합산의 공통 동작: 행 클릭 · Enter/Space 로 선택 토글(행 안 버튼 · 링크 · 입력은 제외), 선택 해제 버튼, 요약 칸에 금액 넣기.
// 합산 자체(무엇을 더하고 어떤 칸에 쓰는지)는 화면마다 다르므로 부르는 쪽 update() 에 남긴다.
//
// 2026-10-22 까지 배당 변동 요인 카드(dividendSummaryCards.jte)와 실현손익 계좌별 · 종목별 표(tradeRealizedSections.jte)의 인라인
// 스크립트가 setRow · putAmount · 행 결합 · 선택 해제를 글자까지 같게 한 벌씩 들고 있었다. 인라인 스크립트는 조각이 그려지는 자리에서
// 바로 돌므로, 이 파일은 stockLayout 본문 앞(date-range-picker.js 옆)에서 classic script 로 먼저 싣는다.
// import/export 없이 classic <script src> 로 로드(tableSort.ts 와 같다).
(function () {
	const win: any = window;
	if (win.StockRowSelection) {
		return;
	}

	const INTERACTIVE = "button, a, input, label, select, textarea";

	function setRow(row: HTMLElement | null, selected: boolean) {
		if (!row) return;
		row.classList.toggle("bg-primary/10", selected);
		row.setAttribute("aria-selected", selected ? "true" : "false");
	}

	/** summary 안 sel 칸에 원화 금액을 쓴다. signed 면 부호와 손익 색을 붙인다. */
	function putAmount(summary: ParentNode, sel: string, value: number, signed: boolean) {
		const el = summary.querySelector<HTMLElement>(sel);
		if (!el) return;
		const abs = Math.abs(Math.round(value));
		const rounded = Math.round(value) || 0;
		// 0 에는 방향이 없다 - 판정은 반올림 뒤에(StockFormatUtil.signedWon 과 같은 규칙).
		const sign = rounded > 0 ? "+" : rounded < 0 ? "-" : "";
		// 로케일은 앱이 정한다(common.js 의 appLocale) - 인자를 빼면 브라우저 로케일을 따라
		// 같은 화면 안에서 자릿수 구분이 갈린다(실측 2026-09-15).
		el.textContent = (signed ? sign : "") + "₩" + abs.toLocaleString(win.appLocale());
		if (signed) {
			el.classList.toggle("text-profit", value >= 0);
			el.classList.toggle("text-loss", value < 0);
		}
	}

	/** container 안 rowSelector 행마다 선택 토글을 묶고, clearSelector 버튼이 있으면 선택 해제를 묶는다. 바뀔 때마다 update() 를 부른다. */
	function bind(container: ParentNode, rowSelector: string, clearSelector: string, update: () => void) {
		const toggle = (row: HTMLElement) => {
			setRow(row, row.getAttribute("aria-selected") !== "true");
			update();
		};
		const rows = container.querySelectorAll<HTMLElement>(rowSelector);
		for (let i = 0; i < rows.length; i++) {
			const row = rows[i];
			row.addEventListener("click", function (event: MouseEvent) {
				const target = event.target as Element | null;
				if (target && target.closest(INTERACTIVE)) return;
				toggle(row);
			});
			row.addEventListener("keydown", function (event: KeyboardEvent) {
				const target = event.target as Element | null;
				if (target && target.closest(INTERACTIVE)) return;
				if (event.key !== "Enter" && event.key !== " ") return;
				event.preventDefault();
				toggle(row);
			});
		}
		const clearButton = container.querySelector<HTMLElement>(clearSelector);
		if (clearButton) {
			clearButton.addEventListener("click", function () {
				const selected = container.querySelectorAll<HTMLElement>(rowSelector + '[aria-selected="true"]');
				for (let i = 0; i < selected.length; i++) setRow(selected[i], false);
				update();
			});
		}
	}

	win.StockRowSelection = { setRow, putAmount, bind };
})();
