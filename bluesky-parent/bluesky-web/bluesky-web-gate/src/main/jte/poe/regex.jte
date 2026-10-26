@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""

!{
	String pageTitle = MessageUtil.getMessage("poe.regex.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	boolean ko = PoeText.isKorean();
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div id="poeRegex" class="space-y-4" data-authenticated="${isAuthenticated}" data-locale="${ko ? "ko" : "en"}">
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
				<span class="badge badge-ghost">${patchLabel}</span>
				<span class="text-sm text-base-content/60">${MessageUtil.getMessage("poe.regex.desc")}</span>
			</div>

			<%-- 결과 정규식 — 스크롤해도 상단에 붙어 따라온다 (navbar 가 sticky top-0 h-16 이라 그 아래에) --%>
			<div class="sticky top-16 z-30 rounded-2xl bg-base-100 p-3 shadow-md space-y-2">
				<div class="flex flex-wrap items-center gap-2">
					<code id="poeRegexOut" class="flex-1 min-w-[min(16rem,100%)] rounded-lg bg-base-200 px-3 py-2 font-mono text-sm break-all select-all min-h-[2.5rem]"></code>
					<span id="poeRegexLen" class="text-xs font-mono text-base-content/60 whitespace-nowrap">0 / 250</span>
					<button id="poeRegexCopy" class="btn btn-sm btn-primary">${MessageUtil.getMessage("poe.common.copy")}</button>
					<button id="poeRegexReset" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.regex.reset")}</button>
				</div>
				<div id="poeRegexWarn" class="text-xs text-warning hidden"></div>
				<%-- 기존 정규식 역파싱 — 붙여넣으면 해당 모드를 자동 선택해 편집 가능 상태로 복원 --%>
				<div class="flex flex-wrap items-center gap-2">
					<input id="poeRegexImport" type="text" class="input input-sm input-bordered flex-1 min-w-[min(16rem,100%)] font-mono" placeholder='${MessageUtil.getMessage("poe.regex.import.placeholder")}' autocomplete="off" aria-label="${MessageUtil.getMessage("poe.regex.import.aria")}">
					<button id="poeRegexImportBtn" class="btn btn-sm">${MessageUtil.getMessage("poe.regex.import")}</button>
				</div>
				<div id="poeRegexImportNote" class="text-xs text-base-content/60 hidden"></div>
				<div id="poeRegexCustom" class="flex flex-wrap items-center gap-1 hidden"></div>
			</div>

			<%-- 프리셋 저장/불러오기 --%>
			<div class="rounded-2xl bg-base-100 p-3 shadow-sm space-y-2">
				<div class="flex flex-wrap items-center gap-2">
					<span class="text-sm font-semibold">${MessageUtil.getMessage("poe.regex.saved")}</span>
					<input id="poeRegexPresetName" type="text" class="input input-sm input-bordered w-48 max-w-full min-w-0" placeholder="${MessageUtil.getMessage("poe.regex.name.placeholder")}" maxlength="40" autocomplete="off" aria-label="${MessageUtil.getMessage("poe.regex.name.aria")}">
					<button id="poeRegexPresetSave" class="btn btn-sm btn-success">${MessageUtil.getMessage("poe.regex.save")}</button>
					<span id="poeRegexPresetEditing" class="badge badge-info badge-sm hidden"></span>
					<button id="poeRegexPresetNew" class="btn btn-xs btn-ghost hidden">${MessageUtil.getMessage("poe.regex.saveasnew")}</button>
					@if(!isAuthenticated)
						<span class="text-xs text-warning">${MessageUtil.getMessage("poe.regex.login")}</span>
					@endif
				</div>
				<div id="poeRegexPresets" class="flex flex-col gap-1"></div>
			</div>

			<%-- 조건: 탭/검색/임계값/옵션 --%>
			<div class="rounded-2xl bg-base-100 p-3 shadow-sm space-y-3">
				<div class="flex flex-wrap items-center gap-2">
					<div class="join" role="tablist">
						<button class="btn btn-sm join-item" data-regex-tab="normal">${MessageUtil.getMessage("poe.regex.tab.normal")}</button>
						<button class="btn btn-sm join-item" data-regex-tab="uber">${MessageUtil.getMessage("poe.regex.tab.t17")}</button>
					</div>
					<input id="poeRegexSearch" type="search" class="input input-sm input-bordered w-full sm:w-64" placeholder="${MessageUtil.getMessage("poe.regex.search.placeholder")}" autocomplete="off" aria-label="${MessageUtil.getMessage("poe.regex.search.aria")}">
					<span id="poeRegexCount" class="text-xs font-mono text-base-content/60"></span>
					<label class="label cursor-pointer gap-1.5 py-0">
						<input id="poeRegexEnglish" type="checkbox" class="checkbox checkbox-xs">
						<span class="label-text text-xs">${MessageUtil.getMessage("poe.regex.english")}</span>
					</label>
				</div>
				<div class="flex flex-wrap items-center gap-2 text-sm">
					<label class="flex items-center gap-1.5">
						<span class="text-xs">${MessageUtil.getMessage("poe.regex.quant")}</span>
						<input id="poeRegexQuant" type="number" class="input input-sm input-bordered w-20" min="1" max="300" placeholder="-">
					</label>
					<label class="flex items-center gap-1.5">
						<span class="text-xs">${MessageUtil.getMessage("poe.regex.pack")}</span>
						<input id="poeRegexPack" type="number" class="input input-sm input-bordered w-20" min="1" max="99" placeholder="-">
					</label>
					<div class="join">
						<button class="btn btn-xs join-item" data-regex-combine="or">OR</button>
						<button class="btn btn-xs join-item" data-regex-combine="and">AND</button>
					</div>
					<span class="text-[11px] text-base-content/60">${MessageUtil.getMessage("poe.regex.combine.hint")}</span>
				</div>
				<div class="text-[11px] text-base-content/60">${ko ? "옵션 클릭: 선택/해제(선택하면 제외 — 빨강, !로 거름) · \"제외\" 배지 클릭: 포함(초록, 강조)으로 전환 · 문구가 포함관계인 옵션(일반↔17등급 쌍둥이)은 함께 선택됩니다" : "Click a mod: select/clear (selected = exclude, red) · click the \"excl\" badge to switch it to include (green) · text-subset twins (normal↔T17) are linked"}</div>
			</div>

			<%-- 모드 목록 (TS 렌더) --%>
			<div id="poeRegexList" class="grid gap-1.5 md:grid-cols-2 [&>*]:min-w-0"></div>
		</div>
		<script src="/js/poe/regex.js" defer></script>
	`)
