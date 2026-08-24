// 복리 시뮬레이터의 계산을 Node 내장 러너로 검증한다.
//
// 이 계산은 화면에 그대로 나가는데 지금까지 어떤 테스트도 없었다. 기대값은 코드가 아니라
// 연금(annuity) 폐쇄형 공식에서 따로 구한 값이다 - 구현을 그대로 베끼면 틀린 것을 고정하게 된다.
//
//   기초 납입: FV = P(1+i)^n + PMT * ((1+i)^n - 1)/i * (1+i)
//   기말 납입: FV = P(1+i)^n + PMT * ((1+i)^n - 1)/i
//   월 모드는 i = 연이율/12, n = 12 * 연수 (일반적인 적금 계산기 관례)
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

// 이 모듈은 최상위 IIFE 에서 DOM 을 찾는다. 없으면 즉시 빠져나가므로 최소 스텁이면 된다.
globalThis.window = globalThis.window ?? {};
globalThis.document = globalThis.document ?? { getElementById: () => null };

// 이 파일은 classic <script> 로 로드되므로 export 를 넣을 수 없다(넣으면 브라우저가 파일 전체를 거부한다).
// 대신 모듈이 전역에 붙여 둔 계산 함수를 읽는다. import 는 파일을 실행시키기 위한 것이다.
await import("../../resources/static/js/stock/compoundSimulator.js");
const mod = globalThis.__stockCompoundSimulatorInternals;

function closedForm({ initial, contribution, ratePct, years, monthly, contributeAtBegin }) {
	const periods = monthly ? 12 : 1;
	const i = monthly ? ratePct / 100 / 12 : ratePct / 100;
	const n = years * periods;
	const fvInitial = initial * Math.pow(1 + i, n);
	let fvPmt;
	if (i === 0) {
		fvPmt = contribution * n;
	} else {
		fvPmt = contribution * ((Math.pow(1 + i, n) - 1) / i);
		if (contributeAtBegin) fvPmt *= 1 + i;
	}
	return fvInitial + fvPmt;
}

function last(input) {
	const rows = mod.projectCompound(input);
	return rows[rows.length - 1];
}

const cases = [
	{ initial: 10_000_000, contribution: 500_000, ratePct: 7, years: 10, monthly: true, contributeAtBegin: true },
	{ initial: 10_000_000, contribution: 500_000, ratePct: 7, years: 10, monthly: true, contributeAtBegin: false },
	{ initial: 0, contribution: 1_000_000, ratePct: 5, years: 20, monthly: true, contributeAtBegin: true },
	{ initial: 50_000_000, contribution: 0, ratePct: 3, years: 5, monthly: false, contributeAtBegin: true },
	{ initial: 1_000_000, contribution: 100_000, ratePct: 10, years: 30, monthly: false, contributeAtBegin: false },
	{ initial: 1_000_000, contribution: 100_000, ratePct: 0, years: 10, monthly: true, contributeAtBegin: true },
	{ initial: 1_000_000, contribution: 100_000, ratePct: -5, years: 10, monthly: true, contributeAtBegin: true },
];

test("최종 잔액이 연금 폐쇄형 공식과 같다", () => {
	for (const input of cases) {
		const actual = last(input).balance;
		const expected = closedForm(input);
		const rel = Math.abs(actual - expected) / Math.max(Math.abs(expected), 1);
		assert.ok(rel < 1e-9, `${JSON.stringify(input)}: ${actual} != ${expected}`);
	}
});

test("누적 원금은 초기값 + 납입 합계다", () => {
	const input = { initial: 10_000_000, contribution: 500_000, ratePct: 7, years: 10, monthly: true, contributeAtBegin: true };
	const rows = mod.projectCompound(input);

	assert.equal(rows.length, 10);
	assert.equal(rows[0].contribution, 500_000 * 12);
	assert.equal(
		rows[rows.length - 1].cumulativePrincipal,
		10_000_000 + 500_000 * 12 * 10,
	);
	// 연차별 수익 합계 = 최종 잔액 - 누적 원금
	const gainSum = rows.reduce((acc, r) => acc + r.gain, 0);
	const lastRow = rows[rows.length - 1];
	assert.ok(
		Math.abs(gainSum - (lastRow.balance - lastRow.cumulativePrincipal)) < 1e-6,
		`수익 합계 ${gainSum} != ${lastRow.balance - lastRow.cumulativePrincipal}`,
	);
});

// 화면의 min/max 는 타이핑을 막지 못하므로 코드에서 잘라야 한다.
// -100% 아래로 내려가면 잔액이 양수와 음수를 오간다(실측: -150% 로 10년 -> 1년차 -5,500,000 /
// 2년차 +2,250,000 / 3년차 -1,625,000).
test("입력 범위를 벗어난 값은 잘라 낸다", () => {
	assert.deepEqual(
		mod.projectCompound({ initial: 1_000_000, contribution: 0, ratePct: -500, years: 3, monthly: false, contributeAtBegin: true }),
		mod.projectCompound({ initial: 1_000_000, contribution: 0, ratePct: mod.COMPOUND_MIN_RATE_PCT, years: 3, monthly: false, contributeAtBegin: true }),
	);
	assert.deepEqual(
		mod.projectCompound({ initial: 1_000_000, contribution: 0, ratePct: 9999, years: 3, monthly: false, contributeAtBegin: true }),
		mod.projectCompound({ initial: 1_000_000, contribution: 0, ratePct: mod.COMPOUND_MAX_RATE_PCT, years: 3, monthly: false, contributeAtBegin: true }),
	);
	assert.equal(
		mod.projectCompound({ initial: 0, contribution: 0, ratePct: 5, years: 9999, monthly: false, contributeAtBegin: true }).length,
		mod.COMPOUND_MAX_YEARS,
	);
	assert.equal(
		mod.projectCompound({ initial: 0, contribution: 0, ratePct: 5, years: 0, monthly: false, contributeAtBegin: true }).length,
		1,
	);
	assert.equal(
		mod.projectCompound({ initial: 0, contribution: 0, ratePct: 5, years: -7, monthly: false, contributeAtBegin: true }).length,
		1,
	);
});

test("음수 금액은 0 으로 본다", () => {
	const rows = mod.projectCompound({ initial: -5_000_000, contribution: -100_000, ratePct: 10, years: 2, monthly: false, contributeAtBegin: true });
	assert.equal(rows[0].cumulativePrincipal, 0);
	assert.equal(rows[rows.length - 1].balance, 0);
});

// 기초 납입은 그 해 이자를 한 번 더 받으므로 항상 기말보다 크다(이율이 양수일 때).
test("기초 납입이 기말 납입보다 크다", () => {
	const base = { initial: 0, contribution: 1_000_000, ratePct: 6, years: 5, monthly: true };
	const begin = last({ ...base, contributeAtBegin: true }).balance;
	const end = last({ ...base, contributeAtBegin: false }).balance;
	assert.ok(begin > end, `${begin} <= ${end}`);
});
