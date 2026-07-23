// 노터블 도유(성유) 레시피: 인게임 노드 툴팁 헤더에 뜨는 기름 3개(아뮬렛 도유법)를 추출한다.
// BlightCraftingRecipes → BlightCraftingItems(.Oil → BaseItemTypes) + BlightCraftingResults(.PassiveSkill
// → PassiveSkills.PassiveSkillGraphId = 트리 노드 id). passive-tree.json 노드에 anoint(오일 slug 배열)를
// 덧붙이고 top-level oils 사전 + 오일 아이콘(icons/oils/)을 emit.
// 사용법: node parse-anoints.mjs (사전: parse-tree.mjs — run-all 에서는 parse-tree 직후)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, loadTable, runExtractor } from "./paths.mjs";

const treeFile = path.join(DATA_DIR, "passive-tree.json");
if (!fs.existsSync(treeFile)) {
	console.warn("passive-tree.json 없음 — parse-tree.mjs 먼저. 건너뜀");
	process.exit(0);
}

// 1) 필요한 테이블 추출 (BaseItemTypes/ItemVisualIdentity 는 오일 이름·아이콘 조인용으로 함께)
const config = loadConfig();
config.files = [];
config.tables = [
	{ name: "BlightCraftingRecipes", columns: ["Id", "BlightCraftingItemsKeys", "BlightCraftingResultsKey"] },
	{ name: "BlightCraftingResults", columns: ["Id", "PassiveSkill"] },
	{ name: "BlightCraftingItems", columns: ["Oil", "Tier"] },
	{ name: "BaseItemTypes", columns: ["Id", "Name", "ItemVisualIdentity"] },
	{ name: "ItemVisualIdentity", columns: ["Id", "DDSFile"] },
	{ name: "PassiveSkills", columns: ["Id", "PassiveSkillGraphId"] },
];
runExtractor(config);

const recipes = loadTable("English", "BlightCraftingRecipes");
const results = loadTable("English", "BlightCraftingResults");
const craftItems = loadTable("English", "BlightCraftingItems");
const baseEn = loadTable("English", "BaseItemTypes");
const baseKo = loadTable("Korean", "BaseItemTypes");
const visual = loadTable("English", "ItemVisualIdentity");
const passives = loadTable("English", "PassiveSkills");

const oilSlug = (baseRow) => baseRow.Id.split("/").pop().toLowerCase();

// 2) 레시피 → 노드 id + 오일 목록
const anointByNode = new Map(); // nodeId → [slug, slug, slug]
const oils = {}; // slug → {name, nameKo, icon, tier}
let matched = 0;
for (const recipe of recipes) {
	const result = results[recipe.BlightCraftingResultsKey];
	if (!result || result.PassiveSkill == null) continue;
	const passive = passives[result.PassiveSkill];
	if (!passive || passive.PassiveSkillGraphId == null) continue;
	const parts = (recipe.BlightCraftingItemsKeys || []).map((k) => craftItems[k]).filter(Boolean);
	if (!parts.length) continue;
	const slugs = [];
	for (const part of parts) {
		const base = part.Oil != null ? baseEn[part.Oil] : null;
		if (!base) continue;
		const slug = oilSlug(base);
		slugs.push(slug);
		if (!oils[slug]) {
			oils[slug] = {
				name: base.Name,
				nameKo: baseKo[part.Oil]?.Name || base.Name,
				icon: "oils/" + slug + ".png",
				tier: part.Tier ?? 0,
			};
		}
	}
	if (slugs.length) {
		// 인게임 표기처럼 낮은 티어 → 높은 티어 순
		slugs.sort((a, b) => (oils[a]?.tier ?? 0) - (oils[b]?.tier ?? 0));
		anointByNode.set(passive.PassiveSkillGraphId, slugs);
		matched++;
	}
}
console.log(`도유 레시피: ${matched}개 노드, 오일 ${Object.keys(oils).length}종`);

// 3) 오일 아이콘 추출 (item-icons 패턴: ItemVisualIdentity → DDS → PNG)
const magickDir = findImageMagick();
const iconDir = path.join(DATA_DIR, "icons", "oils");
if (magickDir) {
	const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");
	const ddsBySlug = new Map();
	for (const row of baseEn) {
		if (!row?.Id) continue;
		const slug = row.Id.split("/").pop().toLowerCase();
		if (!oils[slug]) continue;
		const visualRow = row.ItemVisualIdentity != null ? visual[row.ItemVisualIdentity] : null;
		if (visualRow?.DDSFile) ddsBySlug.set(slug, visualRow.DDSFile.toLowerCase());
	}
	const dl = loadConfig();
	dl.tables = [];
	dl.files = [...new Set(ddsBySlug.values())];
	runExtractor(dl);
	fs.mkdirSync(iconDir, { recursive: true });
	let done = 0;
	for (const [slug, dds] of ddsBySlug) {
		const sheet = path.join(FILES_DIR, dds.replace(/\//g, "@").replace(/\.dds$/, ".png"));
		if (!fs.existsSync(sheet)) continue;
		execFileSync(MAGICK, [sheet, "-resize", "78x78", path.join(iconDir, slug + ".png")]);
		done++;
	}
	console.log(`오일 아이콘: ${done}/${ddsBySlug.size}`);
} else {
	console.warn("ImageMagick 없음 — 오일 아이콘 생략(이름만 표시됨)");
}

// 4) passive-tree.json 에 주입
const tree = JSON.parse(fs.readFileSync(treeFile, "utf8"));
let applied = 0;
for (const node of tree.nodes) {
	const slugs = anointByNode.get(node.id);
	if (slugs) {
		node.anoint = slugs;
		applied++;
	}
}
tree.oils = oils;
fs.writeFileSync(treeFile, JSON.stringify(tree));
console.log(`parse-anoints 완료: 노드 ${applied}개에 도유 주입`);
