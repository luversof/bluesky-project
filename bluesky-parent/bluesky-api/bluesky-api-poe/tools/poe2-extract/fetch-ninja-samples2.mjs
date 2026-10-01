// poe.ninja PoE2 실빌드 표본 — 직업이 겹치지 않게 몇 명 골라 캐릭터 상세의 pathOfBuildingExport(PoB2 코드)를 받는다.
// 용도: 빌드 화면(/poe2/build) 검증용 실제 형식 표본(QA). 산출물: ~/.bluesky-qa/logs/poe2-real-<직업>.pob.txt
// 사용: node fetch-ninja-samples2.mjs [리그] [개수]   (리그 생략 시 스냅샷이 가장 최신인 SC 리그)
// 프로토콜은 PoE1 페처(tools/poe-extract/fetch-ninja-builds.mjs)와 같다 — BASE 만 /poe2.
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const BASE = "https://poe.ninja/poe2";
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) poe-gamedata-fetcher";
const OUT = path.join(os.homedir(), ".bluesky-qa", "logs");
const [leagueArg, countArg] = process.argv.slice(2);
const WANT = Number(countArg || 6);

function readVarint(b, pos) {
	let result = 0n, shift = 0n, p = pos;
	for (;;) {
		const byte = b[p++];
		result |= BigInt(byte & 0x7f) << shift;
		if ((byte & 0x80) === 0) break;
		shift += 7n;
	}
	return [result, p];
}
function walk(bb) {
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
const unpack = (bb) => { const out = []; let p = 0; while (p < bb.length) { const [v, q] = readVarint(bb, p); out.push(Number(v)); p = q; } return out; };
async function get(url, text = false) {
	const res = await fetch(url, { headers: { "User-Agent": UA, Accept: "*/*" } });
	if (!res.ok) throw new Error(`HTTP ${res.status} ${url}`);
	return text ? res.text() : Buffer.from(await res.arrayBuffer());
}

// 리그·스냅샷은 index-state(JSON)의 snapshotVersions 에서 — 처음 판은 /builds HTML 을 정규식 [a-z0-9]+ 로 긁어서
// 하이픈 든 현 리그(forbidden-rites)를 못 잡고 옛 리그(allflame)만 남아 search 가 404 → "poe.ninja 가 PoE2 캐릭터를 안 준다"로 오판했다(09-30).
const state = JSON.parse(await get(`${BASE}/api/data/index-state`, true));
const snaps = {};
for (const s of state.snapshotVersions || []) {
	const name = s.snapshotName;
	if (!name || /(hc|ssf)$/.test(name) || /hardcore|standard|ruthless/.test(name)) continue;
	snaps[name] = { version: s.version, date: (s.version.match(/-(\d{8})-/) || [])[1] || "" };
}
// 기본 = 빌드 리그 목록 맨 앞(현 리그)
const current = (state.buildLeagues || []).map((l) => (state.snapshotVersions || []).find((s) => s.url === l.url)?.snapshotName).find((n) => n && snaps[n]);
const league = leagueArg || current || Object.entries(snaps).sort((a, b) => b[1].date.localeCompare(a[1].date))[0]?.[0];
if (!league || !snaps[league]) throw new Error("리그를 못 찾았다: " + JSON.stringify(Object.keys(snaps)));
const snapshot = snaps[league].version;
console.log(`[poe2 ninja] 리그 ${league} · 스냅샷 ${snapshot} (후보 ${Object.keys(snaps).join(", ")})`);

// search → name / account / class 열(개편 후 field#12, 이전 field#5 모두)
const buf = await get(`${BASE}/api/builds/${snapshot}/search?overview=${league}`);
const [, p0] = readVarint(buf, 0);
const [l0, p1] = readVarint(buf, p0);
const top = walk(buf.subarray(p1, p1 + Number(l0)));
const cols = {};
for (const t of top.filter((x) => (x.fn === 5 || x.fn === 12) && x.w === 2)) {
	const parts = walk(t.sub);
	const nameField = parts.find((p) => p.fn === 1 && p.w === 2 && /^[a-z][\x20-\x7e]*$/.test(p.sub.toString("utf8")));
	if (!nameField) continue;
	const col = nameField.sub.toString("utf8");
	if (t.fn === 12) {
		const strs = parts.filter((p) => p.fn === 7 && p.w === 2);
		const packed = parts.find((p) => p.fn === 6 && p.w === 2);
		cols[col] = strs.length ? strs.map((p) => p.sub.toString("utf8")) : packed ? unpack(packed.sub) : [];
	} else {
		cols[col] = parts.filter((p) => p.fn === 2 && p.w === 2).map((r) => {
			const inner = walk(r.sub);
			const s = inner.find((x) => x.fn === 1 && x.w === 2);
			if (s) return s.sub.toString("utf8");
			const sc = inner.find((x) => x.fn === 2 && x.w === 0);
			return sc ? Number(sc.v) : null;
		});
	}
}
const names = cols.name || [], accounts = cols.account || [], classes = cols.class || [];
console.log(`  캐릭터 ${names.length}명`);
fs.mkdirSync(OUT, { recursive: true });
const seenClass = new Set();
let saved = 0;
for (let i = 0; i < names.length && saved < WANT; i++) {
	if (seenClass.has(classes[i])) continue;
	const url = `${BASE}/api/builds/${snapshot}/character?account=${encodeURIComponent(accounts[i])}&name=${encodeURIComponent(names[i])}&overview=${league}&timeMachine=`;
	let json;
	try {
		json = JSON.parse(await get(url, true));
	} catch (e) {
		console.log(`  ${names[i]}: ${e.message}`);
		if (/429/.test(e.message)) break;
		continue;
	}
	const code = json.pathOfBuildingExport;
	if (!code) continue;
	seenClass.add(classes[i]);
	const cls = (json.class || json.ascendancyClassName || "unknown").toString().replace(/[^A-Za-z]+/g, "");
	const file = path.join(OUT, `poe2-real-${cls.toLowerCase()}.pob.txt`);
	fs.writeFileSync(file, code.trim());
	saved++;
	console.log(`  ✔ ${names[i]} (${json.class || "?"} Lv ${json.level ?? "?"}) → ${file} (${code.length}자)`);
}
console.log(`[poe2 ninja] 표본 ${saved}개`);
