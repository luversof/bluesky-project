// GGG 공식 패시브 트리(grindinggear/skilltree-export data.json) → 뷰어용 경량 JSON.
// 노드 좌표/궤도/그룹/간선을 tree-common 으로 계산하고, 한국어(이름·스탯)를 게임 테이블로 조인한다.
// 사용법: node parse-tree.mjs   (스프라이트 시트는 tree-sprites.mjs 가 별도 처리)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";
import { alignKoToGameOrder, buildKoreanMap, buildTree, joinReminderKo } from "./tree-common.mjs";
import { createTemplateTranslator } from "./ko-templates.mjs";
import { applyOfficialReminderKo, loadKakaoTree } from "./kakao-tree.mjs";

const RAW = path.join(WORK_DIR, "passive-tree-raw.json");
const OUT = path.join(process.env.POE_DATA_OUT || DATA_DIR, "passive-tree.json"); // POE_DATA_OUT = 사본 출력(10-10)
const SOURCE_URL = "https://raw.githubusercontent.com/grindinggear/skilltree-export/master/data.json";

// 트리는 항상 GGG master 에서 최신본을 받는다 — RAW 캐시가 있으면 재fetch 를 건너뛰던 옛 로직은
// 게임 데이터가 갱신돼도 트리를 옛 스냅샷에 고정시켰다(실측: 7/14 fetch 3337노드 vs GGG master 3389노드).
// 다운로드 실패 시에만 기존 캐시로 폴백한다.
try {
	console.log("다운로드:", SOURCE_URL);
	const response = await fetch(SOURCE_URL);
	if (!response.ok) throw new Error(`다운로드 실패: ${response.status}`);
	fs.writeFileSync(RAW, await response.text());
} catch (e) {
	if (!fs.existsSync(RAW)) throw e; // 캐시조차 없으면 진짜 실패
	console.warn("트리 다운로드 실패 — 기존 캐시로 폴백:", e.message);
}

const tree = JSON.parse(fs.readFileSync(RAW, "utf8"));

// 한국어: 게임 데이터 PassiveSkills(이름) + 스탯 문장 파서(스탯 라인).
// GGG 트리 익스포트는 영어 전용이라 PassiveSkillGraphId 로 조인해 결합한다.
// 게임 passive_skill_stat_descriptions 는 stat_descriptions 만 include 한다(10-10 C257) — 기본 사슬은 skill 계열이 끼어 노드 한국어가
//   "소환수 최대 생명력" · "저주 효과" · "보조 대상 스킬의 …"(젬 문장)였다. 영어 줄은 GGG 트리 원본("Minions have …% increased maximum Life")이라 한국어만 어긋났다
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@stat_descriptions.txt",
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
// ---- 한국 공식 패시브 트리(카카오) — 노드별 공식 한국어 스탯 ----
// poe.kakaogames.com/passive-skill-tree 페이지에 트리 데이터(passiveSkillTreeData)가 한국어로 박혀 있다(10-13 C295).
// 게임 테이블 · 서술기로 안 나오는 줄(트리 export 에만 있는 "Nearby Enemies have …" 6줄 · 패시브 포인트 노드)을 이 공식 문장으로 채운다.
// 받기에 실패하면 WORK_DIR 캐시, 그것도 없으면 예전대로(사전 · 수동 번역).
// 받기 · 캐시 · 페이지에서 꺼내기는 아틀라스 트리와 같이 kakao-tree.mjs 로(10-13 C296)
const kakaoNodes = await loadKakaoTree("https://poe.kakaogames.com/passive-skill-tree", path.join(WORK_DIR, "kakao-passive-tree.json"), "패시브 트리");
// 영어 줄 i 의 공식 한국어 — 줄 수가 같을 때만(순서가 같다고 볼 수 있을 때)
const kakaoLine = (node, i) => {
	const k = kakaoNodes?.[String(node.id)];
	return k && node.stats && k.stats.length === node.stats.length && i >= 0 ? k.stats[i] : null;
};
// 게임 데이터가 한글 문장을 아예 안 주는 몇 개는 수동 번역 — 공식 트리를 못 받았을 때의 대체. 문구는 공식 트리와 같게(C295: "1 획득" → "1포인트 제공")
const MANUAL_KO = {
	"Grants 1 Passive Skill Point": "패시브 스킬 포인트 1포인트 제공",
	"Grants 2 Passive Skill Points": "패시브 스킬 포인트 2포인트 제공",
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
	const lines = node.stats.map((line, i) => kakaoLine(node, i) || MANUAL_KO[line] || translator.translate(line));
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
			const ko = (t === node ? kakaoLine(node, t.stats.indexOf(line)) : null) || MANUAL_KO[line] || translator.translate(line);
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
// 줄 수 어긋남 — 서술기가 영어(트리 export)보다 줄을 더 만든 노드는 공식 트리가 영어와 줄 수가 같으면 공식 문장으로(10-13 C295,
// 시간의 모래: 게임 표 스탯 2개라 "20레벨 시간 속 정지 발동" 줄이 하나 더 생겼다 — 영어 · 공식 한국어는 한 줄)
let countFixed = 0;
for (const node of result.nodes) {
	const k = kakaoNodes?.[String(node.id)];
	if (!k || !node.stats?.length || !node.statsKo?.length) continue;
	if (node.statsKo.length !== node.stats.length && k.stats.length === node.stats.length) {
		node.statsKo = [...k.stats];
		countFixed++;
	}
}
console.log(`줄 수 어긋남을 공식 트리로 맞춤: ${countFixed}개 노드`);

// 리마인더 한글 — 게임 테이블 ReminderText(EN/KO) 페어링 조인.
// 테이블은 config 추가(2026-07) 후 추출부터 생긴다 — 옛 추출물로 parse 만 단독 재실행해도 죽지 않게 폴백.
try {
	const rem = joinReminderKo(result, loadTable("English", "ReminderText"), loadTable("Korean", "ReminderText"));
	console.log(`리마인더 한글: ${rem.hit}/${rem.total}`);
	// 페어링에 없는 리마인더 줄은 한국 공식 트리 문장으로(10-13 C301 — 수호자 "거룩한 불길" 설명)
	console.log(`리마인더 공식 트리로 보강: ${applyOfficialReminderKo(result.nodes, kakaoNodes)}개 노드`);
} catch {
	console.log("리마인더 한글: ReminderText 테이블 없음(extract 먼저 실행 필요) — 영문 유지");
}

// 후행 스텝(parse-anoints)이 이 파일에 **주입**해 둔 것을 되살린다.
// 이 파서만 단독 재실행하면 도유(성유) 470개 + 오일 14종이 통째로 날아가 노터블 툴팁의 기름 아이콘이 사라진다
// (실제로 한 번 겪었다). 낡을 수는 있어도 없어지는 것보단 낫고, parse-anoints 를 다시 돌리면 갱신된다.
// 사본 출력(POE_DATA_OUT)이어도 되살릴 원본은 실제 데이터 파일(10-10)
const PREV = path.join(DATA_DIR, "passive-tree.json");
if (fs.existsSync(PREV)) {
	try {
		const prev = JSON.parse(fs.readFileSync(PREV, "utf8"));
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
