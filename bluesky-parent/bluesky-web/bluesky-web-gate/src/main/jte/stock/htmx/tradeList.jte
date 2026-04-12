@import java.util.List
@import java.util.Map
@import java.util.UUID
@import java.time.LocalDate
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
@param LocalDate startDate
@param LocalDate endDate
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
        jsSb.append("{tradeDate:\"").append(t.tradeDate() != null ? ymd.format(t.tradeDate().atZone(java.time.ZoneId.systemDefault())) : "").append("\"");
        jsSb.append(",stockItem:\"").append(ej.apply(t.stockItemName())).append("\"");
        jsSb.append(",account:\"").append(ej.apply(accountNames != null ? accountNames.getOrDefault(t.accountId(), "") : "")).append("\"");
        jsSb.append(",type:\"").append(t.type() != null ? t.type().name() : "").append("\"");
        jsSb.append(",amount:").append(t.amount() != null ? t.amount().toPlainString() : "0");
        jsSb.append(",profit:").append(t.realizedProfit() != null ? t.realizedProfit().toPlainString() : "0");
        jsSb.append("}");
    }
    jsSb.append("];");
    String tradeDataJs = jsSb.toString();
}

<div id="tradeListFragment">

    <!-- Quick Date Selector -->
    <div class="flex flex-wrap gap-2 mb-3 items-center">
        <span class="text-xs opacity-50 mr-1">기간</span>
        <button type="button" class="btn btn-xs btn-ghost" onclick="tradePicker.jumpToEdge('start')">&#171;</button>
        <button type="button" class="btn btn-xs btn-ghost" onclick="tradePicker.shift(-1)">&#8249; 이전</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("1") ? "btn-primary" : ""}" onclick="tradePicker.set(1,this)">1개월</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("3") ? "btn-primary" : ""}" onclick="tradePicker.set(3,this)">3개월</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("6") ? "btn-primary" : ""}" onclick="tradePicker.set(6,this)">6개월</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("ytd") ? "btn-primary" : ""}" onclick="tradePicker.set('ytd',this)">올해</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("12") ? "btn-primary" : ""}" onclick="tradePicker.set(12,this)">1년</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("36") ? "btn-primary" : ""}" onclick="tradePicker.set(36,this)">3년</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("all") ? "btn-primary" : ""}" onclick="tradePicker.set(0,this)">전체</button>
        <button type="button" class="btn btn-xs btn-ghost" onclick="tradePicker.shift(1)">다음 &#8250;</button>
        <button type="button" class="btn btn-xs btn-ghost" onclick="tradePicker.jumpToEdge('end')">&#187;</button>
    </div>

    <!-- Filter & Search Form (collapsible) -->
    <details class="mb-4"$unsafe{(selectedAccountId != null || selectedStockItemId != null || startDate != null) ? " open" : ""}>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            상세 검색
            @if(selectedAccountId != null || selectedStockItemId != null)
                <span class="badge badge-xs badge-primary">필터 적용 중</span>
            @endif
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
                <input type="date" name="startDate" id="tradeStartDateInput" class="input input-sm input-bordered join-item" value="${startDate != null ? startDate.toString() : ""}">
                <input type="date" name="endDate" id="tradeEndDateInput" class="input input-sm input-bordered join-item" value="${endDate != null ? endDate.toString() : ""}">
            </div>
        </div>
        <input type="hidden" name="rangeMode" id="tradeRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">계좌</span></label>
            <select name="accountIdList" class="select select-sm select-bordered">
                <option value="">전체</option>
                @for(Account account : accountList)
                <option value="${account.id().toString()}" selected="${account.id().equals(selectedAccountId)}">${account.name()}</option>
                @endfor
            </select>
        </div>

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">종목</span></label>
            <select name="stockItemIdList" class="select select-sm select-bordered">
                <option value="">전체</option>
                @for(StockItem stockItem : stockItemList)
                <option value="${stockItem.id().toString()}" selected="${stockItem.id().equals(selectedStockItemId)}">${stockItem.name()}</option>
                @endfor
            </select>
        </div>

        <button type="submit" class="btn btn-sm btn-primary">조회</button>
        <button type="button" class="btn btn-sm text-xs"
                hx-get="/stock/htmx/trade/list"
                hx-target="#tradeListFragment"
                hx-select="#tradeListFragment"
                hx-swap="outerHTML">초기화</button>
    </form>
    </details>

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

        <!-- Left: 비중 분석 도넛 -->
        <div class="card bg-base-100 shadow-sm border border-base-200 flex-[2] min-w-[200px]">
            <div class="card-body p-4">
                <div class="flex items-center justify-between mb-2">
                    <div class="text-sm font-bold" id="tradeDonutTitle">매수 집중도</div>
                    <div class="tabs tabs-boxed tabs-xs">
                        <a class="tab" id="tradeDonutMetricProfit" onclick="switchTradeDonut('profit', null)">실현손익</a>
                        <a class="tab tab-active" id="tradeDonutMetricBuy" onclick="switchTradeDonut('buy', null)">매수금액</a>
                    </div>
                </div>
                <div class="flex gap-3">
                    <div class="flex-shrink-0 w-[200px] h-[200px]">
                        <canvas id="tradeDonutChart" class="amount-chart"></canvas>
                    </div>
                    <div class="flex flex-col w-0 flex-1 h-[200px] overflow-hidden">
                        <div class="tabs tabs-boxed tabs-xs mb-1 w-fit flex-shrink-0">
                            <a class="tab tab-active" id="tradeDonutTabStock" onclick="switchTradeDonut(null, 'stock')">종목별</a>
                            <a class="tab" id="tradeDonutTabAccount" onclick="switchTradeDonut(null, 'account')">계좌별</a>
                        </div>
                        <div id="tradeDonutLegend" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Right: 월별 매수/매도 바 차트 -->
        <div class="card bg-base-100 shadow-sm border border-base-200 flex-[3] min-w-[280px] overflow-visible">
            <div class="card-body p-4">
                <h3 class="font-bold text-sm mb-3">월별 매매 금액</h3>
                <div class="relative h-[220px]">
                    <canvas id="tradeMonthlyChart" class="amount-chart"></canvas>
                </div>
            </div>
        </div>

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

        const TRADE_COLORS = [
            'rgba(99,102,241,0.8)','rgba(34,197,94,0.8)','rgba(251,191,36,0.8)',
            'rgba(239,68,68,0.8)','rgba(59,130,246,0.8)','rgba(236,72,153,0.8)',
            'rgba(14,165,233,0.8)','rgba(249,115,22,0.8)','rgba(168,85,247,0.8)',
            'rgba(20,184,166,0.8)','rgba(245,158,11,0.8)','rgba(16,185,129,0.8)'
        ];

        let tradeMonthlyChartInst = null;
        let tradeDonutChartInst = null;
        let currentMetric = 'buy';
        let currentGroupBy = 'stock';

        function buildMonthlyData() {
            const buyMap = {}, sellMap = {};
            tradeData.forEach(function(d) {
                if (!d.tradeDate) return;
                const mon = d.tradeDate.slice(0, 7);
                const amt = Number(d.amount) || 0;
                if (d.type === 'BUY') buyMap[mon] = (buyMap[mon] || 0) + amt;
                else if (d.type === 'SELL') sellMap[mon] = (sellMap[mon] || 0) + amt;
            });
            const allMonths = Object.keys(buyMap).concat(Object.keys(sellMap));
            const months = allMonths.filter(function(v, i, a) { return a.indexOf(v) === i; }).sort();
            return {
                labels: months,
                buyData: months.map(function(m) { return buyMap[m] || 0; }),
                sellData: months.map(function(m) { return sellMap[m] || 0; })
            };
        }

        const PROFIT_COLORS_POS = [
            'rgba(239,68,68,0.85)','rgba(220,38,38,0.8)','rgba(249,115,22,0.8)',
            'rgba(253,164,175,0.85)','rgba(236,72,153,0.8)','rgba(245,158,11,0.8)'
        ];
        const PROFIT_COLORS_NEG = [
            'rgba(59,130,246,0.85)','rgba(14,165,233,0.8)','rgba(99,102,241,0.8)'
        ];

        function buildDonutData(metric, groupBy) {
            const map = {};
            tradeData.forEach(function(d) {
                const key = groupBy === 'stock' ? d.stockItem : d.account;
                if (!key) return;
                if (metric === 'profit') {
                    if (d.type !== 'SELL') return;
                    map[key] = (map[key] || 0) + (Number(d.profit) || 0);
                } else {
                    if (d.type !== 'BUY') return;
                    map[key] = (map[key] || 0) + (Number(d.amount) || 0);
                }
            });
            if (metric === 'profit') {
                const entries = Object.entries(map);
                const pos = entries.filter(function(e) { return e[1] >= 0; }).sort(function(a, b) { return b[1] - a[1]; });
                const neg = entries.filter(function(e) { return e[1] < 0; }).sort(function(a, b) { return a[1] - b[1]; });
                const sorted = pos.concat(neg);
                const posLen = pos.length;
                return {
                    labels: sorted.map(function(e) { return e[0]; }),
                    data: sorted.map(function(e) { return Math.abs(e[1]); }),
                    rawData: sorted.map(function(e) { return e[1]; }),
                    colors: sorted.map(function(e, i) {
                        return e[1] >= 0
                            ? PROFIT_COLORS_POS[i % PROFIT_COLORS_POS.length]
                            : PROFIT_COLORS_NEG[(i - posLen) % PROFIT_COLORS_NEG.length];
                    })
                };
            } else {
                const sorted = Object.entries(map).sort(function(a, b) { return b[1] - a[1]; });
                return {
                    labels: sorted.map(function(e) { return e[0]; }),
                    data: sorted.map(function(e) { return e[1]; }),
                    rawData: sorted.map(function(e) { return e[1]; }),
                    colors: TRADE_COLORS.slice(0, sorted.length)
                };
            }
        }

        function fmtAmt(v) {
            return Number(v).toLocaleString('ko-KR');
        }

        function initTradeMonthlyChart() {
            const m = buildMonthlyData();
            const ctx = document.getElementById('tradeMonthlyChart');
            if (!ctx) return;
            if (tradeMonthlyChartInst) tradeMonthlyChartInst.destroy();
            const gridColor = 'rgba(128,128,128,0.1)';
            tradeMonthlyChartInst = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: m.labels,
                    datasets: [
                        {
                            label: '매수',
                            data: m.buyData,
                            backgroundColor: 'rgba(239,68,68,0.7)',
                            borderRadius: 3
                        },
                        {
                            label: '매도',
                            data: m.sellData,
                            backgroundColor: 'rgba(59,130,246,0.7)',
                            borderRadius: 3
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'top', labels: { font: { size: 11 }, boxWidth: 12 } },
                        tooltip: {
                            callbacks: {
                                label: function(ctx) { return ctx.dataset.label + ': \u20a9' + fmtAmt(ctx.parsed.y); }
                            }
                        }
                    },
                    scales: {
                        x: { grid: { color: gridColor }, ticks: { font: { size: 10 } } },
                        y: {
                            grid: { color: gridColor },
                            ticks: {
                                font: { size: 10 },
                                callback: function(v) {
                                    if (v >= 100000000) return (v/100000000).toFixed(0) + '억';
                                    if (v >= 10000) return (v/10000).toFixed(0) + '만';
                                    return v.toLocaleString('ko-KR');
                                }
                            }
                        }
                    }
                }
            });
        }

        function makeDonutTooltipHandler(rawData, isProfitMode) {
            return function(context) {
                var tooltip = context.tooltip;
                var tooltipEl = document.getElementById('tradeDonutTooltipEl');
                if (!tooltipEl) {
                    tooltipEl = document.createElement('div');
                    tooltipEl.id = 'tradeDonutTooltipEl';
                    tooltipEl.style.cssText = 'position:fixed;z-index:9999;background:rgba(30,30,30,0.92);color:#fff;padding:6px 10px;border-radius:6px;font-size:12px;pointer-events:none;white-space:nowrap;transition:opacity .1s;';
                    document.body.appendChild(tooltipEl);
                }
                if (tooltip.opacity === 0) {
                    tooltipEl.style.opacity = '0';
                    return;
                }
                var idx = tooltip.dataPoints && tooltip.dataPoints[0] ? tooltip.dataPoints[0].dataIndex : 0;
                var label = tooltip.title && tooltip.title[0] ? tooltip.title[0] : '';
                var raw = rawData[idx] !== undefined ? rawData[idx] : 0;
                var sign = isProfitMode ? (raw >= 0 ? '\u25b2 ' : '\u25bc ') : '';
                var valText = sign + '\u20a9' + fmtAmt(Math.abs(raw));
                var color = context.chart.data.datasets[0].backgroundColor[idx] || '#999';
                tooltipEl.innerHTML = '<div style="font-weight:bold;margin-bottom:3px">' + label + '</div>'
                    + '<div style="display:flex;align-items:center;gap:5px">'
                    + '<span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:' + color + '"></span>'
                    + '<span>' + label + ': ' + valText + '</span>'
                    + '</div>';
                var rect = context.chart.canvas.getBoundingClientRect();
                var x = rect.left + tooltip.caretX;
                var y = rect.top + tooltip.caretY;
                tooltipEl.style.opacity = '1';
                // reposition to avoid going off screen
                tooltipEl.style.left = '0px';
                tooltipEl.style.top = '0px';
                var tw = tooltipEl.offsetWidth;
                var th = tooltipEl.offsetHeight;
                var finalX = x + 12;
                var finalY = y - th - 8;
                if (finalX + tw > window.innerWidth - 8) finalX = x - tw - 12;
                if (finalY < 8) finalY = y + 16;
                tooltipEl.style.left = finalX + 'px';
                tooltipEl.style.top = finalY + 'px';
            };
        }

        function initTradeDonutChart(metric, groupBy) {
            const d = buildDonutData(metric || 'profit', groupBy || 'stock');
            const ctx = document.getElementById('tradeDonutChart');
            if (!ctx) return;
            if (tradeDonutChartInst) tradeDonutChartInst.destroy();
            var oldTip = document.getElementById('tradeDonutTooltipEl');
            if (oldTip) oldTip.style.opacity = '0';
            const isProfitMode = (metric || 'profit') === 'profit';
            const legend = document.getElementById('tradeDonutLegend');
            // 데이터 없음 처리
            if (d.labels.length === 0) {
                ctx.style.display = 'none';
                if (legend) legend.innerHTML = '<div class="text-xs opacity-40 pt-4 text-center">해당 기간 ' + (isProfitMode ? '매도 내역' : '매수 내역') + '이 없습니다.</div>';
                return;
            }
            ctx.style.display = '';
            tradeDonutChartInst = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: d.labels,
                    datasets: [{
                        data: d.data,
                        backgroundColor: d.colors,
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '65%',
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            enabled: false,
                            external: makeDonutTooltipHandler(d.rawData, isProfitMode)
                        }
                    }
                }
            });
            const titleEl = document.getElementById('tradeDonutTitle');
            if (titleEl) titleEl.textContent = isProfitMode ? '종목별 손익 기여' : '매수 집중도';
            if (legend) {
                const total = d.data.reduce(function(a, b) { return a + b; }, 0);
                legend.innerHTML = d.labels.map(function(l, i) {
                    const pct = total > 0 ? ((d.data[i] / total) * 100).toFixed(1) : '0.0';
                    const raw = d.rawData[i];
                    const sign = isProfitMode ? (raw >= 0 ? '\u25b2 ' : '\u25bc ') : '';
                    const valClass = isProfitMode ? (raw >= 0 ? 'color:rgba(239,68,68,0.9)' : 'color:rgba(59,130,246,0.9)') : '';
                    return '<div class="flex items-center gap-1 mb-0.5">'
                        + '<span style="flex-shrink:0;display:inline-block;width:8px;height:8px;border-radius:50%;background:' + d.colors[i] + '"></span>'
                        + '<span class="flex-1" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' + l + '">' + l + '</span>'
                        + '<span style="flex-shrink:0;opacity:0.75;' + valClass + '">' + sign + pct + '%</span>'
                        + '</div>';
                }).join('');
            }
        }

        window.switchTradeDonut = function(metric, groupBy) {
            if (metric) currentMetric = metric;
            if (groupBy) currentGroupBy = groupBy;
            document.getElementById('tradeDonutMetricProfit').classList.toggle('tab-active', currentMetric === 'profit');
            document.getElementById('tradeDonutMetricBuy').classList.toggle('tab-active', currentMetric === 'buy');
            document.getElementById('tradeDonutTabStock').classList.toggle('tab-active', currentGroupBy === 'stock');
            document.getElementById('tradeDonutTabAccount').classList.toggle('tab-active', currentGroupBy === 'account');
            initTradeDonutChart(currentMetric, currentGroupBy);
        };

        function fmtDate(d) {
            return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
        }

        function tradeSubmit() {
            document.getElementById('tradeSearchForm').requestSubmit();
        }

        window.tradePicker = window.DateRangePicker
            ? window.DateRangePicker.create({
                formId:      'tradeSearchForm',
                startId:     'tradeStartDateInput',
                endId:       'tradeEndDateInput',
                rangeModeId: 'tradeRangeModeInput',
                btnClass:    'date-range-btn',
                minDate:     '${dataFirstDate != null ? dataFirstDate : ""}'
            })
            : { set: function(){}, shift: function(){}, jumpToEdge: function(){} };

        function initAll() {
            initTradeMonthlyChart();
            initTradeDonutChart(currentMetric, currentGroupBy);
        }

        if (window.Chart) {
            setTimeout(initAll, 0);
            setTimeout(function() {
                if (tradeMonthlyChartInst) tradeMonthlyChartInst.resize();
                if (tradeDonutChartInst) tradeDonutChartInst.resize();
            }, 200);
        } else {
            var s = document.createElement('script');
            s.src = 'https://cdn.jsdelivr.net/npm/chart.js';
            s.onload = function() {
                setTimeout(initAll, 0);
                setTimeout(function() {
                    if (tradeMonthlyChartInst) tradeMonthlyChartInst.resize();
                    if (tradeDonutChartInst) tradeDonutChartInst.resize();
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
