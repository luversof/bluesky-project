// 바꾸면 초기화되는 고르기 폼은 만들던 게 있을 때만 확인(2026-10-18 C442 — PoE2 제작 UX QA: 아이템 레벨을 바꾸면 만들던 아이템이 말없이 사라졌다).
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(swapAnchor.test.mjs 와 같은 방식).
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
	activeElement: null,
});
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.location = { search: "", href: "https://x/poe2/craft", origin: "https://x", pathname: "/poe2/craft" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const { isDirtyValues } = globalThis.__confirmDirtyInternals;

test("빈 값 · 빈 배열 · 빈 객체만 있으면 묻지 않는다", () => {
	assert.equal(isDirtyValues([]), false);
	assert.equal(isDirtyValues([""]), false);
	assert.equal(isDirtyValues(["[]", "{}", ""]), false);
});

test("되돌리기 이력이 하나라도 있으면 묻는다", () => {
	assert.equal(isDirtyValues(['[{"poolKey":"Gloves|Armour"}]']), true);
	assert.equal(isDirtyValues(["[]", "x"]), true);
});
