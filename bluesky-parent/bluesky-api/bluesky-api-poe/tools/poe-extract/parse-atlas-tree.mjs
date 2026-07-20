// GGG 공식 아틀라스 패시브 트리(grindinggear/atlastree-export data.json) → 뷰어용 경량 JSON.
// 스킬 트리와 동일 포맷(groups/nodes/constants/sprites)이라 tree-common.buildTree 를 그대로 쓴다.
// 아틀라스엔 직업/전직이 없고, 한국어 조인 테이블이 없어 영문 이름/스탯을 그대로 사용한다.
// 사용법: node parse-atlas-tree.mjs  (스프라이트 시트는 tree-sprites.mjs 가 별도 처리)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, WORK_DIR } from "./paths.mjs";
import { buildTree } from "./tree-common.mjs";

const RAW = path.join(WORK_DIR, "atlas-tree-raw.json");
const OUT = path.join(DATA_DIR, "atlas-tree.json");
const SOURCE_URL = "https://raw.githubusercontent.com/grindinggear/atlastree-export/master/data.json";

if (!fs.existsSync(RAW)) {
	console.log("다운로드:", SOURCE_URL);
	const response = await fetch(SOURCE_URL);
	if (!response.ok) throw new Error(`다운로드 실패: ${response.status}`);
	fs.writeFileSync(RAW, await response.text());
}

const tree = JSON.parse(fs.readFileSync(RAW, "utf8"));
const result = buildTree(tree, null); // 아틀라스는 영문만
fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(`atlas nodes ${result.nodes.length}, edges ${result.edges.length}, groups ${Object.keys(result.groups).length} → ${OUT}`);
console.log("size:", (fs.statSync(OUT).size / 1024 / 1024).toFixed(1) + "MB");
