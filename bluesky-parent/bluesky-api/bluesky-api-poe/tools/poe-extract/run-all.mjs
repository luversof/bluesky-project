// 전체 파이프라인 일괄 실행 (웹 관리 메뉴/수동 공용 진입점)
// bootstrap(npm 의존성) → extract → transform(스킬젬) → parse-uniques(고유) → parse-items(일반)
// → parse-tree(트리) → icons(아이콘, ImageMagick 없으면 건너뜀) → PoB 소스 클론(엔진용, 없을 때만)
import { execSync } from "node:child_process";
import fs from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
import { WORK_DIR } from "./paths.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));

// 0) 부트스트랩: 데이터 추출 라이브러리 pathofexile-dat 를 **매 실행 latest 로** 맞춘다.
//    목표 = 사용자는 '갱신 실행' 버튼만 누르면 끝(다른 절차 신경 X). 새 패치가 major 를 요구해도 자동으로 최신을 받는다.
//    ⚠ 예전 문제: (1) "node_modules 없을 때만 install" 이라 다른 PC/오래된 node_modules 면 stale 파서로 extract 실패,
//       (2) 이후 "npm update" 로 고쳤으나 ^15 캐럿이라 major(16.x) 를 못 넘어 새 시즌엔 수동 package.json bump 가 필요했고
//       그 절차 안내가 없어 사용자는 에러만 보게 됐다. → pathofexile-dat 만 콕 집어 @latest 로 설치(major 포함, package.json/lock 자동 갱신).
//    - install pathofexile-dat@latest 는 없으면 새로 받고 있으면 최신으로 올린다(한 방에 처리, 유일 직속 의존이라 전체 트리 커버).
//    - 오프라인/레지스트리 불가: 이미 받아둔 게 있으면 그걸로 계속(소프트 실패), 아예 없으면 진행 불가라 하드 실패.
//    (PATH 의존 없이 현재 node 에 딸린 npm-cli.js 로 실행 — JVM 경유 실행 대비)
const npmCli = path.join(path.dirname(process.execPath), "node_modules", "npm", "bin", "npm-cli.js");
const npm = (args) => execSync(`"${process.execPath}" "${npmCli}" ${args}`, { stdio: "inherit", cwd: here });
const havePod = fs.existsSync(path.join(here, "node_modules", "pathofexile-dat"));
try {
	console.log("===== bootstrap: npm install pathofexile-dat@latest (항상 최신) =====");
	npm("install pathofexile-dat@latest --no-audit --no-fund");
} catch (e) {
	if (!havePod) {
		throw e; // 처음이라 설치본이 없는데 다운로드도 실패 → 추출 불가
	}
	console.warn("pathofexile-dat 최신 설치 실패 — 기존 설치본으로 계속(오프라인/레지스트리 불가?):", e.message);
}

const steps = ["extract.mjs", "transform.mjs", "parse-uniques.mjs", "parse-items.mjs", "parse-mods.mjs", "parse-mods-full.mjs", "parse-map-mods.mjs", "parse-essences.mjs", "parse-bench.mjs", "parse-eldritch.mjs", "parse-foulborn.mjs", "parse-tree.mjs", "parse-atlas-tree.mjs", "parse-cluster-jewels.mjs", "parse-skill-weapons.mjs", "parse-tattoos.mjs",
	// parse-anoints 는 runExtractor 로 테이블을 재추출(기존 산출물 대체)하므로 테이블 소비 파서들 **뒤**에 둔다
	"parse-anoints.mjs", "essence-icons.mjs", "tattoo-icons.mjs", "currency-icons.mjs", "tree-sprites.mjs", "archive-trees.mjs", "tree-layers.mjs", "icons.mjs", "item-icons.mjs", "unique-icons.mjs", "ui-assets.mjs"];
// ⚠ timeless-bin.mjs 는 여기 두면 안 된다 — PoB 소스보다 **먼저** 돌아 갱신된 .zip 을 못 보고 옛 .bin 을 남긴다
//    (= 스펙 임포트가 조용히 깨져 사이온 빈 빌드 수치). 소스 갱신·패치 뒤로 옮겼다.

for (const step of steps) {
	console.log(`\n===== ${step} =====`);
	// PATH 의존 없이 현재 node 바이너리로 실행 (JVM 경유 실행 대비)
	execSync(`"${process.execPath}" "${path.join(here, step)}"`, { stdio: "inherit", cwd: here });
}

// 거래소 스탯 사전 (결과 레어 → 거래소 검색 링크용) — 네트워크 단계라 비치명 처리.
console.log("\n===== trade-stats.mjs (거래소 스탯 사전, 비치명) =====");
try {
	execSync(`"${process.execPath}" "${path.join(here, "trade-stats.mjs")}"`, { stdio: "inherit", cwd: here });
} catch (e) {
	console.warn("거래소 스탯 사전 갱신 실패 — 기존 파일로 계속:", e.message);
}

// poe.ninja 실빌드 시드 (최적화기 balanced 목표치의 아키타입 근거) — **네트워크 단계**라 비치명 처리.
//   인자 없이 호출하면 현재 빌드 리그를 자동 감지한다. 사이트 불가/리그 변경 시에도 데이터 파이프라인은 계속.
console.log("\n===== fetch-ninja-builds.mjs (poe.ninja 시드, 비치명) =====");
try {
	execSync(`"${process.execPath}" "${path.join(here, "fetch-ninja-builds.mjs")}"`, { stdio: "inherit", cwd: here });
} catch (e) {
	console.warn("poe.ninja 시드 갱신 실패 — 기존 시드/정적 floor 로 계속(오프라인/사이트 변경?):", e.message);
}

// PoB 엔진 소스 (빌드 재계산/시뮬레이터용) — 갱신 실행 한 번으로 **항상 쓸 수 있는 상태**가 돼야 한다.
//   사용자가 따로 알아야 하는 수동 절차가 남으면 그건 설계 실패다(타 PC 사고의 교훈).
const pobSrc = path.join(WORK_DIR, "pob-src");
const POB_REPO = "https://github.com/PathOfBuildingCommunity/PathOfBuilding.git";
const clonePob = (why) => {
	console.log(`\n===== PoB 엔진 소스 클론 (${why}) =====`);
	try {
		execSync(`git clone --depth 1 ${POB_REPO} "${pobSrc}"`, { stdio: "inherit" });
	} catch (e) {
		console.warn("PoB 소스 클론 실패 — 빌드 재계산/시뮬레이터는 비활성. 네트워크 확인 후 데이터 갱신을 다시 실행하세요.");
	}
};

if (!fs.existsSync(pobSrc)) {
	clonePob("최초 1회");
} else if (!fs.existsSync(path.join(pobSrc, ".git"))) {
	// 클론이 중간에 끊겼거나 누가 폴더만 복사해 둔 경우 — git 명령이 전부 실패해 갱신이 영원히 안 된다.
	// 여기서 조용히 넘어가면 "갱신했는데 왜 옛 데이터냐"가 되므로 **버리고 다시 받는다**(파생 캐시라 안전).
	console.log("\n===== PoB 엔진 소스 재설치 (git 저장소가 아님) =====");
	fs.rmSync(pobSrc, { recursive: true, force: true });
	clonePob("손상 복구");
} else {
	// ⚠ 예전엔 "없을 때만 클론"이라 **한 번 받은 소스는 영원히 그대로**였다. 새 리그로 트리 버전이 올라가면
	//    옛 소스엔 그 TreeData 가 없어 스펙 임포트가 기본 클래스로 떨어지고, 시뮬이
	//    "스펙 임포트 실패(클래스 N → 0)" 로 죽는다(타 PC 실사고). 갱신 한 번으로 끝나야 하므로 여기서 맞춘다.
	//    로컬 수정분(patch-pob)은 바로 아래 단계가 다시 입힌다.
	console.log("\n===== PoB 엔진 소스 갱신 =====");
	try {
		execSync(`git -C "${pobSrc}" fetch --depth 1 origin HEAD`, { stdio: "inherit" });
		execSync(`git -C "${pobSrc}" reset --hard FETCH_HEAD`, { stdio: "inherit" });
	} catch (e) {
		console.warn("PoB 소스 갱신 실패 — 기존 소스로 계속(오프라인?):", e.message);
	}
}

// PoB 소스 필수 패치(멱등) — 재클론/갱신된 소스에도 자동 적용돼야 한다.
//   안 입히면 주얄 소켓 nil 버그로 일부 실빌드 재계산이 조용히 빈 빌드 수치를 낸다.
console.log("\n===== patch-pob.mjs (PoB 소스 패치) =====");
try {
	execSync(`"${process.execPath}" "${path.join(here, "patch-pob.mjs")}"`, { stdio: "inherit", cwd: here });
} catch (e) {
	console.warn("PoB 소스 패치 실패 — 계속:", e.message);
}

// 타임리스 주얼 .bin — **소스 갱신·패치 뒤에** 푼다(순서가 뒤바뀌면 옛 .bin 이 남는다).
console.log("\n===== timeless-bin.mjs (타임리스 .bin 추출) =====");
try {
	execSync(`"${process.execPath}" "${path.join(here, "timeless-bin.mjs")}"`, { stdio: "inherit", cwd: here });
} catch (e) {
	console.warn("타임리스 .bin 추출 실패 — 계속:", e.message);
}

// 엔진 자가 점검 — 갱신이 끝난 시점에 "시뮬이 실제로 돌 수 있는 상태인가"를 확인한다.
//   여기서 안 잡으면 사용자가 나중에 시뮬을 돌리다 "스펙 임포트 실패"로 처음 알게 된다.
console.log("\n===== verify-engine.mjs (엔진 점검) =====");
try {
	execSync(`"${process.execPath}" "${path.join(here, "verify-engine.mjs")}"`, { stdio: "inherit", cwd: here });
} catch (e) {
	// ⚠ 여기서 exitCode 를 1 로 두면 안 된다 — 앱(PoeExtractService)은 exit≠0 을 "파이프라인 실패"로 보고
	//    **재로드 체인을 통째로 건너뛴다**. 그러면 방금 추출한 새 데이터가 버려져 엔진 문제 하나로 갱신 전체가 헛돈다.
	//    데이터 추출 성공과 엔진 건강은 별개라, 마커 한 줄로 알리고 판단은 앱에 맡긴다.
	console.error("\n⚠ 엔진 점검 실패 — 데이터는 갱신됐지만 시뮬레이터가 동작하지 않을 수 있습니다. 위 진단을 확인하세요.");
	console.error("@@ENGINE_UNHEALTHY@@");
}

// 아키타입 엔진 벤치 (belowMeta 판정의 **지표 정합 기준값**) — 대표 실빌드를 우리 엔진으로 재계산해
//   ninja-engine-bench.json 생성. 이게 없으면 판정이 poe.ninja 표기 지표(gross)로 폴백해 같은 빌드도
//   "메타 하회"로 뜬다(다른 PC 실사고). 갱신은 api-poe 가 이 파이프라인을 띄우므로 40135 는 살아 있다.
//   **네트워크(ninja 레이트리밋)·엔진 의존이라 비치명** — 실패해도 기존 벤치/폴백으로 계속.
console.log("\n===== calibrate-archetypes.mjs (엔진 벤치, 비치명) =====");
try {
	execSync(`"${process.execPath}" "${path.join(here, "calibrate-archetypes.mjs")}"`, { stdio: "inherit", cwd: here });
} catch (e) {
	console.warn("엔진 벤치 캘리브레이션 실패 — 기존 벤치/ninja 표기 폴백으로 계속(api 미가동·레이트리밋?):", e.message);
}

console.log("\n===== 완료 =====");
