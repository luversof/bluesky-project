@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param boolean available = false
@param boolean running = false
@param String status = "IDLE"
@param List<String> logLines = java.util.List.of()
@param String latestPatch = null
@param boolean outdated = false

!{
	// 최신과 다르면 버튼에 목표 버전 표기(원클릭 갱신), 같으면 재추출
	String runLabel = outdated
		? java.text.MessageFormat.format(MessageUtil.getMessage("poe.admin.extract.run.latest"), latestPatch)
		: MessageUtil.getMessage("poe.admin.extract.run");
	String runningLabel = MessageUtil.getMessage("poe.admin.extract.running");
	String descriptionLabel = MessageUtil.getMessage("poe.admin.extract.desc");
	String unavailableLabel = MessageUtil.getMessage("poe.admin.extract.unavailable");
	String loginRequiredLabel = MessageUtil.getMessage("stock.label.login.required");
	String statusSuccessLabel = MessageUtil.getMessage("poe.admin.extract.status.success");
	String statusFailedLabel = MessageUtil.getMessage("poe.admin.extract.status.failed");
}

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
						<span class="loading loading-spinner loading-xs"></span> ${runningLabel}
					</button>
				@else
					<button type="button" class="btn btn-sm ${outdated ? "btn-warning" : "btn-primary"}"
					        hx-post="/poe/htmx/admin/extract"
					        hx-target="#poeExtractStatusWrap"
					        hx-swap="outerHTML">${runLabel}</button>
				@endif
				@if("SUCCESS".equals(status) && !running)
					<span class="badge badge-success badge-outline">${statusSuccessLabel}</span>
				@elseif("FAILED".equals(status) && !running)
					<span class="badge badge-error badge-outline">${statusFailedLabel}</span>
				@endif
			</div>
			@if(!logLines.isEmpty())
				<pre class="max-h-72 overflow-y-auto rounded-box bg-neutral text-neutral-content/80 p-3 text-xs leading-5 whitespace-pre-wrap">@for(String logLine : logLines)${logLine}
@endfor</pre>
			@endif
	`)
</div>
