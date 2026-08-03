@import net.luversof.web.gate.poe.dto.ArchetypeBenchmark
@import net.luversof.web.gate.poe.PoeText

<%-- 시뮬 폼 실시간 성향 미리보기 — 선택한 스킬×전직의 poe.ninja 실빌드 성향/프로파일을 실행 전에 보여준다.
     매칭 없으면 빈 출력(폼 레이아웃 영향 없음). --%>
@param ArchetypeBenchmark ninjaBenchmark = null

@if(ninjaBenchmark != null && ninjaBenchmark.sample() > 0)
	!{
		boolean ko = PoeText.isKorean();
		java.util.function.LongFunction<String> fmt = n -> {
			if (n >= 1_000_000_000L) return String.format("%.1fB", n / 1_000_000_000.0);
			if (n >= 1_000_000L) return String.format("%.1fM", n / 1_000_000.0);
			if (n >= 1_000L) return String.format("%.0fk", n / 1_000.0);
			return String.valueOf(n);
		};
		String lean = ninjaBenchmark.lean();
		String leanLabel = "dps".equals(lean) ? (ko ? "공격 특화 (고DPS·저EHP)" : "Offense (high DPS)")
			: "ehp".equals(lean) ? (ko ? "생존 특화 (고EHP)" : "Defense (high EHP)")
			: (ko ? "균형형" : "Balanced");
		String leanCls = "dps".equals(lean) ? "badge-error" : "ehp".equals(lean) ? "badge-info" : "badge-ghost";
		String ascLabel = ninjaBenchmark.ascendancy() != null && !ninjaBenchmark.ascendancy().isBlank() ? ninjaBenchmark.ascendancy() : (ko ? "전 전직 통합" : "all ascendancies");
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
	}
	<div class="mt-2 rounded-box bg-base-100 px-3 py-2 text-xs space-y-1 border border-base-200">
		<div class="flex flex-wrap items-center gap-2">
			<span class="text-base-content/50">${ko ? "실빌드 성향" : "Real-build lean"}</span>
			<span class="badge badge-sm ${leanCls}">${leanLabel}</span>
			<span class="badge badge-ghost badge-xs">${ninjaBenchmark.mainSkill()} · ${ascLabel}</span>
			<span class="text-base-content/40">n=${ninjaBenchmark.sample()}</span>
		</div>
		@if(ninjaBenchmark.specializations() != null && !ninjaBenchmark.specializations().isEmpty())
			<div class="flex flex-wrap items-center gap-1">
				<span class="text-base-content/50">${ko ? "특화 컬럼" : "Specializes in"}</span>
				@for(String sk : ninjaBenchmark.specializations())
					<span class="badge badge-xs badge-warning badge-outline">${specLabel.apply(sk)}</span>
				@endfor
			</div>
		@endif
		<%-- 전 컬럼 표시 — poe.ninja 참조하는 모든 컬럼을 그대로 노출(사용자 요구: 모든 컬럼 참조가 눈에 보이게). --%>
		<div class="flex flex-wrap gap-x-3 gap-y-0.5 font-mono text-base-content/60">
			<span>EHP <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.ehp())}</span></span>
			<span>DPS <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.dps())}</span></span>
			<span>${ko ? "생명" : "Life"} <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.life())}</span></span>
			<span>ES <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.energyShield())}</span></span>
			<span>${ko ? "재생" : "Regen"} <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.lifeRegen())}</span></span>
		</div>
		<%-- 조합 벤치의 스킬별 전용 DPS — 위 DPS 는 메인(첫 선택) 스킬 전용(시뮬 지표와 동일 의미) --%>
		@if(ninjaBenchmark.skillDps() != null && ninjaBenchmark.skillDps().size() > 1)
			<div class="flex flex-wrap gap-x-3 gap-y-0.5 font-mono text-base-content/60">
				<span class="font-sans text-base-content/50">${ko ? "스킬별 DPS" : "Per-skill DPS"}</span>
				@for(var sd : ninjaBenchmark.skillDps())
					<span>${sd.name()} <span class="text-base-content/80">${fmt.apply(sd.dps())}</span></span>
				@endfor
			</div>
		@endif
		<div class="flex flex-wrap gap-x-3 gap-y-0.5 font-mono text-base-content/60">
			<span>${ko ? "수호물" : "Ward"} <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.ward())}</span></span>
			<span>${ko ? "마나" : "Mana"} <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.mana())}</span></span>
			<span>${ko ? "이동속도" : "MS"} <span class="text-base-content/80">${ninjaBenchmark.movementSpeed()}%</span></span>
			<span>${ko ? "희귀도" : "Rarity"} <span class="text-base-content/80">${ninjaBenchmark.itemRarity()}%</span></span>
		</div>
		<div class="flex flex-wrap gap-x-3 gap-y-0.5 font-mono text-base-content/60">
			<span>${ko ? "힘" : "Str"} <span class="text-base-content/80">${ninjaBenchmark.str()}</span></span>
			<span>${ko ? "민첩" : "Dex"} <span class="text-base-content/80">${ninjaBenchmark.dex()}</span></span>
			<span>${ko ? "지능" : "Int"} <span class="text-base-content/80">${ninjaBenchmark.intel()}</span></span>
		</div>
		<div class="flex flex-wrap gap-x-3 gap-y-0.5 font-mono text-base-content/60">
			<span>${ko ? "방어도" : "Armour"} <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.armour())}</span></span>
			<span>${ko ? "회피" : "Evasion"} <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.evasion())}</span></span>
			<span>${ko ? "막기" : "Block"} <span class="text-base-content/80">${ninjaBenchmark.block()}%</span></span>
			<span>${ko ? "주문막기" : "SpBlk"} <span class="text-base-content/80">${ninjaBenchmark.spellBlock()}%</span></span>
			<span>${ko ? "회피억제" : "Suppress"} <span class="text-base-content/80">${ninjaBenchmark.suppress()}%</span></span>
			@if(ninjaBenchmark.spellDodge() > 0)<span>${ko ? "주문회피" : "SpDodge"} <span class="text-base-content/80">${ninjaBenchmark.spellDodge()}%</span></span>@endif
			@if(ninjaBenchmark.physTakenAs() > 0)<span>${ko ? "물리전환" : "PhysTaken"} <span class="text-base-content/80">${ninjaBenchmark.physTakenAs()}%</span></span>@endif
		</div>
		<div class="flex flex-wrap gap-x-3 gap-y-0.5 font-mono text-base-content/60">
			<span>${ko ? "저항" : "Res"} <span class="text-base-content/80">${ko ? "화" : "F"}${ninjaBenchmark.fireRes()} ${ko ? "냉" : "C"}${ninjaBenchmark.coldRes()} ${ko ? "번" : "L"}${ninjaBenchmark.lightningRes()} ${ko ? "카" : "Ch"}${ninjaBenchmark.chaosRes()}</span></span>
		</div>
		<div class="flex flex-wrap gap-x-3 gap-y-0.5 font-mono text-base-content/60">
			<span>${ko ? "충전" : "Charges"} <span class="text-base-content/80">${ko ? "인" : "E"}${ninjaBenchmark.enduranceCharges()} ${ko ? "격" : "F"}${ninjaBenchmark.frenzyCharges()} ${ko ? "권" : "P"}${ninjaBenchmark.powerCharges()}</span></span>
			<span>${ko ? "클러스터" : "Cluster"} <span class="text-base-content/80">${ko ? "대" : "L"}${ninjaBenchmark.largeCluster()} ${ko ? "중" : "M"}${ninjaBenchmark.mediumCluster()} ${ko ? "소" : "S"}${ninjaBenchmark.smallCluster()}</span></span>
			<span>${ko ? "유니크" : "Unique"} <span class="text-base-content/80">${ninjaBenchmark.uniqueEquip()}</span></span>
			@if(ninjaBenchmark.mirroredItems() + ninjaBenchmark.mirroredWeapons() + ninjaBenchmark.mirroredArmours() > 0)<span>${ko ? "미러" : "Mirror"} <span class="text-base-content/80">${ninjaBenchmark.mirroredItems() + ninjaBenchmark.mirroredWeapons() + ninjaBenchmark.mirroredArmours()}</span></span>@endif
		</div>
		<div class="flex flex-wrap gap-x-3 gap-y-0.5 font-mono text-base-content/60">
			<span>${ko ? "최대피격" : "Max hit"} <span class="text-base-content/80">${ko ? "물" : "Ph"}${fmt.apply(ninjaBenchmark.physicalMax())} ${ko ? "화" : "F"}${fmt.apply(ninjaBenchmark.fireMax())} ${ko ? "냉" : "C"}${fmt.apply(ninjaBenchmark.coldMax())} ${ko ? "번" : "L"}${fmt.apply(ninjaBenchmark.lightningMax())} ${ko ? "카" : "Ch"}${fmt.apply(ninjaBenchmark.chaosMax())}</span></span>
			@if(ninjaBenchmark.lowestMax() > 0)<span class="text-base-content/45">(${ko ? "최약" : "min"} ${fmt.apply(ninjaBenchmark.lowestMax())})</span>@endif
		</div>
		@if(ninjaBenchmark.topMasteries() != null && !ninjaBenchmark.topMasteries().isEmpty() && (ninjaBenchmark.facets() == null || !ninjaBenchmark.facets().containsKey("masteries")))
			<div class="text-base-content/50">${ko ? "실빌드 마스터리" : "Common masteries"}: ${String.join(" · ", ninjaBenchmark.topMasteries())}</div>
		@endif
		<%-- 패싯(사이드바 집계) — 마스터리/룬크래프트/문신/무기/판테온/아틀라스 등 전체 모집단 사용률(실행 전 미리보기는 3줄) --%>
		@template.poe.htmx.benchFacets(bench = ninjaBenchmark, rows = 3)
		<div class="text-[10px] text-base-content/40">${ko ? "poe.ninja 96레벨+ 실빌드 전 컬럼 — 이상치 캐릭터 제외 후 중앙값. 이 성향에 맞춰 시뮬이 강점 축을 밉니다" : "poe.ninja lv96+ all columns — median after dropping outlier characters; sim pushes this build's strong axis"}</div>
	</div>
@endif
