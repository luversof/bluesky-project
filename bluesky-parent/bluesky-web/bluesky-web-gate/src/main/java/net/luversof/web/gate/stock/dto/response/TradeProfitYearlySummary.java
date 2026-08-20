package net.luversof.web.gate.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 연도별 성과(api-stock 이 연도 구간 일별 시리즈로 계산해 내려준다). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeProfitYearlySummary(int year, TradeProfitTimeSeriesSummary summary) {}
