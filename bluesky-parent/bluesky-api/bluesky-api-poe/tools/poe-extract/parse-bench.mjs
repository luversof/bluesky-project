// 장인 작업대(벤치크래프트) 모드 → bench.json (poedb/craftofexile 의 Crafting Bench 섹션식).
// CraftingBenchOptions 의 AddMod 행을 아이템 클래스별로 묶어 "작업대로 붙일 수 있는 모드 + 비용"을 구성한다.
// 사용법: node parse-bench.mjs  (사전: extract.mjs — config.json 에 CraftingBenchOptions 포함)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

const OUT = path.join(DATA_DIR, "bench.json");

const options = loadTable("English", "CraftingBenchOptions");
// ⚠ 장비 행은 ItemClasses 가 아니라 CraftingItemClassCategories 로 게이트된다(ItemClasses 는 맵 전용 행만).
// 카테고리 행이 자체 ItemClasses 배열(예: One Hand Melee → 8클래스)을 들고 있어 하드코딩 없이 조인 가능.
const benchCategories = loadTable("English", "CraftingItemClassCategories");
const mods = loadTable("English", "Mods");
const stats = loadTable("English", "Stats");
const itemClassesEn = loadTable("English", "ItemClasses");
const baseEn = loadTable("English", "BaseItemTypes");
const baseKo = loadTable("Korean", "BaseItemTypes");
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@stat_descriptions.txt",
]);

// 장비 파이프라인이 다루는 클래스만 (mods.json 과 동일 범위)
const baseItems = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "base-items.json"), "utf8")).items;
const wantedClasses = new Set(baseItems.map((b) => b.itemClass));

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

const GEN = { 1: "prefix", 2: "suffix" };
const classes = {}; // itemClassId → [entry]
let total = 0;
for (const row of options) {
	if (row.AddMod == null || row.IsDisabled) continue;
	const mod = mods[row.AddMod];
	if (!mod || !GEN[mod.GenerationType]) continue;
	const en = describe(rollValues(mod, "max"), "English");
	if (!en.length) continue;
	const enMin = describe(rollValues(mod, "min"), "English");
	const ko = describe(rollValues(mod, "max"), "Korean");
	const koMin = describe(rollValues(mod, "min"), "Korean");
	// 비용(화폐 아이템 × 수량) — 이름 조인만(아이콘은 미추출)
	const cost = [];
	const costItems = row.Cost_BaseItemTypes || [];
	const costValues = row.Cost_Values || [];
	for (let i = 0; i < costItems.length; i++) {
		const b = baseEn[costItems[i]];
		if (!b) continue;
		cost.push({
			name: b.Name,
			nameKo: baseKo[costItems[i]]?.Name || b.Name,
			// 화폐 아이콘은 currency-icons.mjs 가 같은 슬러그로 추출(icons/currency/)
			icon: b.Id ? "currency/" + b.Id.split("/").pop().toLowerCase() + ".png" : null,
			count: costValues[i] ?? 1,
		});
	}
	const entry = {
		gen: GEN[mod.GenerationType],
		tier: row.Tier ?? 0,
		reqLevel: row.RequiredLevel ?? 0,
		modName: mod.Name || "",
		en,
		enMin,
		ko,
		koMin,
		cost,
	};
	// 카테고리 → 클래스 전개(장비 경로) + 직접 ItemClasses(맵 등 — wantedClasses 필터로 자연 배제/포함)
	const clsIndexes = new Set(row.ItemClasses || []);
	for (const catIdx of row.CraftingItemClassCategories || []) {
		for (const clsIdx of benchCategories[catIdx]?.ItemClasses || []) {
			clsIndexes.add(clsIdx);
		}
	}
	for (const clsIdx of clsIndexes) {
		const clsId = itemClassesEn[clsIdx]?.Id;
		if (!clsId || !wantedClasses.has(clsId)) continue;
		(classes[clsId] ??= []).push(entry);
		total++;
	}
}
// 클래스 안에서 접두→접미, 같은 계열(첫 스탯줄 어간)끼리 티어 내림차순으로 안정 정렬
for (const list of Object.values(classes)) {
	list.sort((a, b) => {
		if (a.gen !== b.gen) return a.gen === "prefix" ? -1 : 1;
		const ka = (a.en[0] || "").replace(/[\d.()\-–+%]+/g, "");
		const kb = (b.en[0] || "").replace(/[\d.()\-–+%]+/g, "");
		return ka === kb ? b.tier - a.tier : ka.localeCompare(kb);
	});
}

const result = { patch: loadConfig().patch, classes };
fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(
	`bench.json: 클래스 ${Object.keys(classes).length}개, 항목 ${total}개 → ${OUT}`,
);
const sample = classes.Amulet || [];
console.log("Amulet 샘플:", sample.length + "개,", JSON.stringify(sample[0] || null).slice(0, 220));
