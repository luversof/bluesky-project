@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil

@param String title = null
@param String canvasId = "tradeDonutChart"
@param String legendId = "tradeDonutLegend"
@param String titleId = "tradeDonutTitle"
@param String metricProfitId = "tradeDonutMetricProfit"
@param String metricBuyId = "tradeDonutMetricBuy"
@param String tabStockId = "tradeDonutTabStock"
@param String tabAccountId = "tradeDonutTabAccount"

!{
	String resolvedTitle = title != null ? title : MessageUtil.getMessage("stock.trade.chart.title.buy.concentration");
	String profitLabel = MessageUtil.getMessage("stock.trade.label.realized.profit");
	String buyAmountLabel = MessageUtil.getMessage("stock.trade.label.buy.amount");
	String byStockLabel = MessageUtil.getMessage("stock.label.by.stock");
	String byAccountLabel = MessageUtil.getMessage("stock.label.by.account");
}

@template._components.ui.card(cssClass = "flex-[2] min-w-[200px]", body = @`
		<%-- 제목과 지표 탭은 자리가 모자라면 탭을 아래 줄로 내린다 - 한 줄에 밀어 넣으면 글꼴 200% 의 320px 에서 제목 "매수 집중도" 가
		     한 글자씩 세로로 쌓였다(실측 2026-09-17). --%>
		<div class="flex flex-wrap items-center justify-between gap-2 mb-2">
			<h2 class="text-sm font-bold" id="${titleId}" data-page-section>${resolvedTitle}</h2>
			<div role="tablist" class="tabs tabs-box tabs-xs">
				<a class="tab" id="${metricProfitId}" data-trade-donut-metric="profit" tabindex="0" role="tab" aria-controls="${canvasId}Panel">${profitLabel}</a>
				<a class="tab tab-active" id="${metricBuyId}" data-trade-donut-metric="buy" tabindex="0" role="tab" aria-controls="${canvasId}Panel">${buyAmountLabel}</a>
			</div>
		</div>
		<%-- 범례는 자리가 모자라면 아래 줄로 내려간다. 캔버스가 flex-shrink-0 w-[200px] 라
		     좁은 카드에서는 남는 폭이 음수가 되고 w-0 flex-1 인 범례가 0px 로 찌부러져 통째로 사라졌다
		     (실측 2026-09-15, 640px 2열 배치: 카드 203px 안에 캔버스 200px + gap 12px - 범례 폭 0).
		     종목별 비중이 안 보이던 자리다.
		     최소 폭은 min(10rem, 100%) - 10rem 만 두면 글꼴을 키울 때 같이 커져 320px + 글꼴 200% 에서 320px 이 되고 카드 밖으로 65px 삐져 페이지를 밀었다(실측 2026-09-17: 매매 · 배당 · 배당 이번달). --%>
		<div id="${canvasId}Panel" role="tabpanel" class="flex flex-wrap gap-3">
			<div class="flex-shrink-0 w-[200px] h-[200px]">
				<canvas id="${canvasId}" role="img" aria-label="${resolvedTitle}" class="amount-chart"></canvas>
			</div>
			<div class="flex flex-col min-w-[min(10rem,100%)] flex-1 h-[200px] overflow-hidden">
				<div role="tablist" class="tabs tabs-box tabs-xs mb-1 w-fit flex-shrink-0">
					<a class="tab tab-active" id="${tabStockId}" data-trade-donut-mode="stock" tabindex="0" role="tab" aria-controls="${legendId}">${byStockLabel}</a>
					<a class="tab" id="${tabAccountId}" data-trade-donut-mode="account" tabindex="0" role="tab" aria-controls="${legendId}">${byAccountLabel}</a>
				</div>
				<div id="${legendId}" role="tabpanel" tabindex="0" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
			</div>
		</div>
`)

<script nonce="${CspNonceHolder.getNonce()}">
	// 이 컴포넌트는 htmx 스왑마다 재렌더될 수 있으므로 document 위임 + 전역 1회 가드로 바인딩한다.
	if (!window.__tradeDonutTabsBound) {
		window.__tradeDonutTabsBound = true;
		document.addEventListener('click', function(event) {
			var tab = event.target && event.target.closest ? event.target.closest('[data-trade-donut-metric], [data-trade-donut-mode]') : null;
			if (!tab || typeof window.switchTradeDonut !== 'function') {
				return;
			}
			window.switchTradeDonut(tab.dataset.tradeDonutMetric || null, tab.dataset.tradeDonutMode || null);
		});
	}
</script>
