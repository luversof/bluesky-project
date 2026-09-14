package net.luversof.web.gate.stock.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 대시보드 총자산 추이 선에 쓰는 점 고르기.
 *
 * <p>api-stock 의 시계열은 granularity 로 달마다 한 점씩 내려주지만, 그 뒤에 구간의 실제 최고 · 최저 평가액이 난 <b>그 날</b>을 waypoint
 * 로 끼워 넣는다. 자산 성장 화면은 그 점에 고점 · 저점 주석을 달기 때문에 필요한 점이다.
 *
 * <p>대시보드의 작은 선은 사정이 다르다 &mdash; {@code pointRadius: 0} 이라 점도 주석도 그리지 않아 그 waypoint 가 전달하는 것이 없고, x
 * 축이 시간 축이 아니라 <b>카테고리 축</b>이라 점 간격이 날짜와 무관하게 모두 같은 폭이 된다. 실측 2026-09-12(최근 6개월 · MONTHLY): 8 점의 실제
 * 간격이 30 · 30 · 31 · 18 · 12 · 31 · 31 · 12 일인데 화면에서는 전부 같은 너비였다. 12 일짜리 구간이 31 일짜리와 같은 폭을 차지해 시간축이
 * 2.6 배까지 어긋나고, 6월에만 점이 둘(06-18 · 06-30)이라 캡션의 "월별" 과도 맞지 않았다.
 *
 * <p>그래서 이 화면에서는 달마다 마지막 점만 남긴다. 달의 마지막 점이 곧 그 달의 버킷이므로 남는 선은 캡션이 말하는 그대로의 월별 선이다. 고점 · 저점은 이 선을
 * 눌렀을 때 가는 자산 성장 화면이 주석으로 보여 준다.
 */
public final class StockTrendSeriesUtil {

  private StockTrendSeriesUtil() {}

  /**
   * 같은 달의 점이 여럿이면 마지막 하나만 남긴다. 입력 순서는 그대로 지킨다.
   *
   * @param points 시간순으로 정렬된 점
   * @param dateFn 점에서 날짜를 꺼내는 함수. null 을 돌려주는 점은 그대로 버린다
   */
  public static <T> List<T> lastPerMonth(List<T> points, Function<T, LocalDate> dateFn) {
    if (points == null || points.isEmpty()) {
      return List.of();
    }
    Map<YearMonth, T> lastByMonth = new LinkedHashMap<>();
    for (T point : points) {
      if (point == null) {
        continue;
      }
      LocalDate date = dateFn.apply(point);
      if (date == null) {
        continue;
      }
      lastByMonth.put(YearMonth.from(date), point);
    }
    return new ArrayList<>(lastByMonth.values());
  }
}
