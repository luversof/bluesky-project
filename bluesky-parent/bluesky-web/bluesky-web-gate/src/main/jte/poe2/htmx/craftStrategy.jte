@import net.luversof.web.gate.poe2.dto.Poe2Craft
@import net.luversof.web.gate.poe.PoeText

@param Poe2Craft.StrategyResult result = null
@param java.util.List<String> targetLabels = java.util.List.of()
@param java.util.Map<String, String> nameKo = java.util.Map.of()
@param java.util.Map<String, String> argName = java.util.Map.of()
@param java.util.List<String> sampleLinks = java.util.List.of()

<%-- PoE2 다단계 전략 결과(10-15 C355) — 성공률 · 회차 비용 · 성공 한 번당 비용(디바인), 단계 표, 회차당 화폐 · 징조 개수. 가중치 균등 가정. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.Map<String, String> actKo = new java.util.HashMap<>();
	actKo.put("TRANSMUTE", "진화"); actKo.put("AUGMENTATION", "확장"); actKo.put("REGAL", "제왕"); actKo.put("ALCHEMY", "연금술");
	actKo.put("EXALT", "엑잘티드"); actKo.put("CHAOS", "카오스"); actKo.put("ANNUL", "소멸"); actKo.put("FRACTURE", "분열");
	actKo.put("ESSENCE", "에센스"); actKo.put("DESECRATE", "훼손"); actKo.put("REVEAL", "드러내기"); actKo.put("SEQ", "묶음");
	// 영어 단계 이름(10-23 Q31 — 'TRANSMUTE' 같은 내부 코드가 그대로 나왔다): 묶음만 따로, 나머지는 첫 글자만 대문자
	java.util.Map<String, String> actEnMap = java.util.Map.ofEntries(java.util.Map.entry("SEQ", "Group (repeat)"), java.util.Map.entry("TRANSMUTE", "Transmute"), java.util.Map.entry("AUGMENTATION", "Augment"), java.util.Map.entry("EXALT", "Exalted"), java.util.Map.entry("ANNUL", "Annulment"), java.util.Map.entry("SCOUR", "Scouring"), java.util.Map.entry("FRACTURE", "Fracturing"), java.util.Map.entry("CONQUEROR", "Conqueror's Exalted Orb"), java.util.Map.entry("BENCH", "Bench craft"), java.util.Map.entry("VEILED_CHAOS", "Veiled Chaos"));
	java.util.function.Function<String, String> actEn = a -> a == null || a.isEmpty() ? "" : actEnMap.getOrDefault(a, Character.toUpperCase(a.charAt(0)) + a.substring(1).toLowerCase().replace('_', ' '));
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
				<div>${ko ? "성공률 " : "Success "}<span class="font-mono font-semibold" data-craft2-strategy-rate>${String.format("%.2f%%", result.successRate() * 100)}</span> <span class="text-xs text-base-content/70">(${result.successes()}/${result.runs()})</span>@if(result.successes() > 0 && result.successes() < 20)<span class="text-xs text-warning" data-craft2-strategy-few> ${ko ? "· 성공 " + result.successes() + "번뿐이라 오차가 큽니다" : "· only " + result.successes() + " successes — large error"}</span>@endif</div>
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
								<td>${ko ? actKo.getOrDefault(s.action(), s.action()) : actEn.apply(s.action())}@if(s.arg() != null) · <span class="text-xs">${argName.getOrDefault(s.arg(), s.arg())}</span>@endif</td>
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
			<p class="text-xs text-base-content/70">${ko ? "회차 단위 모의(화폐를 못 쓰면 그 회차 중단 · 목표를 다 채우면 남은 단계는 쓰지 않음) · 가중치 균등 가정(PoE2 비공개) · 베이스 값 = '가격 직접 입력'의 베이스 아이템(비우면 제외)" : "Per-run simulation (a run stops when a currency cannot be used · later steps are skipped once all targets are met) · uniform weights (PoE2 hidden) · base cost = 'Base item' in Custom prices (blank = excluded)"}</p>
			<%-- 성공 예시(10-22 Q17, PoE1 craftStrategy 짝) — 목표를 채운 처음 몇 개, 줄은 접두 → 접미 · 범위 문구 그대로 --%>
			@if(result.samples() != null && !result.samples().isEmpty())
				<details class="rounded-box border border-base-300" data-craft2-strategy-samples>
					<summary class="cursor-pointer select-none px-3 py-1.5 text-sm font-medium">${ko ? "성공 예시" : "Successful items"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "모의에서 목표를 채운 처음 " + result.samples().size() + "개" : "first " + result.samples().size() + " that met the targets"}</span></summary>
					<ol class="grid gap-2 px-3 pb-3 sm:grid-cols-2">
						@for(int k = 0; k < result.samples().size(); k++)
							!{
								Poe2Craft.StrategySample sm = result.samples().get(k);
								java.util.List<Poe2Craft.Line> ls = new java.util.ArrayList<>(sm.lines() == null ? java.util.List.of() : sm.lines());
								ls.sort(java.util.Comparator.comparing(l -> "prefix".equals(l.gen()) ? 0 : 1));
							}
							<li class="min-w-0 space-y-0.5 rounded-box border border-base-300 px-2 py-1.5 text-xs" data-craft2-strategy-sample>
								<div class="flex items-center justify-between gap-2">
									<span class="font-medium">${ko ? "예시 " : "Item "}${k + 1}</span>
									@if(k < sampleLinks.size())<a class="link link-primary" href="${sampleLinks.get(k)}" target="_blank" rel="noopener" data-craft2-strategy-sample-open>${ko ? "제작 화면에서 열기" : "Open in the crafting bench"}</a>@endif
								</div>
								@for(Poe2Craft.Line l : ls)
									!{ java.util.List<String> lx = ko && l.ko() != null && !l.ko().isEmpty() ? l.ko() : (l.en() == null ? java.util.List.of() : l.en()); }
									<div class="flex min-w-0 items-baseline gap-1" data-craft2-strategy-sample-line="${l.id()}">
										<span class="shrink-0 text-base-content/60">${"prefix".equals(l.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}@if(l.tierRank() != null) T${l.tierRank()}@endif</span>
										<span class="min-w-0 wrap-anywhere">$unsafe{PoeText.highlightValues(String.join(", ", lx))}@if(l.fractured())<span class="text-base-content/60"> (${ko ? "분열" : "fractured"})</span>@endif</span>
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
