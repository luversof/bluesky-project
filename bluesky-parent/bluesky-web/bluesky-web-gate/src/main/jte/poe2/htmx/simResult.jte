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
	java.util.function.Function<String, String> objLabel = o -> MessageUtil.getMessage("dps".equals(o) ? "poe.sim.opt.objective.dps" : "ehp".equals(o) ? "poe.sim.opt.objective.ehp" : "poe.sim.opt.objective.balanced");
	java.util.function.Function<String, String> scenLabel = s -> MessageUtil.getMessage("None".equals(s) ? "poe.sim.opt.scenario.none" : "Boss".equals(s) ? "poe.sim.opt.scenario.boss" : "Uber".equals(s) ? "poe.sim.opt.scenario.uber" : "keep".equals(s) ? "poe2.sim.opt.scenario.keep" : "poe.sim.opt.scenario.pinnacle");
}

@if(result == null)
	<div class="alert alert-warning text-sm">${MessageUtil.getMessage("poe.sim.result.notfound")}</div>
@else
!{
	Poe2.SimResult r = result;
	Poe2.NinjaArchetype bm = r.benchmark();
}
<div class="space-y-3 rounded-2xl bg-base-100 p-4 shadow-sm" data-sim-result>
	<div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
		<span class="text-lg font-bold">${ko && r.skillKo() != null ? r.skillKo() : r.skill()}</span>
		<%-- 전직(10-14 C311 사용자 요청 "어떤 전직인지 표시가 안 보인다") — 한국어 이름 + 자동으로 골랐으면 그 표시. 예전엔 영문 이름만 꾸밈없는 배지로 --%>
		<span class="badge badge-outline border-primary/60 font-semibold" data-sim-ascendancy>${ko ? "전직" : "Ascendancy"}: ${ko && r.ascendancyKo() != null ? r.ascendancyKo() : r.ascendancy()}@if(Boolean.TRUE.equals(r.ascendancyAuto()))<span class="ml-1 font-normal text-base-content/60" data-sim-ascendancy-auto>· ${ko ? "자동 선택" : "auto"}</span>@endif</span>
		<span class="badge badge-ghost">${ko ? "목표" : "Objective"}: ${objLabel.apply(r.objective())}</span>
		<span class="badge badge-ghost">${scenLabel.apply(r.scenario())}</span>
		<%-- 닌자 공통 특성(10-14 사용자 요청 "특정 1명이 아니라 그 직업/스킬의 특성을 참고") — 몇 명의 공통 트리 · 고유 · 보조 젬을 견줬는지, 트리 겹침 --%>
		@if(r.consensusN() != null && r.consensusN() > 0)
			<span class="badge badge-outline border-success/60" data-sim-consensus title="${ko ? "이 전직 · 스킬 실빌드들이 많이 찍는 노드로 키운 트리(무기 세트 1 · 2 따로), 절반 이상이 끼는 고유 · 보조 젬을 견줘 나은 것만 썼습니다" : "Compared a tree grown from the nodes these builds take most (weapon sets 1 and 2 separately) and the uniques and supports at least half of them use; only improvements were kept"}">${ko ? "닌자 " + r.consensusN() + "명 공통 특성" : "Common traits of " + r.consensusN() + " ninja builds"}@if(r.consensusOverlap() != null)<span class="ml-1 font-normal text-base-content/60">· ${ko ? "트리 겹침 " : "tree overlap "}${Math.round(r.consensusOverlap() * 100)}%</span>@endif</span>
		@endif
		@if(Integer.valueOf(0).equals(r.consensusN()))
			<span class="text-xs text-base-content/60" data-sim-no-consensus>${ko ? "이 전직 · 스킬은 공통 특성 집계 대상(많이 쓰는 상위 60개)이 아니라 실빌드 후보 중 최선에서만 출발했습니다" : "This ascendancy/skill is outside the common-traits set (top 60), so it started from the best candidate only"}</span>
		@endif
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
					<th class="text-right" title="${ko ? "실빌드 후보 중 엔진 계산이 가장 나은 한 명의 장비 · 젬 · 트리(공통 특성을 입히기 전)" : "Gear, gems and tree of the best-measured candidate, before common traits"}">${ko ? "출발 빌드" : "Start"}</th>
					<th class="text-right">${MessageUtil.getMessage("poe.refine.after")}</th>
					<th class="text-right">${MessageUtil.getMessage("poe.refine.change")}</th>
					<th class="text-right">${ko ? "poe.ninja 중앙값" : "poe.ninja median"}@if(bm != null && bm.sample() != null)<div class="text-[10px] font-normal text-base-content/60">n=${bm.sample()}</div>@endif</th>
				</tr>
			</thead>
			<tbody>
				<tr><td>DPS</td><td class="text-right font-mono">${fmt.apply(r.start().dps())}</td><td class="text-right font-mono font-semibold">${fmt.apply(r.refined().dps())}</td><td class="text-right font-mono">${chg.apply(r.start().dps(), r.refined().dps())}</td><td class="text-right font-mono text-base-content/60">${fmt.apply(bm == null ? null : bm.medianDps())}</td></tr>
				<tr><td>EHP</td><td class="text-right font-mono">${fmt.apply(r.start().ehp())}</td><td class="text-right font-mono font-semibold">${fmt.apply(r.refined().ehp())}</td><td class="text-right font-mono">${chg.apply(r.start().ehp(), r.refined().ehp())}</td><td class="text-right font-mono text-base-content/60">${fmt.apply(bm == null ? null : bm.medianEhp())}</td></tr>
				<tr><td>${ko ? "생명력" : "Life"}</td><td class="text-right font-mono">${fmt.apply(r.start().life())}</td><td class="text-right font-mono">${fmt.apply(r.refined().life())}</td><td class="text-right font-mono">${chg.apply(r.start().life(), r.refined().life())}</td><td class="text-right font-mono text-base-content/60">${fmt.apply(bm == null ? null : bm.medianLife())}</td></tr>
				<tr><td>${ko ? "에너지 보호막" : "ES"}</td><td class="text-right font-mono">${fmt.apply(r.start().es())}</td><td class="text-right font-mono">${fmt.apply(r.refined().es())}</td><td class="text-right font-mono">${chg.apply(r.start().es(), r.refined().es())}</td><td class="text-right font-mono text-base-content/60">${fmt.apply(bm == null ? null : bm.medianEs())}</td></tr>
			</tbody>
		</table>
	</div>
	@template.poe2.htmx.weaponSets(mainSet = r.mainSet(), sets = r.sets(), roles = r.setRoles())
	<div class="space-y-1">
		<div class="text-sm font-semibold">${ko ? "다듬기 단계" : "Refine steps"}</div>
		@if(r.steps() == null || r.steps().isEmpty())
			<div class="text-sm text-base-content/60">${ko ? "공통 특성 · 가이드 교체 중 목표를 더 올리는 것을 찾지 못했습니다 — 출발 빌드가 이미 잘 맞춰져 있습니다." : "Neither the common traits nor the guide swaps improved the objective — the starting build is already well tuned."}</div>
		@else
			<ol class="list-decimal space-y-0.5 pl-5 text-sm" data-sim-steps>
				@for(Poe2.RefineStep st : r.steps())
					<%-- 종류 배지(10-14 C320 — 빌드 가이드 "먼저 바꿀 것"과 같은 배지 말). "유지"로 끝나는 줄은 견주기만 하고 안 바꾼 것이라 흐리게 --%>
					!{
						String sl = st.label() == null ? "" : st.label();
						boolean kept = sl.endsWith("유지");
						String kind = sl.startsWith("패시브 재분배") ? (ko ? "재분배" : "Respec")
							: sl.startsWith("닌자 공통 트리") ? (ko ? "공통 트리" : "Common tree")
							: sl.startsWith("닌자 공통") ? (ko ? "닌자 공통" : "Ninja common")
							: sl.startsWith("보조 젬") || sl.startsWith("보조젬") ? (ko ? "보조 젬" : "Support") /* 옛 결과는 붙여 쓴 꼴(10-25 Q193) */
							: sl.startsWith("고유") ? (ko ? "장비" : "Item") : null;
						String kindCls = sl.startsWith("패시브 재분배") ? "badge-warning badge-outline" : sl.startsWith("닌자 공통") ? "badge-info badge-outline" : sl.startsWith("보조 젬") || sl.startsWith("보조젬") ? "badge-primary badge-outline" : "badge-ghost";
					}
					<li class="${kept ? "text-base-content/60" : ""}" data-sim-step-kind="${kind == null ? "" : kind}">@if(kind != null)<span class="badge badge-xs ${kindCls} mr-1 align-middle">${kind}</span>@endif${!ko && st.labelEn() != null ? st.labelEn() : st.label()} <span class="font-mono text-xs text-base-content/60">DPS ${chg.apply(100d, 100d * (1 + (st.dpsPct() == null ? 0 : st.dpsPct()) / 100))} · EHP ${chg.apply(100d, 100d * (1 + (st.ehpPct() == null ? 0 : st.ehpPct()) / 100))}</span></li>
				@endfor
			</ol>
		@endif
	</div>
	<%-- 전직 자동이면 왜 이 전직인가(C311) — 전직마다 그 전직 후보 중 목표 점수가 가장 높은 한 명의 엔진 DPS · EHP, 고른 전직 표시.
	     점수는 고를 때와 같은 규칙(균형 = DPS · EHP 로그 합, 공격 = DPS, 생존 = EHP — API Poe2SimService.candidateScore) --%>
	!{
		java.util.Map<String, java.util.List<Poe2.SimCandidate>> byAsc = new java.util.LinkedHashMap<>();
		if (Boolean.TRUE.equals(r.ascendancyAuto()) && r.candidates() != null) {
			for (Poe2.SimCandidate c : r.candidates()) {
				if (c.ascendancy() != null && c.dps() != null && c.ehp() != null) byAsc.computeIfAbsent(c.ascendancy(), k -> new java.util.ArrayList<>()).add(c);
			}
		}
		java.util.function.Function<Poe2.SimCandidate, Double> score = c -> "dps".equals(r.objective()) ? c.dps() : "ehp".equals(r.objective()) ? c.ehp() : Math.log(Math.max(c.dps(), 1)) + Math.log(Math.max(c.ehp(), 1));
	}
	@if(!byAsc.isEmpty())
		<div class="rounded-box border border-base-200 px-3 py-2 text-sm" data-sim-asc-compare>
			<div class="font-semibold">${ko ? "전직을 자동으로 고른 이유" : "Why this ascendancy"}</div>
			<div class="text-xs text-base-content/60">${r.ascCompare() != null
				? (ko ? "전직마다 가장 나은 실빌드 후보와, 거기에 그 전직 닌자 실빌드들의 공통 트리를 덮은 값을 우리 엔진으로 재서, 둘 중 나은 목표 점수(" + objLabel.apply(r.objective()) + ")가 가장 높은 전직을 골랐습니다." : "For each ascendancy, the best candidate and the same build with that ascendancy's ninja common tree were measured; the ascendancy with the best objective score of the two was chosen.")
				: (ko ? "같은 조건의 실빌드 후보를 우리 엔진으로 다시 계산해, 목표 점수(" + objLabel.apply(r.objective()) + ")가 가장 높은 전직을 골랐습니다." : "Real-build candidates were recalculated with our engine; the ascendancy with the best objective score was chosen.")}</div>
			<div class="overflow-x-auto">
				<table class="table table-xs w-auto mt-1">
					<thead><tr><th>${ko ? "전직" : "Ascendancy"}</th><th class="text-right">${ko ? "후보" : "Candidates"}</th><th class="text-right">${ko ? "엔진 DPS" : "Engine DPS"}</th><th class="text-right">${ko ? "엔진 EHP" : "Engine EHP"}</th>@if(r.ascCompare() != null)<th class="text-right" data-asc-tree-head>${ko ? "공통 트리 DPS" : "Common tree DPS"}</th><th class="text-right">${ko ? "공통 트리 EHP" : "Common tree EHP"}</th>@endif<th></th></tr></thead>
					<tbody>
						@for(java.util.Map.Entry<String, java.util.List<Poe2.SimCandidate>> e : byAsc.entrySet())
							!{
								Poe2.SimCandidate top = e.getValue().stream().max(java.util.Comparator.comparingDouble(score::apply)).orElse(null);
								boolean picked = e.getKey().equals(r.ascendancy());
								String name = e.getValue().get(0).ascendancyKo() != null && ko ? e.getValue().get(0).ascendancyKo() : e.getKey();
							}
							<tr class="${picked ? "font-semibold" : ""}" data-asc-row="${e.getKey()}">
								<td>${name}</td>
								<td class="text-right font-mono">${e.getValue().size()}</td>
								<td class="text-right font-mono">${fmt.apply(top == null ? null : top.dps())}</td>
								<td class="text-right font-mono">${fmt.apply(top == null ? null : top.ehp())}</td>
								@if(r.ascCompare() != null)
									!{ Poe2.AscCompare ac = r.ascCompare().stream().filter(x -> e.getKey().equals(x.ascendancy())).findFirst().orElse(null); }
									<td class="text-right font-mono" data-asc-tree="${e.getKey()}" title="${ac != null && ac.consensusN() != null ? (ko ? "닌자 " + ac.consensusN() + "명 공통 트리를 이 후보에 덮은 값" : "This candidate with the common tree of " + ac.consensusN() + " ninja builds") : (ko ? "이 전직은 공통 특성 집계 대상이 아닙니다" : "No common traits for this ascendancy")}">${ac == null || ac.treeDps() == null ? "-" : fmt.apply(ac.treeDps())}</td>
									<td class="text-right font-mono">${ac == null || ac.treeEhp() == null ? "-" : fmt.apply(ac.treeEhp())}</td>
								@endif
								<td>@if(picked)<span class="badge badge-primary badge-xs">${ko ? "선택" : "chosen"}</span>@endif</td>
							</tr>
						@endfor
					</tbody>
				</table>
			</div>
			@if(byAsc.size() == 1)
				<div class="text-[11px] text-base-content/60">${ko ? "이 스킬의 실빌드 후보가 한 전직에만 있었습니다." : "All candidates were from one ascendancy."}</div>
			@endif
		</div>
	@endif
	<details class="rounded-box bg-base-200/40 px-3 py-2 text-sm">
		<summary class="cursor-pointer font-semibold">${ko ? "출발점 후보(실빌드 " + r.candidates().size() + "명)" : "Starting-point candidates (" + r.candidates().size() + " real builds)"}</summary>
		<div class="overflow-x-auto">
			<table class="table table-xs w-auto" data-sim-candidates>
				<thead><tr><th>${ko ? "전직" : "Ascendancy"}</th><th>${ko ? "레벨" : "Lv"}</th><th class="text-right">ninja DPS</th><th class="text-right">ninja EHP</th><th class="text-right">${ko ? "엔진 DPS" : "Engine DPS"}</th><th class="text-right">${ko ? "엔진 EHP" : "Engine EHP"}</th><th>${ko ? "무기 세트" : "Set"}</th><th></th></tr></thead>
				<tbody>
					@for(Poe2.SimCandidate c : r.candidates())
						<tr class="${Boolean.TRUE.equals(c.chosen()) ? "font-semibold" : ""}">
							<td>${c.ascendancy() == null ? "-" : ko && c.ascendancyKo() != null ? c.ascendancyKo() : c.ascendancy()}</td>
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
		<div class="text-[11px] text-base-content/60">${ko ? "장비 · 젬 출발점을 정하려고 같은 시나리오로 우리 엔진에서 다시 계산했습니다(균형은 DPS·EHP 로그 합) — 가장 나은 한 명에서 출발해 공통 특성을 견줍니다." : "Recalculated under the same scenario to pick the gear and gem starting point (balanced = log DPS + log EHP); common traits are then compared on top of it."}</div>
	</details>
	<%-- 결과 PoB 코드 + 복사 — PoE1 시뮬 결과와 같은 자리(10-01: PoE2 엔 "빌드 화면에서 열기"만 있었다). PoB 에 붙여넣는 게 용도라 원클릭 복사 --%>
	@if(r.code() != null && !r.code().isBlank())
		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.sim.opt.result.code")}</div>
				<button type="button" class="btn btn-ghost btn-xs" data-copy-target="#poe2SimCode" data-copied-label="${MessageUtil.getMessage("poe.common.copied")}">${MessageUtil.getMessage("poe.common.copy")}</button>
			</div>
			<textarea id="poe2SimCode" readonly rows="3" class="textarea textarea-bordered w-full font-mono text-xs leading-5" aria-label="${MessageUtil.getMessage("poe.sim.opt.result.code")}">${r.code()}</textarea>
		</div>
	@endif
	<div class="flex flex-wrap items-center gap-2">
		<a class="btn btn-primary btn-sm" href="/poe2/build?sim=${historyId == null ? "1" : String.valueOf(historyId)}" data-sim-open>${MessageUtil.getMessage("poe.sim.result.openbuild")}</a>
		<%-- 안내 첫 문장은 PoE1 시뮬 결과와 같다(10-08) --%>
		<span class="text-xs text-base-content/60">${ko ? "아이템마다 거래소 링크 · 업그레이드 가이드는 빌드 화면에서 · 고유 아이템은 품질 20% 기준 · 단계 증감은 적용 뒤 엔진 실측 · 계보 젬은 드물게 얻는 젬" : "Trade links per item and the upgrade guide are on the build view · Uniques at 20% quality · step changes are engine-measured · lineage gems are rare"}</span>
	</div>
</div>
@endif
