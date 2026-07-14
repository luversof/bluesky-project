<%-- 최적화 상태 폴링 래퍼 — 이 요소 자체는 교체되지 않아 interval 폴링이 요청 실패에도 살아남는다.
     완료/유휴 상태는 서버가 HTTP 286 을 돌려줘 폴링이 멈춘다 (새 실행 시 form 이 래퍼를 outerHTML 교체해 재장전). --%>
<div id="poeOptimizeStatusWrap" hx-get="/poe/htmx/sim/optimize/status" hx-trigger="load, every 3s" hx-swap="innerHTML">
	<div class="flex justify-center p-4">
		<span class="loading loading-spinner loading-sm text-primary"></span>
	</div>
</div>
