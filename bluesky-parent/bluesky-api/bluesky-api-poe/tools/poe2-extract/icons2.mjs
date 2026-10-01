// PoE2 아이템 그림 → ~/.poe-gamedata/poe2/icons/{bases,uniques,gems,augments}/<slug>.png (게이트가 /poe-data/poe2/icons/… 로 서빙)
// 그리고 각 데이터 JSON 항목에 image("bases/<slug>.png") 를 붙인다 — 그림이 없는 항목은 image 가 없어 화면이 <img> 를 그리지 않는다.
// PoE2 아이템 DDS 는 무압축 RGBA(DXGI 28)라 ImageMagick 으로 바로 바뀐다. 절반 크기로 줄여 저장(목록 썸네일 · 툴팁 겸용).
import { execFile } from "node:child_process";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { findImageMagick } from "../poe-extract/paths.mjs";
import { DATA_DIR, WORK_DIR, loadTable, openLoader } from "./paths.mjs";

const ICON_DIR = path.join(DATA_DIR, "icons");
const DDS_DIR = path.join(WORK_DIR, "icons-dds");
fs.mkdirSync(DDS_DIR, { recursive: true });

const magickDir = findImageMagick();
if (!magickDir) {
	console.error("[poe2 icons] ImageMagick 이 없다 — 그림 단계를 건너뛴다(데이터는 그대로 쓸 수 있다)");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");

const readJson = (f) => JSON.parse(fs.readFileSync(path.join(DATA_DIR, f), "utf8"));
const writeJson = (f, d) => fs.writeFileSync(path.join(DATA_DIR, f), JSON.stringify(d));

// ── 게임 테이블: 베이스 Id/이름 → 그림, 고유 이름 → 그림 ──
const bases = loadTable("English", "BaseItemTypes");
const visuals = loadTable("English", "ItemVisualIdentity");
const words = loadTable("English", "Words");
const stash = loadTable("English", "UniqueStashLayout");
const ddsOf = (vi) => (vi != null ? visuals[vi]?.DDSFile || null : null);
const byBaseId = new Map(bases.map((b) => [b.Id, ddsOf(b.ItemVisualIdentity)]));
const byBaseName = new Map();
for (const b of bases) if (!byBaseName.has(b.Name)) byBaseName.set(b.Name, ddsOf(b.ItemVisualIdentity));
const byUnique = new Map();
for (const s of stash) {
	const w = words[s.WordsKey];
	const dds = ddsOf(s.ItemVisualIdentityKey);
	// 이름은 Text 와 Text2 둘 다로 건다 — 고유 이름이 Text2(표시 이름)인 경우가 있다: Brynabas/Byrnabas, Husk of Dreams/Reverie,
	//   Sekhema's Resolve Cold/Eshtera's Path 등 7개가 Text 로만 걸었을 때 그림 없이 빠졌다(10-01, PoE1 unique-icons 와 같은 원인)
	for (const name of new Set([w?.Text, w?.Text2].filter(Boolean))) {
		// 대체 아트(IsAlternateArt)보다 기본 아트를 먼저
		if (dds && (!byUnique.has(name) || !s.IsAlternateArt)) byUnique.set(name, dds);
	}
}

// ── 할 일 모으기 ──
const jobs = []; // { dir, slug, dds, set(image) }
const baseData = readJson("base-items.json");
for (const b of baseData.items) jobs.push({ dir: "bases", slug: b.slug, dds: b.icon || byBaseName.get(b.name), item: b });
const gemData = readJson("gems.json");
// 젬은 스킬 아이콘(스킬 막대와 같은 그림, 64px BC1)을 먼저 — 젬 아이템 그림은 상당수가 같은 회색 구슬이라 구분이 안 된다
for (const g of gemData.gems) jobs.push({ dir: "gems", slug: g.slug, dds: g.icon || byBaseId.get(g.id), fallback: byBaseId.get(g.id), keepSize: !!g.icon, item: g });
const augData = readJson("augments.json");
for (const a of augData.items) jobs.push({ dir: "augments", slug: a.slug, dds: a.icon, item: a });
const uniqueData = readJson("uniques.json");
for (const u of uniqueData.items) jobs.push({ dir: "uniques", slug: u.slug, dds: byUnique.get(u.name) || null, item: u });

// ── DDS 받기(같은 그림은 한 번) → PNG 변환(동시 8개) ──
const loader = await openLoader();
const ddsCache = new Map(); // 게임 경로 → 로컬 dds 파일(없으면 null)
async function localDds(p) {
	if (!p) return null;
	if (ddsCache.has(p)) return ddsCache.get(p);
	const file = path.join(DDS_DIR, crypto.createHash("sha1").update(p).digest("hex").slice(0, 16) + ".dds");
	let ok = fs.existsSync(file);
	if (!ok) {
		const buf = (await loader.get(p)) || (await loader.get(p.toLowerCase()));
		if (buf) {
			fs.writeFileSync(file, buf);
			ok = true;
		}
	}
	ddsCache.set(p, ok ? file : null);
	return ok ? file : null;
}
const convert = (src, dst, keepSize) =>
	new Promise((resolve) => {
		fs.mkdirSync(path.dirname(dst), { recursive: true });
		execFile(MAGICK, keepSize ? [src, "-strip", dst] : [src, "-resize", "50%", "-strip", dst], (err) => resolve(!err));
	});

let made = 0, reused = 0, missing = 0, failed = 0;
const queue = [...jobs];
async function worker() {
	while (queue.length) {
		const job = queue.shift();
		const dst = path.join(ICON_DIR, job.dir, job.slug + ".png");
		let src = await localDds(job.dds);
		if (!src && job.fallback && job.fallback !== job.dds) {
			src = await localDds(job.fallback);
			job.keepSize = false;
		}
		if (!src) {
			missing++;
			delete job.item.image;
			continue;
		}
		if (fs.existsSync(dst) && fs.statSync(dst).mtimeMs >= fs.statSync(src).mtimeMs) {
			reused++;
			job.item.image = job.dir + "/" + job.slug + ".png";
			continue;
		}
		if (await convert(src, dst, job.keepSize)) {
			made++;
			job.item.image = job.dir + "/" + job.slug + ".png";
		} else {
			failed++;
			delete job.item.image;
		}
	}
}
await Promise.all(Array.from({ length: 8 }, worker));
writeJson("base-items.json", baseData);
writeJson("gems.json", gemData);
writeJson("augments.json", augData);
writeJson("uniques.json", uniqueData);
const by = jobs.reduce((m, j) => ((m[j.dir] = m[j.dir] || { ok: 0, none: 0 }), j.item.image ? m[j.dir].ok++ : m[j.dir].none++, m), {});
console.log(`[poe2 icons] 새로 ${made} · 재사용 ${reused} · 그림 없음 ${missing} · 변환 실패 ${failed}`, JSON.stringify(by));
