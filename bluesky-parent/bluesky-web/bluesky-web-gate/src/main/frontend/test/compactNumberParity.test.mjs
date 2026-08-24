// 금액 압축 표기(영어)가 서버와 화면에서 같은 값을 내는지 본다.
//
// 같은 규칙이 두 곳에 있다 - 서버는 StockFormatUtil.compactKrw, 화면 차트 축은 stock-charts.ts 의
// compactNumber. 한국어 표기는 서로 다른 목적이라(서버는 "1억 2,345만" 절삭, 축은 "1.2억" 반올림)
// 비교 대상이 아니지만, 영어 K/M/B 는 둘 다 소수 1자리라 같아야 한다.
//
// 실측 2026-08-23: 화면 쪽에는 "반올림이 자릿수를 넘기면 단위를 올린다" 처리가 이미 있었는데 서버 쪽에는
// 없어서 999,999 가 화면 "1M" / 서버 "1,000K" 로 갈렸다. 서버를 맞춘 뒤 16개 값이 전부 일치한다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const TABLE = resolve(HERE, "../../../test/resources/compact-number-en.txt");
const SOURCE = resolve(HERE, "../src/stock-charts.ts");

// stock-charts.ts 의 영어 분기를 그대로 옮긴 것. 아래에서 소스와 대조한다.
function compactEn(numeric) {
	let abs = Math.abs(numeric);
	const sign = numeric < 0 ? "-" : "";
	const promote = (unit, next, digits) => {
		if (abs >= unit && abs < next && Number((abs / unit).toFixed(digits)) >= next / unit) {
			abs = next;
		}
	};
	const trim = (v) => {
		const t = v.toFixed(1);
		return t.endsWith(".0") ? t.slice(0, -2) : t;
	};
	promote(1000, 1000000, 1);
	promote(1000000, 1000000000, 1);
	if (abs >= 1000000000) return sign + trim(abs / 1000000000) + "B";
	if (abs >= 1000000) return sign + trim(abs / 1000000) + "M";
	if (abs >= 1000) return sign + trim(abs / 1000) + "K";
	return sign + new Intl.NumberFormat("en-US").format(abs);
}

test("서버가 낸 영어 압축 표기와 화면 규칙이 같다", () => {
	const rows = readFileSync(TABLE, "utf8")
		.split("\n")
		.map((l) => l.trim())
		.filter(Boolean)
		.map((l) => l.split("|"));
	// 표가 조용히 비면 검사가 무력해진다.
	assert.ok(rows.length >= 16, `기준표가 너무 작다: ${rows.length}`);

	const bad = [];
	for (const [raw, expected] of rows) {
		const actual = compactEn(Number(raw));
		if (actual !== expected) bad.push(`${raw}: 서버 ${expected} / 화면 ${actual}`);
	}
	assert.deepEqual(bad, [], "서버와 화면의 압축 표기가 갈렸다");
});

test("여기 옮겨 적은 규칙이 화면 소스와 같다", () => {
	const source = readFileSync(SOURCE, "utf8");
	assert.ok(
		source.includes("promote(1000, 1000000, 1)") && source.includes("promote(1000000, 1000000000, 1)"),
		"단위 승급 처리가 화면 소스에서 사라졌다",
	);
	assert.ok(source.includes('t.endsWith(".0")'), "소수 끝자리 정리 규칙이 바뀌었다");
});
