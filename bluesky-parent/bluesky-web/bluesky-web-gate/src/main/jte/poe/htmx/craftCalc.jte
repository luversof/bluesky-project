@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.CalcResponse calc = null
@param java.util.List<String> targetLabels = java.util.List.of()

<%-- 제작 계산기 결과(10-15 C336 — 사용자 요청 "가장 적은 비용을 쓰는 걸 계산해 주는 시뮬레이터"). 방법마다 시도 한 번의 성공 확률(엔진 몬테카를로) ·
     기대 시도 · 기대 비용(poe.ninja 거래소 시세, 카오스), 비용이 적은 순. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> num = v -> v == null ? "-" : v >= 100 ? String.format("%,.0f", v) : String.format("%,.2f", v);
	// 90% 예산(C368) — 시도마다 독립(기하분포): 이만큼 시도하면 90% 확률로 한 번은 성공
	java.util.function.Function<Double, Long> n90 = p -> p == null || p <= 0 ? null : p >= 1 ? 1L : (long) Math.ceil(Math.log(0.1) / Math.log(1 - p));
}
<div id="craft-calc-result" class="space-y-2" data-craft-calc-result>
	@if(calc != null && calc.error() == null)
		<h3 class="text-sm font-semibold">${ko ? "방법별 비교 — 한 가지 방법만 반복할 때" : "Per-method comparison — repeating a single method"}</h3>
	@endif
	@if(calc == null)
	@elseif(calc.error() != null)
		<div role="alert" class="alert alert-warning py-2 text-sm" data-craft-calc-error>${calc.error()}</div>
	@else
		<div class="text-xs text-base-content/60">
			${ko ? "목표: " : "Targets: "}${String.join(" · ", targetLabels)}
			@if(calc.league() != null) · ${ko ? "시세 " : "Prices "}${calc.league()}@if(calc.pricesAt() != null) (${calc.pricesAt().length() >= 16 ? calc.pricesAt().substring(0, 16).replace('T', ' ') : calc.pricesAt()} UTC)@endif @endif
		</div>
		<%-- 가로 스크롤 단서(C424 UX QA — 375px 에서 화폐 · 90% 예산 열이 잘려 보였는데 밀 수 있다는 표시가 없었다) --%>
		<p class="text-xs text-base-content/60 sm:hidden" data-craft-scroll-hint>${ko ? "좁은 화면 — 표를 옆으로 밀어 나머지 열을 보세요" : "Narrow screen — swipe the table sideways for more columns"}</p>
		<div class="overflow-x-auto">
			<table class="table table-sm">
				<thead><tr>
					<th class="min-w-[9rem]">${ko ? "방법" : "Method"}</th>
					<th class="text-right">${ko ? "한 번 성공 확률" : "Chance per try"}</th>
					<th class="text-right">${ko ? "기대 시도" : "Expected tries"}</th>
					<th class="text-right">${ko ? "시도당 비용" : "Cost per try"}</th>
					<th class="text-right">${ko ? "기대 비용" : "Expected cost"}</th>
					<th class="text-right" title="${ko ? "이만큼 쓰면 90% 확률로 한 번은 성공(시도마다 독립)" : "Spend this much to succeed at least once with 90% probability (independent tries)"}">${ko ? "90% 예산" : "90% budget"}</th>
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
							!{ Long k90 = r.hits() == 0 || r.costPerAttempt() == null ? null : n90.apply(r.chance()); }
							<td class="text-right font-mono" data-craft-calc-p90>${k90 == null ? "-" : num.apply(k90 * r.costPerAttempt()) + "c"}</td>
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
		<p class="text-xs text-base-content/60">${ko ? "확률은 같은 제작 엔진으로 " : "Chances come from "}${calc.results().isEmpty() ? 0 : calc.results().get(0).trials()}${ko ? "번 모의 시행한 값입니다. 매우 낮은 확률은 오차가 큽니다." : " simulated tries with the same crafting engine; very low chances are noisy."} ${ko ? "90% 예산 = 시도마다 독립일 때 90% 확률로 한 번은 성공하는 비용(기대 비용의 약 2.3배)." : "90% budget = cost to succeed at least once with 90% probability for independent tries (about 2.3× the expected cost)."}</p>
	@endif
</div>
