// PoE2 아키타입 공통 특성(10-14 사용자 요청 — 시뮬레이터가 "실빌드 한 명"이 아니라 그 직업/스킬 실빌드들의 특성을 참고하도록).
// fetch-ninja-start2 가 재고 남긴 아키타입별 실빌드 여러 명(ninja-archetype-members2.json)의 PoB 코드를 풀어 빈도를 낸다:
//   · 패시브 노드 — 본 트리 · 무기 세트 1 · 세트 2 · 전직(PoE2 는 세트 전용 포인트가 따로 있어 나눠 센다, 3번 요청)
//   · 메인 스킬 그룹의 보조 젬 · 다른 스킬 그룹의 첫 젬(오라 · 버프 · 보조 스킬)
//   · 장비 칸마다 고유 이름 · 레어/마법 베이스 · 희귀도
//   · 포인트 중앙값(본 트리 · 세트 1 · 세트 2 · 전직)
// 산출물: ~/.poe-gamedata/poe2/ninja/ninja-archetype-consensus2.json  — 사용: node archetype-consensus2.mjs (fetch-ninja-start2 끝에서도 부른다)
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { decodePob } from "../poe-extract/ninja-normalize.mjs";

const DIR = path.join(os.homedir(), ".poe-gamedata", "poe2", "ninja");
const TREE = path.join(os.homedir(), ".poe-gamedata", "poe2", "passive-tree.json");
export const MEMBERS_PATH = path.join(DIR, "ninja-archetype-members2.json");
export const CONSENSUS_PATH = path.join(DIR, "ninja-archetype-consensus2.json");

const median = (a) => { const s = a.slice().sort((x, y) => x - y); return s.length ? s[Math.floor((s.length - 1) / 2)] : 0; };
const bump = (m, k) => { if (k != null && k !== "") m[k] = (m[k] || 0) + 1; };
const unescape = (s) => String(s).replace(/&apos;/g, "'").replace(/&quot;/g, '"').replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">");
const ids = (s) => String(s || "").split(",").map((x) => Number(x.trim())).filter((x) => Number.isFinite(x) && x > 0);

/** PoB XML 한 벌 → 이 빌드의 선택들(트리 · 젬 · 장비). */
export function parseBuild(xml, ascendancyNodes, baseNames = []) {
	const build = xml.match(/<Build\b[^>]*>/)?.[0] || "";
	const attr = (tag, name) => unescape((tag.match(new RegExp(`\\b${name}="([^"]*)"`)) || [])[1] || "");
	const spec = xml.match(/<Spec\b[^>]*>/)?.[0] || "";
	const all = ids(attr(spec, "nodes"));
	const set1 = new Set(ids((xml.match(/<WeaponSet1\b[^>]*\bnodes="([^"]*)"/) || [])[1]));
	const set2 = new Set(ids((xml.match(/<WeaponSet2\b[^>]*\bnodes="([^"]*)"/) || [])[1]));
	const asc = [], main = [];
	for (const id of all) {
		if (set1.has(id) || set2.has(id)) continue;
		(ascendancyNodes.has(id) ? asc : main).push(id);
	}
	// 스킬 그룹 — 메인 그룹(mainSocketGroup, 1부터)의 보조 젬, 나머지 그룹은 첫 액티브 젬
	const skills = [...xml.matchAll(/<Skill\b[^>]*>([\s\S]*?)<\/Skill>/g)].map((m) => [...m[1].matchAll(/<Gem\b[^>]*>/g)].map((g) => ({ name: attr(g[0], "nameSpec"), id: attr(g[0], "gemId"), enabled: attr(g[0], "enabled") !== "false" })));
	const mainIdx = Number(attr(build, "mainSocketGroup")) - 1;
	const supports = [], others = [];
	skills.forEach((gems, i) => {
		const live = gems.filter((g) => g.enabled && g.name);
		if (i === mainIdx) live.filter((g) => /Support/i.test(g.id)).forEach((g) => supports.push(g.name));
		else { const act = live.find((g) => !/Support/i.test(g.id)); if (act) others.push(act.name); }
	});
	// 장비 — 켜진 아이템 세트의 칸 → 아이템 머리(희귀도 · 이름 · 베이스)
	const itemsTag = xml.match(/<Items\b[^>]*>/)?.[0] || "";
	const activeSet = attr(itemsTag, "activeItemSet") || "1";
	const setBlock = [...xml.matchAll(/<ItemSet\b([^>]*)>([\s\S]*?)<\/ItemSet>/g)].find((m) => (attr(`<x${m[1]}>`, "id") || "1") === activeSet)?.[2] || "";
	const itemText = new Map([...xml.matchAll(/<Item\b[^>]*\bid="(\d+)"[^>]*>([\s\S]*?)<\/Item>/g)].map((m) => [m[1], m[2]]));
	const slots = {};
	for (const m of setBlock.matchAll(/<Slot\b[^>]*>/g)) {
		const name = attr(m[0], "name"), itemId = attr(m[0], "itemId");
		if (!name || !itemId || itemId === "0" || !itemText.has(itemId)) continue;
		const lines = itemText.get(itemId).split(/\r?\n/).map((l) => l.trim()).filter((l) => l && !l.startsWith("<"));
		const ri = lines.findIndex((l) => /^Rarity:/i.test(l));
		if (ri < 0) continue;
		const rarity = lines[ri].replace(/^Rarity:\s*/i, "").toUpperCase();
		const two = rarity === "UNIQUE" || rarity === "RARE";
		let base = unescape(two ? lines[ri + 2] || "" : lines[ri + 1] || "");
		// 마법 아이템 한 줄 이름엔 접두 · 접미가 붙는다("Clinician's Dousing Charm of the Distiller") — 베이스 목록에서 가장 긴 일치
		if (rarity === "MAGIC" && baseNames.length) base = baseNames.find((n) => base.includes(n)) || base;
		slots[name] = { rarity, name: unescape(lines[ri + 1] || ""), base };
	}
	// 능력치 노드의 고른 능력치(힘 1 · 민첩 2 · 지능 3) — withTree 의 attrs 와 같은 번호
	const ov = xml.match(/<AttributeOverride\b[^>]*>/)?.[0] || "";
	const attrs = {};
	["strNodes", "dexNodes", "intNodes"].forEach((k, i) => ids(attr(ov, k)).forEach((id) => (attrs[id] = i + 1)));
	// 켜진 무기 세트(저장 당시) — 세트 성격(setRoles)의 "주로 쓰는 세트"
	const activeWeaponSet = /\buseSecondWeaponSet="true"/.test(xml.match(/<ItemSet\b[^>]*>/)?.[0] || "") ? 2 : 1;
	return { className: attr(build, "className"), ascendancy: attr(build, "ascendClassName"), level: Number(attr(build, "level")) || 0, main, asc, set1: [...set1], set2: [...set2], supports, others, slots, attrs, activeWeaponSet };
}

// 세트 전용 노드 문구 → 특성(10-15 사용자 요청 "세트 1 · 2 가 어떤 특성에 치중하는지 파악해 시뮬레이션하고 보여 달라" — 버프/공격, 광역/보스, 지속 피해/치명타).
// 노드 하나가 여러 특성에 걸릴 수 있다. 특출 노드는 3배(작은 노드 여럿보다 방향을 정한다)
export const SET_TRAITS = [
	["damage", /(?:increased|more) [A-Za-z ]*Damage(?! taken)|Damage Bonus|Penetrat/i],
	["crit", /Critical/i],
	["dot", /Damage over Time|Poison|Bleed|Ignite|Magnitude of Damaging Ailments|Wither/i],
	["area", /Area of Effect|Projectile|Chain|Fork|Pierce|Additional Arrow/i],
	["boss", /Rare or Unique|Unique Enem|Armour Break|Break Armour|Fully Broken|Exposure/i],
	["speed", /Attack Speed|Cast Speed|Skill Speed/i],
	["accuracy", /Accuracy/i],
	["buff", /Spirit|Aura|Buff|Herald|Presence|Offering|Banner|Reservation|Curse|\bMark/i],
	["minion", /Minion|Companion|Allies/i],
	["defense", /Armour|Evasion|Energy Shield|maximum Life|Resist|Block|Damage taken|Regenerat|Recovery|Flask|Charm|Stun Threshold|Ailment Threshold/i],
];
const ATTACK_TRAITS = new Set(["damage", "crit", "dot", "area", "boss"]);
const SUPPORT_TRAITS = new Set(["buff", "minion", "speed", "accuracy"]);

/**
 * 세트 성격 — 멤버마다 세트 1 · 2 전용 노드 문구를 특성으로 묶어 비율을 내고, 두 세트의 차이(이 세트가 상대보다 더 치중한 특성)로 역할을 정한다.
 * kind: attack(공격 — 다듬기 목표 DPS) · defense(방어 — EHP) · support(버프 · 소환 · 속도 · 명중 — 균형). 세트를 나눠 쓰는 멤버가 절반 미만이면 null.
 * 무기는 그 세트 무기 칸의 아이템 종류 다수결("Bow+Quiver", 비면 "none").
 */
export function setRoles(builds, pobTree, classOf = new Map(), classKo = new Map()) {
	if (!pobTree?.nodes || !builds.length) return null;
	const using = builds.filter((b) => b.set1.length + b.set2.length > 0);
	if (using.length * 2 < builds.length) return null;
	const sum = [{}, {}];
	for (const b of using) {
		[b.set1, b.set2].forEach((nodes, i) => {
			for (const id of nodes) {
				const n = pobTree.nodes[id];
				if (!n?.stats?.length) continue;
				const txt = n.stats.join(" | ");
				for (const [k, re] of SET_TRAITS) if (re.test(txt)) sum[i][k] = (sum[i][k] || 0) + (n.isNotable ? 3 : 1);
			}
		});
	}
	const share = sum.map((o) => { const t = Object.values(o).reduce((a, c) => a + c, 0) || 1; return Object.fromEntries(Object.entries(o).map(([k, v]) => [k, Math.round((v / t) * 1000) / 1000])); });
	const weaponOf = (b, a, c) => [b.slots[a], b.slots[c]].filter(Boolean).map((it) => classOf.get(it.base) || it.base).join("+") || "none";
	const sets = [0, 1].map((i) => {
		const other = share[1 - i];
		const diff = Object.keys({ ...share[i], ...other }).map((k) => [k, (share[i][k] || 0) - (other[k] || 0)]).filter(([, d]) => d >= 0.04).sort((a, b) => b[1] - a[1]);
		const focus = diff.slice(0, 3).map(([k]) => k);
		// 역할 = 더 치중한 특성 차이를 묶음(공격 · 지원 · 방어)별로 더해 큰 쪽(첫 특성 하나로 정하면 "속도 + 공격력 + 치명타" 가 지원이 됐다).
		// 차이가 없으면(두 세트가 닮음) 이 세트 자체 비율로
		const by = diff.length ? diff : Object.entries(share[i]);
		const score = { attack: 0, support: 0, defense: 0 };
		by.forEach(([k, d]) => (score[ATTACK_TRAITS.has(k) ? "attack" : SUPPORT_TRAITS.has(k) ? "support" : "defense"] += d));
		const kind = by.length ? Object.entries(score).sort((a, b) => b[1] - a[1])[0][0] : null;
		const w = {};
		for (const b of builds) bump(w, i === 0 ? weaponOf(b, "Weapon 1", "Weapon 2") : weaponOf(b, "Weapon 1 Swap", "Weapon 2 Swap"));
		const [weapon, weaponCount] = Object.entries(w).sort((a, b) => b[1] - a[1])[0] || ["none", 0];
		const weaponKo = weapon === "none" ? "" : weapon.split("+").map((c) => classKo.get(c) || c).join(" + ");
		return { set: i + 1, kind, focus, traits: share[i], weapon, weaponKo, weaponCount, active: builds.filter((b) => b.activeWeaponSet === i + 1).length };
	});
	return { n: builds.length, used: using.length, sets };
}

/** 한 아키타입의 멤버들 → 빈도. */
export function aggregate(builds) {
	const out = { n: builds.length, className: "", nodes: {}, set1: {}, set2: {}, ascNodes: {}, attrs: {}, supports: {}, skills: {}, slots: {}, points: {} };
	const classes = {};
	for (const b of builds) {
		bump(classes, b.className);
		for (const [id, v] of Object.entries(b.attrs || {})) bump((out.attrs[id] ||= {}), v);
		b.main.forEach((id) => bump(out.nodes, id));
		b.set1.forEach((id) => bump(out.set1, id));
		b.set2.forEach((id) => bump(out.set2, id));
		b.asc.forEach((id) => bump(out.ascNodes, id));
		new Set(b.supports).forEach((g) => bump(out.supports, g));
		new Set(b.others).forEach((g) => bump(out.skills, g));
		for (const [slot, it] of Object.entries(b.slots)) {
			const s = (out.slots[slot] ||= { rarity: {}, unique: {}, base: {} });
			bump(s.rarity, it.rarity);
			if (it.rarity === "UNIQUE") bump(s.unique, it.name);
			else bump(s.base, it.base);
		}
	}
	out.className = Object.entries(classes).sort((a, b) => b[1] - a[1])[0]?.[0] || "";
	out.points = {
		main: median(builds.map((b) => b.main.length)),
		set1: median(builds.map((b) => b.set1.length)),
		set2: median(builds.map((b) => b.set2.length)),
		asc: median(builds.map((b) => b.asc.length)),
		level: median(builds.map((b) => b.level)),
	};
	return out;
}

/** PoB 가 쓰는 트리 데이터(가장 새 버전 폴더의 tree.json) — 간선은 이쪽이 정답(10-14: 우리 passive-tree.json 의 out 은 간선 일부가 빠져
 *  드루이드 오라클 실빌드 하나를 넣어도 트리의 절반이 시작점에서 안 닿았다. 예: 폭풍 충전 38535 ↔ 42205). 없으면 null → passive-tree.json 의 out */
export function loadPobTree() {
	const dir = path.join(os.homedir(), ".poe-gamedata", "poe2", "work", "pob2-src", "src", "TreeData");
	try {
		const v = fs.readdirSync(dir).filter((d) => /^\d+_\d+$/.test(d)).sort((a, b) => { const [a1, a2] = a.split("_").map(Number), [b1, b2] = b.split("_").map(Number); return a1 - b1 || a2 - b2; }).pop();
		return v ? JSON.parse(fs.readFileSync(path.join(dir, v, "tree.json"), "utf8")) : null;
	} catch { return null; }
}

/** 트리 그래프(무향) — PoB tree.json 의 connections(없으면 passive-tree.json 의 out). 특성 숙련(mastery)은 길이 아니라 뺀다. */
export function treeGraph(tree, pobTree = null) {
	const adj = new Map();
	const link = (a, b) => { (adj.get(a) || adj.set(a, new Set()).get(a)).add(b); };
	for (const [id, n] of Object.entries(pobTree ? pobTree.nodes : tree.nodes)) {
		if (tree.nodes[id]?.kind === "mastery" || !tree.nodes[id]) continue;
		for (const o of (pobTree ? n.connections : n.out) || []) {
			const to = tree.nodes[o.id];
			if (!to || to.kind === "mastery") continue;
			link(Number(id), Number(o.id));
			link(Number(o.id), Number(id));
		}
	}
	return adj;
}

/**
 * 빈도로 키우는 트리 — 이미 찍힌 노드(have)에서 출발해 빈도(freq: id → 0~1)가 높은 노드까지의 길을 붙인다. 한 번에 "길 위 빈도 합 ÷ 길이" 가 가장 큰 길 하나.
 * 길의 비용은 (1.0001 − 빈도)라 많이 찍는 노드를 지나는 길을 고른다. 포인트(budget)를 다 쓰거나 빈도 minFreq 이상 노드가 닿지 않으면 멈춘다.
 * allow(id) = 지날 수 있는 노드(본 트리면 전직 노드 제외, 전직이면 그 전직 노드만).
 * free = 이어지지 않아도 찍을 수 있는 노드(인게임 규칙 — 오라클 "얽힌 현실": 찍은 핵심 근처 노드는 트리와 안 이어져도 된다. 멤버 트리에서 시작점과
 *   안 닿는 노드를 모은 것). 길의 첫 노드로 쓸 수 있다.
 */
export function growTree(adj, have, freq, budget, allow, minFreq = 0.25, free = new Set()) {
	const taken = new Set(have);
	const added = [];
	while (added.length < budget) {
		const dist = new Map(), prev = new Map(), gain = new Map(), len = new Map();
		const heap = [];
		for (const s of taken) { dist.set(s, 0); gain.set(s, 0); len.set(s, 0); heap.push([0, s]); }
		for (const s of free) {
			if (taken.has(s) || !allow(s)) continue;
			const c = 1.0001 - (freq[s] || 0);
			if (c < (dist.has(s) ? dist.get(s) : Infinity)) { dist.set(s, c); prev.delete(s); gain.set(s, freq[s] || 0); len.set(s, 1); heap.push([c, s]); }
		}
		let best = null, bestRatio = 0;
		while (heap.length) {
			let bi = 0;
			for (let i = 1; i < heap.length; i++) if (heap[i][0] < heap[bi][0]) bi = i;
			const [d, u] = heap[bi];
			heap[bi] = heap[heap.length - 1];
			heap.pop();
			if (d > dist.get(u)) continue;
			if (!taken.has(u) && (freq[u] || 0) >= minFreq && len.get(u) <= budget - added.length) {
				const ratio = gain.get(u) / len.get(u);
				if (ratio > bestRatio) { bestRatio = ratio; best = u; }
			}
			for (const v of adj.get(u) || []) {
				if (taken.has(v) || !allow(v)) continue;
				const nd = d + (1.0001 - (freq[v] || 0));
				if (nd < (dist.has(v) ? dist.get(v) : Infinity)) {
					dist.set(v, nd); prev.set(v, u); gain.set(v, gain.get(u) + (freq[v] || 0)); len.set(v, len.get(u) + 1);
					heap.push([nd, v]);
				}
			}
		}
		if (best == null) break;
		const path = [];
		for (let v = best; v != null && !taken.has(v); v = prev.get(v)) path.push(v);
		path.reverse().forEach((v) => { taken.add(v); added.push(v); });
	}
	return added;
}

/** 아키타입 빈도 → 공통 트리(본 · 전직 · 세트 1 · 세트 2) + 멤버와 겹치는 정도. sim 이 withTree 로 덮어 잰다. */
export function consensusTree(agg, builds, tree, adj) {
	const cls = (tree.classes || []).find((c) => c.name === agg.className);
	const start = cls?.startNodeId;
	if (!start) return null;
	const ascName = Object.entries(builds.reduce((m, b) => (bump(m, b.ascendancy), m), {})).sort((a, b) => b[1] - a[1])[0]?.[0] || "";
	const ascStart = cls.ascendancies?.find((a) => a.id === ascName || a.name === ascName)?.startNodeId;
	const f = (counts) => Object.fromEntries(Object.entries(counts).map(([id, c]) => [Number(id), c / agg.n]));
	const isAsc = (id) => !!tree.nodes[id]?.ascendancy;
	// 이어지지 않은 채 찍힌 노드(멤버 둘 이상) — growTree 의 free
	const loose = {};
	for (const b of builds) {
		const mine = new Set([...b.main, ...b.set1, ...b.set2]), seen = new Set([start]), q = [start];
		while (q.length) { const u = q.shift(); for (const v of adj.get(u) || []) if (mine.has(v) && !seen.has(v)) { seen.add(v); q.push(v); } }
		mine.forEach((id) => !seen.has(id) && bump(loose, id));
	}
	const free = new Set(Object.entries(loose).filter(([, c]) => c >= Math.min(2, builds.length)).map(([id]) => Number(id)));
	const main = growTree(adj, [start], f(agg.nodes), agg.points.main, (id) => !isAsc(id) && tree.nodes[id]?.kind !== "classStart", 0.25, free);
	const ascNode = (id) => tree.nodes[id]?.ascendancy === (tree.nodes[ascStart]?.ascendancy ?? "_");
	const asc = ascStart ? growTree(adj, [ascStart], f(agg.ascNodes), agg.points.asc, ascNode) : [];
	const allow = (id) => !isAsc(id) && tree.nodes[id]?.kind !== "classStart";
	const set1 = agg.points.set1 ? growTree(adj, [start, ...main], f(agg.set1), agg.points.set1, allow, 0.25, free) : [];
	const set2 = agg.points.set2 ? growTree(adj, [start, ...main], f(agg.set2), agg.points.set2, allow, 0.25, free) : [];
	const all = [...main, ...(ascStart ? [ascStart] : []), ...asc, ...set1, ...set2];
	const attrs = {};
	for (const id of all) {
		const c = agg.attrs[id];
		if (c) attrs[id] = Number(Object.entries(c).sort((a, b) => b[1] - a[1])[0][0]);
	}
	const sets = {};
	set1.forEach((id) => (sets[id] = 1));
	set2.forEach((id) => (sets[id] = sets[id] ? sets[id] : 2)); // 두 세트가 같은 노드를 원하면 세트 1(드묾 — 노드 하나는 한 세트에만)
	// 겹침 — 멤버마다 본 트리 자카드 유사도의 중앙값(공통 트리가 실빌드와 얼마나 닮았나)
	const mainSet = new Set(main);
	const jac = builds.map((b) => { const bs = new Set(b.main); let inter = 0; bs.forEach((x) => mainSet.has(x) && inter++); return inter / (bs.size + mainSet.size - inter || 1); });
	return { className: agg.className, ascendancy: ascName, nodes: all, attrs, sets, loose: free.size, overlap: Math.round(median(jac) * 1000) / 1000 };
}

/** 멤버 파일 → 공통 특성 파일. 반환: 아키타입 수 */
export function writeConsensus() {
	if (!fs.existsSync(MEMBERS_PATH)) return 0;
	const members = JSON.parse(fs.readFileSync(MEMBERS_PATH, "utf8"));
	const tree = JSON.parse(fs.readFileSync(TREE, "utf8"));
	const ascendancyNodes = new Set(Object.entries(tree.nodes).filter(([, n]) => n.ascendancy).map(([id]) => Number(id)));
	let baseNames = [];
	const classOf = new Map(), classKo = new Map();
	try {
		const bi = JSON.parse(fs.readFileSync(path.join(os.homedir(), ".poe-gamedata", "poe2", "base-items.json"), "utf8"));
		const list = bi.items || Object.values(bi);
		baseNames = [...new Set(list.map((b) => b.name).filter(Boolean))].sort((a, b) => b.length - a.length);
		list.forEach((b) => b.name && b.itemClass && classOf.set(b.name, b.itemClass));
		list.forEach((b) => b.itemClass && b.itemClassKo && classKo.set(b.itemClass, b.itemClassKo));
	} catch { /* 베이스 목록이 없으면 이름 그대로 */ }
	const pobTree = loadPobTree();
	const adj = treeGraph(tree, pobTree);
	const archetypes = {};
	for (const [key, list] of Object.entries(members.archetypes || {})) {
		const builds = [];
		for (const m of list) {
			try { builds.push(parseBuild(decodePob(m.code), ascendancyNodes, baseNames)); } catch { /* 풀 수 없는 코드는 건너뜀 */ }
		}
		if (!builds.length) continue;
		const agg = aggregate(builds);
		try { agg.tree = consensusTree(agg, builds, tree, adj); } catch (e) { agg.treeError = String(e?.message || e); }
		try { agg.setRoles = setRoles(builds, pobTree, classOf, classKo); } catch (e) { agg.setRolesError = String(e?.message || e); }
		archetypes[key] = agg;
	}
	fs.writeFileSync(CONSENSUS_PATH, JSON.stringify({ league: members.league, snapshot: members.snapshot, builtAt: new Date().toISOString(), archetypes }));
	return Object.keys(archetypes).length;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
	const n = writeConsensus();
	console.log(`[consensus2] 아키타입 ${n}개 → ${CONSENSUS_PATH}`);
}
