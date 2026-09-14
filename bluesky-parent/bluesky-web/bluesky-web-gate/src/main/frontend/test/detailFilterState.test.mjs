// 상세 조회 필터와 개별 섹션의 접힘/펼침을 htmx 스왑 뒤에도 지키는 규칙.
//
// 규칙 셋:
//  1) 저장값이 있으면 그것이 이긴다(사용자가 한 번이라도 접거나 펼친 선택).
//  2) 저장값이 없으면 "필터가 걸려 있을 때만" 펼친다 - 실측 2026-09-10: 네 화면 모두 필터 0개인데
//     펼친 채로 열려 화면당 116~124px 를 먹었다.
//  3) 개별 섹션([data-persist-open])은 저장값이 있을 때만 복원한다(없으면 서버가 그린 대로).
//
// 이 파일에는 테스트가 없었다(실측 2026-09-12: 주식 프론트엔드 모듈 중 유일한 무가드).
// 빌드 산출물을 최소 DOM 스텁에 올리고, 등록된 리스너를 직접 불러 확인한다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

const noop = () => {};

/** <details> 흉내. open 을 바꾸면 실제 요소처럼 값만 남는다. */
function details({ filter = false, sectionKey = null, active = false, open = false } = {}) {
	const attrs = {};
	if (filter) attrs["data-detail-filter"] = "";
	if (sectionKey !== null) attrs["data-persist-open"] = sectionKey;
	if (active) attrs["data-filter-active"] = "true";
	return {
		open,
		attrs,
		getAttribute: (name) => (name in attrs ? attrs[name] : null),
		matches: (selector) =>
			(selector === "[data-detail-filter]" && filter) ||
			(selector === "[data-persist-open]" && sectionKey !== null),
	};
}

/**
 * 스텁 문서를 새로 깔고 산출물을 <b>다시 평가</b>한다.
 *
 * <p>{@code import()} 로는 안 된다 - 이 파일은 모듈이 아니라 classic script 라 CommonJS 캐시에 걸려
 * 프로세스당 한 번만 실행된다(첫 테스트만 통과하고 나머지가 전부 깨졌다). 글자를 읽어 매번 새로 평가한다.
 */
const SOURCE = readFileSync(
	new URL("../../resources/static/js/stock/detailFilterState.js", import.meta.url),
	"utf8",
);

function load(elements, stored) {
	const listeners = {};
	const localStorage = {
		getItem: (k) => (k in stored ? stored[k] : null),
		setItem: (k, v) => {
			stored[k] = String(v);
		},
		removeItem: (k) => {
			delete stored[k];
		},
	};
	const document = {
		readyState: "complete",
		addEventListener: (type, fn) => {
			(listeners[type] = listeners[type] || []).push(fn);
		},
		querySelectorAll: (selector) => elements.filter((el) => el.matches(selector)),
	};
	// 산출물은 document · localStorage · setTimeout 만 쓴다.
	new Function("document", "localStorage", "setTimeout", SOURCE)(document, localStorage, noop);
	const fire = (type, target) => {
		(listeners[type] || []).forEach((fn) => fn({ target }));
	};
	return { fire, listeners, stored };
}

test("저장값이 없고 필터도 없으면 접는다", () => {
	const filter = details({ filter: true, active: false, open: true });
	load([filter], {});
	assert.equal(filter.open, false);
});

test("저장값이 없어도 필터가 걸려 있으면 펼친다", () => {
	const filter = details({ filter: true, active: true, open: false });
	load([filter], {});
	assert.equal(filter.open, true);
});

test("저장값이 필터 상태를 이긴다", () => {
	const closed = details({ filter: true, active: true, open: true });
	load([closed], { stockDetailFilterOpen: "0" });
	assert.equal(closed.open, false, "접기를 골랐으면 필터가 걸려 있어도 접힌 채로");

	const opened = details({ filter: true, active: false, open: false });
	load([opened], { stockDetailFilterOpen: "1" });
	assert.equal(opened.open, true, "펼치기를 골랐으면 필터가 없어도 펼친 채로");
});

test("한 화면의 필터 여럿은 같은 상태로 맞춘다", () => {
	const a = details({ filter: true, open: true });
	const b = details({ filter: true, open: true });
	const { fire } = load([a, b], { stockDetailFilterOpen: "1" });

	a.open = false;
	fire("toggle", a);

	assert.equal(b.open, false, "하나를 접으면 나머지도 접힌다");
	assert.equal(a.open, false);
});

test("접거나 펼친 선택을 저장한다", () => {
	const filter = details({ filter: true, open: true });
	const { fire, stored } = load([filter], {});

	filter.open = false;
	fire("toggle", filter);
	assert.equal(stored.stockDetailFilterOpen, "0");

	filter.open = true;
	fire("toggle", filter);
	assert.equal(stored.stockDetailFilterOpen, "1");
});

test("개별 섹션은 저장값이 있을 때만 복원한다", () => {
	const kept = details({ sectionKey: "tradeDetail", open: true });
	load([kept], {});
	assert.equal(kept.open, true, "저장값이 없으면 서버가 그린 대로 둔다");

	const restored = details({ sectionKey: "tradeDetail", open: true });
	load([restored], { "stockDetailSectionOpen:tradeDetail": "0" });
	assert.equal(restored.open, false);
});

test("섹션은 자기 키에만 저장한다", () => {
	const section = details({ sectionKey: "dividendDetail", open: false });
	const { fire, stored } = load([section], {});

	section.open = true;
	fire("toggle", section);

	assert.equal(stored["stockDetailSectionOpen:dividendDetail"], "1");
	assert.equal(stored.stockDetailFilterOpen, undefined, "섹션 토글이 공용 필터 키를 건드리면 안 된다");
});

test("htmx 스왑 뒤에도 다시 맞춘다", () => {
	const filter = details({ filter: true, active: false, open: false });
	const section = details({ sectionKey: "tradeDetail", open: false });
	const { fire } = load([filter, section], {
		stockDetailFilterOpen: "1",
		"stockDetailSectionOpen:tradeDetail": "1",
	});
	assert.equal(filter.open, true);
	assert.equal(section.open, true);

	// 스왑으로 서버가 접힌 채로 다시 그렸다고 치고
	filter.open = false;
	section.open = false;
	fire("htmx:afterSwap", filter);

	assert.equal(filter.open, true, "필터는 저장값대로 다시 펼쳐진다");
	assert.equal(section.open, true, "섹션도 저장값대로");
});
