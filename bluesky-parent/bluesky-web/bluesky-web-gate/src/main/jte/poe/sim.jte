@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeGem
@import net.luversof.web.gate.poe.dto.PoeUniqueItem

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param List<PoeGem> activeGems = java.util.List.of()
@param List<PoeUniqueItem> uniqueItems = java.util.List.of()
@param String treeNodes = ""
@param String masteries = ""
@param String jewels = ""
@param String clusters = ""
@param String tattoos = ""
@param String anoint = ""
@param java.util.Set<String> preSkills = java.util.Set.of()
@param java.util.Set<String> preUniques = java.util.Set.of()
@param String fixedClassName = ""

!{
	String pageTitle = MessageUtil.getMessage("poe.sim.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String optimizeTitle = MessageUtil.getMessage("poe.sim.opt.title");
	String optimizeDesc = MessageUtil.getMessage("poe.sim.opt.desc");
	String optimizeRun = MessageUtil.getMessage("poe.sim.opt.run");
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

			<!-- 최적 조합 탐색: 젬 + 목표 선택 → 보조젬/트리/아이템 greedy 탐색 -->
			<div class="space-y-3 rounded-2xl bg-base-100 p-4 shadow-sm">
				<h2 class="text-base font-bold">${optimizeTitle}</h2>
				<div class="text-sm text-base-content/60">${optimizeDesc}</div>
				<form class="space-y-3"
				      hx-post="/poe/htmx/sim/optimize"
				      hx-target="#poeOptimizeStatusWrap"
				      hx-swap="outerHTML">
					<!-- 트리 에디터에서 넘어온 확정 트리 — 있으면 트리 탐색을 건너뛰고 이 트리로 최적화 -->
					@if(!treeNodes.isBlank())
						<input type="hidden" name="treeNodes" value="${treeNodes}">
						<input type="hidden" name="masteries" value="${masteries}">
						<input type="hidden" name="jewels" value="${jewels}">
						<input type="hidden" name="clusters" value="${clusters}">
						<input type="hidden" name="tattoos" value="${tattoos}">
						<input type="hidden" name="anoint" value="${anoint}">
						<input type="hidden" name="className" value="${fixedClassName}">
						<div class="flex items-center gap-2 rounded border border-primary/40 bg-primary/5 px-3 py-1.5 text-xs">
							<span class="font-semibold text-primary">${MessageUtil.getMessage("poe.sim.opt.fixedtree")}</span>
							<span class="font-mono text-base-content/60">${treeNodes.split(",").length}</span>
							<!-- 클러스터/주얼/마스터리/문신이 함께 넘어왔는지 눈에 보이게 — 안 보이면 인계가 됐는지 알 수 없다 -->
							@if(!masteries.isBlank())
								<span class="badge badge-sm badge-outline border-sky-400 text-sky-300">${net.luversof.web.gate.poe.PoeText.isKorean() ? "마스터리 " + masteries.split(",").length : "masteries " + masteries.split(",").length}</span>
							@endif
							@if(!clusters.isBlank())
								<span class="badge badge-sm badge-secondary badge-outline">${net.luversof.web.gate.poe.PoeText.isKorean() ? "클러스터 " + clusters.split(",").length : "clusters " + clusters.split(",").length}</span>
							@endif
							@if(!jewels.isBlank())
								<span class="badge badge-sm badge-accent badge-outline">${net.luversof.web.gate.poe.PoeText.isKorean() ? "주얼 " + jewels.split(",").length : "jewels " + jewels.split(",").length}</span>
							@endif
							@if(!tattoos.isBlank())
								<span class="badge badge-sm badge-outline border-purple-400 text-purple-300">${net.luversof.web.gate.poe.PoeText.isKorean() ? "문신 " + tattoos.split("\\|").length : "tattoos " + tattoos.split("\\|").length}</span>
							@endif
							<a class="link link-primary ml-auto" href="/poe/sim">${MessageUtil.getMessage("poe.sim.opt.fixedtree.clear")}</a>
						</div>
					@endif
					<!-- 주 입력(스킬젬·고유템) + 최적화 설정을 한 줄로 — 셋 다 짧아 줄을 나눌 이유가 없다(사용자 요청).
					     좁은 화면에선 flex-wrap 이 알아서 접고, items-end 로 라벨 있는 칸과 컨트롤의 밑선을 맞춘다. -->
					<div class="flex flex-wrap items-end gap-3">
						<div class="min-w-64 flex-1">
							<label class="block text-xs font-semibold text-base-content/70 mb-1">${MessageUtil.getMessage("poe.sim.opt.skills")}</label>
							<select name="skills" multiple data-poe-multi data-placeholder="${MessageUtil.getMessage("poe.sim.opt.skills.placeholder")}" size="4" class="select select-sm select-bordered w-full">
								@for(PoeGem skillOpt : activeGems)
									<option value="${skillOpt.slug()}" selected="${preSkills.contains(skillOpt.slug())}">${net.luversof.web.gate.poe.PoeText.name(skillOpt.nameKo(), skillOpt.name())}</option>
								@endfor
							</select>
						</div>
						<div class="min-w-64 flex-1">
							<label class="block text-xs font-semibold text-base-content/70 mb-1">${MessageUtil.getMessage("poe.sim.opt.uniques")}</label>
							<select name="uniques" multiple data-poe-multi data-placeholder="${MessageUtil.getMessage("poe.sim.opt.uniques.placeholder")}" size="4" class="select select-sm select-bordered w-full">
								@for(PoeUniqueItem uniqueItem : uniqueItems)
									<option value="${uniqueItem.slug()}" selected="${preUniques.contains(uniqueItem.slug())}">${net.luversof.web.gate.poe.PoeText.name(uniqueItem.nameKo(), uniqueItem.name())}</option>
								@endfor
							</select>
						</div>
						<%-- min-w-0: flex 아이템 기본값(min-width:auto)이면 내용 너비를 고집해 좁은 폭에서 가로 스크롤이 생긴다(768px 실측). --%>
						<div class="flex min-w-0 flex-wrap items-center gap-2">
						<%-- 목표(objective) 셀렉트 제거(2026-07-30): poe.ninja 실빌드 성향이 목표축을 자동 결정(위 미리보기). objective=auto → API가 성향으로 dps/ehp/balanced 해석 --%>
						<span class="self-center whitespace-nowrap text-xs text-base-content/60" title="선택한 스킬×전직의 poe.ninja 실빌드 성향으로 목표를 자동 결정">${net.luversof.web.gate.poe.PoeText.isKorean() ? "목표: 실빌드 성향 자동" : "Objective: auto (real-build lean)"}</span>
						<select name="scenario" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.scenario")}">
							<option value="None">${MessageUtil.getMessage("poe.sim.opt.scenario.none")}</option>
							<option value="Boss">${MessageUtil.getMessage("poe.sim.opt.scenario.boss")}</option>
							<option value="Pinnacle" selected>${MessageUtil.getMessage("poe.sim.opt.scenario.pinnacle")}</option>
							<option value="Uber">${MessageUtil.getMessage("poe.sim.opt.scenario.uber")}</option>
						</select>
						<select name="ascendancy" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.ascendancy")}">
							<option value="">${MessageUtil.getMessage("poe.sim.opt.ascendancy.auto")}</option>
							<optgroup label="${net.luversof.web.gate.poe.PoeText.name("사이온", "Scion")}">
								<option value="Ascendant">${net.luversof.web.gate.poe.PoeText.name("어센던트", "Ascendant")}</option>
								<option value="Reliquarian">${net.luversof.web.gate.poe.PoeText.name("렐리쿼리언", "Reliquarian")}</option>
							</optgroup>
							<optgroup label="${net.luversof.web.gate.poe.PoeText.name("머라우더", "Marauder")}">
								<option value="Juggernaut">${net.luversof.web.gate.poe.PoeText.name("저거넛", "Juggernaut")}</option>
								<option value="Berserker">${net.luversof.web.gate.poe.PoeText.name("버서커", "Berserker")}</option>
								<option value="Chieftain">${net.luversof.web.gate.poe.PoeText.name("치프틴", "Chieftain")}</option>
							</optgroup>
							<optgroup label="${net.luversof.web.gate.poe.PoeText.name("레인저", "Ranger")}">
								<option value="Warden">${net.luversof.web.gate.poe.PoeText.name("워든", "Warden")}</option>
								<option value="Deadeye">${net.luversof.web.gate.poe.PoeText.name("데드아이", "Deadeye")}</option>
								<option value="Pathfinder">${net.luversof.web.gate.poe.PoeText.name("패스파인더", "Pathfinder")}</option>
							</optgroup>
							<optgroup label="${net.luversof.web.gate.poe.PoeText.name("위치", "Witch")}">
								<option value="Occultist">${net.luversof.web.gate.poe.PoeText.name("오컬티스트", "Occultist")}</option>
								<option value="Elementalist">${net.luversof.web.gate.poe.PoeText.name("엘리멘탈리스트", "Elementalist")}</option>
								<option value="Necromancer">${net.luversof.web.gate.poe.PoeText.name("네크로맨서", "Necromancer")}</option>
							</optgroup>
							<optgroup label="${net.luversof.web.gate.poe.PoeText.name("듀얼리스트", "Duelist")}">
								<option value="Slayer">${net.luversof.web.gate.poe.PoeText.name("슬레이어", "Slayer")}</option>
								<option value="Gladiator">${net.luversof.web.gate.poe.PoeText.name("글래디에이터", "Gladiator")}</option>
								<option value="Champion">${net.luversof.web.gate.poe.PoeText.name("챔피언", "Champion")}</option>
							</optgroup>
							<optgroup label="${net.luversof.web.gate.poe.PoeText.name("템플러", "Templar")}">
								<option value="Inquisitor">${net.luversof.web.gate.poe.PoeText.name("인퀴지터", "Inquisitor")}</option>
								<option value="Hierophant">${net.luversof.web.gate.poe.PoeText.name("하이로펀트", "Hierophant")}</option>
								<option value="Guardian">${net.luversof.web.gate.poe.PoeText.name("가디언", "Guardian")}</option>
							</optgroup>
							<optgroup label="${net.luversof.web.gate.poe.PoeText.name("섀도우", "Shadow")}">
								<option value="Assassin">${net.luversof.web.gate.poe.PoeText.name("어쌔신", "Assassin")}</option>
								<option value="Trickster">${net.luversof.web.gate.poe.PoeText.name("트릭스터", "Trickster")}</option>
								<option value="Saboteur">${net.luversof.web.gate.poe.PoeText.name("사보추어", "Saboteur")}</option>
							</optgroup>
						</select>
						<label class="flex items-center gap-1.5 text-sm cursor-pointer" title="${MessageUtil.getMessage("poe.sim.opt.buffs.hint")}">
							<input type="checkbox" name="buffs" value="true" checked class="checkbox checkbox-sm">
							<span>${MessageUtil.getMessage("poe.sim.opt.buffs")}</span>
						</label>
						@if(isAuthenticated)
							<button type="submit" class="btn btn-primary btn-sm">${optimizeRun}</button>
						@else
							<!-- 비로그인 시 실행 버튼을 살려두면 눌러도 아무 일이 없어(서버가 조용히 무시) 오해를 부른다 -->
							<button type="button" class="btn btn-primary btn-sm btn-disabled" disabled>${optimizeRun}</button>
							<a class="link link-primary text-xs" href="${loginUrl}">${MessageUtil.getMessage("poe.sim.opt.login")}</a>
						@endif
						</div>
					</div>
					<%-- 실시간 성향 미리보기 — 선택한 스킬×전직의 poe.ninja 실빌드 성향/프로파일(실행 전). 스킬/전직 변경 시 갱신. --%>
					<div id="poeArchetypeHint"
					     hx-get="/poe/htmx/sim/archetype"
					     hx-trigger="load, change from:select[name='skills'], change from:select[name='ascendancy']"
					     hx-include="select[name='skills'], select[name='ascendancy']"
					     hx-target="this"
					     hx-swap="innerHTML"></div>
				</form>
				@template.poe.htmx.simOptimizeWrap()
				<%-- ⚠ 순서: 목록이 **위**, 뷰어가 **아래**. 결과가 5천 px 를 넘길 때가 있어 뷰어를 위로 올리면
				     목록이 그 아래로 파묻힌다(실측 y=5545). 대신 결과를 띄운 뒤 **목록을 화면 맨 위에 고정**해
				     바로 다른 항목을 고를 수 있게 한다(예전엔 뷰어를 스크롤해 목록이 화면 위로 사라졌다: y=-182). --%>
				<!-- 최근 결과 이력 — 로드 시 채우고, 새 최적화 완료 시 상태 fragment 가 OOB 로 갱신 -->
				<div id="poeOptimizeHistory" class="mt-3" hx-get="/poe/htmx/sim/optimize/history" hx-trigger="load" hx-swap="innerHTML"></div>
				<%-- 지난 결과 뷰어 — 폴링 래퍼(#poeOptimizeStatusWrap)와 분리되어, 최적화 실행 중에도 이전 결과를 계속 볼 수 있다
				     (예전엔 히스토리 결과를 폴링 래퍼에 넣어 3초 뒤 로딩화면으로 되돌아갔다). 로드되면 자동 표시. --%>
				<%-- 보고 있는 히스토리 항목 활성 표시 — border-primary 는 컴파일된 유틸에 존재, 배경은 color-mix 로 테마 무관 --%>
				<style>#poeOptimizeHistory button.poe-hist-active{border-color:var(--color-primary);background-color:color-mix(in oklch, var(--color-primary) 12%, transparent);font-weight:600}</style>
				<div id="poeHistoryResultViewWrap" class="mt-3 hidden">
					<div class="flex items-center justify-between mb-1.5">
						<div class="text-xs font-semibold text-base-content/60">${net.luversof.web.gate.poe.PoeText.isKorean() ? "지난 결과 보기" : "Saved result"}</div>
						<button type="button" class="btn btn-ghost btn-xs"
						        onclick="var v=document.getElementById('poeHistoryResultView'); if(v){v.innerHTML='';} this.closest('#poeHistoryResultViewWrap').classList.add('hidden'); document.querySelectorAll('#poeOptimizeHistory button.poe-hist-active').forEach(function(b){b.classList.remove('poe-hist-active');});">${net.luversof.web.gate.poe.PoeText.isKorean() ? "닫기 ✕" : "Close ✕"}</button>
					</div>
					<div id="poeHistoryResultView"
					     hx-on::after-swap="var w=document.getElementById('poeHistoryResultViewWrap'); if(this.innerHTML.trim()){w.classList.remove('hidden'); var h=document.getElementById('poeOptimizeHistory'); if(h){h.scrollIntoView({block:'start'}); setTimeout(function(){h.scrollIntoView({block:'start'});},350);}}"></div>
				</div>
			</div>

			<!-- 젬 DPS 랭킹 — 페이지 제목이 시뮬레이터로 바뀌어 이 섹션에 자체 제목이 필요하다 -->
			<div class="space-y-3 rounded-2xl bg-base-100 p-4 shadow-sm">
				<h2 class="text-base font-bold">${MessageUtil.getMessage("poe.sim.ranking.title")}</h2>
				<!-- 배치 실행/상태 (고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단. 완료 시 랭킹을 새로 불러온다) -->
				@template.poe.htmx.simWrap()
				<div id="poeSimRanking"></div>
			</div>

			<!-- 젬 상세 레이어 컨테이너 (htmx 로 채워짐) -->
			<div id="poeGemDetail"></div>

			<!-- 추가 스킬/유니크 멀티셀렉트를 공간절약형 드롭다운+검색으로 향상 -->
			<script src="/js/poe/multiSelect.js"></script>
		</div>
	`
)
