// 다크 테마로 인쇄할 때 인쇄 동안만 라이트로 바꿔 칠하고 끝나면 되돌리는지(2026-10-02).
//
// 실측(page.pdf 로 실제 인쇄): beforeprint 때는 아직 print 미디어가 아니라 CSS 변수가 다크 값 그대로였다 - 그 색으로 다시 칠하면
// 다크 글자색(#e6eaf2)이 흰 종이에 찍힌다. 아래는 printChartTheme.test.mjs 와 같은 스텁에 data-theme 상태만 더했다.
//
// (옛 머리말) 다크 테마로 인쇄할 때 차트가 흰 종이에서 보이는지.
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
	documentElement: Object.assign(stubEl(), {
		lang: "ko",
		attrs: { "data-theme": "dark" },
		getAttribute(name) { return this.attrs[name] ?? null; },
		setAttribute(name, value) { this.attrs[name] = value; },
	}),
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

/** 살아 있는 차트 하나. CSS 변수는 그때 data-theme 에 따라 다르게 풀린다(라이트 #0d111b / 다크 #e6eaf2). */
globalThis.getComputedStyle = () => ({
	getPropertyValue: () => (document.documentElement.getAttribute("data-theme") === "dark" ? "#e6eaf2" : "#0d111b"),
});
const chart = {
	options: { scales: { y: { ticks: {}, title: {} } }, plugins: { legend: { labels: {} } } },
	data: { datasets: [{ type: "bar" }] },
	config: { type: "bar" },
	update() {},
};
globalThis.Chart = { defaults: { datasets: {} }, instances: { a: chart }, register: noop };

await import("../../resources/static/js/stock-charts.js");

test("다크 화면을 인쇄하면 인쇄 동안 라이트로 바꿔 종이 색으로 칠한다", () => {
	listeners.get("beforeprint").forEach((fn) => fn());
	assert.equal(document.documentElement.getAttribute("data-theme"), "light", "인쇄 동안 라이트로 바꾸지 않았다");
	assert.equal(chart.options.scales.y.ticks.color, "#0d111b", "다크 글자색이 흰 종이에 찍힌다");
});

test("인쇄가 끝나면 원래 다크로 되돌리고 화면 색으로 다시 칠한다", () => {
	listeners.get("afterprint").forEach((fn) => fn());
	assert.equal(document.documentElement.getAttribute("data-theme"), "dark");
	assert.equal(chart.options.scales.y.ticks.color, "#e6eaf2");
});

test("라이트 화면 인쇄는 테마를 건드리지 않는다", () => {
	document.documentElement.setAttribute("data-theme", "light");
	listeners.get("beforeprint").forEach((fn) => fn());
	listeners.get("afterprint").forEach((fn) => fn());
	assert.equal(document.documentElement.getAttribute("data-theme"), "light", "라이트였는데 인쇄 뒤 다크가 됐다");
});
