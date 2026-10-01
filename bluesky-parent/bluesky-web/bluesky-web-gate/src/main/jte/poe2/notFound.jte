@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param String slug = ""
@param String listUrl = "/poe2"

<%-- PoE2 상세 slug 를 못 찾았을 때 — 404 상태 + 안내(빈 200 을 내지 않는다, PoE1 과 같은 규칙). --%>
@template._layout.poeLayout(
	title = java.text.MessageFormat.format(MessageUtil.getMessage("poe2.notfound"), slug),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			@template.poe2.header(title = java.text.MessageFormat.format(MessageUtil.getMessage("poe2.notfound"), slug), meta = meta)
			@template._components.ui.emptyState(
				boxed = true,
				message = java.text.MessageFormat.format(MessageUtil.getMessage("poe2.notfound"), slug),
				actionLabel = MessageUtil.getMessage("poe2.notfound.back"),
				actionHref = listUrl)
		</div>
	`)
