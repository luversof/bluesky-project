@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param String rarityClass
@param String nameColor
@param String rarityBorder
@param String name
@param String base = null
@param String itemClass = null
@param PoeBaseItem baseItem = null
@param java.util.List<String> modLines = null
@param java.util.List<String> implicitLines = null
@param boolean corrupted = false
@param int quality = 0

!{
	// 이름+베이스 2줄이면 인게임 2줄 헤더 아트(레어/유니크에 --band2-* 정의, 그 외 1줄 아트 폴백)
	boolean twoLine = base != null && !base.equals(name);
	boolean hasWeapon = baseItem != null && baseItem.weapon() != null && baseItem.weapon().damageMax() > 0;
	PoeBaseItem.Armour armour = baseItem != null ? baseItem.armour() : null;
	PoeBaseItem.Flask flask = baseItem != null ? baseItem.flask() : null;
	boolean hasArmour = armour != null && (armour.block() > 0 || armour.armourMax() > 0 || armour.evasionMax() > 0 || armour.energyShieldMax() > 0 || armour.wardMax() > 0);
	// 무기 범위는 근접 무기만 인게임 표시(활/완드=투사체라 미표시, range=120 센티넬 → <100 근접만). 미터=내부 데시미터/10.
	// itemDetail.jte 와 같은 규칙 — 두 화면이 어긋나면 같은 아이템이 화면마다 다르게 보인다.
	boolean showWeaponRange = hasWeapon && baseItem.weapon().range() > 0 && baseItem.weapon().range() < 100;
	// 인게임 요구 사항 줄은 **레벨이 먼저**다("요구 능력치 레벨 68, 155 힘"). 속성만 보고 게이트하면
	// 레벨 요구만 있는 아이템(장신구 등)은 요구 줄이 통째로 사라진다 → dropLevel 도 조건에 넣는다.
	boolean hasReq = baseItem != null
		&& (baseItem.dropLevel() > 0 || baseItem.reqStr() > 0 || baseItem.reqDex() > 0 || baseItem.reqInt() > 0);
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
}

<%-- 게임 아이템 툴팁 본문 (호버 레이어용) — 헤더 밴드 + 아이템클래스 + 베이스 속성 + 요구 + 롤 모드 --%>
<div class="poe-tooltip ${rarityClass} w-80 max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarityBorder}">
	<div class="poe-tt-band${twoLine ? " poe-tt-band-2" : ""}">
		<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${name}</div>
		@if(twoLine)
			<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${base}</div>
		@endif
	</div>
	@if(itemClass != null)
		<div class="px-4 pt-1 text-center text-[11px] text-white/60">${itemClass}</div>
	@endif

	@if(hasWeapon || hasArmour || quality > 0)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
			<%-- 품질은 인게임 속성 블록의 **첫 줄**이고 값은 매직 파랑이다
			     (PoB ItemsTab 이 무기/방어구/플라스크 세 분기 모두 같은 순서·색으로 그린다).
			     PoB 텍스트의 "Quality: N" 을 ITEM_META 로 걸러내기만 해서 임포트하면 품질이 사라졌다. --%>
			@if(quality > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.quality")}: </span><span class="text-[#8888ff]">+${quality}%</span></div>@endif
			@if(hasWeapon)
				<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.physical.damage")}: </span><span class="${value}">${baseItem.weapon().damageMin()}–${baseItem.weapon().damageMax()}</span></div>
				<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.crit.chance")}: </span><span class="${value}">${String.format("%.2f", baseItem.weapon().critChance())}%</span></div>
				<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.attacks.per.second")}: </span><span class="${value}">${String.format("%.2f", baseItem.weapon().attacksPerSecond())}</span></div>
				@if(showWeaponRange)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.range")}: </span><span class="${value}">${String.format("%.1f", baseItem.weapon().range() / 10.0)}${PoeText.isKorean() ? "미터" : "m"}</span></div>@endif
			@endif
			@if(hasArmour)
				<%-- 인게임 방패 툴팁은 **막기 확률이 첫 줄**이다(방어도/회피보다 위). --%>
				@if(armour.block() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.block")}: </span><span class="${value}">${armour.block()}%</span></div>@endif
				@if(armour.armourMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.armour")}: </span><span class="${value}">${armour.armourMin()}${armour.armourMin() != armour.armourMax() ? "–" + armour.armourMax() : ""}</span></div>@endif
				@if(armour.evasionMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.evasion")}: </span><span class="${value}">${armour.evasionMin()}${armour.evasionMin() != armour.evasionMax() ? "–" + armour.evasionMax() : ""}</span></div>@endif
				@if(armour.energyShieldMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.energy.shield")}: </span><span class="${value}">${armour.energyShieldMin()}${armour.energyShieldMin() != armour.energyShieldMax() ? "–" + armour.energyShieldMax() : ""}</span></div>@endif
				<%-- 수호(ward) — 탐험 리그 베이스 9종. hasArmour 조건엔 들어 있었는데 행이 없어
				     수호 전용 아이템은 구분선만 그려지고 속성 칸이 비어 있었다. --%>
				@if(armour.wardMax() > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.ward")}: </span><span class="${value}">${armour.wardMin()}${armour.wardMin() != armour.wardMax() ? "–" + armour.wardMax() : ""}</span></div>@endif
			@endif
		</div>
	@endif

	<%-- 플라스크 속성 (인게임 순서: 회복/지속 + 충전 → 요구 사항 → 모드). itemDetail 과 같은 형식.
	     여기가 비어 있어 빌드에 장착한 플라스크는 회복량·지속·충전이 통째로 안 보였다. --%>
	@if(flask != null)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5">
			@if(flask.type() == 4)
				<div class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.duration"), String.format("%.2f", flask.durationSeconds()))}</div>
			@else
				@if(flask.lifePerUse() > 0)
					<div class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.life"), String.format("%.2f", flask.durationSeconds()), String.valueOf(flask.lifePerUse()))}</div>
				@endif
				@if(flask.manaPerUse() > 0)
					<div class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.mana"), String.format("%.2f", flask.durationSeconds()), String.valueOf(flask.manaPerUse()))}</div>
				@endif
			@endif
			@if(flask.maxCharges() > 0)
				<div class="${label}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.charges"), String.valueOf(flask.maxCharges()), String.valueOf(flask.perCharge()))}</div>
			@endif
		</div>
	@endif

	@if(hasReq)
		<div class="poe-tt-sepimg"></div>
		<%-- 인게임/상세 페이지(itemDetail·uniqueDetail)와 같은 한 줄 형식: "요구 능력치 레벨 68, 155 힘, 60 민첩".
		     여기만 레벨이 빠지고 쉼표 없이 나열돼 같은 아이템이 화면마다 다르게 보였다. --%>
		<div class="px-4 py-1 text-center text-[12px]">
			<%-- ⚠ 한 줄로 붙여 쓸 것. @if 사이에 줄바꿈을 넣으면 그 개행이 공백으로 렌더돼
			     "레벨 62 , 180 힘"처럼 쉼표 앞이 벌어진다(itemDetail 도 같은 이유로 한 줄이다). --%>
			<span class="${label}">${MessageUtil.getMessage("poe.items.detail.requirement")} </span>@if(baseItem.dropLevel() > 0)<span class="${label}">${MessageUtil.getMessage("poe.items.detail.level")} </span><span class="${value}">${baseItem.dropLevel()}</span>@endif@if(baseItem.reqStr() > 0)<span class="${value}">${baseItem.dropLevel() > 0 ? ", " : ""}${baseItem.reqStr()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.str")}</span>@endif@if(baseItem.reqDex() > 0)<span class="${value}">${baseItem.dropLevel() > 0 || baseItem.reqStr() > 0 ? ", " : ""}${baseItem.reqDex()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.dex")}</span>@endif@if(baseItem.reqInt() > 0)<span class="${value}">${baseItem.dropLevel() > 0 || baseItem.reqStr() > 0 || baseItem.reqDex() > 0 ? ", " : ""}${baseItem.reqInt()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.int")}</span>@endif
		</div>
	@endif

	<%-- 임플리싯(암시) — 인게임은 명시 모드와 **구분선으로 갈라진 별도 섹션**이다.
	     한 덩어리로 붙여두면 아이템 고유의 암시 능력인지 롤된 모드인지 구분이 안 된다. --%>
	@if(implicitLines != null && !implicitLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5">
			@for(String line : implicitLines)
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{net.luversof.web.gate.poe.PoeText.highlightValues(line)}</div>
			@endfor
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

	<%-- 인게임은 부패 아이템 툴팁 **맨 아래**에 빨간 "부패됨" 한 줄을 둔다(더 이상 제작 불가라는 신호).
	     색은 화면 전체가 쓰는 --poe-corrupt(다크 #ff5a5a / 라이트 #d20000) 그대로. --%>
	@if(corrupted)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#d20000]">${MessageUtil.getMessage("poe.items.corrupted")}</div>
	@endif
</div>
