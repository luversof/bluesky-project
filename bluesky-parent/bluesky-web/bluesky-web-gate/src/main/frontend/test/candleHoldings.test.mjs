// 종목 상세: 보유 평가액 추이 차트를 주가 캔들에 합쳤다(사용자 요청 2026-10-07: "굳이 2개가 나뉘어있을 필요가 없을거 같아").
//   그 날 보유 평가액(hv) · 원가(hc)가 봉에 실려 툴팁에 나온다. 주봉 · 월봉은 평단처럼 그 봉의 마지막 날 값이다(합치면 뜻이 없다).
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
		open: days.map(() => 100),
		high: days.map(() => 110),
		low: days.map(() => 90),
		close: days.map(() => 100),
		avg: days.map(() => 95),
		hv: days.map((d) => d[1]),
		hc: days.map((d) => d[2]),
	};
}

test("일봉에는 그 날 보유 평가액과 원가가 그대로 실린다", () => {
	const r = load()(series([["2026-09-21", 1000, 900], ["2026-09-22", null, null]]));
	assert.equal(r.unit, "day");
	assert.equal(r.candles[0].hv, 1000);
	assert.equal(r.candles[0].hc, 900);
	assert.equal(r.candles[1].hv, null, "보유가 없던 날은 null - 0 원으로 적으면 다 판 것처럼 읽힌다");
});

test("주봉은 그 주 마지막 날의 평가액이다", () => {
	// 3 주 x 5 일, 봉 수 상한 2 -> 주봉
	const days = [];
	const start = new Date("2026-09-07T00:00:00Z");
	for (let w = 0; w < 3; w++) {
		for (let d = 0; d < 5; d++) {
			const at = new Date(start.getTime() + (w * 7 + d) * 86400000).toISOString().slice(0, 10);
			days.push([at, (w + 1) * 1000 + d, (w + 1) * 900]);
		}
	}
	const r = load()(series(days), 10);
	assert.equal(r.unit, "week");
	assert.deepEqual(Array.from(r.candles, (c) => c.hv), [1004, 2004, 3004]);
	assert.deepEqual(Array.from(r.candles, (c) => c.hc), [900, 1800, 2700]);
});
