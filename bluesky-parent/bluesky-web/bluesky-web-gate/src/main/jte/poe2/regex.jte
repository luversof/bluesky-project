@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null

!{
	String pageTitle = MessageUtil.getMessage("poe2.regex.title");
	boolean ko = PoeText.isKorean();
}

<%-- PoE2 경로석 정규식 — PoE1 지도 정규식과 같은 생성기(js/poe/regex.js). 데이터(/poe-data/poe2/waystone-mods.json)가 머리글 문구·임계값 앵커·보상 라벨을 함께 싣는다.
     경로석엔 아이템 수량 머리글이 없어 첫 임계값 칸은 아이템 희귀도, T17 같은 등급 탭도 없다(대신 옵션마다 "N등급+" 배지). --%>
@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div id="poeRegex" class="space-y-4" data-authenticated="${isAuthenticated}" data-locale="${ko ? "ko" : "en"}"
			data-mods-url="/poe-data/poe2/waystone-mods.json" data-presets-url="/poe2/api/regex/presets" data-quant-short="${MessageUtil.getMessage("poe2.regex.rarity")}">
			@template.poe2.header(title = pageTitle, meta = meta)
			<p class="text-sm text-base-content/60">${MessageUtil.getMessage("poe2.regex.desc")}</p>

			<%-- 결과 정규식 — 스크롤해도 상단에 붙어 따라온다 (navbar 가 sticky top-0 h-16 이라 그 아래에) --%>
			<div class="sticky top-16 z-30 rounded-2xl bg-base-100 p-3 shadow-md space-y-2">
				<div class="flex flex-wrap items-center gap-2">
					<code id="poeRegexOut" class="flex-1 min-w-[min(16rem,100%)] rounded-lg bg-base-200 px-3 py-2 font-mono text-sm break-all select-all min-h-[2.5rem]"></code>
					<span id="poeRegexLen" class="text-xs font-mono text-base-content/60 whitespace-nowrap">0 / 250</span>
					<button id="poeRegexCopy" class="btn btn-sm btn-primary">${MessageUtil.getMessage("poe.common.copy")}</button>
					<button id="poeRegexReset" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.regex.reset")}</button>
				</div>
				<div id="poeRegexWarn" class="text-xs text-warning hidden"></div>
				<div class="flex flex-wrap items-center gap-2">
					<input id="poeRegexImport" type="text" class="input input-sm input-bordered flex-1 min-w-[min(16rem,100%)] font-mono" placeholder='${MessageUtil.getMessage("poe2.regex.import.placeholder")}' aria-label="${MessageUtil.getMessage("poe.regex.import.aria")}" autocomplete="off">
					<button id="poeRegexImportBtn" class="btn btn-sm">${MessageUtil.getMessage("poe.regex.import")}</button>
				</div>
				<div id="poeRegexImportNote" class="text-xs text-base-content/60 hidden"></div>
				<div id="poeRegexCustom" class="flex flex-wrap items-center gap-1 hidden"></div>
			</div>

			<%-- 프리셋 저장/불러오기 — PoE1 지도 프리셋과 따로 저장된다 --%>
			<div class="rounded-2xl bg-base-100 p-3 shadow-sm space-y-2">
				<div class="flex flex-wrap items-center gap-2">
					<span class="text-sm font-semibold">${MessageUtil.getMessage("poe.regex.saved")}</span>
					<input id="poeRegexPresetName" type="text" class="input input-sm input-bordered w-48 max-w-full min-w-0" placeholder="${MessageUtil.getMessage("poe2.regex.name.placeholder")}" aria-label="${MessageUtil.getMessage("poe.regex.name.aria")}" maxlength="40" autocomplete="off">
					<button id="poeRegexPresetSave" class="btn btn-sm btn-success">${MessageUtil.getMessage("poe.regex.save")}</button>
					<span id="poeRegexPresetEditing" class="badge badge-info badge-sm hidden"></span>
					<button id="poeRegexPresetNew" class="btn btn-xs btn-ghost hidden">${MessageUtil.getMessage("poe.regex.saveasnew")}</button>
					@if(!isAuthenticated)
						<span class="text-xs text-warning">${MessageUtil.getMessage("poe.regex.login")}</span>
					@endif
				</div>
				<div id="poeRegexPresets" class="flex flex-col gap-1"></div>
			</div>

			<%-- 조건: 검색/임계값/옵션 --%>
			<div class="rounded-2xl bg-base-100 p-3 shadow-sm space-y-3">
				<div class="flex flex-wrap items-center gap-2">
					<input id="poeRegexSearch" type="search" class="input input-sm input-bordered w-full sm:w-64" placeholder="${MessageUtil.getMessage("poe2.regex.search.placeholder")}" aria-label="${MessageUtil.getMessage("poe.regex.search.aria")}" autocomplete="off">
					<span id="poeRegexCount" class="text-xs font-mono text-base-content/60"></span>
					<label class="label cursor-pointer gap-1.5 py-0">
						<input id="poeRegexEnglish" type="checkbox" class="checkbox checkbox-xs">
						<span class="label-text text-xs">${MessageUtil.getMessage("poe.regex.english")}</span>
					</label>
				</div>
				<div class="flex flex-wrap items-center gap-2 text-sm">
					<label class="flex items-center gap-1.5">
						<span class="text-xs">${MessageUtil.getMessage("poe2.regex.rarity.min")}</span>
						<input id="poeRegexQuant" type="number" class="input input-sm input-bordered w-20" min="1" max="999" placeholder="-">
					</label>
					<label class="flex items-center gap-1.5">
						<span class="text-xs">${MessageUtil.getMessage("poe2.regex.pack")}</span>
						<input id="poeRegexPack" type="number" class="input input-sm input-bordered w-20" min="1" max="999" placeholder="-">
					</label>
					<div class="join">
						<button class="btn btn-xs join-item" data-regex-combine="or">OR</button>
						<button class="btn btn-xs join-item" data-regex-combine="and">AND</button>
					</div>
					<span class="text-[11px] text-base-content/60">${MessageUtil.getMessage("poe2.regex.combine.hint")}</span>
				</div>
				<div class="text-[11px] text-base-content/60">${ko ? "옵션 클릭: 선택/해제(선택하면 제외 — 빨강, !로 거름) · \"제외\" 배지 클릭: 포함(초록, 강조)으로 전환 · 배지: 심연 = 심연 옵션, N등급+ = 그 등급 이상 경로석에만 붙음, 보상 = 최대 롤" : "Click a mod: select/clear (selected = exclude, red) · click the \"excl\" badge to switch it to include (green) · badges: Abyss, T6+ = tier 6+ only, reward = max roll"}</div>
			</div>

			<%-- 옵션 목록 (TS 렌더) --%>
			<div id="poeRegexList" class="grid gap-1.5 md:grid-cols-2 [&>*]:min-w-0"></div>
		</div>
		<script src="/js/poe/regex.js" defer></script>
	`)
