// 레어 아이템 크래프팅용 모드 풀 → mod-pool.json.
// 게임 데이터의 스폰웨이트 태그가 베이스에 1개만 저장되어 완전 매칭이 불가하므로,
// "어떤 모드 그룹이 어느 슬롯에 붙는가"는 큐레이션하고 "티어 값/문장"은 실제 Mods 데이터를 그대로 쓴다.
// 사용법: node parse-mods.mjs  (사전: extract.mjs 실행 완료)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

const OUT = path.join(DATA_DIR, "mod-pool.json");

const mods = loadTable("English", "Mods");
const stats = loadTable("English", "Stats");
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@passive_skill_stat_descriptions.txt",
]);

// 슬롯 카테고리: weaponAttack, weaponSpell, body, helmet, gloves, boots, amulet, ring, belt, shield, quiver
// 큐레이션 패밀리 — idPattern 은 `<Family><숫자>` 티어 사다리, gen=prefix|suffix
const FAMILIES = [
	// 방어 (접두)
	{ key: "life", gen: "prefix", pattern: "IncreasedLife", keywords: ["maximum life", "life"],
		slots: ["body", "helmet", "gloves", "boots", "belt", "amulet", "ring", "shield", "quiver"] },
	{ key: "esLocal", gen: "prefix", pattern: "LocalIncreasedEnergyShield", keywords: ["energy shield"],
		slots: ["body", "helmet", "gloves", "boots", "shield"] },
	{ key: "esGlobal", gen: "prefix", pattern: "IncreasedEnergyShield", keywords: ["energy shield"],
		slots: ["amulet", "ring"] },
	// 저항 (접미)
	{ key: "fireRes", gen: "suffix", pattern: "FireResist", keywords: ["fire resistance", "resistance"],
		slots: ["body", "helmet", "gloves", "boots", "belt", "amulet", "ring", "shield", "quiver"] },
	{ key: "coldRes", gen: "suffix", pattern: "ColdResist", keywords: ["cold resistance", "resistance"],
		slots: ["body", "helmet", "gloves", "boots", "belt", "amulet", "ring", "shield", "quiver"] },
	{ key: "lightRes", gen: "suffix", pattern: "LightningResist", keywords: ["lightning resistance", "resistance"],
		slots: ["body", "helmet", "gloves", "boots", "belt", "amulet", "ring", "shield", "quiver"] },
	{ key: "chaosRes", gen: "suffix", pattern: "ChaosResist", keywords: ["chaos resistance", "resistance"],
		slots: ["body", "gloves", "boots", "belt", "amulet", "ring"] },
	{ key: "allRes", gen: "suffix", pattern: "AllResistances", keywords: ["resistance", "all elemental"],
		slots: ["amulet", "ring", "shield", "belt"] },
	// 능력치 (접미)
	{ key: "str", gen: "suffix", pattern: "Strength", keywords: ["strength"],
		slots: ["body", "helmet", "gloves", "boots", "belt", "amulet", "ring"] },
	{ key: "dex", gen: "suffix", pattern: "Dexterity", keywords: ["dexterity"],
		slots: ["body", "helmet", "gloves", "boots", "belt", "amulet", "ring"] },
	{ key: "int", gen: "suffix", pattern: "Intelligence", keywords: ["intelligence"],
		slots: ["body", "helmet", "gloves", "boots", "belt", "amulet", "ring"] },
	// 주문 피해 (접두/접미)
	{ key: "spellDamage", gen: "prefix", pattern: "SpellDamage", keywords: ["spell", "damage"],
		slots: ["weaponSpell", "amulet", "shield"] },
	// 주문에 추가 원소 피해 (접두) — 레어 주문 무기의 핵심
	{ key: "spellAddedFire", gen: "prefix", pattern: "SpellAddedFireDamage", keywords: ["spell", "fire", "damage"],
		slots: ["weaponSpell"] },
	{ key: "spellAddedCold", gen: "prefix", pattern: "SpellAddedColdDamage", keywords: ["spell", "cold", "damage"],
		slots: ["weaponSpell"] },
	{ key: "spellAddedLight", gen: "prefix", pattern: "SpellAddedLightningDamage", keywords: ["spell", "lightning", "damage"],
		slots: ["weaponSpell"] },
	// 원소 피해 증가 (접두) — 장신구 (주문/공격 공통)
	{ key: "elementalDamage", gen: "prefix", pattern: "ElementalDamagePercent", keywords: ["fire", "cold", "lightning", "damage"],
		slots: ["amulet", "ring"] },
	// 공격 스킬 원소 피해 증가 (접두) — 공격 무기/전통
	{ key: "weaponEleDamage", gen: "prefix", pattern: "WeaponElementalDamage", keywords: ["attack", "fire", "cold", "lightning", "damage"],
		slots: ["weaponAttack", "quiver"] },
	// 치명타 배율 (접미) — 목걸이/무기
	{ key: "critMulti", gen: "suffix", pattern: "CriticalMultiplier", keywords: ["critical"],
		slots: ["amulet", "weaponSpell", "weaponAttack"] },
	{ key: "fireDmgPct", gen: "suffix", pattern: "FireDamagePercent", keywords: ["fire", "damage"],
		slots: ["weaponSpell", "amulet", "ring"] },
	{ key: "coldDmgPct", gen: "suffix", pattern: "ColdDamagePercent", keywords: ["cold", "damage"],
		slots: ["weaponSpell", "amulet", "ring"] },
	{ key: "lightDmgPct", gen: "suffix", pattern: "LightningDamagePercent", keywords: ["lightning", "damage"],
		slots: ["weaponSpell", "amulet", "ring"] },
	{ key: "castSpeed", gen: "suffix", pattern: "IncreasedCastSpeed", keywords: ["spell", "cast speed"],
		slots: ["weaponSpell", "amulet", "ring"] },
	// spellCrit(SpellCriticalStrikeChance) 는 스탯 설명이 보조젬 변형("Supported Skills have…")으로
	// 렌더돼 장비 모드로 부적합 → 제외. 치명은 critMulti 로 커버.
	// 공격 피해 (접두/접미)
	{ key: "addedPhys", gen: "prefix", pattern: "AddedPhysicalDamage", keywords: ["attack", "physical", "damage"],
		slots: ["weaponAttack", "gloves", "ring", "amulet", "quiver"] },
	{ key: "addedFire", gen: "prefix", pattern: "AddedFireDamage", keywords: ["attack", "fire", "damage"],
		slots: ["weaponAttack", "gloves", "ring", "amulet", "quiver"] },
	{ key: "addedCold", gen: "prefix", pattern: "AddedColdDamage", keywords: ["attack", "cold", "damage"],
		slots: ["weaponAttack", "gloves", "ring", "amulet", "quiver"] },
	{ key: "addedLight", gen: "prefix", pattern: "AddedLightningDamage", keywords: ["attack", "lightning", "damage"],
		slots: ["weaponAttack", "gloves", "ring", "amulet", "quiver"] },
	{ key: "attackSpeed", gen: "suffix", pattern: "IncreasedAttackSpeed", keywords: ["attack", "attack speed"],
		slots: ["weaponAttack", "gloves", "ring", "amulet", "quiver"] },
	{ key: "critChance", gen: "suffix", pattern: "CriticalStrikeChance", keywords: ["critical"],
		slots: ["amulet"] },
];

/** 모드 한 행의 스탯 → 최대 롤 값 맵 (StatsKey1..N + StatNMax) */
function maxRollValues(mod) {
	const values = new Map();
	for (let i = 1; i <= 6; i++) {
		const statIndex = mod["StatsKey" + i];
		if (statIndex == null) continue;
		const stat = stats[statIndex];
		if (!stat) continue;
		values.set(stat.Id, mod["Stat" + i + "Max"] ?? 0);
	}
	return values;
}

const families = [];
for (const family of FAMILIES) {
	const re = new RegExp("^" + family.pattern + "\\d+_?$");
	const tierMods = mods
		.filter((m) => m.Domain === 1 && re.test(m.Id || ""))
		.sort((a, b) => b.Level - a.Level); // best-first (높은 요구레벨 = 상위 티어)
	if (!tierMods.length) {
		console.warn("패밀리 티어 없음:", family.key);
		continue;
	}
	const tiers = tierMods.map((mod) => {
		const values = maxRollValues(mod);
		return {
			level: mod.Level,
			en: describe(values, "English"),
			ko: describe(values, "Korean"),
		};
	});
	families.push({
		key: family.key,
		gen: family.gen,
		slots: family.slots,
		keywords: family.keywords,
		tiers,
	});
}

const result = { patch: loadConfig().patch, families };
fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(`families ${families.length} → ${OUT}`);
console.log("sample:", families[0].key, families[0].tiers.length + "티어", JSON.stringify(families[0].tiers[0]));
