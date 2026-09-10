// 탭 패널의 이름: aria-controls 를 가진 탭마다 id 를 보장하고 패널에 aria-labelledby 를 채운다(선택된 탭 우선).
//
// 실측 2026-09-09(qa/tabs-aria.cjs): role=tabpanel 8개 중 aria-labelledby 0. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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
const byId = {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: (id) => byId[id] || null, createElement: () => stubEl(), createTextNode: () => ({}), readyState: "complete",
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
const mod = globalThis.__tabPanelLabelInternals;

function el(attrs = {}) {
	const a = { ...attrs }; let id = attrs.id || "";
	return { attrs: a, get id() { return id; }, set id(v) { id = v; a.id = v; }, getAttribute: (n) => (n === "id" ? id || null : n in a ? a[n] : null), hasAttribute: (n) => (n === "id" ? !!id : n in a), setAttribute: (n, v) => { if (n === "id") id = v; a[n] = v; } };
}
const root = (tabs) => ({ querySelectorAll: (sel) => (sel === '[role="tab"][aria-controls]' ? tabs : []) });

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __tabPanelLabelInternals 를 노출하지 않는다");
});

test("id 없는 탭에 id 를 주고 패널에 aria-labelledby 를 채운다", () => {
	const panel = el({ id: "assetGrowthPanel-yearly", role: "tabpanel" }); byId[panel.id] = panel;
	const tab = el({ "aria-controls": "assetGrowthPanel-yearly", "aria-selected": "true" });
	assert.equal(mod.labelTabPanels(root([tab])), 1);
	assert.equal(tab.id, "tab-for-assetGrowthPanel-yearly");
	assert.equal(panel.attrs["aria-labelledby"], "tab-for-assetGrowthPanel-yearly");
	assert.equal(mod.labelTabPanels(root([tab])), 0, "두 번째는 할 일이 없다");
});

test("한 패널을 나눠 쓰는 탭들은 선택된 탭이 이름이 된다", () => {
	const legend = el({ id: "tradeDonutLegend", role: "tabpanel" }); byId[legend.id] = legend;
	const stock = el({ id: "tabStock", "aria-controls": "tradeDonutLegend", "aria-selected": "false" });
	const account = el({ id: "tabAccount", "aria-controls": "tradeDonutLegend", "aria-selected": "true" });
	mod.labelTabPanels(root([stock, account]));
	assert.equal(legend.attrs["aria-labelledby"], "tabAccount");
	stock.attrs["aria-selected"] = "true"; account.attrs["aria-selected"] = "false";
	mod.labelTabPanels(root([stock, account]));
	assert.equal(legend.attrs["aria-labelledby"], "tabStock", "선택이 바뀌면 이름도 따라간다");
});

test("패널이 없거나 tabpanel 이 아니면 건너뛴다", () => {
	const notPanel = el({ id: "canvasX", role: "img" }); byId[notPanel.id] = notPanel;
	const t1 = el({ "aria-controls": "missing" }), t2 = el({ "aria-controls": "canvasX" });
	assert.equal(mod.labelTabPanels(root([t1, t2])), 0);
	assert.equal(t1.id, "", "패널이 없으면 id 도 만들지 않는다");
	assert.equal("aria-labelledby" in notPanel.attrs, false);
});
