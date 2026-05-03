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
    String investedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.invested.capital");
    String marketValueLabel = MessageUtil.getMessage("stock.dividend.yield.column.market.value");
    String yieldOnCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.cost");
    String yieldOnMarketLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.market");
    String dividendCountLabel = MessageUtil.getMessage("stock.dividend.yield.column.dividend.count");
    String latestDividendDateLabel = MessageUtil.getMessage("stock.dividend.yield.column.latest.date");
    String noYieldAnalyticsLabel = MessageUtil.getMessage("stock.dividend.yield.message.no.analytics");
    String yieldBasisNoteLabel = MessageUtil.getMessage("stock.dividend.yield.basis.note");
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
    <details class="mb-4"$unsafe{((selectedAccountIds != null && !selectedAccountIds.isEmpty()) || (selectedStockItemIds != null && !selectedStockItemIds.isEmpty()) || (selectedStockTags != null && !selectedStockTags.isEmpty()) || startDate != null) ? " open" : ""}>
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
    <form id="dividendSearchForm" class="flex flex-wrap gap-2 mt-2 mb-2 items-end bg-base-100 p-2 rounded-box shadow-sm"
          hx-get="/stock/htmx/dividend/list"
          hx-target="#dividendListFragment"
          hx-select="#dividendListFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">${periodLabel}</span></label>
            <div class="join">
                <input type="date" name="dividendUiStartDate" id="startDateInput" class="input input-sm input-bordered join-item" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
                <input type="date" name="dividendUiEndDate" id="endDateInput" class="input input-sm input-bordered join-item" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
            </div>
        </div>
        <input type="hidden" name="startDate" id="dividendStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
        <input type="hidden" name="endDate" id="dividendEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
        <input type="hidden" name="timeZone" id="dividendTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
        <input type="hidden" name="rangeMode" id="dividendRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">${accountLabel}</span></label>
            <select name="accountIdList" class="select-bordered w-full max-h-52 overflow-auto" multiple data-max-visible="6">
                <option value="">${allLabel}</option>
                @for(Account account : accountList)
                <option value="${account.id().toString()}" selected="${selectedAccountIds != null && selectedAccountIds.contains(account.id())}">${account.name()}</option>
                @endfor
            </select>
        </div>

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">${tagLabel}</span></label>
            <select name="stockTagList" class="select-bordered w-full max-h-52 overflow-auto" multiple data-max-visible="6">
                @for(String stockTag : stockTagList)
                <option value="${stockTag}" selected="${selectedStockTags != null && selectedStockTags.contains(stockTag)}">${stockTag}</option>
                @endfor
            </select>
        </div>

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">${stockNameLabel}</span></label>
            <select name="stockItemIdList" class="select-bordered w-full max-h-52 overflow-auto" multiple data-max-visible="6">
                <option value="">${allLabel}</option>
                @for(StockItem stockItem : stockItemList)
                <option value="${stockItem.id().toString()}" selected="${selectedStockItemIds != null && selectedStockItemIds.contains(stockItem.id())}">${stockItem.name()}</option>
                @endfor
            </select>
        </div>

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
    </form>
    </details>

    <script type="module" src="/js/stock/selectionStorage.js"></script>

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
        <div class="text-sm font-bold">${dividendYieldTitle}</div>
        <div class="text-xs text-base-content/50 mt-1">${yieldBasisNoteLabel}</div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${dividendYieldPeriodLabel}</div>
                <div class="text-xl font-bold text-dividend">${percentFormat.apply(portfolioYieldOnCostPct)}</div>
                <div class="text-xs text-base-content/40 mt-1">${yieldOnMarketLabel} <span class="font-medium">${percentFormat.apply(portfolioYieldOnMarketPct)}</span></div>
            </div>
        </div>

        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${bestYieldStockLabel}</div>
                @if(bestYieldStock != null)
                    <div class="text-sm font-semibold truncate">${bestYieldStock.label()}</div>
                    <div class="text-xl font-bold text-dividend">${percentFormat.apply(bestYieldStock.yieldOnCostPct())}</div>
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
                    <div class="text-xl font-bold text-dividend">${percentFormat.apply(bestYieldAccount.yieldOnCostPct())}</div>
                    <div class="text-xs text-base-content/40 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩ ${decimalFormat.format(bestYieldAccount.totalNetAmount())}</span></div>
                @else
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @endif
            </div>
        </div>
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-3 gap-4 mb-4">
        <div class="card bg-base-100 shadow-sm border border-base-200">
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
                                    <th class="text-right">${netAmountLabel}</th>
                                    <th class="text-right">${investedCapitalLabel}</th>
                                    <th class="text-right">${yieldOnCostLabel}</th>
                                    <th class="text-right">${dividendCountLabel}</th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : yearlyYieldRows)
                                    <tr>
                                        <td class="font-medium">${row.label()}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right">${countFormat.apply(row.dividendCount())}</td>
                                    </tr>
                                @endfor
                            </tbody>
                        </table>
                    </div>
                @endif
            </div>
        </div>

        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-sm font-bold mb-3">${stockYieldSectionLabel}</div>
                @if(stockYieldRows == null || stockYieldRows.isEmpty())
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @else
                    <div class="overflow-x-auto">
                        <table class="table table-xs w-full">
                            <thead>
                                <tr class="text-base-content/60">
                                    <th>${stockNameLabel}</th>
                                    <th class="text-right">${netAmountLabel}</th>
                                    <th class="text-right">${investedCapitalLabel}</th>
                                    <th class="text-right">${yieldOnCostLabel}</th>
                                    <th class="text-right">${yieldOnMarketLabel}</th>
                                    <th class="text-right">${dividendCountLabel}</th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : stockYieldRows)
                                    <tr>
                                        <td class="font-medium">${row.label()}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right text-base-content/70">${percentFormat.apply(row.yieldOnMarketPct())}</td>
                                        <td class="text-right">${countFormat.apply(row.dividendCount())}</td>
                                    </tr>
                                @endfor
                            </tbody>
                        </table>
                    </div>
                @endif
            </div>
        </div>

        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-sm font-bold mb-3">${accountYieldSectionLabel}</div>
                @if(accountYieldRows == null || accountYieldRows.isEmpty())
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @else
                    <div class="overflow-x-auto">
                        <table class="table table-xs w-full">
                            <thead>
                                <tr class="text-base-content/60">
                                    <th>${accountLabel}</th>
                                    <th class="text-right">${netAmountLabel}</th>
                                    <th class="text-right">${investedCapitalLabel}</th>
                                    <th class="text-right">${yieldOnCostLabel}</th>
                                    <th class="text-right">${yieldOnMarketLabel}</th>
                                    <th class="text-right">${dividendCountLabel}</th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : accountYieldRows)
                                    <tr>
                                        <td class="font-medium">${row.label()}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right text-base-content/70">${percentFormat.apply(row.yieldOnMarketPct())}</td>
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
                        <th class="text-right hidden xl:table-cell">${investedCapitalLabel}</th>
                        <th class="text-right">${yieldOnCostLabel}</th>
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
        const filterEndDate = "${endLocal != null ? endLocal.format(DateTimeFormatter.ofPattern("yyyy-MM")) : ""}";
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
            let monthCount;
            if (filterStartDate && filterEndDate) {
                const [sy, sm] = filterStartDate.split('-').map(Number);
                const [ey, em] = filterEndDate.split('-').map(Number);
                monthCount = (ey - sy) * 12 + (em - sm) + 1;
            } else {
                const monthSet = new Set(dividendData.filter(d => d.payDate).map(d => d.payDate.slice(0, 7)));
                monthCount = monthSet.size || 1;
            }
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