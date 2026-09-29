-- 이미 schema-create-postgresql-stock.sql 로 만든 DB 를 최종 스키마로 맞추는 변경 쿼리(새 DB 는 create 파일만 실행하면 된다).
-- 위에서부터 순서대로, 아직 안 한 날짜 묶음만 실행한다. 모든 문장은 IF NOT EXISTS 라 두 번 실행해도 안전하다.

-- =====================================================================
-- 2026-09-28
-- =====================================================================

-- (1) 월배당 ETF 총보수 · 상장일 열. 기존 행은 NULL 로 남고 앱이 채운다. 잠금은 짧다(기본값 없는 NULL 열 추가).
ALTER TABLE "MonthlyDividendProfile" ADD COLUMN IF NOT EXISTS "totalExpenseRatioPct" NUMERIC(6,4);
ALTER TABLE "MonthlyDividendProfile" ADD COLUMN IF NOT EXISTS "listingDate" DATE;

-- (2) 종가 조회용 커버링 인덱스. 만드는 동안 StockPriceHistory 쓰기가 잠긴다(시세 갱신을 안 돌릴 때 실행).
--     쓰기를 막으면 안 되는 환경이면 아래 줄 대신 CONCURRENTLY 판을 트랜잭션 밖에서 실행:
--     CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stockPriceHistory_item_date_close ON "StockPriceHistory" ("stockItem_id", "tradeDate") INCLUDE ("closePrice");
CREATE INDEX IF NOT EXISTS idx_stockPriceHistory_item_date_close ON "StockPriceHistory" ("stockItem_id", "tradeDate") INCLUDE ("closePrice");

-- 확인(열 2 개 · 인덱스 1 개가 나오면 끝)
-- SELECT column_name, data_type, numeric_precision, numeric_scale FROM information_schema.columns
--  WHERE table_name = 'MonthlyDividendProfile' AND column_name IN ('totalExpenseRatioPct', 'listingDate');
-- SELECT indexname, indexdef FROM pg_indexes WHERE indexname = 'idx_stockpricehistory_item_date_close';

-- 되돌리기
-- DROP INDEX IF EXISTS idx_stockPriceHistory_item_date_close;
-- 열 삭제는 채운 값이 사라진다 - 필요할 때만:
-- ALTER TABLE "MonthlyDividendProfile" DROP COLUMN IF EXISTS "totalExpenseRatioPct", DROP COLUMN IF EXISTS "listingDate";
