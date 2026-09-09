// 엔진이 모델링하지 않는 전직 노드 추출 — PoB 의 ModCache 에서 "모드로 변환되지 않은 문장"을 뽑아,
// 그 문장만으로 이루어진 전직 노드를 골라낸다. 그런 노드에 포인트를 쓰면 값이 **정확히 0** 이다.
//
// 발단(2026-09-09): 사이온 루미너리는 스탯 노드 17개 중 15개가 "Your Mercenary ..." 인데
//   PoB 계산 엔진에는 용병 액터가 아예 없다 — src/Modules · src/Classes 통틀어 mercenary 참조 0건이고,
//   Data.lua 가 ModMercenary 를 Explicit/Corrupted/Delve/Eldritch 와 나란히 **아이템 모드 풀**로만 등록한다.
//   그런데 화면엔 아무 표시가 없어서, 사용자가 루미너리를 고르면 전직 8pt 가 0 을 내는 걸 알 수 없었다.
//
// 판별 근거는 PoB 자신의 기록이다 — ModCache 항목은 {모드, 미해석텍스트} 형태이고, 모드가 nil 이면
//   그 문장을 엔진이 modifier 하나로도 바꾸지 못했다는 뜻이다(실측: 파싱 20,393 · 미파싱 2,814).
//   대조 검증: "You have Acceleration Shrine Buff while affected by no Flasks" 는 미파싱 목록에 **없다**
//   (정상 파싱) — 판별법이 아무 문장이나 미파싱으로 몰지 않는다는 확인이다.
//
// ⚠ 정규식으로 짜지 말 것. 이 파일을 만들 때 셸/heredoc 을 거치며 백슬래시가 유실돼 몇 번 깨졌다.
//   문자열 탐색(indexOf)만으로 충분하다.
// 사용법: node parse-engine-unmodeled.mjs
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, WORK_DIR } from "./paths.mjs";

const cacheFile = path.join(WORK_DIR, "pob-src", "src", "Data", "ModCache.lua");
if (!fs.existsSync(cacheFile)) {
	console.warn("ModCache.lua 없음 — 건너뜀(PoB 소스 클론 후 재실행):", cacheFile);
	process.exit(0);
}
const treeFile = path.join(DATA_DIR, "passive-tree.json");
if (!fs.existsSync(treeFile)) {
	console.warn("passive-tree.json 없음 — 건너뜀(parse-tree.mjs 뒤에 실행되어야 한다)");
	process.exit(0);
}

const NL = String.fromCharCode(10), Q = String.fromCharCode(34);
const unparsed = new Set();
let parsedCount = 0;
for (const line of fs.readFileSync(cacheFile, "utf8").split(NL)) {
	const at = line.indexOf("c[" + Q);
	if (at < 0) continue;
	const marker = Q + "]={";
	const end = line.indexOf(marker, at + 2);
	if (end < 0) continue;
	if (line.slice(end + marker.length).indexOf("nil") === 0) unparsed.add(line.slice(at + 3, end));
	else parsedCount++;
}

// ModCache 키는 줄바꿈이 공백으로 눌린 형태로도 들어 있어, 정규화 비교를 함께 쓴다.
const squash = (s) => s.split(NL).join(" ").split(String.fromCharCode(13)).join(" ").split("  ").join(" ").trim();
const unparsedSquashed = new Set([...unparsed].map(squash));
const isUnparsed = (stat) => unparsed.has(stat) || unparsedSquashed.has(squash(stat));

const tree = JSON.parse(fs.readFileSync(treeFile, "utf8"));
const nodes = [];
const summary = {};
for (const n of tree.nodes) {
	if (!n.ascendancy || !(n.stats || []).length) continue;
	const s = (summary[n.ascendancy] = summary[n.ascendancy] || { total: 0, unmodeled: 0 });
	s.total++;
	if ((n.stats || []).every(isUnparsed)) {
		s.unmodeled++;
		nodes.push({ id: n.id, name: n.name || "", nameKo: n.nameKo || "", ascendancy: n.ascendancy });
	}
}

const out = {
	source: "ModCache.lua",
	parsed: parsedCount,
	unparsed: unparsed.size,
	unmodeledNodes: nodes.sort((x, y) => x.id - y.id),
	ascendancySummary: summary,
};
const outFile = path.join(DATA_DIR, "engine-unmodeled.json");
fs.writeFileSync(outFile, JSON.stringify(out, null, "	"));
const worst = Object.entries(summary)
	.filter((e) => e[1].unmodeled > 0)
	.sort((x, y) => y[1].unmodeled / y[1].total - x[1].unmodeled / x[1].total)
	.slice(0, 5)
	.map((e) => e[0] + " " + e[1].unmodeled + "/" + e[1].total);
console.log("engine-unmodeled.json — 파싱 " + parsedCount + " · 미파싱 " + unparsed.size + " · 미모델링 전직 노드 " + nodes.length + "개");
console.log("  상위: " + worst.join(" · "));
