// 제작 화폐 데이터(10-15 C338 — 사용자 요청 "다양한 제작 컨텐츠들이 엮여 있고, 섞어서 사용이 가능하고 그에 따른 제약도 있다") →
//   ~/.poe-gamedata/craft-currencies.json
//   · fossils: DelveCraftingModifiers — 옵션 태그별 가중치 배수(값/100, 0 = 막음), 화석 전용 추가 옵션 · 강제 옵션, 부패 에센스 확률, 행운(수치 두 번 굴려 높은 쪽)
//   · resonators: 공명기(연금 = 일반 → 레어, 혼돈 = 레어 다시) 칸 수 1~4
//   · harvest: HarvestCraftOptions 중 아이템 옵션을 바꾸는 명령(reroll_with_mod · affinity · convert_mod · remove_type_and_add_type_mod ·
//     reroll_with_influence_mod · reroll_influence_types(10-15 C347 영향력 유형 무작위 + 재련)) — 문구 영 · 한, 생명력 종류 · 비용
// 사용법: node parse-craft-currencies.mjs  (사전: config.json 에 DelveCraftingModifiers · HarvestCraftOptions 포함 추출)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, loadConfig, loadTable } from "./paths.mjs";

const OUT = path.join(process.env.POE_DATA_OUT || DATA_DIR, "craft-currencies.json");

const fossilRows = loadTable("English", "DelveCraftingModifiers");
const descEn = loadTable("English", "DelveCraftingModifierDescriptions");
const descKo = loadTable("Korean", "DelveCraftingModifierDescriptions");
const harvestEn = loadTable("English", "HarvestCraftOptions");
const harvestKo = loadTable("Korean", "HarvestCraftOptions");
const baseEn = loadTable("English", "BaseItemTypes");
const baseKo = loadTable("Korean", "BaseItemTypes");
const tags = loadTable("English", "Tags");
const mods = loadTable("English", "Mods");

const slugOf = (row) => (row?.Id || "").split("/").pop();
const clean = (t) => String(t || "").replace(/<[^>]*>/g, "").replace(/[{}]/g, "").trim();

// 1) 화석 — 이름이 "… Fossil" 인 행만(같은 표에 공명기 · 기타 델브 화폐 행도 있다)
const fossils = [];
for (const r of fossilRows) {
	const b = baseEn[r.BaseItemTypesKey];
	if (!b || !/ Fossil$/.test(b.Name || "")) continue;
	const weights = {};
	(r.Weight_TagsKeys || []).forEach((k, i) => (weights[tags[k]?.Id] = (r.Weight_Values?.[i] ?? 100) / 100));
	(r.NegativeWeight_TagsKeys || []).forEach((k, i) => (weights[tags[k]?.Id] = (r.NegativeWeight_Values?.[i] ?? 0) / 100));
	delete weights.undefined;
	const descIdx = r.DelveCraftingModifierDescriptionsKeys || [];
	const blockedIdx = r.BlockedDelveCraftingModifierDescriptionsKeys || [];
	fossils.push({
		slug: slugOf(b),
		name: b.Name,
		nameKo: baseKo[r.BaseItemTypesKey]?.Name || b.Name,
		icon: "currency/" + slugOf(b).toLowerCase() + ".png",
		weights,
		added: (r.AddedModsKeys || []).map((k) => mods[k]?.Id).filter(Boolean),
		forced: (r.ForcedAddModsKeys || []).map((k) => mods[k]?.Id).filter(Boolean),
		corruptedEssenceChance: r.CorruptedEssenceChance || 0,
		lucky: !!r.HasLuckyRolls,
		desc: descIdx.map((i) => clean(descEn[i]?.Description)).filter(Boolean),
		descKo: descIdx.map((i) => clean(descKo[i]?.Description)).filter(Boolean),
		blocks: blockedIdx.map((i) => descEn[i]?.Id).filter(Boolean),
		// 엔진이 지금 다루는 화석 = 태그 배수만 있는 것(추가 · 강제 옵션 · 부패 에센스 · 행운은 다음 단계)
		simple: Object.keys(weights).length > 0 && !(r.AddedModsKeys || []).length && !(r.ForcedAddModsKeys || []).length && !r.CorruptedEssenceChance && !r.HasLuckyRolls,
	});
}
fossils.sort((a, b) => a.name.localeCompare(b.name));

// 2) 공명기 — Delve(Stackable)SocketableCurrency{Upgrade|Reroll}{1..4}
const resonators = [];
const seen = new Set();
baseEn.forEach((b, i) => {
	const m = /DelveStackableSocketableCurrency(Upgrade|Reroll)(\d)$/.exec(b?.Id || "");
	if (!m || seen.has(b.Name)) return;
	seen.add(b.Name);
	resonators.push({ slug: slugOf(b), name: b.Name, nameKo: baseKo[i]?.Name || b.Name, kind: m[1] === "Upgrade" ? "alchemical" : "chaotic", sockets: Number(m[2]), icon: "currency/" + slugOf(b).toLowerCase() + ".png" });
});
resonators.sort((a, b) => a.kind.localeCompare(b.kind) || a.sockets - b.sockets);

// 3) 수확 — 아이템 옵션을 바꾸는 명령만
const HARVEST_COMMANDS = new Set(["reroll_with_mod", "reroll_with_current_tags_affinity_multiplier", "convert_mod", "remove_type_and_add_type_mod", "reroll_with_influence_mod", "reroll_influence_types"]);
const harvest = [];
harvestEn.forEach((r, i) => {
	if (!HARVEST_COMMANDS.has(r.Command)) return;
	harvest.push({
		id: r.Id,
		command: r.Command,
		params: r.Parameters || "",
		text: clean(r.Text) || r.Description,
		textKo: clean(harvestKo[i]?.Text) || clean(harvestKo[i]?.Description) || null,
		lifeforce: ({ 1: "Vivid", 2: "Primal", 3: "Wild", 4: "Sacred" })[r.LifeforceType] || null,
		cost: r.LifeforceCost || 0,
		sacredCost: r.SacredCost || 0,
	});
});

// 4) 제작 화폐 설명(10-15 C340) — CurrencyItems.Action · Directions(인게임 사용 설명, 영 · 한). 에뮬레이터 버튼 설명 · 제약 근거
//    (예: 분열 오브 "Cannot be used on Influenced or Fractured items")
const CRAFT_ACTIONS = /^(add_mod_to_rare|add_mod_to_magic|reroll_rare|reroll_magic|upgrade_to_magic|upgrade_to_rare|upgrade_magic_to_rare|remove_random_mod|remove_all_mods|fracture_random_mod|add_influence_mod_to_rare|transfer_item_influence|upgrade_influence_mod|add_cleansing_fire_implicit_\d|add_great_tangle_implicit_\d|conflict_orb|reroll_rare_eldritch|add_mod_to_rare_eldritch|remove_random_mod_eldritch|reroll_rare_veiled|replace_rare_mod_veiled|corrupt_item|reroll_rare_hellscape|add_mod_to_rare_hellscape|upgrade_mod_tier_hellscape|add_jewellery_quality|add_random_jewellery_quality|reroll_single_attribute_modifier|add_deepwater_hazard_belt_mod|add_pantheon_aspect|add_mod_and_corrupt_rare_abyss_jewel|reroll_rare_infamous|add_eldritch_implicit_amulet|mutated_add_mod_to_magic|mutated_upgrade_magic_to_rare|mutated_add_mod_to_rare)$/; // 두카트 3종(C391, 군왕호 올플레임 제작) // *_jewellery_quality = 기폭제(C385, 반지 · 목걸이 · 허리띠 퀄리티), corrupt_item = 바알 오브(C358), *_hellscape = 더럽혀진 화폐(타락한 레어 전용, C359)
let currencies = [];
try {
	const ciEn = loadTable("English", "CurrencyItems");
	const ciKo = loadTable("Korean", "CurrencyItems");
	ciEn.forEach((r, i) => {
		const b = baseEn[r.BaseItemTypesKey];
		if (!b || !r.Action || !CRAFT_ACTIONS.test(r.Action)) return;
		const c = { name: b.Name, nameKo: baseKo[r.BaseItemTypesKey]?.Name || b.Name, slug: slugOf(b), action: r.Action, directions: clean(r.Directions), directionsKo: clean(ciKo[i]?.Directions) || null };
		// 기폭제(C385) — 종류 키(Id 끝 CurrencyJewelleryQuality{Attack…}) · 퀄리티 머리글(설명 "enhances X modifiers" / "부여된 X을 향상")
		const cat = /^CurrencyJewelleryQuality(\w+)$/.exec(c.slug);
		if (cat) {
			c.catalyst = cat[1].toLowerCase();
			const en = /enhances (.+?) modifiers/i.exec(r.Description || "");
			const ko = /부여된 (.+?)[을를] 향상/.exec(ciKo[i]?.Description || "");
			c.qualityLabel = en ? en[1] + " Modifiers" : null;
			c.qualityLabelKo = ko ? ko[1] : null;
			c.description = clean(r.Description).replace(/\s*\n\s*/g, " ");
			c.descriptionKo = clean(ciKo[i]?.Description).replace(/\s*\n\s*/g, " ") || null;
		}
		currencies.push(c);
	});
} catch (e) {
	console.warn("CurrencyItems 없음 — 화폐 설명 생략:", e.message);
}

fs.writeFileSync(OUT, JSON.stringify({ patch: loadConfig().patch, fossils, resonators, harvest, currencies }));
console.log(`craft-currencies.json: 화폐 설명 ${currencies.length} · 화석 ${fossils.length}(태그 배수만 ${fossils.filter((f) => f.simple).length}) · 공명기 ${resonators.length} · 수확 ${harvest.length} → ${OUT}`);
