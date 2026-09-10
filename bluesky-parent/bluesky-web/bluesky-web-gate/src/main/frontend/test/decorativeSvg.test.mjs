// 장식용 인라인 svg 에 aria-hidden 을 붙인다. 이름(aria-label/role/title)이 있는 svg 는 건드리지 않는다.
//
// 실측 2026-09-09: 13화면의 인라인 svg 265개 중 aria-hidden 이 0개였다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다(pageTitle.test.mjs 와 같은 방식).
import assert from "node:assert/strict";
import test from "node:test";

const noop = () => {};
const stubEl = () => ({
	addEventListener: noop, removeEventListener: noop, setAttribute: noop, removeAttribute: noop,
	getAttribute: () => null, hasAttribute: () => false, querySelectorAll: () => [], querySelector: () => null,
	closest: () => null, matches: () => false, appendChild: noop, remove: noop,
	classList: { contains: () => false, add: noop, remove: noop, toggle: noop }, dataset: {}, style: {}, children: [],
});
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
const mod = globalThis.__decorativeSvgInternals;

/** 속성 맵으로 svg 흉내. title 자식은 hasTitle 로. */
function svg(attrs = {}, hasTitle = false) {
	const set = { ...attrs };
	return {
		hasAttribute: (n) => Object.prototype.hasOwnProperty.call(set, n),
		getAttribute: (n) => (n in set ? set[n] : null),
		setAttribute: (n, v) => { set[n] = v; },
		querySelector: (sel) => (sel === "title" && hasTitle ? {} : null),
		attrs: set,
	};
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __decorativeSvgInternals 를 노출하지 않는다");
	assert.equal(typeof mod.hideDecorativeSvgs, "function");
});

test("이름 없는 svg 만 장식으로 본다", () => {
	assert.equal(mod.isDecorativeSvg(svg()), true);
	assert.equal(mod.isDecorativeSvg(svg({ "aria-label": "닫기" })), false);
	assert.equal(mod.isDecorativeSvg(svg({ role: "img" })), false);
	assert.equal(mod.isDecorativeSvg(svg({ "aria-labelledby": "x" })), false);
	assert.equal(mod.isDecorativeSvg(svg({}, true)), false, "title 자식이 있으면 의미 있는 그림");
	assert.equal(mod.isDecorativeSvg(svg({ "aria-hidden": "true" })), false, "이미 숨긴 것은 다시 세지 않는다");
});

test("장식 svg 에만 aria-hidden 을 붙이고 개수를 돌려준다", () => {
	const a = svg(), b = svg({ "aria-label": "메뉴" }), c = svg({}, true);
	const root = { querySelectorAll: (sel) => (sel === "svg" ? [a, b, c] : []) };
	assert.equal(mod.hideDecorativeSvgs(root), 1);
	assert.equal(a.attrs["aria-hidden"], "true");
	assert.equal(b.attrs["aria-hidden"], undefined);
	assert.equal(c.attrs["aria-hidden"], undefined);
	assert.equal(mod.hideDecorativeSvgs(root), 0, "두 번째 실행은 할 일이 없다");
});
