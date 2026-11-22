-- 이미 schema-create-postgresql-dynamiccrud2.sql 로 만든 DB 를 최종 스키마로 맞추는 변경 쿼리(새 DB 는 create 파일만 실행하면 된다).
-- 위에서부터 순서대로, 아직 안 한 날짜 묶음만 실행한다. 모든 문장은 IF NOT EXISTS 등으로 두 번 실행해도 안전하다.

-- =====================================================================
-- 2026-11-17
-- =====================================================================

-- (1) 주소용 code 열: 사용 화면 주소가 /{프로젝트 code}/{메인 메뉴 code}/{메뉴 code} 가 된다(앱이 이 열을 읽으므로 앱 배포 전에 실행).
--     형식은 영문 소문자 · 숫자 · - (앱이 검사). 프로젝트 code 는 전체에서, 메인 메뉴는 프로젝트 안, 메뉴는 메인 메뉴 안에서 겹치지 않는다.
--     기존 행은 id 앞 8 글자로 임시 code 를 채운다(예: p01a1e828) - 관리 화면에서 원하는 값으로 바꾼다.
--     행이 적은 설정 테이블이라 잠금은 짧다.
ALTER TABLE "Project" ADD COLUMN IF NOT EXISTS "code" VARCHAR(50);
UPDATE "Project" SET "code" = 'p' || substr(replace("id"::text, '-', ''), 1, 8) WHERE "code" IS NULL;
ALTER TABLE "Project" ALTER COLUMN "code" SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_project_code ON "Project" ("code");

ALTER TABLE "MainMenu" ADD COLUMN IF NOT EXISTS "code" VARCHAR(50);
UPDATE "MainMenu" SET "code" = 'm' || substr(replace("id"::text, '-', ''), 1, 8) WHERE "code" IS NULL;
ALTER TABLE "MainMenu" ALTER COLUMN "code" SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_mainMenu_projectId_code ON "MainMenu" ("project_id", "code");

ALTER TABLE "Menu" ADD COLUMN IF NOT EXISTS "code" VARCHAR(50);
UPDATE "Menu" SET "code" = 'n' || substr(replace("id"::text, '-', ''), 1, 8) WHERE "code" IS NULL;
ALTER TABLE "Menu" ALTER COLUMN "code" SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_menu_mainMenuId_code ON "Menu" ("mainMenu_id", "code");

-- 확인(열 3 개 · 인덱스 3 개가 나오면 끝)
-- SELECT table_name, column_name, is_nullable FROM information_schema.columns WHERE column_name = 'code' AND table_name IN ('Project', 'MainMenu', 'Menu');
-- SELECT indexname FROM pg_indexes WHERE indexname IN ('uk_project_code', 'uk_mainmenu_projectid_code', 'uk_menu_mainmenuid_code');

-- 되돌리기(앱을 이전 판으로 돌린 뒤에만, 바꿔 둔 code 가 사라진다)
-- DROP INDEX IF EXISTS uk_project_code;
-- DROP INDEX IF EXISTS uk_mainMenu_projectId_code;
-- DROP INDEX IF EXISTS uk_menu_mainMenuId_code;
-- ALTER TABLE "Project" DROP COLUMN IF EXISTS "code";
-- ALTER TABLE "MainMenu" DROP COLUMN IF EXISTS "code";
-- ALTER TABLE "Menu" DROP COLUMN IF EXISTS "code";

-- 2026-11-18 메뉴 기본 페이지 크기 20 → 15
-- 까닭: 20 행이면 목록이 화면을 넘어 페이지를 넘길 때마다 스크롤을 내려야 한다(사용자 요청), 10 은 너무 적다(사용자). 앱의 새 메뉴 기본값도 15.
ALTER TABLE "Menu" ALTER COLUMN "pageSize" SET DEFAULT 15;
-- 기존 메뉴 중 기본값(20) 그대로인 것 · 앞서 안내한 10 을 적용했던 것만 15 로(관리 화면에서 직접 바꾼 값은 둔다). 원치 않으면 이 줄은 빼고 실행
UPDATE "Menu" SET "pageSize" = 15 WHERE "pageSize" IN (10, 20);

-- 확인
-- SELECT column_default FROM information_schema.columns WHERE table_name = 'Menu' AND column_name = 'pageSize';
-- SELECT "pageSize", count(*) FROM "Menu" GROUP BY "pageSize";

-- 되돌리기(UPDATE 로 바꾼 행은 어느 것이었는지 남지 않는다)
-- ALTER TABLE "Menu" ALTER COLUMN "pageSize" SET DEFAULT 20;

-- 2026-11-18 필드 "수정 가능"(updatable)
-- 까닭: 수정 작업에서 PK · unique 값은 바꾸면 안 된다(사용자 요청). 기존 필드는 NULL(안 정함)로 두면
-- 앱이 그 메뉴를 처음 쓸 때 "컬럼 다시 읽기" 를 한 번 해 PK · unique · 자동 증가를 FALSE 로 채운다.
-- 이 열이 없으면 앱이 필드를 못 읽으므로 앱 배포 전에 적용할 것.
ALTER TABLE "DbField" ADD COLUMN IF NOT EXISTS "updatable" BOOLEAN;
ALTER TABLE "DbField" ADD COLUMN IF NOT EXISTS "updatableUserSet" BOOLEAN NOT NULL DEFAULT FALSE;

-- 확인(열 2 개)
-- SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'DbField' AND column_name IN ('updatable', 'updatableUserSet');

-- 되돌리기(앱을 이전 판으로 돌린 뒤에만)
-- ALTER TABLE "DbField" DROP COLUMN IF EXISTS "updatable";
-- ALTER TABLE "DbField" DROP COLUMN IF EXISTS "updatableUserSet";

-- 2026-11-18 메뉴 검색 설정(searchSettings)
-- 까닭: 쿼리를 고치지 않고 메뉴 설정에서 검색칸 · 통합 검색을 고르게 한다(사용자 요청). NULL = 검색칸 없음.
-- 이 열이 없으면 앱이 메뉴를 못 읽으므로 앱 배포 전에 적용할 것.
ALTER TABLE "Menu" ADD COLUMN IF NOT EXISTS "searchSettings" JSONB;

-- 확인(열 1 개)
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'Menu' AND column_name = 'searchSettings';

-- 되돌리기(앱을 이전 판으로 돌린 뒤에만, 고른 검색칸이 사라진다)
-- ALTER TABLE "Menu" DROP COLUMN IF EXISTS "searchSettings";

-- 2026-11-18 프로젝트 · 메인 메뉴 노출 여부(enableDisplay)
-- 까닭: 메뉴처럼 프로젝트 · 메인 메뉴도 사용 화면 메뉴 목록에서 숨길 수 있게 한다(사용자 요청). 기존 행은 TRUE(지금처럼 보임).
-- 이 열이 없으면 앱이 프로젝트 · 메인 메뉴를 못 읽으므로 앱 배포 전에 적용할 것.
ALTER TABLE "Project" ADD COLUMN IF NOT EXISTS "enableDisplay" BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE "MainMenu" ADD COLUMN IF NOT EXISTS "enableDisplay" BOOLEAN NOT NULL DEFAULT TRUE;

-- 확인(열 2 개, 모두 TRUE)
-- SELECT table_name, column_name, data_type, column_default FROM information_schema.columns WHERE table_name IN ('Project', 'MainMenu') AND column_name = 'enableDisplay';
-- SELECT 'Project', "enableDisplay", count(*) FROM "Project" GROUP BY "enableDisplay" UNION ALL SELECT 'MainMenu', "enableDisplay", count(*) FROM "MainMenu" GROUP BY "enableDisplay";

-- 되돌리기(앱을 이전 판으로 돌린 뒤에만, 숨김 설정이 사라진다)
-- ALTER TABLE "Project" DROP COLUMN IF EXISTS "enableDisplay";
-- ALTER TABLE "MainMenu" DROP COLUMN IF EXISTS "enableDisplay";

-- 2026-11-19 설정 변경 이력(SettingLog)
-- 까닭: 관리 화면에서 바꾼 프로젝트 · 메인 메뉴 · 메뉴 · 쿼리 · 검색 · 필드 설정을 "누가 · 언제 · 무엇을 · 이전 → 이후" 로 남긴다(사용자 요청).
-- 사용 화면의 데이터 변경 이력(ChangeLog)과 따로 저장 · 조회한다. 기존 데이터는 없다(새 테이블).
-- 이 테이블이 없어도 앱은 설정을 저장한다(이력만 못 남기고 로그에 [SETTING_LOG_FAILED]). 이력 화면의 "설정" 탭은 오류가 나므로 앱 배포 전에 적용할 것.
CREATE TABLE IF NOT EXISTS "SettingLog" (
	"id" UUID NOT NULL PRIMARY KEY,
	"project_id" UUID,
	"menu_id" UUID,
	"target" VARCHAR(20) NOT NULL,
	"targetId" UUID,
	"targetName" VARCHAR(200),
	"action" VARCHAR(20) NOT NULL,
	"username" VARCHAR(100) NOT NULL,
	"changes" JSONB,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_settingLog_menuId_createdDate ON "SettingLog" ("menu_id", "createdDate");
CREATE INDEX IF NOT EXISTS idx_settingLog_projectId_createdDate ON "SettingLog" ("project_id", "createdDate");
CREATE INDEX IF NOT EXISTS idx_settingLog_targetId_createdDate ON "SettingLog" ("targetId", "createdDate");

-- 확인(테이블 1 개 · 인덱스 3 개)
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'SettingLog' ORDER BY ordinal_position;
-- SELECT indexname FROM pg_indexes WHERE tablename = 'SettingLog';

-- 되돌리기(앱을 이전 판으로 돌린 뒤에만, 설정 변경 이력이 사라진다)
-- DROP TABLE IF EXISTS "SettingLog";

-- 2026-11-22 필드 선택 설정(DbField.options, JSONB) - 값 목록(valueOptions 키) · 날짜 표시 형식(dateFormat 키)
-- 까닭: enum · 코드 숫자를 화면에서 다른 글자로 보이고(검색 · 입력은 셀렉트), 날짜 · 일시를 정한 형식으로 보인다(사용자 요청).
--      새 설정마다 열 · DDL 을 늘리지 않으려고 JSONB 하나에 키로 둔다(2026-11-23 사용자 결정, 기존 열은 그대로).
-- 그사이 잠시 배포했던 모양("valueOptions" TEXT 열 → "displayOptions" JSONB 열)을 적용한 DB 는 값을 옮기고 그 열을 지운다.
-- 처음 적용하는 DB 는 열 추가만 한다. 앱 배포 전에 적용할 것(이 열이 없으면 앱이 필드를 못 읽는다).
DO $$
BEGIN
	IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'DbField' AND column_name = 'displayOptions')
		AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'DbField' AND column_name = 'options') THEN
		ALTER TABLE "DbField" RENAME COLUMN "displayOptions" TO "options";
	END IF;
END $$;
ALTER TABLE "DbField" ADD COLUMN IF NOT EXISTS "options" JSONB;
DO $$
BEGIN
	IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'DbField' AND column_name = 'valueOptions') THEN
		UPDATE "DbField" SET "options" = coalesce("options", '{}'::jsonb) || jsonb_build_object('valueOptions', "valueOptions")
		WHERE "valueOptions" IS NOT NULL AND NOT coalesce("options" ? 'valueOptions', false);
		ALTER TABLE "DbField" DROP COLUMN "valueOptions";
	END IF;
END $$;

-- 확인(options 하나만)
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'DbField' AND column_name IN ('valueOptions', 'displayOptions', 'options');

-- 되돌리기(앱을 이전 판으로 돌린 뒤에만, 값 목록 · 날짜 형식 설정이 사라진다)
-- ALTER TABLE "DbField" DROP COLUMN IF EXISTS "options";
