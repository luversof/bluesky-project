<%-- PoE2 시뮬레이션 상태 폴링 래퍼 — PoE1 시뮬(simOptimizeWrap)과 같은 방식. 끝나면 서버가 HTTP 286 으로 폴링을 멈춘다. --%>
<div id="poe2SimStatusWrap" hx-get="/poe2/htmx/sim/status" hx-trigger="load, every 3s" hx-swap="innerHTML" data-no-success-toast>
	<div class="flex justify-center p-4">
		<span class="loading loading-spinner loading-sm text-primary"></span>
	</div>
</div>
