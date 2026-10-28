// 매매 도넛 상위 10 + 기타 · 날짜 축 해 눈금(2026-10-27 화면 검토).
//
// 실측: 매매 도넛(전체 기간)이 43 조각에 색 12 개를 돌려 같은 색이 3~4 번 나왔고 23 조각은 1% 미만이었다. 자산 성장은 17 년치
// 날짜 눈금 52 개를 45 도로 눕혀 아래 차트 높이의 27% 가 축이었다. 빌드 산출물을 그대로 부른다.
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.window.addEventListener = globalThis.window.addEventListener ?? (() => {});
globalThis.document = globalThis.document ?? {
	getElementById: () => null,
	querySelectorAll: () => [],
	addEventListener: () => {},
};

await import("../../resources/static/js/stock-charts.js");

const { buildDonutData, yearTickLabel, logTickLabel } = globalThis.window.__monthlyChartInternals;

const buys = (n) =>
	Array.from({ length: n }, (_, i) => ({ type: "BUY", stockItem: "S" + i, amount: String(1000 - i) }));

test("종목이 11 개를 넘으면 상위 10 개 + 기타 한 조각", () => {
	const d = buildDonutData(buys(43), "buy", "stock");
	assert.equal(d.labels.length, 11);
	assert.equal(d.labels[0], "S0");
	assert.match(d.labels[10], /33/);
	const tail = Array.from({ length: 33 }, (_, i) => 1000 - (i + 10)).reduce((a, b) => a + b, 0);
	assert.equal(d.data[10], tail);
	assert.equal(new Set(d.colors.slice(0, 10)).size, 10, "남은 조각 색이 겹치지 않는다");
	assert.ok(d.colors.every(Boolean), "색 없는 조각이 없다");
});

test("11 개까지는 그대로, 계좌별은 묶지 않는다", () => {
	assert.equal(buildDonutData(buys(11), "buy", "stock").labels.length, 11);
	const byAccount = buys(20).map((r) => ({ ...r, account: r.stockItem }));
	assert.equal(buildDonutData(byAccount, "buy", "account").labels.length, 20);
});

test("손익 기타 조각의 크기는 절댓값의 합 - 이익과 손실이 서로 지우지 않는다", () => {
	const sells = Array.from({ length: 13 }, (_, i) => ({
		type: "SELL",
		stockItem: "S" + i,
		profit: String(i < 10 ? 1000 : i === 10 ? 50 : -50),
	}));
	const d = buildDonutData(sells, "profit", "stock");
	assert.equal(d.labels.length, 11);
	assert.equal(d.data[10], 150);
	assert.equal(d.rawData[10], -50);
});

test("해 눈금: 해가 바뀌는 점에만, 늦게 시작한 첫 해는 비운다", () => {
	const labels = ["2009-10-06", "2009-11-30", "2009-12-31", "2010-01-31", "2010-02-28", "2011-01-31", "2012-01-31"];
	const shown = labels.map((_, i) => yearTickLabel(labels, i, 2009, 2012, 1600));
	assert.deepEqual(shown, [null, null, null, "2010", null, "2011", "2012"]);
	const early = ["2009-03-31", "2009-04-30", "2010-01-31"];
	assert.equal(yearTickLabel(early, 0, 2009, 2012, 1600), "2009");
});

test("해 눈금: 폭이 좁으면 몇 해씩 건너뛴다", () => {
	const labels = Array.from({ length: 18 }, (_, i) => 2009 + i + "-01-31");
	const shown = labels.map((_, i) => yearTickLabel(labels, i, 2009, 2026, 300)).filter(Boolean);
	assert.ok(shown.length <= 5, shown.join(","));
	assert.equal(shown[0], "2009");
});

// 2026-10-28: 자산 성장 로그 눈금은 1 · 2 · 5 x 10^n 에만 글자를 단다(다 달면 한 자리에 3~9 가 겹친다).
test("로그 눈금 글자는 1 · 2 · 5 배에만", () => {
	for (const v of [1e5, 2e6, 5e8, 1e9]) assert.notEqual(logTickLabel(v), "", String(v));
	for (const v of [3e8, 7e6, 0, -1, 1.5e8]) assert.equal(logTickLabel(v), "", String(v));
});
