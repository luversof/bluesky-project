// PoE2 번들에서 텍스트 파일(스탯 설명 등)을 받는다 — config.json 의 files 목록. 작업물: ~/.poe-gamedata/poe2/work/files
// CLI 의 파일 추출은 PoE1 스프라이트 목록을 먼저 읽어서 PoE2 에서 실패할 수 있어 로더를 직접 쓴다.
// 사용법: node export-files.mjs [추가 경로 …]
import fs from "node:fs";
import path from "node:path";
import { FILES_DIR, loadConfig, openLoader } from "./paths.mjs";

const wanted = [...(loadConfig().files || []), ...process.argv.slice(2)];
fs.mkdirSync(FILES_DIR, { recursive: true });
const loader = await openLoader();
let ok = 0;
const missing = [];
for (const p of wanted) {
	const buf = await loader.get(p);
	if (!buf) {
		missing.push(p);
		continue;
	}
	fs.writeFileSync(path.join(FILES_DIR, p.replace(/\//g, "@")), buf);
	ok++;
}
console.log(`[poe2 files] ${ok}/${wanted.length} 받음` + (missing.length ? ` · 없음: ${missing.join(", ")}` : ""));
if (missing.length) process.exitCode = 1;
