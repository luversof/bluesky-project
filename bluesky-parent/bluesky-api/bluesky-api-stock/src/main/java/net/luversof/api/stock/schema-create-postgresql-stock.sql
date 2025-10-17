CREATE TABLE "Account" (
	"id" UUID NOT NULL PRIMARY KEY,
	"user_id" UUID NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"createdDate" TIMESTAMP WITH TIME ZONE NOT NULL,
	"jsonConfig" JSONB
);

CREATE INDEX idx_account_userId ON "Account" ("user_id");

CREATE TABLE "StockItem" (
	"id" UUID NOT NULL PRIMARY KEY,
	"ticker" VARCHAR(20) NOT NULL,
	"name" VARCHAR(100) NOT NULL,
	"market" VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX uk_stockItem_ticker ON "StockItem" ("ticker");

CREATE TABLE "Trade" (
	"id" UUID NOT NULL PRIMARY KEY,
	"account_id" UUID NOT NULL,
	"stockItem_id" UUID NOT NULL,
	"type" VARCHAR(10) NOT NULL,
	"quantity" INTEGER NOT NULL,
	"price" NUMERIC NOT NULL,
	"fee" NUMERIC NOT NULL,
	"tax" NUMERIC NOT NULL,
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
	"price" NUMERIC NOT NULL,
	"fee" NUMERIC NOT NULL,
	"tax" NUMERIC NOT NULL,
	"tradeDate" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_dividend_accountId ON "Dividend" ("account_id");
CREATE INDEX idx_dividend_accountId_stockItemId ON "Dividend" ("account_id", "stockItem_id");