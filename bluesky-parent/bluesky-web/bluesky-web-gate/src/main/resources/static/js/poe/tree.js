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
    // 노드 타입 → 아이콘 시트 키(항상 Active=선명) / 프레임 coord 키(할당 상태별)
    const ICON_SHEET = {
        normal: "normalActive",
        notable: "notableActive",
        keystone: "keystoneActive",
        wormhole: "wormholeActive",
        mastery: "mastery",
    };
    function frameCoord(type, allocated) {
        const st = allocated ? "Allocated" : "Unallocated";
        switch (type) {
            case "notable":
                return "NotableFrame" + st;
            case "keystone":
            case "wormhole":
                return "KeystoneFrame" + st;
            case "jewel":
                return "JewelFrame" + st;
            case "normal":
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
    let scale = 0.03;
    let offsetX = 0;
    let offsetY = 0;
    let hovered = null;
    const highlighted = new Set();
    for (const token of (new URLSearchParams(globalThis.location.search).get("nodes") || "").split(",")) {
        const id = Number(token);
        if (Number.isFinite(id) && id > 0)
            highlighted.add(id);
    }
    const hasHighlight = highlighted.size > 0;
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
        context.strokeStyle = hasHighlight ? "rgba(120,130,145,0.10)" : "rgba(130,140,155,0.30)";
        context.lineWidth = Math.max(0.4, 16 * scale);
        tracePath(false);
        context.stroke();
        if (hasHighlight) {
            context.save();
            context.shadowColor = "rgba(224,180,90,0.9)";
            context.shadowBlur = Math.max(4, 40 * scale);
            context.strokeStyle = "rgba(232,194,108,0.95)";
            context.lineWidth = Math.max(1, 22 * scale);
            tracePath(true);
            context.stroke();
            context.restore();
        }
        // 3) 노드 — 아이콘(원 클립) + 프레임 스프라이트. 저줌에선 점만.
        for (const node of nodes) {
            const sx = node.x * scale + offsetX;
            const sy = node.y * scale + offsetY;
            const rWorld = nodeRadiusWorld[node.type] || 45;
            const rScreen = rWorld * scale;
            if (!visible(sx, sy, rScreen))
                continue;
            const isAllocated = highlighted.has(node.id);
            const dim = hasHighlight && !isAllocated;
            context.globalAlpha = dim ? (node.ascendancy ? 0.18 : 0.4) : node.ascendancy ? 0.85 : 1;
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
                blit("mastery", node.icon || "", node.x, node.y);
            }
            else if (node.icon) {
                const sheet = ICON_SHEET[node.type];
                if (sheet)
                    blit(sheet, node.icon, node.x, node.y, true);
            }
            const fc = frameCoord(node.type, isAllocated || !hasHighlight ? isAllocated : false);
            if (fc)
                blit("frame", fc, node.x, node.y);
            else if (node.type === "jewel")
                blit("frame", "JewelFrameUnallocated", node.x, node.y);
            if (isAllocated)
                context.restore();
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
        if (!tooltip)
            return;
        tooltip.replaceChildren();
        const title = document.createElement("div");
        title.className = "font-bold text-sm mb-1";
        const displayName = isKorean && node.nameKo ? node.nameKo : node.name;
        title.textContent = displayName + (node.ascendancy ? " (" + node.ascendancy + ")" : "");
        tooltip.appendChild(title);
        const displayStats = isKorean && node.statsKo && node.statsKo.length ? node.statsKo : node.stats;
        for (const stat of displayStats) {
            const line = document.createElement("div");
            line.className = "text-xs text-sky-300/90 whitespace-pre-line leading-5";
            line.textContent = stat;
            tooltip.appendChild(line);
        }
        const parentRect = tooltip.parentElement.getBoundingClientRect();
        tooltip.style.left = Math.min(clientX - parentRect.left + 14, parentRect.width - 280) + "px";
        tooltip.style.top = clientY - parentRect.top + 14 + "px";
        tooltip.classList.remove("hidden");
    }
    function hideTooltip() {
        tooltip === null || tooltip === void 0 ? void 0 : tooltip.classList.add("hidden");
    }
    // 팬
    let dragging = false;
    let lastX = 0;
    let lastY = 0;
    canvas.addEventListener("mousedown", (event) => {
        dragging = true;
        lastX = event.clientX;
        lastY = event.clientY;
        canvas.style.cursor = "grabbing";
    });
    globalThis.addEventListener("mouseup", () => {
        dragging = false;
        canvas.style.cursor = "grab";
    });
    canvas.addEventListener("mousemove", (event) => {
        if (dragging) {
            offsetX += event.clientX - lastX;
            offsetY += event.clientY - lastY;
            lastX = event.clientX;
            lastY = event.clientY;
            hideTooltip();
            draw();
            return;
        }
        const rect = canvas.getBoundingClientRect();
        const node = findNodeAt(event.clientX - rect.left, event.clientY - rect.top);
        if (node !== hovered) {
            hovered = node;
            draw();
        }
        if (node && (node.name || node.stats.length))
            showTooltip(node, event.clientX, event.clientY);
        else
            hideTooltip();
    });
    canvas.addEventListener("mouseleave", () => {
        hovered = null;
        hideTooltip();
        draw();
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
        draw();
    }, { passive: false });
    globalThis.addEventListener("resize", draw);
    // 매니페스트 → 트리 데이터 순으로 로드
    function loadTree() {
        return fetch(treeSrc)
            .then((response) => response.json())
            .then((data) => {
            nodes = data.nodes;
            edges = data.edges;
            groups = data.groups || {};
            if (data.constants) {
                orbitRadii = data.constants.orbitRadii || orbitRadii;
                skillsPerOrbit = data.constants.skillsPerOrbit || skillsPerOrbit;
            }
            for (const node of nodes)
                nodeById.set(node.id, node);
            let bounds = data.bounds;
            if (hasHighlight) {
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
            draw();
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
