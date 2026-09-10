// 보유 스냅샷 포커스 흐름: 정착하면 카드 제목으로, 닫으면 폼의 날짜 입력으로 포커스를 옮긴다.
//
// 실측 2026-09-09(qa/snapshot-focus.cjs): 조회 뒤 포커스가 날짜 입력에 남고(카드까지 Tab 5회), 닫으면 body 로 떨어졌다.
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(ariaBusy.test.mjs 와 같은 방식).
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
const handlers = {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: (type, fn) => { (handlers[type] = handlers[type] || []).push(fn); },
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
const mod = globalThis.__snapshotFocusInternals;
const fire = (type, ev) => (handlers[type] || []).forEach((fn) => fn(ev));

class Fake extends globalThis.Element {
	constructor(id, children = {}) { super(); this.id = id; this.attrs = {}; this.focused = 0; this.children = children; }
	querySelector(sel) { return this.children[sel] || null; }
	hasAttribute(n) { return n in this.attrs; }
	setAttribute(n, v) { this.attrs[n] = v; }
	focus() { this.focused++; }
	closest(sel) { return this.matchesSel === sel ? this : null; }
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __snapshotFocusInternals 를 노출하지 않는다");
});

test("정착한 대상이 스냅샷 컨테이너면 제목(h3)에 tabindex=-1 을 주고 포커스한다", () => {
	const h3 = new Fake("h3");
	const container = new Fake("holdings-snapshot-container", { h3 });
	assert.equal(mod.focusSnapshotHeading(container), true);
	assert.equal(h3.attrs.tabindex, "-1");
	assert.equal(h3.focused, 1);
	assert.equal(mod.focusSnapshotHeading(new Fake("dividendListFragment", { h3: new Fake("h3") })), false, "다른 조각은 손대지 않는다");
	assert.equal(mod.focusSnapshotHeading(new Fake("holdings-snapshot-container")), false, "빈 컨테이너(닫힘)면 할 일 없음");
	assert.equal(mod.focusSnapshotHeading(undefined), false);
});

test("htmx:afterSettle 이벤트가 컨테이너를 정착시키면 제목으로 간다", () => {
	const h3 = new Fake("h3");
	fire("htmx:afterSettle", { detail: { target: new Fake("holdings-snapshot-container", { h3 }) } });
	assert.equal(h3.focused, 1);
});

test("닫으면 폼의 날짜 입력으로 돌아간다 - 폼이 없으면 false", () => {
	const input = new Fake("date"), canvas = new Fake("assetGrowthChart"); canvas.tagName = "CANVAS";
	// 문서 순서로는 canvas 가 폼보다 앞이다 - 복합 셀렉터로 한 번에 찾으면 canvas 가 잡혀 포커스가 안 옮겨졌다(실측).
	const root = { querySelector: (sel) => (sel.includes(",") ? canvas : sel === "[data-holdings-snapshot-form] input[type=date]" ? input : sel === "canvas#assetGrowthChart" ? canvas : null) };
	assert.equal(mod.restoreSnapshotFormFocus(root), true);
	assert.equal(input.focused, 1, "날짜 입력이 먼저다");
	assert.equal(canvas.focused, 0);
	const onlyCanvas = { querySelector: (sel) => (sel === "canvas#assetGrowthChart" ? canvas : null) };
	assert.equal(mod.restoreSnapshotFormFocus(onlyCanvas), true);
	assert.equal(canvas.attrs.tabindex, "-1", "폼이 없으면 캔버스에 tabindex 를 줘서라도 포커스를 붙잡는다");
	assert.equal(canvas.focused, 1);
	assert.equal(mod.restoreSnapshotFormFocus({ querySelector: () => null }), false);
});

test("닫기 버튼 클릭(위임)이 복귀를 부른다", () => {
	const input = new Fake("date");
	globalThis.document.querySelector = (sel) => (sel === "[data-holdings-snapshot-form] input[type=date]" ? input : null);
	const btn = new Fake("close"); btn.matchesSel = "[data-holdings-snapshot-close]";
	fire("click", { target: btn });
	assert.equal(input.focused, 1);
	const other = new Fake("other");
	fire("click", { target: other });
	assert.equal(input.focused, 1, "다른 클릭은 포커스를 옮기지 않는다");
});
