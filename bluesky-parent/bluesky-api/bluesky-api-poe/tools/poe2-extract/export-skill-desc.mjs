// 스킬 전용 스탯 설명(.csd) — ActiveSkills.StatDescription 이 가리키는 파일 + 같은 폴더의 statset_N(추가 스탯 묶음).
// 젬 레벨별 문장(parse-gems2)이 "공용 체인 + 이 스킬 전용 파일" 순으로 서술하는 데 쓴다. 작업물: work/files/<소문자 경로 @>
import fs from "node:fs";
import path from "node:path";
import { FILES_DIR, loadTable, openLoader } from "./paths.mjs";

import { localName } from "./export-skill-desc-name.mjs";

const actives = loadTable("English", "ActiveSkills");
const wanted = new Set();
for (const a of actives) {
	const p = a.StatDescription;
	if (!p) continue;
	wanted.add(p);
	// 폴더형(statset_0.csd)이면 추가 스탯 묶음 파일도(없으면 받기에서 빠진다)
	const m = p.match(/^(.*\/)statset_\d+\.csd$/i);
	if (m) for (let i = 0; i < 6; i++) wanted.add(`${m[1]}statset_${i}.csd`);
}
fs.mkdirSync(FILES_DIR, { recursive: true });
const loader = await openLoader();
let ok = 0, reused = 0, missing = 0;
for (const p of wanted) {
	const out = path.join(FILES_DIR, localName(p));
	if (fs.existsSync(out)) {
		reused++;
		continue;
	}
	const buf = await loader.get(p);
	if (!buf) {
		missing++;
		continue;
	}
	fs.writeFileSync(out, buf);
	ok++;
}
console.log(`[poe2 skill desc] 받음 ${ok} · 있던 것 ${reused} · 없음 ${missing}(추가 스탯 묶음 후보 포함) / 후보 ${wanted.size}`);
