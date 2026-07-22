// PoE 패시브/아틀라스 트리 읽기 전용 뷰어 — 공식 스프라이트시트로 게임식 렌더.
// 데이터 소스는 캔버스 data-tree-src / data-sprites-src 로 지정(기본=스킬 트리) → 스킬·아틀라스 공용.
// 그룹 배경 + 궤도 곡선 연결 + 스킬 아이콘/프레임 스프라이트 blit. 팬/줌/호버 툴팁/?nodes= 하이라이트/로케일.
(() => {
	const maybeCanvas = document.getElementById("poeTreeCanvas") as HTMLCanvasElement | null;
	const tooltip = document.getElementById("poeTreeTooltip");
	if (!maybeCanvas || maybeCanvas.dataset.poeTreeInitialized === "true") return;
	const canvas = maybeCanvas;
	canvas.dataset.poeTreeInitialized = "true";
	const isKorean = canvas.dataset.locale !== "en";
	const treeSrc = canvas.dataset.treeSrc || "/poe-data/passive-tree.json";
	const spritesSrc = canvas.dataset.spritesSrc || "/poe-data/tree-sprites-skill.json";
	const maybeContext = canvas.getContext("2d");
	if (!maybeContext) return;
	const context = maybeContext;

	interface TreeNode {
		id: number;
		name: string;
		nameKo: string | null;
		type: string;
		group: number;
		orbit: number;
		orbitIndex: number;
		x: number;
		y: number;
		stats: string[];
		statsKo: string[] | null;
		ascendancy: string | null;
		ascendancyStart?: boolean;
		clusterSize?: number;
	expansionJewel?: { size: number; index: number; proxy: number | null; parent: number | null };
	isProxy?: boolean;
		masteryEffects?: { id: number; stats: string[]; statsKo?: string[] }[];
		icon: string | null;
		// 직업 시작 노드의 중앙 아트 키(centerwitch 등)
		startArt?: string;
	}
	interface Group {
		x: number;
		y: number;
		background: { image: string; isHalfImage?: boolean } | null;
		// 전직 시작 그룹이면 전직 이름(스프라이트 키 Classes<이름>) — 배경 아트를 여기에 그린다
		ascendancyStart?: string;
	}
	interface Sprite {
		file: string;
		zoom: number;
		coords: Record<string, { x: number; y: number; w: number; h: number }>;
	}

	// ---- 스프라이트 시트(이미지) 지연 로드 캐시 ----
	const sheetCache = new Map<string, HTMLImageElement>();
	function getSheet(file: string): HTMLImageElement | null {
		let img = sheetCache.get(file);
		if (img) return img.dataset.ready === "true" ? img : null;
		img = new Image();
		img.dataset.ready = "false";
		img.onload = () => {
			img!.dataset.ready = "true";
			scheduleDraw();
		};
		img.onerror = () => sheetCache.set(file, img!);
		img.src = "/poe-assets/" + file;
		sheetCache.set(file, img);
		return null;
	}
	let drawScheduled = false;
	function scheduleDraw() {
		if (drawScheduled) return;
		drawScheduled = true;
		requestAnimationFrame(() => {
			drawScheduled = false;
			draw();
		});
	}

	let sprites: Record<string, Sprite> = {};
	// 트리 배경 레이어(클래스 일러스트) — GGG 트리 데이터의 extraImages(좌표 포함), 이미지는 게임 번들 추출본.
	let extraImages: Array<{ x: number; y: number; image: string }> = [];
	// 이미지 파일명 → 그 아트가 대표하는 직업 id(선택한 직업만 선명하게)
	const LAYER_CLASS_ID: Record<string, number> = {
		"str.png": 1, // 머라우더
		"dex.png": 2, // 레인저
		"int.png": 3, // 위치
		"strdex.png": 4, // 듀얼리스트
		"strint.png": 5, // 템플러
		"dexint.png": 6, // 섀도우
	};
	// 스프라이트 1개를 (월드 중심 wx,wy)에 blit. coordKey 없으면 미그림. 반환: 그렸으면 월드 크기 반폭.
	// 없는 (시트,키) 조합은 조용히 안 그려진다 — 사이클 105 에서 마스터리 313개가 이렇게 통째로 사라졌다.
	// 개발 중 바로 눈에 띄도록 조합당 한 번만 경고한다(콘솔 폭주 방지 + QA 스크립트가 콘솔 오류로 잡을 수 있게).
	const missingSprites = new Set<string>();
	function warnMissing(spriteKey: string, coordKey: string) {
		const k = spriteKey + "|" + coordKey;
		if (missingSprites.has(k) || missingSprites.size > 30) return;
		missingSprites.add(k);
		console.warn("[poe-tree] 스프라이트 키 없음 — 이 노드는 안 그려집니다:", spriteKey, coordKey);
	}
	function blit(spriteKey: string, coordKey: string, wx: number, wy: number, clipCircle = false): number {
		const sp = sprites[spriteKey];
		if (!sp) {
			if (coordKey) warnMissing(spriteKey, coordKey);
			return 0;
		}
		const c = sp.coords[coordKey];
		if (!c) {
			if (coordKey) warnMissing(spriteKey, coordKey);
			return 0;
		}
		const img = getSheet(sp.file);
		const worldW = c.w / sp.zoom;
		const worldH = c.h / sp.zoom;
		if (!img) return worldW / 2;
		const sx = wx * scale + offsetX;
		const sy = wy * scale + offsetY;
		const dw = worldW * scale;
		const dh = worldH * scale;
		if (clipCircle) {
			context.save();
			context.beginPath();
			context.arc(sx, sy, Math.min(dw, dh) * 0.47, 0, Math.PI * 2);
			context.clip();
			context.drawImage(img, c.x, c.y, c.w, c.h, sx - dw / 2, sy - dh / 2, dw, dh);
			context.restore();
		} else {
			context.drawImage(img, c.x, c.y, c.w, c.h, sx - dw / 2, sy - dh / 2, dw, dh);
		}
		return worldW / 2;
	}

	// 노드 타입 → 아이콘 시트 키. 공식 뷰어처럼 할당=Active(선명) / 미할당=Inactive(흐림).
	const ICON_SHEET: Record<string, string> = {
		normal: "normalActive",
		notable: "notableActive",
		keystone: "keystoneActive",
		wormhole: "wormholeActive",
		mastery: "mastery",
	};
	const ICON_SHEET_INACTIVE: Record<string, string> = {
		normal: "normalInactive",
		notable: "notableInactive",
		keystone: "keystoneInactive",
		wormhole: "keystoneInactive",
		mastery: "masteryInactive",
	};
	// 미할당 시트가 없으면(아틀라스 등) Active 로 폴백
	/** 반경 링 아트를 목표 반경에 맞춰 늘려 그린다(blit 은 원본 크기 고정이라 반경별 링엔 못 쓴다). */
	function blitRing(coordKey: string, wx: number, wy: number, worldRadius: number, alpha: number): boolean {
		const sp = sprites.jewelRadius;
		const c = sp?.coords[coordKey];
		const img = c ? getSheet(sp.file) : null;
		if (!c || !img) return false;
		const diameter = worldRadius * 2 * scale;
		context.save();
		context.globalAlpha = alpha;
		context.drawImage(
			img,
			c.x,
			c.y,
			c.w,
			c.h,
			wx * scale + offsetX - diameter / 2,
			wy * scale + offsetY - diameter / 2,
			diameter,
			diameter,
		);
		context.restore();
		return true;
	}
	function iconSheetFor(type: string, allocated: boolean): string | undefined {
		if (!allocated) {
			const inactive = ICON_SHEET_INACTIVE[type];
			if (inactive && sprites[inactive]) return inactive;
		}
		return ICON_SHEET[type];
	}
	// 노드 상태 3단계 — 공식 뷰어와 동일. 할당 / 지금 찍을 수 있음(인접) / 미할당.
	const CLUSTER_SIZE_NAME = ["Small", "Medium", "Large"];
	const CLUSTER_SIZE_KO = ["소형", "중형", "대형"];
	// 클러스터 주얼 정의(크기별 노드 수/스킬 풀) — 소켓 툴팁에서 "무엇을 붙일 수 있는지" 안내에 쓴다.
	// GGG 트리엔 없는 데이터라 별도 추출본(cluster-jewels.json)을 지연 로드한다.
	type ClusterJewelDef = { minNodes: number; maxNodes: number; totalIndicies: number; skills: Record<string, { name: string }> };
	let clusterDefs: Record<string, ClusterJewelDef> | null = null;
	// 프록시 노드별 궤도 시작 오프셋(클러스터 생성에 필수)
	let clusterOffsets: Record<string, Record<string, number>> | null = null;
	// 노터블 정렬 순서 — PoB 는 이 순서로 노터블을 자리(템플릿 인덱스)에 배치한다.
	// 이름→순번이 없으면 같은 노터블 조합이라도 우리와 PoB 의 배치가 어긋나 다른 노드를 찍게 된다.
	let clusterNotableOrder: Record<string, number> | null = null;
	// 노터블 → 붙을 수 있는 스킬 태그·주얼 크기(게임 모드 가중치에서 추출).
	// 이게 없으면 UI 가 노터블 309개를 전부 보여줘 **게임엔 존재할 수 없는 주얼**을 만들게 된다.
	let clusterNotableOptions: Record<string, { tags: string[]; sizes: string[] }> | null = null;
	// 크기별 노터블 최대 개수(게임 규칙) — 대형 3 / 중형 2 / 소형 1
	const CLUSTER_NOTABLE_MAX: Record<string, number> = { Small: 1, Medium: 2, Large: 3 };
	// 크기별 주얼 소켓 최대 개수(게임 규칙, PoB CraftClusterJewel 과 동일) — 대형 2 / 중형 1 / 소형 0
	const CLUSTER_SOCKET_MAX: Record<string, number> = { Small: 0, Medium: 1, Large: 2 };
	const CLUSTER_DEF_KEY = ["Small Cluster Jewel", "Medium Cluster Jewel", "Large Cluster Jewel"];
	function loadClusterDefs(onReady?: () => void) {
		if (clusterDefs !== null) {
			onReady?.();
			return;
		}
		clusterDefs = {};
		fetch("/poe-data/cluster-jewels.json", { cache: "no-cache" })
			.then((r) => (r.ok ? r.json() : null))
			.then((data) => {
				if (data?.jewels) clusterDefs = data.jewels;
				if (data?.orbitOffsets) clusterOffsets = data.orbitOffsets;
				if (data?.notableSortOrder) clusterNotableOrder = data.notableSortOrder;
				if (data?.notableOptions) clusterNotableOptions = data.notableOptions;
			})
			.then(
				() => onReady?.(),
				() => onReady?.(), // tsconfig target 이 es2017 이라 Promise.finally 를 못 쓴다
			);
	}
	function frameCoord(node: TreeNode, allocated: boolean, canAlloc: boolean): string | null {
		const st = allocated ? "Allocated" : canAlloc ? "CanAllocate" : "Unallocated";
		switch (node.type) {
			case "notable":
				return "NotableFrame" + st;
			case "keystone":
			case "wormhole":
				return "KeystoneFrame" + st;
			case "jewel":
				// 클러스터 주얼 소켓은 크기별 전용 아트, 일반 소켓은 JewelSocketAlt 계열(공식 최신 아트)
				if (node.clusterSize !== undefined) {
					const size = CLUSTER_SIZE_NAME[node.clusterSize] || "Small";
					return "JewelSocketClusterAlt" + (allocated ? "Normal" : st === "CanAllocate" ? "CanAllocate" : "Normal") + "1" + size;
				}
				return allocated ? "JewelSocketAltActive" : canAlloc ? "JewelSocketAltCanAllocate" : "JewelSocketAltNormal";
			case "normal":
				// 공식은 일반 노드에 CanAllocate 전용 아트를 쓰지 않는다(Highlighted 는 호버/검색용)
				return allocated ? "PSSkillFrameActive" : "PSSkillFrame";
			default:
				return null;
		}
	}

	// 타입별 프레임 월드 반지름(히트테스트/LOD/폴백) — 매니페스트 로드 후 채움
	const nodeRadiusWorld: Record<string, number> = { normal: 45, notable: 70, keystone: 95, jewel: 55, mastery: 55, wormhole: 95, class: 120 };
	function computeRadii() {
		const fr = sprites.frame;
		if (!fr) return;
		const pick = (k: string) => (fr.coords[k] ? fr.coords[k].w / fr.zoom / 2 : null);
		nodeRadiusWorld.normal = pick("PSSkillFrame") ?? nodeRadiusWorld.normal;
		nodeRadiusWorld.notable = pick("NotableFrameUnallocated") ?? nodeRadiusWorld.notable;
		nodeRadiusWorld.keystone = pick("KeystoneFrameUnallocated") ?? nodeRadiusWorld.keystone;
		nodeRadiusWorld.wormhole = nodeRadiusWorld.keystone;
		nodeRadiusWorld.jewel = pick("JewelFrameUnallocated") ?? nodeRadiusWorld.jewel;
		if (sprites.mastery) {
			const anyM = Object.values(sprites.mastery.coords)[0];
			if (anyM) nodeRadiusWorld.mastery = anyM.w / sprites.mastery.zoom / 2;
		}
	}

	let nodes: TreeNode[] = [];
	let edges: number[][] = [];
	let groups: Record<string, Group> = {};
	let orbitRadii: number[] = [0, 82, 162, 335, 493, 662, 846];
	let skillsPerOrbit: number[] = [1, 6, 16, 16, 40, 72, 72];
	const nodeById = new Map<number, TreeNode>();
	const adjacency = new Map<number, number[]>();
	let scale = 0.03;
	let offsetX = 0;
	let offsetY = 0;
	let hovered: TreeNode | null = null;
	const searchHits = new Set<number>(); // 검색 매칭 노드(청록 테두리 강조)
	let hoverPath: number[] = []; // 호버 노드까지 할당집합에서의 최단경로 미리보기

	// 인터랙티브 편집: 할당 노드 집합 + 현재 직업/전직. 클래스 시작노드가 루트.
	const highlighted = new Set<number>();
	// 아틀라스는 클래스 루트가 없어 규칙이 다르다(자유 시작 + ?nodes= 동기화). 편집은 둘 다 가능.
	const isAtlas = canvas.dataset.treeSrc === "/poe-data/atlas-tree.json";
	const interactive = true;
	// 트리 데이터의 class 시작노드 이름 → GGG classId (0=Scion..6=Shadow)
	const CLASS_START_CLASSID: Record<string, number> = { Seven: 0, MARAUDER: 1, RANGER: 2, WITCH: 3, DUELIST: 4, TEMPLAR: 5, SIX: 6 };
	// 사람이 읽는 직업명 → classId (시뮬 결과 링크의 ?class= 해석용. 시작노드 이름과 다르다)
	const CLASS_NAME_CLASSID: Record<string, number> = {
		Scion: 0,
		Marauder: 1,
		Ranger: 2,
		Witch: 3,
		Duelist: 4,
		Templar: 5,
		Shadow: 6,
	};
	const classStartByClassId = new Map<number, number>(); // classId → 시작노드 id (loadTree 에서 채움)
	let currentClassId = 0;
	let currentAscend = 0;
	let currentBloodline = 0; // 0=없음, 1..N = bloodlines[N-1] (GGG URL 의 secondary ascendancy id)
	let classAsc: string[][] = []; // classId → 전직명 목록 (data.classes)
	let bloodlines: { id: string; name: string }[] = []; // 혈맹(대체 전직) — 인덱스 = secondary id
	let atlasRoot: number | null = null; // 아틀라스 시작 노드(지도 중앙) — 여기서 이어져야 할당 가능
	// 퀘스트 보상 패시브 포인트(레벨과 무관하게 고정) / 평가·최적화가 쓰는 레벨 90 의 총 포인트
	const QUEST_POINTS = 24;
	const EVAL_POINT_BUDGET = 113; // = 24 + (90 - 1)
	let maxPoints = 0; // 패시브 123 / 아틀라스 138
	let maxAscPoints = 0; // 전직 8
	const removalSet = new Set<number>(); // 할당 노드 호버 시 함께 해제될 노드(빨강 미리보기)
	const masteryPicks = new Map<number, number>(); // 마스터리 노드 id → 선택한 효과 id
	const jewelPicks = new Map<number, string>(); // 주얼 소켓 노드 id → 장착한 유니크 주얼 slug
	// 문신: 패시브 노드 id → 문신 영문명(dn). 게임에선 그 패시브가 **다른 노드로 교체**된다.
	// 반경 주얼(붉은 악몽 등)과 짝지어 반경 안 소형 패시브를 저항 문신으로 바꾸는 것이 실전 용법이다.
	const tattooPicks = new Map<number, string>();
	type TattooDef = {
		dn: string;
		nameKo: string | null;
		targetType: string;
		// 문신을 새기면 노드 그림도 바뀐다 — icon 은 노드 아이콘 시트의 좌표 키,
		// activeEffectImage 는 부족별 배경(tattooActiveEffect 시트) 좌표 키.
		icon: string;
		activeEffectImage: string;
		stats: string[];
		statsKo: string[] | null;
	};
	let tattooDefs: TattooDef[] | null = null; // null = 아직 안 받음

	/** 이 노드에 새겨진 문신의 그림 정보(부족 배경 좌표 키가 실제로 있는 것만). 정의를 아직 못 받았으면 null. */
	function tattooArt(nodeId: number): TattooDef | null {
		const dn = tattooPicks.get(nodeId);
		if (!dn || !tattooDefs?.length) return null;
		const def = tattooDefs.find((t) => t.dn === dn);
		return def && hasCoord("tattooActiveEffect", def.activeEffectImage) ? def : null;
	}
	const hasCoord = (spriteKey: string, coordKey: string) => !!coordKey && !!sprites[spriteKey]?.coords[coordKey];

	// 현재 화면에 노출할 전직/혈맹 서브트리 이름. 나머지 전직 섬은 숨긴다(공식 뷰어 동작).
	function currentAscName(): string | null {
		if (isAtlas || currentAscend === 0) return null;
		return (classAsc[currentClassId] || [])[currentAscend - 1] || null;
	}
	function currentBloodlineId(): string | null {
		if (isAtlas || currentBloodline === 0) return null;
		return bloodlines[currentBloodline - 1]?.id || null;
	}
	function nodeVisible(node: TreeNode): boolean {
		// 프록시(자리표시자) 노드는 게임에 없는 좌표 기준점이다 — 보이지도, 찍히지도 않아야 한다.
		// 생성된 클러스터 노드는 isProxy 가 아니므로 영향 없다.
		if (node.isProxy) return false;
		if (!node.ascendancy) return true;
		return node.ascendancy === currentAscName() || node.ascendancy === currentBloodlineId();
	}
	// 경로 탐색용 인접 — 전직↔메인 간선은 "선택한 전직/혈맹" 으로 들어가는 것만 허용한다.
	// (Ascendant 의 Path of the X 처럼 메인으로 되돌아오는 간선이 지름길로 악용되는 것도 함께 막힘)
	function pathNeighbors(id: number): number[] {
		const from = nodeById.get(id);
		if (!from) return [];
		const out: number[] = [];
		for (const nb of adjacency.get(id) || []) {
			const to = nodeById.get(nb);
			if (!to || !nodeVisible(to)) continue;
			// 다른 직업의 시작 노드는 통과할 수 없다 — 게임에서 남의 클래스 시작점은 찍히지 않는데,
			// 막지 않으면 경로 자동할당이 그 노드를 함께 찍어 "남의 시작점을 다리로 쓴" 불법 트리가 된다
			// (실측: 위치 트리에 RANGER 시작 노드가 끼어 연결성이 왜곡됨).
			if (to.type === "class" && nb !== rootNode()) continue;
			if ((from.ascendancy || null) === (to.ascendancy || null)) {
				out.push(nb);
			} else if (to.ascendancyStart || from.ascendancyStart) {
				out.push(nb); // 전직 진입/이탈 지점 (visible 검사를 이미 통과)
			}
		}
		return out;
	}

	// ---- GGG 패시브트리 URL 인코딩(version 6) ----
	function b64urlToBytes(s: string): number[] {
		const b64 = s.replace(/-/g, "+").replace(/_/g, "/");
		const bin = atob(b64);
		const out: number[] = [];
		for (let i = 0; i < bin.length; i++) out.push(bin.charCodeAt(i));
		return out;
	}
	function bytesToB64url(bytes: number[]): string {
		let bin = "";
		for (const b of bytes) bin += String.fromCharCode(b & 0xff);
		return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_");
	}
	// URL 의 GGG 문자열 → {classId, ascend, nodes[]}. 실패 시 null.
	function decodeTree(
		s: string,
	): {
		classId: number;
		ascend: number;
		bloodline: number;
		nodes: number[];
		masteries: { node: number; effect: number }[];
		clusters: number[];
	} | null {
		try {
			const a = b64urlToBytes(s);
			if (a.length < 7) return null;
			const classId = a[4];
			const ascByte = a[5];
			const ascend = ascByte & 0x3;
			const bloodline = ascByte >> 2; // secondary ascendancy id (혈맹)
			const count = a[6];
			const nodes: number[] = [];
			let p = 7;
			for (let i = 0; i < count && p + 1 < a.length; i++) {
				nodes.push(a[p] * 256 + a[p + 1]);
				p += 2;
			}
			// clusterCount(1B) + 클러스터 id(2B each) → 생성 노드 할당 복원용으로 읽는다,
			// 그다음 masteryCount(1B) + 항목(4B: effect 2B + node 2B)
			const masteries: { node: number; effect: number }[] = [];
			const clusters: number[] = [];
			if (p < a.length) {
				const cCount = a[p];
				p += 1;
				for (let i = 0; i < cCount && p + 1 < a.length; i++) {
					clusters.push(65536 + a[p] * 256 + a[p + 1]);
					p += 2;
				}
				if (p < a.length) {
					const mCount = a[p];
					p += 1;
					for (let i = 0; i < mCount && p + 3 < a.length; i++) {
						masteries.push({ effect: a[p] * 256 + a[p + 1], node: a[p + 2] * 256 + a[p + 3] });
						p += 4;
					}
				}
			}
			return { classId, ascend, bloodline, nodes, masteries, clusters };
		} catch (e) {
			return null;
		}
	}
	function encodeTree(): string {
		const a = [0, 0, 0, 6, currentClassId & 0xff, ((currentBloodline & 0x3f) << 2) | (currentAscend & 0x3)];
		let count = 0;
		const body: number[] = [];
		for (const id of highlighted) {
			const node = nodeById.get(id);
			if (!node || node.type === "class" || node.type === "ascendancyStart" || id >= 65536 || count >= 255) continue;
			body.push(Math.floor(id / 256), id % 256);
			count++;
		}
		// 클러스터 생성 노드(id ≥ 65536)는 GGG 규격상 별도 섹션에 id-65536 으로 들어간다
		const cluster: number[] = [];
		let clusterCount = 0;
		for (const id of highlighted) {
			if (id < 65536 || clusterCount >= 255) continue;
			const rel = id - 65536;
			cluster.push(Math.floor(rel / 256), rel % 256);
			clusterCount++;
		}
		// 그다음 masteryCount + 항목(effect 2B + node 2B)
		const mastery: number[] = [];
		let mCount = 0;
		for (const [nodeId, effectId] of masteryPicks) {
			if (!highlighted.has(nodeId) || mCount >= 255) continue;
			mastery.push(Math.floor(effectId / 256), effectId % 256, Math.floor(nodeId / 256), nodeId % 256);
			mCount++;
		}
		a.push(count, ...body, clusterCount, ...cluster, mCount, ...mastery);
		return bytesToB64url(a);
	}
	// URL 로드: ?t=GGG 우선, 없으면 ?nodes= 레거시(콤마 id).
	function loadFromUrl() {
		const params = new URLSearchParams(globalThis.location.search);
		// 클러스터 구성(c=소켓:크기:노드수:스킬키,...) — 트리 로드 후 서브트리를 다시 만들어야 한다
		for (const entry of (params.get("c") || "").split(",")) {
			const [socketId, sizeName, nodeCount, skillKey, notables, socketCount] = entry.split(":");
			if (!socketId || !sizeName || !nodeCount) continue;
			pendingClusters.push({
				socketId: Number(socketId),
				sizeName,
				nodeCount: Number(nodeCount),
				skillKey: skillKey || "",
				notables: (notables || "").split("|").filter(Boolean),
				socketCount: Number(socketCount) || 0,
			});
		}
		// 주얼(j=노드:slug,...) — 트리 인코딩과 무관하게 ?t= / ?nodes= 어느 쪽이든 함께 복원한다
		for (const pair of (params.get("j") || "").split(",")) {
			const sep = pair.indexOf(":");
			if (sep <= 0) continue;
			const nodeId = Number(pair.slice(0, sep));
			const slug = pair.slice(sep + 1).trim();
			if (Number.isFinite(nodeId) && slug) jewelPicks.set(nodeId, slug);
		}
		// 문신(tt=노드:문신영문명|노드:문신영문명) — 문신 이름엔 공백이 있어 콤마 대신 '|' 로 나눈다
		// URLSearchParams.get 은 %XX 와 '+' 를 이미 풀어 준다(문신 이름의 공백)
		for (const pair of (params.get("tt") || "").split("|")) {
			const sep = pair.indexOf(":");
			if (sep <= 0) continue;
			const nodeId = Number(pair.slice(0, sep));
			const dn = pair.slice(sep + 1).trim();
			if (Number.isFinite(nodeId) && dn) tattooPicks.set(nodeId, dn);
		}
		// 링크로 들어온 문신은 **그리기 전에** 정의가 있어야 부족 배경/아이콘이 나온다(팝업을 열 때까진 안 받는 게 기본).
		if (tattooPicks.size) loadTattoos(() => draw());
		// 마스터리 선택(masteries=노드:효과,…) — legacy nodes= 링크(시뮬 결과 등)가 마스터리를 실어 나르는 유일한 길.
		// 이게 없으면 최적화 결과의 트리 링크가 마스터리 스탯 빠진 약한 트리로 열린다(표시≠실제).
		for (const pair of (params.get("masteries") || "").split(",")) {
			const [nodeId, effectId] = pair.split(":").map(Number);
			if (Number.isFinite(nodeId) && Number.isFinite(effectId) && effectId > 0) masteryPicks.set(nodeId, effectId);
		}
		const t = params.get("t");
		if (t) {
			const dec = decodeTree(t);
			if (dec) {
				currentClassId = dec.classId;
				currentAscend = dec.ascend;
				currentBloodline = dec.bloodline;
				for (const id of dec.nodes) highlighted.add(id);
				// 같은 효과가 중복된 URL(옛 링크/손댄 링크)은 첫 것만 살린다 — 게임 규칙상 효과는 1회뿐
				const seenEffects = new Set<number>();
				for (const clusterId of dec.clusters || []) pendingClusterNodes.push(clusterId);
				for (const m of dec.masteries) {
					if (seenEffects.has(m.effect)) continue;
					seenEffects.add(m.effect);
					masteryPicks.set(m.node, m.effect);
				}
				return;
			}
		}
		for (const token of (params.get("nodes") || "").split(",")) {
			const id = Number(token);
			if (Number.isFinite(id) && id > 0) highlighted.add(id);
		}
		// 레거시 ?nodes= 로 들어올 때(시뮬 결과 링크 등) 직업/전직도 함께 복원한다.
		// 안 하면 사이온(0)으로 남아 루트가 어긋나고, 편집 시 고아 정리가 트리를 통째로 날린다.
		pendingClass = params.get("class");
		pendingAscend = params.get("asc");
	}
	// 클러스터는 트리 데이터 로드 후에야 생성할 수 있어 보류했다가 적용한다
	const pendingClusters: Array<{ socketId: number; sizeName: string; nodeCount: number; skillKey: string; notables: string[]; socketCount: number }> = [];
	const pendingClusterNodes: number[] = [];
	let pendingClass: string | null = null;
	let pendingAscend: string | null = null;
	// 트리 데이터 로드 후에만 이름→id 해석이 가능하므로 loadTree 에서 호출한다.
	function applyPendingClass() {
		if (pendingClass) {
			const numeric = Number(pendingClass);
			if (Number.isInteger(numeric) && numeric >= 0 && numeric <= 6) {
				currentClassId = numeric;
			} else {
				for (const [name, id] of Object.entries(CLASS_NAME_CLASSID)) {
					if (name.toLowerCase() === pendingClass.toLowerCase()) currentClassId = id;
				}
			}
		}
		if (pendingAscend) {
			const list = classAsc[currentClassId] || [];
			const idx = list.findIndex((a) => a.toLowerCase() === pendingAscend!.toLowerCase());
			if (idx >= 0) currentAscend = idx + 1;
		}
		pendingClass = null;
		pendingAscend = null;
	}
	loadFromUrl();
	// URL 을 현재 할당 상태로 갱신(실시간 반영). 편집 모드에서만.
	function syncUrl() {
		if (!interactive) return;
		const params = new URLSearchParams(globalThis.location.search);
		if (isAtlas) {
			// 아틀라스는 GGG 클래스 개념이 없어 콤마 id 로 동기화
			params.set("nodes", Array.from(highlighted).join(","));
			params.delete("t");
		} else {
			params.set("t", encodeTree());
			params.delete("nodes");
			// 주얼은 GGG 인코딩에 자리가 없다(공홈은 주얼을 URL 로 안 옮김) → 별도 파라미터로 우리끼리 보존.
			// 이게 없으면 새로고침·링크 공유에서 꽂아둔 주얼이 통째로 사라진다.
			const socketed = Array.from(jewelPicks)
				.filter(([nodeId]) => highlighted.has(nodeId))
				.map(([nodeId, slug]) => nodeId + ":" + slug);
			if (socketed.length) params.set("j", socketed.join(","));
			else params.delete("j");
			// 클러스터 주얼 구성(c=소켓:크기:노드수:스킬키) — GGG URL 엔 주얼 자체가 안 들어가므로
			// 이게 없으면 링크를 열었을 때 생성 노드가 아예 없어 클러스터 할당이 통째로 사라진다.
			const clusterConf = clusterConfEntries();
			if (clusterConf.length) params.set("c", clusterConf.join(","));
			else params.delete("c");
			// 문신(tt=) — 이름에 공백이 있어 '|' 로 잇는다. 할당된 노드의 것만 남긴다(게임에서도 효과 없음).
			const inked = Array.from(tattooPicks)
				.filter(([nodeId]) => highlighted.has(nodeId))
				.map(([nodeId, dn]) => nodeId + ":" + dn);
			if (inked.length) params.set("tt", inked.join("|"));
			else params.delete("tt");
		}
		globalThis.history.replaceState(null, "", globalThis.location.pathname + "?" + params.toString());
	}
	const hasHighlight = () => highlighted.size > 0;

	// 할당의 기준점 — 패시브는 현재 직업의 시작 노드, 아틀라스는 지도 중앙 시작 노드.
	function rootNode(): number | undefined {
		return isAtlas ? (atlasRoot ?? undefined) : classStartByClassId.get(currentClassId);
	}

	// ---- 클릭 할당(연결성 검증) ----
	// 루트(클래스 시작노드)에서 할당 노드만 따라 BFS → 도달 못하는 할당노드(고아) 제거.
	function pruneOrphans() {
		const root = rootNode();
		if (root === undefined) return;
		const reach = reachableSet(root, -1);
		for (const id of Array.from(highlighted)) if (!reach.has(id)) highlighted.delete(id);
		highlighted.add(root);
	}
	// 루트에서 할당 노드만 따라 도달 가능한 집합(제외 노드 하나를 끊어 볼 수 있다).
	function reachableSet(root: number, excluded: number): Set<number> {
		const reach = new Set<number>([root]);
		const queue = [root];
		while (queue.length) {
			const cur = queue.shift() as number;
			for (const nb of pathNeighbors(cur)) {
				if (nb !== excluded && highlighted.has(nb) && !reach.has(nb)) {
					reach.add(nb);
					queue.push(nb);
				}
			}
		}
		return reach;
	}
	// 할당 가능 = 인접에 이미 할당된 노드가 있음(루트까지 사슬 연결).
	function canAllocate(node: TreeNode): boolean {
		if (node.type === "class" || !nodeVisible(node)) return false;
		for (const nb of pathNeighbors(node.id)) if (highlighted.has(nb)) return true;
		return false;
	}
	// 이 할당 노드를 해제하면 함께 떨어져 나가는 노드 집합(자기 자신 포함).
	function computeRemoval(targetId: number): number[] {
		if (!highlighted.has(targetId)) return [];
		const node = nodeById.get(targetId);
		if (!node || node.type === "class") return [];
		const root = rootNode();
		if (root === undefined) return [targetId];
		const reach = reachableSet(root, targetId);
		const out = [targetId];
		for (const id of highlighted) if (id !== targetId && !reach.has(id)) out.push(id);
		return out;
	}
	function toggleNode(node: TreeNode) {
		if (!interactive || node.type === "class" || !nodeVisible(node)) return;
		// 마스터리는 "어떤 효과를 쓸지" 를 골라야 찍힌다 — 할당 가능하면 선택 팝업을 띄운다.
		// 마스터리는 "어떤 효과를 쓸지" 를 골라야 찍힌다 — 도달 가능하면 선택 팝업을 띄운다.
		// (마스터리 이웃은 전부 같은 그룹이라, 도달 = 그룹 패시브를 이미 지남 = 게임 규칙 자동 충족)
		if (node.masteryEffects?.length && !highlighted.has(node.id)) {
			if (canAllocate(node) || computeHoverPath(node.id).length > 1) openMasteryPicker(node);
			return;
		}
		const before = snapshot();
		if (highlighted.has(node.id)) {
			highlighted.delete(node.id);
			masteryPicks.delete(node.id);
			pruneOrphans();
		} else if (canAllocate(node)) {
			highlighted.add(node.id);
		} else {
			// 인접하지 않은 먼 노드 — 최단 경로를 따라 중간 노드까지 한 번에 할당(공식 뷰어 동작)
			const path = computeHoverPath(node.id);
			if (path.length < 2) return; // 도달 불가
			for (const id of path) highlighted.add(id);
		}
		commit(before);
		refreshHoverState(); // 커서가 그대로 노드 위에 있으므로 경로/해제 미리보기를 다시 계산
		updatePoints();
		syncUrl();
		draw();
	}
	// ---- 마스터리 효과 선택 팝업 (공식 뷰어와 동일하게 효과 하나를 골라야 할당된다) ----
	const effectLines = (eff: { stats: string[]; statsKo?: string[] }) => (isKorean && eff.statsKo?.length ? eff.statsKo : eff.stats);
	let masteryPicker: HTMLElement | null = null;
	function closeMasteryPicker() {
		masteryPicker?.remove();
		masteryPicker = null;
	}
	function openMasteryPicker(node: TreeNode) {
		closeMasteryPicker();
		const host = canvas.parentElement as HTMLElement;
		const panel = document.createElement("div");
		panel.className =
			"absolute z-20 max-h-[60%] w-80 overflow-y-auto rounded shadow-2xl border border-amber-700/60 bg-stone-900/97";
		const head = document.createElement("div");
		head.className =
			"sticky top-0 border-y-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-sm px-6 py-1.5";
		head.textContent = (isKorean && node.nameKo ? node.nameKo : node.name) + (isKorean ? " — 효과 선택" : " — pick effect");
		panel.appendChild(head);
		// 게임 규칙: 같은 마스터리 효과는 트리 전체에서 한 번만 고를 수 있다.
		// (효과 353개가 전부 여러 노드에 중복 존재해서, 막지 않으면 같은 효과를 5번까지 찍어 스탯이 부풀려진다)
		const takenElsewhere = new Set<number>();
		for (const [nodeId, effectId] of masteryPicks) {
			if (nodeId !== node.id && highlighted.has(nodeId)) takenElsewhere.add(effectId);
		}
		for (const eff of node.masteryEffects || []) {
			const row = document.createElement("button");
			row.type = "button";
			const taken = takenElsewhere.has(eff.id);
			row.disabled = taken;
			row.className = taken
				? "block w-full text-left px-4 py-2 text-xs leading-5 border-b border-stone-800 text-base-content/30 line-through cursor-not-allowed"
				: "block w-full text-left px-4 py-2 text-xs text-sky-300 leading-5 hover:bg-amber-900/40 border-b border-stone-800";
			row.textContent = effectLines(eff).join("\n") + (taken ? (isKorean ? "  (이미 선택함)" : "  (already taken)") : "");
			if (taken) {
				panel.appendChild(row);
				continue;
			}
			row.addEventListener("click", () => {
				const before = snapshot();
				// 멀리 있으면 경로까지 함께 할당 — 일반 노드 클릭과 동일한 규칙
				if (!canAllocate(node)) for (const id of computeHoverPath(node.id)) highlighted.add(id);
				highlighted.add(node.id);
				masteryPicks.set(node.id, eff.id);
				commit(before);
				closeMasteryPicker();
				refreshHoverState();
				updatePoints();
				syncUrl();
				draw();
			});
			panel.appendChild(row);
		}
		const sx = node.x * scale + offsetX;
		const sy = node.y * scale + offsetY;
		panel.style.left = Math.max(0, Math.min(sx + 20, host.clientWidth - 330)) + "px";
		panel.style.top = Math.max(0, Math.min(sy, host.clientHeight - 120)) + "px";
		host.appendChild(panel);
		masteryPicker = panel;
		hideTooltip();
	}

	// ---- 유사 노드 강조 (스탯 문구가 같은 노드 = 숫자만 다른 같은 계열) ----
	// "생명력 최대치 8% 증가" 와 "생명력 최대치 5% 증가" 를 같은 것으로 보기 위해 숫자를 # 로 정규화.
	const normalizeStat = (line: string) => line.replace(/[+\-]?\d+(?:\.\d+)?/g, "#").trim().toLowerCase();
	const statLinesOf = (node: TreeNode) => (isKorean && node.statsKo?.length ? node.statsKo : node.stats);
	function applySimilar(node: TreeNode): number {
		searchHits.clear();
		const want = new Set(statLinesOf(node).map(normalizeStat).filter(Boolean));
		if (want.size) {
			for (const other of nodes) {
				if (other.type === "class" || !nodeVisible(other)) continue;
				for (const line of statLinesOf(other)) {
					if (want.has(normalizeStat(line))) {
						searchHits.add(other.id);
						break;
					}
				}
			}
		}
		draw();
		return searchHits.size;
	}

	// ---- 노드 우클릭 메뉴 ----
	let nodeMenu: HTMLElement | null = null;
	function closeNodeMenu() {
		nodeMenu?.remove();
		nodeMenu = null;
	}
	function openNodeMenu(node: TreeNode, clientX: number, clientY: number) {
		closeNodeMenu();
		closeMasteryPicker();
		const host = canvas.parentElement as HTMLElement;
		const panel = document.createElement("div");
		panel.className = "absolute z-30 w-56 rounded shadow-2xl border border-amber-700/60 bg-stone-900/97 overflow-hidden";
		const head = document.createElement("div");
		head.className =
			"border-b-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-xs px-4 py-1";
		head.textContent = isKorean && node.nameKo ? node.nameKo : node.name;
		panel.appendChild(head);

		const item = (label: string, action: () => void, disabled = false) => {
			const row = document.createElement("button");
			row.type = "button";
			row.disabled = disabled;
			row.className =
				"block w-full text-left px-4 py-1.5 text-xs border-b border-stone-800 " +
				(disabled ? "text-base-content/30 cursor-not-allowed" : "text-sky-300 hover:bg-amber-900/40");
			row.textContent = label;
			if (!disabled) {
				row.addEventListener("click", () => {
					closeNodeMenu();
					action();
				});
			}
			panel.appendChild(row);
		};

		const allocated = highlighted.has(node.id);
		const hasStats = statLinesOf(node).some((l) => l.trim());
		item(isKorean ? "유사 노드 강조" : "Highlight similar", () => applySimilar(node), !hasStats);
		// 클러스터가 만든 노드에서도 그 주얼을 바로 손볼 수 있게 — 소켓까지 찾아가지 않아도 된다.
		// 주얼 구성은 **할당 여부와 무관**하므로 할당/미할당 분기 바깥에 둔다(미할당 생성 노드에서도 필요).
		const ownerSocket = clusterOwner.get(node.id);
		if (ownerSocket !== undefined && nodeById.get(ownerSocket)) {
			const ownerPlan = clusterPicks.get(ownerSocket);
			item(
				isKorean ? `클러스터 구성 변경 (${ownerPlan?.nodeCount ?? "?"}노드)` : `Change this cluster (${ownerPlan?.nodeCount ?? "?"})`,
				() => openClusterPicker(nodeById.get(ownerSocket)!),
			);
			item(isKorean ? "클러스터 제거" : "Remove cluster", () => {
				const before = snapshot();
				clusterPicks.delete(ownerSocket);
				rebuildClusterNodes();
				updatePoints();
				commit(before);
				syncUrl();
				draw();
			});
		}
		if (node.type !== "class") {
			if (allocated) {
				const cost = computeRemoval(node.id).length;
				item((isKorean ? "여기부터 해제" : "Refund from here") + (cost > 1 ? ` (−${cost})` : ""), () => toggleNode(node));
				if (node.masteryEffects?.length) item(isKorean ? "효과 변경" : "Change effect", () => openMasteryPicker(node));
				// 클러스터 소켓: 주얼 구성을 골라 서브트리를 생성한다(렌더 우선, 할당/URL 은 다음 단계)
				if (node.type === "jewel" && node.expansionJewel && !isAtlas && clusterDefs) {
					const cur = clusterPicks.get(node.id);
					item(
						cur ? (isKorean ? `클러스터 변경 (${cur.nodeCount}노드)` : `Change cluster (${cur.nodeCount})`) : isKorean ? "클러스터 주얼 장착" : "Socket cluster jewel",
						() => openClusterPicker(node),
					);
					if (cur) {
						item(isKorean ? "클러스터 제거" : "Remove cluster", () => {
							const before = snapshot();
							clusterPicks.delete(node.id);
							rebuildClusterNodes();
							updatePoints();
							commit(before);
							syncUrl();
							draw();
						});
					}
				}
				// 문신 — 할당한 패시브를 다른 노드로 교체한다. 소형은 속성 종류가 맞아야 새길 수 있다.
				if (!isAtlas && tattooTarget(node)) {
					const inked = tattooPicks.get(node.id);
					item(
						inked
							? (isKorean ? "문신 변경: " : "Change tattoo: ") + tattooLabel(inked)
							: isKorean
								? "문신 새기기"
								: "Apply tattoo",
						() => openTattooPicker(node),
					);
					if (inked) {
						item(isKorean ? "문신 지우기" : "Remove tattoo", () => {
							const before = snapshot();
							tattooPicks.delete(node.id);
							commit(before);
							markEvalStale(true);
							syncUrl();
							draw();
						});
					}
				}
				// 할당한 주얼 슬롯에만 장착을 허용한다 — 미할당 소켓의 주얼은 게임에서 효과가 없다
				if (node.type === "jewel" && !node.expansionJewel && !isAtlas) {
					const cur = jewelPicks.get(node.id);
					item(
						cur ? (isKorean ? "주얼 변경: " : "Change jewel: ") + jewelName(cur) : isKorean ? "주얼 장착" : "Socket jewel",
						() => openJewelPicker(node),
					);
					// 반경 주얼을 꽂았으면 그 반경 안 패시브를 한 번에 문신으로 — 반경 변환(붉은 악몽 등)을 노린 실전 조작
					const radiusTargets = radiusTattooTargets(node.id);
					if (radiusTargets.length) {
						item(
							isKorean ? `반경 내 문신 일괄 (${radiusTargets.length}개)` : `Bulk tattoo in radius (${radiusTargets.length})`,
							() => loadTattoos(() => renderTattooPicker(node, radiusTargets)),
						);
					}
				}
			} else {
				const path = computeHoverPath(node.id);
				const reachable = canAllocate(node) || path.length > 1;
				const cost = canAllocate(node) ? 1 : path.length - 1;
				item(
					(isKorean ? "여기까지 할당" : "Allocate to here") + (reachable ? ` (+${cost})` : ""),
					() => toggleNode(node),
					!reachable,
				);
			}
		}
		item(isKorean ? "강조 해제" : "Clear highlight", () => {
			searchHits.clear();
			draw();
		});

		const rect = canvas.getBoundingClientRect();
		panel.style.left = Math.max(0, Math.min(clientX - rect.left, host.clientWidth - 230)) + "px";
		panel.style.top = Math.max(0, Math.min(clientY - rect.top, host.clientHeight - 140)) + "px";
		host.appendChild(panel);
		nodeMenu = panel;
		hideTooltip();
	}

	// 현재 호버 중인 노드 기준으로 미리보기를 다시 계산한다(할당 직후에도 즉시 반영).
	function refreshHoverState() {
		hoverPath = [];
		removalSet.clear();
		if (!hovered || !interactive) return;
		if (highlighted.has(hovered.id)) for (const id of computeRemoval(hovered.id)) removalSet.add(id);
		else hoverPath = computeHoverPath(hovered.id);
	}

	// ---- 포인트 카운터 ----
	// 패시브/전직 포인트를 따로 센다(공식: 패시브 123, 전직 8). 클래스 시작·전직 시작 노드는 무료.
	function updatePoints() {
		updateStatsPanel();
		// 트리가 평가 시점과 달라졌으면 결과 패널을 낡음 처리(값 자체는 남겨 비교는 가능하게)
		if (lastEvalSignature !== null) markEvalStale(lastEvalSignature !== snapshot());
		const el = document.getElementById("poeTreePoints");
		if (!el) return;
		let passive = 0;
		let asc = 0;
		const root = rootNode();
		for (const id of highlighted) {
			const node = nodeById.get(id);
			if (!node || node.type === "class" || node.ascendancyStart) continue;
			if (id === root) continue; // 시작 노드는 무료(아틀라스 중앙 시작점 포함)
			if (node.ascendancy) asc++;
			else passive++;
		}
		el.replaceChildren();
		const add = (label: string, used: number, max: number) => {
			const span = document.createElement("span");
			span.className = "font-mono" + (max > 0 && used > max ? " text-error font-bold" : "");
			span.textContent = label + " " + used + (max > 0 ? " / " + max : "");
			el.appendChild(span);
		};
		add(isKorean ? "포인트" : "Points", passive, maxPoints);
		if (!isAtlas && maxAscPoints > 0) add(isKorean ? "· 전직" : "· Asc", asc, maxAscPoints);
		// 트리 상한 123 은 만렙(100) 기준인데 평가/최적화는 레벨 90(=퀘스트 24 + 레벨업 89 = 113포인트)이다.
		// 113 을 넘으면 그 레벨에선 못 찍는 트리라 계산이 과대평가된다 → 필요 레벨을 표시해 알린다.
		if (!isAtlas && passive > EVAL_POINT_BUDGET) {
			const needLevel = Math.min(100, passive - QUEST_POINTS + 1);
			const warn = document.createElement("span");
			warn.className = "font-mono text-warning";
			warn.textContent = isKorean
				? `· 레벨 ${needLevel} 필요 (평가는 90)`
				: `· needs level ${needLevel} (eval at 90)`;
			warn.title = isKorean
				? "평가는 레벨 90(포인트 113) 기준이라 초과분은 실제로는 찍을 수 없습니다."
				: "Evaluation assumes level 90 (113 points); the excess is not actually obtainable.";
			el.appendChild(warn);
		}
	}

	// ---- 할당 스탯 합계 ----
	// 같은 계열 문장(숫자만 다름)을 하나로 묶어 수치를 더한다. 마스터리는 "고른 효과"만 반영.
	const NUM_RE = /[+\-]?\d+(?:\.\d+)?/g;
	function aggregateStats(): { text: string; count: number }[] {
		// 문장을 [고정문구 조각들] + [숫자들] 로 쪼개 조각열을 키로 묶고 숫자만 더한다(자리표시자 문자 불필요).
		const acc = new Map<string, { parts: string[]; nums: number[]; signed: boolean[]; count: number }>();
		const add = (lines: string[]) => {
			for (const raw of lines) {
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
					nums.forEach((n, i) => (cur.nums[i] = (cur.nums[i] || 0) + n));
				} else {
					acc.set(key, { parts, nums, signed, count: 1 });
				}
			}
		};
		for (const id of highlighted) {
			const node = nodeById.get(id);
			if (!node || node.type === "class") continue;
			// 문신이 새겨진 노드는 **교체**된 것 — 마스터리(룬 접합 포함)보다 먼저 봐야 요약이 실제 계산과 일치한다
			const inked = tattooPicks.get(id);
			if (inked) {
				add(tattooLines(inked));
				continue;
			}
			if (node.masteryEffects?.length) {
				const pick = masteryPicks.get(id);
				const eff = pick !== undefined ? node.masteryEffects.find((e) => e.id === pick) : undefined;
				if (eff) add(effectLines(eff));
				continue;
			}
			add(statLinesOf(node));
		}
		// 꽂아둔 주얼의 모드도 합계에 넣는다 — 주얼은 트리의 일부인데 요약에서 빠지면
		// "평가 결과는 올랐는데 스탯 요약은 그대로"라 어디서 온 수치인지 알 수 없다.
		for (const [nodeId, slug] of jewelPicks) {
			if (!highlighted.has(nodeId)) continue;
			const option = document.querySelector<HTMLOptionElement>('#poeTreeJewelList option[data-slug="' + slug + '"]');
			const mods = option?.dataset.mods;
			if (mods) add(mods.split(" / "));
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
	// 할당한 키스톤/노터블 목록 — 빌드의 정체성을 한눈에 보고, 눌러서 그 노드로 이동한다.
	// (스탯 합계만으로는 "무슨 키스톤을 찍었는지" 가 묻힌다)
	const NEWLINE = String.fromCharCode(10); // 툴팁(title)은 줄바꿈 문자로 여러 줄을 만든다
	function appendKeyNodes(panel: HTMLElement) {
		// 문신이 새겨진 노터블/키스톤은 다른 노드로 교체된 상태 — 원래 이름을 목록에 남기지 않는다
		const picked = nodes.filter(
			(n) => highlighted.has(n.id) && (n.type === "keystone" || n.type === "notable") && !tattooPicks.has(n.id),
		);
		const socketedJewels = Array.from(jewelPicks).filter(([nodeId]) => highlighted.has(nodeId));
		// 룬 접합(문신)이 새겨진 마스터리는 효과가 **교체로 소멸**한 상태 — 옛 효과 줄을 남기면 표시≠실제가 된다
		const pickedMasteries = Array.from(masteryPicks).filter(
			([nodeId]) => highlighted.has(nodeId) && !tattooPicks.has(nodeId),
		);
		// 꽂아둔 클러스터 주얼도 빌드 정체성이다 — 어떤 주얼을 몇 노드로 꽂았는지 목록에 함께 보인다
		const socketedClusters = Array.from(clusterPicks).filter(([nodeId]) => highlighted.has(nodeId));
		// 노터블이 없어도 마스터리/주얼만 있으면 목록을 보여준다(예전엔 조기 return 해 그 줄들이 통째로 사라졌다)
		if (!picked.length && !socketedJewels.length && !pickedMasteries.length && !socketedClusters.length) return;
		picked.sort((a, b) => (a.type === b.type ? 0 : a.type === "keystone" ? -1 : 1));
		const head = document.createElement("div");
		head.className = "px-3 pt-2 pb-1 text-[10px] uppercase tracking-wide text-base-content/40";
		const total = picked.length + pickedMasteries.length + socketedJewels.length + socketedClusters.length;
		head.textContent = isKorean ? `핵심 노드 ${total}개` : `Key nodes (${total})`;
		panel.appendChild(head);
		for (const n of picked) {
			const row = document.createElement("button");
			row.type = "button";
			row.className =
				"block w-full text-left px-3 py-0.5 text-xs hover:bg-base-200/60 " +
				(n.type === "keystone" ? "text-amber-300 font-semibold" : "text-base-content/80");
			row.textContent = (n.type === "keystone" ? "◆ " : "• ") + (isKorean && n.nameKo ? n.nameKo : n.name);
			// 이름만으론 무슨 효과인지 알 수 없다 — 마우스만 올려도 스탯을 보여준다(캔버스로 찾아갈 필요 없이)
			const stats = statLinesOf(n).filter((line) => line.trim());
			row.title =
				(stats.length ? stats.join("\n") + "\n\n" : "") +
				(isKorean ? "클릭하면 해당 노드로 이동" : "Click to jump to node");
			row.addEventListener("click", () => {
				centerOnNode(n.id, 0.35);
				draw();
			});
			panel.appendChild(row);
		}
		// 고른 마스터리 효과도 빌드 정체성의 일부다 — 어떤 효과를 골랐는지 목록에서 바로 보이게
		for (const [nodeId, effectId] of masteryPicks) {
			// 룬 접합이 새겨진 마스터리는 효과가 교체로 소멸 — 옛 효과 줄을 남기면 표시≠실제
			if (!highlighted.has(nodeId) || tattooPicks.has(nodeId)) continue;
			const node = nodeById.get(nodeId);
			const eff = node?.masteryEffects?.find((e) => e.id === effectId);
			if (!node || !eff) continue;
			const row = document.createElement("button");
			row.type = "button";
			row.className = "block w-full text-left px-3 py-0.5 text-xs text-sky-300 hover:bg-base-200/60";
			const lines = effectLines(eff);
			row.textContent = "◇ " + (isKorean && node.nameKo ? node.nameKo : node.name) + " — " + (lines[0] || "");
			row.title = lines.join("\n") + "\n\n" + (isKorean ? "클릭하면 해당 노드로 이동" : "Click to jump to node");
			row.addEventListener("click", () => {
				centerOnNode(nodeId, 0.35);
				draw();
			});
			panel.appendChild(row);
		}
		// 꽂아둔 주얼도 같은 목록에 — 클릭하면 그 소켓으로 이동
		for (const [nodeId, slug] of socketedJewels) {
			const row = document.createElement("button");
			row.type = "button";
			row.className = "block w-full text-left px-3 py-0.5 text-xs text-emerald-300 hover:bg-base-200/60";
			row.textContent = "◈ " + jewelName(slug);
			const mods = document.querySelector<HTMLOptionElement>('#poeTreeJewelList option[data-slug="' + slug + '"]')?.dataset.mods;
			row.title =
				(mods ? mods.split(" / ").join("\n") + "\n\n" : "") +
				(isKorean ? "클릭하면 해당 소켓으로 이동" : "Click to jump to socket");
			row.addEventListener("click", () => {
				centerOnNode(nodeId, 0.35);
				draw();
			});
			panel.appendChild(row);
		}
		// 클러스터 주얼 — 크기/노드 수/효과, 노터블까지 한 줄로. 클릭하면 그 소켓으로 이동한다.
		for (const [nodeId, plan] of socketedClusters) {
			const skill = clusterDefs?.[`${plan.sizeName} Cluster Jewel`]?.skills?.[plan.skillKey] as
				| { name?: string; stats?: string[]; statsKo?: string[] }
				| undefined;
			const sizeKo = CLUSTER_SIZE_KO[CLUSTER_SIZE_NAME.indexOf(plan.sizeName)] || plan.sizeName;
			const notableNames = (plan.notables || []).map((n) => (isKorean && clusterNotables.get(n)?.nameKo) || n);
			const row = document.createElement("button");
			row.type = "button";
			row.className = "block w-full text-left px-3 py-0.5 text-xs text-purple-300 hover:bg-base-200/60";
			row.textContent =
				"❖ " +
				(isKorean ? sizeKo : plan.sizeName) +
				" " +
				plan.nodeCount +
				(isKorean ? "노드" : " nodes") +
				(skill?.name ? " — " + skill.name : "") +
				(notableNames.length ? " (" + notableNames.join(", ") + ")" : "");
			const effectLine = (isKorean && skill?.statsKo?.length ? skill.statsKo : skill?.stats) || [];
			row.title =
				(effectLine.length ? effectLine.join(NEWLINE) + NEWLINE + NEWLINE : "") +
				(plan.socketCount ? (isKorean ? `주얼 소켓 ${plan.socketCount}개` : `${plan.socketCount} jewel sockets`) + NEWLINE + NEWLINE : "") +
				(isKorean ? "클릭하면 해당 소켓으로 이동" : "Click to jump to socket");
			row.addEventListener("click", () => {
				centerOnNode(nodeId, 0.35);
				draw();
			});
			panel.appendChild(row);
		}
		const sep = document.createElement("div");
		sep.className = "border-b border-base-300 my-1";
		panel.appendChild(sep);
	}
	function updateStatsPanel() {
		const panel = document.getElementById("poeTreeStatsBody");
		if (!panel) return;
		const rows = aggregateStats();
		panel.replaceChildren();
		appendKeyNodes(panel);
		if (!rows.length) {
			const empty = document.createElement("div");
			empty.className = "text-xs text-base-content/40 px-3 py-2";
			empty.textContent = isKorean ? "할당한 노드가 없습니다." : "No allocated nodes.";
			panel.appendChild(empty);
			return;
		}
		for (const row of rows) {
			const line = document.createElement("div");
			line.className = "flex items-baseline gap-2 px-3 py-0.5 text-xs";
			const text = document.createElement("span");
			text.className = "text-sky-300 flex-1";
			text.textContent = row.text;
			line.appendChild(text);
			if (row.count > 1) {
				const badge = document.createElement("span");
				badge.className = "text-[10px] font-mono text-base-content/40 shrink-0";
				badge.textContent = "×" + row.count;
				line.appendChild(badge);
			}
			panel.appendChild(line);
		}
	}

	// 전직/혈맹의 한글 표시명 — 트리 데이터엔 영문 id 뿐이라, 그 서브트리 시작 노드의 nameKo 를 쓴다
	// ("Guardian" → "가디언", "Aul" → "아울 혈맹"). 시작 노드가 없거나 영문 로케일이면 영문 그대로.
	const ascLabelCache = new Map<string, string>();
	function ascendancyLabel(ascId: string, fallback?: string): string {
		const cached = ascLabelCache.get(ascId);
		if (cached) return cached;
		let label = fallback || ascId;
		if (isKorean) {
			const start = nodes.find((n) => n.ascendancy === ascId && n.ascendancyStart);
			if (start?.nameKo) {
				// 시작 노드 이름이 전직명과 다른 경우(Reliquarian→Scavenger, Warden→Warden of the Maji)
				// 한글명만 쓰면 정작 전직 이름이 사라져 사용자가 못 찾는다 → 전직명을 앞에 둔다.
				label = start.name === ascId ? start.nameKo : ascId + " (" + start.nameKo + ")";
			}
		}
		ascLabelCache.set(ascId, label);
		return label;
	}

	// ---- B: 직업/전직/혈맹 선택 ----
	function centerOnNode(id: number, atScale = 0.11) {
		const n = nodeById.get(id);
		if (!n) return;
		scale = atScale;
		offsetX = canvas.clientWidth / 2 - n.x * scale;
		offsetY = canvas.clientHeight / 2 - n.y * scale;
	}
	function fillAscendOptions(sel: HTMLSelectElement) {
		sel.replaceChildren();
		const none = document.createElement("option");
		none.value = "0";
		none.textContent = isKorean ? "전직 없음" : "No ascendancy";
		sel.appendChild(none);
		(classAsc[currentClassId] || []).forEach((asc, i) => {
			const o = document.createElement("option");
			o.value = String(i + 1);
			o.textContent = ascendancyLabel(asc);
			sel.appendChild(o);
		});
		sel.value = String(currentAscend);
	}
	function fillBloodlineOptions(sel: HTMLSelectElement) {
		sel.replaceChildren();
		const none = document.createElement("option");
		none.value = "0";
		none.textContent = isKorean ? "혈맹 없음" : "No bloodline";
		sel.appendChild(none);
		bloodlines.forEach((bl, i) => {
			const o = document.createElement("option");
			o.value = String(i + 1);
			o.textContent = ascendancyLabel(bl.id, bl.name);
			sel.appendChild(o);
		});
		sel.value = String(currentBloodline);
	}
	// 전직/혈맹을 바꾸면 이전 서브트리에 찍어둔 노드는 화면에서 사라지므로 함께 해제한다.
	function dropHiddenAllocations() {
		for (const id of Array.from(highlighted)) {
			const node = nodeById.get(id);
			if (node && !nodeVisible(node)) highlighted.delete(id);
		}
		pruneOrphans();
	}
	// ---- 실행취소 / 다시실행 (공식 뷰어와 동일: Ctrl+Z / Ctrl+Shift+Z) ----
	// 경로 자동할당은 한 번에 10개 넘게 찍히기도 해서, 되돌리기 없이는 실수 복구가 사실상 불가능하다.
	// 스냅샷은 "직업/전직/혈맹 + 할당집합 + 마스터리 선택" 전체 — 부분 되돌리기는 상태가 어긋난다.
	function snapshot(): string {
		// 클러스터 구성도 포함해야 한다 — 빠지면 주얼 장착/제거가 **실행취소 기록 자체를 안 남기고**(스냅샷이 같아 commit 이 조기 return)
		// 되돌릴 때도 옛 서브트리가 그대로 남는다.
		return JSON.stringify([
			currentClassId,
			currentAscend,
			currentBloodline,
			Array.from(highlighted),
			Array.from(masteryPicks),
			Array.from(jewelPicks),
			Array.from(clusterPicks),
			Array.from(tattooPicks),
		]);
	}
	const undoStack: string[] = [];
	const redoStack: string[] = [];
	// 변경 "직전" 스냅샷을 넘겨 호출 — 실제로 달라졌을 때만 기록하므로 조기 return 경로는 스택을 더럽히지 않는다.
	function commit(before: string) {
		if (!interactive || before === snapshot()) return;
		undoStack.push(before);
		if (undoStack.length > 100) undoStack.shift();
		redoStack.length = 0;
		updateHistoryButtons();
	}
	function restoreSnapshot(s: string) {
		const [c, a, b, n, m, j, cl, tt] = JSON.parse(s) as [
			number,
			number,
			number,
			number[],
			[number, number][],
			[number, string][],
			[number, { sizeName: string; nodeCount: number; skillKey: string; notables: string[]; socketCount: number }][],
			[number, string][],
		];
		currentClassId = c;
		currentAscend = a;
		currentBloodline = b;
		masteryPicks.clear();
		for (const [k, v] of m) masteryPicks.set(k, v);
		jewelPicks.clear();
		for (const [k, v] of j || []) jewelPicks.set(k, v);
		tattooPicks.clear();
		for (const [k, v] of tt || []) tattooPicks.set(k, v);
		// 클러스터를 **먼저** 되돌려 서브트리를 다시 만든 뒤에 할당을 복원한다 —
		// 순서를 바꾸면 아직 존재하지 않는 생성 노드의 할당이 통째로 버려진다.
		clusterPicks.clear();
		for (const [k, v] of cl || []) clusterPicks.set(k, v);
		rebuildClusterNodes();
		highlighted.clear();
		for (const id of n) highlighted.add(id);
		// 셀렉트 박스도 함께 되돌린다 — 안 하면 표시와 실제 상태가 어긋난다.
		const classSel = document.getElementById("poeTreeClass") as HTMLSelectElement | null;
		if (classSel) classSel.value = String(currentClassId);
		const ascSel = document.getElementById("poeTreeAscend") as HTMLSelectElement | null;
		if (ascSel) fillAscendOptions(ascSel);
		const bloodSel = document.getElementById("poeTreeBloodline") as HTMLSelectElement | null;
		if (bloodSel) fillBloodlineOptions(bloodSel);
		hoverPath = [];
		removalSet.clear();
		updatePoints();
		syncUrl();
		draw();
		updateHistoryButtons();
	}
	function undo() {
		const s = undoStack.pop();
		if (s === undefined) return;
		redoStack.push(snapshot());
		restoreSnapshot(s);
	}
	function redo() {
		const s = redoStack.pop();
		if (s === undefined) return;
		undoStack.push(snapshot());
		restoreSnapshot(s);
	}
	function updateHistoryButtons() {
		const u = document.getElementById("poeTreeUndo") as HTMLButtonElement | null;
		const r = document.getElementById("poeTreeRedo") as HTMLButtonElement | null;
		if (u) u.disabled = undoStack.length === 0;
		if (r) r.disabled = redoStack.length === 0;
	}
	// 직업 변경 — PoB(PassiveSpec:SelectClass)와 동일하게 **시작 노드만 교체하고 나머지 할당은 유지**한다.
	// 새 시작점에서 연결이 끊긴 노드만 pruneOrphans 가 떨어뜨린다(예전엔 통째로 초기화해 작업이 날아갔다).
	function applyClass(classId: number) {
		const before = snapshot();
		const previousRoot = classStartByClassId.get(currentClassId);
		const previousCount = highlighted.size;
		currentClassId = classId;
		currentAscend = 0;
		if (previousRoot !== undefined) highlighted.delete(previousRoot);
		// 예전 버전에서 경로에 끼어든 다른 직업 시작 노드가 남아 있을 수 있다 — 전부 정리
		for (const id of Array.from(highlighted)) {
			if (nodeById.get(id)?.type === "class") highlighted.delete(id);
		}
		// 전직 서브트리 노드는 직업이 바뀌면 존재하지 않으므로 함께 정리
		for (const id of Array.from(highlighted)) {
			if (nodeById.get(id)?.ascendancy) highlighted.delete(id);
		}
		const root = classStartByClassId.get(classId);
		if (root !== undefined) {
			highlighted.add(root);
			centerOnNode(root);
		}
		pruneOrphans();
		if (previousCount > 1) {
			// 몇 개가 살아남았는지 알려준다 — 조용히 사라지면 "왜 트리가 줄었지?" 가 된다
			const kept = highlighted.size - 1; // 시작 노드 제외
			const dropped = previousCount - 1 - kept;
			// 전용 공지 자리 — 검색 카운트를 덮어쓰면 둘이 서로를 지운다(검색 중 직업을 바꾸면 개수가 사라졌다)
			const el = document.getElementById("poeTreeNotice");
			if (el && dropped > 0) {
				el.textContent = isKorean
					? `직업 변경: ${kept}개 유지 · ${dropped}개 해제(연결 끊김)`
					: `Class changed: kept ${kept}, dropped ${dropped}`;
				globalThis.setTimeout(() => {
					if (el.textContent?.startsWith(isKorean ? "직업 변경" : "Class changed")) el.textContent = "";
				}, 8000);
			}
		}
		commit(before);
		updatePoints();
		syncUrl();
		draw();
	}
	// ---- D: 호버 최단경로 미리보기 (할당집합 → 호버 노드, 미할당 통과) ----
	function computeHoverPath(targetId: number): number[] {
		if (!interactive || highlighted.has(targetId) || highlighted.size === 0) return [];
		const prev = new Map<number, number>();
		const visited = new Set<number>(highlighted);
		let frontier = Array.from(highlighted);
		let depth = 0;
		while (frontier.length && depth < 60) {
			const next: number[] = [];
			for (const cur of frontier) {
				for (const nb of pathNeighbors(cur)) {
					if (visited.has(nb)) continue;
					// 마스터리는 경유지가 될 수 없다(효과를 골라야 찍히는 노드다) — API 의 isTraversable 과 같은 규칙.
					// 목표 자신은 예외: 마스터리를 직접 클릭하면 거기까지의 경로를 계산해야 한다.
					if (nb !== targetId && nodeById.get(nb)?.masteryEffects?.length) continue;
					visited.add(nb);
					prev.set(nb, cur);
					if (nb === targetId) {
						const path = [targetId];
						let p = prev.get(targetId);
						while (p !== undefined) {
							path.push(p);
							if (highlighted.has(p)) break;
							p = prev.get(p);
						}
						return path.reverse();
					}
					next.push(nb);
				}
			}
			frontier = next;
			depth++;
		}
		return [];
	}

	// ---- C: 검색 ----
	// 분류어 → 노드 타입. 공식 뷰어엔 없지만 범례에 쓰는 말이라 검색으로도 찾아지는 게 자연스럽다.
	const SEARCH_TYPE_WORDS: Record<string, string> = {
		키스톤: "keystone",
		keystone: "keystone",
		노터블: "notable",
		notable: "notable",
		주얼: "jewel",
		"주얼 슬롯": "jewel",
		jewel: "jewel",
		마스터리: "mastery",
		mastery: "mastery",
	};
	// 검색 순환 커서와 라벨 접미문구(숨김 안내) — Enter 순환 표시에서 함께 쓴다
	let searchCursor = 0;
	let searchCursorPrimed = false; // 첫 Enter 는 이미 이동해 있는 1번째를 가리켜야 한다(2번째로 건너뛰면 하나를 놓친다)
	let searchCountSuffix = "";
	function applySearch(query: string) {
		searchHits.clear();
		const q = query.trim().toLowerCase();
		if (q) {
			let first: TreeNode | null = null;
			for (const node of nodes) {
				if (node.type === "class") continue;
				const hay = (
					node.name +
					" " +
					(node.nameKo || "") +
					" " +
					node.stats.join(" ") +
					" " +
					(node.statsKo || []).join(" ")
				).toLowerCase();
				// 이름/스탯 텍스트 또는 분류어(키스톤·노터블·주얼·마스터리)로 찾는다.
				// 분류어는 노드 텍스트에 안 들어 있어서 예전엔 "키스톤" 검색이 0건이었다(사용자가 자연히 시도하는 말인데).
				if (hay.indexOf(q) !== -1 || SEARCH_TYPE_WORDS[q] === node.type) {
					searchHits.add(node.id);
					// 화면에 보이는 매치만 이동 대상 — 숨은(미선택 전직) 노드로 이동하면 빈 화면만 보인다
					if (!first && nodeVisible(node)) first = node;
				}
			}
			if (first) centerOnNode(first.id);
		}
		// 몇 개가 걸렸는지 보여준다 — 결과 수를 모르면 Enter 순환이 몇 번짜리인지도 알 수 없다
		const countEl = document.getElementById("poeTreeSearchCount");
		if (countEl) {
			const visible = nodes.filter((n) => searchHits.has(n.id) && nodeVisible(n)).length;
			// 전직을 안 고르면 그 서브트리 노드는 화면에 없어 검색에도 안 잡힌다 —
			// 그냥 "0개 일치"만 보여주면 사용자는 왜 없는지 알 수 없으므로 숨은 수를 함께 알린다.
			// 숨김 수는 "전직 미선택 때문"만 센다 — 프록시(자리표시자)는 애초에 존재하지 않는 노드라
			// 함께 세면 "전직 42개 숨김" 같은 거짓 안내가 된다.
			const hidden = nodes.filter((n) => searchHits.has(n.id) && !nodeVisible(n) && !n.isProxy).length;
			const hiddenText = hidden ? (isKorean ? ` (전직 ${hidden}개 숨김)` : ` (${hidden} hidden in ascendancies)`) : "";
			searchCountSuffix = hiddenText;
			searchCursor = 0; // 검색어가 바뀌면 순환도 처음부터
			searchCursorPrimed = false;
			countEl.textContent = q ? (isKorean ? `${visible}개 일치${hiddenText}` : `${visible} match${hiddenText}`) : "";
			countEl.className = "text-xs font-mono " + (hidden && !visible ? "text-warning" : "text-base-content/50");
			// 숨은 매치가 있으면 한 번에 그 전직으로 전환할 수 있게 — 안내만 하고 방법을 안 주면 결국 사용자가 찾아 헤맨다.
			// 현재 직업의 전직일 때만 제안한다(다른 직업이면 트리를 갈아엎게 되므로 직업명을 알려주기만).
			countEl.onclick = null;
			countEl.title = "";
			if (hidden) {
				const hiddenNode = nodes.find((n) => searchHits.has(n.id) && !nodeVisible(n) && n.ascendancy);
				const list = classAsc[currentClassId] || [];
				const index = hiddenNode ? list.indexOf(hiddenNode.ascendancy as string) : -1;
				if (index >= 0) {
					countEl.className += " cursor-pointer underline decoration-dotted";
					countEl.title = isKorean
						? `클릭하면 ${ascendancyLabel(list[index])} 전직을 선택해 보여줍니다`
						: `Click to switch to ${ascendancyLabel(list[index])}`;
					countEl.onclick = () => {
						const ascSel = document.getElementById("poeTreeAscend") as HTMLSelectElement | null;
						if (!ascSel) return;
						ascSel.value = String(index + 1);
						ascSel.dispatchEvent(new Event("change"));
						applySearch(query);
					};
				}
			}
		}
		draw();
	}

	function setupControls() {
		// 검색·전체화면은 아틀라스 포함 항상 배선
		const searchInput = document.getElementById("poeTreeSearch") as HTMLInputElement | null;
		if (searchInput) {
			searchInput.addEventListener("input", () => applySearch(searchInput.value));
			// Enter — 다음 매치로 이동(Shift+Enter 는 이전). 강조만으로는 화면 밖 매치를 찾아갈 수 없다.
			// 검색어가 바뀌면 커서를 0 으로 되돌린다 — 안 그러면 새 검색인데 엉뚱한 순번부터 시작한다.
			searchInput.addEventListener("keydown", (event) => {
				if (event.key !== "Enter") return;
				event.preventDefault();
				const matches = nodes.filter((n) => searchHits.has(n.id) && nodeVisible(n));
				if (!matches.length) return;
				matches.sort((a, b) => Number(highlighted.has(b.id)) - Number(highlighted.has(a.id)));
				if (!searchCursorPrimed && !event.shiftKey) {
					searchCursorPrimed = true; // applySearch 가 이미 1번째로 옮겨 놨다
				} else {
					searchCursorPrimed = true;
					searchCursor = (searchCursor + (event.shiftKey ? -1 : 1) + matches.length) % matches.length;
				}
				const target = matches[searchCursor];
				// "3/14" 로 지금 몇 번째인지 보여준다 — 순환 중 위치를 모르면 같은 곳을 맴돌게 된다
				const countEl = document.getElementById("poeTreeSearchCount");
				if (countEl) {
					countEl.textContent =
						(isKorean ? `${searchCursor + 1}/${matches.length}번째` : `${searchCursor + 1}/${matches.length}`) + searchCountSuffix;
				}
				centerOnNode(target.id, 0.35);
				draw();
			});
		}
		// 스탯 요약 패널 접기/펼치기
		const statsBtn = document.getElementById("poeTreeStatsToggle");
		const statsPanel = document.getElementById("poeTreeStats");
		if (statsBtn && statsPanel) {
			statsBtn.addEventListener("click", () => {
				statsPanel.classList.toggle("hidden");
				updateStatsPanel();
			});
		}
		// 트리 계산 — 찍은 노드를 그대로 PoB 엔진에 보내 실계산(장비/보조젬 없음)
		const evalBtn = document.getElementById("poeTreeEval");
		const evalPanel = document.getElementById("poeTreeEvalPanel");
		const evalBody = document.getElementById("poeTreeEvalBody");
		if (evalBtn && evalPanel && evalBody && !isAtlas) {
			const runEval = () => {
				const ids = Array.from(highlighted).filter((id) => {
					const n = nodeById.get(id);
					return n && n.type !== "class";
				});
				evalPanel.classList.remove("hidden");
				if (!ids.length) {
					evalBody.textContent = isKorean ? "할당한 노드가 없습니다." : "No allocated nodes.";
					return;
				}
				evalBody.textContent = isKorean ? "계산 중..." : "Calculating...";
				const body = new URLSearchParams();
				body.set("classId", String(currentClassId));
				body.set("nodes", ids.join(","));
				const asc = currentAscName();
				if (asc) body.set("ascendancy", asc);
				// 마스터리는 "어떤 효과를 골랐는지"까지 보내야 PoB 가 스탯에 반영한다
				const picks = Array.from(masteryPicks)
					.filter(([nodeId]) => highlighted.has(nodeId))
					.map(([nodeId, effectId]) => nodeId + ":" + effectId);
				if (picks.length) body.set("masteries", picks.join(","));
				// 주 스킬: datalist 에서 고른 표시명 → data-slug 로 변환(미입력이면 서버가 표준 스킬 사용)
				const gemInput = document.getElementById("poeTreeEvalGem") as HTMLInputElement | null;
				const typed = gemInput?.value.trim();
				if (typed) {
					const opt = document.querySelector<HTMLOptionElement>(
						'#poeTreeGemList option[value="' + typed.replace(/"/g, '\\"') + '"]',
					);
					if (opt?.dataset.slug) body.set("gem", opt.dataset.slug);
				}
				// 주얼: 소켓이 실제 할당된 것만 보낸다(서버도 한 번 더 거른다)
				const sockets = Array.from(jewelPicks)
					.filter(([nodeId]) => highlighted.has(nodeId))
					.map(([nodeId, slug]) => nodeId + ":" + slug);
				if (sockets.length) body.set("jewels", sockets.join(","));
				// 문신도 함께 — 없으면 화면엔 문신이 보이는데 계산은 원래 패시브로 돌아 수치가 어긋난다
				const inked = Array.from(tattooPicks)
					.filter(([nodeId]) => highlighted.has(nodeId))
					.map(([nodeId, dn]) => nodeId + ":" + dn);
				if (inked.length) body.set("tattoos", inked.join(","));
				// 클러스터 구성도 함께 — PoB 는 주얼 문구로 서브트리를 만들므로 이게 없으면
				// 우리가 찍은 생성 노드(id ≥ 65536)를 엔진이 "존재하지 않는 노드"로 무시한다.
				const clusterConf = clusterConfEntries();
				if (clusterConf.length) body.set("clusters", clusterConf.join(","));
				fetch("/poe/htmx/tree/stats", {
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: body.toString(),
				})
					.then((r) => r.text())
					.then((html) => {
						evalBody.innerHTML = html;
						annotateEvalDelta(evalBody);
					})
					.catch(() => {
						evalBody.textContent = isKorean ? "요청 실패" : "Request failed";
					});
			};
			evalBtn.addEventListener("click", runEval);
			document.getElementById("poeTreeEvalRerun")?.addEventListener("click", runEval);
			// 스킬 입력에서 엔터로도 재계산
			document.getElementById("poeTreeEvalGem")?.addEventListener("keydown", (e) => {
				if ((e as KeyboardEvent).key === "Enter") runEval();
			});
		}
		// 이 트리로 최적화 — 확정 트리를 시뮬 페이지로 넘긴다(거기서 트리 탐색을 건너뛰고 장비/보조젬만 최적화)
		const toSimBtn = document.getElementById("poeTreeToSim");
		if (toSimBtn && !isAtlas) {
			toSimBtn.addEventListener("click", () => {
				const ids = Array.from(highlighted).filter((id) => nodeById.get(id)?.type !== "class");
				if (!ids.length) return;
				// 마스터리 효과도 함께 넘긴다 — 빼먹으면 최적화기가 마스터리 스탯 없는 더 약한 트리를 평가한다
				const picks = Array.from(masteryPicks)
					.filter(([nodeId]) => highlighted.has(nodeId))
					.map(([nodeId, effectId]) => nodeId + ":" + effectId);
				// 직업/전직도 반드시 넘긴다 — 최적화기가 다른 직업을 고르면 이 트리는 그 시작점에서 연결되지 않아
				// PoB 가 대부분을 할당하지 않고 조용히 버린다(고정 트리가 통째로 무의미해짐).
				const className = Object.entries(CLASS_NAME_CLASSID).find(([, id]) => id === currentClassId)?.[0] || "";
				let url = "/poe/sim?treeNodes=" + encodeURIComponent(ids.join(","));
				if (picks.length) url += "&masteries=" + encodeURIComponent(picks.join(","));
				// 소켓에 꽂아둔 주얼도 인계 — 최적화기는 남은 소켓만 채운다
				const sockets = Array.from(jewelPicks)
					.filter(([nodeId]) => highlighted.has(nodeId))
					.map(([nodeId, slug]) => nodeId + ":" + slug);
				if (sockets.length) url += "&jewels=" + encodeURIComponent(sockets.join(","));
				// 클러스터 구성도 인계 — 없으면 최적화기가 생성 노드를 못 만들어 트리 화면보다 낮은 수치로 돈다
				const clusterConf = clusterConfEntries();
				if (clusterConf.length) url += "&clusters=" + encodeURIComponent(clusterConf.join(","));
				// 문신도 인계 — 없으면 최적화기가 원래 패시브로 계산해 트리 화면보다 낮은 수치가 나온다
				const inked = Array.from(tattooPicks)
					.filter(([nodeId]) => highlighted.has(nodeId))
					.map(([nodeId, dn]) => nodeId + ":" + dn);
				if (inked.length) url += "&tattoos=" + encodeURIComponent(inked.join("|"));
				if (className) url += "&className=" + encodeURIComponent(className);
				const ascName = currentAscName();
				if (ascName) url += "&ascendancy=" + encodeURIComponent(ascName);
				globalThis.location.href = url;
			});
		}
		const fsBtn = document.getElementById("poeTreeFullscreen");
		if (fsBtn) {
			fsBtn.addEventListener("click", () => {
				// 컨트롤 바까지 포함한 껍데기를 전체화면으로 — 캔버스만 넣으면 전체화면에서 조작이 불가능하다
				const box = document.getElementById("poeTreeShell") || canvas.parentElement || canvas;
				if (document.fullscreenElement) document.exitFullscreen();
				else if (box.requestFullscreen) box.requestFullscreen();
			});
		}
		if (!interactive) return; // 이하 할당 관련은 편집(패시브 트리)만
		const classSel = document.getElementById("poeTreeClass") as HTMLSelectElement | null;
		const ascSel = document.getElementById("poeTreeAscend") as HTMLSelectElement | null;
		if (classSel) {
			classSel.value = String(currentClassId);
			classSel.addEventListener("change", () => {
				applyClass(Number(classSel.value));
				if (ascSel) fillAscendOptions(ascSel);
			});
		}
		if (ascSel) {
			fillAscendOptions(ascSel);
			ascSel.addEventListener("change", () => {
				const before = snapshot();
				currentAscend = Number(ascSel.value);
				dropHiddenAllocations();
				commit(before);
				focusAscendancy(currentAscName());
				updatePoints();
				syncUrl();
				draw();
			});
		}
		const bloodSel = document.getElementById("poeTreeBloodline") as HTMLSelectElement | null;
		if (bloodSel) {
			fillBloodlineOptions(bloodSel);
			bloodSel.addEventListener("change", () => {
				const before = snapshot();
				currentBloodline = Number(bloodSel.value);
				dropHiddenAllocations();
				commit(before);
				focusAscendancy(currentBloodlineId());
				updatePoints();
				syncUrl();
				draw();
			});
		}
		// 초기화 — 할당을 루트만 남기고 클리어
		const resetBtn = document.getElementById("poeTreeReset");
		if (resetBtn) {
			resetBtn.addEventListener("click", () => {
				const before = snapshot();
				highlighted.clear();
				const root = rootNode();
				if (root !== undefined) highlighted.add(root);
				commit(before);
				hoverPath = [];
				removalSet.clear();
				updatePoints();
				syncUrl();
				draw();
			});
		}
		document.getElementById("poeTreeUndo")?.addEventListener("click", undo);
		document.getElementById("poeTreeRedo")?.addEventListener("click", redo);
		updateHistoryButtons();

		// ---- 공식 트리 링크 가져오기 / 현재 링크 복사 ----
		const importInput = document.getElementById("poeTreeImport") as HTMLInputElement | null;
		if (importInput) {
			const run = () => {
				const ok = importCode(importInput.value);
				importInput.classList.toggle("input-error", !ok && importInput.value.trim().length > 0);
				if (ok) importInput.value = "";
			};
			document.getElementById("poeTreeImportGo")?.addEventListener("click", run);
			importInput.addEventListener("keydown", (e) => {
				if (e.key === "Enter") {
					e.preventDefault();
					run();
				}
			});
		}
		const copyBtn = document.getElementById("poeTreeCopy") as HTMLButtonElement | null;
		if (copyBtn) {
			const label = copyBtn.textContent || "";
			copyBtn.addEventListener("click", async () => {
				try {
					await navigator.clipboard.writeText(globalThis.location.href);
					copyBtn.textContent = isKorean ? "복사됨" : "Copied";
				} catch {
					copyBtn.textContent = isKorean ? "복사 실패" : "Failed";
				}
				setTimeout(() => (copyBtn.textContent = label), 1500);
			});
		}
		// 공홈 링크 복사 — 우리 t= 코드는 GGG 인코딩 그대로라 공홈 플래너 URL 에 붙이면 그대로 열린다.
		// (아틀라스도 동일 인코딩 — class/asc 바이트가 0 일 뿐이다. 마스터리 픽·클러스터도 규격 그대로 실린다)
		const officialBtn = document.getElementById("poeTreeCopyOfficial") as HTMLButtonElement | null;
		if (officialBtn) {
			const officialLabel = officialBtn.textContent || "";
			officialBtn.addEventListener("click", async () => {
				const prefix = isAtlas
					? "https://www.pathofexile.com/fullscreen-atlas-skill-tree/"
					: "https://www.pathofexile.com/fullscreen-passive-skill-tree/";
				try {
					await navigator.clipboard.writeText(prefix + encodeTree());
					officialBtn.textContent = isKorean ? "복사됨" : "Copied";
				} catch {
					officialBtn.textContent = isKorean ? "복사 실패" : "Failed";
				}
				setTimeout(() => (officialBtn.textContent = officialLabel), 1500);
			});
		}
	}
	// 공홈 공유 링크(`pathofexile.com/passive-skill-tree/<버전>/<코드>`)나 코드만 붙여넣어도 받는다.
	// 우리 URL 의 ?t= 값과 같은 인코딩이라 디코더를 그대로 재사용한다.
	// 쿼리 값 해석 — `+` 는 공백, `%XX` 는 문자. decodeURIComponent 만 쓰면 base64 의 `%3D`(=)가 남고
	// 노터블 이름의 `+` 가 공백으로 안 바뀐다(둘 다 실제로 붙여넣기 실패를 만들었다).
	const decodeParam = (v: string) => decodeURIComponent(v.replace(/\+/g, "%20"));
	function importCode(raw: string): boolean {
		// 아틀라스는 GGG 직업 기반 인코딩(t=)을 쓰지 않는다 — 우리 링크의 nodes= 목록만 받는다.
		// (패시브 t= 코드를 아틀라스에 붙여넣으면 다른 트리의 id 라 엉뚱한 노드가 찍힌다 → 거부)
		let token = raw.trim();
		// 우리 링크를 붙여넣은 경우 주얼(j=)도 함께 살린다 — 공홈 링크엔 없는 파라미터라 있으면 우리 것.
		const jewelParam = /[?&]j=([^&#]+)/.exec(token);
		// t= 는 첫 파라미터가 아닐 수 있다(우리 링크는 ...&t=...) — indexOf("?t=") 로 찾으면
		// **우리가 복사한 링크를 그대로 붙여넣었을 때 실패**한다. 정규식으로 어디에 있든 잡는다.
		const tParam = /[?&]t=([^&#]+)/.exec(token);
		// 우리 레거시 형식(?class=&nodes=콤마id) — 시뮬 결과 링크가 이 모양이라 함께 받는다
		// 주소창에서 복사한 링크는 콤마가 %2C 로 인코딩돼 있다 — 값 전체를 잡아 디코드해야
		// **첫 번째 id 하나만 읽고 나머지를 통째로 버리는** 사고가 안 난다(아틀라스 링크에서 발각).
		const legacyNodes = /[?&]nodes=([^&#]+)/.exec(token);
		// 클러스터 구성(c=)도 우리 링크에만 있는 파라미터 — 이게 없으면 서브트리가 아예 안 생겨
		// t= 의 클러스터 노드 할당이 통째로 버려진다(붙여넣으면 클러스터가 사라지는 증상).
		const clusterParam = /[?&]c=([^&#]+)/.exec(token);
		let dec: {
			classId: number;
			ascend: number;
			bloodline: number;
			nodes: number[];
			masteries: { node: number; effect: number }[];
			clusters?: number[];
		} | null = null;
		// 아틀라스도 공홈 인코딩을 그대로 쓴다(클래스/전직 바이트만 0, id 전부 uint16 범위) —
		// pathofexile.com/fullscreen-atlas-skill-tree/<코드> 링크를 붙여넣으면 여기서 풀린다.
		if (legacyNodes && !tParam) {
			const classMatch = /[?&]class=(\d)/.exec(token);
			const ids = decodeParam(legacyNodes[1]).split(",").map(Number).filter((n) => Number.isFinite(n) && n > 0);
			if (ids.length) {
				dec = { classId: classMatch ? Number(classMatch[1]) : currentClassId, ascend: 0, bloodline: 0, nodes: ids, masteries: [] };
			}
		} else {
			// 공홈 링크(.../passive-skill-tree/3.28/<코드>)는 마지막 경로 조각이 코드다
			token = (tParam ? decodeParam(tParam[1]) : token.split(/[/?#&]/).filter(Boolean).pop() || "").trim();
			if (!token) return false;
			dec = decodeTree(token);
		}
		if (!dec || dec.nodes.length === 0 || dec.classId > 6) return false;
		if (isAtlas) {
			// 아틀라스엔 직업/전직/혈맹이 없다 — 코드의 그 바이트들은 0 취급하고, 존재하는 노드만 받는다
			// (패시브 트리 코드를 잘못 붙여넣으면 id 가 거의 안 맞아 여기서 자연히 걸러진다)
			const known = dec.nodes.filter((id) => nodeById.has(id));
			if (known.length < Math.max(1, Math.floor(dec.nodes.length * 0.6))) return false;
			dec = { ...dec, classId: 0, ascend: 0, bloodline: 0, nodes: known, masteries: [], clusters: [] };
		}
		const before = snapshot();
		// 주얼/클러스터까지 한 번의 스냅샷 복원으로 처리한다 — 복원 함수가 클러스터를 먼저 만들고
		// 그다음 할당을 붙이므로, 생성 노드(id ≥ 65536) 할당이 살아남는다.
		const jewelEntries: [number, string][] = [];
		if (jewelParam) {
			for (const pair of decodeParam(jewelParam[1]).split(",")) {
				const sep = pair.indexOf(":");
				const nodeId = sep > 0 ? Number(pair.slice(0, sep)) : NaN;
				if (Number.isFinite(nodeId) && pair.slice(sep + 1)) jewelEntries.push([nodeId, pair.slice(sep + 1)]);
			}
		}
		const clusterEntries: [number, { sizeName: string; nodeCount: number; skillKey: string; notables: string[]; socketCount: number }][] = [];
		if (clusterParam) {
			for (const entry of decodeParam(clusterParam[1]).split(",")) {
				const [socketId, sizeName, nodeCount, skillKey, notables, socketCount] = entry.split(":");
				if (!socketId || !sizeName || !nodeCount) continue;
				clusterEntries.push([
					Number(socketId),
					{
						sizeName,
						nodeCount: Number(nodeCount),
						skillKey: skillKey || "",
						notables: (notables || "").split("|").filter(Boolean),
						socketCount: Number(socketCount) || 0,
					},
				]);
			}
		}
		restoreSnapshot(
			JSON.stringify([
				dec.classId,
				dec.ascend,
				dec.bloodline,
				dec.nodes.concat(dec.clusters || []), // 클러스터 생성 노드 할당은 t= 의 별도 구간에 들어 있다
				dec.masteries.map((m) => [m.node, m.effect]),
				jewelEntries,
				clusterEntries,
			]),
		);
		// 가져온 트리는 시작 노드가 빠져 있을 수 있다 — 넣어줘야 고아 정리에 통째로 날아가지 않는다.
		const root = rootNode();
		if (root !== undefined) {
			highlighted.add(root);
			centerOnNode(root);
		}
		commit(before);
		updatePoints();
		syncUrl();
		draw();
		return true;
	}
	// 전직/혈맹을 고르면 시작 노드를 무료로 부여하고(게임과 동일) 그 서브트리로 화면을 옮긴다.
	// 전직 서브트리는 메인 트리보다 훨씬 촘촘해서 확대율을 따로 준다.
	function focusAscendancy(ascName: string | null) {
		if (!ascName) return;
		const start = nodes.find((n) => n.ascendancy === ascName && n.ascendancyStart);
		if (!start) return;
		highlighted.add(start.id);
		centerOnNode(start.id, 0.45);
	}

	// 노드의 궤도 각(월드). 위치식 x=group+r·sin(a), y=group-r·cos(a) → 캔버스각 θ=a-90°.
	// 궤도 각도 — GGG 공식 스펙상 16/40 노드 궤도는 균등 분할이 아니다(추출기 tree-common.mjs 와 동일 표).
	// 여기서 균등식을 쓰면 노드 좌표(추출기 계산)와 연결선 각도(렌더 계산)가 어긋나 호가 비뚤어진다.
	const ORBIT_ANGLES_16 = [0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 240, 270, 300, 315, 330];
	const ORBIT_ANGLES_40 = [
		0, 10, 20, 30, 40, 45, 50, 60, 70, 80, 90, 100, 110, 120, 130, 135, 140, 150, 160, 170, 180, 190, 200, 210,
		220, 225, 230, 240, 250, 260, 270, 280, 290, 300, 310, 315, 320, 330, 340, 350,
	];
	function orbitAngleAt(orbit: number, orbitIndex: number): number {
		const per = skillsPerOrbit[orbit] || 1;
		const table = per === 16 ? ORBIT_ANGLES_16 : per === 40 ? ORBIT_ANGLES_40 : null;
		if (table) return ((table[orbitIndex % table.length] || 0) * Math.PI) / 180;
		return (2 * Math.PI * orbitIndex) / per;
	}
	function orbitAngle(node: TreeNode): number {
		return orbitAngleAt(node.orbit, node.orbitIndex) - Math.PI / 2;
	}

	function draw() {
		const width = canvas.clientWidth;
		const height = canvas.clientHeight;
		const ratio = globalThis.devicePixelRatio || 1;
		if (canvas.width !== width * ratio || canvas.height !== height * ratio) {
			canvas.width = width * ratio;
			canvas.height = height * ratio;
		}
		context.setTransform(ratio, 0, 0, ratio, 0, 0);
		context.clearRect(0, 0, width, height);

		// 배경 — 어두운 청흑색 방사형
		const bg = context.createRadialGradient(width / 2, height / 2, 0, width / 2, height / 2, Math.max(width, height) * 0.8);
		bg.addColorStop(0, "#0f1520");
		bg.addColorStop(0.6, "#0a0e15");
		bg.addColorStop(1, "#05070b");
		context.fillStyle = bg;
		context.fillRect(0, 0, width, height);

		// 0-b) 공식 배경 텍스처 타일 — 시트에 있는데 안 쓰던 아트. 트리와 함께 팬/줌 되도록
		// 패턴 변환(offset·scale)을 걸어 한 번의 fillRect 로 깐다(타일마다 drawImage 하면 줌아웃에서 수천 번이 된다).
		const pattern = backgroundPattern();
		if (pattern && scale > 0.015) {
			// PoB 와 같은 배율(bg.width * scale * 1.33 * 2.5) — 원본 픽셀 그대로 깔면 타일이 너무 잘아
			// 줌아웃에서 그냥 어두운 판으로 뭉개진다.
			const tile = backgroundTilePx * scale * 3.325;
			const setTransform = (pattern as unknown as { setTransform?: (m: DOMMatrix) => void }).setTransform;
			if (typeof setTransform === "function" && typeof DOMMatrix === "function") {
				const k = tile / backgroundTilePx;
				setTransform.call(pattern, new DOMMatrix([k, 0, 0, k, offsetX % tile, offsetY % tile]));
				context.globalAlpha = 0.65; // 노드 가독성 우선 — 원본보다 약하게
				context.fillStyle = pattern;
				context.fillRect(0, 0, width, height);
				context.globalAlpha = 1;
			}
		}

		const pad = 200 * scale;
		const visible = (sx: number, sy: number, r: number) => sx > -r - pad && sy > -r - pad && sx < width + r + pad && sy < height + r + pad;

		// 0-c) 클래스 일러스트 레이어 — 좌표는 트리 데이터(extraImages)가 준다(PoB 는 이걸 하드코딩했다).
		// 고른 직업의 아트만 선명하게, 나머지는 흐리게 — 공식 뷰어처럼 "내 직업 영역"이 드러난다.
		if (scale > 0.02 && extraImages.length) {
			for (const layer of extraImages) {
				const img = getSheet("tree-layers/" + layer.image);
				if (!img) continue;
				const w = img.naturalWidth * scale;
				const h = img.naturalHeight * scale;
				const sx = layer.x * scale + offsetX;
				const sy = layer.y * scale + offsetY;
				if (!visible(sx + w / 2, sy + h / 2, Math.max(w, h) / 2)) continue;
				context.globalAlpha = LAYER_CLASS_ID[layer.image] === currentClassId ? 0.85 : 0.32;
				context.drawImage(img, sx, sy, w, h);
			}
			context.globalAlpha = 1;
		}

		// 1) 그룹 배경 (궤도 링 아트) — 충분히 확대됐을 때만
		if (scale > 0.02 && sprites.groupBackground) {
			for (const g of Object.values(groups)) {
				if (!g.background) continue;
				const sx = g.x * scale + offsetX;
				const sy = g.y * scale + offsetY;
				const sp = sprites.groupBackground;
				const c = sp.coords[g.background.image];
				if (!c || !visible(sx, sy, (c.w / sp.zoom) * scale)) continue;
				const img = getSheet(sp.file);
				if (!img) continue;
				const w = (c.w / sp.zoom) * scale;
				const h = (c.h / sp.zoom) * scale;
				context.globalAlpha = 0.55;
				if (g.background.isHalfImage) {
					// 상단 반쪽 이미지 → 아래로 상하 반전해 전체 링 구성
					context.drawImage(img, c.x, c.y, c.w, c.h, sx - w / 2, sy - h, w, h);
					context.save();
					context.translate(sx, sy);
					context.scale(1, -1);
					context.drawImage(img, c.x, c.y, c.w, c.h, -w / 2, -h, w, h);
					context.restore();
				} else {
					context.drawImage(img, c.x, c.y, c.w, c.h, sx - w / 2, sy - h / 2, w, h);
				}
			}
			context.globalAlpha = 1;
		}

		// 1-b) 전직 배경 아트 — 전직 시작 그룹에 Classes<전직명> 을 깐다(PoB renderGroup 과 동일).
		// 선택 안 된 전직은 25% 로 흐리게: 공식 뷰어처럼 "지금 고른 전직"이 한눈에 보인다.
		if (scale > 0.02 && sprites.ascendancy && !isAtlas) {
			const sp = sprites.ascendancy;
			const img = getSheet(sp.file);
			const currentAsc = currentAscName();
			const currentBlood = currentBloodlineId();
			if (img) {
				for (const g of Object.values(groups)) {
					const ascName = g.ascendancyStart;
					if (!ascName) continue;
					const c = sp.coords["Classes" + ascName];
					if (!c) continue;
					const sx = g.x * scale + offsetX;
					const sy = g.y * scale + offsetY;
					const w = (c.w / sp.zoom) * scale;
					const h = (c.h / sp.zoom) * scale;
					if (!visible(sx, sy, Math.max(w, h) / 2)) continue;
					context.globalAlpha = ascName === currentAsc || ascName === currentBlood ? 1 : 0.25;
					context.drawImage(img, c.x, c.y, c.w, c.h, sx - w / 2, sy - h / 2, w, h);
				}
				context.globalAlpha = 1;
			}
		}

		// 2) 연결선 — 같은 group·orbit 이면 궤도 따라 arc, 아니면 직선
		// mode: "all" 전체(어둡게) / "intermediate" 한쪽만 할당(= 지금 이을 수 있는 길) / "allocated" 양쪽 다 할당
		// 공식 뷰어는 이 3단계를 색으로 구분한다 — 중간 단계가 없으면 "어디로 뻗을 수 있는지"가 안 보인다.
		function tracePath(mode: "all" | "intermediate" | "allocated") {
			context.beginPath();
			for (const [fromId, toId] of edges) {
				const fromOn = highlighted.has(fromId);
				const toOn = highlighted.has(toId);
				if (mode === "allocated" && !(fromOn && toOn)) continue;
				if (mode === "intermediate" && fromOn === toOn) continue;
				const from = nodeById.get(fromId);
				const to = nodeById.get(toId);
				if (!from || !to) continue;
				// 전직 서브트리는 메인 트리에서 멀리 떨어진 좌표에 배치돼 있다. 데이터상 연결(전직시작↔클래스시작 등)을
				// 그대로 그리면 화면을 가로지르는 긴 선이 생겨서, 공식 뷰어처럼 전직↔메인/다른전직 엣지는 그리지 않는다.
				if ((from.ascendancy || null) !== (to.ascendancy || null)) continue;
				if (!nodeVisible(from) || !nodeVisible(to)) continue;
				const ax = from.x * scale + offsetX;
				const ay = from.y * scale + offsetY;
				if (from.group === to.group && from.orbit === to.orbit && from.orbit > 0) {
					const g = groups[from.group];
					if (g) {
						const cx = g.x * scale + offsetX;
						const cy = g.y * scale + offsetY;
						const r = orbitRadii[from.orbit] * scale;
						let a1 = orbitAngle(from);
						let a2 = orbitAngle(to);
						let d = a2 - a1;
						while (d > Math.PI) d -= 2 * Math.PI;
						while (d < -Math.PI) d += 2 * Math.PI;
						context.moveTo(ax, ay);
						context.arc(cx, cy, r, a1, a1 + d, d < 0);
						continue;
					}
				}
				context.moveTo(ax, ay);
				context.lineTo(to.x * scale + offsetX, to.y * scale + offsetY);
			}
		}
		context.lineCap = "round";
		context.strokeStyle = "rgba(130,140,155,0.30)";
		context.lineWidth = Math.max(0.4, 16 * scale);
		tracePath("all");
		context.stroke();
		if (hasHighlight()) {
			// 중간 단계 — 할당 노드에서 뻗어 나가는 길. 골드보다 어둡고 미할당보다 밝게.
			context.strokeStyle = "rgba(170,178,192,0.55)";
			context.lineWidth = Math.max(0.6, 18 * scale);
			tracePath("intermediate");
			context.stroke();
			context.save();
			context.shadowColor = "rgba(224,180,90,0.9)";
			context.shadowBlur = Math.max(4, 40 * scale);
			context.strokeStyle = "rgba(232,194,108,0.95)";
			context.lineWidth = Math.max(1, 22 * scale);
			tracePath("allocated");
			context.stroke();
			context.restore();
		}

		// 2b) 호버 경로 미리보기 — 반투명 골드 점선
		if (hoverPath.length > 1) {
			context.save();
			context.setLineDash([Math.max(2, 10 * scale), Math.max(2, 8 * scale)]);
			context.strokeStyle = "rgba(240,208,137,0.7)";
			context.lineWidth = Math.max(1, 14 * scale);
			context.beginPath();
			for (let i = 0; i < hoverPath.length; i++) {
				const n = nodeById.get(hoverPath[i]);
				if (!n) continue;
				const px = n.x * scale + offsetX;
				const py = n.y * scale + offsetY;
				if (i === 0) context.moveTo(px, py);
				else context.lineTo(px, py);
			}
			context.stroke();
			context.restore();
		}

		// 2b') 주얼 반경 링 — 호버 중인 슬롯 + 주얼을 실제로 꽂아둔 슬롯(꽂았으면 계속 보여야 반경을 보고 판단할 수 있다)
		const ringNodes: TreeNode[] = [];
		if (!isAtlas) {
			if (hovered?.type === "jewel" && !hovered.expansionJewel) ringNodes.push(hovered);
			for (const nodeId of jewelPicks.keys()) {
				if (nodeId === hovered?.id || !highlighted.has(nodeId)) continue;
				const socket = nodeById.get(nodeId);
				if (socket) ringNodes.push(socket);
			}
		}
		for (const ringNode of ringNodes) {
			const socketed = jewelPicks.has(ringNode.id) && highlighted.has(ringNode.id);
			const hx = ringNode.x * scale + offsetX;
			const hy = ringNode.y * scale + offsetY;
			// 꽂은 주얼이 반경 모드를 가졌으면 **그 주얼의 실제 반경**만 공식 링 아트로 그린다
			// (3단 점선은 빈 슬롯 계획용이다 — 꽂은 뒤에도 3개를 다 그리면 어느 게 진짜인지 알 수 없다)
			const spec = socketed ? jewelPicks.get(ringNode.id) : undefined;
			const band = spec ? jewelRadiusOf(spec) : null;
			if (band) {
				const ringKey = TIMELESS_RING[spec!.split(":")[0]] || "JewelCircle1";
				const drawn = blitRing(ringKey, ringNode.x, ringNode.y, band.r, ringNode.id === hovered?.id ? 0.9 : 0.5);
				if (!drawn) {
					context.save();
					context.strokeStyle = "rgba(110,231,183,0.5)";
					context.lineWidth = Math.max(1, 10 * scale);
					context.beginPath();
					context.arc(hx, hy, band.r * scale, 0, Math.PI * 2);
					context.stroke();
					context.restore();
				}
				continue;
			}
			context.save();
			context.setLineDash([Math.max(2, 12 * scale), Math.max(2, 10 * scale)]);
			context.lineWidth = Math.max(1, 8 * scale);
			context.font = `${Math.max(10, Math.round(90 * scale))}px sans-serif`;
			context.textAlign = "center";
			for (const band of JEWEL_RADII) {
				// 꽂아둔 슬롯은 흐리게 상시 표시(트리를 가리지 않게), 호버 중인 슬롯은 진하게
				context.strokeStyle = socketed && ringNode.id !== hovered?.id ? "rgba(110,231,183,0.28)" : "rgba(110,231,183,0.55)";
				context.beginPath();
				context.arc(hx, hy, band.r * scale, 0, Math.PI * 2);
				context.stroke();
				if (ringNode.id === hovered?.id) {
					context.fillStyle = "rgba(110,231,183,0.85)";
					context.fillText(isKorean ? band.ko : band.en, hx, hy - band.r * scale - 6);
				}
			}
			context.restore();
		}

		// 2c) 해제 미리보기 — 할당 노드에 마우스를 올리면 함께 사라질 경로를 붉은 점선으로
		if (removalSet.size > 1) {
			context.save();
			context.setLineDash([Math.max(2, 10 * scale), Math.max(2, 8 * scale)]);
			context.strokeStyle = "rgba(232,110,96,0.85)";
			context.lineWidth = Math.max(1, 16 * scale);
			context.beginPath();
			for (const [fromId, toId] of edges) {
				if (!removalSet.has(fromId) || !removalSet.has(toId)) continue;
				const from = nodeById.get(fromId);
				const to = nodeById.get(toId);
				if (!from || !to) continue;
				context.moveTo(from.x * scale + offsetX, from.y * scale + offsetY);
				context.lineTo(to.x * scale + offsetX, to.y * scale + offsetY);
			}
			context.stroke();
			context.restore();
		}

		// 2-b) 직업 시작 노드 아트 — 고른 직업만 그 직업 아트(centerwitch 등), 나머지는 비활성 배경.
		// 공식 뷰어의 중앙 원판이 이것이다(없으면 트리 한가운데가 휑하다).
		if (scale > 0.02 && sprites.startNode && !isAtlas) {
			const sp = sprites.startNode;
			const img = getSheet(sp.file);
			if (img) {
				for (const node of nodes) {
					if (node.type !== "class" || !node.startArt) continue;
					const isCurrent = classStartByClassId.get(currentClassId) === node.id;
					const c = sp.coords[isCurrent ? node.startArt : "PSStartNodeBackgroundInactive"];
					if (!c) continue;
					const sx = node.x * scale + offsetX;
					const sy = node.y * scale + offsetY;
					const w = (c.w / sp.zoom) * scale;
					const h = (c.h / sp.zoom) * scale;
					if (!visible(sx, sy, Math.max(w, h) / 2)) continue;
					context.globalAlpha = isCurrent ? 1 : 0.6;
					context.drawImage(img, c.x, c.y, c.w, c.h, sx - w / 2, sy - h / 2, w, h);
				}
				context.globalAlpha = 1;
			}
		}

		// 3) 노드 — 아이콘(원 클립) + 프레임 스프라이트. 저줌에선 점만.
		// 지금 찍을 수 있는 노드(할당집합에 인접) 를 한 번에 모아 프레임 상태에 쓴다.
		// 반경 주얼 소켓 위에 있을 때만 계산 — 반경 안 "문신 가능 패시브" 표시용
		const radiusHint = new Set<number>(
			!isAtlas && hovered?.type === "jewel" && !hovered.expansionJewel ? radiusTattooTargets(hovered.id) : [],
		);
		const canAllocSet = new Set<number>();
		if (interactive) {
			for (const id of highlighted) {
				for (const nb of pathNeighbors(id)) if (!highlighted.has(nb)) canAllocSet.add(nb);
			}
		}
		for (const node of nodes) {
			if (!nodeVisible(node)) continue; // 선택하지 않은 전직/혈맹 서브트리는 숨김
			const sx = node.x * scale + offsetX;
			const sy = node.y * scale + offsetY;
			const rWorld = nodeRadiusWorld[node.type] || 45;
			const rScreen = rWorld * scale;
			if (!visible(sx, sy, rScreen)) continue;
			const isAllocated = highlighted.has(node.id);
			context.globalAlpha = node.ascendancy ? 0.85 : 1;

			// 저줌: 스프라이트 대신 점(성능)
			if (rScreen < 5) {
				context.beginPath();
				context.arc(sx, sy, Math.max(1, rScreen), 0, Math.PI * 2);
				context.fillStyle = isAllocated ? "#f0d089" : node.type === "notable" ? "#c8a24e" : node.type === "keystone" || node.type === "wormhole" ? "#cf6642" : "#6b7583";
				context.fill();
				continue;
			}

			if (isAllocated) {
				context.save();
				context.shadowColor = "rgba(240,208,137,0.9)";
				context.shadowBlur = rScreen * 0.9;
			}
			if (node.type === "mastery" && isAtlas) {
				// 아틀라스 마스터리는 선택식 효과가 없는 **그룹 표지**다(공홈도 상시 또렷하게 그린다).
				// 스킬 트리 규칙(미선택=흐림)을 그대로 쓰면 120개가 전부 반투명이 돼 버린다.
				blit("masteryOverlay", node.icon || "", node.x, node.y);
				blit("mastery", node.icon || "", node.x, node.y);
			} else if (node.type === "mastery") {
				// 마스터리 3단계: 효과 선택 / 지금 고를 수 있음(인접) / 그 외.
				// ⚠ masteryInactive·masteryConnected 시트는 **키 이름 체계가 달라**(PassiveMastery…Inactive.png)
				//   노드 아이콘 키가 하나도 안 맞는다 → 그 시트로 blit 하면 313개가 통째로 안 그려졌다.
				//   커버리지 313/313 인 mastery 시트를 쓰고 밝기로 상태를 구분한다.
				const pickedMastery = masteryPicks.has(node.id);
				const connectedMastery = canAllocSet.has(node.id) || isAllocated;
				const prevAlpha = context.globalAlpha;
				// 룬 접합(마스터리 문신)이 새겨졌으면 부족 배경을 깔아 교체됐음을 보여준다
				// (룬 접합 자체 아이콘 32종은 노드 시트에 없어 마스터리 아이콘을 그 위에 유지)
				const inkedMastery = tattooArt(node.id);
				if (inkedMastery) blit("tattooActiveEffect", inkedMastery.activeEffectImage, node.x, node.y);
				context.globalAlpha = prevAlpha * (pickedMastery || inkedMastery ? 1 : connectedMastery ? 0.75 : 0.4);
				blit("mastery", node.icon || "", node.x, node.y);
				context.globalAlpha = prevAlpha;
			} else if (node.icon) {
				const sheet = iconSheetFor(node.type, isAllocated);
				// 문신을 새긴 패시브는 게임에서도 **그림이 통째로 바뀐다** — 부족 배경 + 문신 아이콘.
				// (문신 아이콘 137/169 는 노드 시트에 있고, 나머지는 원래 아이콘으로 떨어진다)
				const ink = tattooArt(node.id);
				if (ink) blit("tattooActiveEffect", ink.activeEffectImage, node.x, node.y);
				const iconKey = ink && sheet && hasCoord(sheet, ink.icon) ? ink.icon : node.icon;
				if (sheet) blit(sheet, iconKey, node.x, node.y, true);
			}
			const fc = frameCoord(node, isAllocated, canAllocSet.has(node.id));
			if (fc) blit("frame", fc, node.x, node.y);
			if (isAllocated) context.restore();

			// 반경 주얼 소켓에 올리면 "반경 안에서 문신을 새길 수 있는 패시브"를 청록 테두리로 — 어디를 갈아끼울지 바로 보인다
			if (radiusHint.has(node.id)) {
				context.globalAlpha = 1;
				context.save();
				context.beginPath();
				context.arc(sx, sy, rScreen + Math.max(2, 4 * scale), 0, Math.PI * 2);
				context.strokeStyle = "rgba(110,231,183,0.9)";
				context.lineWidth = Math.max(1.5, 4 * scale);
				context.stroke();
				context.restore();
			}

			// 문신 부족 배경이 아직 안 떴을 때만 자주색 테두리로 표시(배경이 뜨면 그림만으로 구분된다)
			if (tattooPicks.has(node.id) && !tattooArt(node.id)) {
				context.globalAlpha = 1;
				context.save();
				context.shadowColor = "rgba(196,132,252,0.95)";
				context.shadowBlur = rScreen * 0.8;
				context.beginPath();
				context.arc(sx, sy, rScreen + Math.max(2, 5 * scale), 0, Math.PI * 2);
				context.strokeStyle = "#c084fc";
				context.lineWidth = Math.max(2, 5 * scale);
				context.stroke();
				context.restore();
			}

			// 해제 대상 — 붉은 테두리
			if (removalSet.has(node.id)) {
				context.globalAlpha = 1;
				context.save();
				context.shadowColor = "rgba(232,110,96,0.95)";
				context.shadowBlur = rScreen * 0.8;
				context.beginPath();
				context.arc(sx, sy, rScreen + Math.max(2, 7 * scale), 0, Math.PI * 2);
				context.strokeStyle = "#e86e60";
				context.lineWidth = Math.max(2, 6 * scale);
				context.stroke();
				context.restore();
			}

			// 검색 매칭 — 청록 글로우 테두리
			if (searchHits.has(node.id)) {
				context.globalAlpha = 1;
				context.save();
				context.shadowColor = "rgba(78,201,212,0.95)";
				context.shadowBlur = rScreen * 0.8;
				context.beginPath();
				context.arc(sx, sy, rScreen + Math.max(2, 7 * scale), 0, Math.PI * 2);
				context.strokeStyle = "#4ec9d4";
				context.lineWidth = Math.max(2, 6 * scale);
				context.stroke();
				context.restore();
			}

			if (node === hovered) {
				context.globalAlpha = 1;
				context.beginPath();
				context.arc(sx, sy, rScreen + Math.max(2, 6 * scale), 0, Math.PI * 2);
				context.strokeStyle = "#ffffff";
				context.lineWidth = 2;
				context.stroke();
			}
		}
		context.globalAlpha = 1;
	}

	function findNodeAt(screenX: number, screenY: number): TreeNode | null {
		let best: TreeNode | null = null;
		let bestDistance = Infinity;
		for (const node of nodes) {
			if (!nodeVisible(node)) continue;
			const radius = Math.max((nodeRadiusWorld[node.type] || 45) * scale, 6);
			const dx = node.x * scale + offsetX - screenX;
			const dy = node.y * scale + offsetY - screenY;
			const distance = Math.hypot(dx, dy);
			if (distance <= radius + 2 && distance < bestDistance) {
				best = node;
				bestDistance = distance;
			}
		}
		return best;
	}

	// 배경 타일 패턴 — 시트에서 타일 한 장을 오프스크린으로 떠서 repeat 패턴으로 만든다(1회 캐시).
	let bgPattern: CanvasPattern | null = null;
	let backgroundTileWorld = 0;
	let backgroundTilePx = 0;
	function backgroundPattern(): CanvasPattern | null {
		if (bgPattern) return bgPattern;
		const sp = sprites.background;
		if (!sp) return null;
		const coord = sp.coords.Background2 || Object.values(sp.coords)[0];
		const img = coord ? getSheet(sp.file) : null;
		if (!coord || !img) return null;
		const off = document.createElement("canvas");
		off.width = coord.w;
		off.height = coord.h;
		const octx = off.getContext("2d");
		if (!octx) return null;
		octx.drawImage(img, coord.x, coord.y, coord.w, coord.h, 0, 0, coord.w, coord.h);
		backgroundTilePx = coord.w;
		backgroundTileWorld = coord.w / sp.zoom;
		bgPattern = context.createPattern(off, "repeat");
		return bgPattern;
	}

	// ---- 평가 결과 증감 표시 ----
	// 트리를 고치고 다시 계산했을 때 "그래서 얼마나 좋아졌나"가 핵심인데, 절대값만 보면 알 수가 없다.
	// 서버 프래그먼트를 파싱해 직전 계산과의 차이를 각 스탯 옆에 붙인다(직전값은 브라우저에만 보관).
	let lastEvalStats: Map<string, number> | null = null;
	// 평가 결과는 "그 순간의 트리" 값이다. 이후 트리를 고치면 화면 숫자와 실제 트리가 어긋나므로
	// 계산 시점의 상태를 기억해 두고, 달라지면 **결과가 낡았다고 표시**한다(조용한 오해가 가장 나쁘다).
	let lastEvalSignature: string | null = null;
	function markEvalStale(stale: boolean) {
		const panel = document.getElementById("poeTreeEvalPanel");
		const body = document.getElementById("poeTreeEvalBody");
		if (!panel || !body) return;
		const existing = document.getElementById("poeTreeEvalStale");
		if (!stale || panel.classList.contains("hidden")) {
			existing?.remove();
			body.classList.remove("opacity-50");
			return;
		}
		body.classList.add("opacity-50");
		if (existing) return;
		const badge = document.createElement("div");
		badge.id = "poeTreeEvalStale";
		badge.className = "px-2 py-1 text-[11px] font-mono text-warning border-b border-warning/40 bg-warning/10";
		badge.textContent = isKorean ? "트리가 바뀌었습니다 — 다시 계산하세요" : "Tree changed — recalculate";
		body.parentElement?.insertBefore(badge, body);
	}
	function annotateEvalDelta(root: HTMLElement) {
		const current = new Map<string, number>();
		for (const box of Array.from(root.querySelectorAll<HTMLElement>(".grid > div"))) {
			const label = (box.children[0]?.textContent || "").trim();
			const valueEl = box.children[1] as HTMLElement | undefined;
			const text = (valueEl?.textContent || "").trim();
			// "3,990,432" / "78" / "1.76" 만 대상 — 단위·기호가 섞인 값은 건너뛴다
			if (!label || !valueEl || !/^-?[\d,]+(\.\d+)?$/.test(text)) continue;
			const value = Number(text.replace(/,/g, ""));
			if (!Number.isFinite(value)) continue;
			current.set(label, value);
			const prev = lastEvalStats?.get(label);
			if (prev === undefined || prev === value) continue;
			const diff = value - prev;
			const delta = document.createElement("span");
			delta.className = "ml-1 text-[10px] font-mono " + (diff > 0 ? "text-success" : "text-error");
			delta.textContent = (diff > 0 ? "▲+" : "▼") + Math.abs(diff).toLocaleString();
			valueEl.appendChild(delta);
		}
		if (current.size) lastEvalStats = current;
		// 이 결과가 어느 트리의 것인지 기록 — 이후 편집이 생기면 낡음 표시가 붙는다
		lastEvalSignature = snapshot();
		markEvalStale(false);
	}

	// 소켓에 꽂은 클러스터 주얼(소켓 id → 구성) 과 그로부터 생성된 노드들.
	// 생성 노드는 트리 데이터에 없는 합성 노드라 nodes 배열에 합쳐 렌더/히트테스트에 태운다.
	const clusterPicks = new Map<number, { sizeName: string; nodeCount: number; skillKey: string; notables: string[]; socketCount: number }>();
	// 이름 → 노터블 정의(스탯/아이콘/한글). 트리 데이터에 좌표 없이 실려 온다.
	type ClusterNotable = { name: string; nameKo: string | null; stats: string[]; statsKo: string[] | null; icon: string | null; keystone?: boolean };
	const clusterNotables = new Map<string, ClusterNotable>();
	// URL(c=)과 평가 요청(clusters=)이 쓰는 공통 직렬화 — 두 곳이 갈라지면 "링크로 열면 다른 수치"가 된다.
	// 형식: 소켓:크기:노드수:스킬키:노터블|노터블 (노터블 이름은 PoB 가 그대로 파싱하는 영문 원문)
	function clusterConfEntries(): string[] {
		return Array.from(clusterPicks)
			.filter(([socketId]) => highlighted.has(socketId))
			.map(
				([socketId, plan]) =>
					`${socketId}:${plan.sizeName}:${plan.nodeCount}:${plan.skillKey}` +
					((plan.notables || []).length || plan.socketCount ? ":" + (plan.notables || []).join("|") : "") +
					(plan.socketCount ? ":" + plan.socketCount : ""),
			);
	}
	let clusterNodes: TreeNode[] = [];
	let clusterEdges: Array<[number, number]> = [];
	// 생성 노드 id → 그 노드를 만든 소켓 id. 생성 노드에서 우클릭했을 때 "어느 주얼의 것인지" 알아야
	// 구성 변경/제거를 그 자리에서 제공할 수 있다(소켓까지 찾아가지 않아도 되게).
	const clusterOwner = new Map<number, number>();
	// 클러스터가 드러낸 주얼 소켓(원본 노드) — 구성이 바뀌면 원래 숨은 상태로 되돌린다
	const clusterSocketBackup = new Map<number, TreeNode>();
	const clusterSocketShown = new Set<number>();
	function rebuildClusterNodes() {
		// 지난번에 드러낸 소켓을 먼저 원복 — 안 하면 주얼을 빼도 소켓이 남아 떠 있다
		const wasShown = new Set(clusterSocketShown);
		for (const id of clusterSocketShown) {
			const original = clusterSocketBackup.get(id);
			if (!original) continue;
			const cur = nodeById.get(id);
			const at = cur ? nodes.indexOf(cur) : -1;
			if (at >= 0) nodes[at] = original;
			nodeById.set(id, original);
		}
		clusterSocketShown.clear();
		clusterOwner.clear();
		// 이전 생성 노드 제거 후 재생성 — 구성이 바뀌면 통째로 다시 만든다(PoB 도 서브그래프를 지우고 재생성한다)
		if (clusterNodes.length) {
			const generated = new Set(clusterNodes.map((n) => n.id));
			nodes = nodes.filter((n) => !generated.has(n.id) || nodeById.get(n.id)?.expansionJewel);
			for (const n of clusterNodes) if (!n.expansionJewel) nodeById.delete(n.id);
		}
		// 이전 간선도 제거 — 안 하면 주얼을 바꿀 때마다 유령 연결이 쌓인다
		if (clusterEdges.length) {
			const dead = new Set(clusterEdges.map(([a, b]) => `${a}-${b}`));
			edges = edges.filter(([a, b]) => !dead.has(`${a}-${b}`));
			for (const [a, b] of clusterEdges) {
				adjacency.set(a, (adjacency.get(a) || []).filter((x) => x !== b));
				adjacency.set(b, (adjacency.get(b) || []).filter((x) => x !== a));
			}
			clusterEdges = [];
		}
		clusterNodes = [];
		// 중첩(대형 안 소켓에 중형) 때문에 **부모 먼저** 만들어야 한다 — 자식 서브그래프 id 는 부모의 baseId 를 물려받는다.
		// 순서를 어기면 id 가 통째로 어긋나(실측 66064 vs 66256) 엔진이 다른 노드를 본다.
		const childBase = new Map<number, number>();
		const ordered: number[] = [];
		const pending = new Set(clusterPicks.keys());
		while (pending.size) {
			let progressed = false;
			for (const id of Array.from(pending)) {
				const parent = nodeById.get(id)?.expansionJewel?.parent ?? null;
				if (parent === null || !clusterPicks.has(parent)) {
					ordered.push(id);
					pending.delete(id);
					progressed = true;
				} else if (ordered.indexOf(parent) >= 0) {
					ordered.push(id);
					pending.delete(id);
					progressed = true;
				}
			}
			if (!progressed) break; // 순환은 있을 수 없지만 무한루프 방지
		}
		for (const socketId of ordered) {
			const plan = clusterPicks.get(socketId)!;
			const socket = nodeById.get(socketId);
			if (!socket?.expansionJewel) continue;
			const parentId = socket.expansionJewel.parent ?? null;
			const built = buildClusterSubgraph(
				socket,
				{
					sizeName: plan.sizeName,
					nodeCount: plan.nodeCount,
					socketCount: plan.socketCount || 0,
					notableCount: (plan.notables || []).length,
					notables: plan.notables || [],
					skillKey: plan.skillKey,
				},
				parentId !== null && childBase.has(parentId) ? childBase.get(parentId) : undefined,
			);
			if (!built) continue;
			childBase.set(socketId, built.baseIdForChildren);
			for (const [a, b] of built.links || []) {
				// 렌더용 edges + 할당 판정용 adjacency 양쪽에 넣는다(둘 중 하나만 넣으면 선만 보이거나 못 찍는다)
				edges.push([a, b]);
				clusterEdges.push([a, b]);
				if (!adjacency.has(a)) adjacency.set(a, []);
				if (!adjacency.has(b)) adjacency.set(b, []);
				adjacency.get(a)!.push(b);
				adjacency.get(b)!.push(a);
			}
			for (const n of built.nodes) {
				if (nodeById.has(n.id)) {
					// 클러스터가 만든 주얼 소켓은 **트리에 이미 있는(프록시 그룹의 숨은) 노드를 재사용**한다.
					// 원본은 isProxy 로 감춰져 있어 그대로 두면 소켓이 안 보이고 클릭도 안 된다 →
					// 좌표만 서브그래프 자리로 옮기고 숨김을 푼다. 원본은 되돌릴 수 있게 보관.
					if (n.type === "jewel") {
						const original = nodeById.get(n.id)!;
						if (!clusterSocketBackup.has(n.id)) clusterSocketBackup.set(n.id, original);
						const shown = { ...original, x: n.x, y: n.y, orbit: n.orbit, orbitIndex: n.orbitIndex, group: n.group, isProxy: undefined } as TreeNode;
						nodeById.set(n.id, shown);
						const at = nodes.indexOf(original);
						if (at >= 0) nodes[at] = shown;
						clusterSocketShown.add(n.id);
					}
					continue;
				}
				const skill = clusterDefs?.[`${plan.sizeName} Cluster Jewel`]?.skills?.[plan.skillKey];
				// 스킬 이름/스탯은 **작은 패시브에만** 붙는다 — 마스터리까지 덮어쓰면 노드 수가 부풀려 보인다
				// (검색 개수로 발각: 12노드 주얼인데 13개가 잡혔다)
				const named =
					skill && n.type === "normal"
						? {
								...n,
								name: skill.name,
								nameKo: skill.name,
								stats: (skill as { stats?: string[] }).stats || [],
								// 클러스터 정의는 영문 문장만 준다 — 추출 단계에서 만든 statsKo 가 있으면 툴팁이 한글로 뜬다
								statsKo: (skill as { statsKo?: string[] }).statsKo || null,
							}
						: n;
				clusterNodes.push(named as TreeNode);
				nodes.push(named as TreeNode);
				nodeById.set(named.id, named as TreeNode);
				clusterOwner.set(named.id, socketId);
			}
		}
		// 구성이 바뀌면 사라진 생성 노드의 할당도 같이 지운다 — 안 그러면 없는 노드에 포인트가 묶인 채
		// 남아 포인트 수와 URL 이 실제 트리와 어긋난다(주얼을 바꿔 끼울 때 발생).
		for (const id of Array.from(highlighted)) {
			if (id >= 0x10000 && !nodeById.has(id)) highlighted.delete(id);
		}
		// 이번 구성에서 사라진 주얼 소켓의 할당도 정리(소켓은 실제 트리 id 라 위 조건에 안 걸린다)
		for (const id of wasShown) if (!clusterSocketShown.has(id)) highlighted.delete(id);
	}

	// ---- 클러스터 서브트리 생성 (PoB BuildSubgraph 이식) ----
	// 규칙은 ~/.bluesky-qa/cluster-port-check.js 로 PoB 실제 생성 결과와 노드 단위 100% 대조해 확정했다.
	//  · 궤도 인덱스는 템플릿 공간(6/12칸) → 트리 궤도(16칸) 변환이 필요하다: (idx + 프록시 offset) % total → 변환표
	//  · 소켓은 새로 만들지 않고 프록시 그룹의 실제 소켓 노드를 재사용한다(= parent/index 로 찾을 수 있다)
	//  · 중첩은 부모 id(크기 비트 전)를 물려받는다
	const CLUSTER_TRANSLATE: Record<string, number[]> = {
		"12>16": [0, 1, 3, 4, 5, 7, 8, 9, 11, 12, 13, 15],
		"6>16": [0, 3, 5, 8, 11, 13],
	};
	function translateClusterOrbitIndex(src: number, srcPer: number, destPer: number): number {
		if (srcPer === destPer) return src;
		const table = CLUSTER_TRANSLATE[`${srcPer}>${destPer}`];
		return table ? (table[src] ?? src) : src;
	}
	type ClusterPlan = { sizeName: string; nodeCount: number; socketCount: number; notableCount: number; notables?: string[]; skillKey?: string };
	function buildClusterSubgraph(socket: TreeNode, plan: ClusterPlan, baseId = 0x10000) {
		const exp = socket.expansionJewel;
		const def = clusterDefs?.[`${plan.sizeName} Cluster Jewel`] as
			| (ClusterJewelDef & { smallIndicies: number[]; notableIndicies: number[]; socketIndicies: number[] })
			| undefined;
		if (!exp || !def) return null;
		const sizeIndex = CLUSTER_DEF_KEY.indexOf(`${plan.sizeName} Cluster Jewel`);
		let id = baseId;
		if (exp.size === 2) id += exp.index << 6;
		else if (exp.size === 1) id += exp.index << 9;
		const nodeId = id + (sizeIndex << 4);
		const proxy = nodeById.get(exp.proxy ?? -1);
		const group = proxy ? groups[String(proxy.group)] : null;
		if (!group) return null;
		const nodeOrbit = sizeIndex + 1;
		const startOidx = clusterOffsets?.[String(exp.proxy)]?.[String(sizeIndex)] ?? 0;
		const orbitSlots = skillsPerOrbit[nodeOrbit];
		const toOrbitIndex = (templateIndex: number) =>
			translateClusterOrbitIndex((templateIndex + startOidx) % def.totalIndicies, def.totalIndicies, orbitSlots);
		const used = new Set<number>();
		const out: TreeNode[] = [];
		const placedByTemplate = new Map<number, TreeNode>();
		const place = (type: string, templateIndex: number, name: string, id2?: number, detail?: ClusterNotable) => {
			const oidx = toOrbitIndex(templateIndex);
			const angle = orbitAngleAt(nodeOrbit, oidx);
			out.push({
				id: id2 ?? nodeId + templateIndex,
				name,
				nameKo: detail?.nameKo || name,
				type,
				group: proxy!.group,
				orbit: nodeOrbit,
				orbitIndex: oidx,
				x: Math.round(group.x + orbitRadii[nodeOrbit] * Math.sin(angle)),
				y: Math.round(group.y - orbitRadii[nodeOrbit] * Math.cos(angle)),
				stats: detail?.stats || [],
				statsKo: detail?.statsKo || null,
				ascendancy: null,
				icon: detail?.icon || null,
			} as TreeNode);
			placedByTemplate.set(templateIndex, out[out.length - 1]);
			used.add(templateIndex);
		};
		// 마스터리(궤도 0 중앙) — **스킬에 masteryIcon 이 있을 때만** 존재한다(PoB BuildSubgraph 와 동일).
		// 소형 클러스터 스킬 17종은 전부 마스터리가 없다 — 무조건 만들면 게임/엔진엔 없는 노드를 찍게 된다.
		const planSkill = plan.skillKey ? (def.skills?.[plan.skillKey] as { masteryIcon?: string } | undefined) : undefined;
		if (planSkill?.masteryIcon) out.push({
			id: nodeId + 12,
			name: "Nothingness",
			nameKo: "무",
			type: "mastery",
			group: proxy!.group,
			orbit: 0,
			orbitIndex: 0,
			x: Math.round(group.x),
			y: Math.round(group.y),
			stats: [],
			statsKo: null,
			ascendancy: null,
			icon: null,
		} as TreeNode);
		const smallCount = plan.nodeCount - plan.socketCount - plan.notableCount;
		const findSocket = (jewelIndex: number) =>
			nodes.find((n) => n.expansionJewel?.parent === socket.id && n.expansionJewel?.index === jewelIndex);
		const getJewels = [0, 2, 1];
		if (plan.sizeName === "Large" && plan.socketCount === 1) {
			const s = findSocket(1);
			if (s) place("jewel", 6, s.nameKo || s.name, s.id);
		} else {
			for (let i = 0; i < plan.socketCount; i++) {
				const s = findSocket(getJewels[i]);
				if (s) place("jewel", def.socketIndicies[i], s.nameKo || s.name, s.id);
			}
		}
		const notableIdx: number[] = [];
		for (let idx of def.notableIndicies) {
			if (notableIdx.length === plan.notableCount) break;
			if (plan.sizeName === "Medium") {
				if (plan.socketCount === 0 && plan.notableCount === 2) idx = idx === 6 ? 4 : idx === 10 ? 8 : idx;
				else if (plan.nodeCount === 4) idx = idx === 10 ? 9 : idx === 2 ? 3 : idx;
			}
			if (!used.has(idx)) notableIdx.push(idx);
		}
		notableIdx.sort((a, b) => a - b);
		// 어느 노터블이 어느 자리에 가는지 = PoB 와 동일하게 notableSortOrder 오름차순(PassiveSpec:BuildSubgraph).
		// 이름 순/고른 순으로 넣으면 같은 조합이라도 노드 id 가 뒤바뀌어 엔진과 트리가 다른 걸 찍는다.
		const chosen = (plan.notables || [])
			.map((name) => clusterNotables.get(name))
			.filter((n): n is ClusterNotable => !!n)
			.sort((a, b) => (clusterNotableOrder?.[a.name] ?? 0) - (clusterNotableOrder?.[b.name] ?? 0));
		for (let i = 0; i < plan.notableCount && i < notableIdx.length; i++) {
			const detail = chosen[i];
			place(detail?.keystone ? "keystone" : "notable", notableIdx[i], detail?.name || "Notable", undefined, detail);
		}
		const smallIdx: number[] = [];
		for (let idx of def.smallIndicies) {
			if (smallIdx.length === smallCount) break;
			if (plan.sizeName === "Medium") {
				if (plan.nodeCount === 5 && idx === 4) idx = 3;
				else if (plan.nodeCount === 4) idx = idx === 8 ? 9 : idx === 4 ? 3 : idx;
			}
			if (!used.has(idx)) smallIdx.push(idx);
		}
		for (let i = 0; i < smallCount && i < smallIdx.length; i++) place("normal", smallIdx[i], "Small Passive");
		// 연결: 템플릿 인덱스 오름차순으로 이웃끼리 잇고, 소형이 아니면 고리를 닫는다.
		// 입구(entrance)는 템플릿 인덱스 0 노드이며 부모 소켓과 연결된다(PoB: subGraph.entranceNode = indicies[0]).
		const byTemplate = new Map<number, TreeNode>();
		for (const [templateIndex, node] of placedByTemplate) byTemplate.set(templateIndex, node);
		const links: Array<[number, number]> = [];
		let firstId: number | null = null;
		let lastId: number | null = null;
		for (let i = 0; i < def.totalIndicies; i++) {
			const node = byTemplate.get(i);
			if (!node) continue;
			if (firstId === null) firstId = node.id;
			if (lastId !== null) links.push([lastId, node.id]);
			lastId = node.id;
		}
		if (firstId !== null && lastId !== null && firstId !== lastId && plan.sizeName !== "Small") links.push([firstId, lastId]);
		const entrance = byTemplate.get(0);
		if (entrance) links.push([socket.id, entrance.id]);
		return { id: nodeId, baseIdForChildren: id, nodes: out, links };
	}

	// ---- 주얼 장착 팝업 (할당한 주얼 슬롯에 유니크 주얼을 꽂는다 → 평가에 반영) ----
	// 목록은 JTE 가 넘긴 datalist(#poeTreeJewelList)에서 읽는다 — 별도 API 왕복 없음.
	let jewelPicker: HTMLElement | null = null;
	function closeJewelPicker() {
		jewelPicker?.remove();
		jewelPicker = null;
	}
	// ---- 문신 ----
	// 문신은 패시브를 **교체**한다. 게임 규칙상 소형은 속성 종류까지 맞아야 한다(힘 소형엔 힘 문신 + 속성 공용).
	const ATTR_PATTERNS: Array<[string, RegExp]> = [
		["Strength", /to Strength$/],
		["Dexterity", /to Dexterity$/],
		["Intelligence", /to Intelligence$/],
	];
	const nodeAttribute = (node: TreeNode) =>
		ATTR_PATTERNS.find(([, re]) => (node.stats || []).some((line) => re.test(line.trim())))?.[0] || null;
	function tattooTarget(node: TreeNode): string | null {
		if (node.type === "notable") return "Notable";
		if (node.type === "keystone") return "Keystone";
		if (node.type === "mastery") return "Mastery";
		if (node.type !== "normal") return null;
		const attr = nodeAttribute(node);
		return attr ? "Small " + attr : null; // 속성이 아닌 소형 패시브엔 새길 문신이 없다
	}
	const tattooCandidates = (node: TreeNode) => {
		const target = tattooTarget(node);
		if (!target || !tattooDefs) return [];
		const smallAttr = target.startsWith("Small ");
		return tattooDefs.filter((t) => t.targetType === target || (smallAttr && t.targetType === "Small Attribute"));
	};
	const tattooByDn = (dn: string) => tattooDefs?.find((t) => t.dn === dn) || null;
	/**
	 * 이 소켓에 꽂은 반경 주얼의 반경 안에서 **문신을 새길 수 있는 할당 패시브** id 들.
	 * 반경 주얼(붉은 악몽 등)은 반경 안 패시브의 스탯을 변환하므로, 그 자리를 문신으로 갈아끼우는 게 실전 용법이다.
	 */
	function radiusTattooTargets(socketId: number): number[] {
		const spec = jewelPicks.get(socketId);
		const socket = nodeById.get(socketId);
		if (!spec || !socket || !highlighted.has(socketId)) return [];
		const band = jewelRadiusOf(spec);
		if (!band) return [];
		const targets: number[] = [];
		for (const id of highlighted) {
			const node = nodeById.get(id);
			if (!node || !tattooTarget(node)) continue;
			const dx = node.x - socket.x;
			const dy = node.y - socket.y;
			if (dx * dx + dy * dy <= band.r * band.r) targets.push(id);
		}
		return targets;
	}
	const tattooLabel = (dn: string) => {
		const def = tattooByDn(dn);
		return (isKorean && def?.nameKo) || def?.dn || dn;
	};
	const tattooLines = (dn: string) => {
		const def = tattooByDn(dn);
		if (!def) return [];
		return isKorean && def.statsKo?.length ? def.statsKo : def.stats;
	};
	function loadTattoos(onReady: () => void) {
		if (tattooDefs !== null) {
			onReady();
			return;
		}
		tattooDefs = [];
		fetch("/poe-data/tattoos.json", { cache: "no-cache" })
			.then((r) => (r.ok ? r.json() : null))
			.then((data) => {
				if (Array.isArray(data?.tattoos)) tattooDefs = data.tattoos;
			})
			.then(onReady, onReady);
	}
	let tattooPicker: HTMLElement | null = null;
	function closeTattooPicker() {
		tattooPicker?.remove();
		tattooPicker = null;
	}
	function openTattooPicker(node: TreeNode) {
		loadTattoos(() => renderTattooPicker(node));
	}
	/**
	 * @param targets 지정하면 그 노드들에 **일괄** 적용(반경 주얼 안 소형 패시브를 한 번에 저항 문신으로 바꾸는 실전 용법).
	 *   생략하면 anchor 노드 하나에만 새긴다.
	 */
	function renderTattooPicker(node: TreeNode, targets?: number[]) {
		closeTattooPicker();
		closeJewelPicker();
		const bulk = targets && targets.length > 0;
		// 일괄 모드의 후보는 대상 노드들이 받을 수 있는 문신의 합집합(대상마다 속성이 달라도 한 번에 보여준다)
		let list: TattooDef[] = tattooCandidates(node);
		if (bulk) {
			const byDn = new Map<string, TattooDef>();
			for (const id of targets!) {
				const target = nodeById.get(id);
				if (target) for (const def of tattooCandidates(target)) byDn.set(def.dn, def);
			}
			list = Array.from(byDn.values());
		}
		const host = canvas.parentElement as HTMLElement;
		const panel = document.createElement("div");
		panel.className =
			"absolute z-20 max-h-[60%] w-96 overflow-y-auto rounded shadow-2xl border border-amber-700/60 bg-stone-900/97";
		const head = document.createElement("div");
		head.className =
			"sticky top-0 border-y-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-sm px-6 py-1.5";
		head.textContent = bulk
			? (isKorean ? `반경 내 ${targets!.length}개 패시브 — 문신 일괄` : `${targets!.length} passives in radius — bulk tattoo`)
			: (isKorean && node.nameKo ? node.nameKo : node.name) + (isKorean ? " — 문신 새기기" : " — apply tattoo");
		panel.appendChild(head);
		const filter = document.createElement("input");
		filter.type = "text";
		filter.className = "w-full px-3 py-1.5 text-xs bg-stone-800 text-amber-100 border-b border-stone-700 outline-none";
		filter.placeholder = isKorean ? "문신 이름/효과 검색" : "Filter by name or mod";
		panel.appendChild(filter);
		const rows = document.createElement("div");
		panel.appendChild(rows);
		const apply = (dn: string | null) => {
			const before = snapshot();
			// 일괄이면 그 문신을 받을 수 있는 대상에만 새긴다(힘 문신은 힘 소형에만)
			const ids = !bulk
				? [node.id]
				: dn === null
					? targets! // 지우기는 대상 전부에서
					: targets!.filter((id) => {
							const target = nodeById.get(id);
							return !!target && tattooCandidates(target).some((t) => t.dn === dn);
						});
			for (const id of ids) {
				if (dn) tattooPicks.set(id, dn);
				else tattooPicks.delete(id);
			}
			commit(before);
			closeTattooPicker();
			markEvalStale(true);
			updatePoints();
			syncUrl();
			draw();
		};
		const render = () => {
			rows.replaceChildren();
			const inkedAny = bulk ? targets!.some((id) => tattooPicks.has(id)) : tattooPicks.has(node.id);
			if (inkedAny) {
				const clear = document.createElement("button");
				clear.type = "button";
				clear.className = "block w-full text-left px-4 py-2 text-xs text-rose-300 hover:bg-rose-900/30 border-b border-stone-800";
				clear.textContent = isKorean ? (bulk ? "반경 내 문신 모두 지우기" : "문신 지우기") : bulk ? "Remove all in radius" : "Remove tattoo";
				clear.addEventListener("click", () => apply(null));
				rows.appendChild(clear);
			}
			const q = filter.value.trim().toLowerCase();
			let shown = 0;
			for (const def of list) {
				const name = (isKorean && def.nameKo) || def.dn;
				const mods = ((isKorean && def.statsKo?.length ? def.statsKo : def.stats) || []).join(" / ");
				if (q && !name.toLowerCase().includes(q) && !mods.toLowerCase().includes(q) && !def.dn.toLowerCase().includes(q)) continue;
				if (++shown > 60) break;
				const row = document.createElement("button");
				row.type = "button";
				row.className = "block w-full text-left px-4 py-1.5 hover:bg-amber-900/40 border-b border-stone-800";
				const title = document.createElement("div");
				title.className = "text-xs text-amber-200 font-semibold";
				title.textContent = name;
				row.appendChild(title);
				if (mods) {
					const sub = document.createElement("div");
					sub.className = "text-[11px] text-sky-300 leading-4";
					sub.textContent = mods;
					row.appendChild(sub);
				}
				row.addEventListener("click", () => apply(def.dn));
				rows.appendChild(row);
			}
			if (!shown) {
				const none = document.createElement("div");
				none.className = "px-4 py-2 text-xs text-base-content/50";
				none.textContent = isKorean ? "새길 수 있는 문신이 없습니다." : "No tattoo fits this passive.";
				rows.appendChild(none);
			}
		};
		filter.addEventListener("input", render);
		render();
		const sx = node.x * scale + offsetX;
		const sy = node.y * scale + offsetY;
		panel.style.left = Math.max(0, Math.min(sx + 20, host.clientWidth - 400)) + "px";
		panel.style.top = Math.max(0, Math.min(sy, host.clientHeight - 160)) + "px";
		host.appendChild(panel);
		tattooPicker = panel;
		filter.focus();
	}

	function openJewelPicker(node: TreeNode) {
		closeJewelPicker();
		const options = Array.from(
			document.querySelectorAll<HTMLOptionElement>("#poeTreeJewelList option"),
		);
		if (!options.length) return;
		const host = canvas.parentElement as HTMLElement;
		const panel = document.createElement("div");
		panel.className =
			"absolute z-20 max-h-[60%] w-96 overflow-y-auto rounded shadow-2xl border border-amber-700/60 bg-stone-900/97";
		const head = document.createElement("div");
		head.className =
			"sticky top-0 border-y-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-sm px-6 py-1.5";
		head.textContent = (isKorean && node.nameKo ? node.nameKo : node.name) + (isKorean ? " — 주얼 장착" : " — socket jewel");
		panel.appendChild(head);
		const filter = document.createElement("input");
		filter.type = "text";
		filter.className = "w-full px-3 py-1.5 text-xs bg-stone-800 text-amber-100 border-b border-stone-700 outline-none";
		filter.placeholder = isKorean ? "주얼 이름/효과 검색" : "Filter by name or mod";
		panel.appendChild(filter);
		const list = document.createElement("div");
		panel.appendChild(list);
		const applyPick = (spec: string | null) => {
			const before = snapshot();
			if (spec) jewelPicks.set(node.id, spec);
			else jewelPicks.delete(node.id);
			commit(before);
			closeJewelPicker();
			// 링크(j=)와 요약 목록을 즉시 맞춘다 — 없으면 꽂자마자 링크를 복사했을 때 주얼이 빠진다
			updatePoints();
			syncUrl();
			draw();
		};
		// 타임리스는 정복자·시드를 골라야 반경 변환이 정해진다 — 고른 뒤 한 번 더 물어본다.
		const pick = (slug: string | null) => {
			const def = slug ? TIMELESS_JEWELS[slug] : null;
			if (!slug || !def) {
				applyPick(slug);
				return;
			}
			list.replaceChildren();
			const box = document.createElement("div");
			box.className = "px-3 py-2 space-y-2";
			const title = document.createElement("div");
			title.className = "text-xs text-amber-200 font-semibold";
			title.textContent = jewelName(slug);
			box.appendChild(title);
			const hint = document.createElement("div");
			hint.className = "text-[11px] text-base-content/50 leading-4";
			hint.textContent = isKorean
				? "정복자와 시드에 따라 반경 안 패시브가 다른 스탯으로 바뀝니다."
				: "Conqueror and seed decide how passives in radius are transformed.";
			box.appendChild(hint);
			const conqSel = document.createElement("select");
			conqSel.className = "select select-xs select-bordered w-full bg-stone-900 text-amber-100";
			for (const c of def.conquerors) {
				const opt = document.createElement("option");
				opt.value = c;
				opt.textContent = c;
				conqSel.appendChild(opt);
			}
			box.appendChild(conqSel);
			const seedInput = document.createElement("input");
			seedInput.type = "number";
			seedInput.min = String(def.min);
			seedInput.max = String(def.max);
			seedInput.value = String(Math.floor((def.min + def.max) / 2));
			seedInput.className = "input input-xs input-bordered w-full bg-stone-900 text-amber-100";
			seedInput.title = `${def.min} ~ ${def.max}`;
			box.appendChild(seedInput);
			const range = document.createElement("div");
			range.className = "text-[10px] font-mono text-base-content/40";
			range.textContent = (isKorean ? "시드 범위 " : "seed ") + def.min + " ~ " + def.max;
			box.appendChild(range);
			const ok = document.createElement("button");
			ok.type = "button";
			ok.className = "btn btn-xs btn-primary w-full";
			ok.textContent = isKorean ? "장착" : "Socket";
			ok.addEventListener("click", () => {
				// 범위를 벗어난 시드는 PoB 가 데이터를 못 찾아 계산이 비어 버린다 — 여기서 잘라 준다
				const seed = Math.max(def.min, Math.min(def.max, Number(seedInput.value) || def.min));
				applyPick(`${slug}:${conqSel.value}:${seed}`);
			});
			box.appendChild(ok);
			list.appendChild(box);
		};
		const render = () => {
			list.replaceChildren();
			if (jewelPicks.has(node.id)) {
				const clear = document.createElement("button");
				clear.type = "button";
				clear.className = "block w-full text-left px-4 py-2 text-xs text-rose-300 hover:bg-rose-900/30 border-b border-stone-800";
				clear.textContent = isKorean ? "주얼 빼기" : "Remove jewel";
				clear.addEventListener("click", () => pick(null));
				list.appendChild(clear);
			}
			const q = filter.value.trim().toLowerCase();
			let shown = 0;
			for (const opt of options) {
				const name = opt.value;
				const mods = opt.dataset.mods || "";
				if (q && !name.toLowerCase().includes(q) && !mods.toLowerCase().includes(q)) continue;
				if (++shown > 60) break; // 목록이 길어 렌더 상한 — 검색으로 좁히도록
				const row = document.createElement("button");
				row.type = "button";
				row.className = "block w-full text-left px-4 py-1.5 hover:bg-amber-900/40 border-b border-stone-800";
				const title = document.createElement("div");
				title.className = "text-xs text-amber-200 font-semibold";
				title.textContent = name;
				row.appendChild(title);
				if (mods) {
					const sub = document.createElement("div");
					sub.className = "text-[11px] text-sky-300 leading-4";
					sub.textContent = mods;
					row.appendChild(sub);
				}
				row.addEventListener("click", () => pick(opt.dataset.slug || null));
				list.appendChild(row);
			}
			if (!shown) {
				const none = document.createElement("div");
				none.className = "px-4 py-2 text-xs text-base-content/50";
				none.textContent = isKorean ? "검색 결과가 없습니다." : "No matches.";
				list.appendChild(none);
			}
		};
		filter.addEventListener("input", render);
		render();
		const sx = node.x * scale + offsetX;
		const sy = node.y * scale + offsetY;
		panel.style.left = Math.max(0, Math.min(sx + 20, host.clientWidth - 400)) + "px";
		panel.style.top = Math.max(0, Math.min(sy, host.clientHeight - 160)) + "px";
		host.appendChild(panel);
		jewelPicker = panel;
		filter.focus();
	}
	// 주얼 지정 문자열은 "slug" 또는 타임리스 "slug:정복자:시드" — 표시할 땐 slug 부분만 쓴다
	const jewelName = (spec: string) => {
		const slug = spec.split(":")[0];
		const base = document.querySelector<HTMLOptionElement>('#poeTreeJewelList option[data-slug="' + slug + '"]')?.value || slug;
		const parts = spec.split(":");
		return parts.length > 1 ? `${base} (${parts.slice(1).join(" ")})` : base;
	};

	// 무궁한(타임리스) 주얼 — 정복자·시드에 따라 반경 패시브 변환이 달라진다.
	// (API PoeOptimizeService.TIMELESS_JEWELS 와 같은 게임 고정 데이터. 한쪽만 고치면 어긋난다)
	const TIMELESS_JEWELS: Record<string, { conquerors: string[]; min: number; max: number }> = {
		"brutal-restraint": { conquerors: ["Asenath", "Deshret", "Nasima", "Balbala"], min: 500, max: 8000 },
		"lethal-pride": { conquerors: ["Kaom", "Kiloava", "Rakiata", "Akoya"], min: 10000, max: 18000 },
		"glorious-vanity": { conquerors: ["Doryani", "Xibaqua", "Zerphi", "Ahuana"], min: 100, max: 8000 },
		"militant-faith": { conquerors: ["Avarius", "Dominus", "Venarius", "Maxarius"], min: 2000, max: 10000 },
		"elegant-hubris": { conquerors: ["Cadiro", "Chitus", "Victario", "Caspiro"], min: 2000, max: 160000 },
		"heroic-tragedy": { conquerors: ["Vorana", "Uhtred", "Medved"], min: 100, max: 8000 },
	};

	// 클러스터 주얼 구성 팝업 — 노드 수와 작은 패시브 효과를 고르면 서브트리를 생성한다.
	function openClusterPicker(socket: TreeNode) {
		closeJewelPicker();
		const sizeName = CLUSTER_SIZE_NAME[socket.expansionJewel?.size ?? 0] || "Small";
		const def = clusterDefs?.[`${sizeName} Cluster Jewel`];
		if (!def) return;
		const host = canvas.parentElement as HTMLElement;
		const current = clusterPicks.get(socket.id);
		const skillEntries = Object.entries(def.skills || {}) as Array<[string, { name: string; stats?: string[]; statsKo?: string[]; tag?: string }]>;

		let chosenCount = current?.nodeCount ?? def.maxNodes;
		const socketMax = CLUSTER_SOCKET_MAX[sizeName] ?? 0;
		let chosenSockets = Math.min(current?.socketCount ?? 0, socketMax);
		const notableMax = CLUSTER_NOTABLE_MAX[sizeName] ?? 1;
		const chosenNotables: string[] = (current?.notables || []).slice(0, notableMax);
		let chosenSkill = current?.skillKey && def.skills?.[current.skillKey] ? current.skillKey : skillEntries[0]?.[0] || "";
		let showAllNotables = false;

		const panel = document.createElement("div");
		panel.className = "absolute z-20 flex max-h-[70%] w-80 flex-col rounded shadow-2xl border border-purple-700/60 bg-stone-900/97";
		const head = document.createElement("div");
		head.className = "border-b-2 border-purple-500/70 bg-gradient-to-b from-stone-700 to-stone-900 text-purple-100 text-center font-bold text-sm px-6 py-1.5";
		head.textContent = (isKorean ? "클러스터 주얼 — " : "Cluster jewel — ") + (isKorean ? CLUSTER_SIZE_KO[socket.expansionJewel?.size ?? 0] : sizeName);
		panel.appendChild(head);
		const body = document.createElement("div");
		body.className = "flex-1 overflow-y-auto";
		panel.appendChild(body);

		// 숫자 선택 줄(노드 수 / 주얼 소켓 수) — 공통 헬퍼
		const numberRow = (label: string, min: number, max: number, initial: number, onPick: (v: number) => void) => {
			const row = document.createElement("div");
			row.className = "flex items-center gap-1 px-3 py-2 border-b border-stone-800";
			const tag = document.createElement("span");
			tag.className = "text-[11px] text-stone-400 mr-1";
			tag.textContent = label;
			row.appendChild(tag);
			const buttons: HTMLButtonElement[] = [];
			for (let c = min; c <= max; c++) {
				const b = document.createElement("button");
				b.type = "button";
				b.className = "btn btn-xs " + (c === initial ? "btn-primary" : "btn-ghost");
				b.textContent = String(c);
				b.addEventListener("click", () => {
					onPick(c);
					buttons.forEach((other, i) => (other.className = "btn btn-xs " + (min + i === c ? "btn-primary" : "btn-ghost")));
				});
				buttons.push(b);
				row.appendChild(b);
			}
			body.appendChild(row);
		};
		numberRow(isKorean ? "노드 수" : "Nodes", def.minNodes, def.maxNodes, chosenCount, (v) => (chosenCount = v));
		if (socketMax > 0) {
			numberRow(isKorean ? "주얼 소켓" : "Sockets", 0, socketMax, chosenSockets, (v) => (chosenSockets = v));
		}

		// ---- 작은 패시브 효과(스킬) 선택 ----
		// 노터블 후보가 스킬에 따라 달라지므로 **스킬을 먼저 고른다**(게임 크래프트 순서와 동일).
		const skillBox = document.createElement("div");
		skillBox.className = "border-b border-stone-800";
		const skillHead = document.createElement("div");
		skillHead.className = "px-3 pt-2 text-[11px] text-stone-400";
		skillHead.textContent = isKorean ? "작은 패시브 효과" : "Small passive effect";
		skillBox.appendChild(skillHead);
		const skillList = document.createElement("div");
		skillList.className = "max-h-40 overflow-y-auto";
		skillBox.appendChild(skillList);
		const skillRows: Array<{ el: HTMLElement; key: string }> = [];
		const markSkills = () => {
			for (const r of skillRows) {
				r.el.className =
					"block w-full text-left px-4 py-1 border-b border-stone-800/60 " +
					(r.key === chosenSkill ? "bg-purple-900/50" : "hover:bg-purple-900/30");
			}
		};
		for (const [key, skill] of skillEntries) {
			const row = document.createElement("button");
			row.type = "button";
			const title = document.createElement("div");
			title.className = "text-xs text-purple-200 font-semibold";
			title.textContent = skill.name;
			row.appendChild(title);
			const sub = document.createElement("div");
			sub.className = "text-[11px] text-sky-300 leading-4";
			sub.textContent = ((isKorean && skill.statsKo?.length ? skill.statsKo : skill.stats) || []).join(" / ");
			row.appendChild(sub);
			row.addEventListener("click", () => {
				chosenSkill = key;
				markSkills();
				applyNotableFilter();
			});
			skillList.appendChild(row);
			skillRows.push({ el: row, key });
		}
		markSkills();
		body.appendChild(skillBox);

		// ---- 노터블 선택 ----
		// 실제 주얼은 "1 Added Passive Skill is <노터블>" 로 노터블을 얹는다. 이름이 곧 계약이라
		// 트리 데이터의 클러스터 노터블 목록에서 고른다 — 임의 이름이면 PoB 가 서브트리를 버린다.
		// 게다가 **어떤 노터블이 나올 수 있는지는 스킬 태그·주얼 크기가 정한다**(notableOptions).
		const notableBox = document.createElement("div");
		notableBox.className = "px-3 py-2";
		const notableHead = document.createElement("div");
		notableHead.className = "text-[11px] text-stone-400 mb-1";
		const renderHead = () => {
			notableHead.textContent =
				(isKorean ? "노터블" : "Notables") +
				` ${chosenNotables.length}/${notableMax}` +
				(chosenNotables.length ? " — " + chosenNotables.map((n) => (isKorean && clusterNotables.get(n)?.nameKo) || n).join(", ") : "");
		};
		renderHead();
		notableBox.appendChild(notableHead);
		const filter = document.createElement("input");
		filter.type = "text";
		filter.className = "input input-xs w-full bg-stone-950 border-stone-700 text-stone-200";
		filter.placeholder = isKorean ? "노터블 검색…" : "Search notables…";
		notableBox.appendChild(filter);
		const allToggle = document.createElement("label");
		allToggle.className = "mt-1 flex items-center gap-1 text-[11px] text-stone-500";
		const allBox = document.createElement("input");
		allBox.type = "checkbox";
		allBox.className = "checkbox checkbox-xs";
		allToggle.appendChild(allBox);
		const allText = document.createElement("span");
		allToggle.appendChild(allText);
		notableBox.appendChild(allToggle);
		const list = document.createElement("div");
		list.className = "mt-1 max-h-40 overflow-y-auto";
		notableBox.appendChild(list);

		const rows: Array<{ el: HTMLElement; name: string; hay: string; mark: () => void }> = [];
		for (const cn of Array.from(clusterNotables.values())) {
			const row = document.createElement("button");
			row.type = "button";
			const label = (isKorean && cn.nameKo) || cn.name;
			const mark = () => {
				const on = chosenNotables.indexOf(cn.name) >= 0;
				row.textContent = (on ? "✓ " : "") + label;
				row.className =
					"block w-full text-left px-2 py-0.5 text-[11px] rounded hover:bg-purple-900/40 " +
					(on ? "text-amber-300 font-semibold" : cn.keystone ? "text-rose-300" : "text-stone-300");
			};
			mark();
			row.addEventListener("click", () => {
				const at = chosenNotables.indexOf(cn.name);
				if (at >= 0) chosenNotables.splice(at, 1);
				else if (chosenNotables.length < notableMax) chosenNotables.push(cn.name);
				mark();
				renderHead();
			});
			list.appendChild(row);
			rows.push({ el: row, name: cn.name, hay: (cn.name + " " + (cn.nameKo || "")).toLowerCase(), mark });
		}
		// 이 주얼(크기 + 스킬 태그)에 실제로 붙을 수 있는 노터블인지
		const eligible = (name: string) => {
			const opt = clusterNotableOptions?.[name];
			if (!opt) return false;
			if (opt.sizes?.length && opt.sizes.indexOf(sizeName) < 0) return false;
			const tag = (def.skills?.[chosenSkill] as { tag?: string } | undefined)?.tag;
			return !!tag && (opt.tags || []).indexOf(tag) >= 0;
		};
		// 목록은 한 번만 만들고 필터로 숨긴다(재생성보다 싸고 선택 상태가 유지된다)
		function applyNotableFilter() {
			const q = filter.value.trim().toLowerCase();
			let shown = 0;
			for (const r of rows) {
				const ok = (showAllNotables || eligible(r.name)) && (!q || r.hay.indexOf(q) >= 0);
				r.el.style.display = ok ? "" : "none";
				if (ok) shown++;
			}
			allText.textContent = isKorean
				? `이 주얼에 없는 것까지 보기 (지금 ${shown}개)`
				: `show all notables (${shown} listed)`;
		}
		filter.addEventListener("input", applyNotableFilter);
		allBox.addEventListener("change", () => {
			showAllNotables = allBox.checked;
			applyNotableFilter();
		});
		applyNotableFilter();
		body.appendChild(notableBox);

		// ---- 적용 ----
		const footer = document.createElement("div");
		footer.className = "border-t border-stone-800 p-2";
		const apply = document.createElement("button");
		apply.type = "button";
		apply.className = "btn btn-sm btn-primary w-full";
		apply.textContent = isKorean ? "적용" : "Apply";
		apply.addEventListener("click", () => {
			const before = snapshot();
			// 소켓/노터블이 노드 수를 넘지 않게(작은 패시브 수가 음수가 되면 PoB 와 노드 구성이 어긋난다)
			const sockets = Math.min(chosenSockets, Math.max(0, chosenCount - 1));
			const notables = chosenNotables.slice(0, Math.max(0, Math.min(notableMax, chosenCount - sockets - 1)));
			clusterPicks.set(socket.id, { sizeName, nodeCount: chosenCount, skillKey: chosenSkill, notables, socketCount: sockets });
			rebuildClusterNodes();
			// 구성이 바뀌면 사라진 생성 노드의 할당도 정리되므로 포인트/요약을 다시 계산해야 한다
			// (안 하면 포인트 수와 "핵심 노드" 목록이 옛 상태로 남는다)
			updatePoints();
			commit(before);
			syncUrl(); // 구성 직후 링크에 반영 — 없으면 다른 편집을 하기 전까지 URL 에 클러스터가 빠진다
			panel.remove();
			jewelPicker = null;
			draw();
		});
		footer.appendChild(apply);
		panel.appendChild(footer);

		const sx = socket.x * scale + offsetX;
		const sy = socket.y * scale + offsetY;
		panel.style.left = Math.max(0, Math.min(sx + 20, host.clientWidth - 340)) + "px";
		panel.style.top = Math.max(0, Math.min(sy, host.clientHeight - 200)) + "px";
		host.appendChild(panel);
		jewelPicker = panel;
	}

	// ---- 주얼 슬롯 반경 (공식 뷰어와 동일하게 소형/중형/대형 반경 링을 보여준다) ----
	// 반경 값은 게임 고정 상수(JewelRadius). 반경 주얼(예: 효율적 훈련)은 이 안의 패시브 수로 효과가 정해져서
	// "이 슬롯이 몇 개를 덮는가"가 슬롯 선택의 핵심 정보다.
	// ⚠ 값은 **트리 버전마다 다르다** — 800/1200/1500 은 3.15 것이고, 3.16 이후는 아래 값이다
	//   (PoB Modules/Data.lua `data.jewelRadii["3_16"]`). 옛 값으로 세면 반경 안 패시브 수가 실제보다 적게 나온다.
	const JEWEL_RADII: { en: string; ko: string; r: number }[] = [
		{ en: "Small", ko: "소형", r: 960 },
		{ en: "Medium", ko: "중형", r: 1440 },
		{ en: "Large", ko: "대형", r: 1800 },
		{ en: "Very Large", ko: "초대형", r: 2400 },
		{ en: "Massive", ko: "거대", r: 2880 },
	];
	// 꽂은 주얼의 반경 링 아트(공식 시트) — 무궁한 주얼은 정복자 세력별로 링 색이 다르다.
	const TIMELESS_RING: Record<string, string> = {
		"glorious-vanity": "VaalJewelCircle1",
		"lethal-pride": "KaruiJewelCircle1",
		"brutal-restraint": "MarakethJewelCircle1",
		"militant-faith": "TemplarJewelCircle1",
		"elegant-hubris": "EternalEmpireJewelCircle1",
		"heroic-tragedy": "KalguurJewelCircle1",
	};
	/** 슬러그로 그 주얼의 반경(월드 단위). 반경 모드가 없는 주얼은 null. */
	function jewelRadiusOf(spec: string): { label: string; r: number } | null {
		const slug = spec.split(":")[0];
		const option = document.querySelector<HTMLOptionElement>('#poeTreeJewelList option[data-slug="' + slug + '"]');
		const label = option?.dataset.radius || "";
		const band = JEWEL_RADII.find((b) => b.en === label);
		return band ? { label: isKorean ? band.ko : band.en, r: band.r } : null;
	}
	// 반경 안의 패시브 수 — 주얼 슬롯/마스터리/클래스 시작/전직 노드는 반경 효과 대상이 아니라 제외.
	function jewelRadiusInfo(node: TreeNode) {
		return JEWEL_RADII.map((band) => {
			let total = 0;
			let alloc = 0;
			for (const n of nodes) {
				if (n.id === node.id || n.type === "jewel" || n.type === "mastery" || n.type === "class" || n.ascendancy) continue;
				const dx = n.x - node.x;
				const dy = n.y - node.y;
				if (dx * dx + dy * dy <= band.r * band.r) {
					total++;
					if (highlighted.has(n.id)) alloc++;
				}
			}
			return { label: isKorean ? band.ko : band.en, r: band.r, total, alloc };
		});
	}
	function showTooltip(node: TreeNode, clientX: number, clientY: number) {
		if (!tooltip) return;
		tooltip.replaceChildren();
		// 공식 뷰어식: 금색테 헤더(제목 중앙) + 어두운 본문(청색 스탯)
		const displayName = isKorean && node.nameKo ? node.nameKo : node.name;
		const header = document.createElement("div");
		header.className =
			"border-y-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-sm px-6 py-1.5";
		const inkedDn = tattooPicks.get(node.id);
		// 문신이 새겨졌으면 그 패시브는 **교체**된 상태다 — 툴팁도 문신 이름/효과를 보여줘야 화면과 계산이 일치한다
		header.textContent = inkedDn
			? tattooLabel(inkedDn)
			: displayName + (node.ascendancy ? " (" + ascendancyLabel(node.ascendancy) + ")" : "");
		tooltip.appendChild(header);
		// 마스터리: 고른 효과가 있으면 그 문장을, 없으면 선택지 개수를 보여준다
		let displayStats = isKorean && node.statsKo && node.statsKo.length ? node.statsKo : node.stats;
		if (inkedDn) displayStats = tattooLines(inkedDn);
		else if (isAtlas && node.type === "mastery") {
			// 아틀라스 마스터리는 할당 불가한 그룹 표지 — 이름만 있으면 클릭 가능해 보여 오해를 부른다
			displayStats = [isKorean ? "그룹 표지 — 할당할 수 없습니다" : "Group emblem — not allocatable"];
		} else if (node.masteryEffects?.length) {
			const picked = masteryPicks.get(node.id);
			const eff = picked !== undefined ? node.masteryEffects.find((e) => e.id === picked) : undefined;
			displayStats = eff
				? effectLines(eff)
				: [(isKorean ? "효과 " : "") + node.masteryEffects.length + (isKorean ? "개 중 선택 — 클릭" : " effects — click to pick")];
		}
		if (displayStats.length) {
			const body = document.createElement("div");
			body.className = "bg-stone-900/95 px-4 py-2 border-x border-b border-amber-900/50";
			for (const stat of displayStats) {
				const line = document.createElement("div");
				line.className = "text-xs text-sky-300 whitespace-pre-line leading-5";
				line.textContent = stat;
				body.appendChild(line);
			}
			tooltip.appendChild(body);
		}
		// 주얼 슬롯이면 반경별 "덮는 패시브 수(할당/전체)" 를 덧붙인다
		// 클러스터 전용 소켓(트리 외곽 42개)은 반경 주얼을 넣는 자리가 아니다 — 반경 수치를 보여주면 오해를 부른다.
		if (node.type === "jewel" && node.expansionJewel && !isAtlas) {
			const box = document.createElement("div");
			box.className = "bg-stone-900/95 px-4 py-2 border-x border-b border-amber-900/50";
			const line = document.createElement("div");
			line.className = "text-[11px] font-mono text-purple-300 leading-5";
			const sizeName = CLUSTER_SIZE_KO[node.expansionJewel.size] || "";
			line.textContent = isKorean
				? `클러스터 주얼 전용 (${sizeName})`
				: `Cluster jewel socket (${CLUSTER_SIZE_NAME[node.expansionJewel.size] || ""})`;
			box.appendChild(line);
			// 이 크기의 클러스터 주얼이 무엇을 붙여주는지 — 계획 단계에서 알아야 하는 정보
			const def = clusterDefs?.[CLUSTER_DEF_KEY[node.expansionJewel.size]];
			if (def) {
				const info = document.createElement("div");
				info.className = "text-[11px] font-mono text-base-content/60 leading-5";
				const skillCount = Object.keys(def.skills || {}).length;
				info.textContent = isKorean
					? `추가 패시브 ${def.minNodes}~${def.maxNodes}개 · 선택 가능 효과 ${skillCount}종`
					: `Adds ${def.minNodes}-${def.maxNodes} passives · ${skillCount} skill options`;
				box.appendChild(info);
			}
			const todo = document.createElement("div");
			todo.className = "text-[10px] text-base-content/40 leading-4";
			todo.textContent = isKorean ? "서브트리 편집은 아직 미지원" : "Subtree editing not supported yet";
			box.appendChild(todo);
			tooltip.appendChild(box);
		} else if (node.type === "jewel" && !isAtlas) {
			const box = document.createElement("div");
			box.className = "bg-stone-900/95 px-4 py-2 border-x border-b border-amber-900/50";
			const socketed = jewelPicks.get(node.id);
			if (socketed) {
				const line = document.createElement("div");
				line.className = "text-xs text-amber-200 font-semibold leading-5 mb-1";
				line.textContent = "◈ " + jewelName(socketed);
				box.appendChild(line);
			}
			for (const band of jewelRadiusInfo(node)) {
				const line = document.createElement("div");
				line.className = "text-[11px] font-mono font-semibold text-emerald-200 leading-5";
				line.textContent = `${band.label} ${band.r} — ${band.alloc}/${band.total}` + (isKorean ? " 패시브" : " passives");
				box.appendChild(line);
			}
			tooltip.appendChild(box);
		}
		// 비용 안내 — 미할당은 경로까지 몇 포인트 드는지, 할당은 해제 시 몇 개가 함께 빠지는지
		// (아틀라스 마스터리는 할당 대상이 아니라 "연결 불가" 비용 줄이 군더더기다)
		if (interactive && node.type !== "class" && !(isAtlas && node.type === "mastery")) {
			let costText = "";
			let costClass = "";
			if (highlighted.has(node.id)) {
				if (removalSet.size) {
					costText = (isKorean ? "해제 −" : "Refund −") + removalSet.size + (isKorean ? " 포인트" : " points");
					costClass = "text-rose-400";
				}
			} else if (hoverPath.length > 1) {
				costText = "+" + (hoverPath.length - 1) + (isKorean ? " 포인트" : " points");
				costClass = "text-amber-300";
			} else if (highlighted.size > 0) {
				costText = isKorean ? "연결 불가" : "Unreachable";
				costClass = "text-base-content/50";
			}
			if (costText) {
				const foot = document.createElement("div");
				foot.className = "bg-stone-950/95 px-4 py-1 border-x border-b border-amber-900/50 text-[11px] font-mono " + costClass;
				foot.textContent = costText;
				tooltip.appendChild(foot);
			}
		}
		const parentRect = (tooltip.parentElement as HTMLElement).getBoundingClientRect();
		tooltip.style.left = Math.min(clientX - parentRect.left + 14, parentRect.width - 280) + "px";
		tooltip.style.top = clientY - parentRect.top + 14 + "px";
		tooltip.classList.remove("hidden");
	}
	function hideTooltip() {
		tooltip?.classList.add("hidden");
	}

	// 팬 + 클릭 할당(드래그와 클릭 구분: 이동량 작으면 클릭)
	let dragging = false;
	let dragMoved = false;
	let downX = 0;
	let downY = 0;
	let lastX = 0;
	let lastY = 0;
	canvas.addEventListener("mousedown", (event) => {
		if (event.button !== 0) return; // 좌클릭만 팬/할당 — 우클릭은 contextmenu 가 처리
		dragging = true;
		dragMoved = false;
		downX = event.clientX;
		downY = event.clientY;
		lastX = event.clientX;
		lastY = event.clientY;
		canvas.style.cursor = "grabbing";
	});
	globalThis.addEventListener("mouseup", (event) => {
		if (dragging && !dragMoved) {
			// 클릭 — 노드 토글 (열려 있던 팝업/메뉴는 먼저 닫는다)
			closeMasteryPicker();
			closeNodeMenu();
			const rect = canvas.getBoundingClientRect();
			const node = findNodeAt(event.clientX - rect.left, event.clientY - rect.top);
			if (node) toggleNode(node);
		}
		dragging = false;
		canvas.style.cursor = "grab";
	});
	// 우클릭 — 노드 메뉴 (브라우저 기본 메뉴는 막는다)
	canvas.addEventListener("contextmenu", (event) => {
		event.preventDefault();
		const rect = canvas.getBoundingClientRect();
		const node = findNodeAt(event.clientX - rect.left, event.clientY - rect.top);
		if (node) openNodeMenu(node, event.clientX, event.clientY);
		else closeNodeMenu();
	});
	globalThis.addEventListener("keydown", (event) => {
		if (event.key === "Escape") {
			closeNodeMenu();
			closeMasteryPicker();
			closeJewelPicker();
			closeTattooPicker();
		}
		// 검색창 등 입력 중에는 브라우저 기본 실행취소를 방해하지 않는다.
		const tag = (event.target as HTMLElement | null)?.tagName;
		if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;
		if (!interactive || !(event.ctrlKey || event.metaKey)) return;
		const key = event.key.toLowerCase();
		if (key === "z" && !event.shiftKey) {
			event.preventDefault();
			undo();
		} else if ((key === "z" && event.shiftKey) || key === "y") {
			event.preventDefault();
			redo();
		}
	});

	canvas.addEventListener("mousemove", (event) => {
		if (dragging) {
			if (Math.abs(event.clientX - downX) + Math.abs(event.clientY - downY) > 4) dragMoved = true;
			offsetX += event.clientX - lastX;
			offsetY += event.clientY - lastY;
			lastX = event.clientX;
			lastY = event.clientY;
			hideTooltip();
			scheduleDraw(); // 고빈도 mousemove 를 rAF 로 병합(즉시 draw 하면 이동 1건당 전체 렌더)
			return;
		}
		const rect = canvas.getBoundingClientRect();
		const node = findNodeAt(event.clientX - rect.left, event.clientY - rect.top);
		if (node !== hovered) {
			hovered = node;
			hoverPath = [];
			removalSet.clear();
			if (node && interactive) {
				// 할당 노드면 "해제 시 함께 빠질 집합", 미할당이면 "여기까지 최단 경로"
				if (highlighted.has(node.id)) for (const id of computeRemoval(node.id)) removalSet.add(id);
				else hoverPath = computeHoverPath(node.id);
			}
			scheduleDraw();
		}
		if (node && (node.name || node.stats.length)) showTooltip(node, event.clientX, event.clientY);
		else hideTooltip();
	});
	canvas.addEventListener("mouseleave", () => {
		hovered = null;
		hoverPath = [];
		removalSet.clear();
		hideTooltip();
		scheduleDraw();
	});

	// 줌 (커서 고정)
	canvas.addEventListener(
		"wheel",
		(event) => {
			event.preventDefault();
			const rect = canvas.getBoundingClientRect();
			const mouseX = event.clientX - rect.left;
			const mouseY = event.clientY - rect.top;
			const factor = Math.pow(1.0015, -event.deltaY);
			const nextScale = Math.min(0.6, Math.max(0.008, scale * factor));
			offsetX = mouseX - ((mouseX - offsetX) / scale) * nextScale;
			offsetY = mouseY - ((mouseY - offsetY) / scale) * nextScale;
			scale = nextScale;
			hideTooltip();
			scheduleDraw();
		},
		{ passive: false },
	);

	globalThis.addEventListener("resize", scheduleDraw);

	// 매니페스트 → 트리 데이터 순으로 로드
	function loadTree() {
		// no-cache = 항상 서버에 재검증(ETag) — 게임 패치로 트리 JSON 이 바뀌어도 옛 캐시를 물지 않는다.
		// (1MB 라 no-store 로 매번 새로 받지는 않고 304 로 재사용)
		return fetch(treeSrc, { cache: "no-cache" })
				.then((response) => response.json())
				.then((data) => {
					nodes = data.nodes;
					edges = data.edges;
					groups = data.groups || {};
					// 클러스터 주얼 노터블 — 좌표가 없어(그룹 밖) 일반 노드 목록엔 못 들어간다. 이름으로만 참조된다.
					for (const cn of data.clusterNotables || []) clusterNotables.set(cn.name, cn);
					extraImages = data.extraImages || [];
					if (data.constants) {
						orbitRadii = data.constants.orbitRadii || orbitRadii;
						skillsPerOrbit = data.constants.skillsPerOrbit || skillsPerOrbit;
					}
					// 직업별 전직 목록·혈맹·최대 포인트는 트리 데이터가 그대로 들고 있다(하드코딩 금지)
					classAsc = (data.classes || []).map((cls: { ascendancies: string[] }) => cls.ascendancies || []);
					bloodlines = data.bloodlines || [];
					// 아틀라스는 원본 root 노드의 out(지도 중앙 1개)이 시작점 — 여기서부터 이어야 할당된다
					atlasRoot = isAtlas && data.startNodes?.length ? data.startNodes[0] : null;
					maxPoints = data.points?.totalPoints || 0;
					maxAscPoints = data.points?.ascendancyPoints || 0;
					for (const node of nodes) nodeById.set(node.id, node);
					// 인접 그래프(양방향) — 클릭 할당 연결성 검증용
					for (const [a, b] of edges) {
						if (!adjacency.has(a)) adjacency.set(a, []);
						if (!adjacency.has(b)) adjacency.set(b, []);
						adjacency.get(a)!.push(b);
						adjacency.get(b)!.push(a);
					}
					// 클래스 시작노드 매핑(classId → 노드 id)
					for (const node of nodes) {
						if (node.type === "class" && CLASS_START_CLASSID[node.name] !== undefined) {
							classStartByClassId.set(CLASS_START_CLASSID[node.name], node.id);
						}
					}
					applyPendingClass(); // ?class=/?asc= (레거시 nodes 링크) 를 루트 결정 전에 반영
					if (!isAtlas) {
						// 클러스터 정의는 비동기 로드 — 링크로 들어온 구성(c=)은 정의가 온 뒤에 복원해야 한다
						loadClusterDefs(() => {
							if (!pendingClusters.length) return;
							for (const pc of pendingClusters) {
								clusterPicks.set(pc.socketId, { sizeName: pc.sizeName, nodeCount: pc.nodeCount, skillKey: pc.skillKey, notables: pc.notables || [], socketCount: pc.socketCount || 0 });
							}
							pendingClusters.length = 0;
							rebuildClusterNodes();
							// 생성된 뒤에야 클러스터 노드 할당(t= 의 클러스터 섹션)을 복원할 수 있다
							for (const id of pendingClusterNodes) if (nodeById.has(id)) highlighted.add(id);
							pendingClusterNodes.length = 0;
							updatePoints();
							draw();
						});
					}
					// 편집 모드: 현재 직업의 시작노드를 루트로 항상 할당
					if (interactive) {
						const root = rootNode();
						if (root !== undefined) highlighted.add(root);
					}
					let bounds = data.bounds;
					// 할당 영역에 맞춰 확대하는 건 "실제 트리를 불러왔을 때"만. 시작 노드 하나뿐이면
					// 그 점에 맞춰 과하게 확대돼 이웃이 화면 밖으로 나간다 → 전체 트리를 보여준다.
					if (highlighted.size > 1) {
						const allocated = nodes.filter((node) => highlighted.has(node.id));
						if (allocated.length) {
							const padding = 2200;
							bounds = {
								minX: Math.min(...allocated.map((n) => n.x)) - padding,
								minY: Math.min(...allocated.map((n) => n.y)) - padding,
								maxX: Math.max(...allocated.map((n) => n.x)) + padding,
								maxY: Math.max(...allocated.map((n) => n.y)) + padding,
							};
						}
					}
					const width = canvas.clientWidth;
					const height = canvas.clientHeight;
					scale = Math.min(0.6, Math.min(width / (bounds.maxX - bounds.minX), height / (bounds.maxY - bounds.minY)) * 0.95);
					offsetX = width / 2 - ((bounds.minX + bounds.maxX) / 2) * scale;
					offsetY = height / 2 - ((bounds.minY + bounds.maxY) / 2) * scale;
					updatePoints();
					draw();
					setupControls();
				})
				.catch((error) => console.warn("passive tree load failed", error));
	}

	// 매니페스트는 항상 최신으로(no-store) — 시트 URL 에 박힌 ?v 로 시트 캐시가 갈리므로,
	// 매니페스트만 새로 받으면 재생성된 시트를 일반 새로고침으로도 바로 받는다(깨진 캐시 회피).
	fetch(spritesSrc, { cache: "no-store" })
		.then((r) => r.json())
		.then((m) => {
			sprites = m;
			computeRadii();
			// 시트 미리 로드 — 큰 JPEG(skills)가 프레임보다 늦게 떠서 "아이콘 없는 빈 프레임"으로
			// 보이는 창을 없앤다. 로드 완료 시 각자 scheduleDraw 로 재그림.
			const seen = new Set<string>();
			for (const sp of Object.values(sprites)) {
				if (sp && sp.file && !seen.has(sp.file)) {
					seen.add(sp.file);
					getSheet(sp.file);
				}
			}
		})
		.catch((e) => console.warn("tree sprites load failed", e))
		.then(loadTree);
})();
