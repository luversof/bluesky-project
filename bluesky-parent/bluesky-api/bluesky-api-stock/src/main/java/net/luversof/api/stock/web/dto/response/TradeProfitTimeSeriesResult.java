package net.luversof.api.stock.web.dto.response;

import java.util.List;

/** 시계열 + 기간 요약 + 연도별 성과 + 기간 쪼갬. 한 번의 시뮬레이션 결과를 함께 돌려주기 위한 응답. */
public record TradeProfitTimeSeriesResult(
    List<TradeProfitTimeSeriesPoint> series,
    TradeProfitTimeSeriesSummary summary,
    List<TradeProfitYearlySummary> yearly,
    /**
     * 요청한 기간을 달/해로 쪼갠 성과. {@code breakdown} 파라미터를 주지 않으면 빈 목록이다.
     *
     * <p>{@link #yearly} 와 같은 계산이지만 단위가 요청에 따라 달라진다. 기존 화면(자산 성장)은 그대로 {@code yearly} 를 쓰므로 그쪽 응답
     * 크기는 변하지 않는다.
     */
    List<TradeProfitPeriodSummary> breakdown) {}
