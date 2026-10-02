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
  void 매매가_없으면_0_으로_채운다() {
    assertThat(ChartSeriesJs.candleSeries(List.of(day("2026-09-21"))))
        .contains("buy:[0]")
        .contains("sell:[0]");
  }
}
