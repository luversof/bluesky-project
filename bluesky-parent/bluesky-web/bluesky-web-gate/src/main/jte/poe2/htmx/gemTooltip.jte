@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.Gem gem
<%-- 보일 레벨 — null 이면 최대 레벨(목록 호버). 레벨 수치가 없는 젬(메타 등)은 설명만. --%>
@param Integer level = null
<%-- 상세 페이지에 박힌 툴팁이면 true — 페이지가 레벨 단추를 툴팁 밖에 따로 두므로 안쪽 단추는 그리지 않는다 --%>
@param boolean pageMode = false

!{
	// 인게임 젬 툴팁 — 이름은 젬 희귀도 색(청록), 태그 줄 회색, 설명 금색, 보조 설명은 설명과 같은 자리(PoE1 툴팁과 같은 배치)
	String nameColor = "#1ba29b";
	String label = "text-[#7f7f7f]";
	String value = "text-white";
	java.util.List<String> tags = PoeText.lines(gem.tagsKo(), gem.tags());
	String description = PoeText.name(gem.descriptionKo(), gem.description());
	String supportText = PoeText.name(gem.supportTextKo(), gem.supportText());
	java.util.List<String> family = PoeText.lines(gem.familyKo(), gem.family());
	Poe2.GemLevel lv = null;
	if (gem.levels() != null && !gem.levels().isEmpty()) {
		lv = gem.levels().get(gem.levels().size() - 1);
		if (level != null) for (Poe2.GemLevel x : gem.levels()) if (x.level() != null && x.level().equals(level)) lv = x;
	}
	java.util.List<String> statLines = lv == null ? java.util.List.of() : PoeText.lines(lv.statLinesKo(), lv.statLines());
	// 스탯 묶음 머리글(10-09 C236 — PoE2 GrantedEffectStatSets.Label → GrantedEffectLabels, poe2db 얼음 화살 "투사체" / "폭파").
	// 주 묶음 머리글은 추가 묶음이 있을 때만 — 라벨이 없거나 Hidden 이면 젬 이름(poe2db 구형 번개 · 얼음 폭발)
	java.util.List<Poe2.GemStatSet> extraSets = lv == null || lv.statSets() == null ? java.util.List.of() : lv.statSets();
	String setLabel = PoeText.name(gem.statSetLabelKo(), gem.statSetLabel());
	// 부여 스킬이 있어도 주 칸 머리글을 단다 — 라벨이 없으면 보조 젬은 "보조", 스킬 젬은 젬 이름(10-10 C249, poe2db 세차게 흐르는 전류 · 점멸)
	java.util.List<Poe2.GrantedSkillLevel> grantedLv = lv == null || lv.grantedSkills() == null || gem.grantedSkills() == null ? java.util.List.of() : lv.grantedSkills();
	if (setLabel == null || setLabel.isBlank()) setLabel = "support".equals(gem.kind()) ? MessageUtil.getMessage("poe2.gems.kind.support") : PoeText.name(gem.nameKo(), gem.name());
	if (extraSets.isEmpty() && grantedLv.isEmpty()) setLabel = null;
	// 요구 능력치 비율(%) — PoE2 젬은 레벨이 오를수록 요구치가 이 비율로 는다. 어느 능력치를 요구하는지가 핵심이라 이름만 보인다.
	java.util.List<String> attrs = new java.util.ArrayList<>();
	if (gem.reqStr() != null && gem.reqStr() > 0) attrs.add(MessageUtil.getMessage("poe.items.detail.str"));
	if (gem.reqDex() != null && gem.reqDex() > 0) attrs.add(MessageUtil.getMessage("poe.items.detail.dex"));
	if (gem.reqInt() != null && gem.reqInt() > 0) attrs.add(MessageUtil.getMessage("poe.items.detail.int"));
	// 요구 능력치 수치(10-09 C227 — PoB2 getGemStatRequirement, poe2db 얼음 화살 90레벨 "157 민첩"). 8 미만은 요구 없음
	java.util.List<String> attrsNum = new java.util.ArrayList<>();
	if (lv != null && lv.requiredLevel() != null) {
		boolean sup = "support".equals(gem.kind());
		int rs = net.luversof.web.gate.poe.PoeGemRequirements.poe2Stat(lv.requiredLevel(), sup, gem.reqStr());
		int rd = net.luversof.web.gate.poe.PoeGemRequirements.poe2Stat(lv.requiredLevel(), sup, gem.reqDex());
		int ri = net.luversof.web.gate.poe.PoeGemRequirements.poe2Stat(lv.requiredLevel(), sup, gem.reqInt());
		if (rs > 0) attrsNum.add(rs + " " + MessageUtil.getMessage("poe.items.detail.str"));
		if (rd > 0) attrsNum.add(rd + " " + MessageUtil.getMessage("poe.items.detail.dex"));
		if (ri > 0) attrsNum.add(ri + " " + MessageUtil.getMessage("poe.items.detail.int"));
	}
}

<div class="poe-tooltip poe-rar-gem poe-tt-w-gem max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${nameColor}">
	<div class="poe-tt-band">
		<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${PoeText.name(gem.nameKo(), gem.name())}</div>
	</div>
	@if(tags != null && !tags.isEmpty())
		<div class="px-4 pt-1 text-center text-[11.5px] text-[#7f7f7f]">${String.join(", ", tags)}</div>
	@endif
	<%-- 유형(10-09 C231 — 게임 ClientStrings SupportFamilyDisplayString "유형", poe2db 처럼 태그 바로 아래) --%>
	@if(family != null && !family.isEmpty())
		<div class="px-4 pt-0.5 text-center text-[11.5px]" data-gem-family><span class="${label}">${MessageUtil.getMessage("poe2.gems.family")}: </span><span class="${value}">${String.join(", ", family)}</span>@if(Boolean.TRUE.equals(gem.lineage()))<span class="ml-1 text-[#af6025]">(${MessageUtil.getMessage("poe2.gems.lineage")})</span>@endif</div>
	@endif
	<div class="poe-tt-sepimg"></div>
	<div class="px-4 py-1.5 text-[12px] text-center space-y-0.5">
		@if(lv != null && gem.maxLevel() != null && gem.maxLevel() > 1)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.level")}: </span><span class="${value}">${lv.level()}</span><span class="${label}"> / ${gem.maxLevel()}</span></div>
		@endif
		@if(gem.tier() != null && gem.tier() > 0)
			<div><span class="${label}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.tier"), gem.tier())}</span></div>
		@endif
		<%-- 보조 젬 소모 · 점유 배율(10-09 C231 — PoB2 manaMultiplier · reservationMultiplier, PoB2 GemTooltip.lua 처럼 보조 젬만 · 세 갈래:
		     같으면 CostAndReservationMultiplier · 점유만이면 ReservationMultiplier · 아니면 CostMultiplier) --%>
		!{ Integer cmul = lv == null ? null : lv.costMultiplierPct(); Integer rmul = lv == null ? null : lv.reservationMultiplierPct(); }
		@if("support".equals(gem.kind()) && (cmul != null || rmul != null))
			<div data-gem-cost-multiplier><span class="${label}">${MessageUtil.getMessage(cmul != null && cmul.equals(rmul) ? "poe2.gems.cost.reservation.multiplier" : rmul != null ? "poe2.gems.reservation.multiplier" : "poe2.gems.cost.multiplier")}: </span><span class="${value}">${rmul != null && (cmul == null || !cmul.equals(rmul)) ? rmul : cmul}%</span></div>
		@endif
		@if(lv != null && lv.cost() != null && lv.cost() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cost")}: </span><span class="${value}">${PoeText.costValue(lv.cost(), lv.costType())}</span></div>
		@endif
		@if(lv != null && lv.reservation() != null && lv.reservation() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe2.gems.reservation")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.spirit.value"), lv.reservation())}</span></div>
		@endif
		<%-- 시전 속도 — 공격 젬엔 없다, 0 이면 "즉시 시전"(10-09 C227 — PoB2 GemTooltip.lua · ClientStrings SkillIsInstant, 예전엔 얼음 화살에도 "1.00초") --%>
		@if(gem.castTimeMs() != null && !"support".equals(gem.kind()) && (gem.tags() == null || !gem.tags().contains("Attack")))
			<div data-gem-cast-time><span class="${label}">${MessageUtil.getMessage("poe2.tt.cast.time")}: </span><span class="${value}">${gem.castTimeMs() > 0 ? java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.value.seconds"), String.format("%.2f", gem.castTimeMs() / 1000.0)) : MessageUtil.getMessage("poe.gems.cast.instant")}</span></div>
		@endif
		@if(lv != null && lv.cooldownMs() != null && lv.cooldownMs() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cooldown")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.value.seconds"), String.format("%.2f", lv.cooldownMs() / 1000.0))}</span></div>
		@endif
		@if(lv != null && lv.critChance() != null && lv.critChance() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe2.tt.crit")}: </span><span class="${value}">${String.format("%.2f", lv.critChance())}%</span></div>
		@endif
		<%-- 공격 스킬 배율(10-08 C200 — 게임 ClientStrings ItemDisplaySkillGemAttackDamageEffectiveness "공격 피해" · AttackSpeedMultiplier "공격 속도" · PercentageOfBase "기본 수치의 {0}%") --%>
		<%-- 공격 속도 → 공격 피해 순(10-09 C227 — PoB2 GemTooltip.lua · poe2db 순서) --%>
		@if(lv != null && lv.attackSpeedPct() != null)
			<div data-gem-attack-speed><span class="${label}">${MessageUtil.getMessage("poe2.gems.attack.speed")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.of.base"), lv.attackSpeedPct())}</span></div>
		@endif
		@if(lv != null && lv.attackDamagePct() != null)
			<div data-gem-attack-damage><span class="${label}">${MessageUtil.getMessage("poe2.gems.attack.damage")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.of.base"), lv.attackDamagePct())}</span></div>
		@endif
		@if(gem.weaponRequirements() != null)
			<div><span class="${label}">${MessageUtil.getMessage("poe2.gems.weapon")}: </span><span class="${value}" data-gem-weapon>${PoeText.name(gem.weaponRequirementsKo(), gem.weaponRequirements())}</span></div>
		@endif
		<%-- 요구 사항 — PoE1 젬 툴팁과 같은 "요구 레벨 N, 능력치" 줄(10-01: 레벨이 없었다). 요구 레벨 = PoB levelRequirement --%>
		<%-- 보조 요구사항(10-09 C233 — ClientStrings SupportGemRequirementsLabel "보조 요구사항:", poe2db: 비율이 가장 높은 능력치 +5 — 포악함 I·III · 무기력 ·
		     빙결 확률 · 아흔의 성채 다섯 표본이 모두 그렇다(혼합 40/60 도 큰 쪽만). 보조 젬 레벨 줄은 1 이면 뺀다(poe2db 와 같음) --%>
		@if("support".equals(gem.kind()))
			!{ int sr = gem.reqStr() == null ? 0 : gem.reqStr(), dr = gem.reqDex() == null ? 0 : gem.reqDex(), ir = gem.reqInt() == null ? 0 : gem.reqInt();
			   String supAttr = sr == 0 && dr == 0 && ir == 0 ? null : (sr >= dr && sr >= ir ? "poe.items.detail.str" : dr >= ir ? "poe.items.detail.dex" : "poe.items.detail.int"); }
			@if(lv != null && lv.requiredLevel() != null && lv.requiredLevel() > 1)
				<div data-gem-req><span class="${label}">${MessageUtil.getMessage("poe2.tt.requires")} ${MessageUtil.getMessage("poe.items.detail.level")} </span><span class="${value}">${lv.requiredLevel()}</span></div>
			@endif
			@if(supAttr != null)
				<div data-gem-support-req><span class="${label}">${MessageUtil.getMessage("poe2.gems.support.requirements")} </span><span class="${value}">+5 ${MessageUtil.getMessage(supAttr)}</span></div>
			@endif
		@elseif(lv != null && lv.requiredLevel() != null)
			<%-- 게임 문구 "요구 사항: 레벨 14, 민첩"(PoE2 클라이언트, 10-02 — 예전엔 "요구 레벨 14, 민첩") --%>
			<div data-gem-req><span class="${label}">${MessageUtil.getMessage("poe2.tt.requires")} ${MessageUtil.getMessage("poe.items.detail.level")} </span><span class="${value}">${lv.requiredLevel()}</span>@if(!attrsNum.isEmpty())<span class="${label}">, </span><span class="${value}">${String.join(", ", attrsNum)}</span>@endif</div>
		@elseif(!attrs.isEmpty())
			<div><span class="${label}">${MessageUtil.getMessage("poe2.tt.requires")} </span><span class="${value}">${String.join(", ", attrs)}</span></div>
		@endif
	</div>
	@if(description != null && !description.isBlank())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] leading-snug text-center text-[#b4966a] whitespace-pre-line">${description}</div>
	@endif
	<%-- 보조 젬 설명은 스탯 앞(10-09 C231 — poe2db 순서: 설명 → 스탯) --%>
	@if(supportText != null && !supportText.isBlank())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] leading-snug text-center text-[#b4966a] whitespace-pre-line">${supportText}</div>
	@endif
	@if(!statLines.isEmpty() || setLabel != null)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center space-y-0.5" data-gem-statset>
			@if(setLabel != null)
				<div class="text-[12.5px] text-white" data-gem-statset-label>${setLabel}</div>
			@endif
			@for(String line : statLines)
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endif
	<%-- 추가 스탯 묶음(10-09 C236 — GrantedEffects.AdditionalStatSets, 묶음마다 구분선 · 머리글 · 묶음 고유 치명타/공격 피해 · 스탯 줄) --%>
	@for(Poe2.GemStatSet set : extraSets)
		!{ String setName = PoeText.name(set.labelKo(), set.label()); java.util.List<String> setLines = PoeText.lines(set.statLinesKo(), set.statLines()); }
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center space-y-0.5" data-gem-statset-extra>
			@if(setName != null && !setName.isBlank())
				<div class="text-[12.5px] text-white" data-gem-statset-label>${setName}</div>
			@endif
			@if(set.critChance() != null && set.critChance() > 0)
				<div class="text-[12px]"><span class="${label}">${MessageUtil.getMessage("poe2.tt.crit")}: </span><span class="${value}">${String.format("%.2f", set.critChance())}%</span></div>
			@endif
			@if(set.attackDamagePct() != null)
				<div class="text-[12px]"><span class="${label}">${MessageUtil.getMessage("poe2.gems.attack.damage")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.of.base"), set.attackDamagePct())}</span></div>
			@endif
			<%-- 속성 줄과 스탯 줄 사이 구분선(poe2db 구형 번개 "화염 주입": 치명타 명중 확률 → 구분선 → 스탯) --%>
			@if(((set.critChance() != null && set.critChance() > 0) || set.attackDamagePct() != null) && setLines != null && !setLines.isEmpty())
				<div class="poe-tt-sepimg -mx-4 my-1" data-gem-statset-sep></div>
			@endif
			@if(setLines != null)
				@for(String line : setLines)
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
				@endfor
			@endif
		</div>
	@endfor
	<%-- 젬이 덤으로 주는 스킬(10-10 C249 — GemEffects.AdditionalGrantedEffects, poe2db 세차게 흐르는 전류 · 점멸 · 화산 걸음): 칸마다 머리글(라벨, 없으면 이름) ·
	     "젬의 레벨을 따릅니다"(ClientStrings ItemDisplaySkillGemLevelWillCopyLevelFromParent) · 소모 · 재사용 · 시전(공격 아닐 때) · 치명타 · 공격 피해 → 설명 → 스탯 줄 --%>
	@for(int gk = 0; gk < grantedLv.size() && gk < gem.grantedSkills().size(); gk++)
		!{ Poe2.GrantedSkill gs = gem.grantedSkills().get(gk); Poe2.GrantedSkillLevel gl = grantedLv.get(gk); }
		@if(gl != null)
			!{ String gTitle = PoeText.name(gs.labelKo(), gs.label()); if (gTitle == null || gTitle.isBlank()) gTitle = PoeText.name(gs.nameKo(), gs.name()); if ((gTitle == null || gTitle.isBlank()) && Boolean.TRUE.equals(gs.support())) gTitle = MessageUtil.getMessage("poe2.gems.kind.support"); String gDesc = PoeText.name(gs.descriptionKo(), gs.description()); java.util.List<String> gLines = PoeText.lines(gl.statLinesKo(), gl.statLines());
				// 속성 줄 · 설명이 없는 칸(메타 젬 "보조" · 석궁 사격 "탄약")은 머리글 바로 아래 스탯 줄(poe2db, 사이 구분선 없음)
				boolean gHead = Boolean.TRUE.equals(gs.copiesLevel()) || (gl.cost() != null && gl.cost() > 0) || (gl.cooldownMs() != null && gl.cooldownMs() > 0) || (gs.castTimeMs() != null && gl.attackDamagePct() == null) || (gl.critChance() != null && gl.critChance() > 0) || gl.attackDamagePct() != null || (gDesc != null && !gDesc.isBlank()); }
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center space-y-0.5 text-[12px]" data-gem-granted>
				<div class="text-[12.5px] text-white" data-gem-statset-label>${gTitle}</div>
				@if(Boolean.TRUE.equals(gs.copiesLevel()))
					<div class="${label}" data-gem-granted-copy>${MessageUtil.getMessage("poe2.gems.granted.copy.level")}</div>
				@endif
				@if(gl.cost() != null && gl.cost() > 0)
					<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cost")}: </span><span class="${value}">${PoeText.costValue(gl.cost(), gl.costType())}</span></div>
				@endif
				@if(gl.cooldownMs() != null && gl.cooldownMs() > 0)
					<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cooldown")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.value.seconds"), String.format("%.2f", gl.cooldownMs() / 1000.0))}</span></div>
				@endif
				@if(gs.castTimeMs() != null && gl.attackDamagePct() == null)
					<div data-gem-granted-cast><span class="${label}">${MessageUtil.getMessage("poe2.tt.cast.time")}: </span><span class="${value}">${gs.castTimeMs() > 0 ? java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.value.seconds"), String.format("%.2f", gs.castTimeMs() / 1000.0)) : MessageUtil.getMessage("poe.gems.cast.instant")}</span></div>
				@endif
				@if(gl.critChance() != null && gl.critChance() > 0)
					<div><span class="${label}">${MessageUtil.getMessage("poe2.tt.crit")}: </span><span class="${value}">${String.format("%.2f", gl.critChance())}%</span></div>
				@endif
				@if(gl.attackDamagePct() != null)
					<div><span class="${label}">${MessageUtil.getMessage("poe2.gems.attack.damage")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.of.base"), gl.attackDamagePct())}</span></div>
				@endif
			</div>
			@if(gDesc != null && !gDesc.isBlank())
				<div class="poe-tt-sepimg"></div>
				<div class="px-4 py-1.5 text-[12px] leading-snug text-center text-[#b4966a] whitespace-pre-line" data-gem-granted-desc>${gDesc}</div>
			@endif
			@if(gLines != null && !gLines.isEmpty())
				@if(gHead)
					<div class="poe-tt-sepimg"></div>
				@endif
				<div class="px-4 py-1.5 text-center space-y-0.5" data-gem-granted-lines>
					@for(String line : gLines)
						<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
					@endfor
				</div>
			@endif
			<%-- 부여 스킬의 추가 스탯 묶음(poe2db 촉진시키는 방출 "화염" · "번개" · "냉기") — 주 스킬 추가 묶음과 같은 칸 --%>
			@if(gl.statSets() != null)
				@for(Poe2.GemStatSet gset : gl.statSets())
					!{ String gsName = PoeText.name(gset.labelKo(), gset.label()); java.util.List<String> gsLines = PoeText.lines(gset.statLinesKo(), gset.statLines()); }
					<div class="poe-tt-sepimg"></div>
					<div class="px-4 py-1.5 text-center space-y-0.5" data-gem-granted-set>
						@if(gsName != null && !gsName.isBlank())
							<div class="text-[12.5px] text-white" data-gem-statset-label>${gsName}</div>
						@endif
						@if(gset.critChance() != null && gset.critChance() > 0)
							<div class="text-[12px]"><span class="${label}">${MessageUtil.getMessage("poe2.tt.crit")}: </span><span class="${value}">${String.format("%.2f", gset.critChance())}%</span></div>
						@endif
						@if(gsLines != null)
							@for(String line : gsLines)
								<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
							@endfor
						@endif
					</div>
				@endfor
			@endif
		@endif
	@endfor
	<%-- 퀄리티로 인한 추가 효과(10-08 C195, PoE1 젬 상세 짝 — 게임 ClientStrings ItemDescriptionGemQualityStatDivider "퀄리티로 인한 추가 효과:", 퀄리티 20% 값) --%>
	!{ java.util.List<String> qualityLines = PoeText.lines(gem.qualityStatLinesKo(), gem.qualityStatLines()); }
	@if(qualityLines != null && !qualityLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center space-y-0.5" data-tt-quality-effects>
			<div class="text-[12px] text-white/60">${MessageUtil.getMessage("poe.gems.detail.quality")}:</div>
			@for(String line : qualityLines)
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endif
	<%-- 안내(10-09 C231 — PoE2 ClientStrings ItemDescriptionActiveSkillGem "스킬은 스킬 창에서 관리할 수 있습니다." / ItemDescriptionSupportSkillGem,
	     poe2db 와 같은 맨 아래 문단. 예전 "같은 계열 보조젬은 …" 자체 문구는 게임 안내가 같은 내용을 담아 뺐다) --%>
	<div class="poe-tt-sepimg"></div>
	<div class="px-4 py-1.5 text-[11.5px] leading-snug text-center text-[#7f7f7f]" data-gem-help>${MessageUtil.getMessage("support".equals(gem.kind()) ? "poe2.gems.help.support" : "poe2.gems.help.active")}</div>
	@if(PoeText.isKorean())
		<div class="px-4 pb-2 text-center text-[10px] text-white/30">${gem.name()}</div>
	@endif
	<%-- 키워드 설명(10-04 C111) — 고유 툴팁과 같은 조각 --%>
	@template.poe2.htmx.keywordBox(keywords = gem.keywords())
	<%-- 레벨 단추 + 종류 배지 — PoE1 젬 툴팁(poe/htmx/gemDetail.jte)과 같은 자리·모양(10-01). 호버 레이어는 클릭하면 고정돼 단추를 누를 수 있다.
	     레벨 1 · 5의 배수 · 최대 레벨. 상세 페이지(pageMode)는 툴팁 밖에 같은 단추가 있어 여기선 뺀다. --%>
	@if(!pageMode && lv != null && gem.levels().size() > 1)
		!{
			java.util.List<Integer> steps = new java.util.ArrayList<>();
			for (Poe2.GemLevel x : gem.levels()) if (x.level() != null && (x.level() == 1 || x.level() % 5 == 0 || x.level().equals(gem.maxLevel()))) steps.add(x.level());
		}
		<div class="px-4 py-2 border-t border-white/10" data-gem-levels>
			<div class="flex flex-wrap gap-1 items-center">
				<span class="text-[11px] text-white/40 mr-1">${MessageUtil.getMessage("poe.gems.detail.level")}</span>
				@for(Integer step : steps)
					<button type="button"
					        class="px-1.5 py-0.5 rounded text-[11px] ${step.equals(lv.level()) ? "bg-[#1ba29b] text-white" : "bg-white/5 text-white/60 hover:bg-white/10"}"
					        hx-get="/poe2/htmx/gems/detail?slug=${gem.slug()}&level=${step}"
					        hx-target="#poePreview"
					        hx-swap="innerHTML">${step}</button>
				@endfor
				<span class="ml-auto text-[10px] px-1.5 py-0.5 rounded border ${"support".equals(gem.kind()) ? "border-fuchsia-500/40 text-fuchsia-400" : "border-cyan-500/40 text-cyan-400"}">${"support".equals(gem.kind()) ? MessageUtil.getMessage("poe2.gems.type.support") : MessageUtil.getMessage("poe.gems.type.active")}</span>
			</div>
		</div>
	@endif
</div>
