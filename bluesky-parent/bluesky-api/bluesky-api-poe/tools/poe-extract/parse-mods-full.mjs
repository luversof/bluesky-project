// 전체 크래프팅 모드 풀 → mods.json (poedb Modifiers 페이지식).
// 큐레이션(parse-mods.mjs → mod-pool.json, 최적화기용)과 별개로, 게임 Mods 테이블 전체를
// **스폰웨이트 태그 매칭**으로 아이템 클래스별 접두/접미 풀로 구성한다.
// 매칭 규칙(poedb/craftofexile 과 동일): 모드의 SpawnWeight_TagsKeys 를 앞에서부터 베이스의
// 태그 집합과 대조, 처음 매치된 항목의 weight 가 0 이면 불가·>0 이면 스폰 가능.
// 사용법: node parse-mods-full.mjs (사전: extract.mjs — run-all 에선 parse-mods 직후)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

const OUT = path.join(DATA_DIR, "mods.json");

const mods = loadTable("English", "Mods");
const modsKo = loadTable("Korean", "Mods");
const stats = loadTable("English", "Stats");
const tags = loadTable("English", "Tags");
const baseEn = loadTable("English", "BaseItemTypes");
const itemClassesEn = loadTable("English", "ItemClasses");
const itemClassesKo = loadTable("Korean", "ItemClasses");
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@stat_descriptions.txt",
]);

// 장비 아이템 클래스만 (base-items.json 에 있는 클래스 = 장비 파이프라인이 이미 거른 것)
const baseItems = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "base-items.json"), "utf8")).items;

// 클래스 레벨 태그 증강 — BaseItemTypes.TagsKeys 에는 속성 변형 태그(int_armour 등)만 담기고,
// 스폰웨이트가 참조하는 일반 슬롯/무기 태그(armour, gloves, weapon, bow, default…)는 ItemClass 에서
// 온다(게임 런타임 태그 상속). 이게 없으면 저항·공격속도 등이 방어구/무기에 안 붙는다(실측 버그).
const CLASS_TAGS = {
	Gloves: ["gloves", "armour"],
	Boots: ["boots", "armour"],
	"Body Armour": ["body_armour", "armour"],
	Helmet: ["helmet", "armour"],
	Shield: ["shield", "armour"],
	Ring: ["ring"],
	Amulet: ["amulet"],
	Belt: ["belt"],
	Quiver: ["quiver"],
	Claw: ["claw", "weapon", "one_hand_weapon", "onehand", "melee"],
	Dagger: ["dagger", "weapon", "one_hand_weapon", "onehand", "melee"],
	"Rune Dagger": ["rune_dagger", "dagger", "weapon", "one_hand_weapon", "onehand", "caster"],
	Wand: ["wand", "weapon", "one_hand_weapon", "onehand", "ranged", "caster"],
	"One Hand Sword": ["sword", "weapon", "one_hand_weapon", "onehand", "melee"],
	"Thrusting One Hand Sword": ["sword", "weapon", "one_hand_weapon", "onehand", "melee"],
	"One Hand Axe": ["axe", "weapon", "one_hand_weapon", "onehand", "melee"],
	"One Hand Mace": ["mace", "weapon", "one_hand_weapon", "onehand", "melee"],
	Sceptre: ["sceptre", "weapon", "one_hand_weapon", "onehand", "melee", "caster"],
	Bow: ["bow", "weapon", "two_hand_weapon", "twohand", "ranged"],
	Staff: ["staff", "weapon", "two_hand_weapon", "twohand", "melee"],
	Warstaff: ["warstaff", "staff", "weapon", "two_hand_weapon", "twohand", "melee"],
	"Two Hand Sword": ["sword", "weapon", "two_hand_weapon", "twohand", "melee"],
	"Two Hand Axe": ["axe", "weapon", "two_hand_weapon", "twohand", "melee"],
	"Two Hand Mace": ["mace", "weapon", "two_hand_weapon", "twohand", "melee"],
};
// 이 파이프라인이 모드 풀을 보여줄 대상 = 장비(무기/방어/장신구)만. 플라스크/주얼은 별도 모드 도메인이라 제외.
const wantedClasses = new Set(Object.keys(CLASS_TAGS));

// 베이스 이름 → BaseItemTypes 행 (base-items.json 과 게임 테이블 조인)
const baseRowByName = new Map();
for (const row of baseEn) if (row?.Name) baseRowByName.set(row.Name.toLowerCase(), row);

// 속성 변형 — 방어구는 str/dex/int(및 혼합) 베이스마다 **붙는 모드가 다르다**(예: 에너지 보호막은
// int_armour 에만, 방어도는 str_armour 에만). 클래스 단위로 합치면 한 장갑에 ES·방어도가 다 붙는 것처럼 보인다.
const VARIANT_LABEL = {
	str_armour: { name: "Strength", nameKo: "힘" },
	dex_armour: { name: "Dexterity", nameKo: "민첩" },
	int_armour: { name: "Intelligence", nameKo: "지능" },
	str_dex_armour: { name: "Str/Dex", nameKo: "힘/민첩" },
	str_int_armour: { name: "Str/Int", nameKo: "힘/지능" },
	dex_int_armour: { name: "Dex/Int", nameKo: "민첩/지능" },
	str_dex_int_armour: { name: "Str/Dex/Int", nameKo: "힘/민첩/지능" },
};

// 영향력(셰이퍼/엘더/정복자 4종) — 영향력 아이템에만 붙는 전용 접두/접미가 있다. 게임은 슬롯별 태그
// `<슬롯>_<영향력>`(gloves_shaper, wand_elder …)로 게이팅하므로, 그 태그를 태그 집합에 얹으면 그대로 잡힌다.
// 정복자 내부명: crusader=십자군, eyrie=구원자, basilisk=사냥꾼, adjudicator=전쟁군주.
const INFLUENCES = [
	{ key: "shaper", name: "Shaper", nameKo: "쉐이퍼" },
	{ key: "elder", name: "Elder", nameKo: "엘더" },
	{ key: "crusader", name: "Crusader", nameKo: "십자군" },
	{ key: "eyrie", name: "Redeemer", nameKo: "구원자" },
	{ key: "basilisk", name: "Hunter", nameKo: "사냥꾼" },
	{ key: "adjudicator", name: "Warlord", nameKo: "전쟁군주" },
];
// 영향력 태그가 붙는 슬롯 태그 — CLASS_TAGS 의 슬롯 태그(첫 항목)를 쓴다(gloves/bow/wand …).
const influenceSlotTag = (itemClass) => (CLASS_TAGS[itemClass] || [])[0];

// (아이템 클래스 × 속성 변형 × 영향력)별 태그 집합. 무기/장신구는 변형이 없어 변형 키 "", 영향력 없음도 "".
const tagSetsByPool = new Map(); // "itemClass|variant|influence" → [{sig,tagSet}]
const classLabel = new Map(); // itemClass명 → {name, nameKo}
const variantsByClass = new Map(); // itemClass명 → Map<variantKey, label>
for (const b of baseItems) {
	if (!wantedClasses.has(b.itemClass)) continue; // 장비만
	const row = baseRowByName.get((b.name || "").toLowerCase());
	if (!row) continue;
	const tagSet = new Set((row.TagsKeys || []).map((i) => tags[i]?.Id).filter(Boolean));
	tagSet.add("default"); // 모든 베이스의 암묵 태그
	for (const t of CLASS_TAGS[b.itemClass] || []) tagSet.add(t); // 클래스 레벨 일반 태그
	// 속성 변형(str_armour 등) → 일반 armour 파생
	const variantTag = [...tagSet].find((t) => VARIANT_LABEL[t]) || "";
	for (const t of [...tagSet]) if (t.endsWith("_armour")) tagSet.add("armour");

	if (!classLabel.has(b.itemClass)) {
		// base-items.json 의 itemClass 는 ItemClasses 의 **Id**(단수 "Amulet")를 쓴다 — Name(복수 "Amulets")으로
		// 조인하면 복수형이 다른 클래스(Amulet/Bow/Claw/Ring…)가 한글화에 실패한다. Id 로 조인한다.
		const clsIdx = itemClassesEn.findIndex((c) => c?.Id === b.itemClass);
		classLabel.set(b.itemClass, {
			name: b.itemClass,
			nameKo: clsIdx >= 0 ? itemClassesKo[clsIdx]?.Name || b.itemClass : b.itemClass,
		});
		variantsByClass.set(b.itemClass, new Map());
	}
	if (variantTag) variantsByClass.get(b.itemClass).set(variantTag, VARIANT_LABEL[variantTag]);

	// 영향력 없음("") + 영향력 6종 각각을 별도 풀로. 영향력 풀은 기본 태그에 `<슬롯>_<영향력>` 를 얹은 것이라
	// 일반 모드 + 그 영향력 전용 모드가 함께 잡힌다(게임에서도 영향력 아이템은 일반 모드도 그대로 받는다).
	const slotTag = influenceSlotTag(b.itemClass);
	for (const influence of ["", ...(slotTag ? INFLUENCES.map((i) => i.key) : [])]) {
		const finalTags = new Set(tagSet);
		if (influence) finalTags.add(slotTag + "_" + influence);
		const poolKey = b.itemClass + "|" + variantTag + "|" + influence;
		let list = tagSetsByPool.get(poolKey);
		if (!list) {
			list = [];
			tagSetsByPool.set(poolKey, list);
		}
		// 같은 태그 집합은 한 번만
		const sig = [...finalTags].sort().join(",");
		if (!list.some((e) => e.sig === sig)) list.push({ sig, tagSet: finalTags });
	}
}

/** 스폰웨이트 첫 매치 — 스폰 가능하면 weight(>0), 아니면 0 */
function spawnWeight(mod, tagSet) {
	const keys = mod.SpawnWeight_TagsKeys || [];
	const values = mod.SpawnWeight_Values || [];
	for (let i = 0; i < keys.length; i++) {
		const tagId = tags[keys[i]]?.Id;
		if (tagId && tagSet.has(tagId)) return values[i] ?? 0;
	}
	return 0;
}

/** 모드 스탯 → 값 맵 (min/max 선택) */
function rollValues(mod, kind) {
	const values = new Map();
	for (let i = 1; i <= 6; i++) {
		const statIndex = mod["StatsKey" + i];
		if (statIndex == null) continue;
		const stat = stats[statIndex];
		if (!stat) continue;
		values.set(stat.Id, mod["Stat" + i + (kind === "min" ? "Min" : "Max")] ?? 0);
	}
	return values;
}

// 패밀리 키: Id 의 꼬리 숫자를 벗긴 티어 사다리 이름 (IncreasedLife4 → IncreasedLife)
const familyKey = (mod) => (mod.Id || "").replace(/\d+_?$/, "") || mod.Id;

// 1) 장비 도메인 접두/접미 크래프팅 모드 수집 (Name 없는 내부 모드 제외)
const GEN = { 1: "prefix", 2: "suffix" };
const candidates = [];
mods.forEach((mod, index) => {
	if (mod.Domain !== 1 || !GEN[mod.GenerationType] || !mod.Name) return;
	if (!(mod.SpawnWeight_TagsKeys || []).length) return;
	candidates.push({ mod, index });
});

// 2) (클래스×변형) 풀별 매칭 + 패밀리 구성
const families = new Map(); // famKey → {gen, essence, tiers: Map<index, tier>}
const perPool = new Map(); // "itemClass|variant" → Map<famKey, weight>
for (const [poolKey, tagSets] of tagSetsByPool) {
	const famWeights = new Map();
	perPool.set(poolKey, famWeights);
	for (const { mod, index } of candidates) {
		// 같은 변형 안의 베이스들끼리는 태그가 같으므로 max 로 합쳐도 섞이지 않는다
		let weight = 0;
		for (const { tagSet } of tagSets) weight = Math.max(weight, spawnWeight(mod, tagSet));
		if (weight <= 0) continue;
		const key = familyKey(mod);
		famWeights.set(key, Math.max(famWeights.get(key) || 0, weight));
		let fam = families.get(key);
		if (!fam) {
			fam = { gen: GEN[mod.GenerationType], essence: !!mod.IsEssenceOnlyModifier, tiers: new Map() };
			families.set(key, fam);
		}
		if (!fam.tiers.has(index)) {
			fam.tiers.set(index, {
				id: mod.Id,
				name: mod.Name,
				nameKo: modsKo[index]?.Name || mod.Name,
				ilvl: mod.Level,
				weight: weight,
				en: describe(rollValues(mod, "max"), "English"),
				enMin: describe(rollValues(mod, "min"), "English"),
				ko: describe(rollValues(mod, "max"), "Korean"),
				koMin: describe(rollValues(mod, "min"), "Korean"),
			});
		}
	}
}

// 3) 직렬화 — 티어는 ilvl 내림차순(상위 티어 먼저)
const outFamilies = {};
for (const [key, fam] of families) {
	const tiers = [...fam.tiers.values()].sort((a, b) => b.ilvl - a.ilvl);
	// 스탯 서술이 아예 안 나온 패밀리(내부용/특수)는 페이지에 보여줄 게 없다 — 제외
	if (!tiers.some((t) => t.en?.length)) continue;
	outFamilies[key] = { gen: fam.gen, essence: fam.essence || undefined, tiers };
}
// 풀(클래스|변형) → 접두/접미 패밀리 키
const outPools = {};
for (const [poolKey, famWeights] of perPool) {
	outPools[poolKey] = {
		prefixes: [...famWeights.keys()].filter((k) => outFamilies[k]?.gen === "prefix").sort(),
		suffixes: [...famWeights.keys()].filter((k) => outFamilies[k]?.gen === "suffix").sort(),
	};
}
// 클래스 목록 — 변형이 여럿이면 variants 로 노출(UI 가 하위 탭을 만든다). 변형 없으면 빈 목록.
const VARIANT_ORDER = ["str_armour", "dex_armour", "int_armour", "str_dex_armour", "str_int_armour", "dex_int_armour", "str_dex_int_armour"];
const outClasses = [...classLabel.keys()]
	.map((itemClass) => {
		const variants = [...(variantsByClass.get(itemClass) || new Map()).entries()]
			.sort((a, b) => VARIANT_ORDER.indexOf(a[0]) - VARIANT_ORDER.indexOf(b[0]))
			.map(([key, label]) => {
				const pool = outPools[itemClass + "|" + key + "|"] || { prefixes: [], suffixes: [] };
				return { key, name: label.name, nameKo: label.nameKo, prefixCount: pool.prefixes.length, suffixCount: pool.suffixes.length };
			});
		// 클래스 칩에 쓰는 대표 풀 — 변형이 있는 클래스는 **첫 변형** 기준이어야 한다.
		// "변형 없음" 풀은 모든 베이스가 변형을 가진 클래스(갑옷 등)에선 비어 있어 칩이 0 으로 나왔다.
		const repVariant = variants.length ? variants[0].key : "";
		const base = outPools[itemClass + "|" + repVariant + "|"] || { prefixes: [], suffixes: [] };
		// 이 클래스에서 실제로 영향력 전용 모드가 더 붙는 영향력만 노출(없으면 UI 에 탭을 안 만든다)
		const firstVariant = variants.length ? variants[0].key : "";
		const baseKeys = new Set([
			...(outPools[itemClass + "|" + firstVariant + "|"]?.prefixes || []),
			...(outPools[itemClass + "|" + firstVariant + "|"]?.suffixes || []),
		]);
		const influences = INFLUENCES.map((inf) => {
			const pool = outPools[itemClass + "|" + firstVariant + "|" + inf.key];
			if (!pool) return null;
			const extra = [...pool.prefixes, ...pool.suffixes].filter((k) => !baseKeys.has(k)).length;
			return extra > 0 ? { key: inf.key, name: inf.name, nameKo: inf.nameKo, extraCount: extra } : null;
		}).filter(Boolean);
		return {
			itemClass,
			name: classLabel.get(itemClass)?.name || itemClass,
			nameKo: classLabel.get(itemClass)?.nameKo || itemClass,
			variants,
			influences,
			// 변형 없는 클래스(무기/장신구)의 기본 풀 — UI 개수 표시용
			prefixes: base.prefixes,
			suffixes: base.suffixes,
		};
	})
	.sort((a, b) => a.itemClass.localeCompare(b.itemClass));

const result = { patch: loadConfig().patch, itemClasses: outClasses, pools: outPools, families: outFamilies };
fs.writeFileSync(OUT, JSON.stringify(result));
const famCount = Object.keys(outFamilies).length;
const tierCount = Object.values(outFamilies).reduce((n, f) => n + f.tiers.length, 0);
console.log(`mods.json: 클래스 ${outClasses.length}개(풀 ${Object.keys(outPools).length}), 패밀리 ${famCount}개, 티어 ${tierCount}개 → ${OUT}`);
const infSample = outClasses.find((c) => c.influences?.length);
if (infSample) console.log(`영향력 예(${infSample.nameKo}):`, infSample.influences.map((i) => `${i.nameKo}+${i.extraCount}`).join(" · "));
const sample = outClasses.find((c) => c.itemClass === "Gloves");
if (sample) console.log("Gloves 변형:", sample.variants.map((v) => `${v.nameKo} 접두${v.prefixCount}/접미${v.suffixCount}`).join(" · "));
