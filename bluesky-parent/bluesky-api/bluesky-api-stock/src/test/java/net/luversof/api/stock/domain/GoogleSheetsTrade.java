package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.databind.GoogleSheetsCurrencyDeserializer;
import net.luversof.api.stock.databind.GoogleSheetsDateDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleSheetsTrade {

	private static final Logger log = LoggerFactory.getLogger(GoogleSheetsTrade.class);

	@JsonProperty("날짜")
	@JsonDeserialize(using = GoogleSheetsDateDeserializer.class)
	private Instant 날짜;

	@JsonProperty("종목")
	private String 종목;

	@JsonProperty("구분")
	private String 구분;

	@JsonProperty("계좌")
	private String 계좌;

	@JsonProperty("매매가")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 매매가;

	@JsonProperty("매매 수량")
	private Integer 매매_수량;

	@JsonProperty("수수료")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 수수료;

	@JsonProperty("거래세")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 거래세;

	@JsonProperty("매수 수량")
	private Integer 매수_수량;

	@JsonProperty("매수 금액")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 매수_금액;

	@JsonProperty("매도 수량")
	private Integer 매도_수량;

	@JsonProperty("매도 금액")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 매도_금액;

	@JsonProperty("현재가")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 현재가;

	public Instant get날짜() {
		return 날짜;
	}

	public void set날짜(Instant 날짜) {
		this.날짜 = 날짜;
	}

	public String get종목() {
		return 종목;
	}

	public void set종목(String 종목) {
		this.종목 = 종목;
	}

	public String get구분() {
		return 구분;
	}

	public void set구분(String 구분) {
		this.구분 = 구분;
	}

	public String get계좌() {
		return 계좌;
	}

	public void set계좌(String 계좌) {
		this.계좌 = 계좌;
	}

	public BigDecimal get매매가() {
		return 매매가;
	}

	public void set매매가(BigDecimal 매매가) {
		this.매매가 = 매매가;
	}

	public Integer get매매_수량() {
		return 매매_수량;
	}

	public void set매매_수량(Integer 매매_수량) {
		this.매매_수량 = 매매_수량;
	}

	public BigDecimal get수수료() {
		return 수수료;
	}

	public void set수수료(BigDecimal 수수료) {
		this.수수료 = 수수료;
	}

	public BigDecimal get거래세() {
		return 거래세;
	}

	public void set거래세(BigDecimal 거래세) {
		this.거래세 = 거래세;
	}

	public Integer get매수_수량() {
		return 매수_수량;
	}

	public void set매수_수량(Integer 매수_수량) {
		this.매수_수량 = 매수_수량;
	}

	public BigDecimal get매수_금액() {
		return 매수_금액;
	}

	public void set매수_금액(BigDecimal 매수_금액) {
		this.매수_금액 = 매수_금액;
	}

	public Integer get매도_수량() {
		return 매도_수량;
	}

	public void set매도_수량(Integer 매도_수량) {
		this.매도_수량 = 매도_수량;
	}

	public BigDecimal get매도_금액() {
		return 매도_금액;
	}

	public void set매도_금액(BigDecimal 매도_금액) {
		this.매도_금액 = 매도_금액;
	}

	public BigDecimal get현재가() {
		return 현재가;
	}

	public void set현재가(BigDecimal 현재가) {
		this.현재가 = 현재가;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GoogleSheetsTrade other = (GoogleSheetsTrade) obj;
		return Objects.equals(거래세, other.거래세) && Objects.equals(계좌, other.계좌) && Objects.equals(구분, other.구분)
				&& Objects.equals(날짜, other.날짜) && Objects.equals(매도_금액, other.매도_금액)
				&& Objects.equals(매도_수량, other.매도_수량) && Objects.equals(매매_수량, other.매매_수량)
				&& Objects.equals(매매가, other.매매가) && Objects.equals(매수_금액, other.매수_금액)
				&& Objects.equals(매수_수량, other.매수_수량) && Objects.equals(수수료, other.수수료) && Objects.equals(종목, other.종목)
				&& Objects.equals(현재가, other.현재가);
	}

	@Override
	public int hashCode() {
		return Objects.hash(거래세, 계좌, 구분, 날짜, 매도_금액, 매도_수량, 매매_수량, 매매가, 매수_금액, 매수_수량, 수수료, 종목, 현재가);
	}

	@Override
	public String toString() {
		return "TradeCsvRecord [날짜=" + 날짜 + ", 종목=" + 종목 + ", 구분=" + 구분 + ", 계좌=" + 계좌 + ", 매매가=" + 매매가 + ", 매매_수량="
				+ 매매_수량 + ", 수수료=" + 수수료 + ", 거래세=" + 거래세 + ", 매수_수량=" + 매수_수량 + ", 매수_금액=" + 매수_금액 + ", 매도_수량=" + 매도_수량
				+ ", 매도_금액=" + 매도_금액 + ", 현재가=" + 현재가 + "]";
	}

	public Trade toTrade(HashMap<String, UUID> accountMap, List<StockItem> stockItemList) {
		Trade trade = new Trade();
		trade.setType(구분.equals("매수") ? TradeType.BUY : TradeType.SELL);
		trade.setQuantity(매매_수량);
		trade.setPrice(매매가);
		trade.setFee(수수료 == null ? BigDecimal.ZERO : 수수료);
		trade.setTax(거래세 == null ? BigDecimal.ZERO : 거래세);

		trade.setTradeDate(날짜);

		// 계좌 이름으로 accountId 설정
		String accountName = get계좌();
		if (accountName != null && !accountName.isBlank()) {
			UUID accountId = accountMap.get(accountName);
			if (accountId != null) {
				trade.setAccountId(accountId);
			} else {
				log.warn("Account not found for name: {}", accountName);
			}
		}

		var stockItem = stockItemList.stream()
				.filter(s -> s.getName().equals(get종목()))
				.findFirst()
				.orElseGet(() -> null);

		if (stockItem == null) {
			log.debug("stockItem not found : {}", get종목());
			return null; // stockItem이 없으면 trade를 생성하지 않음
		}

		trade.setStockItemId(stockItem.getId());

		return trade;
	}
}