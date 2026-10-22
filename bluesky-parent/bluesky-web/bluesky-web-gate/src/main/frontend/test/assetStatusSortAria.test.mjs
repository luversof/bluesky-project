// 자산현황 표(계좌별/종목별 현황)의 정렬 상태가 보조기술에 닿는지 지킨다.
//
// 실측 2026-09-10: 정렬 자체는 정상이었다 - 표 4개·정렬키 40개·양방향 80회 검사에서 값 순서,
// th 의 aria-sort, 행 데이터 속성, 펼침 상세가 부모 계좌를 따라가는지 모두 이상 0.
//
// 다만 이 파일은 aria-sort 를 button.parentElement 에 달고 있었다. 지금 마크업은 th > button 이라
// 맞지만, 버튼을 레이아웃용 div 로 한 겹만 감싸면 aria-sort 가 div 로 가서 th 에서 조용히 사라진다.
// 형제 구현(tableSort.ts)은 th 에 직접 달고 있어 둘이 어긋나 있었다. 그 구멍을 여기서 막는다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

const listeners = [];
globalThis.document = {
	readyState: "complete",
	addEventListener: (type, fn) => listeners.push([type, fn]),
	querySelectorAll: () => [],
	querySelector: () => null,
	getElementById: () => null,
	createElement: () => ({ id: "", className: "", style: {}, classList: { add() {}, remove() {}, contains: () => true, toggle() {} }, textContent: "", appendChild() {}, getBoundingClientRect: () => ({ width: 0, height: 0 }) }),
	documentElement: { getAttribute: () => "ko-KR" },
	body: { appendChild() {} },
};
globalThis.window = globalThis;
globalThis.addEventListener = (type, fn) => listeners.push([type, fn]);

// 2026-10-22 부터 정렬기는 자산 현황 · 배당 수익 분석이 함께 쓰는 clientTableSort.js 에 있다(레이아웃이 먼저 싣는다).
// 자산 현황 스크립트도 그 뒤에 실어 본다 - 공용 정렬기를 부르는 초기화가 가짜 문서에서 던지지 않아야 한다.
await import("../../resources/static/js/stock/clientTableSort.js");
await import("../../resources/static/js/stock/assetStatus.js");
const internals = { updateSortIndicators: globalThis.StockClientTableSort?.updateIndicators };

/** th > (감싸개)* > button 구조를 최소한으로 흉내낸다. */
function fakeHeader({ wrapped }) {
	const th = { tag: "TH", attrs: {}, setAttribute(k, v) { this.attrs[k] = v; } };
	const button = {
		dataset: { sortKey: "buyAmount" },
		querySelector: () => null,
		closest: (sel) => (/th/i.test(sel) ? th : null),
		parentElement: null,
	};
	if (wrapped) {
		const div = { tag: "DIV", attrs: {}, setAttribute(k, v) { this.attrs[k] = v; } };
		button.parentElement = div;
		th.wrapper = div;
	} else {
		button.parentElement = th;
	}
	const table = {
		dataset: { sortKey: "buyAmount", sortDirection: "asc" },
		querySelectorAll: () => [button],
	};
	return { th, button, table };
}

test("내부 규칙이 노출돼 있다", () => {
	assert.equal(typeof internals?.updateSortIndicators, "function");
});

test("정렬 상태는 th 에 붙는다", () => {
	const { th, table } = fakeHeader({ wrapped: false });

	internals.updateSortIndicators(table);

	assert.equal(th.attrs["aria-sort"], "ascending");
});

test("버튼이 한 겹 감싸여도 th 에 붙는다", () => {
	const { th, table } = fakeHeader({ wrapped: true });

	internals.updateSortIndicators(table);

	assert.equal(th.attrs["aria-sort"], "ascending", "감싸개에 붙으면 th 에서 정렬 상태가 사라진다");
	assert.equal(th.wrapper.attrs["aria-sort"], undefined);
});

test("활성 열이 아니면 none 이다", () => {
	const { th, table } = fakeHeader({ wrapped: true });
	table.dataset.sortKey = "evaluationAmount";

	internals.updateSortIndicators(table);

	assert.equal(th.attrs["aria-sort"], "none");
});
