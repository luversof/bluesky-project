package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 차트에 쓰는 일별 종가 한 점. 종목 id 는 호출자가 아는 값이라 행마다 싣지 않는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockPriceHistoryPoint(LocalDate tradeDate, BigDecimal closePrice) {}
