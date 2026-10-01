// PoE2 장비 옵션(접두·접미) → ~/.poe-gamedata/poe2/mods.json
// 풀 = 같은 아이템 클래스에서 스폰 태그 조합이 같은 베이스 묶음(갑옷은 방어도/회피/에너지 보호막/복합이 서로 다른 풀).
// 스폰 판정: 모드의 SpawnWeight_Tags 를 순서대로 보며 베이스 태그에 처음 걸린 태그의 가중치(>0 이면 붙는다) — 게임 규칙 그대로.
// 티어 사다리는 ModType(같은 계열) 단위, 레벨이 높은 것이 1티어(인게임 표기와 같다).
import { createDescriber, stripMarkup } from "./common2.mjs";
import { loadConfig, loadTable, writeJson } from "./paths.mjs";

const T = (t) => [loadTable("English", t), loadTable("Korean", t)];
const [mods, modsKo] = T("Mods");
const [modTypes] = T("ModType");
const [stats] = T("Stats");
const [tags] = T("Tags");
const [bases] = T("BaseItemTypes");
const [classes, classesKo] = T("ItemClasses");
const baseItems = JSON.parse(
	(await import("node:fs")).readFileSync(
		(await import("node:path")).join((await import("./paths.mjs")).DATA_DIR, "base-items.json"),
		"utf8",
	),
);
const describe = createDescriber();

// 모드 도메인 — 1 ITEM · 2 FLASK(플라스크·호신부) · 11 JEWEL. 플라스크 도메인 옵션은 스폰 태그가 default 뿐이라
//   도메인으로 가르지 않으면 활에 "사용 1회당 충전 소모량" 이 붙는다(2026-09-30 실측).
const domainsFor = (category) => (category === "flask" ? [2] : category === "jewel" ? [11] : [1]);
const GEN = { 1: "prefix", 2: "suffix" };
const baseIdxByName = new Map();
bases.forEach((b, i) => {
	if (!baseIdxByName.has(b.Name)) baseIdxByName.set(b.Name, i);
});

// 베이스 태그 — 게임 BaseItemTypes.Tags 는 직접 붙은 태그뿐이고 상속 태그(weapon·body_armour·armour …)는 .it 상속에서 온다.
//   PoB 베이스 목록의 tags 가 상속까지 푼 것이라 둘을 합친다(게임 태그만 쓰면 활에 붙는 옵션 계열이 5개로 줄었다, 2026-09-30).
//   *_basetype(지역 계열 표시)은 스폰과 무관해 풀 구분에서 뺀다.
const tagIds = (i, item) => [...new Set(["default", ...(item.tags || []), ...(bases[i].Tags || []).map((t) => tags[t].Id)])];
const signature = (ids) => ids.filter((t) => !t.endsWith("_basetype")).sort().join(",");

/** 이 모드가 태그 집합에 붙는 가중치(0 = 안 붙음). */
function spawnWeight(mod, tagSet) {
	const ts = mod.SpawnWeight_Tags || [];
	const vs = mod.SpawnWeight_Values || [];
	for (let k = 0; k < ts.length; k++) {
		if (tagSet.has(tags[ts[k]].Id)) return vs[k] || 0;
	}
	return 0;
}

/** 최소·최대 값으로 각각 문장을 만든 뒤 숫자를 짝지어 합친다 — 영어 "(10-19)", 한국어 "10~19". */
function rangeText(mod, lang) {
	const lo = new Map(), hi = new Map();
	for (let i = 1; i <= 6; i++) {
		if (mod["Stat" + i] == null) continue;
		const v = mod["Stat" + i + "Value"];
		const [a, b] = Array.isArray(v) ? v : [v, v];
		lo.set(stats[mod["Stat" + i]].Id, a);
		hi.set(stats[mod["Stat" + i]].Id, b);
	}
	const L = describe(lo, lang), H = describe(hi, lang);
	return L.map((line, k) => {
		const other = H[k];
		if (!other || other === line) return line;
		const ln = line.match(/-?\d+(?:\.\d+)?/g) || [], hn = other.match(/-?\d+(?:\.\d+)?/g) || [];
		if (ln.length !== hn.length) return line;
		let idx = 0;
		return line.replace(/-?\d+(?:\.\d+)?/g, (n, offset) => {
			let a = n, b = hn[idx++];
			if (b === a) return a;
			// 음수 스탯(감소)은 최소·최대가 뒤집혀 나온다 — 작은 값부터
			if (Number(a) > Number(b)) [a, b] = [b, a];
			if (lang !== "Korean") return `(${a}-${b})`;
			// "{0}~{1} 추가" 처럼 템플릿 자체에 물결이 붙은 자리는 괄호로 묶어야 읽힌다: (72~81)~(110~123)
			const near = line[offset - 1] === "~" || line[offset + n.length] === "~";
			return near ? `(${a}~${b})` : `${a}~${b}`;
		});
	});
}

// 풀 만들기 — base-items.json(PoB 기준 출시 베이스)만 대상
const pools = new Map();
for (const item of baseItems.items) {
	const i = baseIdxByName.get(item.name);
	if (i == null) continue;
	const ids = tagIds(i, item);
	const key = item.itemClass + "|" + signature(ids);
	if (!pools.has(key)) pools.set(key, { key, itemClass: item.itemClass, itemClassKo: item.itemClassKo, category: item.category, domains: domainsFor(item.category), tagSet: new Set(ids), bases: [] });
	pools.get(key).bases.push(item.name);
}

// 풀 이름(같은 클래스에 풀이 여럿일 때) — 방어 속성 태그로 붙인다
const DEF = [
	["str_dex_int_armour", "Armour/Evasion/Energy Shield", "방어도/회피/에너지 보호막"],
	["str_dex_armour", "Armour/Evasion", "방어도/회피"],
	["str_int_armour", "Armour/Energy Shield", "방어도/에너지 보호막"],
	["dex_int_armour", "Evasion/Energy Shield", "회피/에너지 보호막"],
	["str_armour", "Armour", "방어도"],
	["dex_armour", "Evasion", "회피"],
	["int_armour", "Energy Shield", "에너지 보호막"],
	["strjewel", "Ruby", "루비"],
	["dexjewel", "Emerald", "에메랄드"],
	["intjewel", "Sapphire", "사파이어"],
];

const out = [];
for (const pool of pools.values()) {
	const byType = new Map();
	for (let mi = 0; mi < mods.length; mi++) {
		const m = mods[mi];
		if (!pool.domains.includes(m.Domain) || !GEN[m.GenerationType]) continue;
		const w = spawnWeight(m, pool.tagSet);
		if (w <= 0) continue;
		const typeName = modTypes[m.ModType]?.Name || m.Id;
		const k = GEN[m.GenerationType] + "|" + typeName;
		if (!byType.has(k)) byType.set(k, { modType: typeName, gen: GEN[m.GenerationType], tiers: [] });
		byType.get(k).tiers.push({
			id: m.Id,
			name: stripMarkup(m.Name),
			nameKo: stripMarkup(modsKo[mi].Name) || null,
			level: m.Level,
			weight: w,
			text: rangeText(m, "English"),
			textKo: rangeText(m, "Korean"),
		});
	}
	const groups = [...byType.values()]
		.map((g) => ({ ...g, tiers: g.tiers.sort((a, b) => b.level - a.level || a.id.localeCompare(b.id)) }))
		.filter((g) => g.tiers.some((t) => t.text.length))
		// 표시 순서: 접두 먼저, 같은 쪽은 최고 티어 레벨이 낮은(흔한) 계열부터가 아니라 이름순 — 목록 화면에서 찾기 쉽게
		.sort((a, b) => (a.gen === b.gen ? a.tiers[a.tiers.length - 1].text.join().localeCompare(b.tiers[b.tiers.length - 1].text.join()) : a.gen === "prefix" ? -1 : 1));
	// 다이아몬드처럼 힘·민첩·지능 주얼 태그를 다 가진 베이스는 첫 매치(루비)로 잘못 붙었다 → 둘 이상이면 속성 이름 없이(아래에서 베이스 이름으로)
	const jewelAttrs = ["strjewel", "dexjewel", "intjewel"].filter((t) => pool.tagSet.has(t)).length;
	const def = jewelAttrs > 1 ? null : DEF.find(([t]) => pool.tagSet.has(t));
	out.push({
		key: pool.key,
		itemClass: pool.itemClass,
		itemClassKo: pool.itemClassKo,
		category: pool.category,
		variant: def ? def[1] : null,
		variantKo: def ? def[2] : null,
		bases: pool.bases,
		groups,
	});
}
// 태그 조합은 달라도(룬 제련 베이스 등) 붙는 옵션이 완전히 같은 풀은 하나로 합친다 — 화면에서 같은 표가 두 번 나오지 않게
const merged = new Map();
for (const p of out) {
	const ids = p.groups.flatMap((g) => g.tiers.map((t) => t.id + ":" + t.weight)).sort().join(",");
	const k = p.itemClass + "|" + ids;
	if (merged.has(k)) merged.get(k).bases.push(...p.bases);
	else merged.set(k, p);
}
out.length = 0;
out.push(...merged.values());
// 같은 클래스·같은 변형 이름인데 옵션이 다른 풀(마법봉 원소별 · 지팡이 · 시간을 잃은 주얼 …)은 화면 칩이 똑같이 "마법봉" 으로 여러 개 보였다
//   → 베이스 이름으로 구분한다(3개 넘으면 앞 2개 + 나머지 수). 키도 이 이름을 써서 번호(|2)보다 안정적으로.
const baseKoByName = new Map(baseItems.items.map((b) => [b.name, b.nameKo || b.name]));
const sameLabel = new Map();
for (const p of out) {
	const k = p.itemClass + "|" + (p.variant || "");
	if (!sameLabel.has(k)) sameLabel.set(k, []);
	sameLabel.get(k).push(p);
}
for (const list of sameLabel.values()) {
	if (list.length < 2) continue;
	for (const p of list) {
		const names = [...new Set(p.bases)];
		const en = names.length <= 3 ? names.join(" · ") : `${names.slice(0, 2).join(" · ")} +${names.length - 2}`;
		const koNames = names.map((n) => baseKoByName.get(n) || n);
		const ko = koNames.length <= 3 ? koNames.join(" · ") : `${koNames.slice(0, 2).join(" · ")} 외 ${koNames.length - 2}종`;
		p.variant = (p.variant ? p.variant + " — " : "") + en;
		p.variantKo = (p.variantKo ? p.variantKo + " — " : "") + ko;
	}
}
// 그래도 같은 이름이 남으면(베이스까지 같을 수는 없지만 방어용) 뒤에 번호
const seenVariant = new Map();
for (const p of out) {
	const k = p.itemClass + "|" + (p.variant || "");
	const n = (seenVariant.get(k) || 0) + 1;
	seenVariant.set(k, n);
	p.key = p.itemClass + "|" + (p.variant || "기본") + (n > 1 ? "|" + n : "");
}
out.sort((a, b) => a.itemClass.localeCompare(b.itemClass) || (a.variant || "").localeCompare(b.variant || "") || b.bases.length - a.bases.length);
const file = writeJson("mods.json", { patch: loadConfig().patch, pools: out });
const tiers = out.reduce((n, p) => n + p.groups.reduce((m, g) => m + g.tiers.length, 0), 0);
const ko = out.reduce((n, p) => n + p.groups.reduce((m, g) => m + g.tiers.filter((t) => t.textKo.join() !== t.text.join()).length, 0), 0);
console.log(`[poe2 mods] 풀 ${out.length}개 · 계열 ${out.reduce((n, p) => n + p.groups.length, 0)} · 티어 ${tiers}(한국어 ${ko}) → ${file}`);
void classes;
void classesKo;
