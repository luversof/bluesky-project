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
const OUT = path.join(process.env.POE_DATA_OUT || DATA_DIR, "watchers-eye.json"); // POE_DATA_OUT = 사본 출력(10-13)

if (!fs.existsSync(SRC)) {
	console.warn("WatchersEye.lua 없음 — PoB 소스 갱신 후 다시. 건너뜀");
	process.exit(0);
}
const toKo = createModTranslator(FILES_DIR, ["metadata@statdescriptions@stat_descriptions.txt", "metadata@statdescriptions@passive_skill_stat_descriptions.txt"]) /* 게임 passive 사슬 = stat → passive(C257 과 같다, 10-13 C288 — 기본 사슬의 gem/skill 문장이 이기지 않게) */;
const source = fs.readFileSync(SRC, "utf8");

// 음수 범위 "-(10-5)" 는 번역기가 못 맞춘다(10-13 C300 — 명상 "스킬의 총 마나 소모" 2개가 영어로 남았다). 안 옮겨졌을 때만
//   범위를 큰 쪽 값 하나("-10")로 바꿔 옮긴 뒤 그 자리에 범위를 되돌린다. 공용 번역기는 그대로(PoE2 음수 범위 표기는 보류 중인 결정)
const NEG_RANGE = /-\((\d+)-(\d+)\)/g;
function translate(text) {
	const ko = toKo([text])[0];
	if (ko && ko !== text) return ko;
	const ranges = [];
	const probe = text.replace(NEG_RANGE, (m, a) => (ranges.push([m, "-" + a]), "-" + a));
	if (!ranges.length) return text;
	let out = toKo([probe])[0];
	if (!out || out === probe) return text;
	for (const [range, single] of ranges) {
		const at = out.indexOf(single);
		if (at < 0) return text;
		out = out.slice(0, at) + range + out.slice(at + single.length);
	}
	return out;
}

// ["Key"] = { affix = "", "모드 문구", ["둘째 줄", …] statOrder = { 9519, 9519.1 } …
//   게임 설명이 두 줄인 한 문장은 PoB 가 문자열을 둘 두고 statOrder 에 ".1" 을 붙인다(10-13 C300 — "Prevent +(50-75)% of Reflected Lightning Damage
//   you would take while" · "affected by Purity of Lightning"). 예전엔 첫 문자열만 잡아 영어가 "… while" 에서 잘리고 오라도 못 찾았다(번개의 순수함 2 ·
//   원소의 순수함 1) → ".N" 이어짐은 공백으로 한 문장, 다른 statOrder 는 다른 줄("\n"). 오라는 한 줄짜리에서만 — 같은 파일에 든 Sublime Vision ·
//   석궁병 옵션(여러 줄)은 예전처럼 오라 없음(최적화기 감시자의 눈 후보에 안 들어간다)
const AURA = /while affected by ([A-Z][A-Za-z' ]+?)(?:$|[,.])/;
const mods = [];
for (const m of source.matchAll(/\["([^"]+)"\]\s*=\s*\{\s*affix\s*=\s*"[^"]*",((?:\s*"[^"]+",)+)\s*statOrder\s*=\s*\{([^}]*)\}/g)) {
	const id = m[1];
	const parts = [...m[2].matchAll(/"([^"]+)"/g)].map((x) => x[1]);
	const orders = m[3].split(",").map((x) => x.trim()).filter(Boolean);
	const lines = [];
	parts.forEach((part, i) => {
		if (i > 0 && /\.\d+$/.test(orders[i] || "") && lines.length) lines[lines.length - 1] += " " + part;
		else lines.push(part);
	});
	const aura = lines.length === 1 ? AURA.exec(lines[0]) : null;
	mods.push({ id, en: lines.join("\n"), ko: lines.map(translate).join("\n"), aura: aura ? aura[1].trim() : null });
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
