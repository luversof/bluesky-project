@import io.github.luversof.boot.context.support.MessageUtil

@param String error
@param String loginUrl = ""
<%-- 세션이 풀렸으면 로그인으로 가는 링크, 없으면 문구만. 백엔드 조회 실패는 이 조각이 아니라 공통 예외 처리기(StockHtmxErrorResolver)의 loadError 조각이
     그리고, GET 조회면 거기에 재시도 버튼이 붙는다(LoadErrorRetryTest). 2026-10-22 까지 여기에도 재시도 버튼이 있었지만 그 값을 채우던
     remoteFailureView 를 부르는 곳이 2026-09-09 부터 없었다. --%>

<div class="alert alert-error" role="alert" data-load-error>
	<svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
		<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
			d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
	</svg>
	<span class="min-w-0">${error != null ? error : MessageUtil.getMessage("common.message.error")}</span>
	@if(loginUrl != null && !loginUrl.isEmpty())
		<a href="${loginUrl}" class="btn btn-sm btn-outline shrink-0">${MessageUtil.getMessage("layout.navbar.login")}</a>
	@endif
</div>