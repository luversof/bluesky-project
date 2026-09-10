// 한국어 데이터 표시(WCAG 3.1.2): 영어 화면에서 한글을 직접 품은 요소에 lang="ko" 를 붙인다.
//
// 실측 2026-09-10(qa/lang-of-parts.cjs): 영어 화면(문서 lang=en-US) 12개에 한글 텍스트 1,190곳(종목명·계좌명)이 표시 없이 있었다.
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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

/** 최소 DOM: el(tag, text, children) 트리와 그 위를 도는 TreeWalker 흉내. 관찰자가 instanceof Element 로 거르므로 실제 인스턴스여야 한다. */
function el(tag, text, children = []) {
	const node = Object.assign(new globalThis.Element(), {
		tagName: tag.toUpperCase(), attrs: {}, childNodes: [], parentElement: null,
		hasAttribute(n) { return n in this.attrs; },
		getAttribute(n) { return n in this.attrs ? this.attrs[n] : null; },
		setAttribute(n, v) { this.attrs[n] = v; },
	});
	if (text != null) node.childNodes.push({ nodeType: 3, textContent: text, parentElement: node });
	for (const c of children) { c.parentElement = node; node.childNodes.push(c); }
	return node;
}
function walk(root) {
	const texts = [];
	(function visit(n) { for (const c of n.childNodes || []) { if (c.nodeType === 3) texts.push(c); else visit(c); } })(root);
	let i = 0;
	return { nextNode: () => (i < texts.length ? texts[i++] : null) };
}

globalThis.NodeFilter = { SHOW_TEXT: 4 };
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "en-US" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: noop, createTreeWalker: (scope) => walk(scope),
});
globalThis.Document = class Document {};
globalThis.window = globalThis;
let observers = [];
globalThis.MutationObserver = class { constructor(cb) { this.cb = cb; this.observed = []; observers.push(this); } observe(target, opts) { this.observed.push([target, opts]); } disconnect() {} };
globalThis.location = { search: "", href: "https://x/stock", origin: "https://x", pathname: "/stock" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const mod = globalThis.__langPartsInternals;

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __langPartsInternals 를 노출하지 않는다");
});

test("영어 문서: 한글을 직접 품은 요소에만 lang=ko 를 붙인다", () => {
	const name = el("a", "삼성전자");
	const amount = el("td", "1,234,567");
	const row = el("tr", null, [name, amount]);
	const root = el("table", null, [row]);
	assert.equal(mod.markKoreanParts(root, "en-US"), 1);
	assert.equal(name.getAttribute("lang"), "ko");
	assert.equal(amount.getAttribute("lang"), null, "숫자만 있는 칸은 건드리지 않는다");
	assert.equal(row.getAttribute("lang"), null, "자식이 품은 한글로 조상까지 표시하지 않는다");
});

test("한국어 문서에서는 아무것도 하지 않는다(문서 언어가 이미 한국어)", () => {
	const name = el("a", "삼성전자");
	const root = el("div", null, [name]);
	assert.equal(mod.markKoreanParts(root, "ko-KR"), 0);
	assert.equal(name.getAttribute("lang"), null);
	assert.equal(mod.isKoreanDocument("ko-KR"), true);
	assert.equal(mod.isKoreanDocument("en-US"), false);
});

test("이미 표시된 요소·가지는 다시 붙이지 않는다(여러 번 불러도 안전)", () => {
	const marked = el("span", "한국투자증권");
	marked.setAttribute("lang", "ko");
	const insideKo = el("span", "위탁");
	const koBranch = el("div", null, [insideKo]);
	koBranch.setAttribute("lang", "ko");
	const fresh = el("span", "KB증권 위탁");
	const root = el("div", null, [marked, koBranch, fresh]);
	assert.equal(mod.markKoreanParts(root, "en-US"), 1, "새로 붙는 것은 표시 없는 하나뿐");
	assert.equal(fresh.getAttribute("lang"), "ko");
	assert.equal(insideKo.getAttribute("lang"), null, "조상이 ko 면 그대로 둔다");
	assert.equal(mod.markKoreanParts(root, "en-US"), 0, "두 번째 호출은 새로 붙일 것이 없다");
	assert.equal(mod.hasKoreanLangAncestor(insideKo), true);
	assert.equal(mod.hasKoreanLangAncestor(fresh), true, "방금 붙은 뒤에는 자신이 ko");
});

test("한글과 숫자가 섞인 칸도 요소째 표시한다, 스크립트는 제외", () => {
	const mixed = el("div", "476800 · KODEX 한국부동산리츠인프라");
	const script = el("script", "var x = '삼성전자';");
	const root = el("div", null, [mixed, script]);
	assert.equal(mod.markKoreanParts(root, "en-US"), 1);
	assert.equal(mixed.getAttribute("lang"), "ko");
	assert.equal(script.getAttribute("lang"), null);
});

// 실측 2026-09-10: common.js 는 <head> 동기 로드라 첫 호출 때 body 가 비어 있고, outerHTML 교체에서는 afterSettle 의 target 이
// 떨어져 나간 옛 요소다. 그래서 준비 시점과 교체 뒤에는 문서 전체를 훑되, 이미 표시된 것은 건너뛴다.
test("문서 전체를 반복해 훑어도 새로 붙는 것만 센다(교체 뒤 재훑기 비용)", () => {
	const a = el("a", "삼성전자");
	const b = el("a", "KODEX 200타겟위클리커버드콜");
	const latin = el("a", "KODEX 200");
	const root = el("div", null, [a, b, latin]);
	assert.equal(mod.markKoreanParts(root, "en-US"), 2, "한글 없는 표기(KODEX 200)는 세지 않는다");
	assert.equal(latin.getAttribute("lang"), null);
	assert.equal(mod.markKoreanParts(root, "en-US"), 0, "두 번째 훑기는 새로 붙일 것이 없다");
	const added = el("a", "한국투자증권 위탁");
	added.parentElement = root; root.childNodes.push(added);
	assert.equal(mod.markKoreanParts(root, "en-US"), 1, "교체로 들어온 새 요소만 표시한다");
	assert.equal(added.getAttribute("lang"), "ko");
});

// 실측 2026-09-10(qa/lang-leftover.cjs): afterSettle 뒤 스크립트가 만드는 DOM(도넛 범례 span, 차트 요약 p.sr-only)이 배당 11곳·매매 9곳 남았다.
test("관찰자: 더해진 가지만 훑어 늦게 생긴 DOM 도 표시한다", () => {
	observers = [];
	const observer = mod.observeKoreanParts();
	assert.ok(observer, "영어 문서에서는 관찰자를 만든다");
	assert.equal(observers.length, 1);
	assert.deepEqual(observers[0].observed[0][1], { childList: true, subtree: true }, "속성 변경은 보지 않는다(표시 자체가 속성 변경이라 되먹임 방지)");
	const late = el("span", "KODEX 한국부동산리츠인프라");
	const legend = el("div", null, [late]);
	observers[0].cb([{ addedNodes: [legend] }]);
	assert.equal(late.getAttribute("lang"), "ko");
	const text = { nodeType: 3, textContent: "삼성전자" };
	assert.doesNotThrow(() => observers[0].cb([{ addedNodes: [text] }]), "요소가 아닌 노드는 건너뛴다");
});

// 실측 2026-09-10(qa/lang-observer-debug.cjs): 관찰자 시작을 "이미 로드됨" 갈래에만 두어 실제 화면(head 로드 시 readyState="loading")에서는
// 관찰자가 생기지 않았고, 교체본 319곳이 통째로 표시되지 않았다. 두 갈래 모두 훑기+관찰자를 시작해야 한다.
test("DOM 준비 전에 실행돼도 준비되면 훑기와 관찰자가 함께 시작한다", () => {
	observers = [];
	const listeners = {};
	const saved = { readyState: globalThis.document.readyState, addEventListener: globalThis.document.addEventListener };
	globalThis.document.readyState = "loading";
	globalThis.document.addEventListener = (type, fn) => { listeners[type] = fn; };
	try {
		mod.markKoreanPartsWhenReady();
		assert.equal(observers.length, 0, "준비 전에는 관찰자를 만들지 않는다");
		assert.equal(typeof listeners.DOMContentLoaded, "function", "DOMContentLoaded 를 기다려야 한다");
		listeners.DOMContentLoaded();
		assert.equal(observers.length, 1, "준비되면 관찰자가 시작한다");
	} finally {
		globalThis.document.readyState = saved.readyState;
		globalThis.document.addEventListener = saved.addEventListener;
	}
	observers = [];
	mod.markKoreanPartsWhenReady();
	assert.equal(observers.length, 1, "이미 로드된 경우엔 바로 시작한다");
});
