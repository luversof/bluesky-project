package net.luversof.api.stock;

/**
 * 상장폐지된 종목 코드 모음
 */
public enum DelistedStocks {

	쌍방울("KRX", "102280")
	;
	
	private String market;
	private String stockCode;
	
	DelistedStocks(String market, String stockCode) {
		this.market = market;
		this.stockCode = stockCode;
	}
	
	public String getMarket() {
		return market;
	}
	
	public String getStockCode() {
		return stockCode;
	}

}
