@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.PoeText

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
				hint = PoeText.isKorean() ? "주소가 바뀌었거나 데이터 갱신으로 이름이 달라졌을 수 있습니다 — 목록에서 다시 찾아 보세요." : "The address may have changed or the data was updated — search the list again.",
				actionLabel = "← " + MessageUtil.getMessage(listUrl.startsWith("/poe2/uniques") ? "poe2.uniques.title" : listUrl.startsWith("/poe2/items") ? "poe2.items.title" : "poe2.gems.title"),
				actionHref = listUrl)
		</div>
	`)
