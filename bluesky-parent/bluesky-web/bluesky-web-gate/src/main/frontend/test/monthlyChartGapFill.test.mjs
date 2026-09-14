// 월별 매매 금액 막대가 거래 없는 달을 건너뛰지 않는지.
//
// 실측 2026-09-12: /stock/trade 의 tradeMonthlyChart 는 2009-10 ~ 2026-09 의 204 개월 중
// 거래가 있던 61 개월만 그려 143 개월(70%)이 통째로 빠졌다. Chart.js 는 labels 문자열 배열을
// 카테고리 축으로 그리므로 간격이 날짜와 무관하게 균등하다 - 그 탓에 2010-03 -> 2014-11 의
// 4.7 년 공백이 2018-03 -> 2018-04 의 한 달과 같은 폭을 차지했다.
//
// 형제 화면인 배당 내역의 월별 막대는 이미 빈 달을 채운다(실측: 78 개월 전부 연속).
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

const { fillMonthGaps, buildMonthlyData } = globalThis.window.__monthlyChartInternals;

test("사이에 낀 달을 빠짐없이 채운다", () => {
	assert.deepEqual(fillMonthGaps(["2026-01", "2026-04"]), [
		"2026-01",
		"2026-02",
		"2026-03",
		"2026-04",
	]);
});

test("해를 넘어가도 이어진다", () => {
	assert.deepEqual(fillMonthGaps(["2025-11", "2026-02"]), [
		"2025-11",
		"2025-12",
		"2026-01",
		"2026-02",
	]);
});

test("긴 공백도 실제 개월 수만큼 늘어난다", () => {
	const filled = fillMonthGaps(["2010-03", "2014-11"]);
	assert.equal(filled.length, 57);
	assert.equal(filled[0], "2010-03");
	assert.equal(filled[filled.length - 1], "2014-11");
});

test("0 개 · 1 개는 그대로 둔다", () => {
	assert.deepEqual(fillMonthGaps([]), []);
	assert.deepEqual(fillMonthGaps(["2026-05"]), ["2026-05"]);
});

test("달 모양이 아닌 라벨은 손대지 않는다", () => {
	const odd = ["not-a-month", "2026-05"];
	assert.deepEqual(fillMonthGaps(odd), odd);
});

test("채운 달은 매수·매도 0, 실현손익은 null 이다", () => {
	const m = buildMonthlyData([
		{ tradeDate: "2026-01-05", type: "BUY", amount: 1000 },
		{ tradeDate: "2026-03-09", type: "SELL", amount: 2000, profit: 300 },
	]);
	assert.deepEqual(m.labels, ["2026-01", "2026-02", "2026-03"]);
	assert.deepEqual(m.buyData, [1000, 0, 0]);
	assert.deepEqual(m.sellData, [0, 0, 2000]);
	// 안 판 달의 선은 0 으로 꺼지지 않아야 한다(기존 규칙).
	assert.deepEqual(m.profitData, [null, null, 300]);
});

// 빈 달을 채우면 전체 기간에서 칸이 204 개가 되어 막대가 1900px 에서 1.5px, 375px 에서 0.38px 로
// 사실상 안 보였다(실측 2026-09-12). 60 개월을 넘으면 해 단위로 묶는다.
const monthsOf = (n, from = 2010) => {
	const rows = [];
	for (let i = 0; i < n; i++) {
		const y = from + Math.floor(i / 12);
		const mo = (i % 12) + 1;
		rows.push({
			tradeDate: `${y}-${mo < 10 ? "0" + mo : mo}-05`,
			type: "BUY",
			amount: 10,
		});
	}
	return rows;
};

test("60 개월까지는 월 단위 그대로다", () => {
	const m = buildMonthlyData(monthsOf(60));
	assert.equal(m.bucket, "month");
	assert.equal(m.labels.length, 60);
});

test("60 개월을 넘으면 해 단위로 묶는다", () => {
	const m = buildMonthlyData(monthsOf(61));
	assert.equal(m.bucket, "year");
	assert.deepEqual(m.labels, ["2010", "2011", "2012", "2013", "2014", "2015"]);
	// 2010~2014 는 12 달씩, 2015 는 한 달
	assert.deepEqual(m.buyData, [120, 120, 120, 120, 120, 10]);
});

test("해 단위에서도 거래 없는 해는 남는다", () => {
	const rows = monthsOf(1, 2010).concat(monthsOf(1, 2020));
	// 두 건뿐이면 월 단위지만, 채워진 달이 121 개라 해 단위로 넘어간다
	const m = buildMonthlyData(rows);
	assert.equal(m.bucket, "year");
	assert.equal(m.labels.length, 11);
	assert.equal(m.labels[0], "2010");
	assert.equal(m.labels[10], "2020");
	assert.deepEqual(m.buyData.slice(1, 10), [0, 0, 0, 0, 0, 0, 0, 0, 0]);
});

test("해 단위에서도 안 판 해의 실현손익은 null 이다", () => {
	const rows = monthsOf(1, 2010).concat([
		{ tradeDate: "2020-01-05", type: "SELL", amount: 50, profit: 7 },
	]);
	const m = buildMonthlyData(rows);
	assert.equal(m.bucket, "year");
	assert.equal(m.profitData[0], null);
	assert.equal(m.profitData[10], 7);
});
