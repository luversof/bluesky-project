// Windows 고대비(forced-colors: active)에서 눌림/선택/활성 상태가 색 외의 단서(밑줄)로 남는다.
//
// 실측 2026-09-09(qa/forced-pixels.cjs): 고대비 에뮬레이션에서 기간 프리셋(aria-pressed)·독 활성 항목·탭의 스크린샷 하단 띠가 비활성과 같았다 -
// 배경·글자색이 시스템 색으로 강제되기 때문. 외곽선은 포커스 링과 겹치고 box-shadow 는 고대비에서 제거되므로 밑줄을 쓴다. CSS 는 소스 규칙을 못박는다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const css = readFileSync(new URL("../main.css", import.meta.url), "utf8");

test("forced-colors 블록이 상태 셀렉터에 밑줄을 준다", () => {
	const start = css.indexOf("@media (forced-colors: active)");
	assert.ok(start >= 0, "forced-colors 블록이 없다");
	// 블록 끝: 중괄호 짝 맞추기
	let depth = 0, end = -1;
	for (let i = css.indexOf("{", start); i < css.length; i++) {
		if (css[i] === "{") depth++;
		else if (css[i] === "}") { depth--; if (depth === 0) { end = i; break; } }
	}
	const block = css.slice(start, end);
	for (const sel of ['[aria-pressed="true"]', '[role="tab"][aria-selected="true"]', ".tab-active", ".dock-active"]) {
		assert.ok(block.includes(sel), "고대비 상태 단서 대상에서 빠짐: " + sel);
	}
	assert.match(block, /text-decoration:\s*underline/, "색 외의 단서(밑줄)가 없다");
	assert.ok(block.includes('tr[aria-selected="true"] > td:first-child::before'), "선택 행은 배경색만 달라 고대비에서 사라진다 - 표식 필요");
	// 정규식 안의 \2 는 8진 이스케이프로 읽히므로 백슬래시를 이스케이프해 CSS 원문의 "\25B8" 을 찾는다.
	assert.match(block, /content:\s*"\\25B8/, "선택 행 표식 글리프가 없다");
	assert.doesNotMatch(block, /\boutline\s*:/, "외곽선은 포커스 링과 겹친다");
});
