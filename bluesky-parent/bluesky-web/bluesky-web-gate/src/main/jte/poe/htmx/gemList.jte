@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.poe.dto.PoeGem
@import io.github.luversof.boot.context.support.MessageUtil

@param List<PoeGem> gems
@param int totalCount = 0
@param String iconVersion = "0"

!{
	String countLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.count"), gems.size(), totalCount);
	String emptyLabel = MessageUtil.getMessage("poe.gems.empty");
	boolean korean = net.luversof.web.gate.poe.PoeText.isKorean();

	// poedb 처럼 속성(색)별 세로 컬럼: 힘=빨강, 민첩=초록, 지능=파랑, 그 외=백색. 각 컬럼은 요구(드롭) 레벨 오름차순.
	Map<String, List<PoeGem>> byColor = new java.util.LinkedHashMap<>();
	for (String c : new String[] { "red", "green", "blue", "white" }) byColor.put(c, new java.util.ArrayList<>());
	for (PoeGem g : gems) {
		String c = switch (g.color()) {
			case "red", "green", "blue" -> g.color();
			default -> "white";
		};
		byColor.get(c).add(g);
	}
	byColor.values().forEach(l -> l.sort(java.util.Comparator.comparingInt(PoeGem::dropLevel)));
}

@if(totalCount == 0)
	<%-- 게임 데이터 미준비: 추출 파이프라인 안내 --%>
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: npx pathofexile-dat && node transform.mjs")
@else
<div class="text-xs text-base-content/50 mb-2">${countLabel}</div>
@if(gems.isEmpty())
	@template._components.ui.emptyState(message = emptyLabel)
@else
	<%-- 힘/민첩/지능(+그외) 세로 3열. 각 컬럼은 요구 레벨순 정렬. --%>
	<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 items-start">
		@for(Map.Entry<String, List<PoeGem>> group : byColor.entrySet())
			@if(!group.getValue().isEmpty())
				!{
					String color = group.getKey();
					String accent = switch (color) {
						case "red" -> "bg-red-500";
						case "green" -> "bg-green-500";
						case "blue" -> "bg-blue-500";
						default -> "bg-gray-300";
					};
					String attrLabel = MessageUtil.getMessage("poe.gems.attr." + color);
				}
				<div class="space-y-1.5">
					<%-- 컬럼 헤더 (속성) --%>
					<div class="flex items-center gap-2 sticky top-0 bg-base-200/85 backdrop-blur py-1.5 z-[1] rounded">
						<span class="w-3 h-3 rounded-full ${accent}"></span>
						<span class="font-semibold text-sm">${attrLabel}</span>
						<span class="text-xs text-base-content/40">${group.getValue().size()}</span>
					</div>
					@for(PoeGem gem : group.getValue())
						!{
							String displayName = net.luversof.web.gate.poe.PoeText.name(gem.nameKo(), gem.name());
							List<String> displayTags = net.luversof.web.gate.poe.PoeText.lines(gem.tagsKo(), gem.tags());
						}
						<%-- 카드 마우스오버 → 게임 툴팁형 상세 레이어. 목록은 아이콘·이름·태그(+레벨)만. --%>
						<div class="poe-hover flex items-center gap-2.5 rounded-lg bg-base-100 border border-base-200 px-2.5 py-2 cursor-help transition-colors duration-300 hover:border-primary/40 hover:duration-75"
						     hx-get="/poe/htmx/gems/detail?slug=${gem.slug()}"
						     hx-target="#poePreview"
						     hx-trigger="mouseenter delay:100ms"
						     hx-swap="innerHTML">
							<img src="/poe-assets/gems/${gem.slug()}.png?v=${iconVersion}" class="w-10 h-10 object-contain shrink-0 drop-shadow-[0_1px_2px_rgba(0,0,0,0.6)]" alt="" loading="lazy">
							<div class="min-w-0 flex-1">
								<div class="flex items-baseline gap-1.5">
									<span class="font-semibold truncate" title="${gem.name()}">${displayName}</span>
									@if(korean)
										<span class="text-[11px] text-base-content/40 truncate">${gem.name()}</span>
									@endif
									<span class="ml-auto shrink-0 text-[10px] text-base-content/40">Lv ${gem.dropLevel()}</span>
								</div>
								@if(displayTags != null && !displayTags.isEmpty())
									<div class="flex flex-wrap gap-1 mt-1">
										@for(String tag : displayTags)
											<span class="badge badge-xs badge-ghost">${tag}</span>
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
