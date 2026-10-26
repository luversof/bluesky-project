// PoE2 패시브 트리 노드 툴팁 머리 띠 아트(10-23 Q60) — PoE2 는 띠 모양이 PoE1 과 다르다(특출 캡이 아치형 등).
//   art/uiimages1.txt(atlas 좌표표)에서 4K 항목을 찾아 DDS 를 받아 crop → ~/.poe-gamedata/poe2/ui-assets/psheader*.png
//   (화면 /poe-data/poe2/ui-assets/…, CSS .poe2-psh). PoE2 에는 주얼 소켓 띠가 없어 주얼은 PoE1 아트 그대로.
// 사용법: node ui-assets2.mjs  (ImageMagick 필요 — 없으면 건너뜀)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { findImageMagick } from "../poe-extract/paths.mjs";
import { DATA_DIR, openLoader } from "./paths.mjs";

const magickDir = findImageMagick();
if (!magickDir) {
	console.warn("ImageMagick 이 없어 ui-assets2 단계를 건너뜁니다.");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");
const OUT_DIR = path.join(DATA_DIR, "ui-assets");
const PREFIX = "Art/2DArt/UIImages/InGame/4K/";

const WANTED = new Map();
for (const type of ["Normal", "Notable", "Keystone", "Ascendancy"]) {
	for (const side of ["Left", "Middle", "Right"]) WANTED.set(`${type}PassiveHeader${side}`, `psheader${type.toLowerCase()}${side.toLowerCase()}`);
}

const loader = await openLoader();
const index = await loader.get("art/uiimages1.txt");
if (!index) throw new Error("art/uiimages1.txt 없음");
const entries = [];
for (const line of Buffer.from(index).toString("utf16le").split(/\r?\n/)) {
	const m = line.match(/"([^"]+)"\s+"([^"]+)"\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)/);
	if (!m || !m[1].startsWith(PREFIX)) continue;
	const out = WANTED.get(m[1].slice(PREFIX.length));
	if (out) entries.push({ out, tex: m[2], x: +m[3], y: +m[4], w: +m[5] - +m[3], h: +m[6] - +m[4] });
}
const missing = [...WANTED.values()].filter((o) => !entries.some((e) => e.out === o));
console.log(`atlas 매칭: ${entries.length}/${WANTED.size}${missing.length ? " (누락: " + missing.join(", ") + ")" : ""}`);

fs.mkdirSync(OUT_DIR, { recursive: true });
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "poe2-ui-"));
let done = 0;
try {
	for (const e of entries) {
		const buf = await loader.get(e.tex);
		if (!buf) {
			console.warn(`텍스처 없음: ${e.tex} (${e.out})`);
			continue;
		}
		const dds = path.join(tmp, e.out + ".dds");
		fs.writeFileSync(dds, buf);
		execFileSync(MAGICK, [dds, "-crop", `${e.w}x${e.h}+${e.x}+${e.y}`, "+repage", "-strip", path.join(OUT_DIR, e.out + ".png")]);
		done++;
	}
} finally {
	fs.rmSync(tmp, { recursive: true, force: true });
}
console.log(`ui-assets2 완료: ${done}개 → ${OUT_DIR}`);
if (done !== WANTED.size) process.exitCode = 1;
