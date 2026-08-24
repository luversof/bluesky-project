// 전역 기간(localStorage 'globalDateRange')을 hidden input 으로 옮기는 스크립트를 검증한다.
//
// 이 파일에는 날짜->instant 변환이 두 벌 복사돼 있었고, 정본(date-range-picker)까지 셋의 동작이 서로
// 달랐다. 실측 결과:
//   "2026-08-"             정본 예외 / 사본 둘 다 조용히 2026-07-31  <- 틀린 기간이 표시 없이 조회에 실린다
//   "2026-08-23T00:00:00"  정본 정상 / 사본1 예외 / 사본2 ""
//   "abc"                  정본·사본1 예외 / 사본2 ""
// 지금은 규칙이 한 벌이고 형식이 아니면 "" 다(= 기간 없음).
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

const inputs = new Map();
function fakeInput(id) {
	if (!inputs.has(id)) inputs.set(id, { id, value: "" });
	return inputs.get(id);
}

globalThis.window = globalThis.window ?? {};
globalThis.window.addEventListener = globalThis.window.addEventListener ?? (() => {});
globalThis.document = {
	getElementById: (id) => fakeInput(id),
	querySelectorAll: () => [],
	addEventListener: () => {},
};
globalThis.localStorage = {
	_v: JSON.stringify({
		start: "2026-08-01",
		end: "2026-08-19",
		mode: "custom",
		timeZone: "Asia/Seoul",
	}),
	getItem(key) {
		return key === "globalDateRange" ? this._v : null;
	},
};

await import("../../resources/static/js/date-range-picker.js");
await import("../../resources/static/js/stock/globalDateRange.js");

const shared = globalThis.__dateRangePickerInternals.localDateToInstantIso;

function localMidnightIso(y, m, d) {
	return new Date(y, m - 1, d, 0, 0, 0, 0).toISOString();
}

test("최초 로드에 저장된 기간이 hidden input 으로 들어간다", () => {
	assert.equal(fakeInput("globalStartInstantInput").value, localMidnightIso(2026, 8, 1));
	// 종료일은 다음 날 00:00 (api-stock 의 배타 규약)
	assert.equal(fakeInput("globalEndInstantInput").value, localMidnightIso(2026, 8, 20));
	assert.equal(fakeInput("globalTimeZoneInput").value, "Asia/Seoul");
	assert.equal(fakeInput("globalRangeModeInput").value, "custom");
});

test("형식이 아니면 날짜를 지어내지 않고 빈 값이다", () => {
	// 잘린 값이 조용히 전날로 바뀌던 자리다.
	assert.equal(shared("2026-08-", 1), "", "잘린 날짜가 2026-07-31 로 바뀌면 안 된다");
	assert.equal(shared("abc", 1), "");
	assert.equal(shared("2026-08-23T00:00:00", 1), "");
	assert.equal(shared("", 1), "");
});

test("정상 형식은 자릿수를 채우지 않아도 읽는다", () => {
	assert.equal(shared("2026-8-3", 0), localMidnightIso(2026, 8, 3));
});

test("globalDateRange 는 변환 규칙을 다시 구현하지 않는다", () => {
	const source = readFileSync(
		new URL("../../resources/static/js/stock/globalDateRange.js", import.meta.url),
		"utf8",
	);
	assert.ok(
		source.includes("__dateRangePickerInternals"),
		"공용 규칙을 쓰지 않는다 - 사본이 다시 생기면 셋의 동작이 갈린다",
	);
	assert.ok(
		!/new Date\(\s*y\s*,\s*m\s*,\s*d\s*,/.test(source),
		"날짜를 직접 만드는 사본이 남아 있다",
	);
});

test("레이아웃이 정본을 먼저 로드한다", () => {
	const layout = readFileSync(
		new URL("../../jte/_layout/stockLayout.jte", import.meta.url),
		"utf8",
	);
	const pickerAt = layout.indexOf("/js/date-range-picker.js");
	const globalAt = layout.indexOf("/js/stock/globalDateRange.js");
	assert.ok(pickerAt > 0 && globalAt > 0, "두 스크립트가 레이아웃에 없다");
	assert.ok(
		pickerAt < globalAt,
		"date-range-picker.js 가 먼저 로드돼야 globalDateRange 가 공용 규칙을 쓸 수 있다",
	);
});
