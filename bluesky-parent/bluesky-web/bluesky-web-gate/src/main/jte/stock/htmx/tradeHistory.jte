@import java.util.List
@import java.math.BigDecimal
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import java.util.Map
@import java.util.UUID
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.constant.TradeType
@import net.luversof.web.common.menu.domain.Pagination
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil

@param List<TradeResponse> trades
<%-- 날짜 칸에 쓸 존. 이 화면(자산성장)의 기간 표시와 같은 존이어야 한다. --%>
@param java.time.ZoneId zone
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
@param long totalSellCount = -1L
<%-- 계좌명이 없으면 같은 날 같은 값의 다른 계좌 거래가 화면에서 완전히 같은 줄이 된다.
     실측 2026-09-10: 258행 중 2쌍이 연금저축1/연금저축2 로 갈리는 별개 거래인데 구분할 길이 없었다.
     매매 화면의 같은 표(tradeDetailList)는 계좌 열을 갖고 있어 둘이 어긋나 있었다. --%>
@param Map<UUID, String> accountNames = java.util.Map.of()
@param String tradePeriod
@param Pagination pagination = null
@param String tradeHistoryFilterQuery = ""

!{
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone);
DecimalFormat df = net.luversof.web.gate.stock.util.StockFormatUtil.wonFormat();
// 이 표는 매매 화면의 tradeDetailList 와 같은 표인데 두 규칙이 이쪽만 빠져 있었다 -
// 실측 2026-09-13(258 행): 거래세 228 행 · 수수료 51 행이 "0", 매수 203 행의 실현 손익은 빈 칸,
// 그리고 실현 손익 55 개 중 부호가 붙은 것은 음수 6 개뿐이고 양수 49 개는 맨 숫자였다.
String zeroAmountTitle = MessageUtil.getMessage("common.amount.zero.title");
String buyNoRealizedTitle = MessageUtil.getMessage("stock.trade.realized.buy.none");
String noDataTitle = MessageUtil.getMessage("common.message.no.data");
String safeFrom = from != null ? from : "";
String safeTo = to != null ? to : "";
String safeFilterQuery = tradeHistoryFilterQuery != null ? tradeHistoryFilterQuery : "";
BigDecimal safeBuy = totalBuy != null ? totalBuy : BigDecimal.ZERO;
BigDecimal safeSell = totalSell != null ? totalSell : BigDecimal.ZERO;
BigDecimal safeRp = totalRealizedProfit != null ? totalRealizedProfit : BigDecimal.ZERO;
BigDecimal safeFee = totalFee != null ? totalFee : BigDecimal.ZERO;
BigDecimal safeTax = totalTax != null ? totalTax : BigDecimal.ZERO;
boolean rpPositive = safeRp.compareTo(BigDecimal.ZERO) >= 0;
// 이 카드의 금액(실현손익)은 기간 전체다. 건수도 같은 범위여야 한다 - 예전에는 현재 페이지의 행만 세어
// 자산성장 화면이 "실현손익 +225,630,135 · 매도 0건(페이지 기준)" 을 보여줬다(실측 2026-09-10).
// 1페이지가 최근 매수로만 차 있으면 0 이 나온다. 매매 화면은 같은 카드를 "매도 55건" 으로 보여준다.
// 호출부가 기간 건수를 주지 않으면(-1) 예전처럼 페이지 기준으로 떨어진다.
long pageSellCount = trades != null ? trades.stream().filter(t -> t.type() == TradeType.SELL).count() : 0L;
boolean sellCountIsPeriod = totalSellCount >= 0;
long sellCount = sellCountIsPeriod ? totalSellCount : pageSellCount;

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
String accountLabel = MessageUtil.getMessage("common.label.account");
String typeLabel = MessageUtil.getMessage("common.label.type");
String unitPriceLabel = MessageUtil.getMessage("stock.trade.column.unit.price");
String quantityLabel = MessageUtil.getMessage("common.label.quantity");
String amountLabel = MessageUtil.getMessage("common.label.amount");
String noTradeDataLabel = MessageUtil.getMessage("stock.trade.message.no.data");
String sellCountPageLabel = sellCountIsPeriod
	? sellLabel + " " + countMessage.apply(sellCount)
	: java.text.MessageFormat.format(MessageUtil.getMessage("stock.trade.history.sell.count.page"), sellLabel, countMessage.apply(sellCount));
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
<%-- 기간에 거래가 하나도 없으면 여기서 끝난다 - 아래 표의 빈 상태에는 닿지 않는다.
     그래서 다음 행동(기간 넓히기)도 이 자리에 둔다. 공용 컴포넌트를 쓰면 아이콘 · 여백도 형제 화면과 같아진다. --%>
@template._components.ui.emptyState(message = noTradeInPeriodLabel, padding = "py-6", widenRangeAction = !safeFrom.isEmpty() || !safeTo.isEmpty())
@else
<div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
@template._components.ui.card(body = @`
<div class="text-xs text-base-content/60 mb-1">${periodStatusLabel}</div>
<div class="flex items-baseline gap-1 mb-2">
<span class="text-xl font-bold amount-value">&#x20A9;${df.format(safeBuy)}</span>
<span class="text-xs text-base-content/60">${buyLabel}</span>
</div>
<div class="flex gap-4 text-xs text-base-content/60">
<span>${sellLabel} <span class="font-medium amount-value">&#x20A9;${df.format(safeSell)}</span></span>
<%-- 매수+매도 전체 건수다. 라벨이 없으면 왼쪽의 "매도" 건수로 읽힌다(실측 2026-09-12: 98건 = 매수 91 + 매도 7). --%>
<span class="ml-auto">${MessageUtil.getMessage("stock.activity.label.trade")} ${countMessage.apply(totalItems)}</span>
</div>
`)
@template._components.ui.card(body = @`
<div class="text-xs text-base-content/60 mb-1">${realizedProfitLabel}</div>
<div class="text-xl font-bold ${rpPositive ? "text-profit" : "text-loss"} amount-value">
${StockFormatUtil.displayWon(safeRp) > 0 ? "+" : ""}&#x20A9;${df.format(safeRp)}
</div>
<div class="text-xs text-base-content/60 mt-1">${sellCountPageLabel}</div>
`)
@template._components.ui.card(body = @`
<div class="text-xs text-base-content/60 mb-1">${tradingCostLabel}</div>
<div class="text-xl font-bold amount-value">&#x20A9;${df.format(safeFee.add(safeTax))}</div>
<div class="flex gap-4 text-xs text-base-content/60 mt-1">
<span>${feeLabel} <span class="font-medium text-base-content/70 amount-value">&#x20A9;${df.format(safeFee)}</span></span>
<span>${tradingTaxLabel} <span class="font-medium text-base-content/70 amount-value">&#x20A9;${df.format(safeTax)}</span></span>
</div>
`)
</div>

<div class="overflow-x-auto">
<table class="table w-full table-sm table-pin-rows pin-top-nav" aria-label="${historyTitle}">
<thead>
<tr class="bg-base-200">
<th scope="col">${dateLabel}</th>
<th scope="col">${typeLabel}</th>
<th scope="col">${stockLabel}</th>
<th scope="col" class="hidden sm:table-cell">${accountLabel}</th>
<th scope="col" class="text-right">${unitPriceLabel}</th>
<th scope="col" class="text-right">${quantityLabel}</th>
<th scope="col" class="text-right">${amountLabel}</th>
<th scope="col" class="text-right hidden lg:table-cell print:table-cell">${feeLabel}</th>
<th scope="col" class="text-right hidden lg:table-cell print:table-cell">${tradingTaxLabel}</th>
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
<td class="font-bold whitespace-nowrap"><a href="${StockDetailLinkUtil.item(trade.stockItemId() != null ? trade.stockItemId().toString() : "")}" class="link link-hover">${trade.stockItemName() != null ? trade.stockItemName() : "-"}</a></td>
<td class="whitespace-nowrap hidden sm:table-cell"><a href="${StockDetailLinkUtil.account(trade.accountId() != null ? trade.accountId().toString() : "")}" class="link link-hover">${accountNames != null ? accountNames.getOrDefault(trade.accountId(), "") : ""}</a></td>
<td class="text-right font-mono amount-value">${trade.price() != null ? df.format(trade.price()) : "0"}</td>
<td class="text-right font-mono amount-value">${df.format(trade.quantity())}</td>
<td class="text-right font-mono amount-value">${trade.amount() != null ? df.format(trade.amount()) : "0"}</td>
<td class="text-right text-xs text-base-content/60 amount-value hidden lg:table-cell print:table-cell">@if(trade.fee() == null || trade.fee().signum() == 0)<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>@else${df.format(trade.fee())}@endif</td>
<td class="text-right text-xs text-base-content/60 amount-value hidden lg:table-cell print:table-cell">@if(trade.tax() == null || trade.tax().signum() == 0)<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>@else${df.format(trade.tax())}@endif</td>
<td class="text-right font-bold font-mono amount-value ${hasProfit ? profitClass : ""}">
@if(hasProfit && !isBuy)${StockFormatUtil.signedWon(StockFormatUtil.displayWon(trade.realizedProfit()))}@else<span class="text-base-content/60" aria-hidden="true" title="${isBuy ? buyNoRealizedTitle : noDataTitle}">-</span><span class="sr-only">${isBuy ? buyNoRealizedTitle : noDataTitle}</span>@endif
</td>
</tr>
@endfor
@if(trades == null || trades.isEmpty())
<tr>
<td colspan="10">@template._components.ui.emptyState(message = noTradeDataLabel, widenRangeAction = !safeFrom.isEmpty() || !safeTo.isEmpty())</td>
</tr>
@endif
</tbody>
</table>
</div>

@if(pagination != null && totalPages > 1)
@template._components.htmxPagination(
	pagination = pagination,
	url = "/stock/htmx/trade-history?from=" + safeFrom + "&to=" + safeTo + "&size=" + pageSize + safeFilterQuery,
	includeSelector = "this",
	targetSelector = "#trade-history-panel")
@endif
@endif
`)