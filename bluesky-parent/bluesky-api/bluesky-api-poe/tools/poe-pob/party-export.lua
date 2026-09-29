-- 파티원(루미너리 용병) 빌드의 버프를 PoB 파티 탭 형식 텍스트로 내보낸다.
-- 사용법: (cwd = <pob-src>/src 필수) luajit <이경로>/party-export.lua <파티원 빌드 XML 파일>
-- 출력: 성공 = "@@POB_RESULT@@{json}", 실패 = "@@POB_ERROR@@메시지"
--   json.sections: { Aura, Curse, "Warcry Skills", "Link Skills", EnemyConditions, EnemyMods } — 받는 쪽 빌드의
--     <Party><ImportedBuffs name="...">텍스트</ImportedBuffs></Party> 에 그대로 넣는다(PartyTab:Load 가 읽는 형식).
--   json.auras / json.curses / json.links: [{name, effect}] — 화면 요약용(effect = 효과 배율 %, 100 = 기본).
--
-- 왜 PoB 가 만든 텍스트를 쓰나: 파티 버프 줄은 modLib.formatSourceMod 로 직렬화된 PoB 내부 모드라 직접 만들면 틀린다.
-- 파티원 빌드를 PoB 로 계산하면 젬 레벨·품질·장비의 오라 효과 옵션까지 반영된 효과 배율이 나온다(용병 장비가 중요한 까닭).
-- ⚠ enableExportBuffs 는 XML 에 저장되지 않는 플래그라 로드 뒤 켜고 다시 계산해야 내보내기가 채워진다(Calcs → PartyTab:setBuffExports).

local xmlPath = ...
if not xmlPath then
	print("@@POB_ERROR@@usage: luajit party-export.lua <build-xml>")
	os.exit(1)
end

package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
package.cpath = package.cpath .. ";../runtime/?.dll"

local ok, err = pcall(function()
	dofile("HeadlessWrapper.lua")

	-- calc.lua 와 같은 최소 스텁 — 타임리스 주얼 .bin 을 압축 해제본 경로로 읽게 한다
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

	-- 로드 중 오류를 PoB 가 삼키면 기본값 빌드가 계산돼 그럴듯한 가짜 버프가 나간다 — 붙잡아 실패로 바꾼다(calc.lua 와 같은 이유)
	local pobLoadError = nil
	local pobCapturing = false
	local rawPCall = PCall
	function PCall(func, ...)
		local e = rawPCall(func, ...)
		if pobCapturing and type(e) == "string" and not pobLoadError then pobLoadError = e end
		return e
	end

	local file = assert(io.open(xmlPath, "rb"), "빌드 XML 열기 실패: " .. xmlPath)
	local xmlText = file:read("*a")
	file:close()

	pobLoadError = nil
	pobCapturing = true
	loadBuildFromXML(xmlText)
	pobCapturing = false
	if pobLoadError then
		error("파티원 빌드 로드 실패: " .. pobLoadError)
	end

	build.partyTab.enableExportBuffs = true
	build.calcsTab:BuildOutput()
	local exports = build.partyTab.buffExports or {}

	-- 요약(이름·효과 배율) — exportBuffs 가 표를 문자열로 바꾸며 지우므로 먼저 읽는다
	local function summary(kind)
		local out = {}
		for name, buff in pairs(exports[kind] or {}) do
			if type(buff) == "table" and buff.effectMult and name ~= "extraAura" and name ~= "otherEffects" then
				out[#out + 1] = { name = name, effect = buff.effectMult * 100 }
			end
		end
		table.sort(out, function(a, b) return a.name < b.name end)
		return out
	end
	local result = {
		auras = summary("Aura"),
		curses = summary("Curse"),
		links = summary("Link"),
		sections = {},
	}
	-- 내보내기 종류 → 받는 쪽 ImportedBuffs 이름(PartyTab:Load)
	local SECTIONS = {
		{ "Aura", "Aura" },
		{ "Curse", "Curse" },
		{ "Warcry", "Warcry Skills" },
		{ "Link", "Link Skills" },
		{ "EnemyConditions", "EnemyConditions" },
		{ "EnemyMods", "EnemyMods" },
	}
	for _, pair in ipairs(SECTIONS) do
		result.sections[pair[2]] = build.partyTab:exportBuffs(pair[1]) or ""
	end

	local dkjson = require("dkjson")
	print("@@POB_RESULT@@" .. dkjson.encode(result))
end)

if not ok then
	print("@@POB_ERROR@@" .. tostring(err))
	os.exit(1)
end
