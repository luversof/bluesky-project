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
@param net.luversof.web.gate.poe.dto.ArchetypeBenchmark ninjaBenchmark = null
@param net.luversof.web.gate.poe.dto.RealStart realStart = null
@param java.util.Map<String, net.luversof.web.gate.poe.dto.PoeBaseItem> rareBases = null
<%-- 삿된 · 변형 고유의 실제 장착 옵션 사본(슬롯별) + 그 베이스 — 일반 상세(hx-get)는 원래 옵션을 띄워서(10-05 C152) --%>
@param java.util.Map<String, net.luversof.web.gate.poe.dto.PoeUniqueItem> uniqueOverrides = null
@param java.util.Map<String, net.luversof.web.gate.poe.dto.PoeBaseItem> uniqueOverrideBases = null
<%-- 감시자의 눈 · 금단 짝 결과 주얼의 고른 옵션 사본(키 = PoeHtmxController.jewelOverrideKey) + 베이스(10-05 C154) --%>
@param java.util.Map<String, net.luversof.web.gate.poe.dto.PoeUniqueItem> jewelOverrides = null
@param java.util.Map<String, net.luversof.web.gate.poe.dto.PoeBaseItem> jewelOverrideBases = null

@if(!available)
	<div class="text-sm text-base-content/60">${MessageUtil.getMessage("poe.sim.unavailable")}</div>
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
			@if(isAuthenticated)
				<%-- 중지 버튼 — 잘못된 조건으로 시작한 실행을 취소. 다음 폴링(≤3초)에서 중지됨 상태로 갱신된다. --%>
				<button type="button" class="btn btn-error btn-xs ml-auto"
				        hx-post="/poe/htmx/sim/optimize/stop" hx-swap="none"
				        onclick="this.disabled=true;this.textContent='${MessageUtil.getMessage("poe.sim.opt.stopping")}'">${MessageUtil.getMessage("poe.sim.opt.stop")}</button>
			@endif
		</div>
		@if(phaseTotal > 0)
			<progress class="progress progress-primary w-full" value="${phaseDone}" max="${phaseTotal}"></progress>
		@endif
		@if(!logLines.isEmpty())
			<%-- 진행 로그는 한국어로만 쓴다(최적화기 로그 문구) — lang 으로 언어를 밝힌다(WCAG 3.1.2, 10-03) --%><pre lang="ko" class="max-h-40 overflow-y-auto rounded-box bg-neutral text-neutral-content/80 p-3 text-xs leading-5 whitespace-pre-wrap">@for(String logLine : logLines)${logLine}
@endfor</pre>
		@endif
	</div>
@else
	@if("FAILED".equals(status))
		<div class="alert alert-error text-sm">${MessageUtil.getMessage("poe.admin.extract.status.failed")}</div>
		@if(!logLines.isEmpty())
			<%-- 진행 로그는 한국어로만 쓴다(최적화기 로그 문구) — lang 으로 언어를 밝힌다(WCAG 3.1.2, 10-03) --%><pre lang="ko" class="max-h-40 overflow-y-auto rounded-box bg-neutral text-neutral-content/80 p-3 text-xs leading-5 whitespace-pre-wrap mt-2">@for(String logLine : logLines)${logLine}
@endfor</pre>
		@endif
	@elseif("CANCELLED".equals(status))
		<div class="alert alert-warning text-sm">${MessageUtil.getMessage("poe.sim.opt.cancelled")}</div>
	@endif
	@template.poe.htmx.simOptimizeResult(result = result, tattooIcons = tattooIcons, ninjaBenchmark = ninjaBenchmark, rareBases = rareBases, realStart = realStart, uniqueOverrides = uniqueOverrides, uniqueOverrideBases = uniqueOverrideBases, jewelOverrides = jewelOverrides, jewelOverrideBases = jewelOverrideBases, historyId = null)
	<%-- 완료 결과가 있으면 최근 결과 목록을 OOB 로 새로 불러온다 — 방금 완료된 실행이 목록에 바로 반영된다. --%>
	@if(result != null)
		<div id="poeOptimizeHistory" class="mt-3" hx-swap-oob="true" hx-get="/poe/htmx/sim/optimize/history" hx-trigger="load" hx-swap="innerHTML"></div>
	@endif
@endif
