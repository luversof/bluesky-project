CREATE TABLE "GoogleIamServiceAccountInfo" (
	"id" UUID  NOT NULL PRIMARY KEY,
	"user_id" UUID NOT NULL,
	"keyStr" TEXT NOT NULL
);

CREATE UNIQUE INDEX uk_googleIamServiceAccountInfo_userId ON "GoogleIamServiceAccountInfo" ("user_id");

CREATE TABLE "GoogleSpreadSheetInfo" (
	"id" UUID  NOT NULL PRIMARY KEY,
	"googleIamServiceAccountInfo_id" UUID NOT NULL,
	"type" VARCHAR(50) NOT NULL,
	"spreadsheetId" VARCHAR(255) NOT NULL,
	"range" VARCHAR(255) NOT NULL
);

CREATE INDEX idx_googleSpreadSheetInfo_googleIamServiceAccountInfo_id ON "GoogleSpreadSheetInfo" ("googleIamServiceAccountInfo_id");
CREATE UNIQUE INDEX uk_googleSpreadSheetInfo_googleIamServiceAccountInfo_id_type ON "GoogleSpreadSheetInfo" ("googleIamServiceAccountInfo_id", "type");
