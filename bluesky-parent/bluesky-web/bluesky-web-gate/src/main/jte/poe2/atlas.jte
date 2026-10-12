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
			<%-- 안내 + 범례 — PoE1 과 같은 자리(툴바 위) · 순서(핵심 → 주요 → 주얼 → 게임 전용), 10-12 C271 --%>
			<div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-base-content/60">
				<span>${MessageUtil.getMessage("poe2.atlas.hint")}</span>
				<span class="ml-auto flex flex-wrap items-center gap-3">
					<span class="flex items-center gap-1"><span class="inline-block w-3.5 h-3.5 rounded-full bg-[#e0bf6a]"></span>${MessageUtil.getMessage("poe.tree.legend.keystone")}</span>
					<span class="flex items-center gap-1"><span class="inline-block w-3 h-3 rounded-full bg-[#c9a55a]"></span>${MessageUtil.getMessage("poe.tree.legend.notable")}</span>
				</span>
			</div>
			<div id="poe2TreeShell" class="space-y-3">
			<%-- 툴바 — PoE1 과 같은 모바일 "도구" 접기(details.poe-tools · 데스크톱 펼침은 common.ts). 포인트는 접힘과 무관하게 밖에 둔다(10-12 C271) --%>
			<div class="flex items-start gap-2 rounded-2xl bg-base-100 p-3 shadow-sm">
			<details class="poe-tools min-w-0 flex-1">
				<summary class="btn btn-sm btn-outline sm:hidden">${MessageUtil.getMessage("poe.tree.tool.toggle")}</summary>
				<div class="flex flex-wrap items-center gap-2 pt-2 sm:pt-0">
				<select id="poe2AtlasTree" class="select select-sm select-bordered" aria-label="${MessageUtil.getMessage("poe2.atlas.jump")}"></select>
				<input id="poe2TreeSearch" type="search" class="input input-sm input-bordered w-full sm:w-56" placeholder="${MessageUtil.getMessage("poe.tree.search")}" aria-label="${MessageUtil.getMessage("poe.tree.search")}" autocomplete="off">
				<span id="poe2TreeSearchCount" class="text-xs font-mono text-base-content/60" aria-live="polite"></span>
				<div class="flex flex-wrap items-center gap-1">
					<button id="poe2TreeClear" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.reset")}</button>
				</div>
				<%-- PoE1 트리와 같은 도구(10-01): 실행취소/다시실행 · 스탯 요약 · 링크 복사 · 전체 화면 --%>
				<div class="flex flex-wrap items-center gap-1">
					<button id="poe2TreeUndo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Z">${MessageUtil.getMessage("poe.tree.tool.undo")}</button>
					<button id="poe2TreeRedo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Shift+Z">${MessageUtil.getMessage("poe.tree.tool.redo")}</button>
					<button id="poe2TreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.stats")}</button>
					<button id="poe2TreeCopy" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.copy")}</button>
					<button id="poe2TreeFullscreen" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.fullscreen")}</button>
				</div>
				</div>
			</details>
				<span id="poe2TreePoints" class="min-w-0 self-center text-sm text-base-content/70" aria-live="polite"></span>
			</div>
			<div class="relative">
				<canvas id="poe2TreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab touch-none" style="background:#0e1116"
				        data-locale="${PoeText.isKorean() ? "ko" : "en"}" data-tree-src="/poe-data/poe2/atlas-tree.json${net.luversof.web.gate.poe.config.PoeDataVersion.q("/poe-data/poe2/atlas-tree.json")}" data-sprite-base="/poe-data/poe2/atlas-sprites/"
				        role="img" aria-label="${pageTitle}"></canvas>
				<div id="poe2TreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs poe-node-popup overflow-hidden"></div>
				<div id="poe2TreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto poe-node-popup">
					<div class="sticky top-0 poe-popup-head text-xs">${MessageUtil.getMessage("poe.tree.stats.total")}</div>
					<div id="poe2TreeStatsBody" class="py-1"></div>
				</div>
				<%-- ＋/−/처음 보기 — PoE1 트리처럼 캔버스 오른쪽 아래에 띄운다(접힌 모바일 툴바에서도 늘 보인다, 10-12 C271) --%>
				<div class="absolute right-3 bottom-3 z-10 flex flex-col gap-1">
					<button id="poe2TreeZoomIn" type="button" class="btn btn-sm btn-square border border-amber-700/60 bg-stone-900/85 text-amber-100 text-lg" title="${MessageUtil.getMessage("poe.tree.tool.zoomin")}" aria-label="${MessageUtil.getMessage("poe.tree.tool.zoomin")}">+</button>
					<button id="poe2TreeZoomOut" type="button" class="btn btn-sm btn-square border border-amber-700/60 bg-stone-900/85 text-amber-100 text-lg" title="${MessageUtil.getMessage("poe.tree.tool.zoomout")}" aria-label="${MessageUtil.getMessage("poe.tree.tool.zoomout")}">−</button>
					<button id="poe2TreeFit" type="button" class="btn btn-sm btn-square border border-amber-700/60 bg-stone-900/85 text-amber-100 text-lg" title="${MessageUtil.getMessage("poe.tree.tool.fit")}" aria-label="${MessageUtil.getMessage("poe.tree.tool.fit")}">⤢</button>
				</div>
				<div id="poe2TreeStatus" class="absolute left-3 top-3 text-sm text-warning" aria-live="polite">${MessageUtil.getMessage("poe2.atlas.loading")}</div>
			</div>
			</div>
			<script src="/js/poe2/tree2.js"></script>
		</div>
	`
)
