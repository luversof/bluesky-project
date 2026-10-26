package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;

/** 대시보드 요약의 두 셈(종목별 손익 · 미니 추이 배열) - 2026-10-22 summary 에서 정적 도우미로 옮기며 처음 둔 시험. */
class StockSummaryHtmxControllerHelpersTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static TradeProfit row(UUID stock, UUID account, String totalProfitNet) {
    Object[] args = new Object[26];
    args[0] = stock;
    args[2] = account;
    args[6] = 0;
    args[10] = 0;
    args[24] = totalProfitNet == null ? null : new BigDecimal(totalProfitNet);
    try {
      var types =
          Arrays.stream(TradeProfit.class.getRecordComponents())
              .limit(26)
              .map(java.lang.reflect.RecordComponent::getType)
              .toArray(Class<?>[]::new);
      return TradeProfit.class.getConstructor(types).newInstance(args);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static TradeProfitTimeSeriesPoint point(String instant, String holdings) {
    return new TradeProfitTimeSeriesPoint(
        instant == null ? null : Instant.parse(instant),
        null,
        null,
        0,
        0,
        0,
        holdings == null ? null : new BigDecimal(holdings),
        null,
        null,
        null);
  }

  @Test
  void 종목별_손익은_계좌를_합친다() {
    UUID s1 = UUID.randomUUID();
    UUID s2 = UUID.randomUUID();
    var map =
        StockSummaryHtmxController.totalProfitByStockItem(
            Arrays.asList(
                row(s1, UUID.randomUUID(), "100"),
                row(s1, UUID.randomUUID(), "-30"),
                row(s2, UUID.randomUUID(), null),
                row(null, UUID.randomUUID(), "999"),
                null));

    assertThat(map).containsOnlyKeys(s1, s2);
    assertThat(map.get(s1)).as("두 계좌 합").isEqualByComparingTo("70");
    assertThat(map.get(s2)).as("값 없으면 0").isEqualByComparingTo("0");
  }

  @Test
  void 추이_배열은_존의_날짜와_평가액() {
    var chart =
        StockSummaryHtmxController.trendChartJs(
            Arrays.asList(
                point("2026-09-30T15:30:00Z", "1000.50"),
                null,
                point(null, "5"),
                point("2026-10-21T16:00:00Z", null)),
            KST);

    assertThat(chart.labels())
        .as("KST 날짜(UTC 15:30 은 다음 날)")
        .isEqualTo("[%s2026-10-01%s,%s2026-10-22%s]".formatted(Q, Q, Q, Q));
    assertThat(chart.values()).isEqualTo("[1000.50,0]");
    assertThat(chart.pointCount()).isEqualTo(2);
  }

  @Test
  void 점이_없으면_빈_배열() {
    var chart = StockSummaryHtmxController.trendChartJs(List.of(), KST);

    assertThat(chart.labels()).isEqualTo("[]");
    assertThat(chart.values()).isEqualTo("[]");
    assertThat(chart.pointCount()).isZero();
  }

  private static final String Q = String.valueOf((char) 34);
}
