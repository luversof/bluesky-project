@param String title = "월별 매매 금액"
@param String canvasId = "tradeMonthlyChart"

<div class="card bg-base-100 shadow-sm border border-base-200 flex-[3] min-w-[280px] overflow-visible">
    <div class="card-body p-4">
        <h3 class="font-bold text-sm mb-3">${title}</h3>
        <div class="relative h-[220px]">
            <canvas id="${canvasId}" class="amount-chart"></canvas>
        </div>
    </div>
</div>
