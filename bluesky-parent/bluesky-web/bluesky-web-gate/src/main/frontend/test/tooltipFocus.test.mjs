// 툴팁은 마우스 hover 만이 아니라 키보드 포커스(:focus-visible)에서도 열린다.
//
// 실측 2026-09-09: 트리거에 tabindex="0" 을 줬는데도 main.css 의 표시 규칙이 :hover 뿐이라 4/4 툴팁이 포커스 시 opacity 0/hidden 이었다.
// CSS 는 브라우저 없이 실행할 수 없으니 소스 규칙을 못박는다(빌드 산출물은 minify 로 형태가 바뀌어 원본을 본다).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const css = readFileSync(new URL("../main.css", import.meta.url), "utf8");

test("툴팁 표시 규칙에 :focus-visible 이 hover 와 나란히 있다", () => {
	const rules = css.split("}").filter((r) => /opacity:\s*1;\s*visibility:\s*visible/.test(r) && r.includes(".tooltip"));
	assert.ok(rules.length >= 1, "툴팁 표시 규칙을 못 찾았다");
	for (const r of rules) {
		const selector = r.slice(0, r.indexOf("{"));
		assert.match(selector, /:focus-visible/, "포커스로는 안 열리는 툴팁 규칙: " + selector.trim());
		assert.match(selector, /:hover/, "hover 로는 안 열리는 툴팁 규칙: " + selector.trim());
	}
	// data-tip 말풍선(::before)과 블록 툴팁(.tooltip-content) 둘 다 같은 규칙 안에 있어야 한다.
	const joined = rules.map((r) => r.slice(0, r.indexOf("{"))).join(" ");
	assert.match(joined, /\.tooltip-content/);
	assert.match(joined, /:before|::before/);
});

// WCAG 1.4.13 실측 2026-09-09: 말풍선이 pointer-events:none 이라 포인터를 그 위로 옮기는 순간 4/4 가 사라졌고, Escape 로는 안 닫혔다.
test("보이는 툴팁은 포인터를 받고(hoverable), 숨김은 틈을 건널 지연이 있으며, tooltip-dismissed 가 표시 규칙을 이긴다", () => {
	const blocks = css.split("}");
	const show = blocks.filter((r) => r.includes(".tooltip") && /:focus-visible/.test(r) && /opacity:\s*1/.test(r));
	assert.ok(show.length >= 1);
	for (const r of show) assert.match(r, /pointer-events:\s*auto/, "말풍선 위에서 hover 가 끊긴다: " + r.slice(0, r.indexOf("{")).trim());
	const base = blocks.find((r) => /\.tooltip\s*>\s*\.tooltip-content,\s*\.tooltip:before\s*\{/.test(r));
	assert.ok(base, "툴팁 기본 규칙을 못 찾았다");
	assert.match(base, /visibility 0s linear \.\d+s/, "숨김 지연이 없으면 트리거-말풍선 틈에서 사라진다");
	const dismissedIdx = blocks.findIndex((r) => /\.tooltip\.tooltip-dismissed/.test(r) && /visibility:\s*hidden/.test(r));
	assert.ok(dismissedIdx >= 0, "Escape 로 닫힌 툴팁을 숨기는 규칙이 없다");
	const showIdx = blocks.findIndex((r) => show.includes(r));
	assert.ok(dismissedIdx > showIdx, "tooltip-dismissed 규칙은 표시 규칙 뒤에 있어야 이긴다");
});
