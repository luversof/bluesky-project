@import net.luversof.web.gate.util.CspNonceHolder
@import java.time.LocalDate
@import java.time.Instant
@import io.github.luversof.boot.context.support.MessageUtil

@param String pickerVar
@param String formId
@param String fragmentId
@param String contentUrl
@param String idPrefix
@param String extraName
@param String extraValue
@param LocalDate startLocal = null
@param LocalDate endLocal = null
@param Instant startInstant = null
@param Instant endInstant = null
@param String timeZone = null
@param String rangeMode = null
@param String dataFirstDate = ""
@param boolean canPrev = true
@param boolean canNext = true

!{
    String periodLabel = MessageUtil.getMessage("common.label.period");
    String searchLabel = MessageUtil.getMessage("common.button.search");
    String btnClass = idPrefix + "-range-btn";
}

<%-- 내비바의 endDate 는 '배타적' 경계다(컴포넌트가 배지에 찍을 때 하루를 뺀다).
     여기 endLocal 은 컨트롤러가 이미 minusDays(1) 로 만든 '포함' 마지막 날이라 그대로 넘기면 하루가 더 빠진다.
     실측: 8/1~8/31 구간 배지가 8-30, 하루짜리 구간은 8-01 ~ 07-31 로 역전됐다. --%>
!{java.time.LocalDate navBarEndExclusive = endLocal != null ? endLocal.plusDays(1) : null;}
@template.stock.htmx.fragments.dateRangeNavBar(
    pickerName = pickerVar,
    btnClass = btnClass,
    canPrev = canPrev,
    canNext = canNext,
    rangeMode = rangeMode,
    startDate = startLocal,
    endDate = navBarEndExclusive,
    showRangeText = true
)

<form id="${formId}" class="mb-4 rounded-2xl bg-base-100 p-3 shadow-sm flex flex-wrap items-end gap-2"
      hx-get="${contentUrl}"
      hx-target="#${fragmentId}"
      hx-select="#${fragmentId}"
      hx-swap="outerHTML"
      hx-trigger="submit">
    <input type="hidden" name="${extraName}" value="${extraValue}">
    <div class="form-control">
        <label class="label p-1" id="${idPrefix}PeriodLabel"><span class="label-text text-xs">${periodLabel}</span></label>
        <div class="join" role="group" aria-labelledby="${idPrefix}PeriodLabel">
            <input type="date" name="uiStartDate" id="${idPrefix}StartDateInput" class="input input-sm input-bordered join-item" value="${startLocal != null ? startLocal.toString() : ""}">
            <input type="date" name="uiEndDate" id="${idPrefix}EndDateInput" class="input input-sm input-bordered join-item" value="${endLocal != null ? endLocal.toString() : ""}">
        </div>
    </div>
    <button type="button" class="btn btn-sm btn-primary"
        hx-get="${contentUrl}"
        hx-target="#${fragmentId}"
        hx-select="#${fragmentId}"
        hx-swap="outerHTML"
        hx-include="#${formId}">${searchLabel}</button>
    <input type="hidden" name="startDate" id="${idPrefix}StartInstantInput" value="${startInstant != null ? startInstant.toString() : ""}">
    <input type="hidden" name="endDate" id="${idPrefix}EndInstantInput" value="${endInstant != null ? endInstant.toString() : ""}">
    <input type="hidden" name="timeZone" id="${idPrefix}TimeZoneInput" value="${timeZone != null ? timeZone : ""}">
    <input type="hidden" name="rangeMode" id="${idPrefix}RangeModeInput" value="${rangeMode != null ? rangeMode : ""}">
</form>

<script nonce="${CspNonceHolder.getNonce()}">
(function() {
    if (typeof DateRangePicker === 'undefined') return;
    var picker = DateRangePicker.create({
        formId:         '${formId}',
        startId:        '${idPrefix}StartDateInput',
        endId:          '${idPrefix}EndDateInput',
        instantStartId: '${idPrefix}StartInstantInput',
        instantEndId:   '${idPrefix}EndInstantInput',
        timeZoneId:     '${idPrefix}TimeZoneInput',
        rangeModeId:    '${idPrefix}RangeModeInput',
        btnClass:       '${idPrefix}-range-btn',
        minDate:        '${dataFirstDate != null ? dataFirstDate : ""}',
        globalKey:      'globalDateRange',
        rootSelector:   '#${fragmentId}'
    });
    window.${pickerVar} = picker;
})();
</script>
