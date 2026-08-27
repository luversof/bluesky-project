package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 시계열 기간 요약(api-stock 이 다운샘플 이전 일별 시리즈로 계산해 내려준다). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeProfitTimeSeriesSummary(
    BigDecimal openingValue,
    BigDecimal closingValue,
    Double growthRatePct,
    Double timeWeightedReturnPct,
    BigDecimal periodProfit,
    BigDecimal principalDelta,
    BigDecimal unrealizedStart,
    BigDecimal unrealizedEnd,
    Double unrealizedEndPct,
    BigDecimal recoveredAmount,
    BigDecimal netNewProfit,
    Double maxDrawdownPct,
    LocalDate maxDrawdownPeakDate,
    LocalDate maxDrawdownTroughDate,
    Double currentDrawdownPct,
    /** 기간 손익률 - 넣어 둔 돈(기초 평가액 + 기간 중 순유입) 대비. 증가율·투자 수익률과 분모가 다르다. */
    Double periodProfitRatePct,
    /** 기간 중 평가액 고점/저점(평가액 0 인 날은 제외). 차트의 최고/최저 주석과 같은 규칙이다. */
    BigDecimal peakValue,
    LocalDate peakValueDate,
    BigDecimal troughValue,
    LocalDate troughValueDate) {}
