@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeModItemClass
@import net.luversof.web.gate.poe.dto.PoeModClass
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param List<PoeModItemClass> modClasses = java.util.List.of()
@param String activeClass = ""
@param java.util.List<net.luversof.web.gate.poe.dto.PoeAnointEntry> anoints = null
@param PoeModClass mods = null
@param net.luversof.web.gate.poe.dto.PoeEldritch eldritch = null
@param String activeInfluence = ""
@param java.util.List<net.luversof.web.gate.poe.dto.PoeEssenceEntry> essences = null
@param java.util.List<net.luversof.web.gate.poe.dto.PoeBenchEntry> bench = null

!{
	String pageTitle = MessageUtil.getMessage("layout.menu.poe.mods");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	boolean ko = PoeText.isKorean();
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
				<span class="text-sm text-base-content/50">${ko ? "일반 아이템 제작 모드 — 아이템 클래스별 접두/접미 티어" : "Craftable modifiers by item class"}</span>
			</div>

			<%-- 아이템 클래스 칩 — 30클래스라 평면 나열 대신 그룹(방어구/무기/장신구/플라스크·주얼) 행으로.
			     클릭 시 htmx 로 본문 교체 + URL ?itemClass= 동기화 --%>
			!{
				var chipGroups = new java.util.LinkedHashMap<String, java.util.List<PoeModItemClass>>();
				// 표시 순서 고정(선시드) — modClasses 는 알파벳순이라 그대로 두면 플라스크·주얼이 맨 위로 온다
				chipGroups.put(ko ? "방어구" : "Armour", new java.util.ArrayList<>());
				chipGroups.put(ko ? "무기" : "Weapons", new java.util.ArrayList<>());
				chipGroups.put(ko ? "장신구" : "Accessories", new java.util.ArrayList<>());
				chipGroups.put(ko ? "플라스크·주얼" : "Flasks · Jewels", new java.util.ArrayList<>());
				for (PoeModItemClass c : modClasses) {
					String id = c.itemClass();
					String g;
					if (id.contains("Flask") || id.contains("Jewel")) g = ko ? "플라스크·주얼" : "Flasks · Jewels";
					else if (java.util.Set.of("Body Armour", "Helmet", "Gloves", "Boots", "Shield").contains(id)) g = ko ? "방어구" : "Armour";
					else if (java.util.Set.of("Amulet", "Ring", "Belt", "Quiver").contains(id)) g = ko ? "장신구" : "Accessories";
					else g = ko ? "무기" : "Weapons";
					chipGroups.computeIfAbsent(g, k -> new java.util.ArrayList<>()).add(c);
				}
			}
			<div class="space-y-1.5 rounded-2xl bg-base-100 p-3 shadow-sm" data-mod-classchips>
				@for(var grp : chipGroups.entrySet())
					<div class="flex flex-wrap items-center gap-1.5">
						<span class="w-24 shrink-0 text-xs font-semibold text-base-content/50">${grp.getKey()}</span>
						@for(PoeModItemClass c : grp.getValue())
							<a class="poe-chip badge ${c.itemClass().equals(activeClass) ? "badge-primary" : "badge-ghost"} cursor-pointer"
							   href="/poe/mods?itemClass=${java.net.URLEncoder.encode(c.itemClass(), java.nio.charset.StandardCharsets.UTF_8)}"
							   hx-get="/poe/htmx/mods?itemClass=${java.net.URLEncoder.encode(c.itemClass(), java.nio.charset.StandardCharsets.UTF_8)}"
							   hx-target="#poeModBody"
							   hx-swap="innerHTML"
							   hx-push-url="/poe/mods?itemClass=${java.net.URLEncoder.encode(c.itemClass(), java.nio.charset.StandardCharsets.UTF_8)}"
							   data-chip-value="${c.itemClass()}">
								${ko ? c.nameKo() : c.name()}
								<span class="ml-1 text-[10px] opacity-60">${c.prefixes().size() + c.suffixes().size()}</span>
							</a>
						@endfor
					</div>
				@endfor
			</div>

			@template.poe.modFilterBar(scope = "#poeModBody", idPrefix = "poeMod")

			<div id="poeModBody">
				@template.poe.htmx.modClass(mods = mods, eldritch = eldritch, anoints = anoints, essences = essences, bench = bench)
			</div>
			<script>
				(function () {
					var bar = document.querySelector("[data-mod-classchips]");
					// htmx 는 본문만 교체하므로 활성 칩 하이라이트는 클릭 시 직접 옮긴다(전체 새로고침 땐 서버가 렌더)
					if (bar) {
						bar.addEventListener("click", function (e) {
							var chip = e.target.closest(".poe-chip");
							if (!chip || !bar.contains(chip)) return;
							bar.querySelectorAll(".poe-chip").forEach(function (c) {
								c.classList.toggle("badge-primary", c === chip);
								c.classList.toggle("badge-ghost", c !== chip);
							});
						});
					}
				})();
			</script>
		</div>
	`
)
