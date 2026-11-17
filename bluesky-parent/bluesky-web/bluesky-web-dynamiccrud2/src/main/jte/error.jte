<%-- Spring Boot 오류 화면(BasicErrorController 가 "error" 뷰로 부른다). 스택 트레이스는 보이지 않는다 --%>
@param Integer status = 500
@param String path = null

!{var message = status == 404 ? "찾는 화면이 없습니다. 주소를 확인하세요." : status == 403 ? "이 화면을 볼 권한이 없습니다." : status == 401 ? "로그인이 필요합니다." : "처리 중 오류가 났습니다. 잠시 뒤 다시 시도하세요.";}

<%-- 오류 화면 요청에는 세션이 안 붙어 로그인해 있어도 "로그인" 이 보이므로 사용자 영역을 숨긴다 --%>
@template._layout.layout(title = "오류 " + status, showUser = false, content = @`
	<div class="card max-w-xl grid gap-3">
		<p class="text-lg font-semibold">${status}</p>
		<p>${message}</p>
		@if(path != null)
			<p class="hint font-mono">${path}</p>
		@endif
		<div class="flex gap-2">
			<a class="btn btn-sm btn-primary" href="/">메뉴 목록</a>
			<button type="button" class="btn btn-sm btn-ghost" onclick="history.back()">뒤로</button>
		</div>
	</div>
`)
