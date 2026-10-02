@import io.github.luversof.boot.context.support.MessageUtil

@param String message = null
@param boolean notFound = false

<%-- PoE2 조각 요청 실패 — 자리에 안내만(화면 전체가 깨지지 않게). 원인은 게이트 로그에. --%>
@if(notFound)
	<div class="alert alert-warning text-sm" role="alert" data-error-kind="notfound"><span>${MessageUtil.getMessage("poe.api.notfound")}</span></div>
@else
	<div class="alert alert-warning text-sm" role="alert" data-error-kind="error"><span>${MessageUtil.getMessage("poe2.error")}</span></div>
@endif
