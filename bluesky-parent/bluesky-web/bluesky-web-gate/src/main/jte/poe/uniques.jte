@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param int totalCount = 0
@param List<String> categories = java.util.List.of()

!{
	String pageTitle = MessageUtil.getMessage("poe.uniques.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String searchPlaceholder = MessageUtil.getMessage("poe.uniques.search.placeholder");
	String categoryAllLabel = MessageUtil.getMessage("poe.uniques.category.all");
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


			@template.poe.itemTabs(activeTab = "uniques")

			<div class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<form id="poeUniqueFilter" class="flex flex-wrap gap-2 items-center"
				      hx-get="/poe/htmx/uniques"
				      hx-target="#poeUniqueList"
				      hx-swap="innerHTML"
				      hx-trigger="input changed delay:300ms, change">
					<input type="search" name="q" class="input input-sm input-bordered w-full sm:w-72" placeholder="${searchPlaceholder}" autocomplete="off" autofocus>
					<input type="hidden" name="category" value="all">
				</form>
				<div class="flex flex-wrap gap-1.5" data-chip-group data-chip-target="#poeUniqueFilter" data-chip-field="category">
					<button type="button" class="poe-chip poe-chip-active" data-chip-value="all">${categoryAllLabel}</button>
					@for(String category : categories)
						<button type="button" class="poe-chip" data-chip-value="${category}">${MessageUtil.getMessage("poe.uniques.category." + category)}</button>
					@endfor
				</div>
			</div>

			<div id="poeUniqueList" hx-get="/poe/htmx/uniques" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg text-primary"></span>
				</div>
			</div>

			<div id="poeUniqueDetail"></div>
		</div>
	`
)
