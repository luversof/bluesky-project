@import io.github.luversof.boot.context.support.MessageUtil

<%-- PoE API 호출 실패 공용 프래그먼트 — htmx 대상에 빈 응답이 스왑되면 "아무 일도 없는" 침묵 실패가 되므로,
     어떤 핸들러가 죽어도 최소한 이 알림은 보이게 한다(컨트롤러 @ExceptionHandler 가 렌더). --%>
<div class="alert alert-error text-sm">${MessageUtil.getMessage("poe.api.error")}</div>
