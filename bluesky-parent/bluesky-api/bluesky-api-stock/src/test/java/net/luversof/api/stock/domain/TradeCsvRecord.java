package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.databind.CurrencyDeserializer;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeCsvRecord {
	
	@JsonProperty("날짜")
	private String 날짜;

	@JsonProperty("종목")
	private String 종목;

	@JsonProperty("구분")
	private String 구분;

	@JsonProperty("계좌")
	private String 계좌;

	@JsonProperty("매매가")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 매매가;

	@JsonProperty("매매 수량")
	private Integer 매매_수량;

	@JsonProperty("수수료")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 수수료;

	@JsonProperty("거래세")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 거래세;
	
	@JsonProperty("매수 수량")
	private Integer 매수_수량;

	@JsonProperty("매수 금액")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 매수_금액;
	
	@JsonProperty("매도 수량")
	private Integer 매도_수량;
	
	@JsonProperty("매도 금액")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 매도_금액;
	
	@JsonProperty("현재가")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 현재가;
	
	public Trade toTrade() {
		Trade trade = new Trade();
		trade.setType(구분.equals("매수") ? TradeType.BUY : TradeType.SELL);
		trade.setQuantity(매매_수량);
		trade.setPrice(매매가);
		trade.setFee(수수료 == null ? BigDecimal.ZERO : 수수료);
		trade.setTax(거래세 == null ? BigDecimal.ZERO : 거래세);
		
		var formatter = DateTimeFormatter.ofPattern("yyyy. M. d");
		var localDate = LocalDate.parse(날짜, formatter);
		var offsetDateTime = localDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9));
		
		trade.setTradeDate(offsetDateTime);
		return trade;
	}
}