-- dynamiccrud2 설정 catalog (PostgreSQL). 설계: 모듈 루트 DESIGN.md
-- 묶음(aggregate): Project · MainMenu · Menu(+MenuQuery) · DbTable(+DbField) · ChangeLog

CREATE TABLE "Project" (
	"id" UUID NOT NULL PRIMARY KEY,
	"name" VARCHAR(100) NOT NULL,
	"displayOrder" INTEGER NOT NULL DEFAULT 0,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE "MainMenu" (
	"id" UUID NOT NULL PRIMARY KEY,
	"project_id" UUID NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"displayOrder" INTEGER NOT NULL DEFAULT 0,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_mainMenu_projectId ON "MainMenu" ("project_id");

-- 대상 DB 테이블. 같은 테이블을 쓰는 메뉴들이 필드(DbField)를 공유한다.
CREATE TABLE "DbTable" (
	"id" UUID NOT NULL PRIMARY KEY,
	-- connectionInfo 의 connection 이름
	"connectionName" VARCHAR(100) NOT NULL,
	"catalogName" VARCHAR(128),
	"schemaName" VARCHAR(128),
	"tableName" VARCHAR(128) NOT NULL,
	-- 마지막 "컬럼 다시 읽기" 시각
	"columnsReadDate" TIMESTAMP WITH TIME ZONE,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

-- catalog · schema 는 DB 마다 없을 수 있어(NULL) 빈 문자열로 맞춰 유일성을 건다
CREATE UNIQUE INDEX uk_dbTable_location ON "DbTable" ("connectionName", COALESCE("catalogName", ''), COALESCE("schemaName", ''), "tableName");

-- 테이블 단위 필드 메타. PK · unique 는 저장하지 않고 쓸 때 DB 에서 읽는다.
CREATE TABLE "DbField" (
	"dbTable_id" UUID NOT NULL,
	"columnName" VARCHAR(128) NOT NULL,
	-- 계산 · 함수 결과처럼 DB 에 없는 조회 전용 필드
	"virtual" BOOLEAN NOT NULL DEFAULT FALSE,
	-- 사용자 지정 라벨. 화면 제목 = 쿼리 별칭 → label → dbComment → columnName
	"label" VARCHAR(200),
	"dbComment" VARCHAR(1000),
	-- DB 가 알려 준 타입 이름(예: varchar, int4)
	"dataType" VARCHAR(100),
	-- TEXT · NUMBER · DATE · DATETIME · BOOLEAN · TEXTAREA
	"inputType" VARCHAR(20) NOT NULL,
	"required" BOOLEAN NOT NULL DEFAULT FALSE,
	"maxLength" INTEGER,
	"autoIncrement" BOOLEAN NOT NULL DEFAULT FALSE,
	"sortable" BOOLEAN NOT NULL DEFAULT FALSE,
	-- 사용자가 sortable 을 바꿨으면 "컬럼 다시 읽기" 가 덮어쓰지 않는다
	"sortableUserSet" BOOLEAN NOT NULL DEFAULT FALSE,
	-- 다시 읽었을 때 DB 에서 사라진 컬럼(지우지 않고 표시)
	"missingInDb" BOOLEAN NOT NULL DEFAULT FALSE,
	"displayOrder" INTEGER NOT NULL DEFAULT 0,
	PRIMARY KEY ("dbTable_id", "columnName")
);

CREATE TABLE "Menu" (
	"id" UUID NOT NULL PRIMARY KEY,
	"mainMenu_id" UUID NOT NULL,
	-- 메뉴 등록 때 고른 기준 테이블
	"dbTable_id" UUID NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	-- 역할 문자열 하나(예: ROLE_ADMIN). NULL = 로그인한 사용자 누구나
	"authority" VARCHAR(50),
	"pageSize" INTEGER NOT NULL DEFAULT 20,
	"enableExcel" BOOLEAN NOT NULL DEFAULT FALSE,
	"enableDisplay" BOOLEAN NOT NULL DEFAULT TRUE,
	"displayOrder" INTEGER NOT NULL DEFAULT 0,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_menu_mainMenuId ON "Menu" ("mainMenu_id");
CREATE INDEX idx_menu_dbTableId ON "Menu" ("dbTable_id");

-- 메뉴의 작업. READ 는 메뉴당 1개, INSERT · UPDATE · DELETE 는 N 개.
CREATE TABLE "MenuQuery" (
	"id" UUID NOT NULL PRIMARY KEY,
	"menu_id" UUID NOT NULL,
	-- READ · INSERT · UPDATE · DELETE
	"kind" VARCHAR(10) NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	-- :name 파라미터를 쓴 쿼리 원문(저장 시 고치지 않는다)
	"queryText" TEXT NOT NULL,
	-- 파라미터 이름별 설정(역할 · 필수 · 입력 형식 · 기본값)
	"paramSettings" JSONB,
	-- 작업 실행 권한. NULL = 메뉴 권한을 따른다
	"authority" VARCHAR(50),
	"displayOrder" INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_menuQuery_menuId ON "MenuQuery" ("menu_id");
CREATE UNIQUE INDEX uk_menuQuery_menuId_read ON "MenuQuery" ("menu_id") WHERE "kind" = 'READ';

-- 입력 · 수정 · 삭제 실행 이력
CREATE TABLE "ChangeLog" (
	"id" UUID NOT NULL PRIMARY KEY,
	"menu_id" UUID NOT NULL,
	"menuQuery_id" UUID NOT NULL,
	"username" VARCHAR(100) NOT NULL,
	"bindValues" JSONB,
	"affectedRows" INTEGER NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_changeLog_menuId_createdDate ON "ChangeLog" ("menu_id", "createdDate");
