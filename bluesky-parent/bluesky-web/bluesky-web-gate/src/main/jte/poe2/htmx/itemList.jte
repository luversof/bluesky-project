@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import static net.luversof.web.gate.poe2.Poe2View.nz
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Poe2.BaseItem> items
@param Integer total = 0

!{
	boolean korean = PoeText.isKorean();
	// 아이템 클래스별로 묶고, 안에서는 요구 레벨 순(게임에서 얻는 순서)
	java.util.Map<String, List<Poe2.BaseItem>> byClass = new java.util.LinkedHashMap<>();
	for (Poe2.BaseItem b : items) byClass.computeIfAbsent(net.luversof.web.gate.poe.PoeClassNames.poe2(b.itemClassKo(), b.itemClass()), k -> new java.util.ArrayList<>()).add(b);
	byClass.values().forEach(l -> l.sort(java.util.Comparator.comparingInt((Poe2.BaseItem b) -> nz(b.reqLevel())).thenComparing(Poe2.BaseItem::name)));
}

@if(total == null || total == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.nodata"))
@else
	<div class="text-xs text-base-content/50 mb-2">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.count"), items.size(), total)}</div>
	@if(items.isEmpty())
		@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.empty"))
	@else
		<div class="space-y-4">
			@for(java.util.Map.Entry<String, List<Poe2.BaseItem>> group : byClass.entrySet())
				<section>
					<h2 class="text-sm font-semibold mb-1.5">${group.getKey()} <span class="text-xs font-normal text-base-content/40">${group.getValue().size()}</span></h2>
					<%-- 카드 = PoE1 베이스 목록과 같은 구성(10-01: 이름 · 레벨뿐이었다) — 요약 수치(방어/피해·DPS/회복) + 암시 옵션 첫 줄 --%>
					<div class="grid grid-cols-1 md:grid-cols-2 2xl:grid-cols-3 gap-2">
						@for(Poe2.BaseItem b : group.getValue())
							<div class="poe-hover flex items-center gap-3 rounded-lg bg-base-100 border border-base-200 px-3 py-2 cursor-help transition-colors duration-300 hover:border-primary/40 hover:duration-75"
							     hx-get="/poe2/htmx/items/detail?slug=${b.slug()}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">
								<div class="w-10 h-12 shrink-0 flex items-center justify-center">
									@if(b.image() != null)
										<img src="/poe-data/poe2/icons/${b.image()}" class="max-w-10 max-h-12 object-contain" alt="" loading="lazy">
									@endif
								</div>
								<div class="min-w-0 flex-1">
								<div class="flex items-baseline gap-1.5 min-w-0">
									<a class="font-semibold truncate link-hover" href="/poe2/items/${b.slug()}" title="${b.name()}">${PoeText.name(b.nameKo(), b.name())}</a>
									@if(korean)
										<span class="text-[11px] text-base-content/35 truncate">${b.name()}</span>
									@endif
								</div>
								!{ String summary = net.luversof.web.gate.poe2.Poe2View.cardSummary(b); List<String> impl = net.luversof.web.gate.poe2.Poe2View.implicitLines(b); }
								<div class="flex items-center gap-2 text-[12px] text-base-content/70">
									<span class="font-mono truncate" data-card-summary>${summary}</span>
									<span class="ml-auto shrink-0 text-[10px] text-base-content/40">Lv ${nz(b.reqLevel())}</span>
								</div>
								@if(!impl.isEmpty())
									<p class="text-xs leading-5 text-sky-600/80 dark:text-sky-300/80 line-clamp-1" data-card-implicit>${impl.get(0)}</p>
								@endif
								</div>
							</div>
						@endfor
					</div>
				</section>
			@endfor
		</div>
	@endif
@endif
