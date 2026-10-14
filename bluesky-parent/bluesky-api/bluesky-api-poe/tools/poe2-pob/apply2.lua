-- 가이드 추천 한 건을 PoB 안에서 **실제로 적용**하고 저장 — PoE2 자동 다듬기(Poe2RefineService)가 쓴다.
-- 원본은 QA 검증기 ~/.bluesky-qa/poe2-apply-change.lua(가이드 추천 354건 실적용 = 예측 일치, 09-30) — 두 곳을 고칠 땐 같이 볼 것.
-- 사용: (cwd = pob2-src/src) luajit apply2.lua <build.xml> <change.json> <out.xml>   → 성공 시 "@@APPLIED@@<길이>"
-- change.json: {type:"unique", slot, raw} | {type:"mod", slot, lines:[...]} | {type:"gem", from:"약한 보조 영문명", gemId}
--              | {type:"nodes", ids:[...]} | {type:"remove", slot} | {type:"noop"}(저장 왕복만)
--              | {type:"respec", remove:노드, add:노드}(10-14 패시브 재분배 — 본 트리 특화 하나와 그것만 잇던 길을 빼고, 그 점수로 다른 특화까지 길을 찍는다.
--                 본 트리 사용 점수가 늘면 실패 = 후보 탈락)
-- 저장 뒤 계산은 이 파일이 아니라 calc.lua 를 **새 프로세스**로 돌려서 한다(적용 경로와 계산 경로를 떼어 놓는다).
local inPath, changePath, outPath = ...
package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
package.cpath = package.cpath .. ";../runtime/?.dll"
local ok, err = pcall(function()
	dofile("HeadlessWrapper.lua")
	local f = assert(io.open(inPath, "rb")); local xml = f:read("*a"); f:close()
	local cf = assert(io.open(changePath, "rb")); local ch = require("dkjson").decode(cf:read("*a")); cf:close()
	loadBuildFromXML(xml)
	local function makeItem(text)
		local ok1, it = pcall(new, "Item", text)
		if ok1 and it and it.name then return it end
		return new("Item"):Item(text)
	end
	local function equip(slotName, item)
		local slot = assert(build.itemsTab.slots[slotName], "칸 없음 " .. tostring(slotName))
		build.itemsTab:AddItem(item, true)
		slot:SetSelItemId(item.id)
	end
	if ch.type == "unique" then
		-- PoB 고유 DB 는 new("Item", raw, "UNIQUE", true) — 셋째 인자 highQuality = 품질 20%. 가이드(비교 계산기)도 그 아이템을 끼운다.
		--   ch.quality20 이면 같게, 아니면 품질 0(떨어진 그대로) — 두 경우를 나눠 잰다
		if ch.quality20 then
			local okq, it = pcall(new, "Item", ch.raw, "UNIQUE", true)
			equip(ch.slot, (okq and it and it.name) and it or new("Item"):Item(ch.raw, "UNIQUE", true))
		else
			equip(ch.slot, makeItem(ch.raw))
		end
	elseif ch.type == "mod" then
		local slot = assert(build.itemsTab.slots[ch.slot])
		local cur = assert(build.itemsTab.items[slot.selItemId], "빈 칸")
		equip(ch.slot, makeItem(cur:BuildRaw() .. "\n" .. table.concat(ch.lines, "\n")))
	elseif ch.type == "remove" then
		build.itemsTab.slots[ch.slot]:SetSelItemId(0)
	elseif ch.type == "gem" then
		local group = build.skillsTab.socketGroupList[build.mainSocketGroup or 1]
		local target
		for _, g in ipairs(group.gemList or {}) do
			if g.nameSpec == ch.from then target = g end
		end
		assert(target, "보조젬 못 찾음 " .. tostring(ch.from))
		local newId
		for id, gd in pairs(build.data.gems) do
			if gd.gameId == ch.gemId or id == ch.gemId or gd.name == ch.name then newId = id end
		end
		assert(newId, "젬 id 못 찾음 " .. tostring(ch.gemId))
		target.gemId, target.skillId, target.nameSpec = newId, nil, nil
		build.skillsTab:ProcessSocketGroup(group)
		target.level = build.skillsTab:ProcessGemLevel(target.gemData)
	elseif ch.type == "nodes" then
		-- 가이드가 준 경로 노드만 정확히 찍는다 — AllocNode 를 노드마다 부르면 PoB 가 고른 다른 경로까지 찍혀 노드가 늘었다.
		-- 능력치 노드는 AllocNode 와 같은 규칙(마지막 고른 능력치, 없으면 힘)으로 바꾼다.
		local before = 0
		for _ in pairs(build.spec.allocNodes) do before = before + 1 end
		for _, id in ipairs(ch.ids) do
			local node = assert(build.spec.nodes[id], "노드 없음 " .. id)
			node.alloc = true
			node.allocMode = 0
			build.spec.allocNodes[id] = node
			if node.isAttribute then build.spec:SwitchAttributeNode(id, build.spec.attributeIndex or 1) end
		end
		build.spec:BuildAllDependsAndPaths()
		local after = 0
		for _ in pairs(build.spec.allocNodes) do after = after + 1 end
		assert(after - before == #ch.ids, string.format("할당 수 어긋남 %d → %d (기대 +%d)", before, after, #ch.ids))
	elseif ch.type == "respec" then
		local spec = build.spec
		-- mode = 뺄 노드의 점수 칸(0 본 트리 · 1/2 무기 세트, 10-14 C316) — 같은 칸 점수로 찍고, 그 칸 사용 점수가 늘면 거절
		local target = assert(spec.nodes[ch.remove], "노드 없음 " .. tostring(ch.remove))
		local mode = target.allocMode or 0
		local function usedMain()
			local u, _, _, _, w1, w2 = spec:CountAllocNodes()
			return mode == 1 and w1 or mode == 2 and w2 or u - w1 - w2
		end
		local before = usedMain()
		assert(target.alloc and not target.ascendancyName, "재분배: 찍힌 노드가 아님 " .. tostring(ch.remove))
		-- 그 특화만 잇던 길 — 특화에서 트리 쪽으로, 찍힌 이웃이 둘뿐인 작은 노드가 이어지는 동안(갈림길 · 다른 특화에서 멈춘다)
		local function allocNeighbors(n)
			local out = {}
			for _, o in ipairs(n.linked or {}) do if o.alloc then out[#out + 1] = o end end
			return out
		end
		-- 특화를 빼고(PoB 가 거기 매달린 것도 함께 뺀다), 빈 자리 둘레에서 **끝에 매달린 작은 노드**(찍힌 이웃 하나 · 본 트리 · 특화/핵심/소켓 아님)를
		-- 걷어 내기를 되풀이한다 — 특화가 작은 노드 무리 가운데 있으면 한 줄 길 걷기로는 1점만 풀렸다(10-14 실측: 131 → 133 초과). 둘레에서만
		-- 걷으므로 다른 곳에 일부러 찍은 작은 노드는 건드리지 않는다
		local frontier = {}
		for _, d in ipairs(target.depends or { target }) do
			for _, o in ipairs(allocNeighbors(d)) do frontier[#frontier + 1] = o end
		end
		spec:DeallocNode(target)
		local pruned = true
		while pruned do
			pruned = false
			local nextFrontier = {}
			for _, n in ipairs(frontier) do
				local nbs = allocNeighbors(n)
				if n.alloc and n.type == "Normal" and (n.allocMode or 0) == mode and not n.ascendancyName and #nbs == 1 then
					spec:DeallocNode(n)
					nextFrontier[#nextFrontier + 1] = nbs[1]
					pruned = true
				end
			end
			frontier = nextFrontier
		end
		local freed = before - usedMain()
		assert(freed <= 10, string.format("재분배: 다리 노드 — 빼면 %d점이 풀린다(매달린 트리가 끊김)", freed))
		local addNode = assert(spec.nodes[ch.add], "노드 없음 " .. tostring(ch.add))
		assert(not addNode.alloc and addNode.path and addNode.pathDist and addNode.pathDist > 0, "재분배: 찍을 수 없는 노드 " .. tostring(ch.add))
		for _, n in ipairs(addNode.path) do
			if not n.alloc then
				n.alloc = true
				n.allocMode = mode
				spec.allocNodes[n.id] = n
				if n.isAttribute then spec:SwitchAttributeNode(n.id, spec.attributeIndex or 1) end
			end
		end
		spec:BuildAllDependsAndPaths()
		local after = usedMain()
		assert(after <= before, string.format("재분배 점수 초과: %d → %d (뺀 %d)", before, after, freed))
		assert(spec.allocNodes[ch.add], "재분배: 찍은 노드가 풀렸다 " .. tostring(ch.add))
	elseif ch.type == "noop" then
		-- 바꾸지 않고 저장만 — 저장 왕복(SaveDB) 자체가 수치를 바꾸는 빌드가 있어(10-01: 워브링어 집속 수류탄 DPS +4.4%)
		--   자동 다듬기·검증기가 기준선도 같은 왕복을 거친 빌드로 잰다
	else
		error("모르는 변경 " .. tostring(ch.type))
	end
	build.buildFlag = true
	build.calcsTab:BuildOutput()
	local text = build:SaveDB("verify")
	local o = assert(io.open(outPath, "wb")); o:write(text); o:close()
	print("@@APPLIED@@" .. #text)
end)
if not ok then
	print("@@POB_ERROR@@" .. tostring(err))
	os.exit(1)
end
