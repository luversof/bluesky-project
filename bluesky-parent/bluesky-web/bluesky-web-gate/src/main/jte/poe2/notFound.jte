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

<%-- PoE2 상세 slug 를 못 찾았을 때 — 404 상태 + 안내(빈 200 을 내지 않는다, PoE1 과 같은 규칙).
     문구는 PoE1 과 같은 종류별 키(10-02 C28: PoE1 "그런 고유 아이템을 찾을 수 없습니다" ↔ PoE2 "찾을 수 없습니다: slug" 로 갈려 있었다) --%>
!{ String notFoundKey = listUrl.startsWith("/poe2/uniques") ? "poe.notfound.unique" : listUrl.startsWith("/poe2/items") ? "poe.notfound.item" : "poe.notfound.gem"; }
@template._layout.poeLayout(
	title = MessageUtil.getMessage(notFoundKey),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			@template.poe2.header(title = MessageUtil.getMessage(notFoundKey), meta = meta)
			@template._components.ui.emptyState(
				boxed = true,
				message = MessageUtil.getMessage(notFoundKey),
				hint = slug + " — " + (PoeText.isKorean() ? "주소가 바뀌었거나 데이터 갱신으로 이름이 달라졌을 수 있습니다 — 목록에서 다시 찾아 보세요." : "The address may have changed or the data was updated — search the list again."),
				actionLabel = "← " + MessageUtil.getMessage(listUrl.startsWith("/poe2/uniques") ? "poe2.uniques.title" : listUrl.startsWith("/poe2/items") ? "poe2.items.title" : "poe2.gems.title"),
				actionHref = listUrl)
		</div>
	`)
