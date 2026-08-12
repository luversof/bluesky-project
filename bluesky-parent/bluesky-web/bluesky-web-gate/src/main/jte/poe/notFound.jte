@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String notFoundSlug = ""
@param String notFoundMessageKey = "poe.notfound.item"
@param String notFoundListUrl = "/poe"
@param String notFoundListLabelKey = "layout.menu.poe.gems"

<%-- 상세 slug 를 못 찾았을 때 — 이전엔 200 + 빈 본문이라 사용자가 백지를 봤다(젬·고유·일반 3종 공통). --%>
@template._layout.poeLayout(
	title = MessageUtil.getMessage(notFoundMessageKey),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<h1 class="text-2xl font-bold">${MessageUtil.getMessage(notFoundMessageKey)}</h1>
			@template._components.ui.emptyState(
				boxed = true,
				message = MessageUtil.getMessage(notFoundMessageKey),
				hint = notFoundSlug,
				actionLabel = MessageUtil.getMessage(notFoundListLabelKey),
				actionHref = notFoundListUrl)
		</div>
	`)
