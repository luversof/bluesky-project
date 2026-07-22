// 영↔한 **문장 템플릿 사전** — 스탯 id 없이 영문 문장만 있는 데이터를 한글로 옮길 때 쓴다.
// (PoB 파생 데이터·트리 export 처럼 완성된 영문 문장만 주는 소스가 여럿이라 공용화했다)
//
// 원리: 숫자를 자리표시자(#)로 바꿔 키를 만들고, 한글 템플릿에 원래 숫자를 순서대로 다시 끼운다.
//   "10% increased Attack Damage" → 키 "#% increased attack damage" → "공격 피해 #% 증가" → "공격 피해 10% 증가"
// 숫자 개수가 다른 쌍은 버린다(끼워넣기가 어긋난다).

const NUM_RE = /[+\-]?\d+(?:\.\d+)?/g;

export function createTemplateTranslator() {
	const templates = new Map();
	const key = (line) => line.replace(NUM_RE, "#").toLowerCase().trim();

	/** 영/한 한 쌍을 사전에 등록(먼저 등록된 것이 우선). */
	function add(en, ko) {
		if (!en || !ko) return;
		if ((en.match(NUM_RE) || []).length !== (ko.match(NUM_RE) || []).length) return;
		const k = key(en);
		if (!templates.has(k)) templates.set(k, ko.replace(NUM_RE, "#"));
	}

	/** 스탯 서술기로 게임 전체 스탯을 영/한 서술해 사전을 채운다(약 1.1만 템플릿). */
	function addFromStats(describe, statsTable, sampleValue = 12) {
		for (const stat of statsTable) {
			if (!stat?.Id) continue;
			const values = new Map([[stat.Id, sampleValue]]);
			const en = describe(values, "English");
			const ko = describe(values, "Korean");
			if (en?.length && ko?.length && en.length === ko.length) {
				en.forEach((line, i) => add(line, ko[i]));
			}
		}
	}

	/** 이미 영/한이 짝지어진 문장 목록(트리 노드·마스터리 효과 등)으로 보충. */
	function addPairs(pairs) {
		for (const { en, ko } of pairs) {
			if (!en || !ko || en.length !== ko.length) continue;
			en.forEach((line, i) => add(line, ko[i]));
		}
	}

	/** @returns 한글 문장 또는 null(사전에 없음) */
	function translate(line) {
		const template = templates.get(key(line));
		if (!template) return null;
		const nums = line.match(NUM_RE) || [];
		let i = 0;
		return template.replace(/#/g, () => nums[i++] ?? "#");
	}

	return { add, addFromStats, addPairs, translate, get size() { return templates.size; } };
}
