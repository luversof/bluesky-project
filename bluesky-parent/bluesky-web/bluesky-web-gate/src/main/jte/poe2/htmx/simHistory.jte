@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param List<Poe2.SimHistoryEntry> history = java.util.List.of()
<%-- 세션 만료로 삭제가 수행되지 않았을 때 true — 성공처럼 보이는 무음 실패를 막는 안내 배너(PoE1 과 같다) --%>
@param boolean loginRequired = false

<%-- PoE2 시뮬 최근 결과 목록 — PoE1 poe/htmx/simOptimizeHistory.jte 의 마크업을 그대로 옮겼다(10-01 "PoE1 에 맞춰").
     항목 클릭 → 폴링과 분리된 지난-결과 뷰어(#poe2HistoryResultView)에 결과 조각(simResult). PoE1 과 다른 점: 결과값 자리에
     "출발 → 다듬은 뒤" 의 목표 축 수치를 보인다(PoE2 시뮬은 실빌드에서 출발해 다듬으므로). --%>
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
}
@if(loginRequired)
	<div class="alert alert-warning py-1.5 px-3 text-xs mb-1.5">
		${ko ? "로그인이 풀려 삭제되지 않았습니다 — 다시 로그인 후 시도해 주세요." : "Session expired — the result was not deleted. Please sign in again."}
	</div>
@endif
@if(!history.isEmpty())
	<div class="space-y-1.5" data-sim-history>
		<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.sim.opt.history")}</div>
		<div class="flex flex-col gap-1">
			@for(Poe2.SimHistoryEntry h : history)
				!{
					String metric = "ehp".equals(h.objective())
							? MessageUtil.getMessage("poe.sim.opt.objective.ehp")
							: "balanced".equals(h.objective())
									? MessageUtil.getMessage("poe.sim.opt.objective.balanced")
									: MessageUtil.getMessage("poe.sim.opt.objective.dps");
					boolean ehpAxis = "ehp".equals(h.objective());
					String finalValue = ehpAxis
							? fmt.apply(h.startEhp()) + " → " + fmt.apply(h.refinedEhp()) + " EHP"
							: fmt.apply(h.startDps()) + " → " + fmt.apply(h.refinedDps()) + " DPS";
					String scen = "None".equals(h.scenario()) ? MessageUtil.getMessage("poe.sim.opt.scenario.none")
							: "Boss".equals(h.scenario()) ? MessageUtil.getMessage("poe.sim.opt.scenario.boss")
							: "Uber".equals(h.scenario()) ? MessageUtil.getMessage("poe.sim.opt.scenario.uber")
							: "keep".equals(h.scenario()) ? (ko ? "플레이어 설정 그대로" : "Player setting")
							: MessageUtil.getMessage("poe.sim.opt.scenario.pinnacle");
					String when = new java.text.SimpleDateFormat("MM-dd HH:mm").format(new java.util.Date(h.id()));
				}
				<div class="flex items-stretch gap-1">
				<button type="button"
				        class="flex min-w-0 flex-1 flex-wrap items-center gap-2 rounded border border-base-300 bg-base-100 px-3 py-1.5 text-left text-xs hover:border-primary hover:bg-primary/5"
				        hx-get="/poe2/htmx/sim/result?id=${h.id()}"
				        hx-target="#poe2HistoryResultView"
				        hx-swap="innerHTML"
				        onclick="this.closest('#poe2SimHistory').querySelectorAll('button.poe-hist-active').forEach(function(b){b.classList.remove('poe-hist-active');}); this.classList.add('poe-hist-active');">
					<span class="font-semibold">${PoeText.name(h.skillKo(), h.skill())}</span>
					@if(h.ascendancy() != null && !h.ascendancy().isBlank())
						<span class="badge badge-ghost badge-sm">${PoeText.name(h.ascendancyKo(), h.ascendancy())}</span>
					@endif
					<span class="badge badge-outline badge-sm">${scen}</span>
					<span class="badge badge-ghost badge-sm">${metric}</span>
					<span class="font-mono font-semibold text-primary">${finalValue}</span>
					<span class="ml-auto font-mono text-base-content/60">${when}</span>
				</button>
				<%-- 이력 삭제 — 삭제 후 갱신된 목록으로 #poe2SimHistory 교체. 버튼 중첩 불가라 행 밖에 둔다(PoE1 과 같다) --%>
				<button type="button"
				        class="btn btn-ghost btn-xs px-2 text-base-content/60 hover:text-error shrink-0 self-start"
				        title="${ko ? "이 결과 삭제" : "Delete this result"}"
				        hx-delete="/poe2/htmx/sim/history/${h.id()}"
				        hx-target="#poe2SimHistory"
				        hx-swap="innerHTML"
				        hx-confirm="${ko ? "이 최근 결과를 삭제할까요?" : "Delete this saved result?"}">✕</button>
				</div>
			@endfor
		</div>
	</div>
@endif
