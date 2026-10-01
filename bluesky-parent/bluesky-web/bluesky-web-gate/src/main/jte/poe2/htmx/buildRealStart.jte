@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.BuildSummary build = null
@param Poe2.NinjaArchetype archetype = null
@param String oobCode = null
@param boolean refined = false

<%-- PoE2 빌드 화면 "실빌드에서 출발"(archetype) · 다듬은 빌드 불러오기(refined) — 출처 안내 + 자동 다듬기 버튼 + 코드 칸 채우기(OOB) + 일반 요약.
     코드 칸은 OOB 로 **먼저** 바뀐다(htmx 는 OOB 를 본문보다 먼저 처리) — 요약이 뜨자마자 부르는 재계산·가이드가 그 칸을 보낸다. --%>
!{ boolean ko = PoeText.isKorean(); }
@if(oobCode != null)
	<textarea id="poe2BuildCode" hx-swap-oob="true" name="code" rows="4" class="textarea textarea-bordered w-full font-mono text-xs"
	          placeholder="${MessageUtil.getMessage("poe2.build.placeholder")}" aria-label="${MessageUtil.getMessage("poe2.build.placeholder")}" spellcheck="false">${oobCode}</textarea>
@endif
@if(archetype != null && archetype.start() != null)
	!{ Poe2.NinjaStart st = archetype.start(); }
	<div class="alert mb-3 flex flex-col items-start gap-2 text-sm" data-real-start-note>
		<span>${ko
			? "poe.ninja 실빌드에서 출발" + (st.level() != null ? " (Lv" + st.level() + ")" : "") + " — " + archetype.ascendancy() + " · " + archetype.mainSkill() + ", 실빌드 " + st.measured() + "명을 PoB-PoE2 엔진으로 다시 계산해 DPS·EHP 중앙값에 가장 가까운 빌드입니다(플레이어 설정 그대로). 아래 가이드를 보거나 자동으로 다듬어 보세요."
			: "Starting from a poe.ninja real build" + (st.level() != null ? " (Lv" + st.level() + ")" : "") + " — " + archetype.ascendancy() + " · " + archetype.mainSkill() + ", closest to the DPS/EHP median of " + st.measured() + " real builds recalculated with the PoB-PoE2 engine (player settings kept). Check the guide below or refine it automatically."}</span>
		<div class="flex flex-wrap items-center gap-2">
			<button type="button" class="btn btn-primary btn-sm" data-refine-start data-no-success-toast
			        hx-post="/poe2/htmx/build/refine/start" hx-include="#poe2BuildImport [name=code]" hx-target="#poe2RefineArea" hx-swap="innerHTML">
				${ko ? "자동으로 다듬기" : "Refine automatically"}
			</button>
			<span class="text-xs text-base-content/60">${ko ? "가이드의 고유 아이템·보조젬 교체 중 DPS·EHP 를 함께 올리는 것을 하나씩 실제로 적용하고 엔진으로 다시 잽니다(최대 3단계, 로그인 필요)." : "Applies the guide's unique/support swaps that raise DPS and EHP together, one at a time, re-measured by the engine (up to 3 steps, sign-in required)."}</span>
		</div>
		<div id="poe2RefineArea" class="w-full"></div>
	</div>
@elseif(refined)
	<div class="alert alert-success mb-3 text-sm" data-refined-note>${ko ? "다듬은 빌드를 불러왔습니다. 코드 칸도 이 빌드로 바뀌었습니다." : "Loaded the refined build. The code box now holds it."}</div>
@endif
@template.poe2.htmx.buildSummary(build = build, error = null, detail = null)
