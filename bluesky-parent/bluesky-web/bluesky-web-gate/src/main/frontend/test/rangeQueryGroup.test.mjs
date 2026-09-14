// URL 쿼리가 조각 요청에 병합될 때, 기간 세 키(startDate/endDate/rangeMode)가 한 묶음으로 움직이는지 본다.
//
// 예전에는 키 단위로만 덮어써서 URL 의 기간과 hx-include(저장된 전역 기간)의 기간이 섞였다.
// 실측 2026-09-11, /stock/trade 에서 저장된 기간이 1개월인 상태:
//   ?rangeMode=ytd            -> 버튼은 '올해' 인데 조각은 2026-08-12~09-11 을 조회해 20 행
//   ?startDate=..&endDate=..  -> 버튼은 '1개월' 인데 조각은 2026-01-01~09-11 을 조회해 115 행
// 즉 화면이 말하는 기간과 실제 데이터가 달랐다. URL 이 셋 중 하나라도 들고 있으면 나머지는 보내지 않아야
// 서버가 남은 하나로 기간을 다시 계산한다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

const listeners = new Map();
const store = new Map();

globalThis.sessionStorage = {
	getItem: (k) => (store.has(k) ? store.get(k) : null),
	setItem: (k, v) => store.set(k, String(v)),
	removeItem: (k) => store.delete(k),
};
globalThis.localStorage = globalThis.sessionStorage;
globalThis.location = { search: "", pathname: "/stock/trade", href: "https://x/stock/trade", origin: "https://x" };
globalThis.document = {
	readyState: "complete",
	addEventListener: (type, fn) => {
		if (!listeners.has(type)) listeners.set(type, []);
		listeners.get(type).push(fn);
	},
	removeEventListener: () => {},
	querySelectorAll: () => [],
	querySelector: () => null,
	getElementById: () => null,
	createElement: () => ({ style: {}, setAttribute: () => {}, appendChild: () => {}, classList: { add: () => {}, remove: () => {} } }),
	body: { dataset: {}, appendChild: () => {}, classList: { add: () => {}, remove: () => {} } },
	documentElement: { lang: "ko", dataset: {}, classList: { add: () => {}, remove: () => {} } },
};
globalThis.window = globalThis;
globalThis.addEventListener = () => {};
globalThis.Element = class Element {};
globalThis.matchMedia = () => ({ matches: false, addEventListener: () => {}, addListener: () => {}, removeEventListener: () => {} });
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.history = { replaceState: () => {}, pushState: () => {} };
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);
globalThis.NodeFilter = { SHOW_TEXT: 4 };

await import("../../resources/static/js/common.js");

/** hx-include 로 이미 실려 있는 전역 기간(= 저장된 기간). */
function includedRange() {
	return {
		startDate: "2026-08-11T15:00:00.000Z",
		endDate: "2026-09-11T15:00:00.000Z",
		rangeMode: "1",
		accountIdList: ["a"],
	};
}

function fire(search, parameters) {
	globalThis.location.search = search;
	const elt = Object.assign(new globalThis.Element(), {
		matches: (sel) => sel === "[data-params-from-query]",
	});
	const event = { type: "htmx:configRequest", detail: { parameters, elt } };
	for (const fn of listeners.get("htmx:configRequest") || []) fn(event);
	return parameters;
}

test("URL 에 rangeMode 만 있으면 실려 있던 날짜는 빠진다", () => {
	const p = fire("?rangeMode=ytd", includedRange());
	assert.equal(p.rangeMode, "ytd");
	assert.ok(!("startDate" in p), "startDate 가 남으면 서버가 옛 기간으로 조회한다");
	assert.ok(!("endDate" in p), "endDate 가 남으면 서버가 옛 기간으로 조회한다");
});

test("URL 에 날짜만 있으면 실려 있던 rangeMode 는 빠진다", () => {
	const p = fire("?startDate=2026-01-01T00:00:00Z&endDate=2026-09-12T00:00:00Z", includedRange());
	assert.equal(p.startDate, "2026-01-01T00:00:00Z");
	assert.equal(p.endDate, "2026-09-12T00:00:00Z");
	assert.ok(!("rangeMode" in p), "rangeMode 가 남으면 엉뚱한 프리셋 버튼이 눌린 것처럼 보인다");
});

test("URL 이 세 키를 모두 주면 그대로 간다", () => {
	const p = fire("?startDate=2026-01-01T00:00:00Z&endDate=2026-09-12T00:00:00Z&rangeMode=ytd", includedRange());
	assert.equal(p.startDate, "2026-01-01T00:00:00Z");
	assert.equal(p.endDate, "2026-09-12T00:00:00Z");
	assert.equal(p.rangeMode, "ytd");
});

test("URL 에 기간 키가 없으면 실려 있던 기간을 건드리지 않는다", () => {
	const p = fire("?accountIdList=b", includedRange());
	assert.equal(p.startDate, "2026-08-11T15:00:00.000Z");
	assert.equal(p.endDate, "2026-09-11T15:00:00.000Z");
	assert.equal(p.rangeMode, "1");
});

test("기간이 아닌 키는 예전처럼 키 단위로 덮어쓴다", () => {
	const p = fire("?accountIdList=b&rangeMode=ytd", includedRange());
	assert.equal(p.accountIdList, "b");
	assert.equal(p.rangeMode, "ytd");
	assert.ok(!("startDate" in p));
});
