// 입력 도움말/미리보기(.form-control 안의 .label-text-alt, p.text-xs)를 컨트롤의 aria-describedby 로 잇는다.
//
// 실측 2026-09-10(qa/form-controls.cjs): 도움말 6개가 컨트롤과 연결돼 있지 않았다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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
const mod = globalThis.__describeControlsInternals;

function helper(text, id = "") {
	return { id, textContent: text, contains: () => false };
}
function control({ id = "", name = "", wrap, describedby = null }) {
	const attrs = describedby ? { "aria-describedby": describedby } : {};
	return { id, attrs, getAttribute: (n) => (n === "name" ? name || null : n in attrs ? attrs[n] : null), setAttribute: (n, v) => { attrs[n] = v; }, closest: (sel) => (sel === ".form-control" ? wrap : null) };
}
const wrapOf = (helpers) => ({ querySelectorAll: (sel) => (sel === ".label-text-alt, p.text-xs" ? helpers : []) });
const root = (controls) => ({ querySelectorAll: (sel) => (sel.startsWith("input:not") ? controls : []) });

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __describeControlsInternals 를 노출하지 않는다");
});

test("도움말에 id 를 만들고 컨트롤의 aria-describedby 로 잇는다(기존 값은 보존)", () => {
	const preview = helper("1,000,000,000");
	const wrap = wrapOf([preview]);
	const c = control({ id: "principal", wrap, describedby: "existingNote" });
	assert.equal(mod.describeControls(root([c])), 1);
	assert.equal(preview.id, "principal-help-1");
	assert.equal(c.attrs["aria-describedby"], "existingNote principal-help-1");
	assert.equal(mod.describeControls(root([c])), 0, "두 번째는 할 일이 없다");
});

test("id 가 없는 컨트롤은 name 으로, 도움말이 없거나 form-control 밖이면 건너뛴다", () => {
	const note = helper("아직 등록되지 않은 종목도 선택");
	const c1 = control({ name: "profileSymbolSelection", wrap: wrapOf([note]) });
	const c2 = control({ id: "x", wrap: wrapOf([helper("   ")]) });
	const c3 = control({ id: "y", wrap: null });
	assert.equal(mod.describeControls(root([c1, c2, c3])), 1);
	assert.match(c1.attrs["aria-describedby"], /^profileSymbolSelection-help-\d+$/);
	assert.equal(c2.attrs["aria-describedby"], undefined, "빈 도움말은 잇지 않는다");
	assert.equal(c3.attrs["aria-describedby"], undefined);
});
