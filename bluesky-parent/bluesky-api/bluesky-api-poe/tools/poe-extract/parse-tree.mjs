// GGG 공식 패시브 트리(grindinggear/skilltree-export data.json) → 뷰어용 경량 JSON.
// 노드 좌표 = 그룹 중심 + 궤도(orbit) 반지름/각도. 사용법: node parse-tree.mjs
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

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
const { orbitRadii, skillsPerOrbit } = tree.constants;

// 한국어: 게임 데이터 PassiveSkills(이름) + 스탯 문장 파서(스탯 라인).
// GGG 트리 익스포트는 영어 전용이라 PassiveSkillGraphId 로 조인해 결합한다.
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@passive_skill_stat_descriptions.txt",
]);
const statsTable = loadTable("English", "Stats");
const passivesEn = loadTable("English", "PassiveSkills");
const passivesKo = loadTable("Korean", "PassiveSkills");
const koByGraphId = new Map();
passivesEn.forEach((passive, i) => {
	if (passive.PassiveSkillGraphId == null) return;
	const statValues = new Map();
	(passive.Stats || []).forEach((statIndex, statPosition) => {
		const value = passive["Stat" + (statPosition + 1) + "Value"] ?? 0;
		statValues.set(statsTable[statIndex].Id, value);
	});
	koByGraphId.set(passive.PassiveSkillGraphId, {
		nameKo: passivesKo[i]?.Name || null,
		statsKo: describe(statValues, "Korean"),
	});
});

function nodeType(node) {
	if (node.classStartIndex != null) return "class";
	if (node.isKeystone) return "keystone";
	if (node.isNotable) return "notable";
	if (node.isMastery) return "mastery";
	if (node.isJewelSocket) return "jewel";
	return "normal";
}

// node.icon("Art/2DArt/SkillIcons/passives/X.png") → 서빙용 키(tree-icons.mjs 와 동일 규칙)
function iconKey(node) {
	if (!node.icon) return null;
	const lower = node.icon.replace(/\\/g, "/").toLowerCase();
	const idx = lower.indexOf("skillicons/");
	const tail = idx >= 0 ? lower.slice(idx + "skillicons/".length) : lower.split("/").pop();
	return tail.replace(/\//g, "_").replace(/\.png$/, "") + ".png";
}

const nodes = [];
const positioned = new Set();
for (const [id, node] of Object.entries(tree.nodes)) {
	if (node.group == null || node.orbit == null || node.orbitIndex == null) continue; // 클러스터 주얼 템플릿 등
	const group = tree.groups[node.group];
	if (!group) continue;
	const angle = (2 * Math.PI * node.orbitIndex) / skillsPerOrbit[node.orbit];
	const radius = orbitRadii[node.orbit];
	const korean = koByGraphId.get(Number(id));
	nodes.push({
		id: Number(id),
		name: node.name || "",
		nameKo: korean?.nameKo || null,
		type: nodeType(node),
		x: Math.round(group.x + radius * Math.sin(angle)),
		y: Math.round(group.y - radius * Math.cos(angle)),
		stats: node.stats || [],
		statsKo: korean?.statsKo?.length ? korean.statsKo : null,
		ascendancy: node.ascendancyName || null,
		ascendancyStart: node.isAscendancyStart ? true : undefined,
		icon: iconKey(node),
	});
	positioned.add(Number(id));
}

// 간선: out 기준, 양 끝이 모두 배치된 노드일 때만 (a<b 정규화로 중복 제거)
const edgeSet = new Set();
for (const [id, node] of Object.entries(tree.nodes)) {
	const from = Number(id);
	if (!positioned.has(from)) continue;
	for (const outId of node.out || []) {
		const to = Number(outId);
		if (!positioned.has(to)) continue;
		const key = from < to ? from + "-" + to : to + "-" + from;
		edgeSet.add(key);
	}
}
const edges = [...edgeSet].map((k) => k.split("-").map(Number));

const result = {
	bounds: { minX: tree.min_x, minY: tree.min_y, maxX: tree.max_x, maxY: tree.max_y },
	// 직업별 전직 목록 — 배열 순서가 PoB Spec 의 ascendClassId (1부터) 와 일치한다
	classes: (tree.classes || []).map((cls) => ({
		name: cls.name,
		ascendancies: (cls.ascendancies || []).map((asc) => asc.name),
	})),
	nodes,
	edges,
};
fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(`nodes ${nodes.length}, edges ${edges.length} → ${OUT}`);
console.log("size:", (fs.statSync(OUT).size / 1024 / 1024).toFixed(1) + "MB");
