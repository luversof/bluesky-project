@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param int totalCount = 0

!{
	String pageTitle = MessageUtil.getMessage("poe.gems.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String searchPlaceholder = MessageUtil.getMessage("poe.gems.search.placeholder");
	String typeAllLabel = MessageUtil.getMessage("poe.gems.type.all");
	String typeActiveLabel = MessageUtil.getMessage("poe.gems.type.active");
	String typeSupportLabel = MessageUtil.getMessage("poe.gems.type.support");
	String colorAllLabel = MessageUtil.getMessage("poe.gems.color.all");
	String colorRedLabel = MessageUtil.getMessage("poe.gems.color.red");
	String colorGreenLabel = MessageUtil.getMessage("poe.gems.color.green");
	String colorBlueLabel = MessageUtil.getMessage("poe.gems.color.blue");
	String colorWhiteLabel = MessageUtil.getMessage("poe.gems.color.white");
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


			<form id="poeGemFilter" class="flex flex-wrap gap-2 items-center rounded-2xl bg-base-100 p-3 shadow-sm"
			      hx-get="/poe/htmx/gems"
			      hx-target="#poeGemList"
			      hx-swap="innerHTML"
			      hx-trigger="input changed delay:300ms, change">
				<input type="search" name="q" class="input input-sm input-bordered w-full sm:w-72" placeholder="${searchPlaceholder}" autocomplete="off" autofocus>
				<select name="type" class="select select-sm select-bordered">
					<option value="all">${typeAllLabel}</option>
					<option value="active">${typeActiveLabel}</option>
					<option value="support">${typeSupportLabel}</option>
				</select>
				<select name="color" class="select select-sm select-bordered">
					<option value="all">${colorAllLabel}</option>
					<option value="red">${colorRedLabel}</option>
					<option value="green">${colorGreenLabel}</option>
					<option value="blue">${colorBlueLabel}</option>
					<option value="white">${colorWhiteLabel}</option>
				</select>
			</form>

			<div id="poeGemList" hx-get="/poe/htmx/gems" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg text-primary"></span>
				</div>
			</div>

			<!-- 젬 상세 레이어 컨테이너 (htmx 로 채워짐) -->
			<div id="poeGemDetail"></div>
		</div>
	`
)
