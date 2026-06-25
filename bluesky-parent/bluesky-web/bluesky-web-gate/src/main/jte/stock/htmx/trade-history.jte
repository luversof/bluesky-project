@import java.util.List
@import java.math.BigDecimal
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.constant.TradeType
@import io.github.luversof.boot.context.support.MessageUtil

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
long sellCount = trades != null ? trades.stream().filter(t -> t.type() == TradeType.SELL).count() : 0L;

java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);
String historyTitle = MessageUtil.getMessage("stock.page.trade.title");
String noTradeInPeriodLabel = MessageUtil.getMessage("stock.trade.message.no.period.data");
String periodStatusLabel = MessageUtil.getMessage("stock.trade.label.period.status");
String buyLabel = MessageUtil.getMessage("stock.trade.label.buy");
String sellLabel = MessageUtil.getMessage("stock.trade.label.sell");
String realizedProfitLabel = MessageUtil.getMessage("stock.trade.label.realized.profit");
String tradingCostLabel = MessageUtil.getMessage("stock.trade.label.trading.cost");
String feeLabel = MessageUtil.getMessage("stock.trade.label.fee");
String tradingTaxLabel = MessageUtil.getMessage("stock.trade.label.trading.tax");
String dateLabel = MessageUtil.getMessage("common.label.date");
String stockLabel = MessageUtil.getMessage("common.label.stock");
String typeLabel = MessageUtil.getMessage("common.label.type");
String unitPriceLabel = MessageUtil.getMessage("stock.trade.column.unit.price");
String quantityLabel = MessageUtil.getMessage("common.label.quantity");
String amountLabel = MessageUtil.getMessage("common.label.amount");
String noTradeDataLabel = MessageUtil.getMessage("stock.trade.message.no.data");
String sellCountPageLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.trade.history.sell.count.page"), sellLabel, countMessage.apply(sellCount));
}

<div class="card bg-base-100 border border-base-200 rounded-box shadow-sm mt-4" id="trade-history-panel">
<div class="card-body p-4">
<div class="flex items-center justify-between mb-3">
<h3 class="card-title text-base">
${historyTitle}
<span class="text-sm font-normal text-base-content/60 ml-2">${tradePeriod}</span>
</h3>
@if(totalItems > 0)
<span class="badge badge-ghost badge-sm">${countMessage.apply(totalItems)}</span>
@endif
</div>

@if(totalItems == 0)
<div class="text-center py-6 text-base-content/50">
<p>${noTradeInPeriodLabel}</p>
</div>
@else
<div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
<div class="card bg-base-100 shadow-sm border border-base-200">
<div class="card-body p-4">
<div class="text-xs text-base-content/50 mb-1">${periodStatusLabel}</div>
<div class="flex items-baseline gap-1 mb-2">
<span class="text-xl font-bold text-error amount-value">&#x20A9;${df.format(safeBuy)}</span>
<span class="text-xs text-base-content/40">${buyLabel}</span>
</div>
<div class="flex gap-4 text-xs text-base-content/50">
<span>${sellLabel} <span class="font-medium text-info amount-value">&#x20A9;${df.format(safeSell)}</span></span>
<span class="ml-auto">${countMessage.apply(totalItems)}</span>
</div>
</div>
</div>
<div class="card bg-base-100 shadow-sm border border-base-200">
<div class="card-body p-4">
<div class="text-xs text-base-content/50 mb-1">${realizedProfitLabel}</div>
<div class="text-xl font-bold ${rpPositive ? "text-success" : "text-error"} amount-value">
${rpPositive ? "+" : ""}&#x20A9;${df.format(safeRp)}
</div>
<div class="text-xs text-base-content/40 mt-1">${sellCountPageLabel}</div>
</div>
</div>
<div class="card bg-base-100 shadow-sm border border-base-200">
<div class="card-body p-4">
<div class="text-xs text-base-content/50 mb-1">${tradingCostLabel}</div>
<div class="text-xl font-bold amount-value">&#x20A9;${df.format(safeFee.add(safeTax))}</div>
<div class="flex gap-4 text-xs text-base-content/50 mt-1">
<span>${feeLabel} <span class="font-medium text-base-content/70 amount-value">&#x20A9;${df.format(safeFee)}</span></span>
<span>${tradingTaxLabel} <span class="font-medium text-base-content/70 amount-value">&#x20A9;${df.format(safeTax)}</span></span>
</div>
</div>
</div>
</div>

<div class="overflow-x-auto">
<table class="table table-hover table-striped w-full table-sm table-pin-rows">
<thead>
<tr class="bg-base-200">
<th>${dateLabel}</th>
<th>${typeLabel}</th>
<th>${stockLabel}</th>
<th class="text-right">${unitPriceLabel}</th>
<th class="text-right">${quantityLabel}</th>
<th class="text-right">${amountLabel}</th>
<th class="text-right hidden lg:table-cell">${feeLabel}</th>
<th class="text-right hidden lg:table-cell">${tradingTaxLabel}</th>
<th class="text-right">${realizedProfitLabel}</th>
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
<td>
<span class="${isBuy ? "badge badge-error badge-outline badge-sm" : "badge badge-info badge-outline badge-sm"}">
${isBuy ? buyLabel : sellLabel}
</span>
</td>
<td class="font-bold whitespace-nowrap"><a href="/stock/item?stockItemId=${trade.stockItemId() != null ? trade.stockItemId().toString() : ""}" class="link link-hover">${trade.stockItemName() != null ? trade.stockItemName() : "-"}</a></td>
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
<td colspan="9" class="text-center py-10 opacity-50">${noTradeDataLabel}</td>
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