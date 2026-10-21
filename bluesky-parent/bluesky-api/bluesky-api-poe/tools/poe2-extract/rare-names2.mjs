// 레어 아이템 이름 낱말 → ~/.poe-gamedata/poe2/rare-name-words.json (10-04 C144, PoE1 rare-names.mjs 짝)
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
// 종류별 낱말(10-20 C499, PoE1 C488 짝 — 제작 에뮬레이터 레어 이름) — SpawnWeight_Tags/Values 를 그 종류 베이스 태그로 앞에서부터 보고 처음 맞는
//   태그 가중치가 0 보다 크면 쓸 수 있다. PoE2 베이스 Tags 에는 종류 태그(gloves · armour …)가 들어 있다(PoE1 과 달리 상속 표가 필요 없음)
const enTags = loadTable("English", "Tags");
const enBases = loadTable("English", "BaseItemTypes");
const enClasses = loadTable("English", "ItemClasses");
const classTags = {};
enBases.forEach((b) => {
	const cls = enClasses[b.ItemClass];
	if (!cls || !usable(b.Name)) return;
	const set = (classTags[cls.Id] ??= new Set(["default", ...inherited(cls.Id)]));
	(b.Tags || []).forEach((t) => enTags[t] && set.add(enTags[t].Id));
});
// 종류 상속 태그 — 베이스 Tags 엔 gloves · armour 같은 종류 태그가 없다(PoE1 과 같음, 게임은 베이스 종류 파일 상속으로 붙임).
//   종류 이름(snake) · 마지막 낱말(mace · sword …) · 방어구/무기. 버클러는 방패 낱말을 쓴다(Words 에 buckler 태그가 없음)
function inherited(id) {
	const ARMOUR = new Set(["Gloves", "Boots", "Helmet", "Body Armour", "Shield", "Buckler", "Focus"]);
	const WEAPON = new Set(["Bow", "Crossbow", "Claw", "Dagger", "Flail", "One Hand Axe", "Two Hand Axe", "One Hand Mace", "Two Hand Mace", "One Hand Sword", "Two Hand Sword", "Sceptre", "Spear", "Staff", "Warstaff", "Wand", "Talisman"]);
	const words = id.split(" ");
	const out = [id.toLowerCase().replace(/ /g, "_"), words[words.length - 1].toLowerCase()];
	if (ARMOUR.has(id)) out.push("armour");
	if (WEAPON.has(id)) out.push("weapon", id.startsWith("Two") || ["Bow", "Crossbow", "Staff", "Warstaff", "Spear"].includes(id) ? "two_hand_weapon" : "one_hand_weapon");
	if (id === "Buckler") out.push("shield");
	return out;
}
const allowed = (w, tags) => {
	const ts = w.SpawnWeight_Tags || [];
	const vs = w.SpawnWeight_Values || [];
	for (let i = 0; i < ts.length; i++) {
		const id = enTags[ts[i]]?.Id;
		if (id && tags.has(id)) return (vs[i] ?? 0) > 0;
	}
	return false;
};
const classWords = {};
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
fs.writeFileSync(out, JSON.stringify({ prefix, suffix, classWords }));
console.log(`종류별 레어 이름 낱말: ${Object.keys(classWords).length}종 (예: 장갑 접두 ${classWords.Gloves?.prefix.length ?? 0} · 접미 ${classWords.Gloves?.suffix.length ?? 0})`);
console.log(`레어 이름 낱말: 접두 ${Object.keys(prefix).length} · 접미 ${Object.keys(suffix).length} → ${out}`);
