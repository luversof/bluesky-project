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
		<%-- 전 컬럼 표시 — 단순 텍스트 열거 대신 섹션별 라벨-값 그리드(각 셀: 라벨 좌 / 값 우측 정렬). --%>
		!{
			// 셀 하나(라벨/값) — JTE 안에서 반복 마크업을 줄이기 위한 조립 헬퍼
			java.util.function.BiFunction<String, String, String> cell = (label, value) ->
				"<div class=\"flex items-baseline justify-between gap-2 rounded bg-base-200/50 px-2 py-0.5\">"
				+ "<span class=\"text-base-content/50\">" + label + "</span>"
				+ "<span class=\"font-mono text-base-content/85\">" + value + "</span></div>";
			String NBSP = " ";
		}
		<div class="grid grid-cols-3 gap-1 sm:grid-cols-4 lg:grid-cols-5">
			$unsafe{cell.apply("EHP", fmt.apply(ninjaBenchmark.ehp()))}
			$unsafe{cell.apply("DPS", fmt.apply(ninjaBenchmark.dps()))}
			$unsafe{cell.apply(ko ? "생명" : "Life", fmt.apply(ninjaBenchmark.life()))}
			$unsafe{cell.apply("ES", fmt.apply(ninjaBenchmark.energyShield()))}
			$unsafe{cell.apply(ko ? "재생" : "Regen", fmt.apply(ninjaBenchmark.lifeRegen()))}
		</div>
		<%-- 조합 벤치의 스킬별 전용 DPS — 위 DPS 는 메인(첫 선택) 스킬 전용(시뮬 지표와 동일 의미) --%>
		@if(ninjaBenchmark.skillDps() != null && ninjaBenchmark.skillDps().size() > 1)
			<div class="grid grid-cols-2 gap-1 sm:grid-cols-3">
				@for(var sd : ninjaBenchmark.skillDps())
					<%-- 스킬명은 외부(poe.ninja) 문자열 — $unsafe 셀 조립에 넣기 전에 이스케이프 --%>
					$unsafe{cell.apply((ko ? "스킬 DPS · " : "DPS · ") + sd.name().replace("&", "&amp;").replace("<", "&lt;"), fmt.apply(sd.dps()))}
				@endfor
			</div>
		@endif
		<div class="pt-1 text-[10px] uppercase tracking-wide text-base-content/40">${ko ? "방어층" : "Defences"}</div>
		<div class="grid grid-cols-3 gap-1 sm:grid-cols-4 lg:grid-cols-5">
			$unsafe{cell.apply(ko ? "방어도" : "Armour", fmt.apply(ninjaBenchmark.armour()))}
			$unsafe{cell.apply(ko ? "회피" : "Evasion", fmt.apply(ninjaBenchmark.evasion()))}
			$unsafe{cell.apply(ko ? "막기" : "Block", ninjaBenchmark.block() + "%")}
			$unsafe{cell.apply(ko ? "주문막기" : "Sp.block", ninjaBenchmark.spellBlock() + "%")}
			$unsafe{cell.apply(ko ? "회피억제" : "Suppress", ninjaBenchmark.suppress() + "%")}
			@if(ninjaBenchmark.spellDodge() > 0)$unsafe{cell.apply(ko ? "주문회피" : "Sp.dodge", ninjaBenchmark.spellDodge() + "%")}@endif
			@if(ninjaBenchmark.physTakenAs() > 0)$unsafe{cell.apply(ko ? "물리전환" : "Phys taken", ninjaBenchmark.physTakenAs() + "%")}@endif
			$unsafe{cell.apply(ko ? "저항" : "Res", (ko ? "화" : "F") + ninjaBenchmark.fireRes() + NBSP + (ko ? "냉" : "C") + ninjaBenchmark.coldRes() + NBSP + (ko ? "번" : "L") + ninjaBenchmark.lightningRes() + NBSP + (ko ? "카" : "Ch") + ninjaBenchmark.chaosRes())}
			$unsafe{cell.apply(ko ? "최대피격(최약)" : "Max hit (min)", ninjaBenchmark.lowestMax() > 0 ? fmt.apply(ninjaBenchmark.lowestMax()) : "—")}
		</div>
		<div class="pt-1 text-[10px] uppercase tracking-wide text-base-content/40">${ko ? "자원·속성" : "Resources & attributes"}</div>
		<div class="grid grid-cols-3 gap-1 sm:grid-cols-4 lg:grid-cols-5">
			$unsafe{cell.apply(ko ? "수호물" : "Ward", fmt.apply(ninjaBenchmark.ward()))}
			$unsafe{cell.apply(ko ? "마나" : "Mana", fmt.apply(ninjaBenchmark.mana()))}
			$unsafe{cell.apply(ko ? "이동속도" : "Move spd", ninjaBenchmark.movementSpeed() + "%")}
			$unsafe{cell.apply(ko ? "희귀도" : "Rarity", ninjaBenchmark.itemRarity() + "%")}
			$unsafe{cell.apply(ko ? "힘·민·지" : "Str·Dex·Int", ninjaBenchmark.str() + NBSP + "·" + NBSP + ninjaBenchmark.dex() + NBSP + "·" + NBSP + ninjaBenchmark.intel())}
		</div>
		<div class="pt-1 text-[10px] uppercase tracking-wide text-base-content/40">${ko ? "구성" : "Setup"}</div>
		<div class="grid grid-cols-3 gap-1 sm:grid-cols-4 lg:grid-cols-5">
			$unsafe{cell.apply(ko ? "충전(인·격·권)" : "Charges E·F·P", ninjaBenchmark.enduranceCharges() + NBSP + "·" + NBSP + ninjaBenchmark.frenzyCharges() + NBSP + "·" + NBSP + ninjaBenchmark.powerCharges())}
			$unsafe{cell.apply(ko ? "클러스터(대·중·소)" : "Cluster L·M·S", ninjaBenchmark.largeCluster() + NBSP + "·" + NBSP + ninjaBenchmark.mediumCluster() + NBSP + "·" + NBSP + ninjaBenchmark.smallCluster())}
			$unsafe{cell.apply(ko ? "유니크" : "Uniques", String.valueOf(ninjaBenchmark.uniqueEquip()))}
			@if(ninjaBenchmark.mirroredItems() + ninjaBenchmark.mirroredWeapons() + ninjaBenchmark.mirroredArmours() > 0)$unsafe{cell.apply(ko ? "미러" : "Mirrored", String.valueOf(ninjaBenchmark.mirroredItems() + ninjaBenchmark.mirroredWeapons() + ninjaBenchmark.mirroredArmours()))}@endif
			$unsafe{cell.apply(ko ? "최대피격 물·화" : "Max hit Ph·F", fmt.apply(ninjaBenchmark.physicalMax()) + NBSP + "·" + NBSP + fmt.apply(ninjaBenchmark.fireMax()))}
			$unsafe{cell.apply(ko ? "최대피격 냉·번·카" : "Max hit C·L·Ch", fmt.apply(ninjaBenchmark.coldMax()) + NBSP + "·" + NBSP + fmt.apply(ninjaBenchmark.lightningMax()) + NBSP + "·" + NBSP + fmt.apply(ninjaBenchmark.chaosMax()))}
		</div>
		@if(ninjaBenchmark.topMasteries() != null && !ninjaBenchmark.topMasteries().isEmpty() && (ninjaBenchmark.facets() == null || !ninjaBenchmark.facets().containsKey("masteries")))
			<div class="text-base-content/50">${ko ? "실빌드 마스터리" : "Common masteries"}: ${String.join(" · ", ninjaBenchmark.topMasteries())}</div>
		@endif
		<%-- 패싯(사이드바 집계) — 마스터리/룬크래프트/문신/무기/판테온/아틀라스 등 전체 모집단 사용률(실행 전 미리보기는 3줄) --%>
		@template.poe.htmx.benchFacets(bench = ninjaBenchmark, rows = 3)
		<div class="text-[10px] text-base-content/40">${ko ? "poe.ninja 96레벨+ 실빌드 전 컬럼 — 이상치 캐릭터 제외 후 중앙값. 이 성향에 맞춰 시뮬이 강점 축을 밉니다" : "poe.ninja lv96+ all columns — median after dropping outlier characters; sim pushes this build's strong axis"}</div>
	</div>
@endif
