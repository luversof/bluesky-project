package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;

class MonthlyDividendPayoutImportParserTest {

  private final MonthlyDividendPayoutImportParser parser = new MonthlyDividendPayoutImportParser();

  @Test
  void parsesKodexFormatWithTwoDigitYearsAndDistributionRate() {
    List<MonthlyDividendPayoutUpsertRequest> requests =
        parser.parse(
            "498400",
            """
						지급기준일	실지급일	분배율(%)	분배금액(원)	주당과세표준액(원)
						26.05.15	26.05.19	1.42%	348	0
						26.04.15	26.04.17	1.43%	262	43
						""");

    assertThat(requests).hasSize(2);
    assertPayout(
        requests.get(0),
        "498400",
        LocalDate.of(2026, 5, 15),
        LocalDate.of(2026, 5, 19),
        "1.42",
        "348",
        "0");
    assertPayout(
        requests.get(1),
        "498400",
        LocalDate.of(2026, 4, 15),
        LocalDate.of(2026, 4, 17),
        "1.43",
        "262",
        "43");
  }

  @Test
  void parsesRiseFormatWithoutDistributionRateAndDashTaxableBase() {
    List<MonthlyDividendPayoutUpsertRequest> requests =
        parser.parse(
            "381170",
            """
						지급기준일	실지급일	분배금액(원)	주당과세표준액(원)
						2024-12-30	2025-01-03	126	-
						2024-11-29	2024-12-03	134	-
						""");

    assertThat(requests).hasSize(2);
    assertPayout(
        requests.get(0),
        "381170",
        LocalDate.of(2024, 12, 30),
        LocalDate.of(2025, 1, 3),
        null,
        "126",
        "0");
    assertPayout(
        requests.get(1),
        "381170",
        LocalDate.of(2024, 11, 29),
        LocalDate.of(2024, 12, 3),
        null,
        "134",
        "0");
  }

  @Test
  void parsesPlusFormatWithSpacedHeaders() {
    List<MonthlyDividendPayoutUpsertRequest> requests =
        parser.parse(
            "488770",
            """
						지급 기준일	실 지급일	분배금 (원)	주당과세표준액 (원)
						2026.05.15	2026.05.19	159	44
						2026.04.15	2026.04.17	163	163
						""");

    assertThat(requests).hasSize(2);
    assertPayout(
        requests.get(0),
        "488770",
        LocalDate.of(2026, 5, 15),
        LocalDate.of(2026, 5, 19),
        null,
        "159",
        "44");
    assertPayout(
        requests.get(1),
        "488770",
        LocalDate.of(2026, 4, 15),
        LocalDate.of(2026, 4, 17),
        null,
        "163",
        "163");
  }

  @Test
  void parsesTigerFormatWithAlternatePayDateHeader() {
    List<MonthlyDividendPayoutUpsertRequest> requests =
        parser.parse(
            "458730",
            """
						지급기준일	실제지급일	분배금액(원)	주당 과세 표준액(원)
						2026-04-30	2026-05-06	415	25
						2026-03-31	2026-04-02	350	60
						""");

    assertThat(requests).hasSize(2);
    assertPayout(
        requests.get(0),
        "458730",
        LocalDate.of(2026, 4, 30),
        LocalDate.of(2026, 5, 6),
        null,
        "415",
        "25");
    assertPayout(
        requests.get(1),
        "458730",
        LocalDate.of(2026, 3, 31),
        LocalDate.of(2026, 4, 2),
        null,
        "350",
        "60");
  }

  @Test
  void rejectsRowsWhosePayDateIsEarlierThanRecordDateWithExactValues() {
    assertThatThrownBy(
            () ->
                parser.parse(
                    "466940",
                    """
								지급기준일	실지급일	분배금액(원)	주당과세표준액(원)
								2026-05-15	2025-05-19	390	8
								2026-04-15	2026-04-17	340	54
								"""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("2번째 줄의 실지급일(2025-05-19)은 지급기준일(2026-05-15)보다 빠를 수 없습니다.");
  }

  private void assertPayout(
      MonthlyDividendPayoutUpsertRequest request,
      String symbol,
      LocalDate recordDate,
      LocalDate payDate,
      String distributionRate,
      String dividendAmount,
      String taxableBase) {
    assertThat(request.getSymbol()).isEqualTo(symbol);
    assertThat(request.getRecordDate()).isEqualTo(recordDate);
    assertThat(request.getPayDate()).isEqualTo(payDate);
    assertThat(request.getDistributionRatePct())
        .isEqualTo(distributionRate != null ? new BigDecimal(distributionRate) : null);
    assertThat(request.getDividendAmountPerShare()).isEqualTo(new BigDecimal(dividendAmount));
    assertThat(request.getTaxableBasePerShare()).isEqualTo(new BigDecimal(taxableBase));
  }
}
