package net.luversof.api.stock.service.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/** 시황 분석의 지표 계산과 채점 규칙. 상태 없는 순수 함수만 둔다. */
public final class PredictionMath {

  public static final ZoneId MARKET_ZONE_ID = ZoneId.of("Asia/Seoul");

  /** 장 시작 시각. 이 시각 이후에 만든 예측은 장중 정보가 섞인다. */
  public static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);

  public enum Direction {
    UP,
    DOWN,
    FLAT
  }

  private PredictionMath() {}

  /** 보합으로 보는 변화율 폭(%, 절댓값). 1 거래일 ±0.5, 5 거래일 ±1.5(사용자 결정 2026-11-09). */
  public static BigDecimal flatBandPct(int horizonDays) {
    return switch (horizonDays) {
      case 1 -> new BigDecimal("0.5");
      case 5 -> new BigDecimal("1.5");
      default -> throw new IllegalArgumentException("horizonDays must be 1 or 5: " + horizonDays);
    };
  }

  /** (to - from) / from * 100. from 이 0 이하면 null. */
  public static BigDecimal changePct(BigDecimal from, BigDecimal to) {
    if (from == null || to == null || from.signum() <= 0) {
      return null;
    }
    return to.subtract(from)
        .multiply(BigDecimal.valueOf(100))
        .divide(from, 4, RoundingMode.HALF_UP);
  }

  /** 폭 경계값은 보합이다(±0.5% 정확히는 FLAT). */
  public static Direction classify(BigDecimal changePct, BigDecimal flatBandPct) {
    if (changePct.compareTo(flatBandPct) > 0) {
      return Direction.UP;
    }
    if (changePct.compareTo(flatBandPct.negate()) < 0) {
      return Direction.DOWN;
    }
    return Direction.FLAT;
  }

  /** 기준일 다음 거래일 09:00(KST) 이후에 만들었으면 늦은 예측이다. */
  public static boolean isLate(Instant createdDate, LocalDate firstTradingDayAfterBase) {
    Instant open = firstTradingDayAfterBase.atTime(MARKET_OPEN).atZone(MARKET_ZONE_ID).toInstant();
    return !createdDate.isBefore(open);
  }

  /** 하루치 시세. */
  public record Bar(LocalDate tradeDate, BigDecimal close, long volume) {}

  /**
   * 분석 입력 지표. 데이터가 모자라면 그 값만 null.
   *
   * @param volumeRatio20 마지막 거래량 / 그 앞 20 거래일 평균 거래량
   */
  public record Indicators(
      BigDecimal change1dPct,
      BigDecimal change5dPct,
      BigDecimal change20dPct,
      BigDecimal ma5,
      BigDecimal ma20,
      BigDecimal closeVsMa20Pct,
      BigDecimal volumeRatio20) {}

  /** bars 는 오래된 것부터. */
  public static Indicators indicators(List<Bar> bars) {
    int n = bars.size();
    if (n == 0) {
      return new Indicators(null, null, null, null, null, null, null);
    }
    BigDecimal last = bars.get(n - 1).close();
    BigDecimal ma20 = movingAverage(bars, 20);
    return new Indicators(
        changeOver(bars, 1),
        changeOver(bars, 5),
        changeOver(bars, 20),
        movingAverage(bars, 5),
        ma20,
        changePct(ma20, last),
        volumeRatio(bars, 20));
  }

  private static BigDecimal changeOver(List<Bar> bars, int days) {
    int n = bars.size();
    return n > days ? changePct(bars.get(n - 1 - days).close(), bars.get(n - 1).close()) : null;
  }

  private static BigDecimal movingAverage(List<Bar> bars, int days) {
    int n = bars.size();
    if (n < days) {
      return null;
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (int i = n - days; i < n; i++) {
      sum = sum.add(bars.get(i).close());
    }
    return sum.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
  }

  private static BigDecimal volumeRatio(List<Bar> bars, int days) {
    int n = bars.size();
    if (n < days + 1) {
      return null;
    }
    long sum = 0;
    for (int i = n - 1 - days; i < n - 1; i++) {
      sum += bars.get(i).volume();
    }
    if (sum == 0) {
      return null;
    }
    BigDecimal average =
        BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(days), MathContext.DECIMAL64);
    return BigDecimal.valueOf(bars.get(n - 1).volume()).divide(average, 2, RoundingMode.HALF_UP);
  }
}
