// 사용자 지정(모드 없는) 기간의 '이전/다음' 이동.
//
// 표시 구간은 양끝 포함이라 길이는 end - start + 1 일이다. 예전에는 end - start 만큼만 옮겨
// 직전 구간이 원래 시작일을 다시 덮었다 - 실측 2026-09-11(브라우저):
//   5/10~5/19(10일) → 이전 5/1~5/10   (5/10 이 두 구간에 들어감)
//   6/08~6/14( 7일) → 이전 6/2~6/8    (6/8 겹침)
//   4/01~5/15(45일) → 이전 2/16~4/1   (4/1 겹침)
//   6/10~6/10( 1일) → 이전 6/10~6/10  (움직이지 않음 - 버튼이 죽어 있었다)
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
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
const { shiftFreeRange, freeRangeShiftAllowed } = globalThis.__dateRangePickerInternals;

// 로컬 자정 Date 를 로컬 기준으로 찍는다 - toISOString 은 KST 에서 하루 앞으로 밀린다.
const iso = (d) => d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
const back = (s, e) => { const r = shiftFreeRange(s, e, -1); return r && [iso(r.start), iso(r.end)]; };
const forward = (s, e) => { const r = shiftFreeRange(s, e, 1); return r && [iso(r.start), iso(r.end)]; };
const dayBefore = (s) => iso(new Date(new Date(s + "T00:00:00").getTime() - 86400000));

test("내부 계산이 노출돼 있다", () => {
	assert.equal(typeof shiftFreeRange, "function");
});

test("직전 구간은 원래 시작일 바로 앞에서 끝난다", () => {
	for (const [s, e] of [["2026-05-10", "2026-05-19"], ["2026-06-08", "2026-06-14"], ["2026-04-01", "2026-05-15"]]) {
		const [ns, ne] = back(s, e);
		assert.equal(ne, dayBefore(s), s + "~" + e + " 의 직전 구간이 겹친다");
		assert.ok(ns < ne || ns === ne);
	}
});

test("길이가 보존된다", () => {
	for (const [s, e] of [["2026-05-10", "2026-05-19"], ["2026-06-10", "2026-06-10"], ["2026-04-01", "2026-05-15"]]) {
		const len = (a, b) => Math.round((new Date(b + "T00:00:00") - new Date(a + "T00:00:00")) / 86400000) + 1;
		const [ns, ne] = back(s, e);
		assert.equal(len(ns, ne), len(s, e), s + "~" + e);
	}
});

test("하루짜리 구간도 하루씩 움직인다", () => {
	assert.deepEqual(back("2026-06-10", "2026-06-10"), ["2026-06-09", "2026-06-09"]);
	assert.deepEqual(forward("2026-06-10", "2026-06-10"), ["2026-06-11", "2026-06-11"]);
});

test("다음 구간은 원래 종료일 바로 다음에서 시작한다", () => {
	const [ns] = forward("2026-05-10", "2026-05-19");
	assert.equal(ns, "2026-05-20");
});

test("앞뒤로 한 번씩 옮기면 제자리로 돌아온다", () => {
	for (const [s, e] of [["2026-05-10", "2026-05-19"], ["2026-02-01", "2026-02-28"], ["2024-02-29", "2024-03-31"]]) {
		const [bs, be] = back(s, e);
		assert.deepEqual(forward(bs, be), [s, e], s + "~" + e);
	}
});

test("거꾸로거나 값이 없으면 옮기지 않는다", () => {
	assert.equal(shiftFreeRange("2026-05-19", "2026-05-10", -1), null);
	assert.equal(shiftFreeRange("", "2026-05-10", -1), null);
	assert.equal(shiftFreeRange("2026-05-10", "", -1), null);
});
test("앞으로 갈 때는 시작이 오늘을 넘을 때만 막는다", () => {
	const today = "2026-09-11";
	// 8/30~9/8 의 다음 창은 9/9~9/18 - 끝은 오늘을 넘지만 시작은 넘지 않으므로 허용해야 한다
	// (적용 쪽이 9/9~9/11 로 자른다). 예전에는 여기서 막혀 마지막 창에 도달하지 못했다.
	assert.equal(freeRangeShiftAllowed("2026-08-30", "2026-09-08", 1, today), true);
	// 9/9~9/11 의 다음 창은 시작이 9/12 - 오늘을 넘으므로 막는다
	assert.equal(freeRangeShiftAllowed("2026-09-09", "2026-09-11", 1, today), false);
});

test("뒤로 갈 때는 최소일 이전으로 못 간다", () => {
	assert.equal(freeRangeShiftAllowed("2020-04-10", "2020-04-19", -1, "2026-09-11", "2020-04-08"), false);
	assert.equal(freeRangeShiftAllowed("2020-04-20", "2020-04-29", -1, "2026-09-11", "2020-04-08"), true);
});

test("옮길 수 없는 구간은 허용하지 않는다", () => {
	assert.equal(freeRangeShiftAllowed("2026-05-19", "2026-05-10", -1, "2026-09-11"), false);
	assert.equal(freeRangeShiftAllowed("", "2026-05-10", 1, "2026-09-11"), false);
});
