<%-- 업그레이드 가이드 상태 폴링 래퍼 — 최적화 화면(simOptimizeWrap)과 같은 방식.
     이 요소 자체는 교체되지 않아 interval 폴링이 요청 하나 실패해도 살아남고, 끝나면 서버가 HTTP 286 으로 폴링을 멈춘다. --%>
<div id="poeUpgradeGuideStatus" hx-get="/poe/htmx/build/guide/status" hx-trigger="load, every 3s" hx-swap="innerHTML">
	<div class="flex justify-center p-4">
		<span class="loading loading-spinner loading-sm text-primary"></span>
	</div>
</div>
