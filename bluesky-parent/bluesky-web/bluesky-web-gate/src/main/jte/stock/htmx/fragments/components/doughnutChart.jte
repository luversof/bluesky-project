@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil

@param List<String> labels
@param java.util.List values
@param String title = null
@param String canvasId = "doughnutChart"
@param String legendId = "doughnutLegend"
@param java.util.List<String> colors = null
@param String cutout = "60%"

!{
    String resolvedTitle = title != null ? title : MessageUtil.getMessage("stock.chart.title.weight.analysis");
}

@template._components.ui.tableCard(cssClass = "h-full", body = @`
    <div class="card-body p-4">
        <h2 class="card-title text-base-content/70 text-sm mb-2">${resolvedTitle}</h2>
        <div class="flex h-60">
            <div class="w-1/2 h-full flex items-center justify-center relative">
                <canvas id="${canvasId}" class="amount-chart"></canvas>
            </div>
            <div id="${legendId}" class="w-1/2 h-full overflow-y-auto pl-2 scrollbar-thin amount-chart"></div>
        </div>
    </div>
    <script>
        (function() {
            if (typeof Chart === 'undefined' || !window.StockCharts) return;
            const canvasEl = document.getElementById('${canvasId}');
            if (!canvasEl) return;
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
            const colors = [
                @if(colors != null && !colors.isEmpty())
                    @for(String c : colors)
                        '${c}',
                    @endfor
                @else
                    '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40',
                    '#E7E9ED', '#76A346', '#4D5360', '#F7464A', '#46BFBD', '#FDB45C'
                @endif
            ];

            var config = {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: values,
                        backgroundColor: colors,
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    let value = context.raw;
                                    let total = context.dataset.data.reduce((a, b) => a + b, 0);
                                    let percentage = total > 0 ? ((value / total) * 100).toFixed(1) + '%' : '0%';
                                    return context.label + ': ' + percentage;
                                }
                            }
                        }
                    },
                    cutout: '${cutout}'
                }
            };
            window['${canvasId}Instance'] = window.StockCharts.createChart(canvasId, config, window['${canvasId}Instance']);

            const legendContainer = document.getElementById('${legendId}');
            const total = values.reduce((a, b) => a + b, 0);
            let html = '<ul class="space-y-2">';
            labels.forEach((label, index) => {
                const value = values[index] || 0;
                const percent = total > 0 ? ((value / total) * 100).toFixed(1) + '%' : '0%';
                const color = colors[index % colors.length];
                html += '\n                    <li class="flex justify-between items-start text-xs">'
                    + '<div class="flex items-center gap-2 flex-1 min-w-0 pr-2">'
                    + '<span class="w-3 h-3 rounded-full flex-shrink-0" style="background-color: ' + color + '"></span>'
                    + '<span class="truncate font-medium text-base-content/80" title="' + label + '">' + label + '</span>'
                    + '</div>'
                    + '<div class="text-right flex-shrink-0">'
                    + '<div class="font-bold text-base-content">' + window.StockCharts.formatNumber(value) + '</div>'
                    + '<div class="text-[10px] text-base-content/50">' + percent + '</div>'
                    + '</div>'
                    + '</li>';
            });
            html += '</ul>';
            if (legendContainer) legendContainer.innerHTML = html;
        })();
    </script>
`)
