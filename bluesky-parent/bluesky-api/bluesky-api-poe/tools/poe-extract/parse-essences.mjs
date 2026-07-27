// 에센스 제작 정보 → essences.json (poedb/craftofexile 의 "에센스" 섹션식).
// Essences 테이블의 클래스별 *_ModsKey 로 "이 에센스를 쓰면 이 클래스에 어떤 모드가 보장되는가"를 구성한다.
// 사용법: node parse-essences.mjs  (사전: extract.mjs — config.json 에 Essences 테이블 포함)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

const OUT = path.join(DATA_DIR, "essences.json");

const essences = loadTable("English", "Essences");
const mods = loadTable("English", "Mods");
const stats = loadTable("English", "Stats");
const baseEn = loadTable("English", "BaseItemTypes");
const baseKo = loadTable("Korean", "BaseItemTypes");
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@stat_descriptions.txt",
]);

// 우리 itemClass Id → Essences 테이블 컬럼. 퀴버는 에센스 부여 불가(컬럼 없음), 룬 단검/전쟁지팡이는
// 게임이 단검/지팡이 컬럼을 그대로 쓴다.
const CLASS_COLUMN = {
	Helmet: "Helmet_ModsKey",
	"Body Armour": "BodyArmour_ModsKey",
	Boots: "Boots_ModsKey",
	Gloves: "Gloves_ModsKey",
	Bow: "Bow_ModsKey",
	Wand: "Wand_ModsKey",
	Staff: "Staff_ModsKey",
	Warstaff: "Staff_ModsKey",
	"Two Hand Sword": "TwoHandSword_ModsKey",
	"Two Hand Axe": "TwoHandAxe_ModsKey",
	"Two Hand Mace": "TwoHandMace_ModsKey",
	Claw: "Claw_ModsKey",
	Dagger: "Dagger_ModsKey",
	"Rune Dagger": "Dagger_ModsKey",
	"One Hand Sword": "OneHandSword_ModsKey",
	"Thrusting One Hand Sword": "OneHandThrustingSword_ModsKey",
	"One Hand Axe": "OneHandAxe_ModsKey",
	"One Hand Mace": "OneHandMace_ModsKey",
	Sceptre: "Sceptre_ModsKey",
	Belt: "Belt_ModsKey",
	Amulet: "Amulet_ModsKey",
	Ring: "Ring_ModsKey",
	Shield: "Shield_ModsKey",
};

/** 모드 스탯 → 값 맵 (min/max) */
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

const classes = {};
let essenceCount = 0;
for (const [itemClass, column] of Object.entries(CLASS_COLUMN)) {
	const list = [];
	for (const e of essences) {
		const modIndex = e[column];
		if (modIndex == null) continue;
		const mod = mods[modIndex];
		if (!mod) continue;
		const baseRow = baseEn[e.BaseItemTypesKey];
		const baseRowKo = baseKo[e.BaseItemTypesKey];
		const en = describe(rollValues(mod, "max"), "English");
		if (!en.length) continue;
		list.push({
			// 계열 키 = 이름 꼬리("… Essence of Anger" → "Anger"). UI 가 계열 단위 접이식 카드로 묶는 기준.
			family: (baseRow?.Name || "?").replace(/^.*Essence of /, ""),
			name: baseRow?.Name || "?",
			nameKo: baseRowKo?.Name || baseRow?.Name || "?",
			// 아이콘은 essence-icons.mjs 가 같은 슬러그(BaseItemTypes Id 꼬리)로 추출한다
			icon: baseRow?.Id ? "essences/" + baseRow.Id.split("/").pop().toLowerCase() + ".png" : null,
			// Level = 에센스 티어(1 속삭임 … 7 비명, IsScreamingEssence 이후 8 절규류)
			tier: e.Level,
			ilvlMax: e.ItemLevelRestriction || 0,
			gen: mod.GenerationType === 1 ? "prefix" : "suffix",
			modName: mod.Name || "",
			en,
			enMin: describe(rollValues(mod, "min"), "English"),
			ko: describe(rollValues(mod, "max"), "Korean"),
			koMin: describe(rollValues(mod, "min"), "Korean"),
		});
	}
	// 같은 에센스 계열(이름의 "of X" 꼬리) 안에서 상위 티어 먼저
	list.sort((a, b) => {
		const fa = a.name.replace(/^.*Essence of /, "");
		const fb = b.name.replace(/^.*Essence of /, "");
		return fa === fb ? b.tier - a.tier : fa.localeCompare(fb);
	});
	if (list.length) {
		classes[itemClass] = list;
		essenceCount += list.length;
	}
}

const result = { patch: loadConfig().patch, classes };
fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(
	`essences.json: 클래스 ${Object.keys(classes).length}개, 항목 ${essenceCount}개 → ${OUT}`,
);
const sample = classes.Gloves || [];
console.log("Gloves 샘플:", sample.length + "개,", JSON.stringify(sample[0] || null).slice(0, 200));
