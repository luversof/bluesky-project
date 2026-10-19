@import net.luversof.web.gate.poe.PoeText

@param String jobId
@param int progress = 0
@param java.util.List<String> targetLabels = java.util.List.of()

<%-- 최적 조합 기다림(C457k) — API 백그라운드 작업을 1.5초마다 조회. 끝나면 결과 조각(craftOptimize)으로 바뀐다 --%>
!{
	boolean ko = PoeText.isKorean();
	StringBuilder q = new StringBuilder();
	for (String l : targetLabels) {
		q.append(q.length() == 0 ? "?" : "&").append("label=").append(java.net.URLEncoder.encode(l, java.nio.charset.StandardCharsets.UTF_8));
	}
}
<div data-craft-optimize data-craft-optimize-wait="${progress}" hx-get="/poe/htmx/craft/optimize/${jobId}${q.toString()}" hx-trigger="load delay:1500ms" hx-swap="outerHTML" class="rounded-box border border-base-300 bg-base-100 p-3 space-y-2 text-sm" role="status">
	<div><span class="loading loading-spinner loading-xs align-middle"></span> ${ko ? "최적 조합 찾는 중(약 20초) — 에센스 · 화석 · 수확 · 제작대 · 짐승 · 화폐를 섞어 상태마다 가장 싼 수단을 찾습니다" : "Searching the optimal combination (about 20 s) — mixing essences, fossils, harvest, bench, beasts and orbs"}</div>
	<progress class="progress progress-primary w-full" value="${progress}" max="100" aria-label="${ko ? "최적 조합 진행" : "Optimization progress"}"></progress>
</div>
