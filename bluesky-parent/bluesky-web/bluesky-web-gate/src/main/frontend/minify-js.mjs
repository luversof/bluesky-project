// tsc 산출물(/js)을 제자리에서 미니파이한다. 번들링은 하지 않으므로 모듈 구조와 import 경로는 그대로다.
// vendor/ 는 배포처에서 이미 미니파이된 파일이라 건너뛴다.
import { transform } from "esbuild";
import { readdir, readFile, writeFile, stat } from "node:fs/promises";
import { join, resolve } from "node:path";

const ROOT = resolve("../resources/static/js");

async function walk(dir) {
	const out = [];
	for (const name of await readdir(dir)) {
		const p = join(dir, name);
		const s = await stat(p);
		if (s.isDirectory()) {
			if (name === "vendor") continue;
			out.push(...(await walk(p)));
		} else if (name.endsWith(".js")) {
			out.push(p);
		}
	}
	return out;
}

const files = await walk(ROOT);
let before = 0;
let after = 0;

for (const file of files) {
	const src = await readFile(file, "utf8");
	before += Buffer.byteLength(src);
	const res = await transform(src, {
		// 식별자 renaming은 끈다. 일부 스크립트는 type="module" 이 아닌 classic script 로 로드되어
		// 전역 스코프를 공유하는데, 최상위 이름을 짧게 바꾸면 파일 간 충돌이 난다
		// (실제로 "Identifier 'g' has already been declared" 발생).
		minifyWhitespace: true,
		minifySyntax: true,
		minifyIdentifiers: false,
		format: "esm",
		target: "es2017",
		legalComments: "none",
	});
	await writeFile(file, res.code, "utf8");
	after += Buffer.byteLength(res.code);
}

const pct = before ? Math.round(((before - after) / before) * 100) : 0;
console.log(`[minify-js] ${files.length} files: ${before} -> ${after} bytes (-${pct}%)`);
