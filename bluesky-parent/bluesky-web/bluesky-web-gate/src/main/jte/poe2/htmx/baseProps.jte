@import net.luversof.web.gate.poe2.dto.Poe2
@import static net.luversof.web.gate.poe2.Poe2View.nz
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.BaseItem base
<%-- 품질 %(빌드 툴팁, 10-04 C148) — 인게임 속성 칸 첫 줄 "품질: +N%"(값 마법 파랑, PoE1 itemTooltip 과 같은 모양) --%>
@param int quality = 0

!{
	// 베이스 속성(툴팁 가운데 칸) — 인게임처럼 라벨 회색 · 값 흰색. 고유는 옵션으로 값이 바뀌므로 베이스 값을 그대로 보인다.
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
	Poe2.Armour a = base.armour();
	Poe2.Weapon w = base.weapon();
	Poe2.Flask f = base.flask();
	boolean hasArmour = a != null && (nz(a.armour()) > 0 || nz(a.evasion()) > 0 || nz(a.energyShield()) > 0 || nz(a.block()) > 0);
	boolean hasWeapon = w != null && nz(w.damageMax()) > 0;
	boolean hasFlask = f != null && (nz(f.lifePerUse()) > 0 || nz(f.manaPerUse()) > 0 || (f.recoverySeconds() != null && f.recoverySeconds() > 0));
}
@if(hasWeapon || hasArmour || hasFlask || quality > 0)
	<div class="poe-tt-sepimg"></div>
	<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
		@if(quality > 0)<div data-tt-quality><span class="${label}">${MessageUtil.getMessage("poe.items.detail.quality")}: </span><span class="text-[#8888ff]">+${quality}%</span></div>@endif
		@if(hasWeapon)
			<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.physical.damage")}: </span><span class="${value}">${w.damageMin()}–${w.damageMax()}</span></div>
			<div><span class="${label}">${MessageUtil.getMessage("poe2.tt.crit")}: </span><span class="${value}">${String.format("%.2f", w.critChance())}%</span></div>
			<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.attacks.per.second")}: </span><span class="${value}">${String.format("%.2f", w.attacksPerSecond())}</span></div>
			@if(nz(w.reloadTime()) > 0)
				<div><span class="${label}">${MessageUtil.getMessage("poe2.items.reload")}: </span><span class="${value}">${String.format("%.2f", w.reloadTime() / 1000.0)}s</span></div>
			@endif
		@endif
		@if(hasArmour)
			@if(nz(a.block()) > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.block")}: </span><span class="${value}">${a.block()}%</span></div>@endif
			@if(nz(a.armour()) > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.armour")}: </span><span class="${value}">${a.armour()}</span></div>@endif
			@if(nz(a.evasion()) > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.evasion")}: </span><span class="${value}">${a.evasion()}</span></div>@endif
			@if(nz(a.energyShield()) > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.energy.shield")}: </span><span class="${value}">${a.energyShield()}</span></div>@endif
			@if(a.movementPenalty() != null && a.movementPenalty() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe2.items.movement")}: </span><span class="${value}">-${String.format("%.0f", a.movementPenalty())}%</span></div>@endif
		@endif
		@if(hasFlask)
			@if(nz(f.lifePerUse()) > 0)
				<div class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.life"), String.format("%.1f", f.recoverySeconds()), String.valueOf(f.lifePerUse()))}</div>
			@elseif(nz(f.manaPerUse()) > 0)
				<div class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.mana"), String.format("%.1f", f.recoverySeconds()), String.valueOf(f.manaPerUse()))}</div>
			@else
				<div class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.duration"), String.format("%.1f", f.recoverySeconds()))}</div>
			@endif
		@endif
	</div>
@endif
<%-- 요구 사항 줄은 requirements.jte 하나로(10-01) — 여기서 베이스 레벨로 그리면 고유가 자기 요구 레벨 대신 베이스 레벨을 보였고, 고유 툴팁엔 두 줄이 겹쳤다. --%>
