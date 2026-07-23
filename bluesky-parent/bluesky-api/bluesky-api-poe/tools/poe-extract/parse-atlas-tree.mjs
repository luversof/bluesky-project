// GGG 공식 아틀라스 패시브 트리(grindinggear/atlastree-export data.json) → 뷰어용 경량 JSON.
// 스킬 트리와 동일 포맷(groups/nodes/constants/sprites)이라 tree-common.buildTree 를 그대로 쓴다.
// 아틀라스엔 직업/전직이 없다. 한국어는 스킬 트리와 같은 PassiveSkills 테이블에 들어 있어(867/867 조인) 동일하게 결합한다.
// 사용법: node parse-atlas-tree.mjs  (스프라이트 시트는 tree-sprites.mjs 가 별도 처리)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";
import { buildKoreanMap, buildTree, joinReminderKo } from "./tree-common.mjs";

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

// 아틀라스 스탯은 전용 설명 파일이 있다(패시브용으로는 308/866 밖에 못 만든다 → 전용 파일로 828/866)
// 아틀라스 전용 파일 + 그것이 include 하는 map 설명 파일(울티메이텀 등 리그 문구가 여기에 있다)
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@map_stat_descriptions.txt",
	"metadata@statdescriptions@atlas_stat_descriptions.txt",
]);
const koByGraphId = buildKoreanMap({
	describe,
	statsTable: loadTable("English", "Stats"),
	passivesEn: loadTable("English", "PassiveSkills"),
	passivesKo: loadTable("Korean", "PassiveSkills"),
});
const result = buildTree(tree, koByGraphId);
const named = result.nodes.filter((n) => n.type !== "mastery");
console.log(
	`한글 이름 ${named.filter((n) => n.nameKo).length}/${named.length}, 한글 스탯 ${named.filter((n) => n.statsKo?.length).length}/${named.filter((n) => n.stats.length).length}`,
);
// 리마인더 한글 — 게임 테이블 ReminderText(EN/KO) 페어링 조인.
// 테이블은 config 추가(2026-07) 후 추출부터 생긴다 — 옛 추출물로 parse 만 단독 재실행해도 죽지 않게 폴백.
try {
	const rem = joinReminderKo(result, loadTable("English", "ReminderText"), loadTable("Korean", "ReminderText"));
	console.log(`리마인더 한글: ${rem.hit}/${rem.total}`);
} catch {
	console.log("리마인더 한글: ReminderText 테이블 없음(extract 먼저 실행 필요) — 영문 유지");
}

fs.mkdirSync(DATA_DIR, { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(result));
console.log(`atlas nodes ${result.nodes.length}, edges ${result.edges.length}, groups ${Object.keys(result.groups).length} → ${OUT}`);
console.log("size:", (fs.statSync(OUT).size / 1024 / 1024).toFixed(1) + "MB");
