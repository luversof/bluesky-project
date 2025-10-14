package net.luversof.api.stock.web.dto.request;

/**
 * TradeProfitRequest의 조회 조건 그룹
 * STOCKITEM : 종목 기준
 * ACCOUNT_AND_STOCKITEM : 계좌 + 종목 기준
 * 
 * @author luversof
 *
 */
public enum TradeProfitRequestGroup {

	STOCKITEM,
	ACCOUNT_AND_STOCKITEM,
	;
	
}
