// 데이터 갱신 마지막 단계 — "시뮬레이터가 실제로 돌 수 있는 상태인가"를 갱신 시점에 확인한다.
//
// 여기서 안 잡으면 사용자는 나중에 시뮬을 돌리다 `스펙 임포트 실패(클래스 3 → 0)` 로 처음 알게 되고,
// 원인(PoB 소스가 옛 버전 / 타임리스 .bin stale)은 화면 어디에도 안 나온다. 실제로 다른 PC 에서 그 사고가 났다.
//
// 점검 항목은 **실제로 그 증상을 낸 적이 있는 것**만 둔다:
//   1) 앱이 쓰는 트리 버전의 PoB TreeData 가 있는가 — 없으면 스펙이 기본 클래스(사이온)로 떨어진다
//   2) 타임리스 주얼 .bin 이 원본 .zip 보다 최신인가 — stale 이면 LUT 가 어긋나 스펙 임포트가 중단된다
//   3) 상류 PoB nil 버그 패치가 입혀졌는가 — 안 입히면 일부 빌드가 조용히 빈 빌드 수치를 낸다
//
// 문제를 찾으면 **비정상 종료**해 run-all 이 실패로 표시하게 한다(조용히 넘어가면 점검하는 의미가 없다).
import fs from "node:fs";
import path from "node:path";
import { WORK_DIR } from "./paths.mjs";

const POB = path.join(WORK_DIR, "pob-src", "src");
const problems = [];
const notes = [];

// 앱이 쓰는 트리 버전 — Java 기본값(poe.sim.tree-version)과 같아야 한다.
// 서비스마다 기본값이 달라(최적화기 3_29 / 시뮬 3_28) **둘 다** 있어야 한다.
const TREE_VERSIONS = ["3_28", "3_29"];

if (!fs.existsSync(POB)) {
	console.log("PoB 소스 없음 — 빌드 재계산/시뮬레이터 비활성(데이터 갱신 자체는 정상)");
	process.exit(0);
}

// 1) 트리 데이터
for (const v of TREE_VERSIONS) {
	const dir = path.join(POB, "TreeData", v);
	if (fs.existsSync(dir)) {
		notes.push(`TreeData/${v} 있음`);
	} else {
		problems.push(`PoB 소스에 TreeData/${v} 없음 — 스펙 임포트가 기본 클래스로 떨어집니다(소스 갱신 실패?)`);
	}
}

// 2) 타임리스 .bin 신선도
const tj = path.join(POB, "Data", "TimelessJewelData");
if (fs.existsSync(tj)) {
	// ⚠ 폴더의 .zip 을 전부 요구하면 안 된다 — Abyss*.zip 처럼 추출 대상이 아닌 것이 섞여 있어
	//    "5개 없음" 같은 오탐이 난다(첫 구현에서 실제로 그랬다). **timeless-bin.mjs 와 같은 목록**만 본다.
	const NAMES = ["BrutalRestraint", "LethalPride", "MilitantFaith", "ElegantHubris", "HeroicTragedy", "GloriousVanity"];
	let stale = 0;
	let missing = 0;
	let checked = 0;
	for (const name of NAMES) {
		// 원본은 통짜 .zip 이거나 분할(.zip.part0…)이다 — 찬란한 허영심이 분할 형태다
		const sources = [path.join(tj, `${name}.zip`)].filter(fs.existsSync);
		if (sources.length === 0) {
			sources.push(
				...fs.readdirSync(tj).filter((f) => f.startsWith(`${name}.zip.part`)).map((f) => path.join(tj, f)),
			);
		}
		if (sources.length === 0) continue; // 이 소스 버전엔 없는 주얼 — 검사 대상 아님
		checked++;
		const bin = path.join(tj, `${name}.bin`);
		const newest = Math.max(...sources.map((f) => fs.statSync(f).mtimeMs));
		if (!fs.existsSync(bin) || fs.statSync(bin).size === 0) {
			missing++;
		} else if (fs.statSync(bin).mtimeMs < newest) {
			stale++;
		}
	}
	if (missing) problems.push(`타임리스 .bin ${missing}개 없음 — timeless-bin.mjs 가 실패했습니다`);
	if (stale) problems.push(`타임리스 .bin ${stale}개가 .zip 보다 오래됨(stale) — LUT 어긋남으로 스펙 임포트가 깨집니다`);
	if (!missing && !stale) notes.push(`타임리스 .bin ${checked}개 최신`);
}

// 3) 상류 버그 패치 — patch-pob.mjs 가 고치는 지점이 실제로 고쳐졌는지 본문으로 확인
const specFile = path.join(POB, "Classes", "PassiveSpec.lua");
if (fs.existsSync(specFile)) {
	const src = fs.readFileSync(specFile, "utf8");
	// 패치 전 원본은 nil 검사 없이 item 을 역참조한다. 패치본은 item 존재를 먼저 본다.
	const patched = /if\s+item\s+and\s+item\./.test(src) || /item\s*==\s*nil/.test(src);
	if (patched) notes.push("PoB nil 버그 패치 적용됨");
	else problems.push("PoB nil 버그 패치가 안 보임 — patch-pob.mjs 실패(일부 빌드가 빈 수치를 냅니다)");
}

for (const n of notes) console.log("  ok  " + n);
if (problems.length === 0) {
	console.log("엔진 점검 통과 — 시뮬레이터 사용 가능");
	process.exit(0);
}
console.error("");
for (const p of problems) console.error("  ✗ " + p);
console.error("\n조치: 네트워크를 확인하고 데이터 갱신을 한 번 더 실행하세요(소스 갱신·패치·.bin 추출이 이 순서로 다시 돕니다).");
process.exit(1);
