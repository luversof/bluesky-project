// PoE2 번들 색인에서 폴더 내용을 나열한다(파일 경로 확인용). 사용법: node list-dir.mjs metadata/statdescriptions
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { DAT_LIB, WORK_DIR, loadConfig } from "./paths.mjs";

const lib = (p) => import(pathToFileURL(path.join(DAT_LIB, "dist", p)).href);
const { decompressedBundleSize, decompressSliceInBundle } = await lib("bundles/bundle.js");
const { readIndexBundle } = await lib("bundles/index-bundle.js");
const { getDirContent, getRootDirs } = await lib("bundles/index-paths.js");
const loaders = await lib("cli/bundle-loaders.js");

const { patch } = loadConfig();
const bl = new loaders.CachingBundleLoader(await loaders.CdnBundleLoader.create(path.join(WORK_DIR, ".cache"), patch));
const indexBin = await bl.fetchFile("_.index.bin");
const indexBundle = new Uint8Array(decompressedBundleSize(indexBin));
decompressSliceInBundle(indexBin, 0, indexBundle);
const idx = readIndexBundle(indexBundle);
const pathReps = new Uint8Array(decompressedBundleSize(idx.pathRepsBundle));
decompressSliceInBundle(idx.pathRepsBundle, 0, pathReps);
const dir = process.argv[2];
if (!dir) {
	console.log(getRootDirs(pathReps, idx.dirsInfo).join("\n"));
} else {
	const { files, dirs } = getDirContent(dir, pathReps, idx.dirsInfo);
	console.log(dirs.map((d) => d + "/").concat(files).join("\n"));
}
void fs;
