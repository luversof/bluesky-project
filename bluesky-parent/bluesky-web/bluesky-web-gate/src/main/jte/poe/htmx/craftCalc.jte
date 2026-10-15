@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.CalcResponse calc = null
@param java.util.List<String> targetLabels = java.util.List.of()

<%-- 제작 계산기 결과(10-15 C336 — 사용자 요청 "가장 적은 비용을 쓰는 걸 계산해 주는 시뮬레이터"). 방법마다 시도 한 번의 성공 확률(엔진 몬테카를로) ·
     기대 시도 · 기대 비용(poe.ninja 거래소 시세, 카오스), 비용이 적은 순. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> num = v -> v == null ? "-" : v >= 100 ? String.format("%,.0f", v) : String.format("%,.2f", v);
}
<div id="craft-calc-result" class="space-y-2" data-craft-calc-result>
	@if(calc == null)
	@elseif(calc.error() != null)
		<div role="alert" class="alert alert-warning py-2 text-sm" data-craft-calc-error>${calc.error()}</div>
	@else
		<div class="text-xs text-base-content/60">
			${ko ? "목표: " : "Targets: "}${String.join(" · ", targetLabels)}
			@if(calc.league() != null) · ${ko ? "시세 " : "Prices "}${calc.league()}@if(calc.pricesAt() != null) (${calc.pricesAt().length() >= 16 ? calc.pricesAt().substring(0, 16).replace('T', ' ') : calc.pricesAt()} UTC)@endif @endif
		</div>
		<div class="overflow-x-auto">
			<table class="table table-sm">
				<thead><tr>
					<th>${ko ? "방법" : "Method"}</th>
					<th class="text-right">${ko ? "한 번 성공 확률" : "Chance per try"}</th>
					<th class="text-right">${ko ? "기대 시도" : "Expected tries"}</th>
					<th class="text-right">${ko ? "시도당 비용" : "Cost per try"}</th>
					<th class="text-right">${ko ? "기대 비용" : "Expected cost"}</th>
				</tr></thead>
				<tbody>
					@for(int i = 0; i < calc.results().size(); i++)
						!{ PoeCraft.MethodResult r = calc.results().get(i); }
						<tr class="${i == 0 && r.expectedCost() != null ? "font-semibold" : ""}" data-craft-calc-method="${r.method()}">
							<td class="min-w-0">
								${r.label()}@if(i == 0 && r.expectedCost() != null) <span class="badge badge-primary badge-xs">${ko ? "가장 저렴" : "cheapest"}</span>@endif
								@if(r.note() != null && !r.note().isEmpty())<div class="text-xs font-normal text-base-content/60">${r.note()}</div>@endif
							</td>
							<td class="text-right font-mono">${r.hits() == 0 ? "< " + String.format("%.4f%%", 100.0 / Math.max(1, r.trials())) : String.format(r.chance() < 0.01 ? "%.4f%%" : "%.2f%%", r.chance() * 100)}</td>
							<td class="text-right font-mono" data-craft-calc-attempts>${num.apply(r.expectedAttempts())}</td>
							<td class="text-right font-mono">${r.costPerAttempt() == null ? "-" : num.apply(r.costPerAttempt()) + "c"}</td>
							<td class="text-right font-mono" data-craft-calc-cost>${r.expectedCost() == null ? "-" : num.apply(r.expectedCost()) + "c"}</td>
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
		<p class="text-xs text-base-content/60">${ko ? "확률은 같은 제작 엔진으로 " : "Chances come from "}${calc.results().isEmpty() ? 0 : calc.results().get(0).trials()}${ko ? "번 모의 시행한 값입니다. 매우 낮은 확률은 오차가 큽니다." : " simulated tries with the same crafting engine; very low chances are noisy."}</p>
	@endif
</div>
