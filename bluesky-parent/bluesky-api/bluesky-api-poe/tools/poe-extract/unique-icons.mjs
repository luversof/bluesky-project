// 고유 아이템 **전용 아이콘** 추출 — 지금은 베이스 아이콘으로 대체돼 "고유 주얼이 일반 주얼로 보이는" 문제가 있다.
// 연결 고리: UniqueStashLayout(WordsKey → ItemVisualIdentityKey) → ItemVisualIdentity.DDSFile → 번들에서 PNG.
// 사용법: node unique-icons.mjs  (ImageMagick 필요 — 없으면 이 단계만 건너뜀)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, loadTable, runExtractor } from "./paths.mjs";

if (!findImageMagick()) {
	console.warn("ImageMagick 이 없어 고유 아이콘 단계를 건너뜁니다.");
	process.exit(0);
}
const OUT_DIR = path.join(DATA_DIR, "icons", "uniques");
const uniquesFile = path.join(DATA_DIR, "unique-items.json");
if (!fs.existsSync(uniquesFile)) {
	console.warn("unique-items.json 없음 — parse-uniques.mjs 를 먼저 실행하세요");
	process.exit(0);
}
const raw = JSON.parse(fs.readFileSync(uniquesFile, "utf8"));
const items = Array.isArray(raw) ? raw : raw.items || [];

const words = loadTable("English", "Words");
const layout = loadTable("English", "UniqueStashLayout");
const visual = loadTable("English", "ItemVisualIdentity");
const ddsByName = new Map();
for (const row of layout) {
	const word = words[row.WordsKey];
	const art = visual[row.ItemVisualIdentityKey];
	if (word?.Text && art?.DDSFile) {
		ddsByName.set(word.Text, art.DDSFile.toLowerCase());
	}
}

// slug → dds (우리가 가진 고유만)
const ddsBySlug = new Map();
for (const item of items) {
	const dds = ddsByName.get(item.name);
	if (dds) ddsBySlug.set(item.slug, dds);
}
console.log(`고유 ${items.length}개 중 아이콘 경로 확보 ${ddsBySlug.size}개 (${Math.round((ddsBySlug.size / items.length) * 100)}%)`);
if (!ddsBySlug.size) {
	process.exit(0);
}

// ⚠ 추출기는 tables/files 목록을 **그대로 재생성**한다 — 기본 config 의 것을 반드시 합쳐서 넘긴다.
// (빼먹으면 이미 추출해 둔 테이블·스탯 설명 파일이 삭제돼 다음 단계가 조용히 망가진다)
const baseConfig = loadConfig();
const ddsFiles = [...new Set(ddsBySlug.values())];
runExtractor({ ...baseConfig, tables: baseConfig.tables, files: [...(baseConfig.files || []), ...ddsFiles] });

fs.mkdirSync(OUT_DIR, { recursive: true });
let done = 0;
for (const [slug, dds] of ddsBySlug) {
	const extracted = path.join(FILES_DIR, dds.replace(/\//g, "@").replace(/\.dds$/, ".png"));
	if (!fs.existsSync(extracted)) continue;
	fs.copyFileSync(extracted, path.join(OUT_DIR, `${slug}.png`));
	done++;
}
console.log(`고유 아이콘 ${done}/${ddsBySlug.size}개 → ${OUT_DIR}`);
