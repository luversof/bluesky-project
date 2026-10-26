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


			@if(!hasTreeData)
				@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-tree.mjs")
			@else
				<div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-base-content/60">
					<span>${hintLabel}</span>
					<span class="ml-auto flex flex-wrap items-center gap-3">
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#cf6642"></span> ${legendKeystoneLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#d4a94e"></span> ${legendNotableLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#4ec9d4"></span> ${legendJewelLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#9a6ad4"></span> ${legendMasteryLabel}</span>
					</span>
				</div>
				<div id="poeTreeShell" class="bg-base-100">
				<%-- 좁은 화면에서는 이 툴바가 7줄로 쌓여 화면의 2/3 를 먹고 트리가 하단 320px 에 갇혔다
				     (실측 390x820: 캔버스 top=548 · h=320). 모바일에서만 접고 데스크톱은 그대로 편다.
				     포인트 표시는 접힘과 무관하게 항상 보이도록 details 밖에 둔다. --%>
				<div class="flex items-start gap-2 mb-2">
					<details class="poe-tools min-w-0 flex-1">
						<summary class="btn btn-sm btn-outline sm:hidden">${MessageUtil.getMessage("poe.tree.tool.toggle")}</summary>
						<div class="flex flex-wrap items-center gap-2 pt-2 sm:pt-0">
					<input id="poeTreeSearch" type="text" title="${MessageUtil.getMessage("poe.tree.search.title")}" class="input input-sm input-bordered w-full sm:w-52" placeholder="${MessageUtil.getMessage("poe.tree.search")}" aria-label="${MessageUtil.getMessage("poe.tree.search")}">
					<span id="poeTreeSearchCount" class="text-xs font-mono text-base-content/60" aria-live="polite"></span>
					<span id="poeTreeNotice" class="text-xs font-mono text-warning"></span>
					<select id="poeTreeClass" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.tree.class")}">
						<option value="0">${net.luversof.web.gate.poe.PoeText.name("사이온", "Scion")}</option>
						<option value="1">${net.luversof.web.gate.poe.PoeText.name("머라우더", "Marauder")}</option>
						<option value="2">${net.luversof.web.gate.poe.PoeText.name("레인저", "Ranger")}</option>
						<option value="3">${net.luversof.web.gate.poe.PoeText.name("위치", "Witch")}</option>
						<option value="4">${net.luversof.web.gate.poe.PoeText.name("듀얼리스트", "Duelist")}</option>
						<option value="5">${net.luversof.web.gate.poe.PoeText.name("템플러", "Templar")}</option>
						<option value="6">${net.luversof.web.gate.poe.PoeText.name("쉐도우", "Shadow")}</option>
					</select>
					<select id="poeTreeAscend" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.tree.ascendancy")}"></select>
					<select id="poeTreeBloodline" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.tree.bloodline")}"></select>
					<button id="poeTreeUndo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Z">${MessageUtil.getMessage("poe.tree.tool.undo")}</button>
					<button id="poeTreeRedo" type="button" class="btn btn-sm btn-ghost" disabled title="Ctrl+Shift+Z">${MessageUtil.getMessage("poe.tree.tool.redo")}</button>
					<button id="poeTreeReset" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.reset")}</button>
					<button id="poeTreeAnoint" type="button" class="btn btn-sm btn-ghost" title="${MessageUtil.getMessage("poe.tree.anoint.title")}">${MessageUtil.getMessage("poe.tree.anoint")}</button>
					<button id="poeTreeStatsToggle" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.stats")}</button>
					<input id="poeTreeImport" type="text" class="input input-sm input-bordered w-full sm:w-56" placeholder="${MessageUtil.getMessage("poe.tree.import.placeholder")}" aria-label="${MessageUtil.getMessage("poe.tree.import.placeholder")}">
					<button id="poeTreeImportGo" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.import")}</button>
					<button id="poeTreeCopy" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.copy")}</button>
					<button id="poeTreeCopyOfficial" type="button" class="btn btn-sm btn-ghost" title="pathofexile.com">${MessageUtil.getMessage("poe.tree.tool.official")}</button>
					<button id="poeTreeEval" type="button" class="btn btn-sm btn-primary btn-outline">${MessageUtil.getMessage("poe.tree.eval")}</button>
					<button id="poeTreeToSim" type="button" class="btn btn-sm btn-secondary btn-outline">${MessageUtil.getMessage("poe.tree.tosim")}</button>
					<button id="poeTreeFullscreen" type="button" class="btn btn-sm btn-ghost">${MessageUtil.getMessage("poe.tree.tool.fullscreen")}</button>
						</div>
					</details>
					<span id="poeTreePoints" class="flex flex-wrap min-w-0 items-center gap-2 text-sm text-base-content/70"></span>
				</div>
				<div class="relative">
					<canvas id="poeTreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab" style="background:#0e1116" data-locale="${net.luversof.web.gate.poe.PoeText.isKorean() ? "ko" : "en"}" data-tree-src="${treeSrc}${net.luversof.web.gate.poe.config.PoeDataVersion.q(treeSrc)}" data-sprites-src="${spritesSrc}${net.luversof.web.gate.poe.config.PoeDataVersion.q(spritesSrc)}"></canvas>
					<div id="poeTreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs poe-node-popup overflow-hidden"></div>
					<div id="poeTreeEvalPanel" class="hidden absolute left-2 top-2 z-10 w-72 max-h-[70%] overflow-y-auto rounded shadow-2xl border border-primary/50 bg-base-300/95">
						<div class="sticky top-0 border-b border-primary/40 bg-base-200 text-xs px-4 py-1 flex items-center">
							<span class="flex-1 text-center font-bold">${MessageUtil.getMessage("poe.tree.eval.title")}</span>
							<button id="poeTreeEvalClose" type="button" class="shrink-0 px-1 text-base-content/60 hover:text-base-content" title="${MessageUtil.getMessage("poe.tree.tool.close")}">✕</button>
						</div>
						<div class="border-b border-primary/20 bg-base-200/60 p-2 space-y-1">
							<div class="text-[10px] text-base-content/60 leading-4">${MessageUtil.getMessage("poe.tree.eval.gem.help")}</div>
							<div class="flex items-center gap-1.5">
								<input id="poeTreeEvalGem" list="poeTreeGemList" type="text" class="input input-xs input-bordered flex-1" placeholder="${MessageUtil.getMessage("poe.tree.eval.gem")}" aria-label="${MessageUtil.getMessage("poe.tree.eval.gem")}">
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
								<option data-slug="${jewel.slug()}" data-radius="${jewel.radius() == null ? "" : jewel.radius()}" data-en="${jewel.explicits() == null ? "" : String.join("\n", jewel.explicits())}" data-rem="${net.luversof.web.gate.poe.PoeText.reminderData(jewel.explicitsRemindersKo(), jewel.explicitsReminders())}" data-mods="${String.join(" / ", java.util.Objects.requireNonNullElse(net.luversof.web.gate.poe.PoeText.isKorean() && jewel.explicitsKo() != null && !jewel.explicitsKo().isEmpty() ? jewel.explicitsKo() : jewel.explicits(), java.util.List.<String>of()))}" value="${net.luversof.web.gate.poe.PoeText.name(jewel.nameKo(), jewel.name())}"></option>
							@endfor
						</datalist>
						<%-- 주얼 변형(불가능한 탈출 핵심 · 희망의 실 고리 · 분열된 인격 …) — 주얼 장착 창의 둘째 단계(10-03 C75, PoE2 C74 의 짝) --%>
						<div id="poeTreeJewelVariants" hidden>@for(net.luversof.web.gate.poe.dto.PoeUniqueItem jewel : jewelUniques)@if(jewel.variants() != null && jewel.variants().size() > 1)@for(net.luversof.web.gate.poe.dto.PoeUniqueVariant jv : jewel.variants())<span data-slug="${jewel.slug()}" data-index="${jv.index()}" data-name="${net.luversof.web.gate.poe.PoeText.name(jv.nameKo(), jv.name())}" data-mods="${String.join(" / ", java.util.Objects.requireNonNullElse(net.luversof.web.gate.poe.PoeText.isKorean() && jv.explicitsKo() != null && !jv.explicitsKo().isEmpty() ? jv.explicitsKo() : jv.explicits(), java.util.List.<String>of()))}" data-en="${jv.explicits() == null ? "" : String.join("\n", jv.explicits())}" data-rem="${net.luversof.web.gate.poe.PoeText.reminderData(jv.explicitsRemindersKo(), jv.explicitsReminders())}"></span>@endfor@endif@endfor</div>
						<div id="poeTreeEvalBody" class="p-2"></div>
					</div>
					<div id="poeTreeStats" class="hidden absolute right-2 top-2 z-10 w-72 max-h-[60%] overflow-y-auto poe-node-popup">
						<div class="sticky top-0 poe-popup-head text-xs">${MessageUtil.getMessage("poe.tree.stats.total")}</div>
						<div id="poeTreeStatsBody" class="py-1"></div>
					</div>
				</div>
				</div>
				<script src="/js/poe/tree.js"></script>
			@endif
		</div>
	`
)
