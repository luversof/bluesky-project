// 붙박이 상단바의 실제 높이를 --site-header-height 로 알린다 - 구역 막대 top 과 html scroll-padding-top 이 따라간다(2026-09-17).
//
// 상단바는 폭·글꼴에 따라 줄이 접혀 64~320px 가 되는데 막대는 71px 고정값에 붙어 있어, 375px 기본 글꼴에서 막대 41px 중 29px,
// 글꼴 150~200% 에서는 막대 전부가 상단바 밑에 깔렸다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
import assert from "node:assert/strict";
import test from "node:test";

const noop = () => {};
const stubEl = () => ({
	addEventListener: noop, removeEventListener: noop, setAttribute: noop, removeAttribute: noop,
	getAttribute: () => null, hasAttribute: () => false, querySelectorAll: () => [], querySelector: () => null,
	closest: () => null, matches: () => false, appendChild: noop, remove: noop,
	classList: { contains: () => false, add: noop, remove: noop, toggle: noop }, dataset: {}, style: {}, children: [],
});
const rootStyle = {};
let header = null;
globalThis.Element = class Element {};
globalThis.HTMLElement = class HTMLElement extends globalThis.Element {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR", style: { setProperty: (name, value) => { rootStyle[name] = value; } } }),
	body: stubEl(), head: stubEl(), title: "t",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: noop,
	querySelector: (sel) => (sel === "header.navbar" ? header : null),
});
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
const observers = [];
globalThis.ResizeObserver = class { constructor(fn) { this.fn = fn; this.targets = []; observers.push(this); } observe(t) { this.targets.push(t); } disconnect() {} };
globalThis.location = { search: "", href: "https://x/stock", origin: "https://x", pathname: "/stock" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const mod = globalThis.__stickyHeaderInternals;

function fakeHeader(height) {
	return { height, getBoundingClientRect() { return { top: 0, bottom: this.height, height: this.height, left: 0, right: 375, width: 375 }; } };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __stickyHeaderInternals 를 노출하지 않는다 - npm run build");
	assert.equal(typeof mod.syncSiteHeaderHeight, "function");
	assert.equal(typeof mod.watchSiteHeaderHeight, "function");
});

test("상단바가 없는 화면에서는 아무것도 하지 않는다", () => {
	header = null;
	const before = observers.length;
	mod.watchSiteHeaderHeight();
	assert.equal(rootStyle["--site-header-height"], undefined);
	assert.equal(observers.length, before, "없는 상단바를 지켜보지 않는다");
});

test("실제 높이를 바로 알리고, 줄이 접혀 높이가 바뀌면 다시 알린다", () => {
	header = fakeHeader(100);
	mod.watchSiteHeaderHeight();
	assert.equal(rootStyle["--site-header-height"], "100px", "375px 에서 접힌 높이");
	const observer = observers[observers.length - 1];
	assert.deepEqual(observer.targets, [header], "상단바 크기 변화를 지켜본다");
	header.height = 64;
	observer.fn([]);
	assert.equal(rootStyle["--site-header-height"], "64px", "한 줄로 펴지면 줄어든 높이");
});

test("상단바가 사라져 높이가 0 이면(인쇄 등) 이전 값을 둔다", () => {
	header = fakeHeader(0);
	rootStyle["--site-header-height"] = "71px";
	mod.syncSiteHeaderHeight(header);
	assert.equal(rootStyle["--site-header-height"], "71px");
});
