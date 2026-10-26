@import net.luversof.web.gate.poe2.dto.Poe2Craft
@import net.luversof.web.gate.poe.PoeText

@param Poe2Craft.CalcResponse calc = null
@param java.util.List<String> targetLabels = java.util.List.of()
@param java.util.Map<String, String> nameKo = java.util.Map.of()

<%-- PoE2 시뮬레이터 결과(10-15 C352) — 비용 순, 첫 줄 = 가장 저렴. 비용은 디바인(엑잘 환산 함께), 베이스 값 제외, 가중치 균등 가정. --%>
!{
	boolean ko = PoeText.isKorean();
	Double exPerDiv = calc == null ? null : calc.exaltPerDivine();
	// 90% 예산(C368) — 회차마다 독립(새 베이스): 회차당 비용 = 성공당 비용 × 성공률, 회차 수 = ceil(ln 0.1 / ln(1 - 성공률))
	java.util.function.Function<Double, Long> n90 = p -> p == null || p <= 0 ? null : p >= 1 ? 1L : (long) Math.ceil(Math.log(0.1) / Math.log(1 - p));
}
<div id="craft2-calc-result" class="space-y-2" data-craft2-calc-result>
	@if(calc != null)
		@if(calc.error() != null)
			<div role="alert" class="alert alert-warning py-2 text-sm">${ko ? calc.error() : net.luversof.web.gate.poe2.Poe2CraftText.en(calc.error())}</div>
		@else
			@if(!targetLabels.isEmpty())
				<p class="text-xs text-base-content/70">${ko ? "목표: " : "Targets: "}${String.join(" · ", targetLabels)}</p>
			@endif
			<%-- 가로 스크롤 단서(C424 UX QA — 375px 에서 화폐 · 90% 예산 열이 잘려 보였는데 밀 수 있다는 표시가 없었다) --%>
			<p class="text-xs text-base-content/60 sm:hidden" data-craft-scroll-hint>${ko ? "좁은 화면 — 표를 옆으로 밀어 나머지 열을 보세요" : "Narrow screen — swipe the table sideways for more columns"}</p>
			<div class="overflow-x-auto">
				<table class="table table-sm">
					<thead><tr><th class="min-w-[9rem]">${ko ? "방법" : "Method"}</th><th class="text-right" title="${ko ? "시도 한 번(카오스 반복처럼 상태가 남는 방법은 회차 한 번)이 목표를 채울 확률" : "Chance per attempt (per run for methods that keep state, e.g. repeated Chaos)"}">${ko ? "성공률(시도 · 회차당)" : "Success (per attempt/run)"}</th><th>${ko ? "화폐(성공당 개수)" : "Currency per success"}</th><th class="text-right">${ko ? "성공당 비용" : "Cost / success"}</th><th class="text-right" title="${ko ? "이만큼 쓰면 90% 확률로 한 번은 성공(회차마다 독립)" : "Spend this much to succeed at least once with 90% probability (independent runs)"}">${ko ? "90% 예산" : "90% budget"}</th></tr></thead>
					<tbody>
						@for(int i = 0; i < calc.results().size(); i++)
							!{ Poe2Craft.CalcMethod m = calc.results().get(i); }
							<tr data-craft2-calc-method="${m.method()}">
								<td class="min-w-[9rem]">
									!{ String lab = !ko && m.labelEn() != null ? m.labelEn() : m.label(); if (ko && lab != null) for (var en : nameKo.entrySet()) if (lab.contains(en.getKey())) lab = lab.replace(en.getKey(), en.getValue()); String mnote = !ko && m.noteEn() != null ? m.noteEn() : m.note(); }
									<div class="font-medium">${lab}@if(i == 0 && m.costPerSuccess() != null) <span class="badge badge-sm badge-success badge-outline" title="${ko ? "이 표의 한 가지 방법 중 가장 저렴 — 여러 수단을 섞는 최적 조합은 위 결과" : "Cheapest single method in this table — the mixed optimal plan is above"}">${ko ? "가장 저렴" : "Cheapest"}</span>@endif</div>
									@if(mnote != null)<div class="text-xs text-base-content/70">${mnote}</div>@endif
									<%-- 좁은 화면(10-23 Q32 ⑪, PoE1 짝) — 성공당 비용을 방법 이름 아래에도 --%>
									<div class="sm:hidden text-xs font-mono" data-craft2-calc-cost-mobile>${ko ? "성공당 " : "Per success "}@if(m.costPerSuccess() != null)${String.format("%.2f", m.costPerSuccess())} div@if(exPerDiv != null) (≈ ${String.format("%,.0f", m.costPerSuccess() * exPerDiv)} ex)@endif@else-@endif</div>
								</td>
								<td class="text-right font-mono">${String.format("%.2f%%", m.successRate() * 100)}</td>
								<%-- 열 순서 = 계산 흐름(C439 UX QA): 성공률 · 화폐 개수 → 성공당 비용 → 90% 예산 --%>
								<td class="text-xs">
									@for(var e : m.perSuccess().entrySet())
										<div>${ko ? nameKo.getOrDefault(e.getKey(), e.getKey()) : e.getKey()} × ${String.format("%,.1f", e.getValue())}</div>
									@endfor
								</td>
								<td class="text-right font-mono" data-craft2-calc-cost>@if(m.costPerSuccess() != null)${String.format("%.2f", m.costPerSuccess())} div@if(exPerDiv != null)<div class="text-xs text-base-content/70">≈ ${String.format("%,.0f", m.costPerSuccess() * exPerDiv)} ex</div>@endif@else-@endif</td>
								<%-- 반쪽 재조합(C409 검토)은 시도마다 반쪽 제작 비용도 흔들려 성공률만으로 낸 90% 예산이 낮게 나온다 → 셈하지 않음 --%>
								!{ boolean recombRow = m.method() != null && m.method().startsWith("RECOMB"); Long k90 = m.costPerSuccess() == null || recombRow ? null : n90.apply(m.successRate()); }
								<td class="text-right font-mono" data-craft2-calc-p90 title="${recombRow ? (ko ? "반쪽 재조합은 반쪽 제작 비용의 변동까지 들어간 90% 예산을 계산하지 않습니다" : "Not computed for half-and-recombine — the halves' own cost variance would be missing") : null}">@if(k90 != null)${String.format("%.2f", k90 * m.costPerSuccess() * m.successRate())} div@else-@endif</td>
							</tr>
						@endfor
					</tbody>
				</table>
			</div>
			@if(net.luversof.web.gate.poe.CraftPrices.current() != null)<p class="text-xs text-base-content/70" data-craft-calc-custom-prices>${ko ? "사용자 가격 " + net.luversof.web.gate.poe.CraftPrices.current().size() + "개 반영" : net.luversof.web.gate.poe.CraftPrices.current().size() + " custom prices applied"}</p>@endif
			<p class="text-xs text-base-content/70">${ko ? "시세: poe.ninja " : "Prices: poe.ninja "}${calc.league() == null ? "-" : calc.league()}@if(calc.pricesAt() != null) (${calc.pricesAt().substring(0, Math.min(16, calc.pricesAt().length()))})@endif · ${ko ? "가중치 균등 가정(PoE2 비공개) · 베이스 값 = '가격 직접 입력'의 베이스 아이템(비우면 제외) · 90% 예산 = 회차마다 독립일 때 90% 확률로 한 번은 성공하는 비용" : "uniform weights (PoE2 hidden) · base cost = 'Base item' in Custom prices (blank = excluded) · 90% budget = cost to succeed at least once with 90% probability"}</p>
		@endif
	@endif
</div>
