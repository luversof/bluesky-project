@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil

!{
	String weightAnalysisLabel = MessageUtil.getMessage("stock.chart.title.weight.analysis");
	String byStockLabel = MessageUtil.getMessage("stock.label.by.stock");
	String byAccountLabel = MessageUtil.getMessage("stock.label.by.account");
	String monthlyDividendsLabel = MessageUtil.getMessage("stock.dividend.chart.title.monthly.simple");
}

	<!-- Row 2: Charts -->
	<div class="flex flex-wrap gap-4 mb-4">

		<!-- Left: 비중 분석 -->
		@template._components.ui.card(cssClass = "flex-[2] min-w-[200px]", body = @`
				<h2 class="text-sm font-bold mb-3" data-page-section>${weightAnalysisLabel}</h2>
				<%-- 범례는 자리가 모자라면 아래 줄로 내려간다. 캔버스가 flex-shrink-0 w-[200px] 라
				     좁은 카드에서는 남는 폭이 음수가 되고 w-0 flex-1 인 범례가 0px 로 찌부러져 통째로 사라졌다
				     (실측 2026-09-15, 640px 2열 배치: 카드 203px 안에 캔버스 200px + gap 12px - 범례 폭 0).
				     종목별 비중이 안 보이던 자리다.
				     최소 폭은 min(10rem, 100%) - 10rem 만 두면 글꼴을 키울 때 같이 커져 320px + 글꼴 200% 에서 320px 이 되고 카드 밖으로 65px 삐져 페이지를 밀었다(실측 2026-09-17: 매매 · 배당 · 배당 이번달). --%>
				<div class="flex flex-wrap gap-3">
					<div class="flex-shrink-0 w-[200px] h-[200px]">
						<canvas id="donutChart" role="img" aria-label="${weightAnalysisLabel}" class="amount-chart"></canvas>
					</div>
					<div class="flex flex-col min-w-[min(10rem,100%)] flex-1 h-[200px] overflow-hidden">
						<div role="tablist" class="tabs tabs-box tabs-xs mb-1 w-fit flex-shrink-0">
							<a class="tab tab-active" id="donutTabStock" data-dividend-donut-switch="stock" tabindex="0" role="tab" aria-controls="donutLegend">${byStockLabel}</a>
							<a class="tab" id="donutTabAccount" data-dividend-donut-switch="account" tabindex="0" role="tab" aria-controls="donutLegend">${byAccountLabel}</a>
						</div>
						<div id="donutLegend" role="tabpanel" tabindex="0" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
					</div>
				</div>
		`)

		<!-- Right: Monthly Bar Chart -->
		@template._components.ui.card(cssClass = "flex-[3] min-w-[280px] overflow-visible", body = @`
				<h2 class="font-bold text-sm mb-3" data-page-section>${monthlyDividendsLabel}</h2>
				<div class="relative h-[280px]">
					<canvas id="monthlyBarChart" role="img" aria-label="${monthlyDividendsLabel}" class="amount-chart"></canvas>
				</div>
		`)

	</div>

<script nonce="${CspNonceHolder.getNonce()}">
	// 이 프래그먼트는 htmx 스왑마다 재렌더될 수 있으므로 document 위임 + 전역 1회 가드로 바인딩한다.
	if (!window.__dividendDonutSwitchBound) {
		window.__dividendDonutSwitchBound = true;
		document.addEventListener('click', function(event) {
			var tab = event.target && event.target.closest ? event.target.closest('[data-dividend-donut-switch]') : null;
			if (!tab || typeof window.switchDonut !== 'function') {
				return;
			}
			window.switchDonut(tab.dataset.dividendDonutSwitch);
		});
	}
</script>
