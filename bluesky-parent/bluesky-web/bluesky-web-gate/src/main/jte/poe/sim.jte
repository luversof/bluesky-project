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
					<!-- 주 입력: 스킬젬 · 고유템 (둘 다 선택사항, 여러 개 가능). 선택된 것을 기준으로 최적 조합 계산 -->
					<div class="flex flex-wrap gap-3">
						<div class="w-full sm:w-96">
							<label class="block text-xs font-semibold text-base-content/70 mb-1">${MessageUtil.getMessage("poe.sim.opt.skills")}</label>
							<select name="skills" multiple data-poe-multi data-placeholder="${MessageUtil.getMessage("poe.sim.opt.skills.placeholder")}" size="4" class="select select-sm select-bordered w-full sm:w-96">
								@for(PoeGem skillOpt : activeGems)
									<option value="${skillOpt.slug()}">${net.luversof.web.gate.poe.PoeText.name(skillOpt.nameKo(), skillOpt.name())}</option>
								@endfor
							</select>
						</div>
						<div class="w-full sm:w-96">
							<label class="block text-xs font-semibold text-base-content/70 mb-1">${MessageUtil.getMessage("poe.sim.opt.uniques")}</label>
							<select name="uniques" multiple data-poe-multi data-placeholder="${MessageUtil.getMessage("poe.sim.opt.uniques.placeholder")}" size="4" class="select select-sm select-bordered w-full sm:w-96">
								@for(PoeUniqueItem uniqueItem : uniqueItems)
									<option value="${uniqueItem.slug()}">${net.luversof.web.gate.poe.PoeText.name(uniqueItem.nameKo(), uniqueItem.name())}</option>
								@endfor
							</select>
						</div>
					</div>
					<!-- 최적화 설정 -->
					<div class="flex flex-wrap items-center gap-2">
						<!-- 기본은 균형 — DPS 전용은 생명력 1·저항 미달 유리대포가 나와 실전에 못 쓴다(실측) -->
						<select name="objective" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.objective")}">
							<option value="balanced" selected>${MessageUtil.getMessage("poe.sim.opt.objective.balanced")}</option>
							<option value="dps">${MessageUtil.getMessage("poe.sim.opt.objective.dps")}</option>
							<option value="ehp">${MessageUtil.getMessage("poe.sim.opt.objective.ehp")}</option>
						</select>
						<select name="scenario" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.scenario")}">
							<option value="None">${MessageUtil.getMessage("poe.sim.opt.scenario.none")}</option>
							<option value="Boss">${MessageUtil.getMessage("poe.sim.opt.scenario.boss")}</option>
							<option value="Pinnacle" selected>${MessageUtil.getMessage("poe.sim.opt.scenario.pinnacle")}</option>
							<option value="Uber">${MessageUtil.getMessage("poe.sim.opt.scenario.uber")}</option>
						</select>
						<select name="ascendancy" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.ascendancy")}">
							<option value="">${MessageUtil.getMessage("poe.sim.opt.ascendancy.auto")}</option>
							<optgroup label="사이온 (Scion)">
								<option value="Ascendant">어센던트 (Ascendant)</option>
								<option value="Reliquarian">렐리쿼리언 (Reliquarian)</option>
							</optgroup>
							<optgroup label="머라우더 (Marauder)">
								<option value="Juggernaut">저거넛 (Juggernaut)</option>
								<option value="Berserker">버서커 (Berserker)</option>
								<option value="Chieftain">치프틴 (Chieftain)</option>
							</optgroup>
							<optgroup label="레인저 (Ranger)">
								<option value="Warden">워든 (Warden)</option>
								<option value="Deadeye">데드아이 (Deadeye)</option>
								<option value="Pathfinder">패스파인더 (Pathfinder)</option>
							</optgroup>
							<optgroup label="위치 (Witch)">
								<option value="Occultist">오컬티스트 (Occultist)</option>
								<option value="Elementalist">엘리멘탈리스트 (Elementalist)</option>
								<option value="Necromancer">네크로맨서 (Necromancer)</option>
							</optgroup>
							<optgroup label="듀얼리스트 (Duelist)">
								<option value="Slayer">슬레이어 (Slayer)</option>
								<option value="Gladiator">글래디에이터 (Gladiator)</option>
								<option value="Champion">챔피언 (Champion)</option>
							</optgroup>
							<optgroup label="템플러 (Templar)">
								<option value="Inquisitor">인퀴지터 (Inquisitor)</option>
								<option value="Hierophant">하이로펀트 (Hierophant)</option>
								<option value="Guardian">가디언 (Guardian)</option>
							</optgroup>
							<optgroup label="섀도우 (Shadow)">
								<option value="Assassin">어쌔신 (Assassin)</option>
								<option value="Trickster">트릭스터 (Trickster)</option>
								<option value="Saboteur">사보추어 (Saboteur)</option>
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
				</form>
				@template.poe.htmx.simOptimizeWrap()
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
