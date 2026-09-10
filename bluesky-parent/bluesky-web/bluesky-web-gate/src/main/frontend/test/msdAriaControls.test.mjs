// 다중선택 드롭다운의 토글은 aria-controls 로 패널과 연결된다(두 생성 지점 모두).
//
// 실측 2026-09-09(qa/msd-a11y.cjs): aria-haspopup/aria-expanded/Enter/Escape 복귀는 갖췄지만 aria-controls 만 없었다.
// 이 모듈은 로드 시 DOM 을 통째로 만들어 스텁으로 띄우기 어려우므로 원본 소스를 스캔한다(빌드 산출물은 minify 로 형태가 바뀐다).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const src = readFileSync(new URL("../src/stock/multiSelectDropdown.ts", import.meta.url), "utf8");

test("패널 생성 지점마다 id 를 주고 토글에 aria-controls 를 붙인다", () => {
	const panels = src.split('panel.setAttribute("data-msd-panel", "1");').length - 1;
	assert.ok(panels >= 2, "패널 생성 지점이 줄었다(" + panels + ")");
	const wired = src.split('toggle.setAttribute("aria-controls", panel.id);').length - 1;
	assert.equal(wired, panels, "aria-controls 가 빠진 생성 지점이 있다");
	assert.ok(src.includes('panel.id = "msd-panel-" + (++msdPanelSeq);'), "패널 id 를 일련번호로 만들지 않는다");
	assert.ok(src.includes("var msdPanelSeq = 0;"), "일련번호 카운터가 없다 - 조각 교체 뒤 id 가 겹친다");
});
