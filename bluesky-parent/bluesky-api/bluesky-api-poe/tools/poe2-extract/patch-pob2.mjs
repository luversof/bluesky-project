// PoB-PoE2 소스(~/.poe-gamedata/poe2/work/pob2-src)를 받아(없으면 얕은 클론, 있으면 fetch+reset) 표준 LuaJIT 으로 돌게 고친다(멱등).
// PoB 는 자체 LuaJIT 포크라 표준 LuaJIT 이 못 읽는 문법 두 가지를 쓴다 — 파일 하나만 걸려도 엔진 전체가 로드 단계에서 죽는다:
//  ① 복합 대입 `x += 1` → PoE1 patch-pob.mjs 의 일반 변환을 POB_SRC 로 그대로 재사용
//  ② 안전 탐색 `a?.b` / `a?.[k]` (PoE2 전용, 09-30 기준 8곳) → `(a and a.b…)` — 뒤따르는 `.c.d` 까지 한 괄호에 넣어 체인 전체를 단락한다
//     (`gem.gemData?.grantedEffect.support` 를 `(gem.gemData and gem.gemData.grantedEffect).support` 로 풀면 nil 에서 터진다).
// 생성 데이터(Data/·TreeData/)는 손대지 않는다. 끝에 남은 `?.` 가 있으면 파일:줄로 알린다(자가 검증).
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { POE1_TOOLS, WORK_DIR } from "./paths.mjs";

// POB2_ROOT: 다른 사본(릴리스 태그 비교 등)을 패치할 때 덮어쓴다
const ROOT = process.env.POB2_ROOT || path.join(WORK_DIR, "pob2-src");
const SRC = path.join(ROOT, "src");
const REPO = "https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2.git";

// 따라가는 판 = **최신 릴리스 태그(v0.x.y)** — dev 브랜치가 아니다. 사용자와 poe.ninja 가 쓰는 PoB 가 릴리스라서,
// dev 로 계산하면 저장값과 어긋났다(09-30 실빌드 6개: dev 는 블러드 메이지 EHP 21,021 → 250,667 등, 릴리스 v0.23.1 은 저장값과 정확히 일치).
// 태그 v2.x 는 PoE1 PoB 에서 물려받은 것이라 거른다(v0.* 만). POB2_REF 로 다른 참조를 강제할 수 있다.
function latestReleaseTag() {
	const out = execFileSync("git", ["ls-remote", "--tags", REPO], { encoding: "utf8", timeout: 60000 });
	const tags = [...out.matchAll(/refs\/tags\/(v0\.\d+\.\d+)$/gm)].map((m) => m[1]);
	const num = (t) => t.slice(1).split(".").map(Number);
	tags.sort((a, b) => { const x = num(a), y = num(b); return x[0] - y[0] || x[1] - y[1] || x[2] - y[2]; });
	if (!tags.length) throw new Error("PoB-PoE2 릴리스 태그를 못 찾았다");
	return tags[tags.length - 1];
}
const wantUpdate = process.argv.includes("--update") || !fs.existsSync(SRC);
const ref = wantUpdate ? process.env.POB2_REF || latestReleaseTag() : null;
if (!fs.existsSync(SRC)) {
	console.log(`[pob2] 클론: ${REPO} (${ref}) → ${ROOT}`);
	execFileSync("git", ["-c", "advice.detachedHead=false", "clone", "--depth", "1", "-q", "--branch", ref, REPO, ROOT], { stdio: "inherit" });
} else if (wantUpdate) {
	// 우리 패치가 입혀진 작업 트리 — 그 판으로 되돌린 뒤 다시 패치한다
	execFileSync("git", ["-C", ROOT, "fetch", "--depth", "1", "-q", "origin", "tag", ref, "--no-tags"], { stdio: "inherit" });
	execFileSync("git", ["-C", ROOT, "reset", "--hard", "-q", ref], { stdio: "inherit" });
}
if (ref) console.log(`[pob2] PoB-PoE2 ${ref}: ${execFileSync("git", ["-C", ROOT, "log", "-1", "--format=%h %cs %s"]).toString().trim()}`);

// ① 복합 대입 — PoE1 스크립트 재사용
execFileSync(process.execPath, [path.join(POE1_TOOLS, "patch-pob.mjs")], { stdio: "inherit", env: { ...process.env, POB_SRC: SRC } });
// PoB 자체 시험(spec/)도 같은 문법을 쓴다 — pob2-spec-runner.lua 로 우리 엔진을 검증할 때 읽히도록 같이 되돌린다(엔진 동작과 무관)
const SPEC = path.join(ROOT, "spec");
if (fs.existsSync(SPEC)) execFileSync(process.execPath, [path.join(POE1_TOOLS, "patch-pob.mjs")], { stdio: ["ignore", "ignore", "inherit"], env: { ...process.env, POB_SRC: SPEC } });
function* allLuaFiles() {
	yield* luaFiles(SRC);
	if (fs.existsSync(SPEC)) yield* luaFiles(SPEC);
}

// ② 안전 탐색
const ID = "[A-Za-z_][A-Za-z0-9_]*";
const BRACKET = "\\[(?:[^\\[\\]]|\\[[^\\[\\]]*\\])*\\]"; // 한 겹 중첩까지: SkillType?.[skillTypeMap[x]]
const SAFE_RE = new RegExp(`(${ID}(?:\\.${ID})*)\\?\\.(${BRACKET}|${ID})((?:\\.${ID}|${BRACKET})*)`, "g");

function* luaFiles(dir) {
	for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
		const full = path.join(dir, entry.name);
		if (entry.isDirectory()) {
			if (entry.name === "TreeData" || entry.name === "Data") continue;
			yield* luaFiles(full);
		} else if (entry.name.endsWith(".lua")) yield full;
	}
}
/** 따옴표 밖 구간에만 fn 적용(문자열 패턴 ":?.*" 같은 것은 건드리지 않는다), 줄끝 주석도 제외 */
function outsideStrings(line, fn) {
	let out = "", buf = "", quote = null;
	for (let i = 0; i < line.length; i++) {
		const c = line[i];
		if (quote) {
			buf += c;
			if (c === "\\") buf += line[++i] ?? "";
			else if (c === quote) {
				out += buf;
				buf = "";
				quote = null;
			}
			continue;
		}
		if (c === "-" && line[i + 1] === "-") return out + fn(buf) + line.slice(i);
		if (c === '"' || c === "'") {
			out += fn(buf);
			buf = c;
			quote = c;
			continue;
		}
		buf += c;
	}
	return out + (quote ? buf : fn(buf));
}
// ①-b 줄 가운데 복합 대입 — `if c == "{" then depth += 1 elseif c == "}" then depth -= 1 end` (PoE1 규칙은 줄 머리만 본다)
const INLINE_RE = new RegExp(`\\b(then|else|do)(\\s+)(${ID}(?:\\.${ID}|${BRACKET})*)\\s*(\\.\\.|[-+*/%^])=(?!=)\\s*(.+?)(?=\\s+(?:elseif|else|end)\\b)`, "g");
let inlineCount = 0;
for (const file of allLuaFiles()) {
	const before = fs.readFileSync(file, "utf8");
	if (!/(\.\.|[-+*/%^])=(?!=)/.test(before)) continue;
	const after = before
		.split("\n")
		.map((line) =>
			outsideStrings(line, (code) =>
				code.replace(INLINE_RE, (m, kw, sp, lv, op, rhs) => {
					inlineCount++;
					return `${kw}${sp}${lv} = ${lv} ${op} (${rhs})`;
				}),
			),
		)
		.join("\n");
	if (after !== before) {
		fs.writeFileSync(file, after);
		console.log(`  줄 가운데 복합 대입 변환: ${path.relative(SRC, file)}`);
	}
}
if (inlineCount) console.log(`[pob2] 줄 가운데 복합 대입: ${inlineCount}곳`);

// ③ `continue` 문(포크 전용, 09-30 기준 4곳) → `goto continue__N` + 감싼 반복문의 `end` 바로 앞에 `::continue__N::`
//    반복문 끝 라벨은 Lua 5.2 규칙상 지역 변수 범위 제한을 받지 않아(블록 끝 라벨) 안전하다. 파일 전체를 토큰으로 훑어
//    문자열·주석을 건너뛰고 for/while…do · function · if · repeat · do · end 짝을 맞춘다.
function desugarContinue(text) {
	const edits = []; // [offset, deleteLen, insert]
	const stack = []; // { kind: "loop"|"loopHead"|"function"|"if"|"do"|"repeat", label?: string }
	let labelSeq = 0;
	let i = 0;
	const n = text.length;
	const longBracket = (at) => {
		const m = /^\[(=*)\[/.exec(text.slice(at, at + 20));
		return m ? m[1].length : -1;
	};
	while (i < n) {
		const c = text[i];
		if (c === "-" && text[i + 1] === "-") {
			const lvl = text[i + 2] === "[" ? longBracket(i + 2) : -1;
			if (lvl >= 0) {
				const close = "]" + "=".repeat(lvl) + "]";
				const e = text.indexOf(close, i + 4 + lvl);
				i = e < 0 ? n : e + close.length;
			} else {
				const e = text.indexOf("\n", i);
				i = e < 0 ? n : e;
			}
			continue;
		}
		if (c === '"' || c === "'") {
			i++;
			while (i < n && text[i] !== c) i += text[i] === "\\" ? 2 : 1;
			i++;
			continue;
		}
		if (c === "[") {
			const lvl = longBracket(i);
			if (lvl >= 0) {
				const close = "]" + "=".repeat(lvl) + "]";
				const e = text.indexOf(close, i + 2 + lvl);
				i = e < 0 ? n : e + close.length;
				continue;
			}
		}
		if (/[A-Za-z_]/.test(c)) {
			let j = i;
			while (j < n && /[A-Za-z0-9_]/.test(text[j])) j++;
			const word = text.slice(i, j);
			// 앞 글자가 . 또는 : 이면 필드 이름(obj.end 같은 건 없지만 obj.continue 대비)
			let p = i - 1;
			while (p >= 0 && /[ \t]/.test(text[p])) p--;
			// `::label:: end` 의 `::` 는 필드 접근이 아니다(두 번째 실행에서 라벨 뒤 end 를 놓쳐 짝이 어긋났다)
			const isField = p >= 0 && ((text[p] === "." && text[p - 1] !== ".") || (text[p] === ":" && text[p - 1] !== ":"));
			if (!isField) {
				if (word === "for" || word === "while") stack.push({ kind: "loopHead" });
				else if (word === "do") {
					const top = stack[stack.length - 1];
					if (top && top.kind === "loopHead") top.kind = "loop";
					else stack.push({ kind: "do" });
				} else if (word === "function") stack.push({ kind: "function" });
				else if (word === "if") stack.push({ kind: "if" });
				else if (word === "repeat") stack.push({ kind: "repeat" });
				else if (word === "until") stack.pop();
				else if (word === "end") {
					const top = stack.pop();
					if (top && top.kind === "loop" && top.label) edits.push([i, 0, `::${top.label}:: `]);
				} else if (word === "continue") {
					// 앞 낱말이 goto 면 이미 라벨 이름(goto continue) — 건너뛴다
					// 이미 표준 문법인 `goto continue` · `::continue::` 라벨은 건너뛴다(상류가 쓰는 곳이 여럿이다)
					const before = text.slice(Math.max(0, i - 6), i);
					if (!/goto\s*$/.test(before) && !/::\s*$/.test(before) && text.slice(j, j + 2) !== "::") {
						let k = stack.length - 1;
						while (k >= 0 && stack[k].kind !== "loop" && stack[k].kind !== "function") k--;
						if (k < 0 || stack[k].kind !== "loop") throw new Error(`continue 가 반복문 밖에 있다 (offset ${i})`);
						stack[k].label = stack[k].label || `continue__${++labelSeq}`;
						edits.push([i, word.length, `goto ${stack[k].label}`]);
					}
				}
			}
			i = j;
			continue;
		}
		i++;
	}
	if (stack.length) throw new Error(`블록 짝이 안 맞는다(남은 ${stack.length})`);
	let out = text;
	for (const [at, del, ins] of edits.sort((a, b) => b[0] - a[0])) out = out.slice(0, at) + ins + out.slice(at + del);
	return { out, count: edits.filter((e) => e[1] > 0).length };
}
let contCount = 0;
for (const file of allLuaFiles()) {
	const before = fs.readFileSync(file, "utf8");
	if (!/\bcontinue\b/.test(before)) continue;
	let res;
	try {
		res = desugarContinue(before);
	} catch (e) {
		// 짝 맞추기 실패 — 그 파일에 **진짜** continue 문이 없으면(주석·이름 속 낱말) 무시, 있으면 알린다
		if (/(^|[\s;)])continue\s*($|[\s;])/m.test(before.replace(/--.*$/gm, ""))) console.warn(`  ⚠ continue 변환 실패: ${path.relative(SRC, file)} — ${e.message}`);
		continue;
	}
	const { out, count: c } = res;
	if (c) {
		fs.writeFileSync(file, out);
		contCount += c;
		console.log(`  continue ${c}곳 변환: ${path.relative(SRC, file)}`);
	}
}
if (contCount) console.log(`[pob2] continue → goto: ${contCount}곳`);

// ④ 짧은 함수 `|a, b| -> expr` (포크 전용, 09-30 기준 1곳) → `function(a, b) return expr end`
//    본문은 깊이 0 의 `,` `}` `)` `]` 또는 줄 끝까지.
const LAMBDA_HEAD = /\|((?:\s*[A-Za-z_][A-Za-z0-9_]*\s*(?:,\s*[A-Za-z_][A-Za-z0-9_]*\s*)*)?)\|\s*->\s*/g;
function desugarLambda(code) {
	let out = "";
	let last = 0;
	LAMBDA_HEAD.lastIndex = 0;
	let m;
	while ((m = LAMBDA_HEAD.exec(code))) {
		let j = LAMBDA_HEAD.lastIndex, depth = 0;
		for (; j < code.length; j++) {
			const ch = code[j];
			if ("([{".includes(ch)) depth++;
			else if (")]}".includes(ch)) {
				if (depth === 0) break;
				depth--;
			} else if (ch === "," && depth === 0) break;
		}
		const body = code.slice(LAMBDA_HEAD.lastIndex, j).trimEnd();
		out += code.slice(last, m.index) + `function(${m[1].trim()}) return ${body} end` + code.slice(LAMBDA_HEAD.lastIndex + body.length, j);
		last = j;
		LAMBDA_HEAD.lastIndex = j;
	}
	return out + code.slice(last);
}
let lambdaCount = 0;
for (const file of allLuaFiles()) {
	const before = fs.readFileSync(file, "utf8");
	if (!/\|\s*->/.test(before)) continue;
	const after = before
		.split("\n")
		.map((line) => outsideStrings(line, (code) => (/\|\s*->/.test(code) ? (lambdaCount++, desugarLambda(code)) : code)))
		.join("\n");
	if (after !== before) {
		fs.writeFileSync(file, after);
		console.log(`  짧은 함수 변환: ${path.relative(SRC, file)}`);
	}
}
if (lambdaCount) console.log(`[pob2] 짧은 함수 → function: ${lambdaCount}줄`);

let files = 0, count = 0;
const left = [];
for (const file of allLuaFiles()) {
	const before = fs.readFileSync(file, "utf8");
	if (!before.includes("?.")) continue;
	let changed = 0;
	const after = before
		.split("\n")
		.map((line) =>
			outsideStrings(line, (code) =>
				code.replace(SAFE_RE, (m, obj, first, rest) => {
					changed++;
					const acc = first.startsWith("[") ? first : "." + first;
					return `(${obj} and ${obj}${acc}${rest})`;
				}),
			),
		)
		.join("\n");
	if (changed) {
		fs.writeFileSync(file, after);
		files++;
		count += changed;
		console.log(`  안전 탐색 ${changed}곳 변환: ${path.relative(SRC, file)}`);
	}
	after.split("\n").forEach((l, i) => {
		if (outsideStrings(l, (code) => (/\?\./.test(code) ? "\u0000" : "")).includes("\u0000")) left.push(`${path.relative(SRC, file)}:${i + 1}: ${l.trim()}`);
	});
}
console.log(count ? `[pob2] 안전 탐색 되돌리기: ${count}곳 / ${files}파일` : "[pob2] 안전 탐색 되돌리기: 대상 없음");
if (left.length) {
	console.warn(`  ⚠ 안전 탐색 ${left.length}곳이 남았습니다 — 엔진이 로드 단계에서 죽습니다:`);
	for (const l of left.slice(0, 5)) console.warn("    " + l);
	process.exitCode = 1;
}
