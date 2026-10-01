<%-- 자동 다듬기 상태 폴링 래퍼 — 가이드(buildGuideWrap)와 같은 방식. 끝나면 서버가 HTTP 286 으로 폴링을 멈춘다. --%>
<div id="poeRefineStatus" hx-get="/poe/htmx/build/refine/status" hx-trigger="load, every 3s" hx-swap="innerHTML">
	<div class="flex justify-center p-2">
		<span class="loading loading-spinner loading-sm text-primary"></span>
	</div>
</div>
