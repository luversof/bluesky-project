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
    boolean drawdownAvailable = maxDrawdownPct != null;
    boolean drawdownRangeAvailable = maxDrawdownPeakDate != null && maxDrawdownTroughDate != null;
}

<div id="assetGrowthPeriodReturnSummary" class="rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3 mb-3">

    <%-- 주지표: 평가손익 — 사용자가 가장 중요하게 보는 "지금 원금 대비 얼마인가" --%>
    <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
        <span class="text-sm font-semibold text-base-content/70">${unrealizedLabel}</span>
        @if(!fromDate.isBlank() && !toDate.isBlank())
            <span class="inline-flex items-center rounded-full bg-base-200/80 px-2 py-0.5 text-[11px] font-medium text-base-content/55">
                ${fromDate} ~ ${toDate}
            </span>
        @endif
    </div>
    <div class="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
        @if(unrealizedAvailable)
            <span class="text-4xl font-bold leading-none amount-value ${unrealizedEndPositive ? "text-profit" : "text-loss"}">
                ${unrealizedEndPositive ? "+" : "-"}₩${amountFormat.format(unrealizedEnd.abs())}
            </span>
            @if(unrealizedEndPct != null)
                <span class="text-xl font-semibold ${unrealizedEndPositive ? "text-profit" : "text-loss"}">
                    ${String.format("%+.2f%%", unrealizedEndPct)}
                </span>
            @endif
        @else
            <span class="text-lg font-semibold text-base-content/40">${unavailableLabel}</span>
        @endif
    </div>
    @if(unrealizedAvailable)
        <p class="text-xs text-base-content/45 mt-1">
            ${unrealizedTrendLabel}
            <span class="amount-value font-medium ${unrealizedStartPositive ? "text-profit" : "text-loss"}">${unrealizedStartPositive ? "+" : "-"}₩${amountFormat.format(unrealizedStart.abs())}</span>
            <span class="text-base-content/35">→</span>
            <span class="amount-value font-medium ${unrealizedEndPositive ? "text-profit" : "text-loss"}">${unrealizedEndPositive ? "+" : "-"}₩${amountFormat.format(unrealizedEnd.abs())}</span>
        </p>
    @endif

    <%-- 보조 지표: 운용 성과 / 기간 손익 / 자산 증가율 --%>
    <div class="mt-3 pt-3 border-t border-base-200 grid gap-3 sm:grid-cols-3">
        <div class="min-w-0">
            <div class="text-xs text-base-content/50 mb-0.5">${twrTitle}</div>
            @if(twrAvailable)
                <div class="text-lg font-bold leading-none ${twrPositive ? "text-profit" : "text-loss"}">
                    ${String.format("%+.2f%%", timeWeightedReturnPct)}
                </div>
            @else
                <div class="text-sm font-semibold text-base-content/40">${unavailableLabel}</div>
            @endif
            <p class="text-[11px] leading-4 text-base-content/40 mt-0.5">${twrDescription}</p>
            @if(drawdownAvailable)
                <%-- 수익률의 짝이 되는 위험 지표. 입출금을 제거한 기준가로 계산해 입금에 희석되지 않는다. --%>
                <p class="text-[11px] leading-4 mt-0.5">
                    <span class="text-base-content/45">${maxDrawdownLabel}</span>
                    <span class="font-semibold text-loss">${String.format("%.2f%%", maxDrawdownPct)}</span>
                    @if(drawdownRangeAvailable)
                        <span class="text-base-content/35">(${maxDrawdownPeakDate.toString()} → ${maxDrawdownTroughDate.toString()})</span>
                    @endif
                </p>
            @endif
        </div>

        <div class="min-w-0">
            <div class="text-xs text-base-content/50 mb-0.5">${profitTitle}</div>
            @if(profitAvailable)
                <div class="text-lg font-bold leading-none amount-value ${profitPositive ? "text-profit" : "text-loss"}">
                    ${profitPositive ? "+" : "-"}₩${amountFormat.format(periodProfit.abs())}
                </div>
            @else
                <div class="text-sm font-semibold text-base-content/40">${unavailableLabel}</div>
            @endif
            <p class="text-[11px] leading-4 text-base-content/40 mt-0.5">${profitDescription}</p>
            @if(splitAvailable)
                <%-- 기간 손익 = 손실 회복분 + 순증분. 회복분은 '되찾은 돈', 순증분이 '새로 번 돈'. --%>
                <p class="text-[11px] leading-4 mt-0.5">
                    <span class="text-base-content/45">${recoveredLabel}</span>
                    <span class="amount-value font-medium text-base-content/60">₩${amountFormat.format(recoveredAmount)}</span>
                    <span class="text-base-content/30 mx-0.5">·</span>
                    <span class="text-base-content/45">${netNewLabel}</span>
                    <span class="amount-value font-semibold ${netNewPositive ? "text-profit" : "text-loss"}">${netNewPositive ? "+" : "-"}₩${amountFormat.format(netNewProfit.abs())}</span>
                </p>
            @endif
        </div>

        <div class="min-w-0">
            <div class="text-xs text-base-content/50 mb-0.5">${growthTitle}</div>
            @if(returnCalculable && periodReturnRatePct != null)
                <div class="text-lg font-bold leading-none ${growthPositive ? "text-profit" : "text-loss"}">
                    ${String.format("%+.2f%%", periodReturnRatePct)}
                </div>
            @else
                <div class="text-sm font-semibold text-base-content/40">${unavailableLabel}</div>
            @endif
            <p class="text-[11px] leading-4 text-base-content/40 mt-0.5">${growthDescription}</p>
            @if(principalDeltaAvailable)
                <p class="text-[11px] leading-4 text-base-content/40 mt-0.5">
                    ${principalDeltaLabel}
                    <span class="amount-value font-medium text-base-content/60">${principalIncreased ? "+" : "-"}₩${amountFormat.format(principalDelta.abs())}</span>
                </p>
            @endif
        </div>
    </div>
</div>
