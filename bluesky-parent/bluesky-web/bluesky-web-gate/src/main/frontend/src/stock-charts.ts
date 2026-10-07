// Frontend-managed TypeScript version of stock-charts
declare const Chart: any;

interface StockChartsAPI {
	/** Chart.js 전역 애니메이션 기본값을 다시 적용한다(검증·재초기화용). */
	applyChartAnimationDefaults?: (chartLib?: any) => number | null;
	applyChartLocaleDefault?: (chartLib?: any) => string | null;
	/** 첫 등장 애니메이션 플러그인(검증용). */
	entryAnimationPlugin?: any;
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
	balancedPercents?: (values: number[], digits?: number) => string[];
	renderChartEmptyNote?: (
		canvasId: string,
		isEmpty: boolean,
		message: string,
	) => boolean;
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
	extremeGapText?: (lastV: number, extreme: number) => string;
	candleBuckets?: (series: any, maxCandles?: number) => any;
	splitTooltipLine?: (line: string) => string[];
	candleYRange?: (candles: any[]) => any;
	candleChartConfig?: (series: any, texts: any) => any;
	createCandleChart?: (canvasId: string, series: any, texts: any, existingInstance?: any) => any;
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
	// 규칙은 common.js 의 appLocale 한 곳에 있다(먼저 로드된다). 없으면 같은 차례로 찾는다.
	const shared = (globalThis as any).appLocale;
	if (typeof shared === "function") {
		return shared();
	}
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

/**
 * 조각 백분율을 함께 반올림해 표시값의 합이 100.0 이 되게 한다(최대잔여법).
 *
 * 조각마다 따로 toFixed 하면 합이 어긋난다 - 실측 2026-09-11: 매매 화면 도넛 범례 43개의 합이 99.9% 였다
 * (배당 화면 18개는 우연히 100.0). 표 쪽은 StockFormatUtil.balancedPct 가 같은 규칙을 쓴다.
 * 값이 비었거나 합이 0 이면 "0.0" 을 돌려준다.
 */
function balancedPercents(values: number[], digits?: number): string[] {
	const scale = typeof digits === "number" ? digits : 1;
	const factor = Math.pow(10, scale);
	const list = Array.isArray(values) ? values.map((v) => (Number.isFinite(Number(v)) ? Number(v) : 0)) : [];
	const total = list.reduce((a, b) => a + b, 0);
	if (!list.length || total <= 0) return list.map(() => (0).toFixed(scale));
	const exact = list.map((v) => (v / total) * 100 * factor);
	const floors = exact.map((v) => Math.floor(v));
	let spare = Math.round(100 * factor - floors.reduce((a, b) => a + b, 0));
	const order = exact
		.map((v, i) => ({ i, rem: v - Math.floor(v) }))
		.sort((a, b) => b.rem - a.rem);
	const bumped = floors.slice();
	for (let k = 0; k < order.length && spare > 0; k++, spare--) bumped[order[k].i] += 1;
	return bumped.map((v) => (v / factor).toFixed(scale));
}
StockCharts.balancedPercents = balancedPercents;

StockCharts.formatNumber = function (value: any) {
	return new Intl.NumberFormat(resolveLocale()).format(Number(value) || 0);
};

StockCharts.formatCurrency = function (value: any) {
	const numeric = Math.round(Number(value) || 0);
	return "₩" + StockCharts.formatNumber!(numeric);
};

/**
 * 차트의 최고 · 최저 표시 옆 "지금 대비" 글자. 1,000% 이상이면 배수로 적는다 - 위 요약 카드(assetGrowthPeriodReturnSummary)와
 * 같은 규칙이다. 예전에는 차트만 "+418794.23%" 로 적어 같은 화면의 카드("x4,189")와 읽기가 갈렸다(실측 2026-10-01 '전체').
 */
function extremeGapText(lastV: number, extreme: number): string {
	if (!extreme || isNaN(lastV)) return "";
	const raw = ((lastV - extreme) / Math.abs(extreme)) * 100;
	if (Math.abs(raw) >= 1000) {
		return "x" + StockCharts.formatNumber!(Math.round(lastV / extreme));
	}
	// 표시 자릿수(2)로 먼저 반올림한다 - 0 에는 방향이 없고 -0.004 는 '0.00%' 다.
	const pct = Number(raw.toFixed(2));
	return (pct > 0 ? "+" : "") + pct.toFixed(2) + "%";
}
StockCharts.extremeGapText = extremeGapText;

StockCharts.formatCompactNumber = function (value: any) {
	return compactNumber(value);
};

// 거래가 없던 달을 빼면 시간축이 거짓말을 한다.
//
// Chart.js 는 labels 문자열 배열을 카테고리 축으로 그리므로 점 간격이 날짜와 무관하게 
// 모두 같다. 실측 2026-09-12: "월별 매매 금액" 은 2009-10 ~ 2026-09 의 204 개월 중 거래가 있던 
// 61 개월만 그려, 143 개월(70%)이 통째로 빠졌다. 그 탓에 2010-03 → 2014-11 의 4.7 년 공백이 
// 2018-03 → 2018-04 의 한 달과 같은 폭을 차지해, 활동이 17 년 내내 고르게 있었던 것처럼 보였다.
//
// 형제 화면인 배당 내역의 월별 막대는 이미 빈 달을 채운다(실측: 78 개월 전부 연속). 같은 모양의 
// 차트가 화면마다 다른 규칙을 쓰지 않게 여기도 채운다.
// 이 개수를 넘으면 해 단위로 묶는다. 60 개월(5년)까지는 월 막대가 읽힌다(실측 1900px 기준 칸 14px).
const MONTHLY_BAR_LIMIT = 60;

function fillMonthGaps(sortedMonths: string[]): string[] {
	if (sortedMonths.length < 2) return sortedMonths;
	const index = (m: string) => {
		const year = Number(m.slice(0, 4));
		const month = Number(m.slice(5, 7));
		return Number.isFinite(year) && Number.isFinite(month) && month >= 1 && month <= 12
			? year * 12 + (month - 1)
			: null;
	};
	const first = index(sortedMonths[0]);
	const last = index(sortedMonths[sortedMonths.length - 1]);
	// 달 모양이 아닌 라벨이 섮이면 손대지 않는다 - 짐작하다 없는 달을 만드느니 그대로 두는 편이 낫다.
	if (first === null || last === null || last < first) return sortedMonths;
	const filled: string[] = [];
	for (let i = first; i <= last; i++) {
		const year = Math.floor(i / 12);
		const month = (i % 12) + 1;
		filled.push(year + "-" + (month < 10 ? "0" + month : String(month)));
	}
	return filled;
}

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
	const traded = allMonths.filter((v, i, a) => a.indexOf(v) === i).sort();
	const months = fillMonthGaps(traded);
	// 빈 달을 채우고 나면 전체 기간에서는 칸이 너무 많아진다 - 실측 2026-09-12: 204 개월을
	// 그리면 막대 하나가 1900px 에서 1.5px, 375px 에서 0.38px 로 사실상 안 보인다.
	// 길면 해 단위로 묶는다 - 칸이 줄어드는 것이지 자료가 사라지는 것은 아니며, 빈 해도 그대로 남아
	// 시간축은 계속 정직하다. 월 단위가 보고 싶으면 기간을 좁히면 된다.
	const bucket: "month" | "year" = months.length > MONTHLY_BAR_LIMIT ? "year" : "month";
	if (bucket === "month") {
		return {
			labels: months,
			buyData: months.map((m) => buyMap[m] || 0),
			sellData: months.map((m) => sellMap[m] || 0),
			// 판 달에만 값이 있다. 안 판 달은 null 로 두어 선이 0 으로 꺼지지 않게 한다.
			profitData: months.map((m) => (m in profitMap ? profitMap[m] : null)),
			hasSell,
			bucket,
		};
	}
	const years: string[] = [];
	for (const m of months) {
		const y = m.slice(0, 4);
		if (years[years.length - 1] !== y) years.push(y);
	}
	const sumBy = (map: Record<string, number>, year: string) =>
		months
			.filter((m) => m.slice(0, 4) === year)
			.reduce((acc, m) => acc + (map[m] || 0), 0);
	return {
		labels: years,
		buyData: years.map((y) => sumBy(buyMap, y)),
		sellData: years.map((y) => sumBy(sellMap, y)),
		// 달 단위와 같은 규칙 - 한 번도 안 판 해는 null 로 두어 선을 끊는다.
		profitData: years.map((y) =>
			months.some((m) => m.slice(0, 4) === y && m in profitMap)
				? sumBy(profitMap, y)
				: null,
		),
		hasSell,
		bucket,
	};
}

(window as any).__monthlyChartInternals = {
	fillMonthGaps,
	buildMonthlyData,
	buildRealizedProfitDataset,
	snapLine,
};

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

// 자료가 한 점도 없으면 캔버스만 덩그러니 남는다 - 도넛은 이미 캔버스를 감추고 안내를 띄우는데
// 월별 막대(배당·매매)는 그 처리가 없었다. 실측 2026-09-11(2013-01~03 구간): 배당 화면은 도넛 자리에
// "해당 기간에 배당 내역이 없습니다" 가 뜨는데 바로 옆 "월별 배당금" 은 280px 빈 캔버스였고,
// 매매 화면도 같은 모양(220px)이었다. 같은 화면 안에서 빈 상태 규칙이 카드마다 달랐다.
// 자료가 생기면 안내를 걷어내고 캔버스를 되돌린다(조각이 htmx 로 다시 그려질 수 있다).
function renderChartEmptyNote(
	canvasId: string,
	isEmpty: boolean,
	message: string,
): boolean {
	const canvas = document.getElementById(canvasId) as HTMLElement | null;
	if (!canvas) return false;
	const noteId = canvasId + "EmptyNote";
	const existing = document.getElementById(noteId);
	if (!isEmpty) {
		if (existing && existing.parentElement)
			existing.parentElement.removeChild(existing);
		canvas.style.display = "";
		return false;
	}
	canvas.style.display = "none";
	let note = existing;
	if (!note) {
		note = document.createElement("div");
		note.id = noteId;
		note.className =
			"text-xs text-base-content/70 h-full flex items-center justify-center text-center";
		(canvas.parentElement || document.body).appendChild(note);
	}
	note.textContent = message;
	return true;
}
StockCharts.renderChartEmptyNote = renderChartEmptyNote;

// 해 단위로 묶였으면 제목도 그렇게 말해야 한다 - "월별 매매 금액" 아래에 연 합계가 서 있으면
// 막대 하나가 한 달치로 읽힌다. 제목은 barShell 이 그려 둔 h2(data-page-section)와 캔버스의
// 접근성 이름 둘 다 갈아 끼운다. 서버가 다시 그려 주는 경우를 위해 원래 문구는 남겨 둔다.
function applyMonthlyBucketTitle(
	canvas: HTMLCanvasElement,
	bucket: "month" | "year",
) {
	let scope: HTMLElement | null = canvas.parentElement;
	while (scope && !scope.querySelector("[data-page-section]")) {
		scope = scope.parentElement;
	}
	const heading = scope?.querySelector("[data-page-section]") as HTMLElement | null;
	if (!heading) return;
	if (!heading.dataset.barTitleDefault) {
		heading.dataset.barTitleDefault = heading.textContent || "";
	}
	const yearly = appMessage(
		"stockChartTitleYearlyTradeAmount",
		"Yearly trade amount",
	);
	const next =
		bucket === "year" ? yearly : heading.dataset.barTitleDefault || "";
	if (!next) return;
	heading.textContent = next;
	canvas.setAttribute("aria-label", next);
}

// 실현손익은 그 달에 <b>판</b> 결과다 - 달과 달 사이에 이어지는 값이 아니다.
//
// 2026-09-14 이전에는 선이었다. 안 판 달을 null 로 두긴 했으나 `spanGaps: true` 가 그 구멍을 건너뛰어
// 이어 버렸고 `tension` 이 두 매도 사이에 없는 궤적까지 그렸다(실측: 18 해 중 11 해만 매도인데 선은 18 칸을
// 끊김 없이 갔다). 그 뒤 점으로 바꿔 봤으나 막대 둘 옆에 점 하나는 문법이 섞여 어색했다.
//
// 그래서 <b>같은 막대 문법으로, 자기 칸에서</b> 읽히게 한다. 실현손익은 그해 거래금액의 0~38%
// (중앙값 3% 안팎)라 같은 축에 두면 대부분의 해에 2~7px 로 뭉개지고, 축만 달리해 막대를 나란히 두면
// 높이가 견줄 수 있는 것처럼 보인다. 위/아래 칸으로 갈라 각자 축에서 읽게 한다.
const REALIZED_PROFIT_COLOR = "rgba(189,44,56,0.75)";
const REALIZED_LOSS_COLOR = "rgba(49,89,196,0.75)";

/**
 * 1 CSS 픽셀 선을 장치 픽셀 격자에 맞춘다.
 *
 * 캔버스 컨텍스트는 이미 DPR 로 확대돼 있으므로 좌표와 두께는 CSS 픽셀로 준다. 그대로 주면
 * 선이 장치 픽셀 사이에 걸쳐 두 줄로 번지고, 정수로 반올림하면 이번엔 제자리를 벗어난다
 * (실측 2026-09-14: 경계가 179.667px 인데 `Math.round(x) + 0.5` 가 180.5px 에 그려 0.83px 어긋났다).
 *
 * 규칙은 하나다 - 장치 픽셀 폭이 <b>홀수면 중심이 반픽셀</b>, 짝수면 정수여야 또렷하다.
 */
function snapLine(position: number, dpr: number) {
	const ratio = dpr > 0 ? dpr : 1;
	const widthDev = Math.max(1, Math.round(ratio));
	// 홀수 폭은 '가장 가까운 반픽셀' 로 가야 한다. 정수로 반올림한 뒤 0.5 를 더하면 최대 0.5 장치픽셀
	// 더 밀린다(실측: 179.667 -> 180.5, 0.83px 어긋남). 내림 후 0.5 가 맞다.
	const centerDev =
		widthDev % 2 === 1
			? Math.floor(position * ratio) + 0.5
			: Math.round(position * ratio);
	return { center: centerDev / ratio, width: widthDev / ratio };
}

/**
 * 두 칸(매매 금액 / 실현손익) 사이의 구분선.
 *
 * 축을 위아래로 쌓으면 경계에 눈금이 두 벌 붙는다 - 위 칸의 0 과 아래 칸의 꼭대기가 나란히 서서
 * 어느 숫자가 어느 칸 것인지 한눈에 안 갈린다(실측 2026-09-14). 선 하나로 칸을 갈라 준다.
 *
 * 선은 두 칸이 맞닿는 자리(`y.bottom` = `y2.top`)에 정확히 놓는다 - 위 칸 막대의 밑변이 거기다.
 */
const panelDividerPlugin = {
	id: "monthlyPanelDivider",
	// 아래 칸에 아주 옅은 바탕을 깐다. 경계의 눈금(아래 칸의 꼭대기 값)이 선 위에 놓여
	// 위 칸의 기준선처럼 읽히던 것을, 구역을 눈으로 갈라 해소한다.
	beforeDatasetsDraw(chart: any) {
		const bottom = chart.scales?.y2;
		const area = chart.chartArea;
		if (!bottom || !bottom.options?.display || !area) return;
		const ctx = chart.ctx;
		ctx.save();
		ctx.fillStyle = "rgba(128,128,128,0.055)";
		// 눈금 자리까지 함께 덮는다 - 경계의 눈금은 아래 칸 값인데, 바탕이 그림 영역에서만 끝나면
		// 그 눈금만 흰 바탕에 남아 여전히 위 칸 것처럼 읽힌다.
		ctx.fillRect(
			0,
			Math.round(bottom.top),
			Math.round(area.right),
			Math.round(bottom.bottom) - Math.round(bottom.top),
		);
		ctx.restore();
	},
	afterDatasetsDraw(chart: any) {
		const top = chart.scales?.y;
		const bottom = chart.scales?.y2;
		if (!top || !bottom || !bottom.options?.display) return;
		const area = chart.chartArea;
		if (!area) return;
		const line = snapLine(top.bottom, chart.currentDevicePixelRatio || 1);
		const ctx = chart.ctx;
		ctx.save();
		ctx.strokeStyle = "rgba(128,128,128,0.45)";
		ctx.lineWidth = line.width;
		ctx.beginPath();
		ctx.moveTo(Math.round(area.left), line.center);
		ctx.lineTo(Math.round(area.right), line.center);
		ctx.stroke();
		ctx.restore();
	},
};

/** 아래 칸에 들어가는 월별 실현손익 막대. */
function buildRealizedProfitDataset(
	profitData: (number | null)[],
	bucket: "month" | "year" = "month",
) {
	const colors = profitData.map((v) =>
		Number(v) < 0 ? REALIZED_LOSS_COLOR : REALIZED_PROFIT_COLOR,
	);
	// 해 단위로 묶였으면 범례도 그렇게 말해야 한다 - 제목은 이미 바꾸는데(applyMonthlyBucketTitle)
	// 범례만 "매도한 달" 로 남으면 한 칸이 한 달로 읽힌다.
	return {
		type: "bar",
		label:
			bucket === "year"
				? appMessage(
						"stockLabelRealizedProfitYears",
						"Realized profit (years with sales)",
					)
				: appMessage(
						"stockLabelRealizedProfitMonths",
						"Realized profit (months with sales)",
					),
		// 판 칸에만 값이 있다. 안 판 칸은 null 이라 막대가 아예 서지 않는다.
		data: profitData,
		// 매수/매도와 <b>같은 x 축</b>을 쓴다. 전용 축을 따로 두면 칸 가운데가 어긋난다
		// (실측 2026-09-14: 매수·매도 쌍의 중앙 349.8px 인데 손익 막대는 373.1px - 23.3px 오른쪽).
		xAxisID: "x",
		yAxisID: "y2",
		// grouped:false 라야 매수/매도 옆 '세 번째 자리' 로 밀리지 않고 칸 한가운데에 선다.
		grouped: false,
		// 묶이지 않으면 칸 전체 폭을 먹어 매수/매도보다 꼭 두 배 두꺼워진다(실측 20.97 vs 10.49).
		// 기본값(0.9)의 절반을 줘서 매수/매도 한 개와 같은 폭으로 맞춘다.
		barPercentage: 0.45,
		backgroundColor: colors,
		borderRadius: 3,
		maxBarThickness: 36,
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
	applyMonthlyBucketTitle(ctx, m.bucket);
	if (
		renderChartEmptyNote(
			canvasId,
			m.labels.length === 0,
			appMessage("stockChartMessageNoTradeData", "No trades in this period."),
		)
	) {
		if (existingInstance)
			try {
				existingInstance.destroy();
			} catch (e) {}
		return null;
	}
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
		plugins: [panelDividerPlugin],
		data: {
			labels: m.labels,
			datasets: [
				{
					label: appMessage("stockLabelBuy", "Buy"),
					data: m.buyData,
					xAxisID: "x",
					yAxisID: "y",
					backgroundColor: "rgba(239,68,68,0.7)",
					borderRadius: 6,
					maxBarThickness: 36,
				},
				{
					label: appMessage("stockLabelSell", "Sell"),
					data: m.sellData,
					xAxisID: "x",
					yAxisID: "y",
					backgroundColor: "rgba(59,130,246,0.7)",
					borderRadius: 6,
					maxBarThickness: 36,
				},
				// 실현손익(오른쪽 축). 매도가 하나도 없으면 축도 표식도 없다.
				...(m.hasSell ? [buildRealizedProfitDataset(m.profitData, m.bucket)] : []),
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
							// 실현손익만 부호가 뜻이다(아래 칸 막대).
							if (ctx.dataset.yAxisID === "y2") {
								const v = Math.round(Number(ctx.parsed.y) || 0);
								// 0 에는 방향이 없다 - 반올림 뒤에 판정한다.
								const sign = v > 0 ? "+" : v < 0 ? "-" : "";
								return ctx.dataset.label + ": " + sign + "\u20a9" + fmtAmt(Math.abs(v));
							}
							return ctx.dataset.label + ": \u20a9" + fmtAmt(ctx.parsed.y);
						},
					},
				},
			},
			scales: {
				// 세 막대가 한 축을 쓴다 - 같은 달이 같은 세로줄에 서려면 축이 하나여야 한다.
				x: { grid: gridX, ticks: { font: { size: 10 } } },
				// 매도가 있을 때만 칸을 나눈다 - 판 적이 없으면 아래 칸은 빈 띠일 뿐이다.
				//
				// 스택 안에서는 <b>먼저 적은 축이 아래</b>다(실측 2026-09-14: y 를 먼저 적었더니 손익 칸이 위로 갔다).
				// 실현손익 칸은 x 눈금 바로 위에 와야 "이 해에 얼마" 로 읽히므로 y2 를 먼저 적는다.
				y2: {
					display: m.hasSell,
					stack: "monthly",
					// 1 로 두면 아래 칸이 56px 이라 눈금 간격이 2억 단위로 벌어져 축이 -2~2억 이 된다
					// (실제 값은 -0.07~1.3억). 눈금 셋이 촘촘히 들어갈 만큼만 키운다.
					stackWeight: 1.5,
					// beginAtZero 는 두지 않는다 - 막대 차트는 이미 0 을 기준선으로 잡고, 이걸 켜면
					// 눈금이 대칭으로 벌어져(실측 2026-09-14: 실제 -0.07~1.3억인데 축이 -2~2억) 아래 칸 절반이 빈다.
					grid: gridY,
					ticks: {
						font: { size: 10 },
						callback: (v: any) => compactNumber(v),
					},
				},
				y: {
					...(m.hasSell ? { stack: "monthly", stackWeight: 3 } : {}),
					grid: gridY,
					ticks: {
						font: { size: 10 },
						// 위 칸의 0 은 아래 칸의 꼭대기 눈금과 나란히 서서 겹친다. 둘 중 이 0 을 비운다 -
						// 막대가 어디서 시작하는지는 구분선이 말해 주고, 아래 칸은 눈금 하나를 잃으면
						// 양수 쪽 배율이 통째로 사라진다(실측 2026-09-14 연 단위: 0 과 -1억 만 남았다).
						callback: (v: any) =>
							m.hasSell && Number(v) === 0 ? "" : compactNumber(v),
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
			holder.className = "text-xs text-base-content/70 pt-4 text-center";
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
		// 조각마다 따로 반올림하면 범례 백분율의 합이 100.0 이 아니게 된다(실측: 매매 도넛 43개 합 99.9%).
		const shownPercents = balancedPercents(d.data, 1);
		legendEl.innerHTML = d.labels
			.map((l: string, i: number) => {
				const pct = shownPercents[i];
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
					'<span class="chart-legend-swatch" style="flex-shrink:0;display:inline-block;width:8px;height:8px;border-radius:50%;background:' +
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
	const animate = opts && opts.animate === false ? false : true;
	const t = texts || {};
	const o = opts || {};
	// maxLabel/minLabel 이 주어진 차트만 범위 내 최고/최저 주석을 그린다
	const showExtremes = !!(t.maxLabel && t.minLabel);

	// 원가 계열이 비어 있으면 그 선은 아예 긋지 않는다. 지금 보유가 없는 종목의 "현재 평균단가" 는
	// 0 이 아니라 없는 값인데, 0 으로 그으면 종가가 20 만 원인 차트 바닥에 선이 깔려 공짜로 산 것처럼
	// 읽힌다(실측 2026-09-12: 매매 이력 43 종목 중 34 종목이 수량 0). 평가액 선의 채움도 원가 선을
	// 기준으로 삼고 있으니(target "-1") 함께 끈다 - 기준이 없는데 위/아래를 칠할 수는 없다.
	const hasCostSeries = costData.length > 0;

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
					drawArrow(px, py + 2, true, chartMarkerColors().buy);
					drawn.push({ px: px, py: py + 2 + arrow * 0.7, date: labels[i] });
				}
				if (parseFloat(dailyRealizedData[i]) > 0) {
					drawArrow(px, py - 2, false, chartMarkerColors().sell);
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
				const gap = extremeGapText(lastV, extreme);
				return gap ? " (" + gap + ")" : "";
			}
			let backColor = "#ffffff";
			try {
				const bv = getComputedStyle(document.documentElement)
					.getPropertyValue("--color-base-100")
					.trim();
				if (bv) backColor = bv;
			} catch (e) {}
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
				// 글자 뒤에 바탕색을 깐다 - 매매 표시(▲▼)가 먼저 그려져 그 위에 글자가 얹히면 읽을 수 없다
				// (실측 2026-10-01 자산 성장 '전체': "최저" 가 첫 매도 표시 셋에 덮였다).
				ctx.fillStyle = backColor;
				ctx.globalAlpha = 0.85;
				ctx.fillRect(tx - 2, ty - 10, w + 4, 13);
				ctx.fillStyle = baseColor;
				ctx.globalAlpha = 0.95;
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
				...(hasCostSeries
					? [
							{
								type: "line",
								label: t.costLabel || "",
								data: costData,
								borderColor: "rgba(139, 145, 156, 1)",
								borderWidth: 2,
								borderDash: [5, 5],
								fill: false,
								order: 0,
							},
						]
					: []),
				{
					type: "line",
					label: t.valueLabel || "",
					data: valueData,
					borderColor: "rgba(62, 159, 159, 1)",
					borderWidth: 2,
					fill: !hasCostSeries ? false : {
						target: "-1",
						above: "rgba(255, 99, 132, 0.25)",
						below: "rgba(54, 162, 235, 0.25)",
					},
					order: 1,
				},
			],
		},
		options: {
			// animate 를 안 주면 켜 둔다 - 예전에는 그 반대라, 호출부가 한 줄 빠뜨리면
			// 그 화면만 조용히 정지 화면이 됐다.
			animation: animate === false ? false : { duration: CHART_ANIMATION_MS },
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
								const diff = Math.round(v - c);
								const pct = c !== 0 ? Number((((v - c) / c) * 100).toFixed(2)) : 0;
								// 0 에는 방향이 없다 - 둘 다 반올림 뒤에 판정한다.
								const diffSign = diff > 0 ? "+" : diff < 0 ? "-" : "";
								const pctSign = pct > 0 ? "+" : pct < 0 ? "-" : "";
								lines.push("─────────────────");
								lines.push(
									(t.profitLabel || "") +
										": " +
										diffSign +
										StockCharts.formatCurrency!(Math.abs(diff)) +
										" (" +
										pctSign +
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

// 종목 상세 주가 추이 캔들(사용자 요청 2026-10-02: "일반적인 주식 막대 그래프(시가 · 종가 · 고가 · 저가)").
// 서버는 일봉(시 · 고 · 저 · 종 · 그 날 평균 단가)을 준다. 봉이 너무 많으면(전체 기간 삼성전자 1,600 일) 뭉개지므로 증권사 앱처럼
// 일봉 -> 주봉 -> 월봉으로 묶는다: 시가 = 첫날 시가, 종가 = 마지막 날 종가, 고가 = 최고, 저가 = 최저, 평단 = 마지막 날 평단(그 봉이 끝날 때).
// 순수 계산이라 시험에서 직접 부른다.
function candleBuckets(series: any, maxCandles?: number) {
	const limit = maxCandles && maxCandles > 0 ? maxCandles : 130;
	const labels: string[] = (series && series.labels) || [];
	const num = (v: any) => (v === null || v === undefined || v === "" ? null : Number(v));
	const days = labels.map((label: string, i: number) => ({
		date: String(label),
		open: num(series.open[i]),
		high: num(series.high[i]),
		low: num(series.low[i]),
		close: num(series.close[i]),
		avg: series.avg ? num(series.avg[i]) : null,
		buy: series.buy ? Number(series.buy[i]) || 0 : 0,
		sell: series.sell ? Number(series.sell[i]) || 0 : 0,
		dist: series.dist ? Number(series.dist[i]) || 0 : 0,
		// 그 날 보유 평가액 · 원가(2026-10-07, 보유 평가액 추이 차트를 합쳤다) - 보유가 없던 날은 null.
		hv: series.hv ? num(series.hv[i]) : null,
		hc: series.hc ? num(series.hc[i]) : null,
	})).filter((d: any) => d.close !== null);
	function weekKey(date: string): string {
		// 월요일 시작 주. 시간대와 무관하게 날짜만으로 센다(UTC 자정으로 만들어 요일을 읽는다).
		const d = new Date(date + "T00:00:00Z");
		const back = (d.getUTCDay() + 6) % 7;
		d.setUTCDate(d.getUTCDate() - back);
		return d.toISOString().slice(0, 10);
	}
	function group(keyOf: (date: string) => string) {
		const out: any[] = [];
		days.forEach((d: any) => {
			const key = keyOf(d.date);
			const last = out.length ? out[out.length - 1] : null;
			if (last && last.key === key) {
				last.label = d.date;
				if (d.high !== null) last.high = last.high === null ? d.high : Math.max(last.high, d.high);
				if (d.low !== null) last.low = last.low === null ? d.low : Math.min(last.low, d.low);
				last.close = d.close;
				last.avg = d.avg;
				last.buy += d.buy;
				last.sell += d.sell;
				last.dist += d.dist;
				// 평가액 · 원가는 평단처럼 그 봉이 끝날 때 값(합치면 뜻이 없다).
				last.hv = d.hv;
				last.hc = d.hc;
			} else {
				out.push({ key: key, label: d.date, open: d.open, high: d.high, low: d.low, close: d.close, avg: d.avg, buy: d.buy, sell: d.sell, dist: d.dist, hv: d.hv, hc: d.hc });
			}
		});
		return out;
	}
	let unit = "day";
	let candles: any[] = days.map((d: any) => ({ key: d.date, label: d.date, open: d.open, high: d.high, low: d.low, close: d.close, avg: d.avg, buy: d.buy, sell: d.sell, dist: d.dist, hv: d.hv, hc: d.hc }));
	if (candles.length > limit) {
		unit = "week";
		candles = group(weekKey);
	}
	if (candles.length > limit) {
		unit = "month";
		candles = group((date: string) => date.slice(0, 7));
	}
	return { unit: unit, candles: candles };
}
StockCharts.candleBuckets = candleBuckets;

// 세로축 범위(사용자 요청 2026-10-02: "평단이 너무 낮으면 세로축은 캔들 기준으로"). 평단을 넣으면 축이 캔들 범위(저가~고가)의 30% 넘게
// 늘어나는 경우 축을 캔들에 맞추고(위아래 5% 여유) 평단 선은 축 밖에서 잘린다 - 그때는 범례에 축 위/아래와 마지막 평단 값을 적는다.
// 실측: 삼성전자 최근 3 개월 캔들 25~30 만인데 평단 7.2 만이라 축이 5 만까지 내려가 캔들이 위쪽에 몰렸다. 아니면 null(Chart.js 자동).
function candleYRange(candles: any[]) {
	let low = Infinity;
	let high = -Infinity;
	let avgLow = Infinity;
	let avgHigh = -Infinity;
	let lastAvg: number | null = null;
	candles.forEach((c: any) => {
		const l = c.low === null || c.low === undefined ? c.close : c.low;
		const h = c.high === null || c.high === undefined ? c.close : c.high;
		if (l !== null && l !== undefined) low = Math.min(low, l);
		if (h !== null && h !== undefined) high = Math.max(high, h);
		if (c.avg !== null && c.avg !== undefined) {
			avgLow = Math.min(avgLow, c.avg);
			avgHigh = Math.max(avgHigh, c.avg);
			lastAvg = c.avg;
		}
	});
	if (!isFinite(low) || !isFinite(high) || !isFinite(avgLow)) return null;
	const span = Math.max(high - low, Math.abs(high) * 0.01);
	const below = low - avgLow > span * 0.3;
	const above = avgHigh - high > span * 0.3;
	if (!below && !above) return null;
	// 축 끝은 보기 좋은 단위로 내림 · 올림한다(18.2 만 · 33.2 만 대신 18 만 · 34 만) - 끝 눈금이 그 값으로 찍힌다.
	const step = Math.pow(10, Math.floor(Math.log10(span / 5)));
	return {
		min: Math.floor((low - span * 0.05) / step) * step,
		max: Math.ceil((high + span * 0.05) / step) * step,
		side: lastAvg !== null && lastAvg > high ? "above" : "below",
		lastAvg: lastAvg,
	};
}
StockCharts.candleYRange = candleYRange;

/**
 * 차트 위 표시(매수 ▲ · 매도 ▼ · 분배락 ◆) 색 - 그릴 때마다 그때 테마로 고른다(테마 전환 · 인쇄 중 라이트 전환에도 맞는다).
 *
 * 실측 2026-10-03(WCAG 1.4.11 비텍스트 대비 3:1, 흰 카드 배경): 옛 색 분홍 ▲ 2.72 · 파랑 ▼ 2.65 · 주황 ◆ 3.00 으로 라이트에서 모자랐다
 * (다크 rgb(15,22,35) 에서는 5.2~6.0 으로 충분). 라이트만 한 단계 진한 색(4.6 · 5.2 · 5.0)으로 바꾼다.
 */
function chartMarkerColors(): { buy: string; sell: string; dist: string } {
	const dark = typeof document !== "undefined" && !!document.documentElement && document.documentElement.getAttribute("data-theme") === "dark";
	return dark
		? { buy: "rgba(255, 99, 132, 0.95)", sell: "rgba(54, 162, 235, 0.95)", dist: "rgba(217, 119, 6, 0.95)" }
		: { buy: "rgb(219, 39, 119)", sell: "rgb(37, 99, 235)", dist: "rgb(180, 83, 9)" };
}
/**
 * 툴팁 한 줄을 이름 · 금액 · 덧붙임으로 가른다(2026-10-07, 금액 열을 자리수대로 세우려고). "평가 손익: +₩1,009,170,921 (+278.37%)" ->
 * ["평가 손익", "+₩1,009,170,921", "(+278.37%)"]. ": " 가 없는 줄(▲ 매수 15주)은 이름 칸에만 둔다. 순수 계산이라 시험에서 직접 부른다.
 */
function splitTooltipLine(line: string): string[] {
	const text = String(line);
	const at = text.indexOf(": ");
	if (at < 0) return [text, "", ""];
	const name = text.slice(0, at);
	const rest = text.slice(at + 2);
	const paren = rest.indexOf(" (");
	return paren < 0 ? [name, rest, ""] : [name, rest.slice(0, paren), rest.slice(paren + 1)];
}
StockCharts.splitTooltipLine = splitTooltipLine;

/** 캔들 툴팁(HTML). 금액 칸은 오른쪽 정렬 + 고정폭 숫자(tabular-nums)라 자리수가 세로로 맞는다. 글자는 콜백이 만든 그대로(textContent - 마크업으로 읽지 않는다). */
function candleTooltipHandler(context: any) {
	const tooltip = context.tooltip;
	let el = document.getElementById("candleTooltipEl") as HTMLElement | null;
	if (!el) {
		el = document.createElement("div");
		el.id = "candleTooltipEl";
		el.setAttribute("aria-hidden", "true");
		el.style.cssText =
			"position:fixed;z-index:9999;background:rgba(30,30,30,0.92);color:#fff;padding:6px 10px;border-radius:6px;font-size:12px;line-height:1.45;pointer-events:none;white-space:nowrap;transition:opacity .1s;";
		document.body.appendChild(el);
	}
	if (!tooltip || tooltip.opacity === 0) {
		el.style.opacity = "0";
		return;
	}
	el.textContent = "";
	const title = document.createElement("div");
	title.style.cssText = "font-weight:bold;margin-bottom:3px";
	title.textContent = tooltip.title && tooltip.title.length ? tooltip.title.join(" ") : "";
	el.appendChild(title);
	const table = document.createElement("table");
	table.style.cssText = "border-collapse:collapse;font-variant-numeric:tabular-nums";
	(tooltip.body || []).forEach((part: any) => {
		(part.lines || []).forEach((line: string) => {
			const cells = splitTooltipLine(line);
			const tr = document.createElement("tr");
			cells.forEach((cell: string, i: number) => {
				const td = document.createElement("td");
				td.textContent = cell;
				td.style.cssText =
					i === 0
						? "padding:0 10px 0 0;text-align:left"
						: i === 1
							? "padding:0;text-align:right"
							: "padding:0 0 0 6px;text-align:left;opacity:.75";
				if (i === 0 && !cells[1]) td.colSpan = 3;
				if (i === 0 || cells[1]) tr.appendChild(td);
			});
			table.appendChild(tr);
		});
	});
	el.appendChild(table);
	const rect = context.chart.canvas.getBoundingClientRect();
	const x = rect.left + tooltip.caretX;
	const y = rect.top + tooltip.caretY;
	el.style.opacity = "1";
	el.style.left = "0px";
	el.style.top = "0px";
	const tw = el.offsetWidth;
	const th = el.offsetHeight;
	let finalX = x + 12;
	let finalY = y - th / 2;
	if (finalX + tw > window.innerWidth - 8) finalX = x - tw - 12;
	if (finalX < 8) finalX = 8;
	if (finalY + th > window.innerHeight - 8) finalY = window.innerHeight - th - 8;
	if (finalY < 8) finalY = 8;
	el.style.left = finalX + "px";
	el.style.top = finalY + "px";
}

StockCharts.candleChartConfig = function (series: any, texts: any) {
	const t = texts || {};
	const buckets = StockCharts.candleBuckets!(series);
	const candles: any[] = buckets.candles;
	// 한국 관례: 오르면 빨강, 내리면 파랑(손익 색과 같다). 시가 = 종가면 회색.
	const UP = "rgba(220,38,38,0.9)";
	const DOWN = "rgba(37,99,235,0.9)";
	const FLAT = "rgba(128,128,128,0.9)";
	const colorOf = (c: any) =>
		c.open === null || c.close === null || c.close === c.open ? FLAT : c.close > c.open ? UP : DOWN;
	const colors = candles.map(colorOf);
	const labels = candles.map((c: any) => (buckets.unit === "month" ? c.label.slice(0, 7) : c.label));
	// 시가 = 종가인 봉은 몸통 높이가 0 이라 안 보인다 - 아주 얇게라도 남긴다(가격의 0.05%).
	const body = candles.map((c: any) => {
		const o = c.open === null ? c.close : c.open;
		if (o === c.close) {
			const e = Math.max(Math.abs(c.close) * 0.0005, 0.5);
			return [c.close - e, c.close + e];
		}
		return [o, c.close];
	});
	const wick = candles.map((c: any) => [c.low === null ? c.close : c.low, c.high === null ? c.close : c.high]);
	const hasAvg = candles.some((c: any) => c.avg !== null && c.avg !== undefined);
	const fmt = (v: any) => (StockCharts.formatCurrency ? StockCharts.formatCurrency(v) : String(v));
	const yRange = hasAvg ? candleYRange(candles) : null;
	// "{0}주" / "{0} shares"(1 이면 "{0} share") - 서버 메시지 패턴을 받아 숫자만 채운다.
	const shareText = (n: number) => {
		const pattern = n === 1 && t.shareOnePattern ? t.shareOnePattern : t.sharePattern || "{0}";
		return String(pattern).replace("{0}", n.toLocaleString(StockCharts.getLocale ? StockCharts.getLocale() : undefined));
	};
	// 내 매수 ▲(저가 아래) · 매도 ▼(고가 위). 보유 평가액 차트의 표시와 같은 색 · 모양(2026-10-02, "어느 가격대에서 샀는지").
	const tradeMarkers = {
		id: "candleTradeMarkers",
		afterDatasetsDraw: function (chart: any) {
			const ctx = chart.ctx;
			const xAxis = chart.scales["x"];
			const yAxis = chart.scales["y"];
			if (!xAxis || !yAxis) return;
			const size = 5;
			const area = chart.chartArea;
			candles.forEach((c: any, i: number) => {
				const px = xAxis.getPixelForValue(i);
				if (px < area.left || px > area.right) return;
				if (c.buy > 0) {
					const py = Math.min(yAxis.getPixelForValue(c.low === null ? c.close : c.low) + 4, area.bottom - size * 1.4);
					ctx.save();
					ctx.fillStyle = chartMarkerColors().buy;
					ctx.beginPath();
					ctx.moveTo(px, py);
					ctx.lineTo(px + size, py + size * 1.4);
					ctx.lineTo(px - size, py + size * 1.4);
					ctx.closePath();
					ctx.fill();
					ctx.restore();
				}
				// 분배락(◆, 2026-10-02): 원주가 차트라 매달 분배금만큼 떨어지는 날이 있다 - 왜 떨어졌는지 보이게 x 축 바로 위에 작은 마름모.
				if (c.dist > 0) {
					const py = area.bottom - 4;
					ctx.save();
					ctx.fillStyle = chartMarkerColors().dist;
					ctx.beginPath();
					ctx.moveTo(px, py - 4);
					ctx.lineTo(px + 3, py);
					ctx.lineTo(px, py + 4);
					ctx.lineTo(px - 3, py);
					ctx.closePath();
					ctx.fill();
					ctx.restore();
				}
				if (c.sell > 0) {
					const py = Math.max(yAxis.getPixelForValue(c.high === null ? c.close : c.high) - 4, area.top + size * 1.4);
					ctx.save();
					ctx.fillStyle = chartMarkerColors().sell;
					ctx.beginPath();
					ctx.moveTo(px, py);
					ctx.lineTo(px + size, py - size * 1.4);
					ctx.lineTo(px - size, py - size * 1.4);
					ctx.closePath();
					ctx.fill();
					ctx.restore();
				}
			});
		},
	};
	// 축 밖 평단을 축 끝 바로 안쪽에 붙인다(축 높이의 0.6%) - 축 선과 겹치면 안 보인다.
	const pinToAxis = (v: number) => {
		if (!yRange) return v;
		const pad = (yRange.max - yRange.min) * 0.006;
		return Math.min(Math.max(v, yRange.min + pad), yRange.max - pad);
	};
	// 축 밖 평단의 실제 값을 선 오른끝에 적는다("평균 단가 72,150 ▼") - 범례만으로는 선이 축 끝에 붙은 까닭을 놓친다.
	const avgEdgeLabel = {
		id: "candleAvgEdgeLabel",
		afterDatasetsDraw: function (chart: any) {
			if (!yRange || yRange.lastAvg === null || yRange.lastAvg === undefined) return;
			const area = chart.chartArea;
			const yAxis = chart.scales["y"];
			if (!area || !yAxis) return;
			const text = (t.avgLabel || "Average cost") + " " + fmt(yRange.lastAvg) + (yRange.side === "above" ? " \u25b2" : " \u25bc");
			const ctx = chart.ctx;
			ctx.save();
			ctx.font = "10px sans-serif";
			ctx.textAlign = "right";
			ctx.textBaseline = yRange.side === "above" ? "top" : "bottom";
			ctx.fillStyle = "rgba(120,120,120,0.95)";
			const y = yAxis.getPixelForValue(pinToAxis(yRange.lastAvg)) + (yRange.side === "above" ? 3 : -3);
			ctx.fillText(text, area.right - 2, y);
			ctx.restore();
		},
	};
	// 범위 안 최고가 · 최저가(사용자 요청 2026-10-07: "최고 최저 표시도 주가 추이에") - 합치기 전 보유 평가액 차트가 하던 표시를 캔들에.
	// 최고는 봉의 고가, 최저는 저가 중에서 고른다(종가가 아니다 - 봉이 그리는 끝점이 그 값이어야 표시가 봉 끝에 붙는다). 옆에 마지막 종가가 그 값에서
	// 얼마나 떨어져 있는지(extremeGapText, 자산 성장 차트와 같은 규칙). 매매 표시(고가 위 ▼ · 저가 아래 ▲)와 겹치면 한 칸 더 띄운다.
	const candleExtremes = {
		id: "candleRangeExtremes",
		afterDatasetsDraw: function (chart: any) {
			if (!t.maxLabel || !t.minLabel || candles.length < 2) return;
			const xAxis = chart.scales["x"];
			const yAxis = chart.scales["y"];
			if (!xAxis || !yAxis) return;
			let maxIdx = -1;
			let minIdx = -1;
			let maxV = -Infinity;
			let minV = Infinity;
			candles.forEach((c: any, i: number) => {
				const h = c.high === null || c.high === undefined ? c.close : c.high;
				const l = c.low === null || c.low === undefined ? c.close : c.low;
				if (h !== null && h > maxV) {
					maxV = h;
					maxIdx = i;
				}
				if (l !== null && l < minV) {
					minV = l;
					minIdx = i;
				}
			});
			const lastClose = candles[candles.length - 1].close;
			if (maxIdx < 0 || minIdx < 0 || maxV === minV || lastClose === null) return;
			const area = chart.chartArea;
			let baseColor = "#6b7280";
			let backColor = "#ffffff";
			try {
				const style = getComputedStyle(document.documentElement);
				baseColor = style.getPropertyValue("--color-base-content").trim() || baseColor;
				backColor = style.getPropertyValue("--color-base-100").trim() || backColor;
			} catch (e) {}
			const ctx = chart.ctx;
			const draw = (idx: number, v: number, isMax: boolean, label: string, bump: boolean) => {
				const px = xAxis.getPixelForValue(idx);
				if (px < area.left || px > area.right) return;
				const py = yAxis.getPixelForValue(v);
				const gap = extremeGapText(lastClose, v);
				const text = label + " " + fmt(v) + (gap ? " (" + gap + ")" : "");
				ctx.save();
				ctx.font = "11px sans-serif";
				const w = ctx.measureText(text).width;
				let tx = px - w / 2;
				if (tx < area.left + 2) tx = area.left + 2;
				if (tx + w > area.right - 2) tx = area.right - 2 - w;
				const extra = bump ? 9 : 0;
				let ty = isMax ? py - 6 - extra : py + 15 + extra;
				if (ty < area.top + 11) ty = area.top + 11;
				if (ty > area.bottom - 3) ty = area.bottom - 3;
				// 글자 뒤에 바탕색을 깐다(봉 · 평단 점선 위에 얹혀도 읽히게 - 보유 평가액 차트와 같은 처리).
				ctx.fillStyle = backColor;
				ctx.globalAlpha = 0.85;
				ctx.fillRect(tx - 2, ty - 10, w + 4, 13);
				ctx.fillStyle = baseColor;
				ctx.globalAlpha = 0.95;
				ctx.fillText(text, tx, ty);
				ctx.restore();
			};
			draw(maxIdx, maxV, true, t.maxLabel, candles[maxIdx].sell > 0);
			draw(minIdx, minV, false, t.minLabel, candles[minIdx].buy > 0);
		},
	};
	const avgLegend =
		(t.avgLabel || "Average cost") +
		(yRange
			? " (" + (yRange.side === "above" ? t.avgAboveLabel || "above the axis" : t.avgBelowLabel || "below the axis") + " " + fmt(yRange.lastAvg) + ")"
			: "");
	const datasets: any[] = [
		{
			type: "bar",
			label: t.rangeLabel || "High-Low",
			// 화면 낭독 요약에서 뺀다 - 몸통(종가)이 그 날을 말한다(common.ts chartSummaryText).
			summaryHidden: true,
			data: wick,
			backgroundColor: colors,
			borderWidth: 0,
			// 테마 전환이 막대 테두리를 테마 회색으로 덮지 않게(applyChartTheme) - 덮으면 빨강 · 파랑 봉이 회색이 된다(2026-10-02).
			keepEdge: true,
			barPercentage: 0.12,
			categoryPercentage: 1,
			grouped: false,
			order: 2,
		},
		{
			type: "bar",
			// 요약이 "종가: 62개 지점, 처음 … 최고 … 최저 …" 로 읽히게 이름은 종가(값은 [시가, 종가] 의 끝).
			label: t.closeLabel || "Close",
			data: body,
			backgroundColor: colors,
			borderWidth: 0,
			keepEdge: true,
			barPercentage: 0.7,
			categoryPercentage: 0.9,
			grouped: false,
			order: 1,
		},
	];
	if (hasAvg) {
		datasets.push({
			type: "line",
			label: avgLegend,
			// 축이 캔들에 맞춰져 평단이 축 밖이면(사용자 요청 2026-10-02) 선을 축 끝에 붙여 그린다(2026-10-07: "평균 단가가 짧은 기간엔 안 보인다" -
			// 잘려서 아예 없었다). 실제 값은 툴팁 · 범례 · 끝 글자(avgEdgeLabel)가 말한다.
			data: candles.map((c: any) => (c.avg === null || c.avg === undefined ? null : pinToAxis(c.avg))),
			borderColor: "rgba(120,120,120,0.9)",
			backgroundColor: "rgba(120,120,120,0.9)",
			borderDash: [5, 4],
			borderWidth: 1.5,
			pointRadius: 0,
			pointHoverRadius: 3,
			spanGaps: false,
			stepped: "after",
			order: 0,
		});
	}
	return {
		unit: buckets.unit,
		config: {
			type: "bar",
			data: { labels: labels, datasets: datasets },
			plugins: [tradeMarkers, avgEdgeLabel, candleExtremes],
			options: {
				responsive: true,
				maintainAspectRatio: false,
				interaction: { mode: "index", intersect: false },
				plugins: {
					// 텍스트 대안에 표시 개수(▲▼◆ 는 모양뿐이라 요약문에 없으면 낭독기 사용자는 모른다, 2026-10-02).
					a11ySummary: {
						counts: [
							{ label: t.buyLabel || "Buy", count: candles.filter((c: any) => c.buy > 0).length },
							{ label: t.sellLabel || "Sell", count: candles.filter((c: any) => c.sell > 0).length },
							{ label: t.distLabel || "Ex-distribution", count: candles.filter((c: any) => c.dist > 0).length },
						],
					},
					legend: { display: hasAvg, labels: { filter: (item: any) => item.datasetIndex === 2, boxWidth: 14, font: { size: 10 } } },
					tooltip: {
						// 금액 자리수를 맞춰 세운다(사용자 요청 2026-10-07) - 캔버스 툴팁은 줄 단위로만 정렬돼 열을 못 맞춘다. 글자는 아래 콜백이 그대로 만들고
						// (탐침 · 시험이 콜백을 읽는다), 그리기만 HTML 표로 한다(candleTooltipHandler).
						enabled: false,
						external: candleTooltipHandler,
						callbacks: {
							title: (items: any[]) => (items.length ? labels[items[0].dataIndex] : ""),
							label: (ctx: any) => {
								const c = candles[ctx.dataIndex];
								if (ctx.datasetIndex === 0) return null;
								if (ctx.datasetIndex === 2) return c.avg === null || c.avg === undefined ? null : (t.avgLabel || "Average cost") + ": " + fmt(c.avg);
								const lines = [
									(t.openLabel || "Open") + ": " + fmt(c.open),
									(t.highLabel || "High") + ": " + fmt(c.high),
									(t.lowLabel || "Low") + ": " + fmt(c.low),
									(t.closeLabel || "Close") + ": " + fmt(c.close),
								];
								if (c.buy > 0) lines.push("▲ " + (t.buyLabel || "Buy") + " " + shareText(c.buy));
								if (c.sell > 0) lines.push("▼ " + (t.sellLabel || "Sell") + " " + shareText(c.sell));
								if (c.dist > 0) lines.push("◆ " + (t.distLabel || "Ex-distribution") + " " + fmt(c.dist));
								// 그 날 보유 평가액(2026-10-07: 보유 평가액 추이 차트를 합쳤다). 보유가 없던 날은 줄을 안 단다.
								// 순서는 매수 원가 -> 평가액 -> 평가 손익(사용자 요청 2026-10-07) - 셈 흐름(평가액 - 원가 = 손익) 그대로 읽힌다.
								if (c.hv !== null && c.hv !== undefined) {
									const hasCost = c.hc !== null && c.hc !== undefined && c.hc > 0;
									if (hasCost) lines.push((t.holdingCostLabel || "Cost") + ": " + fmt(c.hc));
									lines.push((t.holdingValueLabel || "Holdings value") + ": " + fmt(c.hv));
									if (hasCost) {
										const profit = c.hv - c.hc;
										const pct = (profit / c.hc) * 100;
										const sign = profit > 0 ? "+" : profit < 0 ? "-" : "";
										lines.push((t.holdingProfitLabel || "Unrealized") + ": " + sign + fmt(Math.abs(profit)) + " (" + sign + Math.abs(pct).toFixed(2) + "%)");
									}
								}
								return lines;
							},
						},
					},
				},
				scales: {
					x: { stacked: false, grid: { display: false }, ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 8, font: { size: 10 } } },
					y: {
						beginAtZero: false,
						min: yRange ? yRange.min : undefined,
						max: yRange ? yRange.max : undefined,
						grid: { color: "rgba(128,128,128,0.14)" },
						ticks: { callback: (v: any) => (StockCharts.formatCompactNumber ? StockCharts.formatCompactNumber(v) : v), font: { size: 10 } },
					},
				},
			},
		},
	};
};

StockCharts.createCandleChart = function (canvasId: string, series: any, texts: any, existingInstance?: any) {
	const built = StockCharts.candleChartConfig!(series, texts);
	const chart = StockCharts.createChart!(canvasId, built.config, existingInstance);
	if (chart) (chart as any).$candleUnit = built.unit;
	return chart;
};

// 축 눈금·범례 글자색을 테마 본문색에 맞춘다. 실측 2026-09-09(qa/chart-tick-contrast.cjs): Chart.js 기본 #666 은 다크 카드 배경
// rgb(15,22,35) 위에서 3.15:1 로 11px 글자 기준(4.5:1)에 못 미쳤다(라이트 5.74:1). --color-base-content 는 oklch 라 1px 캔버스로 rgb 를
// 얻고 alpha .75 로 본문보다 한 단계 옅게 쓴다. 테마 토글(html[data-theme]) 때 살아 있는 차트도 다시 그린다.
/**
 * 차트 애니메이션 길이(ms). <b>한 곳에서만 정한다.</b>
 *
 * 2026-09-14 까지는 화면마다 달랐다 - 자산 성장 · 종목/계좌 상세는 600ms 인데 대시보드 · 매매 · 배당 ·
 * 활동 · 시뮬레이터는 Chart.js 기본값 1000ms 였다(실측 9 화면 12 차트). 같은 앱에서 어떤 차트는 느리게,
 * 어떤 차트는 빠르게 그려지면 화면을 옮길 때마다 리듬이 달라진다.
 *
 * <p><b>prefers-reduced-motion 으로 감싸지 않는다</b> - 이 PC 는 Windows 애니메이션 효과가 꺼져 있어
 * 그 질의가 항상 참이다. 감싸면 의도한 애니메이션이 아예 안 보인다(전에 한 번 겪었다).
 */
const CHART_ANIMATION_MS = 600;

/**
 * 첫 등장 애니메이션.
 *
 * <p>애니메이션 길이를 맞춰도 <b>막대가 자라지 않았다</b>(도넛은 회전이 보였다). 실측 2026-09-14 -
 * 차트를 만들면 Chart.js 가 생성 도중 캔버스를 300x150 에서 실제 크기로 <b>리사이즈</b>하고,
 * 그 리사이즈는 {@code update("resize")} 로 처리되는데 이 전환은 길이가 <b>0</b> 이다. 그래서 최종 모양이
 * 즉시 박히고, 이어지는 첫 {@code update} 는 '같은 값 -> 같은 값' 이라 움직일 것이 남지 않는다:
 *
 * <pre>resize 300x150 -> resize 858x280 -> update(resize) -> update(undefined)</pre>
 *
 * <p>그래서 레이아웃이 끝난 다음 프레임에 <b>한 번만</b> 되감아 다시 그린다. 리사이즈 전환의 길이를 늘리는
 * 방법도 있지만 그러면 창 크기를 바꿀 때마다 모든 차트가 600ms 씩 다시 그려진다 - 이 앱은 그 재렌더를
 * 피하려고 {@code resizeIfChanged} 까지 두고 있다.
 */
const entryAnimationPlugin = {
	id: "chartEntryAnimation",
	afterInit(chart: any) {
		if (!chart || chart.$entryAnimationQueued) return;
		if (chart.options?.animation === false) return;
		chart.$entryAnimationQueued = true;
		const raf =
			typeof requestAnimationFrame === "function"
				? requestAnimationFrame
				: (fn: any) => setTimeout(fn, 16);
		// 두 프레임 기다린다 - 생성 직후의 리사이즈가 한 번 더 오는 경우가 있어(실측 t=11ms),
		// 되감자마자 그것이 다시 최종값으로 박아 버린다.
		raf(() =>
			raf(() => {
				try {
					if (!chart.canvas || !chart.ctx) return;
					chart.reset();
					chart.update();
				} catch (e) {}
			}),
		);
	},
};
StockCharts.entryAnimationPlugin = entryAnimationPlugin;

/** Chart.js 전역 기본값. 개별 차트가 options.animation 을 안 주면 이 값으로 그려진다. */
function applyChartAnimationDefaults(chartLib: any = (globalThis as any).Chart) {
	if (!chartLib?.defaults) return null;
	chartLib.defaults.animation = {
		...(chartLib.defaults.animation || {}),
		duration: CHART_ANIMATION_MS,
	};
	return chartLib.defaults.animation.duration;
}
StockCharts.applyChartAnimationDefaults = applyChartAnimationDefaults;

/**
 * Chart.js 전역 로케일. 기본값이 navigator.language 라, 콜백을 안 준 차트의 툴팁·축은
 * **브라우저 로케일**로 찍힌다(값은 맞고 자릿수 구분만 달라져 한국어 브라우저로는 안 보인다).
 *
 * 실측 2026-09-15: 브라우저를 de-DE 로 두자 매매 도넛의 내부 툴팁 모델이 "466.231.000" 이었다.
 * 그 차트는 external 툴팁으로 따로 그려 화면엔 안 나왔지만, 콜백 없는 차트가 하나라도 생기면
 * 그대로 나간다. 규칙은 appLocale 한 곳이므로 라이브러리 기본값도 거기에 맞춘다.
 */
function applyChartLocaleDefault(chartLib: any = (globalThis as any).Chart) {
	if (!chartLib?.defaults) return null;
	chartLib.defaults.locale = resolveLocale();
	return chartLib.defaults.locale;
}
StockCharts.applyChartLocaleDefault = applyChartLocaleDefault;

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
/**
 * 막대·도넛 조각의 테두리 색. 채움만으로는 요소 경계가 배경과 구분되지 않는다 - 실측 2026-09-10:
 * 라이트 152개 · 다크 47개 요소가 WCAG 1.4.11(비텍스트 3:1) 미달이었고(도넛 amber 1.51 · 막대 1.82~2.88),
 * 막대는 borderWidth 0, 도넛은 테두리색이 배경과 같아(대비 1.00) 채움이 유일한 단서였다.
 * 팔레트를 어둡게 바꾸면 계열끼리 구분이 되레 나빠지므로, 색은 두고 경계를 보이게 한다.
 * 값은 폼 컨트롤 경계와 같은 토큰(--color-control-line: 라이트 3.25 · 다크 3.34)을 쓴다.
 */
function chartElementEdgeColor(): string {
	let raw = "";
	try {
		raw = getComputedStyle(document.documentElement).getPropertyValue("--color-control-line").trim();
	} catch (e) {}
	if (!raw) return "#868fa1";
	const rgb = resolveCssColor(raw);
	return rgb ? "rgb(" + rgb.join(",") + ")" : raw;
}

/** 테두리를 얹을 차트 종류. 선 차트의 borderColor 는 선 자체 색이라 건드리면 안 된다. */
const EDGE_TYPES = ["bar", "doughnut", "pie", "polarArea"];

function applyChartTheme(chartLib: any = (globalThis as any).Chart): string | null {
	if (!chartLib || !chartLib.defaults) return null;
	const c = chartTextColor();
	chartLib.defaults.color = c;
	const edge = chartElementEdgeColor();
	chartLib.defaults.datasets = chartLib.defaults.datasets || {};
	for (const type of EDGE_TYPES) {
		chartLib.defaults.datasets[type] = chartLib.defaults.datasets[type] || {};
		chartLib.defaults.datasets[type].borderColor = edge;
		chartLib.defaults.datasets[type].borderWidth = 1;
	}
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
			// 이미 만들어진 차트는 defaults 를 안 다시 읽는다. 데이터셋에 직접 써 넣는다
			// (borderWidth 0 을 명시한 곳이 있어 falsy 면 1 로 올린다).
			for (const ds of (chart.data && chart.data.datasets) || []) {
				const type = ds.type || (chart.config && (chart.config.type || (chart.config._config && chart.config._config.type)));
				if (!EDGE_TYPES.includes(type)) continue;
				// 색이 곧 뜻인 막대(캔들 몸통 · 꼬리)는 건드리지 않는다 - 실측 2026-10-02: 테마를 바꾸면 봉 테두리가 회색 1px 로 덮여
				// 오름(빨강) · 내림(파랑)이 모두 회색으로 보였다.
				if (ds.keepEdge) continue;
				ds.borderColor = edge;
				if (!ds.borderWidth) ds.borderWidth = 1;
			}
			// "none" 으로 다시 그리면 막대들이 함께 쓰는 옵션(테두리색)이 갱신되지 않는다(Chart.js 의 direct 갱신은 shared options 를 건너뛴다) -
			// 실측 2026-10-02: 라이트 -> 다크 전환 뒤 막대 테두리가 라이트 회색(#868fa1) 그대로라 처음부터 다크로 연 화면(#5e6a84)과 달랐다.
			// 기본 갱신으로 옵션을 다시 풀되, 애니메이션은 잠시 꺼서 깜빡이지 않게 한다.
			// 원래 값은 사용자 옵션(config.options)에서 - chart.options 는 해석된 프록시라 그것을 되써 넣으면 겹겹이 쌓인다.
			// update 가 던져도 애니메이션이 꺼진 채 남지 않게 finally 로 되돌린다(2026-10-03 검토).
			const userOptions = chart.config && chart.config.options ? chart.config.options : chart.options;
			const animation = userOptions.animation;
			userOptions.animation = false;
			try {
				chart.update("default");
			} finally {
				userOptions.animation = animation;
			}
		} catch (e) {}
	}
	return c;
}
applyChartTheme();
applyChartAnimationDefaults();
applyChartLocaleDefault();
try {
	(globalThis as any).Chart?.register?.(entryAnimationPlugin);
} catch (e) {}
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
// 종이에는 라이트 팔레트를 쓴다(main.css 의 @media print 가 [data-theme="dark"] 의 --color-* 를 라이트 값으로 덮는다).
// 그런데 캔버스는 CSS 변수 교체에 반응하지 않는다 - 이미 칠해 둔 픽셀이라 그대로 찍힌다. 실측 2026-09-11(816px, print):
// 다크 테마로 인쇄하면 축·범례 글자가 rgb(240,240,240) 로 흰 종이 대비 1.14 였다(자산성장 1,053px · 배당 5,966px).
// 인쇄 직전에 다시 칠하면 라이트와 같은 rgb(24,24,24)·대비 17.76 이 된다. 끝나면 화면 색으로 되돌린다.
try {
	// 실측 2026-10-02(page.pdf 로 실제 인쇄): beforeprint 때는 아직 print 미디어가 아니다(matchMedia("print") false) - CSS 변수가
	// 다크 값 그대로라 위 방식으로 다시 칠해도 다크 글자색(#e6eaf2)이 흰 종이에 찍혔다(9-11 측정은 print 미디어를 먼저 켠 뒤라 통과했다).
	// 그래서 인쇄 동안만 사이트 테마를 라이트로 바꿔(인쇄 CSS 도 어차피 라이트 팔레트다) 그 색으로 칠하고, 끝나면 되돌린다.
	// PoE 화면(html.poe-theme)은 인게임 다크 고정이라 건드리지 않는다.
	let printRestoreTheme: string | null = null;
	if (typeof globalThis.addEventListener === "function") {
		globalThis.addEventListener("beforeprint", () => {
			const html = document.documentElement;
			if (html && html.getAttribute("data-theme") === "dark" && !(html.classList && html.classList.contains("poe-theme"))) {
				printRestoreTheme = "dark";
				html.setAttribute("data-theme", "light");
			}
			applyChartTheme();
		});
		globalThis.addEventListener("afterprint", () => {
			if (printRestoreTheme) {
				document.documentElement.setAttribute("data-theme", printRestoreTheme);
				printRestoreTheme = null;
			}
			applyChartTheme();
		});
	}
} catch (e) {}
(window as any).__chartThemeInternals = { resolveCssColor, chartTextColor, applyChartTheme , applyChartAnimationDefaults, applyChartLocaleDefault, CHART_ANIMATION_MS };
// Attach to window so templates can use <script src="/js/stock-charts.js"></script>
(window as any).StockCharts = StockCharts;
