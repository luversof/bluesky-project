@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import io.github.luversof.boot.context.support.MessageUtil

@param BigDecimal totalAsset
@param BigDecimal totalUnrealizedProfit
@param BigDecimal totalRealizedProfit
@param BigDecimal totalDividend
@param double winRate
@param List<TradeProfit> topBuying
@param List<TradeProfit> bottomBuying
@param List<TradeProfit> topRealized
@param List<TradeProfit> bottomRealized
@param List<TradeProfit> topUnrealized
@param List<TradeProfit> bottomUnrealized
@param List<TradeProfit> topDividend
@param List<TradeProfit> bottomDividend
@param java.util.Map<String, BigDecimal> allocation
@param java.util.Map<String, BigDecimal> accountAllocation
@param BigDecimal displayPrincipal
@param BigDecimal displayCurrentEvaluationProfit
@param BigDecimal combinedAdjustmentAmount
@param BigDecimal holdingFeeAdjustment
@param BigDecimal manualPrincipalAdjustment
@param boolean showCharts = true

!{
    String currentTotalAssetLabel = MessageUtil.getMessage("stock.summary.label.current.total.asset");
    String principalLabel = MessageUtil.getMessage("stock.label.principal");
    String currentEvaluationProfitLabel = MessageUtil.getMessage("stock.summary.label.current.evaluation.profit");
    String returnRateLabel = MessageUtil.getMessage("stock.label.return.rate");
    String cumulativeRealizedIncomeLabel = MessageUtil.getMessage("stock.summary.label.cumulative.realized.income");
    String realizedProfitLabel = MessageUtil.getMessage("stock.profit.realized");
    String cumulativeDividendLabel = MessageUtil.getMessage("stock.summary.label.cumulative.dividend");
    String totalCombinedProfitLabel = MessageUtil.getMessage("stock.summary.label.total.combined.profit");
    String calculationDetailsLabel = MessageUtil.getMessage("stock.summary.label.calculation.details");
    String combinedAdjustmentLabel = MessageUtil.getMessage("stock.summary.label.combined.adjustment");
    String holdingFeeAdjustmentLabel = MessageUtil.getMessage("stock.summary.label.holding.fee.adjustment");
    String manualPrincipalAdjustmentLabel = MessageUtil.getMessage("stock.summary.label.manual.principal.adjustment");
    String combinedAdjustmentAddedLabel = MessageUtil.getMessage("stock.summary.label.combined.adjustment.added");
    String combinedAdjustmentSubtractedLabel = MessageUtil.getMessage("stock.summary.label.combined.adjustment.subtracted");
    String winRateLabel = MessageUtil.getMessage("stock.summary.label.win.rate");
    String profitableStockRatioLabel = MessageUtil.getMessage("stock.summary.label.profitable.stock.ratio");
    String topBuyingLabel = MessageUtil.getMessage("stock.summary.card.top.buying");
    String bottomBuyingLabel = MessageUtil.getMessage("stock.summary.card.bottom.buying");
    String topRealizedLabel = MessageUtil.getMessage("stock.summary.card.top.realized");
    String bottomRealizedLabel = MessageUtil.getMessage("stock.summary.card.bottom.realized");
    String topUnrealizedLabel = MessageUtil.getMessage("stock.summary.card.top.unrealized");
    String bottomUnrealizedLabel = MessageUtil.getMessage("stock.summary.card.bottom.unrealized");
    String noUnrealizedLossLabel = MessageUtil.getMessage("stock.summary.card.no.unrealized.loss");
    String topDividendLabel = MessageUtil.getMessage("stock.summary.card.top.dividend");
    String bottomDividendLabel = MessageUtil.getMessage("stock.summary.card.bottom.dividend");
    String tabBuyingLabel = MessageUtil.getMessage("stock.summary.tab.buying");
    String tabRealizedLabel = MessageUtil.getMessage("stock.summary.tab.realized");
    String tabUnrealizedLabel = MessageUtil.getMessage("stock.summary.tab.unrealized");
    String tabDividendLabel = MessageUtil.getMessage("stock.summary.tab.dividend");
}

!{BigDecimal currentPrincipal = displayPrincipal != null ? displayPrincipal : totalAsset.subtract(totalUnrealizedProfit);}
!{BigDecimal currentEvaluationProfit = displayCurrentEvaluationProfit != null ? displayCurrentEvaluationProfit : totalUnrealizedProfit;}
!{double unrealizedRate = currentPrincipal.compareTo(BigDecimal.ZERO) > 0 ? currentEvaluationProfit.doubleValue() / currentPrincipal.doubleValue() * 100 : 0.0;}
!{BigDecimal totalRealizedIncome = totalRealizedProfit.add(totalDividend);}
!{BigDecimal combinedAdjustment = combinedAdjustmentAmount != null ? combinedAdjustmentAmount : BigDecimal.ZERO;}
!{BigDecimal totalCombinedProfit = currentEvaluationProfit.add(totalRealizedIncome).subtract(combinedAdjustment);}
!{BigDecimal combinedPrincipal = totalAsset.subtract(totalUnrealizedProfit);}
!{boolean hasCombinedAdjustment = combinedAdjustment.compareTo(BigDecimal.ZERO) != 0;}
!{BigDecimal combinedAdjustmentAbs = combinedAdjustment.abs();}
!{String combinedAdjustmentOperator = combinedAdjustment.compareTo(BigDecimal.ZERO) < 0 ? "+" : "-";}
!{String combinedAdjustmentMeaning = hasCombinedAdjustment ? (combinedAdjustment.compareTo(BigDecimal.ZERO) < 0 ? combinedAdjustmentAddedLabel : combinedAdjustmentSubtractedLabel) : "";}
!{double combinedRate = combinedPrincipal.compareTo(BigDecimal.ZERO) > 0 ? totalCombinedProfit.doubleValue() / combinedPrincipal.doubleValue() * 100 : 0.0;}
!{String evalColor = currentEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";}
!{String combinedColor = totalCombinedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";}
!{String realizedColor = totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";}

<div class="flex flex-col gap-4">

    <!-- 핵심 요약: 총자산(히어로) + 통합손익 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-3">
        <div class="lg:col-span-2 card bg-base-100 border border-base-200 rounded-box shadow-sm">
            <div class="card-body p-5">
                <div class="flex items-center gap-1 text-sm text-base-content/60">
                    <span>${currentTotalAssetLabel}</span>
                    <span class="tooltip tooltip-right" data-tip="보유 종목의 현재 평가액 합계 (원금 + 평가손익)">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current opacity-60"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </span>
                </div>
                <div class="text-3xl lg:text-4xl font-bold text-principal amount-value mt-1" title="${String.format("%,d원", totalAsset.longValue())}">${StockFormatUtil.compactKrw(totalAsset.longValue())}</div>
                <div class="text-sm text-base-content/70 mt-2">
                    ${principalLabel} <span class="font-mono amount-value" title="${String.format("%,d원", currentPrincipal.longValue())}">${StockFormatUtil.compactKrw(currentPrincipal.longValue())}</span>
                </div>
            </div>
        </div>

        <div class="card bg-base-200 rounded-box">
            <div class="card-body p-5 justify-center">
                <div class="flex items-center gap-1 text-sm text-base-content/60">
                    <span>${totalCombinedProfitLabel}</span>
                    <div class="tooltip tooltip-bottom-end shrink-0">
                        <div class="tooltip-content z-[20] w-64 sm:w-72 lg:w-80 max-w-[calc(100vw-2rem)] rounded-box bg-neutral text-neutral-content shadow-lg">
                            <div class="space-y-1 text-left text-xs leading-5">
                                <div class="font-semibold mb-1">${calculationDetailsLabel}</div>
                                <div class="flex items-center justify-between gap-3">
                                    <span>${currentEvaluationProfitLabel}</span>
                                    <span class="amount-value font-medium">${String.format("%,d", currentEvaluationProfit.longValue())}</span>
                                </div>
                                <div class="flex items-center justify-between gap-3">
                                    <span>+ ${cumulativeRealizedIncomeLabel}</span>
                                    <span class="amount-value font-medium">${String.format("%,d", totalRealizedIncome.longValue())}</span>
                                </div>
                                @if(hasCombinedAdjustment)
                                <div class="flex items-center justify-between gap-3">
                                    <span>${combinedAdjustmentOperator} ${combinedAdjustmentLabel}</span>
                                    <span class="amount-value font-medium">${String.format("%,d", combinedAdjustmentAbs.longValue())}</span>
                                </div>
                                <div class="border-t border-neutral-content/20 pt-1 text-neutral-content/80">
                                    ${combinedAdjustmentMeaning}
                                </div>
                                @endif
                                @if(holdingFeeAdjustment != null && holdingFeeAdjustment.compareTo(BigDecimal.ZERO) != 0)
                                <div class="flex items-center justify-between gap-3 text-neutral-content/80">
                                    <span>${holdingFeeAdjustmentLabel}</span>
                                    <span class="amount-value">${String.format("%+,d", holdingFeeAdjustment.longValue())}</span>
                                </div>
                                @endif
                                @if(manualPrincipalAdjustment != null && manualPrincipalAdjustment.compareTo(BigDecimal.ZERO) != 0)
                                <div class="flex items-center justify-between gap-3 text-neutral-content/80">
                                    <span>${manualPrincipalAdjustmentLabel}</span>
                                    <span class="amount-value">${String.format("%+,d", manualPrincipalAdjustment.longValue())}</span>
                                </div>
                                @endif
                                <div class="flex items-center justify-between gap-3 border-t border-neutral-content/20 pt-1 font-semibold">
                                    <span>= ${totalCombinedProfitLabel}</span>
                                    <span class="amount-value">${String.format("%,d", totalCombinedProfit.longValue())}</span>
                                </div>
                            </div>
                        </div>
                        <button type="button" class="btn btn-circle btn-ghost btn-xs text-info align-middle">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        </button>
                    </div>
                </div>
                <div class="text-2xl lg:text-3xl font-bold amount-value mt-1 ${combinedColor}" title="${String.format("%,d원", totalCombinedProfit.longValue())}">${StockFormatUtil.compactKrw(totalCombinedProfit.longValue())}</div>
                <div class="text-sm font-semibold mt-1 ${combinedColor}">${returnRateLabel} ${String.format("%+.2f%%", combinedRate)}</div>
            </div>
        </div>
    </div>

    <!-- 보조 지표 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <div class="bg-base-200 rounded-box p-3">
            <div class="text-xs text-base-content/60">${currentEvaluationProfitLabel}</div>
            <div class="text-lg font-bold font-mono amount-value ${evalColor}" title="${String.format("%,d원", currentEvaluationProfit.longValue())}">${String.format("%,d", currentEvaluationProfit.longValue())}</div>
            <div class="text-xs font-semibold ${evalColor}">${returnRateLabel} ${String.format("%+.2f%%", unrealizedRate)}</div>
        </div>
        <div class="bg-base-200 rounded-box p-3">
            <div class="text-xs text-base-content/60">${realizedProfitLabel}</div>
            <div class="text-lg font-bold font-mono amount-value ${realizedColor}" title="${String.format("%,d원", totalRealizedProfit.longValue())}">${String.format("%,d", totalRealizedProfit.longValue())}</div>
        </div>
        <div class="bg-base-200 rounded-box p-3">
            <div class="text-xs text-base-content/60">${cumulativeDividendLabel}</div>
            <div class="text-lg font-bold font-mono text-dividend amount-value" title="${String.format("%,d원", totalDividend.longValue())}">${String.format("%,d", totalDividend.longValue())}</div>
        </div>
        <div class="bg-base-200 rounded-box p-3">
            <div class="text-xs text-base-content/60">${winRateLabel}</div>
            <div class="text-lg font-bold">${String.format("%.1f", winRate)}%</div>
            <div class="text-xs text-base-content/50">${profitableStockRatioLabel}</div>
        </div>
    </div>

    <!-- Charts Row -->
    @if(showCharts)
    <div class="grid grid-cols-1 xl:grid-cols-2 gap-4 w-full">
         @template.stock.htmx.fragments.components.chartsAllocation(allocation = allocation)
         @template.stock.htmx.fragments.components.chartsAccountAllocation(accountAllocation = accountAllocation)
    </div>
    @endif

    <!-- 랭킹: 4개 카테고리 × (상위/하위) 를 탭으로 통합 -->
    <div id="summaryRankPanel" class="card bg-base-100 border border-base-200 rounded-box shadow-sm">
        <div class="card-body p-3">
            <div role="tablist" class="tabs tabs-box mb-3 flex-nowrap overflow-x-auto" data-rank-tabs>
                <button type="button" role="tab" class="tab" data-rank-tab="0">${tabBuyingLabel}</button>
                <button type="button" role="tab" class="tab" data-rank-tab="1">${tabRealizedLabel}</button>
                <button type="button" role="tab" class="tab" data-rank-tab="2">${tabUnrealizedLabel}</button>
                <button type="button" role="tab" class="tab" data-rank-tab="3">${tabDividendLabel}</button>
            </div>

            <!-- 0: 매수 집중 -->
            <div data-rank-panel="0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
                !{int c0a = 1;}
                <div>
                    <h3 class="text-sm text-base-content/60 mb-2">${topBuyingLabel}</h3>
                    <div class="space-y-1">
                        @if(topBuying != null && !topBuying.isEmpty())
                        @for(TradeProfit item : topBuying)
                        <div class="flex justify-between items-center text-xs">
                            <span class="flex items-center gap-2 min-w-0"><span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${c0a++}</span><span class="truncate">${item.stockItemName()}</span></span>
                            <span class="font-mono font-bold amount-value shrink-0">${String.format("%,d", item.totalBuyCost() != null ? item.totalBuyCost().longValue() : 0)}</span>
                        </div>
                        @endfor
                        @else
                        <div class="text-xs opacity-40">-</div>
                        @endif
                    </div>
                </div>
                !{int c0b = 1;}
                <div>
                    <h3 class="text-sm text-base-content/60 mb-2">${bottomBuyingLabel}</h3>
                    <div class="space-y-1">
                        @if(bottomBuying != null && !bottomBuying.isEmpty())
                        @for(TradeProfit item : bottomBuying)
                        <div class="flex justify-between items-center text-xs">
                            <span class="flex items-center gap-2 min-w-0"><span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${c0b++}</span><span class="truncate">${item.stockItemName()}</span></span>
                            <span class="font-mono font-bold amount-value shrink-0">${String.format("%,d", item.totalBuyCost() != null ? item.totalBuyCost().longValue() : 0)}</span>
                        </div>
                        @endfor
                        @else
                        <div class="text-xs opacity-40">-</div>
                        @endif
                    </div>
                </div>
            </div>

            <!-- 1: 실현손익 -->
            <div data-rank-panel="1" class="grid grid-cols-1 md:grid-cols-2 gap-4 hidden">
                !{int c1a = 1;}
                <div>
                    <h3 class="text-sm text-base-content/60 mb-2">${topRealizedLabel}</h3>
                    <div class="space-y-1">
                        @if(topRealized != null && !topRealized.isEmpty())
                        @for(TradeProfit item : topRealized)
                        <div class="flex justify-between items-center text-xs">
                            <span class="flex items-center gap-2 min-w-0"><span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${c1a++}</span><span class="truncate">${item.stockItemName()}</span></span>
                            <span class="font-mono font-bold text-profit amount-value shrink-0">${String.format("%+,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}</span>
                        </div>
                        @endfor
                        @else
                        <div class="text-xs opacity-40">-</div>
                        @endif
                    </div>
                </div>
                !{int c1b = 1;}
                <div>
                    <h3 class="text-sm text-base-content/60 mb-2">${bottomRealizedLabel}</h3>
                    <div class="space-y-1">
                        @if(bottomRealized != null && !bottomRealized.isEmpty())
                        @for(TradeProfit item : bottomRealized)
                        <div class="flex justify-between items-center text-xs">
                            <span class="flex items-center gap-2 min-w-0"><span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${c1b++}</span><span class="truncate">${item.stockItemName()}</span></span>
                            <span class="font-mono font-bold text-loss amount-value shrink-0">${String.format("%+,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}</span>
                        </div>
                        @endfor
                        @else
                        <div class="text-xs opacity-40">-</div>
                        @endif
                    </div>
                </div>
            </div>

            <!-- 2: 평가손익 -->
            <div data-rank-panel="2" class="grid grid-cols-1 md:grid-cols-2 gap-4 hidden">
                !{int c2a = 1;}
                <div>
                    <h3 class="text-sm text-base-content/60 mb-2">${topUnrealizedLabel}</h3>
                    <div class="space-y-1">
                        @if(topUnrealized != null && !topUnrealized.isEmpty())
                        @for(TradeProfit item : topUnrealized)
                        <div class="flex justify-between items-center text-xs">
                            <span class="flex items-center gap-2 min-w-0"><span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${c2a++}</span><span class="truncate">${item.stockItemName()}</span></span>
                            <span class="font-mono font-bold text-profit amount-value shrink-0">${String.format("%+,d", item.evaluationProfitNet() != null ? item.evaluationProfitNet().longValue() : 0)}</span>
                        </div>
                        @endfor
                        @else
                        <div class="text-xs opacity-40">-</div>
                        @endif
                    </div>
                </div>
                !{int c2b = 1;}
                <div>
                    <h3 class="text-sm text-base-content/60 mb-2">${bottomUnrealizedLabel}</h3>
                    <div class="space-y-1">
                        @if(bottomUnrealized != null && !bottomUnrealized.isEmpty())
                        @for(TradeProfit item : bottomUnrealized)
                        <div class="flex justify-between items-center text-xs">
                            <span class="flex items-center gap-2 min-w-0"><span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${c2b++}</span><span class="truncate">${item.stockItemName()}</span></span>
                            <span class="font-mono font-bold text-loss amount-value shrink-0">${String.format("%+,d", item.evaluationProfitNet() != null ? item.evaluationProfitNet().longValue() : 0)}</span>
                        </div>
                        @endfor
                        @else
                        <div class="text-xs opacity-40">${noUnrealizedLossLabel}</div>
                        @endif
                    </div>
                </div>
            </div>

            <!-- 3: 배당 -->
            <div data-rank-panel="3" class="grid grid-cols-1 md:grid-cols-2 gap-4 hidden">
                !{int c3a = 1;}
                <div>
                    <h3 class="text-sm text-base-content/60 mb-2">${topDividendLabel}</h3>
                    <div class="space-y-1">
                        @if(topDividend != null && !topDividend.isEmpty())
                        @for(TradeProfit item : topDividend)
                        <div class="flex justify-between items-center text-xs">
                            <span class="flex items-center gap-2 min-w-0"><span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${c3a++}</span><span class="truncate">${item.stockItemName()}</span></span>
                            <span class="font-mono font-bold text-dividend amount-value shrink-0">${String.format("%,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}</span>
                        </div>
                        @endfor
                        @else
                        <div class="text-xs opacity-40">-</div>
                        @endif
                    </div>
                </div>
                !{int c3b = 1;}
                <div>
                    <h3 class="text-sm text-base-content/60 mb-2">${bottomDividendLabel}</h3>
                    <div class="space-y-1">
                        @if(bottomDividend != null && !bottomDividend.isEmpty())
                        @for(TradeProfit item : bottomDividend)
                        <div class="flex justify-between items-center text-xs">
                            <span class="flex items-center gap-2 min-w-0"><span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${c3b++}</span><span class="truncate">${item.stockItemName()}</span></span>
                            <span class="font-mono font-bold text-dividend amount-value shrink-0">${String.format("%,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}</span>
                        </div>
                        @endfor
                        @else
                        <div class="text-xs opacity-40">-</div>
                        @endif
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var root = document.getElementById("summaryRankPanel");
    if (!root || root.dataset.rankInit === "1") return;
    root.dataset.rankInit = "1";
    var tabs = root.querySelectorAll("[data-rank-tab]");
    var panels = root.querySelectorAll("[data-rank-panel]");
    function activate(idx) {
        tabs.forEach(function (t) { t.classList.toggle("tab-active", t.dataset.rankTab === idx); });
        panels.forEach(function (p) { p.classList.toggle("hidden", p.dataset.rankPanel !== idx); });
    }
    root.addEventListener("click", function (e) {
        var t = e.target.closest("[data-rank-tab]");
        if (t) activate(t.dataset.rankTab);
    });
    activate("0");
})();
</script>
