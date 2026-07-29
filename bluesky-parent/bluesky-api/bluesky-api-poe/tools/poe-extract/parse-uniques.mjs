// PoB(Path of Building, MIT) 고유 아이템 데이터 → 표시용 JSON.
// 영어 모드 텍스트는 PoB 원문을 쓰고, 아이템 이름/베이스의 한국어는 게임 데이터(Words.Text2, BaseItemTypes)로 결합한다.
// 사용법: node parse-uniques.mjs  (사전 조건: pob-uniques/*.lua 다운로드, tables/ 추출 완료)
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { DATA_DIR, FILES_DIR, POB_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createModTranslator } from "./statDescriptions.mjs";

// 영어 모드 배열 → 한국어 배열 역번역 (멀티라인 모드 결합, 매칭 실패 라인은 영어 유지)
const toKo = createModTranslator(FILES_DIR, [
	"metadata@statdescriptions@passive_skill_stat_descriptions.txt",
]);

const PATCH = loadConfig().patch;
const OUT = path.join(DATA_DIR, "unique-items.json");

const load = loadTable;

// 한국어 이름 사전: Words(고유 이름) + BaseItemTypes(베이스 이름)
const wordsEn = load("English", "Words");
const wordsKo = load("Korean", "Words");
const nameKoByEn = new Map();
wordsEn.forEach((w, i) => {
	if (w.Text) nameKoByEn.set(w.Text, wordsKo[i]?.Text2 || null);
});
// KR 클라이언트 Words 테이블에 번역이 없는 레거시/제거 고유템의 한글명 폴백(영문명 기준, best-effort 커뮤니티 표준)
const nameKoOverrides = (() => {
	try {
		const raw = JSON.parse(fs.readFileSync(path.join(import.meta.dirname, "unique-nameko-overrides.json"), "utf8"));
		delete raw._comment;
		return raw;
	} catch {
		return {};
	}
})();
const baseEn = load("English", "BaseItemTypes");
const baseKo = load("Korean", "BaseItemTypes");
const baseKoByEn = new Map();
baseEn.forEach((b, i) => {
	if (b.Name) baseKoByEn.set(b.Name, baseKo[i]?.Name || null);
});

// 블록 메타데이터로 취급하는 접두사 (모드 라인이 아님)
const META_PREFIXES = [
	"Variant:", "League:", "Source:", "Upgrade:", "Selected Variant:", "Selected Alt Variant",
	"Has Alt Variant", "Talisman Tier:", "Requires ", "LevelReq:", "Implicits:",
	"Elder Item", "Shaper Item", "Crusader Item", "Redeemer Item", "Hunter Item", "Warlord Item",
	"Sockets:", "Limited to:", "Radius:", "Grants Skill:", "Cluster Jewel",
];

function parseBlock(block, category) {
	const lines = block.split("\n").map((l) => l.trim()).filter((l) => l.length);
	if (lines.length < 2) return null;
	const name = lines[0];
	const baseType = lines[1];

	let variantCount = 0;
	let requiredLevel = null;
	let league = null;
	let radius = null; // 반경 주얼("…in Radius")은 이 라벨이 없으면 PoB 가 반경 모드를 **조용히 무시**한다
	let implicitCount = 0;
	const modSection = []; // 메타 이후의 원시 라인들 (implicit 구분 전)

	for (const line of lines.slice(2)) {
		if (line.startsWith("Variant:")) { variantCount++; continue; }
		if (line.startsWith("League:")) { league = line.slice(7).trim(); continue; }
		if (line.startsWith("Radius:")) { radius = line.slice(7).trim(); continue; }
		if (line.startsWith("LevelReq:")) { requiredLevel = Number(line.slice(9).trim()) || null; continue; }
		if (line.startsWith("Requires Level")) {
			const m = line.match(/Requires Level (\d+)/);
			if (m) requiredLevel = Number(m[1]);
			continue;
		}
		if (line.startsWith("Implicits:")) { implicitCount = Number(line.slice(10).trim()) || 0; modSection.length = 0; continue; }
		if (META_PREFIXES.some((p) => line.startsWith(p))) continue;
		modSection.push(line);
	}

	const currentVariant = variantCount > 0 ? variantCount : null;

	// {variant:...} 필터 + {tags:...}/{range:...} 등 마크업 제거
	function cleanLine(raw) {
		const variantMatch = raw.match(/\{variant:([\d,]+)\}/);
		if (variantMatch && currentVariant != null) {
			const variants = variantMatch[1].split(",").map(Number);
			if (!variants.includes(currentVariant)) return null;
		}
		const text = raw.replace(/\{[^}]*\}/g, "").trim();
		return text.length ? text : null;
	}

	// implicitCount 는 파일 원문 라인 기준이므로 필터 전에 자른다
	const implicitRaw = modSection.slice(0, implicitCount);
	const explicitRaw = modSection.slice(implicitCount);
	const implicits = implicitRaw.map(cleanLine).filter(Boolean);
	const explicits = explicitRaw.map(cleanLine).filter(Boolean);

	return {
		name,
		nameKo: nameKoByEn.get(name) || nameKoOverrides[name] || null,
		slug: name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, ""),
		baseType,
		baseTypeKo: baseKoByEn.get(baseType) || null,
		category,
		requiredLevel,
		league,
		radius,
		implicits,
		implicitsKo: toKo(implicits),
		explicits,
		explicitsKo: toKo(explicits),
	};
}

// PoB 데이터가 없으면 GitHub 에서 받아온다 (부위별 lua)
const POB_FILES = ["amulet","axe","belt","body","boots","bow","claw","dagger","fishing","flask","gloves","helmet","jewel","mace","quiver","ring","shield","staff","sword","wand","tincture"];
fs.mkdirSync(POB_DIR, { recursive: true });
for (const name of POB_FILES) {
	const target = path.join(POB_DIR, name + ".lua");
	if (fs.existsSync(target)) continue;
	const url = `https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding/master/src/Data/Uniques/${name}.lua`;
	const response = await fetch(url);
	if (!response.ok) throw new Error(`PoB 다운로드 실패: ${url} (${response.status})`);
	fs.writeFileSync(target, await response.text());
	console.log("다운로드:", name + ".lua");
}

const items = [];
for (const file of fs.readdirSync(POB_DIR)) {
	if (!file.endsWith(".lua")) continue;
	const category = file.replace(".lua", "");
	const source = fs.readFileSync(path.join(POB_DIR, file), "utf8");
	for (const match of source.matchAll(/\[\[([\s\S]*?)\]\]/g)) {
		const item = parseBlock(match[1], category);
		if (item) items.push(item);
	}
}

// slug 중복 시 카테고리 접미사로 해소
const seen = new Map();
for (const item of items) {
	if (seen.has(item.slug)) item.slug = item.slug + "-" + item.category;
	seen.set(item.slug, true);
}

items.sort((a, b) => a.name.localeCompare(b.name));
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, JSON.stringify({ patch: PATCH, items }, null, 1));
const koCount = items.filter((i) => i.nameKo).length;
console.log(`${items.length} uniques → ${OUT} (한국어 이름 ${koCount})`);
console.log("sample:", JSON.stringify(items.find((i) => i.name === "The Anvil")));
