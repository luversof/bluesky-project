"use strict";
const StockCharts = {};
const TRADE_COLORS = [
    'rgba(99,102,241,0.8)', 'rgba(34,197,94,0.8)', 'rgba(251,191,36,0.8)',
    'rgba(239,68,68,0.8)', 'rgba(59,130,246,0.8)', 'rgba(236,72,153,0.8)',
    'rgba(14,165,233,0.8)', 'rgba(249,115,22,0.8)', 'rgba(168,85,247,0.8)',
    'rgba(20,184,166,0.8)', 'rgba(245,158,11,0.8)', 'rgba(16,185,129,0.8)'
];
const PROFIT_COLORS_POS = [
    'rgba(239,68,68,0.85)', 'rgba(220,38,38,0.8)', 'rgba(249,115,22,0.8)',
    'rgba(253,164,175,0.85)', 'rgba(236,72,153,0.8)', 'rgba(245,158,11,0.8)'
];
const PROFIT_COLORS_NEG = [
    'rgba(59,130,246,0.85)', 'rgba(14,165,233,0.8)', 'rgba(99,102,241,0.8)'
];
function fmtAmt(v) { return Number(v).toLocaleString('ko-KR'); }
function resolveLocale() {
    var _a, _b;
    return ((_a = document.body) === null || _a === void 0 ? void 0 : _a.dataset.locale) || ((_b = document.documentElement) === null || _b === void 0 ? void 0 : _b.lang) || navigator.language || 'ko-KR';
}
function compactNumber(value) {
    const numeric = Number(value) || 0;
    const abs = Math.abs(numeric);
    const sign = numeric < 0 ? '-' : '';
    const locale = resolveLocale();
    if (abs >= 100000000) {
        const digits = abs >= 1000000000 ? 0 : 1;
        return sign + new Intl.NumberFormat(locale, { maximumFractionDigits: digits }).format(abs / 100000000) + '억';
    }
    if (abs >= 10000) {
        const digits = abs >= 1000000 ? 0 : 1;
        return sign + new Intl.NumberFormat(locale, { maximumFractionDigits: digits }).format(abs / 10000) + '만';
    }
    return sign + new Intl.NumberFormat(locale).format(abs);
}
StockCharts.getLocale = function () {
    return resolveLocale();
};
StockCharts.formatNumber = function (value) {
    return new Intl.NumberFormat(resolveLocale()).format(Number(value) || 0);
};
StockCharts.formatCurrency = function (value) {
    const numeric = Math.round(Number(value) || 0);
    return '₩' + StockCharts.formatNumber(numeric);
};
StockCharts.formatCompactNumber = function (value) {
    return compactNumber(value);
};
function buildMonthlyData(tradeData = []) {
    const buyMap = {}, sellMap = {};
    tradeData.forEach((d) => {
        if (!d || !d.tradeDate)
            return;
        const mon = d.tradeDate.slice(0, 7);
        const amt = Number(d.amount) || 0;
        if (d.type === 'BUY')
            buyMap[mon] = (buyMap[mon] || 0) + amt;
        else if (d.type === 'SELL')
            sellMap[mon] = (sellMap[mon] || 0) + amt;
    });
    const allMonths = Object.keys(buyMap).concat(Object.keys(sellMap));
    const months = allMonths.filter((v, i, a) => a.indexOf(v) === i).sort();
    return {
        labels: months,
        buyData: months.map(m => buyMap[m] || 0),
        sellData: months.map(m => sellMap[m] || 0)
    };
}
function buildDonutData(tradeData = [], metric = 'profit', groupBy = 'stock') {
    const map = {};
    tradeData.forEach(d => {
        const key = groupBy === 'stock' ? d.stockItem : d.account;
        if (!key)
            return;
        if (metric === 'profit') {
            if (d.type !== 'SELL')
                return;
            map[key] = (map[key] || 0) + (Number(d.profit) || 0);
        }
        else {
            if (d.type !== 'BUY')
                return;
            map[key] = (map[key] || 0) + (Number(d.amount) || 0);
        }
    });
    if (metric === 'profit') {
        const entries = Object.entries(map);
        const pos = entries.filter(e => e[1] >= 0).sort((a, b) => b[1] - a[1]);
        const neg = entries.filter(e => e[1] < 0).sort((a, b) => a[1] - b[1]);
        const sorted = pos.concat(neg);
        const posLen = pos.length;
        return {
            labels: sorted.map(e => e[0]),
            data: sorted.map(e => Math.abs(e[1])),
            rawData: sorted.map(e => e[1]),
            colors: sorted.map((e, i) => e[1] >= 0 ? PROFIT_COLORS_POS[i % PROFIT_COLORS_POS.length] : PROFIT_COLORS_NEG[(i - posLen) % PROFIT_COLORS_NEG.length])
        };
    }
    else {
        const sorted = Object.entries(map).sort((a, b) => b[1] - a[1]);
        return {
            labels: sorted.map(e => e[0]),
            data: sorted.map(e => e[1]),
            rawData: sorted.map(e => e[1]),
            colors: TRADE_COLORS.slice(0, sorted.length)
        };
    }
}
function makeDonutTooltipHandler(rawData, isProfitMode, tooltipId = 'tradeDonutTooltipEl') {
    return function (context) {
        const tooltip = context.tooltip;
        let tooltipEl = document.getElementById(tooltipId);
        if (!tooltipEl) {
            tooltipEl = document.createElement('div');
            tooltipEl.id = tooltipId;
            tooltipEl.style.cssText = 'position:fixed;z-index:9999;background:rgba(30,30,30,0.92);color:#fff;padding:6px 10px;border-radius:6px;font-size:12px;pointer-events:none;white-space:nowrap;transition:opacity .1s;';
            document.body.appendChild(tooltipEl);
        }
        if (tooltip.opacity === 0) {
            tooltipEl.style.opacity = '0';
            return;
        }
        const idx = tooltip.dataPoints && tooltip.dataPoints[0] ? tooltip.dataPoints[0].dataIndex : 0;
        const label = tooltip.title && tooltip.title[0] ? tooltip.title[0] : '';
        const raw = rawData[idx] !== undefined ? rawData[idx] : 0;
        const sign = isProfitMode ? (raw >= 0 ? '\u25b2 ' : '\u25bc ') : '';
        const valText = sign + '\u20a9' + fmtAmt(Math.abs(raw));
        const color = context.chart.data.datasets[0].backgroundColor[idx] || '#999';
        tooltipEl.innerHTML = '<div style="font-weight:bold;margin-bottom:3px">' + label + '</div>'
            + '<div style="display:flex;align-items:center;gap:5px">'
            + '<span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:' + color + '"></span>'
            + '<span>' + label + ': ' + valText + '</span>'
            + '</div>';
        const rect = context.chart.canvas.getBoundingClientRect();
        const x = rect.left + tooltip.caretX;
        const y = rect.top + tooltip.caretY;
        tooltipEl.style.opacity = '1';
        tooltipEl.style.left = '0px';
        tooltipEl.style.top = '0px';
        const tw = tooltipEl.offsetWidth;
        const th = tooltipEl.offsetHeight;
        let finalX = x + 12;
        let finalY = y - th - 8;
        if (finalX + tw > window.innerWidth - 8)
            finalX = x - tw - 12;
        if (finalY < 8)
            finalY = y + 16;
        tooltipEl.style.left = finalX + 'px';
        tooltipEl.style.top = finalY + 'px';
    };
}
StockCharts.initMonthlyFromData = function (tradeData, canvasId = 'tradeMonthlyChart', existingInstance) {
    const m = buildMonthlyData(tradeData);
    const ctx = document.getElementById(canvasId);
    if (!ctx)
        return null;
    if (existingInstance)
        try {
            existingInstance.destroy();
        }
        catch (e) { }
    const gridColor = 'rgba(128,128,128,0.1)';
    const inst = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: m.labels,
            datasets: [
                { label: '매수', data: m.buyData, backgroundColor: 'rgba(239,68,68,0.7)', borderRadius: 3 },
                { label: '매도', data: m.sellData, backgroundColor: 'rgba(59,130,246,0.7)', borderRadius: 3 }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'top', labels: { font: { size: 11 }, boxWidth: 12 } },
                tooltip: { callbacks: { label: (ctx) => { return ctx.dataset.label + ': \u20a9' + fmtAmt(ctx.parsed.y); } } }
            },
            scales: {
                x: { grid: { color: gridColor }, ticks: { font: { size: 10 } } },
                y: {
                    grid: { color: gridColor },
                    ticks: {
                        font: { size: 10 },
                        callback: (v) => {
                            if (v >= 100000000)
                                return (v / 100000000).toFixed(0) + '억';
                            if (v >= 10000)
                                return (v / 10000).toFixed(0) + '만';
                            return v.toLocaleString('ko-KR');
                        }
                    }
                }
            }
        }
    });
    return inst;
};
StockCharts.initDonutFromData = function (tradeData, opts = {}, existingInstance) {
    const metric = opts.metric || 'profit';
    const groupBy = opts.groupBy || 'stock';
    const canvas = document.getElementById(opts.canvasId || 'tradeDonutChart');
    if (!canvas)
        return null;
    const d = buildDonutData(tradeData, metric, groupBy);
    if (existingInstance)
        try {
            existingInstance.destroy();
        }
        catch (e) { }
    const oldTip = document.getElementById(opts.tooltipId || 'tradeDonutTooltipEl');
    if (oldTip)
        oldTip.style.opacity = '0';
    const isProfitMode = metric === 'profit';
    const legendEl = opts.legendId ? document.getElementById(opts.legendId) : null;
    if (d.labels.length === 0) {
        canvas.style.display = 'none';
        if (legendEl)
            legendEl.innerHTML = '<div class="text-xs opacity-40 pt-4 text-center">해당 기간 ' + (isProfitMode ? '매도 내역' : '매수 내역') + '이 없습니다.</div>';
        return null;
    }
    canvas.style.display = '';
    const inst = new Chart(canvas, {
        type: 'doughnut',
        data: { labels: d.labels, datasets: [{ data: d.data, backgroundColor: d.colors, borderWidth: 1 }] },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: { display: false },
                tooltip: { enabled: false, external: makeDonutTooltipHandler(d.rawData, isProfitMode, opts.tooltipId || 'tradeDonutTooltipEl') }
            }
        }
    });
    const titleEl = opts.titleId ? document.getElementById(opts.titleId) : null;
    if (titleEl)
        titleEl.textContent = isProfitMode ? '종목별 손익 기여' : '매수 집중도';
    if (legendEl) {
        const total = d.data.reduce((a, b) => a + b, 0);
        legendEl.innerHTML = d.labels.map((l, i) => {
            const pct = total > 0 ? ((d.data[i] / total) * 100).toFixed(1) : '0.0';
            const raw = d.rawData[i];
            const sign = isProfitMode ? (raw >= 0 ? '\u25b2 ' : '\u25bc ') : '';
            const valClass = isProfitMode ? (raw >= 0 ? 'color:rgba(239,68,68,0.9)' : 'color:rgba(59,130,246,0.9)') : '';
            return '<div class="flex items-center gap-1 mb-0.5">'
                + '<span style="flex-shrink:0;display:inline-block;width:8px;height:8px;border-radius:50%;background:' + d.colors[i] + '"></span>'
                + '<span class="flex-1" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' + l + '">' + l + '</span>'
                + '<span style="flex-shrink:0;opacity:0.75;' + valClass + '">' + sign + pct + '%</span>'
                + '</div>';
        }).join('');
    }
    return inst;
};
StockCharts.createChart = function (canvasId, config, existingInstance) {
    const canvas = document.getElementById(canvasId);
    if (!canvas)
        return null;
    if (existingInstance && typeof existingInstance.destroy === 'function') {
        try {
            existingInstance.destroy();
        }
        catch (e) { }
    }
    const ctx = canvas.getContext('2d');
    const inst = new Chart(ctx, config);
    return inst;
};
// Attach to window so templates can use <script src="/js/stock-charts.js"></script>
window.StockCharts = StockCharts;
