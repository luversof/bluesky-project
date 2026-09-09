// 망령(Spectre) 목록 추출 — PoB 의 Data/Spectres.lua 를 JSON 으로 옮긴다.
//
// 발단(2026-09-09): 유령 소환 축이 **망령을 아예 소환하지 않고** 있었다. PoB 는 빌드 XML 의
//   `<Build>` 자식 `<Spectre id="Metadata/..."/>` 로만 spectreList 를 채우는데(Build.lua:987-991)
//   우리가 그 섹션을 안 써서 spectreList=0 -> minionList=0 -> 미니언 미생성이었다.
//   그 축의 값 6,035,036 은 전부 Hextoad 보조젬이 만드는 Bursting Toad 에서 나온 것이었다(엔진 진단으로 확인).
//   최적화기가 망령을 고르려면 후보 목록이 필요하다 — 그게 이 산출물이다.
//
// ⚠ 정규식으로 파싱하지 말 것(셸/heredoc 을 거치며 백슬래시가 유실돼 몇 번 깨졌다). 문자열 탐색으로 충분하다.
// 사용법: node parse-spectres.mjs
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, WORK_DIR } from "./paths.mjs";

const srcFile = path.join(WORK_DIR, "pob-src", "src", "Data", "Spectres.lua");
if (!fs.existsSync(srcFile)) {
	console.warn("Spectres.lua 없음 — 건너뜀(PoB 소스 클론 후 재실행):", srcFile);
	process.exit(0);
}
const text = fs.readFileSync(srcFile, "utf8");
const Q = String.fromCharCode(34);
const HEAD = "minions[" + Q;

/** blockText 안에서 `key = ` 뒤 값을 한 줄 단위로 읽는다. */
function scalar(block, key) {
	const at = block.indexOf(key + " = ");
	if (at < 0) return null;
	const from = at + key.length + 3;
	const end = block.indexOf(",", from);
	return block.slice(from, end < 0 ? block.length : end).trim();
}
/** `key = { "a", "b", }` 를 문자열 배열로. */
function list(block, key) {
	const at = block.indexOf(key + " = {");
	if (at < 0) return [];
	const from = at + key.length + 4;
	const end = block.indexOf("}", from);
	const body = block.slice(from, end < 0 ? block.length : end);
	const out = [];
	let i = body.indexOf(Q);
	while (i >= 0) {
		const j = body.indexOf(Q, i + 1);
		if (j < 0) break;
		out.push(body.slice(i + 1, j));
		i = body.indexOf(Q, j + 1);
	}
	return out;
}

const spectres = [];
let cursor = text.indexOf(HEAD);
while (cursor >= 0) {
	const idFrom = cursor + HEAD.length;
	const idTo = text.indexOf(Q, idFrom);
	const id = text.slice(idFrom, idTo);
	const next = text.indexOf(HEAD, idTo);
	const block = text.slice(idTo, next < 0 ? text.length : next);
	const num = (k) => { const v = scalar(block, k); const n = v === null ? NaN : Number(v); return Number.isFinite(n) ? n : null; };
	const nameRaw = scalar(block, "name");
	spectres.push({
		id,
		name: nameRaw ? nameRaw.split(Q).join("") : "",
		tags: list(block, "monsterTags"),
		skills: list(block, "skillList"),
		damage: num("damage"),
		life: num("life"),
		attackTime: num("attackTime"),
	});
	cursor = next;
}

const outFile = path.join(DATA_DIR, "spectres.json");
fs.writeFileSync(outFile, JSON.stringify({ count: spectres.length, spectres }, null, "	"));
const withSkills = spectres.filter((s) => s.skills.length > 1).length;
console.log("spectres.json — " + spectres.length + "종 (스킬 2개 이상 " + withSkills + "종)");
