// Debug: indicate that the timeSeriesChart module was loaded
console.debug("[timeSeriesChart] module loaded");
async function fetchTimeSeries(params = {}) {
    const qs = new URLSearchParams();
    for (const [k, v] of Object.entries(params || {})) {
        if (v == null)
            continue;
        if (Array.isArray(v)) {
            v.forEach((x) => qs.append(k, String(x)));
        }
        else {
            qs.set(k, String(v));
        }
    }
    const url = "/stock/api/timeSeries" + (qs.toString() ? "?" + qs.toString() : "");
    const res = await fetch(url, { credentials: "same-origin" });
    if (!res.ok)
        throw new Error("Time series fetch failed: " + res.status);
    return res.json();
}
function toChartData(series) {
    const labels = series.map((p) => new Date(p.timestamp).toLocaleDateString());
    const cumulative = series.map((p) => { var _a; return Number((_a = p.cumulativeRealizedProfit) !== null && _a !== void 0 ? _a : 0); });
    const daily = series.map((p) => { var _a; return Number((_a = p.dailyRealizedProfit) !== null && _a !== void 0 ? _a : 0); });
    return { labels, cumulative, daily };
}
export async function renderTimeSeriesChart(chartId, params = {}) {
    try {
        console.debug("[timeSeriesChart] renderTimeSeriesChart called, params:", params);
        const series = await fetchTimeSeries(params);
        const canvas = document.getElementById(chartId);
        if (!canvas)
            return;
        const ctx = canvas.getContext("2d");
        if (!ctx)
            return;
        const data = toChartData(series);
        if (window._timeSeriesChart &&
            typeof window._timeSeriesChart.destroy === "function") {
            window._timeSeriesChart.destroy();
        }
        // Assumes Chart.js is loaded globally as `Chart`
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        window._timeSeriesChart = new window.Chart(ctx, {
            type: "line",
            data: {
                labels: data.labels,
                datasets: [
                    {
                        label: "Cumulative Realized Profit",
                        data: data.cumulative,
                        borderColor: "rgba(54,162,235,1)",
                        backgroundColor: "rgba(54,162,235,0.2)",
                        fill: false,
                        tension: 0.15,
                    },
                    {
                        label: "Daily Realized Profit",
                        data: data.daily,
                        borderColor: "rgba(75,192,192,1)",
                        backgroundColor: "rgba(75,192,192,0.15)",
                        fill: false,
                        tension: 0.15,
                    },
                ],
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { position: "top" },
                },
                scales: {
                    x: { display: true },
                    y: { display: true },
                },
            },
        });
    }
    catch (e) {
        // don't throw to avoid breaking the page
        // eslint-disable-next-line no-console
        console.error("renderTimeSeriesChart error", e);
    }
}
// Expose globally for templates
window.renderTimeSeriesChart = renderTimeSeriesChart;
export default renderTimeSeriesChart;
