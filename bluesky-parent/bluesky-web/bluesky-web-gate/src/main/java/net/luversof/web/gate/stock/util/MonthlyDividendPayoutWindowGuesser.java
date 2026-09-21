package net.luversof.web.gate.stock.util;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 지급 이력의 기준일로 <b>월중 · 월말</b>을 가린다(사용자 결정 2026-09-21: 링크 등록 때 자동 판정).
 *
 * <p>규칙은 실측으로 정했다. 등록된 12 종목의 지급 이력 258 건에 그대로 대 보니 <b>맞음 12 · 틀림 0 · 미확인 0</b> 이었고, 종목 안에서도 한 방향으로
 * 100% 모였다(예: 329200 월말 58/58, 476800 월중 30/30, 이력이 2 건뿐인 신규 2 종목도 맞았다).
 *
 * <p>한 방향으로 60% 도 안 모이면 {@link #UNKNOWN} 으로 두고 사람이 고르게 한다 &mdash; 억지로 하나를 고르면 월배당 화면의 지급 시기 필터와 "월중
 * · 월말" 집계가 조용히 틀어진다.
 */
public final class MonthlyDividendPayoutWindowGuesser {

  public static final String MID_MONTH = "MID_MONTH";
  public static final String MONTH_END = "MONTH_END";
  public static final String OTHER = "OTHER";
  public static final String UNKNOWN = "UNKNOWN";

  /** 말일에서 이 날수 안쪽이면 월말로 본다. */
  static final int MONTH_END_WITHIN_DAYS = 5;

  /** 이 날짜 이하면 월중으로 본다. */
  static final int MID_MONTH_UNTIL_DAY = 20;

  /** 한 방향이 이만큼은 모여야 정한다. */
  static final double REQUIRED_SHARE = 0.6;

  private MonthlyDividendPayoutWindowGuesser() {}

  public static String guess(List<LocalDate> recordDates) {
    if (recordDates == null || recordDates.isEmpty()) {
      return UNKNOWN;
    }

    Map<String, Integer> votes = new HashMap<>();
    int total = 0;
    for (LocalDate recordDate : recordDates) {
      if (recordDate == null) {
        continue;
      }

      votes.merge(classify(recordDate), 1, Integer::sum);
      total++;
    }
    if (total == 0) {
      return UNKNOWN;
    }

    String top = UNKNOWN;
    int best = 0;
    for (Map.Entry<String, Integer> vote : votes.entrySet()) {
      if (vote.getValue() > best) {
        best = vote.getValue();
        top = vote.getKey();
      }
    }
    return (double) best / total >= REQUIRED_SHARE ? top : UNKNOWN;
  }

  /** 하루를 월중 · 월말 · 기타로. 달마다 말일이 달라 "말일에서 며칠 전" 으로 센다(2월 · 31일 달). */
  static String classify(LocalDate recordDate) {
    int lastDay = recordDate.lengthOfMonth();
    if (lastDay - recordDate.getDayOfMonth() <= MONTH_END_WITHIN_DAYS) {
      return MONTH_END;
    }

    return recordDate.getDayOfMonth() <= MID_MONTH_UNTIL_DAY ? MID_MONTH : OTHER;
  }
}
