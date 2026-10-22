// 표 안 태그 풍선(data-stock-tag-tooltip)을 지킨다.
//
// 2026-10-22 까지 assetStatus.ts 와 dividendHistory.ts 가 같은 구현을 한 벌씩 들고 있었다(먼저 실린 쪽이 정의). 한 벌로 모아
// stockLayout 이 싣는 stockTagTooltip.ts 로 옮겼다 - 여기서는 ① 정의가 그 파일 한 곳뿐이고 레이아웃이 싣는지,
// ② 같은 대상을 여러 번 초기화해도 한 번만 묶는지, ③ 풍선 위치가 화면 오른쪽 · 아래 가장자리에서 안쪽으로 접히는지 본다.
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

function fakeClassList(initial) {
	const set = new Set(initial.split(/\s+/).filter(Boolean));
	return {
		add: (c) => set.add(c),
		remove: (c) => set.delete(c),
		contains: (c) => set.has(c),
	};
}

const created = [];
const winListeners = [];
globalThis.window = globalThis;
globalThis.innerWidth = 1000;
globalThis.innerHeight = 800;
globalThis.addEventListener = (type, fn, capture) => winListeners.push({ type, fn, capture });

let targets = [];
globalThis.document = {
	createElement() {
		const el = {
			id: "",
			style: {},
			textContent: "",
			rect: { width: 200, height: 50 },
			getBoundingClientRect() {
				return this.rect;
			},
			set className(v) {
				this.classList = fakeClassList(v);
			},
		};
		created.push(el);
		return el;
	},
	body: { appendChild() {} },
	querySelectorAll: () => targets,
};

function fakeTarget(message) {
	const listeners = {};
	return {
		dataset: { stockTagTooltip: message },
		listeners,
		addEventListener(type, fn) {
			(listeners[type] ||= []).push(fn);
		},
		fire(type, clientX, clientY) {
			for (const fn of listeners[type] || []) fn.call(this, { clientX, clientY });
		},
	};
}

await import("../../resources/static/js/stock/stockTagTooltip.js");
const init = globalThis.initializeStockTagHoverTooltips;

test("정의는 stockTagTooltip 한 곳 - 조각 스크립트는 부르기만, 레이아웃이 싣는다", () => {
	assert.equal(typeof init, "function");
	for (const name of ["assetStatus", "dividendHistory"]) {
		const src = readFileSync(`src/stock/${name}.ts`, "utf8");
		assert.doesNotMatch(src, /initializeStockTagHoverTooltips\s*=(?!=)/, `${name}.ts 가 다시 정의한다`);
		assert.match(src, /win\.initializeStockTagHoverTooltips\(document\)/, `${name}.ts 가 부르지 않는다`);
	}
	const layout = readFileSync("../jte/_layout/stockLayout.jte", "utf8");
	assert.match(layout, /<script src="\/js\/stock\/stockTagTooltip\.js"><\/script>/);
});

test("같은 대상을 몇 번 초기화해도 한 번만 묶고, 풍선 요소는 하나", () => {
	const a = fakeTarget("가");
	targets = [a];
	init(document);
	init(document);
	targets = [a, fakeTarget("나")];
	init(null);
	assert.equal(a.listeners.mouseenter.length, 1);
	assert.equal(a.listeners.mousemove.length, 1);
	assert.equal(a.listeners.mouseleave.length, 1);
	assert.equal(created.length, 1);
	assert.equal(created[0].id, "stockTagHoverTooltip");
	// 스크롤(캡처) · 크기 바뀜에 숨기는 리스너도 한 번만.
	assert.deepEqual(
		winListeners.map((l) => `${l.type}:${l.capture === true}`),
		["scroll:true", "resize:false"],
	);
});

test("보이기 · 따라가기 · 가장자리 접기 · 빈 문구 · 숨기기", () => {
	const el = created[0];
	const t = fakeTarget("설명");
	targets = [t];
	init(document);

	t.fire("mouseenter", 100, 100);
	assert.equal(el.textContent, "설명");
	assert.equal(el.classList.contains("hidden"), false);
	assert.deepEqual([el.style.left, el.style.top], ["112px", "118px"]);

	// 오른쪽: 100+12 대신 innerWidth - 너비 - 12 = 788. 아래: 커서 위로(780 - 50 - 18 = 712).
	t.fire("mousemove", 990, 780);
	assert.deepEqual([el.style.left, el.style.top], ["788px", "712px"]);

	t.fire("mouseleave");
	assert.equal(el.classList.contains("hidden"), true);
	assert.equal(el.textContent, "");

	// 숨은 채로는 움직여도 자리를 바꾸지 않는다.
	t.fire("mousemove", 10, 10);
	assert.deepEqual([el.style.left, el.style.top], ["788px", "712px"]);

	// 빈 문구는 띄우지 않는다.
	const empty = fakeTarget("");
	targets = [empty];
	init(document);
	empty.fire("mouseenter", 100, 100);
	assert.equal(el.classList.contains("hidden"), true);

	// 스크롤하면 숨는다.
	t.fire("mouseenter", 100, 100);
	winListeners.find((l) => l.type === "scroll").fn();
	assert.equal(el.classList.contains("hidden"), true);
});
