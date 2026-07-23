@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeGroups
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param int totalCount = 0
@param List<PoeGroups.Group> classGroups = java.util.List.of()
@param String initialQ = ""
@param String activeValue = "all"

!{
	String pageTitle = MessageUtil.getMessage("poe.items.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String searchPlaceholder = MessageUtil.getMessage("poe.items.search.placeholder");
	String classAllLabel = MessageUtil.getMessage("poe.uniques.category.all");
	// 탭 전환 진입 시 초기 필터를 그대로 로드(URL 인코딩 — itemClass 키/검색어에 공백 포함 가능)
	String initialUrl = "/poe/htmx/items?q=" + java.net.URLEncoder.encode(initialQ, java.nio.charset.StandardCharsets.UTF_8)
		+ "&itemClass=" + java.net.URLEncoder.encode(activeValue, java.nio.charset.StandardCharsets.UTF_8);
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
				<span class="badge badge-ghost">${patchLabel}</span>
			</div>

			@template.poe.itemTabs(activeTab = "items")

			<div class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<form id="poeItemFilter" class="flex flex-wrap gap-2 items-center"
				      hx-get="/poe/htmx/items"
				      hx-target="#poeItemList"
				      hx-swap="innerHTML"
				      hx-trigger="input changed delay:300ms, change"
				      data-url-sync data-url-map="itemClass:slot">
					<input type="search" name="q" value="${initialQ}" class="input input-sm input-bordered w-full sm:w-72" placeholder="${searchPlaceholder}" autocomplete="off" autofocus>
					<input type="hidden" name="itemClass" value="${activeValue}">
				</form>
				@template.poe.groupChips(groups = classGroups, field = "itemClass", target = "#poeItemFilter", groupLabelPrefix = "poe.itemgroup.", allLabel = classAllLabel, activeValue = activeValue)
			</div>

			<div id="poeItemList" hx-get="${initialUrl}" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg text-primary"></span>
				</div>
			</div>

			<div id="poeItemDetail"></div>
			<script src="/js/poe/filterUrl.js" defer></script>
		</div>
	`
)
