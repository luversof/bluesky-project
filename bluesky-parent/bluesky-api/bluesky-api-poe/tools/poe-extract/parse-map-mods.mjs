// 맵 모드(도메인 5 접두/접미) → map-mods.json — /poe/regex 지도 정규식 생성기용.
// 맵에 실제 스폰되는 모드만 담는다: 스폰웨이트가 low/mid/top_tier_map(일반) 또는 uber_tier_map(T17)
// 태그에 양수인 것. 같은 어픽스의 티어 변형(수치만 다름)은 이름+숫자 제거 문구로 병합하고
// 수치는 변형 전체를 아우르는 (min-max) 범위로 합친다. 수량/희귀도/무리 규모 표준 라인은
// 모든 맵 모드에 붙는 보상 라인이라 효과 라인과 분리해 배지용 필드로 담는다.
// 사용법: node parse-map-mods.mjs (사전: extract.mjs)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, FILES_DIR, loadConfig, loadTable } from "./paths.mjs";
import { createStatDescriber } from "./statDescriptions.mjs";

const OUT = path.join(DATA_DIR, "map-mods.json");

const mods = loadTable("English", "Mods");
const modsKo = loadTable("Korean", "Mods");
const stats = loadTable("English", "Stats");
const tags = loadTable("English", "Tags");
const describe = createStatDescriber(FILES_DIR, [
	"metadata@statdescriptions@map_stat_descriptions.txt",
]);

// 스폰웨이트는 **첫 매치 우선**: 모드의 태그 목록을 앞에서부터 베이스(맵)의 태그 집합과 대조해
// 처음 매치된 항목의 weight 로 판정한다(parse-mods-full 과 동일 규칙). 일반 맵과 T17(uber) 맵의
// 태그 집합을 각각 시뮬레이션한다. 서식 모드처럼 default:150 앞에 uber:0/secret:0 배제 게이트가
// 달린 모드가 많아, 티어 태그 양수만 보면 놓친다(실측).
const NORMAL_MAP_TAGS = new Set(["low_tier_map", "mid_tier_map", "top_tier_map", "map", "default"]);
const UBER_MAP_TAGS = new Set([
	"uber_tier_map",
	"has_uber_map_prefix",
	"has_uber_map_suffix",
	"map",
	"default",
]);
const GEN = { 1: "prefix", 2: "suffix" };

/** 맵 태그 집합에 대한 첫 매치 스폰웨이트 (매치 없음 = 0) */
function spawnWeightFor(spawnTags, mapTagSet) {
	for (const t of spawnTags) {
		if (mapTagSet.has(t.id)) return t.weight;
	}
	return 0;
}
const NUM = /-?\d+(?:\.\d+)?/g;

/** 모드 스탯 → 값 맵 (min/max 선택) — parse-mods-full 과 동일 규칙 */
function rollValues(mod, kind) {
	const values = new Map();
	for (let i = 1; i <= 6; i++) {
		const statIndex = mod["StatsKey" + i];
		if (statIndex == null) continue;
		const stat = stats[statIndex];
		if (!stat) continue;
		values.set(stat.Id, mod["Stat" + i + (kind === "min" ? "Min" : "Max")] ?? 0);
	}
	return values;
}

/** 두 문장의 수치를 자리별로 결합(fn=Math.min|Math.max). 구조가 다르면 a 유지. */
function combineNumbers(a, b, fn) {
	if (a == null) return b;
	if (b == null) return a;
	const numsB = b.match(NUM) || [];
	const numsA = a.match(NUM) || [];
	if (numsA.length !== numsB.length) return a;
	let i = 0;
	return a.replace(NUM, (n) => String(fn(Number(n), Number(numsB[i++]))));
}

/** min/max 문장 쌍 → 수치가 다르면 "(min-max)" 범위 표기 하나로 병합 */
function mergeRange(minLine, maxLine) {
	if (minLine == null || minLine === maxLine) return maxLine;
	const minNums = minLine.match(NUM) || [];
	const maxNums = maxLine.match(NUM) || [];
	if (minNums.length !== maxNums.length) return maxLine; // 구조가 다르면 max 기준
	let i = 0;
	return maxLine.replace(NUM, (n) => {
		const lo = minNums[i++];
		return lo === n ? n : `(${lo}-${n})`;
	});
}

// 수량/희귀도/무리 규모 표준 라인 (영문 기준으로 판정, ko 는 같은 인덱스 제거)
const QUANT_EN = /^(\d+)% increased Quantity of Items found in this Area$/;
const RARITY_EN = /^(\d+)% increased Rarity of Items found in this Area$/;
const PACK_EN = /^(\d+)% increased Pack size$/;

const entries = new Map(); // 병합 키 → entry (min/max 라인 배열을 들고 있다가 마지막에 범위 병합)
let sourceCount = 0;

mods.forEach((mod, index) => {
	if (!mod || mod.Domain !== 5 || !GEN[mod.GenerationType] || !mod.Name) return;
	const spawnTags = (mod.SpawnWeight_TagsKeys || []).map((t, j) => ({
		id: tags[t]?.Id,
		weight: mod.SpawnWeight_Values?.[j] ?? 0,
	}));
	const normal = spawnWeightFor(spawnTags, NORMAL_MAP_TAGS) > 0;
	const uber = spawnWeightFor(spawnTags, UBER_MAP_TAGS) > 0;
	if (!normal && !uber) return;
	sourceCount++;

	const enMax = describe(rollValues(mod, "max"), "English");
	const enMin = describe(rollValues(mod, "min"), "English");
	const koMax = describe(rollValues(mod, "max"), "Korean");
	const koMin = describe(rollValues(mod, "min"), "Korean");

	let quant = 0;
	let rarity = 0;
	let packSize = 0;
	const effectIdx = [];
	enMax.forEach((line, i) => {
		const q = line.match(QUANT_EN);
		const r = line.match(RARITY_EN);
		const p = line.match(PACK_EN);
		if (q) quant = Number(q[1]);
		else if (r) rarity = Number(r[1]);
		else if (p) packSize = Number(p[1]);
		else effectIdx.push(i);
	});
	if (effectIdx.length === 0) return; // 효과 없는 보상 전용 모드는 검색 대상 아님
	const pick = (arr) => effectIdx.map((i) => arr[i]).filter((s) => s != null);

	const key = mod.Name + "|" + pick(enMax).map((s) => s.replace(NUM, "#")).join("|");
	let entry = entries.get(key);
	if (!entry) {
		entry = {
			id: mod.Id,
			name: mod.Name,
			nameKo: modsKo[index]?.Name || mod.Name,
			gen: GEN[mod.GenerationType],
			normal: false,
			uber: false,
			quant: 0,
			rarity: 0,
			packSize: 0,
			enMin: pick(enMin),
			enMax: pick(enMax),
			koMin: pick(koMin),
			koMax: pick(koMax),
		};
		entries.set(key, entry);
	} else {
		// 티어 변형 결합 — 수치 자리별 min/max 를 넓힌다
		entry.enMin = entry.enMin.map((s, i) => combineNumbers(s, pick(enMin)[i], Math.min));
		entry.enMax = entry.enMax.map((s, i) => combineNumbers(s, pick(enMax)[i], Math.max));
		entry.koMin = entry.koMin.map((s, i) => combineNumbers(s, pick(koMin)[i], Math.min));
		entry.koMax = entry.koMax.map((s, i) => combineNumbers(s, pick(koMax)[i], Math.max));
	}
	entry.normal ||= normal;
	entry.uber ||= uber;
	entry.quant = Math.max(entry.quant, quant);
	entry.rarity = Math.max(entry.rarity, rarity);
	entry.packSize = Math.max(entry.packSize, packSize);
});

const list = [...entries.values()].map((e) => ({
	id: e.id,
	name: e.name,
	nameKo: e.nameKo,
	gen: e.gen,
	normal: e.normal,
	uber: e.uber,
	quant: e.quant,
	rarity: e.rarity,
	packSize: e.packSize,
	en: e.enMax.map((s, i) => mergeRange(e.enMin[i], s)),
	ko: e.koMax.map((s, i) => mergeRange(e.koMin[i], s)),
}));

// 정렬: 일반맵 먼저, 접두→접미, 한글 문구순
list.sort((a, b) => {
	if (a.normal !== b.normal) return a.normal ? -1 : 1;
	if (a.gen !== b.gen) return a.gen === "prefix" ? -1 : 1;
	return (a.ko[0] || "").localeCompare(b.ko[0] || "", "ko");
});

const config = loadConfig();
fs.writeFileSync(OUT, JSON.stringify({ patch: config.patch, mods: list }, null, "\t"), "utf8");
const normalCount = list.filter((m) => m.normal).length;
const uberCount = list.filter((m) => m.uber && !m.normal).length;
console.log(
	`map-mods.json: 원본 ${sourceCount}건 → ${list.length}종 (일반 ${normalCount}, T17 전용 ${uberCount}) → ${OUT}`,
);
