// 브라우저 표 정렬(clientTableSort.ts)을 지킨다 - 자산 현황 · 배당 수익 분석이 함께 쓴다.
//
// 2026-10-22 까지 두 조각 스크립트가 같은 정렬기를 한 벌씩 들고 있었다(검사는 aria-sort 위치 하나뿐). 한 벌로 모으면서 동작을 고정한다:
// 숫자 열은 내림차순 · 글자 열은 오름차순부터, 같은 열을 다시 누르면 뒤집기, 값이 같으면 이름 키 순, 펼침 상세 줄은 부모 줄을 따라 움직이기,
// 버튼은 표마다 한 번만 묶기, 정렬 상태(aria-sort · 표시)는 th 에.
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

const byId = {};
let tables = [];
globalThis.window = globalThis;
globalThis.document = {
	getElementById: (id) => byId[id] || null,
	querySelectorAll: () => tables,
};
await import("../../resources/static/js/stock/clientTableSort.js");
const S = globalThis.StockClientTableSort;

const ROWS = "[data-sortable-row]";
const OPTIONS = { tableSelector: "[data-sortable-table]", rowSelector: ROWS, tieBreakKeys: ["account", "stockName"] };

function row(name, data, detailId) {
	const r = { name, dataset: { ...data }, sortable: true, parentElement: null };
	if (detailId) {
		r.dataset.detailRowId = detailId;
		const d = { name: name + "-detail", dataset: {}, sortable: false, parentElement: null };
		byId[detailId] = d;
		r.detail = d;
	}
	return r;
}

function header(sortKey, sortType) {
	const th = { attrs: {}, setAttribute(k, v) { this.attrs[k] = v; } };
	const indicator = { textContent: "↕" };
	const listeners = [];
	const button = {
		dataset: { sortKey, sortType },
		th,
		indicator,
		listeners,
		parentElement: th,
		querySelector: (sel) => (sel === "[data-sort-indicator]" ? indicator : null),
		closest: (sel) => (sel === "th, td" ? th : sel === OPTIONS.tableSelector ? button.table : null),
		addEventListener: (type, fn) => listeners.push(fn),
		click() {
			for (const fn of listeners) fn.call(button);
		},
	};
	return button;
}

function table(rows, buttons) {
	const tbody = {
		children: [],
		appendChild(n) {
			const i = this.children.indexOf(n);
			if (i >= 0) this.children.splice(i, 1);
			this.children.push(n);
			n.parentElement = this;
		},
		querySelectorAll(sel) {
			assert.equal(sel, ROWS);
			return this.children.filter((c) => c.sortable);
		},
	};
	for (const r of rows) {
		tbody.appendChild(r);
		if (r.detail) tbody.appendChild(r.detail);
	}
	const t = {
		dataset: {},
		tBodies: [tbody],
		querySelectorAll: (sel) => (sel === "[data-sort-key]" ? buttons : []),
	};
	for (const b of buttons) b.table = t;
	return t;
}

const order = (t) => t.tBodies[0].children.map((c) => c.name);

test("숫자 열은 큰 것부터, 다시 누르면 뒤집는다 · 글자 열은 가나다 순부터", () => {
	const t = table([row("a", { buy: "10", account: "나" }), row("b", { buy: "30", account: "가" }), row("c", { buy: "20", account: "다" })], []);
	S.sortTable(t, "buy", "number", OPTIONS);
	assert.deepEqual(order(t), ["b", "c", "a"]);
	assert.equal(t.dataset.sortDirection, "desc");
	S.sortTable(t, "buy", "number", OPTIONS);
	assert.deepEqual(order(t), ["a", "c", "b"]);
	S.sortTable(t, "account", "text", OPTIONS);
	assert.deepEqual(order(t), ["b", "a", "c"]);
	assert.equal(t.dataset.sortDirection, "asc");
});

test("숫자가 아닌 값은 0, 값이 같으면 이름 키(앞 키부터 값이 있는 것) 순", () => {
	const t = table(
		[
			row("x", { buy: "abc", stockName: "삼성" }),
			row("y", { buy: "5", account: "연금" }),
			row("z", { buy: "5", stockName: "가나" }),
		],
		[],
	);
	S.sortTable(t, "buy", "number", OPTIONS);
	// 내림차순: 5 두 줄(이름 가나 < 연금 이 뒤집혀 연금 · 가나) 다음 0.
	assert.deepEqual(order(t), ["y", "z", "x"]);
});

test("펼침 상세 줄은 부모 줄을 따라 움직인다", () => {
	const t = table([row("a", { buy: "1" }, "d-a"), row("b", { buy: "2" }, "d-b")], []);
	S.sortTable(t, "buy", "number", OPTIONS);
	assert.deepEqual(order(t), ["b", "b-detail", "a", "a-detail"]);
});

test("버튼은 표마다 한 번만 묶고, 정렬 상태는 th 와 표시에", () => {
	const buy = header("buy", "number");
	const name = header("account", "text");
	const t = table([row("a", { buy: "1", account: "나" }), row("b", { buy: "2", account: "가" })], [buy, name]);
	tables = [t];
	S.initialize(OPTIONS);
	S.initialize(OPTIONS);
	assert.equal(buy.listeners.length, 1);
	assert.equal(buy.th.attrs["aria-sort"], "none");

	buy.click();
	assert.deepEqual(order(t), ["b", "a"]);
	assert.equal(buy.th.attrs["aria-sort"], "descending");
	assert.equal(buy.indicator.textContent, "▼");
	assert.equal(name.th.attrs["aria-sort"], "none");
	assert.equal(name.indicator.textContent, "↕");

	name.click();
	assert.equal(name.th.attrs["aria-sort"], "ascending");
	assert.equal(name.indicator.textContent, "▲");
	assert.equal(buy.th.attrs["aria-sort"], "none");
});
