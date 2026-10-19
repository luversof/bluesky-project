@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.OptimizeResult opt = null
@param String optError = null
@param java.util.List<String> targetLabels = java.util.List.of()

<%-- 최적 조합(10-19 C457 사용자 요청 "에센스 · 제작대 · 야수 같은 다양한 방법을 조합해 원하는 아이템을 뽑는 최소 비용") — 계산기 표 아래.
     제작 과정을 상태(희귀도 · 맞은 목표 · 잡옵션 · 막힌 자리 · 제작대 옵션)로 보고 상태마다 가장 싼 수단을 찾은 정책, 그 정책을 실제 아이템에 굴려 잰 비용. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> num = v -> v == null ? "-" : v >= 100 ? String.format("%,.0f", v) : v >= 1 ? String.format("%,.1f", v) : String.format("%,.3f", v);
	String err = optError != null ? optError : opt == null ? (ko ? "결과가 없습니다" : "No result") : opt.error();
	Double gain = opt == null || opt.expectedCost() == null || opt.upper() == null || opt.upper() <= 0 ? null : 1 - opt.expectedCost() / opt.upper();
}
<section class="rounded-box border border-primary/40 bg-base-100 p-3 space-y-3" data-craft-optimize-result>
	<h3 class="text-base font-semibold">${ko ? "최적 조합" : "Optimal combination"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "에센스 · 화석 · 수확 · 제작대(메타 제작 포함) · 짐승 · 화폐를 섞어 상태마다 가장 싼 수단" : "essences, fossils, harvest, bench (incl. meta-crafts), beasts and orbs mixed — the cheapest action in every state"}</span></h3>
	@if(err != null)
		<div role="alert" class="alert alert-warning py-2 text-sm" data-craft-optimize-error>${err}</div>
	@else
		<div class="flex flex-wrap items-baseline gap-x-4 gap-y-1">
			<div><span class="text-xs text-base-content/70">${ko ? "성공 한 번당 기대 비용" : "Expected cost per success"}</span>
				<div class="text-2xl font-bold font-mono" data-craft-optimize-cost="${opt.expectedCost()}">${num.apply(opt.expectedCost())}c@if(opt.stderr() != null)<span class="text-sm font-normal text-base-content/70"> ±${num.apply(opt.stderr() * 1.96)}</span>@endif</div>
			</div>
			@if(opt.upper() != null && opt.upper() < 1e8)
				<div class="text-sm" data-craft-optimize-compare>
					${ko ? "표의 가장 싼 방법" : "Cheapest single method"} (${opt.upperLabel()}): <span class="font-mono">${num.apply(opt.upper())}c</span>
					@if(gain != null && gain > 0.005)
						<span class="badge badge-success badge-sm" data-craft-optimize-gain>${ko ? String.format("%.0f%% 저렴", gain * 100) : String.format("%.0f%% cheaper", gain * 100)}</span>
					@elseif(gain != null)
						<span class="badge badge-ghost badge-sm">${ko ? "표의 방법보다 나은 조합을 못 찾음" : "No better combination found"}</span>
					@endif
				</div>
			@endif
		</div>
		@if(opt.path() != null && !opt.path().isEmpty())
			<div class="space-y-1">
				<div class="text-xs font-medium text-base-content/70">${ko ? "흔한 흐름(성공한 회차 중 비용이 중간인 것 — 같은 수단 연속은 ×n)" : "Typical run (median-cost success — repeats as ×n)"}</div>
				<p class="text-sm leading-relaxed wrap-anywhere" data-craft-optimize-path>${opt.path().length() > 900 ? opt.path().substring(0, opt.path().lastIndexOf(" → ", 900)) + " → …" : opt.path()}</p>
			</div>
		@endif
		@if(!opt.rules().isEmpty())
			<details class="rounded-box border border-base-300" data-craft-optimize-rules open>
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "상태별 결정" : "Decision per state"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "자주 지나는 상태 — 목표 번호는 위 '목표' 순서" : "most visited states — target numbers follow the target list"}</span></summary>
				<div class="px-3 pb-3 overflow-x-auto">
					<table class="table table-xs">
						<thead><tr><th>${ko ? "상태" : "State"}</th><th>${ko ? "쓸 수단" : "Action"}</th><th class="text-right">${ko ? "지나는 회차" : "Visited"}</th><th class="text-right">${ko ? "남은 기대 비용" : "Cost to go"}</th></tr></thead>
						<tbody>
							@for(PoeCraft.OptimizeRule r : opt.rules())
								<tr data-craft-optimize-rule>
									<td class="min-w-[12rem] wrap-anywhere">${r.state()}</td>
									<td class="min-w-[8rem] wrap-anywhere font-medium">${r.action()}</td>
									<td class="text-right font-mono">${String.format("%.0f%%", r.share() * 100)}</td>
									<td class="text-right font-mono">${r.remaining() == null ? "-" : num.apply(r.remaining()) + "c"}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			</details>
		@endif
		@if(!opt.perSuccess().isEmpty())
			<details class="rounded-box border border-base-300" data-craft-optimize-spend>
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "성공 한 번당 쓰는 화폐" : "Currency per success"}</summary>
				<div class="px-3 pb-3 overflow-x-auto">
					<table class="table table-xs">
						<tbody>
							@for(var e : opt.perSuccess().entrySet())
								<tr><td class="wrap-anywhere">${ko ? opt.namesKo().getOrDefault(e.getKey(), e.getKey()) : e.getKey()}</td><td class="text-right font-mono">${num.apply(e.getValue())}</td></tr>
							@endfor
						</tbody>
					</table>
				</div>
			</details>
		@endif
		<details class="text-xs text-base-content/70">
			<summary class="cursor-pointer select-none">${ko ? "어떻게 계산했나" : "How this is computed"}</summary>
			<p class="mt-1">${ko ? "제작 과정을 상태(희귀도 · 맞은 목표 · 쪽마다 남는 잡옵션 수 · 잡옵션이 목표 자리를 막는지 · 제작대 옵션)로 보고, 상태마다 쓸 수 있는 수단의 결과를 같은 제작 엔진으로 표본을 떠 확률을 만든 뒤 상태별 최소 기대 비용(가치 반복)을 구했습니다. 아는 가장 싼 방법(표의 첫 줄)보다 나을 때만 다른 길로 가고, 정한 정책을 실제 아이템에 다시 굴려 비용을 쟀습니다(± = 95% 구간). 짐승 단가는 poe.ninja 짐승 시세(레시피에 필요한 짐승 합)." : "Crafting is modelled as states (rarity, targets met, unwanted modifiers per side, blocked target slots, bench modifiers); each action's outcomes are sampled with the same crafting engine, minimum expected costs per state come from value iteration, alternatives are only taken when cheaper than the best single method, and the resulting policy is replayed on real items (± = 95% interval). Beast recipe prices are the sum of the required beasts on poe.ninja."}</p>
			<p class="mt-1">${opt.note()}</p>
			<p class="mt-1">${ko ? "고려한 수단: " : "Actions considered: "}${String.join(" · ", opt.methods())}</p>
		</details>
	@endif
</section>
