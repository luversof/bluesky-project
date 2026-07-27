// 문신 아이콘 보충 추출: tattoos.json 의 icon 중 icons/tree/ 에 없는 것(히네코라 전용 아트 등)만
// DDS→PNG 추출해 같은 키 공간(icons/tree/, tree-icons 평탄화 규칙)에 배치 — tattoos.jte 가 그대로 참조.
// ⚠ runExtractor 는 테이블 산출물을 대체하므로 run-all 에선 테이블 소비 파서들 뒤(essence-icons 옆)에 둔다.
// 사용법: node tattoo-icons.mjs  (사전: tattoos.json, ImageMagick)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, findImageMagick, loadConfig, runExtractor } from "./paths.mjs";

if (!findImageMagick()) {
	console.warn("ImageMagick 없음 — 문신 아이콘 보충 생략");
	process.exit(0);
}
const tattooFile = path.join(DATA_DIR, "tattoos.json");
if (!fs.existsSync(tattooFile)) {
	console.warn("tattoos.json 없음 — parse-tattoos.mjs 먼저 실행");
	process.exit(0);
}
const ICON_DIR = path.join(DATA_DIR, "icons", "tree");

function toKey(iconPath) {
	const lower = iconPath.replace(/\\/g, "/").toLowerCase();
	const idx = lower.indexOf("skillicons/");
	const tail = idx >= 0 ? lower.slice(idx + "skillicons/".length) : lower.split("/").pop();
	return tail.replace(/\//g, "_").replace(/\.png$/, "") + ".png";
}
const toDds = (iconPath) => iconPath.replace(/\\/g, "/").toLowerCase().replace(/\.png$/, ".dds");

const data = JSON.parse(fs.readFileSync(tattooFile, "utf8"));
const tattoos = data.tattoos || data;
const byKey = new Map();
for (const t of tattoos) {
	if (!t.icon || !t.icon.toLowerCase().includes("skillicons/")) continue;
	const key = toKey(t.icon);
	if (!fs.existsSync(path.join(ICON_DIR, key))) byKey.set(key, toDds(t.icon));
}
console.log(`문신 아이콘 누락분: ${byKey.size}개`);
if (!byKey.size) process.exit(0);

const config = loadConfig();
config.files = [...new Set(byKey.values())];
config.tables = [];
runExtractor(config);

fs.mkdirSync(ICON_DIR, { recursive: true });
let copied = 0;
const missing = [];
for (const [key, dds] of byKey) {
	const source = path.join(FILES_DIR, dds.replace(/\//g, "@").replace(/\.dds$/, ".png"));
	if (fs.existsSync(source)) {
		fs.copyFileSync(source, path.join(ICON_DIR, key));
		copied++;
	} else {
		missing.push(key);
	}
}
console.log(`복사 완료: ${copied}, 실패: ${missing.length}${missing.length ? " → " + missing.slice(0, 5).join(", ") : ""}`);
