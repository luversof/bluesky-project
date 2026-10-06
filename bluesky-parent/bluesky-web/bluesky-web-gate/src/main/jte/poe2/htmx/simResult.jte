@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.SimResult result = null
@param Long historyId = null

<%-- PoE2 시뮬레이션 결과 한 건 — 진행 상태 조각(simStatus, 막 끝난 결과)과 결과 이력 뷰어(지난 결과)가 같이 쓴다(PoE1 simOptimizeResult 와 같은 역할).
     historyId 가 있으면 "빌드 화면에서 열기"가 그 이력 결과로 연다(없으면 마지막 결과). --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> fmt = v -> {
		if (v == null) return "-";
		double n = v;
		if (n >= 1_000_000_000d) return String.format("%.2fB", n / 1_000_000_000d);
		if (n >= 1_000_000d) return String.format("%.2fM", n / 1_000_000d);
		if (n >= 10_000d) return String.format("%.1fk", n / 1_000d);
		return String.format("%,.0f", n);
	};
	java.util.function.BiFunction<Double, Double, String> chg = (a, b) -> {
		if (a == null || b == null || a <= 0) return "-";
		double p = (b / a - 1) * 100;
		return Math.abs(p) < 0.05 ? "0.0%" : String.format("%+.1f%%", p);
	};
	java.util.function.Function<String, String> objLabel = o -> "dps".equals(o) ? (ko ? "공격(DPS)" : "Offense (DPS)") : "ehp".equals(o) ? (ko ? "생존(EHP)" : "Defense (EHP)") : (ko ? "균형(DPS·EHP 합)" : "Balanced (DPS+EHP)");
	java.util.function.Function<String, String> scenLabel = s -> "None".equals(s) ? (ko ? "보스 아님" : "Not a boss") : "Boss".equals(s) ? (ko ? "일반 보스" : "Standard boss") : "Uber".equals(s) ? (ko ? "우버 보스" : "Uber boss") : "keep".equals(s) ? (ko ? "플레이어 설정 그대로" : "Player setting") : (ko ? "최종 보스" : "Pinnacle boss");
}

@if(result == null)
	<div class="alert alert-warning text-sm">${ko ? "결과를 찾지 못했습니다(삭제됐거나 오래돼 정리됐습니다)." : "Result not found (deleted or pruned)."}</div>
@else
!{
	Poe2.SimResult r = result;
	Poe2.NinjaArchetype bm = r.benchmark();
}
<div class="space-y-3 rounded-2xl bg-base-100 p-4 shadow-sm" data-sim-result>
	<div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
		<span class="text-lg font-bold">${ko && r.skillKo() != null ? r.skillKo() : r.skill()}</span>
		<span class="badge badge-outline">${r.ascendancy()}</span>
		<span class="badge badge-ghost">${ko ? "목표" : "Objective"}: ${objLabel.apply(r.objective())}</span>
		<span class="badge badge-ghost">${scenLabel.apply(r.scenario())}</span>
		@if(r.fromTreeNodes() != null)
			<span class="badge badge-outline border-primary/60" data-from-tree>${ko ? "내 트리 · 노드 " + r.fromTreeNodes() + "개" : "My tree · " + r.fromTreeNodes() + " nodes"}</span>
		@endif
		@if(r.forcedUnique() != null)
			<span class="badge badge-outline border-orange-400/60 poe-name-unique" data-forced-unique>${ko ? "고정: " : "Locked: "}${ko && r.forcedUniqueKo() != null ? r.forcedUniqueKo() : r.forcedUnique()}</span>
		@endif
		<span class="text-xs text-base-content/60">${r.durationMs() == null ? "" : Math.round(r.durationMs() / 1000.0) + (ko ? "초" : "s")}</span>
	</div>
	<div class="overflow-x-auto">
		<table class="table table-sm w-auto" data-sim-compare>
			<thead>
				<tr>
					<th></th>
					<th class="text-right">${ko ? "출발 실빌드" : "Real-build start"}</th>
					<th class="text-right">${ko ? "다듬은 뒤" : "Refined"}</th>
					<th class="text-right">${ko ? "변화" : "Change"}</th>
					<th class="text-right">${ko ? "poe.ninja 중앙값" : "poe.ninja median"}@if(bm != null && bm.sample() != null)<div class="text-[10px] font-normal text-base-content/60">n=${bm.sample()}</div>@endif</th>
				</tr>
			</thead>
			<tbody>
				<tr><td>DPS</td><td class="text-right font-mono">${fmt.apply(r.start().dps())}</td><td class="text-right font-mono font-semibold">${fmt.apply(r.refined().dps())}</td><td class="text-right font-mono">${chg.apply(r.start().dps(), r.refined().dps())}</td><td class="text-right font-mono text-base-content/60">${fmt.apply(bm == null ? null : bm.medianDps())}</td></tr>
				<tr><td>EHP</td><td class="text-right font-mono">${fmt.apply(r.start().ehp())}</td><td class="text-right font-mono font-semibold">${fmt.apply(r.refined().ehp())}</td><td class="text-right font-mono">${chg.apply(r.start().ehp(), r.refined().ehp())}</td><td class="text-right font-mono text-base-content/60">${fmt.apply(bm == null ? null : bm.medianEhp())}</td></tr>
				<tr><td>${ko ? "생명" : "Life"}</td><td class="text-right font-mono">${fmt.apply(r.start().life())}</td><td class="text-right font-mono">${fmt.apply(r.refined().life())}</td><td class="text-right font-mono">${chg.apply(r.start().life(), r.refined().life())}</td><td class="text-right font-mono text-base-content/60">${fmt.apply(bm == null ? null : bm.medianLife())}</td></tr>
				<tr><td>ES</td><td class="text-right font-mono">${fmt.apply(r.start().es())}</td><td class="text-right font-mono">${fmt.apply(r.refined().es())}</td><td class="text-right font-mono">${chg.apply(r.start().es(), r.refined().es())}</td><td class="text-right font-mono text-base-content/60">${fmt.apply(bm == null ? null : bm.medianEs())}</td></tr>
			</tbody>
		</table>
	</div>
	@template.poe2.htmx.weaponSets(mainSet = r.mainSet(), sets = r.sets())
	<div class="space-y-1">
		<div class="text-sm font-semibold">${ko ? "다듬기 단계" : "Refine steps"}</div>
		@if(r.steps() == null || r.steps().isEmpty())
			<div class="text-sm text-base-content/60">${ko ? "목표를 더 올리는 고유 아이템·보조젬 교체를 찾지 못했습니다 — 출발 실빌드가 가이드가 보는 범위에서 이미 잘 맞춰져 있습니다." : "No unique/support swap improved the objective — the starting build is already well tuned within the guide's scope."}</div>
		@else
			<ol class="list-decimal space-y-0.5 pl-5 text-sm" data-sim-steps>
				@for(Poe2.RefineStep st : r.steps())
					<li>${st.label()} <span class="font-mono text-xs text-base-content/60">DPS ${chg.apply(100d, 100d * (1 + (st.dpsPct() == null ? 0 : st.dpsPct()) / 100))} · EHP ${chg.apply(100d, 100d * (1 + (st.ehpPct() == null ? 0 : st.ehpPct()) / 100))}</span></li>
				@endfor
			</ol>
		@endif
	</div>
	<details class="rounded-box bg-base-200/40 px-3 py-2 text-sm">
		<summary class="cursor-pointer font-semibold">${ko ? "실빌드 후보 " + r.candidates().size() + "명" : r.candidates().size() + " real-build candidates"}</summary>
		<div class="overflow-x-auto">
			<table class="table table-xs w-auto" data-sim-candidates>
				<thead><tr><th>${ko ? "레벨" : "Lv"}</th><th class="text-right">ninja DPS</th><th class="text-right">ninja EHP</th><th class="text-right">${ko ? "엔진 DPS" : "Engine DPS"}</th><th class="text-right">${ko ? "엔진 EHP" : "Engine EHP"}</th><th>${ko ? "무기 세트" : "Set"}</th><th></th></tr></thead>
				<tbody>
					@for(Poe2.SimCandidate c : r.candidates())
						<tr class="${Boolean.TRUE.equals(c.chosen()) ? "font-semibold" : ""}">
							<td>${c.level()}</td>
							<td class="text-right font-mono">${fmt.apply(c.ninjaDps())}</td>
							<td class="text-right font-mono">${fmt.apply(c.ninjaEhp())}</td>
							<td class="text-right font-mono">${fmt.apply(c.dps())}</td>
							<td class="text-right font-mono">${fmt.apply(c.ehp())}</td>
							<td class="text-center text-xs text-base-content/60">${c.set() == null ? "-" : String.valueOf(c.set())}</td>
							<td>@if(Boolean.TRUE.equals(c.chosen()))<span class="badge badge-primary badge-xs">${ko ? "선택" : "chosen"}</span>@elseif(c.error() != null)<span class="text-xs text-warning">${c.error()}</span>@endif</td>
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
		<div class="text-[11px] text-base-content/60">${ko ? "같은 시나리오로 우리 엔진에서 다시 계산해 목표에 가장 맞는 한 명을 골랐습니다(균형은 DPS·EHP 로그 합)." : "Recalculated under the same scenario; picked the best fit for the objective (balanced = log DPS + log EHP)."}</div>
	</details>
	<%-- 결과 PoB 코드 + 복사 — PoE1 시뮬 결과와 같은 자리(10-01: PoE2 엔 "빌드 화면에서 열기"만 있었다). PoB 에 붙여넣는 게 용도라 원클릭 복사 --%>
	@if(r.code() != null && !r.code().isBlank())
		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.sim.opt.result.code")}</div>
				<button type="button" class="btn btn-ghost btn-xs" data-copy-target="#poe2SimCode" data-copied-label="${ko ? "복사됨 ✓" : "Copied ✓"}">${ko ? "복사" : "Copy"}</button>
			</div>
			<textarea id="poe2SimCode" readonly rows="3" class="textarea textarea-bordered w-full font-mono text-xs leading-5" aria-label="${MessageUtil.getMessage("poe.sim.opt.result.code")}">${r.code()}</textarea>
		</div>
	@endif
	<div class="flex flex-wrap items-center gap-2">
		<a class="btn btn-primary btn-sm" href="/poe2/build?sim=${historyId == null ? "1" : String.valueOf(historyId)}" data-sim-open>${ko ? "빌드 화면에서 열기" : "Open in build view"}</a>
		<span class="text-xs text-base-content/60">${ko ? "고유 아이템은 품질 20% 기준 · 단계 증감은 적용 뒤 엔진 실측 · 계보 젬은 드물게 얻는 젬" : "Uniques at 20% quality · step changes are engine-measured · lineage gems are rare"}</span>
	</div>
</div>
@endif
