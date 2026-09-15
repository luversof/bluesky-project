// 인쇄에서 달력 열이 무너지지 않는가.
//
// main.css 의 @media print 는 표를 쪽 폭에 맞추려고 `table { table-layout: auto !important }` 를 건다.
// 열 많은 자료표에는 맞는 처방이지만 **달력**에 걸리면 요일 정렬이 깨진다 - 달력은 열 폭이 곧 뜻이다.
//
// 실측 2026-09-15(배당 캘린더, 816px 인쇄 미디어):
//   화면  [107,107,107,107,107,107,107]  편차 0
//   종이  [ 41, 40, 40,270,247, 43, 67]  편차 230px   ← 종목이 있는 수·목만 넓어졌다
// 고친 뒤 종이도 [107×7] 편차 0.
//
// 눈으로는 인쇄 미리보기를 열어야만 보이고, 화면 검사로는 절대 안 잡힌다. 그래서 여기서 지킨다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

const css = readFileSync(new URL("../main.css", import.meta.url), "utf8");

/** @media print 블록 본문만 모은다(중첩 괄호를 센다). */
function printBlocks() {
	const blocks = [];
	const re = /@media\s+print[^{]*\{/g;
	let m;
	while ((m = re.exec(css)) !== null) {
		let i = m.index + m[0].length;
		let depth = 1;
		const start = i;
		while (i < css.length && depth > 0) {
			if (css[i] === "{") depth++;
			else if (css[i] === "}") depth--;
			i++;
		}
		blocks.push(css.slice(start, i - 1));
	}
	return blocks;
}

test("인쇄 블록을 실제로 찾는다", () => {
	const blocks = printBlocks();
	assert.ok(blocks.length >= 1, "@media print 를 못 찾았다 - 아래 검사가 공짜로 통과한다");
	assert.ok(
		blocks.some((b) => b.includes("table-layout")),
		"인쇄 블록에 table-layout 규칙이 없다 - 파서가 무력하거나 규칙이 사라졌다",
	);
});

test("table-fixed 를 붙인 표는 인쇄에서도 fixed 다", () => {
	const blocks = printBlocks();
	const auto = blocks.filter((b) => /table\s*\{[^}]*table-layout:\s*auto/.test(b));
	if (auto.length === 0) {
		// 뭉뚱그린 auto 규칙이 사라졌다면 이 예외도 필요 없다.
		return;
	}
	const exempt = blocks.some((b) =>
		/table\.table-fixed\s*\{[^}]*table-layout:\s*fixed\s*!important/.test(b),
	);
	assert.ok(
		exempt,
		"인쇄가 table-layout:auto 를 뭉뚱그려 걸면서 table.table-fixed 예외를 두지 않았다." +
			" 달력 열이 종목 있는 날만 넓어진다(실측 편차 230px)",
	);
});

test("달력 표는 table-fixed 를 달고 있다", () => {
	const template = readFileSync(
		new URL("../../jte/stock/fragments/dividendCalendarMonth.jte", import.meta.url),
		"utf8",
	);
	assert.match(
		template,
		/<table[^>]*table-fixed[^>]*data-calendar-table/,
		"달력 표에서 table-fixed 가 빠지면 위 예외가 아무것도 지키지 못한다",
	);
});
