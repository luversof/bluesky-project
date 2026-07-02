@import java.util.List
@import java.time.LocalDate
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.controller.StockDividendHtmxController.DividendChange
@import io.github.luversof.boot.context.support.MessageUtil

@param DecimalFormat decimalFormat
@param BigDecimal safeAllNet
@param BigDecimal safeAllGross
@param BigDecimal safeAllTaxable
@param BigDecimal safeAllTax
@param BigDecimal allDeduction
@param BigDecimal allOtherDeduction
@param BigDecimal safePrevNet
@param boolean hasComparison
@param boolean diffPositive
@param String diffPct
@param BigDecimal diffAmount
@param String totalItemsCountText
@param LocalDate prevStartDate
@param LocalDate prevEndDate
@param List<DividendChange> dividendChangeContributors
@param String taxLabel
@param String taxableAmountLabel

!{
    String periodDividendLabel = MessageUtil.getMessage("stock.dividend.summary.period.total");
    String netLabel = MessageUtil.getMessage("common.label.net");
    String grossLabel = MessageUtil.getMessage("common.label.gross");
    String previousComparisonLabel = MessageUtil.getMessage("stock.dividend.summary.previous.comparison");
    String previousPeriodLabel = MessageUtil.getMessage("stock.dividend.summary.previous.period");
    String comparisonEmptyLabel = MessageUtil.getMessage("stock.dividend.summary.select.period.compare");
    String monthlyAverageLabel = MessageUtil.getMessage("stock.dividend.summary.monthly.average");
    String changeTitleLabel = MessageUtil.getMessage("stock.dividend.change.title");
    String changeNewLabel = MessageUtil.getMessage("stock.dividend.change.new");
    String changeStoppedLabel = MessageUtil.getMessage("stock.dividend.change.stopped");
    String deductionLabel = MessageUtil.getMessage("stock.dividend.deduction");
    String deductionOtherLabel = MessageUtil.getMessage("stock.dividend.deduction.other");
    String calcNoteLabel = MessageUtil.getMessage("stock.dividend.calc.note");
    String taxableNoteLabel = MessageUtil.getMessage("stock.dividend.taxable.note");
}

    <!-- Row 1: Summary Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">

        <!-- Card 1: 기간 합계 (세전/세후/과세) -->
        @template._components.ui.card(body = @`
                <!-- 세후 배당금 / 과세 금액: 서로 다른 개념이라 동등 비중의 별도 지표로 나란히 표시 -->
                <div class="flex flex-wrap items-start gap-x-6 gap-y-2 mb-2">
                    <div class="min-w-0">
                        <div class="text-xs text-base-content/50 mb-1">${periodDividendLabel}</div>
                        <div class="flex items-baseline gap-1">
                            <span class="text-xl font-bold text-dividend amount-value">₩ ${decimalFormat.format(safeAllNet)}</span>
                            <span class="text-xs text-base-content/40">${netLabel}</span>
                        </div>
                    </div>
                    <div class="min-w-0">
                        <div class="text-xs text-base-content/50 mb-1">${taxableAmountLabel}</div>
                        <div class="text-xl font-bold text-base-content/70 amount-value">₩ ${decimalFormat.format(safeAllTaxable)}</div>
                        <div class="text-[11px] leading-4 text-base-content/40">${taxableNoteLabel}</div>
                    </div>
                </div>
                <div class="flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs text-base-content/50">
                    <span>${grossLabel} <span class="font-medium text-base-content/70 amount-value">₩ ${decimalFormat.format(safeAllGross)}</span></span>
                    <span>${deductionLabel} <span class="font-medium text-base-content/70 amount-value">-₩ ${decimalFormat.format(allDeduction)}</span></span>
                    <span class="ml-auto">${totalItemsCountText}</span>
                </div>
                <!-- 차감 분해(항상 표시): 세전 − (세금 + 수수료·기타) = 세후. 과세 금액은 차감 항목이 아님(별도 표시). -->
                <div class="mt-1 text-[11px] leading-4 text-base-content/40" title="${calcNoteLabel}">
                    ${deductionLabel} = ${taxLabel} <span class="amount-value">₩${decimalFormat.format(safeAllTax)}</span>
                    + ${deductionOtherLabel} <span class="amount-value">₩${decimalFormat.format(allOtherDeduction)}</span>
                </div>
        `)

        <!-- Card 2: 전기 대비 -->
        @if(hasComparison)
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${previousComparisonLabel}
                    @if(prevStartDate != null && prevEndDate != null)
                    <span class="opacity-40">(${prevStartDate.toString()} ~ ${prevEndDate.toString()})</span>
                    @endif
                </div>
                <div class="flex items-baseline gap-2 mb-1">
                    <span class="text-xl font-bold ${diffPositive ? "text-profit" : "text-loss"}">${diffPositive ? "▲" : "▼"} ${diffPct}%</span>
                    <span class="text-xs amount-value ${diffPositive ? "text-profit" : "text-loss"}">${diffPositive ? "+" : "-"}₩${decimalFormat.format(diffAmount.abs())}</span>
                </div>
                <div class="text-xs text-base-content/40">${previousPeriodLabel} <span class="amount-value">₩ ${decimalFormat.format(safePrevNet)}</span></div>
        `)
        @else
        @template._components.ui.card(bodyClass = "p-4 items-center justify-center text-center", body = @`
                <div class="text-xs text-base-content/30">${comparisonEmptyLabel}</div>
        `)
        @endif

        <!-- Card 3: 월평균 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${monthlyAverageLabel}</div>
                <div class="text-xl font-bold"><span id="avgAmountDisplay" class="amount-value">-</span></div>
                <div class="text-xs text-base-content/40 mt-1" id="avgAmountDesc"></div>
        `)

    </div>

    <!-- 변동 요인: 전기 대비 종목별 배당 증감(왜 차이가 났는지) -->
    @if(hasComparison && dividendChangeContributors != null && !dividendChangeContributors.isEmpty())
    @template._components.ui.card(cssClass = "mb-4", body = @`
            <div class="flex items-baseline justify-between gap-2 mb-2">
                <h3 class="font-bold text-sm">${changeTitleLabel}</h3>
                @if(prevStartDate != null && prevEndDate != null)
                <span class="text-xs text-base-content/40">vs ${prevStartDate.toString()} ~ ${prevEndDate.toString()}</span>
                @endif
            </div>
            <ul class="divide-y divide-base-200">
                @for(int ci = 0; ci < Math.min(dividendChangeContributors.size(), 8); ci++)
                    !{
                        DividendChange ch = dividendChangeContributors.get(ci);
                        boolean up = ch.delta().signum() > 0;
                        boolean isNew = ch.previousNet().signum() == 0 && ch.currentNet().signum() != 0;
                        boolean stopped = ch.currentNet().signum() == 0 && ch.previousNet().signum() != 0;
                    }
                    <li class="flex items-center gap-2 py-1.5 text-sm">
                        <span class="flex-1 min-w-0 truncate">${ch.stockName()}</span>
                        @if(isNew)
                        <span class="badge badge-xs badge-success badge-outline shrink-0">${changeNewLabel}</span>
                        @elseif(stopped)
                        <span class="badge badge-xs badge-ghost shrink-0">${changeStoppedLabel}</span>
                        @endif
                        <span class="hidden sm:inline text-xs text-base-content/40 amount-value">₩${decimalFormat.format(ch.previousNet())} → ₩${decimalFormat.format(ch.currentNet())}</span>
                        <span class="w-28 text-right font-semibold amount-value ${up ? "text-profit" : "text-loss"}">${up ? "+" : "-"}₩${decimalFormat.format(ch.delta().abs())}</span>
                    </li>
                @endfor
            </ul>
            @if(dividendChangeContributors.size() > 8)
            <div class="text-xs text-base-content/40 mt-2">+ ${dividendChangeContributors.size() - 8}</div>
            @endif
    `)
    @endif
