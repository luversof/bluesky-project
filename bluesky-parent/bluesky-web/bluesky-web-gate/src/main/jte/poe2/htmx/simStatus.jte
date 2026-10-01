@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param Poe2.SimStatus status = null
@param Poe2.BuildSummary build = null
@param boolean loginRequired = false
@param boolean notStarted = false

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
	java.util.function.Function<String, String> objLabel = o -> "dps".equals(o) ? (ko ? "공격(DPS)" : "Offense (DPS)") : "ehp".equals(o) ? (ko ? "생존(EHP)" : "Defense (EHP)") : (ko ? "균형(DPS·EHP 합)" : "Balanced (DPS+EHP)");
	java.util.function.Function<String, String> scenLabel = s -> "None".equals(s) ? (ko ? "보스 아님" : "Not a boss") : "Boss".equals(s) ? (ko ? "일반 보스" : "Standard boss") : "Uber".equals(s) ? (ko ? "우버 보스" : "Uber boss") : "keep".equals(s) ? (ko ? "플레이어 설정 그대로" : "Player setting") : (ko ? "최종 보스" : "Pinnacle boss");
}
<div class="space-y-3" data-sim-status>
@if(loginRequired)
	<div class="text-sm text-base-content/60">${ko ? "시뮬레이션은 로그인 후 실행할 수 있습니다(poe.ninja 요청과 엔진 계산을 여러 번 합니다)." : "Sign in to run the simulation (it queries poe.ninja and runs the engine many times)."}</div>
@elseif(status == null)
	<div class="text-sm text-error">${ko ? "시뮬레이터 서버에 연결하지 못했습니다." : "Could not reach the simulator service."}</div>
@elseif(notStarted && Boolean.TRUE.equals(status.running()))
	<div class="text-sm text-warning">${ko ? "다른 시뮬레이션(또는 자동 다듬기)이 진행 중입니다. 끝난 뒤 다시 실행해 주세요." : "Another simulation (or refine) is running. Try again when it finishes."}</div>
@elseif(Boolean.TRUE.equals(status.running()))
	<div class="flex flex-wrap items-center gap-2 text-sm">
		<span class="loading loading-spinner loading-xs text-primary"></span>
		<span class="font-semibold">${status.phase()}</span>
		@if(status.round() != null && status.round() > 0)
			<span class="font-mono text-base-content/60">${status.round()}/${status.rounds()}</span>
		@endif
		<span class="text-xs text-base-content/50">${ko ? "실빌드 후보 받기·재계산 → 목표에 맞춰 다듬기(보통 1~2분)" : "Fetching real-build candidates → refining toward the objective (usually 1-2 min)"}</span>
	</div>
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
