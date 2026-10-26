package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** 기준일 하루를 월중 · 월말 · 기타로 가르는 경계(말일에서 5 일 안 = 월말, 20 일까지 = 월중) - 2026-10-22 에 경계를 바로 보는 시험을 더했다. */
class PayoutWindowClassifyTest {

  private static String classify(String date) {
    return MonthlyDividendPayoutWindowGuesser.classify(LocalDate.parse(date));
  }

  @Test
  void 말일에서_5_일_안이면_월말() {
    assertThat(classify("2026-10-26")).as("31 일 달 - 말일 5 일 전").isEqualTo("MONTH_END");
    assertThat(classify("2026-10-25")).as("6 일 전").isEqualTo("OTHER");
    assertThat(classify("2026-02-23")).as("2 월(28 일) - 말일 5 일 전").isEqualTo("MONTH_END");
    assertThat(classify("2026-02-22")).isEqualTo("OTHER");
    assertThat(classify("2028-02-24")).as("윤년 2 월(29 일)").isEqualTo("MONTH_END");
  }

  @Test
  void 이십_일까지는_월중() {
    assertThat(classify("2026-10-20")).isEqualTo("MID_MONTH");
    assertThat(classify("2026-10-01")).isEqualTo("MID_MONTH");
    assertThat(classify("2026-10-21")).isEqualTo("OTHER");
  }
}
