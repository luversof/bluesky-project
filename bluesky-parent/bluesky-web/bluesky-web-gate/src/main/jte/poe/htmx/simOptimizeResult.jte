@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeBuild
@import net.luversof.web.gate.poe.dto.PoeOptimizeResult

@param PoeOptimizeResult result = null
@param java.util.Map<String, String> tattooIcons = null
@param net.luversof.web.gate.poe.dto.ArchetypeBenchmark ninjaBenchmark = null
@param java.util.Map<String, net.luversof.web.gate.poe.dto.PoeBaseItem> rareBases = null

!{
	// 결과는 실행 시점 이름으로 저장되므로 API 가 한/영을 함께 싣는다 — 화면은 로케일대로 고르기만 한다.
	// ⚠ 여러 섹션(노터블·마스터리·문신)이 쓰므로 **파일 최상단**에 둔다. 첫 섹션 안에 두면 뒤 섹션에서 스코프 밖이다.
	// 구 히스토리(영문 목록 없음)는 한글로 폴백 — 옛 결과가 빈칸이 되면 안 된다.
	java.util.function.BiFunction<java.util.List<String>, java.util.List<String>, java.util.List<String>> pickList =
		(koList, enList) -> !net.luversof.web.gate.poe.PoeText.isKorean() && enList != null && !enList.isEmpty() ? enList : koList;
	java.util.List<String> dNotables = pickList.apply(result.treeNotables(), result.treeNotablesEn());
	java.util.List<String> dMasteryLabels = pickList.apply(result.treeMasteryLabels(), result.treeMasteryLabelsEn());
	java.util.List<String> dTattooLabels = pickList.apply(result.treeTattooLabels(), result.treeTattooLabelsEn());
	// 티어 비교 헤더는 T1/중간/하위 **고정 3값**이라 DTO 를 늘리지 않고 여기서 옮긴다(구 히스토리도 함께 해결된다).
	java.util.Map<String, String> tierLabelEn = java.util.Map.of("T1", "T1", "중간", "Mid", "하위", "Low");
	java.util.function.Function<String, String> tierLabel =
		l -> net.luversof.web.gate.poe.PoeText.isKorean() ? l : tierLabelEn.getOrDefault(l, l);
}

@if(result != null)
	!{
		String gemLabel = net.luversof.web.gate.poe.PoeText.name(result.gemNameKo(), result.gemName());
		String objectiveLabel = MessageUtil.getMessage("poe.sim.opt.objective." + result.objective());
		String nodesParam = String.join(",", result.treeNodeIds().stream().map(String::valueOf).toList());
		// 트리 에디터가 직업/전직까지 복원하도록 함께 넘긴다(안 넘기면 사이온으로 열려 편집 시 트리가 날아감)
		String treeLink = "/poe/tree?nodes=" + nodesParam
			+ (result.className() != null ? "&class=" + java.net.URLEncoder.encode(result.className(), java.nio.charset.StandardCharsets.UTF_8) : "")
			+ (result.ascendancy() != null ? "&asc=" + java.net.URLEncoder.encode(result.ascendancy(), java.nio.charset.StandardCharsets.UTF_8) : "")
			// 클러스터/주얼 구성도 함께 — 없으면 트리 화면에서 생성 노드가 사라져 결과 수치와 어긋난다
			+ (result.treeClusters() != null && !result.treeClusters().isBlank() ? "&c=" + java.net.URLEncoder.encode(result.treeClusters(), java.nio.charset.StandardCharsets.UTF_8) : "")
			+ (result.treeJewels() != null && !result.treeJewels().isBlank() ? "&j=" + java.net.URLEncoder.encode(result.treeJewels(), java.nio.charset.StandardCharsets.UTF_8) : "")
			+ (result.treeTattoos() != null && !result.treeTattoos().isBlank() ? "&tt=" + java.net.URLEncoder.encode(result.treeTattoos(), java.nio.charset.StandardCharsets.UTF_8) : "")
			// 마스터리 선택 — 빠지면 트리 화면 평가가 결과보다 약하게 나온다(표시=실제)
			+ (result.treeMasteries() != null && !result.treeMasteries().isBlank() ? "&masteries=" + java.net.URLEncoder.encode(result.treeMasteries(), java.nio.charset.StandardCharsets.UTF_8) : "")
			// 도유 — 빠지면 트리 화면에서 도유 노터블이 사라져 결과 수치와 어긋난다
			+ (result.treeAnoint() != null ? "&an=" + result.treeAnoint() : "")
			// 혈맹 — 안 실으면 혈맹 서브트리 노드가 트리 화면에서 통째로 숨는다(노터블 focus 실패 실사고)
			+ (result.bloodline() != null ? "&bloodline=" + java.net.URLEncoder.encode(result.bloodline(), java.nio.charset.StandardCharsets.UTF_8) : "");
		String treeLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.tree"), result.treeNodeIds().size());
		String durationLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.duration"), result.durationMs() / 1000, result.evalCount());
		// 현실성 점검 — 원소 저항 미달(75 미만)이나 유효 체력 바닥이면 인게임에서 못 쓰는 유리대포다
		java.util.function.Function<String, Integer> statInt = key -> {
			for (PoeBuild.PlayerStat st : result.stats()) {
				if (st.key().equals(key)) {
					try { return (int) Double.parseDouble(st.value().replace(",", "")); } catch (Exception e) { return null; }
				}
			}
			return null;
		};
		Integer fireRes = statInt.apply("fireresist");
		Integer coldRes = statInt.apply("coldresist");
		Integer lightRes = statInt.apply("lightningresist");
		Integer ehpValue = statInt.apply("totalehp");
		java.util.List<String> uncapped = new java.util.ArrayList<>();
		if (fireRes != null && fireRes < 75) uncapped.add(MessageUtil.getMessage("poe.sim.opt.result.defense.fire"));
		if (coldRes != null && coldRes < 75) uncapped.add(MessageUtil.getMessage("poe.sim.opt.result.defense.cold"));
		if (lightRes != null && lightRes < 75) uncapped.add(MessageUtil.getMessage("poe.sim.opt.result.defense.lightning"));
		Integer netRegen = statInt.apply("netliferegen");
		boolean unsustainable = netRegen != null && netRegen < 0;
		boolean fragile = (ehpValue != null && ehpValue < 10000) || !uncapped.isEmpty() || unsustainable;
		// 스탯 셀 위험 색 — PoB 처럼 미달 저항은 빨강, 음수 순생명재생도 빨강. 그 외는 중립.
		java.util.function.BiFunction<String, String, String> statColor = (key, val) -> {
			Integer n;
			try { n = (int) Double.parseDouble(val.replace(",", "").replace("x", "")); } catch (Exception e) { return ""; }
			switch (key) {
				case "fireresist": case "coldresist": case "lightningresist":
					return n < 75 ? "text-rose-500" : "";
				case "netliferegen":
					return n < 0 ? "text-rose-500" : "";
				default:
					return "";
			}
		};
	}
	<div class="space-y-3 rounded-box border border-primary/25 bg-primary/5 p-4 mt-2">
		<div class="flex flex-wrap items-center gap-2">
			<span class="text-sm font-bold text-primary">${MessageUtil.getMessage("poe.sim.opt.result.title")}</span>
			<span class="font-bold">${gemLabel}</span>
			<span class="badge badge-primary badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(result.classNameKo(), result.className())}${result.ascendancyKo() != null ? " · " + net.luversof.web.gate.poe.PoeText.name(result.ascendancyKo(), result.ascendancy()) : ""}</span>
			@if(result.bloodline() != null)
				<span class="badge badge-sm text-rose-500 border-rose-500/40">${MessageUtil.getMessage("poe.sim.opt.result.bloodline")}: ${net.luversof.web.gate.poe.PoeText.name(result.bloodlineKo(), result.bloodline())}</span>
			@endif
			<span class="badge badge-ghost badge-sm">${objectiveLabel}</span>
			@if(result.scenarioKo() != null)
				<span class="badge badge-error badge-outline badge-sm">${MessageUtil.getMessage("poe.sim.opt.scenario")}: ${net.luversof.web.gate.poe.PoeText.name(result.scenarioKo(), result.scenario())}</span>
			@endif
			@if(result.combatBuffs())
				<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.sim.opt.buffs")}</span>
			@endif
			<span class="badge badge-ghost badge-sm font-mono">${durationLabel}</span>
		</div>

		@if(fragile)
			<div class="flex flex-wrap items-center gap-2 rounded border border-warning/50 bg-warning/10 px-3 py-2 text-xs">
				<span class="font-bold text-warning">${MessageUtil.getMessage("poe.sim.opt.result.fragile")}</span>
				@if(!uncapped.isEmpty())
					<span class="text-base-content/70">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.fragile.res"), String.join(", ", uncapped))}</span>
				@endif
				@if(ehpValue != null && ehpValue < 10000)
					<span class="text-base-content/70">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.fragile.ehp"), ehpValue)}</span>
				@endif
				@if(unsustainable)
					<span class="font-semibold text-rose-500">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.unsustainable"), netRegen)}</span>
				@endif
				@if("dps".equals(result.objective()))
					<span class="text-base-content/50 ml-auto">${MessageUtil.getMessage("poe.sim.opt.result.fragile.hint")}</span>
				@else
					<span class="text-base-content/50 ml-auto">${MessageUtil.getMessage("poe.sim.opt.result.fragile.hint.balanced")}</span>
				@endif
			</div>
		@endif

		<%-- P3 메타 기준선 게이트 — 공격·생존 모두 실빌드 중앙값 하회(지배당함)면 명시 경고. 비교표에서 축별 확인. --%>
		@if(Boolean.TRUE.equals(result.belowMeta()))
			<div class="flex flex-wrap items-center gap-2 rounded border border-error/50 bg-error/10 px-3 py-2 text-xs">
				<span class="font-bold text-error">${net.luversof.web.gate.poe.PoeText.isKorean() ? "실빌드 중앙값 하회" : "Below real-build median"}</span>
				<span class="text-base-content/70">${net.luversof.web.gate.poe.PoeText.isKorean() ? "이 결과는 같은 스킬×전직의 poe.ninja 실빌드 중앙값보다 공격(DPS)·생존(EHP) 모두 낮습니다 — 아래 비교표에서 부족 축을 확인하세요" : "Both DPS and EHP are below the poe.ninja real-build median for this skill × ascendancy — see the comparison table below"}</span>
			</div>
		@endif

		<div class="flex flex-wrap items-baseline gap-3">
			<span class="text-sm text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.baseline")} <span class="font-mono">${result.baselineValue()}</span></span>
			<span class="text-lg font-bold font-mono text-primary">→ ${result.finalValue()}</span>
			<span class="text-[11px] text-base-content/40">${net.luversof.web.gate.poe.PoeText.isKorean() ? "(모든 레어 최상위 티어 가정)" : "(all rares at top tier)"}</span>
		</div>

		<%-- poe.ninja 실빌드 벤치마크 — 이 (전직×스킬)의 실 유저 빌드 중앙값과 비교. 결과가 실데이터에 근거함을 보여준다. --%>
		@if(ninjaBenchmark != null && ninjaBenchmark.sample() > 0)
			!{
				boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
				java.util.function.LongFunction<String> fmt = n -> {
					if (n >= 1_000_000_000L) return String.format("%.1fB", n / 1_000_000_000.0);
					if (n >= 1_000_000L) return String.format("%.1fM", n / 1_000_000.0);
					if (n >= 1_000L) return String.format("%.0fk", n / 1_000.0);
					return String.valueOf(n);
				};
				Integer myLife = statInt.apply("life");
				Integer myEhp = statInt.apply("totalehp");
				Integer myDps = statInt.apply("combineddps");
				// 생명 재생 — RF 등 지속형은 이게 생존 핵심. 순재생(net) 우선, 없으면 총재생(gross). 실빌드 medianLifeRegen 과 비교.
				Integer myRegen = statInt.apply("netliferegen");
				if (myRegen == null) myRegen = statInt.apply("liferegen");
				// 실빌드 중앙값 대비 60% 미만이면 "낮음"(참고용, 강제 아님)
				java.util.function.BiFunction<Integer, Long, Boolean> low = (mine, med) -> mine != null && med > 0 && mine < med * 0.6;
				// 실빌드 성향(lean): 이 (전직×스킬)을 실제 유저들이 공격특화/생존특화/균형 중 어디로 굴리는지. 시뮬 목표의 근거.
				String lean = ninjaBenchmark.lean();
				String leanLabel = "dps".equals(lean) ? (ko ? "공격 특화 (고DPS·저EHP)" : "Offense (high DPS)")
					: "ehp".equals(lean) ? (ko ? "생존 특화 (고EHP)" : "Defense (high EHP)")
					: (ko ? "균형형" : "Balanced");
				String leanCls = "dps".equals(lean) ? "badge-error" : "ehp".equals(lean) ? "badge-info" : "badge-ghost";
				// 전 컬럼 특화 판정(키→로케일 라벨). 어느 컬럼이 두드러진 실빌드인지 배지로.
				java.util.function.Function<String, String> specLabel = k -> switch (k) {
					case "dps" -> ko ? "공격(DPS)" : "DPS";
					case "ehp" -> ko ? "생존(EHP)" : "EHP";
					case "life" -> ko ? "생명" : "Life";
					case "es" -> ko ? "에너지실드" : "ES";
					case "liferegen" -> ko ? "생명재생" : "Life regen";
					case "armour" -> ko ? "방어도" : "Armour";
					case "evasion" -> ko ? "회피" : "Evasion";
					case "block" -> ko ? "막기" : "Block";
					case "suppress" -> ko ? "회피억제" : "Suppress";
					case "maxres" -> ko ? "최대저항" : "Max res";
					case "ward" -> ko ? "수호물" : "Ward";
					case "mana" -> ko ? "마나" : "Mana";
					case "rarity" -> ko ? "아이템희귀도" : "Item rarity";
					case "movespeed" -> ko ? "이동속도" : "Move speed";
					case "str" -> ko ? "힘" : "Str";
					case "dex" -> ko ? "민첩" : "Dex";
					case "int" -> ko ? "지능" : "Int";
					case "cluster" -> ko ? "클러스터주얼" : "Cluster jewels";
					case "unique" -> ko ? "유니크장비" : "Unique gear";
					case "spellblock" -> ko ? "주문막기" : "Spell block";
					case "spelldodge" -> ko ? "주문회피" : "Spell dodge";
					case "phystaken" -> ko ? "물리피해전환" : "Phys taken as";
					case "echarge" -> ko ? "인내충전" : "Endur. charges";
					case "fcharge" -> ko ? "격노충전" : "Frenzy charges";
					case "pcharge" -> ko ? "권능충전" : "Power charges";
					case "mirror" -> ko ? "미러장비" : "Mirrored gear";
					default -> k;
				};
				// ── 한눈 비교표(사용자 요청): 지표별 실빌드 중앙값 vs 내 결과 vs 대비. ──
				//   행 = {라벨, 실빌드, 내, 대비, 색}. 매그니튜드는 %대비(상회=success, 60%미만=warning),
				//   캡형(%지표)은 포인트차. 내 값이 없는 지표는 "-" 로 두되 실빌드 값은 보여준다.
				Integer myEs = statInt.apply("energyshield");
				Integer myArmour = statInt.apply("armour");
				Integer myEvasion = statInt.apply("evasion");
				Integer myMana = statInt.apply("mana");
				Integer myBlock = statInt.apply("blockchance");
				Integer mySuppress = statInt.apply("spellsuppressionchance");
				Integer myFireRes = statInt.apply("fireresist");
				Integer myColdRes = statInt.apply("coldresist");
				Integer myLightRes = statInt.apply("lightningresist");
				Integer myChaosRes = statInt.apply("chaosresist");
				// 내 최대피격 — 결과 defenseHits(type→값)에서. 실빌드 medianXxxMax 와 직접 비교 가능.
				java.util.Map<String, Long> myHits = new java.util.HashMap<>();
				if (result.defenseHits() != null) {
					for (var dh : result.defenseHits()) {
						try { myHits.put(dh.type(), Long.parseLong(dh.value().replace(",", ""))); } catch (Exception e) { /* skip */ }
					}
				}
				Long myLowestHit = myHits.values().stream().min(Long::compare).orElse(null);
				// ── 비교표 2단(좌=공격·생존, 우=방어층·자원·속성·기타) — 벤치의 **모든 수치 컬럼**을 표로 통합. ──
				//   행 = {라벨, 실빌드, 내, 대비, 색}. 내 값이 없는 컬럼(이동속도·충전 등 엔진 미노출)은 "—" + 대비 공란.
				java.util.List<String[]> cmpL = new java.util.ArrayList<>();
				java.util.List<String[]> cmpR = new java.util.ArrayList<>();
				// v = {실빌드, 내(없으면 Long.MIN_VALUE), 캡형(%지표)이면 1}
				java.util.function.BiConsumer<java.util.List<String[]>, Object[]> addCmp = (list, a) -> {
					String label = (String) a[0];
					long bench = (long) a[1];
					Long mine = (Long) a[2];
					boolean cap = (boolean) a[3];
					if (bench <= 0 && (mine == null || mine <= 0)) return; // 양쪽 다 없으면 행 생략
					String benchS = cap ? bench + "%" : fmt.apply(bench);
					String mineS = mine == null ? "—" : (cap ? mine + "%" : fmt.apply(mine));
					String delta = "";
					String cls = "text-base-content/40";
					if (mine != null && bench > 0) {
						if (cap) {
							long d = mine - bench;
							delta = (d >= 0 ? "+" : "") + d + "p";
							cls = d >= 0 ? "text-success" : d >= -10 ? "text-base-content/60" : "text-warning";
						} else {
							long p = Math.round(mine * 100.0 / bench) - 100;
							delta = (p >= 0 ? "+" : "") + p + "%";
							cls = p >= 0 ? "text-success" : p >= -40 ? "text-base-content/60" : "text-warning";
						}
					}
					list.add(new String[] {label, benchS, mineS, delta, cls});
				};
				// 실빌드 전용 문자열 행(내 값 개념이 없는 컬럼 — 충전/클러스터 구성 등)
				java.util.function.BiConsumer<java.util.List<String[]>, String[]> addInfo = (list, a) ->
					list.add(new String[] {a[0], a[1], "—", "", "text-base-content/40"});
				Long L = null; // 캐스팅 편의
				// ── 좌: 공격·생존 ──
				addCmp.accept(cmpL, new Object[] {ko ? "DPS(메인 스킬)" : "DPS (main)", ninjaBenchmark.dps(), myDps == null ? null : Long.valueOf(myDps), false});
				if (ninjaBenchmark.skillDps() != null) {
					for (int si = 1; si < ninjaBenchmark.skillDps().size(); si++) {
						var sd = ninjaBenchmark.skillDps().get(si);
						addInfo.accept(cmpL, new String[] {"DPS·" + sd.name(), fmt.apply(sd.dps())});
					}
				}
				addCmp.accept(cmpL, new Object[] {"EHP", ninjaBenchmark.ehp(), myEhp == null ? null : Long.valueOf(myEhp), false});
				addCmp.accept(cmpL, new Object[] {ko ? "생명" : "Life", ninjaBenchmark.life(), myLife == null ? null : Long.valueOf(myLife), false});
				addCmp.accept(cmpL, new Object[] {ko ? "에너지실드" : "ES", ninjaBenchmark.energyShield(), myEs == null ? null : Long.valueOf(myEs), false});
				addCmp.accept(cmpL, new Object[] {ko ? "생명재생/초" : "Life regen/s", ninjaBenchmark.lifeRegen(), myRegen == null ? null : Long.valueOf(myRegen), false});
				addCmp.accept(cmpL, new Object[] {ko ? "최대피격(물리)" : "Max hit (phys)", ninjaBenchmark.physicalMax(), myHits.get("physical"), false});
				addCmp.accept(cmpL, new Object[] {ko ? "최대피격(화염)" : "Max hit (fire)", ninjaBenchmark.fireMax(), myHits.get("fire"), false});
				addCmp.accept(cmpL, new Object[] {ko ? "최대피격(냉기)" : "Max hit (cold)", ninjaBenchmark.coldMax(), myHits.get("cold"), false});
				addCmp.accept(cmpL, new Object[] {ko ? "최대피격(번개)" : "Max hit (light)", ninjaBenchmark.lightningMax(), myHits.get("lightning"), false});
				addCmp.accept(cmpL, new Object[] {ko ? "최대피격(카오스)" : "Max hit (chaos)", ninjaBenchmark.chaosMax(), myHits.get("chaos"), false});
				addCmp.accept(cmpL, new Object[] {ko ? "최약 최대피격" : "Lowest max hit", ninjaBenchmark.lowestMax(), myLowestHit, false});
				// ── 우: 방어층·자원·속성·기타 ──
				addCmp.accept(cmpR, new Object[] {ko ? "방어도" : "Armour", ninjaBenchmark.armour(), myArmour == null ? null : Long.valueOf(myArmour), false});
				addCmp.accept(cmpR, new Object[] {ko ? "회피" : "Evasion", ninjaBenchmark.evasion(), myEvasion == null ? null : Long.valueOf(myEvasion), false});
				addCmp.accept(cmpR, new Object[] {ko ? "막기" : "Block", (long) ninjaBenchmark.block(), myBlock == null ? null : Long.valueOf(myBlock), true});
				addCmp.accept(cmpR, new Object[] {ko ? "주문막기" : "Spell block", (long) ninjaBenchmark.spellBlock(), L, true});
				addCmp.accept(cmpR, new Object[] {ko ? "회피억제" : "Suppress", (long) ninjaBenchmark.suppress(), mySuppress == null ? null : Long.valueOf(mySuppress), true});
				addCmp.accept(cmpR, new Object[] {ko ? "주문회피" : "Spell dodge", (long) ninjaBenchmark.spellDodge(), L, true});
				addCmp.accept(cmpR, new Object[] {ko ? "물리피해전환" : "Phys taken as", (long) ninjaBenchmark.physTakenAs(), L, true});
				addCmp.accept(cmpR, new Object[] {ko ? "화염 저항" : "Fire res", (long) ninjaBenchmark.fireRes(), myFireRes == null ? null : Long.valueOf(myFireRes), true});
				addCmp.accept(cmpR, new Object[] {ko ? "냉기 저항" : "Cold res", (long) ninjaBenchmark.coldRes(), myColdRes == null ? null : Long.valueOf(myColdRes), true});
				addCmp.accept(cmpR, new Object[] {ko ? "번개 저항" : "Light res", (long) ninjaBenchmark.lightningRes(), myLightRes == null ? null : Long.valueOf(myLightRes), true});
				addCmp.accept(cmpR, new Object[] {ko ? "카오스 저항" : "Chaos res", (long) ninjaBenchmark.chaosRes(), myChaosRes == null ? null : Long.valueOf(myChaosRes), true});
				addCmp.accept(cmpR, new Object[] {ko ? "마나" : "Mana", ninjaBenchmark.mana(), myMana == null ? null : Long.valueOf(myMana), false});
				addCmp.accept(cmpR, new Object[] {ko ? "수호물" : "Ward", ninjaBenchmark.ward(), L, false});
				addCmp.accept(cmpR, new Object[] {ko ? "힘" : "Str", (long) ninjaBenchmark.str(), statInt.apply("str") == null ? null : Long.valueOf(statInt.apply("str")), false});
				addCmp.accept(cmpR, new Object[] {ko ? "민첩" : "Dex", (long) ninjaBenchmark.dex(), statInt.apply("dex") == null ? null : Long.valueOf(statInt.apply("dex")), false});
				addCmp.accept(cmpR, new Object[] {ko ? "지능" : "Int", (long) ninjaBenchmark.intel(), statInt.apply("int") == null ? null : Long.valueOf(statInt.apply("int")), false});
				addCmp.accept(cmpR, new Object[] {ko ? "이동속도" : "Move speed", (long) ninjaBenchmark.movementSpeed(), L, true});
				addCmp.accept(cmpR, new Object[] {ko ? "아이템 희귀도" : "Item rarity", (long) ninjaBenchmark.itemRarity(), L, true});
				if (ninjaBenchmark.enduranceCharges() + ninjaBenchmark.frenzyCharges() + ninjaBenchmark.powerCharges() > 0) {
					addInfo.accept(cmpR, new String[] {ko ? "충전(인/격/권)" : "Charges (E/F/P)", ninjaBenchmark.enduranceCharges() + "/" + ninjaBenchmark.frenzyCharges() + "/" + ninjaBenchmark.powerCharges()});
				}
				if (ninjaBenchmark.largeCluster() + ninjaBenchmark.mediumCluster() + ninjaBenchmark.smallCluster() > 0) {
					addInfo.accept(cmpR, new String[] {ko ? "클러스터(대/중/소)" : "Cluster (L/M/S)", ninjaBenchmark.largeCluster() + "/" + ninjaBenchmark.mediumCluster() + "/" + ninjaBenchmark.smallCluster()});
				}
				if (ninjaBenchmark.uniqueEquip() > 0) {
					addInfo.accept(cmpR, new String[] {ko ? "유니크 장비" : "Unique gear", String.valueOf(ninjaBenchmark.uniqueEquip())});
				}
				int mirrorSum = ninjaBenchmark.mirroredItems() + ninjaBenchmark.mirroredWeapons() + ninjaBenchmark.mirroredArmours();
				if (mirrorSum > 0) {
					addInfo.accept(cmpR, new String[] {ko ? "미러 장비" : "Mirrored gear", String.valueOf(mirrorSum)});
				}
			}
			<div class="rounded-box bg-base-100 px-3 py-2 text-xs space-y-1">
				<div class="flex items-center gap-2">
					<span class="font-bold text-info">${ko ? "poe.ninja 실빌드 중앙값" : "poe.ninja real-build median"}</span>
					<span class="badge badge-ghost badge-xs">${ninjaBenchmark.mainSkill()}${ninjaBenchmark.ascendancy() != null && !ninjaBenchmark.ascendancy().isBlank() ? " · " + ninjaBenchmark.ascendancy() : ""}</span>
					@if(lean != null)
						<%-- 실빌드 성향 배지 — 이 조합의 실 유저 분포가 공격/생존 어느 쪽인지(시뮬이 미는 방향의 근거) --%>
						<span class="badge badge-xs ${leanCls}" title="${ko ? "poe.ninja 실빌드 DPS·EHP 중앙값을 전체 아키타입과 비교한 성향" : "Real-build DPS/EHP tendency vs all archetypes"}">${leanLabel}</span>
					@endif
					<span class="text-base-content/40">n=${ninjaBenchmark.sample()}</span>
				</div>
				<%-- 전 컬럼 참조 특화 판정 — 이 스킬×전직 실빌드가 두드러진 컬럼(전체 중앙값 대비, 저표본 제외). "어느 컬럼 특화인지"를 명시. --%>
				@if(ninjaBenchmark.specializations() != null && !ninjaBenchmark.specializations().isEmpty())
					<div class="flex flex-wrap items-center gap-1">
						<span class="text-base-content/50">${ko ? "특화 컬럼" : "Specializes in"}</span>
						@for(String sk : ninjaBenchmark.specializations())
							<span class="badge badge-xs badge-warning badge-outline">${specLabel.apply(sk)}</span>
						@endfor
					</div>
				@endif
				<%-- ★ 한눈 비교표(사용자 요청) — poe.ninja 전 컬럼을 두 표로 통합(좌=공격·생존, 우=방어층·자원·속성·기타).
				     산재 텍스트 행 없이 모든 수치가 실빌드|내|대비 형식. 내 값이 없는 컬럼(엔진 미노출)은 "—". --%>
				<div class="grid gap-x-6 md:grid-cols-2">
					@for(java.util.List<String[]> side : java.util.List.of(cmpL, cmpR))
						<div class="overflow-x-auto">
							<table class="table table-xs">
								<thead>
									<tr class="text-base-content/50">
										<th class="font-sans">${ko ? "지표" : "Metric"}</th>
										<th class="text-right font-sans">${ko ? "실빌드" : "Real"}</th>
										<th class="text-right font-sans">${ko ? "내 결과" : "Mine"}</th>
										<th class="text-right font-sans">${ko ? "대비" : "Δ"}</th>
									</tr>
								</thead>
								<tbody class="font-mono">
									@for(String[] row : side)
										<tr>
											<td class="font-sans text-base-content/70">${row[0]}</td>
											<td class="text-right text-base-content/60">${row[1]}</td>
											<td class="text-right">${row[2]}</td>
											<td class="text-right ${row[4]}">${row[3]}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					@endfor
				</div>
				@if(ninjaBenchmark.skillDps() != null && ninjaBenchmark.skillDps().size() > 1)
					<div class="text-[10px] text-base-content/40">${ko ? "DPS(메인 스킬)·내 DPS 는 메인 스킬 전용 — 추가 스킬 DPS 는 실빌드 참고값(DPS·스킬명 행)" : "Main-skill DPS on both sides; extra-skill rows are real-build reference"}</div>
				@endif
				@if(ninjaBenchmark.topKeystones() != null && !ninjaBenchmark.topKeystones().isEmpty())
					<div class="text-base-content/50">${ko ? "실빌드 키스톤" : "common keystones"}: ${String.join(", ", ninjaBenchmark.topKeystones())}</div>
				@endif
				@if(ninjaBenchmark.topCoSkills() != null && !ninjaBenchmark.topCoSkills().isEmpty())
					<div class="text-base-content/50">${ko ? "함께 쓰는 스킬" : "common co-skills"}: ${String.join(", ", ninjaBenchmark.topCoSkills())}</div>
				@endif
				<%-- 실빌드 마스터리(캐릭터 상세 채집) — 패싯이 없을 때만(패싯 패널이 모집단 기준이라 우선) --%>
				@if(ninjaBenchmark.topMasteries() != null && !ninjaBenchmark.topMasteries().isEmpty() && (ninjaBenchmark.facets() == null || !ninjaBenchmark.facets().containsKey("masteries")))
					<div class="text-base-content/50">${ko ? "실빌드 마스터리" : "common masteries"}: ${String.join(" · ", ninjaBenchmark.topMasteries())}</div>
				@endif
				<%-- 패싯(사이드바 집계) — 마스터리/룬크래프트/문신/무기/판테온/아틀라스 등 전체 모집단 사용률 --%>
				@template.poe.htmx.benchFacets(bench = ninjaBenchmark, rows = 5)
			</div>
		@endif

		<%-- 어픽스 예산 축: "필수 옵션 수"에 따른 현실적 DPS 곡선 — 레어가 있을 때만. 필수 많을수록 finalValue 에 수렴 --%>
		@if(result.affixBudget() != null && !result.affixBudget().isEmpty())
			<div class="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-box bg-base-100 px-3 py-2 text-sm"
			     title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "레어 아이템의 필수 옵션 N개만 최상위 티어로 확보하고 나머지는 중위 티어라고 가정했을 때의 값. 실제 제작 난이도에 가까운 현실적 기대치." : "Value assuming only N essential rare mods reach top tier, the rest are mid-tier"}">
				<span class="font-semibold text-base-content/70">${net.luversof.web.gate.poe.PoeText.isKorean() ? "어픽스 예산(현실적 제작)" : "Affix budget (realistic)"}</span>
				@for(var point : result.affixBudget())
					<span class="flex items-baseline gap-1">
						<span class="text-[11px] text-base-content/50">${net.luversof.web.gate.poe.PoeText.isKorean() ? "필수 " + point.essentialCount() + "개" : point.essentialCount() + " essential"}</span>
						<span class="font-mono text-warning">${point.value()}</span>
					</span>
				@endfor
			</div>
		@endif

		@if(!result.stats().isEmpty())
			<%-- 오프셋(공격) 스탯이 0이면 숨김 — DoT/RF 빌드에서 총DPS·평균피해·공격속도·치명타 0 등은 노이즈(PoB 도 미표시).
			     저항·방어·생명·순생명재생 등 방어/유틸 스탯은 0/음수도 중요하므로 항상 표시. --%>
			!{ java.util.Set<String> hideIfZero = java.util.Set.of("totaldps", "averagedamage", "speed", "energyshield", "mana", "critchance"); }
			<div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-2">
				@for(PoeBuild.PlayerStat stat : result.stats())
					!{ String sv = stat.value() == null ? "" : stat.value().replace(",", "").trim(); boolean isZero = sv.equals("0") || sv.equals("0.0") || sv.equals("0.00"); }
					@if(!(hideIfZero.contains(stat.key()) && isZero))
						<div class="rounded-box bg-base-100 px-3 py-2">
							<div class="text-[11px] font-semibold text-base-content/50">${MessageUtil.getMessage("poe.build.stat." + stat.key())}</div>
							<div class="text-sm font-bold font-mono ${statColor.apply(stat.key(), stat.value())}">${stat.value()}</div>
						</div>
					@endif
				@endfor
			</div>
		@endif

		@if(result.defenseHits() != null && !result.defenseHits().isEmpty())
			!{
				java.util.Map<String, String> defColor = java.util.Map.of(
					"physical", "poe-el-phys",
					"fire", "poe-el-fire",
					"cold", "poe-el-cold",
					"lightning", "poe-el-lightning",
					"chaos", "poe-el-chaos");
			}
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.defense")}</span>
				<div class="grid grid-cols-3 sm:grid-cols-5 gap-2">
					@for(PoeOptimizeResult.DefenseHit hit : result.defenseHits())
						<div class="rounded-box bg-base-100 px-3 py-2">
							<div class="text-[11px] font-semibold ${defColor.getOrDefault(hit.type(), "text-base-content/50")}">${MessageUtil.getMessage("poe.sim.opt.result.defense." + hit.type())}</div>
							<div class="text-sm font-bold font-mono">${hit.value()}</div>
						</div>
					@endfor
				</div>
			</div>
		@endif

		<%-- 보조젬 스킬 레이어 — 젬 아이콘 + 인게임 호버 툴팁(#poePreview) + 젬 상세 링크(젬 목록/랭킹과 동일 파리티) --%>
		@if(!result.supports().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.supports")}</span>
				@for(PoeOptimizeResult.SupportPick support : result.supports())
					@if(support.slug() != null && !support.slug().isBlank())
						<a class="poe-hover inline-flex items-center gap-1 badge badge-info badge-outline badge-sm cursor-pointer pl-1"
						   href="/poe/gems/${support.slug()}"
						   hx-get="/poe/htmx/gems/detail?slug=${support.slug()}"
						   hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">
							<img src="/poe-assets/gems/${support.slug()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}" class="w-4 h-4 object-contain shrink-0" alt="" loading="lazy" onerror="this.remove()">${net.luversof.web.gate.poe.PoeText.name(support.nameKo(), support.name())}</a>
					@else
						<span class="badge badge-info badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(support.nameKo(), support.name())}</span>
					@endif
				@endfor
			</div>
		@endif

		<%-- 오라/헤럴드 스킬 레이어 — 보조젬과 동형 --%>
		@if(result.auras() != null && !result.auras().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.auras")}</span>
				@for(PoeOptimizeResult.SupportPick aura : result.auras())
					@if(aura.slug() != null && !aura.slug().isBlank())
						<a class="poe-hover inline-flex items-center gap-1 badge badge-success badge-outline badge-sm cursor-pointer pl-1"
						   href="/poe/gems/${aura.slug()}"
						   hx-get="/poe/htmx/gems/detail?slug=${aura.slug()}"
						   hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">
							<img src="/poe-assets/gems/${aura.slug()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}" class="w-4 h-4 object-contain shrink-0" alt="" loading="lazy" onerror="this.remove()">${net.luversof.web.gate.poe.PoeText.name(aura.nameKo(), aura.name())}</a>
					@else
						<span class="badge badge-success badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(aura.nameKo(), aura.name())}</span>
					@endif
				@endfor
			</div>
		@endif

		@if(result.blockedAuras() != null && !result.blockedAuras().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1" title="${MessageUtil.getMessage("poe.sim.opt.result.blocked.hint")}">${MessageUtil.getMessage("poe.sim.opt.result.blocked")}</span>
				@for(PoeOptimizeResult.BlockedAura blocked : result.blockedAuras())
					<span class="badge badge-sm badge-ghost text-base-content/50 line-through decoration-base-content/30">${net.luversof.web.gate.poe.PoeText.name(blocked.nameKo(), blocked.name())}</span>
					<span class="text-[10px] font-mono text-base-content/40 -ml-1">−${blocked.shortfall()}</span>
				@endfor
			</div>
		@endif

		<!-- 속성 부족으로 실제로는 못 입는 장비 — 조용히 두면 게임에서 쓸 수 없는 빌드를 그대로 믿게 된다 -->
		@if(result.unmetRequirements() != null && !result.unmetRequirements().isEmpty())
			<div class="rounded border border-warning/40 bg-warning/10 px-3 py-2 text-xs space-y-1">
				<div class="font-semibold text-warning">${MessageUtil.getMessage("poe.sim.opt.result.unmet")}</div>
				@for(PoeOptimizeResult.UnmetRequirement unmet : result.unmetRequirements())
					<div class="font-mono text-base-content/70">${net.luversof.web.gate.poe.PoeText.name(unmet.nameKo(), unmet.name())} — ${MessageUtil.getMessage("poe.build.stat." + unmet.attribute())} ${unmet.required()} / ${unmet.actual()}</div>
				@endfor
			</div>
		@endif

		<%-- 추가 스킬 레이어 — 보조젬/오라와 동형(젬 아이콘 + 인게임 호버 툴팁 #poePreview + 젬 상세 링크) --%>
		@if(result.additionalSkills() != null && !result.additionalSkills().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.extraskills")}</span>
				@for(PoeOptimizeResult.SupportPick extra : result.additionalSkills())
					@if(extra.slug() != null && !extra.slug().isBlank())
						<a class="poe-hover inline-flex items-center gap-1 badge badge-accent badge-outline badge-sm cursor-pointer pl-1"
						   href="/poe/gems/${extra.slug()}"
						   hx-get="/poe/htmx/gems/detail?slug=${extra.slug()}"
						   hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">
							<img src="/poe-assets/gems/${extra.slug()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}" class="w-4 h-4 object-contain shrink-0" alt="" loading="lazy" onerror="this.remove()">${net.luversof.web.gate.poe.PoeText.name(extra.nameKo(), extra.name())}</a>
					@else
						<span class="badge badge-accent badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(extra.nameKo(), extra.name())}</span>
					@endif
					<%-- 추가 스킬 전용 보조젬(1b 선발) — 계산엔 이미 링크됐는데 표시가 없어 "화염덫 보조젬 0"으로 보였다(사용자 지적) --%>
					!{
						java.util.List<PoeOptimizeResult.SupportPick> extraLinks =
							result.additionalSkillSupports() != null ? result.additionalSkillSupports().get(extra.slug()) : null;
					}
					@if(extraLinks != null && !extraLinks.isEmpty())
						<span class="text-base-content/40 text-xs">→</span>
						@for(PoeOptimizeResult.SupportPick linkGem : extraLinks)
							@if(linkGem.slug() != null && !linkGem.slug().isBlank())
								<a class="poe-hover inline-flex items-center gap-1 badge badge-outline badge-sm cursor-pointer pl-1"
								   href="/poe/gems/${linkGem.slug()}"
								   hx-get="/poe/htmx/gems/detail?slug=${linkGem.slug()}"
								   hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">
									<img src="/poe-assets/gems/${linkGem.slug()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}" class="w-4 h-4 object-contain shrink-0" alt="" loading="lazy" onerror="this.remove()">${net.luversof.web.gate.poe.PoeText.name(linkGem.nameKo(), linkGem.name())}</a>
							@else
								<span class="badge badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(linkGem.nameKo(), linkGem.name())}</span>
							@endif
						@endfor
					@endif
				@endfor
			</div>
		@endif


		@if(!result.treeNodeIds().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${treeLabel}</span>
				!{java.util.List<Integer> nids = result.treeNotableIds();}
				@for(int ni = 0; ni < dNotables.size(); ni++)
					!{int nid = nids != null && ni < nids.size() ? nids.get(ni) : 0;}
					@if(nid > 0)
						<%-- 노터블 클릭 → 트리에서 그 노드를 중앙에(도유 목록과 같은 focus 딥링크) --%>
						<a class="badge badge-warning badge-outline badge-sm link-hover" href="${treeLink}&focus=${nid}">${dNotables.get(ni)}</a>
					@else
						<span class="badge badge-warning badge-outline badge-sm">${dNotables.get(ni)}</span>
					@endif
				@endfor
				<a class="link link-primary text-sm ml-1" href="${treeLink}">${MessageUtil.getMessage("poe.build.summary.tree.view")}</a>
			</div>
		@endif

		<!-- 마스터리 선택 — 자동 채택된 효과("양손 숙련 — 적중 피해 60% 증가")가 안 보이면 어디서 온 수치인지 알 수 없다 -->
		@if(result.treeMasteryLabels() != null && !result.treeMasteryLabels().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${net.luversof.web.gate.poe.PoeText.isKorean() ? "마스터리" : "Masteries"}</span>
				<div class="flex flex-col gap-1">
					@for(String label : dMasteryLabels)
						<div class="flex items-baseline gap-2 text-sm">
							<span class="badge badge-outline badge-sm shrink-0 w-16 justify-center border-sky-400 text-sky-700 dark:text-sky-300">◇ ${net.luversof.web.gate.poe.PoeText.isKorean() ? "숙련" : "Mastery"}</span>
							<span class="text-sky-700 dark:text-sky-300 whitespace-normal">${label}</span>
						</div>
					@endfor
				</div>
			</div>
		@endif

		<!-- 문신 구성 — 문신은 패시브를 통째로 교체하므로, 안 보이면 트리 노드 목록과 실제 계산이 어긋나 보인다 -->
		@if(result.treeTattooLabels() != null && !result.treeTattooLabels().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${net.luversof.web.gate.poe.PoeText.isKorean() ? "문신" : "Tattoos"}</span>
				<div class="flex flex-col gap-1">
					@for(String label : dTattooLabels)
						!{
							String tattooNameKo = label.contains(" ×") ? label.substring(0, label.lastIndexOf(" ×")) : label;
							String tattooIconPath = tattooIcons != null ? tattooIcons.get(tattooNameKo) : null;
						}
						<div class="flex items-center gap-2 text-sm">
							<span class="badge badge-outline badge-sm shrink-0 w-16 justify-center border-purple-400 text-purple-700 dark:text-purple-300">${net.luversof.web.gate.poe.PoeText.isKorean() ? "문신" : "Tattoo"}</span>
							@if(tattooIconPath != null)
								<img src="/poe-assets/${tattooIconPath}" class="w-5 h-5 object-contain shrink-0" alt="" loading="lazy" onerror="this.remove()">
							@endif
							<%-- 문신 아이템 레이어 — 유니크/주얼과 동일한 인게임 호버 툴팁(#poePreview) + 사전 페이지 링크(href) --%>
							<a class="poe-hover text-purple-700 dark:text-purple-300 link-hover underline decoration-dotted cursor-pointer"
							   href="/poe/tattoos?q=${java.net.URLEncoder.encode(tattooNameKo, java.nio.charset.StandardCharsets.UTF_8)}"
							   hx-get="/poe/htmx/tattoos/detail?name=${java.net.URLEncoder.encode(tattooNameKo, java.nio.charset.StandardCharsets.UTF_8)}"
							   hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML"
							   title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "문신 사전에서 보기" : "Open in tattoos dictionary"}">${label}</a>
						</div>
					@endfor
				</div>
			</div>
		@endif

		<!-- 클러스터 주얼 구성 — 노터블만 보이면 "어떤 주얼로 얻은 건지"를 알 수 없다 -->
		@if(result.treeClusters() != null && !result.treeClusters().isBlank())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${net.luversof.web.gate.poe.PoeText.isKorean() ? "클러스터" : "Clusters"}</span>
				<div class="flex flex-col gap-1">
					@for(String entry : result.treeClusters().split(","))
						!{
							String[] parts = entry.split(":");
							String sizeKo = parts.length > 1 ? switch (parts[1]) { case "Large" -> "대형"; case "Medium" -> "중형"; case "Small" -> "소형"; default -> parts[1]; } : "";
							String sizeLabel = net.luversof.web.gate.poe.PoeText.isKorean() ? sizeKo : (parts.length > 1 ? parts[1] : "");
							String nodeCount = parts.length > 2 ? parts[2] : "";
						}
						<div class="flex items-center gap-2 text-sm">
							<span class="badge badge-secondary badge-outline badge-sm shrink-0 w-16 justify-center">${sizeLabel}</span>
							<span class="text-base-content/80">${nodeCount}${net.luversof.web.gate.poe.PoeText.isKorean() ? "노드" : ""}@if(parts.length > 4 && !parts[4].isBlank()) ·
								@for(String notable : parts[4].split("[|]"))
									<a class="link-hover underline decoration-dotted" href="/poe/clusters?q=${java.net.URLEncoder.encode(notable, java.nio.charset.StandardCharsets.UTF_8)}" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "클러스터 노터블 사전에서 보기" : "Open in cluster notables"}">${notable}</a>
								@endfor
							@endif</span>
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(result.jewels() != null && !result.jewels().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.jewels")}</span>
				<div class="flex flex-col gap-1">
					@for(PoeOptimizeResult.SupportPick jewel : result.jewels())
						<div class="flex items-center gap-2 text-sm">
							<span class="badge badge-outline badge-sm shrink-0 w-16 justify-center border-cyan-500/40 text-cyan-500">${net.luversof.web.gate.poe.PoeText.isKorean() ? "주얼" : "Jewel"}</span>
							@if(jewel.slug() != null && !jewel.slug().isBlank())
								<%-- 유니크 주얼 — 장비 유니크와 동일한 인게임 호버 툴팁(#poePreview) + 상세 페이지 링크(아이템 레이어 파리티) --%>
								<a class="poe-hover font-medium text-cyan-500 link-hover cursor-pointer"
								   href="/poe/uniques/${jewel.slug()}"
								   hx-get="/poe/htmx/uniques/detail?slug=${jewel.slug()}"
								   hx-target="#poePreview"
								   hx-trigger="mouseenter delay:100ms"
								   hx-swap="innerHTML">${net.luversof.web.gate.poe.PoeText.name(jewel.nameKo(), jewel.name())}</a>
							@elseif(jewel.lines() != null && !jewel.lines().isEmpty())
								<%-- 제작 레어 주얼 — 상세 페이지가 없어 이름만 뜨던 자리. 붙일 slug 가 없을 뿐
								     모드는 알고 있으므로, **장비 레어와 같은 인게임 호버 레이어**로 띄운다
								     (data-poe-tip 자식을 common.ts 가 #poePreview 로 복제 — 유니크 주얼과 동일한 레이어 파리티). --%>
								!{ java.util.List<String> jLines = net.luversof.web.gate.poe.PoeText.isKorean() || jewel.linesEn() == null || jewel.linesEn().isEmpty() ? jewel.lines() : jewel.linesEn(); }
								<span class="poe-hover cursor-pointer text-cyan-500">${net.luversof.web.gate.poe.PoeText.name(jewel.nameKo(), jewel.name())}<span data-poe-tip class="hidden">@template.poe.htmx.resultRareTooltip(name = jewel.name(), nameKo = jewel.nameKo(), modLines = jLines)</span></span>
							@else
								<span class="text-cyan-500">${net.luversof.web.gate.poe.PoeText.name(jewel.nameKo(), jewel.name())}</span>
							@endif
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(!result.items().isEmpty())
			!{ boolean resultHasFlask = result.items().stream().anyMatch(i -> i.slot() != null && i.slot().startsWith("Flask")); }
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.items")}</span>
				<%-- 플라스크 상시 활성은 PoB 빌드 계획의 표준 가정 — 명중 +2000 처럼 가정은 화면에 드러낸다(표시=실제) --%>
				@if(resultHasFlask)
					<span class="ml-2 text-[11px] text-base-content/40">${net.luversof.web.gate.poe.PoeText.isKorean() ? "· 플라스크는 상시 활성 가정(PoB 표준)" : "· Flasks assumed permanently active"}</span>
				@endif
				<%-- 인게임 캐릭터창식 페이퍼돌 — 슬롯을 실제 게임 배치로. 유니크는 slug 아이콘, 레어는 이름(테두리색=레어리티).
				     호버는 상세 리스트와 동일(유니크=#poePreview, 레어=베이스 상세 링크). 인증 슬롯 프레임 텍스처는 미추출이라 CSS 게임풍 박스로 대체. --%>
				!{
					java.util.Map<String, PoeOptimizeResult.ItemPick> dollBySlot = new java.util.HashMap<>();
					for (PoeOptimizeResult.ItemPick pick : result.items()) if (pick.slot() != null) dollBySlot.put(pick.slot(), pick);
					// {영문슬롯, left%, top%, width%, height%} — 공식 MainInventory.png(600x781) 장비영역 크롭(600x515) 기준.
					// 좌표는 눈대중이 아니라 배경 픽셀 밝기 프로파일로 리세스 경계를 검출해 도출(객관).
					String[][] dollSlots = {
						{"Weapon 1", "11.0", "21.6", "15.3", "36.3"}, {"Weapon 2", "73.0", "21.6", "15.3", "36.3"},
						{"Helmet", "42.0", "19.2", "15.3", "17.7"}, {"Body Armour", "42.0", "40.0", "15.3", "26.8"}, {"Belt", "42.0", "69.9", "15.3", "8.3"},
						{"Amulet", "61.8", "37.7", "6.5", "8.5"}, {"Ring 1", "30.7", "49.1", "6.7", "8.5"}, {"Ring 2", "61.8", "49.1", "6.5", "8.5"},
						{"Gloves", "22.8", "60.6", "14.5", "17.9"}, {"Boots", "61.7", "60.6", "14.8", "17.7"},
						{"Flask 1", "31.2", "81.2", "7.0", "17.7"}, {"Flask 2", "38.7", "81.2", "7.5", "17.7"}, {"Flask 3", "46.5", "81.2", "7.5", "17.7"},
						{"Flask 4", "54.5", "81.2", "7.5", "17.7"}, {"Flask 5", "62.3", "81.2", "7.7", "17.7"}
					};
				}
				<style>
					.poe-doll-slot{position:absolute;display:flex;align-items:center;justify-content:center;padding:.4%;border-radius:.2rem;text-decoration:none;overflow:hidden}
					.poe-doll-slot:hover{background:rgba(255,255,255,.06);box-shadow:inset 0 0 0 1px rgba(255,255,255,.22)}
					.poe-doll-slot img{max-width:100%;max-height:100%;object-fit:contain;filter:drop-shadow(0 1px 2px rgba(0,0,0,.75))}
				</style>
				<%-- 인게임 소지품 패널 배경(InventoryPanelUpperBackground) 위에 슬롯 좌표로 아이템을 절대배치.
				     크기는 실제 <img> 로 확정(aspect-ratio CSS 의존 제거 — 브라우저 무관 정렬 보장). 슬롯은 이 컨테이너(=이미지) % 좌표. --%>
				<div class="poe-doll mx-auto mb-2" style="position:relative;width:100%;max-width:34rem">
					<img src="/poe-data/ui-assets/inventory-panel.png" alt="" style="display:block;width:100%;height:auto">
					@for(String[] ds : dollSlots)
						!{ PoeOptimizeResult.ItemPick di = dollBySlot.get(ds[0]); }
						@if(di != null)
							!{ boolean dRare = "RARE".equals(di.rarity()); String dName = net.luversof.web.gate.poe.PoeText.name(di.nameKo(), di.name()); String pos = "left:" + ds[1] + "%;top:" + ds[2] + "%;width:" + ds[3] + "%;height:" + ds[4] + "%"; boolean smallSlot = Double.parseDouble(ds[3]) < 10; }
							@if(dRare)
								<%-- 레어: 베이스 아이콘 + 인게임 호버 툴팁. data-poe-tip 자식을 common.ts 가 #poePreview 로 복제해 띄운다(슬롯 overflow 클리핑 회피). 유니크와 동일한 레이어 파리티. --%>
								!{ java.util.List<String> dLines = net.luversof.web.gate.poe.PoeText.isKorean() || di.modLinesEn() == null || di.modLinesEn().isEmpty() ? di.modLines() : di.modLinesEn(); }
								<a class="poe-doll-slot poe-hover cursor-pointer" style="${pos}" href="${di.slug() != null ? "/poe/items/" + di.slug() : "#"}" title="${dName}">
									@if(di.slug() != null)
										<img src="/poe-assets/items/${di.slug()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}" alt="" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.classList.remove('hidden')">
										<span class="hidden max-h-full overflow-hidden text-[8px] font-semibold poe-name-rare text-center leading-[1.15]">${dName}</span>
									@else
										<span class="max-h-full overflow-hidden text-[8px] font-semibold poe-name-rare text-center leading-[1.15] drop-shadow-[0_1px_1px_rgba(0,0,0,0.95)]">${dName}</span>
									@endif
									<span data-poe-tip class="hidden">@template.poe.htmx.resultRareTooltip(name = di.name(), nameKo = di.nameKo(), modLines = dLines, modTiers = di.modTiers(), base = rareBases == null ? null : rareBases.get(di.slug()))</span>
								</a>
							@else
								<a class="poe-doll-slot poe-hover cursor-pointer" style="${pos}" href="/poe/uniques/${di.slug()}"
								   hx-get="/poe/htmx/uniques/detail?slug=${di.slug()}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML" title="${dName}">
									<img src="/poe-assets/uniques/${di.slug()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}" alt="" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.classList.remove('hidden')">
									<span class="hidden text-[8px] font-semibold poe-name-unique text-center leading-tight">${dName}</span>
								</a>
							@endif
						@endif
					@endfor
				</div>
				<div class="flex flex-col gap-1">
					@for(PoeOptimizeResult.ItemPick item : result.items())
						!{
							// 로케일별 모드 라인 — 결과 페이지가 유일한 ko 전용 표면이었던 갭 해소(EN 은 modLinesEn, 없으면 ko 폴백)
							java.util.List<String> shownModLines =
								net.luversof.web.gate.poe.PoeText.isKorean() || item.modLinesEn() == null || item.modLinesEn().isEmpty()
									? item.modLines() : item.modLinesEn();
						}
						<div class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 text-sm">
							<span class="badge badge-ghost badge-sm shrink-0 w-16 justify-center">${net.luversof.web.gate.poe.PoeText.name(item.slotKo(), item.slot())}</span>
							@if("RARE".equals(item.rarity()))
								<%-- 레어의 slug = 베이스 아이템 상세(모드 풀·에센스·작업대) — 결과에서 제작 정보로 바로 이동.
								     호버 = 인게임식 툴팁(유니크 호버 레이어 파리티) — 데이터가 이미 여기 있어 서버 호출 없이 CSS 로만 띄운다 --%>
								<span class="relative group">
									@if(item.slug() != null)
										<a class="font-semibold poe-name-rare link-hover" href="/poe/items/${item.slug()}">${MessageUtil.getMessage("poe.sim.opt.result.rare")} · ${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</a>
									@else
										<span class="font-semibold poe-name-rare">${MessageUtil.getMessage("poe.sim.opt.result.rare")} · ${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</span>
									@endif
									<%-- 거래소 검색(사용자 요청) — 최상위(전체 모드, 해당 티어 최저 롤)와 실속형(필수 모드만, T2 이상) 두 링크 --%>
									@if(item.tradeQuery() != null && !item.tradeQuery().isBlank())
											<a class="badge badge-xs badge-outline badge-info align-middle ml-1" target="_blank" rel="noopener"
										   href="https://poe.game.daum.net/trade/search?q=${java.net.URLEncoder.encode(item.tradeQuery(), java.nio.charset.StandardCharsets.UTF_8)}"
										   title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "전체 옵션 · 표시 티어 기준으로 거래소 검색" : "Search trade — all mods at shown tier"}">${net.luversof.web.gate.poe.PoeText.isKorean() ? "거래소 최상위" : "Trade top"}</a>
									@endif
									@if(item.tradeQueryBudget() != null && !item.tradeQueryBudget().isBlank())
										<a class="badge badge-xs badge-outline badge-success align-middle ml-1" target="_blank" rel="noopener"
										   href="https://poe.game.daum.net/trade/search?q=${java.net.URLEncoder.encode(item.tradeQueryBudget(), java.nio.charset.StandardCharsets.UTF_8)}"
										   title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "중요 옵션만 · 2티어 이상으로 거래소 검색(실속형)" : "Search trade — essential mods at tier 2+"}">${net.luversof.web.gate.poe.PoeText.isKorean() ? "거래소 실속" : "Trade budget"}</a>
									@endif
									<div class="poe-tooltip poe-rar-rare w-80 shadow-2xl hidden group-hover:block absolute left-0 top-full z-30" data-rare-tooltip>
										<div class="poe-tt-band poe-tt-band-2">
											<div class="text-center font-bold text-[15px] leading-tight text-[#ffff77] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</div>
											<div class="text-center text-[13px] leading-tight text-[#ffff77] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${MessageUtil.getMessage("poe.sim.opt.result.rare")}</div>
										</div>
										!{
											// (엘드리치) 임플리싯은 구분선 위(인게임 배치), 나머지는 아래 — 마커별 인게임 배색. EN 마커도 인식
											java.util.List<String> rtImplicits = new java.util.ArrayList<>();
											java.util.List<String> rtExplicits = new java.util.ArrayList<>();
											java.util.List<String> rtExplicitTiers = new java.util.ArrayList<>();
											java.util.List<String> itemTiers = item.modTiers();
											for (int mi = 0; mi < shownModLines.size(); mi++) {
												String ml = shownModLines.get(mi);
												String mlTier = itemTiers != null && mi < itemTiers.size() ? itemTiers.get(mi) : null;
												if (ml.startsWith("(엘드리치)") || ml.startsWith("(eldritch)") || ml.startsWith("(총주교)") || ml.startsWith("(포식자)") || ml.startsWith("(exarch)") || ml.startsWith("(eater)")) rtImplicits.add(ml.substring(ml.indexOf(") ") + 2));
												else { rtExplicits.add(ml); rtExplicitTiers.add(mlTier); }
											}
										}
										@if(!rtImplicits.isEmpty())
											<div class="poe-tt-sepimg"></div>
											<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
												@for(String ml : rtImplicits)
													<div class="text-[#d0a2f7]">${ml}</div>
												@endfor
											</div>
										@endif
										<div class="poe-tt-sepimg"></div>
										<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
											@for(int rti = 0; rti < rtExplicits.size(); rti++)
												!{
													String ml = rtExplicits.get(rti);
													int rtEnd = ml.startsWith("(") ? ml.indexOf(") ") : -1;
													String rtMk = rtEnd > 0 ? ml.substring(1, rtEnd) : null;
													String rtRest = rtMk != null ? ml.substring(rtEnd + 2) : ml;
													String rtTier = rti < rtExplicitTiers.size() ? rtExplicitTiers.get(rti) : null;
												}
												<div class="text-[#8888ff]">@if(rtMk != null)<span class="text-[10px] text-white/35">(${rtMk})</span> @endif@if(rtTier != null && !rtTier.isEmpty())<span class="text-[10px] font-mono text-white/40 mr-0.5">${rtTier}</span> @endif${rtRest}</div>
											@endfor
										</div>
									</div>
								</span>
								@if(!shownModLines.isEmpty())
									@template.poe.htmx.resultModLines(modLines = shownModLines, modTiers = item.modTiers())
								@endif
							@else
								<%-- 유니크: 목록과 같은 호버 레이어(인게임 툴팁) + 상세 페이지 링크 — 결과에서 바로 아이템을 확인할 수 있게 --%>
								@if(item.slug() != null)
									<a class="poe-hover font-semibold poe-name-unique link-hover cursor-pointer"
									   href="/poe/uniques/${item.slug()}"
									   hx-get="/poe/htmx/uniques/detail?slug=${item.slug()}"
									   hx-target="#poePreview"
									   hx-trigger="mouseenter delay:100ms"
									   hx-swap="innerHTML">${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</a>
								@else
									<span class="font-semibold poe-name-unique">${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</span>
								@endif
									<%-- 유니크 거래소 링크(사용자 요청) — 고유명 검색 + 즉시 구입. 티어 개념이 없어 배지 하나 --%>
									@if(item.tradeQuery() != null && !item.tradeQuery().isBlank())
										<a class="badge badge-xs badge-outline badge-info align-middle ml-1" target="_blank" rel="noopener"
										   href="https://poe.game.daum.net/trade/search?q=${java.net.URLEncoder.encode(item.tradeQuery(), java.nio.charset.StandardCharsets.UTF_8)}"
										   title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "이 고유 아이템을 거래소에서 검색" : "Search trade for this unique"}">${net.luversof.web.gate.poe.PoeText.isKorean() ? "거래소" : "Trade"}</a>
									@endif
								@if(!shownModLines.isEmpty())
									@template.poe.htmx.resultModLines(modLines = shownModLines, modTiers = item.modTiers())
								@endif
							@endif
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(!result.tierComparisons().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.tiers")}</span>
				<div class="overflow-x-auto rounded-box bg-base-100">
					<table class="table table-xs">
						<thead>
							<tr>
								<th>${MessageUtil.getMessage("poe.sim.opt.result.items")}</th>
								@for(PoeOptimizeResult.TierRow row : result.tierComparisons().get(0).rows())
									<th class="text-right">${tierLabel.apply(row.label())}</th>
								@endfor
							</tr>
						</thead>
						<tbody>
							@for(PoeOptimizeResult.SlotTierCompare compare : result.tierComparisons())
								<tr>
									<td class="font-semibold">${net.luversof.web.gate.poe.PoeText.name(compare.slotKo(), compare.slot())}</td>
									@for(PoeOptimizeResult.TierRow row : compare.rows())
										<td class="text-right font-mono">${row.value()}</td>
									@endfor
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			</div>
		@endif

		@if(result.scenarioMatrix() != null && !result.scenarioMatrix().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.matrix")}</span>
				<div class="overflow-x-auto rounded-box bg-base-100">
					<table class="table table-xs">
						<thead>
							<tr>
								<th>${MessageUtil.getMessage("poe.sim.opt.scenario")}</th>
								<th class="text-right">${MessageUtil.getMessage("poe.sim.opt.result.matrix.buffoff")}</th>
								<th class="text-right">${MessageUtil.getMessage("poe.sim.opt.result.matrix.buffon")}</th>
							</tr>
						</thead>
						<tbody>
							@for(PoeOptimizeResult.ScenarioCell cell : result.scenarioMatrix())
								<tr>
									<td class="font-semibold">${net.luversof.web.gate.poe.PoeText.name(cell.scenarioKo(), cell.scenario())}</td>
									<td class="text-right font-mono text-base-content/60">${cell.dpsBuffOff()}</td>
									<td class="text-right font-mono text-primary">${cell.dpsBuffOn()}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			</div>
		@endif

		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<div class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.code")}</div>
				<%-- 클립보드 복사 — PoB 에 붙여넣는 게 이 코드의 유일한 용도라 원클릭이 맞다 --%>
				<button type="button" class="btn btn-ghost btn-xs" id="poePobCopy"
				        onclick="navigator.clipboard.writeText(this.closest('div.space-y-1').querySelector('textarea').value).then(() => { this.textContent = '${net.luversof.web.gate.poe.PoeText.isKorean() ? "복사됨 ✓" : "Copied ✓"}'; setTimeout(() => { this.textContent = '${net.luversof.web.gate.poe.PoeText.isKorean() ? "복사" : "Copy"}'; }, 1500); })">${net.luversof.web.gate.poe.PoeText.isKorean() ? "복사" : "Copy"}</button>
			</div>
			<textarea readonly rows="3" class="textarea textarea-bordered w-full font-mono text-xs leading-5">${result.pobCode()}</textarea>
		</div>
	</div>
@endif
