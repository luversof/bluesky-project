// PoE2 패시브 트리 노드 아이콘 → ~/.poe-gamedata/poe2/tree-sprites/{skills-64,skills-128,…}.webp + index.json
// 원본: PoB-PoE2 TreeData/0_5/skills_*_BC1.dds.zst — zstd 로 감싼 DDS **텍스처 배열**(DXGI 71 = BC1, 레이어마다 밉 체인).
//   트리 JSON 의 ddsCoords[파일][아이콘 경로] = 1-base 레이어 번호. BC1 은 4×4 블록당 8바이트(RGB565 두 색 + 2비트 색인)라 직접 푼다.
// 시트: 레이어를 16열 격자로 붙인 RGBA → ImageMagick 으로 WebP(품질 88 — PNG 는 128px 시트가 9MB 라 화면용으로 무겁다, WebP 2.1MB). index.json: { "<아이콘 경로>": { s: 시트, x, y, w, h } } (큰 것 우선).
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";
import { findImageMagick } from "../poe-extract/paths.mjs";
import { pobFile } from "./common2.mjs";
import { DATA_DIR, WORK_DIR } from "./paths.mjs";

const TREE_VER = "0_5";
const OUT = path.join(DATA_DIR, "tree-sprites");
fs.mkdirSync(OUT, { recursive: true });
const magickDir = findImageMagick();
if (!magickDir) {
	console.error("[poe2 tree sprites] ImageMagick 이 없다 — 건너뜀(트리는 아이콘 없이 그려진다)");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");

const tree = JSON.parse(await pobFile(`src/TreeData/${TREE_VER}/tree.json`));
const coords = tree.ddsCoords;

/** zst 원본 받기(캐시) */
async function zstFile(name) {
	const local = path.join(WORK_DIR, "pob", `tree-${TREE_VER}-${name}`);
	if (!fs.existsSync(local)) {
		const res = await fetch(`https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/dev/src/TreeData/${TREE_VER}/${name}`);
		if (!res.ok) throw new Error(`${name}: HTTP ${res.status}`);
		fs.writeFileSync(local, Buffer.from(await res.arrayBuffer()));
	}
	return zlib.zstdDecompressSync(fs.readFileSync(local));
}

/** RGB565 → [r,g,b] */
const c565 = (v) => [((v >> 11) & 31) * 255 / 31, ((v >> 5) & 63) * 255 / 63, (v & 31) * 255 / 31];

/** BC1 한 장(w×h) → RGBA 버퍼 */
function decodeBC1(buf, off, w, h) {
	const out = Buffer.alloc(w * h * 4);
	const bw = Math.max(1, Math.ceil(w / 4)), bh = Math.max(1, Math.ceil(h / 4));
	for (let by = 0; by < bh; by++) {
		for (let bx = 0; bx < bw; bx++) {
			const p = off + (by * bw + bx) * 8;
			const a = buf.readUInt16LE(p), b = buf.readUInt16LE(p + 2);
			const ca = c565(a), cb = c565(b);
			const pal = a > b
				? [ca, cb, ca.map((x, i) => (2 * x + cb[i]) / 3), ca.map((x, i) => (x + 2 * cb[i]) / 3)]
				: [ca, cb, ca.map((x, i) => (x + cb[i]) / 2), [0, 0, 0]];
			const alpha3 = a <= b; // 3색 모드의 4번째 = 투명
			const bits = buf.readUInt32LE(p + 4);
			for (let py = 0; py < 4; py++) {
				for (let px = 0; px < 4; px++) {
					const x = bx * 4 + px, y = by * 4 + py;
					if (x >= w || y >= h) continue;
					const idx = (bits >> (2 * (py * 4 + px))) & 3;
					const o = (y * w + x) * 4;
					const col = pal[idx];
					out[o] = Math.round(col[0]);
					out[o + 1] = Math.round(col[1]);
					out[o + 2] = Math.round(col[2]);
					out[o + 3] = alpha3 && idx === 3 ? 0 : 255;
				}
			}
		}
	}
	return out;
}

const index = {};
const sheets = [];
for (const file of Object.keys(coords).filter((f) => /^skills_\d+_\d+_BC1\.dds\.zst$/.test(f))) {
	const dds = await zstFile(file);
	if (dds.toString("latin1", 0, 4) !== "DDS " || dds.toString("latin1", 84, 88) !== "DX10") throw new Error(`${file}: DX10 DDS 아님`);
	const h = dds.readUInt32LE(12), w = dds.readUInt32LE(16), mips = Math.max(1, dds.readUInt32LE(28));
	const dxgi = dds.readUInt32LE(128), layers = dds.readUInt32LE(140);
	if (dxgi !== 71) throw new Error(`${file}: BC1 아님(dxgi ${dxgi})`);
	let layerBytes = 0;
	for (let m = 0, mw = w, mh = h; m < mips; m++, mw = Math.max(1, mw >> 1), mh = Math.max(1, mh >> 1)) {
		layerBytes += Math.max(1, Math.ceil(mw / 4)) * Math.max(1, Math.ceil(mh / 4)) * 8;
	}
	const base = 148;
	if (base + layerBytes * layers > dds.length) throw new Error(`${file}: 길이 불일치`);
	const cols = Math.min(16, layers), rows = Math.ceil(layers / cols);
	const sheetW = cols * w, sheetH = rows * h;
	const sheet = Buffer.alloc(sheetW * sheetH * 4);
	for (let l = 0; l < layers; l++) {
		const rgba = decodeBC1(dds, base + l * layerBytes, w, h);
		const sx = (l % cols) * w, sy = Math.floor(l / cols) * h;
		for (let y = 0; y < h; y++) rgba.copy(sheet, ((sy + y) * sheetW + sx) * 4, y * w * 4, (y + 1) * w * 4);
	}
	const sheetName = `skills-${w}.webp`;
	const raw = path.join(WORK_DIR, `tree-sheet-${w}.rgba`);
	fs.writeFileSync(raw, sheet);
	execFileSync(MAGICK, ["-size", `${sheetW}x${sheetH}`, "-depth", "8", `rgba:${raw}`, "-quality", "88", path.join(OUT, sheetName)]);
	fs.rmSync(raw, { force: true });
	sheets.push({ name: sheetName, w, layers });
	for (const [icon, layer] of Object.entries(coords[file])) {
		if (typeof layer !== "number") continue;
		const l = layer - 1;
		const entry = { s: sheetName, x: (l % cols) * w, y: Math.floor(l / cols) * h, w, h };
		// 같은 아이콘이 여러 크기에 있으면 큰 것(노터블·키스톤용)을 우선
		if (!index[icon] || index[icon].w < w) index[icon] = entry;
	}
}
fs.writeFileSync(path.join(OUT, "index.json"), JSON.stringify({ treeVersion: TREE_VER, icons: index }));
console.log(`[poe2 tree sprites] 시트 ${sheets.map((s) => `${s.name}(${s.layers})`).join(", ")} · 아이콘 ${Object.keys(index).length}개 → ${OUT}`);
