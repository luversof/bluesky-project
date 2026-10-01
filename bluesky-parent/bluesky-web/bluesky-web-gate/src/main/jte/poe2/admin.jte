@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param boolean loggedIn = false

!{ String pageTitle = MessageUtil.getMessage("poe2.admin.title"); }

<%-- PoE2 데이터 관리 — 추출(run-all2.mjs) 실행 + 상태. 실행은 로그인 사용자만(PoE1 관리 화면과 같은 규칙). --%>
@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
			</div>
			<p class="text-sm text-base-content/70 max-w-3xl">${MessageUtil.getMessage("poe2.admin.desc")}</p>
			<div id="poe2Extract" hx-get="/poe2/htmx/admin/status" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10"><span class="loading loading-spinner loading-lg text-primary"></span></div>
			</div>
		</div>
	`
)
