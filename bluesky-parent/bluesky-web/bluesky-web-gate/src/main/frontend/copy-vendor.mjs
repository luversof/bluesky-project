// 외부 CDN 의존을 없애기 위해 npm으로 설치한(버전 고정) 브라우저용 라이브러리를
// 정적 리소스(/js/vendor, /css/vendor)로 복사한다. build:vendor 단계에서 실행된다.
import { mkdirSync, copyFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const nm = resolve(here, "node_modules");
const jsDest = resolve(here, "../resources/static/js/vendor");
const cssDest = resolve(here, "../resources/static/css/vendor");

// [node_modules 내 소스, vendor 내 대상 파일명]
const jsFiles = [
	["htmx.org/dist/htmx.min.js", "htmx.min.js"],
	["htmx-ext-json-enc/json-enc.js", "json-enc.js"],
	["chart.js/dist/chart.umd.min.js", "chart.umd.min.js"],
	// hammerjs / chartjs-plugin-zoom 은 코드 어디에서도 참조하지 않아 복사 대상에서 제외했다.
	// (차트 줌/제스처 기능을 다시 쓰게 되면 여기에 되살릴 것)
	["dayjs/dayjs.min.js", "dayjs.min.js"],
	["dayjs/locale/ko.js", "dayjs.locale.ko.js"],
	["dayjs/plugin/relativeTime.js", "dayjs.plugin.relativeTime.js"],
	["easymde/dist/easymde.min.js", "easymde.min.js"],
];

const cssFiles = [["easymde/dist/easymde.min.css", "easymde.min.css"]];

function copyAll(dest, list) {
	mkdirSync(dest, { recursive: true });
	for (const [src, name] of list) {
		copyFileSync(resolve(nm, src), resolve(dest, name));
		console.log(`[copy-vendor] ${name}`);
	}
}

copyAll(jsDest, jsFiles);
copyAll(cssDest, cssFiles);
console.log(
	`[copy-vendor] ${jsFiles.length} js -> ${jsDest}, ${cssFiles.length} css -> ${cssDest}`,
);
