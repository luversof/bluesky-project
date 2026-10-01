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
@param String activeKind = "all"
@param String activeClass = "all"

!{
	String pageTitle = MessageUtil.getMessage("poe2.augments.title");
	java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
	String initialUrl = "/poe2/htmx/augments?q=" + java.net.URLEncoder.encode(initialQ, utf8)
		+ "&kind=" + java.net.URLEncoder.encode(activeKind, utf8)
		+ "&itemClass=" + java.net.URLEncoder.encode(activeClass, utf8);
	// 장착 대상 칩 — 증강물 홈이 있는 장비 클래스만(플라스크 · 주얼 제외)
	List<Poe2.ItemClass> socketable = classes.stream().filter(c -> !"flask".equals(c.category()) && !"jewel".equals(c.category())).toList();
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
			<div class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<form id="poe2AugmentFilter" class="flex flex-wrap gap-2 items-center"
				      hx-get="/poe2/htmx/augments" hx-target="#poe2AugmentList" hx-swap="innerHTML"
				      hx-trigger="input changed delay:300ms, change" data-url-sync="true">
					<input type="search" name="q" value="${initialQ}" class="input input-sm input-bordered w-full sm:w-72"
					       placeholder="${MessageUtil.getMessage("poe2.search.placeholder")}" aria-label="${MessageUtil.getMessage("poe2.search.placeholder")}" autocomplete="off">
					<input type="hidden" name="kind" value="${activeKind}">
					<input type="hidden" name="itemClass" value="${activeClass}">
				</form>
				<div class="flex flex-wrap items-center gap-1.5" data-chip-group data-chip-target="#poe2AugmentFilter" data-chip-field="kind">
					<button type="button" class="poe-chip ${"all".equals(activeKind) ? "poe-chip-active" : ""}" data-chip-value="all">${MessageUtil.getMessage("poe2.all")}</button>
					@for(String k : List.of("rune", "soulcore", "idol", "abyssaleye", "mist"))
						<button type="button" class="poe-chip ${k.equals(activeKind) ? "poe-chip-active" : ""}" data-chip-value="${k}">${MessageUtil.getMessage("poe2.augments.kind." + k)}</button>
					@endfor
				</div>
				<div class="flex flex-wrap items-center gap-1.5 border-t border-base-200 pt-2" data-chip-group data-chip-target="#poe2AugmentFilter" data-chip-field="itemClass">
					<span class="text-xs font-semibold text-base-content/50 mr-1" title="${MessageUtil.getMessage("poe2.augments.class.hint")}">${MessageUtil.getMessage("poe2.augments.target")}</span>
					<button type="button" class="poe-chip ${"all".equals(activeClass) ? "poe-chip-active" : ""}" data-chip-value="all">${MessageUtil.getMessage("poe2.all")}</button>
					@for(Poe2.ItemClass c : socketable)
						<button type="button" class="poe-chip ${c.key().equals(activeClass) ? "poe-chip-active" : ""}" data-chip-value="${c.key()}">${PoeText.name(c.ko(), c.key())}</button>
					@endfor
				</div>
			</div>
			<div id="poe2AugmentList" hx-get="${initialUrl}" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10"><span class="loading loading-spinner loading-lg text-primary"></span></div>
			</div>
			<script src="/js/poe/filterUrl.js" defer></script>
		</div>
	`
)
