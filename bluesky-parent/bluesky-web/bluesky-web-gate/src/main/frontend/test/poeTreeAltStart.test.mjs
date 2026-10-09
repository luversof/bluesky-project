// PoE1 트리 "다른 직업 시작점"(10-02) — 어센던트 "Path of the X" 문장 → 직업 id. 빌드된 tree.js 에서 순수 함수를 이름으로 꺼내 시험한다.
//   id 는 GGG 직업 번호(사이온 0 · 머라우더 1 · 레인저 2 · 위치 3 · 듀얼리스트 4 · 템플러 5 · 섀도우 6) = classStartByClassId 키.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const BUILT = "../resources/static/js/poe/tree.js";

function load() {
	const source = readFileSync(join(JTE_ROOT, BUILT), "utf8");
	const sandbox = {};
	vm.createContext(sandbox);
	vm.runInContext(extractFunction(source, "altStartClassIds"), sandbox);
	return sandbox;
}

test("어센던트 Path of the X — 직업 id", () => {
	const t = load();
	const ids = (lines) => [...t.altStartClassIds(lines)];
	assert.deepEqual(ids(["Can Allocate Passives from the Ranger's starting point"]), [2]);
	assert.deepEqual(ids(["Can Allocate Passives from the Marauder's starting point"]), [1]);
	assert.deepEqual(ids(["Can Allocate Passives from the Shadow's starting point"]), [6]);
	assert.deepEqual(ids(["Can Allocate Passive Skills from the Witch's starting point"]), [3]); // PoE2 식 문구도
	assert.deepEqual(ids(["Can Allocate Passives from the Ranger's starting point", "Can Allocate Passives from the Ranger's starting point"]), [2]); // 중복 한 번
	assert.deepEqual(ids(["Can Allocate Passives from the Sorceress's starting point"]), []); // PoE1 에 없는 직업
	assert.deepEqual(ids(["+10 to Dexterity"]), []);
});
