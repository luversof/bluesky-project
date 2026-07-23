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
@param PoeModClass mods = null
@param net.luversof.web.gate.poe.dto.PoeEldritch eldritch = null
@param String activeInfluence = ""

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

			<%-- 아이템 클래스 칩 — 클릭 시 htmx 로 본문 교체 + URL ?itemClass= 동기화 --%>
			<div class="flex flex-wrap gap-1.5 rounded-2xl bg-base-100 p-3 shadow-sm" data-mod-classchips>
				@for(PoeModItemClass c : modClasses)
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

			@template.poe.modFilterBar(scope = "#poeModBody", idPrefix = "poeMod")

			<div id="poeModBody">
				@template.poe.htmx.modClass(mods = mods, eldritch = eldritch)
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
