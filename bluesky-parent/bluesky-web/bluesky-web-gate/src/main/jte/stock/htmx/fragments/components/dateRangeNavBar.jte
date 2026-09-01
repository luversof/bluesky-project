@import net.luversof.web.gate.util.CspNonceHolder
@import java.time.LocalDate
@import io.github.luversof.boot.context.support.MessageUtil

@param String pickerName
@param String btnClass
@param boolean canPrev
@param boolean canNext
@param String rangeMode = null
@param LocalDate startDate = null
@param LocalDate endDate = null
@param boolean showRangeText = true
<%-- '전체' 처럼 기간을 안 거는 경우에 대신 적을, 화면이 실제로 덮은 구간. --%>
@param LocalDate coveredStartDate = null
@param LocalDate coveredEndDate = null

!{
    String periodLabel = MessageUtil.getMessage("common.label.period");
    String previousLabel = MessageUtil.getMessage("common.button.previous");
    String nextLabel = MessageUtil.getMessage("common.button.next");
    String previousCompactLabel = MessageUtil.getMessage("common.button.previous.compact");
    String nextCompactLabel = MessageUtil.getMessage("common.button.next.compact");
    String thisMonthLabel = MessageUtil.getMessage("stock.range.this.month");
    String oneMonthLabel = MessageUtil.getMessage("stock.range.one.month");
    String threeMonthsLabel = MessageUtil.getMessage("stock.range.three.months");
    String sixMonthsLabel = MessageUtil.getMessage("stock.range.six.months");
    String thisYearLabel = MessageUtil.getMessage("stock.range.this.year");
    String oneYearLabel = MessageUtil.getMessage("stock.range.one.year");
    String threeYearsLabel = MessageUtil.getMessage("stock.range.three.years");
    String allLabel = MessageUtil.getMessage("stock.label.period.all");
    String thisMonthCompactLabel = MessageUtil.getMessage("stock.range.this.month.compact");
    String oneMonthCompactLabel = MessageUtil.getMessage("stock.range.one.month.compact");
    String threeMonthsCompactLabel = MessageUtil.getMessage("stock.range.three.months.compact");
    String sixMonthsCompactLabel = MessageUtil.getMessage("stock.range.six.months.compact");
    String thisYearCompactLabel = MessageUtil.getMessage("stock.range.this.year.compact");
    String oneYearCompactLabel = MessageUtil.getMessage("stock.range.one.year.compact");
    String threeYearsCompactLabel = MessageUtil.getMessage("stock.range.three.years.compact");
    String allCompactLabel = MessageUtil.getMessage("stock.label.period.all.compact");
    boolean useCompactEnglishLabels = "en".equalsIgnoreCase(org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage());
}

<div class="flex flex-wrap gap-2 mb-3 items-center">
    <span class="text-xs opacity-50 mr-1">${periodLabel}</span>
    <button type="button" class="btn btn-xs btn-ghost date-range-prev${!canPrev ? " btn-disabled opacity-40" : ""}" $unsafe{!canPrev ? "disabled" : ""} data-picker="${pickerName}" data-picker-action="jump" data-picker-arg="start">«</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-prev${!canPrev ? " btn-disabled opacity-40" : ""}" title="${previousLabel}" aria-label="${previousLabel}" $unsafe{!canPrev ? "disabled" : ""} data-picker="${pickerName}" data-picker-action="shift" data-picker-arg="-1">‹ ${useCompactEnglishLabels ? previousCompactLabel : previousLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("mtd") ? "btn-primary" : "btn-ghost"}" title="${thisMonthLabel}" aria-label="${thisMonthLabel}" data-picker="${pickerName}" data-picker-action="set" data-picker-arg="mtd">${useCompactEnglishLabels ? thisMonthCompactLabel : thisMonthLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("1") ? "btn-primary" : "btn-ghost"}" title="${oneMonthLabel}" aria-label="${oneMonthLabel}" data-picker="${pickerName}" data-picker-action="set" data-picker-arg="1">${useCompactEnglishLabels ? oneMonthCompactLabel : oneMonthLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("3") ? "btn-primary" : "btn-ghost"}" title="${threeMonthsLabel}" aria-label="${threeMonthsLabel}" data-picker="${pickerName}" data-picker-action="set" data-picker-arg="3">${useCompactEnglishLabels ? threeMonthsCompactLabel : threeMonthsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("6") ? "btn-primary" : "btn-ghost"}" title="${sixMonthsLabel}" aria-label="${sixMonthsLabel}" data-picker="${pickerName}" data-picker-action="set" data-picker-arg="6">${useCompactEnglishLabels ? sixMonthsCompactLabel : sixMonthsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("ytd") ? "btn-primary" : "btn-ghost"}" title="${thisYearLabel}" aria-label="${thisYearLabel}" data-picker="${pickerName}" data-picker-action="set" data-picker-arg="ytd">${useCompactEnglishLabels ? thisYearCompactLabel : thisYearLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("12") ? "btn-primary" : "btn-ghost"}" title="${oneYearLabel}" aria-label="${oneYearLabel}" data-picker="${pickerName}" data-picker-action="set" data-picker-arg="12">${useCompactEnglishLabels ? oneYearCompactLabel : oneYearLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("36") ? "btn-primary" : "btn-ghost"}" title="${threeYearsLabel}" aria-label="${threeYearsLabel}" data-picker="${pickerName}" data-picker-action="set" data-picker-arg="36">${useCompactEnglishLabels ? threeYearsCompactLabel : threeYearsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("all") ? "btn-primary" : "btn-ghost"}" title="${allLabel}" aria-label="${allLabel}" data-picker="${pickerName}" data-picker-action="set" data-picker-arg="0">${useCompactEnglishLabels ? allCompactLabel : allLabel}</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-next${!canNext ? " btn-disabled opacity-40" : ""}" title="${nextLabel}" aria-label="${nextLabel}" $unsafe{!canNext ? "disabled" : ""} data-picker="${pickerName}" data-picker-action="shift" data-picker-arg="1">${useCompactEnglishLabels ? nextCompactLabel : nextLabel} ›</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-next${!canNext ? " btn-disabled opacity-40" : ""}" $unsafe{!canNext ? "disabled" : ""} data-picker="${pickerName}" data-picker-action="jump" data-picker-arg="end">»</button>
    @if(showRangeText && (startDate != null || endDate != null))
        <span class="badge badge-ghost badge-sm font-medium text-base-content/80 ml-1 whitespace-nowrap">
            ${startDate != null ? startDate.toString() : "?"} ~ ${endDate != null ? endDate.minusDays(1).toString() : "?"}
        </span>
    @elseif(showRangeText && (coveredStartDate != null || coveredEndDate != null))
        <%-- '전체' 는 날짜를 안 보내므로 위 배지가 그릴 값이 없다. 그렇다고 비워 두면 그 구간이 몇 년치인지
             화면 어디에서도 알 수 없다(종목마다 첫 거래일이 다르다). 조회된 자료의 구간을 대신 적되,
             기간 필터로 오해하지 않게 '전체' 라고 앞에 붙인다. --%>
        <span class="badge badge-ghost badge-sm font-medium text-base-content/60 ml-1 whitespace-nowrap" data-covered-range>
            ${allLabel} · ${coveredStartDate != null ? coveredStartDate.toString() : "?"} ~ ${coveredEndDate != null ? coveredEndDate.toString() : "?"}
        </span>
    @endif
</div>

<script nonce="${CspNonceHolder.getNonce()}">
    // 이 컴포넌트는 htmx 스왑마다 재렌더되므로 document 위임 + 전역 1회 가드로 바인딩한다.
    if (!window.__dateRangeNavBarBound) {
        window.__dateRangeNavBarBound = true;
        document.addEventListener('click', function(event) {
            var button = event.target && event.target.closest ? event.target.closest('[data-picker-action]') : null;
            if (!button) {
                return;
            }
            var picker = window[button.dataset.picker];
            if (!picker) {
                return;
            }
            var action = button.dataset.pickerAction;
            var arg = button.dataset.pickerArg;
            if (action === 'jump') {
                picker.jumpToEdge(arg);
            } else if (action === 'shift') {
                picker.shift(parseInt(arg, 10));
            } else if (action === 'set') {
                picker.set(/^-?\d+$/.test(arg) ? parseInt(arg, 10) : arg, button);
            }
        });
    }
</script>
