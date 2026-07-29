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
// 패시브/아틀라스 트리 노드 팝업(툴팁)의 인게임 장식 구분선 — 팝업 자체는 민무늬 암색 패널이고
// 제목 아래 이 구분선이 유일한 아트다(트리 툴팁 인게임화의 핵심 에셋).
WANTED.set("PassiveSkillScreenPassivePopupSeparator", "passivepopupseparator");

// 트리 노드 툴팁 프레임 — 인게임 패시브/아틀라스 트리 툴팁이 쓰는 공용 프레임은
// InGame 이 아니라 Misc/4K 네임스페이스에 있다(TooltipTop=장식 헤더, Middle=본문 타일, Bottom=하단 마감).
const WANTED_FULL = new Map([
	["Art/2DArt/UIImages/Misc/4K/TooltipTop", "tooltiptop"],
	["Art/2DArt/UIImages/Misc/4K/TooltipMiddle", "tooltipmiddle"],
	["Art/2DArt/UIImages/Misc/4K/TooltipBottom", "tooltipbottom"],
]);
// 참고: 시뮬 결과 장비 페이퍼돌 배경 `inventory-panel.png`(600x515)은 게임 번들이 아니라 **공식 웹 캐릭터시트**
//   MainInventory.png(600x781, web.poecdn.com/protected/image/inventory/MainInventory.png)를 받아 장비영역만 크롭한 것.
//   (게임 번들의 InventoryPanelUpperBackground(969x723)는 현행 웹/게임 캐릭터창과 레이아웃이 달라 사용하지 않음.)
//   슬롯 좌표는 simOptimizeResult.jte dollSlots 에 배경 픽셀 밝기 프로파일로 검출한 값으로 하드코딩.
// 트리 노드 툴팁의 **타입별 장식 헤더 밴드**(금장 캡 + 암적색 밴드, 인게임 실물) — 4K 판이 없어
// 기본 InGame 네임스페이스에서 추출한다(사용자 스크린샷 "비전의 의지" = Notable 헤더).
for (const type of ["Normal", "Notable", "Keystone", "Jewel", "Ascendancy"]) {
	for (const side of ["Left", "Middle", "Right"]) {
		WANTED_FULL.set(`Art/2DArt/UIImages/InGame/${type}PassiveHeader${side}`, `psheader${type.toLowerCase()}${side.toLowerCase()}`);
	}
}
for (const side of ["Left", "Middle", "Right"]) {
	WANTED_FULL.set(`Art/2DArt/UIImages/InGame/PassiveMastery/MasteryPassiveHeader${side}`, `psheadermastery${side.toLowerCase()}`);
}

// 1) atlas 좌표표 추출 (UTF-16LE)
const baseConfig = loadConfig();
runExtractor({ ...baseConfig, tables: [], files: [...(baseConfig.files || []), "art/uiimages1.txt"] });
const indexText = fs.readFileSync(path.join(FILES_DIR, "art@uiimages1.txt")).toString("utf16le");

// 2) 필요한 항목의 좌표/시트 수집
const entries = [];
for (const line of indexText.split(/\r?\n/)) {
	const m = line.match(/"([^"]+)"\s+"([^"]+)"\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)/);
	if (!m) continue;
	const out = m[1].startsWith(PREFIX) ? WANTED.get(m[1].slice(PREFIX.length)) : WANTED_FULL.get(m[1]);
	if (!out) continue;
	entries.push({ out, tex: m[2].toLowerCase(), x: +m[3], y: +m[4], w: +m[5] - +m[3], h: +m[6] - +m[4] });
}
const wantedCount = WANTED.size + WANTED_FULL.size;
const missing = [...WANTED.values(), ...WANTED_FULL.values()].filter((o) => !entries.some((e) => e.out === o));
console.log(`atlas 매칭: ${entries.length}/${wantedCount}${missing.length ? " (누락: " + missing.join(", ") + ")" : ""}`);

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
