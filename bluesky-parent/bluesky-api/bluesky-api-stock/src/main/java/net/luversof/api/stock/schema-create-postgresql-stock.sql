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
	"recordDate" TIMESTAMP WITH TIME ZONE,
	"payDate" TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_dividend_accountId ON "Dividend" ("account_id");
CREATE INDEX idx_dividend_accountId_stockItemId ON "Dividend" ("account_id", "stockItem_id");

CREATE TABLE "StockPrice" (
	"id" UUID NOT NULL PRIMARY KEY,
	"stockItem_id" UUID NOT NULL,
	"price" NUMERIC NOT NULL,
	"updatedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_stockPrice_stockItemId ON "StockPrice" ("stockItem_id");

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
	"provider" VARCHAR(50) NOT NULL,
	"appKey" VARCHAR(255) NOT NULL,
	"appSecret" VARCHAR(255) NOT NULL,
	"accessToken" VARCHAR(2000),
	"tokenUpdatedDate" TIMESTAMP WITH TIME ZONE,
	"updatedDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_openApiConfig_provider ON "OpenApiConfig" ("provider");