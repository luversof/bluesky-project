@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""

!{
	String pageTitle = MessageUtil.getMessage("layout.menu.poe.regex");
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
				<span class="text-sm text-base-content/50">${ko ? "맵 모드를 골라 인게임 검색창용 정규식을 만들고 저장합니다 — 제외(!)·포함 선택, 최단 고유 문자열 자동 압축" : "Pick map mods to build an in-game search regex — exclude(!)/include, auto-shortened unique substrings"}</span>
			</div>

			<%-- 결과 정규식 — 스크롤해도 상단에 붙어 따라온다 (navbar 가 sticky top-0 h-16 이라 그 아래에) --%>
			<div class="sticky top-16 z-30 rounded-2xl bg-base-100 p-3 shadow-md space-y-2">
				<div class="flex flex-wrap items-center gap-2">
					<code id="poeRegexOut" class="flex-1 min-w-[16rem] rounded-lg bg-base-200 px-3 py-2 font-mono text-sm break-all select-all min-h-[2.5rem]"></code>
					<span id="poeRegexLen" class="text-xs font-mono text-base-content/50 whitespace-nowrap">0 / 250</span>
					<button id="poeRegexCopy" class="btn btn-sm btn-primary">${ko ? "복사" : "Copy"}</button>
					<button id="poeRegexReset" class="btn btn-sm btn-ghost">${ko ? "리셋" : "Reset"}</button>
				</div>
				<div id="poeRegexWarn" class="text-xs text-warning hidden"></div>
				<%-- 기존 정규식 역파싱 — 붙여넣으면 해당 모드를 자동 선택해 편집 가능 상태로 복원 --%>
				<div class="flex flex-wrap items-center gap-2">
					<input id="poeRegexImport" type="text" class="input input-sm input-bordered flex-1 min-w-[16rem] font-mono" placeholder='${ko ? "기존 정규식 붙여넣기 — 예: \"!기절 면역|반사\" \"량: \\+9[5-9]%\"" : "Paste an existing regex to edit"}' autocomplete="off">
					<button id="poeRegexImportBtn" class="btn btn-sm">${ko ? "가져오기" : "Import"}</button>
				</div>
				<div id="poeRegexImportNote" class="text-xs text-base-content/50 hidden"></div>
				<div id="poeRegexCustom" class="flex flex-wrap items-center gap-1 hidden"></div>
			</div>

			<%-- 프리셋 저장/불러오기 --%>
			<div class="rounded-2xl bg-base-100 p-3 shadow-sm space-y-2">
				<div class="flex flex-wrap items-center gap-2">
					<span class="text-sm font-semibold">${ko ? "저장한 정규식" : "Saved presets"}</span>
					<input id="poeRegexPresetName" type="text" class="input input-sm input-bordered w-48" placeholder="${ko ? "이름 (예: 8모드 거르기)" : "Preset name"}" maxlength="40" autocomplete="off">
					<button id="poeRegexPresetSave" class="btn btn-sm btn-success">${ko ? "저장" : "Save"}</button>
					<span id="poeRegexPresetEditing" class="badge badge-info badge-sm hidden"></span>
					<button id="poeRegexPresetNew" class="btn btn-xs btn-ghost hidden">${ko ? "새로 저장으로 전환" : "Save as new"}</button>
					@if(!isAuthenticated)
						<span class="text-xs text-warning">${ko ? "저장하려면 로그인이 필요합니다" : "Login required to save"}</span>
					@endif
				</div>
				<div id="poeRegexPresets" class="flex flex-col gap-1"></div>
			</div>

			<%-- 조건: 탭/검색/임계값/옵션 --%>
			<div class="rounded-2xl bg-base-100 p-3 shadow-sm space-y-3">
				<div class="flex flex-wrap items-center gap-2">
					<div class="join" role="tablist">
						<button class="btn btn-sm join-item" data-regex-tab="normal">${ko ? "일반 맵" : "Normal maps"}</button>
						<button class="btn btn-sm join-item" data-regex-tab="uber">${ko ? "T17(악몽)" : "T17"}</button>
					</div>
					<input id="poeRegexSearch" type="search" class="input input-sm input-bordered w-full sm:w-64" placeholder="${ko ? "맵모드 검색 (예: 반사, 저주)" : "Search map mods"}" autocomplete="off">
					<span id="poeRegexCount" class="text-xs font-mono text-base-content/50"></span>
					<label class="label cursor-pointer gap-1.5 py-0">
						<input id="poeRegexEnglish" type="checkbox" class="checkbox checkbox-xs">
						<span class="label-text text-xs">${ko ? "영문 정규식" : "English client regex"}</span>
					</label>
				</div>
				<div class="flex flex-wrap items-center gap-2 text-sm">
					<label class="flex items-center gap-1.5">
						<span class="text-xs">${ko ? "아이템 수량 ≥" : "Item quantity ≥"}</span>
						<input id="poeRegexQuant" type="number" class="input input-sm input-bordered w-20" min="1" max="300" placeholder="-">
					</label>
					<label class="flex items-center gap-1.5">
						<span class="text-xs">${ko ? "몬스터 무리 규모 ≥" : "Pack size ≥"}</span>
						<input id="poeRegexPack" type="number" class="input input-sm input-bordered w-20" min="1" max="99" placeholder="-">
					</label>
					<div class="join">
						<button class="btn btn-xs join-item" data-regex-combine="or">OR</button>
						<button class="btn btn-xs join-item" data-regex-combine="and">AND</button>
					</div>
					<span class="text-[11px] text-base-content/40">${ko ? "OR=둘 중 하나, AND=둘 다 충족한 맵 강조" : "OR=either, AND=both"}</span>
				</div>
				<div class="text-[11px] text-base-content/40">${ko ? "모드 클릭: 선택/해제(선택하면 제외 — 빨강, !로 거름) · \"제외\" 배지 클릭: 포함(초록, 강조)으로 전환 · 문구가 포함관계인 모드(일반↔T17 쌍둥이)는 함께 선택됩니다" : "Click a mod: select/clear (selected = exclude, red) · click the \"excl\" badge to switch it to include (green) · text-subset twins (normal↔T17) are linked"}</div>
			</div>

			<%-- 모드 목록 (TS 렌더) --%>
			<div id="poeRegexList" class="grid gap-1.5 md:grid-cols-2 [&>*]:min-w-0"></div>
		</div>
		<script src="/js/poe/regex.js" defer></script>
	`)
