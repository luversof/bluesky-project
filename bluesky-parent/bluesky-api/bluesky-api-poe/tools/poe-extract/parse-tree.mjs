// GGG 공식 패시브 트리(grindinggear/skilltree-export data.json) → 뷰어용 경량 JSON.
// 노드 좌표/궤도/그룹/간선을 tree-common 으로 계산하고, 한국어(이름·스탯)를 게임 테이블로 조인한다.
// 사용법: node parse-tree.mjs   (스프라이트 시트는 tree-sprites.mjs 가 별도 처리)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";
import { alignKoToGameOrder, buildKoreanMap, buildTree, joinReminderKo } from "./tree-common.mjs";
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
	// 영문 서술도 함께 — 게임 표기 순서(eff.stats)로 재배열할 때 다리(alignKoToGameOrder)
	koByEffectHash.set(row.HASH16, { ko: describe(statValues, "Korean"), en: describe(statValues, "English") });
}
let effectTotal = 0;
let effectKo = 0;
for (const node of result.nodes) {
	for (const eff of node.masteryEffects || []) {
		effectTotal++;
		const entry = koByEffectHash.get(eff.id);
		if (entry?.ko?.length) {
			eff.statsKo = alignKoToGameOrder(eff.stats, entry.ko, entry.en);
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
	"Grants 2 Passive Skill Points": "패시브 스킬 포인트 2 획득",
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
// 부분 미번역 줄 보강 — 게임 PassiveSkills 테이블엔 없고 트리 export 에만 있는 줄은
// alignKoToGameOrder 가 영문 원문 그대로 넣어 둔다(정보 손실 방지). 그 줄들만 골라 사전으로 다시 옮긴다.
let lineFilled = 0;
let lineEnglish = 0;
for (const node of result.nodes) {
	const targets = [node, ...(node.masteryEffects || [])];
	for (const t of targets) {
		if (!t.statsKo?.length || !t.stats?.length) continue;
		t.statsKo = t.statsKo.map((line) => {
			if (!t.stats.includes(line)) return line; // 이미 한글
			const ko = MANUAL_KO[line] || translator.translate(line);
			if (ko) {
				lineFilled++;
				return ko;
			}
			lineEnglish++;
			return line;
		});
	}
}
console.log(
	`스탯 한글 보강: 템플릿 ${translator.size}개로 노드 ${filled}개 + 마스터리 효과 ${effectFilled}개 채움, 남은 미번역 노드 ${stillMissing}개`,
);
console.log(`부분 미번역 줄: ${lineFilled}줄 사전으로 한글화, ${lineEnglish}줄은 한글 소스 없어 영문 유지`);

// 리마인더 한글 — 게임 테이블 ReminderText(EN/KO) 페어링 조인.
// 테이블은 config 추가(2026-07) 후 추출부터 생긴다 — 옛 추출물로 parse 만 단독 재실행해도 죽지 않게 폴백.
try {
	const rem = joinReminderKo(result, loadTable("English", "ReminderText"), loadTable("Korean", "ReminderText"));
	console.log(`리마인더 한글: ${rem.hit}/${rem.total}`);
} catch {
	console.log("리마인더 한글: ReminderText 테이블 없음(extract 먼저 실행 필요) — 영문 유지");
}

// 후행 스텝(parse-anoints)이 이 파일에 **주입**해 둔 것을 되살린다.
// 이 파서만 단독 재실행하면 도유(성유) 470개 + 오일 14종이 통째로 날아가 노터블 툴팁의 기름 아이콘이 사라진다
// (실제로 한 번 겪었다). 낡을 수는 있어도 없어지는 것보단 낫고, parse-anoints 를 다시 돌리면 갱신된다.
if (fs.existsSync(OUT)) {
	try {
		const prev = JSON.parse(fs.readFileSync(OUT, "utf8"));
		const anoints = new Map((prev.nodes || []).filter((n) => n.anoint).map((n) => [n.id, n.anoint]));
		if (anoints.size) {
			for (const node of result.nodes) {
				const a = anoints.get(node.id);
				if (a) node.anoint = a;
			}
			if (prev.oils) result.oils = prev.oils;
			console.log(`이전 산출물에서 도유 ${anoints.size}개 노드 + 오일 ${Object.keys(prev.oils || {}).length}종 보존(갱신은 parse-anoints)`);
		}
	} catch {
		console.log("이전 passive-tree.json 읽기 실패 — 주입분 보존 건너뜀");
	}
}

fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(`nodes ${result.nodes.length}, edges ${result.edges.length}, groups ${Object.keys(result.groups).length} → ${OUT}`);
console.log("size:", (fs.statSync(OUT).size / 1024 / 1024).toFixed(1) + "MB");
