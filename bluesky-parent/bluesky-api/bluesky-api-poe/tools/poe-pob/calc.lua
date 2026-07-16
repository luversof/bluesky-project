-- PoB 헤드리스 계산 러너.
-- 사용법: (cwd = <pob-src>/src 필수) luajit <이경로>/calc.lua <build-xml-파일>
-- 빌드 XML(이미 zlib 해제된 PoB XML)을 로드해 계산 스탯을 JSON 한 줄로 출력한다.
-- 출력 마커: 성공 = "@@POB_RESULT@@{...}", 실패 = "@@POB_ERROR@@메시지" (그 외 stdout 은 PoB 로그)

local xmlPath = ...
if not xmlPath then
	print("@@POB_ERROR@@usage: luajit calc.lua <build-xml>")
	os.exit(1)
end

-- PoB 순수 Lua 모듈(base64/sha1/xml/dkjson)은 runtime/lua, 바이너리 모듈(lua-utf8 등)은 runtime 에 들어있다
package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
package.cpath = package.cpath .. ";../runtime/?.dll"

local ok, err = pcall(function()
	dofile("HeadlessWrapper.lua")

	local file = assert(io.open(xmlPath, "rb"), "빌드 XML 열기 실패: " .. xmlPath)
	local xmlText = file:read("*a")
	file:close()

	loadBuildFromXML(xmlText)

	local output = build.calcsTab.mainOutput or {}
	-- 표시/랭킹에 쓰는 핵심 스탯만 추린다 (없는 키는 그대로 생략됨)
	local keys = {
		"CombinedDPS", "TotalDPS", "FullDPS", "AverageDamage", "Speed",
		"Life", "LifeUnreserved", "EnergyShield", "Mana", "Ward",
		"Armour", "Evasion", "TotalEHP",
		"FireResist", "ColdResist", "LightningResist", "ChaosResist",
		"CritChance", "CritMultiplier", "EffectiveMovementSpeedMod",
		-- 유형별 최대 피격 생존(단일 히트) — 방어 관점 가정 시뮬레이션
		"PhysicalMaximumHitTaken", "FireMaximumHitTaken", "ColdMaximumHitTaken",
		"LightningMaximumHitTaken", "ChaosMaximumHitTaken",
	}
	local result = {}
	for _, key in ipairs(keys) do
		local value = output[key]
		if type(value) == "number" and value == value and value ~= math.huge and value ~= -math.huge then
			result[key] = value
		end
	end

	local dkjson = require("dkjson")
	print("@@POB_RESULT@@" .. dkjson.encode(result))
end)

if not ok then
	print("@@POB_ERROR@@" .. tostring(err))
	os.exit(1)
end
