@import net.luversof.web.gate.poe.dto.PoeGem
@import net.luversof.web.gate.poe.dto.PoeGemLevel
@import net.luversof.web.gate.poe.dto.PoeReminderText
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

<%-- 젬의 둘째 효과 구역(10-29 R11 — GemEffects.GrantedEffect2, PoB GemTooltip.AddGemTooltip): 바알 젬은 기본 스킬이 먼저(showLevel),
     그 밖(지옥불 함성 → 발화 등)은 주 구역 뒤에 소제목(header)과 함께. 수치 줄 모양 · 순서는 gemDetail 주 구역과 같다 --%>
@param PoeGem.Secondary sec
@param int displayLevel = 20
@param boolean attack = false
@param boolean showLevel = false
@param String header = null
@param java.util.Map<String, PoeReminderText> reminderText = null
@param String nameColor = "#1ba29b"

!{
	PoeGemLevel cur = sec == null || sec.levels() == null ? null : sec.levels().stream().filter(l -> l.level() == displayLevel).findFirst().orElse(null);
	java.util.List<String> lines = cur == null ? null : PoeText.lines(cur.statLinesKo(), cur.statLines());
	String label = "text-[#7f7f7f]";
	String value = "text-white";
}
@if(cur != null)
	@if(header != null)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center font-semibold text-xs" style="color:${nameColor}" data-gem-secondary-title>${header}</div>
	@endif
	<div class="poe-tt-sepimg"></div>
	<div class="px-4 py-1.5 text-xs text-center space-y-0.5" data-gem-secondary>
		@if(showLevel)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.level")}: </span><span class="${value}" data-tt-gem-level>${displayLevel}</span></div>
		@endif
		@if(cur.cost() != null && cur.cost() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cost")}: </span><span class="${value}">${PoeText.costValue(cur.cost(), cur.costType())}@if(cur.extraCosts() != null)@for(int ci = 0; ci < cur.extraCosts().size(); ci++), ${PoeText.costValue(cur.extraCosts().get(ci), cur.extraCostTypes().get(ci))}@endfor@endif</span></div>
		@endif
		@if(cur.cooldownMs() != null)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cooldown")}: </span><span class="${value}">${cur.cooldownUses() != null ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.value.seconds.uses"), String.format("%.2f", cur.cooldownMs() / 1000.0), String.valueOf(cur.cooldownUses())) : java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.value.seconds"), String.format("%.2f", cur.cooldownMs() / 1000.0))}</span></div>
		@endif
		@if(sec.castTimeMs() != null && !attack)
			<div data-gem-cast-time><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cast.time")}: </span><span class="${value}">${sec.castTimeMs() > 0 ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.value.seconds"), String.format("%.2f", sec.castTimeMs() / 1000.0)) : MessageUtil.getMessage("poe.gems.cast.instant")}</span></div>
		@endif
		@if(cur.critChance() != null)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.crit.chance")}: </span><span class="${value}">${String.format("%.2f", cur.critChance())}%</span></div>
		@endif
		@if(cur.attackSpeedPct() != null)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.attack.speed")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.of.base"), String.valueOf(cur.attackSpeedPct()))}</span></div>
		@endif
		@if(attack && cur.attackTimeMs() != null)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.attack.time")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.value.seconds"), String.format("%.2f", cur.attackTimeMs() / 1000.0))}</span></div>
		@endif
		@if(cur.baseMultiplier() != null)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.attack.damage")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.of.base"), cur.baseMultiplier() % 1 == 0 ? String.valueOf(cur.baseMultiplier().longValue()) : String.format("%.1f", cur.baseMultiplier()))}</span></div>
		@endif
		@if(cur.damageEffectiveness() != null)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.damage.effectiveness")}: </span><span class="${value}">${String.format("%.0f", cur.damageEffectiveness())}%</span></div>
		@endif
	</div>
	@if(lines != null && !lines.isEmpty())
		!{
			java.util.List<java.util.List<String>> remIds = cur.statReminders();
			boolean remPerLine = remIds != null && reminderText != null && remIds.size() == lines.size();
		}
		<div class="px-4 py-1.5 text-center space-y-0.5" data-gem-secondary-stats>
			@for(int si = 0; si < lines.size(); si++)
				<div class="text-xs leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(lines.get(si))}</div>
				@if(remPerLine && remIds.get(si) != null)
					@for(String rid : remIds.get(si))
						@if(reminderText.get(rid) != null)
							<div class="text-2xs leading-snug text-[#7f7f7f]" data-tt-reminder>${PoeText.name(reminderText.get(rid).ko(), reminderText.get(rid).en())}</div>
						@endif
					@endfor
				@endif
			@endfor
		</div>
	@endif
@endif
