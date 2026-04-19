@import java.math.BigDecimal
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.TradeProfit

@param Map<String, TradeProfit> accountTotalMap
@param List<TradeProfit> stockAggregated
@param BigDecimal totalEvaluationAmount
@param BigDecimal totalEvaluationProfit

<div class="flex flex-col gap-6">

    <!-- Section 1: 계좌별 현황 테이블 -->
    <div>
        <h2 class="text-lg font-semibold mb-3 opacity-70">계좌별 현황</h2>
        <div class="card bg-base-100 shadow-md overflow-x-auto">
            <table class="table table-sm w-full">
                <thead class="bg-base-200">
                    <tr>
                        <th>계좌명</th>
                        <th class="text-right">평가금액</th>
                        <th class="text-right hidden sm:table-cell">투자원금</th>
                        <th class="text-right">평가손익</th>
                    </tr>
                </thead>
                <tbody>
                @for(Map.Entry<String, TradeProfit> entry : accountTotalMap.entrySet())
                    !{TradeProfit acc = entry.getValue();}
                    !{BigDecimal principal = acc.evaluationAmount() != null && acc.evaluationProfit() != null
                        ? acc.evaluationAmount().subtract(acc.evaluationProfit())
                        : (acc.totalBuyCost() != null ? acc.totalBuyCost() : BigDecimal.ZERO);}
                    !{double rate = principal.compareTo(BigDecimal.ZERO) > 0 && acc.evaluationProfit() != null
                        ? acc.evaluationProfit().doubleValue() / principal.doubleValue() * 100
                        : 0.0;}
                    <tr class="hover">
                        <td>
                            <div class="font-semibold">${entry.getKey()}</div>
                            <div class="text-xs opacity-50">주식</div>
                        </td>
                        <td class="text-right font-bold font-mono amount-value text-principal">
                            ${String.format("%,d", acc.evaluationAmount() != null ? acc.evaluationAmount().longValue() : 0)}
                        </td>
                        <td class="text-right font-mono hidden sm:table-cell amount-value opacity-70">
                            ${String.format("%,d", principal.longValue())}
                        </td>
                        <td class="text-right">
                            <div class="font-bold font-mono amount-value ${acc.evaluationProfit() != null && acc.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                ${String.format("%+,d", acc.evaluationProfit() != null ? acc.evaluationProfit().longValue() : 0)}
                            </div>
                            <div class="text-xs font-semibold ${acc.evaluationProfit() != null && acc.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                ${String.format("%+.1f%%", rate)}
                            </div>
                        </td>
                    </tr>
                @endfor
                </tbody>
                <tfoot class="bg-base-200 font-bold">
                    !{BigDecimal totalPrincipal = totalEvaluationAmount != null && totalEvaluationProfit != null
                        ? totalEvaluationAmount.subtract(totalEvaluationProfit)
                        : BigDecimal.ZERO;}
                    <tr>
                        <td>합계</td>
                        <td class="text-right font-mono amount-value text-principal">
                            ${String.format("%,d", totalEvaluationAmount != null ? totalEvaluationAmount.longValue() : 0)}
                        </td>
                        <td class="text-right font-mono hidden sm:table-cell amount-value opacity-70">
                            ${String.format("%,d", totalPrincipal.longValue())}
                        </td>
                        <td class="text-right font-mono amount-value ${totalEvaluationProfit != null && totalEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", totalEvaluationProfit != null ? totalEvaluationProfit.longValue() : 0)}
                        </td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>

    <!-- Section 2: 종목별 집계 테이블 (계좌 통합) -->
    <div>
        <h2 class="text-lg font-semibold mb-3 opacity-70">종목별 현황 <span class="text-xs font-normal opacity-60">(전체 계좌 합산)</span></h2>
        <div class="card bg-base-100 shadow-md overflow-x-auto">
            <table class="table table-sm table-zebra w-full">
                <thead class="bg-base-200">
                    <tr>
                        <th>종목명</th>
                        <th class="text-right hidden lg:table-cell">보유수량</th>
                        <th class="text-right hidden xl:table-cell">평균단가</th>
                        <th class="text-right hidden lg:table-cell">현재가</th>
                        <th class="text-right">평가금액</th>
                        <th class="text-right hidden sm:table-cell">투자원금</th>
                        <th class="text-right">평가손익</th>
                    </tr>
                </thead>
                <tbody>
                    @if(stockAggregated == null || stockAggregated.isEmpty())
                        <tr>
                            <td colspan="7" class="text-center py-8 opacity-50">보유 종목이 없습니다.</td>
                        </tr>
                    @else
                        @for(TradeProfit item : stockAggregated)
                            !{BigDecimal evalProfitRate = item.totalBuyCost() != null
                                && item.totalBuyCost().compareTo(BigDecimal.ZERO) > 0
                                && item.evaluationProfit() != null
                                ? item.evaluationProfit().divide(item.totalBuyCost(), 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;}
                            <tr class="hover">
                                <td class="font-semibold">${item.stockItemName() != null ? item.stockItemName() : "-"}</td>
                                <td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", item.holdingQuantity())}</td>
                                <td class="text-right font-mono hidden xl:table-cell amount-value">${String.format("%,d", item.averageBuyPrice() != null ? item.averageBuyPrice().longValue() : 0)}</td>
                                <td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", item.currentPrice() != null ? item.currentPrice().longValue() : 0)}</td>
                                <td class="text-right font-mono font-semibold amount-value text-principal">${String.format("%,d", item.evaluationAmount() != null ? item.evaluationAmount().longValue() : 0)}</td>
                                <td class="text-right font-mono hidden sm:table-cell amount-value opacity-70">${String.format("%,d", item.totalBuyCost() != null ? item.totalBuyCost().longValue() : 0)}</td>
                                <td class="text-right font-mono font-bold amount-value ${item.evaluationProfit() != null && item.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                    ${String.format("%+,d", item.evaluationProfit() != null ? item.evaluationProfit().longValue() : 0)}
                                    <span class="text-xs opacity-70">(${String.format("%+.1f%%", evalProfitRate.doubleValue())})</span>
                                </td>
                            </tr>
                        @endfor
                    @endif
                </tbody>
                <tfoot class="bg-base-200 font-bold">
                    !{BigDecimal totalStockPrincipal = stockAggregated != null
                        ? stockAggregated.stream().map(i -> i.totalBuyCost() != null ? i.totalBuyCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;}
                    <tr>
                        <td>합계</td>
                        <td class="hidden lg:table-cell"></td>
                        <td class="hidden xl:table-cell"></td>
                        <td class="hidden lg:table-cell"></td>
                        <td class="text-right font-mono amount-value text-principal">${String.format("%,d", totalEvaluationAmount != null ? totalEvaluationAmount.longValue() : 0)}</td>
                        <td class="text-right font-mono hidden sm:table-cell amount-value opacity-70">${String.format("%,d", totalStockPrincipal.longValue())}</td>
                        <td class="text-right font-mono font-bold amount-value ${totalEvaluationProfit != null && totalEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">${String.format("%+,d", totalEvaluationProfit != null ? totalEvaluationProfit.longValue() : 0)}</td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>

</div>
