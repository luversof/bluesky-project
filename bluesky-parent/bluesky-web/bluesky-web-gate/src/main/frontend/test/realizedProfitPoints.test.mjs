// 월별 매매 차트의 실현손익은 '이어진 선'이 아니라 '아래 칸의 월별 막대'여야 한다.
//
// 실현손익은 그 달에 판 결과다. 2026-09-14 까지는 선이었고, 안 판 달을 null 로 두긴 했으나
// spanGaps: true 가 그 구멍을 건너뛰어 이어 버렸다. 거기에 tension: 0.35 가 두 매도 사이에 없는 궤적까지
// 그려, 판 적 없는 달에도 "그 달의 실현손익은 이쯤" 이라는 값이 읽혔다.
// 실측 2026-09-14(전체 기간): 매도가 있던 달은 204 개월 중 33 개월뿐인데 선은 204 칸을 끊김 없이 갔다.
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

const { buildRealizedProfitDataset, buildMonthlyData } =
	globalThis.window.__monthlyChartInternals;

test("막대다 - 선을 긋지 않는다", () => {
	const ds = buildRealizedProfitDataset([100, null, null, -50]);

	assert.equal(ds.type, "bar", "선이면 판 적 없는 달까지 값이 읽힌다");
	// 선으로 되돌아가면 이 셋 중 하나는 반드시 생긴다.
	assert.equal(ds.showLine, undefined);
	assert.equal(ds.spanGaps, undefined, "spanGaps 는 구멍을 건너뛰어 잇는다");
	assert.equal(ds.tension, undefined, "tension 은 두 매도 사이에 없는 궤적을 그린다");
});

test("아래 칸에서 읽히되 x 축은 매수/매도와 같다", () => {
	const ds = buildRealizedProfitDataset([100, null, -50]);

	assert.equal(ds.yAxisID, "y2", "매매 금액 축에 얹으면 대부분의 달에 막대가 뭉개진다");
	// 전용 x 축을 따로 두면 같은 달인데 세로줄이 어긋난다
	// (실측 2026-09-14: 매수·매도 쌍의 중앙 349.8px 인데 손익 막대는 373.1px - 23.3px 오른쪽).
	assert.equal(ds.xAxisID, "x", "축이 둘이면 같은 달이 같은 세로줄에 서지 못한다");
	// 같은 축에 두면 이번엔 '세 번째 자리' 로 밀린다 - grouped:false 라야 칸 한가운데에 선다.
	assert.equal(ds.grouped, false, "묶이면 칸 가운데가 아니라 옆으로 밀려 선다");
	// 묶이지 않으면 칸 전체 폭을 먹어 매수/매도보다 꼭 두 배가 된다(실측 20.97 vs 10.49).
	// 기본 0.9 의 절반이라야 매수/매도 한 개와 폭이 같다.
	assert.equal(ds.barPercentage, 0.45, "폭이 매수/매도의 두 배가 된다");
});

test("안 판 달은 값이 없다(0 이 아니다)", () => {
	const ds = buildRealizedProfitDataset([100, null, null, -50]);

	assert.deepEqual(ds.data, [100, null, null, -50]);
});

test("부호를 색으로 가른다", () => {
	const ds = buildRealizedProfitDataset([100, null, -50]);

	assert.notEqual(
		ds.backgroundColor[0],
		ds.backgroundColor[2],
		"이익과 손해가 같은 색이면 손해 본 매도가 이익처럼 보인다",
	);
});

test("자료가 만드는 값도 판 달에만 있다", () => {
	const m = buildMonthlyData([
		{ tradeDate: "2026-01-05", type: "BUY", amount: 1000 },
		{ tradeDate: "2026-03-05", type: "SELL", amount: 1200, profit: 200 },
		{ tradeDate: "2026-05-05", type: "BUY", amount: 500 },
	]);

	assert.deepEqual(m.labels, ["2026-01", "2026-02", "2026-03", "2026-04", "2026-05"]);
	assert.deepEqual(
		m.profitData,
		[null, null, 200, null, null],
		"매수만 한 달에 0 을 적으면 '그 달에 팔아서 0 원 남겼다'로 읽힌다",
	);

	const ds = buildRealizedProfitDataset(m.profitData);
	assert.equal(ds.data.filter((v) => v !== null).length, 1, "막대는 판 달 하나뿐이어야 한다");
});

// 해 단위로 묶였으면 범례도 그렇게 말해야 한다. 제목은 이미 바꾸는데(applyMonthlyBucketTitle)
// 범례만 "매도한 달" 로 남으면 한 칸이 한 달로 읽힌다 - 실측 2026-09-14 전체 기간은 18 칸이 전부 해다.
test("해 단위로 묶이면 범례도 해라고 말한다", () => {
	const months = buildRealizedProfitDataset([1], "month").label;
	const years = buildRealizedProfitDataset([1], "year").label;

	assert.notEqual(months, years, "묶음 단위가 달라도 범례가 같으면 한 칸을 잘못 읽는다");
});

test("묶음을 안 주면 달 기준이다", () => {
	assert.equal(
		buildRealizedProfitDataset([1]).label,
		buildRealizedProfitDataset([1], "month").label,
	);
});

// 칸 구분선은 두 칸이 맞닿는 자리에 또렷하게 놓여야 한다.
//
// 실측 2026-09-14: 경계가 179.667px 인데 `Math.round(x) + 0.5` 로 그려 180.5px 에 놓였다(0.83px 어긋남).
// 게다가 +0.5 는 DPR 1 에서만 또렷하고 1.5·2 에서는 장치 픽셀 사이에 걸쳐 번진다.
const { snapLine } = globalThis.window.__monthlyChartInternals;

test("구분선은 제자리를 반 픽셀 넘게 벗어나지 않는다", () => {
	for (const dpr of [1, 1.25, 1.5, 2, 3]) {
		const { center } = snapLine(179.66666666666666, dpr);
		assert.ok(
			Math.abs(center - 179.66666666666666) <= 0.5 / dpr + 1e-9,
			`dpr ${dpr}: ${center} 가 경계에서 너무 멀다`,
		);
	}
});

test("장치 픽셀 폭이 홀수면 중심이 반픽셀, 짝수면 정수다", () => {
	// dpr 1 -> 1 장치픽셀(홀수) -> 중심 x.5
	assert.equal(snapLine(179.6666, 1).center % 1, 0.5);
	// dpr 2 -> 2 장치픽셀(짝수) -> 중심이 장치 격자에 정확히 (179.5 * 2 = 359)
	assert.equal((snapLine(179.6666, 2).center * 2) % 1, 0);
});

test("두께는 언제나 장치 픽셀 정수다", () => {
	for (const dpr of [1, 1.25, 1.5, 2, 3]) {
		const { width } = snapLine(100, dpr);
		assert.equal((width * dpr) % 1, 0, `dpr ${dpr}: 두께가 장치 픽셀에 안 맞는다`);
		assert.ok(width * dpr >= 1);
	}
});
