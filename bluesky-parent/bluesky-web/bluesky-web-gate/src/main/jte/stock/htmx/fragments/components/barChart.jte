@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil

@param List<String> labels
@param java.util.List values
@param String title = null
@param String canvasId = "barChart"
@param String barColor = "#36A2EB"

!{
    String resolvedTitle = title != null ? title : MessageUtil.getMessage("stock.chart.title.bar.default");
}

<div class="card bg-base-100 border border-base-200 rounded-box shadow-sm h-full">
    <div class="card-body p-4">
        <h2 class="card-title text-base-content/70 text-sm mb-2">${resolvedTitle}</h2>
        <div class="relative w-full amount-chart h-52">
            <canvas id="${canvasId}" class="amount-chart"></canvas>
        </div>
    </div>
    <script>
        (function() {
            if (typeof Chart === 'undefined' || !window.StockCharts) return;
            const canvasId = '${canvasId}';
            const labels = [
                @for(String l : labels)
                    '${l}',
                @endfor
            ];
            const values = [
                @for(Object v : values)
                    $unsafe{v != null ? v.toString() : "0"},
                @endfor
            ];
            var config = {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        label: '${resolvedTitle}',
                        data: values,
                        backgroundColor: '${barColor}',
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        y: { beginAtZero: true, ticks: { callback: v => v.toLocaleString ? v.toLocaleString() : v } }
                    }
                }
            };
            window.ensureStockCharts(function() { window['${canvasId}Instance'] = window.StockCharts.createChart(canvasId, config, window['${canvasId}Instance']); });
        })();
    </script>
</div>
