package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 대시보드 총자산 추이 선은 달마다 한 점이어야 한다.
 *
 * <p>실측 2026-09-12(최근 6개월 · MONTHLY): api-stock 이 8 점을 내려주었고 그중 2026-06-18 은 구간 최고
 * 평가액(2,098,800,125)이 난 날로 끼워 넣은 waypoint 였다. 자산 성장 화면은 그 점에 고점 주석을 달아 쓸모가 있지만, 대시보드 선은 pointRadius
 * 0 이라 아무것도 그리지 않고 x 축도 카테고리 축이라 간격만 어긋났다 &mdash; 실제 간격 30 · 30 · 31 · 18 · 12 · 31 · 31 · 12 일이 모두
 * 같은 너비로 그려졌고, 6월에만 점이 둘이라 캐션의 "월별" 과도 맞지 않았다.
 */
class StockTrendSeriesUtilTest {

  private record Point(LocalDate date, long value) {}

  @Test
  void 같은_달은_마지막_점만_남긴다() {
    List<Point> points =
        List.of(
            new Point(LocalDate.of(2026, 3, 31), 1),
            new Point(LocalDate.of(2026, 4, 30), 2),
            new Point(LocalDate.of(2026, 6, 18), 3),
            new Point(LocalDate.of(2026, 6, 30), 4),
            new Point(LocalDate.of(2026, 9, 12), 5));
    assertThat(StockTrendSeriesUtil.lastPerMonth(points, Point::date))
        .extracting(Point::value)
        .as("6월은 30일 하나만 남아야 한다")
        .containsExactly(1L, 2L, 4L, 5L);
  }

  @Test
  void 시간순서는_그대로_지킨다() {
    List<Point> points = new ArrayList<>();
    for (int month = 1; month <= 12; month++) {
      points.add(new Point(LocalDate.of(2026, month, 15), month));
      points.add(new Point(LocalDate.of(2026, month, 28), 100L + month));
    }
    assertThat(StockTrendSeriesUtil.lastPerMonth(points, Point::date))
        .extracting(Point::date)
        .isSorted();
    assertThat(StockTrendSeriesUtil.lastPerMonth(points, Point::date)).hasSize(12);
  }

  @Test
  void 빈_입력과_날짜_없는_점은_버린다() {
    assertThat(StockTrendSeriesUtil.lastPerMonth(null, Point::date)).isEmpty();
    assertThat(StockTrendSeriesUtil.lastPerMonth(List.<Point>of(), Point::date)).isEmpty();
    List<Point> withNulls = new ArrayList<>();
    withNulls.add(null);
    withNulls.add(new Point(null, 9));
    withNulls.add(new Point(LocalDate.of(2026, 5, 1), 7));
    assertThat(StockTrendSeriesUtil.lastPerMonth(withNulls, Point::date))
        .extracting(Point::value)
        .containsExactly(7L);
  }

  /** 대시보드 컨트롤러가 실제로 이 규칙을 거쳐야 한다 - 거치지 않으면 waypoint 가 다시 선에 끼어든다. */
  @Test
  void 대시보드_추이는_이_규칙을_거친다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java"),
            StandardCharsets.UTF_8);
    assertThat(flatten(source))
        .as("trendSeries 는 lastPerMonth 를 거쳐야 한다")
        .contains(
            "List<TradeProfitTimeSeriesPoint> trendSeries = net.luversof.web.gate.stock.util.StockTrendSeriesUtil.lastPerMonth(");
  }

  /** spotless 가 줄을 나누므로 공백을 평탄하게 해서 본다. */
  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
