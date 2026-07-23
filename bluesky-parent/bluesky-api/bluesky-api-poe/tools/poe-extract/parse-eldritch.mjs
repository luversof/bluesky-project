// 엘드리치 임플리싯(총주교/포식자) 풀 → eldritch-implicits.json.
// 3.19 이후 특정 방어/장신구 슬롯에 총주교(Searing Exarch)·포식자(Eater of Worlds) 화폐로 부여하는 임플리싯.
// 게임 Mods 에서 `*EldritchImplicit<티어>` 패턴을 뽑아 (팩션 × 슬롯 × 계열)로 묶고 티어 사다리를 emit.
//   GenerationType 28 = 총주교, 29 = 포식자. 슬롯은 스폰웨이트 태그(gloves/boots/helmet/body_armour/amulet/ring/belt).
// 사용법: node parse-eldritch.mjs (사전: extract.mjs) — run-all 에선 parse-mods-full 뒤.
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

const OUT = path.join(DATA_DIR, "eldritch-implicits.json");

const mods = loadTable("English", "Mods");
const stats = loadTable("English", "Stats");
const tags = loadTable("English", "Tags");
const describe = createStatDescriber(FILES_DIR, ["metadata@statdescriptions@stat_descriptions.txt"]);

// 팩션: GenerationType → {key, 이름}
const FACTION = {
	28: { key: "exarch", name: "Searing Exarch", nameKo: "작열의 총주교" },
	29: { key: "eater", name: "Eater of Worlds", nameKo: "세계를 삼키는 포식자" },
};
// 엘드리치 부여 가능 슬롯(스폰 태그 → 아이템 클래스명). 이 태그가 붙은 것만 취급.
const SLOT_TAG_TO_CLASS = {
	body_armour: "Body Armour",
	helmet: "Helmet",
	gloves: "Gloves",
	boots: "Boots",
	amulet: "Amulet",
	ring: "Ring",
	belt: "Belt",
};

function rollValues(mod, kind) {
	const values = new Map();
	for (let i = 1; i <= 6; i++) {
		const si = mod["StatsKey" + i];
		if (si == null) continue;
		const stat = stats[si];
		if (stat) values.set(stat.Id, mod["Stat" + i + (kind === "min" ? "Min" : "Max")] ?? 0);
	}
	return values;
}
const familyKey = (id) => id.replace(/\d+$/, "");
const tierNum = (id) => parseInt((id.match(/(\d+)$/) || [])[1] || "0", 10);

// 1) 엘드리치 임플리싯 수집 → (faction, family) 로 티어 묶음, 슬롯은 태그로
const families = new Map(); // `${faction}::${family}` → {faction, key, slots:Set, tiers:Map<tierNum, tier>}
for (const mod of mods) {
	if (!/EldritchImplicit\d+$/.test(mod.Id || "")) continue;
	const faction = FACTION[mod.GenerationType];
	if (!faction) continue;
	// 스폰 태그 중 엘드리치 슬롯인 것만(weight>0)
	const slots = new Set();
	const keys = mod.SpawnWeight_TagsKeys || [];
	const vals = mod.SpawnWeight_Values || [];
	for (let i = 0; i < keys.length; i++) {
		const tagId = tags[keys[i]]?.Id;
		if (tagId && SLOT_TAG_TO_CLASS[tagId] && (vals[i] ?? 0) > 0) slots.add(SLOT_TAG_TO_CLASS[tagId]);
	}
	if (!slots.size) continue;
	const fam = familyKey(mod.Id);
	const mapKey = faction.key + "::" + fam;
	let entry = families.get(mapKey);
	if (!entry) {
		entry = { faction: faction.key, key: fam, slots: new Set(), tiers: new Map() };
		families.set(mapKey, entry);
	}
	for (const s of slots) entry.slots.add(s);
	const tn = tierNum(mod.Id);
	if (!entry.tiers.has(tn)) {
		entry.tiers.set(tn, {
			tier: tn,
			en: describe(rollValues(mod, "max"), "English"),
			ko: describe(rollValues(mod, "max"), "Korean"),
		});
	}
}

// 2) 아이템 클래스 × 팩션 → 계열 목록(티어 사다리, 강→약). 스탯 서술 없는 계열은 제외.
const bySlot = {}; // itemClass → { exarch:[], eater:[] }
for (const entry of families.values()) {
	const tiers = [...entry.tiers.values()].sort((a, b) => b.tier - a.tier); // 티어 큰 값 = 강함 → 먼저
	if (!tiers.some((t) => t.en?.length)) continue;
	const fam = { key: entry.key, tiers };
	for (const cls of entry.slots) {
		const slot = (bySlot[cls] ||= { exarch: [], eater: [] });
		slot[entry.faction].push(fam);
	}
}
// 계열 정렬(키 알파벳) — 안정적 표시
for (const cls of Object.values(bySlot)) {
	cls.exarch.sort((a, b) => a.key.localeCompare(b.key));
	cls.eater.sort((a, b) => a.key.localeCompare(b.key));
}

const factions = {};
for (const f of Object.values(FACTION)) factions[f.key] = { name: f.name, nameKo: f.nameKo };
const result = { patch: loadConfig().patch, factions, bySlot };
fs.writeFileSync(OUT, JSON.stringify(result));
const clsCount = Object.keys(bySlot).length;
const famCount = [...new Set([...families.values()].map((e) => e.faction + "::" + e.key))].length;
console.log(`eldritch-implicits.json: 슬롯 ${clsCount}종, 계열 ${famCount}개 → ${OUT}`);
const g = bySlot["Gloves"];
if (g) console.log(`장갑: 총주교 ${g.exarch.length} · 포식자 ${g.eater.length}`);
