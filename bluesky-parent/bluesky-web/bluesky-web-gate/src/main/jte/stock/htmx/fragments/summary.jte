@import java.math.BigDecimal
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.TradeProfit
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
@param Map<String, BigDecimal> allocation
@param Map<String, BigDecimal> accountAllocation
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
}

<div class="flex flex-col gap-4">
    <!-- Stats -->
    <div class="stats stats-vertical lg:stats-horizontal shadow w-full bg-base-100 overflow-visible">
        
        <!-- 1. 현재 자산 상태 (원금 + 평가손익) -->
        !{BigDecimal currentPrincipal = displayPrincipal != null ? displayPrincipal : totalAsset.subtract(totalUnrealizedProfit);}
        !{BigDecimal currentEvaluationProfit = displayCurrentEvaluationProfit != null ? displayCurrentEvaluationProfit : totalUnrealizedProfit;}
        !{double unrealizedRate = currentPrincipal.compareTo(BigDecimal.ZERO) > 0 ? currentEvaluationProfit.doubleValue() / currentPrincipal.doubleValue() * 100 : 0.0;}
        <div class="stat">
            <div class="stat-figure text-primary">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            </div>
            <div class="stat-title">${currentTotalAssetLabel}</div>
            <div class="stat-value text-principal font-bold amount-value">${String.format("%,d", totalAsset.longValue())}</div>
            <div class="stat-desc font-semibold mt-1">
                ${principalLabel}: <span class="amount-value">${String.format("%,d", currentPrincipal.longValue())}</span>
            </div>
        </div>

        <div class="stat">
            <div class="stat-title">${currentEvaluationProfitLabel}</div>
             <div class="stat-value font-bold amount-value ${currentEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                 ${String.format("%,d", currentEvaluationProfit.longValue())}
             </div>
            <div class="stat-desc font-bold mt-1 ${currentEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                ${returnRateLabel}: <span>${String.format("%+.2f%%", unrealizedRate)}</span>
            </div>
        </div>

        <!-- 2. 확정 수익 (실현손익 + 배당금) -->
        !{BigDecimal totalRealizedIncome = totalRealizedProfit.add(totalDividend);}
        <div class="stat border-l border-base-200">
            <div class="stat-figure text-secondary">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path></svg>
            </div>
            <div class="stat-title">${cumulativeRealizedIncomeLabel}</div>
            <div class="stat-value font-bold amount-value ${totalRealizedIncome.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                 ${String.format("%,d", totalRealizedIncome.longValue())}
            </div>
            <div class="stat-desc mt-1">
                ${realizedProfitLabel}: <span class="amount-value">${String.format("%,d", totalRealizedProfit.longValue())}</span>
            </div>
            <div class="stat-desc">
                ${cumulativeDividendLabel}: <span class="amount-value">${String.format("%,d", totalDividend.longValue())}</span>
            </div>
        </div>

        <!-- 3. 총 합산 수익 (평가손익 + 확정수익) -->
        !{BigDecimal totalCombinedProfit = currentEvaluationProfit.add(totalRealizedIncome).subtract(combinedAdjustmentAmount);}
        !{BigDecimal combinedPrincipal = totalAsset.subtract(totalUnrealizedProfit);}
        !{boolean hasCombinedAdjustment = combinedAdjustmentAmount != null && combinedAdjustmentAmount.compareTo(BigDecimal.ZERO) != 0;}
        !{BigDecimal combinedAdjustmentAbs = combinedAdjustmentAmount != null ? combinedAdjustmentAmount.abs() : BigDecimal.ZERO;}
        !{String combinedAdjustmentOperator = combinedAdjustmentAmount != null && combinedAdjustmentAmount.compareTo(BigDecimal.ZERO) < 0 ? "+" : "-";}
        !{String combinedAdjustmentMeaning = hasCombinedAdjustment ? (combinedAdjustmentAmount.compareTo(BigDecimal.ZERO) < 0 ? combinedAdjustmentAddedLabel : combinedAdjustmentSubtractedLabel) : "";}
        !{double combinedRate = combinedPrincipal.compareTo(BigDecimal.ZERO) > 0 ? totalCombinedProfit.doubleValue() / combinedPrincipal.doubleValue() * 100 : 0.0;}
        <div class="stat border-l border-base-200">
            <div class="stat-figure text-accent">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>
            </div>
            <div class="stat-title flex items-center gap-1">
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
                    <button type="button" class="btn btn-circle btn-ghost btn-xs text-info">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    </button>
                </div>
            </div>
            <div class="stat-value font-bold amount-value ${totalCombinedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                 ${String.format("%,d", totalCombinedProfit.longValue())}
            </div>
            <div class="stat-desc font-bold mt-2 ${totalCombinedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                ${returnRateLabel}: <span>${String.format("%+.2f%%", combinedRate)}</span>
            </div>
        </div>

        <!-- 승률 -->
        <div class="stat hidden 2xl:grid border-l border-base-200">
            <div class="stat-title">${winRateLabel}</div>
            <div class="stat-value font-bold">${String.format("%.1f", winRate)}%</div>
            <div class="stat-desc mt-1">${profitableStockRatioLabel}</div>
        </div>
    </div>

    <!-- Charts Row -->
    @if(showCharts)
    <div class="grid grid-cols-1 xl:grid-cols-2 gap-4 w-full">
         @template.stock.htmx.fragments.components.chartsAllocation(allocation = allocation)
         @template.stock.htmx.fragments.components.chartsAccountAllocation(accountAllocation = accountAllocation)
    </div>
    @endif
    
    <!-- Top/Bottom Lists (4 Columns) -->
    <div class="mt-4 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        
        <!-- Buying -->
        <div class="space-y-4">
            <!-- Top Buying -->
            @if(topBuying != null && !topBuying.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">💰 ${topBuyingLabel}</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : topBuying)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold amount-value">
                                ${String.format("%,d", item.totalBuyCost() != null ? item.totalBuyCost().longValue() : 0)}
                            </span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @endif
            <!-- Bottom Buying -->
            @if(bottomBuying != null && !bottomBuying.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">📉 ${bottomBuyingLabel}</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : bottomBuying)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold amount-value">
                                ${String.format("%,d", item.totalBuyCost() != null ? item.totalBuyCost().longValue() : 0)}
                            </span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @endif
        </div>

        <!-- Realized -->
        <div class="space-y-4">
            <!-- Top Realized -->
            @if(topRealized != null && !topRealized.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">🏆 ${topRealizedLabel}</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : topRealized)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-profit amount-value">
                                ${String.format("%+,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}
                            </span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @endif
            <!-- Bottom Realized -->
            @if(bottomRealized != null && !bottomRealized.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">💔 ${bottomRealizedLabel}</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : bottomRealized)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-loss amount-value">
                                ${String.format("%+,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}
                            </span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @endif
        </div>

        <!-- Unrealized -->
        <div class="space-y-4">
            <!-- Top Unrealized -->
            @if(topUnrealized != null && !topUnrealized.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">🚀 ${topUnrealizedLabel}</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : topUnrealized)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-profit amount-value">
                                ${String.format("%+,d", item.evaluationProfitNet() != null ? item.evaluationProfitNet().longValue() : 0)}
                            </span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @endif
            <!-- Bottom Unrealized -->
            @if(bottomUnrealized != null && !bottomUnrealized.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">⚓ ${bottomUnrealizedLabel}</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : bottomUnrealized)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-loss amount-value">
                                ${String.format("%+,d", item.evaluationProfitNet() != null ? item.evaluationProfitNet().longValue() : 0)}
                            </span>
                        </div>
                        @endfor
                        @for(int i = bottomUnrealized.size(); i < 4; i++)
                        <div class="flex justify-between items-center text-xs invisible">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0">-</span>
                                <span>-</span>
                            </div>
                            <span>-</span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @else
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">⚓ ${bottomUnrealizedLabel}</h3>
                    <div class="space-y-1">
                        <div class="flex justify-between items-center text-xs opacity-40">
                            <span>${noUnrealizedLossLabel}</span>
                        </div>
                        @for(int i = 1; i < 4; i++)
                        <div class="flex justify-between items-center text-xs invisible">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0">-</span>
                                <span>-</span>
                            </div>
                            <span>-</span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @endif
        </div>

        <!-- Dividend -->
        <div class="space-y-4">
            <!-- Top Dividend -->
            @if(topDividend != null && !topDividend.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">💸 ${topDividendLabel}</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : topDividend)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-dividend amount-value">
                                ${String.format("%,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}
                            </span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @endif
            <!-- Bottom Dividend -->
            @if(bottomDividend != null && !bottomDividend.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">💧 ${bottomDividendLabel}</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : bottomDividend)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-dividend amount-value">
                                ${String.format("%,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}
                            </span>
                        </div>
                        @endfor
                    </div>
                </div>
            </div>
            @endif
        </div>
    </div>
</div>