// GGG 공식 패시브 트리(grindinggear/skilltree-export data.json) → 뷰어용 경량 JSON.
// 노드 좌표/궤도/그룹/간선을 tree-common 으로 계산하고, 한국어(이름·스탯)를 게임 테이블로 조인한다.
// 사용법: node parse-tree.mjs   (스프라이트 시트는 tree-sprites.mjs 가 별도 처리)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";
import { buildKoreanMap, buildTree } from "./tree-common.mjs";
import { createTemplateTranslator } from "./ko-templates.mjs";

const RAW = path.join(WORK_DIR, "passive-tree-raw.json");
const OUT = path.join(DATA_DIR, "passive-tree.json");
const SOURCE_URL = "https://raw.githubusercontent.com/grindinggear/skilltree-export/master/data.json";

if (!fs.existsSync(RAW)) {
	console.log("다운로드:", SOURCE_URL);
	const response = await fetch(SOURCE_URL);
	if (!response.ok) throw new Error(`다운로드 실패: ${response.status}`);
	fs.writeFileSync(RAW, await response.text());
}

const tree = JSON.parse(fs.readFileSync(RAW, "utf8"));

// 한국어: 게임 데이터 PassiveSkills(이름) + 스탯 문장 파서(스탯 라인).
// GGG 트리 익스포트는 영어 전용이라 PassiveSkillGraphId 로 조인해 결합한다.
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@passive_skill_stat_descriptions.txt",
]);
const statsTable = loadTable("English", "Stats");
const passivesEn = loadTable("English", "PassiveSkills");
const passivesKo = loadTable("Korean", "PassiveSkills");
const koByGraphId = buildKoreanMap({ describe, statsTable, passivesEn, passivesKo });

const result = buildTree(tree, koByGraphId);

// 마스터리 효과 한글: GGG 트리 익스포트는 완성된 영문 문장만 주므로, 게임 테이블
// PassiveSkillMasteryEffects 의 HASH16(= 트리 export 의 effect id) 로 조인해 스탯을 한글로 서술한다.
const masteryEn = loadTable("English", "PassiveSkillMasteryEffects");
const koByEffectHash = new Map();
for (const row of masteryEn) {
	const statValues = new Map();
	(row.Stats || []).forEach((statIndex, i) => {
		statValues.set(statsTable[statIndex].Id, row["Stat" + (i + 1) + "Value"] ?? 0);
	});
	koByEffectHash.set(row.HASH16, describe(statValues, "Korean"));
}
let effectTotal = 0;
let effectKo = 0;
for (const node of result.nodes) {
	for (const eff of node.masteryEffects || []) {
		effectTotal++;
		const lines = koByEffectHash.get(eff.id);
		if (lines?.length) {
			eff.statsKo = lines;
			effectKo++;
		}
	}
}
console.log(`마스터리 노드 ${result.nodes.filter((n) => n.masteryEffects).length}개, 효과 ${effectTotal}개 중 한글 ${effectKo}개 (${((effectKo / effectTotal) * 100).toFixed(0)}%)`);

// ---- 2차 한글화: 스탯 서술기가 문장을 못 만든 노드를 **문장 템플릿 사전**으로 메운다 ----
// 1차 조인(PassiveSkills + 서술기)이 놓치는 노드가 24개 남는다(대부분 "Grants 1 Passive Skill Point").
// 같은 문장이 다른 노드에선 한글로 나오므로, 그 쌍을 모아 사전을 만들어 되메운다.
const translator = createTemplateTranslator();
translator.addFromStats(describe, statsTable);
translator.addPairs(result.nodes.map((node) => ({ en: node.stats || [], ko: node.statsKo || [] })));
translator.addPairs(
	result.nodes.flatMap((node) => (node.masteryEffects || []).map((eff) => ({ en: eff.stats || [], ko: eff.statsKo || [] }))),
);
// 게임 데이터가 한글 문장을 아예 안 주는 몇 개는 수동 번역(직접 옮긴 것임을 명시).
const MANUAL_KO = {
	"Grants 1 Passive Skill Point": "패시브 스킬 포인트 1 획득",
};
// 마스터리 효과도 같은 사전으로 보강 — 1825개 중 7개가 서술기에서 안 나온다
let effectFilled = 0;
for (const node of result.nodes) {
	for (const eff of node.masteryEffects || []) {
		if (eff.statsKo?.length || !eff.stats?.length) continue;
		const lines = eff.stats.map((line) => MANUAL_KO[line] || translator.translate(line));
		if (lines.every(Boolean)) {
			eff.statsKo = lines;
			effectFilled++;
		}
	}
}
let filled = 0;
let stillMissing = 0;
for (const node of result.nodes) {
	if (!node.stats?.length || node.statsKo?.length) continue;
	const lines = node.stats.map((line) => MANUAL_KO[line] || translator.translate(line));
	if (lines.every(Boolean)) {
		node.statsKo = lines;
		filled++;
	} else {
		stillMissing++;
	}
}
console.log(
	`스탯 한글 보강: 템플릿 ${translator.size}개로 노드 ${filled}개 + 마스터리 효과 ${effectFilled}개 채움, 남은 미번역 노드 ${stillMissing}개`,
);

fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(`nodes ${result.nodes.length}, edges ${result.edges.length}, groups ${Object.keys(result.groups).length} → ${OUT}`);
console.log("size:", (fs.statSync(OUT).size / 1024 / 1024).toFixed(1) + "MB");
