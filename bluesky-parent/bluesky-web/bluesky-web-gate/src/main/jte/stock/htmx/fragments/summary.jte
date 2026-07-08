@import java.math.BigDecimal
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import io.github.luversof.boot.context.support.MessageUtil

@param BigDecimal totalAsset
@param BigDecimal totalUnrealizedProfit
@param BigDecimal totalRealizedProfit
@param BigDecimal totalDividend
@param double winRate
@param BigDecimal displayPrincipal
@param BigDecimal displayCurrentEvaluationProfit
@param BigDecimal combinedAdjustmentAmount
@param BigDecimal holdingFeeAdjustment
@param BigDecimal manualPrincipalAdjustment
@param String trendChartLabelsJs = "[]"
@param String trendChartValuesJs = "[]"
@param int trendPointCount = 0

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
<%-- 1개 포인트는 선을 그릴 수 없으므로 2개 이상일 때만 트렌드 차트를 렌더 (미만이면 기존 3열 레이아웃 유지) --%>
!{boolean hasTrendSeries = trendPointCount >= 2;}

<div class="flex flex-col gap-4">

    <!-- 히어로: 총자산/통합손익 카드 + 자산 추이 미니 차트(최근 6개월) -->
    <div class="grid grid-cols-1 ${hasTrendSeries ? "lg:grid-cols-2" : "lg:grid-cols-3"} gap-3">
        <%-- 시계열이 없으면 display:contents 로 기존 3열 배치(총자산 col-span-2 + 통합손익)를 유지한다 --%>
        <div class="${hasTrendSeries ? "grid grid-cols-1 gap-3 content-start" : "contents"}">
        @template._components.ui.card(cssClass = hasTrendSeries ? "" : "lg:col-span-2", bodyClass = "p-5", body = @`
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
        `)

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
                        <button type="button" class="btn btn-circle btn-ghost btn-xs text-info align-middle" aria-label="${calculationDetailsLabel}">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        </button>
                    </div>
                </div>
                <div class="text-2xl lg:text-3xl font-bold amount-value mt-1 ${combinedColor}" title="${String.format("%,d원", totalCombinedProfit.longValue())}">${StockFormatUtil.compactKrw(totalCombinedProfit.longValue())}</div>
                <div class="text-sm font-semibold mt-1 ${combinedColor}">${returnRateLabel} ${String.format("%+.2f%%", combinedRate)}</div>
            </div>
        </div>
        </div>

        @if(hasTrendSeries)
        @template._components.ui.card(cssClass = "h-full", bodyClass = "p-4 h-full", body = @`
            <div class="relative w-full h-full" style="min-height:160px">
                <canvas id="dashboardTrendChart" class="amount-chart"></canvas>
            </div>
        `)
        @endif
    </div>

    <!-- 보조 지표 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
        @template._components.ui.statCard(
            label = currentEvaluationProfitLabel,
            value = String.format("%,d", currentEvaluationProfit.longValue()),
            valueTitle = String.format("%,d원", currentEvaluationProfit.longValue()),
            sub = returnRateLabel + " " + String.format("%+.2f%%", unrealizedRate),
            tone = evalColor)
        @template._components.ui.statCard(
            label = realizedProfitLabel,
            value = String.format("%,d", totalRealizedProfit.longValue()),
            valueTitle = String.format("%,d원", totalRealizedProfit.longValue()),
            tone = realizedColor)
        @template._components.ui.statCard(
            label = cumulativeDividendLabel,
            value = String.format("%,d", totalDividend.longValue()),
            valueTitle = String.format("%,d원", totalDividend.longValue()),
            tone = "text-dividend")
        @template._components.ui.statCard(
            label = winRateLabel,
            value = String.format("%.1f", winRate) + "%",
            sub = profitableStockRatioLabel,
            amount = false)
    </div>
</div>

@if(hasTrendSeries)
<script>
(function () {
    // 자산 추이 미니 차트 — Chart.js 는 stockLayout 에서 1회 로드됨.
    // htmx 스왑 정착 후 실행(setTimeout 0): 스왑 중엔 이전/새 canvas 가 공존할 수 있다(activityList.jte 와 동일).
    setTimeout(function () {
        var trendCanvas = document.getElementById('dashboardTrendChart');
        if (!trendCanvas || typeof Chart === 'undefined') return;
        var trendLabels = $unsafe{trendChartLabelsJs};
        var trendValues = $unsafe{trendChartValuesJs};
        if (!trendLabels.length) return;
        if (window.dashboardTrendChartInst) {
            try { window.dashboardTrendChartInst.destroy(); } catch (e) {}
            window.dashboardTrendChartInst = null;
        }
        window.dashboardTrendChartInst = new Chart(trendCanvas, {
            type: 'line',
            data: {
                labels: trendLabels,
                datasets: [{
                    data: trendValues,
                    borderColor: 'rgba(59,130,246,0.9)',
                    backgroundColor: 'rgba(59,130,246,0.12)',
                    fill: true,
                    borderWidth: 2,
                    pointRadius: 0,
                    tension: 0.3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) { return '₩' + (Number(ctx.parsed.y) || 0).toLocaleString(); }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { size: 10 }, maxTicksLimit: 5, maxRotation: 0, autoSkip: true }
                    },
                    y: {
                        grid: { color: 'rgba(128,128,128,0.1)' },
                        ticks: {
                            font: { size: 10 },
                            maxTicksLimit: 4,
                            callback: function (v) {
                                if (v >= 100000000) return (v / 100000000).toFixed(1).replace('.0', '') + '억';
                                if (v >= 10000) return (v / 10000).toFixed(0) + '만';
                                return Number(v).toLocaleString();
                            }
                        }
                    }
                }
            }
        });
    }, 0);
})();
</script>
@endif
