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
// 풀(클래스|변형)별로 **실제 붙는 티어** — families 는 같은 그룹의 티어를 모든 클래스에서 합쳐 담으므로(생명력 +175~189 는 갑옷 전용인데
// 장화 패밀리에도 들어 있다) "이 장화에서 2티어" 를 알려면 풀별 목록이 따로 있어야 한다. mods.json 스키마를 건드리지 않으려고 별도 파일로 쓴다
// (PoeModDataService 가 레코드로 읽는다). 소비자: 업그레이드 가이드의 레어 목표(2티어) 추천.
// ⚠ 티어 사다리는 게임의 **ModTypeKey** 로 묶는다 — familyKey(Id 꼬리 숫자 떼기)는 "ChanceToSuppressSpells5__" 처럼 밑줄이 둘인 최상위
//   티어를 따로 떼어 내, 2티어를 잘못 세고(억제 +9~10% 를 2티어로) 같은 계열을 한 아이템에 두 번 얹었다(2026-09-29). 한 아이템에 같이 못 붙는지는
//   게임의 **Families** 로 판정한다(생명력[0] 과 방어도+생명력 복합[2605] 은 같이 붙는다).
const OUT_TIERS = path.join(DATA_DIR, "mod-pool-tiers.json");
// 제작 엔진용(10-15 C335 — 사용자 요청 "craftofexile 같은 제작 시뮬레이터 · 에뮬레이터"): 풀마다 옵션 id → 그 풀의 가중치, 옵션마다
//   계열 · 접두/접미 · 아이템 레벨 · 옵션 그룹(Families — 같은 그룹은 한 아이템에 하나) · 옵션 태그(ImplicitTags — 화석 · 수확 가중치용).
//   mods.json 의 등급 가중치는 그 등급이 처음 나온 풀의 값이라 클래스마다 가중치가 다른 옵션이 틀릴 수 있어 따로 싣는다
const OUT_CRAFT = path.join(DATA_DIR, "craft-mods.json");

const mods = loadTable("English", "Mods");
// 옵션 유형 이름(C384 — 수확 원소 변환의 매개변수가 ModType 이름 목록이다). 옛 작업 폴더면 없을 수 있다
let modTypeNames = [];
try {
	modTypeNames = loadTable("English", "ModType");
} catch {
	console.warn("ModType 표 없음 — craft-mods.json 에 mt 를 싣지 않는다");
}
// 리마인더(인게임 회색 부연, 10-04 C131) — 티어 영어(최대롤) 줄마다 고른 템플릿의 reminderstring Id(describe.reminders)를 rem 으로,
//   쓰인 Id 의 영 · 한 문구는 맨 위 reminderText 로 한 번(빌드 화면 레어 아이템 툴팁이 API 번역기를 거쳐 쓴다)
const remEnRows = loadTable("English", "ReminderText");
const remKoRows = loadTable("Korean", "ReminderText");
const reminderTextById = new Map();
remEnRows.forEach((r, i) => {
	if (r.Text) reminderTextById.set(r.Id, { en: r.Text, ko: remKoRows[i]?.Id === r.Id && remKoRows[i]?.Text ? remKoRows[i].Text : r.Text });
});
const usedReminderIds = new Set();
const remOf = () => {
	const ids = (describe.reminders || []).map((l) => l.filter((id) => reminderTextById.has(id)));
	if (!ids.some((l) => l.length)) return {};
	ids.flat().forEach((id) => usedReminderIds.add(id));
	return { rem: ids };
};
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
	// 플라스크/주얼 — 장비(도메인 1)와 모드 도메인이 다르다(CLASS_DOMAIN 참고)
	LifeFlask: ["life_flask", "flask"],
	ManaFlask: ["mana_flask", "flask"],
	HybridFlask: ["hybrid_flask", "flask"],
	UtilityFlask: ["utility_flask", "flask"],
	Jewel: ["jewel", "default"],
	AbyssJewel: ["abyss_jewel", "default"],
};

// 클래스별 모드 도메인 — 기본 1(장비). 플라스크 2, 일반 주얼 10, 어비스 주얼 13(게임 테이블 실측).
const CLASS_DOMAIN = {
	LifeFlask: 2,
	ManaFlask: 2,
	HybridFlask: 2,
	UtilityFlask: 2,
	Jewel: 10,
	AbyssJewel: 13,
};
// 이 파이프라인이 모드 풀을 보여줄 대상 = 장비(무기/방어/장신구)만. 플라스크/주얼은 별도 모드 도메인이라 제외.
const wantedClasses = new Set(Object.keys(CLASS_TAGS));

// 군집 주얼(10-20 C514) — 베이스는 'Jewel' 클래스지만 옵션은 도메인 21(군집 전용 — 노터블 · 작은 패시브 효과)이고,
// 인챈트(작은 패시브 효과)마다 붙는 노터블이 다르다. 스폰 태그 = 크기 태그(expansion_jewel_*, 베이스 TagsKeys) + 인챈트 태그(affliction_*).
// 크기마다 인챈트 태그가 겹치지 않아(대형 17 · 중형 21 · 소형 17종, 10-20 확인) 생성 태그만으로 노터블 크기 제한까지 맞는다.
// 인챈트 목록은 PoB 파생 cluster-jewels.json(이 파서보다 뒤 단계 산출물 — 지난 실행본)에서 읽고, 없으면 군집 주얼을 뺀다(일반 주얼 풀에 섞지 않는다).
// 변형 키 = "cluster_<크기>.<인챈트 태그>" — 풀 키 "Jewel|cluster_large.affliction_attack_damage_|".
const CLUSTER_DOMAIN = 21;
let clusterDefs = null;
try {
	clusterDefs = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "cluster-jewels.json"), "utf8")).jewels || null;
} catch {
	clusterDefs = null;
}
const isClusterBase = (b) => b.itemClass === "Jewel" && /Cluster Jewel$/.test(b.name || "");
// 카드 인챈트 줄(인게임 순서 = PoB ItemsTab CraftClusterJewel): 패시브 수(최소) → 주얼 홈(대형 2 · 중형 1 · 소형 없음) → 작은 패시브 효과
// 문구는 게임 서술(local_jewel_expansion_passive_node_count · local_jewel_expansion_jewels_count) + ClientStrings 접두
const clusterGrantPrefix = (() => {
	const pick = (lang) => (loadTable(lang, "ClientStrings").find((r) => r?.Id === "StatDescripotionTreeExpansionJewelGrantedSmallStat")?.Text || "");
	try {
		return { en: pick("English") || "Added Small Passive Skills grant: {0}", ko: pick("Korean") || "" };
	} catch {
		return { en: "Added Small Passive Skills grant: {0}", ko: "" };
	}
})();
const CLUSTER_SOCKETS = { Large: 2, Medium: 1, Small: 0 };
const CLUSTER_SIZE_ORDER = ["large", "medium", "small"];
// 주얼 색 · 심연 주얼 종류(10-20 C516) — 주얼 옵션은 not_str · not_dex · not_int 태그로, 심연 주얼은 abyss_jewel_melee 등으로 베이스마다 붙는 게
// 다르다(진홍 106 · 진청록 114 · 코발트 108묶음). 클래스 하나로 합치면 진홍 주얼에 코발트 전용 옵션이 붙는다 → 베이스 하나 = 변형 하나.
// 이 태그가 정확히 하나인 베이스만 제작 풀에 넣는다 — 분광 · 무궁한(셋) · 조립된 눈(abyss_jewel_special, 없음)은 고유 전용.
const JEWEL_VARIANT_TAGS = ["strjewel", "dexjewel", "intjewel", "abyss_jewel_melee", "abyss_jewel_ranged", "abyss_jewel_caster", "abyss_jewel_summoner"];
const JEWEL_CLASSES = new Set(["Jewel", "AbyssJewel"]);
const domainOf = (poolKey) => {
	const [cls, variant] = poolKey.split("|");
	return (variant || "").startsWith("cluster_") ? CLUSTER_DOMAIN : (CLASS_DOMAIN[cls] ?? 1);
};

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
	{ key: "eyrie", name: "Redeemer", nameKo: "대속자" }, // 공식 이름(BaseItemTypes "대속자의 엑잘티드 오브", 10-15)
	{ key: "basilisk", name: "Hunter", nameKo: "사냥꾼" },
	{ key: "adjudicator", name: "Warlord", nameKo: "전쟁군주" },
];
// 영향력 태그가 붙는 슬롯 태그 — CLASS_TAGS 의 슬롯 태그(첫 항목)를 쓴다(gloves/bow/wand …).
const influenceSlotTag = (itemClass) => (CLASS_TAGS[itemClass] || [])[0];

// (아이템 클래스 × 속성 변형 × 영향력)별 태그 집합. 무기/장신구는 변형이 없어 변형 키 "", 영향력 없음도 "".
const tagSetsByPool = new Map(); // "itemClass|variant|influence" → [{sig,tagSet}]
// 영향력 없는 풀의 태그 서명 → 그 서명을 가진 베이스 이름들. 같은 클래스라도 베이스마다 붙는 모드가 다르다
// (소환수 피해는 ring_can_roll_minion_modifiers 태그가 있는 반지에만 — 산호 반지엔 안 붙는다). 가이드 레어 목표용.
const basesBySig = new Map(); // "itemClass|variant|" → Map<sig, Set<baseName>>
const classLabel = new Map(); // itemClass명 → {name, nameKo}
const variantsByClass = new Map(); // itemClass명 → Map<variantKey, label>
const variantBases = new Map(); // "itemClass|variant" → Set<베이스 이름>(C516)
for (const b of baseItems) {
	if (!wantedClasses.has(b.itemClass)) continue; // 장비만
	const row = baseRowByName.get((b.name || "").toLowerCase());
	if (!row) continue;
	const tagSet = new Set((row.TagsKeys || []).map((i) => tags[i]?.Id).filter(Boolean));
	tagSet.add("default"); // 모든 베이스의 암묵 태그
	for (const t of CLASS_TAGS[b.itemClass] || []) tagSet.add(t); // 클래스 레벨 일반 태그
	// 주얼 · 심연 주얼(C516) — 색 · 종류 태그가 하나가 아니면(고유 전용 베이스) 건너뛴다. 군집 주얼은 아래에서 따로
	const jewelTags = JEWEL_CLASSES.has(b.itemClass) ? JEWEL_VARIANT_TAGS.filter((t) => tagSet.has(t)) : [];
	if (JEWEL_CLASSES.has(b.itemClass) && !isClusterBase(b) && jewelTags.length !== 1) continue;
	// 속성 변형(str_armour 등) → 일반 armour 파생
	const variantTag = jewelTags[0] || [...tagSet].find((t) => VARIANT_LABEL[t]) || "";
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
	if (variantTag) variantsByClass.get(b.itemClass).set(variantTag, VARIANT_LABEL[variantTag] || { name: b.name, nameKo: b.nameKo || b.name });
	// 변형 → 베이스 이름(C516) — 가져오기 · 레어 목표 · 베이스 고르기가 베이스로 변형을 찾는다
	if (variantTag) {
		if (!variantBases.has(b.itemClass + "|" + variantTag)) variantBases.set(b.itemClass + "|" + variantTag, new Set());
		variantBases.get(b.itemClass + "|" + variantTag).add(b.name);
	}

	if (isClusterBase(b)) {
		// 군집 주얼(C514) — 인챈트마다 풀 하나(영향력 없음만), 일반 주얼 풀("Jewel||")엔 넣지 않는다
		const def = clusterDefs?.[b.name];
		for (const [enchantTag, skill] of Object.entries(def?.skills || {})) {
			const variantKey = "cluster_" + String(def.size || "").toLowerCase() + "." + enchantTag;
			const finalTags = new Set(tagSet);
			finalTags.add(enchantTag);
			const stats = skill.statsKo?.length ? skill.statsKo : skill.stats || [];
			const head = (lang) => [
				...describe(new Map([["local_jewel_expansion_passive_node_count", def.minNodes]]), lang),
				...(CLUSTER_SOCKETS[def.size] ? describe(new Map([["local_jewel_expansion_jewels_count", CLUSTER_SOCKETS[def.size]]]), lang) : []),
			];
			const grantKo = skill.statsKo?.length && clusterGrantPrefix.ko ? skill.statsKo.map((x) => clusterGrantPrefix.ko.replace("{0}", x)) : null;
			variantsByClass.get(b.itemClass).set(variantKey, {
				name: b.name + ": " + (skill.name || enchantTag),
				nameKo: (b.nameKo || b.name) + ": " + stats.join(" / "),
				enchant: [...head("English"), ...(skill.enchant || (skill.stats || []).map((x) => clusterGrantPrefix.en.replace("{0}", x)))],
				enchantKo: grantKo ? [...head("Korean"), ...grantKo] : null,
			});
			const poolKey = b.itemClass + "|" + variantKey + "|";
			variantBases.set(b.itemClass + "|" + variantKey, new Set([b.name]));
			const sig = [...finalTags].sort().join(",");
			tagSetsByPool.set(poolKey, [{ sig, tagSet: finalTags }]);
			basesBySig.set(poolKey, new Map([[sig, new Set([b.name])]]));
		}
		continue;
	}

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
		if (!influence) {
			if (!basesBySig.has(poolKey)) basesBySig.set(poolKey, new Map());
			const bySig = basesBySig.get(poolKey);
			if (!bySig.has(sig)) bySig.set(sig, new Set());
			bySig.get(sig).add(b.name);
		}
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
// 군집 옵션(도메인 21)은 꼬리 밑줄이 여럿이거나 숫자 없이 밑줄만 붙어(GrantLife_ · GrantLife2_ · GrantLife3) 한 사다리가 갈라진다 → 밑줄 → 숫자 순으로 벗긴다(C514, 다른 도메인은 그대로)
const familyKey = (mod) =>
	(mod.Domain === CLUSTER_DOMAIN ? (mod.Id || "").replace(/_+$/, "").replace(/\d+$/, "") : (mod.Id || "").replace(/\d+_?$/, "")) || mod.Id;

// 1) 장비 도메인 접두/접미 크래프팅 모드 수집 (Name 없는 내부 모드 제외)
const GEN = { 1: "prefix", 2: "suffix" };
const WANTED_DOMAINS = new Set([1, ...Object.values(CLASS_DOMAIN), CLUSTER_DOMAIN]);
const candidatesByDomain = new Map(); // domain → [{mod,index}]
mods.forEach((mod, index) => {
	if (!WANTED_DOMAINS.has(mod.Domain) || !GEN[mod.GenerationType] || !mod.Name) return;
	if (!(mod.SpawnWeight_TagsKeys || []).length) return;
	let list = candidatesByDomain.get(mod.Domain);
	if (!list) {
		list = [];
		candidatesByDomain.set(mod.Domain, list);
	}
	list.push({ mod, index });
});

// 1a) 해제(Unveiled) 옵션 — 도메인 28("Chosen" · "of the Order", 장막 옵션을 해제할 때 셋 중 고르는 Jun 옵션, 10-15 C341). 장비와 같은 스폰웨이트 태그 매칭
const veiledCandidates = [];
mods.forEach((mod, index) => {
	if (mod.Domain !== 28 || !GEN[mod.GenerationType] || !(mod.SpawnWeight_TagsKeys || []).length) return;
	veiledCandidates.push({ mod, index });
});
const craftVeiled = new Map(); // poolKey → Map<modId, weight>
const craftVeiledMods = new Map(); // modId → {g, l, grp, t, name, nameKo, en, enMin, ko, koMin}
// 1a') 화석 추가 · 강제 옵션 — 도메인 16(델브: 연마한 화석 젬 레벨 · 공허의 화석 심연 홈 · 얽혀든 화석 바알 옵션, 10-15 C342). 장막과 같은 매칭
//      델브 도메인의 나머지(관통 · 흡수 등 130여 개)는 스폰 풀에서 빠진 레거시라(poedb) 화석이 직접 넣는 것(Added · ForcedAdd)만 남긴다
const fossilModIdx = new Set();
try {
	for (const r of loadTable("English", "DelveCraftingModifiers")) for (const k of [...(r.AddedModsKeys || []), ...(r.ForcedAddModsKeys || [])]) fossilModIdx.add(k);
} catch { /* 표 없으면 화석 옵션 생략 */ }
const delveCandidates = [];
mods.forEach((mod, index) => {
	if (mod.Domain !== 16 || !fossilModIdx.has(index) || !GEN[mod.GenerationType] || !(mod.SpawnWeight_TagsKeys || []).length) return;
	delveCandidates.push({ mod, index });
});
const craftFossil = new Map(); // poolKey → Map<modId, weight>
const craftFossilMods = new Map(); // modId → 장막과 같은 모양
// 1a'') 엘드리치 임플리싯(10-15 C343) — GenerationType 28 = 작열의 총주교, 29 = 세계 포식자. …EldritchImplicitN 의 N = 등급(1 하급 ~ 6 완벽한,
//       화폐 등급과 같다 — poedb "Adds a Lesser … implicit"). 스폰 태그 매칭은 같고(no_tier_* 태그는 아이템에 없어 건너뜀), g = exarch|eater
const mods0ById = new Map(mods.map((m) => [m.Id, m]));
const modsTable = mods; // 아래 출력 블록에서 mods 가 출력 객체로 가려진다(C388)
const eldritchCandidates = [];
mods.forEach((mod, index) => {
	if ((mod.GenerationType !== 28 && mod.GenerationType !== 29) || !/EldritchImplicit\d+$/.test(mod.Id || "") || !(mod.SpawnWeight_TagsKeys || []).length) return;
	eldritchCandidates.push({ mod, index });
});
const craftEldritch = new Map(); // poolKey → Map<modId, weight>
const craftEldritchMods = new Map();
// 바알 오브 부패 임플리싯(C358) — 제작 에뮬레이터의 "임플리싯 변경" 결과(장비 도메인 1, 풀별 가중치)
const craftCorrupted = new Map(); // poolKey → Map<modId, weight>
const craftCorruptedMods = new Map();

// 1b) 바알 오브 부패 임플리싯(GenerationType 5) — 접두/접미와 같은 스폰웨이트 태그 매칭을 쓰지만
// Name 이 빈 문자열이라 별도 수집. 영향력과 무관하게 붙으므로 영향력 없음("") 풀에만 담는다.
// 플라스크 인챈트(주입 gen21/점화 gen22 오브) — 도메인 1 이지만 flask 태그로만 스폰. 플라스크 풀에만 담는다.
const ENCHANT_GEN = { 21: { name: "Instilling", nameKo: "주입 오브" }, 22: { name: "Enkindling", nameKo: "점화 오브" } };
const enchantCandidates = [];
mods.forEach((mod, index) => {
	if (mod.Domain !== 1 || !ENCHANT_GEN[mod.GenerationType]) return;
	if (!(mod.SpawnWeight_TagsKeys || []).length) return;
	enchantCandidates.push({ mod, index });
});

const corruptedByDomain = new Map(); // domain → [{mod,index}] (주얼 도메인 10/13 에도 부패 임플리싯이 있다 — "부패한 피 면역" 등)
mods.forEach((mod, index) => {
	if (!WANTED_DOMAINS.has(mod.Domain) || mod.GenerationType !== 5) return;
	if (!(mod.SpawnWeight_TagsKeys || []).length) return;
	let list = corruptedByDomain.get(mod.Domain);
	if (!list) {
		list = [];
		corruptedByDomain.set(mod.Domain, list);
	}
	list.push({ mod, index });
});

// 2) (클래스×변형) 풀별 매칭 + 패밀리 구성
const families = new Map(); // famKey → {gen, essence, tiers: Map<index, tier>}
const perPool = new Map(); // "itemClass|variant" → Map<famKey, weight>
const craftPools = new Map(); // "itemClass|variant|influence" → Map<modId, weight>(그 풀 가중치, C335)
const craftMods = new Map(); // modId → {f, g, l, grp, t, ess}
const perPoolTiers = new Map(); // "itemClass|variant|" → Map<sig, Map<modType, {gen, families:Set, mods:[{id, ilvl, fam}]}>> (영향력 없는 풀만)
const corruptedPerPool = new Map(); // "itemClass|variant|" → Set<famKey>
const enchantPerPool = new Map(); // 플라스크 클래스 풀 → Set<famKey> (주입/점화 인챈트)
for (const [poolKey, tagSets] of tagSetsByPool) {
	if (poolKey.endsWith("|")) {
		// 플라스크 풀엔 인챈트(주입/점화) 매칭
		if (domainOf(poolKey) === 2) {
			const enchKeys = new Set();
			enchantPerPool.set(poolKey, enchKeys);
			for (const { mod, index } of enchantCandidates) {
				let weight = 0;
				for (const { tagSet } of tagSets) weight = Math.max(weight, spawnWeight(mod, tagSet));
				if (weight <= 0) continue;
				const key = familyKey(mod);
				enchKeys.add(key);
				let fam = families.get(key);
				if (!fam) {
					fam = { gen: "enchant", essence: false, tiers: new Map() };
					families.set(key, fam);
				}
				if (!fam.tiers.has(index)) {
					const label = ENCHANT_GEN[mod.GenerationType];
					fam.tiers.set(index, {
						id: mod.Id,
						name: label.name,
						nameKo: label.nameKo,
						ilvl: mod.Level,
						weight: weight,
						en: describe(rollValues(mod, "max"), "English"), ...remOf(), // 바로 위 영어 호출의 리마인더 Id(C131)
						enMin: describe(rollValues(mod, "min"), "English"),
						ko: describe(rollValues(mod, "max"), "Korean"),
						koMin: describe(rollValues(mod, "min"), "Korean"),
					});
				}
			}
		}
		// 영향력 없음 풀에만 부패 임플리싯 매칭
		const famKeys = new Set();
		corruptedPerPool.set(poolKey, famKeys);
		const corruptedCandidates = corruptedByDomain.get(domainOf(poolKey)) || [];
		for (const { mod, index } of corruptedCandidates) {
			let weight = 0;
			for (const { tagSet } of tagSets) weight = Math.max(weight, spawnWeight(mod, tagSet));
			if (weight <= 0) continue;
			const key = familyKey(mod);
			famKeys.add(key);
			let fam = families.get(key);
			if (!fam) {
				fam = { gen: "corrupted", essence: false, tiers: new Map() };
				families.set(key, fam);
			}
			if (!fam.tiers.has(index)) {
				fam.tiers.set(index, {
					id: mod.Id,
					name: "Corrupted",
					nameKo: "부패",
					ilvl: mod.Level,
					weight: weight,
					en: describe(rollValues(mod, "max"), "English"), ...remOf(), // 바로 위 영어 호출의 리마인더 Id(C131)
					enMin: describe(rollValues(mod, "min"), "English"),
					ko: describe(rollValues(mod, "max"), "Korean"),
					koMin: describe(rollValues(mod, "min"), "Korean"),
				});
			}
		}
	}
	const famWeights = new Map();
	perPool.set(poolKey, famWeights);
	const poolDomain = domainOf(poolKey);
	const candidates = candidatesByDomain.get(poolDomain) || [];
	// 바알 오브 부패 임플리싯(C358)은 플라스크(도메인 2 — 바알 오브를 못 쓴다)만 빼고 클래스 도메인별(주얼 10 · 심연 주얼 13)
	const extraSets = poolDomain === 1 ? [[veiledCandidates, craftVeiled, craftVeiledMods], [delveCandidates, craftFossil, craftFossilMods], [eldritchCandidates, craftEldritch, craftEldritchMods]] : [];
	if (poolDomain !== 2) extraSets.push([corruptedByDomain.get(poolDomain) || [], craftCorrupted, craftCorruptedMods]);
	{
		for (const [cands, outPool, outMods] of extraSets) for (const { mod, index } of cands) {
			let weight = 0;
			for (const { tagSet } of tagSets) weight = Math.max(weight, spawnWeight(mod, tagSet));
			if (weight <= 0) continue;
			if (!outPool.has(poolKey)) outPool.set(poolKey, new Map());
			outPool.get(poolKey).set(mod.Id, weight);
			if (!outMods.has(mod.Id)) {
				outMods.set(mod.Id, {
					g: GEN[mod.GenerationType] || (mod.GenerationType === 5 ? "corrupted" : undefined),
					l: mod.Level,
					grp: mod.Families || [],
					t: (mod.ImplicitTagsKeys || []).map((i) => tags[i]?.Id).filter(Boolean),
					name: mod.Name,
					nameKo: modsKo[index]?.Name || mod.Name,
					en: describe(rollValues(mod, "max"), "English"),
					enMin: describe(rollValues(mod, "min"), "English"),
					ko: describe(rollValues(mod, "max"), "Korean"),
					koMin: describe(rollValues(mod, "min"), "Korean"),
				});
			}
		}
	}
	for (const { mod, index } of candidates) {
		// 같은 변형 안의 베이스들끼리는 태그가 같으므로 max 로 합쳐도 섞이지 않는다
		let weight = 0;
		for (const { tagSet } of tagSets) weight = Math.max(weight, spawnWeight(mod, tagSet));
		if (weight <= 0) continue;
		const key = familyKey(mod);
		famWeights.set(key, Math.max(famWeights.get(key) || 0, weight));
		if (!/Royale/.test(mod.Id)) {
			if (!craftPools.has(poolKey)) craftPools.set(poolKey, new Map());
			craftPools.get(poolKey).set(mod.Id, weight);
			if (!craftMods.has(mod.Id)) {
				craftMods.set(mod.Id, {
					f: key,
					g: GEN[mod.GenerationType],
					l: mod.Level,
					grp: mod.Families || [],
					t: (mod.ImplicitTagsKeys || []).map((i) => tags[i]?.Id).filter(Boolean),
					...(mod.IsEssenceOnlyModifier ? { ess: true } : {}),
					...(modTypeNames[mod.ModTypeKey]?.Name ? { mt: modTypeNames[mod.ModTypeKey].Name } : {}),
				});
			}
		}
		// Royale(PvP 전용) 모드는 일반 모드와 ModTypeKey 가 같아 사다리에 섞인다(46곳) — 일반 레어엔 안 붙으므로 뺀다
		if (poolKey.endsWith("|") && !/Royale/.test(mod.Id)) {
			if (!perPoolTiers.has(poolKey)) perPoolTiers.set(poolKey, new Map());
			const bySig = perPoolTiers.get(poolKey);
			// 합집합(max) 말고 **서명마다** 판정한다 — 베이스별로 붙는 모드가 다르다
			for (const { sig, tagSet } of tagSets) {
				if (spawnWeight(mod, tagSet) <= 0) continue;
				if (!bySig.has(sig)) bySig.set(sig, new Map());
				const byType = bySig.get(sig);
				const type = mod.ModTypeKey;
				if (!byType.has(type)) byType.set(type, { gen: GEN[mod.GenerationType], families: new Set(), mods: [] });
				const g = byType.get(type);
				for (const f of mod.Families || []) g.families.add(f);
				if (!g.mods.some((x) => x.id === mod.Id)) g.mods.push({ id: mod.Id, ilvl: mod.Level, fam: key });
			}
		}
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
				en: describe(rollValues(mod, "max"), "English"), ...remOf(), // 바로 위 영어 호출의 리마인더 Id(C131)
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
// 풀(클래스|변형) → 접두/접미(+부패 임플리싯) 패밀리 키
const outPools = {};
for (const [poolKey, famWeights] of perPool) {
	const corrupted = [...(corruptedPerPool.get(poolKey) || [])].filter((k) => outFamilies[k]?.gen === "corrupted").sort();
	const enchants = [...(enchantPerPool.get(poolKey) || [])].filter((k) => outFamilies[k]?.gen === "enchant").sort();
	outPools[poolKey] = {
		prefixes: [...famWeights.keys()].filter((k) => outFamilies[k]?.gen === "prefix").sort(),
		suffixes: [...famWeights.keys()].filter((k) => outFamilies[k]?.gen === "suffix").sort(),
		...(corrupted.length ? { corrupted } : {}),
		...(enchants.length ? { enchants } : {}),
	};
}
// 클래스 목록 — 변형이 여럿이면 variants 로 노출(UI 가 하위 탭을 만든다). 변형 없으면 빈 목록.
const VARIANT_ORDER = ["str_armour", "dex_armour", "int_armour", "str_dex_armour", "str_int_armour", "dex_int_armour", "str_dex_int_armour", ...JEWEL_VARIANT_TAGS];
const outClasses = [...classLabel.keys()]
	.map((itemClass) => {
		const variantMap = new Map(variantsByClass.get(itemClass) || new Map());
		// 군집 주얼 변형이 있는 클래스(Jewel)는 일반 주얼("" 풀)을 첫 변형으로(C514) — 크기 순 → 이름 순
		const hasCluster = [...variantMap.keys()].some((k) => k.startsWith("cluster_"));
		if (hasCluster && outPools[itemClass + "||"]) variantMap.set("", { name: itemClass, nameKo: classLabel.get(itemClass)?.nameKo || itemClass });
		const variantRank = (k) => (k === "" ? -1 : k.startsWith("cluster_") ? 100 + CLUSTER_SIZE_ORDER.indexOf(k.slice(8, k.indexOf("."))) : VARIANT_ORDER.indexOf(k));
		const variants = [...variantMap.entries()]
			.sort((a, b) => variantRank(a[0]) - variantRank(b[0]) || (a[0].startsWith("cluster_") ? String(a[1].name).localeCompare(String(b[1].name)) : 0))
			.map(([key, label]) => {
				const pool = outPools[itemClass + "|" + key + "|"] || { prefixes: [], suffixes: [] };
				const vb = variantBases.get(itemClass + "|" + key);
				return { key, name: label.name, nameKo: label.nameKo, prefixCount: pool.prefixes.length, suffixCount: pool.suffixes.length, ...(vb ? { bases: [...vb].sort() } : {}), ...(label.enchant ? { enchant: label.enchant, enchantKo: label.enchantKo || label.enchant } : {}) };
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

// 풀 밖 옵션의 영 · 한 쌍(10-04 C135, PoE2 C133 짝) — 빌드 화면 번역 사전 보강. 플라스크 · 제작대 · 엘드리치 · 심연 홈처럼 패밀리 풀에 없는 옵션이
//   실빌드에 붙어 영어로 남았다(ninja 59빌드 레어 줄 3349 중 568). 티어와 같은 describe(최대 · 최소롤), 영어 틀(숫자 → #)이 이미 풀에 있으면 뺀다
const NUM = /-?\d+(?:\.\d+)?/g;
const poolTemplates = new Set(Object.values(outFamilies).flatMap((f) => f.tiers.flatMap((t) => [...(t.en || []), ...(t.enMin || [])].map((l) => l.replace(NUM, "#")))));
const extraSeen = new Set();
const extra = [];
mods.forEach((mod, index) => {
	if (mod.StatsKey1 == null) return;
	const en = describe(rollValues(mod, "max"), "English");
	const remIds = describe.reminders.map((l) => l.filter((id) => reminderTextById.has(id))); // 바로 위 영어 호출 기준(C141)
	const ko = describe(rollValues(mod, "max"), "Korean");
	if (!en.length || en.length !== ko.length || en.join() === ko.join()) return;
	const key = en.map((l) => l.replace(NUM, "#")).join("|");
	if (extraSeen.has(key) || en.every((l) => poolTemplates.has(l.replace(NUM, "#")))) return;
	extraSeen.add(key);
	// 리마인더도(10-04 C141) — 빌드 화면 고유 줄 · 풀 밖 줄의 회색 부연
	const rem = remIds.some((l) => l.length) ? remIds : null;
	if (rem) rem.flat().forEach((id) => usedReminderIds.add(id));
	extra.push({ en, ko, enMin: describe(rollValues(mod, "min"), "English"), koMin: describe(rollValues(mod, "min"), "Korean"), ...(rem ? { rem } : {}) });
	void index;
});
console.log(`풀 밖 옵션 쌍: ${extra.length}개`);
// 옵션 이름 영 · 한 전부(접두 · 접미 = GenerationType 1 · 2) — 빌드 화면 마법 아이템 이름(10-04 C146). 티어 이름에 없는 제작대 접미("of Craft" → "- 제작") 등
const affixNames = {};
mods.forEach((mod, index) => {
	if (mod.GenerationType !== 1 && mod.GenerationType !== 2) return;
	const k = modsKo[index]?.Name;
	if (mod.Name && k && !(mod.Name in affixNames)) affixNames[mod.Name] = k;
});
console.log(`옵션 이름: ${Object.keys(affixNames).length}개`);
const reminderText = Object.fromEntries([...usedReminderIds].sort().map((id) => [id, reminderTextById.get(id)]));
console.log(`리마인더: 티어 ${Object.values(outFamilies).reduce((n, f) => n + f.tiers.filter((t) => t.rem).length, 0)}개 · 문구 ${usedReminderIds.size}종`);
const result = { patch: loadConfig().patch, itemClasses: outClasses, pools: outPools, families: outFamilies, reminderText, extra, affixNames };
fs.writeFileSync(OUT, JSON.stringify(result));
// 풀별 묶음 — ModTypeKey 하나 = 티어 사다리 하나(ilvl 내림차순, 상위 먼저). 서술이 없어 families 에서 빠진 티어는 뺀다.
// key 는 사람이 읽는 이름(구성 티어의 familyKey 중 가장 많은 것 — 억제는 ChanceToSuppressSpells), families 는 동시 부착 판정용 게임 값.
const outPoolTiers = {};
const hasText = (m) => (outFamilies[m.fam]?.tiers || []).some((t) => t.id === m.id);
for (const [poolKey, bySig] of perPoolTiers) {
	const sigs = [];
	for (const [sig, byType] of bySig) {
		const groups = [];
		for (const [type, g] of byType) {
			if (g.gen !== "prefix" && g.gen !== "suffix") continue;
			const mods2 = g.mods.filter(hasText).sort((a, b) => b.ilvl - a.ilvl);
			if (!mods2.length) continue;
			const counts = new Map();
			for (const m of mods2) counts.set(m.fam, (counts.get(m.fam) || 0) + 1);
			const key = [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))[0][0];
			groups.push({ key, gen: g.gen, modType: type, families: [...g.families].sort((a, b) => a - b), tiers: mods2.map((m) => m.id) });
		}
		groups.sort((a, b) => a.key.localeCompare(b.key));
		sigs.push({ bases: [...(basesBySig.get(poolKey)?.get(sig) || [])].sort(), groups });
	}
	outPoolTiers[poolKey] = sigs;
}
fs.writeFileSync(OUT_TIERS, JSON.stringify({ patch: loadConfig().patch, pools: outPoolTiers }));
// 지배의 오브(10-15 C345) — 영향력 풀에만 있는 옵션(같은 클래스 · 변형의 영향력 없는 풀에 없는 것)을 ModTypeKey 사다리로 묶어 "한 등급 위" id.
//   최고 등급이면 고양 옵션(…Maven, 같은 ModTypeKey, 스폰 태그 끝이 _<영향력>, 가중치 0 이라 풀엔 없다 — poedb "Upgrading a modifier of the
//   highest tier transforms the modifier into an Elevated Modifier"). 고양 옵션 문구도 여기서.
const INFLUENCE_KEYS = ["shaper", "elder", "crusader", "eyrie", "basilisk", "adjudicator"];
const elevatedByType = new Map(); // `${ModTypeKey}|${영향력}` → {mod, index}
mods.forEach((mod, index) => {
	if (mod.Domain !== 1 || !/Maven/.test(mod.Id || "") || !GEN[mod.GenerationType]) return;
	const inf = (mod.SpawnWeight_TagsKeys || []).map((k) => tags[k]?.Id || "").map((t) => t.split("_").pop()).find((t) => INFLUENCE_KEYS.includes(t));
	if (inf) elevatedByType.set(`${mod.ModTypeKey}|${inf}`, { mod, index });
});
const craftUpgrade = new Map(); // poolKey → {id: 다음 id}
const craftElevated = new Map(); // 고양 옵션 id → 문구
for (const [poolKey, ws] of craftPools) {
	const [cls, variant, inf] = poolKey.split("|");
	if (!inf || !INFLUENCE_KEYS.includes(inf)) continue;
	const base = craftPools.get(`${cls}|${variant}|`) || new Map();
	const byType = new Map();
	for (const id of ws.keys()) {
		if (base.has(id)) continue;
		const m = mods0ById.get(id);
		if (!m) continue;
		if (!byType.has(m.ModTypeKey)) byType.set(m.ModTypeKey, []);
		byType.get(m.ModTypeKey).push(m);
	}
	const next = {};
	for (const [type, list] of byType) {
		list.sort((a, b) => a.Level - b.Level);
		list.forEach((m, i) => {
			if (i + 1 < list.length) next[m.Id] = list[i + 1].Id;
			else {
				const el = elevatedByType.get(`${type}|${inf}`);
				if (!el) return;
				next[m.Id] = el.mod.Id;
				if (!craftElevated.has(el.mod.Id))
					craftElevated.set(el.mod.Id, {
						g: GEN[el.mod.GenerationType],
						l: el.mod.Level,
						grp: el.mod.Families || [],
						t: (el.mod.ImplicitTagsKeys || []).map((k) => tags[k]?.Id).filter(Boolean),
						name: el.mod.Name,
						nameKo: modsKo[el.index]?.Name || el.mod.Name,
						en: describe(rollValues(el.mod, "max"), "English"),
						enMin: describe(rollValues(el.mod, "min"), "English"),
						ko: describe(rollValues(el.mod, "max"), "Korean"),
						koMin: describe(rollValues(el.mod, "min"), "Korean"),
					});
			}
		});
	}
	craftUpgrade.set(poolKey, next);
}
{
	// 제작용(C335) — 문구가 있는 계열(outFamilies)의 옵션만
	const mods = {};
	for (const [id, m] of craftMods) if (outFamilies[m.f]) mods[id] = m;
	const pools = {};
	for (const [k, ws] of craftPools) pools[k] = Object.fromEntries([...ws].filter(([id]) => mods[id]));
	// 해제 옵션(C341) — 문구가 있는 것만
	const veiledMods = {};
	for (const [id, m] of craftVeiledMods) if (m.en?.length) veiledMods[id] = m;
	const veiled = {};
	for (const [k, ws] of craftVeiled) veiled[k] = Object.fromEntries([...ws].filter(([id]) => veiledMods[id]));
	// 화석 추가 · 강제 옵션(C342)
	const fossilMods = {};
	for (const [id, m] of craftFossilMods) if (m.en?.length) fossilMods[id] = m;
	const fossil = {};
	for (const [k, ws] of craftFossil) fossil[k] = Object.fromEntries([...ws].filter(([id]) => fossilMods[id]));
	// 엘드리치(C343) — g = 진영, tier = 이름 끝 숫자
	const eldritchMods = {};
	for (const [id, m] of craftEldritchMods) {
		if (!m.en?.length) continue;
		const src = mods0ById.get(id);
		eldritchMods[id] = { ...m, g: src.GenerationType === 28 ? "exarch" : "eater", tier: parseInt(id.match(/(\d+)$/)[1], 10) };
	}
	const eldritch = {};
	for (const [k, ws] of craftEldritch) eldritch[k] = Object.fromEntries([...ws].filter(([id]) => eldritchMods[id]));
	// 지배의 오브(C345) — 다음 등급이 문구 있는 옵션(제작 옵션 또는 고양 옵션)인 것만
	const elevatedMods = {};
	for (const [id, m] of craftElevated) if (m.en?.length) elevatedMods[id] = m;
	const upgrade = {};
	for (const [k, nx] of craftUpgrade) {
		const kept = Object.fromEntries(Object.entries(nx).filter(([a, b]) => mods[a] && (mods[b] || elevatedMods[b])));
		if (Object.keys(kept).length) upgrade[k] = kept;
	}
	// 바알 오브 부패 임플리싯(C358)
	const corruptedMods = {};
	for (const [id, m] of craftCorruptedMods) if (m.en?.length) corruptedMods[id] = m;
	const corrupted = {};
	for (const [k, ws] of craftCorrupted) corrupted[k] = Object.fromEntries([...ws].filter(([id]) => corruptedMods[id]));
	// 짐승 제작 위상 스킬(C388, BestiaryRecipes EinharMasterCraftMorrigan1~4 "30레벨 … 위상 스킬") — 제작 접미어, 스폰 가중치 없음(레시피로만)
	const beastMods = {};
	// 두카트 제작(C391, 군왕호 올플레임 — 도메인 42): 부패모의 두카트 = 희귀 허리띠에 덫 · 지뢰 옵션(태그 deepwater_hazard_belt 가중치),
	//   브라인훅의 두카트 = 판테온 위상 스킬(GrantsXAspectCrafted, deepwater_pantheon_aspect) — 위상은 beastMods 에 함께
	const hazardMods = {};
	const hazardTag = tags.findIndex((x) => x?.Id === "deepwater_hazard_belt");
	modsTable.forEach((mod, index) => {
		const k = (mod.SpawnWeight_TagsKeys || []).indexOf(hazardTag);
		if (mod.Domain !== 42 || k < 0) return;
		hazardMods[mod.Id] = {
			g: GEN[mod.GenerationType],
			l: mod.Level,
			grp: mod.Families || [],
			t: (mod.ImplicitTagsKeys || []).map((i) => tags[i]?.Id).filter(Boolean),
			name: mod.Name,
			nameKo: modsKo[index]?.Name || mod.Name,
			en: describe(rollValues(mod, "max"), "English"),
			enMin: describe(rollValues(mod, "min"), "English"),
			ko: describe(rollValues(mod, "max"), "Korean"),
			koMin: describe(rollValues(mod, "min"), "Korean"),
			w: mod.SpawnWeight_Values[k],
		};
	});
	// 사이악산의 두카트(C393) "악명 높은 속성" — 도메인 41(트라투스 용병 옵션), 부위 태그 가중치(장화 · 장갑 …). cls = 그 태그
	const infamousMods = {};
	modsTable.forEach((mod, index) => {
		if (mod.Domain !== 41) return;
		const ks = mod.SpawnWeight_TagsKeys || [];
		const cls = ks.map((k, i) => (mod.SpawnWeight_Values[i] > 0 ? tags[k]?.Id : null)).filter((x) => x && x !== "default");
		if (!cls.length) return;
		infamousMods[mod.Id] = {
			g: GEN[mod.GenerationType],
			l: mod.Level,
			grp: mod.Families || [],
			t: (mod.ImplicitTagsKeys || []).map((i) => tags[i]?.Id).filter(Boolean),
			name: mod.Name,
			nameKo: modsKo[index]?.Name || mod.Name,
			en: describe(rollValues(mod, "max"), "English"),
			enMin: describe(rollValues(mod, "min"), "English"),
			ko: describe(rollValues(mod, "max"), "Korean"),
			koMin: describe(rollValues(mod, "min"), "Korean"),
			cls,
			w: Math.max(...ks.map((k, i) => (cls.includes(tags[k]?.Id) ? mod.SpawnWeight_Values[i] : 0))),
		};
	});
	modsTable.forEach((mod, index) => {
		if (!/^Grants((Cat|Bird|Spider|Crab)AspectCrafted30|(BrineKing|Lunaris|Solaris|Arakaali)AspectCrafted)$/.test(mod.Id || "")) return;
		beastMods[mod.Id] = {
			g: GEN[mod.GenerationType],
			l: mod.Level,
			grp: mod.Families || [],
			t: (mod.ImplicitTagsKeys || []).map((i) => tags[i]?.Id).filter(Boolean),
			name: mod.Name,
			nameKo: modsKo[index]?.Name || mod.Name,
			en: describe(rollValues(mod, "max"), "English"),
			enMin: describe(rollValues(mod, "min"), "English"),
			ko: describe(rollValues(mod, "max"), "Korean"),
			koMin: describe(rollValues(mod, "min"), "Korean"),
		};
	});
	// 옵션 태그 표시 이름(10-20 C480 — 사용자 요청 "옵션의 태그도 보여주면") — Tags.Name(이름이 있는 태그만 인게임 상세 보기에 " — 생명력, 방어력"),
	// tagNames[id] = [영어, 한국어]. 옵션의 태그 순서는 mods[id].t(ImplicitTags)
	const tagsKo = loadTable("Korean", "Tags");
	const tagNames = {};
	tags.forEach((t, i) => {
		if (t && t.Name) tagNames[t.Id] = [t.Name, (tagsKo[i] && tagsKo[i].Name) || t.Name];
	});
	fs.writeFileSync(OUT_CRAFT, JSON.stringify({ patch: loadConfig().patch, mods, pools, veiledMods, veiled, fossilMods, fossil, eldritchMods, eldritch, elevatedMods, upgrade, corruptedMods, corrupted, beastMods, hazardMods, infamousMods, tagNames }));
	console.log(`craft-mods.json: 옵션 ${Object.keys(mods).length}개 · 풀 ${Object.keys(pools).length}개 · 해제 옵션 ${Object.keys(veiledMods).length}개(풀 ${Object.keys(veiled).length}) · 화석 옵션 ${Object.keys(fossilMods).length}개(풀 ${Object.keys(fossil).length}) · 엘드리치 ${Object.keys(eldritchMods).length}개(풀 ${Object.keys(eldritch).length}) · 고양 ${Object.keys(elevatedMods).length}개(올림 풀 ${Object.keys(upgrade).length}) · 부패 임플리싯 ${Object.keys(corruptedMods).length}개(풀 ${Object.keys(corrupted).length}) → ${OUT_CRAFT}`);
}
console.log(`mod-pool-tiers.json: 풀 ${Object.keys(outPoolTiers).length}개 → ${OUT_TIERS}`);
const famCount = Object.keys(outFamilies).length;
const tierCount = Object.values(outFamilies).reduce((n, f) => n + f.tiers.length, 0);
console.log(`mods.json: 클래스 ${outClasses.length}개(풀 ${Object.keys(outPools).length}), 패밀리 ${famCount}개, 티어 ${tierCount}개 → ${OUT}`);
const infSample = outClasses.find((c) => c.influences?.length);
if (infSample) console.log(`영향력 예(${infSample.nameKo}):`, infSample.influences.map((i) => `${i.nameKo}+${i.extraCount}`).join(" · "));
const sample = outClasses.find((c) => c.itemClass === "Gloves");
if (sample) console.log("Gloves 변형:", sample.variants.map((v) => `${v.nameKo} 접두${v.prefixCount}/접미${v.suffixCount}`).join(" · "));
