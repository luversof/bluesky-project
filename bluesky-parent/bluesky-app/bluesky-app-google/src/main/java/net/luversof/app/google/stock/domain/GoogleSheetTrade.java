package net.luversof.app.google.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.luversof.app.google.stock.databind.StockCurrencyDeserializer;
import net.luversof.app.google.stock.databind.StockDateDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleSheetTrade {

	@JsonProperty("날짜")
	@JsonDeserialize(using = StockDateDeserializer.class)
	private Instant 날짜;

	@JsonProperty("종목")
	private String 종목;

	@JsonProperty("구분")
	private String 구분;

	@JsonProperty("계좌")
	private String 계좌;

	@JsonProperty("매매가")
	@JsonDeserialize(using = StockCurrencyDeserializer.class)
	private BigDecimal 매매가;

	@JsonProperty("매매 수량")
	private Integer 매매_수량;

	@JsonProperty("수수료")
	@JsonDeserialize(using = StockCurrencyDeserializer.class)
	private BigDecimal 수수료;

	@JsonProperty("거래세")
	@JsonDeserialize(using = StockCurrencyDeserializer.class)
	private BigDecimal 거래세;

	@JsonProperty("매수 수량")
	private Integer 매수_수량;

	@JsonProperty("매수 금액")
	@JsonDeserialize(using = StockCurrencyDeserializer.class)
	private BigDecimal 매수_금액;

	@JsonProperty("매도 수량")
	private Integer 매도_수량;

	@JsonProperty("매도 금액")
	@JsonDeserialize(using = StockCurrencyDeserializer.class)
	private BigDecimal 매도_금액;

	@JsonProperty("현재가")
	@JsonDeserialize(using = StockCurrencyDeserializer.class)
	private BigDecimal 현재가;
	
	@JsonProperty("매도 실현 손익")
	@JsonDeserialize(using = StockCurrencyDeserializer.class)
	private BigDecimal 매도실현손익;

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

	public BigDecimal get매도실현손익() {
		return 매도실현손익;
	}

	public void set매도실현손익(BigDecimal 매도실현손익) {
		this.매도실현손익 = 매도실현손익;
	}

	@Override
	public String toString() {
		return "GoogleSheetTrade [날짜=" + 날짜 + ", 종목=" + 종목 + ", 구분=" + 구분 + ", 계좌=" + 계좌 + ", 매매가=" + 매매가 + ", 매매_수량="
				+ 매매_수량 + ", 수수료=" + 수수료 + ", 거래세=" + 거래세 + ", 매수_수량=" + 매수_수량 + ", 매수_금액=" + 매수_금액 + ", 매도_수량=" + 매도_수량
				+ ", 매도_금액=" + 매도_금액 + ", 현재가=" + 현재가 + ", 매도실현손익=" + 매도실현손익 + "]";
	}

}