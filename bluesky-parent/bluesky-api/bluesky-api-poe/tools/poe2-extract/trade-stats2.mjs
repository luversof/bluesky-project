// PoE2 거래소 스탯 필터 id · 리그 추출(10-08) — PoE1 trade-stats.mjs 짝. 한국 서버(poe.kakaogames.com — 옛 poe.game.daum.net 은 301)의
// 공개 PoE2 스탯 사전을 받아 {정규화 한글 텍스트 → stat id} 맵을 ~/.poe-gamedata/poe2/trade-stats.json 으로 쓴다. 빌드 화면 아이템의
// 한국어 옵션 줄을 거래소 검색 쿼리(q JSON)의 스탯 필터로 바꾸는 데 쓴다(API Poe2TradeStatDataService).
// PoE2 거래소 주소는 리그가 경로에 들어간다(/trade2/search/poe2/<리그>?q=) — 지금 리그를 함께 적는다(poe.ninja 데이터의 리그와 같은 것을 고른다).
// 정규화 = 숫자만 # 치환. PoE2 거래소 문구는 "+" 부호가 없다("생명력 최대치 #") — 조회 쪽이 "+#" → "#" 로 한 번 더 찾는다.
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR } from "./paths.mjs";

const HOST = "https://poe.kakaogames.com";
const norm = (s) => s.replace(/[0-9]+(\.[0-9]+)?/g, "#").replace(/\s+/g, " ").trim();
const get = async (p) => {
	const res = await fetch(HOST + p, { headers: { "User-Agent": "Mozilla/5.0", Accept: "application/json" } });
	if (!res.ok) throw new Error(`HTTP ${res.status} ${p}`);
	return res.json();
};

let json;
try {
	json = await get("/api/trade2/data/stats");
} catch (e) {
	console.error(`[trade-stats2] ${e.message} — 기존 파일 유지(soft-fail)`);
	process.exit(0);
}
const sectionMap = (id) => {
	const section = (json.result || []).find((s) => s.id === id);
	const map = {};
	for (const e of section?.entries || []) {
		const key = norm(e.text);
		if (!map[key]) map[key] = e.id; // 동일 텍스트 중복(로컬 변형 등)은 첫 항목 우선
	}
	return map;
};
const explicit = sectionMap("explicit");
if (!Object.keys(explicit).length) {
	console.error("[trade-stats2] explicit 섹션 없음 — soft-fail");
	process.exit(0);
}
const pseudo = sectionMap("pseudo");
const implicit = sectionMap("implicit");

// 지금 리그 — poe.ninja 데이터가 보는 리그(예: forbidden-rites)와 거래소 리그 id("Forbidden Rites")를 맞춘다. 못 맞추면 하드코어 · 스탠다드가 아닌 첫 리그
let league = null;
try {
	const leagues = (await get("/api/trade2/data/leagues")).result || [];
	let ninjaLeague = null;
	try {
		ninjaLeague = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "ninja", "ninja-builds2.json"), "utf8")).league || null;
	} catch {}
	const slug = (s) => String(s).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
	const pick =
		(ninjaLeague && leagues.find((l) => slug(l.id) === slug(ninjaLeague))) ||
		leagues.find((l) => !/hardcore|^hc |standard/i.test(l.id));
	league = pick ? pick.id : null;
} catch (e) {
	console.warn(`[trade-stats2] 리그 목록 실패(${e.message}) — 리그 없이(게이트가 Standard 로)`);
}

// 거래소가 아는 베이스 이름(10-08 C174, PoE1 trade-stats.mjs 짝) — 사전에 없는 베이스(룬벼림 · 고유 전용 · 미출시)는 베이스 상세 거래소 단추를 숨긴다
let types = [];
try {
	const d = await get("/api/trade2/data/items");
	types = [...new Set((d.result || []).flatMap((g) => (g.entries || []).map((e) => e.type)).filter(Boolean))].sort();
} catch (e) {
	console.warn(`[trade-stats2] 베이스 이름 목록 실패(${e.message}) — 빈 목록(단추는 그대로)`);
}

const out = path.join(DATA_DIR, "trade-stats.json");
fs.writeFileSync(out, JSON.stringify({ explicit, pseudo, implicit, league, types }, null, 0));
console.log(`[trade-stats2] explicit ${Object.keys(explicit).length}건 + pseudo ${Object.keys(pseudo).length}건 + implicit ${Object.keys(implicit).length}건 · 리그 ${league} · 베이스 ${types.length}건 → ${out}`);
