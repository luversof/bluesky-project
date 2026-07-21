@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param boolean hasAtlasData = false

!{
	String pageTitle = MessageUtil.getMessage("poe.atlas.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String hintLabel = MessageUtil.getMessage("poe.tree.hint");
	String legendKeystoneLabel = MessageUtil.getMessage("poe.tree.legend.keystone");
	String legendNotableLabel = MessageUtil.getMessage("poe.tree.legend.notable");
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

			@if(!hasAtlasData)
				@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-atlas-tree.mjs && node tree-sprites.mjs")
			@else
				<div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-base-content/50">
					<span>${hintLabel}</span>
					<span class="ml-auto flex items-center gap-3">
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#cf6642"></span> ${legendKeystoneLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#d4a94e"></span> ${legendNotableLabel}</span>
					</span>
				</div>
				<div id="poeTreeShell" class="relative bg-base-100">
					<div class="flex flex-wrap items-center gap-2 mb-2">
						<input id="poeTreeSearch" type="text" class="input input-sm input-bordered w-full sm:w-52" placeholder="${net.luversof.web.gate.poe.PoeText.isKorean() ? "검색 (노드/스탯)" : "Search (node/stat)"}">
						<button id="poeTreeReset" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "초기화" : "Reset"}</button>
						<button id="poeTreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "스탯 요약" : "Stat summary"}</button>
						<button id="poeTreeFullscreen" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "전체 화면" : "Fullscreen"}</button>
						<span id="poeTreePoints" class="ml-auto flex items-center gap-2 text-sm text-base-content/70"></span>
					</div>
					<canvas id="poeTreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab" style="background:#0e1116" data-locale="${net.luversof.web.gate.poe.PoeText.isKorean() ? "ko" : "en"}" data-tree-src="/poe-data/atlas-tree.json" data-sprites-src="/poe-data/tree-sprites-atlas.json"></canvas>
					<div id="poeTreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs rounded shadow-2xl overflow-hidden"></div>
					<div id="poeTreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto rounded shadow-2xl border border-amber-700/60 bg-stone-900/95">
						<div class="sticky top-0 border-b-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-xs px-4 py-1">${net.luversof.web.gate.poe.PoeText.isKorean() ? "할당 스탯 합계" : "Allocated stats"}</div>
						<div id="poeTreeStatsBody" class="py-1"></div>
					</div>
				</div>
				<script src="/js/poe/tree.js"></script>
			@endif
		</div>
	`
)
