# poe-pob — Path of Building 헤드리스 계산 엔진

PoB 커뮤니티(MIT)의 계산 엔진을 LuaJIT 로 헤드리스 구동해 빌드 스탯을 실제 재계산한다.
웹의 `/poe/build` "PoB 엔진으로 재계산" 버튼과 (예정) 시뮬레이터 배치 평가가 이걸 사용한다.

## 구성

- `calc.lua` — 러너. 빌드 XML(zlib 해제된 PoB XML)을 받아 `@@POB_RESULT@@{json}` 한 줄 출력.
  cwd 가 PoB 소스의 `src/` 여야 한다 (HeadlessWrapper/Launch 가 상대 경로를 쓴다).
- PoB 소스는 파생 산출물이라 **git 밖** `~/.poe-gamedata/work/pob-src` 에 클론해 둔다.

## 셋업 (1회)

```
winget install --id DEVCOM.LuaJIT        # LuaJIT (%LOCALAPPDATA%\Programs\LuaJIT)
cd ~/.poe-gamedata/work
git clone --depth 1 https://github.com/PathOfBuildingCommunity/PathOfBuilding.git pob-src
```

순수 Lua 모듈(base64/sha1/xml/dkjson)과 바이너리 모듈(lua-utf8.dll)은 PoB 저장소의
`runtime/` 에 이미 들어 있어 별도 설치가 필요 없다 (`calc.lua` 가 package.path/cpath 에 추가).
lua-utf8.dll 은 lua51 ABI 라 LuaJIT 에서 그대로 로드된다.

## 수동 실행

```
cd ~/.poe-gamedata/work/pob-src/src
luajit <repo>/tools/poe-pob/calc.lua <build.xml>   # 회당 약 1.5초
```

## 서버 프로퍼티

- `poe.pob.src-dir` (기본 `${user.home}/.poe-gamedata/work/pob-src`)
- `poe.pob.runner` (기본 `tools/poe-pob/calc.lua`, 서버 작업 디렉토리 기준)
- `poe.pob.luajit-path` (기본: winget 설치 경로 → 없으면 PATH 의 `luajit`)

소스/러너가 없으면 `PoePobEngineService.isAvailable()` 이 false 라 버튼이 숨는다 (k8s 등).

## 시즌 갱신

PoB 소스만 최신으로 당기면 된다: `cd ~/.poe-gamedata/work/pob-src && git pull`
(트리 버전은 PoB 의 `src/TreeData/<버전>` 지원 범위를 따른다.)
