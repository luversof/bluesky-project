// 기간 프리셋 기준표가 화면 소스와 여전히 같은지 본다.
//
// src/test/resources/date-range-preset-frontend.txt 는 서버 쪽 규칙(PresetRangeParityTest)이 대조하는
// 기준표다. 손으로 만든 파일이라 date-range-picker.ts 가 바뀌면 조용히 낡는다 - 그러면 서버 검사는
// 통과하는데 실제 화면과는 어긋나는 상태가 된다. 여기서 화면 소스의 규칙을 그대로 다시 계산해 맞춘다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const PICKER = resolve(HERE, "../src/date-range-picker.ts");
const TABLE = resolve(HERE, "../../../test/resources/date-range-preset-frontend.txt");

// date-range-picker.ts 의 addMonthsClamped 를 그대로 옮긴 것. 아래에서 소스와 대조한다.
const addMonthsClamped = (date, months) => {
	const targetFirst = new Date(date.getFullYear(), date.getMonth() + months, 1);
	const targetLastDay = new Date(
		targetFirst.getFullYear(),
		targetFirst.getMonth() + 1,
		0,
	).getDate();
	return new Date(
		targetFirst.getFullYear(),
		targetFirst.getMonth(),
		Math.min(date.getDate(), targetLastDay),
	);
};
const addDays = (d, n) => {
	const c = new Date(d);
	c.setDate(c.getDate() + n);
	return c;
};
const fmt = (d) =>
	d.getFullYear() +
	"-" +
	String(d.getMonth() + 1).padStart(2, "0") +
	"-" +
	String(d.getDate()).padStart(2, "0");

function range(todayStr, mode) {
	const today = new Date(todayStr + "T00:00:00");
	if (mode === "mtd") {
		return [fmt(new Date(today.getFullYear(), today.getMonth(), 1)), fmt(today)];
	}
	if (mode === "ytd") {
		return [today.getFullYear() + "-01-01", fmt(today)];
	}
	return [fmt(addDays(addMonthsClamped(today, -Number(mode)), 1)), fmt(today)];
}

test("서버가 대조하는 기준표가 화면 규칙과 같다", () => {
	const rows = readFileSync(TABLE, "utf8")
		.split("\n")
		.map((l) => l.trim())
		.filter(Boolean)
		.map((l) => l.split("|"));
	// 표가 조용히 비면 검사가 무력해진다(기준일 8 x 모드 7).
	assert.equal(rows.length, 56);

	const bad = [];
	for (const [todayStr, mode, start, end] of rows) {
		const [s, e] = range(todayStr, mode);
		if (s !== start || e !== end) {
			bad.push(`${todayStr} ${mode}: 표 ${start}~${end} / 규칙 ${s}~${e}`);
		}
	}
	assert.deepEqual(bad, [], "기준표가 낡았다. 다시 뽑아 넣을 것");
});

test("여기 옮겨 적은 규칙이 화면 소스와 같다", () => {
	const source = readFileSync(PICKER, "utf8");
	// 옮겨 적은 계산이 소스와 갈리면 위 검사가 엉뚱한 것을 지키게 된다.
	assert.ok(
		source.includes("Math.min(date.getDate(), targetLastDay)"),
		"addMonthsClamped 의 말일 클램프가 사라졌다",
	);
	assert.ok(
		source.includes("addDays(addMonthsClamped(maxDate, -months), 1)"),
		"N개월 프리셋의 '하루 더하기' 규칙이 바뀌었다",
	);
	assert.ok(
		source.includes('today.getFullYear() + "-01-01"'),
		"ytd 시작일 규칙이 바뀌었다",
	);
});
