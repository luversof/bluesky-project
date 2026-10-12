// PoE2 일반(베이스) 아이템 → ~/.poe-gamedata/poe2/base-items.json
// 목록은 PoB-PoE2 Data/Bases/*.lua 에서 `hidden = true` 가 아닌 것(미출시·내부 베이스 제외), 수치·한국어·아이콘 경로는 게임 테이블에서 붙인다.
import { CSD_CHAINS, createDescriber, createKeywordIndex, createTranslator, loadKeywords, makeSlugger, pobFile, stripMarkup } from "./common2.mjs";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable, writeJson } from "./paths.mjs";
import fs from "node:fs";
import path from "node:path";

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
		// 룬 수호(Runic Ward) — 추출한 ArmourTypes 표엔 수호 열이 없어 PoB 베이스 armour = { …, Ward = N } 에서 읽는다(10-08 C185)
		const ward = Number((body.match(/\n\tarmour = \{[^}]*\bWard = (\d+)/) || [])[1]) || 0;
		// 정신력(Spirit) — 셉터의 베이스 속성(인게임 "정신력: 100", 10-08 C188). 게임 표 대신 PoB 베이스 spirit = N 에서 읽는다
		const spirit = Number((body.match(/\n\tspirit = (\d+)/) || [])[1]) || 0;
		// 플라스크 · 호신부 충전 소모 · 최대와 호신부 버프 줄(10-08 C189 — 인게임 "사용 시 충전 40회 중 30회 소모" · 버프 "+18% to Chaos Resistance"). 게임 Flasks 표엔 충전이 없어 PoB 베이스에서
		const fc = (body.match(/\n\t(?:flask|charm) = \{(.*)\},?(?:\n|$)/) || [])[1] || "";
		const chargesUsed = Number((fc.match(/chargesUsed = (\d+)/) || [])[1]) || 0;
		const chargesMax = Number((fc.match(/chargesMax = (\d+)/) || [])[1]) || 0;
		const buffM = fc.match(/buff = \{([^}]*)\}/);
		const buff = buffM ? [...buffM[1].matchAll(/"([^"]*)"/g)].map((x) => x[1]) : [];
		out.push({ name: m[1], type: str("type"), subType: str("subType"), hidden: /\n\thidden = true/.test(body), implicit: str("implicit"), req, tags, ward, spirit, chargesUsed, chargesMax, buff });
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
// 같은 이름 베이스가 여럿이다(10-08 C180 — 토너먼트 사슬옷 ×3 = 화염 · 냉기 · 번개 저항 암시, 룬벼림 짧은 활 ×2 = 암시 · 요구 레벨이 다름).
//   예전엔 이름 → 첫 행 하나라 PoB 의 세 항목이 모두 같은 게임 행(화염 저항)을 받았다 → 이름이 같은 행 목록을 두고 PoB 항목마다 암시 · 요구 레벨이 맞는 행을 한 번씩 고른다
const baseIdxsByName = new Map();
bases.forEach((b, i) => {
	if (!b.Name) return;
	if (!baseIdxsByName.has(b.Name)) baseIdxsByName.set(b.Name, []);
	baseIdxsByName.get(b.Name).push(i);
});
const usedBaseIdx = new Set();
const describe = createDescriber();
const translateMods = createTranslator();
// "Grants Skill: Parry" 는 스탯 설명이 아니라 ClientStrings(ItemDisplayGrantedSkill) 문구 — 스킬 이름만 한국어로 바꿔 끼운다
const [activeSkills, activeSkillsKo] = T("ActiveSkills");
const skillKo = new Map();
// 젬 아이템 이름이 먼저(10-10 C252 — 게임 · poe2db "스킬 부여: 방패 들기"(젬 이름), 스킬 표시 이름은 "방패 올리기". 마나 고갈/흡수도 같은 경우)
const gemKo = new Map();
bases.forEach((b, i) => {
	const en = stripMarkup(b.Name || ""), ko = stripMarkup(basesKo[i]?.Name || "");
	if (/\/Gems?\//i.test(b.Id || "") && en && ko && !/^\[DNT/.test(en) && !gemKo.has(en)) gemKo.set(en, ko);
});
// PoB 는 소환수 스킬을 "Skeletal Warrior Minion" 으로 적는데 무기가 주는 젬은 "Skeletal Warrior"(SkillGemSkeletalWarriorWeaponSkill, poe2db 헌신의 셉터 "스킬 부여: 해골 전사") — 꼬리를 떼고도 찾는다(C255)
const grantedNameKo = (en) => gemKo.get(en) || gemKo.get(en.replace(/ Minion$/, "")) || skillKo.get(en);
activeSkills.forEach((a, i) => {
	const en = stripMarkup(a.DisplayedName || ""), ko = stripMarkup(activeSkillsKo[i].DisplayedName || "");
	if (en && ko && !skillKo.has(en)) skillKo.set(en, ko);
});
const translate = (lines) =>
	lines.map((line) => {
		const g = stripMarkup(line).match(/^Grants Skill: (?:Level (\d+|\(\d+-\d+\)) )?(.+)$/);
		if (g && grantedNameKo(g[2])) return "스킬 부여: " + (g[1] ? g[1] + "레벨 " : "") + grantedNameKo(g[2]);
		return translateMods([line])[0];
	});

// 호신부 버프 줄 번역(10-08 C189) — item 사슬 번역기는 저항 · 면역 줄("+25% to Fire Resistance")을 못 옮긴다(고유 파서는 더 넓은 사슬을 쓴다).
//   못 옮기면 지난 산출물(고유 · 옵션 풀)의 영/한 쌍에서 숫자 자리를 # 으로 맞춰 찾고 숫자를 채운다. 그래도 없으면 영어 그대로
const NUMTOK = /[+-]?\(?-?\d+(?:\.\d+)?(?:--?\d+(?:\.\d+)?)?\)?/g;
const koPairs = (() => {
	const pairs = new Map();
	const add = (en, ko) => {
		if (!en || !ko || en === ko) return;
		const key = en.replace(NUMTOK, "#");
		if (!pairs.has(key)) pairs.set(key, ko);
	};
	const read = (f) => {
		try {
			return JSON.parse(fs.readFileSync(path.join(DATA_DIR, f), "utf8"));
		} catch {
			return null;
		}
	};
	const u = read("uniques.json");
	for (const x of (u && (u.items || u)) || []) {
		(x.explicits || []).forEach((en, i) => add(en, (x.explicitsKo || [])[i]));
		(x.implicits || []).forEach((en, i) => add(en, (x.implicitsKo || [])[i]));
	}
	const walk = (o) => {
		if (Array.isArray(o)) o.forEach(walk);
		else if (o && typeof o === "object") {
			if (Array.isArray(o.text) && Array.isArray(o.textKo)) o.text.forEach((en, i) => add(en, o.textKo[i]));
			Object.values(o).forEach(walk);
		}
	};
	walk(read("mods.json"));
	// 게임 스탯 설명(stat_descriptions.csd)의 숫자 없는 한 줄 문구 — 마크업을 벗겨 영/한 쌍으로("Immune to [Freeze]" → "[Freeze|동결]에 면역" → 동결에 면역).
	//   item 사슬 번역기가 이 마크업 낀 원문을 PoB 의 맨 문구와 못 맞춘다
	try {
		const raw = fs.readFileSync(path.join(FILES_DIR, "data@statdescriptions@stat_descriptions.csd"));
		const text = raw[0] === 0xff && raw[1] === 0xfe ? raw.toString("utf16le") : raw.toString("utf8");
		const plain = (s) => s.replace(/\[([^\]|]+)\|([^\]]+)\]/g, "$2").replace(/\[([^\]]+)\]/g, "$1");
		// 블록 = "description" 다음 줄들 — 영어 문구(변형 1개)가 먼저, 여러 언어 뒤에 lang "Korean"(변형 1개)
		for (const block of text.split(/\r?\ndescription\r?\n/)) {
			const en = block.match(/^\t\d+ [^\r\n]+\r?\n\t1\r?\n\t\t[^"\r\n]*"([^"{}\r\n]+)"/);
			const ko = block.match(/\tlang "Korean"\r?\n\t1\r?\n\t\t[^"\r\n]*"([^"{}\r\n]+)"/);
			if (en && ko) add(plain(en[1]), plain(ko[1]));
		}
	} catch {
		// 파일이 없으면 위 쌍만
	}
	return pairs;
})();
// 부적 버프 문장(10-09 C222) — 인게임 부적은 버프 스탯을 utility_flask_buff_stat_descriptions.csd 로 서술한다("Grants Immunity to Poison"
//   / "중독 면역 부여", poe2db 와 같음). PoB2 베이스의 buff 는 일반 설명 문장("Immune to Poison")이라, 그 문장의 스탯을 일반 설명에서 찾아
//   같은 스탯의 부적 전용 문장(영 · 한)으로 바꾼다. 전용 문장이 없는 버프(저항 · 희귀도 등)는 그대로
const charmBuff = (() => {
	const map = new Map();
	const read = (name) => {
		try {
			const raw = fs.readFileSync(path.join(FILES_DIR, name));
			return raw[0] === 0xff && raw[1] === 0xfe ? raw.toString("utf16le") : raw.toString("utf8");
		} catch {
			return null;
		}
	};
	const main = read("data@statdescriptions@stat_descriptions.csd");
	const charm = read("data@statdescriptions@utility_flask_buff_stat_descriptions.csd");
	if (!main || !charm) return map;
	const plain = (s) => s.replace(/\[([^\]|]+)\|([^\]]+)\]/g, "$2").replace(/\[([^\]]+)\]/g, "$1");
	const blocks = (text) => {
		const out = [];
		for (const block of text.split(/\r?\ndescription\r?\n/)) {
			const head = block.match(/^\t1 (\S+)\r?\n\t\d+\r?\n\t\t[^"\r\n]*"([^"{}\r\n]+)"/);
			if (!head) continue;
			const ko = block.match(/\tlang "Korean"\r?\n\t\d+\r?\n\t\t[^"\r\n]*"([^"{}\r\n]+)"/);
			out.push({ stat: head[1], en: plain(head[2]), ko: ko ? plain(ko[1]) : null });
		}
		return out;
	};
	const charmByStat = new Map(blocks(charm).map((b) => [b.stat, b]));
	for (const b of blocks(main)) {
		const c = charmByStat.get(b.stat);
		if (c && c.ko && !map.has(b.en)) map.set(b.en, { en: c.en, ko: c.ko });
	}
	return map;
})();
const buffKo = (en) => {
	const t = translate([en])[0];
	if (t && t !== en) return t;
	const ko = koPairs.get(en.replace(NUMTOK, "#"));
	if (!ko) return en;
	const nums = en.match(/\d+(?:\.\d+)?/g) || [];
	let i = 0;
	// 한국어 쪽 수치 자리(범위 포함)를 영어 쪽 고정값으로 — 부호는 한국어 원문 것을 둔다
	return ko.replace(/\(?-?\d+(?:\.\d+)?(?:--?\d+(?:\.\d+)?)?\)?/g, (m) => (i < nums.length ? nums[i++] : m));
};

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

// 베이스 암시 수치는 범위다(루비 반지 "+(20-30)% to Fire Resistance" — 10-08 C181: 예전엔 최소값만 "+20%" 로 실어 PoE1 베이스 · PoB 와 달랐다).
//   최소 · 최대로 한 번씩 설명을 만들어 숫자 자리마다 "(최소-최대)" 로 합친다(parse-waystone-mods2 mergeRange 와 같은 규칙)
const NUM = /-?\d+(?:\.\d+)?/g;
function mergeRange(minLine, maxLine) {
	if (minLine == null || minLine === maxLine) return maxLine;
	const lo = minLine.match(NUM) || [];
	const hi = maxLine.match(NUM) || [];
	if (lo.length !== hi.length) return maxLine;
	let i = 0;
	return maxLine.replace(NUM, (n) => {
		const [a, b] = [Number(lo[i++]), Number(n)].sort((x, y) => x - y);
		if (a === b) return n;
		if (b < 0) return `-(${-b}-${-a})`;
		return `(${a}-${b})`;
	});
}
const implicitLines = (baseIdx, lang) => {
	const out = [];
	for (const mi of bases[baseIdx].Implicit_Mods || []) {
		const m = mods[mi];
		if (!m) continue;
		const lo = new Map();
		const hi = new Map();
		for (let i = 1; i <= 6; i++) {
			if (m["Stat" + i] == null) continue;
			const v = m["Stat" + i + "Value"];
			lo.set(stats[m["Stat" + i]].Id, Array.isArray(v) ? v[0] : v);
			hi.set(stats[m["Stat" + i]].Id, Array.isArray(v) ? v[v.length - 1] : v);
		}
		const minLines = describe(lo, lang);
		const maxLines = describe(hi, lang);
		out.push(...(minLines.length === maxLines.length ? maxLines.map((l, k) => mergeRange(minLines[k], l)) : minLines));
	}
	return out;
};

// 암시 대조용 — 수치 · 범위("+(20-25)%" · "+20%")를 # 하나로
const implicitKey = (lines) =>
	lines
		.map((l) => l.replace(/[+-]?\(?\d+(?:\.\d+)?(?:-\d+(?:\.\d+)?)?\)?/g, "#").replace(/\s+/g, " ").trim().toLowerCase())
		.join("|");
// PoB 베이스 항목 → 게임 BaseItemTypes 행: 이름이 하나면 그 행, 여럿이면 아직 안 쓴 행 중 암시가 같은(2점) · 요구 레벨이 같은(1점) 행
const pickBaseIdx = (pb) => {
	const all = baseIdxsByName.get(pb.name) || [];
	if (all.length <= 1) return all[0];
	const free = all.filter((i) => !usedBaseIdx.has(i));
	const cands = free.length ? free : all;
	const want = implicitKey(pb.implicit ? pb.implicit.split("\\n").map(stripMarkup) : []);
	// 암시가 같고 능력치 요구만 다른 변형(룬벼림 움켜쥐는 사슬옷 ×4 = 힘 · 민첩 · 지능 조합)도 있어 요구 능력치도 맞춰 본다
	const reqOf = (i) => attrOf.get(i) || {};
	const score = (i) =>
		(implicitKey(implicitLines(i, "English")) === want ? 4 : 0) +
		(bases[i].DropLevel === pb.req.level ? 1 : 0) +
		((reqOf(i).ReqStr ?? 0) === (pb.req.str ?? 0) ? 1 : 0) +
		((reqOf(i).ReqDex ?? 0) === (pb.req.dex ?? 0) ? 1 : 0) +
		((reqOf(i).ReqInt ?? 0) === (pb.req.int ?? 0) ? 1 : 0);
	const best = cands.reduce((a, i) => (score(i) > score(a) ? i : a), cands[0]);
	usedBaseIdx.add(best);
	return best;
};

// 키워드 설명(10-04 C113, 고유 C112 짝) — 암시 줄의 강조 용어를 원문 템플릿 색인으로 찾아 정의째
const keywordById = loadKeywords(loadTable);
const keywordIdsOf = createKeywordIndex(CSD_CHAINS.item);
const slug = makeSlugger();
// 숨김 베이스(PoB hidden — 고유 전용 · 내부)는 목록에서 빼되 따로 싣는다(10-08 C176): 고유 툴팁이 그 베이스 수치(방어도 · 막기 · 피해)를 그린다
//   (해질녘의 Glacial Fortress 처럼 고유 전용 베이스). 따로 이름표를 매겨 보이는 베이스 slug 순번이 밀리지 않게 한다
const hiddenSlug = makeSlugger();
const hiddenItems = [];
const items = [];
const classUse = new Map();
let hidden = 0;
for (const file of FILES) {
	for (const pb of parseBases(await pobFile(`src/Data/Bases/${file}.lua`))) {
		if (pb.hidden) hidden++;
		const i = pickBaseIdx(pb);
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
		// 허리띠 "Has (1-3) Charm Slot" — 게임 BaseItemTypes.Implicit_Mods 밖에서 붙어(PoB2 implicit 문장엔 있음) 예전 허리띠 12종에서 빠져 있었다(10-10 C254).
		//   맨 앞에 둔다: 새 허리띠(갈래 · 기원 · 힘줄 · 추적)는 게임 임플리싯 목록 자체에 "Has 1 Charm Slot" 이 첫 줄이고, poe2db 베이스 · 고유 허리띠도 전부 첫 줄
		if (en.length && pb.implicit) {
			const have = new Set(en.map((l) => implicitKey([l])));
			// "Grants Skill: Spear Throw" 도 같은 경우(10-10 C255) — 게임 임플리싯 모드(SpearImplicitDisplaySpearThrow1)는 스탯 설명 없는 표시용이라 서술기가 줄을 못 만들어
			//   두 줄짜리 창 17종에서 빠졌다(poe2db 거대한 창 "스킬 부여: 창 투척" 이 첫 줄). 순서: 스킬 부여 → 호신부 슬롯 → 게임 줄
			const extra = pb.implicit.split("\\n").map(stripMarkup).filter((l) => /Charm Slot|^Grants Skill:/.test(l) && !have.has(implicitKey([l])));
			extra.sort((a, b) => Number(!/^Grants Skill:/.test(a)) - Number(!/^Grants Skill:/.test(b)));
			if (extra.length) implicits.unshift(...extra.map((line) => ({ en: line, ko: translate([line])[0] })));
		}
		const item = {
			name: b.Name,
			nameKo: basesKo[i].Name || b.Name,
			slug: pb.hidden ? hiddenSlug(b.Name) : slug(b.Name),
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
			armour: a || s || pb.ward ? { armour: a?.Armour ?? 0, evasion: a?.Evasion ?? 0, energyShield: a?.EnergyShield ?? 0, movementPenalty: a ? -(a.IncreasedMovementSpeed || 0) / 100 : 0, block: s?.Block ?? 0, ...(pb.ward ? { ward: pb.ward } : {}) } : null,
			weapon: w ? { damageMin: w.DamageMin, damageMax: w.DamageMax, critChance: w.CritChance / 100, attacksPerSecond: Math.round((1000 / w.Speed) * 100) / 100, range: w.RangeMax, reloadTime: w.ReloadTime || null } : null,
			flask: f
				? {
						type: f.Type,
						lifePerUse: f.LifePerUse,
						manaPerUse: f.ManaPerUse,
						recoverySeconds: f.RecoveryTime / 10,
						...(pb.chargesMax ? { maxCharges: pb.chargesMax, perCharge: pb.chargesUsed } : {}),
						...(pb.buff.length ? { buffLines: pb.buff.filter(Boolean).map((en) => charmBuff.get(en) || { en, ko: buffKo(en) }) } : {}),
					}
				: null,
			implicits,
			tags: pb.tags,
			subType: pb.subType,
			icon: b.ItemVisualIdentity != null ? visuals[b.ItemVisualIdentity]?.DDSFile || null : null,
			...(pb.spirit ? { spirit: pb.spirit } : {}),
		};
		const kwIds = keywordIdsOf(implicits.map((x) => x.en)).filter((id) => keywordById.has(id));
		if (kwIds.length) item.keywords = kwIds.map((id) => keywordById.get(id));
		if (pb.hidden) {
			hiddenItems.push(item);
			continue;
		}
		items.push(item);
		// en = 게임 영어 분류 이름(ItemClasses.Name — "Fishing Rods" · "Traps") — 영어 화면 칩이 내부 id(FishingRod · TrapTool)로 나오지 않게(10-02 ZZ)
		classUse.set(cls.Id, { key: cls.Id, ko: item.itemClassKo, en: cls.Name || cls.Id, category: item.category });
	}
}
items.sort((a, b) => a.itemClass.localeCompare(b.itemClass) || a.reqLevel - b.reqLevel || a.name.localeCompare(b.name));
hiddenItems.sort((a, b) => a.name.localeCompare(b.name));
const file = writeJson("base-items.json", { patch: loadConfig().patch, classes: [...classUse.values()], items, hiddenItems });
const cats = items.reduce((m, x) => ((m[x.category] = (m[x.category] || 0) + 1), m), {});
console.log(`[poe2 bases] ${items.length}개(숨김 ${hidden} 제외 — 그중 게임 테이블에 있는 ${hiddenItems.length}개는 hiddenItems 로) → ${file}`, cats);
