// 전체 파이프라인 일괄 실행 (웹 관리 메뉴/수동 공용 진입점)
// bootstrap(npm 의존성) → extract → transform(스킬젬) → parse-uniques(고유) → parse-items(일반)
// → parse-tree(트리) → icons(아이콘, ImageMagick 없으면 건너뜀) → PoB 소스 클론(엔진용, 없을 때만)
import { execSync } from "node:child_process";
import fs from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
import { WORK_DIR } from "./paths.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));

// 0) 최초 실행 부트스트랩: pathofexile-dat 의존성이 없으면 npm install
//    (PATH 의존 없이 현재 node 에 딸린 npm-cli.js 로 실행 — JVM 경유 실행 대비)
if (!fs.existsSync(path.join(here, "node_modules", "pathofexile-dat"))) {
	console.log("===== bootstrap: npm install (최초 1회) =====");
	const npmCli = path.join(path.dirname(process.execPath), "node_modules", "npm", "bin", "npm-cli.js");
	execSync(`"${process.execPath}" "${npmCli}" install --no-audit --no-fund`, { stdio: "inherit", cwd: here });
}

const steps = ["extract.mjs", "transform.mjs", "parse-uniques.mjs", "parse-items.mjs", "parse-tree.mjs", "icons.mjs"];

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
