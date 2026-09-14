// 도넛 범례의 조각 백분율은 함께 반올림해 합이 100.0% 가 되어야 한다.
//
// 실측 2026-09-11(전체 기간): 매매 화면 도넛 범례 43개의 합이 99.9% 였다(배당 화면 18개는 우연히 100.0).
// 표 쪽은 StockFormatUtil.balancedPct 가 같은 최대잔여법을 쓴다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const BUILT = "../resources/static/js/stock-charts.js";

function load() {
	const source = readFileSync(join(JTE_ROOT, BUILT), "utf8");
	const sandbox = { Math, Number, Array, StockCharts: {} };
	vm.createContext(sandbox);
	const body = extractFunction(source, "balancedPercents");
	vm.runInContext("function balancedPercents" + body.slice(body.indexOf("(")), sandbox);
	return sandbox.balancedPercents;
}

const sum = (list) => Math.round(list.reduce((a, b) => a + Number(b), 0) * 10) / 10;

test("여러 조각의 합이 100.0", () => {
	const f = load();
	assert.equal(sum(f([1, 1, 1], 1)), 100);
	assert.equal(sum(f([83.79, 4.7, 4.54, 3.43, 1.81, 1.29, 0.27, 0.13, 0.06], 1)), 100);
});

test("43조각(매매 도넛 모양)도 100.0", () => {
	const f = load();
	const values = Array.from({ length: 43 }, (_, i) => (i + 1) * 3.7);
	assert.equal(sum(f(values, 1)), 100);
});

test("빈 목록·합 0 은 안전하게 처리", () => {
	const f = load();
	assert.deepEqual(f([], 1), []);
	assert.deepEqual(f([0, 0], 1), ["0.0", "0.0"]);
});

test("한 조각은 100.0", () => {
	const f = load();
	assert.deepEqual(f([42], 1), ["100.0"]);
});
