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
    String selectionSummaryLabel = MessageUtil.getMessage("stock.analytics.selection.summary");
    String selectionCountTemplate = MessageUtil.getMessage("stock.analytics.selection.count");
    String selectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
    String selectionPrevLabel = MessageUtil.getMessage("stock.dividend.change.selection.previous");
    String selectionCurrentLabel = MessageUtil.getMessage("stock.dividend.change.selection.current");
    String selectionDeltaLabel = MessageUtil.getMessage("stock.dividend.change.selection.delta");
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
    @template._components.ui.card(cssClass = "mb-4", id = "dividendChangeCard", body = @`
            <div class="flex items-baseline justify-between gap-2 mb-2">
                <h3 class="font-bold text-sm">${changeTitleLabel}</h3>
                @if(prevStartDate != null && prevEndDate != null)
                <span class="text-xs text-base-content/40">vs ${prevStartDate.toString()} ~ ${prevEndDate.toString()}</span>
                @endif
            </div>
            <!-- 선택 합산 패널: 수익률 랭킹(data-dividend-yield-selection-summary)과 동일한 UX -->
            <div class="hidden rounded-2xl border border-info/15 bg-info/5 px-4 py-3 mb-3"
                 data-dividend-change-summary
                 data-count-template="${selectionCountTemplate}"
                 aria-live="polite">
                <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                    <div class="flex flex-wrap items-center gap-2">
                        <h3 class="text-sm font-semibold">${selectionSummaryLabel}</h3>
                        <span class="badge badge-info badge-outline" data-dividend-change-selection-count></span>
                    </div>
                    <button type="button" class="btn btn-ghost btn-xs self-start" data-dividend-change-selection-clear>${selectionClearLabel}</button>
                </div>
                <div class="mt-3 grid gap-3 sm:grid-cols-3">
                    <div class="rounded-box bg-base-100/80 px-3 py-2">
                        <div class="text-xs text-base-content/60">${selectionPrevLabel}</div>
                        <div class="mt-1 font-mono font-semibold amount-value" data-dividend-change-selection-prev>0</div>
                    </div>
                    <div class="rounded-box bg-base-100/80 px-3 py-2">
                        <div class="text-xs text-base-content/60">${selectionCurrentLabel}</div>
                        <div class="mt-1 font-mono font-semibold amount-value" data-dividend-change-selection-current>0</div>
                    </div>
                    <div class="rounded-box bg-base-100/80 px-3 py-2">
                        <div class="text-xs text-base-content/60">${selectionDeltaLabel}</div>
                        <div class="mt-1 font-mono font-semibold amount-value" data-dividend-change-selection-delta>0</div>
                    </div>
                </div>
            </div>
            <ul class="divide-y divide-base-200">
                @for(int ci = 0; ci < Math.min(dividendChangeContributors.size(), 8); ci++)
                    !{
                        DividendChange ch = dividendChangeContributors.get(ci);
                        boolean up = ch.delta().signum() > 0;
                        boolean isNew = ch.previousNet().signum() == 0 && ch.currentNet().signum() != 0;
                        boolean stopped = ch.currentNet().signum() == 0 && ch.previousNet().signum() != 0;
                    }
                    <li class="flex items-center gap-2 py-1.5 text-sm cursor-pointer rounded-field"
                        data-dividend-change-row
                        tabindex="0"
                        role="button"
                        aria-pressed="false"
                        data-prev="${ch.previousNet().toPlainString()}"
                        data-current="${ch.currentNet().toPlainString()}"
                        data-delta="${ch.delta().toPlainString()}">
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
            <script>
            (function() {
                // 변동 요인 행 선택 합산 — 수익률 랭킹의 선택 방식(행 클릭 토글 + 상단 요약 + 선택 해제)과 동일.
                var card = document.getElementById('dividendChangeCard');
                if (!card || card.dataset.selectionInitialized === 'true') return;
                card.dataset.selectionInitialized = 'true';
                var summary = card.querySelector('[data-dividend-change-summary]');
                function setRow(row, selected) {
                    if (!row) return;
                    row.classList.toggle('bg-primary/10', selected);
                    row.setAttribute('aria-pressed', selected ? 'true' : 'false');
                }
                function putAmount(sel, value, signed) {
                    var el = summary.querySelector(sel);
                    if (!el) return;
                    var abs = Math.abs(Math.round(value));
                    el.textContent = (signed ? (value >= 0 ? '+' : '-') : '') + '₩' + abs.toLocaleString();
                    if (signed) {
                        el.classList.toggle('text-profit', value >= 0);
                        el.classList.toggle('text-loss', value < 0);
                    }
                }
                function update() {
                    if (!summary) return;
                    var selected = card.querySelectorAll('[data-dividend-change-row][aria-pressed="true"]');
                    summary.classList.toggle('hidden', selected.length === 0);
                    if (selected.length === 0) return;
                    var prev = 0, current = 0, delta = 0;
                    for (var i = 0; i < selected.length; i++) {
                        prev += Number(selected[i].dataset.prev || '0');
                        current += Number(selected[i].dataset.current || '0');
                        delta += Number(selected[i].dataset.delta || '0');
                    }
                    var countEl = summary.querySelector('[data-dividend-change-selection-count]');
                    if (countEl) countEl.textContent = (summary.dataset.countTemplate || '{0}').replace('{0}', selected.length);
                    putAmount('[data-dividend-change-selection-prev]', prev, false);
                    putAmount('[data-dividend-change-selection-current]', current, false);
                    putAmount('[data-dividend-change-selection-delta]', delta, true);
                }
                var rows = card.querySelectorAll('[data-dividend-change-row]');
                for (var i = 0; i < rows.length; i++) {
                    (function(row) {
                        row.addEventListener('click', function(event) {
                            if (event.target && event.target.closest('button, a, input, label, select, textarea')) return;
                            setRow(this, this.getAttribute('aria-pressed') !== 'true');
                            update();
                        });
                        row.addEventListener('keydown', function(event) {
                            if (event.target && event.target.closest('button, a, input, label, select, textarea')) return;
                            if (event.key !== 'Enter' && event.key !== ' ') return;
                            event.preventDefault();
                            setRow(this, this.getAttribute('aria-pressed') !== 'true');
                            update();
                        });
                    })(rows[i]);
                }
                var clearButton = card.querySelector('[data-dividend-change-selection-clear]');
                if (clearButton) {
                    clearButton.addEventListener('click', function() {
                        var selected = card.querySelectorAll('[data-dividend-change-row][aria-pressed="true"]');
                        for (var i = 0; i < selected.length; i++) setRow(selected[i], false);
                        update();
                    });
                }
            })();
            </script>
    `)
    @endif
