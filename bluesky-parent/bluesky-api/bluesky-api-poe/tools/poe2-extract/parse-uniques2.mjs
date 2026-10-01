// PoE2 고유 아이템 — PoB-PoE2(Path of Building PoE2, MIT) 고유 데이터 → 영/한 표시용 JSON.
// 영어 모드 텍스트는 PoB 원문을 쓰고, 한국어는 게임 데이터로 옮긴다.
//   이름 = Words(Wordlist 6, 고유 이름) Text → 한국어 Text2, 베이스 = BaseItemTypes.Name, 분류 = ItemClasses
//   모드 라인 = PoE2 스탯 설명(.csd)의 영/한 변형 템플릿 역번역(PoE1 createModTranslator 재사용)
// 사용법: node parse-uniques2.mjs  (사전 조건: extract.mjs 로 tables/, export-files.mjs 로 files/*.csd 추출 완료)
// 산출물: ~/.poe-gamedata/poe2/uniques.json
import fs from "node:fs";
import path from "node:path";
import { FILES_DIR, WORK_DIR, loadConfig, loadTable, writeJson } from "./paths.mjs";
import { createModTranslator } from "../poe-extract/statDescriptions.mjs";

const PATCH = loadConfig().patch;
const POB_DIR = path.join(WORK_DIR, "pob-uniques");
const POB_BRANCH = "dev";
const POB_FILES = [
	"amulet", "axe", "belt", "body", "boots", "bow", "claw", "crossbow", "dagger", "fishing", "flail", "flask",
	"focus", "gloves", "helmet", "incursionlimb", "jewel", "mace", "quiver", "ring", "sceptre", "shield",
	"soulcore", "spear", "staff", "sword", "talisman", "tincture", "traptool", "wand",
];

// ─── PoB 원본 내려받기 ───────────────────────────────────────────────────────────
// 매 실행 최신본을 받는다(PoE1 에서 existsSync 캐시가 신규 고유를 영원히 막은 적이 있다).
// 네트워크가 안 되면 캐시로 진행하고, 캐시도 없으면 실패한다.
fs.mkdirSync(POB_DIR, { recursive: true });
let refreshed = 0;
let cached = 0;
for (const name of POB_FILES) {
	const target = path.join(POB_DIR, name + ".lua");
	const url = `https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/${POB_BRANCH}/src/Data/Uniques/${name}.lua`;
	try {
		const response = await fetch(url);
		if (!response.ok) throw new Error(`HTTP ${response.status}`);
		const next = await response.text();
		const prev = fs.existsSync(target) ? fs.readFileSync(target, "utf8") : null;
		if (next !== prev) {
			fs.writeFileSync(target, next);
			refreshed++;
		}
	} catch (e) {
		if (!fs.existsSync(target)) throw new Error(`PoB 다운로드 실패(캐시 없음): ${url} — ${e.message}`);
		cached++;
	}
}
console.log(`PoB-PoE2 유니크 원본: 갱신 ${refreshed}개, 오프라인 캐시 사용 ${cached}개`);

// ─── 스탯 설명 평문 사본 ─────────────────────────────────────────────────────────
// PoE2 스탯 설명엔 "[Critical|Critical Hit]" 같은 용어 링크 마크업이 있다(표시 텍스트는 마지막 '|' 뒤).
// PoE1 번역기는 마크업을 모르므로 **마크업을 벗긴 사본**을 만들어 넘긴다. 사본에서는
//   - 영어/한국어 섹션만 남기고(30MB → 수 MB, 파싱 시간 단축)
//   - 영어 템플릿은 소문자로 바꾼다 — PoB 라인과 게임 원문의 대소문자가 어긋나는 일이 잦아서(비교는 소문자끼리).
//     한국어 결과는 한국어 템플릿 + PoB 숫자로만 만들어지므로 영어 소문자화는 결과에 영향이 없다.
const stripMarkup = (text) => {
	let prev;
	let out = text;
	do {
		prev = out;
		out = out.replace(/\[([^\[\]]*)\]/g, (_, inner) => {
			const parts = inner.split("|");
			return parts[parts.length - 1] || parts[0];
		});
	} while (out !== prev);
	return out;
};
const CSD_FILES = [
	"data@statdescriptions@stat_descriptions.csd",
	"data@statdescriptions@gem_stat_descriptions.csd",
	"data@statdescriptions@active_skill_gem_stat_descriptions.csd",
	"data@statdescriptions@skill_stat_descriptions.csd",
	"data@statdescriptions@passive_skill_stat_descriptions.csd",
];
const PLAIN_DIR = path.join(WORK_DIR, "statdesc-plain");
const PLAIN_VERSION = "1"; // 사본 만드는 규칙을 바꾸면 올린다 — 원본이 그대로여도 사본을 다시 만든다
fs.mkdirSync(PLAIN_DIR, { recursive: true });
const plainStamp = path.join(PLAIN_DIR, "version.txt");
const plainFresh = fs.existsSync(plainStamp) && fs.readFileSync(plainStamp, "utf8") === PLAIN_VERSION;
for (const name of CSD_FILES) {
	const src = path.join(FILES_DIR, name);
	const dst = path.join(PLAIN_DIR, name);
	if (!fs.existsSync(src)) throw new Error(`스탯 설명 파일 없음: ${src} (export-files.mjs 먼저)`);
	if (plainFresh && fs.existsSync(dst) && fs.statSync(dst).mtimeMs >= fs.statSync(src).mtimeMs) continue;
	let text = fs.readFileSync(src).toString("utf16le");
	if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
	const out = [];
	let skip = false;
	let english = false;
	for (const line of text.split(/\r?\n/)) {
		const trimmed = line.trim();
		const lang = trimmed.match(/^lang "(.+)"$/);
		if (lang) {
			skip = lang[1] !== "Korean";
			english = false;
			if (!skip) out.push(line);
			continue;
		}
		// 들여쓰기 없는 줄(description / include / no_description / 빈 줄)은 섹션 경계
		if (!/^\s/.test(line) || trimmed.startsWith("description")) {
			skip = false;
			english = trimmed.startsWith("description");
			out.push(line);
			continue;
		}
		if (skip) continue;
		const plain = stripMarkup(line);
		out.push(english ? plain.toLowerCase() : plain);
	}
	fs.writeFileSync(dst, Buffer.from("﻿" + out.join("\r\n"), "utf16le"));
}
fs.writeFileSync(plainStamp, PLAIN_VERSION);

// 멀티라인 템플릿 조각 사전 — 게임 템플릿은 "A\nB\nC" 한 덩어리인데 PoB 는 그중 일부 줄만 적는 일이 있다
//   (타임리스 주얼 "Glorifying …" 과 "Passives in radius …" 만 적고 셋째 줄은 생략 등). 번역기는 통째 일치만 보므로
//   영/한 조각 수가 같고 조각마다 자리표시자 집합이 같은 것만 **조각 단위 가짜 설명 블록**으로 만들어 마지막 파일로 넘긴다
//   (번역기는 먼저 본 스켈레톤 우선이라 진짜 템플릿이 항상 이긴다).
const SEGMENT_FILE = "poe2-unique-segments.csd";
{
	const placeholders = (s) => [...s.matchAll(/\{(\d*)[^}]*\}/g)].map((m) => m[1]).sort().join(",");
	const quoted = (line) => line.match(/"([^"]*)"/)?.[1] ?? null;
	const pairs = new Map();
	for (const name of CSD_FILES) {
		let text = fs.readFileSync(path.join(PLAIN_DIR, name)).toString("utf16le");
		if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
		for (const block of text.split(/\r?\ndescription/)) {
			const [enPart, koPart] = block.split(/\r?\n\s*lang "Korean"/);
			if (!koPart) continue;
			const en = enPart.split(/\r?\n/).map(quoted).filter((s) => s != null);
			const ko = koPart.split(/\r?\n/).map(quoted).filter((s) => s != null);
			en.forEach((enText, k) => {
				const koText = ko[k];
				if (!koText || !enText.includes("\\n")) return;
				const enSegs = enText.split("\\n");
				const koSegs = koText.split("\\n");
				if (enSegs.length !== koSegs.length) return;
				enSegs.forEach((seg, j) => {
					if (!/[a-z]/.test(seg) || placeholders(seg) !== placeholders(koSegs[j])) return;
					if (!pairs.has(seg)) pairs.set(seg, koSegs[j]);
				});
			});
		}
	}
	const out = [];
	let n = 0;
	for (const [en, ko] of pairs) {
		out.push("description", `\t1 poe2_unique_segment_${n++}`, "\t1", `\t\t# "${en}"`, '\tlang "Korean"', "\t1", `\t\t# "${ko}"`, "");
	}
	fs.writeFileSync(path.join(PLAIN_DIR, SEGMENT_FILE), Buffer.from("﻿" + out.join("\r\n"), "utf16le"));
}

// PoE1 번역기 재사용 — 기본 PoE1 파일명(.txt)은 이 폴더에 없어 건너뛰고, 아래 순서(범용 → 전용 → 조각)로 읽는다.
const toKoRaw = createModTranslator(PLAIN_DIR, [...CSD_FILES, SEGMENT_FILE]);

// ─── 게임 테이블 사전 ───────────────────────────────────────────────────────────
const wordsEn = loadTable("English", "Words");
const wordsKo = loadTable("Korean", "Words");
const UNIQUE_WORDLIST = 6; // Words.Wordlist 6 = 고유 아이템 이름 (Headhunter/Astramentis 등으로 확인)
const nameKoByEn = new Map();
// 고유 이름 목록을 먼저, 다른 목록은 빈자리만 채운다(같은 낱말이 접두/접미 목록에도 있을 수 있어서)
for (const pass of [true, false]) {
	wordsEn.forEach((w, i) => {
		if ((w.Wordlist === UNIQUE_WORDLIST) !== pass) return;
		const ko = wordsKo[i]?.Text2 || null;
		for (const key of [w.Text, w.Text2]) {
			if (key && ko && ko !== key && !nameKoByEn.has(key)) nameKoByEn.set(key, ko);
		}
	});
}

const classesEn = loadTable("English", "ItemClasses");
const classesKo = loadTable("Korean", "ItemClasses");
const baseEn = loadTable("English", "BaseItemTypes");
const baseKo = loadTable("Korean", "BaseItemTypes");
// 같은 이름의 베이스가 여러 행일 수 있다 — 분류가 있고 Demo/Test 가 아닌 첫 행을 쓴다
const baseByEn = new Map();
baseEn.forEach((b, i) => {
	if (!b.Name) return;
	const cls = classesEn[b.ItemClass];
	const candidate = {
		nameKo: baseKo[i]?.Name || null,
		itemClass: cls?.Id || null,
		itemClassKo: classesKo[b.ItemClass]?.Name || null,
		junk: !cls || /demo|test|DONOTUSE/i.test(b.Id + " " + (cls?.Id || "")),
	};
	const prev = baseByEn.get(b.Name);
	if (!prev || (prev.junk && !candidate.junk)) baseByEn.set(b.Name, candidate);
});
const classKoById = new Map(classesEn.map((c, i) => [c.Id, classesKo[i]?.Name || null]));

// 부여 스킬 이름 (ActiveSkills.DisplayedName) — "Grants Skill: …" 라인과 변형 라벨에 쓴다
const skillsEn = loadTable("English", "ActiveSkills");
const skillsKo = loadTable("Korean", "ActiveSkills");
const skillKoByEn = new Map();
skillsEn.forEach((s, i) => {
	const ko = skillsKo[i]?.DisplayedName;
	if (s.DisplayedName && ko && ko !== s.DisplayedName && !skillKoByEn.has(s.DisplayedName)) skillKoByEn.set(s.DisplayedName, ko);
});
// "Grants Skill: Level {1} {0}" 한국어 틀은 ClientStrings 에 있다("스킬 부여: {1}레벨 {0}")
const clientTemplate = (id, fallback) => {
	const en = loadTable("English", "ClientStrings");
	const ko = loadTable("Korean", "ClientStrings");
	const i = en.findIndex((r) => r.Id === id);
	const text = i >= 0 ? ko[i]?.Text : null;
	if (!text) return fallback;
	return text.replace(/<[a-z]+>/g, "").replace(/\{\{(.*)\}\}/, "$1");
};
const GRANTED_SKILL_KO = clientTemplate("ItemDisplayGrantedSkill", "스킬 부여: {1}레벨 {0}");
const GRANTED_SKILL_NO_LEVEL_KO = clientTemplate("ItemDisplayGrantedSkillNoScaling", "스킬 부여: {0}");

function grantsSkillKo(line) {
	const m = line.match(/^Grants Skill: (?:Level (\S+) )?(.+)$/);
	if (!m) return null;
	const skill = skillKoByEn.get(m[2]);
	if (!skill) return null;
	return m[1]
		? GRANTED_SKILL_KO.replace("{0}", skill).replace("{1}", m[1])
		: GRANTED_SKILL_NO_LEVEL_KO.replace("{0}", skill);
}

// 스탯 설명 번역기로 못 옮기는 줄 — 각각 게임 데이터에서 한국어를 가져온다.
//   - "Legacy of Amethyst": 템플릿 "legacy of {0}" 의 {0} 은 숫자가 아니라 mages_legacy_index 핸들러가 이름으로 바꾼다.
//     이름은 호신부 베이스와 같다("Amethyst Charm" = "자수정 호신부") → 베이스 한국어에서 분류명 꼬리를 뗀다.
//   - "Mirrored"/"Corrupted": 상태 표시(ClientStrings ItemPopup*)
//   - "Has N Augment Sockets": 게임은 홈을 그림으로 보여 줘 스탯 설명이 없다 → 스탯 "홈 {0}개" 틀 + ClientStrings 의 [Augment|증강물]
const mageLegacyKo = (() => {
	let text = fs.readFileSync(path.join(PLAIN_DIR, CSD_FILES[0])).toString("utf16le");
	const m = text.match(/"legacy of \{0\}"[^\r\n]*\r?\n\s*lang "Korean"\r?\n\s*\d+\r?\n\s*\S+\s+"([^"]+)"/);
	text = null;
	return m ? m[1] : "{0}의 유산";
})();
const charmTailKo = classKoById.get("UtilityFlask") || "호신부";
const popupKo = (id, fallback) => stripMarkup(clientTemplate(id, fallback)).trim();
const STATUS_KO = { Mirrored: popupKo("ItemPopupMirrored", "복제"), Corrupted: popupKo("ItemPopupCorrupted", "타락") };
const AUGMENT_KO = (() => {
	const en = loadTable("English", "ClientStrings");
	const ko = loadTable("Korean", "ClientStrings");
	const i = en.findIndex((r) => r.Id === "GambleTabNameAugments");
	return (i >= 0 && ko[i]?.Text) || "증강물";
})();
function specialKo(line) {
	const granted = grantsSkillKo(line);
	if (granted) return granted;
	if (STATUS_KO[line]) return STATUS_KO[line];
	const legacy = line.match(/^Legacy of (.+)$/);
	if (legacy) {
		// 호신부 베이스 → 낱말 사전(Words) → 같은 이름 베이스(Diamond 주얼) 순. 셋 다 없으면(Basalt 등 PoE1 플라스크 이름) 영어 유지
		const charm = baseByEn.get(`${legacy[1]} Charm`)?.nameKo;
		const word = (charm && charm.endsWith(" " + charmTailKo) ? charm.slice(0, -charmTailKo.length - 1) : null)
			|| nameKoByEn.get(legacy[1]) || baseByEn.get(legacy[1])?.nameKo || null;
		return word ? mageLegacyKo.replace(/\{0[^}]*\}/, word) : null;
	}
	const sockets = line.match(/^Has (\S+) Augment Sockets?$/);
	if (sockets) return `${AUGMENT_KO} 홈 ${sockets[1]}개`;
	return null;
}

// ─── 영어 라인 배열 → 한국어 배열 ─────────────────────────────────────────────────
const HANGUL = /[가-힣]/;
// 번역 여부 판정 — 한글이 한 글자도 없으면 옮기지 못한 줄이다(영문 폴백).
export const isUntranslated = (ko) => !HANGUL.test(ko);

/**
 * 번역기 앞뒤 손질:
 *  - 소문자화(사본 영어 템플릿이 소문자)
 *  - "-(8-5)%" 처럼 부호가 괄호 밖인 범위는 번역기 토큰 정규식이 부호를 못 먹어 스켈레톤이 어긋난다 →
 *    "-987650" 같은 표지 숫자로 바꿔 번역한 뒤 원문으로 되돌린다.
 *  - "Grants Skill:" 은 스탯 설명이 아니라 ClientStrings 틀로 옮긴다.
 * 결과 길이는 입력과 다를 수 있다(PoB 가 쪼갠 멀티라인 모드는 한 줄로 합쳐진다). 실패 줄은 영어 원문 유지.
 */
function toKo(lines) {
	const sentinels = new Map();
	let seq = 0;
	const originalByPrepared = new Map();
	const prepared = lines.map((line) => {
		const text = line
			.replace(/-\((\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)\)/g, (m) => {
				const key = String(-(987650 + seq++));
				sentinels.set(key, m);
				return key;
			})
			.toLowerCase();
		originalByPrepared.set(text, line);
		return text;
	});
	const restore = (text) => text.replace(/-98765\d+/g, (m) => sentinels.get(m) ?? m);

	const out = [];
	// 특수 줄(Grants Skill 등)은 따로 떼어 번역기에 넣지 않는다(합쳐 번역하는 창에 섞이지 않게)
	let chunk = [];
	const flush = () => {
		if (!chunk.length) return;
		for (const ko of toKoRaw(chunk)) {
			out.push(originalByPrepared.has(ko) ? originalByPrepared.get(ko) : restore(ko));
		}
		chunk = [];
	};
	lines.forEach((line, i) => {
		const special = specialKo(line);
		if (special) {
			flush();
			out.push(special);
		} else chunk.push(prepared[i]);
	});
	flush();
	return out;
}

// ─── 변형 라벨 한글화 ────────────────────────────────────────────────────────────
// PoB 라벨은 영어 자유 문구라 완역이 불가능하다 — **확실한 것만** 옮기고 못 옮기면 null(화면이 영어로 폴백).
//   확실한 것 = 이 아이템의 모드 줄과 같은 문구(그 줄의 번역), 스킬/베이스/아이템 분류 이름, 아래 사전 낱말.
const VARIANT_WORD_KO = {
	"Physical": "물리", "Fire": "화염", "Cold": "냉기", "Lightning": "번개", "Chaos": "카오스",
	"Elemental": "원소", "Attributes": "능력치", "Strength": "힘", "Dexterity": "민첩", "Intelligence": "지능",
	"Life": "생명력", "Mana": "마나", "Spirit": "정신력", "Energy Shield": "에너지 보호막", "Armour": "방어도",
	"Evasion": "회피", "Evasion Rating": "회피", "Item Rarity": "아이템 희귀도", "Movement Speed": "이동 속도",
	"Stun Threshold": "기절 한계치", "Life Regeneration": "생명력 재생",
	"All Resistances": "모든 저항", "Chaos Resistance": "카오스 저항",
	"Helmet": classKoById.get("Helmet"), "Gloves": classKoById.get("Gloves"), "Boots": classKoById.get("Boots"),
	"Shield": classKoById.get("Shield"), "Body Armour": classKoById.get("Body Armour"),
};
function variantNameKo(label, lineKo) {
	const direct = lineKo.get(label.toLowerCase()) || VARIANT_WORD_KO[label] || skillKoByEn.get(label)
		|| baseByEn.get(label)?.nameKo || nameKoByEn.get(label);
	if (direct) return direct;
	const paren = label.match(/^(.*?)\s*\((.+)\)$/);
	if (paren) {
		const head = variantNameKo(paren[1], lineKo);
		const tail = variantNameKo(paren[2], lineKo);
		return head && tail ? `${head} (${tail})` : null;
	}
	for (const [sep, join] of [[", ", ", "], ["/", "/"], [" and ", " · "]]) {
		if (label.includes(sep)) {
			const parts = label.split(sep).map((p) => variantNameKo(p.trim(), lineKo));
			return parts.every(Boolean) ? parts.join(join) : null;
		}
	}
	return null;
}

// 과거 버전 변형(Pre 0.4.0 등)은 지금 게임에 없어 목록에서 감춘다(PoE1 과 같은 규칙)
const HISTORICAL_VARIANT = /\bPre[ -]\d/i;
const isHistoricalVariant = (name) => name === "Current" || HISTORICAL_VARIANT.test(name);
const normalizeVariantName = (name) =>
	name
		.replace(/^Current\s*\((.+)\)$/i, "$1")
		.replace(/^Current\s*[-–]\s*/i, "")
		.replace(/\s*\(Current\)\s*$/i, "")
		.replace(/\s+Current$/i, "")
		.trim();

// 블록 메타데이터로 취급하는 접두사 (모드 라인이 아님).
//   PoE1 과 달리 "Grants Skill:" 은 **모드 줄**이다 — PoE2 무기/방패의 베이스 스킬이 Implicits 개수에 포함된다.
const META_PREFIXES = [
	"Variant:", "Version:", "League:", "Source:", "Upgrade:", "Selected Variant:", "Selected Version:",
	"Selected Variant Group:", "Selected Alt Variant", "Has Alt Variant", "Allow Duplicate Variants:",
	"Base Variant:", "Selected Base Variant:", "Talisman Tier:", "Requires ", "LevelReq:", "Implicits:",
	"Sockets:", "Limited to:", "Radius:", "Crafted:", "Item Level:", "Quality:",
];
const tagsOf = (raw) => ({
	variants: raw.match(/\{variant:([^}]*)\}/) ? parseIdSpec(raw.match(/\{variant:([^}]*)\}/)[1]) : null,
	versions: raw.match(/\{version:([^}]*)\}/) ? parseIdSpec(raw.match(/\{version:([^}]*)\}/)[1]) : null,
	groups: raw.match(/\{group:([^}]*)\}/) ? parseIdSpec(raw.match(/\{group:([^}]*)\}/)[1]) : null,
	text: stripMarkup(raw.replace(/\{[^}]*\}/g, "")).replace(/\s+/g, " ").trim(),
});
// 베이스 줄 판정 — PoB 도 "알려진 베이스 이름과 같은 줄"을 베이스로 본다. 변형별 베이스({variant:1}Plated Mace)나
//   Variant/Source 뒤에 오는 베이스도 있어 둘째 줄 고정으로는 못 잡는다.
const isBaseLine = (raw) => {
	const base = baseByEn.get(tagsOf(raw).text);
	return !!base && !base.junk;
};

const parseIdSpec = (spec) => new Set((spec || "").split(",").map((s) => Number(s.trim())).filter((n) => Number.isFinite(n)));

// PoB Item.lua 의 CheckModLineVariant 규칙을 그대로 옮긴다:
//   - {version:..}: 선택 버전(기본 = 마지막 = Current)에 없으면 제외
//   - {group:..} 이 있는 아이템: 줄의 그룹 중 하나라도 그 그룹의 선택 변형이 줄 variant 목록에 있으면 포함,
//     그룹 없는 variant 줄은 제외
//   - 그 외: variant 없음 → 포함, 있으면 활성 변형(주 변형 + Alt 변형들) 중 하나와 겹치면 포함
function parseBlock(block, category) {
	const lines = block.split("\n").map((l) => l.trim()).filter((l) => l.length && !l.startsWith("--"));
	if (lines.length < 2) return null;
	const name = lines[0];
	const baseLines = [];

	const variantNames = [];
	const versionNames = [];
	let selectedVariant = null;
	let selectedVersion = null;
	const altSelected = []; // Selected Alt Variant / Two / Three …
	let hasAlt = false;
	const groupSelectionSpec = new Map();
	let requiredLevel = null;
	let league = null;
	let source = null;
	let implicitCount = 0;
	const modSection = [];

	lines.slice(1).forEach((line, i) => {
		// 둘째 줄은 항상 베이스(PoB 규칙) — 매칭 실패해도 이름은 남긴다. 그 뒤는 알려진 베이스 이름일 때만.
		if (i === 0 && !line.includes(":") && !/^\{/.test(line)) { baseLines.push(tagsOf(line)); return; }
		if (isBaseLine(line)) { baseLines.push(tagsOf(line)); return; }
		parseMeta(line);
	});
	function parseMeta(line) {
		if (line.startsWith("Variant:")) { variantNames.push(line.slice(8).trim()); return; }
		if (line.startsWith("Version:")) { versionNames.push(line.slice(8).trim()); return; }
		if (line.startsWith("Selected Variant:")) { selectedVariant = Number(line.slice(17).trim()) || null; return; }
		if (line.startsWith("Selected Version:")) { selectedVersion = Number(line.slice(17).trim()) || null; return; }
		if (line.startsWith("Selected Variant Group:")) {
			const m = line.slice(23).match(/^\s*(\d+)\s*=\s*(\d+)/);
			if (m) groupSelectionSpec.set(Number(m[1]), Number(m[2]));
			return;
		}
		if (line.startsWith("Selected Alt Variant")) {
			const v = Number(line.split(":")[1]);
			if (v) altSelected.push(v);
			return;
		}
		if (line.startsWith("Has Alt Variant")) { hasAlt = true; return; }
		if (line.startsWith("League:")) { league = line.slice(7).trim(); return; }
		if (line.startsWith("Source:")) { source = line.slice(7).trim(); return; }
		if (line.startsWith("LevelReq:")) { requiredLevel = Number(line.slice(9).trim()) || null; return; }
		if (line.startsWith("Requires Level")) {
			const m = line.match(/Requires Level (\d+)/);
			if (m) requiredLevel = Number(m[1]);
			return;
		}
		if (line.startsWith("Implicits:")) { implicitCount = Number(line.slice(10).trim()) || 0; modSection.length = 0; return; }
		if (META_PREFIXES.some((p) => line.startsWith(p))) return;
		modSection.push(line);
	}

	// 모드 줄 → { raw, text, variants, versions, groups }
	const parsed = modSection.map(tagsOf).filter((l) => l.text.length);
	// implicitCount 는 원문 줄 기준이라 필터 전에 가른다 (빈 줄은 위에서 이미 빠졌다)
	parsed.forEach((l, i) => { l.implicit = i < implicitCount; });

	const variantCount = variantNames.length;
	const version = versionNames.length ? Math.max(1, Math.min(versionNames.length, selectedVersion || versionNames.length)) : null;
	const versionOk = (l) => !l.versions || (version != null && l.versions.has(version));

	// 변형 그룹: 그룹 → 변형 → 가능한 버전(0 = 전 버전)
	const groups = new Map();
	for (const l of parsed) {
		if (!l.groups || !l.variants) continue;
		for (const g of l.groups) {
			if (!groups.has(g)) groups.set(g, new Map());
			for (const v of l.variants) {
				if (v < 1 || v > variantCount) continue;
				const versions = groups.get(g).get(v) || new Set();
				if (l.versions) for (const ver of l.versions) versions.add(ver);
				else versions.add(0);
				groups.get(g).set(v, versions);
			}
		}
	}
	const grouped = groups.size > 0;
	const eligible = (g, v) => {
		const versions = groups.get(g)?.get(v);
		return !!versions && (versions.has(0) || (version != null && versions.has(version)));
	};
	// PoB NormaliseVariantSelections: 지정 선택이 유효하면 쓰고, 아니면 그룹별 첫 가능 변형(다른 그룹과 중복 없이)
	const defaultGroupSel = new Map();
	if (grouped) {
		const used = new Set();
		const pending = [];
		for (const g of [...groups.keys()].sort((a, b) => a - b)) {
			const sel = groupSelectionSpec.get(g);
			if (sel && eligible(g, sel) && !used.has(sel)) { defaultGroupSel.set(g, sel); used.add(sel); } else pending.push(g);
		}
		for (const g of pending) {
			for (let v = 1; v <= variantCount; v++) {
				if (eligible(g, v) && !used.has(v)) { defaultGroupSel.set(g, v); used.add(v); break; }
			}
		}
	}

	const includeLine = (l, active, groupSel) => {
		if (!versionOk(l)) return false;
		if (grouped) {
			if (l.groups) {
				if (!l.variants) return false;
				for (const g of l.groups) if (groupSel.has(g) && l.variants.has(groupSel.get(g))) return true;
				return false;
			}
			return !l.variants;
		}
		if (!l.variants) return true;
		for (const v of active) if (l.variants.has(v)) return true;
		return false;
	};
	const linesFor = (active, groupSel) => {
		const picked = parsed.filter((l) => includeLine(l, active, groupSel));
		// 변형별 베이스: 선택에 맞는 첫 베이스 줄 (없으면 첫 줄)
		const baseLine = baseLines.find((l) => includeLine(l, active, groupSel)) || baseLines[0];
		return {
			baseType: baseLine?.text || null,
			implicits: picked.filter((l) => l.implicit).map((l) => l.text),
			explicits: picked.filter((l) => !l.implicit).map((l) => l.text),
		};
	};

	// 기본 선택 (PoB 규칙: Selected Variant 우선, 없으면 마지막. Alt 도 지정값 없으면 마지막)
	let mainVariant = variantCount ? Math.max(1, Math.min(variantCount, selectedVariant || variantCount)) : null;
	const liveVariants = variantNames
		.map((vName, i) => ({ index: i + 1, name: normalizeVariantName(vName), historical: isHistoricalVariant(vName) }))
		.filter((v) => !v.historical);
	if (!grouped && !hasAlt && mainVariant != null && isHistoricalVariant(variantNames[mainVariant - 1]) && liveVariants.length) {
		mainVariant = liveVariants[liveVariants.length - 1].index;
	}
	const defaultActive = new Set(mainVariant ? [mainVariant] : []);
	if (hasAlt && !grouped) {
		const altList = altSelected.length ? altSelected : [variantCount];
		for (const v of altList) if (v >= 1 && v <= variantCount) defaultActive.add(v);
	}
	const { baseType, implicits, explicits } = linesFor(defaultActive, defaultGroupSel);

	// 변형 목록 — 현재 변형이 둘 이상일 때만.
	//   그룹 아이템: 기본 선택에서 그 변형의 그룹만 바꿔 끼운 완성품
	//   Alt 아이템(여러 칸을 따로 고르는 경우): 그 변형 하나만 켠 줄(공통 줄 + 그 변형 줄)
	//   일반: 그 변형 하나
	let variants = null;
	if (liveVariants.length > 1) {
		// 변형 라벨이 모드 줄과 같은 문구면 그 줄의 번역을 라벨로 (Mageblood "Legacy of Amethyst").
		//   줄 하나씩 옮긴다 — 묶어 옮기면 멀티라인 결합으로 인덱스가 어긋난다.
		const lineKo = new Map();
		const labels = new Set(variantNames.map((n) => normalizeVariantName(n).toLowerCase()));
		for (const l of parsed) {
			if (!labels.has(l.text.toLowerCase())) continue;
			const [ko] = toKo([l.text]);
			if (ko && !isUntranslated(ko)) lineKo.set(l.text.toLowerCase(), ko);
		}
		variants = liveVariants
			.filter((v) => !grouped || [...groups.keys()].some((g) => eligible(g, v.index)))
			.map((v) => {
				let set;
				if (grouped) {
					const sel = new Map(defaultGroupSel);
					for (const g of groups.keys()) if (eligible(g, v.index)) sel.set(g, v.index);
					set = linesFor(new Set(), sel);
				} else {
					set = linesFor(new Set([v.index]), defaultGroupSel);
				}
				const variant = {
					index: v.index,
					name: v.name,
					nameKo: variantNameKo(v.name, lineKo),
				};
				// 변형마다 베이스가 다르면(Seeing Stars: Plated → Marching Mace) 그 변형의 베이스를 싣는다
				if (set.baseType && set.baseType !== baseType) {
					const b = baseByEn.get(set.baseType);
					Object.assign(variant, { baseType: set.baseType, baseTypeKo: b?.nameKo || null, itemClass: b?.itemClass || null, itemClassKo: b?.itemClassKo || null });
				}
				return Object.assign(variant, {
					implicits: set.implicits,
					implicitsKo: toKo(set.implicits),
					explicits: set.explicits,
					explicitsKo: toKo(set.explicits),
				});
			});
		if (variants.length < 2) variants = null;
	}
	const defaultIndexes = grouped ? [...defaultGroupSel.entries()].sort((a, b) => a[0] - b[0]).map(([, v]) => v) : [...defaultActive];

	const base = baseByEn.get(baseType);
	const item = {
		name,
		nameKo: nameKoByEn.get(name) || null,
		slug: name.toLowerCase().normalize("NFKD").replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, ""),
		baseType,
		baseTypeKo: base?.nameKo || null,
		itemClass: base?.itemClass || null,
		itemClassKo: base?.itemClassKo || null,
		category,
		requiredLevel,
		implicits,
		implicitsKo: toKo(implicits),
		explicits,
		explicitsKo: toKo(explicits),
		variants,
		defaultVariant: variants ? (defaultIndexes[0] ?? null) : null,
		flavour: null, // PoB 에 없고, 게임 FlavourText 는 고유 이름과 잇는 키가 없어 비워 둔다
		flavourKo: null,
		league,
	};
	// 여러 칸을 동시에 고르는 아이템(그룹/Alt)은 기본 조합 전체를 남긴다
	if (variants && defaultIndexes.length > 1) item.defaultVariants = defaultIndexes;
	// 얻을 수 없는 고유 — PoB 가 명시한 경우만 (그 외 판정은 하지 않는다)
	if (source && /no longer obtainable/i.test(source)) item.legacy = true;
	return item;
}

// ─── 파싱 ─────────────────────────────────────────────────────────────────────
const items = [];
for (const category of POB_FILES) {
	const source = fs.readFileSync(path.join(POB_DIR, category + ".lua"), "utf8").replace(/\r\n/g, "\n");
	for (const match of source.matchAll(/\[\[([\s\S]*?)\]\]/g)) {
		const item = parseBlock(match[1], category);
		if (item) items.push(item);
	}
}
/**
 * PoB 생성 고유의 시험용 변형("Variant: Everything (QoL Test Variant)" — 모든 줄을 켠다)을 뺀다. 마지막 변형이라 기본으로 골려
 * 허무의 산물이 34줄을 한꺼번에 달고 나왔다. Variant: 줄을 지우고 {variant:…} 목록에서 그 번호를 빼고, 뒤 번호는 하나씩 당긴다.
 */
function dropTestVariants(raw) {
	const lines = raw.split("\n");
	const variantLines = lines.filter((l) => /^\s*Variant:/.test(l));
	const drop = new Set();
	variantLines.forEach((l, i) => { if (/test variant/i.test(l)) drop.add(i + 1); });
	if (!drop.size) return raw;
	const remap = (n) => n - [...drop].filter((d) => d < n).length;
	const out = [];
	let vi = 0;
	for (const l of lines) {
		if (/^\s*Variant:/.test(l)) {
			vi++;
			if (drop.has(vi)) continue;
			out.push(l);
			continue;
		}
		const m = l.match(/^(\s*)\{variant:([\d,]+)\}(.*)$/);
		if (m) {
			const kept = m[2].split(",").map(Number).filter((n) => !drop.has(n)).map(remap);
			if (!kept.length) continue; // 시험 변형에만 있던 줄
			out.push(`${m[1]}{variant:${kept.join(",")}}${m[3]}`);
			continue;
		}
		out.push(l);
	}
	return out.join("\n");
}
// PoB 엔진 고유 DB 덤프(tools/poe2-pob/dump-uniques2.lua → work/pob-unique-db.json)로 파일에서 못 읽은 고유를 보탠다.
//   Special/Generated.lua 는 Lua 코드로 원문을 만들어(로어위브·묠니르·메갈로매니악 등 8종) 위 [[…]] 정규식으로는 안 잡히고,
//   [[ ]] 가 아닌 문자열로 적힌 블록(쿨레막의 손아귀)도 빠졌다(09-30: PoB 443 vs 우리 435). 덤프가 없으면(엔진 없음) 건너뛴다.
{
	const dbPath = path.join(WORK_DIR, "pob-unique-db.json");
	if (fs.existsSync(dbPath)) {
		const have = new Set(items.map((i) => i.name.toLowerCase()));
		// 생성 고유는 파일 분류가 없다 — 베이스 아이템 클래스로 우리 분류를 고른다(이미 읽은 고유에서 클래스 → 분류 표)
		const catByClass = new Map();
		for (const i of items) if (i.itemClass && !catByClass.has(i.itemClass)) catByClass.set(i.itemClass, i.category);
		let added = 0;
		for (const { src, raw } of JSON.parse(fs.readFileSync(dbPath, "utf8"))) {
			const lines = String(raw).replace(/\r\n/g, "\n").split("\n").map((l) => l.trim()).filter(Boolean);
			if (!lines.length || have.has(lines[0].toLowerCase())) continue;
			let category = src;
			if (!POB_FILES.includes(category)) {
				const base = baseByEn.get(lines[1]);
				category = (base && catByClass.get(base.itemClass)) || src;
			}
			const item = parseBlock(dropTestVariants(String(raw).replace(/\r\n/g, "\n")), category);
			// 변형이 수백 개인 생성 고유(메갈로매니악 874 — 특화 조합마다 하나)는 변형 목록을 싣지 않는다(선택 상자가 쓸 수 없고 파일만 커진다)
			if (item && item.variants && item.variants.length > 60) {
				item.variantCount = item.variants.length;
				item.variants = null;
				item.defaultVariant = null;
				delete item.defaultVariants;
			}
			if (item) {
				item.pobGenerated = src === "generated" || undefined;
				items.push(item);
				have.add(item.name.toLowerCase());
				added++;
			}
		}
		console.log(`PoB 엔진 덤프로 보탠 고유 ${added}종`);
	}
}

// slug 중복 해소: 카테고리 접미 → 그래도 겹치면 번호
const seen = new Set();
for (const item of items) {
	let slug = item.slug || "unique";
	if (seen.has(slug)) slug = `${item.slug}-${item.category}`;
	for (let n = 2; seen.has(slug); n++) slug = `${item.slug}-${item.category}-${n}`;
	item.slug = slug;
	seen.add(slug);
}
items.sort((a, b) => a.name.localeCompare(b.name) || a.slug.localeCompare(b.slug));
const out = writeJson("uniques.json", { patch: PATCH, items });

// ─── 보고 ─────────────────────────────────────────────────────────────────────
// 번역 커버리지: Ko 배열의 각 줄 중 한글이 있는 줄 비율 (기본 줄 + 변형 줄 모두)
function coverage(list) {
	let total = 0;
	let translated = 0;
	const misses = new Map();
	const count = (ko) => {
		for (const line of ko) {
			total++;
			if (!isUntranslated(line)) translated++;
			else {
				const shape = line.replace(/[+-]?\(?-?\d[\d.\-]*\)?/g, "#");
				misses.set(shape, (misses.get(shape) || 0) + 1);
			}
		}
	};
	for (const item of list) {
		count(item.implicitsKo);
		count(item.explicitsKo);
		for (const v of item.variants || []) { count(v.implicitsKo); count(v.explicitsKo); }
	}
	return { total, translated, misses };
}

// 자가검사: 번역될 수 없는 가짜 줄을 심어 카운터가 잡는지, 진짜 줄은 통과하는지 확인
{
	const fake = toKo(["Zorblax gains (3-5) wibbles per Glorp"]);
	const real = toKo(["+(10-20)% to all Elemental Resistances"]);
	const probe = coverage([{ implicitsKo: fake, explicitsKo: real, variants: null }]);
	console.log(`자가검사: 가짜 줄 → ${JSON.stringify(fake)} / 진짜 줄 → ${JSON.stringify(real)} → 번역 ${probe.translated}/${probe.total} (기대 1/2)`);
	if (probe.total !== 2 || probe.translated !== 1) throw new Error("커버리지 카운터 자가검사 실패");
}

const byCategory = {};
for (const item of items) byCategory[item.category] = (byCategory[item.category] || 0) + 1;
const cov = coverage(items);
const nameKoCount = items.filter((i) => i.nameKo).length;
const baseKoCount = items.filter((i) => i.baseTypeKo).length;
console.log(`${items.length} uniques → ${out}`);
console.log(`  변형 있음 ${items.filter((i) => i.variants).length}, legacy ${items.filter((i) => i.legacy).length}`);
console.log(`  카테고리별 ${JSON.stringify(byCategory)}`);
console.log(`  한국어 이름 ${nameKoCount}/${items.length} (${(nameKoCount / items.length * 100).toFixed(1)}%), 베이스 ${baseKoCount}/${items.length}, 분류 ${items.filter((i) => i.itemClass).length}/${items.length}`);
console.log(`  한국어 줄 ${cov.translated}/${cov.total} (${(cov.translated / cov.total * 100).toFixed(1)}%)`);
if (process.argv.includes("--misses")) {
	for (const [shape, n] of [...cov.misses].sort((a, b) => b[1] - a[1]).slice(0, Number(process.env.MISSES || 20))) console.log(`   ${n}\t${shape}`);
}
const noName = items.filter((i) => !i.nameKo).map((i) => i.name);
if (noName.length) console.log(`  한국어 이름 없음: ${noName.join(", ")}`);
const noBase = [...new Set(items.filter((i) => !i.baseTypeKo || !i.itemClass).map((i) => i.baseType))];
if (noBase.length) console.log(`  베이스 매칭 실패: ${noBase.join(", ")}`);
