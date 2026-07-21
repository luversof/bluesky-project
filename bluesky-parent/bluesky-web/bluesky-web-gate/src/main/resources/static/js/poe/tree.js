"use strict";
// PoE 패시브/아틀라스 트리 읽기 전용 뷰어 — 공식 스프라이트시트로 게임식 렌더.
// 데이터 소스는 캔버스 data-tree-src / data-sprites-src 로 지정(기본=스킬 트리) → 스킬·아틀라스 공용.
// 그룹 배경 + 궤도 곡선 연결 + 스킬 아이콘/프레임 스프라이트 blit. 팬/줌/호버 툴팁/?nodes= 하이라이트/로케일.
(() => {
    const maybeCanvas = document.getElementById("poeTreeCanvas");
    const tooltip = document.getElementById("poeTreeTooltip");
    if (!maybeCanvas || maybeCanvas.dataset.poeTreeInitialized === "true")
        return;
    const canvas = maybeCanvas;
    canvas.dataset.poeTreeInitialized = "true";
    const isKorean = canvas.dataset.locale !== "en";
    const treeSrc = canvas.dataset.treeSrc || "/poe-data/passive-tree.json";
    const spritesSrc = canvas.dataset.spritesSrc || "/poe-data/tree-sprites-skill.json";
    const maybeContext = canvas.getContext("2d");
    if (!maybeContext)
        return;
    const context = maybeContext;
    // ---- 스프라이트 시트(이미지) 지연 로드 캐시 ----
    const sheetCache = new Map();
    function getSheet(file) {
        let img = sheetCache.get(file);
        if (img)
            return img.dataset.ready === "true" ? img : null;
        img = new Image();
        img.dataset.ready = "false";
        img.onload = () => {
            img.dataset.ready = "true";
            scheduleDraw();
        };
        img.onerror = () => sheetCache.set(file, img);
        img.src = "/poe-assets/" + file;
        sheetCache.set(file, img);
        return null;
    }
    let drawScheduled = false;
    function scheduleDraw() {
        if (drawScheduled)
            return;
        drawScheduled = true;
        requestAnimationFrame(() => {
            drawScheduled = false;
            draw();
        });
    }
    let sprites = {};
    // 스프라이트 1개를 (월드 중심 wx,wy)에 blit. coordKey 없으면 미그림. 반환: 그렸으면 월드 크기 반폭.
    function blit(spriteKey, coordKey, wx, wy, clipCircle = false) {
        const sp = sprites[spriteKey];
        if (!sp)
            return 0;
        const c = sp.coords[coordKey];
        if (!c)
            return 0;
        const img = getSheet(sp.file);
        const worldW = c.w / sp.zoom;
        const worldH = c.h / sp.zoom;
        if (!img)
            return worldW / 2;
        const sx = wx * scale + offsetX;
        const sy = wy * scale + offsetY;
        const dw = worldW * scale;
        const dh = worldH * scale;
        if (clipCircle) {
            context.save();
            context.beginPath();
            context.arc(sx, sy, Math.min(dw, dh) * 0.47, 0, Math.PI * 2);
            context.clip();
            context.drawImage(img, c.x, c.y, c.w, c.h, sx - dw / 2, sy - dh / 2, dw, dh);
            context.restore();
        }
        else {
            context.drawImage(img, c.x, c.y, c.w, c.h, sx - dw / 2, sy - dh / 2, dw, dh);
        }
        return worldW / 2;
    }
    // 노드 타입 → 아이콘 시트 키. 공식 뷰어처럼 할당=Active(선명) / 미할당=Inactive(흐림).
    const ICON_SHEET = {
        normal: "normalActive",
        notable: "notableActive",
        keystone: "keystoneActive",
        wormhole: "wormholeActive",
        mastery: "mastery",
    };
    const ICON_SHEET_INACTIVE = {
        normal: "normalInactive",
        notable: "notableInactive",
        keystone: "keystoneInactive",
        wormhole: "keystoneInactive",
        mastery: "masteryInactive",
    };
    // 미할당 시트가 없으면(아틀라스 등) Active 로 폴백
    function iconSheetFor(type, allocated) {
        if (!allocated) {
            const inactive = ICON_SHEET_INACTIVE[type];
            if (inactive && sprites[inactive])
                return inactive;
        }
        return ICON_SHEET[type];
    }
    // 노드 상태 3단계 — 공식 뷰어와 동일. 할당 / 지금 찍을 수 있음(인접) / 미할당.
    const CLUSTER_SIZE_NAME = ["Small", "Medium", "Large"];
    function frameCoord(node, allocated, canAlloc) {
        const st = allocated ? "Allocated" : canAlloc ? "CanAllocate" : "Unallocated";
        switch (node.type) {
            case "notable":
                return "NotableFrame" + st;
            case "keystone":
            case "wormhole":
                return "KeystoneFrame" + st;
            case "jewel":
                // 클러스터 주얼 소켓은 크기별 전용 아트, 일반 소켓은 JewelSocketAlt 계열(공식 최신 아트)
                if (node.clusterSize !== undefined) {
                    const size = CLUSTER_SIZE_NAME[node.clusterSize] || "Small";
                    return "JewelSocketClusterAlt" + (allocated ? "Normal" : st === "CanAllocate" ? "CanAllocate" : "Normal") + "1" + size;
                }
                return allocated ? "JewelSocketAltActive" : canAlloc ? "JewelSocketAltCanAllocate" : "JewelSocketAltNormal";
            case "normal":
                // 공식은 일반 노드에 CanAllocate 전용 아트를 쓰지 않는다(Highlighted 는 호버/검색용)
                return allocated ? "PSSkillFrameActive" : "PSSkillFrame";
            default:
                return null;
        }
    }
    // 타입별 프레임 월드 반지름(히트테스트/LOD/폴백) — 매니페스트 로드 후 채움
    const nodeRadiusWorld = { normal: 45, notable: 70, keystone: 95, jewel: 55, mastery: 55, wormhole: 95, class: 120 };
    function computeRadii() {
        var _a, _b, _c, _d;
        const fr = sprites.frame;
        if (!fr)
            return;
        const pick = (k) => (fr.coords[k] ? fr.coords[k].w / fr.zoom / 2 : null);
        nodeRadiusWorld.normal = (_a = pick("PSSkillFrame")) !== null && _a !== void 0 ? _a : nodeRadiusWorld.normal;
        nodeRadiusWorld.notable = (_b = pick("NotableFrameUnallocated")) !== null && _b !== void 0 ? _b : nodeRadiusWorld.notable;
        nodeRadiusWorld.keystone = (_c = pick("KeystoneFrameUnallocated")) !== null && _c !== void 0 ? _c : nodeRadiusWorld.keystone;
        nodeRadiusWorld.wormhole = nodeRadiusWorld.keystone;
        nodeRadiusWorld.jewel = (_d = pick("JewelFrameUnallocated")) !== null && _d !== void 0 ? _d : nodeRadiusWorld.jewel;
        if (sprites.mastery) {
            const anyM = Object.values(sprites.mastery.coords)[0];
            if (anyM)
                nodeRadiusWorld.mastery = anyM.w / sprites.mastery.zoom / 2;
        }
    }
    let nodes = [];
    let edges = [];
    let groups = {};
    let orbitRadii = [0, 82, 162, 335, 493, 662, 846];
    let skillsPerOrbit = [1, 6, 16, 16, 40, 72, 72];
    const nodeById = new Map();
    const adjacency = new Map();
    let scale = 0.03;
    let offsetX = 0;
    let offsetY = 0;
    let hovered = null;
    const searchHits = new Set(); // 검색 매칭 노드(청록 테두리 강조)
    let hoverPath = []; // 호버 노드까지 할당집합에서의 최단경로 미리보기
    // 인터랙티브 편집: 할당 노드 집합 + 현재 직업/전직. 클래스 시작노드가 루트.
    const highlighted = new Set();
    // 아틀라스는 클래스 루트가 없어 규칙이 다르다(자유 시작 + ?nodes= 동기화). 편집은 둘 다 가능.
    const isAtlas = canvas.dataset.treeSrc === "/poe-data/atlas-tree.json";
    const interactive = true;
    // 트리 데이터의 class 시작노드 이름 → GGG classId (0=Scion..6=Shadow)
    const CLASS_START_CLASSID = { Seven: 0, MARAUDER: 1, RANGER: 2, WITCH: 3, DUELIST: 4, TEMPLAR: 5, SIX: 6 };
    // 사람이 읽는 직업명 → classId (시뮬 결과 링크의 ?class= 해석용. 시작노드 이름과 다르다)
    const CLASS_NAME_CLASSID = {
        Scion: 0,
        Marauder: 1,
        Ranger: 2,
        Witch: 3,
        Duelist: 4,
        Templar: 5,
        Shadow: 6,
    };
    const classStartByClassId = new Map(); // classId → 시작노드 id (loadTree 에서 채움)
    let currentClassId = 0;
    let currentAscend = 0;
    let currentBloodline = 0; // 0=없음, 1..N = bloodlines[N-1] (GGG URL 의 secondary ascendancy id)
    let classAsc = []; // classId → 전직명 목록 (data.classes)
    let bloodlines = []; // 혈맹(대체 전직) — 인덱스 = secondary id
    let atlasRoot = null; // 아틀라스 시작 노드(지도 중앙) — 여기서 이어져야 할당 가능
    let maxPoints = 0; // 패시브 123 / 아틀라스 138
    let maxAscPoints = 0; // 전직 8
    const removalSet = new Set(); // 할당 노드 호버 시 함께 해제될 노드(빨강 미리보기)
    const masteryPicks = new Map(); // 마스터리 노드 id → 선택한 효과 id
    // 현재 화면에 노출할 전직/혈맹 서브트리 이름. 나머지 전직 섬은 숨긴다(공식 뷰어 동작).
    function currentAscName() {
        if (isAtlas || currentAscend === 0)
            return null;
        return (classAsc[currentClassId] || [])[currentAscend - 1] || null;
    }
    function currentBloodlineId() {
        var _a;
        if (isAtlas || currentBloodline === 0)
            return null;
        return ((_a = bloodlines[currentBloodline - 1]) === null || _a === void 0 ? void 0 : _a.id) || null;
    }
    function nodeVisible(node) {
        if (!node.ascendancy)
            return true;
        return node.ascendancy === currentAscName() || node.ascendancy === currentBloodlineId();
    }
    // 경로 탐색용 인접 — 전직↔메인 간선은 "선택한 전직/혈맹" 으로 들어가는 것만 허용한다.
    // (Ascendant 의 Path of the X 처럼 메인으로 되돌아오는 간선이 지름길로 악용되는 것도 함께 막힘)
    function pathNeighbors(id) {
        const from = nodeById.get(id);
        if (!from)
            return [];
        const out = [];
        for (const nb of adjacency.get(id) || []) {
            const to = nodeById.get(nb);
            if (!to || !nodeVisible(to))
                continue;
            if ((from.ascendancy || null) === (to.ascendancy || null)) {
                out.push(nb);
            }
            else if (to.ascendancyStart || from.ascendancyStart) {
                out.push(nb); // 전직 진입/이탈 지점 (visible 검사를 이미 통과)
            }
        }
        return out;
    }
    // ---- GGG 패시브트리 URL 인코딩(version 6) ----
    function b64urlToBytes(s) {
        const b64 = s.replace(/-/g, "+").replace(/_/g, "/");
        const bin = atob(b64);
        const out = [];
        for (let i = 0; i < bin.length; i++)
            out.push(bin.charCodeAt(i));
        return out;
    }
    function bytesToB64url(bytes) {
        let bin = "";
        for (const b of bytes)
            bin += String.fromCharCode(b & 0xff);
        return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_");
    }
    // URL 의 GGG 문자열 → {classId, ascend, nodes[]}. 실패 시 null.
    function decodeTree(s) {
        try {
            const a = b64urlToBytes(s);
            if (a.length < 7)
                return null;
            const classId = a[4];
            const ascByte = a[5];
            const ascend = ascByte & 0x3;
            const bloodline = ascByte >> 2; // secondary ascendancy id (혈맹)
            const count = a[6];
            const nodes = [];
            let p = 7;
            for (let i = 0; i < count && p + 1 < a.length; i++) {
                nodes.push(a[p] * 256 + a[p + 1]);
                p += 2;
            }
            // clusterCount(1B) + 클러스터 id(2B each) → 건너뛰고, masteryCount(1B) + 항목(4B: effect 2B + node 2B)
            const masteries = [];
            if (p < a.length) {
                p += 1 + a[p] * 2;
                if (p < a.length) {
                    const mCount = a[p];
                    p += 1;
                    for (let i = 0; i < mCount && p + 3 < a.length; i++) {
                        masteries.push({ effect: a[p] * 256 + a[p + 1], node: a[p + 2] * 256 + a[p + 3] });
                        p += 4;
                    }
                }
            }
            return { classId, ascend, bloodline, nodes, masteries };
        }
        catch (e) {
            return null;
        }
    }
    function encodeTree() {
        const a = [0, 0, 0, 6, currentClassId & 0xff, ((currentBloodline & 0x3f) << 2) | (currentAscend & 0x3)];
        let count = 0;
        const body = [];
        for (const id of highlighted) {
            const node = nodeById.get(id);
            if (!node || node.type === "class" || node.type === "ascendancyStart" || id >= 65536 || count >= 255)
                continue;
            body.push(Math.floor(id / 256), id % 256);
            count++;
        }
        // clusterCount=0, 그다음 masteryCount + 항목(effect 2B + node 2B)
        const mastery = [];
        let mCount = 0;
        for (const [nodeId, effectId] of masteryPicks) {
            if (!highlighted.has(nodeId) || mCount >= 255)
                continue;
            mastery.push(Math.floor(effectId / 256), effectId % 256, Math.floor(nodeId / 256), nodeId % 256);
            mCount++;
        }
        a.push(count, ...body, 0, mCount, ...mastery);
        return bytesToB64url(a);
    }
    // URL 로드: ?t=GGG 우선, 없으면 ?nodes= 레거시(콤마 id).
    function loadFromUrl() {
        const params = new URLSearchParams(globalThis.location.search);
        const t = params.get("t");
        if (t) {
            const dec = decodeTree(t);
            if (dec) {
                currentClassId = dec.classId;
                currentAscend = dec.ascend;
                currentBloodline = dec.bloodline;
                for (const id of dec.nodes)
                    highlighted.add(id);
                for (const m of dec.masteries)
                    masteryPicks.set(m.node, m.effect);
                return;
            }
        }
        for (const token of (params.get("nodes") || "").split(",")) {
            const id = Number(token);
            if (Number.isFinite(id) && id > 0)
                highlighted.add(id);
        }
        // 레거시 ?nodes= 로 들어올 때(시뮬 결과 링크 등) 직업/전직도 함께 복원한다.
        // 안 하면 사이온(0)으로 남아 루트가 어긋나고, 편집 시 고아 정리가 트리를 통째로 날린다.
        pendingClass = params.get("class");
        pendingAscend = params.get("asc");
    }
    let pendingClass = null;
    let pendingAscend = null;
    // 트리 데이터 로드 후에만 이름→id 해석이 가능하므로 loadTree 에서 호출한다.
    function applyPendingClass() {
        if (pendingClass) {
            const numeric = Number(pendingClass);
            if (Number.isInteger(numeric) && numeric >= 0 && numeric <= 6) {
                currentClassId = numeric;
            }
            else {
                for (const [name, id] of Object.entries(CLASS_NAME_CLASSID)) {
                    if (name.toLowerCase() === pendingClass.toLowerCase())
                        currentClassId = id;
                }
            }
        }
        if (pendingAscend) {
            const list = classAsc[currentClassId] || [];
            const idx = list.findIndex((a) => a.toLowerCase() === pendingAscend.toLowerCase());
            if (idx >= 0)
                currentAscend = idx + 1;
        }
        pendingClass = null;
        pendingAscend = null;
    }
    loadFromUrl();
    // URL 을 현재 할당 상태로 갱신(실시간 반영). 편집 모드에서만.
    function syncUrl() {
        if (!interactive)
            return;
        const params = new URLSearchParams(globalThis.location.search);
        if (isAtlas) {
            // 아틀라스는 GGG 클래스 개념이 없어 콤마 id 로 동기화
            params.set("nodes", Array.from(highlighted).join(","));
            params.delete("t");
        }
        else {
            params.set("t", encodeTree());
            params.delete("nodes");
        }
        globalThis.history.replaceState(null, "", globalThis.location.pathname + "?" + params.toString());
    }
    const hasHighlight = () => highlighted.size > 0;
    // 할당의 기준점 — 패시브는 현재 직업의 시작 노드, 아틀라스는 지도 중앙 시작 노드.
    function rootNode() {
        return isAtlas ? (atlasRoot !== null && atlasRoot !== void 0 ? atlasRoot : undefined) : classStartByClassId.get(currentClassId);
    }
    // ---- 클릭 할당(연결성 검증) ----
    // 루트(클래스 시작노드)에서 할당 노드만 따라 BFS → 도달 못하는 할당노드(고아) 제거.
    function pruneOrphans() {
        const root = rootNode();
        if (root === undefined)
            return;
        const reach = reachableSet(root, -1);
        for (const id of Array.from(highlighted))
            if (!reach.has(id))
                highlighted.delete(id);
        highlighted.add(root);
    }
    // 루트에서 할당 노드만 따라 도달 가능한 집합(제외 노드 하나를 끊어 볼 수 있다).
    function reachableSet(root, excluded) {
        const reach = new Set([root]);
        const queue = [root];
        while (queue.length) {
            const cur = queue.shift();
            for (const nb of pathNeighbors(cur)) {
                if (nb !== excluded && highlighted.has(nb) && !reach.has(nb)) {
                    reach.add(nb);
                    queue.push(nb);
                }
            }
        }
        return reach;
    }
    // 할당 가능 = 인접에 이미 할당된 노드가 있음(루트까지 사슬 연결).
    function canAllocate(node) {
        if (node.type === "class" || !nodeVisible(node))
            return false;
        for (const nb of pathNeighbors(node.id))
            if (highlighted.has(nb))
                return true;
        return false;
    }
    // 이 할당 노드를 해제하면 함께 떨어져 나가는 노드 집합(자기 자신 포함).
    function computeRemoval(targetId) {
        if (!highlighted.has(targetId))
            return [];
        const node = nodeById.get(targetId);
        if (!node || node.type === "class")
            return [];
        const root = rootNode();
        if (root === undefined)
            return [targetId];
        const reach = reachableSet(root, targetId);
        const out = [targetId];
        for (const id of highlighted)
            if (id !== targetId && !reach.has(id))
                out.push(id);
        return out;
    }
    function toggleNode(node) {
        var _a;
        if (!interactive || node.type === "class" || !nodeVisible(node))
            return;
        // 마스터리는 "어떤 효과를 쓸지" 를 골라야 찍힌다 — 할당 가능하면 선택 팝업을 띄운다.
        // 마스터리는 "어떤 효과를 쓸지" 를 골라야 찍힌다 — 도달 가능하면 선택 팝업을 띄운다.
        // (마스터리 이웃은 전부 같은 그룹이라, 도달 = 그룹 패시브를 이미 지남 = 게임 규칙 자동 충족)
        if (((_a = node.masteryEffects) === null || _a === void 0 ? void 0 : _a.length) && !highlighted.has(node.id)) {
            if (canAllocate(node) || computeHoverPath(node.id).length > 1)
                openMasteryPicker(node);
            return;
        }
        if (highlighted.has(node.id)) {
            highlighted.delete(node.id);
            masteryPicks.delete(node.id);
            pruneOrphans();
        }
        else if (canAllocate(node)) {
            highlighted.add(node.id);
        }
        else {
            // 인접하지 않은 먼 노드 — 최단 경로를 따라 중간 노드까지 한 번에 할당(공식 뷰어 동작)
            const path = computeHoverPath(node.id);
            if (path.length < 2)
                return; // 도달 불가
            for (const id of path)
                highlighted.add(id);
        }
        refreshHoverState(); // 커서가 그대로 노드 위에 있으므로 경로/해제 미리보기를 다시 계산
        updatePoints();
        syncUrl();
        draw();
    }
    // ---- 마스터리 효과 선택 팝업 (공식 뷰어와 동일하게 효과 하나를 골라야 할당된다) ----
    const effectLines = (eff) => { var _a; return (isKorean && ((_a = eff.statsKo) === null || _a === void 0 ? void 0 : _a.length) ? eff.statsKo : eff.stats); };
    let masteryPicker = null;
    function closeMasteryPicker() {
        masteryPicker === null || masteryPicker === void 0 ? void 0 : masteryPicker.remove();
        masteryPicker = null;
    }
    function openMasteryPicker(node) {
        closeMasteryPicker();
        const host = canvas.parentElement;
        const panel = document.createElement("div");
        panel.className =
            "absolute z-20 max-h-[60%] w-80 overflow-y-auto rounded shadow-2xl border border-amber-700/60 bg-stone-900/97";
        const head = document.createElement("div");
        head.className =
            "sticky top-0 border-y-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-sm px-6 py-1.5";
        head.textContent = (isKorean && node.nameKo ? node.nameKo : node.name) + (isKorean ? " — 효과 선택" : " — pick effect");
        panel.appendChild(head);
        for (const eff of node.masteryEffects || []) {
            const row = document.createElement("button");
            row.type = "button";
            row.className = "block w-full text-left px-4 py-2 text-xs text-sky-300 leading-5 hover:bg-amber-900/40 border-b border-stone-800";
            row.textContent = effectLines(eff).join("\n");
            row.addEventListener("click", () => {
                // 멀리 있으면 경로까지 함께 할당 — 일반 노드 클릭과 동일한 규칙
                if (!canAllocate(node))
                    for (const id of computeHoverPath(node.id))
                        highlighted.add(id);
                highlighted.add(node.id);
                masteryPicks.set(node.id, eff.id);
                closeMasteryPicker();
                refreshHoverState();
                updatePoints();
                syncUrl();
                draw();
            });
            panel.appendChild(row);
        }
        const sx = node.x * scale + offsetX;
        const sy = node.y * scale + offsetY;
        panel.style.left = Math.max(0, Math.min(sx + 20, host.clientWidth - 330)) + "px";
        panel.style.top = Math.max(0, Math.min(sy, host.clientHeight - 120)) + "px";
        host.appendChild(panel);
        masteryPicker = panel;
        hideTooltip();
    }
    // ---- 유사 노드 강조 (스탯 문구가 같은 노드 = 숫자만 다른 같은 계열) ----
    // "생명력 최대치 8% 증가" 와 "생명력 최대치 5% 증가" 를 같은 것으로 보기 위해 숫자를 # 로 정규화.
    const normalizeStat = (line) => line.replace(/[+\-]?\d+(?:\.\d+)?/g, "#").trim().toLowerCase();
    const statLinesOf = (node) => { var _a; return (isKorean && ((_a = node.statsKo) === null || _a === void 0 ? void 0 : _a.length) ? node.statsKo : node.stats); };
    function applySimilar(node) {
        searchHits.clear();
        const want = new Set(statLinesOf(node).map(normalizeStat).filter(Boolean));
        if (want.size) {
            for (const other of nodes) {
                if (other.type === "class" || !nodeVisible(other))
                    continue;
                for (const line of statLinesOf(other)) {
                    if (want.has(normalizeStat(line))) {
                        searchHits.add(other.id);
                        break;
                    }
                }
            }
        }
        draw();
        return searchHits.size;
    }
    // ---- 노드 우클릭 메뉴 ----
    let nodeMenu = null;
    function closeNodeMenu() {
        nodeMenu === null || nodeMenu === void 0 ? void 0 : nodeMenu.remove();
        nodeMenu = null;
    }
    function openNodeMenu(node, clientX, clientY) {
        var _a;
        closeNodeMenu();
        closeMasteryPicker();
        const host = canvas.parentElement;
        const panel = document.createElement("div");
        panel.className = "absolute z-30 w-56 rounded shadow-2xl border border-amber-700/60 bg-stone-900/97 overflow-hidden";
        const head = document.createElement("div");
        head.className =
            "border-b-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-xs px-4 py-1";
        head.textContent = isKorean && node.nameKo ? node.nameKo : node.name;
        panel.appendChild(head);
        const item = (label, action, disabled = false) => {
            const row = document.createElement("button");
            row.type = "button";
            row.disabled = disabled;
            row.className =
                "block w-full text-left px-4 py-1.5 text-xs border-b border-stone-800 " +
                    (disabled ? "text-base-content/30 cursor-not-allowed" : "text-sky-300 hover:bg-amber-900/40");
            row.textContent = label;
            if (!disabled) {
                row.addEventListener("click", () => {
                    closeNodeMenu();
                    action();
                });
            }
            panel.appendChild(row);
        };
        const allocated = highlighted.has(node.id);
        const hasStats = statLinesOf(node).some((l) => l.trim());
        item(isKorean ? "유사 노드 강조" : "Highlight similar", () => applySimilar(node), !hasStats);
        if (node.type !== "class") {
            if (allocated) {
                const cost = computeRemoval(node.id).length;
                item((isKorean ? "여기부터 해제" : "Refund from here") + (cost > 1 ? ` (−${cost})` : ""), () => toggleNode(node));
                if ((_a = node.masteryEffects) === null || _a === void 0 ? void 0 : _a.length)
                    item(isKorean ? "효과 변경" : "Change effect", () => openMasteryPicker(node));
            }
            else {
                const path = computeHoverPath(node.id);
                const reachable = canAllocate(node) || path.length > 1;
                const cost = canAllocate(node) ? 1 : path.length - 1;
                item((isKorean ? "여기까지 할당" : "Allocate to here") + (reachable ? ` (+${cost})` : ""), () => toggleNode(node), !reachable);
            }
        }
        item(isKorean ? "강조 해제" : "Clear highlight", () => {
            searchHits.clear();
            draw();
        });
        const rect = canvas.getBoundingClientRect();
        panel.style.left = Math.max(0, Math.min(clientX - rect.left, host.clientWidth - 230)) + "px";
        panel.style.top = Math.max(0, Math.min(clientY - rect.top, host.clientHeight - 140)) + "px";
        host.appendChild(panel);
        nodeMenu = panel;
        hideTooltip();
    }
    // 현재 호버 중인 노드 기준으로 미리보기를 다시 계산한다(할당 직후에도 즉시 반영).
    function refreshHoverState() {
        hoverPath = [];
        removalSet.clear();
        if (!hovered || !interactive)
            return;
        if (highlighted.has(hovered.id))
            for (const id of computeRemoval(hovered.id))
                removalSet.add(id);
        else
            hoverPath = computeHoverPath(hovered.id);
    }
    // ---- 포인트 카운터 ----
    // 패시브/전직 포인트를 따로 센다(공식: 패시브 123, 전직 8). 클래스 시작·전직 시작 노드는 무료.
    function updatePoints() {
        updateStatsPanel();
        const el = document.getElementById("poeTreePoints");
        if (!el)
            return;
        let passive = 0;
        let asc = 0;
        const root = rootNode();
        for (const id of highlighted) {
            const node = nodeById.get(id);
            if (!node || node.type === "class" || node.ascendancyStart)
                continue;
            if (id === root)
                continue; // 시작 노드는 무료(아틀라스 중앙 시작점 포함)
            if (node.ascendancy)
                asc++;
            else
                passive++;
        }
        el.replaceChildren();
        const add = (label, used, max) => {
            const span = document.createElement("span");
            span.className = "font-mono" + (max > 0 && used > max ? " text-error font-bold" : "");
            span.textContent = label + " " + used + (max > 0 ? " / " + max : "");
            el.appendChild(span);
        };
        add(isKorean ? "포인트" : "Points", passive, maxPoints);
        if (!isAtlas && maxAscPoints > 0)
            add(isKorean ? "· 전직" : "· Asc", asc, maxAscPoints);
    }
    // ---- 할당 스탯 합계 ----
    // 같은 계열 문장(숫자만 다름)을 하나로 묶어 수치를 더한다. 마스터리는 "고른 효과"만 반영.
    const NUM_RE = /[+\-]?\d+(?:\.\d+)?/g;
    function aggregateStats() {
        var _a;
        // 문장을 [고정문구 조각들] + [숫자들] 로 쪼개 조각열을 키로 묶고 숫자만 더한다(자리표시자 문자 불필요).
        const acc = new Map();
        const add = (lines) => {
            for (const raw of lines) {
                const line = raw.trim();
                if (!line)
                    continue;
                const nums = [];
                const signed = [];
                for (const m of line.match(NUM_RE) || []) {
                    nums.push(parseFloat(m));
                    signed.push(m[0] === "+" || m[0] === "-");
                }
                const parts = line.split(NUM_RE);
                const key = parts.join("\u0001");
                const cur = acc.get(key);
                if (cur) {
                    cur.count++;
                    nums.forEach((n, i) => (cur.nums[i] = (cur.nums[i] || 0) + n));
                }
                else {
                    acc.set(key, { parts, nums, signed, count: 1 });
                }
            }
        };
        for (const id of highlighted) {
            const node = nodeById.get(id);
            if (!node || node.type === "class")
                continue;
            if ((_a = node.masteryEffects) === null || _a === void 0 ? void 0 : _a.length) {
                const pick = masteryPicks.get(id);
                const eff = pick !== undefined ? node.masteryEffects.find((e) => e.id === pick) : undefined;
                if (eff)
                    add(effectLines(eff));
                continue;
            }
            add(statLinesOf(node));
        }
        const out = [];
        for (const v of acc.values()) {
            let text = v.parts[0] || "";
            for (let i = 0; i < v.nums.length; i++) {
                const n = v.nums[i];
                const body = Number.isInteger(n) ? String(n) : n.toFixed(1);
                text += (v.signed[i] && n > 0 ? "+" : "") + body + (v.parts[i + 1] || "");
            }
            out.push({ text, count: v.count });
        }
        return out.sort((a, b) => b.count - a.count || a.text.localeCompare(b.text));
    }
    function updateStatsPanel() {
        const panel = document.getElementById("poeTreeStatsBody");
        if (!panel)
            return;
        const rows = aggregateStats();
        panel.replaceChildren();
        if (!rows.length) {
            const empty = document.createElement("div");
            empty.className = "text-xs text-base-content/40 px-3 py-2";
            empty.textContent = isKorean ? "할당한 노드가 없습니다." : "No allocated nodes.";
            panel.appendChild(empty);
            return;
        }
        for (const row of rows) {
            const line = document.createElement("div");
            line.className = "flex items-baseline gap-2 px-3 py-0.5 text-xs";
            const text = document.createElement("span");
            text.className = "text-sky-300 flex-1";
            text.textContent = row.text;
            line.appendChild(text);
            if (row.count > 1) {
                const badge = document.createElement("span");
                badge.className = "text-[10px] font-mono text-base-content/40 shrink-0";
                badge.textContent = "×" + row.count;
                line.appendChild(badge);
            }
            panel.appendChild(line);
        }
    }
    // 전직/혈맹의 한글 표시명 — 트리 데이터엔 영문 id 뿐이라, 그 서브트리 시작 노드의 nameKo 를 쓴다
    // ("Guardian" → "가디언", "Aul" → "아울 혈맹"). 시작 노드가 없거나 영문 로케일이면 영문 그대로.
    const ascLabelCache = new Map();
    function ascendancyLabel(ascId, fallback) {
        const cached = ascLabelCache.get(ascId);
        if (cached)
            return cached;
        let label = fallback || ascId;
        if (isKorean) {
            const start = nodes.find((n) => n.ascendancy === ascId && n.ascendancyStart);
            if (start === null || start === void 0 ? void 0 : start.nameKo) {
                // 시작 노드 이름이 전직명과 다른 경우(Reliquarian→Scavenger, Warden→Warden of the Maji)
                // 한글명만 쓰면 정작 전직 이름이 사라져 사용자가 못 찾는다 → 전직명을 앞에 둔다.
                label = start.name === ascId ? start.nameKo : ascId + " (" + start.nameKo + ")";
            }
        }
        ascLabelCache.set(ascId, label);
        return label;
    }
    // ---- B: 직업/전직/혈맹 선택 ----
    function centerOnNode(id, atScale = 0.11) {
        const n = nodeById.get(id);
        if (!n)
            return;
        scale = atScale;
        offsetX = canvas.clientWidth / 2 - n.x * scale;
        offsetY = canvas.clientHeight / 2 - n.y * scale;
    }
    function fillAscendOptions(sel) {
        sel.replaceChildren();
        const none = document.createElement("option");
        none.value = "0";
        none.textContent = isKorean ? "전직 없음" : "No ascendancy";
        sel.appendChild(none);
        (classAsc[currentClassId] || []).forEach((asc, i) => {
            const o = document.createElement("option");
            o.value = String(i + 1);
            o.textContent = ascendancyLabel(asc);
            sel.appendChild(o);
        });
        sel.value = String(currentAscend);
    }
    function fillBloodlineOptions(sel) {
        sel.replaceChildren();
        const none = document.createElement("option");
        none.value = "0";
        none.textContent = isKorean ? "혈맹 없음" : "No bloodline";
        sel.appendChild(none);
        bloodlines.forEach((bl, i) => {
            const o = document.createElement("option");
            o.value = String(i + 1);
            o.textContent = ascendancyLabel(bl.id, bl.name);
            sel.appendChild(o);
        });
        sel.value = String(currentBloodline);
    }
    // 전직/혈맹을 바꾸면 이전 서브트리에 찍어둔 노드는 화면에서 사라지므로 함께 해제한다.
    function dropHiddenAllocations() {
        for (const id of Array.from(highlighted)) {
            const node = nodeById.get(id);
            if (node && !nodeVisible(node))
                highlighted.delete(id);
        }
        pruneOrphans();
    }
    function applyClass(classId) {
        currentClassId = classId;
        currentAscend = 0;
        highlighted.clear();
        const root = classStartByClassId.get(classId);
        if (root !== undefined) {
            highlighted.add(root);
            centerOnNode(root);
        }
        updatePoints();
        syncUrl();
        draw();
    }
    // ---- D: 호버 최단경로 미리보기 (할당집합 → 호버 노드, 미할당 통과) ----
    function computeHoverPath(targetId) {
        if (!interactive || highlighted.has(targetId) || highlighted.size === 0)
            return [];
        const prev = new Map();
        const visited = new Set(highlighted);
        let frontier = Array.from(highlighted);
        let depth = 0;
        while (frontier.length && depth < 60) {
            const next = [];
            for (const cur of frontier) {
                for (const nb of pathNeighbors(cur)) {
                    if (visited.has(nb))
                        continue;
                    visited.add(nb);
                    prev.set(nb, cur);
                    if (nb === targetId) {
                        const path = [targetId];
                        let p = prev.get(targetId);
                        while (p !== undefined) {
                            path.push(p);
                            if (highlighted.has(p))
                                break;
                            p = prev.get(p);
                        }
                        return path.reverse();
                    }
                    next.push(nb);
                }
            }
            frontier = next;
            depth++;
        }
        return [];
    }
    // ---- C: 검색 ----
    function applySearch(query) {
        searchHits.clear();
        const q = query.trim().toLowerCase();
        if (q) {
            let first = null;
            for (const node of nodes) {
                if (node.type === "class")
                    continue;
                const hay = (node.name +
                    " " +
                    (node.nameKo || "") +
                    " " +
                    node.stats.join(" ") +
                    " " +
                    (node.statsKo || []).join(" ")).toLowerCase();
                if (hay.indexOf(q) !== -1) {
                    searchHits.add(node.id);
                    if (!first)
                        first = node;
                }
            }
            if (first)
                centerOnNode(first.id);
        }
        draw();
    }
    function setupControls() {
        var _a, _b;
        // 검색·전체화면은 아틀라스 포함 항상 배선
        const searchInput = document.getElementById("poeTreeSearch");
        if (searchInput) {
            searchInput.addEventListener("input", () => applySearch(searchInput.value));
        }
        // 스탯 요약 패널 접기/펼치기
        const statsBtn = document.getElementById("poeTreeStatsToggle");
        const statsPanel = document.getElementById("poeTreeStats");
        if (statsBtn && statsPanel) {
            statsBtn.addEventListener("click", () => {
                statsPanel.classList.toggle("hidden");
                updateStatsPanel();
            });
        }
        // 트리 계산 — 찍은 노드를 그대로 PoB 엔진에 보내 실계산(장비/보조젬 없음)
        const evalBtn = document.getElementById("poeTreeEval");
        const evalPanel = document.getElementById("poeTreeEvalPanel");
        const evalBody = document.getElementById("poeTreeEvalBody");
        if (evalBtn && evalPanel && evalBody && !isAtlas) {
            const runEval = () => {
                const ids = Array.from(highlighted).filter((id) => {
                    const n = nodeById.get(id);
                    return n && n.type !== "class";
                });
                evalPanel.classList.remove("hidden");
                if (!ids.length) {
                    evalBody.textContent = isKorean ? "할당한 노드가 없습니다." : "No allocated nodes.";
                    return;
                }
                evalBody.textContent = isKorean ? "계산 중..." : "Calculating...";
                const body = new URLSearchParams();
                body.set("classId", String(currentClassId));
                body.set("nodes", ids.join(","));
                const asc = currentAscName();
                if (asc)
                    body.set("ascendancy", asc);
                // 마스터리는 "어떤 효과를 골랐는지"까지 보내야 PoB 가 스탯에 반영한다
                const picks = Array.from(masteryPicks)
                    .filter(([nodeId]) => highlighted.has(nodeId))
                    .map(([nodeId, effectId]) => nodeId + ":" + effectId);
                if (picks.length)
                    body.set("masteries", picks.join(","));
                // 주 스킬: datalist 에서 고른 표시명 → data-slug 로 변환(미입력이면 서버가 표준 스킬 사용)
                const gemInput = document.getElementById("poeTreeEvalGem");
                const typed = gemInput === null || gemInput === void 0 ? void 0 : gemInput.value.trim();
                if (typed) {
                    const opt = document.querySelector('#poeTreeGemList option[value="' + typed.replace(/"/g, '\\"') + '"]');
                    if (opt === null || opt === void 0 ? void 0 : opt.dataset.slug)
                        body.set("gem", opt.dataset.slug);
                }
                fetch("/poe/htmx/tree/stats", {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: body.toString(),
                })
                    .then((r) => r.text())
                    .then((html) => {
                    evalBody.innerHTML = html;
                })
                    .catch(() => {
                    evalBody.textContent = isKorean ? "요청 실패" : "Request failed";
                });
            };
            evalBtn.addEventListener("click", runEval);
            (_a = document.getElementById("poeTreeEvalRerun")) === null || _a === void 0 ? void 0 : _a.addEventListener("click", runEval);
            // 스킬 입력에서 엔터로도 재계산
            (_b = document.getElementById("poeTreeEvalGem")) === null || _b === void 0 ? void 0 : _b.addEventListener("keydown", (e) => {
                if (e.key === "Enter")
                    runEval();
            });
        }
        const fsBtn = document.getElementById("poeTreeFullscreen");
        if (fsBtn) {
            fsBtn.addEventListener("click", () => {
                // 컨트롤 바까지 포함한 껍데기를 전체화면으로 — 캔버스만 넣으면 전체화면에서 조작이 불가능하다
                const box = document.getElementById("poeTreeShell") || canvas.parentElement || canvas;
                if (document.fullscreenElement)
                    document.exitFullscreen();
                else if (box.requestFullscreen)
                    box.requestFullscreen();
            });
        }
        if (!interactive)
            return; // 이하 할당 관련은 편집(패시브 트리)만
        const classSel = document.getElementById("poeTreeClass");
        const ascSel = document.getElementById("poeTreeAscend");
        if (classSel) {
            classSel.value = String(currentClassId);
            classSel.addEventListener("change", () => {
                applyClass(Number(classSel.value));
                if (ascSel)
                    fillAscendOptions(ascSel);
            });
        }
        if (ascSel) {
            fillAscendOptions(ascSel);
            ascSel.addEventListener("change", () => {
                currentAscend = Number(ascSel.value);
                dropHiddenAllocations();
                focusAscendancy(currentAscName());
                updatePoints();
                syncUrl();
                draw();
            });
        }
        const bloodSel = document.getElementById("poeTreeBloodline");
        if (bloodSel) {
            fillBloodlineOptions(bloodSel);
            bloodSel.addEventListener("change", () => {
                currentBloodline = Number(bloodSel.value);
                dropHiddenAllocations();
                focusAscendancy(currentBloodlineId());
                updatePoints();
                syncUrl();
                draw();
            });
        }
        // 초기화 — 할당을 루트만 남기고 클리어
        const resetBtn = document.getElementById("poeTreeReset");
        if (resetBtn) {
            resetBtn.addEventListener("click", () => {
                highlighted.clear();
                const root = rootNode();
                if (root !== undefined)
                    highlighted.add(root);
                hoverPath = [];
                removalSet.clear();
                updatePoints();
                syncUrl();
                draw();
            });
        }
    }
    // 전직/혈맹을 고르면 시작 노드를 무료로 부여하고(게임과 동일) 그 서브트리로 화면을 옮긴다.
    // 전직 서브트리는 메인 트리보다 훨씬 촘촘해서 확대율을 따로 준다.
    function focusAscendancy(ascName) {
        if (!ascName)
            return;
        const start = nodes.find((n) => n.ascendancy === ascName && n.ascendancyStart);
        if (!start)
            return;
        highlighted.add(start.id);
        centerOnNode(start.id, 0.45);
    }
    // 노드의 궤도 각(월드). 위치식 x=group+r·sin(a), y=group-r·cos(a) → 캔버스각 θ=a-90°.
    function orbitAngle(node) {
        const per = skillsPerOrbit[node.orbit] || 1;
        return (2 * Math.PI * node.orbitIndex) / per - Math.PI / 2;
    }
    function draw() {
        const width = canvas.clientWidth;
        const height = canvas.clientHeight;
        const ratio = globalThis.devicePixelRatio || 1;
        if (canvas.width !== width * ratio || canvas.height !== height * ratio) {
            canvas.width = width * ratio;
            canvas.height = height * ratio;
        }
        context.setTransform(ratio, 0, 0, ratio, 0, 0);
        context.clearRect(0, 0, width, height);
        // 배경 — 어두운 청흑색 방사형
        const bg = context.createRadialGradient(width / 2, height / 2, 0, width / 2, height / 2, Math.max(width, height) * 0.8);
        bg.addColorStop(0, "#0f1520");
        bg.addColorStop(0.6, "#0a0e15");
        bg.addColorStop(1, "#05070b");
        context.fillStyle = bg;
        context.fillRect(0, 0, width, height);
        const pad = 200 * scale;
        const visible = (sx, sy, r) => sx > -r - pad && sy > -r - pad && sx < width + r + pad && sy < height + r + pad;
        // 1) 그룹 배경 (궤도 링 아트) — 충분히 확대됐을 때만
        if (scale > 0.02 && sprites.groupBackground) {
            for (const g of Object.values(groups)) {
                if (!g.background)
                    continue;
                const sx = g.x * scale + offsetX;
                const sy = g.y * scale + offsetY;
                const sp = sprites.groupBackground;
                const c = sp.coords[g.background.image];
                if (!c || !visible(sx, sy, (c.w / sp.zoom) * scale))
                    continue;
                const img = getSheet(sp.file);
                if (!img)
                    continue;
                const w = (c.w / sp.zoom) * scale;
                const h = (c.h / sp.zoom) * scale;
                context.globalAlpha = 0.55;
                if (g.background.isHalfImage) {
                    // 상단 반쪽 이미지 → 아래로 상하 반전해 전체 링 구성
                    context.drawImage(img, c.x, c.y, c.w, c.h, sx - w / 2, sy - h, w, h);
                    context.save();
                    context.translate(sx, sy);
                    context.scale(1, -1);
                    context.drawImage(img, c.x, c.y, c.w, c.h, -w / 2, -h, w, h);
                    context.restore();
                }
                else {
                    context.drawImage(img, c.x, c.y, c.w, c.h, sx - w / 2, sy - h / 2, w, h);
                }
            }
            context.globalAlpha = 1;
        }
        // 2) 연결선 — 같은 group·orbit 이면 궤도 따라 arc, 아니면 직선
        function tracePath(onlyAllocated) {
            context.beginPath();
            for (const [fromId, toId] of edges) {
                if (onlyAllocated && (!highlighted.has(fromId) || !highlighted.has(toId)))
                    continue;
                const from = nodeById.get(fromId);
                const to = nodeById.get(toId);
                if (!from || !to)
                    continue;
                // 전직 서브트리는 메인 트리에서 멀리 떨어진 좌표에 배치돼 있다. 데이터상 연결(전직시작↔클래스시작 등)을
                // 그대로 그리면 화면을 가로지르는 긴 선이 생겨서, 공식 뷰어처럼 전직↔메인/다른전직 엣지는 그리지 않는다.
                if ((from.ascendancy || null) !== (to.ascendancy || null))
                    continue;
                if (!nodeVisible(from) || !nodeVisible(to))
                    continue;
                const ax = from.x * scale + offsetX;
                const ay = from.y * scale + offsetY;
                if (from.group === to.group && from.orbit === to.orbit && from.orbit > 0) {
                    const g = groups[from.group];
                    if (g) {
                        const cx = g.x * scale + offsetX;
                        const cy = g.y * scale + offsetY;
                        const r = orbitRadii[from.orbit] * scale;
                        let a1 = orbitAngle(from);
                        let a2 = orbitAngle(to);
                        let d = a2 - a1;
                        while (d > Math.PI)
                            d -= 2 * Math.PI;
                        while (d < -Math.PI)
                            d += 2 * Math.PI;
                        context.moveTo(ax, ay);
                        context.arc(cx, cy, r, a1, a1 + d, d < 0);
                        continue;
                    }
                }
                context.moveTo(ax, ay);
                context.lineTo(to.x * scale + offsetX, to.y * scale + offsetY);
            }
        }
        context.lineCap = "round";
        context.strokeStyle = "rgba(130,140,155,0.30)";
        context.lineWidth = Math.max(0.4, 16 * scale);
        tracePath(false);
        context.stroke();
        if (hasHighlight()) {
            context.save();
            context.shadowColor = "rgba(224,180,90,0.9)";
            context.shadowBlur = Math.max(4, 40 * scale);
            context.strokeStyle = "rgba(232,194,108,0.95)";
            context.lineWidth = Math.max(1, 22 * scale);
            tracePath(true);
            context.stroke();
            context.restore();
        }
        // 2b) 호버 경로 미리보기 — 반투명 골드 점선
        if (hoverPath.length > 1) {
            context.save();
            context.setLineDash([Math.max(2, 10 * scale), Math.max(2, 8 * scale)]);
            context.strokeStyle = "rgba(240,208,137,0.7)";
            context.lineWidth = Math.max(1, 14 * scale);
            context.beginPath();
            for (let i = 0; i < hoverPath.length; i++) {
                const n = nodeById.get(hoverPath[i]);
                if (!n)
                    continue;
                const px = n.x * scale + offsetX;
                const py = n.y * scale + offsetY;
                if (i === 0)
                    context.moveTo(px, py);
                else
                    context.lineTo(px, py);
            }
            context.stroke();
            context.restore();
        }
        // 2c) 해제 미리보기 — 할당 노드에 마우스를 올리면 함께 사라질 경로를 붉은 점선으로
        if (removalSet.size > 1) {
            context.save();
            context.setLineDash([Math.max(2, 10 * scale), Math.max(2, 8 * scale)]);
            context.strokeStyle = "rgba(232,110,96,0.85)";
            context.lineWidth = Math.max(1, 16 * scale);
            context.beginPath();
            for (const [fromId, toId] of edges) {
                if (!removalSet.has(fromId) || !removalSet.has(toId))
                    continue;
                const from = nodeById.get(fromId);
                const to = nodeById.get(toId);
                if (!from || !to)
                    continue;
                context.moveTo(from.x * scale + offsetX, from.y * scale + offsetY);
                context.lineTo(to.x * scale + offsetX, to.y * scale + offsetY);
            }
            context.stroke();
            context.restore();
        }
        // 3) 노드 — 아이콘(원 클립) + 프레임 스프라이트. 저줌에선 점만.
        // 지금 찍을 수 있는 노드(할당집합에 인접) 를 한 번에 모아 프레임 상태에 쓴다.
        const canAllocSet = new Set();
        if (interactive) {
            for (const id of highlighted) {
                for (const nb of pathNeighbors(id))
                    if (!highlighted.has(nb))
                        canAllocSet.add(nb);
            }
        }
        for (const node of nodes) {
            if (!nodeVisible(node))
                continue; // 선택하지 않은 전직/혈맹 서브트리는 숨김
            const sx = node.x * scale + offsetX;
            const sy = node.y * scale + offsetY;
            const rWorld = nodeRadiusWorld[node.type] || 45;
            const rScreen = rWorld * scale;
            if (!visible(sx, sy, rScreen))
                continue;
            const isAllocated = highlighted.has(node.id);
            context.globalAlpha = node.ascendancy ? 0.85 : 1;
            // 저줌: 스프라이트 대신 점(성능)
            if (rScreen < 5) {
                context.beginPath();
                context.arc(sx, sy, Math.max(1, rScreen), 0, Math.PI * 2);
                context.fillStyle = isAllocated ? "#f0d089" : node.type === "notable" ? "#c8a24e" : node.type === "keystone" || node.type === "wormhole" ? "#cf6642" : "#6b7583";
                context.fill();
                continue;
            }
            if (isAllocated) {
                context.save();
                context.shadowColor = "rgba(240,208,137,0.9)";
                context.shadowBlur = rScreen * 0.9;
            }
            if (node.type === "mastery") {
                // 마스터리는 효과를 고른 것만 선명하게
                blit(masteryPicks.has(node.id) ? "mastery" : iconSheetFor("mastery", false) || "mastery", node.icon || "", node.x, node.y);
            }
            else if (node.icon) {
                const sheet = iconSheetFor(node.type, isAllocated);
                if (sheet)
                    blit(sheet, node.icon, node.x, node.y, true);
            }
            const fc = frameCoord(node, isAllocated, canAllocSet.has(node.id));
            if (fc)
                blit("frame", fc, node.x, node.y);
            if (isAllocated)
                context.restore();
            // 해제 대상 — 붉은 테두리
            if (removalSet.has(node.id)) {
                context.globalAlpha = 1;
                context.save();
                context.shadowColor = "rgba(232,110,96,0.95)";
                context.shadowBlur = rScreen * 0.8;
                context.beginPath();
                context.arc(sx, sy, rScreen + Math.max(2, 7 * scale), 0, Math.PI * 2);
                context.strokeStyle = "#e86e60";
                context.lineWidth = Math.max(2, 6 * scale);
                context.stroke();
                context.restore();
            }
            // 검색 매칭 — 청록 글로우 테두리
            if (searchHits.has(node.id)) {
                context.globalAlpha = 1;
                context.save();
                context.shadowColor = "rgba(78,201,212,0.95)";
                context.shadowBlur = rScreen * 0.8;
                context.beginPath();
                context.arc(sx, sy, rScreen + Math.max(2, 7 * scale), 0, Math.PI * 2);
                context.strokeStyle = "#4ec9d4";
                context.lineWidth = Math.max(2, 6 * scale);
                context.stroke();
                context.restore();
            }
            if (node === hovered) {
                context.globalAlpha = 1;
                context.beginPath();
                context.arc(sx, sy, rScreen + Math.max(2, 6 * scale), 0, Math.PI * 2);
                context.strokeStyle = "#ffffff";
                context.lineWidth = 2;
                context.stroke();
            }
        }
        context.globalAlpha = 1;
    }
    function findNodeAt(screenX, screenY) {
        let best = null;
        let bestDistance = Infinity;
        for (const node of nodes) {
            if (!nodeVisible(node))
                continue;
            const radius = Math.max((nodeRadiusWorld[node.type] || 45) * scale, 6);
            const dx = node.x * scale + offsetX - screenX;
            const dy = node.y * scale + offsetY - screenY;
            const distance = Math.hypot(dx, dy);
            if (distance <= radius + 2 && distance < bestDistance) {
                best = node;
                bestDistance = distance;
            }
        }
        return best;
    }
    function showTooltip(node, clientX, clientY) {
        var _a;
        if (!tooltip)
            return;
        tooltip.replaceChildren();
        // 공식 뷰어식: 금색테 헤더(제목 중앙) + 어두운 본문(청색 스탯)
        const displayName = isKorean && node.nameKo ? node.nameKo : node.name;
        const header = document.createElement("div");
        header.className =
            "border-y-2 border-amber-600/70 bg-gradient-to-b from-stone-700 to-stone-900 text-amber-100 text-center font-bold text-sm px-6 py-1.5";
        header.textContent = displayName + (node.ascendancy ? " (" + ascendancyLabel(node.ascendancy) + ")" : "");
        tooltip.appendChild(header);
        // 마스터리: 고른 효과가 있으면 그 문장을, 없으면 선택지 개수를 보여준다
        let displayStats = isKorean && node.statsKo && node.statsKo.length ? node.statsKo : node.stats;
        if ((_a = node.masteryEffects) === null || _a === void 0 ? void 0 : _a.length) {
            const picked = masteryPicks.get(node.id);
            const eff = picked !== undefined ? node.masteryEffects.find((e) => e.id === picked) : undefined;
            displayStats = eff
                ? effectLines(eff)
                : [(isKorean ? "효과 " : "") + node.masteryEffects.length + (isKorean ? "개 중 선택 — 클릭" : " effects — click to pick")];
        }
        if (displayStats.length) {
            const body = document.createElement("div");
            body.className = "bg-stone-900/95 px-4 py-2 border-x border-b border-amber-900/50";
            for (const stat of displayStats) {
                const line = document.createElement("div");
                line.className = "text-xs text-sky-300 whitespace-pre-line leading-5";
                line.textContent = stat;
                body.appendChild(line);
            }
            tooltip.appendChild(body);
        }
        // 비용 안내 — 미할당은 경로까지 몇 포인트 드는지, 할당은 해제 시 몇 개가 함께 빠지는지
        if (interactive && node.type !== "class") {
            let costText = "";
            let costClass = "";
            if (highlighted.has(node.id)) {
                if (removalSet.size) {
                    costText = (isKorean ? "해제 −" : "Refund −") + removalSet.size + (isKorean ? " 포인트" : " points");
                    costClass = "text-rose-400";
                }
            }
            else if (hoverPath.length > 1) {
                costText = "+" + (hoverPath.length - 1) + (isKorean ? " 포인트" : " points");
                costClass = "text-amber-300";
            }
            else if (highlighted.size > 0) {
                costText = isKorean ? "연결 불가" : "Unreachable";
                costClass = "text-base-content/50";
            }
            if (costText) {
                const foot = document.createElement("div");
                foot.className = "bg-stone-950/95 px-4 py-1 border-x border-b border-amber-900/50 text-[11px] font-mono " + costClass;
                foot.textContent = costText;
                tooltip.appendChild(foot);
            }
        }
        const parentRect = tooltip.parentElement.getBoundingClientRect();
        tooltip.style.left = Math.min(clientX - parentRect.left + 14, parentRect.width - 280) + "px";
        tooltip.style.top = clientY - parentRect.top + 14 + "px";
        tooltip.classList.remove("hidden");
    }
    function hideTooltip() {
        tooltip === null || tooltip === void 0 ? void 0 : tooltip.classList.add("hidden");
    }
    // 팬 + 클릭 할당(드래그와 클릭 구분: 이동량 작으면 클릭)
    let dragging = false;
    let dragMoved = false;
    let downX = 0;
    let downY = 0;
    let lastX = 0;
    let lastY = 0;
    canvas.addEventListener("mousedown", (event) => {
        if (event.button !== 0)
            return; // 좌클릭만 팬/할당 — 우클릭은 contextmenu 가 처리
        dragging = true;
        dragMoved = false;
        downX = event.clientX;
        downY = event.clientY;
        lastX = event.clientX;
        lastY = event.clientY;
        canvas.style.cursor = "grabbing";
    });
    globalThis.addEventListener("mouseup", (event) => {
        if (dragging && !dragMoved) {
            // 클릭 — 노드 토글 (열려 있던 팝업/메뉴는 먼저 닫는다)
            closeMasteryPicker();
            closeNodeMenu();
            const rect = canvas.getBoundingClientRect();
            const node = findNodeAt(event.clientX - rect.left, event.clientY - rect.top);
            if (node)
                toggleNode(node);
        }
        dragging = false;
        canvas.style.cursor = "grab";
    });
    // 우클릭 — 노드 메뉴 (브라우저 기본 메뉴는 막는다)
    canvas.addEventListener("contextmenu", (event) => {
        event.preventDefault();
        const rect = canvas.getBoundingClientRect();
        const node = findNodeAt(event.clientX - rect.left, event.clientY - rect.top);
        if (node)
            openNodeMenu(node, event.clientX, event.clientY);
        else
            closeNodeMenu();
    });
    globalThis.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeNodeMenu();
            closeMasteryPicker();
        }
    });
    canvas.addEventListener("mousemove", (event) => {
        if (dragging) {
            if (Math.abs(event.clientX - downX) + Math.abs(event.clientY - downY) > 4)
                dragMoved = true;
            offsetX += event.clientX - lastX;
            offsetY += event.clientY - lastY;
            lastX = event.clientX;
            lastY = event.clientY;
            hideTooltip();
            scheduleDraw(); // 고빈도 mousemove 를 rAF 로 병합(즉시 draw 하면 이동 1건당 전체 렌더)
            return;
        }
        const rect = canvas.getBoundingClientRect();
        const node = findNodeAt(event.clientX - rect.left, event.clientY - rect.top);
        if (node !== hovered) {
            hovered = node;
            hoverPath = [];
            removalSet.clear();
            if (node && interactive) {
                // 할당 노드면 "해제 시 함께 빠질 집합", 미할당이면 "여기까지 최단 경로"
                if (highlighted.has(node.id))
                    for (const id of computeRemoval(node.id))
                        removalSet.add(id);
                else
                    hoverPath = computeHoverPath(node.id);
            }
            scheduleDraw();
        }
        if (node && (node.name || node.stats.length))
            showTooltip(node, event.clientX, event.clientY);
        else
            hideTooltip();
    });
    canvas.addEventListener("mouseleave", () => {
        hovered = null;
        hoverPath = [];
        removalSet.clear();
        hideTooltip();
        scheduleDraw();
    });
    // 줌 (커서 고정)
    canvas.addEventListener("wheel", (event) => {
        event.preventDefault();
        const rect = canvas.getBoundingClientRect();
        const mouseX = event.clientX - rect.left;
        const mouseY = event.clientY - rect.top;
        const factor = Math.pow(1.0015, -event.deltaY);
        const nextScale = Math.min(0.6, Math.max(0.008, scale * factor));
        offsetX = mouseX - ((mouseX - offsetX) / scale) * nextScale;
        offsetY = mouseY - ((mouseY - offsetY) / scale) * nextScale;
        scale = nextScale;
        hideTooltip();
        scheduleDraw();
    }, { passive: false });
    globalThis.addEventListener("resize", scheduleDraw);
    // 매니페스트 → 트리 데이터 순으로 로드
    function loadTree() {
        // no-cache = 항상 서버에 재검증(ETag) — 게임 패치로 트리 JSON 이 바뀌어도 옛 캐시를 물지 않는다.
        // (1MB 라 no-store 로 매번 새로 받지는 않고 304 로 재사용)
        return fetch(treeSrc, { cache: "no-cache" })
            .then((response) => response.json())
            .then((data) => {
            var _a, _b, _c;
            nodes = data.nodes;
            edges = data.edges;
            groups = data.groups || {};
            if (data.constants) {
                orbitRadii = data.constants.orbitRadii || orbitRadii;
                skillsPerOrbit = data.constants.skillsPerOrbit || skillsPerOrbit;
            }
            // 직업별 전직 목록·혈맹·최대 포인트는 트리 데이터가 그대로 들고 있다(하드코딩 금지)
            classAsc = (data.classes || []).map((cls) => cls.ascendancies || []);
            bloodlines = data.bloodlines || [];
            // 아틀라스는 원본 root 노드의 out(지도 중앙 1개)이 시작점 — 여기서부터 이어야 할당된다
            atlasRoot = isAtlas && ((_a = data.startNodes) === null || _a === void 0 ? void 0 : _a.length) ? data.startNodes[0] : null;
            maxPoints = ((_b = data.points) === null || _b === void 0 ? void 0 : _b.totalPoints) || 0;
            maxAscPoints = ((_c = data.points) === null || _c === void 0 ? void 0 : _c.ascendancyPoints) || 0;
            for (const node of nodes)
                nodeById.set(node.id, node);
            // 인접 그래프(양방향) — 클릭 할당 연결성 검증용
            for (const [a, b] of edges) {
                if (!adjacency.has(a))
                    adjacency.set(a, []);
                if (!adjacency.has(b))
                    adjacency.set(b, []);
                adjacency.get(a).push(b);
                adjacency.get(b).push(a);
            }
            // 클래스 시작노드 매핑(classId → 노드 id)
            for (const node of nodes) {
                if (node.type === "class" && CLASS_START_CLASSID[node.name] !== undefined) {
                    classStartByClassId.set(CLASS_START_CLASSID[node.name], node.id);
                }
            }
            applyPendingClass(); // ?class=/?asc= (레거시 nodes 링크) 를 루트 결정 전에 반영
            // 편집 모드: 현재 직업의 시작노드를 루트로 항상 할당
            if (interactive) {
                const root = rootNode();
                if (root !== undefined)
                    highlighted.add(root);
            }
            let bounds = data.bounds;
            // 할당 영역에 맞춰 확대하는 건 "실제 트리를 불러왔을 때"만. 시작 노드 하나뿐이면
            // 그 점에 맞춰 과하게 확대돼 이웃이 화면 밖으로 나간다 → 전체 트리를 보여준다.
            if (highlighted.size > 1) {
                const allocated = nodes.filter((node) => highlighted.has(node.id));
                if (allocated.length) {
                    const padding = 2200;
                    bounds = {
                        minX: Math.min(...allocated.map((n) => n.x)) - padding,
                        minY: Math.min(...allocated.map((n) => n.y)) - padding,
                        maxX: Math.max(...allocated.map((n) => n.x)) + padding,
                        maxY: Math.max(...allocated.map((n) => n.y)) + padding,
                    };
                }
            }
            const width = canvas.clientWidth;
            const height = canvas.clientHeight;
            scale = Math.min(0.6, Math.min(width / (bounds.maxX - bounds.minX), height / (bounds.maxY - bounds.minY)) * 0.95);
            offsetX = width / 2 - ((bounds.minX + bounds.maxX) / 2) * scale;
            offsetY = height / 2 - ((bounds.minY + bounds.maxY) / 2) * scale;
            updatePoints();
            draw();
            setupControls();
        })
            .catch((error) => console.warn("passive tree load failed", error));
    }
    // 매니페스트는 항상 최신으로(no-store) — 시트 URL 에 박힌 ?v 로 시트 캐시가 갈리므로,
    // 매니페스트만 새로 받으면 재생성된 시트를 일반 새로고침으로도 바로 받는다(깨진 캐시 회피).
    fetch(spritesSrc, { cache: "no-store" })
        .then((r) => r.json())
        .then((m) => {
        sprites = m;
        computeRadii();
        // 시트 미리 로드 — 큰 JPEG(skills)가 프레임보다 늦게 떠서 "아이콘 없는 빈 프레임"으로
        // 보이는 창을 없앤다. 로드 완료 시 각자 scheduleDraw 로 재그림.
        const seen = new Set();
        for (const sp of Object.values(sprites)) {
            if (sp && sp.file && !seen.has(sp.file)) {
                seen.add(sp.file);
                getSheet(sp.file);
            }
        }
    })
        .catch((e) => console.warn("tree sprites load failed", e))
        .then(loadTree);
})();
