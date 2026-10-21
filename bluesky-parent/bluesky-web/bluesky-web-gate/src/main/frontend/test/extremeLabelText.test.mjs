// 자산 성장 차트의 최고 · 최저 표시 글자(사용자 요청 2026-10-21): 그 지점 평가액 대신 "최고 대비 / 최저 대비 손익" 금액.
// 금액 = 지금 - 그 지점. 부호는 반올림 뒤에 정하고 0 엔 붙이지 않는다. 다른 차트(계좌 상세)는 예전처럼 그 지점 평가액.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.window.addEventListener = globalThis.window.addEventListener ?? (() => {});
globalThis.document = globalThis.document ?? {
	getElementById: () => null,
	querySelectorAll: () => [],
	addEventListener: () => {},
	documentElement: { lang: "ko" },
};

await import("../../resources/static/js/stock-charts.js");
const SC = globalThis.window.StockCharts;

test("최고 대비는 지금 - 최고(하락이면 -)", () => {
	assert.equal(
		SC.extremeLabelText(true, "최고 대비", 150000000, 142150000, true),
		"▼ 최고 대비 -" + SC.formatCurrency(7850000) + " (-5.23%)",
	);
});

test("최저 대비는 지금 - 최저(오르면 +)", () => {
	assert.equal(
		SC.extremeLabelText(false, "최저 대비", 100000000, 142150000, true),
		"▲ 최저 대비 +" + SC.formatCurrency(42150000) + " (+42.15%)",
	);
});

test("0 엔 부호가 없다 - 반올림 뒤 판정", () => {
	assert.equal(
		SC.extremeLabelText(true, "최고 대비", 100000000, 100000000.4, true),
		"▼ 최고 대비 " + SC.formatCurrency(0) + " (0.00%)",
	);
});

test("손익 모드가 아니면 예전처럼 그 지점 평가액", () => {
	assert.equal(
		SC.extremeLabelText(true, "최고", 150000000, 142150000, false),
		"▼ 최고 " + SC.formatCurrency(150000000) + " (-5.23%)",
	);
});
