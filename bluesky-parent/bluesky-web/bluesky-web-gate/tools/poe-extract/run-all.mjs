// 전체 파이프라인 일괄 실행 (웹 관리 메뉴/수동 공용 진입점)
// extract → transform(스킬젬) → parse-uniques(고유) → parse-tree(트리) → icons(아이콘)
import { execSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import path from "node:path";

const here = path.dirname(fileURLToPath(import.meta.url));
const steps = ["extract.mjs", "transform.mjs", "parse-uniques.mjs", "parse-items.mjs", "parse-tree.mjs", "icons.mjs"];

for (const step of steps) {
	console.log(`\n===== ${step} =====`);
	// PATH 의존 없이 현재 node 바이너리로 실행 (JVM 경유 실행 대비)
	execSync(`"${process.execPath}" "${path.join(here, step)}"`, { stdio: "inherit", cwd: here });
}
console.log("\n===== 완료 =====");
