// PoE1 트리 추가 패시브 포인트(10-03 C51) — PoB ModParser "grants N passive skill points" → ExtraPoints, 한도 = 기본 + 추가.
//   어센던트 · 렐리쿼리언 "Passive Point"(+1) · 어센던트 "Path of the X"(+2). 없으면 실빌드가 "포인트 127 / 123" 빨강.
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
	vm.runInContext(extractFunction(source, "extraPassivePoints"), sandbox);
	return sandbox.extraPassivePoints;
}

test("PoE1 추가 포인트 — Grants 1 / 2 Passive Skill Point(s) 합, 다른 줄은 0", () => {
	const extra = load();
	assert.equal(extra(["Grants 1 Passive Skill Point", "Grants 1 Passive Skill Point"]), 2);
	assert.equal(extra(["Can Allocate Passives from the Ranger's starting point", "Grants 2 Passive Skill Points"]), 2);
	assert.equal(extra(["+10 to Strength", "10% increased maximum Life"]), 0);
	// 아틀라스 포인트 문장 등 비슷한 꼴은 세지 않는다(줄 전체가 맞아야)
	assert.equal(extra(["Grants 1 Passive Skill Point to Minions"]), 0);
	assert.equal(extra([]), 0);
});
