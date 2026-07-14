// GGG 스탯 설명 DSL 파서 — stat id + 값 → 사람이 읽는 문장 ("Deals 1640 to 2460 Fire Damage").
// 파일 형식(UTF-16LE):
//   description [이름]
//   \t<개수> statId1 statId2 ...
//   \t<변형수>                  ← 영어(기본) 섹션
//   \t\t<조건들> "텍스트 {0}" [핸들러 인덱스]...
//   \tlang "Korean"             ← 언어 섹션 반복
//   ...
import fs from "node:fs";

// 값 변환 핸들러 — 게임이 내부 값을 표시 값으로 바꿀 때 쓰는 함수들 (모르는 건 항등 처리 후 로그)
const HANDLERS = {
	negate: (v) => -v,
	invert_chance: (v) => 100 - v,
	double: (v) => v * 2,
	milliseconds_to_seconds: (v) => v / 1000,
	milliseconds_to_seconds_0dp: (v) => Math.round(v / 1000),
	milliseconds_to_seconds_1dp: (v) => Math.round(v / 100) / 10,
	milliseconds_to_seconds_2dp: (v) => Math.round(v / 10) / 100,
	milliseconds_to_seconds_2dp_if_required: (v) => Math.round(v / 10) / 100,
	deciseconds_to_seconds: (v) => v / 10,
	divide_by_two_0dp: (v) => Math.round(v / 2),
	divide_by_three: (v) => v / 3,
	divide_by_four: (v) => v / 4,
	divide_by_five: (v) => v / 5,
	divide_by_six: (v) => v / 6,
	divide_by_ten_0dp: (v) => Math.round(v / 10),
	divide_by_ten_1dp: (v) => Math.round(v) / 10,
	divide_by_ten_1dp_if_required: (v) => Math.round(v) / 10,
	divide_by_twelve: (v) => v / 12,
	divide_by_fifteen_0dp: (v) => Math.round(v / 15),
	divide_by_twenty_then_double_0dp: (v) => Math.round(v / 20) * 2,
	divide_by_fifty: (v) => v / 50,
	divide_by_one_hundred: (v) => v / 100,
	divide_by_one_hundred_2dp: (v) => Math.round(v) / 100,
	divide_by_one_hundred_2dp_if_required: (v) => Math.round(v) / 100,
	divide_by_one_hundred_and_negate: (v) => -v / 100,
	divide_by_one_thousand: (v) => v / 1000,
	per_minute_to_per_second: (v) => Math.round(v / 60),
	per_minute_to_per_second_0dp: (v) => Math.round(v / 60),
	per_minute_to_per_second_1dp: (v) => Math.round(v / 6) / 10,
	per_minute_to_per_second_2dp: (v) => Math.round(v / 0.6) / 100,
	per_minute_to_per_second_2dp_if_required: (v) => Math.round(v / 0.6) / 100,
	multiplicative_damage_modifier: (v) => v + 100,
	multiplicative_permyriad_damage_modifier: (v) => v / 100 + 100,
	"30%_of_value": (v) => v * 0.3,
	"60%_of_value": (v) => v * 0.6,
	old_leech_percent: (v) => v / 5,
	old_leech_permyriad: (v) => v / 500,
	times_twenty: (v) => v * 20,
	times_one_point_five: (v) => v * 1.5,
	plus_two_hundred: (v) => v + 200,
	locations_to_metres: (v) => v / 10,
	metres_to_locations: (v) => v * 10,
};
const unknownHandlers = new Set();

function decode(file) {
	const buf = fs.readFileSync(file);
	let text = buf.toString("utf16le");
	if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
	return text;
}

// 변형 라인 파싱: 조건 n개, "텍스트", 후행 핸들러 토큰들
function parseVariant(line, statCount) {
	const tokens = [];
	let i = 0;
	while (i < line.length) {
		if (/\s/.test(line[i])) { i++; continue; }
		if (line[i] === '"') {
			const end = line.indexOf('"', i + 1);
			tokens.push({ quoted: line.slice(i + 1, end) });
			i = end + 1;
		} else {
			let j = i;
			while (j < line.length && !/\s/.test(line[j])) j++;
			tokens.push({ raw: line.slice(i, j) });
			i = j;
		}
	}
	const conditions = tokens.slice(0, statCount).map((t) => t.raw);
	const textToken = tokens.slice(statCount).find((t) => t.quoted !== undefined);
	if (!textToken) return null;
	// 후행 토큰: 핸들러명 + 스탯 위치(1-base) 쌍 / reminderstring 은 무시
	const rest = tokens.slice(tokens.indexOf(textToken) + 1);
	const handlers = [];
	for (let k = 0; k < rest.length; k++) {
		const name = rest[k].raw ?? "";
		if (name === "reminderstring") { k++; continue; }
		const next = rest[k + 1]?.raw;
		if (next !== undefined && /^\d+$/.test(next)) {
			handlers.push({ name, statIndex: Number(next) - 1 });
			k++;
		}
	}
	return { conditions, text: textToken.quoted, handlers };
}

function parseFile(text, blocks) {
	const lines = text.split(/\r?\n/);
	let i = 0;
	while (i < lines.length) {
		const line = lines[i].trim();
		if (!line.startsWith("description")) { i++; continue; }
		i++;
		const statLine = lines[i]?.trim();
		const statMatch = statLine?.match(/^(\d+)\s+(.+)$/);
		if (!statMatch) continue;
		const stats = statMatch[2].trim().split(/\s+/).slice(0, Number(statMatch[1]));
		i++;
		const block = { stats, variants: { English: [] } };
		let lang = "English";
		while (i < lines.length) {
			const sectionLine = lines[i].trim();
			const langMatch = sectionLine.match(/^lang "(.+)"$/);
			if (langMatch) { lang = langMatch[1]; i++; continue; }
			if (!/^\d+$/.test(sectionLine)) break; // 다음 description 등
			const count = Number(sectionLine);
			i++;
			const variants = [];
			for (let v = 0; v < count && i < lines.length; v++, i++) {
				const parsed = parseVariant(lines[i], stats.length);
				if (parsed) variants.push(parsed);
			}
			block.variants[lang] = variants;
		}
		blocks.push(block);
	}
}

function conditionMatches(condition, value) {
	if (condition === "#") return true;
	if (condition.includes("|")) {
		const [lo, hi] = condition.split("|");
		if (lo !== "#" && value < Number(lo)) return false;
		if (hi !== "#" && value > Number(hi)) return false;
		return true;
	}
	if (condition.startsWith("!")) return value !== Number(condition.slice(1));
	return value === Number(condition);
}

function formatValue(value) {
	if (Number.isInteger(value)) return String(value);
	return String(Math.round(value * 100) / 100);
}

/** @param extraFiles 마지막에 파싱되어 최우선 적용되는 추가 설명 파일 (예: passive_skill_stat_descriptions) */
export function createStatDescriber(fileDir, extraFiles = []) {
	const blocks = [];
	// 뒤 파일이 앞 파일을 include 하는 구조 — 범용(stat) → 전용 순으로 파싱하고 뒤가 우선하도록 나중에 색인
	for (const name of [
		"metadata@statdescriptions@stat_descriptions.txt",
		"metadata@statdescriptions@gem_stat_descriptions.txt",
		"metadata@statdescriptions@active_skill_gem_stat_descriptions.txt",
		"metadata@statdescriptions@skill_stat_descriptions.txt",
		...extraFiles,
	]) {
		const path = fileDir + "/" + name;
		if (fs.existsSync(path)) parseFile(decode(path), blocks);
	}
	// stat id → 블록 (뒤에 파싱된 블록이 우선)
	const blockByStat = new Map();
	for (const block of blocks) {
		for (const stat of block.stats) blockByStat.set(stat, block);
	}

	/** statValues: Map<statId, value> (표시 순서 유지) → 언어별 문장 배열 */
	return function describe(statValues, lang) {
		const consumed = new Set();
		const result = [];
		for (const [statId] of statValues) {
			if (consumed.has(statId)) continue;
			const block = blockByStat.get(statId);
			if (!block) { consumed.add(statId); continue; }
			block.stats.forEach((s) => consumed.add(s));
			const values = block.stats.map((s) => statValues.get(s) ?? 0);
			if (values.every((v) => v === 0)) continue;
			const variants = block.variants[lang]?.length
				? block.variants[lang]
				: block.variants.English;
			const variant = variants.find((v) =>
				v.conditions.every((c, idx) => conditionMatches(c, values[idx])),
			);
			if (!variant) continue;
			const display = [...values];
			for (const h of variant.handlers) {
				const fn = HANDLERS[h.name];
				if (!fn) { unknownHandlers.add(h.name); continue; }
				if (h.statIndex >= 0 && h.statIndex < display.length) {
					display[h.statIndex] = fn(display[h.statIndex]);
				}
			}
			let sequential = 0;
			const text = variant.text
				.replace(/\{(\d*)(?::([^}]*))?\}/g, (m, idx, spec) => {
					const position = idx === "" ? sequential++ : Number(idx);
					const value = display[position];
					if (value === undefined) return m;
					const formatted = formatValue(value);
					return spec && spec.includes("+") && value > 0 ? "+" + formatted : formatted;
				})
				.replace(/\\n/g, "\n");
			result.push(text);
		}
		return result;
	};
}

export function reportUnknownHandlers() {
	return [...unknownHandlers];
}
