// 인출(배당 생활) 시뮬레이터의 계산을 Node 내장 러너로 검증한다.
//
// 이 계산도 화면에 그대로 나가는데 지금까지 어떤 테스트도 없었다. 기대값은 구현을 베끼지 않고
// 모델에서 유도한다 - 구현을 옮겨 적으면 틀린 것을 고정하게 된다.
//
// 모델(코드에서 읽어 정리):
//   보유주식 = floor(원금 / 현재가), 남는 돈은 현금
//   주당 연배당 = 현재가 x 배당수익률,  월 배당 = 보유주식 x 주당연배당 / 12
//   매월: 주가 성장 -> 현금 += (월배당 - 월지출) -> 현금이 음수면 주식 매도, 재투자면 매수
//   주식 매도는 같은 가격으로 주식을 현금으로 바꾸는 것이라 총자산을 바꾸지 않는다
//   => 성장률이 모두 0 이면 Y 년 뒤 총자산 = 원금 - 연지출 x Y
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.document = globalThis.document ?? { getElementById: () => null };
globalThis.localStorage = globalThis.localStorage ?? {
	getItem: () => null,
	setItem: () => {},
	removeItem: () => {},
};

// 이 파일은 classic <script> 로 로드되므로 export 를 넣을 수 없다(넣으면 브라우저가 파일 전체를 거부한다).
// 대신 모듈이 전역에 붙여 둔 계산 함수를 읽는다. import 는 파일을 실행시키기 위한 것이다.
await import("../../resources/static/js/stock/stockSimulator.js");
const mod = globalThis.__stockWithdrawalSimulatorInternals;

function scenario(overrides) {
	return {
		id: "t",
		name: "테스트",
		principal: 1_000_000,
		currentPrice: 1_000,
		dividendYieldPct: 0,
		annualSpending: 0,
		annualPriceGrowthPct: 0,
		annualDividendGrowthPct: 0,
		annualSpendingGrowthPct: 0,
		years: 1,
		reinvestDividends: false,
		...overrides,
	};
}

test("연 성장률을 월 성장률로 바꾸면 12개월 복리가 연 성장률과 같다", () => {
	for (const annual of [0.07, 0.03, -0.2, 1.5]) {
		const monthly = mod.annualRateToMonthlyRate(annual);
		const compounded = Math.pow(1 + monthly, 12) - 1;
		assert.ok(
			Math.abs(compounded - annual) < 1e-12,
			`${annual}: ${compounded}`,
		);
	}
	assert.equal(mod.annualRateToMonthlyRate(0), 0);
	// -100% 이하는 잔액이 부호를 오가므로 -1 에서 자른다.
	assert.equal(mod.annualRateToMonthlyRate(-1), -1);
	assert.equal(mod.annualRateToMonthlyRate(-5), -1);
	assert.equal(mod.annualRateToMonthlyRate("숫자아님"), 0);
	assert.equal(mod.annualRateToMonthlyRate(undefined), 0);
});

test("아무 일도 없으면 총자산이 원금 그대로다", () => {
	const result = mod.simulateScenario(scenario({ years: 5 }));

	assert.equal(result.summary.finalWealth, 1_000_000);
	assert.equal(result.summary.depletionYear, null);
	assert.equal(result.records.at(-1).shares, 1_000);
});

// 성장률이 0 이면 매도는 총자산을 바꾸지 않으므로, 줄어드는 것은 지출뿐이다.
test("성장이 없으면 총자산은 딱 지출만큼 줄어든다", () => {
	for (const years of [1, 3, 7]) {
		const result = mod.simulateScenario(
			scenario({ annualSpending: 120_000, years }),
		);
		assert.ok(
			Math.abs(result.summary.finalWealth - (1_000_000 - 120_000 * years)) < 1e-6,
			`${years}년: ${result.summary.finalWealth}`,
		);
	}
});

test("배당이 지출을 정확히 상쇄하면 주식을 팔지 않는다", () => {
	// 주식 1,000주 x 주당 연배당 50원 = 연 50,000원. 지출도 같게 둔다.
	const result = mod.simulateScenario(
		scenario({ dividendYieldPct: 5, annualSpending: 50_000, years: 10 }),
	);

	for (const record of result.records.slice(1)) {
		assert.equal(record.soldSharesForSpending, 0, `${record.year}년차에 매도 발생`);
		assert.equal(record.shares, 1_000);
	}
	assert.ok(Math.abs(result.summary.finalWealth - 1_000_000) < 1e-6);
	assert.equal(result.summary.firstShareSaleYear, null);
	assert.equal(result.summary.depletionYear, null);
});

test("지출이 자산을 넘으면 고갈 연도를 알리고 거기서 멈춘다", () => {
	const result = mod.simulateScenario(
		scenario({ principal: 100_000, annualSpending: 120_000, years: 30 }),
	);

	assert.equal(result.summary.depletionYear, 1);
	assert.equal(result.summary.sustainableYears, 1);
	// 고갈 뒤로는 더 계산하지 않는다(초기 기록 + 1년치).
	assert.equal(result.records.length, 2);
});

test("재투자를 켜면 배당으로 주식이 늘어난다", () => {
	const withReinvest = mod.simulateScenario(
		scenario({ dividendYieldPct: 5, years: 10, reinvestDividends: true }),
	);
	const without = mod.simulateScenario(
		scenario({ dividendYieldPct: 5, years: 10, reinvestDividends: false }),
	);

	assert.ok(
		withReinvest.records.at(-1).shares > 1_000,
		`재투자했는데 주식이 늘지 않았다: ${withReinvest.records.at(-1).shares}`,
	);
	assert.equal(without.records.at(-1).shares, 1_000);
	// 재투자 여부와 무관하게 배당 총액만큼 자산이 늘어난 것은 같다(성장률 0).
	assert.ok(withReinvest.summary.finalWealth >= without.summary.finalWealth);
});

// 보유량보다 많이 팔면 주식이 음수가 되어 평가액이 음수로 새어 나간다.
test("보유량보다 많이 팔지 않는다", () => {
	const settled = mod.settleCashFlow({
		sharePrice: 1_000,
		shares: 3,
		cashReserve: -1_000_000,
		reinvestDividends: false,
	});

	assert.equal(settled.shares, 0);
	assert.equal(settled.soldSharesForSpending, 3);
	// 부족분은 현금이 음수로 남는다(없는 주식을 만들어 내지 않는다).
	assert.equal(settled.cashReserve, -1_000_000 + 3 * 1_000);
});

test("현금이 남아 있으면 팔지 않고, 재투자는 정수 주로만 산다", () => {
	const noSale = mod.settleCashFlow({
		sharePrice: 1_000,
		shares: 10,
		cashReserve: 500,
		reinvestDividends: false,
	});
	assert.equal(noSale.soldSharesForSpending, 0);
	assert.equal(noSale.shares, 10);

	const reinvested = mod.settleCashFlow({
		sharePrice: 1_000,
		shares: 10,
		cashReserve: 2_500,
		reinvestDividends: true,
	});
	assert.equal(reinvested.reinvestedShares, 2);
	assert.equal(reinvested.shares, 12);
	assert.equal(reinvested.cashReserve, 500);
});

// 화면은 평가액·현금·총자산을 각각 보여 준다. 세 값이 어긋나면 어느 것도 믿을 수 없는데, 개별 동작
// 테스트만으로는 그 관계가 지켜지는지 알 수 없다.
//
// 실측 2026-08-23: 성장·배당·지출이 모두 걸린 10년, 재투자 8년, 고갈 시나리오, 무성장 5년에서
// marketValue = 주식수 x 주가, totalWealth = marketValue + cashReserve 가 오차 0 으로 성립했다.
// 무작위 3000 시나리오로도 위 두 항등식 위반 0(최대오차 0), 음수 현금 1996행 전부가
// 주식 0 이면서 소진 연도였다.
//
// 처음엔 "현금 >= 0" 을 넣었다가 고갈 시나리오에서 -18,253,708 로 깨졌다. 그건 코드 결함이
// 아니라 못 채운 지출(부족액)이라 단정이 틀린 것이었고, 아래처럼 조건부로 바꿨다.
test("평가액과 총자산이 주식·현금과 어긋나지 않는다", () => {
	const cases = [
		{ dividendYieldPct: 4, annualSpending: 5_000_000, annualPriceGrowthPct: 6, annualDividendGrowthPct: 5, annualSpendingGrowthPct: 3, years: 10 },
		{ dividendYieldPct: 5, annualPriceGrowthPct: 4, years: 8, reinvestDividends: true },
		{ dividendYieldPct: 1, annualSpending: 30_000_000, years: 10 },
		{ years: 5 },
		// UI 가 허용하는 하한(min="-100"): 주가가 0 이 되어 매도로 지출을 못 메운다.
		{ annualPriceGrowthPct: -100, annualSpending: 30_000_000, years: 5 },
	];

	for (const overrides of cases) {
		const result = mod.simulateScenario(
			scenario({ principal: 100_000_000, currentPrice: 50_000, ...overrides }),
		);
		assert.ok(result.records.length > 0, "연차가 하나도 없다");
		for (const row of result.records) {
			assert.ok(
				Math.abs(row.shares * row.sharePrice - row.marketValue) < 1e-6,
				`${row.year}년 평가액이 주식수 x 주가와 다르다: ${row.marketValue}`,
			);
			assert.ok(
				Math.abs(row.marketValue + row.cashReserve - row.totalWealth) < 1e-6,
				`${row.year}년 총자산이 평가액 + 현금과 다르다: ${row.totalWealth}`,
			);
			assert.ok(row.shares >= 0, `${row.year}년 주식이 음수다: ${row.shares}`);
			// 현금은 음수가 될 수 있다 - 지출을 다 대지 못한 부족액이다. 다만 그건 팔 주식이
			// 하나도 남지 않은 소진 연도에서만 나와야 한다. 주식이 남았는데 현금이 음수라면
			// settleCashFlow 가 매도로 메우지 못한 것이다.
			if (row.cashReserve < -1e-6) {
				// 주가가 0 이면(주가성장 -100%) 팔 수 있는 값이 없어 주식이 남은 채로 음수가 된다.
				assert.ok(
					row.shares === 0 || row.sharePrice === 0,
					`${row.year}년 주식이 남았는데 현금이 음수다: 주식 ${row.shares} 주가 ${row.sharePrice} 현금 ${row.cashReserve}`,
				);
				assert.strictEqual(
					row.year,
					result.summary.depletionYear,
					`${row.year}년 현금이 음수인데 소진 연도(${result.summary.depletionYear})가 아니다`,
				);
			}
		}
	}
});

// 재투자를 켜고 지출이 없으면 주식 수가 기하급수로 늘어, 화면이 낼 수 있는 범위를 넘길 수 있다.
// 실측 2026-08-23(가드 넣기 전): 지출 0 / 재투자 / 주가성장 0% / 배당성장 10% / 100년 에서
// 90년차부터 totalWealth 가 NaN 이 되어 마지막 11개 연차와 최종 자산이 ₩NaN 으로 나갔다.
// 배당성장 20~200% 에서도 각각 55/42/31/24/21/18/16년차부터 같은 증상이었다.
test("표현 범위를 넘으면 NaN 을 내보내지 않고 거기서 멈춘다", () => {
	const result = mod.simulateScenario(
		scenario({
			principal: 100_000_000,
			currentPrice: 50_000,
			dividendYieldPct: 4,
			annualSpending: 0,
			annualPriceGrowthPct: 0,
			annualDividendGrowthPct: 10,
			annualSpendingGrowthPct: 0,
			years: 100,
			reinvestDividends: true,
		}),
	);

	for (const row of result.records) {
		assert.ok(
			Number.isFinite(row.totalWealth) &&
				Number.isFinite(row.marketValue) &&
				Number.isFinite(row.cashReserve) &&
				Number.isFinite(row.shares),
			`${row.year}년에 표현할 수 없는 값이 남아 있다: 총자산 ${row.totalWealth} 주식 ${row.shares}`,
		);
	}
	assert.ok(
		Number.isFinite(result.summary.finalWealth),
		`최종 자산이 유한하지 않다: ${result.summary.finalWealth}`,
	);

	// 100년을 넣었지만 89년차까지만 표현할 수 있었다 - 그 사실이 요약에 남아야 한다.
	assert.strictEqual(result.summary.overflowYear, 90);
	assert.strictEqual(result.records.at(-1).year, 89);
	// 지속 연수를 입력값(100)으로 두면 "100년 버틴다"고 잘못 말하게 된다.
	assert.strictEqual(result.summary.sustainableYears, 89);
});
