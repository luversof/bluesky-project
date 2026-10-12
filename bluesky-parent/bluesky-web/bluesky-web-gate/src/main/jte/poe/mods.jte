@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeModItemClass
@import net.luversof.web.gate.poe.dto.PoeModClass
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

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
	String pageTitle = MessageUtil.getMessage("poe.mods.title");
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
				<span class="text-sm text-base-content/60">${MessageUtil.getMessage("poe.mods.desc")}</span>
			</div>

			<%-- 아이템 클래스 칩 — 분류별 줄 + 줄 이름. PoE2 모드 화면(poe2/mods.jte)과 같은 묶음 · 순서 · 칩 모양(10-12 C272).
			     클릭 시 htmx 로 본문 교체 + URL ?itemClass= 동기화 --%>
			<nav class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm" aria-label="${MessageUtil.getMessage("poe.mods.title")}" data-mod-classchips>
				@for(String cat : net.luversof.web.gate.poe.controller.PoeViewController.MOD_CATEGORIES)
					!{ List<PoeModItemClass> inCat = modClasses.stream().filter(c -> cat.equals(net.luversof.web.gate.poe.controller.PoeViewController.modCategory(c.itemClass()))).toList(); }
					@if(!inCat.isEmpty())
						<div class="flex flex-wrap items-center gap-1.5" data-chip-row="${cat}">
							<span class="w-16 shrink-0 text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.itemgroup." + cat)}</span>
							@for(PoeModItemClass c : inCat)
								!{ boolean on = c.itemClass().equals(activeClass); }
								<a class="poe-chip ${on ? "poe-chip-active" : ""}"
								   href="/poe/mods?itemClass=${java.net.URLEncoder.encode(c.itemClass(), java.nio.charset.StandardCharsets.UTF_8)}"
								   hx-get="/poe/htmx/mods?itemClass=${java.net.URLEncoder.encode(c.itemClass(), java.nio.charset.StandardCharsets.UTF_8)}"
								   hx-target="#poeModBody"
								   hx-swap="innerHTML"
								   hx-push-url="/poe/mods?itemClass=${java.net.URLEncoder.encode(c.itemClass(), java.nio.charset.StandardCharsets.UTF_8)}"
								   aria-current="${on ? "page" : null}"
								   data-chip-value="${c.itemClass()}">${ko ? c.nameKo() : c.name()}<span class="ml-1 text-[10px] opacity-60">${c.prefixes().size() + c.suffixes().size()}</span></a>
							@endfor
						</div>
					@endif
				@endfor
			</nav>

			@template.poe.modFilterBar(scope = "#poeModBody", idPrefix = "poeMod")

			<div id="poeModBody">
				@template.poe.htmx.modClass(mods = mods, eldritch = eldritch, anoints = anoints, essences = essences, bench = bench)
			</div>
			<script nonce="${CspNonceHolder.getNonce()}">
				(function () {
					var bar = document.querySelector("[data-mod-classchips]");
					// htmx 는 본문만 교체하므로 활성 칩 하이라이트는 클릭 시 직접 옮긴다(전체 새로고침 땐 서버가 렌더)
					if (bar) {
						bar.addEventListener("click", function (e) {
							var chip = e.target.closest(".poe-chip");
							if (!chip || !bar.contains(chip)) return;
							bar.querySelectorAll(".poe-chip").forEach(function (c) {
								c.classList.toggle("poe-chip-active", c === chip);
								if (c === chip) c.setAttribute("aria-current", "page");
								else c.removeAttribute("aria-current");
							});
						});
					}
				})();
			</script>
		</div>
	`
)
