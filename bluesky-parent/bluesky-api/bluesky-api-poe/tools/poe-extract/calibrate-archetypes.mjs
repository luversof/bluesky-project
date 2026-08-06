// 아키타입 엔진 벤치 캘리브레이션 — 상위 아키타입별 대표(96+ 최고레벨) 캐릭터의 pathOfBuildingExport 를
// 로컬 api-poe 엔진(/api/poe/build/recalculate)으로 재계산해 **엔진 지표 기준 정답값**(dps/ehp/netRegen/life)을
// ninja-engine-bench.json 에 저장한다. ninja 표기 지표(gross lifeRegen 등)와 PoB 지표의 불일치를 우회해
// belowMeta 판정·비교를 지표 정합으로 만든다(실측: ninja lifeRegen 2,085 ↔ PoB net 1,176).
//
// 사용: node calibrate-archetypes.mjs [상위 N, 기본 12]   (api-poe 가 40135 에 떠 있어야 함)
// ⚠ 캐릭터 상세 엔드포인트는 창당 ~60요청 레이트리밋(429 시 중단·캐시 보존, 재실행 시 이어서).
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR } from "./paths.mjs";

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0"; // 로컬 api 자가서명 인증서

const UA = { headers: { "User-Agent": "Mozilla/5.0", "Accept": "*/*" } };
const NINJA = "https://poe.ninja/poe1";
const API = "https://localhost:40135";
const OUT_DIR = path.join(DATA_DIR, "ninja");
const TOP_N = Number(process.argv[2] || 12);
const LEVEL_ENDGAME = 96;

const data = JSON.parse(fs.readFileSync(path.join(OUT_DIR, "ninja-builds.json"), "utf8"));
const league = data.leagues[0];
const snapshot = data.snapshots[league];

// (전직|메인스킬) 그룹 → 표본 내림차순 상위 N, 그룹당 대표 1명(96+ 중 **중앙값 레벨**).
// 최고 레벨 대표는 개인 이상치가 된다(실측: Penance Brand L100 대표가 억제 100% 인데 모집단 중앙값은
// 억제 10/주문막기 78 — 모집단과 다른 방어 체계). 중앙값 대표가 belowMeta 기준으로 공정하다.
const groups = {};
for (const b of data.builds) {
	if (b.ascendancy && b.mainSkill) (groups[`${b.ascendancy}|${b.mainSkill}`] ||= []).push(b);
}
const median96 = (arr) => {
	const eg = arr.filter((b) => (b.level ?? 0) >= LEVEL_ENDGAME);
	const pool = eg.length ? eg : arr;
	const sorted = pool.slice().sort((a, b) => (a.level ?? 0) - (b.level ?? 0));
	return sorted[Math.floor(sorted.length / 2)];
};
const isCi = (b) => (b.keystones || []).some((k) => /Chaos Inoculation/i.test(k));
const targets = Object.entries(groups)
	.sort((a, b) => b[1].length - a[1].length)
	.slice(0, TOP_N)
	.flatMap(([key, arr]) => {
		const out = [{ key, rep: median96(arr) }];
		// CI 혼재 아키타입(PB: CI 45/비CI 53, 생명 중앙값 1 vs 2,905)은 단일 대표가 서브그룹을 대표하지
		// 못한다 — 양쪽 다 5명 이상이면 서브그룹 대표를 별도 키(|ci, |life)로 캘리브레이션해 belowMeta 가
		// 우리 빌드 스타일(CI=생명1)과 같은 서브그룹과 비교하게 한다.
		const ci = arr.filter(isCi);
		const life = arr.filter((b) => !isCi(b));
		if (ci.length >= 5 && life.length >= 5) {
			out.push({ key: key + "|ci", rep: median96(ci) });
			out.push({ key: key + "|life", rep: median96(life) });
		}
		return out;
	});

const outPath = path.join(OUT_DIR, "ninja-engine-bench.json");
let bench = {};
try { bench = JSON.parse(fs.readFileSync(outPath, "utf8")); } catch { /* 첫 실행 */ }

const statOf = (stats, key) => {
	const s = (stats || []).find((x) => x.key === key);
	return s ? Number(String(s.value).replace(/,/g, "").replace(/x/g, "")) : null;
};

let done = 0, skip = 0, fail = 0, stopped = false;
for (const { key, rep } of targets) {
	// 대표가 바뀌면(선정 기준 변경 등) 캐시 무효 — 같은 스냅샷이라도 account/name 까지 일치해야 스킵
	if (bench[key]
		&& bench[key].snapshot === snapshot
		&& bench[key].account === rep.account
		&& bench[key].name === rep.name) { skip++; continue; }
	if (stopped) continue;
	try {
		const cu = `${NINJA}/api/builds/${snapshot}/character?account=${encodeURIComponent(rep.account)}&name=${encodeURIComponent(rep.name)}&overview=${league}&type=0&timeMachine=`;
		const cr = await fetch(cu, UA);
		if (cr.status === 429) { console.log(`[calibrate] 레이트리밋 — 중단(${done} 완료, 재실행 시 이어서)`); stopped = true; continue; }
		if (!cr.ok) { fail++; continue; }
		const cj = await cr.json();
		const code = cj.pathOfBuildingExport;
		if (!code) { fail++; continue; }
		// ⚠ Config 공정화 — 실빌드 export 의 Config 는 대개 **비어 있다**(비보스·무버프 PoB 기본값).
		// 우리 잡은 Pinnacle+충전+전투버프 가정이라 그대로 비교하면 벤치가 부풀거나(비보스 적 저항)
		// 꺼진다. 우리 buildXml 의 표준 가정과 동일한 Config 를 주입해 같은 조건으로 재계산한다
		// (판테온은 대표 것이 있으면 유지 — Config 전체 교체라 함께 소실되지만 영향 미미).
		const NORM_CONFIG = '<Config><Input name="enemyIsBoss" string="Pinnacle"/>'
			+ '<Input name="usePowerCharges" boolean="true"/><Input name="useFrenzyCharges" boolean="true"/>'
			+ '<Input name="useEnduranceCharges" boolean="true"/><Input name="buffOnslaught" boolean="true"/>'
			+ '<Input name="multiplierRage" number="30"/><Input name="buffFortify" boolean="true"/>'
			+ '<Input name="conditionEnemyShocked" boolean="true"/><Input name="conditionEnemyChilled" boolean="true"/>'
			+ '<Input name="conditionEnemyIgnited" boolean="true"/><Input name="conditionEnemyPoisoned" boolean="true"/>'
			+ '<Input name="conditionEnemyBleeding" boolean="true"/></Config>';
		const zlib = await import("node:zlib");
		let xml = zlib.inflateSync(Buffer.from(code.replace(/-/g, "+").replace(/_/g, "/"), "base64")).toString("utf8");
		if (/<Config>[\s\S]*?<\/Config>/.test(xml)) xml = xml.replace(/<Config>[\s\S]*?<\/Config>/, NORM_CONFIG);
		else if (/<Config\s*\/>/.test(xml)) xml = xml.replace(/<Config\s*\/>/, NORM_CONFIG);
		else xml = xml.replace("</PathOfBuilding>", NORM_CONFIG + "</PathOfBuilding>");
		const normCode = zlib.deflateSync(Buffer.from(xml, "utf8"), { level: 9 }).toString("base64").replace(/\+/g, "-").replace(/\//g, "_");
		const rr = await fetch(`${API}/api/poe/build/recalculate`, {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body: "code=" + encodeURIComponent(normCode),
		});
		if (!rr.ok) { fail++; continue; }
		const er = await rr.json();
		const entry = {
			snapshot,
			account: rep.account,
			name: rep.name,
			level: rep.level,
			dps: statOf(er.stats, "combineddps"),
			ehp: statOf(er.stats, "totalehp"),
			netRegen: statOf(er.stats, "netliferegen"),
			life: statOf(er.stats, "life"),
		};
		if (!entry.dps && !entry.ehp) { fail++; continue; } // 엔진이 못 읽은 코드(스킨/변형) — 저장 안 함
		bench[key] = entry;
		done++;
		console.log(`[calibrate] ${key}: dps=${entry.dps} ehp=${entry.ehp} netRegen=${entry.netRegen} (${rep.name})`);
	} catch (e) { fail++; console.warn(`[calibrate] ${key} 실패:`, e.message); }
}
fs.writeFileSync(outPath, JSON.stringify(bench, null, 1));
console.log(`[calibrate] 완료: 신규 ${done}, 캐시 ${skip}, 실패 ${fail} → ${outPath} (총 ${Object.keys(bench).length} 아키타입)`);
