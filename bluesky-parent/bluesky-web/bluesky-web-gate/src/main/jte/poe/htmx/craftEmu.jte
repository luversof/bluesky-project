@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.PoeModRange
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.View view
@param String error = null
@param String lastAction = null
@param java.util.List<String> added = java.util.List.of()
@param String stateJson = "{}"
@param String historyJson = "[]"
@param int historySize = 0
@param java.util.List<PoeCraft.PoolLine> pool = java.util.List.of()
@param java.util.List<net.luversof.web.gate.poe.CraftPoolGroups.Family<PoeCraft.PoolLine>> poolGroups = java.util.List.of()
@param java.util.List<PoeCraft.Essence> essences = java.util.List.of()
@param String classLabel = ""
@param String baseName = ""
@param String baseNameKo = ""
@param String itemName = ""
@param String itemNameKo = ""
@param net.luversof.web.gate.poe.dto.PoeBaseItem craftBase = null
@param java.util.List<String> craftEnchant = null
@param Integer passives = null
@param String spamNote = null
@param PoeCraft.Options options = null
@param String imprintJson = ""
@param String imprintRollsJson = "{}"
@param String foresee = ""
@param PoeCraft.View preview = null
@param String previewAction = null
@param String previewArg = null
@param String spentJson = "[]"
@param PoeCraft.SpentResult spent = null
@param String rollsJson = "{}"
@param java.util.Map<String, Long> rolls = java.util.Map.of()
@param String rollHistJson = "[]"
@param java.util.List<PoeCraft.HistRow> historyRows = java.util.List.of()
@param Integer historyStartUndo = null
@param int historyDropped = 0
@param net.luversof.web.gate.poe.dto.PoeCraft.OptimizeNext recommend = null
@param String optJob = null
@param java.util.List<PoeCraft.View> ghosts = null
@param java.util.List<String> ghostSeeds = null
@param String ghostAction = null
@param int ghost = 0

<%-- PoE1 제작 에뮬레이터 결과 구역(10-15 C335) — 화폐를 누르면 이 구역만 바꿔 끼운다. 아이템 상태 · 되돌리기 이력은 숨은 칸(서버는 상태를 안 든다).
     툴팁은 인게임 배색(일반 흰색 · 매직 파랑 · 레어 노랑 헤더, 옵션 파랑), 줄마다 그 풀 안 등급(T1 = 최상위)과 수치 범위. --%>
!{
	boolean ko = PoeText.isKorean();
	PoeCraft.Item item = view.item();
	String rarity = item.rarity() == null ? "NORMAL" : item.rarity();
	String rarCls = switch (rarity) { case "MAGIC" -> "poe-rar-magic"; case "RARE" -> "poe-rar-rare"; default -> "poe-rar-white"; };
	String headColor = switch (rarity) { case "MAGIC" -> "#8888ff"; case "RARE" -> "#ffff77"; default -> "#c8c8c8"; };
	// 테두리 = 이름 색(다른 아이템 툴팁 buildSummary → itemTooltip 의 rarityBorder 와 같은 규칙)
	String borderColor = headColor;
	java.util.List<PoeCraft.Line> lines = view.lines() == null ? java.util.List.of() : view.lines();
	long prefixes = lines.stream().filter(l -> "prefix".equals(l.gen())).count();
	long suffixes = lines.stream().filter(l -> "suffix".equals(l.gen())).count();
	// 카드 이름은 베이스(C487 — 고른 베이스 · 가져온 베이스, 없으면 종류 이름)
	String baseLabel = baseName == null || baseName.isBlank() ? classLabel : (ko && baseNameKo != null && !baseNameKo.isBlank() ? baseNameKo : baseName);
	// 매직 이름 = 접두 이름 + 베이스 + 접미 이름(인게임과 같은 조합). 레어는 가져온 이름, 없으면 "레어 아이템"
	String magicName = baseLabel;
	// 레어 이름(C488) — 가져온 이름 · 레어가 될 때 지은 종류별 무작위 이름(영 · 한)
	String shownName = ko && itemNameKo != null && !itemNameKo.isBlank() ? itemNameKo : itemName;
	if ("MAGIC".equals(rarity)) {
		String pre = lines.stream().filter(l -> "prefix".equals(l.gen())).map(l -> ko && l.nameKo() != null ? l.nameKo() : l.name()).findFirst().orElse(null);
		String suf = lines.stream().filter(l -> "suffix".equals(l.gen())).map(l -> ko && l.nameKo() != null ? l.nameKo() : l.name()).findFirst().orElse(null);
		magicName = (pre != null ? pre + " " : "") + baseLabel + (suf != null ? " " + suf : "");
	}
	boolean normal = "NORMAL".equals(rarity), magic = "MAGIC".equals(rarity), rare = "RARE".equals(rarity);
	int max = view.maxPerGen();
	boolean full = prefixes >= max && suffixes >= max;
	boolean removable = lines.stream().anyMatch(l -> !l.fractured());
	record Btn(String action, String icon, String ko, String en, boolean enabled) {}
	java.util.List<Btn> btns = java.util.List.of(
		new Btn("TRANSMUTE", "currencyupgradetomagic", "진화의 오브", "Orb of Transmutation", normal),
		new Btn("ALTERATION", "currencyrerollmagic", "변화의 오브", "Orb of Alteration", magic),
		new Btn("AUGMENTATION", "currencyaddmodtomagic", "확장의 오브", "Orb of Augmentation", magic && lines.size() < 2),
		new Btn("REGAL", "currencyupgrademagictorare", "제왕의 오브", "Regal Orb", magic),
		new Btn("ALCHEMY", "currencyupgradetorare", "연금술의 오브", "Orb of Alchemy", normal),
		new Btn("CHAOS", "currencyrerollrare", "카오스 오브", "Chaos Orb", rare),
		new Btn("EXALT", "currencyaddmodtorare", "엑잘티드 오브", "Exalted Orb", rare && !full),
		new Btn("ANNUL", "currencyremovemod", "소멸의 오브", "Orb of Annulment", !normal && removable),
		new Btn("SCOUR", "currencyconverttonormal", "정제의 오브", "Orb of Scouring", !normal),
		new Btn("FRACTURE", "currencyfracturerare", "분열의 오브", "Fracturing Orb", rare && lines.size() >= 4 && lines.stream().noneMatch(PoeCraft.Line::fractured) && (item.influence() == null || item.influence().isEmpty())),
		// 장막(10-15 C341 — 신디케이트) — 카오스처럼 다시 굴리되 장막 옵션 하나 보장 / 무작위 옵션 하나를 장막 옵션으로
		new Btn("VEILED_CHAOS", "currencyrerollrareveiledchaos", "장막의 카오스 오브", "Veiled Chaos Orb", rare),
		// 장막 옵션이 있으면 끔(C415 — 인게임 "이미 장막 속성 부여가 있습니다.")
		new Btn("VEILED_EXALT", "currencyrerollrareveiled", "장막의 엑잘티드 오브", "Veiled Exalted Orb", rare && removable && lines.stream().noneMatch(l -> l.id().startsWith("Veiled"))),
		// 장막 해제(10-20 사용자 요청 "가려져 있는 상태에서 선택하면 장막 옵션 중 하나를 고르는 식으로 단계가") — 인게임처럼 장막 옵션은 가려진 채 남고,
		// 이 단추를 눌러야 셋 중 하나를 고르는 칸이 뜬다(아이템은 그대로 · 화폐 안 듦). 아이콘 없음(전용 화폐가 아님)
		new Btn("UNVEIL_OPEN", null, "장막 해제", "Unveil", lines.stream().anyMatch(l -> l.id().startsWith("Veiled")) && !item.corrupted()),
		// 삿된 화폐(C401) — 보통 확장 · 제왕 · 엑잘과 같되 "레벨이 높은 속성이 붙을 가능성이 더 높음"(가중은 축성된 화석과 같다고 가정)
		new Btn("FOULBORN_AUG", "currencymutatedaddmodtomagic", "삿된 확장의 오브", "Foulborn Orb of Augmentation", magic && lines.size() < 2 && !item.corrupted()),
		new Btn("FOULBORN_REGAL", "currencymutatedupgrademagictorare", "삿된 제왕의 오브", "Foulborn Regal Orb", magic && !item.corrupted()),
		new Btn("FOULBORN_EXALT", "currencymutatedaddmodtorare", "삿된 엑잘티드 오브", "Foulborn Exalted Orb", rare && !full && !item.corrupted()),
		// 바알 오브(10-16 C358) — 타락 + 넷 중 하나(변화 없음 · 소켓 · 레어 재구성 · 부패 고정 속성). 타락하면 더는 못 바꾼다
		new Btn("VAAL", "currencycorrupt", "바알 오브", "Vaal Orb", !item.corrupted()));
	java.util.List<PoeCraft.Line> unveil = view.unveilOffers() == null ? java.util.List.of() : view.unveilOffers();
	// 엘드리치(C343) — 고정 속성 줄 · 등급 이름(ClientStrings EldritchCurrencyTier1~6) · 진영 이름(ItemPopup…Item)
	java.util.List<PoeCraft.Line> implicits = view.implicits() == null ? java.util.List.of() : view.implicits();
	String[] tierKo = {"하급", "상급", "우수한", "특출난", "정교한", "완벽한"};
	String[] tierEn = {"Lesser", "Greater", "Grand", "Exceptional", "Exquisite", "Perfect"};
	// 결과에 영향을 주는 메타 옵션(잠금 · 공격/주문 금지) — 에센스 · 화석을 못 쓴다(C430, CoE 'Prevents usage', 인게임 ItemErrorItemHasCraftingMetaMod)
	boolean outcomeMeta = lines.stream().anyMatch(l -> l.id() != null && (l.id().contains("CannotChangePrefixes") || l.id().contains("CannotChangeSuffixes") || l.id().contains("CannotRollAttackAffixes") || l.id().contains("CannotRollCasterAffixes")));
	boolean eldritchClass = java.util.Set.of("Body Armour", "Boots", "Gloves", "Helmet").contains(item.itemClass());
	// 방금 쓴 화폐의 묶음은 열린 채로(10-15 C343 — 다시 그리면 접혀서 불씨 → 영액처럼 이어 쓸 때마다 다시 펼쳐야 했다)
	String la = lastAction == null ? "" : lastAction;
	String openGroup = la.startsWith("ESSENCE") ? "essence" : la.startsWith("FOSSIL") ? "fossil" : la.startsWith("HARVEST") ? "harvest" : la.startsWith("BENCH") ? "bench"
		: (la.startsWith("CONQUEROR") || la.startsWith("DOMINANCE") || la.startsWith("AWAKENER")) ? "influence" : la.startsWith("DUCAT") ? "ducat"
		: (la.startsWith("EMBER") || la.startsWith("ICHOR") || la.startsWith("ELDRITCH") || la.startsWith("CONFLICT")) ? "eldritch"
		: la.startsWith("MANUAL") ? "manual" : (la.startsWith("IMPRINT") || la.startsWith("BEAST")) ? "beast" : la.startsWith("TAINTED") ? "tainted" : la.startsWith("CATALYST") ? "catalyst" : "";
	// 화폐 인게임 사용 설명(C340, CurrencyItems.Directions) — 버튼 설명(이름 = 영문 키)
	java.util.Map<String, PoeCraft.CurrencyInfo> infoByName = new java.util.HashMap<>();
	if (options != null && options.currencies() != null) for (PoeCraft.CurrencyInfo ci : options.currencies()) infoByName.put(ci.name(), ci);
	java.util.Map<String, String> blocked = view.blocked() == null ? java.util.Map.of() : ko ? view.blocked() : net.luversof.web.gate.poe.PoeCraftText.en(view.blocked());
	java.util.function.Function<String, String> tip = en -> { PoeCraft.CurrencyInfo ci = infoByName.get(en); if (ci == null) return null; String d = ko && ci.directionsKo() != null ? ci.directionsKo() : ci.directions(); return (ko ? ci.nameKo() : ci.name()) + (d == null || d.isEmpty() ? "" : " — " + d); };
	// 영향력 키 → 정복자 이름 · 오브 아이콘 · 영향력 아이콘(10-15 C338) — 십자군 · 대속자(eyrie) · 사냥꾼(basilisk) · 전쟁군주(adjudicator)
	java.util.Map<String, String[]> conq = new java.util.LinkedHashMap<>();
	conq.put("crusader", new String[] {"십자군", "Crusader", "addmodtorarecrusader", "influence-crusader", "Crusader's Exalted Orb"});
	conq.put("eyrie", new String[] {"대속자", "Redeemer", "addmodtorareredeemer", "influence-redeemer", "Redeemer's Exalted Orb"});
	conq.put("basilisk", new String[] {"사냥꾼", "Hunter", "addmodtorarehunter", "influence-hunter", "Hunter's Exalted Orb"});
	conq.put("adjudicator", new String[] {"전쟁군주", "Warlord", "addmodtorarewarlord", "influence-warlord", "Warlord's Exalted Orb"});
	// 셰이퍼 · 엘더의 엑잘티드 오브(C391 — CurrencyItems add_influence_mod_to_rare, 인게임 이름 "쉐이퍼" · "엘더")
	conq.put("shaper", new String[] {"쉐이퍼", "Shaper", "currencyaddmodtorareshaper", "influence-shaper", "Shaper's Exalted Orb"});
	conq.put("elder", new String[] {"엘더", "Elder", "currencyaddmodtorareelder", "influence-elder", "Elder's Exalted Orb"});
	String inf = item.influence() == null ? "" : item.influence();
	// 머리 아이콘(인게임): 영향력 하나 = 양쪽 같은 아이콘, 둘(각성자의 오브, C346) = 왼쪽 · 오른쪽 각각, 엘드리치 = 왼쪽 총주교 · 오른쪽 포식자
	java.util.Map<String, String> infIcon = java.util.Map.of("shaper", "influence-shaper", "elder", "influence-elder", "crusader", "influence-crusader", "eyrie", "influence-redeemer", "basilisk", "influence-hunter", "adjudicator", "influence-warlord");
	String[] infs = inf.isEmpty() ? new String[0] : inf.split(",");
	String iconL = infs.length > 0 ? infIcon.get(infs[0]) : null;
	String iconR = infs.length > 1 ? infIcon.get(infs[1]) : iconL;
	if (infs.length == 0 && view.implicits() != null) {
		for (PoeCraft.Line il : view.implicits()) {
			if ("exarch".equals(il.gen())) iconL = "influence-exarch";
			if ("eater".equals(il.gen())) iconR = "influence-eater";
		}
	}
	// 분열 심볼(10-23 Q112) — 분열 옵션이 있으면 비어 있는 · 같은 아이콘인 오른쪽 자리에
	if (lines.stream().anyMatch(PoeCraft.Line::fractured) && java.util.Objects.equals(iconL, iconR)) {
		iconR = "influence-fractured";
		if (iconL == null) iconL = iconR;
	}
	String[] infInfo = iconL != null || iconR != null ? new String[] {"", "", "", iconL != null ? iconL : iconR} : null;
	PoeCraft.Meta meta = view.meta();
	long craftedCount = lines.stream().filter(PoeCraft.Line::crafted).count();
	String poolTitle = normal ? (ko ? "다음 옵션 확률 — 레어로 만들 때(연금 · 에센스)" : "Next modifier odds — when made rare (alchemy, essence)")
		: (ko ? "다음 옵션 확률 — 지금 아이템에 붙을 수 있는 것" : "Next modifier odds — what can still roll on this item");
	java.util.function.Function<java.util.List<String>, java.util.List<String>> nz = l -> l == null ? java.util.List.of() : l;
	// 기폭제(C385) — 퀄리티 종류 · 양, 수치가 오르는 옵션 id, 기폭제 화폐(장신구만 보인다)
	String qKey = null;
	int qAmt = 0;
	if (item.quality() != null && item.quality().indexOf(':') > 0) {
		qKey = item.quality().substring(0, item.quality().indexOf(':'));
		try { qAmt = Integer.parseInt(item.quality().substring(item.quality().indexOf(':') + 1)); } catch (NumberFormatException e) { qAmt = 0; }
	}
	java.util.List<String> boostedIds = view.boosted() == null ? java.util.List.of() : view.boosted();
	java.util.List<PoeCraft.CurrencyInfo> catalysts = options == null || options.currencies() == null ? java.util.List.of() : options.currencies().stream().filter(c -> c.catalyst() != null).toList();
	PoeCraft.CurrencyInfo qInfo = null;
	for (PoeCraft.CurrencyInfo c : catalysts) if (c.catalyst().equals(qKey)) qInfo = c;
	boolean jewellery = java.util.Set.of("Ring", "Amulet", "Belt").contains(item.itemClass());
}
<%-- 화폐 클릭은 조회가 아니라 시험 적용이라 "처리되었습니다" 토스트를 띄우지 않는다(누를 때마다 쌓였다) --%>
<%-- C449 UX QA(PoE2 C440 과 같은 공용 장치): 누른 단추가 든 상자를 제자리에 · 열어 둔 묶음 유지 · 오류가 헤더 밑 · 화면 밖이면 보이게 --%>
<div id="craft-emu" data-craft-arm-scope class="space-y-3" data-craft-rarity="${rarity}" data-no-success-toast data-swap-anchor="#craft-emu [data-craft-actions]" data-keep-open="data-craft-group" data-error-into-view=".craft-error-reveal" data-keep-values="essence,harvest,bench,fossil">
	<form hx-post="/poe/htmx/craft" hx-target="#craft-emu" hx-swap="outerHTML" class="space-y-2 min-w-0" hx-include="#craft-opt-result input[name=optJob]" data-craft-toolbar>
		<input type="hidden" name="itemClass" value="${item.itemClass()}">
		<input type="hidden" name="variant" value="${item.variant()}">
		<input type="hidden" name="influence" value="${item.influence()}">
		<input type="hidden" name="ilvl" value="${item.ilvl()}">
		<input type="hidden" name="classLabel" value="${classLabel}">
		<input type="hidden" name="baseName" value="${baseName}">
		<input type="hidden" name="baseNameKo" value="${baseNameKo}">
		<input type="hidden" name="itemName" value="${itemName}">
		<input type="hidden" name="itemNameKo" value="${itemNameKo}">
		<input type="hidden" name="state" value="${stateJson}">
		<input type="hidden" name="history" value="${historyJson}">
		<input type="hidden" name="imprint" value="${imprintJson}">
		<input type="hidden" name="imprintRolls" value="${imprintRollsJson}">
		<input type="hidden" name="foresee" value="${foresee}">
		<input type="hidden" name="previewArg" value="${previewArg}">
		<input type="hidden" name="spent" value="${spentJson}">
		<input type="hidden" name="rolls" value="${rollsJson}">
		<input type="hidden" name="rollHistory" value="${rollHistJson}">
		@if(passives != null)<input type="hidden" name="passives" value="${passives}">@endif
		<%-- 올플레임 제작(10-20 C523, 3.29 Deepwater — ClientStrings DeepwaterCraftingUIInfo) — 켜면 화폐 한 번에 원본에 각각 적용한 실체 없는 복제본 N 개 중 하나를 고른다 --%>
		<div class="flex flex-wrap items-center justify-between gap-2 text-xs" data-craft-ghost-row>
			<label class="flex items-center gap-1" title="${ko ? "인게임: 아이템과 화폐를 넣고 망자의 유황을 내면, 그 화폐를 원본에 각각 적용한 실체 없는 복제본 여럿 중 하나를 가진다(화폐 효과가 복제본 수만큼). 복제본 수(2~4)는 화폐마다 달라 직접 고릅니다. 타락한 아이템 · 히네코라의 자물쇠 · 바알 · 품질 화폐는 안 됩니다" : "In game: place an item and a currency and pay Dead Man's Sulphur to get several Ghostly Copies, each the result of that currency on the original, and keep one. The copy count (2-4) depends on the currency, so pick it. Not on corrupted items, with Hinekora's Lock, Vaal or quality currency"}">${ko ? "올플레임 제작" : "Allflame Crafting"}
				<select name="ghost" class="select select-xs select-bordered h-7 min-h-0" aria-label="${ko ? "실체 없는 복제본 수" : "Ghostly Copies"}" data-craft-ghost-count>
					<option value="0" selected="${ghost < 2}">${ko ? "끔" : "Off"}</option>
					@for(int gc = 2; gc <= 4; gc++)
						<option value="${gc}" selected="${ghost == gc}">${ko ? "복제본 " + gc + "개" : gc + " copies"}</option>
					@endfor
				</select>
			</label>
			<%-- 되돌리기 · 새 일반 아이템(10-21 C546 사용자 요청 "화폐랑 되돌리기 · 새 일반 아이템은 기능이 다르니 라인을 바꾸는 등 다른 위치에") — 화폐 칸 밖 이 줄 오른쪽, 화폐 타일과 다른 한 줄 단추. 신성 · 축복 · 머리카락 · 바알은 화폐라 칸에 남김 --%>
			<div class="flex flex-wrap items-center gap-1.5" data-craft-controls data-swap-anchor-key="controls">
				<button type="submit" name="action" value="UNDO" class="btn btn-sm gap-1 btn-outline" data-craft-action="UNDO" disabled="${historySize == 0}" aria-keyshortcuts="Control+Z" title="${ko ? "한 단계 되돌리기(Ctrl+Z) — 여러 단계는 아래 단계 기록의 '여기로'" : "Undo one step (Ctrl+Z) — use 'Go here' in the step history for more"}"><span class="text-base leading-none" aria-hidden="true">↶</span><span>${ko ? "되돌리기" : "Undo"} (${historySize})</span></button>
				<button type="submit" name="action" value="RESET" class="btn btn-sm gap-1 btn-ghost border border-base-300" data-craft-action="RESET" title="${ko ? "새 일반 아이템 — 기록 · 쓴 화폐도 처음부터" : "New normal item — history and spending start over"}"><span class="text-base leading-none" aria-hidden="true">↺</span><span>${ko ? "새 일반 아이템" : "New normal item"}</span></button>
			</div>
		</div>

		<div class="grid grid-cols-4 min-[380px]:grid-cols-5 gap-1.5 sm:flex sm:flex-wrap sm:items-stretch" data-craft-actions data-swap-anchor-key="actions">
			@for(Btn b : btns)
				<button type="submit" name="action" value="${b.action()}" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 ${recommend != null && recommend.actionId() != null && (recommend.actionId().equals(b.action()) || recommend.actionId().startsWith(b.action() + ":") || ("FALLBACK".equals(recommend.actionId()) && "SCOUR".equals(b.action())) || ("UNVEIL".equals(recommend.actionId()) && "UNVEIL_OPEN".equals(b.action()))) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""} ${b.action().equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="${b.action()}" disabled="${"UNVEIL_OPEN".equals(b.action()) ? !b.enabled() : view.blocked() != null ? blocked.containsKey(b.action()) : !b.enabled()}" data-craft-blocked="${blocked.get(b.action())}" title="${blocked.containsKey(b.action()) ? (ko ? b.ko() : b.en()) + " — " + blocked.get(b.action()) : (tip.apply(b.en()) != null ? tip.apply(b.en()) : (ko ? b.ko() : b.en()))}">
					@if(b.icon() != null)<img src="/poe-assets/currency/${b.icon()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">@else<span class="w-7 h-7 grid place-items-center text-lg leading-none" aria-hidden="true">◈</span>@endif
					<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal break-normal hyphens-auto">${ko ? b.ko().replace(" 오브", "").replace("의", "") : b.en().replace("Orb of ", "").replace(" Orb", "").replace("Transmutation", "Transmute").replace("Augmentation", "Augment")}</span>
				</button>
			@endfor
			<%-- 신성 · 축복 · 머리카락도 같은 줄(C455 — CoE 처럼 제작 수단은 위 한 줄). 되돌리기 · 새 아이템은 C546 에서 위 줄로 --%>
			<span class="hidden sm:block w-px self-stretch bg-base-300 mx-1" aria-hidden="true"></span>
			<button type="submit" name="action" value="DIVINE" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 btn-ghost border border-base-300" data-craft-action="DIVINE" disabled="${item.corrupted() || lines.stream().allMatch(PoeCraft.Line::fractured)}" title="${ko ? "신성한 오브 — 옵션 수치를 다시 굴립니다(분열 옵션 제외). 되돌리기로 돌아가지 않습니다" : "Divine Orb — rerolls modifier values (not fractured). Not undoable"}"><img src="/poe-assets/currency/currencymodvalues.png" alt="" class="w-7 h-7 object-contain" loading="lazy"><span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? "신성한 오브" : "Divine Orb"}</span></button>
			<button type="submit" name="action" value="BLESSED" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 btn-ghost border border-base-300" data-craft-action="BLESSED" disabled="${item.corrupted() || implicits.isEmpty()}" title="${ko ? "축복의 오브 — 고정 속성(고정 속성) 수치를 다시 굴립니다. 되돌리기로 돌아가지 않습니다" : "Blessed Orb — rerolls implicit values. Not undoable"}"><img src="/poe-assets/currency/currencyrerollimplicit.png" alt="" class="w-7 h-7 object-contain" loading="lazy"><span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? "축복의 오브" : "Blessed Orb"}</span></button>
			<button type="submit" name="action" value="FORESEE" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 btn-ghost border border-base-300" data-craft-action="FORESEE" disabled="${!foresee.isEmpty()}" title="${ko ? "히네코라의 머리카락 — 다음에 쓸 화폐의 결과를 미리 봅니다. 아이템이 바뀌면 사라집니다" : "Hinekora's Lock — foresee the result of the next currency. Lost once the item is modified"}"><img src="/poe-data/poe2/icons/currency/currencyhinekoraslock.png" alt="" class="w-7 h-7 object-contain" loading="lazy"><span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? "히네코라의 머리카락" : "Hinekora's Lock"}</span></button>
		</div>
		<%-- 최적 조합 따라 하기(C464) — 계산기의 최적 조합 정책이 지금 아이템에 쓸 다음 수단(단추는 테두리로 강조) --%>
		@if(recommend != null)
			<div class="text-xs rounded-box border border-primary/50 bg-primary/10 px-2 py-1" data-craft-recommend="${recommend.actionId()}" role="status">
				@if(recommend.done())
					<span class="font-semibold">${ko ? "최적 조합: 목표를 모두 채웠습니다" : "Optimal plan: all targets met"}</span>
				@elseif(!recommend.known())
					${ko ? "최적 조합: 이 상태는 계산에 없던 상태입니다(목표 · 아이템이 다르면 계산기를 다시)" : "Optimal plan: this state was not part of the plan (re-run the calculator if targets or item changed)"}
				@else
					<span class="font-semibold">${ko ? "최적 조합 다음 수단: " : "Optimal plan next: "}${ko || recommend.actionEn() == null ? recommend.action() : recommend.actionEn()}</span>
					<span class="text-base-content/70"> · ${ko || recommend.stateEn() == null ? recommend.state() : recommend.stateEn()}@if(recommend.remaining() != null) · ${ko ? "남은 기대 " : "cost to go "}${String.format(recommend.remaining() >= 1 ? "%,.1f" : "%,.3f", recommend.remaining())}@endif</span>
				@endif
			</div>
		@endif

		<%-- 화폐 결과 칸(머리카락 미리 보기 · 더럽혀진 · 기폭제 · 얽혀든 · 갈등 · 바알 · 장막 해제)은 단추줄 아래(10-20 사용자 보고 "장막 카오스를 바르면 장막 옵션이 보여야 하는데 안 보여" — 단추줄 위에 있어 누른 단추 자리를 지키는 스크롤에 화면 위로 밀려났다) --%>
		@if(ghosts != null && !ghosts.isEmpty())
			<%-- 올플레임 제작(C523) — 실체 없는 복제본 중 하나 고르기. 무형성(다음 올플레임 제작이 복제본 하나만 보일 확률)은 수치가 공개되지 않아 넣지 않았다 --%>
			!{ String glabel = btns.stream().filter(b -> b.action().equals(ghostAction)).map(b -> ko ? b.ko() : b.en()).findFirst().orElse(ghostAction); }
			<div class="rounded-box border border-warning/60 bg-base-100 p-3 space-y-2" data-craft-ghosts="${ghosts.size()}">
				<div class="text-sm font-medium">${ko ? "올플레임 제작 — 실체 없는 복제본 " + ghosts.size() + "개 중 하나 고르기" : "Allflame Crafting — keep one of " + ghosts.size() + " Ghostly Copies"} <span class="text-xs font-normal text-base-content/70">· ${glabel}</span></div>
				<div class="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
					@for(int gi = 0; gi < ghosts.size(); gi++)
						!{
							PoeCraft.View gv = ghosts.get(gi);
							java.util.List<PoeCraft.Line> gl = gv.lines() == null ? java.util.List.of() : gv.lines();
							java.util.List<String> gadd = gv.added() == null ? java.util.List.of() : gv.added();
							String gr = gv.item().rarity();
						}
						<div class="rounded-box border border-base-300 p-2 text-xs space-y-0.5 min-w-0" data-craft-ghost="${gi}">
							<div class="font-medium">${ko ? "복제본 " : "Copy "}${gi + 1} · ${"RARE".equals(gr) ? (ko ? "레어" : "Rare") : "MAGIC".equals(gr) ? (ko ? "매직" : "Magic") : (ko ? "일반" : "Normal")}</div>
							@for(PoeCraft.Line l : gl)
								!{ java.util.List<String> gx = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en()); }
								<div class="wrap-anywhere ${gadd.contains(l.id()) ? "font-medium text-info" : ""}" data-craft-ghost-line="${l.id()}">${"prefix".equals(l.gen()) ? (ko ? "접두 " : "P ") : (ko ? "접미 " : "S ")}@if(l.tierRank() != null)T${l.tierRank()} @endif${String.join(", ", gx)}</div>
							@endfor
							@if(gl.isEmpty())<div class="text-base-content/70">${ko ? "옵션 없음" : "No modifiers"}</div>@endif
							<button type="submit" name="action" value="GHOST:${ghostSeeds.get(gi)}:${ghostAction}" class="btn btn-xs btn-primary mt-1" data-craft-ghost-pick="${gi}">${ko ? "이걸로" : "Keep this"}</button>
						</div>
					@endfor
				</div>
				<p class="text-xs text-base-content/60">${ko ? "고르면 화폐 하나를 쓴 것으로 기록합니다(망자의 유황은 시세가 없어 비용에 넣지 않음). 수치는 최대 굴림 문구 — 고른 뒤 굴립니다. 무형성(다음 올플레임 제작이 복제본 하나만 보일 확률)은 수치가 공개되지 않아 넣지 않았습니다." : "Keeping one records a single currency use (Dead Man's Sulphur has no price and is not counted). Values show the top roll and are rolled after you keep one. Intangibility (the chance a later Allflame craft shows one copy) is unpublished and not modelled."}</p>
			</div>
		@endif
		@if(!foresee.isEmpty())
			<%-- 히네코라의 머리카락(C360) — 다음 화폐 결과를 미리 본다(적용 안 함). "이대로 적용"은 같은 결과 --%>
			<div class="rounded-box border border-info/60 bg-base-100 p-3 space-y-2" data-craft-foresee>
				<div class="text-sm font-medium">${ko ? "히네코라의 머리카락 — 다음 화폐 결과 미리 보기" : "Hinekora's Lock — foreseeing the next currency"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "아이템이 바뀌면 사라집니다" : "lost once the item is modified"}</span></div>
				@if(preview != null)
					!{
						java.util.List<PoeCraft.Line> pl = preview.lines() == null ? java.util.List.of() : preview.lines();
						java.util.List<String> padd = preview.added() == null ? java.util.List.of() : preview.added();
						String pr = preview.item().rarity();
						// 더럽혀진 화폐는 기본 버튼 목록에 없다(C369 검토 — 원래 키가 보였다)
						java.util.Map<String, String[]> tName = java.util.Map.of("TAINTED_CHAOS", new String[] {"더럽혀진 카오스 오브", "Tainted Chaos Orb"}, "TAINTED_EXALT", new String[] {"더럽혀진 엑잘티드 오브", "Tainted Exalted Orb"}, "TAINTED_DIVINE", new String[] {"더럽혀진 신성한 눈물방울", "Tainted Divine Teardrop"});
						String plabel = btns.stream().filter(b -> b.action().equals(previewAction)).map(b -> ko ? b.ko() : b.en()).findFirst().orElse(tName.containsKey(previewAction) ? tName.get(previewAction)[ko ? 0 : 1] : previewAction);
						java.util.List<PoeCraft.Line> pimp = preview.implicits() == null ? java.util.List.of() : preview.implicits();
						String pn = preview.note() == null ? "" : preview.note();
						// 결과 종류(C369 검토 — 바알 오브 미리 보기가 넷을 구분하지 못했다)
						String pnote = switch (pn) {
							case "vaal:none", "vaal:none:sockets", "vaal:none:reforge", "vaal:none:implicit" -> ko ? "바알 결과: 변화 없음(타락만)" : "Vaal outcome: no change (corrupted)";
							case "vaal:sockets" -> ko ? "바알 결과: 소켓 · 연결 · 색 다시(옵션 그대로)" : "Vaal outcome: sockets rerolled (modifiers unchanged)";
							case "vaal:reforge" -> ko ? "바알 결과: 레어로 다시 굴림" : "Vaal outcome: reforged as rare";
							case "vaal:implicit" -> ko ? "바알 결과: 고정 속성이 부패 고정 속성으로" : "Vaal outcome: corrupted implicit";
							case "tainted:reforge" -> ko ? "결과: 새 옵션으로 다시 굴림" : "Outcome: reforged";
							case "tainted:clear" -> ko ? "결과: 옵션을 전부 지움" : "Outcome: all modifiers removed";
							case "tainted:add" -> ko ? "결과: 옵션 하나를 붙임" : "Outcome: added a modifier";
							case "tainted:remove" -> ko ? "결과: 옵션 하나를 지움" : "Outcome: removed a modifier";
							default -> pn.startsWith("tainted:tier:") ? (ko ? "결과: 등급 올림 " : "Outcome: tiers raised ") + pn.split(":")[2] + (ko ? " · 내림 " : ", lowered ") + pn.split(":")[3]
								// C402 검토 — 얽혀든 화석 · 갈등의 오브 미리 보기에도 결과(태그 · 진영)
								: pn.startsWith("tangled:") ? (ko ? "얽혀든 화석: 더 " : "Tangled: more ") + pn.split(":")[1] + (ko ? " · 없음 " : ", no ") + pn.split(":")[2]
								: pn.startsWith("conflict:") ? (ko ? "갈등의 오브: " : "Conflict: ") + ("exarch".equals(pn.split(":")[1]) ? (ko ? "총주교 올림" : "Exarch raised") : (ko ? "포식자 올림" : "Eater raised")) + (pn.endsWith(":removed") ? (ko ? " · 다른 쪽 지워짐" : ", other removed") : "")
								: null;
						};
					}
					<div class="text-xs space-y-0.5" data-craft-preview="${previewAction}">
						<div class="font-medium">${ko ? "미리 본 화폐: " : "Foreseen: "}${plabel} → ${"RARE".equals(pr) ? (ko ? "레어" : "Rare") : "MAGIC".equals(pr) ? (ko ? "매직" : "Magic") : (ko ? "일반" : "Normal")}@if(preview.item().corrupted()) · ${ko ? "타락" : "Corrupted"}@endif</div>
						@if(pnote != null)<div data-craft-preview-note="${pn}">${pnote}</div>@endif
						@for(PoeCraft.Line l : pimp)
							!{ java.util.List<String> ix = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en()); }
							<div class="text-base-content/70" data-craft-preview-implicit="${l.id()}">${ko ? "고정 속성" : "Implicit"}(${ko ? l.nameKo() : l.name()}) ${String.join(", ", ix)}</div>
						@endfor
						@for(PoeCraft.Line l : pl)
							!{ java.util.List<String> px = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en()); }
							<div class="${padd.contains(l.id()) ? "font-medium text-info" : ""}" data-craft-preview-line="${l.id()}">${"prefix".equals(l.gen()) ? (ko ? "접두 " : "P ") : (ko ? "접미 " : "S ")}${String.join(", ", px)}</div>
						@endfor
						@if(pl.isEmpty())<div class="text-base-content/70">${ko ? "옵션 없음" : "No modifiers"}</div>@endif
					</div>
					<div class="flex flex-wrap gap-2">
						<button type="submit" name="action" value="COMMIT:${previewAction}" class="btn btn-sm btn-primary" data-craft-action="COMMIT">${ko ? "이대로 적용" : "Apply this result"}</button>
						<button type="submit" name="action" value="FORESEE_OFF" class="btn btn-sm btn-ghost border border-base-300" data-craft-action="FORESEE_OFF">${ko ? "그만 보기" : "Stop foreseeing"}</button>
					</div>
				@else
					<p class="text-xs text-base-content/70">${ko ? "화폐를 누르면 결과를 미리 보여 줍니다(적용하지 않음). 같은 화폐는 몇 번을 눌러도 같은 결과입니다." : "Click a currency to preview its result without applying it. The same currency always shows the same result."}</p>
					<button type="submit" name="action" value="FORESEE_OFF" class="btn btn-sm btn-ghost border border-base-300" data-craft-action="FORESEE_OFF">${ko ? "그만 보기" : "Stop foreseeing"}</button>
				@endif
			</div>
		@endif
		@if(error == null && view.note() != null && view.note().startsWith("tainted:"))
			!{
				String tn = view.note();
				String taintMsg;
				if (tn.startsWith("tainted:tier:")) {
					String[] ud = tn.substring("tainted:tier:".length()).split(":");
					taintMsg = ko ? "등급 올림 " + ud[0] + " · 내림 " + ud[1] + "(사다리 끝 · 분열 · 작업대 옵션은 그대로)" : "Tiers raised " + ud[0] + ", lowered " + ud[1] + " (ends of the ladder, fractured and crafted modifiers unchanged)";
				} else {
					taintMsg = switch (tn) {
						case "tainted:reforge" -> ko ? "새 옵션으로 다시 굴림" : "Reforged with new random modifiers";
						case "tainted:clear" -> ko ? "옵션을 전부 지움(분열 · 변경 불가 쪽은 남음)" : "All modifiers removed (fractured and locked stay)";
						case "tainted:add" -> ko ? "옵션 하나를 붙임" : "Added a modifier";
						case "tainted:remove" -> ko ? "옵션 하나를 지움" : "Removed a modifier";
						default -> tn;
					};
				}
			}
			<div role="status" class="alert py-2 text-sm" data-craft-tainted="${tn}">
				<span><span class="font-medium">${ko ? "더럽혀진 화폐 결과: " : "Tainted currency outcome: "}</span>${taintMsg}<br><span class="text-xs text-base-content/70">${ko ? "둘 중 어느 쪽인지는 확률이 공개되지 않아 반반으로 굴립니다" : "Which of the two happens has unpublished odds and is rolled 50/50"}</span></span>
			</div>
		@endif
		@if(error == null && view.note() != null && view.note().startsWith("catalyst:"))
			!{
				String[] cn = view.note().split(":");
				PoeCraft.CurrencyInfo cType = null;
				PoeCraft.CurrencyInfo cUsed = null;
				boolean cTainted = la.startsWith("CATALYST:vaal");
				for (PoeCraft.CurrencyInfo c : catalysts) {
					if (cn.length > 1 && c.catalyst().equals(cn[1])) cType = c;
					if (c.catalyst().equals(cTainted ? "vaal" : cn.length > 1 ? cn[1] : "")) cUsed = c;
				}
				String cLabel = cType == null ? "" : ko ? cType.qualityLabelKo() : cType.qualityLabel();
				String cName = cUsed == null ? "" : ko ? cUsed.nameKo() : cUsed.name();
			}
			<div role="status" class="alert py-2 text-sm" data-craft-catalyst-note="${view.note()}">
				<span><span class="font-medium">${cName}${cn.length > 3 && !cTainted ? " × " + cn[3] : ""}: </span>${ko ? "퀄리티 (" + cLabel + ") " + (cn.length > 2 ? cn[2] : "") + "%" : "Quality (" + cLabel + ") " + (cn.length > 2 ? cn[2] : "") + "%"}@if(cTainted)<br><span class="text-xs text-base-content/70">${ko ? "종류 · 양(1~20%)은 확률이 공개되지 않아 균등으로 굴립니다" : "Type and amount (1–20%) have unpublished odds and are rolled evenly"}</span>@endif</span>
			</div>
		@endif
		@if(error == null && view.note() != null && view.note().startsWith("tangled:"))
			<%-- 얽혀든 화석(C400) — 무작위로 고른 태그 둘(가정: 단순 화석이 쓰는 태그, 더 = ×10 · 막음 = ×0) --%>
			!{
				String[] tgn = view.note().split(":");
				java.util.Map<String, String> tagKo = java.util.Map.ofEntries(java.util.Map.entry("attack", "공격"), java.util.Map.entry("attribute", "능력치"), java.util.Map.entry("aura", "오라"), java.util.Map.entry("bleed", "출혈"), java.util.Map.entry("caster", "시전"), java.util.Map.entry("chaos", "카오스"), java.util.Map.entry("cold", "냉기"), java.util.Map.entry("critical", "치명타"), java.util.Map.entry("curse", "저주"), java.util.Map.entry("defences", "방어"), java.util.Map.entry("elemental", "원소"), java.util.Map.entry("fire", "화염"), java.util.Map.entry("gem", "젬"), java.util.Map.entry("life", "생명력"), java.util.Map.entry("lightning", "번개"), java.util.Map.entry("mana", "마나"), java.util.Map.entry("minion", "소환수"), java.util.Map.entry("physical", "물리"), java.util.Map.entry("poison", "중독"), java.util.Map.entry("speed", "속도"));
				String tMore = tgn.length > 1 ? (ko ? tagKo.getOrDefault(tgn[1], tgn[1]) : tgn[1]) : "";
				String tNo = tgn.length > 2 ? (ko ? tagKo.getOrDefault(tgn[2], tgn[2]) : tgn[2]) : "";
			}
			<div role="status" class="alert py-2 text-sm" data-craft-tangled-note="${view.note()}">
				<span><span class="font-medium">${ko ? "얽혀든 화석: " : "Tangled Fossil: "}</span>${ko ? tMore + " 속성 훨씬 더 · " + tNo + " 속성 없음" : "more " + tMore + " modifiers, no " + tNo + " modifiers"}<br><span class="text-xs text-base-content/70">${ko ? "어느 종류인지는 공개되지 않아 다른 화석이 쓰는 태그 중 둘을 균등으로 고릅니다(더 = ×10 · 없음 = ×0, 부패 에센스 옵션 10%)" : "Which types is unpublished — two tags used by other fossils are picked evenly (more = ×10, none = ×0; corrupted essence modifier 10%)"}</span></span>
			</div>
		@endif
		@if(error == null && view.note() != null && view.note().startsWith("conflict:"))
			!{
				String[] cfn = view.note().split(":");
				java.util.function.Function<String, String> side = g -> "exarch".equals(g) ? (ko ? "작열의 총주교" : "Searing Exarch") : (ko ? "세계 포식자" : "Eater of Worlds");
			}
			<div role="status" class="alert py-2 text-sm" data-craft-conflict-note="${view.note()}">
				<span><span class="font-medium">${ko ? "갈등의 오브: " : "Orb of Conflict: "}</span>${side.apply(cfn[1])}${ko ? " 올림 · " : " raised, "}${side.apply(cfn[2])}${cfn.length > 3 ? (ko ? " 내려서 지워짐" : " lowered and removed") : (ko ? " 내림" : " lowered")}</span>
			</div>
		@endif
		@if(error == null && view.note() != null && view.note().startsWith("vaal:"))
			!{
				String vn = view.note();
				String vaalMsg = switch (vn) {
					case "vaal:sockets" -> ko ? "소켓 · 연결 · 색을 다시 굴림 — 옵션은 그대로(소켓은 이 에뮬레이터가 다루지 않습니다)" : "Sockets, links and colours rerolled — modifiers unchanged (sockets are not modelled here)";
					case "vaal:none:sockets" -> ko ? "소켓 다시 굴림 — 홈이 없는 아이템이라 변화 없음" : "Socket reroll — no sockets on this item, so no change";
					case "vaal:reforge" -> ko ? "레어로 다시 굴림 — 고정 속성은 그대로, 분열 · 변경 불가 쪽 옵션은 남음" : "Reforged as rare — implicits kept, fractured and locked modifiers stay";
					case "vaal:none:reforge" -> ko ? "레어로 다시 굴림 — 붙일 옵션이 없어 변화 없음" : "Reforge as rare — nothing could roll, so no change";
					case "vaal:implicit" -> ko ? "고정 속성이 부패 고정 속성 하나로 바뀜(섬뜩한 고정 속성 포함)" : "Implicits replaced with one corrupted implicit (eldritch ones too)";
					case "vaal:none:implicit" -> ko ? "고정 속성 변경 — 이 아이템에 붙을 부패 고정 속성이 없어 변화 없음" : "Implicit change — no corrupted implicit for this item, so no change";
					default -> ko ? "변화 없음 — 타락만" : "No change — just corrupted";
				};
			}
			<div role="status" class="alert py-2 text-sm" data-craft-vaal="${vn}">
				<span><span class="font-medium">${ko ? "바알 오브 결과: " : "Vaal Orb outcome: "}</span>${vaalMsg}<br><span class="text-xs text-base-content/70">${ko ? "결과 넷(변화 없음 · 소켓 · 레어 재구성 · 고정 속성)의 확률은 공개되지 않아 균등으로 굴립니다" : "The four outcomes (none, sockets, reforge, implicit) have unpublished odds and are rolled evenly"}</span></span>
			</div>
		@endif

		@if(!unveil.isEmpty() && "UNVEIL_OPEN".equals(lastAction))
			<%-- 장막 해제(C341) — 제시 셋은 아이템 상태로 정해져 다시 열어도 같다. 고르면 그 옵션으로 바뀐다. '장막 해제' 단추를 눌렀을 때만(C475) --%>
			<div class="rounded-box border border-base-300 bg-base-100 p-3 space-y-2" data-craft-unveil>
				<div class="text-sm font-medium">${ko ? "장막 해제 — 셋 중 하나를 고르세요" : "Unveil — choose one of three"}</div>
				<div class="grid gap-2">
					@for(PoeCraft.Line u : unveil)
						!{ java.util.List<String> ux = ko && !nz.apply(u.ko()).isEmpty() ? u.ko() : nz.apply(u.en()); java.util.List<String> un = ko && !nz.apply(u.koMin()).isEmpty() ? u.koMin() : nz.apply(u.enMin()); }
						<button type="submit" name="action" value="UNVEIL:${u.id()}" class="btn btn-sm h-auto min-h-12 py-2 btn-ghost border border-base-300 text-left justify-start font-normal" data-craft-action="UNVEIL:${u.id()}" title="${ko && u.nameKo() != null ? u.nameKo() : u.name()}">
							<span class="text-[10px] font-mono text-base-content/60 shrink-0">${"prefix".equals(u.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}</span>
							<span class="text-xs leading-snug whitespace-normal">
								@for(int i = 0; i < ux.size(); i++)
									$unsafe{PoeText.highlightValues(PoeModRange.range(i < un.size() ? un.get(i) : null, ux.get(i)))}@if(i < ux.size() - 1)<br>@endif
								@endfor
							</span>
						</button>
					@endfor
				</div>
			</div>
		@endif


		<%-- 접는 칸은 일반 details(게이트 .collapse 는 체크박스 방식만 지원 — details[open] 이어도 내용이 visibility:hidden 이라 확률 표가 안 보였다, 10-15) --%>
		<%-- 섞어 쓰는 제작 수단(10-15 C338 — 사용자 요청 "다양한 제작 컨텐츠들이 엮여 있고, 섞어서 사용이 가능하고 그에 따른 제약도 있다").
		     묶음마다 접는 칸 — 에센스 · 화석과 공명기 · 수확 · 작업대 · 정복자. 쓸 수 없는 상태면 버튼이 꺼지고, 서버가 이유를 알려 준다 --%>
		<%-- 둘째 줄(C455): 섞어 쓰는 제작 수단 — 누르면 이 줄 바로 아래에 그 묶음을 펼친다(C476: 한 번에 하나 — data-toggle-exclusive, 닫힌 묶음은 자리를 차지하지 않음) --%>
		<%-- 최적 조합 다음 수단이 접힌 칸 안이면 그 칸 단추도 테두리(C464b) --%>
		!{ String recId = recommend == null || recommend.actionId() == null ? "" : recommend.actionId(); String recGroup = recId.startsWith("ESSENCE") ? "essence" : recId.startsWith("FOSSIL") ? "fossil" : recId.startsWith("HARVEST") ? "harvest" : recId.startsWith("BENCH") ? "bench" : recId.startsWith("CONQUEROR") ? "influence" : (recId.startsWith("EMBER") || recId.startsWith("ICHOR") || recId.startsWith("ELDRITCH")) ? "eldritch" : (recId.startsWith("BEAST") || recId.startsWith("IMPRINT")) ? "beast" : ""; }
		<%-- 묶음 켜고 끄기(C476 — 사용자 요청 "아이템 상태에 따라 사용 가능한 제작 방식과 세부 제작 옵션이 활성/비활성"): 묶음 안 옵션이 모두 엔진에
		     막히면(서버가 옵션마다 시험 적용한 이유) 그 묶음 단추도 끈다. 이유는 마우스를 올리면 --%>
		!{
			java.util.function.Function<java.util.List<String>, String> allWhy = ks -> { String w = null; for (String k : ks) { String r = blocked.get(k); if (r == null) return null; if (w == null) w = r; } return w; };
			String essWhy = allWhy.apply(essences.stream().map(e -> "ESSENCE:" + e.slug()).toList());
			String fosWhy = options == null ? null : allWhy.apply(options.fossils().stream().map(f -> "FOSSIL:" + f.slug()).toList());
			String harWhy = options == null ? null : allWhy.apply(options.harvest().stream().map(h -> "HARVEST:" + h.id()).toList());
			java.util.List<String> benchKeys = new java.util.ArrayList<>();
			if (options != null) { options.bench().forEach(b -> benchKeys.add("BENCH:" + b.modId())); }
			benchKeys.add("BENCH_REMOVE");
			String benWhy = allWhy.apply(benchKeys);
			java.util.List<String> infKeys = new java.util.ArrayList<>(java.util.List.of("DOMINANCE"));
			if (options != null) { options.conquerors().forEach(c -> { infKeys.add("CONQUEROR:" + c); infKeys.add("AWAKENER:" + c); }); }
			String infWhy = allWhy.apply(infKeys);
			String eldWhy = allWhy.apply(java.util.List.of("EMBER", "ICHOR", "ELDRITCH_CHAOS", "ELDRITCH_EXALT", "ELDRITCH_ANNUL", "CONFLICT"));
			String taiWhy = allWhy.apply(java.util.List.of("TAINTED_CHAOS", "TAINTED_EXALT", "TAINTED_DIVINE"));
			java.util.List<String> beastKeys = new java.util.ArrayList<>(java.util.List.of("BEAST:swap-prefix", "BEAST:swap-suffix", "BEAST:aspect:cat", "BEAST:aspect:avian", "BEAST:aspect:spider", "BEAST:aspect:crab"));
			if (!inf.isEmpty()) { for (String one : inf.split(",")) beastKeys.add("BEAST:influence:" + one.trim()); }
			String beaWhy = magic || !imprintJson.isEmpty() ? null : allWhy.apply(beastKeys);
			java.util.Map<String, String> groupWhy = new java.util.HashMap<>();
			groupWhy.put("essence", essWhy); groupWhy.put("fossil", fosWhy); groupWhy.put("harvest", harWhy); groupWhy.put("bench", benWhy);
			groupWhy.put("influence", infWhy); groupWhy.put("eldritch", eldWhy); groupWhy.put("tainted", taiWhy); groupWhy.put("beast", beaWhy);
		}
		<%-- 목표까지 연타(C506, craftofexile 시뮬레이터의 '될 때까지') — 아래 계산기 목표(☆)를 모두 채우거나 최대 횟수까지 같은 화폐, 한 단계 · 쓴 화폐 N개로 기록 --%>
		<div class="flex flex-wrap items-center gap-1.5 text-xs" data-craft-spam>
			<span class="text-base-content/70">${ko ? "목표까지 연타" : "Repeat until targets"}</span>
			<select name="spamAct" class="select select-xs select-bordered h-7 min-h-0" aria-label="${ko ? "연타할 화폐" : "Currency to repeat"}" data-craft-spam-act>
				<option value="CHAOS">${ko ? "카오스 오브" : "Chaos Orb"}</option>
				<option value="ALTERATION">${ko ? "변화의 오브(매직)" : "Orb of Alteration (magic)"}</option>
				<option value="ESSENCE">${ko ? "에센스(고른 것)" : "Essence (picked)"}</option>
				<option value="FOSSIL">${ko ? "화석(고른 조합)" : "Fossils (picked)"}</option>
				<option value="HARVEST">${ko ? "수확(고른 것)" : "Harvest (picked)"}</option>
				<option value="VEILED_CHAOS">${ko ? "장막의 카오스 오브" : "Veiled Chaos Orb"}</option>
				<option value="EXALT">${ko ? "엑잘티드 오브" : "Exalted Orb"}</option>
			</select>
			<label class="flex items-center gap-1">${ko ? "최대" : "max"}<input type="number" name="spamMax" min="1" max="5000" value="1000" class="input input-xs input-bordered w-20 h-7 min-h-0" aria-label="${ko ? "최대 횟수" : "Maximum uses"}"></label>
			<button type="button" name="action" value="SPAM" class="btn btn-xs btn-ghost border border-base-300" hx-post="/poe/htmx/craft" hx-target="#craft-emu" hx-swap="outerHTML" hx-include="#craft-emu > form, [data-craft-calc-form] [name^='t_']" data-craft-spam-run title="${ko ? "아래 계산기에서 고른 목표를 모두 채울 때까지(또는 최대 횟수까지) 같은 화폐를 씁니다 — 쓴 화폐는 한 단계에 개수로" : "Uses the same currency until all calculator targets are met (or the limit) — recorded as one step with the count"}">${ko ? "연타" : "Repeat"}</button>
			<span class="text-base-content/60" data-craft-spam-expect aria-live="polite"></span>
			@if(spamNote != null)<span class="text-success" data-craft-spam-note>${spamNote}</span>@endif
		</div>
		<div class="flex flex-wrap items-center gap-1.5" data-craft-means data-toggle-exclusive role="group" aria-label="${ko ? "다른 제작 수단" : "Other crafting methods"}">
			@if((!essences.isEmpty()))
			<button type="button" class="btn btn-sm ${"essence".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="essence"]' aria-expanded="false" data-craft-means-open="essence" disabled="${groupWhy.get("essence") != null}" data-craft-means-blocked="${groupWhy.get("essence")}" title="${groupWhy.get("essence")}">${ko ? "에센스" : "Essences"}</button>
			@endif
			@if((options != null && !options.fossils().isEmpty()))
			<button type="button" class="btn btn-sm ${"fossil".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="fossil"]' aria-expanded="false" data-craft-means-open="fossil" disabled="${groupWhy.get("fossil") != null}" data-craft-means-blocked="${groupWhy.get("fossil")}" title="${groupWhy.get("fossil")}">${ko ? "화석 · 공명기" : "Fossils"}</button>
			@endif
			@if((options != null && !options.harvest().isEmpty()))
			<button type="button" class="btn btn-sm ${"harvest".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="harvest"]' aria-expanded="false" data-craft-means-open="harvest" disabled="${groupWhy.get("harvest") != null}" data-craft-means-blocked="${groupWhy.get("harvest")}" title="${groupWhy.get("harvest")}">${ko ? "수확" : "Harvest"}</button>
			@endif
			@if((options != null && !options.bench().isEmpty()))
			<button type="button" class="btn btn-sm ${"bench".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="bench"]' aria-expanded="false" data-craft-means-open="bench" disabled="${groupWhy.get("bench") != null}" data-craft-means-blocked="${groupWhy.get("bench")}" title="${groupWhy.get("bench")}">${ko ? "작업대" : "Bench"}</button>
			@endif
			@if((options != null && !options.conquerors().isEmpty()))
			<button type="button" class="btn btn-sm ${"influence".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="influence"]' aria-expanded="false" data-craft-means-open="influence" disabled="${groupWhy.get("influence") != null}" data-craft-means-blocked="${groupWhy.get("influence")}" title="${groupWhy.get("influence")}">${ko ? "영향력 엑잘" : "Influence exalts"}</button>
			@endif
			@if((eldritchClass))
			<button type="button" class="btn btn-sm ${"eldritch".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="eldritch"]' aria-expanded="false" data-craft-means-open="eldritch" disabled="${groupWhy.get("eldritch") != null}" data-craft-means-blocked="${groupWhy.get("eldritch")}" title="${groupWhy.get("eldritch")}">${ko ? "섬뜩한" : "Eldritch"}</button>
			@endif
			@if((jewellery && !catalysts.isEmpty()))
			<button type="button" class="btn btn-sm ${"catalyst".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="catalyst"]' aria-expanded="false" data-craft-means-open="catalyst">${ko ? "기폭제" : "Catalysts"}</button>
			@endif
			<button type="button" class="btn btn-sm ${"ducat".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="ducat"]' aria-expanded="false" data-craft-means-open="ducat">${ko ? "두카트" : "Ducats"}</button>
			<button type="button" class="btn btn-sm ${"tainted".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="tainted"]' aria-expanded="false" data-craft-means-open="tainted" disabled="${groupWhy.get("tainted") != null}" data-craft-means-blocked="${groupWhy.get("tainted")}" title="${groupWhy.get("tainted")}">${ko ? "더럽혀진 화폐" : "Tainted"}</button>
			<button type="button" class="btn btn-sm ${"beast".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="beast"]' aria-expanded="false" data-craft-means-open="beast" disabled="${groupWhy.get("beast") != null}" data-craft-means-blocked="${groupWhy.get("beast")}" title="${groupWhy.get("beast")}">${ko ? "야수 제작 · 각인" : "Beastcrafting"}</button>
			<button type="button" class="btn btn-sm ${"manual".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft-emu details[data-craft-group="manual"]' aria-expanded="false" data-craft-means-open="manual">${ko ? "직접 편집" : "Edit directly"}</button>
		</div>
		<div class="space-y-2 [&>details:not([open])]:hidden" data-craft-groups>
			@if(!essences.isEmpty())
				<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="essence" data-swap-anchor-key="essence" open="${"essence".equals(openGroup)}">
					<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "에센스" : "Essences"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "보장 옵션 + 나머지는 카오스처럼" : "guaranteed modifier, rest like chaos"}</span></summary>
					<div class="px-3 pb-3 space-y-2" data-craft-essence>
						<%-- 종류 아이콘 · 등급 단추로 고르기(10-21 C547) — 조각(craftEmu 생성 메서드 64KB 한도) --%>
						@template.poe.htmx.craftEssencePicker(essences = essences, blocked = blocked, selectLabel = ko ? "에센스" : "Essence")
						<button type="submit" name="action" value="ESSENCE" class="btn btn-sm" data-craft-action="ESSENCE" disabled="${magic || item.corrupted() || outcomeMeta || essWhy != null}" title="${outcomeMeta ? (ko ? "속성의 결과에 영향을 미치는 속성 부여가 포함된 아이템에는 해당 화폐를 사용할 수 없습니다." : "You can't use this currency on items with a modifier that affects mod outcomes.") : null}">${ko ? "에센스 사용" : "Use essence"}</button>
					</div>
				</details>
			@endif

			@if(options != null && !options.fossils().isEmpty())
				!{
					String resoKind = normal ? "alchemical" : "chaotic";
					java.util.List<PoeCraft.Resonator> resos = options.resonators().stream().filter(r -> resoKind.equals(r.kind())).toList();
				}
				<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="fossil" data-swap-anchor-key="fossil" open="${"fossil".equals(openGroup)}">
					<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "화석 · 공명기" : "Fossils · Resonators"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "1~4개, 옵션 태그 배수를 곱함 · 일반 = 연금술 공명기, 레어 = 혼돈 공명기" : "1–4, tag weight multipliers multiply · normal = alchemical, rare = chaotic"}</span></summary>
					<div class="px-3 pb-3 space-y-2">
						<div class="grid grid-cols-1 sm:grid-cols-2 gap-1">
							@for(PoeCraft.Fossil f : options.fossils())
								!{
									String effect = String.join(", ", ko && f.descKo() != null && !f.descKo().isEmpty() ? f.descKo() : (f.desc() == null ? java.util.List.of() : f.desc()));
								}
								<label class="flex items-start gap-2 text-xs min-w-0 ${!blocked.containsKey("FOSSIL:" + f.slug()) ? "" : "opacity-50"}" data-craft-fossil="${f.slug()}" title="${blocked.get("FOSSIL:" + f.slug())}">
									<input type="checkbox" name="fossil" value="${f.slug()}" class="checkbox checkbox-xs mt-0.5" disabled="${magic || blocked.containsKey("FOSSIL:" + f.slug())}">
									<img src="/poe-assets/${f.icon()}" alt="" class="w-5 h-5 object-contain shrink-0" loading="lazy">
									<span class="min-w-0 wrap-anywhere"><span class="font-medium">${ko ? f.nameKo() : f.name()}</span> <span class="text-base-content/60">${effect}</span></span>
								</label>
							@endfor
						</div>
						<div class="flex flex-wrap items-center gap-2">
							@for(PoeCraft.Resonator r : resos)
								<img src="/poe-assets/${r.icon()}" alt="${ko ? r.nameKo() : r.name()}" title="${ko ? r.nameKo() : r.name()} (${r.sockets()}${ko ? "칸" : " sockets"})" class="w-6 h-6 object-contain" loading="lazy">
							@endfor
							<button type="submit" name="action" value="FOSSIL" class="btn btn-sm" data-craft-action="FOSSIL" disabled="${magic || item.corrupted() || outcomeMeta || fosWhy != null}" title="${outcomeMeta ? (ko ? "속성의 결과에 영향을 미치는 속성 부여가 포함된 아이템에는 해당 화폐를 사용할 수 없습니다." : "You can't use this currency on items with a modifier that affects mod outcomes.") : null}">${ko ? (normal ? "화석 적용 · 연금 공명기" : "화석 적용 · 혼돈 공명기") : (normal ? "Apply fossils (alchemical)" : "Apply fossils (chaotic)")}</button>
						</div>
					</div>
					<p class="px-3 pb-3 text-xs text-base-content/70" data-craft-fossil-hint>${ko ? "화석을 고르고 '화석' 단추를 누르면(고른 상태) 옵션 표가 이 조합 기준 확률 · 배수로 바뀝니다." : "Pick fossils and click Fossil (armed) — the modifier table switches to this combination's odds and multipliers."}</p>
				</details>
			@endif

			@if(options != null && !options.harvest().isEmpty())
				!{
					java.util.Map<String, String[]> hGroups = new java.util.LinkedHashMap<>();
					hGroups.put("reroll_with_mod", new String[] {"태그 재제작(그 태그 옵션 하나 보장)", "Reforge with a tag"});
					hGroups.put("reroll_with_current_tags_affinity_multiplier", new String[] {"같은 유형 더/덜", "More / less of the same types"});
					hGroups.put("remove_type_and_add_type_mod", new String[] {"태그 옵션 추가 + 무작위 제거", "Add a tag modifier, remove a random one"});
					hGroups.put("convert_mod", new String[] {"저항 변환", "Resistance conversion"});
					hGroups.put("reroll_with_influence_mod", new String[] {"영향력 재제작", "Reforge with an influence modifier"});
					hGroups.put("reroll_influence_types", new String[] {"영향력 유형 바꾸기 + 재련", "Randomise influence types"});
				}
				<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="harvest" data-swap-anchor-key="harvest" open="${"harvest".equals(openGroup)}">
					<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "수확" : "Harvest"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "레어 재제작 · 변환 · 추가/제거" : "reforge, convert, add/remove"}</span></summary>
					<div class="px-3 pb-3 flex flex-wrap items-center gap-2">
						<select name="harvest" class="select select-sm select-bordered min-w-0 flex-1" aria-label="${ko ? "수확 제작" : "Harvest craft"}">
							@for(var hg : hGroups.entrySet())
								<optgroup label="${ko ? hg.getValue()[0] : hg.getValue()[1]}">
									@for(PoeCraft.Harvest h : options.harvest())
										@if(hg.getKey().equals(h.command()))
											<option value="${h.id()}" disabled="${blocked.containsKey("HARVEST:" + h.id())}">${ko && h.textKo() != null ? h.textKo() : h.text()}@if(blocked.containsKey("HARVEST:" + h.id())) (${blocked.get("HARVEST:" + h.id())})@endif</option>
										@endif
									@endfor
								</optgroup>
							@endfor
						</select>
						<button type="submit" name="action" value="HARVEST" class="btn btn-sm" data-craft-action="HARVEST" disabled="${(normal && inf.isEmpty()) || item.corrupted() || harWhy != null}" title="${harWhy}">${ko ? "수확 사용" : "Use harvest"}</button>
					</div>
				</details>
			@endif

			@if(options != null && !options.bench().isEmpty())
				!{
					java.util.List<PoeCraft.BenchCraft> metaCrafts = options.bench().stream().filter(b -> b.modId() != null && b.modId().contains("MasterItemGeneration")).toList();
					java.util.List<PoeCraft.BenchCraft> normalCrafts = options.bench().stream().filter(b -> b.modId() == null || !b.modId().contains("MasterItemGeneration")).toList();
				}
				<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="bench" data-swap-anchor-key="bench" open="${"bench".equals(openGroup)}">
					<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "작업대" : "Crafting bench"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "작업대 옵션 하나(메타 제작 \"제작 옵션 3개\"면 셋) · 메타 제작" : "one crafted modifier (three with the multimod) · meta-crafts"}</span></summary>
					<div class="px-3 pb-3 flex flex-wrap items-center gap-2">
						<select name="bench" class="select select-sm select-bordered min-w-0 flex-1" aria-label="${ko ? "작업대 옵션" : "Bench craft"}">
							<optgroup label="${ko ? "메타 제작" : "Meta-crafts"}">
								@for(PoeCraft.BenchCraft b : metaCrafts)
									<option value="${b.modId()}" disabled="${blocked.containsKey("BENCH:" + b.modId())}">${String.join(", ", ko && b.ko() != null && !b.ko().isEmpty() ? b.ko() : b.en())} (${"prefix".equals(b.gen()) ? (ko ? "접두" : "prefix") : (ko ? "접미" : "suffix")})@if(blocked.containsKey("BENCH:" + b.modId())) (${blocked.get("BENCH:" + b.modId())})@endif</option>
								@endfor
							</optgroup>
							@for(String gen : java.util.List.of("prefix", "suffix"))
								<optgroup label="${"prefix".equals(gen) ? (ko ? "접두" : "Prefixes") : (ko ? "접미" : "Suffixes")}">
									@for(PoeCraft.BenchCraft b : normalCrafts)
										@if(gen.equals(b.gen()))
											!{
												java.util.List<String> mx = ko && b.ko() != null && !b.ko().isEmpty() ? b.ko() : b.en();
												java.util.List<String> mn = ko && b.koMin() != null && !b.koMin().isEmpty() ? b.koMin() : b.enMin();
												java.util.List<String> parts = new java.util.ArrayList<>();
												for (int i = 0; i < mx.size(); i++) parts.add(PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)));
											}
											<option value="${b.modId()}" disabled="${blocked.containsKey("BENCH:" + b.modId())}">${String.join(", ", parts)}@if(blocked.containsKey("BENCH:" + b.modId())) (${blocked.get("BENCH:" + b.modId())})@endif</option>
										@endif
									@endfor
								</optgroup>
							@endfor
						</select>
						<button type="submit" name="action" value="BENCH" class="btn btn-sm" data-craft-action="BENCH" disabled="${item.corrupted() || options.bench().stream().allMatch(b -> blocked.containsKey("BENCH:" + b.modId()))}">${ko ? "제작" : "Craft"}</button>
						<button type="submit" name="action" value="BENCH_REMOVE" class="btn btn-sm btn-ghost" data-craft-action="BENCH_REMOVE" disabled="${craftedCount == 0}" title="${blocked.get("BENCH_REMOVE")}">${ko ? "작업대 옵션 제거" : "Remove crafted mods"}</button>
					</div>
				</details>
			@endif

			@if(options != null && !options.conquerors().isEmpty())
				<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="influence" data-swap-anchor-key="influence" open="${"influence".equals(openGroup)}">
					<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "영향력 엑잘(정복자 · 쉐이퍼 · 엘더)" : "Influence exalts (conquerors, Shaper, Elder)"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "영향력 없는 레어에 영향력 + 전용 옵션 하나" : "influence + one influenced modifier on an uninfluenced rare"}</span></summary>
					<div class="px-3 pb-3 grid grid-cols-2 sm:grid-cols-4 gap-2">
						@for(String c : options.conquerors())
							!{ String[] ci = conq.getOrDefault(c, new String[] {c, c, "currencyaddmodtorare", "", ""}); }
							<button type="submit" name="action" value="CONQUEROR:${c}" class="btn btn-sm h-auto min-h-12 flex-col gap-0.5 py-1 btn-ghost border border-base-300" data-craft-action="CONQUEROR:${c}" disabled="${view.blocked() != null ? blocked.containsKey("CONQUEROR:" + c) : (!rare || !inf.isEmpty() || full)}" data-craft-blocked="${blocked.get("CONQUEROR:" + c)}" title="${blocked.containsKey("CONQUEROR:" + c) ? (ko ? ci[0] : ci[1]) + " — " + blocked.get("CONQUEROR:" + c) : (tip.apply(ci[4]) != null ? tip.apply(ci[4]) : (ko ? ci[0] : ci[1]))}">
								<img src="/poe-assets/currency/${ci[2]}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
								<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? ci[0] : ci[1]}</span>
							</button>
						@endfor
					</div>
					@if(eldritchClass)
						<%-- 지배의 오브(C345) — 영향력 옵션 둘 이상: 하나 올리고(최고 등급이면 고양) 하나 지움 --%>
						!{ PoeCraft.CurrencyInfo dom = infoByName.get("Orb of Dominance"); String domWhy = blocked.get("DOMINANCE"); }
						<div class="px-3 pb-3">
							<button type="submit" name="action" value="DOMINANCE" class="btn btn-sm h-auto min-h-12 gap-2 py-1 ${"DOMINANCE".equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="DOMINANCE" disabled="${domWhy != null}" data-craft-blocked="${domWhy}" title="${(ko && dom != null ? dom.nameKo() : "Orb of Dominance") + (domWhy != null ? " — " + domWhy : "")}">
								<img src="/poe-assets/currency/currencyupgradeinfluencemod.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
								<span class="text-xs leading-tight whitespace-normal text-left">${ko ? "지배의 오브 — 영향력 옵션 하나 올림(최고면 고양) · 다른 하나 지움" : "Orb of Dominance — upgrade one influenced mod (top tier → Elevated), remove another"}</span>
							</button>
						</div>
					@endif
					<%-- 각성자의 오브(C346) — 다른 영향력 아이템을 없애 두 영향력 레어로(양쪽 영향력 옵션 하나씩 보장, 없앨 아이템 옵션은 그 영향력 전용에서 무작위) --%>
					<div class="px-3 pb-3 space-y-1">
						<div class="text-xs text-base-content/70">${ko ? "각성자의 오브 — 없앨 아이템의 영향력:" : "Awakener's Orb — influence of the item to destroy:"}</div>
						<div class="grid grid-cols-2 sm:grid-cols-4 gap-2">
							@for(String c : options.conquerors())
								!{ String[] ci = conq.getOrDefault(c, new String[] {c, c, "currencyaddmodtorare", "", ""}); String aw = "AWAKENER:" + c; String awWhy = blocked.get(aw); }
								<button type="submit" name="action" value="${aw}" class="btn btn-sm h-auto min-h-10 gap-1 py-1 btn-ghost border border-base-300" data-craft-action="${aw}" disabled="${awWhy != null}" data-craft-blocked="${awWhy}" title="${(ko ? "각성자의 오브 + " + ci[0] : "Awakener's Orb + " + ci[1]) + (awWhy != null ? " — " + awWhy : "")}">
									<img src="/poe-assets/currency/applyinfluence.png" alt="" class="w-5 h-5 object-contain" loading="lazy">
									<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? ci[0] : ci[1]}</span>
								</button>
							@endfor
						</div>
					</div>
				</details>
			@endif

			@if(eldritchClass)
				<%-- 엘드리치(C343) — 불씨 = 작열의 총주교, 영액 = 세계 포식자 고정 속성(등급 = 화폐 등급). 섬뜩한 카오스 · 엑잘 · 소멸은 우세한 쪽만(총주교 = 접두, 포식자 = 접미, poedb) --%>
				<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="eldritch" data-swap-anchor-key="eldritch" open="${"eldritch".equals(openGroup)}">
					<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "섬뜩한" : "Eldritch"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "불씨 · 영액 = 고정 속성(등급 = 화폐 등급), 섬뜩한 화폐 = 우세한 쪽 접두/접미만" : "embers · ichors = implicits (tier = currency tier), eldritch orbs = dominant side only"}</span></summary>
					<div class="px-3 pb-3 space-y-2">
						@for(String kind : java.util.List.of("EMBER", "ICHOR"))
							<div class="grid grid-cols-4 gap-2">
								@for(int t = 1; t <= 4; t++)
									!{
										String en = tierEn[t - 1] + ("EMBER".equals(kind) ? " Eldritch Ember" : " Eldritch Ichor");
										PoeCraft.CurrencyInfo ci = infoByName.get(en);
										String act = kind + ":" + t;
										String why = blocked.get(kind);
									}
									<button type="submit" name="action" value="${act}" class="btn btn-sm h-auto min-h-12 flex-col gap-0.5 py-1 ${act.equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="${act}" disabled="${why != null}" data-craft-blocked="${why}" title="${(ko && ci != null ? ci.nameKo() : en) + (why != null ? " — " + why : "")}">
										<img src="/poe-assets/currency/${ci == null ? "" : ci.slug().toLowerCase()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
										<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? tierKo[t - 1] + ("EMBER".equals(kind) ? " 불씨" : " 영액") : tierEn[t - 1] + ("EMBER".equals(kind) ? " Ember" : " Ichor")}</span>
									</button>
								@endfor
							</div>
						@endfor
						<div class="grid grid-cols-3 gap-2">
							@for(String[] eo : java.util.List.of(new String[] {"ELDRITCH_CHAOS", "Eldritch Chaos Orb", "섬뜩한 카오스"}, new String[] {"ELDRITCH_EXALT", "Eldritch Exalted Orb", "섬뜩한 엑잘티드"}, new String[] {"ELDRITCH_ANNUL", "Eldritch Orb of Annulment", "섬뜩한 소멸"}))
								!{
									PoeCraft.CurrencyInfo ci = infoByName.get(eo[1]);
									String why = blocked.get(eo[0]);
								}
								<button type="submit" name="action" value="${eo[0]}" class="btn btn-sm h-auto min-h-12 flex-col gap-0.5 py-1 ${eo[0].equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="${eo[0]}" disabled="${why != null}" data-craft-blocked="${why}" title="${(ko && ci != null ? ci.nameKo() : eo[1]) + (why != null ? " — " + why : "")}">
									<img src="/poe-assets/currency/${ci == null ? "" : ci.slug().toLowerCase()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
									<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? eo[2] : eo[1].replace("Eldritch ", "").replace(" Orb", "").replace("Orb of ", "")}</span>
								</button>
							@endfor
						</div>
						<%-- 갈등의 오브(C398) — 총주교 · 포식자 고정 속성 둘 다: 하나 올리고 하나 내림(관련 강도 — 커뮤니티 측정 50% ± 11%/등급) --%>
						!{ PoeCraft.CurrencyInfo cf = infoByName.get("Orb of Conflict"); String cfWhy = blocked.get("CONFLICT"); String cfD = cf == null ? null : ko && cf.directionsKo() != null ? cf.directionsKo() : cf.directions(); }
						<button type="submit" name="action" value="CONFLICT" class="btn btn-sm h-auto min-h-12 gap-2 py-1 w-full justify-start ${"CONFLICT".equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="CONFLICT" disabled="${cfWhy != null}" data-craft-blocked="${cfWhy}" title="${(ko ? "갈등의 오브" : "Orb of Conflict") + (cfWhy != null ? " — " + cfWhy : cfD != null ? " — " + cfD : "")}">
							<img src="/poe-assets/currency/currencyconflictorb.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
							<span class="text-xs leading-tight whitespace-normal text-left">${ko ? "갈등의 오브 — 하나 올리고 하나 내림(등급이 같으면 반반, 차이 1당 약 11% — 커뮤니티 측정, 하급을 내리면 지움)" : "Orb of Conflict — raise one, lower the other (50/50 at equal tiers, ~11% per tier of difference — community-measured; a Lesser one lowered is removed)"}</span>
						</button>
					</div>
				</details>
			@endif
			<%-- 야수 제작 각인(C350) — 매직 아이템을 각인해 두고, 제왕 등이 마음에 안 들면 각인 상태로 되돌린다(한 번 쓰면 각인이 사라짐) --%>
			@if(jewellery && !catalysts.isEmpty())
				<%-- 기폭제(C385) — 반지 · 목걸이 · 허리띠 퀄리티: 태그가 맞는 옵션 수치 × (100 + 퀄리티)%(PoB 규칙), 다른 종류를 바르면 처음부터 --%>
				<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="catalyst" data-swap-anchor-key="catalyst" open="${"catalyst".equals(openGroup)}">
					<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "기폭제 — 장신구 퀄리티" : "Catalysts — jewellery quality"} <span class="text-xs font-normal text-base-content/60" data-craft-quality-now="${qAmt > 0 ? qKey : null}">· ${qAmt > 0 && qInfo != null ? (ko ? "지금: " + qInfo.qualityLabelKo() + " " + qAmt + "%" : "now: " + qInfo.qualityLabel() + " " + qAmt + "%") : (ko ? "태그가 맞는 옵션 수치를 최대 20% 올림" : "raises matching modifier values by up to 20%")}</span></summary>
					<div class="px-3 pb-3 space-y-2">
						<div class="grid grid-cols-3 sm:grid-cols-4 gap-2">
							@for(PoeCraft.CurrencyInfo c : catalysts)
								!{
									boolean tv = "vaal".equals(c.catalyst());
									String why = tv ? (item.corrupted() ? null : (ko ? "타락한 장신구에만" : "corrupted jewellery only"))
										: item.corrupted() ? (ko ? "타락한 아이템 — 더럽혀진 기폭제만" : "corrupted — tainted catalyst only")
										: c.catalyst().equals(qKey) && qAmt >= 20 ? (ko ? "아이템의 퀄리티가 이미 최대입니다." : "Item is already at the maximum Quality.") : null;
									String d = ko && c.descriptionKo() != null ? c.descriptionKo() : c.description();
									String cAct = "CATALYST:" + c.catalyst();
								}
								<button type="submit" name="action" value="${cAct}" class="btn btn-sm h-auto min-h-12 flex-col gap-0.5 py-1 ${cAct.equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="${cAct}" disabled="${why != null}" data-craft-blocked="${why}" title="${(ko ? c.nameKo() : c.name()) + (why != null ? " — " + why : d != null ? " — " + d : "")}">
									<img src="/poe-assets/currency/${c.slug().toLowerCase()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
									<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? c.nameKo() : c.name().replace(" Catalyst", "")}</span>
								</button>
							@endfor
						</div>
						<label class="flex items-center gap-2 text-xs cursor-pointer"><input type="checkbox" name="catalystFill" value="1" class="checkbox checkbox-xs" data-craft-catalyst-fill>${ko ? "20%까지 한 번에(쓴 개수만큼 비용)" : "Fill to 20% at once (cost = catalysts used)"}</label>
						<p class="text-xs text-base-content/70">${ko ? "인게임 설명: 아이템 레벨이 낮을수록 한 번에 더 붙고 최대 20%, 다른 종류를 바르면 그 종류로 바뀝니다. 한 번에 붙는 양은 공개되지 않아 높은 레벨 기준 1%로 가정합니다. 오르는 옵션 = 태그가 맞는 옵션(좌측 · 우측은 접두 · 접미), 수치 × (100 + 퀄리티)%(Path of Building 규칙). 게임 데이터에 장신구 전용 태그가 없어 넓은 태그(원소 피해 · 방어 · 저항 · 능력치)로 판정하는 가정이라 실제보다 많은 옵션이 오를 수 있고, 에센스 전용 옵션은 태그가 없어 좌측 · 우측만 맞습니다. 더럽혀진 기폭제는 타락한 장신구에 무작위 종류 · 1~20%(균등 가정)." : "In game: lower item levels gain more per use, up to 20%, and a different type replaces the quality. The amount per use is unpublished, so 1% (high item level) is assumed. Boosted = modifiers with a matching tag (Sinistral/Dextral = prefixes/suffixes), values × (100 + quality)% (Path of Building rule). The game data has no jewellery-only tags, so broad tags (elemental damage, defences, resistance, attribute) are assumed and may boost more than the game does; essence-only modifiers have no tags and only match Sinistral/Dextral. Tainted Catalyst: random type and 1–20% on corrupted jewellery (even odds assumed)."}</p>
					</div>
				</details>
			@endif
			<%-- 두카트(C391, 군왕호 올플레임 제작 — CurrencyItems 설명) --%>
			!{
				boolean hasAspect2 = lines.stream().anyMatch(l -> l.id().matches("Grants\\w+AspectCrafted\\d*"));
				long sufN2 = lines.stream().filter(l -> "suffix".equals(l.gen())).count();
				boolean brineOff = item.corrupted() || item.itemClass().contains("Flask") || item.itemClass().contains("Jewel") || hasAspect2
					|| sufN2 >= (normal ? 1 : max) || craftedCount >= (meta != null && meta.multimod() ? 3 : 1);
				record Ducat(String arg, String icon, String en, String ko, String effEn, String effKo, boolean off) {}
				java.util.List<Ducat> ducats = java.util.List.of(
					new Ducat("genteel", "deepwaterattributeswap", "The Genteel's Ducat", "고상한 자의 두카트", "Changes a modifier that grants a single Attribute to grant a different Attribute", "능력치 하나를 부여하는 속성을 변경하여 다른 능력치를 부여하게 합니다.", normal || item.corrupted() || lines.stream().noneMatch(l -> !l.fractured() && !l.crafted() && l.id().matches("(Strength|Dexterity|Intelligence)\\d+"))),
					new Ducat("rotmother", "deepwatersaboteur", "Rotmother's Ducat", "부패모의 두카트", "Augments a rare belt with a Trap or Mine modifier", "희귀 허리띠에 덫 또는 지뢰 속성을 1개 부여합니다.", !"Belt".equals(item.itemClass()) || !rare || item.corrupted()),
					new Ducat("brinehook", "deepwaterpantheon", "Brinehook's Ducat", "브라인훅의 두카트", "Crafts a random Pantheon Aspect Skill onto an item", "아이템에 무작위 판테온 위상 스킬을 부여합니다.", brineOff),
					new Ducat("cyaxan", "deepwatermercenarycurrency", "Cyaxan's Ducat", "사이악산의 두카트", "Reforges a Rare item with random modifiers, including an Infamous modifier", "희귀 아이템을 재련해 악명 높은 속성을 포함하는 무작위 신규 속성 부여", !rare || item.corrupted() || item.ilvl() < 68 || !java.util.Set.of("Body Armour", "Boots", "Gloves", "Helmet", "Amulet", "Belt", "Ring", "Quiver", "Bow", "Staff", "Warstaff", "Wand").contains(item.itemClass())),
					new Ducat("ukatoa", "deepwatereldritch", "Ukatoa's Ducat", "우카토아의 두카트", "Replaces the implicit Modifier of a non-Unique Amulet with an Eldritch implicit, provided it does not already have one.", "비-고유 목걸이에 섬뜩한 고정 속성이 없는 경우, 고정 속성을 섬뜩한 고정 속성으로 교체합니다.", !"Amulet".equals(item.itemClass()) || item.corrupted() || (item.influence() != null && !item.influence().isEmpty()) || implicits.stream().anyMatch(l -> "exarch".equals(l.gen()) || "eater".equals(l.gen()))),
					new Ducat("merrick", "deepwaterabysscurrency", "Merrick's Ducat", "메릭의 두카트", "Augment a Rare Abyssal Jewel with a random additional Modifier and corrupt it, even if it is already at maximum Modifier count.", "희귀 심연 주얼에 무작위 속성 하나를 추가하고 그 주얼을 타락시킵니다. 이미 속성 개수가 최대치인 경우에도 적용할 수 있습니다.", !"AbyssJewel".equals(item.itemClass()) || !rare || item.corrupted()));
			}
			<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="ducat" data-swap-anchor-key="ducat" open="${"ducat".equals(openGroup)}">
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "두카트 — 군왕호 올플레임 제작" : "Ducats — Allflame crafting aboard The Sovereign"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "능력치 바꾸기 · 허리띠 덫/지뢰 · 판테온 위상 · 악명 높은 재련 · 목걸이 섬뜩한 고정 속성 · 심연 주얼 추가+타락" : "attribute swap · belt trap/mine · Pantheon aspect · Infamous reforge · amulet Eldritch implicit · abyss jewel add+corrupt"}</span></summary>
				<div class="px-3 pb-3 space-y-2">
					<div class="grid grid-cols-2 sm:grid-cols-4 gap-2">
						@for(Ducat dc : ducats)
							<button type="submit" name="action" value="DUCAT:${dc.arg()}" class="btn btn-sm h-auto min-h-12 flex-col gap-0.5 py-1 ${("DUCAT:" + dc.arg()).equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="DUCAT:${dc.arg()}" disabled="${dc.off()}" data-craft-blocked="${dc.off() ? (ko ? "이 아이템엔 쓸 수 없습니다(대상 종류 · 희귀도 · 타락)" : "Cannot be used on this item (class / rarity / corruption)") : null}" title="${(dc.off() ? (ko ? "이 아이템엔 쓸 수 없습니다 — " : "Cannot be used on this item — ") : "") + (ko ? dc.ko() + " — " + dc.effKo() : dc.en() + " — " + dc.effEn())}">
								<img src="/poe-assets/currency/${dc.icon()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
								<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? dc.ko() : dc.en()}</span>
							</button>
						@endfor
					</div>
					<p class="text-xs text-base-content/70">${ko ? "고상한 자 = 힘 · 민첩 · 지능 옵션 하나를 다른 둘 중 하나로(같은 등급, 분열 · 제작 · 잠긴 쪽 제외), 부패모 = 덫 · 지뢰 옵션 11종 가중치대로(같은 그룹이라 하나만), 브라인훅 = 판테온 위상 넷 중 하나(제작 접미어 — 위상 · 작업대 한도와 같은 규칙), 메릭 = 희귀 심연 주얼에 옵션 하나(자리 한도 무시) + 타락, 사이악산 = 카오스처럼 재련하되 그 부위의 악명 높은 속성(트라투스 용병 옵션 28종, 아이템 레벨 68) 하나 보장. 우카토아 = 목걸이 고정 속성을 섬뜩한 고정 속성 하나로(이미 있으면 불가 — 진영 · 등급은 설명에 없어 목걸이 풀 가중치대로 가정)." : "Genteel: one Str/Dex/Int modifier becomes one of the other two (same tier; fractured, crafted and locked excluded). Rotmother: one of 11 trap/mine modifiers by weight (one group). Brinehook: one of four Pantheon aspects (crafted suffix — same rules as aspects/bench). Merrick: one modifier on a rare Abyss Jewel ignoring the limit, then corrupted. Cyaxan: reforge like a Chaos Orb with one Infamous modifier for that slot guaranteed (28 Mercenary modifiers, item level 68). Ukatoa: replaces an amulet's implicit with one Eldritch implicit (not if it already has one — faction and tier aren't stated, so the amulet pool weights are assumed)."}</p>
				</div>
			</details>
			<%-- 타락 뒤(C359) — 더럽혀진 화폐(타락한 희귀 아이템 전용, 인게임 설명 그대로 둘 중 하나) --%>
			<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="tainted" data-swap-anchor-key="tainted" open="${"tainted".equals(openGroup)}">
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "타락 뒤 — 더럽혀진 화폐" : "After corruption — tainted currency"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "타락한 희귀 아이템에만: 다시 굴림 | 전부 지움, 붙임 | 지움, 등급 ±1" : "corrupted rares only: reforge | clear, add | remove, tier ±1"}</span></summary>
				<div class="px-3 pb-3 space-y-2">
					<div class="grid grid-cols-3 gap-2">
						@for(String[] tc : java.util.List.of(new String[] {"TAINTED_CHAOS", "Tainted Chaos Orb", "더럽혀진 카오스"}, new String[] {"TAINTED_EXALT", "Tainted Exalted Orb", "더럽혀진 엑잘티드"}, new String[] {"TAINTED_DIVINE", "Tainted Divine Teardrop", "더럽혀진 눈물방울"}))
							!{
								PoeCraft.CurrencyInfo ci = infoByName.get(tc[1]);
								String why = blocked.get(tc[0]);
								String d = ci == null ? null : ko && ci.directionsKo() != null ? ci.directionsKo() : ci.directions();
							}
							<button type="submit" name="action" value="${tc[0]}" class="btn btn-sm h-auto min-h-12 flex-col gap-0.5 py-1 ${tc[0].equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="${tc[0]}" disabled="${why != null}" data-craft-blocked="${why}" title="${(ko && ci != null ? ci.nameKo() : tc[1]) + (why != null ? " — " + why : d != null ? " — " + d : "")}">
								<img src="/poe-assets/currency/${ci == null ? "" : ci.slug().toLowerCase()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
								<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? tc[2] : tc[1].replace("Tainted ", "").replace(" Orb", "")}</span>
							</button>
						@endfor
					</div>
					<p class="text-xs text-base-content/70">${ko ? "엑잘티드는 자리가 꽉 찼으면 지우기만, 지울 옵션이 없으면 붙이기만 합니다(가정)." : "Exalted only removes when full and only adds when nothing can be removed (assumed)."}</p>
				</div>
			</details>

			<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="beast" data-swap-anchor-key="beast" open="${"beast".equals(openGroup)}">
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "야수 제작 · 각인" : "Beastcrafting · Imprint"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "접두/접미 맞바꾸기 · 영향력 옵션 추가 · 위상 스킬(아인하르 레시피)" : "prefix/suffix swap, influence mod, aspect skills (Einhar recipes)"}</span>@if(!imprintJson.isEmpty()) <span class="badge badge-sm badge-info badge-outline" data-craft-imprinted>${ko ? "각인됨" : "Imprinted"}</span>@endif</summary>
				<div class="px-3 pb-3 flex flex-wrap gap-2">
					<button type="submit" name="action" value="IMPRINT" class="btn btn-sm btn-ghost border border-base-300 ${recommend != null && "IMPRINT".equals(recommend.actionId()) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-craft-action="IMPRINT" disabled="${!magic}">${ko ? "각인 만들기(매직)" : "Create imprint (magic)"}</button>
					<button type="submit" name="action" value="IMPRINT_RESTORE" class="btn btn-sm btn-ghost border border-base-300 ${recommend != null && "IMPRINT_RESTORE".equals(recommend.actionId()) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-craft-action="IMPRINT_RESTORE" disabled="${imprintJson.isEmpty()}">${ko ? "각인으로 되돌리기" : "Restore imprint"}</button>
				</div>
				<%-- 야수 제작 레시피(C387, BestiaryRecipes — 문구는 레시피 설명 그대로, 메모 "희귀 아이템에만 적용") --%>
				!{
					java.util.Map<String, String[]> beastInf = new java.util.LinkedHashMap<>();
					beastInf.put("shaper", new String[] {"쉐이퍼 아이템에 속성 부여 추가", "Add a Mod to a Shaper Item"});
					beastInf.put("elder", new String[] {"엘더 아이템에 속성 부여 추가", "Add a Mod to an Elder Item"});
					beastInf.put("eyrie", new String[] {"대속자 아이템에 속성 부여 추가", "Add a Mod to a Redeemer Item"});
					beastInf.put("basilisk", new String[] {"사냥꾼 아이템에 속성 부여 추가", "Add a Mod to a Hunter Item"});
					beastInf.put("crusader", new String[] {"십자군 아이템에 속성 부여 추가", "Add a Mod to a Crusader Item"});
					beastInf.put("adjudicator", new String[] {"전쟁군주 아이템에 속성 부여 추가", "Add a Mod to a Warlord Item"});
					java.util.List<String> myInf = java.util.Arrays.asList(infs);
					long sufN = lines.stream().filter(l -> "suffix".equals(l.gen())).count();
					long preN = lines.stream().filter(l -> "prefix".equals(l.gen())).count();
					// 지울 수 있는 쪽(C389 검토 — 엔진은 분열 · 메타 잠긴 쪽을 지우지 않는다)
					boolean preLock = meta != null && meta.prefixLocked(), sufLock = meta != null && meta.suffixLocked();
					long preRm = preLock ? 0 : lines.stream().filter(l -> "prefix".equals(l.gen()) && !l.fractured()).count();
					long sufRm = sufLock ? 0 : lines.stream().filter(l -> "suffix".equals(l.gen()) && !l.fractured()).count();
					// 위상(C388) — 접미 자리 · 제작 옵션 한도 · 이미 위상
					boolean hasAspect = lines.stream().anyMatch(l -> l.id().matches("Grants\\w+AspectCrafted\\d*"));
					boolean aspectOff = item.corrupted() || item.itemClass().contains("Flask") || item.itemClass().contains("Jewel") || hasAspect
						|| sufN >= (normal ? 1 : max) || craftedCount >= (meta != null && meta.multimod() ? 3 : 1);
					// 꺼진 까닭(C451 UX QA — 설명문만 보였다). 맞바꾸기는 지울 쪽이 잠기면 붙이기만(C430 엔진 · maxroll) → 잠김은 끄지 않는다
					String swapSufWhy = !rare ? (ko ? "희귀 아이템에만 쓸 수 있습니다" : "Rare items only") : item.corrupted() ? (ko ? "타락한 아이템은 바꿀 수 없습니다" : "Corrupted items cannot be modified") : sufN >= max ? (ko ? "접미 자리가 없습니다" : "No suffix slot") : preRm == 0 && !preLock ? (ko ? "지울 수 있는 접두어가 없습니다(분열 제외)" : "No removable prefix (fractured excluded)") : null;
					String swapPreWhy = !rare ? (ko ? "희귀 아이템에만 쓸 수 있습니다" : "Rare items only") : item.corrupted() ? (ko ? "타락한 아이템은 바꿀 수 없습니다" : "Corrupted items cannot be modified") : preN >= max ? (ko ? "접두 자리가 없습니다" : "No prefix slot") : sufRm == 0 && !sufLock ? (ko ? "지울 수 있는 접미어가 없습니다(분열 제외)" : "No removable suffix (fractured excluded)") : null;
					String beastInfWhy = !rare ? (ko ? "희귀 아이템에만 쓸 수 있습니다" : "Rare items only") : item.corrupted() ? (ko ? "타락한 아이템은 바꿀 수 없습니다" : "Corrupted items cannot be modified") : full ? (ko ? "빈 자리가 없습니다" : "No empty slot") : null;
					String aspectWhy = !aspectOff ? null : item.corrupted() ? (ko ? "타락한 아이템은 바꿀 수 없습니다" : "Corrupted items cannot be modified") : item.itemClass().contains("Flask") || item.itemClass().contains("Jewel") ? (ko ? "플라스크 · 주얼엔 쓸 수 없습니다" : "Not for flasks or jewels") : hasAspect ? (ko ? "이미 위상 스킬이 있습니다" : "Already has an aspect skill") : sufN >= (normal ? 1 : max) ? (ko ? "접미 자리가 없습니다" : "No suffix slot") : (ko ? "제작 옵션 한도(1, 메타 '제작 옵션 3개'면 3)" : "Crafted modifier limit (1, or 3 with the multimod meta-craft)");
				}
				<div class="px-3 pb-3 flex flex-wrap gap-2 border-t border-base-200 pt-2">
					<button type="submit" name="action" value="BEAST:swap-suffix" class="btn btn-sm ${"BEAST:swap-suffix".equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="BEAST:swap-suffix" disabled="${swapSufWhy != null}" data-craft-blocked="${swapSufWhy}" title="${(swapSufWhy != null ? swapSufWhy + " — " : "") + (ko ? "희귀 아이템에만 적용 — 무작위 접두어 하나를 지우고(분열 · 잠긴 쪽 제외 — 접두가 잠겼으면 붙이기만) 접미어 하나를 붙입니다" : "Rare items only — removes a random prefix (not fractured or locked — only adds if prefixes are locked) and adds a suffix")}">${ko ? "접미어 추가, 무작위 접두어 제거" : "Add a Suffix, Remove a Random Prefix"}</button>
					<button type="submit" name="action" value="BEAST:swap-prefix" class="btn btn-sm ${"BEAST:swap-prefix".equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="BEAST:swap-prefix" disabled="${swapPreWhy != null}" data-craft-blocked="${swapPreWhy}" title="${(swapPreWhy != null ? swapPreWhy + " — " : "") + (ko ? "희귀 아이템에만 적용 — 무작위 접미어 하나를 지우고(분열 · 잠긴 쪽 제외 — 접미가 잠겼으면 붙이기만) 접두어 하나를 붙입니다" : "Rare items only — removes a random suffix (not fractured or locked — only adds if suffixes are locked) and adds a prefix")}">${ko ? "접두어 추가, 무작위 접미어 제거" : "Add a Prefix, Remove a Random Suffix"}</button>
					@for(java.util.Map.Entry<String, String[]> bi : beastInf.entrySet())
						@if(myInf.contains(bi.getKey()))
							<button type="submit" name="action" value="BEAST:influence:${bi.getKey()}" class="btn btn-sm ${("BEAST:influence:" + bi.getKey()).equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="BEAST:influence:${bi.getKey()}" disabled="${beastInfWhy != null}" data-craft-blocked="${beastInfWhy}" title="${(beastInfWhy != null ? beastInfWhy + " — " : "") + (ko ? "그 영향력 아이템에 그 영향력 전용 옵션 하나(정복자 엑잘과 같은 옵션, 영향력은 그대로)" : "One influence-only modifier of that type (same pool as the conqueror exalt), influence unchanged")}">${ko ? bi.getValue()[0] : bi.getValue()[1]}</button>
						@endif
					@endfor
					<%-- 위상 스킬(C388, 레시피 "30레벨 … 위상 스킬") — 제작 접미어, 작업대 옵션 한도 · 같은 그룹(위상끼리) --%>
					@for(String[] asp : java.util.List.of(new String[] {"cat", "30레벨 고양이의 위상 스킬", "Level 30 Aspect of the Cat skill"}, new String[] {"avian", "30레벨 새의 위상 스킬", "Level 30 Aspect of the Avian skill"}, new String[] {"spider", "30레벨 거미의 위상 스킬", "Level 30 Aspect of the Spider skill"}, new String[] {"crab", "30레벨 게의 위상 스킬", "Level 30 Aspect of the Crab skill"}))
						<button type="submit" name="action" value="BEAST:aspect:${asp[0]}" class="btn btn-sm ${("BEAST:aspect:" + asp[0]).equals(lastAction) ? "btn-ghost border border-primary" : "btn-ghost border border-base-300"}" data-craft-action="BEAST:aspect:${asp[0]}" disabled="${aspectOff}" data-craft-blocked="${aspectWhy}" title="${(aspectWhy != null ? aspectWhy + " — " : "") + (ko ? "제작 접미어 하나(작업대 옵션처럼 한도 1 — 메타 '제작 옵션 3개'면 3, 위상끼리 같은 그룹). 대상 종류는 레시피 표에 없어 장비 전부로 가정" : "One crafted suffix (counts toward the crafted limit, aspects share a group). Target classes aren't in the recipe data — all gear assumed")}">${ko ? asp[1] : asp[2]}</button>
					@endfor
					<p class="w-full text-xs text-base-content/70">${ko ? "재료 야수는 레시피 표에 없어 쓴 화폐에는 횟수만 셉니다(시세 없음)." : "The beasts used aren't listed in the recipe data, so only the count is tracked (no price)."}</p>
				</div>
			</details>

			<%-- 시작 아이템 직접 편집(C348 — craftofexile 처럼 분열 베이스 등에서 출발). 넣기는 오른쪽 "다음 옵션 확률" 표의 + · +분열, 빼기는 여기서 --%>
			<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="manual" data-swap-anchor-key="manual" open="${"manual".equals(openGroup)}">
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "직접 편집" : "Edit directly"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "시작 아이템 만들기 — 넣기는 확률 표(넓은 화면은 오른쪽, 좁은 화면은 아래)의 + / +분열(화폐 비용 없음)" : "build a starting item — add with + / +frac (fractured) in the probability table (right, or below on narrow screens; no currency)"}</span></summary>
				<div class="px-3 pb-3 space-y-1">
					@if(lines.isEmpty())
						<p class="text-xs text-base-content/70">${ko ? "옵션이 없습니다." : "No modifiers."}</p>
					@endif
					@for(PoeCraft.Line l : lines)
						!{ java.util.List<String> lx = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en()); }
						<div class="flex items-center gap-2 text-xs" data-craft-manual-line="${l.id()}">
							<span class="min-w-0 flex-1 wrap-anywhere">${"prefix".equals(l.gen()) ? (ko ? "접두 " : "P ") : (ko ? "접미 " : "S ")}${String.join(", ", lx)}@if(l.fractured()) (${ko ? "분열" : "fractured"})@endif</span>
							<button type="submit" name="action" value="MANUAL_REMOVE:${l.id()}" class="btn btn-xs btn-ghost border border-base-300" data-craft-action="MANUAL_REMOVE:${l.id()}">${ko ? "빼기" : "Remove"}</button>
						</div>
					@endfor
				</div>
			</details>
		</div>

	</form>

	<%-- 작업 영역(C455 — 한 화면): 아이템 카드 | 다음 옵션 확률(칸 안 스크롤) | 단계 기록 + 쓴 화폐(칸 안 스크롤). 2칸(lg)은 카드 아래 기록, 좁은 화면은 확률 표가 맨 뒤 --%>
	<div class="grid gap-4 items-start lg:grid-cols-[minmax(0,22rem)_minmax(0,1fr)] xl:grid-cols-[minmax(0,22rem)_minmax(0,1fr)_minmax(24rem,1fr)]" data-craft-workspace>
	<%-- 대상 아이템은 늘 보이게(C455, PoE2 C453 의 짝) — 좁은 화면 · 넓은 3칸 화면은 화면 위에 붙는다. 2칸(lg)은 아래 기록이 가려져 붙이지 않음 --%>
	<div class="sticky top-[var(--site-header-height,4.4375rem)] z-20 bg-base-100 pt-1 pb-2 space-y-1 min-w-0 max-h-[32vh] overflow-y-auto lg:static lg:max-h-none lg:overflow-visible xl:sticky" data-craft-item-sticky><%-- 좁은 화면 높이 40 → 32vh · 위치 = 실제 헤더 높이(10-22 Q9 UX QA — 390 에서 화면 35~46% · 헤더와 틈) --%>
		<div class="poe-tooltip ${rarCls} w-full max-w-[24rem]" style="border-color:${borderColor}" data-craft-tooltip data-craft-arm-target>
			<%-- 레어 = 인게임 2줄 머리 그림(이름 + 베이스, 10-23 Q24 — 1줄 그림에 두 줄을 눌러 넣었다, itemTooltip 과 같은 poe-tt-band-2) --%>
			<div class="poe-tt-band${rare ? " poe-tt-band-2" : ""}">
				@if(infInfo != null)
					<img src="/poe-data/ui-assets/${infInfo[3]}.png" alt="" style="position:absolute;left:8px;top:6px;width:28px;height:28px;object-fit:contain" data-craft-influence="${inf}">
					<img src="/poe-data/ui-assets/${iconR != null ? iconR : infInfo[3]}.png" alt="" style="position:absolute;right:8px;top:6px;width:28px;height:28px;object-fit:contain" data-craft-influence-right="${iconR}">
				@endif
				<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${headColor}" data-craft-name>${rare ? (shownName != null && !shownName.isBlank() ? shownName : (ko ? "레어 아이템" : "Rare Item")) : magicName}</div>
				@if(rare)<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${headColor}" data-craft-base>${baseLabel}</div>@endif
			</div>
			<%-- 베이스 속성 · 요구 사항(C490 — 인게임 툴팁처럼 고른 베이스 + 이 아이템 로컬 옵션, 규칙은 빌드 툴팁과 같은 PoeLocalProps · PoeRequirements, 요구 레벨 = 베이스 레벨과 옵션 최고 레벨의 80% 중 큰 값) --%>
			!{
				java.util.List<String> cbMods = new java.util.ArrayList<>();
				for (PoeCraft.Line cl : lines) {
					java.util.List<String> cx = cl.en() == null ? java.util.List.of() : cl.en();
					java.util.List<String> cn = cl.enMin() == null ? java.util.List.of() : cl.enMin();
					Long crs = rolls.get(cl.id());
					for (int ci = 0; ci < cx.size(); ci++) {
						String crg = PoeModRange.range(ci < cn.size() ? cn.get(ci) : null, cx.get(ci));
						cbMods.add(crs != null ? net.luversof.web.gate.poe.PoeModRoll.roll(crg, crs + ci) : crg);
					}
				}
				java.util.List<String> cbImp = new java.util.ArrayList<>();
				for (PoeCraft.Line cl : implicits) {
					java.util.List<String> cx = cl.en() == null ? java.util.List.of() : cl.en();
					java.util.List<String> cn = cl.enMin() == null ? java.util.List.of() : cl.enMin();
					Long crs = rolls.get("imp:" + cl.id());
					for (int ci = 0; ci < cx.size(); ci++) {
						String crg = PoeModRange.range(ci < cn.size() ? cn.get(ci) : null, cx.get(ci));
						cbImp.add(crs != null ? net.luversof.web.gate.poe.PoeModRoll.roll(crg, crs + ci) : crg);
					}
				}
				net.luversof.web.gate.poe.dto.PoeBaseItem.Armour cbA = craftBase == null ? null : craftBase.armour();
				boolean cbWeapon = craftBase != null && craftBase.weapon() != null && craftBase.weapon().damageMax() > 0;
				boolean cbArmour = cbA != null && (cbA.block() > 0 || cbA.armourMax() > 0 || cbA.evasionMax() > 0 || cbA.energyShieldMax() > 0 || cbA.wardMax() > 0);
				net.luversof.web.gate.poe.PoeLocalProps.Result cbLp = !(cbWeapon || cbArmour) ? null : net.luversof.web.gate.poe.PoeLocalProps.apply(new net.luversof.web.gate.poe.PoeLocalProps.Base(
					cbA == null ? 0 : cbA.armourMin(), cbA == null ? 0 : cbA.armourMax(),
					cbA == null ? 0 : cbA.evasionMin(), cbA == null ? 0 : cbA.evasionMax(),
					cbA == null ? 0 : cbA.energyShieldMin(), cbA == null ? 0 : cbA.energyShieldMax(),
					cbA == null ? 0 : cbA.wardMin(), cbA == null ? 0 : cbA.wardMax(),
					cbA == null ? 0 : cbA.block(),
					cbWeapon ? craftBase.weapon().damageMin() : 0, cbWeapon ? craftBase.weapon().damageMax() : 0,
					cbWeapon ? craftBase.weapon().attacksPerSecond() : 0, cbWeapon ? craftBase.weapon().critChance() : 0),
					net.luversof.web.gate.poe.PoeLocalProps.join(cbImp, cbMods), cbArmour, cbWeapon, 0);
				// 일반 주얼은 요구 사항이 없다(어느 레벨에서나 장착, C514) — 군집 · 심연 주얼은 옵션 레벨로 요구 레벨이 있다
				boolean cbNoReq = "Jewel".equals(item.itemClass()) && (item.variant() == null || !item.variant().startsWith("cluster_"));
				net.luversof.web.gate.poe.PoeRequirements.Result cbReq = craftBase == null || cbNoReq ? null : net.luversof.web.gate.poe.PoeRequirements.apply(craftBase.reqStr(), craftBase.reqDex(), craftBase.reqInt(), cbImp, cbMods);
				int cbModLevel = lines.stream().mapToInt(PoeCraft.Line::ilvl).max().orElse(0);
				int cbReqLevel = craftBase == null ? 0 : Math.max(craftBase.requiredLevel(), (int) Math.floor(cbModLevel * 0.8));
				boolean cbReqHigher = craftBase != null && cbReqLevel > craftBase.requiredLevel();
				String cbLab = "text-[#7f7f7f]";
				String cbVal = "text-[#e0e0e0]";
				String cbAug = "text-[#8888ff]";
			}
			@if(cbLp != null)
				<div class="px-4 pt-2 pb-1 text-[13px] space-y-0.5 text-center" data-craft-props>
					@if(cbWeapon)
						<%-- 무기 종류(10-23 Q25 — 인게임 무기 툴팁 속성 칸 첫 줄 '활' · 'Bow', 회색). 이름은 PoeClassNames.poe1Weapon 표(ClientStrings WeaponClassDisplayName*, Q43 · Q45) --%>
						<div class="${cbLab}" data-craft-prop-class>${net.luversof.web.gate.poe.PoeClassNames.poe1Weapon(craftBase.itemClass()) != null ? net.luversof.web.gate.poe.PoeClassNames.poe1Weapon(craftBase.itemClass()) : craftBase.itemClass()}</div>
						<div><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.physical.damage")}: </span><span class="${cbLp.physMin().changed() || cbLp.physMax().changed() ? cbAug : cbVal}" data-craft-prop-phys>${cbLp.physMin().damageText()}-${cbLp.physMax().damageText()}</span></div>
						@if(cbLp.hasElemental())<div data-craft-prop-ele><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.elemental.damage")}: </span>!{ boolean cbFirst = true; }@if(cbLp.fire().present())<span class="text-[#960000]">${cbLp.fire().text()}</span>!{ cbFirst = false; }@endif@if(cbLp.cold().present())${cbFirst ? "" : ", "}<span class="text-[#366492]">${cbLp.cold().text()}</span>!{ cbFirst = false; }@endif@if(cbLp.lightning().present())${cbFirst ? "" : ", "}<span class="text-[#ffd700]">${cbLp.lightning().text()}</span>@endif</div>@endif@if(cbLp.chaos().present())<div data-craft-prop-chaos><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.chaos.damage")}: </span><span class="text-[#d02090]">${cbLp.chaos().text()}</span></div>@endif
						<div><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.gems.detail.crit.chance")}: </span><span class="${cbLp.critChance().changed() ? cbAug : cbVal}">${cbLp.critChance().text2()}%</span></div>
						<div><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.attacks.per.second")}: </span><span class="${cbLp.attacksPerSecond().changed() ? cbAug : cbVal}" data-craft-prop-aps>${cbLp.attacksPerSecond().text2()}</span></div>
						@if(craftBase.weapon().range() > 0 && craftBase.weapon().range() < 100)<div data-craft-prop-range><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.range")}: </span><span class="${cbVal}">${String.format("%.1f", craftBase.weapon().range() / 10.0)}${ko ? "미터" : (craftBase.weapon().range() == 10 ? " metre" : " metres")}</span></div>@endif
					@endif
					@if(cbArmour)
						@if(cbLp.block().present())<div><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.block")}: </span><span class="${cbLp.block().changed() ? cbAug : cbVal}">${cbLp.block().text("-")}%</span></div>@endif
						@if(cbLp.armour().present())<div><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.armour")}: </span><span class="${cbLp.armour().changed() ? cbAug : cbVal}" data-craft-prop-armour>${cbLp.armour().text("-")}</span></div>@endif
						@if(cbLp.evasion().present())<div><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.evasion")}: </span><span class="${cbLp.evasion().changed() ? cbAug : cbVal}" data-craft-prop-evasion>${cbLp.evasion().text("-")}</span></div>@endif
						@if(cbLp.energyShield().present())<div><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.energy.shield")}: </span><span class="${cbLp.energyShield().changed() ? cbAug : cbVal}" data-craft-prop-es>${cbLp.energyShield().text("-")}</span></div>@endif
						@if(cbLp.ward().present())<div><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.ward")}: </span><span class="${cbLp.ward().changed() ? cbAug : cbVal}">${cbLp.ward().text("-")}</span></div>@endif
					@endif
				</div>
				<div class="poe-tt-sepimg"></div>
			@endif
			@if(qAmt > 0 && qInfo != null)
				<%-- 기폭제 퀄리티(C385) — 인게임 "Quality (Attack Modifiers): +20%", 값은 보강 색 --%>
				<div class="px-4 pt-2 -mb-1 text-center text-[13px] text-white/60" data-craft-quality="${qKey}">${ko ? "퀄리티 (" + qInfo.qualityLabelKo() + "): " : "Quality (" + qInfo.qualityLabel() + "): "}<span style="color:#8888ff">+${qAmt}%</span></div>
				<div class="poe-tt-sepimg"></div>
			@endif
			@if(cbReq != null && (cbReqLevel > 0 || cbReq.str().present() || cbReq.dex().present() || cbReq.intel().present()))
				<%-- 한 줄로 붙여 쓸 것(itemTooltip 과 같은 이유 — @if 사이 줄바꿈이 공백으로 렌더) --%>
				<div class="px-4 pt-2 text-center text-[13px]" data-craft-req><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.requirement")} </span>@if(cbReqLevel > 0)<span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.level")} </span><span class="${cbReqHigher ? cbAug : cbVal}" data-craft-req-level>${cbReqLevel}</span>@endif@if(cbReq.str().present())<span class="${cbVal}">${cbReqLevel > 0 ? ", " : ""}${cbReq.str().text()} </span><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.str")}</span>@endif@if(cbReq.dex().present())<span class="${cbVal}">${cbReqLevel > 0 || cbReq.str().present() ? ", " : ""}${cbReq.dex().text()} </span><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.dex")}</span>@endif@if(cbReq.intel().present())<span class="${cbVal}">${cbReqLevel > 0 || cbReq.str().present() || cbReq.dex().present() ? ", " : ""}${cbReq.intel().text()} </span><span class="${cbLab}">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.int")}</span>@endif</div>
			@endif
			<%-- 인게임 칸 나눔: 속성 / 요구 사항 / 아이템 레벨(Q24 — 요구 사항과 아이템 레벨 사이 구분선이 없었다) --%>
			@if(cbReq != null && (cbReqLevel > 0 || cbReq.str().present() || cbReq.dex().present() || cbReq.intel().present()))<div class="poe-tt-sepimg"></div>@endif
			<div class="px-4 py-2 text-center text-[13px] text-white/60" data-craft-ilvl>
				${ko ? "아이템 레벨: " : "Item Level: "}<span class="text-white">${item.ilvl()}</span>
			</div>
			@if(craftEnchant != null && !craftEnchant.isEmpty())
				<%-- 군집 주얼 인챈트(C514, 인게임 순서 — 아이템 레벨 → 인챈트 → 고정 속성 → 옵션, 인챈트 색) --%>
				<div class="poe-tt-sepimg"></div>
				<div class="px-4 py-2 space-y-0.5 text-center text-[13px] text-[#b8daf2]" data-craft-enchant>
					@for(String en : craftEnchant)
						<div>$unsafe{PoeText.highlightValues(en)}</div>
					@endfor
				</div>
			@endif
			@if(!implicits.isEmpty())
				<div class="poe-tt-sepimg"></div>
				<div class="px-4 py-2 space-y-0.5 text-center text-[13px]" data-craft-implicits>
					@for(PoeCraft.Line l : implicits)
						!{
							java.util.List<String> ix = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en());
							int ti = l.tierRank() == null ? 0 : Math.max(0, Math.min(5, l.tierRank() - 1));
						}
						<div style="color:#8888ff" data-craft-implicit="${l.id()}" data-craft-side="${l.gen()}" data-craft-ex-tier-label="${"corrupted".equals(l.gen()) ? null : (ko ? tierKo[ti] : tierEn[ti])}">
							@if("corrupted".equals(l.gen()))
								<span class="text-[10px] font-mono text-white/60 mr-0.5" title="${ko ? "바알 오브 부패 고정 속성 · 아이템 레벨 " + l.ilvl() : "Vaal Orb corrupted implicit · item level " + l.ilvl()}">${ko ? "타락" : "Corrupted"}</span>
							@else
								<span class="text-[10px] font-mono text-white/60 mr-0.5" title="${ko ? l.nameKo() + " 고정 속성 · " + tierKo[ti] : l.name() + " implicit · " + tierEn[ti]}">${ko ? ("exarch".equals(l.gen()) ? "총주교" : "포식자") + " " + tierKo[ti] : ("exarch".equals(l.gen()) ? "Exarch" : "Eater") + " " + tierEn[ti]}</span>
							@endif
							!{ java.util.List<String> imn = ko && !nz.apply(l.koMin()).isEmpty() ? l.koMin() : nz.apply(l.enMin()); Long irs = rolls.get("imp:" + l.id()); }
							@for(int i = 0; i < ix.size(); i++)
								<%-- 고정 속성도 굴린 값(C375) — 범위는 마우스를 올리면 --%>
								!{ String iRange = PoeModRange.range(i < imn.size() ? imn.get(i) : null, ix.get(i)); }
								!{ String iRolled = net.luversof.web.gate.poe.PoeModRoll.scale(irs != null ? net.luversof.web.gate.poe.PoeModRoll.roll(iRange, irs + i) : iRange, boostedIds.contains(l.id()) ? qAmt : 0); }
								<%-- Alt 상세 보기도 옵션 줄과 같이 값 뒤 범위(10-23 Q25 — 고정 속성만 title 로만 보였다) --%>
								<div class="inline" title="${irs != null ? iRange : null}" data-craft-rolled-implicit="${irs != null ? "1" : null}" data-craft-boosted="${boostedIds.contains(l.id()) && qAmt > 0 ? "1" : null}">@if(irs != null)<span class="poe-classic">$unsafe{PoeText.highlightValues(iRolled)}</span><span class="poe-advanced" data-craft-advanced>$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRoll.withRanges(iRolled, iRange))}</span>@else$unsafe{PoeText.highlightValues(iRolled)}@endif</div>@if(i < ix.size() - 1)<br>@endif
							@endfor
						</div>
					@endfor
				</div>
			@endif
			@if(!lines.isEmpty())
				<div class="poe-tt-sepimg"></div>
				<div class="px-4 py-2 space-y-0.5 text-center text-[13px]">
					@for(PoeCraft.Line l : lines)
						!{
							java.util.List<String> mx = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en());
							java.util.List<String> mn = ko && !nz.apply(l.koMin()).isEmpty() ? l.koMin() : nz.apply(l.enMin());
							boolean isNew = added != null && added.contains(l.id());
							String color = l.fractured() ? "#a29162" : l.crafted() ? "#b4b4ff" : "#8888ff";
							// 내보내기(C489) — 게임 고급 복사(Ctrl+Alt+C) 머리줄 재료: 이름 · 등급 · 태그
							java.util.List<String> exTags = ko ? (view.tagsKo() == null ? null : view.tagsKo().get(l.id())) : (view.tags() == null ? null : view.tags().get(l.id()));
						}
						<div class="${isNew ? "bg-white/10 rounded" : ""}" style="color:${color}" data-craft-line="${l.id()}" data-craft-gen="${l.gen()}" data-craft-new="${isNew ? "1" : null}" data-craft-ex-name="${ko ? l.nameKo() : l.name()}" data-craft-ex-tier="${l.tierRank() == null ? null : String.valueOf(l.tierRank())}" data-craft-ex-tags="${exTags == null ? null : String.join(", ", exTags)}" data-craft-ex-crafted="${l.crafted() ? "1" : null}" data-craft-ex-fractured="${l.fractured() ? "1" : null}">
							<span class="text-[10px] font-mono text-white/40 block leading-tight" data-craft-line-tag title="${ko ? (l.nameKo() != null ? l.nameKo() : l.name()) + (("prefix".equals(l.gen())) ? " · 접두" : " · 접미") + " · 아이템 레벨 " + l.ilvl() : l.name() + (("prefix".equals(l.gen())) ? " · prefix" : " · suffix") + " · item level " + l.ilvl()}">${"prefix".equals(l.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}@if(l.tierRank() != null) T${l.tierRank()}@endif@if(l.id().startsWith("Veiled")) <span class="font-sans" data-craft-line-veiled>${ko ? "장막 — 해제 전(쥰)" : "Veiled — not yet unveiled"}</span>@endif@if(!l.id().startsWith("Veiled") && !l.id().startsWith("Unrevealed") && (ko ? (l.nameKo() != null && !l.nameKo().isBlank()) : (l.name() != null && !l.name().isBlank()))) <span class="font-sans text-white/75" data-craft-line-affix>${(ko ? l.nameKo() : l.name()).replaceFirst("^- ", "")}</span>@endif!{ java.util.List<String> lineTags = ko ? (view.tagsKo() == null ? null : view.tagsKo().get(l.id())) : (view.tags() == null ? null : view.tags().get(l.id())); }@if(lineTags != null && !lineTags.isEmpty()) <span class="font-sans text-white/50" data-craft-line-tags>— ${String.join(", ", lineTags)}</span>@endif@if(l.essence()) ${ko ? "에센스" : "Ess"}@endif@if(l.crafted()) ${ko ? "제작" : "Crafted"}@endif@if(l.fractured()) ${ko ? "분열" : "Fractured"}@endif@if(l.name() != null && l.name().contains("Elevated")) ${ko ? "고양" : "Elevated"}@endif@if(rolls.containsKey("lucky:" + l.id())) <span data-craft-line-lucky title="${ko ? "축성된 화석 — 수치 행운(두 번 굴려 큰 값)" : "Sanctified Fossil — lucky values (higher of two rolls)"}">${ko ? "행운" : "Lucky"}</span>@endif@if(boostedIds.contains(l.id()) && qAmt > 0) <span data-craft-line-boosted title="${ko ? "기폭제 퀄리티로 수치 +" + qAmt + "%" : "Values +" + qAmt + "% from catalyst quality"}">${ko ? "퀄리티" : "Q"}</span>@endif</span>
							@if(l.id().startsWith("Veiled"))
								<%-- 장막 옵션(10-20 C482 — "게임 내랑 동일한 디자인") — 인게임 툴팁처럼 룬처럼 뒤섞인 글자 그림(게임 튜토리얼 그림에서 잘라 냄, ui-assets.mjs 5)) --%>
								<span class="craft-veiled" data-craft-veiled><img src="/poe-data/ui-assets/veiled-mod.png" alt="${ko ? l.nameKo() : l.name()}" data-craft-veiled-img><span class="craft-veiled-f" data-f="1" aria-hidden="true"></span><span class="craft-veiled-f" data-f="2" aria-hidden="true"></span></span>
							@else
								@for(int i = 0; i < mx.size(); i++)
									!{ String rangeTxt = PoeModRange.range(i < mn.size() ? mn.get(i) : null, mx.get(i)); Long rs = rolls.get(l.id()); String rolledTxt = net.luversof.web.gate.poe.PoeModRoll.scale(rs != null ? (rolls.containsKey("lucky:" + l.id()) ? net.luversof.web.gate.poe.PoeModRoll.rollLucky(rangeTxt, rangeTxt, rs + i) : net.luversof.web.gate.poe.PoeModRoll.roll(rangeTxt, rs + i)) : rangeTxt, boostedIds.contains(l.id()) ? qAmt : 0); }
									<%-- 굴린 값(C370, 인게임 툴팁처럼) — 범위는 마우스를 올리면, Alt 를 누르는 동안은 인게임 상세 보기처럼 값 뒤에 범위(10-22 Q3c — html.poe-alt) --%>
									<div class="inline" title="${rs != null ? rangeTxt : null}" data-craft-rolled="${rs != null ? "1" : null}" data-craft-boosted="${boostedIds.contains(l.id()) && qAmt > 0 ? "1" : null}">@if(rs != null)<span class="poe-classic">$unsafe{PoeText.highlightValues(rolledTxt)}</span><span class="poe-advanced" data-craft-advanced>$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRoll.withRanges(rolledTxt, rangeTxt))}</span>@else$unsafe{PoeText.highlightValues(rolledTxt)}@endif</div>@if(i < mx.size() - 1)<br>@endif
								@endfor
							@endif
						</div>
					@endfor
				</div>
				@if(lines.stream().anyMatch(x -> x.id().startsWith("Veiled")))
					<%-- 인게임 안내(ClientStrings VeiledModItemInformation) — 카드를 우클릭하면 장막 해제 칸이 열린다(common.ts) --%>
					<div class="text-xs italic text-white/55 mt-1" data-craft-veiled-hint>${ko ? "이 아이템에 우클릭하여 장막을 해제하십시오." : "Right click this item to Unveil it."}</div>
				@endif
			@endif
			@if(item.corrupted())
				<div class="poe-tt-sepimg"></div>
				<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#d20000]" data-craft-corrupted>${ko ? "타락" : "Corrupted"}</div>
			@endif
		</div>
		<%-- 내보내기 · 공유(C489, craftofexile 의 RAW 내보내기 · Share link) — 게임 고급 복사(Ctrl+Alt+C) 형식 글은 붙여넣기로 다시 들어온다 --%>
		!{
			String exInf = switch (item.influence() == null ? "" : item.influence()) {
				case "shaper" -> ko ? "쉐이퍼 아이템" : "Shaper Item";
				case "elder" -> ko ? "엘더 아이템" : "Elder Item";
				case "crusader" -> ko ? "십자군 아이템" : "Crusader Item";
				case "eyrie" -> ko ? "대속자 아이템" : "Redeemer Item";
				case "basilisk" -> ko ? "사냥꾼 아이템" : "Hunter Item";
				case "adjudicator" -> ko ? "전쟁군주 아이템" : "Warlord Item";
				default -> null;
			};
		}
		<%-- 화폐를 고른 상태 안내(C477) — 카드 아래(10-23 Q35: 위에 있으면 나타날 때 카드가 밀려 누를 자리가 움직였다) --%>
		<div class="text-xs rounded-box border border-warning/60 bg-warning/10 px-2 py-1 max-w-[24rem]" data-craft-arm-hint hidden><span class="font-semibold" data-craft-arm-label></span> ${ko ? "— 아이템을 누르면 적용(계속 누르면 연타) · 단추를 다시 누르거나 Esc 로 취소" : "— click the item to apply (keep clicking to repeat) · click the button again or Esc to cancel"}</div>
		<div class="flex flex-wrap items-center gap-1" data-craft-export-row>
			<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-export-text data-ex-lang="${ko ? "ko" : "en"}" data-ex-class="${classLabel}" data-ex-rarity="${rarity}" data-ex-ilvl="${item.ilvl()}" data-ex-name="${rare ? shownName : magicName}" data-ex-base="${baseLabel}" data-ex-influence="${exInf}" data-ex-corrupted="${item.corrupted() ? "1" : null}" title="${ko ? "게임 고급 복사(Ctrl+Alt+C)와 같은 형식 — 이 화면 붙여넣기 · 다른 도구에 그대로" : "Same format as the game's advanced copy (Ctrl+Alt+C) — paste back here or into other tools"}">${ko ? "아이템 글 복사" : "Copy item text"}</button>
			<%-- 아이템 붙여넣기(10-21 C545 사용자 요청 "아이템 글 복사가 있는데 인게임에서 복사한 아이템 붙여넣기도") — 복사의 짝. 클립보드를 읽어 게임 글이면 바로 시작, 못 읽으면 위 붙여넣기 칸(C485)을 연다 --%>
			<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-import-paste title="${ko ? "게임에서 아이템에 마우스를 올리고 Ctrl+C(옵션 이름 · 등급까지는 Ctrl+Alt+C)로 복사한 글을 붙여 넣어 그 아이템으로 시작합니다 — 클립보드 읽기를 허용하지 않으면 위쪽 붙여넣기 칸이 열립니다(Ctrl+V)" : "Paste an item copied in game with Ctrl+C (Ctrl+Alt+C for mod names and tiers) and start from it — if clipboard access is blocked, the paste box above opens (Ctrl+V)"}">${ko ? "아이템 붙여넣기" : "Paste item"}</button>
			<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-to-targets title="${ko ? "카드의 옵션(일반 옵션 · 그 등급 이상)을 아래 계산기 목표로 — '지금 아이템에서 시작'을 꺼서 이 아이템을 처음부터 만드는 최소 비용을 봅니다(작업대 · 장막 옵션은 빼고)" : "Use the card's modifiers (that tier or better) as calculator targets — unchecks 'start from current item' to price crafting this item from scratch (bench and veiled modifiers skipped)"}">${ko ? "옵션을 계산기 목표로" : "Use as calculator targets"}</button>
			<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-share-link title="${ko ? "지금 아이템을 여는 주소(새로 고침 · 공유)" : "A link that opens this item (reload · share)"}">${ko ? "공유 링크 복사" : "Copy share link"}</button>
			<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-inv-save title="${ko ? "지금 아이템을 이 브라우저의 보관함에 담아 둡니다 — 위쪽 '보관함'에서 그대로 다시 열기" : "Keeps this item in this browser's inventory — reopen it from 'Inventory' above"}">${ko ? "보관함에 저장" : "Save to inventory"}</button>
			<button type="button" class="btn btn-xs btn-ghost border border-base-300" aria-pressed="false" data-craft-alt-toggle title="${ko ? "인게임 Alt 상세 보기처럼 굴린 값 뒤에 범위 — Alt 를 누르고 있어도 됩니다" : "Like the in-game Alt view: value ranges after rolled values — or hold Alt"}">${ko ? "수치 범위 (Alt)" : "Value ranges (Alt)"}</button>
<span class="text-xs text-success" data-craft-export-msg aria-live="polite"></span>
		</div>
		<div class="text-xs text-base-content/60" data-craft-slots>
			${ko ? "접두 " : "Prefixes "}${prefixes}/${max} · ${ko ? "접미 " : "Suffixes "}${suffixes}/${max}@if(craftedCount > 0) · ${ko ? "작업대 옵션 " : "Crafted "}${craftedCount}/${meta != null && meta.multimod() ? 3 : 1}@endif
		</div>
		@if(implicits.stream().anyMatch(l -> "exarch".equals(l.gen()) || "eater".equals(l.gen())))
			<div class="text-xs text-base-content/70" data-craft-dominance="${view.dominance()}">
				@if("exarch".equals(view.dominance()))${ko ? "우세: 작열의 총주교 — 섬뜩한 화폐는 접두만" : "Dominance: Searing Exarch — eldritch currency affects prefixes"}
				@elseif("eater".equals(view.dominance()))${ko ? "우세: 세계 포식자 — 섬뜩한 화폐는 접미만" : "Dominance: Eater of Worlds — eldritch currency affects suffixes"}
				@else${ko ? "우세 없음(두 고정 속성 등급이 같음) — 섬뜩한 화폐를 쓸 수 없습니다" : "No dominance (equal tiers) — eldritch currency cannot be used"}@endif
			</div>
		@endif
		@if(meta != null && (meta.prefixLocked() || meta.suffixLocked() || meta.multimod() || meta.noAttack() || meta.noCaster()))
			<%-- 메타 제작 상태 — 어떤 제약이 걸려 있는지(다시 굴리기가 무엇을 남기는지) --%>
			<div class="flex flex-wrap gap-1" data-craft-meta>
				@if(meta.prefixLocked())<span class="badge badge-sm badge-info badge-outline" title="${ko ? "다시 굴리기 · 소멸 · 정제가 접두를 바꾸지 않고 새 접두도 붙지 않습니다" : "Rerolls, annulment and scouring keep prefixes and add none"}">${ko ? "접두어 변경 불가" : "Prefixes Cannot Be Changed"}</span>@endif
				@if(meta.suffixLocked())<span class="badge badge-sm badge-info badge-outline" title="${ko ? "다시 굴리기 · 소멸 · 정제가 접미를 바꾸지 않고 새 접미도 붙지 않습니다" : "Rerolls, annulment and scouring keep suffixes and add none"}">${ko ? "접미어 변경 불가" : "Suffixes Cannot Be Changed"}</span>@endif
				@if(meta.multimod())<span class="badge badge-sm badge-info badge-outline">${ko ? "작업대 옵션 3개" : "Up to 3 crafted"}</span>@endif
				@if(meta.noAttack())<span class="badge badge-sm badge-info badge-outline">${ko ? "공격 옵션 금지" : "No attack mods"}</span>@endif
				@if(meta.noCaster())<span class="badge badge-sm badge-info badge-outline">${ko ? "주문 옵션 금지" : "No caster mods"}</span>@endif
			</div>
		@endif
	<%-- 직전 단계 변화(C453) — 붙은 옵션은 툴팁에서 강조, 지운 옵션은 툴팁에 없어 여기에 --%>
	@if(!historyRows.isEmpty() && historyRows.get(0).now() && (!historyRows.get(0).added().isEmpty() || !historyRows.get(0).removed().isEmpty()))
		!{ PoeCraft.HistRow lastRow = historyRows.get(0); }
		<div class="text-xs max-w-[24rem] space-y-0.5" data-craft-last-change>
			<span class="text-base-content/70">${ko ? "직전 단계(" + lastRow.no() + ") " + lastRow.label() + ":" : "Last step (" + lastRow.no() + ") " + lastRow.label() + ":"}</span>
			@for(String a : lastRow.added())<div class="text-success wrap-anywhere">+ ${a}</div>@endfor
			@for(String r : lastRow.removed())<div class="text-error wrap-anywhere line-through decoration-1">− ${r}</div>@endfor
		</div>
	@endif
	<%-- 오류는 아이템 카드와 함께 붙어 있게(PoE2 C453c 와 같게) --%>
	@if(error != null)
		<div role="alert" class="alert alert-warning py-2 text-sm scroll-mt-24 craft-error-reveal" data-craft-error>${ko ? error : net.luversof.web.gate.poe.PoeCraftText.en(error)}</div>
	@endif
	</div>
	<section class="min-w-0 space-y-2 max-lg:order-last lg:row-span-2 xl:row-span-1 lg:max-h-[max(26rem,calc(100vh-24rem))] lg:overflow-y-auto lg:pr-1" id="craft-pool" data-craft-pool>
		@if(item.corrupted())
			<%-- 타락한 아이템(C379) — 화폐로는 더 붙지 않는다. 표는 직접 편집(+)용으로 남긴다 --%>
			<h2 class="font-semibold text-sm" data-craft-pool-corrupted>${ko ? "타락한 아이템 — 화폐로는 더 붙지 않습니다" : "Corrupted — no currency can add modifiers"}</h2>
			<p class="text-xs text-base-content/70">${ko ? "아래 + · +분열은 직접 편집(시작 아이템 만들기)용, ☆ 은 계산기 목표(그 등급 이상 — 화폐 단추에 목표 적중 확률)입니다." : "The + / +frac buttons below are for direct editing; ☆ sets a calculator target (that tier or better — hit chance on currency buttons)."}</p>
		@else
			<h2 class="font-semibold text-sm">${poolTitle}</h2>
		@endif
		<%-- 결과 분포(10-21 C533, craftofexile 대량 모의 — 옵션 분포) — 고른 화폐를 지금 아이템에 5천 번 따로 써서 계열마다 붙는 비율 · 1등급 · 옵션 개수.
		     확률 표 위에(10-21 C542 사용자 요청 "결과 분포를 쓰려면 접두 · 접미를 접고 나서 가야 — 동선이 불편"), 화폐를 써도 열린 채로(data-craft-group) --%>
		<details class="rounded-box border border-base-300" data-craft-dist-panel data-craft-group="dist">
			<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "결과 분포 — 대량 모의" : "Result distribution — mass simulation"} <span class="text-xs font-normal text-base-content/70">${ko ? "· 이 화폐를 지금 아이템에 여러 번 쓰면 어떤 옵션이 몇 % 로 붙나" : "· which modifiers this currency gives on the current item, and how often"}</span></summary>
			<div class="px-3 pb-3 space-y-2">
				<div class="flex flex-wrap items-center gap-2 text-xs">
					<select name="distAct" class="select select-xs select-bordered h-7 min-h-0" aria-label="${ko ? "모의할 화폐" : "Currency to simulate"}" data-craft-dist-act>
						<option value="CHAOS">${ko ? "카오스 오브" : "Chaos Orb"}</option>
						<option value="ALCHEMY">${ko ? "연금술의 오브" : "Orb of Alchemy"}</option>
						<option value="EXALT">${ko ? "엑잘티드 오브" : "Exalted Orb"}</option>
						<option value="ALTERATION">${ko ? "변화의 오브" : "Orb of Alteration"}</option>
						<option value="AUGMENTATION">${ko ? "확장의 오브" : "Orb of Augmentation"}</option>
						<option value="REGAL">${ko ? "제왕의 오브" : "Regal Orb"}</option>
						<option value="TRANSMUTE">${ko ? "진화의 오브" : "Orb of Transmutation"}</option>
						<option value="ESSENCE">${ko ? "고른 에센스" : "Picked essence"}</option>
						<option value="FOSSIL">${ko ? "고른 화석" : "Picked fossils"}</option>
						<option value="VEILED_CHAOS">${ko ? "장막의 카오스 오브" : "Veiled Chaos Orb"}</option>
					</select>
					<button type="button" class="btn btn-xs btn-ghost border border-base-300" hx-post="/poe/htmx/craft/dist" hx-include="#craft-emu > form, [data-craft-dist-act]" hx-target="#craft-dist-result" hx-swap="outerHTML" data-craft-dist-run>${ko ? "5천 번 모의" : "Simulate 5,000"}</button>
				</div>
				<div id="craft-dist-result" data-craft-dist></div>
			</div>
		</details>
		<%-- 계열 묶음(10-21 C542) — 조각으로 뗌(craftEmu 의 생성 메서드가 64KB 한도를 넘어 'code too large') --%>
		<%-- 고른 화폐 기준 표(10-22 P3) — 화폐를 고르면 common.ts refreshCraftPool 이 이 칸을 그 수단 기준으로 바꿔 끼운다 --%>
		<div id="craft-pool-groups" class="space-y-2" data-craft-pool-groups="/poe/htmx/craft/action-pool" data-keep-open="data-craft-group">
			@template.poe.htmx.craftPoolGroups(poolGroups = poolGroups, lines = lines, added = added, max = max, prefixes = prefixes, suffixes = suffixes, lineTags = ko ? (view.tagsKo() == null ? java.util.Map.of() : view.tagsKo()) : (view.tags() == null ? java.util.Map.of() : view.tags()), actionLabel = null, actionPicked = false, actionRarity = null, actionMult = java.util.Map.of(), actionError = null, forcedFamilies = java.util.Map.of(), forcedText = null, waysUrl = net.luversof.web.gate.poe.CraftPoolGroups.waysUrl("/poe/htmx/craft/family-ways", item.itemClass(), item.variant(), item.influence(), item.ilvl()))
		</div>
		<p class="text-xs text-base-content/60">
			${ko ? "삿된 화폐의 '레벨 높은 속성' 가중은 공개 수치가 없어 축성된 화석과 같다고(요구 레벨 40 기준 레벨당 ±1%) 가정합니다. 옵션 수(매직 1~2 반반 · 레어 4/5/6 = 8:3:1)는 게임 데이터에 없어 커뮤니티 측정값을 씁니다. 툴팁 수치는 굴린 값(범위는 마우스를 올리면), 확률 표는 범위입니다." : "Foulborn currency's higher-level bias is unpublished; the Sanctified Fossil weighting (±1% per required level from 40) is assumed. Affix counts (magic 1–2 at 50/50, rare 4/5/6 at 8:3:1) are not in the game data; community-measured values are used. Tooltip values are rolled (hover for the range); the odds table shows ranges."}
		</p>
	</section>
	<%-- 단계 기록(C455, PoE2 C453 의 짝) — 단계마다 쓴 화폐 · 그 단계 · 누적 비용 · 변화, 아무 단계로 되돌리기(이 단계 뒤 상태로)
	     변화는 쓴 화폐 칸 아래(10-22 C562 사용자 요청 "단계 기록의 변화가 너무 좁다" — 따로 칸이면 143px), xl 세 칸은 기록이 확률 칸과 남는 폭을 나눈다(minmax(24rem,1fr)) --%>
	<section class="min-w-0 space-y-3 lg:col-start-1 xl:col-start-3 xl:row-start-1 lg:max-h-[max(26rem,calc(100vh-24rem))] lg:overflow-y-auto lg:pr-1" data-craft-history>
		!{ Double histCum = historyRows.isEmpty() ? null : historyRows.get(0).cumChaos(); }
		<h2 class="text-base font-semibold">${ko ? "단계 기록" : "Step history"} <span class="text-xs font-normal text-base-content/70">· ${historyRows.size()}${ko ? "단계" : " steps"}@if(histCum != null) · ${ko ? "누적 " : "total "}${String.format("%.2f", histCum)} c@endif@if(historyDropped > 0) · ${ko ? "앞 " + historyDropped + "단계는 기록 한도로 되돌릴 수 없음" : historyDropped + " oldest steps beyond the undo limit"}@endif</span></h2>
		@if(historyRows.isEmpty())
			<p class="text-sm text-base-content/70" data-craft-history-empty>${ko ? "화폐를 쓰면 단계마다 쓴 화폐 · 비용 · 붙은/지운 옵션이 여기에 쌓이고, 어느 단계로든 되돌릴 수 있습니다." : "Each step's currency, cost and added/removed modifiers build up here, and you can go back to any step."}</p>
		@else
			<div class="overflow-x-auto rounded-box border border-base-300">
				<table class="table table-xs">
					<thead class="sticky top-0 z-10 bg-base-200"><tr><th>#</th><th>${ko ? "쓴 화폐 · 변화" : "Currency · change"}</th><th class="text-right" title="${ko ? "이 단계 비용 · 아래 작은 글씨는 여기까지 누적(c)" : "This step · total so far below (c)"}">${ko ? "비용 · 누적" : "Cost · total"}</th><th class="text-right">${ko ? "되돌리기" : "Go back"}</th></tr></thead>
					<tbody>
						@for(PoeCraft.HistRow r : historyRows)
							<tr data-craft-history-row="${r.no()}" class="${r.now() ? "bg-primary/10" : null}">
								<td class="font-mono">${r.no()}</td>
								<td class="min-w-0 wrap-anywhere"><div class="font-medium break-keep">${r.label()}</div>
									<div class="mt-0.5" data-craft-history-change>
										@for(String a : r.added())<div class="text-success">+ ${a}</div>@endfor
										@for(String rm : r.removed())<div class="text-error line-through decoration-1">− ${rm}</div>@endfor
										@if(r.note() != null)<div class="text-base-content/60">${r.note()}</div>@endif
									</div>
								</td>
								<td class="text-right font-mono whitespace-nowrap">
									<div data-craft-history-cost>@if(r.chaos() != null)${String.format("%.2f", r.chaos())}@else-@endif</div>
									<div class="text-[10px] text-base-content/60" data-craft-history-cum>Σ @if(r.cumChaos() != null)${String.format("%.2f", r.cumChaos())}@else-@endif</div>
								</td>
								<td class="text-right whitespace-nowrap">
									@if(r.now())
										<span class="badge badge-primary badge-xs" data-craft-history-now>${ko ? "지금" : "now"}</span>
									@elseif(r.undoTo() != null)
										<button type="button" name="action" value="UNDO_TO:${r.undoTo()}" class="btn btn-xs btn-ghost border border-base-300 whitespace-nowrap" hx-post="/poe/htmx/craft" hx-target="#craft-emu" hx-swap="outerHTML" hx-include="#craft-emu > form" data-craft-action="UNDO_TO:${r.undoTo()}" title="${ko ? "이 단계(" + r.no() + ")를 마친 상태로 되돌립니다 — 그 뒤 단계 · 쓴 화폐는 빠집니다" : "Go back to the state after step " + r.no() + " — later steps and their currency are removed"}">${ko ? "여기로" : "Go here"}</button>
									@endif
								</td>
							</tr>
						@endfor
						@if(historyStartUndo != null)
							<tr data-craft-history-row="0">
								<td class="font-mono">0</td>
								<td colspan="2" class="text-base-content/70">${historyDropped > 0 ? (ko ? "기록에 남은 가장 오래된 상태" : "Oldest state in the history") : (ko ? "시작 — 새 아이템" : "Start — new item")}</td>
								<td class="text-right"><button type="button" name="action" value="UNDO_TO:${historyStartUndo}" class="btn btn-xs btn-ghost border border-base-300 whitespace-nowrap" hx-post="/poe/htmx/craft" hx-target="#craft-emu" hx-swap="outerHTML" hx-include="#craft-emu > form" data-craft-action="UNDO_TO:${historyStartUndo}">${ko ? "여기로" : "Go here"}</button></td>
							</tr>
						@endif
					</tbody>
				</table>
			</div>
		@endif
		@if(spent != null && !spent.lines().isEmpty())
			<%-- 쓴 화폐 · 총비용(C365) — 아이템을 바꾼 단계마다 센다(되돌리면 빠짐, 머리카락은 남음). 열 순서 = 개수 · 단가 · 합계(계산 흐름) --%>
			!{
				// 한국어 이름 — 기본 화폐 버튼 · 화폐 설명 · 에센스 · 화석 · 공명기(없으면 영어)
				java.util.Map<String, String> spKo = new java.util.HashMap<>();
				for (Btn b : btns) spKo.put(b.en(), b.ko());
				spKo.put("Hinekora's Lock", "히네코라의 머리카락");
				spKo.put("Divine Orb", "신성한 오브");
				spKo.put("Blessed Orb", "축복의 오브");
				if (options != null) {
					if (options.currencies() != null) for (PoeCraft.CurrencyInfo ci : options.currencies()) spKo.put(ci.name(), ci.nameKo());
					if (options.fossils() != null) for (PoeCraft.Fossil f : options.fossils()) spKo.put(f.name(), f.nameKo());
					if (options.resonators() != null) for (PoeCraft.Resonator r : options.resonators()) spKo.put(r.name(), r.nameKo());
				}
				for (PoeCraft.Essence e : essences) spKo.put(e.name(), e.nameKo());
				java.util.function.Function<Double, String> fmt = v -> v == null ? "—" : v >= 100 ? String.format("%,.0f", v) : v >= 1 ? String.format("%.1f", v) : String.format("%.3f", v);
			}
			<details class="rounded-box border border-base-300 bg-base-100" data-craft-spent open>
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "쓴 화폐" : "Currency spent"} <span class="text-xs font-normal text-base-content/70" data-craft-spent-total>· ${ko ? "합계 " : "total "}${fmt.apply(spent.totalChaos())}c@if(spent.divineChaos() != null && spent.divineChaos() > 0) (${fmt.apply(spent.totalChaos() / spent.divineChaos())} div)@endif@if(spent.missing() > 0) · ${ko ? "시세 없는 것 " + spent.missing() + "개 제외" : spent.missing() + " without price excluded"}@endif</span></summary>
				<div class="px-3 pb-3 overflow-x-auto">
					<table class="table table-xs">
						<thead><tr><th>${ko ? "화폐" : "Currency"}</th><th class="text-right">${ko ? "개수" : "Count"}</th><th class="text-right">${ko ? "단가(c)" : "Unit (c)"}</th><th class="text-right">${ko ? "합계(c)" : "Subtotal (c)"}</th></tr></thead>
						<tbody>
							@for(PoeCraft.SpentLine sl : spent.lines())
								!{ String sk = spKo.get(sl.name()); }
								<tr data-craft-spent-line="${sl.name()}">
									<td class="wrap-anywhere">${ko ? (sl.nameKo() != null ? sl.nameKo() : sk != null ? sk : sl.name()) : sl.name()}</td>
									<td class="text-right font-mono">${sl.count() == Math.rint(sl.count()) ? String.valueOf((long) sl.count()) : String.format("%.1f", sl.count())}</td>
									<td class="text-right font-mono">${fmt.apply(sl.unitChaos())}</td>
									<td class="text-right font-mono">${fmt.apply(sl.subtotalChaos())}</td>
								</tr>
							@endfor
						</tbody>
					</table>
					<p class="mt-1 text-xs text-base-content/70">${ko ? "poe.ninja 시세" : "poe.ninja prices"}@if(spent.league() != null) · ${spent.league()}@endif · ${ko ? "되돌리면 그 단계는 빠지고, 히네코라의 머리카락은 남습니다" : "undone steps are removed; Hinekora's Lock stays"}</p>
				</div>
			</details>
		@endif
	</section>
	</div>

</div>
