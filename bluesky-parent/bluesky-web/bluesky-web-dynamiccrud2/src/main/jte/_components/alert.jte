<%-- 안내 상자. message 가 없으면 아무것도 내지 않아 화면에서 @if 로 감쌀 필요가 없다.
     tone: error · warning(role=alert, 바로 읽힘) / info · success(role=status) --%>
@param String message
@param String tone = "error"
@param String cssClass = "mb-4"

@if(message != null && !message.isBlank())
	<div class="alert alert-${tone} ${cssClass}" role="${"info".equals(tone) || "success".equals(tone) ? "status" : "alert"}">${message}</div>
@endif
