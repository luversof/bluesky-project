@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param PoeUniqueItem item
@param PoeBaseItem base = null
@param Integer variant = null
@param java.util.List<net.luversof.web.gate.poe.dto.PoeFoulbornGroup> foulborn = null

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
				<a href="/poe/uniques" class="link link-hover text-sm text-base-content/60">← ${MessageUtil.getMessage("poe.uniques.title")}</a>
				<h1 class="text-2xl font-bold">${displayName}</h1>
				@if(ko)<span class="text-base-content/40 text-sm">${item.name()}</span>@endif
				<span class="badge badge-ghost">${patchLabel}</span>
				<a class="btn btn-sm btn-primary btn-outline ml-auto" href="/poe/sim?uniques=${item.slug()}">${net.luversof.web.gate.poe.PoeText.isKorean() ? "이 유니크로 최적화 →" : "Optimize with this unique →"}</a>
			</div>

			<div class="flex flex-wrap gap-6 items-start">
				<%-- 호버 레이어(툴팁)를 그대로 바닥에 --%>
				@template.poe.htmx.uniqueVariants(item = item, base = base, variant = variant)

				<div class="flex-1 min-w-[20rem] space-y-4">
					<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
						<h2 class="font-bold mb-2">${ko ? "정보" : "Info"}</h2>
						<table class="table table-xs">
							<tbody>
								<tr><td class="text-base-content/50">${ko ? "영문명" : "Name"}</td><td>${item.name()}</td></tr>
								<tr><td class="text-base-content/50">${ko ? "베이스" : "Base"}</td><td>
									@if(base != null)
										<a class="link link-hover" href="/poe/items/${base.slug()}">${PoeText.name(item.baseTypeKo(), item.baseType())}</a>
									@else
										${PoeText.name(item.baseTypeKo(), item.baseType())}
									@endif
								</td></tr>
								@if(base != null)
									<tr><td class="text-base-content/50">${ko ? "아이템 클래스" : "Item class"}</td><td>${PoeText.name(base.itemClassKo(), base.itemClass())}</td></tr>
								@endif
								@if(item.category() != null)
									<tr><td class="text-base-content/50">${ko ? "분류" : "Category"}</td><td>${item.category()}</td></tr>
								@endif
								<%-- 리그 출처 — 이 아이템을 얻으려면 해당 리그 기제를 돌려야 한다(목록 배지와 같은 정보) --%>
								@if(item.league() != null && !item.league().isBlank())
									<tr><td class="text-base-content/50">${ko ? "리그 출처" : "League"}</td><td>${item.league()}</td></tr>
								@endif
								<%-- 획득 가능 여부 — 지금 리그에서 못 얻는 고유는 그 사실이 스펙만큼 중요하다(시뮬 후보에서도 빠진다) --%>
								@if(item.legacy())
									<tr><td class="text-base-content/50">${ko ? "획득" : "Obtainable"}</td><td class="text-warning">${ko ? "현재 리그 획득 불가 (스탠다드 보관분만)" : "Not obtainable in the current league"}</td></tr>
								@endif
							</tbody>
						</table>
					</div>

					<%-- 영문 원문도 **지금 보고 있는 변형** 기준이어야 한다 — 툴팁은 화염인데 원문만 카오스면 같은 화면이 서로 다른 말을 한다 --%>
					!{
						java.util.List<String> engMods = item.explicits();
						if (variant != null && item.variants() != null) {
							for (var v : item.variants()) {
								if (v.index() == variant.intValue()) engMods = v.explicits();
							}
						}
					}
					<%-- 인게임 "삿된 XXX" — 이 유니크의 기존 모드를 대체하는 옵션 풀.
					     아이템마다 붙을 수 있는 옵션이 정해져 있어서, 아이템 화면에 함께 있어야 쓸모가 있다. --%>
					@if(foulborn != null && !foulborn.isEmpty())
						<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
							<div class="mb-2 flex flex-wrap items-baseline gap-2">
								<h2 class="font-bold text-fuchsia-500">${ko ? "삿된 옵션" : "Foulborn options"}</h2>
								<span class="text-xs font-mono text-base-content/40">${foulborn.stream().mapToInt(g -> g.mods().size()).sum()}</span>
								<span class="text-xs text-base-content/50">${ko ? "삿된 오브로 기존 모드 하나가 아래 중 하나로 바뀝니다" : "Foulborn currency replaces one existing mod with one of these"}</span>
								<a class="ml-auto link link-hover text-xs text-base-content/60" href="/poe/foulborn?q=${item.name()}">${ko ? "삿된 사전에서 보기 →" : "Open in Foulborn list →"}</a>
							</div>
							<%-- 원본 → 삿된 대비. 인게임 색(삿된 #CD2285)과 취소선으로 "무엇이 밀려나는지" 를 그대로 보여준다 --%>
							!{ java.util.List<net.luversof.web.gate.poe.dto.PoeFoulbornGroup.FoulbornMod> foulMods = foulborn.stream().flatMap(g -> g.mods().stream()).toList(); }
							@template.poe.htmx.foulbornOptions(mods = foulMods)
						</div>
					@endif

					@if(ko && engMods != null && !engMods.isEmpty())
						<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
							<h2 class="font-bold mb-2">모드 원문 (영문)</h2>
							@for(String line : engMods)
								<div class="text-xs text-base-content/60 whitespace-pre-line">${line}</div>
							@endfor
						</div>
					@endif
				</div>
			</div>
		</div>
	`
)
