-- 이미 만든 가계부 DB 를 최종 스키마(schema-create-postgresql-bookkeeping.sql)로 맞추는 변경 쿼리. 새 DB 는 create 파일만 실행하면 된다.
-- 위에서부터 순서대로, 아직 안 한 날짜 묶음만 실행한다. 모든 문장은 두 번 실행해도 안전하다.

-- =====================================================================
-- 2026-11-03 가계부 전면 개편(DESIGN.md)
-- =====================================================================

-- (1) 옛 스키마 삭제. 기존 데이터는 보존하지 않기로 했다(설계 합의).
--     옛 Asset · Entry 는 새 테이블과 이름이 같다. 그래서 옛 스키마 표식인 "Bookkeeping" 테이블이 있을 때만 지운다.
--     다시 실행해도 새 Asset · Entry(새 데이터)는 지우지 않는다.
DO $$
BEGIN
	IF to_regclass('"Bookkeeping"') IS NOT NULL THEN
		DROP TABLE IF EXISTS "Entry", "EntryType", "Asset", "AssetType", "Bookkeeping";
	END IF;
END
$$;

-- (2) 이어서 schema-create-postgresql-bookkeeping.sql 전체를 실행한다(모든 문장이 IF NOT EXISTS).

-- 확인(6 줄이 나오고, 옛 테이블 Bookkeeping · AssetType · EntryType 는 안 나오면 끝)
-- SELECT table_name FROM information_schema.tables
--  WHERE table_schema = current_schema() AND table_name IN ('Book', 'BookMember', 'BookInvite', 'Asset', 'Category', 'Entry', 'Bookkeeping', 'AssetType', 'EntryType')
--  ORDER BY table_name;
-- 새 Asset 인지(옛 Asset 에는 assetType_id 가 있다): 'kind' 한 줄이 나와야 한다
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'Asset' AND column_name IN ('kind', 'assetType_id');

-- 되돌리기: 없음. 옛 데이터는 (1) 에서 사라진다. 필요하면 실행 전에 pg_dump 로 받아 둔다:
--   pg_dump -t '"Bookkeeping"' -t '"AssetType"' -t '"EntryType"' -t '"Asset"' -t '"Entry"' <db> > bookkeeping-old.sql
-- 새 스키마만 걷어 낼 때:
-- DROP TABLE IF EXISTS "Entry", "Category", "Asset", "BookInvite", "BookMember", "Book";

-- =====================================================================
-- 2026-11-03 (2차) 예산 · 즐겨찾기 · 반복 내역 · 할부 · 환율(DESIGN.md 2차)
-- =====================================================================

-- 새 테이블 5개와 Entry 칼럼 1개. 모두 IF NOT EXISTS 라 두 번 실행해도 안전하다. 잠금은 짧다(NULL 칼럼 추가).

-- 할부 개월 수(카드 지출만, 2~36). 기존 행은 NULL(일시불).
ALTER TABLE "Entry" ADD COLUMN IF NOT EXISTS "installmentMonths" SMALLINT CHECK ("installmentMonths" BETWEEN 2 AND 36);

-- 예산: 매달 같은 금액. category_id 가 NULL 이면 가계부 전체 지출 예산, 아니면 대분류 지출 예산. 기준통화 기준.
CREATE TABLE IF NOT EXISTS "Budget" (
	"id" UUID NOT NULL PRIMARY KEY,
	"book_id" UUID NOT NULL,
	"category_id" UUID,
	"amount" NUMERIC(19,4) NOT NULL CHECK ("amount" > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_budget_book_total ON "Budget" ("book_id") WHERE "category_id" IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_budget_book_category ON "Budget" ("book_id", "category_id") WHERE "category_id" IS NOT NULL;

-- 즐겨찾기: 이름 붙인 내역 틀. 값은 비워 둘 수 있다(모달을 채우는 용도).
CREATE TABLE IF NOT EXISTS "BookFavorite" (
	"id" UUID NOT NULL PRIMARY KEY,
	"book_id" UUID NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"type" VARCHAR(10) NOT NULL CHECK ("type" IN ('INCOME', 'EXPENSE', 'TRANSFER')),
	"amount" NUMERIC(19,4) CHECK ("amount" > 0),
	"toAmount" NUMERIC(19,4) CHECK ("toAmount" > 0),
	"asset_id" UUID,
	"toAsset_id" UUID,
	"category_id" UUID,
	"content" VARCHAR(200),
	"memo" VARCHAR(1000),
	"displayOrder" INTEGER NOT NULL DEFAULT 0,
	"createdBy" UUID NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_bookFavorite_bookId ON "BookFavorite" ("book_id");

-- 반복 내역 틀. 자동으로 넣지 않는다 - 화면이 도래한 회차를 보여 주고 사용자가 넣는다(RecurringDone 이 중복을 막는다).
-- MONTHLY = 매월 dayOfMonth 일(그 달에 없으면 말일), WEEKLY = 매주 dayOfWeek(1 월 ~ 7 일), YEARLY = 매년 monthOfYear 월 dayOfMonth 일.
CREATE TABLE IF NOT EXISTS "BookRecurring" (
	"id" UUID NOT NULL PRIMARY KEY,
	"book_id" UUID NOT NULL,
	"type" VARCHAR(10) NOT NULL CHECK ("type" IN ('INCOME', 'EXPENSE', 'TRANSFER')),
	"amount" NUMERIC(19,4) NOT NULL CHECK ("amount" > 0),
	"toAmount" NUMERIC(19,4) CHECK ("toAmount" > 0),
	"asset_id" UUID NOT NULL,
	"toAsset_id" UUID,
	"category_id" UUID,
	"content" VARCHAR(200),
	"memo" VARCHAR(1000),
	"cycle" VARCHAR(10) NOT NULL CHECK ("cycle" IN ('MONTHLY', 'WEEKLY', 'YEARLY')),
	"dayOfMonth" SMALLINT CHECK ("dayOfMonth" BETWEEN 1 AND 31),
	"dayOfWeek" SMALLINT CHECK ("dayOfWeek" BETWEEN 1 AND 7),
	"monthOfYear" SMALLINT CHECK ("monthOfYear" BETWEEN 1 AND 12),
	"startDate" DATE NOT NULL,
	"endDate" DATE,
	"createdBy" UUID NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT ck_recurring_cycle CHECK (
		("cycle" = 'MONTHLY' AND "dayOfMonth" IS NOT NULL)
		OR ("cycle" = 'WEEKLY' AND "dayOfWeek" IS NOT NULL)
		OR ("cycle" = 'YEARLY' AND "dayOfMonth" IS NOT NULL AND "monthOfYear" IS NOT NULL)
	),
	CONSTRAINT ck_recurring_typeColumns CHECK (
		("type" = 'TRANSFER' AND "toAsset_id" IS NOT NULL AND "category_id" IS NULL AND "toAsset_id" <> "asset_id")
		OR ("type" IN ('INCOME', 'EXPENSE') AND "toAsset_id" IS NULL AND "toAmount" IS NULL AND "category_id" IS NOT NULL)
	)
);
CREATE INDEX IF NOT EXISTS idx_bookRecurring_bookId ON "BookRecurring" ("book_id");

-- 반복 틀에서 넣은 회차. 같은 회차를 두 번 넣지 않는다.
CREATE TABLE IF NOT EXISTS "RecurringDone" (
	"recurring_id" UUID NOT NULL,
	"occurrenceDate" DATE NOT NULL,
	"entry_id" UUID NOT NULL,
	PRIMARY KEY ("recurring_id", "occurrenceDate")
);

-- 월 환율: 외화 1 단위 = 기준통화 rate. 월은 가계부 월(YYYY-MM).
CREATE TABLE IF NOT EXISTS "BookFxRate" (
	"book_id" UUID NOT NULL,
	"month" CHAR(7) NOT NULL CHECK ("month" ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
	"currency" CHAR(3) NOT NULL,
	"rate" NUMERIC(19,8) NOT NULL CHECK ("rate" > 0),
	PRIMARY KEY ("book_id", "month", "currency")
);

-- 확인(5 줄 + 1 줄이 나오면 끝)
-- SELECT table_name FROM information_schema.tables WHERE table_name IN ('Budget', 'BookFavorite', 'BookRecurring', 'RecurringDone', 'BookFxRate') ORDER BY 1;
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'Entry' AND column_name = 'installmentMonths';

-- 되돌리기(2차 데이터가 사라진다)
-- DROP TABLE IF EXISTS "RecurringDone", "BookRecurring", "BookFavorite", "Budget", "BookFxRate";
-- ALTER TABLE "Entry" DROP COLUMN IF EXISTS "installmentMonths";

-- =====================================================================
-- 2026-11-04 수입 · 지출 0원 허용
-- =====================================================================
-- 까닭: 복지카드처럼 실제로 나간 돈은 0 이고 쓴 금액은 메모에 적는 내역이 있다.
--       내역 · 반복 틀 · 즐겨찾기의 amount 를 0 이상으로 넓힌다(이체 0원은 API 가 막는다). 예산은 그대로 0 보다 커야.
-- 다시 실행해도 안전하다(지우고 다시 만든다).
ALTER TABLE "Entry" DROP CONSTRAINT IF EXISTS "Entry_amount_check";
ALTER TABLE "Entry" ADD CONSTRAINT "Entry_amount_check" CHECK ("amount" >= 0);
ALTER TABLE "BookRecurring" DROP CONSTRAINT IF EXISTS "BookRecurring_amount_check";
ALTER TABLE "BookRecurring" ADD CONSTRAINT "BookRecurring_amount_check" CHECK ("amount" >= 0);
ALTER TABLE "BookFavorite" DROP CONSTRAINT IF EXISTS "BookFavorite_amount_check";
ALTER TABLE "BookFavorite" ADD CONSTRAINT "BookFavorite_amount_check" CHECK ("amount" >= 0);

-- 확인(3 줄 모두 >= 0 이면 끝)
-- SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conname IN ('Entry_amount_check', 'BookRecurring_amount_check', 'BookFavorite_amount_check');

-- 되돌리기(0원 내역이 있으면 실패한다 - 먼저 지우거나 금액을 고칠 것)
-- ALTER TABLE "Entry" DROP CONSTRAINT IF EXISTS "Entry_amount_check";
-- ALTER TABLE "Entry" ADD CONSTRAINT "Entry_amount_check" CHECK ("amount" > 0);
-- (BookRecurring · BookFavorite 도 같은 방식)
