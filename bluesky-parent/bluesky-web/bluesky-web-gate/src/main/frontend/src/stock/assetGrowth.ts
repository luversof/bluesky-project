// 자산 성장 화면의 브라우저 동작: 기간 선택기 · 자산/누적손익 차트 두 개(툴팁 동기화, 클릭 -> 보유 스냅샷 조회, 마커 클릭).
//
// 2026-09-08 까지 asset-growth.jte 안의 인라인 <script> 둘(34 + 274 줄)이었다. 서버가 넣어 주던 것(시계열 배열 8 개,
// 라벨 13 개, 타임존, 선택기 minDate)은 조각의 작은 인라인 스크립트가 window.assetGrowthConfig 로 건네고, 로직은 여기 있다.
// 조각은 htmx 로 스왑되므로 그때마다 새 config 가 실리고 htmx:afterSettle 에서 run(cfg) 를 다시 돈다(조각 루트
// #assetGrowthFragment 의 data-asset-growth-init 표식으로 한 DOM 에 한 번만). 타입은 느슨하다(any).
export {};

declare const htmx: any;

/** 조각의 인라인 스크립트가 window.assetGrowthConfig 로 싣는 것. 시계열 값은 서버가 숫자로 넣지만 코드가 parseFloat 로 읽으므로 넓게 둔다. */
interface AssetGrowthSeries {
	labels: string[];
	totalValueData: any[];
	totalCostData: any[];
	cumulativeRealizedProfitData: any[];
	cumulativeDividendData: any[];
	tradeCountData: any[];
	buyCountData: any[];
	dailyRealizedProfitData: any[];
}
interface AssetGrowthConfig {
	snapshotTz: string;
	minDate: string;
	series: AssetGrowthSeries | null;
	labels: Record<string, string>;
}

(() => {
	const win: any = window;
	if (win.__assetGrowthAttached) {
		if (typeof win.initializeAssetGrowth === "function") win.initializeAssetGrowth();
		return;
	}
	win.__assetGrowthAttached = true;

	function runPicker(cfg: AssetGrowthConfig) {
        var assetPicker = win.DateRangePicker.create({
            formId:         'assetGrowthSearchForm',
            startId:        'assetStartDateInput',
            endId:          'assetEndDateInput',
            instantStartId: 'assetStartInstantInput',
            instantEndId:   'assetEndInstantInput',
            timeZoneId:     'assetTimeZoneInput',
            rangeModeId:    'assetRangeModeInput',
            btnClass:       'asset-range-btn',
            minDate:        cfg.minDate || '',
            globalKey:      'globalDateRange',
            rootSelector:   '#assetGrowthFragment'
        });
        win.assetPicker = assetPicker;
        // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
        try {
            if (win.assetPicker && typeof win.assetPicker.getState === 'function') {
                var __s = win.assetPicker.getState();
                if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
                    var __key = 'dateRangeDefaultApplied:assetPicker';
                    try {
                        if (!sessionStorage.getItem(__key)) {
                            var _btn = document.querySelector<HTMLElement>('#assetGrowthFragment .asset-range-btn');
                            win.assetPicker.set('mtd', _btn || null);
                            sessionStorage.setItem(__key, '1');
                        }
                    } catch(e) { win.assetPicker.set('mtd', null); }
                }
            }
        } catch(e) {}

	}

	function runCharts(cfg: AssetGrowthConfig) {
		const L: Record<string, string> = cfg.labels || {};
            // Chart.js 는 아래 win.ensureStockCharts 콜백에서 필요할 때 로드된다.
            // 여기서 typeof Chart 로 조기 반환하면 로더를 부르기도 전에 끝나 차트가 영영 안 그려진다.
            // 보유 스냅샷 조회 시 함께 넘길 타임존. 안 넘기면 서버 기본 타임존으로
            // 집계돼 컨테이너가 UTC 인 환경에서 날짜가 하루 어긋난다.
            var snapshotTz = encodeURIComponent(cfg.snapshotTz || Intl.DateTimeFormat().resolvedOptions().timeZone || '');
            var ctxMain = document.getElementById('assetGrowthChart');
            var ctxDividend = document.getElementById('dividendChart');
            if (!ctxMain || !ctxDividend) return;
            var series0: Partial<AssetGrowthSeries> = cfg.series || {};
            var labels: string[] = series0.labels || [];
            var totalValueData: any[] = series0.totalValueData || [];
            var totalCostData: any[] = series0.totalCostData || [];
            var cumulativeRealizedProfitData: any[] = series0.cumulativeRealizedProfitData || [];
            var cumulativeDividendData: any[] = series0.cumulativeDividendData || [];
            var tradeCountData: any[] = series0.tradeCountData || [];
            var buyCountData: any[] = series0.buyCountData || [];
            var dailyRealizedProfitData: any[] = series0.dailyRealizedProfitData || [];



            // 현재 평가 손익(미실현) = 총 자산 평가액 - 투자 원금
            var currentUnrealizedProfitData = totalValueData.map(function(v: any, i: number) {
                return parseFloat(v) - parseFloat(totalCostData[i]);
            });
            // 총 누적 이익 = 누적 결산 손익 + 누적 배당금
            var totalCumulativeProfitData = cumulativeRealizedProfitData.map(function(v: any, i: number) {
                return parseFloat(v) + parseFloat(cumulativeDividendData[i]);
            });

            var yTicksCallback = function(value: any) {
                if (win.StockCharts) return win.StockCharts.formatCompactNumber(value);
                return new Intl.NumberFormat().format(value);
            };

            var locale: string | undefined = win.StockCharts ? win.StockCharts.getLocale() : undefined;
            var percentFormatter = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
            var formatNumber = function(value: number) {
                return win.StockCharts ? win.StockCharts.formatNumber(value) : new Intl.NumberFormat(locale).format(value);
            };
            var formatCurrency = function(value: number) {
                return win.StockCharts ? win.StockCharts.formatCurrency(Math.round(value)) : ('₩' + Math.round(value).toLocaleString());
            };
            var formatSignedCurrency = function(value: number) {
                var rounded = Math.round(value || 0);
                var sign = rounded >= 0 ? '+' : '-';
                return sign + formatCurrency(Math.abs(rounded));
            };
            var formatSignedPercent = function(value: number) {
                var sign = value >= 0 ? '+' : '-';
                return sign + percentFormatter.format(Math.abs(value)) + '%';
            };

            var syncing = false;
            var mainChart: any, dividendChart: any;

            win.ensureStockCharts(function() {
                var series = {
                    labels: labels,
                    value: totalValueData,
                    cost: totalCostData,
                    buyCount: buyCountData,
                    dailyRealized: dailyRealizedProfitData
                };
                mainChart = win.StockCharts.createHoldingsChart('assetGrowthChart', series, {
                    valueLabel: L.totalAsset,
                    costLabel: L.principal,
                    axisLabel: L.totalAssetAxis,
                    profitLabel: L.unrealizedProfit,
                    maxLabel: L.rangeMax,
                    minLabel: L.rangeMin
                }, {
                    animate: true,
                    showMarkers: true,
                    tooltipAfterBody: function(idx: number) {
                        if (tradeCountData[idx] > 0) {
                            var drp = parseFloat(dailyRealizedProfitData[idx]);
                            var lines = ['─────────────────', L.clickTradeHistory, L.tradeCount.replace('{0}', formatNumber(tradeCountData[idx]))];
                            if (drp !== 0) lines.push(L.dailyRealizedProfit + ': ' + formatSignedCurrency(drp));
                            return lines;
                        }
                        return [];
                    },
                    onHover: function(event: any, elements: any[]) {
                        if (syncing || !dividendChart) return;
                        event.native.target.style.cursor = 'default';
                        syncing = true;
                        var lineEl = elements.find(function(e: any) { return e.datasetIndex < 2; });
                        if (!lineEl) {
                            dividendChart.tooltip.setActiveElements([], { x: 0, y: 0 });
                        } else {
                            var idx = lineEl.index;
                            var active = dividendChart.data.datasets.map(function(_: any, i: number) { return { datasetIndex: i, index: idx }; });
                            var meta0 = dividendChart.getDatasetMeta(0);
                            var pt = meta0 && meta0.data && meta0.data[idx];
                            dividendChart.tooltip.setActiveElements(active, pt ? { x: pt.x, y: pt.y } : { x: 0, y: 0 });
                        }
                        dividendChart.update('none');
                        syncing = false;
                    },
                    onClick: function(event: any, elements: any[]) {
                        if (!elements.length) return;
                        if (markerClicked) { markerClicked = false; return; }
                        var clicked = elements[0];
                        var date = labels[clicked.index];
                        htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + date + '&timeZone=' + snapshotTz, {
                            target: '#holdings-snapshot-container',
                            swap: 'innerHTML'
                        });
                    }
                }, mainChart);
            });

            win.ensureStockCharts(function() {
            dividendChart = win.StockCharts.createChart('dividendChart', {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: L.cumulativeRealized,
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
                            label: L.cumulativeTotal,
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
                    animation: { duration: 600 },
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
                        legend: {
                            position: 'bottom'
                        },
                        tooltip: {
                            callbacks: {
                                // dataset label 은 숨기고 afterBody 에서 원하는 형태로 직접 구성
                                label: function() { return null; },
                                afterBody: function(tooltipItems: any[]) {
                                    if (!tooltipItems.length) return [];
                                    var idx = tooltipItems[0].dataIndex;
                                    var realized  = cumulativeRealizedProfitData[idx];
                                    var dividend  = cumulativeDividendData[idx];
                                    var total     = (parseFloat(realized) || 0) + (parseFloat(dividend) || 0);
                                    return [
                                        L.cumulativeRealized + ': ' + formatSignedCurrency(realized),
                                        L.cumulativeDividend + ': ' + formatSignedCurrency(dividend),
                                        '─────────────────────',
                                        L.cumulativeTotal + ': ' + formatSignedCurrency(total)
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
                                minRotation: 45
                            }
                        },
                        y: {
                            display: true,
                            position: 'left',
                            title: { display: true, text: L.cumulativeProfitAxis },
                            ticks: { callback: yTicksCallback }
                        }
                    },
                    onHover: function(event: any, elements: any[]) {
                        if (syncing || !mainChart) return;
                        syncing = true;
                        if (!elements.length) {
                            mainChart.tooltip.setActiveElements([], { x: 0, y: 0 });
                        } else {
                            var idx = elements[0].index;
                            var active = mainChart.data.datasets.map(function(_: any, i: number) { return { datasetIndex: i, index: idx }; });
                            var meta0 = mainChart.getDatasetMeta(0);
                            var pt = meta0 && meta0.data && meta0.data[idx];
                            mainChart.tooltip.setActiveElements(active, pt ? { x: pt.x, y: pt.y } : { x: 0, y: 0 });
                        }
                        mainChart.update('none');
                        syncing = false;
                    },
                    onClick: function(event: any, elements: any[]) {
                        if (!elements.length) return;
                        if (markerClicked) { markerClicked = false; return; } // 마커 클릭 시 중복 방지
                        var date = labels[elements[0].index];
                        htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + date + '&timeZone=' + snapshotTz, {
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
                c1El.addEventListener('click', function(e: MouseEvent) {
                    var rect = c1El!.getBoundingClientRect();
                    var mx = e.clientX - rect.left;
                    var my = e.clientY - rect.top;
                    var hitR = 12;
                    var drawnMarkers = (mainChart && mainChart.$drawnMarkers) || [];
                    for (var k = 0; k < drawnMarkers.length; k++) {
                        var m = drawnMarkers[k];
                        if (Math.abs(mx - m.px) <= hitR && Math.abs(my - m.py) <= hitR) {
                            markerClicked = true;
                            htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + m.date + '&timeZone=' + snapshotTz, {
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
                [mainChart, dividendChart].forEach(function(c: any) {
                    if (!c || !c.canvas || !c.canvas.isConnected) return;
                    // 크기가 실제로 달라졌을 때만 다시 그린다(소수 폭에서 resize() 는 매번 전체 재렌더 - stock-charts.ts resizeIfChanged 참고).
                    try { const SC = (window as any).StockCharts; if (SC && SC.resizeIfChanged) SC.resizeIfChanged(c); else c.resize(); } catch (e) {}
                });
            }
            setTimeout(resizeAssetGrowthCharts, 0);
            setTimeout(resizeAssetGrowthCharts, 200);
            setTimeout(resizeAssetGrowthCharts, 500);

	}

	function initializeAssetGrowth() {
		const fragment = document.getElementById("assetGrowthFragment");
		const cfg = win.assetGrowthConfig;
		if (!fragment || !cfg) return;
		if (fragment.dataset.assetGrowthInit === "1") return;
		fragment.dataset.assetGrowthInit = "1";
		runPicker(cfg);
		runCharts(cfg);
	}

	win.initializeAssetGrowth = initializeAssetGrowth;
	document.addEventListener("htmx:afterSettle", initializeAssetGrowth);
	initializeAssetGrowth();
})();
