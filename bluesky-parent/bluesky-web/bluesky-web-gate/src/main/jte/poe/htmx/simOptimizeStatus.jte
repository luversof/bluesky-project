@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeOptimizeResult

@param boolean isAuthenticated = false
@param boolean available = false
@param boolean running = false
@param String status = "IDLE"
@param String phase = ""
@param int phaseDone = 0
@param int phaseTotal = 0
@param int evalCount = 0
@param List<String> logLines = java.util.List.of()
@param PoeOptimizeResult result = null
@param java.util.Map<String, String> tattooIcons = null

@if(!available)
	<div class="text-sm text-base-content/50">${MessageUtil.getMessage("poe.sim.unavailable")}</div>
@elseif(running)
	!{ String phaseLabel = MessageUtil.getMessage("poe.sim.opt.phase." + (phase.isEmpty() ? "baseline" : phase)); }
	<div class="space-y-2">
		<div class="flex flex-wrap items-center gap-2 text-sm">
			<span class="loading loading-spinner loading-xs text-primary"></span>
			<span class="font-semibold">${phaseLabel}</span>
			@if(phaseTotal > 0)
				<span class="font-mono text-base-content/60">${phaseDone}/${phaseTotal}</span>
			@endif
			<span class="badge badge-ghost badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.evals"), evalCount)}</span>
		</div>
		@if(phaseTotal > 0)
			<progress class="progress progress-primary w-full" value="${phaseDone}" max="${phaseTotal}"></progress>
		@endif
		@if(!logLines.isEmpty())
			<pre class="max-h-40 overflow-y-auto rounded-box bg-neutral text-neutral-content/80 p-3 text-xs leading-5 whitespace-pre-wrap">@for(String logLine : logLines)${logLine}
@endfor</pre>
		@endif
	</div>
@else
	@if("FAILED".equals(status))
		<div class="alert alert-error text-sm">${MessageUtil.getMessage("poe.admin.extract.status.failed")}</div>
		@if(!logLines.isEmpty())
			<pre class="max-h-40 overflow-y-auto rounded-box bg-neutral text-neutral-content/80 p-3 text-xs leading-5 whitespace-pre-wrap mt-2">@for(String logLine : logLines)${logLine}
@endfor</pre>
		@endif
	@endif
	@template.poe.htmx.simOptimizeResult(result = result, tattooIcons = tattooIcons)
	<%-- 완료 결과가 있으면 최근 결과 목록을 OOB 로 새로 불러온다 — 방금 완료된 실행이 목록에 바로 반영된다. --%>
	@if(result != null)
		<div id="poeOptimizeHistory" class="mt-3" hx-swap-oob="true" hx-get="/poe/htmx/sim/optimize/history" hx-trigger="load" hx-swap="innerHTML"></div>
	@endif
@endif
