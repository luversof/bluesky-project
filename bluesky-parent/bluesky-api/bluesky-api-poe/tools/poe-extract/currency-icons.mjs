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
// 제작 에뮬레이터 화폐 버튼(10-15 C335) — 제작대 비용에 안 나오는 소멸 · 정제 등도
// 화석 · 공명기(10-15 C338) — craft-currencies.json 이 있으면 그 이름들
try {
	const cc = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "craft-currencies.json"), "utf8"));
	for (const f of [...(cc.fossils || []), ...(cc.resonators || [])]) wantedNames.add(f.name);
	// 기폭제 13종(C385)
	for (const c of cc.currencies || []) if (c.catalyst || /^(reroll_single_attribute_modifier|add_deepwater_hazard_belt_mod|add_pantheon_aspect|add_mod_and_corrupt_rare_abyss_jewel|reroll_rare_infamous|add_eldritch_implicit_amulet)$/.test(c.action)) wantedNames.add(c.name); // + 두카트(C391)
} catch { /* 없으면 건너뜀 */ }
for (const n of ["Orb of Transmutation", "Orb of Alteration", "Orb of Augmentation", "Regal Orb", "Orb of Alchemy", "Chaos Orb", "Exalted Orb", "Orb of Annulment", "Orb of Scouring", "Fracturing Orb", "Awakener's Orb", "Crusader's Exalted Orb", "Hunter's Exalted Orb", "Redeemer's Exalted Orb", "Warlord's Exalted Orb", "Shaper's Exalted Orb", "Elder's Exalted Orb", "Veiled Chaos Orb", "Veiled Exalted Orb", "Eldritch Chaos Orb", "Eldritch Exalted Orb", "Eldritch Orb of Annulment", "Orb of Dominance", "Orb of Conflict", "Foulborn Orb of Augmentation", "Foulborn Regal Orb", "Foulborn Exalted Orb", "Vaal Orb", "Tainted Chaos Orb", "Tainted Exalted Orb", "Tainted Divine Teardrop", "Divine Orb", "Blessed Orb", ...["Lesser", "Greater", "Grand", "Exceptional"].flatMap((t) => [t + " Eldritch Ember", t + " Eldritch Ichor"])]) wantedNames.add(n);

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
