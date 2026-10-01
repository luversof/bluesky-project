// 차트의 최고 · 최저 표시 옆 "지금 대비" 글자가 위 요약 카드와 같은 규칙인지.
//
// 실측 2026-10-01(자산 성장 '전체'): 카드는 최저 평가액 대비를 "x4,189" 로 적는데 차트만 "+418794.23%" 로 적었다.
// 카드 규칙(assetGrowthPeriodReturnSummary.jte): |비율| >= 1000% 이면 배수(지금 / 그 지점, 반올림), 아니면 부호 붙은 소수 둘째 자리.
// 그리고 그 글자 뒤에는 바탕색을 깐다 - 매매 표시(▲▼)에 덮여 "최저" 를 못 읽었다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.window.addEventListener = globalThis.window.addEventListener ?? (() => {});
globalThis.document = globalThis.document ?? {
	getElementById: () => null,
	querySelectorAll: () => [],
	addEventListener: () => {},
	documentElement: { lang: "ko" },
};

await import("../../resources/static/js/stock-charts.js");
const SC = globalThis.window.StockCharts;

test("1,000% 이상은 배수로 - 카드와 같은 x4,189", () => {
	// 실측 값: 지금 1,618,858,655 / 최저 386,460
	assert.equal(SC.extremeGapText(1618858655, 386460), "x" + SC.formatNumber(4189));
	assert.match(SC.extremeGapText(1618858655, 386460), /^x4[,.\s ]?189$/);
});

test("경계: 999.99% 는 비율, 1,000% 는 배수", () => {
	assert.equal(SC.extremeGapText(1099.99, 100), "+999.99%");
	assert.equal(SC.extremeGapText(1100, 100), "x" + SC.formatNumber(11));
});

test("최고 대비 하락은 그대로 비율", () => {
	assert.equal(SC.extremeGapText(1618858655, 2098800125), "-22.87%");
});

test("0 에는 부호가 없고, 기준이 0 이면 글자 없음", () => {
	assert.equal(SC.extremeGapText(100, 100), "0.00%");
	assert.equal(SC.extremeGapText(99.999, 100), "0.00%");
	assert.equal(SC.extremeGapText(100, 0), "");
	assert.equal(SC.extremeGapText(NaN, 100), "");
});

test("주석 글자 뒤에 바탕을 깔고 그 위에 글자를 쓴다", () => {
	const calls = [];
	const ctx = new Proxy(
		{ measureText: (s) => ({ width: s.length * 6 }) },
		{
			get(target, key) {
				if (key in target) return target[key];
				return (...args) => calls.push([key, ...args]);
			},
			set(target, key, value) {
				calls.push(["set:" + String(key), value]);
				return true;
			},
		},
	);
	const config = SC.holdingsChartConfig(
		{ labels: ["a", "b", "c"], value: [100, 5, 50000], cost: [], buyCount: [], dailyRealized: [] },
		{ valueLabel: "v", maxLabel: "최고", minLabel: "최저" },
	);
	const plugin = config.plugins.find((p) => p.id === "holdingsRangeExtremes");
	assert.ok(plugin, "최고 · 최저 플러그인");
	const area = { left: 0, right: 1000, top: 0, bottom: 300 };
	plugin.afterDatasetsDraw({
		ctx,
		chartArea: area,
		scales: {
			x: { getPixelForValue: (i) => 100 + i * 300 },
			y: { getPixelForValue: (v) => 280 - v / 200 },
		},
	});
	const texts = calls.filter((c) => c[0] === "fillText").map((c) => c[1]);
	assert.equal(texts.length, 2);
	assert.ok(texts.some((s) => s.includes("최저") && s.includes("(x" + SC.formatNumber(10000) + ")")), texts.join(" | "));
	// 글자마다 바로 앞에 바탕 사각형이 있다
	for (let i = 0; i < calls.length; i++) {
		if (calls[i][0] !== "fillText") continue;
		const before = calls.slice(0, i).map((c) => c[0]);
		assert.ok(before.lastIndexOf("fillRect") > before.lastIndexOf("fillText"), "글자 앞 바탕: " + calls[i][1]);
	}
});
