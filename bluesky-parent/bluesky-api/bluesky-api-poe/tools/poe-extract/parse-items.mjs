// 일반(베이스) 아이템 → 표시용 JSON. 장비 클래스만 대상 (시뮬레이터의 아이템 슬롯 탐색 기반 데이터).
// 방어구 수치(ArmourTypes)/무기 수치(WeaponTypes)/요구 능력치/암시 모드(Mods+스탯 파서, 한/영)를 결합한다.
// 사용법: node parse-items.mjs
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

const PATCH = loadConfig().patch;
const OUT = path.join(DATA_DIR, "base-items.json");

// 장비 클래스 화이트리스트 (ItemClasses.Id) → 광역 카테고리
const EQUIPMENT_CLASSES = {
	"One Hand Sword": "weapon", "Thrusting One Hand Sword": "weapon", "One Hand Axe": "weapon",
	"One Hand Mace": "weapon", "Sceptre": "weapon", "Claw": "weapon", "Dagger": "weapon",
	"Rune Dagger": "weapon", "Wand": "weapon", "Bow": "weapon", "Staff": "weapon",
	"Warstaff": "weapon", "Two Hand Sword": "weapon", "Two Hand Axe": "weapon", "Two Hand Mace": "weapon",
	"Body Armour": "armour", "Helmet": "armour", "Gloves": "armour", "Boots": "armour", "Shield": "armour",
	"Amulet": "accessory", "Ring": "accessory", "Belt": "accessory", "Quiver": "accessory",
	"LifeFlask": "flask", "ManaFlask": "flask", "HybridFlask": "flask", "UtilityFlask": "flask",
	"Jewel": "jewel", "AbyssJewel": "jewel",
};

const en = {
	base: loadTable("English", "BaseItemTypes"),
	classes: loadTable("English", "ItemClasses"),
	armour: loadTable("English", "ArmourTypes"),
	weapon: loadTable("English", "WeaponTypes"),
	requirements: loadTable("English", "ComponentAttributeRequirements"),
	mods: loadTable("English", "Mods"),
	stats: loadTable("English", "Stats"),
	flasks: loadTable("English", "Flasks"),
	charges: loadTable("English", "ComponentCharges"),
	buffs: loadTable("English", "BuffDefinitions"),
	shields: loadTable("English", "ShieldTypes"),
};
const ko = {
	base: loadTable("Korean", "BaseItemTypes"),
	classes: loadTable("Korean", "ItemClasses"),
};

const describe = createStatDescriber(FILES_DIR);

const armourByBase = new Map(en.armour.map((row) => [row.BaseItemTypesKey, row]));
// 방패 기본 막기 확률 (ShieldTypes.BaseItemTypesKey = 행 인덱스, Block = 정수 %)
const shieldByBase = new Map(en.shields.map((row) => [row.BaseItemTypesKey, row]));
const weaponByBase = new Map(en.weapon.map((row) => [row.BaseItemTypesKey, row]));
// ComponentAttributeRequirements 는 행 인덱스가 아니라 BaseItemTypes.Id 문자열로 참조한다
const requirementsByBase = new Map(en.requirements.map((row) => [row.BaseItemTypesKey, row]));
// 플라스크 회복/지속 (Flasks.BaseItemTypesKey = 행 인덱스) + 충전 (ComponentCharges.BaseItemTypesKey = Id 문자열)
const flaskByBase = new Map(en.flasks.map((row) => [row.BaseItemTypesKey, row]));
const chargesByBase = new Map(en.charges.map((row) => [row.BaseItemTypesKey, row]));

// 모드 → (한/영) 표시 문장. 값이 범위(min≠max)면 숫자 자리만 "min-max" 로 병합한다.
function describeModRange(mod, lang) {
	const buildValues = (kind) => {
		const values = new Map();
		for (let statPosition = 1; statPosition <= 4; statPosition++) {
			const statIndex = mod["StatsKey" + statPosition];
			if (statIndex == null) continue;
			values.set(en.stats[statIndex].Id, mod["Stat" + statPosition + (kind === "min" ? "Min" : "Max")] ?? 0);
		}
		return values;
	};
	const minLines = describe(buildValues("min"), lang);
	const maxLines = describe(buildValues("max"), lang);
	return minLines.map((minLine, i) => {
		const maxLine = maxLines[i];
		if (!maxLine || minLine === maxLine) return minLine;
		const numberPattern = /-?\d+(?:\.\d+)?/g;
		const minSkeleton = minLine.replace(numberPattern, "#");
		if (minSkeleton !== maxLine.replace(numberPattern, "#")) return minLine; // 형태가 다르면 병합 불가 — min 기준
		const maxNumbers = maxLine.match(numberPattern) || [];
		let numberIndex = 0;
		return minLine.replace(numberPattern, (minNumber) => {
			const maxNumber = maxNumbers[numberIndex++];
			return minNumber === maxNumber ? minNumber : "(" + minNumber + "-" + maxNumber + ")";
		});
	});
}

const junkName = /\bDNT\b|\[UNUSED\]|^[. ]+$/i;
const items = [];
for (const base of en.base) {
	const itemClass = en.classes[base.ItemClassesKey];
	if (!itemClass) continue;
	const category = EQUIPMENT_CLASSES[itemClass.Id];
	if (!category) continue;
	if (!base.Name || junkName.test(base.Name)) continue;
	if (base.Id.includes("Royale") || base.Id.includes("Talisman")) continue;

	const armour = armourByBase.get(base._index);
	const weapon = weaponByBase.get(base._index);
	const requirement = requirementsByBase.get(base.Id);
	// 플라스크 속성 — Type 1=생명 2=마나 3=하이브리드 4=특수, RecoveryTime 단위=1/10초(게임 원본).
	const flaskRow = category === "flask" ? flaskByBase.get(base._index) : null;
	const chargeRow = category === "flask" ? chargesByBase.get(base.Id) : null;
	// 특수(Type 4) 플라스크의 부여 버프 = BuffDefinitions.StatsKeys ↔ Flasks.BuffStatValues (임플리싯과 동형 describe).
	let flaskBuffLines = [];
	if (flaskRow && flaskRow.Type === 4 && flaskRow.BuffDefinitionsKey != null) {
		const buffDef = en.buffs[flaskRow.BuffDefinitionsKey];
		if (buffDef && buffDef.StatsKeys) {
			const buffValues = new Map();
			buffDef.StatsKeys.forEach((statIndex, i) => {
				if (en.stats[statIndex]) buffValues.set(en.stats[statIndex].Id, (flaskRow.BuffStatValues || [])[i] ?? 0);
			});
			const buffEn = describe(buffValues, "English");
			const buffKo = describe(buffValues, "Korean");
			flaskBuffLines = buffEn.map((line, i) => ({ en: line, ko: buffKo[i] || null }));
		}
	}
	const flask = flaskRow
		? {
				type: flaskRow.Type,
				lifePerUse: flaskRow.LifePerUse || 0,
				manaPerUse: flaskRow.ManaPerUse || 0,
				durationSeconds: (flaskRow.RecoveryTime || 0) / 10,
				maxCharges: chargeRow?.MaxCharges || 0,
				perCharge: chargeRow?.PerCharge || 0,
				buffLines: flaskBuffLines,
			}
		: null;
	const implicitLines = (base.Implicit_ModsKeys || []).flatMap((modIndex) => {
		const mod = en.mods[modIndex];
		if (!mod) return [];
		const enLines = describeModRange(mod, "English");
		const koLines = describeModRange(mod, "Korean");
		return enLines.map((line, i) => ({ en: line, ko: koLines[i] || null }));
	});

	items.push({
		name: base.Name,
		nameKo: ko.base[base._index]?.Name || null,
		slug: base.Id.substring(base.Id.lastIndexOf("/") + 1),
		itemClass: itemClass.Id,
		itemClassKo: ko.classes[base.ItemClassesKey]?.Name || null,
		category,
		dropLevel: base.DropLevel,
		reqStr: requirement?.ReqStr || 0,
		reqDex: requirement?.ReqDex || 0,
		reqInt: requirement?.ReqInt || 0,
		armour:
			armour && (armour.ArmourMax || armour.EvasionMax || armour.EnergyShieldMax || armour.WardMax)
				? {
						armourMin: armour.ArmourMin, armourMax: armour.ArmourMax,
						evasionMin: armour.EvasionMin, evasionMax: armour.EvasionMax,
						energyShieldMin: armour.EnergyShieldMin, energyShieldMax: armour.EnergyShieldMax,
						wardMin: armour.WardMin, wardMax: armour.WardMax,
						block: shieldByBase.get(base._index)?.Block || 0,
					}
				: null,
		weapon:
			weapon && weapon.DamageMax
				? {
						damageMin: weapon.DamageMin, damageMax: weapon.DamageMax,
						critChance: weapon.Critical / 100,
						attacksPerSecond: Math.round(100000 / weapon.Speed) / 100,
						range: weapon.RangeMax,
					}
				: null,
		flask,
		implicits: implicitLines,
	});
}

// slug 중복 시 클래스 접미사
const seen = new Set();
for (const item of items) {
	if (seen.has(item.slug)) item.slug = item.slug + "-" + item.itemClass.replace(/\s+/g, "");
	seen.add(item.slug);
}

items.sort((a, b) => a.name.localeCompare(b.name));
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, JSON.stringify({ patch: PATCH, items }, null, 1));
const koCount = items.filter((i) => i.nameKo).length;
console.log(`${items.length} base items → ${OUT} (한국어 이름 ${koCount})`);
console.log("sample:", JSON.stringify(items.find((i) => i.name === "Vaal Regalia")));
console.log("weapon sample:", JSON.stringify(items.find((i) => i.name === "Thicket Bow")));
