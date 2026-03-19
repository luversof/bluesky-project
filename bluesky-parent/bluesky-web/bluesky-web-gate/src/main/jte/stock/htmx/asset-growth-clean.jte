@import java.util.List
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint

@param List<TradeProfitTimeSeriesPoint> timeSeries

!{
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
}

<div class="card bg-base-100 shadow-xl">
    <div class="card-body">
        <h2 class="card-title text-base-content/70">누적 자산 성장 차트</h2>
        <div class="h-96 w-full flex justify-center items-center">
            @if(timeSeries == null || timeSeries.isEmpty())
                <p class="text-gray-500">데이터가 없습니다.</p>
            @else
                <canvas id="assetGrowthChart"></canvas>
            @endif
        </div>
    </div>

    @if(timeSeries != null && !timeSeries.isEmpty())
    <script>
        (function() {
            if (typeof Chart === 'undefined') return;
            const ctx = document.getElementById('assetGrowthChart');
            if (!ctx) return;
            
                                                            const seriesData = [
                @for(TradeProfitTimeSeriesPoint pt : timeSeries)
                    {
                        label: '${formatter.format(pt.timestamp())}',
                        totalValue: ${pt.totalHoldingsValue() != null ? pt.totalHoldingsValue() : 0},
                        totalCost: ${pt.totalHoldingsCost() != null ? pt.totalHoldingsCost() : 0},
                        cumulativeTotal: ${pt.cumulativeTotalProfit() != null ? pt.cumulativeTotalProfit() : 0},
                        daily: ${pt.dailyRealizedProfit() != null ? pt.dailyRealizedProfit() : 0}
                    },
                @endfor
            ];

            const labels = seriesData.map(d => d.label);
            const totalValueData = seriesData.map(d => d.totalValue);
            const totalCostData = seriesData.map(d => d.totalCost);
            const cumulativeTotalProfitData = seriesData.map(d => d.cumulativeTotal);
            const dailyData = seriesData.map(d => d.daily);

            new Chart(ctx.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            type: 'line',
                            label: '총 자산 평가액',
                            data: totalValueData,
                            borderColor: 'rgba(75, 192, 192, 1)',
                            backgroundColor: 'rgba(75, 192, 192, 0.1)',
                            borderWidth: 2,
                            fill: true,
                            tension: 0.1,
                            yAxisID: 'y'
                        },
                        {
                            type: 'line',
                            label: '투자 원금',
                            data: totalCostData,
                            borderColor: 'rgba(201, 203, 207, 1)',
                            borderWidth: 2,
                            borderDash: [5, 5],
                            fill: false,
                            tension: 0.1,
                            yAxisID: 'y'
                        },
                        {
                            type: 'bar',
                            label: '일일 실현 손익',
                            data: dailyData,
                            backgroundColor: 'rgba(54, 162, 235, 0.5)',
                            borderColor: 'rgba(54, 162, 235, 1)',
                            borderWidth: 1,
                            yAxisID: 'y1'
                        },
                        {
                            type: 'line',
                            label: '누적 총 손실/이익(평가포함)',
                            data: cumulativeTotalProfitData,
                            borderColor: 'rgba(153, 102, 255, 1)',
                            backgroundColor: 'rgba(153, 102, 255, 0.2)',
                            borderWidth: 2,
                            fill: true,
                            tension: 0.1,
                            yAxisID: 'y1'
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'index',
                        intersect: false,
                    },
                    plugins: {
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    let label = context.dataset.label || '';
                                    if (label) {
                                        label += ': ';
                                    }
                                    if (context.parsed.y !== null) {
                                        label += new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(context.parsed.y);
                                    }
                                    return label;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            display: true
                        },
                        y: {
                            display: true,
                            position: 'left',
                            title: { display: true, text: '총액' }
                        },
                        y1: {
                            display: true,
                            position: 'right',
                            grid: { drawOnChartArea: false },
                            title: { display: true, text: '손익' }
                        }
                    }
                }
            });
        })();
    </script>
    @endif
</div>


