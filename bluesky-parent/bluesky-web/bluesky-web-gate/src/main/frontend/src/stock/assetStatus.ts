// 자산 현황 조각의 브라우저 동작: 계좌 상세 펼침 · 표 정렬 · 행 선택 합계 · 태그 툴팁.
//
// 2026-09-08 까지 assetStatus.jte 안의 인라인 <script> 였다(669 줄 - 이 모듈 인라인 스크립트 22 개 중 가장 큰
// 순수 JS 였다: JTE 식이 하나도 없어 파일로 뺄 수 있었다). 인라인일 때는 검사가 템플릿 문자열에서 함수를
// 오려 내어 돌렸고(inlineTemplateScript.mjs), 타입 검사는 전혀 없었다.
//
// 로딩: 조각이 <script type="module" src> 로 부른다. 모듈은 문서당 한 번만 실행되므로 첫 실행 때 초기화하고,
// 그 뒤 htmx 재스왑은 htmx:afterSettle 에서 다시 초기화한다. 초기화 함수들은 data-*-bound 표식으로 중복 결합을
// 막으므로 몇 번 불려도 안전하다(인라인 시절부터 그랬다).
//
// 타입은 느슨하다(any). 옮기면서 동작을 바꾸지 않는 것이 먼저라, 검사기가 요구하는 만큼만 붙였다.
export {};

(() => {
	const win: any = window;
	if (win.__assetStatusScriptAttached) {
		if (typeof win.initializeAssetStatus === "function") win.initializeAssetStatus();
		return;
	}
	win.__assetStatusScriptAttached = true;


    function toggleAssetStatusAccountDetail(button: HTMLElement, rowId: string) {
        var detailRow = document.getElementById(rowId);
        if (!button || !detailRow) {
            return;
        }

        var expanded = button.getAttribute('aria-expanded') === 'true';
        var nextExpanded = !expanded;
        button.setAttribute('aria-expanded', nextExpanded ? 'true' : 'false');
        detailRow.classList.toggle('hidden', !nextExpanded);

        var labelEl = button.querySelector<HTMLElement>('[data-label]');
        if (labelEl) {
            labelEl.textContent = nextExpanded
                ? button.dataset.collapseLabel!
                : button.dataset.expandLabel!;
        }

        var iconEl = button.querySelector<HTMLElement>('[data-icon]');
        if (iconEl) {
            iconEl.textContent = nextExpanded ? '▲' : '▼';
        }
    }

    function getAssetStatusSortValue(row: HTMLElement, sortKey: string, sortType: string): any {
        if (!row || !sortKey) {
            return sortType === 'number' ? 0 : '';
        }

        var rawValue = row.dataset[sortKey];
        if (sortType === 'number') {
            var parsedValue = Number(rawValue);
            return Number.isFinite(parsedValue) ? parsedValue : 0;
        }

        return rawValue || '';
    }

    function compareAssetStatusSortValues(leftValue: any, rightValue: any, sortType: string): number {
        if (sortType === 'number') {
            return leftValue - rightValue;
        }

        return String(leftValue).localeCompare(String(rightValue), 'ko', {
            numeric: true,
            sensitivity: 'base'
        });
    }

    function updateAssetStatusSortIndicators(table: HTMLTableElement) {
        if (!table) {
            return;
        }

        var activeKey = table.dataset.sortKey;
        var direction = table.dataset.sortDirection;
        var buttons = table.querySelectorAll<HTMLElement>('[data-sort-key]');
        for (var i = 0; i < buttons.length; i++) {
            var button = buttons[i];
            var indicator = button.querySelector<HTMLElement>('[data-sort-indicator]');
            var headerCell = button.parentElement;
            var isActive = button.dataset.sortKey === activeKey;

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

    function sortAssetStatusTable(table: HTMLTableElement, sortKey: string, sortType: string) {
        if (!table || !sortKey || !sortType || !table.tBodies || table.tBodies.length === 0) {
            return;
        }

        var nextDirection: string;
        if (table.dataset.sortKey === sortKey) {
            nextDirection = table.dataset.sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            nextDirection = sortType === 'text' ? 'asc' : 'desc';
        }

        table.dataset.sortKey = sortKey;
        table.dataset.sortDirection = nextDirection;

        var tbody = table.tBodies[0];
        var rows = tbody.querySelectorAll<HTMLTableRowElement>('[data-sortable-row]');
        var rowGroups = [];

        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            var detailRowId = row.dataset.detailRowId;
            var groupNodes: HTMLElement[] = [row];

            if (detailRowId) {
                var detailRow = document.getElementById(detailRowId);
                if (detailRow && detailRow.parentElement === tbody) {
                    groupNodes.push(detailRow);
                }
            }

            rowGroups.push({
                row: row,
                groupNodes: groupNodes
            });
        }

        rowGroups.sort(function(left: any, right: any) {
            var compared = compareAssetStatusSortValues(
                getAssetStatusSortValue(left.row, sortKey, sortType),
                getAssetStatusSortValue(right.row, sortKey, sortType),
                sortType
            );

            if (compared === 0) {
                compared = compareAssetStatusSortValues(
                    left.row.dataset.account || left.row.dataset.stockName || '',
                    right.row.dataset.account || right.row.dataset.stockName || '',
                    'text'
                );
            }

            return nextDirection === 'asc' ? compared : -compared;
        });

        for (var j = 0; j < rowGroups.length; j++) {
            var group = rowGroups[j].groupNodes;
            for (var k = 0; k < group.length; k++) {
                tbody.appendChild(group[k]);
            }
        }

        updateAssetStatusSortIndicators(table);
    }

    function initializeAssetStatusDetailToggles() {
        var buttons = document.querySelectorAll<HTMLElement>('[data-account-detail-toggle]');
        for (var i = 0; i < buttons.length; i++) {
            var button = buttons[i];
            if (button.dataset.detailToggleBound === 'true') {
                continue;
            }

            button.dataset.detailToggleBound = 'true';
            button.addEventListener('click', function(this: HTMLElement) {
                toggleAssetStatusAccountDetail(this, this.dataset.accountDetailToggle!);
            });
        }
    }

    function initializeAssetStatusSortableTables() {
        var tables = document.querySelectorAll<HTMLTableElement>('[data-sortable-table]');
        for (var i = 0; i < tables.length; i++) {
            var table = tables[i];
            if (table.dataset.sortableInitialized === 'true') {
                continue;
            }

            table.dataset.sortableInitialized = 'true';

            var buttons = table.querySelectorAll<HTMLElement>('[data-sort-key]');
            for (var j = 0; j < buttons.length; j++) {
                buttons[j].addEventListener('click', function(this: HTMLElement) {
                    var targetTable = this.closest<HTMLTableElement>('[data-sortable-table]');
                    sortAssetStatusTable(targetTable!, this.dataset.sortKey!, this.dataset.sortType!);
                });
            }

            updateAssetStatusSortIndicators(table);
        }
    }

    function formatAssetStatusMessage(template: string, value: string | number) {
        return String(template || '').replace('{0}', String(value));
    }

    function getAssetStatusLocale() {
        return document.documentElement.getAttribute('lang') || 'ko-KR';
    }

    function formatAssetStatusNumber(value: number, fractionDigits: number) {
        return new Intl.NumberFormat(getAssetStatusLocale(), {
            minimumFractionDigits: fractionDigits,
            maximumFractionDigits: fractionDigits
        }).format(value);
    }

    function formatAssetStatusSignedNumber(value: number, fractionDigits: number) {
        var absoluteValue = Math.abs(value);
        var formattedValue = formatAssetStatusNumber(absoluteValue, fractionDigits);

        if (value > 0) {
            return '+' + formattedValue;
        }

        if (value < 0) {
            return '-' + formattedValue;
        }

        return formatAssetStatusNumber(0, fractionDigits);
    }

    function setAssetStatusStockRowSelection(row: HTMLElement | null, selected: boolean) {
        if (!row) {
            return;
        }

        row.classList.toggle('asset-status-stock-row-selected', selected);
        row.setAttribute('aria-pressed', selected ? 'true' : 'false');

    }

    function setAssetStatusAccountRowSelection(row: HTMLElement | null, selected: boolean) {
        if (!row) {
            return;
        }

        row.classList.toggle('asset-status-account-row-selected', selected);
        row.dataset.selected = selected ? 'true' : 'false';
        row.setAttribute('aria-pressed', selected ? 'true' : 'false');

    }

    function updateAssetStatusAccountSelectionSummary(section: HTMLElement | null) {
        if (!section) {
            return;
        }

        var summary = section.querySelector<HTMLElement>('[data-asset-status-account-selection-summary]');
        var table = section.querySelector<HTMLTableElement>('[data-asset-status-account-table]');
        if (!summary || !table || !table.tBodies || table.tBodies.length === 0) {
            return;
        }

        var selectedRows = table!.tBodies[0].querySelectorAll<HTMLElement>('[data-account-selectable-row][data-selected="true"]');
        var selectedCount = selectedRows.length;
        var reveal = summary.closest<HTMLElement>('.selection-reveal');
        if (reveal) {
            reveal.classList.toggle('is-open', selectedCount > 0);
        } else {
            summary.classList.toggle('hidden', selectedCount === 0);
        }

        if (selectedCount === 0) {
            return;
        }

        var totalBuyAmount = 0;
        var totalEvaluationAmountValue = 0;
        var totalEvaluationProfitValue = 0;
        var totalPrincipalValue = 0;
        var totalPrincipalReturnValue = 0;

        for (var i = 0; i < selectedRows.length; i++) {
            var row = selectedRows[i];
            totalBuyAmount += Number(row.dataset.buyAmount || '0');
            totalEvaluationAmountValue += Number(row.dataset.evaluationAmount || '0');
            totalEvaluationProfitValue += Number(row.dataset.evaluationProfit || '0');
            totalPrincipalValue += Number(row.dataset.principal || '0');
            totalPrincipalReturnValue += Number(row.dataset.principalReturn || '0');
        }

        var totalEvaluationAmount = Number(summary.dataset.totalEvaluationAmount || '0');
        var totalWeightValue = totalEvaluationAmount > 0
            ? (totalEvaluationAmountValue / totalEvaluationAmount) * 100
            : 0;
        var evaluationProfitRateValue = totalBuyAmount > 0
            ? (totalEvaluationProfitValue / totalBuyAmount) * 100
            : 0;
        var principalReturnRateValue = totalPrincipalValue > 0
            ? (totalPrincipalReturnValue / totalPrincipalValue) * 100
            : 0;

        var countLabel = summary.querySelector<HTMLElement>('[data-account-selection-count]');
        if (countLabel) {
            countLabel.textContent = formatAssetStatusMessage(summary.dataset.countTemplate!, selectedCount);
        }

        var accountWeightLabel = summary.querySelector<HTMLElement>('[data-account-selection-weight]');
        if (accountWeightLabel) {
            accountWeightLabel.textContent = formatAssetStatusNumber(totalWeightValue, 1) + '%';
        }

        var buyAmountLabel = summary.querySelector<HTMLElement>('[data-account-selection-buy-amount]');
        if (buyAmountLabel) {
            buyAmountLabel.textContent = formatAssetStatusNumber(totalBuyAmount, 0);
        }

        var evaluationAmountLabel = summary.querySelector<HTMLElement>('[data-account-selection-evaluation-amount]');
        if (evaluationAmountLabel) {
            evaluationAmountLabel.textContent = formatAssetStatusNumber(totalEvaluationAmountValue, 0);
        }

        var evaluationProfitLabel = summary.querySelector<HTMLElement>('[data-account-selection-evaluation-profit]');
        if (evaluationProfitLabel) {
            evaluationProfitLabel.textContent = formatAssetStatusSignedNumber(totalEvaluationProfitValue, 0);
            evaluationProfitLabel.classList.toggle('text-profit', totalEvaluationProfitValue >= 0);
            evaluationProfitLabel.classList.toggle('text-loss', totalEvaluationProfitValue < 0);
        }

        var evaluationProfitRateLabel = summary.querySelector<HTMLElement>('[data-account-selection-profit-rate]');
        if (evaluationProfitRateLabel) {
            evaluationProfitRateLabel.textContent = '(' + formatAssetStatusSignedNumber(evaluationProfitRateValue, 1) + '%)';
            evaluationProfitRateLabel.classList.toggle('text-profit', totalEvaluationProfitValue >= 0);
            evaluationProfitRateLabel.classList.toggle('text-loss', totalEvaluationProfitValue < 0);
        }

        var principalLabelEl = summary.querySelector<HTMLElement>('[data-account-selection-principal]');
        if (principalLabelEl) {
            principalLabelEl.textContent = formatAssetStatusNumber(totalPrincipalValue, 0);
        }

        var principalReturnLabelEl = summary.querySelector<HTMLElement>('[data-account-selection-principal-return]');
        if (principalReturnLabelEl) {
            principalReturnLabelEl.textContent = formatAssetStatusSignedNumber(totalPrincipalReturnValue, 0);
            principalReturnLabelEl.classList.toggle('text-profit', totalPrincipalReturnValue >= 0);
            principalReturnLabelEl.classList.toggle('text-loss', totalPrincipalReturnValue < 0);
        }

        var principalReturnRateLabel = summary.querySelector<HTMLElement>('[data-account-selection-principal-return-rate]');
        if (principalReturnRateLabel) {
            principalReturnRateLabel.textContent = '(' + formatAssetStatusSignedNumber(principalReturnRateValue, 1) + '%)';
            principalReturnRateLabel.classList.toggle('text-profit', totalPrincipalReturnValue >= 0);
            principalReturnRateLabel.classList.toggle('text-loss', totalPrincipalReturnValue < 0);
        }
    }

    function updateAssetStatusStockSelectionSummary(section: HTMLElement | null) {
        if (!section) {
            return;
        }

        var summary = section.querySelector<HTMLElement>('[data-asset-status-stock-selection-summary]');
        var table = section.querySelector<HTMLTableElement>('[data-asset-status-stock-table]');
        if (!summary || !table || !table.tBodies || table.tBodies.length === 0) {
            return;
        }

        var selectedRows = table!.tBodies[0].querySelectorAll<HTMLElement>('[data-stock-selectable-row][aria-pressed="true"]');
        var selectedCount = selectedRows.length;
        var reveal = summary.closest<HTMLElement>('.selection-reveal');
        if (reveal) {
            reveal.classList.toggle('is-open', selectedCount > 0);
        } else {
            summary.classList.toggle('hidden', selectedCount === 0);
        }

        if (selectedCount === 0) {
            return;
        }

        var totalBuyAmount = 0;
        var totalEvaluationAmountValue = 0;
        var totalEvaluationProfitValue = 0;

        for (var i = 0; i < selectedRows.length; i++) {
            var row = selectedRows[i];
            totalBuyAmount += Number(row.dataset.buyAmount || '0');
            totalEvaluationAmountValue += Number(row.dataset.evaluationAmount || '0');
            totalEvaluationProfitValue += Number(row.dataset.evaluationProfit || '0');
        }

        var totalEvaluationAmount = Number(summary.dataset.totalEvaluationAmount || '0');
        var totalWeightValue = totalEvaluationAmount > 0
            ? (totalEvaluationAmountValue / totalEvaluationAmount) * 100
            : 0;
        var profitRateValue = totalBuyAmount > 0
            ? (totalEvaluationProfitValue / totalBuyAmount) * 100
            : 0;

        var countLabel = summary.querySelector<HTMLElement>('[data-selection-count]');
        if (countLabel) {
            countLabel.textContent = formatAssetStatusMessage(summary.dataset.countTemplate!, selectedCount);
        }

        var buyAmountLabel = summary.querySelector<HTMLElement>('[data-selection-buy-amount]');
        if (buyAmountLabel) {
            buyAmountLabel.textContent = formatAssetStatusNumber(totalBuyAmount, 0);
        }

        var evaluationAmountLabel = summary.querySelector<HTMLElement>('[data-selection-evaluation-amount]');
        if (evaluationAmountLabel) {
            evaluationAmountLabel.textContent = formatAssetStatusNumber(totalEvaluationAmountValue, 0);
        }

        var evaluationProfitLabel = summary.querySelector<HTMLElement>('[data-selection-evaluation-profit]');
        if (evaluationProfitLabel) {
            evaluationProfitLabel.textContent = formatAssetStatusSignedNumber(totalEvaluationProfitValue, 0);
            evaluationProfitLabel.classList.toggle('text-profit', totalEvaluationProfitValue >= 0);
            evaluationProfitLabel.classList.toggle('text-loss', totalEvaluationProfitValue < 0);
        }

        var profitRateLabel = summary.querySelector<HTMLElement>('[data-selection-profit-rate]');
        if (profitRateLabel) {
            profitRateLabel.textContent = '(' + formatAssetStatusSignedNumber(profitRateValue, 1) + '%)';
            profitRateLabel.classList.toggle('text-profit', totalEvaluationProfitValue >= 0);
            profitRateLabel.classList.toggle('text-loss', totalEvaluationProfitValue < 0);
        }

        var weightLabel = summary.querySelector<HTMLElement>('[data-selection-weight]');
        if (weightLabel) {
            weightLabel.textContent = formatAssetStatusNumber(totalWeightValue, 1) + '%';
        }
    }

    function initializeAssetStatusStockSelection() {
        var sections = document.querySelectorAll<HTMLElement>('[data-asset-status-stock-section]');
        for (var i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section.dataset.selectionInitialized === 'true') {
                continue;
            }

            var table = section.querySelector<HTMLTableElement>('[data-asset-status-stock-table]');
            if (!table || !table.tBodies || table.tBodies.length === 0) {
                continue;
            }

            section.dataset.selectionInitialized = 'true';

            var rows = table.tBodies[0].querySelectorAll<HTMLElement>('[data-stock-selectable-row]');
            for (var j = 0; j < rows.length; j++) {
                var row = rows[j];

                row.addEventListener('click', function(this: HTMLElement, event: MouseEvent) {
                    if (event.target && (event.target as Element).closest('button, a, input, label, select, textarea')) {
                        return;
                    }

                    var isSelected = this.getAttribute('aria-pressed') === 'true';
                    setAssetStatusStockRowSelection(this, !isSelected);
                    updateAssetStatusStockSelectionSummary(section);
                });

                row.addEventListener('keydown', function(this: HTMLElement, event: KeyboardEvent) {
                    if (event.target && (event.target as Element).closest('button, a, input, label, select, textarea')) {
                        return;
                    }

                    if (event.key !== 'Enter' && event.key !== ' ') {
                        return;
                    }

                    event.preventDefault();
                    var isSelected = this.getAttribute('aria-pressed') === 'true';
                    setAssetStatusStockRowSelection(this, !isSelected);
                    updateAssetStatusStockSelectionSummary(section);
                });

            }

            var clearButton = section.querySelector<HTMLElement>('[data-selection-clear]');
            if (clearButton) {
                clearButton.addEventListener('click', function(this: HTMLElement) {
                    var selectedRows = table!.tBodies[0].querySelectorAll<HTMLElement>('[data-stock-selectable-row][aria-pressed="true"]');
                    for (var k = 0; k < selectedRows.length; k++) {
                        setAssetStatusStockRowSelection(selectedRows[k], false);
                    }

                    updateAssetStatusStockSelectionSummary(section);
                });
            }

            updateAssetStatusStockSelectionSummary(section);
        }
    }

    function initializeAssetStatusAccountSelection() {
        var sections = document.querySelectorAll<HTMLElement>('[data-asset-status-account-section]');
        for (var i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section.dataset.accountSelectionInitialized === 'true') {
                continue;
            }

            var table = section.querySelector<HTMLTableElement>('[data-asset-status-account-table]');
            if (!table || !table.tBodies || table.tBodies.length === 0) {
                continue;
            }

            section.dataset.accountSelectionInitialized = 'true';

            var rows = table.tBodies[0].querySelectorAll<HTMLElement>('[data-account-selectable-row]');
            for (var j = 0; j < rows.length; j++) {
                var row = rows[j];

                row.addEventListener('click', function(this: HTMLElement, event: MouseEvent) {
                    if (event.target && (event.target as Element).closest('button, a, input, label, select, textarea')) {
                        return;
                    }

                    var isSelected = this.dataset.selected === 'true';
                    setAssetStatusAccountRowSelection(this, !isSelected);
                    updateAssetStatusAccountSelectionSummary(section);
                });

                // 같은 화면의 종목 표는 Enter/Space 로 선택되는데 계좌 표는 클릭 전용이었다
                // (실측: 계좌 행은 focus() 조차 걸리지 않아 키보드로는 선택 자체가 불가능).
                row.addEventListener('keydown', function(this: HTMLElement, event: KeyboardEvent) {
                    if (event.target && (event.target as Element).closest('button, a, input, label, select, textarea')) {
                        return;
                    }

                    if (event.key !== 'Enter' && event.key !== ' ') {
                        return;
                    }

                    event.preventDefault();
                    var isSelected = this.dataset.selected === 'true';
                    setAssetStatusAccountRowSelection(this, !isSelected);
                    updateAssetStatusAccountSelectionSummary(section);
                });

            }

            var clearButton = section.querySelector<HTMLElement>('[data-account-selection-clear]');
            if (clearButton) {
                clearButton.addEventListener('click', function(this: HTMLElement) {
                    var selectedRows = table!.tBodies[0].querySelectorAll<HTMLElement>('[data-account-selectable-row][data-selected="true"]');
                    for (var k = 0; k < selectedRows.length; k++) {
                        setAssetStatusAccountRowSelection(selectedRows[k], false);
                    }

                    updateAssetStatusAccountSelectionSummary(section);
                });
            }

            updateAssetStatusAccountSelectionSummary(section);
        }
    }

    if (typeof win.initializeStockTagHoverTooltips !== 'function') {
        win.initializeStockTagHoverTooltips = function(root: ParentNode | null) {
            var tooltip = win.stockTagHoverTooltip;
            if (!tooltip) {
                var tooltipEl = document.createElement('div');
                tooltipEl.id = 'stockTagHoverTooltip';
                tooltipEl.className = 'pointer-events-none fixed left-0 top-0 z-50 hidden w-64 max-w-[calc(100vw-2rem)] rounded-box bg-neutral px-3 py-2 text-left text-xs leading-5 text-neutral-content shadow-lg sm:w-72';
                document.body.appendChild(tooltipEl);

                tooltip = {
                    element: tooltipEl,
                    show: function(message: string, clientX: number, clientY: number) {
                        if (!message) {
                            this.hide();
                            return;
                        }

                        this.element.textContent = message;
                        this.element.classList.remove('hidden');
                        this.move(clientX, clientY);
                    },
                    move: function(clientX: number, clientY: number) {
                        if (this.element.classList.contains('hidden')) {
                            return;
                        }

                        var offsetX = 12;
                        var offsetY = 18;
                        var viewportPadding = 12;
                        var left = clientX + offsetX;
                        var top = clientY + offsetY;
                        var rect = this.element.getBoundingClientRect();

                        if (left + rect.width + viewportPadding > win.innerWidth) {
                            left = Math.max(viewportPadding, win.innerWidth - rect.width - viewportPadding);
                        }

                        if (top + rect.height + viewportPadding > win.innerHeight) {
                            top = Math.max(viewportPadding, clientY - rect.height - offsetY);
                        }

                        this.element.style.left = left + 'px';
                        this.element.style.top = top + 'px';
                    },
                    hide: function() {
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

            var scope = root || document;
            var targets = scope.querySelectorAll<HTMLElement>('[data-stock-tag-tooltip]');
            for (var i = 0; i < targets.length; i++) {
                var target = targets[i];
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

                target.addEventListener('mouseleave', function(this: HTMLElement) {
                    tooltip.hide();
                });
            }
        };
    }

    function initializeAssetStatus() {
        initializeAssetStatusDetailToggles();
        initializeAssetStatusSortableTables();
        initializeAssetStatusAccountSelection();
        initializeAssetStatusStockSelection();
        win.initializeStockTagHoverTooltips(document);
    }

    win.initializeAssetStatus = initializeAssetStatus;
    document.addEventListener("htmx:afterSettle", initializeAssetStatus);
    initializeAssetStatus();
})();
