@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param boolean isAuthenticated = false
@param boolean available = false
@param boolean running = false
@param String status = "IDLE"
@param List<String> logLines = java.util.List.of()
@param String latestPatch = null
@param boolean outdated = false
@param String dataPatch = null
@param String configPatch = null
@param String pobVersion = null

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
			<%-- 버전 비교 — 갱신 버튼 바로 옆. 최신이어도 "검사 결과"를 보여야 사용자가 확인 여부를 안다. --%>
			<div class="flex flex-wrap items-center gap-2 text-xs">
				<span class="text-base-content/50">${MessageUtil.getMessage("poe.admin.version.current")}</span>
				<span class="font-mono font-semibold">${dataPatch == null ? "-" : dataPatch}</span>
				<span class="text-base-content/30">→</span>
				<span class="text-base-content/50">${MessageUtil.getMessage("poe.admin.version.latest")}</span>
				<span class="font-mono font-semibold ${outdated ? "text-warning" : ""}">${latestPatch == null ? "?" : latestPatch}</span>
				@if(latestPatch == null)
					<span class="badge badge-ghost badge-sm">${MessageUtil.getMessage("poe.admin.version.unknown")}</span>
				@elseif(outdated)
					<span class="badge badge-warning badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.admin.version.outdated"), latestPatch)}</span>
				@else
					<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.version.uptodate")}</span>
				@endif
				@if(configPatch != null && dataPatch != null && !configPatch.equals(dataPatch))
					<span class="badge badge-info badge-outline badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.admin.version.config"), configPatch)}</span>
				@endif
				@if(pobVersion != null)
					<span class="badge badge-ghost badge-sm" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "PoB 계산 엔진 버전(시뮬 트리 기준)" : "PoB calc engine version (sim tree basis)"}">PoB ${pobVersion}</span>
				@endif
			</div>
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
				<pre id="poeExtractLog" class="max-h-72 overflow-y-auto rounded-box bg-neutral text-neutral-content/80 p-3 text-xs leading-5 whitespace-pre-wrap">@for(String logLine : logLines)${logLine}
@endfor</pre>
				<%-- 폴링 스왑마다 pre 가 새로 생겨 스크롤이 최상단으로 리셋(사용자 지적) — tail 추적:
				     기본은 맨 아래(새 라인)를 따라가고, 사용자가 위로 올려 읽는 중이면 그 위치를 유지, 다시 바닥 근처로 내리면 추적 재개 --%>
				<script nonce="${CspNonceHolder.getNonce()}">
					(function () {
						var el = document.getElementById("poeExtractLog");
						if (!el) return;
						var s = window.__poeExtractLogScroll;
						if (s && !s.atBottom) {
							el.scrollTop = s.top;
						} else {
							el.scrollTop = el.scrollHeight;
						}
						el.addEventListener("scroll", function () {
							window.__poeExtractLogScroll = {
								top: el.scrollTop,
								atBottom: el.scrollHeight - el.scrollTop - el.clientHeight < 24
							};
						});
					})();
				</script>
			@endif
	`)
</div>
