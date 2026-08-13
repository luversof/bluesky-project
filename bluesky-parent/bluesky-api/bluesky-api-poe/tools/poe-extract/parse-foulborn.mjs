// 삿된(Foulborn) 옵션 → foulborn-mods.json.
//
// 삿된 = Foulborn. 현재 리그 화폐로 **유니크의 기존 모드 하나를 다른 모드로 대체**한다.
// 인게임에선 "삿된 붉은 꿈" 처럼 아이템마다 붙을 수 있는 옵션이 정해져 있고, 그 아이템의 어느 모드가
// 무엇으로 바뀌는지도 정해져 있다. 그래서 이 산출물의 단위는 **유니크 1개 = 옵션 목록**이다.
//
// 소스 두 갈래:
//   ① PoB `Data/ModFoulbornMap.jsonc` — {유니크 영문명: {원본 모드 id: 삿된 모드 id}}.
//      poewiki 표를 정리한 것으로, **어느 유니크에 무엇이 붙는지 + 원본 무엇이 대체되는지**의 정답지다.
//      ⚠ 이게 필요한 이유: 삿된 모드 id 의 토큰(MutatedUnique**Jewel85**…)은 "그 모드가 처음 정의된 유니크"
//        일 뿐이라, 한 모드가 여러 유니크에 걸리면(실측 9건) 토큰만 보고는 나머지를 놓친다.
//        실제로 토큰 방식은 32종에서 개수가 어긋났고 묠니르·데이드벨 등은 아예 0개였다.
//   ② 게임 Mods 테이블 — 문구(한/영)와 롤 범위. 지도에 없는 삿된 모드(신규 패치 등)는 여기서만 나오므로,
//      토큰으로 유니크를 추정해 폴백으로 싣는다(추정이 실패하면 이름 없이 토큰만).
//
// 사용법: node parse-foulborn.mjs (사전: extract.mjs, parse-uniques.mjs, PoB 소스)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, WORK_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

const OUT = path.join(DATA_DIR, "foulborn-mods.json");

const mods = loadTable("English", "Mods");
const stats = loadTable("English", "Stats");
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@stat_descriptions.txt",
	"metadata@statdescriptions@passive_skill_stat_descriptions.txt",
]);

/** 모드의 스탯 → 값 맵 (min|max) — 다른 추출기와 동일 규칙 */
function rollValues(mod, kind) {
	const values = new Map();
	for (let i = 1; i <= 6; i++) {
		const si = mod["StatsKey" + i];
		if (si == null) continue;
		const stat = stats[si];
		if (stat) values.set(stat.Id, mod["Stat" + i + (kind === "min" ? "Min" : "Max")] ?? 0);
	}
	return values;
}

/** 표시 가치 없는 더미 스탯 줄 제거 — dummy_stat_display_nothing 등은 문구가 비거나 자리만 차지한다 */
const isMeaningful = (line) => typeof line === "string" && line.trim().length > 0;
const modById = new Map(mods.filter((m) => m?.Id).map((m) => [m.Id, m]));
/** 모드 id → 표시 문구(최대/최소 롤, 한/영). 못 찾으면 null. */
function lines(modId) {
	const mod = modById.get(modId);
	if (!mod) return null;
	const out = {
		en: describe(rollValues(mod, "max"), "English").filter(isMeaningful),
		ko: describe(rollValues(mod, "max"), "Korean").filter(isMeaningful),
		enMin: describe(rollValues(mod, "min"), "English").filter(isMeaningful),
		koMin: describe(rollValues(mod, "min"), "Korean").filter(isMeaningful),
	};
	return out.en.length || out.ko.length ? out : null;
}

// ⚠ 원본 id 에는 **GGG 쪽 오타와 중복 접두**가 그대로 있다(실측):
//   MutatedUniqueAmluet24…(Amulet 오타), MutatedUniqueBottsStr7…(Boots 오타), MutatedUniqueUniqueAmulet6…(Unique 두 번).
const CATEGORY_ALIAS = {
	Amluet: "Amulet",
	UniqueAmulet: "Amulet",
	UniqueBelt: "Belt",
	UniqueBow: "Bow",
	UniqueTwoHandMace: "TwoHandMace",
	UniqueTwoHandSword: "TwoHandSword",
	Botts: "Boots",
	Helm: "Helmet",
};

// 카테고리 → 화면 분류(한글). 매핑에 없으면 원값 그대로(임의 분류 금지 — 새 카테고리를 조용히 삼키지 않게).
const CATEGORY_KO = {
	Jewel: "주얼", Amulet: "목걸이", Ring: "반지", Belt: "허리띠", Helmet: "투구", Body: "갑옷",
	Gloves: "장갑", Boots: "장화", Shield: "방패", Quiver: "화살통", Bow: "활", Wand: "마법봉",
	Dagger: "단검", Claw: "클로", Sword: "검", OneHandSword: "한손검", TwoHandSword: "양손검",
	OneHandAxe: "한손도끼", TwoHandAxe: "양손도끼", OneHandMace: "한손철퇴", TwoHandMace: "양손철퇴",
	Axe: "도끼", Mace: "철퇴", Staff: "지팡이", Sceptre: "셉터", Flask: "플라스크", FishingRod: "낚싯대",
};
// 우리 고유 데이터의 category(파일명 유래) → 화면 분류. 지도 경로에서는 이쪽이 우선이다(아이템이 진실).
const UNIQUE_CATEGORY_KO = {
	amulet: "목걸이", ring: "반지", belt: "허리띠", helmet: "투구", body: "갑옷", gloves: "장갑",
	boots: "장화", shield: "방패", quiver: "화살통", bow: "활", wand: "마법봉", dagger: "단검",
	claw: "클로", sword: "검", axe: "도끼", mace: "철퇴", staff: "지팡이", jewel: "주얼",
	flask: "플라스크", fishing: "낚싯대", tincture: "팅크제",
};

// ── 유니크 이름/분류 사전 (parse-uniques 산출물) ────────────────────────────
// ⚠ 지도 키는 발음부호가 벗겨져 있다(생성 스크립트가 ö→o 로 바꾼다: Mjölner → "Mjolner").
//   그대로 대조하면 그 아이템만 연결이 끊겨 **상세에 삿된 섹션이 통째로 안 나온다**(실측 1건).
const nameKey = (name) => name.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
const uniqueByName = new Map();
try {
	const raw = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "unique-items.json"), "utf8"));
	for (const item of raw.items || []) {
		if (!uniqueByName.has(nameKey(item.name))) uniqueByName.set(nameKey(item.name), item);
	}
} catch {
	console.warn("unique-items.json 없음 — 분류/한글명 없이 진행");
}
/** 지도/토큰에서 온 이름으로 우리 고유 아이템 찾기(발음부호·대소문자 무시). */
const findUnique = (name) => (name ? uniqueByName.get(nameKey(name)) : null) || null;

// ── 토큰 → 유니크 이름 (폴백 경로) ─────────────────────────────────────────
// 유니크 스태시 탭 배치표(UniqueStashLayout)가 "이 아트(IVI)는 이 이름" 을 들고 있다.
const iviRows = loadTable("English", "ItemVisualIdentity");
const stashRows = loadTable("English", "UniqueStashLayout");
const wordsEnRows = loadTable("English", "Words");
const wordsKoRows = loadTable("Korean", "Words");
const stashByIvi = new Map(stashRows.map((r) => [r.ItemVisualIdentityKey, r]));
const nameByIviId = new Map();
iviRows.forEach((row, i) => {
	const stash = stashByIvi.get(i);
	if (!stash || !row.Id) return;
	nameByIviId.set(row.Id, {
		en: (wordsEnRows[stash.WordsKey] || {}).Text || null,
		ko: (wordsKoRows[stash.WordsKey] || {}).Text2 || null,
	});
});
const iviCandidates = [...nameByIviId.keys()]
	.map((id) => ({ key: id.replace(/_+$/, ""), id }))
	.sort((a, b) => b.key.length - a.key.length || a.id.length - b.id.length);
const ID_TYPOS = [[/Botts/, "Boots"], [/Amluet/, "Amulet"]];
function resolveUniqueByToken(modId) {
	const rest = modId.slice("Mutated".length);
	const tries = [rest, rest.replace(/^Unique/, "")];
	for (const t of [...tries]) {
		for (const [re, to] of ID_TYPOS) if (re.test(t)) tries.push(t.replace(re, to));
	}
	for (const t of tries) {
		const found = iviCandidates.find((c) => t.startsWith(c.key));
		if (found) return nameByIviId.get(found.id);
	}
	return null;
}

// ── ① PoB 지도 ────────────────────────────────────────────────────────────
function loadFoulbornMap() {
	const file = path.join(WORK_DIR, "pob-src", "src", "Data", "ModFoulbornMap.jsonc");
	if (!fs.existsSync(file)) {
		console.warn(`PoB 삿된 지도 없음(${file}) — 토큰 추정만으로 진행합니다(아이템별 옵션이 불완전해집니다)`);
		return null;
	}
	// jsonc: 줄 단위 // 주석만 쓰인다(파일 상단의 생성 스크립트 설명)
	const text = fs.readFileSync(file, "utf8").split("\n").filter((l) => !l.trim().startsWith("//")).join("\n");
	try {
		return JSON.parse(text);
	} catch (e) {
		console.warn("PoB 삿된 지도 파싱 실패 — 토큰 추정만으로 진행:", e.message);
		return null;
	}
}

const foulbornMap = loadFoulbornMap();
const groups = new Map(); // 유니크 영문명(없으면 토큰) → 그룹
const usedFoulIds = new Set();

const tokenOf = (modId) => {
	const m = modId.match(/^MutatedUnique([A-Za-z]+?)(\d+)/);
	return m ? m[1] + m[2] : modId.slice("Mutated".length);
};
const categoryOfToken = (modId) => {
	const m = modId.match(/^MutatedUnique([A-Za-z]+?)(\d+)/);
	if (!m) return null;
	const stripped = m[1].replace(/(StrDex|StrInt|DexInt|Str|Dex|Int)$/, "") || m[1];
	return CATEGORY_ALIAS[stripped] || stripped;
};

function groupFor(key, { uniqueName, uniqueNameKo, category, categoryKo, token }) {
	let group = groups.get(key);
	if (!group) {
		group = { token, category, categoryKo, uniqueName, uniqueNameKo, uniqueSlug: null, mods: [] };
		groups.set(key, group);
	}
	return group;
}

let mapped = 0;
let missingText = 0;
if (foulbornMap) {
	for (const [uniqueName, pairs] of Object.entries(foulbornMap)) {
		const item = findUnique(uniqueName);
		for (const [origId, foulId] of Object.entries(pairs)) {
			const foul = lines(foulId);
			if (!foul) {
				// 지도에는 있는데 게임 Mods 에 문구가 없다 = 이번 패치에 빠진 모드. 조용히 버리지 않고 센다.
				missingText++;
				continue;
			}
			const orig = lines(origId);
			const category = item ? item.category : categoryOfToken(foulId);
			const group = groupFor(item ? item.name : uniqueName, {
				uniqueName: item ? item.name : uniqueName,
				uniqueNameKo: item ? item.nameKo : (resolveUniqueByToken(foulId) || {}).ko || null,
				category,
				categoryKo: (item ? UNIQUE_CATEGORY_KO[item.category] : CATEGORY_KO[category]) || category,
				token: tokenOf(foulId),
			});
			if (item) group.uniqueSlug = item.slug;
			group.mods.push({
				id: foulId,
				en: foul.en, ko: foul.ko, enMin: foul.enMin, koMin: foul.koMin,
				// 이 옵션이 **무엇을 밀어내는지**. 원본 문구를 못 찾으면 null(대체 대상 불명으로 표시).
				origId,
				origEn: orig ? orig.en : null,
				origKo: orig ? orig.ko : null,
			});
			usedFoulIds.add(foulId);
			mapped++;
		}
	}
}

// ── ② 지도에 없는 삿된 모드 폴백 ──────────────────────────────────────────
let fallback = 0;
for (const mod of mods) {
	const id = mod?.Id || "";
	if (!id.startsWith("Mutated") || usedFoulIds.has(id)) continue;
	const text = lines(id);
	if (!text) continue;
	const guess = resolveUniqueByToken(id);
	const category = categoryOfToken(id);
	const item = guess ? findUnique(guess.en) : null;
	const key = guess && guess.en ? guess.en : tokenOf(id);
	const group = groupFor(key, {
		uniqueName: guess ? guess.en : null,
		uniqueNameKo: guess ? guess.ko : null,
		category,
		categoryKo: (item ? UNIQUE_CATEGORY_KO[item.category] : CATEGORY_KO[category]) || category,
		token: tokenOf(id),
	});
	if (item) group.uniqueSlug = item.slug;
	group.mods.push({
		id, en: text.en, ko: text.ko, enMin: text.enMin, koMin: text.koMin,
		origId: null, origEn: null, origKo: null,
	});
	fallback++;
}

const list = [...groups.values()].sort(
	(a, b) =>
		Number(!a.uniqueName) - Number(!b.uniqueName) ||
		String(a.categoryKo).localeCompare(String(b.categoryKo)) ||
		String(a.uniqueNameKo || a.uniqueName || a.token).localeCompare(
			String(b.uniqueNameKo || b.uniqueName || b.token),
		),
);
const byCategory = {};
for (const g of list) byCategory[g.categoryKo] = (byCategory[g.categoryKo] || 0) + g.mods.length;

const result = { patch: loadConfig().patch, groups: list, byCategory };
fs.writeFileSync(OUT, JSON.stringify(result));
const modCount = list.reduce((n, g) => n + g.mods.length, 0);
const withOrig = list.reduce((n, g) => n + g.mods.filter((m) => m.origEn).length, 0);
const named = list.filter((g) => g.uniqueName).length;
console.log(`foulborn-mods.json: 옵션 ${modCount}개 / 유니크 ${list.length}개 → ${OUT}`);
console.log(`  PoB 지도 ${mapped}개(원본 대응 ${withOrig}개) + 지도 밖 폴백 ${fallback}개, 문구 없는 지도 항목 ${missingText}개 제외`);
console.log(`  유니크 이름 해석 ${named}/${list.length} (미해석은 토큰만 노출)`);
if (named < list.length) {
	console.log("  미해석:", list.filter((g) => !g.uniqueName).map((g) => g.token).join(", "));
}
console.log("  분류별:", JSON.stringify(byCategory));
