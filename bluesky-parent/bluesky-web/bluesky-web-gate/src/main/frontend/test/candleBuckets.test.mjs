// 종목 상세 주가 캔들의 봉 묶기(사용자 요청 2026-10-02). 봉이 많으면(전체 기간 삼성전자 1,600 일) 뭉개지므로 일봉 -> 주봉 -> 월봉.
//   시가 = 첫날 시가, 종가 = 마지막 날 종가, 고가 = 최고, 저가 = 최저, 평단 = 마지막 날 평단(그 봉이 끝날 때). 주는 월요일 시작.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const BUILT = "../resources/static/js/stock-charts.js";

function load() {
	const source = readFileSync(join(JTE_ROOT, BUILT), "utf8");
	const sandbox = { Math, Number, String, Date, Array };
	vm.createContext(sandbox);
	vm.runInContext(extractFunction(source, "candleBuckets"), sandbox);
	return sandbox.candleBuckets;
}

function series(days) {
	return {
		labels: days.map((d) => d[0]),
		open: days.map((d) => d[1]),
		high: days.map((d) => d[2]),
		low: days.map((d) => d[3]),
		close: days.map((d) => d[4]),
		avg: days.map((d) => (d.length > 5 ? d[5] : null)),
	};
}

test("봉이 적으면 일봉 그대로", () => {
	const r = load()(series([["2026-09-28", 100, 110, 90, 105], ["2026-09-29", 105, 108, 101, 102]]), 130);
	assert.equal(r.unit, "day");
	assert.equal(r.candles.length, 2);
	assert.deepEqual([r.candles[1].open, r.candles[1].close], [105, 102]);
});

test("많으면 주봉: 월요일 시작, 시 = 첫날 시가 · 종 = 마지막 날 종가 · 고 = 최고 · 저 = 최저 · 평단 = 마지막 날", () => {
	const days = [
		["2026-09-21", 100, 120, 95, 110, null], // 월
		["2026-09-23", 110, 130, 105, 125, 90], // 수
		["2026-09-25", 125, 126, 80, 85, 92], // 금
		["2026-09-28", 85, 90, 84, 88, null], // 다음 주 월(보유 없음)
	];
	const r = load()(series(days), 3);
	assert.equal(r.unit, "week");
	assert.equal(r.candles.length, 2);
	const w = r.candles[0];
	assert.deepEqual([w.open, w.high, w.low, w.close, w.avg, w.label], [100, 130, 80, 85, 92, "2026-09-25"]);
	assert.equal(r.candles[1].avg, null, "보유가 없는 주는 평단 선을 끊는다");
});

test("주봉도 많으면 월봉", () => {
	const days = [];
	for (let m = 1; m <= 6; m++) {
		for (const dd of ["05", "12", "19"]) days.push([`2026-0${m}-${dd}`, m * 10, m * 10 + 5, m * 10 - 5, m * 10 + 1]);
	}
	const r = load()(series(days), 8);
	assert.equal(r.unit, "month");
	assert.equal(r.candles.length, 6);
	assert.equal(r.candles[0].key, "2026-01");
});

test("종가가 없는 날은 뺀다", () => {
	const r = load()(series([["2026-09-28", 1, 1, 1, null], ["2026-09-29", 2, 2, 2, 2]]), 130);
	assert.equal(r.candles.length, 1);
});

test("주봉 · 월봉으로 묶을 때 내 매수 · 매도 수량도 합친다(▲ · ▼ 표시)", () => {
	const s = series([
		["2026-09-21", 100, 120, 95, 110],
		["2026-09-23", 110, 130, 105, 125],
		["2026-09-28", 85, 90, 84, 88],
	]);
	s.buy = [10, 5, 0];
	s.sell = [0, 3, 7];
	s.dist = [0, 540, 0];
	const r = load()(s, 2);
	assert.equal(r.unit, "week");
	assert.deepEqual([r.candles[0].buy, r.candles[0].sell, r.candles[1].buy, r.candles[1].sell], [15, 3, 0, 7]);
	assert.deepEqual([r.candles[0].dist, r.candles[1].dist], [540, 0], "분배락 금액도 그 주로 합친다");
	const daily = load()(s, 130);
	assert.deepEqual(daily.candles.map((c) => c.buy), [10, 5, 0], "일봉은 그대로");
});

