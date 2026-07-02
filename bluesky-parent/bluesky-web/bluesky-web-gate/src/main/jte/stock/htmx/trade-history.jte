@import java.util.List
@import java.math.BigDecimal
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.constant.TradeType
@import net.luversof.web.common.menu.domain.Pagination
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
@param Pagination pagination = null

!{
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
DecimalFormat df = new DecimalFormat("#,##0");
String safeFrom = from != null ? from : "";
String safeTo = to != null ? to : "";
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

@template._components.ui.card(cssClass = "mt-4", id = "trade-history-panel", body = @`
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
@template._components.ui.card(body = @`
<div class="text-xs text-base-content/50 mb-1">${periodStatusLabel}</div>
<div class="flex items-baseline gap-1 mb-2">
<span class="text-xl font-bold text-error amount-value">&#x20A9;${df.format(safeBuy)}</span>
<span class="text-xs text-base-content/40">${buyLabel}</span>
</div>
<div class="flex gap-4 text-xs text-base-content/50">
<span>${sellLabel} <span class="font-medium text-info amount-value">&#x20A9;${df.format(safeSell)}</span></span>
<span class="ml-auto">${countMessage.apply(totalItems)}</span>
</div>
`)
@template._components.ui.card(body = @`
<div class="text-xs text-base-content/50 mb-1">${realizedProfitLabel}</div>
<div class="text-xl font-bold ${rpPositive ? "text-profit" : "text-loss"} amount-value">
${rpPositive ? "+" : ""}&#x20A9;${df.format(safeRp)}
</div>
<div class="text-xs text-base-content/40 mt-1">${sellCountPageLabel}</div>
`)
@template._components.ui.card(body = @`
<div class="text-xs text-base-content/50 mb-1">${tradingCostLabel}</div>
<div class="text-xl font-bold amount-value">&#x20A9;${df.format(safeFee.add(safeTax))}</div>
<div class="flex gap-4 text-xs text-base-content/50 mt-1">
<span>${feeLabel} <span class="font-medium text-base-content/70 amount-value">&#x20A9;${df.format(safeFee)}</span></span>
<span>${tradingTaxLabel} <span class="font-medium text-base-content/70 amount-value">&#x20A9;${df.format(safeTax)}</span></span>
</div>
`)
</div>

<div>
<table class="table table-hover w-full table-sm table-pin-rows pin-top-nav">
<thead>
<tr class="bg-base-200">
<th scope="col">${dateLabel}</th>
<th scope="col">${typeLabel}</th>
<th scope="col">${stockLabel}</th>
<th scope="col" class="text-right">${unitPriceLabel}</th>
<th scope="col" class="text-right">${quantityLabel}</th>
<th scope="col" class="text-right">${amountLabel}</th>
<th scope="col" class="text-right hidden lg:table-cell">${feeLabel}</th>
<th scope="col" class="text-right hidden lg:table-cell">${tradingTaxLabel}</th>
<th scope="col" class="text-right">${realizedProfitLabel}</th>
</tr>
</thead>
<tbody>
@for(TradeResponse trade : trades)
!{
boolean isBuy = trade.type() == TradeType.BUY;
boolean hasProfit = trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) != 0;
String profitClass = trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";
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
<td colspan="9">@template._components.ui.emptyState(message = noTradeDataLabel)</td>
</tr>
@endif
</tbody>
</table>
</div>

@if(pagination != null && totalPages > 1)
@template._components.htmxPagination(
	pagination = pagination,
	url = "/stock/htmx/trade-history?from=" + safeFrom + "&to=" + safeTo + "&size=" + pageSize,
	includeSelector = "this",
	targetSelector = "#trade-history-panel")
@endif
@endif
`)