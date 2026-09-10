// 선택 가능한 표의 행 체크박스: 공용 처리 하나가 표 다섯 곳을 모두 덮는다.
//
// 실측 2026-09-10(qa/selectable-rows-a11y.cjs): 선택을 tr 의 aria-selected 로만 표시하던 표 네 곳
// (자산 현황 종목 9행·계좌 5행, 배당 수익률 14행, 매매 실현손익 8행)에서 행을 고른 뒤 그 행의 접근성 노드
// 속성이 focusable/focused 뿐이었다 - 순수 table 안의 row 는 aria-selected 를 접근성 트리에 내보내지 않는다.
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
		this.checked = false;
		this.clicked = 0;
	}
	get firstChild() { return this.children[0] || null; }
	setAttribute(name, value) { this.attrs[name] = String(value); }
	getAttribute(name) { return name in this.attrs ? this.attrs[name] : null; }
	removeAttribute(name) { delete this.attrs[name]; }
	hasAttribute(name) { return name in this.attrs; }
	appendChild(child) { this.children.push(child); child.parent = this; return child; }
	insertBefore(child, before) { this.children.unshift(child); child.parent = this; return child; }
	matches(selector) { return selector.replace(/[[\]]/g, "") in this.attrs; }
	closest(selector) { let node = this; while (node) { if (node.matches && node.matches(selector)) return node; node = node.parent; } return null; }
	querySelector(selector) { return this.querySelectorAll(selector)[0] || null; }
	querySelectorAll(selector) {
		const wanted = selector.split(",").map((s) => s.trim());
		const out = [];
		const walk = (node) => {
			for (const child of node.children) {
				if (wanted.some((w) => (w.startsWith("[") ? child.matches(w) : child.tagName === w.toUpperCase()))) out.push(child);
				walk(child);
			}
		};
		walk(this);
		return out;
	}
	click() { this.clicked++; if (this.onclick) this.onclick(); }
	cloneNode() {
		const copy = new FakeElement(this.tagName);
		copy.attrs = { ...this.attrs };
		copy._text = this._text; // 자식 글자까지 합친 값을 넣으면 잡음을 걷어내도 남는다
		copy.children = this.children.map((c) => { const cc = c.cloneNode(true); cc.parent = copy; return cc; });
		return copy;
	}
	remove() { if (this.parent) this.parent.children = this.parent.children.filter((c) => c !== this); }
	get textContent() {
		if (this._text !== undefined && this.children.length === 0) return this._text;
		return (this._text || "") + this.children.map((c) => c.textContent).join(" ");
	}
	set textContent(value) { this._text = value; }
}

globalThis.Element = class Element {};
globalThis.Document = class Document {};
globalThis.document = Object.assign(new FakeElement("document"), {
	documentElement: new FakeElement("html"),
	body: new FakeElement("body"),
	head: new FakeElement("head"),
	title: "t",
	readyState: "complete",
	getElementById: () => null,
	createElement: (tag) => new FakeElement(tag),
	createTextNode: () => ({}),
	createTreeWalker: () => ({ nextNode: () => null }),
	addEventListener: noop,
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
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const mod = globalThis.__rowSelectInternals;

/** 첫 칸에 글자가 있는 선택 가능한 행 하나. */
function row({ template = "{0} 선택", selected = false, label = "삼성전자", tabindex = "0" } = {}) {
	const tr = new FakeElement("tr");
	tr.setAttribute("data-row-select", template);
	tr.setAttribute("aria-selected", selected ? "true" : "false");
	if (tabindex !== null) tr.setAttribute("tabindex", tabindex);
	const td = new FakeElement("td");
	td.textContent = "  " + label + "\n  ";
	tr.appendChild(td);
	tr.onclick = () => tr.setAttribute("aria-selected", tr.getAttribute("aria-selected") === "true" ? "false" : "true");
	return tr;
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod && typeof mod.ensureRowCheckbox === "function", "common.js 가 ensureRowCheckbox 를 노출하지 않는다");
});

test("행 첫 칸에 이름 있는 체크박스를 만들어 넣는다", () => {
	const tr = row();
	assert.equal(mod.ensureRowCheckbox(tr), true);
	const box = tr.querySelector("[data-row-select-checkbox]");
	assert.ok(box, "체크박스를 만들지 않았다");
	assert.equal(box.getAttribute("aria-label"), "삼성전자 선택");
	assert.equal(box.type, "checkbox");
	assert.equal(tr.querySelector("td").firstChild.className, "row-select-box", "첫 칸 맨 앞에 넣지 않았다");
});

test("행이 스스로 탭 정지였으면 뗀다 - 행마다 탭 정지가 둘이 되지 않게", () => {
	const tr = row();
	mod.ensureRowCheckbox(tr);
	assert.equal(tr.getAttribute("tabindex"), null);
});

test("이미 선택된 행이면 체크된 채로 만든다", () => {
	const tr = row({ selected: true });
	mod.ensureRowCheckbox(tr);
	assert.equal(tr.querySelector("[data-row-select-checkbox]").checked, true);
});

test("두 번 불러도 하나만 만든다", () => {
	const tr = row();
	assert.equal(mod.ensureRowCheckbox(tr), true);
	assert.equal(mod.ensureRowCheckbox(tr), false);
	assert.equal(tr.querySelectorAll("[data-row-select-checkbox]").length, 1);
});

test("체크박스가 켜지면 행을 클릭해 기존 선택 코드를 태운다", () => {
	const tr = row();
	mod.ensureRowCheckbox(tr);
	const box = tr.querySelector("[data-row-select-checkbox]");
	box.checked = true;
	mod.reconcileRowSelection(box);
	assert.equal(tr.clicked, 1, "행을 클릭하지 않았다");
	assert.equal(tr.getAttribute("aria-selected"), "true");
});

test("이미 맞으면 행을 건드리지 않는다", () => {
	const tr = row({ selected: true });
	mod.ensureRowCheckbox(tr);
	const box = tr.querySelector("[data-row-select-checkbox]");
	mod.reconcileRowSelection(box);
	assert.equal(tr.clicked, 0);
});

test("첫 칸의 펼침 버튼·끌기 손잡이는 이름에서 걷어낸다", () => {
	const tr = row({ label: "KB증권 위탁" });
	const td = tr.querySelector("td");
	const handle = new FakeElement("span");
	handle.setAttribute("data-profile-order-handle", "");
	handle.textContent = "::";
	td.children.unshift(Object.assign(handle, { parent: td }));
	const button = new FakeElement("button");
	button.textContent = "보유 종목 보기 (3)";
	td.appendChild(button);
	mod.ensureRowCheckbox(tr);
	assert.equal(tr.querySelector("[data-row-select-checkbox]").getAttribute("aria-label"), "KB증권 위탁 선택");
});

test("{0} 이 없는 이름틀은 그대로 쓴다", () => {
	assert.equal(mod.rowSelectName(row({ template: "선택" }), "선택"), "선택");
});
