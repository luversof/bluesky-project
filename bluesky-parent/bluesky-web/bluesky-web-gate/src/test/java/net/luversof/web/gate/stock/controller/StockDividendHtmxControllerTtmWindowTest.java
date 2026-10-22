package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/** 배당 머리 숫자(최근 1 년)의 창 - 2026-10-22 dividendList 에서 ttmWindow 로 옮기며 처음 둔 시험. */
class StockDividendHtmxControllerTtmWindowTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

  /** 기간 끝은 배타(다음 날 자정)로 들어온다. */
  private static Instant exclusiveEndOf(LocalDate lastDay) {
    return lastDay.plusDays(1).atStartOfDay(KST).toInstant();
  }

  @Test
  void 기간을_고르지_않으면_오늘까지_365_일() {
    var window = StockDividendHtmxController.ttmWindow(null, TODAY, KST);

    assertThat(window.endDate()).isEqualTo(TODAY);
    assertThat(window.startDate()).isEqualTo(LocalDate.of(2025, 10, 23));
    assertThat(window.startInstant())
        .isEqualTo(LocalDate.of(2025, 10, 23).atStartOfDay(KST).toInstant());
    assertThat(window.endInstant()).as("끝 다음 날 자정(배타)").isEqualTo(exclusiveEndOf(TODAY));
  }

  @Test
  void 지난_기간이면_그_끝에서_거꾸로() {
    var window =
        StockDividendHtmxController.ttmWindow(
            exclusiveEndOf(LocalDate.of(2025, 12, 31)), TODAY, KST);

    assertThat(window.endDate()).isEqualTo(LocalDate.of(2025, 12, 31));
    assertThat(window.startDate()).isEqualTo(LocalDate.of(2025, 1, 1));
  }

  @Test
  void 끝이_오늘을_넘으면_오늘에서_멈춘다() {
    var window =
        StockDividendHtmxController.ttmWindow(
            exclusiveEndOf(LocalDate.of(2026, 12, 31)), TODAY, KST);

    assertThat(window.endDate()).isEqualTo(TODAY);
    assertThat(window.endInstant()).isEqualTo(exclusiveEndOf(TODAY));
  }
}
