@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.util.StockContributionUtil.Contribution
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Contribution> stockContributions = java.util.List.of()
@param Contribution stockContributionTotal = null
@param Contribution stockContributionOthers = null
@param int stockContributionOthersCount = 0

!{
    DecimalFormat amountFormat = new DecimalFormat("#,##0");
    String title = MessageUtil.getMessage("stock.asset.growth.contribution.title");
    String description = MessageUtil.getMessage("stock.asset.growth.contribution.desc");
    String stockLabel = MessageUtil.getMessage("common.label.stock");
    String contributionLabel = MessageUtil.getMessage("stock.asset.growth.contribution.col.contribution");
    String shareLabel = MessageUtil.getMessage("stock.analytics.account.holdings.weight");
    String unrealizedDeltaLabel = MessageUtil.getMessage("stock.item.detail.breakdown.col.unrealized.delta");
    String realizedProfitLabel = MessageUtil.getMessage("stock.profit.realized");
    String dividendLabel = MessageUtil.getMessage("stock.asset.status.col.dividend.total");
    String soldOutLabel = MessageUtil.getMessage("stock.asset.growth.contribution.sold.out");
    String sumLabel = MessageUtil.getMessage("stock.asset.growth.cost.sum.row");
    String othersLabel = stockContributionOthersCount > 0
            ? java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.asset.growth.contribution.others"),
                String.valueOf(stockContributionOthersCount))
            : "";
    BigDecimal totalContribution = stockContributionTotal != null
            ? stockContributionTotal.contribution() : BigDecimal.ZERO;
}

<%-- 종목이 하나뿐이면 표가 위의 기간 손익 카드를 그대로 되풀이할 뿐이다.
     접힌 줄도 세어야 한다 - 보이는 줄만 세면 접기가 표를 통째로 지운다. --%>
@if(stockContributions != null && stockContributionTotal != null
        && stockContributions.size() + stockContributionOthersCount > 1)
    @template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-3", body = @`
            <div class="space-y-1">
                <h3 class="card-title text-base">${title}</h3>
                <p class="text-xs text-base-content/60">${description}</p>
            </div>
            <div class="overflow-x-auto">
                <table class="table table-zebra table-sm whitespace-nowrap" data-stock-contribution>
                    <thead>
                        <tr>
                            <th scope="col">${stockLabel}</th>
                            <th scope="col" class="text-right">${contributionLabel}</th>
                            <th scope="col" class="text-right">${shareLabel}</th>
                            <th scope="col" class="text-right hidden md:table-cell">${unrealizedDeltaLabel}</th>
                            <th scope="col" class="text-right hidden lg:table-cell">${realizedProfitLabel}</th>
                            <th scope="col" class="text-right hidden lg:table-cell">${dividendLabel}</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(Contribution row : stockContributions)
                            !{
                                boolean up = row.contribution().signum() >= 0;
                                boolean deltaUp = row.unrealizedDelta().signum() >= 0;
                                boolean realizedUp = row.realizedProfit().signum() >= 0;
                                // 비중은 기간 손익 대비다. 손익이 0 이면 나눌 수 없다(음수 손익이면
                                // 음의 기여가 양의 비중으로 뒤집히므로 그때도 적지 않는다).
                                Double sharePct = totalContribution.signum() > 0
                                        ? row.contribution().multiply(BigDecimal.valueOf(100))
                                                .divide(totalContribution, 2, java.math.RoundingMode.HALF_UP).doubleValue()
                                        : null;
                            }
                            <tr>
                                <td class="font-medium">
                                    <a href="/stock/item?stockItemId=${row.stockItemId() != null ? row.stockItemId().toString() : ""}" class="link link-hover">${row.stockItemName() != null ? row.stockItemName() : "-"}</a>
                                    <%-- 그 기간에 다 판 종목. 밝히지 않으면 왜 자산 현황에 없는지 알 수 없다. --%>
                                    @if(!row.heldAtEnd())
                                        <span class="badge badge-ghost badge-sm ml-1">${soldOutLabel}</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums"><span class="amount-value font-semibold ${up ? "text-profit" : "text-loss"}">${up ? "+" : "-"}&#8361;${amountFormat.format(row.contribution().abs())}</span></td>
                                <td class="text-right tabular-nums">
                                    @if(sharePct != null)
                                        <span class="text-base-content/70">${String.format("%.1f%%", sharePct)}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums hidden md:table-cell">
                                    @if(row.unrealizedDelta().signum() != 0)
                                        <span class="amount-value ${deltaUp ? "text-profit" : "text-loss"}">${deltaUp ? "+" : "-"}&#8361;${amountFormat.format(row.unrealizedDelta().abs())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums hidden lg:table-cell">
                                    @if(row.realizedProfit().signum() != 0)
                                        <span class="amount-value ${realizedUp ? "text-profit" : "text-loss"}">${realizedUp ? "+" : "-"}&#8361;${amountFormat.format(row.realizedProfit().abs())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums hidden lg:table-cell">
                                    @if(row.dividendTotal().signum() != 0)
                                        <span class="amount-value text-dividend">&#8361;${amountFormat.format(row.dividendTotal())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                            </tr>
                        @endfor
                        <%-- 접어 넣은 나머지. 기여가 작은 줄까지 다 내면 전 기간이 44 줄이 된다
                             (실측 2026-09-07: 그중 25 줄이 100 만원 미만). 접어도 합계는 그대로다. --%>
                        @if(stockContributionOthers != null && stockContributionOthersCount > 0)
                            !{
                                boolean othersUp = stockContributionOthers.contribution().signum() >= 0;
                                boolean othersDeltaUp = stockContributionOthers.unrealizedDelta().signum() >= 0;
                                boolean othersRealizedUp = stockContributionOthers.realizedProfit().signum() >= 0;
                                Double othersShare = totalContribution.signum() > 0
                                        ? stockContributionOthers.contribution().multiply(BigDecimal.valueOf(100))
                                                .divide(totalContribution, 2, java.math.RoundingMode.HALF_UP).doubleValue()
                                        : null;
                            }
                            <tr class="text-base-content/70" data-stock-contribution-others>
                                <td>${othersLabel}</td>
                                <td class="text-right tabular-nums"><span class="amount-value ${othersUp ? "text-profit" : "text-loss"}">${othersUp ? "+" : "-"}&#8361;${amountFormat.format(stockContributionOthers.contribution().abs())}</span></td>
                                <td class="text-right tabular-nums">
                                    @if(othersShare != null)
                                        <span>${String.format("%.1f%%", othersShare)}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums hidden md:table-cell"><span class="amount-value ${othersDeltaUp ? "text-profit" : "text-loss"}">${othersDeltaUp ? "+" : "-"}&#8361;${amountFormat.format(stockContributionOthers.unrealizedDelta().abs())}</span></td>
                                <td class="text-right tabular-nums hidden lg:table-cell"><span class="amount-value ${othersRealizedUp ? "text-profit" : "text-loss"}">${othersRealizedUp ? "+" : "-"}&#8361;${amountFormat.format(stockContributionOthers.realizedProfit().abs())}</span></td>
                                <td class="text-right tabular-nums hidden lg:table-cell"><span class="amount-value text-dividend">&#8361;${amountFormat.format(stockContributionOthers.dividendTotal())}</span></td>
                            </tr>
                        @endif
                    </tbody>
                    <%-- 합계는 위 카드의 '기간 손익' 과 같아야 한다 - 같은 값을 종목으로 쪼갠 것이므로
                         두 자리가 어긋나면 어느 쪽이 맞는지 알 수 없다(실측 2026-09-07: 세 기간 모두 차이 0). --%>
                    !{ boolean totalUp = stockContributionTotal.contribution().signum() >= 0;
                       boolean totalDeltaUp = stockContributionTotal.unrealizedDelta().signum() >= 0;
                       boolean totalRealizedUp = stockContributionTotal.realizedProfit().signum() >= 0; }
                    <tfoot>
                        <tr class="border-t-2 border-base-content/20 font-semibold" data-stock-contribution-total>
                            <td>${sumLabel}</td>
                            <td class="text-right tabular-nums"><span class="amount-value ${totalUp ? "text-profit" : "text-loss"}">${totalUp ? "+" : "-"}&#8361;${amountFormat.format(stockContributionTotal.contribution().abs())}</span></td>
                            <td class="text-right tabular-nums"><span class="text-base-content/70">${totalContribution.signum() > 0 ? "100.0%" : "-"}</span></td>
                            <td class="text-right tabular-nums hidden md:table-cell"><span class="amount-value ${totalDeltaUp ? "text-profit" : "text-loss"}">${totalDeltaUp ? "+" : "-"}&#8361;${amountFormat.format(stockContributionTotal.unrealizedDelta().abs())}</span></td>
                            <td class="text-right tabular-nums hidden lg:table-cell"><span class="amount-value ${totalRealizedUp ? "text-profit" : "text-loss"}">${totalRealizedUp ? "+" : "-"}&#8361;${amountFormat.format(stockContributionTotal.realizedProfit().abs())}</span></td>
                            <td class="text-right tabular-nums hidden lg:table-cell"><span class="amount-value text-dividend">&#8361;${amountFormat.format(stockContributionTotal.dividendTotal())}</span></td>
                        </tr>
                    </tfoot>
                </table>
            </div>
    `)
@endif
