<%-- PoE2 자동 다듬기 상태 폴링 래퍼 — PoE1(poe/htmx/buildRefineWrap)과 같은 방식. 끝나면 서버가 HTTP 286 으로 폴링을 멈춘다. --%>
<div id="poe2RefineStatus" hx-get="/poe2/htmx/build/refine/status" hx-trigger="load, every 3s" hx-swap="innerHTML" data-no-success-toast>
	<div class="flex justify-center p-2">
		<span class="loading loading-spinner loading-sm text-primary"></span>
	</div>
</div>
