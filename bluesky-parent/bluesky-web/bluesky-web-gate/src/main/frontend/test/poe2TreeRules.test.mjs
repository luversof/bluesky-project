// PoE2 트리 규칙(10-02) — 빌드된 tree2.js 에서 순수 함수를 이름으로 꺼내 시험한다(PoB-PoE2 규칙과 같아야 한다).
//   · 소형 패시브 효과(타이탄 육중한 형체): 대상 = 일반 노드(전직 아님), 정수는 소수 버림(m_modf(round(v×배율, 2))), 소수는 0.01 자리 버림
//   · 뒤얽힌 현실(오라클): 핵심 노드 반경 안 주요 · 일반 · 능력치 노드(전직 아님)
//   · 소서리스의 길(패스파인더): "from the X's starting point" 문장 → X
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const BUILT = "../resources/static/js/poe2/tree2.js";
const NAMES = ["scaleLine", "isSmallPassiveNode", "smallPassiveIncOf", "inLeapRadius", "altStartClassNames", "pointSummary", "inFromNothingRadius", "inLeapRing", "unlockMet", "choiceSiblingsOf"];

function load() {
	const source = readFileSync(join(JTE_ROOT, BUILT), "utf8");
	const sandbox = { Math, Number, String, parseFloat };
	vm.createContext(sandbox);
	for (const name of NAMES) vm.runInContext(extractFunction(source, name), sandbox);
	return sandbox;
}

test("소형 패시브 배율 — 정수는 소수 버림, 소수는 0.01 자리 버림", () => {
	const t = load();
	assert.equal(t.scaleLine("+20 to Armour", 1.5), "+30 to Armour");
	assert.equal(t.scaleLine("+5 to maximum Life", 1.5), "+7 to maximum Life"); // 7.5 → 7
	assert.equal(t.scaleLine("10% increased Melee Damage", 1.5), "15% increased Melee Damage");
	assert.equal(t.scaleLine("Regenerate 0.2% of maximum Life per second", 1.5), "Regenerate 0.3% of maximum Life per second");
	assert.equal(t.scaleLine("Adds 3 to 7 Fire Damage", 1.5), "Adds 4 to 10 Fire Damage"); // 4.5 → 4, 10.5 → 10
	assert.equal(t.scaleLine("+20 to Armour", 1), "+20 to Armour");
});

test("소형 패시브 대상 — 일반 노드만, 능력치 · 주요 · 핵심 · 전직은 아님", () => {
	const t = load();
	assert.equal(t.isSmallPassiveNode({ kind: "normal", ascendancy: null }), true);
	assert.equal(t.isSmallPassiveNode({ kind: "attribute", ascendancy: null }), false);
	assert.equal(t.isSmallPassiveNode({ kind: "notable", ascendancy: null }), false);
	assert.equal(t.isSmallPassiveNode({ kind: "keystone", ascendancy: null }), false);
	assert.equal(t.isSmallPassiveNode({ kind: "normal", ascendancy: "Titan" }), false);
});

test("소형 패시브 효과 합 — 여러 문장 · 다른 문장 섞여도", () => {
	const t = load();
	assert.equal(t.smallPassiveIncOf(["50% increased effect of Small Passive Skills"]), 50);
	assert.equal(t.smallPassiveIncOf(["8% increased Strength", "25% increased effect of Small Passive Skills", "25% increased effect of Small Passive Skills"]), 50);
	assert.equal(t.smallPassiveIncOf(["30% increased effect of Notable Passive Skills"]), 0);
	assert.equal(t.smallPassiveIncOf([]), 0);
});

test("뒤얽힌 현실 반경 — 경계 포함, 밖은 아님, 대상 종류만", () => {
	const t = load();
	const keys = [{ x: 0, y: 0 }];
	assert.equal(t.inLeapRadius({ kind: "normal", ascendancy: null, x: 1150, y: 0 }, keys, 1150), true); // 경계
	assert.equal(t.inLeapRadius({ kind: "normal", ascendancy: null, x: 1151, y: 0 }, keys, 1150), false);
	assert.equal(t.inLeapRadius({ kind: "notable", ascendancy: null, x: 600, y: 600 }, keys, 1150), true);
	assert.equal(t.inLeapRadius({ kind: "attribute", ascendancy: null, x: 10, y: 0 }, keys, 1150), true);
	assert.equal(t.inLeapRadius({ kind: "keystone", ascendancy: null, x: 10, y: 0 }, keys, 1150), false);
	assert.equal(t.inLeapRadius({ kind: "jewel", ascendancy: null, x: 10, y: 0 }, keys, 1150), false);
	assert.equal(t.inLeapRadius({ kind: "normal", ascendancy: "Oracle", x: 10, y: 0 }, keys, 1150), false);
	assert.equal(t.inLeapRadius({ kind: "normal", ascendancy: null, x: 10, y: 0 }, [], 1150), false); // 핵심 노드 없음
});

test("다른 직업 시작점 문장 — Passive Skills / Passives 둘 다", () => {
	const t = load();
	assert.deepEqual([...t.altStartClassNames(["Can Allocate Passive Skills from the Sorceress's starting point"])], ["Sorceress"]);
	assert.deepEqual([...t.altStartClassNames(["Can Allocate Passives from the Ranger's starting point"])], ["Ranger"]);
	assert.deepEqual([...t.altStartClassNames(["+5 to Strength"])], []);
});

test("포인트 한도 — PoB 규칙: 일반 = 공용 + 큰 쪽 세트, 123 · 세트 24 · 전직 8 (10-03 C38)", () => {
	const t = load();
	const a = t.pointSummary(91, 24, 24, 8);
	assert.equal(a.normal, 115); // 실빌드: 공용 91 + 세트 24(양쪽 같으면 한 번만)
	assert.equal(a.normalMax, 123);
	assert.equal(a.setMax, 24);
	assert.equal(a.ascMax, 8);
	assert.equal(t.pointSummary(100, 3, 10, 0).normal, 110); // 큰 쪽(세트 II 10)만 일반 포인트를 쓴다
	assert.equal(t.pointSummary(5, 0, 0, 2).normal, 5);
	// 추가 포인트 — 패스파인더 "Grants 1 Passive Skill Point" ×2 + "Grants 4 Passive Skill Point" → 한도 129, 위치헌터 무기 달인 → 세트 한도 124
	assert.equal(t.pointSummary(0, 0, 0, 0, ["Grants 1 Passive Skill Point", "Grants 1 Passive Skill Point", "Grants 4 Passive Skill Point"]).normalMax, 129);
	assert.equal(t.pointSummary(0, 0, 0, 0, ["100 Passive Skill Points become Weapon Set Skill Points"]).setMax, 124);
	assert.equal(t.pointSummary(0, 0, 0, 0, ["10% increased Damage"]).normalMax, 123);
	// 엔드게임 포인트(C49) — 빌드 코드에 없는 최대 2 는 넘침이 아니라 따로, 3 부터 빨강
	const e1 = t.pointSummary(100, 24, 0, 8);
	assert.deepEqual([e1.normal, e1.endgame, e1.normalOver], [124, 1, false]);
	assert.deepEqual([t.pointSummary(101, 24, 0, 8).endgame, t.pointSummary(101, 24, 0, 8).normalOver], [2, false]);
	assert.deepEqual([t.pointSummary(102, 24, 0, 8).endgame, t.pointSummary(102, 24, 0, 8).normalOver], [2, true]);
	assert.deepEqual([t.pointSummary(99, 24, 0, 8).endgame, t.pointSummary(99, 24, 0, 8).normalOver], [0, false]);
	// 무기 세트는 올로스 은혜 1 만(세트 한도 24 + 1 까지는 빨강 아님 — 화면 add 의 allow)
	assert.deepEqual([e1.normalEndgame, e1.setEndgame], [2, 1]);
	// 전직 추가 포인트가 먼저 한도를 올린다(패스파인더 +6 이면 130 까지는 엔드게임 0)
	assert.equal(t.pointSummary(106, 24, 0, 8, ["Grants 4 Passive Skill Point", "Grants 1 Passive Skill Point", "Grants 1 Passive Skill Point"]).endgame, 1);
});

test("From Nothing — 핵심 노드 반경 안(핵심 자신 · 주얼 칸 · 직업 시작 · 전직 제외), 핵심 노드는 찍지 않아도 (10-03 C40)", () => {
	const t = load();
	const key = { id: 1, x: 0, y: 0 };
	const r = 1300 * 1.2; // Large × PassiveTreeJewelDistanceMultiplier
	assert.equal(t.inFromNothingRadius({ id: 2, kind: "notable", ascendancy: null, x: 1560, y: 0 }, key, r), true); // 경계
	assert.equal(t.inFromNothingRadius({ id: 3, kind: "notable", ascendancy: null, x: 1561, y: 0 }, key, r), false);
	assert.equal(t.inFromNothingRadius({ id: 4, kind: "keystone", ascendancy: null, x: 10, y: 0 }, key, r), true); // 다른 핵심은 됨
	assert.equal(t.inFromNothingRadius({ id: 1, kind: "keystone", ascendancy: null, x: 0, y: 0 }, key, r), false); // 자신
	assert.equal(t.inFromNothingRadius({ id: 5, kind: "jewel", ascendancy: null, x: 10, y: 0 }, key, r), false);
	assert.equal(t.inFromNothingRadius({ id: 6, kind: "classStart", ascendancy: null, x: 10, y: 0 }, key, r), false);
	assert.equal(t.inFromNothingRadius({ id: 7, kind: "normal", ascendancy: "Deadeye", x: 10, y: 0 }, key, r), false);
});

test("주얼 칸 둘레 고리(Controlled Metamorphosis Massive Ring 2160~2520) — 안 · 바깥 경계, 칸 · 시작 · 전직 제외 (10-03 C47)", () => {
	const t = load();
	const socket = { id: 9, x: 0, y: 0 };
	assert.equal(t.inLeapRing({ id: 1, kind: "notable", ascendancy: null, x: 2160, y: 0 }, socket, 2160, 2520), true);
	assert.equal(t.inLeapRing({ id: 2, kind: "notable", ascendancy: null, x: 2520, y: 0 }, socket, 2160, 2520), true);
	assert.equal(t.inLeapRing({ id: 3, kind: "normal", ascendancy: null, x: 2159, y: 0 }, socket, 2160, 2520), false);
	assert.equal(t.inLeapRing({ id: 4, kind: "normal", ascendancy: null, x: 2521, y: 0 }, socket, 2160, 2520), false);
	assert.equal(t.inLeapRing({ id: 5, kind: "jewel", ascendancy: null, x: 2300, y: 0 }, socket, 2160, 2520), false);
	assert.equal(t.inLeapRing({ id: 6, kind: "normal", ascendancy: "Lich", x: 2300, y: 0 }, socket, 2160, 2520), false);
});

test("잠금 조건 — 조건 노드가 모두 찍혀야(스피릿 워커 Sacred Unity, 10-03 C50)", () => {
	const t = load();
	const sacred = { unlockConstraint: [41401, 62743, 46070] };
	assert.equal(t.unlockMet(sacred, (id) => [41401, 62743, 46070].includes(id)), true);
	assert.equal(t.unlockMet(sacred, (id) => [41401, 62743].includes(id)), false);
	assert.equal(t.unlockMet({}, () => false), true); // 조건 없는 노드
});

test("갈래 선택지 — 같은 부모(선택지 이웃 둘 이상)의 다른 선택지만 (10-03 C65)", () => {
	const t = load();
	// 부모 10 — 선택지 21 · 22, 21 아래 자식 30(선택지 아님), 다른 갈래 부모 40 — 선택지 41 하나뿐
	const adj = { 10: [21, 22], 21: [10, 30], 22: [10], 30: [21], 40: [41], 41: [40] };
	const isOpt = (id) => [21, 22, 41].includes(id);
	const j = (a) => JSON.stringify([...a].sort());
	assert.equal(j(t.choiceSiblingsOf(21, (id) => adj[id] || [], isOpt)), "[22]"); // 자식 30 쪽은 부모가 아님(선택지 이웃 1개)
	assert.equal(j(t.choiceSiblingsOf(41, (id) => adj[id] || [], isOpt)), "[]"); // 형제 없는 갈래
	assert.equal(j(t.choiceSiblingsOf(10, (id) => adj[id] || [], isOpt)), "[]"); // 선택지가 아님
});
