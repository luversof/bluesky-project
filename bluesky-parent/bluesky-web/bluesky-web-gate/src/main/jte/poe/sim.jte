@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeGem

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param List<PoeGem> activeGems = java.util.List.of()

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
				<form class="flex flex-wrap items-center gap-2"
				      hx-post="/poe/htmx/sim/optimize"
				      hx-target="#poeOptimizeStatusWrap"
				      hx-swap="outerHTML">
					<select name="slug" class="select select-sm select-bordered w-full sm:w-72" required>
						@for(PoeGem activeGem : activeGems)
							<option value="${activeGem.slug()}">${net.luversof.web.gate.poe.PoeText.name(activeGem.nameKo(), activeGem.name())} (${activeGem.name()})</option>
						@endfor
					</select>
					<select name="objective" class="select select-sm select-bordered">
						<option value="dps">${MessageUtil.getMessage("poe.sim.opt.objective.dps")}</option>
						<option value="balanced">${MessageUtil.getMessage("poe.sim.opt.objective.balanced")}</option>
						<option value="ehp">${MessageUtil.getMessage("poe.sim.opt.objective.ehp")}</option>
					</select>
					<select name="scenario" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.scenario")}">
						<option value="None">${MessageUtil.getMessage("poe.sim.opt.scenario.none")}</option>
						<option value="Boss">${MessageUtil.getMessage("poe.sim.opt.scenario.boss")}</option>
						<option value="Pinnacle" selected>${MessageUtil.getMessage("poe.sim.opt.scenario.pinnacle")}</option>
						<option value="Uber">${MessageUtil.getMessage("poe.sim.opt.scenario.uber")}</option>
					</select>
					<select name="className" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.class")}">
						<option value="">${MessageUtil.getMessage("poe.sim.opt.class.auto")}</option>
						<option value="Marauder">머라우더 (Marauder)</option>
						<option value="Ranger">레인저 (Ranger)</option>
						<option value="Witch">위치 (Witch)</option>
						<option value="Duelist">듀얼리스트 (Duelist)</option>
						<option value="Templar">템플러 (Templar)</option>
						<option value="Shadow">섀도우 (Shadow)</option>
						<option value="Scion">사이온 (Scion)</option>
					</select>
					<label class="flex items-center gap-1.5 text-sm cursor-pointer" title="${MessageUtil.getMessage("poe.sim.opt.buffs.hint")}">
						<input type="checkbox" name="buffs" value="true" checked class="checkbox checkbox-sm">
						<span>${MessageUtil.getMessage("poe.sim.opt.buffs")}</span>
					</label>
					<button type="submit" class="btn btn-primary btn-sm">${optimizeRun}</button>
				</form>
				@template.poe.htmx.simOptimizeWrap()
			</div>

			<!-- 배치 실행/상태 (고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단. 완료 시 랭킹을 새로 불러온다) -->
			@template.poe.htmx.simWrap()

			<div id="poeSimRanking"></div>

			<!-- 젬 상세 레이어 컨테이너 (htmx 로 채워짐) -->
			<div id="poeGemDetail"></div>
		</div>
	`
)
