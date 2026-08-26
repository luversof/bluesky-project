@import net.luversof.web.gate.poe.PoeText

@param String stamp = ""
@param java.util.List<String> currentTask = java.util.List.of()
@param boolean jobKnown = false
@param boolean jobRunning = false
@param String jobStatus = ""
@param String jobPhase = ""
@param int jobEval = 0
@param java.util.List<String> jobLog = java.util.List.of()
@param int batteryDone = 0
@param int batteryTotal = 0
@param long batteryFail = 0
@param java.util.List<String> batteryTail = java.util.List.of()
@param java.util.List<String> queue = java.util.List.of()
@param long doneCount = 0
@param java.util.List<String> recentDone = java.util.List.of()

!{ boolean ko = PoeText.isKorean(); }

<div class="space-y-3">
	<div class="text-xs text-base-content/40">${ko ? "갱신" : "updated"} ${stamp}</div>

	<%-- 지금 하는 일 — 사이클이 mark-status.sh 로 남긴 한 줄 --%>
	<div class="rounded-2xl bg-base-100 p-4 shadow-md">
		<div class="text-xs font-semibold text-base-content/50">${ko ? "현재 작업" : "Current task"}</div>
		@if(currentTask.isEmpty())
			<div class="text-sm text-base-content/40">${ko ? "(기록 없음)" : "(none)"}</div>
		@else
			@for(String line : currentTask)
				<div class="text-sm">${line}</div>
			@endfor
		@endif
	</div>

	<div class="grid gap-3 md:grid-cols-2">
		<%-- 최적화 잡 — API 가 진실 --%>
		<div class="rounded-2xl bg-base-100 p-4 shadow-md space-y-2">
			<div class="flex items-center gap-2">
				<span class="text-xs font-semibold text-base-content/50">${ko ? "최적화 잡" : "Optimizer job"}</span>
				@if(!jobKnown)
					<span class="badge badge-sm badge-ghost">${ko ? "확인 불가(API 무응답)" : "unknown"}</span>
				@elseif(jobRunning)
					<span class="badge badge-sm badge-primary">${ko ? "실행 중" : "running"}</span>
				@else
					<span class="badge badge-sm badge-ghost">${ko ? "유휴" : "idle"}</span>
				@endif
			</div>
			@if(jobKnown)
				<div class="text-sm">
					${ko ? "단계" : "phase"} <span class="font-mono">${jobPhase == null || jobPhase.isBlank() ? "-" : jobPhase}</span>
					· ${ko ? "평가" : "evals"} <span class="font-mono">${jobEval}</span>
					· ${ko ? "마지막" : "last"} <span class="font-mono">${jobStatus}</span>
				</div>
				@if(!jobLog.isEmpty())
					<pre class="max-h-40 overflow-auto rounded bg-base-200 p-2 text-[11px] leading-snug">@for(String l : jobLog)${l}
@endfor</pre>
				@endif
			@endif
		</div>

		<%-- 배터리 재실행 진행 --%>
		<div class="rounded-2xl bg-base-100 p-4 shadow-md space-y-2">
			<div class="text-xs font-semibold text-base-content/50">${ko ? "배터리 재실행" : "Battery re-run"}</div>
			@if(batteryTotal > 0)
				<div class="flex items-center gap-2">
					<progress class="progress progress-primary w-40" value="${batteryDone}" max="${batteryTotal}"></progress>
					<span class="font-mono text-sm">${batteryDone}/${batteryTotal}</span>
					@if(batteryFail > 0)
						<span class="badge badge-sm badge-error">${ko ? "실패 " + batteryFail : batteryFail + " failed"}</span>
					@endif
				</div>
			@else
				<div class="text-sm text-base-content/40">${ko ? "(진행 중인 재실행 없음)" : "(no run)"}</div>
			@endif
			@if(!batteryTail.isEmpty())
				<pre class="max-h-40 overflow-auto rounded bg-base-200 p-2 text-[11px] leading-snug">@for(String l : batteryTail)${l}
@endfor</pre>
			@endif
		</div>
	</div>

	<%-- 백로그 --%>
	<div class="rounded-2xl bg-base-100 p-4 shadow-md space-y-2">
		<div class="flex items-baseline gap-2">
			<span class="text-xs font-semibold text-base-content/50">${ko ? "백로그" : "Backlog"}</span>
			<span class="text-xs text-base-content/40">${ko ? "완료 누적 " + doneCount + "건" : doneCount + " done"}</span>
		</div>
		@if(queue.isEmpty())
			<div class="text-sm text-base-content/40">${ko ? "대기 항목 없음" : "queue empty"}</div>
		@else
			@for(String line : queue)
				<div class="text-sm">${line}</div>
			@endfor
		@endif
		@if(!recentDone.isEmpty())
			<div class="pt-1 text-xs font-semibold text-base-content/50">${ko ? "최근 완료" : "Recently done"}</div>
			@for(String line : recentDone)
				<div class="text-xs text-base-content/60">${line}</div>
			@endfor
		@endif
	</div>
</div>
