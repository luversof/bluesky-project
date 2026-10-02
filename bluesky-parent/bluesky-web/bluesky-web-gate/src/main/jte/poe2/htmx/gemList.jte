@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Poe2.Gem> gems
@param Integer total = 0

!{
	boolean korean = PoeText.isKorean();
	// 속성(색)별 세로 컬럼 — 힘 · 민첩 · 지능 · 속성 없음. 컬럼 안은 티어(얻는 시점) → 이름 순.
	Map<String, List<Poe2.Gem>> byColor = new java.util.LinkedHashMap<>();
	for (String c : new String[] { "str", "dex", "int", "white" }) byColor.put(c, new java.util.ArrayList<>());
	for (Poe2.Gem g : gems) byColor.getOrDefault(g.color(), byColor.get("white")).add(g);
	byColor.values().forEach(l -> l.sort(java.util.Comparator
		.comparing((Poe2.Gem g) -> g.tier() == null ? 99 : g.tier())
		.thenComparing(g -> PoeText.name(g.nameKo(), g.name()))));
}

@if(total == null || total == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.nodata"))
@else
	<div class="text-xs text-base-content/50 mb-2">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.count"), gems.size(), total)}</div>
	@if(gems.isEmpty())
		@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.empty"))
	@else
		<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 items-start">
			@for(Map.Entry<String, List<Poe2.Gem>> group : byColor.entrySet())
				@if(!group.getValue().isEmpty())
					!{
						String color = group.getKey();
						// 인게임 소켓 색(PoE1 과 같은 값) — 점·이름 색을 맞춘다
						String dot = switch (color) { case "str" -> "bg-[#e05030]"; case "dex" -> "bg-[#70ff70]"; case "int" -> "bg-[#7070ff]"; default -> "bg-[#d8d8d8]"; };
						String nameColor = switch (color) { case "str" -> "poe-gem-red"; case "dex" -> "poe-gem-green"; case "int" -> "poe-gem-blue"; default -> "poe-gem-white"; };
					}
					<div class="space-y-1.5">
						<div class="flex items-center gap-2 sticky top-0 bg-base-200/85 backdrop-blur py-1.5 z-[1] rounded">
							<span class="w-3 h-3 rounded-full ${dot}"></span>
							<span class="font-semibold text-sm">${MessageUtil.getMessage("poe2.gems.color." + color)}</span>
							<span class="text-xs text-base-content/40">${group.getValue().size()}</span>
						</div>
						@for(Poe2.Gem gem : group.getValue())
							!{ List<String> shownTags = PoeText.lines(gem.tagsKo(), gem.tags()); }
							<div class="poe-hover flex items-center gap-2.5 rounded-lg bg-base-100 border border-base-200 px-2.5 py-2 cursor-help transition-colors duration-300 hover:border-primary/40 hover:duration-75"
							     hx-get="/poe2/htmx/gems/detail?slug=${gem.slug()}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">
								@if(gem.image() != null)
									<img src="/poe-data/poe2/icons/${gem.image()}" class="w-11 h-11 object-contain shrink-0 drop-shadow-[0_1px_2px_rgba(0,0,0,0.6)]" alt="" loading="lazy">
								@endif
								<div class="min-w-0 flex-1">
								<div class="flex items-baseline gap-1.5 min-w-0">
									<a class="font-semibold truncate link-hover ${nameColor}" href="/poe2/gems/${gem.slug()}" title="${gem.name()}">${PoeText.name(gem.nameKo(), gem.name())}</a>
									@if(!"skill".equals(gem.kind()))
										<span class="shrink-0 badge badge-xs badge-outline">${MessageUtil.getMessage("poe2.gems.kind." + gem.kind())}</span>
									@endif
									@if(gem.tier() != null && gem.tier() > 0)
										<span class="ml-auto shrink-0 text-[10px] text-base-content/40">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.tier"), gem.tier())}</span>
									@endif
								</div>
								@if(korean)
									<div class="text-[11px] text-base-content/40 truncate">${gem.name()}</div>
								@endif
								@if(shownTags != null && !shownTags.isEmpty())
									<div class="flex flex-wrap gap-1 mt-1">
										@for(String t : shownTags)
											<span class="badge badge-xs badge-ghost">${t}</span>
										@endfor
									</div>
								@endif
								</div>
							</div>
						@endfor
					</div>
				@endif
			@endfor
		</div>
	@endif
@endif
