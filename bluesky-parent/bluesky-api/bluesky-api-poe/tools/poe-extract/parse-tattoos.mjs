// 문신(Tattoo) 정의 추출 — PoB 의 Data/TattooPassives.lua(게임 데이터 파생본)를 JSON 으로 옮긴다.
// 문신은 **소형/노터블 패시브를 통째로 다른 노드로 교체**하는 아이템이라, 트리 export 에는 존재하지 않는다.
// PoB 는 빌드 XML 의 <Spec><Overrides><Override nodeId dn .../> 를 tree.tattoo.nodes[dn] 로 찾아 적용한다
// (헤드리스에서도 동작함을 실측 확인: 소형 지능 패시브를 "Honoured Tattoo of the Sky" 로 덮으니 원소 저항 -60 → -57).
//
// 교체 규칙(게임): targetType 이 맞는 패시브만 덮을 수 있고, 인접 할당 수 제한(Minimum/MaximumConnected)이 있다.
//   · "Small Attribute"  : 힘/민첩/지능 소형 패시브
//   · "Small"            : 그 외 소형 패시브
//   · "Notable" / "Keystone" : 노터블/키스톤
// 한글 이름은 게임 테이블 BaseItemTypes(문신은 아이템이다)에서, 스탯 문장은 템플릿 사전에서 가져온다.
// 사용법: node parse-tattoos.mjs
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";
import { createTemplateTranslator } from "./ko-templates.mjs";

const pobSrc = path.join(WORK_DIR, "pob-src", "src");
const dataFile = path.join(pobSrc, "Data", "TattooPassives.lua");
if (!fs.existsSync(dataFile)) {
	console.warn("TattooPassives.lua 없음 — 이번엔 건너뜀(PoB 소스 클론 후 재실행하면 생성됨):", dataFile);
	process.exit(0);
}

// 정규식 파싱은 중첩 테이블에서 깨진다 — luajit 으로 실제 테이블을 로드해 JSON 으로 뽑는다.
const script = `
package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
local data = dofile("Data/TattooPassives.lua")
local dkjson = require("dkjson")
io.write(dkjson.encode(data))
`;
const scriptPath = path.join(fs.mkdtempSync(path.join(os.tmpdir(), "poe-tattoo-")), "dump.lua");
fs.writeFileSync(scriptPath, script, "utf8");
const dumped = JSON.parse(execFileSync("luajit", [scriptPath], { cwd: pobSrc, encoding: "utf8", maxBuffer: 32 * 1024 * 1024 }));
const nodes = dumped.nodes || {};

// 한글 이름: 문신은 아이템이라 BaseItemTypes 에 영/한이 나란히 있다(같은 인덱스).
const baseEn = loadTable("English", "BaseItemTypes");
const baseKo = loadTable("Korean", "BaseItemTypes");
const nameKo = new Map();
baseEn.forEach((row, i) => {
	const ko = baseKo[i]?.Name;
	if (row?.Name && ko) nameKo.set(row.Name, ko);
});
// 키스톤 문신의 dn 은 아이템 이름이 아니라 **부여하는 키스톤 이름**("Acrobatics")이라 BaseItemTypes 로는 안 잡힌다.
// 트리의 같은 이름 노드에서 한글 이름을 가져온다.
const treeFile = path.join(DATA_DIR, "passive-tree.json");
const tree = fs.existsSync(treeFile) ? JSON.parse(fs.readFileSync(treeFile, "utf8")) : { nodes: [] };
const treeKoByName = new Map();
for (const node of tree.nodes || []) {
	if (node.name && node.nameKo && !nameKo.has(node.name)) nameKo.set(node.name, node.nameKo);
	if (node.name && node.statsKo?.length && !treeKoByName.has(node.name)) treeKoByName.set(node.name, node.statsKo);
}

// 스탯 문장 한글 1순위: 게임 테이블 PassiveSkillOverrides(문신이 만드는 대체 노드의 원본 정의).
// Name 이 PoB dn 과 일치하고 Stats+StatValues 를 서술기로 풀면 **게임 원문 한글**이 나온다.
// (문신 아이템엔 implicit 모드가 없고, 룬 접합 문장은 마스터리 효과와도 달라 템플릿 사전만으론 못 옮긴다)
const describe = createStatDescriber(FILES_DIR, ["metadata@statdescriptions@passive_skill_stat_descriptions.txt"]);
const statsTable = loadTable("English", "Stats");
// 옛 추출본(테이블 추가 전)에서도 파서가 죽지 않게 — 없으면 템플릿 번역만으로 돌아간다
let overridesEn = [];
try {
	overridesEn = loadTable("English", "PassiveSkillOverrides");
} catch {
	console.warn("PassiveSkillOverrides 없음 — extract.mjs 재실행 전까지 템플릿 번역만 사용");
}
const koLinesByName = new Map();
for (const row of overridesEn) {
	if (!row?.Name || !Array.isArray(row.Stats) || !row.Stats.length) continue;
	const values = new Map();
	row.Stats.forEach((statIndex, i) => {
		const stat = statsTable[statIndex];
		if (stat?.Id) values.set(stat.Id, (row.StatValues || [])[i] ?? 0);
	});
	const lines = describe(values, "Korean");
	if (lines?.length && !koLinesByName.has(row.Name)) koLinesByName.set(row.Name, lines);
}
console.log(`PassiveSkillOverrides ${overridesEn.length}행 중 한글 서술 ${koLinesByName.size}건`);
const translator = createTemplateTranslator();
translator.addFromStats(describe, loadTable("English", "Stats"));
// 문신 문장 상당수는 트리 노드/마스터리 효과에 그대로 있다(서술기만으로는 절반도 못 옮긴다) — 그 쌍으로 보강.
translator.addPairs((tree.nodes || []).map((node) => ({ en: node.stats || [], ko: node.statsKo || [] })));
translator.addPairs(
	(tree.nodes || []).flatMap((node) => (node.masteryEffects || []).map((eff) => ({ en: eff.stats || [], ko: eff.statsKo || [] }))),
);
// 게임 데이터가 한글 문장을 안 주는 문신 전용 제한 문구는 직접 옮긴다(인게임 표기 기준).
const MANUAL_KO = {
	"Limited to 1 Keystone Tattoo": "키스톤 문신 1개로 제한",
	"Limited to 1 Ancestral Tattoo": "선조의 문신 1개로 제한",
	"Limited to 1 Ascendancy Tattoo": "승천 문신 1개로 제한",
};
// "Limited to N <종류> Tattoo" 계열은 종류가 여러 갈래라 패턴으로 처리한다(위 표에 없는 것만).
// 룬 접합은 자기 이름으로 제한된다("Limited to 1 Runegraft of X") — 그 한글 이름을 그대로 쓴다.
const limitKo = (line) => {
	const match = /^Limited to (\d+) (.+?) Tattoos?$/.exec(line);
	if (match) return `${match[2]} 문신 ${match[1]}개로 제한`;
	const rune = /^Limited to (\d+) (Runegraft of .+)$/.exec(line);
	if (rune) return `${nameKo.get(rune[2]) || rune[2]} ${rune[1]}개로 제한`;
	return null;
};

const out = [];
let koName = 0;
let koStat = 0;
let statTotal = 0;
for (const [dn, node] of Object.entries(nodes)) {
	if (!dn || !node?.isTattoo) continue;
	const stats = (node.sd || []).filter(Boolean);
	statTotal += stats.length;
	// 1순위: 게임 원문(PassiveSkillOverrides 서술). PoB sd 는 긴 문장을 두 줄로 쪼개므로 줄 수가 달라도 되고,
	//   제한 문구("Limited to …")는 게임 서술엔 없어 별도로 옮겨 덧붙인다.
	// 2순위: 줄 단위 템플릿 번역, 못 옮긴 줄은 영문 유지(한 줄 때문에 전부 영문이 되는 것을 막는다).
	// 키스톤 문신은 실제 키스톤을 부여한다 — 트리의 같은 이름 노드에 게임 원문 한글이 이미 있다
	//   (PoB sd 와 줄 나눔이 달라 템플릿 번역으론 못 옮긴다)
	const gameKo = koLinesByName.get(dn) || (node.ks ? treeKoByName.get(dn) : null);
	const limitLines = stats.filter((line) => /^Limited to /.test(line));
	const statsKo = gameKo
		? [...gameKo, ...limitLines.map((line) => MANUAL_KO[line] || limitKo(line) || line)]
		: stats.map((line) => MANUAL_KO[line] || translator.translate(line) || limitKo(line) || line);
	koStat += gameKo ? stats.length : statsKo.filter((line, i) => line !== stats[i]).length;
	const allKo = stats.length > 0;
	const ko = nameKo.get(dn);
	if (ko) koName++;
	out.push({
		dn,
		name: dn,
		nameKo: ko || null,
		id: node.id || null,
		icon: node.icon || "",
		activeEffectImage: node.activeEffectImage || "",
		targetType: node.targetType || "",
		targetValue: node.targetValue || "",
		overrideType: node.overrideType || "",
		minConnected: node.MinimumConnected ?? 0,
		maxConnected: node.MaximumConnected ?? 100,
		notable: !!node["not"],
		keystone: !!node.ks,
		stats,
		statsKo: allKo ? statsKo : null,
	});
}
out.sort((a, b) => a.dn.localeCompare(b.dn));

const byTarget = out.reduce((acc, t) => ((acc[t.targetType] = (acc[t.targetType] || 0) + 1), acc), {});
const outFile = path.join(DATA_DIR, "tattoos.json");
fs.writeFileSync(outFile, JSON.stringify({ tattoos: out }, null, "\t"), "utf8");
console.log(
	`문신 ${out.length}종 → ${outFile}\n  대상별: ${Object.entries(byTarget).map(([k, v]) => `${k || "(없음)"} ${v}`).join(", ")}` +
		`\n  한글 이름 ${koName}/${out.length}, 한글 스탯 ${koStat}/${statTotal}`,
);
