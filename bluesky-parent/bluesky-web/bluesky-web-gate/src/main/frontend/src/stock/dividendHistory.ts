// 배당 내역 조각의 브라우저 동작: 월별/도넛 차트 · 평균 카드 · 수익률 표 정렬과 행 선택 합계 · 기간 선택기.
//
// 2026-09-08 까지 tabsDividendHistory.jte 안의 인라인 <script> 였다(746 줄 - 이 모듈에서 가장 컸다). 서버가 넣어 주던 것
// (배당 데이터 배열, 기간 문자열, 라벨 9 개, 선택기 minDate)은 조각에 남은 작은 인라인 스크립트가 window.dividendHistoryConfig 로
// 건네고, 로직은 전부 여기 있다. 조각은 htmx 로 스왑되므로 그때마다 새 config 가 실리고, htmx:afterSettle 에서 run(cfg) 를
// 다시 돈다(조각 루트 #dividendListFragment 의 data-dividend-history-init 표식으로 한 DOM 에 한 번만).
//
// 검사(dividendMonthlyRange / dividendSelectionYield)는 빌드 산출물에서 이름 붙은 함수를 오려 내어 돌린다 - 그래서 함수들은
// 계속 자유 식별자(dividendData, filterStartDate …)를 닫힘(closure)으로 본다. 타입은 느슨하다(any).
export {};

declare const Chart: any;

/** 조각의 인라인 스크립트가 만드는 배당 한 줄(dividendDataJs). 금액은 숫자 리터럴로 실린다. 세전(gross)은 싣지 않는다 - 차트가 쓰지 않는다. */
interface DividendRow {
	payDate: string;
	stockItem: string;
	account: string;
	net: number;
}
/** window.dividendHistoryConfig */
interface DividendHistoryConfig {
	dividendData: DividendRow[];
	filterStartDate: string;
	filterEndDate: string;
	filterStartDay: string;
	filterEndDay: string;
	countPattern: string;
	noDataLabel: string;
	noPeriodHistoryLabel: string;
	averageNoDataLabel: string;
	averageDescTemplate: string;
	/** 딱 1 개월일 때의 문구. 없으면 averageDescTemplate 을 쓴다. */
	averageDescOneTemplate?: string;
	netAmountLabel: string;
	othersLabel: string;
	minDate: string;
	/** 달(yyyy-MM) -> 그 달을 끝으로 하는 12 개월 세후 합. 서버가 전체 원장으로 낸다. */
}
interface MonthRange {
	from: string;
	to: string;
}

(() => {
	const win: any = window;
	if (win.__dividendHistoryAttached) {
		if (typeof win.initializeDividendHistory === "function") win.initializeDividendHistory();
		return;
	}
	win.__dividendHistoryAttached = true;

	function run(cfg: DividendHistoryConfig) {
        const dividendData: DividendRow[] = cfg.dividendData || [];
        const filterStartDate: string = cfg.filterStartDate || "";
        const filterEndDate: string = cfg.filterEndDate || "";
        const filterStartDay: string = cfg.filterStartDay || "";
        const filterEndDay: string = cfg.filterEndDay || "";
        const countPattern: string = cfg.countPattern || "";
        const noDataLabel: string = cfg.noDataLabel || "";
        const noPeriodHistoryLabel: string = cfg.noPeriodHistoryLabel || "";
        const averageNoDataLabel: string = cfg.averageNoDataLabel || "";
        const averageDescTemplate: string = cfg.averageDescTemplate || "";
        const averageDescOneTemplate: string = cfg.averageDescOneTemplate || averageDescTemplate;
        const netAmountLabel: string = cfg.netAmountLabel || "";
        const othersLabel: string = cfg.othersLabel || "";

        const CHART_COLORS = [
            'rgba(99,102,241,0.8)','rgba(34,197,94,0.8)','rgba(251,191,36,0.8)',
            'rgba(239,68,68,0.8)','rgba(59,130,246,0.8)','rgba(236,72,153,0.8)',
            'rgba(14,165,233,0.8)','rgba(249,115,22,0.8)','rgba(168,85,247,0.8)',
            'rgba(20,184,166,0.8)','rgba(245,158,11,0.8)','rgba(16,185,129,0.8)'
        ];

        // stock-charts.ts 의 resolveLocale 과 같은 순서. 인라인 시절엔 브라우저 로케일(undefined)을 썼는데 앱 로케일과 어긋난다.
        function resolveLocale(): string {
            return (document.body && document.body.dataset && document.body.dataset.locale) || document.documentElement.lang || navigator.language || 'ko-KR';
        }

        // 공용 규칙(common.js 의 appLocale)을 쓴다. 아직 안 올라왔으면 같은 차례로 찾는다.
        function appLocaleOrDefault(): string {
            const shared = (globalThis as any).appLocale;
            return typeof shared === 'function' ? shared() : (document.documentElement.lang || 'ko-KR');
        }
        function formatNumber(value: number) {
            return win.StockCharts ? win.StockCharts.formatNumber(value) : new Intl.NumberFormat(resolveLocale()).format(value);
        }

        function formatCurrency(value: number) {
            return win.StockCharts ? win.StockCharts.formatCurrency(value) : ('₩' + Math.round(value).toLocaleString(appLocaleOrDefault()));
        }

        // 단수/복수 고르기는 common.ts 의 applyCountChoice(모든 화면 공용) - 없으면 문구 그대로.
        const countChoice = (pattern: string, value: number): string =>
            typeof (globalThis as any).applyCountChoice === 'function' ? (globalThis as any).applyCountChoice(pattern, value) : pattern;

        function formatCount(value: number) {
            return countChoice(countPattern, value).replace('{0}', formatNumber(value));
        }

        function formatFixedNumber(value: number, fractionDigits: number) {
            return new Intl.NumberFormat(resolveLocale(), {
                minimumFractionDigits: fractionDigits,
                maximumFractionDigits: fractionDigits
            }).format(value);
        }

        function setDividendYieldRowSelection(row: HTMLElement | null, selected: boolean) {
            if (!row) {
                return;
            }

            const selectionClass = row.dataset.selectionClass || 'dividend-yield-row-selected';
            row.classList.toggle(selectionClass, selected);
            row.setAttribute('aria-selected', selected ? 'true' : 'false');

        }

        function updateDividendYieldSelectionSummary(section: HTMLElement | null) {
            if (!section) {
                return;
            }

            const summary = section.querySelector<HTMLElement>('[data-dividend-yield-selection-summary]');
            const table = section.querySelector<HTMLTableElement>('[data-dividend-yield-selection-table]');
            if (!summary || !table || !table.tBodies || table.tBodies.length === 0) {
                return;
            }

            const selectedRows = table.tBodies[0].querySelectorAll<HTMLElement>('[data-dividend-yield-row][aria-selected="true"]');
            const selectedCount = selectedRows.length;
            const reveal = summary.closest<HTMLElement>('.selection-reveal');
            if (reveal) {
                reveal.classList.toggle('is-open', selectedCount > 0);
            } else {
                summary.classList.toggle('hidden', selectedCount === 0);
            }

            if (selectedCount === 0) {
                return;
            }

            let totalGrossAmount = 0;
            let totalNetAmount = 0;
            let totalTaxAmount = 0;
            let totalTaxableAmount = 0;
            let totalDailyPrincipalCost = 0;
            let totalAveragePrincipalCost = 0;
            // 기준일 원금이 0 인 배당(지급일 이전에 이미 전량 매도한 건)은 분모에 기여하지 않는다.
            // 그 세후액을 분자에 넣으면 수익률이 과대 계상되므로 서버 합계행은 이미 빼고 계산한다.
            // 여기서도 같은 값을 써야 같은 행을 골랐을 때 합계행과 숫자가 맞는다
            // (실측 2026-08-22: 배당 193건 중 5건이 그 대상 = 전체 세후의 0.23%).
            //
            // 세 수익률이 모두 이 값을 쓴다. 2026-08-24 까지는 일평균원금 기준만 totalNetAmount 를 썼고
            // 같은 표의 합계행도 그랬다 - 그래서 선택 합계와 합계행은 서로 맞았지만, 둘 다 그 열의
            // '행'과 어긋나 있었다(행은 서버가 걸러서 계산한 값을 그대로 그린다). 합계행을 행에 맞추면서
            // 여기도 함께 맞췄다.
            let totalNetWithPrincipalCost = 0;
            // 2026-09-28: 선택 요약의 수익률도 표의 기본 열(연 수익률)과 같은 규칙 - 세후 배당 x 365 / (날마다 들고 있던 원금의 합).
            // 들고 있던 날 평균 원금은 행끼리 더할 수 없지만(겹친 날) 원금 x 일 합은 더할 수 있어 합계행 · 서버와 같은 값이 나온다.
            let totalPrincipalCostDaySum = 0;

            for (let i = 0; i < selectedRows.length; i++) {
                const row = selectedRows[i];
                totalGrossAmount += Number(row.dataset.grossAmount || '0');
                totalNetAmount += Number(row.dataset.netAmount || '0');
                totalTaxAmount += Number(row.dataset.taxAmount || '0');
                totalTaxableAmount += Number(row.dataset.taxableAmount || '0');
                totalDailyPrincipalCost += Number(row.dataset.averageDailyPrincipalCost || '0');
                totalAveragePrincipalCost += Number(row.dataset.averagePrincipalCost || '0');
                totalNetWithPrincipalCost += Number(row.dataset.netWithPrincipalCost || '0');
                totalPrincipalCostDaySum += Number(row.dataset.principalCostDaySum || '0');
            }

            const totalNetReference = Number(summary.dataset.totalNetAmount || '0');
            const selectionWeight = totalNetReference > 0
                ? (totalNetAmount / totalNetReference) * 100
                : 0;
            const annualizedYield = totalPrincipalCostDaySum > 0
                ? (totalNetWithPrincipalCost * 365 / totalPrincipalCostDaySum) * 100
                : 0;
            const yieldOnBasisAverageCost = totalAveragePrincipalCost > 0
                ? (totalNetWithPrincipalCost / totalAveragePrincipalCost) * 100
                : 0;

            const countLabel = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-count]');
            if (countLabel) {
                countLabel.textContent = countChoice(summary.dataset.countTemplate!, selectedCount).replace('{0}', formatNumber(selectedCount));
            }

            const weightBadge = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-weight-badge]');
            if (weightBadge) {
                weightBadge.textContent = summary.dataset.weightLabel + ' ' + formatFixedNumber(selectionWeight, 1) + '%';
            }

            const grossLabel = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-gross]');
            if (grossLabel) {
                grossLabel.textContent = formatFixedNumber(totalGrossAmount, 0);
            }

            const netLabel = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-net]');
            if (netLabel) {
                netLabel.textContent = formatFixedNumber(totalNetAmount, 0);
            }

            const taxLabelEl = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-tax]');
            if (taxLabelEl) {
                taxLabelEl.textContent = formatFixedNumber(totalTaxAmount, 0);
            }

            const taxableLabelEl = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-taxable]');
            if (taxableLabelEl) {
                taxableLabelEl.textContent = formatFixedNumber(totalTaxableAmount, 0);
            }

            const dailyCapitalLabel = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-daily-capital]');
            if (dailyCapitalLabel) {
                dailyCapitalLabel.textContent = formatFixedNumber(totalDailyPrincipalCost, 0);
            }

            const yieldLabel = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-yield]');
            if (yieldLabel) {
                yieldLabel.textContent = formatFixedNumber(annualizedYield, 2) + '%';
            }

            const basisYieldLabel = summary.querySelector<HTMLElement>('[data-dividend-yield-selection-basis-yield]');
            if (basisYieldLabel) {
                basisYieldLabel.textContent = summary.dataset.basisLabel + ' ' + formatFixedNumber(yieldOnBasisAverageCost, 2) + '%';
            }
        }

        function getDividendYieldSortValue(row: HTMLElement, sortKey: string, sortType: string): any {
            if (!row || !sortKey) {
                return sortType === 'number' ? 0 : '';
            }

            const rawValue = row.dataset[sortKey];
            if (sortType === 'number') {
                const parsedValue = Number(rawValue);
                return Number.isFinite(parsedValue) ? parsedValue : 0;
            }

            return rawValue || '';
        }

        function compareDividendYieldSortValues(leftValue: any, rightValue: any, sortType: string): number {
            if (sortType === 'number') {
                return leftValue - rightValue;
            }

            return String(leftValue).localeCompare(String(rightValue), 'ko', {
                numeric: true,
                sensitivity: 'base'
            });
        }

        function updateDividendYieldSortIndicators(table: HTMLTableElement) {
            if (!table) {
                return;
            }

            const activeKey = table.dataset.sortKey;
            const direction = table.dataset.sortDirection;
            const buttons = table.querySelectorAll<HTMLElement>('[data-sort-key]');
            for (let i = 0; i < buttons.length; i++) {
                const button = buttons[i];
                const indicator = button.querySelector<HTMLElement>('[data-sort-indicator]');
                const headerCell = button.parentElement;
                const isActive = activeKey && button.dataset.sortKey === activeKey;

                if (indicator) {
                    indicator.textContent = isActive
                        ? (direction === 'asc' ? '▲' : '▼')
                        : '↕';
                }

                if (headerCell) {
                    headerCell.setAttribute(
                        'aria-sort',
                        isActive
                            ? (direction === 'asc' ? 'ascending' : 'descending')
                            : 'none'
                    );
                }
            }
        }

        function sortDividendYieldTable(table: HTMLTableElement, sortKey: string, sortType: string) {
            if (!table || !sortKey || !sortType || !table.tBodies || table.tBodies.length === 0) {
                return;
            }

            let nextDirection: string;
            if (table.dataset.sortKey === sortKey) {
                nextDirection = table.dataset.sortDirection === 'asc' ? 'desc' : 'asc';
            } else {
                nextDirection = sortType === 'text' ? 'asc' : 'desc';
            }

            table.dataset.sortKey = sortKey;
            table.dataset.sortDirection = nextDirection;

            const tbody = table.tBodies[0];
            const rows = Array.from(tbody.querySelectorAll('[data-dividend-yield-row]'));
            rows.sort(function(left: any, right: any) {
                let compared = compareDividendYieldSortValues(
                    getDividendYieldSortValue(left, sortKey, sortType),
                    getDividendYieldSortValue(right, sortKey, sortType),
                    sortType
                );

                if (compared === 0) {
                    compared = compareDividendYieldSortValues(
                        left.dataset.label || '',
                        right.dataset.label || '',
                        'text'
                    );
                }

                return nextDirection === 'asc' ? compared : -compared;
            });

            for (let i = 0; i < rows.length; i++) {
                tbody.appendChild(rows[i]);
            }

            updateDividendYieldSortIndicators(table);
        }

        function initializeDividendYieldSortableTables() {
            const tables = document.querySelectorAll<HTMLTableElement>('[data-dividend-yield-sortable-table]');
            for (let i = 0; i < tables.length; i++) {
                const table = tables[i];
                if (table.dataset.sortableInitialized === 'true') {
                    continue;
                }

                table.dataset.sortableInitialized = 'true';

                const buttons = table.querySelectorAll<HTMLElement>('[data-sort-key]');
                for (let j = 0; j < buttons.length; j++) {
                    buttons[j].addEventListener('click', function() {
                        const targetTable = this.closest<HTMLTableElement>('[data-dividend-yield-sortable-table]');
                        sortDividendYieldTable(targetTable!, this.dataset.sortKey!, this.dataset.sortType!);
                    });
                }

                updateDividendYieldSortIndicators(table);
            }
        }

        function initializeDividendYieldSelection() {
            const sections = document.querySelectorAll<HTMLElement>('[data-dividend-yield-selection-section]');
            for (let i = 0; i < sections.length; i++) {
                const section = sections[i];
                if (section.dataset.selectionInitialized === 'true') {
                    continue;
                }

                const table = section.querySelector<HTMLTableElement>('[data-dividend-yield-selection-table]');
                if (!table || !table.tBodies || table.tBodies.length === 0) {
                    continue;
                }

                section.dataset.selectionInitialized = 'true';

                const rows = table.tBodies[0].querySelectorAll<HTMLElement>('[data-dividend-yield-row]');
                for (let j = 0; j < rows.length; j++) {
                    const row = rows[j];

                    row.addEventListener('click', function(this: HTMLElement, event: MouseEvent) {
                        if (event.target && (event.target as Element).closest('button, a, input, label, select, textarea')) {
                            return;
                        }

                        const isSelected = this.getAttribute('aria-selected') === 'true';
                        setDividendYieldRowSelection(this, !isSelected);
                        updateDividendYieldSelectionSummary(section);
                    });

                    row.addEventListener('keydown', function(this: HTMLElement, event: KeyboardEvent) {
                        if (event.target && (event.target as Element).closest('button, a, input, label, select, textarea')) {
                            return;
                        }

                        if (event.key !== 'Enter' && event.key !== ' ') {
                            return;
                        }

                        event.preventDefault();
                        const isSelected = this.getAttribute('aria-selected') === 'true';
                        setDividendYieldRowSelection(this, !isSelected);
                        updateDividendYieldSelectionSummary(section);
                    });

                }

                const clearButton = section.querySelector<HTMLElement>('[data-dividend-yield-selection-clear]');
                if (clearButton) {
                    clearButton.addEventListener('click', function() {
                        const selectedRows = table.tBodies[0].querySelectorAll<HTMLElement>('[data-dividend-yield-row][aria-selected="true"]');
                        for (let k = 0; k < selectedRows.length; k++) {
                            setDividendYieldRowSelection(selectedRows[k], false);
                        }

                        updateDividendYieldSelectionSummary(section);
                    });
                }

                updateDividendYieldSelectionSummary(section);
            }
        }

        if (typeof win.initializeStockTagHoverTooltips !== 'function') {
            win.initializeStockTagHoverTooltips = function(root: ParentNode | null) {
                let tooltip = win.stockTagHoverTooltip;
                if (!tooltip) {
                    const tooltipEl = document.createElement('div');
                    tooltipEl.id = 'stockTagHoverTooltip';
                    tooltipEl.className = 'pointer-events-none fixed left-0 top-0 z-50 hidden w-64 max-w-[calc(100vw-2rem)] rounded-box bg-neutral px-3 py-2 text-left text-xs leading-5 text-neutral-content shadow-lg sm:w-72';
                    document.body.appendChild(tooltipEl);

                    tooltip = {
                        element: tooltipEl,
                        show(message: string, clientX: number, clientY: number) {
                            if (!message) {
                                this.hide();
                                return;
                            }

                            this.element.textContent = message;
                            this.element.classList.remove('hidden');
                            this.move(clientX, clientY);
                        },
                        move(clientX: number, clientY: number) {
                            if (this.element.classList.contains('hidden')) {
                                return;
                            }

                            const offsetX = 12;
                            const offsetY = 18;
                            const viewportPadding = 12;
                            let left = clientX + offsetX;
                            let top = clientY + offsetY;
                            const rect = this.element.getBoundingClientRect();

                            if (left + rect.width + viewportPadding > win.innerWidth) {
                                left = Math.max(viewportPadding, win.innerWidth - rect.width - viewportPadding);
                            }

                            if (top + rect.height + viewportPadding > win.innerHeight) {
                                top = Math.max(viewportPadding, clientY - rect.height - offsetY);
                            }

                            this.element.style.left = left + 'px';
                            this.element.style.top = top + 'px';
                        },
                        hide() {
                            this.element.classList.add('hidden');
                            this.element.textContent = '';
                        }
                    };

                    win.stockTagHoverTooltip = tooltip;
                    win.addEventListener('scroll', function() {
                        if (win.stockTagHoverTooltip) {
                            win.stockTagHoverTooltip.hide();
                        }
                    }, true);
                    win.addEventListener('resize', function() {
                        if (win.stockTagHoverTooltip) {
                            win.stockTagHoverTooltip.hide();
                        }
                    });
                }

                const scope = root || document;
                const targets = scope.querySelectorAll<HTMLElement>('[data-stock-tag-tooltip]');
                for (let i = 0; i < targets.length; i++) {
                    const target = targets[i];
                    if (target.dataset.stockTagTooltipInitialized === 'true') {
                        continue;
                    }

                    target.dataset.stockTagTooltipInitialized = 'true';

                    target.addEventListener('mouseenter', function(this: HTMLElement, event: MouseEvent) {
                        tooltip.show(this.dataset.stockTagTooltip, event.clientX, event.clientY);
                    });

                    target.addEventListener('mousemove', function(this: HTMLElement, event: MouseEvent) {
                        tooltip.move(event.clientX, event.clientY);
                    });

                    target.addEventListener('mouseleave', function() {
                        tooltip.hide();
                    });
                }
            };
        }

        let monthlyChart: any = null;
        let donutChart: any = null;

        // 월별 라벨이 덮을 구간. 두 차트와 월평균 카드가 같은 답을 쓰도록 한 곳에서 정한다.
        //
        // '전체'를 고르면 rangeMode 가 all 이라 시작일·종료일이 아예 비어 온다. 예전에는 그때
        // 빈 달을 채우는 경로를 통째로 건너뛰어, 배당이 들어온 달만 라벨이 됐다 - 그래서 월평균의
        // 분모가 '개월수'가 아니라 '배당이 있었던 달의 수'였다(실측 2026-08-24: 2020-04 ~ 2026-08
        // 은 달력으로 77개월인데 36으로 나눠, 실제 달력 기준보다 2.14배 크게 나왔다).
        // 다른 기간(1년·3년 등)은 날짜가 채워져 있어 빈 달까지 세므로, '전체'만 기준이 달랐다.
        //
        // 전체일 때는 자료의 첫 달 ~ 마지막 달을 구간으로 삼는다. 그러면 분모가 달력 개월수가 되고
        // 차트 x축에도 배당이 0원이던 달(실측 41개월)이 그대로 드러난다.
        /**
         * 기간을 '달 수' 로 환산한다. 달마다 기간이 덮은 일수 / 그 달의 총 일수 를 더한다.
         *
         * 걸친 달력 월 수를 세면 기간이 달 경계를 넘을 때마다 분모가 한 달씩 부풀어, 월평균이 그만큼 작아진다
         * - 실측 2026-09-11(배당 화면): 1개월 프리셋 31일이 2개월로 나뉘어 월평균이 실제의 절반(-49%),
         *   3개월 92일 -> 4개월(-25%), 6개월 184일 -> 7개월(-14%), 12개월 365일 -> 13개월(-8%).
         * 연 환산 수익률이 이미 일수 기준(365/기간일수)이므로 같은 규칙으로 맞춘다.
         */
        function monthEquivalent(fromDay: string, toDay: string): number {
            if (!fromDay || !toDay) return 0;
            const start = new Date(fromDay + "T00:00:00Z");
            const end = new Date(toDay + "T00:00:00Z");
            if (isNaN(start.getTime()) || isNaN(end.getTime()) || end < start) return 0;
            let total = 0;
            let cursor = new Date(Date.UTC(start.getUTCFullYear(), start.getUTCMonth(), 1));
            while (cursor <= end) {
                const monthStart = cursor;
                const monthEnd = new Date(Date.UTC(cursor.getUTCFullYear(), cursor.getUTCMonth() + 1, 0));
                const daysInMonth = monthEnd.getUTCDate();
                const from = monthStart > start ? monthStart : start;
                const to = monthEnd < end ? monthEnd : end;
                if (to >= from) {
                    const covered = Math.round((to.getTime() - from.getTime()) / 86400000) + 1;
                    total += covered / daysInMonth;
                }
                cursor = new Date(Date.UTC(cursor.getUTCFullYear(), cursor.getUTCMonth() + 1, 1));
            }
            return total;
        }

        /** 월평균의 분모. 필터 기간이 있으면 그 기간, 없으면(전체) 데이터의 첫 배당일 ~ 마지막 배당일. */
        function averageMonthSpan(): number {
            let from = filterStartDay, to = filterEndDay;
            if (!from || !to) {
                const days = dividendData
                    .filter((d: DividendRow) => d.payDate)
                    .map((d: DividendRow) => d.payDate)
                    .sort();
                if (days.length === 0) return 0;
                from = days[0];
                to = days[days.length - 1];
            }
            return monthEquivalent(from, to);
        }

        function monthlyRange() {
            let from = filterStartDate, to = filterEndDate;
            if (!from || !to) {
                const seen = dividendData
                    .filter((d: DividendRow) => d.payDate)
                    .map((d: DividendRow) => d.payDate.slice(0, 7))
                    .sort();
                if (seen.length === 0) return null;
                from = seen[0];
                to = seen[seen.length - 1];
            }
            return { from: from, to: to };
        }

        /** from ~ to 사이의 'YYYY-MM' 을 빠짐없이 돌려준다. */
        function monthsInRange(range: MonthRange | null): string[] {
            const months: string[] = [];
            if (!range) return months;
            let cur = range.from;
            while (cur <= range.to) {
                months.push(cur);
                const parts = cur.split('-');
                const y = Number(parts[0]), mo = Number(parts[1]);
                cur = mo === 12 ? (y + 1) + '-01' : y + '-' + String(mo + 1).padStart(2, '0');
            }
            return months;
        }

        function buildMonthlyData() {
            const map: Record<string, number> = {};
            monthsInRange(monthlyRange()).forEach((m: string) => { map[m] = 0; });
            dividendData.forEach((d: DividendRow) => {
                if (!d.payDate) return;
                const mon = d.payDate.slice(0, 7);
                map[mon] = (map[mon] || 0) + Number(d.net);
            });
            const months = Object.keys(map).sort();
            return { labels: months, data: months.map((m: string) => map[m]) };
        }

        // 월별 × 종목 누적: 어떤 종목이 각 달의 증감을 만들었는지 한눈에. 상위 N개 + 나머지는 '기타'.
        function buildMonthlyStackedData() {
            const monthSet: Record<string, boolean> = {};
            monthsInRange(monthlyRange()).forEach((m: string) => { monthSet[m] = true; });
            dividendData.forEach((d: DividendRow) => { if (d.payDate) monthSet[d.payDate.slice(0, 7)] = true; });
            const months = Object.keys(monthSet).sort();
            const monthIndex: Record<string, number> = {};
            months.forEach((m: string, i: number) => { monthIndex[m] = i; });

            const stockTotal: Record<string, number> = {};
            dividendData.forEach((d: DividendRow) => { stockTotal[d.stockItem] = (stockTotal[d.stockItem] || 0) + Number(d.net); });
            const ranked = Object.keys(stockTotal).sort((a: string, b: string) => stockTotal[b] - stockTotal[a]);
            const TOP = 8;
            const topStocks = ranked.slice(0, TOP);
            const isTop: Record<string, boolean> = {};
            topStocks.forEach((s: string) => { isTop[s] = true; });
            const hasOthers = ranked.length > TOP;

            const seriesMap: Record<string, number[]> = {};
            topStocks.forEach((s: string) => { seriesMap[s] = months.map(() => 0); });
            const othersSeries = hasOthers ? months.map(() => 0) : null;
            dividendData.forEach((d: DividendRow) => {
                if (!d.payDate) return;
                const mi = monthIndex[d.payDate.slice(0, 7)];
                if (mi == null) return;
                const amt = Number(d.net);
                if (isTop[d.stockItem]) seriesMap[d.stockItem][mi] += amt;
                else if (othersSeries) othersSeries[mi] += amt;
            });

            const datasets = topStocks.map((s: string, i: number) => ({
                label: s,
                data: seriesMap[s],
                backgroundColor: CHART_COLORS[i % CHART_COLORS.length],
                // borderWidth 를 0 으로 박으면 stock-charts 의 요소 테두리(WCAG 1.4.11)가 안 걸린다
                // - 실측 2026-09-10: 이 차트만 라이트 84개 요소가 3:1 미달로 남았다. 기본값(1)에 맡긴다.
                borderRadius: 2
            }));
            if (othersSeries) {
                // 라벨에 기준을 실어 카드의 "기타"(월중/월말 태그 없는 종목)와 가른다 - 같은 화면에 두 뜻이
                // 있었고 수는 565 배까지 달랐다(실측 2026-09-12 올해: 카드 6,152,014 vs 차트 10,899).
                const othersCount = ranked.length - TOP;
                const othersText = othersLabel
                    .replace("{0}", String(othersCount))
                    .replace("{1}", String(TOP));
                datasets.push({
                    label: othersText,
                    data: othersSeries,
                    backgroundColor: 'rgba(148,163,184,0.6)',
                    borderRadius: 2
                });
            }
            return { labels: months, datasets: datasets };
        }

        function buildDonutData(groupBy: string) {
            const map: Record<string, number> = {};
            dividendData.forEach((d: DividendRow) => {
                const key = groupBy === 'stock' ? d.stockItem : d.account;
                map[key] = (map[key] || 0) + Number(d.net);
            });
            const sorted = Object.entries(map).sort((a: [string, number], b: [string, number]) => b[1] - a[1]);
            return { labels: sorted.map((e: [string, number]) => e[0]), data: sorted.map((e: [string, number]) => e[1]) };
        }

        function initMonthlyChart() {
            const m = buildMonthlyStackedData();
            const gridColor = 'rgba(128,128,128,0.1)';
            const canvasId = 'monthlyBarChart';
            const canvasEl = document.getElementById(canvasId);
            if (!canvasEl) return;
            if (monthlyChart) try { monthlyChart.destroy(); } catch(e) {}
            // 도넛과 같은 규칙: 자료가 없으면 빈 캔버스 대신 안내를 둔다.
            // 라벨은 자료가 0건이어도 기간에서 만들어진다(실측: 라벨 1개 · 점 0개) - 점 개수로 판정해야 한다.
            const monthlyPointCount = (m.datasets || []).reduce((sum: number, d: any) => sum + ((d && d.data ? d.data.length : 0)), 0);
            // 툴팁도 앱 로케일을 따라야 한다 - 예전에는 로케일 인자가 없어 브라우저 로케일로 찍혔다
            // (실측 2026-09-15: 브라우저 de-DE 에서 이 툴팁만 '₩37.980' 으로 갈렸다).
            const fmtFull = (v: number) => (win.StockCharts
                ? win.StockCharts.formatCurrency(v)
                : '₩' + Math.round(v).toLocaleString(appLocaleOrDefault()));
            // "최근 12개월 합" 선(오른쪽 축)은 뺐다(사용자 요청 2026-10-02) - 막대와 크기대가 다른 금액이 따로 붙어 헷갈렸다.
            const chartDatasets: any[] = m.datasets.slice();
            var config = {
                type: 'bar',
                data: { labels: m.labels, datasets: chartDatasets },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    layout: { padding: { right: 8, bottom: 4 } },
                    // 한 달의 모든 종목 구간 + 합계를 함께 보여줌(증감 원인 파악용)
                    interaction: { mode: 'index', intersect: false },
                    plugins: {
                        legend: { display: true, position: 'bottom', labels: { font: { size: 10 }, boxWidth: 10, boxHeight: 10, padding: 8, usePointStyle: true } },
                        tooltip: {
                            itemSort: (a: any, b: any) => b.parsed.y - a.parsed.y,
                            callbacks: {
                                label: (ctx: any) => ctx.parsed.y ? (ctx.dataset.label + ': ' + fmtFull(ctx.parsed.y)) : null,
                                footer: (items: any) => {
                                    let sum = 0; items.forEach((it: any) => { sum += it.parsed.y; });
                                    return netAmountLabel + ': ' + fmtFull(sum);
                                }
                            }
                        }
                    },
                    scales: {
                        x: { stacked: true, grid: { color: gridColor }, ticks: { maxRotation: 45, font: { size: 11 } } },
                        y: { stacked: true, beginAtZero: true, grid: { color: gridColor }, ticks: { callback: (v: any) => win.StockCharts ? win.StockCharts.formatCompactNumber(v) : Math.round(v).toLocaleString(appLocaleOrDefault()), font: { size: 11 } } }
                    }
                }
            };
            win.ensureStockCharts(function() {
                if (win.StockCharts.renderChartEmptyNote(canvasId, monthlyPointCount === 0, noPeriodHistoryLabel)) {
                    monthlyChart = null;
                    return;
                }
                monthlyChart = win.StockCharts.createChart(canvasId, config, monthlyChart);
            });
        }

        function initDonutChart(mode: string) {
            const d = buildDonutData(mode);
            const canvasId = 'donutChart';
            const canvasEl = document.getElementById(canvasId);
            if (!canvasEl) return;
            if (donutChart) try { donutChart.destroy(); } catch(e) {}
            const legend = document.getElementById('donutLegend');
            if (d.labels.length === 0) {
                canvasEl.style.display = 'none';
                if (legend) legend.innerHTML = '<div class="text-xs text-base-content/70 pt-4 text-center">' + noPeriodHistoryLabel + '</div>';
                return;
            }
            if (canvasEl.parentElement) canvasEl.parentElement.style.display = '';
            canvasEl.style.display = '';
            var config = {
                type: 'doughnut',
                data: {
                    labels: d.labels,
                    datasets: [{
                        data: d.data,
                        backgroundColor: d.labels.map((_: string, i: number) => CHART_COLORS[i % CHART_COLORS.length]),
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: { callbacks: { label: (ctx: any) => ' ' + formatCurrency(Math.round(ctx.parsed)) } }
                    },
                    cutout: '62%'
                }
            };
            win.ensureStockCharts(function() { donutChart = win.StockCharts.createChart(canvasId, config, donutChart); });
            const total = d.data.reduce((sum: number, v: number) => sum + v, 0);
            if (legend) {
                // 조각마다 따로 반올림하면 합이 100.0 이 아니게 된다(매매 도넛 실측 99.9%). 공용 규칙을 쓴다.
                const shownPercents = win.StockCharts && win.StockCharts.balancedPercents
                    ? win.StockCharts.balancedPercents(d.data, 1)
                    : d.data.map((v: number) => (total > 0 ? ((v / total) * 100).toFixed(1) : '0.0'));
                legend.innerHTML = d.labels.map((label: string, i: number) => {
                    const pct = shownPercents[i];
                    const color = CHART_COLORS[i % CHART_COLORS.length];
                    return '<div class="flex items-center gap-1 mb-0.5">'
                        + '<span class="chart-legend-swatch" style="flex-shrink:0;display:inline-block;width:8px;height:8px;border-radius:50%;background:' + color + '"></span>'
                        + '<span class="flex-1" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' + label + '">' + label + '</span>'
                        + '<span style="flex-shrink:0;opacity:0.75;">' + pct + '%</span>'
                        + '</div>';
                }).join('');
            }
        }

        function updateAvgCard() {
            const displayEl = document.getElementById('avgAmountDisplay');
            const descEl = document.getElementById('avgAmountDesc');
            if (!displayEl) return;
            if (dividendData.length === 0) {
                displayEl.textContent = formatCurrency(0);
                if (descEl) descEl.textContent = averageNoDataLabel;
                return;
            }
            const totalNet = dividendData.reduce((sum: number, d: DividendRow) => sum + Number(d.net), 0);
            // 분모는 기간 길이(월 환산)다. 걸친 달력 월 수를 쓰면 경계를 넘을 때마다 한 달씩 부풀어 평균이 작아진다.
            const span = averageMonthSpan();
            const monthCount = span > 0 ? span : (buildMonthlyData().labels.length || 1);
            const avgPerMonth = totalNet / monthCount;
            displayEl.textContent = formatCurrency(Math.round(avgPerMonth));
            if (descEl) {
                // 총액은 실제 배당 합계라 '금액 숨김' 대상이다. amount-value 로 감싸지 않으면
                // 숨김을 켜도 이 줄만 그대로 보인다(실측: 배당내역에서 유일한 누락).
                // 표시한 개월수로 나누면 화면의 월평균이 다시 나와야 한다. 정수로 반올림하면 재현이 깨진다
                // (실측 2026-09-11: 전체 기간 76.83개월을 '77개월' 로 적으면 65,652,134/77 = 852,625 로 화면값 854,475 와 다르다).
                // 정수에 가까우면 정수로, 아니면 소수 한 자리로 적고, 정확한 구간과 개월수는 툴팁에 남긴다.
                const monthText = Math.abs(monthCount - Math.round(monthCount)) < 0.05
                    ? formatNumber(Math.round(monthCount))
                    : String(Math.round(monthCount * 10) / 10);
                // 딱 1 개월이면 단수 문구("Average for 1 month") - 2026-10-01 영어 점검.
                descEl.innerHTML = (monthText === "1" ? averageDescOneTemplate : averageDescTemplate)
                    .replace('{0}', monthText)
                    .replace('{1}', '<span class="amount-value">' + formatCurrency(Math.round(totalNet)) + '</span>')
                    .replace('{2}', formatCount(dividendData.length));
                const spanFrom = filterStartDay || (dividendData.map((d: DividendRow) => d.payDate).filter(Boolean).sort()[0] || "");
                const spanTo = filterEndDay || (dividendData.map((d: DividendRow) => d.payDate).filter(Boolean).sort().slice(-1)[0] || "");
                const spanDetail = spanFrom && spanTo
                    ? spanFrom + " ~ " + spanTo + " (" + (Math.round(monthCount * 100) / 100) + ")"
                    : "";
                descEl.title = spanDetail;
                // title 은 접근성 트리에 안 올라간다(이 앱이 2026-09-10 에 겪은 실수) - 같은 내용을 sr-only 로도 남긴다.
                if (spanDetail) {
                    const srSpan = document.createElement("span");
                    srSpan.className = "sr-only";
                    srSpan.textContent = " " + spanDetail;
                    descEl.appendChild(srSpan);
                }
            }
        }

        function updateNavButtons() {
            // nav button state is handled by DateRangePicker
        }

        function initAll() {
            updateNavButtons();
            initMonthlyChart();
            initDonutChart('stock');
            updateAvgCard();
        }

        win.switchDonut = function(mode: string) {
            (document.getElementById('donutTabStock') as any).classList.toggle('tab-active', mode === 'stock');
            (document.getElementById('donutTabAccount') as any).classList.toggle('tab-active', mode !== 'stock');
            initDonutChart(mode);
        };

        win.dividendPicker = win.DateRangePicker
            ? win.DateRangePicker.create({
                formId:      'dividendSearchForm',
                startId:     'startDateInput',
                endId:       'endDateInput',
                instantStartId: 'dividendStartInstantInput',
                instantEndId:   'dividendEndInstantInput',
                timeZoneId:     'dividendTimeZoneInput',
                rangeModeId: 'dividendRangeModeInput',
                btnClass:    'date-range-btn',
                minDate:     cfg.minDate || '',
                globalKey:   'globalDateRange',
                rootSelector: '#dividendListFragment'
            })
            : { set: function(){}, shift: function(){}, jumpToEdge: function(){} };

        // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
        try {
            if (win.dividendPicker && typeof win.dividendPicker.getState === 'function') {
                var __s = win.dividendPicker.getState();
                if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
                    var __key = 'dateRangeDefaultApplied:dividendPicker';
                    try {
                        if (!sessionStorage.getItem(__key)) {
                            var _btn = document.querySelector<HTMLElement>('#dividendListFragment .date-range-btn');
                            win.dividendPicker.set('mtd', _btn || null);
                            sessionStorage.setItem(__key, '1');
                        }
                    } catch(e) { win.dividendPicker.set('mtd', null); }
                }
            }
        } catch(e) {}

        // details arrow animation
        const detailSection = document.getElementById('detailSection') as HTMLDetailsElement | null;
        if (detailSection) {
            const arrow = detailSection.querySelector<HTMLElement>('.detail-arrow');
            detailSection.addEventListener('toggle', function() {
                if (arrow) arrow.style.transform = detailSection.open ? 'rotate(90deg)' : '';
            });
        }

        initializeDividendYieldSortableTables();
        initializeDividendYieldSelection();
        win.initializeStockTagHoverTooltips(document);

        // Chart.js 는 stockLayout 에서 1회 로드되어 항상 존재한다. (initAll 은 ensureStockCharts 로 헬퍼 보장)
        setTimeout(initAll, 0);
        setTimeout(() => { const SC = win.StockCharts; const fit = (c: any) => { if (!c) return; if (SC && SC.resizeIfChanged) SC.resizeIfChanged(c); else c.resize(); }; fit(monthlyChart); fit(donutChart); }, 200);


	}

	function initializeDividendHistory() {
		const fragment = document.getElementById("dividendListFragment");
		const cfg = win.dividendHistoryConfig;
		if (!fragment || !cfg) return;
		if (fragment.dataset.dividendHistoryInit === "1") return;
		fragment.dataset.dividendHistoryInit = "1";
		run(cfg);
	}

	win.initializeDividendHistory = initializeDividendHistory;
	document.addEventListener("htmx:afterSettle", initializeDividendHistory);
	initializeDividendHistory();
})();
