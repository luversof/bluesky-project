// 조각이 실어 온 이름([data-page-title])으로 문서 제목을 맞춘다.
//
// 실측 2026-09-09: 종목·계좌 상세의 껍데기는 이름을 몰라 제목이 "종목 상세 · Bluesky Stock" 으로만 나왔다. 조각이 정착하면
// 그 이름으로 바꾼다. 빌드 산출물(common.js)을 그대로 부르되, 문서 전역 훅이 많아 최소 DOM 스텁으로 띄운다.
import assert from "node:assert/strict";
import test from "node:test";

const listeners = {};
const noop = () => {};
const stubEl = () => ({
	addEventListener: noop,
	removeEventListener: noop,
	setAttribute: noop,
	removeAttribute: noop,
	getAttribute: () => null,
	hasAttribute: () => false,
	querySelectorAll: () => [],
	querySelector: () => null,
	closest: () => null,
	matches: () => false,
	appendChild: noop,
	remove: noop,
	classList: { contains: () => false, add: noop, remove: noop, toggle: noop },
	dataset: {},
	style: {},
	children: [],
});
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }),
	body: stubEl(),
	head: stubEl(),
	title: "종목 상세 · Bluesky Stock",
	getElementById: () => null,
	createElement: () => stubEl(),
	createTextNode: () => ({}),
	readyState: "complete",
	addEventListener: (name, fn) => {
		(listeners[name] = listeners[name] || []).push(fn);
	},
});
globalThis.window = globalThis;
globalThis.MutationObserver = class {
	observe() {}
	disconnect() {}
};
globalThis.location = { search: "", href: "https://x/stock/item", origin: "https://x", pathname: "/stock/item" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const mod = globalThis.__pageTitleInternals;

test("내부 함수가 노출돼 있고 afterSettle 훅이 걸려 있다", () => {
	assert.ok(mod, "common.js 가 __pageTitleInternals 를 노출하지 않는다");
	assert.equal(typeof mod.pageTitleFor, "function");
	assert.ok((listeners["htmx:afterSettle"] || []).length > 0, "htmx:afterSettle 훅이 없다");
});

test("이름이 있으면 '이름 · Bluesky Stock'", () => {
	assert.equal(mod.pageTitleFor("TIGER 리츠부동산인프라", "종목 상세 · Bluesky Stock"), "TIGER 리츠부동산인프라 · Bluesky Stock");
	assert.equal(mod.pageTitleFor("  KB증권 위탁  ", "x"), "KB증권 위탁 · Bluesky Stock");
});

test("이름이 비면 지금 제목을 그대로 둔다(없는 id 화면)", () => {
	assert.equal(mod.pageTitleFor("", "종목 상세 · Bluesky Stock"), "종목 상세 · Bluesky Stock");
	assert.equal(mod.pageTitleFor(null, "계좌 상세 · Bluesky Stock"), "계좌 상세 · Bluesky Stock");
});

test("정착한 조각에 data-page-title 이 있으면 document.title 을 바꾼다", () => {
	const root = { querySelector: (sel) => (sel === "[data-page-title]" ? { getAttribute: () => "삼성전자" } : null) };
	mod.applyFragmentTitle(root);
	assert.equal(globalThis.document.title, "삼성전자 · Bluesky Stock");
	const before = globalThis.document.title;
	mod.applyFragmentTitle({ querySelector: () => null });
	assert.equal(globalThis.document.title, before);
});
