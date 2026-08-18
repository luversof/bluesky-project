// 에센스 아이콘 추출 → icons/essences/<slug>.png (parse-anoints 의 오일 아이콘 패턴).
// ⚠ runExtractor(files) 는 테이블 산출물을 대체하므로 run-all 에선 테이블 소비 파서들 **뒤**(parse-anoints 뒤)에 둔다.
// 사용법: node essence-icons.mjs  (사전: essences.json 존재, ImageMagick)
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, loadTable, runExtractor } from "./paths.mjs";

const essFile = path.join(DATA_DIR, "essences.json");
if (!fs.existsSync(essFile)) {
	console.warn("essences.json 없음 — parse-essences.mjs 먼저 실행");
	process.exit(0);
}
const magickDir = findImageMagick();
if (!magickDir) {
	console.warn("ImageMagick 없음 — 에센스 아이콘 생략(이름만 표시됨)");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");

// essences.json 이 참조하는 에센스 이름 집합 → BaseItemTypes/ItemVisualIdentity 조인으로 DDS 수집
const essences = JSON.parse(fs.readFileSync(essFile, "utf8"));
const wantedNames = new Set();
for (const list of Object.values(essences.classes)) for (const e of list) wantedNames.add(e.name);

const baseEn = loadTable("English", "BaseItemTypes");
const visual = loadTable("English", "ItemVisualIdentity");
const ddsBySlug = new Map();
for (const row of baseEn) {
	if (!row?.Name || !wantedNames.has(row.Name)) continue;
	const slug = (row.Id || "").split("/").pop().toLowerCase();
	const visualRow = row.ItemVisualIdentity != null ? visual[row.ItemVisualIdentity] : null;
	if (slug && visualRow?.DDSFile) ddsBySlug.set(slug, visualRow.DDSFile.toLowerCase());
}
console.log(`에센스 ${wantedNames.size}종 중 DDS 매칭 ${ddsBySlug.size}건`);

const dl = loadConfig();
dl.tables = [];
dl.files = [...new Set(ddsBySlug.values())];
// 아이콘 DDS 만 받는 추출(테이블 없음) — tables/ 를 갈아엎으므로 run-all 에서 테이블 소비 파서 뒤에 있다
runExtractor(dl, { partial: true });

const iconDir = path.join(DATA_DIR, "icons", "essences");
fs.mkdirSync(iconDir, { recursive: true });
let done = 0;
for (const [slug, dds] of ddsBySlug) {
	const sheet = path.join(FILES_DIR, dds.replace(/\//g, "@").replace(/\.dds$/, ".png"));
	if (!fs.existsSync(sheet)) continue;
	execFileSync(MAGICK, [sheet, "-resize", "78x78", path.join(iconDir, slug + ".png")]);
	done++;
}
console.log(`에센스 아이콘: ${done}/${ddsBySlug.size} → ${iconDir}`);
