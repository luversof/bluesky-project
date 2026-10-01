// PoE2 패시브 트리(PoB-PoE2 src/TreeData/0_5/tree.json) → 뷰어용 경량 JSON (한국어 이름·스탯 포함).
// 사용법: node parse-tree2.mjs   → ~/.poe-gamedata/poe2/passive-tree.json
//
// 입력
//   - 트리: PoB-PoE2 dev 브랜치 tree.json (work/pob/tree-0_5.json, 없으면 받는다). 영어 전용.
//   - 한국어: PoE2 게임 테이블 PassiveSkills(영/한 같은 행 순서) — PassiveSkillGraphId == PoB 노드 skill 로 조인.
//   - 스탯 문장: PoE2 스탯 서술 파일(.csd, UTF-16LE, PoE1 과 같은 DSL)을 PoE1 서술기로 읽는다.
//
// 노드 종류(kind) — PoB PassiveTree.lua 의 판정 순서를 그대로 따른다:
//   classesStart         → classStart       (직업 시작점, 6개 — PoE1/PoE2 직업이 한 자리를 나눠 쓴다)
//   isAscendancyStart    → ascendancyStart
//   isOnlyImage          → mastery          (PoE2 의 "~ Mastery" 는 할당 불가 배경 그림 노드뿐. PoB 도 연결선을 안 그린다)
//   isJewelSocket        → jewel
//   isKeystone           → keystone
//   isNotable            → notable
//   isAttribute          → attribute        (+5 힘/민첩/지능 중 택1 — options 로 선택지를 싣는다)
//   그 외                 → normal
//
// 좌표: group(x,y) + orbitRadii[orbit] 에 각도 constants.orbitAnglesByOrbit[orbit][orbitIndex]
//   (없으면 2π·index/skillsPerOrbit). GGG 관례 x = gx + r·sin(a), y = gy − r·cos(a) (PoB ProcessNode 와 동일).
//
// 간선(out): PoB connections 는 이미 한쪽 노드에만 적혀 있다(5832개 중 양방향 1쌍). 무향 키로 한 번만 남기고
//   원래 적힌 쪽의 orbit 값을 보존한다(곡선 호: orbit≠0 이면 |orbit| 궤도 반지름의 호, 부호는 휘는 방향 —
//   PoB BuildConnector). 2147483647 은 "호 아님" 표지로 PoB 가 직선으로 그리므로 값 그대로 둔다.
//   PoB 가 연결하지 않는 것은 뺀다: 없는 노드/그룹 없는 노드로의 연결, 자기 자신, mastery(OnlyImage) 노드와의 연결.
//   직업 시작점·다른 전직 사이 연결은 PoB 가 선은 안 그리지만 할당 경로(linkedId)로는 쓰므로 남긴다.
//
// 출력 부가 필드(명세 외, 뷰어에 필요한 최소분):
//   groups{번호:{x,y}}  — 같은 그룹·같은 궤도 호(connection orbit 0)의 중심. 번호는 node.group 과 같은 PoB 1-base 값.
//   classes[].integerId — GGG/PoB 직업 번호(빌드 코드용). index 는 PoB classes 배열 순서.
//   classes[].ascendancies[].startNodeId, 노드 options(능력치 택1), flavour/flavourKo(키스톤 문구).
//
// 리마인더: PoE2 게임 테이블 ReminderText 는 DNT 자리표시 2행뿐이고 PassiveSkills.ReminderStrings 도 전부 비어 있어
//   (PoB 트리에도 reminderText 가 없음) reminders 는 현재 비어 있다 — 데이터가 생기면 채우도록 코드만 둔다.
import fs from "node:fs";
import path from "node:path";
import { FILES_DIR, WORK_DIR, loadConfig, loadTable, writeJson } from "./paths.mjs";
import { createStatDescriber, reportUnknownHandlers } from "../poe-extract/statDescriptions.mjs";

const TREE_VERSION = "0_5";
const TREE_PATH = path.join(WORK_DIR, "pob", `tree-${TREE_VERSION}.json`);
const TREE_URL = `https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/dev/src/TreeData/${TREE_VERSION}/tree.json`;

if (!fs.existsSync(TREE_PATH)) {
	console.log("다운로드:", TREE_URL);
	const response = await fetch(TREE_URL);
	if (!response.ok) throw new Error(`트리 다운로드 실패: ${response.status}`);
	fs.mkdirSync(path.dirname(TREE_PATH), { recursive: true });
	fs.writeFileSync(TREE_PATH, await response.text());
}
const tree = JSON.parse(fs.readFileSync(TREE_PATH, "utf8"));
const { patch } = loadConfig();

// ---- 텍스트 유틸 ----
// PoE2 서술 텍스트 마크업: [Critical|Critical Hit] → "Critical Hit", [Spirit] → "Spirit"
export const stripMarkup = (s) =>
	s.replace(/\[([^\]|]*)\|([^\]]*)\]/g, "$2").replace(/\[([^\]]*)\]/g, "$1");
// 비교용 정규화: 마크업·공백·대소문자, 그리고 게임이 붙이는 줄머리 불릿("•Red: …" — PoB 는 뺀다)
const norm = (s) => stripMarkup(s).replace(/^\s*•\s*/, "").replace(/\s+/g, " ").trim().toLowerCase();
const HANGUL = /[가-힣]/;

/**
 * PoB 줄(게임 표기 순서)에 게임 서술 줄(영/한 평행)을 맞춘다.
 * PoB 는 긴 문장을 툴팁 폭으로 **여러 줄로 쪼개** 싣기도 해서(예: "…dealing a" / "tenth of their…"),
 * PoB 연속 줄 1~6개를 이어 붙인 것이 게임 영문 한 줄과 같으면 그 묶음을 한국어 한 줄로 바꾼다.
 * full = PoB 줄이 전부 게임 줄로 소비되고 게임 줄도 남지 않음(= 영문 완전 일치). 매치율 카운터와 자가검사가 이걸 쓴다.
 * 못 맞춘 PoB 줄 자리는 남은 한국어 줄로 순서대로 채우고(수치 표기만 다른 경우), 그마저 없으면 PoB 영문을 둔다
 * (효과가 한국어 툴팁에서 사라지지 않게 — PoE1 tree-common alignKoToGameOrder 와 같은 원칙).
 */
export function alignLines(pobLines, gameEn, gameKo, { fillHoles = true } = {}) {
	const pool = gameEn.map((line, i) => ({ key: norm(line), ko: gameKo[i], used: false }));
	const out = [];
	const holes = [];
	let matchedPob = 0;
	for (let i = 0; i < pobLines.length; ) {
		let hit = null;
		let width = 1;
		for (let w = 1; w <= Math.min(6, pobLines.length - i) && !hit; w++) {
			const key = norm(pobLines.slice(i, i + w).join(" "));
			hit = pool.find((p) => !p.used && p.key === key) || null;
			width = w;
		}
		if (hit) {
			hit.used = true;
			out.push(hit.ko);
			matchedPob += width;
			i += width;
		} else {
			holes.push(out.length);
			out.push(pobLines[i]);
			i++;
		}
	}
	const leftover = pool.filter((p) => !p.used).map((p) => p.ko);
	if (fillHoles) {
		holes.forEach((slot, k) => {
			if (k < leftover.length) out[slot] = leftover[k];
		});
		for (let k = holes.length; k < leftover.length; k++) out.push(leftover[k]);
	}
	return { lines: out, matchedPob, full: matchedPob === pobLines.length && pool.every((p) => p.used) };
}
export const linesMatch = (gameLines, pobLines) => alignLines(pobLines, gameLines, gameLines).full;

// ---- 스탯 서술 파일 전처리 ----
// PoE1 서술기(statDescriptions.mjs)는 PoE1 DSL 기준이라 PoE2 파일을 그대로 먹이면 두 군데서 틀린다.
// 서술기는 건드리지 않고, 읽히기 전에 사본(work/statdesc-clean)을 고쳐 넘긴다.
//  (a) `table_only` 변형: 거래소 표 전용 축약문("Critical Damage Bonus vs full life enemies@{0}%")이 조건 "#" 로
//      맨 앞에 있어 서술기가 그것을 고른다 → 줄을 지우고 변형 개수를 1 줄인다.
//  (b) `per_minute_to_per_second`: PoE1 서술기는 정수로 반올림(150/60 → 3)하지만 PoE2 표기는 2.5%(PoB 도 2.5)
//      → 소수 유지 핸들러 per_minute_to_per_second_2dp_if_required 로 바꾼다(정수값은 그대로 정수).
// 파일 순서: passive_skill_stat_descriptions.csd 가 `include stat_descriptions.csd` 하는 체인이 패시브의 실제 서술 경로다.
//  gem/skill 계열까지 넣으면 뒤 파일이 이기는 구조라 보조 젬 문구("Supported Skills have …")가 패시브 스탯을 덮어
//  영문 일치율이 86.7% 로 떨어진다(stat+passive 만: 98.3%, 2026-09-30 실측). 그래서 이 둘만 쓴다.
const CLEAN_DIR = path.join(WORK_DIR, "statdesc-clean");
const DESC_FILES = ["data@statdescriptions@stat_descriptions.csd", "data@statdescriptions@passive_skill_stat_descriptions.csd"];
const cleanStats = { tableOnly: 0, perMinute: 0 };
fs.mkdirSync(CLEAN_DIR, { recursive: true });
for (const name of DESC_FILES) {
	let text = fs.readFileSync(path.join(FILES_DIR, name)).toString("utf16le");
	if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
	const lines = text.split(/\r?\n/);
	const out = [];
	let countIdx = -1;
	for (const line of lines) {
		if (/^\s*\d+\s*$/.test(line)) countIdx = out.length;
		if (/^\s+[^"]*\btable_only\s+"/.test(line) && countIdx >= 0) {
			out[countIdx] = out[countIdx].replace(/\d+/, (n) => String(Number(n) - 1));
			cleanStats.tableOnly++;
			continue;
		}
		out.push(
			line.replace(/("\s.*?)\bper_minute_to_per_second(?=\s+\d)/g, (m, pre) => {
				cleanStats.perMinute++;
				return pre + "per_minute_to_per_second_2dp_if_required";
			}),
		);
	}
	fs.writeFileSync(path.join(CLEAN_DIR, name), Buffer.from("﻿" + out.join("\r\n"), "utf16le"));
}
const describe = createStatDescriber(CLEAN_DIR, DESC_FILES);

// ---- 게임 테이블 조인 ----
const statsTable = loadTable("English", "Stats");
const passivesEn = loadTable("English", "PassiveSkills");
const passivesKo = loadTable("Korean", "PassiveSkills");
const clientEn = loadTable("English", "ClientStrings");
const clientKo = loadTable("Korean", "ClientStrings");
let reminderEn = [];
let reminderKo = [];
try {
	reminderEn = loadTable("English", "ReminderText");
	reminderKo = loadTable("Korean", "ReminderText");
} catch {
	/* 테이블 없음 → 리마인더 생략 */
}
const usableText = (s) => s && !/^\[DNT/.test(s) && s !== "WIP";

// 스탯이 아니라 PassiveSkills 열로 붙는 줄(PoB 는 문장으로 싣는다): 부여 스킬 / 패시브 포인트 / 무기 세트 포인트.
// 문구는 게임 ClientStrings 에서 영/한 쌍으로 가져온다("Grants Skill: <underline>{{{0}}}" → "Grants Skill: {0}").
const clientString = (id) => {
	const i = clientEn.findIndex((r) => r.Id === id);
	if (i < 0) return null;
	const clean = (t) => stripMarkup(t.replace(/<[^>]+>/g, "").replace(/\{\{\{(\d)\}\}\}/g, "{$1}"));
	return { en: clean(clientEn[i].Text), ko: clean(clientKo[i].Text) };
};
const fill = (tpl, v) => tpl.replace(/\{0\}/g, v);
const T = {
	grantsSkill: clientString("ItemDisplayGrantedSkillNoScaling"),
	point: clientString("PassiveNodeGrantsPassivePoint"),
	points: clientString("PassiveNodeGrantsPassivePoints"),
	weaponPoint: clientString("PassiveNodeGrantsSpecialisationPoint"),
	weaponPoints: clientString("PassiveNodeGrantsSpecialisationPoints"),
};
const skillGems = loadTable("English", "SkillGems");
const baseEn = loadTable("English", "BaseItemTypes");
const baseKo = loadTable("Korean", "BaseItemTypes");
function extraLines(row) {
	const en = [];
	const ko = [];
	if (row.GrantedSkill != null && T.grantsSkill) {
		// PassiveSkills.GrantedSkill → SkillGems 행 → BaseItemTypes 이름(PoB 표기와 같은 이름. ActiveSkills 는 "Command: {0}" 같은 틀이 섞임)
		const b = skillGems[row.GrantedSkill]?.BaseItemType;
		if (b != null && baseEn[b]?.Name) {
			en.push(fill(T.grantsSkill.en, baseEn[b].Name));
			ko.push(fill(T.grantsSkill.ko, usableText(baseKo[b]?.Name) ? baseKo[b].Name : baseEn[b].Name));
		}
	}
	const pts = row.SkillPointsGranted || 0;
	const tp = pts === 1 ? T.point : T.points;
	if (pts && tp) {
		en.push(fill(tp.en, pts));
		ko.push(fill(tp.ko, pts));
	}
	const wp = row.WeaponPointsGranted || 0;
	const tw = wp === 1 ? T.weaponPoint : T.weaponPoints;
	if (wp && tw) {
		en.push(fill(tw.en, wp));
		ko.push(fill(tw.ko, wp));
	}
	return { en, ko };
}

/** 서술 결과(설명 블록 단위 배열)를 줄 단위 영/한 평행 배열로 편다. 블록 안 \n 은 PoB 처럼 줄로 나눈다. */
function describeLines(statValues) {
	const en = describe(statValues, "English").map(stripMarkup);
	const ko = describe(statValues, "Korean").map(stripMarkup);
	const enLines = [];
	const koLines = [];
	en.forEach((block, i) => {
		const e = block.split("\n");
		const k = (ko[i] ?? "").split("\n");
		if (en.length === ko.length && e.length === k.length) {
			enLines.push(...e);
			koLines.push(...k);
		} else {
			// 줄 수가 언어마다 다르면 블록을 한 줄로 합쳐 평행을 유지
			enLines.push(e.join(" "));
			koLines.push(k.join(" "));
		}
	});
	return { enLines, koLines };
}

const gameByGraphId = new Map();
passivesEn.forEach((row, i) => {
	if (row.PassiveSkillGraphId == null) return;
	const statValues = new Map();
	// 값 열은 config 가 Stat1Value..Stat5Value 만 뽑는다. 스탯이 6개 이상인 행(트리엔 "Way of the Mountain" 1개)은
	// 뒤 스탯 값을 모르므로 서술에서 빼고(=0 취급과 같음) valuesMissing 으로 표시 — 그 노드는 못 맞춘 줄을 한글로 메우지 않는다.
	let valuesMissing = false;
	(row.Stats || []).forEach((statIndex, pos) => {
		const key = "Stat" + (pos + 1) + "Value";
		if (!(key in row)) {
			valuesMissing = true;
			return;
		}
		statValues.set(statsTable[statIndex].Id, row[key] ?? 0);
	});
	const ko = passivesKo[i];
	const reminders = (row.ReminderStrings || [])
		.map((r) => ({ en: reminderEn[r]?.Text, ko: reminderKo[r]?.Text }))
		.filter((r) => usableText(r.en));
	const described = describeLines(statValues);
	const extra = extraLines(row);
	gameByGraphId.set(row.PassiveSkillGraphId, {
		nameEn: stripMarkup(row.Name || ""),
		nameKo: usableText(ko?.Name) ? stripMarkup(ko.Name) : null,
		flavourKo: usableText(ko?.FlavourText) ? stripMarkup(ko.FlavourText) : null,
		valuesMissing,
		reminders,
		enLines: [...described.enLines, ...extra.en],
		koLines: [...described.koLines, ...extra.ko],
	});
});

// ---- 기하 ----
const { orbitRadii, skillsPerOrbit, orbitAnglesByOrbit } = tree.constants;
// 주의: PoB-PoE2 tree.json 의 groups 는 **JSON 배열**이고 node.group 은 Lua 식 **1-base** 번호다.
// groups[node.group] 로 읽으면 전 노드가 옆 그룹 좌표로 가서(실측: 직업 시작점이 원점에서 1.4k~11.6k 로 흩어짐)
// group.nodes 소속과 0/4914 일치, groups[node.group - 1] 은 4914/4914 일치. 출력의 group 번호는 PoB 값(1-base) 그대로 쓴다.
const groupOf = (groupNo) => tree.groups[groupNo - 1] || null;
function orbitAngle(orbit, index) {
	const table = orbitAnglesByOrbit?.[orbit];
	if (table && table[index] != null) return table[index];
	return (2 * Math.PI * index) / skillsPerOrbit[orbit];
}
function position(node) {
	const g = groupOf(node.group);
	const a = orbitAngle(node.orbit, node.orbitIndex);
	const r = orbitRadii[node.orbit];
	return { x: g.x + r * Math.sin(a), y: g.y - r * Math.cos(a), angle: a };
}

function kindOf(n) {
	if (n.classesStart) return "classStart";
	if (n.isAscendancyStart) return "ascendancyStart";
	if (n.isOnlyImage) return "mastery";
	if (n.isJewelSocket) return "jewel";
	if (n.isKeystone) return "keystone";
	if (n.isNotable) return "notable";
	if (n.isAttribute) return "attribute";
	return "normal";
}

// ---- 직업/전직 ----
// 한국어: Characters(IntegerId == PoB integerId), Ascendancy(Id == PoB internalId — "Ranger1" 등).
// PoE2 Ascendancy 테이블엔 "[DNT-UNUSED]"/"WIP" 자리표시 행이 섞여 있어 이름이 아니라 internalId 로 잇고,
// 영문 이름이 PoB 와 같은지 교차 확인한다(불일치면 PoB 이름으로 한 번 더 찾는다).
const charsEn = loadTable("English", "Characters");
const charsKo = loadTable("Korean", "Characters");
const ascEn = loadTable("English", "Ascendancy");
const ascKo = loadTable("Korean", "Ascendancy");
const pobClassNames = new Set(tree.classes.map((c) => c.name));
const startNodeByClass = new Map();
for (const [id, n] of Object.entries(tree.nodes)) {
	for (const cls of n.classesStart || []) if (pobClassNames.has(cls)) startNodeByClass.set(cls, Number(id));
}
const ascStartNode = new Map();
for (const [id, n] of Object.entries(tree.nodes)) {
	if (!n.isAscendancyStart) continue;
	ascStartNode.set(n.ascendancyName, Number(id));
	if (n.isSwitchable) for (const other of Object.keys(n.options || {})) ascStartNode.set(other, Number(id));
}
const ascWarnings = [];
const classes = tree.classes.map((cls, index) => {
	const ci = charsEn.findIndex((c) => c.IntegerId === cls.integerId);
	return {
		index,
		integerId: cls.integerId,
		name: cls.name,
		nameKo: ci >= 0 && usableText(charsKo[ci]?.Name) ? charsKo[ci].Name : null,
		startNodeId: startNodeByClass.get(cls.name) ?? null,
		ascendancies: (cls.ascendancies || []).map((asc) => {
			let ai = ascEn.findIndex((a) => a.Id === asc.internalId);
			if (ai < 0 || ascEn[ai].Name !== asc.name) {
				ascWarnings.push(`${asc.name}: internalId ${asc.internalId} → ${ai >= 0 ? ascEn[ai].Name : "없음"}`);
				ai = ascEn.findIndex((a) => a.Name === asc.name);
			}
			return {
				id: asc.id,
				name: asc.name,
				nameKo: ai >= 0 && usableText(ascKo[ai]?.Name) ? ascKo[ai].Name : null,
				startNodeId: ascStartNode.get(asc.name) ?? null,
			};
		}),
	};
});

// ---- 노드 ----
const ascKoByName = new Map(classes.flatMap((c) => c.ascendancies.map((a) => [a.name, a.nameKo])));
const allIds = new Set(Object.keys(tree.nodes).map(Number));
const placed = new Set();
const skipped = { noGroup: [], badConnection: 0, masteryEdge: 0, duplicateEdge: 0, selfEdge: 0 };
for (const [id, n] of Object.entries(tree.nodes)) {
	if (groupOf(n.group) && n.orbit != null && n.orbitIndex != null) placed.add(Number(id));
	else skipped.noGroup.push(`${id} ${n.name}`);
}

const stat = {
	withStats: 0, matched: 0, lineTotal: 0, lineMatched: 0, noRow: 0,
	koLines: 0, koHangul: 0, nameKo: 0, nameEnMatch: 0, mismatches: [], describeEmpty: 0,
	nameFromAscendancy: [], nameMismatch: [], nodesKoFull: 0,
};
const edgeSeen = new Set();
const positions = new Map();
const nodes = {};
for (const [idStr, n] of Object.entries(tree.nodes)) {
	const id = Number(idStr);
	if (!placed.has(id)) continue;
	const p = position(n);
	positions.set(id, p);
	const kind = kindOf(n);
	const game = gameByGraphId.get(n.skill);
	const pobStats = n.stats || [];

	// 이름
	// 이름: 게임 행 한글. 단 전직 시작 노드 몇 개는 게임 행 이름이 옛 전직명이다(Invoker→"Master of the Elements",
	// Lich→"Necromancer", Warbringer→"Brute", Gemling Legionnaire→"Gambler") — 영문이 PoB 와 다르고 PoB 이름이
	// 전직 이름이면 Ascendancy 테이블 한글을 쓴다.
	let nameKo = game?.nameKo || null;
	const nameAgrees = game && norm(game.nameEn) === norm(n.name || "");
	if (nameAgrees) stat.nameEnMatch++;
	else if (ascKoByName.get(n.name)) {
		nameKo = ascKoByName.get(n.name);
		stat.nameFromAscendancy.push(`${n.name}(게임 행: ${game?.nameEn})`);
	} else if (game) stat.nameMismatch.push(`${id} ${n.name} ≠ ${game.nameEn}`);
	if (nameKo) stat.nameKo++;

	// 스탯: 서술기 영문이 PoB 와 같으면 한글을 믿는다. 다르면 alignLines 가 맞는 줄은 한글, 남은 한글 줄로
	// 빈 자리를 채우고, 한글이 모자라면 PoB 영문을 남긴다(효과가 툴팁에서 사라지지 않게).
	// 게임 행이 없는 노드는 PoB 영문 그대로.
	let statsKo = pobStats.slice();
	if (!game) stat.noRow++;
	if (pobStats.length) {
		stat.withStats++;
		stat.lineTotal += pobStats.length;
		if (game) {
			const aligned = alignLines(pobStats, game.enLines, game.koLines, { fillHoles: !game.valuesMissing });
			stat.lineMatched += aligned.matchedPob;
			if (aligned.full) stat.matched++;
			else stat.mismatches.push({ id, name: n.name, pob: pobStats, game: game.enLines, ko: aligned.lines });
			if (game.koLines.length) statsKo = aligned.lines;
			else stat.describeEmpty++;
		}
		stat.koLines += statsKo.length;
		stat.koHangul += statsKo.filter((l) => HANGUL.test(l)).length;
		if (statsKo.every((l) => HANGUL.test(l))) stat.nodesKoFull++;
	}

	// 간선
	const out = [];
	for (const c of n.connections || []) {
		const to = Number(c.id);
		if (!allIds.has(to) || !placed.has(to)) { skipped.badConnection++; continue; }
		if (to === id) { skipped.selfEdge++; continue; }
		if (n.isOnlyImage || tree.nodes[to].isOnlyImage) { skipped.masteryEdge++; continue; }
		const key = id < to ? id + "-" + to : to + "-" + id;
		if (edgeSeen.has(key)) { skipped.duplicateEdge++; continue; }
		edgeSeen.add(key);
		out.push({ id: to, orbit: c.orbit });
	}

	const entry = {
		name: n.name || "",
		nameKo,
		kind,
		stats: pobStats,
		statsKo,
		x: Math.round(p.x),
		y: Math.round(p.y),
		group: n.group,
		orbit: n.orbit,
		orbitIndex: n.orbitIndex,
		out,
		ascendancy: n.ascendancyName || null,
		classStart: n.classesStart ? n.classesStart.filter((c) => pobClassNames.has(c)) : null,
		icon: n.icon || null,
	};
	if (game?.reminders?.length) {
		entry.reminders = game.reminders.map((r) => stripMarkup(r.en));
		entry.remindersKo = game.reminders.map((r) => (usableText(r.ko) ? stripMarkup(r.ko) : stripMarkup(r.en)));
	}
	if (n.flavourText) {
		entry.flavour = n.flavourText;
		if (game?.flavourKo) entry.flavourKo = game.flavourKo;
	}
	// 능력치 노드 선택지(+5 힘/민첩/지능) — 선택지 id 도 PassiveSkills 그래프 id 라 같은 방식으로 한글화
	if (n.isAttribute && Array.isArray(n.options)) {
		entry.options = n.options.map((o) => {
			const g = gameByGraphId.get(o.id);
			const stats = o.stats || [];
			return {
				id: o.id,
				name: o.name,
				nameKo: g?.nameKo || null,
				stats,
				statsKo: g?.koLines?.length ? alignLines(stats, g.enLines, g.koLines).lines : stats,
				icon: o.icon || null,
			};
		});
	}
	nodes[id] = entry;
}

// 그룹 중심 — 같은 그룹·같은 궤도 호(connection orbit 0)를 그리려면 중심이 필요하다(PoB BuildConnector)
const groups = {};
for (const id of placed) {
	const g = tree.nodes[id].group;
	if (!groups[g]) groups[g] = { x: Math.round(groupOf(g).x), y: Math.round(groupOf(g).y) };
}

const result = {
	patch,
	treeVersion: TREE_VERSION,
	bounds: { minX: tree.min_x, minY: tree.min_y, maxX: tree.max_x, maxY: tree.max_y },
	orbitRadii,
	classes,
	groups,
	nodes,
};
const outPath = writeJson("passive-tree.json", result);

// ======================= 보고 =======================
const pct = (a, b) => (b ? ((a / b) * 100).toFixed(1) + "%" : "-");
const placedCount = Object.keys(nodes).length;
const byKind = {};
for (const e of Object.values(nodes)) byKind[e.kind] = (byKind[e.kind] || 0) + 1;
console.log(`노드 ${placedCount}개` + (skipped.noGroup.length ? ` (그룹 없는 ${skipped.noGroup.length}개 제외: ${skipped.noGroup.join(", ")})` : ""));
console.log("종류별:", JSON.stringify(byKind));
console.log(`간선 ${edgeSeen.size}개 — 제외: 트리에 없는 노드로 ${skipped.badConnection}, mastery ${skipped.masteryEdge}, 중복 ${skipped.duplicateEdge}, 자기 ${skipped.selfEdge}`);
console.log(`게임 행 없는 노드 ${stat.noRow}, 영문 이름 일치 ${stat.nameEnMatch}/${placedCount - stat.noRow} (${pct(stat.nameEnMatch, placedCount - stat.noRow)})`);
console.log(`  전직명으로 대체 ${stat.nameFromAscendancy.length}: ${stat.nameFromAscendancy.join(", ")}`);
if (stat.nameMismatch.length) console.log(`  그 밖의 이름 불일치 ${stat.nameMismatch.length}: ${stat.nameMismatch.slice(0, 10).join(", ")}`);
console.log(`한글 이름: ${stat.nameKo}/${placedCount} (${pct(stat.nameKo, placedCount)})`);
console.log(`영문 스탯 일치(노드): ${stat.matched}/${stat.withStats} (${pct(stat.matched, stat.withStats)}), 줄: ${stat.lineMatched}/${stat.lineTotal} (${pct(stat.lineMatched, stat.lineTotal)})`);
console.log(`한글 스탯: 노드 ${stat.nodesKoFull}/${stat.withStats} (${pct(stat.nodesKoFull, stat.withStats)}), 줄 ${stat.koHangul}/${stat.koLines} (${pct(stat.koHangul, stat.koLines)}), 서술 빈 노드 ${stat.describeEmpty}`);
const unknown = reportUnknownHandlers();
console.log(`서술 파일 전처리: table_only 변형 ${cleanStats.tableOnly}줄 제거, per_minute_to_per_second ${cleanStats.perMinute}곳 소수 유지로 교체`);
if (unknown.length) console.log("모르는 핸들러(패시브 전체 9731행 서술 중 만남):", unknown.join(", "));
if (ascWarnings.length) console.log("전직 internalId 불일치:", ascWarnings.join(" / "));
console.log("불일치 예시:");
for (const m of stat.mismatches.slice(0, 12)) console.log(`  ${m.id} ${m.name}\n    PoB : ${JSON.stringify(m.pob)}\n    game: ${JSON.stringify(m.game)}`);
if (process.env.DUMP_MISMATCH) fs.writeFileSync(process.env.DUMP_MISMATCH, JSON.stringify(stat.mismatches, null, 1));

// ---- 기하 점검 ----
// (0) 그룹 번호 해석: 모든 노드가 자기 그룹의 group.nodes 목록에 들어 있어야 한다(좌표와 무관한 독립 증거).
// (1) 각도 관례: 그룹 간 간선 길이(p50/p90)가 sin/cos 를 뒤바꾼 배치보다 짧아야 한다(관례가 틀리면 그룹 간 간선이 늘어난다).
// (2) 같은 그룹·같은 궤도에 이웃한 노드 쌍의 현(chord) 길이 == 2r·sin(Δa/2) (각도표 → 좌표 변환 확인).
// (3) 호 간선(orbit≠0, 2147483647 제외): 두 끝 거리 ≤ 2·orbitRadii[|orbit|] (PoB 가 sqrt(r²−d²/4) 로 중심을 구하므로 필수).
// (4) 직업 시작점 6개: 원점에서 거의 같은 거리, 60° 간격.
// (5) 모든 노드가 bounds 안.
const swapped = (n) => {
	const g = groupOf(n.group);
	const a = orbitAngle(n.orbit, n.orbitIndex);
	const r = orbitRadii[n.orbit];
	return { x: g.x + r * Math.cos(a), y: g.y + r * Math.sin(a) };
};
const lens = [];
const lensSwapped = [];
let chordChecked = 0;
let chordMaxErr = 0;
let arcChecked = 0;
let arcViolations = 0;
for (const [idStr, e] of Object.entries(nodes)) {
	const id = Number(idStr);
	const a = positions.get(id);
	for (const c of e.out) {
		const b = positions.get(c.id);
		const n1 = tree.nodes[id];
		const n2 = tree.nodes[c.id];
		if (n1.group !== n2.group) {
			lens.push(Math.hypot(a.x - b.x, a.y - b.y));
			const sa = swapped(n1);
			const sb = swapped(n2);
			lensSwapped.push(Math.hypot(sa.x - sb.x, sa.y - sb.y));
		}
		if (n1.group === n2.group && n1.orbit === n2.orbit && n1.orbit > 0) {
			const r = orbitRadii[n1.orbit];
			const expected = 2 * r * Math.abs(Math.sin((a.angle - b.angle) / 2));
			chordMaxErr = Math.max(chordMaxErr, Math.abs(expected - Math.hypot(a.x - b.x, a.y - b.y)));
			chordChecked++;
		}
		if (c.orbit !== 0 && Math.abs(c.orbit) < orbitRadii.length) {
			arcChecked++;
			if (Math.hypot(a.x - b.x, a.y - b.y) > 2 * orbitRadii[Math.abs(c.orbit)] + 1) arcViolations++;
		}
	}
}
const q = (arr, f) => { const s = [...arr].sort((a, b) => a - b); return s[Math.floor(s.length * f)]; };
const membership = [...placed].filter((id) => groupOf(tree.nodes[id].group)?.nodes?.includes(id)).length;
console.log(`기하(0) 그룹 소속 일치 ${membership}/${placed.size}`);
console.log(`기하(1) 그룹 간 간선 ${lens.length}개 길이 p50/p90: ${q(lens, 0.5).toFixed(0)}/${q(lens, 0.9).toFixed(0)} vs sin/cos 뒤바꿈 ${q(lensSwapped, 0.5).toFixed(0)}/${q(lensSwapped, 0.9).toFixed(0)}`);
// 같은 궤도에서 연속 이웃(Δindex=1)의 간격은 모두 같은가 — 궤도별 현 길이 분산
const sameOrbitSteps = new Map();
for (const [idStr, e] of Object.entries(nodes)) {
	const n1 = tree.nodes[idStr];
	for (const c of e.out) {
		const n2 = tree.nodes[c.id];
		if (n1.group !== n2.group || n1.orbit !== n2.orbit || n1.orbit === 0) continue;
		const count = skillsPerOrbit[n1.orbit];
		const d = Math.abs(n1.orbitIndex - n2.orbitIndex);
		const step = Math.min(d, count - d);
		const key = `${n1.orbit}:${step}`;
		const a = positions.get(Number(idStr));
		const b = positions.get(c.id);
		const len = Math.hypot(a.x - b.x, a.y - b.y);
		const s = sameOrbitSteps.get(key) || { min: Infinity, max: -Infinity, n: 0 };
		s.min = Math.min(s.min, len); s.max = Math.max(s.max, len); s.n++;
		sameOrbitSteps.set(key, s);
	}
}
const stepSpread = [...sameOrbitSteps].map(([k, s]) => [k, s.n, (s.max - s.min).toFixed(3)]).sort((a, b) => b[1] - a[1]);
console.log(`기하(2) 같은 궤도 이웃 ${chordChecked}쌍, 현 길이 오차 최대 ${chordMaxErr.toExponential(2)}; 궤도:간격별 (쌍수, 최대-최소):`, JSON.stringify(stepSpread.slice(0, 8)));
console.log(`기하(3) 호 간선 ${arcChecked}개 중 반지름 초과 ${arcViolations}개`);
const starts = Object.entries(nodes).filter(([, e]) => e.kind === "classStart").map(([id, e]) => {
	const p = positions.get(Number(id));
	return { id, cls: e.classStart.join("/"), r: Math.hypot(p.x, p.y).toFixed(0), deg: ((Math.atan2(p.x, -p.y) * 180) / Math.PI).toFixed(1) };
});
console.log("기하(4) 직업 시작점(원점 거리, 12시 기준 시계방향 각도):", JSON.stringify(starts));
const outOfBounds = [...positions.values()].filter((p) => p.x < tree.min_x - 1 || p.x > tree.max_x + 1 || p.y < tree.min_y - 1 || p.y > tree.max_y + 1).length;
console.log(`기하(5) bounds 밖 노드 ${outOfBounds}개`);

// ---- 자가검사: 매치율 카운터가 불일치를 잡는가 ----
// 일치한 노드 3개를 골라 PoB 줄을 일부러 망가뜨려(숫자 +1, 줄 삭제, 줄 추가) linesMatch 가 false 로 바뀌는지,
// 원본은 여전히 true 인지 본다. 하나라도 통과하면 카운터가 무력한 것이므로 실패로 종료한다.
{
	const matchedIds = Object.entries(nodes)
		.filter(([id, e]) => e.stats.length && gameByGraphId.get(tree.nodes[id].skill) && linesMatch(gameByGraphId.get(tree.nodes[id].skill).enLines, e.stats))
		.map(([id]) => id);
	const pick = [matchedIds.find((id) => nodes[id].stats.some((s) => /\d/.test(s))), matchedIds[1], matchedIds[2]];
	const defects = [
		(lines) => { const i = lines.findIndex((s) => /\d/.test(s)); const c = lines.slice(); c[i] = c[i].replace(/\d+/, (d) => String(Number(d) + 1)); return c; },
		(lines) => (lines.length > 1 ? lines.slice(1) : ["Planted defect line"]),
		(lines) => lines.concat(["10% increased Planted Defect"]),
	];
	let caught = 0;
	const report = [];
	pick.forEach((id, k) => {
		const game = gameByGraphId.get(tree.nodes[id].skill).enLines;
		const orig = nodes[id].stats;
		const bad = defects[k](orig);
		const okOrig = linesMatch(game, orig);
		const okBad = linesMatch(game, bad);
		if (okOrig && !okBad) caught++;
		report.push({ id, orig, planted: bad, origMatches: okOrig, plantedMatches: okBad });
	});
	console.log("자가검사(심은 결함):", JSON.stringify(report));
	console.log(`자가검사 결과: ${caught}/${pick.length} 결함 탐지`);
	if (caught !== pick.length) {
		console.error("자가검사 실패 — 매치율 카운터가 불일치를 못 잡는다");
		process.exitCode = 1;
	}
}

const size = fs.statSync(outPath).size;
console.log(`→ ${outPath} (${(size / 1024 / 1024).toFixed(2)}MB, ${size} bytes)`);
