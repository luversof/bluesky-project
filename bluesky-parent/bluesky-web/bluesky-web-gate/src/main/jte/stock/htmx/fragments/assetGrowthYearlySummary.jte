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
    String partialLabel = MessageUtil.getMessage("stock.asset.growth.yearly.partial");
    String gapTemplate = MessageUtil.getMessage("stock.asset.growth.yearly.gap");
}

@if(yearlySummaries != null && !yearlySummaries.isEmpty())
    @template._components.ui.card(cssClass = "mb-4", bodyClass = "gap-3", body = @`
            <div class="space-y-1">
                <h3 class="card-title text-base">${title}</h3>
                <p class="text-xs text-base-content/60">${description}</p>
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
                        <%-- 표는 보유도 거래도 없던 해를 아예 내지 않는다. 그래서 연도가 건너뛴 자리가 생기는데,
                             그대로 두면 자료가 빠진 것처럼 보인다(실측 2026-08-24: 2010 다음이 2014 이고 기말이
                             0 에서 갑자기 뛴다. 2011~2013 은 거래 0건 · 배당 0건 · 보유 0 으로 실제로 빈 해다).
                             건너뛴 구간을 한 줄로 밝힌다. --%>
                        !{Integer previousYear = null;}
                        @for(TradeProfitYearlySummary row : yearlySummaries)
                            @if(previousYear != null && previousYear - row.year() > 1)
                                <tr data-yearly-gap="${(row.year() + 1) + "-" + (previousYear - 1)}">
                                    <td colspan="5" class="text-center text-[11px] text-base-content/50">${java.text.MessageFormat.format(gapTemplate, String.valueOf(row.year() + 1), String.valueOf(previousYear - 1))}</td>
                                </tr>
                            @endif
                            !{previousYear = row.year();}
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
                                <td class="font-medium">
                                    ${String.valueOf(row.year())}
                                    @if(!row.fullYear() && row.fromDate() != null && row.toDate() != null)
                                        <%-- 조회 기간에 잘린 해. 구간을 밝히지 않으면 그 해 전체 성과로 오해한다. --%>
                                        <div class="text-[11px] font-normal leading-4 text-base-content/60">
                                            ${partialLabel} ${row.fromDate().toString().substring(5)} ~ ${row.toDate().toString().substring(5)}
                                        </div>
                                    @endif
                                </td>
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
