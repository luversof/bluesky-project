// 차트 축 눈금·범례 글자색이 테마 본문색(--color-base-content)을 따른다.
//
// 실측 2026-09-09(qa/chart-tick-contrast.cjs): Chart.js 기본 #666 이 다크 카드 배경에서 3.15:1 이었다(12개 측정 중 다크 6개 전부 4.5 미달).
// 빌드 산출물(stock-charts.js)을 최소 스텁으로 띄운다(stockChartsMessages.test.mjs 와 같은 방식) - Chart 전역과 1px 캔버스를 흉내 낸다.
import assert from "node:assert/strict";
import test from "node:test";

/** 캔버스 fillStyle 파서 흉내: 아는 색만 rgb 로 푼다. 모르는 색은 fillStyle 이 바뀌지 않는다(브라우저와 같은 실패 신호). */
const KNOWN = { "oklch(97% 0.01 260)": [220, 225, 235], "#222": [34, 34, 34] };
function fakeCanvas() {
	let style = "#000000", px = [0, 0, 0];
	return {
		getContext: () => ({
			set fillStyle(v) { if (v === "#010203") { style = v; px = [1, 2, 3]; } else if (KNOWN[v]) { style = v; px = KNOWN[v]; } },
			get fillStyle() { return style; },
			fillRect() {},
			getImageData: () => ({ data: [...px, 255] }),
		}),
	};
}
let cssVar = "oklch(97% 0.01 260)";
globalThis.document = {
	getElementById: () => null,
	createElement: (tag) => (tag === "canvas" ? fakeCanvas() : { className: "", textContent: "", style: {}, dataset: {} }),
	body: { dataset: {} },
	documentElement: { lang: "ko" },
};
globalThis.getComputedStyle = () => ({ getPropertyValue: () => cssVar });
globalThis.window = globalThis;
const instance = { options: { scales: { x: { ticks: {}, title: {} }, y: { ticks: {} } }, plugins: { legend: { labels: {} } } }, updated: null, animationDuringUpdate: "unset", update(mode) { this.updated = mode; this.animationDuringUpdate = this.options.animation; } };
globalThis.Chart = { defaults: { color: "#666" }, instances: { 1: instance } };

await import("../../resources/static/js/stock-charts.js");
const mod = globalThis.__chartThemeInternals;

test("모듈이 뜨면서 Chart.defaults.color 를 테마 본문색으로 바꾼다", () => {
	assert.ok(mod, "stock-charts.js 가 __chartThemeInternals 를 노출하지 않는다");
	assert.equal(globalThis.Chart.defaults.color, "rgba(220,225,235,0.75)");
	assert.equal(instance.options.color, "rgba(220,225,235,0.75)", "살아 있는 차트에도 적용");
	// "none" 은 막대가 함께 쓰는 옵션(테두리색)을 다시 풀지 않는다(2026-10-02 실측: 전환 뒤 라이트 회색 테두리가 남음) - 기본 갱신 + 애니메이션 끔.
	assert.equal(instance.updated, "default", "옵션을 다시 푸는 갱신 모드로 다시 그린다");
	assert.equal(instance.animationDuringUpdate, false, "다시 그릴 때 애니메이션을 끈다(깜빡임 방지)");
	assert.equal(instance.options.animation, undefined, "끝나면 원래 애니메이션 설정으로 돌린다");
	assert.equal(instance.options.scales.x.ticks.color, "rgba(220,225,235,0.75)", "눈금은 캐시된 값이 남으니 직접 써 넣는다");
	assert.equal(instance.options.scales.x.title.color, "rgba(220,225,235,0.75)");
	assert.equal(instance.options.plugins.legend.labels.color, "rgba(220,225,235,0.75)");
});

test("테마가 바뀌면 다시 계산한다", () => {
	cssVar = "#222";
	assert.equal(mod.applyChartTheme(globalThis.Chart), "rgba(34,34,34,0.75)");
	assert.equal(globalThis.Chart.defaults.color, "rgba(34,34,34,0.75)");
});

test("변수가 없으면 Chart.js 기본값, 캔버스가 못 푸는 색이면 원문 그대로", () => {
	cssVar = "";
	assert.equal(mod.chartTextColor(), "#666");
	cssVar = "color-mix(in oklab, red, blue)";
	assert.equal(mod.chartTextColor(), "color-mix(in oklab, red, blue)");
	assert.equal(mod.resolveCssColor("#010203").join(","), "1,2,3", "감시용 색 자체는 파싱 실패로 보지 않는다");
});

test("Chart 전역이 없으면 아무 일도 하지 않는다", () => {
	assert.equal(mod.applyChartTheme(null), null, "undefined 는 기본 인자(전역 Chart)로 떨어지므로 null 로 준다");
	assert.equal(mod.applyChartTheme({}), null);
});

test("색이 곧 뜻인 막대(keepEdge)는 테마 테두리로 덮지 않는다", () => {
	// 실측 2026-10-02: 종목 상세 캔들에서 테마를 바꾸면 봉 테두리가 회색 1px 로 덮여 오름 · 내림 색이 사라졌다.
	cssVar = "#222";
	const candle = { type: "bar", borderWidth: 0, keepEdge: true };
	const plain = { type: "bar", borderWidth: 0 };
	const chart = { options: { scales: {} }, data: { datasets: [candle, plain] }, update() {} };
	mod.applyChartTheme({ defaults: { color: "#666" }, instances: { 1: chart } });
	assert.equal(candle.borderWidth, 0, "캔들 막대 테두리 두께를 올렸다");
	assert.equal(candle.borderColor, undefined, "캔들 막대에 테마 테두리색을 칠했다");
	assert.equal(plain.borderWidth, 1, "보통 막대에는 예전처럼 테두리를 얹는다");
	assert.ok(plain.borderColor, "보통 막대에는 예전처럼 테두리색을 얹는다");
});
