@import io.github.luversof.boot.context.support.MessageUtil

@param boolean notFound = false

<%-- PoE API 호출 실패 공용 프래그먼트 — htmx 대상에 빈 응답이 스왑되면 "아무 일도 없는" 침묵 실패가 되므로,
     어떤 핸들러가 죽어도 최소한 이 알림은 보이게 한다(컨트롤러 @ExceptionHandler 가 렌더).
     없는 항목(404)과 일시 장애를 구분한다 — 삭제·개명된 slug 에 "잠시 후 다시 시도"를 권하면
     영영 안 되는 걸 계속 누르게 된다. --%>
@if(notFound)
	<div class="alert alert-warning text-sm">${MessageUtil.getMessage("poe.api.notfound")}</div>
@else
	<div class="alert alert-error text-sm">${MessageUtil.getMessage("poe.api.error")}</div>
@endif
