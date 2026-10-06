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
	// 후행 토큰: 핸들러명 + 스탯 위치(1-base) 쌍 / reminderstring 은 ReminderText Id 로 따로 모은다(인게임 회색 부연 줄 — 10-04 C114, 서술 결과엔 영향 없음)
	const rest = tokens.slice(tokens.indexOf(textToken) + 1);
	const handlers = [];
	const reminders = [];
	for (let k = 0; k < rest.length; k++) {
		const name = rest[k].raw ?? "";
		if (name === "reminderstring") { if (rest[k + 1]?.raw) reminders.push(rest[k + 1].raw); k++; continue; }
		const next = rest[k + 1]?.raw;
		if (next !== undefined && /^\d+$/.test(next)) {
			handlers.push({ name, statIndex: Number(next) - 1 });
			k++;
		}
	}
	return reminders.length ? { conditions, text: textToken.quoted, handlers, reminders } : { conditions, text: textToken.quoted, handlers };
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

// 파일 단위 파싱 캐시 — 같은 파일을 여러 서술기가 쓰면(PoE2 젬: 스킬마다 공용 파일 + 스킬 전용 파일) 매번 다시 파싱하지 않는다.
// 블록 배열은 서술기가 읽기만 하므로 공유해도 결과는 같다. 파일이 바뀌면(mtime) 다시 파싱한다.
const parsedCache = new Map();
// PoE2 키워드 색인(poe2-extract/common2 createKeywordIndex, 10-04 C112)도 원문 블록을 읽는다
export function parsedBlocks(path) {
	const mtime = fs.statSync(path).mtimeMs;
	const hit = parsedCache.get(path);
	if (hit && hit.mtime === mtime) return hit.blocks;
	const blocks = [];
	parseFile(decode(path), blocks);
	parsedCache.set(path, { mtime, blocks });
	return blocks;
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
		if (fs.existsSync(path)) for (const b of parsedBlocks(path)) blocks.push(b);
	}
	// stat id → 블록 (뒤에 파싱된 블록이 우선)
	const blockByStat = new Map();
	for (const block of blocks) {
		for (const stat of block.stats) blockByStat.set(stat, block);
	}

	/** statValues: Map<statId, value> (표시 순서 유지) → 언어별 문장 배열 */
	//  고른 변형의 reminderstring Id 는 describe.reminders 에 결과와 같은 길이로 남긴다(마지막 호출 기준, 10-04 C115)
	const describe = function describe(statValues, lang) {
		const consumed = new Set();
		const result = [];
		const reminders = [];
		describe.reminders = reminders;
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
			reminders.push(variant.reminders || []);
		}
		return result;
	};
	describe.reminders = [];
	return describe;
}

export function reportUnknownHandlers() {
	return [...unknownHandlers];
}

/**
 * 영어 모드 문장 배열 → 한국어 배열 역번역기. 스탯 설명의 영어/한국어 변형 텍스트를 토큰(플레이스홀더+고정숫자) 단위로 정렬해
 * PoB 고유 아이템 explicit 을 옮긴다. PoB 가 쪼갠 멀티라인 모드는 인접 라인을 합쳐 시도. 실패 라인은 영어 원문 유지.
 */
/**
 * @param options.keepLineBreaks 게임 설명이 여러 줄("…tree" + "통로" · 아세나스의 두 모드처럼)이고 PoB 도 같은 수의 줄로 쪼갰으면 한국어도 그 줄 수대로 낸다
 *   (10-04 C93). 끄면 예전처럼 공백으로 한 줄 — 고유 툴팁에서 두 옵션이 한 줄로 이어 보이고 영어 · 한국어 줄 수가 어긋났다(PoE1 171종).
 */
export function createModTranslator(fileDir, extraFiles = [], options = {}) {
	const blocks = [];
	for (const name of [
		"metadata@statdescriptions@stat_descriptions.txt",
		"metadata@statdescriptions@gem_stat_descriptions.txt",
		"metadata@statdescriptions@active_skill_gem_stat_descriptions.txt",
		"metadata@statdescriptions@skill_stat_descriptions.txt",
		...extraFiles,
	]) {
		const path = fileDir + "/" + name;
		if (fs.existsSync(path)) for (const b of parsedBlocks(path)) blocks.push(b);
	}

	// 토큰 = 플레이스홀더({..}) 또는 숫자/범위(선행 +, 감싸는 괄호 포함). 둘 다 스켈레톤에서 sentinel 로 바꿔
	// 템플릿의 고정 숫자("2 seconds")도 PoB 라인 숫자와 정렬되게 한다.
	const TOKEN = /\+?\(?\{[^}]*\}\)?|\+?\(?-?\d[\d.\-]*\)?/g;
	const skel = (text) => text.replace(/\\n/g, " ").replace(TOKEN, "").replace(/\s+/g, " ").trim();
	const tokensOf = (text) => text.replace(/\\n/g, " ").match(TOKEN) || [];
	const isPlaceholder = (token) => token.includes("{");

	// 영어 스켈레톤 → { enTokens, koText } (먼저 등장한 것 우선)
	const bySkeleton = new Map();
	for (const block of blocks) {
		const en = block.variants.English || [];
		const ko = block.variants.Korean || [];
		for (let k = 0; k < en.length; k++) {
			if (!ko[k]) continue;
			const key = skel(en[k].text);
			if (key && !bySkeleton.has(key)) bySkeleton.set(key, { enTokens: tokensOf(en[k].text), koText: ko[k].text });
		}
	}

	// 단일 라인 번역
	function translateLine(englishLine) {
		const entry = bySkeleton.get(skel(englishLine));
		if (!entry) return null;
		const pobTokens = tokensOf(englishLine);
		if (pobTokens.length !== entry.enTokens.length) return null;
		// 영어/PoB 토큰은 1:1 정렬 — 플레이스홀더 인덱스별 PoB 값 (고정 숫자는 무시)
		const valueByIndex = new Map();
		let seq = 0;
		entry.enTokens.forEach((enToken, i) => {
			if (!isPlaceholder(enToken)) return;
			const m = enToken.match(/\{(\d*)/);
			valueByIndex.set(m && m[1] !== "" ? Number(m[1]) : seq++, pobTokens[i]);
		});
		let koSeq = 0;
		return entry.koText
			.replace(/\\n/g, "\n")
			.replace(/\+?\(?\{[^}]*\}\)?/g, (token) => {
				const m = token.match(/\{(\d*)/);
				const index = m && m[1] !== "" ? Number(m[1]) : koSeq++;
				return valueByIndex.has(index) ? valueByIndex.get(index) : "";
			})
			.split("\n")
			.map((part) => part.replace(/\s+/g, " ").trim())
			.filter(Boolean)
			.join("\n");
	}

	// 여러 explicit 라인을 한국어로 — PoB 가 한 모드를 여러 라인으로 쪼갠 경우(멀티라인) 합쳐서 시도.
	// 결과 배열은 원본과 길이가 다를 수 있다(합쳐진 모드는 한 줄). 실패 라인은 영어 원문 유지.
	return function translate(lines) {
		const result = [];
		for (let i = 0; i < lines.length; ) {
			let matched = false;
			// 단일 라인 우선, 실패 시에만 다음 라인들과 합쳐 시도 (멀티라인 모드)
			for (let window = 1; window <= Math.min(3, lines.length - i); window++) {
				const joined = lines.slice(i, i + window).join(" ");
				const ko = translateLine(joined);
				if (ko) {
					// 한국어 줄 수가 합친 영어 줄 수와 같을 때만 나눠 낸다(그 밖엔 한 줄 — 줄 맞춤이 깨지지 않게)
					const parts = ko.split("\n");
					if (options.keepLineBreaks && parts.length === window) result.push(...parts);
					else result.push(parts.join(" "));
					i += window;
					matched = true;
					break;
				}
			}
			if (!matched) {
				result.push(lines[i]);
				i++;
			}
		}
		return result;
	};
}

/**
 * 옵션 줄 → 인게임 리마인더(회색 부연) Id(10-04 C114). 번역기(createModTranslator)와 같은 영어 뼈대로 템플릿을 찾아
 * 그 템플릿의 reminderstring Id 를 돌려준다. 여러 줄로 쪼갠 모드(최대 3줄)는 마지막 줄에 붙인다(인게임도 모드 끝에 부연).
 * 반환: (lines) => lines 와 같은 길이의 Id 배열 배열
 */
export function createReminderIndex(fileDir, extraFiles = []) {
	const blocks = [];
	for (const name of [
		"metadata@statdescriptions@stat_descriptions.txt",
		"metadata@statdescriptions@gem_stat_descriptions.txt",
		"metadata@statdescriptions@active_skill_gem_stat_descriptions.txt",
		"metadata@statdescriptions@skill_stat_descriptions.txt",
		...extraFiles,
	]) {
		const path = fileDir + "/" + name;
		if (fs.existsSync(path)) for (const b of parsedBlocks(path)) blocks.push(b);
	}
	const TOKEN = /\+?\(?\{[^}]*\}\)?|\+?\(?-?\d[\d.\-]*\)?/g;
	// 번역기(createModTranslator)와 같은 뼈대 — 원문의 글자 그대로 "\n"(역슬래시+n) · 토큰 자리표시 \u0001(10-04 C127 검토 지적)
	const skel = (text) => text.replace(/\\n/g, " ").replace(TOKEN, "\u0001").replace(/\s+/g, " ").trim();
	const bySkeleton = new Map();
	for (const block of blocks) {
		for (const v of block.variants.English || []) {
			const key = skel(v.text);
			if (!key || bySkeleton.has(key)) continue;
			bySkeleton.set(key, v.reminders || []);
		}
	}
	return (lines) => {
		const out = lines.map(() => []);
		for (let i = 0; i < lines.length; ) {
			let step = 1;
			for (let w = 1; w <= Math.min(3, lines.length - i); w++) {
				const ids = bySkeleton.get(skel(lines.slice(i, i + w).join(" ")));
				if (ids !== undefined) {
					out[i + w - 1] = [...ids];
					step = w;
					break;
				}
			}
			i += step;
		}
		return out;
	};
}
