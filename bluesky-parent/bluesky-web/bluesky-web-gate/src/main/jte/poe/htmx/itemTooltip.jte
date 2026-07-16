@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import io.github.luversof.boot.context.support.MessageUtil

@param String rarityClass
@param String nameColor
@param String rarityBorder
@param String name
@param String base = null
@param String itemClass = null
@param PoeBaseItem baseItem = null
@param java.util.List<String> modLines = null

!{
	boolean hasWeapon = baseItem != null && baseItem.weapon() != null && baseItem.weapon().damageMax() > 0;
	PoeBaseItem.Armour armour = baseItem != null ? baseItem.armour() : null;
	boolean hasArmour = armour != null && (armour.armourMax() > 0 || armour.evasionMax() > 0 || armour.energyShieldMax() > 0 || armour.wardMax() > 0);
	boolean hasReq = baseItem != null && (baseItem.reqStr() > 0 || baseItem.reqDex() > 0 || baseItem.reqInt() > 0);
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
}

<%-- 게임 아이템 툴팁 본문 (호버 레이어용) — 헤더 밴드 + 아이템클래스 + 베이스 속성 + 요구 + 롤 모드 --%>
<div class="poe-tooltip ${rarityClass} w-80 max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarityBorder}">
	<div class="poe-tt-band">
		<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${name}</div>
		@if(base != null && !base.equals(name))
			<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${base}</div>
		@endif
	</div>
	@if(itemClass != null)
		<div class="px-4 pt-1 text-center text-[11px] text-white/60">${itemClass}</div>
	@endif

	@if(hasWeapon || hasArmour)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
			@if(hasWeapon)
				<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.physical.damage")}: </span><span class="${value}">${baseItem.weapon().damageMin()}–${baseItem.weapon().damageMax()}</span></div>
				<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.crit.chance")}: </span><span class="${value}">${String.format("%.2f", baseItem.weapon().critChance())}%</span></div>
				<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.attacks.per.second")}: </span><span class="${value}">${String.format("%.2f", baseItem.weapon().attacksPerSecond())}</span></div>
			@endif
			@if(hasArmour)
				@if(armour.armourMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.armour")}: </span><span class="${value}">${armour.armourMin()}${armour.armourMin() != armour.armourMax() ? "–" + armour.armourMax() : ""}</span></div>@endif
				@if(armour.evasionMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.evasion")}: </span><span class="${value}">${armour.evasionMin()}${armour.evasionMin() != armour.evasionMax() ? "–" + armour.evasionMax() : ""}</span></div>@endif
				@if(armour.energyShieldMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.energy.shield")}: </span><span class="${value}">${armour.energyShieldMin()}${armour.energyShieldMin() != armour.energyShieldMax() ? "–" + armour.energyShieldMax() : ""}</span></div>@endif
			@endif
		</div>
	@endif

	@if(hasReq)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1 text-center text-[12px]">
			<span class="${label}">${MessageUtil.getMessage("poe.items.detail.requirement")} </span>
			@if(baseItem.reqStr() > 0)<span class="${value}">${baseItem.reqStr()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.str")} </span>@endif
			@if(baseItem.reqDex() > 0)<span class="${value}">${baseItem.reqDex()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.dex")} </span>@endif
			@if(baseItem.reqInt() > 0)<span class="${value}">${baseItem.reqInt()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.int")}</span>@endif
		</div>
	@endif

	@if(modLines != null && !modLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5">
			@for(String line : modLines)
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{net.luversof.web.gate.poe.PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endif
</div>
