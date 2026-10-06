@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeUpgradeGuide

@param PoeUpgradeGuide.RefineStatus status = null
@param boolean loginRequired = false
@param boolean notStarted = false

<%-- 자동 다듬기 진행/결과 — 단계마다 적용한 교체와 그 단계의 증감, 전후 DPS·EHP, 다듬은 빌드 불러오기. --%>
!{
	boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
	java.util.function.Function<Double, String> sgn = v -> v == null ? "-" : (Math.abs(v) < 0.05 ? "0.0%" : String.format("%+.1f%%", v));
}
<div class="rounded-box bg-base-100 px-3 py-2 text-sm text-base-content" data-refine-status>
@if(loginRequired)
	<div class="text-base-content/60">${MessageUtil.getMessage("poe.build.guide.login")}</div>
@elseif(status == null)
	<div class="text-error">${MessageUtil.getMessage("poe.build.guide.unavailable")}</div>
@elseif(Boolean.TRUE.equals(status.running()) && notStarted)
	<div class="text-warning">${ko ? "다른 분석(업그레이드 가이드 또는 자동 다듬기)이 진행 중입니다. 끝난 뒤 다시 눌러 주세요." : "Another analysis (guide or refine) is running. Try again when it finishes."}</div>
@elseif(Boolean.TRUE.equals(status.running()))
	<div class="flex flex-wrap items-center gap-2">
		<span class="loading loading-spinner loading-xs text-primary"></span>
		<span class="font-semibold">${ko ? "자동 다듬기" : "Refining"} ${status.round()}/${status.rounds()}</span>
		<span class="text-base-content/60">${status.phase()}</span>
		@if(status.total() != null && status.total() > 0)
			<span class="font-mono text-base-content/60">${status.done()}/${status.total()}</span>
		@endif
	</div>
@elseif(status.error() != null)
	<div class="text-error">${status.error()}</div>
@elseif(status.result() != null)
	!{ PoeUpgradeGuide.RefineResult r = status.result(); }
	<div class="space-y-2" data-refine-result>
		<div class="font-semibold">${ko ? "자동 다듬기 결과" : "Refine result"}</div>
		<table class="table table-xs w-auto">
			<thead><tr><th></th><th class="text-right">${ko ? "출발" : "Start"}</th><th class="text-right">${ko ? "다듬은 뒤" : "Refined"}</th><th class="text-right">${ko ? "변화" : "Change"}</th></tr></thead>
			<tbody>
				<tr><td>DPS</td><td class="text-right font-mono">${String.format("%,.0f", r.before().dps())}</td><td class="text-right font-mono">${String.format("%,.0f", r.after().dps())}</td><td class="text-right font-mono">${sgn.apply(r.before().dps() > 0 ? (r.after().dps() / r.before().dps() - 1) * 100 : null)}</td></tr>
				<tr><td>EHP</td><td class="text-right font-mono">${String.format("%,.0f", r.before().ehp())}</td><td class="text-right font-mono">${String.format("%,.0f", r.after().ehp())}</td><td class="text-right font-mono">${sgn.apply(r.before().ehp() > 0 ? (r.after().ehp() / r.before().ehp() - 1) * 100 : null)}</td></tr>
			</tbody>
		</table>
		@if(r.steps() == null || r.steps().isEmpty())
			<div class="text-base-content/60">${ko ? "DPS·EHP 를 함께 올리는 교체를 찾지 못했습니다 — 이 빌드는 가이드가 보는 범위(보조젬·고유·레어 목표)에서 이미 잘 맞춰져 있습니다." : "No swap raised DPS and EHP together — the build is already well tuned within the guide's scope."}</div>
		@else
			<ol class="list-decimal space-y-1 pl-5" data-refine-steps>
				@for(PoeUpgradeGuide.RefineStep st : r.steps())
					<li><span>${st.label()}</span> <span class="font-mono text-xs text-base-content/60">DPS ${sgn.apply(st.dpsPct())} · EHP ${sgn.apply(st.ehpPct())}</span></li>
				@endfor
			</ol>
			<form hx-post="/poe/htmx/build/import-replace" hx-target="#poeBuildSummary" hx-swap="innerHTML" data-no-success-toast>
				<input type="hidden" name="code" value="${r.code()}">
				<button type="submit" class="btn btn-primary btn-sm" data-refine-load>${ko ? "다듬은 빌드 불러오기" : "Load refined build"}</button>
			</form>
		@endif
		<div class="text-[11px] text-base-content/60">${ko ? "각 단계의 증감은 직전 빌드 대비이며, 적용 후 PoB 엔진으로 다시 계산한 값입니다. 보스 단일 대상(최종 보스) 기준이라, 효과 범위·투사체 수 보조를 바꾸는 단계는 사냥 속도를 줄일 수 있습니다." : "Each step's change is vs. the previous build, re-measured with the PoB engine after applying. Measured against a single pinnacle boss — steps that swap out area or projectile-count supports can slow down clearing."}</div>
	</div>
@endif
</div>
