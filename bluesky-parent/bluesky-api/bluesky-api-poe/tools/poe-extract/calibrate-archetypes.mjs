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
// 아키타입별 ninja 표기 중앙 DPS — 재계산 결과의 신뢰도 판정 기준(자릿수 대조용)
const medianDpsByKey = (() => {
	try {
		const arch = JSON.parse(fs.readFileSync(path.join(OUT_DIR, "ninja-archetypes.json"), "utf8")).archetypes;
		const map = {};
		for (const a of Object.values(arch)) {
			if (a.ascendancy && a.mainSkill && a.medianDPS) map[`${a.ascendancy}|${a.mainSkill}`] = a.medianDPS;
		}
		return map;
	} catch {
		return {};
	}
})();
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

// 지난 리그 벤치는 버린다 — belowMeta 는 이 파일을 **1순위 기준**으로 쓰므로, 남겨두면 현재 리그 빌드를
// 지난 리그 정답값과 조용히 비교하게 된다(리그 교체 시 사고). 리그가 다르면 ninja 표기 중앙값 폴백이 낫다.
for (const [k, v] of Object.entries(bench)) {
	if (v && v.league !== league) delete bench[k];
}

// 레이트리밋(429) 대응 — 데이터 갱신 **한 번**으로 끝나야 하므로 중단 대신 창이 열릴 때까지 기다렸다 재시도한다
// (직전 단계인 fetch-ninja-builds 의 마스터리 채집이 창을 소진한 채로 넘어오는 게 정상 경로다).
const RATE_WAIT_MS = 65_000;
const RATE_MAX_WAITS = 8; // 최대 ~9분 — 그래도 안 열리면 남은 건 다음 갱신에서 이어서
let waits = 0;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// 상주 PoB 워커는 기동 시 엔진 소스/트리를 메모리에 올려둔다. 이 파이프라인이 그 파일들을 갱신한 뒤라
// 워커가 살아 있으면 낡은 상태로 계산해 **예외 없이 빈 stats** 를 돌려준다(전건 실패했던 실사고).
// 재계산을 쓰기 전에 한 번 비운다. 실패해도 비치명(엔진 미가동이면 어차피 아래에서 걸린다).
try {
	const rs = await fetch(`${API}/api/poe/build/engine/reset`, { method: "POST" });
	console.log(`[calibrate] 엔진 리셋 ${rs.ok ? "완료" : "응답 " + rs.status}`);
} catch (e) {
	console.warn("[calibrate] 엔진 리셋 호출 실패 — 계속:", e.message);
}

let done = 0, skip = 0, fail = 0, emptyStats = 0, stopped = false;
for (const { key, rep } of targets) {
	// 대표가 바뀌면(선정 기준 변경 등) 캐시 무효 — 같은 스냅샷이라도 account/name 까지 일치해야 스킵
	if (bench[key]
		&& bench[key].snapshot === snapshot
		&& bench[key].account === rep.account
		&& bench[key].name === rep.name) { skip++; continue; }
	if (stopped) continue;
	try {
		const cu = `${NINJA}/api/builds/${snapshot}/character?account=${encodeURIComponent(rep.account)}&name=${encodeURIComponent(rep.name)}&overview=${league}&type=0&timeMachine=`;
		let cr = await fetch(cu, UA);
		while (cr.status === 429 && waits < RATE_MAX_WAITS) {
			waits++;
			// 서버가 Retry-After 로 남은 시간을 알려준다(약 250초) — 그걸 따르고, 없으면 기본 대기.
			const ra = Number(cr.headers.get("retry-after"));
			const waitMs = Number.isFinite(ra) && ra > 0 ? Math.min(ra * 1000 + 5000, 300_000) : RATE_WAIT_MS;
			console.log(`[calibrate] 레이트리밋 — ${Math.round(waitMs / 1000)}초 대기 후 재시도 (${waits}/${RATE_MAX_WAITS})`);
			await sleep(waitMs);
			cr = await fetch(cu, UA);
		}
		if (cr.status === 429) { console.log(`[calibrate] 레이트리밋 지속 — 중단(${done} 완료, 다음 갱신에서 이어서)`); stopped = true; continue; }
		if (!cr.ok) { fail++; console.warn(`[calibrate] ${key} 실패: 캐릭터 조회 HTTP ${cr.status}`); continue; }
		const cj = await cr.json();
		const code = cj.pathOfBuildingExport;
		if (!code) { fail++; console.warn(`[calibrate] ${key} 실패: export 없음`); continue; }
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
		// 메인 소켓 그룹을 **그 아키타입의 스킬**로 맞춘다.
		//   빌드가 저장해 둔 mainSocketGroup 은 오라·이동기 그룹인 경우가 흔하다(실측: RF 대표의 메인 그룹은
		//   Eternal Blessing+Malevolence 오라 그룹). 그대로 재계산하면 "대표 실빌드의 정의의 화염 DPS" 가 아니라
		//   엉뚱한 그룹 값이 벤치가 되어, 우리 결과와의 비교가 통째로 의미를 잃는다(아키타입별 0.06~19x 편차의 정체).
		//   같은 스킬을 담은 그룹 중 **발라(Vaal) 아닌** 것을 고른다(발라는 버스트라 지속 DPS 와 다른 축).
		const skillName = key.split("|")[1];
		const groups = [...xml.matchAll(/<Skill[^>]*>[\s\S]*?<\/Skill>/g)].map((m) => m[0]);
		// 이름이 딱 맞지 않는 경우가 흔하다: 발라 변종("Vaal Righteous Fire"), 변형젬("Reap of Butchery").
		//   그래서 ① 완전 일치 → ② 발라 아닌 포함 → ③ 포함 순으로 찾는다.
		let exactIdx = -1;
		let containsIdx = -1;
		let vaalIdx = -1;
		groups.forEach((g, i) => {
			const gems = [...g.matchAll(/nameSpec="([^"]+)"/g)].map((m) => m[1]);
			if (exactIdx < 0 && gems.some((n) => n === skillName)) exactIdx = i;
			for (const n of gems) {
				if (!n.includes(skillName)) continue;
				if (/^Vaal /.test(n)) {
					if (vaalIdx < 0) vaalIdx = i;
				} else if (containsIdx < 0) {
					containsIdx = i;
				}
			}
		});
		const mainIdx = exactIdx >= 0 ? exactIdx : containsIdx >= 0 ? containsIdx : vaalIdx;
		if (mainIdx >= 0) {
			xml = xml.replace(/(<Build[^>]*?)mainSocketGroup="\d+"/, `$1mainSocketGroup="${mainIdx + 1}"`);
		} else {
			console.warn(`[calibrate] ${key}: 스킬 그룹을 못 찾음 — 빌드 기본 메인 그룹으로 계산(비교 신뢰도 낮음)`);
		}
		const normCode = zlib.deflateSync(Buffer.from(xml, "utf8"), { level: 9 }).toString("base64").replace(/\+/g, "-").replace(/\//g, "_");
		const rr = await fetch(`${API}/api/poe/build/recalculate`, {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body: "code=" + encodeURIComponent(normCode),
		});
		if (!rr.ok) { fail++; console.warn(`[calibrate] ${key} 실패: 재계산 HTTP ${rr.status} — ${(await rr.text()).slice(0, 160)}`); continue; }
		const er = await rr.json();
		const entry = {
			league, // 리그 교체 시 지난 리그 벤치를 자동으로 버리기 위한 표식
			snapshot,
			account: rep.account,
			name: rep.name,
			level: rep.level,
			dps: statOf(er.stats, "combineddps"),
			ehp: statOf(er.stats, "totalehp"),
			netRegen: statOf(er.stats, "netliferegen"),
			life: statOf(er.stats, "life"),
			skillAligned: mainIdx >= 0, // 아키타입 스킬 그룹으로 맞춰 계산했는가
		};
		// 신뢰도 — 재계산값이 그 아키타입의 ninja 표기 중앙값과 자릿수가 맞는가.
		//   대표 1인의 빌드는 발라 버스트가 메인이거나(RF: Vaal RF 만 보유) 트리거 그룹이 잡히는 등
		//   엉뚱한 값이 나오기 쉽다(실측 편차 0.05~19x). 밴드를 벗어난 벤치는 belowMeta 판정에서 뺀다 —
		//   못 믿을 기준으로 "메타 하회" 를 찍으면 개선 방향 자체가 틀어진다.
		const med = medianDpsByKey[key.split("|").slice(0, 2).join("|")];
		if (med > 0 && entry.dps > 0) {
			const ratio = entry.dps / med;
			entry.medianDps = med;
			entry.ratio = Number(ratio.toFixed(2));
			entry.reliable = ratio >= 0.25 && ratio <= 4;
			if (!entry.reliable) {
				console.warn(`[calibrate] ${key}: 신뢰도 낮음(중앙값 대비 ${entry.ratio}x) — belowMeta 판정에서 제외`);
			}
		} else {
			entry.reliable = false;
		}
		// 엔진이 못 읽은 코드(스킨/변형) — 저장 안 함. stats 자체가 비면 개별 빌드 문제가 아니라 엔진 쪽이다.
		if (!entry.dps && !entry.ehp) { fail++; if (!(er.stats || []).length) emptyStats++; console.warn(`[calibrate] ${key} 실패: 유효 스탯 없음(stats ${(er.stats || []).length}개)`); continue; }
		bench[key] = entry;
		done++;
		console.log(`[calibrate] ${key}: dps=${entry.dps} ehp=${entry.ehp} netRegen=${entry.netRegen} (${rep.name})`);
	} catch (e) { fail++; console.warn(`[calibrate] ${key} 실패:`, e.message); }
}
fs.writeFileSync(outPath, JSON.stringify(bench, null, 1));
console.log(`[calibrate] 완료: 신규 ${done}, 캐시 ${skip}, 실패 ${fail} → ${outPath} (총 ${Object.keys(bench).length} 아키타입)`);
if (emptyStats) {
	console.warn(`[calibrate] ⚠ 재계산이 빈 stats 를 ${emptyStats}건 반환 — 엔진 쪽 문제다(상주 워커가 낡은 데이터를 물고 있거나 PoB 소스 손상).`
		+ " api-poe 재기동 또는 POST /api/poe/build/engine/reset 후 재실행.");
}
