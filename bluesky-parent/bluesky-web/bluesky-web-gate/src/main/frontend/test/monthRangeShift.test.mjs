// 상대 개월(1/3/6/12/36개월) 구간의 '이전/다음' 이동.
//
// 실측 2026-09-11(브라우저): 말일에서 시작하는 1개월 창을 뒤로 걸어가니 양끝을 따로 클램프해
// 창 길이가 틀어졌다 - 1/28~2/28 은 32 일이었다(1개월이면 1/28~2/27 = 31 일).
// 종료일을 옮긴 시작일에서 다시 세면 서버의 프리셋 정의(minusMonths(N).plusDays(1))와 맞는다.
//
// 앵커 소실(3/31 -> 2/28 -> 1/28 로 내려가 28 일에 갇힘)은 이 파일이 다루지 않는다 - 앵커를 기억할
// 상태가 없어 정책 결정이 필요하다(보고만 함).
import assert from "node:assert/strict";
import test from "node:test";

const listeners = [];
globalThis.document = {
	readyState: "complete",
	addEventListener: (type, fn) => listeners.push([type, fn]),
	querySelectorAll: () => [],
	querySelector: () => null,
	getElementById: () => null,
	createElement: () => ({ className: "", textContent: "", style: {}, classList: { add() {}, remove() {}, toggle() {}, contains: () => false } }),
	documentElement: { getAttribute: () => "ko-KR" },
	body: { appendChild() {} },
};
globalThis.window = globalThis;
globalThis.addEventListener = (type, fn) => listeners.push([type, fn]);

await import("../../resources/static/js/date-range-picker.js");
const internals = globalThis.__dateRangePickerInternals;
// create() 안에서 붙으므로 한 번 만들어 준다.
if (!internals.shiftNumericMonthRange && typeof globalThis.DateRangePicker?.create === "function") {
	try { globalThis.DateRangePicker.create({}); } catch (e) { /* DOM 이 없어도 노출까지는 지난다 */ }
}
const { shiftNumericMonthRange } = globalThis.__dateRangePickerInternals;

const len = (a, b) => Math.round((new Date(b + "T00:00:00") - new Date(a + "T00:00:00")) / 86400000) + 1;

test("내부 계산이 노출돼 있다", () => {
	assert.equal(typeof shiftNumericMonthRange, "function");
});

test("통월 구간은 통월로 남는다", () => {
	let cur = { start: "2026-03-01", end: "2026-03-31" };
	const seen = [];
	for (let i = 0; i < 5; i++) {
		cur = shiftNumericMonthRange(cur.start, cur.end, 1, -1);
		seen.push(cur.start + "~" + cur.end);
	}
	assert.deepEqual(seen, [
		"2026-02-01~2026-02-28",
		"2026-01-01~2026-01-31",
		"2025-12-01~2025-12-31",
		"2025-11-01~2025-11-30",
		"2025-10-01~2025-10-31",
	]);
});

test("윤년 2월도 통월로 맞는다", () => {
	const r = shiftNumericMonthRange("2024-03-01", "2024-03-31", 1, -1);
	assert.deepEqual(r, { start: "2024-02-01", end: "2024-02-29" });
});

test("통월이 아닌 1개월 창은 길이가 한 달을 넘지 않는다", () => {
	let cur = { start: "2026-03-31", end: "2026-04-29" };
	for (let i = 0; i < 6; i++) {
		cur = shiftNumericMonthRange(cur.start, cur.end, 1, -1);
		const days = len(cur.start, cur.end);
		assert.ok(days >= 28 && days <= 31, cur.start + "~" + cur.end + " 가 " + days + "일이다");
	}
});

test("종료일은 옮긴 시작일에서 다시 센다", () => {
	// 1/28 에서 1개월이면 2/27 까지다(2/28 이 아니다).
	const r = shiftNumericMonthRange("2026-02-28", "2026-03-29", 1, -1);
	assert.equal(r.start, "2026-01-28");
	assert.equal(r.end, "2026-02-27");
	assert.equal(len(r.start, r.end), 31);
});

test("3개월 창도 같은 규칙이다", () => {
	const r = shiftNumericMonthRange("2026-05-31", "2026-08-30", 3, -1);
	assert.equal(r.start, "2026-02-28");
	assert.equal(r.end, "2026-05-27");
});

test("앞으로 옮겨도 길이 규칙은 같다", () => {
	const r = shiftNumericMonthRange("2026-01-28", "2026-02-27", 1, 1);
	assert.equal(r.start, "2026-02-28");
	assert.equal(r.end, "2026-03-27");
});
