@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String titleKey = "stock.error.fragment.title"
@param String descKey = "stock.error.fragment.desc"

<%-- 페이지 렌더 도중 백엔드 호출이 실패했을 때 쓰는 전체 화면.
     이게 없으면 공통 예외 처리기가 본문 없는 200 을 만들어 화면이 완전히 백지가 된다
     (실측: 백엔드 중지 시 매매내역/배당캘린더/월배당/실현손익/관리-월배당이 본문 0자). --%>
@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="w-full">
			<div class="alert alert-error" role="alert">
				<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
					<path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
				</svg>
				<div class="min-w-0">
					<div class="font-semibold">${MessageUtil.getMessage(titleKey)}</div>
					<div class="text-sm opacity-80 whitespace-normal">${MessageUtil.getMessage(descKey)}</div>
				</div>
			</div>
		</div>
	`
)
