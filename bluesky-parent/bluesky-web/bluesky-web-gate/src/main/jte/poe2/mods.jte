@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param List<Poe2.ModPoolSummary> pools = java.util.List.of()
@param String activeKey = null

!{
	String pageTitle = MessageUtil.getMessage("poe2.mods.title");
	// 아이템 클래스별 칩 — 같은 클래스에 풀이 여럿(방어 속성)이면 속성 칩이 한 줄 더
	java.util.Map<String, List<Poe2.ModPoolSummary>> byClass = new java.util.LinkedHashMap<>();
	for (String cat : List.of("weapon", "offhand", "armour", "jewellery", "flask", "jewel", "other")) {
		for (Poe2.ModPoolSummary p : pools) {
			String c = p.category() == null ? "other" : p.category();
			if (c.equals(cat)) byClass.computeIfAbsent(p.itemClass(), k -> new java.util.ArrayList<>()).add(p);
		}
	}
	Poe2.ModPoolSummary active = pools.stream().filter(p -> p.key().equals(activeKey)).findFirst().orElse(null);
	String enc = activeKey == null ? "" : java.net.URLEncoder.encode(activeKey, java.nio.charset.StandardCharsets.UTF_8);
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
			<%-- PoE1 모디파이어 화면과 같은 한 줄 설명(10-01) --%>
			<div class="text-sm text-base-content/60 -mt-2">${PoeText.isKorean() ? "일반 아이템 제작 모드 — 아이템 클래스별 접두/접미 티어" : "Craftable modifiers by item class"}</div>
			<nav class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm" aria-label="${MessageUtil.getMessage("poe2.mods.pool")}">
				<%-- 분류별 줄 + 줄 이름 — 베이스·고유 목록(poe2/classChips.jte)·PoE1 과 같은 모양(10-01: 한 줄에 전부 늘어놓았다) --%>
				@for(String cat : List.of("weapon", "offhand", "armour", "jewellery", "flask", "jewel", "other"))
					!{
						List<java.util.Map.Entry<String, List<Poe2.ModPoolSummary>>> inCat = byClass.entrySet().stream()
							.filter(e -> cat.equals(e.getValue().get(0).category() == null ? "other" : e.getValue().get(0).category())).toList();
					}
					@if(!inCat.isEmpty())
						<div class="flex flex-wrap items-center gap-1.5" data-chip-row="${cat}">
							<span class="w-16 shrink-0 text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.itemgroup." + cat)}</span>
							@for(java.util.Map.Entry<String, List<Poe2.ModPoolSummary>> e : inCat)
								!{
									// 분류 칩은 베이스가 가장 많은 풀로 간다 — 첫 풀로 가면 투구 · 장화 · 방패가 변형 없는 1~2개짜리(반신 베이스) 풀에 떨어졌다(10-01)
									Poe2.ModPoolSummary first = e.getValue().stream().max(java.util.Comparator.comparingInt(Poe2.ModPoolSummary::baseCount)).orElse(e.getValue().get(0));
									boolean on = active != null && active.itemClass().equals(e.getKey());
								}
								<a class="poe-chip ${on ? "poe-chip-active" : ""}" href="/poe2/mods?key=${java.net.URLEncoder.encode(first.key(), java.nio.charset.StandardCharsets.UTF_8)}" aria-current="${on ? "page" : null}" data-mods-chip>${net.luversof.web.gate.poe.PoeClassNames.poe2(first.itemClassKo(), first.itemClass())}<span class="ml-1 text-[10px] opacity-60">${first.groupCount()}</span></a>
							@endfor
						</div>
					@endif
				@endfor
				@if(active != null && byClass.get(active.itemClass()) != null && byClass.get(active.itemClass()).size() > 1)
					<div class="flex flex-wrap items-center gap-1.5 border-t border-base-200 pt-2">
						<%-- 변형 없는 풀은 "전체"가 아니라 방어 유형이 없는 나머지 베이스(투구 Golden Visage 등 반신 2개)라 "기타 베이스"로, 맨 뒤에(10-01) --%>
						!{
							List<Poe2.ModPoolSummary> siblings = new java.util.ArrayList<>(byClass.get(active.itemClass()));
							siblings.sort(java.util.Comparator.comparing(p -> p.variant() == null));
						}
						@for(Poe2.ModPoolSummary p : siblings)
							<a class="poe-chip ${p.key().equals(activeKey) ? "poe-chip-active" : ""}" href="/poe2/mods?key=${java.net.URLEncoder.encode(p.key(), java.nio.charset.StandardCharsets.UTF_8)}" data-pool-variant="${p.variant() == null ? "" : p.variant()}" data-mods-chip>${p.variant() == null ? MessageUtil.getMessage("poe2.mods.variant.other") : PoeText.name(p.variantKo(), p.variant())} <span class="opacity-60">(${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.mods.bases"), p.baseCount())})</span></a>
						@endfor
					</div>
				@endif
			</nav>
			@if(activeKey != null)
				<%-- 모드 검색 + 아이템 레벨 — PoE1 모디파이어 화면과 같은 막대(10-01: PoE2 엔 없었다) --%>
				@template.poe.modFilterBar(scope = "#poe2ModPool", idPrefix = "poe2Mod")
				<div id="poe2ModPool" hx-get="/poe2/htmx/mods?key=${enc}" hx-trigger="load" hx-swap="innerHTML">
					<div class="flex justify-center p-10"><span class="loading loading-spinner loading-lg text-primary"></span></div>
				</div>
			@endif
			<%-- 분류·변형 칩을 눌러도 검색어·아이템 레벨(주소 q=/lvl=, modFilterBar 가 씀)을 그대로 가져간다 — PoE1 은 칩이 같은 자리를 바꿔 끼워
			     유지됐는데 PoE2 칩은 링크라 새 화면에서 빈칸이 됐다(10-01) --%>
			<script nonce="${CspNonceHolder.getNonce()}">
				document.addEventListener("click", function (e) {
					var a = e.target.closest && e.target.closest("a[data-mods-chip]");
					if (!a) return;
					var cur = new URLSearchParams(location.search);
					var url = new URL(a.href, location.href);
					["q", "lvl"].forEach(function (k) { if (cur.get(k)) url.searchParams.set(k, cur.get(k)); });
					a.href = url.pathname + url.search;
				});
			</script>
		</div>
	`
)
