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
        <div class="flex items-center gap-2 mb-2">
            <h2 class="card-title text-base-content/70 m-0">누적 자산 성장 차트</h2>
            <div class="dropdown dropdown-hover dropdown-right">
                <label tabindex="0" class="btn btn-circle btn-ghost btn-xs text-info">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-5 h-5 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                </label>
                <div tabindex="0" class="card compact dropdown-content z-[10] shadow-lg bg-base-200 rounded-box w-max">
                    <div class="card-body">
                        <h3 class="card-title text-sm mb-1">차트 조작 도움말</h3>
                        <ul class="text-xs list-disc pl-4 space-y-1 whitespace-nowrap">
                            <li><strong>마우스 휠:</strong> 차트 확대 및 축소 (Zoom)</li>
                            <li><strong>클릭 후 드래그:</strong> 선택한 특정 구간 확대</li>
                            <li><strong>Alt + 드래그:</strong> 차트 좌우 이동 (Pan)</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
        <div class="w-full flex flex-col gap-4">
            @if(timeSeries == null || timeSeries.isEmpty())
                <div class="w-full flex justify-center items-center" style="height: 600px;">
                    <p class="text-gray-500">데이터가 없습니다.</p>
                </div>
            @else
                <div class="relative w-full" style="height: 450px;">
                    <canvas id="assetGrowthChart" class="amount-chart"></canvas>
                    
                    <!-- 바로가기 화살표 버튼 -->
                    <button type="button" id="btnScrollStart" class="btn btn-sm btn-circle absolute top-1/2 left-2 transform -translate-y-1/2 bg-base-100/70 shadow hover:bg-base-200 z-10" style="display: none;" onclick="jumpToEdge('start')" title="맨 앞으로 이동">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" /></svg>
                    </button>
                    <button type="button" id="btnScrollEnd" class="btn btn-sm btn-circle absolute top-1/2 right-2 transform -translate-y-1/2 bg-base-100/70 shadow hover:bg-base-200 z-10" style="display: none;" onclick="jumpToEdge('end')" title="맨 위치로 이동">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" /></svg>
                    </button>
                </div>
                <div class="w-full" style="height: 200px;">
                    <canvas id="dividendChart" class="amount-chart"></canvas>
                </div>
            @endif
        </div>
    </div>

    @if(timeSeries != null && !timeSeries.isEmpty())
    <script>
        (function() {
            if (typeof Chart === 'undefined') return;
            var ctxMain = document.getElementById('assetGrowthChart');
            var ctxDividend = document.getElementById('dividendChart');
            if (!ctxMain || !ctxDividend) return;

            var labels = [];
            var totalValueData = [];
            var totalCostData = [];
            var cumulativeTotalProfitData = [];
            var cumulativeDividendData = [];

            @for(TradeProfitTimeSeriesPoint pt : timeSeries)
                labels.push("$unsafe{formatter.format(pt.timestamp())}");
                totalValueData.push($unsafe{pt.totalHoldingsValue() != null ? pt.totalHoldingsValue().toPlainString() : "0"});
                totalCostData.push($unsafe{pt.totalHoldingsCost() != null ? pt.totalHoldingsCost().toPlainString() : "0"});
                cumulativeTotalProfitData.push($unsafe{pt.cumulativeTotalProfit() != null ? pt.cumulativeTotalProfit().toPlainString() : "0"});
                cumulativeDividendData.push($unsafe{pt.cumulativeDividend() != null ? pt.cumulativeDividend().toPlainString() : "0"});
            @endfor

            var maxIndex = labels.length - 1;
            var defaultMinIndex = maxIndex > 365 ? maxIndex - 365 : 0;
            var defaultMinDate = labels[defaultMinIndex];
            var defaultMaxDate = labels[maxIndex];

            var mainChart, dividendChart;

            var checkNavButtons = function() {
                if (!mainChart) return;
                
                // Chart.js 줌/팬 플러그인은 내부적으로 min/max를 index(숫자) 또는 timestamp로 반환할 수 있음
                var xAxis = mainChart.scales.x;
                if (!xAxis) return;
                
                // fallback to options if scales aren't fully resolved, though bounds usually exist
                var xMin = xAxis.min; 
                var xMax = xAxis.max; 
                
                var minIdx = typeof xMin === 'number' ? xMin : labels.indexOf(xMin);
                var maxIdx = typeof xMax === 'number' ? xMax : labels.indexOf(xMax);
                
                // 안전장치
                if (minIdx === -1 || minIdx === undefined) minIdx = 0;
                if (maxIdx === -1 || maxIdx === undefined) maxIdx = labels.length - 1;
                
                var btnStart = document.getElementById('btnScrollStart');
                var btnEnd = document.getElementById('btnScrollEnd');
                
                if (btnStart) {
                    btnStart.style.display = 'inline-flex';
                    btnStart.style.opacity = Math.round(minIdx) > 0 ? '1' : '0';
                    btnStart.style.pointerEvents = Math.round(minIdx) > 0 ? 'auto' : 'none';
                    btnStart.style.zIndex = '50';
                }
                if (btnEnd) {
                    btnEnd.style.display = 'inline-flex';
                    btnEnd.style.opacity = Math.round(maxIdx) < labels.length - 1 ? '1' : '0';
                    btnEnd.style.pointerEvents = Math.round(maxIdx) < labels.length - 1 ? 'auto' : 'none';
                    btnEnd.style.zIndex = '50';
                }
            };

            window.jumpToEdge = function(position) {
                if (!mainChart || !dividendChart || labels.length === 0) return;
                
                var xAxis = mainChart.scales.x;
                var xMin = xAxis.min; 
                var xMax = xAxis.max; 
                
                var minIdx = typeof xMin === 'number' ? xMin : labels.indexOf(xMin);
                var maxIdx = typeof xMax === 'number' ? xMax : labels.indexOf(xMax);
                minIdx = Math.round(minIdx);
                maxIdx = Math.round(maxIdx);
                
                if (minIdx < 0 || isNaN(minIdx)) minIdx = 0;
                if (maxIdx < 0 || isNaN(maxIdx)) maxIdx = labels.length - 1;
                
                var span = maxIdx - minIdx;
                if (span <= 0) span = Math.min(365, labels.length - 1);
                
                var newMinIdx, newMaxIdx;
                if (position === 'start') {
                    newMinIdx = 0;
                    newMaxIdx = Math.min(span, labels.length - 1);
                } else {
                    newMaxIdx = labels.length - 1;
                    newMinIdx = Math.max(0, newMaxIdx - span);
                }
                
                var newMinDate = labels[newMinIdx];
                var newMaxDate = labels[newMaxIdx];
                
                mainChart.options.scales.x.min = newMinDate;
                mainChart.options.scales.x.max = newMaxDate;
                mainChart.update();
                
                dividendChart.options.scales.x.min = newMinDate;
                dividendChart.options.scales.x.max = newMaxDate;
                dividendChart.update();
                
                checkNavButtons();
            };

            var syncZoomPan = function({chart}) {
                var target = (chart.canvas.id === 'assetGrowthChart') ? dividendChart : mainChart;
                if(target) {
                    target.options.scales.x.min = chart.scales.x.min;
                    target.options.scales.x.max = chart.scales.x.max;
                    target.update('none');
                }
                setTimeout(checkNavButtons, 50); // let the chart animation/update settle then check bounds
            };

            var commonZoomOptions = {
                zoom: {
                    wheel: { enabled: true },
                    drag: { enabled: true, backgroundColor: 'rgba(54, 162, 235, 0.3)' },
                    pinch: { enabled: true },
                    mode: 'x',
                    onZoom: syncZoomPan
                },
                pan: {
                    enabled: true,
                    mode: 'x',
                    modifierKey: 'alt',
                    onPan: syncZoomPan
                }
            };

            var yTicksCallback = function(value) {
                if (value >= 100000000 || value <= -100000000) return (value / 100000000).toFixed(1) + '억';
                if (value >= 10000 || value <= -10000) return (value / 10000).toFixed(0) + '만';
                return new Intl.NumberFormat('ko-KR').format(value);
            };

            mainChart = new Chart(ctxMain.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            type: 'line',
                            label: '투자 원금',
                            data: totalCostData,
                            borderColor: 'rgba(201, 203, 207, 1)',
                            borderWidth: 2,
                            borderDash: [5, 5],
                            fill: false,
                            yAxisID: 'y'
                        },
                        {
                            type: 'line',
                            label: '총 자산 평가액',
                            data: totalValueData,
                            borderColor: 'rgba(75, 192, 192, 1)',
                            backgroundColor: 'rgba(75, 192, 192, 0.1)',
                            borderWidth: 2,
                            fill: false,
                            yAxisID: 'y'
                        },
                        {
                            type: 'bar',
                            label: '누적 총 손실/이익(평가포함)',
                            data: cumulativeTotalProfitData,
                            backgroundColor: cumulativeTotalProfitData.map(val => val >= 0 ? 'rgba(255, 99, 132, 0.6)' : 'rgba(54, 162, 235, 0.6)'),
                            borderWidth: 0,
                            yAxisID: 'y1'
                        }
                    ]
                },
                options: {
                    animation: false,
                    normalized: true,
                    elements: {
                        line: { tension: 0 },
                        point: { radius: 0, hitRadius: 10, hoverRadius: 4 }
                    },
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'index',
                        intersect: false
                    },
                    plugins: {
                        zoom: commonZoomOptions,
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    var label = context.dataset.label || '';
                                    if (label) label += ': ';
                                    if (context.parsed.y !== null) {
                                        label += new Intl.NumberFormat('ko-KR').format(context.parsed.y) + '원';
                                    }
                                    return label;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            display: true,
                            min: defaultMinDate,
                            max: defaultMaxDate,
                            ticks: {
                                maxRotation: 45,
                                minRotation: 45,
                                maxTicksLimit: 15
                            }
                        },
                        y: {
                            display: true,
                            position: 'left',
                            beginAtZero: false,
                            title: { display: true, text: '총액' },
                            ticks: { callback: yTicksCallback }
                        },
                        y1: {
                            display: true,
                            position: 'right',
                            grid: { drawOnChartArea: false },
                            title: { display: true, text: '손익' },
                            ticks: { callback: yTicksCallback }
                        }
                    }
                }
            });

            dividendChart = new Chart(ctxDividend.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            type: 'line',
                            label: '누적 배당금',
                            data: cumulativeDividendData,
                            borderColor: 'rgba(255, 159, 64, 1)',
                            backgroundColor: 'rgba(255, 159, 64, 0.2)',
                            borderWidth: 2,
                            fill: 'origin', // 바닥에서부터 채워지는 영역 차트
                            yAxisID: 'y'
                        }
                    ]
                },
                options: {
                    animation: false,
                    normalized: true,
                    elements: {
                        line: { tension: 0 },
                        point: { radius: 0, hitRadius: 10, hoverRadius: 4 }
                    },
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'index',
                        intersect: false
                    },
                    plugins: {
                        zoom: commonZoomOptions,
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    var label = context.dataset.label || '';
                                    if (label) label += ': ';
                                    if (context.parsed.y !== null) {
                                        label += new Intl.NumberFormat('ko-KR').format(context.parsed.y) + '원';
                                    }
                                    return label;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            display: true,
                            min: defaultMinDate,
                            max: defaultMaxDate,
                            ticks: {
                                maxRotation: 45,
                                minRotation: 45,
                                maxTicksLimit: 15
                            }
                        },
                        y: {
                            display: true,
                            position: 'left',
                            beginAtZero: true,
                            title: { display: true, text: '배당금 누적' },
                            ticks: { callback: yTicksCallback }
                        },
                        y1: {
                            display: true,
                            position: 'right',
                            grid: { drawOnChartArea: false },
                            title: { display: true, text: '손익', color: 'transparent' },
                            ticks: { callback: yTicksCallback, color: 'transparent' },
                            border: { display: false }
                        }
                    }
                }
            });
            
            // 초기 버튼 상태 업데이트
            checkNavButtons();
        })();
    </script>
    @endif
</div>
