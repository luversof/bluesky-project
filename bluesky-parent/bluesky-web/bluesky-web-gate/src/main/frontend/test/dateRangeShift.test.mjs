// 기간 창 이동(« ‹ › »)의 계산을 검증한다.
//
// 실측 2026-09-12(매매 화면에서 실제로 눌러 본 값):
//   1개월  2026-08-13~09-12 → 07-13~08-12 → 06-13~07-12 → 05-13~06-12  (빈틈·겹침 0)
//   3개월  2026-06-13~09-12 → 03-13~06-12 → 2025-12-13~2026-03-12
//   이번달 2026-09-01~09-12 → 08-01~08-31 → 07-01~07-31 (통월은 달 전체로)
//   «     첫 거래일(2009-10-06)로 가고 거기서 «·‹ 이전이 비활성
//   왕복  ‹ 3번 뒤 › 3번이면 처음 창으로 정확히 복귀
//
// 이 계산은 2026-09-12 까지 초기화 클로저 안에 있어 테스트가 닿지 못했다(브라우저에서 초기화되기 전에는
// 노출 객체에도 없었다). 순수 계산이라 모듈 범위로 올려 여기서 고정한다.
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.document = globalThis.document ?? { getElementById: () => null };

await import("../../resources/static/js/date-range-picker.js");
const mod = globalThis.__dateRangePickerInternals;

const shift = (s, e, months, dir, max, min) => mod.shiftNumericMonthRange(s, e, months, dir, max, min);
const day = (s) => new Date(s + "T00:00:00");
const lengthOf = (w) => Math.round((day(w.end) - day(w.start)) / 86400000) + 1;

test("계산이 노출돼 있다", () => {
	assert.equal(typeof mod.shiftNumericMonthRange, "function");
	assert.equal(typeof mod.isWholeMonthRange, "function");
});

test("1개월 창을 뒤로 걸으면 빈틈도 겹침도 없다", () => {
	let w = { start: "2026-08-13", end: "2026-09-12" };
	const seen = [w];
	for (let i = 0; i < 3; i++) {
		const prev = shift(w.start, w.end, 1, -1);
		assert.ok(prev, "뒤로 이동이 null 이면 안 된다");
		// 이음새: 앞 창의 시작 = 이 창의 끝 + 1일
		assert.equal(Math.round((day(w.start) - day(prev.end)) / 86400000), 1, "이음새가 하루여야 한다");
		w = prev;
		seen.push(w);
	}
	assert.deepEqual(seen.map((x) => x.start + "~" + x.end), [
		"2026-08-13~2026-09-12",
		"2026-07-13~2026-08-12",
		"2026-06-13~2026-07-12",
		"2026-05-13~2026-06-12",
	]);
});

test("창 길이는 달 길이를 따라가되 늘어나지 않는다", () => {
	// 2026-01-28~02-27 을 뒤로: 2월이 짧다고 32 일짜리가 되면 안 된다(주석에 남은 실측 결함).
	const w = shift("2026-01-28", "2026-02-27", 1, -1);
	assert.equal(w.start, "2025-12-28");
	assert.equal(w.end, "2026-01-27");
	assert.equal(lengthOf(w), 31);
});

test("통월 창은 달 전체로 움직인다", () => {
	assert.equal(mod.isWholeMonthRange("2026-09-01", "2026-09-30"), true);
	assert.deepEqual(shift("2026-09-01", "2026-09-30", 1, -1), { start: "2026-08-01", end: "2026-08-31" });
	assert.deepEqual(shift("2026-02-01", "2026-02-28", 1, -1), { start: "2026-01-01", end: "2026-01-31" });
});

test("앞으로 갈 때 오늘을 넘지 않는다", () => {
	const max = day("2026-09-12");
	// 창 전체가 미래면 이동하지 않는다(버튼 비활성과 짝)
	assert.equal(shift("2026-08-13", "2026-09-12", 1, 1, max), null);
	// 끝만 넘으면 오늘로 자른다
	const w = shift("2026-07-13", "2026-08-12", 1, 1, max);
	assert.equal(w.start, "2026-08-13");
	assert.equal(w.end, "2026-09-12");
});

test("뒤로 갈 때 첫 데이터 이전으로 가지 않는다", () => {
	const min = day("2009-10-06");
	assert.equal(shift("2009-10-06", "2009-11-05", 1, -1, undefined, min), null);
	assert.ok(shift("2009-11-06", "2009-12-05", 1, -1, undefined, min));
});

test("뒤로 갔다 앞으로 오면 처음 창으로 돌아온다", () => {
	const max = day("2026-09-12");
	const first = { start: "2026-08-13", end: "2026-09-12" };
	let w = first;
	for (let i = 0; i < 3; i++) w = shift(w.start, w.end, 1, -1);
	for (let i = 0; i < 3; i++) w = shift(w.start, w.end, 1, 1, max);
	assert.deepEqual(w, first);
});

test("3개월도 같은 규칙이다", () => {
	assert.deepEqual(shift("2026-06-13", "2026-09-12", 3, -1), { start: "2026-03-13", end: "2026-06-12" });
	assert.deepEqual(shift("2026-03-13", "2026-06-12", 3, -1), { start: "2025-12-13", end: "2026-03-12" });
});
