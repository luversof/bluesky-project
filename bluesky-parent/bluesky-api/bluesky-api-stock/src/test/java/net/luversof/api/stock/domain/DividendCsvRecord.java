package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.luversof.api.stock.databind.CurrencyDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DividendCsvRecord {

	@JsonProperty("지급일")
	private String 지급일;

	@JsonProperty("종목")
	private String 종목;

	@JsonProperty("계좌")
	private String 계좌;

	@JsonProperty("배당금")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 배당금;

	@JsonProperty("세금")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 세금;

	@JsonProperty("실지급")
	@JsonDeserialize(using = CurrencyDeserializer.class)
	private BigDecimal 실지급;

	public String get지급일() {
		return 지급일;
	}

	public void set지급일(String 지급일) {
		this.지급일 = 지급일;
	}

	public String get종목() {
		return 종목;
	}

	public void set종목(String 종목) {
		this.종목 = 종목;
	}

	public String get계좌() {
		return 계좌;
	}

	public void set계좌(String 계좌) {
		this.계좌 = 계좌;
	}

	public BigDecimal get배당금() {
		return 배당금;
	}

	public void set배당금(BigDecimal 배당금) {
		this.배당금 = 배당금;
	}

	public BigDecimal get세금() {
		return 세금;
	}

	public void set세금(BigDecimal 세금) {
		this.세금 = 세금;
	}

	public BigDecimal get실지급() {
		return 실지급;
	}

	public void set실지급(BigDecimal 실지급) {
		this.실지급 = 실지급;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DividendCsvRecord other = (DividendCsvRecord) obj;
		return Objects.equals(계좌, other.계좌) && Objects.equals(배당금, other.배당금) && Objects.equals(세금, other.세금)
				&& Objects.equals(실지급, other.실지급) && Objects.equals(종목, other.종목) && Objects.equals(지급일, other.지급일);
	}

	@Override
	public int hashCode() {
		return Objects.hash(계좌, 배당금, 세금, 실지급, 종목, 지급일);
	}

	@Override
	public String toString() {
		return "DividendCsvRecord [지급일=" + 지급일 + ", 종목=" + 종목 + ", 계좌=" + 계좌 + ", 배당금=" + 배당금 + ", 세금=" + 세금 + ", 실지급="
				+ 실지급 + "]";
	}
}
