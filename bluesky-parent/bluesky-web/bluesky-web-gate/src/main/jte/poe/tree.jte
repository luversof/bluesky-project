@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param boolean hasTreeData = false
@param java.util.List<net.luversof.web.gate.poe.dto.PoeGem> activeGems = java.util.List.of()
@param java.util.List<net.luversof.web.gate.poe.dto.PoeUniqueItem> jewelUniques = java.util.List.of()
@param java.util.List<String> treeVersions = java.util.List.of()
@param String activeVer = ""
@param String activePatch = ""
@param String treeSrc = "/poe-data/passive-tree.json"
@param String spritesSrc = "/poe-data/tree-sprites-skill.json"

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
				@if(!treeVersions.isEmpty())
					<%-- 버전별 아카이브 조회 — 빈 값=현행(최신) 데이터 --%>
					<select class="select select-xs select-bordered" onchange="var u=new URL(location.href); if(this.value) u.searchParams.set('ver', this.value); else u.searchParams.delete('ver'); location.href=u.toString();">
						<option value="">${net.luversof.web.gate.poe.PoeText.isKorean() ? "최신" : "Latest"}</option>
						@for(String v : treeVersions)
							<option value="${v}" selected="${v.equals(activeVer)}">${v}</option>
						@endfor
					</select>
				@endif
				@if(!activeVer.isEmpty())
					<span class="badge badge-warning badge-sm">${net.luversof.web.gate.poe.PoeText.isKorean() ? "아카이브 보기" : "Archive view"}@if(!activePatch.isEmpty()) · ${activePatch}@endif — ${net.luversof.web.gate.poe.PoeText.isKorean() ? "계산·최적화는 최신 기준" : "calc uses latest"}</span>
				@endif
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
					<span id="poeTreeSearchCount" class="text-xs font-mono text-base-content/50"></span>
					<span id="poeTreeNotice" class="text-xs font-mono text-warning"></span>
					<select id="poeTreeClass" class="select select-sm select-bordered" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "캐릭터 직업" : "Class"}">
						<option value="0">${net.luversof.web.gate.poe.PoeText.name("사이온", "Scion")}</option>
						<option value="1">${net.luversof.web.gate.poe.PoeText.name("머라우더", "Marauder")}</option>
						<option value="2">${net.luversof.web.gate.poe.PoeText.name("레인저", "Ranger")}</option>
						<option value="3">${net.luversof.web.gate.poe.PoeText.name("위치", "Witch")}</option>
						<option value="4">${net.luversof.web.gate.poe.PoeText.name("듀얼리스트", "Duelist")}</option>
						<option value="5">${net.luversof.web.gate.poe.PoeText.name("템플러", "Templar")}</option>
						<option value="6">${net.luversof.web.gate.poe.PoeText.name("섀도우", "Shadow")}</option>
					</select>
					<select id="poeTreeAscend" class="select select-sm select-bordered" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "전직" : "Ascendancy"}"></select>
					<select id="poeTreeBloodline" class="select select-sm select-bordered" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "혈맹" : "Bloodline"}"></select>
					<button id="poeTreeUndo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Z">${net.luversof.web.gate.poe.PoeText.isKorean() ? "실행취소" : "Undo"}</button>
					<button id="poeTreeRedo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Shift+Z">${net.luversof.web.gate.poe.PoeText.isKorean() ? "다시실행" : "Redo"}</button>
					<button id="poeTreeReset" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "트리 초기화" : "Reset"}</button>
					<button id="poeTreeAnoint" type="button" class="btn btn-sm btn-ghost" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "아뮬렛 도유로 노터블 부여" : "Anoint a notable"}">${net.luversof.web.gate.poe.PoeText.isKorean() ? "도유" : "Anoint"}</button>
					<button id="poeTreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "스탯 요약" : "Stat summary"}</button>
					<input id="poeTreeImport" type="text" class="input input-sm input-bordered w-full sm:w-56" placeholder="${net.luversof.web.gate.poe.PoeText.isKorean() ? "공홈 트리 링크 붙여넣기" : "Paste official tree link"}">
					<button id="poeTreeImportGo" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "가져오기" : "Import"}</button>
					<button id="poeTreeCopy" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "링크 복사" : "Copy link"}</button>
					<button id="poeTreeCopyOfficial" type="button" class="btn btn-sm btn-ghost" title="pathofexile.com">${net.luversof.web.gate.poe.PoeText.isKorean() ? "공홈 링크" : "Official link"}</button>
					<button id="poeTreeEval" type="button" class="btn btn-sm btn-primary btn-outline">${MessageUtil.getMessage("poe.tree.eval")}</button>
					<button id="poeTreeToSim" type="button" class="btn btn-sm btn-secondary btn-outline">${MessageUtil.getMessage("poe.tree.tosim")}</button>
					<button id="poeTreeFullscreen" type="button" class="btn btn-sm btn-ghost">${net.luversof.web.gate.poe.PoeText.isKorean() ? "전체 화면" : "Fullscreen"}</button>
					<span id="poeTreePoints" class="ml-auto flex items-center gap-2 text-sm text-base-content/70"></span>
				</div>
				<div class="relative">
					<canvas id="poeTreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab" style="background:#0e1116" data-locale="${net.luversof.web.gate.poe.PoeText.isKorean() ? "ko" : "en"}" data-tree-src="${treeSrc}" data-sprites-src="${spritesSrc}"></canvas>
					<div id="poeTreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs poe-node-popup overflow-hidden"></div>
					<div id="poeTreeEvalPanel" class="hidden absolute left-2 top-2 z-10 w-72 max-h-[70%] overflow-y-auto rounded shadow-2xl border border-primary/50 bg-base-300/95">
						<div class="sticky top-0 border-b border-primary/40 bg-base-200 text-xs px-4 py-1 flex items-center">
							<span class="flex-1 text-center font-bold">${MessageUtil.getMessage("poe.tree.eval.title")}</span>
							<button id="poeTreeEvalClose" type="button" class="shrink-0 px-1 text-base-content/60 hover:text-base-content" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "닫기" : "Close"}">✕</button>
						</div>
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
						<!-- 주얼 슬롯 우클릭 메뉴에서 쓰는 유니크 주얼 목록(이름/설명을 data 속성으로 실어 보낸다) -->
						<datalist id="poeTreeJewelList">
							@for(net.luversof.web.gate.poe.dto.PoeUniqueItem jewel : jewelUniques)
								<option data-slug="${jewel.slug()}" data-radius="${jewel.radius() == null ? "" : jewel.radius()}" data-mods="${String.join(" / ", net.luversof.web.gate.poe.PoeText.isKorean() && jewel.explicitsKo() != null && !jewel.explicitsKo().isEmpty() ? jewel.explicitsKo() : jewel.explicits())}" value="${net.luversof.web.gate.poe.PoeText.name(jewel.nameKo(), jewel.name())}"></option>
							@endfor
						</datalist>
						<div id="poeTreeEvalBody" class="p-2"></div>
					</div>
					<div id="poeTreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto poe-node-popup">
						<div class="sticky top-0 poe-popup-head text-xs">${net.luversof.web.gate.poe.PoeText.isKorean() ? "할당 스탯 합계" : "Allocated stats"}</div>
						<div id="poeTreeStatsBody" class="py-1"></div>
					</div>
				</div>
				</div>
				<script src="/js/poe/tree.js"></script>
			@endif
		</div>
	`
)
