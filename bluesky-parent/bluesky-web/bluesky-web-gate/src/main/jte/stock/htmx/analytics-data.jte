@import java.util.List
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.controller.StockBaseHtmxController.AnalyticsRow
@import net.luversof.web.gate.stock.controller.StockBaseHtmxController.ChartDataset

@param String totalLabel = null
@param BigDecimal totalValue = null
@param List<AnalyticsRow> tableData
@param String chartType
@param List<String> chartLabels
@param List<ChartDataset> chartDatasets
@param String canvasId
@param String value4Label = null
@param String chartTitle = null
@param String type = "PROFIT"

<!-- 2. Data Content Fragment (Loaded via HTMX) -->
<div class="fade-in space-y-6">

    <!-- 1. Stats Summary -->
    <div class="stats shadow w-full bg-base-100">
        <div class="stat">
            <div class="stat-figure text-primary">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path></svg>
            </div>
            <div class="stat-title">${totalLabel != null ? totalLabel : "총 합계"}</div>
            <div class="stat-value amount-value">${totalValue != null ? String.format("%,d", totalValue.longValue()) : "0"}</div>
            <div class="stat-desc">선택 기간/기준</div>
        </div>
        
        <div class="stat">
            <div class="stat-figure text-secondary">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"></path></svg>
            </div>
            <div class="stat-title">데이터 포인트</div>
            <div class="stat-value text-secondary">${tableData != null ? tableData.size() : 0} 개</div>
            <div class="stat-desc">상세 내역 수</div>
        </div>
    </div>

    <!-- 2. Chart Area (Large) -->
    <div class="card bg-base-100 shadow-xl overflow-hidden">
        <div class="card-body p-4 sm:p-6" style="min-height: 400px;">
            <h2 class="card-title text-base opacity-70 mb-4">${chartTitle != null ? chartTitle : ("bar".equals(chartType) ? "기간별 추이 (Bar)" : "누적 성장 (Line)")}</h2>
            <div class="relative w-full h-[350px] sm:h-[450px]">
                <canvas id="${canvasId}" class="amount-chart"></canvas>
            </div>
        </div>
    </div>

    <!-- 3. Detailed Data Table -->
    <div class="card bg-base-100 shadow-xl">
        <div class="card-body p-0 overflow-x-auto">
            <table class="table table-zebra w-full text-center">
                <!-- Head -->
                <thead class="bg-base-200">
                    <tr>
                        <th class="w-16">No</th>
                        <th>${value4Label != null ? value4Label : "날짜"}</th>
                        <th class="text-left">항목/계좌</th>
                        @if("PROFIT".equals(type))
                            <th class="text-right">매도금액</th>
                            <th class="text-right font-bold">실현손익</th>
                            <th class="text-right hidden sm:table-cell">보유손익</th>
                        @else
                            <th class="text-right">배당금(세전)</th>
                            <th class="text-right font-bold">실지급액</th>
                            <th class="text-right hidden sm:table-cell">세금</th>
                        @endif
                    </tr>
                </thead>
                <tbody>
                    @if(tableData == null || tableData.isEmpty())
                        <tr>
                            <td colspan="6" class="py-6 text-base-content/50">데이터가 없습니다.</td>
                        </tr>
                    @else
                        !{int count = 1;}
                        @for(AnalyticsRow row : tableData)
                            <tr class="hover">
                                <td>${count++}</td>
                                <td class="font-mono text-sm">${row.key()}</td>
                                <td class="text-left font-bold truncate max-w-[150px]" title="${row.subKey()}">${row.subKey()}</td>
                                
                                @if("PROFIT".equals(type))
                                    <!-- 매도금액 - Value5 -->
                                    <td class="text-right font-mono text-sm amount-value">
                                        ${String.format("%,d", row.value5() != null ? row.value5().longValue() : 0)}
                                    </td>
                                    <!-- 실현손익 - Value6 -->
                                    <td class="text-right font-mono font-bold text-sm amount-value ${row.value6() != null && row.value6().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                        ${String.format("%+,d", row.value6() != null ? row.value6().longValue() : 0)}
                                    </td>
                                    <!-- 보유손익 - Value4 -->
                                    <td class="text-right font-mono text-sm hidden sm:table-cell amount-value ${row.value4() != null && row.value4().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                        ${String.format("%+,d", row.value4() != null ? row.value4().longValue() : 0)}
                                    </td>
                                @else
                                    <!-- 배당금(세전) - Value1 -->
                                    <td class="text-right font-mono text-sm amount-value">
                                        ${String.format("%,d", row.value1() != null ? row.value1().longValue() : 0)}
                                    </td>
                                    <!-- 실지급액 - Value2 -->
                                    <td class="text-right font-mono font-bold text-sm amount-value text-profit">
                                        ${String.format("%,d", row.value2() != null ? row.value2().longValue() : 0)}
                                    </td>
                                    <!-- 세금 - Value3 -->
                                    <td class="text-right font-mono text-sm hidden sm:table-cell amount-value text-loss">
                                        ${String.format("%,d", row.value3() != null ? row.value3().longValue() : 0)}
                                    </td>
                                @endif
                            </tr>
                        @endfor
                    @endif
                </tbody>
            </table>
        </div>
    </div>

    <!-- Chart Script -->
    <script>
        (function() {
            const chartId = '${canvasId}';
            const ctx = document.getElementById(chartId);
            if(!ctx) return;
            
            // Cleanup just in case
            if (window.Chart) {
                const existingChart = Chart.getChart(ctx);
                if(existingChart) existingChart.destroy();
            }

            const labels = [];
            @if(chartLabels != null)
                @for(String label : chartLabels)
                    labels.push("${label}");
                @endfor
            @endif

            const datasets = [];
            @if(chartDatasets != null)
                @for(ChartDataset ds : chartDatasets)
                    {
                        const data = [];
                        @if(ds.data() != null)
                            @for(BigDecimal val : ds.data())
                                data.push($unsafe{val != null ? val.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "null"});
                            @endfor
                        @endif
                        
                        datasets.push({
                            label: "${ds.label() != null ? ds.label() : ""}",
                            data: data,
                            borderColor: "${ds.borderColor() != null ? ds.borderColor() : "#000"}",
                            backgroundColor: "${ds.backgroundColor() != null ? ds.backgroundColor() : "#000"}",
                            borderWidth: $unsafe{ds.borderWidth() != null ? String.valueOf(ds.borderWidth()) : "1"},
                            tension: 0.3,
                            fill: false
                        });
                    }
                @endfor
            @endif
            const type = "${chartType != null ? chartType : "bar"}";

            new Chart(ctx, {
                type: type, // 'line' or 'bar'
                data: {
                    labels: labels,
                    datasets: datasets
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'index',
                        intersect: false,
                    },
                    plugins: {
                        legend: {
                            position: 'top',
                            labels: { usePointStyle: true, boxWidth: 8 }
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    let label = context.dataset.label || '';
                                    if (label) {
                                        label += ': ';
                                    }
                                    if (context.parsed.y !== null) {
                                        label += new Intl.NumberFormat('ko-KR').format(context.parsed.y);
                                    }
                                    return label;
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            grid: { color: 'rgba(0, 0, 0, 0.05)' }
                        },
                        x: {
                            grid: { display: false }
                        }
                    }
                }
            });
        })();
    </script>
</div>