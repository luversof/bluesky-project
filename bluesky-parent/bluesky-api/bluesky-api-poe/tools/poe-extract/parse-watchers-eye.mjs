// 감시자의 눈(Watcher's Eye) 모드 풀 → watchers-eye.json.
// PoB 는 이 유니크를 아이템 목록이 아니라 **코드로 생성**한다(Data/Uniques/Special/WatchersEye.lua = 모드 풀).
// 그래서 우리 유니크 데이터엔 없고, 최적화기가 평생 못 쓴다 — 실빌드는 표준 장비로 낀다.
// 모드는 "… while affected by <오라>" 형태라, 지금 낀 오라에 맞는 것만 골라 합성할 수 있게 오라별로 묶어 둔다.
// 사용법: node parse-watchers-eye.mjs (사전: run-all 이 pob-src 를 갱신)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig } from "./paths.mjs";
import { createModTranslator } from "./statDescriptions.mjs";

const SRC = path.join(DATA_DIR, "work", "pob-src", "src", "Data", "Uniques", "Special", "WatchersEye.lua");
const OUT = path.join(DATA_DIR, "watchers-eye.json");

if (!fs.existsSync(SRC)) {
	console.warn("WatchersEye.lua 없음 — PoB 소스 갱신 후 다시. 건너뜀");
	process.exit(0);
}
const toKo = createModTranslator(FILES_DIR, ["metadata@statdescriptions@passive_skill_stat_descriptions.txt"]);
const source = fs.readFileSync(SRC, "utf8");

// ["Key"] = { affix = "", "모드 문구", statOrder = …
const AURA = /while affected by ([A-Z][A-Za-z' ]+?)(?:$|[,.])/;
const mods = [];
for (const m of source.matchAll(/\["([^"]+)"\]\s*=\s*\{\s*affix\s*=\s*"[^"]*",\s*"([^"]+)"/g)) {
	const [, id, text] = m;
	const aura = AURA.exec(text);
	mods.push({ id, en: text, ko: toKo([text])[0] || text, aura: aura ? aura[1].trim() : null });
}
const byAura = {};
for (const mod of mods) {
	const key = mod.aura || "(기타)";
	(byAura[key] ||= []).push(mod);
}
fs.writeFileSync(
	OUT,
	JSON.stringify({ patch: loadConfig().patch, mods, auras: Object.keys(byAura).sort() }, null, "\t"),
	"utf8",
);
console.log(
	`watchers-eye.json: 모드 ${mods.length}개 / 오라 ${Object.keys(byAura).length}종 → ${OUT}`,
);
