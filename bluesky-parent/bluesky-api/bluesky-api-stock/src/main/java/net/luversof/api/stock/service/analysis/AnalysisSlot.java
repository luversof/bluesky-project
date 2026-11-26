package net.luversof.api.stock.service.analysis;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 분석 시점(사용자 결정 2026-11-27): 예측은 아무 때나 돌리고, 그 시각까지 있던 자료로 다음 종가(D+1) · 5 거래일 뒤(D+5)를 판단한다. 과거 기준일도 새
 * 정보가 생기는 네 시점에서 다시 판단해 예측 양을 늘린다. 버전끼리는 같은 (기준일, 시점) 에서만 비교한다.
 *
 * <p>INTRADAY 는 장이 열린 뒤 만든 실시간 예측만(늦은 예측) - 과거 시점으로는 만들지 않는다.
 */
public enum AnalysisSlot {
  /** 기준일 16:00 - 종가 뒤. 간밤 미국 장은 기준일 전날 것까지, 야간선물 · 장전가 없음. */
  CLOSE(0, LocalTime.of(16, 0), false, false, false),
  /** 기준일 20:00 - 저녁 뉴스까지. 나머지는 CLOSE 와 같다. */
  EVENING(0, LocalTime.of(20, 0), false, false, false),
  /** 다음 날 06:00 - 야간선물(05:00 마감) · 기준일 미국 장 뒤. 장전가 없음. */
  NIGHT(1, LocalTime.of(6, 0), true, true, false),
  /** 다음 날 08:30 - NXT 장전가 30 분 뒤(v10 규칙). */
  PREMARKET(1, LocalTime.of(8, 30), true, true, true),
  /** 장이 열린 뒤 만든 실시간 예측. 입력은 PREMARKET 과 같고, 늦은 예측으로 따로 센다. */
  INTRADAY(1, LocalTime.of(9, 0), true, true, true);

  /** 과거 기준일을 다시 판단하는 시점(채우는 순서). */
  public static final java.util.List<AnalysisSlot> REPLAYABLE =
      java.util.List.of(CLOSE, EVENING, NIGHT, PREMARKET);

  private final int dayOffset;
  private final LocalTime time;
  private final boolean overseasOfBaseDate;
  private final boolean nightFutures;
  private final boolean premarket;

  AnalysisSlot(
      int dayOffset,
      LocalTime time,
      boolean overseasOfBaseDate,
      boolean nightFutures,
      boolean premarket) {
    this.dayOffset = dayOffset;
    this.time = time;
    this.overseasOfBaseDate = overseasOfBaseDate;
    this.nightFutures = nightFutures;
    this.premarket = premarket;
  }

  /** 기준일의 이 시점 시각. 금요일 기준일의 NIGHT · PREMARKET 은 토요일(달력 다음 날) - 그 전부터 쓰던 08:30 리플레이와 같다. */
  public Instant at(LocalDate baseDate) {
    return baseDate
        .plusDays(dayOffset)
        .atTime(time)
        .atZone(PredictionMath.MARKET_ZONE_ID)
        .toInstant();
  }

  /** 해외 지수에 기준일(미국 날짜)의 장이 들어가나 - 한국 시간 다음 날 새벽에 끝난다. */
  public boolean overseasOfBaseDate() {
    return overseasOfBaseDate;
  }

  public boolean nightFutures() {
    return nightFutures;
  }

  public boolean premarket() {
    return premarket;
  }

  /** 옛 morning 플래그: true = 다음 날 08:30, false = 기준일 20:00. */
  public static AnalysisSlot ofMorning(boolean morning) {
    return morning ? PREMARKET : EVENING;
  }

  /**
   * 지금 실행하면 쓸 시점: 기준일의 시점 중 now 이전의 마지막(16:00 전이면 CLOSE). 장이 열린 뒤면 INTRADAY - 입력은 PREMARKET 과 같다.
   */
  public static AnalysisSlot current(LocalDate baseDate, Instant now) {
    if (!now.isBefore(
        nextWeekday(baseDate).atTime(9, 0).atZone(PredictionMath.MARKET_ZONE_ID).toInstant())) {
      return INTRADAY;
    }
    AnalysisSlot out = CLOSE;
    for (AnalysisSlot s : REPLAYABLE) {
      if (!now.isBefore(s.at(baseDate))) {
        out = s;
      }
    }
    return out;
  }

  /**
   * 저장된 예측의 시점: 만든 시각으로 정한다(DB 열 없이 - 2026-11-27 전에 만든 예측도 같은 규칙). late 가 있으면(채점됨) 그것으로, 없으면 다음 평일
   * 09:00 으로 장중을 가른다(휴장일은 채점 뒤에 바로잡힌다).
   */
  public static AnalysisSlot of(LocalDate baseDate, Instant created, Boolean late) {
    if (Boolean.TRUE.equals(late)) {
      return INTRADAY;
    }
    AnalysisSlot s = current(baseDate, created);
    return s == INTRADAY && late == null ? INTRADAY : s == INTRADAY ? PREMARKET : s;
  }

  static LocalDate nextWeekday(LocalDate d) {
    LocalDate next = d.plusDays(1);
    while (next.getDayOfWeek() == DayOfWeek.SATURDAY || next.getDayOfWeek() == DayOfWeek.SUNDAY) {
      next = next.plusDays(1);
    }
    return next;
  }
}
