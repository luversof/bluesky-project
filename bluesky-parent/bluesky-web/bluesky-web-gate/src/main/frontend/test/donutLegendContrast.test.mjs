// 매매 도넛 범례의 손익 부호 퍼센트는 표와 같은 테마 토큰(text-error/text-info)으로 칠한다.
//
// axe 실측 2026-09-09(매매 화면 '실현손익' 도넛): 인라인 rgba(239,68,68,.9) + opacity .75 가 라이트에서 2.52:1 (범례 7개 전부).
// 빌드 산출물(stock-charts.js)을 최소 스텁으로 띄운다(stockChartsMessages.test.mjs 와 같은 방식).
import assert from "node:assert/strict";
import test from "node:test";

const nodes = {};
function el(id, extra = {}) {
	return (nodes[id] = { id, style: {}, dataset: {}, textContent: "", innerHTML: "", children: [], className: "", replaceChildren(...k) { this.children = k; }, getContext: () => ({}), ...extra });
}
el("app-config", { dataset: {} });
el("donutChart");
el("donutLegend");
el("donutTitle");
globalThis.document = { getElementById: (id) => nodes[id] || null, createElement: () => ({ className: "", textContent: "", style: {}, dataset: {} }), body: { dataset: {} }, documentElement: { lang: "ko" } };
globalThis.window = globalThis;
// 캔버스 차트 생성은 Chart 스텁으로 흉내 낸다(범례는 순수 DOM 문자열).
globalThis.Chart = class { constructor() { this.destroyed = false; } destroy() { this.destroyed = true; } static getChart() { return null; } };
globalThis.Chart.defaults = {}; globalThis.Chart.instances = {};

await import("../../resources/static/js/stock-charts.js");
const Charts = globalThis.window.StockCharts ?? globalThis.StockCharts;

const trades = [
	{ type: "SELL", stockItem: "이익종목", account: "a", profit: 120000, amount: 0 },
	{ type: "SELL", stockItem: "손실종목", account: "a", profit: -30000, amount: 0 },
	{ type: "BUY", stockItem: "매수종목", account: "a", profit: 0, amount: 500000 },
];

test("실현손익 범례: 이익은 text-error, 손실은 text-info, 인라인 rgba/opacity 없음", () => {
	Charts.initDonutFromData(trades, { metric: "profit", groupBy: "stock", canvasId: "donutChart", legendId: "donutLegend", titleId: "donutTitle" });
	const html = nodes.donutLegend.innerHTML;
	assert.match(html, /class="shrink-0 text-error">▲ /, "이익 부호 색이 테마 토큰이 아니다");
	assert.match(html, /class="shrink-0 text-info">▼ /, "손실 부호 색이 테마 토큰이 아니다");
	assert.doesNotMatch(html, /rgba\(239,68,68,0\.9\)|rgba\(59,130,246,0\.9\)|opacity:0\.75/, "라이트에서 2.52:1 이던 인라인 색이 남아 있다");
});

test("매수 집중 범례: 부호 없이 opacity-75 클래스로 눌러 쓴다", () => {
	Charts.initDonutFromData(trades, { metric: "buy", groupBy: "stock", canvasId: "donutChart", legendId: "donutLegend", titleId: "donutTitle" });
	const html = nodes.donutLegend.innerHTML;
	assert.match(html, /class="shrink-0 opacity-75">100\.0%/);
	assert.doesNotMatch(html, /text-error|text-info/);
});
