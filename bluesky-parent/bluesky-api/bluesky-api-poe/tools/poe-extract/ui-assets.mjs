// 인게임 아이템 툴팁 UI 아트 추출: art/uiimages1.txt(atlas 좌표표)를 읽어
// 4K 시트에서 헤더 밴드(1줄/2줄)·구분선·젬 소켓 배경을 crop → ~/.poe-gamedata/ui-assets/
// (기존 PoB 저해상 에셋을 게임 원본 4K 로 대체. 2줄 헤더는 레어/유니크 인게임 툴팁용)
// 사용법: node ui-assets.mjs  (ImageMagick 필요 — 없으면 이 단계만 건너뜀)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, runExtractor } from "./paths.mjs";

const magickDir = findImageMagick();
if (!magickDir) {
	console.warn("ImageMagick 이 없어 ui-assets 단계를 건너뜁니다.");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");

const OUT_DIR = path.join(DATA_DIR, "ui-assets");
const PREFIX = "Art/2DArt/UIImages/InGame/4K/";

// 게임 atlas 항목명(4K) → 산출 파일명. SingleLine = 1줄 밴드(기존 이름 유지, 4K 로 화질만 향상),
// 접미사 없는 Rare/Unique = 인게임 2줄 헤더(이름+베이스) → itemsheader2*.
const WANTED = new Map();
for (const side of ["Left", "Middle", "Right"]) {
	const s = side.toLowerCase();
	WANTED.set(`ItemsHeaderUnique${side}`, `itemsheader2unique${s}`);
	WANTED.set(`ItemsHeaderRare${side}`, `itemsheader2rare${s}`);
	WANTED.set(`ItemsHeaderUniqueSingleLine${side}`, `itemsheaderunique${s}`);
	WANTED.set(`ItemsHeaderRareSingleLine${side}`, `itemsheaderrare${s}`);
	WANTED.set(`ItemsHeaderWhite${side}`, `itemsheaderwhite${s}`);
	WANTED.set(`ItemsHeaderMagic${side}`, `itemsheadermagic${s}`);
	WANTED.set(`ItemsHeaderGem${side}`, `itemsheadergem${s}`);
}
for (const rarity of ["White", "Magic", "Rare", "Unique", "Gem"]) {
	WANTED.set(`ItemsSeparator${rarity}`, `itemsseparator${rarity.toLowerCase()}`);
}
WANTED.set("ItemsBackgroundGem", "itemsbackgroundgem");

// 1) atlas 좌표표 추출 (UTF-16LE)
const baseConfig = loadConfig();
runExtractor({ ...baseConfig, tables: [], files: [...(baseConfig.files || []), "art/uiimages1.txt"] });
const indexText = fs.readFileSync(path.join(FILES_DIR, "art@uiimages1.txt")).toString("utf16le");

// 2) 필요한 항목의 좌표/시트 수집
const entries = [];
for (const line of indexText.split(/\r?\n/)) {
	const m = line.match(/"([^"]+)"\s+"([^"]+)"\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)/);
	if (!m || !m[1].startsWith(PREFIX)) continue;
	const out = WANTED.get(m[1].slice(PREFIX.length));
	if (!out) continue;
	entries.push({ out, tex: m[2].toLowerCase(), x: +m[3], y: +m[4], w: +m[5] - +m[3], h: +m[6] - +m[4] });
}
const missing = [...WANTED.values()].filter((o) => !entries.some((e) => e.out === o));
console.log(`atlas 매칭: ${entries.length}/${WANTED.size}${missing.length ? " (누락: " + missing.join(", ") + ")" : ""}`);

// 3) 필요한 4K 시트만 추출 (dds → png 는 pathofexile-dat + ImageMagick 이 처리)
runExtractor({ ...baseConfig, tables: [], files: [...(baseConfig.files || []), ...new Set(entries.map((e) => e.tex))] });

// 4) 시트에서 crop
fs.mkdirSync(OUT_DIR, { recursive: true });
let done = 0;
for (const e of entries) {
	const sheet = path.join(FILES_DIR, e.tex.replace(/\//g, "@").replace(/\.dds$/, ".png"));
	if (!fs.existsSync(sheet)) {
		console.warn(`시트 없음: ${sheet} (${e.out})`);
		continue;
	}
	execFileSync(MAGICK, [sheet, "-crop", `${e.w}x${e.h}+${e.x}+${e.y}`, "+repage", path.join(OUT_DIR, e.out + ".png")]);
	done++;
}
console.log(`ui-assets 완료: ${done}개 → ${OUT_DIR}`);
