// 패시브/아틀라스 트리 공용 파서 — GGG 공식 익스포트 data.json 을 뷰어용 경량 JSON 으로 변환.
// 스킬 트리와 아틀라스 트리가 동일 포맷(groups/nodes/constants/sprites)이라 이 로직을 공유한다.
// 프론트가 게임식으로 그릴 수 있도록 궤도/그룹 구조(constants·groups·orbit)를 함께 emit 한다.

function nodeType(node) {
	if (node.classStartIndex != null) return "class";
	if (node.isKeystone) return "keystone";
	if (node.isNotable) return "notable";
	if (node.isMastery) return "mastery";
	if (node.isJewelSocket) return "jewel";
	return "normal";
}

/**
 * @param tree  파싱한 data.json
 * @param koByGraphId  Map<graphId,{nameKo,statsKo}> (없으면 영문만)
 * @returns 뷰어용 { bounds, constants, classes, groups, nodes, edges }
 */
export function buildTree(tree, koByGraphId = null) {
	const { orbitRadii, skillsPerOrbit } = tree.constants;

	const nodes = [];
	const positioned = new Set();
	for (const [id, node] of Object.entries(tree.nodes)) {
		if (node.group == null || node.orbit == null || node.orbitIndex == null) continue; // 클러스터 주얼 템플릿 등
		const group = tree.groups[node.group];
		if (!group) continue;
		const angle = (2 * Math.PI * node.orbitIndex) / skillsPerOrbit[node.orbit];
		const radius = orbitRadii[node.orbit];
		const ko = koByGraphId ? koByGraphId.get(Number(id)) : null;
		nodes.push({
			id: Number(id),
			name: node.name || "",
			nameKo: ko?.nameKo || null,
			type: nodeType(node),
			group: Number(node.group),
			orbit: node.orbit,
			orbitIndex: node.orbitIndex,
			x: Math.round(group.x + radius * Math.sin(angle)),
			y: Math.round(group.y - radius * Math.cos(angle)),
			stats: node.stats || [],
			statsKo: ko?.statsKo?.length ? ko.statsKo : null,
			ascendancy: node.ascendancyName || null,
			ascendancyStart: node.isAscendancyStart ? true : undefined,
			// icon = 원본 경로(스프라이트 coords 키와 정확히 일치) — 프론트가 타입별 시트에서 blit
			icon: node.icon || null,
		});
		positioned.add(Number(id));
	}

	// 간선: out 기준, 양 끝이 배치된 노드일 때만(a<b 정규화로 중복 제거)
	const edgeSet = new Set();
	for (const [id, node] of Object.entries(tree.nodes)) {
		const from = Number(id);
		if (!positioned.has(from)) continue;
		for (const outId of node.out || []) {
			const to = Number(outId);
			if (!positioned.has(to)) continue;
			edgeSet.add(from < to ? from + "-" + to : to + "-" + from);
		}
	}
	const edges = [...edgeSet].map((k) => k.split("-").map(Number));

	// 그룹: 좌표 + 배경(있으면). 프론트가 그룹 배경 스프라이트를 그리고, 궤도 arc 연결에 group/orbit 을 쓴다.
	const groups = {};
	for (const [gid, g] of Object.entries(tree.groups)) {
		groups[gid] = {
			x: g.x,
			y: g.y,
			orbits: g.orbits || [],
			background: g.background || null,
		};
	}

	return {
		bounds: { minX: tree.min_x, minY: tree.min_y, maxX: tree.max_x, maxY: tree.max_y },
		constants: { orbitRadii, skillsPerOrbit },
		// 직업별 전직 목록 — 배열 순서가 PoB Spec 의 ascendClassId(1부터) 와 일치(아틀라스엔 없음)
		classes: (tree.classes || []).map((cls) => ({
			name: cls.name,
			ascendancies: (cls.ascendancies || []).map((asc) => asc.name),
		})),
		groups,
		nodes,
		edges,
	};
}
