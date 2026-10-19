// poe.ninja 화폐 시세(10-15 C336 — 제작 계산기 "가장 적은 비용"의 단가). 경제 리그는 index-state 의 첫 비하드코어 리그.
// 거래소(Currency Exchange) 개요가 전체 시세를 준다(stash 개요는 현재 리그에서 13종뿐 — 엑잘 · 카오스가 빠졌다, 10-15 실측).
// 짐승은 거래소 개요에 없어 stash 개요(type=Beast)에서 더한다(C457).
// 산출물: ~/.poe-gamedata/ninja/craft-prices.json { league, fetchedAt, chaos: { "Exalted Orb": 3.38, … } } — 값은 카오스 기준.
// 사용법: node fetch-ninja-prices.mjs   (실패해도 기존 파일을 지우지 않는다)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR } from "./paths.mjs";

const OUT = path.join(DATA_DIR, "ninja", "craft-prices.json");
const BASE = "https://poe.ninja/poe1/api";
const TYPES = ["Currency", "Essence", "Fossil", "Resonator"];

async function getJson(url) {
	const res = await fetch(url, { headers: { "User-Agent": "Mozilla/5.0 bluesky-poe-tools" } });
	if (!res.ok) throw new Error(`${res.status} ${url}`);
	return res.json();
}

try {
	const index = await getJson(`${BASE}/data/index-state`);
	const league = (index.economyLeagues || []).find((l) => !/hardcore/i.test(l.name))?.name;
	if (!league) throw new Error("경제 리그를 못 찾음");
	const chaos = { "Chaos Orb": 1 };
	for (const type of TYPES) {
		try {
			const j = await getJson(`${BASE}/economy/exchange/current/overview?league=${encodeURIComponent(league)}&type=${type}`);
			const names = new Map((j.items || []).map((i) => [i.id, i.name]));
			for (const l of j.lines || []) {
				const name = names.get(l.id);
				if (name && l.primaryValue > 0) chaos[name] = l.primaryValue;
			}
		} catch (e) {
			console.warn(`[prices] ${type} 실패: ${e.message}`);
		}
	}
	// 짐승(C457 — 짐승 제작 레시피 단가 = 필요한 짐승 시세 합, API PoeCraftPriceService.BEAST_OF_RECIPE). 거래소 개요엔 없어 stash 개요에서
	try {
		const j = await getJson(`${BASE}/economy/stash/current/item/overview?league=${encodeURIComponent(league)}&type=Beast`);
		let n = 0;
		for (const l of j.lines || []) {
			if (l.name && l.chaosValue > 0 && chaos[l.name] === undefined) {
				chaos[l.name] = l.chaosValue;
				n++;
			}
		}
		console.log(`[prices] 짐승 ${n}종`);
	} catch (e) {
		console.warn(`[prices] 짐승 실패: ${e.message}`);
	}
	if (Object.keys(chaos).length < 10) throw new Error(`시세가 너무 적다(${Object.keys(chaos).length}) — 기존 파일 유지`);
	fs.mkdirSync(path.dirname(OUT), { recursive: true });
	fs.writeFileSync(OUT, JSON.stringify({ league, fetchedAt: new Date().toISOString(), chaos }));
	console.log(`[prices] ${league}: ${Object.keys(chaos).length}종 → ${OUT}`);
} catch (e) {
	console.warn(`[prices] 실패(기존 파일 유지): ${e.message}`);
}
