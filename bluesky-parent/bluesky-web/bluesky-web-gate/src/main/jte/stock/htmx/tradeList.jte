@import java.util.List
@import java.util.Map
@import java.util.UUID
@import java.time.Instant
@import java.time.ZoneId
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.common.menu.domain.Pagination
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.AccountRealizedRow
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradeResponse> tradeList
@param List<TradeResponse> allTradeList = java.util.List.of()
@param Pagination pagination
@param int totalItems
@param int totalPages
@param int currentPage
@param int size
@param List<Account> accountList
@param List<StockItem> stockItemList
@param List<StockItem> tagCountStockItemList = stockItemList
@param List<String> stockTagList = java.util.List.of()
@param Map<UUID, String> accountNames
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param List<UUID> selectedAccountIds = java.util.List.of()
@param List<UUID> selectedStockItemIds = java.util.List.of()
@param List<String> selectedStockTags = java.util.List.of()
@param java.time.Instant startDate
@param java.time.Instant endDate
@param String timeZone = null
@param String sort
@param BigDecimal totalFee
@param BigDecimal totalTax
@param BigDecimal totalRealizedProfit
@param BigDecimal totalAllBuyAmount = BigDecimal.ZERO
@param BigDecimal totalAllSellAmount = BigDecimal.ZERO
@param BigDecimal totalAllFee = BigDecimal.ZERO
@param BigDecimal totalAllTax = BigDecimal.ZERO
@param BigDecimal totalAllRealizedProfit = BigDecimal.ZERO
@param String rangeMode = null
@param String dataFirstDate = null
@param List<AccountRealizedRow> accountRealizedList = java.util.List.of()
@param List<TradeProfit> stockRealizedList = java.util.List.of()
@param long realizedWinCount = 0L
@param int realizedStockCount = 0

!{
    DecimalFormat decimalFormat = new DecimalFormat("#,##0");
    java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);

    // 여러 섹션이 공유하는 라벨 (하위 컴포넌트에 param 으로 전달)
    String accountLabel = MessageUtil.getMessage("common.label.account");
    String stockLabel = MessageUtil.getMessage("common.label.stock");
    String buyLabel = MessageUtil.getMessage("stock.trade.label.buy");
    String sellLabel = MessageUtil.getMessage("stock.trade.label.sell");
    String realizedProfitLabel = MessageUtil.getMessage("stock.trade.label.realized.profit");
    String feeLabel = MessageUtil.getMessage("stock.trade.label.fee");
    String tradingTaxLabel = MessageUtil.getMessage("stock.trade.label.trading.tax");
    String buyConcentrationTitle = MessageUtil.getMessage("stock.trade.chart.title.buy.concentration");
    String monthlyTradeAmountTitle = MessageUtil.getMessage("stock.trade.chart.title.monthly.trade.amount");

    // Build JSON for charts
    java.util.function.Function<String, String> ej = s ->
        s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    StringBuilder jsSb = new StringBuilder();
    jsSb.append("const tradeData=[");
    List<TradeResponse> chartList = allTradeList != null ? allTradeList : java.util.List.of();
    for (int ci = 0; ci < chartList.size(); ci++) {
        TradeResponse t = chartList.get(ci);
        if (ci > 0) jsSb.append(",");
        // Use the user's timezone (if provided) when rendering trade dates in JS payload
        java.time.ZoneId _zone = (timeZone != null && !timeZone.isEmpty()) ? java.time.ZoneId.of(timeZone) : java.time.ZoneId.systemDefault();
        jsSb.append("{tradeDate:\"").append(t.tradeDate() != null ? ymd.format(t.tradeDate().atZone(_zone)) : "").append("\"");
        jsSb.append(",stockItem:\"").append(ej.apply(t.stockItemName())).append("\"");
        jsSb.append(",account:\"").append(ej.apply(accountNames != null ? accountNames.getOrDefault(t.accountId(), "") : "")).append("\"");
        jsSb.append(",type:\"").append(t.type() != null ? t.type().name() : "").append("\"");
        jsSb.append(",amount:").append(t.amount() != null ? t.amount().toPlainString() : "0");
        jsSb.append(",profit:").append(t.realizedProfit() != null ? t.realizedProfit().toPlainString() : "0");
        jsSb.append("}");
    }
    jsSb.append("];");
    String tradeDataJs = jsSb.toString();

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
}

<div id="tradeListFragment">

    @template.stock.htmx.fragments.dateRangeNavBar(
        pickerName = "tradePicker",
        btnClass = "date-range-btn",
        canPrev = canPrev,
        canNext = canNext,
        rangeMode = rangeMode,
        startDate = startLocal,
        endDate = endLocal
    )

    @template.stock.htmx.fragments.trade.tradeSearchForm(
        accountList = accountList,
        stockItemList = stockItemList,
        tagCountStockItemList = tagCountStockItemList,
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
        stockLabel = stockLabel
    )

    @template.stock.htmx.fragments.trade.tradeSummaryCards(
        allTradeList = allTradeList,
        totalItems = totalItems,
        totalAllBuyAmount = totalAllBuyAmount,
        totalAllSellAmount = totalAllSellAmount,
        totalAllFee = totalAllFee,
        totalAllTax = totalAllTax,
        totalAllRealizedProfit = totalAllRealizedProfit,
        decimalFormat = decimalFormat,
        countMessage = countMessage,
        buyLabel = buyLabel,
        sellLabel = sellLabel,
        realizedProfitLabel = realizedProfitLabel,
        feeLabel = feeLabel,
        tradingTaxLabel = tradingTaxLabel
    )

    <!-- Charts Row -->
    <div class="flex flex-wrap gap-4 mb-4 overflow-visible">

        @template.stock.htmx.fragments.components.doughnutShell(
            title = buyConcentrationTitle,
            canvasId = "tradeDonutChart",
            legendId = "tradeDonutLegend",
            titleId = "tradeDonutTitle",
            metricProfitId = "tradeDonutMetricProfit",
            metricBuyId = "tradeDonutMetricBuy",
            tabStockId = "tradeDonutTabStock",
            tabAccountId = "tradeDonutTabAccount"
        )

        @template.stock.htmx.fragments.components.barShell(
            title = monthlyTradeAmountTitle,
            canvasId = "tradeMonthlyChart"
        )

    </div>

    @template.stock.htmx.fragments.trade.tradeRealizedSections(
        accountRealizedList = accountRealizedList,
        stockRealizedList = stockRealizedList,
        realizedWinCount = realizedWinCount,
        realizedStockCount = realizedStockCount,
        decimalFormat = decimalFormat,
        accountLabel = accountLabel,
        stockLabel = stockLabel,
        realizedProfitLabel = realizedProfitLabel
    )

    @template.stock.htmx.fragments.trade.tradeDetailList(
        tradeList = tradeList,
        pagination = pagination,
        totalItems = totalItems,
        accountNames = accountNames,
        sort = sort,
        totalFee = totalFee,
        totalTax = totalTax,
        totalRealizedProfit = totalRealizedProfit,
        decimalFormat = decimalFormat,
        countMessage = countMessage,
        accountLabel = accountLabel,
        stockLabel = stockLabel,
        realizedProfitLabel = realizedProfitLabel,
        feeLabel = feeLabel,
        tradingTaxLabel = tradingTaxLabel,
        buyLabel = buyLabel,
        sellLabel = sellLabel
    )

    <script>
    (function() {
        $unsafe{tradeDataJs}

        window.tradeMonthlyChartInst = null;
        window.tradeDonutChartInst = null;
        window.currentMetric = 'buy';
        window.currentGroupBy = 'stock';

        window.switchTradeDonut = function(metric, groupBy) {
            if (metric) window.currentMetric = metric;
            if (groupBy) window.currentGroupBy = groupBy;
            var elProfit = document.getElementById('tradeDonutMetricProfit');
            var elBuy = document.getElementById('tradeDonutMetricBuy');
            var elStock = document.getElementById('tradeDonutTabStock');
            var elAccount = document.getElementById('tradeDonutTabAccount');
            if (elProfit) elProfit.classList.toggle('tab-active', window.currentMetric === 'profit');
            if (elBuy) elBuy.classList.toggle('tab-active', window.currentMetric === 'buy');
            if (elStock) elStock.classList.toggle('tab-active', window.currentGroupBy === 'stock');
            if (elAccount) elAccount.classList.toggle('tab-active', window.currentGroupBy === 'account');

            if (window.StockCharts) {
                window.tradeDonutChartInst = window.StockCharts.initDonutFromData(tradeData, {
                    canvasId: 'tradeDonutChart',
                    legendId: 'tradeDonutLegend',
                    titleId: 'tradeDonutTitle',
                    metric: window.currentMetric,
                    groupBy: window.currentGroupBy,
                    tooltipId: 'tradeDonutTooltipEl'
                }, window.tradeDonutChartInst);
            }
        };

        function initAll() {
            if (!window.StockCharts) return;
            window.tradeMonthlyChartInst = window.StockCharts.initMonthlyFromData(tradeData, 'tradeMonthlyChart', window.tradeMonthlyChartInst);
            window.tradeDonutChartInst = window.StockCharts.initDonutFromData(tradeData, {
                canvasId: 'tradeDonutChart',
                legendId: 'tradeDonutLegend',
                titleId: 'tradeDonutTitle',
                metric: window.currentMetric,
                groupBy: window.currentGroupBy,
                tooltipId: 'tradeDonutTooltipEl'
            }, window.tradeDonutChartInst);
        }

        window.tradePicker = window.DateRangePicker
            ? window.DateRangePicker.create({
                formId:      'tradeSearchForm',
                startId:     'tradeStartDateInput',
                endId:       'tradeEndDateInput',
                instantStartId: 'tradeStartInstantInput',
                instantEndId:   'tradeEndInstantInput',
                timeZoneId:     'tradeTimeZoneInput',
                rangeModeId: 'tradeRangeModeInput',
                btnClass:    'date-range-btn',
                minDate:     '${dataFirstDate != null ? dataFirstDate : ""}',
                globalKey:   'globalDateRange',
                rootSelector: '#tradeListFragment'
            })
            : { set: function(){}, shift: function(){}, jumpToEdge: function(){} };
        // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
        try {
            if (window.tradePicker && typeof window.tradePicker.getState === 'function') {
                var __s = window.tradePicker.getState();
                if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
                    var __key = 'dateRangeDefaultApplied:tradePicker';
                    try {
                        if (!sessionStorage.getItem(__key)) {
                            var _btn = document.querySelector('#tradeListFragment .date-range-btn');
                            window.tradePicker.set('mtd', _btn || null);
                            sessionStorage.setItem(__key, '1');
                        }
                    } catch(e) { window.tradePicker.set('mtd', null); }
                }
            }
        } catch(e) {}
        // Chart.js / StockCharts 는 stockLayout 에서 1회 로드되어 항상 존재한다. (initAll 내부에서도 자체 가드)
        setTimeout(initAll, 0);
        setTimeout(function() {
            if (window.tradeMonthlyChartInst && window.tradeMonthlyChartInst.resize) window.tradeMonthlyChartInst.resize();
            if (window.tradeDonutChartInst && window.tradeDonutChartInst.resize) window.tradeDonutChartInst.resize();
        }, 200);

        const detail = document.getElementById('tradeDetailSection');
        if (detail) {
            const arrow = detail.querySelector('.detail-arrow');
            detail.addEventListener('toggle', function() {
                if (arrow) arrow.style.transform = detail.open ? 'rotate(90deg)' : '';
            });
        }

        // 페이징 클릭 시 fragment 상단 스크롤
        if (window.__pendingScrollTo === 'tradeListFragment') {
            delete window.__pendingScrollTo;
            var frag = document.getElementById('tradeListFragment');
            if (frag) frag.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
        document.querySelectorAll('#tradeListFragment .join button[hx-get]').forEach(function(btn) {
            btn.addEventListener('click', function() { window.__pendingScrollTo = 'tradeListFragment'; });
        });
    })();
    </script>
</div>
