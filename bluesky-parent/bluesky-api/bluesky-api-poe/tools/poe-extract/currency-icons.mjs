// 화폐 아이콘 추출 → icons/currency/<slug>.png (bench.json 비용이 참조 — essence-icons 패턴).
// ⚠ runExtractor(files) 는 테이블 산출물을 대체하므로 run-all 에선 테이블 소비 파서들 뒤에 둔다.
// 사용법: node currency-icons.mjs  (사전: bench.json, ImageMagick)
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, loadTable, runExtractor } from "./paths.mjs";

const benchFile = path.join(DATA_DIR, "bench.json");
if (!fs.existsSync(benchFile)) {
	console.warn("bench.json 없음 — parse-bench.mjs 먼저 실행");
	process.exit(0);
}
const magickDir = findImageMagick();
if (!magickDir) {
	console.warn("ImageMagick 없음 — 화폐 아이콘 생략(이름만 표시됨)");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");
const ICON_DIR = path.join(DATA_DIR, "icons", "currency");

// bench.json 비용에 등장하는 화폐 이름 집합
const bench = JSON.parse(fs.readFileSync(benchFile, "utf8"));
const wantedNames = new Set();
for (const list of Object.values(bench.classes)) {
	for (const e of list) for (const c of e.cost || []) wantedNames.add(c.name);
}

const baseEn = loadTable("English", "BaseItemTypes");
const visual = loadTable("English", "ItemVisualIdentity");
const ddsBySlug = new Map();
for (const row of baseEn) {
	if (!row?.Name || !wantedNames.has(row.Name)) continue;
	const slug = (row.Id || "").split("/").pop().toLowerCase();
	if (!slug || fs.existsSync(path.join(ICON_DIR, slug + ".png"))) continue;
	const visualRow = row.ItemVisualIdentity != null ? visual[row.ItemVisualIdentity] : null;
	if (visualRow?.DDSFile) ddsBySlug.set(slug, visualRow.DDSFile.toLowerCase());
}
console.log(`화폐 ${wantedNames.size}종 중 신규 추출 대상 ${ddsBySlug.size}건`);
if (!ddsBySlug.size) process.exit(0);

const dl = loadConfig();
dl.tables = [];
dl.files = [...new Set(ddsBySlug.values())];
// 아이콘 DDS 만 받는 추출(테이블 없음) — tables/ 를 갈아엎으므로 run-all 에서 테이블 소비 파서 뒤에 있다
runExtractor(dl, { partial: true });

fs.mkdirSync(ICON_DIR, { recursive: true });
let done = 0;
for (const [slug, dds] of ddsBySlug) {
	const sheet = path.join(FILES_DIR, dds.replace(/\//g, "@").replace(/\.dds$/, ".png"));
	if (!fs.existsSync(sheet)) continue;
	execFileSync(MAGICK, [sheet, "-resize", "48x48", path.join(ICON_DIR, slug + ".png")]);
	done++;
}
console.log(`화폐 아이콘: ${done}/${ddsBySlug.size} → ${ICON_DIR}`);
