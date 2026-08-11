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
	const sourceFiles = fs.existsSync(zipPath) ? [zipPath] : parts.map((p) => path.join(DIR, p));
	if (!sourceFiles.length) {
		console.warn("  소스 없음:", name);
		continue;
	}
	// ⚠ "있으면 스킵"으로 두면 PoB 소스를 갱신(git pull)한 뒤에도 **옛 .bin** 이 그대로 남는다.
	// NewFileSearch 보정이 .bin 을 항상 "더 최신"이라고 보고하므로 PoB 는 스스로 못 고치고,
	// 크기가 어긋난 LUT 를 읽어 반경 노드 변환이 nil 이 된다 → 스펙 임포트 실패 → 클래스가 사이온으로
	// 떨어진 **빈 빌드 수치**가 조용히 나온다(실측: ninja 대표 16건이 전부 동일한 EHP 9,606).
	// 원본이 .bin 보다 새로우면 반드시 다시 푼다.
	const newestSource = Math.max(...sourceFiles.map((f) => fs.statSync(f).mtimeMs));
	if (fs.existsSync(binPath) && fs.statSync(binPath).size > 0 && fs.statSync(binPath).mtimeMs >= newestSource) {
		done++;
		continue;
	}
	try {
		const source = Buffer.concat(sourceFiles.map((f) => fs.readFileSync(f)));
		const out = zlib.inflateSync(source);
		fs.writeFileSync(binPath, out);
		console.log(`  ${name} 재생성 (${out.length.toLocaleString()} 바이트)`);
		done++;
	} catch (error) {
		console.warn(`  ${name} 해제 실패:`, error.message);
	}
}
console.log(`타임리스 주얼 데이터 ${done}/${names.length}종 준비 → ${DIR}`);
