// htmx 조각 요청 중인 대상 영역에 aria-busy 를 걸고, 정착하거나 실패하면 지운다.
//
// 실측 2026-09-09(qa/aria-busy-probe.cjs): 4/4 화면에서 요청 중 hx-target 에 aria-busy 가 없었다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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
const mod = globalThis.__ariaBusyInternals;
const fire = (type, detail) => (handlers[type] || []).forEach((fn) => fn({ type, detail, target: document }));

class Fake extends globalThis.Element {
	constructor() { super(); this.attrs = {}; }
	setAttribute(n, v) { this.attrs[n] = v; }
	removeAttribute(n) { delete this.attrs[n]; }
	hasAttribute(n) { return n in this.attrs; }
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __ariaBusyInternals 를 노출하지 않는다");
	assert.equal(typeof mod.markBusy, "function");
	assert.equal(typeof mod.clearBusy, "function");
});

test("요소에만 aria-busy 를 걸고 지운다", () => {
	const t = new Fake();
	assert.equal(mod.markBusy(t), true);
	assert.equal(t.attrs["aria-busy"], "true");
	assert.equal(mod.clearBusy(t), true);
	assert.equal("aria-busy" in t.attrs, false);
	assert.equal(mod.clearBusy(t), false, "이미 없으면 false");
	assert.equal(mod.markBusy(undefined), false, "target 없는 이벤트");
	assert.equal(mod.markBusy({}), false, "요소가 아닌 대상");
});

test("htmx 이벤트 흐름: beforeRequest 에 걸리고 afterSettle 에 풀린다, 실패는 afterRequest 에서 푼다", () => {
	const t = new Fake();
	fire("htmx:beforeRequest", { target: t });
	assert.equal(t.attrs["aria-busy"], "true", "요청 시작에 걸려야 한다");
	fire("htmx:afterRequest", { target: t, successful: true });
	assert.equal(t.attrs["aria-busy"], "true", "성공 응답은 정착 전까지 유지");
	fire("htmx:afterSettle", { target: t });
	assert.equal("aria-busy" in t.attrs, false);
	fire("htmx:beforeRequest", { target: t });
	fire("htmx:afterRequest", { target: t, successful: false });
	assert.equal("aria-busy" in t.attrs, false, "실패하면 정착 이벤트가 없으니 afterRequest 에서 풀어야 한다");
});
