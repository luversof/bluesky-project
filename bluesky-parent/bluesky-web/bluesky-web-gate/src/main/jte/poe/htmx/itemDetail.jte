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
	String displayClass = PoeText.name(item.itemClassKo(), item.itemClass());

	// 요구 사항은 인게임처럼 한 줄(값-라벨 순): "요구 사항 레벨 X, N 힘, M 민첩". 베이스는 dropLevel=요구 레벨.
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
	<div class="poe-tooltip poe-rar-white w-80 max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
		<div class="poe-tt-band">
			<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:#e6e6e6">${displayName}</div>
			<div class="text-center text-[12px] leading-tight text-[#c8c8c8] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${displayClass}</div>
		</div>
		@if(PoeText.isKorean())
			<div class="px-4 pt-1 text-center text-[10px] text-white/35">${item.name()}</div>
		@endif

		<%-- 아이템 클래스 속성 (방어/무기 수치) --%>
		@if(hasProps)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-[12px] space-y-0.5">
				@if(item.weapon() != null)
					<div class="flex justify-between"><span class="text-[#7f7f7f]">${physicalDamageLabel}</span><span class="text-[#e0e0e0] font-medium">${item.weapon().damageMin()}-${item.weapon().damageMax()}</span></div>
					<div class="flex justify-between"><span class="text-[#7f7f7f]">${critChanceLabel}</span><span class="text-white">${String.format("%.2f", item.weapon().critChance())}%</span></div>
					<div class="flex justify-between"><span class="text-[#7f7f7f]">${attacksPerSecondLabel}</span><span class="text-white">${String.format("%.2f", item.weapon().attacksPerSecond())}</span></div>
					@if(showWeaponRange)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${weaponRangeLabel}</span><span class="text-white">${String.format("%.1f", item.weapon().range() / 10.0)}${PoeText.isKorean() ? "미터" : "m"}</span></div>
					@endif
				@endif
				@if(item.armour() != null)
					@if(item.armour().block() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${MessageUtil.getMessage("poe.items.detail.block")}</span><span class="text-[#e0e0e0] font-medium">${item.armour().block()}%</span></div>
					@endif
					@if(item.armour().armourMax() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${armourLabel}</span><span class="text-[#e0e0e0] font-medium">${item.armour().armourMin()}-${item.armour().armourMax()}</span></div>
					@endif
					@if(item.armour().evasionMax() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${evasionLabel}</span><span class="text-[#e0e0e0] font-medium">${item.armour().evasionMin()}-${item.armour().evasionMax()}</span></div>
					@endif
					@if(item.armour().energyShieldMax() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${energyShieldLabel}</span><span class="text-[#e0e0e0] font-medium">${item.armour().energyShieldMin()}-${item.armour().energyShieldMax()}</span></div>
					@endif
					@if(item.armour().wardMax() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${wardLabel}</span><span class="text-[#e0e0e0] font-medium">${item.armour().wardMin()}-${item.armour().wardMax()}</span></div>
					@endif
				@endif
			</div>
		@endif

		<%-- 플라스크 속성 (인게임: 회복/지속 + 충전) — type 1생명 2마나 3하이브리드 4특수 --%>
		@if(flask != null)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5">
				@if(flask.type() == 4)
					<div class="text-[#e0e0e0]">${java.text.MessageFormat.format(flaskDurationLabel, String.format("%.2f", flask.durationSeconds()))}</div>
				@else
					@if(flask.lifePerUse() > 0)
						<div class="text-[#e0e0e0]">${java.text.MessageFormat.format(flaskLifeLabel, String.format("%.2f", flask.durationSeconds()), String.valueOf(flask.lifePerUse()))}</div>
					@endif
					@if(flask.manaPerUse() > 0)
						<div class="text-[#e0e0e0]">${java.text.MessageFormat.format(flaskManaLabel, String.format("%.2f", flask.durationSeconds()), String.valueOf(flask.manaPerUse()))}</div>
					@endif
				@endif
				@if(flask.maxCharges() > 0)
					<div class="text-[#7f7f7f]">${java.text.MessageFormat.format(flaskChargesLabel, String.valueOf(flask.maxCharges()), String.valueOf(flask.perCharge()))}</div>
				@endif
			</div>
		@endif

		<%-- 요구 사항 — 인게임처럼 한 줄(요구 사항 레벨 X, N 힘, M 민첩, K 지능). 베이스는 dropLevel=요구 레벨. --%>
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px]">
			<span class="text-[#7f7f7f]">${requirementLabel} </span>
			<span class="text-[#7f7f7f]">${levelLabel} </span><span class="text-[#e0e0e0]">${item.dropLevel()}</span>@if(item.reqStr() > 0)<span class="text-[#e0e0e0]">, ${item.reqStr()} </span><span class="text-[#7f7f7f]">${reqStrLabel}</span>@endif@if(item.reqDex() > 0)<span class="text-[#e0e0e0]">, ${item.reqDex()} </span><span class="text-[#7f7f7f]">${reqDexLabel}</span>@endif@if(item.reqInt() > 0)<span class="text-[#e0e0e0]">, ${item.reqInt()} </span><span class="text-[#7f7f7f]">${reqIntLabel}</span>@endif
		</div>

		<%-- 암시 모드 --%>
		@if(!item.implicits().isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center space-y-0.5">
				@for(var implicitLine : item.implicits())
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(PoeText.name(implicitLine.ko(), implicitLine.en()))}</div>
				@endfor
			</div>
		@endif

		<%-- 특수 플라스크 부여 버프 (인게임: 임플리싯식 파랑 모드 줄 — 자수정 카오스저항, 화강암 방어도 등) --%>
		@if(flask != null && flask.buffLines() != null && !flask.buffLines().isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center space-y-0.5">
				@for(var buffLine : flask.buffLines())
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(PoeText.name(buffLine.ko(), buffLine.en()))}</div>
				@endfor
			</div>
		@endif

		<%-- 가능한 모드 티어 열거는 레이어에서 제외(의미 없음, 사용자 요청). 전체 모드 풀은 상세 페이지(/poe/items/{slug})에서 확인. --%>
	</div>
