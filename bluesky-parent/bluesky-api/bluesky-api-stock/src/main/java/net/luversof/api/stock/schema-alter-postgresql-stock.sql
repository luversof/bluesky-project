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

-- =====================================================================
-- 2026-10-07
-- =====================================================================

-- (1) 지급 이력의 주당 과세표준을 "확인 안 됨"(NULL)으로 둘 수 있게 한다. 운용사 화면이 과세표준을 "-" · 빈 칸으로 보여 줄 때
--     예전 가져오기는 0 원으로 저장해 진짜 0 원과 구분되지 않았고, 다음 갱신이 그 행을 채우지 못했다.
--     앱은 이제 모르면 NULL 로 두고(MonthlyDividendPayoutImportParser), 갱신에서 숫자가 오면 채운다(MonthlyDividendPayoutService.mergeTaxableBase).
--     저장된 0 은 출처가 다시 "-" 를 주면 NULL 로 되돌린다. 과세표준 비중은 값을 아는 행만으로 낸다.
--     이 DDL 전에 "-" 가 든 출처를 가져오면 NULL 을 넣지 못해 저장이 실패한다 - 이것부터 적용한다.
--     잠금은 짧다(NOT NULL 제약만 푼다, 행을 다시 쓰지 않는다).
ALTER TABLE "MonthlyDividendPayout" ALTER COLUMN "taxableBasePerShare" DROP NOT NULL;

-- 확인(is_nullable = YES 가 나오면 끝)
-- SELECT column_name, is_nullable FROM information_schema.columns
--  WHERE table_name = 'MonthlyDividendPayout' AND column_name = 'taxableBasePerShare';

-- 적용 뒤: 월배당 종목마다 출처에서 다시 가져오기 - 운용사가 "-" 로 보여 주는 행은 NULL(확인 안 됨)로, 진짜 0 원은 0 으로 정리된다.

-- 되돌리기(NULL 행이 있으면 먼저 0 으로 채워야 한다 - 그러면 "확인 안 됨" 이 다시 0 원이 된다)
-- UPDATE "MonthlyDividendPayout" SET "taxableBasePerShare" = 0 WHERE "taxableBasePerShare" IS NULL;
-- ALTER TABLE "MonthlyDividendPayout" ALTER COLUMN "taxableBasePerShare" SET NOT NULL;

-- =====================================================================
-- 2026-11-02
-- =====================================================================

-- (1) 월배당 스냅숏의 과세표준 비중을 모르면 NULL(확인 안 됨)로 둔다(사용자 결정 2026-11-02).
--     지금은 NOT NULL 이라 원장 실적도 운용사 과세표준도 없으면 0 으로 저장돼 "비과세" 로 읽혔다(10-07 의 "모르면 null" 규칙과 어긋남).
--     앱은 이제 모르면 NULL 을 넣는다(MonthlyDividendPayoutService.applyStats · MonthlyDividendSnapshotService.upsert) -
--     이 DDL 전에 앱을 올리면 그런 종목의 저장이 실패한다. 이것부터 적용한다.
--     잠금은 짧다(NOT NULL 제약만 푼다, 행을 다시 쓰지 않는다).
ALTER TABLE "MonthlyDividendSnapshot" ALTER COLUMN "averageTaxableBaseRatio1y" DROP NOT NULL;

-- 확인(is_nullable = YES 가 나오면 끝)
-- SELECT column_name, is_nullable FROM information_schema.columns
--  WHERE table_name = 'MonthlyDividendSnapshot' AND column_name = 'averageTaxableBaseRatio1y';

-- 적용 뒤: 이미 0 으로 저장된 행은 진짜 0(비과세)인지 모름인지 구분할 수 없어 그대로 둔다. 지급 이력을 다시 가져오면(관리 > 월배당 참조)
-- 그 종목 스냅숏을 다시 계산해 모르는 것은 NULL 로 바뀐다.

-- 되돌리기(NULL 행이 있으면 먼저 0 으로 채워야 한다 - 그러면 "확인 안 됨" 이 다시 0% 가 된다)
-- UPDATE "MonthlyDividendSnapshot" SET "averageTaxableBaseRatio1y" = 0 WHERE "averageTaxableBaseRatio1y" IS NULL;
-- ALTER TABLE "MonthlyDividendSnapshot" ALTER COLUMN "averageTaxableBaseRatio1y" SET NOT NULL;


-- 시황 분석(2026-11-09): 뉴스 · 공시, 방향 예측, 채점. 예측은 넣기만 하고 고치지 않는다 - 채점은 별도 테이블에 쌓는다.
CREATE TABLE IF NOT EXISTS "StockNews" (
	"id" UUID NOT NULL PRIMARY KEY,
	"stockItem_id" UUID NOT NULL,
	-- NAVER(뉴스) | DART(공시)
	"source" VARCHAR(10) NOT NULL,
	-- NAVER: 기사 원문 URL, DART: 접수번호(rcept_no)
	"externalId" VARCHAR(1000) NOT NULL,
	"title" VARCHAR(500) NOT NULL,
	"summary" VARCHAR(2000),
	"url" VARCHAR(2000),
	"publishedDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stockNews_stockItemId_source_externalId ON "StockNews" ("stockItem_id", "source", "externalId");
CREATE INDEX IF NOT EXISTS idx_stockNews_publishedDate ON "StockNews" ("publishedDate");

CREATE TABLE IF NOT EXISTS "StockPrediction" (
	"id" UUID NOT NULL PRIMARY KEY,
	"stockItem_id" UUID NOT NULL,
	-- 예측의 기준 거래일(그때 DB 의 마지막 종가 날짜)과 그 종가
	"baseDate" DATE NOT NULL,
	"basePrice" NUMERIC NOT NULL,
	-- 1 | 5 (거래일)
	"horizonDays" SMALLINT NOT NULL,
	-- UP | DOWN | FLAT
	"direction" VARCHAR(4) NOT NULL,
	"confidence" SMALLINT NOT NULL,
	"rationale" VARCHAR(1000),
	"modelId" VARCHAR(100) NOT NULL,
	"promptVersion" VARCHAR(50) NOT NULL,
	-- 예측에 쓴 입력(지표 · 뉴스) 그대로
	"inputSnapshot" JSONB,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stockPrediction_stockItemId_baseDate_horizonDays ON "StockPrediction" ("stockItem_id", "baseDate", "horizonDays");

CREATE TABLE IF NOT EXISTS "StockPredictionGrade" (
	"id" UUID NOT NULL PRIMARY KEY,
	"prediction_id" UUID NOT NULL,
	"targetDate" DATE NOT NULL,
	"actualClose" NUMERIC NOT NULL,
	"actualChangePct" NUMERIC(12,4) NOT NULL,
	"actualDirection" VARCHAR(4) NOT NULL,
	"hit" BOOLEAN NOT NULL,
	-- 기준일 다음 거래일 09:00(KST) 이후에 만든 예측 - 장중 정보가 섞여 통계에서 뺀다
	"late" BOOLEAN NOT NULL,
	-- 기준선: 늘 UP / 기준일 하루 움직임 방향 그대로
	"alwaysUpHit" BOOLEAN NOT NULL,
	"prevDirectionHit" BOOLEAN,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stockPredictionGrade_predictionId ON "StockPredictionGrade" ("prediction_id");
-- 되돌리기
-- DROP TABLE IF EXISTS "StockPredictionGrade", "StockPrediction", "StockNews";

-- =====================================================================
-- 2026-11-10
-- =====================================================================

-- (1) 시황 분석 회차. 같은 종목 · 기준일 · 기간이라도 분석할 때마다 따로 쌓는다(회차끼리 적중률을 비교해 개선을 판단).
--     기존 예측은 제출 단위(모델 · 프롬프트 버전 · 분)로 회차를 채운다.
ALTER TABLE "StockPrediction" ADD COLUMN IF NOT EXISTS "runId" UUID;
UPDATE "StockPrediction" SET "runId" = md5("modelId" || '|' || "promptVersion" || '|' || date_trunc('minute', "createdDate")::text)::uuid WHERE "runId" IS NULL;
DROP INDEX IF EXISTS uk_stockPrediction_stockItemId_baseDate_horizonDays;
CREATE INDEX IF NOT EXISTS idx_stockPrediction_stockItemId_baseDate_horizonDays ON "StockPrediction" ("stockItem_id", "baseDate", "horizonDays");
CREATE INDEX IF NOT EXISTS idx_stockPrediction_runId ON "StockPrediction" ("runId");
-- 되돌리기(같은 종목 · 기준일 · 기간에 회차가 둘 이상이면 먼저 정리해야 유일 인덱스가 만들어진다)
-- DROP INDEX IF EXISTS idx_stockPrediction_runId, idx_stockPrediction_stockItemId_baseDate_horizonDays;
-- CREATE UNIQUE INDEX uk_stockPrediction_stockItemId_baseDate_horizonDays ON "StockPrediction" ("stockItem_id", "baseDate", "horizonDays");
-- ALTER TABLE "StockPrediction" DROP COLUMN "runId";
