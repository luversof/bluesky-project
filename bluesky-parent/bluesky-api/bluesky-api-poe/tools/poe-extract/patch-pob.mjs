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

// ─────────────────────────────────────────────────────────────────────────────
// 복합 대입 되돌리기 (`x += 1` → `x = x + (1)`)
//
// PoB 는 **자체 LuaJIT 포크**(복합 대입을 지원하도록 확장한 빌드)로 돌지만, 우리는 표준 LuaJIT 을 쓴다.
// 표준 LuaJIT 은 이 문법을 못 읽고 **파일 파싱 단계에서** 죽는다 → 그 파일을 로드하는 엔진이 통째로 실패한다:
//     PLoadModule() error loading 'Modules/Main.lua': Modules/Main.lua:342: '=' expected near '+'
// 실제로 상류가 Main.lua 에 `count += 1` 을 넣은 뒤 다른 PC 의 시뮬이 **모든 빌드에서** 이렇게 죽었다.
// 한 줄짜리 고정 패치로 두면 상류가 다음에 다른 파일에 쓰는 순간 같은 사고가 반복되므로 **전 파일 일반 변환**으로 둔다.
//
// 생성 데이터(TreeData/·Data/)는 손대지 않는다 — 손으로 쓴 코드가 아니고 용량만 크다.
const COMPOUND_RE =
	/^(\s*)((?:[A-Za-z_][A-Za-z0-9_]*)(?:\s*(?:\.[A-Za-z_][A-Za-z0-9_]*|\[[^[\]]*\]))*)\s*(\.\.|[-+*/%^])=(?!=)\s*(\S.*)$/;

/** 코드와 줄끝 주석을 나눈다(따옴표 안의 `--` 는 주석이 아니다). */
function splitTrailingComment(code) {
	let quote = null;
	for (let i = 0; i < code.length; i++) {
		const c = code[i];
		if (quote) {
			if (c === "\\") i++;
			else if (c === quote) quote = null;
			continue;
		}
		if (c === '"' || c === "'") quote = c;
		else if (c === "-" && code[i + 1] === "-") return [code.slice(0, i), code.slice(i)];
	}
	return [code, ""];
}

function* luaFiles(dir) {
	for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
		const full = path.join(dir, entry.name);
		if (entry.isDirectory()) {
			if (entry.name === "TreeData" || entry.name === "Data") continue;
			yield* luaFiles(full);
		} else if (entry.name.endsWith(".lua")) {
			yield full;
		}
	}
}

/** ⚠ 클론본은 CRLF 다 — `split("\n")` 뒤 줄 끝에 남는 CR 을 반드시 떼고 정규식을 돌린다.
 *  JS 정규식에서 `.` 은 CR 을 매치하지 않아 `(\S.*)$` 가 `1<CR>` 에서 실패한다.
 *  이걸 놓쳐서 LF 사본으로 한 검증은 통과했는데 **실제 갱신 파이프라인에선 조용히 0건**을 하고
 *  엔진이 깨진 채 남았다(2026-08-31 실사고). 줄 끝 CR 은 변환 후 그대로 돌려놓는다. */
function stripCr(raw) {
	return raw.endsWith("\r") ? [raw.slice(0, -1), "\r"] : [raw, ""];
}

let desugarFiles = 0;
let desugarLines = 0;
for (const file of luaFiles(SRC)) {
	const before = fs.readFileSync(file, "utf8");
	if (!/(\.\.|[-+*/%^])=(?!=)/.test(before)) continue; // 후보 없음 — 빠른 탈출
	let changed = 0;
	const after = before
		.split("\n")
		.map((raw) => {
			const [line, cr] = stripCr(raw);
			const m = COMPOUND_RE.exec(line);
			if (!m) return raw;
			const [, indent, lvalue, op, rest] = m;
			const [expr, comment] = splitTrailingComment(rest);
			const body = expr.trim();
			if (!body) return raw; // `x +=` 만 있는 줄은 어차피 문법 오류 — 손대지 않는다
			changed++;
			// RHS 는 반드시 괄호로 묶는다 — `x *= a + b` 를 `x = x * a + b` 로 풀면 결과가 달라진다.
			return `${indent}${lvalue} = ${lvalue} ${op} (${body})${comment ? ` ${comment.trim()}` : ""}${cr}`;
		})
		.join("\n");
	if (changed) {
		fs.writeFileSync(file, after);
		desugarFiles++;
		desugarLines += changed;
		console.log(`  복합 대입 ${changed}줄 변환: ${path.relative(SRC, file)}`);
	}
}
console.log(
	desugarLines
		? `복합 대입 되돌리기: ${desugarLines}줄 / ${desugarFiles}파일 (표준 LuaJIT 이 못 읽는 문법)`
		: "복합 대입 되돌리기: 대상 없음",
);

// 자기 검증 — 변환이 **정말** 끝났는지 여기서 확인한다.
//   첫 구현은 CRLF 를 못 넘겨 0건을 하고도 "대상 없음"이라고 성공처럼 보고했고, 그 상태로 파이프라인이
//   끝나 엔진이 깨진 채 남았다. 놓친 게 있으면 **패치 단계에서** 파일:줄로 드러나게 한다.
const leftovers = [];
for (const file of luaFiles(SRC)) {
	const lines = fs.readFileSync(file, "utf8").split("\n");
	for (let i = 0; i < lines.length; i++) {
		const [line] = stripCr(lines[i]);
		if (COMPOUND_RE.test(line)) {
			leftovers.push(`${path.relative(SRC, file)}:${i + 1}: ${line.trim()}`);
		}
	}
}
if (leftovers.length) {
	console.warn(`  ⚠ 복합 대입 ${leftovers.length}줄이 남았습니다 — 엔진이 로드 단계에서 죽습니다:`);
	for (const l of leftovers.slice(0, 5)) console.warn("    " + l);
}
