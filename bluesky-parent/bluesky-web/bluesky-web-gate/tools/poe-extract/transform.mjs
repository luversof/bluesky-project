// dat 테이블(JSON) → 게이트가 서빙할 표시용 스킬젬 JSON 변환.
// 사용법: node extract.mjs 후 node transform.mjs → ~/.poe-gamedata/skill-gems.json
// GGG 스키마 변경은 이 파일과 config.json 만 고치면 된다 (표시용 스키마는 안정 유지).
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { createStatDescriber, reportUnknownHandlers } from "./statDescriptions.mjs";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable } from "./paths.mjs";

const PATCH = loadConfig().patch;
// 게임 데이터는 파생 산출물이라 git 에 커밋하지 않는다 — 홈 디렉토리에 두고 서버가 부팅 시 로드 (poe.data-dir 프로퍼티)
const OUT = path.join(DATA_DIR, "skill-gems.json");

const load = loadTable;

const en = {
	base: load("English", "BaseItemTypes"),
	gems: load("English", "SkillGems"),
	effects: load("English", "GemEffects"),
	tags: load("English", "GemTags"),
	granted: load("English", "GrantedEffects"),
	active: load("English", "ActiveSkills"),
	perLevel: load("English", "GrantedEffectsPerLevel"),
	statSetsPerLevel: load("English", "GrantedEffectStatSetsPerLevel"),
	costTypes: load("English", "CostTypes"),
	stats: load("English", "Stats"),
	statSets: load("English", "GrantedEffectStatSets"),
};

const describe = createStatDescriber(FILES_DIR);

// GrantedEffect 인덱스 → 레벨별 행 묶음 (조회 성능용 사전 구축)
const perLevelByEffect = new Map();
for (const row of en.perLevel) {
	if (!perLevelByEffect.has(row.GrantedEffect)) perLevelByEffect.set(row.GrantedEffect, []);
	perLevelByEffect.get(row.GrantedEffect).push(row);
}
const statsByEffectLevel = new Map();
for (const row of en.statSetsPerLevel) {
	for (const effectIndex of row.GrantedEffects || []) {
		statsByEffectLevel.set(effectIndex + ":" + row.GemLevel, row);
	}
}
const ko = {
	base: load("Korean", "BaseItemTypes"),
	effects: load("Korean", "GemEffects"),
	tags: load("Korean", "GemTags"),
	active: load("Korean", "ActiveSkills"),
};

// GemColour: 1=힘(빨강), 2=민첩(초록), 3=지능(파랑), 그 외=화이트
const COLORS = { 1: "red", 2: "green", 3: "blue" };

// 개발용 더미 항목: DNT(Do Not Translate), [UNUSED], 이름이 점뿐인 것
const junkName = /\bDNT\b|\[UNUSED\]|^[. ]+$/i;

const gems = [];
for (const gem of en.gems) {
	const base = en.base[gem.BaseItemTypesKey];
	if (!base || !base.Name || junkName.test(base.Name)) continue;
	if (base.Id.includes("Royale")) continue; // 로얄(배틀로얄 모드) 전용 제외
	if (gem.IsVaalVariant) continue; // 바알 젬은 별도 항목으로 노출하지 않는다 (스파이크 범위)

	const variantIndex = (gem.GemVariants || [])[0];
	const effect = variantIndex != null ? en.effects[variantIndex] : null;
	if (!effect) continue;
	if (effect.Name && junkName.test(effect.Name)) continue;
	if (effect.SupportName && junkName.test(effect.SupportName)) continue;

	const granted = effect.GrantedEffect != null ? en.granted[effect.GrantedEffect] : null;
	const active = granted && granted.ActiveSkill != null ? en.active[granted.ActiveSkill] : null;
	const activeKo = granted && granted.ActiveSkill != null ? ko.active[granted.ActiveSkill] : null;
	const effectKo = variantIndex != null ? ko.effects[variantIndex] : null;
	const baseKo = ko.base[gem.BaseItemTypesKey];

	const tagNames = (effect.GemTags || [])
		.map((t) => en.tags[t] && en.tags[t].Tag)
		.filter((t) => t);
	const tagNamesKo = (effect.GemTags || [])
		.map((t) => ko.tags[t] && ko.tags[t].Tag)
		.filter((t) => t);

	// 레벨별 수치: 요구 레벨/소모/재사용/크리 확률/피해 효율/지원 배율
	const levels = (perLevelByEffect.get(effect.GrantedEffect) || []).map((row) => {
		const stat = statsByEffectLevel.get(effect.GrantedEffect + ":" + row.Level);
		const costType = (row.CostTypes || []).length ? en.costTypes[row.CostTypes[0]].Id : null;
		const critChance = stat ? Math.max(stat.SpellCritChance, stat.AttackCritChance) : 0;
		// 스탯 id → 값 (표시 순서: 레벨별 float → 레벨별 int → 상수 → 암시 플래그)
		const statValues = new Map();
		if (stat) {
			(stat.FloatStats || []).forEach((statIndex, i) => {
				statValues.set(en.stats[statIndex].Id, (stat.BaseResolvedValues || [])[i] ?? 0);
			});
			(stat.AdditionalStats || []).forEach((statIndex, i) => {
				statValues.set(en.stats[statIndex].Id, (stat.AdditionalStatsValues || [])[i] ?? 0);
			});
			const statSet = en.statSets[stat.StatSet];
			if (statSet) {
				(statSet.ConstantStats || []).forEach((statIndex, i) => {
					statValues.set(en.stats[statIndex].Id, (statSet.ConstantStatsValues || [])[i] ?? 0);
				});
				(statSet.ImplicitStats || []).forEach((statIndex) => {
					statValues.set(en.stats[statIndex].Id, 1);
				});
			}
		}
		return {
			level: row.Level,
			requiredLevel: row.PlayerLevelReq,
			cost: (row.CostAmounts || [])[0] ?? null,
			costType,
			costMultiplier: gem.IsSupport ? row.CostMultiplier : null,
			cooldownMs: row.Cooldown > 0 ? row.Cooldown : null,
			critChance: critChance > 0 ? critChance / 100 : null,
			damageEffectiveness: stat && stat.DamageEffectiveness > 0 ? stat.DamageEffectiveness / 100 : null,
			baseMultiplier: stat && stat.BaseMultiplier > 0 ? stat.BaseMultiplier / 100 : null,
			statLines: describe(statValues, "English"),
			statLinesKo: describe(statValues, "Korean"),
		};
	});

	gems.push({
		id: base.Id,
		slug: base.Id.substring(base.Id.lastIndexOf("/") + 1),
		name: base.Name,
		nameKo: baseKo ? baseKo.Name : null,
		isSupport: !!gem.IsSupport,
		color: COLORS[gem.GemColour] || "white",
		dropLevel: base.DropLevel,
		requiresStr: gem.StrengthRequirementPercent > 0,
		requiresDex: gem.DexterityRequirementPercent > 0,
		requiresInt: gem.IntelligenceRequirementPercent > 0,
		castTimeMs: granted && !gem.IsSupport ? granted.CastTime : null,
		description: gem.IsSupport ? effect.SupportText : active ? active.Description : null,
		descriptionKo: gem.IsSupport
			? effectKo && effectKo.SupportText
			: activeKo
				? activeKo.Description
				: null,
		tags: tagNames,
		tagsKo: tagNamesKo,
		levels,
	});
}

// slug 는 상세 조회 키로 쓰므로 중복이 있으면 추출 단계에서 실패시킨다
const slugCounts = new Map();
gems.forEach((g) => slugCounts.set(g.slug, (slugCounts.get(g.slug) || 0) + 1));
const dupes = [...slugCounts.entries()].filter(([, n]) => n > 1);
if (dupes.length) {
	throw new Error("slug 중복: " + dupes.map(([s]) => s).join(", "));
}

gems.sort((a, b) => a.name.localeCompare(b.name));
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(
	OUT,
	JSON.stringify({ patch: PATCH, gems }, null, 1),
);
console.log(`patch ${PATCH}: ${gems.length} gems → ${OUT}`);
const support = gems.filter((g) => g.isSupport).length;
console.log(`  active ${gems.length - support} / support ${support}`);
const unknown = reportUnknownHandlers();
if (unknown.length) console.log("  미구현 핸들러:", unknown.join(", "));
console.log("  sample:", JSON.stringify(gems.find((g) => g.name === "Fireball")));
