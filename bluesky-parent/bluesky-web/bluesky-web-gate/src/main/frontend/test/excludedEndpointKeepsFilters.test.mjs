// 제외 목록에 든 엔드포인트라도, 호출자가 일부러 실은 계좌·종목 값까지 지우면 안 된다.
//
// 이 처리기의 목적은 "저장된 행 선택을 자동 주입하지 않는다" 이다. 그런데 프로그램으로 부른 요청에서는
// 이미 들어 있던 accountIdList/stockItemIdList 까지 경로에서 지우고 있었다.
//
// 실측 2026-09-12(자산 성장, 한 계좌로 좁힌 상태에서 "1년" 클릭): 매매 내역을 다시 부르는 요청에서 이 값이
// 지워져, 좁혀 놓은 표에 다섯 계좌의 거래가 +83ms~+135ms 동안 보였다가 뒤늦게 정정됐다. 서버가 그린 패널의
// hx-get 은 같은 이름으로 이 값을 싣고 있다 - 이 엔드포인트는 필터를 받는 계약이다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

const handlers = new Map();
const store = new Map();
globalThis.sessionStorage = {
	getItem: (k) => (store.has(k) ? store.get(k) : null),
	setItem: (k, v) => store.set(k, String(v)),
	removeItem: (k) => store.delete(k),
	key: () => null,
	length: 0,
};
globalThis.localStorage = globalThis.sessionStorage;
globalThis.location = { href: "https://example.test/stock/asset-growth" };
globalThis.Element = class Element {};
globalThis.window = { __stockSelectionStorageAttached: false };
globalThis.document = {
	readyState: "complete",
	addEventListener: (type, fn) => handlers.set(type, fn),
	removeEventListener: () => {},
	getElementById: () => null,
	querySelector: () => null,
	querySelectorAll: () => [],
	createElement: () => ({ setAttribute() {}, querySelector: () => null }),
};

await import("../../resources/static/js/stock/selectionStorage.js");
const onConfig = handlers.get("htmx:configRequest");

function run(path) {
	const ev = { detail: { path, parameters: {}, elt: null } };
	onConfig(ev);
	return ev.detail.path;
}

// 저장된 선택이 있어도 제외 엔드포인트에는 주입하지 않는다는 원래 목적을 함께 고정한다.
store.set(
	"__stockSelection:global",
	JSON.stringify({
		accountIdList: [{ id: "STORED", text: "저장된 계좌" }],
		stockItemIdList: [],
		stockTagList: [],
	}),
);

test("매매 내역: 호출자가 실은 계좌 필터는 그대로 남는다", () => {
	const out = run("/stock/htmx/trade-history?from=2025-09-13&to=2026-09-12&accountIdList=A1");
	assert.ok(
		out.includes("accountIdList=A1"),
		"호출자가 실은 필터가 지워지면 좁혀 놓은 표에 남의 계좌가 보인다: " + out,
	);
});

test("매매 내역: 저장된 선택을 대신 주입하지는 않는다", () => {
	const ev = { detail: { path: "/stock/htmx/trade-history?from=a&to=b", parameters: {}, elt: null } };
	onConfig(ev);
	assert.equal(ev.detail.parameters.accountIdList, undefined);
});

test("요약·자산현황은 그대로 지운다", () => {
	for (const p of [
		"/stock/htmx/summary?accountIdList=A1",
		"/stock/htmx/asset-status?accountIdList=A1",
		"/stock/htmx/recent-activities?accountIdList=A1",
	]) {
		assert.ok(!run(p).includes("accountIdList"), p + " 는 계속 지워야 한다");
	}
});
