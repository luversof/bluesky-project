// 이미지 실패 처리(10-13 C287) — 인라인 onerror 를 데이터 속성 + common.js capture 처리기로 옮겼다.
// 대체 주소는 한 번만(대체 그림도 없으면 무한 반복이 된다), 동작 속성별로 지우기 · 숨기기 · 옆 칸 보이기 · 글자로 바꾸기.
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
	getElementById: () => null, createElement: () => stubEl(), createTextNode: (t) => ({ text: t }), readyState: "complete",
	addEventListener: noop,
});
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.location = { search: "", href: "https://x/poe", origin: "https://x", pathname: "/poe" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const { handleImgError } = globalThis.__imgErrorInternals;

const img = (dataset) => {
	const el = { tagName: "IMG", dataset: { ...dataset }, style: {}, src: "a.png", removed: false, replaced: null, next: { cls: new Set(["hidden"]) } };
	el.remove = () => { el.removed = true; };
	el.replaceWith = (n) => { el.replaced = n; };
	el.nextElementSibling = { classList: { remove: (c) => el.next.cls.delete(c) } };
	return el;
};

test("대체 주소는 한 번만 — 두 번째 실패에선 다음 동작으로", () => {
	const el = img({ fallbackSrc: "/b.png", onerror: "hide" });
	assert.equal(handleImgError(el), "fallback");
	assert.equal(el.src, "/b.png");
	assert.equal(handleImgError(el), "hide");
	assert.equal(el.style.display, "none");
});

test("지우기 · 숨기고 옆 칸 보이기 · 글자로 바꾸기", () => {
	const r = img({ onerror: "remove" });
	assert.equal(handleImgError(r), "remove");
	assert.equal(r.removed, true);
	const n = img({ onerror: "hide-reveal-next" });
	assert.equal(handleImgError(n), "hide-reveal-next");
	assert.equal(n.style.display, "none");
	assert.equal(n.next.cls.has("hidden"), false);
	const t = img({ onerror: "text", onerrorText: "카오스 오브" });
	assert.equal(handleImgError(t), "text");
	assert.deepEqual(t.replaced, { text: "카오스 오브" });
});

test("속성 없는 그림 · 그림이 아닌 대상은 건드리지 않는다", () => {
	const plain = img({});
	assert.equal(handleImgError(plain), "none");
	assert.equal(plain.removed, false);
	assert.equal(plain.style.display, undefined);
	assert.equal(handleImgError({ tagName: "SCRIPT", dataset: { onerror: "remove" } }), "skip");
	assert.equal(handleImgError(null), "skip");
});
