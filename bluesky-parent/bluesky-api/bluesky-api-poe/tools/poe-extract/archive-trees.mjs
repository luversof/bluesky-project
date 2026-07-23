// 트리 데이터 버전별 아카이브: 현재 패치의 passive/atlas 트리 JSON + 스프라이트 시트를
// major.minor(예: 3.28) 폴더로 스냅샷해 이전 버전도 다시 볼 수 있게 한다.
//  - JSON:   ~/.poe-gamedata/trees/<ver>/{passive-tree,atlas-tree,tree-sprites-skill,tree-sprites-atlas}.json
//  - 시트:   ~/.poe-gamedata/icons/trees/<ver>/{skill,atlas}/  (매니페스트 file 경로도 여기로 재작성)
//  - 색인:   ~/.poe-gamedata/trees/index.json = [{ver, patch}] (게이트 버전 선택 드롭다운용)
// tree-layers(직업 일러스트)는 버전 간 사실상 불변이라 공용을 그대로 쓴다.
// 사용법: node archive-trees.mjs (parse-tree/parse-atlas-tree/tree-sprites 후)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, loadConfig } from "./paths.mjs";

const patch = loadConfig().patch;
if (!patch) {
	console.warn("config.json 에 patch 가 없어 archive-trees 단계를 건너뜁니다.");
	process.exit(0);
}
const ver = patch.split(".").slice(0, 2).join(".");

const TREES_DIR = path.join(DATA_DIR, "trees", ver);
const SHEET_SRC = path.join(DATA_DIR, "icons", "tree-sprites");
const SHEET_DST = path.join(DATA_DIR, "icons", "trees", ver);
fs.mkdirSync(TREES_DIR, { recursive: true });

// 1) 트리 JSON 스냅샷
let copied = 0;
for (const name of ["passive-tree.json", "atlas-tree.json"]) {
	const src = path.join(DATA_DIR, name);
	if (!fs.existsSync(src)) continue;
	fs.copyFileSync(src, path.join(TREES_DIR, name));
	copied++;
}

// 2) 시트 복사 + 매니페스트 file 경로 재작성 (tree-sprites/<kind>/ → trees/<ver>/<kind>/)
let sheets = 0;
for (const kind of ["skill", "atlas"]) {
	const manifestSrc = path.join(DATA_DIR, `tree-sprites-${kind}.json`);
	if (!fs.existsSync(manifestSrc)) continue;
	const dstDir = path.join(SHEET_DST, kind);
	fs.mkdirSync(dstDir, { recursive: true });
	const srcDir = path.join(SHEET_SRC, kind);
	if (fs.existsSync(srcDir)) {
		for (const f of fs.readdirSync(srcDir)) {
			fs.copyFileSync(path.join(srcDir, f), path.join(dstDir, f));
			sheets++;
		}
	}
	const manifest = JSON.parse(fs.readFileSync(manifestSrc, "utf8"));
	for (const entry of Object.values(manifest)) {
		if (entry.file) entry.file = entry.file.replace(`tree-sprites/${kind}/`, `trees/${ver}/${kind}/`);
	}
	fs.writeFileSync(path.join(TREES_DIR, `tree-sprites-${kind}.json`), JSON.stringify(manifest));
}

// 3) 버전 색인 갱신 — 폴더 스캔 + 이 버전의 정확한 patch 기록(내림차순)
const root = path.join(DATA_DIR, "trees");
const indexPath = path.join(root, "index.json");
let index = [];
try {
	index = JSON.parse(fs.readFileSync(indexPath, "utf8"));
} catch {
	index = [];
}
index = index.filter((e) => e.ver !== ver && fs.existsSync(path.join(root, e.ver)));
index.push({ ver, patch });
index.sort((a, b) => b.ver.localeCompare(a.ver, undefined, { numeric: true }));
fs.writeFileSync(indexPath, JSON.stringify(index, null, "\t"));

console.log(`archive-trees 완료: ${ver} (patch ${patch}) — JSON ${copied}개, 시트 ${sheets}개, 색인 ${index.length}버전`);
