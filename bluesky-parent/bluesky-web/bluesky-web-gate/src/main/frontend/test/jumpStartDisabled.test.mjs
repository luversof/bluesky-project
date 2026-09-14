// '가장 이른 기간으로'(«) 버튼을 언제 비활성으로 그릴지.
//
// 이 버튼은 데이터 시작일(minDate)을 알아야 목표를 정할 수 있다. 모르면 doJumpToEdge 가 그냥
// 돌아오는데, 버튼은 눌리는 채로 남아 아무 반응이 없다 - 실측 2026-09-13: 종목 상세·계좌 상세에서
// 두 번 눌러도 기간이 2026-08-14~2026-09-13 그대로였다(매매 화면은 2009-10-06~2009-11-05 로 이동).
// 상세 화면은 detailDateFilter 에 dataFirstDate 를 안 넘겨 minDate 가 빈 값이다.
//
// '전체'(all) 모드에서도 같은 함수가 첫 줄에서 돌아오므로 함께 막는다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

const SOURCE = readFileSync(
	new URL("../../resources/static/js/date-range-picker.js", import.meta.url),
	"utf8",
);

function loadFn() {
	const at = SOURCE.indexOf("function jumpStartDisabled");
	assert.ok(at >= 0, "산출물에 jumpStartDisabled 가 있어야 한다");
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
	return new Function(SOURCE.slice(at, end) + "; return jumpStartDisabled;")();
}

const jumpStartDisabled = loadFn();

test("데이터 시작일을 모르면 비활성이다", () => {
	assert.equal(jumpStartDisabled("", "12"), true);
	assert.equal(jumpStartDisabled("", "mtd"), true);
	assert.equal(jumpStartDisabled("", ""), true);
});

test("데이터 시작일을 알면 활성이다", () => {
	assert.equal(jumpStartDisabled("2009-10-06", "12"), false);
	assert.equal(jumpStartDisabled("2020-03-23", "mtd"), false);
	assert.equal(jumpStartDisabled("2009-10-06", ""), false);
});

/** '전체'는 기간을 걸지 않겠다는 뜻이라 이동할 창 자체가 없다. */
test("전체 모드는 시작일을 알아도 비활성이다", () => {
	assert.equal(jumpStartDisabled("2009-10-06", "all"), true);
	assert.equal(jumpStartDisabled("", "all"), true);
});

/**
 * « 는 '이전'과 같은 클래스(date-range-prev)를 달고 있어, 앞선 판정에서 이미 비활성이 될 수 있다.
 * 뒤에서 무조건 덮어쓰면 최저 구간에 도착해 막아 둔 버튼이 다시 눌린다 - 실측 2026-09-13:
 * 매매 화면에서 « 를 눌러 2009-10-06 으로 간 뒤에도 버튼이 활성으로 남았다(고치기 전).
 */
test("이미 비활성인 버튼을 되살리지 않는다", () => {
	assert.ok(
		/disableJumpStart\s*\|\|\s*\w+\.disabled/.test(SOURCE),
		"앞선 판정을 OR 로 이어받아야 한다",
	);
});

test("비활성 처리가 산출물에 배선돼 있다", () => {
	assert.ok(
		SOURCE.includes('data-picker-action="jump"'),
		"이동 버튼을 찾아야 한다",
	);
	assert.ok(SOURCE.includes('data-picker-arg="start"'), "시작 방향만 대상이다");
	// 산출물은 미니파이돼 공백이 없다 - 공백에 묶이지 않게 본다.
	assert.ok(
		/jumpStartEls[\s\S]{0,300}?\w+\.disabled\s*=\s*off/.test(SOURCE),
		"찾은 버튼에 disabled 를 걸어야 한다",
	);
	assert.ok(
		/jumpStartEls[\s\S]{0,400}?off\)\s*\{[^}]{0,200}?setAttribute\(\s*"aria-disabled"/.test(
			SOURCE,
		),
		"비활성으로 그릴 때 aria-disabled 도 붙여야 한다(색만 흐리게 하면 낭독기는 모른다)",
	);
});
