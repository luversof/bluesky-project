@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeGem
@import io.github.luversof.boot.context.support.MessageUtil

@param List<PoeGem> gems
@param int totalCount = 0

!{
	String countLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.count"), gems.size(), totalCount);
	String emptyLabel = MessageUtil.getMessage("poe.gems.empty");
	String supportBadgeLabel = MessageUtil.getMessage("poe.gems.type.support");
	String activeBadgeLabel = MessageUtil.getMessage("poe.gems.type.active");
	String dropLevelLabel = MessageUtil.getMessage("poe.gems.label.drop.level");
	String castTimeLabel = MessageUtil.getMessage("poe.gems.label.cast.time");
}

@if(totalCount == 0)
	<%-- 게임 데이터 미준비: 추출 파이프라인 안내 --%>
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: npx pathofexile-dat && node transform.mjs")
@else
<div class="text-xs text-base-content/50 mb-2">${countLabel}</div>
@if(gems.isEmpty())
	@template._components.ui.emptyState(message = emptyLabel)
@else
	<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
		@for(PoeGem gem : gems)
			!{
				// PoE 관례: 힘=빨강, 민첩=초록, 지능=파랑 (Tailwind 스캐너가 보도록 리터럴 유지)
				String dotClass = switch (gem.color()) {
					case "red" -> "bg-red-500";
					case "green" -> "bg-green-500";
					case "blue" -> "bg-blue-500";
					default -> "bg-gray-300";
				};
				String displayName = net.luversof.web.gate.poe.PoeText.name(gem.nameKo(), gem.name());
				String displayDescription = net.luversof.web.gate.poe.PoeText.name(gem.descriptionKo(), gem.description());
				List<String> displayTags = net.luversof.web.gate.poe.PoeText.lines(gem.tagsKo(), gem.tags());
			}
			<%-- 카드 마우스오버 → 게임 툴팁형 상세 레이어(플로팅) --%>
			<div class="poe-hover card bg-base-100 shadow-sm border border-base-200 cursor-help transition-colors duration-300 hover:border-primary/40 hover:duration-75"
			     hx-get="/poe/htmx/gems/detail?slug=${gem.slug()}"
			     hx-target="#poePreview"
			     hx-trigger="mouseenter delay:100ms"
			     hx-swap="innerHTML">
				<div class="card-body p-4 gap-2">
					<div class="flex items-center gap-2">
						<img src="/poe-assets/gems/${gem.slug()}.png" class="w-8 h-8 object-contain shrink-0" alt="" loading="lazy">
						<span class="w-2.5 h-2.5 rounded-full shrink-0 ${dotClass}"></span>
						<span class="font-bold truncate" title="${gem.name()}">${displayName}</span>
						@if(net.luversof.web.gate.poe.PoeText.isKorean())
							<span class="text-xs text-base-content/40 truncate">${gem.name()}</span>
						@endif
						<span class="badge badge-xs ${gem.isSupport() ? "badge-secondary badge-outline" : "badge-primary badge-outline"} ml-auto shrink-0">${gem.isSupport() ? supportBadgeLabel : activeBadgeLabel}</span>
					</div>
					@if(displayDescription != null && !displayDescription.isEmpty())
						<p class="text-xs leading-5 text-base-content/60 line-clamp-2" title="${displayDescription}">${displayDescription}</p>
					@endif
					<div class="flex flex-wrap items-center gap-1">
						@if(displayTags != null)
							@for(String tag : displayTags)
								<span class="badge badge-xs badge-ghost">${tag}</span>
							@endfor
						@endif
						<span class="ml-auto text-[11px] text-base-content/40">
							${dropLevelLabel} ${gem.dropLevel()}@if(gem.castTimeMs() != null) · ${castTimeLabel} ${String.format("%.2f", gem.castTimeMs() / 1000.0)}s@endif
						</span>
					</div>
				</div>
			</div>
		@endfor
	</div>
@endif
@endif
