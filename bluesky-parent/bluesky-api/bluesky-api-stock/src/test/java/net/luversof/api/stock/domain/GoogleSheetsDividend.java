package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.luversof.api.stock.databind.GoogleSheetsCurrencyDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleSheetsDividend {
	
	private static final Logger log = LoggerFactory.getLogger(GoogleSheetsDividend.class);
	
	private static final ZoneOffset KST = ZoneOffset.ofHours(9);
	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
			DateTimeFormatter.ofPattern("yyyy. M. d"),
			DateTimeFormatter.ofPattern("yyyy-M-d"),
			DateTimeFormatter.ISO_LOCAL_DATE);

	@JsonProperty("지급일")
	private String 지급일;

	@JsonProperty("종목")
	private String 종목;

	@JsonProperty("계좌")
	private String 계좌;

	@JsonProperty("주식 수")
	private int 주식수;

	@JsonProperty("분배금액")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 분배금액;

	@JsonProperty("주당과세표준액")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 주당과세표준액;

	@JsonProperty("배당금")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 배당금;

	@JsonProperty("세금")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 세금;

	@JsonProperty("실지급")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 실지급;

	@JsonProperty("과세금액")
	@JsonDeserialize(using = GoogleSheetsCurrencyDeserializer.class)
	private BigDecimal 과세금액;

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

	public int get주식수() {
		return 주식수;
	}

	public void set주식수(int 주식수) {
		this.주식수 = 주식수;
	}

	public BigDecimal get분배금액() {
		return 분배금액;
	}

	public void set분배금액(BigDecimal 분배금액) {
		this.분배금액 = 분배금액;
	}

	public BigDecimal get주당과세표준액() {
		return 주당과세표준액;
	}

	public void set주당과세표준액(BigDecimal 주당과세표준액) {
		this.주당과세표준액 = 주당과세표준액;
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

	public BigDecimal get과세금액() {
		return 과세금액;
	}

	public void set과세금액(BigDecimal 과세금액) {
		this.과세금액 = 과세금액;
	}

	@Override
	public String toString() {
		return "GoogleSheetsDividendItem [지급일=" + 지급일 + ", 종목=" + 종목 + ", 계좌=" + 계좌 + ", 주식수=" + 주식수 + ", 분배금액=" + 분배금액
				+ ", 주당과세표준액=" + 주당과세표준액 + ", 배당금=" + 배당금 + ", 세금=" + 세금
				+ ", 실지급=" + 실지급 + ", 과세금액=" + 과세금액 + "]";
	}
	
	
	public Dividend toDividend(Map<String, UUID> accountMap, Map<String, UUID> stockItemMap) {
		var accountName = get계좌();
		var stockName = get종목();

		if (!StringUtils.hasText(accountName) || !StringUtils.hasText(stockName)) {
			return null;
		}

		var accountId = accountMap.get(accountName.trim());
		var stockItemId = stockItemMap.get(stockName.trim());
		var payDate = parsePayDate(get지급일());

		if (accountId == null || stockItemId == null || payDate == null) {
			log.warn("Skip dividend row. accountId: {}, stockItemId: {}, payDate: {}", accountId, stockItemId, payDate);
			return null;
		}

		var dividend = new Dividend();
		dividend.setAccountId(accountId);
		dividend.setStockItemId(stockItemId);
		dividend.setType("DIVIDEND");
		dividend.setQuantity(0);
		dividend.setAmountPerShare(배당금);
		dividend.setTaxPerShare(주당과세표준액);
		dividend.setGrossAmount(get배당금() == null ? BigDecimal.ZERO : get배당금());
		dividend.setTax(get세금() == null ? BigDecimal.ZERO : get세금());
		dividend.setFee(BigDecimal.ZERO);
		dividend.setRecordDate(payDate);
		dividend.setPayDate(payDate);
		return dividend;
	}
	
	private Instant parsePayDate(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		var trimmed = value.trim();
		for (var formatter : DATE_FORMATTERS) {
			try {
				return LocalDate.parse(trimmed, formatter).atStartOfDay().toInstant(KST);
			} catch (DateTimeParseException ignored) {
				// try next pattern
			}
		}
		log.warn("Unable to parse dividend pay date: {}", value);
		return null;
	}

}
