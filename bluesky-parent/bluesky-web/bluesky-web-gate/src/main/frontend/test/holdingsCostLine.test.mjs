// 보유가 없으면 "현재 평균단가" 선을 긋지 않는지.
//
// 실측 2026-09-12(/stock/item): 매매 이력이 있는 43 종목 중 34 종목이 수량 0 인데, 그 종목들의
// 주가 추이 차트는 "현재 평균단가" 계열을 전 구간 0 으로 그렸다 - 기아 2,740점 · 삼성SDI 1,575점 ·
// 나노팀 750점 · 에스디바이오센서 1,151점 모두 0 이 아닌 값 0 개. 종가가 각각 206,000 / 799,869 /
// 36,900 / 78,600 까지 오르는 차트 바닥에 선이 깔려 "공짜로 샀다" 로 읽혔고, 보조기술 요약에도
// "최고 0, 최저 0" 으로 나갔다. 보유 중인 삼성전자 · KODEX 200타겟위클리커버드콜 은 정상이었다.
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

const config = (series) =>
	globalThis.window.StockCharts.holdingsChartConfig(series, {
		valueLabel: "종가",
		costLabel: "현재 평균단가",
	});

const base = { labels: ["2026-01-01", "2026-01-02"], value: [100, 120], buyCount: [], dailyRealized: [] };

test("원가 계열이 있으면 두 선을 다 긋는다", () => {
	const ds = config({ ...base, cost: [90, 90] }).data.datasets;
	assert.equal(ds.length, 2);
	assert.equal(ds[0].label, "현재 평균단가");
	assert.deepEqual(ds[0].data, [90, 90]);
	assert.equal(ds[1].label, "종가");
});

test("원가 계열이 비면 그 선을 아예 빼고 채움도 끈다", () => {
	const ds = config({ ...base, cost: [] }).data.datasets;
	assert.equal(ds.length, 1);
	assert.equal(ds[0].label, "종가");
	// 채움 기준이 사라졌으니 위/아래를 칠하면 안 된다
	assert.equal(ds[0].fill, false);
});

test("원가 계열이 있을 때의 채움은 그대로다", () => {
	const ds = config({ ...base, cost: [90, 90] }).data.datasets;
	assert.equal(ds[1].fill.target, "-1");
});

test("cost 키가 아예 없어도 선을 만들지 않는다", () => {
	const ds = config({ ...base }).data.datasets;
	assert.equal(ds.length, 1);
	assert.equal(ds[0].label, "종가");
});
