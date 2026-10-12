// 주가 추이 캔들 툴팁을 키보드로(2026-10-08, WCAG 2.1.1). ← → 로 봉을 옮기고 Home · End 로 끝으로. 처음 누르면 왼쪽은 마지막 봉,
// 오른쪽은 첫 봉. 끝에서는 멈춘다(돌아가지 않는다). 모르는 키는 -2(처리 안 함 - 탭 이동 등을 가로채지 않는다).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const BUILT = "../resources/static/js/stock-charts.js";

function load() {
	const source = readFileSync(join(JTE_ROOT, BUILT), "utf8");
	const sandbox = { Math };
	vm.createContext(sandbox);
	vm.runInContext(extractFunction(source, "nextCandleIndex"), sandbox);
	return sandbox.nextCandleIndex;
}

test("처음 누르면 왼쪽은 마지막 봉, 오른쪽은 첫 봉", () => {
	const next = load();
	assert.equal(next("ArrowLeft", -1, 60), 59);
	assert.equal(next("ArrowRight", -1, 60), 0);
});

test("한 칸씩 옮기고 끝에서 멈춘다", () => {
	const next = load();
	assert.equal(next("ArrowRight", 10, 60), 11);
	assert.equal(next("ArrowLeft", 10, 60), 9);
	assert.equal(next("ArrowLeft", 0, 60), 0);
	assert.equal(next("ArrowRight", 59, 60), 59);
	assert.equal(next("Home", 30, 60), 0);
	assert.equal(next("End", 3, 60), 59);
});

test("PageUp · PageDown 은 10 칸씩, 끝에서 멈추고 처음엔 화살표처럼 연다(S82)", () => {
	const next = load();
	assert.equal(next("PageUp", 30, 60), 20);
	assert.equal(next("PageDown", 30, 60), 40);
	assert.equal(next("PageUp", 4, 60), 0);
	assert.equal(next("PageDown", 55, 60), 59);
	assert.equal(next("PageUp", -1, 60), 59);
	assert.equal(next("PageDown", -1, 60), 0);
	assert.equal(next("PageDown", -1, 0), -2);
});

test("다른 키 · 봉이 없을 때는 처리하지 않는다", () => {
	const next = load();
	assert.equal(next("Tab", 3, 60), -2);
	assert.equal(next("Enter", 3, 60), -2);
	assert.equal(next("ArrowLeft", -1, 0), -2);
});
