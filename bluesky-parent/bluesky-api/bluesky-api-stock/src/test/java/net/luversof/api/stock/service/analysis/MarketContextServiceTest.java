package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.analysis.MarketContextService.Def;
import net.luversof.api.stock.service.analysis.MarketContextService.Kind;
import net.luversof.api.stock.service.analysis.MarketContextService.MarketLine;

class MarketContextServiceTest {

  private static final Def SPX = new Def("SPX", "S&P 500", Kind.OVERSEAS, "N", "SPX");
  private static final Def KOSPI = new Def("KOSPI", "KOSPI", Kind.DOMESTIC, "U", "0001");

  private static Map<String, Object> overseas(String date, String close) {
    return Map.of("stck_bsop_date", date, "ovrs_nmix_prpr", close);
  }

  @Test
  void 마지막_날과_1_5_거래일_변화율() {
    // KIS 는 최근 날짜가 먼저 온다 - 순서와 무관하게 날짜순으로 계산한다. 휴장 자리(0)는 거래일로 세지 않는다.
    List<Map<String, Object>> rows =
        List.of(
            overseas("20261109", "110"),
            overseas("20261106", "100"),
            overseas("20261105", "0"),
            overseas("20261104", "98"),
            overseas("20261103", "97"),
            overseas("20261102", "96"),
            overseas("20261030", "100"));
    MarketLine line = MarketContextService.lineOf(SPX, rows);
    assertThat(line.lastDate()).isEqualTo(LocalDate.of(2026, 11, 9));
    assertThat(line.close()).isEqualByComparingTo("110");
    assertThat(line.change1dPct()).isEqualByComparingTo("10");
    // 5 거래일 전 = 10-30(100) - 11-05 의 0 은 빠진다.
    assertThat(line.change5dPct()).isEqualByComparingTo("10");
  }

  @Test
  void 국내_지수는_업종_지수_필드를_읽고_행이_적으면_5일은_비운다() {
    List<Map<String, Object>> rows =
        List.of(
            Map.of("stck_bsop_date", "20261110", "bstp_nmix_prpr", "2600.50"),
            Map.of("stck_bsop_date", "20261109", "bstp_nmix_prpr", "2626.77"));
    MarketLine line = MarketContextService.lineOf(KOSPI, rows);
    assertThat(line.change1dPct()).isEqualByComparingTo(new BigDecimal("-1.0001"));
    assertThat(line.change5dPct()).isNull();
  }

  @Test
  void 종가가_하루뿐이면_싣지_않는다() {
    assertThat(MarketContextService.lineOf(SPX, List.of(overseas("20261109", "110")))).isNull();
  }
}
