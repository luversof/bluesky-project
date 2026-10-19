// PoE2 제작 데이터(10-15 C351 — 사용자 요청 "제작 에뮬레이터 · 시뮬레이터", P5) → ~/.poe-gamedata/poe2/craft2-mods.json
// 풀 = mods.json 풀(클래스|변형) 그대로, 옵션마다 접두/접미 · 레벨 · 그룹(Families, 같은 그룹은 한 아이템에 하나) · 태그(ImplicitTags,
// 동질화 징조 · 촉매용) · 계열(ModType).
// 가중치: PoE2 클라이언트 Mods 가중치는 0/1 뿐(GGG 비공개) — 엔진은 균등으로 굴리고 화면이 그렇게 밝힌다.
// 사용법: node parse-craft2.mjs  (사전: parse-mods2.mjs 가 만든 mods.json)
import fs from "node:fs";
import path from "node:path";
import { createDescriber } from "./common2.mjs";
import { DATA_DIR, loadConfig, loadTable, writeJson } from "./paths.mjs";

const mods = loadTable("English", "Mods");
const tags = loadTable("English", "Tags");
const modTypes = loadTable("English", "ModType");
const byId = new Map(mods.map((m) => [m.Id, m]));
const modsJson = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "mods.json"), "utf8"));
const GEN = { 1: "prefix", 2: "suffix" };

const out = {};
const pools = {};
for (const p of modsJson.pools) {
	const ids = [];
	for (const g of p.groups || []) {
		for (const t of g.tiers || []) {
			const m = byId.get(t.id);
			if (!m || !GEN[m.GenerationType]) continue;
			ids.push(t.id);
			if (!out[t.id])
				out[t.id] = {
					g: GEN[m.GenerationType],
					l: m.Level,
					grp: m.Families || [],
					t: (m.ImplicitTags || []).map((k) => tags[k]?.Id).filter(Boolean),
					mt: modTypes[m.ModType]?.Id || String(m.ModType),
				};
		}
	}
	pools[p.key] = ids;
}
// 징조(BaseItemTypes "Omen of …") — 키 = 이름 뒤를 붙여 쓴 것(SinistralExaltation), 영 · 한 이름 · 아이콘 슬러그
const basesEn = loadTable("English", "BaseItemTypes");
const basesKo = loadTable("Korean", "BaseItemTypes");
const omens = [];
const seenOmen = new Set();
basesEn.forEach((b, i) => {
	const m = /^Omen of (?:the )?(.+)$/.exec(b?.Name || "");
	if (!m || seenOmen.has(b.Name)) return;
	seenOmen.add(b.Name);
	omens.push({ key: m[1].replace(/[^A-Za-z]/g, ""), name: b.Name, nameKo: basesKo[i]?.Name || b.Name, slug: (b.Id || "").split("/").pop() });
});
// 제작 화폐(엔진 동작 · 등급) — 영 · 한 공식 이름 · 아이콘 슬러그(BaseItemTypes)
const CURRENCY = [
	["TRANSMUTE", "", "Orb of Transmutation"], ["TRANSMUTE", "greater", "Greater Orb of Transmutation"], ["TRANSMUTE", "perfect", "Perfect Orb of Transmutation"],
	["AUGMENTATION", "", "Orb of Augmentation"], ["AUGMENTATION", "greater", "Greater Orb of Augmentation"], ["AUGMENTATION", "perfect", "Perfect Orb of Augmentation"],
	["REGAL", "", "Regal Orb"], ["REGAL", "greater", "Greater Regal Orb"], ["REGAL", "perfect", "Perfect Regal Orb"],
	["ALCHEMY", "", "Orb of Alchemy"],
	["EXALT", "", "Exalted Orb"], ["EXALT", "greater", "Greater Exalted Orb"], ["EXALT", "perfect", "Perfect Exalted Orb"],
	["CHAOS", "", "Chaos Orb"], ["CHAOS", "greater", "Greater Chaos Orb"], ["CHAOS", "perfect", "Perfect Chaos Orb"],
	["ANNUL", "", "Orb of Annulment"],
	["FRACTURE", "", "Fracturing Orb"],
];
const currencies = [];
for (const [action, tier, name] of CURRENCY) {
	const i = basesEn.findIndex((b) => b?.Name === name);
	if (i < 0) {
		console.warn("화폐 없음:", name);
		continue;
	}
	currencies.push({ action, tier, name, nameKo: basesKo[i]?.Name || name, slug: (basesEn[i].Id || "").split("/").pop() });
}
// 기폭제(10-16 C386) — BaseItemTypes Id Currency(Jewellery|Jewel)Quality{종류}: 반지 · 목걸이 13 + 제련된(주얼) 13. tier = 종류 키(주얼은 "jewel:" 앞붙임).
//   설명 문구는 PoE2 표에 없어 poe2db(Catalysts, 한 · 영)와 같은 틀로 만든다 — "Adds quality that enhances X modifiers on a ring or amulet"
const CATALYST_LABEL = {
	life: ["Life", "생명력"], mana: ["Mana", "마나"], defences: ["Armour, Evasion and Energy Shield", "방어도, 회피, 에너지 보호막"],
	physical: ["Physical", "물리"], fire: ["Fire", "화염"], cold: ["Cold", "냉기"], lightning: ["Lightning", "번개"], chaos: ["Chaos", "카오스"],
	attack: ["Attack", "공격"], caster: ["Caster", "시전"], speed: ["Speed", "속도"], attribute: ["Attribute", "능력치"], minion: ["Minion", "소환수"],
};
basesEn.forEach((b, i) => {
	const slug = (b?.Id || "").split("/").pop();
	const m = /^Currency(Jewellery|Jewel)Quality(\w+)$/.exec(slug);
	const lab = m && CATALYST_LABEL[m[2].toLowerCase()];
	if (!lab) return;
	const jewel = m[1] === "Jewel";
	currencies.push({
		action: "CATALYST", tier: (jewel ? "jewel:" : "") + m[2].toLowerCase(), name: b.Name, nameKo: basesKo[i]?.Name || b.Name, slug,
		qualityLabel: lab[0] + " Modifiers", qualityLabelKo: lab[1] + " 속성",
		description: `Adds quality that enhances ${lab[0]} modifiers on a ${jewel ? "jewel" : "ring or amulet"}. Replaces other quality types`,
		descriptionKo: `퀄리티를 추가하여 ${jewel ? "주얼" : "반지나 목걸이"}에 부여된 ${lab[1]} 속성을 향상합니다. 기존 퀄리티 유형을 대체합니다.`,
	});
});
// 에센스(10-15 C353) — Essences · EssenceMods · EssenceTargetItemCategories. 하급 · 보통 · 상위 = 매직 → 레어 + 보장 옵션(보통 옵션 id),
//   완벽 · 타락(히스테리아 · 섬망 · 공포 · 광기 · 심연 · 균열) = 레어에서 하나 지우고 보장(전용 옵션, poe2db). 보장 옵션은 클래스마다(대상 분류 → 아이템 종류).
const essences = [];
const essenceMods = {};
try {
	const E = loadTable("English", "Essences");
	const EM = loadTable("English", "EssenceMods");
	const EMko = loadTable("Korean", "EssenceMods");
	const CAT = loadTable("English", "EssenceTargetItemCategories");
	const IC = loadTable("English", "ItemClasses");
	const stats = loadTable("English", "Stats");
	const modsKo = loadTable("Korean", "Mods");
	const describe = createDescriber();
	const rangeText = (mod, lang) => {
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
			return line.replace(/-?\d+(?:\.\d+)?/g, (n) => {
				let a = n, b = hn[idx++];
				if (b === a) return a;
				if (Number(a) > Number(b)) [a, b] = [b, a];
				return lang !== "Korean" ? `(${a}-${b})` : `${a}~${b}`;
			});
		});
	};
	const CORRUPT = /^Essence of (Hysteria|Delirium|Horror|Insanity|the Abyss|the Breach)$/;
	const byEssence = new Map();
	EM.forEach((row, i) => {
		const e = E[row.Essence];
		const m = mods[row.Mod];
		const cat = CAT[row.TargetItemCategory];
		if (!e || !m || !cat) return;
		const classes = (Array.isArray(cat.ItemClasses) ? cat.ItemClasses : [cat.ItemClasses]).map((k) => IC[k]?.Id).filter(Boolean);
		if (!byEssence.has(row.Essence)) byEssence.set(row.Essence, {});
		for (const c of classes) byEssence.get(row.Essence)[c] = m.Id;
		if (!out[m.Id] && !essenceMods[m.Id] && GEN[m.GenerationType]) {
			const mi = mods.indexOf(m);
			essenceMods[m.Id] = {
				g: GEN[m.GenerationType],
				l: m.Level,
				grp: m.Families || [],
				t: (m.ImplicitTags || []).map((k) => tags[k]?.Id).filter(Boolean),
				mt: modTypes[m.ModType]?.Id || String(m.ModType),
				name: m.Name || "",
				nameKo: modsKo[mi]?.Name || m.Name || "",
				en: rangeText(m, "English"),
				ko: rangeText(m, "Korean"),
			};
		}
	});
	for (const [ei, byClass] of byEssence) {
		const e = E[ei];
		const b = basesEn[e.BaseItemType];
		// 같은 표에 합금(Alloy, 룬 셰이프)도 있다 — poe2db "Removes a random modifier and augments a Rare item with a new guaranteed modifier"
		//   = 완벽 에센스와 같은 동작이라 kind "alloy" 로 싣는다(10-15 C356). 그 밖은 뺀다
		if (!b || !/Essence|Alloy/.test(b.Name || "")) continue;
		const corrupted = CORRUPT.test(b.Name);
		const alloy = /Alloy/.test(b.Name);
		essences.push({
			slug: (b.Id || "").split("/").pop(),
			name: b.Name,
			nameKo: basesKo[e.BaseItemType]?.Name || b.Name,
			tier: e.Tier || 0,
			kind: alloy ? "alloy" : e.Perfect ? "perfect" : corrupted ? "corrupted" : "normal",
			mods: byClass,
		});
	}
	essences.sort((a, b) => a.name.localeCompare(b.name));
} catch (e) {
	console.warn("에센스 표 없음 — 에센스 생략:", e.message);
}
// 탈신성화(10-15 C354, 심연) — 도메인 28 "AbyssMod…" 옵션(울라만 · 아마나무 · 쿠르갈 등)이 풀(태그)마다 붙는지 = parse-mods2 와 같은 스폰 판정.
//   뼈 = AbyssBenchTicketTypes(쓸 수 있는 종류 · 갉아먹힌 = 아이템 레벨 64 이하 · 고대 = 최소 옵션 레벨 40, poe2db).
const desecrated = {};
const desecratedMods = {};
// 바알 오브 타락 인챈트(10-16 C376) — GenerationType 5 · 도메인 1(Corruption… 108개), 풀마다 같은 스폰 판정
const corrupted = {};
const corruptedMods = {};
const bones = [];
try {
	const baseItems2 = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "base-items.json"), "utf8"));
	const itemByName = new Map(baseItems2.items.map((it) => [it.name, it]));
	const baseIdx = new Map();
	basesEn.forEach((b, i) => {
		if (b?.Name && !baseIdx.has(b.Name)) baseIdx.set(b.Name, i);
	});
	const stats2 = loadTable("English", "Stats");
	const modsKo2 = loadTable("Korean", "Mods");
	const describe2 = createDescriber();
	const text2 = (mod, lang) => {
		const lo = new Map(), hi = new Map();
		for (let i = 1; i <= 6; i++) {
			if (mod["Stat" + i] == null) continue;
			const v = mod["Stat" + i + "Value"];
			const [x, y] = Array.isArray(v) ? v : [v, v];
			lo.set(stats2[mod["Stat" + i]].Id, x);
			hi.set(stats2[mod["Stat" + i]].Id, y);
		}
		const L = describe2(lo, lang), H = describe2(hi, lang);
		return L.map((line, k) => {
			const other = H[k];
			if (!other || other === line) return line;
			const hn = other.match(/-?\d+(?:\.\d+)?/g) || [];
			let idx = 0;
			return line.replace(/-?\d+(?:\.\d+)?/g, (n) => {
				let x = n, y = hn[idx++];
				if (y == null || y === x) return x;
				if (Number(x) > Number(y)) [x, y] = [y, x];
				return lang !== "Korean" ? `(${x}-${y})` : `${x}~${y}`;
			});
		});
	};
	const cands = [];
	const corrCands = [];
	mods.forEach((m, i) => {
		if (m.Domain === 28 && GEN[m.GenerationType] && /^AbyssMod/.test(m.Id || "")) cands.push([m, i]);
		if (m.Domain === 1 && m.GenerationType === 5) corrCands.push([m, i]);
	});
	const lichOf = (name) => (/Ulaman/.test(name) ? "ulaman" : /Amanamu/.test(name) ? "amanamu" : /Kurgal/.test(name) ? "kurgal" : null);
	for (const p of modsJson.pools) {
		const tagSet = new Set(["default"]);
		for (const bn of p.bases || []) {
			const it = itemByName.get(bn);
			const bi = baseIdx.get(bn);
			for (const t of it?.tags || []) tagSet.add(t);
			for (const t of bi != null ? basesEn[bi].Tags || [] : []) if (tags[t]) tagSet.add(tags[t].Id);
		}
		const ids = [];
		for (const [m, i] of cands) {
			const ts = m.SpawnWeight_Tags || [];
			const vs = m.SpawnWeight_Values || [];
			let w = 0;
			for (let k = 0; k < ts.length; k++) {
				if (tagSet.has(tags[ts[k]]?.Id)) {
					w = vs[k] || 0;
					break;
				}
			}
			if (w <= 0) continue;
			ids.push(m.Id);
			if (!desecratedMods[m.Id])
				desecratedMods[m.Id] = {
					g: GEN[m.GenerationType],
					l: m.Level,
					grp: m.Families || [],
					t: (m.ImplicitTags || []).map((k) => tags[k]?.Id).filter(Boolean),
					name: m.Name || "",
					nameKo: modsKo2[i]?.Name || m.Name || "",
					lich: lichOf(m.Name || ""),
					en: text2(m, "English"),
					ko: text2(m, "Korean"),
				};
		}
		if (ids.length) desecrated[p.key] = ids;
		const cids = [];
		for (const [m, i] of corrCands) {
			const ts = m.SpawnWeight_Tags || [];
			const vs = m.SpawnWeight_Values || [];
			let w = 0;
			for (let k = 0; k < ts.length; k++) {
				if (tagSet.has(tags[ts[k]]?.Id)) {
					w = vs[k] || 0;
					break;
				}
			}
			if (w <= 0) continue;
			cids.push(m.Id);
			if (!corruptedMods[m.Id])
				corruptedMods[m.Id] = {
					g: "corrupted",
					l: m.Level,
					grp: m.Families || [],
					t: (m.ImplicitTags || []).map((k) => tags[k]?.Id).filter(Boolean),
					en: text2(m, "English"),
					ko: text2(m, "Korean"),
				};
		}
		if (cids.length) corrupted[p.key] = cids;
	}
	const ABT = loadTable("English", "AbyssBenchTicketTypes");
	const IC2 = loadTable("English", "ItemClasses");
	for (const r of ABT) {
		const b = basesEn[r.BaseItemType];
		if (!b) continue;
		bones.push({
			slug: (b.Id || "").split("/").pop(),
			name: b.Name,
			nameKo: basesKo[r.BaseItemType]?.Name || b.Name,
			classes: (Array.isArray(r.UsableOnItemClasses) ? r.UsableOnItemClasses : [r.UsableOnItemClasses]).map((k) => IC2[k]?.Id).filter(Boolean),
			maxIlvl: r.MaximumItemLevel || 0,
			minModLevel: r.MinimumModLevel || 0,
			breach: r.Tag != null,
		});
	}
} catch (e) {
	console.warn("탈신성화 데이터 생략:", e.message);
}
writeJson("craft2-mods.json", { patch: loadConfig().patch, mods: out, pools, omens, currencies, essences, essenceMods, desecrated, desecratedMods, bones, corrupted, corruptedMods });
console.log(`craft2-mods.json: 옵션 ${Object.keys(out).length} · 풀 ${Object.keys(pools).length} · 징조 ${omens.length} · 화폐 ${currencies.length} · 에센스 ${essences.length}(전용 옵션 ${Object.keys(essenceMods).length}) · 탈신성 옵션 ${Object.keys(desecratedMods).length}(풀 ${Object.keys(desecrated).length}) · 뼈 ${bones.length} · 타락 인챈트 ${Object.keys(corruptedMods).length}(풀 ${Object.keys(corrupted).length})`);
