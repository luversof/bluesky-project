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
// PoB 원문이 롤 범위를 고정값으로 단순화한 라인 보정(빛나는 묘약 "(1-2)초" 등 — 거래소 가변 옵션 판정용)
const lineOverrides = (() => {
	try {
		const raw = JSON.parse(fs.readFileSync(path.join(import.meta.dirname, "unique-line-overrides.json"), "utf8"));
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

// 변형 라벨 한글화 — PoB 라벨은 영어 자유 문구라 완역이 불가능하다. 그래서 **확실한 것만** 옮기고
//   못 옮기면 null 로 두어 화면이 영어 원문으로 폴백하게 한다(어설픈 반쪽 한글보다 낫다).
//   확실한 것 = 젬/베이스 이름 전체 일치, 또는 아래 사전에 있는 낱말. 구분자(":", "/", " and ")로 쪼개 각각 시도한다.
const gemKoByEn = (() => {
	const map = new Map();
	try {
		const gems = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "skill-gems.json"), "utf8")).gems || [];
		for (const gem of gems) if (gem.name && gem.nameKo) map.set(gem.name, gem.nameKo);
	} catch { /* 젬 산출물이 아직 없으면 사전 없이 진행 */ }
	return map;
})();
const VARIANT_WORD_KO = {
	"Physical": "물리", "Fire": "화염", "Cold": "냉기", "Lightning": "번개", "Chaos": "카오스",
	"Elemental": "원소", "Attributes": "속성", "Strength": "힘", "Dexterity": "민첩", "Intelligence": "지능",
	"Life": "생명력", "Mana": "마나", "Energy Shield": "에너지 보호막", "Armour": "방어도", "Evasion": "회피",
	"Evasion Rating": "회피", "Accuracy Rating": "정확도", "Attack Speed": "공격 속도", "Cast Speed": "시전 속도",
	"Damage": "피해", "Spell Damage": "주문 피해", "Attack Damage": "공격 피해", "Area of Effect": "효과 범위",
	"Item Rarity": "아이템 희귀도", "Item Quantity": "아이템 수량", "Movement Speed": "이동 속도",
	"Chaos Resistance": "카오스 저항", "Fire Resistance": "화염 저항", "Cold Resistance": "냉기 저항",
	"Lightning Resistance": "번개 저항", "Elemental Resistances": "원소 저항", "Max Resistance": "최대 저항",
	"Life Regen": "생명력 재생", "Mana Regen": "마나 재생", "Crit Chance": "치명타 확률",
	"Crit Multi": "치명타 피해", "Crit Multiplier": "치명타 피해", "Buff Effect": "버프 효과",
	"Aura Effect": "오라 효과", "Skill Reservation": "스킬 점유", "Global Crit Chance": "전역 치명타 확률",
	"Small Ring": "작은 고리", "Medium Ring": "중간 고리", "Large Ring": "큰 고리",
	"Very Large Ring": "매우 큰 고리", "Massive Ring": "거대한 고리",
	"Scorch": "이글거림", "Brittle": "취약", "Sap": "쇠약",
	"One Abyssal Socket": "심연 홈 1개", "Two Abyssal Sockets": "심연 홈 2개", "Three Abyssal Sockets": "심연 홈 3개",
};
function variantWordKo(word) {
	const trimmed = word.trim();
	if (!trimmed) return null;
	return VARIANT_WORD_KO[trimmed] || gemKoByEn.get(trimmed) || baseKoByEn.get(trimmed) || null;
}
function variantNameKo(_itemName, label) {
	// 괄호 부기는 통째로 다시 태워 본다: "Two-Toned Boots (Armour/Evasion)"
	const paren = label.match(/^(.*?)\s*\((.+)\)$/);
	if (paren) {
		const head = variantNameKo(_itemName, paren[1]);
		const tail = variantNameKo(_itemName, paren[2]);
		return head && tail ? `${head} (${tail})` : null;
	}
	for (const [sep, join] of [[": ", ": "], ["/", "/"], [" and ", " · "]]) {
		if (label.includes(sep)) {
			const parts = label.split(sep).map(variantWordKo);
			return parts.every(Boolean) ? parts.join(join) : null;
		}
	}
	return variantWordKo(label);
}

// 변형(Variant) 이름 중 **지금 게임에 없는 것**(과거 버전 보존용)을 걸러낸다.
//   PoB 는 옛 롤을 계속 들고 있어서(예: "Pre 3.21.0", "One Abyssal Socket (Pre 3.12.0)")
//   그대로 보여주면 인게임에 없는 선택지가 목록에 섞인다. 인게임 정합이 우선이라 과거분은 감춘다.
const HISTORICAL_VARIANT = /\bPre[ -]\d/i;
const isHistoricalVariant = (name) => name === "Current" || HISTORICAL_VARIANT.test(name);
// "Fire and Chaos Resistances (Current)" → "Fire and Chaos Resistances" (현재분 표식은 라벨에서 군더더기)
const normalizeVariantName = (name) => name.replace(/\s*\(Current\)\s*$/i, "").replace(/\s+Current$/i, "").trim();

function parseBlock(block, category) {
	const lines = block.split("\n").map((l) => l.trim()).filter((l) => l.length);
	if (lines.length < 2) return null;
	const name = lines[0];
	const baseType = lines[1];

	const variantNames = [];
	let selectedVariant = null;
	let requiredLevel = null;
	let league = null;
	let radius = null; // 반경 주얼("…in Radius")은 이 라벨이 없으면 PoB 가 반경 모드를 **조용히 무시**한다
	let implicitCount = 0;
	const modSection = []; // 메타 이후의 원시 라인들 (implicit 구분 전)

	for (const line of lines.slice(2)) {
		if (line.startsWith("Variant:")) { variantNames.push(line.slice(8).trim()); continue; }
		if (line.startsWith("Selected Variant:")) { selectedVariant = Number(line.slice(17).trim()) || null; continue; }
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

	const variantCount = variantNames.length;
	// 지금 게임에 존재하는 변형만 (임프레션스 물리/화염/…, 도리아니의 망상 9종 등)
	const liveVariants = variantNames
		.map((vName, i) => ({ index: i + 1, name: normalizeVariantName(vName) }))
		.filter((v, i) => !isHistoricalVariant(variantNames[i]));

	// 기본으로 보여줄 변형. PoB 규칙(Selected Variant 우선, 없으면 마지막)을 따르되,
	// 그 기본이 과거분이면 **현재분 중 마지막**으로 당긴다 — 아니면 인게임에 없는 롤이 대표로 나간다.
	let defaultVariant = selectedVariant && selectedVariant <= variantCount ? selectedVariant : (variantCount || null);
	if (defaultVariant != null && isHistoricalVariant(variantNames[defaultVariant - 1]) && liveVariants.length) {
		defaultVariant = liveVariants[liveVariants.length - 1].index;
	}

	// {variant:...} 필터 + {tags:...}/{range:...} 등 마크업 제거
	function cleanLine(raw, variant) {
		const variantMatch = raw.match(/\{variant:([\d,]+)\}/);
		if (variantMatch && variant != null) {
			const variants = variantMatch[1].split(",").map(Number);
			if (!variants.includes(variant)) return null;
		}
		const text = raw.replace(/\{[^}]*\}/g, "").trim();
		return text.length ? text : null;
	}

	// implicitCount 는 파일 원문 라인 기준이므로 필터 전에 자른다
	const implicitRaw = modSection.slice(0, implicitCount);
	const explicitRaw = modSection.slice(implicitCount);
	const overrides = lineOverrides[name] || {};
	const applyOverride = (line) => overrides[line] || line;
	const linesFor = (variant) => ({
		implicits: implicitRaw.map((l) => cleanLine(l, variant)).filter(Boolean).map(applyOverride),
		explicits: explicitRaw.map((l) => cleanLine(l, variant)).filter(Boolean).map(applyOverride),
	});

	const { implicits, explicits } = linesFor(defaultVariant);

	// 변형이 여럿일 때만 전수 보존한다. 하나뿐이면(대개 "Pre x.y / Current") 기본분이 곧 전부라 잡음만 는다.
	const variants = liveVariants.length > 1
		? liveVariants.map((v) => {
			const set = linesFor(v.index);
			return {
				index: v.index,
				name: v.name,
				nameKo: variantNameKo(name, v.name),
				implicits: set.implicits,
				implicitsKo: toKo(set.implicits),
				explicits: set.explicits,
				explicitsKo: toKo(set.explicits),
			};
		})
		: null;

	return {
		variants,
		defaultVariant: variants ? defaultVariant : null,
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
