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


-- ⚠ PoB 는 빌드 로드 중 오류를 자신의 PCall 로 삼킨다. 그러면 스펙 임포트가 중단된 채
-- 기본값(사이온·빈 트리) 빌드가 계산되어 **그럴듯한 가짜 수치**가 나간다(실측: 타임리스 주얼 빌드
-- 16건이 전부 동일한 EHP 9,606). 로드 구간의 오류를 붙잡아 명시적 실패로 바꾼다.
local pobLoadError = nil
local pobCapturing = false
local rawPCall = PCall
function PCall(func, ...)
	local err = rawPCall(func, ...)
	if pobCapturing and type(err) == "string" and not pobLoadError then pobLoadError = err end
	return err
end


	local file = assert(io.open(xmlPath, "rb"), "빌드 XML 열기 실패: " .. xmlPath)
	local xmlText = file:read("*a")
	file:close()

	pobLoadError = nil
	pobCapturing = true
	loadBuildFromXML(xmlText)
	pobCapturing = false
	-- 로드 검증: 스펙 임포트가 조용히 실패하면 PoB 는 예외 없이 기본값 빌드를 계산해 **가짜 수치**를 낸다
	-- (실측: 타임리스 주얼 빌드 16건이 마라우더 → 사이온으로 떨어져 전부 동일한 EHP 9,606).
	-- XML 의 classId 와 실제 로드된 클래스를 대조해 어긋나면 명시적 실패로 바꾼다.
	local wantClass = tonumber(xmlText:match('<Spec[^>]-%sclassId="(%d+)"'))
	local gotClass = build.spec and build.spec.curClassId
	if wantClass and gotClass and wantClass ~= gotClass then
		error("스펙 임포트 실패(클래스 " .. wantClass .. " → " .. gotClass .. ")" .. (pobLoadError and (": " .. pobLoadError) or ""))
	end
	-- ⚠ 여기서 runCallback("OnFrame") 로 계산을 재촉하면 안 된다 — OnFrame 은 UI 작업(Build.lua 로드아웃 목록)을
	-- 거쳐 헤드리스에서 오류를 내고 상주 워커의 treeTab 을 손상시킨다(다음 요청부터 specList 가 비어 스펙 임포트 실패).
	if not build.calcsTab.mainOutput or next(build.calcsTab.mainOutput) == nil then
		error("계산 결과 없음(mainOutput 비어 있음)" .. (pobLoadError and (": " .. pobLoadError) or ""))
	end

	local output = build.calcsTab.mainOutput or {}
	-- 미니언 빌드: 플레이어가 직접 안 때려 player output 의 DPS 는 0 이고 실제 피해는 미니언 output 에 있다.
	-- DPS 키만 미니언 값으로 폴백(방어/생명 등은 플레이어 값 유지). 경로 전부 nil-guard → 미니언 없으면 무효(안전).
	local minionOut = nil
	do
		local env = build.calcsTab.mainEnv
		local ms = env and env.player and env.player.mainSkill
		if ms and ms.minion then minionOut = ms.minion.output end
	end
	local MINION_DPS_KEYS = { CombinedDPS = true, TotalDPS = true, AverageDamage = true, FullDPS = true, FullDotDPS = true }
	-- 표시/랭킹에 쓰는 핵심 스탯만 추린다 (없는 키는 그대로 생략됨)
	local keys = {
		"CombinedDPS", "TotalDPS", "FullDPS", "AverageDamage", "Speed",
		"Life", "LifeUnreserved", "EnergyShield", "Mana", "Ward",
		-- 속성: 장비 요구치(힘/민첩/지능) 충족 여부 판정에 쓴다
		"Str", "Dex", "Int",
		-- 총 요구 속성(장비+젬 합산, PoB 집계) — 젬 요구치까지 포함한 실현 가능성 판정에 쓴다
		"ReqStr", "ReqDex", "ReqInt",
	-- 명중/명중률 — 공격 빌드의 DPS 는 명중 가정에 크게 좌우된다(표준 무기 +2000 전제)
	"Accuracy", "AccuracyHitChance",
		"Armour", "Evasion", "TotalEHP",
		-- 생명 재생/순재생 — 정의의 화염류(자가연소) 지속력 판정용. NetLifeRegen<=0 이면 제 불에 타 죽는 빌드.
		"LifeRegen", "LifeRegenRecovery", "NetLifeRegen",
			"ManaReserved", "ManaReservedPercent", "ManaUnreserved", "ManaUnreservedPercent",
			"LifeReserved", "LifeReservedPercent",
		"FireResist", "ColdResist", "LightningResist", "ChaosResist",
		-- 실효 캡 대비 미달/초과 — 캡(최대 저항) 자체가 병목인지(치프틴 RF 화염 86 정체 수사) 판별용.
		-- Missing = max(0, 캡-현재), OverCap = max(0, 총량-캡). Missing>0 이면 총량 부족, =0 인데 목표 미달이면 캡 부족.
		"MissingFireResist", "MissingColdResist", "MissingLightningResist",
		"FireResistOverCap", "ColdResistOverCap", "LightningResistOverCap",
		-- 방어 레이어(현 패치 핵심): 주문 억제/막기/주문 막기 — PoB 스탯시트 파리티
		"SpellSuppressionChance", "BlockChance", "SpellBlockChance",
		"CritChance", "CritMultiplier", "EffectiveMovementSpeedMod",
		-- 유형별 최대 피격 생존(단일 히트) — 방어 관점 가정 시뮬레이션
		"PhysicalMaximumHitTaken", "FireMaximumHitTaken", "ColdMaximumHitTaken",
		"LightningMaximumHitTaken", "ChaosMaximumHitTaken",
	}
	local result = {}
	for _, key in ipairs(keys) do
		local value = output[key]
		-- DPS 키가 0/부재이고 미니언 output 에 값이 있으면 미니언 값 사용
		if minionOut and MINION_DPS_KEYS[key] and (not value or value == 0) and type(minionOut[key]) == "number" then
			value = minionOut[key]
		end
		if type(value) == "number" and value == value and value ~= math.huge and value ~= -math.huge then
			result[key] = value
		end
	end

	local dkjson = require("dkjson")
	-- dkjson 은 빈 테이블을 배열 []로 인코딩한다 → Java 쪽 Map 역직렬화가 터져 진짜 원인이 가려진다(worker.lua 와 동일 조치).
	print("@@POB_RESULT@@" .. (next(result) == nil and "{}" or dkjson.encode(result)))
end)

if not ok then
	print("@@POB_ERROR@@" .. tostring(err))
	os.exit(1)
end
