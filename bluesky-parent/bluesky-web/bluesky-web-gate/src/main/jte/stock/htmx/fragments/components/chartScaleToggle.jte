@import io.github.luversof.boot.context.support.MessageUtil

<%-- 보유 평가액 그래프의 세로 눈금 전환(일반 | 로그, 2026-10-28 12차 자율). 자산 성장 · 계좌 상세 · 종목 상세가 같이 쓴다 -
     긴 기간이면 초기 금액이 0 근처에 깔린다. 동작은 stock-charts.ts(data-chart-scale-for 문서 위임 · 이 브라우저 localStorage 하나). --%>
@param String canvasId

<div class="flex items-center justify-end gap-2 text-xs text-base-content/70 mb-1" role="group" aria-label="${MessageUtil.getMessage("stock.asset.growth.chart.scale")}" title="${MessageUtil.getMessage("stock.asset.growth.chart.scale.log.hint")}" data-chart-scale-for="${canvasId}">
	<span aria-hidden="true">${MessageUtil.getMessage("stock.asset.growth.chart.scale")}</span>
	<div class="join">
		<button type="button" class="btn btn-xs join-item btn-primary" aria-pressed="true" data-chart-scale="linear">${MessageUtil.getMessage("stock.asset.growth.chart.scale.linear")}</button>
		<button type="button" class="btn btn-xs join-item btn-ghost" aria-pressed="false" data-chart-scale="logarithmic">${MessageUtil.getMessage("stock.asset.growth.chart.scale.log")}</button>
	</div>
</div>
