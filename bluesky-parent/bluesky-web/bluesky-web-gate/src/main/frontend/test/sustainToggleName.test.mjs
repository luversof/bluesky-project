// 연도별 상세의 펼침 단추는 해마다 다른 이름을 가져야 한다.
//
// 실측 2026-09-12: 이 표의 펼침 단추 42 개가 모두 "월별 상세 펼치기" 하나였다(고유 이름 1 종, aria-controls 0 개).
// 단추만 훑는 사용자에게는 42 개가 같은 것으로 들려 어느 해를 펼치는지 고를 수 없다.
// 같은 세션의 자산 현황은 이미 계좌 이름을 실어 5 개가 5 종이고 aria-controls 도 5 개다 - 그 규칙에 맞춘다.
//
// 빌드 산출물을 그대로 읽는다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

const built = readFileSync(
	new URL("../../resources/static/js/stock/stockSimulator.js", import.meta.url),
	"utf8",
);

test("해 번호를 넣은 이름 틀을 쓴다", () => {
	assert.ok(built.includes("yearlyToggleOpenNamed"), "펼치기 이름 틀이 있어야 한다");
	assert.ok(built.includes("yearlyToggleCloseNamed"), "접기 이름 틀이 있어야 한다");
	assert.ok(
		built.includes('replace("{0}"'),
		"틀의 {0} 자리에 해 번호를 넣어야 한다",
	);
});

// 빌드가 ${...} 안의 공백을 지우므로 원본 서식으로 비교하면 영영 맞지 않는다(실측: 그 탓에 변이가 가드를
// 그대로 통과했다). 공백을 눌러 비교하고, "없어야 한다" 가 아니라 "이렇게 쓰여 있어야 한다" 로 못 박는다.
const flat = built.replace(/\s+/g, "");

test("단추 이름은 해마다 만드는 함수가 만든다", () => {
	assert.ok(
		flat.includes('aria-label="${escapeHtml(yearlyToggleName('),
		"aria-label 이 yearlyToggleName 을 거쳐야 42 개가 다른 이름이 된다",
	);
	assert.ok(
		!flat.includes('aria-label="${escapeHtml(expanded?i18n.yearlyToggleClose'),
		"고정 문구를 그대로 쓰면 다시 42 개가 같은 이름이 된다",
	);
});

test("단추가 여는 줄을 가리킨다", () => {
	assert.ok(
		flat.includes('aria-controls="${yearlyDetailRowId('),
		"단추의 aria-controls 가 줄 id 규칙을 써야 한다",
	);
	assert.ok(flat.includes('<trid="${yearlyDetailRowId('), "그 줄에 같은 id 가 붙어야 한다");
	// 같은 규칙이 단추와 줄 양쪽에 쓰여야 짝이 맞는다
	const uses = built.split("yearlyDetailRowId(").length - 1;
	assert.ok(uses >= 3, "정의 1 + 단추 1 + 줄 1 이상이어야 한다. 실제 " + uses);
});

test("메시지 키가 두 번들에 다 있다", () => {
	for (const bundle of ["uiMessage.properties", "uiMessage_ko.properties"]) {
		const text = readFileSync(new URL("../../resources/" + bundle, import.meta.url), "latin1");
		for (const key of [
			"stock.simulator.toggle.open.monthly.named",
			"stock.simulator.toggle.close.monthly.named",
		]) {
			assert.ok(text.includes(key), bundle + " 에 " + key + " 가 없다");
		}
		const line = text.split("\n").find((l) => l.startsWith("stock.simulator.toggle.open.monthly.named"));
		assert.ok(line.includes("{0}"), bundle + " 의 이름 틀에 {0} 자리가 없다");
	}
});
