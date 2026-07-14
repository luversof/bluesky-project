// 스킬젬 아이콘 추출: BaseItemTypes → ItemVisualIdentity → DDS 경로 목록을 config 에 넣어
// pathofexile-dat 로 PNG 변환(ImageMagick 필요) 후 ~/.poe-gamedata/icons/gems/<slug>.png 로 배치한다.
// 사용법: node icons.mjs  (사전 조건: extract.mjs + transform.mjs 실행 완료)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable, runExtractor } from "./paths.mjs";

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
runExtractor(config);

// files/art@2ditems@gems@fireball.png → icons/gems/SkillGemFireball.png
fs.mkdirSync(ICON_DIR, { recursive: true });
let copied = 0;
const missing = [];
for (const [slug, dds] of ddsBySlug) {
	const escaped = dds.replace(/\//g, "@").replace(/\.dds$/, ".png");
	const source = path.join(FILES_DIR, escaped);
	if (fs.existsSync(source)) {
		fs.copyFileSync(source, path.join(ICON_DIR, slug + ".png"));
		copied++;
	} else {
		missing.push(slug);
	}
}
console.log(`복사 완료: ${copied}, 누락: ${missing.length}${missing.length ? " → " + missing.slice(0, 5).join(", ") : ""}`);
