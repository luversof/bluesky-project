@import net.luversof.web.gate.poe.service.PoeGem
@import net.luversof.web.gate.poe.service.PoeGemLevel
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeGem gem
@param int displayLevel = 20

!{
	String levelLabel = MessageUtil.getMessage("poe.gems.detail.level");
	String requiredLevelLabel = MessageUtil.getMessage("poe.gems.detail.required.level");
	String costLabel = MessageUtil.getMessage("poe.gems.detail.cost");
	String costMultiplierLabel = MessageUtil.getMessage("poe.gems.detail.cost.multiplier");
	String castTimeLabel = MessageUtil.getMessage("poe.gems.label.cast.time");
	String cooldownLabel = MessageUtil.getMessage("poe.gems.detail.cooldown");
	String critChanceLabel = MessageUtil.getMessage("poe.gems.detail.crit.chance");
	String damageEffectivenessLabel = MessageUtil.getMessage("poe.gems.detail.damage.effectiveness");
	String closeLabel = MessageUtil.getMessage("common.button.close");
	String supportBadgeLabel = MessageUtil.getMessage("poe.gems.type.support");
	String activeBadgeLabel = MessageUtil.getMessage("poe.gems.type.active");

	// 게임 툴팁 느낌의 젬 색상 헤더 (Tailwind 스캐너용 리터럴 유지)
	String nameColorClass = switch (gem.color()) {
		case "red" -> "text-red-400";
		case "green" -> "text-green-400";
		case "blue" -> "text-blue-400";
		default -> "text-gray-200";
	};
	String displayName = gem.nameKo() != null ? gem.nameKo() : gem.name();
	String displayDescription = gem.descriptionKo() != null ? gem.descriptionKo() : gem.description();
	java.util.List<String> displayTags = gem.tagsKo() != null && !gem.tagsKo().isEmpty() ? gem.tagsKo() : gem.tags();

	PoeGemLevel current = gem.levels().stream().filter(l -> l.level() == displayLevel).findFirst().orElse(null);
	// 레벨 내비: 1/5/10/15/20 + (있으면) 21~ 최대 레벨
	java.util.List<Integer> levelSteps = new java.util.ArrayList<>();
	for (PoeGemLevel l : gem.levels()) {
		if (l.level() == 1 || l.level() % 5 == 0) levelSteps.add(l.level());
	}
}

<%-- 오버레이: 바깥/X/ESC 로 닫힘 (common.js 의 data-overlay 위임) --%>
<div class="fixed inset-0 z-[90] flex items-center justify-center bg-black/60 p-4" data-overlay>
	<div class="w-full max-w-md rounded-box overflow-hidden bg-neutral text-neutral-content shadow-2xl border border-neutral-content/10">
		<div class="relative px-4 py-3 bg-neutral-focus/60 border-b border-neutral-content/15 text-center">
			<img src="/poe-assets/gems/${gem.slug()}.png" class="w-12 h-12 object-contain mx-auto mb-1" alt="">
			<div class="text-lg font-bold ${nameColorClass}">${displayName}</div>
			<div class="text-xs text-neutral-content/50">${gem.name()}</div>
			@if(displayTags != null && !displayTags.isEmpty())
				<div class="text-[11px] text-neutral-content/60 mt-1">${String.join(", ", displayTags)}</div>
			@endif
			<button type="button" class="btn btn-ghost btn-xs absolute right-2 top-2 text-neutral-content/60" data-overlay-close aria-label="${closeLabel}">✕</button>
		</div>

		<div class="px-4 py-3 space-y-1 text-sm">
			<div class="flex justify-between"><span class="text-neutral-content/60">${levelLabel}</span><span class="font-mono">${displayLevel}</span></div>
			@if(current != null)
				<div class="flex justify-between"><span class="text-neutral-content/60">${requiredLevelLabel}</span><span class="font-mono">${current.requiredLevel()}</span></div>
				@if(current.cost() != null && current.cost() > 0)
					<div class="flex justify-between"><span class="text-neutral-content/60">${costLabel}</span><span class="font-mono">${current.cost()} ${current.costType()}</span></div>
				@endif
				@if(current.costMultiplier() != null && current.costMultiplier() != 100)
					<div class="flex justify-between"><span class="text-neutral-content/60">${costMultiplierLabel}</span><span class="font-mono">${current.costMultiplier()}%</span></div>
				@endif
				@if(gem.castTimeMs() != null)
					<div class="flex justify-between"><span class="text-neutral-content/60">${castTimeLabel}</span><span class="font-mono">${String.format("%.2f", gem.castTimeMs() / 1000.0)}s</span></div>
				@endif
				@if(current.cooldownMs() != null)
					<div class="flex justify-between"><span class="text-neutral-content/60">${cooldownLabel}</span><span class="font-mono">${String.format("%.2f", current.cooldownMs() / 1000.0)}s</span></div>
				@endif
				@if(current.critChance() != null)
					<div class="flex justify-between"><span class="text-neutral-content/60">${critChanceLabel}</span><span class="font-mono">${String.format("%.2f", current.critChance())}%</span></div>
				@endif
				@if(current.damageEffectiveness() != null)
					<div class="flex justify-between"><span class="text-neutral-content/60">${damageEffectivenessLabel}</span><span class="font-mono">${String.format("%.0f", current.damageEffectiveness())}%</span></div>
				@endif
				@if(current.baseMultiplier() != null)
					<div class="flex justify-between"><span class="text-neutral-content/60">${damageEffectivenessLabel}</span><span class="font-mono">${String.format("%.0f", current.baseMultiplier())}%</span></div>
				@endif
			@endif
		</div>

		!{java.util.List<String> displayStatLines = current != null && current.statLinesKo() != null && !current.statLinesKo().isEmpty() ? current.statLinesKo() : (current != null ? current.statLines() : null);}
		@if(displayStatLines != null && !displayStatLines.isEmpty())
			<div class="px-4 pb-3">
				<div class="border-t border-neutral-content/15 pt-2 space-y-0.5 text-center">
					@for(String statLine : displayStatLines)
						<div class="text-sm text-sky-300/90 whitespace-pre-line">${statLine}</div>
					@endfor
				</div>
			</div>
		@endif

		@if(displayDescription != null && !displayDescription.isEmpty())
			<div class="px-4 pb-3">
				<p class="text-xs leading-5 italic text-amber-200/80 border-t border-neutral-content/15 pt-2">${displayDescription}</p>
			</div>
		@endif

		<div class="px-4 pb-4">
			<div class="flex flex-wrap gap-1 items-center">
				<span class="text-[11px] text-neutral-content/50 mr-1">${levelLabel}</span>
				@for(int step : levelSteps)
					<button type="button"
					        class="btn btn-xs ${step == displayLevel ? "btn-primary" : "btn-ghost text-neutral-content/70"}"
					        hx-get="/poe/htmx/gems/detail?slug=${gem.slug()}&level=${step}"
					        hx-target="#poeGemDetail"
					        hx-swap="innerHTML">${step}</button>
				@endfor
				<span class="badge badge-xs ${gem.isSupport() ? "badge-secondary" : "badge-primary"} badge-outline ml-auto">${gem.isSupport() ? supportBadgeLabel : activeBadgeLabel}</span>
			</div>
		</div>
	</div>
</div>
