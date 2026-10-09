// PoE2 추출 파이프라인 공용 경로 — PoE1(tools/poe-extract)과 산출물이 섞이지 않게 ~/.poe-gamedata/poe2 아래에 따로 둔다.
//   PoE1 CLI 는 tables/ 를 통째로 비우고 다시 쓰므로 작업 폴더를 공유하면 서로의 테이블을 지운다.
// 추출 라이브러리(pathofexile-dat)는 PoE1 폴더의 node_modules 를 그대로 쓴다 — patch 가 "4." 으로 시작하면
// 같은 라이브러리가 PoE2 CDN(patch-poe2.poecdn.com)과 PoE2 스키마 정의(validFor & 2)를 고른다.
import { execSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

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

export const writeJson = (name, data) => {
	fs.mkdirSync(DATA_DIR, { recursive: true });
	const out = path.join(DATA_DIR, name);
	fs.writeFileSync(out, JSON.stringify(data));
	return out;
};

/** 테이블만 CLI 로 뽑는다(files 는 비움) — CLI 의 파일 추출은 PoE1 스프라이트 목록(Art/UIImages1.txt …)을 먼저 읽는데
 *  PoE2 번들엔 없을 수 있어서, 파일은 exportFiles() 가 로더를 직접 불러 받는다. */
export function runExtractor() {
	fs.mkdirSync(WORK_DIR, { recursive: true });
	const config = { ...loadConfig(), files: [] };
	fs.writeFileSync(path.join(WORK_DIR, "config.json"), JSON.stringify(config, null, 2));
	const cli = path.join(DAT_LIB, "dist", "cli", "run.js");
	// 추출기는 시작할 때 스키마를 github 에서 받고 다시 시도하지 않는다 — PoE1 파이프라인이 연결 시간 초과 한 번에 중간에서 멈춰
	//   고유 아이템 데이터가 반쯤 쓰인 채 나갔다(10-03 C53). PoE2 도 같은 추출기라 3번까지 다시 한다(C54).
	for (let attempt = 1; ; attempt++) {
		try {
			execSync(`"${process.execPath}" "${cli}"`, { stdio: "inherit", cwd: WORK_DIR });
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
