// PoE2 아틀라스 패시브 트리 → ~/.poe-gamedata/poe2/atlas-tree.json (게이트 /poe2/atlas 가 tree2.js 아틀라스 모드로 그린다)
// 원천: 게임 번들 metadata/atlasskillgraphs/atlasskillgraph.psg(배치) + PassiveSkills(이름·스탯) — PoB-PoE2 는 아틀라스를 안 싣고 GGG 공식 내보내기도 없다.
// PSG 형식(PoB-PoE2 src/Export/Scripts/passivetree.lua 와 같음): u16 버전 · 11바이트 · i32 뿌리 수 · u64 뿌리[] · i32 그룹 수 ·
//   그룹마다 f32 x · f32 y · i32 flags · i32 unk · u8 · i32 노드 수 · 노드마다 i32 그래프 id · i32 궤도 · i32 궤도 위치 · i32 연결 수 · 연결마다 i32 id · i32 궤도.
//   아틀라스 파일은 버전 259 지만 같은 형식으로 끝까지 정확히 읽힌다(소비 바이트 = 파일 길이로 확인).
// 좌표는 본 트리와 같은 상수(PoB tree.json constants)·같은 식 — **본 트리 PSG 로 같은 계산을 해 PoB 좌표와 대조**해 식을 검증한다(자가검사).
// 하위 트리: 뿌리 7개(일반·의식·균열·환영·심연·침입·탐험) — 뿌리에서 다른 뿌리를 넘지 않는 BFS 로 노드마다 소속(tree)을 붙인다.
import fs from "node:fs";
import path from "node:path";
import { createDescriber, pobFile, stripMarkup } from "./common2.mjs";
import { DATA_DIR, loadConfig, loadTable, openLoader, writeJson } from "./paths.mjs";

const loader = await openLoader();
const readPsg = async (p) => {
	const b = Buffer.from(await loader.get(p));
	let o = 0;
	const u16 = () => ((o += 2), b.readUInt16LE(o - 2));
	const i32 = () => ((o += 4), b.readInt32LE(o - 4));
	const f32 = () => ((o += 4), b.readFloatLE(o - 4));
	const version = u16();
	o += 11;
	const roots = [];
	for (let i = i32(); i > 0; i--) {
		roots.push(Number(b.readBigUInt64LE(o)));
		o += 8;
	}
	const groups = [];
	for (let g = i32(); g > 0; g--) {
		const grp = { x: f32(), y: f32(), flags: i32(), unk1: i32(), unk2: b[o++], nodes: [] };
		for (let n = i32(); n > 0; n--) {
			const node = { id: i32(), orbit: i32(), orbitIndex: i32(), out: [] };
			for (let c = i32(); c > 0; c--) node.out.push({ id: i32(), orbit: i32() });
			grp.nodes.push(node);
		}
		groups.push(grp);
	}
	if (o !== b.length) throw new Error(`${p}: ${o}/${b.length} 바이트만 읽음 — 형식이 바뀌었다`);
	return { version, roots, groups };
};

// ── 좌표 상수(본 트리와 공용) ──
const pobTree = JSON.parse(await pobFile("src/TreeData/0_5/tree.json"));
const { orbitRadii, skillsPerOrbit, orbitAnglesByOrbit } = pobTree.constants;
const angle = (orbit, index) => {
	const table = orbitAnglesByOrbit?.[orbit];
	if (table && table[index] != null) return table[index];
	return (2 * Math.PI * index) / skillsPerOrbit[orbit];
};
const place = (g, n) => {
	const a = angle(n.orbit, n.orbitIndex);
	const r = orbitRadii[n.orbit] ?? 0;
	return { x: g.x + r * Math.sin(a), y: g.y - r * Math.cos(a) };
};

// ── 자가검사: 본 트리 PSG 로 계산한 좌표 = PoB tree.json 좌표? (전직 노드는 PoB 가 옮겨 놓으므로 제외) ──
{
	const main = await readPsg("metadata/passiveskillgraph.psg");
	// 대조 대상 = parse-tree2 가 PoB 그룹 좌표로 계산한 passive-tree.json(PoB tree.json 노드엔 x/y 가 없다). 전직은 PoB 가 둘레로 옮겨 제외.
	const ours = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "passive-tree.json"), "utf8")).nodes;
	let compared = 0, worst = 0;
	for (const g of main.groups) {
		for (const n of g.nodes) {
			const p = ours[n.id];
			if (!p || p.ascendancy || p.x == null) continue;
			const { x, y } = place(g, n);
			worst = Math.max(worst, Math.hypot(x - p.x, y - p.y));
			compared++;
		}
	}
	console.log(`[atlas2] 자가검사: 본 트리 ${compared}노드 좌표 최대 오차 ${worst.toFixed(2)}`);
	if (compared < 3000 || worst > 2) throw new Error("좌표 식이 PoB 와 다르다 — 상수/각도표 확인");
}

// ── 아틀라스 ──
const psg = await readPsg("metadata/atlasskillgraphs/atlasskillgraph.psg");
const ps = loadTable("English", "PassiveSkills");
const psKo = loadTable("Korean", "PassiveSkills");
const stats = loadTable("English", "Stats");
const byGraph = new Map(ps.map((p, i) => [p.PassiveSkillGraphId, i]));
const describe = createDescriber("atlas");

// 하위 트리 이름 — 게임 문구 "균열 아틀라스 스킬 포인트 1포인트를 획득했습니다" 에서 앞 낱말을 뗀다(일반 트리는 "아틀라스")
const cs = loadTable("English", "ClientStrings");
const csKo = loadTable("Korean", "ClientStrings");
const csIdx = new Map(cs.map((c, i) => [c.Id, i]));
function mechanicName(key) {
	const i = csIdx.get(`ReceivedAtlasPassivePoint${key}1`);
	if (i == null) return null;
	const en = (cs[i].Text.match(/receive(?:d)? an? (.+?) Atlas Skill Point/i) || [])[1];
	const ko = (stripMarkup(csKo[i].Text).match(/^(.+?) 아틀라스 스킬 포인트/) || [])[1];
	return en && ko ? { en, ko } : null;
}
const ROOT_KEY = (id) => (id.match(/^Atlas(\w+?)Start_?$/) || [])[1] || id;

const nodes = {};
const groups = {};
let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
psg.groups.forEach((g, gi) => {
	const group = gi + 1;
	groups[group] = { x: Math.round(g.x), y: Math.round(g.y) };
	for (const n of g.nodes) {
		const idx = byGraph.get(n.id);
		const row = idx != null ? ps[idx] : null;
		const rowKo = idx != null ? psKo[idx] : null;
		const values = new Map();
		(row?.Stats || []).forEach((s, k) => values.set(stats[s].Id, row[`Stat${k + 1}Value`] ?? 0));
		const { x, y } = place(g, n);
		minX = Math.min(minX, x);
		minY = Math.min(minY, y);
		maxX = Math.max(maxX, x);
		maxY = Math.max(maxY, y);
		const isRoot = psg.roots.includes(n.id);
		nodes[n.id] = {
			name: stripMarkup(row?.Name || ""),
			nameKo: stripMarkup(rowKo?.Name || "") || undefined,
			kind: isRoot ? "classStart" : row?.IsKeystone ? "keystone" : row?.IsNotable ? "notable" : row?.IsJewelSocket ? "jewel" : "normal",
			stats: values.size ? describe(values, "English") : [],
			statsKo: values.size ? describe(values, "Korean") : [],
			x: Math.round(x),
			y: Math.round(y),
			group,
			orbit: n.orbit,
			orbitIndex: n.orbitIndex,
			out: n.out.map((c) => ({ id: c.id, orbit: c.orbit })),
			ascendancy: null,
			classStart: null,
			flavour: stripMarkup(row?.FlavourText || "") || undefined,
			flavourKo: stripMarkup(rowKo?.FlavourText || "") || undefined,
			icon: row?.Icon_DDSFile || undefined,
			pointsGranted: row?.SkillPointsGranted || undefined,
			rowId: row?.Id,
		};
	}
});

// 인접(양방향) → 뿌리별 BFS(다른 뿌리는 못 넘는다)
const adj = new Map(Object.keys(nodes).map((id) => [Number(id), []]));
for (const [id, n] of Object.entries(nodes)) {
	for (const c of n.out) {
		if (!adj.has(c.id)) continue;
		adj.get(Number(id)).push(c.id);
		adj.get(c.id).push(Number(id));
	}
}
const trees = [];
for (const rootId of psg.roots) {
	const root = nodes[rootId];
	const key = ROOT_KEY(root.rowId || String(rootId));
	const label = key === "Generic" ? { en: "Atlas", ko: "아틀라스" } : mechanicName(key) || { en: key, ko: key };
	// 뿌리 노드는 게임 이름이 비어 있다 — 하위 트리 이름을 붙여 툴팁·검색에 보이게
	if (!root.name) root.name = label.en;
	if (!root.nameKo) root.nameKo = label.ko;
	const seen = new Set([rootId]);
	const queue = [rootId];
	while (queue.length) {
		const cur = queue.shift();
		nodes[cur].tree = key;
		for (const nb of adj.get(cur)) {
			if (seen.has(nb) || psg.roots.includes(nb)) continue;
			seen.add(nb);
			queue.push(nb);
		}
	}
	trees.push({ key, name: label.en, nameKo: label.ko, rootId, nodeCount: seen.size - 1 });
}
// 연결이 하나도 없는 특화(Mastery) 장식 노드 — 인게임에선 그룹 배경 문양이라 할당·표시 대상이 아니다(본 트리처럼 mastery 로 숨김)
for (const [id, n] of Object.entries(nodes)) {
	if (!n.tree && adj.get(Number(id)).length === 0 && /Mastery/i.test(n.rowId || "")) n.kind = "mastery";
	delete n.rowId;
}

const orphan = Object.values(nodes).filter((n) => !n.tree && n.kind !== "mastery").length;
const noName = Object.values(nodes).filter((n) => !n.name && n.kind !== "mastery").length;
const noKo = Object.values(nodes).filter((n) => n.stats.length && n.statsKo.join() === n.stats.join()).length;
const pad = 400;
writeJson("atlas-tree.json", {
	patch: loadConfig().patch,
	mode: "atlas",
	psgVersion: psg.version,
	bounds: { minX: minX - pad, minY: minY - pad, maxX: maxX + pad, maxY: maxY + pad },
	orbitRadii,
	classes: [],
	trees,
	groups,
	nodes,
});
console.log(
	`[atlas2] atlas-tree.json: 노드 ${Object.keys(nodes).length} · 그룹 ${psg.groups.length} · 하위 트리 ${trees.map((t) => `${t.nameKo} ${t.nodeCount}`).join(", ")}` +
		` · 소속 없음 ${orphan} · 이름 없음 ${noName} · 한국어 미번역 스탯 노드 ${noKo}`,
);
