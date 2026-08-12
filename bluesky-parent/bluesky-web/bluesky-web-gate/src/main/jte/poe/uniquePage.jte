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
				<div class="shrink-0">
					@template.poe.htmx.uniqueDetail(item = item, base = base)
				</div>

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
							</tbody>
						</table>
					</div>

					!{java.util.List<String> engMods = item.explicits();}
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
