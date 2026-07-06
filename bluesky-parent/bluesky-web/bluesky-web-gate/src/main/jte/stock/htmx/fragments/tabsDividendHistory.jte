@import java.util.List
@import java.util.UUID
@import java.time.Instant
@import java.time.ZoneId
@import java.time.LocalDate
@import java.math.BigDecimal
@import java.math.RoundingMode
@import net.luversof.web.gate.stock.dto.response.DividendView
@import net.luversof.web.common.menu.domain.Pagination
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView
@import net.luversof.web.gate.stock.controller.StockDividendHtmxController.DividendChange
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import io.github.luversof.boot.context.support.MessageUtil

@param List<DividendView> dividendList
@param List<DividendView> allDividendList = java.util.List.of()
@param Pagination pagination
@param int totalItems
@param int totalPages
@param int currentPage
@param int size
@param List<Account> accountList
@param List<StockItem> stockItemList
@param List<String> stockTagList = java.util.List.of()
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param List<UUID> selectedAccountIds = java.util.List.of()
@param List<UUID> selectedStockItemIds = java.util.List.of()
@param List<String> selectedStockTags = java.util.List.of()
@param java.time.Instant startDate
@param java.time.Instant endDate
@param String timeZone = null
@param String sort
@param BigDecimal totalGrossAmount
@param BigDecimal totalNetAmount
@param BigDecimal totalTax
@param BigDecimal totalTaxableAmount
@param BigDecimal totalAllGrossAmount = BigDecimal.ZERO
@param BigDecimal totalAllNetAmount = BigDecimal.ZERO
@param BigDecimal totalAllTaxableAmount = BigDecimal.ZERO
@param BigDecimal totalAllTax = BigDecimal.ZERO
@param BigDecimal prevPeriodNetAmount = null
@param LocalDate prevStartDate = null
@param LocalDate prevEndDate = null
@param List<DividendChange> dividendChangeContributors = java.util.List.of()
@param BigDecimal portfolioYieldOnDailyAverageCostPct = null
@param BigDecimal portfolioYieldOnCostPct = null
@param BigDecimal portfolioYieldOnMarketPct = null
@param DividendYieldGroupView bestYieldStock = null
@param DividendYieldGroupView bestYieldAccount = null
@param List<DividendYieldGroupView> stockYieldRows = java.util.List.of()
@param java.util.Map<String, UUID> yieldStockIdByName = java.util.Map.of()
@param java.util.Map<String, UUID> yieldAccountIdByName = java.util.Map.of()
@param List<DividendYieldGroupView> accountYieldRows = java.util.List.of()
@param List<DividendYieldGroupView> yearlyYieldRows = java.util.List.of()
@param String rangeMode = null
@param String dataFirstDate = null

!{
    DecimalFormat decimalFormat = new DecimalFormat("#,##0");

    // Safe nulls
    BigDecimal safeAllNet = totalAllNetAmount != null ? totalAllNetAmount : BigDecimal.ZERO;
    BigDecimal safeAllGross = totalAllGrossAmount != null ? totalAllGrossAmount : BigDecimal.ZERO;
    BigDecimal safeAllTaxable = totalAllTaxableAmount != null ? totalAllTaxableAmount : BigDecimal.ZERO;
    BigDecimal safeAllTax = totalAllTax != null ? totalAllTax : BigDecimal.ZERO;
    // 세전 − 세후 = 총 차감액(세금 + 수수료·기타). 수수료·기타 = 차감액 − 세금.
    BigDecimal allDeduction = safeAllGross.subtract(safeAllNet);
    BigDecimal allOtherDeduction = allDeduction.subtract(safeAllTax);
    BigDecimal safePrevNet = prevPeriodNetAmount != null ? prevPeriodNetAmount : BigDecimal.ZERO;
    boolean hasComparison = prevPeriodNetAmount != null;
    BigDecimal diffAmount = hasComparison ? safeAllNet.subtract(safePrevNet) : BigDecimal.ZERO;
    boolean diffPositive = diffAmount.compareTo(BigDecimal.ZERO) >= 0;
    String diffPct = "";
    if (hasComparison && safePrevNet.compareTo(BigDecimal.ZERO) != 0) {
        diffPct = safeAllNet.subtract(safePrevNet)
            .multiply(new BigDecimal("100"))
            .divide(safePrevNet, 1, RoundingMode.HALF_UP)
            .toPlainString();
    } else if (hasComparison) {
        diffPct = "∞";
    }

    // Build JSON for charts
    java.util.function.Function<String, String> ej = s ->
        s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    StringBuilder jsSb = new StringBuilder();
    jsSb.append("const dividendData=[");
    List<DividendView> chartList = allDividendList != null ? allDividendList : java.util.List.of();
    for (int ci = 0; ci < chartList.size(); ci++) {
        DividendView d = chartList.get(ci);
        if (ci > 0) jsSb.append(",");
        java.time.ZoneId _zone = (timeZone != null && !timeZone.isEmpty()) ? java.time.ZoneId.of(timeZone) : java.time.ZoneId.systemDefault();
        jsSb.append("{payDate:\"").append(d.payDate() != null ? ymd.format(d.payDate().atZone(_zone)) : "").append("\"");
        jsSb.append(",stockItem:\"").append(ej.apply(d.stockItemName())).append("\"");
        jsSb.append(",account:\"").append(ej.apply(d.accountName())).append("\"");
        jsSb.append(",net:").append(d.netAmount() != null ? d.netAmount().toPlainString() : "0");
        jsSb.append(",gross:").append(d.grossAmount() != null ? d.grossAmount().toPlainString() : "0");
        jsSb.append("}");
    }
    jsSb.append("];");
    String dividendDataJs = jsSb.toString();

    // Nav 버튼 활성화 여부 (use client timezone if available)
    boolean isAllMode = "all".equals(rangeMode);
    java.time.ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? java.time.ZoneId.of(timeZone) : java.time.ZoneId.systemDefault();
    java.time.LocalDate today = java.time.LocalDate.now(zone);
    java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endLocalInclusive = endLocal != null ? endLocal.minusDays(1) : null;
    boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
    boolean canPrev = !isAllMode && startLocal != null
        && dataFirstDate != null && !dataFirstDate.isEmpty()
        && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));

    java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);
    String totalItemsCountText = countMessage.apply(totalItems);
    String detailSummaryLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.detail.summary"), countMessage.apply(totalItems));

    // 여러 섹션이 공유하는 라벨 (하위 컴포넌트에 param 으로 전달)
    String accountLabel = MessageUtil.getMessage("common.label.account");
    String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
    String tagLabel = MessageUtil.getMessage("common.label.tag");
    String grossAmountLabel = MessageUtil.getMessage("stock.dividend.gross");
    String netAmountLabel = MessageUtil.getMessage("stock.dividend.net");
    String taxLabel = MessageUtil.getMessage("stock.dividend.tax");
    String taxableAmountLabel = MessageUtil.getMessage("stock.dividend.taxable.amount");
    String totalLabel = MessageUtil.getMessage("common.label.total");
    String noDataLabel = MessageUtil.getMessage("common.message.no.data");

    // 하단 인라인 스크립트에서 사용하는 라벨
    String othersLabel = MessageUtil.getMessage("stock.chart.others");
    String noPeriodHistoryLabel = MessageUtil.getMessage("stock.dividend.message.no.period.history");
    String averageNoDataLabel = MessageUtil.getMessage("stock.dividend.summary.no.data");
    String averageDescTemplate = MessageUtil.getMessage("stock.dividend.summary.average.desc");
    String countPattern = MessageUtil.getMessage("common.count");
    java.util.function.Function<BigDecimal, String> percentFormat = value -> value != null ? String.format("%.2f%%", value.doubleValue()) : "-";
    java.util.function.Function<Long, String> countFormat = count -> java.text.MessageFormat.format(countPattern, count);
}

<div id="dividendListFragment">

    @template.stock.htmx.fragments.dateRangeNavBar(
        pickerName = "dividendPicker",
        btnClass = "date-range-btn",
        canPrev = canPrev,
        canNext = canNext,
        rangeMode = rangeMode,
        startDate = startLocal,
        endDate = endLocal
    )

    @template.stock.htmx.fragments.dividend.dividendSearchForm(
        accountList = accountList,
        stockItemList = stockItemList,
        stockTagList = stockTagList,
        selectedAccountId = selectedAccountId,
        selectedStockItemId = selectedStockItemId,
        selectedAccountIds = selectedAccountIds,
        selectedStockItemIds = selectedStockItemIds,
        selectedStockTags = selectedStockTags,
        startDate = startDate,
        endDate = endDate,
        timeZone = timeZone,
        rangeMode = rangeMode,
        zone = zone,
        accountLabel = accountLabel,
        stockNameLabel = stockNameLabel,
        tagLabel = tagLabel
    )

    <!-- Dashboard -->

    @template.stock.htmx.fragments.dividend.dividendSummaryCards(
        decimalFormat = decimalFormat,
        safeAllNet = safeAllNet,
        safeAllGross = safeAllGross,
        safeAllTaxable = safeAllTaxable,
        safeAllTax = safeAllTax,
        allDeduction = allDeduction,
        allOtherDeduction = allOtherDeduction,
        safePrevNet = safePrevNet,
        hasComparison = hasComparison,
        diffPositive = diffPositive,
        diffPct = diffPct,
        diffAmount = diffAmount,
        totalItemsCountText = totalItemsCountText,
        prevStartDate = prevStartDate,
        prevEndDate = prevEndDate,
        dividendChangeContributors = dividendChangeContributors,
        taxLabel = taxLabel,
        taxableAmountLabel = taxableAmountLabel
    )

    @template.stock.htmx.fragments.dividend.dividendCharts()

    @template.stock.htmx.fragments.dividend.dividendYieldAnalytics(
        decimalFormat = decimalFormat,
        percentFormat = percentFormat,
        countFormat = countFormat,
        safeAllNet = safeAllNet,
        portfolioYieldOnDailyAverageCostPct = portfolioYieldOnDailyAverageCostPct,
        portfolioYieldOnCostPct = portfolioYieldOnCostPct,
        portfolioYieldOnMarketPct = portfolioYieldOnMarketPct,
        bestYieldStock = bestYieldStock,
        bestYieldAccount = bestYieldAccount,
        yearlyYieldRows = yearlyYieldRows,
        stockYieldRows = stockYieldRows,
        accountYieldRows = accountYieldRows,
        yieldStockIdByName = yieldStockIdByName,
        yieldAccountIdByName = yieldAccountIdByName,
        stockItemList = stockItemList,
        accountLabel = accountLabel,
        stockNameLabel = stockNameLabel,
        tagLabel = tagLabel,
        noDataLabel = noDataLabel,
        grossAmountLabel = grossAmountLabel,
        netAmountLabel = netAmountLabel,
        taxLabel = taxLabel,
        taxableAmountLabel = taxableAmountLabel,
        totalLabel = totalLabel
    )

    @template.stock.htmx.fragments.dividend.dividendTable(
        dividendList = dividendList,
        pagination = pagination,
        sort = sort,
        decimalFormat = decimalFormat,
        percentFormat = percentFormat,
        detailSummaryLabel = detailSummaryLabel,
        totalGrossAmount = totalGrossAmount,
        totalNetAmount = totalNetAmount,
        totalTax = totalTax,
        totalTaxableAmount = totalTaxableAmount,
        accountLabel = accountLabel,
        stockNameLabel = stockNameLabel,
        grossAmountLabel = grossAmountLabel,
        netAmountLabel = netAmountLabel,
        taxLabel = taxLabel,
        taxableAmountLabel = taxableAmountLabel,
        totalLabel = totalLabel
    )

    <script>
    (function() {
        $unsafe{dividendDataJs}
        const filterStartDate = "${startLocal != null ? startLocal.format(DateTimeFormatter.ofPattern("yyyy-MM")) : ""}";
        const filterEndDate = "${endLocalInclusive != null ? endLocalInclusive.format(DateTimeFormatter.ofPattern("yyyy-MM")) : ""}";
        const countPattern = "${countPattern}";
        const noDataLabel = "${noDataLabel}";
        const noPeriodHistoryLabel = "${noPeriodHistoryLabel}";
        const averageNoDataLabel = "${averageNoDataLabel}";
        const averageDescTemplate = "${averageDescTemplate}";
        const netAmountLabel = "${netAmountLabel}";
        const othersLabel = "${othersLabel}";

        const CHART_COLORS = [
            'rgba(99,102,241,0.8)','rgba(34,197,94,0.8)','rgba(251,191,36,0.8)',
            'rgba(239,68,68,0.8)','rgba(59,130,246,0.8)','rgba(236,72,153,0.8)',
            'rgba(14,165,233,0.8)','rgba(249,115,22,0.8)','rgba(168,85,247,0.8)',
            'rgba(20,184,166,0.8)','rgba(245,158,11,0.8)','rgba(16,185,129,0.8)'
        ];

        function formatNumber(value) {
            return window.StockCharts ? window.StockCharts.formatNumber(value) : new Intl.NumberFormat().format(value);
        }

        function formatCurrency(value) {
            return window.StockCharts ? window.StockCharts.formatCurrency(value) : ('₩' + Math.round(value).toLocaleString());
        }

        function formatCount(value) {
            return countPattern.replace('{0}', formatNumber(value));
        }

        function formatFixedNumber(value, fractionDigits) {
            return new Intl.NumberFormat(undefined, {
                minimumFractionDigits: fractionDigits,
                maximumFractionDigits: fractionDigits
            }).format(value);
        }

        function setDividendYieldRowSelection(row, selected) {
            if (!row) {
                return;
            }

            const selectionClass = row.dataset.selectionClass || 'dividend-yield-row-selected';
            row.classList.toggle(selectionClass, selected);
            row.setAttribute('aria-pressed', selected ? 'true' : 'false');

            const checkbox = row.querySelector('[data-dividend-yield-selection-toggle]');
            if (checkbox) {
                checkbox.checked = selected;
            }
        }

        function updateDividendYieldSelectionSummary(section) {
            if (!section) {
                return;
            }

            const summary = section.querySelector('[data-dividend-yield-selection-summary]');
            const table = section.querySelector('[data-dividend-yield-selection-table]');
            if (!summary || !table || !table.tBodies || table.tBodies.length === 0) {
                return;
            }

            const selectedRows = table.tBodies[0].querySelectorAll('[data-dividend-yield-row][aria-pressed="true"]');
            const selectedCount = selectedRows.length;
            const reveal = summary.closest('.selection-reveal');
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

            for (let i = 0; i < selectedRows.length; i++) {
                const row = selectedRows[i];
                totalGrossAmount += Number(row.dataset.grossAmount || '0');
                totalNetAmount += Number(row.dataset.netAmount || '0');
                totalTaxAmount += Number(row.dataset.taxAmount || '0');
                totalTaxableAmount += Number(row.dataset.taxableAmount || '0');
                totalDailyPrincipalCost += Number(row.dataset.averageDailyPrincipalCost || '0');
                totalAveragePrincipalCost += Number(row.dataset.averagePrincipalCost || '0');
            }

            const totalNetReference = Number(summary.dataset.totalNetAmount || '0');
            const selectionWeight = totalNetReference > 0
                ? (totalNetAmount / totalNetReference) * 100
                : 0;
            const yieldOnDailyAverageCost = totalDailyPrincipalCost > 0
                ? (totalNetAmount / totalDailyPrincipalCost) * 100
                : 0;
            const yieldOnBasisAverageCost = totalAveragePrincipalCost > 0
                ? (totalNetAmount / totalAveragePrincipalCost) * 100
                : 0;

            const countLabel = summary.querySelector('[data-dividend-yield-selection-count]');
            if (countLabel) {
                countLabel.textContent = summary.dataset.countTemplate.replace('{0}', formatNumber(selectedCount));
            }

            const weightBadge = summary.querySelector('[data-dividend-yield-selection-weight-badge]');
            if (weightBadge) {
                weightBadge.textContent = summary.dataset.weightLabel + ' ' + formatFixedNumber(selectionWeight, 1) + '%';
            }

            const grossLabel = summary.querySelector('[data-dividend-yield-selection-gross]');
            if (grossLabel) {
                grossLabel.textContent = formatFixedNumber(totalGrossAmount, 0);
            }

            const netLabel = summary.querySelector('[data-dividend-yield-selection-net]');
            if (netLabel) {
                netLabel.textContent = formatFixedNumber(totalNetAmount, 0);
            }

            const taxLabelEl = summary.querySelector('[data-dividend-yield-selection-tax]');
            if (taxLabelEl) {
                taxLabelEl.textContent = formatFixedNumber(totalTaxAmount, 0);
            }

            const taxableLabelEl = summary.querySelector('[data-dividend-yield-selection-taxable]');
            if (taxableLabelEl) {
                taxableLabelEl.textContent = formatFixedNumber(totalTaxableAmount, 0);
            }

            const dailyCapitalLabel = summary.querySelector('[data-dividend-yield-selection-daily-capital]');
            if (dailyCapitalLabel) {
                dailyCapitalLabel.textContent = formatFixedNumber(totalDailyPrincipalCost, 0);
            }

            const yieldLabel = summary.querySelector('[data-dividend-yield-selection-yield]');
            if (yieldLabel) {
                yieldLabel.textContent = formatFixedNumber(yieldOnDailyAverageCost, 2) + '%';
            }

            const basisYieldLabel = summary.querySelector('[data-dividend-yield-selection-basis-yield]');
            if (basisYieldLabel) {
                basisYieldLabel.textContent = summary.dataset.basisLabel + ' ' + formatFixedNumber(yieldOnBasisAverageCost, 2) + '%';
            }
        }

        function getDividendYieldSortValue(row, sortKey, sortType) {
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

        function compareDividendYieldSortValues(leftValue, rightValue, sortType) {
            if (sortType === 'number') {
                return leftValue - rightValue;
            }

            return String(leftValue).localeCompare(String(rightValue), 'ko', {
                numeric: true,
                sensitivity: 'base'
            });
        }

        function updateDividendYieldSortIndicators(table) {
            if (!table) {
                return;
            }

            const activeKey = table.dataset.sortKey;
            const direction = table.dataset.sortDirection;
            const buttons = table.querySelectorAll('[data-sort-key]');
            for (let i = 0; i < buttons.length; i++) {
                const button = buttons[i];
                const indicator = button.querySelector('[data-sort-indicator]');
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

        function sortDividendYieldTable(table, sortKey, sortType) {
            if (!table || !sortKey || !sortType || !table.tBodies || table.tBodies.length === 0) {
                return;
            }

            let nextDirection;
            if (table.dataset.sortKey === sortKey) {
                nextDirection = table.dataset.sortDirection === 'asc' ? 'desc' : 'asc';
            } else {
                nextDirection = sortType === 'text' ? 'asc' : 'desc';
            }

            table.dataset.sortKey = sortKey;
            table.dataset.sortDirection = nextDirection;

            const tbody = table.tBodies[0];
            const rows = Array.from(tbody.querySelectorAll('[data-dividend-yield-row]'));
            rows.sort(function(left, right) {
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
            const tables = document.querySelectorAll('[data-dividend-yield-sortable-table]');
            for (let i = 0; i < tables.length; i++) {
                const table = tables[i];
                if (table.dataset.sortableInitialized === 'true') {
                    continue;
                }

                table.dataset.sortableInitialized = 'true';

                const buttons = table.querySelectorAll('[data-sort-key]');
                for (let j = 0; j < buttons.length; j++) {
                    buttons[j].addEventListener('click', function() {
                        const targetTable = this.closest('[data-dividend-yield-sortable-table]');
                        sortDividendYieldTable(targetTable, this.dataset.sortKey, this.dataset.sortType);
                    });
                }

                updateDividendYieldSortIndicators(table);
            }
        }

        function initializeDividendYieldSelection() {
            const sections = document.querySelectorAll('[data-dividend-yield-selection-section]');
            for (let i = 0; i < sections.length; i++) {
                const section = sections[i];
                if (section.dataset.selectionInitialized === 'true') {
                    continue;
                }

                const table = section.querySelector('[data-dividend-yield-selection-table]');
                if (!table || !table.tBodies || table.tBodies.length === 0) {
                    continue;
                }

                section.dataset.selectionInitialized = 'true';

                const rows = table.tBodies[0].querySelectorAll('[data-dividend-yield-row]');
                for (let j = 0; j < rows.length; j++) {
                    const row = rows[j];

                    row.addEventListener('click', function(event) {
                        if (event.target && event.target.closest('button, a, input, label, select, textarea')) {
                            return;
                        }

                        const isSelected = this.getAttribute('aria-pressed') === 'true';
                        setDividendYieldRowSelection(this, !isSelected);
                        updateDividendYieldSelectionSummary(section);
                    });

                    row.addEventListener('keydown', function(event) {
                        if (event.target && event.target.closest('button, a, input, label, select, textarea')) {
                            return;
                        }

                        if (event.key !== 'Enter' && event.key !== ' ') {
                            return;
                        }

                        event.preventDefault();
                        const isSelected = this.getAttribute('aria-pressed') === 'true';
                        setDividendYieldRowSelection(this, !isSelected);
                        updateDividendYieldSelectionSummary(section);
                    });

                    const checkbox = row.querySelector('[data-dividend-yield-selection-toggle]');
                    if (checkbox) {
                        checkbox.addEventListener('click', function(event) {
                            event.stopPropagation();
                        });

                        checkbox.addEventListener('change', function(event) {
                            event.stopPropagation();
                            const targetRow = this.closest('[data-dividend-yield-row]');
                            setDividendYieldRowSelection(targetRow, this.checked);
                            updateDividendYieldSelectionSummary(section);
                        });
                    }
                }

                const clearButton = section.querySelector('[data-dividend-yield-selection-clear]');
                if (clearButton) {
                    clearButton.addEventListener('click', function() {
                        const selectedRows = table.tBodies[0].querySelectorAll('[data-dividend-yield-row][aria-pressed="true"]');
                        for (let k = 0; k < selectedRows.length; k++) {
                            setDividendYieldRowSelection(selectedRows[k], false);
                        }

                        updateDividendYieldSelectionSummary(section);
                    });
                }

                updateDividendYieldSelectionSummary(section);
            }
        }

        if (typeof window.initializeStockTagHoverTooltips !== 'function') {
            window.initializeStockTagHoverTooltips = function(root) {
                let tooltip = window.stockTagHoverTooltip;
                if (!tooltip) {
                    const tooltipEl = document.createElement('div');
                    tooltipEl.id = 'stockTagHoverTooltip';
                    tooltipEl.className = 'pointer-events-none fixed left-0 top-0 z-50 hidden w-64 max-w-[calc(100vw-2rem)] rounded-box bg-neutral px-3 py-2 text-left text-xs leading-5 text-neutral-content shadow-lg sm:w-72';
                    document.body.appendChild(tooltipEl);

                    tooltip = {
                        element: tooltipEl,
                        show(message, clientX, clientY) {
                            if (!message) {
                                this.hide();
                                return;
                            }

                            this.element.textContent = message;
                            this.element.classList.remove('hidden');
                            this.move(clientX, clientY);
                        },
                        move(clientX, clientY) {
                            if (this.element.classList.contains('hidden')) {
                                return;
                            }

                            const offsetX = 12;
                            const offsetY = 18;
                            const viewportPadding = 12;
                            let left = clientX + offsetX;
                            let top = clientY + offsetY;
                            const rect = this.element.getBoundingClientRect();

                            if (left + rect.width + viewportPadding > window.innerWidth) {
                                left = Math.max(viewportPadding, window.innerWidth - rect.width - viewportPadding);
                            }

                            if (top + rect.height + viewportPadding > window.innerHeight) {
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

                    window.stockTagHoverTooltip = tooltip;
                    window.addEventListener('scroll', function() {
                        if (window.stockTagHoverTooltip) {
                            window.stockTagHoverTooltip.hide();
                        }
                    }, true);
                    window.addEventListener('resize', function() {
                        if (window.stockTagHoverTooltip) {
                            window.stockTagHoverTooltip.hide();
                        }
                    });
                }

                const scope = root || document;
                const targets = scope.querySelectorAll('[data-stock-tag-tooltip]');
                for (let i = 0; i < targets.length; i++) {
                    const target = targets[i];
                    if (target.dataset.stockTagTooltipInitialized === 'true') {
                        continue;
                    }

                    target.dataset.stockTagTooltipInitialized = 'true';

                    target.addEventListener('mouseenter', function(event) {
                        tooltip.show(this.dataset.stockTagTooltip, event.clientX, event.clientY);
                    });

                    target.addEventListener('mousemove', function(event) {
                        tooltip.move(event.clientX, event.clientY);
                    });

                    target.addEventListener('mouseleave', function() {
                        tooltip.hide();
                    });
                }
            };
        }

        let monthlyChart = null;
        let donutChart = null;

        function buildMonthlyData() {
            const map = {};
            if (filterStartDate && filterEndDate) {
                let cur = filterStartDate;
                while (cur <= filterEndDate) {
                    map[cur] = 0;
                    const parts = cur.split('-');
                    const y = Number(parts[0]), mo = Number(parts[1]);
                    cur = mo === 12 ? (y + 1) + '-01' : y + '-' + String(mo + 1).padStart(2, '0');
                }
            }
            dividendData.forEach(d => {
                if (!d.payDate) return;
                const mon = d.payDate.slice(0, 7);
                map[mon] = (map[mon] || 0) + Number(d.net);
            });
            const months = Object.keys(map).sort();
            return { labels: months, data: months.map(m => map[m]) };
        }

        // 월별 × 종목 누적: 어떤 종목이 각 달의 증감을 만들었는지 한눈에. 상위 N개 + 나머지는 '기타'.
        function buildMonthlyStackedData() {
            const monthSet = {};
            if (filterStartDate && filterEndDate) {
                let cur = filterStartDate;
                while (cur <= filterEndDate) {
                    monthSet[cur] = true;
                    const parts = cur.split('-');
                    const y = Number(parts[0]), mo = Number(parts[1]);
                    cur = mo === 12 ? (y + 1) + '-01' : y + '-' + String(mo + 1).padStart(2, '0');
                }
            }
            dividendData.forEach(d => { if (d.payDate) monthSet[d.payDate.slice(0, 7)] = true; });
            const months = Object.keys(monthSet).sort();
            const monthIndex = {};
            months.forEach((m, i) => { monthIndex[m] = i; });

            const stockTotal = {};
            dividendData.forEach(d => { stockTotal[d.stockItem] = (stockTotal[d.stockItem] || 0) + Number(d.net); });
            const ranked = Object.keys(stockTotal).sort((a, b) => stockTotal[b] - stockTotal[a]);
            const TOP = 8;
            const topStocks = ranked.slice(0, TOP);
            const isTop = {};
            topStocks.forEach(s => { isTop[s] = true; });
            const hasOthers = ranked.length > TOP;

            const seriesMap = {};
            topStocks.forEach(s => { seriesMap[s] = months.map(() => 0); });
            const othersSeries = hasOthers ? months.map(() => 0) : null;
            dividendData.forEach(d => {
                if (!d.payDate) return;
                const mi = monthIndex[d.payDate.slice(0, 7)];
                if (mi == null) return;
                const amt = Number(d.net);
                if (isTop[d.stockItem]) seriesMap[d.stockItem][mi] += amt;
                else if (othersSeries) othersSeries[mi] += amt;
            });

            const datasets = topStocks.map((s, i) => ({
                label: s,
                data: seriesMap[s],
                backgroundColor: CHART_COLORS[i % CHART_COLORS.length],
                borderWidth: 0,
                borderRadius: 2
            }));
            if (othersSeries) {
                datasets.push({
                    label: othersLabel,
                    data: othersSeries,
                    backgroundColor: 'rgba(148,163,184,0.6)',
                    borderWidth: 0,
                    borderRadius: 2
                });
            }
            return { labels: months, datasets: datasets };
        }

        function buildDonutData(groupBy) {
            const map = {};
            dividendData.forEach(d => {
                const key = groupBy === 'stock' ? d.stockItem : d.account;
                map[key] = (map[key] || 0) + Number(d.net);
            });
            const sorted = Object.entries(map).sort((a, b) => b[1] - a[1]);
            return { labels: sorted.map(e => e[0]), data: sorted.map(e => e[1]) };
        }

        function initMonthlyChart() {
            const m = buildMonthlyStackedData();
            const gridColor = 'rgba(128,128,128,0.1)';
            const canvasId = 'monthlyBarChart';
            const canvasEl = document.getElementById(canvasId);
            if (!canvasEl) return;
            if (monthlyChart) try { monthlyChart.destroy(); } catch(e) {}
            const fmtFull = v => '₩' + Math.round(v).toLocaleString();
            var config = {
                type: 'bar',
                data: { labels: m.labels, datasets: m.datasets },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    layout: { padding: { right: 8, bottom: 4 } },
                    // 한 달의 모든 종목 구간 + 합계를 함께 보여줌(증감 원인 파악용)
                    interaction: { mode: 'index', intersect: false },
                    plugins: {
                        legend: { display: true, position: 'bottom', labels: { font: { size: 10 }, boxWidth: 10, boxHeight: 10, padding: 8, usePointStyle: true } },
                        tooltip: {
                            itemSort: (a, b) => b.parsed.y - a.parsed.y,
                            callbacks: {
                                label: ctx => ctx.parsed.y ? (ctx.dataset.label + ': ' + fmtFull(ctx.parsed.y)) : null,
                                footer: items => {
                                    let sum = 0; items.forEach(it => { sum += it.parsed.y; });
                                    return netAmountLabel + ': ' + fmtFull(sum);
                                }
                            }
                        }
                    },
                    scales: {
                        x: { stacked: true, grid: { color: gridColor }, ticks: { maxRotation: 45, font: { size: 11 } } },
                        y: { stacked: true, beginAtZero: true, grid: { color: gridColor }, ticks: { callback: v => window.StockCharts ? window.StockCharts.formatCompactNumber(v) : Math.round(v).toLocaleString(), font: { size: 11 } } }
                    }
                }
            };
            window.ensureStockCharts(function() { monthlyChart = window.StockCharts.createChart(canvasId, config, monthlyChart); });
        }

        function initDonutChart(mode) {
            const d = buildDonutData(mode);
            const canvasId = 'donutChart';
            const canvasEl = document.getElementById(canvasId);
            if (!canvasEl) return;
            if (donutChart) try { donutChart.destroy(); } catch(e) {}
            const legend = document.getElementById('donutLegend');
            if (d.labels.length === 0) {
                canvasEl.style.display = 'none';
                if (legend) legend.innerHTML = '<div class="text-xs opacity-40 pt-4 text-center">' + noPeriodHistoryLabel + '</div>';
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
                        backgroundColor: d.labels.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]),
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: { callbacks: { label: ctx => ' ' + formatCurrency(Math.round(ctx.parsed)) } }
                    },
                    cutout: '62%'
                }
            };
            window.ensureStockCharts(function() { donutChart = window.StockCharts.createChart(canvasId, config, donutChart); });
            const total = d.data.reduce((s, v) => s + v, 0);
            if (legend) {
                legend.innerHTML = d.labels.map((label, i) => {
                    const pct = total > 0 ? ((d.data[i] / total) * 100).toFixed(1) : '0.0';
                    const color = CHART_COLORS[i % CHART_COLORS.length];
                    return '<div class="flex items-center gap-1 mb-0.5">'
                        + '<span style="flex-shrink:0;display:inline-block;width:8px;height:8px;border-radius:50%;background:' + color + '"></span>'
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
            const totalNet = dividendData.reduce((s, d) => s + Number(d.net), 0);
            const monthCount = buildMonthlyData().labels.length || 1;
            const avgPerMonth = totalNet / monthCount;
            displayEl.textContent = formatCurrency(Math.round(avgPerMonth));
            if (descEl) {
                descEl.innerHTML = averageDescTemplate
                    .replace('{0}', formatNumber(monthCount))
                    .replace('{1}', formatCurrency(Math.round(totalNet)))
                    .replace('{2}', formatCount(dividendData.length));
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

        window.switchDonut = function(mode) {
            document.getElementById('donutTabStock').classList.toggle('tab-active', mode === 'stock');
            document.getElementById('donutTabAccount').classList.toggle('tab-active', mode !== 'stock');
            initDonutChart(mode);
        };

        window.dividendPicker = window.DateRangePicker
            ? window.DateRangePicker.create({
                formId:      'dividendSearchForm',
                startId:     'startDateInput',
                endId:       'endDateInput',
                instantStartId: 'dividendStartInstantInput',
                instantEndId:   'dividendEndInstantInput',
                timeZoneId:     'dividendTimeZoneInput',
                rangeModeId: 'dividendRangeModeInput',
                btnClass:    'date-range-btn',
                minDate:     '${dataFirstDate != null ? dataFirstDate : ""}',
                globalKey:   'globalDateRange',
                rootSelector: '#dividendListFragment'
            })
            : { set: function(){}, shift: function(){}, jumpToEdge: function(){} };

        // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
        try {
            if (window.dividendPicker && typeof window.dividendPicker.getState === 'function') {
                var __s = window.dividendPicker.getState();
                if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
                    var __key = 'dateRangeDefaultApplied:dividendPicker';
                    try {
                        if (!sessionStorage.getItem(__key)) {
                            var _btn = document.querySelector('#dividendListFragment .date-range-btn');
                            window.dividendPicker.set('mtd', _btn || null);
                            sessionStorage.setItem(__key, '1');
                        }
                    } catch(e) { window.dividendPicker.set('mtd', null); }
                }
            }
        } catch(e) {}

        // details arrow animation
        const detailSection = document.getElementById('detailSection');
        if (detailSection) {
            const arrow = detailSection.querySelector('.detail-arrow');
            detailSection.addEventListener('toggle', function() {
                if (arrow) arrow.style.transform = detailSection.open ? 'rotate(90deg)' : '';
            });
        }

        initializeDividendYieldSortableTables();
        initializeDividendYieldSelection();
        window.initializeStockTagHoverTooltips(document);

        // Chart.js 는 stockLayout 에서 1회 로드되어 항상 존재한다. (initAll 은 ensureStockCharts 로 헬퍼 보장)
        setTimeout(initAll, 0);
        setTimeout(() => { if (monthlyChart) monthlyChart.resize(); if (donutChart) donutChart.resize(); }, 200);

        // 페이징 클릭 시 fragment 상단 스크롤
        if (window.__pendingScrollTo === 'dividendListFragment') {
            delete window.__pendingScrollTo;
            const frag = document.getElementById('dividendListFragment');
            if (frag) frag.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
        document.querySelectorAll('#dividendListFragment .join button[hx-get]').forEach(btn => {
            btn.addEventListener('click', () => { window.__pendingScrollTo = 'dividendListFragment'; });
        });
    })();
    </script>

</div>