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
                <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center gap-1">
                        <button type="button" id="btnFirst" class="btn btn-xs btn-ghost btn-circle" style="opacity:0.2;" onclick="jumpToEdge('start')" title="제일 앞으로">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" /></svg>
                        </button>
                        <button type="button" id="btnPrev" class="btn btn-xs btn-ghost btn-circle" style="opacity:0.2;" onclick="shiftView(-1)" title="이전 기간">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
                        </button>
                        <div class="flex gap-1" id="quickRangeButtons">
                            <button type="button" class="btn btn-xs btn-ghost" data-months="1" onclick="setViewRange(1)">1개월</button>
                            <button type="button" class="btn btn-xs btn-ghost" data-months="3" onclick="setViewRange(3)">3개월</button>
                            <button type="button" class="btn btn-xs btn-ghost" data-months="6" onclick="setViewRange(6)">6개월</button>
                            <button type="button" class="btn btn-xs btn-ghost" data-months="-1" onclick="setViewRange(-1)">올해</button>
                            <button type="button" class="btn btn-xs btn-ghost" data-months="12" onclick="setViewRange(12)">1년</button>
                            <button type="button" class="btn btn-xs btn-ghost" data-months="36" onclick="setViewRange(36)">3년</button>
                            <button type="button" class="btn btn-xs btn-ghost" data-months="0" onclick="setViewRange(0)">전체</button>
                        </div>
                        <button type="button" id="btnNext" class="btn btn-xs btn-ghost btn-circle" onclick="shiftView(1)" title="다음 기간">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
                        </button>
                        <button type="button" id="btnLast" class="btn btn-xs btn-ghost btn-circle" onclick="jumpToEdge('end')" title="제일 뒤로">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" /></svg>
                        </button>
                    </div>
                    <p class="text-xs text-base-content/50">📈 자산 추이 &mdash; 투자 원금(점선) 대비 총 평가액(실선), 색상 면적이 현재 손익 규모 &bull; <span class="text-info">날짜 클릭 시 해당일 보유 종목 조회</span></p>
                </div>
                <div class="w-full relative" style="height: 450px;">
                    <canvas id="assetGrowthChart" class="amount-chart"></canvas>
                    <div id="chartLoadingOverlay" class="absolute inset-0 flex items-center justify-center hidden" style="z-index:10; background-color:color-mix(in oklch, var(--color-base-100) 80%, transparent);">
                        <span class="loading loading-spinner loading-md text-primary"></span>
                    </div>
                </div>
                <p class="text-xs text-base-content/50 text-center py-1">💰 손익 분석 &mdash; <span class="text-indigo-400">■</span> 누적 결산 손익 + <span class="text-green-400">■</span> 누적 배당금 = 수익 합산 선</p>
                <div class="w-full" style="height: 250px;">
                    <canvas id="dividendChart" class="amount-chart"></canvas>
                </div>
                <div id="holdings-snapshot-container"></div>
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
            var cumulativeRealizedProfitData = [];
            var cumulativeDividendData = [];

            @for(TradeProfitTimeSeriesPoint pt : timeSeries)
                labels.push("$unsafe{formatter.format(pt.timestamp())}");
                totalValueData.push($unsafe{pt.totalHoldingsValue() != null ? pt.totalHoldingsValue().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                totalCostData.push($unsafe{pt.totalHoldingsCost() != null ? pt.totalHoldingsCost().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                cumulativeRealizedProfitData.push($unsafe{pt.cumulativeRealizedProfit() != null ? pt.cumulativeRealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                cumulativeDividendData.push($unsafe{pt.cumulativeDividend() != null ? pt.cumulativeDividend().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
            @endfor

            // 현재 평가 손익(미실현) = 총 자산 평가액 - 투자 원금
            var currentUnrealizedProfitData = totalValueData.map(function(v, i) {
                return parseFloat(v) - parseFloat(totalCostData[i]);
            });
            // 총 누적 이익 = 누적 결산 손익 + 누적 배당금
            var totalCumulativeProfitData = cumulativeRealizedProfitData.map(function(v, i) {
                return parseFloat(v) + parseFloat(cumulativeDividendData[i]);
            });

            // ── 줌/팬 시 동적 그래뉼래러티 로딩 ─────────────────────────────────
            var baseData = {
                labels: labels.slice(),
                totalValueData: totalValueData.slice(),
                totalCostData: totalCostData.slice(),
                cumulativeRealizedProfitData: cumulativeRealizedProfitData.slice(),
                cumulativeDividendData: cumulativeDividendData.slice()
            };
            // 초기 데이터 tier 감지 (0=MONTHLY, 1=WEEKLY, 2=DAILY)
            var baseDataTierNum = (function() {
                if (labels.length < 2) return 2;
                var diff = (new Date(labels[1]) - new Date(labels[0])) / 86400000;
                return diff >= 20 ? 0 : (diff >= 5 ? 1 : 2);
            })();
            var currentTierNum = baseDataTierNum;
            var detailCacheEntries = [];
            var isFetching = false;
            var setLoading = function(val) {
                var overlay = document.getElementById('chartLoadingOverlay');
                if (overlay) overlay.classList.toggle('hidden', !val);
                var c1 = document.getElementById('assetGrowthChart');
                if (c1) c1.style.opacity = val ? '0.35' : '1';
                var c2 = document.getElementById('dividendChart');
                if (c2) c2.style.opacity = val ? '0.35' : '1';
            };
            var setFetching = function(val) {
                isFetching = val;
                if (!val) setLoading(false);
            };
            var detailFetchTimer = null;
            var checkDetailTimer = null;
            var activeMonths = 12; // 현재 선택된 퀘셀렉터 기간 (0=전체)
            // shiftView/setViewRange 가 명시적으로 지정한 목표 날짜 (BASE labels 포인트가 희박할 때 getOptDates 대체)
            var pendingVisStart = null;
            var pendingVisEnd   = null;

            var scaleAnimFrame = null; // requestAnimationFrame handle for scale transition

            // 뷰 구간을 부드럽게(easeOutCubic) 전환하는 헬퍼
            // shiftView / setViewRange / jumpToEdge 에서 사용
            var animateScaleChange = function(toMin, toMax, duration) {
                if (scaleAnimFrame !== null) {
                    cancelAnimationFrame(scaleAnimFrame);
                    scaleAnimFrame = null;
                }
                var fromMin = mainChart.scales.x.min;
                var fromMax = mainChart.scales.x.max;
                // 이미 목표값에 매우 가까우면 즉시 적용
                if (Math.abs(fromMin - toMin) < 0.5 && Math.abs(fromMax - toMax) < 0.5) {
                    mainChart.options.scales.x.min = toMin;
                    mainChart.options.scales.x.max = toMax;
                    dividendChart.options.scales.x.min = toMin;
                    dividendChart.options.scales.x.max = toMax;
                    mainChart.update('none');
                    dividendChart.update('none');
                    checkNavButtons();
                    return;
                }
                var startTime = null;
                var step = function(ts) {
                    if (!startTime) startTime = ts;
                    var t = Math.min((ts - startTime) / duration, 1);
                    var ease = 1 - Math.pow(1 - t, 3); // easeOutCubic
                    var curMin = fromMin + (toMin - fromMin) * ease;
                    var curMax = fromMax + (toMax - fromMax) * ease;
                    mainChart.options.scales.x.min = curMin;
                    mainChart.options.scales.x.max = curMax;
                    dividendChart.options.scales.x.min = curMin;
                    dividendChart.options.scales.x.max = curMax;
                    mainChart.update('none');
                    dividendChart.update('none');
                    if (t < 1) {
                        scaleAnimFrame = requestAnimationFrame(step);
                    } else {
                        scaleAnimFrame = null;
                        checkNavButtons();
                    }
                };
                scaleAnimFrame = requestAnimationFrame(step);
            };

            var applyChartData = function(newData, visStart, visEnd) {
                // 진행 중인 scale 애니메이션 취소 (데이터 교체와 충돌 방지)
                if (scaleAnimFrame !== null) {
                    cancelAnimationFrame(scaleAnimFrame);
                    scaleAnimFrame = null;
                }
                // onHover 핸들러 진입 차단 (stale 인덱스 방지)
                syncing = true;

                labels.length = 0;
                newData.labels.forEach(function(l) { labels.push(l); });
                totalValueData.length = 0;
                newData.totalValueData.forEach(function(v) { totalValueData.push(v); });
                totalCostData.length = 0;
                newData.totalCostData.forEach(function(v) { totalCostData.push(v); });
                cumulativeRealizedProfitData.length = 0;
                newData.cumulativeRealizedProfitData.forEach(function(v) { cumulativeRealizedProfitData.push(v); });
                cumulativeDividendData.length = 0;
                newData.cumulativeDividendData.forEach(function(v) { cumulativeDividendData.push(v); });

                var newMin = 0;
                for (var i = 0; i < labels.length; i++) {
                    if (labels[i] >= visStart) { newMin = i; break; }
                }
                var newMax = labels.length - 1;
                for (var j = labels.length - 1; j >= 0; j--) {
                    if (labels[j] <= visEnd) { newMax = j; break; }
                }
                // visStart/visEnd가 labels 내에 없는 날짜(주말 등)일 때 역전 방지
                if (newMax < newMin) { newMax = Math.min(newMin + 1, labels.length - 1); }

                mainChart.data.labels = labels;
                mainChart.data.datasets[0].data = totalCostData.slice();
                mainChart.data.datasets[1].data = totalValueData.slice();
                mainChart.options.scales.x.min = newMin;
                mainChart.options.scales.x.max = newMax;
                // DETAIL 데이터 교체 시 zoom limits 를 새 데이터 범위로 맞춤
                // BASE 복원 시에도 항상 0 ~ labels.length-1 로 갱신
                var newLabelLen = labels.length;
                mainChart.options.plugins.zoom.limits.x.min = 0;
                mainChart.options.plugins.zoom.limits.x.max = newLabelLen > 0 ? newLabelLen - 1 : 0;

                dividendChart.data.labels = labels;
                dividendChart.data.datasets[0].data = cumulativeRealizedProfitData.slice();
                dividendChart.data.datasets[1].data = cumulativeRealizedProfitData.map(function(v, i) {
                    return parseFloat(v) + parseFloat(cumulativeDividendData[i]);
                });
                dividendChart.options.scales.x.min = newMin;
                dividendChart.options.scales.x.max = newMax;
                dividendChart.options.plugins.zoom.limits.x.min = 0;
                dividendChart.options.plugins.zoom.limits.x.max = newLabelLen > 0 ? newLabelLen - 1 : 0;

                mainChart.update('none');
                dividendChart.update('none');
                syncing = false;
                checkNavButtons();
            };

            var getOptDates = function() {
                // scales.x 는 zoom/pan 이후 실제 렌더된 값으로 갱신됨
                // options.scales.x 는 zoom 플러그인이 건드리지 않아 stale 해짐 → 사용 금지
                var xScale = mainChart.scales.x;
                var rawMin = xScale.min;
                var rawMax = xScale.max;
                var minIdx = Math.max(0, Math.min(labels.length - 1, Math.round(rawMin)));
                var maxIdx = Math.max(0, Math.min(labels.length - 1, Math.round(rawMax)));
                return {
                    visStart: labels[minIdx] || labels[0],
                    visEnd:   labels[maxIdx] || labels[labels.length - 1]
                };
            };

            var checkAndFetchDetail = function() {
                if (!mainChart || isFetching) { setLoading(false); return; }

                // pendingVisStart/End 가 있으면 우선 사용 (shiftView/setViewRange 가 명시적으로 지정한 날짜)
                // 없으면 scales.x 에서 읽음 (zoom/pan 콜백)
                var visStart, visEnd;
                if (pendingVisStart && pendingVisEnd) {
                    visStart = pendingVisStart;
                    visEnd   = pendingVisEnd;
                    pendingVisStart = null;
                    pendingVisEnd   = null;
                } else {
                    var od = getOptDates();
                    visStart = od.visStart;
                    visEnd   = od.visEnd;
                }
                var visibleDays = (new Date(visEnd) - new Date(visStart)) / 86400000;
                if (visibleDays < 0) { setLoading(false); return; }
                var neededTier = visibleDays <= 180 ? 2 : (visibleDays <= 730 ? 1 : 0);

                if (currentTierNum > baseDataTierNum) {
                    // ── DETAIL 모드 ──────────────────────────────────────────
                    // zoom limits = [0, DETAIL_labels.length-1] 이므로
                    // 완전히 줌아웃 시 scales.x.min=0, max=DETAIL_last → getOptDates() 는 labels 의 양 끝 날짜를 반환
                    // → 이 시점을 BASE 복원 트리거로 사용 (% 비교보다 명확하고 신뢰성 높음)
                    var atDetailEdge = visStart <= labels[0] && visEnd >= labels[labels.length - 1];
                    if (atDetailEdge) {
                        applyChartData(baseData, baseData.labels[0], baseData.labels[baseData.labels.length - 1]);
                        currentTierNum = baseDataTierNum;
                        setLoading(false);
                        return;
                    }

                    // 현재 DETAIL 데이터가 뷰포트를 완전히 커버하고 충분히 세밀하면 종료
                    var detailCoversView = visStart >= labels[0] && visEnd <= labels[labels.length - 1];
                    if (detailCoversView && neededTier <= currentTierNum) { setLoading(false); return; }

                    // 커버 바깥이거나 더 세밀한 데이터 필요 → cache/fetch 로 fall through
                    // (아래 BASE 모드 조기 return 체크는 건너뜀)
                } else {
                    // ── BASE 모드 ─────────────────────────────────────────────
                    // 현재 granularity 로 충분 → fetch 불필요
                    if (neededTier <= baseDataTierNum) { setLoading(false); return; }
                    if (neededTier <= currentTierNum) { setLoading(false); return; }
                }

                // 클라이언트 캐시 확인
                for (var i = 0; i < detailCacheEntries.length; i++) {
                    var c = detailCacheEntries[i];
                    if (c.tier >= neededTier && c.start <= visStart && c.end >= visEnd) {
                        applyChartData(c, visStart, visEnd);
                        currentTierNum = c.tier;
                        setLoading(false);
                        return;
                    }
                }

                // 서버 조회 스케줄 (debounce 400ms) — 현 시점 날짜를 캡처해서 사용
                var capturedNeededTier = neededTier;
                var capturedVisStart = visStart;
                var capturedVisEnd = visEnd;
                clearTimeout(detailFetchTimer);
                detailFetchTimer = setTimeout(function() {
                    var fetchStart = new Date(capturedVisStart);
                    var fetchEnd = new Date(capturedVisEnd);
                    fetchStart.setDate(fetchStart.getDate() - 30);
                    fetchEnd.setDate(fetchEnd.getDate() + 30);
                    var startStr = fetchStart.toISOString().split('T')[0];
                    var endStr = fetchEnd.toISOString().split('T')[0];

                    isFetching = true;
                    var granStr = capturedNeededTier === 2 ? 'DAILY' : 'WEEKLY';
                    fetch('/stock/htmx/asset-growth/data?from=' + startStr + '&to=' + endStr + '&gran=' + granStr)
                        .then(function(r) { return r.json(); })
                        .then(function(data) {
                            if (!data.labels || data.labels.length === 0) return;
                            var entry = {
                                tier: capturedNeededTier,
                                start: data.labels[0],
                                end: data.labels[data.labels.length - 1],
                                labels: data.labels,
                                totalValueData: data.totalValueData,
                                totalCostData: data.totalCostData,
                                cumulativeRealizedProfitData: data.cumulativeRealizedProfitData,
                                cumulativeDividendData: data.cumulativeDividendData
                            };
                            detailCacheEntries.push(entry);
                            // fetch 완료 후 applyChartData 의 표시 범위는 capturedVisStart/End 사용
                            // (getOptDates() 로 재읽으면 BASE labels 기준 좁은 범위가 반환되어 화면이 비어 보임)
                            var curVD = (new Date(capturedVisEnd) - new Date(capturedVisStart)) / 86400000;
                            var viewIsNarrowEnough =
                                (entry.tier === 2 && curVD <= 180) ||
                                (entry.tier === 1 && curVD <= 730);
                            if (viewIsNarrowEnough) {
                                applyChartData(entry, capturedVisStart, capturedVisEnd);
                                currentTierNum = entry.tier;
                            }
                        })
                        .catch(function(e) { console.warn('Detail fetch failed:', e); })
                        .finally(function() { setFetching(false); });
                }, 400);
            };
            // ─────────────────────────────────────────────────────────────────

            var maxIndex = labels.length - 1;
            var defaultMinIndex = maxIndex > 365 ? maxIndex - 365 : 0;
            var defaultMinDate = labels[defaultMinIndex];
            var defaultMaxDate = labels[maxIndex];

            var mainChart, dividendChart;

            var checkNavButtons = function() {
                if (!mainChart) return;
                var xAxis = mainChart.scales.x;
                if (!xAxis) return;
                var xMin = xAxis.min;
                var xMax = xAxis.max;
                var minIdx = typeof xMin === 'number' ? Math.round(xMin) : labels.indexOf(xMin);
                var maxIdx = typeof xMax === 'number' ? Math.round(xMax) : labels.indexOf(xMax);
                if (minIdx < 0 || isNaN(minIdx)) minIdx = 0;
                if (maxIdx < 0 || isNaN(maxIdx)) maxIdx = labels.length - 1;

                // DETAIL 상태에서도 올바르게 판단하기 위해 baseData 기준 날짜 비교
                var visMinDate = labels[minIdx] || '';
                var visMaxDate = labels[maxIdx] || '';
                var atStart = visMinDate <= baseData.labels[0];
                var atEnd   = visMaxDate >= baseData.labels[baseData.labels.length - 1];

                var btnFirst = document.getElementById('btnFirst');
                var btnPrev = document.getElementById('btnPrev');
                var btnNext = document.getElementById('btnNext');
                var btnLast = document.getElementById('btnLast');

                if (btnFirst) { btnFirst.style.opacity = atStart ? '0.2' : '1'; btnFirst.style.pointerEvents = atStart ? 'none' : 'auto'; }
                if (btnPrev)  { btnPrev.style.opacity  = atStart ? '0.2' : '1'; btnPrev.style.pointerEvents  = atStart ? 'none' : 'auto'; }
                if (btnNext)  { btnNext.style.opacity  = atEnd   ? '0.2' : '1'; btnNext.style.pointerEvents  = atEnd   ? 'none' : 'auto'; }
                if (btnLast)  { btnLast.style.opacity  = atEnd   ? '0.2' : '1'; btnLast.style.pointerEvents  = atEnd   ? 'none' : 'auto'; }
            };

            window.jumpToEdge = function(position) {
                if (!mainChart || !dividendChart) return;
                // 상세 모드라면 base로 복원 후 전체 범위 기준
                var src = (currentTierNum > baseDataTierNum) ? baseData : { labels: labels };
                var total = src.labels.length;
                if (total === 0) return;

                var xAxis = mainChart.scales.x;
                var rawMin = xAxis.min, rawMax = xAxis.max;
                var minIdx = typeof rawMin === 'number' ? Math.round(rawMin) : labels.indexOf(rawMin);
                var maxIdx = typeof rawMax === 'number' ? Math.round(rawMax) : labels.indexOf(rawMax);
                if (minIdx < 0 || isNaN(minIdx)) minIdx = 0;
                if (maxIdx < 0 || isNaN(maxIdx)) maxIdx = labels.length - 1;

                // 현재 뷰포트 span을 baseData 인덱스로 환산
                var curSpanDays = labels.length > 1
                    ? (new Date(labels[Math.min(maxIdx, labels.length-1)]) - new Date(labels[Math.max(minIdx, 0)])) / 86400000
                    : 365;

                if (currentTierNum > baseDataTierNum) {
                    applyChartData(baseData, baseData.labels[0], baseData.labels[total - 1]);
                    currentTierNum = baseDataTierNum;
                    // base 인덱스로 다시 계산
                    minIdx = 0;
                    maxIdx = labels.length - 1;
                }

                // curSpanDays 기준 span 유지
                var spanPts = 0;
                for (var i = 0; i < labels.length; i++) {
                    if ((new Date(labels[i]) - new Date(labels[0])) / 86400000 >= curSpanDays) {
                        spanPts = i;
                        break;
                    }
                }
                if (spanPts === 0) spanPts = maxIdx - minIdx;
                if (spanPts <= 0) spanPts = Math.min(365, labels.length - 1);

                var edgeMinIdx, edgeMaxIdx;
                if (position === 'start') {
                    edgeMinIdx = 0;
                    edgeMaxIdx = Math.min(spanPts, labels.length - 1);
                } else {
                    edgeMaxIdx = labels.length - 1;
                    edgeMinIdx = Math.max(0, labels.length - 1 - spanPts);
                }

                animateScaleChange(edgeMinIdx, edgeMaxIdx, 320);
            };

            // 퀘셀렉터: 단위 M=월, Y=년, 0=전체
            window.setViewRange = function(months) {
                if (!mainChart || !dividendChart) return;
                activeMonths = months;
                // 퀘셀렉터 버튼 활성화 표시
                document.querySelectorAll('#quickRangeButtons button').forEach(function(btn) {
                    var m = parseInt(btn.dataset.months, 10);
                    btn.classList.toggle('btn-active', m === months);
                });
                // 현재 뷰포트의 max 날짜를 먼저 읽기 (복원 전에)
                var xAxisNow = mainChart.scales.x;
                var rawMaxNow = xAxisNow.max;
                var curMaxIdx = typeof rawMaxNow === 'number' ? Math.round(rawMaxNow) : labels.indexOf(rawMaxNow);
                if (curMaxIdx < 0 || curMaxIdx >= labels.length) curMaxIdx = labels.length - 1;
                var viewEndDate = labels[curMaxIdx];
                // 상세 모드라면 base로 먼저 복원
                if (currentTierNum > baseDataTierNum) {
                    applyChartData(baseData, baseData.labels[0], baseData.labels[baseData.labels.length - 1]);
                    currentTierNum = baseDataTierNum;
                    // base labels 기준으로 viewEndDate 보정
                    var bEnd = baseData.labels[baseData.labels.length - 1];
                    for (var bi = baseData.labels.length - 1; bi >= 0; bi--) {
                        if (baseData.labels[bi] <= viewEndDate) { bEnd = baseData.labels[bi]; break; }
                    }
                    viewEndDate = bEnd;
                }
                var newMinIdx, newMaxIdx;
                if (months === 0) {
                    newMinIdx = 0;
                    newMaxIdx = labels.length - 1;
                } else if (months === -1) {
                    // 올해: Jan 1 of current year ~ today
                    var ytdStart = new Date().getFullYear() + '-01-01';
                    newMaxIdx = labels.length - 1;
                    for (var k = labels.length - 1; k >= 0; k--) {
                        if (labels[k] <= viewEndDate) { newMaxIdx = k; break; }
                    }
                    newMinIdx = 0;
                    for (var i = 0; i < labels.length; i++) {
                        if (labels[i] >= ytdStart) { newMinIdx = i; break; }
                    }
                } else {
                    var startDate = new Date(viewEndDate);
                    startDate.setMonth(startDate.getMonth() - months);
                    var startStr = startDate.toISOString().split('T')[0];
                    newMaxIdx = labels.length - 1;
                    for (var k = labels.length - 1; k >= 0; k--) {
                        if (labels[k] <= viewEndDate) { newMaxIdx = k; break; }
                    }
                    newMinIdx = 0;
                    for (var i = 0; i < labels.length; i++) {
                        if (labels[i] >= startStr) { newMinIdx = i; break; }
                    }
                }
                if (newMaxIdx < newMinIdx) newMaxIdx = Math.min(newMinIdx + 1, labels.length - 1);
                // checkAndFetchDetail 이 BASE labels 포인트를 getOptDates() 로 읽으면 희박한 범위가 나오므로
                // 실제 목표 날짜를 명시적으로 저장
                pendingVisStart = labels[newMinIdx];
                pendingVisEnd   = labels[newMaxIdx];
                setLoading(true);
                clearTimeout(checkDetailTimer);
                checkDetailTimer = setTimeout(checkAndFetchDetail, 200);
                animateScaleChange(newMinIdx, newMaxIdx, 320);
            };

            // 이전/다음 기간 이동 (direction: -1=이전, 1=다음)
            window.shiftView = function(direction) {
                if (!mainChart || !dividendChart) return;

                // 현재 뷰 범위를 scales.x(실제 렌더된 인덱스)에서 읽기
                var od = getOptDates();
                var curVisStart = od.visStart, curVisEnd = od.visEnd;
                var curSpanMs = new Date(curVisEnd) - new Date(curVisStart);

                // 이동 거리 계산 (activeMonths=0 이면 현재 span 사용, activeMonths=-1(올해)이면 1년 단위)
                var shiftMs;
                if (activeMonths === -1) {
                    var d = new Date(curVisEnd);
                    d.setFullYear(d.getFullYear() - 1);
                    shiftMs = new Date(curVisEnd) - d;
                } else if (activeMonths > 0) {
                    var d = new Date(curVisEnd);
                    d.setMonth(d.getMonth() - activeMonths);
                    shiftMs = new Date(curVisEnd) - d;
                } else {
                    shiftMs = curSpanMs > 0 ? curSpanMs : 86400000 * 30;
                }
                var spanMs = curSpanMs > 0 ? curSpanMs : shiftMs;

                // 목표 날짜 계산 (날짜 문자열, 아직 labels 에 없을 수 있음)
                var newMinTarget = new Date(new Date(curVisStart).getTime() + direction * shiftMs).toISOString().split('T')[0];
                var newMaxTarget = new Date(new Date(curVisEnd).getTime()   + direction * shiftMs).toISOString().split('T')[0];

                // 경계 체크 — 항상 baseData 기준
                var bFirst = baseData.labels[0];
                var bLast  = baseData.labels[baseData.labels.length - 1];
                if (newMinTarget < bFirst) {
                    newMinTarget = bFirst;
                    var tmpMax = new Date(new Date(bFirst).getTime() + spanMs).toISOString().split('T')[0];
                    newMaxTarget = tmpMax > bLast ? bLast : tmpMax;
                }
                if (newMaxTarget > bLast) {
                    newMaxTarget = bLast;
                    var tmpMin = new Date(new Date(bLast).getTime() - spanMs).toISOString().split('T')[0];
                    newMinTarget = tmpMin < bFirst ? bFirst : tmpMin;
                }

                // DETAIL 모드일 때, 목표 범위가 DETAIL labels 범위를 벗어나면 BASE 로 먼저 복원
                // 그렇지 않으면 범위 밖을 index 0 으로 클램핑해 엉뚱한 구간이 표시됨
                if (currentTierNum > baseDataTierNum) {
                    var targetInDetail = newMinTarget >= labels[0] && newMaxTarget <= labels[labels.length - 1];
                    if (!targetInDetail) {
                        applyChartData(baseData, baseData.labels[0], baseData.labels[baseData.labels.length - 1]);
                        currentTierNum = baseDataTierNum;
                        // 이후 인덱스 계산은 BASE labels 기준으로 진행됨
                    }
                }

                // 목표 날짜 → 현재 labels(BASE 또는 DETAIL) 내 가장 가까운 인덱스
                var newMinIdx = labels.length - 1;
                for (var i = 0; i < labels.length; i++) {
                    if (labels[i] >= newMinTarget) { newMinIdx = i; break; }
                }
                var newMaxIdx = 0;
                for (var j = labels.length - 1; j >= 0; j--) {
                    if (labels[j] <= newMaxTarget) { newMaxIdx = j; break; }
                }
                if (newMaxIdx < newMinIdx) newMaxIdx = Math.min(newMinIdx + 1, labels.length - 1);

                // checkAndFetchDetail 이 BASE labels 포인트를 getOptDates() 로 읽으면 희박한 범위가 나오므로
                // 실제 목표 날짜를 명시적으로 저장 (newMinTarget/newMaxTarget 은 경계 보정된 날짜 문자열)
                pendingVisStart = newMinTarget;
                pendingVisEnd   = newMaxTarget;
                setLoading(true);
                clearTimeout(checkDetailTimer);
                checkDetailTimer = setTimeout(checkAndFetchDetail, 200);
                animateScaleChange(newMinIdx, newMaxIdx, 320);
            };

            var syncZoomPan = function({chart}) {
                var target = (chart.canvas.id === 'assetGrowthChart') ? dividendChart : mainChart;
                if(target) {
                    target.options.scales.x.min = chart.scales.x.min;
                    target.options.scales.x.max = chart.scales.x.max;
                    target.update('none');
                }
                // 줌/팬 시 퀵셀렉터 비활성화 — 현재 뷰 span 으로 이전/다음 이동
                activeMonths = 0;
                document.querySelectorAll('#quickRangeButtons button').forEach(function(btn) {
                    btn.classList.remove('btn-active');
                });
                setTimeout(checkNavButtons, 50);
                clearTimeout(checkDetailTimer);
                checkDetailTimer = setTimeout(checkAndFetchDetail, 200);
            };

            // 팬(alt+drag) 이 현재 데이터 끝에 닿았을 때 baseData 에 더 많은 데이터가 있으면
            // 해당 방향으로 뷰를 시프트하여 새 데이터 fetch 트리거
            var handlePanComplete = function({chart}) {
                var xMin = Math.round(chart.scales.x.min);
                var xMax = Math.round(chart.scales.x.max);
                var moreLeft  = labels.length > 0 && labels[0] > baseData.labels[0];
                var moreRight = labels.length > 0 && labels[labels.length - 1] < baseData.labels[baseData.labels.length - 1];
                if (xMin <= 0 && moreLeft) {
                    shiftView(-1);
                } else if (xMax >= labels.length - 1 && moreRight) {
                    shiftView(1);
                }
            };

            var commonZoomOptions = {
                limits: {
                    x: { min: 0, max: labels.length > 0 ? labels.length - 1 : 0, minRange: 9 } // applyChartData 에서 갱신됨
                },
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
                    onPan: syncZoomPan,
                    onPanComplete: handlePanComplete
                }
            };

            var yTicksCallback = function(value) {
                if (value >= 100000000 || value <= -100000000) return (value / 100000000).toFixed(1) + '억';
                if (value >= 10000 || value <= -10000) return (value / 10000).toFixed(0) + '만';
                return new Intl.NumberFormat('ko-KR').format(value);
            };

            var syncing = false;

            mainChart = new Chart(ctxMain.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            type: 'line',
                            label: '투자 원금',
                            data: totalCostData,
                            borderColor: 'rgba(156, 163, 175, 1)',
                            borderWidth: 2,
                            borderDash: [5, 5],
                            fill: false,
                            yAxisID: 'y',
                            order: 1
                        },
                        {
                            type: 'line',
                            label: '총 자산 평가액',
                            data: totalValueData,
                            borderColor: 'rgba(75, 192, 192, 1)',
                            borderWidth: 2,
                            fill: {
                                target: '-1',
                                above: 'rgba(255, 99, 132, 0.25)',
                                below: 'rgba(54, 162, 235, 0.25)'
                            },
                            yAxisID: 'y',
                            order: 0
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
                                },
                                afterBody: function(tooltipItems) {
                                    var idx = tooltipItems[0].dataIndex;
                                    var value = parseFloat(totalValueData[idx]);
                                    var cost = parseFloat(totalCostData[idx]);
                                    var diff = value - cost;
                                    var sign = diff >= 0 ? '+' : '-';
                                    var pct = cost !== 0 ? ((diff / cost) * 100).toFixed(2) : '0.00';
                                    return ['─────────────────', '평가 손익: ' + sign + new Intl.NumberFormat('ko-KR').format(Math.abs(diff)) + '원 (' + sign + pct + '%)'];
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
                            title: { display: true, text: '금융 자산 총액' },
                            ticks: { callback: yTicksCallback }
                        }
                    },
                    onHover: function(event, elements) {
                        if (syncing || !dividendChart) return;
                        syncing = true;
                        if (!elements.length) {
                            dividendChart.tooltip.setActiveElements([], { x: 0, y: 0 });
                        } else {
                            var idx = elements[0].index;
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
                        var date = labels[elements[0].index];
                        htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + date, {
                            target: '#holdings-snapshot-container',
                            swap: 'innerHTML'
                        });
                    }
                }
            });

            dividendChart = new Chart(ctxDividend.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: '누적 결산 손익(실현)',
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
                            label: '수익 합산(결산+배당)',
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
                        zoom: commonZoomOptions,
                        tooltip: {
                            callbacks: {
                                // dataset label 은 숨기고 afterBody 에서 원하는 형태로 직접 구성
                                label: function() { return null; },
                                afterBody: function(tooltipItems) {
                                    if (!tooltipItems.length) return [];
                                    var idx = tooltipItems[0].dataIndex;
                                    var fmt = function(val) {
                                        var v = Math.round(parseFloat(val) || 0);
                                        return (v >= 0 ? '+' : '-') + new Intl.NumberFormat('ko-KR').format(Math.abs(v)) + '원';
                                    };
                                    var realized  = cumulativeRealizedProfitData[idx];
                                    var dividend  = cumulativeDividendData[idx];
                                    var total     = (parseFloat(realized) || 0) + (parseFloat(dividend) || 0);
                                    return [
                                        '누적 결산 손익: ' + fmt(realized),
                                        '누적 배당금:    ' + fmt(dividend),
                                        '─────────────────────',
                                        '수익 합산:      ' + fmt(total)
                                    ];
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
                            title: { display: true, text: '누적 손익 / 이익' },
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
                        var date = labels[elements[0].index];
                        htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + date, {
                            target: '#holdings-snapshot-container',
                            swap: 'innerHTML'
                        });
                    }
                }
            });

            // 초기 버튼 상태 업데이트
            checkNavButtons();

            // canvas 에 opacity 부드러운 전환 적용 (데이터 로딩 시 fade in/out 효과)
            var c1El = document.getElementById('assetGrowthChart');
            var c2El = document.getElementById('dividendChart');
            if (c1El) c1El.style.transition = 'opacity 0.25s ease';
            if (c2El) c2El.style.transition = 'opacity 0.25s ease';

            // 두 차트 plot 너비 맞추기: double-RAF 로 브라우저 렌더 완료 후 y1 축 너비 측정
            requestAnimationFrame(function() {
                requestAnimationFrame(function() {
                    if (!dividendChart || !dividendChart.scales || !dividendChart.scales.y1) return;
                    var y1Width = dividendChart.scales.y1.width || 0;
                    if (y1Width > 0) {
                        mainChart.options.layout = { padding: { right: y1Width } };
                        mainChart.update('none');
                    }
                });
            });
        })();
    </script>
    @endif
</div>
