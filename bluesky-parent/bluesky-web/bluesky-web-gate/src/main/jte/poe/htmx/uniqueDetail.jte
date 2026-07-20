@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeUniqueItem item
@param PoeBaseItem base = null

!{
	String closeLabel = MessageUtil.getMessage("common.button.close");
	String displayName = PoeText.name(item.nameKo(), item.name());
	String displayBase = PoeText.name(item.baseTypeKo(), item.baseType());
	String displayClass = base != null ? PoeText.name(base.itemClassKo(), base.itemClass()) : null;
	java.util.List<String> displayImplicits = PoeText.lines(item.implicitsKo(), item.implicits());
	java.util.List<String> displayExplicits = PoeText.lines(item.explicitsKo(), item.explicits());
	// PoE 고유(주황) 색
	String rarity = "#af6025";
	// 고유는 기본 퀄리티 20% — 무기 물리 피해에 반영해 게임 표기(예 57-171 → 68-205)에 맞춘다
	int quality = 20;
	double qMul = 1 + quality / 100.0;
	boolean hasWeapon = base != null && base.weapon() != null && base.weapon().damageMax() > 0;
	PoeBaseItem.Armour armour = base != null ? base.armour() : null;
	boolean hasArmour = armour != null && (armour.armourMax() > 0 || armour.evasionMax() > 0 || armour.energyShieldMax() > 0 || armour.wardMax() > 0);
	boolean hasProps = hasWeapon || hasArmour;
	// 값 색: 라벨 회색, 기본 수치 흰색, 증가(퀄리티/모드)는 파랑
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
	String aug = "text-[#8888ff]";
}

<%-- PoE 게임 아이템 툴팁 (고유/주황) — 호버 시 #poePreview 에 떠서 표시(플로팅) --%>
	<div class="poe-tooltip poe-rar-unique w-80 max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
		<%-- 헤더 밴드 (인게임 2줄 아트: 이름 + 베이스) --%>
		<div class="poe-tt-band poe-tt-band-2">
			<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${displayName}</div>
			<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${displayBase}</div>
		</div>

		<%-- 아이템 클래스 + 영문 병기 --%>
		@if(displayClass != null || PoeText.isKorean())
			<div class="px-4 pt-1.5 text-center text-[11px]">
				@if(displayClass != null)<span class="text-white/70">${displayClass}</span>@endif
				@if(PoeText.isKorean())<div class="text-[10px] text-white/30">${item.name()} · ${item.baseType()}</div>@endif
			</div>
		@endif

		<%-- 속성 (무기/방어) --%>
		@if(hasProps)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
				<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.quality")}: </span><span class="${aug}">+${quality}%</span></div>
				@if(hasWeapon)
					<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.physical.damage")}: </span><span class="${aug}">${String.format("%.1f", base.weapon().damageMin() * qMul)}–${String.format("%.1f", base.weapon().damageMax() * qMul)}</span></div>
					<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.crit.chance")}: </span><span class="${value}">${String.format("%.2f", base.weapon().critChance())}%</span></div>
					<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.attacks.per.second")}: </span><span class="${value}">${String.format("%.2f", base.weapon().attacksPerSecond())}</span></div>
					@if(base.weapon().range() > 0)
						<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.range")}: </span><span class="${value}">${String.format("%.1f", base.weapon().range() / 10.0)}${PoeText.isKorean() ? "미터" : "m"}</span></div>
					@endif
				@endif
				@if(hasArmour)
					@if(armour.armourMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.armour")}: </span><span class="${aug}">${Math.round(armour.armourMin() * qMul)}${armour.armourMin() != armour.armourMax() ? "–" + Math.round(armour.armourMax() * qMul) : ""}</span></div>@endif
					@if(armour.evasionMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.evasion")}: </span><span class="${aug}">${Math.round(armour.evasionMin() * qMul)}${armour.evasionMin() != armour.evasionMax() ? "–" + Math.round(armour.evasionMax() * qMul) : ""}</span></div>@endif
					@if(armour.energyShieldMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.energy.shield")}: </span><span class="${aug}">${Math.round(armour.energyShieldMin() * qMul)}${armour.energyShieldMin() != armour.energyShieldMax() ? "–" + Math.round(armour.energyShieldMax() * qMul) : ""}</span></div>@endif
					@if(armour.wardMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.ward")}: </span><span class="${aug}">${Math.round(armour.wardMin() * qMul)}</span></div>@endif
				@endif
			</div>
		@endif

		<%-- 요구 사항 (레벨 + 힘/민첩/지능) --%>
		@if(item.requiredLevel() != null || base != null)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px]">
				<span class="${label}">${MessageUtil.getMessage("poe.items.detail.requirement")} </span>
				@if(item.requiredLevel() != null)<span class="${label}">${MessageUtil.getMessage("poe.items.detail.level")} </span><span class="${value}">${item.requiredLevel()}</span>@endif
				@if(base != null && base.reqStr() > 0)<span class="${value}">, ${base.reqStr()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.str")}</span>@endif
				@if(base != null && base.reqDex() > 0)<span class="${value}">, ${base.reqDex()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.dex")}</span>@endif
				@if(base != null && base.reqInt() > 0)<span class="${value}">, ${base.reqInt()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.int")}</span>@endif
			</div>
		@endif

		<%-- 암시 모드 (게임처럼 살짝 다른 배경) --%>
		@if(!displayImplicits.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center space-y-0.5 bg-white/[0.03]">
				@for(String line : displayImplicits)
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{net.luversof.web.gate.poe.PoeText.highlightValues(line)}</div>
				@endfor
			</div>
		@endif

		<%-- 설명(explicit) 모드 --%>
		@if(!displayExplicits.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center space-y-0.5">
				@for(String line : displayExplicits)
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{net.luversof.web.gate.poe.PoeText.highlightValues(line)}</div>
				@endfor
			</div>
		@endif
	</div>
