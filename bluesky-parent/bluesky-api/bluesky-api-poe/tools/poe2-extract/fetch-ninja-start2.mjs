// poe.ninja PoE2 실빌드 출발점 — 아키타입(전직|메인 스킬, fetch-ninja-builds2 산출)마다 실빌드 여러 명을 우리 PoE2 엔진으로 재계산해,
// 생존이 성립하는 사람 중 (DPS, EHP) 로그 중앙값에 가장 가까운 한 명의 PoB 코드를 저장한다(PoE1 fetch-ninja-seeds 의 PoE2 판).
//   · Config 는 **플레이어 것 그대로** — PoE2 엔 최적화기가 없어 맞출 "표준 가정"이 없고, 공명·충전 같은 운용 설정을 지우면 그 빌드를
//     과소평가한다(PoE1 에서 겪음). 메인 그룹만 아키타입 스킬로 맞추고, 그러면 저장 스탯이 달라지므로 엔진 계산값으로 바꿔 넣는다.
// 사용: node fetch-ninja-start2.mjs [상위 N=60] [아키타입당 인원=8]   (api-poe 가 40135 에 떠 있어야 함)
// 산출물(10-14 추가): ninja-archetype-members2.json(재던 전원) · ninja-archetype-consensus2.json(공통 특성 — archetype-consensus2.mjs)
// 산출물: ~/.poe-gamedata/poe2/ninja/ninja-start-builds2.json
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { alignMainGroup, decodePob, encodePob } from "../poe-extract/ninja-normalize.mjs";
import { BASE2, httpGet } from "./ninja-protocol2.mjs";
import { MEMBERS_PATH, writeConsensus } from "./archetype-consensus2.mjs";

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0"; // 로컬 api 자가서명 인증서
const API = "https://localhost:40135";
const DIR = path.join(os.homedir(), ".poe-gamedata", "poe2", "ninja");
const TOP_N = Number(process.argv[2] || 60); // 시뮬레이터 표의 "실빌드 출발" 칸이 비지 않게 — 레이트리밋으로 첫 수집은 ~20분, 이후엔 스냅샷이 바뀔 때만
// 아키타입당 인원 — 10-14 5 → 8: 재던 사람 전원을 공통 특성(archetype-consensus2)에 쓰므로 한 명이라도 더(레이트리밋과의 타협)
const PER = Number(process.argv[3] || 8);
const MIN_VIABLE_EHP = 3000;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const median = (a) => { const s = a.slice().sort((x, y) => x - y); return s.length ? s[Math.floor((s.length - 1) / 2)] : null; };

const arch = JSON.parse(fs.readFileSync(path.join(DIR, "ninja-archetypes2.json"), "utf8"));
const { builds, league, snapshot } = JSON.parse(fs.readFileSync(path.join(DIR, "ninja-builds2.json"), "utf8"));
const outPath = path.join(DIR, "ninja-start-builds2.json");
let starts = {};
try { starts = JSON.parse(fs.readFileSync(outPath, "utf8")); } catch { /* 첫 실행 */ }
// 메인 그룹 규칙이 바뀐 옛 판(alignV2 없음)은 다시 뽑는다 — 메타 젬 그룹이 잡혀 DPS 가 틀렸을 수 있다
for (const [k, v] of Object.entries(starts)) if (!v || v.snapshot !== snapshot || !v.alignV2) delete starts[k];
// 재던 실빌드 전원(10-14 사용자 요청 — 시뮬레이터가 한 명이 아니라 그 직업/스킬의 공통 특성을 쓰도록). 예전엔 대표 한 명만 남기고 버렸다
let members = { league, snapshot, archetypes: {} };
try { const m = JSON.parse(fs.readFileSync(MEMBERS_PATH, "utf8")); if (m.snapshot === snapshot) members = m; } catch { /* 첫 실행 */ }

const targets = Object.values(arch.archetypes)
	.filter((a) => a.medianDPS > 0)
	.sort((a, b) => b.sample - a.sample)
	.slice(0, TOP_N);

let waits = 0;
async function character(b) {
	const u = `${BASE2}/api/builds/${snapshot}/character?account=${encodeURIComponent(b.account)}&name=${encodeURIComponent(b.name)}&overview=${league}&timeMachine=`;
	for (;;) {
		try { return JSON.parse(await httpGet(u, true)); }
		catch (e) {
			if (e.status !== 429 || waits >= 12) return e.status === 429 ? { stop: true } : null;
			waits++;
			const w = Math.min((Number.isFinite(e.retryAfter) && e.retryAfter > 0 ? e.retryAfter : 60) * 1000 + 5000, 300_000);
			console.log(`[start2] 레이트리밋 — ${Math.round(w / 1000)}초 대기 (${waits}/12)`);
			await sleep(w);
		}
	}
}
async function recalc(code) {
	const r = await fetch(`${API}/api/poe2/build/recalc`, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: "code=" + encodeURIComponent(code) });
	if (!r.ok) return null;
	const j = await r.json();
	if (!j.rows) return null;
	return Object.fromEntries(j.rows.filter((x) => x.computed != null).map((x) => [x.key, x.computed]));
}
/** 저장 스탯(PlayerStat)을 엔진 계산값으로 — 있는 키만 값을 바꾼다(PoE2 요약·재계산 대조가 저장값을 기준으로 삼는다). */
function withComputedStats(xml, v) {
	return xml.replace(/<PlayerStat\b([^>]*)\/>/g, (tag, attrs) => {
		const stat = (attrs.match(/\bstat="([^"]+)"/) || [])[1];
		if (!stat || v[stat] == null || !Number.isFinite(v[stat])) return tag;
		return tag.replace(/\bvalue="[^"]*"/, `value="${v[stat]}"`);
	});
}

let done = 0, skip = 0, fail = 0, stopped = false;
for (const a of targets) {
	const key = `${a.ascendancy}|${a.mainSkill}`;
	// 멤버에 계정 · 캐릭터 이름이 없는 옛 판(10-14 C319 이전)은 다시 잰다 — 시뮬레이터가 그 이름으로 저장 코드를 찾아 poe.ninja 실시간 요청을 건너뛴다
	if (starts[key] && members.archetypes[key] && members.archetypes[key][0]?.account) { skip++; continue; }
	if (stopped) break;
	const pool = builds.filter((b) => b.ascendancy === a.ascendancy && b.mainSkill === a.mainSkill).sort((x, y) => (y.level ?? 0) - (x.level ?? 0)).slice(0, PER);
	const measured = [];
	for (const b of pool) {
		const c = await character(b);
		if (c && c.stop) { stopped = true; break; }
		const code = c && c.pathOfBuildingExport;
		if (!code) continue;
		const { xml, mainIdx } = alignMainGroup(decodePob(code), a.mainSkill, { preferSavedMain: true });
		if (mainIdx < 0) continue;
		const v = await recalc(encodePob(xml));
		if (!v) continue;
		const dps = v.CombinedDPS || v.FullDPS || 0, ehp = v.TotalEHP || 0;
		if (!(dps > 0) || ehp < MIN_VIABLE_EHP) continue;
		measured.push({ b, dps, ehp, life: v.Life, es: v.EnergyShield, code: encodePob(withComputedStats(xml, v)) });
	}
	if (!measured.length) { fail++; console.warn(`[start2] ${key}: 쓸 수 있는 실빌드 없음(${pool.length}명 조회)`); continue; }
	const mDps = median(measured.map((m) => m.dps)), mEhp = median(measured.map((m) => m.ehp));
	const dist = (m) => Math.hypot(Math.log(m.dps / mDps), Math.log(m.ehp / mEhp));
	const rep = measured.slice().sort((x, y) => dist(x) - dist(y))[0];
	members.archetypes[key] = measured.map((m) => ({ account: m.b.account, name: m.b.name, level: m.b.level, dps: m.dps, ehp: m.ehp, life: m.life, es: m.es, code: m.code }));
	fs.writeFileSync(MEMBERS_PATH, JSON.stringify(members));
	starts[key] = {
		league, snapshot, name: rep.b.name, level: rep.b.level,
		dps: rep.dps, ehp: rep.ehp, life: rep.life, es: rep.es,
		n: measured.length, medianDps: mDps, medianEhp: mEhp, sample: a.sample, lean: a.lean,
		code: rep.code,
		alignV2: true,
	};
	done++;
	console.log(`[start2] ${key}: L${rep.b.level} dps=${Math.round(rep.dps)} ehp=${Math.round(rep.ehp)} (측정 ${measured.length}명)`);
	fs.writeFileSync(outPath, JSON.stringify(starts, null, 1));
}
fs.writeFileSync(outPath, JSON.stringify(starts, null, 1));
console.log(`[start2] 공통 특성: 아키타입 ${writeConsensus()}개`);
console.log(`[start2] 완료: 신규 ${done}, 캐시 ${skip}, 실패 ${fail}${stopped ? ", 레이트리밋으로 중단(다음 실행에서 이어서)" : ""} → ${outPath} (총 ${Object.keys(starts).length})`);
