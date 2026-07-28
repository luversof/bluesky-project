// poe.ninja 실빌드 페처 — private protobuf(컬럼형)+dictionary 응답을 역공학해 빌드 레코드로 복원.
//
// 흐름:
//   1) poe.ninja/poe1/builds/{league} HTML 에서 해당 리그의 snapshot(version) 해석
//   2) /poe1/api/builds/{snapshot}/search?overview={league}&type=exp  → protobuf
//        - top-level field#5 = 행 데이터(컬럼별 컬럼 배열, 기본 10컬럼)
//        - 응답 본문의 40-hex 문자열 = 범주 차원별 dictionary content-hash (15개)
//   3) /poe1/api/builds/dictionary/{hash} → 각 차원의 라벨 배열(self-describing field#1=차원명)
//   4) 컬럼 배열을 zip + dictionary join → per-build 레코드
//   5) 아키타입(전직×메인스킬)별 집계 → 최적화기 시드 후보
//
// 산출물: ~/.poe-gamedata/ninja/ninja-builds-{league}.json, ninja-archetypes-{league}.json
//
// 컬럼 인코딩(각 field#5 엔트리 = {#1:컬럼명, #2:행엔트리 반복}):
//   문자열(name,account,ehp,dps): 행엔트리 내부 #1 = 표시문자열
//   스칼라(level,life,energyshield): 내부 #2 = varint
//   단일범주(class=전직): 내부 #2 = dict 인덱스
//   리스트범주(skills,keypassives): 내부 #3 = packed varint 배열(dict 인덱스들)

import fs from "node:fs";
import path from "node:path";
import { DATA_DIR } from "./paths.mjs";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) poe-gamedata-fetcher";
const BASE = "https://poe.ninja/poe1";
// 리그는 여러 개 지정 가능(공백/콤마). 서버가 정렬별 top-100 만 주므로 여러 리그를 모아 표본을 키운다.
// 인자 없으면 런타임에 현재 빌드 리그(스냅샷 최신 SC 리그)를 자동 감지 → run-all(시즌 자동 갱신)에서 인자 없이 호출.
const LEAGUE_ARGS = process.argv.slice(2).join(",").split(/[,\s]+/).filter(Boolean);
const OUT_DIR = path.join(DATA_DIR, "ninja");

// ---------- protobuf 와이어 디코더 ----------
function readVarint(b, pos) {
	let result = 0n, shift = 0n, p = pos;
	while (true) {
		const byte = b[p++];
		result |= BigInt(byte & 0x7f) << shift;
		if ((byte & 0x80) === 0) break;
		shift += 7n;
	}
	return [result, p];
}

/** length-delimited 메시지를 필드 목록으로 (얕게) 디코드 */
function walk(bb) {
	const out = [];
	let pos = 0;
	while (pos < bb.length) {
		const [tag, q] = readVarint(bb, pos);
		const fn = Number(tag >> 3n), w = Number(tag & 7n);
		if (fn === 0) break;
		let np = q, info;
		if (w === 0) { const [v, y] = readVarint(bb, q); info = { fn, w, v }; np = y; }
		else if (w === 2) { const [l, x] = readVarint(bb, q); const L = Number(l); info = { fn, w, sub: bb.subarray(x, x + L), len: L }; np = x + L; }
		else if (w === 5) { info = { fn, w, v: BigInt(bb.readUInt32LE(q)) }; np = q + 4; }
		else if (w === 1) { info = { fn, w, v: bb.readBigUInt64LE(q) }; np = q + 8; }
		else break;
		out.push(info);
		pos = np;
	}
	return out;
}

/** packed varint 배열 디코드 */
function unpackVarints(bb) {
	const out = [];
	let pos = 0;
	while (pos < bb.length) { const [v, p] = readVarint(bb, pos); out.push(Number(v)); pos = p; }
	return out;
}

// ---------- HTTP ----------
async function httpGet(url, asText = false) {
	const res = await fetch(url, { headers: { "User-Agent": UA, "Accept": "*/*" } });
	if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
	if (asText) return res.text();
	return Buffer.from(await res.arrayBuffer());
}

// ---------- 현재 빌드 리그 자동 감지 ----------
// poe.ninja 빌드는 "현재 경제 리그"(allflame 등, 신규라 빌드 스냅샷 없음)와 다르다. 빌드 스냅샷이 있는
// **SC 베이스 리그 중 스냅샷 날짜가 최신**인 것을 고른다(HC/SSF/Ruthless/private/standard 제외).
async function resolveCurrentLeagues() {
	const clean = (await httpGet(`${BASE}/builds`, true)).replace(/&quot;/g, '"').replace(/&amp;/g, "&");
	const re = /"version":\[0,"(20\d\d)-(\d{8})-\d+"\],"snapshotName":\[0,"([a-z]+)"\]/g;
	let m; const cand = {};
	while ((m = re.exec(clean))) {
		const [, seq, date, url] = m;
		// SC 베이스만: hc/ssf/r 접미어, standard/hardcore 제외
		if (/(hc|ssf|r)$/.test(url) || url === "standard" || url === "hardcore") continue;
		const prev = cand[url];
		if (!prev || date > prev.date) cand[url] = { date, seq };
	}
	const sorted = Object.entries(cand).sort((a, b) => (b[1].date.localeCompare(a[1].date)) || (b[1].seq.localeCompare(a[1].seq)));
	return sorted.length ? [sorted[0][0]] : ["mirage"];
}

// ---------- 1) snapshot 해석 ----------
async function resolveSnapshot(league) {
	const html = await httpGet(`${BASE}/builds/${league}`, true);
	const clean = html.replace(/&quot;/g, '"').replace(/&amp;/g, "&");
	// snapshot 리스트 엔트리는 version 과 snapshotName 이 인접 페어다:
	//   "version":[0,"2031-20260720-57744"],"snapshotName":[0,"mirage"]
	// snapshotName 이 리그 식별자이므로 이걸로 정확히 매칭한다(url 매칭은 드롭다운/중첩과 충돌).
	const re = new RegExp(`"version":\\[0,"(20\\d\\d-\\d{8}-\\d+)"\\],"snapshotName":\\[0,"${league}"\\]`);
	const m = clean.match(re);
	if (!m) throw new Error(`snapshot 해석 실패: league=${league}`);
	return m[1];
}

const toSigned = (v) => (v >= (1n << 63n) ? Number(v - (1n << 64n)) : Number(v)); // 카오스저항 등 음수 varint

// ---------- 2) search → 컬럼 + dict 해시 ----------
// columnsParam(옵션): 스칼라 컬럼(저항/armour/per-type maxhit 등)을 골라 받는다. null=기본 10컬럼(skills/keypassives 포함).
// ⚠ 리스트 컬럼(skills/keypassives/items/masteries)은 columns= 로 선택 불가 — 기본 세트에만 있음.
// filterSkill(옵션): skills=<스킬명> 서버측 필터 → 그 스킬 사용 top-100 만 반환(검증: 반환 전 행에 해당 젬 인덱스 존재).
async function fetchSearch(snapshot, league, columnsParam, filterSkill) {
	let url = `${BASE}/api/builds/${snapshot}/search?overview=${league}&type=exp`;
	if (columnsParam) url += `&columns=${columnsParam}`;
	if (filterSkill) url += `&skills=${encodeURIComponent(filterSkill)}`;
	const buf = await httpGet(url);
	// 최상위 래퍼 field#1 언랩
	const [, p0] = readVarint(buf, 0);
	const [l0, p1] = readVarint(buf, p0);
	const body = buf.subarray(p1, p1 + Number(l0));
	const top = walk(body);

	// dict 해시(40-hex) 전부 수집 (본문 전체에서)
	const hashes = [...new Set((buf.toString("latin1").match(/[0-9a-f]{40}/g) || []))];

	// field#5 = 데이터 컬럼
	const columns = {};
	let rowCount = 0;
	for (const t of top.filter((x) => x.fn === 5 && x.w === 2)) {
		const parts = walk(t.sub);
		// 컬럼명 = field#1 문자열. skills= 필터 시 dps 컬럼은 "dps-<스킬명>"(공백/대문자 포함)으로 온다 → 관대하게 허용.
		const nameField = parts.find((p) => p.fn === 1 && p.w === 2 && /^[a-z][\x20-\x7e]*$/.test(p.sub.toString("utf8")));
		if (!nameField) continue;
		const colName = nameField.sub.toString("utf8");
		const rows = parts.filter((p) => p.fn === 2 && p.w === 2);
		rowCount = Math.max(rowCount, rows.length);
		columns[colName] = rows.map((r) => {
			const inner = walk(r.sub);
			const s = inner.find((x) => x.fn === 1 && x.w === 2);
			if (s) return s.sub.toString("utf8");            // 문자열/약어(maxhit·ehp·dps) 컬럼
			const list = inner.find((x) => x.fn === 3 && x.w === 2);
			if (list) return unpackVarints(list.sub);         // 리스트 범주(packed)
			const scalar = inner.find((x) => x.fn === 2 && x.w === 0);
			if (scalar) return toSigned(scalar.v);            // 스칼라/단일범주(음수 가능)
			return null;                                       // 빈 값(인덱스 0 / 없음)
		});
	}
	return { columns, hashes, rowCount };
}

// ---------- 3) dictionaries ----------
async function fetchDictionaries(hashes) {
	const dicts = {}; // dimensionName -> string[]
	for (const h of hashes) {
		let buf;
		try { buf = await httpGet(`${BASE}/api/builds/dictionary/${h}`); }
		catch { continue; }
		const parts = walk(buf);
		const name = parts[0] && parts[0].w === 2 ? parts[0].sub.toString("utf8") : null;
		if (!name) continue;
		// field#2 반복 = 라벨(순서 = 인덱스)
		const labels = parts.filter((p) => p.fn === 2 && p.w === 2).map((p) => p.sub.toString("utf8"));
		dicts[name] = labels;
	}
	return dicts;
}

// ---------- 유틸 ----------
function parseAbbrev(s) { // "686k","2.9M","1.1B" -> number
	if (s == null || s === "-" || s === "") return 0;
	const m = String(s).match(/^([\d.]+)\s*([kKmMbB]?)$/);
	if (!m) return Number(s) || 0;
	const n = parseFloat(m[1]);
	const mult = { "": 1, k: 1e3, K: 1e3, m: 1e6, M: 1e6, b: 1e9, B: 1e9 }[m[2]] || 1;
	return Math.round(n * mult);
}
const lbl = (dict, i) => (dict && i != null && i >= 0 && i < dict.length ? dict[i] : null);
const isSupportGem = (name) => /\bSupport$/.test(name); // 보조젬은 이름이 "... Support" 로 끝남

// 리치(방어 스칼라·per-type maxhit) 컬럼 — columns= 로 요청 가능한 것만(리스트 컬럼 제외).
const RICH_COLS = [
	"liferegen", "fireres", "coldres", "lightningres", "chaosres",
	"armour", "evasion", "block", "suppress", "phystakenas",
	"physicalmax", "firemax", "coldmax", "lightningmax", "chaosmax", "lowestmax",
].join(",");

const MAX_FILTER_SKILLS = Number(process.env.NINJA_MAX_SKILLS || 80); // per-skill 확장 상한(요청 수 제어; overview 인기 스킬 대부분 커버)

// 한 쿼리(overview 또는 skills= 필터)의 base+rich 를 병합해 빌드 배열로 디코드.
async function decodePair(snapshot, league, dicts, filterSkill) {
	const classDict = dicts.class || [], gemDict = dicts.gem || [], keyDict = dicts.keypassive || [];
	const base = await fetchSearch(snapshot, league, null, filterSkill);
	const rich = await fetchSearch(snapshot, league, RICH_COLS, filterSkill);
	const richIdx = {};
	(rich.columns.name || []).forEach((nm, i) => { if (nm != null) richIdx[nm] = i; });
	const rc = rich.columns;
	// dps 컬럼: overview 는 "dps", skills= 필터는 "dps-<스킬명>"(해당 스킬 전용 DPS). 어느 쪽이든 잡는다.
	const dpsKey = Object.keys(base.columns).find((k) => k === "dps" || k.startsWith("dps-"));
	const dpsCol = dpsKey ? base.columns[dpsKey] : null;
	const builds = [];
	for (let i = 0; i < base.rowCount; i++) {
		const name = base.columns.name?.[i] ?? null;
		const gems = Array.isArray(base.columns.skills?.[i]) ? base.columns.skills[i].map((x) => lbl(gemDict, x)).filter(Boolean) : [];
		const actives = gems.filter((g) => !isSupportGem(g));
		const keystones = Array.isArray(base.columns.keypassives?.[i]) ? base.columns.keypassives[i].map((x) => lbl(keyDict, x)).filter(Boolean) : [];
		const j = name != null && richIdx[name] != null ? richIdx[name] : i; // 폴백: 같은 인덱스
		builds.push({
			league, name, account: base.columns.account?.[i] ?? null,
			ascendancy: lbl(classDict, base.columns.class?.[i] ?? 0),
			level: base.columns.level?.[i] ?? null, life: base.columns.life?.[i] ?? null,
			energyShield: base.columns.energyshield?.[i] ?? null,
			ehp: parseAbbrev(base.columns.ehp?.[i]), dps: parseAbbrev(dpsCol?.[i]),
			// skills= 필터 시엔 그 스킬을 메인으로 간주(overview 순서와 무관하게 요청 스킬이 정체성).
			mainSkill: filterSkill && actives.includes(filterSkill) ? filterSkill : (actives[0] ?? null),
			activeSkills: actives, keystones,
			lifeRegen: rc.liferegen?.[j] ?? null,
			fireRes: rc.fireres?.[j] ?? null, coldRes: rc.coldres?.[j] ?? null,
			lightningRes: rc.lightningres?.[j] ?? null, chaosRes: rc.chaosres?.[j] ?? null,
			armour: rc.armour?.[j] ?? null, evasion: rc.evasion?.[j] ?? null,
			block: rc.block?.[j] ?? null, suppress: rc.suppress?.[j] ?? null,
			physTakenAs: rc.phystakenas?.[j] ?? null,
			physicalMax: parseAbbrev(rc.physicalmax?.[j]), fireMax: parseAbbrev(rc.firemax?.[j]),
			coldMax: parseAbbrev(rc.coldmax?.[j]), lightningMax: parseAbbrev(rc.lightningmax?.[j]),
			chaosMax: parseAbbrev(rc.chaosmax?.[j]), lowestMax: parseAbbrev(rc.lowestmax?.[j]),
		});
	}
	return builds;
}

// ---------- 리그 단위 페치 (overview + per-skill 필터 확장) ----------
// 서버가 정렬별 top-100 만 주므로, overview 로 인기 스킬을 식별한 뒤 skills=<스킬> 필터로 스킬별 top-100 을
// 추가 수집한다(GET 서버측 필터 확인됨). 아키타입 표본이 스킬당 ~6개 → ~100개로 커진다.
async function fetchLeague(league) {
	const snapshot = await resolveSnapshot(league);
	const overviewBase = await fetchSearch(snapshot, league, null);
	const dicts = await fetchDictionaries(overviewBase.hashes);
	const gemDict = dicts.gem || [];

	const seen = new Set();
	const all = [];
	const add = (arr) => { for (const b of arr) { const key = `${b.league}|${b.name}`; if (b.name && !seen.has(key)) { seen.add(key); all.push(b); } } };

	// 1) overview top-100
	add(await decodePair(snapshot, league, dicts, null));
	// 2) 인기 스킬(= overview 빌드의 메인 액티브 distinct) 별 top-100 확장
	const skillFreq = {};
	for (const b of all) if (b.mainSkill) skillFreq[b.mainSkill] = (skillFreq[b.mainSkill] || 0) + 1;
	const targetSkills = Object.entries(skillFreq).sort((a, b) => b[1] - a[1]).slice(0, MAX_FILTER_SKILLS).map(([s]) => s);
	let expanded = 0;
	for (const skill of targetSkills) {
		try {
			const before = all.length;
			add(await decodePair(snapshot, league, dicts, skill));
			expanded += all.length - before;
		} catch (e) { console.warn(`  skills=${skill} 실패:`, e.message); }
	}
	console.log(`[ninja] ${league} @${snapshot}: ${all.length} builds (overview + ${targetSkills.length}스킬 확장 +${expanded}, dict gem ${gemDict.length})`);
	return { snapshot, builds: all };
}

// ---------- 아키타입 집계 ----------
const _median = (arr) => { const s = arr.filter((v) => v != null && !Number.isNaN(v)).sort((a, x) => a - x); return s.length ? s[Math.floor(s.length / 2)] : 0; };
const _topN = (counter, n) => Object.entries(counter).sort((a, x) => x[1] - a[1]).slice(0, n).map(([k, c]) => ({ name: k, count: c }));

/** 한 그룹(빌드 배열)의 중앙값 프로파일. base = 그룹 식별 필드({ascendancy?, mainSkill}). */
function groupProfile(arr, base) {
	const med = (f) => _median(arr.map(f));
	const coCnt = {}, keyCnt = {};
	for (const b of arr) {
		for (const s of b.activeSkills) if (s !== base.mainSkill) coCnt[s] = (coCnt[s] || 0) + 1;
		for (const k of b.keystones) keyCnt[k] = (keyCnt[k] || 0) + 1;
	}
	return {
		...base, sample: arr.length,
		medianLevel: med((b) => b.level),
		medianLife: med((b) => b.life), medianES: med((b) => b.energyShield),
		medianEHP: med((b) => b.ehp), medianDPS: med((b) => b.dps),
		medianLifeRegen: med((b) => b.lifeRegen),
		medianFireRes: med((b) => b.fireRes), medianColdRes: med((b) => b.coldRes),
		medianLightningRes: med((b) => b.lightningRes), medianChaosRes: med((b) => b.chaosRes),
		medianArmour: med((b) => b.armour), medianEvasion: med((b) => b.evasion),
		medianBlock: med((b) => b.block), medianSuppress: med((b) => b.suppress),
		medianPhysicalMax: med((b) => b.physicalMax), medianFireMax: med((b) => b.fireMax),
		medianColdMax: med((b) => b.coldMax), medianLightningMax: med((b) => b.lightningMax),
		medianChaosMax: med((b) => b.chaosMax), medianLowestMax: med((b) => b.lowestMax),
		topCoSkills: _topN(coCnt, 8),
		topKeystones: _topN(keyCnt, 6),
	};
}

/** (전직 × 메인스킬) 아키타입 집계. */
function aggregate(builds) {
	const groups = {};
	for (const b of builds) {
		if (!b.ascendancy || !b.mainSkill) continue;
		(groups[`${b.ascendancy}|${b.mainSkill}`] ||= []).push(b);
	}
	return Object.entries(groups)
		.map(([key, arr]) => { const [ascendancy, mainSkill] = key.split("|"); return groupProfile(arr, { ascendancy, mainSkill }); })
		.sort((a, b) => b.sample - a.sample);
}

/** 스킬 단위 집계(전 전직 통합) — 자동전직 잡의 견고한 폴백 목표치(전직별 저표본 노이즈 회피). */
function aggregateBySkill(builds) {
	const groups = {};
	for (const b of builds) {
		if (!b.mainSkill) continue;
		(groups[b.mainSkill] ||= []).push(b);
	}
	return Object.entries(groups)
		.map(([mainSkill, arr]) => groupProfile(arr, { mainSkill }))
		.sort((a, b) => b.sample - a.sample);
}

// ---------- 메인 ----------
async function main() {
	const LEAGUES = LEAGUE_ARGS.length ? LEAGUE_ARGS : await resolveCurrentLeagues();
	console.log(`[ninja] leagues=${LEAGUES.join(", ")}${LEAGUE_ARGS.length ? "" : " (자동 감지)"}`);
	fs.mkdirSync(OUT_DIR, { recursive: true });
	const all = [];
	const snapshots = {};
	for (const league of LEAGUES) {
		try {
			const { snapshot, builds } = await fetchLeague(league);
			snapshots[league] = snapshot;
			all.push(...builds);
		} catch (e) { console.error(`[ninja] ${league} 실패:`, e.message); }
	}
	const archetypes = aggregate(all);
	const skillArchetypes = aggregateBySkill(all); // 스킬 단위 폴백(자동전직)
	// 서비스(PoeOptimizeService)가 읽는 canonical 파일명은 고정 — 리그/병합 무관하게 항상 최신을 로드.
	//   run-all 자동감지(단일 리그)든 수동 다중 리그든 여기에 쓴다. tag 파일은 참고용 부가.
	const buildsData = JSON.stringify({ leagues: LEAGUES, snapshots, fetchedRows: all.length, builds: all }, null, 2);
	const archData = JSON.stringify({ leagues: LEAGUES, snapshots, archetypes, skillArchetypes }, null, 2);
	const buildsPath = path.join(OUT_DIR, "ninja-builds.json");
	const archPath = path.join(OUT_DIR, "ninja-archetypes.json");
	fs.writeFileSync(buildsPath, buildsData);
	fs.writeFileSync(archPath, archData);
	const tag = LEAGUES.length === 1 ? LEAGUES[0] : "merged"; // 참고용 태그 사본
	fs.writeFileSync(path.join(OUT_DIR, `ninja-archetypes-${tag}.json`), archData);
	console.log(`[ninja] wrote ${all.length} builds → ${buildsPath}`);
	console.log(`[ninja] wrote ${archetypes.length} archetypes → ${archPath}`);
	console.log(`\n[ninja] 상위 아키타입:`);
	for (const a of archetypes.slice(0, 10)) {
		console.log(`  ${a.ascendancy} / ${a.mainSkill} (n=${a.sample}) life≈${a.medianLife} es≈${a.medianES} ehp≈${a.medianEHP} dps≈${a.medianDPS}`);
		console.log(`     maxhit phys/fire/cold/lit/chaos/low = ${a.medianPhysicalMax}/${a.medianFireMax}/${a.medianColdMax}/${a.medianLightningMax}/${a.medianChaosMax}/${a.medianLowestMax} | res ${a.medianFireRes}/${a.medianColdRes}/${a.medianLightningRes}/${a.medianChaosRes}`);
		if (a.topKeystones.length) console.log(`     keystones: ${a.topKeystones.slice(0, 4).map((s) => s.name).join(", ")}`);
	}
}

main().catch((e) => { console.error(e); process.exit(1); });
