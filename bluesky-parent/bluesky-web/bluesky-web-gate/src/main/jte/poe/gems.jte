@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeGroups
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param int totalCount = 0
@param List<PoeGroups.Group> tagGroups = java.util.List.of()
@param String initialQ = ""
@param String initialType = "all"
@param String initialColor = "all"
@param String activeTag = "all"

!{
	String pageTitle = MessageUtil.getMessage("poe.gems.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String searchPlaceholder = MessageUtil.getMessage("poe.gems.search.placeholder");
	String tagAllLabel = MessageUtil.getMessage("poe.gems.tag.all");
	// URL 쿼리(q/type/color/tag)로 진입 시 초기 필터를 그대로 로드
	String enc = java.nio.charset.StandardCharsets.UTF_8.name();
	String initialUrl = "/poe/htmx/gems?q=" + java.net.URLEncoder.encode(initialQ, java.nio.charset.StandardCharsets.UTF_8)
		+ "&type=" + initialType + "&color=" + initialColor
		+ "&tag=" + java.net.URLEncoder.encode(activeTag, java.nio.charset.StandardCharsets.UTF_8);
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


			<div class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<form id="poeGemFilter" class="flex flex-wrap gap-2 items-center"
				      hx-get="/poe/htmx/gems"
				      hx-target="#poeGemList"
				      hx-swap="innerHTML"
				      hx-trigger="input changed delay:300ms, change"
			      data-url-sync="true">
					<input type="search" name="q" value="${initialQ}" class="input input-sm input-bordered w-full sm:w-72" placeholder="${searchPlaceholder}" autocomplete="off" autofocus>
					<select name="type" class="select select-sm select-bordered">
						<option value="all" selected="${"all".equals(initialType)}">${typeAllLabel}</option>
						<option value="active" selected="${"active".equals(initialType)}">${typeActiveLabel}</option>
						<option value="support" selected="${"support".equals(initialType)}">${typeSupportLabel}</option>
					</select>
					<select name="color" class="select select-sm select-bordered">
						<option value="all" selected="${"all".equals(initialColor)}">${colorAllLabel}</option>
						<option value="red" selected="${"red".equals(initialColor)}">${colorRedLabel}</option>
						<option value="green" selected="${"green".equals(initialColor)}">${colorGreenLabel}</option>
						<option value="blue" selected="${"blue".equals(initialColor)}">${colorBlueLabel}</option>
						<option value="white" selected="${"white".equals(initialColor)}">${colorWhiteLabel}</option>
					</select>
					<input type="hidden" name="tag" value="${activeTag}">
				</form>
				@template.poe.groupChips(groups = tagGroups, field = "tag", target = "#poeGemFilter", groupLabelPrefix = "poe.gemgroup.", allLabel = tagAllLabel, activeValue = activeTag)
			</div>

			<div id="poeGemList" hx-get="${initialUrl}" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg text-primary"></span>
				</div>
			</div>

			<!-- 젬 상세 레이어 컨테이너 (htmx 로 채워짐) -->
			<div id="poeGemDetail"></div>
			<script src="/js/poe/filterUrl.js" defer></script>
		</div>
	`
)
