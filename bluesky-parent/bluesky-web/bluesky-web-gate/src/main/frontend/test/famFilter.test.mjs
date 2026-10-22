// 계열 표 거르기(2026-10-22 P1, poedb 속성 부여의 태그 칩 · 검색) — 판정 함수와 기억(화면마다 · 탭 안).
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(swapAnchor.test.mjs 와 같은 방식).
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
globalThis.location = { search: "", href: "https://x/poe2/craft", origin: "https://x", pathname: "/poe2/craft" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
const store = new Map();
globalThis.sessionStorage = { getItem: (k) => (store.has(k) ? store.get(k) : null), setItem: (k, v) => store.set(k, String(v)), removeItem: (k) => store.delete(k) };
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const { famFilterMatch, famFilterActive, readFamFilter, saveFamFilter, EMPTY_FAM_FILTER } = globalThis.__famFilterInternals;

const F = (q = "", tags = [], rollable = false) => ({ q, tags, rollable });

test("글자는 공백 · 대소문자를 무시하고 포함이면 맞음", () => {
	assert.equal(famFilterMatch("생명력 최대치 +(175—189)", [], true, F("생명력최대")), true);
	assert.equal(famFilterMatch("+# to maximum Life", [], true, F("MAXIMUM life")), true);
	assert.equal(famFilterMatch("화염 저항 +#%", [], true, F("냉기")), false);
	assert.equal(famFilterMatch("아무거나", [], false, F("  ")), true);
});

test("태그 여러 개면 모두 가진 계열만", () => {
	const tags = ["원소", "화염", "저항"];
	assert.equal(famFilterMatch("화염 저항", tags, true, F("", ["화염"])), true);
	assert.equal(famFilterMatch("화염 저항", tags, true, F("", ["화염", "저항"])), true);
	assert.equal(famFilterMatch("화염 저항", tags, true, F("", ["화염", "냉기"])), false);
	assert.equal(famFilterMatch("생명력", [], true, F("", ["생명력"])), false);
});

test("'붙을 수 있는 것만'은 못 붙는 계열을 뺀다 — 셋을 함께 걸면 모두 만족", () => {
	assert.equal(famFilterMatch("생명력", ["생명력"], false, F("", [], true)), false);
	assert.equal(famFilterMatch("생명력", ["생명력"], true, F("", [], true)), true);
	assert.equal(famFilterMatch("생명력 최대치", ["생명력"], true, F("최대", ["생명력"], true)), true);
	assert.equal(famFilterMatch("생명력 최대치", ["생명력"], false, F("최대", ["생명력"], true)), false);
});

test("빈 거르기는 꺼진 것으로 센다", () => {
	assert.equal(famFilterActive(EMPTY_FAM_FILTER), false);
	assert.equal(famFilterActive(F("  ")), false);
	assert.equal(famFilterActive(F("a")), true);
	assert.equal(famFilterActive(F("", ["화염"])), true);
	assert.equal(famFilterActive(F("", [], true)), true);
});

test("저장한 거르기는 같은 화면 열쇠로 다시 읽힌다(처음엔 빈 거르기)", () => {
	assert.deepEqual(readFamFilter(), EMPTY_FAM_FILTER);
	saveFamFilter(F("저항", ["화염"], true));
	assert.deepEqual(readFamFilter(), F("저항", ["화염"], true));
	assert.ok(store.get("craftFamFilter:/poe2/craft").includes("저항"));
});
