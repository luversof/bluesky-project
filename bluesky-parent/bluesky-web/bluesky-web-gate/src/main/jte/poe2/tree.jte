@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param java.util.List<Poe2.SimChoice> evalSkills = java.util.List.of()

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
			<div id="poe2TreeShell" class="space-y-3">
			<div class="flex flex-wrap items-center gap-2 rounded-2xl bg-base-100 p-3 shadow-sm">
				<select id="poe2TreeClass" class="select select-sm select-bordered" aria-label="${MessageUtil.getMessage("poe2.tree.class")}"></select>
				<select id="poe2TreeAsc" class="select select-sm select-bordered" aria-label="${MessageUtil.getMessage("poe2.tree.ascendancy")}" disabled></select>
				<input id="poe2TreeSearch" type="search" class="input input-sm input-bordered w-full sm:w-56" placeholder="${MessageUtil.getMessage("poe2.tree.search")}" aria-label="${MessageUtil.getMessage("poe2.tree.search")}" autocomplete="off">
				<span id="poe2TreeSearchCount" class="text-xs font-mono text-base-content/50" aria-live="polite"></span>
				<div class="flex items-center gap-1">
					<button id="poe2TreeZoomIn" type="button" class="btn btn-sm btn-ghost" aria-label="${PoeText.isKorean() ? "확대" : "Zoom in"}">＋</button>
					<button id="poe2TreeZoomOut" type="button" class="btn btn-sm btn-ghost" aria-label="${PoeText.isKorean() ? "축소" : "Zoom out"}">－</button>
					<button id="poe2TreeFit" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe2.tree.reset")}</button>
					<button id="poe2TreeClear" type="button" class="btn btn-sm btn-ghost">${PoeText.isKorean() ? "할당 초기화" : "Clear points"}</button>
				</div>
				<%-- PoE1 트리와 같은 도구(10-01): 실행취소/다시실행 · 스탯 요약 · 링크 복사 · 전체 화면 --%>
				<div class="flex flex-wrap items-center gap-1">
					<button id="poe2TreeUndo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Z">${PoeText.isKorean() ? "실행취소" : "Undo"}</button>
					<button id="poe2TreeRedo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Shift+Z">${PoeText.isKorean() ? "다시실행" : "Redo"}</button>
					<button id="poe2TreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${PoeText.isKorean() ? "스탯 요약" : "Stat summary"}</button>
					<button id="poe2TreeCopy" type="button" class="btn btn-sm btn-ghost">${PoeText.isKorean() ? "링크 복사" : "Copy link"}</button>
					<button id="poe2TreeFullscreen" type="button" class="btn btn-sm btn-ghost">${PoeText.isKorean() ? "전체 화면" : "Fullscreen"}</button>
				</div>
				<%-- 트리 계산 — PoE1 트리의 "트리 계산"과 같은 자리(10-01). 직업·전직·노드는 tree2.js 가 숨은 칸에 채운다 --%>
				<form id="poe2TreeEvalForm" class="flex flex-wrap items-center gap-1" data-no-success-toast
				      hx-post="/poe2/htmx/tree/eval" hx-target="#poe2TreeEvalBody" hx-swap="innerHTML" hx-indicator="#poe2TreeEvalSpin">
					<input type="hidden" name="className" value="">
					<input type="hidden" name="ascendancy" value="">
					<input type="hidden" name="nodes" value="">
					<input type="hidden" name="attrs" value="">
					<div class="w-52">
						<select name="skill" data-poe-multi data-placeholder="${PoeText.isKorean() ? "메인 스킬(선택)…" : "Main skill (optional)…"}" class="select select-sm select-bordered w-full" title="${PoeText.isKorean() ? "메인 스킬" : "Main skill"}">
							<option value="">${PoeText.isKorean() ? "스킬 없이(방어·능력치)" : "No skill (defence only)"}</option>
							@for(Poe2.SimChoice c : evalSkills)
								<option value="${c.name()}">${PoeText.name(c.nameKo(), c.name())}</option>
							@endfor
						</select>
					</div>
					<button id="poe2TreeEval" type="submit" class="btn btn-sm btn-primary btn-outline">${PoeText.isKorean() ? "트리 계산" : "Evaluate"}</button>
				</form>
				<span id="poe2TreePoints" class="ml-auto text-sm text-base-content/70" aria-live="polite"></span>
			</div>
			<div class="relative">
				<canvas id="poe2TreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab touch-none" style="background:#0e1116"
				        data-locale="${PoeText.isKorean() ? "ko" : "en"}" data-tree-src="/poe-data/poe2/passive-tree.json"
				        role="img" aria-label="${pageTitle}"></canvas>
				<div id="poe2TreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs poe-node-popup overflow-hidden"></div>
				<div id="poe2TreeEvalPanel" class="hidden absolute left-2 top-2 z-10 w-72 max-h-[70%] overflow-y-auto poe-node-popup">
					<div class="sticky top-0 poe-popup-head text-xs flex items-center">
						<span class="flex-1 text-center">${PoeText.isKorean() ? "트리 계산 (PoB-PoE2)" : "Tree evaluation (PoB-PoE2)"}</span>
						<span id="poe2TreeEvalSpin" class="htmx-indicator loading loading-spinner loading-xs mr-1"></span>
						<button id="poe2TreeEvalClose" type="button" class="px-1 text-stone-400 hover:text-white" aria-label="${PoeText.isKorean() ? "닫기" : "Close"}">✕</button>
					</div>
					<div id="poe2TreeEvalBody" class="p-2"></div>
				</div>
				<div id="poe2TreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto poe-node-popup">
					<div class="sticky top-0 poe-popup-head text-xs">${PoeText.isKorean() ? "할당 스탯 합계" : "Allocated stats"}</div>
					<div id="poe2TreeStatsBody" class="py-1"></div>
				</div>
				<div id="poe2TreeStatus" class="absolute left-3 top-3 text-sm text-warning" aria-live="polite">${MessageUtil.getMessage("poe2.tree.loading")}</div>
			</div>
			</div>
			<div class="flex flex-wrap items-center gap-4 text-xs text-base-content/60">
				<span>${MessageUtil.getMessage("poe2.tree.hint")}</span>
				<span class="flex items-center gap-1"><span class="inline-block w-3 h-3 rounded-full bg-[#c9a55a]"></span>${MessageUtil.getMessage("poe2.tree.legend.notable")}</span>
				<span class="flex items-center gap-1"><span class="inline-block w-3.5 h-3.5 rounded-full bg-[#e0bf6a]"></span>${MessageUtil.getMessage("poe2.tree.legend.keystone")}</span>
				<span class="flex items-center gap-1"><span class="inline-block w-3 h-3 rounded-full bg-[#6aa8e0]"></span>${MessageUtil.getMessage("poe2.tree.legend.jewel")}</span>
				<span class="flex items-center gap-1"><span class="inline-block w-3 h-3 rounded-full bg-[#b58cd6]"></span>${MessageUtil.getMessage("poe2.tree.legend.ascendancy")}</span>
			</div>
			<script src="/js/poe2/tree2.js"></script>
			<script src="/js/poe/multiSelect.js"></script>
		</div>
	`
)
