@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param boolean hasAtlasData = false
@param java.util.List<String> treeVersions = java.util.List.of()
@param String activeVer = ""
@param String activePatch = ""
@param String treeSrc = "/poe-data/atlas-tree.json"
@param String spritesSrc = "/poe-data/tree-sprites-atlas.json"

!{
	String pageTitle = MessageUtil.getMessage("poe.atlas.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String hintLabel = MessageUtil.getMessage("poe.tree.hint");
	String legendKeystoneLabel = MessageUtil.getMessage("poe.tree.legend.keystone");
	String legendNotableLabel = MessageUtil.getMessage("poe.tree.legend.notable");
	String legendMasteryLabel = MessageUtil.getMessage("poe.tree.legend.mastery");
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
				@if(!treeVersions.isEmpty())
					<%-- 버전별 아카이브 조회 — 빈 값=현행(최신) 데이터 --%>
					<select class="select select-xs select-bordered" data-poe-ver-select aria-label="${MessageUtil.getMessage("poe.tree.version")}">
						<option value="">${MessageUtil.getMessage("poe.tree.version.latest")}</option>
						@for(String v : treeVersions)
							<option value="${v}" selected="${v.equals(activeVer)}">${v}</option>
						@endfor
					</select>
				@endif
				@if(!activeVer.isEmpty())
					<span class="badge badge-warning badge-sm">${MessageUtil.getMessage("poe.tree.version.archive")}@if(!activePatch.isEmpty()) · ${activePatch}@endif — ${MessageUtil.getMessage("poe.tree.version.calclatest")}</span>
				@endif
			</div>

			@if(!hasAtlasData)
				@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-atlas-tree.mjs && node tree-sprites.mjs")
			@else
				<div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-base-content/60">
					<span>${hintLabel}</span>
					<span class="ml-auto flex flex-wrap items-center gap-3">
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#cf6642"></span> ${legendKeystoneLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#d4a94e"></span> ${legendNotableLabel}</span>
						<%-- 숙련 126개를 그린다(10-23 Q67 — 게임 PassiveSkillSearchMastery, 패시브 화면과 같은 색) --%>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#9a6ad4"></span> ${legendMasteryLabel}</span>
					</span>
				</div>
				<div id="poeTreeShell" class="relative bg-base-100">
					<%-- 좁은 화면에서는 이 툴바가 7줄로 쌓여 화면의 2/3 를 먹고 트리가 하단 320px 에 갇혔다
					     (실측 390x820: 캔버스 top=548 · h=320). 모바일에서만 접고 데스크톱은 그대로 편다.
					     포인트 표시는 접힘과 무관하게 항상 보이도록 details 밖에 둔다. --%>
					<div class="flex items-start gap-2 mb-2">
						<details class="poe-tools min-w-0 flex-1">
							<summary class="btn btn-sm btn-outline sm:hidden">${MessageUtil.getMessage("poe.tree.tool.toggle")}</summary>
							<div class="flex flex-wrap items-center gap-2 pt-2 sm:pt-0">
						<input id="poeTreeSearch" type="text" class="input input-sm input-bordered w-full sm:w-52" placeholder="${MessageUtil.getMessage("poe.tree.search")}" aria-label="${MessageUtil.getMessage("poe.tree.search")}">
					<span id="poeTreeSearchCount" class="text-xs font-mono text-base-content/60"></span>
					<span id="poeTreeNotice" class="text-xs font-mono text-warning"></span>
						<button id="poeTreeUndo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Z">${MessageUtil.getMessage("poe.tree.tool.undo")}</button>
					<button id="poeTreeRedo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Shift+Z">${MessageUtil.getMessage("poe.tree.tool.redo")}</button>
					<button id="poeTreeReset" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.reset")}</button>
					<input id="poeTreeImport" type="text" class="input input-sm input-bordered w-full sm:w-56" placeholder="${MessageUtil.getMessage("poe.atlas.import.placeholder")}" aria-label="${MessageUtil.getMessage("poe.atlas.import.placeholder")}">
					<button id="poeTreeImportGo" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.import")}</button>
					<button id="poeTreeCopy" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.copy")}</button>
					<button id="poeTreeCopyOfficial" type="button" class="btn btn-sm btn-ghost" title="pathofexile.com">${MessageUtil.getMessage("poe.tree.tool.official")}</button>
						<button id="poeTreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.stats")}</button>
						<button id="poeTreeFullscreen" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.fullscreen")}</button>
							</div>
						</details>
						<span id="poeTreePoints" class="shrink-0 flex items-center gap-2 text-sm text-base-content/70"></span>
					</div>
					<canvas id="poeTreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab" style="background:#0e1116" data-locale="${net.luversof.web.gate.poe.PoeText.isKorean() ? "ko" : "en"}" data-tree-src="${treeSrc}${net.luversof.web.gate.poe.config.PoeDataVersion.q(treeSrc)}" data-sprites-src="${spritesSrc}${net.luversof.web.gate.poe.config.PoeDataVersion.q(spritesSrc)}"></canvas>
					<div id="poeTreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs poe-node-popup overflow-hidden"></div>
					<div id="poeTreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto poe-node-popup">
						<div class="sticky top-0 poe-popup-head text-xs">${MessageUtil.getMessage("poe.tree.stats.total")}</div>
						<div id="poeTreeStatsBody" class="py-1"></div>
					</div>
				</div>
				<script src="/js/poe/tree.js"></script>
			@endif
		</div>
	`
)
