@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param Poe2.Unique item
@param Poe2.BaseItem base = null
@param Integer variant = null
@param boolean forceable = false

!{
	String title = PoeText.name(item.nameKo(), item.name());
	Integer active = variant != null ? variant : item.defaultVariant();
}

<%-- PoE2 고유 상세 — 툴팁 + 변형 칩(툴팁 바깥, 인게임엔 라벨이 없으므로). 오른쪽 "정보" 표와 "모드 원문(영문)"은 PoE1 고유 상세
     (poe/uniquePage.jte)와 같은 구성(10-01: PoE2 엔 없었다). --%>
@template._layout.poeLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			<a href="/poe2/uniques" class="link link-hover text-sm text-base-content/60">← ${MessageUtil.getMessage("poe2.uniques.title")}</a>
			@template.poe2.header(title = title, meta = meta)
			@if(forceable)
				<%-- PoE1 고유 상세와 같은 바로가기 — 시뮬레이터가 이 고유를 고정한 채 열린다(10-01, 방어구·장신구만) --%>
				<a class="btn btn-sm btn-primary btn-outline" href="/poe2/sim?unique=${item.slug()}" data-optimize-unique>${PoeText.isKorean() ? "이 고유로 최적화 →" : "Optimize with this unique →"}</a>
			@endif
			@if(PoeText.isKorean())
				<div class="text-sm text-base-content/50">${MessageUtil.getMessage("poe2.english")}: ${item.name()} · ${item.baseType()}</div>
			@endif
			@if(item.variants() != null && !item.variants().isEmpty())
				<div class="flex flex-wrap items-center gap-1.5" aria-label="${MessageUtil.getMessage("poe2.uniques.variant")}">
					<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe2.uniques.variant")}</span>
					@for(Poe2.UniqueVariant v : item.variants())
						<a class="poe-chip ${v.index() != null && v.index().equals(active) ? "poe-chip-active" : ""}" href="/poe2/uniques/${item.slug()}?variant=${v.index()}">${PoeText.name(v.nameKo(), v.name())}</a>
					@endfor
				</div>
			@endif
			<div class="flex flex-wrap items-start gap-6">
				@if(item.image() != null)
					<img src="/poe-data/poe2/icons/${item.image()}" class="max-h-64 max-w-40 object-contain drop-shadow-[0_2px_6px_rgba(0,0,0,0.5)]" alt="${item.name()}">
				@endif
				<div class="max-w-md w-full sm:w-auto">
					@template.poe2.htmx.uniqueTooltip(item = item, base = base, variant = active)
				</div>
				!{
					boolean ko = PoeText.isKorean();
					java.util.List<String> engMods = item.explicits();
					if (active != null && item.variants() != null) {
						for (Poe2.UniqueVariant v : item.variants()) {
							if (v.index() != null && v.index().equals(active) && v.explicits() != null) engMods = v.explicits();
						}
					}
				}
				<div class="flex-1 min-w-[20rem] space-y-4">
					<div class="rounded-2xl bg-base-100 p-4 shadow-sm" data-unique-info>
						<h2 class="font-bold mb-2">${ko ? "정보" : "Info"}</h2>
						<table class="table table-xs">
							<tbody>
								<tr><td class="text-base-content/50">${ko ? "영문명" : "Name"}</td><td>${item.name()}</td></tr>
								<tr><td class="text-base-content/50">${ko ? "베이스" : "Base"}</td><td>
									@if(base != null && base.slug() != null)
										<a class="link link-hover" href="/poe2/items/${base.slug()}">${PoeText.name(item.baseTypeKo(), item.baseType())}</a>
									@else
										${PoeText.name(item.baseTypeKo(), item.baseType())}
									@endif
								</td></tr>
								@if(item.itemClass() != null)
									<tr><td class="text-base-content/50">${ko ? "아이템 클래스" : "Item class"}</td><td>${PoeText.name(item.itemClassKo(), item.itemClass())}</td></tr>
								@endif
								@if(item.requiredLevel() != null && item.requiredLevel() > 1)
									<tr><td class="text-base-content/50">${ko ? "요구 레벨" : "Required level"}</td><td>${item.requiredLevel()}</td></tr>
								@endif
								@if(item.league() != null && !item.league().isBlank())
									<tr><td class="text-base-content/50">${ko ? "리그 출처" : "League"}</td><td>${item.league()}</td></tr>
								@endif
							</tbody>
						</table>
					</div>
					@if(ko && engMods != null && !engMods.isEmpty())
						<div class="rounded-2xl bg-base-100 p-4 shadow-sm" data-unique-english>
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
