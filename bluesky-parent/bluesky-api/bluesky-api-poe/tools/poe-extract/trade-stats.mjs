// PoE 거래소 스탯 필터 id 추출 — 한국 서버(poe.game.daum.net)의 공개 스탯 사전을 받아
// {정규화 한글 텍스트 → stat id} 맵을 emit 한다. 시뮬 결과 레어의 ko 모드 라인을
// 거래소 검색 쿼리(q JSON)로 변환하는 데 쓴다(글로벌 pathofexile.com 은 Cloudflare 403 —
// stat id 는 서버 공통이라 daum 사전으로 충분).
// explicit 외에 pseudo(합산) 섹션도 emit — 생명력/저항 등은 시장 매물이 순수+하이브리드 여러 모드로
// 나뉘어 있어 explicit 단일 모드 min 검색은 T1 롤만 잡혀 매물이 거의 없다. pseudo_total_* 은
// 모드 합산 총량 기준이라 같은 min 으로도 구매 가능한 매물이 잡힌다.
// 정규화 = 숫자만 # 치환(± 부호는 보존 — trade 텍스트가 "+#" 리터럴이라 부호까지 치환하면 전 매칭 실패).
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR } from "./paths.mjs";

const norm = (s) => s.replace(/[0-9]+(\.[0-9]+)?/g, "#").replace(/\s+/g, " ").trim();

const res = await fetch("https://poe.game.daum.net/api/trade/data/stats", {
	headers: { "User-Agent": "Mozilla/5.0", "Accept": "application/json" },
});
if (!res.ok) {
	console.error(`[trade-stats] HTTP ${res.status} — 기존 파일 유지(soft-fail)`);
	process.exit(0);
}
const json = await res.json();
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
	console.error("[trade-stats] explicit 섹션 없음 — soft-fail");
	process.exit(0);
}
const pseudo = sectionMap("pseudo");
// implicit — 결합(Synthesis) 유니크(성운 등)의 "빌드 유효 임플리싯 보유" count 필터용
const implicit = sectionMap("implicit");
const out = path.join(DATA_DIR, "trade-stats.json");
fs.writeFileSync(out, JSON.stringify({ explicit, pseudo, implicit }, null, 0));
console.log(`[trade-stats] explicit ${Object.keys(explicit).length}건 + pseudo ${Object.keys(pseudo).length}건 + implicit ${Object.keys(implicit).length}건 → ${out}`);
