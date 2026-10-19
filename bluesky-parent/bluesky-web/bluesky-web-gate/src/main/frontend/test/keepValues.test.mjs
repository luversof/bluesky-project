// 구역을 바꿔 끼워도 고른 select 값 · 체크박스를 되살린다(2026-10-19 C450 — PoE1 제작 UX QA: 화폐를 쓰면 고른 에센스가 첫 항목으로 돌아가 연타하면 엉뚱한 에센스를 썼다).
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
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);


await import("../../resources/static/js/common.js");
const { rememberValues, restoreValues } = globalThis.__keepValuesInternals;

const sel = (value, opts) => ({ value, options: opts.map((v) => ({ value: v })) });
const box = (value, checked, disabled = false) => ({ type: "checkbox", value, checked, disabled });
const root = (id, names, selects, boxes) =>
	Object.assign(new globalThis.Element(), {
		id,
		getAttribute: (n) => (n === "data-keep-values" ? names : null),
		querySelector: (q) => { const m = q.match(/name="([^"]+)"/); return m && q.startsWith("select") ? selects[m[1]] || null : null; },
		querySelectorAll: (q) => { const m = q.match(/name="([^"]+)"/); return m ? boxes[m[1]] || [] : []; },
	});

test("교체 직전 고른 값 · 체크된 값을 적어 둔다", () => {
	const memo = rememberValues(root("craft-emu", "essence, fossil", { essence: sel("Greed7", ["Anger6", "Greed7"]) }, { fossil: [box("Dense", true), box("Pristine", false), box("Aberrant", true)] }));
	assert.deepEqual(memo, { id: "craft-emu", selects: { essence: "Greed7" }, checks: { fossil: ["Dense", "Aberrant"] } });
});

test("표시 · id 가 없으면 아무것도 안 한다", () => {
	assert.equal(rememberValues(root("craft-emu", null, {}, {})), null);
	assert.equal(rememberValues(root("", "essence", {}, {})), null);
	assert.equal(rememberValues({}), null);
});

test("새 대상에 되돌린다 — 선택지에 없는 값 · 꺼진 체크박스는 그대로", () => {
	const s1 = sel("Anger6", ["Anger6", "Greed7"]);
	const s2 = sel("ReforgeFire", ["ReforgeFire"]);
	const b = [box("Dense", false), box("Aberrant", false, true), box("Pristine", false)];
	const fresh = { querySelector: (q) => (q.includes('"essence"') ? s1 : q.includes('"harvest"') ? s2 : null), querySelectorAll: (q) => (q.includes('"fossil"') ? b : []) };
	const n = restoreValues({ id: "craft-emu", selects: { essence: "Greed7", harvest: "ReforgePhysical" }, checks: { fossil: ["Dense", "Aberrant"] } }, { getElementById: () => fresh });
	assert.equal(n, 2);
	assert.equal(s1.value, "Greed7");
	assert.equal(s2.value, "ReforgeFire");
	assert.deepEqual(b.map((x) => x.checked), [true, false, false]);
	assert.equal(restoreValues(null), 0);
});
