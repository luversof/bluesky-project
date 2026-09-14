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

// 도넛은 넘치는 항목을 "외 N개" 로 밝히는데 선/막대는 4 개에서 조용히 잘랐다.
// 실측 2026-09-12(배당 내역 "월별 배당금", 데이터셋 10 개): 요약 356 자가 앞 4 계열만 담고
// 나머지 6 개(기타 · 최근 12개월 합 포함)는 흔적도 없었다 - 보조기술은 그런 계열이 있는지조차 몰랐다.
test("막대/선: 데이터셋 4개를 넘으면 '외 N개 계열' 을 붙인다", () => {
	const ds = (n) => Array.from({ length: n }, (_, i) => ({ label: "S" + i, data: [1, 2] }));
	const four = { config: { type: "bar" }, data: { labels: ["a", "b"], datasets: ds(4) } };
	assert.ok(!mod.chartSummaryText(four, "ko-KR").includes("외 "), "4개까지는 군더더기가 없어야 한다");
	const ten = { config: { type: "bar" }, data: { labels: ["a", "b"], datasets: ds(10) } };
	const ko = mod.chartSummaryText(ten, "ko-KR");
	assert.ok(ko.startsWith("S0: 2개 지점"), ko);
	assert.ok(ko.endsWith(". 외 6개 계열"), ko);
	assert.ok(!ko.includes("S4:"), "5번째부터는 담지 않는다");
	const en = mod.chartSummaryText(ten, "en-US");
	assert.ok(en.endsWith(". and 6 more series"), en);
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

// 금액 가리기를 켜면 요약에서도 금액을 뺀다.
//
// 실측 2026-09-11(가리기를 켠 채 9 화면): sr-only 요약 14곳이 금액을 그대로 담고 있었다 - 화면은 흐려지는데
// 보조기술에는 "투자원금: 39개 지점, 처음 2026-01-01 637,902,360 ..." 이 그대로 읽혔다.
test("가리기를 켜면 선/막대 요약에서 금액이 빠진다(라벨·지점 수는 남는다)", () => {
	const chart = { config: { type: "line" }, data: { labels: ["2026-01", "2026-02"], datasets: [{ label: "투자원금", data: [1000, 2000] }] } };
	const shown = mod.chartSummaryText(chart, "ko-KR");
	assert.ok(shown.includes("1,000"), shown);

	document.documentElement.classList.contains = (name) => name === "hide-amounts";
	const hiddenText = mod.chartSummaryText(chart, "ko-KR");
	document.documentElement.classList.contains = () => false;

	assert.equal(hiddenText, "투자원금: 2개 지점(금액 가림)");
	assert.ok(!/[0-9],[0-9]/.test(hiddenText), hiddenText);
});

test("가리기를 켜면 도넛 요약은 비중만 남는다", () => {
	const chart = { config: { type: "doughnut" }, data: { labels: ["A", "B"], datasets: [{ data: [30, 70] }] } };
	document.documentElement.classList.contains = (name) => name === "hide-amounts";
	const hiddenText = mod.chartSummaryText(chart, "ko-KR");
	document.documentElement.classList.contains = () => false;

	assert.equal(hiddenText, "항목 2개: A (30%), B (70%)");
});

test("가리기가 꺼져 있으면 예전 그대로다", () => {
	const chart = { config: { type: "doughnut" }, data: { labels: ["A", "B"], datasets: [{ data: [30, 70] }] } };
	assert.equal(mod.chartSummaryText(chart, "ko-KR"), "항목 2개: A 30 (30%), B 70 (70%)");
});

test("영어도 같은 규칙", () => {
	const chart = { config: { type: "line" }, data: { labels: ["2026-01", "2026-02"], datasets: [{ label: "Principal", data: [1000, 2000] }] } };
	document.documentElement.classList.contains = (name) => name === "hide-amounts";
	const hiddenText = mod.chartSummaryText(chart, "en-US");
	document.documentElement.classList.contains = () => false;

	assert.equal(hiddenText, "Principal: 2 points (amounts hidden)");
});

// 근거 문구(title + sr-only)의 금액도 가린다.
//
// 실측 2026-09-11: 가리기를 켠 채 대시보드 합산 수익률에 hover 하면 "수익률 = 총 합산 수익 1,271,376,178 ÷ 기준 원금 642,..."
// 가 그대로 떴다(그 요소는 백분율이라 가림 대상이 아니었다).
// 흐리게 하는 자리(.amount-value)에 붙은 title 은 흐림이 안 걸린다 - 켜 놓고 hover 하면 정확한 금액이 그대로 뜬다.
// 실측 2026-09-12(가리기 켜고 9 화면): 31 개가 그랬다(대시보드 "1,622,109,770원" · 종목 상세 "1,359,088,500원" 등).
// 템플릿 52 곳이 이런 title 을 내므로 하나씩 표식을 다는 대신 공통 처리에서 가린다.
test("가리기를 켜면 amount-value 의 title 도 가려지고, 끄면 되돌아온다", () => {
	const original = "1,622,109,770원";
	// srExact 짝(sr-only)도 같은 값을 낭독기에 준다 - 2026-09-12 에 title 만 가렸더니 대시보드 10 곳에서
	// title 은 "가림" 인데 sr-only 는 "1,622,109,770원" 그대로였다.
	const sr = { textContent: original };
	const el = {
		attrs: { title: original },
		getAttribute(n) { return this.attrs[n] ?? null; },
		setAttribute(n, v) { this.attrs[n] = v; },
		querySelector(sel) { return sel === ".sr-only" ? sr : null; },
	};
	document.querySelectorAll = (sel) => (sel === ".amount-value[title]" ? [el] : []);
	document.documentElement.lang = "ko-KR";

	document.documentElement.classList.contains = (name) => name === "hide-amounts";
	assert.equal(mod.maskAmountBasis(document), 1);
	assert.ok(!/1,622,109,770/.test(el.attrs.title), el.attrs.title);
	assert.equal(el.attrs["data-amount-title"], original, "되돌리려면 원본을 남겨야 한다");
	assert.equal(el.attrs.title, "가림", "숫자에 붙은 원도 같이 가려 \"가림원\" 이 되지 않게 한다");
	assert.equal(sr.textContent, "가림", "낭독기가 읽는 짝도 같이 가린다");

	document.documentElement.classList.contains = () => false;
	assert.equal(mod.maskAmountBasis(document), 1);
	assert.equal(el.attrs.title, original, "끄면 원래 금액으로 돌아온다");
	assert.equal(sr.textContent, original, "sr-only 도 되돌아온다");
	document.querySelectorAll = () => [];
});

test("가리기를 켜면 근거 문구의 금액만 가려지고 백분율은 남는다", () => {
	const original = "수익률 = 총 합산 수익 1,271,376,178 ÷ 기준 원금 642,014,000 = +12.34%. 기준 원금은 ...";
	const sr = { textContent: original };
	const el = {
		attrs: { title: original, "data-amount-basis": original },
		getAttribute(n) { return this.attrs[n] ?? null; },
		setAttribute(n, v) { this.attrs[n] = v; },
		querySelector(sel) { return sel === ".sr-only" ? sr : null; },
	};
	document.querySelectorAll = (sel) => (sel === "[data-amount-basis]" ? [el] : []);
	document.documentElement.lang = "ko-KR";

	document.documentElement.classList.contains = (name) => name === "hide-amounts";
	assert.equal(mod.maskAmountBasis(document), 1);
	assert.ok(!/1,271,376,178/.test(el.attrs.title), el.attrs.title);
	assert.ok(!/642,014,000/.test(el.attrs.title), el.attrs.title);
	assert.ok(el.attrs.title.includes("+12.34%"), "백분율은 화면에서도 안 가린다");
	assert.equal(sr.textContent, el.attrs.title, "보조기술이 읽는 값도 같이 가린다");

	document.documentElement.classList.contains = () => false;
	assert.equal(mod.maskAmountBasis(document), 1);
	assert.equal(el.attrs.title, original, "끄면 원래 문구로 돌아온다");
	assert.equal(sr.textContent, original);
	document.querySelectorAll = () => [];
});
