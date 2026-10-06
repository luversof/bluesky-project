// 트리 검색 비교 열쇠(10-02 C24) — PoE1 tree.js · PoE2 tree2.js 의 searchKey 가 같은 규칙인가:
//   소문자 + 띄어쓰기(공백 · 탭 · NBSP) 무시, 줄바꿈은 남겨 이름 · 스탯 줄이 서로 붙어 거짓으로 맞지 않게.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

function load(built) {
	const source = readFileSync(join(JTE_ROOT, built), "utf8");
	const sandbox = {};
	vm.createContext(sandbox);
	vm.runInContext(extractFunction(source, "searchKey"), sandbox);
	return sandbox.searchKey;
}

for (const [game, built] of [["PoE1", "../resources/static/js/poe/tree.js"], ["PoE2", "../resources/static/js/poe2/tree2.js"]]) {
	test(`${game} 트리 검색 — 띄어쓰기 · 대소문자 무시, 줄 경계는 유지`, () => {
		const key = load(built);
		assert.equal(key("피의 마법"), key("피의마법"));
		assert.equal(key("Blood  Magic"), "bloodmagic");
		assert.equal(key("a b\tc"), "abc");
		// 이름과 다음 스탯 줄 사이 줄바꿈은 남는다 — "마법" 끝 + "생명력" 처음이 "마법생명력" 으로 붙지 않는다
		const hay = key(["피의 마법", "생명력 최대치 +10"].join("\n"));
		assert.ok(hay.includes(key("생명력최대치")));
		assert.ok(!hay.includes(key("마법생명력")));
	});
}
