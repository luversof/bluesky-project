// 패시브/아틀라스 트리 공식 스프라이트시트 self-host.
// GGG data.json 의 sprites 는 web.poecdn.com 시트 + zoom 별 coords 를 담는다. 최고 zoom 시트를
// 한 번 받아 ~/.poe-gamedata/icons/tree-sprites/{skill,atlas}/ 로 저장하고, 프론트가 blit 할 수 있게
// 매니페스트(tree-sprites-{skill,atlas}.json = {spriteKey:{file,coords}})를 emit 한다.
// 런타임 CDN 금지 관례에 맞춰 self-host. 사용법: node tree-sprites.mjs (parse-tree/parse-atlas-tree 후)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, WORK_DIR, findImageMagick } from "./paths.mjs";

const SHEET_ROOT = path.join(DATA_DIR, "icons", "tree-sprites"); // → /poe-assets/tree-sprites/ 로 서빙

// 스킬 아이콘 시트는 .jpg 인데, 서버가 image/jpeg;charset=UTF-8 + nosniff 로 내려주면 일부 브라우저가
// 이 JPEG 를 캔버스에 못 그린다(프레임 등 PNG 시트는 정상). → .jpg/.webp 는 PNG 로 변환해 self-host.
const magickDir = findImageMagick();
const MAGICK = magickDir ? (magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe")) : null;
const NEEDS_PNG = /\.(jpe?g|webp)$/i;
const pngName = (base) => (MAGICK && NEEDS_PNG.test(base) ? base.replace(NEEDS_PNG, ".png") : base);

// zoom 키 중 최대(가장 고해상도) 엔트리 선택 → {entry, zoom}
function maxZoomEntry(spriteZooms) {
	let best = null;
	let bestZoom = -1;
	for (const [z, entry] of Object.entries(spriteZooms)) {
		const zoom = Number(z);
		if (entry && entry.filename && zoom > bestZoom) {
			bestZoom = zoom;
			best = entry;
		}
	}
	return best ? { entry: best, zoom: bestZoom } : null;
}

// url 시트를 outDir 에 저장(필요 시 PNG 로 변환). 반환: 최종 파일명(png 변환 여부 반영).
async function download(url, outDir, srcBase) {
	const dstBase = pngName(srcBase);
	const dest = path.join(outDir, dstBase);
	if (fs.existsSync(dest)) return dstBase; // 이미 있으면 스킵
	const res = await fetch(url);
	if (!res.ok) throw new Error(`시트 다운로드 실패 ${res.status}: ${url}`);
	const buf = Buffer.from(await res.arrayBuffer());
	if (dstBase !== srcBase) {
		// .jpg/.webp → .png 변환 (stdin 으로 magick 에 전달)
		execFileSync(MAGICK, [srcBase.match(/\.webp$/i) ? "webp:-" : "jpg:-", dest], { input: buf });
	} else {
		fs.writeFileSync(dest, buf);
	}
	return dstBase;
}

async function processTree(rawFile, subdir, manifestName) {
	const rawPath = path.join(WORK_DIR, rawFile);
	if (!fs.existsSync(rawPath)) {
		console.warn(`  ${rawFile} 없음 — 건너뜀(parse 단계 먼저 실행)`);
		return;
	}
	const tree = JSON.parse(fs.readFileSync(rawPath, "utf8"));
	const outDir = path.join(SHEET_ROOT, subdir);
	fs.mkdirSync(outDir, { recursive: true });

	// 캐시버스터 — 시트를 재생성할 때마다 값이 바뀌어 브라우저가 옛(혹은 깨진) 시트를 재사용하지 않는다.
	const ver = Date.now();
	const manifest = {};
	const sheets = new Map(); // url → 원본 파일명(srcBase)
	const spriteMeta = []; // [key, url, zoom, coords]
	for (const [key, zooms] of Object.entries(tree.sprites || {})) {
		const picked = maxZoomEntry(zooms);
		if (!picked) continue;
		const { entry, zoom } = picked;
		const url = entry.filename;
		const base = url.split("?")[0].split("/").pop(); // skills-4.jpg 등
		if (!sheets.has(url)) sheets.set(url, base);
		spriteMeta.push([key, url, zoom, entry.coords || {}]);
	}

	// 유니크 시트만 다운로드(필요 시 PNG 변환) → url→최종파일명
	let n = 0;
	const finalName = new Map();
	for (const [url, srcBase] of sheets) {
		finalName.set(url, await download(url, outDir, srcBase));
		n++;
	}
	// 매니페스트는 최종 파일명 + 캐시버스터로 기록
	for (const [key, url, zoom, coords] of spriteMeta) {
		manifest[key] = { file: `tree-sprites/${subdir}/${finalName.get(url)}?v=${ver}`, zoom, coords };
	}
	const manifestPath = path.join(DATA_DIR, manifestName);
	fs.writeFileSync(manifestPath, JSON.stringify(manifest));
	console.log(`  ${subdir}: 시트 ${n}개, 스프라이트 키 ${Object.keys(manifest).length}개 → ${manifestName}`);
}

console.log("트리 스프라이트시트 self-host:");
await processTree("passive-tree-raw.json", "skill", "tree-sprites-skill.json");
await processTree("atlas-tree-raw.json", "atlas", "tree-sprites-atlas.json");
console.log("완료");
