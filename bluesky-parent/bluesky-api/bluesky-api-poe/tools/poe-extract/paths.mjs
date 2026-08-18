// 추출 파이프라인 공용 경로 — 게임 데이터(중간 산출물 포함)는 전부 repo 밖(~/.poe-gamedata)에 둔다.
// repo 에는 스크립트와 config.json 만 남는다.
import { execSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

export const REPO_DIR = path.dirname(fileURLToPath(import.meta.url));
export const DATA_DIR = path.join(os.homedir(), ".poe-gamedata");
export const WORK_DIR = path.join(DATA_DIR, "work"); // CLI 작업 디렉토리 (번들 캐시/tables/files)
export const TABLES_DIR = path.join(WORK_DIR, "tables");
export const FILES_DIR = path.join(WORK_DIR, "files");
export const POB_DIR = path.join(WORK_DIR, "pob-uniques");

export const loadTable = (lang, table) =>
	JSON.parse(fs.readFileSync(path.join(TABLES_DIR, lang, table + ".json"), "utf8"));

export const loadConfig = () =>
	JSON.parse(fs.readFileSync(path.join(REPO_DIR, "config.json"), "utf8"));

/** ImageMagick 설치 위치 탐색 — 표준 설치 폴더 경로, PATH 에 있으면 "PATH", 없으면 null */
export function findImageMagick() {
	try {
		const magickDir = fs
			.readdirSync("C:/Program Files")
			.filter((d) => d.startsWith("ImageMagick"))
			.map((d) => "C:/Program Files/" + d)[0];
		if (magickDir) return magickDir;
	} catch (e) { /* 비 Windows 등 */ }
	try {
		execSync("magick -version", { stdio: "ignore" });
		return "PATH";
	} catch (e) {
		return null;
	}
}

/** repo 의 config(또는 override)를 작업 디렉토리에 복사하고 pathofexile-dat CLI 를 실행한다.
 *  ⚠ CLI 는 tables/ 를 **통째로 비우고** config 에 적힌 테이블만 다시 쓴다.
 *  그래서 테이블을 줄인 config 로 돌리면 나머지 테이블이 사라진다.
 *    · **의도한 축소 추출**(parse-anoints·아이콘 스크립트들처럼 자기가 쓸 테이블만 뽑는 경우)은
 *      run-all 이 이들을 테이블 소비 파서 **뒤**에 배치해 두었다 → { partial: true } 로 명시한다.
 *    · 그 표식 없이 줄인 config 를 넘기면 사고다(즉석 재추출로 tables/ 를 날린 적이 있다) → 막는다.
 */
export function runExtractor(configOverride, { partial = false } = {}) {
	fs.mkdirSync(WORK_DIR, { recursive: true });
	const config = configOverride ?? loadConfig();
	if (configOverride && !partial) {
		const full = loadConfig().tables.length;
		if ((configOverride.tables || []).length < full) {
			throw new Error(
				`추출기는 tables/ 를 비우고 다시 쓴다 — 테이블을 줄인 config(${configOverride.tables.length}/${full})는 ` +
				"나머지 테이블을 지운다. 의도한 축소 추출이면 runExtractor(config, { partial: true }) 로 부르고, " +
				"컬럼만 추가한 것이면 config.json 을 고쳐 전체 추출(node extract.mjs)을 돌릴 것.",
			);
		}
	}
	fs.writeFileSync(path.join(WORK_DIR, "config.json"), JSON.stringify(config, null, 2));

	// ImageMagick 이 PATH 에 없으면 표준 설치 경로를 붙인다 (DDS→PNG 변환용).
	// 주의: Windows/JVM 경유 실행 시 환경변수 키가 "Path" 등 대소문자가 다를 수 있어 키를 탐색한다.
	const env = { ...process.env };
	const magickDir = findImageMagick();
	if (magickDir && magickDir !== "PATH") {
		const pathKey = Object.keys(env).find((k) => k.toUpperCase() === "PATH") || "PATH";
		env[pathKey] = magickDir + path.delimiter + (env[pathKey] || "");
	}

	const cli = path.join(REPO_DIR, "node_modules", "pathofexile-dat", "dist", "cli", "run.js");
	// PATH 의존 없이 현재 node 바이너리로 실행
	execSync(`"${process.execPath}" "${cli}"`, { stdio: "inherit", cwd: WORK_DIR, env });
}
