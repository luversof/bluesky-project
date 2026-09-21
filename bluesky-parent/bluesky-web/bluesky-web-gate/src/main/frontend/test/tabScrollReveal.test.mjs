// 가로로 스크롤하는 탭 줄(.tabs-scroll)은 지금 탭이 줄 밖에 있으면 줄만 옆으로 옮겨 보이게 한다(2026-09-17).
//
// 큰 글꼴 폰 폭에서 탭이 글자 폭 밑으로 줄지 않고 줄이 스크롤하게 바꾸자, 시뮬레이터 '적립식 복리' 처럼 뒤쪽 탭에 있으면 줄 첫머리만
// 보였다. 페이지는 움직이면 안 된다 - scrollIntoView 대신 줄의 scrollLeft 만 계산한다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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
globalThis.HTMLElement = class HTMLElement extends globalThis.Element {};
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
const mod = globalThis.__tabScrollInternals;

/** 탭 줄 흉내: 폭 · 내용 폭 · 지금 탭 위치. */
function bar(clientWidth, scrollWidth, activeLeft, activeWidth, scrollLeft = 0) {
	const active = activeLeft == null ? null : { offsetLeft: activeLeft, offsetWidth: activeWidth };
	return { clientWidth, scrollWidth, scrollLeft, querySelector: (sel) => (sel === ".tab-active" ? active : null) };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __tabScrollInternals 를 노출하지 않는다 - npm run build");
	assert.equal(typeof mod.tabScrollLeftFor, "function");
	assert.equal(typeof mod.revealActiveTabs, "function");
	assert.equal(typeof mod.revealFocused, "function");
	assert.equal(typeof mod.focusScrollLeftFor, "function");
});

test("보이는 탭이면 그대로, 오른쪽 밖이면 오른끝을 맞추고, 왼쪽 밖이면 왼끝을 맞춘다", () => {
	assert.equal(mod.tabScrollLeftFor(0, 300, 40, 100), 0);
	assert.equal(mod.tabScrollLeftFor(0, 300, 380, 120), 200, "380 + 120 - 300");
	assert.equal(mod.tabScrollLeftFor(250, 300, 100, 90), 100);
	assert.equal(mod.tabScrollLeftFor(100, 300, 100, 300), 100, "딱 맞으면 그대로");
});

test("줄보다 넓은 탭은 글자가 시작하는 왼쪽 끝을 맞춘다", () => {
	assert.equal(mod.tabScrollLeftFor(0, 158, 176, 176), 176, "오른끝 맞춤(194)이 아니라 왼끝");
	assert.equal(mod.tabScrollLeftFor(500, 158, 360, 232), 360);
});

test("넘치는 탭 줄만 옮기고, 안 넘치거나 지금 탭이 없으면 건드리지 않는다", () => {
	const wide = bar(311, 520, 380, 130);
	const fits = bar(704, 704, 470, 230);
	const noActive = bar(311, 520, null, 0);
	const root = { querySelectorAll: (sel) => (sel === ".tabs-scroll" ? [wide, fits, noActive] : []) };
	mod.revealActiveTabs(root);
	assert.equal(wide.scrollLeft, 199, "380 + 130 - 311");
	assert.equal(fits.scrollLeft, 0);
	assert.equal(noActive.scrollLeft, 0);
});

test("키보드 포커스를 받은 요소를 보이는 폭 안에 넣는다 - 화면 좌표로 받는다", () => {
	// 기본 글꼴 375px 자산 성장 탭 줄: 줄 41..334(보이는 폭 293), 탭 309..424 -> 오른끝을 맞춘다(424 - 334 = 90)
	assert.equal(mod.focusScrollLeftFor(0, 41, 293, 309, 115), 90);
	// 보이면 그대로
	assert.equal(mod.focusScrollLeftFor(40, 41, 293, 100, 80), 40);
});

test("좁은 화면 표는 고정 첫 칸 폭을 뺀 자리에 넣는다", () => {
	// 자산 현황 375px: 상자 17..358, 고정 첫 칸 147px -> 보이는 자리 164..358(194). Shift+Tab 으로 "평가 손익" 150..225 가 첫 칸 밑에 14px 숨음
	assert.equal(mod.focusScrollLeftFor(256, 164, 194, 150, 75), 242, "왼끝을 보이는 자리 왼쪽(164)에 - 14px 되돌림");
	// 오른쪽으로 걸친 종목 링크(자산 성장): 보이는 자리 157..317(160), 링크 232..362 -> 오른끝 맞춤(362 - 317 = 45)
	assert.equal(mod.focusScrollLeftFor(0, 157, 160, 232, 130), 45);
	// 보이는 자리보다 넓은 링크는 왼끝(232 - 157 = 75)
	assert.equal(mod.focusScrollLeftFor(0, 157, 160, 232, 220), 75);
});

/** 표 구역 흉내: 행마다 칸 목록 [{ span, sticky, width }]. 행은 nextElementSibling 으로, 칸은 firstElementChild 로 잇는다. */
function tableSection(rows) {
	const section = { firstElementChild: null };
	let prevRow = null;
	return rows.map((cells) => {
		const row = { parentElement: section, nextElementSibling: null, firstElementChild: null, cells: [] };
		row.cells = cells.map((c) => ({
			rowSpan: c.span === undefined ? 1 : c.span, sticky: !!c.sticky, width: c.width || 100, parentElement: row,
			getBoundingClientRect() { return { width: this.width }; },
		}));
		row.firstElementChild = row.cells[0] || null;
		if (prevRow) prevRow.nextElementSibling = row;
		else section.firstElementChild = row;
		prevRow = row;
		return row;
	});
}

test("위 행 첫 칸이 rowspan 으로 내려오면 그 칸이 이 행의 첫 열이다 - 이 행의 첫 칸은 둘째 열", () => {
	// 월배당 시뮬 머리: 첫 줄 [표시 순서(rowspan 2) · 묶음 · 예상 월 배당금(rowspan 2)] / 둘째 줄 [주당 월배당금 · 주당 과세표준액]
	const [top, second] = tableSection([[{ span: 2 }, {}, { span: 2 }], [{}, {}]]);
	assert.equal(mod.firstColumnCell(second), top.cells[0]);
	assert.equal(mod.firstColumnCell(top), top.cells[0]);
	const [a, b, c] = tableSection([[{ span: 2 }], [{}], [{}]]);
	assert.equal(mod.firstColumnCell(c), c.cells[0], "rowspan 이 끝난 행은 자기 첫 칸");
	assert.equal(mod.firstColumnCell(b), a.cells[0]);
	const [x, , z] = tableSection([[{ span: 0 }], [{}], [{}]]);
	assert.equal(mod.firstColumnCell(z), x.cells[0], "rowspan=0 은 구역 끝까지");
	const [p, q, r] = tableSection([[{ span: 3 }], [{ span: 2 }], [{}]]);
	assert.equal(mod.firstColumnCell(r), p.cells[0], "사이 행 첫 칸(둘째 열)의 rowspan 에 속지 않는다");
	assert.equal(mod.firstColumnCell(q), p.cells[0]);
});

test("고정 첫 열 폭은 rowspan 으로 내려온 칸에서 잰다 - 그 칸 안의 요소는 늘 보인다", () => {
	const saved = globalThis.getComputedStyle;
	globalThis.getComputedStyle = (el) => ({ position: el.sticky ? "sticky" : "static" });
	try {
		const [top, second] = tableSection([[{ span: 2, sticky: true, width: 259 }, {}, { span: 2 }], [{}, {}]]);
		const inside = (cell) => ({ closest: () => cell });
		assert.equal(mod.stickyFirstCellInset(inside(second.cells[1])), 259, "둘째 줄 정렬 링크는 고정 첫 칸 폭만큼 가려진다");
		assert.equal(mod.stickyFirstCellInset(inside(top.cells[0])), -1, "고정 첫 칸 안의 링크");
		assert.equal(mod.stickyFirstCellInset(inside(top.cells[1])), 259);
		const [plain] = tableSection([[{ width: 180 }, {}]]);
		assert.equal(mod.stickyFirstCellInset(inside(plain.cells[1])), 0, "첫 열이 고정이 아니면 빼지 않는다(넓은 화면)");
		assert.equal(mod.stickyFirstCellInset({ closest: () => null }), 0, "표 밖 요소");
	} finally {
		globalThis.getComputedStyle = saved;
	}
});
