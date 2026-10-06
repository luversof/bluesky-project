// 실빌드 구성 분포(poe.ninja 패싯) 이름 사전 — 판테온 신 · 산적처럼 우리 다른 데이터엔 한글 원본이 없는 이름을 게임 테이블에서(10-03 C59).
//   PantheonPanelLayout(GodName1~4) · NPCs(Name · ShortName)의 영문 ↔ 한글을 같은 행 번호로 짝지어 facet-names-ko.json 으로 쓴다.
//   API PoeOptimizeService.facetNameKoMap 이 이 파일을 읽는다(없으면 건너뜀 — 영문 유지).
// 사용법: node parse-facet-names.mjs   (extract 뒤, run-all 이 부른다)
import fs from "node:fs";
import path from "node:path";
import { DATA_DIR, loadTable } from "./paths.mjs";

const OUT = path.join(DATA_DIR, "facet-names-ko.json");
const out = {};
let pairs = 0;
const add = (en, ko) => {
	if (!en || !ko || en === ko || /[가-힣]/.test(en) || !/[가-힣]/.test(ko)) return;
	if (out[en] && out[en] !== ko) return; // 같은 영문에 다른 한글이 오면 처음 것(판테온이 NPC 보다 먼저)
	if (!out[en]) pairs++;
	out[en] = ko;
};
const pairTables = (table, columns) => {
	let en, ko;
	try {
		en = loadTable("English", table);
		ko = loadTable("Korean", table);
	} catch (e) {
		console.warn(`${table} 테이블 없음 — 건너뜀(config.json 에 넣고 extract 필요)`);
		return;
	}
	if (en.length !== ko.length) {
		console.warn(`${table}: 영문 ${en.length}행 · 한글 ${ko.length}행 — 행 번호 짝이 안 맞아 건너뜀`);
		return;
	}
	for (let i = 0; i < en.length; i++) for (const c of columns) add(en[i][c], ko[i][c]);
};
pairTables("PantheonPanelLayout", ["GodName1", "GodName2", "GodName3", "GodName4"]);
// 신 이름 자체는 "Soul of Arakaali" / "아라칼리의 영혼" 안에만 있다 — 패싯은 "Arakaali" · "The Brine King" 으로 온다
try {
	const en = loadTable("English", "PantheonPanelLayout"), ko = loadTable("Korean", "PantheonPanelLayout");
	for (let i = 0; i < en.length && i < ko.length; i++) {
		const m = /^Soul of (.+)$/.exec(en[i].GodName1 || ""), k = /^(.+)의 영혼$/.exec(ko[i].GodName1 || "");
		if (m && k) add(m[1].charAt(0).toUpperCase() + m[1].slice(1), k[1]);
	}
} catch (e) {
	/* 위에서 이미 경고 */
}
pairTables("NPCs", ["Name", "ShortName"]);
fs.writeFileSync(OUT, JSON.stringify(out, null, 1));
console.log(`facet 이름 사전 ${pairs}개 → ${OUT}`);
for (const k of ["Arakaali", "Lunaris", "Solaris", "The Brine King", "Abberath", "Ralakesh", "Alira", "Kraityn", "Oak", "Eramir"]) console.log(`  ${k} → ${out[k] ?? "(없음)"}`);
