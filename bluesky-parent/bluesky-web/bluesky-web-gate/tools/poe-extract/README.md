# poe-extract

PoE 게임 데이터 추출 파이프라인. 모든 산출물(중간물 포함)은 `~/.poe-gamedata/` 에 생성되며 git 으로 관리하지 않는다.

## 시즌 갱신 절차

1. 최신 패치 버전 확인: https://raw.githubusercontent.com/poe-tool-dev/latest-patch-version/main/latest.txt
2. `config.json` 의 `patch` 수정
3. 실행:
   ```
   node extract.mjs        # 게임 번들에서 dat 테이블/스탯 설명 추출 (~/.poe-gamedata/work)
   node transform.mjs      # 스킬젬 표시용 JSON (skill-gems.json, 스탯 문장 조립 포함)
   node parse-uniques.mjs  # 고유 아이템 JSON (PoB 데이터 + 한국어 이름 결합, unique-items.json)
   node icons.mjs          # 젬 아이콘 DDS→PNG (icons/gems/*.png, ImageMagick 필요)
   ```
4. 게이트 서버 재시작 (poe.data-dir 프로퍼티, 기본 `~/.poe-gamedata`)

- 스키마가 깨지면: 커뮤니티 스키마(poe-tool-dev/dat-schema) 갱신 대기 후 재시도, 컬럼명 변경 시 config.json/transform.mjs 수정
- PoB 고유 아이템 원본 갱신: `~/.poe-gamedata/work/pob-uniques/` 삭제 후 parse-uniques.mjs 재실행
