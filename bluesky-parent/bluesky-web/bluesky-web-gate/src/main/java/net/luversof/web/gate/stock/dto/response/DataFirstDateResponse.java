package net.luversof.web.gate.stock.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 사용자의 최초 데이터 일자. 거래/배당이 없으면 각 필드는 null. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataFirstDateResponse(Instant tradeFirstDate, Instant dividendFirstDate) {}
