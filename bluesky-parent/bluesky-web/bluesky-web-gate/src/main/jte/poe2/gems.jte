@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param List<Poe2.TagGroup> tagGroups = java.util.List.of()
@param String initialQ = ""
@param String initialKind = "all"
@param String initialColor = "all"
@param String activeTag = "all"

!{
	String pageTitle = MessageUtil.getMessage("poe2.gems.title");
	java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
	String initialUrl = "/poe2/htmx/gems?q=" + java.net.URLEncoder.encode(initialQ, utf8)
		+ "&kind=" + java.net.URLEncoder.encode(initialKind, utf8)
		+ "&color=" + java.net.URLEncoder.encode(initialColor, utf8)
		+ "&tag=" + java.net.URLEncoder.encode(activeTag, utf8);
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
				<form id="poe2GemFilter" class="flex flex-wrap gap-2 items-center"
				      hx-get="/poe2/htmx/gems" hx-target="#poe2GemList" hx-swap="innerHTML"
				      hx-trigger="input changed delay:300ms, change" data-url-sync="true">
					<input type="search" name="q" value="${initialQ}" autofocus class="input input-sm input-bordered w-full sm:w-72"
					       placeholder="${MessageUtil.getMessage("poe.gems.search.placeholder")}" aria-label="${MessageUtil.getMessage("poe2.search.placeholder")}" autocomplete="off">
					<select name="kind" class="select select-sm select-bordered" aria-label="kind">
						<option value="all" selected="${"all".equals(initialKind)}">${MessageUtil.getMessage("poe2.all")}</option>
						@for(String k : List.of("skill", "support", "meta"))
							<option value="${k}" selected="${k.equals(initialKind)}">${MessageUtil.getMessage("poe2.gems.kind." + k)}</option>
						@endfor
					</select>
					<select name="color" class="select select-sm select-bordered" aria-label="color">
						<option value="all" selected="${"all".equals(initialColor)}">${MessageUtil.getMessage("poe2.all")}</option>
						@for(String c : List.of("str", "dex", "int", "white"))
							<option value="${c}" selected="${c.equals(initialColor)}">${MessageUtil.getMessage("poe2.gems.color." + c)}</option>
						@endfor
					</select>
					<input type="hidden" name="tag" value="${activeTag}">
				</form>
				<%-- 태그 칩 — PoE1 젬 화면(poe/groupChips.jte)과 같은 묶음별 줄 + 줄 이름(10-01: 예전엔 많이 쓰인 30개만 한 줄, 23개는 칩이 없었다) --%>
				<div class="space-y-1.5" data-chip-group data-chip-target="#poe2GemFilter" data-chip-field="tag">
					<div class="flex flex-wrap items-center gap-1.5">
						<span class="w-16 shrink-0"></span>
						<button type="button" class="poe-chip ${"all".equals(activeTag) ? "poe-chip-active" : ""}" data-chip-value="all">${MessageUtil.getMessage("poe.gems.tag.all")}</button>
					</div>
					@for(Poe2.TagGroup g : tagGroups)
						<div class="flex flex-wrap items-center gap-1.5" data-chip-row="${g.key()}">
							<span class="w-16 shrink-0 text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.gemgroup." + g.key())}</span>
							@for(Poe2.ModLine t : g.tags())
								<button type="button" class="poe-chip ${t.en().equals(activeTag) ? "poe-chip-active" : ""}" data-chip-value="${t.en()}">${PoeText.name(t.ko(), t.en())}</button>
							@endfor
						</div>
					@endfor
				</div>
			</div>

			<div id="poe2GemList" hx-get="${initialUrl}" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10"><span class="loading loading-spinner loading-lg text-primary"></span></div>
			</div>
			<script src="/js/poe/filterUrl.js" defer></script>
		</div>
	`
)
