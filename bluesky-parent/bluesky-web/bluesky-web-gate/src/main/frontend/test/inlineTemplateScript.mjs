// JTE 템플릿에 인라인으로 박힌 함수를 그대로 꺼내 가짜 DOM 위에서 돌린다.
//
// 이 스크립트들은 브라우저 없이는 실행할 수 없어 오래 검사 밖에 있었다. 그 사이 실제 결함이 있었다 -
// 배당 수익률 표의 선택 합계가 같은 표의 행과 다른 분자를 쓰고 있었다(2026-08-24 확인·수정).
// 문자열을 찾는 검사로는 "식을 다르게 고쳐 적은 경우"를 못 잡으므로 실행해서 결과로 본다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

export const JTE_ROOT = join(
	resolve(dirname(fileURLToPath(import.meta.url)), "../.."),
	"jte",
);

/** 이름 붙은 함수 하나를 중괄호 짝을 맞춰 잘라 낸다. */
export function extractFunction(source, name) {
	const start = source.indexOf("function " + name);
	assert.ok(start >= 0, name + " 을 템플릿에서 찾지 못했다 - 검사가 무력해진다");
	let at = source.indexOf("{", start);
	let depth = 1;
	while (depth > 0) {
		at++;
		const c = source[at];
		if (c === undefined) throw new Error(name + " 의 중괄호 짝이 맞지 않는다");
		if (c === "{") depth++;
		else if (c === "}") depth--;
	}
	return source.slice(start, at + 1);
}

function stubClassList() {
	return { toggle() {}, add() {}, remove() {} };
}

/**
 * 선택 합계 갱신 함수를 돌리고, 화면에 쓴 문자열을 속성 이름별로 돌려준다.
 *
 * @param templatePath JTE 경로(jte 루트 기준)
 * @param names        꺼낼 함수 이름들. 마지막 것이 호출 대상이다(앞의 것들은 그 함수가 쓰는 포매터).
 * @param rows         선택된 행의 dataset
 * @param summaryData  요약 영역의 dataset
 * @param extras       샌드박스에 미리 넣을 값(로케일 헬퍼 등)
 */
export function runSelectionSummary({
	templatePath,
	names,
	rows,
	summaryData,
	extras = {},
}) {
	const source = readFileSync(join(JTE_ROOT, templatePath), "utf8");
	const written = {};
	const label = (key) => ({
		classList: stubClassList(),
		set textContent(value) {
			written[key] = value;
		},
	});
	const summary = {
		dataset: summaryData,
		classList: stubClassList(),
		closest: () => null,
		querySelector(selector) {
			const m = /\[(data-[a-z-]+)\]/.exec(selector);
			return m ? label(m[1]) : null;
		},
	};
	const table = {
		tBodies: [{ querySelectorAll: () => rows.map((dataset) => ({ dataset })) }],
		querySelectorAll: () => rows.map((dataset) => ({ dataset })),
	};
	const section = {
		querySelector(selector) {
			if (selector.includes("selection-summary")) return summary;
			if (selector.includes("table")) return table;
			return null;
		},
	};

	const sandbox = { Intl, Math, Number, String, ...extras };
	vm.createContext(sandbox);
	const bodies = names.map((name) => extractFunction(source, name)).join("\n");
	vm.runInContext(bodies + ";this.__run = " + names[names.length - 1] + ";", sandbox);
	sandbox.__run(section);
	return written;
}
