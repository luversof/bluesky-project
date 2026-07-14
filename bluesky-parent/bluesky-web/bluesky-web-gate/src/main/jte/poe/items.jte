@import java.util.Map
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param int totalCount = 0
@param Map<String, String> itemClasses = java.util.Map.of()

!{
	String pageTitle = MessageUtil.getMessage("poe.items.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String searchPlaceholder = MessageUtil.getMessage("poe.items.search.placeholder");
	String classAllLabel = MessageUtil.getMessage("poe.uniques.category.all");
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

			<form id="poeItemFilter" class="flex flex-wrap gap-2 items-center rounded-2xl bg-base-100 p-3 shadow-sm"
			      hx-get="/poe/htmx/items"
			      hx-target="#poeItemList"
			      hx-swap="innerHTML"
			      hx-trigger="input changed delay:300ms, change">
				<input type="search" name="q" class="input input-sm input-bordered w-full sm:w-72" placeholder="${searchPlaceholder}" autocomplete="off" autofocus>
				<select name="itemClass" class="select select-sm select-bordered">
					<option value="all">${classAllLabel}</option>
					@for(Map.Entry<String, String> itemClassEntry : itemClasses.entrySet())
						<option value="${itemClassEntry.getKey()}">${itemClassEntry.getValue()}</option>
					@endfor
				</select>
			</form>

			<div id="poeItemList" hx-get="/poe/htmx/items" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg text-primary"></span>
				</div>
			</div>

			<div id="poeItemDetail"></div>
		</div>
	`
)
