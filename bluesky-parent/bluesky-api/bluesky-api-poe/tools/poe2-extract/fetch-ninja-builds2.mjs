// poe.ninja PoE2 실빌드 → 아키타입(전직|메인 스킬) 집계 — PoE1 시뮬레이터가 쓰는 ninja-archetypes.json 의 PoE2 판.
//   ① 개요 검색(top-100) + 전직별 class= 필터 검색(각 top-100)을 모아 캐릭터를 중복 없이 합친다(표본 확대)
//      ⚠ skills= 필터는 쓰지 않는다 — PoE2 의 skills 차원은 "쓴 모든 스킬"(저주·표식·오라 포함, 패싯에 Elemental Weakness 2만)이라
//        필터한 스킬을 메인으로 볼 수 없고, 그 응답엔 캐릭터의 메인 스킬(dps.skill)도 빠진다(dps-<스킬>.* 만, 표식·저주는 DPS 공란). 09-30 실측.
//        class= 필터 응답은 dps.total·dps.skill(사이트가 고른 메인)이 그대로라 아키타입 키가 정확하다.
//   ② 아키타입별 중앙값(레벨·생명·ES·EHP·DPS)·성향(lean: 전체 중앙값 대비 DPS/EHP 로그비 — PoE1 leanOf 와 같은 임계 0.35)·
//      상위 키스톤·동반 스킬, 스킬별 패싯(아이템·키스톤·도유 사용률 — **전체 모집단** 기준)
// 사용: node fetch-ninja-builds2.mjs
// 산출물: ~/.poe-gamedata/poe2/ninja/ninja-builds2.json · ninja-archetypes2.json
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { currentLeague2, fetchDictionaries2, fetchSearch2, parseAbbrev } from "./ninja-protocol2.mjs";

const OUT_DIR = path.join(os.homedir(), ".poe-gamedata", "poe2", "ninja");
const MIN_ARCHETYPE = 5; // 표본이 이보다 적은 아키타입은 중앙값이 한두 사람 값이라 싣지 않는다
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const median = (a) => { const s = a.filter((x) => x != null && Number.isFinite(x)).sort((x, y) => x - y); return s.length ? s[Math.floor((s.length - 1) / 2)] : null; };

async function search(snapshot, league, cls) {
	for (let attempt = 0; attempt < 4; attempt++) {
		try { return await fetchSearch2(snapshot, league, null, cls); }
		catch (e) {
			if (e.status !== 429) throw e;
			const w = (Number.isFinite(e.retryAfter) && e.retryAfter > 0 ? e.retryAfter : 60) * 1000 + 3000;
			console.log(`[ninja2] 레이트리밋 — ${Math.round(w / 1000)}초 대기`);
			await sleep(w);
		}
	}
	throw new Error("레이트리밋 지속");
}

function decode(s, dicts) {
	const lbl = (dim, i) => (dicts[dim] && i != null && i >= 0 ? dicts[dim][i] ?? null : null);
	const c = s.columns, out = [];
	for (let i = 0; i < s.rowCount; i++) {
		const gems = Array.isArray(c.skills?.[i]) ? c.skills[i].map((x) => lbl("gem", x)).filter(Boolean) : [];
		const main = lbl("gem", c["dps.skill"]?.[i]);
		out.push({
			name: c.name?.[i] ?? null,
			account: c.account?.[i] ?? null,
			ascendancy: lbl("class", c.class?.[i]),
			level: c.level?.[i] ?? null,
			life: c.life?.[i] ?? null,
			energyShield: c.energyshield?.[i] ?? null,
			ehp: parseAbbrev(c.ehp__str?.[i] ?? c.ehp?.[i]),
			dps: parseAbbrev(c["dps.total"]?.[i]),
			// 사이트가 고른 메인(dps.skill = DPS 기준 스킬) — 없으면 아키타입에 넣지 않는다(첫 스킬은 오라·표식일 수 있다)
			mainSkill: main ?? null,
			skills: gems,
			keystones: Array.isArray(c.keypassives?.[i]) ? c.keypassives[i].map((x) => lbl("keypassive", x)).filter(Boolean) : [],
		});
	}
	return out;
}

function facetsOf(s, dicts, keep = ["items", "keypassives", "anointed", "weaponmode"], cap = 12) {
	const out = {};
	for (const f of s.facets || []) {
		if (!keep.includes(f.name)) continue;
		const dict = dicts[f.dim] || [];
		out[f.name] = f.buckets.slice().sort((a, b) => b.c - a.c).slice(0, cap).map((b) => ({ name: dict[b.i] ?? `#${b.i}`, count: b.c }));
	}
	return { total: s.total, groups: out };
}

const top = (counter, n) => [...counter.entries()].sort((a, b) => b[1] - a[1]).slice(0, n).map(([name, count]) => ({ name, count }));

const { league, snapshot } = await currentLeague2();
// 버전 확인만(NINJA_CHECK=1) — API 의 스냅샷 동기(Poe2NinjaSyncService)가 저장본(ninja-archetypes2.json snapshot)과 비교해 바뀐 때만 전체 수집.
// process.exit 로 끊으면 윈도에서 열린 소켓 정리 중 libuv 단언으로 비정상 종료(127)한다 — 수집부를 함수로 두고 자연 종료.
if (process.env.NINJA_CHECK) {
	console.log(`@@NINJA_VERSION@@ ${JSON.stringify({ league, snapshot })}`);
} else {
	await collect();
}

async function collect() {
	console.log(`[ninja2] 리그 ${league} · 스냅샷 ${snapshot}`);
	const overview = await search(snapshot, league);
	const dicts = await fetchDictionaries2(overview.dimHash);
	const byName = new Map();
	const add = (list) => { for (const b of list) if (b.name && !byName.has(b.name)) byName.set(b.name, b); };
	add(decode(overview, dicts));
	// 전직 = 개요 패싯 class(전체 모집단 수) 중 캐릭터가 있는 것 전부 — 전직별 top-100 으로 표본을 넓힌다. 패싯(아이템·키스톤·도유)도 전직별로 둔다.
	const classFacet = (overview.facets || []).find((f) => f.name === "class");
	const classNames = classFacet
		? classFacet.buckets.filter((b) => b.c > 0).sort((a, b) => b.c - a.c).map((b) => (dicts[classFacet.dim] || [])[b.i]).filter(Boolean)
		: [];
	// 전직별 **전체 모집단** 수(개요 패싯) — 캐릭터 표본은 전직마다 top-100 이라 전직 인기를 못 담는다. API 가 선택지 순서를 인구 가중으로 추정할 때 쓴다.
	const classPopulation = {};
	if (classFacet) for (const b of classFacet.buckets) { const n = (dicts[classFacet.dim] || [])[b.i]; if (n && b.c > 0) classPopulation[n] = b.c; }
	const classFacets = {};
	for (const cls of classNames) {
		try {
			const s = await search(snapshot, league, cls);
			add(decode(s, dicts));
			classFacets[cls] = facetsOf(s, dicts);
		} catch (e) {
			console.warn(`[ninja2] ${cls} 검색 실패: ${e.message}`);
		}
		await sleep(300);
	}
	const builds = [...byName.values()].filter((b) => b.ascendancy && b.mainSkill);
	console.log(`[ninja2] 캐릭터 ${builds.length}명 (전직 필터 ${classNames.length}개)`);

	const gDps = median(builds.map((b) => b.dps).filter((x) => x > 0)), gEhp = median(builds.map((b) => b.ehp).filter((x) => x > 0));
	const leanOf = (dps, ehp) => {
		if (!(dps > 0 && ehp > 0 && gDps > 0 && gEhp > 0)) return "balanced";
		const diff = Math.log10(dps / gDps) - Math.log10(ehp / gEhp);
		return diff > 0.35 ? "dps" : diff < -0.35 ? "ehp" : "balanced";
	};
	function aggregate(list, ascendancy, mainSkill) {
		const keys = new Map(), co = new Map();
		for (const b of list) {
			for (const k of b.keystones) keys.set(k, (keys.get(k) || 0) + 1);
			for (const g of b.skills) if (g !== mainSkill) co.set(g, (co.get(g) || 0) + 1);
		}
		const mDps = median(list.map((b) => b.dps).filter((x) => x > 0)), mEhp = median(list.map((b) => b.ehp).filter((x) => x > 0));
		return {
			ascendancy, mainSkill, sample: list.length,
			medianLevel: median(list.map((b) => b.level)),
			medianLife: median(list.map((b) => b.life)),
			medianES: median(list.map((b) => b.energyShield)),
			medianEHP: mEhp, medianDPS: mDps, lean: leanOf(mDps, mEhp),
			topKeystones: top(keys, 6), topCoSkills: top(co, 8),
		};
	}
	const groups = new Map(), bySkill = new Map();
	for (const b of builds) {
		const k = `${b.ascendancy}|${b.mainSkill}`;
		(groups.get(k) || groups.set(k, []).get(k)).push(b);
		(bySkill.get(b.mainSkill) || bySkill.set(b.mainSkill, []).get(b.mainSkill)).push(b);
	}
	const archetypes = {};
	// 패싯은 전직 단위(아이템·키스톤·도유 사용률 — 그 전직 **전체 모집단** 기준)
	for (const [k, list] of groups) if (list.length >= MIN_ARCHETYPE) archetypes[k] = { ...aggregate(list, k.split("|")[0], k.split("|")[1]), facets: classFacets[k.split("|")[0]] ?? null };
	const skillArchetypes = {};
	for (const [sk, list] of bySkill) if (list.length >= MIN_ARCHETYPE) skillArchetypes[sk] = aggregate(list, "", sk);

	fs.mkdirSync(OUT_DIR, { recursive: true });
	fs.writeFileSync(path.join(OUT_DIR, "ninja-builds2.json"), JSON.stringify({ league, snapshot, fetchedAt: new Date().toISOString(), classPopulation, builds }, null, 1));
	fs.writeFileSync(path.join(OUT_DIR, "ninja-archetypes2.json"), JSON.stringify({ league, snapshot, globalMedianDps: gDps, globalMedianEhp: gEhp, archetypes, skillArchetypes }, null, 1));
	console.log(`[ninja2] 아키타입 ${Object.keys(archetypes).length}개 · 스킬 ${Object.keys(skillArchetypes).length}개 → ${OUT_DIR}`);
}
