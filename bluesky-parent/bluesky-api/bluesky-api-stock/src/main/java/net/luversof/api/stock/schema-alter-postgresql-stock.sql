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

-- =====================================================================
-- 2026-10-02
-- =====================================================================

-- (1) 시세 원주가(수정 전 종가) 열. 기존 행은 NULL 로 남고, 앱이 매매가 있던 날의 행만 채운다(KIS 일별 시세 FID_ORG_ADJ_PRC=1).
--     평가 수량의 분할 · 병합 계수를 "원주가 / 수정 종가" 로 정확히 내려고 쓴다. 지금은 거래가 / 수정 종가를 정수배로 반올림해 추정해서
--     43 종목 중 14 종목이 어긋난다(공모주 1/2 오판 - 하이브 · HD현대중공업 · 나노팀, 비정수 계수 - 한화오션 4.45 · 쌍방울 1/40~48 · 초록뱀미디어).
--     잠금은 짧다(기본값 없는 NULL 열 추가). 앱은 열이 없어도 동작한다(있으면 쓰고, 없으면 지금 추정 그대로).
ALTER TABLE "StockPriceHistory" ADD COLUMN IF NOT EXISTS "rawClosePrice" NUMERIC;

-- 확인(한 줄이 나오면 끝)
-- SELECT column_name, data_type, is_nullable FROM information_schema.columns
--  WHERE table_name = 'StockPriceHistory' AND column_name = 'rawClosePrice';

-- 되돌리기(채운 원주가가 사라진다 - 평가는 정수배 추정으로 돌아간다)
-- ALTER TABLE "StockPriceHistory" DROP COLUMN IF EXISTS "rawClosePrice";
