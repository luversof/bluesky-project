-- 가이드 추천 한 건을 PoB 안에서 **실제로 적용**하고 저장 — PoE2 자동 다듬기(Poe2RefineService)가 쓴다.
-- 원본은 QA 검증기 ~/.bluesky-qa/poe2-apply-change.lua(가이드 추천 354건 실적용 = 예측 일치, 09-30) — 두 곳을 고칠 땐 같이 볼 것.
-- 사용: (cwd = pob2-src/src) luajit apply2.lua <build.xml> <change.json> <out.xml>   → 성공 시 "@@APPLIED@@<길이>"
-- change.json: {type:"unique", slot, raw} | {type:"mod", slot, lines:[...]} | {type:"gem", from:"약한 보조 영문명", gemId}
--              | {type:"nodes", ids:[...]} | {type:"remove", slot} | {type:"noop"}(저장 왕복만)
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
