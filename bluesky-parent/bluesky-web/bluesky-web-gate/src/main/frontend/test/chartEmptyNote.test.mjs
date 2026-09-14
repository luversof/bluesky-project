// 자료가 없을 때 캔버스를 감추고 안내를 띄우는 규칙(도넛과 같은 규칙을 월별 막대에도).
//
// 실측 2026-09-11(2013-01~03 구간): 배당 화면 도넛은 숨고 "해당 기간에 배당 내역이 없습니다" 가 떴는데
// 바로 옆 "월별 배당금" 은 280px 빈 캔버스였다(매매 화면 "월별 매매 금액" 도 220px).
import assert from "node:assert/strict";
import test from "node:test";

function makeEl(id, tag = "div") {
	const el = {
		id, tagName: tag.toUpperCase(), style: {}, className: "", textContent: "",
		children: [], parentElement: null,
		appendChild(child) { this.children.push(child); child.parentElement = this; return child; },
		removeChild(child) { this.children = this.children.filter((c) => c !== child); child.parentElement = null; return child; },
	};
	return el;
}

/** id -> 요소 인 최소 document. createElement 는 붙이기 전까지 부모가 없다. */
function makeDoc(elements) {
	const byId = new Map(elements.map((e) => [e.id, e]));
	// 진짜 DOM 은 떼어 낸 요소를 getElementById 로 못 찾는다 - 스텁도 같아야 한다
	// (안 그러면 "두 번째 호출에서 안내를 다시 만든다" 를 검사하지 못한다).
	const detach = (el) => { if (el && el.id) byId.delete(el.id); };
	for (const el of elements) {
		const original = el.removeChild.bind(el);
		el.removeChild = (child) => { detach(child); return original(child); };
	}
	return {
		getElementById: (id) => byId.get(id) || null,
		createElement: (tag) => {
			const el = makeEl("", tag);
			const original = el.removeChild.bind(el);
			el.removeChild = (child) => { detach(child); return original(child); };
			Object.defineProperty(el, "id", { get: () => el._id || "", set: (v) => { el._id = v; byId.set(v, el); }, configurable: true });
			return el;
		},
		body: makeEl("body"),
	};
}

const noop = () => {};
globalThis.window = globalThis;
// navigator 는 node 24 에서 읽기 전용이라 손대지 않는다(로케일은 documentElement.lang 으로 정해진다).
globalThis.Chart = function () {};
globalThis.Chart.getChart = () => null;
globalThis.Chart.register = noop;

const host = makeEl("host");
const canvas = makeEl("monthlyBarChart", "canvas");
host.appendChild(canvas);
globalThis.document = makeDoc([host, canvas]);
globalThis.document.documentElement = makeEl("html");

await import("../../resources/static/js/stock-charts.js");
const api = globalThis.StockCharts;

test("자료가 없으면 캔버스를 감추고 안내를 넣는다", () => {
	const hidden = api.renderChartEmptyNote("monthlyBarChart", true, "해당 기간에 배당 내역이 없습니다.");
	assert.equal(hidden, true);
	assert.equal(canvas.style.display, "none");
	const note = host.children.find((c) => c !== canvas);
	assert.ok(note, "안내 요소가 캔버스 자리에 들어가야 한다");
	assert.equal(note.textContent, "해당 기간에 배당 내역이 없습니다.");
});

test("자료가 생기면 안내를 걷어내고 캔버스를 되돌린다", () => {
	const hidden = api.renderChartEmptyNote("monthlyBarChart", false, "무시");
	assert.equal(hidden, false);
	assert.equal(canvas.style.display, "");
	assert.equal(host.children.length, 1, "안내가 남아 있으면 자료 위에 문구가 겹친다");
});

test("두 번 불러도 안내는 하나만 남는다", () => {
	api.renderChartEmptyNote("monthlyBarChart", true, "없음");
	api.renderChartEmptyNote("monthlyBarChart", true, "없음");
	assert.equal(host.children.length, 2);
	api.renderChartEmptyNote("monthlyBarChart", false, "");
	assert.equal(host.children.length, 1);
});

test("캔버스가 없으면 아무것도 하지 않는다", () => {
	assert.equal(api.renderChartEmptyNote("없는캔버스", true, "없음"), false);
});
