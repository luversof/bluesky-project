// Escape 가 열린 툴팁을 닫고(tooltip-dismissed), 포인터가 떠나거나 포커스가 빠지면 되돌린다(WCAG 1.4.13 dismissable).
//
// 실측 2026-09-09: 4/4 툴팁이 Escape 에 안 닫혔다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(decorativeSvg.test.mjs 와 같은 방식).
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
const mod = globalThis.__tooltipInternals;

/** 클래스 집합으로 요소 흉내. Element 를 상속해 instanceof 가 통하게 한다. */
class Fake extends globalThis.Element {
	constructor(classes) { super(); this.set = new Set(classes); }
	get classList() {
		const set = this.set;
		return { contains: (c) => set.has(c), add: (c) => set.add(c), remove: (c) => set.delete(c) };
	}
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __tooltipInternals 를 노출하지 않는다");
	assert.equal(mod.TOOLTIP_DISMISSED, "tooltip-dismissed");
});

test("열린 툴팁에만 tooltip-dismissed 를 붙이고 개수를 돌려준다", () => {
	const open = new Fake(["tooltip"]), already = new Fake(["tooltip", "tooltip-dismissed"]);
	const root = { querySelectorAll: (sel) => (sel.includes(":hover") && sel.includes(":focus-visible") ? [open, already] : []) };
	assert.equal(mod.dismissOpenTooltips(root), 1, "이미 닫힌 것은 세지 않는다");
	assert.ok(open.classList.contains("tooltip-dismissed"));
	assert.equal(mod.dismissOpenTooltips(root), 0);
});

test("포인터가 떠나거나 포커스가 빠진 툴팁은 되돌린다 - 툴팁이 아닌 대상은 무시", () => {
	const t = new Fake(["tooltip", "tooltip-dismissed"]);
	assert.equal(mod.restoreTooltip(t), true);
	assert.equal(t.classList.contains("tooltip-dismissed"), false);
	assert.equal(mod.restoreTooltip(new Fake(["btn", "tooltip-dismissed"])), false, "tooltip 클래스가 없으면 손대지 않는다");
	assert.equal(mod.restoreTooltip(null), false);
	assert.equal(mod.restoreTooltip({}), false, "Element 가 아닌 대상(document/window)");
});
