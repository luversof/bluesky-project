package net.luversof.web.gate.stock.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 시계열 + 기간 요약 + 연도별 성과. 시뮬레이션 1회 결과를 함께 받기 위한 응답. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeProfitTimeSeriesResult(
    List<TradeProfitTimeSeriesPoint> series,
    TradeProfitTimeSeriesSummary summary,
    List<TradeProfitYearlySummary> yearly) {}
