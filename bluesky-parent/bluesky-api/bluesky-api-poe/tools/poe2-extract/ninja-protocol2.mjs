// poe.ninja 빌드 검색 프로토콜(PoE2) — 컬럼형 protobuf 검색 응답 + NDIC 사전.
// PoE1 페처(tools/poe-extract/fetch-ninja-builds.mjs)가 역공학한 형식과 같다(검색 field#12 열 단위, 사전 "NDIC"). 그 파일은 스크립트라
// 가져다 쓸 수 없어, PoE1 파이프라인을 건드리지 않고 필요한 해석부만 옮겼다(두 곳을 고칠 땐 같이 볼 것).
export const BASE2 = "https://poe.ninja/poe2";
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) poe-gamedata-fetcher";

export function readVarint(b, pos) {
	let result = 0n, shift = 0n, p = pos;
	for (;;) {
		const byte = b[p++];
		result |= BigInt(byte & 0x7f) << shift;
		if ((byte & 0x80) === 0) break;
		shift += 7n;
	}
	return [result, p];
}
export function walk(bb) {
	const out = [];
	let pos = 0;
	while (pos < bb.length) {
		const [tag, q] = readVarint(bb, pos);
		const fn = Number(tag >> 3n), w = Number(tag & 7n);
		if (fn === 0) break;
		let np = q, info;
		if (w === 0) { const [v, y] = readVarint(bb, q); info = { fn, w, v }; np = y; }
		else if (w === 2) { const [l, x] = readVarint(bb, q); const L = Number(l); info = { fn, w, sub: bb.subarray(x, x + L) }; np = x + L; }
		else if (w === 5) { info = { fn, w, v: BigInt(bb.readUInt32LE(q)) }; np = q + 4; }
		else if (w === 1) { info = { fn, w, v: bb.readBigUInt64LE(q) }; np = q + 8; }
		else break;
		out.push(info);
		pos = np;
	}
	return out;
}
const toSigned = (v) => (v >= (1n << 63n) ? Number(v - (1n << 64n)) : Number(v));
const unpack = (bb) => { const out = []; let p = 0; while (p < bb.length) { const [v, q] = readVarint(bb, p); out.push(Number(v)); p = q; } return out; };
const unpackSigned = (bb) => { const out = []; let p = 0; while (p < bb.length) { const [v, q] = readVarint(bb, p); out.push(toSigned(v)); p = q; } return out; };

export async function httpGet(url, asText = false) {
	const res = await fetch(url, { headers: { "User-Agent": UA, Accept: "*/*" } });
	if (!res.ok) { const e = new Error(`HTTP ${res.status} ${url}`); e.status = res.status; e.retryAfter = Number(res.headers.get("retry-after")); throw e; }
	return asText ? res.text() : Buffer.from(await res.arrayBuffer());
}

/** 현재 빌드 리그 — index-state 의 buildLeagues 맨 앞(SC). HTML 정규식은 하이픈 리그를 놓쳤다(09-30 "캐릭터가 없다" 오판). */
export async function currentLeague2() {
	const state = JSON.parse(await httpGet(`${BASE2}/api/data/index-state`, true));
	const snaps = {};
	for (const s of state.snapshotVersions || []) {
		const name = s.snapshotName;
		if (!name || /(hc|ssf)$/.test(name) || /hardcore|standard|ruthless/.test(name)) continue;
		snaps[name] = s.version;
	}
	const league = (state.buildLeagues || []).map((l) => (state.snapshotVersions || []).find((s) => s.url === l.url)?.snapshotName).find((n) => n && snaps[n]);
	if (!league) throw new Error("PoE2 빌드 리그를 못 찾았다: " + Object.keys(snaps).join(","));
	return { league, snapshot: snaps[league] };
}

/** 검색 한 번 — 열 이름 → 값 배열(문자열 · 스칼라 · 리스트), 차원 → 사전 해시, 전체 모집단 수. */
export async function fetchSearch2(snapshot, league, filterSkill, filterClass) {
	let url = `${BASE2}/api/builds/${snapshot}/search?overview=${league}`;
	if (filterSkill) url += `&skills=${encodeURIComponent(filterSkill)}`;
	if (filterClass) url += `&class=${encodeURIComponent(filterClass)}`;
	const buf = await httpGet(url);
	const [, p0] = readVarint(buf, 0);
	const [l0, p1] = readVarint(buf, p0);
	const top = walk(buf.subarray(p1, p1 + Number(l0)));
	const total = Number(top.find((x) => x.fn === 1 && x.w === 0)?.v ?? 0);
	const dimHash = {};
	for (const t of top.filter((x) => x.fn === 6 && x.w === 2)) {
		const parts = walk(t.sub);
		const nm = parts.find((p) => p.fn === 1 && p.w === 2)?.sub.toString("utf8");
		const h = parts.find((p) => p.fn === 2 && p.w === 2)?.sub.toString("utf8");
		if (nm && h) dimHash[nm] = h;
	}
	// field#2 = 패싯(사이트 좌측 사이드바 집계) — {1:이름, 2:차원, 3:버킷*}, 버킷 {1:사전 인덱스(생략=0), 2:카운트}. 카운트는 **전체 모집단** 기준.
	const facets = [];
	for (const t of top.filter((x) => x.fn === 2 && x.w === 2)) {
		const parts = walk(t.sub);
		const nm = parts.find((p) => p.fn === 1 && p.w === 2)?.sub.toString("utf8");
		const dim = parts.find((p) => p.fn === 2 && p.w === 2)?.sub.toString("utf8");
		if (!nm || !dim) continue;
		const buckets = parts.filter((p) => p.fn === 3 && p.w === 2).map((p) => {
			const inner = walk(p.sub);
			return { i: Number(inner.find((x) => x.fn === 1 && x.w === 0)?.v ?? 0), c: Number(inner.find((x) => x.fn === 2 && x.w === 0)?.v ?? 0) };
		});
		facets.push({ name: nm, dim, buckets });
	}
	const columns = {};
	let rowCount = 0;
	for (const t of top.filter((x) => (x.fn === 5 || x.fn === 12) && x.w === 2)) {
		const parts = walk(t.sub);
		const nameField = parts.find((p) => p.fn === 1 && p.w === 2 && /^[a-z][\x20-\x7e]*$/.test(p.sub.toString("utf8")));
		if (!nameField) continue;
		const col = nameField.sub.toString("utf8");
		const count = Number(parts.find((p) => p.fn === 13 && p.w === 0)?.v ?? 0);
		const strs = parts.filter((p) => p.fn === 7 && p.w === 2);
		const lists = parts.filter((p) => p.fn === 9 && p.w === 2);
		const packed = parts.find((p) => p.fn === 6 && p.w === 2);
		let values;
		if (strs.length) values = strs.map((p) => p.sub.toString("utf8"));
		// 리스트 범주는 행이 {f1: packed} 로 한 겹 더 감싸여 있다(벗기지 않으면 태그·길이가 값으로 섞인다 — PoE1 실측)
		else if (lists.length) values = lists.map((p) => { const w = walk(p.sub).find((x) => x.fn === 1 && x.w === 2); return unpack(w ? w.sub : p.sub); });
		else if (packed) values = unpackSigned(packed.sub);
		else values = new Array(count).fill(null);
		rowCount = Math.max(rowCount, count || values.length);
		columns[col] = values;
	}
	return { columns, rowCount, total, dimHash, facets };
}

/** NDIC 사전 — 길이 테이블 시작을 자기검증으로 찾는다(헤더 길이가 사전마다 다르다). 라벨은 바이트 길이라 Buffer 로 자른다. */
export function decodeNdic(b) {
	if (b.length < 16 || b.toString("latin1", 0, 4) !== "NDIC") return null;
	const count = b.readUInt32LE(12);
	if (!count || count > 200000) return null;
	for (let table = 16; table < Math.min(b.length, 8192); table++) {
		let p = table, ok = true, sum = 0;
		const lens = new Array(count);
		for (let i = 0; i < count; i++) {
			let r = 0, s = 0, guard = 0;
			for (;;) {
				if (p >= b.length || ++guard > 5) { ok = false; break; }
				const y = b[p++];
				r |= (y & 0x7f) << s;
				if ((y & 0x80) === 0) break;
				s += 7;
			}
			if (!ok) break;
			lens[i] = r;
			sum += r;
		}
		if (!ok || sum !== b.length - p) continue;
		const labels = [];
		for (const len of lens) { labels.push(b.subarray(p, p + len).toString("utf8")); p += len; }
		return labels;
	}
	return null;
}

export async function fetchDictionaries2(dimHash) {
	const dicts = {};
	for (const [dim, h] of Object.entries(dimHash || {})) {
		let buf;
		try { buf = await httpGet(`${BASE2}/api/builds/dictionary/${h}`); } catch { continue; }
		const labels = decodeNdic(buf);
		if (labels) dicts[dim] = labels;
	}
	return dicts;
}

/** "686k","2.9M","1.1B" → 수 */
export function parseAbbrev(s) {
	if (s == null || s === "-" || s === "") return 0;
	const m = String(s).match(/^([\d.]+)\s*([kKmMbB]?)$/);
	if (!m) return Number(s) || 0;
	return Math.round(parseFloat(m[1]) * ({ "": 1, k: 1e3, K: 1e3, m: 1e6, M: 1e6, b: 1e9, B: 1e9 }[m[2]] || 1));
}
