// 표 행 선택 합산 공통(rowSelection.ts)을 지킨다.
//
// 2026-10-22 까지 배당 변동 요인 카드와 실현손익 표의 인라인 스크립트가 같은 setRow · putAmount · 행 결합 · 선택 해제를 한 벌씩
// 들고 있었다(검사 0). 한 벌로 모으면서 동작을 여기서 고정한다: 클릭 · Enter · Space 토글, 행 안 버튼 · 링크 클릭은 무시,
// 선택 해제, 금액 부호(반올림 뒤 0 은 부호 없음) · 손익 색 · 앱 로케일 자릿수.
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

globalThis.window = globalThis;
globalThis.appLocale = () => "en-US";
await import("../../resources/static/js/stock/rowSelection.js");
const RS = globalThis.StockRowSelection;

function classList() {
	const set = new Set();
	return {
		set,
		toggle(c, on) {
			if (on === undefined ? !set.has(c) : on) set.add(c);
			else set.delete(c);
		},
		contains: (c) => set.has(c),
	};
}

function fakeRow() {
	const attrs = {};
	const listeners = {};
	return {
		attrs,
		classList: classList(),
		setAttribute(k, v) {
			attrs[k] = v;
		},
		getAttribute: (k) => (k in attrs ? attrs[k] : null),
		addEventListener(type, fn) {
			(listeners[type] ||= []).push(fn);
		},
		fire(type, init = {}) {
			let prevented = false;
			const event = { target: init.target || { closest: () => null }, key: init.key, preventDefault: () => (prevented = true) };
			for (const fn of listeners[type] || []) fn.call(this, event);
			return prevented;
		},
		listeners,
	};
}

function fakeContainer(rows, withClear = true) {
	const clearListeners = [];
	const clear = withClear ? { addEventListener: (t, fn) => clearListeners.push(fn) } : null;
	return {
		querySelectorAll(sel) {
			if (sel === "[data-row]") return rows;
			if (sel === '[data-row][aria-selected="true"]') return rows.filter((r) => r.attrs["aria-selected"] === "true");
			throw new Error("unexpected selector " + sel);
		},
		querySelector: (sel) => (sel === "[data-clear]" ? clear : null),
		clickClear: () => clearListeners.forEach((fn) => fn()),
	};
}

const selected = (rows) => rows.map((r) => r.attrs["aria-selected"] === "true");

test("클릭 · Enter · Space 로 토글하고, 다른 키는 무시한다", () => {
	const rows = [fakeRow(), fakeRow()];
	let updates = 0;
	RS.bind(fakeContainer(rows), "[data-row]", "[data-clear]", () => updates++);

	rows[0].fire("click");
	assert.deepEqual(selected(rows), [true, false]);
	assert.equal(rows[0].classList.contains("bg-primary/10"), true);

	assert.equal(rows[1].fire("keydown", { key: "Enter" }), true);
	assert.equal(rows[1].fire("keydown", { key: " " }), true);
	assert.equal(rows[1].fire("keydown", { key: "a" }), false);
	assert.deepEqual(selected(rows), [true, false]);

	rows[0].fire("click");
	assert.deepEqual(selected(rows), [false, false]);
	assert.equal(rows[0].classList.contains("bg-primary/10"), false);
	assert.equal(updates, 4);
});

test("행 안 버튼 · 링크 · 입력에서 온 클릭 · 키는 선택을 바꾸지 않는다", () => {
	const rows = [fakeRow()];
	let updates = 0;
	RS.bind(fakeContainer(rows), "[data-row]", "[data-clear]", () => updates++);
	const asked = [];
	const inner = { closest: (sel) => (asked.push(sel), {}) };
	rows[0].fire("click", { target: inner });
	assert.equal(rows[0].fire("keydown", { target: inner, key: "Enter" }), false);
	assert.deepEqual(selected(rows), [false]);
	assert.equal(updates, 0);
	assert.equal(asked[0], "button, a, input, label, select, textarea");
});

test("선택 해제는 고른 행만 풀고 update 를 부른다 - 해제 버튼이 없어도 묶기는 된다", () => {
	const rows = [fakeRow(), fakeRow(), fakeRow()];
	let updates = 0;
	const c = fakeContainer(rows);
	RS.bind(c, "[data-row]", "[data-clear]", () => updates++);
	rows[0].fire("click");
	rows[2].fire("click");
	c.clickClear();
	assert.deepEqual(selected(rows), [false, false, false]);
	assert.equal(updates, 3);

	const lone = [fakeRow()];
	RS.bind(fakeContainer(lone, false), "[data-row]", "[data-clear]", () => {});
	lone[0].fire("click");
	assert.deepEqual(selected(lone), [true]);
});

test("putAmount: 원화 · 앱 로케일 자릿수 · 반올림 뒤 0 은 부호 없음 · 손익 색", () => {
	const cells = {};
	const summary = { querySelector: (sel) => (cells[sel] ||= { textContent: "", classList: classList() }) };

	RS.putAmount(summary, "[a]", 1234567.4, false);
	assert.equal(cells["[a]"].textContent, "₩1,234,567");
	assert.equal(cells["[a]"].classList.set.size, 0);

	RS.putAmount(summary, "[b]", 1500.5, true);
	assert.equal(cells["[b]"].textContent, "+₩1,501");
	assert.deepEqual([...cells["[b]"].classList.set], ["text-profit"]);

	RS.putAmount(summary, "[b]", -2000, true);
	assert.equal(cells["[b]"].textContent, "-₩2,000");
	assert.deepEqual([...cells["[b]"].classList.set], ["text-loss"]);

	// -0.4 는 0 으로 반올림된다 - 부호 없이 적는다(색은 값의 부호를 따른다: 예전 인라인 스크립트와 같다).
	RS.putAmount(summary, "[c]", -0.4, true);
	assert.equal(cells["[c]"].textContent, "₩0");

	// 칸이 없으면 아무것도 안 한다.
	RS.putAmount({ querySelector: () => null }, "[x]", 1, true);
});

test("두 인라인 스크립트는 공통을 다시 들고 있지 않고, 레이아웃이 본문보다 먼저 싣는다", () => {
	for (const f of ["dividend/dividendSummaryCards.jte", "trade/tradeRealizedSections.jte"]) {
		const src = readFileSync(`../jte/stock/htmx/fragments/${f}`, "utf8");
		assert.doesNotMatch(src, /function (setRow|putAmount)\b/, f);
		assert.match(src, /RS\.bind\(/, f);
	}
	const layout = readFileSync("../jte/_layout/stockLayout.jte", "utf8");
	const at = layout.indexOf('<script src="/js/stock/rowSelection.js"></script>');
	assert.ok(at > 0, "레이아웃이 싣지 않는다");
	assert.ok(at < layout.indexOf("${contentStock}"), "본문보다 늦게 실린다 - 인라인 스크립트가 먼저 돈다");
});
