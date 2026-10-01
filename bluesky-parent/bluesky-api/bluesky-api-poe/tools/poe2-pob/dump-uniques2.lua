-- PoB-PoE2 고유 DB 원문 덤프 — (cwd = pob2-src/src) luajit dump-uniques2.lua <출력 json>
-- data.uniques[분류] = 고유 원문 목록. 대부분은 Data/Uniques/*.lua 파일 그대로라 parse-uniques2 가 직접 읽지만,
-- Special/Generated.lua 는 Lua 코드로 원문을 **만들어**(로어위브·묠니르·메갈로매니악 등) 노드로는 못 읽는다 → 엔진으로 만든 결과를 덤프한다.
-- 출력: [{ "src": 분류, "raw": 원문 }, …]
local outPath = ...
package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
package.cpath = package.cpath .. ";../runtime/?.dll"
local ok, err = pcall(function()
	dofile("HeadlessWrapper.lua")
	local list = {}
	local srcs = {}
	for src in pairs(data.uniques) do srcs[#srcs + 1] = src end
	table.sort(srcs)
	for _, src in ipairs(srcs) do
		for _, raw in ipairs(data.uniques[src]) do
			list[#list + 1] = { src = src, raw = raw }
		end
	end
	local dkjson = require("dkjson")
	local f = assert(io.open(outPath, "wb"))
	f:write(dkjson.encode(list))
	f:close()
	print("@@DUMP@@" .. #list)
end)
if not ok then
	print("@@POB_ERROR@@" .. tostring(err))
	os.exit(1)
end
