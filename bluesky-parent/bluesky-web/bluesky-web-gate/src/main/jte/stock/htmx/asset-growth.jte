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

!{
    boolean isAllMode = "all".equals(rangeMode);
    java.time.ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? java.time.ZoneId.of(timeZone) : java.time.ZoneId.systemDefault();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone);

    java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate requestedStartLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate requestedEndDisplayLocal = endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1) : null;

    // Normalize model-provided `timeSeries` into `ts` for template use
    List<TradeProfitTimeSeriesPoint> ts = null;
    if (timeSeries instanceof List) {
        ts = (List<TradeProfitTimeSeriesPoint>) timeSeries;
    }

    if (startLocal == null && ts != null && !ts.isEmpty()) {
        try {
            TradeProfitTimeSeriesPoint firstPt = ts.get(0);
            if (firstPt != null && firstPt.timestamp() != null) startLocal = firstPt.timestamp().atZone(zone).toLocalDate();
        } catch (Exception e) { }
    }
    if (endLocal == null && ts != null && !ts.isEmpty()) {
        try {
            TradeProfitTimeSeriesPoint lastPt = ts.get(ts.size() - 1);
            if (lastPt != null && lastPt.timestamp() != null) endLocal = lastPt.timestamp().atZone(zone).toLocalDate();
        } catch (Exception e) { }
    }

    java.time.LocalDate today = java.time.LocalDate.now(zone);
    boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
    boolean canPrev = !isAllMode && startLocal != null && dataFirstDate != null && !dataFirstDate.isEmpty()
            && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));

    String assetGrowthTitle = MessageUtil.getMessage("stock.page.asset.growth.title");
    String chartHelpTitle = MessageUtil.getMessage("stock.asset.growth.help.title");
    String chartHelpWheel = MessageUtil.getMessage("stock.asset.growth.help.zoom.wheel");
    String chartHelpDrag = MessageUtil.getMessage("stock.asset.growth.help.zoom.drag");
    String chartHelpPan = MessageUtil.getMessage("stock.asset.growth.help.pan.alt.drag");
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
}

<div id="assetGrowthFragment" class="card bg-base-100 border border-base-200 rounded-box shadow-sm">
    <div class="card-body">
        <div class="flex items-center gap-2 mb-2">
            <h2 class="card-title text-base-content/70 m-0">${assetGrowthTitle}</h2>
            <div class="dropdown dropdown-hover dropdown-right">
                <label tabindex="0" class="btn btn-circle btn-ghost btn-xs text-info">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-5 h-5 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                </label>
                <div tabindex="0" class="card compact dropdown-content z-[10] shadow-lg bg-base-200 rounded-box w-max">
                    <div class="card-body">
                        <h3 class="card-title text-sm mb-1">${chartHelpTitle}</h3>
                        <ul class="text-xs list-disc pl-4 space-y-1 whitespace-nowrap">
                            <li>${chartHelpWheel}</li>
                            <li>${chartHelpDrag}</li>
                            <li>${chartHelpPan}</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
        <div class="w-full flex flex-col gap-4">
            @if(ts == null || ts.isEmpty())
                <div class="w-full flex justify-center items-center" style="height: 600px;">
                    <p class="text-gray-500">${noDataLabel}</p>
                </div>
            @else
                <div class="mb-2 flex flex-col gap-2 xl:flex-row xl:items-start xl:gap-4">
                    <div class="asset-growth-nav-wrapper flex min-w-0 flex-col gap-1 xl:flex-none xl:flex-row xl:items-center xl:gap-3">
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
                        <span id="assetRangeDisplay" class="whitespace-nowrap text-xs text-base-content/40"></span>
                    </div>
                    <p class="min-w-0 text-xs text-base-content/50 xl:pt-1 xl:text-right">
                        <span class="xl:whitespace-nowrap">📈 ${chartDescriptionLabel} &bull;</span>
                        <span class="inline-block whitespace-nowrap text-info">${clickDateHoldingsLabel}</span>
                    </p>
                    <style>
                        /* Keep the period controls content-sized on wide screens so the help text uses the remaining width. */
                        .asset-growth-nav-wrapper > div {
                            margin-bottom: 0 !important;
                            min-width: 0;
                        }
                        .asset-growth-nav-wrapper > div > * { flex-shrink: 0; }
                        @media (min-width: 1280px) {
                            .asset-growth-nav-wrapper > div {
                                flex: 0 0 auto;
                                flex-wrap: nowrap !important;
                            }
                        }
                    </style>
                </div>
                <div id="assetGrowthPeriodReturnSummary" class="rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3 mb-3">
                    <div class="skeleton h-4 w-32 mb-2"></div>
                    <div class="skeleton h-8 w-24 mb-2"></div>
                    <div class="skeleton h-3 w-56"></div>
                </div>
                <div class="w-full relative" style="height: 450px;">
                    <canvas id="assetGrowthChart" class="amount-chart"></canvas>
                    <div id="chartLoadingOverlay" class="absolute inset-0 flex items-center justify-center hidden" style="z-index:10; background-color:color-mix(in oklch, var(--color-base-100) 80%, transparent);">
                        <span class="loading loading-spinner loading-md text-primary"></span>
                    </div>
                </div>
                <p class="text-xs text-base-content/50 text-center py-1">💰 ${profitAnalysisLabel} &mdash; <span class="text-indigo-400">■</span> ${cumulativeRealizedDatasetLabel} + <span class="text-green-400">■</span> ${cumulativeDividendLabel} = ${cumulativeTotalDatasetLabel}</p>
                <div class="w-full" style="height: 250px;">
                    <canvas id="dividendChart" class="amount-chart"></canvas>
                </div>
                <div id="holdings-snapshot-container"></div>
                <div id="trade-history-panel"></div>
            @endif
        </div>
    </div>

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

            // Lightweight logging helper to trace sequence and values
            var agLogSeq = 0;
            var agLog = function(msg, obj) {
                try {
                    agLogSeq++;
                    console.debug && console.debug('[asset-growth][' + agLogSeq + '] ' + msg, obj || {});
                } catch(e) {}
            };
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
            try {
                console.debug && console.debug('[asset-growth] arrays', {
                    labels: labels.length,
                    totalValueData: totalValueData.length,
                    totalCostData: totalCostData.length,
                    currentUnrealizedProfitData: currentUnrealizedProfitData.length
                });
                console.debug && console.debug('[asset-growth] sample currentUnrealizedProfitData', currentUnrealizedProfitData.slice(0, 6));
            } catch (e) {}
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
                cumulativeDividendData: cumulativeDividendData.slice(),
                tradeCountData: tradeCountData.slice(),
                buyCountData: buyCountData.slice(),
                dailyRealizedProfitData: dailyRealizedProfitData.slice()
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
            var forceDetailFetch = false;
            var deferRangeDisplay = false;
            var deferredDisplayStart = null;
            var deferredDisplayEnd = null;
            var pendingBaseApply = null;
            var suppressDeferredDisplay = false;
            var deferredDisplayFallbackTimer = null;
            // DateRangePicker(콜백모드)가 내부 상태로 관리 — activeMonths 불필요
            var pendingVisStart = null;
            var pendingVisEnd   = null;
            var requestedStartLabel = '${requestedStartLocal != null ? requestedStartLocal.toString() : ""}';
            var requestedEndLabel = '${requestedEndDisplayLocal != null ? requestedEndDisplayLocal.toString() : ""}';
            var selectedRangeStart = requestedStartLabel || '';
            var selectedRangeEnd = requestedEndLabel || '';
            var selectedRangeMode = '${rangeMode != null ? rangeMode.replace("\\", "\\\\").replace("'", "\\'") : ""}';
            var periodReturnTimer = null;
            var periodReturnAbortController = null;
            var loadedPeriodReturnKey = '';
            var requestedPeriodReturnKey = '';
            var zoomOutLockRange = null;

            // 기간 표시 업데이트
            var updateRangeDisplay = function(startStr, endStr) {
                var el = document.getElementById('assetRangeDisplay');
                if (!el) return;
                if (startStr && endStr) {
                    el.textContent = startStr + ' ~ ' + endStr;
                } else {
                    el.textContent = '';
                }
            };

            var refreshPeriodReturnSummary = function(startStr, endStr) {
                if (!startStr || !endStr) return;

                var zone = '${timeZone != null ? timeZone.replace("\\", "\\\\").replace("'", "\\'") : ""}';
                if (!zone) {
                    try {
                        zone = Intl.DateTimeFormat().resolvedOptions().timeZone || '';
                    } catch (e) {
                        zone = '';
                    }
                }

                var key = startStr + '|' + endStr + '|' + zone;
                if (key === loadedPeriodReturnKey || key === requestedPeriodReturnKey) {
                    return;
                }

                clearTimeout(periodReturnTimer);
                periodReturnTimer = setTimeout(function() {
                    var container = document.getElementById('assetGrowthPeriodReturnSummary');
                    if (!container) return;

                    requestedPeriodReturnKey = key;
                    if (periodReturnAbortController) {
                        try { periodReturnAbortController.abort(); } catch (e) {}
                    }
                    periodReturnAbortController = typeof AbortController !== 'undefined'
                        ? new AbortController()
                        : null;

                    var options = {};
                    if (periodReturnAbortController) {
                        options.signal = periodReturnAbortController.signal;
                    }

                    fetch(
                        '/stock/htmx/asset-growth/period-return?from=' + encodeURIComponent(startStr)
                            + '&to=' + encodeURIComponent(endStr)
                            + '&timeZone=' + encodeURIComponent(zone),
                        options)
                        .then(function(response) { return response.text(); })
                        .then(function(html) {
                            requestedPeriodReturnKey = '';
                            loadedPeriodReturnKey = key;
                            if (container && html) {
                                container.outerHTML = html;
                            }
                        })
                        .catch(function(error) {
                            requestedPeriodReturnKey = '';
                            if (error && error.name === 'AbortError') {
                                return;
                            }
                            try {
                                agLog('asset growth period return fetch failed', {
                                    message: error && error.message ? error.message : String(error)
                                });
                            } catch (e) {}
                        });
                }, 120);
            };

            // 기간 선택 피커 onApply 콜백: 날짜 문자열 → 차트 인덱스 적용
            // Accept optional third arg `mode` (e.g. 'all') when DateRangePicker
            // calls the callback with a mode value. If mode==='all' or both
            // start/end are empty, treat as '전체' and apply baseData immediately.
            var applyRangeToChart = function(startStr, endStr, mode) {
                zoomOutLockRange = null;
                try {
                    if ((mode === 'all') || (!startStr && !endStr)) {
                        if (baseData && baseData.labels && baseData.labels.length > 0) {
                            // Treat 'all' as the entire available span. Prefer server-provided
                            // earliest date (`df`) if available; otherwise fall back to
                            // baseData's first label.
                            var rawStart = (typeof df !== 'undefined' && df) ? df : baseData.labels[0];
                            startStr = rawStart;
                            endStr = baseData.labels[baseData.labels.length - 1];
                            zoomOutLockRange = { start: startStr, end: endStr };
                            try { applyChartData(baseData, startStr, endStr); currentTierNum = baseDataTierNum; } catch (e) {}
                            updateRangeDisplay(startStr, endStr);
                            // mark that caller explicitly requested full-range; bypass
                            // the "base covers range" early-exit in checkAndFetchDetail
                            forceDetailFetch = true;
                        }
                    }

                    // If caller passed a numeric preset (e.g. '6' months) that is
                    // large enough, or special presets like 'ytd', prefer to force
                    // a detail fetch so the chart can obtain daily-resolution data
                    // and align precisely to the requested dates instead of
                    // snapping to coarse base labels.
                    try {
                        if (mode === 'ytd') {
                            forceDetailFetch = true;
                        } else if (mode && (/^\d+$/.test(String(mode)))) {
                            var _monthsNum = Number(mode);
                            if (!isNaN(_monthsNum) && _monthsNum >= 6) {
                                forceDetailFetch = true;
                            }
                        }
                    } catch (e) {}
                    // If we are explicitly forcing a detail fetch (large numeric preset),
                    // suppress applying the deferred label-aligned display text until
                    // the fetch completes to avoid a transient, incorrect label snap.
                    try {
                        suppressDeferredDisplay = !!forceDetailFetch;
                        if (suppressDeferredDisplay) {
                            if (deferredDisplayFallbackTimer) { clearTimeout(deferredDisplayFallbackTimer); deferredDisplayFallbackTimer = null; }
                            deferredDisplayFallbackTimer = setTimeout(function() {
                                suppressDeferredDisplay = false;
                                deferredDisplayFallbackTimer = null;
                                try { agLog('deferredDisplayFallback timer fired'); } catch(e) {}
                                setTimeout(checkNavButtons, 0);
                            }, 1000);
                        }
                    } catch (e) {}
                } catch (e) {}
                selectedRangeStart = startStr || '';
                selectedRangeEnd = endStr || '';
                selectedRangeMode = mode || '';
                if (!mainChart || !dividendChart) return;
                // DETAIL 범위를 벗어나면 BASE 로 복원
                if (currentTierNum > baseDataTierNum && labels.length > 0) {
                    if (startStr < labels[0] || endStr > labels[labels.length - 1]) {
                        applyChartData(baseData, baseData.labels[0], baseData.labels[baseData.labels.length - 1]);
                        currentTierNum = baseDataTierNum;
                    }
                }
                var newMinIdx = 0;
                for (var i = 0; i < labels.length; i++) { if (labels[i] >= startStr) { newMinIdx = i; break; } }
                var newMaxIdx = labels.length - 1;
                for (var k = labels.length - 1; k >= 0; k--) { if (labels[k] <= endStr) { newMaxIdx = k; break; } }
                if (newMaxIdx < newMinIdx) newMaxIdx = Math.min(newMinIdx + 1, labels.length - 1);
                pendingVisStart = startStr;
                pendingVisEnd   = endStr;
                // Prefer the raw requested dates for display when the requested
                // start/end fall outside the current `labels` (e.g. when the
                // chart currently contains a narrower detail window). If the
                // requested dates are inside the current labels range, align
                // them to the nearest label to keep trade-history/chart label
                // snapping consistent.
                var displayStart, displayEnd;
                if (labels && labels.length > 0) {
                    var firstLabel = labels[0];
                    var lastLabel = labels[labels.length - 1];
                    if (startStr >= firstLabel && startStr <= lastLabel) {
                        displayStart = labels[newMinIdx];
                    } else {
                        displayStart = startStr;
                    }
                    if (endStr >= firstLabel && endStr <= lastLabel) {
                        displayEnd = labels[newMaxIdx];
                    } else {
                        displayEnd = endStr;
                    }
                } else {
                    displayStart = startStr;
                    displayEnd = endStr;
                }
                // Defer visual text update until the chart animation / nav check
                // settles so we only update once (avoids double-flash).
                deferredDisplayStart = displayStart;
                deferredDisplayEnd = displayEnd;
                deferRangeDisplay = true;
                // Update the shared globalDateRange with the label-aligned values
                // so subsequent reloads or other fragments will see the same
                // normalized range instead of the raw requested dates.
                try {
                    if (typeof sessionStorage !== 'undefined') {
                        var __tz = (Intl && Intl.DateTimeFormat) ? Intl.DateTimeFormat().resolvedOptions().timeZone : null;
                        // Persist the original requested range (startStr/endStr) so
                        // other fragments receive the searched period, not the
                        // label-aligned display range which may be snapped.
                        sessionStorage.setItem('globalDateRange', JSON.stringify({ start: startStr || '', end: endStr || '', mode: mode || '', timeZone: __tz || '' }));
                        // trade-history refresh is handled by the global layout listener
                    }
                } catch (e) {}
                try { agLog('applyRangeToChart scheduled', { reqStart: startStr, reqEnd: endStr, mode: mode || '', displayStart: displayStart, displayEnd: displayEnd }); } catch(e) {}
                setLoading(true);
                clearTimeout(checkDetailTimer);
                checkDetailTimer = setTimeout(checkAndFetchDetail, 200);
                animateScaleChange(newMinIdx, newMaxIdx, 320);
            };
                        var assetPicker = window.DateRangePicker
                                            ? window.DateRangePicker.create({
                                                    btnClass:    'asset-range-btn',
                                                    activeClass: 'btn-primary',
                                                    minDate:     baseData.labels.length > 0 ? baseData.labels[0] : null,
                                                    maxDate:     baseData.labels.length > 0 ? baseData.labels[baseData.labels.length - 1] : null,
                                                    onApply:     applyRangeToChart,
                                                    // optional global sync key to unify period across pages
                                                    globalKey:   'globalDateRange',
                                                    // scope DOM queries to this fragment to avoid cross-fragment selection
                                                    rootSelector: '#assetGrowthFragment'
                                                })
                            : { set: function(){}, shift: function(){}, jumpToEdge: function(){}, setState: function(){}, setMaxDate: function(){} };
            window.assetPicker = assetPicker;
                        // prefer server-provided overall earliest date when available
                        try {
                            if (assetPicker && typeof assetPicker.setMinDate === 'function') {
                                var df = '${dataFirstDate != null ? dataFirstDate : ""}';
                                if (df) assetPicker.setMinDate(df);
                            }
                        } catch(e) {}

            // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
            try {
                if (assetPicker && typeof assetPicker.getState === 'function') {
                    var _s = assetPicker.getState();
                    if ((!_s || !_s.mode) && (!_s || !_s.start) && (!_s || !_s.end)) {
                            var _key = 'dateRangeDefaultApplied:assetGrowthFragment';
                            try {
                                if (!sessionStorage.getItem(_key)) {
                                        var _root = document.querySelector('#assetGrowthFragment') || document;
                                        var _btn = _root.querySelector('.asset-range-btn');
                                        assetPicker.set('mtd', _btn || null);
                                        // ensure visual activation: prefer btn-primary and remove btn-ghost if present
                                        try {
                                            if (_btn) {
                                                _btn.classList.add('btn-primary');
                                                _btn.classList.remove('btn-ghost');
                                            }
                                            console.debug && console.debug('assetPicker default applied, state=', assetPicker.getState());
                                        } catch(e) {}
                                        sessionStorage.setItem(_key, '1');
                                    }
                            } catch(e) { assetPicker.set('mtd', null); }
                    }
                }
            } catch(e) {}

            var scaleAnimFrame = null; // requestAnimationFrame handle for scale transition
            var programmaticViewportChange = false;

            // 뷰 구간을 부드럽게(easeOutCubic) 전환하는 헬퍼
            // shiftView / setViewRange / jumpToEdge 에서 사용
            var animateScaleChange = function(toMin, toMax, duration) {
                if (scaleAnimFrame !== null) {
                    cancelAnimationFrame(scaleAnimFrame);
                    scaleAnimFrame = null;
                }
                programmaticViewportChange = true;
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
                    programmaticViewportChange = false;
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
                        programmaticViewportChange = false;
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
                tradeCountData.length = 0;
                (newData.tradeCountData || []).forEach(function(v) { tradeCountData.push(v); });
                buyCountData.length = 0;
                (newData.buyCountData || []).forEach(function(v) { buyCountData.push(v); });
                dailyRealizedProfitData.length = 0;
                (newData.dailyRealizedProfitData || []).forEach(function(v) { dailyRealizedProfitData.push(v); });

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
                // 매수/매도 마커 업데이트 (afterDraw 플러그인용 외부 변수)
                buyMarkersData = buildBuyMarkers(labels, buyCountData);
                sellMarkersData = buildSellMarkers(labels, dailyRealizedProfitData);
                mainChart.options.scales.x.min = newMin;
                mainChart.options.scales.x.max = newMax;
                // DETAIL 데이터 교체 시 zoom limits 를 새 데이터 범위로 맞춤
                // BASE 복원 시에도 항상 0 ~ todayMaxIdx 로 갱신 (미래 날짜 차단)
                var zoomBounds = resolveZoomLimitBounds();
                mainChart.options.plugins.zoom.limits.x.min = zoomBounds.min;
                mainChart.options.plugins.zoom.limits.x.max = zoomBounds.max;

                dividendChart.data.labels = labels;
                dividendChart.data.datasets[0].data = cumulativeRealizedProfitData.slice();
                dividendChart.data.datasets[1].data = cumulativeRealizedProfitData.map(function(v, i) {
                    return parseFloat(v) + parseFloat(cumulativeDividendData[i]);
                });
                dividendChart.options.scales.x.min = newMin;
                dividendChart.options.scales.x.max = newMax;
                dividendChart.options.plugins.zoom.limits.x.min = zoomBounds.min;
                dividendChart.options.plugins.zoom.limits.x.max = zoomBounds.max;

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
                console.debug('[asset-growth] checkAndFetchDetail start', { mainChartExists: !!mainChart, isFetching: isFetching, labelsLen: labels.length, baseTier: baseDataTierNum, currentTier: currentTierNum });
                if (!mainChart || isFetching) { setLoading(false); console.debug('[asset-growth] aborted: no chart or already fetching'); return; }

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
                if (visibleDays < 0) { setLoading(false); console.debug('[asset-growth] aborted: visibleDays<0', visStart, visEnd); return; }
                var neededTier = visibleDays <= 180 ? 2 : (visibleDays <= 730 ? 1 : 0);
                console.debug('[asset-growth] visStart/visEnd', visStart, visEnd, 'visibleDays', visibleDays, 'neededTier', neededTier);

                // Quick client cache check BEFORE applying baseData fallback.
                // If we already have a cached detail entry that fully covers the
                // requested view at the needed granularity, apply it immediately
                // to avoid reverting to baseData and causing a UI flicker.
                try {
                    if (detailCacheEntries && detailCacheEntries.length > 0) {
                        for (var ci = 0; ci < detailCacheEntries.length; ci++) {
                            var ce = detailCacheEntries[ci];
                            if (ce.tier >= neededTier && ce.start <= visStart && ce.end >= visEnd) {
                                console.debug('[asset-growth] pre-base cache hit', { idx: ci, tier: ce.tier, start: ce.start, end: ce.end, neededTier: neededTier });
                                applyChartData(ce, visStart, visEnd);
                                currentTierNum = ce.tier;
                                setLoading(false);
                                return;
                            }
                        }
                    }
                } catch (e) { console.debug && console.debug('[asset-growth] pre-base cache check failed', e); }

                if (currentTierNum > baseDataTierNum) {
                    // ── DETAIL 모드 ──────────────────────────────────────────
                    // zoom limits = [0, DETAIL_labels.length-1] 이므로
                    // 완전히 줌아웃 시 scales.x.min=0, max=DETAIL_last → getOptDates() 는 labels 의 양 끝 날짜를 반환
                    // → 이 시점을 BASE 복원 트리거로 사용 (% 비교보다 명확하고 신뢰성 높음)
                    var atDetailEdge = visStart <= labels[0] && visEnd >= labels[labels.length - 1];
                    if (atDetailEdge) {
                        console.debug('[asset-growth] atDetailEdge -> restore baseData');
                        applyChartData(baseData, baseData.labels[0], baseData.labels[baseData.labels.length - 1]);
                        currentTierNum = baseDataTierNum;
                        setLoading(false);
                        return;
                    }

                    // 현재 DETAIL 데이터가 뷰포트를 완전히 커버하고 충분히 세밀하면 종료
                    var detailCoversView = visStart >= labels[0] && visEnd <= labels[labels.length - 1];
                    if (detailCoversView && neededTier <= currentTierNum) { console.debug('[asset-growth] detailCoversView && neededTier <= currentTierNum -> no fetch'); setLoading(false); return; }

                    // 커버 바깥이거나 더 세밀한 데이터 필요 → cache/fetch 로 fall through
                    // (아래 BASE 모드 조기 return 체크는 건너뜀)
                } else {
                        // ── BASE 모드 ─────────────────────────────────────────────
                        // 기본 데이터(baseData) 해상도가 충분하더라도 요청한 뷰가 baseData 범위 밖이면
                        // 서버에서 더 넓은 범위의 데이터를 가져와야 함. 따라서 baseData가 실제로 범위를
                        // 커버하는지 확인 후에만 조기 종료한다.
                        var baseCoversRange = false;
                        try {
                            baseCoversRange = baseData && baseData.labels && baseData.labels.length > 0
                                && visStart >= baseData.labels[0] && visEnd <= baseData.labels[baseData.labels.length - 1];
                        } catch (e) { baseCoversRange = false; }
                        // If base covers the range but we have a cached detail entry
                        // that partially overlaps the requested view, build a
                        // merged dataset preferring detail values where present
                        // and falling back to baseData otherwise. This avoids a
                        // full baseData replace (which causes visible flicker)
                        // when only part of the view requires base coverage.
                        try {
                            if (detailCacheEntries && detailCacheEntries.length > 0) {
                                var bestEntry = null;
                                var bestOverlap = 0;
                                var vStartTs = new Date(visStart).getTime();
                                var vEndTs = new Date(visEnd).getTime();
                                for (var di = 0; di < detailCacheEntries.length; di++) {
                                    var ent = detailCacheEntries[di];
                                    if (!ent || ent.tier < neededTier) continue;
                                    try {
                                        var es = new Date(ent.start).getTime();
                                        var ee = new Date(ent.end).getTime();
                                        var ovStart = Math.max(es, vStartTs);
                                        var ovEnd = Math.min(ee, vEndTs);
                                        var ov = Math.max(0, Math.round((ovEnd - ovStart) / 86400000));
                                        if (ov > bestOverlap) { bestOverlap = ov; bestEntry = ent; }
                                    } catch (ee) { /* ignore parse errors */ }
                                }
                                if (bestEntry && bestOverlap > 0 && !forceDetailFetch && neededTier > 0) {
                                    // construct merged labels (detail first, then base labels)
                                    var seen = {};
                                    var mergedLabels = [];
                                    for (var m = 0; m < bestEntry.labels.length; m++) {
                                        var d = bestEntry.labels[m];
                                        if (d >= visStart && d <= visEnd && !seen[d]) { mergedLabels.push(d); seen[d] = true; }
                                    }
                                    for (var m2 = 0; m2 < baseData.labels.length; m2++) {
                                        var d2 = baseData.labels[m2];
                                        if (d2 >= visStart && d2 <= visEnd && !seen[d2]) { mergedLabels.push(d2); seen[d2] = true; }
                                    }
                                    mergedLabels.sort();
                                    // build lookup maps for values
                                    var beMap = {};
                                    for (var ii = 0; ii < bestEntry.labels.length; ii++) {
                                        beMap[bestEntry.labels[ii]] = {
                                            tv: bestEntry.totalValueData[ii],
                                            tc: bestEntry.totalCostData[ii],
                                            cr: bestEntry.cumulativeRealizedProfitData[ii],
                                            cd: bestEntry.cumulativeDividendData[ii],
                                            tcnt: (bestEntry.tradeCountData || [])[ii] || 0,
                                            bcnt: (bestEntry.buyCountData || [])[ii] || 0,
                                            drp: (bestEntry.dailyRealizedProfitData || [])[ii] || 0
                                        };
                                    }
                                    var bdMap = {};
                                    for (var ii2 = 0; ii2 < baseData.labels.length; ii2++) {
                                        bdMap[baseData.labels[ii2]] = {
                                            tv: baseData.totalValueData[ii2],
                                            tc: baseData.totalCostData[ii2],
                                            cr: baseData.cumulativeRealizedProfitData[ii2],
                                            cd: baseData.cumulativeDividendData[ii2],
                                            tcnt: (baseData.tradeCountData || [])[ii2] || 0,
                                            bcnt: (baseData.buyCountData || [])[ii2] || 0,
                                            drp: (baseData.dailyRealizedProfitData || [])[ii2] || 0
                                        };
                                    }
                                    var newTotalValueData = [];
                                    var newTotalCostData = [];
                                    var newCumulativeRealizedProfitData = [];
                                    var newCumulativeDividendData = [];
                                    var newTradeCountData = [];
                                    var newBuyCountData = [];
                                    var newDailyRealizedProfitData = [];
                                    for (var mi = 0; mi < mergedLabels.length; mi++) {
                                        var ld = mergedLabels[mi];
                                        var src = beMap[ld] || bdMap[ld] || { tv: 0, tc: 0, cr: 0, cd: 0, tcnt: 0, bcnt: 0, drp: 0 };
                                        newTotalValueData.push(src.tv);
                                        newTotalCostData.push(src.tc);
                                        newCumulativeRealizedProfitData.push(src.cr);
                                        newCumulativeDividendData.push(src.cd);
                                        newTradeCountData.push(src.tcnt);
                                        newBuyCountData.push(src.bcnt);
                                        newDailyRealizedProfitData.push(src.drp);
                                    }
                                    var mergedData = {
                                        labels: mergedLabels,
                                        totalValueData: newTotalValueData,
                                        totalCostData: newTotalCostData,
                                        cumulativeRealizedProfitData: newCumulativeRealizedProfitData,
                                        cumulativeDividendData: newCumulativeDividendData,
                                        tradeCountData: newTradeCountData,
                                        buyCountData: newBuyCountData,
                                        dailyRealizedProfitData: newDailyRealizedProfitData
                                    };
                                    try { agLog('applying merged detail+base', { entryStart: bestEntry.start, entryEnd: bestEntry.end, overlapDays: bestOverlap }); } catch (e) {}
                                    applyChartData(mergedData, visStart, visEnd);
                                    currentTierNum = bestEntry.tier;
                                    setLoading(false);
                                    return;
                                } else {
                                    try { console.debug && console.debug('[asset-growth] merge skipped (forceDetailFetch=' + !!forceDetailFetch + ', neededTier=' + neededTier + ', bestEntryTier=' + (bestEntry ? bestEntry.tier : 'n/a') + ')'); } catch(e) {}
                                }
                            }
                        } catch (e) { console.debug && console.debug('[asset-growth] merge attempt failed', e); }
                        if (!forceDetailFetch && baseCoversRange && neededTier <= baseDataTierNum) {
                            console.debug('[asset-growth] BASE mode: baseData covers requested range and granularity sufficient -> apply baseData view and no fetch');
                            if (deferRangeDisplay) {
                                // Defer applying baseData until after chart animation/nav
                                // settle to avoid transient incorrect date display.
                                pendingBaseApply = { data: baseData, start: visStart, end: visEnd, tier: baseDataTierNum };
                                try { agLog('deferring baseData apply', pendingBaseApply); } catch(e) {}
                                setLoading(false);
                                return;
                            }
                            try { applyChartData(baseData, visStart, visEnd); } catch (e) { /* noop */ }
                            currentTierNum = baseDataTierNum;
                            setLoading(false);
                            return;
                        }
                        if (!forceDetailFetch && baseCoversRange && neededTier <= currentTierNum) {
                            console.debug('[asset-growth] BASE mode: neededTier <= currentTierNum and baseData covers range -> apply baseData view and no fetch');
                            if (deferRangeDisplay) {
                                pendingBaseApply = { data: baseData, start: visStart, end: visEnd, tier: currentTierNum };
                                try { agLog('deferring baseData apply (currentTier)', pendingBaseApply); } catch(e) {}
                                setLoading(false);
                                return;
                            }
                            try { applyChartData(baseData, visStart, visEnd); } catch (e) { /* noop */ }
                            setLoading(false);
                            return;
                        }
                }

                // 클라이언트 캐시 확인
                console.debug('[asset-growth] checking cache entries', detailCacheEntries.length);
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
                var capturedNeededTier = forceDetailFetch ? 2 : neededTier;
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

                    // Ensure trade-history updates use the same requested (non-buffered)
                    // visible range as the upcoming detail fetch so both components
                    // remain in sync and avoid flicker from out-of-order responses.
                    try {
                        agLog('skipping local refreshTradeHistory; global listener will refresh trade-history', { from: capturedVisStart, to: capturedVisEnd });
                    } catch (e) {}

                    console.debug('[asset-growth] fetching detail', { startStr: startStr, endStr: endStr, gran: capturedNeededTier === 2 ? 'DAILY' : 'WEEKLY' });

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
                                cumulativeDividendData: data.cumulativeDividendData,
                                tradeCountData: data.tradeCountData || [],
                                buyCountData: data.buyCountData || [],
                                dailyRealizedProfitData: data.dailyRealizedProfitData || []
                            };
                            detailCacheEntries.push(entry);
                            // fetch 완료 후 applyChartData 의 표시 범위는 capturedVisStart/End 사용
                            // (getOptDates() 로 재읽으면 BASE labels 기준 좁은 범위가 반환되어 화면이 비어 보임)
                            var curVD = (new Date(capturedVisEnd) - new Date(capturedVisStart)) / 86400000;
                            var viewIsNarrowEnough =
                                (entry.tier === 2 && curVD <= 180) ||
                                (entry.tier === 1 && curVD <= 730);
                            var entryCoversView = entry.start <= capturedVisStart && entry.end >= capturedVisEnd;
                            console.debug && console.debug('[asset-growth] fetched entry', { tier: entry.tier, start: entry.start, end: entry.end, curVD: curVD, entryCoversView: entryCoversView, viewIsNarrowEnough: viewIsNarrowEnough });
                            // Apply immediately if the fetched entry actually covers the requested view
                            if (entryCoversView || viewIsNarrowEnough) {
                                try { agLog('applying fetched entry to chart', { tier: entry.tier, entryStart: entry.start, entryEnd: entry.end, visStart: capturedVisStart, visEnd: capturedVisEnd }); } catch(e) {}
                                // Clear any deferred base application since fetched detail
                                // supersedes it.
                                pendingBaseApply = null;
                                applyChartData(entry, capturedVisStart, capturedVisEnd);
                                currentTierNum = entry.tier;
                            } else {
                                console.debug && console.debug('[asset-growth] fetched entry cached only (will apply when view narrows or when cache covers view)');
                            }
                        })
                        .catch(function(e) { console.warn('Detail fetch failed:', e); })
                        .finally(function() {
                            setFetching(false);
                            forceDetailFetch = false;
                            try {
                                if (deferredDisplayFallbackTimer) { clearTimeout(deferredDisplayFallbackTimer); deferredDisplayFallbackTimer = null; }
                            } catch (e) {}
                            suppressDeferredDisplay = false;
                            // Ensure nav/buttons/display update now that fetch completed
                            setTimeout(checkNavButtons, 0);
                        });
                }, 400);
            };
            // ─────────────────────────────────────────────────────────────────

            // 오늘 날짜 기준 최대 인덱스 (미래 projected 데이터 포함 시에도 오늘까지만 표시)
            var getTodayMaxIdx = function() {
                var ts = new Date().toISOString().slice(0, 10);
                for (var i = labels.length - 1; i >= 0; i--) {
                    if (labels[i] <= ts) return i;
                }
                return labels.length - 1;
            };
            var maxIndex = labels.length - 1;
            var todayMaxIdx = getTodayMaxIdx();
            var requestedStartLabel = '${requestedStartLocal != null ? requestedStartLocal.toString() : ""}';
            var requestedEndLabel = '${requestedEndDisplayLocal != null ? requestedEndDisplayLocal.toString() : ""}';
            var defaultMinIndex = todayMaxIdx > 365 ? todayMaxIdx - 365 : 0;
            if (requestedStartLabel) {
                for (var si = 0; si < labels.length; si++) {
                    if (labels[si] >= requestedStartLabel) {
                        defaultMinIndex = si;
                        break;
                    }
                }
            }
            var defaultMaxIndex = todayMaxIdx;
            if (requestedEndLabel) {
                for (var ei = labels.length - 1; ei >= 0; ei--) {
                    if (labels[ei] <= requestedEndLabel) {
                        defaultMaxIndex = ei;
                        break;
                    }
                }
            }
            if (defaultMaxIndex < defaultMinIndex) {
                defaultMaxIndex = Math.min(todayMaxIdx, defaultMinIndex + 1);
            }
            var defaultMinDate = labels[defaultMinIndex];
            var defaultMaxDate = labels[defaultMaxIndex];

            var mainChart, dividendChart;

            var resolveZoomLimitBounds = function() {
                if (!labels || labels.length === 0) {
                    return { min: 0, max: 0 };
                }

                var minBound = 0;
                var maxBound = getTodayMaxIdx();

                if (zoomOutLockRange && zoomOutLockRange.start && zoomOutLockRange.end) {
                    for (var leftIdx = 0; leftIdx < labels.length; leftIdx++) {
                        if (labels[leftIdx] >= zoomOutLockRange.start) {
                            minBound = leftIdx;
                            break;
                        }
                    }
                    for (var rightIdx = Math.min(maxBound, labels.length - 1); rightIdx >= minBound; rightIdx--) {
                        if (labels[rightIdx] <= zoomOutLockRange.end) {
                            maxBound = rightIdx;
                            break;
                        }
                    }
                    if (maxBound < minBound) {
                        minBound = 0;
                        maxBound = getTodayMaxIdx();
                    }
                }

                return { min: minBound, max: maxBound };
            };

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

                var visMinDate = labels[minIdx] || '';
                var visMaxDate = labels[maxIdx] || '';
                var pickerRangeStart = selectedRangeStart || visMinDate;
                var pickerRangeEnd = selectedRangeEnd || visMaxDate;

                // Ensure picker internal state matches current view so canShift uses correct values
                try {
                    if (typeof assetPicker !== 'undefined' && assetPicker && typeof assetPicker.setState === 'function') {
                        assetPicker.setState(
                            pickerRangeStart,
                            pickerRangeEnd,
                            selectedRangeMode || '',
                        );
                    }
                } catch (e) {}

                // Prefer DateRangePicker.canShift when available (single source of truth)
                var disablePrev = false, disableNext = false;
                try {
                    if (typeof assetPicker !== 'undefined' && assetPicker && typeof assetPicker.canShift === 'function') {
                        disablePrev = !assetPicker.canShift(-1);
                        disableNext = !assetPicker.canShift(1);
                    } else {
                        // fallback to previous logic
                        var atStart = visMinDate <= baseData.labels[0];
                        var atEnd   = visMaxDate >= new Date().toISOString().slice(0, 10);
                        disablePrev = atStart;
                        disableNext = atEnd;
                    }
                } catch (e) {
                    disablePrev = disableNext = false;
                }

                // scope to this fragment to avoid touching other navs on the page
                var rootEl = document.getElementById('assetGrowthFragment') || document;
                var prevEls = Array.from(rootEl.querySelectorAll('.date-range-prev'));
                var nextEls = Array.from(rootEl.querySelectorAll('.date-range-next'));
                // include legacy ID buttons if present
                ['btnFirst','btnPrev'].forEach(function(id){ var e = document.getElementById(id); if (e && prevEls.indexOf(e) === -1) prevEls.push(e); });
                ['btnNext','btnLast'].forEach(function(id){ var e = document.getElementById(id); if (e && nextEls.indexOf(e) === -1) nextEls.push(e); });

                var applyDisableToList = function(list, disabled) {
                    list.forEach(function(el){
                        try {
                            el.style.opacity = disabled ? '0.2' : '1';
                            el.style.pointerEvents = disabled ? 'none' : 'auto';
                            el.disabled = disabled;
                            if (disabled) {
                                el.classList.add('btn-disabled');
                                el.classList.add('opacity-40');
                            } else {
                                el.classList.remove('btn-disabled');
                                el.classList.remove('opacity-40');
                            }
                        } catch (e) { /* ignore */ }
                    });
                };

                applyDisableToList(prevEls, disablePrev);
                applyDisableToList(nextEls, disableNext);
                // If a range update was deferred while the chart animated, prefer
                // to show the requested display range (captured earlier) rather
                // than the label-aligned chart labels which can cause a brief
                // incorrect date to appear. Clear the deferred values after use.
                try {
                    if (deferRangeDisplay) {
                        // If suppression requested (we expect a detail fetch), hold
                        // off on showing the deferred label-aligned display to avoid
                        // a transient wrong date. The fallback timer will clear
                        // suppression if fetch doesn't complete in time.
                        if (suppressDeferredDisplay) {
                            try { agLog('suppressing deferred display until fetch completes'); } catch(e) {}
                            return;
                        }
                        // If a pending base apply exists for the deferred range,
                        // apply it now, but keep the user-selected range for the
                        // summary/period text instead of label-aligned chart dates.
                        if (pendingBaseApply) {
                            try { agLog('applying pendingBaseApply from checkNavButtons', pendingBaseApply); } catch (e) {}
                            var p = pendingBaseApply; pendingBaseApply = null;
                            deferRangeDisplay = false; deferredDisplayStart = deferredDisplayEnd = null;
                            applyChartData(p.data, p.start, p.end);
                            currentTierNum = p.tier;
                            // applyChartData will call checkNavButtons again; bail out now
                            return;
                        }
                        deferRangeDisplay = false;
                        deferredDisplayStart = deferredDisplayEnd = null;
                        updateRangeDisplay(pickerRangeStart, pickerRangeEnd);
                        refreshPeriodReturnSummary(pickerRangeStart, pickerRangeEnd);
                    } else {
                        updateRangeDisplay(pickerRangeStart, pickerRangeEnd);
                        refreshPeriodReturnSummary(pickerRangeStart, pickerRangeEnd);
                    }
                } catch (e) {}
            };

            var syncZoomPan = function({chart}) {
                if (syncing || programmaticViewportChange) {
                    return;
                }
                var target = (chart.canvas.id === 'assetGrowthChart') ? dividendChart : mainChart;
                if(target) {
                    target.options.scales.x.min = chart.scales.x.min;
                    target.options.scales.x.max = chart.scales.x.max;
                    target.update('none');
                }
                // 줌/팬 시 퀵셀렉터 비활성화 — 현재 뷰 span 으로 이전/다음 이동
                var od = getOptDates();
                selectedRangeStart = od.visStart || '';
                selectedRangeEnd = od.visEnd || '';
                selectedRangeMode = '';
                assetPicker.setState(od.visStart, od.visEnd, '');
                syncDraggedRangeToGlobal(od.visStart, od.visEnd);
                setTimeout(checkNavButtons, 50);
                try { agLog('syncZoomPan scheduling checkAndFetchDetail', { visStart: od.visStart, visEnd: od.visEnd }); } catch(e) {}
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
                    assetPicker.shift(-1);
                } else if (xMax >= labels.length - 1 && moreRight) {
                    assetPicker.shift(1);
                }
            };

            var commonZoomOptions = {
                limits: {
                    x: { min: 0, max: labels.length > 0 ? getTodayMaxIdx() : 0, minRange: 9 } // applyChartData 에서 갱신됨
                },
                zoom: {
                    wheel: { enabled: false }, // 커스텀 휠 핸들러로 대체 (차트 중심 기준 줌)
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

            // \ub9e4\uc218/\ub9e4\ub3c4 \ub9c8\ucee4 \ub370\uc774\ud130 \uc0dd\uc131: tradeCount > 0 \uc778 \uc778\ub371\uc2a4\ub97c scatter \ud3ec\uc778\ud2b8\ub85c \ubcc0\ud658
            // dailyRealizedProfit > 0 \uc774\uba74 \ub9e4\ub3c4(\uc5ed\uc0bc\uac01\ud615), \uc544\ub2c8\uba74 \ub9e4\uc218(\uc0bc\uac01\ud615)
            var buildBuyMarkers = function(lbls, bcounts) {
                var pts = [];
                for (var i = 0; i < lbls.length; i++) {
                    if (Number(bcounts[i]) > 0) {
                        pts.push({ date: lbls[i] }); // 인덱스 대신 날짜 문자열 저장
                    }
                }
                return pts;
            };
            var buildSellMarkers = function(lbls, drealized) {
                var pts = [];
                for (var i = 0; i < lbls.length; i++) {
                    if (parseFloat(drealized[i]) > 0) {
                        pts.push({ date: lbls[i] }); // 인덱스 대신 날짜 문자열 저장
                    }
                }
                return pts;
            };

            var buyMarkersData = buildBuyMarkers(labels, buyCountData);
            var sellMarkersData = buildSellMarkers(labels, dailyRealizedProfitData);
            var drawnMarkers = []; // afterDatasetsDraw에서 갱신, { px, py, date } 형태

            var syncing = false;

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
                        zoom: commonZoomOptions,
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
                        zoom: commonZoomOptions,
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

            // 세션에 저장된 전역 기간이 있으면 차트 생성 직후 적용
            try {
                var __rawApply = (typeof sessionStorage !== 'undefined') ? sessionStorage.getItem('globalDateRange') : null;
                if (__rawApply) {
                    var __gApply = JSON.parse(__rawApply);
                    if (__gApply && (__gApply.start || __gApply.end || __gApply.mode)) {
                        try { assetPicker.setState(__gApply.start || '', __gApply.end || '', __gApply.mode || ''); } catch (e) {}
                        try {
                            // If a saved mode exists (e.g. 6, 'mtd', 'ytd'), prefer re-computing
                            // the start/end from the mode (so it uses current maxDate/minDate)
                            if (__gApply.mode) {
                                try {
                                    var __modeArg = (__gApply.mode === 'all') ? 0 : __gApply.mode;
                                    // Find the corresponding quick-button element and pass it
                                    // to assetPicker.set so the picker's internal visual
                                    // state update doesn't clear the active class.
                                    var __foundBtn = null;
                                    try {
                                        var __rootBtn = document.querySelector('#assetGrowthFragment') || document;
                                        var __cands = Array.from((__rootBtn).querySelectorAll('.asset-range-btn')) || [];
                                        for (var __i = 0; __i < __cands.length; __i++) {
                                            var __c = __cands[__i];
                                            var __onclick = __c.getAttribute && __c.getAttribute('onclick') ? __c.getAttribute('onclick') || '' : '';
                                            if (__onclick.indexOf('set(' + __gApply.mode + ',') !== -1 || __onclick.indexOf("set('" + __gApply.mode + "'") !== -1 || __onclick.indexOf('set(' + __modeArg + ',') !== -1) { __foundBtn = __c; break; }
                                        }
                                    } catch (e) {}
                                    assetPicker.set(__modeArg, __foundBtn);
                                } catch (e) {
                                    applyRangeToChart(__gApply.start || '', __gApply.end || '', __gApply.mode || '');
                                }
                            } else {
                                try { applyRangeToChart(__gApply.start || '', __gApply.end || '', __gApply.mode || ''); } catch (e) {}
                            }
                        } catch (e) {}
                    }
                }
            } catch (e) {}

            // 초기 버튼 상태 업데이트
            checkNavButtons();

 

            // canvas 에 opacity 부드러운 전환 적용 (데이터 로딩 시 fade in/out 효과)
            var c1El = document.getElementById('assetGrowthChart');
            var c2El = document.getElementById('dividendChart');
            if (c1El) c1El.style.transition = 'opacity 0.25s ease';
            if (c2El) c2El.style.transition = 'opacity 0.25s ease';

            // 마커 삼각형 클릭 감지 (afterDraw로 그린 삼각형은 Chart.js hit detection 밖이므로 직접 처리)
            var markerClicked = false; // Chart.js onClick 중복 호출 방지 플래그
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

            // 휠 스크롤: 마우스 위치가 줌 후 뷰 중앙에 오도록 확대/축소
            var handleCenteredWheel = function(chart, event) {
                event.preventDefault();
                var xAxis = chart.scales['x'];
                if (!xAxis) return;
                var currentMin = xAxis.min;
                var currentMax = xAxis.max;
                var currentRange = currentMax - currentMin;
                var limits = chart.options.plugins.zoom.limits.x;
                var minBound = typeof limits.min === 'number' ? limits.min : 0;
                var maxBound = typeof limits.max === 'number' ? Math.min(limits.max, getTodayMaxIdx()) : getTodayMaxIdx();
                if (maxBound < minBound) {
                    minBound = 0;
                    maxBound = getTodayMaxIdx();
                }
                var minRange = limits.minRange || 9;
                var maxRange = Math.max(minRange, maxBound - minBound);
                // deltaY > 0: 축소, < 0: 확대
                var speed = 0.15;
                var zoomFactor = event.deltaY > 0 ? (1 + speed) : (1 - speed);
                var newRange = currentRange * zoomFactor;
                newRange = Math.max(minRange, Math.min(maxRange, newRange));
                if (event.deltaY > 0 && currentMin <= minBound && currentMax >= maxBound) {
                    return;
                }
                // 마우스가 위치한 데이터 인덱스를 구해서 새 뷰의 중앙으로 배치
                var rect = chart.canvas.getBoundingClientRect();
                var mouseX = event.clientX - rect.left;
                var mouseIndex = xAxis.getValueForPixel(mouseX);
                // mouseIndex가 새 범위의 중심이 되도록 계산
                var newMin = mouseIndex - newRange / 2;
                var newMax = mouseIndex + newRange / 2;
                // 경계 보정
                if (newMin < minBound) { newMax = Math.min(maxBound, newMax + (minBound - newMin)); newMin = minBound; }
                if (newMax > maxBound) { newMin = Math.max(minBound, newMin - (newMax - maxBound)); newMax = maxBound; }
                chart.options.scales.x.min = newMin;
                chart.options.scales.x.max = newMax;
                chart.update('none');
                syncZoomPan({chart: chart});
            };
            if (c1El) c1El.addEventListener('wheel', function(e) { handleCenteredWheel(mainChart, e); }, { passive: false });
            if (c2El) c2El.addEventListener('wheel', function(e) { handleCenteredWheel(dividendChart, e); }, { passive: false });

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

            // ── 매매 내역 자동 갱신 ──────────────────────────────────────────
            var tradeHistoryTimer = null;
            var chartRangeSyncTimer = null;
            var lastChartRangeSyncKey = '';
            var syncDraggedRangeToGlobal = function(from, to) {
                if (!from || !to) return;
                clearTimeout(chartRangeSyncTimer);
                chartRangeSyncTimer = setTimeout(function() {
                    var nextKey = from + '::' + to;
                    if (nextKey === lastChartRangeSyncKey) {
                        return;
                    }
                    lastChartRangeSyncKey = nextKey;

                    var nextTimeZone = '';
                    try {
                        var raw = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('globalDateRange') : null;
                        var saved = raw ? JSON.parse(raw) : null;
                        nextTimeZone = saved && (saved.timeZone || saved.timezone)
                            ? (saved.timeZone || saved.timezone)
                            : ((Intl && Intl.DateTimeFormat) ? Intl.DateTimeFormat().resolvedOptions().timeZone : '');
                        if (typeof sessionStorage !== 'undefined') {
                            sessionStorage.setItem('globalDateRange', JSON.stringify({
                                start: from,
                                end: to,
                                mode: '',
                                timeZone: nextTimeZone || ''
                            }));
                        }
                    } catch (e) {}

                    try {
                        if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
                            window.dispatchEvent(new CustomEvent('globalDateRange:changed', {
                                detail: {
                                    start: from,
                                    end: to,
                                    mode: '',
                                    timeZone: nextTimeZone || ''
                                }
                            }));
                        }
                    } catch (e) {}
                }, 450);
            };
            var refreshTradeHistory = function(from, to) {
                clearTimeout(tradeHistoryTimer);
                tradeHistoryTimer = setTimeout(function() {
                    var url = '/stock/htmx/trade-history';
                    if (from && to) {
                        url += '?from=' + from + '&to=' + to;
                    }
                    htmx.ajax('GET', url, {
                        target: '#trade-history-panel',
                        swap: 'outerHTML'
                    });
                }, 500);
            };

            // 초기 로드: trade-history 갱신은 전역 레이아웃의 globalDateRange 핸들러에서 수행합니다.
        })();
    </script>
    @endif
</div>
