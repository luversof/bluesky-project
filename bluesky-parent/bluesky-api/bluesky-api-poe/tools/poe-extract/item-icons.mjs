// 베이스 아이템 아이콘 추출(젬 icons.mjs 패턴): BaseItemTypes → ItemVisualIdentity → DDS → PNG.
// 고유/일반 아이템 목록에서 아이콘으로 쓴다(고유 전용 아트는 추출 데이터에 없어 베이스 아트 사용).
// 출력: ~/.poe-gamedata/icons/items/<baseSlug>.png  → /poe-assets/items/ 로 서빙.
// 사용법: node item-icons.mjs  (사전: extract.mjs + parse-items.mjs 완료)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, loadTable, runExtractor } from "./paths.mjs";

const magickDir = findImageMagick();
if (!magickDir) {
	console.warn("ImageMagick 이 없어 아이템 아이콘 단계를 건너뜁니다. (winget install ImageMagick.ImageMagick)");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");

const ICON_DIR = path.join(DATA_DIR, "icons", "items");
const baseItemsFile = path.join(DATA_DIR, "base-items.json");
if (!fs.existsSync(baseItemsFile)) {
	console.warn("base-items.json 없음 — parse-items.mjs 먼저 실행 필요. 건너뜀");
	process.exit(0);
}

const bases = JSON.parse(fs.readFileSync(baseItemsFile, "utf8")).items;
const baseTable = loadTable("English", "BaseItemTypes");
const visual = loadTable("English", "ItemVisualIdentity");

// 베이스 이름(영문) → BaseItemTypes 행 (이름 매칭)
const byName = new Map();
for (const row of baseTable) if (row.Name) byName.set(row.Name.toLowerCase(), row);

// baseSlug → dds 경로
const ddsBySlug = new Map();
for (const b of bases) {
	const row = byName.get((b.name || "").toLowerCase());
	const visualRow = row != null && row.ItemVisualIdentity != null ? visual[row.ItemVisualIdentity] : null;
	if (visualRow && visualRow.DDSFile) ddsBySlug.set(b.slug, visualRow.DDSFile.toLowerCase());
}
console.log(`아이템 아이콘 대상: ${ddsBySlug.size} / ${bases.length}`);

const config = loadConfig();
config.files = [...(config.files || []), ...new Set(ddsBySlug.values())];
config.tables = [];
// 자기가 쓸 테이블만 뽑는 축소 추출 — tables/ 를 갈아엎으므로 run-all 에서 **테이블 소비 파서 뒤**에 있다
runExtractor(config, { partial: true });

// slug → category (플라스크 3프레임 합성 판단용)
const catBySlug = new Map(bases.map((b) => [b.slug, b.category]));

fs.mkdirSync(ICON_DIR, { recursive: true });
let copied = 0, flaskComposited = 0;
const missing = [];
for (const [slug, dds] of ddsBySlug) {
	const escaped = dds.replace(/\//g, "@").replace(/\.dds$/, ".png");
	const source = path.join(FILES_DIR, escaped);
	if (!fs.existsSync(source)) {
		missing.push(slug);
		continue;
	}
	const out = path.join(ICON_DIR, slug + ".png");
	// 플라스크 아이콘 = [껍데기|마스크|내용물] 3프레임 가로 스트립(각 W/3). **게임 규칙: 세 프레임을 같은 셀 위치에
	// 그대로 겹친다(스케일 없음)** — 프레임들이 셀-상대 동일 x 오프셋에 그려져 자동 정렬되고, 액체(frame3)는 각 플라스크에
	// 맞는 크기로 이미 그려져 있다. 따라서 내용물(frame3) 위에 껍데기(frame1)를 native 오버레이만 하면 전 티어(소형~영원의,
	// 생명/마나/하이브리드/특수) 정확히 채워진다. ⚠ 과거 스케일 보정은 오판이었음(상위 티어 과대·세로 스트레치 유발).
	//   frame2(마스크)는 부분충전 애니메이션용이라 정적 아이콘에선 안 쓴다. -flatten 기본 흰배경 방지 위해 -background none 필수.
	let composited = false;
	if (catBySlug.get(slug) === "flask") {
		try {
			const b = fs.readFileSync(source);
			const W = b.readUInt32BE(16), H = b.readUInt32BE(20);
			const fw = Math.round(W / 3);
			execFileSync(MAGICK, [
				"(", source, "-crop", `${fw}x${H}+${W - fw}+0`, "+repage", ")",
				"(", source, "-crop", `${fw}x${H}+0+0`, "+repage", ")",
				"-background", "none", "-flatten", "-trim", "+repage", out,
			]);
			composited = true;
			flaskComposited++;
		} catch (e) {
			// 합성 실패 시 아래 일반 처리로 폴백
		}
	}
	if (!composited) {
		// 그 외 베이스 아이콘은 단일 이미지 — 여백만 다듬어 그대로 저장.
		execFileSync(MAGICK, ["-background", "none", source, "-trim", "+repage", out]);
	}
	copied++;
}
if (flaskComposited) console.log(`플라스크 3프레임 합성: ${flaskComposited}개`);
// 캐시버스터 — 템플릿(itemList/uniqueList/simOptimizeResult)의 아이템·고유 아이콘 URL 은
// PoeIconVersion.current()=icons/version.txt 를 ?v 로 쓴다. 아이템 아이콘 재생성 시 이 공유 version.txt 를
// 갱신해야 브라우저가 새 아이콘을 받는다(items-version.txt 는 아무도 안 읽어 잠복버그였음).
const bust = String(Date.now());
fs.writeFileSync(path.join(DATA_DIR, "icons", "version.txt"), bust);
fs.writeFileSync(path.join(DATA_DIR, "icons", "items-version.txt"), bust);
console.log(`복사 완료: ${copied}, 누락: ${missing.length}`);
