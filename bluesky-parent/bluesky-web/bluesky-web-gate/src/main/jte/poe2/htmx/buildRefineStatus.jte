@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param Poe2.RefineStatus status = null
@param boolean loginRequired = false
@param boolean notStarted = false

<%-- PoE2 자동 다듬기 진행/결과 — 단계별 적용 교체와 실측 증감, 전후 DPS·EHP, 다듬은 빌드 불러오기. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> sgn = v -> v == null ? "-" : (Math.abs(v) < 0.05 ? "0.0%" : String.format("%+.1f%%", v));
	java.util.function.BiFunction<Double, Double, Double> chg = (a, b) -> a != null && b != null && a > 0 ? (b / a - 1) * 100 : null;
}
<div class="rounded-box bg-base-100 px-3 py-2 text-sm text-base-content" data-refine-status>
@if(loginRequired)
	<div class="text-base-content/60">${ko ? "자동 다듬기는 로그인 후 쓸 수 있습니다(엔진 계산을 여러 번 합니다)." : "Sign in to refine automatically (it runs the engine many times)."}</div>
@elseif(status == null)
	<div class="text-error">${ko ? "다듬기 서버에 연결하지 못했습니다." : "Could not reach the refine service."}</div>
@elseif(Boolean.TRUE.equals(status.running()) && notStarted)
	<div class="text-warning">${ko ? "다른 자동 다듬기가 진행 중입니다. 끝난 뒤 다시 눌러 주세요." : "Another refine is running. Try again when it finishes."}</div>
@elseif(Boolean.TRUE.equals(status.running()))
	<div class="flex flex-wrap items-center gap-2">
		<span class="loading loading-spinner loading-xs text-primary"></span>
		<span class="font-semibold">${ko ? "자동 다듬기" : "Refining"} ${status.round()}/${status.rounds()}</span>
		<span class="text-base-content/60">${status.phase()}</span>
	</div>
@elseif(status.error() != null)
	<div class="text-error">${status.error()}</div>
@elseif(status.result() != null)
	!{ Poe2.RefineResult r = status.result(); }
	<div class="space-y-2" data-refine-result>
		<div class="font-semibold">${ko ? "자동 다듬기 결과" : "Refine result"}</div>
		<table class="table table-xs w-auto">
			<thead><tr><th></th><th class="text-right">${ko ? "출발" : "Start"}</th><th class="text-right">${ko ? "다듬은 뒤" : "Refined"}</th><th class="text-right">${ko ? "변화" : "Change"}</th></tr></thead>
			<tbody>
				<tr><td>DPS</td><td class="text-right font-mono">${String.format("%,.0f", r.before().dps())}</td><td class="text-right font-mono">${String.format("%,.0f", r.after().dps())}</td><td class="text-right font-mono">${sgn.apply(chg.apply(r.before().dps(), r.after().dps()))}</td></tr>
				<tr><td>EHP</td><td class="text-right font-mono">${String.format("%,.0f", r.before().ehp())}</td><td class="text-right font-mono">${String.format("%,.0f", r.after().ehp())}</td><td class="text-right font-mono">${sgn.apply(chg.apply(r.before().ehp(), r.after().ehp()))}</td></tr>
			</tbody>
		</table>
		@template.poe2.htmx.weaponSets(mainSet = r.mainSet(), sets = r.sets())
		@if(r.steps() == null || r.steps().isEmpty())
			<div class="text-base-content/60">${ko ? "DPS·EHP 를 함께 올리는 고유 아이템·보조젬 교체를 찾지 못했습니다." : "No unique or support swap raised DPS and EHP together."}</div>
		@else
			<ol class="list-decimal space-y-1 pl-5" data-refine-steps>
				@for(Poe2.RefineStep st : r.steps())
					<li><span>${st.label()}</span> <span class="font-mono text-xs text-base-content/60">DPS ${sgn.apply(st.dpsPct())} · EHP ${sgn.apply(st.ehpPct())}</span></li>
				@endfor
			</ol>
			<form hx-post="/poe2/htmx/build/import-replace" hx-target="#poe2BuildResult" hx-swap="innerHTML" data-no-success-toast>
				<input type="hidden" name="code" value="${r.code()}">
				<button type="submit" class="btn btn-primary btn-sm" data-refine-load>${ko ? "다듬은 빌드 불러오기" : "Load refined build"}</button>
			</form>
		@endif
		<div class="text-[11px] text-base-content/50">${ko ? "각 단계의 증감은 직전 빌드 대비이며, 적용한 빌드를 PoB-PoE2 엔진으로 다시 계산한 값입니다(가이드 예측이 아니라 실측). 고유 아이템은 품질 20% 기준. DPS 는 주 스킬 한 대상 기준이라, 갈래·연쇄·투사체 수 보조를 바꾸는 단계는 사냥 속도를 줄일 수 있습니다." : "Each step's change is vs. the previous build, measured by recalculating the applied build (not the guide's prediction). Uniques at 20% quality. DPS is single-target for the main skill — steps that swap out fork, chain or projectile-count supports can slow down clearing."}</div>
	</div>
@endif
</div>
