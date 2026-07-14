<%-- 젬 랭킹 배치 상태 폴링 래퍼 — interval 폴링(요청 실패에도 유지), 유휴 시 서버가 286 으로 중단 --%>
<div id="poeSimStatusWrap" hx-get="/poe/htmx/sim/status" hx-trigger="load, every 3s" hx-swap="innerHTML">
	<div class="flex justify-center p-6">
		<span class="loading loading-spinner loading-md text-primary"></span>
	</div>
</div>
