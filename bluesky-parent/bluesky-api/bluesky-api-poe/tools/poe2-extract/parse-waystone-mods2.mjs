// PoE2 경로석(Waystone) 옵션 → ~/.poe-gamedata/poe2/waystone-mods.json — /poe2/regex 경로석 정규식 생성기용.
// PoE1 parse-map-mods.mjs 와 같은 모양(mods/names)으로 내보내 게이트 regex.ts 를 그대로 쓴다. 다른 점:
//  · 옵션은 도메인 6 중 스폰 태그에 map_key_* 가 있는 것만 — 같은 도메인에 탐험 일지(23)·수정 오브(3, default 태그) 옵션이 섞여 있다.
//  · 스폰은 첫 매치 규칙으로 등급 구간(1 / 2~5 / 6~10 / 11~15 / 16)의 태그 집합마다 따져 가장 낮은 등급(minTier)을 기록한다.
//  · 보상 스탯(*_final_from_map)은 설명 문구가 없고 경로석 머리글(희귀도·무리 규모·경로석 출현 확률…)로 합산돼 보인다 →
//    rewards 로 따로 담아 배지로 보여준다.
//  · 머리글 문구(headers)·임계값 앵커(thresholds)·보상 라벨(rewardDefs)도 이 파일에 실어, 생성기가 게임별 차이를 데이터로 받는다.
import { createDescriber, stripMarkup } from "./common2.mjs";
import { loadConfig, loadTable, writeJson } from "./paths.mjs";

const mods = loadTable("English", "Mods");
const modsKo = loadTable("Korean", "Mods");
const stats = loadTable("English", "Stats");
const tags = loadTable("English", "Tags");
const describe = createDescriber("item");

// 경로석 등급 → 베이스 태그(게임 BaseItemTypes 에서 확인: 1=map_key_low, 2~5=+high_level_map, 6~10=medium, 11~15=high, 16=highest)
const TIER_TAGS = [
	[1, ["map_key_low"]],
	[2, ["high_level_map", "map_key_low"]],
	[6, ["high_level_map", "map_key_medium"]],
	[11, ["high_level_map", "map_key_high"]],
	[16, ["high_level_map", "map_key_highest"]],
].map(([tier, t]) => [tier, new Set([...t, "default"])]);
const ABYSS_TAGS = new Set(["map", "default"]);
const GEN = { 1: "prefix", 2: "suffix", 3: "prefix" }; // 3 = 일부 접두 변형(쾌속 1·2, 피폐의) — 이름이 접두형이라 접두 열에

function firstMatchWeight(mod, tagSet) {
	for (let j = 0; j < mod.SpawnWeight_Tags.length; j++) {
		if (tagSet.has(tags[mod.SpawnWeight_Tags[j]]?.Id)) return mod.SpawnWeight_Values[j] ?? 0;
	}
	return 0;
}

// 보상 스탯 → 키(배지·머리글과 같은 뜻)
const REWARD = {
	"map_map_item_drop_chance_+%_final_from_map": "waystone",
	"map_item_drop_rarity_+%_final_from_map": "rarity",
	"map_pack_size_+%_final_from_map": "pack",
	"map_number_of_magic_and_rare_packs_+%_final_and_rare_monster_modifiers_chance_+%_final_from_map": "magicRare",
	"map_monster_potency_+%_final_from_map": "potency",
};
const NUM = /-?\d+(?:\.\d+)?/g;

function valuesOf(mod, pick) {
	const effect = new Map();
	const reward = {};
	for (let k = 1; k <= 6; k++) {
		const s = mod["Stat" + k];
		if (s == null) continue;
		const id = stats[s].Id;
		const v = mod["Stat" + k + "Value"]?.[pick] ?? 0;
		if (REWARD[id]) reward[REWARD[id]] = v;
		else effect.set(id, v);
	}
	return { effect, reward };
}

function combine(a, b, fn) {
	if (a == null) return b;
	if (b == null) return a;
	const nb = b.match(NUM) || [];
	const na = a.match(NUM) || [];
	if (na.length !== nb.length) return a;
	let i = 0;
	return a.replace(NUM, (n) => String(fn(Number(n), Number(nb[i++]))));
}
function mergeRange(minLine, maxLine) {
	if (minLine == null || minLine === maxLine) return maxLine;
	const lo = minLine.match(NUM) || [];
	const hi = maxLine.match(NUM) || [];
	if (lo.length !== hi.length) return maxLine;
	let i = 0;
	return maxLine.replace(NUM, (n) => {
		const [a, b] = [Number(lo[i++]), Number(n)].sort((x, y) => x - y);
		if (a === b) return n;
		// 둘 다 음수면 "(-10--3)" 대신 부호를 밖으로 빼 "-(3-10)" — 읽기 쉽고, 생성기의 범위 치환((a-b) → 한 자리)도 그대로 먹는다
		if (b < 0) return `-(${-b}-${-a})`;
		return `(${a}-${b})`;
	});
}
const clean = (s) => stripMarkup(s);

const entries = new Map();
let sourceCount = 0;
mods.forEach((mod, index) => {
	if (!mod || !GEN[mod.GenerationType] || !mod.Name) return;
	// 심연 옵션(도메인 28, map 태그) — 심연 징조 등으로 경로석에 붙는다. "3/10초 피해 없음" 같은 치명 옵션이 있어 거르기 목록에 꼭 필요.
	const abyss = mod.Domain === 28 && mod.GenerationType !== 3 && firstMatchWeight(mod, ABYSS_TAGS) > 0;
	if (!abyss && (mod.Domain !== 6 || !mod.SpawnWeight_Tags.some((t) => /^map_key_/.test(tags[t]?.Id || "")))) return;
	const tiers = abyss ? [1] : TIER_TAGS.filter(([, set]) => firstMatchWeight(mod, set) > 0).map(([tier]) => tier);
	if (!tiers.length) return; // 예: 분할의(Splitting) — 모든 태그 가중치 0
	sourceCount++;
	const lo = valuesOf(mod, 0);
	const hi = valuesOf(mod, 1);
	const enMin = describe(lo.effect, "English").map(clean);
	const enMax = describe(hi.effect, "English").map(clean);
	const koMin = describe(lo.effect, "Korean").map(clean);
	const koMax = describe(hi.effect, "Korean").map(clean);
	// 병합 키는 **문구만**(이름 제외) — 같은 효과의 상위 티어가 이름만 달라(감전 지대 1~3 vs 4) 같은 줄이 두 번 보였다.
	//   정규식으로는 문구가 같으면 구분할 수 없으니 한 줄로 합치고, 수치 범위·최저 등급·보상만 넓힌다.
	const key = (abyss ? "abyss|" : "") + enMax.map((s) => s.replace(NUM, "#")).join("|");
	let e = entries.get(key);
	if (!e) {
		e = {
			id: mod.Id,
			name: mod.Name,
			nameKo: (modsKo[index]?.Name || mod.Name).replace(/^-\s*/, ""),
			gen: GEN[mod.GenerationType],
			minTier: Math.min(...tiers),
			special: abyss ? "abyss" : undefined,
			rewards: {},
			enMin,
			enMax,
			koMin,
			koMax,
		};
		entries.set(key, e);
	} else {
		if (mod.GenerationType !== 3) e.gen = GEN[mod.GenerationType];
		e.minTier = Math.min(e.minTier, ...tiers);
		e.enMin = e.enMin.map((s, i) => combine(s, enMin[i], Math.min));
		e.enMax = e.enMax.map((s, i) => combine(s, enMax[i], Math.max));
		e.koMin = e.koMin.map((s, i) => combine(s, koMin[i], Math.min));
		e.koMax = e.koMax.map((s, i) => combine(s, koMax[i], Math.max));
	}
	for (const [k, v] of Object.entries(hi.reward)) e.rewards[k] = Math.max(e.rewards[k] ?? 0, v);
});

const list = [...entries.values()].map((e) => ({
	id: e.id,
	name: e.name,
	nameKo: e.nameKo,
	gen: e.gen,
	normal: true,
	uber: false,
	quant: 0,
	rarity: 0,
	packSize: 0,
	minTier: e.minTier,
	special: e.special,
	rewards: e.rewards,
	en: e.enMax.map((s, i) => mergeRange(e.enMin[i], s)),
	ko: e.koMax.map((s, i) => mergeRange(e.koMin[i], s)),
}));
list.sort((a, b) => (!!a.special !== !!b.special ? (a.special ? 1 : -1) : a.gen !== b.gen ? (a.gen === "prefix" ? -1 : 1) : (a.ko[0] || "").localeCompare(b.ko[0] || "", "ko")));

// 경로석 이름(1~16등급) — 항이 이름과 겹치면 그 등급 경로석이 통째로 걸린다
const baseEn = loadTable("English", "BaseItemTypes");
const baseKo = loadTable("Korean", "BaseItemTypes");
const classes = loadTable("English", "ItemClasses");
const classesKo = loadTable("Korean", "ItemClasses");
const mapClass = classes.findIndex((c) => c.Id === "Map");
const names = { en: [], ko: [] };
baseEn.forEach((b, i) => {
	if (b.ItemClass !== mapClass || !b.Name) return;
	names.en.push(b.Name);
	if (baseKo[i]?.Name) names.ko.push(baseKo[i].Name);
});

// 경로석 머리글 — 게임 ClientStrings 문구로 조립(인게임 툴팁 순서). 수치 자리는 99, 등급은 16.
const cs = loadTable("English", "ClientStrings");
const csKo = loadTable("Korean", "ClientStrings");
const csIdx = new Map(cs.map((c, i) => [c.Id, i]));
const S = (id, lang) => {
	const i = csIdx.get(id);
	if (i == null) throw new Error(`ClientStrings ${id} 없음`);
	return stripMarkup((lang === "ko" ? csKo : cs)[i].Text);
};
const header = (lang) => {
	const aug = lang === "ko" ? " (증강됨)" : " (augmented)";
	return [
		`${S("ItemDisplayStringClass", lang)}: ${lang === "ko" ? classesKo[mapClass].Name : classes[mapClass].Name}`,
		`${S("ItemDisplayStringRarity", lang)}: ${S("ItemDisplayStringRare", lang)}`,
		`${S("ItemDisplayMapTier", lang)}: 16`,
		`${S("NumberOfPortalsPerWaystone", lang)}: 0`,
		`${S("ItemDisplayMapMonsterEffectiveness", lang)}: +99%${aug}`,
		`${S("ItemDisplayMapItemRarity", lang)}: +99%${aug}`,
		`${S("ItemDisplayMapPack", lang)}: +99%${aug}`,
		`${S("ItemDisplayMapMagicMonsterQuantityBonus", lang)}: +99%${aug}`,
		`${S("ItemDisplayMapRareMonsterQuantityBonus", lang)}: +99%${aug}`,
		`${S("ItemDisplayMapWaystoneDropChance", lang)}: +99%${aug}`,
		`${S("ItemDisplayMapExperienceGained", lang)}: +99%${aug}`,
		S("MonsterCorrupted", lang),
		lang === "ko" ? "타락함" : "Corrupted",
	];
};

const config = loadConfig();
const out = {
	patch: config.patch,
	game: "poe2",
	mods: list,
	names,
	headers: { ko: header("ko"), en: header("en") },
	// 임계값 두 칸: quant 칸 = 아이템 희귀도, pack 칸 = 무리 규모(경로석엔 아이템 수량 머리글이 없다).
	// 앵커는 머리글에서 다른 줄과 안 겹치는 최단 낱말 — "아이템 희귀도: 희귀"(희귀도 줄)는 숫자가 없어 안 걸린다.
	thresholds: {
		quant: { ko: "희귀도.*", en: "m Rarity.*", key: "희귀도|Rarity" },
		pack: { ko: "규모.*", en: "Size.*", key: "규모|무리|Pack ?Size|\\bSize" },
	},
	rewardDefs: [
		{ key: "waystone", ko: "경로석", en: "WS", title: [S("ItemDisplayMapWaystoneDropChance", "ko"), S("ItemDisplayMapWaystoneDropChance", "en")] },
		{ key: "rarity", ko: "희귀도", en: "R", title: [S("ItemDisplayMapItemRarity", "ko"), S("ItemDisplayMapItemRarity", "en")] },
		{ key: "pack", ko: "무리", en: "P", title: [S("ItemDisplayMapPack", "ko"), S("ItemDisplayMapPack", "en")] },
		{ key: "magicRare", ko: "마법·희귀", en: "M/R", title: [S("ItemDisplayMapMagicMonsterQuantityBonus", "ko") + "·" + S("ItemDisplayMapRareMonsterQuantityBonus", "ko"), S("ItemDisplayMapMagicMonsterQuantityBonus", "en") + "/" + S("ItemDisplayMapRareMonsterQuantityBonus", "en")] },
		{ key: "potency", ko: "효율", en: "Eff", title: [S("ItemDisplayMapMonsterEffectiveness", "ko"), S("ItemDisplayMapMonsterEffectiveness", "en")] },
	],
	specialDefs: { abyss: { ko: "심연", en: "Abyss", title: ["심연 징조 등으로 붙는 심연 옵션", "Abyssal modifier (added by Abyss crafting)"] } },
};
writeJson("waystone-mods.json", out);
const g = (x) => list.filter((m) => m.gen === x).length;
console.log(`waystone-mods.json: 원본 ${sourceCount}건 → ${list.length}종 (접두 ${g("prefix")}, 접미 ${g("suffix")}, 심연 ${list.filter((m) => m.special).length}) · 경로석 이름 ${names.ko.length}`);
