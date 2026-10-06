@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param String startAscendancy = ""
@param String startSkill = ""

!{
	String pageTitle = MessageUtil.getMessage("poe.build.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String descriptionLabel = MessageUtil.getMessage("poe.build.desc");
	String codePlaceholder = MessageUtil.getMessage("poe.build.code.placeholder");
	String importLabel = MessageUtil.getMessage("poe.build.import");
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
				<span class="badge badge-ghost">${patchLabel}</span>
			</div>

			<%-- 코드 해석 · 재계산 · 가이드는 조회라 "처리되었습니다" 성공 토스트를 내지 않는다(PoE2 와 같음, 10-02 C34: 해석 실패 화면에 성공 토스트가 같이 떴다).
			     결과 칸은 낭독기 알림(aria-live) — 실패 문구가 들린다 --%>
			<form id="poeBuildImport" data-no-success-toast class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm"
			      hx-post="/poe/htmx/build/import"
			      hx-target="#poeBuildSummary"
			      hx-swap="innerHTML">
				<div class="text-sm text-base-content/60">${descriptionLabel}</div>
				<textarea id="poeBuildCode" name="code" rows="4" required
				          class="textarea textarea-bordered w-full font-mono text-xs leading-5"
				          placeholder="${codePlaceholder}" autocomplete="off" spellcheck="false" aria-label="${codePlaceholder}"></textarea>
				<%-- 루미너리 용병 — PoB 엔진엔 용병이 없어, 용병을 따로 만든 PoB 빌드로 계산한 오라·저주를 파티 탭으로 넣는다(API PoeMercenaryService).
				     재계산·업그레이드 가이드 버튼이 hx-include="#poeBuildImport textarea" 로 이 칸도 함께 보낸다. --%>
				<details class="rounded-box bg-base-200/40 px-3 py-2">
					<summary class="cursor-pointer text-sm font-semibold">${MessageUtil.getMessage("poe.build.merc.title")}</summary>
					<div class="mt-2 space-y-1">
						<div class="text-xs text-base-content/60">${MessageUtil.getMessage("poe.build.merc.desc")}</div>
						<textarea name="mercCode" rows="2"
						          class="textarea textarea-bordered w-full font-mono text-xs leading-5"
						          placeholder="${MessageUtil.getMessage("poe.build.merc.placeholder")}" autocomplete="off" spellcheck="false" aria-label="${MessageUtil.getMessage("poe.build.merc.placeholder")}"></textarea>
					</div>
				</details>
				<div class="flex justify-end">
					<button type="submit" class="btn btn-primary btn-sm">${importLabel}</button>
				</div>
			</form>

			<%-- 시뮬레이터 "실빌드에서 출발" 바로가기(/poe/build?ascendancy=&skill=) — 대표 실빌드 코드를 서버에서 받아 바로 불러오고,
			     코드 칸도 채운다(OOB). 재계산·업그레이드 가이드 버튼이 그 칸을 보내므로 이어서 쓸 수 있다. --%>
			@if(!startSkill.isBlank())
				<div id="poeBuildSummary" aria-live="polite" data-no-success-toast
				     hx-get="/poe/htmx/build/real-start?ascendancy=${java.net.URLEncoder.encode(startAscendancy, java.nio.charset.StandardCharsets.UTF_8)}&skill=${java.net.URLEncoder.encode(startSkill, java.nio.charset.StandardCharsets.UTF_8)}"
				     hx-trigger="load" hx-swap="innerHTML">
					<span class="loading loading-spinner loading-sm"></span>
				</div>
			@else
				<div id="poeBuildSummary" aria-live="polite" data-no-success-toast></div>
			@endif

			<!-- 젬/아이템 상세 레이어 컨테이너 (htmx 로 채워짐) -->
			<div id="poeBuildDetail"></div>
		</div>
	`
)
