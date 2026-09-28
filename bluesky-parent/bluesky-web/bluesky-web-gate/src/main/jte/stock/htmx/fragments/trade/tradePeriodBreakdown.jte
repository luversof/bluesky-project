@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.util.StockTradePeriodUtil.TradePeriod
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradePeriod> tradePeriods = java.util.List.of()

!{
	// 0 을 "-" 로 적는 자리에는 그 뜻을 달아 둔다(값이 없는 칸의 "-" 와 구분). 공용 규칙은 _components/ui/amountCell 과 같다.
	String zeroAmountTitle = MessageUtil.getMessage("common.amount.zero.title");
	// 판 것이 없어 비는 자리에도 까닭을 적는다 - 실측 2026-09-12: 이 표의 "-" 세 칸(2024·2017·2009)에는
	// title 도 sr-only 도 없어 마우스로도 낭독기로도 왜 비었는지 알 수 없었다. 같은 줄의 다른 칸은 "0원" 을 달고 있었다.
	String noSaleTitle = MessageUtil.getMessage("stock.trade.breakdown.realized.none");
	DecimalFormat amountFormat = net.luversof.web.gate.stock.util.StockFormatUtil.wonFormat();
	String unit = tradePeriods != null && !tradePeriods.isEmpty() ? tradePeriods.get(0).unit() : "MONTH";
	// 이 표는 '성과' 가 아니라 거래 집계다(건수·매수·매도·수수료·거래세). 자산 성장의 성과 표와
	// 같은 이름을 쓰면 화면에 '연도별 성과' 라는 제목으로 전혀 다른 열이 뜬다(실측 2026-09-07:
	// 자산 성장 / 종목 상세 / 매매 세 화면이 모두 '연도별 성과' 였다). 단위만 같은 결로 맞춘다.
	String title = "YEAR".equals(unit)
			? MessageUtil.getMessage("stock.trade.breakdown.title.year")
			: MessageUtil.getMessage("stock.trade.breakdown.title.month");
	String description = MessageUtil.getMessage("stock.trade.breakdown.desc");
	String periodLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.period");
	String countLabel = MessageUtil.getMessage("stock.trade.breakdown.col.count");
	String buyLabel = MessageUtil.getMessage("stock.trade.label.buy");
	String sellLabel = MessageUtil.getMessage("stock.trade.label.sell");
	String feeLabel = MessageUtil.getMessage("stock.trade.label.fee");
	String tradingTaxLabel = MessageUtil.getMessage("stock.trade.label.trading.tax");
	String realizedProfitLabel = MessageUtil.getMessage("stock.trade.label.realized.profit");
	// 연도별 세금·비용 표와 같은 키 - 두 표가 같은 줄을 다른 이름으로 부르지 않는다.
	String sumLabel = MessageUtil.getMessage("stock.asset.growth.cost.sum.row");
	// 표를 못 낼 때 남길 까닭. 고르는 규칙은 자산 성장·종목 상세와 한 곳에서 나온다.
	String breakdownNote = net.luversof.web.gate.stock.util.StockBreakdownNoteUtil
			.tradeNote(tradePeriods == null ? 0 : tradePeriods.size());
}

<%-- 구간이 하나뿐이면 표가 위의 요약 카드를 되풀이할 뿐이라 아무것도 더해 주지 않는다. --%>
@if(tradePeriods != null && tradePeriods.size() > 1)
	@template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-3", body = @`
			<div class="space-y-1">
				<h3 class="card-title text-base" data-page-section>${title}</h3>
				<p class="text-xs text-base-content/60">${description}</p>
			</div>
			<div class="overflow-x-auto">
				<table class="table table-zebra table-sm whitespace-nowrap" data-trade-breakdown="${unit}" aria-label="${title}">
					<thead>
						<tr>
							<th scope="col">${periodLabel}</th>
							<th scope="col" class="text-right">${countLabel}</th>
							<th scope="col" class="text-right">${buyLabel}</th>
							<th scope="col" class="text-right">${sellLabel}</th>
							<th scope="col" class="text-right">${feeLabel}</th>
							<th scope="col" class="text-right">${tradingTaxLabel}</th>
							<th scope="col" class="text-right">${realizedProfitLabel}</th>
						</tr>
					</thead>
					<tbody>
						@for(TradePeriod row : tradePeriods)
							!{
								BigDecimal realized = row.realizedProfit();
								boolean realizedUp = realized.signum() >= 0;
							}
							<tr>
								<th scope="row" class="font-medium">
									${row.label()}
									<%-- 매수만 있었는지 팔기만 했는지가 그 구간을 읽는 첫 단서다. --%>
									<div class="text-[11px] font-normal leading-4 text-base-content/60">
										${buyLabel} ${String.valueOf(row.buyCount())} · ${sellLabel} ${String.valueOf(row.sellCount())}
									</div>
								</th>
								<td class="text-right tabular-nums">${String.valueOf(row.count())}</td>
								<td class="text-right tabular-nums">
									@if(row.buyAmount().signum() != 0)
										<span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.buyAmount())}</span>
									@else
										<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>
									@endif
								</td>
								<td class="text-right tabular-nums">
									@if(row.sellAmount().signum() != 0)
										<span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.sellAmount())}</span>
									@else
										<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>
									@endif
								</td>
								<%-- 수수료·거래세는 나간 돈이라 부호를 붙이지 않고 금액만 적는다. --%>
								<td class="text-right tabular-nums">
									@if(row.fee().signum() != 0)
										<span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.fee())}</span>
									@else
										<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>
									@endif
								</td>
								<td class="text-right tabular-nums">
									@if(row.tax().signum() != 0)
										<span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.tax())}</span>
									@else
										<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>
									@endif
								</td>
								<td class="text-right tabular-nums">
									<%-- 판 것이 없으면 실현손익은 0 이 아니라 '없음' 이다. 0 원과 뜻이 다르다. --%>
									@if(row.sellCount() == 0)
										<span class="text-base-content/60" aria-hidden="true" title="${noSaleTitle}">-</span><span class="sr-only">${noSaleTitle}</span>
									@else
										<span class="amount-value font-semibold ${realizedUp ? "text-profit" : "text-loss"}">${realized.signum() == 0 ? "" : (realizedUp ? "+" : "-")}&#8361;${amountFormat.format(realized.abs())}</span>
									@endif
								</td>
							</tr>
						@endfor
					</tbody>
					<%-- 합계 줄. 여덟 값이 모두 그냥 더하면 되는 값이다 - 성과 표처럼 곱해서 이어야 하는
						 수익률도, 합계라는 것이 없는 기말 평가액도 없다. 그래서 '전체 기간' 이 아니라 '합계'.
						 이 줄은 화면 위 요약 카드와 같아야 한다 - 같은 원장을 같은 규칙으로 더한 값이다. --%>
					!{ var totals = net.luversof.web.gate.stock.util.StockTradePeriodUtil.total(tradePeriods);
					   boolean totalRealizedUp = totals.realizedProfit().signum() >= 0; }
					<tfoot>
						<tr class="border-t-2 border-base-content/20 font-semibold" data-trade-breakdown-total>
							<th scope="row">
								${sumLabel}
								<div class="text-[11px] font-normal leading-4 text-base-content/60">
									${buyLabel} ${String.valueOf(totals.buyCount())} · ${sellLabel} ${String.valueOf(totals.sellCount())}
								</div>
							</th>
							<td class="text-right tabular-nums">${String.valueOf(totals.count())}</td>
							<%-- 0 이면 '-' 로 둔다 - 줄과 같은 규칙이어야 한 열이 위아래로 다르게 읽히지 않는다. --%>
							<td class="text-right tabular-nums">
								@if(totals.buyAmount().signum() != 0)
									<span class="amount-value text-base-content/70">&#8361;${amountFormat.format(totals.buyAmount())}</span>
								@else
									<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>
								@endif
							</td>
							<td class="text-right tabular-nums">
								@if(totals.sellAmount().signum() != 0)
									<span class="amount-value text-base-content/70">&#8361;${amountFormat.format(totals.sellAmount())}</span>
								@else
									<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>
								@endif
							</td>
							<td class="text-right tabular-nums">
								@if(totals.fee().signum() != 0)
									<span class="amount-value text-base-content/70">&#8361;${amountFormat.format(totals.fee())}</span>
								@else
									<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>
								@endif
							</td>
							<td class="text-right tabular-nums">
								@if(totals.tax().signum() != 0)
									<span class="amount-value text-base-content/70">&#8361;${amountFormat.format(totals.tax())}</span>
								@else
									<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>
								@endif
							</td>
							<td class="text-right tabular-nums">
								<%-- 고른 기간에 판 것이 하나도 없으면 실현손익은 0 이 아니라 '없음' 이다.
									 그 까닭은 줄과 마찬가지로 마우스와 낭독기 모두에 닿아야 한다 - 여기만 맨 '-' 였다. --%>
								@if(totals.sellCount() == 0)
									<span class="text-base-content/60" aria-hidden="true" title="${noSaleTitle}">-</span><span class="sr-only">${noSaleTitle}</span>
								@else
									<span class="amount-value font-semibold ${totalRealizedUp ? "text-profit" : "text-loss"}">${totals.realizedProfit().signum() == 0 ? "" : (totalRealizedUp ? "+" : "-")}&#8361;${amountFormat.format(totals.realizedProfit().abs())}</span>
								@endif
							</td>
						</tr>
					</tfoot>
				</table>
			</div>
	`)
<%-- 표를 낼 수 없는 구간이면 자리를 비우지 않고 까닭을 적는다 - 형제 조각(periodBreakdownTable)이 이미
     그렇게 한다. 실측 2026-09-16(빈 기간): 이 표만 제목째 사라져 같은 화면의 다른 구역과 어긋났다. --%>
@elseif(!breakdownNote.isEmpty())
	@template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-2", body = @`
			<h3 class="card-title text-base" data-page-section>${title}</h3>
			<p class="text-xs text-base-content/60" data-trade-breakdown-note>${breakdownNote}</p>
	`)
@endif
