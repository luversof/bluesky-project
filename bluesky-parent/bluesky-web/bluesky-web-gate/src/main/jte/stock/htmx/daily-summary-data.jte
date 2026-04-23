@import java.util.List
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.controller.StockBaseHtmxController.AnalyticsRow
@import net.luversof.web.gate.stock.controller.StockBaseHtmxController.ChartDataset

@param String chartTitle
@param String keyLabel
@param String subKeyLabel = null
@param String value1Label = null
@param String value2Label = null
@param String value3Label = null
@param String value4Label = null
@param String value5Label = null
@param String value6Label = null
@param String totalLabel = null
@param List<AnalyticsRow> tableData
@param String chartType
@param List<String> chartLabels
@param List<ChartDataset> chartDatasets
@param String canvasId

<!-- 2. Data Content Fragment -->
<div class="flex flex-col gap-6 animate-fade-in">
    
    @if(tableData != null && tableData.size() == 1 && subKeyLabel == null)
        <!-- KPI Cards Area (Summary View) -->
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            !{var row = tableData.get(0);}
            
            @if(value1Label != null && !value1Label.isEmpty())
            <div class="stat bg-base-100 shadow-xl rounded-box p-4">
                <div class="stat-title text-xs opacity-70">${value1Label}</div>
                <div class="stat-value amount-value text-lg mt-1 font-mono ${row.value1() != null && row.value1().compareTo(BigDecimal.ZERO) >= 0 ? "text-principal" : "text-loss"}">
                    ${String.format("%,d", row.value1() != null ? row.value1().longValue() : 0)}
                </div>
            </div>
            @endif
            
            @if(value2Label != null && !value2Label.isEmpty())
            <div class="stat bg-base-100 shadow-xl rounded-box p-4">
                <div class="stat-title text-xs opacity-70">${value2Label}</div>
                <div class="stat-value amount-value text-lg mt-1 font-mono ${row.value2() != null && row.value2().compareTo(BigDecimal.ZERO) >= 0 ? "text-principal" : "text-loss"}">
                    ${String.format("%,d", row.value2() != null ? row.value2().longValue() : 0)}
                </div>
            </div>
            @endif
            
            @if(value3Label != null && !value3Label.isEmpty())
            <div class="stat bg-base-100 shadow-xl rounded-box p-4">
                <div class="stat-title text-xs opacity-70">${value3Label}</div>
                <div class="stat-value amount-value text-lg mt-1 font-mono ${row.value3() != null && row.value3().compareTo(BigDecimal.ZERO) >= 0 ? "text-principal" : "text-loss"}">
                    ${String.format("%,d", row.value3() != null ? row.value3().longValue() : 0)}
                </div>
            </div>
            @endif
            
            @if(value4Label != null && !value4Label.isEmpty())
            <div class="stat bg-base-100 shadow-xl rounded-box p-4">
                <div class="stat-title text-xs opacity-70">${value4Label}</div>
                <div class="stat-value amount-value text-lg mt-1 font-mono ${row.value4() != null && row.value4().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                    ${String.format("%,d", row.value4() != null ? row.value4().longValue() : 0)}
                </div>
            </div>
            @endif
            
            @if(value5Label != null && !value5Label.isEmpty())
            <div class="stat bg-base-100 shadow-xl rounded-box p-4">
                <div class="stat-title text-xs opacity-70">${value5Label}</div>
                <div class="stat-value amount-value text-lg mt-1 font-mono">
                    ${String.format("%,d", row.value5() != null ? row.value5().longValue() : 0)}
                </div>
            </div>
            @endif
            
            @if(value6Label != null && !value6Label.isEmpty())
            <div class="stat bg-base-100 shadow-xl rounded-box p-4">
                <div class="stat-title text-xs opacity-70">${value6Label}</div>
                <div class="stat-value amount-value text-lg mt-1 font-mono ${row.value6() != null && row.value6().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                    ${String.format("%,d", row.value6() != null ? row.value6().longValue() : 0)}
                </div>
            </div>
            @endif
            
            @if(totalLabel != null && !totalLabel.isEmpty())
            !{BigDecimal total = row.value7() != null ? row.value7() : (row.value4() != null ? row.value4() : BigDecimal.ZERO).add(row.value6() != null ? row.value6() : BigDecimal.ZERO);}
            <div class="stat bg-base-100 shadow-xl rounded-box p-4">
                <div class="stat-title text-xs opacity-70 font-bold">${totalLabel}</div>
                <div class="stat-value amount-value text-lg mt-1 font-mono font-bold ${total.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                    ${String.format("%,d", total.longValue())}
                </div>
            </div>
            @endif
        </div>
    @else
        <!-- Chart Area -->
        <div class="card bg-base-100 shadow-xl w-full">
            <div class="card-body">
                <h2 class="card-title text-sm opacity-70">${chartTitle}</h2>
                <div class="h-[300px] w-full relative">
                     <canvas id="${canvasId}" class="amount-chart"></canvas>
                </div>
                <div class="text-xs text-right opacity-50 mt-2">* 상위 20개 항목만 표시됩니다.</div>
            </div>
        </div>
    @endif

    <!-- Summary Table Area -->
    <div class="card bg-base-100 shadow-xl w-full">
        <div class="card-body overflow-x-auto p-0 scrollbar-thin">
            <table class="table table-sm table-pin-rows">
                <thead>
                    <tr class="bg-base-200">
                        <th>${keyLabel}</th>
                        <!-- Dynamic Sub Key -->
                        @if(subKeyLabel != null)
                            <th>${subKeyLabel}</th>
                        @endif
                        @if(value1Label != null && !value1Label.isEmpty())
                            <th class="text-right">${value1Label}</th>
                        @endif
                        @if(value2Label != null && !value2Label.isEmpty())
                            <th class="text-right">${value2Label}</th>
                        @endif
                        @if(value3Label != null && !value3Label.isEmpty())
                            <th class="text-right">${value3Label}</th>
                        @endif
                        @if(value4Label != null && !value4Label.isEmpty())
                            <th class="text-right">${value4Label}</th>
                        @endif
                        @if(value5Label != null && !value5Label.isEmpty())
                            <th class="text-right">${value5Label}</th>
                        @endif
                        @if(value6Label != null && !value6Label.isEmpty())
                            <th class="text-right">${value6Label}</th>
                        @endif
                        @if(totalLabel != null && !totalLabel.isEmpty())
                            <th class="text-right">${totalLabel}</th>
                        @endif
                    </tr>
                </thead>
                <tbody>
                    @if(tableData == null || tableData.isEmpty())
                        <tr>
                            <td colspan="7" class="text-center py-10 text-base-content/50">
                                <div>데이터가 없습니다.</div>
                                <div class="text-xs mt-1">기간이나 조건을 변경해보세요.</div>
                            </td>
                        </tr>
                    @else
                        @for(AnalyticsRow row : tableData)
                            <tr class="hover">
                                <td class="font-medium">${row.key()}</td>
                                @if(subKeyLabel != null)
                                    <td class="text-sm opacity-80">${row.subKey()}</td>
                                @endif
                                
                                <!-- Value 1 -->
                                @if(value1Label != null && !value1Label.isEmpty())
                                    <td class="text-right amount-value font-mono whitespace-nowrap ${row.value1() != null && row.value1().compareTo(BigDecimal.ZERO) >= 0 ? "text-principal" : "text-loss"}">
                                        ${String.format("%,d", row.value1() != null ? row.value1().longValue() : 0)}
                                    </td>
                                @endif

                                <!-- Value 2 (Optional) -->
                                @if(value2Label != null && !value2Label.isEmpty())
                                    <td class="text-right amount-value font-mono whitespace-nowrap ${row.value2() != null && row.value2().compareTo(BigDecimal.ZERO) >= 0 ? "text-principal" : "text-loss"}">
                                        ${String.format("%,d", row.value2() != null ? row.value2().longValue() : 0)}
                                    </td>
                                @endif

                                <!-- Value 3 (Optional) -->
                                @if(value3Label != null && !value3Label.isEmpty())
                                    <td class="text-right amount-value font-mono whitespace-nowrap ${row.value3() != null && row.value3().compareTo(BigDecimal.ZERO) >= 0 ? "text-principal" : "text-loss"}">
                                        ${String.format("%,d", row.value3() != null ? row.value3().longValue() : 0)}
                                    </td>
                                @endif

                                <!-- Value 4 -->
                                @if(value4Label != null && !value4Label.isEmpty())
                                    <td class="text-right amount-value font-mono whitespace-nowrap ${row.value4() != null && row.value4().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                        ${String.format("%,d", row.value4() != null ? row.value4().longValue() : 0)}
                                    </td>
                                @endif

                                <!-- Value 5 -->
                                @if(value5Label != null && !value5Label.isEmpty())
                                    <td class="text-right amount-value font-mono whitespace-nowrap">
                                        ${String.format("%,d", row.value5() != null ? row.value5().longValue() : 0)}
                                    </td>
                                @endif

                                <!-- Value 6 -->
                                @if(value6Label != null && !value6Label.isEmpty())
                                    <td class="text-right amount-value font-mono whitespace-nowrap ${row.value6() != null && row.value6().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                        ${String.format("%,d", row.value6() != null ? row.value6().longValue() : 0)}
                                    </td>
                                @endif

                                <!-- Total (Conditional) -->
                                @if(totalLabel != null && !totalLabel.isEmpty())
                                    !{BigDecimal total = row.value7() != null ? row.value7() : (row.value4() != null ? row.value4() : BigDecimal.ZERO).add(row.value6() != null ? row.value6() : BigDecimal.ZERO);}
                                    <td class="text-right amount-value font-bold text-base-content whitespace-nowrap ${total.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                        ${String.format("%,d", total.longValue())}
                                    </td>
                                @endif
                            </tr>
                        @endfor
                    @endif
                </tbody>
                <!-- Footer Total -->
                @if(tableData != null && !tableData.isEmpty())
                    <tfoot class="bg-base-200 font-bold">
                        <tr>
                            <td colspan="${subKeyLabel != null ? 2 : 1}" class="text-center text-base-content">합계</td>
                            
                            <!-- Total Value 1 -->
                            @if(value1Label != null && !value1Label.isEmpty())
                                !{BigDecimal sum1 = tableData.stream().map(r -> r.value1() != null ? r.value1() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);}
                                <td class="text-right amount-value text-principal">
                                    ${String.format("%,d", sum1.longValue())}
                                </td>
                            @endif

                            <!-- Total Value 2 -->
                            @if(value2Label != null && !value2Label.isEmpty())
                                !{BigDecimal sum2 = tableData.stream().map(r -> r.value2() != null ? r.value2() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);}
                                <td class="text-right amount-value text-principal">
                                    ${String.format("%,d", sum2.longValue())}
                                </td>
                            @endif

                            <!-- Total Value 3 -->
                            @if(value3Label != null && !value3Label.isEmpty())
                                !{BigDecimal sum3 = tableData.stream().map(r -> r.value3() != null ? r.value3() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);}
                                <td class="text-right amount-value text-principal">
                                    ${String.format("%,d", sum3.longValue())}
                                </td>
                            @endif

                            <!-- Total Value 4 -->
                            @if(value4Label != null && !value4Label.isEmpty())
                                !{BigDecimal sum4 = tableData.stream().map(r -> r.value4() != null ? r.value4() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);}
                                <td class="text-right amount-value ${sum4.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                    ${String.format("%,d", sum4.longValue())}
                                </td>
                            @endif

                            <!-- Total Value 5 -->
                            @if(value5Label != null && !value5Label.isEmpty())
                                !{BigDecimal sum5 = tableData.stream().map(r -> r.value5() != null ? r.value5() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);}
                                <td class="text-right amount-value">
                                    ${String.format("%,d", sum5.longValue())}
                                </td>
                            @endif

                            <!-- Total Value 6 -->
                            @if(value6Label != null && !value6Label.isEmpty())
                                !{BigDecimal sum6 = tableData.stream().map(r -> r.value6() != null ? r.value6() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);}
                                <td class="text-right amount-value ${sum6.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                    ${String.format("%,d", sum6.longValue())}
                                </td>
                            @endif

                            <!-- Total -->
                            @if(totalLabel != null && !totalLabel.isEmpty())
                                !{BigDecimal sumTotal = tableData.stream().map(r -> r.value7() != null ? r.value7() : (r.value4() != null ? r.value4() : BigDecimal.ZERO).add(r.value6() != null ? r.value6() : BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);}
                                <td class="text-right amount-value ${sumTotal.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                    ${String.format("%,d", sumTotal.longValue())}
                                </td>
                            @endif
                        </tr>
                    </tfoot>
                @endif
            </table>
        </div>
    </div>
</div>

<!-- Chart Initialization Script -->
<script>
    (function() {
        // Destroy existing chart if any
        if (window.dailySummaryChartInstance) {
            window.dailySummaryChartInstance.destroy();
            window.dailySummaryChartInstance = null;
        }

        const canvasId = '${canvasId}';
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;

        const chartType = '${chartType}';
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
                            data.push($unsafe{val != null ? val.toPlainString() : "null"});
                        @endfor
                    @endif
                    
                    const dataset = {
                        label: "${ds.label() != null ? ds.label() : ""}",
                        data: data,
                        backgroundColor: "${ds.backgroundColor() != null ? ds.backgroundColor() : "#000"}",
                        borderColor: "${ds.borderColor() != null ? ds.borderColor() : "#000"}",
                        borderWidth: $unsafe{ds.borderWidth() != null ? String.valueOf(ds.borderWidth()) : "1"},
                        fill: false,
                        tension: 0.1
                    };
                    
                    @if(ds.borderDash() != null && !ds.borderDash().isEmpty())
                        const borderDash = [];
                        @for(Integer dash : ds.borderDash())
                            borderDash.push($unsafe{String.valueOf(dash)});
                        @endfor
                        dataset.borderDash = borderDash;
                    @endif
                    
                    datasets.push(dataset);
                }
            @endfor
        @endif

        var config = {
            type: chartType,
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: "index",
                    intersect: false,
                },
                plugins: {
                    legend: {
                        position: "top",
                        labels: {
                            usePointStyle: true,
                            boxWidth: 8
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                let label = context.dataset.label || "";
                                if (label) {
                                    label += ": ";
                                }
                                if (context.parsed.y !== null) {
                                    label += new Intl.NumberFormat("ko-KR").format(context.parsed.y);
                                }
                                return label;
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                if (value >= 100000000 || value <= -100000000) {
                                    return (value / 100000000).toFixed(1) + "억";
                                } else if (value >= 10000 || value <= -10000) {
                                    return (value / 10000).toFixed(0) + "만";
                                }
                                return new Intl.NumberFormat("ko-KR").format(value);
                            }
                        }
                    }
                }
            }
        };

        window.dailySummaryChartInstance = window.StockCharts.createChart(canvasId, config, window.dailySummaryChartInstance);
    })();
</script>