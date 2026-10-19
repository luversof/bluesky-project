// poe.ninja PoE2 화폐 시세(10-15 C352 — PoE2 제작 시뮬레이터 단가). 경제 리그 = index-state 의 첫 "indexed" 비하드코어 리그.
// 거래소 개요 Currency · Essences · Ritual(징조) · Abyss(뼈) · Breach(기폭제, C386) — 값은 디바인 오브 기준(primaryValue).
// 산출물: ~/.poe-gamedata/poe2/ninja/craft-prices2.json { league, fetchedAt, unit: "Divine Orb", divine: { "Exalted Orb": 0.0019, … } }
// 사용법: node fetch-ninja-prices2.mjs   (실패해도 기존 파일을 지우지 않는다)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR } from "./paths.mjs";

const OUT = path.join(DATA_DIR, "ninja", "craft-prices2.json");
const BASE = "https://poe.ninja/poe2/api";
const TYPES = ["Currency", "Essences", "Ritual", "Abyss", "Breach"]; // Breach = 기폭제 26종(C386)

async function getJson(url) {
	const res = await fetch(url, { headers: { "User-Agent": "Mozilla/5.0 bluesky-poe-tools" } });
	if (!res.ok) throw new Error(`${res.status} ${url}`);
	return res.json();
}

try {
	const index = await getJson(`${BASE}/data/index-state`);
	const leagues = index.economyLeagues || [];
	const league = (leagues.find((l) => l.indexed && !l.hardcore) || leagues.find((l) => !l.hardcore))?.name;
	if (!league) throw new Error("경제 리그를 못 찾음");
	const divine = { "Divine Orb": 1 };
	for (const type of TYPES) {
		try {
			const j = await getJson(`${BASE}/economy/exchange/current/overview?league=${encodeURIComponent(league)}&type=${type}`);
			const names = new Map((j.items || []).map((i) => [i.id, i.name]));
			for (const l of j.lines || []) {
				const name = names.get(l.id);
				if (name && l.primaryValue > 0) divine[name] = l.primaryValue;
			}
		} catch (e) {
			console.warn(`[prices2] ${type} 실패: ${e.message}`);
		}
	}
	if (Object.keys(divine).length < 10) throw new Error(`시세가 너무 적다(${Object.keys(divine).length}) — 기존 파일 유지`);
	fs.mkdirSync(path.dirname(OUT), { recursive: true });
	fs.writeFileSync(OUT, JSON.stringify({ league, fetchedAt: new Date().toISOString(), unit: "Divine Orb", divine }));
	console.log(`[prices2] ${league}: ${Object.keys(divine).length}종 → ${OUT}`);
} catch (e) {
	console.warn(`[prices2] 실패(기존 파일 유지): ${e.message}`);
}
