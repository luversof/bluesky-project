// 클론해 둔 PoB 소스에 우리 쪽 필수 패치를 입힌다(멱등).
//
// pob-src 는 파생 산출물이라 git 밖(~/.poe-gamedata/work/pob-src)에 두고, 갱신 파이프라인이 다시 클론/갱신할 수
// 있다. 그때마다 손으로 고치면 "기억해야 하는 절차"가 늘어나므로 여기서 자동 적용한다.
// 이미 적용돼 있으면 조용히 건너뛴다.
//
// 사용법: node patch-pob.mjs
import fs from "node:fs";
import path from "node:path";
import { WORK_DIR } from "./paths.mjs";

const SRC = path.join(WORK_DIR, "pob-src", "src");
if (!fs.existsSync(SRC)) {
	console.warn("PoB 소스 없음 — 패치 건너뜀:", SRC);
	process.exit(0);
}

const patches = [
	{
		file: "Classes/PassiveSpec.lua",
		// ⚠ 상류 버그: item 을 nil 검사(다음 줄)보다 **먼저** 역참조한다. 주얼 소켓이 가리키는 아이템이
		//   itemsTab 에 없으면(파싱 못 한 아이템 등) 여기서 터지고, PoB 가 그 예외를 삼켜 스펙 임포트가
		//   중단된다 → 클래스가 사이온으로 떨어진 **빈 빌드 수치**가 예외 없이 나간다.
		//   실측: poe.ninja 대표 아키타입 4건(Soulrend|ci, Dominating Blow x2, Heavy Strike)이 이걸로 실패.
		//   radiusIndex 는 아래 `if item and ...` 블록 안에서만 쓰이므로 nil 가드만 붙이면 충분하다.
		// 줄끝 없이 매치 — 클론본은 CRLF 라 "\n" 까지 묶으면 안 잡힌다(실측).
		find: "\t\tlocal radiusIndex = item.jewelRadiusIndex",
		replace: "\t\tlocal radiusIndex = item and item.jewelRadiusIndex",
		name: "PassiveSpec:NodesInIntuitiveLeapLikeRadius nil 가드",
	},
	{
		file: "Classes/PassiveSpec.lua",
		// 같은 함수의 두 번째 미가드 역참조 — 이쪽은 if 조건 자체가 item 을 검사하지 않는다.
		//   위 한 줄만 고치면 크래시 지점이 그대로 여기로 옮겨간다(실측: 1071 → 1081).
		find: "\t\tif item.jewelData and item.jewelData.impossibleEscapeKeystone then",
		replace: "\t\tif item and item.jewelData and item.jewelData.impossibleEscapeKeystone then",
		name: "PassiveSpec impossibleEscapeKeystone nil 가드",
	},
];

let applied = 0;
let already = 0;
for (const p of patches) {
	const file = path.join(SRC, p.file);
	if (!fs.existsSync(file)) {
		console.warn(`  대상 없음: ${p.file}`);
		continue;
	}
	const before = fs.readFileSync(file, "utf8");
	if (before.includes(p.replace)) {
		already++;
		continue;
	}
	if (!before.includes(p.find)) {
		// 상류가 스스로 고쳤거나 코드가 바뀐 것 — 실패가 아니라 알림(패치 목록 정리 신호)
		console.warn(`  적용 지점 없음(상류 변경?): ${p.name}`);
		continue;
	}
	fs.writeFileSync(file, before.replace(p.find, p.replace));
	console.log(`  적용: ${p.name} (${p.file})`);
	applied++;
}
console.log(`PoB 소스 패치 ${applied}건 적용, ${already}건 이미 적용됨 → ${SRC}`);
