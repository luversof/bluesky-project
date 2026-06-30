@import java.math.BigDecimal
@import java.math.RoundingMode
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.TradeProfit
@import io.github.luversof.boot.context.support.MessageUtil

@param Map<String, TradeProfit> accountRealizedMap
@param Map<String, java.util.UUID> realizedAccountIdByName = java.util.Map.of()
@param List<TradeProfit> stockRealizedList
@param BigDecimal totalRealizedProfit
@param BigDecimal totalSellProceeds
@param BigDecimal totalFees
@param double winRate
@param TradeProfit bestStock
@param TradeProfit worstStock
@param List<String> chartProfitLabels
@param List<Long> chartProfitValues
@param List<String> chartLossLabels
@param List<Long> chartLossValues
@param String rangeMode = null
@param String dataFirstDate = null
@param java.time.Instant startDate = null
@param java.time.Instant endDate = null
@param String timeZone = null
@param java.util.List<net.luversof.web.gate.stock.domain.Account> accountList = null
@param java.util.List<net.luversof.web.gate.stock.domain.StockItem> stockItemList = null
@param java.util.List<String> stockTagList = java.util.List.of()
@param java.util.UUID selectedAccountId = null
@param java.util.UUID selectedStockItemId = null
@param java.util.List<java.util.UUID> selectedAccountIds = java.util.List.of()
@param java.util.List<java.util.UUID> selectedStockItemIds = java.util.List.of()
@param java.util.List<String> selectedStockTags = java.util.List.of()
@param String debugTradeAccountIds = null
@param Integer debugFinalAccountListSize = null
@param Integer debugFinalStockItemListSize = null

<%-- 수익률 기준: 매도한 주식의 취득원가 = 매도수령액 - 실현손익 --%>
!{BigDecimal totalSoldCost = totalSellProceeds != null && totalRealizedProfit != null
    ? totalSellProceeds.subtract(totalRealizedProfit)
    : BigDecimal.ZERO;}
!{double totalRate = totalSoldCost.compareTo(BigDecimal.ZERO) > 0 && totalRealizedProfit != null
    ? totalRealizedProfit.doubleValue() / totalSoldCost.doubleValue() * 100
    : 0.0;}

<div id="realizedProfitFragment">
    <!-- DEBUG: tradeAccountIds=${debugTradeAccountIds} finalAccountListSize=${debugFinalAccountListSize} finalStockItemListSize=${debugFinalStockItemListSize} -->

    !{
        boolean isAllMode = "all".equals(rangeMode);
        java.time.ZoneId zone = (timeZone != null && !timeZone.isEmpty())
            ? java.time.ZoneId.of(timeZone)
            : java.time.ZoneId.systemDefault();
        java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
        java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
        java.time.LocalDate today = java.time.LocalDate.now(zone);
        boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
        boolean canPrev = !isAllMode && startLocal != null && dataFirstDate != null && !dataFirstDate.isEmpty()
            && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));

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
            String stockMenuLabel = MessageUtil.getMessage("layout.menu.main.stock");
            String realizedProfitLabel = MessageUtil.getMessage("stock.profit.realized");
            String unrealizedProfitLabel = MessageUtil.getMessage("stock.profit.unrealized");
            String returnRateLabel = MessageUtil.getMessage("stock.label.return.rate");
            String totalLabel = MessageUtil.getMessage("common.label.total");
            String sharesLabel = MessageUtil.getMessage("stock.activity.label.shares");
            String totalRealizedNetLabel = MessageUtil.getMessage("stock.realized.summary.total.net");
            String soldCostLabel = MessageUtil.getMessage("stock.realized.summary.sold.cost");
            String bestStockLabel = MessageUtil.getMessage("stock.realized.summary.best.stock");
            String worstStockLabel = MessageUtil.getMessage("stock.realized.summary.worst.stock");
            String totalFeesLabel = MessageUtil.getMessage("stock.realized.summary.total.fees");
            String totalFeesDescLabel = MessageUtil.getMessage("stock.realized.summary.total.fees.desc");
            String profitableStockRatioLabel = MessageUtil.getMessage("stock.summary.label.profitable.stock.ratio");
            String winCountLabel = java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.realized.summary.win.count"),
                stockRealizedList != null ? stockRealizedList.stream().filter(tp -> tp.realizedProfitNet() != null && tp.realizedProfitNet().compareTo(BigDecimal.ZERO) > 0).count() : 0,
                stockRealizedList != null ? stockRealizedList.size() : 0);
            String topProfitChartLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.realized.chart.profit.top"), chartProfitLabels != null ? chartProfitLabels.size() : 0);
            String topLossChartLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.realized.chart.loss.top"), chartLossLabels != null ? chartLossLabels.size() : 0);
            String byAccountSectionLabel = MessageUtil.getMessage("stock.realized.section.by.account");
            String byStockSectionLabel = MessageUtil.getMessage("stock.realized.section.by.stock");
            String byStockSectionSubtitle = MessageUtil.getMessage("stock.realized.section.by.stock.subtitle");
            String showAllLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.realized.button.show.all"), stockRealizedList != null ? stockRealizedList.size() : 0);
            String collapseLabel = MessageUtil.getMessage("stock.realized.button.collapse");
            String soldCostColumnLabel = MessageUtil.getMessage("stock.realized.column.sold.cost");
            String sellQuantityLabel = MessageUtil.getMessage("stock.realized.column.sell.quantity");
            String noRealizedHistoryLabel = MessageUtil.getMessage("stock.realized.message.no.history");
            String fullySoldLabel = MessageUtil.getMessage("stock.realized.badge.fully.sold");
                boolean hasProfitChart = chartProfitLabels != null && !chartProfitLabels.isEmpty();
                boolean hasLossChart = chartLossLabels != null && !chartLossLabels.isEmpty();
                boolean hasAnyChart = hasProfitChart || hasLossChart;
                boolean useDualChartLayout = hasProfitChart && hasLossChart;
    }
    @template.stock.htmx.fragments.dateRangeNavBar(
        pickerName = "realizedProfitPicker",
        btnClass = "date-range-btn",
        canPrev = canPrev,
        canNext = canNext,
        rangeMode = rangeMode,
        startDate = startLocal,
        endDate = endLocal
    )

    <details class="mb-4" open data-detail-filter>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            ${detailSearchLabel}
            @template.stock.htmx.fragments.components.filterBadge(
                startDate = startDate,
                endDate = endDate
            )
        </summary>
    <form id="realizedProfitSearchForm" class="mt-2 mb-2 rounded-2xl bg-base-100 p-3 shadow-sm"
          hx-get="/stock/htmx/realized-profit"
          hx-target="#realizedProfitFragment"
          hx-select="#realizedProfitFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">
        <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(0,18rem)_minmax(0,22rem)_minmax(0,1fr)_minmax(0,19rem)_minmax(0,auto)] xl:items-start">
            <div class="form-control w-full">
                <label class="label p-1" id="filterPeriodLabel"><span class="label-text text-xs">${periodLabel}</span></label>
                <div class="join w-full" role="group" aria-labelledby="filterPeriodLabel">
                    <input type="date" name="realizedUiStartDate" id="realizedStartDateInput" class="input input-sm input-bordered join-item w-1/2" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
                    <input type="date" name="realizedUiEndDate" id="realizedEndDateInput" class="input input-sm input-bordered join-item w-1/2" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
                </div>
            </div>

            <div class="form-control w-full">
                <label class="label p-1" for="filterAccountSelect"><span class="label-text text-xs">${accountLabel}</span></label>
                <select id="filterAccountSelect" name="accountIdList" class="select select-bordered w-full max-h-40 overflow-auto" multiple size="${Math.min(accountList != null ? accountList.size() + 1 : 1, 7)}" data-max-visible="7">
                    <option value="">${allLabel}</option>
                    @if(accountList != null)
                        @for(net.luversof.web.gate.stock.domain.Account account : accountList)
                            <option value="${account.id().toString()}" selected="${selectedAccountIds != null && selectedAccountIds.contains(account.id())}">${account.name()}</option>
                        @endfor
                    @endif
                </select>
            </div>

            <div class="form-control w-full">
                <label class="label p-1" for="filterStockSelect"><span class="label-text text-xs">${stockNameLabel}</span></label>
                <select id="filterStockSelect" name="stockItemIdList" class="select select-bordered w-full max-h-40 overflow-auto bg-base-100" multiple size="${Math.min(stockItemList != null ? stockItemList.size() + 1 : 1, 7)}" data-max-visible="7">
                    <option value="">${allLabel}</option>
                    @if(stockItemList != null)
                        @for(net.luversof.web.gate.stock.domain.StockItem stockItem : stockItemList)
                            !{String stockItemTags = stockItem.tags() != null ? String.join("|", stockItem.tags()) : "";}
                            <option value="${stockItem.id().toString()}"
                                    selected="${selectedStockItemIds != null && selectedStockItemIds.contains(stockItem.id())}"
                                    data-stock-tags="${stockItemTags}">${stockItem.name()}</option>
                        @endfor
                    @endif
                </select>
            </div>

            <div class="contents">
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

                <div class="flex justify-end gap-2 xl:self-end">
                    <button type="button" class="btn btn-sm btn-primary" data-selection-save="true"
                        hx-get="/stock/htmx/realized-profit"
                        hx-target="#realizedProfitFragment"
                        hx-select="#realizedProfitFragment"
                        hx-swap="outerHTML"
                        hx-include="#realizedProfitSearchForm">${searchLabel}</button>
                    <button type="button" class="btn btn-sm text-xs"
                        hx-get="/stock/htmx/realized-profit"
                        hx-target="#realizedProfitFragment"
                        hx-select="#realizedProfitFragment"
                            hx-swap="outerHTML"
                            hx-include="#realizedProfitSearchForm"
                        data-selection-reset="true">${resetLabel}</button>
                </div>
            </div>
        </div>
        <input type="hidden" name="startDate" id="realizedStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
        <input type="hidden" name="endDate" id="realizedEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
        <input type="hidden" name="timeZone" id="realizedTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
        <input type="hidden" name="rangeMode" id="realizedRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">
    </form>
    </details>

    <script type="module" src="/js/stock/selectionStorage.js"></script>
    <script type="module" src="/js/stock/tagFilterChips.js"></script>

    <!-- Section 0: 요약 카드 -->
    <div class="stats stats-vertical lg:stats-horizontal shadow w-full bg-base-100">

        <div class="stat">
            <div class="stat-figure text-primary">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path></svg>
            </div>
            <div class="stat-title">${totalRealizedNetLabel}</div>
            <div class="stat-value font-bold amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                ${String.format("%+,d", totalRealizedProfit != null ? totalRealizedProfit.longValue() : 0)}
            </div>
            <div class="stat-desc mt-1 opacity-60">
                ${soldCostLabel}: <span class="amount-value">${String.format("%,d", totalSoldCost.longValue())}</span>
            </div>
        </div>

        @if(bestStock != null)
        !{double bestRate = bestStock.totalSellProceeds() != null && bestStock.realizedProfitNet() != null
            && bestStock.totalSellProceeds().subtract(bestStock.realizedProfitNet()).compareTo(BigDecimal.ZERO) > 0
            ? bestStock.realizedProfitNet().doubleValue() / bestStock.totalSellProceeds().subtract(bestStock.realizedProfitNet()).doubleValue() * 100
            : 0.0;}
        <div class="stat">
            <div class="stat-figure text-profit">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path></svg>
            </div>
            <div class="stat-title">${bestStockLabel}</div>
            <div class="stat-value text-base font-bold text-profit">
                <a href="/stock/item?stockItemId=${bestStock.stockItemId() != null ? bestStock.stockItemId().toString() : ""}" class="link link-hover">${bestStock.stockItemName() != null ? bestStock.stockItemName() : "-"}</a>
            </div>
            <div class="stat-desc font-semibold mt-1 text-profit">
                <span class="amount-value">${String.format("%+,d", bestStock.realizedProfitNet() != null ? bestStock.realizedProfitNet().longValue() : 0)}</span>
                <span class="ml-1">(${String.format("%+.1f%%", bestRate)})</span>
            </div>
        </div>
        @endif

        @if(worstStock != null)
        !{double worstRate = worstStock.totalSellProceeds() != null && worstStock.realizedProfitNet() != null
            && worstStock.totalSellProceeds().subtract(worstStock.realizedProfitNet()).compareTo(BigDecimal.ZERO) > 0
            ? worstStock.realizedProfitNet().doubleValue() / worstStock.totalSellProceeds().subtract(worstStock.realizedProfitNet()).doubleValue() * 100
            : 0.0;}
        <div class="stat">
            <div class="stat-figure text-loss">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 17h8m0 0V9m0 8l-8-8-4 4-6-6"></path></svg>
            </div>
            <div class="stat-title">${worstStockLabel}</div>
            <div class="stat-value text-base font-bold text-loss">
                <a href="/stock/item?stockItemId=${worstStock.stockItemId() != null ? worstStock.stockItemId().toString() : ""}" class="link link-hover">${worstStock.stockItemName() != null ? worstStock.stockItemName() : "-"}</a>
            </div>
            <div class="stat-desc font-semibold mt-1 text-loss">
                <span class="amount-value">${String.format("%+,d", worstStock.realizedProfitNet() != null ? worstStock.realizedProfitNet().longValue() : 0)}</span>
                <span class="ml-1">(${String.format("%+.1f%%", worstRate)})</span>
            </div>
        </div>
        @endif

        <div class="stat">
            <div class="stat-figure text-warning">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
            </div>
            <div class="stat-title">${totalFeesLabel}</div>
            <div class="stat-value text-base font-bold amount-value text-warning">
                ${String.format("%,d", totalFees != null ? totalFees.longValue() : 0)}
            </div>
            <div class="stat-desc mt-1 opacity-70">${totalFeesDescLabel}</div>
        </div>

        <div class="stat">
            <div class="stat-figure text-accent">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"></path></svg>
            </div>
            <div class="stat-title">${profitableStockRatioLabel}</div>
            <div class="stat-value font-bold text-accent">${String.format("%.1f%%", winRate)}</div>
            <div class="stat-desc mt-1">${winCountLabel}</div>
        </div>

    </div>

    @if(hasAnyChart)
    <!-- Section 1: 수익 Top5 / 손실 Worst5 차트 -->
    <div class="${useDualChartLayout ? "grid grid-cols-1 lg:grid-cols-2 gap-4 mt-6" : "mt-6"}">

        @if(hasProfitChart)
        <div class="card bg-base-100 border border-base-200 rounded-box shadow-sm">
            <div class="card-body p-4">
                <h2 class="text-base font-semibold mb-2 text-profit">${topProfitChartLabel}</h2>
                <div class="relative w-full amount-chart" style="height: ${Math.max(190, chartProfitLabels.size() * 44)}px;">
                    <canvas id="chartProfit"></canvas>
                </div>
            </div>
        </div>
        <script>
        (function() {
            var __script = document.currentScript;
            var __card = __script ? __script.previousElementSibling : null;
            var __canvas = __card ? __card.querySelector('canvas') : null;
            var labels = [];
            @for(String lbl : chartProfitLabels)
                labels.push("${lbl.replace("\"", "\\\"").replace("\\", "\\\\")} ");
            @endfor
            var values = [];
            @for(Long val : chartProfitValues)
                values.push($unsafe{String.valueOf(val)});
            @endfor
            var formatCurrency = function(value) { return window.StockCharts ? window.StockCharts.formatCurrency(value) : ('₩' + Math.round(value).toLocaleString()); };
            var formatSignedCurrency = function(value) { return (value >= 0 ? '+' : '-') + formatCurrency(Math.abs(value)); };
            var formatNumber = function(value) { return window.StockCharts ? window.StockCharts.formatNumber(value) : new Intl.NumberFormat(window.StockCharts ? window.StockCharts.getLocale() : undefined).format(value); };
            var config = {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{ label: '${realizedProfitLabel}', data: values,
                        backgroundColor: 'rgba(34,197,94,0.7)', borderColor: 'rgb(34,197,94)',
                        borderWidth: 1, borderRadius: 4 }]
                },
                options: {
                    indexAxis: 'y', responsive: true, maintainAspectRatio: false,
                    layout: { padding: { top: 4, right: 10, bottom: 12, left: 4 } },
                    plugins: { legend: { display: false },
                        tooltip: { callbacks: { label: function(c) {
                            return ' ' + formatSignedCurrency(c.parsed.x); } } } },
                    scales: {
                        x: { ticks: { callback: function(v) {
                            return window.StockCharts ? window.StockCharts.formatCompactNumber(v) : formatNumber(v); } } },
                        y: { ticks: { font: { size: 12 } } }
                    }
                }
            };
            window.ensureStockCharts(function() { window['chartProfitInstance'] = window.StockCharts.createChart((__canvas && __canvas.id) || 'chartProfit', config, window['chartProfitInstance']); });
        })();
        </script>
        @endif

        @if(hasLossChart)
        <div class="card bg-base-100 border border-base-200 rounded-box shadow-sm">
            <div class="card-body p-4">
                <h2 class="text-base font-semibold mb-2 text-loss">${topLossChartLabel}</h2>
                <div class="relative w-full amount-chart" style="height: ${Math.max(190, chartLossLabels.size() * 44)}px;">
                    <canvas id="chartLoss"></canvas>
                </div>
            </div>
        </div>
        <script>
        (function() {
            var __script = document.currentScript;
            var __card = __script ? __script.previousElementSibling : null;
            var __canvas = __card ? __card.querySelector('canvas') : null;
            var labels = [];
            @for(String lbl : chartLossLabels)
                labels.push("${lbl.replace("\"", "\\\"").replace("\\", "\\\\")} ");
            @endfor
            var values = [];
            @for(Long val : chartLossValues)
                values.push($unsafe{String.valueOf(val)});
            @endfor
            var formatCurrency = function(value) { return window.StockCharts ? window.StockCharts.formatCurrency(value) : ('₩' + Math.round(value).toLocaleString()); };
            var formatNumber = function(value) { return window.StockCharts ? window.StockCharts.formatNumber(value) : new Intl.NumberFormat(window.StockCharts ? window.StockCharts.getLocale() : undefined).format(value); };
            var config = {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{ label: '${realizedProfitLabel}', data: values,
                        backgroundColor: 'rgba(239,68,68,0.7)', borderColor: 'rgb(239,68,68)',
                        borderWidth: 1, borderRadius: 4 }]
                },
                options: {
                    indexAxis: 'y', responsive: true, maintainAspectRatio: false,
                    layout: { padding: { top: 4, right: 10, bottom: 12, left: 4 } },
                    plugins: { legend: { display: false },
                        tooltip: { callbacks: { label: function(c) {
                            return ' ' + formatCurrency(c.parsed.x); } } } },
                    scales: {
                        x: { ticks: { callback: function(v) {
                            return window.StockCharts ? window.StockCharts.formatCompactNumber(v) : formatNumber(v); } } },
                        y: { ticks: { font: { size: 12 } } }
                    }
                }
            };
            window.ensureStockCharts(function() { window['chartLossInstance'] = window.StockCharts.createChart((__canvas && __canvas.id) || 'chartLoss', config, window['chartLossInstance']); });
        })();
        </script>
        @endif

    </div>
    @endif

    @if(hasAnyChart)
    <script>
    (function() {
        function resizeRealizedProfitCharts() {
            try {
                ['chartProfitInstance', 'chartLossInstance'].forEach(function(key) {
                    var chart = window[key];
                    if (!chart || !chart.canvas || !chart.canvas.isConnected) return;
                    try {
                        chart.resize();
                        chart.update('none');
                    } catch (e) {}
                });
            } catch (e) {}
        }

        setTimeout(resizeRealizedProfitCharts, 0);
        setTimeout(resizeRealizedProfitCharts, 200);
        setTimeout(resizeRealizedProfitCharts, 500);

        function handleAfterSettle(event) {
            try {
                var fragment = document.getElementById('realizedProfitFragment');
                var target = event && event.target ? event.target : null;
                if (!fragment || !target) return;
                if (target === fragment || fragment.contains(target) || (target.contains && target.contains(fragment))) {
                    resizeRealizedProfitCharts();
                    setTimeout(resizeRealizedProfitCharts, 200);
                    document.body.removeEventListener('htmx:afterSettle', handleAfterSettle);
                }
            } catch (e) {}
        }

        document.body.addEventListener('htmx:afterSettle', handleAfterSettle);
    })();
    </script>
    @endif

    <!-- Section 2: 계좌별 실현손익 -->
    <div class="mt-6">
        <h2 class="text-lg font-semibold mb-3 opacity-70">${byAccountSectionLabel}</h2>
        <div class="card bg-base-100 border border-base-200 rounded-box shadow-sm overflow-x-auto">
            <table id="accountRealizedTable" class="table table-sm w-full">
                <thead class="bg-base-200">
                    <tr>
                        <th scope="col" class="cursor-pointer select-none" onclick="sortAccountTable(0)">
                            ${accountLabel} <span id="acc-sort-icon-0" class="opacity-40">↕</span>
                        </th>
                        <th scope="col" class="text-right cursor-pointer select-none" onclick="sortAccountTable(1)">
                            ${realizedProfitLabel} <span id="acc-sort-icon-1" class="opacity-40">↕</span>
                        </th>
                        <th scope="col" class="text-right hidden sm:table-cell cursor-pointer select-none" onclick="sortAccountTable(2)">
                            ${returnRateLabel} <span id="acc-sort-icon-2" class="opacity-40">↕</span>
                        </th>
                        <th scope="col" class="text-right hidden sm:table-cell">${unrealizedProfitLabel}</th>
                    </tr>
                </thead>
                <tbody id="accountRealizedTbody">
                !{int[] accIdx = {0};}
                @for(Map.Entry<String, TradeProfit> entry : accountRealizedMap.entrySet())
                    !{accIdx[0]++;}
                    !{TradeProfit acc = entry.getValue();}
                    !{BigDecimal accSoldCost = acc.totalSellProceeds() != null && acc.realizedProfit() != null
                        ? acc.totalSellProceeds().subtract(acc.realizedProfit())
                        : BigDecimal.ZERO;}
                    !{double accRate = accSoldCost.compareTo(BigDecimal.ZERO) > 0 && acc.realizedProfit() != null
                        ? acc.realizedProfit().doubleValue() / accSoldCost.doubleValue() * 100
                        : 0.0;}
                    <tr class="hover account-row" data-initial="${accIdx[0]}">
                        <td data-val="${entry.getKey()}">
                            <div class="font-semibold"><a href="/stock/account?accountId=${realizedAccountIdByName.get(entry.getKey()) != null ? realizedAccountIdByName.get(entry.getKey()).toString() : ""}" class="link link-hover">${entry.getKey()}</a></div>
                            <div class="text-xs opacity-50">${stockMenuLabel}</div>
                        </td>
                        <td class="text-right font-bold font-mono amount-value ${acc.realizedProfit() != null && acc.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}"
                            data-val="$unsafe{acc.realizedProfit() != null ? acc.realizedProfit().toPlainString() : "0"}">
                            ${String.format("%+,d", acc.realizedProfit() != null ? acc.realizedProfit().longValue() : 0)}
                        </td>
                        <td class="text-right font-mono hidden sm:table-cell font-semibold ${accRate >= 0 ? "text-profit" : "text-loss"}"
                            data-val="$unsafe{String.format("%.6f", accRate)}">
                            ${String.format("%+.2f%%", accRate)}
                        </td>
                        <td class="text-right font-mono hidden sm:table-cell amount-value ${acc.evaluationProfit() != null && acc.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", acc.evaluationProfit() != null ? acc.evaluationProfit().longValue() : 0)}
                        </td>
                    </tr>
                @endfor
                </tbody>
                <tfoot class="bg-base-200 font-bold">
                    !{BigDecimal totalAccountRealized = accountRealizedMap.values().stream()
                        .map(acc -> acc.realizedProfit() != null ? acc.realizedProfit() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);}
                    <tr>
                        <td>${totalLabel}</td>
                        <td class="text-right font-mono amount-value ${totalAccountRealized.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", totalAccountRealized.longValue())}
                        </td>
                        <td class="hidden sm:table-cell"></td>
                        <td class="hidden sm:table-cell"></td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>

    <!-- Section 3: 종목별 실현손익 -->
    <div class="mt-6">
        <div class="flex items-center justify-between mb-3">
            <h2 class="text-lg font-semibold opacity-70">${byStockSectionLabel}
                <span class="text-xs font-normal opacity-60">(${byStockSectionSubtitle})</span>
            </h2>
            @if(stockRealizedList != null && stockRealizedList.size() > 10)
            <button id="toggleStockTable" data-expanded="false" onclick="toggleStockRows()"
                class="btn btn-xs btn-ghost opacity-70">
                ${showAllLabel} ▼
            </button>
            @endif
        </div>
        <div class="card bg-base-100 border border-base-200 rounded-box shadow-sm overflow-x-auto">
            <table id="stockRealizedTable" class="table table-sm table-zebra w-full">
                <thead class="bg-base-200">
                    <tr>
                        <th scope="col" class="cursor-pointer select-none" onclick="sortStockTable(0)">
                            ${stockNameLabel} <span id="sort-icon-0" class="opacity-40">↕</span>
                        </th>
                        <th scope="col" class="text-right cursor-pointer select-none" onclick="sortStockTable(1)">
                            ${realizedProfitLabel} <span id="sort-icon-1" class="opacity-40">↕</span>
                        </th>
                        <th scope="col" class="text-right hidden sm:table-cell cursor-pointer select-none" onclick="sortStockTable(2)">
                            ${returnRateLabel} <span id="sort-icon-2" class="opacity-40">↕</span>
                        </th>
                        <th scope="col" class="text-right hidden md:table-cell cursor-pointer select-none" onclick="sortStockTable(3)">
                            ${soldCostColumnLabel} <span id="sort-icon-3" class="opacity-40">↕</span>
                        </th>
                        <th scope="col" class="text-right hidden lg:table-cell">${totalFeesLabel}</th>
                        <th scope="col" class="text-right hidden xl:table-cell">${sellQuantityLabel}</th>
                    </tr>
                </thead>
                <tbody id="stockRealizedTbody">
                    @if(stockRealizedList == null || stockRealizedList.isEmpty())
                        <tr>
                            <td colspan="6">@template._components.ui.emptyState(message = noRealizedHistoryLabel)</td>
                        </tr>
                    @else
                        !{int[] rowIdx = {0};}
                        @for(TradeProfit item : stockRealizedList)
                            !{rowIdx[0]++;}
                            !{BigDecimal itemFees = BigDecimal.ZERO
                                .add(item.totalBuyFee() != null ? item.totalBuyFee() : BigDecimal.ZERO)
                                .add(item.totalSellFee() != null ? item.totalSellFee() : BigDecimal.ZERO)
                                .add(item.totalSellTax() != null ? item.totalSellTax() : BigDecimal.ZERO);}
                            !{BigDecimal itemSoldCost = item.totalSellProceeds() != null && item.realizedProfitNet() != null
                                ? item.totalSellProceeds().subtract(item.realizedProfitNet())
                                : BigDecimal.ZERO;}
                            !{double itemRate = itemSoldCost.compareTo(BigDecimal.ZERO) > 0 && item.realizedProfitNet() != null
                                ? item.realizedProfitNet().doubleValue() / itemSoldCost.doubleValue() * 100
                                : 0.0;}
                            
                            <tr class="hover stock-row" data-initial="${rowIdx[0]}">
                                <td data-val="${item.stockItemName() != null ? item.stockItemName() : ""}">
                                    <div class="font-semibold"><a href="/stock/item?stockItemId=${item.stockItemId() != null ? item.stockItemId().toString() : ""}" class="link link-hover">${item.stockItemName() != null ? item.stockItemName() : "-"}</a></div>
                                    <div class="flex items-center gap-1 mt-0.5">
                                        @if(item.holdingQuantity() == 0)
                                            <span class="badge badge-xs badge-ghost opacity-60">${fullySoldLabel}</span>
                                        @else
                                            <span class="badge badge-xs badge-outline opacity-60">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.realized.badge.holding"), item.holdingQuantity())}</span>
                                        @endif
                                    </div>
                                </td>
                                <td class="text-right" data-val="$unsafe{item.realizedProfitNet() != null ? item.realizedProfitNet().toPlainString() : "0"}">
                                    <div class="font-bold font-mono amount-value ${item.realizedProfitNet() != null && item.realizedProfitNet().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                        ${String.format("%+,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}
                                    </div>
                                    
                                </td>
                                <td class="text-right font-mono hidden sm:table-cell font-semibold ${itemRate >= 0 ? "text-profit" : "text-loss"}"
                                    data-val="$unsafe{String.format("%.6f", itemRate)}">
                                    ${String.format("%+.2f%%", itemRate)}
                                </td>
                                <td class="text-right font-mono hidden md:table-cell amount-value opacity-70"
                                    data-val="$unsafe{itemSoldCost.toPlainString()}">
                                    ${String.format("%,d", itemSoldCost.longValue())}
                                </td>
                                <td class="text-right font-mono hidden lg:table-cell amount-value text-warning opacity-80">
                                    ${String.format("%,d", itemFees.longValue())}
                                </td>
                                <td class="text-right font-mono hidden xl:table-cell opacity-70">
                                    <span class="amount-value">${item.totalSellQuantity()}${sharesLabel}</span>
                                </td>
                            </tr>
                        @endfor
                    @endif
                </tbody>
                <tfoot class="bg-base-200 font-bold">
                    !{BigDecimal totalStockFees = stockRealizedList != null
                        ? stockRealizedList.stream().map(i -> {
                            BigDecimal f = BigDecimal.ZERO;
                            if (i.totalBuyFee() != null) f = f.add(i.totalBuyFee());
                            if (i.totalSellFee() != null) f = f.add(i.totalSellFee());
                            if (i.totalSellTax() != null) f = f.add(i.totalSellTax());
                            return f;
                          }).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;}
                    <tr>
                        <td>${totalLabel}</td>
                        <td class="text-right font-mono amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", totalRealizedProfit != null ? totalRealizedProfit.longValue() : 0)}
                        </td>
                        <td class="hidden sm:table-cell"></td>
                        <td class="text-right font-mono hidden md:table-cell amount-value opacity-70">
                            ${String.format("%,d", totalSoldCost.longValue())}
                        </td>
                        <td class="text-right font-mono hidden lg:table-cell amount-value text-warning opacity-80">
                            ${String.format("%,d", totalStockFees.longValue())}
                        </td>
                        <td class="hidden xl:table-cell"></td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>

    <script>
    (function() {
        var sortState = { col: -1, asc: false };
        var INITIAL_SHOW = 10;
        var compareLocale = window.StockCharts ? window.StockCharts.getLocale() : undefined;
        var showAllLabel = '${showAllLabel}';
        var collapseLabel = '${collapseLabel}';

        function applyVisibility() {
            var rows = document.querySelectorAll('#stockRealizedTbody .stock-row');
            var btn = document.getElementById('toggleStockTable');
            var expanded = btn && btn.dataset.expanded === 'true';
            rows.forEach(function(r, i) {
                r.classList.toggle('hidden', !expanded && i >= INITIAL_SHOW);
            });
        }

        window.toggleStockRows = function() {
            var btn = document.getElementById('toggleStockTable');
            if (!btn) return;
            var expanded = btn.dataset.expanded === 'true';
            btn.dataset.expanded = expanded ? 'false' : 'true';
            btn.textContent = (!expanded) ? (collapseLabel + ' ▲') : (showAllLabel + ' ▼');
            applyVisibility();
        };

        window.sortStockTable = function(colIdx) {
            var tbody = document.getElementById('stockRealizedTbody');
            if (!tbody) return;
            var rows = Array.from(tbody.querySelectorAll('.stock-row'));
            var asc;
            if (sortState.col === colIdx) {
                if (sortState.asc === false) { asc = true; }
                else { asc = null; } // 세 번째 클릭: 원래 순서 복원
            } else {
                asc = false; // 첫 클릭: 내림차순
            }
            // 아이콘 초기화
            [0,1,2,3].forEach(function(i) {
                var el = document.getElementById('sort-icon-' + i);
                if (el) { el.textContent = '↕'; el.className = 'opacity-40'; }
            });
            if (asc === null) {
                // 원래 순서
                rows.sort(function(a, b) {
                    return parseInt(a.dataset.initial) - parseInt(b.dataset.initial);
                });
                sortState = { col: -1, asc: false };
            } else {
                var icon = document.getElementById('sort-icon-' + colIdx);
                if (icon) { icon.textContent = asc ? '↑' : '↓'; icon.className = 'opacity-80'; }
                rows.sort(function(a, b) {
                    var tds_a = a.querySelectorAll('td');
                    var tds_b = b.querySelectorAll('td');
                    var va = tds_a[colIdx] ? tds_a[colIdx].dataset.val || '' : '';
                    var vb = tds_b[colIdx] ? tds_b[colIdx].dataset.val || '' : '';
                    if (colIdx === 0) {
                        return asc ? va.localeCompare(vb, compareLocale) : vb.localeCompare(va, compareLocale);
                    }
                    return asc ? parseFloat(va) - parseFloat(vb) : parseFloat(vb) - parseFloat(va);
                });
                sortState = { col: colIdx, asc: asc };
            }
            rows.forEach(function(r) { tbody.appendChild(r); });
            applyVisibility();
        };

        // 초기 접기 상태 적용
        applyVisibility();
    })();

    var accSortState = { col: -1, asc: false };

    window.sortAccountTable = function(colIdx) {
        var tbody = document.getElementById('accountRealizedTbody');
        if (!tbody) return;
        var rows = Array.from(tbody.querySelectorAll('.account-row'));
        var asc;
        if (accSortState.col === colIdx) {
            if (accSortState.asc === false) { asc = true; }
            else { asc = null; }
        } else {
            asc = false;
        }
        [0,1,2].forEach(function(i) {
            var el = document.getElementById('acc-sort-icon-' + i);
            if (el) { el.textContent = '↕'; el.className = 'opacity-40'; }
        });
        if (asc === null) {
            rows.sort(function(a, b) {
                return parseInt(a.dataset.initial) - parseInt(b.dataset.initial);
            });
            accSortState = { col: -1, asc: false };
        } else {
            var icon = document.getElementById('acc-sort-icon-' + colIdx);
            if (icon) { icon.textContent = asc ? '↑' : '↓'; icon.className = 'opacity-80'; }
            rows.sort(function(a, b) {
                var tds_a = a.querySelectorAll('td');
                var tds_b = b.querySelectorAll('td');
                var va = tds_a[colIdx] ? tds_a[colIdx].dataset.val || '' : '';
                var vb = tds_b[colIdx] ? tds_b[colIdx].dataset.val || '' : '';
                if (colIdx === 0) {
                        return asc ? va.localeCompare(vb, compareLocale) : vb.localeCompare(va, compareLocale);
                }
                return asc ? parseFloat(va) - parseFloat(vb) : parseFloat(vb) - parseFloat(va);
            });
            accSortState = { col: colIdx, asc: asc };
        }
        rows.forEach(function(r) { tbody.appendChild(r); });
    };
    </script>

    <script>
    (function() {
        var realizedProfitPicker = DateRangePicker.create({
            formId:      'realizedProfitSearchForm',
            startId:     'realizedStartDateInput',
            endId:       'realizedEndDateInput',
            instantStartId: 'realizedStartInstantInput',
            instantEndId: 'realizedEndInstantInput',
            timeZoneId:  'realizedTimeZoneInput',
            rangeModeId: 'realizedRangeModeInput',
            btnClass:    'date-range-btn',
            minDate:     '${dataFirstDate != null ? dataFirstDate : ""}',
            globalKey:   'globalDateRange',
            rootSelector: '#realizedProfitFragment'
        });
        window.realizedProfitPicker = realizedProfitPicker;
        // Prevent infinite auto-submit loops: if the DateRangePicker already
        // recorded a one-time apply for this form and it matches current
        // globalDateRange, skip auto-apply. Otherwise continue and re-apply.
        try {
            var __appliedKey = '__globalDateRangeApplied:' + ('realizedProfitSearchForm');
            var __existingApplied = null;
            try { __existingApplied = sessionStorage.getItem(__appliedKey); } catch(e) {}
            var __currentGlobal = null;
            try { __currentGlobal = sessionStorage.getItem('globalDateRange'); } catch(e) {}
            if (__existingApplied && __currentGlobal && __existingApplied === __currentGlobal) {
                try { console.debug('[realized-profit] appliedKey matches current global, skipping auto-apply'); } catch(e) {}
                return;
            }
        } catch(e) {}
        // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
        try {
            if (window.realizedProfitPicker && typeof window.realizedProfitPicker.getState === 'function') {
                var __s = window.realizedProfitPicker.getState();
                if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
                    var __key = 'dateRangeDefaultApplied:realizedProfitPicker';
                    try {
                        if (!sessionStorage.getItem(__key)) {
                                var _btn = document.querySelector('#realizedProfitFragment .date-range-btn');
                                window.realizedProfitPicker.set('mtd', _btn || null);
                                sessionStorage.setItem(__key, '1');
                            }
                    } catch(e) { window.realizedProfitPicker.set('mtd', null); }
                }
            }
        } catch(e) {}

        // Ensure any globally requested range (from other fragments) is applied
        // when this fragment loads. This forces the server-side search to use
        // the same requested start/end instead of falling back to server defaults.
        try {
            var __raw = (typeof sessionStorage !== 'undefined') ? sessionStorage.getItem('globalDateRange') : null;
            if (__raw) {
                try {
                    var __g = JSON.parse(__raw);
                    if (__g && (__g.start || __g.end || __g.mode)) {
                        try { console.debug('[realized-profit] globalDateRange found on load', __g); } catch(e) {}
                        var __cur = window.realizedProfitPicker && typeof window.realizedProfitPicker.getState === 'function' ? window.realizedProfitPicker.getState() : { start: '', end: '', mode: '' };
                        var needApply = (__cur.start !== (__g.start || '') || __cur.end !== (__g.end || '') || __cur.mode !== (__g.mode || ''));
                        // If the server-rendered header range differs from the requested
                        // global range, force a submit even if input values already match
                        // (some fragments are rendered with label-snapped dates on server).
                        var headerRangeText = null;
                        try {
                            var spans = document.querySelectorAll('#realizedProfitFragment span');
                            for (var i = 0; i < spans.length; i++) {
                                var t = (spans[i].textContent || '').trim();
                                if (/\d{4}-\d{2}-\d{2}\s*~\s*\d{4}-\d{2}-\d{2}/.test(t)) { headerRangeText = t; break; }
                            }
                        } catch (e) {}
                        var headerStart = '', headerEnd = '';
                        try {
                            if (headerRangeText) {
                                var mm = headerRangeText.match(/(\d{4}-\d{2}-\d{2})\s*~\s*(\d{4}-\d{2}-\d{2})/);
                                if (mm) { headerStart = mm[1]; headerEnd = mm[2]; }
                            }
                        } catch (e) {}
                        var forceApply = false;
                        try {
                            if (headerStart || headerEnd) {
                                if (headerStart !== (__g.start || '') || headerEnd !== (__g.end || '')) {
                                    forceApply = true;
                                }
                            }
                        } catch (e) {}

                        var shouldApply = needApply || forceApply;
                        try { console.debug('[realized-profit] needApply=%o forceApply=%o header=%o', needApply, forceApply, headerRangeText); } catch(e) {}
                        if (shouldApply) {
                            try { console.debug('[realized-profit] applying globalDateRange to form and submitting', __g); } catch(e) {}
                            var sEl = document.getElementById('realizedStartDateInput');
                            var eEl = document.getElementById('realizedEndDateInput');
                            var mEl = document.getElementById('realizedRangeModeInput');
                            if (sEl) sEl.value = __g.start || '';
                            if (eEl) eEl.value = __g.end || '';
                            if (mEl) mEl.value = __g.mode || '';
                            try {
                                function localDateToInstantIso(ds, addDays) {
                                    if (!ds) return "";
                                    var parts = ds.split("-");
                                    var y = Number.parseInt(parts[0], 10);
                                    var m = Number.parseInt(parts[1], 10) - 1;
                                    var d = Number.parseInt(parts[2], 10);
                                    var dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
                                    return dt.toISOString();
                                }
                                var instS = document.getElementById('realizedStartInstantInput');
                                var instE = document.getElementById('realizedEndInstantInput');
                                var tzEl = document.getElementById('realizedTimeZoneInput');
                                if (instS) instS.value = __g.start ? localDateToInstantIso(__g.start, 0) : '';
                                if (instE) instE.value = __g.end ? localDateToInstantIso(__g.end, 1) : '';
                                try {
                                    var tz = (typeof Intl !== 'undefined' && Intl.DateTimeFormat) ? Intl.DateTimeFormat().resolvedOptions().timeZone : null;
                                    if (tzEl && tz) tzEl.value = tz;
                                    else if (tzEl && __g.timeZone) tzEl.value = __g.timeZone;
                                } catch (ee) {}
                            } catch (e) {}
                            var form = document.getElementById('realizedProfitSearchForm');
                            // Prevent repeated auto-submits: mark this fragment as applied
                            // before submitting so subsequent fragment loads won't re-trigger.
                            try {
                                var __appliedKey = '__globalDateRangeApplied:realizedProfitSearchForm';
                                try { sessionStorage.setItem(__appliedKey, __raw || JSON.stringify(__g)); } catch(e) {}
                            } catch(e) {}
                            if (form) {
                                try {
                                    try { console.debug('[realized-profit] submitting form values', {start: sEl && sEl.value, end: eEl && eEl.value, instantStart: instS && instS.value, instantEnd: instE && instE.value, mode: mEl && mEl.value}); } catch(e) {}
                                    if (typeof form.requestSubmit === 'function') {
                                        form.requestSubmit();
                                    } else {
                                        try {
                                            var ev = new Event('submit', { bubbles: true, cancelable: true });
                                            var prevented = !form.dispatchEvent(ev);
                                            if (!prevented) {
                                                var tmp = document.createElement('button');
                                                tmp.type = 'submit';
                                                tmp.style.display = 'none';
                                                form.appendChild(tmp);
                                                tmp.click();
                                                tmp.remove();
                                            }
                                        } catch (e2) {
                                            try { form.submit(); } catch (e3) {}
                                        }
                                    }
                                } catch(e) { try { form.submit(); } catch(_) {} }
                            }
                        }
                    }
                } catch(e) {}
            }
        } catch(e) {}
    })();
    </script>

</div>
