@import java.time.LocalDate

@param String pickerName
@param String btnClass
@param boolean canPrev
@param boolean canNext
@param String rangeMode = null
@param LocalDate startDate = null
@param LocalDate endDate = null

<div class="flex flex-wrap gap-2 mb-3 items-center">
    <span class="text-xs opacity-50 mr-1">기간</span>
    <button type="button" class="btn btn-xs btn-ghost date-range-prev${!canPrev ? " btn-disabled opacity-40" : ""}" $unsafe{!canPrev ? "disabled" : ""} onclick="${pickerName}.jumpToEdge('start')">«</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-prev${!canPrev ? " btn-disabled opacity-40" : ""}" $unsafe{!canPrev ? "disabled" : ""} onclick="${pickerName}.shift(-1)">‹ 이전</button>
    <button type="button" class="btn btn-xs ${btnClass} btn-ghost" onclick="${pickerName}.set('mtd',this)">이번달</button>
    <button type="button" class="btn btn-xs ${btnClass} btn-ghost" onclick="${pickerName}.set(1,this)">1개월</button>
    <button type="button" class="btn btn-xs ${btnClass} btn-ghost" onclick="${pickerName}.set(3,this)">3개월</button>
    <button type="button" class="btn btn-xs ${btnClass} btn-ghost" onclick="${pickerName}.set(6,this)">6개월</button>
    <button type="button" class="btn btn-xs ${btnClass} btn-ghost" onclick="${pickerName}.set('ytd',this)">올해</button>
    <button type="button" class="btn btn-xs ${btnClass} btn-ghost" onclick="${pickerName}.set(12,this)">1년</button>
    <button type="button" class="btn btn-xs ${btnClass} btn-ghost" onclick="${pickerName}.set(36,this)">3년</button>
    <button type="button" class="btn btn-xs ${btnClass} btn-ghost" onclick="${pickerName}.set(0,this)">전체</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-next${!canNext ? " btn-disabled opacity-40" : ""}" $unsafe{!canNext ? "disabled" : ""} onclick="${pickerName}.shift(1)">다음 ›</button>
    <button type="button" class="btn btn-xs btn-ghost date-range-next${!canNext ? " btn-disabled opacity-40" : ""}" $unsafe{!canNext ? "disabled" : ""} onclick="${pickerName}.jumpToEdge('end')">»</button>
    @if(startDate != null || endDate != null)
        <span class="text-xs text-base-content/40 ml-1">
            ${startDate != null ? startDate.toString() : "?"} ~ ${endDate != null ? endDate.toString() : "?"}
        </span>
    @endif
</div>
