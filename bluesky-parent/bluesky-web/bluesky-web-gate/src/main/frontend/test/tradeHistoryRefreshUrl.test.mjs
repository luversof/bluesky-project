// 기간을 바꿀 때 매매 내역 패널을 다시 부르는 주소가 나머지 조건을 이어받는지.
//
// 실측 2026-09-12: 자산 성장에서 한 계좌로 좁힌 뒤 "1년" 을 누르면, 이 갱신이 계좌·종목 필터 없이
// /stock/htmx/trade-history?from=..&to=.. 만 불러서 좁혀 놓은 표에 다섯 계좌의 거래가 보였다.
// 뒤따라 오는 뷰 전체 재조회가 덮어 주지만 그 전까지(+83ms~+135ms) 화면은 틀린 표였다.
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
};
globalThis.localStorage = globalThis.localStorage ?? { getItem: () => null };

await import("../../resources/static/js/date-range-picker.js");
await import("../../resources/static/js/stock/globalDateRange.js");

const url = globalThis.__globalDateRangeInternals.tradeHistoryRefreshUrl;

const params = (u) => {
	const q = u.indexOf("?");
	return new URLSearchParams(q >= 0 ? u.slice(q + 1) : "");
};

test("패널이 들고 있는 계좌·종목 필터를 그대로 이어받는다", () => {
	const panel =
		"/stock/htmx/trade-history?from=2020-01-01&to=2026-09-12&accountIdList=A1&accountIdList=A2&stockItemIdList=S1&size=20";
	const p = params(url(panel, "?locale=ko_KR", "2025-09-13", "2026-09-12"));
	assert.deepEqual(p.getAll("accountIdList"), ["A1", "A2"]);
	assert.deepEqual(p.getAll("stockItemIdList"), ["S1"]);
	assert.equal(p.get("size"), "20");
	assert.equal(p.get("from"), "2025-09-13");
	assert.equal(p.get("to"), "2026-09-12");
});

test("패널 속성이 없으면 주소에서 읽는다", () => {
	// outerHTML 로 갈아끼운 뒤에는 hx-get 속성이 남아 있지 않다.
	const p = params(
		url("", "?locale=ko_KR&rangeMode=all&accountIdList=A1", "2025-09-13", "2026-09-12"),
	);
	assert.deepEqual(p.getAll("accountIdList"), ["A1"]);
	assert.equal(p.get("from"), "2025-09-13");
});

test("기간을 뜻하는 값은 옛 것을 남기지 않는다", () => {
	const p = params(
		url(
			"",
			"?rangeMode=all&startDate=2010-01-01T00:00:00Z&endDate=2026-01-01T00:00:00Z&timeZone=Asia%2FSeoul&locale=ko_KR&accountIdList=A1",
			"2025-09-13",
			"2026-09-12",
		),
	);
	for (const k of ["rangeMode", "startDate", "endDate", "timeZone", "locale"]) {
		assert.equal(p.get(k), null, k + " 가 남아 있으면 옛 기간이 같이 실린다");
	}
	assert.equal(p.get("from"), "2025-09-13");
	assert.deepEqual(p.getAll("accountIdList"), ["A1"]);
});

test("기간이 비면 from/to 를 싣지 않는다", () => {
	const u = url("", "?accountIdList=A1", "", "");
	const p = params(u);
	assert.equal(p.get("from"), null);
	assert.equal(p.get("to"), null);
	assert.deepEqual(p.getAll("accountIdList"), ["A1"]);
});

test("조건이 없으면 물음표를 붙이지 않는다", () => {
	assert.equal(url("", "", "", ""), "/stock/htmx/trade-history");
});
