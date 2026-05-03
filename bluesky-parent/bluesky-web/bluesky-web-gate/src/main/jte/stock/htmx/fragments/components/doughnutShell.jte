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

<div class="card bg-base-100 shadow-sm border border-base-200 flex-[2] min-w-[200px]">
    <div class="card-body p-4">
        <div class="flex items-center justify-between mb-2">
            <div class="text-sm font-bold" id="${titleId}">${resolvedTitle}</div>
            <div class="tabs tabs-boxed tabs-xs">
                <a class="tab" id="${metricProfitId}" onclick="switchTradeDonut('profit', null)">${profitLabel}</a>
                <a class="tab tab-active" id="${metricBuyId}" onclick="switchTradeDonut('buy', null)">${buyAmountLabel}</a>
            </div>
        </div>
        <div class="flex gap-3">
            <div class="flex-shrink-0 w-[200px] h-[200px]">
                <canvas id="${canvasId}" class="amount-chart"></canvas>
            </div>
            <div class="flex flex-col w-0 flex-1 h-[200px] overflow-hidden">
                <div class="tabs tabs-boxed tabs-xs mb-1 w-fit flex-shrink-0">
                    <a class="tab tab-active" id="${tabStockId}" onclick="switchTradeDonut(null, 'stock')">${byStockLabel}</a>
                    <a class="tab" id="${tabAccountId}" onclick="switchTradeDonut(null, 'account')">${byAccountLabel}</a>
                </div>
                <div id="${legendId}" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
            </div>
        </div>
    </div>
</div>
