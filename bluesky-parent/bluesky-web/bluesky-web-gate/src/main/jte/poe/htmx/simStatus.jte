@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param boolean available = false
@param boolean running = false
@param String status = "IDLE"
@param int progressDone = 0
@param int progressTotal = 0
@param List<String> logLines = java.util.List.of()

!{
	String runLabel = MessageUtil.getMessage("poe.sim.run");
	String runningLabel = MessageUtil.getMessage("poe.sim.running");
	String descriptionLabel = MessageUtil.getMessage("poe.sim.desc");
	String unavailableLabel = MessageUtil.getMessage("poe.sim.unavailable");
	String loginRequiredLabel = MessageUtil.getMessage("stock.label.login.required");
	String statusSuccessLabel = MessageUtil.getMessage("poe.admin.extract.status.success");
	String statusFailedLabel = MessageUtil.getMessage("poe.admin.extract.status.failed");
}

<%-- 유휴/완료 상태(286, 마지막 응답)에서만 랭킹 목록을 (재)로드한다 --%>
@if(!running)
	<div class="hidden" hx-get="/poe/htmx/sim/ranking" hx-trigger="load" hx-target="#poeSimRanking" hx-swap="innerHTML"></div>
@endif
<div>
	@template._components.ui.card(bodyClass = "gap-3", body = @`
			<div class="flex flex-wrap items-center gap-3">
				<div class="text-sm leading-6 text-base-content/60 min-w-0 flex-1">${descriptionLabel}</div>
				@if(!available)
					<span class="badge badge-ghost">${unavailableLabel}</span>
				@elseif(!isAuthenticated)
					<span class="badge badge-ghost">${loginRequiredLabel}</span>
				@elseif(running)
					<button type="button" class="btn btn-primary btn-sm" disabled>
						<span class="loading loading-spinner loading-xs"></span> ${runningLabel} ${progressDone}/${progressTotal}
					</button>
				@else
					<button type="button" class="btn btn-primary btn-sm"
					        hx-post="/poe/htmx/sim/run"
					        hx-target="#poeSimStatusWrap"
					        hx-swap="outerHTML">${runLabel}</button>
				@endif
				@if("SUCCESS".equals(status) && !running)
					<span class="badge badge-success badge-outline">${statusSuccessLabel}</span>
				@elseif("FAILED".equals(status) && !running)
					<span class="badge badge-error badge-outline">${statusFailedLabel}</span>
				@endif
			</div>
			@if(running && progressTotal > 0)
				<progress class="progress progress-primary w-full" value="${progressDone}" max="${progressTotal}"></progress>
			@endif
			@if(!logLines.isEmpty() && (running || "FAILED".equals(status)))
				<pre class="max-h-52 overflow-y-auto rounded-box bg-neutral text-neutral-content/80 p-3 text-xs leading-5 whitespace-pre-wrap">@for(String logLine : logLines)${logLine}
@endfor</pre>
			@endif
	`)
</div>
