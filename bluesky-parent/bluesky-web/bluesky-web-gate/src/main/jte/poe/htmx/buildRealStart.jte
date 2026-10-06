@import net.luversof.web.gate.poe.dto.PoeBuild
@import net.luversof.web.gate.poe.dto.RealStart

@param PoeBuild build = null
@param boolean importError = false
@param boolean engineAvailable = false
@param RealStart realStart = null
@param String oobCode = null

<%-- 빌드 화면 "실빌드에서 출발"(realStart) · 다듬은 빌드 불러오기(oobCode) — 출처 안내 + 코드 칸 채우기(OOB: 재계산·가이드 버튼이 이 칸을 보낸다)
     + 자동 다듬기 버튼 + 일반 요약 fragment. --%>
!{
	boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
	String fillCode = realStart != null ? realStart.code() : oobCode;
}
@if(build != null && fillCode != null)
	@if(realStart != null)
		<div class="alert mb-3 flex flex-col items-start gap-2 text-sm" data-real-start-note>
			<span>${ko
				? "poe.ninja 실빌드에서 출발" + (realStart.level() != null ? " (Lv" + realStart.level() + ")" : "") + " — " + realStart.ascendancy() + " · " + realStart.mainSkill() + ", 실빌드 " + realStart.sample() + "명 중 DPS·EHP 중앙값에 가장 가까운 빌드입니다. 시뮬레이터와 같은 가정(보스·충전·전투 버프)에 그 빌드의 운용 설정(공명·낙인 부착 등)을 더해 불러왔습니다. 아래 업그레이드 가이드로 다듬거나, 자동으로 다듬어 보세요."
				: "Starting from a poe.ninja real build" + (realStart.level() != null ? " (Lv" + realStart.level() + ")" : "") + " — " + realStart.ascendancy() + " · " + realStart.mainSkill() + ", closest to the median of " + realStart.sample() + " real builds, loaded under the simulator's assumptions plus the build's own settings. Refine it with the upgrade guide below, or automatically."}</span>
			<div class="flex flex-wrap items-center gap-2">
				<button type="button" class="btn btn-primary btn-sm" data-refine-start data-no-success-toast
				        hx-post="/poe/htmx/build/refine/start" hx-include="#poeBuildCode" hx-target="#poeRefineArea" hx-swap="innerHTML">
					${ko ? "자동으로 다듬기" : "Refine automatically"}
				</button>
				<span class="text-xs text-base-content/60">${ko ? "가이드 교체안 중 DPS·EHP 를 함께 올리는 것을 하나씩 실제로 적용하며 다시 잽니다(최대 3단계, 약 3~5분, 로그인 필요)." : "Applies the guide's best DPS+EHP swap one at a time and re-measures (up to 3 steps, ~3-5 min, sign-in required)."}</span>
			</div>
			<div id="poeRefineArea" class="w-full"></div>
		</div>
	@else
		<div class="alert alert-success mb-3 text-sm" data-refined-note>${ko ? "다듬은 빌드를 불러왔습니다. 코드 칸도 이 빌드로 바뀌었습니다." : "Loaded the refined build. The code box now holds it."}</div>
	@endif
	<textarea id="poeBuildCode" hx-swap-oob="true" name="code" rows="4" required
	          class="textarea textarea-bordered w-full font-mono text-xs leading-5"
	          autocomplete="off" spellcheck="false">${fillCode}</textarea>
@endif
@template.poe.htmx.buildSummary(build = build, importError = importError, engineAvailable = engineAvailable)
