<%-- PoE2 젬 랭킹 배치 상태 폴링 래퍼 — PoE1 poe/htmx/simWrap.jte 와 같다(interval 폴링, 유휴 시 서버가 286 으로 중단) --%>
<div id="poe2RankingStatusWrap" hx-get="/poe2/htmx/sim/ranking/status" hx-trigger="load, every 3s" hx-swap="innerHTML">
	<div class="flex justify-center p-6">
		<span class="loading loading-spinner loading-md text-primary"></span>
	</div>
</div>
