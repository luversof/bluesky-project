@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Poe2.Unique> items
@param Integer total = 0

!{
	boolean korean = PoeText.isKorean();
	List<Poe2.Unique> sorted = new java.util.ArrayList<>(items);
	sorted.sort(java.util.Comparator.comparing((Poe2.Unique u) -> PoeText.name(u.itemClassKo(), u.itemClass()) == null ? "" : PoeText.name(u.itemClassKo(), u.itemClass()))
		.thenComparing(u -> PoeText.name(u.nameKo(), u.name())));
}

@if(total == null || total == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.nodata"))
@else
	<div class="text-xs text-base-content/50 mb-2">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.count"), items.size(), total)}</div>
	@if(items.isEmpty())
		@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.empty"))
	@else
		<%-- 카드 = PoE1 고유 목록(poe/htmx/uniqueList.jte)과 같은 구성(10-01: PoE2 는 이름 · 베이스만): 베이스 + 요구 레벨 + 리그 배지 + 변형 배지,
		     인게임 툴팁 구조대로 고정(암시) 옵션 → 구분선 → 접두/접미 옵션(모드 파랑, 수치 강조). 변형이 있으면 기본 변형의 옵션. --%>
		<div class="grid grid-cols-1 md:grid-cols-2 gap-3">
			@for(Poe2.Unique u : sorted)
				!{
					Poe2.UniqueVariant dv = null;
					if (u.defaultVariant() != null && u.variants() != null) {
						for (Poe2.UniqueVariant v : u.variants()) {
							if (v.index() != null && v.index().intValue() == u.defaultVariant().intValue()) dv = v;
						}
					}
					List<String> imps = dv != null ? PoeText.lines(dv.implicitsKo(), dv.implicits()) : PoeText.lines(u.implicitsKo(), u.implicits());
					List<String> exps = dv != null ? PoeText.lines(dv.explicitsKo(), dv.explicits()) : PoeText.lines(u.explicitsKo(), u.explicits());
					if (imps == null) imps = List.of();
					if (exps == null) exps = List.of();
				}
				<div class="poe-hover card bg-base-100 shadow-sm border border-base-200 cursor-pointer transition-colors duration-300 hover:border-orange-400/50 hover:duration-75"
				     hx-get="/poe2/htmx/uniques/detail?slug=${u.slug()}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML" data-unique-card>
					<div class="card-body p-3">
					<div class="flex gap-3 items-start">
						<div class="w-14 h-16 shrink-0 flex items-center justify-center">
							@if(u.image() != null)
								<img src="/poe-data/poe2/icons/${u.image()}" class="max-w-14 max-h-16 object-contain drop-shadow-[0_1px_3px_rgba(0,0,0,0.7)]" alt="" loading="lazy">
							@endif
						</div>
						<div class="min-w-0 flex-1 space-y-1">
							<div class="flex items-baseline gap-2 min-w-0">
								<a class="font-bold poe-name-unique truncate link-hover" href="/poe2/uniques/${u.slug()}" title="${u.name()}">${PoeText.name(u.nameKo(), u.name())}</a>
								@if(korean)
									<span class="text-xs text-base-content/40 truncate">${u.name()}</span>
								@endif
							</div>
							<div class="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-base-content/55">
								<span class="truncate">${PoeText.name(u.baseTypeKo(), u.baseType())} · ${PoeText.name(u.itemClassKo(), u.itemClass())}</span>
								@if(u.requiredLevel() != null && u.requiredLevel() > 1)
									<span class="text-base-content/40" data-req-level>${MessageUtil.getMessage("poe.gems.detail.required.level")} ${u.requiredLevel()}</span>
								@endif
								@if(u.league() != null && !u.league().isBlank())
									<span class="badge badge-ghost badge-xs" data-league title="${korean ? "이 고유는 해당 리그 콘텐츠에서 나옵니다" : "Drops from this league's content"}">${u.league()}</span>
								@endif
								@if(u.variants() != null && u.variants().size() > 1)
									<span class="badge badge-outline badge-xs border-fuchsia-500/40 text-fuchsia-500" title="${korean ? "모드 구성이 다른 변형이 있습니다 — 상세에서 선택" : "Has alternate variants - pick one on the detail page"}">${MessageUtil.getMessage("poe2.uniques.variant")} ${u.variants().size()}</span>
								@endif
							</div>
							@if(!imps.isEmpty() || !exps.isEmpty())
								<div class="mt-1 space-y-0.5" data-card-mods>
									@for(String imp : imps)
										<div class="text-[11px] leading-5 text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(imp)}</div>
									@endfor
									@if(!imps.isEmpty() && !exps.isEmpty())
										<div class="my-1 border-t border-base-content/15"></div>
									@endif
									@for(String ex : exps)
										<div class="text-[11px] leading-5 text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(ex)}</div>
									@endfor
								</div>
							@endif
						</div>
					</div>
					</div>
				</div>
			@endfor
		</div>
	@endif
@endif
