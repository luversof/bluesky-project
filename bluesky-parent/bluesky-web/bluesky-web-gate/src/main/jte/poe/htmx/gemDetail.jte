@import net.luversof.web.gate.poe.dto.PoeGem
@import net.luversof.web.gate.poe.dto.PoeGemLevel
@import net.luversof.web.gate.poe.PoeText
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
	String damageLabel = MessageUtil.getMessage("poe.gems.detail.damage");
	String damageEffectivenessLabel = MessageUtil.getMessage("poe.gems.detail.damage.effectiveness");
	String qualityLabel = MessageUtil.getMessage("poe.gems.detail.quality");
	String socketHint = MessageUtil.getMessage("poe.gems.detail.socket.hint");
	String supportBadgeLabel = MessageUtil.getMessage("poe.gems.type.support");
	String activeBadgeLabel = MessageUtil.getMessage("poe.gems.type.active");

	// 젬 색상 = 이름 색 (게임 툴팁 관례)
	String nameColor = switch (gem.color()) {
		case "red" -> "#e05a4f";
		case "green" -> "#4fb96a";
		case "blue" -> "#6a8fe0";
		default -> "#d8d8d8";
	};
	String displayName = PoeText.name(gem.nameKo(), gem.name());
	String displayDescription = PoeText.name(gem.descriptionKo(), gem.description());
	java.util.List<String> displayTags = PoeText.lines(gem.tagsKo(), gem.tags());

	PoeGemLevel current = gem.levels().stream().filter(l -> l.level() == displayLevel).findFirst().orElse(null);
	java.util.List<Integer> levelSteps = new java.util.ArrayList<>();
	for (PoeGemLevel l : gem.levels()) {
		if (l.level() == 1 || l.level() % 5 == 0) levelSteps.add(l.level());
	}
	java.util.List<String> displayStatLines = current == null ? null : PoeText.lines(current.statLinesKo(), current.statLines());
	// 게임 툴팁 색: 라벨 회색, 수치 흰색, 스킬 설명 금색, 스탯(모드) 파랑
	String label = "text-[#7f7f7f]";
	String value = "text-white";
}

<%-- PoE 게임 스킬젬 툴팁 (인게임 배치: 헤더 → 스탯 → 설명(금색) → 스탯라인(파랑) → 퀄리티 → 소켓안내) --%>
	<div class="poe-tooltip poe-rar-gem poe-tt-w-gem max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:#1ba29b">
		<%-- 헤더 밴드 (이름) --%>
		<div class="poe-tt-band">
			<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${displayName}</div>
		</div>

		<%-- 태그 (인게임: 헤더 아래 젬 색) --%>
		@if(displayTags != null && !displayTags.isEmpty())
			<div class="px-4 pt-1 text-center text-[11.5px]" style="color:${nameColor}">${String.join(", ", displayTags)}</div>
		@endif

		<%-- 레벨/요구/소모/피해 등 수치 (중앙 정렬, 라벨 회색 · 수치 흰색) --%>
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] text-center space-y-0.5">
			<div><span class="${label}">${levelLabel}: </span><span class="${value}">${displayLevel}</span></div>
			@if(current != null)
				@if(current.cost() != null && current.cost() > 0)
					<div><span class="${label}">${costLabel}: </span><span class="${value}">${current.costType()} ${current.cost()}</span></div>
				@endif
				@if(current.costMultiplier() != null && current.costMultiplier() != 100)
					<div><span class="${label}">${costMultiplierLabel}: </span><span class="${value}">${current.costMultiplier()}%</span></div>
				@endif
				@if(gem.castTimeMs() != null)
					<div><span class="${label}">${castTimeLabel}: </span><span class="${value}">${String.format("%.2f", gem.castTimeMs() / 1000.0)}s</span></div>
				@endif
				@if(current.cooldownMs() != null)
					<div><span class="${label}">${cooldownLabel}: </span><span class="${value}">${String.format("%.2f", current.cooldownMs() / 1000.0)}s</span></div>
				@endif
				@if(current.critChance() != null)
					<div><span class="${label}">${critChanceLabel}: </span><span class="${value}">${String.format("%.2f", current.critChance())}%</span></div>
				@endif
				@if(current.baseMultiplier() != null)
					<div><span class="${label}">${damageLabel}: </span><span class="${value}">${String.format("%.0f", current.baseMultiplier())}%</span></div>
				@endif
				@if(current.damageEffectiveness() != null)
					<div><span class="${label}">${damageEffectivenessLabel}: </span><span class="${value}">${String.format("%.0f", current.damageEffectiveness())}%</span></div>
				@endif
				<div class="pt-0.5"><span class="${label}">${requiredLevelLabel} </span><span class="${value}">${current.requiredLevel()}</span></div>
			@endif
		</div>

		<%-- 스킬 설명 (녹색, 구분선 사이 — 인게임 배치) --%>
		@if(displayDescription != null && !displayDescription.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center">
				<p class="text-[12px] leading-5" style="color:#68c9a0">${displayDescription}</p>
			</div>
		@endif

		<%-- 레벨별 스탯 라인 (파랑) --%>
		@if(displayStatLines != null && !displayStatLines.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center space-y-0.5">
				@for(String statLine : displayStatLines)
					<div class="text-[12.5px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(statLine)}</div>
				@endfor
			</div>
		@endif

		<%-- 퀄리티(20%)로 인한 추가 효과 (모드와 같은 블록 흐름 — 인게임엔 구분선 없이 한 칸 띄움) --%>
		!{java.util.List<String> displayQuality = PoeText.lines(gem.qualityStatLinesKo(), gem.qualityStatLines());}
		@if(displayQuality != null && !displayQuality.isEmpty())
			<div class="px-4 pb-1.5 pt-2 text-center space-y-0.5">
				<div class="text-[12.5px] text-white/60">${qualityLabel}:</div>
				@for(String qualityLine : displayQuality)
					<div class="text-[12.5px] leading-snug text-[#8888ff]">$unsafe{PoeText.highlightValues(qualityLine)}</div>
				@endfor
			</div>
		@endif

		<%-- 소켓 안내 (하단 회색 이탤릭) + 영문명 --%>
		<div class="px-4 py-2 text-center border-t border-white/10">
			<p class="text-[11px] leading-5 italic text-white/35">${socketHint}</p>
			@if(PoeText.isKorean())
				<p class="text-[11px] italic text-white/25 mt-0.5">${gem.name()}</p>
			@endif
		</div>

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
