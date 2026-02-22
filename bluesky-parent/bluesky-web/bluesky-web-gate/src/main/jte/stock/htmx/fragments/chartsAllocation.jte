@import java.util.Map
@import java.math.BigDecimal

@param Map<String, BigDecimal> allocation

<div class="card bg-base-100 shadow-xl h-full">
    <div class="card-body p-4">
        <h2 class="card-title text-base-content/70 text-sm mb-2">종목 비중</h2>
        <div class="flex h-60">
            <div class="w-1/2 h-full flex items-center justify-center relative">
                <canvas id="allocationChart"></canvas>
            </div>
            <div id="allocationLegend" class="w-1/2 h-full overflow-y-auto pl-2 scrollbar-thin"></div>
        </div>
    </div>
    <script>
        (function() {
            if (typeof Chart === 'undefined') return;
            const ctx = document.getElementById('allocationChart').getContext('2d');
            
            const labels = [
                @for(String key : allocation.keySet())
                    '${key}',
                @endfor
            ];
            const values = [
                @for(BigDecimal val : allocation.values())
                    ${val},
                @endfor
            ];
            
            const colors = [
                '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40',
                '#E7E9ED', '#76A346', '#4D5360', '#F7464A', '#46BFBD', '#FDB45C'
            ];

            new Chart(ctx, {
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
                                    let percentage = ((value / total) * 100).toFixed(1) + '%';
                                    return context.label + ': ' + percentage;
                                }
                            }
                        }
                    },
                    cutout: '60%'
                }
            });

            const legendContainer = document.getElementById('allocationLegend');
            const total = values.reduce((a, b) => a + b, 0);
            let html = '<ul class="space-y-2">';
            labels.forEach((label, index) => {
                const value = values[index];
                const percent = total > 0 ? ((value / total) * 100).toFixed(1) + '%' : '0%';
                const color = colors[index % colors.length];
                html += `
                    <li class="flex justify-between items-start text-xs">
                        <div class="flex items-center gap-2 flex-1 min-w-0 pr-2">
                            <span class="w-3 h-3 rounded-full flex-shrink-0" style="background-color: ` + color + `"></span>
                            <span class="truncate font-medium text-base-content/80" title="` + label + `">` + label + `</span>
                        </div>
                        <div class="text-right flex-shrink-0">
                            <div class="font-bold text-base-content">` + new Intl.NumberFormat("ko-KR").format(value) + `</div>
                            <div class="text-[10px] text-base-content/50">` + percent + `</div>
                        </div>
                    </li>`;
            });
            html += '</ul>';
            legendContainer.innerHTML = html;
        })();
    </script>
</div>