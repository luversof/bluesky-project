package net.luversof.api.stock.service.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DayOfWeek;
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

  /**
   * 종목별 기준 비율(v3): 최근 window 거래일 동안 1 · 5 거래일 변화가 UP / DOWN / FLAT 이었던 비율(%). 채점과 같은 폭으로 나눈다. 예측이
   * "항상 FLAT" 같은 쉬운 답으로 숨지 않게, 그 종목이 실제로 얼마나 자주 움직였는지 알려 준다.
   */
  public record BaseRates(
      int window,
      BigDecimal d1UpPct,
      BigDecimal d1DownPct,
      BigDecimal d1FlatPct,
      BigDecimal d5UpPct,
      BigDecimal d5DownPct,
      BigDecimal d5FlatPct) {}

  /** bars 는 오래된 것부터. 표본이 하나도 없으면 null. */
  public static BaseRates baseRates(List<Bar> bars, int window) {
    BigDecimal[] d1 = directionShares(bars, 1, window);
    BigDecimal[] d5 = directionShares(bars, 5, window);
    if (d1 == null && d5 == null) {
      return null;
    }
    return new BaseRates(
        window,
        d1 == null ? null : d1[0],
        d1 == null ? null : d1[1],
        d1 == null ? null : d1[2],
        d5 == null ? null : d5[0],
        d5 == null ? null : d5[1],
        d5 == null ? null : d5[2]);
  }

  private static BigDecimal[] directionShares(List<Bar> bars, int horizon, int window) {
    int[] counts = new int[3];
    int total = 0;
    for (int i = bars.size() - 1; i - horizon >= 0 && total < window; i--) {
      BigDecimal change = changePct(bars.get(i - horizon).close(), bars.get(i).close());
      if (change == null) {
        continue;
      }
      counts[classify(change, flatBandPct(horizon)).ordinal()]++;
      total++;
    }
    if (total == 0) {
      return null;
    }
    BigDecimal[] out = new BigDecimal[3];
    for (int d = 0; d < 3; d++) {
      out[d] =
          BigDecimal.valueOf(counts[d] * 100L)
              .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
    return out;
  }

  /** 다음 분배 기준일 추정: 최근 기준일이 월말(25 일 이후)이면 그 다음 달들의 마지막 영업일, 아니면 같은 날짜(주말이면 앞 금요일). from 이후 첫 날짜. */
  // shortcut: 공휴일은 모른다 - 주말만 피한다, 휴장일 달력이 생기면 그걸 쓴다.
  public static LocalDate estimateNextRecordDate(LocalDate lastRecordDate, LocalDate from) {
    if (lastRecordDate == null) {
      return null;
    }
    boolean monthEnd = lastRecordDate.getDayOfMonth() >= 25;
    for (int m = 1; m <= 24; m++) {
      LocalDate month = lastRecordDate.withDayOfMonth(1).plusMonths(m);
      LocalDate candidate =
          monthEnd
              ? month.withDayOfMonth(month.lengthOfMonth())
              : month.withDayOfMonth(
                  Math.min(lastRecordDate.getDayOfMonth(), month.lengthOfMonth()));
      candidate = previousOrSameBusinessDay(candidate);
      if (!candidate.isBefore(from)) {
        return candidate;
      }
    }
    return null;
  }

  /** 배당락일: 기준일의 앞 영업일(국내 T+2 결제). 공휴일은 모른다. */
  public static LocalDate exDateOf(LocalDate recordDate) {
    return recordDate == null ? null : previousOrSameBusinessDay(recordDate.minusDays(1));
  }

  private static LocalDate previousOrSameBusinessDay(LocalDate date) {
    LocalDate d = date;
    while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
      d = d.minusDays(1);
    }
    return d;
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
