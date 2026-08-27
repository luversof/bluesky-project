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
    Double currentDrawdownPct,
    /**
     * 기간 손익률 &mdash; {@code periodProfit / (기초 평가액 + 기간 중 순유입 원금)}.
     *
     * <p>{@link #growthRatePct} · {@link #timeWeightedReturnPct} 와 <b>분모가 다른 별개의 값</b>이다. 셋을 같은 것으로
     * 보면 화면이 서로 다른 수를 말하는 것처럼 읽힌다. 실측 2026-08-27(올해): 자산 증가율 76.66% / 투자 수익률 92.88% / 기간 손익률
     * 94.93%.
     *
     * <p>기초 평가액이 0 이어서 증가율을 낼 수 없는 구간('전체' 기간)에서도 이 값은 나온다 &mdash; 분모에 유입 원금이 들어가기 때문이다.
     */
    Double periodProfitRatePct,
    /**
     * 기간 중 평가액 최고점과 그 날짜. 평가액이 <b>0 인 날은 세지 않는다</b>(보유가 없던 날이라 기준점이 못 된다).
     *
     * <p>차트가 그리는 최고/최저 주석과 같은 규칙이어야 한다 &mdash; 같은 화면에서 두 값이 다르면 어느 쪽이 맞는지 알 수 없다.
     */
    BigDecimal peakValue,
    LocalDate peakValueDate,
    BigDecimal troughValue,
    LocalDate troughValueDate) {

  public static TradeProfitTimeSeriesSummary empty() {
    return new TradeProfitTimeSeriesSummary(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null);
  }
}
