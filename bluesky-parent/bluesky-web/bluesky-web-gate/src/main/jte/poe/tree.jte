@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param boolean hasTreeData = false
@param java.util.List<net.luversof.web.gate.poe.dto.PoeGem> activeGems = java.util.List.of()

!{
	String pageTitle = MessageUtil.getMessage("poe.tree.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String hintLabel = MessageUtil.getMessage("poe.tree.hint");
	String legendKeystoneLabel = MessageUtil.getMessage("poe.tree.legend.keystone");
	String legendNotableLabel = MessageUtil.getMessage("poe.tree.legend.notable");
	String legendJewelLabel = MessageUtil.getMessage("poe.tree.legend.jewel");
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
			</div>


			@if(!hasTreeData)
				@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-tree.mjs")
			@else
				<div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-base-content/50">
					<span>${hintLabel}</span>
					<span class="ml-auto flex items-center gap-3">
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#cf6642"></span> ${legendKeystoneLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#d4a94e"></span> ${legendNotableLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#4ec9d4"></span> ${legendJewelLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#9a6ad4"></span> ${legendMasteryLabel}</span>
					</span>
				</div>
				<div id="poeTreeShell" class="bg-base-100">
				<div class="flex flex-wrap items-center gap-2 mb-2">
					<input id="poeTreeSearch" type="text" class="input input-sm input-bordered w-full sm:w-52" placeholder="${net.luversof.web.gate.poe.PoeText.isKorean() ? "검색 (노드/스탯)" : "Search (node/stat)"}">
					<select id="poeTreeClass" class="select select-sm select-bordered" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "캐릭터 직업" : "Class"}">
						<option value="0">사이온 (Scion)</option>
						<option value="1">머라우더 (Marauder)</option>
						<option value="2">레인저 (Ranger)</option>
						<option value="3">위치 (Witch)</option>
						<option value="4">듀얼리스트 (Duelist)</option>
						<option value="5">템플러 (Templar)</option>
						<option value="6">섀도우 (Shadow)</option>
					</select>
					<select id="poeTreeAscend" class="select select-sm select-bordered" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "전직" : "Ascendancy"}"></select>
					<select id="poeTreeBloodline" class="select select-sm select-bordered" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "혈맹" : "Bloodline"}"></select>
					<button id="poeTreeReset" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "트리 초기화" : "Reset"}</button>
					<button id="poeTreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "스탯 요약" : "Stat summary"}</button>
					<button id="poeTreeEval" type="button" class="btn btn-sm btn-primary btn-outline">${MessageUtil.getMessage("poe.tree.eval")}</button>
					<button id="poeTreeFullscreen" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "전체 화면" : "Fullscreen"}</button>
					<span id="poeTreePoints" class="ml-auto flex items-center gap-2 text-sm text-base-content/70"></span>
				</div>
				<div class="relative">
					<canvas id="poeTreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab" style="background:#0e1116" data-locale="${net.luversof.web.gate.poe.PoeText.isKorean() ? "ko" : "en"}" data-tree-src="/poe-data/passive-tree.json" data-sprites-src="/poe-data/tree-sprites-skill.json"></canvas>
					<div id="poeTreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs rounded shadow-2xl overflow-hidden"></div>
					<div id="poeTreeEvalPanel" class="hidden absolute left-2 top-2 z-10 w-72 max-h-[70%] overflow-y-auto rounded shadow-2xl border border-primary/50 bg-base-300/95">
						<div class="sticky top-0 border-b border-primary/40 bg-base-200 text-center font-bold text-xs px-4 py-1">${MessageUtil.getMessage("poe.tree.eval.title")}</div>
						<div class="border-b border-primary/20 bg-base-200/60 p-2 space-y-1">
							<div class="text-[10px] text-base-content/60 leading-4">${MessageUtil.getMessage("poe.tree.eval.gem.help")}</div>
							<div class="flex items-center gap-1.5">
								<input id="poeTreeEvalGem" list="poeTreeGemList" type="text" class="input input-xs input-bordered flex-1" placeholder="${MessageUtil.getMessage("poe.tree.eval.gem")}">
								<button id="poeTreeEvalRerun" type="button" class="btn btn-xs btn-primary">${MessageUtil.getMessage("poe.tree.eval.rerun")}</button>
							</div>
							<datalist id="poeTreeGemList">
								@for(net.luversof.web.gate.poe.dto.PoeGem gem : activeGems)
									<option data-slug="${gem.slug()}" value="${net.luversof.web.gate.poe.PoeText.name(gem.nameKo(), gem.name())}"></option>
								@endfor
							</datalist>
						</div>
						<div id="poeTreeEvalBody" class="p-2"></div>
					</div>
					<div id="poeTreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto rounded shadow-2xl border border-amber-700/60 bg-stone-900/95">
						<div class="sticky top-0 border-b-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-xs px-4 py-1">${net.luversof.web.gate.poe.PoeText.isKorean() ? "할당 스탯 합계" : "Allocated stats"}</div>
						<div id="poeTreeStatsBody" class="py-1"></div>
					</div>
				</div>
				</div>
				<script src="/js/poe/tree.js"></script>
			@endif
		</div>
	`
)
