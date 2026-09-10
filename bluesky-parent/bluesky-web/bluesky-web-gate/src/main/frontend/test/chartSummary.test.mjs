// 차트 텍스트 대안: Chart.js 플러그인이 차트마다 sr-only 요약을 캔버스 뒤에 두고 aria-describedby 로 잇는다(WCAG 1.1.1).
//
// 실측 2026-09-10(qa/chart-alt.cjs): 캔버스 15개 중 13개가 같은 카드 안에 표·목록이 없어 보조기술엔 aria-label(제목)만 닿았다.
// 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
import assert from "node:assert/strict";
import test from "node:test";

const noop = () => {};
const stubEl = () => ({
	addEventListener: noop, removeEventListener: noop, setAttribute: noop, removeAttribute: noop,
	getAttribute: () => null, hasAttribute: () => false, querySelectorAll: () => [], querySelector: () => null,
	closest: () => null, matches: () => false, appendChild: noop, remove: noop,
	classList: { contains: () => false, add: noop, remove: noop, toggle: noop }, dataset: {}, style: {}, children: [],
});
const byId = new Map();
globalThis.Element = class Element {};
globalThis.document = Object.assign(stubEl(), {
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "t",
	getElementById: (id) => byId.get(id) || null,
	createElement: (tag) => Object.assign(stubEl(), { tag, id: "", className: "", textContent: "", attrs: {}, setAttribute(n, v) { this.attrs[n] = v; }, remove() { byId.delete(this.id); this.removed = true; } }),
	createTextNode: () => ({}), readyState: "complete", addEventListener: noop,
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
const mod = globalThis.__chartSummaryInternals;

function fakeCanvas(id) {
	const parent = { inserted: [], insertBefore(el, ref) { this.inserted.push([el, ref]); byId.set(el.id, el); } };
	const canvas = { id, attrs: {}, nextSibling: "NEXT", parentElement: parent, setAttribute(n, v) { this.attrs[n] = v; }, removeAttribute(n) { delete this.attrs[n]; } };
	return { canvas, parent };
}

test("내부 함수와 플러그인이 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __chartSummaryInternals 를 노출하지 않는다");
	assert.equal(mod.chartSummaryPlugin.id, "a11ySummary");
	assert.equal(typeof mod.chartSummaryPlugin.afterInit, "function");
	assert.equal(typeof mod.chartSummaryPlugin.afterUpdate, "function");
});

test("선 차트: 데이터셋별 지점 수·처음·끝·최고·최저를 라벨과 함께 읊는다(ko/en)", () => {
	const chart = { config: { type: "line" }, data: { labels: ["2026-09-01", "2026-09-02", "2026-09-03"], datasets: [{ label: "평가액", data: [100, 300, 200] }] } };
	assert.equal(mod.chartSummaryText(chart, "ko-KR"), "평가액: 3개 지점, 처음 2026-09-01 100, 끝 2026-09-03 200, 최고 300 (2026-09-02), 최저 100 (2026-09-01)");
	assert.equal(mod.chartSummaryText(chart, "en-US"), "평가액: 3 points, first 2026-09-01 100, last 2026-09-03 200, high 300 (2026-09-02), low 100 (2026-09-01)");
});

test("선 차트: {x,y} 지점·숨긴 데이터셋·빈 데이터셋을 처리한다", () => {
	const chart = { config: { type: "line" }, data: { labels: [], datasets: [{ label: "A", data: [{ x: "1월", y: 5 }, { x: "2월", y: 7.256 }] }, { label: "숨김", data: [1, 2] }, { label: "빈", data: [] }] }, isDatasetVisible: (i) => i !== 1 };
	const t = mod.chartSummaryText(chart, "ko-KR");
	assert.equal(t, "A: 2개 지점, 처음 1월 5, 끝 2월 7.26, 최고 7.26 (2월), 최저 5 (1월)");
	assert.equal(mod.chartSummaryText({ config: { type: "bar" }, data: { labels: [], datasets: [] } }, "ko-KR"), "");
});

test("도넛: 항목별 값과 비중, 10개 초과는 '외 N개'", () => {
	const chart = { config: { type: "doughnut" }, data: { labels: ["삼성전자", "KODEX"], datasets: [{ data: [750, 250] }] } };
	assert.equal(mod.chartSummaryText(chart, "ko-KR"), "항목 2개: 삼성전자 750 (75%), KODEX 250 (25%)");
	const many = { config: { type: "pie" }, data: { labels: Array.from({ length: 12 }, (_, i) => "L" + i), datasets: [{ data: Array.from({ length: 12 }, () => 1) }] } };
	const t = mod.chartSummaryText(many, "en-US");
	assert.ok(t.startsWith("12 items: L0 1 (8.3%)"), t);
	assert.ok(t.endsWith("and 2 more"), t);
});

test("동기화: 캔버스 뒤에 sr-only 요약을 만들고 aria-describedby 로 잇는다, 다시 부르면 재사용, 빈 요약이면 거둔다", () => {
	const { canvas, parent } = fakeCanvas("c1");
	const chart = { canvas, config: { type: "bar" }, data: { labels: ["a", "b"], datasets: [{ label: "S", data: [1, 2] }] } };
	const el = mod.syncChartSummary(chart);
	assert.equal(el.id, "c1-summary");
	assert.equal(el.className, "sr-only");
	assert.equal(el.attrs["data-chart-summary"], "");
	assert.deepEqual(parent.inserted[0], [el, "NEXT"], "캔버스 바로 뒤에 넣는다");
	assert.equal(canvas.attrs["aria-describedby"], "c1-summary");
	assert.ok(el.textContent.startsWith("S: 2개 지점"));
	chart.data.datasets[0].data = [1, 9];
	const again = mod.syncChartSummary(chart);
	assert.equal(again, el, "두 번째는 같은 요소를 재사용한다");
	assert.equal(parent.inserted.length, 1);
	assert.ok(el.textContent.includes("최고 9"));
	chart.data.datasets = [];
	assert.equal(mod.syncChartSummary(chart), null);
	assert.equal(el.removed, true);
	assert.equal(canvas.attrs["aria-describedby"], undefined);
	assert.equal(mod.syncChartSummary({ canvas: null }), null, "캔버스 없는 차트는 무시");
});
