@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param String startAscendancy = ""
@param String startSkill = ""
@param boolean startSim = false
@param String startSimId = ""

!{ String pageTitle = MessageUtil.getMessage("poe2.build.title"); }

<%-- PoE2 빌드 — PoB(PoE2) 코드 붙여 넣기 → 요약(htmx). 코드가 커서 POST(form 본문)로 보낸다. --%>
@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			@template.poe2.header(title = pageTitle, meta = meta)
			<p class="text-sm text-base-content/70 max-w-3xl">${MessageUtil.getMessage("poe2.build.desc")}</p>
			<form id="poe2BuildImport" data-no-success-toast class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm"
			      hx-post="/poe2/htmx/build/import" hx-target="#poe2BuildResult" hx-swap="innerHTML">
				<textarea id="poe2BuildCode" name="code" rows="4" class="textarea textarea-bordered w-full font-mono text-xs"
				          placeholder="${MessageUtil.getMessage("poe2.build.placeholder")}" aria-label="${MessageUtil.getMessage("poe2.build.placeholder")}" spellcheck="false"></textarea>
				<button type="submit" class="btn btn-primary btn-sm">${MessageUtil.getMessage("poe2.build.submit")}</button>
			</form>
			<%-- 시뮬레이터 "실빌드에서 출발"(/poe2/build?ascendancy=&skill=) — 대표 실빌드를 서버에서 받아 바로 불러오고 코드 칸도 채운다(OOB) --%>
			@if(startSim)
				<div id="poe2BuildResult" aria-live="polite" data-no-success-toast
				     hx-get="/poe2/htmx/build/sim-result${startSimId.isBlank() ? "" : "?id=" + startSimId}" hx-trigger="load" hx-swap="innerHTML">
					<span class="loading loading-spinner loading-sm"></span>
				</div>
			@elseif(!startSkill.isBlank())
				<div id="poe2BuildResult" aria-live="polite" data-no-success-toast
				     hx-get="/poe2/htmx/build/real-start?ascendancy=${java.net.URLEncoder.encode(startAscendancy, java.nio.charset.StandardCharsets.UTF_8)}&skill=${java.net.URLEncoder.encode(startSkill, java.nio.charset.StandardCharsets.UTF_8)}"
				     hx-trigger="load" hx-swap="innerHTML">
					<span class="loading loading-spinner loading-sm"></span>
				</div>
			@else
				<div id="poe2BuildResult" aria-live="polite"></div>
			@endif
		</div>
	`
)
