CREATE TABLE "Bookkeeping" (
	"id" UUID NOT NULL PRIMARY KEY,
	"user_id" UUID NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"jsonConfig" JSONB
);

CREATE INDEX idx_Bookkeeping_userId ON "Bookkeeping" ("user_id");

CREATE TABLE "AssetType" (
	"id" UUID NOT NULL PRIMARY KEY,
	"bookkeeping_id" UUID NOT NULL,
	"code" VARCHAR(100) NOT NULL,
	"name" VARCHAR(100) NOT NULL
);

CREATE INDEX idx_AssetType_bookkeepingId ON "AssetType" ("bookkeeping_id");
