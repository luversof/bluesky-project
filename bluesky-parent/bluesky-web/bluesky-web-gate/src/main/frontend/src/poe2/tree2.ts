// PoE2 패시브 트리 뷰어/플래너 — /poe-data/poe2/passive-tree.json(tools/poe2-extract/parse-tree2.mjs 산출물)을 캔버스에 그린다.
// 기능: 확대(휠·버튼)·이동(끌기) · 직업/전직 선택 · 노드 검색(이름/스탯, 한/영) · 노드 툴팁(인게임 팝업 구조) ·
//       클릭 할당(시작점에서 가장 짧은 경로로 잇기, 다시 누르면 해제 + 끊긴 노드 정리) · 할당 상태를 주소(#)로 공유.
// 아틀라스 모드: 데이터에 mode "atlas" 면(/poe-data/poe2/atlas-tree.json) 직업 대신 하위 트리 뿌리 7개가 모두 공짜 시작점이고,
//   점수는 하위 트리별로 센다(인게임도 트리마다 포인트가 따로다). 아이콘 시트 위치는 캔버스 data-sprite-base.
// PoE1 뷰어(poe/tree.ts)와 따로 둔 까닭: 데이터 모양(좌표 미리 계산 · 전직이 본 트리 바깥 둘레 · 능력치 노드 선택지)이 다르고,
// PoE1 뷰어는 아틀라스·문신·도유까지 얽혀 4천 줄이 넘는다.
(function () {
	type Conn = { id: number; orbit: number };
	type TreeNode = {
		id: number;
		name: string;
		nameKo?: string;
		kind: string;
		stats: string[];
		statsKo?: string[];
		x: number;
		y: number;
		group: number;
		orbit: number;
		orbitIndex: number;
		out: Conn[];
		ascendancy: string | null;
		classStart: string[] | null;
		multipleChoiceOption?: boolean; // 전직 선택지(PoB isMultipleChoiceOption) — 전직 포인트로 안 센다(C38)
		unlockConstraint?: number[]; // 잠금 조건(PoB unlockConstraint — Sacred Unity): 이 노드들이 다 찍혀야 찍힌다(C50)
		freeAllocate?: boolean; // 공짜 노드(PoB isFreeAllocate — Sanguimancy · Sacred Unity · Smith's Masterwork) — 포인트로 안 센다(C49)
		flavour?: string;
		flavourKo?: string;
		keywords?: string[]; // 스탯 문구의 강조 용어 Id(KeywordPopups) — 인게임 Alt 설명(10-04 C110)
		options?: { id: number; name: string; nameKo?: string; stats: string[]; statsKo?: string[]; icon?: string }[];
		tree?: string; // 아틀라스 하위 트리 키
		adj: number[];
	};
	type SubTree = { key: string; name: string; nameKo?: string; rootId: number; nodeCount: number };
	type Asc = { id: string; name: string; nameKo?: string; startNodeId?: number };
	type Klass = { index: number; name: string; nameKo?: string; startNodeId: number; ascendancies: Asc[] };
	type TreeData = {
		patch: string;
		bounds: { minX: number; minY: number; maxX: number; maxY: number };
		orbitRadii: number[];
		classes: Klass[];
		mode?: string;
		trees?: SubTree[];
		groups: { [id: string]: { x: number; y: number } };
		nodes: { [id: string]: Omit<TreeNode, "id" | "adj"> };
		keywords?: { [id: string]: { term: string; termKo?: string | null; def: string; defKo?: string | null } };
	};

	const canvas = document.getElementById("poe2TreeCanvas") as HTMLCanvasElement | null;
	if (!canvas) return;
	const ctx = canvas.getContext("2d")!;
	const tooltip = document.getElementById("poe2TreeTooltip") as HTMLElement | null;
	const classSel = document.getElementById("poe2TreeClass") as HTMLSelectElement | null;
	const ascSel = document.getElementById("poe2TreeAsc") as HTMLSelectElement | null;
	const search = document.getElementById("poe2TreeSearch") as HTMLInputElement | null;
	const searchCount = document.getElementById("poe2TreeSearchCount");
	const atlasSel = document.getElementById("poe2AtlasTree") as HTMLSelectElement | null; // 아틀라스: 하위 트리로 이동
	const points = document.getElementById("poe2TreePoints");
	const status = document.getElementById("poe2TreeStatus");
	const isKorean = (canvas.getAttribute("data-locale") || "ko") === "ko";
	const src = canvas.getAttribute("data-tree-src") || "/poe-data/poe2/passive-tree.json";
	const t = (ko: string, en: string) => (isKorean ? ko : en);

	const nodes = new Map<number, TreeNode>();
	// 노드 아이콘 — tree-sprites/index.json(아이콘 경로 → 시트 좌표) + 시트 이미지. 없으면 색 원만 그린다.
	type Sprite = { s: string; x: number; y: number; w: number; h: number };
	let sprites: { [icon: string]: Sprite } = {};
	const sheets = new Map<string, HTMLImageElement>();
	// 트리 주소엔 ?v=<파일 시각> 이 붙는다(C38 PoeDataVersion) — 예전 $ 앵커가 안 맞아 아이콘이 통째로 안 그려졌다(10-03 nightly). 쿼리를 떼고 바꾼다
	const spriteBase = canvas.getAttribute("data-sprite-base") || src.replace(/passive-tree\.json(\?.*)?$/, "tree-sprites/");
	let data: TreeData | null = null;
	let klass: Klass | null = null;
	let asc: Asc | null = null;
	const allocated = new Set<number>();
	// 장비가 "Allocates X" 로 준 노드(빌드 주소 g=, 10-03 C38) — 점수가 아니고(PoB isFreeAllocate) 출발점처럼 길을 잇는다.
	//   없으면 그 노드 너머의 유료 노드들이 시작점에서 끊겨 걷혀 나갔다(실빌드 데드아이: 9개, 패시브 124 → 115 로 적게 셈)
	const granted = new Set<number>();
	// 주얼 "From Nothing" — 빌드 주소 l=<핵심 id>:<반경>(주얼 반경 × 1.2, 서버가 계산). 그 반경 안은 연결 없이 찍힌다(C40)
	let fromNothing: { id: number; r: number }[] = [];
	// 주얼 "Passives in Radius can be Allocated without being connected"(Controlled Metamorphosis) — 빌드 주소 r=<주얼 칸>:<안>:<바깥>(C47)
	let leapRings: { id: number; inner: number; outer: number }[] = [];
	// 무기 세트 전용 패시브(10-02, PoB-PoE2 allocMode) — 노드 id → 1·2(없으면 공용 0). 지금 찍는 모드는 curSet.
	//   규칙(PoB PassiveSpec): 핵심·주얼 칸·전직 노드는 늘 공용, 경로는 공용 노드나 같은 세트 노드만 지나간다.
	const setMode = new Map<number, number>();
	let curSet = 0;
	const SET_COLOR = ["", "#e0803a", "#4aa3df"];
	const modeOf = (id: number) => setMode.get(id) || 0;
	const forcedShared = (n: TreeNode) => n.kind === "keystone" || n.kind === "jewel" || !!n.ascendancy;
	// 능력치 노드("+5 아무 능력치") 선택 — 1 힘 · 2 민첩 · 3 지능(PoB AttributeOverride 와 같은 번호). 인게임도 찍을 때 고른다(10-01)
	const attrPick = new Map<number, number>();
	const ATTR_COLOR = ["", "#e05a4a", "#5fbf5f", "#5a8ee0"];
	/** 노드가 실제로 주는 줄 — 능력치 노드는 고른 능력치의 줄(안 골랐으면 "+5 아무 능력치" 그대로) */
	// ── 트리 규칙 순수 함수(10-02) — 바깥 상태 없이 인자만 본다: test/poe2TreeRules.test.mjs 가 빌드된 js 에서 이름으로 꺼내 시험한다 ──
	/** 소형 패시브(효과 배율 대상) — 일반 노드 중 전직 아님(PoB CalcSetup: Normal · not attribute · not ascendancy, 능력치 노드는 kind attribute). */
	// 검색 비교 열쇠(10-02 C24) — PoE1 tree.ts searchKey 와 같은 규칙: 소문자 + 띄어쓰기 무시, 줄바꿈(이름 · 스탯 줄 사이)은 남긴다.
	function searchKey(text: string): string {
		return text.toLowerCase().replace(/[ \t\u00a0]+/g, "");
	}

	// 포인트 한도(PoB-PoE2 Build.lua EstimatePlayerProgress · PassiveSpec:CountAllocNodes, 10-03 C38):
	//   일반 = 공용 + 두 무기 세트 중 큰 쪽(PoB: 전체 − min(세트 I, 세트 II)), 한도 99(레벨) + 24(퀘스트 = QuestRewards 합) = 123.
	//   무기 세트 한도 = 퀘스트 포인트 24(세트마다), 전직 8. 아이템 · 전직의 추가 포인트(ExtraPoints)는 트리만으로는 알 수 없어 뺀다.
	//   추가 포인트(PoB ModParser): "Grants N Passive Skill Point(s)" → ExtraPoints(패스파인더 · 오라클 전직 노드),
	//   "N Passive Skill Points become Weapon Set Skill Points" → 세트 한도 +N(위치헌터 무기 달인). 장비가 주는 것은 트리만으로 모른다.
	//   엔드게임 포인트(10-03 C49): 0.5 의 최초 칙령 순교자 +1(일반) · 탐험 올로스 은혜 +1(무기 세트 — 세트 포인트는 일반 한도에도 든다) — 빌드 코드에
	//   안 남고 PoB QuestRewards 에도 없다. 실빌드 60개: 일반 넘침 최대 2 · 세트 넘침 최대 1(전직 추가 포인트 뺀 값) — 그만큼은 빨강이 아니라 "엔드게임 +N".
	function pointSummary(shared: number, set1: number, set2: number, asc: number, statLines: string[] = []) {
		const ENDGAME_PASSIVE_POINTS = 2;
		const ENDGAME_SET_POINTS = 1;
		let extra = 0;
		let setExtra = 0;
		for (const line of statLines) {
			const g = /^Grants (\d+) Passive Skill Points?$/i.exec(line);
			if (g) extra += Number(g[1]);
			const w = /^(\d+) Passive Skill Points become Weapon Set Skill Points$/i.exec(line);
			if (w) setExtra += Number(w[1]);
		}
		const normal = shared + Math.max(set1, set2);
		const normalMax = 99 + 24 + extra;
		const endgame = Math.min(ENDGAME_PASSIVE_POINTS, Math.max(0, normal - normalMax));
		return { normal, normalMax, endgame, normalOver: normal > normalMax + ENDGAME_PASSIVE_POINTS, normalEndgame: ENDGAME_PASSIVE_POINTS, setMax: 24 + setExtra, setEndgame: ENDGAME_SET_POINTS, ascMax: 8, asc };
	}

	/** 같은 갈래의 다른 전직 선택지(C65, PoE1 choiceSiblings 짝) — PoE2 데이터엔 갈래 부모 표시가 없어, 선택지가 아닌 이웃 중 선택지 이웃이 둘 이상인 것을 부모로 본다. */
	function choiceSiblingsOf(id: number, adjOf: (id: number) => number[], isOption: (id: number) => boolean): number[] {
		if (!isOption(id)) return [];
		const out: number[] = [];
		for (const parent of adjOf(id)) {
			if (isOption(parent)) continue;
			const opts = adjOf(parent).filter((x) => isOption(x));
			if (opts.length < 2) continue;
			for (const o of opts) if (o !== id && !out.includes(o)) out.push(o);
		}
		return out;
	}
	/** 잠금 조건(PoB PassiveSpec unlockConstraint, C50) — 조건 노드가 모두 찍혀 있어야 이 노드로 길이 난다. 하나라도 빠지면 같이 빠진다. */
	function unlockMet(n: { unlockConstraint?: number[] }, isAllocated: (id: number) => boolean): boolean {
		return !n.unlockConstraint || n.unlockConstraint.every((id) => isAllocated(id));
	}

	function isSmallPassiveNode(n: { kind: string; ascendancy: string | null }): boolean {
		return n.kind === "normal" && !n.ascendancy;
	}
	/** 문장들의 "N% increased effect of Small Passive Skills" 합(타이탄 육중한 형체 50). */
	function smallPassiveIncOf(lines: string[]): number {
		let inc = 0;
		for (const s of lines) {
			const m = /(\d+(?:\.\d+)?)% increased effect of Small Passive Skills/i.exec(s);
			if (m) inc += parseFloat(m[1]);
		}
		return inc;
	}
	/** 뒤얽힌 현실 — 할당한 핵심 노드(keys) 중 하나라도 반경 안이면 연결 없이 찍히는 노드인가(주요 · 일반 · 능력치, 전직 아님). */
	// 주얼 "From Nothing"(Passives in Radius of <핵심> can be Allocated without being connected) — 그 핵심 노드 반경 안 노드(PoB PassiveSpec:
	//   직업 시작 · 주얼 칸 · 전직이 아니면 핵심 노드도 됨, 핵심 자신은 빼고). 핵심 노드를 찍지 않아도 된다(오라클 규칙과 다름, 10-03 C40)
	function inFromNothingRadius(n: { id: number; kind: string; ascendancy: string | null; x: number; y: number }, key: { id: number; x: number; y: number }, radius: number): boolean {
		if (n.ascendancy || n.kind === "classStart" || n.kind === "jewel" || n.id === key.id) return false;
		return (n.x - key.x) ** 2 + (n.y - key.y) ** 2 <= radius * radius;
	}
	// 주얼 칸 둘레 고리(안 ≤ 거리 ≤ 바깥) — 직업 시작 · 주얼 칸 · 전직 제외(PoB PassiveSpec intuitiveLeapLike, C47)
	function inLeapRing(n: { id: number; kind: string; ascendancy: string | null; x: number; y: number }, socket: { id: number; x: number; y: number }, inner: number, outer: number): boolean {
		if (n.ascendancy || n.kind === "classStart" || n.kind === "jewel" || n.id === socket.id) return false;
		const d = (n.x - socket.x) ** 2 + (n.y - socket.y) ** 2;
		return d >= inner * inner && d <= outer * outer;
	}
	function inLeapRadius(n: { kind: string; ascendancy: string | null; x: number; y: number }, keys: { x: number; y: number }[], radius: number): boolean {
		if (n.ascendancy || !(n.kind === "normal" || n.kind === "notable" || n.kind === "attribute")) return false;
		return keys.some((k) => (n.x - k.x) ** 2 + (n.y - k.y) ** 2 <= radius * radius);
	}
	/** "Can Allocate Passive Skills from the X's starting point" 문장들 → 직업 이름들(패스파인더 소서리스의 길). */
	function altStartClassNames(lines: string[]): string[] {
		const out: string[] = [];
		for (const t of lines) {
			const m = /Can Allocate Passive(?: Skill)?s from the (\w+)'s starting point/i.exec(t);
			if (m) out.push(m[1]);
		}
		return out;
	}
	function nodeLines(n: TreeNode): string[] {
		const pick = attrPick.get(n.id);
		const o = pick && n.options ? n.options[pick - 1] : undefined;
		if (o) return (isKorean && o.statsKo && o.statsKo.length ? o.statsKo : o.stats) || [];
		return (isKorean && n.statsKo && n.statsKo.length ? n.statsKo : n.stats) || [];
	}
	let matches = new Set<number>();
	let matchOrder: number[] = [];
	let matchCursor = -1;
	let hovered: TreeNode | null = null;
	// 꽂은 고유 주얼(10-03 C73, PoE1 jewelPicks 짝) — 주얼 칸 → slug. 해시 j= · 트리 계산 폼 jewels 로 실려 간다. 칸을 풀면 같이 빠진다
	const jewelPicks = new Map<number, string>();
	// 키워드(data-kw = [{t:용어, d:정의}], 이미 로케일에 맞춘 말 — Poe2View.keywordData, 10-04 C126). 변형의 null(속성 없음) = 고유 것을 쓴다
	type JewelKw = { t: string; d: string };
	const parseKw = (raw: string | undefined): JewelKw[] | null => {
		if (raw === undefined || raw === "") return null;
		try {
			return JSON.parse(raw) as JewelKw[];
		} catch {
			return null;
		}
	};
	type JewelVariant = { index: number; name: string; nameKo: string; explicits: string[]; explicitsKo: string[]; kw: JewelKw[] | null };
	const jewelDefs = new Map<string, { name: string; nameKo: string; explicits: string[]; explicitsKo: string[]; radius: string; variants: JewelVariant[]; kw: JewelKw[] | null }>();
	for (const o of Array.from(document.querySelectorAll<HTMLOptionElement>("#poe2TreeJewelList option"))) {
		jewelDefs.set(o.value, { name: o.dataset.name || o.value, nameKo: o.dataset.nameKo || o.dataset.name || o.value, explicits: (o.dataset.explicits || "").split("\n").filter(Boolean), explicitsKo: (o.dataset.explicitsKo || "").split("\n").filter(Boolean), radius: o.dataset.radius || "", variants: [], kw: parseKw(o.dataset.kw) });
	}
	for (const sp of Array.from(document.querySelectorAll<HTMLElement>("#poe2TreeJewelVariants span"))) {
		jewelDefs.get(sp.dataset.slug || "")?.variants.push({ index: Number(sp.dataset.index), name: sp.dataset.name || "", nameKo: sp.dataset.nameKo || sp.dataset.name || "", explicits: (sp.dataset.explicits || "").split("\n").filter(Boolean), explicitsKo: (sp.dataset.explicitsKo || "").split("\n").filter(Boolean), kw: parseKw(sp.dataset.kw) });
	}
	// 꽂은 값 = "slug" 또는 "slug:변형번호"(C74)
	const splitPick = (v: string): [string, number | null] => { const i = v.lastIndexOf(":"); const n = i > 0 ? Number(v.slice(i + 1)) : NaN; return i > 0 && Number.isFinite(n) ? [v.slice(0, i), n] : [v, null]; };
	const pickVariant = (v: string) => { const [slug, idx] = splitPick(v); return idx === null ? undefined : jewelDefs.get(slug)?.variants.find((x) => x.index === idx); };
	const jewelLabel = (v: string) => { const [slug] = splitPick(v); const d = jewelDefs.get(slug); const vr = pickVariant(v); return d ? t(d.nameKo, d.name) + (vr ? " — " + t(vr.nameKo, vr.name) : "") : v; };
	// 꽂은 주얼의 연결 없이 찍기(C74, PoB ModParser fromNothingKeystone · intuitiveLeapLike) — 빌드 링크의 l= · r= 와 같은 규칙
	const JEWEL_RADIUS_PX: Record<string, number> = { Small: 1200, Medium: 1380, Large: 1560, "Very Large": 1800 };
	const JEWEL_RING_PX: Record<string, [number, number]> = { "very small": [780, 1140], small: [960, 1320], "medium-small": [1140, 1500], medium: [1320, 1680], "medium-large": [1500, 1860], large: [1680, 2040], "very large": [1980, 2340], massive: [2160, 2520] };
	function pickedLeap(): { keys: { id: number; r: number }[]; rings: { id: number; inner: number; outer: number }[] } {
		const keys: { id: number; r: number }[] = [];
		const rings: { id: number; inner: number; outer: number }[] = [];
		for (const [socket, v] of jewelPicks) {
			const [slug] = splitPick(v);
			const d = jewelDefs.get(slug);
			if (!d) continue;
			const lines = (pickVariant(v)?.explicits || d.explicits).join(" ");
			const outer = JEWEL_RADIUS_PX[d.radius] || 0;
			const fn = /Passives in radius of ([A-Za-z' ]+?) can be Allocated without being connected/i.exec(lines);
			if (fn && outer) {
				const key = [...nodes.values()].find((n) => n.kind === "keystone" && n.name.toLowerCase() === fn[1].trim().toLowerCase());
				if (key) keys.push({ id: key.id, r: outer });
				continue;
			}
			if (/Passives in Radius can be Allocated without being connected/i.test(lines)) {
				const ring = /affects Passives in ([A-Za-z -]+?) Ring/i.exec(lines);
				const rr = ring ? JEWEL_RING_PX[ring[1].trim().toLowerCase()] : outer ? ([0, outer] as [number, number]) : undefined;
				if (rr) rings.push({ id: socket, inner: rr[0], outer: rr[1] });
			}
		}
		return { keys, rings };
	}
	/**
	 * 꽂은 주얼의 실제 반경 고리(10-04 C90, PoE1 "꽂은 주얼은 그 반경만" 짝) — 찍힌 칸의 주얼만. From Nothing 은 고른 핵심 노드 둘레(인게임도 그 둘레),
	 *   Controlled Metamorphosis 는 칸 둘레 고리(안 · 바깥), 그 밖의 반경 주얼은 칸 둘레 원.
	 */
	function socketedRings(): { id: number; x: number; y: number; inner: number; outer: number }[] {
		const out: { id: number; x: number; y: number; inner: number; outer: number }[] = [];
		for (const [socket, v] of jewelPicks) {
			if (!allocated.has(socket)) continue;
			const [slug] = splitPick(v);
			const d = jewelDefs.get(slug);
			const sn = nodes.get(socket);
			if (!d || !sn) continue;
			const lines = (pickVariant(v)?.explicits || d.explicits).join(" ");
			const outer = JEWEL_RADIUS_PX[d.radius] || 0;
			const fn = /Passives in radius of ([A-Za-z' ]+?) can be Allocated without being connected/i.exec(lines);
			const key = fn ? [...nodes.values()].find((n) => n.kind === "keystone" && n.name.toLowerCase() === fn[1].trim().toLowerCase()) : undefined;
			if (key && outer) {
				out.push({ id: key.id, x: key.x, y: key.y, inner: 0, outer });
				continue;
			}
			const ring = /affects Passives in ([A-Za-z -]+?) Ring/i.exec(lines);
			const rr = ring ? JEWEL_RING_PX[ring[1].trim().toLowerCase()] : outer ? ([0, outer] as [number, number]) : undefined;
			if (rr) out.push({ id: socket, x: sn.x, y: sn.y, inner: rr[0], outer: rr[1] });
		}
		return out;
	}
	// 호버 미리보기(10-03 C70, PoE1 tree.ts hoverPath · removalSet 짝) — 안 찍은 노드: 찍으면 따라올 경로(금색 점선), 찍은 노드: 풀면 함께 빠질 노드(붉은 점선 · 테두리)
	const JEWEL_RADII2: { en: string; ko: string; r: number }[] = [
		{ en: "Small", ko: "작게", r: 1200 },
		{ en: "Medium", ko: "중간", r: 1380 },
		{ en: "Large", ko: "대형", r: 1560 },
		{ en: "Very Large", ko: "매우 넓은 반경", r: 1800 },
	];
	let hoverPathIds: number[] = [];
	const removalPreview = new Set<number>();
	function refreshPreview() {
		hoverPathIds = [];
		removalPreview.clear();
		const n = hovered;
		if (!n) return;
		if (allocated.has(n.id)) {
			// 실제로 풀어 보고(prune 포함) 되돌린다 — 해제 규칙(세트 · 연결 없이 찍기 · 잠금 조건)을 그대로 따른다
			const keepA = new Set(allocated), keepM = new Map(setMode);
			allocated.delete(n.id);
			setMode.delete(n.id);
			prune();
			for (const id of keepA) if (!allocated.has(id)) removalPreview.add(id);
			allocated.clear();
			keepA.forEach((id) => allocated.add(id));
			setMode.clear();
			keepM.forEach((v, k) => setMode.set(k, v));
		} else if (allocatable(n) && unlockMet(n, (id) => allocated.has(id))) {
			hoverPathIds = pathTo(n.id) || [];
		}
	}
	const isAtlas = () => data?.mode === "atlas";
	const treeOf = (key?: string) => (key ? data?.trees?.find((s) => s.key === key) : undefined);
	const treeLabel = (s?: SubTree) => (s ? (isKorean && s.nameKo ? s.nameKo : s.name) : "");

	// 보기 변환 — 화면 = (세계 - 중심) * scale + 캔버스 중앙
	let scale = 0.02;
	let cx = 0;
	let cy = 0;
	let dpr = Math.max(1, window.devicePixelRatio || 1);

	const RADIUS: { [k: string]: number } = {
		normal: 38, attribute: 34, notable: 58, keystone: 82, jewel: 50, classStart: 110, ascendancyStart: 60, mastery: 0,
	};
	const COLOR: { [k: string]: string } = {
		normal: "#8c8374", attribute: "#6f6f78", notable: "#c9a55a", keystone: "#e0bf6a", jewel: "#6aa8e0", classStart: "#b8b0a0", ascendancyStart: "#b58cd6",
	};

	function visible(n: TreeNode): boolean {
		if (n.kind === "mastery") return false;
		// 잠긴 노드는 조건이 채워질 때까지 안 그린다(PoB PassiveTreeView: 아이콘 · 연결선 · 검색 · 툴팁 모두 checkUnlockConstraints 뒤, 10-03 C68) —
		//   오라클 "보이지 않는 길" 뒤 숨은 길 ~197개가 모든 직업에 보이던 것. 이미 찍힌 노드는 그대로 보인다
		if (n.unlockConstraint && !allocated.has(n.id) && !unlockMet(n, (id) => allocated.has(id))) return false;
		if (n.ascendancy) return !!asc && n.ascendancy === asc.id;
		return true;
	}

	function toScreen(x: number, y: number): [number, number] {
		return [(x - cx) * scale + canvas!.width / 2, (y - cy) * scale + canvas!.height / 2];
	}
	function toWorld(sx: number, sy: number): [number, number] {
		return [(sx - canvas!.width / 2) / scale + cx, (sy - canvas!.height / 2) / scale + cy];
	}

	function resize() {
		dpr = Math.max(1, window.devicePixelRatio || 1);
		const rect = canvas!.getBoundingClientRect();
		canvas!.width = Math.round(rect.width * dpr);
		canvas!.height = Math.round(rect.height * dpr);
		draw();
	}

	/** 두 노드를 잇는 선 — 같은 그룹·같은 궤도면 그룹 중심 호, 연결에 궤도 값(±k)이 있으면 그 반지름의 호, 아니면 직선. */
	function strokeConnection(a: TreeNode, b: TreeNode, orbit: number) {
		const [ax, ay] = toScreen(a.x, a.y);
		const [bx, by] = toScreen(b.x, b.y);
		ctx.beginPath();
		const g = data!.groups[String(a.group)];
		if (orbit === 0 && a.group === b.group && a.orbit === b.orbit && a.orbit > 0 && g) {
			const [gx, gy] = toScreen(g.x, g.y);
			const r = data!.orbitRadii[a.orbit] * scale;
			let a1 = Math.atan2(ay - gy, ax - gx);
			let a2 = Math.atan2(by - gy, bx - gx);
			let d = a2 - a1;
			while (d > Math.PI) d -= 2 * Math.PI;
			while (d < -Math.PI) d += 2 * Math.PI;
			ctx.arc(gx, gy, r, a1, a1 + d, d < 0);
		} else if (orbit !== 0 && Math.abs(orbit) < data!.orbitRadii.length) {
			const r = data!.orbitRadii[Math.abs(orbit)] * scale;
			const dx = bx - ax, dy = by - ay;
			const dist = Math.hypot(dx, dy);
			if (r > dist / 2 + 0.01) {
				const h = Math.sqrt(r * r - (dist / 2) * (dist / 2));
				const px = -dy / dist, py = dx / dist;
				const sign = orbit > 0 ? 1 : -1;
				const ccx = (ax + bx) / 2 + px * h * sign;
				const ccy = (ay + by) / 2 + py * h * sign;
				let a1 = Math.atan2(ay - ccy, ax - ccx);
				let a2 = Math.atan2(by - ccy, bx - ccx);
				let d = a2 - a1;
				while (d > Math.PI) d -= 2 * Math.PI;
				while (d < -Math.PI) d += 2 * Math.PI;
				ctx.arc(ccx, ccy, r, a1, a1 + d, d < 0);
			} else {
				ctx.moveTo(ax, ay);
				ctx.lineTo(bx, by);
			}
		} else {
			ctx.moveTo(ax, ay);
			ctx.lineTo(bx, by);
		}
		ctx.stroke();
	}

	function draw() {
		if (!data) return;
		ctx.setTransform(1, 0, 0, 1, 0, 0);
		ctx.fillStyle = "#0e1116";
		ctx.fillRect(0, 0, canvas!.width, canvas!.height);
		const searching = matches.size > 0;
		// 연결선
		ctx.lineCap = "round";
		for (const n of nodes.values()) {
			if (!visible(n)) continue;
			for (const c of n.out) {
				const m = nodes.get(c.id);
				if (!m || !visible(m)) continue;
				// 전직 트리와 본 트리 사이 연결(직업 시작점 ↔ 전직 시작점)은 인게임에 선으로 보이지 않는다
				if (!!n.ascendancy !== !!m.ascendancy) continue;
				const on = (allocated.has(n.id) || granted.has(n.id)) && (allocated.has(m.id) || granted.has(m.id));
				ctx.strokeStyle = on ? "#e8c46a" : searching ? "rgba(120,110,95,0.25)" : "rgba(120,110,95,0.55)";
				ctx.lineWidth = Math.max(dpr, (on ? 16 : 10) * scale); // scale 은 이미 기기 픽셀 기준
				strokeConnection(n, m, c.orbit);
			}
		}
		// 호버 미리보기 점선(C70) — 경로: 금색, 해제: 붉은색(PoE1 과 같은 색)
		const dashLine = (ids: Set<number>, color: string, width: number, needBoth: boolean) => {
			ctx.save();
			ctx.setLineDash([Math.max(2 * dpr, 10 * scale), Math.max(2 * dpr, 8 * scale)]);
			ctx.strokeStyle = color;
			ctx.lineWidth = Math.max(dpr, width * scale);
			for (const id of ids) {
				const a = nodes.get(id);
				if (!a) continue;
				for (const nb of a.adj) {
					const b = nodes.get(nb);
					if (!b || !visible(b) || !!a.ascendancy !== !!b.ascendancy) continue;
					const other = ids.has(nb) || (!needBoth && (allocated.has(nb) || granted.has(nb) || startIds().includes(nb)));
					if (!other || (ids.has(nb) && nb < id)) continue;
					const [x1, y1] = toScreen(a.x, a.y), [x2, y2] = toScreen(b.x, b.y);
					ctx.beginPath();
					ctx.moveTo(x1, y1);
					ctx.lineTo(x2, y2);
					ctx.stroke();
				}
			}
			ctx.restore();
		};
		// 주얼 칸 반경 고리(C71, PoE1 JEWEL_RADII 짝) — 주얼 칸에 마우스를 올리면 반경별 원과 이름(게임 문구 ClientStrings JewelRadius*)
		//   PoE2 반경 = PoB jewelRadii 바깥 × 1.2(작게 1200 · 중간 1380 · 대형 1560 · 매우 넓은 반경 1800)
		if (hovered && hovered.kind === "jewel" && !hovered.ascendancy && !isAtlas()) {
			const [hx, hy] = toScreen(hovered.x, hovered.y);
			ctx.save();
			ctx.lineWidth = Math.max(dpr, 1.5 * dpr);
			ctx.strokeStyle = "rgba(110,231,183,0.55)";
			ctx.fillStyle = "rgba(110,231,183,0.85)";
			ctx.font = `${Math.round(12 * dpr)}px sans-serif`;
			ctx.textAlign = "center";
			// 반경이 가까워(1200~1800) 이름을 모두 꼭대기에 두면 겹친다(첫 스크린샷) — 위쪽 호를 따라 각도를 나눠 놓는다
			JEWEL_RADII2.forEach((band, i) => {
				ctx.beginPath();
				ctx.arc(hx, hy, band.r * scale, 0, Math.PI * 2);
				ctx.stroke();
				const a = ((-135 + i * 30) * Math.PI) / 180;
				ctx.fillText(t(band.ko, band.en), hx + Math.cos(a) * band.r * scale, hy + Math.sin(a) * band.r * scale - 4 * dpr);
			});
			ctx.restore();
		}
		// 꽂은 주얼의 실제 반경(C90) — 계획용 호버 고리(위)보다 흐린 실선, 탐침용으로 "중심 노드:바깥 반경"을 캔버스 속성에
		const rings = isAtlas() ? [] : socketedRings();
		if (rings.length) {
			ctx.save();
			ctx.lineWidth = Math.max(dpr, 2 * dpr);
			ctx.strokeStyle = "rgba(110,231,183,0.45)";
			for (const g of rings) {
				const [cx, cy] = toScreen(g.x, g.y);
				for (const r of g.inner > 0 ? [g.inner, g.outer] : [g.outer]) {
					ctx.beginPath();
					ctx.arc(cx, cy, r * scale, 0, Math.PI * 2);
					ctx.stroke();
				}
			}
			ctx.restore();
		}
		canvas!.dataset.jewelRings = rings.map((g) => g.id + ":" + g.outer).join(",");
		if (hoverPathIds.length) dashLine(new Set(hoverPathIds), "rgba(240,208,137,0.7)", 14, false);
		if (removalPreview.size > 1) dashLine(removalPreview, "rgba(232,110,96,0.85)", 16, true);
		// 노드
		for (const n of nodes.values()) {
			if (!visible(n)) continue;
			const [sx, sy] = toScreen(n.x, n.y);
			if (sx < -60 || sy < -60 || sx > canvas!.width + 60 || sy > canvas!.height + 60) continue;
			const r = Math.max(2 * dpr, (RADIUS[n.kind] || 36) * scale);
			const on = allocated.has(n.id) || granted.has(n.id);
			const hit = matches.has(n.id);
			ctx.beginPath();
			ctx.arc(sx, sy, r, 0, Math.PI * 2);
			ctx.fillStyle = on ? "#f0d488" : COLOR[n.kind] || "#8c8374";
			ctx.globalAlpha = searching && !hit && !on ? 0.3 : 1;
			ctx.fill();
			// 충분히 크게 보일 때만 아이콘(원 안에 잘라서) — 할당 안 된 노드는 인게임처럼 어둡게
			// 능력치를 고른 노드는 인게임처럼 그 능력치 아이콘(힘/민첩/지능)으로 그린다(10-01)
			const pickedOpt = on && attrPick.get(n.id) && n.options ? n.options[attrPick.get(n.id)! - 1] : undefined;
			const iconKey = pickedOpt?.icon || (n as any).icon;
			const sp = iconKey ? sprites[iconKey] : undefined;
			const img = sp ? sheets.get(sp.s) : undefined;
			if (sp && img && img.complete && img.naturalWidth && r >= 7 * dpr) {
				ctx.save();
				ctx.beginPath();
				ctx.arc(sx, sy, r * 0.94, 0, Math.PI * 2);
				ctx.clip();
				ctx.globalAlpha = on ? 1 : searching && !hit ? 0.25 : 0.62;
				ctx.drawImage(img, sp.x, sp.y, sp.w, sp.h, sx - r, sy - r, r * 2, r * 2);
				ctx.restore();
			}
			ctx.globalAlpha = 1;
			if (n.kind === "keystone" || n.kind === "notable" || n.kind === "classStart" || n.kind === "jewel") {
				ctx.lineWidth = Math.max(dpr, 6 * scale);
				ctx.strokeStyle = on ? "#fff3c4" : "rgba(0,0,0,0.6)";
				ctx.stroke();
			}
			const setColor = on ? SET_COLOR[modeOf(n.id)] : "";
			if (setColor) {
				// 세트 전용 노드 — 인게임처럼 세트 색 테두리(세트 I 주황 · 세트 II 하늘)
				ctx.beginPath();
				ctx.arc(sx, sy, r + 5 * dpr, 0, Math.PI * 2);
				ctx.lineWidth = 2.5 * dpr;
				ctx.strokeStyle = setColor;
				ctx.stroke();
			}
			const pickColor = on ? ATTR_COLOR[attrPick.get(n.id) || 0] : "";
			if (pickColor) {
				ctx.beginPath();
				ctx.arc(sx, sy, r + 2 * dpr, 0, Math.PI * 2);
				ctx.lineWidth = 3 * dpr;
				ctx.strokeStyle = pickColor;
				ctx.stroke();
			}
			if (hit) {
				ctx.beginPath();
				ctx.arc(sx, sy, r + 6 * dpr, 0, Math.PI * 2);
				ctx.lineWidth = 2.5 * dpr;
				ctx.strokeStyle = "#5fd3ff";
				ctx.stroke();
			}
			if (klass && n.id === klass.startNodeId) {
				ctx.beginPath();
				ctx.arc(sx, sy, r + 10 * dpr, 0, Math.PI * 2);
				ctx.lineWidth = 3 * dpr;
				ctx.strokeStyle = "#e8c46a";
				ctx.stroke();
			}
			if (jewelPicks.has(n.id)) {
				// 꽂은 주얼 — 칸 안쪽을 주얼 색 원으로(C73)
				ctx.beginPath();
				ctx.arc(sx, sy, r * 0.55, 0, Math.PI * 2);
				ctx.fillStyle = "#6ee7b7";
				ctx.fill();
			}
			if (removalPreview.has(n.id)) {
				// 해제 대상 — 붉은 테두리(PoE1 과 같은 색)
				ctx.beginPath();
				ctx.arc(sx, sy, r + 7 * dpr, 0, Math.PI * 2);
				ctx.lineWidth = 3 * dpr;
				ctx.strokeStyle = "#e86e60";
				ctx.stroke();
			}
			if (hovered && hovered.id === n.id) {
				ctx.beginPath();
				ctx.arc(sx, sy, r + 3 * dpr, 0, Math.PI * 2);
				ctx.lineWidth = 2 * dpr;
				ctx.strokeStyle = "#ffffff";
				ctx.stroke();
			}
		}
		// 확대가 충분하면 특화·핵심 이름
		if (scale / dpr > 0.09) {
			ctx.font = `${Math.round(12 * dpr)}px sans-serif`;
			ctx.textAlign = "center";
			ctx.fillStyle = "rgba(230,220,200,0.85)";
			for (const n of nodes.values()) {
				if (!visible(n) || (n.kind !== "notable" && n.kind !== "keystone")) continue;
				const [sx, sy] = toScreen(n.x, n.y);
				if (sx < 0 || sy < 0 || sx > canvas!.width || sy > canvas!.height) continue;
				ctx.fillText(isKorean && n.nameKo ? n.nameKo : n.name, sx, sy + (RADIUS[n.kind] * scale) + 14 * dpr);
			}
		}
	}

	// ── 할당 ──
	function startIds(): number[] {
		const out: number[] = [];
		if (isAtlas()) return (data?.trees || []).map((s) => s.rootId);
		if (klass) out.push(klass.startNodeId);
		if (asc && asc.startNodeId) out.push(asc.startNodeId);
		for (const id of granted) out.push(id);
		// 다른 직업 시작점(10-02) — 패스파인더 "소서리스의 길"(Can Allocate Passive Skills from the Sorceress's starting point)을 찍으면
		//   그 직업 시작점에서도 찍는다(인게임 규칙). PoB-PoE2 는 같은 문구를 주얼에서만 출발점으로 쓰지만, 불러온 노드는 연결과 무관하게 계산하므로 엔진 수치와 어긋나지 않는다.
		for (const id of allocated) {
			const n = nodes.get(id);
			if (!n?.ascendancy) continue;
			for (const name of altStartClassNames(n.stats || [])) {
				const other = data?.classes.find((c) => c.name.toLowerCase() === name.toLowerCase());
				if (other && !out.includes(other.startNodeId)) out.push(other.startNodeId);
			}
		}
		return out;
	}
	function allocatable(n: TreeNode): boolean {
		if (!visible(n)) return false;
		if (n.kind === "classStart") return false; // 시작점은 공짜(점수 없음) — 다른 직업 시작점은 지나갈 수 없다
		return true;
	}
	/** 할당된 집합(+시작점)에서 target 까지 가장 짧은 경로(BFS). 없으면 null. */
	// 연결 없이 찍기(10-02) — 오라클 "뒤얽힌 현실": 할당한 핵심 노드의 중간 반경 안 비-핵심 노드는 트리와 이어지지 않아도 찍힌다.
	//   PoB-PoE2 PassiveSpec(intuitiveLeapLikeNodes · AllocateFromNodeRadius { from = Keystone, radiusIndex = 2, to = Notable · Normal }) —
	//   반경 2 = "Medium" 바깥 1150(data.jewelRadius), 대상 = 주요 · 일반(능력치 노드도 Normal). 전직 · 주얼 칸 · 핵심은 아님.
	const LEAP_RE = /Medium Radius of allocated Keystone Passive Skills can be allocated without being connected/i;
	// PoB 는 반경에 PassiveTreeJewelDistanceMultiplier 1.2 를 곱해 잰다(Data.lua outerSquared · Misc.lua) — 예전엔 1150 그대로라 17% 좁았다(10-03 C40)
	const LEAP_RADIUS = 1150 * 1.2;
	function leapRoots(): Set<number> {
		const out = new Set<number>();
		if (isAtlas()) return out;
		const picked = pickedLeap();
		for (const g of [...leapRings, ...picked.rings]) {
			const socket = nodes.get(g.id);
			if (!socket || !allocated.has(g.id)) continue; // PoB: 주얼 칸이 찍혀 있어야
			for (const n of nodes.values()) if (inLeapRing(n, socket, g.inner, g.outer)) out.add(n.id);
		}
		for (const f of [...fromNothing, ...picked.keys]) {
			const key = nodes.get(f.id);
			if (!key) continue;
			for (const n of nodes.values()) if (inFromNothingRadius(n, key, f.r)) out.add(n.id);
		}
		const on = [...allocated].some((id) => {
			const n = nodes.get(id);
			return !!n?.ascendancy && (n.stats || []).some((t) => LEAP_RE.test(t));
		});
		if (!on) return out;
		const keys = [...allocated].map((id) => nodes.get(id)).filter((n): n is TreeNode => !!n && n.kind === "keystone");
		for (const n of nodes.values()) {
			if (inLeapRadius(n, keys, LEAP_RADIUS)) out.add(n.id);
		}
		return out;
	}
	function pathTo(target: number): number[] | null {
		// 지금 모드로 지나갈 수 있는 할당 노드만 출발점 — 다른 세트 노드는 막힌 길
		const passable = (id: number) => modeOf(id) === 0 || (curSet > 0 && modeOf(id) === curSet);
		const from = new Set<number>([...startIds(), ...[...allocated].filter(passable)]);
		if (from.has(target)) return [];
		if (leapRoots().has(target)) return [target]; // 핵심 노드 반경 안 — 그 노드만
		const prev = new Map<number, number>();
		const queue: number[] = [];
		for (const s of from) {
			prev.set(s, -1);
			queue.push(s);
		}
		while (queue.length) {
			const cur = queue.shift()!;
			const n = nodes.get(cur);
			if (!n) continue;
			for (const nb of n.adj) {
				if (prev.has(nb)) continue;
				const m = nodes.get(nb);
				if (!m || !allocatable(m)) continue;
				if (!unlockMet(m, (id) => allocated.has(id))) continue; // 잠긴 노드로는 못 간다(C50)
				if (allocated.has(nb) && !passable(nb)) continue;
				prev.set(nb, cur);
				if (nb === target) {
					const path: number[] = [];
					let at = nb;
					while (at !== -1 && !from.has(at)) {
						path.push(at);
						at = prev.get(at)!;
					}
					return path;
				}
				queue.push(nb);
			}
		}
		return null;
	}
	/** 시작점에서 더는 닿지 않는 할당 노드를 걷어 낸다(노드 해제 뒤). */
	function prune() {
		// 공용 노드는 시작점에서 공용 노드로만, 세트 k 노드는 공용 + 세트 k 노드로 이어져야 남는다(PoB FindStartFromNode 와 같은 규칙)
		const walk = (seed: Set<number>, ok: (id: number) => boolean) => {
			const reach = new Set<number>(seed);
			const queue = [...reach];
			while (queue.length) {
				const cur = queue.shift()!;
				for (const nb of nodes.get(cur)?.adj || []) {
					if (allocated.has(nb) && !reach.has(nb) && ok(nb)) {
						reach.add(nb);
						queue.push(nb);
					}
				}
			}
			return reach;
		};
		// 연결 없이 찍힌 노드(뒤얽힌 현실)는 그 자체가 출발점 — 핵심 노드나 전직 노드를 빼면 그 반경이 사라지니, 바뀌지 않을 때까지 되풀이
		for (let changed = true; changed; ) {
			changed = false;
			const leap = [...leapRoots()].filter((id) => allocated.has(id));
			const seedOf = (k: number) => new Set<number>([...startIds(), ...leap.filter((id) => modeOf(id) === k)]);
			const reach0 = walk(seedOf(0), (id) => modeOf(id) === 0);
			const reach1 = walk(new Set<number>([...reach0, ...seedOf(1)]), (id) => modeOf(id) === 0 || modeOf(id) === 1);
			const reach2 = walk(new Set<number>([...reach0, ...seedOf(2)]), (id) => modeOf(id) === 0 || modeOf(id) === 2);
			for (const id of [...allocated]) {
				const m = modeOf(id);
				// 잠금 조건이 깨진 노드도 걷는다(PoB: 조건 노드의 depends 에 들어 같이 해제, C50)
				const node = nodes.get(id);
				const keep = (m === 0 ? reach0.has(id) : m === 1 ? reach1.has(id) : reach2.has(id)) && (!node || unlockMet(node, (x) => allocated.has(x)));
				if (!keep) {
					allocated.delete(id);
					setMode.delete(id);
					changed = true;
				}
			}
		}
	}
	function toggle(n: TreeNode) {
		if (!klass && !isAtlas()) {
			flash(t("먼저 직업을 고르세요", "Pick a class first"));
			return;
		}
		if (!allocatable(n)) return;
		const before = snapshot();
		if (allocated.has(n.id)) {
			allocated.delete(n.id);
			setMode.delete(n.id);
			prune();
		} else {
			if (!unlockMet(n, (id) => allocated.has(id))) {
				// 잠긴 노드(C50) — 무엇을 먼저 찍어야 하는지 이름으로
				const need = (n.unlockConstraint || []).filter((id) => !allocated.has(id)).map((id) => { const c = nodes.get(id); return c ? t(c.nameKo || c.name, c.name) : String(id); });
				flash(t(`먼저 찍어야 합니다: ${need.join(", ")}`, `Allocate these first: ${need.join(", ")}`));
				return;
			}
			// 갈래 선택지는 하나만 — 이미 고른 형제와 바꾼다(그 아래 이어진 노드는 prune 이 함께 걷는다, C65)
			const sibs = choiceSiblingsOf(n.id, (id) => nodes.get(id)?.adj || [], (id) => !!nodes.get(id)?.multipleChoiceOption).filter((id) => allocated.has(id));
			if (sibs.length) {
				for (const id of sibs) {
					allocated.delete(id);
					setMode.delete(id);
				}
				prune();
			}
			const path = pathTo(n.id);
			if (path === null) {
				if (sibs.length) restore(before); // 못 찍으면 형제를 되돌린다
				flash(t("시작점에서 이어지지 않는 노드입니다", "Not connected to your start"));
				return;
			}
			for (const id of path) {
				allocated.add(id);
				const pn = nodes.get(id);
				if (curSet > 0 && pn && !forcedShared(pn)) setMode.set(id, curSet);
			}
		}
		afterChange();
		record(before);
	}
	function afterChange() {
		for (const id of [...jewelPicks.keys()]) if (!allocated.has(id)) jewelPicks.delete(id); // 안 찍힌 칸의 주얼은 효과가 없다(PoE1 과 같다)
		refreshPreview(); // 찍고 난 뒤에도 커서는 그 노드 위 — 미리보기를 새 상태로
		for (const id of [...attrPick.keys()]) if (!allocated.has(id)) attrPick.delete(id);
		if (isAtlas()) {
			// 하위 트리별 점수 — 인게임 아틀라스도 트리마다 포인트가 따로다
			const per = new Map<string, number>();
			for (const id of allocated) {
				const k = nodes.get(id)?.tree;
				if (k) per.set(k, (per.get(k) || 0) + 1);
			}
			const parts = (data?.trees || []).filter((s) => per.get(s.key)).map((s) => `${treeLabel(s)} ${per.get(s.key)}`);
			if (points) points.textContent = parts.length ? parts.join(" · ") : t("할당 0점", "0 allocated");
			writeHash();
			draw();
			updateStatsPanel();
			return;
		}
		for (const id of [...setMode.keys()]) if (!allocated.has(id)) setMode.delete(id);
		let main = 0, ascPts = 0;
		const setPts = [0, 0, 0];
		for (const id of allocated) {
			const n = nodes.get(id);
			if (!n) continue;
			if (n.ascendancy) {
				if (n.kind !== "ascendancyStart" && !n.multipleChoiceOption && !n.freeAllocate) ascPts++;
			} else if (modeOf(id) > 0) setPts[modeOf(id)]++;
			else main++;
		}
		// 한도까지 PoE1 트리("포인트 N / 123 · 전직 N / 8")처럼, 넘으면 빨강(PoB Build.lua pointDisplay 와 같은 규칙, 10-03 C38)
		const statLines: string[] = [];
		for (const id of allocated) statLines.push(...(nodes.get(id)?.stats || []));
		const sum = pointSummary(main, setPts[1], setPts[2], ascPts, statLines);
		const el = points;
		if (el) {
			el.replaceChildren();
			// allow = 빌드 코드에 없는 엔드게임 포인트(C49) — 그만큼 넘은 건 빨강 대신 흐린 "(엔드게임 +N)"
			const add = (label: string, used: number, max: number, allow = 0, why = "") => {
				const span = document.createElement("span");
				span.className = used > max + allow ? "text-error font-bold" : "";
				span.textContent = `${label} ${used} / ${max}`;
				el.appendChild(span);
				if (used > max && used <= max + allow) {
					const note = document.createElement("span");
					note.className = "text-base-content/60";
					note.textContent = t(` (엔드게임 +${used - max})`, ` (endgame +${used - max})`);
					note.title = why;
					el.appendChild(note);
				}
			};
			add(t("패시브", "Passives"), sum.normal, sum.normalMax, sum.normalEndgame, t("엔드게임에서 얻는 패시브 스킬 포인트(최대 2)는 빌드 코드에 남지 않습니다", "Passive Skill Points from the endgame (up to 2) are not stored in build codes"));
			if (setPts[1] || setPts[2]) {
				const setWhy = t("엔드게임에서 얻는 무기 세트 패시브 스킬 포인트(최대 1)는 빌드 코드에 남지 않습니다", "Weapon Set Passive Skill Points from the endgame (up to 1) are not stored in build codes");
				add(t(" · 세트 I", " · Set I"), setPts[1], sum.setMax, sum.setEndgame, setWhy);
				add(t(" · 세트 II", " · Set II"), setPts[2], sum.setMax, sum.setEndgame, setWhy);
			}
			add(t(" · 전직", " · Ascendancy"), ascPts, sum.ascMax);
		}
		writeHash();
		draw();
		updateStatsPanel();
		syncEvalForm();
	}
	function flash(msg: string) {
		if (!status) return;
		status.textContent = msg;
		window.setTimeout(() => {
			if (status.textContent === msg) status.textContent = "";
		}, 2500);
	}

	// ── 주소(#c=직업&a=전직&n=노드,…) ──
	function writeHash() {
		const p = new URLSearchParams();
		if (klass) p.set("c", klass.name);
		if (asc) p.set("a", asc.id);
		if (allocated.size) p.set("n", [...allocated].sort((a, b) => a - b).join(","));
		if (granted.size) p.set("g", [...granted].sort((a, b) => a - b).join(","));
		if (fromNothing.length) p.set("l", fromNothing.map((f) => f.id + ":" + f.r).join(","));
		if (leapRings.length) p.set("r", leapRings.map((g) => g.id + ":" + g.inner + ":" + g.outer).join(","));
		if (jewelPicks.size) p.set("j", [...jewelPicks].sort((a, b) => a[0] - b[0]).map(([id, slug]) => id + ":" + slug).join(","));
		if (attrPick.size) p.set("s", [...attrPick].sort((a, b) => a[0] - b[0]).map(([id, v]) => id + ":" + v).join(","));
		for (const k of [1, 2]) {
			const ids = [...setMode].filter(([, v]) => v === k).map(([id]) => id).sort((a, b) => a - b);
			if (ids.length) p.set("w" + k, ids.join(","));
		}
		const h = p.toString();
		history.replaceState(null, "", location.pathname + location.search + (h ? "#" + h : ""));
	}
	function readHash() {
		const p = new URLSearchParams(location.hash.slice(1));
		const c = p.get("c");
		if (c) setClass(c, false);
		const a = p.get("a");
		if (a) setAsc(a, false);
		leapRings = (p.get("r") || "").split(",").map((x) => x.split(":").map(Number)).filter(([id, a, b]) => nodes.has(id) && b > 0 && a >= 0).map(([id, inner, outer]) => ({ id, inner, outer }));
		jewelPicks.clear();
		for (const pair of (p.get("j") || "").split(",")) {
			const sep = pair.indexOf(":");
			const id = Number(pair.slice(0, sep));
			const slug = pair.slice(sep + 1);
			if (sep > 0 && nodes.get(id)?.kind === "jewel" && jewelDefs.has(splitPick(slug)[0])) jewelPicks.set(id, slug);
		}
		fromNothing = (p.get("l") || "").split(",").map((x) => x.split(":").map(Number)).filter(([id, r]) => nodes.has(id) && r > 0).map(([id, r]) => ({ id, r }));
		granted.clear();
		for (const s of (p.get("g") || "").split(",")) {
			const n = nodes.get(Number(s));
			if (n && !n.ascendancy) granted.add(n.id);
		}
		for (const s of (p.get("n") || "").split(",")) {
			const id = Number(s);
			if (granted.has(id)) continue; // 장비가 준 노드는 할당(점수)이 아니다
			const n = nodes.get(id);
			// PoB 빌드의 nodes 에는 시작점(직업·전직)도 들어 있다 — 시작점은 점수가 아니라 뺀다
			if (n && n.kind !== "classStart" && n.kind !== "ascendancyStart") allocated.add(id);
		}
		// 세트 전용 노드(w1·w2) — 빌드 주소에 실려 온다. 공용이어야 하는 노드(핵심·주얼 칸·전직)는 무시
		for (const k of [1, 2]) {
			for (const s of (p.get("w" + k) || "").split(",")) {
				const id = Number(s);
				const n = nodes.get(id);
				if (n && allocated.has(id) && !forcedShared(n)) setMode.set(id, k);
			}
		}
		prune();
		for (const s of (p.get("s") || "").split(",")) {
			const [id, v] = s.split(":").map(Number);
			if (allocated.has(id) && nodes.get(id)?.options?.length && v >= 1 && v <= 3) attrPick.set(id, v);
		}
	}

	// ── 직업/전직 ──
	function fillClassSelect() {
		if (!classSel || !data) return;
		classSel.replaceChildren(new Option(t("직업 선택", "Class"), ""));
		for (const k of data.classes) classSel.add(new Option(isKorean && k.nameKo ? k.nameKo : k.name, k.name));
	}
	function fillAscSelect() {
		if (!ascSel) return;
		ascSel.replaceChildren(new Option(t("전직 없음", "No ascendancy"), ""));
		for (const a of klass?.ascendancies || []) ascSel.add(new Option(isKorean && a.nameKo ? a.nameKo : a.name, a.id));
		ascSel.disabled = !klass;
	}
	function setClass(name: string, focus = true) {
		const k = data?.classes.find((c) => c.name === name) || null;
		if (k !== klass) {
			allocated.clear();
			granted.clear();
			fromNothing = [];
			leapRings = [];
			asc = null;
		}
		klass = k;
		if (classSel) classSel.value = k ? k.name : "";
		fillAscSelect();
		if (k && focus) centerOn(nodes.get(k.startNodeId), 0.05);
		afterChange();
	}
	function setAsc(id: string, focus = true) {
		const a = klass?.ascendancies.find((x) => x.id === id) || null;
		// 전직을 바꾸면 이전 전직 노드는 뺀다
		for (const nid of [...allocated]) if (nodes.get(nid)?.ascendancy) allocated.delete(nid);
		asc = a;
		if (ascSel) ascSel.value = a ? a.id : "";
		if (a && focus && a.startNodeId) centerOn(nodes.get(a.startNodeId), 0.06);
		afterChange();
	}
	function centerOn(n: TreeNode | undefined, s?: number) {
		if (!n) return;
		cx = n.x;
		cy = n.y;
		if (s) scale = s * dpr;
		draw();
	}
	function fit() {
		if (!data) return;
		const w = canvas!.width, h = canvas!.height;
		if (isAtlas()) {
			// 아틀라스는 하위 트리 7개가 한 판에 흩어져 있다 — 전체 범위에 맞춘다
			const b = data.bounds;
			scale = Math.min(w / (b.maxX - b.minX), h / (b.maxY - b.minY));
			cx = (b.minX + b.maxX) / 2;
			cy = (b.minY + b.maxY) / 2;
			draw();
			return;
		}
		const bw = 26000, bh = 26000; // 본 트리(전직 둘레 제외) 폭 — bounds 전체로 맞추면 본 트리가 작게 보인다
		scale = Math.min(w / bw, h / bh);
		cx = 0;
		cy = 0;
		draw();
	}

	// ── 검색 ──
	function runSearch() {
		const q = searchKey((search?.value || "").trim());
		matches = new Set();
		matchOrder = [];
		matchCursor = -1;
		if (q.length >= 1) {
			for (const n of nodes.values()) {
				if (!visible(n)) continue;
				const hay = searchKey([n.name, n.nameKo || "", ...n.stats, ...(n.statsKo || [])].join("\n"));
				if (hay.includes(q)) {
					matches.add(n.id);
					matchOrder.push(n.id);
				}
			}
		}
		updateSearchCount();
		draw();
	}

	// ── 툴팁 ──
	function updateSearchCount() {
		if (!searchCount) return;
		const q = (search?.value || "").trim();
		if (!q) {
			searchCount.textContent = "";
			return;
		}
		searchCount.textContent =
			matchCursor >= 0 && matchOrder.length
				? `${matchCursor + 1}/${matchOrder.length}`
				: t(`${matches.size}개 일치`, `${matches.size} matches`);
	}
	function kindHeader(n: TreeNode): string {
		if (n.kind === "keystone" || n.kind === "notable" || n.kind === "jewel") return n.kind;
		if (n.ascendancy) return "ascendancy";
		return "normal";
	}
	// Alt 를 누르고 있는 동안 툴팁에 키워드 정의(C110) — 누르고 떼면 마지막 툴팁을 그 자리에서 다시 그린다
	let altHeld = false;
	let lastTip: [TreeNode, number, number] | null = null;
	const setAlt = (on: boolean) => {
		if (altHeld === on) return;
		altHeld = on;
		if (lastTip && tooltip && !tooltip.classList.contains("hidden")) showTooltip(...lastTip);
	};
	globalThis.addEventListener("keydown", (e) => {
		if (e.key !== "Alt") return;
		e.preventDefault(); // 떼면 브라우저 메뉴로 포커스가 가는 것 막기(keyup 도)
		setAlt(true);
	});
	globalThis.addEventListener("keyup", (e) => {
		if (e.key !== "Alt") return;
		e.preventDefault();
		setAlt(false);
	});
	globalThis.addEventListener("blur", () => setAlt(false));
	/** 키워드 안내 · Alt 정의 상자(C110 노드 · C126 꽂은 주얼 공용) — 평소 용어 이름 한 줄, Alt 누르는 동안 정의(회색) */
	function appendKeywords(body: HTMLElement, list: JewelKw[], attr = "data-tree-keywords") {
		if (!list.length) return;
		if (altHeld) {
			const box = document.createElement("div");
			box.className = "mt-1 pt-1 border-t border-white/10";
			box.setAttribute(attr, "");
			for (const k of list) {
				const term = document.createElement("div");
				term.className = "poe-popup-stat font-semibold";
				term.textContent = k.t;
				box.appendChild(term);
				const def = document.createElement("div");
				def.className = "poe-popup-reminder whitespace-pre-line mb-1";
				def.textContent = k.d;
				box.appendChild(def);
			}
			body.appendChild(box);
		} else {
			const hint = document.createElement("div");
			hint.className = "poe-popup-reminder";
			hint.setAttribute(attr === "data-tree-keywords" ? "data-tree-keyword-hint" : attr + "-hint", "");
			hint.textContent = t("Alt: 용어 설명 — ", "Alt: keywords — ") + list.map((k) => k.t).join(" · ");
			body.appendChild(hint);
		}
	}
	function showTooltip(n: TreeNode, clientX: number, clientY: number) {
		if (!tooltip) return;
		lastTip = [n, clientX, clientY];
		tooltip.replaceChildren();
		const header = document.createElement("div");
		header.className = "poe-psheader poe-psheader-" + kindHeader(n);
		const ascName = n.ascendancy ? klass?.ascendancies.find((a) => a.id === n.ascendancy) : undefined;
		let title = isKorean && n.nameKo ? n.nameKo : n.name;
		// 능력치를 고른 노드는 인게임처럼 그 능력치 이름으로(예: "+5 아무 능력치" 노드 → "힘")
		const pickedOpt = attrPick.get(n.id) && n.options ? n.options[attrPick.get(n.id)! - 1] : undefined;
		if (pickedOpt) title = isKorean && pickedOpt.nameKo ? pickedOpt.nameKo : pickedOpt.name;
		// 직업 시작점은 게임 데이터에 옛 이름(Marauder …)이 남아 있어 그 자리를 쓰는 직업 이름으로 보인다
		if (n.kind === "classStart" && n.classStart?.length && data) {
			title = n.classStart
				.map((c) => data!.classes.find((k) => k.name === c))
				.filter((k): k is Klass => !!k)
				.map((k) => (isKorean && k.nameKo ? k.nameKo : k.name))
				.join(" · ");
		}
		// 아틀라스는 전직 자리에 하위 트리 이름(의식·균열…)을 붙인다
		const ascLabel = ascName ? (isKorean && ascName.nameKo ? ascName.nameKo : ascName.name) : treeLabel(treeOf(n.tree));
		// 전직 시작점은 이름이 곧 전직 이름 — "타이탄 (타이탄)" 처럼 겹치지 않게
		header.textContent = title + (ascLabel && ascLabel !== title ? ` (${ascLabel})` : "");
		tooltip.appendChild(header);
		const body = document.createElement("div");
		body.className = "poe-popup-body";
		const lines = effectiveLines(n);
		for (const s of lines) {
			const line = document.createElement("div");
			line.className = "poe-popup-stat";
			line.textContent = s;
			body.appendChild(line);
		}
		(n.options || []).forEach((o, i) => {
			const line = document.createElement("div");
			const chosen = attrPick.get(n.id) === i + 1;
			line.className = chosen ? "poe-popup-stat" : "poe-popup-reminder";
			line.textContent = (chosen ? "▶ " : "· ") + ((isKorean && o.statsKo && o.statsKo[0]) || o.stats[0] || o.name);
			body.appendChild(line);
		});
		if (n.options?.length && !isAtlas()) {
			const hint = document.createElement("div");
			hint.className = "poe-popup-reminder";
			hint.textContent = allocated.has(n.id)
				? t("우클릭: 능력치 바꾸기", "Right-click: change attribute")
				: t("찍은 뒤 우클릭으로 능력치를 고릅니다", "Allocate, then right-click to choose");
			hint.setAttribute("data-attr-hint", "");
			body.appendChild(hint);
		}
		const fl = isKorean && n.flavourKo ? n.flavourKo : n.flavour;
		if (fl) {
			const line = document.createElement("div");
			line.className = "poe-popup-flavour";
			line.textContent = fl;
			body.appendChild(line);
		}
		if (isKorean && n.nameKo && n.nameKo !== n.name && n.kind !== "classStart") {
			const en = document.createElement("div");
			en.className = "poe-popup-reminder";
			en.textContent = n.name;
			body.appendChild(en);
		}
		// 키워드 설명(10-04 C110) — 인게임 PoE2 는 리마인더 줄 대신 강조 용어를 Alt 로 풀어 준다(KeywordPopups 용어 · 정의).
		//   평소엔 용어 이름만 한 줄 안내, Alt 를 누르고 있으면 정의를 회색으로
		const kws = (n.keywords || []).map((k) => data?.keywords?.[k]).filter((k): k is NonNullable<typeof k> => !!k);
		appendKeywords(body, kws.map((k) => ({ t: (isKorean && k.termKo) || k.term, d: (isKorean && k.defKo) || k.def })));
		if (jewelPicks.has(n.id)) {
			const jl = document.createElement("div");
			jl.className = "mt-1 text-xs text-emerald-300";
			jl.setAttribute("data-tree-jewel", "");
			jl.textContent = t("꽂은 주얼: ", "Socketed: ") + jewelLabel(jewelPicks.get(n.id)!);
			body.appendChild(jl);
			// 꽂은 주얼의 옵션 줄 — 인게임은 꽂은 칸에 올리면 그 주얼의 아이템 툴팁(10-04 C92, PoE1 C91 짝). 변형을 골랐으면 그 변형의 줄
			const pickV = jewelPicks.get(n.id)!;
			const dj = jewelDefs.get(splitPick(pickV)[0]);
			const vr = pickVariant(pickV);
			const enLines = vr ? vr.explicits : dj?.explicits || [];
			const koLines = vr ? vr.explicitsKo : dj?.explicitsKo || [];
			const lines = isKorean && koLines.length === enLines.length && koLines.length ? koLines : enLines;
			for (const line of lines) {
				const ml = document.createElement("div");
				// 타락은 인게임처럼 붉게(PoE1 · 고유 툴팁과 같은 --poe-corrupt 색)
				ml.className = line === "타락" || line === "Corrupted" ? "text-[12px] font-bold text-[#d20000]" : "poe-popup-stat";
				ml.setAttribute("data-jewel-mod", "");
				ml.textContent = line;
				body.appendChild(ml);
			}
			// 꽂은 주얼의 키워드 Alt 설명(10-04 C126) — 변형을 골랐으면 그 변형 것(null 이면 고유 것)
			appendKeywords(body, (vr && vr.kw) || dj?.kw || [], "data-jewel-keywords");
		} else if (n.kind === "jewel" && !n.ascendancy && allocated.has(n.id) && jewelDefs.size) {
			const jl = document.createElement("div");
			jl.className = "mt-1 text-xs text-base-content/60";
			jl.textContent = t("우클릭: 주얼 장착", "Right-click: socket a jewel");
			body.appendChild(jl);
		}
		// 비용 줄(C70, PoE1 과 같은 문구) — 찍으면 +N · 풀면 −N · 닿지 않으면 "연결 불가"
		if (hovered && hovered.id === n.id) {
			let cost = "", cls = "";
			if (allocated.has(n.id)) {
				if (removalPreview.size) { cost = t(`해제 −${removalPreview.size} 포인트`, `Refund −${removalPreview.size} points`); cls = "text-rose-400"; }
			} else if (hoverPathIds.length) {
				cost = t(`+${hoverPathIds.length} 포인트`, `+${hoverPathIds.length} points`); cls = "text-amber-300";
			} else if (allocated.size > 0 && allocatable(n)) {
				cost = t("연결 불가", "Unreachable"); cls = "text-base-content/60";
			}
			if (cost) {
				const foot = document.createElement("div");
				foot.className = "mt-1 text-xs font-semibold " + cls;
				foot.setAttribute("data-tree-cost", "");
				foot.textContent = cost;
				body.appendChild(foot);
			}
		}
		if (body.childElementCount) tooltip.appendChild(body);
		tooltip.classList.remove("hidden");
		const host = canvas!.parentElement!.getBoundingClientRect();
		let left = clientX - host.left + 16;
		let top = clientY - host.top + 16;
		const tw = tooltip.offsetWidth, th = tooltip.offsetHeight;
		if (left + tw > host.width) left = Math.max(0, clientX - host.left - tw - 16);
		if (top + th > host.height) top = Math.max(0, clientY - host.top - th - 16);
		tooltip.style.left = left + "px";
		tooltip.style.top = top + "px";
	}
	function hideTooltip() {
		tooltip?.classList.add("hidden");
	}
	function nodeAt(clientX: number, clientY: number): TreeNode | null {
		const rect = canvas!.getBoundingClientRect();
		const sx = (clientX - rect.left) * dpr, sy = (clientY - rect.top) * dpr;
		let best: TreeNode | null = null;
		let bestD = Infinity;
		for (const n of nodes.values()) {
			if (!visible(n)) continue;
			const [nx, ny] = toScreen(n.x, n.y);
			const r = Math.max(6 * dpr, (RADIUS[n.kind] || 36) * scale * 1.3);
			const d = Math.hypot(nx - sx, ny - sy);
			if (d < r && d < bestD) {
				best = n;
				bestD = d;
			}
		}
		return best;
	}

	// ── 입력 ──
	let dragging = false;
	let moved = false;
	let lastX = 0, lastY = 0;
	canvas.addEventListener("pointerdown", (e) => {
		dragging = true;
		moved = false;
		lastX = e.clientX;
		lastY = e.clientY;
		canvas!.setPointerCapture(e.pointerId);
		canvas!.style.cursor = "grabbing";
	});
	canvas.addEventListener("pointermove", (e) => {
		if (dragging) {
			const dx = e.clientX - lastX, dy = e.clientY - lastY;
			if (Math.abs(dx) + Math.abs(dy) > 3) moved = true;
			cx -= (dx * dpr) / scale;
			cy -= (dy * dpr) / scale;
			lastX = e.clientX;
			lastY = e.clientY;
			hideTooltip();
			draw();
			return;
		}
		const n = nodeAt(e.clientX, e.clientY);
		if (n !== hovered) {
			hovered = n;
			refreshPreview();
			draw();
		}
		if (n) showTooltip(n, e.clientX, e.clientY);
		else hideTooltip();
		canvas!.style.cursor = n ? "pointer" : "grab";
	});
	canvas.addEventListener("pointerup", (e) => {
		dragging = false;
		canvas!.style.cursor = "grab";
		// 왼쪽 단추만 찍기/풀기 — 우클릭(능력치 선택)까지 여기서 받으면 노드가 먼저 풀려 선택이 안 됐다(10-01 탐침)
		if (!moved && e.button === 0) {
			const n = nodeAt(e.clientX, e.clientY);
			if (n) toggle(n);
		}
	});
	let jewelPanel: HTMLElement | null = null;
	// 창을 닫으면 연 요소로 포커스를 되돌린다(10-04 C105, PoE1 restoreFocus 와 같은 규칙) — 창 안이나 body 에 포커스가 있을 때만
	let jewelOpener: HTMLElement | null = null;
	function closeJewelPicker() {
		const active = document.activeElement;
		if (jewelPanel && jewelOpener && document.contains(jewelOpener) && (!active || active === document.body || jewelPanel.contains(active))) jewelOpener.focus();
		jewelPanel?.remove();
		jewelPanel = null;
	}
	function openJewelPicker(node: TreeNode, clientX: number, clientY: number) {
		closeJewelPicker();
		jewelOpener = document.activeElement as HTMLElement | null;
		hideTooltip();
		const host = canvas!.parentElement as HTMLElement;
		const panel = document.createElement("div");
		panel.className = "absolute z-20 max-h-[60%] w-80 max-w-[calc(100%-8px)] overflow-y-auto poe-node-popup";
		panel.setAttribute("role", "dialog");
		panel.setAttribute("data-jewel-picker", "");
		const head = document.createElement("div");
		head.className = "sticky top-0 poe-popup-head";
		head.textContent = t("주얼 슬롯 — 주얼 장착", "Jewel socket — socket jewel");
		panel.appendChild(head);
		panel.setAttribute("aria-label", head.textContent || ""); // 창 이름 = 머리줄(10-04 C103, PoE1 C102 짝)
		const filter = document.createElement("input");
		filter.type = "text";
		filter.className = "w-full px-3 py-1.5 text-xs bg-stone-800 text-amber-100 border-b border-stone-700 outline-none";
		filter.placeholder = t("주얼 이름/효과 검색", "Filter by name or mod");
		filter.setAttribute("aria-label", filter.placeholder);
		panel.appendChild(filter);
		const list = document.createElement("div");
		panel.appendChild(list);
		const pick = (slug: string | null) => {
			const before = snapshot();
			if (slug) jewelPicks.set(node.id, slug);
			else jewelPicks.delete(node.id);
			closeJewelPicker();
			afterChange();
			record(before);
		};
		const render = (q: string) => {
			list.replaceChildren();
			const key = searchKey(q);
			if (jewelPicks.has(node.id)) {
				const rm = document.createElement("button");
				rm.type = "button";
				rm.className = "block w-full text-left px-3 py-1.5 text-xs text-rose-300 hover:bg-stone-700";
				rm.textContent = t("주얼 빼기", "Remove jewel");
				rm.addEventListener("click", () => pick(null));
				list.appendChild(rm);
			}
			for (const [slug, d] of jewelDefs) {
				// 변형 이름 · 옵션으로도 찾는다(10-04 C89) — "거대괴수"(메갈로매니악 변형) · "곡예" 처럼 기본 변형에 없는 말
				const hitBase = !key || searchKey([d.nameKo, d.name, ...d.explicits, ...d.explicitsKo].join("\n")).includes(key);
				const hitVariant = !!key && !hitBase && d.variants.some((v) => searchKey([v.nameKo, v.name, ...v.explicits].join("\n")).includes(key));
				if (!hitBase && !hitVariant) continue;
				const b = document.createElement("button");
				b.type = "button";
				b.className = "block w-full text-left px-3 py-1.5 text-xs hover:bg-stone-700" + (jewelPicks.get(node.id) === slug ? " text-amber-300" : " text-amber-100");
				// 같은 이름 변형(장대한 파장 생명력 · 정신력 · 저항)을 가르도록 첫 옵션 줄을 둘째 줄로
				const nameLine = document.createElement("div");
				nameLine.textContent = t(d.nameKo, d.name);
				const modLine = document.createElement("div");
				modLine.className = "text-[10px] text-base-content/60 truncate";
				modLine.textContent = t(d.explicitsKo[0] || d.explicits[0] || "", d.explicits[0] || "");
				b.append(nameLine, modLine);
				b.title = d.explicits.join("\n");
				b.dataset.slug = slug;
				if (hitVariant) {
					const hint = document.createElement("div");
					hint.className = "text-[10px] text-emerald-300";
					hint.textContent = t("변형에 일치", "matches a variant");
					b.appendChild(hint);
				}
				// 변형으로 찾았으면 변형 단계 검색칸에 같은 말을 채워 바로 좁혀 보인다
				b.addEventListener("click", () => (d.variants.length > 1 ? renderVariants(slug, hitVariant ? q : "") : pick(slug)));
				list.appendChild(b);
			}
		};
		// 변형 고르기(C74) — From Nothing 핵심 · Controlled Metamorphosis 고리 등. 고른 값 = "slug:변형번호"
		const renderVariants = (slug: string, q0 = "") => {
			const d = jewelDefs.get(slug)!;
			list.replaceChildren();
			head.textContent = t(d.nameKo, d.name) + t(" — 변형 고르기", " — pick variant");
			panel.setAttribute("aria-label", head.textContent || "");
			filter.value = q0;
			filter.placeholder = t("변형 검색", "Filter variants");
			const draw2 = (q: string) => {
				list.replaceChildren();
				const k2 = searchKey(q);
				for (const v of d.variants) {
					if (k2 && !searchKey([v.nameKo, v.name, ...v.explicits].join("\n")).includes(k2)) continue;
					const b = document.createElement("button");
					b.type = "button";
					b.className = "block w-full text-left px-3 py-1.5 text-xs hover:bg-stone-700 text-amber-100";
					b.textContent = t(v.nameKo, v.name);
					b.title = v.explicits.join("\n");
					b.dataset.variant = String(v.index);
					b.addEventListener("click", () => pick(slug + ":" + v.index));
					list.appendChild(b);
				}
			};
			filter.oninput = () => draw2(filter.value);
			draw2(q0);
			filter.focus();
		};
		filter.oninput = () => render(filter.value);
		panel.addEventListener("keydown", (ev) => { if (ev.key === "Escape") closeJewelPicker(); });
		render("");
		host.appendChild(panel);
		const r = host.getBoundingClientRect();
		panel.style.left = Math.max(4, Math.min(clientX - r.left + 8, host.clientWidth - panel.offsetWidth - 4)) + "px";
		panel.style.top = Math.max(4, Math.min(clientY - r.top + 8, host.clientHeight - panel.offsetHeight - 4)) + "px";
		jewelPanel = panel;
		filter.focus();
	}
	// 우클릭 — 찍은 능력치 노드의 능력치를 힘 → 민첩 → 지능 → 선택 안 함 순으로 바꾼다(PoB 와 같은 번호)
	canvas.addEventListener("contextmenu", (e) => {
		const n = nodeAt(e.clientX, e.clientY);
		// 주얼 칸 우클릭 — 고유 주얼 꽂기(C73, PoE1 노드 메뉴 "주얼 장착"의 짝). 찍힌 칸에만
		if (n && n.kind === "jewel" && !n.ascendancy && !isAtlas() && jewelDefs.size) {
			e.preventDefault();
			if (!allocated.has(n.id)) {
				flash(t("먼저 주얼 칸을 찍으세요", "Allocate the jewel socket first"));
				return;
			}
			openJewelPicker(n, e.clientX, e.clientY);
			return;
		}
		if (!n || !n.options?.length || isAtlas()) return;
		e.preventDefault();
		if (!allocated.has(n.id)) {
			flash(t("먼저 노드를 찍으세요", "Allocate the node first"));
			return;
		}
		const before = snapshot();
		const next = ((attrPick.get(n.id) || 0) + 1) % 4;
		if (next) attrPick.set(n.id, next);
		else attrPick.delete(n.id);
		afterChange();
		record(before);
		showTooltip(n, e.clientX, e.clientY);
	});
	canvas.addEventListener("pointerleave", () => {
		hovered = null;
		refreshPreview();
		hideTooltip();
		draw();
	});
	canvas.addEventListener(
		"wheel",
		(e) => {
			e.preventDefault();
			const rect = canvas!.getBoundingClientRect();
			const sx = (e.clientX - rect.left) * dpr, sy = (e.clientY - rect.top) * dpr;
			const [wx, wy] = toWorld(sx, sy);
			const factor = Math.exp(-e.deltaY * 0.0015);
			scale = Math.min(0.6 * dpr, Math.max(0.008 * dpr, scale * factor));
			// 커서 아래 지점이 그대로 머물게
			cx = wx - (sx - canvas!.width / 2) / scale;
			cy = wy - (sy - canvas!.height / 2) / scale;
			draw();
		},
		{ passive: false },
	);
	function zoomBy(f: number) {
		scale = Math.min(0.6 * dpr, Math.max(0.008 * dpr, scale * f));
		draw();
	}
	document.getElementById("poe2TreeZoomIn")?.addEventListener("click", () => zoomBy(1.4));
	document.getElementById("poe2TreeZoomOut")?.addEventListener("click", () => zoomBy(1 / 1.4));
	document.getElementById("poe2TreeFit")?.addEventListener("click", fit);
	document.getElementById("poe2TreeClear")?.addEventListener("click", () => {
		const before = snapshot();
		allocated.clear();
		afterChange();
		record(before);
	});
	classSel?.addEventListener("change", () => {
		const before = snapshot();
		setClass(classSel.value);
		record(before);
	});
	ascSel?.addEventListener("change", () => {
		const before = snapshot();
		setAsc(ascSel.value);
		record(before);
	});
	// ── PoE1 트리(poe/tree.ts)와 같은 도구(10-01): 실행취소/다시실행 · 할당 스탯 합계 · 링크 복사 · 전체 화면 ──
	// 스냅샷 = 직업 · 전직 · 할당 노드. 노드를 찍거나 풀기 · 초기화 · 직업/전직 변경이 한 단계(주소 복원은 기록하지 않는다).
	const undoStack: string[] = [];
	const redoStack: string[] = [];
	function snapshot(): string {
		return JSON.stringify([klass?.name || "", asc?.id || "", [...allocated].sort((a, b) => a - b), [...attrPick].sort((a, b) => a[0] - b[0]), [...setMode].sort((a, b) => a[0] - b[0])]);
	}
	function record(before: string) {
		if (before === snapshot()) return;
		undoStack.push(before);
		if (undoStack.length > 200) undoStack.shift();
		redoStack.length = 0;
		updateHistoryButtons();
	}
	function restore(s: string) {
		const [c, a, ids, picks, sets] = JSON.parse(s) as [string, string, number[], [number, number][], [number, number][]];
		klass = data?.classes.find((k) => k.name === c) || null;
		if (classSel) classSel.value = klass ? klass.name : "";
		fillAscSelect();
		asc = klass?.ascendancies.find((x) => x.id === a) || null;
		if (ascSel) ascSel.value = asc ? asc.id : "";
		allocated.clear();
		for (const id of ids) allocated.add(id);
		attrPick.clear();
		for (const [id, v] of picks || []) attrPick.set(id, v);
		setMode.clear();
		for (const [id, v] of sets || []) setMode.set(id, v);
		afterChange();
		updateHistoryButtons();
	}
	function undo() {
		const s = undoStack.pop();
		if (s === undefined) return;
		redoStack.push(snapshot());
		restore(s);
	}
	function redo() {
		const s = redoStack.pop();
		if (s === undefined) return;
		undoStack.push(snapshot());
		restore(s);
	}
	function updateHistoryButtons() {
		const u = document.getElementById("poe2TreeUndo") as HTMLButtonElement | null;
		const r = document.getElementById("poe2TreeRedo") as HTMLButtonElement | null;
		if (u) u.disabled = undoStack.length === 0;
		if (r) r.disabled = redoStack.length === 0;
	}
	document.getElementById("poe2TreeUndo")?.addEventListener("click", undo);
	// 찍기 모드 — 공용 / 세트 I / 세트 II(인게임 무기 세트 패시브 포인트). 바꿔도 이미 찍은 노드는 그대로
	const setButtons = Array.from(document.querySelectorAll<HTMLButtonElement>("[data-tree-set]"));
	const markSetButtons = () =>
		setButtons.forEach((b) => {
			const on = Number(b.dataset.treeSet) === curSet;
			b.classList.toggle("btn-active", on);
			b.setAttribute("aria-pressed", on ? "true" : "false");
		});
	setButtons.forEach((b) =>
		b.addEventListener("click", () => {
			curSet = Number(b.dataset.treeSet) || 0;
			markSetButtons();
		}),
	);
	markSetButtons();
	document.getElementById("poe2TreeRedo")?.addEventListener("click", redo);
	document.addEventListener("keydown", (e) => {
		const el = e.target as HTMLElement | null;
		if (el && (el.tagName === "INPUT" || el.tagName === "TEXTAREA" || el.tagName === "SELECT" || el.isContentEditable)) return;
		if (!(e.ctrlKey || e.metaKey)) return;
		const k = e.key.toLowerCase();
		if (k === "z" && !e.shiftKey) {
			e.preventDefault();
			undo();
		} else if ((k === "z" && e.shiftKey) || k === "y") {
			e.preventDefault();
			redo();
		}
	});

	// 할당 스탯 합계 — PoE1 aggregateStats 와 같은 규칙: 문장을 [고정 조각] + [숫자] 로 쪼개 같은 조각끼리 숫자만 더한다
	const NUM_RE = /[+\-]?\d+(?:\.\d+)?/g;
	// 요약 기준 세트(10-02) — 인게임은 켜진 무기 세트의 전용 패시브만 적용된다. 공용 노드는 늘, 다른 세트 노드는 뺀다
	let summarySet = 1;
	const countsForSummary = (id: number) => modeOf(id) === 0 || modeOf(id) === summarySet;
	// 소형 패시브 효과(10-02 사용자 질문 — 타이탄 "육중한 형체" 소형 패시브 스킬 효과 50% 증가) — PoB-PoE2 CalcSetup.buildModListForNode 와 같은 규칙:
	//   대상 = 일반 노드(kind normal) 중 전직 노드가 아닌 것. 능력치 노드(kind attribute) · 주요 · 핵심은 안 커진다.
	//   배율 = 1 + (찍은 노드들의 "increased effect of Small Passive Skills" 합) / 100.
	//   정수는 소수 버림(PoB ModStore.ScaleAddMod — m_modf(round(v × 배율, 2)): +5 → +7, 10% → 15%), 소수 값은 0.01 자리에서 버림.
	function smallPassiveInc(): number {
		let inc = 0;
		for (const id of allocated) {
			if (!countsForSummary(id)) continue;
			inc += smallPassiveIncOf(nodes.get(id)?.stats || []);
		}
		return inc;
	}
	function scaleLine(line: string, scale: number): string {
		return line.replace(/(\d+(?:\.\d+)?)/g, (num) => {
			const v = parseFloat(num) * scale;
			return String(num.includes(".") ? Math.floor(v * 100) / 100 : Math.trunc(Math.round(v * 100) / 100));
		});
	}
	/** 노드 문장 — 소형 패시브 효과가 있으면 그 배율을 먹인 수치(인게임 툴팁 · 합계와 같다). */
	function effectiveLines(n: TreeNode, inc = smallPassiveInc()): string[] {
		const lines = nodeLines(n);
		if (inc <= 0 || !isSmallPassiveNode(n)) return lines;
		const scale = 1 + inc / 100;
		return lines.map((l) => scaleLine(l, scale));
	}
	function aggregateStats(): { text: string; count: number }[] {
		const acc = new Map<string, { parts: string[]; nums: number[]; signed: boolean[]; count: number }>();
		const inc = smallPassiveInc();
		for (const id of allocated) {
			const n = nodes.get(id);
			if (!n || n.kind === "classStart" || n.kind === "ascendancyStart") continue;
			if (!countsForSummary(id)) continue;
			for (const raw of effectiveLines(n, inc)) {
				const line = raw.trim();
				if (!line) continue;
				const nums: number[] = [];
				const signed: boolean[] = [];
				for (const m of line.match(NUM_RE) || []) {
					nums.push(parseFloat(m));
					signed.push(m[0] === "+" || m[0] === "-");
				}
				const parts = line.split(NUM_RE);
				const key = parts.join("\u0001");
				const cur = acc.get(key);
				if (cur) {
					cur.count++;
					nums.forEach((v, i) => (cur.nums[i] = (cur.nums[i] || 0) + v));
				} else acc.set(key, { parts, nums, signed, count: 1 });
			}
		}
		const out: { text: string; count: number }[] = [];
		for (const v of acc.values()) {
			let text = v.parts[0] || "";
			for (let i = 0; i < v.nums.length; i++) {
				const n = v.nums[i];
				const body = Number.isInteger(n) ? String(n) : n.toFixed(1);
				text += (v.signed[i] && n > 0 ? "+" : "") + body + (v.parts[i + 1] || "");
			}
			out.push({ text, count: v.count });
		}
		return out.sort((a, b) => b.count - a.count || a.text.localeCompare(b.text));
	}
	const statsPanel = document.getElementById("poe2TreeStats");
	function updateStatsPanel() {
		const body = document.getElementById("poe2TreeStatsBody");
		if (!body || !statsPanel || statsPanel.classList.contains("hidden")) return;
		body.replaceChildren();
		// 찍은 키스톤 · 주요 노드 — 빌드의 정체성. 누르면 그 노드로 이동(PoE1 과 같다)
		// 세트 전환 단추 — 세트 노드가 있을 때만 보인다
		const toggle = document.getElementById("poe2TreeStatsSet");
		if (toggle) {
			toggle.classList.toggle("hidden", setMode.size === 0);
			toggle.querySelectorAll<HTMLButtonElement>("[data-summary-set]").forEach((b) => {
				const on = Number(b.dataset.summarySet) === summarySet;
				b.classList.toggle("text-white", on);
				b.classList.toggle("text-stone-500", !on);
				b.setAttribute("aria-pressed", on ? "true" : "false");
			});
		}
		const keys = [...allocated].filter(countsForSummary).map((id) => nodes.get(id)).filter((n): n is TreeNode => !!n && (n.kind === "keystone" || n.kind === "notable"));
		keys.sort((a, b) => (a.kind === b.kind ? 0 : a.kind === "keystone" ? -1 : 1));
		for (const n of keys) {
			const b = document.createElement("button");
			b.type = "button";
			b.className = "block w-full text-left px-3 py-0.5 text-xs hover:bg-white/5 " + (n.kind === "keystone" ? "text-[#e0bf6a]" : "text-[#c9a55a]");
			b.textContent = isKorean && n.nameKo ? n.nameKo : n.name;
			b.setAttribute("data-key-node", String(n.id));
			b.addEventListener("click", () => centerOn(n, Math.max(scale / dpr, 0.06)));
			body.appendChild(b);
		}
		const rows = aggregateStats();
		if (keys.length && rows.length) {
			const hr = document.createElement("div");
			hr.className = "my-1 border-t border-white/10";
			body.appendChild(hr);
		}
		if (!rows.length) {
			const empty = document.createElement("div");
			empty.className = "text-xs text-stone-400 px-3 py-2"; // 패널은 고정 다크(PoE1 과 같다)
			empty.textContent = t("할당한 노드가 없습니다.", "No allocated nodes.");
			body.appendChild(empty);
			return;
		}
		for (const row of rows) {
			const line = document.createElement("div");
			line.className = "flex items-baseline gap-2 px-3 py-0.5 text-xs";
			line.setAttribute("data-stat-row", "");
			const text = document.createElement("span");
			text.className = "text-sky-300 flex-1";
			text.textContent = row.text;
			line.appendChild(text);
			if (row.count > 1) {
				const badge = document.createElement("span");
				badge.className = "text-[10px] font-mono text-stone-400 shrink-0";
				badge.textContent = "×" + row.count;
				line.appendChild(badge);
			}
			body.appendChild(line);
		}
	}
	document.querySelectorAll<HTMLButtonElement>("#poe2TreeStatsSet [data-summary-set]").forEach((b) =>
		b.addEventListener("click", () => {
			summarySet = Number(b.dataset.summarySet) || 1;
			updateStatsPanel();
		}),
	);
	document.getElementById("poe2TreeStatsToggle")?.addEventListener("click", () => {
		statsPanel?.classList.toggle("hidden");
		updateStatsPanel();
	});

	// 트리 계산(10-01, PoE1 "트리 계산"의 짝) — 폼 숨은 칸에 직업·전직·노드를 채워 두면 htmx 가 /poe2/htmx/tree/eval 로 보낸다.
	//   보내는 순간 결과 패널을 연다(PoE1 처럼 캔버스 왼쪽 위).
	const evalForm = document.getElementById("poe2TreeEvalForm") as HTMLFormElement | null;
	const evalPanel = document.getElementById("poe2TreeEvalPanel");
	function syncEvalForm() {
		if (!evalForm) return;
		const set = (name: string, v: string) => {
			const el = evalForm.querySelector<HTMLInputElement>(`input[name="${name}"]`);
			if (el) el.value = v;
		};
		set("className", klass?.name || "");
		set("ascendancy", asc?.id || "");
		set("nodes", [...allocated].sort((a, b) => a - b).join(","));
		set("attrs", [...attrPick].map(([id, v]) => id + ":" + v).join(","));
		set("sets", [...setMode].map(([id, v]) => id + ":" + v).join(","));
		set("jewels", [...jewelPicks].map(([id, slug]) => id + ":" + slug).join(","));
	}
	evalForm?.addEventListener("htmx:beforeRequest", () => {
		syncEvalForm();
		evalPanel?.classList.remove("hidden");
	});
	document.getElementById("poe2TreeEvalClose")?.addEventListener("click", () => evalPanel?.classList.add("hidden"));
	// → 시뮬(10-02, PoE1 트리의 "→ 시뮬" 짝) — 트리 주소와 같은 이름(c·a·n·s)으로 시뮬레이터를 연다
	document.getElementById("poe2TreeToSim")?.addEventListener("click", () => {
		if (!klass || !allocated.size) {
			flash(t("먼저 직업을 고르고 노드를 찍으세요", "Pick a class and allocate nodes first"));
			return;
		}
		const p = new URLSearchParams();
		p.set("c", klass.name);
		if (asc) p.set("a", asc.id);
		p.set("n", [...allocated].sort((x, y) => x - y).join(","));
		if (attrPick.size) p.set("s", [...attrPick].map(([id, v]) => id + ":" + v).join(","));
		if (setMode.size) p.set("w", [...setMode].map(([id, v]) => id + ":" + v).join(","));
		location.href = "/poe2/sim?" + p.toString();
	});

	// 링크 복사 — 할당 상태가 주소(#c=&a=&n=)에 있어 그대로 공유된다
	const copyBtn = document.getElementById("poe2TreeCopy") as HTMLButtonElement | null;
	if (copyBtn) {
		const label = copyBtn.textContent || "";
		copyBtn.addEventListener("click", async () => {
			try {
				await navigator.clipboard.writeText(location.href);
				copyBtn.textContent = t("복사됨", "Copied");
			} catch {
				copyBtn.textContent = t("복사 실패", "Failed");
			}
			window.setTimeout(() => (copyBtn.textContent = label), 1500);
		});
	}

	// 전체 화면 — 도구 막대까지 포함한 껍데기를(캔버스만 넣으면 전체 화면에서 조작이 안 된다, PoE1 과 같다)
	document.getElementById("poe2TreeFullscreen")?.addEventListener("click", () => {
		const shell = document.getElementById("poe2TreeShell") || canvas!.parentElement || canvas!;
		if (document.fullscreenElement) document.exitFullscreen();
		else shell.requestFullscreen?.();
	});
	document.addEventListener("fullscreenchange", () => {
		if (document.fullscreenElement) {
			const top = canvas!.getBoundingClientRect().top;
			canvas!.style.height = Math.max(320, Math.round(window.innerHeight - top)) + "px";
		} else canvas!.style.height = "";
		resize();
	});

	search?.addEventListener("input", runSearch);
	// Enter 다음 · Shift+Enter 이전 + "3/14" 위치 표시 — PoE1 트리 검색과 같다(10-01)
	search?.addEventListener("keydown", (e) => {
		// Shift+F10 · 메뉴 키 — 지금 가운데 둔 검색 노드에 우클릭과 같은 동작(주얼 칸 = 주얼 장착 창, 능력치 노드 = 능력치 바꾸기)(10-04 C108, PoE1 짝)
		if ((e.key === "ContextMenu" || (e.shiftKey && e.key === "F10")) && matchOrder.length) {
			e.preventDefault();
			const cur = nodes.get(matchOrder[Math.max(0, matchCursor)]);
			if (!cur || !canvas) return;
			centerOn(cur);
			const r = canvas.getBoundingClientRect();
			canvas.dispatchEvent(new MouseEvent("contextmenu", { bubbles: true, cancelable: true, clientX: r.left + r.width / 2, clientY: r.top + r.height / 2 }));
			return;
		}
		if (e.key !== "Enter" || !matchOrder.length) return;
		e.preventDefault();
		// Ctrl+Enter — 지금 가운데 둔 검색 노드를 찍기/해제(마우스 클릭과 같은 동작 · 필요하면 경로째). 키보드만으로 트리를 쓰게(10-02 접근성)
		if (e.ctrlKey || e.metaKey) {
			const cur = nodes.get(matchOrder[Math.max(0, matchCursor)]);
			if (!cur) return;
			const was = allocated.has(cur.id);
			toggle(cur);
			const now = allocated.has(cur.id);
			const label = isKorean && cur.nameKo ? cur.nameKo : cur.name;
			const verdict = now === was ? t("못 찍음", "Not allocated") : now ? t("찍음", "Allocated") : t("해제", "Removed");
			const countEl = document.getElementById("poe2TreeSearchCount");
			if (countEl) countEl.textContent = `${verdict}: ${label} · ${(points?.textContent || "").trim()}`;
			return;
		}
		const n = matchOrder.length;
		matchCursor = e.shiftKey ? (matchCursor <= 0 ? n - 1 : matchCursor - 1) : (matchCursor + 1) % n;
		centerOn(nodes.get(matchOrder[matchCursor]), Math.max(scale / dpr, 0.06));
		updateSearchCount();
	});
	window.addEventListener("resize", resize);

	function loadSprites() {
		fetch(spriteBase + "index.json")
			.then((r) => (r.ok ? r.json() : null))
			.then((j) => {
				if (!j || !j.icons) return;
				sprites = j.icons;
				for (const name of new Set(Object.values(sprites).map((x) => x.s))) {
					const img = new Image();
					img.onload = () => draw();
					img.src = spriteBase + name;
					sheets.set(name, img);
				}
			})
			.catch(() => {
				/* 아이콘 없이도 트리는 쓸 수 있다 */
			});
	}

	// ── 불러오기 ──
	fetch(src)
		.then((r) => {
			if (!r.ok) throw new Error("HTTP " + r.status);
			return r.json();
		})
		.then((json: TreeData) => {
			data = json;
			for (const [id, raw] of Object.entries(json.nodes)) {
				nodes.set(Number(id), { ...(raw as Omit<TreeNode, "id" | "adj">), id: Number(id), adj: [] });
			}
			// 연결은 한쪽에만 적혀 있을 수 있어 양방향 인접 목록을 만든다(경로 찾기용)
			for (const n of nodes.values()) {
				for (const c of n.out) {
					const m = nodes.get(c.id);
					if (!m) continue;
					n.adj.push(m.id);
					m.adj.push(n.id);
				}
			}
			fillClassSelect();
			fillAscSelect();
			if (atlasSel && json.trees) {
				atlasSel.replaceChildren(new Option(t("하위 트리로 이동", "Jump to tree"), ""));
				for (const s of json.trees) atlasSel.add(new Option(`${treeLabel(s)} (${s.nodeCount})`, s.key));
				atlasSel.addEventListener("change", () => {
					const s = treeOf(atlasSel.value);
					if (s) centerOn(nodes.get(s.rootId), 0.09);
				});
			}
			resize();
			fit();
			readHash();
			afterChange();
			runSearch();
			if (status) status.textContent = "";
			canvas!.setAttribute("data-ready", "1");
			loadSprites();
		})
		.catch((e) => {
			if (status) status.textContent = t("트리를 불러오지 못했습니다: ", "Failed to load the tree: ") + e.message;
		});
})();
