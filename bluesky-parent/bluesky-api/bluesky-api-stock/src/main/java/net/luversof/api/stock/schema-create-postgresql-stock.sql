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
CREATE INDEX idx_trade_stockItemId ON "Trade" ("stockItem_id");

CREATE TABLE "Devidend" (
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

CREATE INDEX idx_devidend_accountId ON "Devidend" ("account_id");
CREATE INDEX idx_devidend_stockItemId ON "Devidend" ("stockItem_id");