@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param List<Poe2.ItemClass> classes = java.util.List.of()
@param String initialQ = ""
@param String activeClass = "all"

!{
	String pageTitle = MessageUtil.getMessage("poe2.uniques.title");
	java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
	String initialUrl = "/poe2/htmx/uniques?q=" + java.net.URLEncoder.encode(initialQ, utf8) + "&itemClass=" + java.net.URLEncoder.encode(activeClass, utf8);
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			@template.poe2.header(title = pageTitle, meta = meta)
			@template.poe2.itemTabs(active = "uniques")
			<div class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<form id="poe2UniqueFilter" class="flex flex-wrap gap-2 items-center"
				      hx-get="/poe2/htmx/uniques" hx-target="#poe2UniqueList" hx-swap="innerHTML"
				      hx-trigger="input changed delay:300ms, change" data-url-sync="true">
					<input type="search" name="q" value="${initialQ}" autofocus class="input input-sm input-bordered w-full sm:w-72"
					       placeholder="${MessageUtil.getMessage("poe.uniques.search.placeholder")}" aria-label="${MessageUtil.getMessage("poe2.search.placeholder")}" autocomplete="off">
					<input type="hidden" name="itemClass" value="${activeClass}">
				</form>
				<%-- 분류별 줄 + 줄 이름 — PoE1 고유 목록과 같은 모양(10-01: 한 줄에 전부 늘어놓았다) --%>
				@template.poe2.classChips(classes = classes, target = "#poe2UniqueFilter", activeClass = activeClass)
			</div>
			<div id="poe2UniqueList" hx-get="${initialUrl}" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10"><span class="loading loading-spinner loading-lg text-primary"></span></div>
			</div>
			<script src="/js/poe/filterUrl.js" defer></script>
		</div>
	`
)
