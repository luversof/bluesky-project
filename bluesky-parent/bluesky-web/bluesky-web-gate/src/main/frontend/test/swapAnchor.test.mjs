// 구역 교체 때 기준 요소를 화면의 같은 자리에 둔다(2026-10-13: 시뮬레이터 지급 시기 칩이 전체 <-> 월중 · 월말에서 위치가 달라졌다).
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(swapFocus.test.mjs 와 같은 방식).
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
globalThis.location = { search: "", href: "https://x/stock", origin: "https://x", pathname: "/stock" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const { rememberSwapAnchor, restoreSwapAnchor } = globalThis.__swapAnchorInternals;

const at = (top) => ({ getBoundingClientRect: () => ({ top }) });
const target = (selector) => Object.assign(new globalThis.Element(), { getAttribute: (n) => (n === "data-swap-anchor" ? selector : null) });

test("교체 직전 기준 요소의 화면 위치를 재 둔다", () => {
	const memo = rememberSwapAnchor(target("#table"), { querySelector: (s) => (s === "#table" ? at(240) : null) });
	assert.deepEqual(memo, { selector: "#table", top: 240 });
});

test("표시가 없거나 기준 요소가 없으면 아무것도 안 한다", () => {
	assert.equal(rememberSwapAnchor(target(null), { querySelector: () => at(1) }), null);
	assert.equal(rememberSwapAnchor(target("#gone"), { querySelector: () => null }), null);
	assert.equal(rememberSwapAnchor({}, { querySelector: () => at(1) }), null);
});

test("위 카드가 줄어 기준 요소가 올라가면 그만큼 되굴려 같은 자리에 둔다", () => {
	const calls = [];
	// 교체 전 240px 에 있던 표 제목이, 위 추천 카드가 180px 줄어 60px 에 왔다 - 180px 위로 굴려야 한다.
	const delta = restoreSwapAnchor({ selector: "#table", top: 240 }, { querySelector: () => at(60) }, (x, y) => calls.push([x, y]));
	assert.equal(delta, -180);
	assert.deepEqual(calls, [[0, -180]]);
});

test("위치가 같으면 굴리지 않는다", () => {
	const calls = [];
	assert.equal(restoreSwapAnchor({ selector: "#t", top: 100 }, { querySelector: () => at(100) }, (x, y) => calls.push([x, y])), 0);
	assert.equal(calls.length, 0);
});

test("누른 컨트롤이 data-swap-anchor-key 상자 안이면 그 상자를 기준으로 삼는다", () => {
	const box = { getAttribute: (n) => (n === "data-swap-anchor-key" ? "etf-compare" : null) };
	const trigger = Object.assign(new globalThis.Element(), { closest: (s) => (s === "[data-swap-anchor-key]" ? box : null) });
	const memo = rememberSwapAnchor(target("#chips"), { querySelector: (s) => (s === '[data-swap-anchor-key="etf-compare"]' ? at(612) : at(40)) }, trigger);
	assert.deepEqual(memo, { selector: '[data-swap-anchor-key="etf-compare"]', top: 612 });
});

test("상자 밖 컨트롤은 대상의 선택자를 쓴다", () => {
	const trigger = Object.assign(new globalThis.Element(), { closest: () => null });
	const memo = rememberSwapAnchor(target("#chips"), { querySelector: (s) => (s === "#chips" ? at(40) : null) }, trigger);
	assert.deepEqual(memo, { selector: "#chips", top: 40 });
});

test("대상에 표시가 없어도 상자 안 컨트롤이면 고정한다", () => {
	const box = { getAttribute: () => "k" };
	const trigger = Object.assign(new globalThis.Element(), { closest: () => box });
	assert.deepEqual(rememberSwapAnchor(target(null), { querySelector: () => at(5) }, trigger), { selector: '[data-swap-anchor-key="k"]', top: 5 });
});
