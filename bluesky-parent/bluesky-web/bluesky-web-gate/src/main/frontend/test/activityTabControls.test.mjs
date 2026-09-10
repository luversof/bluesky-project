// 활동 보기 탭의 aria-controls: 서버는 보이는 뷰 하나만 그리므로, 지연 로드로 패널이 붙은 뒤 그 탭을 패널에 잇는다.
//
// 실측 2026-09-09(qa/tabs-invalid.cjs): 탭 2개가 DOM 에 없는 패널 id 를 가리켰다. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
import assert from "node:assert/strict";
import test from "node:test";

const noop = () => {};
const stubEl = () => ({
	addEventListener: noop, removeEventListener: noop, setAttribute: noop, removeAttribute: noop,
	getAttribute: () => null, hasAttribute: () => false, querySelectorAll: () => [], querySelector: () => null,
	closest: () => null, matches: () => false, appendChild: noop, remove: noop,
	classList: { contains: () => false, add: noop, remove: noop, toggle: noop }, dataset: {}, style: {}, children: [],
});
globalThis.Element = class Element {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: () => null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
	addEventListener: noop,
});
globalThis.window = globalThis;
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.location = { search: "", href: "https://x/stock", origin: "https://x", pathname: "/stock" };
globalThis.history = { replaceState: noop, pushState: noop };
globalThis.localStorage = { getItem: () => null, setItem: noop, removeItem: noop };
globalThis.sessionStorage = globalThis.localStorage;
globalThis.addEventListener = noop;
globalThis.matchMedia = () => ({ matches: false, addEventListener: noop, addListener: noop });
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);

await import("../../resources/static/js/common.js");
const mod = globalThis.__activityTabInternals;

function fake(attrs = {}) {
	const a = { ...attrs };
	return { attrs: a, getAttribute: (n) => (n in a ? a[n] : null), hasAttribute: (n) => n in a, setAttribute: (n, v) => { a[n] = v; }, removeAttribute: (n) => { delete a[n]; } };
}
function root({ tab, panel }) {
	return { querySelector: (sel) => (sel === '[data-activity-view-tab="calendar"]' ? tab : sel === "#activityCalendarView" ? panel : null) };
}

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __activityTabInternals 를 노출하지 않는다");
});

test("패널이 붙어 있으면 탭에 aria-controls 를 채우고 패널에 role=tabpanel 을 보장한다", () => {
	const tab = fake(), panel = fake();
	assert.equal(mod.linkActivityTabToPanel(root({ tab, panel }), "calendar"), true);
	assert.equal(tab.attrs["aria-controls"], "activityCalendarView");
	assert.equal(panel.attrs.role, "tabpanel");
	const already = fake({ role: "tabpanel" });
	mod.linkActivityTabToPanel(root({ tab, panel: already }), "calendar");
	assert.equal(already.attrs.role, "tabpanel");
});

test("패널이 아직 없으면 없는 id 를 가리키지 않는다(속성 제거)", () => {
	const tab = fake({ "aria-controls": "activityCalendarView" });
	assert.equal(mod.linkActivityTabToPanel(root({ tab, panel: null }), "calendar"), false);
	assert.equal("aria-controls" in tab.attrs, false, "DOM 에 없는 id 를 가리키는 aria-controls 는 거짓 연결");
	assert.equal(mod.linkActivityTabToPanel(root({ tab: null, panel: fake() }), "calendar"), false, "탭이 없으면 할 일 없음");
	assert.equal(mod.linkActivityTabToPanel(root({ tab, panel: fake() }), "nope"), false, "모르는 보기 이름");
});
