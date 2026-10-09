// PoB(Path of Building, MIT) 고유 아이템 데이터 → 표시용 JSON.
// 영어 모드 텍스트는 PoB 원문을 쓰고, 아이템 이름/베이스의 한국어는 게임 데이터(Words.Text2, BaseItemTypes)로 결합한다.
// 사용법: node parse-uniques.mjs  (사전 조건: pob-uniques/*.lua 다운로드, tables/ 추출 완료)
import fs from "node:fs";
import { execFileSync } from "node:child_process";
import os from "node:os";
import path from "node:path";
import { DATA_DIR, FILES_DIR, POB_DIR, findLuaJit, loadConfig, loadTable } from "./paths.mjs";
import { createModTranslator, createReminderIndex } from "./statDescriptions.mjs";

// 영어 모드 배열 → 한국어 배열 역번역 (멀티라인 모드 결합, 매칭 실패 라인은 영어 유지)
const toKoRaw = createModTranslator(FILES_DIR, [
	"metadata@statdescriptions@passive_skill_stat_descriptions.txt",
], { keepLineBreaks: true });

// 번역기 앞뒤 손질(10-02, PoE2 parse-uniques2 toKo 와 같은 방식):
//  - "-(30-20)%" 처럼 부호가 괄호 밖인 음수 범위는 번역기 숫자 토큰이 부호를 못 먹어 틀이 어긋난다 → 음수 숫자 하나로 잠시 바꿨다가 되돌린다
//    (실측: "-(30-20)% to all Elemental Resistances" · "-(30-60) Physical Damage taken from Attack Hits" 류 30여 줄이 영어 그대로였다)
//  - 특수 줄: Corrupted / Mirrored(게임 팝업 문구 — PoE1 추출엔 ClientStrings 가 없어 PoE2 의 ItemPopupCorrupted · ItemPopupMirrored 한국어와 같은 말),
//    줄 전체가 베이스 이름(변형이 바꾸는 베이스 "Leviathan Greaves" 등)
const STATUS_KO = { Corrupted: "타락", Mirrored: "복제" };
// 시간의 주얼 둘째 줄 — 게임에선 여러 줄 설명(local_unique_jewel_alternate_tree_*)이라 번역기가 첫 줄과 합쳐 한 줄로 옮긴다.
//   첫 줄이 못 맞으면(변형 문구 차이) 둘째 줄만 영어로 남는다 → 그때만 게임 원문(stat_descriptions.txt lang "Korean", 10-02 추출,
//   칼구르만 조사 "이")으로 바꾸고, 바로 앞 줄에 이미 합쳐져 있으면 중복이라 뺀다. 따로 떼어 번역하면 첫 줄 합치기가 깨진다(실측: 영어 89줄로 늘었다)
const CONQUERED_KO = {
	"Passives in radius are Conquered by the Vaal": "반경 내 패시브 스킬은 바알의 지배를 받음",
	"Passives in radius are Conquered by the Karui": "반경 내 패시브 스킬은 카루이의 지배를 받음",
	"Passives in radius are Conquered by the Maraketh": "반경 내 패시브 스킬은 마라케스의 지배를 받음",
	"Passives in radius are Conquered by the Templars": "반경 내 패시브 스킬은 템플러의 지배를 받음",
	"Passives in radius are Conquered by the Eternal Empire": "반경 내 패시브 스킬은 영원한 제국의 지배를 받음",
	"Passives in radius are Conquered by the Kalguur": "반경 내 패시브 스킬이 칼구르의 지배를 받음",
};
// 불가능한 탈출 줄 — 게임 설명(local_unique_jewel_disconnected_passives_can_be_allocated_around_keystone_hash)은 핵심 노드를 passive_hash
//   인자로 받아 번역기가 틀을 못 맞춘다(영어 그대로 49줄, 10-03 C76). 게임 한국어 원문 "반경 {} 내 패시브 스킬이 트리와 연결되지 않아도 할당 가능"에
//   핵심 노드 한국어 이름(PassiveSkills)을 넣는다. 원문의 줄바꿈 · "통로" 줄은 PoB 원문에 없는 부분이라 뺀다.
const KEYSTONE_LEAP_EN = /^Passive Skills in radius of (.+) can be allocated without being connected to your tree$/i;
// 같은 처지(이름을 인자로 받는 게임 설명이라 번역기가 못 맞춤)인 PoB 생성 고유 줄들(10-03 C78) — 전부 stat_descriptions.txt lang "Korean" 원문 틀:
//   금단 불꽃/살점 unique_jewel_grants_notable_hash_part_1/2 · 소아사의 진주 local_pearl_random_support_gem_1_* ·
//   모조 용송곳니의 비상 random_skill_gem_level_+_index · 금단의 샤코 "Socketed Gems are Supported by Level N X"(보조 젬마다 따로 있는 설명의 공통 꼴)
const PEARL_SLOT_KO = { Helmet: "투구", Gloves: "장갑", Boots: "장화" };
/** 보조 젬 표시 이름(한국어, "보조" 꼬리 없이) — 게임 문구 "{1} 보조 효과 적용" 의 {1} */
function supportKo(name) {
	const n = name.trim();
	const ko = gemKoByEn.get(n.endsWith(" Support") ? n : n + " Support") || gemKoByEn.get(n);
	return ko ? ko.replace(/\s*보조$/, "") : null;
}
const PARAM_LINE_KO = [
	[KEYSTONE_LEAP_EN, (m) => { const k = passiveKoByEn.get(m[1].trim()); return k && `반경 ${k} 내 패시브 스킬이 트리와 연결되지 않아도 할당 가능`; }],
	[/^Allocates (.+) if you have the matching modifier on Forbidden (Flesh|Flame)$/, (m) => { const k = passiveKoByEn.get(m[1].trim()); return k && `금단의 ${m[2] === "Flesh" ? "살점" : "화염"}에 일치하는 속성이 있을 경우 ${k} 할당`; }],
	[/^Skills Socketed in your (Helmet|Gloves|Boots) are Supported by level (\d+) (.+)$/, (m) => { const k = supportKo(m[3]); return k && `${PEARL_SLOT_KO[m[1]]}에 장착된 스킬에 ${m[2]}레벨 ${k} 보조 효과 적용`; }],
	[/^Skills granted by your Passive Tree are Supported by level (\d+) (.+)$/, (m) => { const k = supportKo(m[2]); return k && `패시브 트리로 부여된 스킬에 ${m[1]}레벨 ${k} 보조 효과 적용`; }],
	[/^\+(\d+) to Level of all (.+) Gems$/, (m) => { const k = gemKoByEn.get(m[2].trim()); return k && `모든 ${k} 젬 레벨 +${m[1]}`; }],
	[/^Socketed Gems are Supported by Level (\(\d+-\d+\)|\d+) (.+)$/, (m) => { const k = supportKo(m[2]); return k && `장착된 젬에 ${m[1]}레벨 ${k} 보조 효과 적용`; }],
	// PoB 원문이 게임 문구와 달라 번역기가 못 맞추는 줄 — 영어(엔진 입력)는 그대로 두고 한국어만 게임 원문(C78):
	//   선도자의 상징 gain_endurance_charge_per_second_if_have_been_hit_recently("Gain 1 …") · gain_maximum_endurance_charges_on_endurance_charge_gained_%_chance("up to maximum …")
	//   공포의 균형 action_speed_cannot_be_slowed_below_base_if_cast_temporal_chains_in_past_10_seconds("cannot be modified to below …")
	[/^Gain an Endurance Charge every second if you've been Hit Recently$/, () => "최근 4초 이내 피격된 경우 1초마다 인내 충전 1개 획득"],
	[/^(\(\d+-\d+\)|\d+)% chance that if you would gain Endurance Charges, you instead gain up to your maximum number of Endurance Charges$/, (m) => `인내 충전 획득 시 ${m[1]}%의 확률로 인내 충전 최대치 획득`],
	[/^Action Speed cannot be Slowed below Base Value if you've cast Temporal Chains in the past 10 seconds$/, () => "최근 10초 이내 시간의 사슬을 시전한 경우 동작 속도가 기본 수치 밑으로 내려가지 않음"],
];
function paramLineKo(line) {
	for (const [re, fn] of PARAM_LINE_KO) {
		const m = re.exec(line);
		if (m) return fn(m) || null;
	}
	return null;
}
function toKo(lines) {
	const sentinels = new Map();
	let seq = 0;
	const prepared = lines.map((line) =>
		line.replace(/-\((\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)\)/g, (m) => {
			const key = String(-(987650 + seq++));
			sentinels.set(key, m);
			return key;
		}),
	);
	const restore = (text) => text.replace(/-98765\d+/g, (m) => sentinels.get(m) ?? m);
	// 번역기는 여러 줄 옵션을 한 줄로 합칠 수 있어 줄 수가 달라질 수 있다 — 순번 대신 "번역 안 된 결과 = 손질한 입력"으로 원래 줄을 되찾는다
	const originalByPrepared = new Map(prepared.map((t, i) => [t, lines[i]]));
	const out = [];
	let chunk = [];
	const flush = () => {
		if (!chunk.length) return;
		for (const ko of toKoRaw(chunk)) out.push(originalByPrepared.has(ko) ? originalByPrepared.get(ko) : restore(ko));
		chunk = [];
	};
	lines.forEach((line, i) => {
		const special = STATUS_KO[line] || (baseKoByEnForMods.get(line) ?? null);
		if (special) {
			flush();
			out.push(special);
		} else chunk.push(prepared[i]);
	});
	flush();
	const fixed = [];
	for (const line of out) {
		// 핵심 노드 이름 한 줄(군주의 가죽 "Roiling Tempest" — 전직 핵심이라 게임 keystone_* 설명이 없다, C78)은 패시브 이름으로
		// 번역기가 못 옮긴(영어 그대로인) 줄에만 이름 인자 틀 · 패시브 이름을 쓴다 — 번역기가 게임의 전용 설명으로 옮긴 줄을 덮지 않게
		//   (실측 C78: 모조 칼리사의 품격 "…촉발 보조 젬 효과 적용"이 일반 틀 "…촉발 보조 효과 적용"으로 덮였다)
		const fallback = !/[가-힣]/.test(line) ? paramLineKo(line) || passiveKoByEn.get(line) : undefined;
		if (fallback) {
			fixed.push(fallback);
			continue;
		}
		const ko = CONQUERED_KO[line];
		if (!ko) fixed.push(line);
		else if (!(fixed.length && fixed[fixed.length - 1].includes(ko))) fixed.push(ko);
	}
	return fixed;
}

const PATCH = loadConfig().patch;
const OUT = path.join(DATA_DIR, "unique-items.json");

const load = loadTable;

// 한국어 이름 사전: Words(고유 이름) + BaseItemTypes(베이스 이름)
const wordsEn = load("English", "Words");
const wordsKo = load("Korean", "Words");
const nameKoByEn = new Map();
wordsEn.forEach((w, i) => {
	if (w.Text) nameKoByEn.set(w.Text, wordsKo[i]?.Text2 || null);
});
// KR 클라이언트 Words 테이블에 번역이 없는 레거시/제거 고유템의 한글명 폴백(영문명 기준, best-effort 커뮤니티 표준)
const nameKoOverrides = (() => {
	try {
		const raw = JSON.parse(fs.readFileSync(path.join(import.meta.dirname, "unique-nameko-overrides.json"), "utf8"));
		delete raw._comment;
		return raw;
	} catch {
		return {};
	}
})();
// PoB 원문이 롤 범위를 고정값으로 단순화한 라인 보정(빛나는 묘약 "(1-2)초" 등 — 거래소 가변 옵션 판정용)
const lineOverrides = (() => {
	try {
		const raw = JSON.parse(fs.readFileSync(path.join(import.meta.dirname, "unique-line-overrides.json"), "utf8"));
		delete raw._comment;
		return raw;
	} catch {
		return {};
	}
})();

// 현재 리그에서 얻을 수 있는지 — 게임 자신의 판정을 쓴다.
//   UniqueStashLayout 은 고유 수집 탭의 칸 정의인데, 칸이 비었을 때 그 칸을 보여줄지를
//   ShowIfEmptyChallengeLeague / ShowIfEmptyStandard 로 갈라 둔다. 지금 못 얻는 고유(레이스 보상 데미갓,
//   예언으로만 만들던 Fated, 삭제된 레거시 주얼 등)는 **둘 다 꺼져 있다**.
//   같은 이름이 대체 아트로 여러 행이면 한 행이라도 켜져 있으면 획득 가능으로 본다.
//   (실빌드 패싯은 아키타입당 상위 12개로 잘려 "안 쓴다"를 증명하지 못한다 — 그래서 게임 플래그를 쓴다.)
const legacyUniqueNames = (() => {
	const layout = load("English", "UniqueStashLayout");
	const hidden = new Set();
	const shown = new Set();
	for (const row of layout) {
		const word = wordsEn[row.WordsKey];
		const name = word && (word.Text2 || word.Text);
		if (!name) continue;
		(row.ShowIfEmptyChallengeLeague || row.ShowIfEmptyStandard ? shown : hidden).add(name);
	}
	for (const name of shown) hidden.delete(name);
	return hidden;
})();

const baseEn = load("English", "BaseItemTypes");
const baseKo = load("Korean", "BaseItemTypes");
const baseKoByEn = new Map();
// 줄 전체가 베이스 이름일 때(toKo 특수 줄) — baseKoByEn 과 같은 내용, toKo 가 위에 있어 따로 이름을 둔다
const baseKoByEnForMods = baseKoByEn;
baseEn.forEach((b, i) => {
	if (b.Name) baseKoByEn.set(b.Name, baseKo[i]?.Name || null);
});

// 블록 메타데이터로 취급하는 접두사 (모드 라인이 아님)
const META_PREFIXES = [
	"Variant:", "League:", "Source:", "Upgrade:", "Selected Variant:", "Selected Alt Variant",
	"Has Alt Variant", "Talisman Tier:", "Requires ", "LevelReq:", "Implicits:",
	"Elder Item", "Shaper Item", "Crusader Item", "Redeemer Item", "Hunter Item", "Warlord Item",
	"Sockets:", "Limited to:", "Radius:", "Grants Skill:", "Cluster Jewel",
	// PoB 생성 고유(금단 불꽃/살점)의 "{variant:N}Item Level: 83" · "{variant:N}Requires Class Duelist" — 아이템 정보 줄이지 옵션이 아니다(10-03 C78)
	"Item Level:",
];

// 변형 라벨 한글화 — PoB 라벨은 영어 자유 문구라 완역이 불가능하다. 그래서 **확실한 것만** 옮기고
//   못 옮기면 null 로 두어 화면이 영어 원문으로 폴백하게 한다(어설픈 반쪽 한글보다 낫다).
//   확실한 것 = 젬/베이스 이름 전체 일치, 또는 아래 사전에 있는 낱말. 구분자(":", "/", " and ")로 쪼개 각각 시도한다.
const gemKoByEn = (() => {
	const map = new Map();
	try {
		const gems = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "skill-gems.json"), "utf8")).gems || [];
		for (const gem of gems) if (gem.name && gem.nameKo) map.set(gem.name, gem.nameKo);
	} catch { /* 젬 산출물이 아직 없으면 사전 없이 진행 */ }
	return map;
})();
const gemKoByLower = new Map([...gemKoByEn].map(([en, ko]) => [en.toLowerCase(), ko]));
const VARIANT_WORD_KO = {
	"Physical": "물리", "Fire": "화염", "Cold": "냉기", "Lightning": "번개", "Chaos": "카오스",
	"Elemental": "원소", "Attributes": "속성", "Strength": "힘", "Dexterity": "민첩", "Intelligence": "지능",
	"Life": "생명력", "Mana": "마나", "Energy Shield": "에너지 보호막", "Armour": "방어도", "Evasion": "회피",
	"Evasion Rating": "회피", "Accuracy Rating": "정확도", "Attack Speed": "공격 속도", "Cast Speed": "시전 속도",
	"Damage": "피해", "Spell Damage": "주문 피해", "Attack Damage": "공격 피해", "Area of Effect": "효과 범위",
	"Item Rarity": "아이템 희귀도", "Item Quantity": "아이템 수량", "Movement Speed": "이동 속도",
	"Chaos Resistance": "카오스 저항", "Fire Resistance": "화염 저항", "Cold Resistance": "냉기 저항",
	"Lightning Resistance": "번개 저항", "Elemental Resistances": "원소 저항", "Max Resistance": "최대 저항",
	"Life Regen": "생명력 재생", "Mana Regen": "마나 재생", "Crit Chance": "치명타 확률",
	"Crit Multi": "치명타 피해", "Crit Multiplier": "치명타 피해", "Buff Effect": "버프 효과",
	"Aura Effect": "오라 효과", "Skill Reservation": "스킬 점유", "Global Crit Chance": "전역 치명타 확률",
	"Small Ring": "작은 고리", "Medium Ring": "중간 고리", "Large Ring": "큰 고리",
	"Very Large Ring": "매우 큰 고리", "Massive Ring": "거대한 고리",
	"Scorch": "이글거림", "Brittle": "취약", "Sap": "쇠약",
	"One Abyssal Socket": "심연 홈 1개", "Two Abyssal Sockets": "심연 홈 2개", "Three Abyssal Sockets": "심연 홈 3개",
	"Two Abyssal Socket": "심연 홈 2개", // PoB 라벨 단수 표기(무덤주먹) — 그 변형 줄 "Has 2 Abyssal Sockets" = "심연 홈 2개"(10-02 C27)
	// 무엇을 강화하는 변형인지 가리키는 낱말 (인게임 용어)
	"Spells": "주문", "Attacks": "공격", "Attack": "공격", "Spell": "주문",
	"Minions": "소환수", "Minion": "소환수", "Totem": "토템", "Brand": "낙인", "Trap": "덫", "Mine": "지뢰",
	"Conversion": "전환", "Penetration": "관통", "Proliferation": "확산", "Channelling": "집중",
	"Duration": "지속시간", "Effect Duration": "효과 지속시간", "Skill Effect Duration": "스킬 효과 지속시간",
	"Curse Effect": "저주 효과", "Additional Curse": "추가 저주", "Malediction": "저주 강화",
	"Blind": "실명", "Impale": "꿰뚫기", "Tailwind": "순풍", "Elusive": "교묘함", "Onslaught": "맹공",
	"Fortify": "축성", "Intimidate": "위협", "Rage": "분노", "Maim": "불구",
	"Freeze": "빙결", "Shock": "감전", "Ignite": "점화", "Ailments": "상태 이상",
	"Frenzy": "격노", "Power": "권능", "Endurance": "인내",
	"Frenzy Charge": "격노 충전", "Power Charge": "권능 충전", "Endurance Charge": "인내 충전",
	"Minimum Frenzy Charges": "최소 격노 충전", "Minimum Power Charges": "최소 권능 충전",
	"Minimum Endurance Charges": "최소 인내 충전", "Minimum Charges": "최소 충전",
	"Physical Damage Reduction": "물리 피해 감소", "Damage Reduction": "피해 감소",
	"Damage over Time": "지속 피해", "Damage over Time Multiplier": "지속 피해 증폭",
	"Area Damage": "지역 피해", "Global Physical Damage": "전역 물리 피해",
	"Mana Cost": "마나 소모", "Skill Cost": "스킬 소모", "Cooldown Recovery": "재사용 대기시간 회복",
	"Additional Projectile": "추가 발사체", "Extra Pierces": "추가 관통",
	"Maximum Life": "최대 생명력", "Energy Shield Regen": "에너지 보호막 재생",
	"ES": "에너지 보호막", "Gems": "젬", "Chance to Freeze": "빙결 확률",
	"Quantity": "수량", "Attributes": "속성", "Accuracy": "정확도",
	// 금단의 샤코 "Added Chaos Damage (Low Level)" — 보조 젬 레벨 1~10 / 11~20(C78)
	"Low Level": "낮은 레벨", "High Level": "높은 레벨",
	// "None"(공포의 균형 · 보라나의 행진 · 감시자의 눈 — 추가 옵션 없는 변형, C98)
	"None": "없음",
};
// 접미 합성 — "Fire Damage" 처럼 사전에 통짜로 없는 조합을 낱말+접미로 만들어 낸다.
//   양쪽 다 확실할 때만 합성하고, 하나라도 모르면 null(영문 폴백)을 유지한다.
const VARIANT_TAIL_KO = {
	"Damage": "피해", "Resistance": "저항", "Resistances": "저항", "Regen": "재생",
	"Effect": "효과", "Duration": "지속시간", "Speed": "속도", "Chance": "확률",
	"Multiplier": "증폭", "Rating": "수치", "Damage over Time": "지속 피해",
};
// 패시브 이름 영→한(게임 PassiveSkills) — 변형 라벨의 핵심 노드 이름("Asenath (Dance with Death)" 의 괄호 안 등, 10-02 PoE2 와 같은 방식)
const passiveKoByEn = new Map();
{
	const en = load("English", "PassiveSkills");
	const ko = load("Korean", "PassiveSkills");
	en.forEach((ps, i) => {
		const k = ko[i]?.Name;
		if (ps.Name && k && k !== ps.Name && !passiveKoByEn.has(ps.Name)) passiveKoByEn.set(ps.Name, k);
	});
}
// 시간의 주얼 핵심 노드 영→한(게임 AlternatePassiveSkills — "Dance with Death" → "죽음의 춤"). 정복된 작은 노드 이름도 섞여 있어
//   변형 라벨 괄호 안(인물의 핵심 노드)에만 쓴다(10-02 C18, 3.29.3.3 갱신 뒤 추출 — 옛 패치는 CDN 이 번들을 안 줬다).
const altPassiveKoByEn = new Map();
{
	const en = load("English", "AlternatePassiveSkills");
	const ko = load("Korean", "AlternatePassiveSkills");
	en.forEach((ps, i) => {
		const k = ko[i]?.Name;
		if (ps.Name && k && k !== ps.Name && !altPassiveKoByEn.has(ps.Name)) altPassiveKoByEn.set(ps.Name, k);
	});
}
function variantWordKo(word) {
	const trimmed = word.trim();
	if (!trimmed) return null;
	// 젬 이름은 대소문자만 달라도 찾는다("Purity Of Elements" → Purity of Elements, 숭고한 환영 C98)
	const direct = VARIANT_WORD_KO[trimmed] || gemKoByEn.get(trimmed) || gemKoByLower.get(trimmed.toLowerCase()) || baseKoByEn.get(trimmed) || passiveKoByEn.get(trimmed) || (gemKoByEn.has(trimmed + " Support") ? supportKo(trimmed) : null); // Words(nameKoByEn)는 이름 조각이라 "Ash" → "재의" 처럼 틀린다 — 쓰지 않는다(10-02 실측)
	if (direct) return direct;
	// "Life on Kill" → "처치 시 생명력" (인게임 어순은 조건이 앞)
	const onKill = trimmed.match(/^(.+?)\s+on Kill$/i);
	if (onKill) {
		const head = variantWordKo(onKill[1]);
		return head ? `처치 시 ${head}` : null;
	}
	// "Fire Damage" = "Fire" + "Damage"
	for (const [tail, tailKo] of Object.entries(VARIANT_TAIL_KO)) {
		if (!trimmed.toLowerCase().endsWith(` ${tail.toLowerCase()}`)) continue;
		const head = variantWordKo(trimmed.slice(0, trimmed.length - tail.length - 1));
		if (head) return `${head} ${tailKo}`;
	}
	return null;
}
// 변형 이름의 고유명사(시간의 주얼 인물 · 무기 종류)는 **그 변형 줄의 한국어**에서 떼어 온다(10-02 C17, PoE2 parse-uniques2 C16 과 같은 방식).
//   게임 문장 틀이 이름 자리를 정해 준다. 한국어는 여러 영어 줄이 한 줄로 합쳐지기도 해서(번역기 병합) 줄 번호 대신 한국어 전체에서 찾는다.
const NAME_SLOT_TEMPLATES = [
	[/in the akhara of (.+)$/, /([^\s]+)의 아카라에/], // 잔혹한 절제(마라케스)
	[/warriors under (.+)$/, /([^\s]+) 휘하 전사/], // 치명적인 긍지(카루이)
	[/to commemorate (.+)$/, /([^\s]+?)[을를] 기념하기/], // 우아한 오만(영원한 제국)
	[/in the name of (.+)$/, /([^\s]+)의 이름으로/], // 영광스러운 허영(바알)
	[/by High Templar (.+)$/, /고위 템플러 ([^\s]+?)[이가] 개종/], // 전투적 신앙(템플러)
	[/by the line of (.+)$/, /([^\s]+)의 핏줄/], // 영웅의 비극(칼구르)
	[/while wielding an? (.+)$/, /([^\s]+) 장착 시/], // 힘과 영향력(무기 종류)
	// 10-02 C36: 장미의 유산(쉐이퍼의 기억이 쓰는 스킬) · 이리엘의 양육(흉포한 소환수) · 목소리(소형 패시브 수 — 라벨은 줄 앞부분)
	[/Grants Level \d+ (.+?), which will be used by Shaper Memory$/, /사용하는 \d+레벨 (.+?) 사용 가능/],
	[/Summon Bestial (\S+) Skill$/, /흉포한 (\S+) 소환/],
	[/^Adds (\d+) Small Passive Skills? which grants? nothing$/, /(소형 패시브 스킬 \d+개 추가)/, (m) => `Adds ${m[1]} Small Passive Skill${m[1] === "1" ? "" : "s"}`],
];
function nameFromOwnLines(label, en, ko) {
	const koText = (ko || []).join(" ");
	for (const line of en || []) {
		for (const [reEn, reKo, labelOf] of NAME_SLOT_TEMPLATES) {
			const m = line.match(reEn);
			if (!m || (labelOf ? labelOf(m) : m[1]) !== label) continue;
			const k = koText.match(reKo);
			if (k && /[가-힣]/.test(k[1])) return k[1];
		}
	}
	return null;
}
/** 라벨 머리 낱말("Thunder")을 낱말로 품은 젬 이름이 그 변형 줄에 **딱 하나** 있으면 그 젬의 한국어("Herald of Thunder" → 천둥의 전령). */
function gemFromOwnLines(head, en) {
	const text = (en || []).join("\n");
	const hits = [...gemKoByEn.keys()].filter((g) => text.includes(g) && g.split(/\s+/).includes(head));
	return hits.length === 1 ? gemKoByEn.get(hits[0]) : null;
}
/** 변형 줄로 정하는 이름 — "Asenath (Dance with Death)" 는 인물(줄에서) + 괄호 안 핵심 노드(패시브 이름). 못 정하면 null. */
function variantNameFromLines(label, en, ko) {
	const own = nameFromOwnLines(label, en, ko);
	if (own) return own;
	// "Thunder: Skill Reservation"(야망의 고리) — 머리는 그 줄의 젬 이름, 꼬리는 낱말 사전(10-02 C27)
	const colon = label.match(/^([A-Za-z]+): (.+)$/);
	if (colon) {
		const head = gemFromOwnLines(colon[1], en);
		const tail = variantNameKo(null, colon[2]);
		if (head && tail) return `${head}: ${tail}`;
	}
	const paren = label.match(/^(.*?)\s*\((.+)\)$/);
	if (!paren) return null;
	const head = nameFromOwnLines(paren[1], en, ko);
	const tail = altPassiveKoByEn.get(paren[2]) || passiveKoByEn.get(paren[2]);
	return head && tail ? `${head} (${tail})` : null;
}
// PoB 생성 고유 변형 라벨 꼴(10-03 C78) — "(Duelist) Bane of Legends"(금단 불꽃/살점: 직업 + 노터블) · "Boots / Added Chaos Damage"(소아사의 진주: 부위 + 보조 젬)
const CLASS_KO = { Scion: "사이온", Marauder: "머라우더", Ranger: "레인저", Witch: "위치", Duelist: "듀얼리스트", Templar: "템플러", Shadow: "섀도우" }; // API PoeOptimizeService.CLASS_KO 와 같은 값
function generatedVariantNameKo(label) {
	const forbidden = label.match(/^\((\w+)\) (.+)$/);
	if (forbidden && CLASS_KO[forbidden[1]]) {
		const notable = passiveKoByEn.get(forbidden[2].trim());
		return notable ? `(${CLASS_KO[forbidden[1]]}) ${notable}` : null;
	}
	const pearl = label.match(/^(Helmet|Gloves|Boots|Passive Tree) \/ (.+)$/);
	if (pearl) {
		const gem = supportKo(pearl[2]);
		return gem ? `${pearl[1] === "Passive Tree" ? "패시브 트리" : PEARL_SLOT_KO[pearl[1]]} / ${gem}` : null;
	}
	return null;
}
/**
 * 마지막 대안 — 그 변형에만 있는 줄(모든 변형 공통 줄 · 타락 제외)이 1~2줄이고 전부 한국어로 옮겨졌으면 그 줄을 이름으로(C78).
 *   인게임엔 "변형 이름"이 없고 옵션 줄이 곧 아이템이라, PoB 라벨("Anger: Fire Damage Life Leech")을 어설프게 옮기는 것보다 정확하다.
 *   번역기가 줄을 합쳐 개수가 어긋나면 짝을 못 맞추니 포기(null → 영어 라벨).
 */
function distinctLinesKo(en, ko, common) {
	// 영어는 PoB 가 한 옵션을 두 줄로 나눴는데 한국어 게임 문구는 한 줄인 경우(속박된 운명 "…if" / "4 Warlord Items are Equipped", C99) —
	//   그 변형의 줄이 전부 변형만의 것이고 한국어가 한 줄이면 그 줄을 이름으로
	if (en?.length > 1 && ko?.length === 1 && en.every((l) => !common.has(l)) && /[가-힣]/.test(ko[0]) && !/[A-Za-z]{3,}/.test(ko[0])) return ko[0];
	if (!en?.length || !ko || ko.length !== en.length) return null;
	const picked = [];
	en.forEach((line, i) => {
		if (!common.has(line) && line !== "Corrupted") picked.push(ko[i]);
	});
	if (!picked.length || picked.length > 2 || picked.some((k) => !/[가-힣]/.test(k) || /[A-Za-z]{3,}/.test(k))) return null;
	return picked.join(" · ");
}
function variantNameKo(_itemName, label) {
	// 괄호 부기는 통째로 다시 태워 본다: "Two-Toned Boots (Armour/Evasion)"
	const paren = label.match(/^(.*?)\s*\((.+)\)$/);
	if (paren) {
		const head = variantNameKo(_itemName, paren[1]);
		const tail = variantNameKo(_itemName, paren[2]);
		return head && tail ? `${head} (${tail})` : null;
	}
	for (const [sep, join] of [[": ", ": "], [", ", ", "], [" + ", " + "], ["/", "/"], [" and ", " · "]]) {
		if (label.includes(sep)) {
			const parts = label.split(sep).map((p) => variantNameKo(_itemName, p));
			return parts.every(Boolean) ? parts.join(join) : null;
		}
	}
	return variantWordKo(label);
}

// 변형(Variant) 이름 중 **지금 게임에 없는 것**(과거 버전 보존용)을 걸러낸다.
//   PoB 는 옛 롤을 계속 들고 있어서(예: "Pre 3.21.0", "One Abyssal Socket (Pre 3.12.0)")
//   그대로 보여주면 인게임에 없는 선택지가 목록에 섞인다. 인게임 정합이 우선이라 과거분은 감춘다.
const HISTORICAL_VARIANT = /\bPre[ -]\d/i;
// PoB 가 시험용으로 넣은 변형("Everything (QoL Test Variant)" — 불가능한 탈출에 핵심 노드 49줄 전부)도 인게임에 없는 아이템이라 같은 취급(10-03 C76).
//   이게 기본 변형(마지막)으로 잡혀 목록 · 최적화기에 49줄짜리 가짜 주얼이 나갔다.
const TEST_VARIANT = /\bTest Variant\b/i;
const isHistoricalVariant = (name) => name === "Current" || HISTORICAL_VARIANT.test(name) || TEST_VARIANT.test(name);
// "Fire and Chaos Resistances (Current)" → "Fire and Chaos Resistances" (현재분 표식은 라벨에서 군더더기)
const normalizeVariantName = (name) =>
	name
		// "Current (Spells)" → "Spells", "Current - Crit Chance" → "Crit Chance", "Rhoa Current" → "Rhoa"
		.replace(/^Current\s*\((.+)\)$/i, "$1")
		.replace(/^Current\s*[-–]\s*/i, "")
		.replace(/\s*\(Current\)\s*$/i, "")
		.replace(/\s+Current$/i, "")
		// "Massive Ring (Uber)"(희망의 실) — PoB 꼬리표, 인게임엔 그냥 거대한 고리(C78)
		.replace(/\s*\(Uber\)\s*$/i, "")
		.trim();

function parseBlock(block, category) {
	// 게임 복사 형식으로 적힌 블록("Item Class: Jewels" / "Rarity: Unique" / 이름 / 베이스 — 상류 That Which Was Taken, 10-02)은 앞의 그 줄들을 뗀다
	const all = block.split("\n").map((l) => l.trim()).filter((l) => l.length);
	let head = 0;
	while (head < all.length && /^(Item Class|Rarity):/.test(all[head])) head++;
	const lines = all.slice(head);
	if (lines.length < 2) return null;
	const name = lines[0];
	// 베이스 줄 — 변형마다 베이스가 다르면 "{variant:1,2}Titan Greaves" 처럼 여러 줄이 온다(도리아니의 망상 등, 10-02).
	//   예전엔 둘째 줄만 베이스로 보고 셋째 줄("{variant:10…}Leviathan Greaves")을 옵션 줄로 넣었다 → 옵션 목록에 베이스 이름이 섞이고 baseType 에 마크업이 남았다.
	//   베이스보다 "Source:" · "League:" 가 먼저 오는 블록도 있다(고르곤의 응시 — 예전엔 baseType 이 "Source: …" 였다) → 그 줄은 건너뛰고 찾는다.
	let baseAt = 1;
	while (baseAt < lines.length && /^(Source|League):/.test(lines[baseAt])) baseAt++;
	const baseLines = [];
	let afterBase = baseAt;
	while (afterBase < lines.length) {
		const m = lines[afterBase].match(/^\{variant:([\d,]+)\}(.+)$/);
		if (!m) break;
		baseLines.push({ variants: new Set(m[1].split(",").map(Number)), base: m[2].trim() });
		afterBase++;
	}
	if (!baseLines.length) afterBase = baseAt + 1;
	// 베이스 줄만 빼고 나머지(앞쪽 Source/League 포함)는 아래 메타 · 모드 읽기로
	const bodyLines = lines.filter((_, i) => i > 0 && (i < baseAt || i >= afterBase));

	const variantNames = [];
	let selectedVariant = null;
	let requiredLevel = null;
	let league = null;
	let radius = null; // 반경 주얼("…in Radius")은 이 라벨이 없으면 PoB 가 반경 모드를 **조용히 무시**한다
	let implicitCount = 0;
	const modSection = []; // 메타 이후의 원시 라인들 (implicit 구분 전)

	for (const line of bodyLines) {
		if (line.startsWith("Variant:")) { variantNames.push(line.slice(8).trim()); continue; }
		if (line.startsWith("Selected Variant:")) { selectedVariant = Number(line.slice(17).trim()) || null; continue; }
		if (line.startsWith("League:")) { league = line.slice(7).trim(); continue; }
		if (line.startsWith("Radius:")) { radius = line.slice(7).trim(); continue; }
		if (line.startsWith("LevelReq:")) { requiredLevel = Number(line.slice(9).trim()) || null; continue; }
		if (line.startsWith("Requires Level")) {
			const m = line.match(/Requires Level (\d+)/);
			if (m) requiredLevel = Number(m[1]);
			continue;
		}
		if (line.startsWith("Implicits:")) { implicitCount = Number(line.slice(10).trim()) || 0; modSection.length = 0; continue; }
		// 메타 줄이 변형 표식 뒤에 숨어 있을 수 있다("{variant:3}Requires Class Witch" — 금단 불꽃/살점 166변형마다, C78)
		const bare = line.replace(/^(\{[^}]*\})+/, "");
		if (META_PREFIXES.some((p) => line.startsWith(p) || bare.startsWith(p))) continue;
		modSection.push(line);
	}

	const variantCount = variantNames.length;
	// 지금 게임에 존재하는 변형만 (임프레션스 물리/화염/…, 도리아니의 망상 9종 등)
	const liveVariants = variantNames
		.map((vName, i) => ({ index: i + 1, name: normalizeVariantName(vName) }))
		.filter((v, i) => !isHistoricalVariant(variantNames[i]));

	// 기본으로 보여줄 변형. PoB 규칙(Selected Variant 우선, 없으면 마지막)을 따르되,
	// 그 기본이 과거분이면 **현재분 중 마지막**으로 당긴다 — 아니면 인게임에 없는 롤이 대표로 나간다.
	let defaultVariant = selectedVariant && selectedVariant <= variantCount ? selectedVariant : (variantCount || null);
	if (defaultVariant != null && isHistoricalVariant(variantNames[defaultVariant - 1]) && liveVariants.length) {
		defaultVariant = liveVariants[liveVariants.length - 1].index;
	}

	// 대표 베이스 = 기본 변형이 속한 베이스 줄(없으면 마지막 줄 — PoB 는 새 베이스를 뒤에 둔다), 베이스 줄이 하나면 그대로
	const baseType = baseLines.length
		? (baseLines.find((b) => defaultVariant != null && b.variants.has(defaultVariant)) || baseLines[baseLines.length - 1]).base
		: lines[baseAt];

	// {variant:...} 필터 + {tags:...}/{range:...} 등 마크업 제거
	function cleanLine(raw, variant) {
		const variantMatch = raw.match(/\{variant:([\d,]+)\}/);
		if (variantMatch && variant != null) {
			const variants = variantMatch[1].split(",").map(Number);
			if (!variants.includes(variant)) return null;
		}
		const text = raw.replace(/\{[^}]*\}/g, "").trim();
		return text.length ? text : null;
	}

	// implicitCount 는 파일 원문 라인 기준이므로 필터 전에 자른다
	const implicitRaw = modSection.slice(0, implicitCount);
	const explicitRaw = modSection.slice(implicitCount);
	const overrides = lineOverrides[name] || {};
	// PoB 생성 고유(선도자의 상징) 원문의 관사 오타 "an Frenzy/Power Charge" — 게임 문구는 "a"(번역기도 그래야 맞춘다, C78)
	const applyOverride = (line) => (overrides[line] || line).replace(/\ban (Frenzy|Power) Charge/g, "a $1 Charge");
	const linesFor = (variant) => ({
		implicits: implicitRaw.map((l) => cleanLine(l, variant)).filter(Boolean).map(applyOverride),
		explicits: explicitRaw.map((l) => cleanLine(l, variant)).filter(Boolean).map(applyOverride),
	});

	const { implicits, explicits } = linesFor(defaultVariant);
	// 모든 변형에 똑같이 있는 줄 — 변형 이름을 그 변형만의 줄로 지을 때 뺀다(감시자의 눈 공통 3줄 등, C78)
	const commonLines = liveVariants.length > 1
		? liveVariants.map((v) => new Set(linesFor(v.index).explicits)).reduce((a, b) => new Set([...a].filter((x) => b.has(x))))
		: new Set();
	// 암시 쪽 공통 줄 — 변형만의 줄이 암시에 있는 고유(그레이트울프의 눈 · 예속의 끈, C98)
	const commonImplicits = liveVariants.length > 1
		? liveVariants.map((v) => new Set(linesFor(v.index).implicits)).reduce((a, b) => new Set([...a].filter((x) => b.has(x))))
		: new Set();

	// 변형이 여럿일 때만 전수 보존한다. 하나뿐이면(대개 "Pre x.y / Current") 기본분이 곧 전부라 잡음만 는다.
	const variants = liveVariants.length > 1
		? liveVariants.map((v) => {
			const set = linesFor(v.index);
			const implicitsKo = toKo(set.implicits);
			const explicitsKo = toKo(set.explicits);
			return {
				index: v.index,
				name: v.name,
				nameKo: variantNameFromLines(v.name, [...set.implicits, ...set.explicits], [...implicitsKo, ...explicitsKo])
					|| variantNameKo(name, v.name)
					|| generatedVariantNameKo(v.name)
					|| distinctLinesKo(set.explicits, explicitsKo, commonLines)
					|| distinctLinesKo(set.implicits, implicitsKo, commonImplicits),
				implicits: set.implicits,
				implicitsKo,
				explicits: set.explicits,
				explicitsKo,
			};
		})
		: null;

	return {
		variants,
		defaultVariant: variants ? defaultVariant : null,
		name,
		nameKo: nameKoByEn.get(name) || nameKoOverrides[name] || null,
		slug: name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, ""),
		baseType,
		// "Two-Toned Boots (Evasion/Energy Shield)" 처럼 괄호 부기가 붙은 베이스는 괄호를 떼고 한 번 더(게임 테이블엔 부기 없는 이름)
		baseTypeKo: baseKoByEn.get(baseType) || baseKoByEn.get(baseType.replace(/\s*\([^)]*\)$/, "")) || null,
		category,
		requiredLevel,
		league,
		legacy: legacyUniqueNames.has(name),
		radius,
		implicits,
		implicitsKo: toKo(implicits),
		explicits,
		explicitsKo: toKo(explicits),
	};
}

// PoB 유니크 원본 — **매 실행 최신본**을 쓴다.
//   예전엔 GitHub 에서 한 번 받아 pob-uniques/ 에 캐시하고 `existsSync` 면 건너뛰어서, 한 번 받은 뒤로는
//   신규·수정 유니크가 영원히 반영되지 않았다(실측: 캐시 jewel.lua 1,962줄 ↔ 최신 2,015줄).
//   run-all 은 이미 pob-src 를 git 으로 최신화하므로 **그 소스를 우선 복사**하고, 없을 때만 내려받는다.
const POB_FILES = ["amulet","axe","belt","body","boots","bow","claw","dagger","fishing","flask","gloves","helmet","jewel","mace","quiver","ring","shield","staff","sword","wand","tincture"];
fs.mkdirSync(POB_DIR, { recursive: true });
const POB_SRC_UNIQUES = path.join(DATA_DIR, "work", "pob-src", "src", "Data", "Uniques");
let fromSrc = 0;
let downloaded = 0;
for (const name of POB_FILES) {
	const target = path.join(POB_DIR, name + ".lua");
	const src = path.join(POB_SRC_UNIQUES, name + ".lua");
	if (fs.existsSync(src)) {
		const next = fs.readFileSync(src, "utf8");
		const prev = fs.existsSync(target) ? fs.readFileSync(target, "utf8") : null;
		if (next !== prev) {
			fs.writeFileSync(target, next);
			fromSrc++;
		}
		continue;
	}
	if (fs.existsSync(target)) continue;
	const url = `https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding/master/src/Data/Uniques/${name}.lua`;
	const response = await fetch(url);
	if (!response.ok) throw new Error(`PoB 다운로드 실패: ${url} (${response.status})`);
	fs.writeFileSync(target, await response.text());
	downloaded++;
}
if (fromSrc || downloaded) console.log(`PoB 유니크 원본 갱신: pob-src ${fromSrc}개, 다운로드 ${downloaded}개`);

const items = [];
for (const file of fs.readdirSync(POB_DIR)) {
	if (!file.endsWith(".lua")) continue;
	const category = file.replace(".lua", "");
	const source = fs.readFileSync(path.join(POB_DIR, file), "utf8");
	for (const match of source.matchAll(/\[\[([\s\S]*?)\]\]/g)) {
		const item = parseBlock(match[1], category);
		if (item) items.push(item);
	}
}

// PoB 가 실행 중에 만드는 고유(Data/Uniques/Special/Generated.lua — Impossible Escape · Watcher's Eye · Megalomaniac · Forbidden Flame/Flesh ·
//   Forbidden Shako · Skin of the Lords · Precursor's Emblem 등 15종, 10-03 C76). 정적 [[ ]] 블록이 없어 통째로 빠져 있었다 —
//   PoB 를 headless 로 띄워(tools/poe-pob/generated-uniques.lua) 원문을 받아 같은 parseBlock 으로 읽는다. pob-src · luajit 가 없으면 건너뛴다.
{
	const pobSrcDir = path.join(DATA_DIR, "work", "pob-src", "src");
	const luajit = findLuaJit();
	const script = path.join(import.meta.dirname, "..", "poe-pob", "generated-uniques.lua");
	if (luajit && fs.existsSync(path.join(pobSrcDir, "HeadlessWrapper.lua")) && fs.existsSync(script)) {
		try {
			const out = execFileSync(luajit, [script], { cwd: pobSrcDir, encoding: "utf8", maxBuffer: 256 * 1024 * 1024 });
			const at = out.indexOf("@@POB_RESULT@@");
			if (at < 0) throw new Error(out.slice(out.indexOf("@@POB_ERROR@@"), 300));
			const raws = JSON.parse(out.slice(at + "@@POB_RESULT@@".length));
			const have = new Set(items.map((i) => i.name));
			// 베이스 이름으로 분류(정적 파일은 파일 이름이 분류였다) — 생성 고유 15종의 베이스가 모두 걸린다
			const categoryOf = (base) =>
				/Jewel$/.test(base) ? "jewel" : /Ring$/.test(base) ? "ring" : /Amulet$/.test(base) ? "amulet"
				: /Crown|Helm|Hood|Mask|Cap$|Circlet|Bascinet|Burgonet|Sallet|Pelt|Cage|Tricorne|Hat$|Visage|Coif/.test(base) ? "helmet"
				: /Sabatons|Boots|Slippers|Greaves|Shoes/.test(base) ? "boots"
				: /Robe|Plate|Coat|Vest|Garb|Regalia|Brigandine|Leather|Tunic|Raiment|Jerkin/.test(base) ? "body" : "jewel";
			let added = 0;
			for (const r of raws) {
				const item = parseBlock(r, "jewel");
				if (!item || have.has(item.name)) continue;
				item.category = categoryOf(item.baseType || "");
				item.generated = true;
				items.push(item);
				have.add(item.name);
				added++;
			}
			console.log(`PoB 생성 고유 ${added}종 추가(받은 원문 ${raws.length})`);
		} catch (e) {
			console.warn("PoB 생성 고유 건너뜀:", e.message);
		}
	} else {
		console.warn("PoB 생성 고유 건너뜀 — luajit 또는 pob-src 없음");
	}
}

// slug 중복 시 카테고리 접미사로 해소 — 그래도 겹치면(같은 이름 · 같은 분류 3종: 전투 집중 · 거대한 스펙트럼 주얼 등) 베이스 이름을 덧붙인다(10-02).
//   예전엔 세 번째가 두 번째와 같은 slug 를 받아 상세 화면에서 열 수 없었다. 앞 둘의 slug(기존 주소)는 그대로 둔다.
const seen = new Map();
const slugOf = (t) => t.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
for (const item of items) {
	if (seen.has(item.slug)) item.slug = item.slug + "-" + item.category;
	if (seen.has(item.slug)) item.slug = slugOf(item.name) + "-" + slugOf(item.baseType);
	seen.set(item.slug, true);
}

items.sort((a, b) => a.name.localeCompare(b.name));
// 리마인더(인게임 회색 부연 — "최근"은 지난 4초 …, 10-04 C114): 옵션 줄의 템플릿 reminderstring Id → ReminderText 영 · 한.
//   explicits 와 같은 길이의 줄별 배열(그 줄 밑에 붙인다). 하나도 없으면 필드를 두지 않는다. 변형도 같은 규칙
{
	const reminderIdsOf = createReminderIndex(FILES_DIR, ["metadata@statdescriptions@passive_skill_stat_descriptions.txt"]);
	const remEn = load("English", "ReminderText");
	const remKo = load("Korean", "ReminderText");
	const textById = new Map(remEn.map((r, i) => [r.Id, { en: r.Text, ko: remKo[i]?.Id === r.Id && remKo[i]?.Text ? remKo[i].Text : r.Text }]));
	let lineCount = 0;
	const attach = (holder) => {
		const ids = reminderIdsOf(holder.explicits || []).map((list) => list.filter((id) => textById.get(id)?.en));
		if (!ids.some((list) => list.length)) return;
		holder.explicitsReminders = ids.map((list) => list.map((id) => textById.get(id).en));
		holder.explicitsRemindersKo = ids.map((list) => list.map((id) => textById.get(id).ko));
		lineCount += ids.filter((list) => list.length).length;
	};
	for (const item of items) {
		attach(item);
		for (const v of item.variants || []) attach(v);
	}
	console.log(`리마인더: 고유 ${items.filter((i) => i.explicitsReminders).length}/${items.length} · 줄 ${lineCount}(변형 포함)`);
}
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, JSON.stringify({ patch: PATCH, items }, null, 1));
const koCount = items.filter((i) => i.nameKo).length;
console.log(`${items.length} uniques → ${OUT} (한국어 이름 ${koCount})`);
console.log("sample:", JSON.stringify(items.find((i) => i.name === "The Anvil")));
