package net.luversof.web.gate.stock.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 시계열 + 기간 요약 + 연도별 성과 + 기간 쪼갬. 시뮬레이션 1회 결과를 함께 받기 위한 응답. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeProfitTimeSeriesResult(
    List<TradeProfitTimeSeriesPoint> series,
    TradeProfitTimeSeriesSummary summary,
    List<TradeProfitYearlySummary> yearly,
    /** {@code breakdown} 파라미터를 준 조회에만 채워진다. 안 주면 빈 목록. */
    List<TradeProfitPeriodSummary> breakdown) {}
