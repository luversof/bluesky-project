// 건수 문구 "{0,choice,0#{0} items|1#{0} item|1<{0} items}" 를 브라우저도 서버(MessageFormat)와 같게 고르는지.
//
// 2026-10-01 영어 점검: "1 items" 가 화면 곳곳에 나왔다. 서버 쪽은 choice 서식으로 고쳤는데 배당 내역 스크립트는 문구를 replace('{0}') 로
// 채워 choice 꼴을 그대로 찍게 된다 - 그 자리의 고르기 함수다. 빌드 산출물에서 함수를 꺼내 부른다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

// 2026-10-01 저녁: 고르기 함수를 common.js 로 옮겼다 - 활동 · 자산 현황 · 시뮬레이터도 같은 것을 쓴다.
const source = readFileSync(join(JTE_ROOT, "../resources/static/js/common.js"), "utf8");
const sandbox = {};
vm.createContext(sandbox);
vm.runInContext(extractFunction(source, "applyCountChoice"), sandbox);
const pick = (pattern, n) => sandbox.applyCountChoice(pattern, n).replace("{0}", String(n));

const EN = "{0,choice,0#{0} items|1#{0} item|1<{0} items}";

test("1 은 단수, 0 · 2 이상 · 소수는 복수 - MessageFormat 과 같다", () => {
	assert.equal(pick(EN, 1), "1 item");
	assert.equal(pick(EN, 0), "0 items");
	assert.equal(pick(EN, 2), "2 items");
	assert.equal(pick(EN, 211), "211 items");
	assert.equal(pick(EN, 1.5), "1.5 items");
});

test("choice 가 아닌 문구(한국어 {0}건)는 그대로", () => {
	assert.equal(pick("{0}건", 1), "1건");
	assert.equal(pick("{0}건", 3), "3건");
});

test("앞뒤 글자는 남긴다", () => {
	assert.equal(pick("Total {0,choice,0#{0} items|1#{0} item|1<{0} items} so far", 1), "Total 1 item so far");
});

test("모르는 꼴(1# 이나 1< 가 없음)은 건드리지 않는다", () => {
	assert.equal(sandbox.applyCountChoice("{0,choice,0#none|2#{0} pairs}", 1), "{0,choice,0#none|2#{0} pairs}");
});
