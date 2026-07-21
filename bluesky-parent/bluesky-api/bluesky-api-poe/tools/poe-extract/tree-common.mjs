// 패시브/아틀라스 트리 공용 파서 — GGG 공식 익스포트 data.json 을 뷰어용 경량 JSON 으로 변환.
// 스킬 트리와 아틀라스 트리가 동일 포맷(groups/nodes/constants/sprites)이라 이 로직을 공유한다.
// 프론트가 게임식으로 그릴 수 있도록 궤도/그룹 구조(constants·groups·orbit)를 함께 emit 한다.

/**
 * 한국어 조인 맵 생성 — PassiveSkills(영/한) + 스탯 문장 파서.
 * GGG 트리 익스포트(스킬/아틀라스 모두)는 영어 전용이라 PassiveSkillGraphId 로 조인한다.
 * 아틀라스 노드도 같은 PassiveSkills 테이블에 들어 있다(867/867 조인 확인).
 * @returns Map<graphId, {nameKo, statsKo}>
 */
export function buildKoreanMap({ describe, statsTable, passivesEn, passivesKo }) {
	const koByGraphId = new Map();
	passivesEn.forEach((passive, i) => {
		if (passive.PassiveSkillGraphId == null) return;
		const statValues = new Map();
		(passive.Stats || []).forEach((statIndex, statPosition) => {
			const value = passive["Stat" + (statPosition + 1) + "Value"] ?? 0;
			statValues.set(statsTable[statIndex].Id, value);
		});
		koByGraphId.set(passive.PassiveSkillGraphId, {
			nameKo: passivesKo[i]?.Name || null,
			statsKo: describe(statValues, "Korean"),
		});
	});
	return koByGraphId;
}

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
			// 클러스터 주얼 소켓(트리 외곽)은 크기별 전용 프레임 아트를 쓴다. 0=Small 1=Medium 2=Large.
			clusterSize: node.expansionJewel ? node.expansionJewel.size : undefined,
			// 마스터리는 노드 자체가 아니라 "효과 하나"를 골라 찍는다. effect id 는 GGG URL 인코딩(마스터리 4바이트)에 그대로 들어감.
			masteryEffects: node.masteryEffects?.length
				? node.masteryEffects.map((eff) => ({ id: eff.effect, stats: eff.stats || [] }))
				: undefined,
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
		// 혈맹(대체 전직) — 배열 순서가 GGG URL 인코딩의 secondary ascendancy id 와 일치.
		bloodlines: (tree.alternate_ascendancies || []).map((alt) => ({ id: alt.id, name: alt.name })),
		// 최대 포인트 — 패시브 { totalPoints:123, ascendancyPoints:8 }, 아틀라스 { totalPoints:138 }
		points: tree.points || null,
		// 시작 노드 — 원본의 특수 노드 "root"(숫자 id 가 아니라 노드 목록에선 빠진다) 의 out.
		// 패시브는 7개 직업 시작점, 아틀라스는 지도 중앙 1개. 아틀라스 할당 연결성 판정의 기준점이 된다.
		startNodes: (tree.nodes?.root?.out || []).map(Number).filter(Number.isFinite),
		groups,
		nodes,
		edges,
	};
}
