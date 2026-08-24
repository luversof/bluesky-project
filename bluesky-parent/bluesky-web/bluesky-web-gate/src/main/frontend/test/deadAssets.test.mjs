// 배포되는 JS 중 어디서도 로드되지 않는 파일을 잡는다.
//
// 왜 필요한가(실측): 배포 산출물 22개 중 2개가 어떤 템플릿에서도 <script src> 로 불리지 않고
// 다른 스크립트가 import 하지도 않았다 - 즉 브라우저에서 한 줄도 실행되지 않는다.
//   stock/timeSeriesChart.js — 시계열 차트는 지금 asset-growth.jte 등의 인라인 스크립트가 그린다.
//   stock/tradeProfit.js     — 찾는 DOM(#tradeProfitForm/#tradeProfitResult)이 어떤 템플릿에도 없다.
// 둘 다 화면이 개편되며 남은 잔재다.
//
// 죽은 파일 자체는 요청되지 않으니 런타임 비용은 없다. 문제는 <b>살아 있는 코드로 오해</b>하는 것이다 -
// 실제로 이 저장소에서 죽은 모듈을 고치고 테스트까지 붙인 적이 있다(그 코드는 화면에서 실행되지 않는다).
//
// 알려진 것은 사유와 함께 목록에 두고, 새로 생기는 것만 실패시킨다. 반대로 목록의 파일이 다시
// 쓰이기 시작하면 목록이 낡은 것이므로 그것도 알린다.
import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync, readdirSync, statSync } from "node:fs";
import { basename, dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

// cwd 가 아니라 이 파일 위치 기준(위 classicScriptParse.test.mjs 와 같은 이유).
const MAIN = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const JTE = join(MAIN, "jte");
const STATIC_JS = join(MAIN, "resources/static/js");

/** 로드되지 않는 것이 확인된 파일과 그 사유. */
const KNOWN_UNUSED = new Map([
	["stock/timeSeriesChart.js", "시계열 차트는 asset-growth.jte 등의 인라인 스크립트가 그린다"],
	["stock/tradeProfit.js", "대상 DOM(#tradeProfitForm)이 어떤 템플릿에도 없다"],
]);

function walk(dir, filter, out = []) {
	for (const name of readdirSync(dir)) {
		const p = join(dir, name);
		if (statSync(p).isDirectory()) {
			if (name === "vendor") continue;
			walk(p, filter, out);
		} else if (filter(name)) {
			out.push(p);
		}
	}
	return out;
}

function referenced() {
	const builtPaths = walk(STATIC_JS, (n) => n.endsWith(".js"));
	const built = builtPaths.map((p) => relative(STATIC_JS, p).split(sep).join("/"));
	const templates = walk(JTE, (n) => n.endsWith(".jte")).map((p) => readFileSync(p, "utf8"));
	const scripts = new Map(built.map((b) => [b, readFileSync(join(STATIC_JS, b), "utf8")]));

	const used = new Set();
	for (const name of built) {
		const bare = basename(name);
		const fromTemplate = templates.some((t) => t.includes("/js/" + name));
		const fromScript = [...scripts].some(
			([other, text]) => other !== name && (text.includes("/" + bare) || text.includes('"./' + bare)),
		);
		if (fromTemplate || fromScript) used.add(name);
	}
	return { built, used };
}

test("배포되는 JS 는 모두 어딘가에서 로드된다", () => {
	const { built, used } = referenced();
	// 스캔이 조용히 0건이 되면 검사가 무력해진다.
	assert.ok(built.length >= 15, `산출물을 찾지 못했다: ${built.length}`);

	const unused = built.filter((b) => !used.has(b));
	const unexpected = unused.filter((b) => !KNOWN_UNUSED.has(b));
	assert.deepEqual(
		unexpected,
		[],
		"어디서도 로드되지 않는 새 산출물이다. 템플릿에 <script src> 를 넣거나, 쓰지 않는 것이면"
			+ " KNOWN_UNUSED 에 사유와 함께 등록할 것",
	);

	const revived = [...KNOWN_UNUSED.keys()].filter((b) => used.has(b));
	assert.deepEqual(revived, [], "다시 쓰이기 시작한 파일이 목록에 남아 있다");

	const gone = [...KNOWN_UNUSED.keys()].filter((b) => !built.includes(b));
	assert.deepEqual(gone, [], "목록의 파일이 더 이상 만들어지지 않는다");
});
