// 종목 필터 검색 상자의 이름과 결과 알림.
//
// 실측 2026-09-12(qa/stocksearch.cjs, 매매 화면 종목 패널을 열고 실제로 타이핑):
//   열었을 때   보이는 항목 43, aria-label 없음(placeholder "검색" 뿐), 라이브 영역 0개
//   "삼성" 입력 보이는 항목 2  (삼성전자 / 삼성SDI) - 알림 없음
//   "zzzz" 입력 보이는 항목 0  - 패널에 "전체" 한 줄만 남고 안내 문구 없음
//
// placeholder 는 값을 치는 순간 사라지므로 이름이 될 수 없다. 결과 수도 눈으로만 보였다.
// 계산은 순수 함수라 모듈 범위로 올려 여기서 고정한다(DOM 조립부는 원본 소스를 스캔).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

globalThis.window = globalThis.window ?? {};
globalThis.document = globalThis.document ?? {
	readyState: "complete",
	addEventListener: () => {},
	querySelectorAll: () => [],
	documentElement: { lang: "ko-KR" },
};

await import("../../resources/static/js/stock/multiSelectDropdown.js");
const mod = globalThis.__msdInternals;
const src = readFileSync(new URL("../src/stock/multiSelectDropdown.ts", import.meta.url), "utf8");

test("계산이 노출돼 있다", () => {
	assert.equal(typeof mod.msdSearchLabel, "function");
	assert.equal(typeof mod.msdSearchStatusText, "function");
	assert.equal(typeof mod.msdEmptyText, "function");
});

test("이름은 필드 이름을 앞세운다", () => {
	assert.equal(mod.msdSearchLabel("종목", "ko-KR"), "종목 검색");
	assert.equal(mod.msdSearchLabel("종목명", "ko-KR"), "종목명 검색");
	assert.equal(mod.msdSearchLabel("Stock", "en"), "Search Stock");
	// 라벨을 못 읽어도 이름은 남아야 한다 - 이름 없는 상자로 돌아가면 안 된다.
	assert.equal(mod.msdSearchLabel("", "ko-KR"), "검색");
	assert.equal(mod.msdSearchLabel("   ", "en-US"), "Search");
});

test("검색어가 없으면 아무 말도 하지 않는다", () => {
	assert.equal(mod.msdSearchStatusText("", 43, "ko-KR"), "");
	// 패널을 열 때 value 를 비우고 input 을 다시 쏘므로, 여기서 말하면 열 때마다 읽힌다.
	assert.equal(mod.msdSearchStatusText("", 0, "en"), "");
});

test("실측 그대로 결과 수를 말한다", () => {
	assert.equal(mod.msdSearchStatusText("삼성", 2, "ko-KR"), "검색 결과 2개");
	assert.equal(mod.msdSearchStatusText("zzzz", 0, "ko-KR"), "검색 결과 없음");
	assert.equal(mod.msdSearchStatusText("sam", 2, "en"), "2 items");
	assert.equal(mod.msdSearchStatusText("zzzz", 0, "en"), "No matching items");
});

test("빈 상태 문구는 로케일을 따른다", () => {
	assert.equal(mod.msdEmptyText("ko-KR"), "검색 결과 없음");
	assert.equal(mod.msdEmptyText("en"), "No matching items");
	assert.equal(mod.msdEmptyText(""), "검색 결과 없음");
});

test("검색 상자에 이름을 달고, 거른 결과를 세어 알린다", () => {
	assert.ok(
		src.includes('search.setAttribute("aria-label", msdSearchLabel(fieldLabel, lang()));'),
		"검색 상자에 aria-label 을 달지 않는다",
	);
	assert.ok(src.includes('searchStatus.setAttribute("role", "status");'), "결과 알림 영역이 없다");
	assert.ok(src.includes('searchStatus.setAttribute("aria-live", "polite");'), "알림이 polite 가 아니다");
	assert.ok(src.includes("if (hit) matched++;"), "거른 결과를 세지 않는다");
	assert.ok(
		src.includes("searchEmpty.hidden = !raw || matched > 0;"),
		"0 개일 때 빈 상태 문구를 띄우지 않는다",
	);
	// 같은 문구를 두 번 읽히지 않게: 알림은 라이브 영역 하나만 한다.
	assert.ok(
		src.includes('searchEmpty.setAttribute("aria-hidden", "true");'),
		"빈 상태 문구가 알림과 겹쳐 두 번 읽힌다",
	);
});
