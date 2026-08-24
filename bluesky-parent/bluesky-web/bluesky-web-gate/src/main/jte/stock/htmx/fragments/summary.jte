@import net.luversof.web.gate.util.CspNonceHolder
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import io.github.luversof.boot.context.support.MessageUtil

@param BigDecimal totalAsset
@param BigDecimal totalUnrealizedProfit
@param BigDecimal totalRealizedProfit
@param BigDecimal totalDividend
@param double winRate
@param long winCount = 0L
@param int winDenominator = 0
@param BigDecimal displayPrincipal
@param BigDecimal displayCurrentEvaluationProfit
@param BigDecimal combinedAdjustmentAmount
@param BigDecimal holdingFeeAdjustment
@param BigDecimal excludedHoldingBuyFee = java.math.BigDecimal.ZERO
@param BigDecimal manualPrincipalAdjustment
@param String trendChartLabelsJs = "[]"
@param String trendChartValuesJs = "[]"
@param int trendPointCount = 0
@param java.time.LocalDate priceBasisDate = null

!{
    String currentTotalAssetLabel = MessageUtil.getMessage("stock.summary.label.current.total.asset");
    String principalLabel = MessageUtil.getMessage("stock.label.principal");
    // 이 값은 투자원금(계좌별 수동 지정이 있으면 그 값) 대비 손익이라 자산 현황의 "원금 대비 손익"과 같다.
    // 예전 라벨은 "현재 평가 손익"이었는데, 자산 현황에서 그 이름은 매수금액 기준의 다른 값을 가리킨다
    // (실측: 871,685,932 vs 861,058,009 - 10,627,923 차이). 같은 이름이 화면마다 다른 수를 뜻하지
    // 않도록 자산 현황과 같은 메시지 키를 쓴다.
    String currentEvaluationProfitLabel = MessageUtil.getMessage("stock.analytics.label.principal.return");
    String returnRateLabel = MessageUtil.getMessage("stock.label.return.rate");
    String cumulativeRealizedIncomeLabel = MessageUtil.getMessage("stock.summary.label.cumulative.realized.income");
    String realizedProfitLabel = MessageUtil.getMessage("stock.profit.realized");
    String cumulativeDividendLabel = MessageUtil.getMessage("stock.summary.label.cumulative.dividend");
    String totalCombinedProfitLabel = MessageUtil.getMessage("stock.summary.label.total.combined.profit");
    String calculationDetailsLabel = MessageUtil.getMessage("stock.summary.label.calculation.details");
    String combinedAdjustmentLabel = MessageUtil.getMessage("stock.summary.label.combined.adjustment");
    String holdingFeeAdjustmentLabel = MessageUtil.getMessage("stock.summary.label.holding.fee.adjustment");
    String excludedHoldingBuyFeeLabel = MessageUtil.getMessage("stock.summary.label.excluded.holding.buy.fee");
    String manualPrincipalAdjustmentLabel = MessageUtil.getMessage("stock.summary.label.manual.principal.adjustment");
    String combinedAdjustmentAddedLabel = MessageUtil.getMessage("stock.summary.label.combined.adjustment.added");
    String combinedAdjustmentSubtractedLabel = MessageUtil.getMessage("stock.summary.label.combined.adjustment.subtracted");
    String combinedRateBasisLabel = MessageUtil.getMessage("stock.summary.label.combined.rate.basis");
    String winRateLabel = MessageUtil.getMessage("stock.summary.label.win.rate");
    String profitableStockRatioLabel = MessageUtil.getMessage("stock.summary.label.profitable.stock.ratio");
    String trendChartTitleLabel = MessageUtil.getMessage("stock.summary.trend.title");
    String trendChartCaptionLabel = MessageUtil.getMessage("stock.summary.trend.caption");
    String assetGrowthMenuLabel = MessageUtil.getMessage("layout.menu.stock.assetGrowth");
    // 이 비율이 무엇을 세는지 밝힌다 - 분자/분모와 손익 기준(실현+평가+배당)을 함께 보여준다.
    String winRateBasisText = java.text.MessageFormat.format(
        MessageUtil.getMessage("stock.summary.win.rate.basis.tooltip"), winCount, winDenominator);
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
<%-- 이 수익률의 분모는 화면에 적힌 '투자원금'(수동 설정 원금 반영)이 아니라 취득원가다.
     그대로 두면 화면 숫자로 나눠도 값이 안 나온다(실측: 1,148,009,706 / 621,595,902 = 184.69% 인데 표시는 181.58%).
     기준 원금을 계산 내역과 툴팁에 밝혀 화면에서 재현할 수 있게 한다. --%>
!{String combinedRateBasisText = java.text.MessageFormat.format(MessageUtil.getMessage("stock.summary.combined.rate.basis.tooltip"), String.format("%,d", StockFormatUtil.displayWon(totalCombinedProfit)), String.format("%,d", StockFormatUtil.displayWon(combinedPrincipal)), String.format("%+.2f", combinedRate));}
!{String evalColor = currentEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";}
!{String combinedColor = totalCombinedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";}
!{String realizedColor = totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";}
<%-- 1개 포인트는 선을 그릴 수 없으므로 2개 이상일 때만 트렌드 차트를 렌더 (미만이면 기존 3열 레이아웃 유지) --%>
!{boolean hasTrendSeries = trendPointCount >= 2;}

<div class="flex flex-col gap-4">

    <!-- 히어로: 총자산(주 지표) 카드 하나 + 자산 추이 미니 차트 -->
    <div class="grid grid-cols-1 ${hasTrendSeries ? "lg:grid-cols-2" : ""} gap-3">
        @template._components.ui.card(bodyClass = "p-5", body = @`
            <div class="flex items-center gap-1 text-sm text-base-content/60">
                <span>${currentTotalAssetLabel}</span>
                <span class="tooltip tooltip-bottom" data-tip="${MessageUtil.getMessage("stock.summary.tooltip.total.asset")}">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current opacity-60"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                </span>
            </div>
            <div class="text-3xl lg:text-4xl font-bold text-principal amount-value mt-1" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(totalAsset))}">${StockFormatUtil.compactKrw(StockFormatUtil.displayWon(totalAsset))}</div>
            <div class="text-sm text-base-content/70 mt-2">
                ${principalLabel} <span class="font-mono amount-value" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(currentPrincipal))}">${StockFormatUtil.compactKrw(StockFormatUtil.displayWon(currentPrincipal))}</span>
            </div>
            <%-- 총자산은 마지막으로 수집된 종가로 계산된다. 이 앱은 시세를 자동 수집하지 않아(스케줄러 없음,
                 관리 화면에서 수동 실행) 며칠 전 값일 수 있는데, 요약 화면만 그 사실을 밝히지 않고 있었다
                 (실측 2026-08-22 토요일: 거래일인 08-21 이 빠진 채 08-20 종가로 표시). 자산 현황·포트폴리오·
                 종목 상세와 같은 문구를 쓴다. --%>
            @if(priceBasisDate != null)
                <div class="text-xs text-base-content/60 mt-1">${MessageUtil.getMessage("stock.asset.status.price.basis").replace("{0}", priceBasisDate.toString())}</div>
            @endif
            <%-- 통합손익은 총자산의 보조 지표로 같은 카드 하단에 배치한다 --%>
            <div class="flex items-center justify-between border-t border-base-200 mt-4 pt-3">
                <div class="flex items-center gap-1 text-sm text-base-content/60">
                    <span>${totalCombinedProfitLabel}</span>
                    <%-- 시작점 정렬은 트리거 위치에 따라 오른쪽으로 밀려 좁은 화면에서 화면 밖으로 나간다
                         (실측: 영어 라벨이 길어 375px 에서 14px 넘침). 중앙 정렬로 둔다. --%>
                    <div class="tooltip tooltip-bottom shrink-0">
                        <div class="tooltip-content z-[20] w-64 sm:w-72 lg:w-80 max-w-[calc(100vw-2rem)] rounded-box bg-neutral text-neutral-content shadow-lg">
                            <div class="space-y-1 text-left text-xs leading-5">
                                <div class="font-semibold mb-1">${calculationDetailsLabel}</div>
                                <div class="flex items-center justify-between gap-3">
                                    <span>${currentEvaluationProfitLabel}</span>
                                    <span class="amount-value font-medium">${String.format("%,d", StockFormatUtil.displayWon(currentEvaluationProfit))}</span>
                                </div>
                                <div class="flex items-center justify-between gap-3">
                                    <span>+ ${cumulativeRealizedIncomeLabel}</span>
                                    <span class="amount-value font-medium">${String.format("%,d", StockFormatUtil.displayWon(totalRealizedIncome))}</span>
                                </div>
                                @if(hasCombinedAdjustment)
                                <div class="flex items-center justify-between gap-3">
                                    <span>${combinedAdjustmentOperator} ${combinedAdjustmentLabel}</span>
                                    <span class="amount-value font-medium">${String.format("%,d", StockFormatUtil.displayWon(combinedAdjustmentAbs))}</span>
                                </div>
                                <div class="border-t border-neutral-content/20 pt-1 text-neutral-content/80">
                                    ${combinedAdjustmentMeaning}
                                </div>
                                @endif
                                @if(holdingFeeAdjustment != null && holdingFeeAdjustment.compareTo(BigDecimal.ZERO) != 0)
                                <div class="flex items-center justify-between gap-3 text-neutral-content/80">
                                    <span>${holdingFeeAdjustmentLabel}</span>
                                    <span class="amount-value">${String.format("%+,d", StockFormatUtil.displayWon(holdingFeeAdjustment))}</span>
                                </div>
                                @endif
                                @if(manualPrincipalAdjustment != null && manualPrincipalAdjustment.compareTo(BigDecimal.ZERO) != 0)
                                <div class="flex items-center justify-between gap-3 text-neutral-content/80">
                                    <span>${manualPrincipalAdjustmentLabel}</span>
                                    <span class="amount-value">${String.format("%+,d", StockFormatUtil.displayWon(manualPrincipalAdjustment))}</span>
                                </div>
                                @endif
                                <div class="flex items-center justify-between gap-3 border-t border-neutral-content/20 pt-1 font-semibold">
                                    <span>= ${totalCombinedProfitLabel}</span>
                                    <span class="amount-value">${String.format("%,d", StockFormatUtil.displayWon(totalCombinedProfit))}</span>
                                </div>
                                @if(combinedPrincipal.compareTo(BigDecimal.ZERO) > 0)
                                <div class="flex items-center justify-between gap-3 text-neutral-content/80">
                                    <span>${combinedRateBasisLabel}</span>
                                    <span class="amount-value">${String.format("%,d", StockFormatUtil.displayWon(combinedPrincipal))}</span>
                                </div>
                                @endif
                                <%-- 표시되는 평가손익은 매수 수수료를 원가로 보지 않는 기준이다(거래 행이 증권사 기록값이라
                                     그 기준으로 통일했다). 그 금액을 적지 않으면 수수료가 이미 빠진 값으로 오해한다. --%>
                                @if(excludedHoldingBuyFee != null && excludedHoldingBuyFee.compareTo(BigDecimal.ZERO) != 0)
                                <div class="flex items-center justify-between gap-3 text-neutral-content/80">
                                    <span>${excludedHoldingBuyFeeLabel}</span>
                                    <span class="amount-value">${String.format("%,d", StockFormatUtil.displayWon(excludedHoldingBuyFee))}</span>
                                </div>
                                @endif
                            </div>
                        </div>
                        <button type="button" class="btn btn-circle btn-ghost btn-xs text-info align-middle" aria-label="${calculationDetailsLabel}">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        </button>
                    </div>
                </div>
                <div class="text-right">
                    <span class="text-xl font-bold amount-value ${combinedColor}" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(totalCombinedProfit))}">${StockFormatUtil.compactKrw(StockFormatUtil.displayWon(totalCombinedProfit))}</span>
                    <span class="ml-2 text-sm font-semibold ${combinedColor}" title="${combinedRateBasisText}">${String.format("%+.2f%%", combinedRate)}</span>
                </div>
            </div>
        `)

        @if(hasTrendSeries)
        @template._components.ui.card(cssClass = "h-full", bodyClass = "p-4 h-full gap-2", body = @`
            <div class="flex items-baseline justify-between gap-2">
                <h2 class="text-sm font-semibold text-base-content/70">${trendChartTitleLabel} <span class="font-normal text-xs text-base-content/60">${trendChartCaptionLabel}</span></h2>
                <a href="/stock/asset-growth" class="link link-hover text-xs shrink-0 tap-target-24">${assetGrowthMenuLabel} →</a>
            </div>
            <div class="relative w-full flex-1" style="min-height:160px">
                <canvas id="dashboardTrendChart" class="amount-chart"></canvas>
            </div>
        `)
        @endif
    </div>

    <!-- 보조 지표 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
    <%-- 요약 지표는 히어로와 같은 축약 표기(억/만)로 통일 — 풀 숫자는 title 툴팁.
         각 카드는 해당 상세 화면으로 이동한다 (drill-down) --%>
        @template._components.ui.statCard(
            label = currentEvaluationProfitLabel,
            value = StockFormatUtil.compactKrw(currentEvaluationProfit.longValue()),
            valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(currentEvaluationProfit)),
            sub = returnRateLabel + " " + String.format("%+.2f%%", unrealizedRate),
            tone = evalColor,
            href = "/stock/analytics")
        @template._components.ui.statCard(
            label = realizedProfitLabel,
            value = StockFormatUtil.compactKrw(totalRealizedProfit.longValue()),
            valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(totalRealizedProfit)),
            tone = realizedColor,
            href = "/stock/trade")
        @template._components.ui.statCard(
            label = cumulativeDividendLabel,
            value = StockFormatUtil.compactKrw(totalDividend.longValue()),
            valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(totalDividend)),
            tone = "text-dividend",
            href = "/stock/dividend")
        @template._components.ui.statCard(
            label = winRateLabel,
            value = String.format("%.1f", winRate) + "%",
            valueTitle = winRateBasisText,
            sub = profitableStockRatioLabel,
            amount = false,
            href = "/stock/analytics")
    </div>
</div>

@if(hasTrendSeries)
<script nonce="${CspNonceHolder.getNonce()}">
(function () {
    // 자산 추이 미니 차트 — Chart.js 는 stockLayout 에서 1회 로드됨.
    // htmx 스왑 정착 후 실행(setTimeout 0): 스왑 중엔 이전/새 canvas 가 공존할 수 있다(activityList.jte 와 동일).
    setTimeout(function () {
        var trendCanvas = document.getElementById('dashboardTrendChart');
        if (!trendCanvas) return;
        window.ensureStockCharts(function () {
        if (typeof Chart === 'undefined') return;
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
                // 차트 클릭 시 자산 성장 상세로 이동 (drill-down)
                onClick: function () { window.location.href = '/stock/asset-growth'; },
                onHover: function (evt, elements, chart) { chart.canvas.style.cursor = 'pointer'; },
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
                                // 로케일과 무관하게 억/만 을 붙이면 영어 화면 축에도 한글 단위가 나온다(실측).
                                var ko = (document.documentElement.lang || '').toLowerCase().indexOf('ko') === 0;
                                var trim = function (x) { var t = x.toFixed(1); return t.slice(-2) === '.0' ? t.slice(0, -2) : t; };
                                if (ko) {
                                    if (v >= 100000000) return trim(v / 100000000) + '억';
                                    if (v >= 10000) return (v / 10000).toFixed(0) + '만';
                                    return Number(v).toLocaleString();
                                }
                                if (v >= 1000000000) return trim(v / 1000000000) + 'B';
                                if (v >= 1000000) return trim(v / 1000000) + 'M';
                                if (v >= 1000) return trim(v / 1000) + 'K';
                                return Number(v).toLocaleString();
                            }
                        }
                    }
                }
            }
        });
        });
    }, 0);
})();
</script>
@endif
