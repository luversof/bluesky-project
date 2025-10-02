package net.luversof.api.stock.constant;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TradeType {

	@JsonProperty("매수")
	BUY,
	
	@JsonProperty("매도")
	SELL
	;
}
