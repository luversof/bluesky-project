// 상세 화면 표의 클라이언트 정렬(계좌 상세 / 종목 상세)을 검증한다.
//
// 이 파일은 130줄인데 테스트가 하나도 없었고, 주석에는 실측으로 고쳤다는 버그 두 건이 적혀 있었다.
//   1) "금액 (증감률%)" 처럼 괄호에 보조 수치가 붙는 열 - 괄호 뒤까지 긁으면 두 수가 이어붙고
//      ("+300,000 (+0.5%)" -> 3000000.5) 숫자 판정도 실패해 문자열 정렬로 떨어졌다
//      (실측: 오름차순인데 -500,000 / +1,900,000 / +2,000,000 / +300,000).
//   2) 텍스트 콜레이션 로케일 - 여기만 undefined 라 영어 브라우저에서 자산현황 표와 순서가 뒤집혔다.
// 고친 사실을 지키는 테스트가 없어서 다시 깨져도 아무도 모른다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

const listeners = [];
globalThis.document = {
	readyState: "complete",
	addEventListener: (type, fn) => listeners.push([type, fn]),
	querySelectorAll: () => [],
	createElement: () => ({ className: "", textContent: "" }),
};

await import("../../resources/static/js/stock/tableSort.js");
const t = globalThis.__tableSortInternals;

/** columnType 은 tbody 를 받으므로 셀 문자열 목록으로 최소 흉내를 낸다. */
function fakeTbody(values) {
	return { rows: values.map((v) => ({ children: [{ textContent: v }] })) };
}

test("내부 규칙이 노출돼 있다", () => {
	assert.ok(t, "tableSort 가 __tableSortInternals 를 노출하지 않는다");
	assert.equal(typeof t.columnType, "function");
});

test("괄호 앞의 대표 수치만 정렬 키로 쓴다", () => {
	// 고치기 전에는 3000000.5 처럼 두 수가 이어붙었다.
	assert.equal(t.leadingNumericText("+300,000 (+0.5%)"), "+300000");
	assert.equal(t.cellNum({ textContent: "+300,000 (+0.5%)" }), 300000);
	assert.equal(t.cellNum({ textContent: "-4,917,426 (-18.3%)" }), -4917426);
});

test("괄호가 붙은 손익 열도 숫자로 판정한다", () => {
	// 판정에 실패하면 문자열 정렬로 떨어져 음수가 뒤섞인다.
	assert.equal(t.columnType(fakeTbody(["-4,917,426 (-18.3%)", "+885,617,421 (+246.6%)"]), 0), "num");
});

test("괄호 열이 실제로 부호 순서대로 정렬된다", () => {
	// 실측으로 어긋났던 그 목록.
	const cells = ["-500,000 (-1.0%)", "+1,900,000 (+2.0%)", "+2,000,000 (+3.0%)", "+300,000 (+0.5%)"];
	const sorted = [...cells].sort((a, b) => t.cellNum({ textContent: a }) - t.cellNum({ textContent: b }));
	assert.deepEqual(sorted, [
		"-500,000 (-1.0%)",
		"+300,000 (+0.5%)",
		"+1,900,000 (+2.0%)",
		"+2,000,000 (+3.0%)",
	]);
});

test("통화·단위·백분율 기호가 붙어도 숫자다", () => {
	assert.equal(t.columnType(fakeTbody(["1,234 주"]), 0), "num");
	assert.equal(t.columnType(fakeTbody(["₩71,887"]), 0), "num");
	assert.equal(t.columnType(fakeTbody(["83.6%"]), 0), "num");
	assert.equal(t.cellNum({ textContent: "83.6%" }), 83.6);
});

test("ISO 날짜는 날짜 열로 본다", () => {
	assert.equal(t.columnType(fakeTbody(["2026-08-19"]), 0), "date");
	assert.equal(t.columnType(fakeTbody(["2026-08-19T00:00:00Z"]), 0), "date");
});

test("빈 셀과 '-' 는 타입 판정에서 건너뛴다", () => {
	// 첫 행이 자리표시자라고 해서 열 전체가 텍스트가 되면 안 된다.
	assert.equal(t.columnType(fakeTbody(["", "-", "1,234"]), 0), "num");
	assert.equal(t.columnType(fakeTbody(["-", "삼성전자"]), 0), "text");
});

test("텍스트 정렬 로케일은 'ko' 로 고정한다", () => {
	// 실행 로케일을 따르면 영어 브라우저에서 자산현황 표와 순서가 뒤집힌다.
	assert.equal(t.TEXT_COLLATION_LOCALE, "ko");
	const names = ["CJ씨푸드", "삼성전자", "KODEX 200", "하이닉스"];
	const sorted = [...names].sort((a, b) => a.localeCompare(b, t.TEXT_COLLATION_LOCALE, { numeric: true }));
	assert.deepEqual(sorted, ["삼성전자", "하이닉스", "CJ씨푸드", "KODEX 200"]);
});
