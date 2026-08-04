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
	// 방어구 로컬 방어력/회피 — 무기와 같은 구멍이었다. 없으면 방어구 레어가 생명력/저항만 받아
	// "방어구인데 방어도가 안 오르는" 상태가 된다(실측: 방어도 2,100 → 3,360, EHP 28,139 → 29,190).
	// ⚠ Local*Rating 은 이름과 달리 flat(+500 방어도)이고 퍼센트 증가는 …Percent 접미 패턴이다 — 둘 다 필요.
	{ key: "armourLocal", gen: "prefix", pattern: "LocalIncreasedPhysicalDamageReductionRating", keywords: ["armour", "physical", "defence"],
		slots: ["body", "helmet", "gloves", "boots", "shield"] },
	{ key: "evasionLocal", gen: "prefix", pattern: "LocalIncreasedEvasionRating", keywords: ["evasion", "defence"],
		slots: ["body", "helmet", "gloves", "boots", "shield"] },
	{ key: "armourPctLocal", gen: "prefix", pattern: "LocalIncreasedPhysicalDamageReductionRatingPercent", keywords: ["armour", "physical", "defence"],
		slots: ["body", "helmet", "gloves", "boots", "shield"] },
	{ key: "evasionPctLocal", gen: "prefix", pattern: "LocalIncreasedEvasionRatingPercent", keywords: ["evasion", "defence"],
		slots: ["body", "helmet", "gloves", "boots", "shield"] },
	{ key: "esPctLocal", gen: "prefix", pattern: "LocalIncreasedEnergyShieldPercent", keywords: ["energy shield", "defence"],
		slots: ["body", "helmet", "gloves", "boots", "shield"] },
	// 방어구 로컬 방어력/회피 (접두) — 무기와 같은 구멍이었다. 이게 없으면 방어구 레어가 생명력/저항만 받아
	// "방어구인데 방어도가 안 오르는" 상태가 되고 EHP 목표가 크게 과소평가된다.
	// ⚠ Local*Rating 은 이름과 달리 flat(+500 방어도)이고, 퍼센트 증가는 …Percent 접미 패턴이다.
	// 엔드게임 방어구의 핵심은 이 퍼센트 쪽이라 둘 다 넣어야 실제 방어력이 나온다.
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
	// 전체 속성 — 단일 속성 슬롯이 소진됐을 때 속성 보정의 2차 수단(장신구에만 스폰)
	{ key: "allattr", gen: "suffix", pattern: "AllAttributes", keywords: ["attributes"],
		slots: ["amulet", "ring"] },
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
	// 지속 피해 배율 (접미) — RF/도트 빌드 무기의 핵심(실빌드 셉터 표준 +25% 급). 풀에 아예 없어서
	// 시뮬 레어가 실빌드 DPS 를 못 따라가던 구멍(RF 684k vs 실빌드 1.1M 수사에서 발견).
	// 합법성은 craftRare 의 canSpawn(베이스 클래스×변형) 판정에 맡긴다. 양손 전용 *TwoHand 패밀리는 별도(미편입).
	{ key: "dotMulti", gen: "suffix", pattern: "GlobalDamageOverTimeMultiplier", keywords: ["damage over time", "burning", "ignite", "poison", "bleed"],
		slots: ["weaponSpell", "weaponAttack"] },
	{ key: "fireDotMulti", gen: "suffix", pattern: "FireDamageOverTimeMultiplier", keywords: ["fire", "burning", "ignite", "damage over time"],
		slots: ["weaponSpell"] },
	{ key: "coldDotMulti", gen: "suffix", pattern: "ColdDamageOverTimeMultiplier", keywords: ["cold", "damage over time"],
		slots: ["weaponSpell"] },
	{ key: "chaosDotMulti", gen: "suffix", pattern: "ChaosDamageOverTimeMultiplier", keywords: ["chaos", "poison", "damage over time"],
		slots: ["weaponSpell"] },
	{ key: "physDotMulti", gen: "suffix", pattern: "PhysicalDamageOverTimeMultiplier", keywords: ["physical", "bleed", "damage over time"],
		slots: ["weaponAttack"] },
	// spellCrit(SpellCriticalStrikeChance) 는 스탯 설명이 보조젬 변형("Supported Skills have…")으로
	// 렌더돼 장비 모드로 부적합 → 제외. 치명은 critMulti 로 커버.
	// 공격 피해 (접두/접미)
	{ key: "addedPhys", gen: "prefix", pattern: "AddedPhysicalDamage", keywords: ["attack", "physical", "damage"],
		slots: ["gloves", "ring", "amulet", "quiver"] },
	{ key: "addedFire", gen: "prefix", pattern: "AddedFireDamage", keywords: ["attack", "fire", "damage"],
		slots: ["weaponAttack", "gloves", "ring", "amulet", "quiver"] },
	{ key: "addedCold", gen: "prefix", pattern: "AddedColdDamage", keywords: ["attack", "cold", "damage"],
		slots: ["weaponAttack", "gloves", "ring", "amulet", "quiver"] },
	{ key: "addedLight", gen: "prefix", pattern: "AddedLightningDamage", keywords: ["attack", "lightning", "damage"],
		slots: ["weaponAttack", "gloves", "ring", "amulet", "quiver"] },
	{ key: "attackSpeed", gen: "suffix", pattern: "IncreasedAttackSpeed", keywords: ["attack", "attack speed"],
		slots: ["gloves", "ring", "amulet", "quiver"] },
	{ key: "critChance", gen: "suffix", pattern: "CriticalStrikeChance", keywords: ["critical"],
		slots: ["amulet"] },
	// 주문 억제 — 현대 PoE 의 핵심 방어 모드(최대 100% 억제 시 주문 피해 절반). 없으면 EHP 가 구조적으로 과소평가된다.
	{ key: "spellSuppress", gen: "suffix", pattern: "ChanceToSuppressSpells", keywords: ["defence", "spell", "suppress"],
		slots: ["body", "helmet", "gloves", "boots", "shield"] },
	// 명중 — 공격 빌드는 명중이 없으면 빗나가서 DPS 가 무의미해진다(무기에 하드코딩해 둔 가정을 데이터로 대체 가능하게).
	{ key: "accuracyLocal", gen: "suffix", pattern: "LocalIncreasedAccuracyNew", keywords: ["attack", "accuracy"],
		slots: ["weaponAttack"] },
	{ key: "accuracy", gen: "suffix", pattern: "IncreasedAccuracyNew", keywords: ["attack", "accuracy"],
		slots: ["gloves", "ring", "amulet", "quiver"] },
	// 무기 로컬 모드 — 무기의 주력 모드는 전역(장신구용) 모드가 아니라 Local* 계열이다.
	// 이게 빠져 있으면 크래프트 무기가 장신구 티어 수치(물리 15-26 등)만 받아 표준 무기조차 못 이긴다.
	{ key: "localPhysPct", gen: "prefix", pattern: "LocalIncreasedPhysicalDamagePercent", keywords: ["attack", "physical", "damage"],
		slots: ["weaponAttack"] },
	{ key: "localAddedPhys", gen: "prefix", pattern: "LocalAddedPhysicalDamage", keywords: ["attack", "physical", "damage"],
		slots: ["weaponAttack"] },
	{ key: "localAttackSpeed", gen: "suffix", pattern: "LocalIncreasedAttackSpeed", keywords: ["attack", "attack speed"],
		slots: ["weaponAttack"] },
	{ key: "localCritChance", gen: "suffix", pattern: "LocalCriticalStrikeChance", keywords: ["critical"],
		slots: ["weaponAttack"] },
	// #3 정의의 화염류(RF) 핵심 크래프트 모드 — 반드시 **맨 뒤**에 추가한다.
	// score(family.keywords, skillKeywords) 는 skillKeyword 가 family 줄의 부분문자열이면 +1 이므로,
	// 줄에 "damage" 를 넣으면 모든 스킬이 가진 만능 키워드 "damage" 에 걸려 히트 빌드 후보를 오염시킨다.
	// 그래서 줄을 fire/burning/life/regen 으로만 두어 RF 게이트(#4 burning, #2 life·regen)에서만 매칭되게 한다.
	// FireDamageOverTimeMultiplier=화염 지속피해 다중(접미, 목걸이). LifeRegeneration=생명 재생(접미, 방어구·장신구).
	// 히트/EHP 빌드는 fire·burning·life·regen 키워드를 안 받아 점수 0 → 미채택(사이클론/아크/ED/EHP 기준선 불변).
	{ key: "fireDotMulti", gen: "suffix", pattern: "FireDamageOverTimeMultiplier", keywords: ["fire", "burning"],
		slots: ["amulet"] },
	{ key: "lifeRegen", gen: "suffix", pattern: "LifeRegeneration", keywords: ["life", "regen"],
		slots: ["body", "helmet", "gloves", "boots", "belt", "amulet", "ring", "shield"] },
	// 소켓 시너지(엘더 헬멧) — "화염 피해 35% 증가 + 장착된 젬에 20레벨 화상 피해 보조 효과 적용"(of the Elder).
	// 실빌드 RF 의 표준 헬멧이자 시뮬 DPS 갭(713k vs 2.58M)의 최대 요인. 소켓 지원은 시뮬레이터가
	// 메인 링크에 Burning Damage 젬을 명시 추가하는 방식으로 모델링한다(키 elder* = 영향력 스폰 판정).
	// 키워드는 fireDotMulti 와 같은 fire/burning 게이트 — 히트 빌드(아크/사이클론/ED)는 점수 0 → 미채택.
	{ key: "elderBurningSupport", gen: "suffix", pattern: "IncreasedBurningDamageSupportedUber", keywords: ["fire", "burning"],
		slots: ["helmet"] },
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
		// 게임 모드 Id 패턴 = 전체 풀(mods.json)의 패밀리 키. 시뮬레이터가 "이 베이스 변형에 실제로 붙는
		// 모드인가"를 하드코딩이 아니라 게임 데이터로 판정하는 데 쓴다.
		pattern: family.pattern,
		tiers,
	});
}

const result = { patch: loadConfig().patch, families };
fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(`families ${families.length} → ${OUT}`);
console.log("sample:", families[0].key, families[0].tiers.length + "티어", JSON.stringify(families[0].tiers[0]));
