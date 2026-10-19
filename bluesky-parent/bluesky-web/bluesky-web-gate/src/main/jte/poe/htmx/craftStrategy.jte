@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.StrategyResult result = null
@param String presetLabel = ""
@param java.util.List<String> targetLabels = java.util.List.of()

<%-- 다단계 전략 시뮬레이터 결과(10-15 C339 — craftofexile 시뮬레이터처럼 단계 · 조건). 전략 한 회차 = 단계를 차례로(반복 단계는 목표 일부를 채울
     때까지), 끝에 모든 목표를 채웠으면 성공. 성공 한 번당 기대 비용 = 회차 비용 / 성공률. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> num = v -> v == null ? "-" : v >= 100 ? String.format("%,.0f", v) : v >= 1 ? String.format("%,.2f", v) : String.format("%,.3f", v);
	java.util.Map<String, String> actKo = new java.util.HashMap<>();
	actKo.put("TRANSMUTE", "진화"); actKo.put("ALTERATION", "변화"); actKo.put("AUGMENTATION", "확장"); actKo.put("REGAL", "제왕");
	actKo.put("ALCHEMY", "연금"); actKo.put("CHAOS", "카오스"); actKo.put("EXALT", "엑잘"); actKo.put("ANNUL", "소멸"); actKo.put("SCOUR", "정제");
	actKo.put("ESSENCE", "에센스"); actKo.put("FOSSIL", "화석"); actKo.put("BENCH", "제작대"); actKo.put("HARVEST", "수확"); actKo.put("SEQ", "묶음 반복");
	actKo.put("CONQUEROR", "정복자"); actKo.put("FRACTURE", "분열");
}
<div id="craft-strategy-result" class="space-y-2" data-craft-strategy-result>
	@if(result != null)
		@if(result.runs() == 0)
			<div role="alert" class="alert alert-warning py-2 text-sm" data-craft-strategy-error>${result.error()}</div>
		@else
			<div class="text-xs text-base-content/60">${ko ? "전략: " : "Strategy: "}${presetLabel} · ${ko ? "목표: " : "Targets: "}${String.join(" · ", targetLabels)}</div>
			<div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
				<div class="rounded-box border border-base-300 px-3 py-2">
					<div class="text-xs text-base-content/60">${ko ? "회차 성공률" : "Success per run"}</div>
					<div class="text-lg font-semibold font-mono" data-craft-strategy-rate>${String.format(result.successRate() < 0.01 ? "%.3f%%" : "%.1f%%", result.successRate() * 100)}</div>
					<div class="text-xs text-base-content/60">${ko ? "모의 " : ""}${String.format("%,d", result.runs())}${ko ? "회 중 " : " runs, "}${String.format("%,d", result.successes())}${ko ? "회" : " hits"}</div>
				</div>
				<div class="rounded-box border border-base-300 px-3 py-2">
					<div class="text-xs text-base-content/60">${ko ? "회차당 비용" : "Cost per run"}</div>
					<div class="text-lg font-semibold font-mono">${result.costPerRun() == null ? "-" : num.apply(result.costPerRun()) + "c"}</div>
				</div>
				<div class="rounded-box border border-base-300 px-3 py-2">
					<div class="text-xs text-base-content/60">${ko ? "성공 한 번당 기대 비용" : "Expected cost per success"}</div>
					<div class="text-lg font-semibold font-mono" data-craft-strategy-cost>${result.expectedCost() == null ? "-" : num.apply(result.expectedCost()) + "c"}</div>
				</div>
			</div>
			@if(result.error() != null)<div role="alert" class="alert alert-warning py-2 text-sm">${result.error()}</div>@endif
			<div class="grid gap-3 md:grid-cols-2">
				<div class="overflow-x-auto">
					<table class="table table-xs">
						<thead><tr><th>${ko ? "단계" : "Step"}</th><th class="text-right">${ko ? "회차당 사용" : "Uses per run"}</th><th class="text-right">${ko ? "도달" : "Reached"}</th></tr></thead>
						<tbody>
							@for(int i = 0; i < result.steps().size(); i++)
								!{ PoeCraft.StepStat st = result.steps().get(i); }
								<tr data-craft-strategy-step="${st.action()}">
									<td>${i + 1}. ${ko ? actKo.getOrDefault(st.action(), st.action()) : st.action()}</td>
									<td class="text-right font-mono">${num.apply(st.avgUses())}</td>
									<td class="text-right font-mono">${String.format("%.1f%%", st.reached() * 100)}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
				<div class="overflow-x-auto">
					<table class="table table-xs">
						<thead><tr><th>${ko ? "화폐" : "Currency"}</th><th class="text-right">${ko ? "회차당 평균" : "Avg per run"}</th></tr></thead>
						<tbody>
							@for(var e : result.perRun().entrySet())
								<tr><td class="min-w-0 wrap-anywhere">${e.getKey()}</td><td class="text-right font-mono">${num.apply(e.getValue())}</td></tr>
							@endfor
						</tbody>
					</table>
				</div>
			</div>
		@endif
	@endif
</div>
