// PoE2 제작 화폐 · 징조 그림(10-15 C351) → ~/.poe-gamedata/poe2/icons/currency/<slug 소문자>.png (게이트 /poe-data/poe2/icons/currency/…)
// craft2-mods.json 의 currencies · omens 슬러그 → BaseItemTypes → ItemVisualIdentity DDS. icons2.mjs 처럼 번들 로더로 받아(작업 폴더 tables 를
// 건드리지 않는다) ImageMagick 으로 48px.
// 사용법: node currency-icons2.mjs  (사전: parse-craft2.mjs)
import { execFile } from "node:child_process";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { findImageMagick } from "../poe-extract/paths.mjs";
import { DATA_DIR, WORK_DIR, loadTable, openLoader } from "./paths.mjs";

const OUT = path.join(DATA_DIR, "icons", "currency");
const DDS_DIR = path.join(WORK_DIR, "icons-dds");
fs.mkdirSync(DDS_DIR, { recursive: true });
fs.mkdirSync(OUT, { recursive: true });
const magickDir = findImageMagick();
if (!magickDir) {
	console.error("[poe2 currency icons] ImageMagick 이 없다 — 건너뛴다");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");
const craft = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "craft2-mods.json"), "utf8"));
const slugs = new Set([...(craft.currencies || []), ...(craft.omens || [])].map((x) => x.slug).filter(Boolean));
// 에뮬레이터 신성한 오브(수치 다시 굴리기) · 히네코라의 머리카락 버튼(10-16 C370) — 제작 화폐 표엔 없다
for (const s of ["CurrencyModValues", "CurrencyHinekorasLock", "CurrencyCorrupt"]) slugs.add(s); // CurrencyCorrupt = 바알 오브(C376)
const bases = loadTable("English", "BaseItemTypes");
const visuals = loadTable("English", "ItemVisualIdentity");
const ddsBySlug = new Map();
for (const b of bases) {
	const slug = (b?.Id || "").split("/").pop();
	if (!slugs.has(slug) || ddsBySlug.has(slug)) continue;
	const dds = b.ItemVisualIdentity != null ? visuals[b.ItemVisualIdentity]?.DDSFile : null;
	if (dds) ddsBySlug.set(slug, dds);
}
const loader = await openLoader();
let made = 0, reused = 0, missing = 0;
for (const [slug, dds] of ddsBySlug) {
	const dst = path.join(OUT, slug.toLowerCase() + ".png");
	if (fs.existsSync(dst)) {
		reused++;
		continue;
	}
	const file = path.join(DDS_DIR, crypto.createHash("sha1").update(dds).digest("hex").slice(0, 16) + ".dds");
	if (!fs.existsSync(file)) {
		const buf = (await loader.get(dds)) || (await loader.get(dds.toLowerCase()));
		if (!buf) {
			missing++;
			continue;
		}
		fs.writeFileSync(file, buf);
	}
	const okConv = await new Promise((r) => execFile(MAGICK, [file, "-resize", "48x48", "-strip", dst], (e) => r(!e)));
	if (okConv) made++;
	else missing++;
}
console.log(`[poe2 currency icons] 새로 ${made} · 있던 것 ${reused} · 없음 ${missing} (대상 ${slugs.size}, 그림 경로 ${ddsBySlug.size}) → ${OUT}`);
