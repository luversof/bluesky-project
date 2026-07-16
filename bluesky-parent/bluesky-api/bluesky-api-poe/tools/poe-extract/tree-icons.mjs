// 패시브 트리 노드 아이콘 추출: passive-tree-raw.json 의 node.icon(Art/2DArt/SkillIcons/passives/*.png)
// → 게임 DDS 경로로 변환해 pathofexile-dat 로 PNG 추출(캐시된 번들 사용, ImageMagick 필요)
// → ~/.poe-gamedata/icons/tree/<key>.png 로 배치. key = skillicons 이하 경로를 소문자 '_' 로 평탄화.
// 사용법: node tree-icons.mjs [limit]   (사전조건: extract.mjs 로 번들 캐시/tables 확보)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, WORK_DIR, findImageMagick, loadConfig, runExtractor } from "./paths.mjs";

if (!findImageMagick()) {
	console.warn("ImageMagick 이 없어 트리 아이콘 단계를 건너뜁니다. 설치: winget install ImageMagick.ImageMagick");
	process.exit(0);
}

const limit = Number(process.argv[2]) || 0; // 테스트용: 앞 N개만
const ICON_DIR = path.join(DATA_DIR, "icons", "tree");
const RAW = path.join(WORK_DIR, "passive-tree-raw.json");

const tree = JSON.parse(fs.readFileSync(RAW, "utf8"));
const nodesObj = tree.nodes;
const nodeList = Array.isArray(nodesObj) ? nodesObj : Object.values(nodesObj);

// icon path("Art/2DArt/SkillIcons/passives/X.png") → { dds, key }
function toKey(iconPath) {
	// skillicons/ 이후를 평탄화 (없으면 basename)
	const lower = iconPath.replace(/\\/g, "/").toLowerCase();
	const idx = lower.indexOf("skillicons/");
	const tail = idx >= 0 ? lower.slice(idx + "skillicons/".length) : lower.split("/").pop();
	return tail.replace(/\//g, "_").replace(/\.png$/, "") + ".png";
}
function toDds(iconPath) {
	return iconPath.replace(/\\/g, "/").toLowerCase().replace(/\.png$/, ".dds");
}

const byKey = new Map(); // key → dds
for (const n of nodeList) {
	if (!n.icon) continue;
	byKey.set(toKey(n.icon), toDds(n.icon));
}
let entries = [...byKey.entries()];
if (limit > 0) entries = entries.slice(0, limit);
console.log(`트리 아이콘 대상: ${entries.length}개 (전체 ${byKey.size})`);

const config = loadConfig();
config.files = [...(config.files || []), ...new Set(entries.map((e) => e[1]))];
config.tables = [];
runExtractor(config);

fs.mkdirSync(ICON_DIR, { recursive: true });
let copied = 0;
const missing = [];
for (const [key, dds] of entries) {
	const escaped = dds.replace(/\//g, "@").replace(/\.dds$/, ".png");
	const source = path.join(FILES_DIR, escaped);
	if (fs.existsSync(source)) {
		fs.copyFileSync(source, path.join(ICON_DIR, key));
		copied++;
	} else {
		missing.push(key);
	}
}
console.log(`복사 완료: ${copied}, 누락: ${missing.length}${missing.length ? " → " + missing.slice(0, 6).join(", ") : ""}`);
