// 기간 입력칸의 min/max 는 달력 힌트다 - 프리셋이 넣은 기간을 폼 검증이 막으면 안 된다.
//
// 실측 2026-09-17: 데이터가 1 년이 안 되는 종목(첫 거래 2026-05-07) 상세에서 '1년' 을 누르면 단추는 눌린 모양이 되는데
// 요청이 하나도 안 나갔다. 시작일 2025-09-18 이 min(2026-05-07) 보다 앞서 "값은 2026-05-07 이후여야 합니다." 로
// requestSubmit 이 막혔다(htmx 도 form.noValidate 를 보고 같은 검증을 한다). 원장 기준 종목 44 개 중 '3년' 12 · '1년' 7 ·
// '올해' 6 · '6개월' 4, 계좌 6 개 중 '3년' 4 · '1년' 1 이 이 상태였다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

globalThis.window = globalThis.window ?? {};
globalThis.window.addEventListener = globalThis.window.addEventListener ?? (() => {});
globalThis.document = globalThis.document ?? {
	getElementById: () => null,
	querySelectorAll: () => [],
	addEventListener: () => {},
};
globalThis.localStorage = globalThis.localStorage ?? { getItem: () => null };

await import("../../resources/static/js/date-range-picker.js");
const relax = globalThis.__dateRangePickerInternals.relaxRangeFormValidation;

test("칸이 든 폼의 검증을 끈다", () => {
	const form = { noValidate: false };
	relax({ form });
	assert.equal(form.noValidate, true);
});

test("폼 밖의 칸이나 빈 값에도 멈추지 않는다", () => {
	assert.doesNotThrow(() => relax({ form: null }));
	assert.doesNotThrow(() => relax(null));
});

// 도우미가 있어도 min 을 거는 자리에서 안 부르면 결함은 그대로다 - 그 자리가 값을 감싸는지 본다.
test("산출물: min/max 를 거는 바로 그 자리에서 검증을 끈다", () => {
	const built = readFileSync(new URL("../../resources/static/js/date-range-picker.js", import.meta.url), "utf8");
	const at = built.indexOf(".min=cfg.minDate");
	assert.ok(at >= 0, "산출물에서 min 을 거는 자리를 못 찾았다");
	// 그 자리를 감싸는 forEach 콜백의 몸통(여는 { 부터 짝이 맞는 } 까지)을 잘라 본다.
	const open = built.lastIndexOf("=>{", at);
	assert.ok(open >= 0 && at - open < 400, "min 을 거는 콜백의 시작을 못 찾았다");
	let depth = 0;
	let end = -1;
	for (let i = open + 2; i < built.length; i++) {
		if (built[i] === "{") depth++;
		else if (built[i] === "}") {
			depth--;
			if (depth === 0) {
				end = i;
				break;
			}
		}
	}
	const body = built.slice(open, end + 1);
	assert.match(body, /\.min=cfg\.minDate/);
	assert.match(body, /relaxRangeFormValidation\(/, "min 을 거는 콜백이 검증을 끄지 않는다: " + body.slice(0, 160));
});
