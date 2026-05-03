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
@import net.luversof.web.gate.stock.constant.TradeType
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat

@param List<TradeResponse> tradeList
@param List<TradeResponse> allTradeList = java.util.List.of()
@param Pagination pagination
@param int totalItems
@param int totalPages
@param int currentPage
@param int size
@param List<Account> accountList
@param List<StockItem> stockItemList
@param Map<UUID, String> accountNames
@param UUID selectedAccountId
@param UUID selectedStockItemId
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

!{
    String sortField = sort != null ? sort.split(",")[0] : "";
    String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DecimalFormat decimalFormat = new DecimalFormat("#,##0");

    BigDecimal safeBuy = totalAllBuyAmount != null ? totalAllBuyAmount : BigDecimal.ZERO;
    BigDecimal safeSell = totalAllSellAmount != null ? totalAllSellAmount : BigDecimal.ZERO;
    BigDecimal safeFee = totalAllFee != null ? totalAllFee : BigDecimal.ZERO;
    BigDecimal safeTax = totalAllTax != null ? totalAllTax : BigDecimal.ZERO;
    BigDecimal safeRp = totalAllRealizedProfit != null ? totalAllRealizedProfit : BigDecimal.ZERO;
    boolean rpPositive = safeRp.compareTo(BigDecimal.ZERO) >= 0;

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

    <!-- Filter & Search Form (collapsible) -->
    <details class="mb-4"$unsafe{(selectedAccountId != null || selectedStockItemId != null || startDate != null) ? " open" : ""}>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            상세 검색
            @template.stock.htmx.fragments.components.filterBadge(
                selectedAccountId = selectedAccountId,
                selectedStockItemId = selectedStockItemId,
                startDate = startDate,
                endDate = endDate
            )
        </summary>
    <form id="tradeSearchForm" class="flex flex-wrap gap-2 mt-2 mb-2 items-end bg-base-100 p-2 rounded-box shadow-sm"
          hx-get="/stock/htmx/trade/list"
          hx-target="#tradeListFragment"
          hx-select="#tradeListFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">기간</span></label>
            <div class="join">
                <input type="date" name="tradeUiStartDate" id="tradeStartDateInput" class="input input-sm input-bordered join-item" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
                <input type="date" name="tradeUiEndDate" id="tradeEndDateInput" class="input input-sm input-bordered join-item" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
            </div>
        </div>
        <input type="hidden" name="startDate" id="tradeStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
        <input type="hidden" name="endDate" id="tradeEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
        <input type="hidden" name="timeZone" id="tradeTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
        <input type="hidden" name="rangeMode" id="tradeRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">계좌</span></label>
            <select name="accountIdList" class="select-bordered w-full max-h-52 overflow-auto" multiple data-max-visible="6">
                <option value="">전체</option>
                @for(Account account : accountList)
                <option value="${account.id().toString()}" selected="${account.id().equals(selectedAccountId)}">${account.name()}</option>
                @endfor
            </select>
        </div>

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">종목</span></label>
            <select name="stockItemIdList" class="select-bordered w-full max-h-52 overflow-auto" multiple data-max-visible="6">
                <option value="">전체</option>
                @for(StockItem stockItem : stockItemList)
                <option value="${stockItem.id().toString()}" selected="${stockItem.id().equals(selectedStockItemId)}">${stockItem.name()}</option>
                @endfor
            </select>
        </div>

        <button type="button" class="btn btn-sm btn-primary" data-selection-save="true"
            hx-get="/stock/htmx/trade/list"
            hx-target="#tradeListFragment"
            hx-select="#tradeListFragment"
            hx-swap="outerHTML"
            hx-include="#tradeSearchForm">조회</button>
        <button type="button" class="btn btn-sm text-xs"
            hx-get="/stock/htmx/trade/list"
            hx-target="#tradeListFragment"
            hx-select="#tradeListFragment"
                hx-swap="outerHTML"
                hx-include="#tradeSearchForm"
            data-selection-reset="true">초기화</button>
    </form>
    </details>

    <script type="module" src="/js/stock/selectionStorage.js"></script>

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">

        <!-- Card 1: 매수/매도 금액 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">기간 매매 현황</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-error amount-value">&#8361; ${decimalFormat.format(safeBuy)}</span>
                    <span class="text-xs text-base-content/40">매수</span>
                </div>
                <div class="flex gap-4 text-xs text-base-content/50">
                    <span>매도 <span class="font-medium text-info amount-value">&#8361; ${decimalFormat.format(safeSell)}</span></span>
                    <span class="ml-auto">${totalItems}건</span>
                </div>
            </div>
        </div>

        <!-- Card 2: 실현손익 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">실현손익</div>
                <div class="text-xl font-bold ${rpPositive ? "text-success" : "text-error"} amount-value">
                    ${rpPositive ? "+" : ""}&#8361; ${decimalFormat.format(safeRp)}
                </div>
                !{
                    long sellCount = allTradeList != null ? allTradeList.stream()
                        .filter(t -> t.type() == net.luversof.web.gate.stock.constant.TradeType.SELL)
                        .count() : 0L;
                }
                <div class="text-xs text-base-content/40 mt-1">매도 ${sellCount}건</div>
            </div>
        </div>

        <!-- Card 3: 거래 비용 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">거래 비용</div>
                <div class="text-xl font-bold amount-value">&#8361; ${decimalFormat.format(safeFee.add(safeTax))}</div>
                <div class="flex gap-4 text-xs text-base-content/50 mt-1">
                    <span>수수료 <span class="font-medium text-base-content/70 amount-value">&#8361; ${decimalFormat.format(safeFee)}</span></span>
                    <span>거래세 <span class="font-medium text-base-content/70 amount-value">&#8361; ${decimalFormat.format(safeTax)}</span></span>
                </div>
            </div>
        </div>

    </div>

    <!-- Charts Row -->
    <div class="flex flex-wrap gap-4 mb-4 overflow-visible">

        @template.stock.htmx.fragments.components.doughnutShell(
            title = "매수 집중도",
            canvasId = "tradeDonutChart",
            legendId = "tradeDonutLegend",
            titleId = "tradeDonutTitle",
            metricProfitId = "tradeDonutMetricProfit",
            metricBuyId = "tradeDonutMetricBuy",
            tabStockId = "tradeDonutTabStock",
            tabAccountId = "tradeDonutTabAccount"
        )

        @template.stock.htmx.fragments.components.barShell(
            title = "월별 매매 금액",
            canvasId = "tradeMonthlyChart"
        )

    </div>

    <!-- Collapsible Detail List -->
    <details id="tradeDetailSection" class="mb-4">
        <summary class="cursor-pointer select-none flex items-center gap-2 font-semibold text-sm p-3 bg-base-200 rounded-box hover:bg-base-300 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 transition-transform detail-arrow" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            </svg>
            상세 목록 (${totalItems}건)
        </summary>

        <div id="tradeListContainer" class="flex flex-col relative mt-2">
            <div class="overflow-x-auto flex-1 min-h-0">
                <table class="table table-hover table-striped w-full table-sm table-pin-rows">
                    <thead>
                    <tr class="bg-base-200">
                        <th class="cursor-pointer hover:bg-base-300"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "tradeDate,${sortField.equals("tradeDate") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            날짜 @if(sortField.equals("tradeDate"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="cursor-pointer hover:bg-base-300"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "stockItemName,${sortField.equals("stockItemName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            종목 @if(sortField.equals("stockItemName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th>구분</th>
                        <th class="hidden sm:table-cell">계좌</th>
                        <th class="text-right">단가</th>
                        <th class="text-right">수량</th>
                        <th class="text-right cursor-pointer hover:bg-base-300"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "amount,${sortField.equals("amount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            금액 @if(sortField.equals("amount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "fee,${sortField.equals("fee") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            수수료 @if(sortField.equals("fee"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right hidden lg:table-cell">거래세</th>
                        <th class="text-right cursor-pointer hover:bg-base-300"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "realizedProfit,${sortField.equals("realizedProfit") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            실현손익 @if(sortField.equals("realizedProfit"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    @for(TradeResponse trade : tradeList)
                    <tr>
                        <td class="whitespace-nowrap">${trade.tradeDate() != null ? dateFormatter.format(trade.tradeDate().atZone(java.time.ZoneId.systemDefault())) : ""}</td>
                        <td class="font-bold whitespace-nowrap">${trade.stockItemName()}</td>
                        <td>
                            <span class="${trade.type() == TradeType.BUY ? "badge badge-error badge-outline badge-sm" : "badge badge-info badge-outline badge-sm"}">
                                ${trade.type() == TradeType.BUY ? "매수" : "매도"}
                            </span>
                        </td>
                        <td class="whitespace-nowrap hidden sm:table-cell">${accountNames != null ? accountNames.getOrDefault(trade.accountId(), "") : ""}</td>
                        <td class="text-right font-mono amount-value">${trade.price() != null ? decimalFormat.format(trade.price()) : "0"}</td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(trade.quantity())}</td>
                        <td class="text-right font-mono amount-value">${trade.amount() != null ? decimalFormat.format(trade.amount()) : "0"}</td>
                        <td class="text-right text-xs text-base-content/60 amount-value hidden lg:table-cell">${trade.fee() != null ? decimalFormat.format(trade.fee()) : "0"}</td>
                        <td class="text-right text-xs text-base-content/60 amount-value hidden lg:table-cell">${trade.tax() != null ? decimalFormat.format(trade.tax()) : "0"}</td>
                        <td class="text-right font-bold font-mono amount-value ${trade.realizedProfit() != null ? (trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-success" : "text-error") : ""}">
                            ${trade.realizedProfit() != null && trade.type() == TradeType.SELL ? decimalFormat.format(trade.realizedProfit()) : ""}
                        </td>
                    </tr>
                    @endfor
                    @if(tradeList == null || tradeList.isEmpty())
                    <tr>
                        <td colspan="10" class="text-center py-10 opacity-50">거래 내역이 없습니다.</td>
                    </tr>
                    @endif
                    </tbody>
                    <tfoot>
                        <tr class="font-bold bg-base-200">
                            <td colspan="6" class="text-right pr-4">합계</td>
                            <td class="text-right font-mono amount-value"></td>
                            <td class="text-right font-mono amount-value hidden lg:table-cell">${totalFee != null ? decimalFormat.format(totalFee) : "0"}</td>
                            <td class="text-right font-mono amount-value hidden lg:table-cell">${totalTax != null ? decimalFormat.format(totalTax) : "0"}</td>
                            <td class="text-right font-mono amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-success" : "text-error"}">
                                ${totalRealizedProfit != null ? decimalFormat.format(totalRealizedProfit) : "0"}
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>

            @if(pagination != null && pagination.getTotalPage() > 1)
            <div class="flex justify-center p-2 border-t border-base-200 bg-base-100">
                <div class="join">
                    <button class="join-item btn btn-sm ${!pagination.getFirstNav().isActive() ? "btn-disabled" : ""}"
                            disabled="${!pagination.getFirstNav().isActive()}"
                            hx-get="/stock/htmx/trade/list" hx-include="#tradeSearchForm"
                            hx-vals='{"page": ${pagination.getFirstNav().page()}}'
                            hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML">&#171;</button>
                    <button class="join-item btn btn-sm ${!pagination.getPrevNav().isActive() ? "btn-disabled" : ""}"
                            disabled="${!pagination.getPrevNav().isActive()}"
                            hx-get="/stock/htmx/trade/list" hx-include="#tradeSearchForm"
                            hx-vals='{"page": ${pagination.getPrevNav().page()}}'
                            hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML">&#12296;</button>
                    @for(Pagination.Nav nav : pagination.getNavList())
                    <button class="join-item btn btn-sm ${nav.isActive() ? "btn-active" : ""}"
                            hx-get="/stock/htmx/trade/list" hx-include="#tradeSearchForm"
                            hx-vals='{"page": ${nav.page()}}'
                            hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML">${nav.page()}</button>
                    @endfor
                    <button class="join-item btn btn-sm ${!pagination.getNextNav().isActive() ? "btn-disabled" : ""}"
                            disabled="${!pagination.getNextNav().isActive()}"
                            hx-get="/stock/htmx/trade/list" hx-include="#tradeSearchForm"
                            hx-vals='{"page": ${pagination.getNextNav().page()}}'
                            hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML">&#12297;</button>
                    <button class="join-item btn btn-sm ${!pagination.getLastNav().isActive() ? "btn-disabled" : ""}"
                            disabled="${!pagination.getLastNav().isActive()}"
                            hx-get="/stock/htmx/trade/list" hx-include="#tradeSearchForm"
                            hx-vals='{"page": ${pagination.getLastNav().page()}}'
                            hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML">&#187;</button>
                </div>
            </div>
            @endif
        </div>
    </details>

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
        if (window.Chart && window.StockCharts) {
            setTimeout(initAll, 0);
            setTimeout(function() {
                if (window.tradeMonthlyChartInst && window.tradeMonthlyChartInst.resize) window.tradeMonthlyChartInst.resize();
                if (window.tradeDonutChartInst && window.tradeDonutChartInst.resize) window.tradeDonutChartInst.resize();
            }, 200);
        } else {
            var s = document.createElement('script');
            s.src = 'https://cdn.jsdelivr.net/npm/chart.js';
            s.onload = function() {
                setTimeout(initAll, 0);
                setTimeout(function() {
                    if (window.tradeMonthlyChartInst && window.tradeMonthlyChartInst.resize) window.tradeMonthlyChartInst.resize();
                    if (window.tradeDonutChartInst && window.tradeDonutChartInst.resize) window.tradeDonutChartInst.resize();
                }, 200);
            };
            document.head.appendChild(s);
        }

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
