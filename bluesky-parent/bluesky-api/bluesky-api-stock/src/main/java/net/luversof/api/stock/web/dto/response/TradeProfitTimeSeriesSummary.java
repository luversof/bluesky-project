package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 시계열 기간 요약. 다운샘플 이전의 일별 시리즈로 계산한다(주/월봉으로 계산하면 TWR/MDD 가 틀린다). */
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
    /** 최대 낙폭(음수 %). 입출금을 제거한 기준가 기준이라 입금으로 희석되지 않는다. */
    Double maxDrawdownPct,
    LocalDate maxDrawdownPeakDate,
    LocalDate maxDrawdownTroughDate,
    /** 기말 시점의 전고점 대비 낙폭(음수 %). 0 이면 신고점. */
    Double currentDrawdownPct) {

  public static TradeProfitTimeSeriesSummary empty() {
    return new TradeProfitTimeSeriesSummary(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }
}
