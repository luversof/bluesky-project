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
			// 같은 서술기의 영문 출력(statsKo 와 평행) — 게임 표기 순서로 재배열할 때 다리로 쓴다
			statsEnDesc: describe(statValues, "English"),
			// 키스톤 플레이버 한글 원문 — 트리 export 의 flavourText 와 짝(멀티라인 → 배열)
			flavourKo: passivesKo[i]?.FlavourText ? passivesKo[i].FlavourText.split(/\r?\n/) : null,
		});
	});
	return koByGraphId;
}

/**
 * 한글 스탯 줄을 게임 표기 순서로 재배열 — 스탯 서술기 출력 순서는 서술 파일 순서라서
 * 인게임(=트리 export 의 stats 배열) 순서와 다르다(예: 비전의 의지 — 마나 재생이 먼저 나옴).
 * enLines(서술기 영문, koLines 와 평행)를 다리로 gameLines 순서에 맞춘다. 못 맞춘 줄은 뒤에 보존.
 */
export function alignKoToGameOrder(gameLines, koLines, enLines) {
	if (!koLines?.length || !enLines?.length || koLines.length !== enLines.length) return koLines;
	const norm = (s) => s.replace(/\s+/g, " ").trim().toLowerCase();
	const pool = enLines.map((line, i) => ({ key: norm(line), i, used: false }));
	const out = [];
	const unmatched = []; // 게임 줄 중 영문 서술과 텍스트가 안 맞은 것 → out 의 자리만 잡아 둔다
	for (const line of gameLines || []) {
		const hit = pool.find((p) => !p.used && p.key === norm(line));
		if (hit) {
			hit.used = true;
			out.push(koLines[hit.i]);
		} else {
			unmatched.push({ slot: out.length, line });
			out.push(null);
		}
	}
	// 안 쓰인 한글 줄을 게임 순서대로 빈 자리에 채운다(수치 표기 차이로 매칭만 실패한 경우).
	const leftover = pool.filter((p) => !p.used).map((p) => koLines[p.i]);
	unmatched.forEach((u, i) => {
		// 한글 줄이 모자라면 GGG 트리 export 에만 있고 게임 테이블엔 없는 줄(트릭스터의 "주변 적 행동 속도" 등)이다.
		// 한글 소스가 아예 없으므로 **영문 원문이라도 남긴다** — 버리면 한국어 툴팁에서 그 효과가 통째로 사라진다.
		out[u.slot] = i < leftover.length ? leftover[i] : u.line;
	});
	for (let i = unmatched.length; i < leftover.length; i++) out.push(leftover[i]);
	return out;
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
			statsKo: ko?.statsKo?.length ? alignKoToGameOrder(node.stats, ko.statsKo, ko.statsEnDesc) : null,
			// 공홈 툴팁 파리티: 리마인더(회색 괄호 부연)와 키스톤 플레이버(이탤릭 문구).
			// 원본이 영어 전용이라 일단 영문 그대로 — 한글 조인은 게임 테이블 확보 시.
			reminder: node.reminderText?.length ? node.reminderText : undefined,
			flavour: node.flavourText?.length ? node.flavourText : undefined,
			flavourKo: node.flavourText?.length && ko?.flavourKo?.length ? ko.flavourKo : undefined,
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
				? node.masteryEffects.map((eff) => ({
						id: eff.effect,
						stats: eff.stats || [],
						// 효과별 리마인더(525/1825개) — 픽커/툴팁에서 회색 부연으로 표시
						reminder: eff.reminderText?.length ? eff.reminderText : undefined,
					}))
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
			statsKo: ko?.statsKo?.length ? alignKoToGameOrder(node.stats, ko.statsKo, ko.statsEnDesc) : null,
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

/**
 * 리마인더 한글 조인 — 트리 export 의 reminderText(완성 영문 문장)를 게임 테이블
 * ReminderText(EN/KO 같은 행 순서)로 페어링해 노드에 reminderKo 를 붙인다.
 * 트리 문장과 테이블 문장이 괄호/공백만 다른 경우가 있어 정규화 후 매칭(실측 803/811).
 * 못 찾은 줄은 영문 폴백으로 채워 배열 길이를 reminder 와 맞춘다.
 */
export function joinReminderKo(result, reminderEn, reminderKo) {
	const norm = (s) => s.replace(/^\(|\)$/g, "").replace(/\s+/g, " ").trim().toLowerCase();
	const koByEn = new Map();
	reminderEn.forEach((row, i) => {
		if (row.Text && reminderKo[i]?.Text) koByEn.set(norm(row.Text), reminderKo[i].Text);
	});
	let hit = 0;
	let total = 0;
	const attach = (holder) => {
		if (!holder.reminder?.length) return;
		const lines = holder.reminder.map((line) => koByEn.get(norm(line)) || null);
		total += lines.length;
		hit += lines.filter(Boolean).length;
		if (lines.some(Boolean)) holder.reminderKo = lines.map((ko, i) => ko || holder.reminder[i]);
	};
	for (const node of result.nodes) {
		attach(node);
		for (const eff of node.masteryEffects || []) attach(eff);
	}
	return { hit, total };
}
