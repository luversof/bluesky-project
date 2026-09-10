// htmx 교체 뒤 빈 차트 보정용 resize() 는 정수 크기가 실제로 달라졌을 때만 부른다.
//
// 실측 2026-09-10(qa/chart-resize-trace.cjs): Chart.js 4.5 retinaScale 은 컨테이너 폭을 0.1 단위로 반올림(570.4)해 정수 canvas.width(570)와
// 비교하므로 소수 폭에선 resize() 마다 전부 다시 그렸다(배당 780점 ~100ms, 종목 주가 3,198점 ~256ms @4x). 빌드 산출물(stock-charts.js)을 띄운다.
import assert from "node:assert/strict";
import test from "node:test";

const noop = () => {};
globalThis.document = {
	getElementById: () => null,
	createElement: (tag) => (tag === "canvas" ? { getContext: () => ({ fillRect() {}, getImageData: () => ({ data: [0, 0, 0, 255] }) }) } : { className: "", textContent: "", style: {}, dataset: {} }),
	body: { dataset: {} }, documentElement: { lang: "ko" }, addEventListener: noop,
};
globalThis.getComputedStyle = () => ({ getPropertyValue: () => "" });
globalThis.window = globalThis;
globalThis.Chart = { defaults: {}, instances: {}, register: noop };
globalThis.MutationObserver = class { observe() {} disconnect() {} };

await import("../../resources/static/js/stock-charts.js");
const mod = globalThis.__chartResizeInternals;

function chart({ parentW = 570, parentH = 280, width = 570.4, height = 280, keepRatio = false, dpr = 1, canvasW, canvasH } = {}) {
	const c = { width, height, currentDevicePixelRatio: dpr, options: { maintainAspectRatio: keepRatio }, resized: 0, resize() { this.resized++; }, canvas: { width: canvasW === undefined ? Math.floor(width * dpr) : canvasW, height: canvasH === undefined ? Math.floor(height * dpr) : canvasH, parentElement: { clientWidth: parentW, clientHeight: parentH } } };
	return c;
}

test("내부 함수가 노출돼 있고 StockCharts API 에도 붙어 있다", () => {
	assert.ok(mod, "stock-charts.js 가 __chartResizeInternals 를 노출하지 않는다");
	assert.equal(typeof globalThis.StockCharts.resizeIfChanged, "function");
});

test("소수 폭(570.4)이라도 정수 크기가 같으면 resize 를 부르지 않는다(실측 재현)", () => {
	const c = chart();
	assert.equal(mod.chartSizeChanged(c), false);
	assert.equal(mod.resizeIfChanged(c), false);
	assert.equal(c.resized, 0);
});

test("폭이 달라졌으면 부른다; 높이는 maintainAspectRatio 가 false 일 때만 본다", () => {
	const wide = chart({ parentW: 700 });
	assert.equal(mod.resizeIfChanged(wide), true);
	assert.equal(wide.resized, 1);
	const taller = chart({ parentH: 320 });
	assert.equal(mod.resizeIfChanged(taller), true, "높이 고정 차트(maintainAspectRatio:false)는 높이 변화도 본다");
	const ratio = chart({ parentH: 320, keepRatio: true });
	assert.equal(mod.resizeIfChanged(ratio), false, "비율 유지 차트는 높이가 폭에서 나오므로 폭만 본다");
});

test("캔버스가 붙어 있지 않거나 크기 0 이면 아무 일도 하지 않는다", () => {
	const detached = chart(); detached.canvas.parentElement = null;
	assert.equal(mod.resizeIfChanged(detached), false);
	assert.equal(mod.resizeIfChanged(null), false);
	assert.equal(mod.resizeIfChanged({}), false);
	const hidden = chart({ parentW: 0, parentH: 0, width: 0, height: 0 });
	assert.equal(mod.resizeIfChanged(hidden), false, "숨은 컨테이너(0×0)에 0 크기 차트면 그대로 둔다(ResizeObserver 가 나중에 맞춘다)");
});

// 실측 2026-09-10(qa/chart-after-swap.cjs): 교체 직후 chart.width 는 990 인데 canvas.width 가 기본 300 인 순간이 잡혔다 - 백킹 스토어가 다르면 크기가 같아 보여도 다시 잡는다.
test("백킹 스토어(canvas.width/height)가 기대 장치 크기와 다르면 크기가 같아 보여도 resize 한다", () => {
	const stale = chart({ parentW: 990, width: 990, canvasW: 300, canvasH: 150 });
	assert.equal(mod.resizeIfChanged(stale), true);
	assert.equal(stale.resized, 1);
	const retina = chart({ dpr: 2, canvasW: 1140, canvasH: 560 });
	assert.equal(mod.resizeIfChanged(retina), false, "dpr 2 에서 1140×560 백킹 스토어는 570.4×280 의 기대값(1140×560)과 같다");
});
