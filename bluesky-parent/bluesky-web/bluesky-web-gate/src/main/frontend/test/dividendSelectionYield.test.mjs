// 배당 수익률 표에서 행을 골랐을 때 나오는 '선택 합계'가 서버 합계행과 같은 규칙으로 계산되는지 본다.
//
// 두 수익률의 분자는 같아야 한다 - 기준일 원금이 있는 배당의 세후액(netWithPrincipalCost)만 쓴다.
// 기준일에 원금이 없던 배당(지급일 전에 전량 매도한 건)은 분모(일수 합계·기준일 평균원금)에 기여하지
// 않으므로, 분자에만 넣으면 수익률이 과대 계상된다. 서버(YieldAccumulator.toView)는 세 수익률 모두
// 그렇게 계산한다.
//
// 실측 2026-08-24: 브라우저 쪽은 일평균원금 기준 수익률만 totalNetAmount(전부)를 쓰고 있었다.
//  - 전체를 고르면 분자가 61,645,687 로 서버 합계행의 61,501,327 보다 0.235% 컸다(차이 144,360 원·5 건).
//  - 배당이 그 5 건뿐인 종목을 하나만 고르면 더 크게 어긋났다 - NAVER 는 배당이 1 건(세후 102,040 원)
//    뿐이고 2021-01-18 에 전량 매도한 뒤 2021-04-08 에 지급됐다. 서버 행은 0.00% 인데 선택 합계는
//    0 이 아닌 값을 냈다.
//
// 문자열을 찾는 대신 JTE 에 박힌 함수를 그대로 꺼내 가짜 DOM 위에서 돌린다 - 식을 바꿔 적어도
// 결과로 잡힌다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const MAIN = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const TEMPLATE = join(MAIN, "jte/stock/htmx/fragments/tabsDividendHistory.jte");
const FUNCTION_NAME = "updateDividendYieldSelectionSummary";

/** 이름 붙은 함수 하나를 중괄호 짝을 맞춰 잘라 낸다. */
function extractFunction(source, name) {
	const start = source.indexOf("function " + name);
	assert.ok(start > 0, name + " 을 템플릿에서 찾지 못했다 - 검사가 무력해진다");
	let at = source.indexOf("{", start);
	let depth = 1;
	while (depth > 0) {
		at++;
		const c = source[at];
		if (c === undefined) throw new Error(name + " 의 중괄호 짝이 맞지 않는다");
		if (c === "{") depth++;
		else if (c === "}") depth--;
	}
	return source.slice(start, at + 1);
}

/** 선택 합계가 화면에 쓴 값을 담는다. */
function fakeSection(rows, summaryDataset) {
	const written = {};
	const labelFor = (key) => ({
		set textContent(value) {
			written[key] = value;
		},
	});
	const summary = {
		dataset: summaryDataset,
		classList: { toggle() {} },
		closest: () => null,
		querySelector(selector) {
			const key = /\[data-dividend-yield-selection-([a-z-]+)\]/.exec(selector);
			return key ? labelFor(key[1]) : null;
		},
	};
	const table = {
		tBodies: [
			{
				querySelectorAll: () =>
					rows.map((dataset) => ({ dataset })),
			},
		],
	};
	return {
		written,
		section: {
			querySelector(selector) {
				if (selector.includes("selection-summary")) return summary;
				if (selector.includes("selection-table")) return table;
				return null;
			},
		},
	};
}

function runSummary(rows, summaryDataset) {
	const body = extractFunction(readFileSync(TEMPLATE, "utf8"), FUNCTION_NAME);
	const sandbox = {
		formatNumber: (value) => String(value),
		formatFixedNumber: (value, digits) => Number(value).toFixed(digits),
	};
	vm.createContext(sandbox);
	vm.runInContext(body + ";this.__run = " + FUNCTION_NAME + ";", sandbox);
	const fake = fakeSection(rows, summaryDataset);
	sandbox.__run(fake.section);
	return fake.written;
}

const SUMMARY_DATASET = {
	totalNetAmount: "1000",
	countTemplate: "{0}",
	weightLabel: "비중",
	basisLabel: "기준",
};

test("일평균원금 기준 수익률은 기준일 원금이 있는 배당만 분자에 넣는다", () => {
	// 원금이 있는 행(세후 800, 일수평균원금 10,000)과 원금이 없던 행(세후 200)을 함께 고른다.
	const written = runSummary(
		[
			{
				grossAmount: "900",
				netAmount: "800",
				taxAmount: "100",
				taxableAmount: "900",
				averageDailyPrincipalCost: "10000",
				averagePrincipalCost: "10000",
				netWithPrincipalCost: "800",
			},
			{
				grossAmount: "220",
				netAmount: "200",
				taxAmount: "20",
				taxableAmount: "220",
				averageDailyPrincipalCost: "0",
				averagePrincipalCost: "0",
				netWithPrincipalCost: "0",
			},
		],
		SUMMARY_DATASET,
	);

	// 분자 800 / 분모 10,000 = 8.00%. 전부(1,000)를 넣으면 10.00% 가 된다.
	assert.equal(written.yield, "8.00%");
	// 세후 합계 자체는 고른 행 전부를 더한다(수익률 분자와 다른 값이다).
	assert.equal(written.net, "1000");
	assert.equal(written["daily-capital"], "10000");
});

test("기준일 원금이 없던 배당만 고르면 수익률이 0 이다", () => {
	// 실측 사례(NAVER): 배당 1 건, 전량 매도 뒤 지급이라 분모에 기여하지 않는다.
	// 일수 합계에는 보유 기간이 있어 분모가 0 이 아니다 - 분자만 0 이어야 한다.
	const written = runSummary(
		[
			{
				grossAmount: "120000",
				netAmount: "102040",
				taxAmount: "17960",
				taxableAmount: "120000",
				averageDailyPrincipalCost: "5000000",
				averagePrincipalCost: "0",
				netWithPrincipalCost: "0",
			},
		],
		SUMMARY_DATASET,
	);

	assert.equal(written.yield, "0.00%", "서버 합계행은 0.00% 인데 선택 합계만 다른 값을 내면 안 된다");
	assert.equal(written["basis-yield"], "기준 0.00%");
});

test("두 수익률이 같은 분자를 쓴다", () => {
	// 원금이 있는 행만 고르면 분자가 같으므로, 분모가 같을 때 두 수익률도 같아야 한다.
	const written = runSummary(
		[
			{
				grossAmount: "900",
				netAmount: "800",
				taxAmount: "100",
				taxableAmount: "900",
				averageDailyPrincipalCost: "20000",
				averagePrincipalCost: "20000",
				netWithPrincipalCost: "800",
			},
		],
		SUMMARY_DATASET,
	);

	assert.equal(written.yield, "4.00%");
	assert.equal(written["basis-yield"], "기준 4.00%");
});
