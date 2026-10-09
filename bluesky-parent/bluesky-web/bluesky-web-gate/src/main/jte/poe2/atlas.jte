@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null

!{ String pageTitle = MessageUtil.getMessage("poe2.atlas.title"); }

<%-- PoE2 아틀라스 패시브 트리 — 패시브 트리와 같은 뷰어(js/poe2/tree2.js)의 아틀라스 모드. 데이터 /poe-data/poe2/atlas-tree.json(parse-atlas2.mjs, 게임 PSG),
     아이콘 /poe-data/poe2/atlas-sprites/(atlas-sprites2.mjs). 하위 트리 7개(아틀라스·의식·균열·환영·심연·사원·탐험) 뿌리가 모두 시작점, 점수는 트리별. --%>
@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-3">
			@template.poe2.header(title = pageTitle, meta = meta)
			<div id="poe2TreeShell" class="space-y-3">
			<div class="flex flex-wrap items-center gap-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<select id="poe2AtlasTree" class="select select-sm select-bordered" aria-label="${MessageUtil.getMessage("poe2.atlas.jump")}"></select>
				<input id="poe2TreeSearch" type="search" class="input input-sm input-bordered w-full sm:w-56" placeholder="${MessageUtil.getMessage("poe2.tree.search")}" aria-label="${MessageUtil.getMessage("poe2.tree.search")}" autocomplete="off">
				<span id="poe2TreeSearchCount" class="text-xs font-mono text-base-content/60" aria-live="polite"></span>
				<div class="flex flex-wrap items-center gap-1">
					<button id="poe2TreeZoomIn" type="button" class="btn btn-sm btn-ghost" aria-label="${PoeText.isKorean() ? "확대" : "Zoom in"}">＋</button>
					<button id="poe2TreeZoomOut" type="button" class="btn btn-sm btn-ghost" aria-label="${PoeText.isKorean() ? "축소" : "Zoom out"}">－</button>
					<button id="poe2TreeFit" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe2.tree.reset")}</button>
					<button id="poe2TreeClear" type="button" class="btn btn-sm btn-ghost">${PoeText.isKorean() ? "트리 초기화" : "Reset"}</button>
				</div>
				<%-- PoE1 트리와 같은 도구(10-01): 실행취소/다시실행 · 스탯 요약 · 링크 복사 · 전체 화면 --%>
				<div class="flex flex-wrap items-center gap-1">
					<button id="poe2TreeUndo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Z">${PoeText.isKorean() ? "실행취소" : "Undo"}</button>
					<button id="poe2TreeRedo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Shift+Z">${PoeText.isKorean() ? "다시실행" : "Redo"}</button>
					<button id="poe2TreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${PoeText.isKorean() ? "스탯 요약" : "Stat summary"}</button>
					<button id="poe2TreeCopy" type="button" class="btn btn-sm btn-ghost">${PoeText.isKorean() ? "링크 복사" : "Copy link"}</button>
					<button id="poe2TreeFullscreen" type="button" class="btn btn-sm btn-ghost">${PoeText.isKorean() ? "전체 화면" : "Fullscreen"}</button>
				</div>
				<span id="poe2TreePoints" class="ml-auto text-sm text-base-content/70" aria-live="polite"></span>
			</div>
			<div class="relative">
				<canvas id="poe2TreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab touch-none" style="background:#0e1116"
				        data-locale="${PoeText.isKorean() ? "ko" : "en"}" data-tree-src="/poe-data/poe2/atlas-tree.json${net.luversof.web.gate.poe.config.PoeDataVersion.q("/poe-data/poe2/atlas-tree.json")}" data-sprite-base="/poe-data/poe2/atlas-sprites/"
				        role="img" aria-label="${pageTitle}"></canvas>
				<div id="poe2TreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs poe-node-popup overflow-hidden"></div>
				<div id="poe2TreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto poe-node-popup">
					<div class="sticky top-0 poe-popup-head text-xs">${PoeText.isKorean() ? "할당 스탯 합계" : "Allocated stats"}</div>
					<div id="poe2TreeStatsBody" class="py-1"></div>
				</div>
				<div id="poe2TreeStatus" class="absolute left-3 top-3 text-sm text-warning" aria-live="polite">${MessageUtil.getMessage("poe2.atlas.loading")}</div>
			</div>
			</div>
			<div class="flex flex-wrap items-center gap-4 text-xs text-base-content/60">
				<span>${MessageUtil.getMessage("poe2.atlas.hint")}</span>
				<span class="flex items-center gap-1"><span class="inline-block w-3 h-3 rounded-full bg-[#c9a55a]"></span>${MessageUtil.getMessage("poe2.tree.legend.notable")}</span>
				<span class="flex items-center gap-1"><span class="inline-block w-3.5 h-3.5 rounded-full bg-[#e0bf6a]"></span>${MessageUtil.getMessage("poe2.tree.legend.keystone")}</span>
			</div>
			<script src="/js/poe2/tree2.js"></script>
		</div>
	`
)
