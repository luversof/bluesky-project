// PoE2 일반(베이스) 아이템 → ~/.poe-gamedata/poe2/base-items.json
// 목록은 PoB-PoE2 Data/Bases/*.lua 에서 `hidden = true` 가 아닌 것(미출시·내부 베이스 제외), 수치·한국어·아이콘 경로는 게임 테이블에서 붙인다.
import { createDescriber, createTranslator, makeSlugger, pobFile, stripMarkup } from "./common2.mjs";
import { loadConfig, loadTable, writeJson } from "./paths.mjs";

const FILES = [
	"amulet", "axe", "belt", "body", "boots", "bow", "claw", "crossbow", "dagger", "fishing", "flail", "flask", "focus",
	"gloves", "helmet", "jewel", "mace", "quiver", "ring", "sceptre", "shield", "spear", "staff", "sword", "talisman",
	"traptool", "wand",
];

// PoB 베이스 블록 → { name, type, subType, hidden, implicit, req, tags }
function parseBases(lua) {
	const out = [];
	for (const m of lua.matchAll(/itemBases\["([^"]+)"\] = \{([\s\S]*?)\n\}/g)) {
		const body = m[2];
		const str = (k) => (body.match(new RegExp(`\\n\\t${k} = "([^"]*)"`)) || [])[1] ?? null;
		const req = {};
		const reqM = body.match(/\n\treq = \{([^}]*)\}/);
		if (reqM) for (const r of reqM[1].matchAll(/(\w+) = (\d+)/g)) req[r[1]] = Number(r[2]);
		const tags = [];
		const tagM = body.match(/\n\ttags = \{([^}]*)\}/);
		if (tagM) for (const t of tagM[1].matchAll(/(\w+) = true/g)) tags.push(t[1]);
		out.push({ name: m[1], type: str("type"), subType: str("subType"), hidden: /\n\thidden = true/.test(body), implicit: str("implicit"), req, tags });
	}
	return out;
}

const T = (t) => [loadTable("English", t), loadTable("Korean", t)];
const [bases, basesKo] = T("BaseItemTypes");
const [classes, classesKo] = T("ItemClasses");
const [mods] = T("Mods");
const [stats] = T("Stats");
const [armours] = T("ArmourTypes");
const [weapons] = T("WeaponTypes");
const [shields] = T("ShieldTypes");
const [attrs] = T("AttributeRequirements");
const [flasks] = T("Flasks");
const [visuals] = T("ItemVisualIdentity");
const byBase = (rows) => new Map(rows.map((r) => [r.BaseItemType, r]));
const armourOf = byBase(armours), weaponOf = byBase(weapons), shieldOf = byBase(shields), attrOf = byBase(attrs), flaskOf = byBase(flasks);
const baseIdxByName = new Map();
bases.forEach((b, i) => {
	if (!baseIdxByName.has(b.Name)) baseIdxByName.set(b.Name, i);
});
const describe = createDescriber();
const translateMods = createTranslator();
// "Grants Skill: Parry" 는 스탯 설명이 아니라 ClientStrings(ItemDisplayGrantedSkill) 문구 — 스킬 이름만 한국어로 바꿔 끼운다
const [activeSkills, activeSkillsKo] = T("ActiveSkills");
const skillKo = new Map();
activeSkills.forEach((a, i) => {
	const en = stripMarkup(a.DisplayedName || ""), ko = stripMarkup(activeSkillsKo[i].DisplayedName || "");
	if (en && ko && !skillKo.has(en)) skillKo.set(en, ko);
});
const translate = (lines) =>
	lines.map((line) => {
		const g = stripMarkup(line).match(/^Grants Skill: (?:Level (\d+|\(\d+-\d+\)) )?(.+)$/);
		if (g && skillKo.has(g[2])) return "스킬 부여: " + (g[1] ? g[1] + "레벨 " : "") + skillKo.get(g[2]);
		return translateMods([line])[0];
	});

/** 큰 분류 — 목록 화면 탭. */
const CATEGORY = {
	"One Hand Sword": "weapon", "Two Hand Sword": "weapon", "One Hand Axe": "weapon", "Two Hand Axe": "weapon",
	"One Hand Mace": "weapon", "Two Hand Mace": "weapon", Claw: "weapon", Dagger: "weapon", Wand: "weapon", Staff: "weapon",
	Warstaff: "weapon", Bow: "weapon", Crossbow: "weapon", Spear: "weapon", Flail: "weapon", Sceptre: "weapon", Talisman: "weapon",
	TrapTool: "weapon", FishingRod: "weapon",
	Shield: "offhand", Buckler: "offhand", Focus: "offhand", Quiver: "offhand",
	Helmet: "armour", "Body Armour": "armour", Gloves: "armour", Boots: "armour",
	Amulet: "jewellery", Ring: "jewellery", Belt: "jewellery",
	LifeFlask: "flask", ManaFlask: "flask", UtilityFlask: "flask",
	Jewel: "jewel",
};

const implicitLines = (baseIdx, lang) => {
	const out = [];
	for (const mi of bases[baseIdx].Implicit_Mods || []) {
		const m = mods[mi];
		if (!m) continue;
		const values = new Map();
		for (let i = 1; i <= 6; i++) {
			if (m["Stat" + i] == null) continue;
			const v = m["Stat" + i + "Value"];
			values.set(stats[m["Stat" + i]].Id, Array.isArray(v) ? v[0] : v); // 베이스 암시는 고정값
		}
		out.push(...describe(values, lang));
	}
	return out;
};

const slug = makeSlugger();
const items = [];
const classUse = new Map();
let hidden = 0;
for (const file of FILES) {
	for (const pb of parseBases(await pobFile(`src/Data/Bases/${file}.lua`))) {
		if (pb.hidden) {
			hidden++;
			continue;
		}
		const i = baseIdxByName.get(pb.name);
		if (i == null) continue;
		const b = bases[i];
		const cls = classes[b.ItemClass];
		const a = armourOf.get(i), w = weaponOf.get(i), s = shieldOf.get(i), r = attrOf.get(i), f = flaskOf.get(i);
		const en = implicitLines(i, "English");
		const ko = implicitLines(i, "Korean");
		const implicits = en.length
			? en.map((line, k) => ({ en: line, ko: ko[k] || line }))
			: pb.implicit
				? pb.implicit.split("\\n").map((line) => ({ en: stripMarkup(line), ko: translate([line])[0] }))
				: [];
		const item = {
			name: b.Name,
			nameKo: basesKo[i].Name || b.Name,
			slug: slug(b.Name),
			itemClass: cls.Id,
			itemClassKo: classesKo[b.ItemClass].Name || cls.Name,
			category: CATEGORY[cls.Id] || "other",
			dropLevel: b.DropLevel,
			reqLevel: pb.req.level ?? b.DropLevel,
			reqStr: r?.ReqStr ?? pb.req.str ?? 0,
			reqDex: r?.ReqDex ?? pb.req.dex ?? 0,
			reqInt: r?.ReqInt ?? pb.req.int ?? 0,
			width: b.Width,
			height: b.Height,
			armour: a || s ? { armour: a?.Armour ?? 0, evasion: a?.Evasion ?? 0, energyShield: a?.EnergyShield ?? 0, movementPenalty: a ? -(a.IncreasedMovementSpeed || 0) / 100 : 0, block: s?.Block ?? 0 } : null,
			weapon: w ? { damageMin: w.DamageMin, damageMax: w.DamageMax, critChance: w.CritChance / 100, attacksPerSecond: Math.round((1000 / w.Speed) * 100) / 100, range: w.RangeMax, reloadTime: w.ReloadTime || null } : null,
			flask: f ? { type: f.Type, lifePerUse: f.LifePerUse, manaPerUse: f.ManaPerUse, recoverySeconds: f.RecoveryTime / 10 } : null,
			implicits,
			tags: pb.tags,
			subType: pb.subType,
			icon: b.ItemVisualIdentity != null ? visuals[b.ItemVisualIdentity]?.DDSFile || null : null,
		};
		items.push(item);
		classUse.set(cls.Id, { key: cls.Id, ko: item.itemClassKo, category: item.category });
	}
}
items.sort((a, b) => a.itemClass.localeCompare(b.itemClass) || a.reqLevel - b.reqLevel || a.name.localeCompare(b.name));
const file = writeJson("base-items.json", { patch: loadConfig().patch, classes: [...classUse.values()], items });
const cats = items.reduce((m, x) => ((m[x.category] = (m[x.category] || 0) + 1), m), {});
console.log(`[poe2 bases] ${items.length}개(숨김 ${hidden} 제외) → ${file}`, cats);
