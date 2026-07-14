# poe-extract

PoE 게임 데이터 추출 파이프라인. 모든 산출물(중간물 포함)은 `~/.poe-gamedata/` 에 생성되며 git 으로 관리하지 않는다.

## 최초 셋업 (빈 ~/.poe-gamedata 에서 시작)

`node run-all.mjs` 하나로 처음부터 끝까지 생성된다 (웹 관리 메뉴 "데이터 갱신 실행"과 동일):

1. bootstrap — `node_modules` 없으면 npm install 자동 실행
2. extract → transform → parse-uniques → parse-items → parse-tree → icons
3. PoB 엔진 소스(`~/.poe-gamedata/work/pob-src`) 없으면 git clone (빌드 재계산/시뮬레이터용)

필요 도구: Node, git. **ImageMagick** 은 아이콘(DDS→PNG) 변환에만 필요하며 없으면 아이콘
단계만 건너뛴다 (`winget install ImageMagick.ImageMagick` 설치 후 재실행하면 아이콘 생성).
관리 화면(/poe/admin)의 "실행 환경" 카드에서 설치 여부를 확인할 수 있다.

## 시즌 갱신 절차

1. 최신 패치 버전 확인: https://raw.githubusercontent.com/poe-tool-dev/latest-patch-version/main/latest.txt
2. `config.json` 의 `patch` 수정
3. `node run-all.mjs` (또는 웹 관리 메뉴에서 실행 — 완료 시 재시작 없이 반영)
   개별 실행이 필요하면:
   ```
   node extract.mjs        # 게임 번들에서 dat 테이블/스탯 설명 추출 (~/.poe-gamedata/work)
   node transform.mjs      # 스킬젬 표시용 JSON (skill-gems.json, 스탯 문장 조립 포함)
   node parse-uniques.mjs  # 고유 아이템 JSON (PoB 데이터 + 한국어 이름 결합, unique-items.json)
   node parse-items.mjs    # 일반(베이스) 아이템 JSON (base-items.json)
   node parse-tree.mjs     # 패시브 트리 JSON (passive-tree.json)
   node icons.mjs          # 젬 아이콘 DDS→PNG (icons/gems/*.png, ImageMagick 필요)
   ```
4. PoB 엔진 소스 갱신: `cd ~/.poe-gamedata/work/pob-src && git pull`

- 스키마가 깨지면: 커뮤니티 스키마(poe-tool-dev/dat-schema) 갱신 대기 후 재시도, 컬럼명 변경 시 config.json/transform.mjs 수정
- PoB 고유 아이템 원본 갱신: `~/.poe-gamedata/work/pob-uniques/` 삭제 후 parse-uniques.mjs 재실행
