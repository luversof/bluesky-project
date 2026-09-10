// 포커스로 열리는 드롭다운: 트리거 aria-expanded 가 열림(컨테이너가 포커스를 품음)을 따라가고, Escape 는 blur 로 닫는다.
//
// 실측 2026-09-09(qa/haspopup.cjs): 네비바 로케일·프로필, 상세 '다른 종목/계좌' 트리거 22개에 aria-expanded 가 없었다.
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
globalThis.HTMLElement = class HTMLElement extends globalThis.Element {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: noop, activeElement: null,
});
globalThis.document.activeElement = globalThis.document.body;
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
const mod = globalThis.__dropdownExpandedInternals;

/** .dropdown > trigger + menu item. contains/closest/blur 를 흉내 낸다. */
function dropdown() {
	const attrs = {};
	const dd = { attrs, hasAttribute: (n) => n in attrs, removeAttribute: (n) => { delete attrs[n]; }, contains: (el) => el === trigger || el === item, querySelector: (sel) => (sel.includes('[role="button"][aria-haspopup]') ? trigger : null) };
	const trigger = new globalThis.HTMLElement(); Object.assign(trigger, { a: {}, parentElement: dd, getAttribute(n) { return n in this.a ? this.a[n] : null; }, setAttribute(n, v) { this.a[n] = v; }, closest: (sel) => (sel === ".dropdown" ? dd : null), blurred: 0, blur() { this.blurred++; globalThis.document.activeElement = globalThis.document.body; } });
	const item = new globalThis.HTMLElement(); Object.assign(item, { closest: (sel) => (sel === ".dropdown" ? dd : null), blurred: 0, blur() { this.blurred++; globalThis.document.activeElement = globalThis.document.body; } });
	return { dd, trigger, item };
}
const root = (triggers) => ({ querySelectorAll: (sel) => (sel === '.dropdown > [role="button"][aria-haspopup]' ? triggers : []) });

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __dropdownExpandedInternals 를 노출하지 않는다");
});

test("aria-expanded 는 컨테이너가 포커스를 품는지를 따라간다", () => {
	const { dd, trigger, item } = dropdown();
	globalThis.document.activeElement = globalThis.document.body;
	assert.equal(mod.syncDropdownExpanded(root([trigger])), 1);
	assert.equal(trigger.a["aria-expanded"], "false");
	globalThis.document.activeElement = trigger;
	mod.syncDropdownExpanded(root([trigger]));
	assert.equal(trigger.a["aria-expanded"], "true", "트리거에 포커스 = 열림(focus-within)");
	globalThis.document.activeElement = item;
	mod.syncDropdownExpanded(root([trigger]));
	assert.equal(trigger.a["aria-expanded"], "true", "메뉴 항목에 포커스 = 여전히 열림");
	globalThis.document.activeElement = globalThis.document.body;
	assert.equal(mod.syncDropdownExpanded(root([trigger])), 1);
	assert.equal(trigger.a["aria-expanded"], "false");
	dd.attrs.open = "";
	assert.equal(mod.isDropdownOpen(dd), true, "open 속성도 열림");
});

// 실측 2026-09-10(qa/space-scroll.cjs): 트리거에서 Space 를 누르면 페이지가 437px 스크롤됐다(기본 동작 미차단).
test("Space/Enter: 트리거면 메뉴 첫 항목으로 포커스를 옮긴다, 트리거가 아니면 false", () => {
	const { dd, trigger, item } = dropdown();
	trigger.matches = (sel) => sel === '.dropdown > [role="button"][aria-haspopup]';
	item.focused = 0; item.focus = () => { item.focused++; globalThis.document.activeElement = item; };
	dd.querySelector = (sel) => (sel.includes(".dropdown-content") ? item : sel.includes('[role="button"][aria-haspopup]') ? trigger : null);
	assert.equal(mod.enterDropdownMenu(trigger), true);
	assert.equal(item.focused, 1, "메뉴 첫 항목이 포커스를 받아야 한다");
	const notTrigger = new globalThis.HTMLElement(); notTrigger.matches = () => false;
	assert.equal(mod.enterDropdownMenu(notTrigger), false);
	assert.equal(mod.enterDropdownMenu(null), false);
});

test("Escape: 드롭다운 안의 활성 요소를 blur 해 닫는다, 밖이면 아무 일 없음", () => {
	const { trigger, item } = dropdown();
	globalThis.document.activeElement = item;
	assert.equal(mod.closeDropdownOnEscape(item), true);
	assert.equal(item.blurred, 1);
	assert.equal(globalThis.document.activeElement, globalThis.document.body);
	const outside = new globalThis.HTMLElement(); outside.closest = () => null; outside.blur = () => { outside.blurred = 1; };
	assert.equal(mod.closeDropdownOnEscape(outside), false);
	assert.equal(outside.blurred, undefined, "드롭다운 밖 요소는 건드리지 않는다");
	assert.equal(mod.closeDropdownOnEscape(null), false);
	void trigger;
});
