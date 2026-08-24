// 화면이 고른 날짜를 서버 창(instant)으로 바꾸는 계산을 검증한다.
//
// 이 한 줄이 모든 주식 화면의 기간을 정한다. api-stock 의 기간 규약은 배타적이라
// (시계열의 toInclusiveEndDate, 필터 id 조회의 `< :endDate`) 종료일은 "다음 날 00:00" 으로
// 보내야 한다. 여기서 하루가 어긋나면 표·합계·차트가 전부 하루씩 밀린다.
//
// 지금까지 이 파일(1,100줄 남짓)에는 테스트가 하나도 없었고, 같은 함수가 세 벌 복사돼 있었다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.document = globalThis.document ?? { getElementById: () => null };

await import("../../resources/static/js/date-range-picker.js");
const mod = globalThis.__dateRangePickerInternals;

/** 로컬 자정을 UTC ISO 로. 브라우저 타임존에 의존하므로 기대값도 같은 방식으로 만든다. */
function localMidnightIso(y, m, d) {
	return new Date(y, m - 1, d, 0, 0, 0, 0).toISOString();
}

test("내부 계산이 노출돼 있다", () => {
	assert.ok(mod, "date-range-picker 가 __dateRangePickerInternals 를 노출하지 않는다");
	assert.equal(typeof mod.localDateToInstantIso, "function");
});

test("시작일은 그 날 로컬 자정 그대로다", () => {
	assert.equal(mod.localDateToInstantIso("2026-08-01", 0), localMidnightIso(2026, 8, 1));
});

test("종료일은 다음 날 로컬 자정이다(api-stock 의 배타 규약)", () => {
	assert.equal(mod.localDateToInstantIso("2026-08-19", 1), localMidnightIso(2026, 8, 20));
});

test("월말·연말을 넘어도 다음 날로 넘어간다", () => {
	assert.equal(mod.localDateToInstantIso("2026-01-31", 1), localMidnightIso(2026, 2, 1));
	assert.equal(mod.localDateToInstantIso("2026-12-31", 1), localMidnightIso(2027, 1, 1));
	// 2028 은 윤년이다. 2/28 다음은 2/29 여야 한다.
	assert.equal(mod.localDateToInstantIso("2028-02-28", 1), localMidnightIso(2028, 2, 29));
});

test("addDays 를 주지 않으면 더하지 않는다", () => {
	assert.equal(mod.localDateToInstantIso("2026-08-19"), localMidnightIso(2026, 8, 19));
});

test("빈 값은 빈 문자열이다(창 없음)", () => {
	assert.equal(mod.localDateToInstantIso("", 1), "");
	assert.equal(mod.localDateToInstantIso(undefined, 1), "");
});

test("시작과 종료가 같은 날이면 창은 하루짜리다", () => {
	const start = mod.localDateToInstantIso("2026-08-19", 0);
	const end = mod.localDateToInstantIso("2026-08-19", 1);
	const hours = (new Date(end) - new Date(start)) / 3_600_000;
	// DST 가 있는 존에서는 23 또는 25 시간일 수 있다. 0 이나 48 이면 규약이 깨진 것이다.
	assert.ok(hours >= 23 && hours <= 25, `하루가 아니라 ${hours}시간이다`);
});

test("이 계산은 한 벌만 있다", async () => {
	// 예전에는 같은 함수가 이 파일 안에 세 벌 있었다. 이 저장소에서 같은 공식이 여러 곳에 있으면
	// 한쪽만 고쳐져 갈라진 사례가 반복됐다(활동 묶기 3벌, 매도원가 2곳, 보유원가 대체경로).
	const { readFile } = await import("node:fs/promises");
	const source = await readFile(
		new URL("../src/date-range-picker.ts", import.meta.url),
		"utf8",
	);
	const copies = source.split("function localDateToInstantIso").length - 1;
	assert.equal(copies, 1, `localDateToInstantIso 가 ${copies} 벌 있다`);
});
