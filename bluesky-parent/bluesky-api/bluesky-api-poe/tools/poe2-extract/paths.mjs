// PoE2 추출 파이프라인 공용 경로 — PoE1(tools/poe-extract)과 산출물이 섞이지 않게 ~/.poe-gamedata/poe2 아래에 따로 둔다.
//   PoE1 CLI 는 tables/ 를 통째로 비우고 다시 쓰므로 작업 폴더를 공유하면 서로의 테이블을 지운다.
// 추출 라이브러리(pathofexile-dat)는 PoE1 폴더의 node_modules 를 그대로 쓴다 — patch 가 "4." 으로 시작하면
// 같은 라이브러리가 PoE2 CDN(patch-poe2.poecdn.com)과 PoE2 스키마 정의(validFor & 2)를 고른다.
import { execSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
// 번들 받기 주소를 패치 서버가 준 CDN(한국은 카카오)으로 돌린다 — 이 모듈을 쓰는 스크립트는 단독 실행해도 적용(10-12 C260)
import "./cdn-redirect.mjs";

export const REPO_DIR = path.dirname(fileURLToPath(import.meta.url));
export const POE1_TOOLS = path.join(REPO_DIR, "..", "poe-extract");
export const DAT_LIB = path.join(POE1_TOOLS, "node_modules", "pathofexile-dat");
export const DATA_DIR = path.join(os.homedir(), ".poe-gamedata", "poe2");
export const WORK_DIR = path.join(DATA_DIR, "work"); // CLI 작업 디렉토리 (번들 캐시/tables/files)
export const TABLES_DIR = path.join(WORK_DIR, "tables");
export const FILES_DIR = path.join(WORK_DIR, "files");

export const loadTable = (lang, table) =>
	JSON.parse(fs.readFileSync(path.join(TABLES_DIR, lang, table + ".json"), "utf8"));

export const loadConfig = () => JSON.parse(fs.readFileSync(path.join(REPO_DIR, "config.json"), "utf8"));

// POE_DATA_OUT — 출력만 다른 폴더로(10-10 C249). API 는 데이터 파일이 바뀌면 바로 다시 읽어서, 회귀 도중 재생성하면 돌던 탐침이 바뀐 데이터를 본다 → 사본으로 만들어 대조한 뒤 회귀 끝나고 교체
export const writeJson = (name, data) => {
	const dir = process.env.POE_DATA_OUT || DATA_DIR;
	fs.mkdirSync(dir, { recursive: true });
	const out = path.join(dir, name);
	fs.writeFileSync(out, JSON.stringify(data));
	return out;
};

/** 번들 캐시 출처 표식(10-13 C297) — 캐시 폴더 이름이 patch("4.5.5.4")뿐이라 GGG 에서 받은 캐시(색인 포함, 09-30)에 C260 뒤로 카카오 번들이 섞였다:
 *  색인은 GGG 것 · 새로 받는 번들은 카카오 것이라 새로 추가한 표(한국어 CurrencyItems)가 엉뚱한 바이트(러시아어 문자열)로 읽혀 추출이 3번 다 실패.
 *  같은 표를 카카오 색인으로 읽으면 정상(번들 08, 1537행). 캐시에 받은 곳(cdn, 해외는 "ggg")을 적어 두고 다르면 옛 캐시를 옆 이름으로 치운다
 *  (지우지 않음 — 되돌리기 = 이름 되돌리기). 표식 없는 옛 캐시도 출처를 모르니 치운다(한 번 새로 받는다). */
function ensureCacheSource() {
	const { patch, cdn } = loadConfig();
	const dir = path.join(WORK_DIR, ".cache", patch);
	const marker = path.join(dir, ".source");
	const source = cdn || "ggg";
	if (fs.existsSync(dir)) {
		const current = fs.existsSync(marker) ? fs.readFileSync(marker, "utf8").trim() : fs.readdirSync(dir).length ? "표식 없음" : source;
		if (current === source) {
			if (!fs.existsSync(marker)) fs.writeFileSync(marker, source);
			return;
		}
		const aside = `${dir}.stale-${new Date().toISOString().replace(/[:.]/g, "").slice(0, 15)}`;
		fs.renameSync(dir, aside);
		console.warn(`[poe2] 번들 캐시 출처가 다르다(${current} → ${source}) — 옛 캐시를 ${aside} 로 치우고 새로 받는다`);
	}
	fs.mkdirSync(dir, { recursive: true });
	fs.writeFileSync(marker, source);
}

/** 테이블만 CLI 로 뽑는다(files 는 비움) — CLI 의 파일 추출은 PoE1 스프라이트 목록(Art/UIImages1.txt …)을 먼저 읽는데
 *  PoE2 번들엔 없을 수 있어서, 파일은 exportFiles() 가 로더를 직접 불러 받는다. */
export function runExtractor() {
	fs.mkdirSync(WORK_DIR, { recursive: true });
	ensureCacheSource();
	const config = { ...loadConfig(), files: [] };
	fs.writeFileSync(path.join(WORK_DIR, "config.json"), JSON.stringify(config, null, 2));
	const cli = path.join(DAT_LIB, "dist", "cli", "run.js");
	// 추출기는 시작할 때 스키마를 github 에서 받고 다시 시도하지 않는다 — PoE1 파이프라인이 연결 시간 초과 한 번에 중간에서 멈춰
	//   고유 아이템 데이터가 반쯤 쓰인 채 나갔다(10-03 C53). PoE2 도 같은 추출기라 3번까지 다시 한다(C54).
	for (let attempt = 1; ; attempt++) {
		try {
			// 추출기는 별도 프로세스라 CDN 주소 바꾸기를 --import 로 넣는다(C260)
			const redirect = pathToFileURL(path.join(REPO_DIR, "cdn-redirect.mjs")).href;
			execSync(`"${process.execPath}" --import "${redirect}" "${cli}"`, { stdio: "inherit", cwd: WORK_DIR });
			return;
		} catch (e) {
			if (attempt >= 3) throw e;
			console.warn(`추출기 실패(${attempt}/3) — 15초 뒤 다시: ${e.message.split(String.fromCharCode(10))[0]}`);
			Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 15000);
		}
	}
}

/** 번들 로더를 직접 열어 파일을 받는다(같은 .cache 를 쓴다). 반환: { get(path) → Buffer|null, close } */
export async function openLoader() {
	ensureCacheSource();
	const { patch } = loadConfig();
	const loaders = await import(pathToFileURL(path.join(DAT_LIB, "dist", "cli", "bundle-loaders.js")).href);
	const loader = await loaders.FileLoader.create(
		new loaders.CachingBundleLoader(await loaders.CdnBundleLoader.create(path.join(WORK_DIR, ".cache"), patch)),
	);
	return {
		async get(p) {
			try {
				const b = await loader.getFileContents(p);
				return b ? Buffer.from(b) : null;
			} catch (e) {
				return null;
			}
		},
	};
}
