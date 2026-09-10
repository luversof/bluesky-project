// 활동 달력 날짜 칸: 펼치면 그날 상세가 보이고, 다시 누르면 aria-expanded 와 상세 블록이 함께 접힌다.
//
// 실측 2026-09-10(qa/cal-enter.cjs): 두 번째 누름에 aria-expanded 는 false 가 됐지만 상세 블록은 hidden 이 되돌아오지 않아 계속 보였다.
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
const mod = globalThis.__calendarDayInternals;

function classes(initial = []) {
	const set = new Set(initial);
	return { set, contains: (c) => set.has(c), add: (c) => set.add(c), remove: (c) => set.delete(c) };
}
/** 조각 루트 > 월 패널 > 상세 블록들, 그리고 날짜 칸. */
function scene() {
	const detail = { classList: classes(["hidden"]) };
	const otherDetail = { classList: classes(["hidden"]) };
	const panel = { classList: classes(), querySelector: (sel) => (sel === '[data-cal-detail="2026-09-02"]' ? detail : null), querySelectorAll: (sel) => (sel === "[data-cal-detail]" ? [detail, otherDetail] : []) };
	const root = { querySelector: (sel) => (sel === '[data-cal-panel="2026-09"]' ? panel : null), querySelectorAll: (sel) => (sel === "[data-cal-date].bg-base-200" ? [cell].filter((c) => c.classList.contains("bg-base-200")) : []) };
	const attrs = {};
	const cell = { classList: classes(), attrs, closest: (sel) => (sel === "#activityListFragment" ? root : null), getAttribute: (n) => ({ "data-cal-date": "2026-09-02", "data-cal-month": "2026-09" })[n] ?? attrs[n] ?? null, setAttribute: (n, v) => { attrs[n] = v; } };
	return { cell, panel, detail, otherDetail };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __calendarDayInternals 를 노출하지 않는다");
});

test("펼치면 상세가 보이고 다른 날 상세는 숨는다", () => {
	const { cell, panel, detail, otherDetail } = scene();
	otherDetail.classList.remove("hidden");
	mod.toggleCalendarDay(cell);
	assert.equal(detail.classList.contains("hidden"), false);
	assert.equal(otherDetail.classList.contains("hidden"), true);
	assert.equal(panel.classList.contains("is-open"), true);
	assert.equal(cell.attrs["aria-expanded"], "true");
});

test("다시 누르면 aria-expanded 와 상세 블록이 함께 접힌다", () => {
	const { cell, panel, detail } = scene();
	mod.toggleCalendarDay(cell);
	mod.toggleCalendarDay(cell);
	assert.equal(cell.attrs["aria-expanded"], "false");
	assert.equal(panel.classList.contains("is-open"), false);
	assert.equal(detail.classList.contains("hidden"), true, "접힌 뒤에도 상세가 보이면 상태 불일치(실측 재현)");
	assert.equal(cell.classList.contains("bg-base-200"), false);
});
