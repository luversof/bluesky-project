// 베이스 아이템 아이콘 추출(젬 icons.mjs 패턴): BaseItemTypes → ItemVisualIdentity → DDS → PNG.
// 고유/일반 아이템 목록에서 아이콘으로 쓴다(고유 전용 아트는 추출 데이터에 없어 베이스 아트 사용).
// 출력: ~/.poe-gamedata/icons/items/<baseSlug>.png  → /poe-assets/items/ 로 서빙.
// 사용법: node item-icons.mjs  (사전: extract.mjs + parse-items.mjs 완료)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, loadTable, runExtractor } from "./paths.mjs";

const magickDir = findImageMagick();
if (!magickDir) {
	console.warn("ImageMagick 이 없어 아이템 아이콘 단계를 건너뜁니다. (winget install ImageMagick.ImageMagick)");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");

const ICON_DIR = path.join(DATA_DIR, "icons", "items");
const baseItemsFile = path.join(DATA_DIR, "base-items.json");
if (!fs.existsSync(baseItemsFile)) {
	console.warn("base-items.json 없음 — parse-items.mjs 먼저 실행 필요. 건너뜀");
	process.exit(0);
}

const bases = JSON.parse(fs.readFileSync(baseItemsFile, "utf8")).items;
const baseTable = loadTable("English", "BaseItemTypes");
const visual = loadTable("English", "ItemVisualIdentity");

// 베이스 이름(영문) → BaseItemTypes 행 (이름 매칭)
const byName = new Map();
for (const row of baseTable) if (row.Name) byName.set(row.Name.toLowerCase(), row);

// baseSlug → dds 경로
const ddsBySlug = new Map();
for (const b of bases) {
	const row = byName.get((b.name || "").toLowerCase());
	const visualRow = row != null && row.ItemVisualIdentity != null ? visual[row.ItemVisualIdentity] : null;
	if (visualRow && visualRow.DDSFile) ddsBySlug.set(b.slug, visualRow.DDSFile.toLowerCase());
}
console.log(`아이템 아이콘 대상: ${ddsBySlug.size} / ${bases.length}`);

const config = loadConfig();
config.files = [...(config.files || []), ...new Set(ddsBySlug.values())];
config.tables = [];
runExtractor(config);

fs.mkdirSync(ICON_DIR, { recursive: true });
let copied = 0;
const missing = [];
for (const [slug, dds] of ddsBySlug) {
	const escaped = dds.replace(/\//g, "@").replace(/\.dds$/, ".png");
	const source = path.join(FILES_DIR, escaped);
	if (fs.existsSync(source)) {
		// 베이스 아이콘은 단일 이미지(그리드 크기별 WxH) — 여백만 다듬어 그대로 저장.
		execFileSync(MAGICK, ["-background", "none", source, "-trim", "+repage", path.join(ICON_DIR, slug + ".png")]);
		copied++;
	} else {
		missing.push(slug);
	}
}
// 캐시버스터(젬 아이콘과 동일 version.txt 재사용 안 함 — 아이템 전용)
fs.writeFileSync(path.join(DATA_DIR, "icons", "items-version.txt"), String(Date.now()));
console.log(`복사 완료: ${copied}, 누락: ${missing.length}`);
