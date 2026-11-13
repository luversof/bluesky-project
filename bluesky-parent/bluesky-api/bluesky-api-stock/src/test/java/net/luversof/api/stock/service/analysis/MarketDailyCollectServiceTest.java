package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockMarketDaily;
import net.luversof.api.stock.service.analysis.MarketDailyCollectService.Contract;

class MarketDailyCollectServiceTest {

  private static LocalDate d(String s) {
    return LocalDate.parse(s);
  }

  @Test
  void 선물_코드와_분기_만기() {
    assertThat(MarketDailyCollectService.contractCode(d("2025-12-01"))).isEqualTo("101W12");
    assertThat(MarketDailyCollectService.contractCode(d("2026-12-01"))).isEqualTo("A01612");
    assertThat(MarketDailyCollectService.expiryOf(d("2025-12-01"))).isEqualTo(d("2025-12-11"));
    // 만기일까지는 그 분기물, 다음 날부터 다음 분기물.
    assertThat(MarketDailyCollectService.quarterMonth(d("2025-12-11"))).isEqualTo(d("2025-12-01"));
    assertThat(MarketDailyCollectService.quarterMonth(d("2025-12-12"))).isEqualTo(d("2026-03-01"));
    assertThat(MarketDailyCollectService.quarterMonth(d("2026-01-05"))).isEqualTo(d("2026-03-01"));
  }

  @Test
  void 만기일엔_다음_물로_넘기고_확정_전_값은_뺀다() {
    BigDecimal one = BigDecimal.ONE;
    BigDecimal two = BigDecimal.TWO;
    // 12 월물: 12-11 만기라 그날 야간이 없다. 3 월물은 12-10 부터 있다.
    Contract dec =
        new Contract(
            "101W12",
            d("2025-12-11"),
            Map.of(d("2025-12-10"), one, d("2025-12-11"), one),
            Map.of(d("2025-12-10"), one));
    Contract mar =
        new Contract(
            "A01603",
            d("2026-03-12"),
            Map.of(d("2025-12-10"), two, d("2025-12-11"), two, d("2025-12-12"), two),
            Map.of(d("2025-12-10"), two, d("2025-12-11"), two, d("2025-12-12"), two));
    // 지금 12-12 10:00: 12-12 주간은 장중, 12-12 야간은 아직 안 열렸다.
    List<StockMarketDaily> rows =
        MarketDailyCollectService.rowsOf(
            List.of(dec, mar),
            d("2025-12-01"),
            d("2025-12-31"),
            LocalDateTime.parse("2025-12-12T10:00").atZone(PredictionMath.MARKET_ZONE_ID));
    assertThat(rows)
        .extracting(r -> r.getCode() + " " + r.getTradeDate() + " " + r.getContract())
        .containsExactly(
            "K200F_DAY 2025-12-10 101W12",
            "K200F_NIGHT 2025-12-10 101W12",
            "K200F_DAY 2025-12-11 A01603",
            "K200F_NIGHT 2025-12-11 A01603");
  }
}
