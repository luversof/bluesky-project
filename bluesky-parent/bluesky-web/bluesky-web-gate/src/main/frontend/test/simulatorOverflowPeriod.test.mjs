// 넘쳐서 멈춘 시뮬레이션을 "끝까지 버텼다" 로 말하면 안 된다.
//
// 배당이 주가와 무관하게 성장하는 모델이라, 재투자를 켜고 지출이 없으면 주식 수가 기하급수로
// 는다. 어느 해부터는 배정밀도 범위를 넘어 총자산이 NaN 이 되므로 그 해는 기록하지 않고 멈춘다
// (simulateScenario 의 isFiniteRecord).
//
// 실측 2026-09-15(원금 10억 · 현재가 10만 · 소비 0 · 주가성장 0% · 배당성장 10% · 재투자 ON · 100년):
//   차트는 89 년까지만 그렸고(90 점) 요약 카드 주석도 "숫자 표현 한계로 중단: 90년 후" 라고 적는데,
//   머리 숫자는 "100년+" 였다. 시나리오 카드에는 그 주석조차 없어 바로잡을 길이 없었다.
//   summary.sustainableYears 는 그때도 89 를 들고 있었다(시나리오 비교는 그 값을 쓴다) -
//   화면 문구만 입력한 기간을 그대로 적고 있었던 것이다.
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
		currentPrice: 100_000,
		dividendYieldPct: 4,
		annualSpending: 0,
		annualPriceGrowthPct: 0,
		annualDividendGrowthPct: 10,
		annualSpendingGrowthPct: 0,
		years: 100,
		reinvestDividends: true,
		...overrides,
	};
}

test("넘침으로 멈추면 요약이 실제로 계산해 낸 해를 말한다", () => {
	const result = mod.simulateScenario(scenario());
	const summary = result.summary;

	// 전제: 이 입력은 실제로 넘쳐서 멈춘다(안 멈추면 이 시험이 아무것도 안 보는 것이다).
	assert.ok(summary.overflowYear, "이 입력은 넘쳐서 멈춰야 한다");
	assert.equal(summary.depletionYear, null, "고갈이 아니라 넘침이다");

	// 마지막으로 기록한 해 = 그린 점의 마지막. 넘친 해는 기록하지 않으므로 overflowYear - 1 이다.
	const lastRecordedYear = result.records.at(-1).year;
	assert.equal(lastRecordedYear, summary.overflowYear - 1);
	assert.equal(summary.sustainableYears, summary.overflowYear - 1);

	// 화면 문구가 그 해를 말해야 한다. 입력한 기간(100)을 적으면 안 된다.
	const facts = mod.sustainablePeriodFacts(summary, 100);
	const shown = String(facts.year);
	// 넘침은 '적어도 그만큼' 이다 - 거기서 딱 끊긴 것이 아니라 더 못 세는 것뿐이다.
	assert.equal(facts.orMore, true);
	assert.ok(
		shown.includes(String(lastRecordedYear)),
		"표기가 실제 마지막 해여야 한다: " + shown,
	);
	assert.ok(
		!shown.includes("100"),
		"넘쳐서 멈췄는데 입력한 기간을 그대로 적으면 안 된다: " + shown,
	);
});

test("끝까지 버틴 경우는 입력한 기간을 그대로 말한다", () => {
	// 배당도 지출도 없고 성장도 없으면 총자산이 그대로라 고갈도 넘침도 없다.
	const result = mod.simulateScenario(
		scenario({
			dividendYieldPct: 0,
			annualDividendGrowthPct: 0,
			reinvestDividends: false,
			years: 30,
		}),
	);
	const summary = result.summary;
	assert.equal(summary.overflowYear, null);
	assert.equal(summary.depletionYear, null);
	assert.equal(summary.sustainableYears, 30);

	const facts = mod.sustainablePeriodFacts(summary, 30);
	assert.ok(String(facts.year).includes("30"), "끝까지 버텼으면 30 을 말한다: " + facts.year);
	assert.equal(facts.orMore, true, "끝까지 갔으면 '적어도 그만큼' 이다");
});

test("고갈한 경우는 고갈한 해를 말한다", () => {
	// 원금 1,000 을 배당 없이 연 400 씩 쓰면 3년차에 바닥난다.
	const result = mod.simulateScenario(
		scenario({
			principal: 10_000,
			currentPrice: 100,
			dividendYieldPct: 0,
			annualDividendGrowthPct: 0,
			annualSpending: 4_000,
			reinvestDividends: false,
			years: 30,
		}),
	);
	const summary = result.summary;
	assert.equal(summary.depletionYear, 3, "1만원을 연 4천원씩 쓰면 3년차");
	assert.equal(summary.overflowYear, null);

	const facts = mod.sustainablePeriodFacts(summary, 30);
	assert.equal(facts.year, 3, "고갈한 해를 말한다");
	// 고갈은 딱 그 해까지다 - '+' 를 붙이면 더 버텼다는 뜻이 되어 거짓말이 된다.
	assert.equal(facts.orMore, false);
});
