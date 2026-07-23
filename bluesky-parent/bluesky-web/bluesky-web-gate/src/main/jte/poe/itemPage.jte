@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.dto.ModFamily
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param PoeBaseItem item
@param java.util.List<ModFamily> modFamilies = null
@param net.luversof.web.gate.poe.dto.PoeModClass fullMods = null
@param net.luversof.web.gate.poe.dto.PoeEldritch eldritch = null
@param String slug = ""

!{
	String displayName = PoeText.name(item.nameKo(), item.name());
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	boolean ko = PoeText.isKorean();
}

@template._layout.poeLayout(
	title = displayName,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<a href="/poe/items" class="link link-hover text-sm text-base-content/60">← ${MessageUtil.getMessage("poe.items.title")}</a>
				<h1 class="text-2xl font-bold">${displayName}</h1>
				@if(ko)<span class="text-base-content/40 text-sm">${item.name()}</span>@endif
				<span class="badge badge-ghost">${patchLabel}</span>
			</div>

			<%-- 상세 레이어(툴팁+큐레이션 티어표)를 그대로 바닥에 --%>
			@template.poe.htmx.itemDetail(item = item, modFamilies = modFamilies)

			<%-- 전체 제작 모드 풀(poedb식) — 장비 클래스만. 큐레이션(위 툴팁)보다 훨씬 많아 별도 섹션으로 --%>
			@if(fullMods != null)
				<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
					<div class="mb-2 flex flex-wrap items-baseline gap-2">
						<h2 class="font-bold">${ko ? "전체 제작 모드" : "All craftable modifiers"}</h2>
						!{
							// 이 베이스의 속성 변형(힘/민첩/지능…) — 변형마다 붙는 모드가 다르므로 무엇 기준인지 밝힌다
							String variantLabel = "";
							for (var v : fullMods.variants()) {
								if (v.key().equals(fullMods.variant())) variantLabel = ko ? v.nameKo() : v.name();
							}
						}
						<span class="text-sm text-base-content/50">${ko ? PoeText.name(fullMods.nameKo(), fullMods.name()) + (variantLabel.isEmpty() ? "" : " · " + variantLabel + " 변형") + " · 접두 " + fullMods.prefixes().size() + " · 접미 " + fullMods.suffixes().size() : fullMods.name() + (variantLabel.isEmpty() ? "" : " · " + variantLabel) + " · " + fullMods.prefixes().size() + " prefixes · " + fullMods.suffixes().size() + " suffixes"}</span>
						<a class="link link-hover text-sm text-primary ml-auto" href="/poe/mods?itemClass=${java.net.URLEncoder.encode(fullMods.itemClass(), java.nio.charset.StandardCharsets.UTF_8)}">${ko ? "모드 페이지에서 보기 →" : "Open in Modifiers →"}</a>
					</div>
					@template.poe.modFilterBar(scope = "#poeItemModBody", idPrefix = "poeItemMod")
					<div id="poeItemModBody" class="mt-2">
						@template.poe.htmx.modClass(mods = fullMods, showVariants = false, influenceHref = "/poe/items/" + slug + "?")
					</div>
				</div>
			@endif

			<%-- 엘드리치 임플리싯(총주교/포식자) — 부여 가능한 슬롯만(방어구·목걸이). 화폐로 붙이는 임플리싯이라 제작 모드와 별도 --%>
			@if(eldritch != null)
				<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
					<div class="mb-2 flex flex-wrap items-baseline gap-2">
						<h2 class="font-bold">${ko ? "엘드리치 임플리싯" : "Eldritch implicits"}</h2>
						<span class="text-sm text-base-content/50">${ko ? "총주교·포식자 화폐로 부여 — 팩션당 1개씩 최대 2개" : "Exarch + Eater — up to one each"}</span>
					</div>
					@template.poe.htmx.eldritchPools(eldritch = eldritch, ko = ko)
				</div>
			@endif
		</div>
	`
)
