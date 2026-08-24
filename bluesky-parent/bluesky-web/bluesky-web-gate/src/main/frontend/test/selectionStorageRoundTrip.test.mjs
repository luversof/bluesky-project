// 화면 필터 선택의 저장/복원을 검증한다.
//
// 이 모듈(950줄 남짓)은 "어떤 데이터를 보여줄지" 를 정하는데 지금까지 테스트가 없었다.
// 특히 화면마다 선택지 집합이 다르다 - 실측 2026-08-26 기준 매매 화면의 종목은 42 개인데
// 배당 화면은 18 개다. 매매에서 고른 종목이 배당 화면에 없으면 이 모듈이 옵션을 만들어 끼운다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

const store = new Map();
globalThis.sessionStorage = {
	getItem: (k) => (store.has(k) ? store.get(k) : null),
	setItem: (k, v) => store.set(k, String(v)),
	removeItem: (k) => store.delete(k),
};
globalThis.localStorage = globalThis.sessionStorage;

class Node {}
class Opt extends Node {
	constructor(value, text) {
		super();
		this.value = value;
		this.text = text;
		this.selected = false;
	}
}
class Select extends Node {
	constructor(name, opts, multiple = true) {
		super();
		this.name = name;
		this.multiple = multiple;
		this.options = opts;
	}
	get selectedOptions() {
		return this.options.filter((o) => o.selected);
	}
	get value() {
		const s = this.selectedOptions[0];
		return s ? s.value : "";
	}
	set value(v) {
		this.options.forEach((o) => (o.selected = o.value === v));
	}
	get selectedIndex() {
		return this.options.findIndex((o) => o.selected);
	}
	get firstChild() {
		return this.options[0] || null;
	}
	insertBefore(node, ref) {
		const at = ref ? this.options.indexOf(ref) : this.options.length;
		this.options.splice(at < 0 ? this.options.length : at, 0, node);
		return node;
	}
	/** "값*" 형태로 현재 목록과 선택 상태를 적는다. */
	describe() {
		return this.options
			.map((o) => (o.value || "(전체)") + (o.selected ? "*" : ""))
			.join(" | ");
	}
}
class Form extends Node {
	constructor(id, selects) {
		super();
		this.id = id;
		this.selects = selects;
	}
	querySelector(sel) {
		const m = /name="([^"]+)"/.exec(sel);
		return m ? this.selects.find((s) => s.name === m[1]) || null : null;
	}
}

globalThis.document = {
	readyState: "complete",
	addEventListener: () => {},
	removeEventListener: () => {},
	querySelectorAll: () => [],
	querySelector: () => null,
	getElementById: () => null,
	createElement: () => new Opt("", ""),
	body: { dataset: {} },
	documentElement: { lang: "ko" },
};
globalThis.window = globalThis;
globalThis.Element = Node;

await import("../../resources/static/js/stock/selectionStorage.js");
const api = globalThis.stockSelectionStorage;

function form(id, stockOptions, selectedIds = []) {
	const stocks = stockOptions.map(([v, t]) => new Opt(v, t));
	stocks.forEach((o) => {
		if (selectedIds.includes(o.value)) o.selected = true;
	});
	const select = new Select("stockItemIdList", stocks);
	return {
		form: new Form(id, [
			new Select("accountIdList", [new Opt("", "전체"), new Opt("acc1", "위탁")]),
			select,
			new Select("stockTagList", [new Opt("", "전체")]),
		]),
		select,
	};
}

const TRADE_OPTIONS = [
	["", "전체"],
	["only-trade", "에스디바이오센서"],
	["both", "삼성전자"],
];
const DIVIDEND_OPTIONS = [
	["", "전체"],
	["both", "삼성전자"],
];

test("전역 API 가 노출된다", () => {
	assert.ok(api?.saveFromForm && api?.restoreToForm && api?.GLOBAL_KEY);
});

test("같은 화면에서는 고른 그대로 되살아난다", () => {
	store.clear();
	const a = form("tradeSearchForm", TRADE_OPTIONS, ["only-trade", "both"]);
	api.saveFromForm(a.form);
	const b = form("tradeSearchForm", TRADE_OPTIONS);
	api.restoreToForm(b.form);
	assert.deepEqual(
		b.select.selectedOptions.map((o) => o.value).sort(),
		["both", "only-trade"],
	);
});

test("그 화면에 없는 종목은 만들어 끼우고 선택한다", () => {
	store.clear();
	const a = form("tradeSearchForm", TRADE_OPTIONS, ["only-trade"]);
	api.saveFromForm(a.form);
	const b = form("dividendSearchForm", DIVIDEND_OPTIONS);
	api.restoreToForm(b.form);
	assert.deepEqual(
		b.select.selectedOptions.map((o) => o.value),
		["only-trade"],
		"매매에서 고른 종목이 배당 화면에서 사라졌다",
	);
});

test("끼워 넣은 항목은 '전체' 뒤에 놓인다", () => {
	store.clear();
	const a = form("tradeSearchForm", TRADE_OPTIONS, ["only-trade"]);
	api.saveFromForm(a.form);
	const b = form("dividendSearchForm", DIVIDEND_OPTIONS);
	api.restoreToForm(b.form);
	assert.equal(
		b.select.describe(),
		"(전체) | only-trade* | both",
		"'전체' 보다 앞에 끼우면 목록 순서가 어색해진다",
	);
});

test("'전체' 옵션이 없으면 맨 앞에 끼운다", () => {
	store.clear();
	const a = form("tradeSearchForm", TRADE_OPTIONS, ["only-trade"]);
	api.saveFromForm(a.form);
	const b = form("dividendSearchForm", [["both", "삼성전자"]]);
	api.restoreToForm(b.form);
	assert.equal(b.select.describe(), "only-trade* | both");
});

test("저장된 선택이 없으면 현재 선택을 건드리지 않는다", () => {
	store.clear();
	const b = form("tradeSearchForm", TRADE_OPTIONS, ["both"]);
	api.restoreToForm(b.form);
	assert.deepEqual(b.select.selectedOptions.map((o) => o.value), ["both"]);
});

test("아무것도 안 고르고 저장하면 빈 선택으로 남는다", () => {
	store.clear();
	const a = form("tradeSearchForm", TRADE_OPTIONS);
	api.saveFromForm(a.form);
	const saved = JSON.parse(store.get(api.GLOBAL_KEY));
	assert.deepEqual(saved.stockItemIdList, []);
	// 그 상태로 복원해도 다른 화면의 선택을 되살리지 않는다.
	const b = form("dividendSearchForm", DIVIDEND_OPTIONS, ["both"]);
	api.restoreToForm(b.form);
	assert.deepEqual(b.select.selectedOptions.map((o) => o.value), ["both"]);
});
