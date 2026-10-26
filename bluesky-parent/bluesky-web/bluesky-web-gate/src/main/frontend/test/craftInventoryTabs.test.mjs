// 보관함 탭(2026-10-23 Q22, craftofexile 'storage tabs') — 탭 목록 · 탭별 아이템 · 탭 지우기(아이템은 기본 탭으로).
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(famFilter.test.mjs 와 같은 방식).
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
globalThis.location = { search: "", href: "https://x/poe/craft", origin: "https://x", pathname: "/poe/craft" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const { craftInvTabs, craftInvDropTab, craftInvInTab, craftInvTabName, CRAFT_INV_TAB_MAX } = globalThis.__craftInvInternals;

const it = (url, tab) => ({ name: url, base: "", rarity: "RARE", url, at: 0, tab });

test("탭 목록 = 기본 먼저 · 저장한 이름 순 · 아이템에만 있는 이름은 뒤에, 중복 없이", () => {
	const items = [it("a", "무기"), it("b", ""), it("c", undefined), it("d", "갑옷")];
	assert.deepEqual(craftInvTabs(items, ["갑옷", "반지"]), ["", "갑옷", "반지", "무기"]);
	assert.deepEqual(craftInvTabs([], []), [""]);
});

test("탭 이름 = 공백 정리 · 20자", () => {
	assert.equal(craftInvTabName("  긴   이름  "), "긴 이름");
	assert.equal(craftInvTabName("x".repeat(30)).length, 20);
	assert.equal(craftInvTabName(undefined), "");
	assert.equal(craftInvTabName(null), "");
});

test(`탭은 기본 포함 ${CRAFT_INV_TAB_MAX}개까지`, () => {
	const saved = Array.from({ length: 20 }, (_, i) => "t" + i);
	assert.equal(craftInvTabs([], saved).length, CRAFT_INV_TAB_MAX);
});

test("탭별 아이템 — 탭 없는 아이템 · 목록 밖 이름은 기본 탭", () => {
	const items = [it("a", "무기"), it("b", ""), it("c", undefined), it("d", "없는탭")];
	const tabs = ["", "무기"];
	assert.deepEqual(craftInvInTab(items, tabs, "").map((x) => x.url), ["b", "c", "d"]);
	assert.deepEqual(craftInvInTab(items, tabs, "무기").map((x) => x.url), ["a"]);
	// 자가검사: 탭을 하나 빼면 그 아이템은 기본 탭으로 보인다
	assert.deepEqual(craftInvInTab(items, [""], "").map((x) => x.url), ["a", "b", "c", "d"]);
});

test("탭 지우기 — 그 탭 아이템은 기본 탭으로, 다른 탭 · 개수는 그대로(데이터 안 지움)", () => {
	const items = [it("a", "무기"), it("b", "갑옷"), it("c", "무기")];
	const out = craftInvDropTab(items, "무기");
	assert.equal(out.length, 3);
	assert.deepEqual(out.map((x) => x.tab), ["", "갑옷", ""]);
	assert.equal(items[0].tab, "무기", "원본은 그대로");
});
