// htmx 가 걷어내는 요소 안의 canvas 에 붙은 Chart.js 인스턴스를 파괴한다.
//
// 실측 2026-09-09(qa/chart-leak.cjs): 자산성장·배당 화면에서 기간 프리셋 10회 전환 후 DOM 에 없는 canvas 를 쥔 인스턴스 20개가
// Chart.instances 에 남았다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(decorativeSvg.test.mjs 와 같은 방식).
import assert from "node:assert/strict";
import test from "node:test";

const noop = () => {};
const stubEl = () => ({
	addEventListener: noop, removeEventListener: noop, setAttribute: noop, removeAttribute: noop,
	getAttribute: () => null, hasAttribute: () => false, querySelectorAll: () => [], querySelector: () => null,
	closest: () => null, matches: () => false, appendChild: noop, remove: noop,
	classList: { contains: () => false, add: noop, remove: noop, toggle: noop }, dataset: {}, style: {}, children: [],
});
globalThis.Element = class Element {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: noop,
});
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.location = { search: "", href: "https://x/stock", origin: "https://x", pathname: "/stock" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const mod = globalThis.__chartCleanupInternals;

class Fake extends globalThis.Element {
	constructor(tagName, canvases = []) { super(); this.tagName = tagName; this.canvases = canvases; }
	querySelectorAll(sel) { return sel === "canvas" ? this.canvases : []; }
}
/** Chart.js 흉내: getChart 는 등록된 canvas 만 인스턴스를 돌려주고, destroy 는 등록을 지운다. */
function fakeChartLib(bound) {
	const map = new Map(bound.map((c) => [c, { destroyed: 0, destroy() { this.destroyed++; map.delete(c); } }]));
	return { getChart: (c) => map.get(c), map };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __chartCleanupInternals 를 노출하지 않는다");
	assert.equal(typeof mod.destroyChartsIn, "function");
});

test("걷어내는 컨테이너 안의 canvas 에 붙은 차트만 파괴하고 개수를 돌려준다", () => {
	const a = new Fake("CANVAS"), b = new Fake("CANVAS"), untouched = new Fake("CANVAS"), plain = new Fake("CANVAS");
	const lib = fakeChartLib([a, b, untouched]);
	const container = new Fake("DIV", [a, b, plain]);
	assert.equal(mod.destroyChartsIn(container, lib), 2, "차트 없는 canvas(plain)는 세지 않는다");
	assert.equal(lib.map.has(a), false);
	assert.equal(lib.map.has(b), false);
	assert.equal(lib.map.has(untouched), true, "컨테이너 밖 차트는 그대로");
	assert.equal(mod.destroyChartsIn(container, lib), 0, "두 번째는 할 일이 없다");
});

test("canvas 자체가 대상이면 그 차트를 파괴한다 - htmx 는 자식마다 이벤트를 따로 보낸다", () => {
	const c = new Fake("CANVAS");
	const lib = fakeChartLib([c]);
	assert.equal(mod.destroyChartsIn(c, lib), 1);
	assert.equal(lib.map.size, 0);
});

test("Chart.js 가 없거나 대상이 요소가 아니면 아무 일도 하지 않는다", () => {
	const c = new Fake("CANVAS");
	assert.equal(mod.destroyChartsIn(c, undefined), 0, "차트 라이브러리 미로드");
	assert.equal(mod.destroyChartsIn(c, {}), 0, "getChart 없는 구버전");
	assert.equal(mod.destroyChartsIn(null, fakeChartLib([c])), 0);
	assert.equal(mod.destroyChartsIn({}, fakeChartLib([c])), 0, "document/window 대상");
});
