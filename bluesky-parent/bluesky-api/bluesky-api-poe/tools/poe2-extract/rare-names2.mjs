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
const out = path.join(DATA_DIR, "rare-name-words.json");
fs.writeFileSync(out, JSON.stringify({ prefix, suffix }));
console.log(`레어 이름 낱말: 접두 ${Object.keys(prefix).length} · 접미 ${Object.keys(suffix).length} → ${out}`);
