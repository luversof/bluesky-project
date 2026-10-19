// 구역을 바꿔 끼워도 열어 둔 details 를 다시 열고, 오류가 고정 헤더 밑이면 보이게(2026-10-18 C440 — PoE2 제작 UX QA).
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
const { rememberOpenDetails, restoreOpenDetails, needsReveal } = globalThis.__keepOpenInternals;

const det = (key, open) => ({ open, getAttribute: (n) => (n === "data-craft-group" ? key : null) });
const root = (id, attr, ds) =>
	Object.assign(new globalThis.Element(), {
		id,
		getAttribute: (n) => (n === "data-keep-open" ? attr : null),
		querySelectorAll: () => ds,
	});

test("교체 직전 열린 details 의 키를 적어 둔다", () => {
	const memo = rememberOpenDetails(root("craft2-emu", "data-craft-group", [det("essence", true), det("bone", false), det("omen", true)]));
	assert.deepEqual(memo, { id: "craft2-emu", attr: "data-craft-group", keys: ["essence", "omen"] });
});

test("표시 · id 가 없으면 아무것도 안 한다", () => {
	assert.equal(rememberOpenDetails(root("craft2-emu", null, [det("essence", true)])), null);
	assert.equal(rememberOpenDetails(root("", "data-craft-group", [det("essence", true)])), null);
	assert.equal(rememberOpenDetails({}), null);
});

test("새 대상에서 적어 둔 키만 연다 — 서버가 연 것 · 다른 키는 그대로", () => {
	const ds = [det("essence", false), det("bone", false), det("omen", true)];
	const fresh = { querySelectorAll: () => ds };
	const n = restoreOpenDetails({ id: "craft2-emu", attr: "data-craft-group", keys: ["essence", "omen"] }, { getElementById: (id) => (id === "craft2-emu" ? fresh : null) });
	assert.equal(n, 1);
	assert.deepEqual(ds.map((d) => d.open), [true, false, true]);
});

test("새 대상이 없거나 적어 둔 키가 없으면 0", () => {
	assert.equal(restoreOpenDetails({ id: "gone", attr: "data-craft-group", keys: ["essence"] }, { getElementById: () => null }), 0);
	assert.equal(restoreOpenDetails({ id: "craft2-emu", attr: "data-craft-group", keys: [] }, { getElementById: () => ({ querySelectorAll: () => [det("essence", false)] }) }), 0);
	assert.equal(restoreOpenDetails(null), 0);
});

test("오류가 헤더(scroll-margin) 밑이거나 화면 아래로 넘치면 보이게 한다", () => {
	assert.equal(needsReveal({ top: 40, bottom: 80 }, 96, 844), true); // 헤더 밑
	assert.equal(needsReveal({ top: 300, bottom: 340 }, 96, 844), false); // 보임
	assert.equal(needsReveal({ top: 820, bottom: 900 }, 96, 844), true); // 아래로 넘침
	assert.equal(needsReveal({ top: -200, bottom: -160 }, 96, 844), true); // 위로 지나감
});
