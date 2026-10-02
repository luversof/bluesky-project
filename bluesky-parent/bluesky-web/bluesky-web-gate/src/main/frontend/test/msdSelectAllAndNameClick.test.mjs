// 상세 검색의 계좌 · 종목 선택 상자(사용자 요청 2026-10-02).
//
// 1) 항목 이름 글자를 눌러도 체크박스를 누른 것과 같아야 한다. 실측: 이름을 누르면 마우스를 누르는 순간 포커스가 패널 밖
//    <main tabindex="-1"> 로 넘어가 focusin 처리가 패널을 닫았고, 클릭이 체크박스에 닿지 않았다(체크 그대로 · 패널 닫힘).
//    -> 패널을 tabindex=-1 로 두어 포커스가 패널 안에 머물게 한다.
// 2) 맨 위 "전체" 는 눌러도 아무 일이 없는 것처럼 보였다(해제만 했다 - 아무것도 안 고른 것이 곧 전체라 화면이 그대로).
//    -> "전체" = 전부 체크, 옆에 "전체 취소" = 전부 해제.
// 계산은 순수 함수(__msdInternals)로, DOM 조립부는 원본 소스를 본다.
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
const squash = (s) => s.replace(/\s+/g, "");

test("전체 취소 글자는 로케일을 따른다", () => {
	assert.equal(mod.msdClearAllText("ko-KR"), "전체 취소");
	assert.equal(mod.msdClearAllText("en"), "Clear all");
	assert.equal(mod.msdClearAllText(""), "전체 취소");
});

test("요약: 하나도 안 골랐거나 전부 골랐으면 전체, 아니면 이름", () => {
	assert.equal(mod.msdSummaryText([], 6, "전체"), "전체");
	assert.equal(mod.msdSummaryText(["a", "b", "c", "d", "e", "f"], 6, "전체"), "전체");
	assert.equal(mod.msdSummaryText(["a", "b"], 6, "전체"), "a, b");
	// 항목이 없는 상자에서 빈 요약이 되지 않게
	assert.equal(mod.msdSummaryText([], 0, "All"), "All");
});

test("패널은 포커스를 받아 이름 클릭에 닫히지 않는다(계좌 · 종목 · 태그 둘 다)", () => {
	const s = squash(src);
	assert.equal(s.split("panel.tabIndex=-1;").length - 1, 2, "두 패널 모두 tabindex=-1");
	// 이름을 감싼 label 이 체크박스를 품어야 이름 클릭이 체크로 이어진다
	assert.ok(s.includes('varitem=document.createElement("label");'));
	assert.ok(s.includes("item.appendChild(cb);item.appendChild(txt);"));
});

test("전체는 전부 체크, 전체 취소는 전부 해제", () => {
	const s = squash(src);
	const all = s.indexOf('allItem.addEventListener("click"');
	const clear = s.indexOf('clearItem.addEventListener("click"');
	assert.ok(all > 0 && clear > all, "두 단추의 처리가 있다");
	const allBody = s.slice(all, clear);
	const clearBody = s.slice(clear, s.indexOf("if(search)", clear));
	assert.ok(allBody.includes("o.selected=true;") && allBody.includes("cb.checked=true;"), "전체 = 체크");
	assert.ok(!allBody.includes("o.selected=false;"), "전체가 해제하면 안 된다(예전 동작)");
	assert.ok(clearBody.includes("o.selected=false;") && clearBody.includes(".checked=false;"), "전체 취소 = 해제");
	assert.ok(allBody.includes('select.dispatchEvent(newEvent("change"') && clearBody.includes('select.dispatchEvent(newEvent("change"'));
});
