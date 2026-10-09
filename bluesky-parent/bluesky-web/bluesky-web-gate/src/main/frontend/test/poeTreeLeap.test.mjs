// PoE1 트리 연결 없이 찍기(10-03 C64) — Impossible Escape(핵심 반경) · Thread of Hope(주얼 칸 고리) · Intuitive Leap(주얼 칸 원) 자리 판정.
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
	vm.runInContext(extractFunction(source, "inLeapArea"), sandbox);
	return sandbox.inLeapArea;
}

test("PoE1 연결 없이 찍기 자리 — 원 · 고리 경계, 제외 종류", () => {
	const inArea = load();
	const c = { id: 1, x: 0, y: 0 };
	const n = (id, x, type = "normal", extra = {}) => ({ id, type, ascendancy: null, x, y: 0, ...extra });
	assert.equal(inArea(n(2, 960), c, 0, 960), true); // 원 경계 포함
	assert.equal(inArea(n(3, 961), c, 0, 960), false);
	assert.equal(inArea(n(4, 1000), c, 960, 1320), true); // 고리 안
	assert.equal(inArea(n(5, 900), c, 960, 1320), false); // 고리 안쪽 빈 곳
	assert.equal(inArea(n(1, 0), c, 0, 960), false); // 중심 자신
	assert.equal(inArea(n(6, 10, "jewel"), c, 0, 960), false);
	assert.equal(inArea(n(7, 10, "mastery"), c, 0, 960), false);
	assert.equal(inArea(n(8, 10, "class"), c, 0, 960), false);
	assert.equal(inArea(n(9, 10, "notable", { ascendancy: "Slayer" }), c, 0, 960), false);
	assert.equal(inArea(n(70000, 10), c, 0, 960), false); // 클러스터 생성 노드
});
