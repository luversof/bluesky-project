package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

/**
 * 화면의 <b>연 값은 화면의 월 값 x 12</b> 여야 한다.
 *
 * <p>실측 2026-09-11(같은 자료, 두 화면): 월배당 시뮬레이터는 원 단위로 자르기 전 합계에 12 를 곱해 <b>36,657,049</b> 를 적었고 배당 캘린더는
 * 행마다 원 단위로 자른 뒤 더해 <b>36,657,048</b> 을 적었다. 월 값은 둘 다 3,054,754 로 같았다. 시뮬레이터 안에서도 3,054,754 x 12 =
 * 36,657,048 과 어긋났고, 과세표준은 220,539 x 12 = 2,646,468 인데 <b>2,646,466</b> 이었다.
 *
 * <p>사용자가 계산기로 검산할 수 있는 쪽(= 화면 값끼리 맞는 쪽)을 택한다.
 */
class MonthlyDividendAnnualRoundingTest {

  private static MonthlyDividendSnapshotResponse row(String monthlyDividend, String taxable) {
    return new MonthlyDividendSnapshotResponse(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        new BigDecimal(monthlyDividend),
        null,
        null,
        null,
        null,
        new BigDecimal(taxable),
        null,
        null,
        null);
  }

  @Test
  void 연_값은_화면의_월_값의_12배다() {
    // 각 행이 소수를 품어 합계가 x.5 를 넘는 경우: 월 표시는 반올림되고 연은 그 12 배여야 한다.
    var summary =
        new MonthlyDividendCalculator()
            .buildSimulatorSummary(List.of(row("100.4", "10.4"), row("100.2", "10.2")));

    long shownMonthly =
        net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(
            summary.totalExpectedMonthlyDividend());
    long shownAnnual =
        net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(
            summary.totalExpectedAnnualDividend());
    assertThat(shownAnnual).isEqualTo(shownMonthly * 12);

    long shownMonthlyTax =
        net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(
            summary.totalExpectedTaxableBaseAmount());
    long shownAnnualTax =
        net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(
            summary.totalExpectedAnnualTaxableBaseAmount());
    assertThat(shownAnnualTax).isEqualTo(shownMonthlyTax * 12);
  }
}
