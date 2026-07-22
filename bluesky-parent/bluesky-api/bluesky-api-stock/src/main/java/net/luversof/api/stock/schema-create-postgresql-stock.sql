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
	"taxableBasePerShare" NUMERIC NOT NULL,
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
	"averageTaxableBaseRatio1y" NUMERIC NOT NULL,
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
	"volume" BIGINT,
	"updatedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_stockPriceHistory_stockItemId_tradeDate ON "StockPriceHistory" ("stockItem_id", "tradeDate");

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
CREATE TABLE "DailyAccountSnapshot" (
        "id" UUID NOT NULL PRIMARY KEY,
        "user_id" UUID NOT NULL,
        "account_id" UUID,
        "date" DATE NOT NULL,
        "totalCost" DECIMAL(19, 4),
        "totalValue" DECIMAL(19, 4),
        "cumulativeRealizedProfit" DECIMAL(19, 4),
        "cumulativeDividend" DECIMAL(19, 4),
        "createdDate" TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        "wmaState" JSONB
);

CREATE INDEX idx_snapshot_userId ON "DailyAccountSnapshot" ("user_id");
CREATE INDEX idx_snapshot_date ON "DailyAccountSnapshot" ("date");
-- 계좌별 직전 스냅샷 조회(account_id = ? AND date < ? ORDER BY date DESC LIMIT 1) 용.
-- account_id 단독 인덱스가 없어 이 조회들이 전체 스캔이었다.
CREATE INDEX idx_snapshot_accountId_date ON "DailyAccountSnapshot" ("account_id", "date");
-- 사용자 전체(account_id IS NULL) 스냅샷의 기간/직전 조회용
CREATE INDEX idx_snapshot_userId_date ON "DailyAccountSnapshot" ("user_id", "date");

