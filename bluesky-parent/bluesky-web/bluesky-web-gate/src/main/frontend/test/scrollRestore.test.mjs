// 뒤로가기 스크롤 복원: 떠날 때 URL 별 scrollY 저장, back_forward 진입이면 조각이 채워져 높이가 충분해질 때 한 번 복원.
//
// 실측 2026-09-10(qa/back-nav.cjs): 목록 4화면에서 300px 내려간 뒤 상세로 갔다 Back 하면 81px 에 멈췄다 - 첫 HTML 이 스켈레톤(881px,
// 뷰포트 800)이라 브라우저 복원이 81 에 잘리고 htmx 조각(1,383~3,919px)이 채워진 뒤엔 다시 시도하지 않는다.
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
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR", scrollHeight: 881 }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: noop,
});
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.location = { search: "?rangeMode=1", href: "https://x/stock/trade?rangeMode=1", origin: "https://x", pathname: "/stock/trade" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const mod = globalThis.__scrollRestoreInternals;

function memoryStorage() {
	const map = new Map();
	return { map, getItem: (k) => (map.has(k) ? map.get(k) : null), setItem: (k, v) => map.set(k, v), removeItem: (k) => map.delete(k) };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __scrollRestoreInternals 를 노출하지 않는다");
});

test("키는 경로+쿼리(프리셋으로 replaceState 된 URL 그대로)", () => {
	assert.equal(mod.scrollMemoKey({ pathname: "/stock/trade", search: "?rangeMode=1" }), "scrollY:/stock/trade?rangeMode=1");
	assert.equal(mod.scrollMemoKey(), "scrollY:/stock/trade?rangeMode=1", "기본값은 현재 location");
});

test("저장: 0 보다 크면 반올림해 적고, 0 이면 지운다; 저장소 예외는 삼킨다", () => {
	const st = memoryStorage();
	mod.saveScrollMemo(st, 300.4, "k");
	assert.equal(st.getItem("k"), "300");
	mod.saveScrollMemo(st, 0, "k");
	assert.equal(st.getItem("k"), null);
	assert.doesNotThrow(() => mod.saveScrollMemo({ setItem() { throw new Error("quota"); }, removeItem() {} }, 10, "k"));
	assert.equal(mod.readScrollMemo(st, "k"), 0);
	st.setItem("k", "300"); assert.equal(mod.readScrollMemo(st, "k"), 300);
	st.setItem("k", "junk"); assert.equal(mod.readScrollMemo(st, "k"), 0);
	assert.equal(mod.readScrollMemo({ getItem() { throw new Error("blocked"); } }, "k"), 0);
});

test("back_forward 판정은 navigation 엔트리 type 을 본다", () => {
	assert.equal(mod.isBackForwardNavigation({ getEntriesByType: () => [{ type: "back_forward" }] }), true);
	assert.equal(mod.isBackForwardNavigation({ getEntriesByType: () => [{ type: "navigate" }] }), false);
	assert.equal(mod.isBackForwardNavigation({ getEntriesByType: () => [] }), false);
	assert.equal(mod.isBackForwardNavigation(undefined), false);
});

test("복원: 스켈레톤 높이(881, 뷰포트 800)면 미룬다(false), 조각이 채워져 1383 이면 300 으로 스크롤한다(true)", () => {
	const calls = [];
	const scroll = (x, y) => calls.push([x, y]);
	assert.equal(mod.restoreScrollIfTall(300, { scrollHeight: 881 }, 800, 81, scroll), false, "881-800=81 < 300 이라 아직 못 복원(실측 재현)");
	assert.deepEqual(calls, []);
	assert.equal(mod.restoreScrollIfTall(300, { scrollHeight: 1383 }, 800, 81, scroll), true);
	assert.deepEqual(calls, [[0, 300]]);
});

test("복원: 저장값 0 이나 사용자가 이미 더 내려간 경우는 건드리지 않고 끝낸다", () => {
	const calls = [];
	const scroll = (x, y) => calls.push([x, y]);
	assert.equal(mod.restoreScrollIfTall(0, { scrollHeight: 5000 }, 800, 0, scroll), true);
	assert.equal(mod.restoreScrollIfTall(300, { scrollHeight: 5000 }, 800, 900, scroll), true, "사용자가 900 까지 내려갔으면 300 으로 되돌리지 않는다");
	assert.deepEqual(calls, []);
});
