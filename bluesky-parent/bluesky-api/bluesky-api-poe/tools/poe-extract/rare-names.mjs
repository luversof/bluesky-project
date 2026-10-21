// 레어 아이템 이름 낱말 → ~/.poe-gamedata/rare-name-words.json (10-04 C143)
// 인게임 레어 이름 = 접두 낱말(Words.Wordlist 1, "Mind") + 접미 낱말(Wordlist 2, 앞에 공백 " Reach"). 한국어 클라이언트는 같은 행의 Text2 로
//   "마음의 역량" 처럼 보인다. 빌드 화면(PoB 임포트) 레어 이름을 API 가 이 표로 옮긴다. 접미는 앞 공백이 있는 행을 먼저(같은 낱말의 공백 없는 행은 다른 쓰임)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, loadTable } from "./paths.mjs";

const en = loadTable("English", "Words");
const ko = loadTable("Korean", "Words");
const prefix = {};
const suffix = {};
const usable = (s) => s && !/^\[DNT/.test(s);
en.forEach((w, i) => {
	const k = ko[i]?.Text2 || ko[i]?.Text;
	if (!usable(w.Text) || !usable(k)) return;
	if (w.Wordlist === 1 && !(w.Text in prefix)) prefix[w.Text] = k;
	if (w.Wordlist === 2) {
		const key = w.Text.trim();
		const spaced = w.Text.startsWith(" ");
		if (!(key in suffix) || spaced) suffix[key] = k.trim();
	}
});
// 장비 베이스 표(base-items.json)에 없는 베이스의 영 · 한(10-05 C162) — 마법 이름 "Potent Rosethorn Tincture of Mastery" 의 베이스를
//   찾아 옮기려고. 팅크처는 parse-items 장비 화이트리스트 밖이라 베이스 표에 넣으면 분류 · 화면 · 최적화기 후보까지 번진다 → 이름 번역용으로만.
const EXTRA_BASE_CLASSES = new Set(["Tincture"]);
const enBases = loadTable("English", "BaseItemTypes");
const koBases = loadTable("Korean", "BaseItemTypes");
const enClasses = loadTable("English", "ItemClasses");
const bases = {};
enBases.forEach((b, i) => {
	const cls = enClasses[b.ItemClassesKey];
	const k = koBases[i]?.Name;
	if (cls && EXTRA_BASE_CLASSES.has(cls.Id) && usable(b.Name) && usable(k) && !(b.Name in bases)) bases[b.Name] = k;
});
// 종류별 낱말(10-20 C488 — 제작 에뮬레이터가 레어 이름을 지을 때) — 낱말 생성 규칙 SpawnWeight_Tags/Values 를 그 종류 베이스 태그로 앞에서부터
//   보고 처음 맞는 태그의 가중치가 0 보다 크면 쓸 수 있다(옵션 생성과 같은 규칙). 종류 = ItemClasses.Id, 낱말 = 영어(한국어는 prefix · suffix 표)
const enTags = loadTable("English", "Tags");
// 종류 태그는 베이스 TagsKeys 에 없다(게임은 베이스 종류 파일 상속으로 붙임 — Titan Gauntlets = [str_armour, top_tier…]) → 종류마다 상속 태그를 둔다
const INHERITED = {
	Gloves: ["gloves", "armour"],
	Boots: ["boots", "armour"],
	Helmet: ["helmet", "armour"],
	"Body Armour": ["body_armour", "armour"],
	Shield: ["shield", "armour"],
	Ring: ["ring"],
	Amulet: ["amulet"],
	Belt: ["belt"],
	Quiver: ["quiver"],
	Bow: ["bow", "ranged", "two_hand_weapon", "weapon"],
	Claw: ["claw", "one_hand_weapon", "weapon"],
	Dagger: ["dagger", "one_hand_weapon", "weapon"],
	"Rune Dagger": ["rune_dagger", "dagger", "one_hand_weapon", "weapon"],
	"One Hand Axe": ["axe", "one_hand_weapon", "weapon"],
	"Two Hand Axe": ["axe", "two_hand_weapon", "weapon"],
	"One Hand Mace": ["mace", "one_hand_weapon", "weapon"],
	Sceptre: ["sceptre", "one_hand_weapon", "weapon"],
	"Two Hand Mace": ["mace", "two_hand_weapon", "weapon"],
	"One Hand Sword": ["sword", "one_hand_weapon", "weapon"],
	"Thrusting One Hand Sword": ["rapier", "sword", "one_hand_weapon", "weapon"],
	"Two Hand Sword": ["sword", "two_hand_weapon", "weapon"],
	Staff: ["staff", "two_hand_weapon", "weapon"],
	Warstaff: ["warstaff", "staff", "two_hand_weapon", "weapon"],
	Wand: ["wand", "one_hand_weapon", "weapon"],
	Jewel: ["jewel"],
	AbyssJewel: ["abyss_jewel", "jewel"],
	LifeFlask: ["flask"],
	ManaFlask: ["flask"],
	HybridFlask: ["flask"],
	UtilityFlask: ["flask"],
};
const classTags = {};
enBases.forEach((b) => {
	const cls = enClasses[b.ItemClassesKey];
	if (!cls || !usable(b.Name) || !INHERITED[cls.Id]) return;
	const set = (classTags[cls.Id] ??= new Set(["default", ...INHERITED[cls.Id]]));
	(b.TagsKeys || []).forEach((t) => enTags[t] && set.add(enTags[t].Id));
});
const classWords = {};
const allowed = (w, tags) => {
	const ts = w.SpawnWeight_Tags || [];
	const vs = w.SpawnWeight_Values || [];
	for (let i = 0; i < ts.length; i++) {
		const id = enTags[ts[i]]?.Id;
		if (id && tags.has(id)) return (vs[i] ?? 0) > 0;
	}
	return false;
};
for (const [cls, tags] of Object.entries(classTags)) {
	const pre = new Set();
	const suf = new Set();
	en.forEach((w, i) => {
		const k = ko[i]?.Text2 || ko[i]?.Text;
		if (!usable(w.Text) || !usable(k) || !allowed(w, tags)) return;
		if (w.Wordlist === 1 && prefix[w.Text]) pre.add(w.Text);
		if (w.Wordlist === 2 && suffix[w.Text.trim()]) suf.add(w.Text.trim());
	});
	if (pre.size && suf.size) classWords[cls] = { prefix: [...pre], suffix: [...suf] };
}
const out = path.join(DATA_DIR, "rare-name-words.json");
fs.writeFileSync(out, JSON.stringify({ prefix, suffix, bases, classWords }));
console.log(`종류별 레어 이름 낱말: ${Object.keys(classWords).length}종 (예: 장갑 접두 ${classWords.Gloves?.prefix.length ?? 0} · 접미 ${classWords.Gloves?.suffix.length ?? 0})`);
console.log(`레어 이름 낱말: 접두 ${Object.keys(prefix).length} · 접미 ${Object.keys(suffix).length} · 추가 베이스 ${Object.keys(bases).length} → ${out}`);
