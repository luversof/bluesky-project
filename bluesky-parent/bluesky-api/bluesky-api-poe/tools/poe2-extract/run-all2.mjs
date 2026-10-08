// PoE2 데이터 파이프라인 전체 실행 — 한 번 실행으로 끝나야 한다(수동 복구 절차를 사용자에게 넘기지 않는다).
// 사용: node run-all2.mjs   (작업 폴더 무관, 산출물 ~/.poe-gamedata/poe2)
//   1) 최신 PoE2 패치 버전 확인(패치 서버) → config.json 의 patch 갱신
//   2) 테이블 추출 → 스탯 설명 파일 → 파서들(베이스 → 젬 → 옵션 → 증강물 → 고유 → 트리)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import net from "node:net";
import path from "node:path";
import { REPO_DIR, WORK_DIR } from "./paths.mjs";

/** 패치 서버(patch.pathofexile2.com:13060)에 [1,6] 을 보내면 CDN 주소(…/patch/4.x.y.z/)가 UTF-16 으로 온다. */
function latestPatch() {
	return new Promise((resolve) => {
		const s = net.connect(13060, "patch.pathofexile2.com", () => s.write(Buffer.from([1, 6])));
		let buf = Buffer.alloc(0);
		const done = () => {
			for (const off of [0, 1]) {
				const m = buf.subarray(off).toString("utf16le").match(/\/patch\/(4\.\d+\.\d+\.\d+)\//);
				if (m) return resolve(m[1]);
			}
			resolve(null);
		};
		s.on("data", (d) => {
			buf = Buffer.concat([buf, d]);
			if (buf.length > 64) s.end();
		});
		s.on("close", done);
		s.on("error", () => resolve(null));
		setTimeout(() => s.destroy(), 10000);
	});
}

const step = (name, file) => {
	const t0 = Date.now();
	console.log(`\n=== ${name} ===`);
	execFileSync(process.execPath, [path.join(REPO_DIR, file)], { stdio: "inherit", cwd: REPO_DIR });
	console.log(`(${name} ${((Date.now() - t0) / 1000).toFixed(1)}초)`);
};

const configPath = path.join(REPO_DIR, "config.json");
const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
const patch = await latestPatch();
if (patch && patch !== config.patch) {
	console.log(`[poe2] 패치 ${config.patch} → ${patch}`);
	config.patch = patch;
	fs.writeFileSync(configPath, JSON.stringify(config, null, 2) + "\n");
} else {
	console.log(`[poe2] 패치 ${config.patch}${patch ? " (최신)" : " (패치 서버 응답 없음 — 설정값 그대로)"}`);
}

step("테이블 추출", "extract.mjs");
step("스탯 설명 파일", "export-files.mjs");
step("스킬 전용 스탯 설명", "export-skill-desc.mjs");
step("베이스 아이템", "parse-items2.mjs");
step("젬", "parse-gems2.mjs");
step("레어 이름 낱말", "rare-names2.mjs"); // 빌드 요약 레어 이름 한국어(10-04 C144)
step("옵션", "parse-mods2.mjs");
step("경로석 옵션", "parse-waystone-mods2.mjs");
step("증강물(룬·영혼 핵)", "parse-augments2.mjs");
// 빌드 엔진(PoB-PoE2) 소스 — 빌드 재계산뿐 아니라 고유 파서도 쓴다(Lua 코드로 만들어지는 고유 8종을 엔진이 덤프) → 파서보다 먼저.
//   실패해도 계속 간다(고유는 파일에서 읽은 것만, 빌드 재계산만 안 된다).
let engineReady = false;
try {
	const t0 = Date.now();
	console.log("\n=== 빌드 엔진(PoB-PoE2) 소스 · 고유 DB 덤프 ===");
	execFileSync(process.execPath, [path.join(REPO_DIR, "patch-pob2.mjs"), "--update"], { stdio: "inherit", cwd: REPO_DIR });
	const winget = path.join(os.homedir(), "AppData", "Local", "Programs", "LuaJIT", "bin", "luajit.exe");
	const out = execFileSync(fs.existsSync(winget) ? winget : "luajit", [path.join(REPO_DIR, "..", "poe2-pob", "dump-uniques2.lua"), path.join(WORK_DIR, "pob-unique-db.json")], {
		cwd: path.join(WORK_DIR, "pob2-src", "src"),
		encoding: "utf8",
		timeout: 120000,
	});
	const n = (out.match(/@@DUMP@@(\d+)/) || [])[1];
	if (!n) throw new Error("고유 DB 덤프 결과 없음");
	console.log(`고유 DB 덤프 ${n}종 (${((Date.now() - t0) / 1000).toFixed(1)}초)`);
	engineReady = true;
} catch (e) {
	console.warn(`[poe2] ⚠ 빌드 엔진 준비 실패 — 고유는 파일에서 읽은 것만, 빌드 재계산은 안 된다: ${String(e.message).split("\n")[0]}`);
}
for (const [name, file] of [["고유 아이템", "parse-uniques2.mjs"], ["패시브 트리", "parse-tree2.mjs"], ["아틀라스 트리", "parse-atlas2.mjs"]]) {
	if (fs.existsSync(path.join(REPO_DIR, file))) step(name, file);
}
// 그림은 파서가 만든 JSON 에 image 를 붙이므로 맨 끝(처음 실행은 약 2분, 이후엔 바뀐 것만)
step("아이템 그림", "icons2.mjs");
step("트리 노드 아이콘", "tree-sprites2.mjs");
step("아틀라스 노드 아이콘", "atlas-sprites2.mjs");
// 고유 로어(플레이버)·요구 레벨 — 위키(poe2wiki.net) + 게임 FlavourText(영·한) 매칭(wiki-uniques2 → PoE1 과 같은 runWikiUniques).
//   고유 JSON 을 다시 쓰는 단계(parse-uniques2·icons2) 뒤. 네트워크 단계라 비치명 — 실패하면 캐시로, 그것도 없으면 로어 없이 계속.
try {
	const t0 = Date.now();
	console.log("\n=== 고유 로어·요구 레벨(위키) ===");
	execFileSync(process.execPath, [path.join(REPO_DIR, "wiki-uniques2.mjs")], { stdio: "inherit", cwd: REPO_DIR });
	console.log(`(고유 로어·요구 레벨 ${((Date.now() - t0) / 1000).toFixed(1)}초)`);
} catch (e) {
	console.warn(`[poe2] ⚠ 고유 로어·요구 레벨 채우기 실패 — 로어 없이 계속: ${String(e.message).split("\n")[0]}`);
}
// 거래소 스탯 사전 · 지금 리그(빌드 화면 아이템 → 거래소 검색 링크, 10-08) — 네트워크 단계라 비치명(실패하면 기존 파일로).
try {
	console.log("\n=== 거래소 스탯 사전 ===");
	execFileSync(process.execPath, [path.join(REPO_DIR, "trade-stats2.mjs")], { stdio: "inherit", cwd: REPO_DIR });
} catch (e) {
	console.warn(`[poe2] ⚠ 거래소 스탯 사전 갱신 실패 — 기존 파일로 계속: ${String(e.message).split("\n")[0]}`);
}
// 빌드 재계산 엔진(PoB-PoE2) — 상류 최신으로 받아 표준 LuaJIT 용으로 고치고(patch-pob2) 표본을 계산해 본다(verify-engine2).
//   실패해도 데이터는 멀쩡하다(빌드 화면의 재계산만 안 된다) — 경고로 남기고 끝낸다.
try {
	const t0 = Date.now();
	console.log("\n=== 빌드 엔진(PoB-PoE2) 점검 ===");
	if (!engineReady) throw new Error("엔진 준비 단계가 실패했다");
	execFileSync(process.execPath, [path.join(REPO_DIR, "verify-engine2.mjs")], { stdio: "inherit", cwd: REPO_DIR });
	console.log(`(엔진 점검 ${((Date.now() - t0) / 1000).toFixed(1)}초)`);
} catch (e) {
	console.warn(`[poe2] ⚠ 빌드 엔진 단계 실패 — 데이터는 정상, 빌드 재계산만 안 된다: ${String(e.message).split("\n")[0]}`);
}
// poe.ninja 실빌드(시뮬레이터 /poe2/sim) — 아키타입 집계(fetch-ninja-builds2) → 실빌드 출발점(fetch-ninja-start2, 엔진 재계산이라 api-poe 필요).
//   네트워크(ninja 레이트리밋)·엔진 의존이라 비치명 — 실패하면 기존 파일로 계속, 출발점은 끊겨도 다음 갱신이 이어서 채운다.
for (const [name, file] of [["poe.ninja 실빌드 집계", "fetch-ninja-builds2.mjs"], ["poe.ninja 실빌드 출발점", "fetch-ninja-start2.mjs"]]) {
	try {
		const t0 = Date.now();
		console.log(`\n=== ${name} ===`);
		execFileSync(process.execPath, [path.join(REPO_DIR, file)], { stdio: "inherit", cwd: REPO_DIR });
		console.log(`(${name} ${((Date.now() - t0) / 1000).toFixed(1)}초)`);
	} catch (e) {
		console.warn(`[poe2] ⚠ ${name} 실패 — 기존 파일로 계속: ${String(e.message).split("\n")[0]}`);
	}
}
console.log("\n[poe2] 전체 완료");
