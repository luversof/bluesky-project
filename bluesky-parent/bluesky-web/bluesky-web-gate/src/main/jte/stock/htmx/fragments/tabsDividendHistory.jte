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
@param BigDecimal prevPeriodNetAmount = null
@param LocalDate prevStartDate = null
@param LocalDate prevEndDate = null
@param BigDecimal portfolioYieldOnDailyAverageCostPct = null
@param BigDecimal portfolioYieldOnCostPct = null
@param BigDecimal portfolioYieldOnMarketPct = null
@param DividendYieldGroupView bestYieldStock = null
@param DividendYieldGroupView bestYieldAccount = null
@param List<DividendYieldGroupView> stockYieldRows = java.util.List.of()
@param List<DividendYieldGroupView> accountYieldRows = java.util.List.of()
@param List<DividendYieldGroupView> yearlyYieldRows = java.util.List.of()
@param String rangeMode = null
@param String dataFirstDate = null

!{
    String sortField = sort != null ? sort.split(",")[0] : "";
    String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DecimalFormat decimalFormat = new DecimalFormat("#,##0");

    // Safe nulls
    BigDecimal safeAllNet = totalAllNetAmount != null ? totalAllNetAmount : BigDecimal.ZERO;
    BigDecimal safeAllGross = totalAllGrossAmount != null ? totalAllGrossAmount : BigDecimal.ZERO;
    BigDecimal safeAllTaxable = totalAllTaxableAmount != null ? totalAllTaxableAmount : BigDecimal.ZERO;
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
    String detailSearchLabel = MessageUtil.getMessage("common.label.detail") + " " + MessageUtil.getMessage("common.button.search");
    String periodLabel = MessageUtil.getMessage("common.label.period");
    String accountLabel = MessageUtil.getMessage("common.label.account");
    String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
    String tagLabel = MessageUtil.getMessage("common.label.tag");
    String tagHelperLabel = MessageUtil.getMessage("stock.filter.tag.helper");
    String tagSummaryLabel = MessageUtil.getMessage("stock.filter.tag.summary.selected");
    String tagStockSummaryPattern = MessageUtil.getMessage("stock.filter.tag.summary.stocks");
    String tagMorePattern = MessageUtil.getMessage("stock.filter.tag.summary.more");
    String allLabel = MessageUtil.getMessage("common.label.all");
    String searchLabel = MessageUtil.getMessage("common.button.search");
    String resetLabel = MessageUtil.getMessage("common.button.reset");
    String periodDividendLabel = MessageUtil.getMessage("stock.dividend.summary.period.total");
    String netLabel = MessageUtil.getMessage("common.label.net");
    String grossLabel = MessageUtil.getMessage("common.label.gross");
    String taxableLabel = MessageUtil.getMessage("common.label.taxable");
    String previousComparisonLabel = MessageUtil.getMessage("stock.dividend.summary.previous.comparison");
    String previousPeriodLabel = MessageUtil.getMessage("stock.dividend.summary.previous.period");
    String comparisonEmptyLabel = MessageUtil.getMessage("stock.dividend.summary.select.period.compare");
    String monthlyAverageLabel = MessageUtil.getMessage("stock.dividend.summary.monthly.average");
    String weightAnalysisLabel = MessageUtil.getMessage("stock.chart.title.weight.analysis");
    String byStockLabel = MessageUtil.getMessage("stock.label.by.stock");
    String byAccountLabel = MessageUtil.getMessage("stock.label.by.account");
    String monthlyDividendsLabel = MessageUtil.getMessage("stock.dividend.chart.title.monthly.simple");
    String detailSummaryLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.detail.summary"), countMessage.apply(totalItems));
    String dateLabel = MessageUtil.getMessage("common.label.date");
    String grossAmountLabel = MessageUtil.getMessage("stock.dividend.gross");
    String netAmountLabel = MessageUtil.getMessage("stock.dividend.net");
    String taxLabel = MessageUtil.getMessage("stock.dividend.tax");
    String taxableAmountLabel = MessageUtil.getMessage("stock.dividend.taxable.amount");
    String noDividendHistoryLabel = MessageUtil.getMessage("stock.dividend.message.no.history");
    String totalLabel = MessageUtil.getMessage("common.label.total");
    String noDataLabel = MessageUtil.getMessage("common.message.no.data");
    String noPeriodHistoryLabel = MessageUtil.getMessage("stock.dividend.message.no.period.history");
    String averageNoDataLabel = MessageUtil.getMessage("stock.dividend.summary.no.data");
    String averageDescTemplate = MessageUtil.getMessage("stock.dividend.summary.average.desc");
    String quantityLabel = MessageUtil.getMessage("common.label.quantity");
    String referencePriceLabel = MessageUtil.getMessage("stock.column.reference.price");
    String dividendYieldTitle = MessageUtil.getMessage("stock.dividend.yield.summary.title");
    String dividendYieldPeriodLabel = MessageUtil.getMessage("stock.dividend.yield.summary.period");
    String bestYieldStockLabel = MessageUtil.getMessage("stock.dividend.yield.summary.best.stock");
    String bestYieldAccountLabel = MessageUtil.getMessage("stock.dividend.yield.summary.best.account");
    String yearlyYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.yearly");
    String stockYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.stock");
    String accountYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.account");
    String yearLabel = MessageUtil.getMessage("stock.dividend.yield.column.year");
    String basisCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.basis.cost");
    String dailyAverageInvestedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.daily.average.invested.capital");
    String basisAverageInvestedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.basis.average.invested.capital");
    String marketValueLabel = MessageUtil.getMessage("stock.dividend.yield.column.market.value");
    String yieldOnDailyAverageCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.daily.average.cost");
    String yieldOnBasisCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.cost");
    String yieldOnBasisAverageCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.average.cost");
    String yieldOnBasisMarketLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.market");
    String dividendYieldPeriodDailyLabel = MessageUtil.getMessage("stock.dividend.yield.summary.period.daily");
    String dividendYieldPeriodBasisLabel = MessageUtil.getMessage("stock.dividend.yield.summary.period.basis");
    String dividendYieldHelpTitleLabel = MessageUtil.getMessage("stock.dividend.yield.help.title");
    String dividendYieldHelpPrimaryLabel = MessageUtil.getMessage("stock.dividend.yield.help.primary");
    String dividendYieldHelpSecondaryLabel = MessageUtil.getMessage("stock.dividend.yield.help.secondary");
    String dividendYieldHelpRankingLabel = MessageUtil.getMessage("stock.dividend.yield.help.ranking");
    String dividendCountLabel = MessageUtil.getMessage("stock.dividend.yield.column.dividend.count");
    String latestDividendDateLabel = MessageUtil.getMessage("stock.dividend.yield.column.latest.date");
    String noYieldAnalyticsLabel = MessageUtil.getMessage("stock.dividend.yield.message.no.analytics");
    String yieldBasisNoteLabel = MessageUtil.getMessage("stock.dividend.yield.basis.note");
    String stockYieldSelectionHelperLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.helper");
    String stockYieldSelectionSummaryLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.summary");
    String stockYieldSelectionCountLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.count");
    String accountYieldSelectionHelperLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.helper");
    String accountYieldSelectionSummaryLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.summary");
    String accountYieldSelectionCountLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.count");
    String dividendYieldSelectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
    String dividendYieldSelectionWeightLabel = MessageUtil.getMessage("stock.analytics.selection.weight");
    java.util.function.Function<String, String> stockTagTooltipByName = stockName -> {
        if (stockName == null || stockName.isBlank() || stockItemList == null) {
            return tagLabel + ": " + noDataLabel;
        }

        return stockItemList.stream()
            .filter(stockItem -> stockItem != null && stockName.equals(stockItem.name()))
            .findFirst()
            .map(stockItem -> stockItem.tags() != null && !stockItem.tags().isEmpty()
                ? tagLabel + ": " + String.join(", ", stockItem.tags())
                : tagLabel + ": " + noDataLabel)
            .orElse(tagLabel + ": " + noDataLabel);
    };
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

    <!-- Filter & Search Form (collapsible) -->
    <details class="mb-4" open>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            ${detailSearchLabel}
            @template.stock.htmx.fragments.components.filterBadge(
                selectedAccountId = selectedAccountId,
                selectedStockItemId = selectedStockItemId,
                startDate = startDate,
                endDate = endDate
            )
        </summary>
    <form id="dividendSearchForm" class="mt-2 mb-2 rounded-2xl bg-base-100 p-3 shadow-sm"
          hx-get="/stock/htmx/dividend/list"
          hx-target="#dividendListFragment"
          hx-select="#dividendListFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">

        <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(0,18rem)_minmax(0,22rem)_minmax(0,1fr)_minmax(0,19rem)] xl:items-start">
            <div class="form-control w-full">
                <label class="label p-1"><span class="label-text text-xs">${periodLabel}</span></label>
                <div class="join w-full">
                    <input type="date" name="dividendUiStartDate" id="startDateInput" class="input input-sm input-bordered join-item w-1/2" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
                    <input type="date" name="dividendUiEndDate" id="endDateInput" class="input input-sm input-bordered join-item w-1/2" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
                </div>
            </div>

            <div class="form-control w-full">
                <label class="label p-1"><span class="label-text text-xs">${accountLabel}</span></label>
                <select name="accountIdList" class="select select-bordered w-full max-h-40 overflow-auto" multiple size="${Math.min(accountList != null ? accountList.size() + 1 : 1, 7)}" data-max-visible="7">
                    <option value="">${allLabel}</option>
                    @for(Account account : accountList)
                    <option value="${account.id().toString()}" selected="${selectedAccountIds != null && selectedAccountIds.contains(account.id())}">${account.name()}</option>
                    @endfor
                </select>
            </div>

            <div class="form-control w-full">
                <label class="label p-1"><span class="label-text text-xs">${stockNameLabel}</span></label>
                <select name="stockItemIdList" class="select select-bordered w-full max-h-40 overflow-auto bg-base-100" multiple size="${Math.min(stockItemList != null ? stockItemList.size() + 1 : 1, 7)}" data-max-visible="7">
                    <option value="">${allLabel}</option>
                    @for(StockItem stockItem : stockItemList)
                    !{String stockItemTags = stockItem.tags() != null ? String.join("|", stockItem.tags()) : "";}
                    <option value="${stockItem.id().toString()}"
                            selected="${selectedStockItemIds != null && selectedStockItemIds.contains(stockItem.id())}"
                            data-stock-tags="${stockItemTags}">${stockItem.name()}</option>
                    @endfor
                </select>
            </div>

            <div class="flex h-full min-h-0 flex-col gap-3 xl:justify-between">
                @template.stock.htmx.fragments.components.stockSelectionPanel(
                    tagLabel = tagLabel,
                    stockLabel = stockNameLabel,
                    allLabel = allLabel,
                    helperLabel = tagHelperLabel,
                    selectedTagSummaryLabel = tagSummaryLabel,
                    selectedStockSummaryPattern = tagStockSummaryPattern,
                    stockPreviewMorePattern = tagMorePattern,
                    stockTagList = stockTagList,
                    selectedStockTags = selectedStockTags,
                    stockItemList = stockItemList
                )

                <div class="flex justify-end gap-2 xl:mt-auto">
                    <button type="button" class="btn btn-sm btn-primary" data-selection-save="true"
                        hx-get="/stock/htmx/dividend/list"
                        hx-target="#dividendListFragment"
                        hx-select="#dividendListFragment"
                        hx-swap="outerHTML"
                        hx-include="#dividendSearchForm">${searchLabel}</button>
                    <button type="button" class="btn btn-sm text-xs"
                        hx-get="/stock/htmx/dividend/list"
                        hx-target="#dividendListFragment"
                        hx-select="#dividendListFragment"
                            hx-swap="outerHTML"
                            hx-include="#dividendSearchForm"
                        data-selection-reset="true">${resetLabel}</button>
                </div>
            </div>
        </div>
        <input type="hidden" name="startDate" id="dividendStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
        <input type="hidden" name="endDate" id="dividendEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
        <input type="hidden" name="timeZone" id="dividendTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
        <input type="hidden" name="rangeMode" id="dividendRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">
    </form>
    </details>

    <script type="module" src="/js/stock/selectionStorage.js"></script>
    <script type="module" src="/js/stock/tagFilterChips.js"></script>

    <!-- Dashboard -->

    <!-- Row 1: Summary Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">

        <!-- Card 1: 기간 합계 (세전/세후/과세) -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${periodDividendLabel}</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-dividend amount-value">₩ ${decimalFormat.format(safeAllNet)}</span>
                    <span class="text-xs text-base-content/40">${netLabel}</span>
                </div>
                <div class="flex gap-4 text-xs text-base-content/50">
                    <span>${grossLabel} <span class="font-medium text-base-content/70 amount-value">₩ ${decimalFormat.format(safeAllGross)}</span></span>
                    <span>${taxableLabel} <span class="font-medium text-base-content/70 amount-value">₩ ${decimalFormat.format(safeAllTaxable)}</span></span>
                    <span class="ml-auto">${countMessage.apply(totalItems)}</span>
                </div>
            </div>
        </div>

        <!-- Card 2: 전기 대비 -->
        @if(hasComparison)
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${previousComparisonLabel}
                    @if(prevStartDate != null && prevEndDate != null)
                    <span class="opacity-40">(${prevStartDate.toString()} ~ ${prevEndDate.toString()})</span>
                    @endif
                </div>
                <div class="flex items-baseline gap-2 mb-1">
                    <span class="text-xl font-bold ${diffPositive ? "text-success" : "text-error"}">${diffPositive ? "▲" : "▼"} ${diffPct}%</span>
                    <span class="text-xs amount-value ${diffPositive ? "text-success" : "text-error"}">${diffPositive ? "+" : "-"}₩${decimalFormat.format(diffAmount.abs())}</span>
                </div>
                <div class="text-xs text-base-content/40">${previousPeriodLabel} <span class="amount-value">₩ ${decimalFormat.format(safePrevNet)}</span></div>
            </div>
        </div>
        @else
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4 items-center justify-center text-center">
                <div class="text-xs text-base-content/30">${comparisonEmptyLabel}</div>
            </div>
        </div>
        @endif

        <!-- Card 3: 월평균 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${monthlyAverageLabel}</div>
                <div class="text-xl font-bold"><span id="avgAmountDisplay" class="amount-value">-</span></div>
                <div class="text-xs text-base-content/40 mt-1" id="avgAmountDesc"></div>
            </div>
        </div>

    </div>

    <!-- Row 2: Charts -->
    <div class="flex flex-wrap gap-4 mb-4">

        <!-- Left: 비중 분석 -->
        <div class="card bg-base-100 shadow-sm border border-base-200 flex-[2] min-w-[200px]">
            <div class="card-body p-4">
                <div class="text-sm font-bold mb-3">${weightAnalysisLabel}</div>
                <div class="flex gap-3">
                    <div class="flex-shrink-0 w-[200px] h-[200px]">
                        <canvas id="donutChart" class="amount-chart"></canvas>
                    </div>
                    <div class="flex flex-col w-0 flex-1 h-[200px] overflow-hidden">
                        <div class="tabs tabs-boxed tabs-xs mb-1 w-fit flex-shrink-0">
                            <a class="tab tab-active" id="donutTabStock" onclick="switchDonut('stock')">${byStockLabel}</a>
                            <a class="tab" id="donutTabAccount" onclick="switchDonut('account')">${byAccountLabel}</a>
                        </div>
                        <div id="donutLegend" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Right: Monthly Bar Chart -->
        <div class="card bg-base-100 shadow-sm border border-base-200 flex-[3] min-w-[280px] overflow-visible">
            <div class="card-body p-4">
                <h3 class="font-bold text-sm mb-3">${monthlyDividendsLabel}</h3>
                <div class="relative h-[220px]">
                    <canvas id="monthlyBarChart" class="amount-chart"></canvas>
                </div>
            </div>
        </div>

    </div>

    <!-- Row 3: Yield Analytics -->
    <div class="mb-3 px-1">
        <div class="flex items-center gap-2">
            <div class="text-sm font-bold">${dividendYieldTitle}</div>
            <div class="tooltip tooltip-right shrink-0">
                <div class="tooltip-content z-[20] w-64 sm:w-72 max-w-[calc(100vw-2rem)] rounded-box bg-neutral text-neutral-content shadow-lg">
                    <div class="space-y-1 text-left text-xs leading-5">
                        <div class="font-semibold mb-1">${dividendYieldHelpTitleLabel}</div>
                        <div>${dividendYieldHelpPrimaryLabel}</div>
                        <div>${dividendYieldHelpSecondaryLabel}</div>
                        <div>${dividendYieldHelpRankingLabel}</div>
                    </div>
                </div>
                <button type="button" class="btn btn-circle btn-ghost btn-xs text-info" aria-label="${dividendYieldHelpTitleLabel}">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                </button>
            </div>
        </div>
        <div class="text-xs text-base-content/50 mt-1">${yieldBasisNoteLabel}</div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${dividendYieldPeriodDailyLabel}</div>
                <div class="text-xl font-bold text-dividend">${percentFormat.apply(portfolioYieldOnDailyAverageCostPct)}</div>
                <div class="text-xs text-base-content/40 mt-1">${dividendYieldPeriodBasisLabel} <span class="font-medium">${percentFormat.apply(portfolioYieldOnCostPct)}</span></div>
                <div class="text-xs text-base-content/40 mt-1">${yieldOnBasisMarketLabel} <span class="font-medium">${percentFormat.apply(portfolioYieldOnMarketPct)}</span></div>
            </div>
        </div>

        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${bestYieldStockLabel}</div>
                @if(bestYieldStock != null)
                    <div class="text-sm font-semibold truncate">${bestYieldStock.label()}</div>
                    <div class="text-xl font-bold text-dividend">${percentFormat.apply(bestYieldStock.yieldOnDailyAverageCostPct())}</div>
                    <div class="text-xs text-base-content/40 mt-1">${dailyAverageInvestedCapitalLabel} <span class="font-medium amount-value">₩ ${bestYieldStock.averageDailyPrincipalCost() != null ? decimalFormat.format(bestYieldStock.averageDailyPrincipalCost()) : "-"}</span></div>
                    <div class="text-xs text-base-content/40 mt-1">${yieldOnBasisAverageCostLabel} <span class="font-medium">${percentFormat.apply(bestYieldStock.yieldOnCostPct())}</span></div>
                    <div class="text-xs text-base-content/40 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩ ${decimalFormat.format(bestYieldStock.totalNetAmount())}</span></div>
                @else
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @endif
            </div>
        </div>

        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${bestYieldAccountLabel}</div>
                @if(bestYieldAccount != null)
                    <div class="text-sm font-semibold truncate">${bestYieldAccount.label()}</div>
                    <div class="text-xl font-bold text-dividend">${percentFormat.apply(bestYieldAccount.yieldOnDailyAverageCostPct())}</div>
                    <div class="text-xs text-base-content/40 mt-1">${dailyAverageInvestedCapitalLabel} <span class="font-medium amount-value">₩ ${bestYieldAccount.averageDailyPrincipalCost() != null ? decimalFormat.format(bestYieldAccount.averageDailyPrincipalCost()) : "-"}</span></div>
                    <div class="text-xs text-base-content/40 mt-1">${yieldOnBasisAverageCostLabel} <span class="font-medium">${percentFormat.apply(bestYieldAccount.yieldOnCostPct())}</span></div>
                    <div class="text-xs text-base-content/40 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩ ${decimalFormat.format(bestYieldAccount.totalNetAmount())}</span></div>
                @else
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @endif
            </div>
        </div>
    </div>

    <div class="grid grid-cols-1 gap-4 mb-4">
        <div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-sm font-bold mb-3">${yearlyYieldSectionLabel}</div>
                @if(yearlyYieldRows == null || yearlyYieldRows.isEmpty())
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @else
                    <div class="overflow-x-auto">
                        <table class="table table-xs w-full">
                            <thead>
                                <tr class="text-base-content/60">
                                    <th>${yearLabel}</th>
                                    <th class="text-right">${grossAmountLabel}</th>
                                    <th class="text-right">${netAmountLabel}</th>
                                    <th class="text-right">${taxLabel}</th>
                                    <th class="text-right">${taxableAmountLabel}</th>
                                    <th class="text-right">${dailyAverageInvestedCapitalLabel}</th>
                                    <th class="text-right">${yieldOnDailyAverageCostLabel}</th>
                                    <th class="text-right hidden xl:table-cell">${basisAverageInvestedCapitalLabel}</th>
                                    <th class="text-right hidden xl:table-cell">${yieldOnBasisAverageCostLabel}</th>
                                    <th class="text-right">${dividendCountLabel}</th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : yearlyYieldRows)
                                    !{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
                                    <tr>
                                        <td class="font-medium">${row.label()}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right amount-value">${rowTaxAmount != null ? decimalFormat.format(rowTaxAmount) : "-"}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalTaxableAmount())}</td>
                                        <td class="text-right amount-value">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnDailyAverageCostPct())}</td>
                                        <td class="text-right hidden xl:table-cell amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right hidden xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right">${countFormat.apply(row.dividendCount())}</td>
                                    </tr>
                                @endfor
                            </tbody>
                        </table>
                    </div>
                @endif
            </div>
        </div>

        <div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4" data-dividend-yield-selection-section>
                <div class="text-sm font-bold mb-3">${stockYieldSectionLabel}</div>
                @if(stockYieldRows == null || stockYieldRows.isEmpty())
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @else
                    <div class="mb-3 flex flex-col gap-3">
                        <p class="text-xs text-base-content/50">${stockYieldSelectionHelperLabel}</p>
                        <div class="hidden rounded-2xl border border-info/15 bg-info/5 px-4 py-3"
                             data-dividend-yield-selection-summary
                             data-count-template="${stockYieldSelectionCountLabel}"
                             data-total-net-amount="${safeAllNet.toPlainString()}"
                             data-weight-label="${dividendYieldSelectionWeightLabel}"
                             data-basis-label="${yieldOnBasisAverageCostLabel}"
                             aria-live="polite">
                            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                                <div class="flex flex-wrap items-center gap-2">
                                    <h3 class="text-sm font-semibold">${stockYieldSelectionSummaryLabel}</h3>
                                    <span class="badge badge-info badge-outline" data-dividend-yield-selection-count>${stockYieldSelectionSummaryLabel}</span>
                                    <span class="badge badge-ghost" data-dividend-yield-selection-weight-badge>${dividendYieldSelectionWeightLabel} 0.0%</span>
                                </div>
                                <button type="button" class="btn btn-ghost btn-xs self-start" data-dividend-yield-selection-clear>${dividendYieldSelectionClearLabel}</button>
                            </div>
                            <div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${grossAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-gross>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${netAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value text-dividend" data-dividend-yield-selection-net>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${taxLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-tax>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${taxableAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-taxable>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${dailyAverageInvestedCapitalLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-daily-capital>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${yieldOnDailyAverageCostLabel}</div>
                                    <div class="mt-1 font-mono font-semibold text-dividend" data-dividend-yield-selection-yield>0.00%</div>
                                    <div class="text-xs text-base-content/60" data-dividend-yield-selection-basis-yield>${yieldOnBasisAverageCostLabel} 0.00%</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table table-xs w-full" data-dividend-yield-selection-table>
                            <thead>
                                <tr class="text-base-content/60">
                                    <th class="w-10"></th>
                                    <th>${stockNameLabel}</th>
                                    <th class="text-right">${grossAmountLabel}</th>
                                    <th class="text-right">${netAmountLabel}</th>
                                    <th class="text-right">${taxLabel}</th>
                                    <th class="text-right">${taxableAmountLabel}</th>
                                    <th class="text-right">${dailyAverageInvestedCapitalLabel}</th>
                                    <th class="text-right">${yieldOnDailyAverageCostLabel}</th>
                                    <th class="text-right hidden xl:table-cell">${basisAverageInvestedCapitalLabel}</th>
                                    <th class="text-right hidden xl:table-cell">${yieldOnBasisAverageCostLabel}</th>
                                    <th class="text-right hidden 2xl:table-cell">${yieldOnBasisMarketLabel}</th>
                                    <th class="text-right">${dividendCountLabel}</th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : stockYieldRows)
                                    !{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
                                    !{String stockTagTooltip = stockTagTooltipByName.apply(row.label());}
                                    <tr class="dividend-yield-stock-row hover"
                                        data-dividend-yield-row
                                        data-selection-class="dividend-yield-stock-row-selected"
                                        tabindex="0"
                                        role="button"
                                        aria-pressed="false"
                                        data-gross-amount="${row.totalGrossAmount() != null ? row.totalGrossAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-net-amount="${row.totalNetAmount() != null ? row.totalNetAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-tax-amount="${rowTaxAmount != null ? rowTaxAmount.toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-taxable-amount="${row.totalTaxableAmount() != null ? row.totalTaxableAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-average-daily-principal-cost="${row.averageDailyPrincipalCost() != null ? row.averageDailyPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-average-principal-cost="${row.averagePrincipalCost() != null ? row.averagePrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}">
                                        <td class="align-top pt-2">
                                            <input type="checkbox"
                                                   class="checkbox checkbox-sm"
                                                   data-dividend-yield-selection-toggle
                                                   aria-label="${row.label() != null ? row.label() : stockNameLabel}">
                                        </td>
                                        <td class="font-medium"><span title="${stockTagTooltip}">${row.label()}</span></td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right amount-value">${rowTaxAmount != null ? decimalFormat.format(rowTaxAmount) : "-"}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalTaxableAmount())}</td>
                                        <td class="text-right amount-value">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnDailyAverageCostPct())}</td>
                                        <td class="text-right hidden xl:table-cell amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right hidden xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right hidden 2xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnMarketPct())}</td>
                                        <td class="text-right">${countFormat.apply(row.dividendCount())}</td>
                                    </tr>
                                @endfor
                            </tbody>
                        </table>
                    </div>
                @endif
            </div>
        </div>

        <div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4" data-dividend-yield-selection-section>
                <div class="text-sm font-bold mb-3">${accountYieldSectionLabel}</div>
                @if(accountYieldRows == null || accountYieldRows.isEmpty())
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @else
                    <div class="mb-3 flex flex-col gap-3">
                        <p class="text-xs text-base-content/50">${accountYieldSelectionHelperLabel}</p>
                        <div class="hidden rounded-2xl border border-secondary/15 bg-secondary/5 px-4 py-3"
                             data-dividend-yield-selection-summary
                             data-count-template="${accountYieldSelectionCountLabel}"
                             data-total-net-amount="${safeAllNet.toPlainString()}"
                             data-weight-label="${dividendYieldSelectionWeightLabel}"
                             data-basis-label="${yieldOnBasisAverageCostLabel}"
                             aria-live="polite">
                            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                                <div class="flex flex-wrap items-center gap-2">
                                    <h3 class="text-sm font-semibold">${accountYieldSelectionSummaryLabel}</h3>
                                    <span class="badge badge-secondary badge-outline" data-dividend-yield-selection-count>${accountYieldSelectionSummaryLabel}</span>
                                    <span class="badge badge-ghost" data-dividend-yield-selection-weight-badge>${dividendYieldSelectionWeightLabel} 0.0%</span>
                                </div>
                                <button type="button" class="btn btn-ghost btn-xs self-start" data-dividend-yield-selection-clear>${dividendYieldSelectionClearLabel}</button>
                            </div>
                            <div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${grossAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-gross>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${netAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value text-dividend" data-dividend-yield-selection-net>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${taxLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-tax>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${taxableAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-taxable>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${dailyAverageInvestedCapitalLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-daily-capital>0</div>
                                </div>
                                <div class="rounded-xl bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${yieldOnDailyAverageCostLabel}</div>
                                    <div class="mt-1 font-mono font-semibold text-dividend" data-dividend-yield-selection-yield>0.00%</div>
                                    <div class="text-xs text-base-content/60" data-dividend-yield-selection-basis-yield>${yieldOnBasisAverageCostLabel} 0.00%</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table table-xs w-full" data-dividend-yield-selection-table>
                            <thead>
                                <tr class="text-base-content/60">
                                    <th class="w-10"></th>
                                    <th>${accountLabel}</th>
                                    <th class="text-right">${grossAmountLabel}</th>
                                    <th class="text-right">${netAmountLabel}</th>
                                    <th class="text-right">${taxLabel}</th>
                                    <th class="text-right">${taxableAmountLabel}</th>
                                    <th class="text-right">${dailyAverageInvestedCapitalLabel}</th>
                                    <th class="text-right">${yieldOnDailyAverageCostLabel}</th>
                                    <th class="text-right hidden xl:table-cell">${basisAverageInvestedCapitalLabel}</th>
                                    <th class="text-right hidden xl:table-cell">${yieldOnBasisAverageCostLabel}</th>
                                    <th class="text-right hidden 2xl:table-cell">${yieldOnBasisMarketLabel}</th>
                                    <th class="text-right">${dividendCountLabel}</th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : accountYieldRows)
                                    !{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
                                    <tr class="dividend-yield-account-row hover"
                                        data-dividend-yield-row
                                        data-selection-class="dividend-yield-account-row-selected"
                                        tabindex="0"
                                        role="button"
                                        aria-pressed="false"
                                        data-gross-amount="${row.totalGrossAmount() != null ? row.totalGrossAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-net-amount="${row.totalNetAmount() != null ? row.totalNetAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-tax-amount="${rowTaxAmount != null ? rowTaxAmount.toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-taxable-amount="${row.totalTaxableAmount() != null ? row.totalTaxableAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-average-daily-principal-cost="${row.averageDailyPrincipalCost() != null ? row.averageDailyPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-average-principal-cost="${row.averagePrincipalCost() != null ? row.averagePrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}">
                                        <td class="align-top pt-2">
                                            <input type="checkbox"
                                                   class="checkbox checkbox-sm"
                                                   data-dividend-yield-selection-toggle
                                                   aria-label="${row.label() != null ? row.label() : accountLabel}">
                                        </td>
                                        <td class="font-medium">${row.label()}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right amount-value">${rowTaxAmount != null ? decimalFormat.format(rowTaxAmount) : "-"}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalTaxableAmount())}</td>
                                        <td class="text-right amount-value">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnDailyAverageCostPct())}</td>
                                        <td class="text-right hidden xl:table-cell amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right hidden xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right hidden 2xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnMarketPct())}</td>
                                        <td class="text-right">${countFormat.apply(row.dividendCount())}</td>
                                    </tr>
                                @endfor
                            </tbody>
                        </table>
                    </div>
                @endif
            </div>
        </div>
    </div>

    <style>
        .dividend-yield-stock-row,
        .dividend-yield-account-row {
            cursor: pointer;
        }

        .dividend-yield-stock-row.dividend-yield-stock-row-selected > td {
            background-color: color-mix(in oklch, var(--color-info) 10%, var(--color-base-100));
        }

        .dividend-yield-stock-row.dividend-yield-stock-row-selected > td:first-child {
            box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-info) 65%, transparent);
        }

        .dividend-yield-account-row.dividend-yield-account-row-selected > td {
            background-color: color-mix(in oklch, var(--color-secondary) 10%, var(--color-base-100));
        }

        .dividend-yield-account-row.dividend-yield-account-row-selected > td:first-child {
            box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-secondary) 65%, transparent);
        }

        .dividend-yield-stock-row:focus-visible,
        .dividend-yield-account-row:focus-visible {
            outline: 2px solid color-mix(in oklch, var(--color-info) 35%, transparent);
            outline-offset: -2px;
        }
    </style>

    <!-- Collapsible Detail List -->
    <details id="detailSection" class="mb-4">
        <summary class="cursor-pointer select-none flex items-center gap-2 font-semibold text-sm p-3 bg-base-200 rounded-box hover:bg-base-300 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 transition-transform detail-arrow" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            </svg>
            ${detailSummaryLabel}
        </summary>

        <div id="dividendListContainer" class="flex flex-col relative mt-2">
            <div class="overflow-x-auto flex-1 min-h-0">
                <table class="table table-hover table-striped w-full table-sm table-pin-rows">
                    <thead>
                    <tr class="bg-base-200">
                        <th class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "payDate,${sortField.equals("payDate") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${dateLabel} @if(sortField.equals("payDate"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="cursor-pointer hover:bg-base-300 hidden sm:table-cell" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "accountName,${sortField.equals("accountName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${accountLabel} @if(sortField.equals("accountName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "stockItemName,${sortField.equals("stockItemName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${stockNameLabel} @if(sortField.equals("stockItemName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "grossAmount,${sortField.equals("grossAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${grossAmountLabel} @if(sortField.equals("grossAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "netAmount,${sortField.equals("netAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${netAmountLabel} @if(sortField.equals("netAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right hidden md:table-cell">${quantityLabel}</th>
                        <th class="text-right hidden xl:table-cell">${referencePriceLabel}</th>
                        <th class="text-right hidden xl:table-cell">${basisCostLabel}</th>
                        <th class="text-right">${yieldOnBasisCostLabel}</th>
                        <th class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "tax,${sortField.equals("tax") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${taxLabel} @if(sortField.equals("tax"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "taxableAmount,${sortField.equals("taxableAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${taxableAmountLabel} @if(sortField.equals("taxableAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    @for(DividendView item : dividendList)
                    <tr>
                        <td>${item.payDate() != null ? dateFormatter.format(item.payDate().atZone(java.time.ZoneId.systemDefault())) : ""}</td>
                        <td class="hidden sm:table-cell">${item.accountName()}</td>
                        <td class="font-bold">${item.stockItemName()}</td>
                        <td class="text-right amount-value">${item.grossAmount() != null ? decimalFormat.format(item.grossAmount()) : "0"}</td>
                        <td class="text-right text-dividend font-bold amount-value">${item.netAmount() != null ? decimalFormat.format(item.netAmount()) : "0"}</td>
                        <td class="text-right hidden md:table-cell amount-value">${item.quantity() != null ? decimalFormat.format(item.quantity()) : "-"}</td>
                        <td class="text-right hidden xl:table-cell amount-value">${item.referencePrice() != null ? decimalFormat.format(item.referencePrice()) : "-"}</td>
                        <td class="text-right hidden xl:table-cell amount-value">${item.principalCost() != null ? decimalFormat.format(item.principalCost()) : "-"}</td>
                        <td class="text-right text-dividend font-medium">${percentFormat.apply(item.yieldOnCostPct())}</td>
                        <td class="text-right hidden lg:table-cell amount-value">${item.tax() != null ? decimalFormat.format(item.tax()) : "0"}</td>
                        <td class="text-right amount-value">${item.taxableAmount() != null ? decimalFormat.format(item.taxableAmount()) : "0"}</td>
                    </tr>
                    @endfor
                    @if(dividendList == null || dividendList.isEmpty())
                    <tr>
                        <td colspan="11" class="text-center py-10 opacity-50">${noDividendHistoryLabel}</td>
                    </tr>
                    @endif
                    </tbody>
                    <tfoot>
                        <tr class="font-bold bg-base-200">
                            <td colspan="3" class="text-center sm:text-right sm:pr-4">${totalLabel}</td>
                            <td class="text-right font-mono amount-value">${totalGrossAmount != null ? decimalFormat.format(totalGrossAmount) : "0"}</td>
                            <td class="text-right font-mono text-dividend amount-value">${totalNetAmount != null ? decimalFormat.format(totalNetAmount) : "0"}</td>
                            <td class="text-right hidden md:table-cell">-</td>
                            <td class="text-right hidden xl:table-cell">-</td>
                            <td class="text-right hidden xl:table-cell">-</td>
                            <td class="text-right">-</td>
                            <td class="text-right font-mono hidden lg:table-cell amount-value">${totalTax != null ? decimalFormat.format(totalTax) : "0"}</td>
                            <td class="text-right font-mono amount-value">${totalTaxableAmount != null ? decimalFormat.format(totalTaxableAmount) : "0"}</td>
                        </tr>
                    </tfoot>
                </table>
            </div>

            @if(pagination != null && pagination.getTotalPage() > 1)
            <div class="flex justify-center p-2 border-t border-base-200 bg-base-100">
                <div class="join">
                    <button class="join-item btn btn-sm ${!pagination.getFirstNav().isActive() ? "btn-disabled" : ""}"
                            disabled="${!pagination.getFirstNav().isActive()}"
                            hx-get="/stock/htmx/dividend/list"
                            hx-include="#dividendSearchForm"
                            hx-vals='{"page": ${pagination.getFirstNav().page()}}'
                            hx-target="#dividendListFragment"
                            hx-select="#dividendListFragment"
                            hx-swap="outerHTML">≪</button>
                    <button class="join-item btn btn-sm ${!pagination.getPrevNav().isActive() ? "btn-disabled" : ""}"
                            disabled="${!pagination.getPrevNav().isActive()}"
                            hx-get="/stock/htmx/dividend/list"
                            hx-include="#dividendSearchForm"
                            hx-vals='{"page": ${pagination.getPrevNav().page()}}'
                            hx-target="#dividendListFragment"
                            hx-select="#dividendListFragment"
                            hx-swap="outerHTML">&lt;</button>
                    @for(Pagination.Nav nav : pagination.getNavList())
                    <button class="join-item btn btn-sm ${nav.isActive() ? "btn-active" : ""}"
                            hx-get="/stock/htmx/dividend/list"
                            hx-include="#dividendSearchForm"
                            hx-vals='{"page": ${nav.page()}}'
                            hx-target="#dividendListFragment"
                            hx-select="#dividendListFragment"
                            hx-swap="outerHTML">${nav.page()}</button>
                    @endfor
                    <button class="join-item btn btn-sm ${!pagination.getNextNav().isActive() ? "btn-disabled" : ""}"
                            disabled="${!pagination.getNextNav().isActive()}"
                            hx-get="/stock/htmx/dividend/list"
                            hx-include="#dividendSearchForm"
                            hx-vals='{"page": ${pagination.getNextNav().page()}}'
                            hx-target="#dividendListFragment"
                            hx-select="#dividendListFragment"
                            hx-swap="outerHTML">&gt;</button>
                    <button class="join-item btn btn-sm ${!pagination.getLastNav().isActive() ? "btn-disabled" : ""}"
                            disabled="${!pagination.getLastNav().isActive()}"
                            hx-get="/stock/htmx/dividend/list"
                            hx-include="#dividendSearchForm"
                            hx-vals='{"page": ${pagination.getLastNav().page()}}'
                            hx-target="#dividendListFragment"
                            hx-select="#dividendListFragment"
                            hx-swap="outerHTML">≫</button>
                </div>
            </div>
            @endif
        </div>
    </details>

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
            summary.classList.toggle('hidden', selectedCount === 0);

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
            const m = buildMonthlyData();
            const gridColor = 'rgba(128,128,128,0.1)';
            const canvasId = 'monthlyBarChart';
            const canvasEl = document.getElementById(canvasId);
            if (!canvasEl) return;
            if (monthlyChart) try { monthlyChart.destroy(); } catch(e) {}
            var config = {
                type: 'bar',
                data: {
                    labels: m.labels,
                    datasets: [{
                        label: netAmountLabel,
                        data: m.data,
                        backgroundColor: 'rgba(34,197,94,0.55)',
                        borderColor: 'rgba(34,197,94,0.9)',
                        borderWidth: 1,
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    layout: { padding: { right: 8, bottom: 4 } },
                    plugins: { legend: { display: false } },
                    scales: {
                        x: { grid: { color: gridColor }, ticks: { maxRotation: 45, font: { size: 11 } } },
                        y: { beginAtZero: true, grid: { color: gridColor }, ticks: { callback: v => window.StockCharts ? window.StockCharts.formatCompactNumber(v) : Math.round(v).toLocaleString(), font: { size: 11 } } }
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

        initializeDividendYieldSelection();

        if (window.Chart) {
            setTimeout(initAll, 0);
            setTimeout(() => { if (monthlyChart) monthlyChart.resize(); if (donutChart) donutChart.resize(); }, 200);
        } else {
            const s = document.createElement('script');
            s.src = 'https://cdn.jsdelivr.net/npm/chart.js';
            s.onload = () => {
                setTimeout(initAll, 0);
                setTimeout(() => { if (monthlyChart) monthlyChart.resize(); if (donutChart) donutChart.resize(); }, 200);
            };
            document.head.appendChild(s);
        }

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