CREATE TABLE "Bookkeeping" (
	"id" UUID NOT NULL PRIMARY KEY,
	"user_id" UUID NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"jsonConfig" JSONB
);

CREATE INDEX idx_bookkeeping_userId ON "Bookkeeping" ("user_id");

CREATE TABLE "AssetType" (
	"id" UUID NOT NULL PRIMARY KEY,
	"bookkeeping_id" UUID NOT NULL,
	"code" VARCHAR(100) NOT NULL,
	"name" VARCHAR(100) NOT NULL
);

CREATE INDEX idx_assetType_bookkeepingId ON "AssetType" ("bookkeeping_id");

CREATE TABLE "Asset" (
	"id" UUID NOT NULL PRIMARY KEY,
	"bookkeeping_id" UUID NOT NULL,
	"assetType_id" UUID NOT NULL,
	"assetTypeCode" VARCHAR(20) NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"jsonConfig" JSONB
);

CREATE INDEX idx_asset_bookkeepingId ON "Asset" ("bookkeeping_id");
CREATE INDEX idx_asset_assetTypeId ON "Asset" ("assetType_id");


CREATE TABLE "EntryType" (
	"id" UUID NOT NULL PRIMARY KEY,
	"bookkeeping_id" UUID NOT NULL,
	"code" VARCHAR(100) NOT NULL,
	"name" VARCHAR(100) NOT NULL
);

CREATE INDEX idx_entryType_bookkeepingId ON "EntryType" ("bookkeeping_id");

CREATE TABLE "Entry" (
	"id" UUID NOT NULL PRIMARY KEY,
	"bookkeeping_id" UUID NOT NULL,
	"entryType_id" UUID NOT NULL,
	"incomeAsset_id" UUID NOT NULL,
	"outgoingAsset_id" UUID NOT NULL,
	"entryDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"code" VARCHAR(100) NOT NULL,
	"name" VARCHAR(100) NOT NULL
);

CREATE INDEX idx_entryType_bookkeepingId ON "EntryType" ("bookkeeping_id");