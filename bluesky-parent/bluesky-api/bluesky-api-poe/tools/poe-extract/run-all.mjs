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

const steps = ["extract.mjs", "transform.mjs", "parse-uniques.mjs", "parse-items.mjs", "parse-mods.mjs", "parse-mods-full.mjs", "parse-essences.mjs", "parse-bench.mjs", "parse-eldritch.mjs", "parse-tree.mjs", "parse-atlas-tree.mjs", "parse-cluster-jewels.mjs", "parse-skill-weapons.mjs", "parse-tattoos.mjs",
	// parse-anoints 는 runExtractor 로 테이블을 재추출(기존 산출물 대체)하므로 테이블 소비 파서들 **뒤**에 둔다
	"parse-anoints.mjs", "essence-icons.mjs", "tattoo-icons.mjs", "currency-icons.mjs", "tree-sprites.mjs", "archive-trees.mjs", "tree-layers.mjs", "icons.mjs", "item-icons.mjs", "unique-icons.mjs", "timeless-bin.mjs", "ui-assets.mjs"];

for (const step of steps) {
	console.log(`\n===== ${step} =====`);
	// PATH 의존 없이 현재 node 바이너리로 실행 (JVM 경유 실행 대비)
	execSync(`"${process.execPath}" "${path.join(here, step)}"`, { stdio: "inherit", cwd: here });
}

// PoB 엔진 소스 (빌드 재계산/시뮬레이터용) — 없으면 클론, 실패해도 데이터 파이프라인은 성공 처리
const pobSrc = path.join(WORK_DIR, "pob-src");
if (!fs.existsSync(pobSrc)) {
	console.log("\n===== PoB 엔진 소스 클론 (최초 1회) =====");
	try {
		execSync(`git clone --depth 1 https://github.com/PathOfBuildingCommunity/PathOfBuilding.git "${pobSrc}"`, {
			stdio: "inherit",
		});
	} catch (e) {
		console.warn("PoB 소스 클론 실패 — 빌드 재계산/시뮬레이터는 비활성. 수동 셋업: tools/poe-pob/README.md");
	}
}

console.log("\n===== 완료 =====");
