@import net.luversof.web.gate.poe2.dto.Poe2Craft
@import net.luversof.web.gate.poe.PoeText

@param Poe2Craft.StrategyResult result = null
@param java.util.List<String> targetLabels = java.util.List.of()
@param java.util.Map<String, String> nameKo = java.util.Map.of()
@param java.util.Map<String, String> argName = java.util.Map.of()

<%-- PoE2 다단계 전략 결과(10-15 C355) — 성공률 · 회차 비용 · 성공 한 번당 비용(디바인), 단계 표, 회차당 화폐 · 징조 개수. 가중치 균등 가정. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.Map<String, String> actKo = new java.util.HashMap<>();
	actKo.put("TRANSMUTE", "진화"); actKo.put("AUGMENTATION", "확장"); actKo.put("REGAL", "제왕"); actKo.put("ALCHEMY", "연금술");
	actKo.put("EXALT", "엑잘티드"); actKo.put("CHAOS", "카오스"); actKo.put("ANNUL", "소멸"); actKo.put("FRACTURE", "분열");
	actKo.put("ESSENCE", "에센스"); actKo.put("DESECRATE", "훼손"); actKo.put("REVEAL", "드러내기"); actKo.put("SEQ", "묶음");
}
<div id="craft2-strategy-result" class="space-y-2" data-craft2-strategy-result>
	@if(result != null)
		@if(result.error() != null)
			<div role="alert" class="alert alert-warning py-2 text-sm">${ko ? result.error() : net.luversof.web.gate.poe2.Poe2CraftText.en(result.error())}</div>
		@else
			@if(!targetLabels.isEmpty())
				<p class="text-xs text-base-content/70">${ko ? "목표: " : "Targets: "}${String.join(" · ", targetLabels)}</p>
			@endif
			<div class="flex flex-wrap gap-4 text-sm">
				<div>${ko ? "성공률 " : "Success "}<span class="font-mono font-semibold" data-craft2-strategy-rate>${String.format("%.2f%%", result.successRate() * 100)}</span> <span class="text-xs text-base-content/70">(${result.successes()}/${result.runs()})</span></div>
				<div>${ko ? "회차 비용 " : "Cost / run "}<span class="font-mono">@if(result.costPerRun() != null)${String.format("%.3f", result.costPerRun())} div@else-@endif</span></div>
				<div>${ko ? "성공 한 번당 " : "Cost / success "}<span class="font-mono font-semibold" data-craft2-strategy-cost>@if(result.expectedCost() != null)${String.format("%.2f", result.expectedCost())} div@else-@endif</span></div>
			</div>
			@if(result.successes() == 0)
				<%-- 성공 0건(C444 UX QA — 불가능인지 드문지 알 수 없었다) --%>
				<p class="text-xs text-base-content/70" data-craft2-strategy-zero>${ko ? "모의 " + result.runs() + "회 중 성공이 없습니다 — 이 단계로는 목표가 불가능하거나 매우 드뭅니다. 아래 단계 표의 '도달'로 어디까지 갔는지 보고, 최대 횟수를 늘리거나 단계 · 목표를 바꿔 보세요." : "No success in " + result.runs() + " runs — the targets are impossible or very rare with these steps. Check 'Reached' below, then raise the max uses or change steps/targets."}</p>
			@endif
			<div class="overflow-x-auto">
				<table class="table table-xs">
					<thead><tr><th>#</th><th>${ko ? "단계" : "Step"}</th><th class="text-right">${ko ? "회차당 사용" : "Uses / run"}</th><th class="text-right">${ko ? "도달" : "Reached"}</th></tr></thead>
					<tbody>
						@for(int i = 0; i < result.steps().size(); i++)
							!{ Poe2Craft.StepStat s = result.steps().get(i); }
							<tr data-craft2-strategy-step="${i}">
								<td class="font-mono">${i + 1}</td>
								<td>${ko ? actKo.getOrDefault(s.action(), s.action()) : s.action()}@if(s.arg() != null) · <span class="text-xs">${argName.getOrDefault(s.arg(), s.arg())}</span>@endif</td>
								<td class="text-right font-mono">${String.format("%.2f", s.avgUses())}</td>
								<td class="text-right font-mono">${String.format("%.0f%%", s.reached() * 100)}</td>
							</tr>
						@endfor
					</tbody>
				</table>
			</div>
			<div class="text-xs">
				@for(var e : result.perRun().entrySet())
					<span class="inline-block mr-3">${ko ? nameKo.getOrDefault(e.getKey(), e.getKey()) : e.getKey()} × ${String.format("%.2f", e.getValue())}</span>
				@endfor
			</div>
			<p class="text-xs text-base-content/70">${ko ? "회차 단위 모의(화폐를 못 쓰면 그 회차 중단) · 가중치 균등 가정(PoE2 비공개) · 베이스 값 제외" : "Per-run simulation (a run stops when a currency cannot be used) · uniform weights (PoE2 hidden) · base excluded"}</p>
		@endif
	@endif
</div>
