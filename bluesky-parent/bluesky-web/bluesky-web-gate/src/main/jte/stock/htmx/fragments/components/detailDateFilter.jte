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
@param java.time.LocalDate coveredStartDate = null
@param java.time.LocalDate coveredEndDate = null
@param String dataFirstDate = ""
@param boolean canPrev = true
@param boolean canNext = true

!{
	String periodLabel = MessageUtil.getMessage("common.label.period");
	String searchLabel = MessageUtil.getMessage("common.button.search");
	String startDateLabel = MessageUtil.getMessage("common.label.start.date");
	String endDateLabel = MessageUtil.getMessage("common.label.end.date");
	String btnClass = idPrefix + "-range-btn";
}

<%-- 내비바의 endDate 는 '배타적' 경계다(컴포넌트가 배지에 찍을 때 하루를 뺀다).
	 여기 endLocal 은 컨트롤러가 이미 minusDays(1) 로 만든 '포함' 마지막 날이라 그대로 넘기면 하루가 더 빠진다.
	 실측: 8/1~8/31 구간 배지가 8-30, 하루짜리 구간은 8-01 ~ 07-31 로 역전됐다. --%>
!{java.time.LocalDate navBarEndExclusive = endLocal != null ? endLocal.plusDays(1) : null;}
<%-- 기간을 지정하지 않고 들어오면 화면은 실제로 전 기간을 보여 주는데, rangeMode 가 비어 있어 '전체' 버튼이
	 눌린 것으로 보이지 않았다 - 실측 2026-09-11: 같은 종목 상세가 ?rangeMode=all 로 들어오면 '전체' 강조,
	 파라미터 없이 들어오면 같은 데이터에 아무 버튼도 강조되지 않았다(배지는 둘 다 "전체 · 2020-03-23 ~ 2026-09-11").
	 강조만 맞추고 숨은 입력(rangeMode)은 그대로 비워 둔다 - 여기서 mode 를 채우면 첫 방문에 '전체' 가 전역 기간으로
	 저장돼(firstVisitRange) 매매·배당 화면의 기본값까지 올해에서 전체로 바뀐다. --%>
!{String navRangeMode = (rangeMode == null || rangeMode.isBlank()) && startLocal == null && endLocal == null ? "all" : rangeMode;}
@template.stock.htmx.fragments.dateRangeNavBar(
	pickerName = pickerVar,
	btnClass = btnClass,
	canPrev = canPrev,
	canNext = canNext,
	rangeMode = navRangeMode,
	startDate = startLocal,
	endDate = navBarEndExclusive,
	showRangeText = true,
	coveredStartDate = coveredStartDate,
	coveredEndDate = coveredEndDate
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
		<div class="join flex-wrap" role="group" aria-labelledby="${idPrefix}PeriodLabel">
			<input type="date" name="uiStartDate" id="${idPrefix}StartDateInput" aria-label="${startDateLabel}" class="input input-sm input-bordered join-item" value="${startLocal != null ? startLocal.toString() : ""}">
			<input type="date" name="uiEndDate" id="${idPrefix}EndDateInput" aria-label="${endDateLabel}" class="input input-sm input-bordered join-item" value="${endLocal != null ? endLocal.toString() : ""}">
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
