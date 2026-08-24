// 자산현황 표의 "선택 합계"가 같은 표의 합계행과 같은 정의를 쓰는지 실행해서 본다.
//
// 합계행(서버/템플릿)의 정의:
//   평가손익률   = Σ평가손익 / Σ매수금액 x 100
//   원금대비수익률 = (Σ평가액 − Σ기준원금) / Σ기준원금 x 100
//
// 기준원금은 매수금액이 아니다. 계좌 설정에 수동 입력값이 있으면 그 값이고(행이 data-principal 로
// 내보낸다), 없으면 보유분 원가로 떨어진다. 실측 2026-08-24: 이 사용자의 6 계좌는 수동 원금이 모두
// 비어 있어 지금은 둘이 우연히 가까울 수 있다 - 그래서 이 검사는 둘을 일부러 다르게 준다.
import assert from "node:assert/strict";
import test from "node:test";
import { runSelectionSummary } from "./inlineTemplateScript.mjs";

const TEMPLATE = "stock/htmx/fragments/assetStatus.jte";
const FORMATTERS = [
	"formatAssetStatusNumber",
	"formatAssetStatusSignedNumber",
	"formatAssetStatusMessage",
];
const EXTRAS = { getAssetStatusLocale: () => "en-US" };

function runAccount(rows, summaryData) {
	return runSelectionSummary({
		templatePath: TEMPLATE,
		names: [...FORMATTERS, "updateAssetStatusAccountSelectionSummary"],
		rows,
		summaryData,
		extras: EXTRAS,
	});
}

function runStock(rows, summaryData) {
	return runSelectionSummary({
		templatePath: TEMPLATE,
		names: [...FORMATTERS, "updateAssetStatusStockSelectionSummary"],
		rows,
		summaryData,
		extras: EXTRAS,
	});
}

test("계좌 선택 합계: 원금대비 수익률은 기준원금으로 나눈다(매수금액이 아니다)", () => {
	const written = runAccount(
		[
			// 수동 원금이 없는 계좌: 기준원금 = 매수금액.
			{
				buyAmount: "1000",
				evaluationAmount: "1200",
				evaluationProfit: "200",
				principal: "1000",
				principalReturn: "200",
			},
			// 수동 원금이 매수금액(3,000)과 다른 계좌(4,000). 두 분모가 갈라진다.
			{
				buyAmount: "3000",
				evaluationAmount: "2700",
				evaluationProfit: "-300",
				principal: "4000",
				principalReturn: "-1300",
			},
		],
		{ totalEvaluationAmount: "7800", countTemplate: "{0}" },
	);

	// 평가손익률: (200 − 300) / (1,000 + 3,000) = −2.5%
	assert.equal(written["data-account-selection-profit-rate"], "(-2.5%)");
	// 원금대비: (200 − 1,300) / (1,000 + 4,000) = −22.0%. 매수금액으로 나누면 −27.5% 가 된다.
	assert.equal(written["data-account-selection-principal-return-rate"], "(-22.0%)");
	assert.equal(written["data-account-selection-principal"], "5,000");
	assert.equal(written["data-account-selection-buy-amount"], "4,000");
	// 비중: 선택한 평가액 3,900 / 전체 7,800
	assert.equal(written["data-account-selection-weight"], "50.0%");
});

test("계좌 선택 합계: 원금이 0 이면 수익률을 0 으로 둔다(나눗셈을 하지 않는다)", () => {
	const written = runAccount(
		[
			{
				buyAmount: "0",
				evaluationAmount: "500",
				evaluationProfit: "500",
				principal: "0",
				principalReturn: "500",
			},
		],
		{ totalEvaluationAmount: "500", countTemplate: "{0}" },
	);

	assert.equal(written["data-account-selection-profit-rate"], "(0.0%)");
	assert.equal(written["data-account-selection-principal-return-rate"], "(0.0%)");
});

test("종목 선택 합계: 평가손익률은 매수금액으로 나눈다(평가액이 아니다)", () => {
	const written = runStock(
		[
			{ buyAmount: "1000", evaluationAmount: "1500", evaluationProfit: "500" },
			{ buyAmount: "1000", evaluationAmount: "700", evaluationProfit: "-300" },
		],
		{ totalEvaluationAmount: "4400", countTemplate: "{0}" },
	);

	// (500 − 300) / (1,000 + 1,000) = 10.0%. 평가액(2,200)으로 나누면 9.1% 가 된다.
	assert.equal(written["data-selection-profit-rate"], "(+10.0%)");
	assert.equal(written["data-selection-buy-amount"], "2,000");
	assert.equal(written["data-selection-evaluation-amount"], "2,200");
	assert.equal(written["data-selection-evaluation-profit"], "+200");
	// 비중: 2,200 / 4,400
	assert.equal(written["data-selection-weight"], "50.0%");
});
