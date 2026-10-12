// PoE2 젬 → ~/.poe-gamedata/poe2/gems.json
// 출시 여부는 PoB-PoE2 Data/Gems.lua(게임 파일에서 생성, 967개)를 기준표로 쓴다 — 게임 테이블엔 [DNT]·미사용 행과 같은 이름 중복(65건)이 섞여 있다.
// 이름·설명·태그의 한국어는 게임 테이블(Korean) 같은 행에서, 마크업("[Spell|주문]")은 표시 문구만 남긴다.
import { createStatDescriber } from "../poe-extract/statDescriptions.mjs";
import { CLEAN_DIR, CSD_CHAINS, cleanCopies, loadKeywords, makeSlugger, markupIds, pobFile, stripMarkup } from "./common2.mjs";
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
// 스탯 묶음 이름(10-09 C236 — GrantedEffectStatSets.Label → GrantedEffectLabels.Text, 인게임 · poe2db 머리글 "투사체" · "폭파"). "Hidden" 은 표시 안 함
const [labelsEn, labelsKo] = T("GrantedEffectLabels");
const labelOf = (setIdx) => {
	const i = statSets[setIdx]?.Label;
	if (i == null || !labelsEn[i]) return null;
	return { en: labelsEn[i].Text || null, ko: labelsKo[i]?.Text || null, id: labelsEn[i].Id };
};
const shownLabel = (l) => l && l.en && l.id !== "Hidden" && l.en !== "Hidden";
// 추가 묶음 k 의 스킬 전용 설명 = 같은 폴더 statset_k.csd(export-skill-desc 가 받아 둔다)
const specificFor = (specific, k) => (specific && /statset_\d+\.csd$/i.test(specific) ? specific.replace(/statset_\d+\.csd$/i, `statset_${k}.csd`) : specific);

// ── 레벨별 문장 — 공용 젬 체인 + 이 스킬 전용 설명 파일(ActiveSkills.StatDescription, 뒤가 이긴다) ──
const describers = new Map();
// 키워드 설명(10-04 C111, 트리 C110 짝) — 레벨 문장 · 설명 · 보조 설명 원문의 강조 용어 Id 를 젬마다 모아 정의째 싣는다(목록 응답은 API 가 뺌)
const keywordById = loadKeywords(loadTable);
const seenKeywordIds = new Set();
// support: 보조 젬은 gem_stat_descriptions 를 맨 뒤(최우선)로(10-09 C231 — 게임 "Supported Skills deal no Elemental Damage" / "보조 대상 스킬로 주는 원소 피해 없음",
//   poe2db 같음. 기본 사슬에선 뒤의 skill_stat_descriptions 가 같은 스탯을 일반 문장 "Deals no Elemental Damage" 로 덮었다 — PoE1 C229 짝)
function describerFor(specific, support = false, quality = false) {
	const key = (specific || "") + (support ? "#support" : "") + (quality ? "#quality" : "");
	if (describers.has(key)) return describers.get(key);
	// meta_gem_stat_descriptions 는 gem_stat → stat_descriptions 만 include 한다(10-10 C250) — skill 계열 문장이 섞이면 여분 줄(주문투척 "기원 스킬에 의해 발동 …")
	const files = specific && /meta_gem_stat_descriptions/i.test(specific)
		? ["data@statdescriptions@stat_descriptions.csd", "data@statdescriptions@gem_stat_descriptions.csd"]
		: [...CSD_CHAINS.gem];
	if (specific) {
		files.push(localName(specific));
	}
	if (support) files.push("data@statdescriptions@gem_stat_descriptions.csd");
	cleanCopies(files);
	// 퀄리티 줄은 gem_quality 변형("연쇄 +1회")을, 일반 줄은 그 밖의 변형("3회 연쇄")을 쓴다(10-09 C244, poe2db 아크)
	const d = createStatDescriber(CLEAN_DIR, files, { fileOrder: true, gemQuality: quality ? "prefer" : "skip" }); // 줄 순서 = 게임 설명 파일 순서(10-09 C242, poe2db)
	const fn = (map, lang) => d(map, lang).map((l) => (lang === "English" && markupIds(l).forEach((id) => seenKeywordIds.add(id)), l)).map((l) => stripMarkup(l).replace(/^([^@\n]+)@([^@\n]+)$/, "$1: $2"));
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
/** 묶음의 고정 스탯(암시 플래그 1 · 고정값) — 추가 묶음이 물려받는 부분(10-09 C243). */
function fixedStats(setIdx) {
	const set = statSets[setIdx];
	const map = new Map();
	for (const s of set.ImplicitStats || []) map.set(stats[s].Id, 1);
	(set.ConstantStats || []).forEach((s, i) => map.set(stats[s].Id, set.ConstantStatsValues[i]));
	return map;
}
function levelsOf(effectIdx, specific, maxLevel, support = false) {
	const ge = granted[effectIdx];
	if (!ge || ge.StatSet == null) return [];
	// 게임 데이터엔 40레벨까지 있다 — 인게임 최대(PoB naturalMaxLevel, 보통 20)까지만
	const rows = (perByStatSet.get(ge.StatSet) || [])
		.filter((r) => r.GemLevel <= (maxLevel || 20))
		.sort((a, b) => a.GemLevel - b.GemLevel);
	const lv = new Map((levelsByEffect.get(effectIdx) || []).map((r) => [r.Level, r]));
	const d = describerFor(specific, support);
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
			// 공격 스킬 배율(10-08 C200) — PoB2 Export/Scripts/skills.lua 식: 피해 = 1 + BaseMultiplier/10000, 공격 속도 = 100 + AttackSpeedMultiplier(%).
			//   인게임 "공격 피해: 기본 수치의 95%" · "공격 속도: 기본 수치의 90%"(ClientStrings PercentageOfBase). 0 이면 없음
			attackDamagePct: row.BaseMultiplier ? Math.round(100 + row.BaseMultiplier / 100) : null,
			attackSpeedPct: l?.AttackSpeedMultiplier ? 100 + l.AttackSpeedMultiplier : null,
			// 보조 젬 소모 · 점유 배율(10-09 C231 — PoB2 manaMultiplier · reservationMultiplier, 인게임 "소모 배율: 120%" · 같으면 "소모 및 점유 배율")
			costMultiplierPct: costMultBySkill.get(ge.Id)?.get(row.GemLevel)?.[0] ?? null,
			reservationMultiplierPct: costMultBySkill.get(ge.Id)?.get(row.GemLevel)?.[1] ?? null,
			statLines: d(map, "English"),
			statLinesKo: d(map, "Korean"),
			// 추가 스탯 묶음(10-09 C236 — GrantedEffects.AdditionalStatSets, PoB2 GemTooltip addStatSetInfo: 묶음마다 머리글 · 치명타 · 공격 피해 · 스탯 줄).
			//   같은 젬 레벨 행, 설명은 statset_k.csd. "Hidden" 묶음과 줄 없는 묶음은 뺀다
			statSets: (ge.AdditionalStatSets || [])
				.map((setIdx, k) => {
					const lab = labelOf(setIdx);
					if (lab && !shownLabel(lab)) return null;
					const r2 = (perByStatSet.get(setIdx) || []).find((r) => r.GemLevel === row.GemLevel);
					if (!r2) return null;
					const d2 = describerFor(specificFor(specific, k + 1), support);
					// 추가 묶음은 주 묶음 스탯을 물려받고 IgnoredStats 만 뺀 뒤 제 스탯으로 덮는다(10-09 C243 — 게임 GrantedEffectStatSets.IgnoredStats,
					//   PoB2 볼 라이트닝 화염 주입 constantStats 에 주 묶음의 "점화 지대 생성 간격 350" · poe2db "0.35초마다 점화 지대 생성")
					//   물려받는 건 주 묶음의 고정 스탯(Implicit · Constant)뿐 — 레벨별 스탯은 아니다(poe2db 파이어볼 "폭파" 칸엔 반경만 · 얼음 화살 "폭파" 에 냉각 강도 없음,
					//   뼈 분쇄 "충격파" · 혜성 · 스파크 · 얼음 회오리는 주 묶음 고정 줄을 되풀이해 보인다)
					const ignored = new Set((statSets[setIdx].IgnoredStats || []).map((s) => stats[s].Id));
					const own = statsAt(setIdx, r2);
					const inherited = [...fixedStats(ge.StatSet)].filter(([id]) => !ignored.has(id) && !own.has(id));
					const m2 = new Map([...inherited, ...own]);
					const lines = d2(m2, "English");
					if (!lines.length) return null;
					// 추가 묶음 치명타는 주 묶음과의 차이값(PoB2 볼 라이트닝 화염 주입: 주 9% + (-2) = 7%). 0 이면 주 묶음 값을 그대로 쓰니 따로 안 보인다
					const crit2 = r2.SpellCritChance || r2.AttackCritChance || 0;
					return {
						label: lab?.en || null,
						labelKo: lab?.ko || null,
						critChance: crit2 ? (crit + crit2) / 100 : null,
						attackDamagePct: r2.BaseMultiplier ? Math.round(100 + r2.BaseMultiplier / 100) : null,
						statLines: lines,
						statLinesKo: d2(m2, "Korean"),
					};
				})
				.filter(Boolean),
		};
	});
}

// 필요 무기 한국어(10-09 C228) — PoB2 문장("Any Martial Weapon" · "Spear, Bow")은 게임 ClientStrings 문장(EveryWeaponType 등)과 같은 영어다.
//   문장째 같은 것은 그 한국어, 아니면 쉼표로 나눠 무기 종류마다 ItemClasses(Id → 이름) · WeaponClassDisplayName* 한국어로.
//   하나라도 못 옮기면 null(영어 유지 — "Armoured Shield" 처럼 게임 문장이 없는 PoB 표현)
const weaponKo = (() => {
	const [csEn, csKo] = T("ClientStrings");
	const [clsEn, clsKo] = T("ItemClasses");
	const ko = new Map();
	csEn.forEach((r, i) => {
		if (!r.Text || !csKo[i]?.Text) return;
		if (/^(Every|Any|WeaponClassDisplayName)/.test(r.Id) || r.Id === "AnyShield") ko.set(stripMarkup(r.Text), stripMarkup(csKo[i].Text));
	});
	clsEn.forEach((r, i) => {
		if (r.Id && clsKo[i]?.Name && !ko.has(r.Id)) ko.set(r.Id, clsKo[i].Name);
	});
	return (en) => {
		if (!en) return null;
		if (ko.has(en)) return ko.get(en);
		const parts = en.split(/, | or /).map((p) => ko.get(p.trim()));
		return parts.every(Boolean) ? parts.join(", ") : null;
	};
})();

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
const costMultBySkill = new Map(); // 스킬 id → 레벨 → [소모 배율 %, 점유 배율 %](10-09 C231)
// 퀄리티 스탯(10-08 C195) — PoB 스킬 블록의 qualityStats = { { "스탯", 퀄리티 1%당 값, … } }. 게임 표(GrantedEffectQualityStats)는 PoE2 추출에 없어 PoB 에서
const qualityBySkill = new Map();
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
		const cm = new Map();
		for (const m of b.matchAll(/\[(\d+)\] = \{([^\n]*)/g)) {
			const mana = m[2].match(/\bmanaMultiplier = (-?\d+)/);
			const res = m[2].match(/\breservationMultiplier = (-?\d+)/);
			if (mana || res) cm.set(Number(m[1]), [mana ? 100 + Number(mana[1]) : null, res ? 100 + Number(res[1]) : null]);
		}
		if (cm.size) costMultBySkill.set(id, cm);
		const q = b.match(/\n\tqualityStats = \{([\s\S]*?)\n\t\},/);
		if (q) {
			const list = [...q[1].matchAll(/\{\s*"([^"]+)",\s*(-?[\d.]+)/g)].map((m) => [m[1], Number(m[2])]);
			if (list.length) qualityBySkill.set(id, list);
		}
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
	seenKeywordIds.clear();
	for (const raw of [act?.Description, eff?.SupportText]) markupIds(raw).forEach((id) => seenKeywordIds.add(id));
	const isSupportGem = KIND[g.GemType] === "support";
	const levels = eff?.GrantedEffect != null ? levelsOf(eff.GrantedEffect, act?.StatDescription || null, info.maxLevel, isSupportGem) : [];
	// 보조 젬 요구 레벨 = SkillGems.MinLevelReq(10-09 C233 — poe2db 아흔의 성채 "요구 사항: 레벨 65" · 무기력 5. PoB 레벨 표는 1 이라 예전엔 전부 "레벨 1")
	if (isSupportGem && g.MinLevelReq > 1) for (const l of levels) l.requiredLevel = Math.max(l.requiredLevel || 1, g.MinLevelReq);
	// 젬이 덤으로 주는 스킬(10-10 C249 — GemEffects.AdditionalGrantedEffects, poe2db 세차게 흐르는 전류: "보조" 칸 뒤 "세차게 흐르는 전류" 칸에
	//   레벨 · 소모 · 재사용 · 시전 · 치명타 · 설명 · 스탯 줄). 부여 스킬은 젬 레벨을 따른다(같은 레벨 행). 이름 없는 내부 스킬은 뺀다
	const grantedSkills = [];
	const grantedByLevel = [];
	for (const gi of eff?.AdditionalGrantedEffects || []) {
		const g2 = granted[gi];
		const a2 = g2?.ActiveSkill != null ? actives[g2.ActiveSkill] : null;
		const a2Ko = g2?.ActiveSkill != null ? activesKo[g2.ActiveSkill] : null;
		// 칸 머리글 = 그 스킬 주 묶음 라벨(쇠뇌 사격의 부여 스킬 "탄약"), 없으면 이름(poe2db 마름쇠 · 화산 걸음)
		const lab2 = labelOf(g2.StatSet);
		// 메타 젬의 보조형 효과(IsSupport, 스킬 없음 — poe2db 치명타 시 시전 "보조" 칸)와 이름 없는 효과(신기루 궁수 "신기루" — 라벨로 머리글)도 칸으로(10-10 C250)
		if (!a2 || !a2.DisplayedName) {
			if (!g2 || (!g2.IsSupport && !shownLabel(lab2))) continue;
			// 메타 젬의 보조 효과는 meta_gem_stat_descriptions 로 설명한다(PoB2 statDescriptionScope — "장착된 주문의 시전 속도 …", gem_stat 의 "보조 대상 스킬의 …" 가 아님)
			//   신기루 궁수 "신기루"(보조 아닌 이름 없는 효과)도 메타 젬이면 같은 파일("신기루 지속시간 8초")
			const meta = KIND[g.GemType] === "meta";
			const lv3 = levelsOf(gi, meta ? "Data/StatDescriptions/meta_gem_stat_descriptions.csd" : null, info.maxLevel, !!g2.IsSupport && !meta);
			if (!lv3.length || !lv3.some((x) => x.statLines.length)) continue;
			grantedSkills.push({
				...(shownLabel(lab2) ? { label: lab2.en, labelKo: lab2.ko } : {}),
				name: null, nameKo: null, description: null, descriptionKo: null,
				copiesLevel: false, castTimeMs: null,
				support: !!g2.IsSupport,
			});
			grantedByLevel.push(new Map(lv3.map((x) => [x.level, x])));
			continue;
		}
		if (a2.DisplayedName.startsWith("[")) continue;
		const lv2 = levelsOf(gi, a2.StatDescription || null, info.maxLevel, false);
		if (!lv2.length || !lv2.some((x) => x.statLines.length)) continue;
		grantedSkills.push({
			...(shownLabel(lab2) ? { label: lab2.en, labelKo: lab2.ko } : {}),
			name: stripMarkup(a2.DisplayedName),
			nameKo: stripMarkup(a2Ko?.DisplayedName || a2.DisplayedName),
			description: stripMarkup(a2.Description) || null,
			descriptionKo: stripMarkup(a2Ko?.Description) || null,
			// 레벨별 행(GrantedEffectsPerLevel)이 없는 부여 스킬은 속성 줄 없이 스탯 줄만(poe2db 석궁 사격 "탄약") — 소모 종류가 비어 있는 것으로 가른다
			...(lv2.some((x) => x.costType) ? { copiesLevel: true, castTimeMs: g2.CastTime ?? null } : { copiesLevel: false, castTimeMs: null }),
		});
		grantedByLevel.push(new Map(lv2.map((x) => [x.level, x])));
	}
	if (grantedSkills.length) {
		for (const l of levels) {
			l.grantedSkills = grantedByLevel.map((m) => {
				const x = m.get(l.level);
				return x ? { cost: x.cost, costType: x.costType, cooldownMs: x.cooldownMs, critChance: x.critChance, attackDamagePct: x.attackDamagePct, statLines: x.statLines, statLinesKo: x.statLinesKo, statSets: x.statSets?.length ? x.statSets : null } : null;
			});
		}
	}
	// 퀄리티로 인한 추가 효과 — PoE1(transform.mjs)과 같이 퀄리티 20% 값(1%당 값 × 20, 반올림)으로 설명. 인게임 젬 툴팁 "퀄리티로 인한 추가 효과:" 줄
	const qStats = ge ? qualityBySkill.get(ge.Id) : null;
	const qMap = new Map((qStats || []).map(([id, v]) => [id, Math.round(v * 20)]));
	const qd = describerFor(act?.StatDescription || null, isSupportGem, true);
	const qualityStatLines = qMap.size ? qd(qMap, "English") : [];
	const qualityStatLinesKo = qMap.size ? qd(qMap, "Korean") : [];
	const keywords = [...seenKeywordIds].filter((id) => keywordById.has(id)).map((id) => keywordById.get(id));
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
		weaponRequirementsKo: weaponKo(info.weaponRequirements),
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
		// 주 스탯 묶음 이름(10-09 C236 — 추가 묶음이 있을 때 첫 머리글 "투사체", PoB2 는 스킬 이름과 다를 때만 보인다)
		...(ge && ((ge.AdditionalStatSets || []).length || grantedSkills.length) && shownLabel(labelOf(ge.StatSet)) ? { statSetLabel: labelOf(ge.StatSet).en, statSetLabelKo: labelOf(ge.StatSet).ko } : {}),
		...(grantedSkills.length ? { grantedSkills } : {}),
		keywords: keywords.length ? keywords : null,
		...(qualityStatLines.length ? { qualityStatLines, qualityStatLinesKo } : {}),
		levels,
	});
}
out.sort((a, b) => a.name.localeCompare(b.name));
const file = writeJson("gems.json", { patch: loadConfig().patch, gems: out });
const by = (k) => out.reduce((m, g) => ((m[g[k]] = (m[g[k]] || 0) + 1), m), {});
console.log(`[poe2 gems] ${out.length}개 → ${file}`, by("kind"), by("color"));
console.log(`  키워드 설명: 젬 ${out.filter((g) => g.keywords).length}개 · 용어 ${new Set(out.flatMap((g) => (g.keywords || []).map((k) => k.term))).size}종 · 한글 정의 빠진 것 ${out.flatMap((g) => g.keywords || []).filter((k) => !k.defKo).length}`);
console.log(`  PoB 기준표 ${pob.size}개 중 게임 행과 맞은 것 ${out.length}`);
