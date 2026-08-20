@import java.util.List
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeProfitYearlySummary
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradeProfitYearlySummary> yearlySummaries = java.util.List.of()

!{
    DecimalFormat amountFormat = new DecimalFormat("#,##0");
    String title = MessageUtil.getMessage("stock.asset.growth.yearly.title");
    String description = MessageUtil.getMessage("stock.asset.growth.yearly.desc");
    String yearLabel = MessageUtil.getMessage("stock.asset.growth.yearly.col.year");
    String twrLabel = MessageUtil.getMessage("stock.asset.growth.yearly.col.twr");
    String profitLabel = MessageUtil.getMessage("stock.asset.growth.yearly.col.profit");
    String principalLabel = MessageUtil.getMessage("stock.asset.growth.yearly.col.principal");
    String closingLabel = MessageUtil.getMessage("stock.asset.growth.yearly.col.closing");
}

@if(yearlySummaries != null && !yearlySummaries.isEmpty())
    @template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-3", body = @`
            <div class="space-y-1">
                <h3 class="card-title text-base">${title}</h3>
                <p class="text-xs text-base-content/50">${description}</p>
            </div>
            <div class="overflow-x-auto">
                <table class="table table-zebra table-sm whitespace-nowrap">
                    <thead>
                        <tr>
                            <th scope="col">${yearLabel}</th>
                            <th scope="col" class="text-right">${twrLabel}</th>
                            <th scope="col" class="text-right">${profitLabel}</th>
                            <th scope="col" class="text-right">${principalLabel}</th>
                            <th scope="col" class="text-right">${closingLabel}</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(TradeProfitYearlySummary row : yearlySummaries)
                            !{
                                var s = row.summary();
                                Double twr = s != null ? s.timeWeightedReturnPct() : null;
                                java.math.BigDecimal profit = s != null ? s.periodProfit() : null;
                                java.math.BigDecimal principal = s != null ? s.principalDelta() : null;
                                java.math.BigDecimal closing = s != null ? s.closingValue() : null;
                                boolean twrUp = twr != null && twr >= 0.0d;
                                boolean profitUp = profit != null && profit.signum() >= 0;
                                boolean principalUp = principal != null && principal.signum() >= 0;
                            }
                            <tr>
                                <td class="font-medium">${String.valueOf(row.year())}</td>
                                <td class="text-right tabular-nums">
                                    @if(twr != null)
                                        <span class="font-semibold ${twrUp ? "text-profit" : "text-loss"}">${String.format("%+.2f%%", twr)}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    @if(profit != null)
                                        <span class="amount-value ${profitUp ? "text-profit" : "text-loss"}">${profitUp ? "+" : "-"}₩${amountFormat.format(profit.abs())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    @if(principal != null && principal.signum() != 0)
                                        <span class="amount-value text-base-content/60">${principalUp ? "+" : "-"}₩${amountFormat.format(principal.abs())}</span>
                                    @else
                                        <span class="text-base-content/30">-</span>
                                    @endif
                                </td>
                                <td class="text-right tabular-nums">
                                    @if(closing != null)
                                        <span class="amount-value text-base-content/70">₩${amountFormat.format(closing)}</span>
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
