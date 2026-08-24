declare global {
	interface Window {
		_timeSeriesChart?: any;
		renderTimeSeriesChart?: (
			chartId: string,
			params?: Record<string, unknown>,
		) => Promise<void>;
	}
}

type Params = Record<string, unknown>;

// Debug: indicate that the timeSeriesChart module was loaded
console.debug("[timeSeriesChart] module loaded");

async function fetchTimeSeries(params: Params = {}): Promise<any[]> {
	const qs = new URLSearchParams();
	for (const [k, v] of Object.entries(params || {})) {
		if (v == null) continue;
		if (Array.isArray(v)) {
			(v as any[]).forEach((x) => qs.append(k, String(x)));
		} else {
			qs.set(k, String(v));
		}
	}

	const url =
		"/stock/api/timeSeries" + (qs.toString() ? "?" + qs.toString() : "");
	const res = await fetch(url, { credentials: "same-origin" });
	if (!res.ok) throw new Error("Time series fetch failed: " + res.status);
	return res.json();
}

// 서버가 집계에 쓴 타임존으로 라벨을 만든다.
//
// 지점의 timestamp 는 그 타임존의 자정을 가리키는 instant 다(예: KST 2026-01-01 -> 2025-12-31T15:00:00Z).
// toLocaleDateString() 을 타임존 없이 부르면 브라우저 로컬로 렌더되어, KST 밖에서는 라벨이 하루씩
// 앞으로 밀린다. 실측: 2025-12-31T15:00:00Z 가 서울에서는 2026-01-01 인데 UTC/뉴욕/런던에서는
// 2025-12-31 로 나와 연도 경계에서 해가 바뀐다.
// 아래 두 함수는 순수 계산이라 브라우저 없이 검증할 수 있다. 라벨 타임존 버그는 이 두 함수에만
// 들어 있으므로, DOM/fetch 를 끌어들이지 않고 여기만 테스트하려고 내보낸다
// (test/timeSeriesChart.test.mjs 가 빌드 산출물을 그대로 불러 쓴다).
export function resolveLabelZone(timeZone?: string): string | undefined {
	if (!timeZone) return undefined;
	try {
		// 알 수 없는 타임존이면 RangeError 가 난다. 그때는 브라우저 로컬(예전 동작)로 돌아간다.
		new Date().toLocaleDateString(undefined, { timeZone });
		return timeZone;
	} catch (e) {
		return undefined;
	}
}

export function toChartData(series: any[], timeZone?: string) {
	const zone = resolveLabelZone(timeZone);
	const options = zone ? { timeZone: zone } : undefined;
	const labels = series.map((p) =>
		new Date(p.timestamp).toLocaleDateString(undefined, options),
	);
	const cumulative = series.map((p) => Number(p.cumulativeRealizedProfit ?? 0));
	const daily = series.map((p) => Number(p.dailyRealizedProfit ?? 0));
	return { labels, cumulative, daily };
}

export async function renderTimeSeriesChart(
	chartId: string,
	params: Params = {},
) {
	try {
		console.debug(
			"[timeSeriesChart] renderTimeSeriesChart called, params:",
			params,
		);
		const series = await fetchTimeSeries(params);
		const canvas = document.getElementById(chartId) as HTMLCanvasElement | null;
		if (!canvas) return;
		const ctx = canvas.getContext("2d");
		if (!ctx) return;

		const data = toChartData(
			series,
			typeof params?.timeZone === "string" ? params.timeZone : undefined,
		);

		if (
			window._timeSeriesChart &&
			typeof window._timeSeriesChart.destroy === "function"
		) {
			window._timeSeriesChart.destroy();
		}

		// Assumes Chart.js is loaded globally as `Chart`
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		window._timeSeriesChart = new (window as any).Chart(ctx, {
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
	} catch (e) {
		// don't throw to avoid breaking the page
		// eslint-disable-next-line no-console
		console.error("renderTimeSeriesChart error", e);
	}
}

// Expose globally for templates
(window as any).renderTimeSeriesChart = renderTimeSeriesChart;

export default renderTimeSeriesChart;
