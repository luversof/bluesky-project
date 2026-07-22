// 무궁한(타임리스) 주얼 데이터 미리 압축 해제 — PoB 헤드리스는 `Inflate()` 가 **빈 스텁**이라
// (HeadlessWrapper.lua 원본 주석: "TODO: And this") Data/TimelessJewelData/*.zip 을 못 읽는다.
// → 반경 패시브 변환이 통째로 계산되지 않는다(실측: 스탯 결과가 빈 객체).
// 여기서 .zip(사실은 raw zlib 스트림) 을 미리 풀어 **.bin** 을 만들어 두면, PoB 가 "압축 해제본이 최신"
// 경로로 읽어 Inflate 없이 동작한다(worker.lua/calc.lua 의 NewFileSearch·GetScriptPath 보정과 짝).
// 사용법: node timeless-bin.mjs
import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";
import { WORK_DIR } from "./paths.mjs";

const DIR = path.join(WORK_DIR, "pob-src", "src", "Data", "TimelessJewelData");
if (!fs.existsSync(DIR)) {
	console.warn("PoB 타임리스 데이터 폴더 없음 — 건너뜀:", DIR);
	process.exit(0);
}

const names = ["BrutalRestraint", "LethalPride", "MilitantFaith", "ElegantHubris", "HeroicTragedy", "GloriousVanity"];
let done = 0;
for (const name of names) {
	const binPath = path.join(DIR, `${name}.bin`);
	const zipPath = path.join(DIR, `${name}.zip`);
	// 분할 파일(.zip.part0…)은 이어 붙여야 한다 — 찬란한 허영심이 이 형태다
	const parts = fs
		.readdirSync(DIR)
		.filter((f) => f.startsWith(`${name}.zip.part`))
		.sort();
	let source = null;
	if (fs.existsSync(zipPath)) {
		source = fs.readFileSync(zipPath);
	} else if (parts.length) {
		source = Buffer.concat(parts.map((p) => fs.readFileSync(path.join(DIR, p))));
	}
	if (!source) {
		console.warn("  소스 없음:", name);
		continue;
	}
	// 이미 최신이면 건너뛴다(49MB 짜리도 있어 매번 풀면 낭비)
	if (fs.existsSync(binPath) && fs.statSync(binPath).size > 0) {
		done++;
		continue;
	}
	try {
		fs.writeFileSync(binPath, zlib.inflateSync(source));
		done++;
	} catch (error) {
		console.warn(`  ${name} 해제 실패:`, error.message);
	}
}
console.log(`타임리스 주얼 데이터 ${done}/${names.length}종 준비 → ${DIR}`);
