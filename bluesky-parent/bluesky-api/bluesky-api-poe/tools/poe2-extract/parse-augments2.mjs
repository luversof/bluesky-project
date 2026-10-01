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
const describe = createDescriber();

// 분류 한국어 표시 — 게임 문구가 비어 있으면 대상 아이템 클래스 한국어 이름으로
const FALLBACK_KO = { All: "모든 장비", Armour: "방어구", "Martial Weapon": "무도 무기" };
const catLabel = (i) => {
	const en = cats[i].Id;
	const ko = stripMarkup(catsKo[i].Display || "");
	if (ko) return { en, ko };
	const targets = (cats[i].TargetItemClasses || []).map((c) => classesKo[c].Name).filter(Boolean);
	return { en, ko: FALLBACK_KO[en] || (targets.length ? targets.join(", ") : en) };
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
		if (!lines.length) continue;
		const label = catLabel(s.StatCategory);
		effects.push({
			target: label.en,
			targetKo: label.ko,
			targetClasses: (cats[s.StatCategory].TargetItemClasses || []).map((x) => classes[x].Id),
			lines,
			linesKo: linesKo.length === lines.length ? linesKo : lines,
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
		effects,
		icon: b.ItemVisualIdentity != null ? visuals[b.ItemVisualIdentity]?.DDSFile || null : null,
	});
}
items.sort((a, b) => a.kind.localeCompare(b.kind) || a.level - b.level || a.name.localeCompare(b.name));
const file = writeJson("augments.json", { patch: loadConfig().patch, items });
const by = items.reduce((m, x) => ((m[x.kind] = (m[x.kind] || 0) + 1), m), {});
const untranslated = items.reduce((n, x) => n + x.effects.filter((e) => e.linesKo.join() === e.lines.join()).length, 0);
console.log(`[poe2 augments] ${items.length}개 → ${file}`, by, `효과 한국어 미번역 ${untranslated}`);
