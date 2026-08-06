@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeJobStatus

@param List<PoeJobStatus.OptimizeHistoryEntry> history = java.util.List.of()
<%-- 세션 만료로 삭제가 수행되지 않았을 때 true — 성공처럼 보이는 무음 실패를 막는 안내 배너 --%>
@param boolean loginRequired = false

@if(loginRequired)
	<div class="alert alert-warning py-1.5 px-3 text-xs mb-1.5">
		${net.luversof.web.gate.poe.PoeText.isKorean() ? "로그인이 풀려 삭제되지 않았습니다 — 다시 로그인 후 시도해 주세요." : "Session expired — the result was not deleted. Please sign in again."}
	</div>
@endif

<%-- 최근 결과 목록 — 항목 클릭 시 그 결과를 결과 fragment 로 되살려 **폴링과 분리된 지난-결과 뷰어**(#poeHistoryResultView)에 표시.
     상태 래퍼(#poeOptimizeStatusWrap)에 넣으면 실행 중 3초 폴링이 로딩화면으로 덮어써 사라졌다 — 그래서 별도 뷰어로 분리. --%>
@if(!history.isEmpty())
	<div class="space-y-1.5">
		<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.sim.opt.history")}</div>
		<div class="flex flex-col gap-1">
			@for(PoeJobStatus.OptimizeHistoryEntry h : history)
				!{
					String metric = "ehp".equals(h.objective())
							? MessageUtil.getMessage("poe.sim.opt.objective.ehp")
							: "balanced".equals(h.objective())
									? MessageUtil.getMessage("poe.sim.opt.objective.balanced")
									: MessageUtil.getMessage("poe.sim.opt.objective.dps");
					String when = new java.text.SimpleDateFormat("MM-dd HH:mm").format(new java.util.Date(h.id()));
				}
				<div class="flex items-stretch gap-1">
				<button type="button"
				        class="flex flex-1 flex-wrap items-center gap-2 rounded border border-base-300 bg-base-100 px-3 py-1.5 text-left text-xs hover:border-primary hover:bg-primary/5"
				        hx-get="/poe/htmx/sim/optimize/result?id=${h.id()}"
				        hx-target="#poeHistoryResultView"
				        hx-swap="innerHTML"
				        onclick="this.closest('#poeOptimizeHistory').querySelectorAll('button.poe-hist-active').forEach(function(b){b.classList.remove('poe-hist-active');}); this.classList.add('poe-hist-active');">
					<span class="font-semibold">${net.luversof.web.gate.poe.PoeText.name(h.gemNameKo(), h.gemName())}</span>
					@if(h.classNameKo() != null && !h.classNameKo().isBlank())
						<span class="text-base-content/60">${net.luversof.web.gate.poe.PoeText.name(h.classNameKo(), h.className())}</span>
					@endif
					@if(h.ascendancy() != null && !h.ascendancy().isBlank())
						<span class="badge badge-ghost badge-sm">${net.luversof.web.gate.poe.PoeText.name(h.ascendancyKo(), h.ascendancy())}</span>
					@endif
					<span class="badge badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(h.scenarioKo(), h.scenario())}</span>
					<span class="badge badge-ghost badge-sm">${metric}</span>
					@if(h.combatBuffs())
						<span class="badge badge-ghost badge-sm">${MessageUtil.getMessage("poe.sim.opt.buffs")}</span>
					@endif
					<span class="font-mono font-semibold text-primary">${h.finalValue()}</span>
					<span class="ml-auto font-mono text-base-content/40">${when}</span>
				</button>
				<%-- 이력 삭제(사용자 요청) — 삭제 후 갱신된 목록 fragment 로 #poeOptimizeHistory 교체. 버튼 중첩 불가라 행 밖에 둔다 --%>
				<button type="button"
				        class="btn btn-ghost btn-xs px-2 text-base-content/40 hover:text-error shrink-0"
				        title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "이 결과 삭제" : "Delete this result"}"
				        hx-delete="/poe/htmx/sim/optimize/history/${h.id()}"
				        hx-target="#poeOptimizeHistory"
				        hx-swap="innerHTML"
				        hx-confirm="${net.luversof.web.gate.poe.PoeText.isKorean() ? "이 최근 결과를 삭제할까요?" : "Delete this saved result?"}">✕</button>
				</div>
			@endfor
		</div>
	</div>
@endif
