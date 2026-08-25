@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.text.DecimalFormat

@param String fromDate = ""
@param String toDate = ""
@param Double periodReturnRatePct = null
@param boolean returnCalculable = false
@param Double timeWeightedReturnPct = null
@param BigDecimal periodProfit = null
@param BigDecimal principalDelta = null
@param BigDecimal unrealizedStart = null
@param BigDecimal unrealizedEnd = null
@param Double unrealizedEndPct = null
@param BigDecimal recoveredAmount = null
@param BigDecimal netNewProfit = null
@param Double maxDrawdownPct = null
@param java.time.LocalDate maxDrawdownPeakDate = null
@param java.time.LocalDate maxDrawdownTroughDate = null
@param Double currentDrawdownPct = null
@param BigDecimal openingValue = null
@param BigDecimal closingValue = null

!{
    DecimalFormat amountFormat = new DecimalFormat("#,##0");
    String unrealizedLabel = MessageUtil.getMessage("stock.profit.unrealized");
    String growthTitle = MessageUtil.getMessage("stock.asset.growth.summary.period.growth");
    String growthDescription = MessageUtil.getMessage("stock.asset.growth.summary.period.growth.desc");
    String unavailableLabel = MessageUtil.getMessage("stock.asset.growth.summary.period.return.unavailable");
    String twrTitle = MessageUtil.getMessage("stock.asset.growth.summary.twr");
    String twrDescription = MessageUtil.getMessage("stock.asset.growth.summary.twr.desc");
    String profitTitle = MessageUtil.getMessage("stock.asset.growth.summary.period.profit");
    String profitDescription = MessageUtil.getMessage("stock.asset.growth.summary.period.profit.desc");
    String principalDeltaLabel = MessageUtil.getMessage("stock.asset.growth.summary.principal.delta");
    String unrealizedTrendLabel = MessageUtil.getMessage("stock.asset.growth.summary.unrealized.trend");
    String unrealizedDescription = MessageUtil.getMessage("stock.asset.growth.summary.unrealized.desc");
    String recoveredLabel = MessageUtil.getMessage("stock.asset.growth.summary.profit.recovered");
    String netNewLabel = MessageUtil.getMessage("stock.asset.growth.summary.profit.net.new");

    boolean growthPositive = periodReturnRatePct != null && periodReturnRatePct >= 0.0d;
    boolean twrAvailable = timeWeightedReturnPct != null;
    boolean twrPositive = twrAvailable && timeWeightedReturnPct >= 0.0d;
    boolean profitAvailable = periodProfit != null;
    boolean profitPositive = profitAvailable && periodProfit.signum() >= 0;
    boolean principalDeltaAvailable = principalDelta != null && principalDelta.signum() != 0;
    boolean principalIncreased = principalDeltaAvailable && principalDelta.signum() > 0;

    boolean unrealizedAvailable = unrealizedStart != null && unrealizedEnd != null;
    boolean unrealizedEndPositive = unrealizedAvailable && unrealizedEnd.signum() >= 0;
    boolean unrealizedStartPositive = unrealizedAvailable && unrealizedStart.signum() >= 0;
    // 손실 회복분이 있을 때만 분해를 보여준다(처음부터 플러스였으면 전액이 순증이라 의미 없음)
    boolean splitAvailable = recoveredAmount != null && netNewProfit != null
        && recoveredAmount.signum() > 0;
    boolean netNewPositive = netNewProfit != null && netNewProfit.signum() >= 0;
    String maxDrawdownLabel = MessageUtil.getMessage("stock.asset.growth.summary.max.drawdown");
    String growthAmountLabel = MessageUtil.getMessage("stock.asset.growth.summary.growth.amount");
    String growthBaseTooSmallLabel = MessageUtil.getMessage("stock.asset.growth.summary.growth.base.too.small");
    // 비율이 없을 때(기초가 무의미할 때) 금액으로 대체 표기
    boolean amountFallbackAvailable = openingValue != null && closingValue != null;
    boolean drawdownAvailable = maxDrawdownPct != null;
    boolean drawdownRangeAvailable = maxDrawdownPeakDate != null && maxDrawdownTroughDate != null;
    // 전고점 대비 현재 위치. 0 에 가까우면 신고점이라는 뜻이라 별도 문구로 알려준다.
    String currentDrawdownLabel = MessageUtil.getMessage("stock.asset.growth.summary.current.drawdown");
    String atPeakLabel = MessageUtil.getMessage("stock.asset.growth.summary.at.peak");
    boolean currentDrawdownAvailable = currentDrawdownPct != null;
    boolean atPeak = currentDrawdownAvailable && currentDrawdownPct > -0.01d;
}

<div id="assetGrowthPeriodReturnSummary" class="rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3 mb-3">

    <%-- 주지표: 기간 손익 — 이 화면은 기간을 고르는 화면이므로, 고른 기간에 답하는 값이 맨 위에 와야 한다.
         예전에는 평가손익이 주지표였는데 그 값은 <b>기간이 끝나는 날</b>만 따라간다. 화면의 프리셋
         (이번달·올해·1년·3년·전체)은 끝나는 날이 모두 오늘이라, 무엇을 골라도 큰 숫자가 꿈쩍하지 않았다
         (실측 2026-08-24: 다섯 기간 모두 +915,129,989). 정작 기간 손익은 -18,659,731 ~ +1,202,320,825
         로 갈렸고, 이번달은 손실인데 맨 위에는 +9억이 떠 있었다. 둘의 자리를 바꾼다. --%>
    <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
        <span class="text-sm font-semibold text-base-content/70">${profitTitle}</span>
        @if(!fromDate.isBlank() && !toDate.isBlank())
            <span class="inline-flex items-center rounded-full bg-base-200/80 px-2 py-0.5 text-[11px] font-medium text-base-content/60">
                ${fromDate} ~ ${toDate}
            </span>
        @endif
    </div>
    <div class="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
        @if(profitAvailable)
            <span class="text-4xl font-bold leading-none amount-value ${profitPositive ? "text-profit" : "text-loss"}">
                ${profitPositive ? "+" : "-"}₩${amountFormat.format(periodProfit.abs())}
            </span>
        @else
            <span class="text-lg font-semibold text-base-content/60">${unavailableLabel}</span>
        @endif
    </div>
    <p class="text-xs text-base-content/60 mt-1">${profitDescription}</p>
    @if(splitAvailable)
        <%-- 기간 손익 = 손실 회복분 + 순증분. 회복분은 되찾은 돈, 순증분이 새로 번 돈. --%>
        <p class="text-xs mt-0.5">
            <span class="text-base-content/60">${recoveredLabel}</span>
            <span class="amount-value font-medium text-base-content/60">₩${amountFormat.format(recoveredAmount)}</span>
            <span class="text-base-content/30 mx-0.5">·</span>
            <span class="text-base-content/60">${netNewLabel}</span>
            <span class="amount-value font-semibold ${netNewPositive ? "text-profit" : "text-loss"}">${netNewPositive ? "+" : "-"}₩${amountFormat.format(netNewProfit.abs())}</span>
        </p>
    @endif

    <%-- 보조 지표: 운용 성과 / 평가 손익 / 자산 증가율 --%>
    <div class="mt-3 pt-3 border-t border-base-200 grid gap-3 sm:grid-cols-3">
        <div class="min-w-0">
            <div class="text-xs text-base-content/60 mb-0.5">${twrTitle}</div>
            @if(twrAvailable)
                <div class="text-lg font-bold leading-none ${twrPositive ? "text-profit" : "text-loss"}">
                    ${String.format("%+.2f%%", timeWeightedReturnPct)}
                </div>
            @else
                <div class="text-sm font-semibold text-base-content/60">${unavailableLabel}</div>
            @endif
            <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">${twrDescription}</p>
            @if(drawdownAvailable)
                <%-- 수익률의 짝이 되는 위험 지표. 입출금을 제거한 기준가로 계산해 입금에 희석되지 않는다. --%>
                <p class="text-[11px] leading-4 mt-0.5">
                    <span class="text-base-content/60">${maxDrawdownLabel}</span>
                    <span class="font-semibold text-loss">${String.format("%.2f%%", maxDrawdownPct)}</span>
                    @if(drawdownRangeAvailable)
                        <span class="text-base-content/35">(${maxDrawdownPeakDate.toString()} → ${maxDrawdownTroughDate.toString()})</span>
                    @endif
                </p>
            @endif
            @if(currentDrawdownAvailable)
                <%-- 지금 전고점 대비 어디쯤인지. 최대 낙폭(과거)과 달리 현재 상태를 말해준다. --%>
                <p class="text-[11px] leading-4 mt-0.5">
                    <span class="text-base-content/60">${currentDrawdownLabel}</span>
                    @if(atPeak)
                        <span class="font-semibold text-profit">${atPeakLabel}</span>
                    @else
                        <span class="font-semibold text-loss">${String.format("%.2f%%", currentDrawdownPct)}</span>
                    @endif
                </p>
            @endif
        </div>

        <div class="min-w-0">
            <div class="text-xs text-base-content/60 mb-0.5">${unrealizedLabel}</div>
            @if(unrealizedAvailable)
                <div class="flex flex-wrap items-baseline gap-x-2">
                    <span class="text-lg font-bold leading-none amount-value ${unrealizedEndPositive ? "text-profit" : "text-loss"}">
                        ${unrealizedEndPositive ? "+" : "-"}₩${amountFormat.format(unrealizedEnd.abs())}
                    </span>
                    @if(unrealizedEndPct != null)
                        <span class="text-sm font-semibold ${unrealizedEndPositive ? "text-profit" : "text-loss"}">
                            ${String.format("%+.2f%%", unrealizedEndPct)}
                        </span>
                    @endif
                </div>
            @else
                <div class="text-sm font-semibold text-base-content/60">${unavailableLabel}</div>
            @endif
            <%-- 자리를 내린 값이므로 무엇을 재는 값인지 밝힌다 - 기간이 아니라 기말 시점 값이다. --%>
            <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">${unrealizedDescription}</p>
            @if(unrealizedAvailable)
                <p class="text-[11px] leading-4 mt-0.5">
                    <span class="text-base-content/60">${unrealizedTrendLabel}</span>
                    <span class="amount-value font-medium ${unrealizedStartPositive ? "text-profit" : "text-loss"}">${unrealizedStartPositive ? "+" : "-"}₩${amountFormat.format(unrealizedStart.abs())}</span>
                    <span class="text-base-content/35">→</span>
                    <span class="amount-value font-medium ${unrealizedEndPositive ? "text-profit" : "text-loss"}">${unrealizedEndPositive ? "+" : "-"}₩${amountFormat.format(unrealizedEnd.abs())}</span>
                </p>
            @endif
        </div>

        <div class="min-w-0">
            <div class="text-xs text-base-content/60 mb-0.5">${growthTitle}</div>
            @if(returnCalculable && periodReturnRatePct != null)
                <div class="text-lg font-bold leading-none ${growthPositive ? "text-profit" : "text-loss"}">
                    ${String.format("%+.2f%%", periodReturnRatePct)}
                </div>
                <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">${growthDescription}</p>
            @elseif(amountFallbackAvailable)
                <%-- 기초 평가액이 거의 0이면 비율이 해석 불가(예: 386,300%)라 금액으로 답한다. --%>
                <div class="text-lg font-bold leading-none amount-value text-base-content/80">
                    ₩${amountFormat.format(closingValue)}
                </div>
                <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">
                    ${growthAmountLabel} <span class="amount-value">₩${amountFormat.format(openingValue)}</span>
                    <span class="text-base-content/35">→</span>
                    <span class="amount-value">₩${amountFormat.format(closingValue)}</span>
                </p>
                <p class="text-[11px] leading-4 text-base-content/35">${growthBaseTooSmallLabel}</p>
            @else
                <div class="text-sm font-semibold text-base-content/60">${unavailableLabel}</div>
                <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">${growthDescription}</p>
            @endif
            @if(principalDeltaAvailable)
                <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">
                    ${principalDeltaLabel}
                    <span class="amount-value font-medium text-base-content/60">${principalIncreased ? "+" : "-"}₩${amountFormat.format(principalDelta.abs())}</span>
                </p>
            @endif
        </div>
    </div>
</div>
