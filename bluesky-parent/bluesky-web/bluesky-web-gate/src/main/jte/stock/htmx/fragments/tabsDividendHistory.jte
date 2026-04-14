@import java.util.List
@import java.util.UUID
@import java.time.LocalDate
@import java.math.BigDecimal
@import java.math.RoundingMode
@import net.luversof.web.gate.stock.dto.response.DividendView
@import net.luversof.web.common.menu.domain.Pagination
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat

@param List<DividendView> dividendList
@param List<DividendView> allDividendList = java.util.List.of()
@param Pagination pagination
@param int totalItems
@param int totalPages
@param int currentPage
@param int size
@param List<Account> accountList
@param List<StockItem> stockItemList
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param LocalDate startDate
@param LocalDate endDate
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
        jsSb.append("{payDate:\"").append(d.payDate() != null ? ymd.format(d.payDate().atZone(java.time.ZoneId.systemDefault())) : "").append("\"");
        jsSb.append(",stockItem:\"").append(ej.apply(d.stockItemName())).append("\"");
        jsSb.append(",account:\"").append(ej.apply(d.accountName())).append("\"");
        jsSb.append(",net:").append(d.netAmount() != null ? d.netAmount().toPlainString() : "0");
        jsSb.append(",gross:").append(d.grossAmount() != null ? d.grossAmount().toPlainString() : "0");
        jsSb.append("}");
    }
    jsSb.append("];");
    String dividendDataJs = jsSb.toString();
}

<div id="dividendListFragment">

    <!-- Quick Date Selector -->
    <div class="flex flex-wrap gap-2 mb-3 items-center">
        <span class="text-xs opacity-50 mr-1">기간</span>
        <button type="button" class="btn btn-xs btn-ghost" onclick="dividendPicker.jumpToEdge('start')">«</button>
        <button type="button" class="btn btn-xs btn-ghost" onclick="dividendPicker.shift(-1)">‹ 이전</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("mtd") ? "btn-primary" : ""}" onclick="dividendPicker.set('mtd',this)">이번달</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("1") ? "btn-primary" : ""}" onclick="dividendPicker.set(1,this)">1개월</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("3") ? "btn-primary" : ""}" onclick="dividendPicker.set(3,this)">3개월</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("6") ? "btn-primary" : ""}" onclick="dividendPicker.set(6,this)">6개월</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("ytd") ? "btn-primary" : ""}" onclick="dividendPicker.set('ytd',this)">올해</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("12") ? "btn-primary" : ""}" onclick="dividendPicker.set(12,this)">1년</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("36") ? "btn-primary" : ""}" onclick="dividendPicker.set(36,this)">3년</button>
        <button type="button" class="btn btn-xs date-range-btn ${rangeMode != null && rangeMode.equals("all") ? "btn-primary" : ""}" onclick="dividendPicker.set(0,this)">전체</button>
        <button type="button" class="btn btn-xs btn-ghost" onclick="dividendPicker.shift(1)">다음 ›</button>
        <button type="button" class="btn btn-xs btn-ghost" onclick="dividendPicker.jumpToEdge('end')">»</button>
    </div>

    <!-- Filter & Search Form (collapsible) -->
    <details class="mb-4"$unsafe{(selectedAccountId != null || selectedStockItemId != null || startDate != null) ? " open" : ""}>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            상세 검색
            @if(selectedAccountId != null || selectedStockItemId != null || startDate != null)
                <span class="badge badge-xs badge-primary">필터 적용 중</span>
            @endif
        </summary>
    <form id="dividendSearchForm" class="flex flex-wrap gap-2 mt-2 mb-2 items-end bg-base-100 p-2 rounded-box shadow-sm"
          hx-get="/stock/htmx/dividend/list"
          hx-target="#dividendListFragment"
          hx-select="#dividendListFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">

        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">기간</span></label>
            <div class="join">
                <input type="date" name="startDate" id="startDateInput" class="input input-sm input-bordered join-item" value="${startDate != null ? startDate.toString() : ""}">
                <input type="date" name="endDate" id="endDateInput" class="input input-sm input-bordered join-item" value="${endDate != null ? endDate.toString() : ""}">
            </div>
        </div>
        <input type="hidden" name="rangeMode" id="dividendRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">

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
                hx-get="/stock/htmx/dividend/list"
                hx-target="#dividendListFragment"
                hx-select="#dividendListFragment"
                hx-swap="outerHTML">초기화</button>
    </form>
    </details>

    <!-- Dashboard -->

    <!-- Row 1: Summary Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">

        <!-- Card 1: 기간 합계 (세전/세후/과세) -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">기간 배당금</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-dividend amount-value">₩ ${decimalFormat.format(safeAllNet)}</span>
                    <span class="text-xs text-base-content/40">세후</span>
                </div>
                <div class="flex gap-4 text-xs text-base-content/50">
                    <span>세전 <span class="font-medium text-base-content/70 amount-value">₩ ${decimalFormat.format(safeAllGross)}</span></span>
                    <span>과세 <span class="font-medium text-base-content/70 amount-value">₩ ${decimalFormat.format(safeAllTaxable)}</span></span>
                    <span class="ml-auto">${totalItems}건</span>
                </div>
            </div>
        </div>

        <!-- Card 2: 전기 대비 -->
        @if(hasComparison)
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">전기 대비
                    @if(prevStartDate != null && prevEndDate != null)
                    <span class="opacity-40">(${prevStartDate.toString()} ~ ${prevEndDate.toString()})</span>
                    @endif
                </div>
                <div class="flex items-baseline gap-2 mb-1">
                    <span class="text-xl font-bold ${diffPositive ? "text-success" : "text-error"}">${diffPositive ? "▲" : "▼"} ${diffPct}%</span>
                    <span class="text-xs amount-value ${diffPositive ? "text-success" : "text-error"}">${diffPositive ? "+" : ""}${decimalFormat.format(diffAmount)}원</span>
                </div>
                <div class="text-xs text-base-content/40">전기 <span class="amount-value">₩ ${decimalFormat.format(safePrevNet)}</span></div>
            </div>
        </div>
        @else
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4 items-center justify-center text-center">
                <div class="text-xs text-base-content/30">기간을 선택하면<br>전기 대비를 표시합니다</div>
            </div>
        </div>
        @endif

        <!-- Card 3: 월평균 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">월평균 배당금</div>
                <div class="text-xl font-bold">₩ <span id="avgAmountDisplay" class="amount-value">-</span></div>
                <div class="text-xs text-base-content/40 mt-1" id="avgAmountDesc"></div>
            </div>
        </div>

    </div>

    <!-- Row 2: Charts -->
    <div class="flex flex-wrap gap-4 mb-4">

        <!-- Left: 비중 분석 -->
        <div class="card bg-base-100 shadow-sm border border-base-200 flex-[2] min-w-[200px]">
            <div class="card-body p-4">
                <div class="text-sm font-bold mb-3">비중 분석</div>
                <div class="flex gap-3">
                    <div class="flex-shrink-0 w-[200px] h-[200px]">
                        <canvas id="donutChart" class="amount-chart"></canvas>
                    </div>
                    <div class="flex flex-col w-0 flex-1 h-[200px] overflow-hidden">
                        <div class="tabs tabs-boxed tabs-xs mb-1 w-fit flex-shrink-0">
                            <a class="tab tab-active" id="donutTabStock" onclick="switchDonut('stock')">종목별</a>
                            <a class="tab" id="donutTabAccount" onclick="switchDonut('account')">계좌별</a>
                        </div>
                        <div id="donutLegend" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Right: Monthly Bar Chart -->
        <div class="card bg-base-100 shadow-sm border border-base-200 flex-[3] min-w-[280px] overflow-visible">
            <div class="card-body p-4">
                <h3 class="font-bold text-sm mb-3">월별 배당금</h3>
                <div class="relative h-[220px]">
                    <canvas id="monthlyBarChart" class="amount-chart"></canvas>
                </div>
            </div>
        </div>

    </div>

    <!-- Collapsible Detail List -->
    <details id="detailSection" class="mb-4">
        <summary class="cursor-pointer select-none flex items-center gap-2 font-semibold text-sm p-3 bg-base-200 rounded-box hover:bg-base-300 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 transition-transform detail-arrow" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            </svg>
            상세 목록 (${totalItems}건)
        </summary>

        <div id="dividendListContainer" class="flex flex-col relative mt-2">
            <div class="overflow-x-auto flex-1 min-h-0">
                <table class="table table-hover table-striped w-full table-sm table-pin-rows">
                    <thead>
                    <tr class="bg-base-200">
                        <th class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "payDate,${sortField.equals("payDate") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            지급일 @if(sortField.equals("payDate"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="cursor-pointer hover:bg-base-300 hidden sm:table-cell" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "accountName,${sortField.equals("accountName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            계좌 @if(sortField.equals("accountName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "stockItemName,${sortField.equals("stockItemName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            종목명 @if(sortField.equals("stockItemName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "grossAmount,${sortField.equals("grossAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            배당금(세전) @if(sortField.equals("grossAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "netAmount,${sortField.equals("netAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            배당금(세후) @if(sortField.equals("netAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "tax,${sortField.equals("tax") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            세금 @if(sortField.equals("tax"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "taxableAmount,${sortField.equals("taxableAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            과세금액 @if(sortField.equals("taxableAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
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
                        <td class="text-right hidden lg:table-cell amount-value">${item.tax() != null ? decimalFormat.format(item.tax()) : "0"}</td>
                        <td class="text-right amount-value">${item.taxableAmount() != null ? decimalFormat.format(item.taxableAmount()) : "0"}</td>
                    </tr>
                    @endfor
                    @if(dividendList == null || dividendList.isEmpty())
                    <tr>
                        <td colspan="7" class="text-center py-10 opacity-50">배당 내역이 없습니다.</td>
                    </tr>
                    @endif
                    </tbody>
                    <tfoot>
                        <tr class="font-bold bg-base-200">
                            <td colspan="3" class="text-center sm:text-right sm:pr-4">합계</td>
                            <td class="text-right font-mono amount-value">${totalGrossAmount != null ? decimalFormat.format(totalGrossAmount) : "0"}</td>
                            <td class="text-right font-mono text-dividend amount-value">${totalNetAmount != null ? decimalFormat.format(totalNetAmount) : "0"}</td>
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

        const CHART_COLORS = [
            'rgba(99,102,241,0.8)','rgba(34,197,94,0.8)','rgba(251,191,36,0.8)',
            'rgba(239,68,68,0.8)','rgba(59,130,246,0.8)','rgba(236,72,153,0.8)',
            'rgba(14,165,233,0.8)','rgba(249,115,22,0.8)','rgba(168,85,247,0.8)',
            'rgba(20,184,166,0.8)','rgba(245,158,11,0.8)','rgba(16,185,129,0.8)'
        ];

        let monthlyChart = null;
        let donutChart = null;

        function buildMonthlyData() {
            const map = {};
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
            const ctx = document.getElementById('monthlyBarChart');
            if (!ctx) return;
            if (monthlyChart) monthlyChart.destroy();
            monthlyChart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: m.labels,
                    datasets: [{
                        label: '배당금(세후)',
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
                        y: { beginAtZero: true, grid: { color: gridColor }, ticks: { callback: v => '₩' + Math.round(v).toLocaleString(), font: { size: 11 } } }
                    }
                }
            });
        }

        function initDonutChart(mode) {
            const d = buildDonutData(mode);
            const ctx = document.getElementById('donutChart');
            if (!ctx) return;
            if (donutChart) donutChart.destroy();
            const legend = document.getElementById('donutLegend');
            if (d.labels.length === 0) {
                ctx.style.display = 'none';
                if (legend) legend.innerHTML = '<div class="text-xs opacity-40 pt-4 text-center">해당 기간 배당 내역이 없습니다.</div>';
                return;
            }
            if (ctx.parentElement) ctx.parentElement.style.display = '';
            ctx.style.display = '';
            donutChart = new Chart(ctx, {
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
                        tooltip: { callbacks: { label: ctx => ' ₩' + Math.round(ctx.parsed).toLocaleString() } }
                    },
                    cutout: '62%'
                }
            });
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
                displayEl.textContent = '0';
                if (descEl) descEl.textContent = '데이터 없음';
                return;
            }
            const totalNet = dividendData.reduce((s, d) => s + Number(d.net), 0);
            const monthSet = new Set(dividendData.filter(d => d.payDate).map(d => d.payDate.slice(0, 7)));
            const monthCount = monthSet.size || 1;
            const avgPerMonth = totalNet / monthCount;
            displayEl.textContent = Math.round(avgPerMonth).toLocaleString();
            if (descEl) descEl.textContent = monthCount + '개월 평균 · 총 ' + Math.round(totalNet).toLocaleString() + '원 / ' + dividendData.length + '건';
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
                rangeModeId: 'dividendRangeModeInput',
                btnClass:    'date-range-btn',
                minDate:     '${dataFirstDate != null ? dataFirstDate : ""}'
            })
            : { set: function(){}, shift: function(){}, jumpToEdge: function(){} };

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