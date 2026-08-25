// 배당 내역의 '월평균' 분모와 월별 차트 x축이 달력 개월수인지 본다.
//
// "전체"를 고르면 rangeMode 가 all 이라 시작일·종료일이 아예 비어 온다. 예전에는 그때 빈 달을 채우는
// 경로를 통째로 건너뛰어, 배당이 들어온 달만 라벨이 됐다 - 월평균의 분모가 '개월수'가 아니라
// '배당이 있었던 달의 수'였다.
//
// 실측 2026-08-24(실데이터): 세후 61,645,687원 / 193건, 첫 배당 2020-04 ~ 마지막 2026-08.
//   배당이 있는 달 36개월 -> 1,712,380원   (예전 화면)
//   달력 개월수  77개월 -> 800,593원      (2.14배 차이)
//   배당이 0원인 달 41개월 - 차트 x축에서도 통째로 빠져 있었다.
// 초기에는 개별주 배당만 있어 연 3~4달만 들어왔다(2020년 3달 ~ 2024년 4달, 2025년 9달, 2026년 8달).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const TEMPLATE = "stock/htmx/fragments/tabsDividendHistory.jte";

/** 템플릿에 박힌 월 범위 함수들을 그대로 꺼내 돌린다. */
function load({ dividendData, filterStartDate = "", filterEndDate = "" }) {
	const source = readFileSync(join(JTE_ROOT, TEMPLATE), "utf8");
	const sandbox = { Number, String, Object, Math, dividendData, filterStartDate, filterEndDate };
	vm.createContext(sandbox);
	const bodies = ["monthlyRange", "monthsInRange", "buildMonthlyData"]
		.map((name) => extractFunction(source, name))
		.join("\n");
	vm.runInContext(`${bodies};this.__build = buildMonthlyData;`, sandbox);
	return sandbox.__build();
}

/** 실데이터의 모양: 2020-04 ~ 2026-08 사이 36개 달에만 배당이 있다. */
function realShapedDividends() {
	const monthsWithDividend = [
		"2020-04", "2020-08", "2020-12",
		"2021-04", "2021-08", "2021-11", "2021-12",
		"2022-04", "2022-08", "2022-11", "2022-12",
		"2023-04", "2023-08", "2023-11", "2023-12",
		"2024-04", "2024-08", "2024-11", "2024-12",
		"2025-01", "2025-04", "2025-06", "2025-07", "2025-08",
		"2025-09", "2025-10", "2025-11", "2025-12",
		"2026-01", "2026-02", "2026-03", "2026-04",
		"2026-05", "2026-06", "2026-07", "2026-08",
	];
	assert.equal(monthsWithDividend.length, 36, "실측 표본이 36개월이어야 한다");
	return monthsWithDividend.map((m) => ({ payDate: `${m}-15`, net: 1000 }));
}

test("전체 기간이면 달력 개월수를 분모로 쓴다", () => {
	const { labels } = load({ dividendData: realShapedDividends() });

	// 2020-04 ~ 2026-08 = 6년 4개월 + 1 = 77개월
	assert.equal(labels.length, 77, "배당이 있는 달만 세면 36이 된다 - 다른 기간과 기준이 달라진다");
	assert.equal(labels[0], "2020-04");
	assert.equal(labels[labels.length - 1], "2026-08");
});

test("배당이 없던 달도 0으로 채워 x축에 남는다", () => {
	const { labels, data } = load({ dividendData: realShapedDividends() });

	// 2020-05 ~ 2020-07 은 배당이 없던 달이다.
	for (const empty of ["2020-05", "2020-06", "2020-07"]) {
		const at = labels.indexOf(empty);
		assert.ok(at >= 0, `${empty} 이 차트에서 빠졌다`);
		assert.equal(data[at], 0, `${empty} 은 0 이어야 한다`);
	}
	assert.equal(
		data.filter((v) => v === 0).length,
		41,
		"배당이 0원인 달이 41개월이어야 한다(실측)",
	);
});

test("기간을 고르면 예전처럼 그 기간의 달력 개월수를 쓴다", () => {
	// 기간을 고르면 서버가 그 기간의 배당만 내려준다. 표본도 그렇게 맞춘다.
	const inRange = realShapedDividends().filter(
		(d) => d.payDate >= "2025-09" && d.payDate <= "2026-08-31",
	);
	const { labels } = load({
		dividendData: inRange,
		filterStartDate: "2025-09",
		filterEndDate: "2026-08",
	});

	assert.equal(labels.length, 12, "고른 기간은 그 기간의 달력 개월수여야 한다");
	assert.equal(labels[0], "2025-09");
	assert.equal(labels[11], "2026-08");
});

test("고른 기간 밖의 배당도 라벨에 더해진다(예전 동작 유지)", () => {
	// 기간을 채워 보내도 그 밖의 행이 섞여 오면 라벨이 늘어난다. 예전 코드와 같은 성질이라 함께 못박는다.
	const { labels } = load({
		dividendData: [{ payDate: "2019-01-15", net: 100 }],
		filterStartDate: "2026-01",
		filterEndDate: "2026-03",
	});

	assert.deepEqual(labels, ["2019-01", "2026-01", "2026-02", "2026-03"]);
});

test("배당이 하나도 없으면 라벨이 비고 터지지 않는다", () => {
	const { labels, data } = load({ dividendData: [] });

	assert.deepEqual(labels, []);
	assert.deepEqual(data, []);
});
