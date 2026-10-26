@import net.luversof.web.gate.poe2.dto.Poe2
@import static net.luversof.web.gate.poe2.Poe2View.nz
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.BaseItem base
<%-- 품질 %(빌드 툴팁, 10-04 C148) — 인게임 속성 칸 첫 줄 "품질: +N%"(값 마법 파랑, PoE1 itemTooltip 과 같은 모양) --%>
@param int quality = 0
<%-- 아이템 옵션 줄(영문 원문) — 속성 칸에 그 아이템 자신의 로컬 옵션을 반영한다(10-08 C182, 해질녘 "(350-450)% increased Armour"). 베이스 툴팁은 없음 --%>
@param java.util.List<String> lines = null
<%-- 실제 방어 수치(10-08 C184 — 빌드 아이템, PoB 원문 최종값). null 이면 계산 --%>
@param net.luversof.web.gate.poe.dto.PoeBuild.Defences defences = null
<%-- 첫 구분선(10-23 Q24) — 제작 카드는 머리 바로 아래라 끈다(인게임엔 머리와 속성 사이 구분선이 없다), 상세 툴팁은 종류 줄 다음이라 켠다 --%>
@param boolean leadSep = true
<%-- 무기 종류 줄(10-23 Q62) — 인게임은 무기만 속성 칸 첫 줄 회색('Crossbow' · '석궁'), 그 아래 바로 물리 피해. 무기가 아니면 안 씀 --%>
@param String classLine = null

!{
	// 베이스 속성(툴팁 가운데 칸) — 인게임처럼 라벨 회색 · 값 흰색. 고유는 옵션으로 값이 바뀌므로 베이스 값을 그대로 보인다.
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
	Poe2.Armour a = base.armour();
	Poe2.Weapon w = base.weapon();
	Poe2.Flask f = base.flask();
	boolean hasArmour = a != null && (nz(a.armour()) > 0 || nz(a.evasion()) > 0 || nz(a.energyShield()) > 0 || nz(a.block()) > 0 || nz(a.ward()) > 0);
	boolean hasWeapon = w != null && nz(w.damageMax()) > 0;
	// 로컬 옵션 반영(10-08 C182, PoB 식) — 옵션으로 바뀐 값은 인게임처럼 파란색. 품질도 곱한다(빌드 아이템의 실제 품질)
	net.luversof.web.gate.poe.PoeLocalProps.Result lp = net.luversof.web.gate.poe.PoeLocalProps.apply(new net.luversof.web.gate.poe.PoeLocalProps.Base(
		a == null ? 0 : nz(a.armour()), a == null ? 0 : nz(a.armour()), a == null ? 0 : nz(a.evasion()), a == null ? 0 : nz(a.evasion()),
		a == null ? 0 : nz(a.energyShield()), a == null ? 0 : nz(a.energyShield()), a == null ? 0 : nz(a.ward()), a == null ? 0 : nz(a.ward()), a == null ? 0 : nz(a.block()),
		hasWeapon ? nz(w.damageMin()) : 0, hasWeapon ? nz(w.damageMax()) : 0,
		hasWeapon && w.attacksPerSecond() != null ? w.attacksPerSecond() : 0, hasWeapon && w.critChance() != null ? w.critChance() : 0),
		lines, hasArmour, hasWeapon, quality);
	String aug = "text-[#8888ff]";
	boolean hasFlask = f != null && (nz(f.lifePerUse()) > 0 || nz(f.manaPerUse()) > 0 || (f.recoverySeconds() != null && f.recoverySeconds() > 0));
	// 정신력(셉터 베이스 속성 "정신력: 100", 10-08 C188 — 게임 ClientStrings ItemDisplaySpiritValue). 셉터에선 "+N to Spirit" · "N% increased Spirit" 가 로컬(PoB2 식)
	boolean hasSpirit = base.spirit() != null && base.spirit() > 0;
	net.luversof.web.gate.poe.PoeLocalProps.Range spiritR = hasSpirit ? net.luversof.web.gate.poe.PoeLocalProps.spirit(base.spirit(), lines) : null;
	// 플라스크 · 호신부 속성에 로컬 옵션 반영(10-08 C187, PoE1 C186 짝 — 회복량 · 지속, PoB 식). PoE2 베이스엔 충전 수치가 없어 충전 줄은 없다. 바뀐 숫자만 파랑
	net.luversof.web.gate.poe.PoeFlaskProps.Result fp = hasFlask ? net.luversof.web.gate.poe.PoeFlaskProps.apply(nz(f.lifePerUse()), nz(f.manaPerUse()), f.recoverySeconds() == null ? 0 : f.recoverySeconds(), nz(f.maxCharges()), nz(f.perCharge()), nz(f.lifePerUse()) == 0 && nz(f.manaPerUse()) == 0, lines, quality) : null;
	java.util.function.BiFunction<String, Boolean, String> fpNum = (v, ch) -> ch ? "<span class=\"text-[#8888ff]\">" + v + "</span>" : "<span class=\"text-[#e0e0e0]\">" + v + "</span>";
	String fdur = fp == null ? "" : fpNum.apply(fp.duration().seconds(1), fp.duration().changed() || (nz(f.lifePerUse()) == 0 && nz(f.manaPerUse()) == 0 && quality > 0));
	String flife = fp == null ? "" : fpNum.apply(fp.life().text(), fp.life().changed() || quality > 0);
	String fmana = fp == null ? "" : fpNum.apply(fp.mana().text(), fp.mana().changed() || quality > 0);
	String fmax = fp == null ? "" : fpNum.apply(fp.maxCharges().text(), fp.maxCharges().changed());
	String fused = fp == null ? "" : fpNum.apply(fp.chargesUsed().text(), fp.chargesUsed().changed());
}
@if(hasWeapon || hasArmour || hasFlask || hasSpirit || quality > 0)
	@if(leadSep)<div class="poe-tt-sepimg"></div>@endif
	<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
		@if(classLine != null && hasWeapon)<div class="${label}" data-tt-weapon-class>${classLine}</div>@endif
		@if(quality > 0)<div data-tt-quality><span class="${label}">${MessageUtil.getMessage("poe.items.detail.quality")}: </span><span class="text-[#8888ff]">+${quality}%</span></div>@endif
		@if(hasSpirit)<div data-tt-spirit><span class="${label}">${MessageUtil.getMessage("poe2.items.spirit")}: </span><span class="${spiritR.changed() ? "text-[#8888ff]" : value}">${spiritR.text()}</span></div>@endif
		@if(hasWeapon)
			<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.physical.damage")}: </span><span class="${lp.physMin().changed() || lp.physMax().changed() || quality > 0 ? aug : value}" data-tt-phys>${lp.physMin().damageText()}-${lp.physMax().damageText()}</span></div>
			<%-- 원소 · 카오스 추가 피해(10-08 C183, PoE1 고유 상세 짝 — 게임 ClientStrings ItemDisplayWeaponElementalDamage "원소 피해") --%>
			@if(lp.hasElemental())<div data-tt-ele><span class="${label}">${MessageUtil.getMessage("poe.items.detail.elemental.damage")}: </span>!{ boolean eleFirst = true; }@if(lp.fire().present())<span class="text-[#960000]">${lp.fire().text()}</span>!{ eleFirst = false; }@endif@if(lp.cold().present())${eleFirst ? "" : ", "}<span class="text-[#366492]">${lp.cold().text()}</span>!{ eleFirst = false; }@endif@if(lp.lightning().present())${eleFirst ? "" : ", "}<span class="text-[#ffd700]">${lp.lightning().text()}</span>@endif</div>@endif@if(lp.chaos().present())<div data-tt-chaos><span class="${label}">${MessageUtil.getMessage("poe.items.detail.chaos.damage")}: </span><span class="text-[#d02090]">${lp.chaos().text()}</span></div>@endif
			<div><span class="${label}">${MessageUtil.getMessage("poe2.tt.crit")}: </span><span class="${lp.critChance().changed() ? aug : value}">${lp.critChance().text2()}%</span></div>
			<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.attacks.per.second")}: </span><span class="${lp.attacksPerSecond().changed() ? aug : value}">${lp.attacksPerSecond().text2()}</span></div>
			@if(nz(w.reloadTime()) > 0)
				<div><span class="${label}">${MessageUtil.getMessage("poe2.items.reload")}: </span>!{ net.luversof.web.gate.poe.PoeLocalProps.Range rl = net.luversof.web.gate.poe.PoeLocalProps.reload(w.reloadTime() / 1000.0, lines); }<span class="${rl.changed() ? aug : value}" data-tt-reload>${rl.text2()}</span></div>
			@endif
		@endif
		@if(hasArmour)
			@if(lp.block().present())<div><span class="${label}">${MessageUtil.getMessage("poe2.items.block.chance")}: </span><span class="${lp.block().changed() ? aug : value}">${lp.block().text()}%</span></div>@endif
			@if(lp.armour().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.armour")}: </span><span class="${lp.armour().changed() || quality > 0 ? aug : value}" data-tt-armour>${defences != null && defences.armour() != null ? String.valueOf(defences.armour()) : lp.armour().text()}</span></div>@endif
			@if(lp.evasion().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.evasion")}: </span><span class="${lp.evasion().changed() || quality > 0 ? aug : value}">${defences != null && defences.evasion() != null ? String.valueOf(defences.evasion()) : lp.evasion().text()}</span></div>@endif
			@if(lp.energyShield().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.energy.shield")}: </span><span class="${lp.energyShield().changed() || quality > 0 ? aug : value}">${defences != null && defences.energyShield() != null ? String.valueOf(defences.energyShield()) : lp.energyShield().text()}</span></div>@endif
			<%-- 룬 수호(10-08 C185 — 게임 ClientStrings ItemDisplayArmourWard "[Ward|룬 수호]") --%>
			@if(lp.ward().present() || (defences != null && defences.ward() != null))<div><span class="${label}">${MessageUtil.getMessage("poe2.items.ward")}: </span><span class="${lp.ward().changed() || quality > 0 ? aug : value}" data-tt-ward>${defences != null && defences.ward() != null ? String.valueOf(defences.ward()) : lp.ward().text()}</span></div>@endif
			@if(a.movementPenalty() != null && a.movementPenalty() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe2.items.movement")}: </span><span class="${value}">-${String.format("%.0f", a.movementPenalty())}%</span></div>@endif
		@endif
		@if(hasFlask)
			@if(nz(f.lifePerUse()) > 0)
				<div class="${label}" data-tt-flask>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.life"), fdur, flife)}</div>
			@elseif(nz(f.manaPerUse()) > 0)
				<div class="${label}" data-tt-flask>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.mana"), fdur, fmana)}</div>
			@else
				<div class="${label}" data-tt-flask>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.duration"), fdur)}</div>
			@endif
			<%-- 충전(10-08 C189 — PoB2 베이스 chargesMax · chargesUsed, 인게임 "사용 시 충전 40회 중 30회 소모") · 호신부 버프 줄(인게임 파란 줄) --%>
			@if(nz(f.maxCharges()) > 0)<div class="${label}" data-tt-charges>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.charges"), fmax, fused)}</div>
				<%-- 현재 충전량(10-09 C221 — ClientStrings ItemDisplayChargesNCharges, 새 아이템 = 최대 충전) --%>
				<div class="${label}" data-tt-charges-now>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.current"), fmax)}</div>@endif
			@if(f.buffLines() != null)
				@for(Poe2.ModLine bl : f.buffLines())<div class="text-[#8888ff]" data-tt-buff>${net.luversof.web.gate.poe.PoeText.name(bl.ko(), bl.en())}</div>@endfor
			@endif
		@endif
	</div>
@endif
<%-- 요구 사항 줄은 requirements.jte 하나로(10-01) — 여기서 베이스 레벨로 그리면 고유가 자기 요구 레벨 대신 베이스 레벨을 보였고, 고유 툴팁엔 두 줄이 겹쳤다. --%>
