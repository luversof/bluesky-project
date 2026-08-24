// 매매 화면 차트의 문구가 코드에 박혀 있지 않고 화면 로케일을 따르는지 검증한다.
//
// 레이아웃(#app-config)은 이미 data-stock-chart-title-* / data-stock-chart-message-* 로
// 번역된 문구를 실어 보낸다. 그런데 JS 는 그중 매수/매도 라벨만 읽고, 차트 제목과 빈 상태 문구는
// 한글을 직접 이어 붙이고 있었다 - 영어 화면에도 "종목별 손익 기여", "해당 기간 매도 내역이 없습니다."
// 가 그대로 나갔다. 번들에는 영어 문구가 이미 있었으므로 읽지 않은 것이 원인이었다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

/** #app-config 와 캔버스/범례/제목 자리를 최소한으로 흉내 낸다. */
function makeDom(configDataset) {
	const nodes = {};
	function el(id, extra = {}) {
		return (nodes[id] = {
			id,
			style: {},
			dataset: {},
			textContent: "",
			innerHTML: "",
			children: [],
			className: "",
			replaceChildren(...kids) {
				this.children = kids;
			},
			getContext: () => ({}),
			...extra,
		});
	}
	el("app-config", { dataset: configDataset });
	el("donutChart");
	el("donutLegend");
	el("donutTitle");
	globalThis.document = {
		getElementById: (id) => nodes[id] || null,
		createElement: () => ({
			className: "",
			textContent: "",
			style: {},
			dataset: {},
		}),
		body: { dataset: {} },
		documentElement: { lang: "en" },
	};
	// 이 모듈은 window 에 붙는다(classic <script> 로 로드되기 때문).
	globalThis.window = globalThis.window ?? globalThis;
	return nodes;
}

const EN = {
	stockChartTitleProfitContribution: "Profit Contribution",
	stockChartTitleBuyConcentration: "Buy Concentration",
	stockChartMessageNoSellData: "No sell trades in this period.",
	stockChartMessageNoBuyData: "No buy trades in this period.",
};

makeDom({});
await import("../../resources/static/js/stock-charts.js");
const Charts = globalThis.window.StockCharts ?? globalThis.StockCharts;

test("차트 모듈이 전역에 붙는다", () => {
	assert.ok(Charts, "StockCharts 전역이 없다");
	assert.equal(typeof Charts.initDonutFromData, "function");
});

test("빈 기간 안내가 화면 문구를 따른다", () => {
	const nodes = makeDom(EN);
	Charts.initDonutFromData([], {
		metric: "profit",
		groupBy: "stock",
		canvasId: "donutChart",
		legendId: "donutLegend",
		titleId: "donutTitle",
	});
	const legend = nodes.donutLegend;
	const text = legend.children.map((c) => c.textContent).join("");
	assert.equal(text, EN.stockChartMessageNoSellData);
	assert.ok(!/[가-힣]/.test(text), `영어 화면에 한글이 남아 있다: ${text}`);
});

test("매수 모드의 빈 기간 안내도 화면 문구를 따른다", () => {
	const nodes = makeDom(EN);
	Charts.initDonutFromData([], {
		metric: "buy",
		groupBy: "stock",
		canvasId: "donutChart",
		legendId: "donutLegend",
		titleId: "donutTitle",
	});
	const text = nodes.donutLegend.children.map((c) => c.textContent).join("");
	assert.equal(text, EN.stockChartMessageNoBuyData);
});

test("문구가 없으면 영어 기본값으로 떨어진다(한글이 새지 않는다)", () => {
	const nodes = makeDom({});
	Charts.initDonutFromData([], {
		metric: "profit",
		groupBy: "stock",
		canvasId: "donutChart",
		legendId: "donutLegend",
		titleId: "donutTitle",
	});
	const text = nodes.donutLegend.children.map((c) => c.textContent).join("");
	assert.ok(!/[가-힣]/.test(text), `기본값에 한글이 박혀 있다: ${text}`);
});

test("소스에 화면 문구가 직접 박혀 있지 않다", async () => {
	const { readFile } = await import("node:fs/promises");
	const source = await readFile(
		new URL("../src/stock-charts.ts", import.meta.url),
		"utf8",
	);
	// 주석은 한글이어도 된다. 코드에서 쓰이는 문자열 리터럴만 본다.
	const code = source
		.replace(/\/\*[\s\S]*?\*\//g, " ")
		.replace(/\/\/[^\n]*/g, " ");
	for (const banned of [
		"종목별 손익 기여",
		"매수 집중도",
		"매도 내역",
		"매수 내역",
	]) {
		assert.ok(
			!code.includes(banned),
			`"${banned}" 가 코드에 박혀 있다 - #app-config 의 문구를 쓸 것`,
		);
	}
});

// 코드가 읽는 문구 키마다 레이아웃이 그 값을 실어 보내는지.
//
// appMessage(key, fallback) 는 #app-config 의 dataset 에서 key 를 찾고, 없으면 <b>영어 기본값</b>으로
// 떨어진다. 즉 키를 새로 쓰면서 레이아웃에 data-* 를 넣지 않으면 한국어 화면에도 영어가 그대로 나가는데,
// 화면은 멀쩡해 보이고 아무 오류도 나지 않는다.
//
// 위 검사들은 정해진 키 몇 개의 동작만 본다. 여기서는 <b>키 목록 자체</b>를 맞춘다.
// 실측 2026-08-24: 코드가 읽는 키 6 개가 모두 defaultLayout.jte 의 #app-config 에 있다.
test("appMessage 로 읽는 키마다 레이아웃에 data 속성이 있다", async () => {
	const { readFile } = await import("node:fs/promises");
	const source = await readFile(
		new URL("../src/stock-charts.ts", import.meta.url),
		"utf8",
	);
	const layout = await readFile(
		new URL("../../jte/_layout/defaultLayout.jte", import.meta.url),
		"utf8",
	);

	const keys = [
		...new Set(
			[...source.matchAll(/appMessage[(]"([A-Za-z0-9]+)"/g)].map((m) => m[1]),
		),
	];
	// 스캔이 조용히 0 건이 되면 이 검사는 늘 통과한다.
	assert.ok(keys.length >= 4, `appMessage 키를 찾지 못했다: ${keys.length}`);

	// dataset 의 camelCase 는 data-kebab-case 에서 온다.
	const toAttribute = (key) =>
		"data-" + key.replace(/[A-Z]/g, (c) => "-" + c.toLowerCase());

	const missing = keys.filter((key) => !layout.includes(toAttribute(key) + "="));
	assert.deepEqual(
		missing,
		[],
		"레이아웃이 이 문구를 실어 보내지 않는다 - 한국어 화면에도 영어 기본값이 나간다: "
			+ missing.map(toAttribute).join(", "),
	);
});
