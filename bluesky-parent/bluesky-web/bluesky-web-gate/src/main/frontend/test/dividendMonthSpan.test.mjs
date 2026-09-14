// 월평균의 분모는 '걸친 달력 월 수' 가 아니라 기간 길이(월 환산)여야 한다.
//
// 실측 2026-09-11(배당 화면, 실데이터): 걸친 달력 월 수로 나누면
//   1개월 프리셋 2026-08-12~09-11 (31일)  -> 2개월   월평균 2,428,796 (실제의 -49%)
//   3개월        2026-06-12~09-11 (92일)  -> 4개월   (-25%)
//   6개월        2026-03-12~09-11 (184일) -> 7개월   (-14%)
//   12개월       2025-09-12~26-09-11(365일)-> 13개월  (-8%)
// 기간이 달 경계를 넘을 때마다 분모가 한 달씩 부풀기 때문이다. 연 환산 수익률이 이미 일수 기준(365/기간일수)이므로
// 같은 규칙(달마다 덮은 일수 / 그 달의 일수)으로 맞춘다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const TEMPLATE = "../resources/static/js/stock/dividendHistory.js";

function load({ dividendData = [], filterStartDay = "", filterEndDay = "" }) {
	const source = readFileSync(join(JTE_ROOT, TEMPLATE), "utf8");
	const sandbox = { Number, String, Object, Math, Date, isNaN, dividendData, filterStartDay, filterEndDay };
	vm.createContext(sandbox);
	const bodies = ["monthEquivalent", "averageMonthSpan"]
		.map((name) => extractFunction(source, name))
		.join(String.fromCharCode(10));
	vm.runInContext(bodies, sandbox);
	return sandbox;
}

test("한 달을 꽉 채우면 1.0 개월", () => {
	const s = load({});
	assert.equal(Math.round(s.monthEquivalent("2026-08-01", "2026-08-31") * 1000) / 1000, 1);
	assert.equal(Math.round(s.monthEquivalent("2026-02-01", "2026-02-28") * 1000) / 1000, 1);
});

test("달 경계를 넘어도 31일은 약 1개월이다", () => {
	const s = load({});
	// 2026-08-12~09-11 = 8월 20일 + 9월 11일 = 20/31 + 11/30
	const span = s.monthEquivalent("2026-08-12", "2026-09-11");
	assert.ok(span > 1.0 && span < 1.05, "31일 구간이 " + span + " 개월로 나왔다");
});

test("92일은 약 3개월, 365일은 약 12개월", () => {
	const s = load({});
	const q = s.monthEquivalent("2026-06-12", "2026-09-11");
	const y = s.monthEquivalent("2025-09-12", "2026-09-11");
	assert.ok(q > 2.95 && q < 3.1, "92일이 " + q + " 개월");
	assert.ok(y > 11.9 && y < 12.1, "365일이 " + y + " 개월");
});

test("하루는 한 달이 아니다", () => {
	const s = load({});
	const one = s.monthEquivalent("2026-09-11", "2026-09-11");
	assert.ok(one > 0.03 && one < 0.04, "하루가 " + one + " 개월");
});

test("잘못된 입력은 0", () => {
	const s = load({});
	assert.equal(s.monthEquivalent("", "2026-09-11"), 0);
	assert.equal(s.monthEquivalent("2026-09-11", "2026-09-01"), 0, "역순 구간");
});

test("필터가 비면 데이터의 첫 배당일 ~ 마지막 배당일을 쓴다", () => {
	const s = load({ dividendData: [{ payDate: "2026-01-15", net: 1 }, { payDate: "2026-03-14", net: 1 }] });
	const span = s.averageMonthSpan();
	// 2026-01-15~03-14 = 17/31 + 1 + 14/31
	assert.ok(span > 1.9 && span < 2.1, "span " + span);
});

test("필터가 있으면 필터 기간을 쓴다", () => {
	const s = load({ dividendData: [{ payDate: "2026-03-14", net: 1 }], filterStartDay: "2026-01-01", filterEndDay: "2026-12-31" });
	assert.equal(Math.round(s.averageMonthSpan() * 100) / 100, 12);
});
