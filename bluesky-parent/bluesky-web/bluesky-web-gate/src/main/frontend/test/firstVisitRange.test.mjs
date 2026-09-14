// 저장된 전역 기간이 아직 없는 첫 방문에서, 공유 링크의 기간이 살아남는지 본다.
//
// 예전에는 저장값이 없으면(localStorage 에 globalDateRange 가 없으면) 초기화 블록을 통째로 건너뛰었다.
// 그래서 링크의 기간이 그 화면 한 장에만 적용되고, 메뉴를 한 번 누르면 사라졌다. 실측 2026-09-11(저장값 지운 상태):
//   /stock/trade?rangeMode=3      3개월로 열림 -> 배당 화면으로 이동하면 올해
//   /stock/dividend?rangeMode=6   6개월로 열림 -> 활동 화면으로 이동하면 올해
//   종목 상세 ?rangeMode=3        3개월로 열림 -> '다른 종목' 으로 바꾸면 전체
// 저장값이 한 번이라도 있으면 멀쩡했다(= 저장 경로만 비어 있었다).
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const PICKER = resolve(dirname(fileURLToPath(import.meta.url)), "../src/date-range-picker.ts");

globalThis.window = globalThis.window ?? {};
globalThis.window.addEventListener = globalThis.window.addEventListener ?? (() => {});
globalThis.document = {
	getElementById: () => null,
	querySelector: () => null,
	querySelectorAll: () => [],
	addEventListener: () => {},
};
globalThis.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };

await import("../../resources/static/js/date-range-picker.js");
const { firstVisitRange, resolveInitialRange } = globalThis.__dateRangePickerInternals;

test("내부 함수가 노출돼 있다", () => {
	assert.equal(typeof firstVisitRange, "function");
});

test("조각이 든 기간을 저장한다 - 다음 화면이 따라오도록", () => {
	const current = { start: "2026-06-12", end: "2026-09-11", mode: "3" };
	const out = firstVisitRange(current);

	assert.ok(out, "첫 방문에 기간이 있으면 결과가 있어야 한다");
	assert.equal(out.persist, true, "저장하지 않으면 다음 화면에서 사라진다");
	assert.deepEqual(out.range, current);
});

test("다시 제출하지 않는다 - 조각은 이미 그 기간으로 그려져 있다", () => {
	const out = firstVisitRange({ start: "2026-06-12", end: "2026-09-11", mode: "3" });

	assert.equal(out.submit, false, "여기서 제출하면 같은 조회가 두 번 나간다");
});

test("모드만 있어도(날짜 없는 전체) 기간으로 친다", () => {
	const out = firstVisitRange({ start: "", end: "", mode: "all" });

	assert.ok(out);
	assert.equal(out.range.mode, "all");
	assert.equal(out.persist, true);
});

test("조각에 기간이 없으면 아무것도 하지 않는다", () => {
	assert.equal(firstVisitRange({ start: "", end: "", mode: "" }), null);
	assert.equal(firstVisitRange(null), null, "콜백 모드는 current 가 null 이다");
});

test("저장값이 있을 때의 규칙은 그대로다", () => {
	const stored = { start: "2026-08-12", end: "2026-09-11", mode: "1" };
	const url = { start: "2026-01-01", end: "2026-09-11", mode: "ytd" };

	assert.equal(resolveInitialRange(url, stored).range.mode, "ytd", "URL 이 이긴다");
	assert.equal(resolveInitialRange(url, stored).persist, true);
	assert.equal(resolveInitialRange(null, stored).range.mode, "1", "조각에 기간이 없으면 저장값");
	assert.equal(resolveInitialRange(null, stored).submit, true);
	assert.equal(resolveInitialRange(stored, stored).submit, false, "같으면 다시 조회하지 않는다");
	assert.equal(resolveInitialRange(stored, stored).persist, false);
});

// 계산이 옳아도 초기화가 그것을 부르지 않으면 화면은 그대로다. 호출부를 소스에서 확인한다
// (presetRangeTable.test.mjs 와 같은 방식 - 빌드 산출물은 이름이 줄어 이름으로 찾을 수 없다).
test("초기화가 저장값 없을 때 firstVisitRange 를 부른다", () => {
	const source = readFileSync(PICKER, "utf8");

	assert.match(
		source,
		/const initial = stored\s*\?\s*resolveInitialRange\(current, stored\)\s*:\s*firstVisitRange\(current\);/,
		"저장값이 없을 때의 갈래가 사라지면 공유 링크가 한 화면만 살고 만다",
	);
	assert.match(
		source,
		/const stored = raw \? JSON\.parse\(raw\) : null;/,
		"raw 가 없다고 초기화를 통째로 건너뛰면 안 된다",
	);
});
