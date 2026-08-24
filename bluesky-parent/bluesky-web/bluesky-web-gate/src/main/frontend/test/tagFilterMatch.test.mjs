// 종목 태그 필터의 파싱 규칙을 고정한다.
//
// 서버(JTE)는 data-stock-tags 에 태그를 "|" 로 이어 붙이고(String.join("|", tags)), 이 모듈이 그것을
// 갈라 읽는다. 두 쪽이 짝을 이루는 계약이라 어느 한쪽만 바뀌면 필터가 조용히 어긋난다 - 화면은 정상으로
// 보이고 걸러진 목록만 틀린다.
//
// 실측 2026-08-23: 이 사용자의 태그는 6개(ETF / 리츠 / 월말배당 / 월배당 / 월중배당 / 커버드콜)이고
// 구분자를 품은 태그도, 앞뒤 공백이 있는 태그도 없다.
import assert from "node:assert/strict";
import test from "node:test";

// 이 모듈은 import 되는 순간 DOM 을 훑는 IIFE 를 돌린다. 브라우저 밖에서 읽으려면 그만큼만 세워 준다.
class FakeElement {}
globalThis.HTMLElement = FakeElement;
globalThis.Element = FakeElement;
globalThis.Event = class {
	constructor(type) {
		this.type = type;
	}
};
globalThis.document = {
	readyState: "complete",
	getElementById: () => null,
	addEventListener: () => {},
	querySelector: () => null,
	querySelectorAll: () => [],
	body: { dataset: {} },
	documentElement: { lang: "ko-KR" },
};
globalThis.window = globalThis;

await import("../../resources/static/js/stock/tagFilterChips.js");
const { optionMatchesSelectedTags, TAG_SEPARATOR } =
	globalThis.__stockTagFilterInternals;

const option = (stockTags) => ({ dataset: { stockTags } });

test("서버가 쓰는 구분자와 같다", () => {
	assert.equal(TAG_SEPARATOR, "|");
});

test("고른 태그가 하나라도 있으면 통과한다(OR)", () => {
	const selected = new Set(["월중배당"]);
	assert.equal(optionMatchesSelectedTags(option("ETF|월배당|월중배당"), selected), true);
	assert.equal(optionMatchesSelectedTags(option("ETF|월배당|월말배당"), selected), false);
});

test("여러 태그를 고르면 그중 하나만 맞아도 통과한다", () => {
	const selected = new Set(["리츠", "커버드콜"]);
	assert.equal(optionMatchesSelectedTags(option("ETF|커버드콜"), selected), true);
	assert.equal(optionMatchesSelectedTags(option("ETF|월배당"), selected), false);
});

test("아무 태그도 고르지 않으면 통과시키지 않는다", () => {
	// "칩으로 거르지 않음" 상태다. 여기서 true 를 주면 전체 선택 처리와 겹쳐 두 번 적용된다.
	assert.equal(optionMatchesSelectedTags(option("ETF|월배당"), new Set()), false);
});

test("태그가 없는 종목은 어떤 칩에도 걸리지 않는다", () => {
	// 실측: 86종목 중 72종목이 태그 없음(대부분 관심종목).
	const selected = new Set(["ETF"]);
	assert.equal(optionMatchesSelectedTags(option(""), selected), false);
	assert.equal(optionMatchesSelectedTags(option(undefined), selected), false);
});

test("빈 조각과 앞뒤 공백을 흘려보낸다", () => {
	const selected = new Set(["ETF"]);
	assert.equal(optionMatchesSelectedTags(option("|ETF|"), selected), true);
	assert.equal(optionMatchesSelectedTags(option(" ETF "), selected), true);
	assert.equal(optionMatchesSelectedTags(option("||"), selected), false);
});

test("부분 일치로는 걸리지 않는다", () => {
	// 태그 비교는 정확히 같아야 한다. 실제 태그로 고르면 판별이 안 된다 - "월중배당" 안에 "월배당" 은
	// 이어진 부분문자열이 아니라(월-중-배-당), 부분 일치로 바꿔도 결과가 같다. 그래서 실제로 갈리는
	// 값으로 잰다.
	assert.equal(
		optionMatchesSelectedTags(option("월배당"), new Set(["배당"])),
		false,
		"'배당' 을 골랐는데 '월배당' 종목이 딸려 오면 안 된다",
	);
	assert.equal(
		optionMatchesSelectedTags(option("ETF"), new Set(["ET"])),
		false,
		"'ET' 로 'ETF' 가 걸리면 안 된다",
	);
	assert.equal(
		optionMatchesSelectedTags(option("월배당|월중배당"), new Set(["월배당"])),
		true,
	);
});
