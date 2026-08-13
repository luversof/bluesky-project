// 고유 아이템 **전용 아이콘** 추출 — 지금은 베이스 아이콘으로 대체돼 "고유 주얼이 일반 주얼로 보이는" 문제가 있다.
// 연결 고리: UniqueStashLayout(WordsKey → ItemVisualIdentityKey) → ItemVisualIdentity.DDSFile → 번들에서 PNG.
// 사용법: node unique-icons.mjs  (ImageMagick 필요 — 없으면 이 단계만 건너뜀)
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, TABLES_DIR, findImageMagick, loadConfig, loadTable, runExtractor } from "./paths.mjs";

const magickDir = findImageMagick();
if (!magickDir) {
	console.warn("ImageMagick 이 없어 고유 아이콘 단계를 건너뜁니다.");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");
const OUT_DIR = path.join(DATA_DIR, "icons", "uniques");
const uniquesFile = path.join(DATA_DIR, "unique-items.json");
if (!fs.existsSync(uniquesFile)) {
	console.warn("unique-items.json 없음 — parse-uniques.mjs 를 먼저 실행하세요");
	process.exit(0);
}
const raw = JSON.parse(fs.readFileSync(uniquesFile, "utf8"));
const items = Array.isArray(raw) ? raw : raw.items || [];

// parse-anoints 의 부분 재추출(테이블 6종 대체)이 Words/UniqueStashLayout 을 지운다 — 없으면 전체 테이블을 복원한다.
// (run-all 전체 실행에서 항상 밟는 경로 — 3.29.0.1 원클릭 갱신 2연속 실패의 원인이었다)
if (["Words", "UniqueStashLayout", "ItemVisualIdentity"].some((t) => !fs.existsSync(path.join(TABLES_DIR, "English", t + ".json")))) {
	console.log("필요 테이블 누락 — 기본 config 로 전체 테이블 재추출");
	runExtractor(loadConfig());
}

const words = loadTable("English", "Words");
const layout = loadTable("English", "UniqueStashLayout");
const visual = loadTable("English", "ItemVisualIdentity");
// ⚠ 한 이름에 배치 행이 여러 개 붙는다(실측 135종). 두 부류다:
//   (1) **대체 아트**(미스터리 박스 스킨 등) — id 가 AlternateArt* 이고, 인게임 기본 모습이 아니다.
//   (2) 변형 아트 — 임프레션스 a~e 처럼 변형마다 그림이 다른 정상 케이스.
//   예전엔 그냥 덮어써서 **마지막 행이 이겼고**, 그 결과 117종이 스킨 아이콘으로 나갔다(화염의 망토 등).
//   그래서 대체 아트를 먼저 걸러내고, 남은 것 중 **첫 행**(정식/첫 변형)을 쓴다.
const rowsByName = new Map();
for (const row of layout) {
	const word = words[row.WordsKey];
	const art = visual[row.ItemVisualIdentityKey];
	if (!word?.Text || !art?.DDSFile) continue;
	const list = rowsByName.get(word.Text) || [];
	list.push({ id: art.Id || "", dds: art.DDSFile.toLowerCase() });
	rowsByName.set(word.Text, list);
}
const ddsByName = new Map();
let skinSkipped = 0;
for (const [name, list] of rowsByName) {
	const canonical = list.filter((r) => !/^AlternateArt/i.test(r.id));
	// 스킨밖에 없는 아이템이면 그거라도 쓴다(아이콘 없는 것보다 낫다)
	const pick = canonical.length ? canonical[0] : list[0];
	if (canonical.length && canonical.length < list.length) skinSkipped++;
	ddsByName.set(name, pick.dds);
}
if (skinSkipped) {
	console.log(`대체 아트(스킨) 제외 ${skinSkipped}종 — 인게임 기본 아트를 씁니다`);
}

// slug → dds (우리가 가진 고유만)
const ddsBySlug = new Map();
for (const item of items) {
	const dds = ddsByName.get(item.name);
	if (dds) ddsBySlug.set(item.slug, dds);
}
console.log(`고유 ${items.length}개 중 아이콘 경로 확보 ${ddsBySlug.size}개 (${Math.round((ddsBySlug.size / items.length) * 100)}%)`);
if (!ddsBySlug.size) {
	process.exit(0);
}

// ⚠ 추출기는 tables/files 목록을 **그대로 재생성**한다 — 기본 config 의 것을 반드시 합쳐서 넘긴다.
// (빼먹으면 이미 추출해 둔 테이블·스탯 설명 파일이 삭제돼 다음 단계가 조용히 망가진다)
const baseConfig = loadConfig();
const ddsFiles = [...new Set(ddsBySlug.values())];
runExtractor({ ...baseConfig, tables: baseConfig.tables, files: [...(baseConfig.files || []), ...ddsFiles] });

// slug → category (플라스크 3프레임 합성 판단용)
const catBySlug = new Map(items.map((i) => [i.slug, i.category]));

fs.mkdirSync(OUT_DIR, { recursive: true });
let done = 0, flaskComposited = 0;
for (const [slug, dds] of ddsBySlug) {
	const extracted = path.join(FILES_DIR, dds.replace(/\//g, "@").replace(/\.dds$/, ".png"));
	if (!fs.existsSync(extracted)) continue;
	const out = path.join(OUT_DIR, `${slug}.png`);
	// 플라스크 아이콘은 [껍데기|마스크|내용물] 3프레임 가로 스트립(236x156)으로 추출된다.
	// 그대로 쓰면 3개가 붙어 보이므로, 내용물(frame3) 위에 껍데기(frame1)를 얹어 채워진 플라스크 1개로 합성한다.
	let composited = false;
	if (catBySlug.get(slug) === "flask") {
		try {
			const b = fs.readFileSync(extracted);
			if (b.readUInt32BE(16) === 236 && b.readUInt32BE(20) === 156) {
				// ⚠ -flatten 은 기본 흰색 배경에 합성해 투명영역이 하얘진다 → 반드시 -background none 로 투명 유지.
				execFileSync(MAGICK, ["(", extracted, "-crop", "78x156+158+0", "+repage", ")", "(", extracted, "-crop", "79x156+0+0", "+repage", ")", "-background", "none", "-flatten", out]);
				composited = true;
				flaskComposited++;
			}
		} catch (e) {
			// 합성 실패 시 원본 복사로 폴백
		}
	}
	if (!composited) fs.copyFileSync(extracted, out);
	done++;
}
if (flaskComposited) console.log(`플라스크 3프레임 합성: ${flaskComposited}개`);
console.log(`고유 아이콘 ${done}/${ddsBySlug.size}개 → ${OUT_DIR}`);
