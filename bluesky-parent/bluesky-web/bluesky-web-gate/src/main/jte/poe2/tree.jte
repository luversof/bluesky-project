@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param java.util.List<Poe2.SimChoice> evalSkills = java.util.List.of()
@param java.util.List<Poe2.Unique> treeJewels = java.util.List.of()

!{ String pageTitle = MessageUtil.getMessage("poe2.tree.title"); }

<%-- PoE2 패시브 트리 — 캔버스 뷰어/플래너(js/poe2/tree2.js). 데이터는 게이트가 /poe-data/poe2/passive-tree.json 으로 서빙. --%>
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
				<span>${MessageUtil.getMessage("poe.tree.hint")}</span>
				<span class="ml-auto flex flex-wrap items-center gap-3">
					<span class="flex items-center gap-1"><span class="inline-block w-3.5 h-3.5 rounded-full bg-[#e0bf6a]"></span>${MessageUtil.getMessage("poe.tree.legend.keystone")}</span>
					<span class="flex items-center gap-1"><span class="inline-block w-3 h-3 rounded-full bg-[#c9a55a]"></span>${MessageUtil.getMessage("poe.tree.legend.notable")}</span>
					<span class="flex items-center gap-1"><span class="inline-block w-3 h-3 rounded-full bg-[#6aa8e0]"></span>${MessageUtil.getMessage("poe.tree.legend.jewel")}</span>
					<span class="flex items-center gap-1"><span class="inline-block w-3 h-3 rounded-full bg-[#b58cd6]"></span>${MessageUtil.getMessage("poe2.tree.legend.ascendancy")}</span>
				</span>
			</div>
			<div id="poe2TreeShell" class="space-y-3">
			<%-- 툴바 — PoE1 과 같은 모바일 "도구" 접기(details.poe-tools · 데스크톱 펼침은 common.ts). 포인트는 접힘과 무관하게 밖에 둔다(10-12 C271) --%>
			<div class="flex items-start gap-2 rounded-2xl bg-base-100 p-3 shadow-sm">
			<details class="poe-tools min-w-0 flex-1">
				<summary class="btn btn-sm btn-outline sm:hidden">${MessageUtil.getMessage("poe.tree.tool.toggle")}</summary>
				<div class="flex flex-wrap items-center gap-2 pt-2 sm:pt-0">
				<select id="poe2TreeClass" class="select select-sm select-bordered" aria-label="${MessageUtil.getMessage("poe2.tree.class")}"></select>
				<select id="poe2TreeAsc" class="select select-sm select-bordered" aria-label="${MessageUtil.getMessage("poe2.tree.ascendancy")}" disabled></select>
				<input id="poe2TreeSearch" type="search" title="${MessageUtil.getMessage("poe.tree.search.title")}" class="input input-sm input-bordered w-full sm:w-56" placeholder="${MessageUtil.getMessage("poe.tree.search")}" aria-label="${MessageUtil.getMessage("poe.tree.search")}" autocomplete="off">
				<span id="poe2TreeSearchCount" class="text-xs font-mono text-base-content/60" aria-live="polite"></span>
				<div class="flex flex-wrap items-center gap-1">
					<button id="poe2TreeClear" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.reset")}</button>
				</div>
				<%-- 찍기 모드 — 무기 세트 전용 패시브(10-02, 인게임 무기 세트 패시브 포인트 · PoB allocMode). 세트 노드는 세트 색 테두리 --%>
				<div class="join flex-wrap" role="group" aria-label="${MessageUtil.getMessage("poe2.tree.mode")}">
					<button type="button" class="btn btn-sm join-item" data-tree-set="0">${MessageUtil.getMessage("poe2.tree.mode.shared")}</button>
					<button type="button" class="btn btn-sm join-item" data-tree-set="1" title="${MessageUtil.getMessage("poe2.tree.mode.set1")}"><span class="inline-block w-2 h-2 rounded-full mr-1" style="background:#e0803a"></span>${MessageUtil.getMessage("poe2.tree.set1")}</button>
					<button type="button" class="btn btn-sm join-item" data-tree-set="2" title="${MessageUtil.getMessage("poe2.tree.mode.set2")}"><span class="inline-block w-2 h-2 rounded-full mr-1" style="background:#4aa3df"></span>${MessageUtil.getMessage("poe2.tree.set2")}</button>
				</div>
				<%-- PoE1 트리와 같은 도구(10-01): 실행취소/다시실행 · 스탯 요약 · 링크 복사 · 전체 화면 --%>
				<div class="flex flex-wrap items-center gap-1">
					<button id="poe2TreeUndo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Z">${MessageUtil.getMessage("poe.tree.tool.undo")}</button>
					<button id="poe2TreeRedo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Shift+Z">${MessageUtil.getMessage("poe.tree.tool.redo")}</button>
					<button id="poe2TreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.stats")}</button>
					<button id="poe2TreeCopy" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.copy")}</button>
					<button id="poe2TreeFullscreen" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.fullscreen")}</button>
				</div>
				<%-- 트리 계산 — PoE1 트리의 "트리 계산"과 같은 자리(10-01). 직업·전직·노드는 tree2.js 가 숨은 칸에 채운다 --%>
				<form id="poe2TreeEvalForm" class="flex flex-wrap items-center gap-1 min-w-0 max-w-full" data-no-success-toast
				      hx-post="/poe2/htmx/tree/eval" hx-target="#poe2TreeEvalBody" hx-swap="innerHTML" hx-indicator="#poe2TreeEvalSpin">
					<input type="hidden" name="className" value="">
					<input type="hidden" name="ascendancy" value="">
					<input type="hidden" name="nodes" value="">
					<input type="hidden" name="attrs" value="">
					<input type="hidden" name="sets" value="">
					<input type="hidden" name="jewels" value="">
					<div class="w-52 max-w-full">
						<select name="skill" data-poe-multi data-placeholder="${MessageUtil.getMessage("poe.tree.eval.gem")}…" class="select select-sm select-bordered w-full" title="${MessageUtil.getMessage("poe.tree.eval.skill")}">
							<option value="">${MessageUtil.getMessage("poe2.tree.eval.noskill")}</option>
							@for(Poe2.SimChoice c : evalSkills)
								<option value="${c.name()}">${PoeText.name(c.nameKo(), c.name())}</option>
							@endfor
						</select>
					</div>
					<button id="poe2TreeEval" type="submit" class="btn btn-sm btn-primary btn-outline">${MessageUtil.getMessage("poe.tree.eval")}</button>
					<%-- PoE1 트리의 "→ 시뮬"과 같은 자리(10-02) — 이 트리로 시뮬레이터를 연다(tree2.js) --%>
					<button id="poe2TreeToSim" type="button" class="btn btn-sm btn-secondary btn-outline">${MessageUtil.getMessage("poe.tree.tosim")}</button>
				</form>
				</div>
			</details>
				<span id="poe2TreePoints" class="min-w-0 self-center text-sm text-base-content/70" aria-live="polite"></span>
			</div>
			<%-- 주얼 칸에 꽂을 고유 주얼(10-03 C73) — tree2.js 가 주얼 칸 우클릭 메뉴에서 쓴다. 옵션 줄은 연결 없이 찍기 규칙(From Nothing · 고리)에 쓴다 --%>
			<datalist id="poe2TreeJewelList">@for(Poe2.Unique tj : treeJewels)<option value="${tj.slug()}" data-name="${tj.name()}" data-name-ko="${tj.nameKo() != null ? tj.nameKo() : tj.name()}" data-kw="${net.luversof.web.gate.poe2.Poe2View.keywordData(tj.keywords())}" data-explicits="${tj.explicits() == null ? "" : String.join("\n", tj.explicits())}" data-explicits-ko="${tj.explicitsKo() == null ? "" : String.join("\n", tj.explicitsKo())}" data-radius="${tj.radius() != null ? tj.radius() : ""}"></option>@endfor</datalist>
			<%-- 변형(From Nothing 핵심 · Controlled Metamorphosis 고리 · 장대한 파장 …) — 고르기 창의 둘째 단계(10-03 C74) --%>
			<div id="poe2TreeJewelVariants" hidden>@for(Poe2.Unique tj : treeJewels)@if(tj.variants() != null && tj.variants().size() > 1)@for(Poe2.UniqueVariant tv : tj.variants())<span data-slug="${tj.slug()}" data-index="${tv.index()}" data-name="${tv.name()}" data-name-ko="${tv.nameKo() != null ? tv.nameKo() : tv.name()}" data-kw="${net.luversof.web.gate.poe2.Poe2View.keywordData(tv.keywords())}" data-explicits="${tv.explicits() == null ? "" : String.join("\n", tv.explicits())}" data-explicits-ko="${tv.explicitsKo() == null ? "" : String.join("\n", tv.explicitsKo())}"></span>@endfor@endif@endfor</div>
			<div class="relative">
				<canvas id="poe2TreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab touch-none" style="background:#0e1116"
				        data-locale="${PoeText.isKorean() ? "ko" : "en"}" data-tree-src="/poe-data/poe2/passive-tree.json${net.luversof.web.gate.poe.config.PoeDataVersion.q("/poe-data/poe2/passive-tree.json")}" data-sprite-base="/poe-data/poe2/tree-sprites/"
				        role="img" aria-label="${pageTitle}"></canvas>
				<div id="poe2TreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs poe-node-popup overflow-hidden"></div>
				<div id="poe2TreeEvalPanel" class="hidden absolute left-2 top-2 z-10 w-72 max-h-[70%] overflow-y-auto poe-node-popup">
					<div class="sticky top-0 poe-popup-head text-xs flex items-center">
						<span class="flex-1 text-center">${MessageUtil.getMessage("poe.tree.eval.title")}</span>
						<span id="poe2TreeEvalSpin" class="htmx-indicator loading loading-spinner loading-xs mr-1"></span>
						<button id="poe2TreeEvalClose" type="button" class="px-1 text-stone-400 hover:text-white" aria-label="${MessageUtil.getMessage("poe.tree.tool.close")}">✕</button>
					</div>
					<div id="poe2TreeEvalBody" class="p-2"></div>
				</div>
				<div id="poe2TreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto poe-node-popup">
					<div class="sticky top-0 poe-popup-head text-xs">${MessageUtil.getMessage("poe.tree.stats.total")}</div>
					<%-- 기준 세트(10-02) — 인게임은 켜진 무기 세트 전용 패시브만 적용. 세트 노드가 있을 때만 보인다(tree2.js) --%>
					<div id="poe2TreeStatsSet" class="hidden flex justify-center gap-3 border-b border-white/10 px-3 py-1 text-[11px]" role="group" aria-label="${MessageUtil.getMessage("poe2.tree.summary.set")}">
						<button type="button" data-summary-set="1"><span class="inline-block w-2 h-2 rounded-full mr-1" style="background:#e0803a"></span>${MessageUtil.getMessage("poe2.tree.summary.set1")}</button>
						<button type="button" data-summary-set="2"><span class="inline-block w-2 h-2 rounded-full mr-1" style="background:#4aa3df"></span>${MessageUtil.getMessage("poe2.tree.summary.set2")}</button>
					</div>
					<div id="poe2TreeStatsBody" class="py-1"></div>
				</div>
				<%-- ＋/−/처음 보기 — PoE1 트리처럼 캔버스 오른쪽 아래에 띄운다(접힌 모바일 툴바에서도 늘 보인다, 10-12 C271) --%>
				<div class="absolute right-3 bottom-3 z-10 flex flex-col gap-1">
					<button id="poe2TreeZoomIn" type="button" class="btn btn-sm btn-square border border-amber-700/60 bg-stone-900/85 text-amber-100 text-lg" title="${MessageUtil.getMessage("poe.tree.tool.zoomin")}" aria-label="${MessageUtil.getMessage("poe.tree.tool.zoomin")}">+</button>
					<button id="poe2TreeZoomOut" type="button" class="btn btn-sm btn-square border border-amber-700/60 bg-stone-900/85 text-amber-100 text-lg" title="${MessageUtil.getMessage("poe.tree.tool.zoomout")}" aria-label="${MessageUtil.getMessage("poe.tree.tool.zoomout")}">−</button>
					<button id="poe2TreeFit" type="button" class="btn btn-sm btn-square border border-amber-700/60 bg-stone-900/85 text-amber-100 text-lg" title="${MessageUtil.getMessage("poe.tree.tool.fit")}" aria-label="${MessageUtil.getMessage("poe.tree.tool.fit")}">⤢</button>
				</div>
				<div id="poe2TreeStatus" class="absolute left-3 top-3 text-sm text-warning" aria-live="polite">${MessageUtil.getMessage("poe2.tree.loading")}</div>
			</div>
			</div>
			<script src="/js/poe2/tree2.js"></script>
			<script src="/js/poe/multiSelect.js"></script>
		</div>
	`
)
