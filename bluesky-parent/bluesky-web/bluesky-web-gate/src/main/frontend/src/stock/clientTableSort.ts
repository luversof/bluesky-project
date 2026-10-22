// 브라우저에서 줄을 다시 세우는 표 정렬(서버 왕복 없음): th > button[data-sort-key][data-sort-type] 를 누르면 그 열로 정렬한다.
// 값은 줄의 data-* 에서 읽는다(data-sort-key="buyAmount" → row.dataset.buyAmount). 숫자 열은 내림차순, 글자 열은 오름차순부터.
//
// 2026-10-22 까지 자산 현황(assetStatus.ts)과 배당 수익 분석(dividendHistory.ts)이 같은 정렬기를 한 벌씩 들고 있었다. 다른 것은
// 줄 선택자와 값이 같을 때 견줄 이름 키뿐이었다. 배당 쪽은 aria-sort 를 button.parentElement 에 달아, 버튼을 한 겹만 감싸도
// th 에서 정렬 상태가 사라질 수 있었다(자산 현황은 2026-09-10 에 th 로 고침) - 한 벌로 모으면서 같은 규칙이 됐다.
// 계좌 표의 펼침 상세 줄(data-detail-row-id 가 가리키는 줄)은 부모 줄과 함께 옮긴다. 상세 줄이 없는 표에서는 하는 일이 없다.
// 상세 화면의 표(tableSort.ts)는 셀 글자를 읽는 다른 방식이라 따로 둔다.
// import/export 없이 classic <script src> 로 로드(stockLayout 본문 끝 - 조각의 모듈 스크립트보다 먼저 돈다).
(function () {
	const win: any = window;
	if (win.StockClientTableSort) {
		return;
	}

	interface SortOptions {
		/** 정렬할 표(그 안의 [data-sort-key] 버튼을 묶는다). */
		tableSelector: string;
		/** 다시 세울 줄. */
		rowSelector: string;
		/** 값이 같을 때 견줄 이름 - 앞 키부터 처음 값이 있는 것. */
		tieBreakKeys: string[];
	}

	function sortValue(row: HTMLElement, sortKey: string, sortType: string): any {
		if (!row || !sortKey) {
			return sortType === "number" ? 0 : "";
		}

		const rawValue = row.dataset[sortKey];
		if (sortType === "number") {
			const parsedValue = Number(rawValue);
			return Number.isFinite(parsedValue) ? parsedValue : 0;
		}

		return rawValue || "";
	}

	function compareValues(leftValue: any, rightValue: any, sortType: string): number {
		if (sortType === "number") {
			return leftValue - rightValue;
		}

		return String(leftValue).localeCompare(String(rightValue), "ko", {
			numeric: true,
			sensitivity: "base",
		});
	}

	function updateIndicators(table: HTMLTableElement) {
		if (!table) {
			return;
		}

		const activeKey = table.dataset.sortKey;
		const direction = table.dataset.sortDirection;
		const buttons = table.querySelectorAll<HTMLElement>("[data-sort-key]");
		for (let i = 0; i < buttons.length; i++) {
			const button = buttons[i];
			const indicator = button.querySelector<HTMLElement>("[data-sort-indicator]");
			// aria-sort 는 헤더 셀(th)에 있어야 보조기술이 정렬 상태를 읽는다. parentElement 로 잡으면
			// 버튼을 레이아웃용 div 로 한 겹만 감싸도 aria-sort 가 div 로 가서 th 에서 조용히 사라진다.
			// 형제 구현(tableSort.ts)은 th 에 직접 달고 있어 둘이 어긋나 있었다.
			const headerCell = button.closest<HTMLElement>("th, td") || button.parentElement;
			const isActive = !!activeKey && button.dataset.sortKey === activeKey;

			if (indicator) {
				indicator.textContent = isActive ? (direction === "asc" ? "▲" : "▼") : "↕";
			}

			if (headerCell) {
				headerCell.setAttribute("aria-sort", isActive ? (direction === "asc" ? "ascending" : "descending") : "none");
			}
		}
	}

	function tieBreak(row: HTMLElement, keys: string[]): string {
		for (let i = 0; i < keys.length; i++) {
			const value = row.dataset[keys[i]];
			if (value) {
				return value;
			}
		}
		return "";
	}

	function sortTable(table: HTMLTableElement, sortKey: string, sortType: string, options: SortOptions) {
		if (!table || !sortKey || !sortType || !table.tBodies || table.tBodies.length === 0) {
			return;
		}

		let nextDirection: string;
		if (table.dataset.sortKey === sortKey) {
			nextDirection = table.dataset.sortDirection === "asc" ? "desc" : "asc";
		} else {
			nextDirection = sortType === "text" ? "asc" : "desc";
		}

		table.dataset.sortKey = sortKey;
		table.dataset.sortDirection = nextDirection;

		const tbody = table.tBodies[0];
		const rows = tbody.querySelectorAll<HTMLElement>(options.rowSelector);
		const groups: { row: HTMLElement; nodes: HTMLElement[] }[] = [];
		for (let i = 0; i < rows.length; i++) {
			const row = rows[i];
			const nodes: HTMLElement[] = [row];
			const detailRowId = row.dataset.detailRowId;
			if (detailRowId) {
				const detailRow = document.getElementById(detailRowId);
				if (detailRow && detailRow.parentElement === tbody) {
					nodes.push(detailRow);
				}
			}
			groups.push({ row: row, nodes: nodes });
		}

		groups.sort(function (left, right) {
			let compared = compareValues(sortValue(left.row, sortKey, sortType), sortValue(right.row, sortKey, sortType), sortType);
			if (compared === 0) {
				compared = compareValues(tieBreak(left.row, options.tieBreakKeys), tieBreak(right.row, options.tieBreakKeys), "text");
			}
			return nextDirection === "asc" ? compared : -compared;
		});

		for (let j = 0; j < groups.length; j++) {
			for (let k = 0; k < groups[j].nodes.length; k++) {
				tbody.appendChild(groups[j].nodes[k]);
			}
		}

		updateIndicators(table);
	}

	/** 문서 안 정렬 표마다 버튼을 한 번만 묶고(data-sortable-initialized) 지금 상태를 표시한다. htmx 재스왑 뒤 다시 불러도 된다. */
	function initialize(options: SortOptions) {
		const tables = document.querySelectorAll<HTMLTableElement>(options.tableSelector);
		for (let i = 0; i < tables.length; i++) {
			const table = tables[i];
			if (table.dataset.sortableInitialized === "true") {
				continue;
			}

			table.dataset.sortableInitialized = "true";

			const buttons = table.querySelectorAll<HTMLElement>("[data-sort-key]");
			for (let j = 0; j < buttons.length; j++) {
				buttons[j].addEventListener("click", function (this: HTMLElement) {
					const targetTable = this.closest<HTMLTableElement>(options.tableSelector);
					sortTable(targetTable!, this.dataset.sortKey!, this.dataset.sortType!, options);
				});
			}

			updateIndicators(table);
		}
	}

	win.StockClientTableSort = { sortValue, compareValues, updateIndicators, sortTable, initialize };
})();
