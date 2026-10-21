// 툴팁 말풍선이 화면 밖으로 나가지 않게 좌/우 끝 맞춤을 고르는 규칙.
//
// 실측 2026-09-11(활동 화면 375px): 트리거가 [309,335] 인 "+N" 계좌 칩의 말풍선(176px)이 [234,410] 으로 놓여
// 35px 잘렸다. 숨어 있어도 레이아웃에 남아 문서가 411px 이 됐다(가로 스크롤 36px). 768px 에서도 [480,800] 이었다.
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
	documentElement: Object.assign(stubEl(), { lang: "ko-KR", clientWidth: 375 }), body: stubEl(), head: stubEl(), title: "t",
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
globalThis.getComputedStyle = () => ({ width: "0px" });

await import("../../resources/static/js/common.js");
const mod = globalThis.__tooltipAlignInternals;

test("가운데로 들어가면 그대로 둔다", () => {
	// 화면 한가운데 트리거: [180,200], 말풍선 176 -> [102,278]
	assert.equal(mod.tooltipAlign(180, 200, 176, 375), "center");
});

test("오른쪽 가장자리 트리거는 오른쪽 끝 맞춤", () => {
	// 실측값: el[309,335] bubble 176 -> 가운데 맞춤이면 오른쪽이 410 (화면 375)
	assert.equal(mod.tooltipAlign(309, 335, 176, 375), "end");
	// 끝 맞춤 결과 [159,335] 는 화면 안
	assert.ok(335 - 176 >= 8);
});

test("왼쪽 가장자리 트리거는 왼쪽 끝 맞춤", () => {
	// 실측값: el[64,95] bubble 176 -> 가운데 맞춤이면 왼쪽이 -9
	assert.equal(mod.tooltipAlign(64, 95, 176, 414), "start");
});

test("넓은 화면에서도 오른쪽 끝 트리거는 끝 맞춤", () => {
	// 실측값: 768px 에서 el[626,653] bubble 320 -> [480,800]
	assert.equal(mod.tooltipAlign(626, 653, 320, 768), "end");
});

test("말풍선이 화면보다 넓으면 남는 공간이 큰 쪽에 붙인다", () => {
	// 트리거가 오른쪽에 있으면 왼쪽 공간이 넓다 -> end(왼쪽으로 펼침)
	assert.equal(mod.tooltipAlign(300, 320, 500, 375), "end");
	// 트리거가 왼쪽이면 반대
	assert.equal(mod.tooltipAlign(20, 40, 500, 375), "start");
});

test("폭을 못 재면 건드리지 않는다", () => {
	assert.equal(mod.tooltipAlign(309, 335, 0, 375), "center");
	assert.equal(mod.tooltipAlign(309, 335, 176, 0), "center");
});

// 실측 2026-10-21(대시보드 1280px): 고정 옆 메뉴가 [0,224] 를 덮는다. 화면 기준이면 트리거 [345,369] 의 말풍선 320 은
// 가운데 [197,517] 로 놓여 왼쪽 27px 이 메뉴에 가렸다. 본문 칸 [224,1280] 기준이면 시작점 맞춤 [345,665].
test("본문 칸이 표시돼 있으면 그 칸을 경계로 잰다", () => {
	const host = { getBoundingClientRect: () => ({ left: 224, right: 1280 }) };
	const el = { closest: (s) => (s === "[data-tooltip-bounds]" ? host : null) };
	const b = mod.tooltipBounds(el, 1280);
	assert.deepEqual(b, { left: 224, width: 1056 });
	assert.equal(mod.tooltipAlign(345, 369, 320, 1280), "center", "화면 기준이면 가운데(메뉴 밑으로 들어감)");
	assert.equal(mod.tooltipAlign(345 - b.left, 369 - b.left, 320, b.width), "start");
});

test("표시가 없거나 칸이 화면 밖이면 화면 전체", () => {
	assert.deepEqual(mod.tooltipBounds({ closest: () => null }, 375), { left: 0, width: 375 });
	const off = { getBoundingClientRect: () => ({ left: 400, right: 900 }) };
	assert.deepEqual(mod.tooltipBounds({ closest: () => off }, 375), { left: 0, width: 375 });
	// 칸이 화면보다 넓으면 화면과 겹치는 부분만
	const wide = { getBoundingClientRect: () => ({ left: -50, right: 2000 }) };
	assert.deepEqual(mod.tooltipBounds({ closest: () => wide }, 1280), { left: 0, width: 1280 });
});

test("세로 변형과 마크업이 고른 끝 맞춤은 건너뛴다", () => {
	const el = (names) => ({ classList: { contains: (n) => names.includes(n) } });
	assert.equal(mod.tooltipAlignable(el(["tooltip", "tooltip-bottom"])), true);
	assert.equal(mod.tooltipAlignable(el(["tooltip", "tooltip-right"])), false);
	assert.equal(mod.tooltipAlignable(el(["tooltip", "tooltip-bottom-end"])), false);
	assert.equal(mod.tooltipAlignable(el(["tooltip", "tooltip-bottom-start"])), false);
});
