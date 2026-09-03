@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.util.StockTradePeriodUtil.TradePeriod
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradePeriod> tradePeriods = java.util.List.of()

!{
    DecimalFormat amountFormat = new DecimalFormat("#,##0");
    String unit = tradePeriods != null && !tradePeriods.isEmpty() ? tradePeriods.get(0).unit() : "MONTH";
    // 자산 성장의 성과 표와 같은 메시지 키를 쓴다 - 두 화면이 같은 단위를 다른 이름으로 부르면 견줄 수 없다.
    String title = "YEAR".equals(unit)
            ? MessageUtil.getMessage("stock.asset.growth.yearly.title")
            : MessageUtil.getMessage("stock.asset.growth.monthly.title");
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
}

<%-- 구간이 하나뿐이면 표가 위의 요약 카드를 되풀이할 뿐이라 아무것도 더해 주지 않는다. --%>
@if(tradePeriods != null && tradePeriods.size() > 1)
    @template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-3", body = @`
            <div class="space-y-1">
                <h3 class="card-title text-base">${title}</h3>
                <p class="text-xs text-base-content/60">${description}</p>
            </div>
            <div class="overflow-x-auto">
                <table class="table table-zebra table-sm whitespace-nowrap" data-trade-breakdown="${unit}">
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
                                <td class="font-medium">
                                    ${row.label()}
                                    <%-- 매수만 있었는지 팔기만 했는지가 그 구간을 읽는 첫 단서다. --%>
                                    <div class="text-[11px] font-normal leading-4 text-base-content/60">
                                        ${buyLabel} ${String.valueOf(row.buyCount())} · ${sellLabel} ${String.valueOf(row.sellCount())}
                                    </div>
                                </td>
                                <td class="text-right tabular-nums">${String.valueOf(row.count())}</td>
                                <td class="text-right tabular-nums">
                                    @if(row.buyAmount().signum() != 0)
                                        <span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.buyAmount())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    @if(row.sellAmount().signum() != 0)
                                        <span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.sellAmount())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <%-- 수수료·거래세는 나간 돈이라 부호를 붙이지 않고 금액만 적는다. --%>
                                <td class="text-right tabular-nums">
                                    @if(row.fee().signum() != 0)
                                        <span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.fee())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    @if(row.tax().signum() != 0)
                                        <span class="amount-value text-base-content/70">&#8361;${amountFormat.format(row.tax())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    <%-- 판 것이 없으면 실현손익은 0 이 아니라 '없음' 이다. 0 원과 뜻이 다르다. --%>
                                    @if(row.sellCount() == 0)
                                        <span class="text-base-content/30">-</span>
                                    @else
                                        <span class="amount-value font-semibold ${realizedUp ? "text-profit" : "text-loss"}">${realizedUp ? "+" : "-"}&#8361;${amountFormat.format(realized.abs())}</span>
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
                            <td>
                                ${sumLabel}
                                <div class="text-[11px] font-normal leading-4 text-base-content/60">
                                    ${buyLabel} ${String.valueOf(totals.buyCount())} · ${sellLabel} ${String.valueOf(totals.sellCount())}
                                </div>
                            </td>
                            <td class="text-right tabular-nums">${String.valueOf(totals.count())}</td>
                            <%-- 0 이면 '-' 로 둔다 - 줄과 같은 규칙이어야 한 열이 위아래로 다르게 읽히지 않는다. --%>
                            <td class="text-right tabular-nums">
                                @if(totals.buyAmount().signum() != 0)
                                    <span class="amount-value text-base-content/70">&#8361;${amountFormat.format(totals.buyAmount())}</span>
                                @else
                                    <span class="text-base-content/30">-</span>
                                @endif
                            </td>
                            <td class="text-right tabular-nums">
                                @if(totals.sellAmount().signum() != 0)
                                    <span class="amount-value text-base-content/70">&#8361;${amountFormat.format(totals.sellAmount())}</span>
                                @else
                                    <span class="text-base-content/30">-</span>
                                @endif
                            </td>
                            <td class="text-right tabular-nums">
                                @if(totals.fee().signum() != 0)
                                    <span class="amount-value text-base-content/70">&#8361;${amountFormat.format(totals.fee())}</span>
                                @else
                                    <span class="text-base-content/30">-</span>
                                @endif
                            </td>
                            <td class="text-right tabular-nums">
                                @if(totals.tax().signum() != 0)
                                    <span class="amount-value text-base-content/70">&#8361;${amountFormat.format(totals.tax())}</span>
                                @else
                                    <span class="text-base-content/30">-</span>
                                @endif
                            </td>
                            <td class="text-right tabular-nums">
                                <%-- 고른 기간에 판 것이 하나도 없으면 실현손익은 0 이 아니라 '없음' 이다. --%>
                                @if(totals.sellCount() == 0)
                                    <span class="text-base-content/30">-</span>
                                @else
                                    <span class="amount-value font-semibold ${totalRealizedUp ? "text-profit" : "text-loss"}">${totalRealizedUp ? "+" : "-"}&#8361;${amountFormat.format(totals.realizedProfit().abs())}</span>
                                @endif
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>
    `)
@endif
