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
// 영향력/엘드리치 아이템 심볼 — 인게임 아이템 툴팁 헤더 좌우 상단 모서리에 뜨는 마크.
// 총주교=CleansingFire, 포식자=Tangled(게임 내부명). 쉐이퍼/엘더는 동명.
WANTED_FULL.set("Art/2DArt/UIImages/InGame/ShaperItemSymbol", "influence-shaper");
WANTED_FULL.set("Art/2DArt/UIImages/InGame/ElderItemSymbol", "influence-elder");
WANTED_FULL.set("Art/2DArt/UIImages/InGame/CleansingFireItemSymbol", "influence-exarch");
WANTED_FULL.set("Art/2DArt/UIImages/InGame/TangledItemSymbol", "influence-eater");
// 정복자 넷(10-04 C137 — 빌드 화면 툴팁 영향력 아이콘). 게임 내부명(art@uiimages1.txt 실측): 사냥꾼=Basilisk · 구원자=Eyrie · 전쟁군주=Judicator
WANTED_FULL.set("Art/2DArt/UIImages/InGame/CrusaderItemSymbol", "influence-crusader");
WANTED_FULL.set("Art/2DArt/UIImages/InGame/BasiliskItemSymbol", "influence-hunter");
WANTED_FULL.set("Art/2DArt/UIImages/InGame/EyrieItemSymbol", "influence-redeemer");
WANTED_FULL.set("Art/2DArt/UIImages/InGame/JudicatorItemSymbol", "influence-warlord");
// 분열 · 합성 아이템도 인게임 헤더 모서리에 심볼(C137)
WANTED_FULL.set("Art/2DArt/UIImages/InGame/FracturedItemSymbol", "influence-fractured");
WANTED_FULL.set("Art/2DArt/UIImages/InGame/SynthesisedItemSymbol", "influence-synthesised");
// 장막 옵션 줄(10-20 C482 — 사용자 요청 "게임 내랑 동일한 디자인이면 좋을 텐데 이미지를 찾아서 대체") — 인게임 툴팁은 장막 옵션을 룬처럼 뒤섞인
// 글자 그림으로 그린다. 그 그림만 따로 든 아틀라스 항목은 없어(art/*.txt 의 'veil' 은 신디케이트 문장 · 제작창뿐), 게임이 그린 툴팁이 든 튜토리얼
// 그림에서 그 띠를 잘라 쓴다(아래 5) — 밝기를 투명도로 해 어느 바탕에도)
WANTED_FULL.set("Art/2DArt/UIImages/InGame/TutorialPictures/UnveiledMods", "tutorial-unveiledmods");

// 1) atlas 좌표표 추출 (UTF-16LE)
const baseConfig = loadConfig();
runExtractor({ ...baseConfig, tables: [], files: [...(baseConfig.files || []), "art/uiimages1.txt"] }, { partial: true });
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
runExtractor({ ...baseConfig, tables: [], files: [...(baseConfig.files || []), ...new Set(entries.map((e) => e.tex))] }, { partial: true });

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
// 5) 장막 옵션 줄 그림(C482) — 튜토리얼 그림(2627x1745) 오른쪽 위 툴팁의 장막 줄(룬 띠)만 잘라, 밝기를 투명도로
{
	const tut = path.join(OUT_DIR, "tutorial-unveiledmods.png");
	if (fs.existsSync(tut)) {
		const tmp = path.join(OUT_DIR, "veiled-mod.tmp.png");
		execFileSync(MAGICK, [tut, "-crop", "600x56+1660+750", "+repage", "-fuzz", "10%", "-trim", "+repage", tmp]);
		execFileSync(MAGICK, [tmp, "(", "+clone", "-colorspace", "gray", "-level", "8%,85%", ")", "-alpha", "off", "-compose", "copy-opacity", "-composite", path.join(OUT_DIR, "veiled-mod.png")]);
		fs.rmSync(tmp, { force: true });
		fs.rmSync(tut, { force: true }); // 큰 원본은 남기지 않는다
		done++;
		veiledFrames(path.join(OUT_DIR, "veiled-mod.png"), path.join(OUT_DIR, "veiled-mod-anim.png"));
		done++;
	} else {
		console.warn("튜토리얼 그림 없음 — veiled-mod.png 건너뜀");
	}
}
console.log(`ui-assets 완료: ${done}개 → ${OUT_DIR}`);

/**
 * 장막 줄 움직임 세 벌(C484) — 게임 툴팁의 장막 줄은 룬 문양이 다른 벌로 바뀌며 반짝인다(정지 그림은 튜토리얼 한 장뿐, 움직이는 원본은 게임 파일에 없음).
 * 룬 띠를 빈 열 기준으로 마디로 쪼개 순서 · 뒤집기를 바꾼 두 벌을 더 만들고 세 줄로 쌓는다(빈 열 폭은 그대로라 너비가 같다). 화면은 CSS 로 세 줄을 번갈아 보인다.
 */
function veiledFrames(src, out) {
	const [w, h] = execFileSync(MAGICK, ["identify", "-format", "%w %h", src]).toString().trim().split(" ").map(Number);
	const col = execFileSync(MAGICK, [src, "-alpha", "extract", "-scale", `${w}x1!`, "-depth", "8", "txt:-"])
		.toString()
		.split(/\r?\n/)
		.slice(1)
		.filter(Boolean)
		.map((l) => Number((l.match(/\((\d+)/) || [0, 0])[1]));
	// 마디 = 밝은 열(> 8)이 이어진 구간, 4px 미만 틈은 마디 안으로
	const segs = [];
	let start = -1;
	let gap = 0;
	for (let x = 0; x <= w; x++) {
		const on = x < w && col[x] > 8;
		if (on) {
			if (start < 0) start = x;
			gap = 0;
		} else if (start >= 0) {
			gap++;
			if (gap >= 4 || x === w) {
				segs.push({ x: start, w: x - gap + 1 - start });
				start = -1;
				gap = 0;
			}
		}
	}
	if (segs.length < 3) {
		fs.copyFileSync(src, out);
		return;
	}
	// 마디 사이 틈(앞 여백 · 뒤 여백 포함)은 원래 순서 그대로, 마디만 바꿔 넣는다
	// 고정 씨앗 섞기(같은 입력이면 늘 같은 그림)
	const shuffled = (seed) => {
		const idx = segs.map((_, i) => i);
		let r = seed;
		for (let i = idx.length - 1; i > 0; i--) {
			r = (r * 1103515245 + 12345) % 2147483648;
			const j = r % (i + 1);
			[idx[i], idx[j]] = [idx[j], idx[i]];
		}
		return idx;
	};
	const p2 = shuffled(7);
	const p3 = shuffled(29);
	const variants = [
		segs.map((s) => ({ ...s, op: null })),
		p2.map((k, i) => ({ ...segs[k], op: i % 2 ? "-flop" : null })),
		p3.map((k, i) => ({ ...segs[k], op: i % 3 === 0 ? "-flip" : i % 3 === 1 ? "-flop" : null })),
	];
	const rows = variants.map((v, k) => {
		const file = out.replace(/\.png$/, `.row${k}.png`);
		const args = ["-size", `${w}x${h}`, "xc:none"];
		let x = segs[0].x;
		v.forEach((s, i) => {
			args.push("(", src, "-crop", `${s.w}x${h}+${s.x}+0`, "+repage");
			if (s.op) args.push(s.op);
			args.push(")", "-geometry", `+${x}+0`, "-composite");
			x += s.w + (i + 1 < segs.length ? segs[i + 1].x - (segs[i].x + segs[i].w) : 0);
		});
		args.push(file);
		execFileSync(MAGICK, args);
		return file;
	});
	execFileSync(MAGICK, [...rows, "-append", out]);
	rows.forEach((f) => fs.rmSync(f, { force: true }));
}
