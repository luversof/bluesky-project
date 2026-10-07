// 주가 추이 툴팁(사용자 요청 2026-10-07): 금액 자리수를 맞춰 세우려고 한 줄을 이름 · 금액 · 덧붙임(퍼센트)으로 가른다.
//   ": " 가 없는 줄(▲ 매수 15주)은 이름 칸에만 - 금액 칸이 비어 표가 틀어지지 않게.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const BUILT = "../resources/static/js/stock-charts.js";

function load() {
	const source = readFileSync(join(JTE_ROOT, BUILT), "utf8");
	const sandbox = { String };
	vm.createContext(sandbox);
	vm.runInContext(extractFunction(source, "splitTooltipLine"), sandbox);
	return (line) => Array.from(sandbox.splitTooltipLine(line));
}

test("이름 · 금액 · 퍼센트로 가른다", () => {
	assert.deepEqual(load()("평가 손익: +₩1,009,170,921 (+278.37%)"), ["평가 손익", "+₩1,009,170,921", "(+278.37%)"]);
});

test("퍼센트가 없으면 셋째 칸은 비운다", () => {
	assert.deepEqual(load()("매수 원가: ₩362,525,079"), ["매수 원가", "₩362,525,079", ""]);
});

test("이름만 있는 줄은 그대로 둔다", () => {
	assert.deepEqual(load()("▲ 매수 15주"), ["▲ 매수 15주", "", ""]);
});
