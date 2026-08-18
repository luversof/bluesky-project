// 스킬젬 아이콘 추출: BaseItemTypes → ItemVisualIdentity → DDS 경로 목록을 config 에 넣어
// pathofexile-dat 로 PNG 변환(ImageMagick 필요) 후 ~/.poe-gamedata/icons/gems/<slug>.png 로 배치한다.
// 사용법: node icons.mjs  (사전 조건: extract.mjs + transform.mjs 실행 완료)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, loadTable, runExtractor } from "./paths.mjs";

// DDS→PNG 변환은 ImageMagick 필수 — 없으면 이 단계만 건너뛰어 파이프라인 전체는 성공시킨다
const magickDir = findImageMagick();
if (!magickDir) {
	console.warn("ImageMagick 이 없어 아이콘 단계를 건너뜁니다. 설치: winget install ImageMagick.ImageMagick (설치 후 데이터 갱신 재실행)");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");

const ICON_DIR = path.join(DATA_DIR, "icons", "gems");

const base = loadTable("English", "BaseItemTypes");
const visual = loadTable("English", "ItemVisualIdentity");
const gems = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "skill-gems.json"), "utf8")).gems;

// slug → dds 경로
const baseById = new Map(base.map((b) => [b.Id, b]));
const ddsBySlug = new Map();
for (const gem of gems) {
	const baseRow = baseById.get(gem.id);
	const visualRow =
		baseRow != null && baseRow.ItemVisualIdentity != null ? visual[baseRow.ItemVisualIdentity] : null;
	if (visualRow && visualRow.DDSFile) ddsBySlug.set(gem.slug, visualRow.DDSFile.toLowerCase());
}
console.log(`아이콘 대상: ${ddsBySlug.size} / ${gems.length}`);

// repo config 는 그대로 두고, 작업 디렉토리용 config 에만 DDS 목록을 추가해 실행
const config = loadConfig();
config.files = [...(config.files || []), ...new Set(ddsBySlug.values())];
config.tables = []; // 아이콘 실행에서는 테이블 재추출 불필요
// 자기가 쓸 테이블만 뽑는 축소 추출 — tables/ 를 갈아엎으므로 run-all 에서 **테이블 소비 파서 뒤**에 있다
runExtractor(config, { partial: true });

// files/art@2ditems@gems@fireball.png → icons/gems/SkillGemFireball.png
fs.mkdirSync(ICON_DIR, { recursive: true });
// 액티브 젬 합성용 임시 파일(젬/글리프 반쪽) — 매 반복 덮어쓰고 끝나면 정리
const TMP_GEM = path.join(ICON_DIR, "_tmp_gem.png");
const TMP_SKILL = path.join(ICON_DIR, "_tmp_skill.png");
let copied = 0;
const missing = [];
for (const [slug, dds] of ddsBySlug) {
	const escaped = dds.replace(/\//g, "@").replace(/\.dds$/, ".png");
	const source = path.join(FILES_DIR, escaped);
	if (fs.existsSync(source)) {
		// 액티브 젬 아이콘은 [스킬 글리프 | 젬 크리스탈]이 가로로 든 스트립(예 236x80).
		// 게임 인벤토리 아이콘처럼 젬 크리스탈(밑) + 스킬 글리프(위)를 **두 bbox 중심을 맞춰 겹친다**(center-merge).
		// 근거: poedb 가 서빙하는 실제 게임 합성 아이콘(78x78)과 대조 시 center-merge 가 가장 근접.
		//  (정확한 젬별 오프셋은 게임 클라이언트 내부 데이터라 추출본만으론 픽셀 완벽은 불가 — center-merge 가 최선 근사.)
		// 보조 젬은 정사각 단일 아이콘이라 그대로(여백만 다듬어 정사각). 공통: -trim 후 정사각 -extent 통일.
		// ⚠️ `-size WxH xc:none` 파렌 안 crop+trim 은 글리프 합성이 조용히 누락 → 반드시 임시 파일로 분리 후 -composite.
		const target = path.join(ICON_DIR, slug + ".png");
		const dim = execFileSync(MAGICK, ["identify", "-format", "%w %h", source]).toString().trim().split(" ").map(Number);
		if (dim[0] > dim[1]) {
			const half = Math.floor(dim[0] / 2);
			execFileSync(MAGICK, ["-background", "none", source, "-crop", `${dim[0] - half}x${dim[1]}+${half}+0`, "+repage", "-trim", "+repage", TMP_GEM]);
			execFileSync(MAGICK, ["-background", "none", source, "-crop", `${half}x${dim[1]}+0+0`, "+repage", "-trim", "+repage", TMP_SKILL]);
			const [gw, gh] = execFileSync(MAGICK, ["identify", "-format", "%w %h", TMP_GEM]).toString().trim().split(" ").map(Number);
			const [sw, sh] = execFileSync(MAGICK, ["identify", "-format", "%w %h", TMP_SKILL]).toString().trim().split(" ").map(Number);
			execFileSync(MAGICK, [
				"-size", `${Math.max(sw, gw)}x${Math.max(sh, gh)}`, "xc:none",
				"(", TMP_GEM, ")", "-gravity", "center", "-composite", // 젬 = 밑
				"(", TMP_SKILL, ")", "-gravity", "center", "-composite", // 글리프 = 위(중심 일치)
				"-trim", "+repage",
				"-set", "option:sq", "%[fx:max(w,h)]", "-gravity", "center", "-background", "none", "-extent", "%[sq]x%[sq]", "+repage",
				target,
			]);
		} else {
			execFileSync(MAGICK, [
				"-background", "none", source, "-trim", "+repage",
				"-set", "option:sq", "%[fx:max(w,h)]", "-gravity", "center", "-extent", "%[sq]x%[sq]", "+repage",
				target,
			]);
		}
		copied++;
	} else {
		missing.push(slug);
	}
}

// 합성용 임시 파일 정리
for (const f of [TMP_GEM, TMP_SKILL]) {
	try { fs.rmSync(f); } catch { /* 없으면 무시 */ }
}

// 캐시버스터 스탬프 — 아이콘을 재생성할 때마다 값이 바뀌어야 브라우저가 옛 아이콘을 재사용하지 않는다.
// (patch 만으로는 같은 patch 에서 아이콘 방식만 바꿔 재생성하면 URL 이 동일해 캐시가 안 갱신됨)
fs.writeFileSync(path.join(DATA_DIR, "icons", "version.txt"), String(Date.now()));

console.log(`복사 완료: ${copied}, 누락: ${missing.length}${missing.length ? " → " + missing.slice(0, 5).join(", ") : ""}`);
