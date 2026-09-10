// 조각 교체 뒤 포커스 복원: 교체 전 활성 요소의 id 또는 구조 경로를 기억해 정착 뒤 같은 자리를 다시 포커스한다.
//
// 실측 2026-09-09(qa/focus-after-swap.cjs): 정렬 헤더(th[hx-get])·활동 '조회' 버튼을 키보드로 누르면 조각이 outerHTML 로 바뀌며
// 포커스가 body 로 떨어졌다(5/5). 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(snapshotFocus.test.mjs 와 같은 방식).
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
const byId = {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: (id) => byId[id] || null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: (type, fn) => { (handlers[type] = handlers[type] || []).push(fn); },
	activeElement: null,
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
const mod = globalThis.__swapFocusInternals;
// 같은 이벤트를 듣는 다른 처리기(오류 배너·aria-busy 등)가 기대하는 필드도 채워 준다.
const fire = (type, ev) => (handlers[type] || []).forEach((fn) => fn({ target: Object.assign(stubEl(), { attributes: {}, tagName: "FORM" }), ...ev, detail: { elt: Object.assign(stubEl(), { attributes: {}, tagName: "FORM" }), xhr: { status: 200, getResponseHeader: () => null }, pathInfo: { requestPath: "/x" }, ...ev.detail } }));

/** 최소 트리: children/parentElement/contains/querySelector(구조 경로만 이해). */
class Fake extends globalThis.Element {
	constructor(tag, id = "", kids = []) {
		super(); this.tagName = tag.toUpperCase(); this.id = id; this.attrs = {}; this.focused = 0; this.parentElement = null;
		this.children = kids; kids.forEach((k) => { k.parentElement = this; });
	}
	hasAttribute(n) { return n in this.attrs; }
	setAttribute(n, v) { this.attrs[n] = v; }
	focus() { this.focused++; }
	contains(el) { if (el === this) return true; return this.children.some((k) => k.contains(el)); }
	querySelector(path) {
		// ":scope > tag:nth-child(i) > ..." 만 해석한다
		let cur = this;
		for (const step of path.replace(/^:scope > /, "").split(" > ")) {
			const m = /^([a-z]+):nth-child\((\d+)\)$/.exec(step); if (!m) return null;
			cur = cur.children[Number(m[2]) - 1]; if (!cur || cur.tagName !== m[1].toUpperCase()) return null;
		}
		return cur;
	}
}
function table() {
	const th1 = new Fake("th"), th2 = new Fake("th"), th3 = new Fake("th");
	const tr = new Fake("tr", "", [th1, th2, th3]); const thead = new Fake("thead", "", [tr]);
	const tbl = new Fake("table", "", [thead]); const frag = new Fake("div", "dividendListFragment", [new Fake("form"), tbl]);
	return { frag, th2 };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __swapFocusInternals 를 노출하지 않는다");
});

test("구조 경로는 교체 대상 기준 nth-child 사슬이다", () => {
	const { frag, th2 } = table();
	assert.equal(mod.structuralPath(th2, frag), ":scope > div:nth-child(2) > thead:nth-child(1) > tr:nth-child(1) > th:nth-child(2)".replace("div:nth-child(2)", "table:nth-child(2)"));
	assert.equal(mod.structuralPath(new Fake("th"), frag), null, "대상 밖 요소는 경로가 없다");
});

test("교체 대상 안의 활성 요소만 기억한다", () => {
	const { frag, th2 } = table();
	const memo = mod.rememberFocus(frag, th2);
	assert.deepEqual(memo, { id: null, path: ":scope > table:nth-child(2) > thead:nth-child(1) > tr:nth-child(1) > th:nth-child(2)", targetId: "dividendListFragment" });
	assert.equal(mod.rememberFocus(frag, new Fake("button")), null, "대상 밖 활성 요소");
	assert.equal(mod.rememberFocus(frag, globalThis.document.body), null, "body 는 기억할 것이 없다");
	assert.equal(mod.rememberFocus(null, th2), null);
});

test("정착 뒤 새 조각의 같은 자리를 포커스한다 - th 는 tabindex 를 보장", () => {
	const before = table(); const after = table();
	const memo = mod.rememberFocus(before.frag, before.th2);
	assert.equal(mod.restoreFocus(memo, after.frag, globalThis.document.body), true);
	assert.equal(after.th2.focused, 1);
	assert.equal(after.th2.attrs.tabindex, "-1", "새 th 에 tabindex 가 없으면 -1 을 준다");
	const withTab = table(); withTab.th2.attrs.tabindex = "0";
	mod.restoreFocus(memo, withTab.frag, globalThis.document.body);
	assert.equal(withTab.th2.attrs.tabindex, "0", "이미 tabindex 가 있으면 그대로");
});

test("다른 코드가 이미 포커스를 옮겼거나 id 로 찾을 수 있으면 그에 따른다", () => {
	const { frag, th2 } = table();
	const memo = mod.rememberFocus(frag, th2);
	assert.equal(mod.restoreFocus(memo, table().frag, new Fake("h3")), false, "활성 요소가 body 가 아니면 건드리지 않는다");
	const btn = new Fake("button", "refreshBtn"); byId.refreshBtn = btn;
	assert.equal(mod.restoreFocus({ id: "refreshBtn", path: null, targetId: "x" }, null, globalThis.document.body), true);
	assert.equal(btn.focused, 1);
	assert.equal(btn.attrs.tabindex, undefined, "button 은 본래 포커스 가능하니 tabindex 를 붙이지 않는다");
	assert.equal(mod.restoreFocus(null, frag, globalThis.document.body), false);
});

test("htmx 이벤트 흐름: beforeSwap 에서 기억하고 afterSettle 에서 새 조각(id 로 재해석)에 복원한다", () => {
	const before = table(); const after = table();
	globalThis.document.activeElement = before.th2;
	fire("htmx:beforeSwap", { detail: { target: before.frag } });
	byId.dividendListFragment = after.frag;
	globalThis.document.activeElement = globalThis.document.body;
	fire("htmx:afterSettle", { detail: { target: before.frag } });
	assert.equal(after.th2.focused, 1, "옛 대상이 detach 됐어도 id 로 새 조각을 찾아 같은 자리를 포커스한다");
	fire("htmx:afterSettle", { detail: { target: before.frag } });
	assert.equal(after.th2.focused, 1, "기억은 한 번만 쓴다");
});
