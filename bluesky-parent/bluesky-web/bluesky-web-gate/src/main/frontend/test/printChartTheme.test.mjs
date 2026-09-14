// 다크 테마로 인쇄할 때 차트가 흰 종이에서 보이는지.
//
// main.css 의 @media print 는 [data-theme="dark"] 의 --color-* 를 라이트 값으로 덮는다. 그런데 캔버스는
// CSS 변수 교체에 반응하지 않는다 - 이미 칠해 둔 픽셀이라 그대로 찍힌다. 실측 2026-09-11(816px, print 미디어,
// 자산성장 화면): 다크 테마로 인쇄하면 축·범례 글자가 rgb(240,240,240) 로 흰 종이 대비 1.14 였다
// (assetGrowthChart 1,053px · dividendChart 5,966px). 인쇄 직전에 applyChartTheme() 를 다시 부르면
// 라이트와 같은 rgb(24,24,24)·대비 17.76 이 된다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

const listeners = new Map();
const noop = () => {};
const stubEl = () => ({
	addEventListener: noop, removeEventListener: noop, setAttribute: noop, removeAttribute: noop,
	getAttribute: () => null, hasAttribute: () => false, querySelectorAll: () => [], querySelector: () => null,
	closest: () => null, matches: () => false, appendChild: noop, remove: noop,
	classList: { contains: () => false, add: noop, remove: noop, toggle: noop }, dataset: {}, style: {}, children: [],
});

let resolvedVar = "#0d111b";
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko" }),
	body: stubEl(), head: stubEl(), readyState: "complete",
	getElementById: () => null,
	createElement: () => ({ width: 0, height: 0, getContext: () => null, style: {} }),
	addEventListener: noop,
});
globalThis.getComputedStyle = () => ({ getPropertyValue: () => resolvedVar });
globalThis.window = globalThis;
globalThis.addEventListener = (type, fn) => {
	if (!listeners.has(type)) listeners.set(type, []);
	listeners.get(type).push(fn);
};
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.location = { search: "", pathname: "/stock", href: "https://x/stock", origin: "https://x" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);
globalThis.NodeFilter = { SHOW_TEXT: 4 };

/** 살아 있는 차트 하나를 흉내 낸다. applyChartTheme 이 여기에 색을 써 넣는지 본다. */
const chart = {
	options: { scales: { y: { ticks: {}, title: {} } }, plugins: { legend: { labels: {} } } },
	data: { datasets: [{ type: "bar" }] },
	config: { type: "bar" },
	updated: 0,
	update() { this.updated++; },
};
globalThis.Chart = { defaults: { datasets: {} }, instances: { a: chart }, register: noop };

await import("../../resources/static/js/stock-charts.js");

test("인쇄 시작·종료에 차트 색을 다시 칠한다", () => {
	assert.ok(listeners.get("beforeprint")?.length, "beforeprint 를 듣지 않는다 - 다크 인쇄에서 차트가 안 보인다");
	assert.ok(listeners.get("afterprint")?.length, "afterprint 가 없으면 인쇄 뒤 화면 색이 돌아오지 않는다");
});

test("beforeprint 가 그때의 CSS 변수 색으로 다시 칠한다", () => {
	const before = chart.updated;
	resolvedVar = "#0d111b"; // 인쇄용 라이트 본문색
	listeners.get("beforeprint").forEach((fn) => fn());

	assert.ok(chart.updated > before, "살아 있는 차트를 다시 그리지 않았다");
	// 이 스텁에는 2d 캔버스가 없어 chartTextColor 가 rgba 로 못 바꾸고 변수 값을 그대로 돌려준다.
	// 여기서 볼 것은 "인쇄 시점의 CSS 변수를 다시 읽어 차트에 써 넣는가" 다.
	assert.equal(chart.options.scales.y.ticks.color, "#0d111b");
	assert.equal(chart.options.plugins.legend.labels.color, "#0d111b");
});

test("afterprint 는 그 시점 색(화면 색)으로 되돌린다", () => {
	resolvedVar = "#e6eaf2"; // 다크 화면 본문색
	listeners.get("afterprint").forEach((fn) => fn());

	assert.equal(chart.options.scales.y.ticks.color, "#e6eaf2");
});
