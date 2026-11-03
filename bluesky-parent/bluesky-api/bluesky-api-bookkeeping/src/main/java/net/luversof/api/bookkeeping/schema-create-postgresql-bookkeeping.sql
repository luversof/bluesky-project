-- 가계부 최종 스키마(설계: bluesky-api-bookkeeping/DESIGN.md). 새 DB 는 이 파일만 실행한다.
-- 옛 스키마(Bookkeeping · AssetType · EntryType · 옛 Asset · 옛 Entry)가 있는 DB 는 schema-alter 를 먼저 실행한다.

-- 가계부. 소유 · 공유는 BookMember 로만 표현한다.
CREATE TABLE IF NOT EXISTS "Book" (
	"id" UUID NOT NULL PRIMARY KEY,
	"name" VARCHAR(100) NOT NULL,
	"baseCurrency" CHAR(3) NOT NULL,
	"monthStartDay" SMALLINT NOT NULL DEFAULT 1 CHECK ("monthStartDay" BETWEEN 1 AND 28),
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 가계부 멤버. OWNER 는 가계부당 1 명(uk_bookMember_owner).
CREATE TABLE IF NOT EXISTS "BookMember" (
	"book_id" UUID NOT NULL,
	"user_id" UUID NOT NULL,
	"role" VARCHAR(10) NOT NULL CHECK ("role" IN ('OWNER', 'MEMBER')),
	"joinedDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	PRIMARY KEY ("book_id", "user_id")
);

CREATE INDEX IF NOT EXISTS idx_bookMember_userId ON "BookMember" ("user_id");
CREATE UNIQUE INDEX IF NOT EXISTS uk_bookMember_owner ON "BookMember" ("book_id") WHERE "role" = 'OWNER';

-- 초대 링크. 1 회용, 만료 시각이 지나거나 usedBy 가 차면 못 쓴다.
CREATE TABLE IF NOT EXISTS "BookInvite" (
	"token" VARCHAR(64) NOT NULL PRIMARY KEY,
	"book_id" UUID NOT NULL,
	"createdBy" UUID NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"expiresAt" TIMESTAMP WITH TIME ZONE NOT NULL,
	"usedBy" UUID,
	"usedDate" TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_bookInvite_bookId ON "BookInvite" ("book_id");

-- 자산. 잔액은 저장하지 않는다(openingBalance + 내역 합으로 계산). 체크카드 kind 는 없다(지출은 은행 자산에 기록).
CREATE TABLE IF NOT EXISTS "Asset" (
	"id" UUID NOT NULL PRIMARY KEY,
	"book_id" UUID NOT NULL,
	"kind" VARCHAR(20) NOT NULL CHECK ("kind" IN ('CASH', 'BANK', 'CREDIT_CARD', 'INVESTMENT', 'LOAN', 'ETC')),
	"name" VARCHAR(100) NOT NULL,
	"currency" CHAR(3) NOT NULL,
	"openingBalance" NUMERIC(19,4) NOT NULL DEFAULT 0,
	"paymentDay" SMALLINT CHECK ("paymentDay" BETWEEN 1 AND 31),
	"displayOrder" INTEGER NOT NULL DEFAULT 0,
	"hidden" BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_asset_bookId ON "Asset" ("book_id");

-- 분류. parent_id 로 대 · 소 2 단계(3 단계 금지와 부모 kind 일치는 서비스가 검증).
CREATE TABLE IF NOT EXISTS "Category" (
	"id" UUID NOT NULL PRIMARY KEY,
	"book_id" UUID NOT NULL,
	"parent_id" UUID,
	"kind" VARCHAR(10) NOT NULL CHECK ("kind" IN ('INCOME', 'EXPENSE')),
	"name" VARCHAR(100) NOT NULL,
	"displayOrder" INTEGER NOT NULL DEFAULT 0,
	"hidden" BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_category_bookId ON "Category" ("book_id");

-- 내역(단식). 수입 · 지출은 분류 필수 · 이체 칼럼 없음, 이체는 받는 자산과 받는 금액 필수 · 분류 없음.
-- 같은 가계부 여부 · 분류 kind 와 type 일치 · 같은 통화 이체의 toAmount = amount 는 서비스가 검증한다.
CREATE TABLE IF NOT EXISTS "Entry" (
	"id" UUID NOT NULL PRIMARY KEY,
	"book_id" UUID NOT NULL,
	"type" VARCHAR(10) NOT NULL CHECK ("type" IN ('INCOME', 'EXPENSE', 'TRANSFER')),
	"entryDate" DATE NOT NULL,
	"entryTime" TIME,
	"amount" NUMERIC(19,4) NOT NULL CHECK ("amount" > 0),
	"toAmount" NUMERIC(19,4) CHECK ("toAmount" > 0),
	"asset_id" UUID NOT NULL,
	"toAsset_id" UUID,
	"category_id" UUID,
	"content" VARCHAR(200),
	"memo" VARCHAR(1000),
	"createdBy" UUID NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT ck_entry_typeColumns CHECK (
		("type" = 'TRANSFER' AND "toAsset_id" IS NOT NULL AND "toAmount" IS NOT NULL AND "category_id" IS NULL AND "toAsset_id" <> "asset_id")
		OR ("type" IN ('INCOME', 'EXPENSE') AND "toAsset_id" IS NULL AND "toAmount" IS NULL AND "category_id" IS NOT NULL)
	)
);

CREATE INDEX IF NOT EXISTS idx_entry_bookId_entryDate ON "Entry" ("book_id", "entryDate");
CREATE INDEX IF NOT EXISTS idx_entry_assetId ON "Entry" ("asset_id");
CREATE INDEX IF NOT EXISTS idx_entry_toAssetId ON "Entry" ("toAsset_id");
CREATE INDEX IF NOT EXISTS idx_entry_categoryId ON "Entry" ("category_id");
