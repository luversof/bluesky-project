// 패시브/아틀라스 트리 공용 파서 — GGG 공식 익스포트 data.json 을 뷰어용 경량 JSON 으로 변환.
// 스킬 트리와 아틀라스 트리가 동일 포맷(groups/nodes/constants/sprites)이라 이 로직을 공유한다.
// 프론트가 게임식으로 그릴 수 있도록 궤도/그룹 구조(constants·groups·orbit)를 함께 emit 한다.

/**
 * 한국어 조인 맵 생성 — PassiveSkills(영/한) + 스탯 문장 파서.
 * GGG 트리 익스포트(스킬/아틀라스 모두)는 영어 전용이라 PassiveSkillGraphId 로 조인한다.
 * 아틀라스 노드도 같은 PassiveSkills 테이블에 들어 있다(867/867 조인 확인).
 * @returns Map<graphId, {nameKo, statsKo}>
 */
// 궤도별 각도 — GGG 공식 스펙(skilltree-export README): 16노드/40노드 궤도는 **균등 분할이 아니다**.
// 균등식으로 계산하면 해당 궤도의 홀수 인덱스 노드가 최대 ~44단위(반지름 335 기준 7.5도) 어긋난다.
// PoB(PassiveTree:CalcOrbitAngles)와 동일한 표를 쓴다.
const ORBIT_ANGLES_16 = [0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 240, 270, 300, 315, 330];
const ORBIT_ANGLES_40 = [
	0, 10, 20, 30, 40, 45, 50, 60, 70, 80, 90, 100, 110, 120, 130, 135, 140, 150, 160, 170,
	180, 190, 200, 210, 220, 225, 230, 240, 250, 260, 270, 280, 290, 300, 310, 315, 320, 330, 340, 350,
];
export function orbitAngle(orbit, orbitIndex, skillsPerOrbit) {
	const count = skillsPerOrbit[orbit];
	const table = count === 16 ? ORBIT_ANGLES_16 : count === 40 ? ORBIT_ANGLES_40 : null;
	if (table) {
		return ((table[orbitIndex % table.length] || 0) * Math.PI) / 180;
	}
	return (2 * Math.PI * orbitIndex) / count;
}

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

// 직업 시작 노드 아트 키(PoB PassiveTree.lua 와 동일 순서 = GGG classId)
const CLASS_START_ART = [
	"centerscion",
	"centermarauder",
	"centerranger",
	"centerwitch",
	"centerduelist",
	"centertemplar",
	"centershadow",
];

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
		const angle = orbitAngle(node.orbit, node.orbitIndex, skillsPerOrbit);
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
			// 프록시 그룹(isProxy) 노드는 게임 화면엔 없는 자리표시자다 — 클러스터 서브트리를 붙일 좌표 기준일 뿐.
			// 표시하면 "위치 대행" 같은 노드를 실제로 찍어 포인트를 버리게 된다(실측으로 발각).
			isProxy: group.isProxy ? true : undefined,
			// 클러스터 주얼 소켓(트리 외곽)은 크기별 전용 프레임 아트를 쓴다. 0=Small 1=Medium 2=Large.
			clusterSize: node.expansionJewel ? node.expansionJewel.size : undefined,
			// 클러스터 주얼이 만들어내는 서브트리를 붙이는 데 필요한 참조.
			// GGG 트리엔 서브트리 정의가 없고(정의는 PoB Data/ClusterJewels.lua) 이 참조만 있다:
			//  proxy  = 생성 노드들이 매달릴 프록시 노드 id, parent = 상위 소켓(중첩 클러스터), index = 부모 안에서의 자리
			expansionJewel: node.expansionJewel
				? {
						size: node.expansionJewel.size,
						index: node.expansionJewel.index,
						proxy: node.expansionJewel.proxy !== undefined ? Number(node.expansionJewel.proxy) : null,
						parent: node.expansionJewel.parent !== undefined ? Number(node.expansionJewel.parent) : null,
					}
				: undefined,
			// 마스터리는 노드 자체가 아니라 "효과 하나"를 골라 찍는다. effect id 는 GGG URL 인코딩(마스터리 4바이트)에 그대로 들어감.
			masteryEffects: node.masteryEffects?.length
				? node.masteryEffects.map((eff) => ({ id: eff.effect, stats: eff.stats || [] }))
				: undefined,
			// icon = 원본 경로(스프라이트 coords 키와 정확히 일치) — 프론트가 타입별 시트에서 blit
			icon: node.icon || null,
			// 직업 시작 노드의 중앙 아트(centerwitch 등) — 공식 뷰어는 선택된 직업만 이 아트를 켠다
			startArt: node.classStartIndex != null ? CLASS_START_ART[node.classStartIndex] : undefined,
		});
		positioned.add(Number(id));
	}

	// 클러스터 주얼 노터블/키스톤 — **그룹이 없는** 노드로 트리에 들어 있다(좌표가 없어 위 루프에서 걸러진다).
	// PoB 도 같은 규칙으로 clusterNodeMap 을 만든다(PassiveTree.lua: group 이 없고 type 이 Notable/Keystone).
	// 주얼에 "1 Added Passive Skill is <이름>" 으로 얹히며, **이름이 하나라도 어긋나면 PoB 가 서브트리를 통째로 버린다**.
	const clusterNotables = [];
	for (const [id, node] of Object.entries(tree.nodes)) {
		if (node.group != null || !node.name) continue;
		if (!node.isNotable && !node.isKeystone) continue;
		const ko = koByGraphId ? koByGraphId.get(Number(id)) : null;
		clusterNotables.push({
			name: node.name,
			nameKo: ko?.nameKo || null,
			stats: node.stats || [],
			statsKo: ko?.statsKo?.length ? ko.statsKo : null,
			keystone: node.isKeystone ? true : undefined,
			icon: node.icon || null,
		});
	}
	clusterNotables.sort((a, b) => a.name.localeCompare(b.name));

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
	// 전직 배경 아트는 **전직 시작 노드가 속한 그룹**에 그린다(PoB renderGroup 과 동일).
	// GGG 그룹 데이터엔 전직 이름이 없어 노드에서 역으로 채운다.
	const ascendancyByGroup = new Map();
	for (const node of Object.values(tree.nodes)) {
		if (node.group != null && node.isAscendancyStart && node.ascendancyName) {
			ascendancyByGroup.set(String(node.group), node.ascendancyName);
		}
	}
	const groups = {};
	for (const [gid, g] of Object.entries(tree.groups)) {
		groups[gid] = {
			x: g.x,
			y: g.y,
			orbits: g.orbits || [],
			background: g.background || null,
			// 이 그룹이 전직 시작 그룹이면 그 전직 이름(= 스프라이트 키 Classes<이름>)
			ascendancyStart: ascendancyByGroup.get(gid) || undefined,
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
		clusterNotables,
		// 트리 배경 레이어(클래스 일러스트) — 좌표까지 데이터에 들어 있다. 이미지는 tree-layers.mjs 가 번들에서 뽑는다.
		extraImages: Object.values(tree.extraImages || {})
			.filter((entry) => entry?.image)
			.map((entry) => ({ x: entry.x, y: entry.y, image: entry.image.split("/").pop().toLowerCase() })),
	};
}
