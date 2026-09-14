// 주소에 적힌 활동 뷰가 첫 조회에 반영되는지.
//
// 실측 2026-09-12: 주소의 activityView 는 어떤 조합에서도 효과가 없었다 - calendar/timeline/list 셋 다
// 활성 탭은 늘 저장값이었고, 저장값이 없으면 캘린더였다. 서버는 이 값을 받아 처리하는데도(normalizeActivityView)
// 브라우저가 htmx:configRequest 에서 저장값으로 덮어썼기 때문이다. 공유 링크가 무력했다.
// 기간(rangeMode)에서 같은 이유로 공유 링크가 무력했던 것을 고친 것과 같은 규칙이다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다. (준비 코드는 activityTabControls.test.mjs 와 같은 스텁)
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

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
globalThis.location = { search: "", href: "https://x/stock/activity", origin: "https://x", pathname: "/stock/activity" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const resolve = globalThis.__activityViewInternals.resolveActivityViewForRequest;

test("주소에 적힌 뷰를 따른다", () => {
	assert.equal(resolve("?activityView=timeline", "calendar"), "timeline");
	assert.equal(resolve("?activityView=list", "timeline"), "list");
	assert.equal(resolve("?activityView=calendar", "timeline"), "calendar");
});

test("주소에 없으면 저장값을 쓴다", () => {
	assert.equal(resolve("?rangeMode=all", "timeline"), "timeline");
	assert.equal(resolve("", "list"), "list");
});

test("둘 다 없거나 모르는 값이면 캘린더", () => {
	assert.equal(resolve("", null), "calendar");
	assert.equal(resolve("?activityView=bogus", null), "calendar");
	assert.equal(resolve("?activityView=bogus", "timeline"), "timeline");
	assert.equal(resolve("", "bogus"), "calendar");
});

test("처리기가 주소를 실제로 본다", () => {
	// 빌드가 ${} 안 공백을 지우므로 공백을 눌러 비교하고, 긍정으로 못 박는다.
	const built = readFileSync(new URL("../../resources/static/js/common.js", import.meta.url), "utf8").replace(/\s+/g, "");
	assert.ok(
		built.includes("resolveActivityViewForRequest(search,saved)"),
		"configRequest 가 주소를 넘겨 뷰를 정해야 한다",
	);
	assert.ok(built.includes("location.search"), "주소의 질의를 읽어야 한다");
});
