// 차트 애니메이션 길이는 한 곳에서만 정한다.
//
// 실측 2026-09-14(9 화면 12 차트): 자산 성장 · 종목/계좌 상세는 600ms 인데 대시보드 · 매매 · 배당 ·
// 활동 · 시뮬레이터는 Chart.js 기본값 1000ms 였다. 화면을 옮길 때마다 그려지는 리듬이 달라진다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.window.addEventListener = globalThis.window.addEventListener ?? (() => {});
globalThis.document = globalThis.document ?? {
	getElementById: () => null,
	querySelectorAll: () => [],
	addEventListener: () => {},
};

await import("../../resources/static/js/stock-charts.js");

const { applyChartAnimationDefaults, CHART_ANIMATION_MS } =
	globalThis.window.__chartThemeInternals;

test("전역 기본값을 상수 하나로 정한다", () => {
	const lib = { defaults: { animation: { duration: 1000, easing: "easeOutQuart" } } };

	assert.equal(applyChartAnimationDefaults(lib), CHART_ANIMATION_MS);
	assert.equal(lib.defaults.animation.duration, CHART_ANIMATION_MS);
	assert.equal(
		lib.defaults.animation.easing,
		"easeOutQuart",
		"기본값을 통째로 갈아치우면 easing 같은 다른 설정이 사라진다",
	);
});

test("Chart 가 없어도 죽지 않는다", () => {
	assert.equal(applyChartAnimationDefaults(undefined), null);
	assert.equal(applyChartAnimationDefaults({}), null);
});

// 보유 차트(자산 성장 · 종목/계좌 상세)는 opts.animate 로 끌 수 있다. 예전에는 기본이 '꺼짐' 이라
// 호출부가 animate 한 줄을 빠뜨리면 그 화면만 조용히 정지 화면이 됐다.
test("보유 차트는 animate 를 안 줘도 애니메이션이 있다", () => {
	const cfg = globalThis.window.StockCharts.holdingsChartConfig(
		{ labels: ["2026-01"], value: [100], cost: [90] },
		{},
		{},
	);

	assert.deepEqual(cfg.options.animation, { duration: CHART_ANIMATION_MS });
});

test("보유 차트는 animate:false 로만 끈다", () => {
	const off = globalThis.window.StockCharts.holdingsChartConfig(
		{ labels: ["2026-01"], value: [100], cost: [90] },
		{},
		{ animate: false },
	);
	const on = globalThis.window.StockCharts.holdingsChartConfig(
		{ labels: ["2026-01"], value: [100], cost: [90] },
		{},
		{ animate: true },
	);

	assert.equal(off.options.animation, false);
	assert.deepEqual(on.options.animation, { duration: CHART_ANIMATION_MS });
});

// 첫 등장 애니메이션.
//
// 길이를 맞춰도 막대가 자라지 않았다(도넛 회전만 보였다). 실측 2026-09-14: 차트를 만들면 Chart.js 가
// 생성 도중 캔버스를 300x150 에서 실제 크기로 리사이즈하고, 그 리사이즈는 update("resize") 로 처리되는데
// 이 전환은 길이가 0 이다 - 최종 모양이 즉시 박히고 이어지는 첫 update 는 '같은 값 -> 같은 값' 이 된다.
//   resize 300x150 -> resize 858x280 -> update(resize) -> update(undefined)
const { entryAnimationPlugin } = globalThis.window.StockCharts;

function fakeChart(overrides = {}) {
	return {
		canvas: {},
		ctx: {},
		options: { animation: { duration: 600 } },
		reset() {
			this.didReset = true;
		},
		update() {
			this.didUpdate = true;
		},
		...overrides,
	};
}

function runFrames(times = 4) {
	const queue = [];
	const realRaf = globalThis.requestAnimationFrame;
	globalThis.requestAnimationFrame = (fn) => queue.push(fn);
	return {
		flush() {
			for (let i = 0; i < times; i++) {
				const pending = queue.splice(0);
				for (const fn of pending) fn();
			}
			globalThis.requestAnimationFrame = realRaf;
		},
	};
}

test("레이아웃이 끝난 뒤 한 번 되감아 다시 그린다", () => {
	const chart = fakeChart();
	const frames = runFrames();

	entryAnimationPlugin.afterInit(chart);
	assert.equal(chart.didReset, undefined, "즉시 되감으면 생성 직후 리사이즈가 다시 최종값으로 박는다");

	frames.flush();
	assert.equal(chart.didReset, true);
	assert.equal(chart.didUpdate, true);
});

test("애니메이션을 끈 차트는 건드리지 않는다", () => {
	const chart = fakeChart({ options: { animation: false } });
	const frames = runFrames();

	entryAnimationPlugin.afterInit(chart);
	frames.flush();

	assert.equal(chart.didReset, undefined);
});

test("한 차트에 한 번만 건다", () => {
	const chart = fakeChart();
	const frames = runFrames();

	entryAnimationPlugin.afterInit(chart);
	entryAnimationPlugin.afterInit(chart);
	entryAnimationPlugin.afterInit(chart);
	let count = 0;
	chart.reset = () => count++;
	frames.flush();

	assert.equal(count, 1, "여러 번 걸리면 되감기가 겹쳐 깜빡인다");
});

test("이미 없어진 차트는 되감지 않는다", () => {
	const chart = fakeChart();
	const frames = runFrames();

	entryAnimationPlugin.afterInit(chart);
	chart.canvas = null; // destroy() 된 상태
	frames.flush();

	assert.equal(chart.didReset, undefined);
});
