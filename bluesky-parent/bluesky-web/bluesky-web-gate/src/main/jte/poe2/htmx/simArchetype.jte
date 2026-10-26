@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param Poe2.NinjaArchetype archetype = null

<%-- PoE2 시뮬 미리보기 — 고른 (전직×스킬)의 poe.ninja 실빌드 성향·중앙값(실행 전). PoE1 시뮬의 실빌드 성향 미리보기(archetypeHint)와 같은 역할.
     성향이 목표를 정한다(공격 특화 → DPS, 생존 특화 → EHP, 균형 → 합). --%>
@if(archetype != null && archetype.sample() != null && archetype.sample() > 0)
	!{
		boolean ko = PoeText.isKorean();
		java.util.function.Function<Double, String> fmt = v -> {
			if (v == null) return "-";
			double n = v;
			if (n >= 1_000_000_000d) return String.format("%.1fB", n / 1_000_000_000d);
			if (n >= 1_000_000d) return String.format("%.1fM", n / 1_000_000d);
			if (n >= 10_000d) return String.format("%.0fk", n / 1_000d);
			return String.format("%,.0f", n);
		};
		String lean = archetype.lean();
		String leanLabel = "dps".equals(lean) ? MessageUtil.getMessage("poe.sim.lean.dps")
			: "ehp".equals(lean) ? MessageUtil.getMessage("poe.sim.lean.ehp")
			: MessageUtil.getMessage("poe.sim.lean.balanced");
		String leanCls = "dps".equals(lean) ? "badge-error" : "ehp".equals(lean) ? "badge-info" : "badge-ghost";
		boolean auto = archetype.ascendancy() == null || archetype.ascendancy().isBlank();
		java.util.List<String> items = archetype.topItems() == null ? java.util.List.of()
			: archetype.topItems().stream().filter(c -> c.name() != null && !c.name().startsWith("Rare ") && !c.name().startsWith("Magic ") && !c.name().startsWith("Normal ")).limit(6).map(c -> PoeText.name(c.nameKo(), c.name())).toList();
		// 한글 이름(10-25 Q172 — API 미리보기 응답이 채운다, 없으면 영어)
		java.util.function.Function<java.util.List<Poe2.NinjaCount>, java.util.List<String>> names = l -> l.stream().map(c -> PoeText.name(c.nameKo(), c.name())).toList();
	}
	<div class="space-y-1 rounded-box border border-base-200 bg-base-100 px-3 py-2 text-xs" data-sim-hint>
		<div class="flex flex-wrap items-center gap-2">
			<span class="text-base-content/60">${MessageUtil.getMessage("poe.sim.archetype")}</span>
			<span class="badge badge-sm ${leanCls}">${leanLabel}</span>
			<span class="badge badge-ghost badge-xs">${PoeText.name(archetype.mainSkillKo(), archetype.mainSkill())} · ${auto ? MessageUtil.getMessage("poe.sim.lean.allasc") : PoeText.name(archetype.ascendancyKo(), archetype.ascendancy())}</span>
			<span class="text-base-content/60">n=${archetype.sample()}</span>
		</div>
		<div class="grid grid-cols-2 gap-1 sm:grid-cols-4">
			<div class="rounded bg-base-200/60 px-2 py-1"><div class="text-[10px] text-base-content/60">${ko ? "생명력" : "Life"}</div><div class="font-mono">${fmt.apply(archetype.medianLife())}</div></div>
			<div class="rounded bg-base-200/60 px-2 py-1"><div class="text-[10px] text-base-content/60">${ko ? "에너지 보호막" : "ES"}</div><div class="font-mono">${fmt.apply(archetype.medianEs())}</div></div>
			<div class="rounded bg-base-200/60 px-2 py-1"><div class="text-[10px] text-base-content/60">EHP</div><div class="font-mono">${fmt.apply(archetype.medianEhp())}</div></div>
			<div class="rounded bg-base-200/60 px-2 py-1"><div class="text-[10px] text-base-content/60">DPS</div><div class="font-mono">${fmt.apply(archetype.medianDps())}</div></div>
		</div>
		@if(archetype.topKeystones() != null && !archetype.topKeystones().isEmpty())
			<div class="text-base-content/60">${MessageUtil.getMessage("poe.sim.bench.keystones")}: ${String.join(", ", names.apply(archetype.topKeystones().stream().limit(4).toList()))}</div>
		@endif
		@if(archetype.topCoSkills() != null && !archetype.topCoSkills().isEmpty())
			<div class="text-base-content/60">${MessageUtil.getMessage("poe.sim.bench.coskills")}: ${String.join(", ", names.apply(archetype.topCoSkills().stream().limit(5).toList()))}</div>
		@endif
		@if(!items.isEmpty())
			<div class="text-base-content/60">${ko ? "자주 쓰는 고유" : "Common uniques"}: ${String.join(", ", items)}</div>
		@endif
		<%-- 실빌드 출발점 바로가기(10-14 C325 — PoE1 archetypeHint 의 짝): 시뮬을 돌리지 않고 이 아키타입의 대표 실빌드를 빌드 화면에서 열어
		     업그레이드 가이드 · 자동 다듬기로 다듬는다(/poe2/build?ascendancy=&skill= 은 이미 있던 경로) --%>
		@if(!auto && archetype.start() != null && archetype.start().dps() != null)
			<div class="flex flex-wrap items-center gap-2 pt-1" data-real-start-hint>
				<a class="btn btn-outline btn-xs" href="/poe2/build?ascendancy=${java.net.URLEncoder.encode(archetype.ascendancy(), java.nio.charset.StandardCharsets.UTF_8)}&skill=${java.net.URLEncoder.encode(archetype.mainSkill(), java.nio.charset.StandardCharsets.UTF_8)}">${ko ? "실빌드에서 출발하기" : "Start from a real build"}</a>
				<span class="text-[11px] text-base-content/60">${ko ? "대표 실빌드 DPS " + fmt.apply(archetype.start().dps()) + " · EHP " + fmt.apply(archetype.start().ehp()) + " — 빌드 화면에서 업그레이드 가이드 · 자동 다듬기" : "Representative build DPS " + fmt.apply(archetype.start().dps()) + " · EHP " + fmt.apply(archetype.start().ehp()) + " — upgrade guide and auto-refine in the build view"}</span>
			</div>
		@endif
		<div class="text-[10px] text-base-content/60">${ko ? "poe.ninja 스냅샷 · 표기 중앙값 — 실행하면 이 조건 실빌드들의 공통 특성(패시브 · 고유 · 보조 젬)을 참고해 우리 엔진으로 계산하고, 성향에 맞춰 다듬습니다" : "poe.ninja snapshot · medians — running uses these builds' common traits (passives, uniques, supports), calculates with our engine and refines toward this lean"}</div>
	</div>
@endif
