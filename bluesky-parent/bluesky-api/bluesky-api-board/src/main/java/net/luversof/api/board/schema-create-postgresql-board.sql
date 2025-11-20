CREATE TABLE "Board" (
	"id" UUID NOT NULL PRIMARY KEY,
	"alias" VARCHAR(15) NOT NULL,
	"bitConfig" BYTEA,
	"jsonConfig" JSONB
);

CREATE UNIQUE INDEX uidx_board_alias ON "Board" ("alias");

-- BoardArticle 테이블
CREATE TABLE "BoardArticle" (
	"id" UUID NOT NULL PRIMARY KEY,
	"board_id" UUID NOT NULL,
	"user_id" UUID NOT NULL,
	"title" VARCHAR(255) NOT NULL,
	"content" TEXT NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"lastModifiedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 인덱스 생성
CREATE INDEX idx_article_boardId ON "BoardArticle" ("board_id");
CREATE INDEX idx_article_userId ON "BoardArticle" ("user_id");

-- BoardArticleComment 테이블
CREATE TABLE "BoardArticleComment" (
	"id" UUID NOT NULL PRIMARY KEY,
	"boardArticle_id" UUID NOT NULL,
	"user_id" UUID NOT NULL,
	"content" TEXT NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"lastModifiedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_comment_boardArticleId ON "BoardArticleComment" ("boardArticle_id");
CREATE INDEX idx_comment_userId ON "BoardArticleComment" ("user_id");
