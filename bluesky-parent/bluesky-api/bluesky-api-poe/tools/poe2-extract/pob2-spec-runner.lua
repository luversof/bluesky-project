-- PoB-PoE2 자체 시험(spec/System/*_spec.lua)을 우리 엔진(표준 LuaJIT + patch-pob2 문법 되돌리기)으로 돌리는 작은 busted 대용.
-- 목적: PoB 가 기대값을 적어 둔 계산 시험이 **우리 쪽에서도 같은 값**을 내는지 — 문법 되돌리기(+=, ?., continue, |x| ->)가
--       의미를 바꾸지 않았다는 근거(실제 PoB 코드가 없어 빌드 대조를 못 하는 대신).
-- 사용: (cwd = pob2-src/src) luajit pob2-spec-runner.lua <spec 파일>...   → 마지막 줄 "@@SPEC@@{pass,fail,error,skip}" + 실패 목록
package.path = package.path .. ";../runtime/lua/?.lua;../runtime/lua/?/init.lua"
package.cpath = package.cpath .. ";../runtime/?.dll"
dofile("HeadlessWrapper.lua")

-- ── luassert 흉내 ── assert.are.equals / assert.is_not_nil / assert.are_not.same … 이름 사슬을 낱말로 쪼개 부정 횟수 + 마지막 동사로 판정
local function deepEq(a, b, seen)
	if a == b then return true end
	if type(a) ~= "table" or type(b) ~= "table" then return false end
	seen = seen or {}
	if seen[a] == b then return true end
	seen[a] = b
	for k, v in pairs(a) do if not deepEq(v, b[k], seen) then return false end end
	for k in pairs(b) do if a[k] == nil then return false end end
	return true
end
local VERBS = {
	equals = function(e, a) return e == a end, equal = function(e, a) return e == a end,
	same = function(e, a) return deepEq(e, a) end,
	near = function(e, a, tol) return type(e) == "number" and type(a) == "number" and math.abs(e - a) <= (tol or 0) end,
	["true"] = function(v) return v == true end, ["false"] = function(v) return v == false end,
	["nil"] = function(v) return v == nil end,
	truthy = function(v) return not not v end, falsy = function(v) return not v end,
	matches = function(p, s, init, plain) return type(s) == "string" and s:find(p, init, plain) ~= nil end,
	errors = function(f) return not pcall(f) end, error = function(f) return not pcall(f) end,
	table = function(v) return type(v) == "table" end, string = function(v) return type(v) == "string" end,
	["function"] = function(v) return type(v) == "function" end,
	fail = function() return false end,
}
local function chain(words)
	return setmetatable({}, {
		__index = function(_, key)
			local w = {}
			for _, x in ipairs(words) do w[#w + 1] = x end
			for part in key:gmatch("[^_]+") do w[#w + 1] = part:lower() end
			return chain(w)
		end,
		__call = function(_, ...)
			local neg, verb = false, nil
			for _, x in ipairs(words) do
				if x == "not" or x == "no" then neg = not neg
				elseif VERBS[x] then verb = x end
			end
			if not verb then error("assert: 알 수 없는 사슬 " .. table.concat(words, "."), 2) end
			local ok = VERBS[verb](...)
			if neg then ok = not ok end
			if not ok then
				local args = { ... }
				error(string.format("assert.%s 실패: %s / %s", table.concat(words, "."), tostring(args[1]), tostring(args[2])), 2)
			end
			return ...
		end,
	})
end
local rawAssert = assert
assert = setmetatable({}, {
	__index = function(_, key) return chain({})[key] end,
	__call = function(_, v, msg, ...) return rawAssert(v, msg, ...) end,
})
function stub() error("stub 미지원") end

-- ── describe / it ──
local ctx = { before = {}, after = {} }
local stackCtx = { ctx }
local counts = { pass = 0, fail = 0, error = 0, skip = 0 }
local failures = {}
local path = {}
function describe(name, fn)
	local c = { before = {}, after = {}, teardown = {} }
	stackCtx[#stackCtx + 1] = c
	path[#path + 1] = name
	local ok, err = pcall(fn)
	if not ok then counts.error = counts.error + 1; failures[#failures + 1] = table.concat(path, " > ") .. " [describe] " .. tostring(err) end
	for _, t in ipairs(c.teardown) do pcall(t) end
	path[#path] = nil
	stackCtx[#stackCtx] = nil
end
function before_each(fn) local c = stackCtx[#stackCtx]; c.before[#c.before + 1] = fn end
function after_each(fn) local c = stackCtx[#stackCtx]; c.after[#c.after + 1] = fn end
function setup(fn) fn() end
lazy_setup = setup
function teardown(fn) local c = stackCtx[#stackCtx]; c.teardown[#c.teardown + 1] = fn end
function pending() counts.skip = counts.skip + 1 end
function it(name, fn)
	-- busted 는 함수 없는 it 를 보류(pending)로 센다(TestSocketables 는 등록 때 바로 단언하고 nil 을 넘긴다)
	if fn == nil then counts.skip = counts.skip + 1 return end
	local ok, err = pcall(function()
		for i = 1, #stackCtx do for _, b in ipairs(stackCtx[i].before) do b() end end
		fn()
	end)
	for i = #stackCtx, 1, -1 do for _, a in ipairs(stackCtx[i].after) do pcall(a) end end
	if ok then counts.pass = counts.pass + 1
	else
		local msg = tostring(err)
		if msg:find("stub 미지원") then counts.skip = counts.skip + 1
		else
			counts.fail = counts.fail + 1
			failures[#failures + 1] = table.concat(path, " > ") .. " > " .. name .. " :: " .. msg:sub(1, 300)
		end
	end
end

for _, file in ipairs({ ... }) do
	local f, err = loadfile(file)
	if not f then
		counts.error = counts.error + 1
		failures[#failures + 1] = file .. " [load] " .. tostring(err)
	else
		local ok, e = pcall(f)
		if not ok then counts.error = counts.error + 1; failures[#failures + 1] = file .. " [run] " .. tostring(e) end
	end
end
for _, l in ipairs(failures) do print("@@FAIL@@" .. l) end
print(string.format('@@SPEC@@{"pass":%d,"fail":%d,"error":%d,"skip":%d}', counts.pass, counts.fail, counts.error, counts.skip))
