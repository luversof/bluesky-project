// 트리 배경 레이어(클래스 일러스트) 추출 — GGG 트리 export 의 `extraImages` 가 좌표까지 준다.
// PoB 는 이 좌표를 하드코딩했지만("position data doesn't seem to be in the tree JSON yet"),
// 지금 export 에는 들어 있어 그대로 쓸 수 있다. 이미지는 게임 번들에서 뽑는다(스프라이트 시트엔 없음).
// 사용법: node tree-layers.mjs  (parse-tree.mjs 가 만든 raw 트리 필요)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadConfig, runExtractor } from "./paths.mjs";

const RAW = path.join(WORK_DIR, "passive-tree-raw.json");
const OUT_DIR = path.join(DATA_DIR, "icons", "tree-layers");

if (!fs.existsSync(RAW)) {
	console.warn("raw 트리 없음 — parse-tree.mjs 를 먼저 실행하세요:", RAW);
	process.exit(0);
}
const raw = JSON.parse(fs.readFileSync(RAW, "utf8"));
const images = Object.values(raw.extraImages || {});
if (!images.length) {
	console.log("extraImages 없음 — 건너뜀");
	process.exit(0);
}

// "Art/2DArt/BaseClassIllustrations/Str.png" → 번들 경로(소문자 .dds) / 산출 파일명
const wanted = new Map();
for (const entry of images) {
	if (!entry?.image) continue;
	const bundlePath = entry.image.toLowerCase().replace(/\.png$/, ".dds");
	wanted.set(bundlePath, path.basename(entry.image).toLowerCase());
}
// ⚠ 추출기는 files 목록을 **그대로 재생성**한다 — 기본 config 의 files(스탯 설명 txt)를 빼먹으면
// 그 파일들이 삭제돼 다음 parse-tree 실행에서 한글 스탯이 통째로 비어 버린다(실제로 겪음).
const baseConfig = loadConfig();
runExtractor({ ...baseConfig, tables: [], files: [...(baseConfig.files || []), ...wanted.keys()] }, { partial: true });

fs.mkdirSync(OUT_DIR, { recursive: true });
let copied = 0;
for (const [bundlePath, outName] of wanted) {
	// 추출물은 경로 구분자를 @ 로 바꿔 저장되고, DDS 는 PNG 로 변환돼 있다
	const extracted = path.join(FILES_DIR, bundlePath.replace(/\//g, "@").replace(/\.dds$/, ".png"));
	if (!fs.existsSync(extracted)) {
		console.warn("  누락:", bundlePath);
		continue;
	}
	fs.copyFileSync(extracted, path.join(OUT_DIR, outName));
	copied++;
}
console.log(`트리 배경 레이어 ${copied}/${wanted.size}개 → ${OUT_DIR}`);
