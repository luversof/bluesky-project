@param String title = "매수 집중도"
@param String canvasId = "tradeDonutChart"
@param String legendId = "tradeDonutLegend"
@param String titleId = "tradeDonutTitle"
@param String metricProfitId = "tradeDonutMetricProfit"
@param String metricBuyId = "tradeDonutMetricBuy"
@param String tabStockId = "tradeDonutTabStock"
@param String tabAccountId = "tradeDonutTabAccount"

<div class="card bg-base-100 shadow-sm border border-base-200 flex-[2] min-w-[200px]">
    <div class="card-body p-4">
        <div class="flex items-center justify-between mb-2">
            <div class="text-sm font-bold" id="${titleId}">${title}</div>
            <div class="tabs tabs-boxed tabs-xs">
                <a class="tab" id="${metricProfitId}" onclick="switchTradeDonut('profit', null)">실현손익</a>
                <a class="tab tab-active" id="${metricBuyId}" onclick="switchTradeDonut('buy', null)">매수금액</a>
            </div>
        </div>
        <div class="flex gap-3">
            <div class="flex-shrink-0 w-[200px] h-[200px]">
                <canvas id="${canvasId}" class="amount-chart"></canvas>
            </div>
            <div class="flex flex-col w-0 flex-1 h-[200px] overflow-hidden">
                <div class="tabs tabs-boxed tabs-xs mb-1 w-fit flex-shrink-0">
                    <a class="tab tab-active" id="${tabStockId}" onclick="switchTradeDonut(null, 'stock')">종목별</a>
                    <a class="tab" id="${tabAccountId}" onclick="switchTradeDonut(null, 'account')">계좌별</a>
                </div>
                <div id="${legendId}" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
            </div>
        </div>
    </div>
</div>
