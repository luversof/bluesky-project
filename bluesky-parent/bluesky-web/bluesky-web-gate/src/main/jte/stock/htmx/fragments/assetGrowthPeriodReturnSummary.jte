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
@param Double periodProfitRatePct = null
@param BigDecimal peakValue = null
@param java.time.LocalDate peakValueDate = null
@param BigDecimal troughValue = null
@param java.time.LocalDate troughValueDate = null

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

    // 기간 손익률 - 헤드라인 금액 옆에 붙이고, 아래 '수익률' 절에서 나머지 둘과 나란히 다시 보여준다.
    // 셋은 분모가 다르다: 손익률(넣어 둔 돈) / 투자 수익률(입출금 제거) / 자산 증가율(평가액).
    boolean profitRateAvailable = periodProfitRatePct != null;
    boolean profitRatePositive = profitRateAvailable && periodProfitRatePct >= 0.0d;

    String ratesTitle = MessageUtil.getMessage("stock.asset.growth.summary.rates.title");
    String profitRateTitle = MessageUtil.getMessage("stock.asset.growth.summary.profit.rate");
    String profitRateDescription = MessageUtil.getMessage("stock.asset.growth.summary.profit.rate.desc");
    String movementTitle = MessageUtil.getMessage("stock.asset.growth.summary.movement.title");
    String periodHighLabel = MessageUtil.getMessage("stock.asset.growth.summary.period.high");
    String periodLowLabel = MessageUtil.getMessage("stock.asset.growth.summary.period.low");
    String vsNowLabel = MessageUtil.getMessage("stock.asset.growth.summary.vs.now");
    String extremesDescription = MessageUtil.getMessage("stock.asset.growth.summary.extremes.desc");
    String riskTitle = MessageUtil.getMessage("stock.asset.growth.summary.risk");

    boolean closingAvailable = closingValue != null;
    boolean peakAvailable = peakValue != null && peakValue.signum() > 0 && closingAvailable;
    boolean troughAvailable = troughValue != null && troughValue.signum() > 0 && closingAvailable;
    double peakGapPct = peakAvailable
        ? closingValue.subtract(peakValue).doubleValue() / peakValue.doubleValue() * 100.0d : 0.0d;
    double troughGapPct = troughAvailable
        ? closingValue.subtract(troughValue).doubleValue() / troughValue.doubleValue() * 100.0d : 0.0d;
    // 전체 기간처럼 저점이 첫 매수일이면 비율이 수십만 %가 되어 읽히지 않는다. 그럴 땐 배수로 답한다
    // (실측 2026-08-27 '전체': 최저 평가액 대비 +400,291% = 약 4,004 배).
    boolean peakAsMultiple = peakAvailable && Math.abs(peakGapPct) >= 1000.0d;
    boolean troughAsMultiple = troughAvailable && Math.abs(troughGapPct) >= 1000.0d;
    String peakGapText = !peakAvailable ? ""
        : peakAsMultiple
            ? String.format("x%,.0f", closingValue.doubleValue() / peakValue.doubleValue())
            : String.format("%+.2f%%", peakGapPct);
    String troughGapText = !troughAvailable ? ""
        : troughAsMultiple
            ? String.format("x%,.0f", closingValue.doubleValue() / troughValue.doubleValue())
            : String.format("%+.2f%%", troughGapPct);
    boolean movementAvailable = peakAvailable || troughAvailable || drawdownAvailable
        || currentDrawdownAvailable;
}

<div id="assetGrowthPeriodReturnSummary" class="rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3 mb-3">

    <%-- 주지표: 기간 손익 — 이 화면은 기간을 고르는 화면이므로, 고른 기간에 답하는 값이 맨 위에 와야 한다.
         예전에는 평가손익이 주지표였는데 그 값은 <b>기간이 끝나는 날</b>만 따라간다. 화면의 프리셋
         (이번달·올해·1년·3년·전체)은 끝나는 날이 모두 오늘이라, 무엇을 골라도 큰 숫자가 꿈쩍하지 않았다
         (실측 2026-08-24: 다섯 기간 모두 <b>완전히 같은 값</b>). 정작 기간 손익은 이번달 손실부터 전체
         기간의 큰 이익까지 기간마다 갈렸고, 이번달은 손실인데 맨 위에는 큰 이익이 떠 있었다.
         둘의 자리를 바꾼다. --%>
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
            @if(profitRateAvailable)
                <%-- 금액만으로는 "많이 번 건지" 를 알 수 없다. 넣어 둔 돈 대비 비율을 바로 옆에 붙인다. --%>
                <span class="text-xl font-bold ${profitRatePositive ? "text-profit" : "text-loss"}">
                    ${String.format("%+.2f%%", periodProfitRatePct)}
                </span>
            @endif
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

    <%-- 수익률 세 가지를 한 줄에 나란히 둔다.
         예전에는 투자 수익률(TWR)과 자산 증가율이 다른 칸에 흩어져 있고 손익률은 아예 없었다. 그래서
         "기간 손익률이 곧 투자 수익률인가" 를 화면에서 확인할 방법이 없었다. 셋은 <b>분모가 다른 별개의
         값</b>이다 - 실측 2026-08-27(올해): 94.93% / 92.88% / 76.66%. 나란히 두고 각자의 분모를 적는다. --%>
    <div class="mt-3 pt-3 border-t border-base-200">
        <div class="text-[11px] font-semibold text-base-content/50 mb-1.5">${ratesTitle}</div>
        <div class="grid gap-3 sm:grid-cols-3">
            <div class="min-w-0">
                <div class="text-xs text-base-content/60 mb-0.5">${profitRateTitle}</div>
                @if(profitRateAvailable)
                    <div class="text-lg font-bold leading-none ${profitRatePositive ? "text-profit" : "text-loss"}">
                        ${String.format("%+.2f%%", periodProfitRatePct)}
                    </div>
                @else
                    <div class="text-sm font-semibold text-base-content/60">${unavailableLabel}</div>
                @endif
                <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">${profitRateDescription}</p>
            </div>

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
            </div>

            <div class="min-w-0">
                <div class="text-xs text-base-content/60 mb-0.5">${growthTitle}</div>
                @if(returnCalculable && periodReturnRatePct != null)
                    <div class="text-lg font-bold leading-none ${growthPositive ? "text-profit" : "text-loss"}">
                        ${String.format("%+.2f%%", periodReturnRatePct)}
                    </div>
                    <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">${growthDescription}</p>
                @elseif(amountFallbackAvailable)
                    <%-- 기초 평가액이 거의 0이면 비율이 해석 불가라 금액으로 답한다. 이때도 왼쪽의 기간
                         손익률은 나온다 - 분모에 유입 원금이 들어가기 때문이다. --%>
                    <div class="text-lg font-bold leading-none amount-value text-base-content/80">
                        &#8361;${amountFormat.format(closingValue)}
                    </div>
                    <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">
                        ${growthAmountLabel} <span class="amount-value">&#8361;${amountFormat.format(openingValue)}</span>
                        <span class="text-base-content/35">&#8594;</span>
                        <span class="amount-value">&#8361;${amountFormat.format(closingValue)}</span>
                    </p>
                    <p class="text-[11px] leading-4 text-base-content/35">${growthBaseTooSmallLabel}</p>
                @else
                    <div class="text-sm font-semibold text-base-content/60">${unavailableLabel}</div>
                    <p class="text-[11px] leading-4 text-base-content/60 mt-0.5">${growthDescription}</p>
                @endif
            </div>
        </div>
    </div>

    <%-- 기간 중 움직임: 고점 · 저점 · 낙폭.
         고점/저점은 차트 주석에만 있었다 - 차트를 눈으로 훑어야만 알 수 있는 값이라 카드에도 적는다.
         값 · 날짜 · 제외 규칙(평가액 0 인 날 제외)이 차트 주석과 같은 계산에서 나오므로 두 곳이 어긋나지 않는다. --%>
    @if(movementAvailable)
        <div class="mt-3 pt-3 border-t border-base-200">
            <div class="text-[11px] font-semibold text-base-content/50 mb-1.5">${movementTitle}</div>
            <div class="grid gap-3 sm:grid-cols-3">
                <div class="min-w-0">
                    <div class="text-xs text-base-content/60 mb-0.5">${periodHighLabel}</div>
                    @if(peakAvailable)
                        <div class="text-lg font-bold leading-none amount-value text-base-content/80">
                            &#8361;${amountFormat.format(peakValue)}
                        </div>
                        <p class="text-[11px] leading-4 mt-0.5">
                            @if(peakValueDate != null)
                                <span class="text-base-content/50">${peakValueDate.toString()}</span>
                                <span class="text-base-content/30 mx-0.5">&middot;</span>
                            @endif
                            <span class="text-base-content/60">${vsNowLabel}</span>
                            <span class="font-semibold ${peakGapPct >= 0.0d ? "text-profit" : "text-loss"}">${peakGapText}</span>
                        </p>
                    @else
                        <div class="text-sm font-semibold text-base-content/60">${unavailableLabel}</div>
                    @endif
                </div>

                <div class="min-w-0">
                    <div class="text-xs text-base-content/60 mb-0.5">${periodLowLabel}</div>
                    @if(troughAvailable)
                        <div class="text-lg font-bold leading-none amount-value text-base-content/80">
                            &#8361;${amountFormat.format(troughValue)}
                        </div>
                        <p class="text-[11px] leading-4 mt-0.5">
                            @if(troughValueDate != null)
                                <span class="text-base-content/50">${troughValueDate.toString()}</span>
                                <span class="text-base-content/30 mx-0.5">&middot;</span>
                            @endif
                            <span class="text-base-content/60">${vsNowLabel}</span>
                            <span class="font-semibold ${troughGapPct >= 0.0d ? "text-profit" : "text-loss"}">${troughGapText}</span>
                        </p>
                    @else
                        <div class="text-sm font-semibold text-base-content/60">${unavailableLabel}</div>
                    @endif
                    <p class="text-[11px] leading-4 text-base-content/35 mt-0.5">${extremesDescription}</p>
                </div>

                <div class="min-w-0">
                    <div class="text-xs text-base-content/60 mb-0.5">${riskTitle}</div>
                    @if(drawdownAvailable)
                        <%-- 수익률의 짝이 되는 위험 지표. 입출금을 제거한 기준가로 계산해 입금에 희석되지 않는다. --%>
                        <div class="text-lg font-bold leading-none text-loss">
                            ${String.format("%.2f%%", maxDrawdownPct)}
                        </div>
                        <p class="text-[11px] leading-4 mt-0.5">
                            <span class="text-base-content/60">${maxDrawdownLabel}</span>
                            @if(drawdownRangeAvailable)
                                <span class="text-base-content/35">(${maxDrawdownPeakDate.toString()} &#8594; ${maxDrawdownTroughDate.toString()})</span>
                            @endif
                        </p>
                    @else
                        <div class="text-sm font-semibold text-base-content/60">${unavailableLabel}</div>
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
            </div>
        </div>
    @endif

    <%-- 평가손익은 기간이 아니라 <b>기말 시점</b> 값이라 아래에 한 줄로 둔다(시작일만 바꾸면 값이 그대로다).
         기간 중 원금 변동도 같은 줄에 붙여, 위의 수익률들이 무엇 위에서 계산됐는지 보이게 한다. --%>
    <div class="mt-3 pt-3 border-t border-base-200 flex flex-wrap items-baseline gap-x-2 gap-y-1 text-[11px] leading-4">
        <span class="text-base-content/60">${unrealizedLabel}</span>
        @if(unrealizedAvailable)
            <span class="amount-value font-semibold ${unrealizedEndPositive ? "text-profit" : "text-loss"}">${unrealizedEndPositive ? "+" : "-"}&#8361;${amountFormat.format(unrealizedEnd.abs())}</span>
            @if(unrealizedEndPct != null)
                <span class="font-semibold ${unrealizedEndPositive ? "text-profit" : "text-loss"}">${String.format("%+.2f%%", unrealizedEndPct)}</span>
            @endif
            <span class="text-base-content/25">|</span>
            <span class="text-base-content/60">${unrealizedTrendLabel}</span>
            <span class="amount-value ${unrealizedStartPositive ? "text-profit" : "text-loss"}">${unrealizedStartPositive ? "+" : "-"}&#8361;${amountFormat.format(unrealizedStart.abs())}</span>
            <span class="text-base-content/35">&#8594;</span>
            <span class="amount-value ${unrealizedEndPositive ? "text-profit" : "text-loss"}">${unrealizedEndPositive ? "+" : "-"}&#8361;${amountFormat.format(unrealizedEnd.abs())}</span>
        @else
            <span class="font-semibold text-base-content/60">${unavailableLabel}</span>
        @endif
        @if(principalDeltaAvailable)
            <span class="text-base-content/25">|</span>
            <span class="text-base-content/60">${principalDeltaLabel}</span>
            <span class="amount-value font-medium text-base-content/60">${principalIncreased ? "+" : "-"}&#8361;${amountFormat.format(principalDelta.abs())}</span>
        @endif
    </div>
    <p class="text-[11px] leading-4 text-base-content/35 mt-1">${unrealizedDescription}</p>
</div>
