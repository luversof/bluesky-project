// 패널/보기 전환으로 드러난 표 래퍼(.overflow-x-auto)도 넘치면 tabindex="0" 을 받는다.
//
// axe 실측 2026-09-09(375px, 양 테마): 자산성장 '연도별 성과' 패널이 scrollable-region-focusable 에 걸렸다 - 로드 때 hidden 이라
// scrollWidth 가 0 이었고, 전환 뒤엔 다시 재지 않았다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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
const mod = globalThis.__scrollFocusInternals;

/** 표 래퍼 흉내: scrollWidth/clientWidth 와 속성 맵. */
function wrapper(scrollWidth, clientWidth) {
	const attrs = {};
	return { scrollWidth, clientWidth, dataset: {}, attrs, hasAttribute: (n) => n in attrs, setAttribute: (n, v) => { attrs[n] = v; }, removeAttribute: (n) => { delete attrs[n]; } };
}
/** 패널 흉내: data-panel 이름, hidden 클래스 집합, 안의 래퍼들. */
function panel(name, wrappers) {
	const cls = new Set(["hidden"]);
	return { name, cls, wrappers, getAttribute: (a) => (a === "data-panel" ? name : null), classList: { toggle: (c, on) => { on ? cls.add(c) : cls.delete(c); }, contains: (c) => cls.has(c) }, querySelectorAll: (sel) => (sel === ".overflow-x-auto" ? wrappers : []) };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __scrollFocusInternals 를 노출하지 않는다");
	assert.equal(typeof mod.syncScrollableFocus, "function");
	assert.equal(typeof mod.applyPanelTabs, "function");
});

test("넘치는 래퍼만 tabindex=0, 더 이상 안 넘치면 우리가 준 것만 거둔다", () => {
	const wide = wrapper(900, 340), narrow = wrapper(300, 340), own = wrapper(300, 340);
	own.attrs.tabindex = "0"; // 템플릿이 직접 준 tabindex 는 건드리지 않는다
	const root = { querySelectorAll: (sel) => (sel === ".overflow-x-auto" ? [wide, narrow, own] : []) };
	mod.syncScrollableFocus(root);
	assert.equal(wide.attrs.tabindex, "0");
	assert.equal(wide.dataset.scrollFocus, "true");
	assert.equal(narrow.attrs.tabindex, undefined);
	assert.equal(own.attrs.tabindex, "0");
	wide.scrollWidth = 300;
	mod.syncScrollableFocus(root);
	assert.equal(wide.attrs.tabindex, undefined, "창이 넓어져 안 넘치면 탭 정지를 거둔다");
	assert.equal(own.attrs.tabindex, "0", "직접 준 것은 그대로");
});

test("패널 전환: 보이게 된 패널의 래퍼를 다시 재고, 여전히 숨은 패널은 손대지 않는다", () => {
	const yearlyWrap = wrapper(900, 340), costWrap = wrapper(900, 0);
	const yearly = panel("yearly", [yearlyWrap]), cost = panel("cost", [costWrap]);
	const root = { querySelectorAll: (sel) => (sel.includes("[data-panel-tab]") ? [] : sel.includes('[data-panel-group="g"]') ? [yearly, cost] : []) };
	mod.applyPanelTabs(root, "g", "yearly");
	assert.equal(yearly.cls.has("hidden"), false);
	assert.equal(cost.cls.has("hidden"), true);
	assert.equal(yearlyWrap.attrs.tabindex, "0", "드러난 패널의 넘치는 표는 키보드로 스크롤할 수 있어야 한다");
	assert.equal(costWrap.attrs.tabindex, undefined, "숨은 패널은 폭이 0 이라 재면 틀린 답이 나온다 - 재지 않는다");
});
