// 패널 탭의 기본값: 저장된 선택 > 화면이 알려준 기본(data-panel-tab-default) > 첫 탭.
//
// 실측 2026-09-10: 배당 '기간별 집계' 는 3개월을 고르면 연도별이 한 줄뿐이라 아무것도 말해 주지 않는다.
// 화면이 그럴 때 월별을 기본으로 알려 줄 수 있어야 한다(사용자가 직접 고른 탭이 있으면 그것이 우선).
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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
let stored = {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: noop, createTreeWalker: () => ({ nextNode: () => null }),
});
globalThis.NodeFilter = { SHOW_TEXT: 4 };
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.location = { search: "", href: "https://x/stock", origin: "https://x", pathname: "/stock" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: (k) => (k in stored ? stored[k] : null), setItem: (k, v) => { stored[k] = v; }, removeItem: (k) => { delete stored[k]; } };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const mod = globalThis.__scrollFocusInternals;

/** 탭 바 하나와 그에 딸린 패널들. applyPanelTabs 가 document 에서 다시 찾으므로 document 질의도 흉내 낸다. */
function scene({ tabDefault = null, tabs = ["year", "month"] } = {}) {
	const made = tabs.map((name) => {
		const el = { name, attrs: { "data-panel-tab": name }, classes: new Set(["text-base-content/60"]),
			getAttribute(n) { return n in this.attrs ? this.attrs[n] : null; },
			classList: { add: (c) => el.classes.add(c), remove: (c) => el.classes.delete(c), contains: (c) => el.classes.has(c), toggle: (c, on) => (on ? el.classes.add(c) : el.classes.delete(c)) } };
		return el;
	});
	const panels = tabs.map((name) => {
		const el = { name, hidden: null, attrs: { "data-panel": name, "data-panel-group": "g" },
			getAttribute(n) { return n in this.attrs ? this.attrs[n] : null; },
			querySelectorAll: () => [], classList: { toggle: (c, on) => { if (c === "hidden") el.hidden = on; } } };
		return el;
	});
	const bar = { attrs: { "data-panel-tab-group": "g", ...(tabDefault ? { "data-panel-tab-default": tabDefault } : {}) },
		getAttribute(n) { return n in this.attrs ? this.attrs[n] : null; },
		querySelectorAll: (sel) => (sel === "[data-panel-tab]" ? made : []) };
	globalThis.document.querySelectorAll = (sel) => {
		if (sel === "[data-panel-tab-group]") return [bar];
		if (sel === '[data-panel-tab-group="g"] [data-panel-tab]') return made;
		if (sel === '[data-panel-group="g"]') return panels;
		return [];
	};
	return { bar, tabs: made, panels, shown: () => panels.filter((p) => p.hidden === false).map((p) => p.name) };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod && typeof mod.restorePanelTabs === "function", "common.js 가 restorePanelTabs 를 노출하지 않는다");
});

test("기본값을 알려주지 않으면 첫 탭", () => {
	stored = {};
	const s = scene();
	mod.restorePanelTabs();
	assert.deepEqual(s.shown(), ["year"]);
});

test("화면이 알려준 기본 탭을 쓴다(연도별이 한 줄일 때 월별)", () => {
	stored = {};
	const s = scene({ tabDefault: "month" });
	mod.restorePanelTabs();
	assert.deepEqual(s.shown(), ["month"]);
});

test("사용자가 고른 탭이 화면 기본값보다 우선한다", () => {
	stored = { "panel-tab:g": "year" };
	const s = scene({ tabDefault: "month" });
	mod.restorePanelTabs();
	assert.deepEqual(s.shown(), ["year"]);
});

test("알 수 없는 기본값·저장값은 무시하고 첫 탭으로 떨어진다", () => {
	stored = { "panel-tab:g": "nope" };
	const s = scene({ tabDefault: "also-nope" });
	mod.restorePanelTabs();
	assert.deepEqual(s.shown(), ["year"]);
});
