@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param Poe2.SimStatus status = null
@param Poe2.BuildSummary build = null
@param boolean loginRequired = false
@param boolean notStarted = false
@param boolean authenticated = false

<%-- PoE2 시뮬레이션 진행/결과 — 결과는 ① 비교표(출발 실빌드 · 다듬은 뒤 · ninja 중앙값) ② 적용 단계 ③ 실빌드 후보 ④ 결과 빌드 요약(빌드 화면과 같은
     조각 — 재계산·가이드가 코드 칸을 hx-include 로 보내므로 숨은 #poe2BuildImport 에 결과 코드를 둔다). --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> fmt = v -> {
		if (v == null) return "-";
		double n = v;
		if (n >= 1_000_000_000d) return String.format("%.2fB", n / 1_000_000_000d);
		if (n >= 1_000_000d) return String.format("%.2fM", n / 1_000_000d);
		if (n >= 10_000d) return String.format("%.1fk", n / 1_000d);
		return String.format("%,.0f", n);
	};
	java.util.function.BiFunction<Double, Double, String> chg = (a, b) -> {
		if (a == null || b == null || a <= 0) return "-";
		double p = (b / a - 1) * 100;
		return Math.abs(p) < 0.05 ? "0.0%" : String.format("%+.1f%%", p);
	};
	java.util.function.Function<String, String> objLabel = o -> MessageUtil.getMessage("dps".equals(o) ? "poe.sim.opt.objective.dps" : "ehp".equals(o) ? "poe.sim.opt.objective.ehp" : "poe.sim.opt.objective.balanced");
	java.util.function.Function<String, String> scenLabel = s -> MessageUtil.getMessage("None".equals(s) ? "poe.sim.opt.scenario.none" : "Boss".equals(s) ? "poe.sim.opt.scenario.boss" : "Uber".equals(s) ? "poe.sim.opt.scenario.uber" : "keep".equals(s) ? "poe2.sim.opt.scenario.keep" : "poe.sim.opt.scenario.pinnacle");
}
<div class="space-y-3" data-sim-status>
@if(loginRequired)
	<div class="text-sm text-base-content/60">${MessageUtil.getMessage("poe.sim.opt.login")}</div>
@elseif(status == null)
	<div class="text-sm text-error">${ko ? "시뮬레이터 서버에 연결하지 못했습니다." : "Could not reach the simulator service."}</div>
@elseif(notStarted && Boolean.TRUE.equals(status.running()))
	<div class="text-sm text-warning">${ko ? "다른 시뮬레이션(또는 자동 다듬기)이 진행 중입니다. 끝난 뒤 다시 실행해 주세요." : "Another simulation (or refine) is running. Try again when it finishes."}</div>
@elseif(Boolean.TRUE.equals(status.running()))
	<%-- 진행 — PoE1 simOptimizeStatus 와 같은 줄: 단계 이름(키) · done/total · 평가 N회 · 중지 단추, 아래 진행 막대(10-12 C269) --%>
	!{
		String phaseCode = status.phase() == null ? "" : status.phase();
		String phaseLabel = phaseCode.startsWith("refine.")
			? MessageUtil.getMessage("poe2.sim.phase.refine") + " · " + MessageUtil.getMessage("poe2.refine.phase." + phaseCode.substring(7), phaseCode.substring(7))
			: MessageUtil.getMessage("poe2.sim.phase." + phaseCode, phaseCode);
		int done = status.round() == null ? 0 : status.round();
		int total = status.rounds() == null ? 0 : status.rounds();
	}
	<div class="flex flex-wrap items-center gap-2 text-sm">
		<span class="loading loading-spinner loading-xs text-primary"></span>
		<span class="font-semibold">${phaseLabel}</span>
		@if(total > 0)
			<span class="font-mono text-base-content/60">${done}/${total}</span>
		@endif
		<span class="badge badge-ghost badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.evals"), status.evals() == null ? 0 : status.evals())}</span>
		@if(authenticated)
			<button type="button" class="btn btn-error btn-xs ml-auto" data-sim-stop
			        hx-post="/poe2/htmx/sim/stop" hx-swap="none"
			        data-busy-label="${MessageUtil.getMessage("poe.sim.opt.stopping")}">${MessageUtil.getMessage("poe.sim.opt.stop")}</button>
		@endif
		<span class="text-xs text-base-content/60">${ko ? "실빌드 후보 받기·재계산 → 목표에 맞춰 다듬기(보통 1~2분)" : "Fetching real-build candidates → refining toward the objective (usually 1-2 min)"}</span>
	</div>
	@if(total > 0)
		<progress class="progress progress-primary w-full" value="${done}" max="${total}"></progress>
	@endif
@elseif(Boolean.TRUE.equals(status.cancelled()))
	<div class="alert alert-warning text-sm" role="status">${MessageUtil.getMessage("poe.sim.opt.cancelled")}</div>
@elseif(status.error() != null)
	<div class="alert alert-warning text-sm">${status.error()}</div>
@elseif(status.result() != null)
	@template.poe2.htmx.simResult(result = status.result(), historyId = null)
	<%-- 끝난 결과는 이력에 저장된다 — 이력 목록을 다시 불러온다(PoE1 simOptimizeStatus 의 OOB 와 같다) --%>
	<div id="poe2SimHistory" hx-swap-oob="true" hx-get="/poe2/htmx/sim/history" hx-trigger="load" hx-swap="innerHTML"></div>
	@if(build != null)
		<form id="poe2BuildImport" class="hidden" aria-hidden="true"><textarea name="code">${status.result().code()}</textarea></form>
		@template.poe2.htmx.buildSummary(build = build, error = null, detail = null)
	@endif
@endif
</div>
