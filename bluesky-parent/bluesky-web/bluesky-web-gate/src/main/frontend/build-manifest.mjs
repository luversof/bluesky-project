// 프런트엔드 소스의 지문을 남긴다.
//
// 왜 필요한가(실측): npm run build 가 check:nonce 에서 죽어 있는 동안, 소스만 고쳐지고
// src/main/resources/static 의 산출물은 옛것 그대로 커밋되어 그대로 배포됐다.
// 그 결과 배포본 게이트는 표 정렬 키보드 지원 전체(tableSort.js 1,793B vs 2,990B),
// 차트 매수/매도 라벨 현지화, 활동 패널 로딩, 시뮬레이터 대비 수정 7곳을 담지 못한 채
// 돌고 있었다. 빌드가 깨진 것도, 산출물이 뒤처진 것도 아무 곳에서도 실패하지 않았다.
//
// 이 파일이 만드는 지문을 FrontendAssetFreshnessTest 가 다시 계산해 대조한다.
// 소스를 고치고 빌드를 안 하면 그 시점에 테스트가 깨진다.
import { createHash } from "node:crypto";
import { readFileSync, writeFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve, sep } from "node:path";

const FRONTEND = resolve(".");
const OUT = resolve("../resources/frontend-build.json");
const CR = String.fromCharCode(13);

/** 지문에 넣을 소스. 빌드 결과를 바꿀 수 있는 것만 넣는다. */
const ROOTS = ["src"];
const FILES = ["main.css", "tailwind.config.js", "tsconfig.json", "package.json"];

function walk(dir, acc) {
  for (const entry of readdirSync(dir)) {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) walk(path, acc);
    else acc.push(path);
  }
  return acc;
}

// 개행은 체크아웃 설정(CRLF/LF)에 따라 달라지므로 지문에서 제외한다.
const digest = (path) =>
  createHash("sha256").update(readFileSync(path, "utf8").split(CR).join("")).digest("hex");

const toKey = (path) => relative(FRONTEND, path).split(sep).join("/");

const collected = [
  ...ROOTS.flatMap((root) => walk(join(FRONTEND, root), [])),
  ...FILES.map((name) => join(FRONTEND, name)),
];
const entries = collected
  .map((path) => [toKey(path), digest(path)])
  .sort(([a], [b]) => a.localeCompare(b));

writeFileSync(OUT, JSON.stringify(Object.fromEntries(entries), null, "\t") + "\n", "utf8");
console.log(`[build-manifest] ${entries.length} sources -> ${toKey(OUT)}`);
