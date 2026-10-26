package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

/**
 * 금융소득 종합과세 기준 대비 비율의 "올해 남은 지급" 셈 - 분기 · 연 배당 종목이 올해 더 받을 횟수와, 달마다 나오는 종목인지 가르는 규칙. 화면 렌더 시험
 * (view/FinancialIncomeThresholdTest)만 있어 규칙을 바로 보는 시험을 2026-10-22 에 더했다.
 */
class FinancialIncomeThresholdCyclesTest {

  private static TreeMap<LocalDate, BigDecimal> days(String... isoDates) {
    TreeMap<LocalDate, BigDecimal> map = new TreeMap<>();
    for (String date : isoDates) {
      map.put(LocalDate.parse(date), BigDecimal.TEN);
    }
    return map;
  }

  @Test
  void 작년_차례가_올해_아직_오지_않았으면_센다() {
    // 작년 11-20 · 12-20 차례는 올해 아직 안 왔다. 올해 08-19 는 작년 08-20 차례(창 밖이라 보지 않음).
    var payDays = days("2025-08-20", "2025-11-20", "2025-12-20", "2026-08-19");

    assertThat(
            FinancialIncomeThresholdSupport.pendingCyclesThisYear(
                payDays, LocalDate.parse("2026-10-22")))
        .isEqualTo(2);
  }

  @Test
  void 이번_차례를_이미_받았으면_세지_않는다() {
    // 2026-10-20 검토: 작년 11-20 · 올해 11-19 면 11-19 에 두 번 셌다.
    var payDays = days("2025-11-20", "2025-12-20", "2026-11-19");

    assertThat(
            FinancialIncomeThresholdSupport.pendingCyclesThisYear(
                payDays, LocalDate.parse("2026-11-25")))
        .as("11 월 차례는 받았고 12 월 차례만 남았다")
        .isEqualTo(1);
  }

  @Test
  void 늦어지는_지급도_창_안이면_아직_올_것으로_본다() {
    var payDays = days("2025-11-20");

    assertThat(
            FinancialIncomeThresholdSupport.pendingCyclesThisYear(
                payDays, LocalDate.parse("2026-12-05")))
        .as("예정일 + 15 일 - 아직 창 안")
        .isEqualTo(1);
    assertThat(
            FinancialIncomeThresholdSupport.pendingCyclesThisYear(
                payDays, LocalDate.parse("2026-12-09")))
        .as("예정일 + 19 일 - 창 안 마지막 날")
        .isEqualTo(1);
    assertThat(
            FinancialIncomeThresholdSupport.pendingCyclesThisYear(
                payDays, LocalDate.parse("2026-12-10")))
        .as("예정일 + 20 일 - 창의 아래 끝은 들지 않는다")
        .isZero();
    assertThat(
            FinancialIncomeThresholdSupport.pendingCyclesThisYear(
                payDays, LocalDate.parse("2026-12-15")))
        .as("예정일 + 25 일 - 작년 지급일이 보는 창(오늘 - 1 년 - 20 일 보다 뒤) 밖이라 세지 않는다")
        .isZero();
  }

  @Test
  void 달마다_나오는_종목인지는_지급_간격의_중앙값으로() {
    assertThat(
            FinancialIncomeThresholdSupport.isMonthlyPayer(
                days("2026-07-15", "2026-08-14", "2026-09-15", "2026-10-15")))
        .isTrue();
    assertThat(
            FinancialIncomeThresholdSupport.isMonthlyPayer(
                days("2026-01-20", "2026-04-20", "2026-07-20", "2026-10-20")))
        .as("분기")
        .isFalse();
    assertThat(
            FinancialIncomeThresholdSupport.isMonthlyPayer(
                days("2026-01-15", "2026-02-15", "2026-03-15", "2026-07-15", "2026-08-15")))
        .as("한 번 크게 비어도 중앙값은 한 달")
        .isTrue();
    assertThat(FinancialIncomeThresholdSupport.isMonthlyPayer(days("2026-10-15")))
        .as("한 번뿐")
        .isFalse();
    assertThat(FinancialIncomeThresholdSupport.isMonthlyPayer(null)).isFalse();
  }
}
