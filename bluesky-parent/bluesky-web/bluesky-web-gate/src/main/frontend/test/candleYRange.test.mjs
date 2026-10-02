// 캔들 차트 세로축(사용자 요청 2026-10-02: "평단이 너무 낮으면 세로축은 캔들 기준으로 맞춰줘").
// 평단을 넣으면 축이 캔들 범위(저가~고가)의 30% 넘게 늘어날 때만 축을 캔들에 맞춘다(위아래 5% 여유). 실측: 삼성전자 최근 3 개월
// 캔들 25~30 만 · 평단 7.2 만 -> 축이 5 만까지 내려가 캔들이 위쪽에 몰렸다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const BUILT = "../resources/static/js/stock-charts.js";

function load() {
	const source = readFileSync(join(JTE_ROOT, BUILT), "utf8");
	const sandbox = { Math, Number, isFinite, Infinity };
	vm.createContext(sandbox);
	vm.runInContext(extractFunction(source, "candleYRange"), sandbox);
	return sandbox.candleYRange;
}

const c = (low, high, avg) => ({ open: low, high, low, close: high, avg });

test("평단이 너무 낮으면 축을 캔들에 맞추고 아래라고 적는다", () => {
	const r = load()([c(250000, 280000, 71887), c(260000, 300000, 71887)]);
	assert.ok(r, "축을 캔들에 맞춘다");
	// 캔들 250,000~300,000 (폭 50,000) 에서 위아래 5% -> 247,500 ~ 302,500, 보기 좋은 단위(1 만)로 내림 · 올림
	assert.equal(r.min, 240000);
	assert.equal(r.max, 310000);
	assert.equal(r.side, "below");
	assert.equal(r.lastAvg, 71887);
});

test("평단이 너무 높아도 같은 규칙, 위라고 적는다", () => {
	const r = load()([c(100, 110, 300), c(105, 120, 300)]);
	assert.equal(r.side, "above");
	// 캔들 100~120 (폭 20) 위아래 5% -> 99 ~ 121, 단위 1 -> 그대로
	assert.equal(r.max, 121);
	assert.equal(r.min, 99);
});

test("평단이 캔들 근처면 손대지 않는다(Chart.js 자동 - 평단 선이 다 보인다)", () => {
	assert.equal(load()([c(250000, 280000, 245000), c(260000, 300000, 247000)]), null, "범위의 30% 안");
	assert.equal(load()([c(100, 110, null)]), null, "평단이 없으면");
});

test("보유가 없던 봉(null)은 건너뛰고 마지막 평단을 적는다", () => {
	const r = load()([c(100, 110, 20), c(105, 120, null), c(108, 118, 25), c(110, 119, null)]);
	assert.equal(r.side, "below");
	assert.equal(r.lastAvg, 25);
});
