// PoE1 트리 렐리쿼리언 진열장 선택지(10-03 C60) — 같은 진열장(선택지의 이웃)의 다른 선택지를 찾는다. 고르면 그것들과 바꾼다(PoB isMultipleChoice).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

function load() {
	const source = readFileSync(join(JTE_ROOT, "../resources/static/js/poe/tree.js"), "utf8");
	const sandbox = {};
	vm.createContext(sandbox);
	vm.runInContext(extractFunction(source, "choiceSiblings"), sandbox);
	return sandbox.choiceSiblings;
}

test("PoE1 진열장 선택지 — 같은 진열장의 다른 선택지만", () => {
	const sib = load();
	// 진열장 20 — 선택지 21 · 22 · 23, 진열장은 경로 노드 10 과도 이어진다(10 은 선택지가 아님)
	const adj = new Map([[10, [20]], [20, [10, 21, 22, 23]], [21, [20]], [22, [20]], [23, [20]]]);
	const isOption = (id) => [21, 22, 23].includes(id);
	// vm 컨텍스트 배열은 프로토타입이 달라 deepEqual 이 실패한다 — 글자로 비교
	const j = (a) => JSON.stringify([...a].sort());
	assert.equal(j(sib(21, adj, isOption)), "[22,23]");
	assert.equal(j(sib(10, adj, isOption)), "[]"); // 선택지가 아니면 없음
	assert.equal(j(sib(20, adj, isOption)), "[]"); // 진열장 자신도 선택지가 아님
});
