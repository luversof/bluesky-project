// 스킬별 **사용 가능한 무기 종류** 추출 — PoB Data/Skills/*.lua 의 weaponTypes.
// 게임 테이블 추출본(ActiveSkills)엔 이 정보가 없고, 젬 태그로도 알 수 없다(예: 마력 착취는 태그가
// [Critical, Attack, Projectile] 뿐인데 실제로는 **완드 전용**이다).
// 이게 없으면 최적화기가 완드 전용 스킬에 도끼를 쥐여주고, PoB 는 스킬을 비활성 처리해 수치가 무너진다.
import { readFileSync, readdirSync, writeFileSync, existsSync } from "node:fs";
import { join } from "node:path";
import { DATA_DIR, WORK_DIR } from "./paths.mjs";

const skillsDir = join(WORK_DIR, "pob-src", "src", "Data", "Skills");
const OUT = join(DATA_DIR, "skill-weapons.json");

if (!existsSync(skillsDir)) {
	console.warn("PoB 스킬 데이터 없음 — 건너뜀:", skillsDir);
	process.exit(0);
}

// 파일 전체를 훑으며 `name = "..."` 뒤에 오는 `weaponTypes = { ... }` 를 짝지어 모은다.
// (블록 파서 대신 순차 스캔 — Lua 를 실행하려면 SkillType 등 PoB 전역이 필요해 비용이 크다)
const NAME_RE = /\bname\s*=\s*"([^"]+)"/g;
const WEAPON_RE = /\bweaponTypes\s*=\s*\{([^}]*)\}/g;

const result = {};
const requiresShield = [];
let files = 0;
for (const file of readdirSync(skillsDir)) {
	if (!file.endsWith(".lua")) continue;
	files++;
	const text = readFileSync(join(skillsDir, file), "utf8");
	// 위치 기준으로 name 과 weaponTypes 를 정렬해 "가장 가까운 앞선 name" 에 귀속시킨다
	const names = [];
	for (const m of text.matchAll(NAME_RE)) names.push({ at: m.index, name: m[1] });
	// 방패 필요 스킬(방패 강타 등) — 방패가 없으면 PoB 가 스킬을 비활성 처리한다
	for (const m of text.matchAll(/skillTypes\s*=\s*\{([^}]*)\}/g)) {
		if (!m[1].includes("RequiresShield")) continue;
		let owner = null;
		for (const n of names) {
			if (n.at < m.index) owner = n.name;
			else break;
		}
		if (owner && !requiresShield.includes(owner)) requiresShield.push(owner);
	}
	for (const m of text.matchAll(WEAPON_RE)) {
		const types = Array.from(m[1].matchAll(/\["([^"]+)"\]\s*=\s*true/g)).map((t) => t[1]);
		if (!types.length) continue;
		let owner = null;
		for (const n of names) {
			if (n.at < m.index) owner = n.name;
			else break;
		}
		if (owner && !result[owner]) result[owner] = types;
	}
}

writeFileSync(OUT, JSON.stringify({ weapons: result, requiresShield }));
const counts = {};
for (const types of Object.values(result)) {
	for (const t of types) counts[t] = (counts[t] || 0) + 1;
}
console.log(`무기 제한 스킬 ${Object.keys(result).length}개 · 방패 필요 ${requiresShield.length}개 (파일 ${files}개) → ${OUT}`);
console.log("종류별:", Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 8).map(([k, v]) => `${k} ${v}`).join(", "));
