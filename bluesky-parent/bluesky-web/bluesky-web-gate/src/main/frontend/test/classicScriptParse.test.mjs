// classic <script src> 로 로드되는 산출물이 '스크립트'로 파싱되는지 본다.
//
// 왜 필요한가(실측 사고): 검증하려고 계산 함수에 export 를 붙였더니 tsc 가 그 파일을 ES 모듈로 만들었다.
// 그런데 화면은 type="module" 없이 <script src> 로 로드하므로, 브라우저는
// "Unexpected token 'export'" 로 파일 전체를 거부한다 - 시뮬레이터 페이지 스크립트가 통째로 죽는다.
// 컴파일도 테스트도 통과하고 서버 로그에도 안 남는다. 로그인 뒤 화면이라 눈에 띄기까지 오래 걸린다.
//
// 템플릿에서 <script src="/js/..."> 를 긁어 type="module" 이 없는 것만 골라, 그 산출물을
// vm.Script 로 컴파일한다. 모듈 문법이 있으면 여기서 깨진다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync, existsSync, readdirSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

// 경로는 cwd 가 아니라 이 파일 위치에서 잡는다 - 예전엔 resolve("../jte") 라 src/main/frontend 에서
// 실행할 때만 돌았고, 모듈 루트에서 돌리면 ENOENT 로 죽었다(다른 10개 파일은 어디서든 돈다).
const MAIN = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const JTE = join(MAIN, "jte");
const STATIC = join(MAIN, "resources/static");

function walk(dir, out = []) {
	for (const name of readdirSync(dir)) {
		const p = join(dir, name);
		if (statSync(p).isDirectory()) walk(p, out);
		else if (name.endsWith(".jte")) out.push(p);
	}
	return out;
}

const SCRIPT_TAG = /<script\b([^>]*)\bsrc\s*=\s*"([^"]+)"([^>]*)>/g;

function classicScriptSources() {
	const found = new Map();
	for (const file of walk(JTE)) {
		const html = readFileSync(file, "utf8");
		for (const m of html.matchAll(SCRIPT_TAG)) {
			const attrs = (m[1] || "") + (m[3] || "");
			const src = m[2];
			if (/type\s*=\s*"module"/.test(attrs)) continue;
			if (!src.startsWith("/js/") || src.includes("/vendor/")) continue;
			if (!found.has(src)) found.set(src, file);
		}
	}
	return found;
}

test("classic script 로 로드되는 산출물에 모듈 문법이 없다", () => {
	const sources = classicScriptSources();
	// 스캔이 조용히 0건이 되면 검사가 무력해진다.
	assert.ok(sources.size >= 5, `script 태그를 찾지 못했다: ${sources.size}`);

	const broken = [];
	for (const [src, from] of sources) {
		const path = join(STATIC, src.replace(/^\//, ""));
		if (!existsSync(path)) {
			broken.push(`${src} — 산출물이 없다 (${from})`);
			continue;
		}
		try {
			new vm.Script(readFileSync(path, "utf8"), { filename: path });
		} catch (e) {
			broken.push(`${src} — ${e.message} (${from})`);
		}
	}
	assert.deepEqual(
		broken,
		[],
		"type=\"module\" 없이 로드되는데 모듈 문법이 들어 있다. export/import 를 빼거나 script 태그에 type=\"module\" 을 붙일 것",
	);
});
