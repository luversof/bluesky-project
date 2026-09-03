@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.YearlyCostSummary
@import io.github.luversof.boot.context.support.MessageUtil

@param List<YearlyCostSummary> yearlyCosts = java.util.List.of()

!{
    DecimalFormat amountFormat = new DecimalFormat("#,##0");
    String title = MessageUtil.getMessage("stock.asset.growth.cost.title");
    String description = MessageUtil.getMessage("stock.asset.growth.cost.desc");
    String yearLabel = MessageUtil.getMessage("stock.asset.growth.yearly.col.year");
    String feeLabel = MessageUtil.getMessage("stock.asset.growth.cost.col.fee");
    String tradeTaxLabel = MessageUtil.getMessage("stock.asset.growth.cost.col.trade.tax");
    String realizedLabel = MessageUtil.getMessage("stock.profit.realized");
    String grossLabel = MessageUtil.getMessage("stock.asset.growth.cost.col.dividend.gross");
    String taxableLabel = MessageUtil.getMessage("stock.asset.growth.cost.col.dividend.taxable");
    String withholdingLabel = MessageUtil.getMessage("stock.asset.growth.cost.col.dividend.tax");
    String netLabel = MessageUtil.getMessage("stock.asset.growth.cost.col.dividend.net");
    String sumLabel = MessageUtil.getMessage("stock.asset.growth.cost.sum.row");
}

@if(yearlyCosts != null && !yearlyCosts.isEmpty())
    @template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-3", body = @`
            <div class="space-y-1">
                <h3 class="card-title text-base">${title}</h3>
                <p class="text-xs text-base-content/60">${description}</p>
            </div>
            <div class="overflow-x-auto">
                <table class="table table-zebra table-sm whitespace-nowrap" data-yearly-cost>
                    <thead>
                        <tr>
                            <th scope="col">${yearLabel}</th>
                            <th scope="col" class="text-right">${realizedLabel}</th>
                            <th scope="col" class="text-right">${feeLabel}</th>
                            <th scope="col" class="text-right">${tradeTaxLabel}</th>
                            <th scope="col" class="text-right">${grossLabel}</th>
                            <th scope="col" class="text-right">${taxableLabel}</th>
                            <th scope="col" class="text-right">${withholdingLabel}</th>
                            <th scope="col" class="text-right">${netLabel}</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(YearlyCostSummary row : yearlyCosts)
                            !{
                                BigDecimal realized = row.realizedProfit() != null ? row.realizedProfit() : BigDecimal.ZERO;
                                boolean realizedUp = realized.signum() >= 0;
                            }
                            <tr>
                                <td class="font-medium">${String.valueOf(row.year())}</td>
                                <td class="text-right tabular-nums">
                                    @if(realized.signum() != 0)
                                        <span class="amount-value font-semibold ${realizedUp ? "text-profit" : "text-loss"}">${realizedUp ? "+" : "-"}&#8361;${amountFormat.format(realized.abs())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <%-- 수수료·거래세는 나간 돈이라 부호를 붙이지 않고 금액만 적는다. --%>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.tradeFee() != null ? row.tradeFee() : BigDecimal.ZERO)}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.tradeTax() != null ? row.tradeTax() : BigDecimal.ZERO)}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.dividendGross() != null ? row.dividendGross() : BigDecimal.ZERO)}</span></td>
                                <%-- 과세금액은 세전에 세율을 곱한 값이 아니다 - 계좌별 분리과세 혜택 뒤 남은 몫이라
                                     세전보다 훨씬 작을 수 있다. 금융소득종합과세 판정은 이 값으로 한다. --%>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.dividendTaxable() != null ? row.dividendTaxable() : BigDecimal.ZERO)}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.dividendTax() != null ? row.dividendTax() : BigDecimal.ZERO)}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value font-semibold text-dividend">&#8361;${amountFormat.format(row.dividendNet() != null ? row.dividendNet() : BigDecimal.ZERO)}</span></td>
                            </tr>
                        @endfor
                    </tbody>
                    <%-- 줄이 하나뿐이면 합계가 그 줄을 그대로 되풀이할 뿐이라 아무것도 더해 주지 않는다.
                         (실측 2026-09-03: '올해' 를 고르면 연도별 세금·비용이 2026 한 줄인데 합계가 같은 수를 한 번 더 적었다) --%>
                    @if(yearlyCosts.size() > 1)
                        <%-- 합계 줄. 연말정산·금투세 때 필요한 수는 대개 '그 해' 가 아니라 '다 합쳐서 얼마' 인데,
                             표가 해마다 한 줄씩만 적고 있어 눈으로 더해야 했다.
                             성과 표와 달리 여기는 일곱 열이 모두 그냥 더하면 되는 값이다 - 곱해서 이어야 하는
                             수익률도, 합계라는 것이 없는 기말 평가액도 없다. 그래서 '전체 기간' 이 아니라 '합계'. --%>
                        !{ var costTotals = net.luversof.web.gate.stock.util.StockYearlyCostTotalUtil.of(yearlyCosts);
                           boolean costRealizedUp = costTotals.realizedProfit().signum() >= 0; }
                        <tfoot>
                            <tr class="border-t-2 border-base-content/20 font-semibold" data-yearly-cost-total>
                                <td>${sumLabel}</td>
                                <td class="text-right tabular-nums">
                                    @if(costTotals.realizedProfit().signum() != 0)
                                        <span class="amount-value ${costRealizedUp ? "text-profit" : "text-loss"}">${costRealizedUp ? "+" : "-"}&#8361;${amountFormat.format(costTotals.realizedProfit().abs())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <%-- 줄과 같은 규칙: 나간 돈에는 부호를 붙이지 않는다. --%>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(costTotals.tradeFee())}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(costTotals.tradeTax())}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(costTotals.dividendGross())}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(costTotals.dividendTaxable())}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value text-base-content/70">&#8361;${amountFormat.format(costTotals.dividendTax())}</span></td>
                                <td class="text-right tabular-nums"><span class="amount-value font-semibold text-dividend">&#8361;${amountFormat.format(costTotals.dividendNet())}</span></td>
                            </tr>
                        </tfoot>
                    @endif
                </table>
            </div>
    `)
@endif
