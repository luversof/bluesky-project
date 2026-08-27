// 차트 축의 숫자 축약(억/만, B/M/K)을 검증한다.
//
// 자산성장·배당이력·월별·보유 차트의 y축 4곳이 이 함수를 쓴다. 지금까지 테스트가 없었다.
//
// 빌드 산출물을 그대로 부른다. 배포되는 것은 그 파일이다.
import assert from "node:assert/strict";
import test from "node:test";

function setLocale(lang) {
	globalThis.document = {
		getElementById: () => null,
		createElement: () => ({ className: "", textContent: "", style: {}, dataset: {} }),
		body: { dataset: {} },
		documentElement: { lang },
	};
}
setLocale("ko-KR");
globalThis.window = globalThis;
// Node 24 의 globalThis.navigator 는 읽기 전용이라 대입하지 않는다.
// resolveLocale 은 documentElement.lang 을 먼저 보므로 스텁만으로 충분하다.

await import("../../resources/static/js/stock-charts.js");
const fmt = (v) => globalThis.window.StockCharts.formatCompactNumber(v);

test("한국어: 만·억 단위를 붙인다", () => {
	setLocale("ko-KR");
	assert.equal(fmt(0), "0");
	assert.equal(fmt(9999), "9,999");
	assert.equal(fmt(12345), "1.2만");
	assert.equal(fmt(1234567), "123만");
	assert.equal(fmt(123456789), "1.2억");
	assert.equal(fmt(1500000000), "15억");
});

test("한국어: 음수는 부호를 유지한다", () => {
	setLocale("ko-KR");
	assert.equal(fmt(-123456789), "-1.2억");
	assert.equal(fmt(-12345), "-1.2만");
});

/**
 * 경계에서 한 단위 아래 표기가 남지 않는다.
 *
 * 예전에는 단위를 고른 뒤 반올림해서 99,999,999 가 "10,000만" 으로 나왔다(1억 이어야 한다).
 */
test("한국어: 반올림이 다음 단위에 닿으면 그 단위로 적는다", () => {
	setLocale("ko-KR");
	assert.equal(fmt(99999999), "1억");
	assert.equal(fmt(99995000), "1억");
	// 아직 닿지 않으면 만 단위 그대로다.
	assert.equal(fmt(99000000), "9,900만");
});

test("영어: B/M/K 를 붙인다", () => {
	setLocale("en-US");
	assert.equal(fmt(999), "999");
	assert.equal(fmt(1500), "1.5K");
	assert.equal(fmt(2500000), "2.5M");
	assert.equal(fmt(1500000000), "1.5B");
});

test("영어: 반올림이 다음 단위에 닿으면 그 단위로 적는다", () => {
	setLocale("en-US");
	assert.equal(fmt(999999), "1M");
	assert.equal(fmt(999999999), "1B");
	assert.equal(fmt(994000), "994K");
});

test("한국어가 아니면 억/만 을 쓰지 않는다", () => {
	setLocale("en-US");
	for (const v of [12345, 123456789, 1500000000]) {
		assert.ok(!/[억만]/.test(fmt(v)), `영어 화면에 한국식 단위가 나온다: ${fmt(v)}`);
	}
});

test("숫자가 아니면 0 으로 본다", () => {
	setLocale("ko-KR");
	assert.equal(fmt(null), "0");
	assert.equal(fmt("어쩌고"), "0");
});
