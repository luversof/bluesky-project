// 종이에서 넓은 표가 지면을 넘지 않는가.
//
// 화면에서는 `.overflow-x-auto` 래퍼가 넓은 표를 감싸 가로 스크롤로 보여 준다(문서는 안 넘친다).
// 그런데 @media print 는 그 래퍼를 풀어 버린다(`.overflow-x-auto { overflow: visible !important }`) -
// 종이에는 스크롤이 없으니 당연한 처방이지만, 표가 그대로 지면 밖으로 나가면 **오른쪽 열이 통째로 안 찍힌다**.
//
// 실측 2026-09-15(816px = A4 본문, print 미디어, 지속가능성 시뮬레이터 '연도별 상세' 12열):
//   화면  표 1117px / 래퍼 734px / 문서 넘침 0      ← 스크롤로 정상
//   종이  표  925px / 문서 넘침 150px               ← 오른쪽 열 잘림
//   print-dense(0.70rem) 로는 816px(넘침 41px) 로 부족, 패딩 축소는 48px 뿐.
//   셀 글자 0.65rem 으로 768px · 넘침 0.
//
// 인쇄 미리보기를 열어야만 보이고 화면 검사로는 절대 안 잡힌다. 그래서 여기서 지킨다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

const css = readFileSync(new URL("../main.css", import.meta.url), "utf8");
const simulator = readFileSync(
	new URL("../../jte/stock/simulator.jte", import.meta.url),
	"utf8",
);

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

test("인쇄가 가로 스크롤 래퍼를 푸는 규칙이 여전히 있다", () => {
	// 이 처방이 사라지면 아래 대비책들도 다시 볼 일이다(그때는 표가 스크롤 래퍼에 잘린다).
	const blocks = printBlocks();
	assert.ok(blocks.length >= 1, "@media print 를 못 찾았다 - 검사가 공짜로 통과한다");
	assert.ok(
		blocks.some((b) => /\.overflow-x-auto[^{]*\{[^}]*overflow:\s*visible/.test(b)),
		"인쇄에서 스크롤 래퍼를 푸는 규칙을 못 찾았다 - 파서가 무력하거나 규칙이 바뀌었다",
	);
});

test("연도별 표는 종이에서 한 단계 더 줄인다", () => {
	const blocks = printBlocks();
	assert.ok(
		blocks.some((b) =>
			/table\[data-sustain-yearly-table\]\s*:is\(th,\s*td\)\s*\{[^}]*font-size:\s*0?\.65rem/.test(b),
		),
		"12열 표를 줄이는 인쇄 규칙이 없다 - 종이에서 오른쪽 열이 잘린다(실측 문서 넘침 150px)",
	);
});

test("연도별 표가 그 훅을 달고 있다", () => {
	assert.match(
		simulator,
		/<table[^>]*data-sustain-yearly-table/,
		"표에서 data-sustain-yearly-table 이 빠지면 위 인쇄 규칙이 아무것도 지키지 못한다",
	);
});
