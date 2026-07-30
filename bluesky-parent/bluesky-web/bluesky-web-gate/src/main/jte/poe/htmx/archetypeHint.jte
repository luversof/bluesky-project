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
	}
	<div class="mt-2 rounded-box bg-base-100 px-3 py-2 text-xs space-y-1 border border-base-200">
		<div class="flex flex-wrap items-center gap-2">
			<span class="text-base-content/50">${ko ? "실빌드 성향" : "Real-build lean"}</span>
			<span class="badge badge-sm ${leanCls}">${leanLabel}</span>
			<span class="badge badge-ghost badge-xs">${ninjaBenchmark.mainSkill()} · ${ascLabel}</span>
			<span class="text-base-content/40">n=${ninjaBenchmark.sample()}</span>
		</div>
		<div class="flex flex-wrap gap-x-4 gap-y-0.5 font-mono text-base-content/60">
			<span>EHP <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.ehp())}</span></span>
			<span>DPS <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.dps())}</span></span>
			<span>${ko ? "생명" : "Life"} <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.life())}</span></span>
			@if(ninjaBenchmark.energyShield() > 0)<span>ES <span class="text-base-content/80">${fmt.apply(ninjaBenchmark.energyShield())}</span></span>@endif
		</div>
		<div class="text-[10px] text-base-content/40">${ko ? "poe.ninja 96레벨+ 실빌드 중앙값 — 이 성향에 맞춰 시뮬이 강점 축을 밉니다" : "poe.ninja lv96+ real-build median — sim pushes this build's strong axis"}</div>
	</div>
@endif
