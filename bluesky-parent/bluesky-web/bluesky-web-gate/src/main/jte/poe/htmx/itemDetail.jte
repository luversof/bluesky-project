@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeBaseItem item

!{
	String closeLabel = MessageUtil.getMessage("common.button.close");
	String dropLevelLabel = MessageUtil.getMessage("poe.items.detail.drop.level");
	String requirementLabel = MessageUtil.getMessage("poe.items.detail.requirement");
	String armourLabel = MessageUtil.getMessage("poe.items.detail.armour");
	String evasionLabel = MessageUtil.getMessage("poe.items.detail.evasion");
	String energyShieldLabel = MessageUtil.getMessage("poe.items.detail.energy.shield");
	String wardLabel = MessageUtil.getMessage("poe.items.detail.ward");
	String physicalDamageLabel = MessageUtil.getMessage("poe.items.detail.physical.damage");
	String critChanceLabel = MessageUtil.getMessage("poe.gems.detail.crit.chance");
	String attacksPerSecondLabel = MessageUtil.getMessage("poe.items.detail.attacks.per.second");
	String weaponRangeLabel = MessageUtil.getMessage("poe.items.detail.range");
	// 무기 범위는 근접 무기만 인게임 표시(활/완드=투사체라 미표시, range=120 센티넬 → <100 근접만). 미터=내부 데시미터/10.
	boolean showWeaponRange = item.weapon() != null && item.weapon().range() > 0 && item.weapon().range() < 100;
	String strLabel = MessageUtil.getMessage("poe.build.stat.armour");
	String displayName = PoeText.name(item.nameKo(), item.name());
	String displayClass = net.luversof.web.gate.poe.PoeClassNames.poe1(item.itemClassKo(), item.itemClass());

	// 요구 사항은 인게임처럼 한 줄(값-라벨 순): "요구 사항 레벨 X, N 힘, M 민첩". 베이스 요구 레벨 = PoB req.level(Q103 — 드롭 레벨 아님).
	String levelLabel = MessageUtil.getMessage("poe.items.detail.level");
	String reqStrLabel = MessageUtil.getMessage("poe.items.detail.str");
	String reqDexLabel = MessageUtil.getMessage("poe.items.detail.dex");
	String reqIntLabel = MessageUtil.getMessage("poe.items.detail.int");
	// 플라스크 속성 라벨(MessageFormat: 지속/회복 초 {0}, 수치 {1} · 충전 최대 {0}/사용당 {1})
	String flaskLifeLabel = MessageUtil.getMessage("poe.items.detail.flask.life");
	String flaskManaLabel = MessageUtil.getMessage("poe.items.detail.flask.mana");
	String flaskDurationLabel = MessageUtil.getMessage("poe.items.detail.flask.duration");
	String flaskChargesLabel = MessageUtil.getMessage("poe.items.detail.flask.charges");
	var flask = item.flask();
	// 일반(흰색) 아이템 색
	String rarity = "#c8c8c8";
	boolean hasProps = (item.armour() != null && (item.armour().armourMax() > 0 || item.armour().evasionMax() > 0 || item.armour().energyShieldMax() > 0 || item.armour().wardMax() > 0)) || item.weapon() != null;
}

<%-- PoE 게임 아이템 툴팁 (일반/흰색) --%>
	<div class="poe-tooltip poe-rar-white w-80 max-w-[calc(100vw-2rem)] max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
		<div class="poe-tt-band">
			<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:#c8c8c8">${displayName}</div>
		</div>
		@if(PoeText.isKorean())
			<div class="px-4 pt-1 text-center text-[10px] text-white/35">${item.name()}</div>
		@endif

		<%-- 아이템 종류 속성 (방어/무기 수치) — 인게임처럼 가운데 '라벨: 값'(10-23 Q43 — 왼쪽 라벨 · 오른쪽 값 양끝 정렬이었다), 무기는 첫 줄 종류 --%>
		!{ String weaponCls = net.luversof.web.gate.poe.PoeClassNames.poe1Weapon(item.itemClass()); }
		@if("AbyssJewel".equals(item.itemClass()))
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px] text-[#7f7f7f]" data-tt-abyss>${PoeText.isKorean() ? "심연" : "Abyss"}</div>
		@endif
		@if(hasProps)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
				@if(item.weapon() != null && weaponCls != null)<div class="text-[#7f7f7f]" data-tt-weapon-class>${weaponCls}</div>@endif
				@if(item.weapon() != null)
					<div><span class="text-[#7f7f7f]">${physicalDamageLabel}: </span><span class="text-[#e0e0e0]">${item.weapon().damageMin()}-${item.weapon().damageMax()}</span></div>
					<div><span class="text-[#7f7f7f]">${critChanceLabel}: </span><span class="text-[#e0e0e0]">${String.format("%.2f", item.weapon().critChance())}%</span></div>
					<div><span class="text-[#7f7f7f]">${attacksPerSecondLabel}: </span><span class="text-[#e0e0e0]">${String.format("%.2f", item.weapon().attacksPerSecond())}</span></div>
					@if(showWeaponRange)
						<div><span class="text-[#7f7f7f]">${weaponRangeLabel}: </span><span class="text-[#e0e0e0]">${String.format("%.1f", item.weapon().range() / 10.0)}${PoeText.isKorean() ? "미터" : (item.weapon().range() == 10 ? " metre" : " metres")}</span></div>
					@endif
				@endif
				@if(item.armour() != null)
					@if(item.armour().block() > 0)
						<div><span class="text-[#7f7f7f]">${MessageUtil.getMessage("poe.items.detail.block")}: </span><span class="text-[#e0e0e0]">${item.armour().block()}%</span></div>
					@endif
					@if(item.armour().armourMax() > 0)
						<div><span class="text-[#7f7f7f]">${armourLabel}: </span><span class="text-[#e0e0e0]">${item.armour().armourMin()}-${item.armour().armourMax()}</span></div>
					@endif
					@if(item.armour().evasionMax() > 0)
						<div><span class="text-[#7f7f7f]">${evasionLabel}: </span><span class="text-[#e0e0e0]">${item.armour().evasionMin()}-${item.armour().evasionMax()}</span></div>
					@endif
					@if(item.armour().energyShieldMax() > 0)
						<div><span class="text-[#7f7f7f]">${energyShieldLabel}: </span><span class="text-[#e0e0e0]">${item.armour().energyShieldMin()}-${item.armour().energyShieldMax()}</span></div>
					@endif
					@if(item.armour().wardMax() > 0)
						<div><span class="text-[#7f7f7f]">${wardLabel}: </span><span class="text-[#e0e0e0]">${item.armour().wardMin()}-${item.armour().wardMax()}</span></div>
					@endif
					<%-- 이동 속도 감소(10-09 C223 — 갑옷 · 방패, PoB Bases MovementPenalty · poedb 툴팁 "이동 속도: -3%", PoE2 와 같은 키) --%>
					@if(item.armour().movementPenalty() != null && item.armour().movementPenalty() > 0)
						<div data-tt-movement><span class="text-[#7f7f7f]">${MessageUtil.getMessage("poe2.items.movement")}: </span><span class="text-[#e0e0e0]">-${item.armour().movementPenalty()}%</span></div>
					@endif
				@endif
			</div>
		@endif

		<%-- 플라스크 속성 (인게임: 회복/지속 + 충전) — type 1생명 2마나 3하이브리드 4특수 --%>
		@if(flask != null)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5">
				@if(flask.type() == 4)
					<div class="text-[#7f7f7f]">$unsafe{java.text.MessageFormat.format(flaskDurationLabel, "<span class=\"text-[#e0e0e0]\">" + String.format("%.2f", flask.durationSeconds()) + "</span>")}</div>
				@else
					@if(flask.lifePerUse() > 0)
						<div class="text-[#7f7f7f]">$unsafe{java.text.MessageFormat.format(flaskLifeLabel, "<span class=\"text-[#e0e0e0]\">" + String.format("%.2f", flask.durationSeconds()) + "</span>", "<span class=\"text-[#e0e0e0]\">" + flask.lifePerUse() + "</span>")}</div>
					@endif
					@if(flask.manaPerUse() > 0)
						<div class="text-[#7f7f7f]">$unsafe{java.text.MessageFormat.format(flaskManaLabel, "<span class=\"text-[#e0e0e0]\">" + String.format("%.2f", flask.durationSeconds()) + "</span>", "<span class=\"text-[#e0e0e0]\">" + flask.manaPerUse() + "</span>")}</div>
					@endif
				@endif
				@if(flask.maxCharges() > 0)
					<div class="text-[#7f7f7f]">$unsafe{java.text.MessageFormat.format(flaskChargesLabel, "<span class=\"text-[#e0e0e0]\">" + flask.maxCharges() + "</span>", "<span class=\"text-[#e0e0e0]\">" + flask.perCharge() + "</span>")}</div>
					<%-- 현재 충전량(10-09 C221 — ClientStrings ItemDisplayChargesNCharges, 새 아이템 = 최대 충전) --%>
					<div class="text-[#7f7f7f]" data-tt-charges-now>$unsafe{java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.detail.flask.current"), "<span class=\"text-[#e0e0e0]\">" + flask.maxCharges() + "</span>")}</div>
					<%-- 플라스크 부여 버프 = 충전 줄 바로 아래 파란 줄(10-09 C241 — PoB ItemsTab.lua buffModLines · poedb 죽어가는 해 순서. 예전(C196·C204)엔 옵션 뒤 별도 칸) --%>
					@if(flask != null && flask.buffLines() != null && !flask.buffLines().isEmpty())
						@for(var buffLine : flask.buffLines())
							<div class="text-[#8888ff] whitespace-pre-line" data-tt-flask-buff>$unsafe{PoeText.highlightValues(PoeText.name(buffLine.ko(), buffLine.en()))}</div>
						@endfor
					@endif
				@endif
			</div>
		@endif

		<%-- 요구 사항 — 인게임처럼 한 줄(요구 사항 레벨 X, N 힘, M 민첩, K 지능). 베이스 요구 레벨 = PoB req.level(Q103 — 드롭 레벨 아님). --%>
		<%-- 레벨 1 · 능력치 요구 없음이면 요구 줄 없음(Q43 — 고유 화면과 같게) --%>
		@if(item.requiredLevel() > 1 || item.reqStr() > 0 || item.reqDex() > 0 || item.reqInt() > 0)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px]">
			<span class="text-[#7f7f7f]">${requirementLabel} </span>
			<span class="text-[#7f7f7f]">${levelLabel} </span><span class="text-[#e0e0e0]">${item.requiredLevel()}</span>@if(item.reqStr() > 0)<span class="text-[#e0e0e0]">, ${item.reqStr()} </span><span class="text-[#7f7f7f]">${reqStrLabel}</span>@endif@if(item.reqDex() > 0)<span class="text-[#e0e0e0]">, ${item.reqDex()} </span><span class="text-[#7f7f7f]">${reqDexLabel}</span>@endif@if(item.reqInt() > 0)<span class="text-[#e0e0e0]">, ${item.reqInt()} </span><span class="text-[#7f7f7f]">${reqIntLabel}</span>@endif
		</div>
		@endif

		<%-- 암시 모드 --%>
		@if(!item.implicits().isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center space-y-0.5">
				@for(var implicitLine : item.implicits())
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(PoeText.name(implicitLine.ko(), implicitLine.en()))}</div>
					<%-- 줄 밑 리마인더(인게임 회색 부연, 10-04 C116) --%>
					@for(String rem : PoeText.lines(implicitLine.remindersKo(), implicitLine.reminders()) == null ? java.util.List.<String>of() : PoeText.lines(implicitLine.remindersKo(), implicitLine.reminders()))
						<div class="text-[11px] leading-snug text-[#7f7f7f]" data-tt-reminder>${rem}</div>
					@endfor
				@endfor
			</div>
		@endif


		<%-- 사용 설명(10-09 C213 — 게임 ClientStrings ItemDescription*, 고유 상세 C212 와 같은 맨 아래 회색 문단) --%>
		!{ String helpKey = net.luversof.web.gate.poe.PoeItemDescription.key(item.category(), item.name()); }
		@if(helpKey != null)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center text-[12px] leading-snug text-[#7f7f7f]" data-tt-help>${MessageUtil.getMessage(helpKey)}</div>
		@endif

		<%-- 가능한 모드 등급 열거는 레이어에서 제외(의미 없음, 사용자 요청). 전체 모드 풀은 상세 페이지(/poe/items/{slug})에서 확인. --%>
	</div>
