// Frontend-managed TypeScript version of stock-charts
declare const Chart: any;

interface StockChartsAPI {
	initMonthlyFromData?: (
		tradeData: any[],
		canvasId?: string,
		existingInstance?: any,
	) => any;
	initDonutFromData?: (
		tradeData: any[],
		opts?: any,
		existingInstance?: any,
	) => any;
	createChart?: (canvasId: string, config: any, existingInstance?: any) => any;
	holdingsChartConfig?: (series: any, texts: any, opts?: any) => any;
	createHoldingsChart?: (
		canvasId: string,
		series: any,
		texts: any,
		opts?: any,
		existingInstance?: any,
	) => any;
	getLocale?: () => string;
	formatNumber?: (value: any) => string;
	formatCurrency?: (value: any) => string;
	formatCompactNumber?: (value: any) => string;
	resizeIfChanged?: (chart: any) => boolean;
}

const StockCharts: StockChartsAPI = {};

const TRADE_COLORS = [
	"rgba(99,102,241,0.8)",
	"rgba(34,197,94,0.8)",
	"rgba(251,191,36,0.8)",
	"rgba(239,68,68,0.8)",
	"rgba(59,130,246,0.8)",
	"rgba(236,72,153,0.8)",
	"rgba(14,165,233,0.8)",
	"rgba(249,115,22,0.8)",
	"rgba(168,85,247,0.8)",
	"rgba(20,184,166,0.8)",
	"rgba(245,158,11,0.8)",
	"rgba(16,185,129,0.8)",
];

const PROFIT_COLORS_POS = [
	"rgba(239,68,68,0.85)",
	"rgba(220,38,38,0.8)",
	"rgba(249,115,22,0.8)",
	"rgba(253,164,175,0.85)",
	"rgba(236,72,153,0.8)",
	"rgba(245,158,11,0.8)",
];
const PROFIT_COLORS_NEG = [
	"rgba(59,130,246,0.85)",
	"rgba(14,165,233,0.8)",
	"rgba(99,102,241,0.8)",
];

/**
 * 차트 툴팁·라벨의 금액. 앱 로케일을 쓴다.
 *
 * <p>예전에는 "ko-KR" 이 박혀 있었다. 같은 파일의 compactNumber 는 이미 앱 로케일을 보고 있었으므로, 축 라벨과 툴팁이 서로 다른 로케일로
 * 찍힐 수 있는 상태였다(ko/en 은 자릿수 구분이 같아 눈에는 안 보인다).
 */
function fmtAmt(v: any): string {
	return Number(v).toLocaleString(resolveLocale());
}

/**
 * 레이아웃의 #app-config 에 실려 오는 화면 문구를 읽는다.
 * 차트 데이터셋 라벨을 코드에 한글로 박아 두면 영어 화면에도 그대로 나온다(실측: 매매내역 차트의 '매수'/'매도').
 */
function appMessage(key: string, fallback: string): string {
	const cfg = document.getElementById("app-config") as HTMLElement | null;
	const value = cfg?.dataset?.[key];
	return value && value.trim() ? value : fallback;
}

function resolveLocale(): string {
	return (
		document.body?.dataset?.locale ||
		document.documentElement?.lang ||
		navigator.language ||
		"ko-KR"
	);
}

function compactNumber(value: any): string {
	const numeric = Number(value) || 0;
	let abs = Math.abs(numeric);
	const sign = numeric < 0 ? "-" : "";
	const locale = resolveLocale();

	// 단위를 고른 뒤 반올림하면 경계에서 한 단위 아래 표기가 남는다
	// (실측: 999,999 -> "1000K"(1M 이어야 한다), 99,999,999 -> "10,000만"(1억 이어야 한다)).
	// 표시 자릿수로 먼저 반올림해 보고, 다음 단위에 닿으면 그 단위로 올린다.
	const promote = (unit: number, next: number, digits: number) => {
		if (abs >= unit && abs < next && Number((abs / unit).toFixed(digits)) >= next / unit) {
			abs = next;
		}
	};

	// 한국어가 아니면 국제 표기(B/M/K). 예전에는 로케일과 무관하게 억/만 을 붙여
	// 영어 화면 차트 축에도 "25억" 이 그대로 나왔다(실측).
	if (!locale.toLowerCase().startsWith("ko")) {
		const trim = (v: number) => {
			const t = v.toFixed(1);
			return t.endsWith(".0") ? t.slice(0, -2) : t;
		};
		promote(1000, 1000000, 1);
		promote(1000000, 1000000000, 1);
		if (abs >= 1000000000) return sign + trim(abs / 1000000000) + "B";
		if (abs >= 1000000) return sign + trim(abs / 1000000) + "M";
		if (abs >= 1000) return sign + trim(abs / 1000) + "K";
		return sign + new Intl.NumberFormat(locale).format(abs);
	}

	promote(10000, 100000000, abs >= 1000000 ? 0 : 1);

	if (abs >= 100000000) {
		const digits = abs >= 1000000000 ? 0 : 1;
		return (
			sign +
			new Intl.NumberFormat(locale, { maximumFractionDigits: digits }).format(
				abs / 100000000,
			) +
			"억"
		);
	}

	if (abs >= 10000) {
		const digits = abs >= 1000000 ? 0 : 1;
		return (
			sign +
			new Intl.NumberFormat(locale, { maximumFractionDigits: digits }).format(
				abs / 10000,
			) +
			"만"
		);
	}

	return sign + new Intl.NumberFormat(locale).format(abs);
}

// htmx 교체 뒤 빈 차트 보정용 resize() 는 크기가 실제로 달라졌을 때만 부른다.
// 실측 2026-09-10(qa/chart-resize-trace.cjs): Chart.js 4.5 retinaScale 은 컨테이너 폭을 0.1 단위로 반올림(570.4)해 정수 canvas.width(570)와
// 비교하므로 소수 폭에선 resize() 마다 '바뀌었다' 고 보고 전부 다시 그렸다(배당 780점 ~100ms, 종목 주가 3,198점 ~256ms @4x CPU).
function chartSizeChanged(chart: any): boolean {
	const canvas = chart?.canvas as HTMLCanvasElement | undefined;
	const parent = canvas?.parentElement;
	if (!canvas || !parent) return false;
	// 백킹 스토어(canvas.width)가 기대 장치 크기와 다르면(교체 직후 기본 300×150 등) 반드시 다시 잡는다.
	const dpr = chart.currentDevicePixelRatio || 1;
	if (canvas.width !== Math.floor((chart.width || 0) * dpr) || canvas.height !== Math.floor((chart.height || 0) * dpr)) return true;
	if (Math.floor(parent.clientWidth) !== Math.floor(chart.width || 0)) return true;
	const keepRatio = chart.options && chart.options.maintainAspectRatio !== false;
	return !keepRatio && Math.floor(parent.clientHeight) !== Math.floor(chart.height || 0);
}
function resizeIfChanged(chart: any): boolean {
	if (!chart || typeof chart.resize !== "function") return false;
	if (!chartSizeChanged(chart)) return false;
	chart.resize();
	return true;
}
StockCharts.resizeIfChanged = resizeIfChanged;
(window as any).__chartResizeInternals = { chartSizeChanged, resizeIfChanged };

StockCharts.getLocale = function () {
	return resolveLocale();
};

StockCharts.formatNumber = function (value: any) {
	return new Intl.NumberFormat(resolveLocale()).format(Number(value) || 0);
};

StockCharts.formatCurrency = function (value: any) {
	const numeric = Math.round(Number(value) || 0);
	return "₩" + StockCharts.formatNumber!(numeric);
};

StockCharts.formatCompactNumber = function (value: any) {
	return compactNumber(value);
};

function buildMonthlyData(tradeData: any[] = []) {
	const buyMap: Record<string, number> = {},
		sellMap: Record<string, number> = {},
		profitMap: Record<string, number> = {};
	let hasSell = false;
	tradeData.forEach((d: any) => {
		if (!d || !d.tradeDate) return;
		const mon = d.tradeDate.slice(0, 7);
		const amt = Number(d.amount) || 0;
		if (d.type === "BUY") buyMap[mon] = (buyMap[mon] || 0) + amt;
		else if (d.type === "SELL") {
			sellMap[mon] = (sellMap[mon] || 0) + amt;
			// 실현손익은 매도에만 붙는다(요약 카드·월별 표와 같은 규칙).
			profitMap[mon] = (profitMap[mon] || 0) + (Number(d.profit) || 0);
			hasSell = true;
		}
	});
	const allMonths = Object.keys(buyMap).concat(Object.keys(sellMap));
	const months = allMonths.filter((v, i, a) => a.indexOf(v) === i).sort();
	return {
		labels: months,
		buyData: months.map((m) => buyMap[m] || 0),
		sellData: months.map((m) => sellMap[m] || 0),
		// 판 달에만 값이 있다. 안 판 달은 null 로 두어 선이 0 으로 꺼지지 않게 한다.
		profitData: months.map((m) => (m in profitMap ? profitMap[m] : null)),
		hasSell,
	};
}

function buildDonutData(
	tradeData: any[] = [],
	metric = "profit",
	groupBy = "stock",
) {
	const map: Record<string, number> = {};
	tradeData.forEach((d) => {
		const key = groupBy === "stock" ? d.stockItem : d.account;
		if (!key) return;
		if (metric === "profit") {
			if (d.type !== "SELL") return;
			map[key] = (map[key] || 0) + (Number(d.profit) || 0);
		} else {
			if (d.type !== "BUY") return;
			map[key] = (map[key] || 0) + (Number(d.amount) || 0);
		}
	});
	if (metric === "profit") {
		const entries = Object.entries(map);
		const pos = entries.filter((e) => e[1] >= 0).sort((a, b) => b[1] - a[1]);
		const neg = entries.filter((e) => e[1] < 0).sort((a, b) => a[1] - b[1]);
		const sorted = pos.concat(neg);
		const posLen = pos.length;
		return {
			labels: sorted.map((e) => e[0]),
			data: sorted.map((e) => Math.abs(e[1])),
			rawData: sorted.map((e) => e[1]),
			colors: sorted.map((e, i) =>
				e[1] >= 0
					? PROFIT_COLORS_POS[i % PROFIT_COLORS_POS.length]
					: PROFIT_COLORS_NEG[(i - posLen) % PROFIT_COLORS_NEG.length],
			),
		};
	} else {
		const sorted = Object.entries(map).sort((a, b) => b[1] - a[1]);
		return {
			labels: sorted.map((e) => e[0]),
			data: sorted.map((e) => e[1]),
			rawData: sorted.map((e) => e[1]),
			colors: TRADE_COLORS.slice(0, sorted.length),
		};
	}
}

function makeDonutTooltipHandler(
	rawData: any[],
	isProfitMode: boolean,
	tooltipId = "tradeDonutTooltipEl",
) {
	return function (context: any) {
		const tooltip = context.tooltip;
		let tooltipEl = document.getElementById(tooltipId) as HTMLElement | null;
		if (!tooltipEl) {
			tooltipEl = document.createElement("div");
			tooltipEl.id = tooltipId;
			tooltipEl.style.cssText =
				"position:fixed;z-index:9999;background:rgba(30,30,30,0.92);color:#fff;padding:6px 10px;border-radius:6px;font-size:12px;pointer-events:none;white-space:nowrap;transition:opacity .1s;";
			document.body.appendChild(tooltipEl);
		}
		if (tooltip.opacity === 0) {
			tooltipEl.style.opacity = "0";
			return;
		}
		const idx =
			tooltip.dataPoints && tooltip.dataPoints[0]
				? tooltip.dataPoints[0].dataIndex
				: 0;
		const label = tooltip.title && tooltip.title[0] ? tooltip.title[0] : "";
		const raw = rawData[idx] !== undefined ? rawData[idx] : 0;
		const sign = isProfitMode ? (raw >= 0 ? "\u25b2 " : "\u25bc ") : "";
		const valText = sign + "\u20a9" + fmtAmt(Math.abs(raw));
		const color = context.chart.data.datasets[0].backgroundColor[idx] || "#999";
		tooltipEl.innerHTML =
			'<div style="font-weight:bold;margin-bottom:3px">' +
			label +
			"</div>" +
			'<div style="display:flex;align-items:center;gap:5px">' +
			'<span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:' +
			color +
			'"></span>' +
			"<span>" +
			label +
			": " +
			valText +
			"</span>" +
			"</div>";
		const rect = context.chart.canvas.getBoundingClientRect();
		const x = rect.left + tooltip.caretX;
		const y = rect.top + tooltip.caretY;
		tooltipEl.style.opacity = "1";
		tooltipEl.style.left = "0px";
		tooltipEl.style.top = "0px";
		const tw = tooltipEl.offsetWidth;
		const th = tooltipEl.offsetHeight;
		let finalX = x + 12;
		let finalY = y - th - 8;
		if (finalX + tw > window.innerWidth - 8) finalX = x - tw - 12;
		if (finalY < 8) finalY = y + 16;
		tooltipEl.style.left = finalX + "px";
		tooltipEl.style.top = finalY + "px";
	};
}

StockCharts.initMonthlyFromData = function (
	tradeData: any[],
	canvasId = "tradeMonthlyChart",
	existingInstance?: any,
) {
	const m = buildMonthlyData(tradeData);
	const ctx = document.getElementById(canvasId) as HTMLCanvasElement | null;
	if (!ctx) return null;
	if (existingInstance)
		try {
			existingInstance.destroy();
		} catch (e) {}
	const gridColor = "rgba(128,128,128,0.14)";
	// 격자가 실선 두 방향으로 깔리면 자료보다 격자가 먼저 보인다 - 세로선은 지우고 가로선만 점선으로 남긴다.
	const gridX = { display: false } as any;
	const gridY = { color: gridColor, drawTicks: false, borderDash: [4, 4] } as any;
	const inst = new Chart(ctx, {
		type: "bar",
		data: {
			labels: m.labels,
			datasets: [
				{
					label: appMessage("stockLabelBuy", "Buy"),
					data: m.buyData,
					backgroundColor: "rgba(239,68,68,0.7)",
					borderRadius: 6,
					maxBarThickness: 36,
				},
				{
					label: appMessage("stockLabelSell", "Sell"),
					data: m.sellData,
					backgroundColor: "rgba(59,130,246,0.7)",
					borderRadius: 6,
					maxBarThickness: 36,
				},
				// 실현손익 선(오른쪽 축). 월별 표에만 있던 값을 차트에도 얹어, 표와 차트가 같은 숫자를 두 번 말하는
				// 대신 차트는 '언제 팔아서 얼마 남겼나' 의 흐름을 답한다(2026-09-08). 매도가 하나도 없으면 선도 축도 없다.
				...(m.hasSell
					? [
							{
								type: "line",
								label: appMessage("stockLabelRealizedProfit", "Realized profit"),
								data: m.profitData,
								yAxisID: "y1",
								order: 0,
								borderColor: "rgba(189,44,56,0.9)",
								backgroundColor: "rgba(189,44,56,0.9)",
								borderWidth: 2.5,
								pointRadius: 0,
								pointHoverRadius: 5,
								spanGaps: true,
								tension: 0.35,
								fill: false,
							},
						]
					: []),
			],
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			interaction: { mode: "index", intersect: false },
			plugins: {
				legend: {
					position: "top",
					labels: { font: { size: 11 }, boxWidth: 12 },
				},
				tooltip: {
					callbacks: {
						label: (ctx: any) => {
							if (ctx.parsed.y === null || ctx.parsed.y === undefined) return null;
							// 실현손익만 부호가 뜻이다.
							if (ctx.dataset.type === "line") {
								const v = Number(ctx.parsed.y) || 0;
								return ctx.dataset.label + ": " + (v >= 0 ? "+" : "-") + "\u20a9" + fmtAmt(Math.abs(v));
							}
							return ctx.dataset.label + ": \u20a9" + fmtAmt(ctx.parsed.y);
						},
					},
				},
			},
			scales: {
				x: { grid: gridX, ticks: { font: { size: 10 } } },
				y: {
					grid: gridY,
					ticks: {
						font: { size: 10 },
						callback: (v: any) => compactNumber(v),
					},
				},
				y1: {
					display: m.hasSell,
					position: "right",
					grid: { drawOnChartArea: false },
					ticks: {
						font: { size: 10 },
						callback: (v: any) => compactNumber(v),
					},
				},
			},
		},
	});
	return inst;
};

StockCharts.initDonutFromData = function (
	tradeData: any[],
	opts: any = {},
	existingInstance?: any,
) {
	const metric = opts.metric || "profit";
	const groupBy = opts.groupBy || "stock";
	const canvas = document.getElementById(
		opts.canvasId || "tradeDonutChart",
	) as HTMLCanvasElement | null;
	if (!canvas) return null;
	const d = buildDonutData(tradeData, metric, groupBy);
	if (existingInstance)
		try {
			existingInstance.destroy();
		} catch (e) {}
	const oldTip = document.getElementById(
		opts.tooltipId || "tradeDonutTooltipEl",
	);
	if (oldTip) oldTip.style.opacity = "0";
	const isProfitMode = metric === "profit";
	const legendEl = opts.legendId
		? document.getElementById(opts.legendId)
		: null;
	if (d.labels.length === 0) {
		canvas.style.display = "none";
		if (legendEl) {
			// 레이아웃이 이미 data-stock-chart-message-* 로 실어 보내는 문구다.
			// 예전에는 여기서 한글을 직접 이어 붙여, 영어 화면에도 한글이 그대로 나갔다.
			const emptyText = isProfitMode
				? appMessage("stockChartMessageNoSellData", "No sell trades in this period.")
				: appMessage("stockChartMessageNoBuyData", "No buy trades in this period.");
			const holder = document.createElement("div");
			holder.className = "text-xs opacity-40 pt-4 text-center";
			holder.textContent = emptyText;
			legendEl.replaceChildren(holder);
		}
		return null;
	}
	canvas.style.display = "";
	const inst = new Chart(canvas, {
		type: "doughnut",
		data: {
			labels: d.labels,
			datasets: [{ data: d.data, backgroundColor: d.colors, borderWidth: 1 }],
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			cutout: "65%",
			plugins: {
				legend: { display: false },
				tooltip: {
					enabled: false,
					external: makeDonutTooltipHandler(
						d.rawData,
						isProfitMode,
						opts.tooltipId || "tradeDonutTooltipEl",
					),
				},
			},
		},
	});
	const titleEl = opts.titleId ? document.getElementById(opts.titleId) : null;
	if (titleEl)
		// 레이아웃이 data-stock-chart-title-* 로 실어 보내는 제목을 쓴다(영어 화면 대응).
		titleEl.textContent = isProfitMode
			? appMessage("stockChartTitleProfitContribution", "Profit Contribution")
			: appMessage("stockChartTitleBuyConcentration", "Buy Concentration");
	if (legendEl) {
		const total = d.data.reduce((a: number, b: number) => a + b, 0);
		legendEl.innerHTML = d.labels
			.map((l: string, i: number) => {
				const pct = total > 0 ? ((d.data[i] / total) * 100).toFixed(1) : "0.0";
				const raw = d.rawData[i];
				const sign = isProfitMode ? (raw >= 0 ? "\u25b2 " : "\u25bc ") : "";
				// 손익 부호 색은 표와 같은 테마 토큰(text-error/text-info)으로. 인라인 rgba(239,68,68,.9)+opacity .75 는
				// 라이트에서 2.52:1 이었다(axe 실측 2026-09-09, 매매 화면 '실현손익' 도넛 범례 7개 전부).
				const valClass = isProfitMode
					? raw >= 0
						? "text-error"
						: "text-info"
					: "opacity-75";
				return (
					'<div class="flex items-center gap-1 mb-0.5">' +
					'<span style="flex-shrink:0;display:inline-block;width:8px;height:8px;border-radius:50%;background:' +
					d.colors[i] +
					'"></span>' +
					'<span class="flex-1" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' +
					l +
					'">' +
					l +
					"</span>" +
					'<span class="shrink-0 ' +
					valClass +
					'">' +
					sign +
					pct +
					"%</span>" +
					"</div>"
				);
			})
			.join("");
	}
	return inst;
};

StockCharts.createChart = function (
	canvasId: string,
	config: any,
	existingInstance?: any,
) {
	const canvas = document.getElementById(canvasId) as HTMLCanvasElement | null;
	if (!canvas) return null;
	if (existingInstance && typeof existingInstance.destroy === "function") {
		try {
			existingInstance.destroy();
		} catch (e) {}
	}
	const ctx = (canvas as HTMLCanvasElement).getContext("2d") as any;
	// 막대차트: 데이터가 적을 때 막대가 카테고리 폭을 가득 채워 과도하게 두꺼워지는 것을 방지.
	// 명시값이 없으면 두께 상한을 기본 적용한다(개별 데이터셋이 지정했으면 존중).
	try {
		const datasets = config && config.data && config.data.datasets;
		if (datasets && datasets.length) {
			const chartIsBar = config.type === "bar";
			datasets.forEach((d: any) => {
				if ((chartIsBar || d.type === "bar") && d.maxBarThickness == null) {
					d.maxBarThickness = 36;
				}
			});
		}
	} catch (e) {}
	const inst = new Chart(ctx, config);
	return inst;
};

// 보유 평가액/원가 추이 공용 차트 (자산 성장 메인 차트와 동일한 표현):
//  - 원금(cost): 회색 점선
//  - 평가액(value): 원금 기준 위=수익(빨강)/아래=손실(파랑) 영역으로 채움
//  - 매수(▲)/매도(▼) 마커, 평가손익 툴팁, 렌더 애니메이션
// 자산 성장과 종목/계좌 상세가 같은 모양이 되도록 한 곳에서 구성한다.
StockCharts.holdingsChartConfig = function (series: any, texts: any, opts?: any) {
	const labels: any[] = (series && series.labels) || [];
	const valueData: any[] = (series && series.value) || [];
	const costData: any[] = (series && series.cost) || [];
	const buyCountData: any[] = (series && series.buyCount) || [];
	const dailyRealizedData: any[] = (series && series.dailyRealized) || [];
	const showMarkers = !opts || opts.showMarkers !== false;
	const animate = !!(opts && opts.animate);
	const t = texts || {};
	const o = opts || {};
	// maxLabel/minLabel 이 주어진 차트만 범위 내 최고/최저 주석을 그린다
	const showExtremes = !!(t.maxLabel && t.minLabel);

	const tradeMarkersPlugin = {
		id: "holdingsTradeMarkers",
		afterDatasetsDraw: function (chart: any) {
			if (!showMarkers) {
				chart.$drawnMarkers = [];
				return;
			}
			const ctx = chart.ctx;
			const xAxis = chart.scales["x"];
			const yAxis = chart.scales["y"];
			if (!xAxis || !yAxis) return;
			const left = chart.chartArea.left;
			const right = chart.chartArea.right;
			const arrow = 7;
			const drawn: any[] = [];
			function drawArrow(cx: number, cy: number, up: boolean, color: string) {
				ctx.save();
				ctx.fillStyle = color;
				ctx.beginPath();
				if (up) {
					ctx.moveTo(cx, cy);
					ctx.lineTo(cx + arrow, cy + arrow * 1.4);
					ctx.lineTo(cx - arrow, cy + arrow * 1.4);
				} else {
					ctx.moveTo(cx, cy);
					ctx.lineTo(cx + arrow, cy - arrow * 1.4);
					ctx.lineTo(cx - arrow, cy - arrow * 1.4);
				}
				ctx.closePath();
				ctx.fill();
				ctx.restore();
			}
			for (let i = 0; i < labels.length; i++) {
				const cost = parseFloat(costData[i]);
				if (isNaN(cost)) continue;
				const px = xAxis.getPixelForValue(i);
				if (px < left || px > right) continue;
				const py = yAxis.getPixelForValue(cost);
				if (Number(buyCountData[i]) > 0) {
					drawArrow(px, py + 2, true, "rgba(255, 99, 132, 0.9)");
					drawn.push({ px: px, py: py + 2 + arrow * 0.7, date: labels[i] });
				}
				if (parseFloat(dailyRealizedData[i]) > 0) {
					drawArrow(px, py - 2, false, "rgba(54, 162, 235, 0.9)");
					drawn.push({ px: px, py: py - 2 - arrow * 0.7, date: labels[i] });
				}
			}
			chart.$drawnMarkers = drawn;
		},
	};

	// 범위 내 평가액 최고/최저 지점에 값 + 현재값 대비 % 주석 (네이버 주식 차트 스타일)
	const rangeExtremesPlugin = {
		id: "holdingsRangeExtremes",
		afterDatasetsDraw: function (chart: any) {
			const xAxis = chart.scales["x"];
			const yAxis = chart.scales["y"];
			if (!xAxis || !yAxis) return;
			let maxIdx = -1;
			let minIdx = -1;
			let maxV = -Infinity;
			let minV = Infinity;
			let lastV = NaN;
			for (let i = 0; i < valueData.length; i++) {
				const v = parseFloat(valueData[i]);
				if (isNaN(v)) continue;
				// 마지막 값(현재)은 0 이어도 '지금' 이므로 세되, 고점/저점 후보에서는 평가액 0 인 날을 뺀다.
				// 보유가 하나도 없던 날이라 기준점이 못 된다 - 예전에는 '전체' 기간에서 늘 "최저 0원" 이
				// 그려졌다(실측 2026-08-27: 6,170 일 중 1,772 일이 평가액 0). 서버 요약도 같은 규칙이라
				// 카드와 차트가 같은 지점을 가리킨다.
				lastV = v;
				if (v <= 0) continue;
				if (v > maxV) {
					maxV = v;
					maxIdx = i;
				}
				if (v < minV) {
					minV = v;
					minIdx = i;
				}
			}
			if (maxIdx < 0 || minIdx < 0 || maxIdx === minIdx || isNaN(lastV)) return;
			const ctx = chart.ctx;
			const area = chart.chartArea;
			let baseColor = "#6b7280";
			try {
				const cv = getComputedStyle(document.documentElement)
					.getPropertyValue("--color-base-content")
					.trim();
				if (cv) baseColor = cv;
			} catch (e) {}
			function pctText(extreme: number) {
				if (extreme === 0) return "";
				const pct = ((lastV - extreme) / Math.abs(extreme)) * 100;
				return " (" + (pct >= 0 ? "+" : "") + pct.toFixed(2) + "%)";
			}
			function drawExtreme(idx: number, v: number, isMax: boolean, label: string) {
				const px = xAxis.getPixelForValue(idx);
				if (px < area.left || px > area.right) return;
				const py = yAxis.getPixelForValue(v);
				const text =
					(isMax ? "▼ " : "▲ ") +
					label +
					" " +
					StockCharts.formatCurrency!(v) +
					pctText(v);
				ctx.save();
				ctx.font = "11px sans-serif";
				const w = ctx.measureText(text).width;
				let tx = px - w / 2;
				if (tx < area.left + 2) tx = area.left + 2;
				if (tx + w > area.right - 2) tx = area.right - 2 - w;
				let ty = isMax ? py - 8 : py + 16;
				if (ty < area.top + 12) ty = py + 16;
				if (ty > area.bottom - 4) ty = py - 8;
				ctx.fillStyle = baseColor;
				ctx.globalAlpha = 0.95;
				ctx.beginPath();
				ctx.arc(px, py, 2.5, 0, Math.PI * 2);
				ctx.fill();
				ctx.fillText(text, tx, ty);
				ctx.restore();
			}
			drawExtreme(maxIdx, maxV, true, t.maxLabel);
			drawExtreme(minIdx, minV, false, t.minLabel);
		},
	};

	const inlinePlugins: any[] = [];
	if (showMarkers) inlinePlugins.push(tradeMarkersPlugin);
	if (showExtremes) inlinePlugins.push(rangeExtremesPlugin);

	return {
		type: "line",
		plugins: inlinePlugins,
		data: {
			labels: labels,
			datasets: [
				{
					type: "line",
					label: t.costLabel || "",
					data: costData,
					borderColor: "rgba(156, 163, 175, 1)",
					borderWidth: 2,
					borderDash: [5, 5],
					fill: false,
					order: 0,
				},
				{
					type: "line",
					label: t.valueLabel || "",
					data: valueData,
					borderColor: "rgba(75, 192, 192, 1)",
					borderWidth: 2,
					fill: {
						target: "-1",
						above: "rgba(255, 99, 132, 0.25)",
						below: "rgba(54, 162, 235, 0.25)",
					},
					order: 1,
				},
			],
		},
		options: {
			animation: animate ? { duration: 600 } : false,
			normalized: true,
			elements: {
				line: { tension: 0.3 },
				point: { radius: 0, hitRadius: 10, hoverRadius: 4 },
			},
			layout: { padding: { top: 20, bottom: 5 } },
			responsive: true,
			maintainAspectRatio: false,
			interaction: { mode: "index", intersect: false },
			onHover: o.onHover,
			onClick: o.onClick,
			plugins: {
				legend: {
					position: "bottom",
					labels: {
						sort: function (a: any, b: any) {
							return a.datasetIndex - b.datasetIndex;
						},
					},
				},
				tooltip: {
					itemSort: function (a: any, b: any) {
						const av = a && a.parsed ? parseFloat(a.parsed.y) : 0;
						const bv = b && b.parsed ? parseFloat(b.parsed.y) : 0;
						return av === bv ? a.datasetIndex - b.datasetIndex : bv - av;
					},
					callbacks: {
						label: function (context: any) {
							let label = context.dataset.label || "";
							if (label) label += ": ";
							if (context.parsed.y !== null)
								label += StockCharts.formatCurrency!(context.parsed.y);
							return label;
						},
						afterBody: function (items: any) {
							const it = items && items[0];
							if (!it) return [];
							const idx = it.dataIndex;
							const v = parseFloat(valueData[idx]);
							const c = parseFloat(costData[idx]);
							const lines: string[] = [];
							if (!isNaN(v) && !isNaN(c)) {
								const diff = v - c;
								const pct = c !== 0 ? (diff / c) * 100 : 0;
								lines.push("─────────────────");
								lines.push(
									(t.profitLabel || "") +
										": " +
										(diff >= 0 ? "+" : "-") +
										StockCharts.formatCurrency!(Math.abs(diff)) +
										" (" +
										(pct >= 0 ? "+" : "-") +
										Math.abs(pct).toFixed(2) +
										"%)",
								);
							}
							if (typeof o.tooltipAfterBody === "function") {
								const extra = o.tooltipAfterBody(idx) || [];
								for (let k = 0; k < extra.length; k++) lines.push(extra[k]);
							}
							return lines;
						},
					},
				},
			},
			scales: {
				x: { display: true, ticks: { maxRotation: 45, minRotation: 45 } },
				y: {
					display: true,
					position: "left",
					beginAtZero: false,
					title: { display: !!t.axisLabel, text: t.axisLabel || "" },
					ticks: {
						callback: function (value: any) {
							return compactNumber(value);
						},
					},
				},
			},
		},
	};
};

StockCharts.createHoldingsChart = function (
	canvasId: string,
	series: any,
	texts: any,
	opts?: any,
	existingInstance?: any,
) {
	return StockCharts.createChart!(
		canvasId,
		StockCharts.holdingsChartConfig!(series, texts, opts),
		existingInstance,
	);
};

// 축 눈금·범례 글자색을 테마 본문색에 맞춘다. 실측 2026-09-09(qa/chart-tick-contrast.cjs): Chart.js 기본 #666 은 다크 카드 배경
// rgb(15,22,35) 위에서 3.15:1 로 11px 글자 기준(4.5:1)에 못 미쳤다(라이트 5.74:1). --color-base-content 는 oklch 라 1px 캔버스로 rgb 를
// 얻고 alpha .75 로 본문보다 한 단계 옅게 쓴다. 테마 토글(html[data-theme]) 때 살아 있는 차트도 다시 그린다.
function resolveCssColor(css: string): [number, number, number] | null {
	try {
		const cv = document.createElement("canvas") as HTMLCanvasElement;
		cv.width = cv.height = 1;
		const x = cv.getContext && (cv.getContext("2d") as any);
		if (!x) return null;
		x.fillStyle = "#010203";
		x.fillStyle = css;
		if (x.fillStyle === "#010203" && css.trim().toLowerCase() !== "#010203") return null;
		x.fillRect(0, 0, 1, 1);
		const d = x.getImageData(0, 0, 1, 1).data;
		return [d[0], d[1], d[2]];
	} catch (e) {
		return null;
	}
}
function chartTextColor(): string {
	let raw = "";
	try {
		raw = getComputedStyle(document.documentElement).getPropertyValue("--color-base-content").trim();
	} catch (e) {}
	if (!raw) return "#666";
	const rgb = resolveCssColor(raw);
	return rgb ? "rgba(" + rgb.join(",") + ",0.75)" : raw;
}
function applyChartTheme(chartLib: any = (globalThis as any).Chart): string | null {
	if (!chartLib || !chartLib.defaults) return null;
	const c = chartTextColor();
	chartLib.defaults.color = c;
	const instances: any[] = chartLib.instances ? Object.values(chartLib.instances) : [];
	// 살아 있는 차트는 defaults 만 바꿔서는 다시 칠해지지 않는다(실측: 테마 토글 뒤 scale.options.ticks.color 가 옛 값 유지) -
	// 눈금·축 제목·범례 자리에 직접 써 넣고 다시 그린다.
	for (const chart of instances) {
		try {
			chart.options.color = c;
			for (const scale of Object.values(chart.options.scales || {}) as any[]) {
				if (scale && scale.ticks) scale.ticks.color = c;
				if (scale && scale.title) scale.title.color = c;
			}
			const labels = chart.options.plugins && chart.options.plugins.legend && chart.options.plugins.legend.labels;
			if (labels) labels.color = c;
			chart.update("none");
		} catch (e) {}
	}
	return c;
}
applyChartTheme();
// 차트 텍스트 대안 플러그인(common.ts 정의). 이 파일은 chart.umd 뒤에 로드되므로 여기서 한 번 등록하면 이후 만드는 차트 전부에 적용된다.
try {
	const summaryPlugin = (globalThis as any).__chartSummaryInternals?.chartSummaryPlugin;
	if (summaryPlugin && (globalThis as any).Chart?.register) (globalThis as any).Chart.register(summaryPlugin);
} catch (e) {}
try {
	if (typeof MutationObserver !== "undefined" && document.documentElement) {
		new MutationObserver(() => applyChartTheme()).observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
	}
} catch (e) {}
(window as any).__chartThemeInternals = { resolveCssColor, chartTextColor, applyChartTheme };
// Attach to window so templates can use <script src="/js/stock-charts.js"></script>
(window as any).StockCharts = StockCharts;
