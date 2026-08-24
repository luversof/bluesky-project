// 선택 저장/초기화가 화면 언어와 무관하게 동작하는지 검증한다.
//
// 예전에는 버튼의 표시 텍스트가 '초기화'/'조회' 인지도 함께 봤다. 그러면 영어 화면의
// 'Reset'/'Search' 는 걸리지 않아 같은 버튼이 언어에 따라 다르게 동작한다. 실측으로 주식 화면의
// 저장/초기화 버튼 8 개가 모두 data-selection-* 속성을 달고 있어 텍스트 비교는 죽은 경로였고,
// 지금은 속성만 본다. 그 성질을 고정한다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

const listeners = new Map();
const store = new Map();

globalThis.sessionStorage = {
	getItem: (k) => (store.has(k) ? store.get(k) : null),
	setItem: (k, v) => store.set(k, String(v)),
	removeItem: (k) => store.delete(k),
};
globalThis.localStorage = globalThis.sessionStorage;
globalThis.document = {
	readyState: "complete",
	addEventListener: (type, fn) => {
		if (!listeners.has(type)) listeners.set(type, []);
		listeners.get(type).push(fn);
	},
	removeEventListener: () => {},
	querySelectorAll: () => [],
	querySelector: () => null,
	getElementById: () => null,
	body: { dataset: {} },
	documentElement: { lang: "en" },
};
globalThis.window = globalThis;
globalThis.Element = class Element {};

await import("../../resources/static/js/stock/selectionStorage.js");

const KEY = globalThis.stockSelectionStorage?.GLOBAL_KEY;

/** data-selection-* 속성을 가진 최소 트리거. Element 상속이어야 instanceof 를 통과한다. */
function trigger(attrs, text) {
	return Object.assign(new globalThis.Element(), {
		textContent: text,
		getAttribute: (name) => (name in attrs ? attrs[name] : null),
	});
}

function fireConfigRequest(triggerElt) {
	const params = { accountIdList: ["a"], stockItemIdList: ["s"] };
	const event = {
		type: "htmx:configRequest",
		detail: { parameters: params, elt: triggerElt, triggeringEvent: null },
		target: triggerElt,
	};
	for (const fn of listeners.get("htmx:configRequest") || []) fn(event);
	return params;
}

test("모듈이 전역 API 를 붙인다", () => {
	assert.ok(globalThis.stockSelectionStorage, "stockSelectionStorage 가 없다");
	assert.ok(KEY, "GLOBAL_KEY 가 없다");
	assert.ok(listeners.has("htmx:configRequest"), "htmx:configRequest 리스너가 없다");
});

test("영어 라벨이어도 속성이 있으면 초기화된다", () => {
	store.set(KEY, JSON.stringify({ accountIdList: [{ id: "a" }] }));
	const params = fireConfigRequest(
		trigger({ "data-selection-reset": "true" }, "Reset"),
	);
	assert.equal(store.has(KEY), false, "저장된 선택이 지워지지 않았다");
	assert.equal(params.accountIdList, undefined, "선택 파라미터가 남아 있다");
	assert.equal(params.stockItemIdList, undefined);
});

test("한글 라벨이어도 속성이 없으면 초기화되지 않는다", () => {
	store.set(KEY, JSON.stringify({ accountIdList: [{ id: "a" }] }));
	fireConfigRequest(trigger({}, "초기화"));
	assert.equal(store.has(KEY), true, "속성 없이 텍스트만으로 초기화됐다 - 언어에 따라 동작이 갈린다");
});

test("소스에 화면 라벨 비교가 남아 있지 않다", async () => {
	const { readFile } = await import("node:fs/promises");
	const source = await readFile(
		new URL("../src/stock/selectionStorage.ts", import.meta.url),
		"utf8",
	);
	const code = source
		.replace(/\/\*[\s\S]*?\*\//g, " ")
		.replace(/\/\/[^\n]*/g, " ");
	for (const banned of ['=== "초기화"', '=== "조회"']) {
		assert.ok(!code.includes(banned), `${banned} 비교가 남아 있다`);
	}
});
