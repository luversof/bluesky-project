@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe2.dto.Poe2Craft

@param Poe2Craft.View view
@param String error = null
@param String lastAction = null
@param java.util.List<String> added = java.util.List.of()
@param String stateJson = "{}"
@param String historyJson = "[]"
@param int historySize = 0
@param java.util.List<Poe2Craft.PoolLine> pool = java.util.List.of()
@param java.util.List<Poe2Craft.Omen> omens = java.util.List.of()
@param java.util.List<String> activeOmens = java.util.List.of()
@param String tier = ""
@param String classLabel = ""
@param java.util.List<Poe2Craft.Currency> currencies = java.util.List.of()
@param java.util.List<Poe2Craft.EssenceOption> essences = java.util.List.of()
@param java.util.List<Poe2Craft.Bone> bones = java.util.List.of()
@param String selEssence = ""
@param String selBone = ""
@param String foresee = ""
@param Poe2Craft.View preview = null
@param String previewAction = null
@param String previewArg = null
@param String previewOmens = null
@param String spentJson = "[]"
@param Poe2Craft.SpentResult spent = null
@param java.util.List<Poe2Craft.HistRow> historyRows = java.util.List.of()
@param Integer historyStartUndo = null
@param int historyDropped = 0
@param net.luversof.web.gate.poe.dto.PoeCraft.OptimizeNext recommend = null
@param String optJob = null
@param String rollsJson = "{}"
@param java.util.Map<String, Long> rolls = java.util.Map.of()
@param String rollHistJson = "[]"
@param String stashJson = ""
@param String stashRollsJson = "{}"
@param Poe2Craft.View stashView = null
@param String recombChance = ""
@param java.util.List<String> recombLSel = java.util.List.of()
@param java.util.List<String> recombRSel = java.util.List.of()
@param java.util.Map<String, Long> stashRolls = java.util.Map.of()

<%-- PoE2 제작 에뮬레이터 결과 구역(10-15 C351) — PoE1 에뮬레이터와 같은 툴팁 · 바꿔 끼우기. 화폐 등급(기본 · 상급 · 완벽 = 최소 옵션 레벨) · 징조 체크(쓰면 사라짐). --%>
!{
	boolean ko = PoeText.isKorean();
	Poe2Craft.Item item = view.item();
	String rarity = item.rarity() == null ? "NORMAL" : item.rarity();
	String rarCls = switch (rarity) { case "MAGIC" -> "poe-rar-magic"; case "RARE" -> "poe-rar-rare"; default -> "poe-rar-white"; };
	String headColor = switch (rarity) { case "MAGIC" -> "#8888ff"; case "RARE" -> "#ffff77"; default -> "#c8c8c8"; };
	java.util.List<Poe2Craft.Line> lines = view.lines() == null ? java.util.List.of() : view.lines();
	long prefixes = lines.stream().filter(l -> "prefix".equals(l.gen())).count();
	long suffixes = lines.stream().filter(l -> "suffix".equals(l.gen())).count();
	java.util.function.Function<java.util.List<String>, java.util.List<String>> nz = x -> x == null ? java.util.List.of() : x;
	java.util.function.Function<String, String> nzsF = x -> x == null ? "" : x;
	String magicName = classLabel;
	if ("MAGIC".equals(rarity)) {
		String pre = lines.stream().filter(l -> "prefix".equals(l.gen())).map(l -> ko && l.nameKo() != null ? l.nameKo() : l.name()).findFirst().orElse(null);
		String suf = lines.stream().filter(l -> "suffix".equals(l.gen())).map(l -> ko && l.nameKo() != null ? l.nameKo() : l.name()).findFirst().orElse(null);
		magicName = (pre != null ? pre + " " : "") + classLabel + (suf != null ? " " + suf : "");
	}
	boolean rare = "RARE".equals(rarity);
	// 영어 화면이면 엔진 문구를 영어로(C447 UX QA)
	java.util.Map<String, String> blocked = view.blocked() == null ? java.util.Map.of() : ko ? view.blocked() : net.luversof.web.gate.poe2.Poe2CraftText.en(view.blocked());
	java.util.function.BiFunction<String, String, Poe2Craft.Currency> cur = (act, t) -> currencies.stream().filter(c -> c.action().equals(act) && nzsF.apply(c.tier()).equals(t)).findFirst()
		.orElse(currencies.stream().filter(c -> c.action().equals(act) && nzsF.apply(c.tier()).isEmpty()).findFirst().orElse(null));
	record Btn(String action, String ko, String en, boolean tiered) {}
	// 기폭제(C386) — 퀄리티 종류 · 양, 수치가 오르는 옵션 id, 이 아이템에 맞는 기폭제(반지 · 목걸이 = 보통, 주얼 = 제련된)
	String qKey = null;
	int qAmt = 0;
	if (item.quality() != null && item.quality().lastIndexOf(':') > 0) {
		qKey = item.quality().substring(0, item.quality().lastIndexOf(':'));
		try { qAmt = Integer.parseInt(item.quality().substring(item.quality().lastIndexOf(':') + 1)); } catch (NumberFormatException e) { qAmt = 0; }
	}
	java.util.List<String> boostedIds = view.boosted() == null ? java.util.List.of() : view.boosted();
	String pk = item.poolKey() == null ? "" : item.poolKey();
	boolean jewelleryItem = pk.startsWith("Ring|") || pk.startsWith("Amulet|");
	boolean jewelItem = pk.startsWith("Jewel|");
	java.util.List<Poe2Craft.Currency> catalysts = currencies.stream().filter(c -> "CATALYST".equals(c.action()) && c.tier() != null && (c.tier().startsWith("jewel:") ? jewelItem : jewelleryItem)).toList();
	Poe2Craft.Currency qInfo = null;
	for (Poe2Craft.Currency c : currencies) if ("CATALYST".equals(c.action()) && c.tier() != null && c.tier().equals(qKey)) qInfo = c;
	java.util.List<Btn> btns = java.util.List.of(
		new Btn("TRANSMUTE", "진화의 오브", "Orb of Transmutation", true),
		new Btn("AUGMENTATION", "확장의 오브", "Orb of Augmentation", true),
		new Btn("REGAL", "제왕의 오브", "Regal Orb", true),
		new Btn("ALCHEMY", "연금술의 오브", "Orb of Alchemy", false),
		new Btn("EXALT", "엑잘티드 오브", "Exalted Orb", true),
		new Btn("CHAOS", "카오스 오브", "Chaos Orb", true),
		new Btn("ANNUL", "소멸의 오브", "Orb of Annulment", false),
		new Btn("FRACTURE", "분열의 오브", "Fracturing Orb", false));
	// 제작 징조 묶음(poe2db 문구) — 다음 해당 화폐 한 번에 쓰이고 사라진다
	java.util.Map<String, Poe2Craft.Omen> omenByKey = new java.util.HashMap<>();
	for (Poe2Craft.Omen o : omens) omenByKey.put(o.key(), o);
	java.util.Map<String, String[]> omenGroups = new java.util.LinkedHashMap<>();
	omenGroups.put(ko ? "엑잘티드(찬미)" : "Exalted", new String[] {"SinistralExaltation", "DextralExaltation", "GreaterExaltation", "HomogenisingExaltation", "CatalysingExaltation"});
	omenGroups.put(ko ? "제왕(대관식)" : "Regal", new String[] {"SinistralCoronation", "DextralCoronation", "HomogenisingCoronation"});
	omenGroups.put(ko ? "연금술" : "Alchemy", new String[] {"SinistralAlchemy", "DextralAlchemy"});
	omenGroups.put(ko ? "카오스" : "Chaos", new String[] {"Whittling", "SinistralErasure", "DextralErasure"});
	omenGroups.put(ko ? "소멸" : "Annulment", new String[] {"SinistralAnnulment", "DextralAnnulment", "GreaterAnnulment"});
	omenGroups.put(ko ? "완벽 · 타락 에센스(결정화)" : "Perfect / corrupted essence", new String[] {"SinistralCrystallisation", "DextralCrystallisation"});
	// 바알 오브(C376) — 타락의 징조 = "변화 없음" 결과를 없앤다
	omenGroups.put(ko ? "바알 오브" : "Vaal Orb", new String[] {"Corruption"});
	// 축성의 징조(C390) — 다음 신성한 오브(레어)가 축성: 옵션마다 0.78~1.22배, 이후 더 못 바꿈
	omenGroups.put(ko ? "신성한 오브" : "Divine Orb", new String[] {"Sanctification"});
	// 재조합의 징조(C407) — 다음 재조합이 행운(두 번 굴려 좋은 쪽)
	omenGroups.put(ko ? "재조합기" : "Recombinator", new String[] {"Recombination"});
	omenGroups.put(ko ? "훼손(뼈)" : "Desecration (bones)", new String[] {"SinistralNecromancy", "DextralNecromancy", "Sovereign", "Liege", "Blackblooded", "Light", "AbyssalEchoes", "Putrefaction"});
	java.util.Map<String, String> omenDesc = java.util.Map.ofEntries(
		java.util.Map.entry("SinistralExaltation", ko ? "다음 엑잘티드 오브가 접두어만 추가" : "next Exalted Orb adds only prefixes"),
		java.util.Map.entry("DextralExaltation", ko ? "다음 엑잘티드 오브가 접미어만 추가" : "next Exalted Orb adds only suffixes"),
		java.util.Map.entry("GreaterExaltation", ko ? "다음 엑잘티드 오브가 옵션 둘 추가" : "next Exalted Orb adds two modifiers"),
		java.util.Map.entry("HomogenisingExaltation", ko ? "다음 엑잘티드 오브가 있는 옵션과 같은 유형을 추가" : "next Exalted Orb adds a modifier of the same type as an existing one"),
		java.util.Map.entry("SinistralCoronation", ko ? "다음 제왕의 오브가 접두어만 추가" : "next Regal Orb adds only prefixes"),
		java.util.Map.entry("DextralCoronation", ko ? "다음 제왕의 오브가 접미어만 추가" : "next Regal Orb adds only suffixes"),
		java.util.Map.entry("HomogenisingCoronation", ko ? "다음 제왕의 오브가 있는 옵션과 같은 유형을 추가" : "next Regal Orb adds a modifier of the same type as an existing one"),
		java.util.Map.entry("SinistralAlchemy", ko ? "다음 연금술의 오브가 접두어 최대" : "next Orb of Alchemy gives the maximum prefixes"),
		java.util.Map.entry("DextralAlchemy", ko ? "다음 연금술의 오브가 접미어 최대" : "next Orb of Alchemy gives the maximum suffixes"),
		java.util.Map.entry("Whittling", ko ? "다음 카오스 오브가 가장 낮은 레벨 옵션을 지움" : "next Chaos Orb removes the lowest level modifier"),
		java.util.Map.entry("SinistralErasure", ko ? "다음 카오스 오브가 접두어만 지움" : "next Chaos Orb removes only prefixes"),
		java.util.Map.entry("DextralErasure", ko ? "다음 카오스 오브가 접미어만 지움" : "next Chaos Orb removes only suffixes"),
		java.util.Map.entry("SinistralAnnulment", ko ? "다음 소멸의 오브가 접두어만 지움" : "next Orb of Annulment removes only prefixes"),
		java.util.Map.entry("DextralAnnulment", ko ? "다음 소멸의 오브가 접미어만 지움" : "next Orb of Annulment removes only suffixes"),
		java.util.Map.entry("GreaterAnnulment", ko ? "다음 소멸의 오브가 옵션 둘을 지움" : "next Orb of Annulment removes two modifiers"),
		java.util.Map.entry("SinistralCrystallisation", ko ? "다음 완벽 · 타락 에센스가 접두어만 지움" : "next Perfect or Corrupted Essence removes only prefixes"),
		java.util.Map.entry("DextralCrystallisation", ko ? "다음 완벽 · 타락 에센스가 접미어만 지움" : "next Perfect or Corrupted Essence removes only suffixes"),
		// 빠져 있던 설명 셋(C414 — CurrencyItems 설명: 바알 "always result in change" · 신성한 오브 "used on a Rare item will Sanctify it" · 엑잘 "consume all Catalyst Quality")
		java.util.Map.entry("Corruption", ko ? "다음 바알 오브가 반드시 변화를 일으킴" : "next Vaal Orb will always result in change"),
		java.util.Map.entry("Sanctification", ko ? "레어 아이템에 쓴 다음 신성한 오브가 축성" : "next Divine Orb used on a Rare item will Sanctify it"),
		java.util.Map.entry("CatalysingExaltation", ko ? "다음 엑잘티드 오브가 기폭제 퀄리티를 모두 소모해 그 종류 속성 확률을 올림" : "next Exalted Orb consumes all Catalyst Quality to raise the chance of that modifier type"),
		java.util.Map.entry("Recombination", ko ? "다음 재조합의 성공이 행운(두 번 굴려 좋은 쪽)" : "next Recombination is Lucky (rolled twice, better kept)"),
		java.util.Map.entry("SinistralNecromancy", ko ? "다음 훼손이 접두어만 추가" : "next desecration adds only prefixes"),
		java.util.Map.entry("DextralNecromancy", ko ? "다음 훼손이 접미어만 추가" : "next desecration adds only suffixes"),
		java.util.Map.entry("Sovereign", ko ? "다음 무기 · 장신구 훼손이 울라만 옵션 보장" : "next weapon or jewellery desecration guarantees an Ulaman modifier"),
		java.util.Map.entry("Liege", ko ? "다음 무기 · 장신구 훼손이 아마나무 옵션 보장" : "next weapon or jewellery desecration guarantees an Amanamu modifier"),
		java.util.Map.entry("Blackblooded", ko ? "다음 무기 · 장신구 훼손이 쿠르갈 옵션 보장" : "next weapon or jewellery desecration guarantees a Kurgal modifier"),
		java.util.Map.entry("Light", ko ? "다음 소멸의 오브가 훼손된 옵션만 지움" : "next Orb of Annulment removes only Desecrated modifiers"),
		java.util.Map.entry("AbyssalEchoes", ko ? "다음 드러내기에서 셋을 한 번 다시 볼 수 있음" : "next reveal can reroll the options once"),
		java.util.Map.entry("Putrefaction", ko ? "다음 훼손이 모든 옵션을 미공개로 바꾸고(최대 6) 타락" : "next desecration replaces all modifiers with up to six unrevealed ones and corrupts"));
}
<%-- C440 UX QA: 누른 단추가 든 상자(data-swap-anchor-key)를 제자리에 · 열어 둔 묶음 유지 · 오류가 헤더 밑이면 보이게 --%>
<div id="craft2-emu" data-craft-arm-scope class="space-y-3" data-craft-rarity="${rarity}" data-no-success-toast data-swap-anchor="#craft2-emu [data-craft-actions]" data-keep-open="data-craft-group" data-error-into-view=".craft-error-reveal">
	<%-- hx-sync(C396) — 징조 체크의 다시 그리기와 화폐 요청이 겹치면 나중 것이 앞 것을 대체(늦게 온 다시 그리기가 엑잘 결과를 덮던 경합) --%>
	<form hx-post="/poe2/htmx/craft" hx-target="#craft2-emu" hx-swap="outerHTML" hx-sync="this:replace" class="space-y-2 min-w-0" hx-include="#craft2-opt-result input[name=optJob]" data-craft-toolbar>
		<input type="hidden" name="poolKey" value="${item.poolKey()}">
		<input type="hidden" name="ilvl" value="${item.ilvl()}">
		<input type="hidden" name="state" value="${stateJson}">
		<input type="hidden" name="history" value="${historyJson}">
		<input type="hidden" name="foresee" value="${foresee}">
		<input type="hidden" name="previewArg" value="${previewArg}">
		<input type="hidden" name="previewOmens" value="${previewOmens}">
		<input type="hidden" name="spent" value="${spentJson}">
		<input type="hidden" name="rolls" value="${rollsJson}">
		<input type="hidden" name="rollHistory" value="${rollHistJson}">
		<input type="hidden" name="stash" value="${stashJson}">
		<input type="hidden" name="stashRolls" value="${stashRollsJson}">

		<%-- 등급 + 기본 화폐(C436 검토) — 등급을 바꾸면 이 구역만 다시 그린다(열린 묶음 · 강조 · 미리보기 유지) --%>
		<div class="space-y-3" data-craft-basic>
		<div class="flex flex-wrap items-center gap-3 text-sm" data-craft-tier hx-post="/poe2/htmx/craft" hx-trigger="change" hx-include="closest form" hx-vals='{"action":"VIEW"}' hx-target="closest [data-craft-basic]" hx-select="[data-craft-basic]" hx-select-oob="#craft2-pool" hx-swap="outerHTML" hx-sync="closest form:replace">
			<span class="text-xs text-base-content/70">${ko ? "화폐 등급" : "Currency tier"}</span>
			<label class="flex items-center gap-1"><input type="radio" name="tier" value="" id="craft2-tier-normal" class="radio radio-xs" checked="${tier.isEmpty()}">${ko ? "기본" : "Normal"}</label>
			<label class="flex items-center gap-1" title="${ko ? "진화 · 확장 옵션 레벨 44 이상, 제왕 · 엑잘 · 카오스 35 이상" : "Transmute/Augment level 44+, Regal/Exalt/Chaos 35+"}"><input type="radio" name="tier" value="greater" id="craft2-tier-greater" class="radio radio-xs" checked="${"greater".equals(tier)}">${ko ? "상위" : "Greater"}</label>
			<label class="flex items-center gap-1" title="${ko ? "진화 · 확장 옵션 레벨 70 이상, 제왕 · 엑잘 · 카오스 50 이상" : "Transmute/Augment level 70+, Regal/Exalt/Chaos 50+"}"><input type="radio" name="tier" value="perfect" id="craft2-tier-perfect" class="radio radio-xs" checked="${"perfect".equals(tier)}">${ko ? "완벽한" : "Perfect"}</label>
		</div>

		<%-- 같은 화폐의 좌 · 우 징조를 함께 걸면 인게임 오류(C437) — 버튼을 미리 끔(C439 UX QA) --%>
		!{
			java.util.Map<String, String> pairOf = java.util.Map.of("REGAL", "Coronation", "EXALT", "Exaltation", "CHAOS", "Erasure", "ANNUL", "Annulment", "ALCHEMY", "Alchemy", "DESECRATE", "Necromancy");
			java.util.function.Function<String, String> omenWhy = act -> { String pk2 = pairOf.get(act); return pk2 != null && activeOmens.contains("Sinistral" + pk2) && activeOmens.contains("Dextral" + pk2) ? (ko ? "유효하지 않은 징조 조합입니다 — 같은 화폐의 좌 · 우 징조를 함께 걸었습니다" : "This combination of Omens is not valid — Sinistral and Dextral for the same currency") : null; };
		}
		<div class="grid grid-cols-4 min-[380px]:grid-cols-5 gap-1.5 sm:flex sm:flex-wrap sm:items-stretch" data-craft-actions data-swap-anchor-key="actions">
			@for(Btn b : btns)
				!{ String why = blocked.get(b.action()) != null ? blocked.get(b.action()) : omenWhy.apply(b.action()) != null ? omenWhy.apply(b.action()) : b.tiered() && !tier.isEmpty() ? blocked.get(b.action() + ":" + tier) : null; Poe2Craft.Currency c = cur.apply(b.action(), b.tiered() ? tier : ""); String label = c == null ? (ko ? b.ko() : b.en()) : (ko ? c.nameKo() : c.name()); }
				<button type="submit" name="action" value="${b.action()}" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 ${recommend != null && recommend.actionId() != null && (recommend.actionId().equals(b.action()) || recommend.actionId().startsWith(b.action() + ":")) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""} ${b.action().equals(lastAction) ? "btn-primary" : "btn-ghost border border-base-300"}" data-craft-action="${b.action()}" disabled="${why != null}" data-craft-blocked="${why}" title="${label + (why != null ? " — " + why : "")}">
					@if(c != null)<img src="/poe-data/poe2/icons/currency/${c.slug().toLowerCase()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">@endif
					<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${label}</span>
				</button>
			@endfor
			<%-- 신성 · 바알 · 머리카락 · 되돌리기 · 새 아이템도 같은 줄(C454 — CoE 처럼 제작 수단은 위 한 줄) --%>
			<span class="hidden sm:block w-px self-stretch bg-base-300 mx-1" aria-hidden="true"></span>
			<button type="submit" name="action" value="UNDO" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 btn-outline" data-craft-action="UNDO" disabled="${historySize == 0}" title="${ko ? "한 단계 되돌리기 — 여러 단계는 아래 단계 기록의 '여기로'" : "Undo one step — use 'Go here' in the step history for more"}"><span class="text-lg leading-none" aria-hidden="true">↶</span><span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? "되돌리기" : "Undo"} (${historySize})</span></button>
			<button type="submit" name="action" value="RESET" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 btn-ghost border border-base-300 ${recommend != null && ("NEW_BASE".equals(recommend.actionId()) || "FALLBACK".equals(recommend.actionId())) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-craft-action="RESET" title="${ko ? "새 일반 아이템 — 기록 · 쓴 화폐도 처음부터" : "New normal item — history and spending start over"}"><span class="text-lg leading-none" aria-hidden="true">↺</span><span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? "새 일반 아이템" : "New normal item"}</span></button>
			<button type="submit" name="action" value="VAAL" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 btn-ghost border border-base-300" data-craft-action="VAAL" disabled="${item.corrupted() || item.sanctified()}" title="${ko ? "바알 오브 — 타락시키며 결과 중 하나(변화 없음 · 옵션 1~3개 무작위 · 타락 인챈트 · 부위 특수). 타락하면 더는 못 바꿉니다" : "Vaal Orb — corrupts with one outcome (none, 1–3 randomized, enchantment, slot special). Corrupted items cannot be modified"}"><img src="/poe-data/poe2/icons/currency/currencycorrupt.png" alt="" class="w-7 h-7 object-contain" loading="lazy"><span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? "바알 오브" : "Vaal Orb"}</span></button>
			<button type="submit" name="action" value="DIVINE" class="btn btn-sm h-auto min-h-12 w-full sm:w-[4.75rem] flex-col gap-0.5 px-1 py-1 btn-ghost border border-base-300" data-craft-action="DIVINE" disabled="${item.corrupted() || item.sanctified() || lines.stream().allMatch(Poe2Craft.Line::fractured)}" title="${ko ? "신성한 오브 — 옵션 수치를 다시 굴립니다(분열 옵션 제외). 되돌리기로 돌아가지 않습니다" : "Divine Orb — rerolls modifier values (not fractured). Not undoable"}"><img src="/poe-data/poe2/icons/currency/currencymodvalues.png" alt="" class="w-7 h-7 object-contain" loading="lazy"><span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? "신성한 오브" : "Divine Orb"}</span></button>
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
					<span class="font-semibold">${ko ? "최적 조합 다음 수단: " : "Optimal plan next: "}${recommend.action()}</span>
					<span class="text-base-content/70"> · ${recommend.state()}@if(recommend.remaining() != null) · ${ko ? "남은 기대 " : "cost to go "}${String.format(recommend.remaining() >= 1 ? "%,.1f" : "%,.3f", recommend.remaining())}@endif</span>
				@endif
			</div>
		@endif
		<%-- 꺼진 단추 이유(C444 UX QA — title 은 마우스를 올려야 보여 터치 · 좁은 폭에선 볼 방법이 없었다) --%>
		!{
			java.util.List<String> offWhy = new java.util.ArrayList<>();
			for (Btn ob : btns) {
				String ow = blocked.get(ob.action()) != null ? blocked.get(ob.action()) : omenWhy.apply(ob.action()) != null ? omenWhy.apply(ob.action()) : ob.tiered() && !tier.isEmpty() ? blocked.get(ob.action() + ":" + tier) : null;
				if (ow != null) {
					Poe2Craft.Currency oc = cur.apply(ob.action(), ob.tiered() ? tier : "");
					offWhy.add((oc == null ? (ko ? ob.ko() : ob.en()) : (ko ? oc.nameKo() : oc.name())) + " — " + ow);
				}
			}
		}
		@if(!offWhy.isEmpty())
			<details class="text-xs text-base-content/70" data-craft-group="blocked" data-craft-blocked-list>
				<summary class="cursor-pointer select-none">${ko ? "꺼진 화폐 " + offWhy.size() + "개 — 이유 보기" : offWhy.size() + " currencies disabled — why?"}</summary>
				<ul class="mt-1 space-y-0.5 list-disc pl-5">
					@for(String w : offWhy)
						<li class="wrap-anywhere">${w}</li>
					@endfor
				</ul>
			</details>
		@endif
		</div>

		<%-- 재조합기(C407) — 같은 종류 매직 · 레어 둘(지금 아이템 + 보관한 재료)에서 고른 속성으로 레어 하나. 성공 확률은 공식이 비공개라 인게임 화면 값을 넣는다 --%>
		!{
			Poe2Craft.Item st = stashView == null ? null : stashView.item();
			java.util.List<Poe2Craft.Line> stLines = stashView == null || stashView.lines() == null ? java.util.List.of() : stashView.lines();
			boolean curRecombOk = ("MAGIC".equals(rarity) || rare) && !item.corrupted() && !item.sanctified() && !jewelItem;
			String stRar = st == null || st.rarity() == null ? "NORMAL" : st.rarity();
			boolean stRecombOk = st != null && ("MAGIC".equals(stRar) || "RARE".equals(stRar)) && !st.corrupted() && !st.sanctified();
			boolean sameClass = st != null && pk.split("[|]")[0].equals(nzsF.apply(st.poolKey()).split("[|]")[0]);
			String recombWhy = st == null ? (ko ? "보관한 재료가 없습니다" : "No stored item")
				: !curRecombOk || !stRecombOk ? (ko ? "아이템을 재조합기에서 사용할 수 없습니다" : "Item cannot be used in the Recombinator")
				: !sameClass ? (ko ? "같은 종류의 아이템만 재조합할 수 있습니다" : "Items must be of the same class") : null;
		}
		<%-- 둘째 줄(C454): 에센스 · 훼손 · 기폭제 · 재조합기 · 직접 편집 · 징조 — 누르면 이 줄 바로 아래에 그 묶음을 펼친다(여럿 열어 둘 수 있음 — 징조 + 에센스처럼 함께 쓰는 수단) --%>
		<%-- 최적 조합 다음 수단이 접힌 칸 안이면 그 칸 단추도 테두리(C464b) --%>
		!{ String recId = recommend == null || recommend.actionId() == null ? "" : recommend.actionId(); String[] recParts = recId.split(":", -1); String recGroup = recId.startsWith("ESSENCE") ? "essence" : (recId.startsWith("DESECRATE") || recId.startsWith("REVEAL")) ? "bone" : recId.startsWith("RECOMB") ? "recomb" : recParts.length > 2 && !recParts[2].isEmpty() ? "omen" : ""; }
		<%-- 묶음 켜고 끄기(C478 — PoE1 C476 의 짝): 에센스 · 뼈 · 징조가 모두 엔진에 막히면(서버가 옵션마다 시험 적용한 이유) 그 묶음 단추도 끈다 --%>
		!{
			java.util.function.Function<java.util.List<String>, String> allWhy2 = ks -> { String w = null; for (String k : ks) { String r = blocked.get(k); if (r == null) return null; if (w == null) w = r; } return w; };
			String essWhy2 = allWhy2.apply(essences.stream().map(e -> "ESSENCE:" + e.slug()).toList());
			String boneWhy2 = allWhy2.apply(bones.stream().map(b -> "DESECRATE:" + b.slug()).toList());
			java.util.List<String> omenKeys2 = new java.util.ArrayList<>();
			omenGroups.values().forEach(arr -> { for (String k : arr) { if (!"Recombination".equals(k)) omenKeys2.add("OMEN:" + k); } });
			String omenWhy2 = null; // 징조는 언제든 켜 둘 수 있어 묶음 단추는 끄지 않는다(옵션은 흐림 + 이유)
			java.util.Map<String, String> groupWhy2 = new java.util.HashMap<>();
			groupWhy2.put("essence", essWhy2); groupWhy2.put("bone", boneWhy2); groupWhy2.put("omen", omenWhy2);
		}
		<div class="flex flex-wrap items-center gap-1.5" data-craft-means data-toggle-exclusive role="group" aria-label="${ko ? "다른 제작 수단" : "Other crafting methods"}">
			@if(!essences.isEmpty())
			<button type="button" class="btn btn-sm ${"essence".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft2-emu details[data-craft-group="essence"]' aria-expanded="${"ESSENCE".equals(lastAction) ? "true" : "false"}" data-craft-means-open="essence" disabled="${groupWhy2.get("essence") != null}" data-craft-means-blocked="${groupWhy2.get("essence")}" title="${groupWhy2.get("essence")}">${ko ? "에센스" : "Essences"}</button>
			@endif
			@if(!bones.isEmpty())
			<button type="button" class="btn btn-sm ${"bone".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft2-emu details[data-craft-group="bone"]' aria-expanded="${"DESECRATE".equals(lastAction) ? "true" : "false"}" data-craft-means-open="bone" disabled="${groupWhy2.get("bone") != null}" data-craft-means-blocked="${groupWhy2.get("bone")}" title="${groupWhy2.get("bone")}">${ko ? "훼손(뼈)" : "Desecrate"}</button>
			@endif
			@if(!catalysts.isEmpty())
			<button type="button" class="btn btn-sm ${"catalyst".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft2-emu details[data-craft-group="catalyst"]' aria-expanded="${lastAction != null && lastAction.startsWith("CATALYST") ? "true" : "false"}" data-craft-means-open="catalyst">${ko ? "기폭제" : "Catalysts"}</button>
			@endif
			<button type="button" class="btn btn-sm ${"recomb".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft2-emu details[data-craft-group="recomb"]' aria-expanded="${st != null || (lastAction != null && java.util.Set.of("STASH", "SWAP", "UNSTASH", "RECOMBINE").contains(lastAction)) ? "true" : "false"}" data-craft-means-open="recomb">${ko ? "재조합기" : "Recombinator"}@if(st != null) <span class="badge badge-xs">1</span>@endif</button>
			<button type="button" class="btn btn-sm ${"manual".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft2-emu details[data-craft-group="manual"]' aria-expanded="${lastAction != null && lastAction.startsWith("MANUAL") ? "true" : "false"}" data-craft-means-open="manual">${ko ? "직접 편집" : "Edit directly"}</button>
			<button type="button" class="btn btn-sm ${"omen".equals(recGroup) ? "ring-2 ring-primary ring-offset-1 ring-offset-base-100" : ""}" data-toggle-active-class="btn-primary" data-toggle-details='#craft2-emu details[data-craft-group="omen"]' aria-expanded="${!activeOmens.isEmpty() ? "true" : "false"}" data-craft-means-open="omen" disabled="${groupWhy2.get("omen") != null}" data-craft-means-blocked="${groupWhy2.get("omen")}" title="${groupWhy2.get("omen")}" data-toggle-keep>${ko ? "징조" : "Omens"}@if(!activeOmens.isEmpty()) <span class="badge badge-xs badge-info">${activeOmens.size()}</span>@endif</button>
		</div>
		<div class="space-y-2 [&>details:not([open])]:hidden" data-craft-means-panels>
		@if(!essences.isEmpty())
			<%-- 에센스(C353) — 보통(하급 · 보통 · 상위) = 매직 → 레어 + 보장 옵션, 완벽 · 타락 = 레어에서 하나 지우고 보장(poe2db) --%>
			!{
				java.util.Map<String, String> kindLabel = java.util.Map.of("normal", ko ? "보통 — 매직 → 레어 + 보장 옵션" : "Normal — magic to rare + guaranteed", "perfect", ko ? "완벽 — 레어에서 하나 지우고 보장" : "Perfect — remove one, add guaranteed", "corrupted", ko ? "타락 — 레어에서 하나 지우고 보장" : "Corrupted — remove one, add guaranteed", "alloy", ko ? "합금 — 레어에서 하나 지우고 보장" : "Alloy — remove one, add guaranteed");
				String essWhy = blocked.get("ESSENCE");
			}
			<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="essence" data-swap-anchor-key="essence" open="${"ESSENCE".equals(lastAction)}">
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "에센스" : "Essences"} <span class="text-xs font-normal text-base-content/70">· ${essences.size()}${ko ? "종 · 이 아이템 종류에 쓸 수 있는 것" : " for this item class"}</span></summary>
				<div class="px-3 pb-3 flex flex-wrap items-center gap-2">
					<select name="essence" class="select select-sm select-bordered min-w-0 flex-1" aria-label="${ko ? "에센스" : "Essence"}">
						@for(String kd : java.util.List.of("normal", "perfect", "corrupted", "alloy"))
							<optgroup label="${kindLabel.get(kd)}">
								@for(Poe2Craft.EssenceOption e : essences)
									@if(kd.equals(e.kind()))
										<option value="${e.slug()}" selected="${e.slug().equals(selEssence)}" disabled="${blocked.containsKey("ESSENCE:" + e.slug())}">${ko ? e.nameKo() : e.name()} — ${String.join(", ", ko && e.ko() != null && !e.ko().isEmpty() ? e.ko() : (e.en() == null ? java.util.List.of() : e.en()))}@if(blocked.containsKey("ESSENCE:" + e.slug())) (${blocked.get("ESSENCE:" + e.slug())})@endif</option>
									@endif
								@endfor
							</optgroup>
						@endfor
					</select>
					<button type="submit" name="action" value="ESSENCE" class="btn btn-sm" data-craft-action="ESSENCE" disabled="${item.corrupted() || item.sanctified() || "NORMAL".equals(rarity) || essWhy2 != null}" title="${ko ? "보통 에센스 = 매직 → 레어, 완벽 · 타락 에센스 = 레어(일반 · 타락 아이템엔 못 씀)" : "Normal essences: magic → rare; perfect/corrupted: rare (not on normal or corrupted items)"}">${ko ? "에센스 사용" : "Use essence"}</button>
				</div>
			</details>
		@endif
		@if(!bones.isEmpty())
			<%-- 훼손(C354, 심연 뼈) — 레어에 미공개 훼손 옵션 하나, 이미 훼손됐으면 못 씀, 꽉 차면 하나 지움. 갉힌 = 아이템 레벨 64 이하, 고대 = 드러낼 옵션 레벨 40 이상 --%>
			<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="bone" data-swap-anchor-key="bone" open="${"DESECRATE".equals(lastAction)}">
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "훼손(뼈)" : "Desecration (bones)"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "레어에 미공개 훼손 옵션 하나 → 드러내기 셋 중 하나" : "adds one unrevealed desecrated modifier → reveal one of three"}</span></summary>
				<div class="px-3 pb-3 flex flex-wrap items-center gap-2">
					<%-- 기본 선택 = 지금 아이템에 쓸 수 있는 첫 뼈(C439 UX QA — 첫 항목 '아이템 레벨 64 이하'가 기본 82 에서 늘 실패) --%>
					!{ String selBoneEff = !selBone.isEmpty() ? selBone : bones.stream().filter(bb -> bb.maxIlvl() == 0 || item.ilvl() <= bb.maxIlvl()).map(Poe2Craft.Bone::slug).findFirst().orElse(""); }
					<select name="bone" class="select select-sm select-bordered min-w-0 flex-1" aria-label="${ko ? "뼈" : "Bone"}">
						@for(Poe2Craft.Bone b : bones)
							<option value="${b.slug()}" selected="${b.slug().equals(selBoneEff)}" disabled="${blocked.containsKey("DESECRATE:" + b.slug())}">${ko ? b.nameKo() : b.name()}@if(b.maxIlvl() > 0) · ${ko ? "아이템 레벨 " + b.maxIlvl() + " 이하" : "ilvl ≤ " + b.maxIlvl()}@endif@if(b.minModLevel() > 0) · ${ko ? "옵션 레벨 " + b.minModLevel() + " 이상" : "mod level ≥ " + b.minModLevel()}@endif@if(blocked.containsKey("DESECRATE:" + b.slug())) (${blocked.get("DESECRATE:" + b.slug())})@endif</option>
						@endfor
					</select>
					<button type="submit" name="action" value="DESECRATE" class="btn btn-sm" data-craft-action="DESECRATE" disabled="${item.corrupted() || item.sanctified() || !rare || omenWhy.apply("DESECRATE") != null || boneWhy2 != null}" title="${omenWhy.apply("DESECRATE") != null ? omenWhy.apply("DESECRATE") : ko ? "훼손은 레어 아이템에만" : "Desecration: rare items only"}">${ko ? "훼손" : "Desecrate"}</button>
				</div>
			</details>
		@endif
		@if(!catalysts.isEmpty())
			<%-- 기폭제(C386) — 반지 · 목걸이(보통) · 주얼(제련된) 퀄리티: 태그가 맞는 옵션 수치 × (100 + 퀄리티)%, 다른 종류를 바르면 처음부터 --%>
			<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="catalyst" data-swap-anchor-key="catalyst" open="${lastAction != null && lastAction.startsWith("CATALYST")}">
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? (jewelItem ? "제련된 기폭제 — 주얼 퀄리티" : "기폭제 — 장신구 퀄리티") : (jewelItem ? "Refined catalysts — jewel quality" : "Catalysts — jewellery quality")} <span class="text-xs font-normal text-base-content/70" data-craft-quality-now="${qAmt > 0 ? qKey : null}">· ${qAmt > 0 && qInfo != null ? (ko ? "지금: " + qInfo.qualityLabelKo() + " " + qAmt + "%" : "now: " + qInfo.qualityLabel() + " " + qAmt + "%") : (ko ? "태그가 맞는 옵션 수치를 최대 20% 올림" : "raises matching modifier values by up to 20%")}</span></summary>
				<div class="px-3 pb-3 space-y-2">
					<div class="grid grid-cols-3 sm:grid-cols-4 gap-2">
						@for(Poe2Craft.Currency c : catalysts)
							!{
								String why = item.corrupted() ? (ko ? "타락한 아이템은 바꿀 수 없습니다" : "Corrupted items cannot be modified")
									: item.sanctified() ? (ko ? "아이템이 축성됨" : "Item is Sanctified")
									: c.tier().equals(qKey) && qAmt >= 20 ? (ko ? "아이템의 퀄리티가 이미 최대입니다." : "Item is already at the maximum Quality.") : null;
								String d = ko && c.descriptionKo() != null ? c.descriptionKo() : c.description();
								String cAct = "CATALYST:" + c.tier();
							}
							<button type="submit" name="action" value="${cAct}" class="btn btn-sm h-auto min-h-12 flex-col gap-0.5 py-1 ${cAct.equals(lastAction) ? "btn-primary" : "btn-ghost border border-base-300"}" data-craft-action="${cAct}" disabled="${why != null}" data-craft-blocked="${why}" title="${(ko ? c.nameKo() : c.name()) + (why != null ? " — " + why : d != null ? " — " + d : "")}">
								<img src="/poe-data/poe2/icons/currency/${c.slug().toLowerCase()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
								<span class="text-[10px] max-[379px]:text-[9px] leading-tight whitespace-normal hyphens-auto">${ko ? c.nameKo() : c.name().replace(" Catalyst", "")}</span>
							</button>
						@endfor
					</div>
					<label class="flex items-center gap-2 text-xs cursor-pointer"><input type="checkbox" name="catalystFill" value="1" class="checkbox checkbox-xs" data-craft-catalyst-fill>${ko ? "20%까지 한 번에(쓴 개수만큼 비용)" : "Fill to 20% at once (cost = catalysts used)"}</label>
					<p class="text-xs text-base-content/70">${ko ? "poe2db 설명: 퀄리티를 추가해 그 종류 속성을 향상하고, 다른 종류를 바르면 그 종류로 바뀝니다. 최대 20% · 한 번에 1%는 PoE1 기폭제와 같다고 가정합니다(공개 안 됨). 오르는 옵션 = 태그가 맞는 옵션, 수치 × (100 + 퀄리티)%. 촉진하는 찬미의 징조를 걸면 다음 엑잘이 퀄리티를 모두 소모해 같은 종류 옵션 가중치를 올립니다(poe2wiki: 20% = ×5, 그 아래는 직선 가정)." : "poe2db: adds quality that enhances that modifier type; a different type replaces it. Max 20% and 1% per use are assumed from PoE1 catalysts (unpublished). Boosted = modifiers with a matching tag, values × (100 + quality)%. With Omen of Catalysing Exaltation the next Exalt consumes all quality to weight that type up (poe2wiki: 20% = ×5, linear below assumed)."}</p>
				</div>
			</details>
		@endif
		<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="recomb" data-swap-anchor-key="recomb" open="${st != null || (lastAction != null && java.util.Set.of("STASH", "SWAP", "UNSTASH", "RECOMBINE").contains(lastAction))}">
			<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "재조합기" : "Recombinator"} <span class="text-xs font-normal text-base-content/70">· ${st == null ? (ko ? "지금 아이템을 재료로 보관한 뒤 다른 아이템과 합칩니다" : "store this item, then combine it with another") : (ko ? "고른 속성으로 레어 하나 — 실패해도 둘 다 소모" : "a rare with the selected modifiers — both consumed even on failure")}</span></summary>
			<div class="px-3 pb-3 space-y-2">
				@if(error == null && view.note() != null && view.note().startsWith("recombine:"))
					<%-- 결과 한 줄(C423 UX QA — 넓은 화면에선 결과 문구가 카드 위 화면 밖이라 아무 일 없는 것처럼 보였다) --%>
					<p class="text-xs font-medium ${view.note().startsWith("recombine:ok") ? "text-success" : "text-error"}" data-craft-recomb-inline>${view.note().startsWith("recombine:ok") ? (ko ? "재조합 성공 — 위 아이템 카드가 결과입니다" : "Recombination Succeeded — the item card above is the result") : (ko ? "재조합 실패 — 두 아이템 모두 소모(되돌리기로 되살릴 수 있음)" : "Recombination Failed — both consumed (Undo restores them)")}</p>
				@elseif(error != null && "RECOMBINE".equals(lastAction))
					<p class="text-xs font-medium text-warning" data-craft-recomb-inline>${ko ? error : net.luversof.web.gate.poe2.Poe2CraftText.en(error)}</p>
				@endif
				@if(st == null && !stashJson.isEmpty())
					<%-- 재료는 숨은 칸에 있는데 API 로 줄을 못 읽었다(C407 검토) --%>
					<div role="alert" class="alert alert-warning py-2 text-xs" data-craft-recomb-stash-error>${ko ? "보관한 재료를 읽지 못했습니다 — 잠시 뒤 다시 시도하거나 재료를 버리고 다시 보관하세요" : "Could not read the stored item — retry later, or discard and store it again"}</div>
				@endif
				@if(st != null)
					<%-- 왼쪽 패널이 좁아(넓은 화면에서도 ~380px) 두 아이템은 위아래로 --%>
					<div class="grid grid-cols-1 gap-2">
						@for(int side = 0; side < 2; side++)
							!{
								java.util.List<Poe2Craft.Line> sl = side == 0 ? lines : stLines;
								String selName = side == 0 ? "recombL" : "recombR";
								java.util.List<String> sel = side == 0 ? recombLSel : recombRSel;
								Poe2Craft.Item si = side == 0 ? item : st;
								String siRar = si.rarity() == null ? "NORMAL" : si.rarity();
								String siRarLabel = "MAGIC".equals(siRar) ? (ko ? "매직" : "Magic") : "RARE".equals(siRar) ? (ko ? "레어" : "Rare") : (ko ? "일반" : "Normal");
							}
							<div class="rounded-box border border-base-300 p-2 space-y-1 min-w-0" data-craft-recomb-side="${side == 0 ? "L" : "R"}">
								<div class="text-xs font-medium">${side == 0 ? (ko ? "지금 아이템" : "Current item") : (ko ? "보관한 재료" : "Stored item")} <span class="font-normal text-base-content/70">· ${siRarLabel}${si.corrupted() ? (ko ? " · 타락" : " · corrupted") : ""}${si.sanctified() ? (ko ? " · 축성됨" : " · sanctified") : ""} · ${ko ? "아이템 레벨 " : "ilvl "}${si.ilvl()}</span></div>
								@if(sl.isEmpty())
									<p class="text-xs text-base-content/70">${ko ? "옵션이 없습니다." : "No modifiers."}</p>
								@endif
								@for(Poe2Craft.Line l : sl)
									!{
										java.util.List<String> lx = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en());
										boolean unsel = l.id().startsWith("Unrevealed") || "corrupted".equals(l.gen());
										// 굴린 값(C423 UX QA — 아이템 카드와 같은 값, 범위는 마우스를 올리면)
										java.util.Map<String, Long> rmap = side == 0 ? rolls : stashRolls;
										Long rsd = rmap == null ? null : rmap.get(l.id());
										java.util.List<String> lxr = new java.util.ArrayList<>();
										for (int ri = 0; ri < lx.size(); ri++) lxr.add(rsd == null ? lx.get(ri) : net.luversof.web.gate.poe.PoeModRoll.roll(lx.get(ri), ri < nz.apply(l.en()).size() ? l.en().get(ri) : lx.get(ri), rsd + ri));
									}
									<label class="flex items-start gap-2 text-xs min-w-0 ${unsel ? "opacity-60" : "cursor-pointer"}" title="${unsel ? (ko ? "재조합에 선택할 수 없는 속성" : "Modifier cannot be selected for Recombination") : null}">
										<input type="checkbox" name="${selName}" value="${l.id()}" class="checkbox checkbox-xs mt-0.5" disabled="${unsel}" checked="${sel.contains(l.id())}" data-craft-recomb-mod="${l.id()}">
										<span class="min-w-0 wrap-anywhere">${"prefix".equals(l.gen()) ? (ko ? "접두 " : "P ") : "suffix".equals(l.gen()) ? (ko ? "접미 " : "S ") : ""}@if(l.tierRank() != null)T${l.tierRank()} @endif<span title="${String.join(", ", lx)}">${String.join(", ", lxr)}</span>@if(l.fractured()) (${ko ? "분열" : "fractured"})@endif</span>
									</label>
								@endfor
							</div>
						@endfor
					</div>
					<label class="flex flex-wrap items-center gap-2 text-xs">${ko ? "성공 확률(인게임 재조합기 화면 값)" : "Chance for Success (from the in-game Recombinator)"} <input type="number" name="recombChance" min="1" max="100" step="1" value="${recombChance}" class="input input-xs input-bordered w-20" data-craft-recomb-chance>%</label>
				@endif
				<div class="flex flex-wrap items-center gap-2">
					@if(st != null)
						<button type="submit" name="action" value="RECOMBINE" class="btn btn-sm btn-primary" data-craft-action="RECOMBINE" disabled="${recombWhy != null}" title="${recombWhy}">${ko ? "재조합" : "Recombine"}</button>
					@endif
					<button type="submit" name="action" value="STASH" class="btn btn-sm btn-ghost border border-base-300" data-craft-action="STASH">${st == null ? (ko ? "지금 아이템을 재료로 보관" : "Store this item") : (ko ? "지금 아이템으로 재료 바꾸기" : "Replace stored item")}</button>
					@if(st != null || !stashJson.isEmpty())
						<button type="submit" name="action" value="SWAP" class="btn btn-sm btn-ghost border border-base-300" data-craft-action="SWAP" title="${ko ? "보관한 재료를 들고 지금 아이템을 보관합니다(되돌리기로 다시 바뀜)" : "Work on the stored item and store the current one (undoable)"}">${ko ? "재료와 바꿔 들기" : "Swap with stored"}</button>
						<button type="submit" name="action" value="UNSTASH" class="btn btn-sm btn-ghost border border-base-300" data-craft-action="UNSTASH">${ko ? "재료 버리기" : "Discard stored"}</button>
					@endif
				</div>
				<%-- 핵심 한 줄을 먼저(C424 UX QA — 12줄 한 문단에 '성공 확률은 인게임 값'이 묻혔다), 나머지 규칙 · 가정은 접어 둔다 --%>
				<p class="text-xs font-medium" data-craft-recomb-key>${ko ? "성공 확률은 공식이 공개되지 않아 직접 넣습니다 — 인게임 재조합기에서 같은 속성을 골랐을 때 뜨는 값을, 재료를 보관하면 나오는 '성공 확률' 칸에 넣으세요." : "The success chance formula is unpublished — after storing an item, enter the value the in-game Recombinator shows for the same selection in the 'Chance' box."}</p>
				<details class="text-xs" data-craft-recomb-rules><summary class="cursor-pointer select-none text-base-content/70">${ko ? "규칙 · 가정 자세히" : "Rules and assumptions"}</summary>
				<p class="text-xs text-base-content/70 mt-1">${ko ? "인게임 설명: 같은 종류의 매직 · 레어 아이템 2개를 조합해, 각각에서 고른 속성과 둘 중 무작위로 고른 바탕(아이템 레벨 · 퀄리티)으로 새 레어를 만듭니다. 실패해도 둘 다 소모됩니다. 한도: 양쪽에서 하나 이상 · 접두/접미 각 3 · 주얼은 대상 아님(poe2db '무기 · 방어구 · 장신구') · 겹치는 속성 불가 · 훼손 속성 1개 · 미공개 훼손 자리는 못 고름. 축성 · 타락 아이템은 못 씁니다(커뮤니티 설명). 성공 확률은 고른 속성에 따라 정해지지만 공식이 공개되지 않아 인게임 화면의 값을 넣습니다. 재조합의 징조 = 행운(두 번 굴려 좋은 쪽). 바탕 쪽 분열 속성은 고르지 않아도 남고(커뮤니티 설명), 그 때문에 한도 · 훼손 1개를 넘는 다른 쪽 속성은 빠지며, 다른 쪽에서 고른 분열 속성은 분열째 옮긴다고 가정합니다. 보관은 복사입니다(지금 아이템은 그대로 — 에뮬레이터에선 같은 아이템끼리도 합쳐 볼 수 있습니다). 위쪽 아이템 종류(같은 클래스의 다른 변형) · 아이템 레벨을 바꿔도 보관한 재료는 남습니다. 예측 불가능한 재조합(항상 성공 · 속성 무작위)은 규칙이 공개되지 않아 넣지 않았습니다. 유물(재조합 비용)은 시세가 없어 쓴 화폐에 넣지 않습니다." : "In-game: combines two Magic or Rare items of the same class into a new rare with the modifiers you select from each and the base (item level, quality) of one input chosen at random. Both inputs are consumed even on failure. Limits: at least one from each, 3 prefixes / 3 suffixes, jewels not accepted (poe2db: weapons, armour, or jewellery), no conflicting modifiers, one Desecrated modifier, unrevealed slots unselectable. Sanctified or corrupted items cannot be used (community). The success chance depends on the selection but its formula is unpublished, so enter the in-game value. Omen of Recombination = lucky (rolled twice). Fractured modifiers on the kept base stay even if unselected (community); other-side selections over the limit or a second Desecrated modifier are assumed to drop, and fractured modifiers selected from the other side are assumed to stay fractured. Storing makes a copy (the current item stays — the emulator lets you combine an item with its own copy). Changing the item class variant or item level above keeps the stored item. Unpredictable Recombination (always succeeds, random modifiers) is not modelled — its rules are unpublished. Artifacts (the cost) have no price and are not counted."}</p>
				</details>
			</div>
		</details>
		<%-- 직접 편집(C357) — 넣기는 오른쪽 표의 + / +분열, 빼기는 여기서(화폐 비용 없음) --%>
		<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="manual" data-swap-anchor-key="manual" open="${lastAction != null && lastAction.startsWith("MANUAL")}">
			<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "직접 편집" : "Edit directly"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "시작 아이템 만들기 — 넣기는 확률 표(넓은 화면은 오른쪽, 좁은 화면은 아래)의 + / +분열" : "build a starting item — add with + / +frac (fractured) in the probability table (right, or below on narrow screens)"}</span></summary>
			<div class="px-3 pb-3 space-y-1">
				@if(lines.isEmpty())
					<p class="text-xs text-base-content/70">${ko ? "옵션이 없습니다." : "No modifiers."}</p>
				@endif
				@for(Poe2Craft.Line l : lines)
					!{ java.util.List<String> lx = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en()); }
					<div class="flex items-center gap-2 text-xs" data-craft-manual-line="${l.id()}">
						<span class="min-w-0 flex-1 wrap-anywhere">${"prefix".equals(l.gen()) ? (ko ? "접두 " : "P ") : (ko ? "접미 " : "S ")}${String.join(", ", lx)}@if(l.fractured()) (${ko ? "분열" : "fractured"})@endif</span>
						<button type="submit" name="action" value="MANUAL_REMOVE:${l.id()}" class="btn btn-xs btn-ghost border border-base-300" data-craft-action="MANUAL_REMOVE:${l.id()}">${ko ? "빼기" : "Remove"}</button>
					</div>
				@endfor
			</div>
		</details>
		<details class="rounded-box border border-base-300 bg-base-100" data-craft-group="omen" data-swap-anchor-key="omen" open="${!activeOmens.isEmpty()}">
			<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "징조" : "Omens"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "체크한 징조는 다음 해당 화폐에 한 번 쓰이고 사라집니다 · 같은 화폐의 좌 · 우를 같이 걸면 '유효하지 않은 징조 조합'" : "checked omens apply once to the next matching currency · Sinistral and Dextral for the same currency together are an invalid combination"}</span>@if(!activeOmens.isEmpty()) <span class="badge badge-sm badge-info badge-outline" data-craft-omen-count>${activeOmens.size()}</span>@endif</summary>
			<div class="px-3 pb-3 space-y-2">
				@for(var g : omenGroups.entrySet())
					<div>
						<div class="text-xs text-base-content/70">${g.getKey()}</div>
						<div class="grid grid-cols-1 sm:grid-cols-2 gap-1">
							@for(String k : g.getValue())
								!{ Poe2Craft.Omen o = omenByKey.get(k); boolean vo = java.util.Set.of("SinistralExaltation", "DextralExaltation", "CatalysingExaltation").contains(k) || k.startsWith("Sinistral") || k.startsWith("Dextral") || "AbyssalEchoes".equals(k); }
								<%-- 지금 아이템에 효과가 없는 징조는 흐리게 + 이유(C478). 체크는 막지 않는다 — 인게임 징조는 언제든 켜 두고 다음 해당 화폐에 소모(매직일 때 미리 좌측 찬미를 켜고 제왕 → 엑잘) --%>
								!{ String omenOff = activeOmens.contains(k) ? null : blocked.get("OMEN:" + k); }
								<label class="flex items-start gap-2 text-xs min-w-0 ${omenOff != null ? "opacity-50" : ""}" data-craft-omen="${k}" data-craft-omen-blocked="${omenOff}" title="${omenOff != null ? (ko ? "지금 아이템엔 효과 없음 — " : "No effect on the current item — ") + omenOff : omenDesc.get(k)}">
									<%-- 표에 반영되는 엑잘 징조(C396)는 바꾸면 화면만 다시 그린다(동작 VIEW — 아이템 그대로) --%>
									<input type="checkbox" name="omen" value="${k}" class="checkbox checkbox-xs mt-0.5" checked="${activeOmens.contains(k)}" hx-post="${vo ? "/poe2/htmx/craft" : null}" hx-trigger="${vo ? "change" : null}" hx-include="${vo ? "closest form" : null}" hx-vals="${vo ? "{\"action\":\"VIEW\"}" : null}" hx-target="${vo ? "#craft2-emu" : null}" hx-swap="${vo ? "outerHTML" : null}" hx-sync="${vo ? "closest form:replace" : null}">
									@if(o != null && o.slug() != null)<img src="/poe-data/poe2/icons/currency/${o.slug().toLowerCase()}.png" alt="" class="w-5 h-5 object-contain shrink-0" loading="lazy">@endif
									<span class="min-w-0 wrap-anywhere"><span class="font-medium">${o == null ? k : (ko ? o.nameKo() : o.name())}</span> <span class="text-base-content/70">${omenDesc.get(k)}</span></span>
								</label>
							@endfor
						</div>
					</div>
				@endfor
			</div>
		</details>
		</div>

		@if(view.revealOffers() != null && !view.revealOffers().isEmpty() && !item.sanctified())
			<%-- 드러내기(C354) — 영혼의 우물에서 셋 중 하나(아이템 상태로 정해진 셋) --%>
			<div class="rounded-box border border-base-300 bg-base-100 p-3 space-y-2" data-craft-reveal>
				<div class="flex flex-wrap items-center gap-2">
					<div class="text-sm font-medium flex-1">${ko ? "훼손된 속성 드러내기 — 셋 중 하나를 고르세요" : "Reveal the desecrated modifier — choose one of three"}</div>
					<button type="submit" name="action" value="REVEAL_REROLL" class="btn btn-xs btn-ghost border border-base-300" data-craft-action="REVEAL_REROLL" disabled="${!activeOmens.contains("AbyssalEchoes")}" title="${ko ? "심연의 메아리의 징조를 체크하고 누르면 셋을 한 번 다시" : "Check Omen of Abyssal Echoes to reroll the options once"}">${ko ? "셋 다시(메아리)" : "Reroll (Echoes)"}</button>
				</div>
				<div class="grid gap-2">
					@for(Poe2Craft.Line u : view.revealOffers())
						!{ java.util.List<String> ux = ko && !nz.apply(u.ko()).isEmpty() ? u.ko() : nz.apply(u.en()); }
						<button type="submit" name="action" value="REVEAL:${u.id()}" class="btn btn-sm h-auto min-h-10 py-2 btn-ghost border border-base-300 text-left justify-start font-normal" data-craft-action="REVEAL:${u.id()}" title="${ko ? u.nameKo() : u.name()}">
							<span class="text-[10px] font-mono text-base-content/70 shrink-0">${"prefix".equals(u.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")} · ${ko ? "레벨 " : "lv "}${u.level()}</span>
							<span class="text-xs leading-snug whitespace-normal">@for(int i = 0; i < ux.size(); i++)$unsafe{PoeText.highlightValues(ux.get(i))}@if(i < ux.size() - 1)<br>@endif@endfor</span>
						</button>
					@endfor
				</div>
			</div>
		@endif

		@if(error == null && view.note() != null && view.note().startsWith("catalysing:"))
			<%-- 촉진하는 찬미(C386) — 엑잘이 기폭제 퀄리티를 모두 소모 --%>
			!{
				String gnote = view.note();
				String gAmt = gnote.substring(gnote.lastIndexOf(':') + 1);
				String gKey = gnote.substring("catalysing:".length(), gnote.lastIndexOf(':'));
				Poe2Craft.Currency gInfo = null;
				for (Poe2Craft.Currency c : currencies) if ("CATALYST".equals(c.action()) && gKey.equals(c.tier())) gInfo = c;
				double gMult = 0;
				try { int q = Integer.parseInt(gAmt); gMult = q <= 20 ? 1 + 0.2 * q : 5 + 0.125 * (q - 20); } catch (NumberFormatException e) { gMult = 0; }
			}
			<div role="status" class="alert py-2 text-sm" data-craft-catalysing="${gnote}">
				<span><span class="font-medium">${ko ? "촉진하는 찬미: " : "Catalysing Exaltation: "}</span>${ko ? "퀄리티 (" + (gInfo == null ? gKey : gInfo.qualityLabelKo()) + ") " + gAmt + "%를 모두 소모 — 그 종류 옵션 가중치 ×" : "consumed all " + gAmt + "% quality (" + (gInfo == null ? gKey : gInfo.qualityLabel()) + ") — that type weighted ×"}${String.format(java.util.Locale.ROOT, "%.1f", gMult)}<br><span class="text-xs text-base-content/70">${ko ? "배수는 poe2wiki(20% = ×5, 40% = ×7.5), 그 아래는 직선 가정" : "Multiplier from poe2wiki (20% = ×5, 40% = ×7.5), linear below assumed"}</span></span>
			</div>
		@endif
		@if(error == null && view.note() != null && view.note().startsWith("catalyst:"))
			!{
				String cnote = view.note();
				String[] cp = cnote.split(":");
				String cAmt = cp.length >= 3 ? cp[cp.length - 2] : "";
				String cUsedN = cp.length >= 4 ? cp[cp.length - 1] : "";
				String cKey = cp.length >= 4 ? String.join(":", java.util.Arrays.copyOfRange(cp, 1, cp.length - 2)) : "";
				Poe2Craft.Currency cInfo = null;
				for (Poe2Craft.Currency c : currencies) if ("CATALYST".equals(c.action()) && cKey.equals(c.tier())) cInfo = c;
			}
			<div role="status" class="alert py-2 text-sm" data-craft-catalyst-note="${cnote}">
				<span><span class="font-medium">${cInfo == null ? "" : ko ? cInfo.nameKo() : cInfo.name()} × ${cUsedN}: </span>${ko ? "퀄리티 (" + (cInfo == null ? "" : cInfo.qualityLabelKo()) + ") " + cAmt + "%" : "Quality (" + (cInfo == null ? "" : cInfo.qualityLabel()) + ") " + cAmt + "%"}</span>
			</div>
		@endif
		@if(error == null && view.note() != null && view.note().startsWith("vaal:"))
			<%-- 바알 오브 결과(C376) — 확률 비공개라 균등으로 굴린다 --%>
			!{
				String vn2 = view.note();
				String vaalMsg2 = vn2.startsWith("vaal:randomize")
					? (ko ? "옵션 " + vn2.substring(vn2.lastIndexOf(':') + 1) + "개가 무작위로 바뀜" : vn2.substring(vn2.lastIndexOf(':') + 1) + " modifier(s) randomized")
					: switch (vn2) {
						case "vaal:enchant" -> ko ? "타락 인챈트가 붙음" : "Corrupted enchantment added";
						case "vaal:socket" -> ko ? "홈 +1(한도 무시) — 홈은 이 에뮬레이터가 다루지 않습니다" : "+1 socket (ignores limit) — sockets are not modelled here";
						case "vaal:quality" -> ko ? "퀄리티 상승(최대 23%) — 퀄리티는 이 에뮬레이터가 다루지 않습니다" : "Quality increased (up to 23%) — quality is not modelled here";
						case "vaal:jewel:add" -> ko ? "옵션 하나가 붙음(자리 한도 무시 — 주얼)" : "A modifier was added (ignoring limits — jewel)";
						case "vaal:jewel:remove" -> ko ? "옵션 하나가 지워짐(주얼)" : "A modifier was removed (jewel)";
						default -> ko ? "변화 없음 — 타락만" : "No change — just corrupted";
					};
			}
			<div role="status" class="alert py-2 text-sm" data-craft-vaal="${vn2}">
				<span><span class="font-medium">${ko ? "바알 오브 결과: " : "Vaal Orb outcome: "}</span>${vaalMsg2}<br><span class="text-xs text-base-content/70">${ko ? "결과(변화 없음 · 옵션 1~3개 무작위 · 타락 인챈트 · 부위 특수 — 주얼은 옵션 추가 | 제거)의 확률은 공개되지 않아 균등으로 굴립니다" : "Outcome odds (none, 1–3 randomized, enchantment, slot special) are unpublished and rolled evenly"}</span></span>
			</div>
		@endif
		@if(error == null && view.note() != null && view.note().startsWith("recombine:"))
			<%-- 재조합기 결과(C407) — 인게임 문구 "재조합 성공 / 실패"(ClientStrings RecombinatorUILastResultLabel*) --%>
			!{
				String[] rn = view.note().split(":");
				boolean rOk = rn.length > 1 && "ok".equals(rn[1]);
				String rP = rOk ? (rn.length > 3 ? rn[3] : "") : (rn.length > 2 ? rn[2] : "");
				String rDrop = rOk && rn.length > 4 ? rn[4] : null;
				boolean rLeft = rOk && rn.length > 2 && "L".equals(rn[2]);
				boolean rLucky = view.omensUsed() != null && view.omensUsed().contains("Recombination");
			}
			<div role="status" class="alert ${rOk ? "" : "alert-error"} py-2 text-sm" data-craft-recomb-note="${view.note()}">
				<span><span class="font-medium">${rOk ? (ko ? "재조합 성공" : "Recombination Succeeded") : (ko ? "재조합 실패" : "Recombination Failed")}</span> · ${ko ? "성공 확률 " : "Chance for Success "}${rP}%${rLucky ? (ko ? " (재조합의 징조 — 행운)" : " (Omen of Recombination — lucky)") : ""}<br><span class="text-xs text-base-content/70">${rOk ? (ko ? "바탕: " + (rLeft ? "지금 아이템" : "보관한 재료") : "Base: " + (rLeft ? "current item" : "stored item")) + (rDrop != null ? (ko ? " · 바탕 쪽 분열 속성 때문에 한도를 넘은 속성 " + rDrop + "개가 빠짐" : " · " + rDrop + " modifier(s) dropped over the limit (fractured on the base)") : "") : (ko ? "두 아이템 모두 소모 — 되돌리기로 재료까지 되살릴 수 있습니다" : "Both items consumed — Undo restores both")}</span></span>
			</div>
		@endif
		@if(error == null && "abyssmark".equals(view.note()))
			<%-- 심연의 군주의 징표(C416) — 인게임 KeywordPopups: 훼손하면 징표가 지워지고 한 등급 높은 미공개 훼손 속성으로 --%>
			<div role="status" class="alert py-2 text-sm" data-craft-abyssmark-note>
				<span><span class="font-medium">${ko ? "심연의 군주의 징표: " : "Mark of the Abyssal Lord: "}</span>${ko ? "징표 자리에 미공개 훼손 속성(다른 속성은 지우지 않음)" : "replaced by an Unrevealed Desecrated modifier in its slot (no other modifier removed)"}<br><span class="text-xs text-base-content/70">${ko ? "인게임 설명의 '한 등급 높은'은 게임 데이터에 훼손된 속성 등급이 하나(레벨 65)뿐이라 효과가 없습니다. 징표는 이 부위에 훼손된 속성이 있는 쪽에 붙고(한쪽뿐이면 그쪽), 양쪽 다 있으면 심연의 에센스가 지운 속성의 쪽에 붙는다고 가정합니다" : "The 'higher tier' has no effect here — the game data has a single Desecrated tier (level 65). The mark is assumed to take a side that has Desecrated modifiers for this item class (or the removed modifier's side when both do)"}</span></span>
			</div>
		@endif
		@if(!foresee.isEmpty())
			<%-- 히네코라의 머리카락(C361) — 다음 화폐 결과를 미리 본다(적용 · 징조 소모 안 함). "이대로 적용"은 같은 결과 --%>
			<div class="rounded-box border border-info/60 bg-base-100 p-3 space-y-2" data-craft-foresee>
				<div class="text-sm font-medium">${ko ? "히네코라의 머리카락 — 다음 화폐 결과 미리 보기" : "Hinekora's Lock — foreseeing the next currency"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "아이템이 바뀌면 사라집니다" : "lost once the item is modified"}</span></div>
				@if(preview != null)
					!{
						java.util.List<Poe2Craft.Line> pl = preview.lines() == null ? java.util.List.of() : preview.lines();
						java.util.List<String> padd = preview.added() == null ? java.util.List.of() : preview.added();
						String pr = preview.item().rarity();
						String pa = previewAction == null ? "" : previewAction.contains(":") ? previewAction.substring(0, previewAction.indexOf(':')) : previewAction;
						String plabel = btns.stream().filter(b -> b.action().equals(pa)).map(b -> ko ? b.ko() : b.en()).findFirst().orElse("VAAL".equals(pa) ? (ko ? "바알 오브" : "Vaal Orb") : pa);
					}
					<div class="text-xs space-y-0.5" data-craft-preview="${previewAction}">
						<div class="font-medium">${ko ? "미리 본 화폐: " : "Foreseen: "}${plabel} → ${"RARE".equals(pr) ? (ko ? "레어" : "Rare") : "MAGIC".equals(pr) ? (ko ? "매직" : "Magic") : (ko ? "일반" : "Normal")}</div>
						@for(Poe2Craft.Line l : pl)
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
					<p class="text-xs text-base-content/70">${ko ? "화폐를 누르면 결과를 미리 보여 줍니다(적용 · 징조 소모 안 함). 같은 화폐 · 징조는 몇 번을 눌러도 같은 결과입니다." : "Click a currency to preview its result without applying it or using omens. The same currency and omens always show the same result."}</p>
					<button type="submit" name="action" value="FORESEE_OFF" class="btn btn-sm btn-ghost border border-base-300" data-craft-action="FORESEE_OFF">${ko ? "그만 보기" : "Stop foreseeing"}</button>
				@endif
			</div>
		@endif

	</form>

	<%-- 작업 영역(C454 — 한 화면): 아이템 카드 | 다음 옵션 확률(칸 안 스크롤) | 단계 기록 + 쓴 화폐(칸 안 스크롤). 2칸(lg)은 카드 아래 기록 --%>
	<div class="grid gap-4 items-start lg:grid-cols-[minmax(0,22rem)_minmax(0,1fr)] xl:grid-cols-[minmax(0,22rem)_minmax(0,1fr)_minmax(0,24rem)]" data-craft-workspace>
	<%-- 대상 아이템은 늘 보이게(C453 사용자 요청) — 좁은 화면 · 넓은 3칸 화면은 화면 위에 붙는다(C453b · C454). 2칸(lg)은 아래 기록이 가려져 붙이지 않음 --%>
	<div class="sticky top-[4.75rem] z-20 bg-base-100 pt-1 pb-2 space-y-1 min-w-0 max-h-[40vh] overflow-y-auto lg:static lg:max-h-none lg:overflow-visible xl:sticky" data-craft-item-sticky>
	<%-- 화폐를 고른 상태 안내(C477 — 고르고 아이템을 누르면 적용, CoE 처럼) --%>
	<div class="text-xs rounded-box border border-warning/60 bg-warning/10 px-2 py-1 max-w-[24rem]" data-craft-arm-hint hidden><span class="font-semibold" data-craft-arm-label></span> ${ko ? "— 아이템을 누르면 적용(계속 누르면 연타) · 단추를 다시 누르거나 Esc 로 취소" : "— click the item to apply (keep clicking to repeat) · click the button again or Esc to cancel"}</div>
	<div class="poe-tooltip ${rarCls} w-full max-w-[24rem]" style="border-color:${headColor}" data-craft-tooltip data-craft-arm-target>
		<div class="poe-tt-band">
			<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${headColor}" data-craft-name>${rare ? (ko ? "레어 아이템" : "Rare Item") : magicName}</div>
			@if(rare)<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${headColor}">${classLabel}</div>@endif
		</div>
		@if(qAmt > 0 && qInfo != null)
			<%-- 기폭제 퀄리티(C386) — "Quality (Life Modifiers): +20%", 값은 보강 색 --%>
			<div class="px-4 pt-2 -mb-1 text-center text-[13px] text-white/60" data-craft-quality="${qKey}">${ko ? "퀄리티 (" + qInfo.qualityLabelKo() + "): " : "Quality (" + qInfo.qualityLabel() + "): "}<span style="color:#8888ff">+${qAmt}%</span></div>
		@endif
		<div class="px-4 py-2 text-center text-[13px] text-white/60">
			${ko ? "아이템 레벨: " : "Item Level: "}<span class="text-white">${item.ilvl()}</span>
		</div>
		@if(!lines.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 space-y-0.5 text-center text-[13px]">
				@for(Poe2Craft.Line l : lines)
					!{
						java.util.List<String> mx = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en());
						boolean isNew = added != null && added.contains(l.id());
						String color = l.fractured() ? "#a29162" : "#8888ff";
					}
					<div class="${isNew ? "bg-white/10 rounded" : ""}" style="color:${color}" data-craft-line="${l.id()}" data-craft-gen="${l.gen()}" data-craft-new="${isNew ? "1" : null}">
						<span class="text-[10px] font-mono text-white/60 block leading-tight" data-craft-line-tag title="${(ko ? l.nameKo() : l.name()) + (ko ? " · 옵션 레벨 " : " · level ") + l.level()}">${"corrupted".equals(l.gen()) ? (ko ? "타락" : "Corrupted") : "prefix".equals(l.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}@if(l.tierRank() != null) T${l.tierRank()}@endif@if(!l.id().startsWith("Veiled") && !l.id().startsWith("Unrevealed") && (ko ? (l.nameKo() != null && !l.nameKo().isBlank()) : (l.name() != null && !l.name().isBlank()))) <span class="font-sans text-white/75" data-craft-line-affix>${(ko ? l.nameKo() : l.name()).replaceFirst("^- ", "")}</span>@endif!{ java.util.List<String> lineTags = ko ? (view.tagsKo() == null ? null : view.tagsKo().get(l.id())) : (view.tags() == null ? null : view.tags().get(l.id())); }@if(lineTags != null && !lineTags.isEmpty()) <span class="font-sans text-white/50" data-craft-line-tags>— ${String.join(", ", lineTags)}</span>@endif@if(l.fractured()) ${ko ? "분열" : "Fractured"}@endif@if(l.id().startsWith("Unrevealed")) ${ko ? "미공개" : "Unrevealed"}@elseif(l.id().startsWith("AbyssMod")) ${ko ? "훼손" : "Desecrated"}@endif@if(boostedIds.contains(l.id()) && qAmt > 0) <span data-craft-line-boosted title="${ko ? "기폭제 퀄리티로 수치 +" + qAmt + "%" : "Values +" + qAmt + "% from catalyst quality"}">${ko ? "퀄리티" : "Q"}</span>@endif</span>
						@for(int i = 0; i < mx.size(); i++)
							!{ Long rs = rolls.get(l.id()); }
							<%-- 굴린 값(C370, 인게임 툴팁처럼) — 범위는 마우스를 올리면 --%>
							<div class="inline" title="${rs != null ? mx.get(i) : null}" data-craft-rolled="${rs != null ? "1" : null}" data-craft-boosted="${boostedIds.contains(l.id()) && qAmt > 0 ? "1" : null}">$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRoll.scale(net.luversof.web.gate.poe.PoeModRoll.scaleUp(rs != null ? net.luversof.web.gate.poe.PoeModRoll.roll(mx.get(i), i < nz.apply(l.en()).size() ? l.en().get(i) : mx.get(i), rs + i) : mx.get(i), rolls.containsKey("sanct:" + l.id()) ? rolls.get("sanct:" + l.id()).intValue() : 100), boostedIds.contains(l.id()) ? qAmt : 0))}</div>@if(i < mx.size() - 1)<br>@endif
						@endfor
					</div>
				@endfor
			</div>
		@endif
		@if(item.sanctified())
			<%-- 축성(C390) — 인게임 "[Sanctified|축성]"(ClientStrings ItemPopupSanctified) --%>
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#e8d08a]" data-craft-sanctified>${ko ? "축성" : "Sanctified"}</div>
		@endif
		@if(item.corrupted())
			<%-- 타락 표시(C376 — 부패의 징조 · 바알 오브로 타락해도 툴팁에 안 보였다, PoE1 툴팁과 같은 인게임 빨강) --%>
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#d20000]" data-craft-corrupted>${ko ? "타락" : "Corrupted"}</div>
		@endif
	</div>
	<div class="text-xs text-base-content/70" data-craft-slots>
		${ko ? "접두 " : "Prefixes "}${prefixes}/${view.maxPerGen()} · ${ko ? "접미 " : "Suffixes "}${suffixes}/${view.maxPerGen()}
	</div>
	<%-- 직전 단계 변화(C453) — 붙은 옵션은 툴팁에서 강조, 지운 옵션은 툴팁에 없어 여기에 --%>
	@if(!historyRows.isEmpty() && historyRows.get(0).now() && (!historyRows.get(0).added().isEmpty() || !historyRows.get(0).removed().isEmpty()))
		!{ Poe2Craft.HistRow lastRow = historyRows.get(0); }
		<div class="text-xs max-w-[24rem] space-y-0.5" data-craft-last-change>
			<span class="text-base-content/70">${ko ? "직전 단계(" + lastRow.no() + ") " + lastRow.label() + ":" : "Last step (" + lastRow.no() + ") " + lastRow.label() + ":"}</span>
			@for(String a : lastRow.added())<div class="text-success wrap-anywhere">+ ${a}</div>@endfor
			@for(String r : lastRow.removed())<div class="text-error wrap-anywhere line-through decoration-1">− ${r}</div>@endfor
		</div>
	@endif
	<%-- 오류는 아이템 카드와 함께 붙어 있게(C453c — 폼 맨 위면 붙은 카드에 가렸다) --%>
	@if(error != null)
		<div role="alert" class="alert alert-warning py-2 text-sm scroll-mt-24 ${"RECOMBINE".equals(lastAction) ? "" : "craft-error-reveal"}" data-craft-error>${ko ? error : net.luversof.web.gate.poe2.Poe2CraftText.en(error)}</div>
	@endif
	</div>
	<section class="min-w-0 space-y-2 max-lg:order-last lg:row-span-2 xl:row-span-1 lg:max-h-[max(26rem,calc(100vh-24rem))] lg:overflow-y-auto lg:pr-1" id="craft2-pool" data-craft-pool>
		!{ boolean exOmen = activeOmens.stream().anyMatch(o -> java.util.Set.of("SinistralExaltation", "DextralExaltation", "CatalysingExaltation").contains(o)); }
		<h2 class="text-base font-semibold" data-craft-pool-omens="${exOmen ? "1" : null}">${ko ? "다음 옵션 — 지금 아이템에 붙을 수 있는 것(균등" + (exOmen ? " · 걸어 둔 엑잘 징조 반영" : "") + (tier.isEmpty() ? "" : " · " + ("perfect".equals(tier) ? "완벽한" : "상위") + " 등급 최소 레벨 반영") + ")" : "Next modifier — what can roll now (uniform" + (exOmen ? ", active Exalt omens applied" : "") + (tier.isEmpty() ? "" : ", " + ("perfect".equals(tier) ? "Perfect" : "Greater") + " tier minimum level applied") + ")"}</h2>
		@if(item.corrupted())
			<%-- 타락한 아이템(C379) — 더 붙을 옵션이 없다(확률 표를 그대로 보여 오해를 샀다) --%>
			<p class="text-sm text-base-content/70" data-craft-pool-corrupted>${ko ? "타락한 아이템이라 더 붙을 수 있는 옵션이 없습니다." : "Corrupted — no modifier can be added."}</p>
		@elseif(pool.isEmpty() || pool.stream().allMatch(p -> p.chance() <= 0))
			<%-- 등급 최소 레벨(진화 · 확장 44/70, 제왕 · 엑잘 · 카오스 35/50)보다 아이템 레벨이 낮을 때만 등급 안내(C445 검토 — 자리 꽉 참과 구분) --%>
			!{ int tierMin = tier.isEmpty() ? 0 : rare ? ("perfect".equals(tier) ? 50 : 35) : ("perfect".equals(tier) ? 70 : 44); }
			<p class="text-sm text-base-content/70" data-craft-pool-empty>${ko ? "더 붙을 수 있는 옵션이 없습니다." : "No modifier can be added."}@if(tierMin > 0 && item.ilvl() < tierMin) ${ko ? "(고른 등급은 아이템 레벨 " + tierMin + " 이상에만 — 아래 표는 직접 편집 넣기용)" : "(the chosen tier needs item level " + tierMin + "+ — the table below is for manual adds)"}@endif</p>
		@endif
		@if(item.sanctified() && !item.corrupted())
			<%-- 축성된 아이템(C397 검토) — 더 바꿀 수 없다(타락과 같게 확률 표 대신 안내) --%>
			<p class="text-sm text-base-content/70" data-craft-pool-sanctified>${ko ? "축성된 아이템이라 더 붙을 수 있는 옵션이 없습니다." : "Sanctified — no modifier can be added."}</p>
		@endif
		@for(String gen : item.corrupted() || item.sanctified() ? java.util.List.<String>of() : java.util.List.of("prefix", "suffix"))
			!{ java.util.List<Poe2Craft.PoolLine> g = pool.stream().filter(p -> gen.equals(p.gen())).toList(); }
			@if(!g.isEmpty())
				<details class="rounded-box border border-base-300 bg-base-100" open>
					<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${"prefix".equals(gen) ? (ko ? "접두" : "Prefixes") : (ko ? "접미" : "Suffixes")} · ${g.size()}${ko ? "개" : ""}</summary>
					<div class="px-3 pb-3 overflow-x-auto">
						<table class="table table-xs">
							<thead><tr><th>${ko ? "등급" : "Tier"}</th><th>${ko ? "옵션" : "Modifier"}</th><th class="text-right">${ko ? "레벨" : "Level"}</th><th class="text-right">${ko ? "확률" : "Chance"}</th><th class="text-right">${ko ? "넣기" : "Add"}</th></tr></thead>
							<tbody>
								@for(Poe2Craft.PoolLine p : g)
									!{ java.util.List<String> mx = ko && !nz.apply(p.ko()).isEmpty() ? p.ko() : nz.apply(p.en()); }
									<%-- 고른 등급에 걸린 줄(C445) = 확률 0 · 흐리게, 직접 편집 넣기는 그대로 --%>
									<tr data-craft-pool-line="${p.id()}" data-craft-pool-zero="${p.chance() <= 0 ? "1" : null}" class="${p.chance() <= 0 ? "opacity-50" : null}">
										<td class="font-mono text-xs">@if(p.tierRank() != null)T${p.tierRank()}@endif</td>
										<td class="min-w-0 wrap-anywhere">
											@for(String t : mx)
												<div>$unsafe{PoeText.highlightValues(t)}</div>
											@endfor
										</td>
										<td class="text-right font-mono">${p.level()}</td>
										<td class="text-right font-mono">@if(p.chance() <= 0)-@else${String.format("%.2f%%", p.chance() * 100)}@endif</td>
										<td class="text-right whitespace-nowrap">
											<button type="button" name="action" value="MANUAL_ADD:${p.id()}" class="btn btn-xs btn-ghost border border-base-300" hx-post="/poe2/htmx/craft" hx-target="#craft2-emu" hx-swap="outerHTML" hx-include="#craft2-emu > form" data-craft-action="MANUAL_ADD:${p.id()}" title="${ko ? "이 옵션을 넣기" : "Add this modifier"}">+</button>
											<button type="button" name="action" value="MANUAL_ADD:${p.id()}:F" class="btn btn-xs btn-ghost border border-base-300" hx-post="/poe2/htmx/craft" hx-target="#craft2-emu" hx-swap="outerHTML" hx-include="#craft2-emu > form" data-craft-action="MANUAL_ADD:${p.id()}:F" title="${ko ? "분열 옵션으로 넣기" : "Add as fractured"}">${ko ? "+분열" : "+frac"}</button>
										</td>
									</tr>
								@endfor
							</tbody>
						</table>
					</div>
				</details>
			@endif
		@endfor
		<p class="text-xs text-base-content/70">${ko ? "PoE2 옵션 가중치는 게임 데이터에 없어(GGG 비공개) 붙을 수 있는 옵션 중 균등으로 굴립니다 — 확률은 참고용입니다. 옵션 수 · 등급 최소 레벨 · 징조 규칙은 poe2db 문구 기준." : "PoE2 modifier weights are hidden, so rolls are uniform among eligible modifiers — odds are indicative only. Counts, tier minimum levels and omen rules follow poe2db text."}</p>
	</section>
	<%-- 단계 기록(C453 사용자 요청) — 단계마다 쓴 화폐 · 그 단계 · 누적 비용 · 변화, 아무 단계로 되돌리기(이 단계 뒤 상태로) --%>
	<section class="min-w-0 space-y-3 lg:col-start-1 xl:col-start-3 xl:row-start-1 lg:max-h-[max(26rem,calc(100vh-24rem))] lg:overflow-y-auto lg:pr-1" data-craft-history>
		!{ Double histCum = historyRows.isEmpty() ? null : historyRows.get(0).cumDivine(); }
		<h2 class="text-base font-semibold">${ko ? "단계 기록" : "Step history"} <span class="text-xs font-normal text-base-content/70">· ${historyRows.size()}${ko ? "단계" : " steps"}@if(histCum != null) · ${ko ? "누적 " : "total "}${String.format("%.4f", histCum)} div@endif@if(historyDropped > 0) · ${ko ? "앞 " + historyDropped + "단계는 기록 한도로 되돌릴 수 없음" : historyDropped + " oldest steps beyond the undo limit"}@endif</span></h2>
		@if(historyRows.isEmpty())
			<p class="text-sm text-base-content/70" data-craft-history-empty>${ko ? "화폐를 쓰면 단계마다 쓴 화폐 · 비용 · 붙은/지운 옵션이 여기에 쌓이고, 어느 단계로든 되돌릴 수 있습니다." : "Each step's currency, cost and added/removed modifiers build up here, and you can go back to any step."}</p>
		@else
			<div class="overflow-x-auto rounded-box border border-base-300">
				<table class="table table-xs">
					<thead class="sticky top-0 z-10 bg-base-200"><tr><th>#</th><th>${ko ? "쓴 화폐" : "Currency"}</th><th class="text-right" title="${ko ? "이 단계 비용 · 아래 작은 글씨는 여기까지 누적(div)" : "This step · total so far below (div)"}">${ko ? "비용 · 누적" : "Cost · total"}</th><th>${ko ? "변화" : "Change"}</th><th class="text-right">${ko ? "되돌리기" : "Go back"}</th></tr></thead>
					<tbody>
						@for(Poe2Craft.HistRow r : historyRows)
							<tr data-craft-history-row="${r.no()}" class="${r.now() ? "bg-primary/10" : null}">
								<td class="font-mono">${r.no()}</td>
								<td class="break-keep">${r.label()}</td>
								<td class="text-right font-mono whitespace-nowrap">
									<div data-craft-history-cost>@if(r.divine() != null)${String.format("%.4f", r.divine())}@else-@endif</div>
									<div class="text-[10px] text-base-content/60" data-craft-history-cum>Σ @if(r.cumDivine() != null)${String.format("%.4f", r.cumDivine())}@else-@endif</div>
								</td>
								<td class="wrap-anywhere" data-craft-history-change>
									@for(String a : r.added())<div class="text-success">+ ${a}</div>@endfor
									@for(String rm : r.removed())<div class="text-error line-through decoration-1">− ${rm}</div>@endfor
									@if(r.note() != null)<div class="text-base-content/60">${r.note()}</div>@endif
								</td>
								<td class="text-right whitespace-nowrap">
									@if(r.now())
										<span class="badge badge-primary badge-xs" data-craft-history-now>${ko ? "지금" : "now"}</span>
									@elseif(r.undoTo() != null)
										<button type="button" name="action" value="UNDO_TO:${r.undoTo()}" class="btn btn-xs btn-ghost border border-base-300 whitespace-nowrap" hx-post="/poe2/htmx/craft" hx-target="#craft2-emu" hx-swap="outerHTML" hx-include="#craft2-emu > form" data-craft-action="UNDO_TO:${r.undoTo()}" title="${ko ? "이 단계(" + r.no() + ")를 마친 상태로 되돌립니다 — 그 뒤 단계 · 쓴 화폐는 빠집니다" : "Go back to the state after step " + r.no() + " — later steps and their currency are removed"}">${ko ? "여기로" : "Go here"}</button>
									@endif
								</td>
							</tr>
						@endfor
						@if(historyStartUndo != null)
							<tr data-craft-history-row="0">
								<td class="font-mono">0</td>
								<td colspan="3" class="text-base-content/70">${historyDropped > 0 ? (ko ? "기록에 남은 가장 오래된 상태" : "Oldest state in the history") : (ko ? "시작 — 새 아이템" : "Start — new item")}</td>
								<td class="text-right"><button type="button" name="action" value="UNDO_TO:${historyStartUndo}" class="btn btn-xs btn-ghost border border-base-300 whitespace-nowrap" hx-post="/poe2/htmx/craft" hx-target="#craft2-emu" hx-swap="outerHTML" hx-include="#craft2-emu > form" data-craft-action="UNDO_TO:${historyStartUndo}">${ko ? "여기로" : "Go here"}</button></td>
							</tr>
						@endif
					</tbody>
				</table>
			</div>
		@endif
		@if(spent != null && !spent.lines().isEmpty())
			<%-- 쓴 화폐 · 총비용(C366, 디바인) — 아이템을 바꾼 단계마다 센다(되돌리면 빠짐, 머리카락은 남음), 쓰인 징조 포함. 열 = 개수 · 단가 · 합계 --%>
			!{
				java.util.Map<String, String> spKo = new java.util.HashMap<>();
				for (Btn b : btns) spKo.put(b.en(), b.ko());
				for (Poe2Craft.Currency c : currencies) spKo.put(c.name(), c.nameKo());
				for (Poe2Craft.Omen o : omens) spKo.put(o.name(), o.nameKo());
				for (Poe2Craft.EssenceOption e : essences) spKo.put(e.name(), e.nameKo());
				for (Poe2Craft.Bone b : bones) spKo.put(b.name(), b.nameKo());
				spKo.put("Hinekora's Lock", "히네코라의 머리카락");
				spKo.put("Divine Orb", "신성한 오브");
				spKo.put("Vaal Orb", "바알 오브");
				java.util.function.Function<Double, String> fmt = v -> v == null ? "—" : v >= 100 ? String.format("%,.0f", v) : v >= 1 ? String.format("%.2f", v) : String.format("%.4f", v);
			}
			<details class="rounded-box border border-base-300 bg-base-100" data-craft-spent open>
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "쓴 화폐" : "Currency spent"} <span class="text-xs font-normal text-base-content/70" data-craft-spent-total>· ${ko ? "합계 " : "total "}${fmt.apply(spent.totalDivine())} div@if(spent.missing() > 0) · ${ko ? "시세 없는 것 " + spent.missing() + "개 제외" : spent.missing() + " without price excluded"}@endif</span></summary>
				<div class="px-3 pb-3 overflow-x-auto">
					<table class="table table-xs">
						<thead><tr><th>${ko ? "화폐" : "Currency"}</th><th class="text-right">${ko ? "개수" : "Count"}</th><th class="text-right">${ko ? "단가(div)" : "Unit (div)"}</th><th class="text-right">${ko ? "합계(div)" : "Subtotal (div)"}</th></tr></thead>
						<tbody>
							@for(Poe2Craft.SpentLine sl : spent.lines())
								!{ String sk = spKo.get(sl.name()); }
								<tr data-craft-spent-line="${sl.name()}">
									<td class="wrap-anywhere">${ko ? (sl.nameKo() != null ? sl.nameKo() : sk != null ? sk : sl.name()) : sl.name()}</td>
									<td class="text-right font-mono">${sl.count() == Math.rint(sl.count()) ? String.valueOf((long) sl.count()) : String.format("%.1f", sl.count())}</td>
									<td class="text-right font-mono">${fmt.apply(sl.unitDivine())}</td>
									<td class="text-right font-mono">${fmt.apply(sl.subtotalDivine())}</td>
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
