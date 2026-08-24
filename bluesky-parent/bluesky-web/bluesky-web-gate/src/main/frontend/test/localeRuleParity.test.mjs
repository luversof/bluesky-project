// 앱 로케일을 고르는 규칙이 화면 스크립트마다 갈리지 않는지 본다.
//
// 이 파일들은 클래식 <script src> 로 로드되므로 import 로 공유할 수 없다. 그래서 같은 규칙을 옮겨 적는데,
// 옮겨 적은 것은 조용히 갈린다 - 실측 2026-08-23: stock-charts.ts 는 앱 로케일(html lang)을 쓰는데
// compoundSimulator.ts 는 "ko-KR" 고정, stockSimulator.ts 는 undefined(브라우저 로케일)였다.
// ko/en 은 자릿수 구분이 같아 눈에는 안 보이지만, 브라우저가 fr 이면 같은 화면 안에서 숫자 표기가 갈린다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const SRC = resolve(HERE, "../src");
/** 경로 구분자. 소스에 이스케이프를 남기지 않기 위한 것. */
const BACKSLASH = String.fromCharCode(92);

/** 로케일 해석 규칙의 핵심 순서. 공백을 지운 뒤 비교한다. */
const RULE = [
	"document.body?.dataset?.locale||",
	"document.documentElement?.lang||",
	"navigator.language||",
	'"ko-KR"',
].join("");

const FILES = [
	"stock-charts.ts",
	"stock/compoundSimulator.ts",
	"stock/stockSimulator.ts",
];

function normalized(file) {
	return readFileSync(resolve(SRC, file), "utf8").replace(/\s+/g, "");
}

test("로케일 해석 규칙이 세 파일에서 같다", () => {
	const missing = FILES.filter((file) => !normalized(file).includes(RULE));
	assert.deepEqual(
		missing,
		[],
		"앱 로케일 대신 다른 기준을 쓰는 파일이다. stock-charts.ts 의 resolveLocale 과 같은 순서를 쓸 것",
	);
});

/** 주식 화면 스크립트 전체. 새 파일이 생겨도 자동으로 대상이 된다. */
function stockScripts(dir = SRC, out = []) {
	for (const name of readdirSync(dir)) {
		const full = join(dir, name);
		if (statSync(full).isDirectory()) {
			// poe 는 이 앱의 다른 영역이라 여기서 보지 않는다.
			if (name !== "poe") stockScripts(full, out);
		} else if (name.endsWith(".ts")) {
			out.push(full);
		}
	}
	return out;
}

test("브라우저 로케일이나 고정 로케일을 숫자 서식에 쓰지 않는다", () => {
	// 셋만 보던 시절, 같은 파일 안의 fmtAmt 가 "ko-KR" 고정인 것을 놓쳤다(실측 2026-08-23).
	// 그래서 파일 목록을 손으로 적지 않고 훑는다.
	const files = stockScripts();
	assert.ok(files.length >= 10, `스크립트를 거의 찾지 못했다: ${files.length}`);

	const bad = [];
	for (const file of files) {
		const source = readFileSync(file, "utf8").replace(/\s+/g, "");
		const shown = file.replace(SRC, "").split(BACKSLASH).join("/");
		if (source.includes("newIntl.NumberFormat(undefined")) {
			bad.push(`${shown}: Intl.NumberFormat(undefined) = 브라우저 로케일`);
		}
		for (const literal of ['"ko-KR"', '"ko"', '"en-US"', '"en"']) {
			if (source.includes(`newIntl.NumberFormat(${literal}`)) {
				bad.push(`${shown}: Intl.NumberFormat(${literal}) 고정`);
			}
			if (source.includes(`.toLocaleString(${literal}`)) {
				bad.push(`${shown}: toLocaleString(${literal}) 고정`);
			}
		}
	}
	assert.deepEqual(
		bad,
		[],
		"앱 로케일을 무시하는 숫자 서식이다. stock-charts.ts 의 resolveLocale 결과를 넘길 것",
	);
});
