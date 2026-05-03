@import io.github.luversof.boot.context.support.MessageUtil

@param String title = null
@param String canvasId = "tradeMonthlyChart"

!{
    String resolvedTitle = title != null ? title : MessageUtil.getMessage("stock.trade.chart.title.monthly.trade.amount");
}

<div class="card bg-base-100 shadow-sm border border-base-200 flex-[3] min-w-[280px] overflow-visible">
    <div class="card-body p-4">
        <h3 class="font-bold text-sm mb-3">${resolvedTitle}</h3>
        <div class="relative h-[220px]">
            <canvas id="${canvasId}" class="amount-chart"></canvas>
        </div>
    </div>
</div>
