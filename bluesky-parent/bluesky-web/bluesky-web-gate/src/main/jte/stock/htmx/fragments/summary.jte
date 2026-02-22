@import java.math.BigDecimal
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.TradeProfit

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

<div class="flex flex-col gap-4">
    <!-- Stats -->
    <div class="stats stats-vertical lg:stats-horizontal shadow w-full bg-base-100">
        <!-- 총 자산 -->
        <div class="stat">
            <div class="stat-figure text-primary">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            </div>
            <div class="stat-title">총 자산</div>
            <div class="stat-value text-principal">${String.format("%,d", totalAsset.longValue())} 원</div>
            <div class="stat-desc">평가 금액 합계</div>
        </div>

        <!-- 평가 손익 -->
        <div class="stat">
            <div class="stat-title">평가 손익</div>
             <div class="stat-value ${totalUnrealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                 ${String.format("%,d", totalUnrealizedProfit.longValue())}
             </div>
            <div class="stat-desc">현재 보유분</div>
        </div>

        <!-- 실현 손익 -->
        <div class="stat">
            <div class="stat-figure text-secondary">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path></svg>
            </div>
            <div class="stat-title">실현 손익</div>
            <div class="stat-value ${totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                 ${String.format("%,d", totalRealizedProfit.longValue())}
            </div>
            <div class="stat-desc">매도 완료분</div>
        </div>

        <!-- 누적 배당 -->
        <div class="stat">
            <div class="stat-title">누적 배당금</div>
             <div class="stat-value text-dividend">
                 ${String.format("%,d", totalDividend.longValue())}
             </div>
            <div class="stat-desc">전체 기간</div>
        </div>

        <!-- 승률 -->
        <div class="stat hidden 2xl:grid">
            <div class="stat-title">종목 승률</div>
            <div class="stat-value">${String.format("%.1f", winRate)}%</div>
            <div class="stat-desc">수익권 종목 비율</div>
        </div>
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-1 xl:grid-cols-2 gap-4 w-full">
         @template.stock.htmx.fragments.chartsAllocation(allocation = allocation)
         @template.stock.htmx.fragments.chartsAccountAllocation(accountAllocation = accountAllocation)
    </div>
    
    <!-- Top/Bottom Lists (4 Columns) -->
    <div class="mt-4 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        
        <!-- Buying -->
        <div class="space-y-4">
            <!-- Top Buying -->
            @if(topBuying != null && !topBuying.isEmpty())
            <div class="card bg-base-100 shadow-sm compact border border-base-200">
                <div class="card-body p-3">
                    <h3 class="card-title text-sm opacity-70 mb-2">💰 매수 상위</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : topBuying)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold">
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
                    <h3 class="card-title text-sm opacity-70 mb-2">📉 매수 하위</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : bottomBuying)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold">
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
                    <h3 class="card-title text-sm opacity-70 mb-2">🏆 실현수익 상위</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : topRealized)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-profit">
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
                    <h3 class="card-title text-sm opacity-70 mb-2">💔 실현손실 하위</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : bottomRealized)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-loss">
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
                    <h3 class="card-title text-sm opacity-70 mb-2">🚀 평가수익 상위</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : topUnrealized)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-profit">
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
                    <h3 class="card-title text-sm opacity-70 mb-2">⚓ 평가손실 하위</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : bottomUnrealized)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-loss">
                                ${String.format("%+,d", item.evaluationProfitNet() != null ? item.evaluationProfitNet().longValue() : 0)}
                            </span>
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
                    <h3 class="card-title text-sm opacity-70 mb-2">💸 배당 상위</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : topDividend)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-dividend">
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
                    <h3 class="card-title text-sm opacity-70 mb-2">💧 배당 하위</h3>
                    <div class="space-y-1">
                        !{int count = 1;}
                        @for(TradeProfit item : bottomDividend)
                        <div class="flex justify-between items-center text-xs">
                            <div class="flex items-center gap-2">
                                <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center">${count++}</span>
                                <span class="truncate max-w-[80px]">${item.stockItemName()}</span>
                            </div>
                            <span class="font-mono font-bold text-dividend">
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