// 계좌/종목 다중선택의 높이 맞춤은 ResizeObserver 로 받아 쓰고, 동기 레이아웃(getBoundingClientRect)을 강제하지 않는다.
//
// 실측 2026-09-10(qa/cpu-profile-cols.cjs, 4배 CPU): htmx 목록 교체 직후 MutationObserver 안의 getBoundingClientRect 가
// 매매 493ms·배당 371ms 자체 시간(가장 긴 작업 753/482ms 의 대부분)이었다.
// 빌드 산출물(multiSelectInit.js)을 최소 DOM 스텁으로 띄운다.
import assert from "node:assert/strict";
import test from "node:test";

const noop = () => {};
globalThis.Element = class Element {};
globalThis.HTMLSelectElement = class HTMLSelectElement extends globalThis.Element {};
globalThis.document = { getElementById: () => null, createElement: () => ({ textContent: "", appendChild: noop, style: {} }), head: { appendChild: noop }, documentElement: { appendChild: noop }, addEventListener: noop, querySelectorAll: () => [] };
globalThis.window = globalThis;
globalThis.console.debug = noop;
let observers = [];
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.ResizeObserver = class { constructor(cb) { this.cb = cb; this.observed = []; observers.push(this); } observe(el, opts) { this.observed.push([el, opts]); } disconnect() {} };

await import("../../resources/static/js/stock/multiSelectInit.js");
const mod = globalThis.__multiSelectInitInternals;

function scene() {
	const account = { name: "accountIdList", rectCalls: 0, getBoundingClientRect() { this.rectCalls++; return { height: 176 }; } };
	const stock = { name: "stockItemIdList", style: {} };
	const form = { querySelector: (sel) => (sel.includes("accountIdList") ? account : sel.includes("stockItemIdList") ? stock : null) };
	const scope = new globalThis.Element(); scope.querySelectorAll = (sel) => (sel === "form" ? [form] : []);
	return { account, stock, form, scope };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "multiSelectInit.js 가 __multiSelectInitInternals 를 노출하지 않는다");
});

test("ResizeObserver 로 계좌 선택을 관찰하고, 동기 레이아웃을 읽지 않는다; 크기 알림이 오면 종목 선택 높이를 맞춘다", () => {
	observers = [];
	const { account, stock, scope } = scene();
	assert.equal(mod.syncLinkedSelectHeights(scope), 1);
	assert.equal(account.rectCalls, 0, "getBoundingClientRect 를 부르면 교체 직후 강제 레이아웃이 난다(실측 재현)");
	assert.equal(observers.length, 1);
	assert.deepEqual(observers[0].observed[0], [account, { box: "border-box" }]);
	assert.equal(stock.style.height, undefined, "알림 전에는 건드리지 않는다");
	observers[0].cb([{ borderBoxSize: [{ blockSize: 176 }], contentRect: { height: 170 } }]);
	assert.equal(stock.style.height, "176px");
	assert.equal(stock.style.maxHeight, "176px");
	assert.equal(stock.style.overflowY, "auto");
	observers[0].cb([{ contentRect: { height: 120 } }]);
	assert.equal(stock.style.height, "120px", "borderBoxSize 가 없으면 contentRect 로");
	assert.equal(mod.syncLinkedSelectHeights(scope), 1);
	assert.equal(observers.length, 1, "같은 종목 선택에 두 번 관찰자를 붙이지 않는다");
});

test("높이 0 이나 같은 값이면 스타일을 건드리지 않는다", () => {
	const stock = { style: {} };
	mod.linkSelectHeights({}, stock, 0);
	assert.deepEqual(stock.style, {});
	mod.linkSelectHeights({}, stock, 90);
	stock.style.maxHeight = "changed";
	mod.linkSelectHeights({}, stock, 90);
	assert.equal(stock.style.maxHeight, "changed", "같은 높이면 다시 쓰지 않는다(레이아웃 무효화 방지)");
});

test("ResizeObserver 가 없으면 예전처럼 즉시 읽어 맞춘다", () => {
	const saved = globalThis.ResizeObserver; delete globalThis.ResizeObserver;
	try {
		const { account, stock, scope } = scene();
		assert.equal(mod.syncLinkedSelectHeights(scope), 1);
		assert.equal(account.rectCalls, 1);
		assert.equal(stock.style.height, "176px");
	} finally { globalThis.ResizeObserver = saved; }
});
