CREATE TABLE "Account" (
	"id" UUID NOT NULL PRIMARY KEY,
	"user_id" UUID NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"base_currency" VARCHAR(3),
	"jsonConfig" JSONB
);

CREATE INDEX idx_account_userId ON "Account" ("user_id");

CREATE TABLE "StockItem" (
	"id" UUID NOT NULL PRIMARY KEY,
	"market" VARCHAR(10) NOT NULL,
	"symbol" VARCHAR(20) NOT NULL,
	"name" VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uk_stockItem_symbol ON "StockItem" ("symbol");

CREATE TABLE "StockItemTag" (
	"id" UUID NOT NULL PRIMARY KEY,
	"stockItem_id" UUID NOT NULL,
	"tag" VARCHAR(255) NOT NULL
);

CREATE INDEX idx_stockItemTag_stockItemId ON "StockItemTag" ("stockItem_id");
CREATE UNIQUE INDEX uk_stockItemTag_stockItemId_tag ON "StockItemTag" ("stockItem_id", "tag");

CREATE TABLE "MonthlyDividendProfile" (
	"id" UUID NOT NULL PRIMARY KEY,
	"stockItem_id" UUID NOT NULL,
	"sourceUrl" VARCHAR(2000),
	"payoutWindow" VARCHAR(20) NOT NULL,
	"displayOrder" INTEGER NOT NULL DEFAULT 0,
	"active" BOOLEAN NOT NULL,
	"note" VARCHAR(1000),
	"lastVerifiedDate" DATE,
	-- 총보수(연, %) · 상장일 - 운용사 상세 화면에서 채운다. 모르면 NULL(2026-09-28 추가)
	"totalExpenseRatioPct" NUMERIC(6,4),
	"listingDate" DATE,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"updatedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_monthlyDividendProfile_stockItemId ON "MonthlyDividendProfile" ("stockItem_id");
CREATE INDEX idx_monthlyDividendProfile_displayOrder ON "MonthlyDividendProfile" ("displayOrder");
CREATE INDEX idx_monthlyDividendProfile_payoutWindow ON "MonthlyDividendProfile" ("payoutWindow");
CREATE INDEX idx_monthlyDividendProfile_active ON "MonthlyDividendProfile" ("active");

CREATE TABLE "MonthlyDividendPayout" (
	"id" UUID NOT NULL PRIMARY KEY,
	"stockItem_id" UUID NOT NULL,
	"recordDate" DATE NOT NULL,
	"payDate" DATE NOT NULL,
	"distributionRatePct" NUMERIC,
	"dividendAmountPerShare" NUMERIC NOT NULL,
	"taxableBasePerShare" NUMERIC,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"updatedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_monthlyDividendPayout_stockItemId ON "MonthlyDividendPayout" ("stockItem_id");
CREATE INDEX idx_monthlyDividendPayout_payDate ON "MonthlyDividendPayout" ("payDate");
CREATE UNIQUE INDEX uk_monthlyDividendPayout_stockItemId_recordDate_payDate ON "MonthlyDividendPayout" ("stockItem_id", "recordDate", "payDate");

CREATE TABLE "Trade" (
	"id" UUID NOT NULL PRIMARY KEY,
	"account_id" UUID NOT NULL,
	"stockItem_id" UUID NOT NULL,
	"type" VARCHAR(10) NOT NULL,
	"quantity" INTEGER NOT NULL,
	"price" NUMERIC NOT NULL,
	"fee" NUMERIC NOT NULL,
	"tax" NUMERIC NOT NULL,
	"realizedProfit" NUMERIC,
	"exchangeRate" NUMERIC,
	"tradeDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_trade_accountId ON "Trade" ("account_id");
CREATE INDEX idx_trade_accountId_stockItemId ON "Trade" ("account_id", "stockItem_id");
-- 기간 조회(account_id IN (...) AND tradeDate BETWEEN ...)와 최초 거래일 집계(MIN(tradeDate))용
CREATE INDEX idx_trade_accountId_tradeDate ON "Trade" ("account_id", "tradeDate");

CREATE TABLE "Dividend" (
	"id" UUID NOT NULL PRIMARY KEY,
	"account_id" UUID NOT NULL,
	"stockItem_id" UUID NOT NULL,
	"type" VARCHAR(10) NOT NULL,
	"quantity" INTEGER NOT NULL,
	"amountPerShare" NUMERIC NOT NULL,
	"taxPerShare" NUMERIC NOT NULL,
	"grossAmount" NUMERIC NOT NULL,
	"fee" NUMERIC NOT NULL,
	"tax" NUMERIC NOT NULL,
	"taxableAmount" NUMERIC,
	"recordDate" TIMESTAMP WITH TIME ZONE,
	"payDate" TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_dividend_accountId ON "Dividend" ("account_id");
CREATE INDEX idx_dividend_accountId_stockItemId ON "Dividend" ("account_id", "stockItem_id");
-- payDate 정렬/범위 조회와 최초 배당일 집계(MIN(payDate))용
CREATE INDEX idx_dividend_accountId_payDate ON "Dividend" ("account_id", "payDate");

CREATE TABLE "MonthlyDividendSnapshot" (
	"id" UUID NOT NULL PRIMARY KEY,
	"user_id" UUID NOT NULL,
	"stockItem_id" UUID NOT NULL,
	"asOfDate" DATE NOT NULL,
	"latestMonthlyDividendPerShare" NUMERIC NOT NULL,
	"averageMonthlyDividendPerShare1y" NUMERIC NOT NULL,
	"averageTaxableBaseRatio1y" NUMERIC,
	"heldQuantity" INTEGER NOT NULL,
	"averageBuyPrice" NUMERIC NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"updatedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_monthlyDividendSnapshot_userId ON "MonthlyDividendSnapshot" ("user_id");
CREATE INDEX idx_monthlyDividendSnapshot_stockItemId ON "MonthlyDividendSnapshot" ("stockItem_id");
CREATE UNIQUE INDEX uk_monthlyDividendSnapshot_userId_stockItemId ON "MonthlyDividendSnapshot" ("user_id", "stockItem_id");

CREATE TABLE "StockPriceHistory" (
	"id" UUID NOT NULL PRIMARY KEY,
	"stockItem_id" UUID NOT NULL,
	"tradeDate" DATE NOT NULL,
	"openPrice" NUMERIC,
	"highPrice" NUMERIC,
	"lowPrice" NUMERIC,
	"closePrice" NUMERIC,
	-- 원주가(수정 전 종가). 매매가 있던 날만 채운다 - 평가 수량의 분할 · 병합 계수 = 원주가 / 수정 종가(2026-10-02).
	"rawClosePrice" NUMERIC,
	"volume" BIGINT,
	"updatedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_stockPriceHistory_stockItemId_tradeDate ON "StockPriceHistory" ("stockItem_id", "tradeDate");
-- 종목별 일자 범위의 종가 조회(데이터 상태 · 카탈로그 · 시계열)를 테이블을 읽지 않고 인덱스만으로 끝내기 위한 커버링 인덱스(2026-09-28 추가)
CREATE INDEX idx_stockPriceHistory_item_date_close ON "StockPriceHistory" ("stockItem_id", "tradeDate") INCLUDE ("closePrice");

CREATE TABLE "OpenApiConfig" (
	"id" UUID NOT NULL PRIMARY KEY,
	"user_id" UUID NOT NULL,
	"provider" VARCHAR(50) NOT NULL,
	"appKey" VARCHAR(255) NOT NULL,
	"appSecret" VARCHAR(255) NOT NULL,
	"accessToken" VARCHAR(2000),
	"tokenUpdatedDate" TIMESTAMP WITH TIME ZONE,
	"updatedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_openApiConfig_provider_userId ON "OpenApiConfig" ("provider", "user_id");

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
	-- 분석 회차: 한 번의 제출로 저장한 예측 묶음(하루 몇 번이든 쌓인다). NULL 허용 - 회차 기능 전 앱이 배포 전까지 넣는 행
	"runId" UUID,
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

CREATE INDEX IF NOT EXISTS idx_stockPrediction_stockItemId_baseDate_horizonDays ON "StockPrediction" ("stockItem_id", "baseDate", "horizonDays");
CREATE INDEX IF NOT EXISTS idx_stockPrediction_runId ON "StockPrediction" ("runId");

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
