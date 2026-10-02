-- PoE2 업그레이드 가이드 러너 — (cwd = pob2-src/src) luajit guide2.lua <build.xml>
-- 빌드를 **한 번** 싣고 PoB 의 비교 계산기(CalcsTab:GetMiscCalculator — 아이템 툴팁 "이걸 끼우면" 과 같은 계산)로 가정을 잰다.
-- 가정마다 수~수십 ms(실빌드는 ~45ms, 보조젬 교체는 빌드 전체 재계산이라 ~53ms) — 프로세스 하나에 전부 싣되 API 가 4조각으로 나눠 돌린다(아래 인자).
-- PoE1 가이드(평가마다 프로세스)와 다른 점.
--   ① 칸 기여: 칸을 비우면 DPS·EHP 가 얼마나 떨어지나
--   ② 보조젬 기여: 주 스킬 묶음의 보조젬을 끄면(인게임에서 적용 안 되는 보조는 따로 표시)
--   ③ 특화·핵심 패시브 기여: 빼면(재분배 후보 — 기여가 0 인 것)
--   ④ 고유 아이템 교체: 무기 밖 칸마다 PoB 고유 DB 의 같은 종류를 끼워 보고 오르는 것(다른 축을 5% 넘게 깎거나 요구 능력치가 모자라면 뺀다)
-- 출력: "@@POB_GUIDE@@{json}" 한 줄, 실패 "@@POB_ERROR@@메시지"
-- 인자: <build.xml> [옵션 후보 json | -] [조각 i/n]
--   조각: 실빌드(레벨 100)에선 계산 한 번이 ~45ms 라 한 프로세스로 20초가 넘었다 → API 가 n 개를 나란히 돌려 합친다(Poe2PobEngineService).
--   ①② 는 모든 조각(가볍고 ④⑤ 가 쓴다), ⑥ = 조각 0, ③ = 조각 1%n, ④ 고유 후보 · ⑦ 다음 패시브 후보는 번호 % n 으로 나눈다
--   (⑦ 은 예전엔 조각 2 혼자 6~7초를 더 써 가이드 전체가 그 조각을 기다렸다 — 10-02 나눔, 조각마다 상위 8 → API 가 같은 기준으로 다시 상위 8).
--   맡지 않은 단계의 목록은 싣지 않는다(nil) — 합치는 쪽이 "있는 조각"에서 가져간다.
local xmlPath, candArg, shardArg = ...
if candArg == "-" then candArg = nil end
local shard, shards = 0, 1
if shardArg then
	local a, b = shardArg:match("^(%d+)/(%d+)$")
	if a then shard, shards = tonumber(a), tonumber(b) end
end
local function mine(k) return (k % shards) == shard end
package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
package.cpath = package.cpath .. ";../runtime/?.dll"

local ok, err = pcall(function()
	dofile("HeadlessWrapper.lua")
	local f = assert(io.open(xmlPath, "rb"))
	local xmlText = f:read("*a")
	f:close()
	loadBuildFromXML(xmlText)
	local out = build.calcsTab.mainOutput
	if not out or next(out) == nil then error("계산 결과 없음") end
	local calcFunc = build.calcsTab:GetMiscCalculator()
	-- 단계별 소요(초) — 큰 빌드에서 어디가 느린지(09-30 실빌드 24초)
	local timing, lastT, lastK = {}, os.clock(), "load"
	local function lap(k) local t = os.clock(); timing[lastK] = (timing[lastK] or 0) + (t - lastT); lastT, lastK = t, k end

	local function dps(o)
		if not o then return 0 end
		local v = (o.FullDPS and o.FullDPS > 0 and o.FullDPS) or o.CombinedDPS or 0
		if v == 0 and o.Minion and type(o.Minion) == "table" then v = o.Minion.CombinedDPS or 0 end
		return v
	end
	local HIT_KEYS = { "PhysicalMaximumHitTaken", "FireMaximumHitTaken", "ColdMaximumHitTaken", "LightningMaximumHitTaken", "ChaosMaximumHitTaken" }
	local function minHit(o)
		local m
		for _, k in ipairs(HIT_KEYS) do
			local v = o[k]
			if type(v) == "number" and v == v and v ~= math.huge then m = m and math.min(m, v) or v end
		end
		return m or 0
	end
	local function pct(new, base)
		if not base or base == 0 then return 0 end
		return (new - base) / math.abs(base) * 100
	end
	local base = { dps = dps(out), ehp = out.TotalEHP or 0, life = out.Life or 0, es = out.EnergyShield or 0, maxHit = minHit(out) }
	local function delta(o)
		return { dps = pct(dps(o), base.dps), ehp = pct(o.TotalEHP or 0, base.ehp), maxHit = pct(minHit(o), base.maxHit), life = (o.Life or 0) - base.life }
	end
	local function shortfall(o)
		return (o.ReqStr or 0) > (o.Str or 0) or (o.ReqDex or 0) > (o.Dex or 0) or (o.ReqInt or 0) > (o.Int or 0)
	end
	local baseShort = shortfall(out)

	local function makeItem(text)
		local ok1, it = pcall(new, "Item", text)
		if ok1 and it and it.name then return it end
		return new("Item"):Item(text)
	end
	local modErrors = 0
	-- 후보 JSON — { mods = {칸: [[줄…]…]}, rares = {칸: {base, implicits, cands = [{gen, fam, lines}]}}, onlyRares = true? }(10-02).
	--   옛 모양(칸: [[줄…]…])도 받는다. onlyRares 면 ⑧ 만 하고 끝낸다(레어 목표는 가이드 뒤에 따로 불러온다 — 본 가이드가 4~6초 느려지지 않게).
	local candDoc = nil
	if candArg then
		local cf = io.open(candArg, "rb")
		candDoc = cf and require("dkjson").decode(cf:read("*a")) or nil
		if cf then cf:close() end
	end

	lap("⑧")
	-- ⑧ 레어 목표(10-02 사용자 요청 "고유 아이템만 보지 말고 레어로도") — PoE1 가이드 "레어 목표"와 같은 방식(사용자 기준 "최상위는 못 사니 2티어"):
	--    칸마다 지금 베이스 그대로 빈 레어(베이스 암시만)를 만들고, 후보 옵션(그 베이스 풀의 계열별 2티어 중간 롤)을 하나씩 끼워 잰다.
	--    DPS·EHP 축마다 빈 레어보다 오르는 옵션을 큰 순으로 접두 3·접미 3(같은 계열 하나)까지 골라 완성품을 한 번 더 잰다 — 지금 아이템 대비 증감.
	--    고유를 낀 칸도 잰다(그 고유를 좋은 레어로 바꾸면 어떤가). 칸은 조각마다 나눈다.
	local rareTargets = {}
	local rareCands = candDoc and candDoc.rares or nil
	if rareCands then
		local rareSlots = {}
		for slotName in pairs(rareCands) do rareSlots[#rareSlots + 1] = slotName end
		table.sort(rareSlots)
		for k, slotName in ipairs(rareSlots) do
			if mine(k) then
				local spec = rareCands[slotName]
				local slot = build.itemsTab.slots[slotName]
				local cur = slot and slot.selItemId and slot.selItemId > 0 and build.itemsTab.items[slot.selItemId]
				local impl = spec.implicits or {}
				local head = "Rarity: RARE\nGuide Rare\n" .. spec.base .. "\nItem Level: 82\nImplicits: " .. #impl
				if #impl > 0 then head = head .. "\n" .. table.concat(impl, "\n") end
				local function measure(lines)
					local text = head
					if #lines > 0 then text = text .. "\n" .. table.concat(lines, "\n") end
					local okI, item2 = pcall(makeItem, text)
					if not okI or not item2 then modErrors = modErrors + 1; return nil end
					local okC, o = pcall(calcFunc, { repSlotName = slotName, repItem = item2 })
					if okC and o then return delta(o) end
					return nil
				end
				local bare = measure({})
				if bare then
					local singles = {}
					for i, c in ipairs(spec.cands or {}) do
						local d = measure(c.lines)
						if d then
							singles[#singles + 1] = { i = i - 1, gen = c.gen, fam = c.fam, lines = c.lines, dps = d.dps - bare.dps, ehp = d.ehp - bare.ehp }
						end
					end
					local function assemble(key)
						local sorted = {}
						for _, x in ipairs(singles) do if x[key] > 0.05 then sorted[#sorted + 1] = x end end
						table.sort(sorted, function(a, b) if a[key] ~= b[key] then return a[key] > b[key] end return a.i < b.i end)
						local nPre, nSuf, fams, picks, lines = 0, 0, {}, {}, {}
						for _, x in ipairs(sorted) do
							local isPre = x.gen == "prefix"
							if not fams[x.fam] and ((isPre and nPre < 3) or (not isPre and nSuf < 3)) then
								fams[x.fam] = true
								if isPre then nPre = nPre + 1 else nSuf = nSuf + 1 end
								picks[#picks + 1] = x.i
								for _, l in ipairs(x.lines) do lines[#lines + 1] = l end
							end
						end
						if #picks == 0 then return nil end
						local final = measure(lines)
						if not final then return nil end
						return { picks = picks, dps = final.dps, ehp = final.ehp }
					end
					rareTargets[#rareTargets + 1] = {
						slot = slotName,
						item = cur and cur.name or nil,
						rarity = cur and cur.rarity or nil,
						tried = #singles,
						dps = assemble("dps"),
						ehp = assemble("ehp"),
					}
				end
			end
		end
	end

	if candDoc and candDoc.onlyRares then
		lap("end")
		print("@@POB_GUIDE@@" .. require("dkjson").encode({ shard = shard, shards = shards, base = base, rareTargets = rareTargets, modErrors = modErrors, timing = timing }))
		return
	end

	lap("①")
	-- ① 칸 기여
	local EQUIP = { "Weapon 1", "Weapon 2", "Helmet", "Body Armour", "Gloves", "Boots", "Amulet", "Ring 1", "Ring 2", "Belt" }
	local slots = {}
	for _, slotName in ipairs(EQUIP) do
		local slot = build.itemsTab.slots[slotName]
		local item = slot and slot.selItemId and slot.selItemId > 0 and build.itemsTab.items[slot.selItemId]
		if item then
			-- 칸 비우기는 **실제로 빼고 전체 계산**한 뒤 되돌린다 — 비교 계산기(repItem=nil)는 일부 빌드에서 전체 계산과 달랐다(10-01 60빌드 실적용:
			--   위치헌터 무기 빼기 EHP 예측 −17.4% / 실제 −25.2%, 허리띠 −22.3% / −27.9%). 10칸뿐이라 비용이 작다(칸당 BuildOutput 두 번).
			local prevId = slot.selItemId
			slot:SetSelItemId(0)
			build.calcsTab:BuildOutput()
			local d = delta(build.calcsTab.mainOutput)
			slot:SetSelItemId(prevId)
			build.calcsTab:BuildOutput()
			slots[#slots + 1] = { slot = slotName, item = item.title or item.name, base = item.baseName, rarity = item.rarity, dps = d.dps, ehp = d.ehp, maxHit = d.maxHit }
		end
	end

	calcFunc = build.calcsTab:GetMiscCalculator() -- 칸을 실제로 뺐다 되돌렸으니 계산기를 다시 받는다
	local back0 = dps(build.calcsTab.mainOutput)
	if math.abs(pct(back0, base.dps)) > 0.01 then error(string.format("칸 비우기 측정 뒤 기준선 어긋남 %.3f → %.3f", base.dps, back0)) end

	lap("②")
	-- ② 보조젬 기여(주 스킬 묶음)
	local supports = {}
	local group = build.skillsTab.socketGroupList and build.skillsTab.socketGroupList[build.mainSocketGroup or 1]
	local env = build.calcsTab.mainEnv
	local mainSkill = env and env.player and env.player.mainSkill
	-- 주 스킬이 보조젬이 만드는 스킬이면(Impending Doom → Dark Consequences) 그룹의 mainActiveSkill 이 그 스킬 번호를 가리킨다.
	--   보조를 껐다 켜는 동안 그 스킬이 잠시 사라지면 PoB 가 번호를 앞으로 당겨 되돌려도 다른 스킬이 메인이 된다
	--   (10-01 60빌드 실적용 검증: 블러드 메이지 기준선 4.08M → 1.48M 으로 자가검사 오류). 번호를 저장했다가 매번 되돌린다.
	local savedMAS = group and { group.mainActiveSkill, group.mainActiveSkillCalcs }
	local function restoreMAS()
		if savedMAS then group.mainActiveSkill, group.mainActiveSkillCalcs = savedMAS[1], savedMAS[2] end
	end
	if group then
		for _, gem in ipairs(group.gemList or {}) do
			local ge = gem.gemData and gem.gemData.grantedEffect
			if ge and ge.support and gem.enabled ~= false then
				local applied = true
				if mainSkill and calcLib and calcLib.canGrantedEffectSupportActiveSkill then
					applied = calcLib.canGrantedEffectSupportActiveSkill(ge, mainSkill) and true or false
				end
				gem.enabled = false
				build.calcsTab:BuildOutput()
				local d = delta(build.calcsTab.mainOutput)
				gem.enabled = true
				restoreMAS()
				supports[#supports + 1] = { name = gem.nameSpec or ge.name, gameId = gem.gemData.gameId, applied = applied, dps = -d.dps, ehp = -d.ehp }
			end
		end
		build.calcsTab:BuildOutput()
	end
	-- 되돌린 뒤 기준선이 그대로인지(끄고 켠 것이 상태를 남기지 않았는지) — 어긋나면 이후 측정은 믿을 수 없다
	local after = dps(build.calcsTab.mainOutput)
	if math.abs(pct(after, base.dps)) > 0.01 then error(string.format("보조젬 측정 뒤 기준선 어긋남 %.3f → %.3f", base.dps, after)) end

	lap("⑤")
	-- ⑤ 보조젬 교체 — 기여가 가장 작은 보조 자리(인게임 미적용이 있으면 그것)에 이 스킬에 적용되는 보조를 하나씩 끼워 본다.
	--    자리마다 다 돌리면 수백×자리 수라 느려서 가장 약한 한 자리만(PoE1 가이드도 "한 번에 하나씩 바꿀 수 있는 단위"). 같은 계열(gemFamily)은 한 스킬에 둘 못 쓴다.
	local gemSwaps, weakest = {}, nil
	if group and mainSkill and calcLib and calcLib.canGrantedEffectSupportActiveSkill then
		local weakestScore
		for _, gem in ipairs(group.gemList or {}) do
			local ge = gem.gemData and gem.gemData.grantedEffect
			if ge and ge.support and gem.enabled ~= false then
				for _, s in ipairs(supports) do
					if s.gameId == gem.gemData.gameId then
						local score = s.applied and (s.dps + s.ehp) or -1e9
						if not weakestScore or score < weakestScore then weakestScore, weakest = score, gem end
					end
				end
			end
		end
	end
	local weakestName
	if weakest then
		weakestName = weakest.nameSpec
		local fam, inGroup = {}, {}
		for _, gem in ipairs(group.gemList or {}) do
			if gem.gemData then
				inGroup[gem.gemData.name] = true
				if gem ~= weakest and gem.gemData.gemFamily then fam[gem.gemData.gemFamily] = true end
			end
		end
		local old = { gemId = weakest.gemId, skillId = weakest.skillId, nameSpec = weakest.nameSpec, level = weakest.level }
		local ids = {}
		for id in pairs(build.data.gems) do ids[#ids + 1] = id end
		table.sort(ids)
		-- 후보마다 빌드 전체를 다시 계산(실빌드 ~53ms × 240)해 가이드에서 가장 무겁다 → 후보 번호 % 조각 수로 나눈다
		local gemNo = -1
		for _, id in ipairs(ids) do
			local gd = build.data.gems[id]
			local ge = gd and gd.grantedEffect
			local eligible = ge and ge.support and not ge.hidden and not inGroup[gd.name] and not (gd.gemFamily and fam[gd.gemFamily])
				and calcLib.canGrantedEffectSupportActiveSkill(ge, mainSkill)
			if eligible then gemNo = gemNo + 1 end
			if eligible and mine(gemNo) then
				weakest.gemId, weakest.skillId, weakest.nameSpec = id, nil, nil
				build.skillsTab:ProcessSocketGroup(group)
				restoreMAS()
				weakest.level = build.skillsTab:ProcessGemLevel(weakest.gemData)
				local okc = pcall(function() build.calcsTab:BuildOutput() end)
				if okc then
					local d = delta(build.calcsTab.mainOutput)
					if not shortfall(build.calcsTab.mainOutput) or baseShort then
						gemSwaps[#gemSwaps + 1] = { name = gd.name, gameId = gd.gameId, item = gd.name, family = gd.gemFamily, slot = "", dps = d.dps, ehp = d.ehp, maxHit = d.maxHit, life = d.life }
					end
				end
			end
		end
		weakest.gemId, weakest.skillId, weakest.nameSpec, weakest.level = old.gemId, old.skillId, old.nameSpec, old.level
		build.skillsTab:ProcessSocketGroup(group)
		restoreMAS()
		weakest.level = old.level
		build.calcsTab:BuildOutput()
		local back = dps(build.calcsTab.mainOutput)
		if math.abs(pct(back, base.dps)) > 0.01 then error(string.format("보조젬 교체 측정 뒤 기준선 어긋남 %.3f → %.3f", base.dps, back)) end
	end
	calcFunc = build.calcsTab:GetMiscCalculator()

	lap("③")
	-- ③ 특화·핵심 패시브 기여
	local nodes = mine(1) and {} or nil
	local allocIds = {}
	for id in pairs(build.spec.allocNodes) do allocIds[#allocIds + 1] = id end
	table.sort(allocIds)
	for _, id in ipairs(nodes and allocIds or {}) do
		local node = build.spec.allocNodes[id]
		if node.type == "Notable" or node.type == "Keystone" then
			local d = delta(calcFunc({ removeNodes = { [node] = true } }))
			nodes[#nodes + 1] = { id = id, name = node.dn, type = node.type, ascendancy = node.ascendancyName, dps = -d.dps, ehp = -d.ehp }
		end
	end

	lap("④")
	-- ④ 고유 아이템 교체
	-- 순서 있는 목록 — pairs 로 돌면 반지 1·2 중 어느 쪽에 붙는지가 실행마다 바뀌었다(결정성)
	-- 반지는 두 칸 중 **빈 칸, 없으면 기여가 더 작은 칸** 하나에만 끼워 본다 — 두 칸 다 돌리면 반지 후보가 두 번(실빌드 가이드 24초의 한 원인),
	-- 그리고 바꿀 반지는 어차피 약한 쪽이다.
	local ringSlot = "Ring 1"
	do
		local contrib = {}
		for _, s in ipairs(slots) do contrib[s.slot] = s.dps + s.ehp end
		if contrib["Ring 1"] and not contrib["Ring 2"] then ringSlot = "Ring 2"
		elseif contrib["Ring 1"] and contrib["Ring 2"] and contrib["Ring 2"] > contrib["Ring 1"] then ringSlot = "Ring 2" end
	end
	local SLOT_TYPE = { { "Helmet", "Helmet" }, { "Body Armour", "Body Armour" }, { "Gloves", "Gloves" }, { "Boots", "Boots" }, { "Amulet", "Amulet" }, { ringSlot, "Ring" }, { "Belt", "Belt" } }
	local uniqueNames = {}
	for name in pairs(main.uniqueDB.list) do uniqueNames[#uniqueNames + 1] = name end
	table.sort(uniqueNames)
	local level = build.characterLevel or 100
	-- 스킬을 부여하는 아이템(Grants Skill …)은 비교 계산기로 재면 틀린다 — PoB 는 부여 스킬을 env.mode == "MAIN" 일 때만
	-- 스킬 묶음으로 만든다(CalcSetup). 실측(09-30 spiritwalker): Forgotten Warden 예측 DPS −1.8% / 실제 장착 +3.3%.
	-- 그런 후보만 실제로 끼우고 전체 계산 → 원래 아이템으로 되돌린다(되돌린 BuildOutput 이 부여 스킬 묶음도 지운다).
	local OUT_KEYS = { "FullDPS", "CombinedDPS", "TotalEHP", "Life", "ReqStr", "ReqDex", "ReqInt", "Str", "Dex", "Int" }
	for _, k in ipairs(HIT_KEYS) do OUT_KEYS[#OUT_KEYS + 1] = k end
	local fullEvals = 0
	local function fullEval(slotName, cand)
		if not cand.raw then return nil end
		local slot = build.itemsTab.slots[slotName]
		local prevId = slot.selItemId
		local okq, it = pcall(new, "Item", cand.raw, "UNIQUE", true)
		if not (okq and it and it.name) then it = new("Item"):Item(cand.raw, "UNIQUE", true) end
		build.itemsTab:AddItem(it, true)
		slot:SetSelItemId(it.id)
		build.calcsTab:BuildOutput()
		local o, snap = build.calcsTab.mainOutput, {}
		for _, k in ipairs(OUT_KEYS) do snap[k] = o[k] end
		if type(o.Minion) == "table" then snap.Minion = { CombinedDPS = o.Minion.CombinedDPS } end
		slot:SetSelItemId(prevId)
		build.itemsTab:DeleteItem(it, true)
		build.calcsTab:BuildOutput()
		calcFunc = build.calcsTab:GetMiscCalculator()
		fullEvals = fullEvals + 1
		return snap
	end
	local swaps = {}
	local tried = 0
	local candNo = -1 -- (칸, 후보) 쌍 번호 — 조각 나누기
	for _, st in ipairs(SLOT_TYPE) do
		local slotName, wantType = st[1], st[2]
		for _, name in ipairs(uniqueNames) do
			local cand = main.uniqueDB.list[name]
			if cand.type == wantType and (not cand.requirements or (cand.requirements.level or 0) <= level) then
				candNo = candNo + 1
			end
			if cand.type == wantType and (not cand.requirements or (cand.requirements.level or 0) <= level) and mine(candNo) then
				local ok2, o
				if cand.grantedSkills and #cand.grantedSkills > 0 then
					ok2, o = pcall(fullEval, slotName, cand)
				else
					ok2, o = pcall(calcFunc, { repSlotName = slotName, repItem = cand })
				end
				tried = tried + 1
				if ok2 and o then
					local d = delta(o)
					local newShort = shortfall(o) and not baseShort
					if not newShort and (d.dps >= 1 or d.ehp >= 1) then
						swaps[#swaps + 1] = { slot = slotName, item = cand.title or name, base = cand.baseName, dps = d.dps, ehp = d.ehp, maxHit = d.maxHit, life = d.life }
					end
				end
			end
		end
	end
	-- 맞바꿈(다른 축 5% 넘게 하락)은 업그레이드가 아니다 — 축별 상위만
	local function top(list, key, other, n)
		local picked = {}
		for _, s in ipairs(list) do
			if s[key] >= 1 and s[other] >= -5 then picked[#picked + 1] = s end
		end
		table.sort(picked, function(a, b)
			if a[key] ~= b[key] then return a[key] > b[key] end
			if a.item ~= b.item then return a.item < b.item end
			return a.slot < b.slot
		end)
		local res, seen = {}, {}
		for _, s in ipairs(picked) do
			-- 같은 아이템이 반지 1·2 에 두 번 나오지 않게 · 같은 계열 보조(출혈 I/II/III)는 가장 좋은 하나만
			local k = s.family or s.item
			if not seen[k] then
				seen[k] = true
				res[#res + 1] = s
				if #res >= n then break end
			end
		end
		return res
	end

	lap("⑥")
	-- ⑥ 옵션 목표 — 두 번째 인자(칸별 후보 줄 JSON, API 가 그 베이스 옵션 풀의 계열별 최고 등급·최대 롤로 만든다)가 있으면
	--    지금 그 칸 아이템에 후보 줄을 **더한** 아이템을 끼워 잰다(PoB 아이템 원문 끝에 줄을 보태면 명시 옵션으로 읽힌다).
	--    인게임에선 접두·접미 칸이 차 있으면 못 더하지만 "어떤 옵션이 이 빌드에 값진가"(제작·구매 때 노릴 것)의 기준이 된다.
	--    이미 같은 틀(숫자만 다른) 줄이 있는 옵션은 건너뛴다.
	-- 아이템 생성 방식이 PoB 판마다 다르다: 릴리스(v0.23) = new("Item", raw), dev = new("Item"):Item(raw)(여분 인자는 오류).
	--   dev 방식만 쓰다가 엔진을 릴리스로 바꾼 뒤 옵션 목표가 전부 조용히 실패했다(pcall 이 삼킴, 09-30). 둘 다 받고, 실패 수는 modErrors 로 싣는다.
	local modTargets = {}
	-- (makeItem · modErrors · candDoc 는 ⑧ 이 먼저 쓰려고 ① 앞으로 옮겼다 — 10-02)
	if candDoc and mine(0) then
		local cands = candDoc.mods or (candDoc.rares == nil and candDoc) or {}
		local function tmpl(s) return (s:gsub("[%d%.]+", "#")) end
		local slotNames = {}
		for slotName in pairs(cands) do slotNames[#slotNames + 1] = slotName end
		table.sort(slotNames)
		for _, slotName in ipairs(slotNames) do
			local slot = build.itemsTab.slots[slotName]
			local cur = slot and slot.selItemId and slot.selItemId > 0 and build.itemsTab.items[slot.selItemId]
			if cur and cur.rarity ~= "UNIQUE" then
				local have = {}
				for _, l in ipairs(cur.explicitModLines or {}) do have[tmpl(l.line)] = true end
				for _, l in ipairs(cur.implicitModLines or {}) do have[tmpl(l.line)] = true end
				local raw = cur:BuildRaw()
				local results = {}
				for i, lines in ipairs(cands[slotName]) do
					local dup = false
					for _, l in ipairs(lines) do if have[tmpl(l)] then dup = true end end
					if not dup then
						local okI, item2 = pcall(makeItem, raw .. "\n" .. table.concat(lines, "\n"))
						if not okI then modErrors = modErrors + 1 end
						if okI and item2 then
							local okC, o = pcall(calcFunc, { repSlotName = slotName, repItem = item2 })
							if okC and o then
								local d = delta(o)
								results[#results + 1] = { i = i - 1, dps = d.dps, ehp = d.ehp }
							end
						end
					end
				end
				local function best(key, other)
					local picked = {}
					for _, r in ipairs(results) do if r[key] >= 0.5 and r[other] >= -5 then picked[#picked + 1] = r end end
					table.sort(picked, function(a, b) if a[key] ~= b[key] then return a[key] > b[key] end return a.i < b.i end)
					local res = {}
					for k = 1, math.min(3, #picked) do res[k] = picked[k] end
					return res
				end
				modTargets[#modTargets + 1] = { slot = slotName, item = cur.name, tried = #results, dps = best("dps", "ehp"), ehp = best("ehp", "dps") }
			end
		end
	end

	lap("⑦")
	-- ⑦ 다음에 찍을 패시브 — 아직 안 찍은 특화·핵심 중 8점 안에 닿는 것마다 **가는 길 전체**를 더해(PoB 가 이미 계산해 둔 최단 경로 node.path)
	--    점수당 증감으로 줄 세운다. 전직 노드는 뺀다(전직 점수는 따로라 같은 저울에 못 올린다).
	-- 경로의 능력치 노드(PoE2 "힘·민첩·지능 중 선택")는 실제로 찍으면 PoB 가 "마지막으로 고른 능력치, 없으면 힘"으로 바꾼다
	-- (PassiveSpec:AllocNode → SwitchAttributeNode). 선택 전 노드를 더하면 그 +능력치가 빠져 예측이 실제보다 낮았다
	-- (09-30 실적용 검증: 블러드 메이지 경로 EHP 예측 +17.1 / 실제 +18.1). 같은 규칙으로 바꾼 복사본을 더한다.
	local function asAllocated(n)
		if not n.isAttribute or not copyTableSafe then return n end
		local copy = copyTableSafe(build.spec.tree.nodes[n.id], false, true)
		if not copy or not copy.isAttribute or not copy.options then return n end
		local option = copy.options[build.spec.attributeIndex or 1]
		if not option then return n end
		build.spec:ReplaceNode(copy, option)
		build.spec.tree:ProcessStats(copy)
		return copy
	end
	local nextNodes = {}
	local nextWeaponSet = 0
	local nodeIds = {}
	local doNext = mine(2) -- 무기 세트로 건너뛴 수는 한 조각만 센다(후보 자체는 조각마다 번호로 나눈다)
	local nextK = 0
	for id in pairs(build.spec.nodes) do nodeIds[#nodeIds + 1] = id end
	table.sort(nodeIds)
	for _, id in ipairs(nodeIds) do
		local node = build.spec.nodes[id]
		-- 경로가 **무기 세트 전용으로 찍힌 노드**에서 출발하면(pathRoot.allocMode ≠ 0) 본 트리 점수로는 못 찍는다 — PoB 도 본 트리로 찍으면
		-- 끊긴 노드로 보고 풀어 버린다(09-30 실적용 검증: 블러드 메이지 탈출 전략). 그 무기 세트 점수로만 가능한 제안이라 뺀다.
		local weaponSetPath = node.pathRoot and (node.pathRoot.allocMode or 0) ~= 0
		if weaponSetPath and not node.alloc and doNext then nextWeaponSet = nextWeaponSet + 1 end
		local candidate = not node.alloc and not weaponSetPath and (node.type == "Notable" or node.type == "Keystone") and not node.ascendancyName
			and node.path and node.pathDist and node.pathDist > 0 and node.pathDist <= 8
		if candidate then nextK = nextK + 1 end
		if candidate and mine(nextK) then
			local add, pathIds = {}, {}
			for _, n in ipairs(node.path) do if not n.alloc then add[asAllocated(n)] = true; pathIds[#pathIds + 1] = n.id end end
			local okC, o = pcall(calcFunc, { addNodes = add })
			if okC and o then
				local d = delta(o)
				nextNodes[#nextNodes + 1] = { id = id, name = node.dn, type = node.type, points = node.pathDist,
					dps = d.dps, ehp = d.ehp, dpsPer = d.dps / node.pathDist, ehpPer = d.ehp / node.pathDist, item = node.dn, slot = "", path = pathIds }
			end
		end
	end

	-- 패시브는 "점수당"으로 줄 세우되 거르기는 **합계**로(점수당 1% 기준이면 생존 쪽이 다 빠졌다: 5점에 +3% = 0.6%/점)
	local function topPer(list, key, other, n)
		local picked = {}
		for _, s in ipairs(list) do
			if s[key] >= 1 and s[other] >= -5 then picked[#picked + 1] = s end
		end
		table.sort(picked, function(a, b)
			local pa, pb = a[key] / a.points, b[key] / b.points
			if pa ~= pb then return pa > pb end
			return a.id < b.id
		end)
		local res = {}
		for k = 1, math.min(n, #picked) do res[k] = picked[k] end
		return res
	end

	lap("end")
	local warn = { unapplied = 0, mainCost = false }
	for _, s in ipairs(supports) do if not s.applied then warn.unapplied = warn.unapplied + 1 end end
	for _, key in ipairs({ "LifeCostWarning", "ManaCostWarning", "RageCostWarning", "ESCostWarning" }) do
		if out[key] == true then warn.mainCost = true end
	end

	local dkjson = require("dkjson")
	local zero = mine(0)
	-- 가이드의 DPS 가 어느 스킬 기준인지 — 주 스킬이 보조용(예: 의식·오라)이면 DPS 추천은 그 스킬 기준이라 화면에 밝힌다(09-30 실빌드 리치: 78 DPS)
	local ae = mainSkill and mainSkill.activeEffect
	base.skill = ae and ae.grantedEffect and ae.grantedEffect.name or nil
	print("@@POB_GUIDE@@" .. dkjson.encode({
		shard = shard,
		shards = shards,
		base = base,
		warn = warn,
		slots = slots,
		supports = supports,
		nodes = nodes,
		swapsDps = top(swaps, "dps", "ehp", 8),
		swapsEhp = top(swaps, "ehp", "dps", 8),
		tried = tried,
		fullEvals = fullEvals,
		gemWeakest = weakestName,
		gemSwaps = top(gemSwaps, "dps", "ehp", 8),
		gemTried = #gemSwaps,
		modTargets = zero and modTargets or nil,
		rareTargets = rareTargets,
		modErrors = zero and modErrors or nil,
		timing = timing,
		nextDps = topPer(nextNodes, "dps", "ehp", 8),
		nextEhp = topPer(nextNodes, "ehp", "dps", 8),
		nextTried = #nextNodes,
		nextWeaponSetSkipped = doNext and nextWeaponSet or nil,
	}))
end)
if not ok then
	print("@@POB_ERROR@@" .. tostring(err))
	os.exit(1)
end
