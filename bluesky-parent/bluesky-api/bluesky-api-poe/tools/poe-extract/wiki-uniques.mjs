// 고유 아이템 로어(플레이버) 텍스트·요구 레벨을 공식 커뮤니티 위키에서 채운다 — PoE1(poewiki.net) · PoE2(poe2wiki.net) 공용(10-01).
//
// 왜 위키인가: 게임 클라이언트 데이터의 FlavourText 표(Id·Text)에는 어느 고유 아이템의 문구인지 잇는 키가 없다(배정은 서버 쪽).
// 위키 cargo(items 표)는 고유 이름 ↔ 영문 로어·요구 레벨을 준다. 다만 위키 문구는 게임과 글자가 조금 다를 때가 있어
// (예: "life into ash" ↔ 게임 "life to ash", 대소문자) 게임 FlavourText(영문)와 **유사도**로 맞춘 뒤, 맞으면 **게임 원문**(영문·한글, 같은 Id)을 쓴다.
// 못 맞추면 위키 영문만 쓴다(한글은 비움 — 화면은 영문으로 폴백).
// 요구 레벨은 데이터에 없을 때(null/0)만 위키 값으로 채운다(PoB 에서 온 값이 있으면 그대로).
//
// 네트워크 단계라 실패해도 파이프라인을 죽이지 않는다(호출부에서 비치명). 위키 응답은 캐시해 오프라인 재실행도 같은 결과.
// 사용(PoE1): node wiki-uniques.mjs     · PoE2: tools/poe2-extract/wiki-uniques2.mjs 가 runWikiUniques 를 부른다.
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const UA = "bluesky-poe-gamedata/1.0 (data pipeline; contact via gate)";
const decode = (s) =>
	String(s ?? "")
		.replace(/&lt;br\s*\/?&gt;|<br\s*\/?>/gi, "\n")
		.replace(/&quot;/g, '"')
		.replace(/&#0?39;/g, "'")
		.replace(/&amp;/g, "&")
		.replace(/&lt;/g, "<")
		.replace(/&gt;/g, ">")
		.replace(/<[^>]+>/g, "")
		.trim();
const tokens = (s) => (String(s).toLowerCase().match(/[a-z0-9]+/g) || []);

/** 위키 cargo items 에서 고유 전체(이름 · 로어 · 요구 레벨) — 500건씩 끝까지. */
async function fetchWiki(host) {
	const rows = [];
	for (let offset = 0; offset < 20000; offset += 500) {
		const url =
			`https://${host}/w/api.php?action=cargoquery&tables=items&fields=items.name,items.flavour_text,items.required_level` +
			`&where=items.rarity_id%3D%22unique%22&limit=500&offset=${offset}&format=json`;
		let res;
		for (let attempt = 0; attempt < 4; attempt++) {
			res = await fetch(url, { headers: { "User-Agent": UA } });
			if (res.status !== 429 && res.status < 500) break;
			await new Promise((r) => setTimeout(r, 5000 * (attempt + 1)));
		}
		if (!res.ok) throw new Error(`위키 ${host} 응답 ${res.status}`);
		const j = await res.json();
		const page = (j.cargoquery || []).map((x) => x.title);
		rows.push(...page);
		if (page.length < 500) break;
		await new Promise((r) => setTimeout(r, 400)); // 위키 서버 예의
	}
	return rows;
}

/** 위키 행 → 이름별 { flavour(영문 줄 배열), requiredLevel } — 같은 이름이 여러 행(레거시·변형)이면 로어 있는 첫 행. */
function byName(rows) {
	const map = new Map();
	for (const r of rows) {
		const name = decode(r.name);
		if (!name) continue;
		const flav = decode(r["flavour text"]);
		const lvl = Number(r["required level"]) || 0;
		const prev = map.get(name);
		if (!prev) map.set(name, { flavour: flav || null, requiredLevel: lvl || null });
		else {
			if (!prev.flavour && flav) prev.flavour = flav;
			if (!prev.requiredLevel && lvl) prev.requiredLevel = lvl;
		}
	}
	return map;
}

/** 위키 영문 로어 ↔ 게임 FlavourText(영문) 유사도 매칭 → { en, ko } (게임 원문). 토큰 자카드 0.75 이상 최고 점수. */
function matcher(enTable, koTable) {
	const koById = new Map(koTable.map((x) => [x.Id, x.Text]));
	const cands = enTable
		.filter((x) => x.Text && x.Text.trim())
		.map((x) => ({ id: x.Id, text: x.Text, toks: new Set(tokens(x.Text)) }));
	const index = new Map(); // 토큰 → 후보(희귀 토큰으로 후보를 좁힌다)
	for (const c of cands) for (const t of c.toks) (index.get(t) || index.set(t, []).get(t)).push(c);
	return (wikiText) => {
		const wt = new Set(tokens(wikiText));
		if (!wt.size) return null;
		const pool = new Set();
		for (const t of wt) for (const c of index.get(t) || []) pool.add(c);
		let best = null, bestScore = 0;
		for (const c of pool) {
			let inter = 0;
			for (const t of wt) if (c.toks.has(t)) inter++;
			const score = inter / (wt.size + c.toks.size - inter);
			if (score > bestScore) { bestScore = score; best = c; }
		}
		if (!best || bestScore < 0.75) return null;
		return { en: best.text, ko: koById.get(best.id) || null, score: bestScore };
	};
}

const lines = (s) => (s ? String(s).replace(/\r\n/g, "\n").split("\n").map((x) => x.trim()).filter(Boolean) : null);
/** 게임 원문 마크업 정리 — `<default>{글}` → 글, 고대 문자 `<<HBGAh>>` 는 글꼴이 없어 지운다, 남은 <태그> 제거. 비면 null. */
const cleanGame = (s) => {
	if (!s) return null;
	const t = String(s)
		.replace(/<[^<>{}]*>\{([^{}]*)\}/g, "$1")
		.replace(/<<[^<>]+>>/g, "")
		.replace(/<[^<>]+>/g, "");
	const ls = lines(t);
	return ls && ls.length ? ls : null;
};

/**
 * 고유 JSON({patch, items}) 에 flavour/flavourKo(줄 배열)·requiredLevel(비었을 때만)을 채워 다시 쓴다.
 * @returns 요약(채운 수 · 게임 원문 매칭 수 …)
 */
export async function runWikiUniques({ host, uniquesFile, tablesDir, cacheFile, label }) {
	let rows;
	try {
		rows = await fetchWiki(host);
		fs.mkdirSync(path.dirname(cacheFile), { recursive: true });
		fs.writeFileSync(cacheFile, JSON.stringify({ host, fetchedAt: new Date().toISOString(), rows }));
	} catch (e) {
		if (!fs.existsSync(cacheFile)) throw e;
		console.warn(`[${label}] 위키 조회 실패(${e.message}) — 캐시로 계속: ${cacheFile}`);
		rows = JSON.parse(fs.readFileSync(cacheFile, "utf8")).rows;
	}
	const wiki = byName(rows);
	const en = JSON.parse(fs.readFileSync(path.join(tablesDir, "English", "FlavourText.json"), "utf8"));
	const ko = JSON.parse(fs.readFileSync(path.join(tablesDir, "Korean", "FlavourText.json"), "utf8"));
	const match = matcher(en, ko);
	const data = JSON.parse(fs.readFileSync(uniquesFile, "utf8"));
	let flav = 0, game = 0, lvl = 0, missingWiki = 0;
	for (const u of data.items) {
		const w = wiki.get(u.name);
		if (!w) { missingWiki++; continue; }
		if (w.flavour) {
			const m = match(w.flavour);
			u.flavour = m ? cleanGame(m.en) : lines(w.flavour);
			u.flavourKo = m && m.ko ? cleanGame(m.ko) : null;
			flav++;
			if (m) game++;
		}
		if (!(u.requiredLevel > 0) && w.requiredLevel > 1) { u.requiredLevel = w.requiredLevel; lvl++; }
	}
	fs.writeFileSync(uniquesFile, JSON.stringify(data, null, 1));
	const summary = { total: data.items.length, wikiRows: rows.length, flavour: flav, gameMatched: game, requiredLevelFilled: lvl, notInWiki: missingWiki };
	console.log(`[${label}] 위키 로어·요구 레벨: ${JSON.stringify(summary)}`);
	return summary;
}

// PoE1 직접 실행
if (import.meta.url === pathToFileURL(process.argv[1]).href) {
	const { DATA_DIR, TABLES_DIR, WORK_DIR } = await import("./paths.mjs");
	await runWikiUniques({
		host: "www.poewiki.net",
		uniquesFile: path.join(DATA_DIR, "unique-items.json"),
		tablesDir: TABLES_DIR,
		cacheFile: path.join(WORK_DIR, "wiki-uniques-cache.json"),
		label: "poe1",
	});
}
