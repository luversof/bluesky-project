// JTE 템플릿의 인라인 <script> 태그에 CSP nonce 가 누락되면 빌드를 실패시킨다.
// (CSP 가 enforcing 으로 전환되면 nonce 없는 인라인 스크립트는 조용히 차단되므로 빌드 단계에서 잡는다)
// JTE content block 은 HTML 컨텍스트로 파싱되어 JS 의 '<' 를 허용하지 않기 때문에
// 래퍼 컴포넌트 강제가 불가능해, 대신 이 검사로 누락을 방지한다.
import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const JTE_ROOT = path.resolve(import.meta.dirname, "../jte");

function walk(dir, acc) {
	for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
		const p = path.join(dir, entry.name);
		if (entry.isDirectory()) walk(p, acc);
		else if (entry.name.endsWith(".jte")) acc.push(p);
	}
	return acc;
}

const violations = [];
for (const file of walk(JTE_ROOT, [])) {
	// JTE 주석(<%-- --%>) 안의 언급은 제외
	const source = fs.readFileSync(file, "utf8").replace(/<%--[\s\S]*?--%>/g, "");
	for (const match of source.matchAll(/<script\b[^>]*>/g)) {
		const tag = match[0];
		if (tag.includes("src=") || tag.includes("nonce=")) continue;
		const line = source.slice(0, match.index).split("\n").length;
		violations.push(`${path.relative(JTE_ROOT, file)}:${line} ${tag}`);
	}
}

if (violations.length > 0) {
	console.error("[check-jte-script-nonce] CSP nonce 가 없는 인라인 <script> 발견:");
	for (const v of violations) console.error("  " + v);
	console.error('인라인 스크립트는 <script nonce="${CspNonceHolder.getNonce()}"> 로 작성해야 합니다.');
	process.exit(1);
}
console.log("[check-jte-script-nonce] OK");
