// PoE2 증강물(룬 · 영혼 핵 · 우상 · 심연의 눈 …) → ~/.poe-gamedata/poe2/augments.json
// 게임 테이블 SoulCores(베이스·요구 레벨·종류·한도) + SoulCoreStats(장착 대상 분류별 스탯) — 대상 분류마다 효과 문장을 영/한으로.
import { createDescriber, makeSlugger, stripMarkup } from "./common2.mjs";
import { loadConfig, loadTable, writeJson } from "./paths.mjs";

const T = (t) => [loadTable("English", t), loadTable("Korean", t)];
const [cores] = T("SoulCores");
const [bases, basesKo] = T("BaseItemTypes");
const [coreStats] = T("SoulCoreStats");
const [cats, catsKo] = T("SoulCoreStatCategories");
const [types, typesKo] = T("SoulCoreTypes");
const [limits, limitsKo] = T("SoulCoreLimits");
const [classes, classesKo] = T("ItemClasses");
const [stats] = T("Stats");
const [visuals] = T("ItemVisualIdentity");
// 중첩 개수(10-13 C297) — CurrencyItems.StackSize, 인게임 "중첩 개수: 1/10"(ClientStrings ItemDisplayStackSize · ItemDisplayStackSizeValue).
//   poe2db: 사막 룬 · 뱀 우상 "1 / 10", 1 짜리(올로스의 유산 룬)와 행 없는 것(떠오르는 활력 · 우상 4종)은 줄 자체가 없다 → 2 이상만 싣는다
const [currency] = T("CurrencyItems");
const stackByBase = new Map(currency.map((r) => [r.BaseItemType, r.StackSize]));
const describe = createDescriber();

// 분류 한국어 표시 — 게임 문구가 비어 있으면 대상 아이템 클래스 한국어 이름으로
const FALLBACK_KO = { All: "모든 장비", Armour: "방어구", "Martial Weapon": "무도 무기" };
const catLabel = (i) => {
	const en = cats[i].Id;
	// 영어 표시 = 게임 Display(10-23 Q80 — 'All' → 'All Equipment', 'Martial Or Caster Weapon' → 'Weapon'). 비면 분류 Id 그대로
	const enDisplay = stripMarkup(cats[i].Display || "") || en;
	const ko = stripMarkup(catsKo[i].Display || "");
	if (ko) return { en, enDisplay, ko };
	const targets = (cats[i].TargetItemClasses || []).map((c) => classesKo[c].Name).filter(Boolean);
	return { en, enDisplay, ko: FALLBACK_KO[en] || (targets.length ? targets.join(", ") : en) };
};
const KIND = { Rune: "rune", SoulCore: "soulcore", Idol: "idol", AbyssalEye: "abyssaleye", CongealedMist: "mist" };

const slug = makeSlugger();
const items = [];
for (let i = 0; i < cores.length; i++) {
	const c = cores[i];
	const b = bases[c.BaseItemType];
	if (!b || !b.Name || b.Name.startsWith("[")) continue;
	const effects = [];
	for (const s of coreStats.filter((x) => x.SoulCore === i)) {
		const values = new Map();
		(s.Stats || []).forEach((k, j) => values.set(stats[k].Id, s.StatsValues[j]));
		const lines = describe(values, "English");
		const linesKo = describe(values, "Korean");
		const label = catLabel(s.StatCategory);
		// 결속 효과(10-09 C238 — SoulCoreStats.BondedStats · BondedStatsValues, 인게임 "결속됨:" 아래 같은 분류 줄. poe2db 사막 룬 "무도 무기: 점화 강도 30% 증가")
		const bonded = new Map();
		(s.BondedStats || []).forEach((k, j) => bonded.set(stats[k].Id, (s.BondedStatsValues || [])[j]));
		const bondedLines = bonded.size ? describe(bonded, "English") : [];
		const bondedLinesKo = bonded.size ? describe(bonded, "Korean") : [];
		// 일반 줄 없이 결속 줄만 있는 분류 행도 남긴다(10-23 Q79 — 기본 룬 12종 무도 무기 · 마법봉/지팡이 · 방어구 결속 36줄이 통째로 빠졌다)
		if (!lines.length && !bondedLines.length) continue;
		effects.push({
			target: label.en,
			targetEn: label.enDisplay,
			targetKo: label.ko,
			targetClasses: (cats[s.StatCategory].TargetItemClasses || []).map((x) => classes[x].Id),
			lines,
			linesKo: linesKo.length === lines.length ? linesKo : lines,
			...(bondedLines.length ? { bondedLines, bondedLinesKo: bondedLinesKo.length === bondedLines.length ? bondedLinesKo : bondedLines } : {}),
		});
	}
	if (!effects.length) continue; // 효과 없는 자리표시 행
	const type = c.Type != null ? types[c.Type] : null;
	const lim = c.Limit != null ? limits[c.Limit] : null;
	items.push({
		name: b.Name,
		nameKo: basesKo[c.BaseItemType].Name || b.Name,
		slug: slug(b.Name),
		kind: type ? KIND[type.Id] || type.Id.toLowerCase() : "rune",
		kindKo: type ? stripMarkup(typesKo[c.Type].Name) : null,
		level: c.RequiredLevel || 0,
		limit: lim ? lim.Limit : null,
		limitText: lim && lim.Text ? stripMarkup(lim.Text).replace("{0}", String(lim.Limit)) : null,
		limitTextKo: lim && limitsKo[c.Limit].Text ? stripMarkup(limitsKo[c.Limit].Text).replace("{0}", String(lim.Limit)) : null,
		stackSize: stackByBase.get(c.BaseItemType) > 1 ? stackByBase.get(c.BaseItemType) : null,
		effects,
		icon: b.ItemVisualIdentity != null ? visuals[b.ItemVisualIdentity]?.DDSFile || null : null,
	});
}
items.sort((a, b) => a.kind.localeCompare(b.kind) || a.level - b.level || a.name.localeCompare(b.name));
const file = writeJson("augments.json", { patch: loadConfig().patch, items });
const by = items.reduce((m, x) => ((m[x.kind] = (m[x.kind] || 0) + 1), m), {});
const untranslated = items.reduce((n, x) => n + x.effects.filter((e) => e.linesKo.join() === e.lines.join()).length, 0);
console.log(`[poe2 augments] ${items.length}개 → ${file}`, by, `효과 한국어 미번역 ${untranslated}`);
