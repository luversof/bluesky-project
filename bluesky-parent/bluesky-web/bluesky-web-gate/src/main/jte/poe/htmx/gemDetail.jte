@import net.luversof.web.gate.poe.dto.PoeGem
@import net.luversof.web.gate.poe.dto.PoeGemLevel
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

	// 젬 색상 = 이름 색 (게임 툴팁 관례). 헤더 밴드는 젬(청록) 프레임.
	String nameColor = switch (gem.color()) {
		case "red" -> "#e05a4f";
		case "green" -> "#4fb96a";
		case "blue" -> "#6a8fe0";
		default -> "#d8d8d8";
	};
	String displayName = net.luversof.web.gate.poe.PoeText.name(gem.nameKo(), gem.name());
	String displayDescription = net.luversof.web.gate.poe.PoeText.name(gem.descriptionKo(), gem.description());
	java.util.List<String> displayTags = net.luversof.web.gate.poe.PoeText.lines(gem.tagsKo(), gem.tags());

	PoeGemLevel current = gem.levels().stream().filter(l -> l.level() == displayLevel).findFirst().orElse(null);
	java.util.List<Integer> levelSteps = new java.util.ArrayList<>();
	for (PoeGemLevel l : gem.levels()) {
		if (l.level() == 1 || l.level() % 5 == 0) levelSteps.add(l.level());
	}
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
}

<%-- PoE 게임 젬 툴팁 — 게임 원본 젬 헤더 밴드/구분선 --%>
	<div class="poe-tooltip poe-rar-gem w-80 max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:#1ba29b">
		<%-- 헤더 밴드 (젬 아이콘 + 이름 + 태그) --%>
		<div class="poe-tt-band" style="min-height:4.25rem">
			<img src="/poe-assets/gems/${gem.slug()}.png" class="w-9 h-9 object-contain mb-0.5 drop-shadow-[0_1px_3px_rgba(0,0,0,0.9)]" alt="">
			<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${displayName}</div>
		</div>

		<%-- 태그 + 영문 병기 --%>
		<div class="px-4 pt-1 text-center">
			@if(displayTags != null && !displayTags.isEmpty())
				<div class="text-[11px] text-[#7f9fb5]">${String.join(", ", displayTags)}</div>
			@endif
			@if(net.luversof.web.gate.poe.PoeText.isKorean())
				<div class="text-[10px] text-white/30">${gem.name()}</div>
			@endif
		</div>

		<%-- 레벨/요구/소모 등 수치 --%>
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] space-y-0.5">
			<div class="flex justify-between"><span class="${label}">${levelLabel}</span><span class="${value} font-mono">${displayLevel}</span></div>
			@if(current != null)
				<div class="flex justify-between"><span class="${label}">${requiredLevelLabel}</span><span class="${value} font-mono">${current.requiredLevel()}</span></div>
				@if(current.cost() != null && current.cost() > 0)
					<div class="flex justify-between"><span class="${label}">${costLabel}</span><span class="${value} font-mono">${current.cost()} ${current.costType()}</span></div>
				@endif
				@if(current.costMultiplier() != null && current.costMultiplier() != 100)
					<div class="flex justify-between"><span class="${label}">${costMultiplierLabel}</span><span class="${value} font-mono">${current.costMultiplier()}%</span></div>
				@endif
				@if(gem.castTimeMs() != null)
					<div class="flex justify-between"><span class="${label}">${castTimeLabel}</span><span class="${value} font-mono">${String.format("%.2f", gem.castTimeMs() / 1000.0)}s</span></div>
				@endif
				@if(current.cooldownMs() != null)
					<div class="flex justify-between"><span class="${label}">${cooldownLabel}</span><span class="${value} font-mono">${String.format("%.2f", current.cooldownMs() / 1000.0)}s</span></div>
				@endif
				@if(current.critChance() != null)
					<div class="flex justify-between"><span class="${label}">${critChanceLabel}</span><span class="${value} font-mono">${String.format("%.2f", current.critChance())}%</span></div>
				@endif
				@if(current.damageEffectiveness() != null)
					<div class="flex justify-between"><span class="${label}">${damageEffectivenessLabel}</span><span class="${value} font-mono">${String.format("%.0f", current.damageEffectiveness())}%</span></div>
				@endif
				@if(current.baseMultiplier() != null)
					<div class="flex justify-between"><span class="${label}">${damageEffectivenessLabel}</span><span class="${value} font-mono">${String.format("%.0f", current.baseMultiplier())}%</span></div>
				@endif
			@endif
		</div>

		<%-- 레벨별 스탯 라인 (파랑) --%>
		!{java.util.List<String> displayStatLines = current == null ? null : net.luversof.web.gate.poe.PoeText.lines(current.statLinesKo(), current.statLines());}
		@if(displayStatLines != null && !displayStatLines.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center space-y-0.5">
				@for(String statLine : displayStatLines)
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">${statLine}</div>
				@endfor
			</div>
		@endif

		<%-- 설명 (기울임 회색) --%>
		@if(displayDescription != null && !displayDescription.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center">
				<p class="text-[11px] leading-5 italic text-white/45">${displayDescription}</p>
			</div>
		@endif

		<%-- 레벨 내비 + 타입 배지 --%>
		<div class="px-4 py-2 border-t border-white/10">
			<div class="flex flex-wrap gap-1 items-center">
				<span class="text-[11px] text-white/40 mr-1">${levelLabel}</span>
				@for(int step : levelSteps)
					<button type="button"
					        class="px-1.5 py-0.5 rounded text-[11px] ${step == displayLevel ? "bg-[#1ba29b] text-white" : "bg-white/5 text-white/60 hover:bg-white/10"}"
					        hx-get="/poe/htmx/gems/detail?slug=${gem.slug()}&level=${step}"
					        hx-target="#poePreview"
					        hx-swap="innerHTML">${step}</button>
				@endfor
				<span class="ml-auto text-[10px] px-1.5 py-0.5 rounded border ${gem.isSupport() ? "border-fuchsia-500/40 text-fuchsia-400" : "border-cyan-500/40 text-cyan-400"}">${gem.isSupport() ? supportBadgeLabel : activeBadgeLabel}</span>
			</div>
		</div>
	</div>
