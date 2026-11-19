<%-- Spring Boot 오류 화면(BasicErrorController 가 "error" 뷰로 부른다). 스택 트레이스는 보이지 않는다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param Integer status = 500
@param String path = null

!{var message = status == 404 ? t("error.404") : status == 403 ? t("error.403") : status == 401 ? t("error.401") : t("error.other");}

<%-- 오류 화면 요청에는 세션이 안 붙어 로그인해 있어도 "로그인" 이 보이므로 사용자 영역을 숨긴다 --%>
@template._layout.layout(title = t("error.title", String.valueOf(status)), showUser = false, content = @`
	<div class="card max-w-xl grid gap-3">
		<p class="text-lg font-semibold">${status}</p>
		<p>${message}</p>
		@if(path != null)
			<p class="hint font-mono">${path}</p>
		@endif
		<div class="flex gap-2">
			<a class="btn btn-sm btn-primary" href="/">${t("error.toMenus")}</a>
			<button type="button" class="btn btn-sm btn-ghost" onclick="history.back()">${t("error.back")}</button>
		</div>
	</div>
`)
