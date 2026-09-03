@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeProfitPeriodSummary
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradeProfitPeriodSummary> periodBreakdown = java.util.List.of()
@param String periodBreakdownNote = ""

!{
    DecimalFormat amountFormat = new DecimalFormat("#,##0");
    String unit = periodBreakdown != null && !periodBreakdown.isEmpty()
            ? periodBreakdown.get(0).unit() : "MONTH";
    // 아래 '연도별 성과' 와 같은 키를 쓴다 - 두 표의 이름이 갈라지지 않는다.
    // 키를 삼항 안에 두면 메시지 키 검사기가 "YEAR" 를 키로 읽는다. 호출을 둘로 나눠 둔다.
    String title = "YEAR".equals(unit)
            ? MessageUtil.getMessage("stock.asset.growth.yearly.title")
            : MessageUtil.getMessage("stock.asset.growth.monthly.title");
    String totalLabel = MessageUtil.getMessage("stock.asset.growth.total.row");
    String description = MessageUtil.getMessage("stock.item.detail.breakdown.desc");
    String periodLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.period");
    String profitLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.profit");
    String unrealizedDeltaLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.unrealized.delta");
    String realizedDividendLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.realized.dividend");
    String returnLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.return");
    String partialLabel = MessageUtil.getMessage("stock.item.detail.breakdown.partial");
}

<%-- 종목 상세와 자산 성장이 함께 쓴다 - 같은 계산이라 두 화면이 같은 뜻을 말한다.
     구간이 하나뿐이면 표가 위의 합계를 되풀이할 뿐이라 아무것도 더해 주지 않는다.
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
                    <%-- 전체 기간 줄. 표는 줄마다의 성과만 적고 '다 합치면 얼마인지' 가 없었다.
                         수익률은 더하면 틀린다 - 곱해서 잇는다(실측 2026-09-03: 연쇄곱이 위 요약
                         카드의 투자 수익률과 소수점 넷째 자리까지 같다). --%>
                    !{ var totals = net.luversof.web.gate.stock.util.StockPeriodTotalUtil.of(
                            periodBreakdown.stream().map(TradeProfitPeriodSummary::summary).toList());
                       boolean totalProfitUp = totals.profit().signum() >= 0;
                       boolean totalDeltaUp = totals.unrealizedDelta().signum() >= 0;
                       boolean totalIncomeUp = totals.realizedAndDividend().signum() >= 0;
                       boolean totalRateUp = totals.chainedReturnPct() != null && totals.chainedReturnPct() >= 0.0d; }
                    <tfoot>
                        <tr class="border-t-2 border-base-content/20 font-semibold" data-period-breakdown-total>
                            <td>${totalLabel}</td>
                            <td class="text-right tabular-nums"><span class="amount-value ${totalProfitUp ? "text-profit" : "text-loss"}">${totalProfitUp ? "+" : "-"}&#8361;${amountFormat.format(totals.profit().abs())}</span></td>
                            <td class="text-right tabular-nums"><span class="amount-value ${totalDeltaUp ? "text-profit" : "text-loss"}">${totalDeltaUp ? "+" : "-"}&#8361;${amountFormat.format(totals.unrealizedDelta().abs())}</span></td>
                            <td class="text-right tabular-nums"><span class="amount-value ${totalIncomeUp ? "text-profit" : "text-loss"}">${totalIncomeUp ? "+" : "-"}&#8361;${amountFormat.format(totals.realizedAndDividend().abs())}</span></td>
                            <td class="text-right tabular-nums">
                                @if(totals.chainedReturnPct() != null)
                                    <span class="${totalRateUp ? "text-profit" : "text-loss"}">${String.format("%+.2f%%", totals.chainedReturnPct())}</span>
                                @else
                                    <span class="text-base-content/30">-</span>
                                @endif
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>
    `)
@elseif(periodBreakdownNote != null && !periodBreakdownNote.isEmpty())
    <%-- 표를 낼 수 없는 구간이면 자리를 비우지 않고 까닭을 적는다. 말없이 사라지면
         기능이 있는지조차 알 수 없다(실측: 기본 화면 '전체' 에서 표가 통째로 없었다). --%>
    @template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-2", body = @`
            <h3 class="card-title text-base">${title}</h3>
            <p class="text-xs text-base-content/60" data-period-breakdown-note>${periodBreakdownNote}</p>
    `)
@endif
