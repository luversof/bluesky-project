// 브라우저 스크립트가 찾는 선택자가 템플릿에 실제로 있는지 본다.
//
// 없는 id/속성을 찾는 코드는 조용히 죽는다 - 예외도 로그도 없고, 그 기능만 아무 일도 하지 않는다.
// 같은 성격의 실측 사례가 서버 쪽에도 있었다: tabsPortfolio.jte 의 정렬 헤더 8개가 존재하지 않는
// #tab-content 를 겨냥하고 있었고, 그건 그 조각 자체가 죽었다는 신호였다(HtmxTargetResolvesTest).
//
// 스크립트가 스스로 만들어 붙이는 마크업은 템플릿에 없는 게 정상이므로 사유와 함께 목록에 둔다.
// 목록에 있는데 실제로는 템플릿에 생겼다면 목록이 낡은 것이므로 그것도 알린다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const MAIN = resolve(HERE, "../..");
const JTE = join(MAIN, "jte");
const TS = join(HERE, "../src");

/** 스크립트가 직접 만들어 붙이는 마크업. 템플릿에 없는 것이 정상이다. */
const SCRIPT_CREATED = new Map([
	["simple-multi-style", "multiSelectInit.ts 가 주입하는 <style> 의 id"],
	["data-msd-wrap", "multiSelectDropdown.ts 가 만드는 드롭다운 껍데기"],
	["data-msd-panel", "multiSelectDropdown.ts 가 만드는 드롭다운 패널"],
	["data-poe-chip", "poe/multiSelect.ts 가 만드는 칩"],
	["data-poe-msd-wrap", "poe/multiSelect.ts 가 만드는 껍데기"],
	["data-poe-msd-panel", "poe/multiSelect.ts 가 만드는 패널"],
	["data-year-toggle", "stockSimulator.ts 가 표 행을 만들며 붙인다"],
	["data-scenario-id", "stockSimulator.ts 가 시나리오 버튼을 만들며 붙인다"],
	["poeTreeEvalStale", "poe/tree.ts 가 만드는 표식"],
]);

/** 읽기만 하고 아무도 만들지 않는 것으로 확인된 것. */
const KNOWN_DEAD = new Map([
	["data-overlay", "common.ts 의 레이어 닫기 처리. 이 속성을 붙이는 마크업이 소스 어디에도 없다"],
	["data-overlay-close", "위와 같은 처리의 닫기 버튼"],
	["tradeProfitForm", "stock/tradeProfit.js 는 죽은 산출물이다(deadAssets.test.mjs 의 KNOWN_UNUSED)"],
	["tradeProfitResult", "위와 같음"],
]);

function walk(dir, ext, out = []) {
	for (const name of readdirSync(dir)) {
		const p = join(dir, name);
		if (statSync(p).isDirectory()) walk(p, ext, out);
		else if (name.endsWith(ext)) out.push(p);
	}
	return out;
}

const templateBlob = walk(JTE, ".jte")
	.map((f) => readFileSync(f, "utf8"))
	.join("\n");

function definedInTemplates(name) {
	if (name.startsWith("data-")) return templateBlob.includes(name);
	return (
		templateBlob.includes(`id="${name}"`) || templateBlob.includes(`id = "${name}"`)
	);
}

/** 스크립트가 찾는 이름들. */
function referenced() {
	const found = new Map();
	for (const file of walk(TS, ".ts")) {
		const source = readFileSync(file, "utf8");
		const add = (name) => {
			if (!found.has(name)) found.set(name, file);
		};
		for (const m of source.matchAll(
			/getElementById\(\s*["']([A-Za-z0-9_-]+)["']/g,
		))
			add(m[1]);
		for (const m of source.matchAll(
			/querySelector(?:All)?\(\s*["']#([A-Za-z0-9_-]+)["']/g,
		))
			add(m[1]);
		for (const m of source.matchAll(/["']\[(data-[a-z0-9-]+)[\]=]/g)) add(m[1]);
	}
	return found;
}

test("스크립트가 찾는 선택자가 템플릿에 있다", () => {
	const refs = referenced();
	// 스캔이 조용히 0건이 되면 검사가 무력해진다.
	assert.ok(refs.size >= 20, `선택자를 거의 찾지 못했다: ${refs.size}`);

	const missing = [];
	for (const [name, file] of refs) {
		if (definedInTemplates(name)) continue;
		if (SCRIPT_CREATED.has(name) || KNOWN_DEAD.has(name)) continue;
		missing.push(`${name} (${file.replace(MAIN, "")})`);
	}
	assert.deepEqual(
		missing,
		[],
		"템플릿에 없는 선택자를 찾고 있다. 마크업을 붙이거나," +
			" 스크립트가 직접 만드는 것이면 SCRIPT_CREATED 에, 죽은 코드면 KNOWN_DEAD 에 사유와 함께 등록할 것",
	);
});

test("목록에 등록한 것이 아직도 템플릿에 없다", () => {
	const stale = [];
	for (const [name, why] of [...SCRIPT_CREATED, ...KNOWN_DEAD]) {
		if (definedInTemplates(name)) stale.push(`${name} — ${why}`);
	}
	assert.deepEqual(stale, [], "템플릿에 생겼는데 목록에 남아 있다");
});
