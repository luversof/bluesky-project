// 긴 화면의 구역 막대: 화면이 알려 준 구역만 모아 만든다.
//
// 실측 2026-09-10(qa/ui-nav.cjs): 배당 7.2화면·활동 5.5화면·매매 5.4화면인데 화면 안 이동 링크는 건너뛰기
// 링크 1개뿐이었고 제목에 id 가 붙은 것은 0개였다. 배당 화면의 마지막 구역은 3,185px 아래에 있었다.
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄워 확인한다.
import assert from "node:assert/strict";
import test from "node:test";

const noop = () => {};
class FakeElement {
	constructor(tag) {
		this.tagName = String(tag || "div").toUpperCase();
		this.attrs = {};
		this.children = [];
		this.classList = { add: noop, remove: noop, toggle: noop, contains: () => false };
		this.style = {};
		this.dataset = {};
		this.id = "";
		this._text = "";
		this.offsetParent = {};
		this.top = 0;
	}
	get firstChild() { return this.children[0] || null; }
	setAttribute(n, v) { this.attrs[n] = String(v); }
	getAttribute(n) { return n in this.attrs ? this.attrs[n] : null; }
	removeAttribute(n) { delete this.attrs[n]; }
	hasAttribute(n) { return n in this.attrs; }
	appendChild(c) { this.children.push(c); c.parent = this; return c; }
	insertBefore(c) { this.children.unshift(c); c.parent = this; return c; }
	remove() { if (this.parent) this.parent.children = this.parent.children.filter((x) => x !== this); }
	matches(sel) { return sel.replace(/[[\]]/g, "") in this.attrs; }
	closest(sel) { let n = this; while (n) { if (n.matches && n.matches(sel)) return n; n = n.parent; } return null; }
	querySelector(sel) { return this.querySelectorAll(sel)[0] || null; }
	querySelectorAll(sel) {
		const wanted = sel.split(",").map((s) => s.trim());
		const out = [];
		const walk = (n) => { for (const c of n.children) {
			if (wanted.some((w) => (w.startsWith("[") ? c.matches(w) : c.tagName === w.toUpperCase()))) out.push(c);
			walk(c);
		} };
		walk(this);
		return out;
	}
	get parentElement() { return this.parent || null; }
	contains(node) { if (node === this) return true; for (const c of this.children) { if (c.contains && c.contains(node)) return true; } return false; }
	getClientRects() { return [{}]; }
	getBoundingClientRect() { return { top: this.top, bottom: this.top + 100, height: 100, width: 200, left: 0, right: 200 }; }
	get textContent() { return this._text + this.children.map((c) => c.textContent).join(""); }
	set textContent(v) { this._text = v; this.children = []; }
}

globalThis.Element = class Element {};
globalThis.Document = class Document {};
const docListeners = [];
const main = new FakeElement("main");
main.id = "mainContent";
main.setAttribute("data-section-nav-label", "이 화면의 구역");
globalThis.document = Object.assign(new FakeElement("document"), {
	documentElement: new FakeElement("html"),
	body: Object.assign(new FakeElement("body"), { scrollHeight: 4000 }),
	head: new FakeElement("head"),
	title: "t",
	readyState: "complete",
	getElementById: (id) => (id === "mainContent" ? main : null),
	createElement: (tag) => new FakeElement(tag),
	createTextNode: () => ({}),
	createTreeWalker: () => ({ nextNode: () => null }),
	addEventListener: (type, fn) => { docListeners.push({ type, fn }); },
});
globalThis.NodeFilter = { SHOW_TEXT: 4 };
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.location = { search: "", href: "https://x/stock", origin: "https://x", pathname: "/stock" };
globalThis.history = { replaceState: noop, pushState: noop };
const stored = {};
globalThis.localStorage = { getItem: (k) => (k in stored ? stored[k] : null), setItem: (k, v) => { stored[k] = v; }, removeItem: (k) => { delete stored[k]; } };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.removeEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);
globalThis.innerHeight = 800;


await import("../../resources/static/js/common.js");
const mod = globalThis.__sectionNavInternals;

function section(label, { value = null, top = 0, id = "" } = {}) {
	const el = new FakeElement("h2");
	el.setAttribute("data-page-section", value === null ? "" : value);
	el.textContent = label;
	el.top = top;
	el.id = id;
	return el;
}

function scene(sections) {
	main.children = [];
	sections.forEach((s) => main.appendChild(s));
	return main;
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod && typeof mod.renderSectionNav === "function", "common.js 가 renderSectionNav 를 노출하지 않는다");
});

test("표식 값이 있으면 그 값이, 없으면 글자가 이름이 된다", () => {
	assert.equal(mod.sectionLabel(section("월별 배당금")), "월별 배당금");
	assert.equal(mod.sectionLabel(section("아무 글자", { value: "상세 목록" })), "상세 목록");
});

test("구역이 3개면 막대를 만들고 링크를 그만큼 단다", () => {
	scene([section("가", { top: 0 }), section("나", { top: 500 }), section("다", { top: 900 })]);
	assert.equal(mod.renderSectionNav(), true);
	const nav = main.querySelector("[data-page-section-nav]");
	assert.ok(nav, "막대를 만들지 않았다");
	assert.equal(nav.querySelectorAll("a").length, 3);
	assert.equal(nav.getAttribute("aria-label"), "이 화면의 구역");
	assert.equal(main.firstChild, nav, "본문 맨 앞에 두지 않았다");
});

test("구역이 2개뿐이면 만들지 않는다 - 짧은 화면에서는 자리만 차지한다", () => {
	scene([section("가"), section("나")]);
	assert.equal(mod.renderSectionNav(), false);
	assert.equal(main.querySelector("[data-page-section-nav]"), null);
});

test("id 가 없는 구역에는 붙여 준다 - 없으면 뛰어갈 수 없다", () => {
	const withId = section("가", { id: "keepMe" });
	const bare = section("나");
	scene([withId, bare, section("다")]);
	mod.renderSectionNav();
	assert.equal(withId.id, "keepMe", "이미 있는 id 를 바꿨다");
	assert.ok(bare.id, "id 를 붙이지 않았다");
	const hrefs = main.querySelector("[data-page-section-nav]").querySelectorAll("a").map((a) => a.href);
	assert.deepEqual(hrefs.slice(0, 2), ["#keepMe", "#" + bare.id]);
});

test("다시 그리면 막대가 겹쳐 쌓이지 않는다", () => {
	scene([section("가"), section("나"), section("다")]);
	mod.renderSectionNav();
	mod.renderSectionNav();
	assert.equal(main.querySelectorAll("[data-page-section-nav]").length, 1);
});

test("구역을 모두 담는 가장 가까운 조상에 넣는다 - 본문 열을 벗어나면 사이드바에 가린다", () => {
	main.children = [];
	const column = new FakeElement("div");
	main.appendChild(column);
	const a = section("가"), b = section("나"), c = section("다");
	[a, b, c].forEach((s) => column.appendChild(s));
	assert.equal(mod.sectionsContainer([a, b, c], main), column);
	mod.renderSectionNav();
	const nav = main.querySelector("[data-page-section-nav]");
	assert.equal(column.firstChild, nav, "본문 열 맨 앞에 두지 않았다");
});

test("칩이 화면보다 넓으면 지금 보는 칩이 보이도록 막대만 옆으로 굴린다", () => {
	const a = section("가", { top: -400 }), b = section("나", { top: -100 }), c = section("다", { top: 600 });
	scene([a, b, c]);
	mod.renderSectionNav();
	const nav = main.querySelector("[data-page-section-nav]");
	const list = nav.querySelector("ul");
	// 스텁에 폭을 준다: 보이는 폭 100, 내용 폭 900, 현재 칩(두 번째)은 400 지점
	list.clientWidth = 100;
	list.scrollWidth = 900;
	list.scrollLeft = 0;
	const links = nav.querySelectorAll("a");
	links.forEach((l, i) => { l.offsetLeft = i * 400; l.offsetWidth = 80; });
	mod.markCurrentSection(nav, [a, b, c]);
	assert.equal(list.scrollLeft, 400 + 80 - 100 + 16, "현재 칩이 보이도록 굴리지 않았다");
});

test("짧은 화면에는 구역이 3개여도 만들지 않는다", () => {
	const before = document.body.scrollHeight;
	document.body.scrollHeight = 900; // 뷰포트 800 의 두 배 미만
	scene([section("가"), section("나"), section("다")]);
	assert.equal(mod.renderSectionNav(), false);
	assert.equal(main.querySelector("[data-page-section-nav]"), null);
	document.body.scrollHeight = before;
});

test("지금 보고 있는 구역만 표시한다", () => {
	const a = section("가", { top: -400 }), b = section("나", { top: -100 }), c = section("다", { top: 600 });
	scene([a, b, c]);
	mod.renderSectionNav();
	const nav = main.querySelector("[data-page-section-nav]");
	const links = nav.querySelectorAll("a");
	assert.equal(links[1].getAttribute("aria-current"), "true", "두 번째 구역이 현재여야 한다");
	assert.equal(links[0].getAttribute("aria-current"), null);
	assert.equal(links[2].getAttribute("aria-current"), null);
});

test("구역 막대는 그리기 전(afterSwap)에 끼운다 - afterSettle 뿐이면 아래가 밀린다", () => {
	// 실측 2026-09-10(qa/nav-ab2.cjs): afterSettle 에서만 만들던 때 배당 CLS 0.023 · 매매 0.0252,
	// 막대를 숨기면 0.0001 · 0 이었다. afterSwap 은 교체와 같은 태스크라 그리기 전에 들어간다.
	const swap = docListeners.filter((l) => l.type === "htmx:afterSwap");
	assert.ok(swap.length > 0, "htmx:afterSwap 을 듣지 않는다 - 막대가 늦게 끼어들어 본문을 민다");
	scene([section("가", { top: 0 }), section("나", { top: 500 }), section("다", { top: 900 })]);
	assert.equal(main.querySelector("[data-page-section-nav]"), null);
	swap.forEach((l) => l.fn({}));
	assert.ok(main.querySelector("[data-page-section-nav]"), "afterSwap 이 막대를 만들지 않았다");
});
