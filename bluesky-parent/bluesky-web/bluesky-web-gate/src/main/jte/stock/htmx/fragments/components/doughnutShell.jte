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
        <div class="flex items-center justify-between mb-2">
            <div class="text-sm font-bold" id="${titleId}">${resolvedTitle}</div>
            <div class="tabs tabs-box tabs-xs">
                <a class="tab" id="${metricProfitId}" data-trade-donut-metric="profit">${profitLabel}</a>
                <a class="tab tab-active" id="${metricBuyId}" data-trade-donut-metric="buy">${buyAmountLabel}</a>
            </div>
        </div>
        <div class="flex gap-3">
            <div class="flex-shrink-0 w-[200px] h-[200px]">
                <canvas id="${canvasId}" class="amount-chart"></canvas>
            </div>
            <div class="flex flex-col w-0 flex-1 h-[200px] overflow-hidden">
                <div class="tabs tabs-box tabs-xs mb-1 w-fit flex-shrink-0">
                    <a class="tab tab-active" id="${tabStockId}" data-trade-donut-mode="stock">${byStockLabel}</a>
                    <a class="tab" id="${tabAccountId}" data-trade-donut-mode="account">${byAccountLabel}</a>
                </div>
                <div id="${legendId}" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
            </div>
        </div>
`)

<script>
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
