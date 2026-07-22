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

-- PoB 의 HeadlessWrapper 는 Inflate/NewFileSearch 가 **빈 스텁**이라(원본 주석: "TODO: And this")
-- 무궁한(타임리스) 주얼 데이터(Data/TimelessJewelData/*.zip)를 아예 못 읽는다 → 반경 변환이 계산되지 않는다.
-- 우리는 파이프라인에서 .zip 을 미리 풀어 **.bin** 을 만들어 두고, 여기서 NewFileSearch 만 최소 구현해
-- PoB 가 "압축 해제본이 최신" 경로를 타도록 한다(Inflate 없이 동작).
function GetScriptPath()
	return "."
end
local function fileExists(path)
	local f = io.open(path, "rb")
	if f then f:close() return true end
	return false
end
local rawNewFileSearch = NewFileSearch
function NewFileSearch(pattern, ...)
	if type(pattern) == "string" and pattern:find("TimelessJewelData") and not pattern:find("%*") then
		if not fileExists(pattern) then return nil end
		-- .bin 을 항상 "더 최신"으로 보고해 압축 해제본 경로를 태운다
		local modified = pattern:sub(-4) == ".bin" and 2 or 1
		local name = pattern:match("[^/]+$") or pattern
		return {
			GetFileName = function() return name end,
			GetFileModifiedTime = function() return modified end,
			GetFileSize = function() return 0 end,
			NextFile = function() return false end,
		}
	end
	if rawNewFileSearch then return rawNewFileSearch(pattern, ...) end
	return nil
end


	local file = assert(io.open(xmlPath, "rb"), "빌드 XML 열기 실패: " .. xmlPath)
	local xmlText = file:read("*a")
	file:close()

	loadBuildFromXML(xmlText)

	local output = build.calcsTab.mainOutput or {}
	-- 표시/랭킹에 쓰는 핵심 스탯만 추린다 (없는 키는 그대로 생략됨)
	local keys = {
		"CombinedDPS", "TotalDPS", "FullDPS", "AverageDamage", "Speed",
		"Life", "LifeUnreserved", "EnergyShield", "Mana", "Ward",
		-- 속성: 장비 요구치(힘/민첩/지능) 충족 여부 판정에 쓴다
		"Str", "Dex", "Int",
	-- 명중/명중률 — 공격 빌드의 DPS 는 명중 가정에 크게 좌우된다(표준 무기 +2000 전제)
	"Accuracy", "AccuracyHitChance",
		"Armour", "Evasion", "TotalEHP",
			"ManaReserved", "ManaReservedPercent", "ManaUnreserved", "ManaUnreservedPercent",
			"LifeReserved", "LifeReservedPercent",
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
