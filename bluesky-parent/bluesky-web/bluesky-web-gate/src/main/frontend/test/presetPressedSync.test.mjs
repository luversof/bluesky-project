// 스왑이 끝난 뒤 프리셋 버튼의 눌린 표시를 "서버가 실제로 적용한 기간"으로 되돌리는 규칙.
//
// 프리셋 버튼은 클릭 즉시 눌린 표시를 바꾼다(낙관적). 그런데 앞 요청이 진행 중이면 뒤 제출이
// 버려져서, 화면이 "이번달"이라고 말하면서 3년 데이터를 보여 준다
// (실측 2026-09-13, 매매 3년→이번달 연속 클릭 12회 중 6회). 그래서 조각이 실어 온
// data-applied-range-mode 로 표시를 되돌린다.
//
// 버튼은 data-picker-arg 로 자기 모드를 들고 있고, 서버 값과 그대로 대응한다.
// 단 하나 '전체'만 서버 all ↔ 버튼 0 이다(실측: mtd·1·3·6·ytd·12·36 은 같고 all 만 0).
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

// 산출물은 classic script 라 import 로는 프로세스당 한 번만 실행된다(CommonJS 캐시).
// 필요한 함수만 글자로 떼어 평가한다.
const SOURCE = readFileSync(
	new URL("../../resources/static/js/stock/globalDateRange.js", import.meta.url),
	"utf8",
);

function loadFn() {
	const at = SOURCE.indexOf("function pressedTargetsForMode");
	assert.ok(at >= 0, "산출물에 pressedTargetsForMode 가 있어야 한다");
	// 함수 본문 끝(짝 맞는 중괄호)까지 잘라 낸다.
	let depth = 0;
	let end = -1;
	for (let i = SOURCE.indexOf("{", at); i < SOURCE.length; i++) {
		if (SOURCE[i] === "{") depth++;
		else if (SOURCE[i] === "}") {
			depth--;
			if (depth === 0) {
				end = i + 1;
				break;
			}
		}
	}
	assert.ok(end > at, "함수 본문을 찾지 못했다");
	const src = SOURCE.slice(at, end);
	return new Function(src + "; return pressedTargetsForMode;")();
}

const ARGS = ["mtd", "1", "3", "6", "ytd", "12", "36", "0"];
const pressedTargetsForMode = loadFn();
const pressedName = (mode) => {
	const flags = pressedTargetsForMode(mode, ARGS);
	return ARGS.filter((_, i) => flags[i]);
};

test("서버 모드와 같은 버튼 하나만 눌린다", () => {
	assert.deepEqual(pressedName("12"), ["12"]);
	assert.deepEqual(pressedName("36"), ["36"]);
	assert.deepEqual(pressedName("mtd"), ["mtd"]);
	assert.deepEqual(pressedName("ytd"), ["ytd"]);
	assert.deepEqual(pressedName("1"), ["1"]);
});

test("'전체'만 서버 all 에 버튼 0 으로 대응한다", () => {
	assert.deepEqual(pressedName("all"), ["0"]);
	// 반대로 서버가 0 을 보내는 일은 없지만, 와도 0 버튼이 눌린다(문자 일치).
	assert.deepEqual(pressedName("0"), ["0"]);
});

test("사용자 지정 기간(빈 모드)이면 아무 버튼도 안 눌린다", () => {
	assert.deepEqual(pressedName(""), []);
});

test("모르는 모드면 아무 버튼도 안 눌린다 - 엉뚱한 버튼을 켜지 않는다", () => {
	assert.deepEqual(pressedName("BOGUS"), []);
	assert.deepEqual(pressedName("120"), []);
});

test("data-picker-arg 가 없는 버튼은 빈 모드에서도 켜지지 않는다", () => {
	// getAttribute 가 없으면 "" 가 되므로, 빈 모드와 문자로 같아져 버린다.
	const args = ["mtd", "12", ""];
	assert.deepEqual(pressedTargetsForMode("", args), [false, false, false]);
	assert.deepEqual(pressedTargetsForMode("12", args), [false, true, false]);
});

test("눌린 버튼은 언제나 최대 하나다", () => {
	for (const mode of ["mtd", "1", "3", "6", "ytd", "12", "36", "all", "", "BOGUS"]) {
		assert.ok(
			pressedName(mode).length <= 1,
			mode + " 에서 " + pressedName(mode).length + " 개가 눌렸다",
		);
	}
});

test("요청이 아예 안 나간 경우에도 되돌리는 배선이 있다", () => {
	// 앞 요청 때문에 뒤 클릭이 버려지면 스왑이 없다. 그때는 마지막으로 적용된 기간으로 되돌린다.
	assert.ok(SOURCE.includes("htmx:beforeRequest"), "요청이 나갔는지 봐야 한다");
	assert.ok(SOURCE.includes("lastAppliedMode"), "마지막으로 적용된 기간을 들고 있어야 한다");
	assert.ok(
		/setTimeout\([\s\S]{0,400}?syncPressed\(lastAppliedMode\)/.test(SOURCE),
		"클릭 뒤 확인 타이머에서 되돌려야 한다",
	);
});

test("스왑 뒤 되돌리는 배선이 산출물에 있다", () => {
	assert.ok(
		SOURCE.includes("htmx:afterSwap"),
		"스왑 뒤에 되돌려야 의미가 있다",
	);
	assert.ok(SOURCE.includes("data-applied-range-mode"));
	assert.ok(SOURCE.includes("tradeListFragment"));
	assert.ok(SOURCE.includes("activityListFragment"));
	assert.ok(
		SOURCE.includes('setAttribute("aria-pressed"'),
		"표시는 aria-pressed 로 전달된다(색만 바꾸면 낭독기가 모른다)",
	);
});
