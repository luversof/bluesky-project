// 차트 위 표시(매수 ▲ · 매도 ▼ · 분배락 ◆) 색의 비텍스트 대비(WCAG 1.4.11, 3:1).
// 실측 2026-10-03: 옛 색은 흰 카드에서 ▲ 2.72 · ▼ 2.65 · ◆ 3.00 이었다 - 라이트만 진한 색으로 바꿨다(다크 카드 rgb(15,22,35) 는 5.2~6.0).
// 빌드 산출물에서 chartMarkerColors 를 꺼내 테마별로 부른다. 대비는 반투명이면 배경과 감마 sRGB 에서 합성한 뒤 잰다.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import vm from "node:vm";

import { extractFunction, JTE_ROOT } from "./inlineTemplateScript.mjs";

const BUILT = "../resources/static/js/stock-charts.js";

function colorsFor(theme) {
	const source = readFileSync(join(JTE_ROOT, BUILT), "utf8");
	const sandbox = { document: { documentElement: { getAttribute: (n) => (n === "data-theme" ? theme : null) } } };
	vm.createContext(sandbox);
	vm.runInContext(extractFunction(source, "chartMarkerColors"), sandbox);
	return vm.runInContext("chartMarkerColors()", sandbox);
}

function parse(css) {
	const m = css.match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+))?\s*\)/);
	assert.ok(m, "색 형식을 읽지 못했다: " + css);
	return { rgb: [Number(m[1]), Number(m[2]), Number(m[3])], a: m[4] === undefined ? 1 : Number(m[4]) };
}

const lin = (c) => { c /= 255; return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4; };
const lum = ([r, g, b]) => 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
function contrast(css, bg) {
	const { rgb, a } = parse(css);
	const over = rgb.map((v, i) => v * a + bg[i] * (1 - a));
	const [hi, lo] = [lum(over), lum(bg)].sort((x, y) => y - x);
	return (hi + 0.05) / (lo + 0.05);
}

for (const [theme, bg] of [["light", [255, 255, 255]], ["dark", [15, 22, 35]]]) {
	test(`${theme} 카드에서 표시 셋이 비텍스트 대비 3:1 을 넘는다`, () => {
		const colors = colorsFor(theme);
		for (const key of ["buy", "sell", "dist"]) {
			const c = contrast(colors[key], bg);
			assert.ok(c >= 3.05, `${theme} ${key} ${colors[key]} 대비 ${c.toFixed(2)} - 3:1 미달(경계값 3.00 도 반올림에 따라 못 미친다)`);
		}
	});
}
