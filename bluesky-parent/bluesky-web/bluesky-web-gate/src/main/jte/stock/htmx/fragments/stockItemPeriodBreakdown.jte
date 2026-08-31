@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeProfitPeriodSummary
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradeProfitPeriodSummary> periodBreakdown = java.util.List.of()

!{
    DecimalFormat amountFormat = new DecimalFormat("#,##0");
    String title = MessageUtil.getMessage("stock.item.detail.breakdown.title");
    String description = MessageUtil.getMessage("stock.item.detail.breakdown.desc");
    String periodLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.period");
    String profitLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.profit");
    String unrealizedDeltaLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.unrealized.delta");
    String realizedDividendLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.realized.dividend");
    String returnLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.return");
    String partialLabel = MessageUtil.getMessage("stock.item.detail.breakdown.partial");
}

<%-- 구간이 하나뿐이면 표가 위의 합산 손익을 되풀이할 뿐이라 아무것도 더해 주지 않는다.
     ('이번달' 을 고르면 달 단위 쪼갬이 한 줄이 된다 - 그때는 그리지 않는다) --%>
@if(periodBreakdown != null && periodBreakdown.size() > 1)
    @template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-3", body = @`
            <div class="space-y-1">
                <h3 class="card-title text-base">${title}</h3>
                <p class="text-xs text-base-content/60">${description}</p>
            </div>
            <div class="overflow-x-auto">
                <table class="table table-zebra table-sm whitespace-nowrap" data-period-breakdown="${periodBreakdown.get(0).unit()}">
                    <thead>
                        <tr>
                            <th scope="col">${periodLabel}</th>
                            <th scope="col" class="text-right">${profitLabel}</th>
                            <th scope="col" class="text-right">${unrealizedDeltaLabel}</th>
                            <th scope="col" class="text-right">${realizedDividendLabel}</th>
                            <th scope="col" class="text-right">${returnLabel}</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(TradeProfitPeriodSummary row : periodBreakdown)
                            !{
                                var s = row.summary();
                                BigDecimal profit = s != null && s.periodProfit() != null ? s.periodProfit() : BigDecimal.ZERO;
                                // 손익 = 평가손익 변동 + (실현 + 배당). 위의 '합산 손익' 카드와 같은 쪼갬이라
                                // 두 자리가 같은 뜻을 말한다. 실현·배당은 따로 오지 않으므로 잔차로 구한다.
                                BigDecimal unrealizedStart = s != null && s.unrealizedStart() != null ? s.unrealizedStart() : BigDecimal.ZERO;
                                BigDecimal unrealizedEnd = s != null && s.unrealizedEnd() != null ? s.unrealizedEnd() : BigDecimal.ZERO;
                                BigDecimal unrealizedDelta = unrealizedEnd.subtract(unrealizedStart);
                                BigDecimal realizedAndDividend = profit.subtract(unrealizedDelta);
                                Double rate = s != null ? s.timeWeightedReturnPct() : null;
                                boolean profitUp = profit.signum() >= 0;
                                boolean deltaUp = unrealizedDelta.signum() >= 0;
                                boolean incomeUp = realizedAndDividend.signum() >= 0;
                                boolean rateUp = rate != null && rate >= 0.0d;
                            }
                            <tr>
                                <td class="font-medium">
                                    ${row.label()}
                                    @if(!row.complete() && row.fromDate() != null && row.toDate() != null)
                                        <%-- 조회 기간에 잘린 구간. 밝히지 않으면 그 달/해 전체 성과로 오해한다. --%>
                                        <div class="text-[11px] font-normal leading-4 text-base-content/60">
                                            ${partialLabel} ${row.fromDate().toString().substring(5)} ~ ${row.toDate().toString().substring(5)}
                                        </div>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    <span class="amount-value font-semibold ${profitUp ? "text-profit" : "text-loss"}">${profitUp ? "+" : "-"}&#8361;${amountFormat.format(profit.abs())}</span>
                                </td>
                                <td class="text-right tabular-nums">
                                    @if(unrealizedDelta.signum() != 0)
                                        <span class="amount-value ${deltaUp ? "text-profit" : "text-loss"}">${deltaUp ? "+" : "-"}&#8361;${amountFormat.format(unrealizedDelta.abs())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    @if(realizedAndDividend.signum() != 0)
                                        <span class="amount-value ${incomeUp ? "text-profit" : "text-loss"}">${incomeUp ? "+" : "-"}&#8361;${amountFormat.format(realizedAndDividend.abs())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    @if(rate != null)
                                        <span class="font-semibold ${rateUp ? "text-profit" : "text-loss"}">${String.format("%+.2f%%", rate)}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                            </tr>
                        @endfor
                    </tbody>
                </table>
            </div>
    `)
@endif
