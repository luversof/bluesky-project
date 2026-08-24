// 월배당 시뮬레이터의 "선택 합계"가 서버 요약 카드와 같은 정의를 쓰는지 실행해서 본다.
//
// 서버(MonthlyDividendCalculator.summarize)의 정의:
//   총매수금액   = Σ(평균매수단가 x 보유수량)      <- 행이 data-buy-amount 로 그대로 내보낸다
//   연배당       = 월배당 합 x 12
//   연과세표준   = 월과세표준 합 x 12
//   연배당수익률 = 연배당 / 총평가액 x 100
//
// 최신 월배당은 종목 태그(월중/월말)로 나눠서도 보여준다 - 행의 data-payout-window 로 가른다.
// 실측 2026-08-24: 이 사용자의 월배당 종목은 8 개이고, 스냅샷에 연배당 필드 자체가 없다
// (스냅샷 키에 expectedAnnualDividend 가 없다). 연배당은 게이트가 x12 로 만든다 - 그래서 이
// 정의가 화면 양쪽에서 같아야 한다.
import assert from "node:assert/strict";
import test from "node:test";
import { runSelectionSummary } from "./inlineTemplateScript.mjs";

const TEMPLATE = "stock/fragments/monthlyDividendSimulator.jte";
const NAMES = [
	"formatMonthlyDividendSimulatorNumber",
	"formatMonthlyDividendSimulatorCurrency",
	"formatMonthlyDividendSimulatorSignedCurrency",
	"formatMonthlyDividendSimulatorSignedPercent",
	"setMonthlyDividendSimulatorSignedTone",
	"updateMonthlyDividendSimulatorSelectionSummary",
];

function run(rows, summaryData) {
	return runSelectionSummary({
		templatePath: TEMPLATE,
		names: NAMES,
		rows,
		summaryData,
		extras: { getMonthlyDividendSimulatorLocale: () => "en-US" },
	});
}

const ROWS = [
	{
		latestMonthlyDividend: "30000",
		payoutWindow: "MID_MONTH",
		buyAmount: "1000000",
		expectedMonthlyDividend: "25000",
		expectedTaxableBase: "5000",
		currentMarketValue: "1200000",
	},
	{
		latestMonthlyDividend: "10000",
		payoutWindow: "MONTH_END",
		buyAmount: "1000000",
		expectedMonthlyDividend: "15000",
		expectedTaxableBase: "3000",
		currentMarketValue: "800000",
	},
];

const SUMMARY = {
	totalExpectedMonthlyDividend: "80000",
	totalCurrentMarketValue: "4000000",
	countTemplate: "{0}",
};

test("연배당·연과세표준은 월 합계의 12 배다", () => {
	const written = run(ROWS, SUMMARY);
	// 월배당 40,000 -> 연 480,000 / 월과세표준 8,000 -> 연 96,000
	assert.equal(written["data-monthly-selection-monthly"], "₩40,000");
	assert.equal(written["data-monthly-selection-annual-dividend"], "₩480,000");
	assert.equal(written["data-monthly-selection-taxable"], "₩8,000");
	assert.equal(written["data-monthly-selection-annual-taxable"], "₩96,000");
});

test("연배당수익률은 연배당을 총평가액으로 나눈다(월배당이 아니다)", () => {
	const written = run(ROWS, SUMMARY);
	// 480,000 / 2,000,000 = 24.00%. 월배당으로 나누면 2.00% 가 된다.
	assert.equal(written["data-monthly-selection-annual-yield"], "24.00%");
});

test("평가손익률은 (평가액 − 매수금액) / 매수금액 이다", () => {
	const written = run(ROWS, SUMMARY);
	// (2,000,000 − 2,000,000) / 2,000,000 = 0.00%
	assert.equal(written["data-monthly-selection-profit-rate"], "0.00%");

	const skewed = run(
		[ROWS[0], { ...ROWS[1], currentMarketValue: "1000000" }],
		SUMMARY,
	);
	// (2,200,000 − 2,000,000) / 2,000,000 = +10.00%
	assert.equal(skewed["data-monthly-selection-profit-rate"], "+10.00%");
	assert.equal(skewed["data-monthly-selection-profit"], "+₩200,000");
});

test("최신 월배당을 월중·월말로 가른다", () => {
	const written = run(ROWS, SUMMARY);
	assert.equal(written["data-monthly-selection-latest"], "₩40,000");
	assert.equal(written["data-monthly-selection-latest-mid"], "₩30,000");
	assert.equal(written["data-monthly-selection-latest-end"], "₩10,000");
});

test("비중은 요약이 들고 있는 전체 기준으로 낸다", () => {
	const written = run(ROWS, SUMMARY);
	// 월배당 40,000 / 80,000 = 50.0% · 평가액 2,000,000 / 4,000,000 = 50.0%
	assert.equal(written["data-monthly-selection-monthly-weight"], "50.0%");
	assert.equal(written["data-monthly-selection-market-weight"], "50.0%");
});
