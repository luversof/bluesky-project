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
		flavour?: string;
		flavourKo?: string;
		options?: { id: number; name: string; nameKo?: string; stats: string[]; statsKo?: string[] }[];
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
	const spriteBase = canvas.getAttribute("data-sprite-base") || src.replace(/passive-tree\.json$/, "tree-sprites/");
	let data: TreeData | null = null;
	let klass: Klass | null = null;
	let asc: Asc | null = null;
	const allocated = new Set<number>();
	// 능력치 노드("+5 아무 능력치") 선택 — 1 힘 · 2 민첩 · 3 지능(PoB AttributeOverride 와 같은 번호). 인게임도 찍을 때 고른다(10-01)
	const attrPick = new Map<number, number>();
	const ATTR_COLOR = ["", "#e05a4a", "#5fbf5f", "#5a8ee0"];
	/** 노드가 실제로 주는 줄 — 능력치 노드는 고른 능력치의 줄(안 골랐으면 "+5 아무 능력치" 그대로) */
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
				const on = allocated.has(n.id) && allocated.has(m.id);
				ctx.strokeStyle = on ? "#e8c46a" : searching ? "rgba(120,110,95,0.25)" : "rgba(120,110,95,0.55)";
				ctx.lineWidth = Math.max(dpr, (on ? 16 : 10) * scale); // scale 은 이미 기기 픽셀 기준
				strokeConnection(n, m, c.orbit);
			}
		}
		// 노드
		for (const n of nodes.values()) {
			if (!visible(n)) continue;
			const [sx, sy] = toScreen(n.x, n.y);
			if (sx < -60 || sy < -60 || sx > canvas!.width + 60 || sy > canvas!.height + 60) continue;
			const r = Math.max(2 * dpr, (RADIUS[n.kind] || 36) * scale);
			const on = allocated.has(n.id);
			const hit = matches.has(n.id);
			ctx.beginPath();
			ctx.arc(sx, sy, r, 0, Math.PI * 2);
			ctx.fillStyle = on ? "#f0d488" : COLOR[n.kind] || "#8c8374";
			ctx.globalAlpha = searching && !hit && !on ? 0.3 : 1;
			ctx.fill();
			// 충분히 크게 보일 때만 아이콘(원 안에 잘라서) — 할당 안 된 노드는 인게임처럼 어둡게
			const sp = (n as any).icon ? sprites[(n as any).icon] : undefined;
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
		return out;
	}
	function allocatable(n: TreeNode): boolean {
		if (!visible(n)) return false;
		if (n.kind === "classStart") return false; // 시작점은 공짜(점수 없음) — 다른 직업 시작점은 지나갈 수 없다
		return true;
	}
	/** 할당된 집합(+시작점)에서 target 까지 가장 짧은 경로(BFS). 없으면 null. */
	function pathTo(target: number): number[] | null {
		const from = new Set<number>([...startIds(), ...allocated]);
		if (from.has(target)) return [];
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
		const reach = new Set<number>(startIds());
		const queue = [...reach];
		while (queue.length) {
			const cur = queue.shift()!;
			for (const nb of nodes.get(cur)?.adj || []) {
				if (allocated.has(nb) && !reach.has(nb)) {
					reach.add(nb);
					queue.push(nb);
				}
			}
		}
		for (const id of [...allocated]) if (!reach.has(id)) allocated.delete(id);
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
			prune();
		} else {
			const path = pathTo(n.id);
			if (path === null) {
				flash(t("시작점에서 이어지지 않는 노드입니다", "Not connected to your start"));
				return;
			}
			for (const id of path) allocated.add(id);
		}
		afterChange();
		record(before);
	}
	function afterChange() {
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
		let main = 0, ascPts = 0;
		for (const id of allocated) {
			const n = nodes.get(id);
			if (!n) continue;
			if (n.ascendancy) {
				if (n.kind !== "ascendancyStart") ascPts++;
			} else main++;
		}
		if (points) points.textContent = t(`패시브 ${main}점 · 전직 ${ascPts}점`, `Passives ${main} · Ascendancy ${ascPts}`);
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
		if (attrPick.size) p.set("s", [...attrPick].sort((a, b) => a[0] - b[0]).map(([id, v]) => id + ":" + v).join(","));
		const h = p.toString();
		history.replaceState(null, "", location.pathname + location.search + (h ? "#" + h : ""));
	}
	function readHash() {
		const p = new URLSearchParams(location.hash.slice(1));
		const c = p.get("c");
		if (c) setClass(c, false);
		const a = p.get("a");
		if (a) setAsc(a, false);
		for (const s of (p.get("n") || "").split(",")) {
			const id = Number(s);
			const n = nodes.get(id);
			// PoB 빌드의 nodes 에는 시작점(직업·전직)도 들어 있다 — 시작점은 점수가 아니라 뺀다
			if (n && n.kind !== "classStart" && n.kind !== "ascendancyStart") allocated.add(id);
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
		const q = (search?.value || "").trim().toLowerCase();
		matches = new Set();
		matchOrder = [];
		matchCursor = -1;
		if (q.length >= 1) {
			for (const n of nodes.values()) {
				if (!visible(n)) continue;
				const hay = [n.name, n.nameKo || "", ...n.stats, ...(n.statsKo || [])].join("\n").toLowerCase();
				if (hay.includes(q)) {
					matches.add(n.id);
					matchOrder.push(n.id);
				}
			}
		}
		if (searchCount) searchCount.textContent = q ? t(`${matches.size}개 일치`, `${matches.size} matches`) : "";
		draw();
	}

	// ── 툴팁 ──
	function kindHeader(n: TreeNode): string {
		if (n.kind === "keystone" || n.kind === "notable" || n.kind === "jewel") return n.kind;
		if (n.ascendancy) return "ascendancy";
		return "normal";
	}
	function showTooltip(n: TreeNode, clientX: number, clientY: number) {
		if (!tooltip) return;
		tooltip.replaceChildren();
		const header = document.createElement("div");
		header.className = "poe-psheader poe-psheader-" + kindHeader(n);
		const ascName = n.ascendancy ? klass?.ascendancies.find((a) => a.id === n.ascendancy) : undefined;
		let title = isKorean && n.nameKo ? n.nameKo : n.name;
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
		const lines = nodeLines(n);
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
	// 우클릭 — 찍은 능력치 노드의 능력치를 힘 → 민첩 → 지능 → 선택 안 함 순으로 바꾼다(PoB 와 같은 번호)
	canvas.addEventListener("contextmenu", (e) => {
		const n = nodeAt(e.clientX, e.clientY);
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
		return JSON.stringify([klass?.name || "", asc?.id || "", [...allocated].sort((a, b) => a - b), [...attrPick].sort((a, b) => a[0] - b[0])]);
	}
	function record(before: string) {
		if (before === snapshot()) return;
		undoStack.push(before);
		if (undoStack.length > 200) undoStack.shift();
		redoStack.length = 0;
		updateHistoryButtons();
	}
	function restore(s: string) {
		const [c, a, ids, picks] = JSON.parse(s) as [string, string, number[], [number, number][]];
		klass = data?.classes.find((k) => k.name === c) || null;
		if (classSel) classSel.value = klass ? klass.name : "";
		fillAscSelect();
		asc = klass?.ascendancies.find((x) => x.id === a) || null;
		if (ascSel) ascSel.value = asc ? asc.id : "";
		allocated.clear();
		for (const id of ids) allocated.add(id);
		attrPick.clear();
		for (const [id, v] of picks || []) attrPick.set(id, v);
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
	function aggregateStats(): { text: string; count: number }[] {
		const acc = new Map<string, { parts: string[]; nums: number[]; signed: boolean[]; count: number }>();
		for (const id of allocated) {
			const n = nodes.get(id);
			if (!n || n.kind === "classStart" || n.kind === "ascendancyStart") continue;
			for (const raw of nodeLines(n)) {
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
		const keys = [...allocated].map((id) => nodes.get(id)).filter((n): n is TreeNode => !!n && (n.kind === "keystone" || n.kind === "notable"));
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
	}
	evalForm?.addEventListener("htmx:beforeRequest", () => {
		syncEvalForm();
		evalPanel?.classList.remove("hidden");
	});
	document.getElementById("poe2TreeEvalClose")?.addEventListener("click", () => evalPanel?.classList.add("hidden"));

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
	search?.addEventListener("keydown", (e) => {
		if (e.key !== "Enter" || !matchOrder.length) return;
		e.preventDefault();
		matchCursor = (matchCursor + 1) % matchOrder.length;
		centerOn(nodes.get(matchOrder[matchCursor]), Math.max(scale / dpr, 0.06));
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
