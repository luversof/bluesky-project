// PoE2 젬 → ~/.poe-gamedata/poe2/gems.json
// 출시 여부는 PoB-PoE2 Data/Gems.lua(게임 파일에서 생성, 967개)를 기준표로 쓴다 — 게임 테이블엔 [DNT]·미사용 행과 같은 이름 중복(65건)이 섞여 있다.
// 이름·설명·태그의 한국어는 게임 테이블(Korean) 같은 행에서, 마크업("[Spell|주문]")은 표시 문구만 남긴다.
import { createStatDescriber } from "../poe-extract/statDescriptions.mjs";
import { CLEAN_DIR, CSD_CHAINS, cleanCopies, makeSlugger, pobFile, stripMarkup } from "./common2.mjs";
import { localName } from "./export-skill-desc-name.mjs";
import { loadConfig, loadTable, writeJson } from "./paths.mjs";

const T = (t) => [loadTable("English", t), loadTable("Korean", t)];
const [gems] = T("SkillGems");
const [bases, basesKo] = T("BaseItemTypes");
const [effects, effectsKo] = T("GemEffects");
const [gemTags, gemTagsKo] = T("GemTags");
const [granted] = T("GrantedEffects");
const [actives, activesKo] = T("ActiveSkills");
const [supports] = T("SupportGems");
const [families, familiesKo] = T("SupportGemFamily");
const [statSets] = T("GrantedEffectStatSets");
const [perLevel] = T("GrantedEffectStatSetsPerLevel");
const [effectLevels] = T("GrantedEffectsPerLevel");
const [costTypes, costTypesKo] = T("CostTypes");
const [stats] = T("Stats");
const group = (rows, key) => rows.reduce((m, r) => (m.get(key(r)) || m.set(key(r), []).get(key(r))).push(r) && m, new Map());
const perByStatSet = group(perLevel, (r) => r.StatSet);
const levelsByEffect = group(effectLevels, (r) => r.GrantedEffect);

// ── 레벨별 문장 — 공용 젬 체인 + 이 스킬 전용 설명 파일(ActiveSkills.StatDescription, 뒤가 이긴다) ──
const describers = new Map();
function describerFor(specific) {
	const key = specific || "";
	if (describers.has(key)) return describers.get(key);
	const files = [...CSD_CHAINS.gem];
	if (specific) {
		files.push(localName(specific));
	}
	cleanCopies(files);
	const d = createStatDescriber(CLEAN_DIR, files);
	const fn = (map, lang) => d(map, lang).map((l) => stripMarkup(l).replace(/^([^@\n]+)@([^@\n]+)$/, "$1: $2"));
	describers.set(key, fn);
	return fn;
}
/** 한 레벨의 스탯 값 — 암시(플래그 1) · 고정 · 부동소수(효과도 보간이면 BaseResolvedValues) · 가산 · 추가 플래그. */
function statsAt(setIdx, row) {
	const set = statSets[setIdx];
	const map = new Map();
	for (const s of set.ImplicitStats || []) map.set(stats[s].Id, 1);
	(set.ConstantStats || []).forEach((s, i) => map.set(stats[s].Id, set.ConstantStatsValues[i]));
	const interp = row.StatInterpolations || [];
	(row.FloatStats || []).forEach((s, i) => {
		const resolved = interp[i] === 3 && row.BaseResolvedValues?.[i] != null ? row.BaseResolvedValues[i] : row.FloatStatsValues[i];
		map.set(stats[s].Id, Math.round(resolved * 100) / 100);
	});
	(row.AdditionalStats || []).forEach((s, i) => map.set(stats[s].Id, row.AdditionalStatsValues[i]));
	for (const s of row.AdditionalFlags || []) map.set(stats[s].Id, 1);
	return map;
}
function levelsOf(effectIdx, specific, maxLevel) {
	const ge = granted[effectIdx];
	if (!ge || ge.StatSet == null) return [];
	// 게임 데이터엔 40레벨까지 있다 — 인게임 최대(PoB naturalMaxLevel, 보통 20)까지만
	const rows = (perByStatSet.get(ge.StatSet) || [])
		.filter((r) => r.GemLevel <= (maxLevel || 20))
		.sort((a, b) => a.GemLevel - b.GemLevel);
	const lv = new Map((levelsByEffect.get(effectIdx) || []).map((r) => [r.Level, r]));
	const d = describerFor(specific);
	const costIdx = ge.CostTypes?.[0];
	return rows.map((row) => {
		const map = statsAt(ge.StatSet, row);
		const l = lv.get(row.GemLevel);
		const crit = row.SpellCritChance || row.AttackCritChance || 0;
		const req = reqLevelBySkill.get(ge.Id)?.get(row.GemLevel);
		return {
			level: row.GemLevel,
			// 요구 캐릭터 레벨 — PoB Data/Skills levelRequirement(1레벨은 0 으로 적혀 있어 1 로). 없으면 null
			requiredLevel: req == null ? null : Math.max(1, req),
			cost: l?.CostAmounts?.[0] || null,
			costType: costIdx != null ? costTypes[costIdx]?.Id || null : null,
			reservation: l?.Reservation || null,
			cooldownMs: l?.Cooldown || null,
			critChance: crit ? crit / 100 : null,
			statLines: d(map, "English"),
			statLinesKo: d(map, "Korean"),
		};
	});
}

// PoB Gems.lua — 블록마다 gameId · Tier · gemType · weaponRequirements · naturalMaxLevel
const lua = await pobFile("src/Data/Gems.lua");
const pob = new Map();
const pobByName = new Map();
for (const m of lua.matchAll(/\n\t\["([^"]+)"\] = \{([\s\S]*?)\n\t\},/g)) {
	const body = m[2];
	const str = (k) => (body.match(new RegExp(`\\n\\t\\t${k} = "([^"]*)"`)) || [])[1] ?? null;
	const num = (k) => {
		const v = (body.match(new RegExp(`\\n\\t\\t${k} = (-?\\d+)`)) || [])[1];
		return v == null ? null : Number(v);
	};
	const entry = {
		name: str("name"),
		gemType: str("gemType"),
		weaponRequirements: str("weaponRequirements"),
		tier: num("Tier"),
		maxLevel: num("naturalMaxLevel"),
	};
	// 키는 PoB 내부 이름(…SkillGemCrescendoSupport)이라 게임 행과는 gameId·이름으로 맞춘다
	pob.set(str("gameId") || m[1], entry);
	if (entry.name) pobByName.set(entry.name, entry);
}

// PoB Data/Skills/*.lua — 스킬(GrantedEffects.Id)별 레벨 요구 캐릭터 레벨(levelRequirement). 게임 테이블엔 ActorLevel(스케일용)뿐이라
//   젬 상세의 "요구 레벨" 칸(PoE1 젬 상세와 같은 칸, 10-01)은 여기서 온다.
const reqLevelBySkill = new Map();
for (const f of ["act_str", "act_dex", "act_int", "sup_str", "sup_dex", "sup_int", "other", "minion", "spectre"]) {
	let text;
	try {
		text = await pobFile(`src/Data/Skills/${f}.lua`);
	} catch (e) {
		console.warn(`[poe2 gems] PoB Skills/${f}.lua 없음 — 요구 레벨 일부 빈칸: ${e.message}`);
		continue;
	}
	const blocks = text.split(/\nskills\["/);
	for (const b of blocks.slice(1)) {
		const id = b.slice(0, b.indexOf('"'));
		const lv = new Map();
		// 키 순서는 스킬마다 다르다(Ice Shot 은 attackSpeedMultiplier · baseMultiplier 뒤) — 같은 줄 어디에 있든 잡는다
		for (const m of b.matchAll(/\[(\d+)\] = \{[^\n]*?\blevelRequirement = (\d+)/g)) lv.set(Number(m[1]), Number(m[2]));
		if (lv.size) reqLevelBySkill.set(id, lv);
	}
}
console.log(`[poe2 gems] PoB 스킬 요구 레벨 ${reqLevelBySkill.size}개`);

const COLOR = { 1: "str", 2: "dex", 3: "int", 4: "white" };
const KIND = { 0: "skill", 1: "support", 2: "meta" };
const supportBySkillGem = new Map(supports.map((s) => [s.SkillGem, s]));
const slug = makeSlugger();
const seen = new Set();
const out = [];
for (let i = 0; i < gems.length; i++) {
	const g = gems[i];
	const base = bases[g.BaseItemType];
	const info = pob.get(base.Id) || pobByName.get(base.Name);
	if (!info) continue; // PoB 기준표에 없는 행 = 미출시/내부용
	const name = base.Name;
	if (!name || name.startsWith("[") || seen.has(name)) continue;
	seen.add(name);
	const effIdx = g.GemEffects?.[0];
	const eff = effIdx != null ? effects[effIdx] : null;
	const effKo = effIdx != null ? effectsKo[effIdx] : null;
	const ge = eff?.GrantedEffect != null ? granted[eff.GrantedEffect] : null;
	const act = ge?.ActiveSkill != null ? actives[ge.ActiveSkill] : null;
	const actKo = ge?.ActiveSkill != null ? activesKo[ge.ActiveSkill] : null;
	const tagRows = (eff?.GemTags || []).map((t) => [gemTags[t], gemTagsKo[t]]).filter(([t]) => t && t.Name);
	const sup = supportBySkillGem.get(i);
	out.push({
		id: base.Id,
		slug: slug(name),
		name,
		nameKo: basesKo[g.BaseItemType].Name || name,
		kind: KIND[g.GemType] || "skill",
		color: COLOR[g.GemColour] || "white",
		tier: info.tier,
		maxLevel: info.maxLevel,
		gemType: info.gemType,
		weaponRequirements: info.weaponRequirements,
		reqStr: g.StrengthRequirementPercent,
		reqDex: g.DexterityRequirementPercent,
		reqInt: g.IntelligenceRequirementPercent,
		tags: tagRows.map(([t]) => stripMarkup(t.Name)),
		tagsKo: tagRows.map(([t, k]) => stripMarkup(k.Name || t.Name)),
		description: act ? stripMarkup(act.Description) || null : null,
		descriptionKo: actKo ? stripMarkup(actKo.Description) || null : null,
		supportText: eff?.SupportText ? stripMarkup(eff.SupportText) : null,
		supportTextKo: effKo?.SupportText ? stripMarkup(effKo.SupportText) : null,
		family: sup ? (sup.Family || []).map((f) => families[f]?.Text).filter(Boolean).map(stripMarkup) : [],
		familyKo: sup ? (sup.Family || []).map((f) => familiesKo[f]?.Text).filter(Boolean).map(stripMarkup) : [],
		lineage: sup ? !!sup.IsLineage : false,
		icon: act?.Icon_DDSFile || sup?.Icon || null,
		castTimeMs: ge?.CastTime || null,
		levels: eff?.GrantedEffect != null ? levelsOf(eff.GrantedEffect, act?.StatDescription || null, info.maxLevel) : [],
	});
}
out.sort((a, b) => a.name.localeCompare(b.name));
const file = writeJson("gems.json", { patch: loadConfig().patch, gems: out });
const by = (k) => out.reduce((m, g) => ((m[g[k]] = (m[g[k]] || 0) + 1), m), {});
console.log(`[poe2 gems] ${out.length}개 → ${file}`, by("kind"), by("color"));
console.log(`  PoB 기준표 ${pob.size}개 중 게임 행과 맞은 것 ${out.length}`);
