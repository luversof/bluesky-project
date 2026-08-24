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

  /**
   * 세 숫자 칸의 {@code -} 처리가 서로 다르다는 사실을 고정한다.
   *
   * <p>주당 과세표준액과 분배율은 {@code -} 를 "값 없음" 으로 받는다(각각 0 과 null). 그런데 <b>분배금액만</b> 예외를 던지고, 그러면 그 줄 하나
   * 때문에 <b>가져오기 전체가 중단</b>된다. ETF 출처 페이지는 분배가 없던 달에 {@code -} 를 적는 곳이 있으므로 실제로 닿을 수 있는 경로다.
   *
   * <p>동작을 바꾸지 않은 이유: 네 곳의 출처가 분배금액 칸에 실제로 {@code -} 를 내는지는 바깥으로 요청을 보내야 알 수 있는데, 그 확인 없이 "건너뛴다" 로
   * 바꾸면 정말 깨진 숫자까지 조용히 삼킨다. 그래서 지금 동작을 고정해 두고, 가져오기가 이 메시지로 실패하면 여기를 보게 한다.
   *
   * <p>실측 2026-08-23: 지금 저장된 지급 이력 202 건은 모두 분배금액이 0 보다 크다. 즉 아직 이 경로에 닿은 적은 없다.
   */
  @Test
  void 분배금액이_대시면_가져오기가_그_줄에서_멈춘다() {
    assertThatThrownBy(
            () ->
                parser.parse(
                    "381170",
                    """
						지급기준일	실지급일	분배금액(원)	주당과세표준액(원)
						2024-12-30	2025-01-03	-	-
						2024-11-29	2024-12-03	134	-
						"""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("주당 분배금");
  }

  /** 같은 자리에 값이 있으면 정상이다. 위 검사가 "대시" 때문임을 분명히 한다. */
  @Test
  void 분배금액에_값이_있으면_정상이다() {
    List<MonthlyDividendPayoutUpsertRequest> requests =
        parser.parse(
            "381170",
            """
						지급기준일	실지급일	분배금액(원)	주당과세표준액(원)
						2024-12-30	2025-01-03	126	-
						""");

    assertThat(requests).hasSize(1);
    assertThat(requests.get(0).getDividendAmountPerShare()).isEqualByComparingTo("126");
    assertThat(requests.get(0).getTaxableBasePerShare()).isEqualByComparingTo("0");
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
