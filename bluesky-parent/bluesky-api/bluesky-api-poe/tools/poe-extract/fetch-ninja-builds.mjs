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
async function fetchSearch(snapshot, league, columnsParam, filterSkill, filterClass) {
	let url = `${BASE}/api/builds/${snapshot}/search?overview=${league}&type=exp`;
	if (columnsParam) url += `&columns=${columnsParam}`;
	if (filterSkill) url += `&skills=${encodeURIComponent(filterSkill)}`;
	if (filterClass) url += `&class=${encodeURIComponent(filterClass)}`;
	const buf = await httpGet(url);
	// 최상위 래퍼 field#1 언랩
	const [, p0] = readVarint(buf, 0);
	const [l0, p1] = readVarint(buf, p0);
	const body = buf.subarray(p1, p1 + Number(l0));
	const top = walk(body);

	// dict 해시(40-hex) 전부 수집 (본문 전체에서)
	const hashes = [...new Set((buf.toString("latin1").match(/[0-9a-f]{40}/g) || []))];

	// field#1(varint) = 필터 매칭 **전체 모집단** 수(top-100 표본이 아님) — 패싯 % 의 분모.
	const total = Number(top.find((x) => x.fn === 1 && x.w === 0)?.v ?? 0);
	// field#6 = 차원 이름 → 사전 해시 (패싯 라벨 해석용)
	const dimHash = {};
	for (const t of top.filter((x) => x.fn === 6 && x.w === 2)) {
		const parts = walk(t.sub);
		const nm = parts.find((p) => p.fn === 1 && p.w === 2)?.sub.toString("utf8");
		const h = parts.find((p) => p.fn === 2 && p.w === 2)?.sub.toString("utf8");
		if (nm && h) dimHash[nm] = h;
	}
	// field#2 = 패싯 블록(사이트 좌측 사이드바 집계) — {1:이름, 2:차원, 3:버킷*}, 버킷 {1:사전인덱스(생략=0), 2:카운트}.
	//   카운트는 **전체 모집단** 기준이라 top-100 표본 집계보다 훨씬 정확(사용자 요청의 근거 데이터).
	const facets = [];
	for (const t of top.filter((x) => x.fn === 2 && x.w === 2)) {
		const parts = walk(t.sub);
		const nm = parts.find((p) => p.fn === 1 && p.w === 2)?.sub.toString("utf8");
		const dim = parts.find((p) => p.fn === 2 && p.w === 2)?.sub.toString("utf8");
		if (!nm || !dim) continue;
		const buckets = parts
			.filter((p) => p.fn === 3 && p.w === 2)
			.map((p) => {
				const inner = walk(p.sub);
				return {
					i: Number(inner.find((x) => x.fn === 1 && x.w === 0)?.v ?? 0),
					c: Number(inner.find((x) => x.fn === 2 && x.w === 0)?.v ?? 0),
				};
			});
		facets.push({ name: nm, dim, buckets });
	}
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
	return { columns, hashes, rowCount, total, facets, dimHash };
}

// 패싯 라벨 해석 + 요약 — 사전으로 인덱스→라벨, 카운트 내림차순 상위 cap 개만.
//   보존 차원: 사용자가 참고 요청한 마스터리/룬크래프트/문신/무기설정/판테온/아틀라스패시브 + 도유/혈맹/산적/장비/키스톤.
const FACET_KEEP = new Set([
	"masteries", "runegrafts", "tattoos", "weaponmode", "pantheons", "pantheon",
	"atlasskills", "anointed", "secondascendancy", "bandit", "items", "keypassives", "shrinebeltbuffs",
]);
function resolveFacets(search, dicts, cap = 12) {
	const out = {};
	for (const f of search.facets || []) {
		if (!FACET_KEEP.has(f.name)) continue;
		const dict = dicts[f.dim] || [];
		out[f.name] = f.buckets
			.slice()
			.sort((a, b) => b.c - a.c)
			.slice(0, cap)
			.map((b) => ({ name: dict[b.i] ?? `#${b.i}`, count: b.c }));
	}
	return { total: search.total, groups: out };
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
// poe.ninja 검색 스칼라 컬럼 전체(스샷의 전 컬럼) — 실제 API 키명(사용자 제공/프로브 확정).
//   방어/공격 스칼라 + 속성(str/dex/int) + 부가 자원(ward/mana/이동속도/아이템희귀도) +
//   충전(echarges/fcharges/pcharges) + 주문막기/회피(sblock/sdodge) +
//   클러스터주얼 개수(cjewels/lcjewels/mcjewels/scjewels) + 유니크/미러 장비 개수(uequip/mequip/mweapons/marmours).
//   ※ character/level/life/energyshield/ehp/dps/keystoneskill 은 base 응답에 이미 포함 → 여기선 제외.
const RICH_COLS = [
	"liferegen", "fireres", "coldres", "lightningres", "chaosres",
	"armour", "evasion", "block", "suppress", "phystakenas",
	"physicalmax", "firemax", "coldmax", "lightningmax", "chaosmax", "lowestmax",
	"ward", "mana", "movementspeed", "itemrarity", "str", "dex", "int",
	"echarges", "fcharges", "pcharges", "sblock", "sdodge",
	"cjewels", "lcjewels", "mcjewels", "scjewels",
	"uequip", "mequip", "mweapons", "marmours",
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
			// 스킬별 전용 DPS — skills= 필터 응답의 dps-<스킬> 은 그 스킬 전용 DPS. 같은 캐릭터가 여러 스킬 필터
			// top-100 에 겹치면 fetchLeague 의 add() 가 이 맵을 병합해 캐릭터당 {스킬: 전용DPS} 를 축적한다.
			// (조합 벤치의 DPS 를 "메인 스킬 전용"으로 통일하는 근거 데이터 — 혼합 dps 중앙값은 의미가 없음.)
			dpsBySkill: filterSkill && dpsKey && dpsKey.startsWith("dps-") ? { [filterSkill]: parseAbbrev(dpsCol?.[i]) } : {},
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
			// 부가 자원/속성/충전/주문막기/클러스터·미러 개수(스샷 전 컬럼).
			ward: parseAbbrev(rc.ward?.[j]), mana: parseAbbrev(rc.mana?.[j]),
			movementSpeed: rc.movementspeed?.[j] ?? null, itemRarity: rc.itemrarity?.[j] ?? null,
			str: rc.str?.[j] ?? null, dex: rc.dex?.[j] ?? null, int: rc.int?.[j] ?? null,
			enduranceCharges: rc.echarges?.[j] ?? null, frenzyCharges: rc.fcharges?.[j] ?? null,
			powerCharges: rc.pcharges?.[j] ?? null,
			spellBlock: rc.sblock?.[j] ?? null, spellDodge: rc.sdodge?.[j] ?? null,
			clusterJewels: rc.cjewels?.[j] ?? null, largeCluster: rc.lcjewels?.[j] ?? null,
			mediumCluster: rc.mcjewels?.[j] ?? null, smallCluster: rc.scjewels?.[j] ?? null,
			uniqueEquip: rc.uequip?.[j] ?? null, mirroredItems: rc.mequip?.[j] ?? null,
			mirroredWeapons: rc.mweapons?.[j] ?? null, mirroredArmours: rc.marmours?.[j] ?? null,
		});
	}
	return { builds, search: base };
}

// ---------- 마스터리 채집 (캐릭터 상세 JSON) ----------
// 검색 API 는 리스트 컬럼(masteries)을 columns= 로 주지 않는다(실측: 요청해도 기본 10컬럼만).
// 대신 캐릭터 상세 `${BASE}/api/builds/{snapshot}/character?...` 가 **평문 JSON** 으로
// masteries[{name,group,nodeId}] 를 노출한다. 전 캐릭터(3.5k) 페치는 과하므로
// (전직|메인스킬) 그룹별 레벨 상위 N 명만 샘플링 + snapshot 단위 디스크 캐시로 재실행 무료화.
// ⚠ 캐릭터 엔드포인트는 창(약 4~5분)당 ~60요청의 엄격한 레이트리밋(429 + Retry-After ~250s)이 있다.
//   → 표본이 큰 아키타입부터 우선 채집 + snapshot 단위 디스크 캐시로 실행할 때마다 누적.
//   기본은 429를 만나면 그 실행에선 중단(캐시에 쌓인 만큼만 반영). NINJA_MASTERY_WAIT=<분> 을 주면
//   그 시간 예산 안에서 Retry-After 만큼 기다렸다 계속(집중 채집용).
const MASTERY_SAMPLE_PER_GROUP = Number(process.env.NINJA_MASTERY_SAMPLE || 12);
async function enrichMasteries(snapshot, league, builds) {
	const groups = {};
	for (const b of builds) {
		if (b.ascendancy && b.mainSkill) (groups[`${b.ascendancy}|${b.mainSkill}`] ||= []).push(b);
	}
	// 대형 아키타입(표본 큰 것 = 벤치로 자주 쓰이는 것)부터 — 리밋에 걸려도 가치 높은 것부터 확보
	const targets = [];
	for (const arr of Object.values(groups).sort((a, b) => b.length - a.length)) {
		const eg = arr.filter((b) => (b.level ?? 0) >= LEVEL_ENDGAME);
		const pool = (eg.length >= ENDGAME_MIN_SAMPLE ? eg : arr).slice().sort((a, b) => (b.level ?? 0) - (a.level ?? 0));
		targets.push(...pool.slice(0, MASTERY_SAMPLE_PER_GROUP));
	}
	const uniq = [...new Map(targets.map((b) => [`${b.account}|${b.name}`, b])).values()];
	const cachePath = path.join(OUT_DIR, `chars-cache-${snapshot}.json`);
	let cache = {};
	try { cache = JSON.parse(fs.readFileSync(cachePath, "utf8")); } catch { /* 첫 실행 */ }
	const waitBudgetMs = Number(process.env.NINJA_MASTERY_WAIT || 0) * 60_000;
	const deadline = Date.now() + waitBudgetMs;
	let fetched = 0, hit = 0, fail = 0, stopped = false;
	for (const b of uniq) {
		const key = `${b.account}|${b.name}`;
		if (cache[key]) { b.masteries = cache[key]; hit++; continue; }
		if (stopped) continue;
		for (;;) {
			try {
				const u = `${BASE}/api/builds/${snapshot}/character?account=${encodeURIComponent(b.account)}&name=${encodeURIComponent(b.name)}&overview=${league}&type=0&timeMachine=`;
				const res = await fetch(u, { headers: { "User-Agent": UA, "Accept": "*/*" } });
				if (res.status === 429) {
					const retryS = Number(res.headers.get("retry-after") || 300);
					if (waitBudgetMs > 0 && Date.now() + retryS * 1000 < deadline) {
						console.log(`[ninja] 마스터리 채집 레이트리밋 — ${retryS}s 대기 후 계속 (진행 ${fetched + hit}/${uniq.length})`);
						await new Promise((s) => setTimeout(s, (retryS + 3) * 1000));
						continue;
					}
					stopped = true; // 이번 실행은 여기까지 — 다음 실행이 캐시 위에 이어서 채집
					break;
				}
				if (!res.ok) { fail++; break; }
				const j = await res.json();
				const ms = Array.isArray(j.masteries) ? j.masteries.map((m) => m.name).filter(Boolean) : [];
				b.masteries = ms;
				cache[key] = ms;
				fetched++;
				break;
			} catch { fail++; break; }
		}
	}
	fs.mkdirSync(OUT_DIR, { recursive: true });
	fs.writeFileSync(cachePath, JSON.stringify(cache));
	const covered = fetched + hit;
	console.log(`[ninja] 마스터리 채집: ${covered}/${uniq.length} 커버 (신규 ${fetched}, 캐시 ${hit}, 실패 ${fail}${stopped ? ", 레이트리밋 중단 — 재실행 시 이어서" : ""})`);
}

// ---------- 리그 단위 페치 (overview + per-skill 필터 확장) ----------
// 서버가 정렬별 top-100 만 주므로, overview 로 인기 스킬을 식별한 뒤 skills=<스킬> 필터로 스킬별 top-100 을
// 추가 수집한다(GET 서버측 필터 확인됨). 아키타입 표본이 스킬당 ~6개 → ~100개로 커진다.
async function fetchLeague(league) {
	const snapshot = await resolveSnapshot(league);
	const overviewBase = await fetchSearch(snapshot, league, null);
	const dicts = await fetchDictionaries(overviewBase.hashes);
	const gemDict = dicts.gem || [];

	// 중복 캐릭터는 버리지 않고 dpsBySkill 을 병합 — 같은 캐릭터가 RF 필터와 화염덫 필터 양쪽 top-100 에
	// 잡히면 {RF: 전용DPS, FireTrap: 전용DPS} 둘 다 확보된다(조합 벤치의 스킬별 DPS 근거).
	const byKey = new Map();
	const all = [];
	const add = (arr) => {
		for (const b of arr) {
			if (!b.name) continue;
			const key = `${b.league}|${b.name}`;
			const ex = byKey.get(key);
			if (ex) { Object.assign(ex.dpsBySkill, b.dpsBySkill); continue; }
			byKey.set(key, b);
			all.push(b);
		}
	};

	// 1) overview top-100
	add((await decodePair(snapshot, league, dicts, null)).builds);
	// 2) 인기 스킬(= overview 빌드의 메인 액티브 distinct) 별 top-100 확장 + 패싯(사이드바 집계) 캡처
	const skillFreq = {};
	for (const b of all) if (b.mainSkill) skillFreq[b.mainSkill] = (skillFreq[b.mainSkill] || 0) + 1;
	const targetSkills = Object.entries(skillFreq).sort((a, b) => b[1] - a[1]).slice(0, MAX_FILTER_SKILLS).map(([s]) => s);
	let expanded = 0;
	const skillFacets = {}; // 스킬 → 패싯(전 전직 통합 모집단)
	const ascFacets = {};   // "클래스|스킬" → 패싯(그 스킬의 최다 클래스 정밀 모집단)
	const classDict = dicts.class || [];
	for (const skill of targetSkills) {
		try {
			const before = all.length;
			const { builds: sb, search } = await decodePair(snapshot, league, dicts, skill);
			add(sb);
			expanded += all.length - before;
			skillFacets[skill] = resolveFacets(search, dicts);
			// 최다 클래스(전직)로 1회 더 — 사이드바와 동일한 (스킬×전직) 정밀 패싯. 표본 10 미만은 생략.
			const cf = (search.facets || []).find((f) => f.name === "class");
			const topC = cf ? cf.buckets.slice().sort((a, b) => b.c - a.c)[0] : null;
			const cls = topC && topC.c >= 10 ? classDict[topC.i] : null;
			if (cls) {
				try {
					const exact = await fetchSearch(snapshot, league, null, skill, cls);
					ascFacets[`${cls}|${skill}`] = resolveFacets(exact, dicts);
				} catch (e) { console.warn(`  facet ${cls}|${skill} 실패:`, e.message); }
			}
		} catch (e) { console.warn(`  skills=${skill} 실패:`, e.message); }
	}
	console.log(`[ninja] ${league} @${snapshot}: ${all.length} builds (overview + ${targetSkills.length}스킬 확장 +${expanded}, 패싯 ${Object.keys(skillFacets).length}스킬/${Object.keys(ascFacets).length}정밀, dict gem ${gemDict.length})`);
	await enrichMasteries(snapshot, league, all);
	return { snapshot, builds: all, skillFacets, ascFacets };
}

// ---------- 아키타입 집계 ----------
const _median = (arr) => { const s = arr.filter((v) => v != null && !Number.isNaN(v)).sort((a, x) => a - x); return s.length ? s[Math.floor(s.length / 2)] : 0; };

// 아키타입 목표치 산출에 쓰는 전 수치 컬럼(스샷 전 컬럼) → 캐릭터별 접근자.
// 이 필드들로 (a)캐릭터 단위 이상치 판정(전 컬럼 z-점수 종합) 후 (b)살아남은 캐릭터들의 중앙값을 목표치로 낸다.
// 즉 "값이 크게 벗어난 캐릭터를 통째로 걸러낸 뒤 중앙값" — 단순 전체 평균/전체 중앙값과 다르다.
const NUM_FIELDS = {
	level: (b) => b.level, life: (b) => b.life, energyShield: (b) => b.energyShield,
	ward: (b) => b.ward, mana: (b) => b.mana, ehp: (b) => b.ehp, dps: (b) => b.dps,
	lifeRegen: (b) => b.lifeRegen, itemRarity: (b) => b.itemRarity, movementSpeed: (b) => b.movementSpeed,
	fireRes: (b) => b.fireRes, coldRes: (b) => b.coldRes, lightningRes: (b) => b.lightningRes, chaosRes: (b) => b.chaosRes,
	armour: (b) => b.armour, evasion: (b) => b.evasion, block: (b) => b.block, spellBlock: (b) => b.spellBlock,
	spellDodge: (b) => b.spellDodge, suppress: (b) => b.suppress, phystakenas: (b) => b.physTakenAs,
	str: (b) => b.str, dex: (b) => b.dex, int: (b) => b.int,
	enduranceCharges: (b) => b.enduranceCharges, frenzyCharges: (b) => b.frenzyCharges, powerCharges: (b) => b.powerCharges,
	clusterJewels: (b) => b.clusterJewels, largeCluster: (b) => b.largeCluster, mediumCluster: (b) => b.mediumCluster, smallCluster: (b) => b.smallCluster,
	uniqueEquip: (b) => b.uniqueEquip, mirroredItems: (b) => b.mirroredItems, mirroredWeapons: (b) => b.mirroredWeapons, mirroredArmours: (b) => b.mirroredArmours,
	physicalMax: (b) => b.physicalMax, fireMax: (b) => b.fireMax, coldMax: (b) => b.coldMax,
	lightningMax: (b) => b.lightningMax, chaosMax: (b) => b.chaosMax, lowestMax: (b) => b.lowestMax,
};
const _topN = (counter, n) => Object.entries(counter).sort((a, x) => x[1] - a[1]).slice(0, n).map(([k, c]) => ({ name: k, count: c }));

// 엔드게임(96레벨+) 표본만으로 프로파일을 낸다. 저레벨 캐릭터는 저항/EHP가 미완성이라
// 아키타입 목표치를 끌어내려 시뮬레이션이 과소평가된다(치프틴 화염저항 90 미반영 사례).
// 상위 10%는 "특화 판단"이 애매하므로(방어/공격 축이 상충) 사용자 결정에 따라 레벨 절단으로 대체.
// 90+는 표본이 여전히 과다(중반 캐릭 다수 포함)라 96+로 상향(진성 엔드게임만).
// 다만 96+ 표본이 지나치게 적은 소수 아키타입은 노이즈를 피해 전체 표본으로 폴백.
const LEVEL_ENDGAME = 96;
const ENDGAME_MIN_SAMPLE = 5;
function endgameSubset(arr) {
	const hi = arr.filter((b) => (b.level ?? 0) >= LEVEL_ENDGAME);
	return hi.length >= ENDGAME_MIN_SAMPLE ? hi : arr;
}

// 캐릭터 단위 이상치 판정 임계(평균 절대 z). 이보다 크게 벗어난 캐릭터는 통째로 제거.
const OUTLIER_MEAN_Z = 1.75;

/**
 * 한 그룹(빌드 배열)의 프로파일.
 * 1) 엔드게임(96+) 부분집합에서 전 수치 컬럼의 코호트 평균·표준편차 계산.
 * 2) 캐릭터별 "평균 절대 z"(전 컬럼 표준편차 대비 편차의 평균)를 구해, 크게 벗어난 캐릭터를 통째로 제거.
 * 3) 살아남은 캐릭터들의 중앙값을 아키타입 목표치로 낸다.
 * → 사용자 요구: 단순 전체 평균/중앙값이 아니라, 값이 크게 다른 캐릭터를 걸러낸 뒤 중앙값.
 * base = 그룹 식별 필드({ascendancy?, mainSkill}).
 */
function groupProfile(fullArr, base) {
	const arr = endgameSubset(fullArr);
	// (1) 컬럼별 코호트 평균·표준편차
	const stat = {};
	for (const [k, f] of Object.entries(NUM_FIELDS)) {
		const vs = arr.map(f).filter((v) => v != null && !Number.isNaN(v));
		const mean = vs.length ? vs.reduce((a, x) => a + x, 0) / vs.length : 0;
		const vr = vs.length ? vs.reduce((a, x) => a + (x - mean) ** 2, 0) / vs.length : 0;
		stat[k] = { mean, std: Math.sqrt(vr) };
	}
	// (2) 캐릭터별 평균 절대 z → 임계 초과 캐릭터 제거(표본 너무 줄면 완화 폴백)
	const scored = arr.map((b) => {
		let sum = 0, n = 0;
		for (const [k, f] of Object.entries(NUM_FIELDS)) {
			const s = stat[k]; if (!s.std) continue;
			const v = f(b); if (v == null || Number.isNaN(v)) continue;
			sum += Math.abs((v - s.mean) / s.std); n++;
		}
		return { b, mz: n ? sum / n : 0 };
	});
	let kept = scored.filter((s) => s.mz <= OUTLIER_MEAN_Z).map((s) => s.b);
	const floor = Math.max(3, Math.ceil(scored.length * 0.5));
	if (kept.length < floor) kept = scored.slice().sort((a, b) => a.mz - b.mz).slice(0, floor).map((s) => s.b); // 과다 제거 방지
	// (3) 살아남은 캐릭터들의 중앙값
	const med = (f) => _median(kept.map(f));
	const coCnt = {}, keyCnt = {}, mastCnt = {};
	for (const b of kept) {
		for (const s of b.activeSkills) if (s !== base.mainSkill) coCnt[s] = (coCnt[s] || 0) + 1;
		for (const k of b.keystones) keyCnt[k] = (keyCnt[k] || 0) + 1;
		for (const m of b.masteries || []) mastCnt[m] = (mastCnt[m] || 0) + 1; // 샘플링된 캐릭터만 기여
	}
	return {
		...base, sample: kept.length, sampleTotal: fullArr.length, sampleEndgame: arr.length,
		medianLevel: med((b) => b.level),
		medianLife: med((b) => b.life), medianES: med((b) => b.energyShield),
		medianWard: med((b) => b.ward), medianMana: med((b) => b.mana),
		// DPS 는 **해당 스킬 전용 DPS**(dpsBySkill) 우선 — 수집 쿼리에 따라 b.dps 의미가 달라(overview=대표,
		// 타 스킬 필터=그 스킬 전용) 혼합 중앙값이 오염되는 것을 방지(화염덫 3.2M 가 RF 벤치에 섞이던 버그).
		medianEHP: med((b) => b.ehp), medianDPS: med((b) => b.dpsBySkill?.[base.mainSkill] ?? b.dps),
		medianLifeRegen: med((b) => b.lifeRegen),
		medianItemRarity: med((b) => b.itemRarity), medianMovementSpeed: med((b) => b.movementSpeed),
		medianFireRes: med((b) => b.fireRes), medianColdRes: med((b) => b.coldRes),
		medianLightningRes: med((b) => b.lightningRes), medianChaosRes: med((b) => b.chaosRes),
		medianArmour: med((b) => b.armour), medianEvasion: med((b) => b.evasion),
		medianBlock: med((b) => b.block), medianSpellBlock: med((b) => b.spellBlock),
		medianSpellDodge: med((b) => b.spellDodge), medianSuppress: med((b) => b.suppress),
		medianPhysTakenAs: med((b) => b.physTakenAs),
		medianStr: med((b) => b.str), medianDex: med((b) => b.dex), medianInt: med((b) => b.int),
		medianEnduranceCharges: med((b) => b.enduranceCharges), medianFrenzyCharges: med((b) => b.frenzyCharges),
		medianPowerCharges: med((b) => b.powerCharges),
		medianClusterJewels: med((b) => b.clusterJewels), medianLargeCluster: med((b) => b.largeCluster),
		medianMediumCluster: med((b) => b.mediumCluster), medianSmallCluster: med((b) => b.smallCluster),
		medianUniqueEquip: med((b) => b.uniqueEquip), medianMirroredItems: med((b) => b.mirroredItems),
		medianMirroredWeapons: med((b) => b.mirroredWeapons), medianMirroredArmours: med((b) => b.mirroredArmours),
		medianPhysicalMax: med((b) => b.physicalMax), medianFireMax: med((b) => b.fireMax),
		medianColdMax: med((b) => b.coldMax), medianLightningMax: med((b) => b.lightningMax),
		medianChaosMax: med((b) => b.chaosMax), medianLowestMax: med((b) => b.lowestMax),
		topCoSkills: _topN(coCnt, 8),
		topKeystones: _topN(keyCnt, 6),
		topMasteries: _topN(mastCnt, 8),
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
	const allSkillFacets = {};
	const allAscFacets = {};
	for (const league of LEAGUES) {
		try {
			const { snapshot, builds, skillFacets, ascFacets } = await fetchLeague(league);
			snapshots[league] = snapshot;
			all.push(...builds);
			Object.assign(allSkillFacets, skillFacets || {});
			Object.assign(allAscFacets, ascFacets || {});
		} catch (e) { console.error(`[ninja] ${league} 실패:`, e.message); }
	}
	// ⚠ 전 리그 실패(레이트리밋 등)로 빈 결과면 **기존 canonical 파일을 절대 덮어쓰지 않는다**.
	//   (실사고: 채집이 예산을 소진해 검색 API 가 429 → 0 builds 로 ninja-archetypes.json 이 비워짐)
	if (all.length === 0) {
		console.error("[ninja] 수집 0건 — canonical 파일 보존을 위해 중단(레이트리밋이면 잠시 후 재실행)");
		process.exit(2);
	}
	const archetypes = aggregate(all);
	const skillArchetypes = aggregateBySkill(all); // 스킬 단위 폴백(자동전직)
	// 패싯(사이드바 집계) 부착 — (전직|스킬) 정밀 패싯 우선, 없으면 스킬 통합 패싯 폴백.
	for (const a of archetypes) {
		a.facets = allAscFacets[`${a.ascendancy}|${a.mainSkill}`] ?? allSkillFacets[a.mainSkill] ?? null;
	}
	for (const a of skillArchetypes) {
		a.facets = allSkillFacets[a.mainSkill] ?? null;
	}
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
