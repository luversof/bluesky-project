@import io.github.luversof.boot.context.support.MessageUtil

@param String message = null

<%-- PoE2 조각 요청 실패 — 자리에 안내만(화면 전체가 깨지지 않게). 원인은 게이트 로그에. --%>
<div class="alert alert-warning text-sm" role="alert">
	<span>${MessageUtil.getMessage("poe2.error")}</span>
</div>
