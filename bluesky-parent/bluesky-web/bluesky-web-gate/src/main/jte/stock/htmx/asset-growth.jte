@import java.util.List
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint
@import io.github.luversof.boot.context.support.MessageUtil

@param Object timeSeries
@param String rangeMode
@param String dataFirstDate = null
@param java.time.Instant startDate = null
@param java.time.Instant endDate = null
@param String timeZone = null
@param java.util.List<net.luversof.web.gate.stock.domain.Account> accountList = null
@param java.util.List<net.luversof.web.gate.stock.domain.StockItem> stockItemList = null
@param java.util.List<String> stockTagList = java.util.List.of()
@param java.util.List<java.util.UUID> selectedAccountIds = java.util.List.of()
@param java.util.List<java.util.UUID> selectedStockItemIds = java.util.List.of()
@param java.util.List<String> selectedStockTags = java.util.List.of()

!{
    boolean isAllMode = "all".equals(rangeMode);
    java.time.ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? java.time.ZoneId.of(timeZone) : java.time.ZoneId.systemDefault();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone);

    java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endDisplayLocal = endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1) : null;

    // Normalize model-provided `timeSeries` into `ts` for template use
    List<TradeProfitTimeSeriesPoint> ts = null;
    if (timeSeries instanceof List) {
        ts = (List<TradeProfitTimeSeriesPoint>) timeSeries;
    }

    java.time.LocalDate today = java.time.LocalDate.now(zone);
    boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
    boolean canPrev = !isAllMode && startLocal != null && dataFirstDate != null && !dataFirstDate.isEmpty()
            && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));

    String assetGrowthTitle = MessageUtil.getMessage("stock.page.asset.growth.title");
    String noDataLabel = MessageUtil.getMessage("common.message.no.data");
    String chartDescriptionLabel = MessageUtil.getMessage("stock.asset.growth.chart.description");
    String clickDateHoldingsLabel = MessageUtil.getMessage("stock.asset.growth.chart.click.date");
    String profitAnalysisLabel = MessageUtil.getMessage("stock.asset.growth.profit.analysis");
    String principalLabel = MessageUtil.getMessage("stock.label.principal");
    String totalAssetDatasetLabel = MessageUtil.getMessage("stock.asset.growth.dataset.total.asset");
    String cumulativeRealizedDatasetLabel = MessageUtil.getMessage("stock.asset.growth.dataset.cumulative.realized");
    String cumulativeDividendLabel = MessageUtil.getMessage("stock.summary.label.cumulative.dividend");
    String cumulativeTotalDatasetLabel = MessageUtil.getMessage("stock.asset.growth.dataset.cumulative.total");
    String unrealizedProfitLabel = MessageUtil.getMessage("stock.asset.growth.tooltip.unrealized.profit");
    String clickTradeHistoryLabel = MessageUtil.getMessage("stock.asset.growth.tooltip.click.trade.history");
    String tradeCountLabel = MessageUtil.getMessage("stock.asset.growth.tooltip.trade.count");
    String dailyRealizedProfitLabel = MessageUtil.getMessage("stock.asset.growth.tooltip.daily.realized.profit");
    String totalAssetAxisLabel = MessageUtil.getMessage("stock.asset.growth.axis.total.asset");
    String cumulativeProfitAxisLabel = MessageUtil.getMessage("stock.asset.growth.axis.cumulative.profit");

    // Detail search filter labels (reuse realized-profit keys)
    String detailSearchLabel = MessageUtil.getMessage("common.label.detail") + " " + MessageUtil.getMessage("common.button.search");
    String periodLabel = MessageUtil.getMessage("common.label.period");
    String accountLabel = MessageUtil.getMessage("common.label.account");
    String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
    String tagLabel = MessageUtil.getMessage("common.label.tag");
    String tagHelperLabel = MessageUtil.getMessage("stock.filter.tag.helper");
    String tagSummaryLabel = MessageUtil.getMessage("stock.filter.tag.summary.selected");
    String tagStockSummaryPattern = MessageUtil.getMessage("stock.filter.tag.summary.stocks");
    String tagMorePattern = MessageUtil.getMessage("stock.filter.tag.summary.more");
    String allLabel = MessageUtil.getMessage("common.label.all");
    String searchLabel = MessageUtil.getMessage("common.button.search");
    String resetLabel = MessageUtil.getMessage("common.button.reset");
}

<div id="assetGrowthFragment" class="card bg-base-100 border border-base-200 rounded-box shadow-sm">
    <div class="card-body">
        <div class="flex items-center gap-2 mb-2">
            <h2 class="card-title text-base-content/70 m-0">${assetGrowthTitle}</h2>
        </div>

        @template.stock.htmx.fragments.dateRangeNavBar(
            pickerName = "assetPicker",
            btnClass = "asset-range-btn",
            canPrev = canPrev,
            canNext = canNext,
            rangeMode = rangeMode,
            startDate = startLocal,
            endDate = endLocal,
            showRangeText = false
        )

        <details class="mb-4" open>
            <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
                ${detailSearchLabel}
                @template.stock.htmx.fragments.components.filterBadge(
                    startDate = startDate,
                    endDate = endDate
                )
            </summary>
            <form id="assetGrowthSearchForm" class="mt-2 mb-2 rounded-2xl bg-base-100 p-3 shadow-sm"
                  hx-get="/stock/htmx/asset-growth/view"
                  hx-target="#assetGrowthFragment"
                  hx-select="#assetGrowthFragment"
                  hx-swap="outerHTML"
                  hx-trigger="submit">
                <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(0,18rem)_minmax(0,22rem)_minmax(0,1fr)_minmax(0,19rem)] xl:items-start">
                    <div class="form-control w-full">
                        <label class="label p-1"><span class="label-text text-xs">${periodLabel}</span></label>
                        <div class="join w-full">
                            <input type="date" name="assetUiStartDate" id="assetStartDateInput" class="input input-sm input-bordered join-item w-1/2" value="${startLocal != null ? startLocal.toString() : ""}">
                            <input type="date" name="assetUiEndDate" id="assetEndDateInput" class="input input-sm input-bordered join-item w-1/2" value="${endDisplayLocal != null ? endDisplayLocal.toString() : ""}">
                        </div>
                    </div>

                    <div class="form-control w-full">
                        <label class="label p-1"><span class="label-text text-xs">${accountLabel}</span></label>
                        <select name="accountIdList" class="select select-bordered w-full max-h-40 overflow-auto" multiple size="${Math.min(accountList != null ? accountList.size() + 1 : 1, 7)}" data-max-visible="7">
                            <option value="">${allLabel}</option>
                            @if(accountList != null)
                                @for(net.luversof.web.gate.stock.domain.Account account : accountList)
                                    <option value="${account.id().toString()}" selected="${selectedAccountIds != null && selectedAccountIds.contains(account.id())}">${account.name()}</option>
                                @endfor
                            @endif
                        </select>
                    </div>

                    <div class="form-control w-full">
                        <label class="label p-1"><span class="label-text text-xs">${stockNameLabel}</span></label>
                        <select name="stockItemIdList" class="select select-bordered w-full max-h-40 overflow-auto bg-base-100" multiple size="${Math.min(stockItemList != null ? stockItemList.size() + 1 : 1, 7)}" data-max-visible="7">
                            <option value="">${allLabel}</option>
                            @if(stockItemList != null)
                                @for(net.luversof.web.gate.stock.domain.StockItem stockItem : stockItemList)
                                    !{String stockItemTags = stockItem.tags() != null ? String.join("|", stockItem.tags()) : "";}
                                    <option value="${stockItem.id().toString()}"
                                            selected="${selectedStockItemIds != null && selectedStockItemIds.contains(stockItem.id())}"
                                            data-stock-tags="${stockItemTags}">${stockItem.name()}</option>
                                @endfor
                            @endif
                        </select>
                    </div>

                    <div class="flex h-full min-h-0 flex-col gap-3 xl:justify-between">
                        @template.stock.htmx.fragments.components.stockSelectionPanel(
                            tagLabel = tagLabel,
                            stockLabel = stockNameLabel,
                            allLabel = allLabel,
                            helperLabel = tagHelperLabel,
                            selectedTagSummaryLabel = tagSummaryLabel,
                            selectedStockSummaryPattern = tagStockSummaryPattern,
                            stockPreviewMorePattern = tagMorePattern,
                            stockTagList = stockTagList,
                            selectedStockTags = selectedStockTags,
                            stockItemList = stockItemList
                        )

                        <div class="flex justify-end gap-2 xl:mt-auto">
                            <button type="button" class="btn btn-sm btn-primary" data-selection-save="true"
                                hx-get="/stock/htmx/asset-growth/view"
                                hx-target="#assetGrowthFragment"
                                hx-select="#assetGrowthFragment"
                                hx-swap="outerHTML"
                                hx-include="#assetGrowthSearchForm">${searchLabel}</button>
                            <button type="button" class="btn btn-sm text-xs"
                                hx-get="/stock/htmx/asset-growth/view"
                                hx-target="#assetGrowthFragment"
                                hx-select="#assetGrowthFragment"
                                hx-swap="outerHTML"
                                hx-include="#assetGrowthSearchForm"
                                data-selection-reset="true">${resetLabel}</button>
                        </div>
                    </div>
                </div>
                <input type="hidden" name="startDate" id="assetStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
                <input type="hidden" name="endDate" id="assetEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
                <input type="hidden" name="timeZone" id="assetTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
                <input type="hidden" name="rangeMode" id="assetRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">
            </form>
        </details>

        <script type="module" src="/js/stock/selectionStorage.js"></script>
        <script type="module" src="/js/stock/tagFilterChips.js"></script>

        <div class="w-full flex flex-col gap-4">
            @if(ts == null || ts.isEmpty())
                <div class="w-full flex justify-center items-center" style="height: 600px;">
                    <p class="text-gray-500">${noDataLabel}</p>
                </div>
            @else
                <p class="min-w-0 text-xs text-base-content/50">
                    <span>📈 ${chartDescriptionLabel} &bull;</span>
                    <span class="inline-block whitespace-nowrap text-info">${clickDateHoldingsLabel}</span>
                </p>
                <div id="assetGrowthPeriodReturnSummary" class="rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3 mb-3"
                     hx-get="/stock/htmx/asset-growth/period-return?from=${startLocal != null ? startLocal.toString() : ""}&to=${endDisplayLocal != null ? endDisplayLocal.toString() : ""}&timeZone=${timeZone != null ? timeZone : ""}"
                     hx-trigger="load"
                     hx-swap="outerHTML">
                    <div class="skeleton h-4 w-32 mb-2"></div>
                    <div class="skeleton h-8 w-24 mb-2"></div>
                    <div class="skeleton h-3 w-56"></div>
                </div>
                <div class="w-full relative" style="height: 450px;">
                    <canvas id="assetGrowthChart" class="amount-chart"></canvas>
                </div>
                <p class="text-xs text-base-content/50 text-center py-1">💰 ${profitAnalysisLabel} &mdash; <span class="text-indigo-400">■</span> ${cumulativeRealizedDatasetLabel} + <span class="text-green-400">■</span> ${cumulativeDividendLabel} = ${cumulativeTotalDatasetLabel}</p>
                <div class="w-full" style="height: 250px;">
                    <canvas id="dividendChart" class="amount-chart"></canvas>
                </div>
                <div id="holdings-snapshot-container"></div>
                <div id="trade-history-panel"
                     hx-get="/stock/htmx/trade-history?from=${startLocal != null ? startLocal.toString() : ""}&to=${endDisplayLocal != null ? endDisplayLocal.toString() : ""}"
                     hx-trigger="load"
                     hx-swap="outerHTML"></div>
            @endif
        </div>
    </div>

    <script>
    (function() {
        var assetPicker = DateRangePicker.create({
            formId:         'assetGrowthSearchForm',
            startId:        'assetStartDateInput',
            endId:          'assetEndDateInput',
            instantStartId: 'assetStartInstantInput',
            instantEndId:   'assetEndInstantInput',
            timeZoneId:     'assetTimeZoneInput',
            rangeModeId:    'assetRangeModeInput',
            btnClass:       'asset-range-btn',
            minDate:        '${dataFirstDate != null ? dataFirstDate : ""}',
            globalKey:      'globalDateRange',
            rootSelector:   '#assetGrowthFragment'
        });
        window.assetPicker = assetPicker;
        // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
        try {
            if (window.assetPicker && typeof window.assetPicker.getState === 'function') {
                var __s = window.assetPicker.getState();
                if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
                    var __key = 'dateRangeDefaultApplied:assetPicker';
                    try {
                        if (!sessionStorage.getItem(__key)) {
                            var _btn = document.querySelector('#assetGrowthFragment .asset-range-btn');
                            window.assetPicker.set('mtd', _btn || null);
                            sessionStorage.setItem(__key, '1');
                        }
                    } catch(e) { window.assetPicker.set('mtd', null); }
                }
            }
        } catch(e) {}
    })();
    </script>

    @if(ts != null && !ts.isEmpty())
    <script>
        (function() {
            if (typeof Chart === 'undefined') return;
            var ctxMain = document.getElementById('assetGrowthChart');
            var ctxDividend = document.getElementById('dividendChart');
            if (!ctxMain || !ctxDividend) return;

            var labels = [];
            var totalValueData = [];
            var totalCostData = [];
            var cumulativeRealizedProfitData = [];
            var cumulativeDividendData = [];
            var tradeCountData = [];
            var buyCountData = [];
            var dailyRealizedProfitData = [];

            @for(Object _pt : ts)
            !{ TradeProfitTimeSeriesPoint pt = (TradeProfitTimeSeriesPoint) _pt; }
                labels.push("$unsafe{formatter.format(pt.timestamp())}");
                totalValueData.push($unsafe{pt.totalHoldingsValue() != null ? pt.totalHoldingsValue().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                totalCostData.push($unsafe{pt.totalHoldingsCost() != null ? pt.totalHoldingsCost().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                cumulativeRealizedProfitData.push($unsafe{pt.cumulativeRealizedProfit() != null ? pt.cumulativeRealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                cumulativeDividendData.push($unsafe{pt.cumulativeDividend() != null ? pt.cumulativeDividend().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                tradeCountData.push($unsafe{String.valueOf(pt.tradeCount())});
                buyCountData.push($unsafe{String.valueOf(pt.buyCount())});
                dailyRealizedProfitData.push($unsafe{pt.dailyRealizedProfit() != null ? pt.dailyRealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
            @endfor

            // 현재 평가 손익(미실현) = 총 자산 평가액 - 투자 원금
            var currentUnrealizedProfitData = totalValueData.map(function(v, i) {
                return parseFloat(v) - parseFloat(totalCostData[i]);
            });
            // 총 누적 이익 = 누적 결산 손익 + 누적 배당금
            var totalCumulativeProfitData = cumulativeRealizedProfitData.map(function(v, i) {
                return parseFloat(v) + parseFloat(cumulativeDividendData[i]);
            });

            var yTicksCallback = function(value) {
                if (window.StockCharts) return window.StockCharts.formatCompactNumber(value);
                return new Intl.NumberFormat().format(value);
            };

            var locale = window.StockCharts ? window.StockCharts.getLocale() : undefined;
            var percentFormatter = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
            var formatNumber = function(value) {
                return window.StockCharts ? window.StockCharts.formatNumber(value) : new Intl.NumberFormat(locale).format(value);
            };
            var formatCurrency = function(value) {
                return window.StockCharts ? window.StockCharts.formatCurrency(Math.round(value)) : ('₩' + Math.round(value).toLocaleString());
            };
            var formatSignedCurrency = function(value) {
                var rounded = Math.round(value || 0);
                var sign = rounded >= 0 ? '+' : '-';
                return sign + formatCurrency(Math.abs(rounded));
            };
            var formatSignedPercent = function(value) {
                var sign = value >= 0 ? '+' : '-';
                return sign + percentFormatter.format(Math.abs(value)) + '%';
            };

            // 매수/매도 마커 데이터 생성: buyCount > 0 이면 매수(▲), dailyRealizedProfit > 0 이면 매도(▼)
            var buildBuyMarkers = function(lbls, bcounts) {
                var pts = [];
                for (var i = 0; i < lbls.length; i++) {
                    if (Number(bcounts[i]) > 0) {
                        pts.push({ date: lbls[i] });
                    }
                }
                return pts;
            };
            var buildSellMarkers = function(lbls, drealized) {
                var pts = [];
                for (var i = 0; i < lbls.length; i++) {
                    if (parseFloat(drealized[i]) > 0) {
                        pts.push({ date: lbls[i] });
                    }
                }
                return pts;
            };

            var buyMarkersData = buildBuyMarkers(labels, buyCountData);
            var sellMarkersData = buildSellMarkers(labels, dailyRealizedProfitData);
            var drawnMarkers = []; // afterDatasetsDraw에서 갱신, { px, py, date } 형태

            var syncing = false;
            var mainChart, dividendChart;

            var tradeMarkersPlugin = {
                id: 'tradeMarkers',
                afterDatasetsDraw: function(chart) { // tooltip 아래에 그리기 위해 afterDatasetsDraw 사용
                    var ctx = chart.ctx;
                    var xAxis = chart.scales['x'];
                    var yAxis = chart.scales['y'];
                    if (!xAxis || !yAxis) return;
                    var chartLeft = chart.chartArea.left;
                    var chartRight = chart.chartArea.right;
                    var arrowSize = 7;
                    // 꼬리 없는 삼각 화살표: 매수는 라인 아래(위를 가리킴 ▲), 매도는 라인 위(아래를 가리킴 ▼)
                    function drawArrow(cx, cy, up, color) {
                        ctx.save();
                        ctx.fillStyle = color;
                        ctx.beginPath();
                        if (up) {
                            ctx.moveTo(cx, cy);
                            ctx.lineTo(cx + arrowSize, cy + arrowSize * 1.4);
                            ctx.lineTo(cx - arrowSize, cy + arrowSize * 1.4);
                        } else {
                            ctx.moveTo(cx, cy);
                            ctx.lineTo(cx + arrowSize, cy - arrowSize * 1.4);
                            ctx.lineTo(cx - arrowSize, cy - arrowSize * 1.4);
                        }
                        ctx.closePath();
                        ctx.fill();
                        ctx.restore();
                    }
                    var buyData = buyMarkersData;
                    var sellData = sellMarkersData;
                    drawnMarkers = [];
                    for (var i = 0; i < buyData.length; i++) {
                        var xi = labels.indexOf(buyData[i].date); // 날짜로 현재 인덱스 조회
                        if (xi < 0) continue;
                        var pxi = xAxis.getPixelForValue(xi);
                        if (pxi < chartLeft || pxi > chartRight) continue;
                        var costI = parseFloat(totalCostData[xi]);
                        if (isNaN(costI)) continue;
                        var pyi = yAxis.getPixelForValue(costI);
                        drawArrow(pxi, pyi + 2, true, 'rgba(255, 99, 132, 0.9)');
                        drawnMarkers.push({ px: pxi, py: pyi + 2 + arrowSize * 0.7, date: buyData[i].date });
                    }
                    for (var j = 0; j < sellData.length; j++) {
                        var xj = labels.indexOf(sellData[j].date); // 날짜로 현재 인덱스 조회
                        if (xj < 0) continue;
                        var pxj = xAxis.getPixelForValue(xj);
                        if (pxj < chartLeft || pxj > chartRight) continue;
                        var costJ = parseFloat(totalCostData[xj]);
                        if (isNaN(costJ)) continue;
                        var pyj = yAxis.getPixelForValue(costJ);
                        drawArrow(pxj, pyj - 2, false, 'rgba(54, 162, 235, 0.9)');
                        drawnMarkers.push({ px: pxj, py: pyj - 2 - arrowSize * 0.7, date: sellData[j].date });
                    }
                }
            };

            window.ensureStockCharts(function() {
                mainChart = window.StockCharts.createChart('assetGrowthChart', {
                type: 'line',
                plugins: [tradeMarkersPlugin],
                data: {
                    labels: labels,
                    datasets: [
                        {
                            type: 'line',
                            label: '${principalLabel}',
                            data: totalCostData,
                            borderColor: 'rgba(156, 163, 175, 1)',
                            borderWidth: 2,
                            borderDash: [5, 5],
                            fill: false,
                            yAxisID: 'y',
                            order: 0
                        },
                        {
                            type: 'line',
                            label: '${totalAssetDatasetLabel}',
                            data: totalValueData,
                            borderColor: 'rgba(75, 192, 192, 1)',
                            borderWidth: 2,
                            fill: {
                                target: '-1',
                                above: 'rgba(255, 99, 132, 0.25)',
                                below: 'rgba(54, 162, 235, 0.25)'
                            },
                            yAxisID: 'y',
                            order: 1
                        },

                    ]
                },
                options: {
                    animation: false,
                    normalized: true,
                    elements: {
                        line: { tension: 0 },
                        point: { radius: 0, hitRadius: 10, hoverRadius: 4 }
                    },
                    layout: {
                        padding: { top: 20, bottom: 5 }
                    },
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'index',
                        intersect: false
                    },
                    plugins: {
                        legend: {
                            labels: {
                                sort: function(a, b) {
                                    return a.datasetIndex - b.datasetIndex;
                                }
                            }
                        },
                        tooltip: {
                            itemSort: function(a, b) {
                                var aValue = a && a.parsed ? parseFloat(a.parsed.y) : 0;
                                var bValue = b && b.parsed ? parseFloat(b.parsed.y) : 0;
                                if (aValue === bValue) {
                                    return a.datasetIndex - b.datasetIndex;
                                }
                                return bValue - aValue;
                            },
                            callbacks: {
                                label: function(context) {
                                    var label = context.dataset.label || '';
                                    if (label) label += ': ';
                                    if (context.parsed.y !== null) {
                                        label += formatCurrency(context.parsed.y);
                                    }
                                    return label;
                                },
                                afterBody: function(tooltipItems) {
                                    // scatter 아이템은 데이터 배열 내 인덱스라 category index가 아님 → 라인 아이템 기준
                                    var lineItem = tooltipItems.find(function(t) { return t.datasetIndex < 2; });
                                    if (!lineItem) return [];
                                    var idx = lineItem.dataIndex;
                                    var value = parseFloat(totalValueData[idx]);
                                    var cost = parseFloat(totalCostData[idx]);
                                    var diff = value - cost;
                                    var pct = cost !== 0 ? ((diff / cost) * 100) : 0;
                                    var lines = [
                                        '─────────────────',
                                        '${unrealizedProfitLabel}: ' + formatSignedCurrency(diff) + ' (' + formatSignedPercent(pct) + ')'
                                    ];
                                    if (tradeCountData[idx] > 0) {
                                        var drp = parseFloat(dailyRealizedProfitData[idx]);
                                        lines.push('─────────────────');
                                        lines.push('${clickTradeHistoryLabel}');
                                        lines.push('${tradeCountLabel}'.replace('{0}', formatNumber(tradeCountData[idx])));
                                        if (drp !== 0) lines.push('${dailyRealizedProfitLabel}: ' + formatSignedCurrency(drp));
                                    }
                                    return lines;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            display: true,
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
                            title: { display: true, text: '${totalAssetAxisLabel}' },
                            ticks: { callback: yTicksCallback }
                        },

                    },
                    onHover: function(event, elements) {
                        if (syncing || !dividendChart) return;
                        event.native.target.style.cursor = 'default';
                        syncing = true;
                        var lineEl = elements.find(function(e) { return e.datasetIndex < 2; });
                        if (!lineEl) {
                            dividendChart.tooltip.setActiveElements([], { x: 0, y: 0 });
                        } else {
                            var idx = lineEl.index;
                            var active = dividendChart.data.datasets.map(function(_, i) { return { datasetIndex: i, index: idx }; });
                            var meta0 = dividendChart.getDatasetMeta(0);
                            var pt = meta0 && meta0.data && meta0.data[idx];
                            dividendChart.tooltip.setActiveElements(active, pt ? { x: pt.x, y: pt.y } : { x: 0, y: 0 });
                        }
                        dividendChart.update('none');
                        syncing = false;
                    },
                    onClick: function(event, elements) {
                        if (!elements.length) return;
                        if (markerClicked) { markerClicked = false; return; } // 마커 클릭 시 중복 방지
                        var clicked = elements[0];
                        var date = labels[clicked.index];
                        htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + date, {
                            target: '#holdings-snapshot-container',
                            swap: 'innerHTML'
                        });
                    }
                }
                }, mainChart);
            });

            window.ensureStockCharts(function() {
            dividendChart = window.StockCharts.createChart('dividendChart', {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: '${cumulativeRealizedDatasetLabel}',
                            data: cumulativeRealizedProfitData,
                            borderColor: 'rgba(99, 102, 241, 1)',
                            backgroundColor: 'rgba(99, 102, 241, 0.25)',
                            borderWidth: 2,
                            fill: 'origin',
                            pointRadius: 0,
                            pointHitRadius: 10,
                            pointHoverRadius: 4,
                            yAxisID: 'y'
                        },
                        {
                            label: '${cumulativeTotalDatasetLabel}',
                            data: totalCumulativeProfitData,
                            borderColor: 'rgba(34, 197, 94, 1)',
                            backgroundColor: 'rgba(34, 197, 94, 0.25)',
                            borderWidth: 2,
                            fill: '-1',
                            pointRadius: 0,
                            pointHitRadius: 10,
                            pointHoverRadius: 4,
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
                        tooltip: {
                            callbacks: {
                                // dataset label 은 숨기고 afterBody 에서 원하는 형태로 직접 구성
                                label: function() { return null; },
                                afterBody: function(tooltipItems) {
                                    if (!tooltipItems.length) return [];
                                    var idx = tooltipItems[0].dataIndex;
                                    var realized  = cumulativeRealizedProfitData[idx];
                                    var dividend  = cumulativeDividendData[idx];
                                    var total     = (parseFloat(realized) || 0) + (parseFloat(dividend) || 0);
                                    return [
                                        '${cumulativeRealizedDatasetLabel}: ' + formatSignedCurrency(realized),
                                        '${cumulativeDividendLabel}: ' + formatSignedCurrency(dividend),
                                        '─────────────────────',
                                        '${cumulativeTotalDatasetLabel}: ' + formatSignedCurrency(total)
                                    ];
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            display: true,
                            ticks: {
                                maxRotation: 45,
                                minRotation: 45,
                                maxTicksLimit: 15
                            }
                        },
                        y: {
                            display: true,
                            position: 'left',
                            title: { display: true, text: '${cumulativeProfitAxisLabel}' },
                            ticks: { callback: yTicksCallback }
                        }
                    },
                    onHover: function(event, elements) {
                        if (syncing || !mainChart) return;
                        syncing = true;
                        if (!elements.length) {
                            mainChart.tooltip.setActiveElements([], { x: 0, y: 0 });
                        } else {
                            var idx = elements[0].index;
                            var active = mainChart.data.datasets.map(function(_, i) { return { datasetIndex: i, index: idx }; });
                            var meta0 = mainChart.getDatasetMeta(0);
                            var pt = meta0 && meta0.data && meta0.data[idx];
                            mainChart.tooltip.setActiveElements(active, pt ? { x: pt.x, y: pt.y } : { x: 0, y: 0 });
                        }
                        mainChart.update('none');
                        syncing = false;
                    },
                    onClick: function(event, elements) {
                        if (!elements.length) return;
                        if (markerClicked) { markerClicked = false; return; } // 마커 클릭 시 중복 방지
                        var date = labels[elements[0].index];
                        htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + date, {
                            target: '#holdings-snapshot-container',
                            swap: 'innerHTML'
                        });
                    }
                }
            });

            });

            // 마커 삼각형 클릭 감지 (afterDraw로 그린 삼각형은 Chart.js hit detection 밖이므로 직접 처리)
            var markerClicked = false; // Chart.js onClick 중복 호출 방지 플래그
            var c1El = document.getElementById('assetGrowthChart');
            if (c1El) {
                c1El.addEventListener('click', function(e) {
                    var rect = c1El.getBoundingClientRect();
                    var mx = e.clientX - rect.left;
                    var my = e.clientY - rect.top;
                    var hitR = 12;
                    for (var k = 0; k < drawnMarkers.length; k++) {
                        var m = drawnMarkers[k];
                        if (Math.abs(mx - m.px) <= hitR && Math.abs(my - m.py) <= hitR) {
                            markerClicked = true;
                            htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + m.date, {
                                target: '#holdings-snapshot-container',
                                swap: 'innerHTML'
                            });
                            return;
                        }
                    }
                });
            }

            // htmx swap(이전/다음·필터 조회) 직후엔 캔버스 크기가 0으로 잡혀 빈 차트로 보일 수 있다.
            // realizedProfit 과 동일하게 레이아웃 확정 후 resize 로 보정한다.
            function resizeAssetGrowthCharts() {
                [mainChart, dividendChart].forEach(function(c) {
                    if (!c || !c.canvas || !c.canvas.isConnected) return;
                    try { c.resize(); c.update('none'); } catch (e) {}
                });
            }
            setTimeout(resizeAssetGrowthCharts, 0);
            setTimeout(resizeAssetGrowthCharts, 200);
            setTimeout(resizeAssetGrowthCharts, 500);
        })();
    </script>
    @endif
</div>
