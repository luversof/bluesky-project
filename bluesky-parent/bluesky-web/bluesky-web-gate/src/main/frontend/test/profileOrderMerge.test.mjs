// 월배당 참조 화면의 순서 저장 규칙을 고정한다.
//
// 화면이 필터로 일부 종목만 보여 줄 때, 사용자는 보이는 것만 끌어 옮긴다. 저장할 때는 그 결과를 전체
// 순서에 다시 끼워 넣어야 한다 - 보이지 않는 종목은 원래 자리를 지키고, 보이는 자리에는 재정렬된 순서가
// 차례로 들어간다.
//
// 이 계산이 틀리면 저장되는 순서에서 종목이 조용히 사라지거나 중복되는데, 화면은 "저장했습니다" 라고만
// 말한다. 그래서 개별 사례뿐 아니라 "잃지도 늘지도 않는다" 는 성질을 무작위로도 확인한다.
import assert from "node:assert/strict";
import test from "node:test";

class FakeElement {}
globalThis.HTMLElement = FakeElement;
globalThis.Element = FakeElement;
globalThis.Event = class {
	constructor(type) {
		this.type = type;
	}
};
globalThis.document = {
	readyState: "complete",
	getElementById: () => null,
	addEventListener: () => {},
	querySelector: () => null,
	querySelectorAll: () => [],
	body: { dataset: {} },
	documentElement: { lang: "ko-KR" },
};
globalThis.window = globalThis;

await import("../../resources/static/js/stock/monthlyDividendProfileOrder.js");
const { mergeVisibleOrderIntoAll } =
	globalThis.__monthlyDividendProfileOrderInternals;

test("전체 목록을 모르면 보이는 순서를 그대로 쓴다", () => {
	assert.deepEqual(mergeVisibleOrderIntoAll([], ["B", "A"]), ["B", "A"]);
});

test("전부 보이면 재정렬 결과가 그대로 저장된다", () => {
	assert.deepEqual(
		mergeVisibleOrderIntoAll(["A", "B", "C"], ["C", "A", "B"]),
		["C", "A", "B"],
	);
});

test("보이지 않는 종목은 원래 자리를 지킨다", () => {
	// A C E 만 보이는 상태에서 A 와 E 를 맞바꿨다. B 와 D 는 2번째·4번째 자리 그대로여야 한다.
	assert.deepEqual(
		mergeVisibleOrderIntoAll(["A", "B", "C", "D", "E"], ["E", "C", "A"]),
		["E", "B", "C", "D", "A"],
	);
});

test("보이는 것이 하나뿐이면 아무것도 바뀌지 않는다", () => {
	assert.deepEqual(
		mergeVisibleOrderIntoAll(["A", "B", "C"], ["B"]),
		["A", "B", "C"],
	);
});

test("전체에 없던 종목이 보이면 뒤에 붙는다", () => {
	assert.deepEqual(
		mergeVisibleOrderIntoAll(["A", "B"], ["B", "A", "Z"]),
		["B", "A", "Z"],
	);
});

test("무작위로도 잃지도 늘지도 않는다", () => {
	// 순열이 보존되지 않으면 저장 후 종목이 사라지거나 중복된다.
	let seed = 20260823;
	const random = () => {
		seed = (seed * 1103515245 + 12345) % 2147483648;
		return seed / 2147483648;
	};
	for (let round = 0; round < 500; round += 1) {
		const size = 2 + Math.floor(random() * 8);
		const all = Array.from({ length: size }, (_, index) => `S${index}`);
		const visible = all.filter(() => random() < 0.6);
		const shuffled = [...visible];
		for (let i = shuffled.length - 1; i > 0; i -= 1) {
			const j = Math.floor(random() * (i + 1));
			[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
		}

		const merged = mergeVisibleOrderIntoAll(all, shuffled);

		assert.equal(merged.length, all.length, `길이가 달라졌다: ${merged}`);
		assert.deepEqual(
			[...merged].sort(),
			[...all].sort(),
			`구성이 달라졌다: ${merged}`,
		);
		// 보이지 않던 종목은 자리를 지켜야 한다.
		const visibleSet = new Set(visible);
		all.forEach((symbol, index) => {
			if (!visibleSet.has(symbol)) {
				assert.equal(merged[index], symbol, `${symbol} 이 자리를 벗어났다`);
			}
		});
	}
});
