// 데이터 갱신 마지막 단계 — "시뮬레이터가 실제로 돌 수 있는 상태인가"를 갱신 시점에 확인한다.
//
// 여기서 안 잡으면 사용자는 나중에 시뮬을 돌리다 `스펙 임포트 실패(클래스 3 → 0)` 로 처음 알게 되고,
// 원인(PoB 소스가 옛 버전 / 타임리스 .bin stale)은 화면 어디에도 안 나온다. 실제로 다른 PC 에서 그 사고가 났다.
//
// 점검 항목은 **실제로 그 증상을 낸 적이 있는 것**만 둔다:
//   1) 앱이 쓰는 트리 버전의 PoB TreeData 가 있는가 — 없으면 스펙이 기본 클래스(사이온)로 떨어진다
//   2) 타임리스 주얼 .bin 이 원본 .zip 보다 최신인가 — stale 이면 LUT 가 어긋나 스펙 임포트가 중단된다
//   3) 상류 PoB nil 버그 패치가 입혀졌는가 — 안 입히면 일부 빌드가 조용히 빈 빌드 수치를 낸다
//
// 문제를 찾으면 **비정상 종료**해 run-all 이 실패로 표시하게 한다(조용히 넘어가면 점검하는 의미가 없다).
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { DATA_DIR, REPO_DIR, WORK_DIR, findLuaJit } from "./paths.mjs";

const POB = path.join(WORK_DIR, "pob-src", "src");
const problems = [];
const notes = [];

// 앱이 쓰는 트리 버전 — Java 기본값(poe.sim.tree-version)과 같아야 한다.
// 서비스마다 기본값이 달라(최적화기 3_29 / 시뮬 3_28) **둘 다** 있어야 한다.
const TREE_VERSIONS = ["3_28", "3_29"];

if (!fs.existsSync(POB)) {
	console.log("PoB 소스 없음 — 빌드 재계산/시뮬레이터 비활성(데이터 갱신 자체는 정상)");
	process.exit(0);
}

// 1) 트리 데이터
for (const v of TREE_VERSIONS) {
	const dir = path.join(POB, "TreeData", v);
	if (fs.existsSync(dir)) {
		notes.push(`TreeData/${v} 있음`);
	} else {
		problems.push(`PoB 소스에 TreeData/${v} 없음 — 스펙 임포트가 기본 클래스로 떨어집니다(소스 갱신 실패?)`);
	}
}

// 2) 타임리스 .bin 신선도
const tj = path.join(POB, "Data", "TimelessJewelData");
if (fs.existsSync(tj)) {
	// ⚠ 폴더의 .zip 을 전부 요구하면 안 된다 — 추출 대상이 아닌 것이 섞이면 "N개 없음" 오탐이 난다(첫 구현에서 실제로 그랬다).
	//    **timeless-bin.mjs 와 같은 목록**만 본다. Abyss 5종은 10-05 C166 부터 대상(러너의 Inflate 대체가 이 .bin 을 쓴다 — 없으면
	//    "Invalid Abyss timeless jewel header" 로 그 주얼 빌드의 스펙 임포트가 실패).
	const NAMES = ["BrutalRestraint", "LethalPride", "MilitantFaith", "ElegantHubris", "HeroicTragedy", "GloriousVanity", "AbyssAmanamu", "AbyssKurgal", "AbyssTecrod", "AbyssUlaman", "AbyssZorath"];
	let stale = 0;
	let missing = 0;
	let checked = 0;
	for (const name of NAMES) {
		// 원본은 통짜 .zip 이거나 분할(.zip.part0…)이다 — 찬란한 허영심이 분할 형태다
		const sources = [path.join(tj, `${name}.zip`)].filter(fs.existsSync);
		if (sources.length === 0) {
			sources.push(
				...fs.readdirSync(tj).filter((f) => f.startsWith(`${name}.zip.part`)).map((f) => path.join(tj, f)),
			);
		}
		if (sources.length === 0) continue; // 이 소스 버전엔 없는 주얼 — 검사 대상 아님
		checked++;
		const bin = path.join(tj, `${name}.bin`);
		const newest = Math.max(...sources.map((f) => fs.statSync(f).mtimeMs));
		if (!fs.existsSync(bin) || fs.statSync(bin).size === 0) {
			missing++;
		} else if (fs.statSync(bin).mtimeMs < newest) {
			stale++;
		}
	}
	if (missing) problems.push(`타임리스 .bin ${missing}개 없음 — timeless-bin.mjs 가 실패했습니다`);
	if (stale) problems.push(`타임리스 .bin ${stale}개가 .zip 보다 오래됨(stale) — LUT 어긋남으로 스펙 임포트가 깨집니다`);
	if (!missing && !stale) notes.push(`타임리스 .bin ${checked}개 최신`);
}

// 3) 상류 버그 패치 — patch-pob.mjs 가 고치는 지점이 실제로 고쳐졌는지 본문으로 확인
const specFile = path.join(POB, "Classes", "PassiveSpec.lua");
if (fs.existsSync(specFile)) {
	const src = fs.readFileSync(specFile, "utf8");
	// 패치 전 원본은 nil 검사 없이 item 을 역참조한다. 패치본은 item 존재를 먼저 본다.
	const patched = /if\s+item\s+and\s+item\./.test(src) || /item\s*==\s*nil/.test(src);
	if (patched) notes.push("PoB nil 버그 패치 적용됨");
	else problems.push("PoB nil 버그 패치가 안 보임 — patch-pob.mjs 실패(일부 빌드가 빈 수치를 냅니다)");
}

// 4) Lua 구문 점검 — **표준 LuaJIT 이 이 소스를 읽을 수 있는가**
//    상류 PoB 는 복합 대입(`x += 1`)을 지원하는 자체 LuaJIT 포크로 돌지만 우리는 표준 LuaJIT 을 쓴다.
//    못 읽는 문법이 하나라도 들어오면 그 파일을 로드하는 순간 엔진이 통째로 죽는다:
//      PLoadModule() error loading 'Modules/Main.lua': Modules/Main.lua:342: '=' expected near '+'
//    실제로 상류가 Main.lua 에 `count += 1` 을 넣은 뒤 다른 PC 의 시뮬이 **모든 빌드에서** 이렇게 죽었다.
//    patch-pob.mjs 가 되돌리지만, 놓친 경우를 여기서 잡아야 사용자가 시뮬을 돌리다 처음 알게 되지 않는다.
//    생성 데이터(TreeData/·Data/)는 제외 — 손으로 쓴 코드가 아니고 5MB 짜리라 점검만 느려진다.
const luajit = findLuaJit();
if (!luajit) {
	notes.push("luajit 없음 — Lua 구문 점검 건너뜀(엔진도 못 돕니다)");
} else {
	const targets = [];
	const walk = (dir) => {
		for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
			const full = path.join(dir, entry.name);
			if (entry.isDirectory()) {
				if (entry.name !== "TreeData" && entry.name !== "Data") walk(full);
			} else if (entry.name.endsWith(".lua")) {
				targets.push(full);
			}
		}
	};
	walk(POB);
	// 파일당 프로세스를 띄우면 181개에 20초가 넘는다(Windows spawn 비용) → 목록을 넘겨 **한 번만** 띄운다.
	const listFile = path.join(os.tmpdir(), "pob-lua-files.txt");
	const checker = path.join(os.tmpdir(), "pob-syntax-check.lua");
	fs.writeFileSync(listFile, targets.join("\n"));
	fs.writeFileSync(
		checker,
		[
			"local list = ...",
			"local bad = 0",
			"for line in io.lines(list) do",
			"  if #line > 0 then",
			"    local fn, err = loadfile(line)",
			"    if not fn then bad = bad + 1 print(err) if bad >= 5 then break end end",
			"  end",
			"end",
			"os.exit(bad == 0 and 0 or 1)",
		].join("\n"),
	);
	try {
		execFileSync(luajit, [checker, listFile], { encoding: "utf8", stdio: "pipe" });
		notes.push(`Lua 구문 점검 통과(${targets.length}파일)`);
	} catch (e) {
		const detail = String(e.stdout || e.message).trim().split("\n").slice(0, 5).join(" / ");
		problems.push(`PoB 소스에 표준 LuaJIT 이 못 읽는 문법이 있습니다 — 엔진이 로드 단계에서 죽습니다: ${detail}`);
	}
}

// 5) 화면 트리와 엔진 트리가 같은 시즌인가(2026-10-29) — 화면 · 최적화기 그래프는 GGG 공식 트리(passive-tree.json)를 쓰고,
//    계산은 PoB 의 TreeData/<poe.sim.tree-version> 으로 한다. GGG 는 시즌 시작에 바로 트리를 올리지만 PoB 새 폴더는 며칠 늦고,
//    렐리쿼리언 선택지는 시즌마다 **노드 번호는 그대로 두고 이름 · 효과만** 바꾼다(3.28→3.29 23개 전부: 35448 Victario's Influence → Kaom's Roots).
//    그 사이에 갱신하면 화면은 새 시즌 이름을 보이는데 엔진은 같은 번호를 옛 효과로 계산한다 — 오류 없이. 그래서 이름을 번호별로 대조한다.
//    (기준선 10-29: 3_29 와 0건 · 3_28 과 44건 — 전직 25 · 선택지 23)
const appTreeFile = path.join(DATA_DIR, "passive-tree.json");
const optimizerSource = path.join(REPO_DIR, "..", "..", "src", "main", "java", "net", "luversof", "api", "poe", "service", "PoeOptimizeService.java");
const engineVersion = fs.existsSync(optimizerSource)
	? (fs.readFileSync(optimizerSource, "utf8").match(/poe\.sim\.tree-version:([0-9_]+)/) || [])[1]
	: TREE_VERSIONS[TREE_VERSIONS.length - 1];
const engineTreeFile = engineVersion && path.join(POB, "TreeData", engineVersion, "tree.lua");
if (!engineVersion) {
	problems.push("PoeOptimizeService 에서 poe.sim.tree-version 기본값을 못 찾았습니다 — 트리 시즌 대조를 못 합니다");
} else if (fs.existsSync(appTreeFile) && fs.existsSync(engineTreeFile)) {
	const pobNames = (lua) => {
		const names = new Map();
		for (const m of lua.matchAll(/\["skill"\]= (\d+),\s*\["name"\]= "((?:[^"\\]|\\.)*)"/g)) names.set(m[1], m[2].replace(/\\"/g, '"'));
		return names;
	};
	const tree = JSON.parse(fs.readFileSync(appTreeFile, "utf8"));
	const appNodes = (Array.isArray(tree.nodes) ? tree.nodes : Object.values(tree.nodes)).filter((n) => n.name);
	const countMismatch = (names) => appNodes.filter((n) => names.get(String(n.id)) !== n.name);
	const mismatched = countMismatch(pobNames(fs.readFileSync(engineTreeFile, "utf8")));
	if (mismatched.length === 0) {
		notes.push(`화면 트리 = 엔진 트리 ${engineVersion}(노드 ${appNodes.length}개 이름 일치)`);
	} else {
		// 어느 PoB 폴더가 화면 트리와 맞는지 찾아 알려 준다 — 보통 새 시즌 폴더가 생겼는데 기본값을 안 올린 경우다
		const match = fs
			.readdirSync(path.join(POB, "TreeData"))
			.filter((v) => /^\d+_\d+$/.test(v) && v !== engineVersion && fs.existsSync(path.join(POB, "TreeData", v, "tree.lua")))
			.find((v) => countMismatch(pobNames(fs.readFileSync(path.join(POB, "TreeData", v, "tree.lua"), "utf8"))).length === 0);
		const choice = mismatched.filter((n) => n.multipleChoiceOption).length;
		const sample = mismatched.slice(0, 3).map((n) => `${n.id} ${n.name}`).join(", ");
		problems.push(
			`화면 트리와 엔진 트리 ${engineVersion} 의 노드 ${mismatched.length}개가 다릅니다(전직 선택지 ${choice}개, 예: ${sample}) — ` +
				(match
					? `PoB TreeData/${match} 가 화면 트리와 맞습니다. poe.sim.tree-version 기본값(PoeOptimizeService · PoeSimService)을 ${match} 로 올리세요`
					: "PoB 가 아직 이 시즌 트리를 내지 않았습니다. PoB 갱신 뒤 다시 돌리세요(그 전 계산은 옛 시즌 효과입니다)"),
		);
	}
}

for (const n of notes) console.log("  ok  " + n);
if (problems.length === 0) {
	console.log("엔진 점검 통과 — 시뮬레이터 사용 가능");
	process.exit(0);
}
console.error("");
for (const p of problems) console.error("  ✗ " + p);
console.error("\n조치: 네트워크를 확인하고 데이터 갱신을 한 번 더 실행하세요(소스 갱신·패치·.bin 추출이 이 순서로 다시 돕니다).");
process.exit(1);
