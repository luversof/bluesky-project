// N개월 프리셋이 "데이터 처음부터 앞으로" 가 되는 조건.
//
// 실측 2026-09-12(오늘 2026-09-12, 매매 데이터 마지막 2026-09-02, 첫 매매 2009-10-06):
//   깨끗한 진입 후 "1년"            -> 2025-09-13 ~ 2026-09-12   (정상)
//   ?rangeMode=all 로 들어가 "1년"   -> 2025-09-13 ~ 2026-09-12   (정상)
//   주소에 2015-01-01~ 을 싣고 "1년" -> 2025-09-13 ~ 2026-09-12   (정상)
//   주소에 2009-01-01~2026-09-11 을 싣고 "1년" -> 2009-10-06 ~ 2010-10-05  (⚠ 16년 어긋남)
// 경계도 재 봤다: 시작 2009-10-06(첫 거래일) 이하면 어긋나고 2009-10-07 부터는 정상이었다.
//
// 원인은 doSet 의 '처음부터' 분기다: 시작이 데이터 첫날 이하이고 끝이 오늘보다 이르면 첫 N개월을 준다.
// 이 테스트는 그 규칙을 <b>있는 그대로</b> 고정한다 - 고친 것이 아니라, 바뀌면 알아채도록 묶어 둔 것이다.
// (picker 의 maxDate 는 데이터 마지막 날이 아니라 '오늘' 이라 데이터 끝으로 판정을 보태도 아무것도 달라지지 않는다.
//  실측: input[type=date] 의 max 가 2026-09-12 = 오늘.)
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.document = globalThis.document ?? { getElementById: () => null };

await import("../../resources/static/js/date-range-picker.js");
const { presetAnchorsAtDataStart } = globalThis.__dateRangePickerInternals;

const TODAY = "2026-09-12";
const DATA_END = "2026-09-02";
const DATA_START = "2009-10-06";
const at = (start, end) => presetAnchorsAtDataStart(start, end, DATA_START, DATA_END, TODAY);

test("계산이 노출돼 있다", () => {
	assert.equal(typeof presetAnchorsAtDataStart, "function");
});

test("오늘까지 보고 있으면 오늘 기준이다", () => {
	assert.equal(at(DATA_START, TODAY), false);
	// 끝을 모르면(빈 값) 전체로 보고 오늘 기준
	assert.equal(at(DATA_START, ""), false);
});

test("끝이 오늘보다 이르면 '처음부터' 로 간다 - 이것이 알려진 함정의 원인이다", () => {
	// 데이터의 마지막 날(09-02)까지 보고 있어도 오늘(09-12)보다 이르므로 '끝에 닿지 않음' 으로 본다.
	assert.equal(at(DATA_START, DATA_END), true);
	// 전체를 덮되 끝이 어제인 창 - 실측에서 "1년" 이 2009-10-06~2010-10-05 를 준 바로 그 경우.
	assert.equal(at("2009-01-01", "2026-09-11"), true);
});

test("처음에 붙어 있고 끝에 닿지 않았으면 앞으로 잡는다", () => {
	// 처음부터 훑어보는 사람 - 이 기능은 그대로 남는다
	assert.equal(at(DATA_START, "2010-01-01"), true);
	assert.equal(at("2009-01-01", "2012-12-31"), true);
});

test("처음에 붙어 있지 않으면 언제나 오늘 기준이다", () => {
	assert.equal(at("2009-10-07", "2010-10-06"), false);
	assert.equal(at("2015-01-01", "2015-12-31"), false);
	assert.equal(at("2024-01-01", "2024-12-31"), false);
});

test("데이터 시작을 모르면 앞으로 잡지 않는다", () => {
	assert.equal(presetAnchorsAtDataStart("2009-01-01", "2010-01-01", null, DATA_END, TODAY), false);
	assert.equal(presetAnchorsAtDataStart("2009-01-01", "2010-01-01", "", DATA_END, TODAY), false);
});
