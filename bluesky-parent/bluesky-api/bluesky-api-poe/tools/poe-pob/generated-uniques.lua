-- PoB 가 실행 중에 만들어 내는 고유 아이템(Data/Uniques/Special/Generated.lua — Impossible Escape · Watcher's Eye · Megalomaniac ·
-- Forbidden Flame/Flesh · Forbidden Shako · Skin of the Lords · Precursor's Emblem …)을 원문 그대로 꺼낸다(10-03 C76).
-- 이 고유들은 Uniques/*.lua 에 정적 [[ ]] 블록이 없고 트리 · 젬 데이터로 변형을 만들어 붙이므로, 정적 파일만 읽던 parse-uniques 에서 통째로 빠졌다.
-- 사용법: (cwd = <pob-src>/src 필수) luajit <이경로>/generated-uniques.lua
-- 출력: 성공 = "@@POB_RESULT@@[\"원문\", …]", 실패 = "@@POB_ERROR@@메시지"

package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
package.cpath = package.cpath .. ";../runtime/?.dll"

local ok, err = pcall(function()
	dofile("HeadlessWrapper.lua")
	local list = {}
	for _, raw in ipairs((data and data.uniques and data.uniques.generated) or {}) do
		if type(raw) == "string" and raw ~= "" then
			table.insert(list, raw)
		end
	end
	local dkjson = require("dkjson")
	io.write("@@POB_RESULT@@" .. dkjson.encode(list))
end)
if not ok then
	io.write("@@POB_ERROR@@" .. tostring(err))
	os.exit(1)
end
