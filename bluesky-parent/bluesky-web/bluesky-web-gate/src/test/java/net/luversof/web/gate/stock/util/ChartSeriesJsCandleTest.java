package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.dto.response.StockPriceChartPoint;
import net.luversof.web.gate.stock.dto.response.TradeResponse;

/** 캔들 시리즈의 내 매수 · 매도 수량(2026-10-02, 캔들 위 ▲ · ▼). */
class ChartSeriesJsCandleTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static StockPriceChartPoint day(String date) {
    BigDecimal p = new BigDecimal("100");
    return new StockPriceChartPoint(LocalDate.parse(date), p, p, p, p, null);
  }

  private static TradeResponse trade(String kstDateTime, TradeType type, int qty) {
    Instant at = java.time.LocalDateTime.parse(kstDateTime).atZone(KST).toInstant();
    return new TradeResponse(
        UUID.randomUUID(), null, null, "x", type, qty, BigDecimal.ONE, null, null, null, null, at);
  }

  @Test
  void 날짜별로_합치고_시세가_없는_날은_다음_시세일에_붙인다() {
    String js =
        ChartSeriesJs.candleSeries(
            List.of(day("2026-09-21"), day("2026-09-22"), day("2026-09-24")),
            List.of(
                trade("2026-09-21T10:00", TradeType.BUY, 10),
                trade("2026-09-21T14:00", TradeType.BUY, 5),
                trade("2026-09-22T11:00", TradeType.SELL, 3),
                // 9-23 은 시세가 없다(휴장 · 청약일 같은 기록) -> 9-24 봉에
                trade("2026-09-23T09:30", TradeType.BUY, 7),
                // 마지막 시세일 뒤는 붙일 봉이 없다 -> 뺀다
                trade("2026-09-25T09:30", TradeType.SELL, 9)),
            KST);
    assertThat(js).contains("buy:[15,0,7]").contains("sell:[0,3,0]");
  }

  @Test
  void 분배락일_주당_분배금을_싣고_없으면_0() {
    BigDecimal p = new BigDecimal("100");
    String js =
        ChartSeriesJs.candleSeries(
            List.of(
                new StockPriceChartPoint(LocalDate.parse("2026-09-11"), p, p, p, p, null, null),
                new StockPriceChartPoint(
                    LocalDate.parse("2026-09-14"), p, p, p, p, null, new BigDecimal("540")),
                new StockPriceChartPoint(
                    LocalDate.parse("2026-09-15"), p, p, p, p, null, new BigDecimal("12.50"))),
            List.of(),
            KST);
    assertThat(js).contains("dist:[0,540,12.5]");
  }

  @Test
  void 매매가_없으면_0_으로_채운다() {
    assertThat(ChartSeriesJs.candleSeries(List.of(day("2026-09-21"))))
        .contains("buy:[0]")
        .contains("sell:[0]");
  }

  private static net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint holding(
      String kstDate, String value, String cost) {
    Instant at = LocalDate.parse(kstDate).atStartOfDay(KST).toInstant();
    return new net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint(
        at,
        null,
        null,
        0,
        0,
        0,
        value == null ? null : new BigDecimal(value),
        cost == null ? null : new BigDecimal(cost),
        null,
        null);
  }

  @Test
  void 그_날_보유_평가액과_원가를_봉에_싣고_보유가_없던_날은_null() {
    // 2026-10-07 사용자 요청: 보유 평가액 추이를 따로 두지 않고 캔들 툴팁에. 다 판 날(평가액 0)과 시계열에 없는 날은 null.
    String js =
        ChartSeriesJs.candleSeries(
            List.of(day("2026-09-21"), day("2026-09-22"), day("2026-09-24")),
            List.of(),
            KST,
            List.of(
                holding("2026-09-21", "1500000.4", "1200000"), holding("2026-09-22", "0", "0")));
    assertThat(js).contains("hv:[1500000,null,null]").contains("hc:[1200000,null,null]");
    // 보유 시계열을 안 주면 칸은 있되 모두 null(옛 호출)
    assertThat(ChartSeriesJs.candleSeries(List.of(day("2026-09-21")), List.of(), KST))
        .contains("hv:[null]")
        .contains("hc:[null]");
  }
}
