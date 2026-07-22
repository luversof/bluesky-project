// 클러스터 주얼 정의 추출 — PoB 의 Data/ClusterJewels.lua(게임 데이터 파생본)를 JSON 으로 옮긴다.
// GGG 트리 export 에는 클러스터 주얼이 만들어내는 서브트리 정의가 없다(소켓의 expansionJewel 참조만 있음).
// PoB 는 그 정의를 별도 데이터로 들고 있으므로, 엔진용으로 이미 받아둔 소스를 그대로 재사용한다.
// 변환은 luajit 로 실제 Lua 테이블을 로드해 JSON 으로 뽑는다(정규식 파싱은 중첩 테이블에서 깨진다).
import { execFileSync } from "node:child_process";
import { writeFileSync, existsSync, mkdtempSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";
import { createTemplateTranslator } from "./ko-templates.mjs";
import { readFileSync } from "node:fs";

const pobSrc = join(WORK_DIR, "pob-src", "src");
const dataFile = join(pobSrc, "Data", "ClusterJewels.lua");
if (!existsSync(dataFile)) {
	// run-all 은 PoB 소스를 스텝 루프 뒤에 클론한다 — 첫 실행에선 없을 수 있으므로 실패가 아니라 건너뛴다.
	console.warn("ClusterJewels.lua 없음 — 이번엔 건너뜀(PoB 소스 클론 후 재실행하면 생성됨):", dataFile);
	process.exit(0);
}

// dkjson 은 PoB 런타임에 들어있다(../runtime/lua). cwd 를 pob-src/src 로 맞춰야 경로가 풀린다.
const script = `
package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
local data = dofile("Data/ClusterJewels.lua")
local mods = dofile("Data/ModJewelCluster.lua")
local dkjson = require("dkjson")
io.write(dkjson.encode({ clusterJewels = data, clusterMods = mods }))
`;
const scriptPath = join(mkdtempSync(join(tmpdir(), "poe-cluster-")), "dump.lua");
writeFileSync(scriptPath, script, "utf8");

const raw = execFileSync("luajit", [scriptPath], { cwd: pobSrc, encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
const dumped = JSON.parse(raw);
const parsed = dumped.clusterJewels || {};

// 노터블이 **어떤 스킬(태그)·어떤 크기**의 주얼에 붙을 수 있는지 — PoB 의 아이템 모드 테이블에서 뽑는다.
//  · weightKey/weightVal : 주얼 스킬 태그별 등장 가중치(>0 이어야 그 스킬의 주얼에 나온다)
//  · weightMultiplierKey/Val : expansion_jewel_large/medium/small 이 0 이면 그 크기엔 안 나온다
// 이게 없으면 UI 가 309개 전부를 보여줘 **게임에 존재할 수 없는 조합**을 만들게 된다.
const SIZE_BY_MULT = { expansion_jewel_large: "Large", expansion_jewel_medium: "Medium", expansion_jewel_small: "Small" };
const notableOptions = {};
for (const mod of Object.values(dumped.clusterMods || {})) {
	// 노터블 모드는 **접두(affix "Notable")와 접미(예: "of Significance") 둘 다** 있다 —
	// affix 로 거르면 절반이 사라진다(묵직한 타격가가 접미라 통째로 빠졌었다). 문구로 판별한다.
	const line = mod[1] || mod["1"] || "";
	const name = /^1 Added Passive Skill is (.+)$/.exec(line)?.[1];
	if (!name) continue;
	const tags = (mod.weightKey || [])
		.map((key, i) => ((mod.weightVal || [])[i] > 0 && key !== "default" ? key : null))
		.filter(Boolean);
	const sizes = (mod.weightMultiplierKey || [])
		.map((key, i) => (SIZE_BY_MULT[key] && (mod.weightMultiplierVal || [])[i] > 0 ? SIZE_BY_MULT[key] : null))
		.filter(Boolean);
	notableOptions[name] = { tags, sizes };
}

// jewels: 크기별 정의(노드 수/인덱스 배치/스킬 풀). notableSortOrder 등 부가 테이블은 그대로 보존.
const out = {
	patch: process.env.POE_PATCH || null,
	jewels: parsed.jewels || {},
	notableSortOrder: parsed.notableSortOrder || null,
	// 프록시 노드별 궤도 시작 오프셋 — 이게 없으면 생성 노드의 궤도 인덱스를 맞출 수 없다
	// (PoB ApplyClusterOrbitIndexAdjustment: oidx = translate((oidx + startOidx) % totalIndicies)).
	orbitOffsets: parsed.orbitOffsets || {},
	// 노터블 → { tags: 붙을 수 있는 스킬 태그, sizes: 붙을 수 있는 주얼 크기 }
	notableOptions,
};

// ---- 작은 패시브 효과 문장 한글화 ----
// ClusterJewels.lua 는 영문 문장만 준다(스탯 id 가 없다). 그래서 **문장 템플릿 사전**을 만들어 갈아끼운다:
//  1) 게임 Stats 테이블 전체를 영/한으로 서술해 템플릿 쌍을 만들고(약 1.1만개)
//  2) 트리 노드/마스터리의 영·한 스탯 쌍으로 보충한다(서술기가 못 만드는 문장 커버).
// 숫자는 자리표시자(#)로 빼고, 클러스터 문장의 숫자를 순서대로 다시 끼운다.
const translator = createTemplateTranslator();
try {
	const describe = createStatDescriber(FILES_DIR, ["metadata@statdescriptions@passive_skill_stat_descriptions.txt"]);
	translator.addFromStats(describe, loadTable("English", "Stats"));
} catch (error) {
	console.warn("스탯 서술기 로드 실패 — 트리 문장만으로 한글화:", error.message);
}
try {
	const tree = JSON.parse(readFileSync(join(DATA_DIR, "passive-tree.json"), "utf8"));
	translator.addPairs((tree.nodes || []).map((node) => ({ en: node.stats || [], ko: node.statsKo || [] })));
	translator.addPairs(
		(tree.nodes || []).flatMap((node) => (node.masteryEffects || []).map((eff) => ({ en: eff.stats || [], ko: eff.statsKo || [] }))),
	);
} catch {
	// 트리 JSON 이 아직 없으면(첫 실행 순서) 서술기 템플릿만 쓴다
}
const toKorean = (line) => translator.translate(line);
let statTotal = 0;
let statKo = 0;
for (const def of Object.values(out.jewels)) {
	for (const skill of Object.values(def.skills || {})) {
		const lines = skill.stats || [];
		const translated = lines.map(toKorean);
		statTotal += lines.length;
		statKo += translated.filter(Boolean).length;
		// 한 줄이라도 못 옮기면 섞이지 않게 통째로 영문 유지(반쪽 한글이 더 헷갈린다)
		if (lines.length && translated.every(Boolean)) skill.statsKo = translated;
	}
}
console.log(`작은 패시브 문장 ${statTotal}개 중 한글 ${statKo}개 (${Math.round((statKo / statTotal) * 100)}%)`);

const sizes = Object.keys(out.jewels);
let skillTotal = 0;
for (const size of sizes) {
	const skills = Object.keys(out.jewels[size].skills || {});
	skillTotal += skills.length;
	console.log(
		`  ${size}: 노드 ${out.jewels[size].minNodes}~${out.jewels[size].maxNodes}` +
			` · 인덱스 ${out.jewels[size].totalIndicies}` +
			` · 스킬 ${skills.length}종`,
	);
}
const target = join(DATA_DIR, "cluster-jewels.json");
writeFileSync(target, JSON.stringify(out), "utf8");
console.log(`클러스터 주얼 ${sizes.length}종 / 스킬 ${skillTotal}개 → ${target}`);
