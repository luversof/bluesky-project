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
const requiresDualWield = [];
const shieldAttackOwners = new Set();
let files = 0;
for (const file of readdirSync(skillsDir)) {
	if (!file.endsWith(".lua")) continue;
	files++;
	const text = readFileSync(join(skillsDir, file), "utf8");
	// 위치 기준으로 name 과 weaponTypes 를 정렬해 "가장 가까운 앞선 name" 에 귀속시킨다
	const names = [];
	for (const m of text.matchAll(NAME_RE)) names.push({ at: m.index, name: m[1] });
	// 메인 스킬명 = 바로 뒤에 baseTypeName 이 오는 name (하위 파트명은 baseTypeName 이 없다). baseFlags 처럼
	//   파트명 뒤에 나오는 속성을 파트가 아니라 메인 스킬에 귀속시킬 때 쓴다.
	const mainNames = names.filter((n) => text.slice(n.at, n.at + 80).includes("baseTypeName"));
	// 방패 필요 스킬(방패 강타 등) — 방패가 없으면 PoB 가 스킬을 비활성 처리한다
	for (const m of text.matchAll(/skillTypes\s*=\s*\{([^}]*)\}/g)) {
		let owner = null;
		for (const n of names) {
			if (n.at < m.index) owner = n.name;
			else break;
		}
		if (!owner) continue;
		if (m[1].includes("RequiresShield") && !requiresShield.includes(owner)) requiresShield.push(owner);
		// 쌍수 전용 스킬(듀얼 스트라이크 등) — 오프핸드에 두 번째 무기가 없으면 PoB 가 스킬을 비활성 처리한다.
		if (m[1].includes("DualWieldOnly") && !requiresDualWield.includes(owner)) requiresDualWield.push(owner);
	}
	// 방패 공격 스킬(신성한 작렬 등) — RequiresShield 스킬타입이 아니라 baseFlags.shieldAttack 로 표기된다.
	// (baseFlags 는 여러 줄 블록이라 skillTypes 단일줄 정규식으론 안 잡힌다.) 방패가 데미지원이라 방패 필수.
	//   한손 무기 기본값은 파일 스캔이 끝난 뒤(실제 weaponTypes 존중) 부여한다.
	for (const m of text.matchAll(/shieldAttack\s*=\s*true/g)) {
		// baseFlags 는 하위 파트명 뒤에 올 수 있어 메인 스킬명(baseTypeName 동반)에만 귀속시킨다.
		let owner = null;
		for (const n of mainNames) {
			if (n.at < m.index) owner = n.name;
			else break;
		}
		if (!owner) continue;
		if (!requiresShield.includes(owner)) requiresShield.push(owner);
		shieldAttackOwners.add(owner);
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

// 방패 공격 스킬에 명시적 weaponTypes 가 없으면 한손 세트를 부여 — 2H 무기로 방패가 빠지는 걸 막는다.
//   (파일 스캔 완료 후 실행 → 실제 weaponTypes 가 있으면 그 값이 이미 result 에 있어 존중된다.)
const SHIELD_ATTACK_1H = [
	"Claw", "Dagger", "None", "One Handed Axe", "One Handed Mace",
	"One Handed Sword", "Sceptre", "Thrusting One Handed Sword",
];
for (const owner of shieldAttackOwners) {
	if (!result[owner]) result[owner] = SHIELD_ATTACK_1H.slice();
}

writeFileSync(OUT, JSON.stringify({ weapons: result, requiresShield, requiresDualWield }));
const counts = {};
for (const types of Object.values(result)) {
	for (const t of types) counts[t] = (counts[t] || 0) + 1;
}
console.log(`무기 제한 스킬 ${Object.keys(result).length}개 · 방패 필요 ${requiresShield.length}개 (파일 ${files}개) → ${OUT}`);
console.log("종류별:", Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 8).map(([k, v]) => `${k} ${v}`).join(", "));
