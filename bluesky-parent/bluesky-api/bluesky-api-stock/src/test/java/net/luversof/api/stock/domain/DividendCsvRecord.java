package net.luversof.api.stock.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;
import net.luversof.api.stock.databind.CurrencyDeserializer;

@Data
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
}
