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
<%-- 줄별 인게임 리마인더(회색 부연) — 줄 수가 같을 때만 그 줄 밑에(10-04 C131, 빌드 화면 레어 · 노멀) --%>
@param java.util.List<java.util.List<String>> modReminders = null
@param java.util.List<java.util.List<String>> implicitReminders = null
@param boolean corrupted = false
@param int quality = 0
<%-- 무형성 %(3.27 아이템 속성, 10-04 C136) — 품질 다음 속성 줄 --%>
@param int intangibility = 0
<%-- 영향력 · 분열 · 합성 심볼 키(10-04 C137) — 인게임 헤더 좌우 모서리: 둘이면 왼쪽 첫째 · 오른쪽 둘째, 하나면 양쪽 같은 아이콘(resultRareTooltip 과 같은 배치) --%>
@param java.util.List<String> influences = null
<%-- 줄 종류(crafted · fractured · enchant, 10-04 C138) — PoeText.modKindClass 로 인게임 색.
     Tailwind 는 Java 를 안 훑으므로 클래스 이름을 여기 적어 둔다: text-[#b4b4ff] text-[#a29162] text-[#b8daf2] --%>
@param java.util.List<String> implicitKinds = null
@param java.util.List<String> modKinds = null
<%-- 고유 로어(플레이버) 줄 — 옵션 밑, 부패됨 위(10-04 C139, 빌드 화면 고유 툴팁) --%>
@param java.util.List<String> flavourLines = null
<%-- 요구 능력치에 반영할 이 아이템 옵션 줄(영문 원문, 10-08 C178 — 빌드 화면 고유 "+100 Strength Requirement" 등. C177 고유 상세와 같은 PoeRequirements 규칙) --%>
@param java.util.List<String> reqImplicits = null
@param java.util.List<String> reqMods = null
<%-- 인게임 요구 레벨(10-08 C179 — PoB "LevelReq" · 고유 요구 레벨). null 이면 베이스 드롭 레벨 --%>
@param Integer levelReq = null
<%-- 실제 방어 수치(10-08 C184 — PoB 원문 "Armour: N" 등 최종값). null 이면 베이스 + 로컬 옵션 계산 --%>
@param net.luversof.web.gate.poe.dto.PoeBuild.Defences defences = null

!{
	// 이름+베이스 2줄이면 인게임 2줄 헤더 아트(레어/유니크에 --band2-* 정의, 그 외 1줄 아트 폴백)
	boolean twoLine = base != null && !base.equals(name);
	boolean hasWeapon = baseItem != null && baseItem.weapon() != null && baseItem.weapon().damageMax() > 0;
	PoeBaseItem.Armour armour = baseItem != null ? baseItem.armour() : null;
	PoeBaseItem.Flask flask = baseItem != null ? baseItem.flask() : null;
	// 플라스크 속성에 이 플라스크 자신의 로컬 옵션 반영(10-08 C186 — 회복량 · 지속 · 충전, PoB 식). 바뀐 숫자만 인게임처럼 파랑(품질이 바꾼 회복량 · 유틸 지속 포함)
	net.luversof.web.gate.poe.PoeFlaskProps.Result fp = flask == null ? null : net.luversof.web.gate.poe.PoeFlaskProps.apply(flask.lifePerUse(), flask.manaPerUse(), flask.durationSeconds(), flask.maxCharges(), flask.perCharge(), flask.type() == 4, net.luversof.web.gate.poe.PoeLocalProps.join(reqImplicits, reqMods), quality);
	java.util.function.BiFunction<String, Boolean, String> fpNum = (v, ch) -> ch ? "<span class=\"text-[#8888ff]\">" + v + "</span>" : v;
	String fdur = fp == null ? "" : fpNum.apply(fp.duration().seconds(), fp.duration().changed() || (flask.type() == 4 && quality > 0));
	String flife = fp == null ? "" : fpNum.apply(fp.life().text(), fp.life().changed() || quality > 0);
	String fmana = fp == null ? "" : fpNum.apply(fp.mana().text(), fp.mana().changed() || quality > 0);
	String fmax = fp == null ? "" : fpNum.apply(fp.maxCharges().text(), fp.maxCharges().changed());
	String fused = fp == null ? "" : fpNum.apply(fp.chargesUsed().text(), fp.chargesUsed().changed());
	boolean hasArmour = armour != null && (armour.block() > 0 || armour.armourMax() > 0 || armour.evasionMax() > 0 || armour.energyShieldMax() > 0 || armour.wardMax() > 0);
	// 무기 범위는 근접 무기만 인게임 표시(활/완드=투사체라 미표시, range=120 센티넬 → <100 근접만). 미터=내부 데시미터/10.
	// itemDetail.jte 와 같은 규칙 — 두 화면이 어긋나면 같은 아이템이 화면마다 다르게 보인다.
	boolean showWeaponRange = hasWeapon && baseItem.weapon().range() > 0 && baseItem.weapon().range() < 100;
	// 인게임 요구 사항 줄은 **레벨이 먼저**다("요구 능력치 레벨 68, 155 힘"). 속성만 보고 게이트하면
	// 레벨 요구만 있는 아이템(장신구 등)은 요구 줄이 통째로 사라진다 → dropLevel 도 조건에 넣는다.
	net.luversof.web.gate.poe.PoeRequirements.Result req = baseItem == null ? null : net.luversof.web.gate.poe.PoeRequirements.apply(baseItem.reqStr(), baseItem.reqDex(), baseItem.reqInt(), reqImplicits, reqMods);
	int reqLevel = levelReq != null ? levelReq : (baseItem == null ? 0 : baseItem.dropLevel());
	// 속성 칸 — 품질 · 로컬 옵션 반영(10-08 C184, C182 PoeLocalProps 와 같은 PoB 식). 예전엔 순수 베이스 값이라 물리 피해 170% 증가 무기도 베이스 피해였다.
	//   방어는 PoB 원문 실제값이 있으면 그것(베이스 롤이 아이템마다 달라 계산은 범위뿐), 무기 · 막기 · 원소 피해는 계산(베이스 고정이라 정확)
	net.luversof.web.gate.poe.PoeLocalProps.Result lp = baseItem == null ? null : net.luversof.web.gate.poe.PoeLocalProps.apply(new net.luversof.web.gate.poe.PoeLocalProps.Base(
		armour == null ? 0 : armour.armourMin(), armour == null ? 0 : armour.armourMax(),
		armour == null ? 0 : armour.evasionMin(), armour == null ? 0 : armour.evasionMax(),
		armour == null ? 0 : armour.energyShieldMin(), armour == null ? 0 : armour.energyShieldMax(),
		armour == null ? 0 : armour.wardMin(), armour == null ? 0 : armour.wardMax(),
		armour == null ? 0 : armour.block(),
		hasWeapon ? baseItem.weapon().damageMin() : 0, hasWeapon ? baseItem.weapon().damageMax() : 0,
		hasWeapon ? baseItem.weapon().attacksPerSecond() : 0, hasWeapon ? baseItem.weapon().critChance() : 0),
		net.luversof.web.gate.poe.PoeLocalProps.join(reqImplicits, reqMods), hasArmour, hasWeapon, quality);
	String aug = "text-[#8888ff]";
	String armourText = defences != null && defences.armour() != null ? String.valueOf(defences.armour()) : (lp == null ? "" : lp.armour().text("-"));
	String evasionText = defences != null && defences.evasion() != null ? String.valueOf(defences.evasion()) : (lp == null ? "" : lp.evasion().text("-"));
	String esText = defences != null && defences.energyShield() != null ? String.valueOf(defences.energyShield()) : (lp == null ? "" : lp.energyShield().text("-"));
	String wardText = defences != null && defences.ward() != null ? String.valueOf(defences.ward()) : (lp == null ? "" : lp.ward().text("-"));
	// 값이 베이스와 다르면(품질 · 옵션) 인게임처럼 파란색
	boolean defAug = quality > 0 || (lp != null && (lp.armour().changed() || lp.evasion().changed() || lp.energyShield().changed() || lp.ward().changed()));
	boolean hasReq = baseItem != null
		&& (reqLevel > 0 || req.str().present() || req.dex().present() || req.intel().present());
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
}

<%-- 게임 아이템 툴팁 본문 (호버 레이어용) — 헤더 밴드 + 아이템클래스 + 베이스 속성 + 요구 + 롤 모드 --%>
<div class="poe-tooltip ${rarityClass} w-80 max-w-[calc(100vw-2rem)] max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarityBorder}">
	!{
		String leftInf = influences != null && !influences.isEmpty() ? "/poe-data/ui-assets/influence-" + influences.get(0) + ".png" : null;
		String rightInf = influences != null && influences.size() > 1 ? "/poe-data/ui-assets/influence-" + influences.get(1) + ".png" : leftInf;
	}
	<div class="poe-tt-band${twoLine ? " poe-tt-band-2" : ""}">
		@if(leftInf != null)
			<img src="${leftInf}" data-tt-influence style="position:absolute;left:8px;top:6px;width:30px;height:30px;object-fit:contain;filter:drop-shadow(0 1px 2px rgba(0,0,0,.9))" alt="">
			<img src="${rightInf}" data-tt-influence style="position:absolute;right:8px;top:6px;width:30px;height:30px;object-fit:contain;filter:drop-shadow(0 1px 2px rgba(0,0,0,.9))" alt="">
		@endif
		<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${name}</div>
		@if(twoLine)
			<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${base}</div>
		@endif
	</div>
	@if(itemClass != null)
		<div class="px-4 pt-1 text-center text-[11px] text-white/60">${itemClass}</div>
	@endif

	@if(hasWeapon || hasArmour || quality > 0 || intangibility > 0)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
			<%-- 품질은 인게임 속성 블록의 **첫 줄**이고 값은 매직 파랑이다
			     (PoB ItemsTab 이 무기/방어구/플라스크 세 분기 모두 같은 순서·색으로 그린다).
			     PoB 텍스트의 "Quality: N" 을 ITEM_META 로 걸러내기만 해서 임포트하면 품질이 사라졌다. --%>
			@if(quality > 0)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.quality")}: </span><span class="text-[#8888ff]">+${quality}%</span></div>@endif
			@if(intangibility > 0)<div data-tt-intangibility><span class="${label}">${MessageUtil.getMessage("poe.items.detail.intangibility")}: </span><span class="text-[#8888ff]">${intangibility}%</span></div>@endif
			@if(hasWeapon)
				<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.physical.damage")}: </span><span class="${quality > 0 || lp.physMin().changed() || lp.physMax().changed() ? aug : value}" data-tt-phys>${lp.physMin().damageText()}-${lp.physMax().damageText()}</span></div>
				@if(lp.hasElemental())<div data-tt-ele><span class="${label}">${MessageUtil.getMessage("poe.items.detail.elemental.damage")}: </span>!{ boolean eleFirst = true; }@if(lp.fire().present())<span class="text-[#960000]">${lp.fire().text()}</span>!{ eleFirst = false; }@endif@if(lp.cold().present())${eleFirst ? "" : ", "}<span class="text-[#366492]">${lp.cold().text()}</span>!{ eleFirst = false; }@endif@if(lp.lightning().present())${eleFirst ? "" : ", "}<span class="text-[#ffd700]">${lp.lightning().text()}</span>@endif</div>@endif@if(lp.chaos().present())<div data-tt-chaos><span class="${label}">${MessageUtil.getMessage("poe.items.detail.chaos.damage")}: </span><span class="text-[#d02090]">${lp.chaos().text()}</span></div>@endif
				<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.crit.chance")}: </span><span class="${lp.critChance().changed() ? aug : value}">${lp.critChance().text2()}%</span></div>
				<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.attacks.per.second")}: </span><span class="${lp.attacksPerSecond().changed() ? aug : value}">${lp.attacksPerSecond().text2()}</span></div>
				@if(showWeaponRange)<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.range")}: </span><span class="${value}">${String.format("%.1f", baseItem.weapon().range() / 10.0)}${PoeText.isKorean() ? "미터" : (baseItem.weapon().range() == 10 ? " metre" : " metres")}</span></div>@endif
			@endif
			@if(hasArmour)
				<%-- 인게임 방패 툴팁은 **막기 확률이 첫 줄**이다(방어도/회피보다 위). --%>
				@if(lp.block().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.block")}: </span><span class="${lp.block().changed() ? aug : value}">${lp.block().text("-")}%</span></div>@endif
				@if(!armourText.isEmpty() && !"0".equals(armourText))<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.armour")}: </span><span class="${defAug ? aug : value}" data-tt-armour>${armourText}</span></div>@endif
				@if(!evasionText.isEmpty() && !"0".equals(evasionText))<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.evasion")}: </span><span class="${defAug ? aug : value}" data-tt-evasion>${evasionText}</span></div>@endif
				@if(!esText.isEmpty() && !"0".equals(esText))<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.energy.shield")}: </span><span class="${defAug ? aug : value}" data-tt-es>${esText}</span></div>@endif
				<%-- 수호(ward) — 탐험 리그 베이스 9종. hasArmour 조건엔 들어 있었는데 행이 없어
				     수호 전용 아이템은 구분선만 그려지고 속성 칸이 비어 있었다. --%>
				@if(!wardText.isEmpty() && !"0".equals(wardText))<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.ward")}: </span><span class="${defAug ? aug : value}">${wardText}</span></div>@endif
				<%-- 이동 속도 감소(10-09 C223 — 갑옷 · 방패, PoB Bases MovementPenalty · poedb 툴팁 "이동 속도: -3%", PoE2 와 같은 키) --%>
				@if(armour != null && armour.movementPenalty() != null && armour.movementPenalty() > 0)<div data-tt-movement><span class="${label}">${MessageUtil.getMessage("poe2.items.movement")}: </span><span class="${value}">-${armour.movementPenalty()}%</span></div>@endif
			@endif
		</div>
	@endif

	<%-- 플라스크 속성 (인게임 순서: 회복/지속 + 충전 → 요구 사항 → 모드). itemDetail 과 같은 형식.
	     여기가 비어 있어 빌드에 장착한 플라스크는 회복량·지속·충전이 통째로 안 보였다. --%>
	@if(flask != null)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5">
			@if(flask.type() == 4)
				<div class="${value}">$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.duration"), fdur)}</div>
			@else
				@if(flask.lifePerUse() > 0)
					<div class="${value}">$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.life"), fdur, flife)}</div>
				@endif
				@if(flask.manaPerUse() > 0)
					<div class="${value}">$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.mana"), fdur, fmana)}</div>
				@endif
			@endif
			@if(flask.maxCharges() > 0)
				<div class="${label}">$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.charges"), fmax, fused)}</div>
				<%-- 현재 충전량(10-09 C221 — ClientStrings ItemDisplayChargesNCharges, 새 아이템 = 최대 충전) --%>
				<div class="${label}" data-tt-charges-now>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.current"), fmax)}</div>
				<%-- 플라스크 부여 버프 = 충전 줄 바로 아래 파란 줄(10-09 C241 — PoB ItemsTab.lua buffModLines · poedb 죽어가는 해 순서. 예전(C196·C204)엔 옵션 뒤 별도 칸) --%>
				@if(flask != null && flask.buffLines() != null && !flask.buffLines().isEmpty())
					@for(var buffLine : flask.buffLines())
						<div class="text-[#8888ff] whitespace-pre-line" data-tt-flask-buff>$unsafe{PoeText.highlightValues(PoeText.name(buffLine.ko(), buffLine.en()))}</div>
					@endfor
				@endif
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
			<span class="${label}">${MessageUtil.getMessage("poe.items.detail.requirement")} </span>@if(reqLevel > 0)<span class="${label}">${MessageUtil.getMessage("poe.items.detail.level")} </span><span class="${value}">${reqLevel}</span>@endif@if(req.str().present())<span class="${value}">${reqLevel > 0 ? ", " : ""}${req.str().text()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.str")}</span>@endif@if(req.dex().present())<span class="${value}">${reqLevel > 0 || req.str().present() ? ", " : ""}${req.dex().text()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.dex")}</span>@endif@if(req.intel().present())<span class="${value}">${reqLevel > 0 || req.str().present() || req.dex().present() ? ", " : ""}${req.intel().text()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.int")}</span>@endif
		</div>
	@endif

	<%-- 임플리싯(암시) — 인게임은 명시 모드와 **구분선으로 갈라진 별도 섹션**이다.
	     한 덩어리로 붙여두면 아이템 고유의 암시 능력인지 롤된 모드인지 구분이 안 된다. --%>
	@if(implicitLines != null && !implicitLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5">
			@for(int ii = 0; ii < implicitLines.size(); ii++)
				@if(!implicitLines.get(ii).isBlank())
				<div class="text-[12px] leading-snug ${net.luversof.web.gate.poe.PoeText.modKindClass(implicitKinds, implicitLines.size(), ii)} whitespace-pre-line" data-mod-kind="${implicitKinds != null && implicitKinds.size() == implicitLines.size() ? implicitKinds.get(ii) : ""}">$unsafe{net.luversof.web.gate.poe.PoeText.highlightValues(implicitLines.get(ii))}</div>
				@if(implicitReminders != null && implicitReminders.size() == implicitLines.size() && implicitReminders.get(ii) != null)
					@for(String rem : implicitReminders.get(ii))
						<div class="text-[11px] leading-snug text-[#7f7f7f]" data-tt-reminder>${rem}</div>
					@endfor
				@endif
				@endif
			@endfor
		</div>
	@endif

	@if(modLines != null && !modLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5">
			@for(int mi = 0; mi < modLines.size(); mi++)
				@if(!modLines.get(mi).isBlank())
				<div class="text-[12px] leading-snug ${net.luversof.web.gate.poe.PoeText.modKindClass(modKinds, modLines.size(), mi)} whitespace-pre-line" data-mod-kind="${modKinds != null && modKinds.size() == modLines.size() ? modKinds.get(mi) : ""}">$unsafe{net.luversof.web.gate.poe.PoeText.highlightValues(modLines.get(mi))}</div>
				@if(modReminders != null && modReminders.size() == modLines.size() && modReminders.get(mi) != null)
					@for(String rem : modReminders.get(mi))
						<div class="text-[11px] leading-snug text-[#7f7f7f]" data-tt-reminder>${rem}</div>
					@endfor
				@endif
				@endif
			@endfor
		</div>
	@endif

	<%-- 타락 줄 = 옵션 바로 아래 · 로어 위(10-09 C216 — poedb 목소리 · poe2db 신념의 프리즘 툴팁 순서: 옵션 → Corrupted → 로어 → 사용 설명. 예전(C157)엔 맨 아래) --%>
	@if(corrupted)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#d20000]" data-tt-corrupted>${MessageUtil.getMessage("poe.items.corrupted")}</div>
	@endif
	@if(flavourLines != null && !flavourLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5" data-tt-flavour>
			@for(String fl : flavourLines)
				<div class="text-[12px] leading-snug italic text-[#af6025]">${fl}</div>
			@endfor
		</div>
	@endif
	<%-- 사용 설명(10-09 C213 — 고유 상세 C212 와 같은 게임 ClientStrings ItemDescription*, 빌드의 플라스크 · 팅크처 · 주얼 카드) --%>
	!{ String helpKey = baseItem == null ? null : net.luversof.web.gate.poe.PoeItemDescription.key(baseItem.category(), baseItem.name()); }
	@if(helpKey != null)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center text-[12px] leading-snug text-[#7f7f7f]" data-tt-help>${MessageUtil.getMessage(helpKey)}</div>
	@endif

</div>
