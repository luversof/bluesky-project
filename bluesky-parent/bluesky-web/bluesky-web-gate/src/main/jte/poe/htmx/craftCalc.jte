@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.CalcResponse calc = null
@param java.util.List<String> targetLabels = java.util.List.of()
@param int ghost = 0

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
		<div role="alert" class="alert alert-warning py-2 text-sm" data-craft-calc-error>${calc.errorFor(ko)}</div>
	@else
		<div class="text-xs text-base-content/60">
			${ko ? "목표: " : "Targets: "}${String.join(" · ", targetLabels)}
			@if(net.luversof.web.gate.poe.CraftPrices.current() != null)<span data-craft-calc-custom-prices> · ${ko ? "사용자 가격 " + net.luversof.web.gate.poe.CraftPrices.current().size() + "개" : net.luversof.web.gate.poe.CraftPrices.current().size() + " custom prices"}</span>@endif
			@if(calc.league() != null) · ${ko ? "시세 " : "Prices "}${calc.league()}@if(calc.pricesAt() != null) (${calc.pricesAt().length() >= 16 ? calc.pricesAt().substring(0, 16).replace('T', ' ') : calc.pricesAt()} UTC)@endif @endif
		</div>
		<%-- 가로 스크롤 단서(C424 UX QA — 375px 에서 화폐 · 90% 예산 열이 잘려 보였는데 밀 수 있다는 표시가 없었다) --%>
		<p class="text-xs text-base-content/60 sm:hidden" data-craft-scroll-hint>${ko ? "좁은 화면 — 표를 옆으로 밀어 나머지 열을 보세요" : "Narrow screen — swipe the table sideways for more columns"}</p>
		<%-- 엘드리치 임플리싯 목표(10-21 C549) — 명시 옵션 표와 따로 잰 불씨 · 영액 비용(임플리싯은 다시 굴려도 남고 불씨 · 영액은 임플리싯만 바꾼다) --%>
		@if(calc.eldritch() != null)
			!{
				PoeCraft.EldritchResult er = calc.eldritch();
				Double bestRow = calc.results() == null ? null : calc.results().stream().map(PoeCraft.MethodResult::expectedCost).filter(java.util.Objects::nonNull).min(Double::compare).orElse(null);
			}
			<div class="rounded-box border border-base-300 bg-base-100 p-2 text-xs space-y-0.5" data-craft-calc-eldritch>
				@if(er.error() != null)
					<div role="alert" class="text-warning" data-craft-calc-eldritch-error>${ko ? "엘드리치 임플리싯 목표: " : "Eldritch implicit targets: "}${er.errorFor(ko)}</div>
				@else
					<div class="font-medium">${ko ? "엘드리치 임플리싯 — 명시 옵션과 따로(불씨 · 영액은 임플리싯만 바꿈), 아래 방법 비용에 더함: " : "Eldritch implicits — separate from the explicit modifiers (embers/ichors only change implicits), added to the methods below: "}<span class="font-mono" data-craft-calc-eldritch-cost="${er.cost()}">${num.apply(er.cost())} c</span>
						@if(bestRow != null && er.cost() != null)<span class="text-base-content/70"> · ${ko ? "합계(가장 싼 방법 + 엘드리치) " : "Total (cheapest method + eldritch) "}<span class="font-mono" data-craft-calc-eldritch-total>${num.apply(bestRow + er.cost())} c</span></span>@endif</div>
					@for(PoeCraft.EldritchStep s : er.steps())
						!{ java.util.List<String> st = ko && s.ko() != null && !s.ko().isEmpty() ? s.ko() : (s.en() == null ? java.util.List.<String>of() : s.en()); }
						<div data-craft-calc-eldritch-step="${s.side()}">${"exarch".equals(s.side()) ? (ko ? "작열의 총주교" : "Exarch") : (ko ? "세계 포식자" : "Eater")}: ${String.join(", ", st)} — ${ko ? s.currencyKo() : s.currency()} ${ko ? "평균 " : "avg "}${String.format("%,.1f", s.attempts())}${ko ? "개" : ""}(${ko ? "한 개 " : "per orb "}${String.format("%.1f%%", s.chance() * 100)}) = <span class="font-mono">${num.apply(s.cost())} c</span></div>
					@endfor
				@endif
			</div>
		@endif
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
								${ko || r.labelEn() == null ? r.label() : r.labelEn()}@if(i == 0 && r.expectedCost() != null) <span class="badge badge-primary badge-xs">${ko ? "가장 저렴" : "cheapest"}</span>@endif
								!{ String rn = ko ? r.note() : r.noteEn() != null ? r.noteEn() : net.luversof.web.gate.poe.PoeCraftText.en(r.note()); }
								@if(rn != null && !rn.isEmpty())<div class="text-xs font-normal text-base-content/60">${rn}</div>@endif
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
		@if(ghost >= 2)
			<%-- 올플레임 제작(10-21 C532, C523 · C524 의 계산기 짝) — 같은 화폐를 되풀이하는 방법(카오스 · 변화 · 에센스 · 화석 · 장막)은 매번 원본에 새로 적용해
			     복제본 N 개면 한 번 성공 1 − (1 − p)^N. 수확(작업대)과 여러 단계 방법은 빼고, 망자의 유황은 시세가 없어 비용에 넣지 않음 --%>
			!{ java.util.List<PoeCraft.MethodResult> gr = calc.results().stream().filter(r -> r.hits() > 0 && r.costPerAttempt() != null && (java.util.Set.of("CHAOS", "ALT").contains(r.method()) || r.method().startsWith("ESSENCE") || r.method().startsWith("FOSSIL") || r.method().startsWith("VEILED"))).toList(); }
			@if(!gr.isEmpty())
				<div class="overflow-x-auto" data-craft-calc-ghost="${ghost}">
					<table class="table table-xs">
						<caption class="text-left text-xs font-medium pb-1">${ko ? "올플레임 복제본 " + ghost + "개 중 고르기(에뮬레이터 설정 · 망자의 유황 제외)" : "Allflame, best of " + ghost + " Ghostly Copies (emulator setting · Dead Man's Sulphur excluded)"}</caption>
						<thead><tr><th>${ko ? "방법" : "Method"}</th><th class="text-right">${ko ? "한 번 성공" : "Per try"}</th><th class="text-right">${ko ? "기대 시도" : "Expected tries"}</th><th class="text-right">${ko ? "기대 비용" : "Expected cost"}</th></tr></thead>
						<tbody>
							@for(PoeCraft.MethodResult r : gr)
								!{ double gp = 1 - Math.pow(1 - r.chance(), ghost); double ga = 1 / gp; }
								<tr data-craft-calc-ghost-method="${r.method()}">
									<td class="min-w-0">${ko || r.labelEn() == null ? r.label() : r.labelEn()}</td>
									<td class="text-right font-mono">${String.format(gp < 0.01 ? "%.4f%%" : "%.2f%%", gp * 100)}</td>
									<td class="text-right font-mono">${num.apply(ga)}</td>
									<td class="text-right font-mono" data-craft-calc-ghost-cost>${num.apply(ga * r.costPerAttempt()) + "c"}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			@endif
		@endif
		<p class="text-xs text-base-content/60">${ko ? "확률은 같은 제작 엔진으로 " : "Chances come from "}${calc.results().isEmpty() ? 0 : calc.results().get(0).trials()}${ko ? "번 모의 시행한 값입니다. 매우 낮은 확률은 오차가 큽니다." : " simulated tries with the same crafting engine; very low chances are noisy."} ${ko ? "90% 예산 = 시도마다 독립일 때 90% 확률로 한 번은 성공하는 비용(기대 비용의 약 2.3배)." : "90% budget = cost to succeed at least once with 90% probability for independent tries (about 2.3× the expected cost)."}</p>
	@endif
</div>
