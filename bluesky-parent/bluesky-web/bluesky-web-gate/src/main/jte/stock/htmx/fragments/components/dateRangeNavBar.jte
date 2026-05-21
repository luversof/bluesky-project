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
    String thisMonthLabel = MessageUtil.getMessage("stock.range.this.month");
    String oneMonthLabel = MessageUtil.getMessage("stock.range.one.month");
    String threeMonthsLabel = MessageUtil.getMessage("stock.range.three.months");
    String sixMonthsLabel = MessageUtil.getMessage("stock.range.six.months");
    String thisYearLabel = MessageUtil.getMessage("stock.range.this.year");
    String oneYearLabel = MessageUtil.getMessage("stock.range.one.year");
    String threeYearsLabel = MessageUtil.getMessage("stock.range.three.years");
    String allLabel = MessageUtil.getMessage("stock.label.period.all");
}

<div class="flex flex-wrap gap-2 mb-3 items-center">
    <span class="text-xs opacity-50 mr-1">${periodLabel}</span>
    <button type="button" class="btn btn-xs btn-ghost date-range-prev${!canPrev ? " btn-disabled opacity-40" : ""}" $unsafe{!canPrev ? "disabled" : ""} onclick="${pickerName}.jumpToEdge('start')">«</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-prev${!canPrev ? " btn-disabled opacity-40" : ""}" $unsafe{!canPrev ? "disabled" : ""} onclick="${pickerName}.shift(-1)">‹ ${previousLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("mtd") ? "btn-primary" : "btn-ghost"}" onclick="${pickerName}.set('mtd',this)">${thisMonthLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("1") ? "btn-primary" : "btn-ghost"}" onclick="${pickerName}.set(1,this)">${oneMonthLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("3") ? "btn-primary" : "btn-ghost"}" onclick="${pickerName}.set(3,this)">${threeMonthsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("6") ? "btn-primary" : "btn-ghost"}" onclick="${pickerName}.set(6,this)">${sixMonthsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("ytd") ? "btn-primary" : "btn-ghost"}" onclick="${pickerName}.set('ytd',this)">${thisYearLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("12") ? "btn-primary" : "btn-ghost"}" onclick="${pickerName}.set(12,this)">${oneYearLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("36") ? "btn-primary" : "btn-ghost"}" onclick="${pickerName}.set(36,this)">${threeYearsLabel}</button>
    <button type="button" class="btn btn-xs ${btnClass} ${rangeMode != null && rangeMode.equals("all") ? "btn-primary" : "btn-ghost"}" onclick="${pickerName}.set(0,this)">${allLabel}</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-next${!canNext ? " btn-disabled opacity-40" : ""}" $unsafe{!canNext ? "disabled" : ""} onclick="${pickerName}.shift(1)">${nextLabel} ›</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-next${!canNext ? " btn-disabled opacity-40" : ""}" $unsafe{!canNext ? "disabled" : ""} onclick="${pickerName}.jumpToEdge('end')">»</button>
    @if(showRangeText && (startDate != null || endDate != null))
        <span class="text-xs text-base-content/40 ml-1">
            ${startDate != null ? startDate.toString() : "?"} ~ ${endDate != null ? endDate.minusDays(1).toString() : "?"}
        </span>
    @endif
</div>
