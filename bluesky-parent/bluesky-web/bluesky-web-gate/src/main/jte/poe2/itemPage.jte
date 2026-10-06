@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param Poe2.BaseItem base
@param Poe2.ModPool pool = null
@param java.util.List<Poe2.Unique> baseUniques = java.util.List.of()

!{ String title = PoeText.name(base.nameKo(), base.name()); }

<%-- PoE2 베이스 상세 — 툴팁 + 이 베이스에 붙을 수 있는 옵션(접두 · 접미 티어 표). --%>
@template._layout.poeLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			<a href="/poe2/items" class="link link-hover text-sm text-base-content/60">← ${MessageUtil.getMessage("poe2.items.title")}</a>
			@template.poe2.header(title = title, meta = meta)
			@if(PoeText.isKorean())
				<div class="text-sm text-base-content/60">${MessageUtil.getMessage("poe2.english")}: ${base.name()}</div>
			@endif
			<div class="flex flex-wrap items-start gap-6">
				@if(base.image() != null)
					<img src="/poe-data/poe2/icons/${base.image()}" class="max-h-64 max-w-40 object-contain drop-shadow-[0_2px_6px_rgba(0,0,0,0.5)]" alt="${base.name()}">
				@endif
				<div class="max-w-md w-full sm:w-auto">
					@template.poe2.htmx.itemTooltip(base = base)
				</div>
			</div>
			<%-- poedb 식 — 이 베이스를 쓰는 고유 아이템(호버 툴팁 + 상세 링크). PoE1 베이스 상세와 같은 구획·마크업(10-01) --%>
			@if(baseUniques != null && !baseUniques.isEmpty())
				<div class="rounded-2xl bg-base-100 p-4 shadow-sm" data-base-uniques>
					<div class="mb-2 flex flex-wrap items-baseline gap-2">
						<h2 class="font-bold text-orange-500">${PoeText.isKorean() ? "이 베이스의 고유 아이템" : "Uniques on this base"}</h2>
						<span class="text-xs font-mono text-base-content/60">${baseUniques.size()}</span>
					</div>
					<div class="flex flex-wrap gap-x-4 gap-y-1">
						@for(Poe2.Unique u : baseUniques)
							<a class="poe-hover font-semibold poe-name-unique link-hover cursor-pointer text-sm"
							   href="/poe2/uniques/${u.slug()}"
							   hx-get="/poe2/htmx/uniques/detail?slug=${u.slug()}"
							   hx-target="#poePreview"
							   hx-trigger="mouseenter delay:100ms"
							   hx-swap="innerHTML">${PoeText.name(u.nameKo(), u.name())}</a>
						@endfor
					</div>
				</div>
			@endif
			@if(pool != null)
				<div class="flex flex-wrap items-baseline gap-2 pt-2">
					<h2 class="text-lg font-semibold">${MessageUtil.getMessage("poe2.items.pool")}</h2>
					<%-- PoE1 베이스 상세와 같은 바로가기 + 검색·레벨 막대(10-01) --%>
					<a class="link link-hover text-sm text-primary ml-auto" href="/poe2/mods?key=${java.net.URLEncoder.encode(pool.key(), java.nio.charset.StandardCharsets.UTF_8)}">${PoeText.isKorean() ? "모드 페이지에서 보기 →" : "Open in Modifiers →"}</a>
				</div>
				@template.poe.modFilterBar(scope = "#poe2ItemModBody", idPrefix = "poe2ItemMod")
				<div id="poe2ItemModBody">
					@template.poe2.htmx.modPool(pool = pool, nested = true)
				</div>
			@endif
		</div>
	`
)
