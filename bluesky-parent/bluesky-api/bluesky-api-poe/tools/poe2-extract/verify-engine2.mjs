// PoB-PoE2 엔진 점검 — patch-pob2.mjs 뒤에 합성 표본(engine-sample.xml, 워리어/타이탄 — 실제 PoB 코드 아님)을 calc.lua 로 계산해
// @@POB_RESULT@@ 가 나오고 핵심 스탯(생명력·DPS)이 0 보다 큰지 본다. 문법 되돌리기가 하나라도 빠지면 로드 단계에서 죽어 여기서 드러난다.
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { REPO_DIR, WORK_DIR } from "./paths.mjs";

const src = path.join(WORK_DIR, "pob2-src", "src");
const winget = path.join(os.homedir(), "AppData", "Local", "Programs", "LuaJIT", "bin", "luajit.exe");
const luajit = fs.existsSync(winget) ? winget : "luajit";
const calc = path.join(REPO_DIR, "..", "poe-pob", "calc.lua");
const out = execFileSync(luajit, [calc, path.join(REPO_DIR, "engine-sample.xml")], { cwd: src, encoding: "utf8", timeout: 120000 });
const line = out.split(/\r?\n/).find((l) => l.startsWith("@@POB_RESULT@@"));
if (!line) {
	const err = out.split(/\r?\n/).find((l) => l.startsWith("@@POB_ERROR@@")) || out.slice(-400);
	console.error(`[pob2 엔진] 계산 실패: ${err}`);
	process.exit(1);
}
const r = JSON.parse(line.slice("@@POB_RESULT@@".length));
if (!(r.Life > 0) || !(r.CombinedDPS > 0)) {
	console.error(`[pob2 엔진] 수치 이상: Life ${r.Life} · CombinedDPS ${r.CombinedDPS}`);
	process.exit(1);
}
console.log(`[pob2 엔진] 점검 통과 — 표본 Life ${r.Life} · CombinedDPS ${Math.round(r.CombinedDPS)} · Spirit ${r.Spirit} · 스탯 ${Object.keys(r).length}개`);

// PoB 자체 시험(spec/System, 약 900건·1분 45초) — 상류 커밋이 바뀌었을 때만(또는 --spec). 문법 되돌리기가 계산 의미를 바꾸지 않았는지의 근거.
//   09-30 첫 실행: 통과 900 · 실패 2 · 보류 19. 실패 2건은 상류 시험 자체의 결함(TradeQueryRequests — 코드는 launch:DownloadPage 메서드 호출,
//   시험 대역은 일반 함수라 인자가 한 칸 밀린다; 우리가 손대지 않은 파일)이라 알려진 실패로 뺀다.
const KNOWN_UPSTREAM_FAIL = /^TradeQueryRequests > /;
const root = path.join(WORK_DIR, "pob2-src");
const head = execFileSync("git", ["-C", root, "rev-parse", "--short", "HEAD"], { encoding: "utf8" }).trim();
const stamp = path.join(WORK_DIR, "pob2-spec-verified.txt");
const last = fs.existsSync(stamp) ? fs.readFileSync(stamp, "utf8").trim() : "";
if (head !== last || process.argv.includes("--spec")) {
	const specDir = path.join(root, "spec", "System");
	const specs = fs.readdirSync(specDir).filter((f) => f.endsWith("_spec.lua")).map((f) => "../spec/System/" + f);
	const t0 = Date.now();
	let text;
	try {
		text = execFileSync(luajit, [path.join(REPO_DIR, "pob2-spec-runner.lua"), ...specs], { cwd: src, encoding: "utf8", timeout: 600000, maxBuffer: 64 * 1024 * 1024 });
	} catch (e) {
		text = String(e.stdout || "");
	}
	const sum = text.split(/\r?\n/).find((l) => l.startsWith("@@SPEC@@"));
	const fails = text.split(/\r?\n/).filter((l) => l.startsWith("@@FAIL@@")).map((l) => l.slice(8));
	const unexpected = fails.filter((l) => !KNOWN_UPSTREAM_FAIL.test(l));
	console.log(`[pob2 엔진] PoB 자체 시험(${head}): ${sum ? sum.slice(8) : "요약 없음"} · 알려진 상류 실패 ${fails.length - unexpected.length} · 새 실패 ${unexpected.length} (${((Date.now() - t0) / 1000).toFixed(0)}초)`);
	if (!sum || unexpected.length) {
		for (const f of unexpected.slice(0, 10)) console.error("  " + f.slice(0, 300));
		process.exit(1);
	}
	fs.writeFileSync(stamp, head + "\n");
}
