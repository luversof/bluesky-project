// 가로로 스크롤하는 탭 줄(.tabs-scroll)은 지금 탭이 줄 밖에 있으면 줄만 옆으로 옮겨 보이게 한다(2026-09-17).
//
// 큰 글꼴 폰 폭에서 탭이 글자 폭 밑으로 줄지 않고 줄이 스크롤하게 바꾸자, 시뮬레이터 '적립식 복리' 처럼 뒤쪽 탭에 있으면 줄 첫머리만
// 보였다. 페이지는 움직이면 안 된다 - scrollIntoView 대신 줄의 scrollLeft 만 계산한다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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
const mod = globalThis.__tabScrollInternals;

/** 탭 줄 흉내: 폭 · 내용 폭 · 지금 탭 위치. */
function bar(clientWidth, scrollWidth, activeLeft, activeWidth, scrollLeft = 0) {
	const active = activeLeft == null ? null : { offsetLeft: activeLeft, offsetWidth: activeWidth };
	return { clientWidth, scrollWidth, scrollLeft, querySelector: (sel) => (sel === ".tab-active" ? active : null) };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __tabScrollInternals 를 노출하지 않는다 - npm run build");
	assert.equal(typeof mod.tabScrollLeftFor, "function");
	assert.equal(typeof mod.revealActiveTabs, "function");
});

test("보이는 탭이면 그대로, 오른쪽 밖이면 오른끝을 맞추고, 왼쪽 밖이면 왼끝을 맞춘다", () => {
	assert.equal(mod.tabScrollLeftFor(0, 300, 40, 100), 0);
	assert.equal(mod.tabScrollLeftFor(0, 300, 380, 120), 200, "380 + 120 - 300");
	assert.equal(mod.tabScrollLeftFor(250, 300, 100, 90), 100);
	assert.equal(mod.tabScrollLeftFor(100, 300, 100, 300), 100, "딱 맞으면 그대로");
});

test("넘치는 탭 줄만 옮기고, 안 넘치거나 지금 탭이 없으면 건드리지 않는다", () => {
	const wide = bar(311, 520, 380, 130);
	const fits = bar(704, 704, 470, 230);
	const noActive = bar(311, 520, null, 0);
	const root = { querySelectorAll: (sel) => (sel === ".tabs-scroll" ? [wide, fits, noActive] : []) };
	mod.revealActiveTabs(root);
	assert.equal(wide.scrollLeft, 199, "380 + 130 - 311");
	assert.equal(fits.scrollLeft, 0);
	assert.equal(noActive.scrollLeft, 0);
});
