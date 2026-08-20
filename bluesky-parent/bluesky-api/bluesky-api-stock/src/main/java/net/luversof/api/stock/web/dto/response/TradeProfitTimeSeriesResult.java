package net.luversof.api.stock.web.dto.response;

import java.util.List;

/** 시계열 + 기간 요약 + 연도별 성과. 한 번의 시뮬레이션 결과를 함께 돌려주기 위한 응답. */
public record TradeProfitTimeSeriesResult(
    List<TradeProfitTimeSeriesPoint> series,
    TradeProfitTimeSeriesSummary summary,
    List<TradeProfitYearlySummary> yearly) {}
