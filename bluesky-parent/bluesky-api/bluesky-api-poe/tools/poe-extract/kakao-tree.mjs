// 한국 공식 패시브 · 아틀라스 트리(카카오) — 페이지에 박힌 passiveSkillTreeData 의 노드별 공식 한국어(10-13 C295 · C296).
//   패시브: https://poe.kakaogames.com/passive-skill-tree · 아틀라스: https://poe.kakaogames.com/atlas-skill-tree
// 게임 테이블 · 서술기로 안 나오는 줄(트리 export 에만 있는 문장, 포인트 노드)을 이 공식 문장으로 채우는 데 쓴다.
// 받기에 실패하면 캐시, 그것도 없으면 null(예전대로 진행).
import fs from "node:fs";

/** text 에서 marker 뒤 첫 { … } 객체를 문자열 · 이스케이프를 지키며 잘라 JSON 으로 읽는다. */
export function extractObjectAfter(text, marker) {
	const at = text.indexOf(marker);
	if (at < 0) return null;
	const start = text.indexOf("{", at);
	let depth = 0;
	let inString = false;
	for (let i = start; i < text.length; i++) {
		const c = text[i];
		if (inString) {
			if (c === "\\") i++;
			else if (c === '"') inString = false;
		} else if (c === '"') inString = true;
		else if (c === "{") depth++;
		else if (c === "}" && --depth === 0) return JSON.parse(text.slice(start, i + 1));
	}
	return null;
}

/** { 노드번호: { name, stats } } — 스탯이 있는 노드만. */
export async function loadKakaoTree(url, cacheFile, label) {
	try {
		const response = await fetch(url, { headers: { "User-Agent": "Mozilla/5.0" } });
		if (!response.ok) throw new Error("HTTP " + response.status);
		const data = extractObjectAfter(await response.text(), "passiveSkillTreeData");
		if (!data?.nodes) throw new Error("passiveSkillTreeData 없음");
		const nodes = {};
		for (const [id, n] of Object.entries(data.nodes)) if (n.stats?.length) nodes[id] = { name: n.name, stats: n.stats };
		fs.writeFileSync(cacheFile, JSON.stringify(nodes));
		console.log(`한국 공식 ${label}: 노드 ${Object.keys(nodes).length}개 → ${cacheFile}`);
		return nodes;
	} catch (e) {
		if (fs.existsSync(cacheFile)) {
			console.warn(`한국 공식 ${label} 받기 실패 — 캐시 사용:`, e.message);
			return JSON.parse(fs.readFileSync(cacheFile, "utf8"));
		}
		console.warn(`한국 공식 ${label} 받기 실패 — 없이 진행:`, e.message);
		return null;
	}
}

const HANGUL = /[가-힣]/;
/**
 * 노드 statsKo 를 공식 문장으로 보강 — 공식 줄 수가 영어와 같을 때만(순서가 같다고 볼 수 있을 때):
 *   statsKo 가 없거나 줄 수가 영어와 다르면 통째로, 영어로 남은 줄은 그 줄만. 고친 노드 수를 돌려준다.
 */
export function applyOfficialKo(nodes, kakao) {
	if (!kakao) return 0;
	let fixed = 0;
	for (const node of nodes) {
		const k = kakao[String(node.id)];
		if (!k || !node.stats?.length || k.stats.length !== node.stats.length) continue;
		const ko = node.statsKo;
		if (!ko?.length || ko.length !== node.stats.length) {
			node.statsKo = [...k.stats];
			fixed++;
			continue;
		}
		let changed = false;
		node.statsKo = ko.map((line, i) => {
			if (HANGUL.test(line)) return line;
			changed = true;
			return k.stats[i];
		});
		if (changed) fixed++;
	}
	return fixed;
}
