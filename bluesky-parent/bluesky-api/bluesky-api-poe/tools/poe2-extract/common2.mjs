// PoE2 파서 공용 도우미 — 마크업 제거·slug·스탯 설명(영/한)·PoB-PoE2 원본 받기.
import fs from "node:fs";
import path from "node:path";
import { createModTranslator, createStatDescriber } from "../poe-extract/statDescriptions.mjs";
import { FILES_DIR, WORK_DIR } from "./paths.mjs";

/** PoE2 문구 마크업 "[Critical|Critical Hit]" → "Critical Hit", "[Spirit]" → "Spirit". 한국어도 같은 규칙. */
export function stripMarkup(text) {
	if (text == null) return text;
	return String(text).replace(/\[([^\]|]*)\|([^\]]*)\]/g, "$2").replace(/\[([^\]|]*)\]/g, "$1");
}

/** 영문 이름 → 소문자 kebab slug(중복이면 -2, -3 …). */
export function makeSlugger() {
	const used = new Map();
	return (name) => {
		const base =
			String(name)
				.toLowerCase()
				.replace(/['’]/g, "")
				.replace(/[^a-z0-9]+/g, "-")
				.replace(/^-+|-+$/g, "") || "item";
		const n = (used.get(base) || 0) + 1;
		used.set(base, n);
		return n === 1 ? base : `${base}-${n}`;
	};
}

/**
 * 스탯 설명 파일 체인 — 용도별로 다르다. 서술기는 뒤 파일이 이기는 구조라, 젬 설명 파일(gem/skill)을 아이템에 섞으면 같은 스탯이
 * "Supported Skills have …"(보조 대상 스킬 …) 젬 문구로 덮인다(옵션 171줄 오염 실측 2026-09-30). 게임의 include 체인과 같게:
 *   item    = stat_descriptions (장비·증강물·베이스 암시)
 *   gem     = stat + gem + active_skill_gem + skill
 *   passive = stat + passive_skill (패시브 트리)
 */
export const CSD_CHAINS = {
	item: ["data@statdescriptions@stat_descriptions.csd"],
	gem: [
		"data@statdescriptions@stat_descriptions.csd",
		"data@statdescriptions@gem_stat_descriptions.csd",
		"data@statdescriptions@active_skill_gem_stat_descriptions.csd",
		"data@statdescriptions@skill_stat_descriptions.csd",
	],
	passive: ["data@statdescriptions@stat_descriptions.csd", "data@statdescriptions@passive_skill_stat_descriptions.csd"],
	// 아틀라스 패시브 — 아틀라스 전용 스탯(map_*·atlas_*)은 atlas_stat_descriptions 에, 일부 지도 스탯은 map_stat_descriptions 에 있다
	atlas: [
		"data@statdescriptions@stat_descriptions.csd",
		"data@statdescriptions@map_stat_descriptions.csd",
		"data@statdescriptions@atlas_stat_descriptions.csd",
	],
};
export const CSD_FILES = CSD_CHAINS.gem;
export const PASSIVE_CSD = "data@statdescriptions@passive_skill_stat_descriptions.csd";

/**
 * 서술기에 넘기기 전 사본(work/statdesc-clean)을 고친다 — PoE1 서술기는 PoE1 DSL 기준이라 PoE2 파일을 그대로 먹이면 두 곳에서 틀린다
 * (parse-tree2 에서 찾은 것):
 *  (a) `table_only` 변형 = 거래소 표 전용 축약문("…@{0}%")이 맨 앞에 있어 먼저 골라진다 → 지우고 변형 개수를 1 줄인다.
 *  (b) per_minute_to_per_second = PoE1 서술기는 정수 반올림(150/60→3)이지만 PoE2 표기는 2.5 → 소수 유지 핸들러로.
 *  (c) 변형이 **전부** table_only 인 정의는 통째로 뺀다 — 빈 정의가 남으면 뒤 파일이 이기는 규칙 때문에 앞 파일의 제대로 된 문장을
 *      가린다(보조젬: gem_stat_descriptions 의 "Supported Skill has …" 를 skill_stat_descriptions 의 표 전용 정의가 덮어
 *      86개 젬의 문장이 비었다, 2026-09-30).
 */
export const CLEAN_DIR = path.join(WORK_DIR, "statdesc-clean2");
export function cleanCopies(files) {
	fs.mkdirSync(CLEAN_DIR, { recursive: true });
	for (const name of files) {
		const src = path.join(FILES_DIR, name);
		const dst = path.join(CLEAN_DIR, name);
		if (!fs.existsSync(src)) continue;
		if (fs.existsSync(dst) && fs.statSync(dst).mtimeMs >= fs.statSync(src).mtimeMs) continue;
		let text = fs.readFileSync(src).toString("utf16le");
		if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
		const out = [];
		// 정의(description) 단위로 모았다가, 영어 변형이 하나도 안 남으면 버린다
		let block = null;
		let firstCount = -1; // 이 정의의 첫(영어) 변형 개수 줄 위치(block 안)
		const flush = () => {
			if (!block) return;
			const englishLeft = firstCount >= 0 ? Number(block[firstCount].trim()) : 1;
			if (englishLeft > 0) out.push(...block);
			block = null;
			firstCount = -1;
		};
		let countIdx = -1;
		for (const line of text.split(/\r?\n/)) {
			if (/^\s*description\b/.test(line)) {
				flush();
				block = [line];
				continue;
			}
			const target = block || out;
			if (/^\s*\d+\s*$/.test(line)) {
				countIdx = target.length;
				if (block && firstCount < 0) firstCount = countIdx;
			}
			if (/^\s+[^"]*\btable_only\s+"/.test(line) && countIdx >= 0) {
				target[countIdx] = target[countIdx].replace(/\d+/, (n) => String(Number(n) - 1));
				continue;
			}
			target.push(line.replace(/("\s.*?)\bper_minute_to_per_second(?=\s+\d)/g, (m, pre) => pre + "per_minute_to_per_second_2dp_if_required"));
		}
		flush();
		fs.writeFileSync(dst, Buffer.from("﻿" + out.join("\r\n"), "utf16le"));
	}
}

/** describe(Map<statId, value>, lang) → 마크업을 걷어 낸 문장 배열. chain = item | gem | passive */
export function createDescriber(chain = "item") {
	const files = CSD_CHAINS[chain];
	cleanCopies(files);
	const describe = createStatDescriber(CLEAN_DIR, files);
	// table_only 를 지워도 표 형식만 있는 설명("라벨@값")이 남으면 "라벨: 값" 으로 보인다
	return (statValues, lang) => describe(statValues, lang).map((line) => stripMarkup(line).replace(/^([^@\n]+)@([^@\n]+)$/, "$1: $2"));
}

/** 영어 문장 배열 → 한국어(실패 줄은 영어 유지). chain = item | gem | passive */
export function createTranslator(chain = "item") {
	const files = CSD_CHAINS[chain];
	cleanCopies(files);
	const translate = createModTranslator(CLEAN_DIR, files);
	return (lines) => translate(lines.map(stripMarkup)).map(stripMarkup);
}

/** PathOfBuilding-PoE2(dev) 원본 파일 — work/pob 에 캐시(없을 때만 받는다, refresh=true 면 다시). */
export async function pobFile(relPath, { refresh = false } = {}) {
	const dir = path.join(WORK_DIR, "pob");
	fs.mkdirSync(dir, { recursive: true });
	const local = path.join(dir, relPath.replace(/[\\/]/g, "@"));
	if (!refresh && fs.existsSync(local)) return fs.readFileSync(local, "utf8");
	const url = `https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/dev/${relPath}`;
	const res = await fetch(url);
	if (!res.ok) throw new Error(`PoB-PoE2 ${relPath}: HTTP ${res.status}`);
	const text = await res.text();
	fs.writeFileSync(local, text);
	return text;
}

/** 한 row 의 Stat1..N / Stat1Value.. 를 Map 으로 (interval 값이면 [min,max] → 중간 대신 min/max 를 따로 준다). */
export function statPairs(row, stats, count = 6) {
	const out = [];
	for (let i = 1; i <= count; i++) {
		const key = row["Stat" + i];
		if (key == null) continue;
		const v = row["Stat" + i + "Value"];
		out.push([stats[key].Id, Array.isArray(v) ? v : [v, v]]);
	}
	return out;
}
