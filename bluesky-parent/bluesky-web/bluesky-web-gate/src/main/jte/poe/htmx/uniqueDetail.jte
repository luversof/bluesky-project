@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeUniqueItem item
@param PoeBaseItem base = null
<%-- 어느 변형을 보여줄지(1-base). null 이면 아이템의 기본 변형. 인게임엔 변형 라벨이 없으므로
     **툴팁 안에는 라벨을 넣지 않는다** — 선택 칩은 툴팁 바깥(uniqueVariants)에 둔다. --%>
@param Integer variant = null

!{
	String closeLabel = MessageUtil.getMessage("common.button.close");
	// 변형이 지정되면 그 변형의 모드 줄로 갈아끼운다(임프레션스 화염 ↔ 카오스처럼 통째로 달라진다)
	net.luversof.web.gate.poe.dto.PoeUniqueVariant picked = null;
	if (variant != null && item.variants() != null) {
		for (var v : item.variants()) {
			if (v.index() == variant.intValue()) picked = v;
		}
	}
	String displayName = PoeText.name(item.nameKo(), item.name());
	// 베이스 이름은 조인한 베이스의 것(10-09 C209 — 고른 변형의 베이스 "사파이어 반지", PoB 표기 "Maelstrom Staff" · "Two-Toned Boots (Evasion/Energy Shield)" 대신 게임 이름). 한국어는 전수 같음
	String displayBase = base != null ? PoeText.name(base.nameKo(), base.name()) : PoeText.name(item.baseTypeKo(), item.baseType());
	String displayClass = base != null ? net.luversof.web.gate.poe.PoeClassNames.poe1(base.itemClassKo(), base.itemClass()) : (item.tincture() != null ? MessageUtil.getMessage("poe.items.class.tincture") : (item.baseType() != null && item.baseType().endsWith(" Talisman") ? MessageUtil.getMessage("poe.items.class.amulet") : null));
	java.util.List<String> displayImplicits = picked != null ? PoeText.lines(picked.implicitsKo(), picked.implicits()) : PoeText.lines(item.implicitsKo(), item.implicits());
	// 영어가 베이스 암시와 같으면 베이스의 한국어(실제 스탯 서술, 10-09 C219 — 스태프 막기 "공격 피해 막기 확률 +25%")
	displayImplicits = net.luversof.web.gate.poe.PoeUniqueBase.implicitLines(displayImplicits, picked != null ? picked.implicits() : item.implicits(), base, PoeText.isKorean() && (picked != null ? picked.implicitsKo() : item.implicitsKo()) != null);
	java.util.List<String> displayExplicits = picked != null ? PoeText.lines(picked.explicitsKo(), picked.explicits()) : PoeText.lines(item.explicitsKo(), item.explicits());
	// 요구 능력치에 이 아이템 자신의 옵션 반영(10-08 C177 — "+100 Strength Requirement" · "800% increased Attribute Requirements" · "Has no Attribute Requirements", PoB 규칙)
	net.luversof.web.gate.poe.PoeRequirements.Result req = base == null ? null : net.luversof.web.gate.poe.PoeRequirements.apply(base.reqStr(), base.reqDex(), base.reqInt(), picked != null ? picked.implicits() : item.implicits(), picked != null ? picked.explicits() : item.explicits());
	// 리마인더(인게임 회색 부연, 10-04 C114) — 영어 explicits 와 같은 길이의 줄별 목록. 보이는 줄 수가 같으면 그 줄 밑에,
	//   한국어가 여러 줄을 한 줄로 합쳐 수가 다르면 옵션 칸 끝에 모아서
	//   한국어 부연은 옵션 줄도 한국어로 보일 때만(옵션 줄과 같은 기준 — 10-04 C127 검토: 영어 줄 밑 한국어 부연)
	java.util.List<String> shownKo = picked != null ? picked.explicitsKo() : item.explicitsKo();
	boolean remKo = PoeText.isKorean() && shownKo != null && !shownKo.isEmpty();
	java.util.List<java.util.List<String>> reminders = picked != null
		? (remKo && picked.explicitsRemindersKo() != null ? picked.explicitsRemindersKo() : picked.explicitsReminders())
		: (remKo && item.explicitsRemindersKo() != null ? item.explicitsRemindersKo() : item.explicitsReminders());
	boolean remindersPerLine = reminders != null && reminders.size() == displayExplicits.size();
	// 데이터의 "Corrupted"/"타락" 옵션 줄(고유 55 · 금단 변형 등) — 인게임은 옵션이 아니라 툴팁 맨 아래 붉은 한 줄(10-05 C157, 빌드 툴팁과 같은 모양)
	// ⚠ Set.of 는 중복을 못 받는다 — 영어 메시지가 "Corrupted" 라 영어 화면 전부 500 이었다(탐침이 잡음). 메시지가 "타락"으로 바뀌어 고정 두 낱말이면 된다
	java.util.Set<String> corruptedWords = java.util.Set.of("Corrupted", "타락");
	boolean corruptedItem = displayExplicits.stream().anyMatch(corruptedWords::contains);
	java.util.List<String> remindersAtEnd = new java.util.ArrayList<>();
	if (reminders != null && !remindersPerLine) for (java.util.List<String> r : reminders) if (r != null) for (String x : r) if (!remindersAtEnd.contains(x)) remindersAtEnd.add(x);
	// PoE 고유(주황) 색
	String rarity = "#af6025";
	// 고유는 기본 퀄리티 20% — 무기 물리 피해에 반영해 게임 표기(예 57-171 → 68-205)에 맞춘다
	int quality = 20;
	double qMul = 1 + quality / 100.0;
	boolean hasWeapon = base != null && base.weapon() != null && base.weapon().damageMax() > 0;
	PoeBaseItem.Armour armour = base != null ? base.armour() : null;
	boolean hasArmour = armour != null && (armour.armourMax() > 0 || armour.evasionMax() > 0 || armour.energyShieldMax() > 0 || armour.wardMax() > 0);
	boolean hasProps = hasWeapon || hasArmour;
	// 속성 칸에 이 고유 자신의 로컬 옵션 반영(10-08 C182 — "(30-50)% increased Ward" · "Adds … Physical Damage" 등, PoB Item.lua 식). 품질 20 은 그대로 곱한다
	net.luversof.web.gate.poe.PoeLocalProps.Result lp = base == null ? null : net.luversof.web.gate.poe.PoeLocalProps.apply(new net.luversof.web.gate.poe.PoeLocalProps.Base(
		armour == null ? 0 : armour.armourMin(), armour == null ? 0 : armour.armourMax(),
		armour == null ? 0 : armour.evasionMin(), armour == null ? 0 : armour.evasionMax(),
		armour == null ? 0 : armour.energyShieldMin(), armour == null ? 0 : armour.energyShieldMax(),
		armour == null ? 0 : armour.wardMin(), armour == null ? 0 : armour.wardMax(),
		armour == null ? 0 : armour.block(),
		hasWeapon ? base.weapon().damageMin() : 0, hasWeapon ? base.weapon().damageMax() : 0,
		hasWeapon ? base.weapon().attacksPerSecond() : 0, hasWeapon ? base.weapon().critChance() : 0),
		net.luversof.web.gate.poe.PoeLocalProps.join(picked != null ? picked.implicits() : item.implicits(), picked != null ? picked.explicits() : item.explicits()), hasArmour, hasWeapon, quality);
	// 유니크 플라스크: 베이스 플라스크의 회복/지속·충전 표시 + 베이스 버프 줄(10-09 C204 — 예전엔 "고유 옵션으로 대체"라며 뺐는데 PoB 는 등급 상관없이 베이스 버프를 붙인다).
	PoeBaseItem.Flask flask = base != null ? base.flask() : null;
	// 플라스크 속성에 이 플라스크 자신의 로컬 옵션 반영(10-08 C186 — 회복량 · 지속 · 충전, PoB 식). 바뀐 숫자만 인게임처럼 파랑(품질이 바꾼 회복량 · 유틸 지속 포함)
	net.luversof.web.gate.poe.PoeFlaskProps.Result fp = flask == null ? null : net.luversof.web.gate.poe.PoeFlaskProps.apply(flask.lifePerUse(), flask.manaPerUse(), flask.durationSeconds(), flask.maxCharges(), flask.perCharge(), flask.type() == 4, net.luversof.web.gate.poe.PoeLocalProps.join(picked != null ? picked.implicits() : item.implicits(), picked != null ? picked.explicits() : item.explicits()), 0);
	java.util.function.BiFunction<String, Boolean, String> fpNum = (v, ch) -> ch ? "<span class=\"text-[#8888ff]\">" + v + "</span>" : "<span class=\"text-[#e0e0e0]\">" + v + "</span>";
	String fdur = fp == null ? "" : fpNum.apply(fp.duration().seconds(), fp.duration().changed() || (flask.type() == 4 && 0 > 0));
	String flife = fp == null ? "" : fpNum.apply(fp.life().text(), fp.life().changed() || 0 > 0);
	String fmana = fp == null ? "" : fpNum.apply(fp.mana().text(), fp.mana().changed() || 0 > 0);
	String fmax = fp == null ? "" : fpNum.apply(fp.maxCharges().text(), fp.maxCharges().changed());
	String fused = fp == null ? "" : fpNum.apply(fp.chargesUsed().text(), fp.chargesUsed().changed());
	// 값 색: 라벨 회색, 기본 수치 흰색, 증가(퀄리티/모드)는 파랑
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
	String aug = "text-[#8888ff]";
	java.util.List<String> flavourLines = PoeText.lines(item.flavourKo(), item.flavour());
	// 팅크처 속성(10-09 C211 — 팅크처는 장비 베이스 표에 없어 베이스 조인이 없다. 고유 데이터의 tincture{manaBurn, cooldown} + 이 고유 자신의 옵션, PoB 식)
	net.luversof.web.gate.poe.PoeTinctureProps.Result tp = item.tincture() == null ? null : net.luversof.web.gate.poe.PoeTinctureProps.apply(item.tincture().manaBurn(), item.tincture().cooldown(), net.luversof.web.gate.poe.PoeLocalProps.join(picked != null ? picked.implicits() : item.implicits(), picked != null ? picked.explicits() : item.explicits()));
}

<%-- PoE 게임 아이템 툴팁 (고유/주황) — 호버 시 #poePreview 에 떠서 표시(플로팅) --%>
	<div class="poe-tooltip poe-rar-unique w-80 max-w-[calc(100vw-2rem)] max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
		<%-- 헤더 밴드 (인게임 2줄 아트: 이름 + 베이스) --%>
		<div class="poe-tt-band poe-tt-band-2">
			<%-- 영향력 상징(10-08 C198 — 셰이퍼 · 엘더 고유, 빌드 카드 itemTooltip 과 같은 자리: 둘이면 왼쪽 첫째 · 오른쪽 둘째, 하나면 양쪽 같은 상징) --%>
			@if(item.influences() != null && !item.influences().isEmpty())
				!{ String infL = "/poe-data/ui-assets/influence-" + item.influences().get(0) + ".png"; String infR = item.influences().size() > 1 ? "/poe-data/ui-assets/influence-" + item.influences().get(1) + ".png" : infL; }
				<img src="${infL}" data-tt-influence style="position:absolute;left:8px;top:6px;width:30px;height:30px;object-fit:contain;filter:drop-shadow(0 1px 2px rgba(0,0,0,.9))" alt="">
				<img src="${infR}" data-tt-influence style="position:absolute;right:8px;top:6px;width:30px;height:30px;object-fit:contain;filter:drop-shadow(0 1px 2px rgba(0,0,0,.9))" alt="">
			@endif
			<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${displayName}</div>
			<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${displayBase}</div>
		</div>

		<%-- 아이템 종류 + 영문 병기 --%>
		<%-- 종류 줄은 무기만 속성 칸 첫 줄(10-23 Q43 — 인게임은 방어구 · 장신구 · 주얼에 종류 줄이 없고, 무기는 단수 'Bow' · '양손 검') --%>
		!{ String weaponCls = net.luversof.web.gate.poe.PoeClassNames.poe1Weapon(base != null ? base.itemClass() : null); boolean abyss = base != null && "AbyssJewel".equals(base.itemClass()); }
		@if(PoeText.isKorean())
			<div class="px-4 pt-1.5 text-center text-[11px]">
				@if(PoeText.isKorean())<div class="text-[10px] text-white/30">${item.name()} · ${base != null ? base.name() : item.baseType()}</div>@endif
			</div>
		@endif

		<%-- 반경 (주얼: 대형/중형/소형 — 인게임 "반경: 대형") --%>
		<%-- 중복 사용 제한(10-08 C197 — 게임 ClientStrings JewelItemLimit "중복 사용 제한: 1" · HistoricJewelLimit "1 역사적인 순간") · 반경 — 인게임은 같은 속성 칸 --%>
		@if(abyss || (item.radius() != null && !item.radius().isBlank()) || (item.limit() != null && !item.limit().isBlank()) || item.talismanTier() != null)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5">
				<%-- 심연 주얼(Q43 — 게임 ItemDisplayAbyssalJewel '심연' · 'Abyss', 속성 칸 첫 줄 회색) --%>
				@if(abyss)<div class="${label}" data-tt-abyss>${PoeText.isKorean() ? "심연" : "Abyss"}</div>@endif
				<%-- 부적 등급(10-09 C214 — 게임 ClientStrings ItemDisplayStringTalismanTier "부적 등급", 같은 속성 칸) --%>
				@if(item.talismanTier() != null)<div data-tt-talisman><span class="${label}">${MessageUtil.getMessage("poe.items.detail.talisman.tier")}: </span><span class="${value}">${item.talismanTier()}</span></div>@endif
				@if(item.limit() != null && !item.limit().isBlank())<div data-tt-limit><span class="${label}">${MessageUtil.getMessage("poe.items.detail.limit")}: </span><span class="${value}">${MessageUtil.getMessage("poe.limit." + item.limit().replace(" ", ""), item.limit())}</span></div>@endif
				@if(item.radius() != null && !item.radius().isBlank())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.radius")}: </span><span class="${value}">${MessageUtil.getMessage("poe.radius." + item.radius().replace(" ", ""), item.radius())}</span></div>@endif
			</div>
		@endif

		<%-- 팅크처 속성(10-09 C211 — 게임 ClientStrings ItemDisplayTinctureToxicityNSeconds "{0}초마다 마나 연소 유발" · ItemDisplayTinctureCooldownNSeconds
		     "비활성화 시 재사용 대기시간 {0}초", 회색 문장에 흰 숫자 · 옵션이 바꾼 숫자는 파랑) --%>
		@if(tp != null)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center ${label}" data-tt-tincture>
				<div>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.tincture.burn"), "<span class=\"" + (tp.manaBurn().changed() ? aug : value) + "\">" + tp.manaBurnText() + "</span>")}</div>
				<div>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.tincture.cooldown"), "<span class=\"" + (tp.cooldown().changed() ? aug : value) + "\">" + tp.cooldownText() + "</span>")}</div>
			</div>
		@endif

		<%-- 속성 (무기/방어) --%>
		@if(hasProps)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
				@if(weaponCls != null)<div class="${label}" data-tt-weapon-class>${weaponCls}</div>@endif
				<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.quality")}: </span><span class="${aug}">+${quality}%</span></div>
				@if(hasWeapon)
					<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.physical.damage")}: </span><span class="${aug}" data-tt-phys>${lp.physMin().damageText()}-${lp.physMax().damageText()}</span></div>
					<%-- 원소 · 카오스 추가 피해(10-08 C183, 인게임 "원소 피해: …" 속성별 색 — 게임 ClientStrings ItemDisplayWeaponElementalDamage) --%>
					@if(lp.hasElemental())<div data-tt-ele><span class="${label}">${MessageUtil.getMessage("poe.items.detail.elemental.damage")}: </span>!{ boolean eleFirst = true; }@if(lp.fire().present())<span class="text-[#960000]">${lp.fire().text()}</span>!{ eleFirst = false; }@endif@if(lp.cold().present())${eleFirst ? "" : ", "}<span class="text-[#366492]">${lp.cold().text()}</span>!{ eleFirst = false; }@endif@if(lp.lightning().present())${eleFirst ? "" : ", "}<span class="text-[#ffd700]">${lp.lightning().text()}</span>@endif</div>@endif@if(lp.chaos().present())<div data-tt-chaos><span class="${label}">${MessageUtil.getMessage("poe.items.detail.chaos.damage")}: </span><span class="text-[#d02090]">${lp.chaos().text()}</span></div>@endif
					<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.crit.chance")}: </span><span class="${lp.critChance().changed() ? aug : value}">${lp.critChance().text2()}%</span></div>
					<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.attacks.per.second")}: </span><span class="${lp.attacksPerSecond().changed() ? aug : value}">${lp.attacksPerSecond().text2()}</span></div>
					@if(base.weapon().range() > 0 && base.weapon().range() < 100)
						<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.range")}: </span><span class="${value}">${String.format("%.1f", base.weapon().range() / 10.0)}${PoeText.isKorean() ? "미터" : (base.weapon().range() == 10 ? " metre" : " metres")}</span></div>
					@endif
				@endif
				@if(hasArmour)
					@if(lp.block().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.block")}: </span><span class="${lp.block().changed() ? aug : value}">${lp.block().text("-")}%</span></div>@endif
					@if(lp.armour().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.armour")}: </span><span class="${aug}" data-tt-armour>${lp.armour().text("-")}</span></div>@endif
					@if(lp.evasion().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.evasion")}: </span><span class="${aug}">${lp.evasion().text("-")}</span></div>@endif
					@if(lp.energyShield().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.energy.shield")}: </span><span class="${aug}">${lp.energyShield().text("-")}</span></div>@endif
					@if(lp.ward().present())<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.ward")}: </span><span class="${aug}" data-tt-ward>${lp.ward().text("-")}</span></div>@endif
					<%-- 이동 속도 감소(10-09 C223 — 갑옷 · 방패, PoB Bases MovementPenalty · poedb 툴팁 "이동 속도: -3%", PoE2 와 같은 키) --%>
					@if(armour.movementPenalty() != null && armour.movementPenalty() > 0)<div data-tt-movement><span class="${label}">${MessageUtil.getMessage("poe2.items.movement")}: </span><span class="${value}">-${armour.movementPenalty()}%</span></div>@endif
				@endif
			</div>
		@endif

		<%-- 플라스크 회복/지속 + 충전 (인게임: 베이스 속성) --%>
		@if(flask != null)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5">
				@if(flask.type() == 4)
					<div class="${label}">$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.duration"), fdur)}</div>
				@else
					@if(flask.lifePerUse() > 0)
						<div class="${label}">$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.life"), fdur, flife)}</div>
					@endif
					@if(flask.manaPerUse() > 0)
						<div class="${label}">$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.mana"), fdur, fmana)}</div>
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

		<%-- 요구 사항 (레벨 + 힘/민첩/지능) --%>
		<%-- 내용 없는 "요구 사항" 제목만 남지 않게(10-09 C217 — 레벨 없는 주얼 186개: 베이스 요구 {} · PoB 원문에 레벨 없음) --%>
		@if(item.requiredLevel() != null || (req != null && (req.str().present() || req.dex().present() || req.intel().present())))
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px]" data-tt-requirements>
				<span class="${label}">${MessageUtil.getMessage("poe.items.detail.requirement")} </span>
				<%-- 쉼표는 앞에 항목이 있을 때만(10-25 Q188 — 요구 레벨 없는 고유 23개가 "요구 사항 , 12 힘" 으로 시작했다, PoE2 requirements.jte 와 같은 규칙) --%>
				!{ boolean rqL = item.requiredLevel() != null; boolean rqS = req != null && req.str().present(); boolean rqD = req != null && req.dex().present(); }
				@if(rqL)<span class="${label}">${MessageUtil.getMessage("poe.items.detail.level")} </span><span class="${value}">${item.requiredLevel()}</span>@endif@if(rqS)<span class="${value}">${rqL ? ", " : ""}${req.str().text()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.str")}</span>@endif@if(rqD)<span class="${value}">${rqL || rqS ? ", " : ""}${req.dex().text()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.dex")}</span>@endif@if(req != null && req.intel().present())<span class="${value}">${rqL || rqS || rqD ? ", " : ""}${req.intel().text()} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.int")}</span>@endif
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
				@for(int li = 0; li < displayExplicits.size(); li++)
					@if(!corruptedWords.contains(displayExplicits.get(li)))
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{net.luversof.web.gate.poe.PoeText.highlightValues(displayExplicits.get(li))}</div>
					@endif
					@if(remindersPerLine && reminders.get(li) != null)
						@for(String rem : reminders.get(li))
							<div class="text-[11px] leading-snug text-[#7f7f7f]" data-tt-reminder>${rem}</div>
						@endfor
					@endif
				@endfor
				@for(String rem : remindersAtEnd)
					<div class="text-[11px] leading-snug text-[#7f7f7f]" data-tt-reminder>${rem}</div>
				@endfor
			</div>
		@endif

		<%-- 로어(플레이버) — 인게임 고유 툴팁 맨 아래 주황 기울임(10-01, 위키 + 게임 FlavourText 원문) --%>
		<%-- 타락 줄 = 옵션 바로 아래 · 로어 위(10-09 C216 — poedb 목소리 · poe2db 신념의 프리즘 툴팁 순서: 옵션 → Corrupted → 로어 → 사용 설명. 예전(C157)엔 맨 아래) --%>
		@if(corruptedItem)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#d20000]" data-tt-corrupted>${MessageUtil.getMessage("poe.items.corrupted")}</div>
		@endif
		@if(flavourLines != null && !flavourLines.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center space-y-0.5" data-tt-flavour>
				@for(String line : flavourLines)
					<div class="text-[12px] leading-snug italic text-[#af6025]">${line}</div>
				@endfor
			</div>
		@endif
		<%-- 사용 설명(10-09 C212 — 게임 ClientStrings ItemDescriptionFlask · Tincture · PassiveJewel · AbyssJewel · AfflictionJewel*, 인게임 툴팁 맨 아래 회색 문단) --%>
		!{ String helpKey = net.luversof.web.gate.poe.PoeItemDescription.key(item.category(), item.baseType()); }
		@if(helpKey != null)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center text-[12px] leading-snug text-[#7f7f7f]" data-tt-help>${MessageUtil.getMessage(helpKey)}</div>
		@endif
	</div>
