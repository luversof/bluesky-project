// PoE2 고유 로어(플레이버)·요구 레벨 — poe2wiki.net 으로 채운다. 로직은 PoE1 과 같은 tools/poe-extract/wiki-uniques.mjs(runWikiUniques).
// parse-uniques2 가 flavour 를 비워 두는 이유(게임 FlavourText 에 이름 연결 키 없음)와 매칭 방식은 그 파일 머리말 참고.
import path from "node:path";
import { DATA_DIR, TABLES_DIR, WORK_DIR } from "./paths.mjs";
import { runWikiUniques } from "../poe-extract/wiki-uniques.mjs";

await runWikiUniques({
	host: "www.poe2wiki.net",
	uniquesFile: path.join(DATA_DIR, "uniques.json"),
	tablesDir: TABLES_DIR,
	cacheFile: path.join(WORK_DIR, "wiki-uniques2-cache.json"),
	label: "poe2",
});
