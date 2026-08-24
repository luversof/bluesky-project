// 브라우저 스크립트의 순수 계산을 Node 내장 러너로 검증한다.
//
// 왜 빌드 산출물을 부르는가: 배포되는 것은 tsc + esbuild 를 거친 js 다. 소스만 맞고 산출물이
// 옛것이면 화면은 여전히 옛 동작을 한다. 실제로 배포되는 파일을 그대로 불러 검증한다.
//
// 주의: 이 모듈은 지금 <b>어떤 템플릿에서도 로드되지 않는다</b>(deadAssets.test.mjs 의 KNOWN_UNUSED).
// 시계열 차트는 asset-graph 계열 인라인 스크립트가 그린다. 따라서 아래 검증은 "배포되지만 실행되지 않는
// 코드"에 대한 것이다 - 이 모듈을 다시 화면에 붙일 때 회귀를 막는 용도로만 의미가 있다.
// (처음 이 테스트를 쓸 때 실행되는 코드라고 적었는데 사실이 아니었다.)
//
// 실행: src/main/frontend 에서 `npm run test:js` (또는 node --test test)
import assert from "node:assert/strict";
import test from "node:test";

// 이 모듈은 최상위에서 window 에 함수를 붙인다. Node 에는 window 가 없으므로 먼저 채운다.
globalThis.window = globalThis.window ?? {};

const chart = await import("../../resources/static/js/stock/timeSeriesChart.js");

test("알 수 없는 타임존은 브라우저 로컬로 되돌린다", () => {
	assert.equal(chart.resolveLabelZone("Asia/Seoul"), "Asia/Seoul");
	assert.equal(chart.resolveLabelZone("Mars/Olympus"), undefined);
	assert.equal(chart.resolveLabelZone(""), undefined);
	assert.equal(chart.resolveLabelZone(undefined), undefined);
});

// 지점의 timestamp 는 서버가 집계에 쓴 타임존의 자정을 가리키는 instant 다.
// KST 2026-01-01 은 2025-12-31T15:00:00Z 이므로, 타임존을 넘기지 않으면 KST 밖에서 하루 앞으로
// 밀려 연도 경계에서 해가 바뀐다. 그 회귀를 고정한다.
test("연도 경계에서 라벨이 서버 타임존을 따른다", () => {
	const series = [{ timestamp: "2025-12-31T15:00:00Z" }];

	const seoul = chart.toChartData(series, "Asia/Seoul").labels[0];
	const utc = chart.toChartData(series, "UTC").labels[0];

	assert.notEqual(seoul, utc, "타임존을 무시하면 두 라벨이 같아진다");
	assert.ok(seoul.includes("2026"), `KST 라벨에 2026 이 없다: ${seoul}`);
	assert.ok(utc.includes("2025"), `UTC 라벨에 2025 가 없다: ${utc}`);
});

test("타임존이 없으면 예전 동작(브라우저 로컬)을 유지한다", () => {
	const series = [{ timestamp: "2025-12-31T15:00:00Z" }];
	const expected = new Date(series[0].timestamp).toLocaleDateString(
		undefined,
		undefined,
	);

	assert.equal(chart.toChartData(series).labels[0], expected);
	assert.equal(chart.toChartData(series, "Mars/Olympus").labels[0], expected);
});

test("값이 없는 지점은 0 으로 채운다", () => {
	const series = [
		{ timestamp: "2026-01-02T00:00:00Z", cumulativeRealizedProfit: 100, dailyRealizedProfit: 7 },
		{ timestamp: "2026-01-03T00:00:00Z" },
		{ timestamp: "2026-01-04T00:00:00Z", cumulativeRealizedProfit: null, dailyRealizedProfit: null },
	];

	const data = chart.toChartData(series, "Asia/Seoul");

	assert.deepEqual(data.cumulative, [100, 0, 0]);
	assert.deepEqual(data.daily, [7, 0, 0]);
	assert.equal(data.labels.length, 3);
});

test("빈 시리즈도 터지지 않는다", () => {
	const data = chart.toChartData([], "Asia/Seoul");
	assert.deepEqual(data.labels, []);
	assert.deepEqual(data.cumulative, []);
	assert.deepEqual(data.daily, []);
});
