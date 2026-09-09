@import io.github.luversof.boot.context.support.MessageUtil

<%-- htmx 가 아닌 평범한 POST 폼이 오래 걸릴 때 덮는 오버레이. 전역 htmx 피드백(#app-progress)은 htmx 요청만 덮는다.
     폼에 data-submit-overlay(+ -title / -desc)를 달면 /js/stock/submitOverlay.js 가 이 패널을 띄우고 두 번째 제출을 막는다.
     한 화면에 한 벌만 둔다. 실측 2026-09-08: 출처 일괄 가져오기가 외부 ETF 사이트 한 건에 8.65 초. --%>
<div data-submit-overlay-panel="true" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50">
	<div class="flex flex-col items-center gap-4 rounded-2xl bg-base-100 px-8 py-8 shadow-xl">
		<span class="loading loading-spinner loading-lg text-primary"></span>
		<div data-submit-overlay-heading="true" class="text-lg font-bold">${MessageUtil.getMessage("common.message.submit.overlay.title")}</div>
		<div data-submit-overlay-note="true" class="text-sm opacity-70">${MessageUtil.getMessage("stock.admin.message.please.wait")}</div>
	</div>
</div>
