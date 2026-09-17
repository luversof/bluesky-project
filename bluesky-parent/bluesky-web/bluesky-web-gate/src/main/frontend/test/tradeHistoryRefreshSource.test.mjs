// 같은 탭의 피커에서 온 기간 변경은 매매 이력 패널을 따로 부르지 않는다 - 화면 자체 재조회가 패널까지 다시 그린다.
//
// 실측 2026-09-17(자산 성장, 1년·전체 3회씩): 기간을 바꿀 때마다 /stock/htmx/trade-history(TTFB 38~54ms, 29KB)가
// asset-growth/view 와 함께 나갔다. 따로 나간 것이 먼저 패널에 들어가고 ~50ms 뒤 뷰가 패널째 덮었다(6/6) -
// 서버가 같은 표를 두 번 만들고 하나를 버렸다. 뷰는 응답 안에 같은 기간의 매매 이력을 함께 넣는다.
//
// 다른 탭에서 온 변경(storage 이벤트)은 화면을 다시 부르지 않는다 - 그때는 이 갱신이 유일하므로 남긴다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

const listeners = new Map();
globalThis.window = globalThis.window ?? {};
globalThis.window.addEventListener = (type, fn) => {
	if (!listeners.has(type)) listeners.set(type, []);
	listeners.get(type).push(fn);
};
const ajaxCalls = [];
globalThis.window.htmx = { ajax: (method, url) => ajaxCalls.push(url) };
const panel = { getAttribute: () => "" };
globalThis.document = {
	getElementById: (id) => (id === "trade-history-panel" ? panel : null),
	querySelectorAll: () => [],
	addEventListener: () => {},
};
globalThis.localStorage = { getItem: () => null };

await import("../../resources/static/js/date-range-picker.js");
await import("../../resources/static/js/stock/globalDateRange.js");

const internals = globalThis.__globalDateRangeInternals;

function fire(detail) {
	const fns = listeners.get("globalDateRange:changed") || [];
	assert.ok(fns.length > 0, "기간 변경 핸들러가 등록돼 있어야 한다");
	for (const fn of fns) fn({ detail });
}

test("판단: 같은 탭 피커에서 온 변경이면 부르지 않는다", () => {
	assert.equal(internals.shouldRefreshTradeHistory({ start: "2025-09-18", end: "2026-09-17", source: "picker" }), false);
});

test("판단: 다른 탭(storage)에서 온 변경이면 부른다", () => {
	assert.equal(internals.shouldRefreshTradeHistory({ start: "2025-09-18", end: "2026-09-17" }), true);
	assert.equal(internals.shouldRefreshTradeHistory(null), true);
});

test("동작: 피커에서 온 변경은 매매 이력 요청을 보내지 않는다", () => {
	ajaxCalls.length = 0;
	fire({ start: "2024-01-01", end: "2024-12-31", mode: "", source: "picker" });
	assert.equal(ajaxCalls.length, 0);
});

test("동작: 다른 탭에서 온 변경은 매매 이력 요청을 한 번 보낸다", () => {
	ajaxCalls.length = 0;
	fire({ start: "2023-01-01", end: "2023-12-31", mode: "" });
	assert.equal(ajaxCalls.length, 1);
	assert.ok(ajaxCalls[0].indexOf("/stock/htmx/trade-history") === 0, ajaxCalls[0]);
});

// 피커가 이벤트를 보내는 자리가 늘어나거나 한 곳에서 출처를 빠뜨리면, 그 자리에서만 조용히 중복 조회가 되살아난다.
test("산출물: 피커가 기간 변경을 보내는 모든 자리가 출처를 단다", () => {
	const built = readFileSync(new URL("../../resources/static/js/date-range-picker.js", import.meta.url), "utf8");
	const marker = 'CustomEvent("globalDateRange:changed"';
	let at = built.indexOf(marker);
	let sites = 0;
	while (at >= 0) {
		sites++;
		// 그 이벤트의 여는 괄호부터 짝이 맞는 닫는 괄호까지가 한 발송이다.
		let depth = 0;
		let end = at + marker.length - 1;
		for (let i = at + "CustomEvent".length; i < built.length; i++) {
			const c = built[i];
			if (c === "(") depth++;
			else if (c === ")") {
				depth--;
				if (depth === 0) {
					end = i;
					break;
				}
			}
		}
		const call = built.slice(at, end + 1);
		assert.match(call, /source:\s*"picker"/, "출처가 빠진 발송: " + call.slice(0, 120));
		at = built.indexOf(marker, end);
	}
	assert.ok(sites >= 2, "피커의 발송 자리를 못 찾았다(" + sites + ")");
});
