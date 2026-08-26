@import net.luversof.web.gate.poe.PoeText

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
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

@template._layout.poeLayout(
	title = ko ? "자율 개선 현황" : "Improvement loop status",
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${ko ? "자율 개선 현황" : "Improvement loop status"}</h1>
				<span class="text-sm text-base-content/50">${ko ? "5초마다 스스로 갱신합니다 — 진행이 멈추면 숫자가 안 올라갑니다" : "Refreshes itself every 5s"}</span>
			</div>

			<%-- 패널만 갈아끼운다: 폴링은 조각(/poe/htmx/loop)이 담당해 화면 전체가 깜빡이지 않게 --%>
			<div id="poeLoopPanel"
			     hx-get="/poe/htmx/loop"
			     hx-trigger="every 5s"
			     hx-target="this"
			     hx-swap="innerHTML">
				@template.poe.htmx.loopPanel(
					stamp = stamp, currentTask = currentTask, jobKnown = jobKnown, jobRunning = jobRunning,
					jobStatus = jobStatus, jobPhase = jobPhase, jobEval = jobEval, jobLog = jobLog,
					batteryDone = batteryDone, batteryTotal = batteryTotal, batteryFail = batteryFail,
					batteryTail = batteryTail, queue = queue, doneCount = doneCount, recentDone = recentDone)
			</div>
		</div>
	`)
