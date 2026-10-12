// PoE2 번들 받기 주소를 패치 서버가 알려 준 CDN 으로 돌린다(10-12 C260).
//
// 왜: 추출 라이브러리(pathofexile-dat)는 CDN 주소를 코드에 박아 둔다(patch-poe2.poecdn.com/<patch>/Bundles2/…). 그런데 한국 IP 에선 패치 서버
//   (patch.pathofexile2.com:13060)가 카카오 CDN(patch.poe2.kakaogames.com/production/patch/0.5.5.4.2/)을 주고, GGG CDN 은 새 핫픽스가 나오면
//   옛 패치 폴더를 내린다 — 10-12 실측: 4.5.5.4 경로가 색인까지 전부 404, 같은 번들이 카카오 CDN 에선 정상. 캐시가 없는 PC 만 터졌다.
//   라이브러리는 받기 실패 시 process.exit(1) 이라 바깥에서 재시도도 못 한다 → fetch 단계에서 주소를 바꿔 준다.
// 어떻게: config.json 의 cdn(run-all2 가 패치 서버 응답으로 적는다)이 있으면 poecdn 번들 주소를 그 주소로 바꾼다. 없으면(해외 — 패치 서버가
//   GGG 주소를 줄 때) 아무것도 안 한다. paths.mjs 가 import 해 두므로 이 폴더 스크립트는 단독 실행해도 적용되고, 추출기 CLI(별도 프로세스)는
//   runExtractor 가 node --import 로 넣는다.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
let cdn = null;
try {
	cdn = JSON.parse(fs.readFileSync(path.join(here, "config.json"), "utf8")).cdn || null;
} catch {
	cdn = null;
}
const POECDN = /^https:\/\/patch(?:-poe2)?\.poecdn\.com\/[^/]+\/Bundles2\/(.+)$/;

if (cdn && !globalThis.__poe2CdnRedirect) {
	globalThis.__poe2CdnRedirect = true;
	const base = cdn.endsWith("/") ? cdn : cdn + "/";
	const original = globalThis.fetch;
	globalThis.fetch = (input, init) => {
		const url = typeof input === "string" ? input : input instanceof URL ? input.href : input?.url;
		const m = url && url.match(POECDN);
		return original(m ? base + "Bundles2/" + m[1] : input, init);
	};
}

export const CDN_BASE = cdn;
