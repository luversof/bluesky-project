-- PoB 헤드리스 상주 워커.
-- HeadlessWrapper 를 한 번만 로드하고, stdin 으로 빌드 XML 을 계속 받아 계산 결과를 stdout 으로 돌려준다.
-- 이렇게 하면 eval 마다 luajit 프로세스 + PoB 데이터 로드를 반복하지 않아 크게 빨라진다.
--
-- 프로토콜(한 요청):
--   stdin  : XML 여러 줄 … 그리고 마지막에 "@@END@@" 한 줄
--   stdout : 성공 "@@POB_RESULT@@{json}" / 실패 "@@POB_ERROR@@메시지" (그 외 stdout 은 PoB 로그)
-- stdin EOF 면 종료.
-- 사용법: (cwd = <pob-src>/src) luajit <이경로>/worker.lua

package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
package.cpath = package.cpath .. ";../runtime/?.dll"

dofile("HeadlessWrapper.lua")

-- PoB 의 HeadlessWrapper 는 Inflate/NewFileSearch 가 **빈 스텁**이라(원본 주석: "TODO: And this")
-- 무궁한(타임리스) 주얼 데이터(Data/TimelessJewelData/*.zip)를 아예 못 읽는다 → 반경 변환이 계산되지 않는다.
-- 우리는 파이프라인에서 .zip 을 미리 풀어 **.bin** 을 만들어 두고, 여기서 NewFileSearch 만 최소 구현해
-- PoB 가 "압축 해제본이 최신" 경로를 타도록 한다(Inflate 없이 동작).
-- GetScriptPath() 스텁이 "" 라서 PoB 가 "/Data/TimelessJewelData/…" 를 **드라이브 루트** 기준으로 연다 → 항상 실패.
-- cwd(= pob-src/src) 를 돌려주면 상대 경로가 제대로 풀린다.
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
-- 이 워커의 Lua 상태를 더 믿을 수 없는 상황 — 상주 워커는 빌드를 여러 번 실어 나면 PoB 내부 상태가
-- 무너져 스펙 임포트가 조용히 실패한다(실측: 동일 XML 이 새 워커에선 성공, 상주 워커에선 클래스 3→0).
-- 빌드 문제가 아니라 워커 문제니 스스로 죽어 Java 가 새 워커로 재시도하게 한다.
local function fatal(msg)
	io.write("@@POB_FATAL@@" .. msg .. (pobLoadError and (": " .. pobLoadError) or "") .. "\n")
	io.flush()
	os.exit(1)
end

function PCall(func, ...)
	local err = rawPCall(func, ...)
	if pobCapturing and type(err) == "string" and not pobLoadError then pobLoadError = err end
	return err
end

local dkjson = require("dkjson")

local KEYS = {
	"CombinedDPS", "TotalDPS", "FullDPS", "AverageDamage", "Speed",
	"Life", "LifeUnreserved", "EnergyShield", "Mana", "Ward",
	-- 속성: 장비 요구치(힘/민첩/지능) 충족 판정 — calc.lua 와 목록을 맞춰야 한다(둘 다 고쳐야 반영됨)
	"Str", "Dex", "Int",
	-- 총 요구 속성(장비+젬 합산) — 젬 요구치 포함 실현 가능성 판정용
	"ReqStr", "ReqDex", "ReqInt",
	-- 명중/명중률 — 공격 빌드의 DPS 는 명중 가정에 크게 좌우된다(표준 무기 +2000 전제)
	"Accuracy", "AccuracyHitChance",
	"Armour", "Evasion", "TotalEHP",
	-- 생명 재생/순재생 — 정의의 화염류(자가연소) 지속력 판정용(NetLifeRegen<=0 이면 제 불에 타 죽음). calc.lua 와 목록 일치 필수.
	"LifeRegen", "LifeRegenRecovery", "NetLifeRegen",
	"ManaReserved", "ManaReservedPercent", "ManaUnreserved", "ManaUnreservedPercent",
	"LifeReserved", "LifeReservedPercent",
	"FireResist", "ColdResist", "LightningResist", "ChaosResist",
	-- 실효 캡 대비 미달/초과 — 캡(최대 저항)이 병목인지 판별용. calc.lua 와 목록 일치 필수.
	"MissingFireResist", "MissingColdResist", "MissingLightningResist",
	"FireResistOverCap", "ColdResistOverCap", "LightningResistOverCap",
	-- 방어 레이어(현 패치 핵심): 주문 억제/막기/주문 막기 — calc.lua 와 목록 일치 필수.
	"SpellSuppressionChance", "BlockChance", "SpellBlockChance",
	"CritChance", "CritMultiplier", "EffectiveMovementSpeedMod",
	"PhysicalMaximumHitTaken", "FireMaximumHitTaken", "ColdMaximumHitTaken",
	"LightningMaximumHitTaken", "ChaosMaximumHitTaken",
}

-- 준비 완료 신호 (Java 가 워커 기동 확인)
io.write("@@POB_READY@@\n")
io.flush()

while true do
	-- 한 요청의 XML 을 "@@END@@" 마커까지 읽어 모은다
	local lines = {}
	local gotEnd = false
	while true do
		local line = io.read("*l")
		if line == nil then
			-- stdin EOF → 종료
			os.exit(0)
		end
		if line == "@@END@@" then
			gotEnd = true
			break
		end
		lines[#lines + 1] = line
	end
	if gotEnd then
		local xml = table.concat(lines, "\n")
		local ok, err = pcall(function()
			pobLoadError = nil
			pobCapturing = true
			loadBuildFromXML(xml)
			pobCapturing = false
			-- 로드 검증: 스펙 임포트가 조용히 실패하면 PoB 는 예외 없이 기본값 빌드를 계산해 **가짜 수치**를 낸다
			-- (실측: 타임리스 주얼 빌드 16건이 마라우더 → 사이온으로 떨어져 전부 동일한 EHP 9,606).
			-- XML 의 classId 와 실제 로드된 클래스를 대조해 어긋나면 명시적 실패로 바꾼다.
			local wantClass = tonumber(xml:match('<Spec[^>]-%sclassId="(%d+)"'))
			local gotClass = build.spec and build.spec.curClassId
			if wantClass and gotClass and wantClass ~= gotClass then
				fatal("스펙 임포트 실패(클래스 " .. wantClass .. " → " .. gotClass .. ")")
			end
			-- ⚠ 여기서 runCallback("OnFrame") 로 계산을 재촉하면 안 된다 — OnFrame 은 UI 작업(Build.lua 로드아웃 목록)을
			-- 거쳐 헤드리스에서 오류를 내고 상주 워커의 treeTab 을 손상시킨다(다음 요청부터 specList 가 비어 스펙 임포트 실패).
			if not build.calcsTab.mainOutput or next(build.calcsTab.mainOutput) == nil then
				fatal("계산 결과 없음(mainOutput 비어 있음)")
			end
			local output = build.calcsTab.mainOutput or {}
			-- 미니언 빌드 DPS 폴백(calc.lua 와 동일·nil-guard): player DPS 0 이면 미니언 output 값 사용.
			local minionOut = nil
			do local env = build.calcsTab.mainEnv; local ms = env and env.player and env.player.mainSkill; if ms and ms.minion then minionOut = ms.minion.output end end
			local MINION_DPS_KEYS = { CombinedDPS = true, TotalDPS = true, AverageDamage = true, FullDPS = true, FullDotDPS = true }
			local result = {}
			for _, key in ipairs(KEYS) do
				local value = output[key]
				if minionOut and MINION_DPS_KEYS[key] and (not value or value == 0) and type(minionOut[key]) == "number" then value = minionOut[key] end
				if type(value) == "number" and value == value and value ~= math.huge and value ~= -math.huge then
					result[key] = value
				end
			end
			-- ⚠ dkjson 은 빈 Lua 테이블을 **배열 []** 로 인코딩한다 → Java 쪽 Map 역직렬화가
			-- "Cannot deserialize LinkedHashMap from Array" 로 터져 진짜 원인이 가려진다(타임리스 주얼에서 발각).
			-- 비면 명시적으로 오브젝트 {} 를 내보내 "빈 결과"가 그대로 전달되게 한다.
			local encoded = next(result) == nil and "{}" or dkjson.encode(result)
			io.write("@@POB_RESULT@@" .. encoded .. "\n")
		end)
		if not ok then
			io.write("@@POB_ERROR@@" .. tostring(err) .. "\n")
		end
		io.flush()
	end
end
