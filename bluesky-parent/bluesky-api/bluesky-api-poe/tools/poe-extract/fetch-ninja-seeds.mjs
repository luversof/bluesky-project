// poe.ninja 실빌드 시드 — 아키타입(전직|메인스킬)마다 실빌드 여러 명을 우리 엔진으로 **같은 가정**(ninja-normalize)으로 재계산해,
// 생존이 성립하는 사람 중 DPS·EHP 중앙값에 가장 가까운 한 명의 정규화 PoB 코드를 시드로 저장한다.
//
// 왜: 최적화기(빈 빌드에서 탐욕 선택)는 실빌드 생존력의 원천인 **조합**(타임리스·Split Personality·Bound By Destiny 같은 주얼,
//   Aegis Aurora 블록 체계 등)에 도달하지 못한다 — balanced 결과 EHP 가 실빌드의 0.17~0.55x(09-30 poe1-optimizer-vs-real).
//   시드는 시뮬레이터가 "실빌드에서 출발한 빌드"를 보여 주고 가이드로 다듬는 출발점이다.
// 대표 1명(calibrate-archetypes)은 개인차로 흔들린다(RF 대표 0.05x) → 여러 명을 재서 중앙값 근처를 고른다.
//
// 사용: node fetch-ninja-seeds.mjs [상위 N=60] [아키타입당 인원=5]   (api-poe 가 40135 에 떠 있어야 함)
// 산출물(Config 는 병합 — mergeConfig): ~/.poe-gamedata/ninja/ninja-start-builds.json  { "전직|스킬": { code, name, account, level, dps, ehp, life, es, n, medianDps, medianEhp, … } }
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR } from "./paths.mjs";
import { alignMainGroup, decodePob, encodePob, mergeConfig, withEngineStats } from "./ninja-normalize.mjs";

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0"; // 로컬 api 자가서명 인증서

const UA = { headers: { "User-Agent": "Mozilla/5.0", Accept: "*/*" } };
const NINJA = "https://poe.ninja/poe1";
const API = "https://localhost:40135";
const OUT_DIR = path.join(DATA_DIR, "ninja");
const TOP_N = Number(process.argv[2] || 60); // 시뮬 폼 미리보기·결과 카드의 출발점 커버리지(PoE2 와 같게 60)
const PER = Number(process.argv[3] || 5);
const LEVEL_ENDGAME = 96;
const MIN_VIABLE_EHP = 5000; // 재계산에서 방어 축이 날아간 빌드(임포트 손실) 배제 — calibrate 와 같은 기준

const data = JSON.parse(fs.readFileSync(path.join(OUT_DIR, "ninja-builds.json"), "utf8"));
const league = data.leagues[0];
const snapshot = data.snapshots[league];

const groups = {};
for (const b of data.builds) {
	if (b.ascendancy && b.mainSkill) (groups[`${b.ascendancy}|${b.mainSkill}`] ||= []).push(b);
}
const targets = Object.entries(groups).sort((a, b) => b[1].length - a[1].length).slice(0, TOP_N);

const outPath = path.join(OUT_DIR, "ninja-start-builds.json");
let seeds = {};
try { seeds = JSON.parse(fs.readFileSync(outPath, "utf8")); } catch { /* 첫 실행 */ }
// 스냅샷이 바뀌었거나 Config 를 통째로 교체하던 옛 판(configMerged 없음 — 플레이어 설정이 지워진 코드라 되살릴 수 없다)이면 다시 뽑는다
for (const [k, v] of Object.entries(seeds)) if (!v || v.snapshot !== snapshot || !v.configMerged || !v.alignV2) delete seeds[k];

const statOf = (stats, key) => {
	const s = (stats || []).find((x) => x.key === key);
	return s ? Number(String(s.value).replace(/,/g, "").replace(/x/g, "")) : null;
};
const median = (a) => { const s = a.slice().sort((x, y) => x - y); return s.length ? s[Math.floor((s.length - 1) / 2)] : null; };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let waits = 0;
const RATE_MAX_WAITS = 12;

async function fetchChar(b) {
	const u = `${NINJA}/api/builds/${snapshot}/character?account=${encodeURIComponent(b.account)}&name=${encodeURIComponent(b.name)}&overview=${league}&type=0&timeMachine=`;
	let r = await fetch(u, UA);
	while (r.status === 429 && waits < RATE_MAX_WAITS) {
		waits++;
		const ra = Number(r.headers.get("retry-after"));
		const waitMs = Number.isFinite(ra) && ra > 0 ? Math.min(ra * 1000 + 5000, 300_000) : 65_000;
		console.log(`[seeds] 레이트리밋 — ${Math.round(waitMs / 1000)}초 대기 (${waits}/${RATE_MAX_WAITS})`);
		await sleep(waitMs);
		r = await fetch(u, UA);
	}
	if (r.status === 429) return { stop: true };
	if (!r.ok) return { error: `HTTP ${r.status}` };
	const j = await r.json();
	return { code: j.pathOfBuildingExport };
}

try { await fetch(`${API}/api/poe/build/engine/reset`, { method: "POST" }); } catch { /* 엔진 미가동이면 아래에서 걸린다 */ }

// 예전 판(저장 스탯 = 원래 캐릭터 값)으로 받은 항목은 ninja 재요청 없이 코드만 재계산해 저장 스탯을 바꾼다
let healed = 0;
for (const [key, v] of Object.entries(seeds)) {
	if (v.engineStats) continue;
	const rr = await fetch(`${API}/api/poe/build/recalculate`, {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: "code=" + encodeURIComponent(v.code),
	});
	if (!rr.ok) { console.warn(`[seeds] ${key}: 저장 스탯 갱신 실패 HTTP ${rr.status}`); continue; }
	const er = await rr.json();
	if (!(er.stats || []).length) continue;
	v.code = encodePob(withEngineStats(decodePob(v.code), er.stats));
	v.engineStats = true;
	healed++;
}
if (healed) { fs.writeFileSync(outPath, JSON.stringify(seeds, null, 1)); console.log(`[seeds] 저장 스탯을 엔진 값으로 갱신: ${healed}건`); }

let done = 0, skip = 0, fail = 0, stopped = false;
for (const [key, arr] of targets) {
	if (seeds[key]) { skip++; continue; }
	if (stopped) break;
	const skill = key.split("|")[1];
	const pool = arr.filter((b) => (b.level ?? 0) >= LEVEL_ENDGAME).sort((a, b) => (b.level ?? 0) - (a.level ?? 0)).slice(0, PER);
	const measured = [];
	for (const b of pool) {
		const f = await fetchChar(b);
		if (f.stop) { stopped = true; break; }
		if (!f.code) continue;
		// Config 는 병합 — 표준 가정만 덮고 플레이어의 운용 설정(삼위일체 공명·낙인 부착·판테온 …)은 살린다(ninja-normalize.mergeConfig)
		const { xml, mainIdx } = alignMainGroup(mergeConfig(decodePob(f.code)), skill, { preferSavedMain: true }); // 저장 메인 우선(벤치와 같은 규칙)
		if (mainIdx < 0) continue; // 그 스킬 그룹이 없는 캐릭터(발라만·다른 메인)는 그 아키타입을 대표하지 못한다
		const code = encodePob(xml);
		const rr = await fetch(`${API}/api/poe/build/recalculate`, {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body: "code=" + encodeURIComponent(code),
		});
		if (!rr.ok) continue;
		const er = await rr.json();
		const dps = statOf(er.stats, "combineddps") || statOf(er.stats, "fulldps") || 0;
		const ehp = statOf(er.stats, "totalehp") || 0;
		if (!dps || ehp < MIN_VIABLE_EHP) continue;
		// 저장 스탯도 재계산 값으로 — 빌드 화면 요약이 저장 스탯을 읽으므로, 안 바꾸면 그 사람 Config 로 계산된 옛 값이 보인다
		measured.push({ b, code: encodePob(withEngineStats(xml, er.stats)), dps, ehp, life: statOf(er.stats, "life"), es: statOf(er.stats, "energyshield"), netRegen: statOf(er.stats, "netliferegen") });
	}
	if (!measured.length) { fail++; console.warn(`[seeds] ${key}: 쓸 수 있는 실빌드 없음(${pool.length}명 조회)`); continue; }
	// 대표 = 로그 공간에서 (DPS, EHP) 중앙값에 가장 가까운 사람 — 한 축만 튄 개인(유리대포·탱커)을 피한다
	const mDps = median(measured.map((m) => m.dps)), mEhp = median(measured.map((m) => m.ehp));
	const dist = (m) => Math.hypot(Math.log(m.dps / mDps), Math.log(m.ehp / mEhp));
	const rep = measured.slice().sort((x, y) => dist(x) - dist(y))[0];
	seeds[key] = {
		league, snapshot,
		name: rep.b.name, account: rep.b.account, level: rep.b.level,
		dps: rep.dps, ehp: rep.ehp, life: rep.life, es: rep.es, netRegen: rep.netRegen,
		n: measured.length, medianDps: mDps, medianEhp: mEhp,
		code: rep.code,
		engineStats: true,
		configMerged: true,
		alignV2: true,
	};
	done++;
	console.log(`[seeds] ${key}: ${rep.b.name} L${rep.b.level} dps=${Math.round(rep.dps)} ehp=${Math.round(rep.ehp)} (측정 ${measured.length}명, 중앙 dps=${Math.round(mDps)} ehp=${Math.round(mEhp)})`);
	fs.writeFileSync(outPath, JSON.stringify(seeds, null, 1)); // 아키타입마다 저장 — 레이트리밋으로 끊겨도 다음 실행이 이어서
}
fs.writeFileSync(outPath, JSON.stringify(seeds, null, 1));
console.log(`[seeds] 완료: 신규 ${done}, 캐시 ${skip}, 실패 ${fail}${stopped ? ", 레이트리밋으로 중단(다음 실행에서 이어서)" : ""} → ${outPath} (총 ${Object.keys(seeds).length})`);
