// PoE 패시브 트리 읽기 전용 뷰어 — /poe-data/passive-tree.json 을 캔버스에 렌더.
// 팬(드래그) / 줌(휠, 커서 중심) / 호버 툴팁.
(() => {
	const maybeCanvas = document.getElementById("poeTreeCanvas") as HTMLCanvasElement | null;
	const tooltip = document.getElementById("poeTreeTooltip");
	if (!maybeCanvas || maybeCanvas.dataset.poeTreeInitialized === "true") return;
	const canvas = maybeCanvas; // 클로저 안에서도 non-null 로 좁혀지도록 고정
	canvas.dataset.poeTreeInitialized = "true";
	const maybeContext = canvas.getContext("2d");
	if (!maybeContext) return;
	const context = maybeContext; // 클로저 안에서도 non-null 로 좁혀지도록 고정

	interface TreeNode {
		id: number;
		name: string;
		nameKo: string | null;
		type: string;
		x: number;
		y: number;
		stats: string[];
		statsKo: string[] | null;
		ascendancy: string | null;
	}

	const COLORS: Record<string, string> = {
		normal: "#7d8590",
		notable: "#d4a94e",
		keystone: "#cf6642",
		jewel: "#4ec9d4",
		mastery: "#9a6ad4",
		class: "#5a7bd4",
	};
	const RADII: Record<string, number> = {
		normal: 28,
		notable: 44,
		keystone: 62,
		jewel: 40,
		mastery: 34,
		class: 90,
	};

	let nodes: TreeNode[] = [];
	let edges: number[][] = [];
	const nodeById = new Map<number, TreeNode>();
	let scale = 0.03;
	let offsetX = 0;
	let offsetY = 0;
	let hovered: TreeNode | null = null;

	// ?nodes=1,2,3 — PoB 빌드 임포트에서 넘어온 할당 노드 강조
	const highlighted = new Set<number>();
	for (const token of (new URLSearchParams(globalThis.location.search).get("nodes") || "").split(",")) {
		const id = Number(token);
		if (Number.isFinite(id) && id > 0) highlighted.add(id);
	}
	const hasHighlight = highlighted.size > 0;

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

		// 간선 (하이라이트 모드에서는 미할당 간선을 흐리게, 할당 경로는 금색으로 덧그림)
		context.strokeStyle = hasHighlight ? "rgba(125, 133, 144, 0.12)" : "rgba(125, 133, 144, 0.28)";
		context.lineWidth = Math.max(0.4, 26 * scale);
		context.beginPath();
		for (const [fromId, toId] of edges) {
			const from = nodeById.get(fromId);
			const to = nodeById.get(toId);
			if (!from || !to) continue;
			context.moveTo(from.x * scale + offsetX, from.y * scale + offsetY);
			context.lineTo(to.x * scale + offsetX, to.y * scale + offsetY);
		}
		context.stroke();
		if (hasHighlight) {
			context.strokeStyle = "rgba(212, 169, 78, 0.9)";
			context.lineWidth = Math.max(0.8, 34 * scale);
			context.beginPath();
			for (const [fromId, toId] of edges) {
				if (!highlighted.has(fromId) || !highlighted.has(toId)) continue;
				const from = nodeById.get(fromId);
				const to = nodeById.get(toId);
				if (!from || !to) continue;
				context.moveTo(from.x * scale + offsetX, from.y * scale + offsetY);
				context.lineTo(to.x * scale + offsetX, to.y * scale + offsetY);
			}
			context.stroke();
		}

		// 노드
		for (const node of nodes) {
			const screenX = node.x * scale + offsetX;
			const screenY = node.y * scale + offsetY;
			const radius = (RADII[node.type] || 28) * scale;
			if (screenX < -radius || screenY < -radius || screenX > width + radius || screenY > height + radius) continue;
			const isAllocated = highlighted.has(node.id);
			context.globalAlpha = hasHighlight && !isAllocated ? (node.ascendancy ? 0.12 : 0.22) : node.ascendancy ? 0.55 : 1;
			if (isAllocated) context.globalAlpha = 1;
			context.fillStyle = COLORS[node.type] || COLORS.normal;
			context.beginPath();
			context.arc(screenX, screenY, Math.max(radius, 1), 0, Math.PI * 2);
			context.fill();
			if (isAllocated) {
				context.strokeStyle = "#d4a94e";
				context.lineWidth = Math.max(1, 10 * scale);
				context.stroke();
			}
			if (node === hovered) {
				context.globalAlpha = 1;
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
			const radius = Math.max((RADII[node.type] || 28) * scale, 6);
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

	function showTooltip(node: TreeNode, clientX: number, clientY: number) {
		if (!tooltip) return;
		tooltip.replaceChildren();
		const title = document.createElement("div");
		title.className = "font-bold text-sm mb-1";
		const displayName = node.nameKo || node.name;
		title.textContent = displayName + (node.ascendancy ? " (" + node.ascendancy + ")" : "");
		tooltip.appendChild(title);
		for (const stat of node.statsKo && node.statsKo.length ? node.statsKo : node.stats) {
			const line = document.createElement("div");
			line.className = "text-xs text-sky-300/90 whitespace-pre-line leading-5";
			line.textContent = stat;
			tooltip.appendChild(line);
		}
		const parentRect = (tooltip.parentElement as HTMLElement).getBoundingClientRect();
		tooltip.style.left = Math.min(clientX - parentRect.left + 14, parentRect.width - 280) + "px";
		tooltip.style.top = clientY - parentRect.top + 14 + "px";
		tooltip.classList.remove("hidden");
	}

	function hideTooltip() {
		tooltip?.classList.add("hidden");
	}

	// 팬
	let dragging = false;
	let lastX = 0;
	let lastY = 0;
	canvas.addEventListener("mousedown", (event) => {
		dragging = true;
		lastX = event.clientX;
		lastY = event.clientY;
		canvas.style.cursor = "grabbing";
	});
	globalThis.addEventListener("mouseup", () => {
		dragging = false;
		canvas.style.cursor = "grab";
	});
	canvas.addEventListener("mousemove", (event) => {
		if (dragging) {
			offsetX += event.clientX - lastX;
			offsetY += event.clientY - lastY;
			lastX = event.clientX;
			lastY = event.clientY;
			hideTooltip();
			draw();
			return;
		}
		const rect = canvas.getBoundingClientRect();
		const node = findNodeAt(event.clientX - rect.left, event.clientY - rect.top);
		if (node !== hovered) {
			hovered = node;
			draw();
		}
		if (node && (node.name || node.stats.length)) {
			showTooltip(node, event.clientX, event.clientY);
		} else {
			hideTooltip();
		}
	});
	canvas.addEventListener("mouseleave", () => {
		hovered = null;
		hideTooltip();
		draw();
	});

	// 줌 (커서 위치 고정)
	canvas.addEventListener(
		"wheel",
		(event) => {
			event.preventDefault();
			const rect = canvas.getBoundingClientRect();
			const mouseX = event.clientX - rect.left;
			const mouseY = event.clientY - rect.top;
			const factor = Math.pow(1.0015, -event.deltaY);
			const nextScale = Math.min(0.6, Math.max(0.012, scale * factor));
			offsetX = mouseX - ((mouseX - offsetX) / scale) * nextScale;
			offsetY = mouseY - ((mouseY - offsetY) / scale) * nextScale;
			scale = nextScale;
			hideTooltip();
			draw();
		},
		{ passive: false },
	);

	globalThis.addEventListener("resize", draw);

	fetch("/poe-data/passive-tree.json")
		.then((response) => response.json())
		.then((data) => {
			nodes = data.nodes;
			edges = data.edges;
			for (const node of nodes) nodeById.set(node.id, node);
			// 초기 배치: 전체 트리(하이라이트 모드면 할당 노드 영역)가 화면에 들어오도록 맞춤
			let bounds = data.bounds;
			if (hasHighlight) {
				const allocated = nodes.filter((node) => highlighted.has(node.id));
				if (allocated.length) {
					const padding = 2200;
					bounds = {
						minX: Math.min(...allocated.map((node) => node.x)) - padding,
						minY: Math.min(...allocated.map((node) => node.y)) - padding,
						maxX: Math.max(...allocated.map((node) => node.x)) + padding,
						maxY: Math.max(...allocated.map((node) => node.y)) + padding,
					};
				}
			}
			const width = canvas.clientWidth;
			const height = canvas.clientHeight;
			scale = Math.min(0.6, Math.min(width / (bounds.maxX - bounds.minX), height / (bounds.maxY - bounds.minY)) * 0.95);
			offsetX = width / 2 - ((bounds.minX + bounds.maxX) / 2) * scale;
			offsetY = height / 2 - ((bounds.minY + bounds.maxY) / 2) * scale;
			draw();
		})
		.catch((error) => console.warn("passive tree load failed", error));
})();
