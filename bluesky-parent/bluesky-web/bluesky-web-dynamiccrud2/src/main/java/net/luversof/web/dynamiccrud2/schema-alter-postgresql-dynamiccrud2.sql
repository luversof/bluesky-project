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
