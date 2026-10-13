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
const OUT = path.join(process.env.POE_DATA_OUT || DATA_DIR, "skill-gems.json"); // POE_DATA_OUT = 사본 출력(10-13)

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
	qualityStats: load("English", "GrantedEffectQualityStats"),
};

const describe = createStatDescriber(FILES_DIR, [], { fileOrder: true });
// 보조 젬 문장은 gem_stat_descriptions 가 우선(10-09 C229 — 게임 "Supported Skills gain 39% of Physical Damage as Extra Fire Damage"
//   / "보조 대상 스킬로 …"). 기본 서술기는 뒤에 파싱되는 skill_stat_descriptions 가 같은 스탯을 "Gain … of your …" 로 덮어 보조 젬이 일반 문장이었다
const describeSupport = createStatDescriber(FILES_DIR, ["metadata@statdescriptions@gem_stat_descriptions.txt"], { fileOrder: true });
// 리마인더(인게임 회색 부연, 10-04 C115) — 레벨 문장마다 고른 템플릿의 reminderstring Id(describe.reminders)를 statReminders 로,
//   그 젬이 쓰는 Id 의 영 · 한 문구는 젬마다 한 번 reminderText 로(레벨마다 문구를 넣으면 25MB 파일이 부푼다)
const remEn = load("English", "ReminderText");
const remKo = load("Korean", "ReminderText");
const reminderTextById = new Map();
remEn.forEach((r, i) => {
	if (r.Text) reminderTextById.set(r.Id, { en: r.Text, ko: remKo[i]?.Id === r.Id && remKo[i]?.Text ? remKo[i].Text : r.Text });
});
let reminderLineCount = 0;

// 바알 스킬 영혼 소모(10-09 C234) — 게임 GrantedEffectsPerLevel 의 영혼 열은 추출 설정에 없어 PoB Data/Skills 의 "cost = { Soul = 50, }" 를 읽는다.
//   인게임 "사용 시 영혼 소모: 50"(ClientStrings ItemDisplaySkillGemSoulsPerUse, poedb 바알 은총). 키 = GrantedEffects.Id(= PoB skills 키)
const soulCostBySkill = (() => {
	const map = new Map();
	const dir = path.join(DATA_DIR, "work", "pob-src", "src", "Data", "Skills");
	try {
		for (const file of fs.readdirSync(dir)) {
			const text = fs.readFileSync(path.join(dir, file), "utf8");
			for (const block of text.split(/\nskills\["/).slice(1)) {
				const id = block.slice(0, block.indexOf('"'));
				const lv = new Map();
				for (const m of block.matchAll(/\[(\d+)\] = \{[^\n]*?cost = \{[^}\n]*?\bSoul = (\d+)/g)) lv.set(Number(m[1]), Number(m[2]));
				if (lv.size) map.set(id, lv);
			}
		}
	} catch {
		console.warn("PoB Data/Skills 없음 — 영혼 소모 없이 진행");
	}
	return map;
})();

// 스킬 전용 스탯 설명(10-09 C235) — 게임은 스킬마다 정한 설명 파일로 서술한다(PoB Data/Skills statDescriptionScope: 오라 40 · 버프 38 · 저주 37 ·
//   디버프 59 · 소환수 주문 83 …). 예전엔 일반 사슬만 써서 바알 은총 "공격 적중 회피 확률 +15%"(인게임 · poedb "자신 및 주변 동료들의 공격 명중
//   회피 확률 +15%"), "기본 지속시간 +6초"(인게임 "6초") 처럼 일반 문장이었다. 파일은 config.json files(extract) — 없으면 일반 사슬
const scopeBySkill = (() => {
	const map = new Map();
	const dir = path.join(DATA_DIR, "work", "pob-src", "src", "Data", "Skills");
	try {
		for (const file of fs.readdirSync(dir)) {
			const text = fs.readFileSync(path.join(dir, file), "utf8");
			for (const block of text.split(/\nskills\["/).slice(1)) {
				const m = block.match(/\n\tstatDescriptionScope = "([a-z_]+)"/);
				if (m) map.set(block.slice(0, block.indexOf('"')), m[1]);
			}
		}
	} catch {
		// PoB 없으면 일반 사슬
	}
	return map;
})();
const scopeDescribers = new Map();
function describerForScope(scope) {
	if (!scope || scope === "skill_stat_descriptions") return describe;
	if (scope === "gem_stat_descriptions") return describeSupport;
	const file = `metadata@statdescriptions@${scope}.txt`;
	if (!fs.existsSync(path.join(FILES_DIR, file))) return describe;
	if (!scopeDescribers.has(scope)) scopeDescribers.set(scope, createStatDescriber(FILES_DIR, [file], { fileOrder: true }));
	return scopeDescribers.get(scope);
}

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
// GrantedEffect → 기본(첫) 퀄리티 스탯 세트 (20% 퀄리티 기준 값)
const qualityByEffect = new Map();
for (const row of en.qualityStats) {
	if (!qualityByEffect.has(row.GrantedEffectsKey)) qualityByEffect.set(row.GrantedEffectsKey, row);
}
const ko = {
	base: load("Korean", "BaseItemTypes"),
	effects: load("Korean", "GemEffects"),
	tags: load("Korean", "GemTags"),
	active: load("Korean", "ActiveSkills"),
};

// GemColour: 1=힘(빨강), 2=민첩(초록), 3=지능(파랑), 그 외=화이트
const COLORS = { 1: "red", 2: "green", 3: "blue" };

// 개발용 더미 항목: DNT(Do Not Translate), [UNUSED], WIP(미완성 표시명 — 예 "WIP Support"), 이름이 점뿐인 것
const junkName = /\bDNT\b|\[UNUSED\]|\bWIP\b|^[. ]+$/i;

const gems = [];
// ⚠ 한 젬 행이 여러 **변형**(GemVariants)을 담는다: 기본 + 변형젬(3.23~, "Ice Nova of Frostbolts" 등).
//    예전엔 [0] 만 취해 변형젬 445개가 통째로 빠졌고, 그래서 실빌드 메타의 상당수(사신 of Butchery,
//    지배의 강타 of Inspiring …)를 시뮬이 아예 다루지 못했다. 변형마다 하나씩 내보낸다.
for (const gem of en.gems) {
	const base = en.base[gem.BaseItemTypesKey];
	if (!base || !base.Name || junkName.test(base.Name)) continue;
	if (base.Id.includes("Royale")) continue; // 로얄(배틀로얄 모드) 전용 제외
	// 바알 젬은 브라우저(목록/상세/툴팁)에는 노출하되 isVaal 플래그를 단다. 최적화기 자동 스킬 후보 풀에선
	// 제외한다 — 소울 생성/지속 업타임을 모델링하지 않아 DPS가 왜곡되고 기존 기준선이 흔들리기 때문.
	const isVaal = !!gem.IsVaalVariant;

	const emittedSlugs = new Set(); // 바알 젬은 같은 변형이 두 번 들어 있다 — 같은 slug 는 한 번만
	for (const variantIndex of gem.GemVariants || []) {
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

	const desc = gem.IsSupport ? describeSupport : describerForScope(granted && scopeBySkill.get(granted.Id));
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
			// 레벨별 플래그(값 1, 10-10 C247 — 추출 설정에 열이 빠져 있었다. PoE2 parse-gems2 statsAt 과 같은 처리)
			(stat.AdditionalFlags || []).forEach((statIndex) => statValues.set(en.stats[statIndex].Id, 1));
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
			// 오라/헤럴드 예약(마나 예약 %). 게임 원본은 permille 스케일(5000=50%) → /100.
			reservation: row.ManaReservationPercent > 0 ? row.ManaReservationPercent / 100 : null,
			costMultiplier: gem.IsSupport ? row.CostMultiplier : null,
			cooldownMs: row.Cooldown > 0 ? row.Cooldown : null,
			critChance: critChance > 0 ? critChance / 100 : null,
			// 게임 값은 100% 에 **더하는** 만분율(+5000 = 150%, -1840 = 81.6%) — PoB 도 1 + x/10000(10-09 C224: 예전 x/100 · 양수만이라 회오리바람
			//   20레벨 공격 피해가 "50%"(실제 150%), 파이어볼 추가 피해 효율 380%(실제 480%), 100% 미만 단계(회오리 1레벨 81.6%)는 줄째 빠졌다)
			damageEffectiveness: stat && stat.DamageEffectiveness ? 100 + stat.DamageEffectiveness / 100 : null,
			baseMultiplier: stat && stat.BaseMultiplier ? 100 + stat.BaseMultiplier / 100 : null,
			// 공격 속도 배율(10-08 C201) — GrantedEffectsPerLevel.AttackSpeedMultiplier(-10 → 기본 수치의 90%, PoE2 C200 짝). 0 이면 없음
			attackSpeedPct: row.AttackSpeedMultiplier ? 100 + row.AttackSpeedMultiplier : null,
			// 바알 스킬 영혼 소모(10-09 C234 — PoB cost.Soul). 없으면 null
			soulCost: (granted && soulCostBySkill.get(granted.Id)?.get(row.Level)) ?? null,
			statLines: desc(statValues, "English", { gemQuality: "skip" }),
			// 바로 위 영어 호출의 고른 변형 기준(한국어 호출 전에 읽어야 한다)
			statReminders: desc.reminders.map((ids) => ids.filter((id) => reminderTextById.has(id))),
			statLinesKo: desc(statValues, "Korean", { gemQuality: "skip" }),
		};
	});
	const usedReminderIds = new Set();
	for (const lv of levels) {
		if (lv.statReminders.some((ids) => ids.length)) {
			lv.statReminders.flat().forEach((id) => usedReminderIds.add(id));
			reminderLineCount += lv.statReminders.filter((ids) => ids.length).length;
		} else delete lv.statReminders;
	}

		// 퀄리티(20%)로 인한 추가 효과 — 젬 단위(레벨 무관). permille/1000 × 20 = 20% 퀄리티 값
		const qualityRow = qualityByEffect.get(effect.GrantedEffect);
		const qualityValues = new Map();
		if (qualityRow) {
			(qualityRow.StatsKeys || []).forEach((statIndex, i) => {
				const perMille = (qualityRow.StatsValuesPermille || [])[i] || 0;
				qualityValues.set(en.stats[statIndex].Id, Math.round((perMille / 1000) * 20));
			});
		}
		const qualityStatLines = qualityValues.size ? desc(qualityValues, "English", { gemQuality: "prefer" }) : [];
		// 퀄리티 줄 리마인더(10-04 C120, 레벨 statReminders 와 같은 규칙 — 영어 호출 직후에 읽는다)
		const qualityReminders = qualityValues.size ? desc.reminders.map((ids) => ids.filter((id) => reminderTextById.has(id))) : [];
		const qualityStatLinesKo = qualityValues.size ? desc(qualityValues, "Korean", { gemQuality: "prefer" }) : [];
		if (qualityReminders.some((ids) => ids.length)) {
			qualityReminders.flat().forEach((id) => usedReminderIds.add(id));
			reminderLineCount += qualityReminders.filter((ids) => ids.length).length;
		}

		// 바알 스킬 영혼 정보(인게임 툴팁): 영혼 획득 방지 시간(ms→초)·저장 사용 횟수. 스킬 단위로 사실상 일정하므로
		// GEPL 레벨행 중 값이 있는 첫 행에서 취한다(non-vaal 은 null).
		let soulPreventionSeconds = null;
		let storedUses = null;
		if (isVaal) {
			const rows = perLevelByEffect.get(effect.GrantedEffect) || [];
			const r = rows.find((x) => x.SoulGainPreventionDuration > 0) || rows[0];
			if (r) {
				soulPreventionSeconds = r.SoulGainPreventionDuration > 0 ? r.SoulGainPreventionDuration / 1000 : null;
				storedUses = r.StoredUses > 0 ? r.StoredUses : null;
			}
		}

	// 변형젬은 **효과 이름**이 곧 젬 이름이다("Ice Nova of Frostbolts"). 기본 변형은 베이스 이름을 쓴다.
	const variantName = effect.Name && effect.Name !== base.Name ? effect.Name : null;
	const variantSuffix = variantName ? "_" + (effect.Id || String(variantIndex)) : "";
	const variantSlug = base.Id.substring(base.Id.lastIndexOf("/") + 1) + variantSuffix;
	if (emittedSlugs.has(variantSlug)) continue;
	emittedSlugs.add(variantSlug);
	gems.push({
		id: base.Id + variantSuffix,
		slug: variantSlug,
		name: variantName || base.Name,
		nameKo: variantName ? (effectKo && effectKo.Name) || null : baseKo ? baseKo.Name : null,
		// 변형젬 여부 — 목록/후보 풀에서 기본 젬과 구분해야 할 때 쓴다
		transfigured: !!variantName,
		isSupport: !!gem.IsSupport,
		isVaal,
		soulPreventionSeconds,
		storedUses,
		color: COLORS[gem.GemColour] || "white",
		dropLevel: base.DropLevel,
		requiresStr: gem.StrengthRequirementPercent > 0,
		requiresDex: gem.DexterityRequirementPercent > 0,
		requiresInt: gem.IntelligenceRequirementPercent > 0,
		// 요구 능력치 비율(10-09 C225) — 인게임 "요구 사항 레벨 70, 68 힘, 98 민첩" 수치를 게이트가 PoB getGemStatRequirement 식으로 낸다
		reqStrPct: gem.StrengthRequirementPercent || null,
		reqDexPct: gem.DexterityRequirementPercent || null,
		reqIntPct: gem.IntelligenceRequirementPercent || null,
		castTimeMs: granted && !gem.IsSupport ? granted.CastTime : null,
		description: gem.IsSupport ? effect.SupportText : active ? active.Description : null,
		descriptionKo: gem.IsSupport
			? effectKo && effectKo.SupportText
			: activeKo
				? activeKo.Description
				: null,
		tags: tagNames,
		tagsKo: tagNamesKo,
		qualityStatLines,
		qualityStatLinesKo,
		...(qualityReminders.some((ids) => ids.length) ? { qualityReminders } : {}),
		levels,
		...(usedReminderIds.size ? { reminderText: Object.fromEntries([...usedReminderIds].sort().map((id) => [id, reminderTextById.get(id)])) } : {}),
	});
	} // 변형 루프
}

// 인게임에서 쓰이지 않는 플레이테스트(개발용) 젬 제외 — 목록·시뮬 후보 양쪽에서 빠져야 한다.
// (SkillGemPlaytestAttack/Slam/Spell — 실제 획득/사용 불가한 개발 전용 스킬)
{
	const before = gems.length;
	const kept = gems.filter((g) => !/Playtest/i.test(g.slug));
	if (kept.length !== before) {
		console.log(`플레이테스트 젬 제외: ${before - kept.length}`);
		gems.length = 0;
		gems.push(...kept);
	}
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
console.log(`리마인더: 젬 ${gems.filter((g) => g.reminderText).length}개 · 레벨 줄 ${reminderLineCount}`);
const support = gems.filter((g) => g.isSupport).length;
console.log(`  active ${gems.length - support} / support ${support}`);
const unknown = reportUnknownHandlers();
if (unknown.length) console.log("  미구현 핸들러:", unknown.join(", "));
console.log("  sample:", JSON.stringify(gems.find((g) => g.name === "Fireball")));
