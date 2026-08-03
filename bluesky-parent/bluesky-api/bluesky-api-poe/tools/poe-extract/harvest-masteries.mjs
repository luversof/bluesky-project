// poe.ninja 마스터리 전용 채집기 — 기존 ninja-builds.json 을 읽어 캐릭터 상세 JSON(masteries)을
// chars-cache-<snapshot>.json 에 누적한다. 검색 API 를 전혀 안 써서(예산 공유) 레이트리밋 예산을
// 캐릭터 요청에만 쓴다. 캐시가 찬 뒤 fetch-ninja-builds.mjs 를 재실행하면 topMasteries 가 집계된다.
//
// 사용: node harvest-masteries.mjs [대기예산(분), 기본 40]
//   창(약 4~5분)당 ~55요청 한도 → 429 를 만나면 Retry-After 만큼 기다렸다 계속(예산 내에서).
//   표본 큰 아키타입(자주 조회되는 벤치)부터 채집한다. 중단해도 캐시는 보존 — 재실행 시 이어서.
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR } from "./paths.mjs";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) poe-gamedata-fetcher";
const BASE = "https://poe.ninja/poe1";
const OUT_DIR = path.join(DATA_DIR, "ninja");
const LEVEL_ENDGAME = 96;
const ENDGAME_MIN_SAMPLE = 5;
const SAMPLE_PER_GROUP = 12;
const budgetMin = Number(process.argv[2] || 40);

const data = JSON.parse(fs.readFileSync(path.join(OUT_DIR, "ninja-builds.json"), "utf8"));
const league = data.leagues[0];
const snapshot = data.snapshots[league];
if (!snapshot || !data.builds.length) {
	console.error("ninja-builds.json 비었음 — fetch-ninja-builds.mjs 먼저 실행");
	process.exit(1);
}

// 대상: (전직|메인스킬) 그룹을 표본 크기 내림차순으로, 그룹당 96+ 레벨 상위 N 명
const groups = {};
for (const b of data.builds) {
	if (b.ascendancy && b.mainSkill) (groups[`${b.ascendancy}|${b.mainSkill}`] ||= []).push(b);
}
const targets = [];
for (const arr of Object.values(groups).sort((a, b) => b.length - a.length)) {
	const eg = arr.filter((b) => (b.level ?? 0) >= LEVEL_ENDGAME);
	const pool = (eg.length >= ENDGAME_MIN_SAMPLE ? eg : arr).slice().sort((a, b) => (b.level ?? 0) - (a.level ?? 0));
	targets.push(...pool.slice(0, SAMPLE_PER_GROUP));
}
const uniq = [...new Map(targets.map((b) => [`${b.account}|${b.name}`, b])).values()];

const cachePath = path.join(OUT_DIR, `chars-cache-${snapshot}.json`);
let cache = {};
try { cache = JSON.parse(fs.readFileSync(cachePath, "utf8")); } catch { /* 첫 실행 */ }

const deadline = Date.now() + budgetMin * 60_000;
let fetched = 0, fail = 0;
const pending = uniq.filter((b) => !cache[`${b.account}|${b.name}`]);
console.log(`[harvest] 대상 ${uniq.length}명, 캐시 ${uniq.length - pending.length}, 잔여 ${pending.length}, 예산 ${budgetMin}분`);

for (const b of pending) {
	if (Date.now() >= deadline) { console.log("[harvest] 예산 소진 — 중단(캐시 보존)"); break; }
	const key = `${b.account}|${b.name}`;
	for (;;) {
		let res;
		try {
			const u = `${BASE}/api/builds/${snapshot}/character?account=${encodeURIComponent(b.account)}&name=${encodeURIComponent(b.name)}&overview=${league}&type=0&timeMachine=`;
			res = await fetch(u, { headers: { "User-Agent": UA, "Accept": "*/*" } });
		} catch { fail++; break; }
		if (res.status === 429) {
			const retryS = Number(res.headers.get("retry-after") || 300) + 3;
			if (Date.now() + retryS * 1000 >= deadline) { console.log(`[harvest] 예산 내 재개 불가(retry ${retryS}s) — 중단`); fs.writeFileSync(cachePath, JSON.stringify(cache)); process.exit(0); }
			console.log(`[harvest] 429 — ${retryS}s 대기 (진행 ${fetched}, 잔여 ${pending.length - fetched - fail})`);
			fs.writeFileSync(cachePath, JSON.stringify(cache)); // 창마다 캐시 저장(중단 대비)
			await new Promise((s) => setTimeout(s, retryS * 1000));
			continue;
		}
		if (!res.ok) { fail++; break; }
		try {
			const j = await res.json();
			cache[key] = Array.isArray(j.masteries) ? j.masteries.map((m) => m.name).filter(Boolean) : [];
			fetched++;
		} catch { fail++; }
		break;
	}
}
fs.writeFileSync(cachePath, JSON.stringify(cache));
console.log(`[harvest] 완료: 신규 ${fetched}, 실패 ${fail}, 캐시 총 ${Object.keys(cache).length}/${uniq.length}`);
