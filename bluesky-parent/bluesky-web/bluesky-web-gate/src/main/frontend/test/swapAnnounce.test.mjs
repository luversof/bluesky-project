// 조각 교체 알림: *Fragment 대상이 정착하면 #stockLiveStatus(role=status)에 '화면 이름: 내용 갱신됨' 을 넣는다(먼저 비운 뒤 넣어 재알림).
//
// 실측 2026-09-10(qa/live-announce.cjs): 매매·활동·자산성장에서 조각 교체 뒤 live 영역 변화 0. 빌드 산출물(common.js)을 최소 DOM 스텁으로 띄운다.
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
	documentElement: Object.assign(stubEl(), { lang: "ko-KR" }), body: stubEl(), head: stubEl(), title: "배당 내역 · Bluesky Stock",
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
const mod = globalThis.__swapAnnounceInternals;

class Fake extends globalThis.Element {
	constructor(id, attrs = {}) { super(); this.id = id; this.attrs = attrs; this.textContent = ""; }
	getAttribute(n) { return n in this.attrs ? this.attrs[n] : null; }
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

test("내부 함수가 노출돼 있다", () => {
	assert.ok(mod, "common.js 가 __swapAnnounceInternals 를 노출하지 않는다");
});

test("문구는 조각의 aria-label, 없으면 문서 제목의 접미사 앞부분을 쓴다", () => {
	assert.equal(mod.swapAnnouncement(new Fake("x", { "aria-label": "배당 목록" }), "배당 내역 · Bluesky Stock", "{0}: 내용 갱신됨"), "배당 목록: 내용 갱신됨");
	assert.equal(mod.swapAnnouncement(new Fake("x"), "매매 내역 · Bluesky Stock", "{0}: content updated"), "매매 내역: content updated");
	assert.equal(mod.swapAnnouncement(new Fake("x"), "", "{0}: updated"), ": updated");
});

test("*Fragment 대상만, status 영역을 비운 뒤 문구를 넣는다 - 단 사용자 조작 뒤에만", async () => {
	const region = new Fake("stockLiveStatus", { "data-message-updated": "{0}: 내용 갱신됨" }); region.textContent = "이전 문구";
	const root = { querySelector: (sel) => (sel === "#stockLiveStatus" ? region : null) };
	// 로드 때 htmx 로 불러오는 조각(종목 상세)은 알리지 않는다 - 실측: 페이지가 열리자마자 '갱신됨' 을 읽었다.
	assert.equal(mod.swapAnnounceState.userInteracted, false);
	assert.equal(mod.announceSwap(new Fake("dividendListFragment"), root), false, "사용자 조작 전에는 침묵");
	assert.equal(region.textContent, "이전 문구");
	mod.noteUserInteraction();
	assert.equal(mod.announceSwap(new Fake("dividendListFragment"), root), true);
	assert.equal(region.textContent, "", "먼저 비워야 같은 문구도 다시 읽힌다");
	await sleep(80);
	assert.equal(region.textContent, "배당 내역: 내용 갱신됨");
	assert.equal(mod.announceSwap(new Fake("holdings-snapshot-container"), root), false, "목록 조각이 아닌 대상은 알리지 않는다");
	assert.equal(mod.announceSwap(new Fake("tradeListFragment"), { querySelector: () => null }), false, "영역이 없으면 할 일 없음");
	assert.equal(mod.announceSwap(undefined, root), false);
});
