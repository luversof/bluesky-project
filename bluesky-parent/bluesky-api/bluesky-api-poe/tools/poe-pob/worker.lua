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
local dkjson = require("dkjson")

local KEYS = {
	"CombinedDPS", "TotalDPS", "FullDPS", "AverageDamage", "Speed",
	"Life", "LifeUnreserved", "EnergyShield", "Mana", "Ward",
	"Armour", "Evasion", "TotalEHP",
	"FireResist", "ColdResist", "LightningResist", "ChaosResist",
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
			loadBuildFromXML(xml)
			local output = build.calcsTab.mainOutput or {}
			local result = {}
			for _, key in ipairs(KEYS) do
				local value = output[key]
				if type(value) == "number" and value == value and value ~= math.huge and value ~= -math.huge then
					result[key] = value
				end
			end
			io.write("@@POB_RESULT@@" .. dkjson.encode(result) .. "\n")
		end)
		if not ok then
			io.write("@@POB_ERROR@@" .. tostring(err) .. "\n")
		end
		io.flush()
	end
end
