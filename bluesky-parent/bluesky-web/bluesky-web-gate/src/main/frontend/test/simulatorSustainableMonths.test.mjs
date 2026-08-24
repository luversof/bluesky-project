// 고갈 시나리오의 "얼마나 버텼는가"가 개월까지 구분되는지 본다.
//
// 지속 기간을 연 단위로만 재면 고갈이 일어난 해를 통째로 세게 되어 최대 11개월을 버린다.
// 화면 표기는 연 단위 그대로지만(그 해에 고갈했다는 것은 사실이다), 시나리오 비교의 1순위로
// 쓰면 실제로 더 오래 버틴 쪽이 밀린다 - 연 단위로 동률이 나서 마지막 해 커버리지 같은
// 무관한 기준이 승부를 가르기 때문이다.
//
// 실측 2026-08-24(무작위 시나리오 4,000개 중 고갈 3,652개를 짝지은 141,648쌍):
//   개월을 보기 전  순위 역전 3,188쌍 (2.25%)
//   개월을 본 뒤    순위 역전 0쌍
// 역전 예시: 65개월 버틴 쪽이 62개월 버틴 쪽에 졌다(둘 다 "6년", 62개월 쪽 커버리지가 높았다).
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.document = globalThis.document ?? { getElementById: () => null };
globalThis.localStorage = globalThis.localStorage ?? {
	getItem: () => null,
	setItem: () => {},
	removeItem: () => {},
};

await import("../../resources/static/js/stock/stockSimulator.js");
const mod = globalThis.__stockWithdrawalSimulatorInternals;

function scenario(overrides) {
	return {
		id: "t",
		name: "테스트",
		principal: 1_000_000_000,
		currentPrice: 1_000,
		dividendYieldPct: 0,
		annualSpending: 0,
		annualPriceGrowthPct: 0,
		annualDividendGrowthPct: 0,
		annualSpendingGrowthPct: 0,
		years: 30,
		reinvestDividends: false,
		...overrides,
	};
}

/** 월 기록을 직접 따라가 총자산이 처음 0 이하가 되기 <b>전까지</b> 버틴 개월 수. */
function livedMonths(simulation) {
	let lived = 0;
	for (const record of simulation.records.slice(1)) {
		for (const month of record.monthlyRecords) {
			if (month.totalWealth <= 0) {
				return lived;
			}
			lived += 1;
		}
	}
	return lived;
}

test("sustainableMonths 가 월 기록에서 센 값과 같다", () => {
	// 성장률이 모두 0 이고 배당이 없으면 매달 정확히 지출만큼 줄어든다.
	// 원금 10억 / 연 지출 3억 -> 월 2,500만 -> 40개월에 0 이 되므로 39개월 버틴다.
	const simulation = mod.simulateScenario(
		scenario({ annualSpending: 300_000_000 }),
	);
	assert.equal(simulation.summary.depletionYear, 4);
	assert.equal(simulation.summary.depletionMonth, 4);
	assert.equal(simulation.summary.sustainableMonths, 39);
	assert.equal(simulation.summary.sustainableMonths, livedMonths(simulation));
});

test("같은 해에 고갈해도 몇 달 더 버틴 쪽이 더 큰 값을 갖는다", () => {
	// 둘 다 4년차에 고갈하지만 버틴 개월이 다르다.
	const slower = mod.simulateScenario(scenario({ annualSpending: 266_000_000 }));
	const faster = mod.simulateScenario(scenario({ annualSpending: 320_000_000 }));

	assert.equal(
		slower.summary.sustainableYears,
		faster.summary.sustainableYears,
		"연 단위로는 동률이어야 이 검사가 의미가 있다",
	);
	assert.ok(
		slower.summary.sustainableMonths > faster.summary.sustainableMonths,
		`개월로는 갈려야 한다: ${slower.summary.sustainableMonths} vs ${faster.summary.sustainableMonths}`,
	);
	assert.equal(slower.summary.sustainableMonths, livedMonths(slower));
	assert.equal(faster.summary.sustainableMonths, livedMonths(faster));
});

test("고갈하지 않으면 기간 전체를 개월로 센다", () => {
	const simulation = mod.simulateScenario(
		scenario({ annualSpending: 0, years: 7 }),
	);
	assert.equal(simulation.summary.depletionYear, null);
	assert.equal(simulation.summary.depletionMonth, null);
	assert.equal(simulation.summary.sustainableMonths, 7 * 12);
});

test("넘쳐서 멈추면 계산해 낸 마지막 해까지만 센다", () => {
	// 배당이 주가와 무관하게 성장하는 모델이라 재투자 + 무지출이면 배정밀도를 넘어선다.
	const simulation = mod.simulateScenario(
		scenario({
			dividendYieldPct: 5,
			annualDividendGrowthPct: 10,
			reinvestDividends: true,
			annualSpending: 0,
			years: 200,
		}),
	);
	assert.ok(simulation.summary.overflowYear, "이 입력은 넘쳐야 한다");
	assert.equal(
		simulation.summary.sustainableMonths,
		(simulation.summary.overflowYear - 1) * 12,
	);
	assert.equal(
		simulation.summary.sustainableYears,
		simulation.summary.overflowYear - 1,
	);
});

test("총자산 항등식: 매수·매도는 총자산을 바꾸지 않는다", () => {
	// 한 달의 총자산 = (직전 주식수 x 새 주가) + 직전 현금 + 월배당 - 월지출.
	// 주식을 팔거나 사는 것은 같은 가격으로 형태만 바꾸므로 총자산에 영향이 없다.
	const cases = [
		scenario({ dividendYieldPct: 8, annualSpending: 60_000_000, currentPrice: 4_210 }),
		scenario({ dividendYieldPct: 8, annualSpending: 60_000_000, reinvestDividends: true }),
		scenario({ annualPriceGrowthPct: -20, dividendYieldPct: 3, annualSpending: 40_000_000 }),
		scenario({ annualSpendingGrowthPct: 3, dividendYieldPct: 5, annualSpending: 50_000_000 }),
	];

	let checked = 0;
	for (const input of cases) {
		const simulation = mod.simulateScenario(input);
		let previousCash = simulation.records[0].cashReserve;
		let previousShares = simulation.records[0].shares;
		for (const record of simulation.records.slice(1)) {
			const plannedMonthlySpending = record.annualSpending / 12;
			for (const month of record.monthlyRecords) {
				const expected =
					previousShares * month.sharePrice +
					previousCash +
					month.monthlyDividend -
					plannedMonthlySpending;
				const scale = Math.max(1, Math.abs(expected));
				assert.ok(
					Math.abs(month.totalWealth - expected) / scale < 1e-12,
					`${record.year}년 ${month.month}월: 기대 ${expected} 실제 ${month.totalWealth}`,
				);
				checked += 1;
				previousCash = month.cashReserve;
				previousShares = month.shares;
			}
		}
	}
	// 검사가 조용히 0건이 되면 위 단언은 한 번도 돌지 않는다.
	assert.ok(checked >= 1000, `검사한 달이 너무 적다: ${checked}`);
});

test("시나리오 비교가 실제로 더 오래 버틴 쪽을 위로 올린다", () => {
	// 둘 다 4년차 고갈이라 연 단위로는 동률이다. 개월을 보지 않으면 마지막 해 커버리지 같은
	// 무관한 기준이 승부를 가른다.
	//
	// 실측 2026-08-24에 무작위 탐색으로 실제 역전 쌍을 뽑아 그대로 쓴다. 옛 순서(개월 없음)로는
	// 마지막 해 커버리지 0.4189 > 0.3757 때문에 62개월 쪽이 65개월 쪽을 이겼다.
	const inputs = [
		scenario({
			currentPrice: 41_946,
			dividendYieldPct: 3.08,
			annualSpending: 237_529_116,
			annualPriceGrowthPct: 6.93,
			annualDividendGrowthPct: 5,
			annualSpendingGrowthPct: -0.33,
		}),
		scenario({
			currentPrice: 21_909,
			dividendYieldPct: 9.44,
			annualSpending: 200_685_066,
			annualPriceGrowthPct: -9.54,
			annualDividendGrowthPct: 0.52,
			annualSpendingGrowthPct: -0.2,
		}),
	];
	const entries = inputs.map((input, index) => {
		const simulation = mod.simulateScenario(input);
		return {
			scenarioId: String(index),
			lived: livedMonths(simulation),
			values: mod.buildComparisonValues(simulation.summary, input.years),
		};
	});

	assert.equal(
		entries[0].values.sustainableYears,
		entries[1].values.sustainableYears,
		"연 단위로 동률이어야 이 검사가 의미가 있다",
	);
	assert.notEqual(entries[0].lived, entries[1].lived, "실제로 버틴 개월은 달라야 한다");

	// 개월을 빼면 무엇이 승부를 가르는지: 마지막 해 커버리지가 더 오래 버틴 쪽보다 높다.
	const worse = entries[0].lived > entries[1].lived ? entries[1] : entries[0];
	const longer = worse === entries[0] ? entries[1] : entries[0];
	assert.ok(
		worse.values.finalCoveragePct > longer.values.finalCoveragePct,
		"덜 버틴 쪽의 커버리지가 더 높아야 폴백이 반대로 고른다 - 이 쌍이 판별력을 갖는 이유다",
	);

	const ranked = [...entries].sort(mod.compareComparisonEntries);
	const better = entries[0].lived > entries[1].lived ? entries[0] : entries[1];
	assert.equal(
		ranked[0].scenarioId,
		better.scenarioId,
		`더 오래 버틴 쪽(${better.lived}개월)이 위로 와야 한다`,
	);
});
