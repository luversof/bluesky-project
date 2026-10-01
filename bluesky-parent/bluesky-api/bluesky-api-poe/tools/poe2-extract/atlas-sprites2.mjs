// PoE2 아틀라스 노드 아이콘 → ~/.poe-gamedata/poe2/atlas-sprites/skills-96.webp + index.json (본 트리 tree-sprites 와 같은 모양)
// 본 트리 아이콘은 PoB 시트를 쓰지만 아틀라스는 PoB 에 없어서 게임 DDS(Art/2DArt/SkillIcons/passives/AtlasTrees/*.dds, BC1)를
// 하나씩 받아 96px 로 줄이고 16열 격자 한 장으로 붙인다(magick montage). parse-atlas2.mjs 다음에 돈다.
import { execFileSync } from "node:child_process";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { findImageMagick } from "../poe-extract/paths.mjs";
import { DATA_DIR, WORK_DIR, openLoader } from "./paths.mjs";

const SIZE = 96;
const COLS = 16;
const OUT = path.join(DATA_DIR, "atlas-sprites");
const TMP = path.join(WORK_DIR, "atlas-icons");
fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(TMP, { recursive: true });
const magickDir = findImageMagick();
if (!magickDir) {
	console.error("[poe2 atlas sprites] ImageMagick 이 없다 — 건너뜀(아틀라스는 아이콘 없이 그려진다)");
	process.exit(0);
}
const MAGICK = magickDir === "PATH" ? "magick" : path.join(magickDir, "magick.exe");

const tree = JSON.parse(fs.readFileSync(path.join(DATA_DIR, "atlas-tree.json"), "utf8"));
const icons = [...new Set(Object.values(tree.nodes).filter((n) => n.kind !== "mastery" && n.icon).map((n) => n.icon))].sort();
const loader = await openLoader();
const pngs = [];
const index = {};
let missing = 0;
for (const icon of icons) {
	const key = crypto.createHash("sha1").update(icon).digest("hex").slice(0, 16);
	const png = path.join(TMP, key + ".png");
	if (!fs.existsSync(png)) {
		const buf = (await loader.get(icon)) || (await loader.get(icon.toLowerCase()));
		if (!buf) {
			missing++;
			continue;
		}
		const dds = path.join(TMP, key + ".dds");
		fs.writeFileSync(dds, buf);
		execFileSync(MAGICK, [dds, "-resize", `${SIZE}x${SIZE}!`, "-strip", png]);
		fs.rmSync(dds, { force: true });
	}
	const i = pngs.length;
	pngs.push(png);
	index[icon] = { s: `skills-${SIZE}.webp`, x: (i % COLS) * SIZE, y: Math.floor(i / COLS) * SIZE, w: SIZE, h: SIZE };
}
execFileSync(MAGICK, [
	"montage",
	...pngs,
	"-background",
	"none",
	"-geometry",
	`${SIZE}x${SIZE}+0+0`,
	"-tile",
	`${COLS}x`,
	"-quality",
	"88",
	path.join(OUT, `skills-${SIZE}.webp`),
]);
fs.writeFileSync(path.join(OUT, "index.json"), JSON.stringify({ icons: index }));
console.log(`[poe2 atlas sprites] 아이콘 ${pngs.length}/${icons.length}(못 받음 ${missing}) → ${OUT}`);
