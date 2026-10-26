@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft
@import net.luversof.web.gate.poe.PoeModRange

@param PoeCraft.StrategyResult result = null
@param String presetLabel = ""
@param java.util.List<String> targetLabels = java.util.List.of()
@param java.util.List<String> sampleLinks = java.util.List.of()

<%-- 다단계 전략 시뮬레이터 결과(10-15 C339 — craftofexile 시뮬레이터처럼 단계 · 조건). 전략 한 회차 = 단계를 차례로(반복 단계는 목표 일부를 채울
     때까지), 끝에 모든 목표를 채웠으면 성공. 성공 한 번당 기대 비용 = 회차 비용 / 성공률. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> num = v -> v == null ? "-" : v >= 100 ? String.format("%,.0f", v) : v >= 1 ? String.format("%,.2f", v) : String.format("%,.3f", v);
	java.util.Map<String, String> actKo = new java.util.HashMap<>();
	actKo.put("TRANSMUTE", "진화"); actKo.put("ALTERATION", "변화"); actKo.put("AUGMENTATION", "확장"); actKo.put("REGAL", "제왕");
	actKo.put("ALCHEMY", "연금술"); actKo.put("CHAOS", "카오스"); actKo.put("EXALT", "엑잘티드"); actKo.put("ANNUL", "소멸"); actKo.put("SCOUR", "정제");
	actKo.put("ESSENCE", "에센스"); actKo.put("FOSSIL", "화석"); actKo.put("BENCH", "작업대"); actKo.put("HARVEST", "수확"); actKo.put("SEQ", "묶음 반복");
	actKo.put("CONQUEROR", "정복자"); actKo.put("FRACTURE", "분열");
	// 영어 단계 이름(10-23 Q31 — 'TRANSMUTE' 같은 내부 코드가 그대로 나왔다): 묶음만 따로, 나머지는 첫 글자만 대문자
	java.util.Map<String, String> actEnMap = java.util.Map.ofEntries(java.util.Map.entry("SEQ", "Group (repeat)"), java.util.Map.entry("TRANSMUTE", "Transmute"), java.util.Map.entry("AUGMENTATION", "Augment"), java.util.Map.entry("EXALT", "Exalted"), java.util.Map.entry("ANNUL", "Annulment"), java.util.Map.entry("SCOUR", "Scouring"), java.util.Map.entry("FRACTURE", "Fracturing"), java.util.Map.entry("CONQUEROR", "Conqueror's Exalted Orb"), java.util.Map.entry("BENCH", "Bench craft"), java.util.Map.entry("VEILED_CHAOS", "Veiled Chaos"));
	java.util.function.Function<String, String> actEn = a -> a == null || a.isEmpty() ? "" : actEnMap.getOrDefault(a, Character.toUpperCase(a.charAt(0)) + a.substring(1).toLowerCase().replace('_', ' '));
}
<div id="craft-strategy-result" class="space-y-2" data-craft-strategy-result>
	@if(result != null)
		@if(result.runs() == 0)
			<div role="alert" class="alert alert-warning py-2 text-sm" data-craft-strategy-error>${ko ? result.error() : net.luversof.web.gate.poe.PoeCraftText.en(result.error())}</div>
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
			@if(result.error() != null)<div role="alert" class="alert alert-warning py-2 text-sm">${ko ? result.error() : net.luversof.web.gate.poe.PoeCraftText.en(result.error())}</div>@endif
			<div class="grid gap-3 md:grid-cols-2">
				<div class="overflow-x-auto">
					<table class="table table-xs">
						<thead><tr><th>${ko ? "단계" : "Step"}</th><th class="text-right">${ko ? "회차당 사용" : "Uses per run"}</th><th class="text-right">${ko ? "도달" : "Reached"}</th></tr></thead>
						<tbody>
							@for(int i = 0; i < result.steps().size(); i++)
								!{ PoeCraft.StepStat st = result.steps().get(i); }
								<tr data-craft-strategy-step="${st.action()}">
									<td>${i + 1}. ${ko ? actKo.getOrDefault(st.action(), st.action()) : actEn.apply(st.action())}</td>
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
			<p class="text-xs text-base-content/70" data-craft-strategy-stop-note>${ko ? "목표를 다 채우면 그 회차는 거기서 끝납니다 — 남은 단계는 쓰지 않고, '도달'은 그 단계까지 간 회차 비율입니다." : "A run ends as soon as all targets are met — later steps are not used, and 'Reached' is the share of runs that got to that step."}</p>
			<%-- 성공 예시(10-22 Q16 — craftofexile 시뮬레이터 'Store Items' · 'Load in Crafting Interface'). 목표를 채운 처음 몇 개 —
			     같이 붙은 옵션을 보고, 링크로 제작 화면에서 이어 만든다. 줄은 접두 → 접미, 범위는 굴림 전(최소–최대) --%>
			@if(result.samples() != null && !result.samples().isEmpty())
				<details class="rounded-box border border-base-300" data-craft-strategy-samples>
					<summary class="cursor-pointer select-none px-3 py-1.5 text-sm font-medium">${ko ? "성공 예시" : "Successful items"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "모의에서 목표를 채운 처음 " + result.samples().size() + "개" : "first " + result.samples().size() + " that met the targets"}</span></summary>
					<ol class="grid gap-2 px-3 pb-3 sm:grid-cols-2">
						@for(int k = 0; k < result.samples().size(); k++)
							!{
								PoeCraft.StrategySample sm = result.samples().get(k);
								java.util.List<PoeCraft.Line> ls = new java.util.ArrayList<>(sm.lines() == null ? java.util.List.of() : sm.lines());
								ls.sort(java.util.Comparator.comparing(l -> "prefix".equals(l.gen()) ? 0 : 1));
							}
							<li class="min-w-0 space-y-0.5 rounded-box border border-base-300 px-2 py-1.5 text-xs" data-craft-strategy-sample>
								<div class="flex items-center justify-between gap-2">
									<span class="font-medium">${ko ? "예시 " : "Item "}${k + 1}</span>
									@if(k < sampleLinks.size())<a class="link link-primary" href="${sampleLinks.get(k)}" target="_blank" rel="noopener" data-craft-strategy-sample-open>${ko ? "제작 화면에서 열기" : "Open in the crafting bench"}</a>@endif
								</div>
								@for(PoeCraft.Line l : ls)
									!{
										boolean useKo = ko && l.ko() != null && !l.ko().isEmpty();
										java.util.List<String> mx = useKo ? l.ko() : l.en();
										java.util.List<String> mn = useKo ? l.koMin() : l.enMin();
										java.util.List<String> parts = new java.util.ArrayList<>();
										for (int i = 0; mx != null && i < mx.size(); i++) parts.add(PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)));
									}
									<div class="flex min-w-0 items-baseline gap-1" data-craft-strategy-sample-line="${l.id()}">
										<span class="shrink-0 text-base-content/60">${"prefix".equals(l.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}@if(l.tierRank() != null) T${l.tierRank()}@endif</span>
										<span class="min-w-0 wrap-anywhere">$unsafe{PoeText.highlightValues(String.join(", ", parts))}@if(l.crafted())<span class="text-base-content/60"> (${ko ? "제작" : "crafted"})</span>@endif@if(l.fractured())<span class="text-base-content/60"> (${ko ? "분열" : "fractured"})</span>@endif@if(l.essence())<span class="text-base-content/60"> (${ko ? "에센스" : "essence"})</span>@endif</span>
									</div>
								@endfor
							</li>
						@endfor
					</ol>
				</details>
			@endif
		@endif
	@endif
</div>
