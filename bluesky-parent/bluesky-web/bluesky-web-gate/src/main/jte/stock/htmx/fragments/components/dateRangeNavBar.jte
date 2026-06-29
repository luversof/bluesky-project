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
    <button type="button" class="btn btn-xs btn-ghost date-range-prev${!canPrev ? " btn-disabled opacity-40" : ""}" $unsafe{!canPrev ? "disabled" : ""} onclick="${pickerName}.jumpToEdge('start')">«</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-prev${!canPrev ? " btn-disabled opacity-40" : ""}" title="${previousLabel}" aria-label="${previousLabel}" $unsafe{!canPrev ? "disabled" : ""} onclick="${pickerName}.shift(-1)">‹ ${useCompactEnglishLabels ? previousCompactLabel : previousLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("mtd") ? "btn-primary" : "btn-ghost"}" title="${thisMonthLabel}" aria-label="${thisMonthLabel}" onclick="${pickerName}.set('mtd',this)">${useCompactEnglishLabels ? thisMonthCompactLabel : thisMonthLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("1") ? "btn-primary" : "btn-ghost"}" title="${oneMonthLabel}" aria-label="${oneMonthLabel}" onclick="${pickerName}.set(1,this)">${useCompactEnglishLabels ? oneMonthCompactLabel : oneMonthLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("3") ? "btn-primary" : "btn-ghost"}" title="${threeMonthsLabel}" aria-label="${threeMonthsLabel}" onclick="${pickerName}.set(3,this)">${useCompactEnglishLabels ? threeMonthsCompactLabel : threeMonthsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("6") ? "btn-primary" : "btn-ghost"}" title="${sixMonthsLabel}" aria-label="${sixMonthsLabel}" onclick="${pickerName}.set(6,this)">${useCompactEnglishLabels ? sixMonthsCompactLabel : sixMonthsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("ytd") ? "btn-primary" : "btn-ghost"}" title="${thisYearLabel}" aria-label="${thisYearLabel}" onclick="${pickerName}.set('ytd',this)">${useCompactEnglishLabels ? thisYearCompactLabel : thisYearLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("12") ? "btn-primary" : "btn-ghost"}" title="${oneYearLabel}" aria-label="${oneYearLabel}" onclick="${pickerName}.set(12,this)">${useCompactEnglishLabels ? oneYearCompactLabel : oneYearLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("36") ? "btn-primary" : "btn-ghost"}" title="${threeYearsLabel}" aria-label="${threeYearsLabel}" onclick="${pickerName}.set(36,this)">${useCompactEnglishLabels ? threeYearsCompactLabel : threeYearsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("all") ? "btn-primary" : "btn-ghost"}" title="${allLabel}" aria-label="${allLabel}" onclick="${pickerName}.set(0,this)">${useCompactEnglishLabels ? allCompactLabel : allLabel}</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-next${!canNext ? " btn-disabled opacity-40" : ""}" title="${nextLabel}" aria-label="${nextLabel}" $unsafe{!canNext ? "disabled" : ""} onclick="${pickerName}.shift(1)">${useCompactEnglishLabels ? nextCompactLabel : nextLabel} ›</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-next${!canNext ? " btn-disabled opacity-40" : ""}" $unsafe{!canNext ? "disabled" : ""} onclick="${pickerName}.jumpToEdge('end')">»</button>
    @if(showRangeText && (startDate != null || endDate != null))
        <span class="badge badge-ghost badge-sm font-medium text-base-content/80 ml-1 whitespace-nowrap">
            ${startDate != null ? startDate.toString() : "?"} ~ ${endDate != null ? endDate.minusDays(1).toString() : "?"}
        </span>
    @endif
</div>
