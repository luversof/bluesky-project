package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendReferenceSummaryView;

/**
 * 월배당 기준 카드의 문구는 <b>실제로 쓴 행 수</b>를 말한다.
 *
 * <p>카드 둘이 "최근 12건 기준" 을 고정으로 달고 있었다. 평균 분배금은 있는 만큼(최대 12 행)을 쓰고, 과세표준 비중은 <b>그중 분배금이 0 보다 큰 행만</b>
 * 쓴다 &mdash; 둘은 서로 다를 수 있고, 12 와도 다를 수 있다. 실측 2026-09-16(0104P0): 저장된 이력이 11 건뿐인데 "최근 12건 기준" 이라고
 * 적혀 있었고 값은 11 건 평균이었다.
 */
class MonthlyDividendBasisRowsTest {

  private final MonthlyDividendCalculator calculator = new MonthlyDividendCalculator();

  @Test
  void 열두_건이_안_되면_있는_만큼을_말한다() {
    List<MonthlyDividendPayoutResponse> rows = new ArrayList<>();
    for (int i = 0; i < 11; i++) {
      rows.add(payout("100", "30", LocalDate.of(2026, 9, 15).minusMonths(i)));
    }

    MonthlyDividendReferenceSummaryView summary = calculator.buildReferenceSummary("AAA", rows);

    assertThat(summary.averageBasisRowCount()).as("평균이 쓴 행 수").isEqualTo(11);
    assertThat(summary.taxableRatioBasisRowCount()).as("과세표준 비중이 쓴 행 수").isEqualTo(11);
    assertThat(summary.payoutCount()).as("저장된 전체 행 수").isEqualTo(11);
  }

  @Test
  void 열두_건이_넘으면_열둘만_쓴다() {
    List<MonthlyDividendPayoutResponse> rows = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      rows.add(payout("100", "30", LocalDate.of(2026, 9, 15).minusMonths(i)));
    }

    MonthlyDividendReferenceSummaryView summary = calculator.buildReferenceSummary("AAA", rows);

    assertThat(summary.averageBasisRowCount()).isEqualTo(12);
    assertThat(summary.taxableRatioBasisRowCount()).isEqualTo(12);
    assertThat(summary.payoutCount()).as("저장된 전체 행 수는 따로다").isEqualTo(30);
  }

  /** 분배금이 0 인 달은 비중을 낼 수 없어 빠진다 - 그래서 두 카드의 기준 수가 갈린다. */
  @Test
  void 분배금이_영인_행은_비중에서_빠진다() {
    List<MonthlyDividendPayoutResponse> rows = new ArrayList<>();
    rows.add(payout("0", "0", LocalDate.of(2026, 9, 15)));
    rows.add(payout("0", "0", LocalDate.of(2026, 8, 15)));
    for (int i = 0; i < 5; i++) {
      rows.add(payout("100", "30", LocalDate.of(2026, 7, 15).minusMonths(i)));
    }

    MonthlyDividendReferenceSummaryView summary = calculator.buildReferenceSummary("AAA", rows);

    assertThat(summary.averageBasisRowCount()).as("평균은 0 인 달도 센다").isEqualTo(7);
    assertThat(summary.taxableRatioBasisRowCount()).as("비중은 0 인 달을 뺀다").isEqualTo(5);
  }

  @Test
  void 이력이_없으면_영이다() {
    MonthlyDividendReferenceSummaryView summary =
        calculator.buildReferenceSummary("AAA", List.of());

    assertThat(summary.averageBasisRowCount()).isZero();
    assertThat(summary.taxableRatioBasisRowCount()).isZero();
  }

  /**
   * 화면이 <b>고정된 12</b> 를 말하지 않는다.
   *
   * <p>값을 맞게 내도 문구가 거짓이면 사용자는 11 건 평균을 12 건 평균으로 읽는다. 문구 키는 {@code {0}} 을 받아 각 카드가 자기 수를 말한다.
   */
  @Test
  void 화면_문구가_기준_건수를_받는다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/monthlyDividendReference.jte"),
            StandardCharsets.UTF_8);
    // 빌드가 ${} 안 공백을 지우므로 공백을 눌러 비교한다.
    String squeezed = template.replaceAll("\\s+", "");

    assertThat(squeezed)
        .as("평균 카드가 자기 기준 건수를 말한다")
        .contains("MessageFormat.format(basisRowsTemplate,averageBasisRows)");
    assertThat(squeezed)
        .as("과세표준 카드가 자기 기준 건수를 말한다")
        .contains("MessageFormat.format(basisRowsTemplate,taxableBasisRows)");
    assertThat(template).as("고정 문구 키는 더 쓰지 않는다").doesNotContain("summary.last.year");

    String messages =
        Files.readString(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8);
    assertThat(messages).as("문구가 수를 받는다").contains("Based on the latest {0} rows");
    assertThat(messages).as("옛 고정 문구는 없다").doesNotContain("Based on the latest 12 rows");
  }

  /** 평균 금액은 표시할 때 두 자리로 한 번만 반올림한다(그대로 찍으면 종목마다 자릿수가 갈린다). */
  @Test
  void 평균_금액은_두_자리로_찍는다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/monthlyDividendReference.jte"),
            StandardCharsets.UTF_8);
    String squeezed = template.replaceAll("\\s+", "");

    assertThat(squeezed)
        .as("표시 직전에 두 자리로 반올림한다")
        .contains(
            "amountFormat.format(averageDividend.setScale(2,java.math.RoundingMode.HALF_UP))");
  }

  private static MonthlyDividendPayoutResponse payout(
      String dividendPerShare, String taxableBasePerShare, LocalDate recordDate) {
    return new MonthlyDividendPayoutResponse(
        null,
        null,
        null,
        null,
        recordDate,
        recordDate.plusDays(2),
        null,
        new BigDecimal(dividendPerShare),
        new BigDecimal(taxableBasePerShare),
        null);
  }
}
