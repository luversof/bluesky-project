@import java.util.List
@import java.math.BigDecimal
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.constant.TradeType

@param List<TradeResponse> trades
@param int totalItems
@param int currentPage
@param int totalPages
@param int pageSize
@param String from = ""
@param String to = ""
@param BigDecimal totalBuy
@param BigDecimal totalSell
@param BigDecimal totalRealizedProfit
@param BigDecimal totalFee = BigDecimal.ZERO
@param BigDecimal totalTax = BigDecimal.ZERO
@param String tradePeriod

!{
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
DecimalFormat df = new DecimalFormat("#,##0");
String safeFrom = from != null ? from : "";
String safeTo = to != null ? to : "";
int windowSize = 10;
int startPage = Math.max(1, currentPage - windowSize / 2);
int endPage = Math.min(totalPages, startPage + windowSize - 1);
if (endPage - startPage < windowSize - 1) startPage = Math.max(1, endPage - windowSize + 1);
BigDecimal safeBuy = totalBuy != null ? totalBuy : BigDecimal.ZERO;
BigDecimal safeSell = totalSell != null ? totalSell : BigDecimal.ZERO;
BigDecimal safeRp = totalRealizedProfit != null ? totalRealizedProfit : BigDecimal.ZERO;
BigDecimal safeFee = totalFee != null ? totalFee : BigDecimal.ZERO;
BigDecimal safeTax = totalTax != null ? totalTax : BigDecimal.ZERO;
boolean rpPositive = safeRp.compareTo(BigDecimal.ZERO) >= 0;
}

<div class="card bg-base-100 shadow-xl mt-4" id="trade-history-panel">
<div class="card-body p-4">
<div class="flex items-center justify-between mb-3">
<h3 class="card-title text-base">
매매 내역
<span class="text-sm font-normal text-base-content/60 ml-2">${tradePeriod}</span>
</h3>
@if(totalItems > 0)
<span class="badge badge-ghost badge-sm">${totalItems}건</span>
@endif
</div>

@if(totalItems == 0)
<div class="text-center py-6 text-base-content/50">
<p>해당 기간의 매매 내역이 없습니다.</p>
</div>
@else
<div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
<div class="card bg-base-100 shadow-sm border border-base-200">
<div class="card-body p-4">
<div class="text-xs text-base-content/50 mb-1">기간 매매 현황</div>
<div class="flex items-baseline gap-1 mb-2">
<span class="text-xl font-bold text-error amount-value">&#x20A9;${df.format(safeBuy)}</span>
<span class="text-xs text-base-content/40">매수</span>
</div>
<div class="flex gap-4 text-xs text-base-content/50">
<span>매도 <span class="font-medium text-info amount-value">&#x20A9;${df.format(safeSell)}</span></span>
<span class="ml-auto">${totalItems}건</span>
</div>
</div>
</div>
<div class="card bg-base-100 shadow-sm border border-base-200">
<div class="card-body p-4">
<div class="text-xs text-base-content/50 mb-1">실현손익</div>
<div class="text-xl font-bold ${rpPositive ? "text-success" : "text-error"} amount-value">
${rpPositive ? "+" : ""}&#x20A9;${df.format(safeRp)}
</div>
!{
long sellCount = trades != null ? trades.stream()
.filter(t -> t.type() == TradeType.SELL).count() : 0L;
}
<div class="text-xs text-base-content/40 mt-1">매도 ${sellCount}건(페이지 기준)</div>
</div>
</div>
<div class="card bg-base-100 shadow-sm border border-base-200">
<div class="card-body p-4">
<div class="text-xs text-base-content/50 mb-1">매매 비용</div>
<div class="text-xl font-bold amount-value">&#x20A9;${df.format(safeFee.add(safeTax))}</div>
<div class="flex gap-4 text-xs text-base-content/50 mt-1">
<span>수수료 <span class="font-medium text-base-content/70 amount-value">&#x20A9;${df.format(safeFee)}</span></span>
<span>거래세 <span class="font-medium text-base-content/70 amount-value">&#x20A9;${df.format(safeTax)}</span></span>
</div>
</div>
</div>
</div>

<div class="overflow-x-auto">
<table class="table table-hover table-striped w-full table-sm table-pin-rows">
<thead>
<tr class="bg-base-200">
<th>날짜</th>
<th>종목</th>
<th>구분</th>
<th class="text-right">단가</th>
<th class="text-right">수량</th>
<th class="text-right">금액</th>
<th class="text-right hidden lg:table-cell">수수료</th>
<th class="text-right hidden lg:table-cell">거래세</th>
<th class="text-right">실현손익</th>
</tr>
</thead>
<tbody>
@for(TradeResponse trade : trades)
!{
boolean isBuy = trade.type() == TradeType.BUY;
boolean hasProfit = trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) != 0;
String profitClass = trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-success" : "text-error";
String tradeDateStr = trade.tradeDate() != null ? fmt.format(trade.tradeDate()) : "-";
}
<tr>
<td class="whitespace-nowrap">${tradeDateStr}</td>
<td class="font-bold whitespace-nowrap">${trade.stockItemName() != null ? trade.stockItemName() : "-"}</td>
<td>
<span class="${isBuy ? "badge badge-error badge-outline badge-sm" : "badge badge-info badge-outline badge-sm"}">
${isBuy ? "매수" : "매도"}
</span>
</td>
<td class="text-right font-mono amount-value">${trade.price() != null ? df.format(trade.price()) : "0"}</td>
<td class="text-right font-mono amount-value">${df.format(trade.quantity())}</td>
<td class="text-right font-mono amount-value">${trade.amount() != null ? df.format(trade.amount()) : "0"}</td>
<td class="text-right text-xs text-base-content/60 amount-value hidden lg:table-cell">${trade.fee() != null ? df.format(trade.fee()) : "0"}</td>
<td class="text-right text-xs text-base-content/60 amount-value hidden lg:table-cell">${trade.tax() != null ? df.format(trade.tax()) : "0"}</td>
<td class="text-right font-bold font-mono amount-value ${hasProfit ? profitClass : ""}">
${hasProfit && !isBuy ? df.format(trade.realizedProfit()) : ""}
</td>
</tr>
@endfor
@if(trades == null || trades.isEmpty())
<tr>
<td colspan="9" class="text-center py-10 opacity-50">매매 내역이 없습니다.</td>
</tr>
@endif
</tbody>
</table>
</div>

@if(totalPages > 1)
<div class="flex justify-center p-2 border-t border-base-200 bg-base-100 mt-2">
<div class="join">
<button class="join-item btn btn-sm ${currentPage <= 1 ? "btn-disabled" : ""}"
hx-get="/stock/htmx/trade-history?from=${safeFrom}&to=${safeTo}&page=1&size=${pageSize}"
hx-target="#trade-history-panel" hx-swap="outerHTML">&laquo;</button>
<button class="join-item btn btn-sm ${currentPage <= 1 ? "btn-disabled" : ""}"
hx-get="/stock/htmx/trade-history?from=${safeFrom}&to=${safeTo}&page=${currentPage - 1}&size=${pageSize}"
hx-target="#trade-history-panel" hx-swap="outerHTML">&lsaquo;</button>
@for(int p = startPage; p <= endPage; p++)
<button class="join-item btn btn-sm ${p == currentPage ? "btn-active" : ""}"
hx-get="/stock/htmx/trade-history?from=${safeFrom}&to=${safeTo}&page=${p}&size=${pageSize}"
hx-target="#trade-history-panel" hx-swap="outerHTML">${p}</button>
@endfor
<button class="join-item btn btn-sm ${currentPage >= totalPages ? "btn-disabled" : ""}"
hx-get="/stock/htmx/trade-history?from=${safeFrom}&to=${safeTo}&page=${currentPage + 1}&size=${pageSize}"
hx-target="#trade-history-panel" hx-swap="outerHTML">&rsaquo;</button>
<button class="join-item btn btn-sm ${currentPage >= totalPages ? "btn-disabled" : ""}"
hx-get="/stock/htmx/trade-history?from=${safeFrom}&to=${safeTo}&page=${totalPages}&size=${pageSize}"
hx-target="#trade-history-panel" hx-swap="outerHTML">&raquo;</button>
</div>
</div>
@endif
@endif
</div>
</div>