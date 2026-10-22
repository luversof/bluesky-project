// 고른 화폐 기준 옵션 표(2026-10-22 P3) — 다시 그릴지 정하는 열쇠: 고른 화폐 + 그 화폐의 고르기 값(에센스 · 화석 · 제작대 · 수확)만.
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
const store = new Map();
globalThis.sessionStorage = { getItem: (k) => (store.has(k) ? store.get(k) : null), setItem: (k, v) => store.set(k, String(v)), removeItem: (k) => store.delete(k) };
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const { craftPoolKey } = globalThis.__craftPoolInternals;

test("고른 화폐가 없으면 빈 열쇠(지금 아이템 기준 표)", () => {
	assert.equal(craftPoolKey(null, [["essence", "x"]]), "");
	assert.equal(craftPoolKey("", []), "");
});

test("고르기 값 넷만 열쇠에 — 상태 · 이력 같은 다른 칸은 무시", () => {
	const a = craftPoolKey("FOSSIL", [["state", "{...}"], ["fossil", "A"], ["fossil", "B"], ["history", "[]"]]);
	const b = craftPoolKey("FOSSIL", [["state", "{다른}"], ["fossil", "A"], ["fossil", "B"]]);
	assert.equal(a, b);
	assert.equal(a, "FOSSIL|fossil=A&fossil=B");
});

test("화폐나 고르기 값이 바뀌면 열쇠가 바뀐다", () => {
	const base = craftPoolKey("ESSENCE", [["essence", "Greed"]]);
	assert.notEqual(base, craftPoolKey("ESSENCE", [["essence", "Hatred"]]));
	assert.notEqual(base, craftPoolKey("CHAOS", [["essence", "Greed"]]));
	assert.notEqual(craftPoolKey("FOSSIL", [["fossil", "A"]]), craftPoolKey("FOSSIL", [["fossil", "A"], ["fossil", "B"]]));
});
