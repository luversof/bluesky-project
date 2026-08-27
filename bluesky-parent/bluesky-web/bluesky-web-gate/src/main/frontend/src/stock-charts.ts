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
		sellMap: Record<string, number> = {};
	tradeData.forEach((d: any) => {
		if (!d || !d.tradeDate) return;
		const mon = d.tradeDate.slice(0, 7);
		const amt = Number(d.amount) || 0;
		if (d.type === "BUY") buyMap[mon] = (buyMap[mon] || 0) + amt;
		else if (d.type === "SELL") sellMap[mon] = (sellMap[mon] || 0) + amt;
	});
	const allMonths = Object.keys(buyMap).concat(Object.keys(sellMap));
	const months = allMonths.filter((v, i, a) => a.indexOf(v) === i).sort();
	return {
		labels: months,
		buyData: months.map((m) => buyMap[m] || 0),
		sellData: months.map((m) => sellMap[m] || 0),
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
	const gridColor = "rgba(128,128,128,0.1)";
	const inst = new Chart(ctx, {
		type: "bar",
		data: {
			labels: m.labels,
			datasets: [
				{
					label: appMessage("stockLabelBuy", "Buy"),
					data: m.buyData,
					backgroundColor: "rgba(239,68,68,0.7)",
					borderRadius: 3,
					maxBarThickness: 36,
				},
				{
					label: appMessage("stockLabelSell", "Sell"),
					data: m.sellData,
					backgroundColor: "rgba(59,130,246,0.7)",
					borderRadius: 3,
					maxBarThickness: 36,
				},
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
							return ctx.dataset.label + ": \u20a9" + fmtAmt(ctx.parsed.y);
						},
					},
				},
			},
			scales: {
				x: { grid: { color: gridColor }, ticks: { font: { size: 10 } } },
				y: {
					grid: { color: gridColor },
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
				const valClass = isProfitMode
					? raw >= 0
						? "color:rgba(239,68,68,0.9)"
						: "color:rgba(59,130,246,0.9)"
					: "";
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
					'<span style="flex-shrink:0;opacity:0.75;' +
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
				line: { tension: 0 },
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

// Attach to window so templates can use <script src="/js/stock-charts.js"></script>
(window as any).StockCharts = StockCharts;
