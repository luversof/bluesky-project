@import io.github.luversof.boot.context.support.MessageUtil

@param String error
@param String loginUrl = ""
@param String retryUrl = ""
<%-- 세션이 풀렸으면 로그인으로 가는 링크, 백엔드 조회가 실패했으면 그 자리에서 다시 부르는 버튼. 둘 다 비면 문구만. --%>

<div class="alert alert-error" role="alert" data-load-error>
	<svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
		<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
			d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
	</svg>
	<span class="min-w-0">${error != null ? error : MessageUtil.getMessage("common.message.error")}</span>
	@if(loginUrl != null && !loginUrl.isEmpty())
		<a href="${loginUrl}" class="btn btn-sm btn-outline shrink-0">${MessageUtil.getMessage("layout.navbar.login")}</a>
	@endif
	@if(retryUrl != null && !retryUrl.isEmpty())
		<button type="button" class="btn btn-sm btn-outline shrink-0" hx-get="${retryUrl}" hx-target="closest [data-load-error]" hx-swap="outerHTML">${MessageUtil.getMessage("common.button.retry")}</button>
	@endif
</div>