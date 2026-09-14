// 활동 화면의 세 뷰는 문서 제목이 서로 달라야 한다.
//
// 실측 2026-09-12: 캘린더·타임라인·목록 세 뷰가 "활동 내역 · Bluesky Stock" 한 종류를 써서
// 브라우저 탭·기록·즐겨찾기에서 구분되지 않았다. 시뮬레이터(3탭 3종)·관리(2탭 2종)·배당(2탭 2종)은 이미 이름을 붙인다.
//
// 이 화면의 전환은 브라우저 안에서 패널을 갈아 끼운다. 이미 붙어 있는 패널로 되돌아가면 요청이 없어서
// 조각의 [data-page-title] 기제로는 닿지 않는다 - 그래서 탭이 data-view-page-title 로 제목을 들고 있다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다. (준비 코드는 activityTabControls.test.mjs 와 같은 스텁)
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

const noop = () => {};
const stubEl = () => ({
	addEventListener: noop, removeEventListener: noop, setAttribute: noop, removeAttribute: noop,
	getAttribute: () => null, hasAttribute: () => false, querySelectorAll: () => [], querySelector: () => null,
	closest: () => null, matches: () => false, appendChild: noop, remove: noop,
	classList: { contains: () => false, add: noop, remove: noop, toggle: noop }, dataset: {}, style: {}, children: [],
});
globalThis.Element = class Element {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "활동 내역 · Bluesky Stock",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: noop,
});
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.location = { search: "", href: "https://x/stock/activity", origin: "https://x", pathname: "/stock/activity" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const { applyActivityViewTitle, pageTitleFor } = globalThis.__pageTitleInternals;

/** 탭 세 개를 가진 최소 루트. */
const rootWith = (titles) => ({
	querySelector(sel) {
		const m = sel.match(/data-activity-view-tab="([^"]+)"/);
		if (!m) return null;
		const name = titles[m[1]];
		if (name === undefined) return null;
		return { getAttribute: (a) => (a === "data-view-page-title" ? name : null) };
	},
});

const TITLES = {
	calendar: "활동 내역 · 캘린더",
	timeline: "활동 내역 · 타임라인",
	list: "활동 내역 · 목록",
};

test("뷰마다 다른 제목이 된다", () => {
	const seen = new Set();
	for (const mode of ["calendar", "timeline", "list"]) {
		document.title = "활동 내역 · Bluesky Stock";
		applyActivityViewTitle(rootWith(TITLES), mode);
		seen.add(document.title);
	}
	assert.equal(seen.size, 3, "세 뷰가 서로 다른 제목을 가져야 한다: " + [...seen].join(" | "));
	assert.ok([...seen].every((t) => t.endsWith(" · Bluesky Stock")), "꼬리표는 그대로 붙는다");
});

test("탭이나 제목이 없으면 건드리지 않는다", () => {
	document.title = "그대로";
	applyActivityViewTitle(rootWith({}), "calendar");
	assert.equal(document.title, "그대로");
	applyActivityViewTitle(rootWith({ calendar: "" }), "calendar");
	assert.equal(document.title, "그대로");
});

test("전환 때마다 불린다", () => {
	// 이미 붙어 있는 패널로 되돌아가는 전환도 applyActivityView 를 거친다.
	const built = readFileSync(new URL("../../resources/static/js/common.js", import.meta.url), "utf8").replace(/\s+/g, "");
	// 정의부(function applyActivityViewTitle(root,mode){)도 같은 문자열을 담는다 - 호출부를 특정해야 한다.
	// 실측 2026-09-12: 정의부만 보는 단언은 호출을 지운 변이를 그대로 통과시켰다.
	assert.ok(
		built.includes("applyActivityTabState(root,mode),applyActivityViewTitle(root,mode)"),
		"applyActivityView 가 탭 상태 뒤에 제목까지 갱신해야 한다",
	);
});

test("템플릿이 뷰마다 제목을 싣는다", () => {
	const jte = readFileSync(new URL("../../jte/stock/htmx/fragments/activityList.jte", import.meta.url), "utf8");
	for (const v of ["calendar", "timeline", "list"]) {
		assert.ok(
			jte.includes('data-activity-view-tab="' + v + '" data-view-page-title='),
			v + " 탭에 data-view-page-title 이 없다",
		);
	}
});
