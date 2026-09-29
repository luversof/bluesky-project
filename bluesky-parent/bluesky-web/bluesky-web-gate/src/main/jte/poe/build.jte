@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""

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

			<form id="poeBuildImport" class="space-y-2 rounded-2xl bg-base-100 p-3 shadow-sm"
			      hx-post="/poe/htmx/build/import"
			      hx-target="#poeBuildSummary"
			      hx-swap="innerHTML">
				<div class="text-sm text-base-content/60">${descriptionLabel}</div>
				<textarea name="code" rows="4" required
				          class="textarea textarea-bordered w-full font-mono text-xs leading-5"
				          placeholder="${codePlaceholder}" autocomplete="off" spellcheck="false"></textarea>
				<%-- 루미너리 용병 — PoB 엔진엔 용병이 없어, 용병을 따로 만든 PoB 빌드로 계산한 오라·저주를 파티 탭으로 넣는다(API PoeMercenaryService).
				     재계산·업그레이드 가이드 버튼이 hx-include="#poeBuildImport textarea" 로 이 칸도 함께 보낸다. --%>
				<details class="rounded-box bg-base-200/40 px-3 py-2">
					<summary class="cursor-pointer text-sm font-semibold">${MessageUtil.getMessage("poe.build.merc.title")}</summary>
					<div class="mt-2 space-y-1">
						<div class="text-xs text-base-content/60">${MessageUtil.getMessage("poe.build.merc.desc")}</div>
						<textarea name="mercCode" rows="2"
						          class="textarea textarea-bordered w-full font-mono text-xs leading-5"
						          placeholder="${MessageUtil.getMessage("poe.build.merc.placeholder")}" autocomplete="off" spellcheck="false"></textarea>
					</div>
				</details>
				<div class="flex justify-end">
					<button type="submit" class="btn btn-primary btn-sm">${importLabel}</button>
				</div>
			</form>

			<div id="poeBuildSummary"></div>

			<!-- 젬/아이템 상세 레이어 컨테이너 (htmx 로 채워짐) -->
			<div id="poeBuildDetail"></div>
		</div>
	`
)
