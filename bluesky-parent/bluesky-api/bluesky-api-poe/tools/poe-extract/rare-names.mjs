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
const out = path.join(DATA_DIR, "rare-name-words.json");
fs.writeFileSync(out, JSON.stringify({ prefix, suffix, bases }));
console.log(`레어 이름 낱말: 접두 ${Object.keys(prefix).length} · 접미 ${Object.keys(suffix).length} · 추가 베이스 ${Object.keys(bases).length} → ${out}`);
