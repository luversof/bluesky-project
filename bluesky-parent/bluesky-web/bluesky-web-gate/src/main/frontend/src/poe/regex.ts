// PoE 지도 정규식 생성기 (/poe/regex) — poeregexkr 식.
//  - /poe-data/map-mods.json 의 맵 모드를 목록으로 보여주고, 클릭으로 제외(!)/포함을 고른다.
//  - 선택 모드마다 "다른 모드와 겹치지 않는 최단 부분 문자열"을 찾아 |로 압축, 인게임 검색창
//    형식("!a|b" "c" — 공백 구분 = AND, !는 부정)으로 출력한다. 한도 250자.
//  - 수치는 롤마다 달라 후보 문자열에서 숫자 구간을 배제한다(숫자 없는 구간의 부분 문자열만 사용).
//  - 맵 아이템 헤더(아이템 수량/희귀도/무리 규모/티어/타락함)와 충돌하는 후보는 금지 — 아니면
//    제외 정규식이 모든 맵을 걸러버린다.
//  - 프리셋 저장/불러오기/편집: /poe/api/regex/presets (저장·삭제는 로그인 필요, 401 시 안내).
(function () {
	const root = document.getElementById("poeRegex");
	if (!root) return;
	const uiKo = (root.getAttribute("data-locale") || "ko") === "ko";
	const authenticated = root.getAttribute("data-authenticated") === "true";

	interface MapMod {
		id: string;
		name: string;
		nameKo: string;
		gen: string; // prefix | suffix
		normal: boolean;
		uber: boolean;
		quant: number;
		rarity: number;
		packSize: number;
		en: string[];
		ko: string[];
	}

	type Pick = "exclude" | "include";
	interface State {
		tab: "normal" | "uber";
		english: boolean;
		combine: "or" | "and";
		quant: number | null;
		pack: number | null;
		picks: { [id: string]: Pick };
		// 역파싱 시 모드로 해석되지 않은 정규식 항 — 잃지 않고 출력에 그대로 유지한다
		customExclude: string[];
		customInclude: string[];
		/**
		 * 수동 항이 <b>어느 모드를 노리는지</b> (모드 id → 항 목록). 체크로 바꾸지는 않는다 —
		 * 예: {@code 4\d.*치로} 는 "강해진"의 특정 티어 롤(40~49)만 노리는데 우리 목록은 티어를 하나로
		 * 병합해 두어서, 체크로 바꾸면 전 티어를 거르는 **더 센 필터**가 된다.
		 * 그래서 화면에만 표시해 "이 항이 이 모드를 노린다"를 알린다(원문은 그대로 출력).
		 */
		customTargets: { [id: string]: string[] };
	}
	const state: State = {
		tab: "normal",
		english: false,
		combine: "or",
		quant: null,
		pack: null,
		picks: {},
		customExclude: [],
		customInclude: [],
		customTargets: {},
	};
	let mods: MapMod[] = [];
	let editingId: number | null = null;
	// 문구 포함관계 연동 — A 의 모든 구간이 B 문구에 들어 있으면(일반↔T17 쌍둥이) A 만 거르는
	// 문자열은 존재하지 않는다. A 선택 시 B 도 같은 상태로 연동해 항상 전체-유일 항만 생성되게
	// 하고(폴백 제거), 가져오기 라운드트립이 문자 그대로 안정되게 한다.
	let impliedBy: { [id: string]: string[] } = {};

	const el = (id: string) => document.getElementById(id) as HTMLElement;
	const listEl = el("poeRegexList");
	const outEl = el("poeRegexOut");
	const lenEl = el("poeRegexLen");
	const warnEl = el("poeRegexWarn");
	const countEl = el("poeRegexCount");
	const searchEl = el("poeRegexSearch") as HTMLInputElement;
	const quantEl = el("poeRegexQuant") as HTMLInputElement;
	const packEl = el("poeRegexPack") as HTMLInputElement;
	const englishEl = el("poeRegexEnglish") as HTMLInputElement;
	const presetsEl = el("poeRegexPresets");
	const presetNameEl = el("poeRegexPresetName") as HTMLInputElement;
	const editingBadgeEl = el("poeRegexPresetEditing");
	const saveAsNewEl = el("poeRegexPresetNew");
	const importEl = el("poeRegexImport") as HTMLInputElement;
	const importNoteEl = el("poeRegexImportNote");
	const customEl = el("poeRegexCustom");

	const LIMIT = 250;

	// ---------- 정규식 생성 ----------

	// 맵 아이템에 항상 있을 수 있는 헤더 문구 — 후보가 이 문구에 걸리면 무효(모든 맵이 걸러진다)
	const HEADER_KO = [
		"아이템 수량: +99% (증강됨)",
		"아이템 희귀도: +99% (증강됨)",
		"몬스터 무리 규모: +99% (증강됨)",
		"아이템 클래스: 지도",
		"지도 티어: 17",
		"타락함",
		"미러티어",
	];
	const HEADER_EN = [
		"Item Quantity: +99% (augmented)",
		"Item Rarity: +99% (augmented)",
		"Monster Pack Size: +99% (augmented)",
		"Item Class: Maps",
		"Map Tier: 17",
		"Corrupted",
	];

	/** 라인 → 숫자 없는 구간들. "(a-b)" 범위 표기는 실제 아이템에선 단일 수치라 먼저 한 자리로 치환. */
	function segmentsOf(lines: string[]): string[] {
		const segs: string[] = [];
		for (const line of lines) {
			const flat = line.replace(/\((-?[\d.]+)[-~](-?[\d.]+)\)/g, "9");
			for (const part of flat.split(/[\d.]+/)) {
				const s = part.trim();
				if (s.length >= 2) segs.push(s);
			}
		}
		return segs;
	}

	function linesOf(m: MapMod, english: boolean): string[] {
		return english ? m.en : m.ko;
	}

	/** 후보가 어떤 모드(구간 집합)에 매치되는가 */
	function hits(cand: string, segs: string[]): boolean {
		for (const s of segs) if (s.indexOf(cand) !== -1) return true;
		return false;
	}

	/** 모드 로드 후 1회 — 포함관계(A 의 모든 구간 ⊂ B) 연동 표 계산 */
	function computeImplied(): void {
		impliedBy = {};
		const segsById: { [id: string]: string[] } = {};
		for (const m of mods) segsById[m.id] = segmentsOf(m.ko);
		for (const a of mods) {
			const segsA = segsById[a.id];
			if (segsA.length === 0) continue;
			for (const b of mods) {
				if (a.id === b.id) continue;
				const segsB = segsById[b.id];
				if (segsA.every((sa) => segsB.some((sb) => sb.indexOf(sa) !== -1))) {
					(impliedBy[a.id] = impliedBy[a.id] || []).push(b.id);
				}
			}
		}
	}

	/** 선택 상태 적용 — 포함관계 모드에도 같은 상태를 연동 */
	function applyPick(id: string, pick: Pick | null): void {
		const targets = [id].concat(impliedBy[id] || []);
		for (const t of targets) {
			if (pick == null) delete state.picks[t];
			else state.picks[t] = pick;
		}
	}

	function escapeRegex(s: string): string {
		return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
	}

	/**
	 * 선택 모드 집합 → 최소 |-결합 문자열 목록 (그리디 셋커버).
	 * 후보 = 각 선택 모드 구간의 부분 문자열(길이 2~14) 중 비선택 모드·헤더에 안 걸리는 것.
	 * 커버 수 최대 → 길이 최소 → 사전순으로 골라 결정적으로 만든다.
	 */
	function coverTerms(selected: MapMod[], english: boolean, warnings: string[]): string[] {
		if (selected.length === 0) return [];
		const selectedIds = new Set(selected.map((m) => m.id));
		const negative: string[][] = [];
		for (const m of mods) if (!selectedIds.has(m.id)) negative.push(segmentsOf(linesOf(m, english)));
		negative.push(segmentsOf(english ? HEADER_EN : HEADER_KO));

		const segsById: { [id: string]: string[] } = {};
		const candsById: { [id: string]: string[] } = {};
		for (const m of selected) {
			const segs = segmentsOf(linesOf(m, english));
			segsById[m.id] = segs;
			const seen = new Set<string>();
			for (const seg of segs) {
				for (let len = 2; len <= Math.min(14, seg.length); len++) {
					for (let i = 0; i + len <= seg.length; i++) {
						const cand = seg.substr(i, len);
						if (cand.trim().length < 2) continue;
						seen.add(cand);
					}
				}
			}
			candsById[m.id] = [...seen].filter((c) => !negative.some((n) => hits(c, n)));
		}

		const uncovered = new Set(selected.map((m) => m.id));
		const terms: string[] = [];
		while (uncovered.size > 0) {
			// 모든 유효 후보에 대해 (아직 안 덮인) 커버 수 계산
			let best: string | null = null;
			let bestCover: string[] = [];
			for (const id of uncovered) {
				for (const cand of candsById[id]) {
					const cover = [...uncovered].filter((u) => hits(cand, segsById[u]));
					if (
						cover.length > bestCover.length ||
						(cover.length === bestCover.length &&
							best !== null &&
							(cand.length < best.length || (cand.length === best.length && cand < best)))
					) {
						best = cand;
						bestCover = cover;
					}
				}
			}
			if (best === null) {
				// 고유 부분 문자열이 없는 모드 — 구간별로 서로 다른 모드에 막힌 경우("몬스터 피해"+"% 증가").
				// 라인 전체를 정규식으로 승격: 롤 수치 자리를 \d+ 로 바꾸면 전체-유일이 되는 라인을 찾는다.
				const id = [...uncovered][0];
				const mod = selected.filter((m) => m.id === id)[0];
				const lineTerm = lineRegexTerm(mod, english, selectedIds);
				if (lineTerm !== null) {
					terms.push(lineTerm);
					const re = new RegExp(lineTerm);
					for (const u of [...uncovered]) {
						const other = selected.filter((m) => m.id === u)[0];
						if (re.test(runtimeText(other, english))) uncovered.delete(u);
					}
					uncovered.delete(id);
					continue;
				}
				const segs = segsById[id];
				// 효과 줄 자체가 없는 어픽스(혈족의·부패의 — 핵심 스탯에 설명 텍스트가 없다)는
				// **이름으로만** 거를 수 있다. 매직 지도는 이름이 "<접두> 베이스 <접미>" 라 걸리지만
				// 레어 지도는 이름이 무작위라 안 걸린다 — 그 한계를 반드시 알린다.
				if (segs.length === 0) {
					// ⚠ 한글 접미 이름은 데이터상 "- 혈맹" 처럼 앞에 "- " 가 붙는다(접미 표기용 관례).
					//    그대로 정규식에 넣으면 인게임 아이템 이름("… 지도 혈맹")과 안 맞아 아무것도 못 거른다.
					// ⚠ 언어는 UI 로케일(uiKo)이 아니라 **정규식 언어**(english)를 따라야 한다 —
					//    영문 정규식에 한글 이름이 섞여 인게임에서 안 걸리는 버그가 있었다.
					const nameTerm = ((english ? mod.name : mod.nameKo) || "").replace(/^-\s*/, "");
					warnings.push(
						(uiKo ? "효과 문구가 없어 이름으로만 거름(레어 지도에선 안 걸림): " : "Name-only filter (rare maps unaffected): ") +
							(uiKo ? mod.nameKo : mod.name),
					);
					if (nameTerm) terms.push(escapeRegex(nameTerm));
					uncovered.delete(id);
					continue;
				}
				// 그래도 없으면(다른 모드와 라인까지 동일) 가장 긴 구간 + 경고(과다 매치 가능)
				const fallback = segs.slice().sort((a, b) => b.length - a.length)[0] || "";
				warnings.push(
					(uiKo ? "고유 문자열 없음(비슷한 모드와 함께 걸림): " : "No unique substring: ") +
						(uiKo ? mod.nameKo : mod.name),
				);
				if (fallback) terms.push(escapeRegex(fallback));
				uncovered.delete(id);
				continue;
			}
			terms.push(escapeRegex(best));
			for (const c of bestCover) uncovered.delete(c);
		}
		return terms.sort();
	}

	/**
	 * 폴백 상급판 — 모드 라인 전체를 정규식으로: 롤 수치 자리를 \d+ 로 바꿔 비선택 모드·헤더 어디에도
	 * 안 걸리는 가장 짧은 라인을 찾는다(없으면 null). 가져오기의 RegExp 매칭과 같은 문법이라 라운드트립이 안정.
	 */
	function lineRegexTerm(mod: MapMod, english: boolean, selectedIds: Set<string>): string | null {
		const patterns = linesOf(mod, english)
			.map((line) =>
				escapeRegex(line.replace(/\((-?[\d.]+)[-~](-?[\d.]+)\)/g, "\u0000")).replace(/\u0000/g, "\\d+"),
			)
			.sort((a, b) => a.length - b.length || (a < b ? -1 : 1));
		const headerText = (english ? HEADER_EN : HEADER_KO).join("\n");
		for (const pat of patterns) {
			let re: RegExp;
			try {
				re = new RegExp(pat);
			} catch (e) {
				continue;
			}
			let collide = re.test(headerText);
			if (!collide) {
				for (const m of mods) {
					if (selectedIds.has(m.id)) continue;
					if (re.test(runtimeText(m, english))) {
						collide = true;
						break;
					}
				}
			}
			if (!collide) return pat;
		}
		return null;
	}

	/** min 이상(≤999)의 정수를 매치하는 숫자 패턴 */
	function numberGte(min: number): string {
		const s = String(Math.max(1, Math.min(999, Math.floor(min))));
		const parts: string[] = [];
		for (let i = s.length - 1; i >= 0; i--) {
			const digit = Number(s.charAt(i));
			const lo = i === s.length - 1 ? digit : digit + 1;
			if (lo > 9) continue;
			const cls = lo === 9 ? "9" : lo === 0 ? "\\d" : "[" + lo + "-9]";
			parts.push(s.substring(0, i) + cls + "\\d".repeat(s.length - 1 - i));
		}
		for (let len = s.length + 1; len <= 3; len++) parts.push("[1-9]" + "\\d".repeat(len - 1));
		return "(" + parts.join("|") + ")";
	}

	function thresholdTerms(english: boolean): string[] {
		const terms: string[] = [];
		// 맵 헤더: "아이템 수량: +N%" / "Item Quantity: +N%" — 희귀도(Rarity)와 안 겹치는 최단 앵커 사용
		if (state.quant != null && state.quant > 0)
			terms.push((english ? "tity: \\+" : "량: \\+") + numberGte(state.quant) + "%");
		if (state.pack != null && state.pack > 0)
			terms.push((english ? "ze: \\+" : "규모: \\+") + numberGte(state.pack) + "%");
		return terms;
	}

	function buildRegex(): { text: string; warnings: string[] } {
		const warnings: string[] = [];
		const english = state.english;
		const exclude = mods.filter((m) => state.picks[m.id] === "exclude");
		const include = mods.filter((m) => state.picks[m.id] === "include");
		const parts: string[] = [];
		const ex = coverTerms(exclude, english, warnings).concat(state.customExclude);
		if (ex.length) parts.push('"!' + ex.join("|") + '"');
		const inc = coverTerms(include, english, warnings).concat(state.customInclude);
		if (inc.length) parts.push('"' + inc.join("|") + '"');
		const th = thresholdTerms(english);
		if (th.length === 2 && state.combine === "or") parts.push('"' + th.join("|") + '"');
		else for (const t of th) parts.push('"' + t + '"');
		return { text: parts.join(" "), warnings };
	}

	// ---------- 렌더 ----------

	function updateOutput(): void {
		const { text, warnings } = buildRegex();
		outEl.textContent = text;
		lenEl.textContent = text.length + " / " + LIMIT;
		lenEl.classList.toggle("text-error", text.length > LIMIT);
		lenEl.classList.toggle("font-bold", text.length > LIMIT);
		if (text.length > LIMIT)
			warnings.push(uiKo ? "250자 초과 — 선택을 줄이세요(인게임 검색창 한도)" : "Over 250 chars (in-game limit)");
		warnEl.textContent = warnings.join(" · ");
		warnEl.classList.toggle("hidden", warnings.length === 0);
	}

	/** 역파싱에서 남은 수동 정규식 항 칩 — ×로 제거 가능 */
	function renderCustom(): void {
		customEl.textContent = "";
		const chips: { term: string; kind: Pick }[] = [];
		for (const t of state.customExclude) chips.push({ term: t, kind: "exclude" });
		for (const t of state.customInclude) chips.push({ term: t, kind: "include" });
		customEl.classList.toggle("hidden", chips.length === 0);
		if (chips.length === 0) return;
		const label = document.createElement("span");
		label.className = "text-[11px] text-base-content/50";
		label.textContent = uiKo ? "수동 항:" : "Custom terms:";
		customEl.appendChild(label);
		for (const chip of chips) {
			const span = document.createElement("span");
			span.className =
				"badge badge-sm gap-1 font-mono " + (chip.kind === "exclude" ? "badge-error badge-outline" : "badge-success badge-outline");
			span.textContent = (chip.kind === "exclude" ? "!" : "") + chip.term;
			const x = document.createElement("button");
			x.className = "cursor-pointer font-bold";
			x.textContent = "×";
			x.addEventListener("click", () => {
				const arr = chip.kind === "exclude" ? state.customExclude : state.customInclude;
				const idx = arr.indexOf(chip.term);
				if (idx >= 0) arr.splice(idx, 1);
				renderCustom();
				updateOutput();
			});
			span.appendChild(x);
			customEl.appendChild(span);
		}
	}

	// ---------- 정규식 역파싱(가져오기) ----------

	/** 최상위 | 로 대안 분리 — 괄호/문자클래스/이스케이프 내부의 | 는 유지 */
	function splitAlternatives(s: string): string[] {
		const out: string[] = [];
		let cur = "";
		let depth = 0;
		let inClass = false;
		for (let i = 0; i < s.length; i++) {
			const ch = s.charAt(i);
			if (ch === "\\") {
				cur += ch + (s.charAt(i + 1) || "");
				i++;
				continue;
			}
			if (inClass) {
				cur += ch;
				if (ch === "]") inClass = false;
				continue;
			}
			if (ch === "[") {
				inClass = true;
				cur += ch;
				continue;
			}
			if (ch === "(") depth++;
			if (ch === ")") depth = Math.max(0, depth - 1);
			if (ch === "|" && depth === 0) {
				out.push(cur);
				cur = "";
				continue;
			}
			cur += ch;
		}
		out.push(cur);
		return out.map((x) => x.trim()).filter((x) => x.length > 0);
	}

	/** "9[5-9]" / "10\d" / "(1[2-9]\d|...)" 류 숫자 패턴에서 하한값 복원 — 첫 대안 기준. 실패 시 null. */
	function decodeMin(alt: string): number | null {
		const paren = alt.match(/\(([^)]*)\)/);
		const pat = paren ? splitAlternatives(paren[1])[0] : alt;
		if (!pat) return null;
		const tokens = pat.match(/\[(\d)[-–]9\]|\\d|\d/g);
		if (!tokens || tokens.length === 0) return null;
		let digits = "";
		for (const t of tokens) {
			if (t === "\\d") digits += "0";
			else if (t.length === 1) digits += t;
			else {
				const m = t.match(/\[(\d)/);
				if (!m) return null;
				digits += m[1];
			}
		}
		const n = parseInt(digits, 10);
		return isNaN(n) || n <= 0 ? null : n;
	}

	/** 모드의 "실제 아이템 문구"(범위 → 최대 롤 수치) — 붙여넣은 정규식을 그대로 실행할 대상 */
	function runtimeText(m: MapMod, english: boolean): string {
		return linesOf(m, english)
			.map((line) => line.replace(/\((-?[\d.]+)[-~](-?[\d.]+)\)/g, "$2"))
			.join("\n");
	}

	/**
	 * 가져오기 매칭 대상 — 효과 줄 + <b>접두/접미 이름</b>.
	 *
	 * <p>매직 지도의 이름은 "&lt;접두&gt; 베이스 &lt;접미&gt;" 로 조합되므로 인게임 검색은 이름으로도 걸린다(예: "분할의").
	 * 남이 만든 정규식에 흔한 방식인데, 효과 줄만 대상으로 삼던 때는 0개 매칭이라 전부 수동 항으로 떨어졌다.
	 */
	function matchTarget(m: MapMod, english: boolean, withName: boolean): string {
		const body = runtimeText(m, english);
		return withName ? (english ? m.name : m.nameKo) + "\n" + body : body;
	}

	function modsMatching(alt: string, english: boolean): MapMod[] {
		let re: RegExp | null = null;
		try {
			re = new RegExp(alt, "i");
		} catch (e) {
			re = null;
		}
		const test = (m: MapMod, withName: boolean) => {
			const text = matchTarget(m, english, withName);
			return re ? re.test(text) : text.indexOf(alt) !== -1; // 정규식으로 못 읽으면 리터럴로
		};
		const byLine = mods.filter((m) => test(m, false));
		if (byLine.length > 0) return byLine;
		// ⚠ 이름 매칭은 **효과 문구가 없는 어픽스에만** 쓴다(혈맹·쇠퇴·적대자의).
		//    전 모드에 열어주면 "적대자의" 같은 항이 같은 이름의 T17 변형(효과 문구 있음)까지 끌어와,
		//    재생성 때 그 변형의 문구 기반 항이 사라진다 — 레어 지도 필터링을 조용히 잃는 손실이라 금지.
		//    (이름으로만 걸리는 항은 수동 항으로 원문 보존되므로 동작 자체는 유지된다.)
		return mods.filter((m) => linesOf(m, english).length === 0 && test(m, true));
	}

	/**
	 * 범위 롤 중 <b>어떤 값에서만</b> 걸리는 항인지 — 예: {@code 력.4.*증폭} 은 "생명력 (20-100)% 증폭" 이
	 * 40~49 로 뜬 지도에서만 걸린다. 우리 코퍼스는 최대 롤 문구라 최대 롤에서는 안 걸린다.
	 *
	 * <p>이런 항은 <b>모드 선택이 아니라 롤 임계값</b>이라 체크박스로 복원할 수 없다. 조용히 버리지 않고 수동 항으로
	 * 보존하되(=동작 그대로 유지), 사용자에게 "왜 체크가 안 켜졌는지"를 알려주기 위해 따로 센다.
	 */
	function modsMatchingSomeRoll(alt: string, english: boolean): MapMod[] {
		const out: MapMod[] = [];
		let re: RegExp;
		try {
			re = new RegExp(alt, "i");
		} catch (e) {
			return out;
		}
		const RANGE = /\((-?[\d.]+)[-~](-?[\d.]+)\)/g;
		for (const m of mods) {
			const raw = linesOf(m, english).join("\n");
			const ranges = raw.match(RANGE);
			if (!ranges) continue;
			// 범위를 하나씩 훑고 나머지는 최대 롤로 고정 — 다중 범위 조합까지 전수로 보진 않는다(실용 근사).
			for (let i = 0; i < ranges.length; i++) {
				const bounds = /\((-?[\d.]+)[-~](-?[\d.]+)\)/.exec(ranges[i]);
				if (!bounds) continue;
				const lo = Math.ceil(Number(bounds[1]));
				const hi = Math.floor(Number(bounds[2]));
				if (!isFinite(lo) || !isFinite(hi) || hi < lo || hi - lo > 400) continue;
				for (let v = lo; v <= hi; v++) {
					let k = -1;
					const text = raw.replace(RANGE, (whole, _a, b) => {
						k++;
						return k === i ? String(v) : b;
					});
					if (re.test(text)) { out.push(m); v = hi; i = ranges.length; break; }
				}
			}
		}
		return out;
	}

	/** 붙여넣은 정규식 → 선택/임계값/수동 항 복원. 요약 메시지를 반환. */
	function importRegex(text: string): string {
		interface Group {
			negated: boolean;
			alts: string[];
		}
		const groups: Group[] = [];
		// "..." 그룹 우선 추출, 나머지는 공백 구분 항 (인게임 검색 규칙과 동일)
		const rest = text.replace(/"([^"]*)"/g, (whole, body) => {
			const negated = body.charAt(0) === "!";
			const inner = negated ? body.slice(1) : body;
			if (inner.trim()) groups.push({ negated, alts: splitAlternatives(inner) });
			return " ";
		});
		for (const token of rest.split(/\s+/)) {
			if (!token) continue;
			const negated = token.charAt(0) === "!";
			const inner = negated ? token.slice(1) : token;
			if (inner) groups.push({ negated, alts: splitAlternatives(inner) });
		}
		if (groups.length === 0) return uiKo ? "인식할 항이 없습니다" : "Nothing to import";

		// 언어 감지 — 임계값 아닌 대안들을 한/영 코퍼스에 각각 대 보고 매치가 많은 쪽
		const QUANT_KEY = /량: ?\\?\+|수량|tity: ?\\?\+|Quantity/i;
		const PACK_KEY = /규모: ?\\?\+|무리|ze: ?\\?\+|Pack ?Size/i;
		const plainAlts: string[] = [];
		for (const g of groups)
			for (const alt of g.alts) if (!QUANT_KEY.test(alt) && !PACK_KEY.test(alt)) plainAlts.push(alt);
		let koHits = 0;
		let enHits = 0;
		for (const alt of plainAlts) {
			if (modsMatching(alt, false).length > 0) koHits++;
			if (modsMatching(alt, true).length > 0) enHits++;
		}
		const english = enHits > koHits;

		// 상태 리셋 후 채우기 (탭·프리셋 편집 상태는 유지)
		state.picks = {};
		state.customExclude = [];
		state.customInclude = [];
		state.customTargets = {};
		state.quant = null;
		state.pack = null;
		state.english = english;
		let matchedMods = 0;
		let customCount = 0;
		let rollCount = 0;
		const thresholdGroups: number[] = []; // 임계값이 나온 그룹 인덱스 — 같은 그룹이면 OR, 다른 그룹이면 AND
		groups.forEach((g, gi) => {
			for (const alt of g.alts) {
				if (QUANT_KEY.test(alt)) {
					const min = decodeMin(alt);
					if (min != null) {
						state.quant = min;
						thresholdGroups.push(gi);
						continue;
					}
				}
				if (PACK_KEY.test(alt)) {
					const min = decodeMin(alt);
					if (min != null) {
						state.pack = min;
						thresholdGroups.push(gi);
						continue;
					}
				}
				const matched = modsMatching(alt, english);
				// 0개 = 해석 불가, 30개 초과 = 지나치게 일반적(헤더 앵커 등) — 수동 항으로 보존
				if (matched.length === 0 || matched.length > 30) {
					(g.negated ? state.customExclude : state.customInclude).push(alt);
					customCount++;
					// 롤 수치를 노린 항은 "왜 체크가 안 켜졌는지"가 사용자에게 안 보이므로 따로 센다
					if (matched.length === 0) {
						const rollHits = modsMatchingSomeRoll(alt, english);
						if (rollHits.length) rollCount++;
						// "인식이 안 된다"의 실체는 **어느 모드를 노리는지 안 보이는 것**이었다.
						// 체크로 바꾸진 않되(티어 병합 때문에 더 센 필터가 된다) 그 모드 행에 표시한다.
						for (const hit of rollHits) (state.customTargets[hit.id] = state.customTargets[hit.id] || []).push(alt);
					}
					continue;
				}
				for (const m of matched) {
					// 제외가 포함을 덮지 않게 — 같은 모드가 두 그룹에 걸리면 먼저 온 그룹 우선
					if (!state.picks[m.id]) state.picks[m.id] = g.negated ? "exclude" : "include";
				}
				matchedMods += matched.length;
			}
		});
		if (state.quant != null && state.pack != null)
			state.combine = thresholdGroups.length === 2 && thresholdGroups[0] === thresholdGroups[1] ? "or" : "and";

		englishEl.checked = state.english;
		quantEl.value = state.quant != null ? String(state.quant) : "";
		packEl.value = state.pack != null ? String(state.pack) : "";
		renderCustom();
		renderList();
		updateOutput();
		const parts: string[] = [];
		const exCount = Object.keys(state.picks).filter((id) => state.picks[id] === "exclude").length;
		const incCount = Object.keys(state.picks).filter((id) => state.picks[id] === "include").length;
		if (exCount) parts.push((uiKo ? "제외 " : "exclude ") + exCount);
		if (incCount) parts.push((uiKo ? "포함 " : "include ") + incCount);
		if (state.quant != null) parts.push((uiKo ? "수량 ≥" : "quant ≥") + state.quant);
		if (state.pack != null) parts.push((uiKo ? "무리 ≥" : "pack ≥") + state.pack);
		if (customCount) parts.push((uiKo ? "수동 항 " : "custom ") + customCount);
		// 왜 체크가 안 켜졌는지 말해준다 — 침묵하면 "가져오기가 고장났다"로 보인다
		if (rollCount)
			parts.push(
				uiKo
					? "그중 " + rollCount + "개는 롤 수치 조건이라 모드 선택으로 못 바꿉니다(그대로 유지)"
					: rollCount + " of them key on rolled values (kept as-is)",
			);
		return (uiKo ? "가져옴: " : "Imported: ") + (parts.length ? parts.join(" · ") : uiKo ? "매칭 없음" : "no matches");
	}

	/**
	 * 인게임 모드 문구의 변동 수치 강조 — 서버 PoeText.highlightValues 와 같은 토큰 규칙(+25%, (80-120), 1.15 …).
	 * innerHTML 대신 DOM 조립(문구는 자체 데이터지만 escape 실수 여지를 아예 없앤다).
	 */
	const VALUE_TOKEN = /[+\-]?\(?\d+(?:\.\d+)?(?:\s*[-~–]\s*\d+(?:\.\d+)?)?\)?%?/g;
	function appendHighlighted(parent: HTMLElement, line: string): void {
		VALUE_TOKEN.lastIndex = 0;
		let last = 0;
		let match: RegExpExecArray | null;
		while ((match = VALUE_TOKEN.exec(line)) !== null) {
			if (match[0] === "") {
				VALUE_TOKEN.lastIndex++;
				continue;
			}
			if (match.index > last) parent.appendChild(document.createTextNode(line.slice(last, match.index)));
			const span = document.createElement("span");
			span.className = "poe-val";
			span.textContent = match[0];
			parent.appendChild(span);
			last = match.index + match[0].length;
		}
		if (last < line.length) parent.appendChild(document.createTextNode(line.slice(last)));
	}

	function pickClass(pick: Pick | undefined): string {
		if (pick === "exclude") return "border-error bg-error/10";
		if (pick === "include") return "border-success bg-success/10";
		return "border-base-200 bg-base-100 hover:border-base-300";
	}

	function renderList(): void {
		const q = (searchEl.value || "").trim().toLowerCase();
		listEl.textContent = "";
		// 접두=왼쪽, 접미=오른쪽 열로 분리 (모바일은 접두→접미 순 세로).
		// 제목은 /poe/mods 접두·접미 섹션과 같은 인게임 모드 파랑 굵은 글씨 + 개수 소자.
		const columns: { [gen: string]: HTMLElement } = {};
		const counts: { [gen: string]: HTMLElement } = {};
		for (const gen of ["prefix", "suffix"]) {
			const col = document.createElement("div");
			col.className = "space-y-1.5 min-w-0";
			const head = document.createElement("h2");
			head.className = "mb-1 flex items-baseline gap-2 px-1 font-bold text-[#8888ff]";
			head.appendChild(
				document.createTextNode(
					gen === "prefix" ? (uiKo ? "접두" : "Prefixes") : uiKo ? "접미" : "Suffixes",
				),
			);
			const cnt = document.createElement("span");
			cnt.className = "text-xs font-normal text-base-content/40";
			head.appendChild(cnt);
			counts[gen] = cnt;
			col.appendChild(head);
			columns[gen] = col;
			listEl.appendChild(col);
		}
		let shown = 0;
		const shownByGen: { [gen: string]: number } = { prefix: 0, suffix: 0 };
		for (const m of mods) {
			if (state.tab === "normal" ? !m.normal : !m.uber) continue;
			const displayLines = uiKo ? m.ko : m.en;
			const hay = (m.ko.join("\n") + "\n" + m.en.join("\n") + "\n" + m.nameKo + "\n" + m.name).toLowerCase();
			if (q && hay.indexOf(q) === -1) continue;
			shown++;
			const pick = state.picks[m.id];
			const row = document.createElement("button");
			row.type = "button";
			row.className =
				"block w-full text-left rounded-lg border px-3 py-1.5 transition-colors cursor-pointer " + pickClass(pick);
			row.setAttribute("data-mod-id", m.id);

			const badges = document.createElement("div");
			badges.className = "flex items-center gap-1 float-right ml-2";
			// 접두/접미 배지는 없앴다 — 열이 이미 갈라 보여주고, 인게임 지도엔 모드마다 라벨이 붙지 않는다.
			if (m.uber && !m.normal) {
				const t17 = document.createElement("span");
				t17.className = "badge badge-warning badge-xs";
				t17.textContent = "T17";
				badges.appendChild(t17);
			}
			// 지도 보상 3종 — 인게임 지도 툴팁과 같은 순서(수량 → 희귀도 → 무리 크기)로 한 묶음.
			//   예전엔 수량만, 그것도 일반 탭에서만 보여줬다. 희귀도는 107종·무리 크기는 112종 전부 데이터가
			//   있는데 화면에 없었고, T17 전용 36종도 세 값이 다 있어 탭으로 가릴 이유가 없다.
			//   모드를 고르는 기준 자체가 이 셋의 trade-off 다(위험 ↔ 보상).
			const reward: string[] = [];
			if (m.quant > 0) reward.push((uiKo ? "수량 +" : "Q +") + m.quant + "%");
			if (m.rarity > 0) reward.push((uiKo ? "희귀 +" : "R +") + m.rarity + "%");
			if (m.packSize > 0) reward.push((uiKo ? "무리 +" : "P +") + m.packSize + "%");
			if (reward.length) {
				const rb = document.createElement("span");
				rb.className = "badge badge-ghost badge-xs font-mono whitespace-nowrap";
				rb.textContent = reward.join(" · ");
				rb.title = uiKo
					? "지도 보상(최대 롤) — 아이템 수량 / 아이템 희귀도 / 몬스터 무리 크기"
					: "Map reward (max roll) — item quantity / item rarity / monster pack size";
				badges.appendChild(rb);
			}
			// 선택된 모드의 제외↔포함 전환은 **이 배지에서만** — 본문 클릭은 선택/해제라 오조작이 없다.
			if (pick) {
				const pb = document.createElement("span");
				pb.className =
					"badge badge-xs cursor-pointer " + (pick === "exclude" ? "badge-error" : "badge-success");
				pb.textContent = pick === "exclude" ? (uiKo ? "제외" : "excl") : uiKo ? "포함" : "incl";
				pb.title =
					pick === "exclude"
						? uiKo
							? "클릭하면 포함(강조)으로 전환"
							: "Click to switch to include"
						: uiKo
							? "클릭하면 제외(거름)로 전환"
							: "Click to switch to exclude";
				pb.setAttribute("data-pick-toggle", pick);
				pb.addEventListener("click", (e) => {
					e.stopPropagation();
					applyPick(m.id, pick === "exclude" ? "include" : "exclude");
					renderList();
					updateOutput();
				});
				badges.appendChild(pb);
			}
			row.appendChild(badges);

			// 가져온 정규식의 수동 항이 이 모드의 **특정 롤**을 노리는 경우 — 체크는 아니지만 눈에 보이게 한다.
			// (예: 4\d.*치로 → "강해진"의 40~49 롤. 티어 변형을 하나로 병합해 둬서 체크로는 못 옮긴다.)
			const targets = state.customTargets[m.id];
			if (targets && targets.length) {
				const tb = document.createElement("div");
				tb.className = "mt-0.5 text-[11px] font-mono text-warning";
				tb.textContent = (uiKo ? "가져온 항이 이 모드의 특정 롤을 노림: " : "Imported term targets a specific roll: ") + targets.join(" , ");
				row.appendChild(tb);
				row.className += " ring-1 ring-warning/50";
			}
			// 효과 문구가 없는 어픽스(혈맹·쇠퇴·적대자의 — 핵심 스탯에 설명 텍스트가 없다)는
			// 행이 통째로 비어 보인다. 이 목록은 원래 **인게임 모드 문구**만 보여주지만, 이 셋만은
			// 어픽스 이름을 대신 띄운다 — 안 그러면 보상 배지만 뜬 정체불명의 빈 줄이 된다.
			if (displayLines.length === 0) {
				const div = document.createElement("div");
				div.className = "text-[13px] leading-5 text-[#8888ff] italic";
				div.textContent = (uiKo ? m.nameKo : m.name) + (uiKo ? " (효과 문구 없음 — 이름으로 거름)" : " (no mod text — name filter)");
				row.appendChild(div);
			}
			// 모드 문구는 인게임 아이템과 같게 — 모드 파랑 + 수치만 강조(.poe-val), 모드 페이지/툴팁과 동일 스택
			for (const line of displayLines) {
				const div = document.createElement("div");
				div.className = "text-[13px] leading-5 text-[#8888ff] whitespace-pre-line";
				appendHighlighted(div, line);
				row.appendChild(div);
			}
			row.addEventListener("click", () => {
				// 본문 클릭 = 선택/해제. 선택은 항상 제외(거르기) — 지도 정규식은 거의 전부 "이 모드 빼고 사기"라
				// 3상 순환(제외→포함→해제)은 해제하려다 포함이 켜지는 오조작만 만들었다(사용자 보고).
				applyPick(m.id, state.picks[m.id] ? null : "exclude");
				renderList();
				updateOutput();
			});
			const genKey = m.gen === "prefix" ? "prefix" : "suffix";
			shownByGen[genKey]++;
			columns[genKey].appendChild(row);
		}
		for (const gen of ["prefix", "suffix"]) {
			counts[gen].textContent = String(shownByGen[gen]);
			// 검색 결과가 한쪽뿐이면 빈 열의 제목만 남아 허전하다 — 통째로 감춘다
			columns[gen].classList.toggle("hidden", shownByGen[gen] === 0);
		}
		countEl.textContent = shown + " / " + mods.filter((m) => (state.tab === "normal" ? m.normal : m.uber)).length;
		// 탭/결합 버튼 활성 표시
		document.querySelectorAll<HTMLElement>("[data-regex-tab]").forEach((b) => {
			b.classList.toggle("btn-primary", b.getAttribute("data-regex-tab") === state.tab);
		});
		document.querySelectorAll<HTMLElement>("[data-regex-combine]").forEach((b) => {
			b.classList.toggle("btn-primary", b.getAttribute("data-regex-combine") === state.combine);
		});
	}

	// ---------- 프리셋 ----------

	function fmtDate(ms: number): string {
		const d = new Date(ms);
		const pad = (n: number) => (n < 10 ? "0" + n : "" + n);
		return d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()) + " " + pad(d.getHours()) + ":" + pad(d.getMinutes());
	}

	function loginWarn(): void {
		alert(uiKo ? "로그인이 필요합니다(세션 만료 포함). 로그인 후 다시 시도하세요." : "Login required (session may have expired).");
	}

	async function refreshPresets(): Promise<void> {
		try {
			const res = await fetch("/poe/api/regex/presets", { headers: { Accept: "application/json" } });
			if (!res.ok) return;
			const list: { id: number; name: string; updatedMs: number; regex: string }[] = await res.json();
			presetsEl.textContent = "";
			if (list.length === 0) {
				const empty = document.createElement("div");
				empty.className = "text-xs text-base-content/40";
				empty.textContent = uiKo ? "저장된 정규식이 없습니다" : "No saved presets";
				presetsEl.appendChild(empty);
				return;
			}
			for (const p of list) {
				const row = document.createElement("div");
				row.className = "flex flex-wrap items-center gap-2 rounded-lg border border-base-200 px-2 py-1";
				const name = document.createElement("span");
				name.className = "text-sm font-semibold";
				name.textContent = p.name;
				const date = document.createElement("span");
				date.className = "text-[11px] text-base-content/40 font-mono";
				date.textContent = fmtDate(p.updatedMs);
				const preview = document.createElement("code");
				preview.className = "flex-1 min-w-[8rem] truncate text-[11px] font-mono text-base-content/60";
				preview.textContent = p.regex;
				const loadBtn = document.createElement("button");
				loadBtn.className = "btn btn-xs";
				loadBtn.textContent = uiKo ? "불러오기" : "Load";
				loadBtn.addEventListener("click", () => loadPreset(p.id));
				const copyBtn = document.createElement("button");
				copyBtn.className = "btn btn-xs btn-ghost";
				copyBtn.textContent = uiKo ? "복사" : "Copy";
				copyBtn.addEventListener("click", () => navigator.clipboard.writeText(p.regex));
				const delBtn = document.createElement("button");
				delBtn.className = "btn btn-xs btn-ghost text-error";
				delBtn.textContent = uiKo ? "삭제" : "Del";
				delBtn.addEventListener("click", async () => {
					if (!confirm(uiKo ? '"' + p.name + '" 프리셋을 삭제할까요?' : "Delete preset?")) return;
					const r = await fetch("/poe/api/regex/presets/" + p.id, { method: "DELETE" });
					if (r.status === 401) return loginWarn();
					if (editingId === p.id) clearEditing();
					refreshPresets();
				});
				row.appendChild(name);
				row.appendChild(date);
				row.appendChild(preview);
				row.appendChild(loadBtn);
				row.appendChild(copyBtn);
				row.appendChild(delBtn);
				presetsEl.appendChild(row);
			}
		} catch (e) {
			/* 목록 실패는 조용히 — 페이지 핵심 기능(생성)은 데이터만으로 동작 */
		}
	}

	function clearEditing(): void {
		editingId = null;
		editingBadgeEl.classList.add("hidden");
		saveAsNewEl.classList.add("hidden");
	}

	function applyState(data: any, name: string, id: number): void {
		state.tab = data.tab === "uber" ? "uber" : "normal";
		state.english = !!data.english;
		state.combine = data.combine === "and" ? "and" : "or";
		state.quant = typeof data.quant === "number" ? data.quant : null;
		state.pack = typeof data.pack === "number" ? data.pack : null;
		state.picks = {};
		const ids = new Set(mods.map((m) => m.id));
		for (const mid of data.exclude || []) if (ids.has(mid)) state.picks[mid] = "exclude";
		for (const mid of data.include || []) if (ids.has(mid)) state.picks[mid] = "include";
		state.customExclude = (data.customExclude || []).filter((t: any) => typeof t === "string");
		state.customInclude = (data.customInclude || []).filter((t: any) => typeof t === "string");
		renderCustom();
		englishEl.checked = state.english;
		quantEl.value = state.quant != null ? String(state.quant) : "";
		packEl.value = state.pack != null ? String(state.pack) : "";
		presetNameEl.value = name;
		editingId = id;
		editingBadgeEl.textContent = (uiKo ? "편집 중: " : "Editing: ") + name;
		editingBadgeEl.classList.remove("hidden");
		saveAsNewEl.classList.remove("hidden");
		renderList();
		updateOutput();
	}

	async function loadPreset(id: number): Promise<void> {
		const res = await fetch("/poe/api/regex/presets/" + id, { headers: { Accept: "application/json" } });
		if (!res.ok) return;
		const text = await res.text();
		if (!text) return;
		const p = JSON.parse(text);
		applyState(p.data || {}, p.name || "", p.id);
	}

	async function savePreset(): Promise<void> {
		const name = (presetNameEl.value || "").trim();
		if (!name) {
			presetNameEl.focus();
			return;
		}
		if (!authenticated) return loginWarn();
		const { text } = buildRegex();
		const data = {
			tab: state.tab,
			english: state.english,
			combine: state.combine,
			quant: state.quant,
			pack: state.pack,
			exclude: Object.keys(state.picks).filter((id) => state.picks[id] === "exclude"),
			include: Object.keys(state.picks).filter((id) => state.picks[id] === "include"),
			customExclude: state.customExclude,
			customInclude: state.customInclude,
		};
		const res = await fetch("/poe/api/regex/presets", {
			method: "POST",
			headers: { "Content-Type": "application/json", Accept: "application/json" },
			body: JSON.stringify({ id: editingId, name: name, regex: text, data: data }),
		});
		if (res.status === 401) return loginWarn();
		if (res.ok) {
			const saved = await res.json();
			if (saved && saved.id) {
				editingId = saved.id;
				editingBadgeEl.textContent = (uiKo ? "편집 중: " : "Editing: ") + saved.name;
				editingBadgeEl.classList.remove("hidden");
				saveAsNewEl.classList.remove("hidden");
			}
			refreshPresets();
		}
	}

	// ---------- 이벤트 ----------

	document.querySelectorAll<HTMLElement>("[data-regex-tab]").forEach((b) => {
		b.addEventListener("click", () => {
			state.tab = b.getAttribute("data-regex-tab") === "uber" ? "uber" : "normal";
			renderList();
		});
	});
	document.querySelectorAll<HTMLElement>("[data-regex-combine]").forEach((b) => {
		b.addEventListener("click", () => {
			state.combine = b.getAttribute("data-regex-combine") === "and" ? "and" : "or";
			renderList();
			updateOutput();
		});
	});
	searchEl.addEventListener("input", () => {
		renderList();
		const url = new URL(location.href);
		const q = (searchEl.value || "").trim();
		if (q) url.searchParams.set("q", q);
		else url.searchParams.delete("q");
		history.replaceState(history.state, "", url);
	});
	quantEl.addEventListener("input", () => {
		const v = parseInt(quantEl.value, 10);
		state.quant = isNaN(v) || v <= 0 ? null : v;
		updateOutput();
	});
	packEl.addEventListener("input", () => {
		const v = parseInt(packEl.value, 10);
		state.pack = isNaN(v) || v <= 0 ? null : v;
		updateOutput();
	});
	englishEl.addEventListener("change", () => {
		state.english = englishEl.checked;
		updateOutput();
	});
	el("poeRegexCopy").addEventListener("click", () => {
		const text = outEl.textContent || "";
		if (!text) return;
		navigator.clipboard.writeText(text).then(() => {
			const btn = el("poeRegexCopy");
			const orig = btn.textContent;
			btn.textContent = uiKo ? "복사됨!" : "Copied!";
			setTimeout(() => (btn.textContent = orig), 1200);
		});
	});
	el("poeRegexReset").addEventListener("click", () => {
		state.picks = {};
		state.quant = null;
		state.pack = null;
		state.customExclude = [];
		state.customInclude = [];
		state.customTargets = {};
		quantEl.value = "";
		packEl.value = "";
		presetNameEl.value = "";
		importEl.value = "";
		importNoteEl.classList.add("hidden");
		clearEditing();
		renderCustom();
		renderList();
		updateOutput();
	});
	el("poeRegexImportBtn").addEventListener("click", () => {
		const text = (importEl.value || "").trim();
		if (!text) {
			importEl.focus();
			return;
		}
		let note = importRegex(text);
		// 선택된 모드가 전부 T17 전용이면 보이도록 탭 자동 전환 (반대도 동일)
		const picked = mods.filter((m) => state.picks[m.id]);
		if (picked.length > 0) {
			if (state.tab === "normal" && picked.every((m) => !m.normal)) state.tab = "uber";
			else if (state.tab === "uber" && picked.every((m) => !m.uber)) state.tab = "normal";
			renderList();
		}
		// 재생성 표기가 붙여넣은 것과 다를 수 있다 — 예: 긴 폴백 문장이 같은 문구를 가진 다른 모드까지
		// 걸러왔다면, 그 모드도 함께 선택되면서 더 짧은 고유 문자열이 합법이 되어 재압축된다. 거르는 대상은 동일.
		if ((outEl.textContent || "").trim() !== text) {
			note += uiKo ? " · 표기는 재압축됨(거르는 대상은 동일)" : " · rewritten shorter (same maps filtered)";
		}
		importNoteEl.textContent = note;
		importNoteEl.classList.remove("hidden");
	});
	importEl.addEventListener("keydown", (e) => {
		if ((e as KeyboardEvent).key === "Enter") el("poeRegexImportBtn").click();
	});
	el("poeRegexPresetSave").addEventListener("click", savePreset);
	saveAsNewEl.addEventListener("click", () => {
		clearEditing();
		presetNameEl.focus();
	});

	// ---------- 초기화 ----------

	fetch("/poe-data/map-mods.json", { cache: "no-cache" })
		.then((r) => r.json())
		.then((d) => {
			mods = d.mods || [];
			computeImplied();
			const initQ = new URLSearchParams(location.search).get("q");
			if (initQ) searchEl.value = initQ;
			renderList();
			updateOutput();
			refreshPresets();
		})
		.catch(() => {
			listEl.textContent = uiKo
				? "맵 모드 데이터가 없습니다 — 데이터 관리에서 갱신을 실행하세요."
				: "No map mod data — run data update in admin.";
		});
})();
