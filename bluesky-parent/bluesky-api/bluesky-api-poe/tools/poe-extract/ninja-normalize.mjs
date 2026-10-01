// poe.ninja 실빌드 PoB 코드 정규화 — 최적화기 결과와 **같은 조건**으로 재계산하기 위한 공용 모듈
// (calibrate-archetypes.mjs 엔진 벤치 · fetch-ninja-seeds.mjs 실빌드 시드).
//   ① Config 를 최적화기 buildXml 의 표준 가정(Pinnacle 보스·충전·전투 버프)으로 교체
//   ② 메인 소켓 그룹을 그 아키타입의 스킬 그룹으로 맞춤
import zlib from "node:zlib";

// 우리 buildXml 의 표준 가정과 같은 Config — 실빌드 Config 는 비보스·무버프이거나 개인 설정이라 그대로 견주면 벤치가 부풀거나 꺼진다.
export const NORM_CONFIG = '<Config><Input name="enemyIsBoss" string="Pinnacle"/>'
	+ '<Input name="usePowerCharges" boolean="true"/><Input name="useFrenzyCharges" boolean="true"/>'
	+ '<Input name="useEnduranceCharges" boolean="true"/><Input name="buffOnslaught" boolean="true"/>'
	+ '<Input name="multiplierRage" number="30"/><Input name="buffFortify" boolean="true"/>'
	+ '<Input name="conditionEnemyShocked" boolean="true"/><Input name="conditionEnemyChilled" boolean="true"/>'
	+ '<Input name="conditionEnemyIgnited" boolean="true"/><Input name="conditionEnemyPoisoned" boolean="true"/>'
	+ '<Input name="conditionEnemyBleeding" boolean="true"/></Config>';

export const decodePob = (code) => zlib.inflateSync(Buffer.from(code.trim().replace(/-/g, "+").replace(/_/g, "/"), "base64")).toString("utf8");
export const encodePob = (xml) => zlib.deflateSync(Buffer.from(xml, "utf8"), { level: 9 }).toString("base64").replace(/\+/g, "-").replace(/\//g, "_");

/** Config 교체. 요즘 export 는 <Config activeConfigSet="1"><ConfigSet …> 형식이라 속성 있는 여는 태그까지 잡는다. */
export function normalizeConfig(xml) {
	if (/<Config\b[^>]*>[\s\S]*?<\/Config>/.test(xml)) return xml.replace(/<Config\b[^>]*>[\s\S]*?<\/Config>/, NORM_CONFIG);
	if (/<Config\s*\/>/.test(xml)) return xml.replace(/<Config\s*\/>/, NORM_CONFIG);
	return xml.replace("</PathOfBuilding>", NORM_CONFIG + "</PathOfBuilding>");
}

/**
 * Config **병합** — 표준 가정 항목(NORM_CONFIG 에 든 이름)만 덮어쓰고, 플레이어가 켜 둔 나머지 설정(삼위일체 공명 수·낙인 부착 수·시듦 중첩·
 * 정지 상태·판테온 등 그 빌드의 운용)은 살린다. 통째 교체(normalizeConfig)는 이것까지 지워 그 기제를 쓰는 빌드를 과소평가했다 — 실측(2026-09-30):
 * 데드아이 번개 화살 실빌드에서 공명 수가 0 이 되어 삼위일체 보조가 "가장 약한 보조"로 잡히고 자동 다듬기가 그걸 빼는 교체(DPS +31.9%)를 골랐다.
 * 실빌드 출발점(fetch-ninja-seeds)용. 엔진 벤치(calibrate)는 판정 기준이 바뀌지 않게 교체 방식을 유지한다.
 * 활성 ConfigSet 의 Input 만 가져온다(Placeholder 는 PoB 가 기본값을 보여 주는 칸이라 설정이 아니다).
 */
export function mergeConfig(xml) {
	const block = (xml.match(/<Config\b[^>]*>[\s\S]*?<\/Config>/) || [])[0];
	if (!block) return normalizeConfig(xml);
	const active = (block.match(/<Config\b[^>]*\bactiveConfigSet="([^"]*)"/) || [])[1];
	let scope = block;
	if (/<ConfigSet\b/.test(block)) {
		const sets = [...block.matchAll(/<ConfigSet\b([^>]*)>([\s\S]*?)<\/ConfigSet>/g)];
		const pick = sets.find((m) => (m[1].match(/\bid="([^"]*)"/) || [])[1] === active) || sets[0];
		scope = pick ? pick[2] : "";
	}
	const normNames = new Set([...NORM_CONFIG.matchAll(/\bname="([^"]+)"/g)].map((m) => m[1]));
	const kept = [...scope.matchAll(/<Input\b[^>]*\/>/g)]
		.map((m) => m[0])
		.filter((tag) => !normNames.has((tag.match(/\bname="([^"]+)"/) || [])[1]));
	const merged = NORM_CONFIG.replace("</Config>", kept.join("") + "</Config>");
	return xml.replace(block, merged);
}

// 빌드 요약(PoePobImportService.STAT_KEYS)이 읽는 PlayerStat 이름 → 재계산 API stats 키
const PLAYER_STAT_KEYS = {
	CombinedDPS: "combineddps", TotalDPS: "totaldps", AverageDamage: "averagedamage", Life: "life", EnergyShield: "energyshield",
	Mana: "mana", Armour: "armour", Evasion: "evasion", TotalEHP: "totalehp", FireResist: "fireresist", ColdResist: "coldresist",
	LightningResist: "lightningresist", ChaosResist: "chaosresist", EffectiveSpellSuppressionChance: "spellsuppressionchance",
	EffectiveBlockChance: "blockchance", EffectiveSpellBlockChance: "spellblockchance", CritChance: "critchance",
};

/**
 * 코드에 든 저장 스탯(PlayerStat)을 **우리 엔진 재계산 값**으로 바꾼다. 빌드 화면 요약은 저장 스탯을 그대로 읽는데, 실빌드의 저장값은
 * 그 사람의 Config(비보스·개인 버프)로 계산된 것이라 정규화한 코드와 어긋난다(실측: RF 저장 DPS 886,017 / EHP 95,051 ↔ 같은 가정 재계산 346,041 / 512,720).
 * 옛 PlayerStat 은 전부 지우고 재계산에 있는 키만 새로 넣는다(남기면 일부만 옛 값이 섞인다).
 */
export function withEngineStats(xml, stats) {
	const byKey = Object.fromEntries((stats || []).map((s) => [s.key, String(s.value).replace(/,/g, "").replace(/^x/, "")]));
	const lines = Object.entries(PLAYER_STAT_KEYS)
		.filter(([, k]) => byKey[k] != null && byKey[k] !== "" && !Number.isNaN(Number(byKey[k])))
		.map(([name, k]) => `<PlayerStat stat="${name}" value="${Number(byKey[k])}"/>`);
	xml = xml.replace(/\s*<PlayerStat\b[^>]*\/>/g, "");
	return xml.replace(/(<Build\b[^>]*>)/, `$1\n${lines.join("\n")}`);
}

/**
 * 메인 소켓 그룹을 그 스킬 그룹으로. 빌드가 저장한 mainSocketGroup 은 오라·이동기 그룹인 경우가 흔하다.
 * 이름이 딱 맞지 않는 경우(발라 변종·변형젬)가 있어 ① 완전 일치 → ② 발라 아닌 포함 → ③ 발라 포함 순.
 * @returns {{xml: string, mainIdx: number}} mainIdx = 0 기반, 못 찾으면 -1(빌드 기본 메인 그룹 유지)
 */
export function alignMainGroup(xml, skillName, { preferSavedMain = false } = {}) {
	const groups = [...xml.matchAll(/<Skill[^>]*>[\s\S]*?<\/Skill>/g)].map((m) => m[0]);
	const gemsOf = (g) => [...g.matchAll(/nameSpec="([^"]+)"/g)].map((m) => m[1]);
	// preferSavedMain(PoE2): ① 빌드가 저장한 메인 그룹에 그 스킬이 있으면 그대로 ② 그 스킬이 **첫 젬**인 그룹(아이템·트리가 준 그룹 제외) —
	//   PoE2 빌드엔 Mirage Deadeye 같은 메타 젬 그룹이 그 스킬을 품고 앞쪽에 있어, "처음 든 그룹"을 고르면 DPS 0 인 그룹이 잡혔다(09-30 데드아이 얼음 사격).
	//   PoE1 벤치·시드도 켠다(09-30: 벤치 대표 45명 중 1명만 달랐고 그 1명은 옛 규칙이 틀렸다 — 겨울 구슬 Automation 그룹).
	if (preferSavedMain) {
		const saved = Number((xml.match(/<Build\b[^>]*\bmainSocketGroup="(\d+)"/) || [])[1]) - 1;
		if (saved >= 0 && saved < groups.length && gemsOf(groups[saved]).includes(skillName)) return { xml, mainIdx: saved };
		const firstIdx = groups.findIndex((g) => !/<Skill\b[^>]*\bsource="/.test(g) && gemsOf(g)[0] === skillName);
		if (firstIdx >= 0) return { xml: xml.replace(/(<Build[^>]*?)mainSocketGroup="\d+"/, `$1mainSocketGroup="${firstIdx + 1}"`), mainIdx: firstIdx };
	}
	let exactIdx = -1, containsIdx = -1, vaalIdx = -1;
	groups.forEach((g, i) => {
		const gems = [...g.matchAll(/nameSpec="([^"]+)"/g)].map((m) => m[1]);
		if (exactIdx < 0 && gems.some((n) => n === skillName)) exactIdx = i;
		for (const n of gems) {
			if (!n.includes(skillName)) continue;
			if (/^Vaal /.test(n)) { if (vaalIdx < 0) vaalIdx = i; }
			else if (containsIdx < 0) containsIdx = i;
		}
	});
	let mainIdx = exactIdx >= 0 ? exactIdx : containsIdx >= 0 ? containsIdx : vaalIdx;
	// preferSavedMain(PoE2): 어느 그룹 젬 이름에도 없는 스킬 — 보조젬이 만들어 내는 스킬(예: Impending Doom → Dark Consequences)이면
	//   빌드가 저장한 메인 그룹(ninja 가 메인으로 본 것과 같은 그룹)을 그대로 쓴다(09-30 블러드 메이지 탈락 원인).
	if (mainIdx < 0 && preferSavedMain) {
		const saved = Number((xml.match(/<Build\b[^>]*\bmainSocketGroup="(\d+)"/) || [])[1]) - 1;
		if (saved >= 0 && saved < groups.length) return { xml, mainIdx: saved };
	}
	if (mainIdx >= 0) xml = xml.replace(/(<Build[^>]*?)mainSocketGroup="\d+"/, `$1mainSocketGroup="${mainIdx + 1}"`);
	return { xml, mainIdx };
}
