@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeJobStatus

@param List<PoeJobStatus.OptimizeHistoryEntry> history = java.util.List.of()

<%-- 최근 결과 목록 — 항목 클릭 시 그 결과를 결과 fragment 로 되살려 상태 래퍼에 그대로 복원(실시간 결과와 동일 DOM). --%>
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
				<button type="button"
				        class="flex flex-wrap items-center gap-2 rounded border border-base-300 bg-base-100 px-3 py-1.5 text-left text-xs hover:border-primary hover:bg-primary/5"
				        hx-get="/poe/htmx/sim/optimize/result?id=${h.id()}"
				        hx-target="#poeOptimizeStatusWrap"
				        hx-swap="innerHTML">
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
			@endfor
		</div>
	</div>
@endif
