@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.ExtractStatus status
@param Poe2.Meta meta = null
@param boolean loggedIn = false

!{
	boolean running = Boolean.TRUE.equals(status.running());
	java.util.List<String> log = status.log() == null ? java.util.List.of() : status.log();
	// 로그는 마지막 40줄만(전체는 API 상태에 200줄)
	java.util.List<String> tail = log.size() > 40 ? log.subList(log.size() - 40, log.size()) : log;
}

<%-- 추출 상태 조각 — 도는 동안 2초마다 스스로 다시 불러온다(끝나면 멈춤). --%>
<%-- JTE 는 값이 null 인 속성을 빼고 그린다 — 끝난 뒤엔 폴링 속성 자체가 없다 --%>
<div class="space-y-3" hx-get="${running ? "/poe2/htmx/admin/status" : null}" hx-trigger="${running ? "every 2s" : null}"
     hx-target="${running ? "#poe2Extract" : null}" hx-swap="${running ? "innerHTML" : null}">
	@if(meta != null)
		<div class="rounded-2xl bg-base-100 p-3 shadow-sm text-sm space-y-1">
			@if(meta.patch() != null && !meta.patch().isBlank())
				<div><span class="badge badge-ghost">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.patch"), meta.patch())}</span></div>
			@endif
			<div>${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.admin.counts"), meta.gems(), meta.baseItems(), meta.modPools(), meta.augments(), meta.uniques())}</div>
			@if(meta.loadedAt() != null && !meta.loadedAt().isBlank())
				<div class="text-xs text-base-content/50">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.admin.loaded"), meta.loadedAt())}</div>
			@endif
		</div>
	@endif
	<div class="flex flex-wrap items-center gap-3">
		@if(loggedIn)
			<button type="button" class="btn btn-primary btn-sm" hx-post="/poe2/htmx/admin/extract" hx-target="#poe2Extract" hx-swap="innerHTML" disabled="${running}">${MessageUtil.getMessage("poe2.admin.run")}</button>
		@else
			<span class="text-sm text-base-content/60">${MessageUtil.getMessage("poe2.admin.login")}</span>
		@endif
		@if(running)
			<span class="flex items-center gap-2 text-sm"><span class="loading loading-spinner loading-xs text-primary"></span>${MessageUtil.getMessage("poe2.admin.running")}</span>
		@elseif(status.exitCode() != null)
			<span class="text-sm ${status.exitCode() == 0 ? "text-success" : "text-error"}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.admin.done"), status.exitCode())}</span>
		@else
			<span class="text-sm text-base-content/50">${MessageUtil.getMessage("poe2.admin.never")}</span>
		@endif
	</div>
	@if(!tail.isEmpty())
		<pre class="rounded-xl bg-base-300/60 p-3 text-xs leading-relaxed overflow-x-auto max-h-96">@for(String line : tail)${line}
@endfor</pre>
	@endif
</div>
