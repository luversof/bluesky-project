CREATE TABLE "Board" (
	"id" UUID NOT NULL PRIMARY KEY,
	"alias" VARCHAR(15) NOT NULL,
	"bitConfig" BYTEA,
	"jsonConfig" JSONB,
	CONSTRAINT uk_board_alias UNIQUE ("alias")
);

-- BoardArticle 테이블
CREATE TABLE "BoardArticle" (
	"id" UUID NOT NULL PRIMARY KEY,
	"board_id" UUID NOT NULL,
	"user_id" VARCHAR(36) NOT NULL,
	"title" VARCHAR(255),
	"content" TEXT,
	"createdDate" TIMESTAMPZ,
	"lastModifiedDate" TIMESTAMPZ
);

-- 인덱스 생성
CREATE INDEX idx_article_boardId ON "BoardArticle" ("board_id");
CREATE INDEX idx_article_userId ON "BoardArticle" ("user_id");