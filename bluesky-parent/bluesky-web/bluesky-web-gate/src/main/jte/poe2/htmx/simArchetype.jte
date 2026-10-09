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
		String leanLabel = "dps".equals(lean) ? (ko ? "공격 특화 → 목표 DPS" : "Offense → DPS objective")
			: "ehp".equals(lean) ? (ko ? "생존 특화 → 목표 EHP" : "Defense → EHP objective")
			: (ko ? "균형 → 목표 DPS·EHP 합" : "Balanced → DPS+EHP objective");
		String leanCls = "dps".equals(lean) ? "badge-error" : "ehp".equals(lean) ? "badge-info" : "badge-ghost";
		boolean auto = archetype.ascendancy() == null || archetype.ascendancy().isBlank();
		java.util.List<String> items = archetype.topItems() == null ? java.util.List.of()
			: archetype.topItems().stream().map(Poe2.NinjaCount::name).filter(x -> x != null && !x.startsWith("Rare ") && !x.startsWith("Magic ") && !x.startsWith("Normal ")).limit(6).toList();
	}
	<div class="space-y-1 rounded-box border border-base-200 bg-base-100 px-3 py-2 text-xs" data-sim-hint>
		<div class="flex flex-wrap items-center gap-2">
			<span class="text-base-content/60">${ko ? "실빌드 성향" : "Real-build lean"}</span>
			<span class="badge badge-sm ${leanCls}">${leanLabel}</span>
			<span class="badge badge-ghost badge-xs">${archetype.mainSkill()} · ${auto ? (ko ? "전 전직" : "all ascendancies") : archetype.ascendancy()}</span>
			<span class="text-base-content/60">n=${archetype.sample()}</span>
		</div>
		<div class="grid grid-cols-2 gap-1 sm:grid-cols-4">
			<div class="rounded bg-base-200/60 px-2 py-1"><div class="text-[10px] text-base-content/60">${ko ? "생명" : "Life"}</div><div class="font-mono">${fmt.apply(archetype.medianLife())}</div></div>
			<div class="rounded bg-base-200/60 px-2 py-1"><div class="text-[10px] text-base-content/60">ES</div><div class="font-mono">${fmt.apply(archetype.medianEs())}</div></div>
			<div class="rounded bg-base-200/60 px-2 py-1"><div class="text-[10px] text-base-content/60">EHP</div><div class="font-mono">${fmt.apply(archetype.medianEhp())}</div></div>
			<div class="rounded bg-base-200/60 px-2 py-1"><div class="text-[10px] text-base-content/60">DPS</div><div class="font-mono">${fmt.apply(archetype.medianDps())}</div></div>
		</div>
		@if(archetype.topKeystones() != null && !archetype.topKeystones().isEmpty())
			<div class="text-base-content/60">${ko ? "실빌드 키스톤" : "Common keystones"}: ${String.join(", ", archetype.topKeystones().stream().limit(4).map(Poe2.NinjaCount::name).toList())}</div>
		@endif
		@if(archetype.topCoSkills() != null && !archetype.topCoSkills().isEmpty())
			<div class="text-base-content/60">${ko ? "함께 쓰는 스킬" : "Common co-skills"}: ${String.join(", ", archetype.topCoSkills().stream().limit(5).map(Poe2.NinjaCount::name).toList())}</div>
		@endif
		@if(!items.isEmpty())
			<div class="text-base-content/60">${ko ? "자주 쓰는 고유" : "Common uniques"}: ${String.join(", ", items)}</div>
		@endif
		<div class="text-[10px] text-base-content/60">${ko ? "poe.ninja 스냅샷 · 표기 중앙값 — 실행하면 이 조건의 실빌드 후보를 받아 우리 엔진으로 다시 계산하고, 성향에 맞춰 다듬습니다" : "poe.ninja snapshot · medians — running fetches real-build candidates, recalculates them with our engine and refines toward this lean"}</div>
	</div>
@endif
