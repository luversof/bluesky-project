package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DividendView;

/**
 * 배당 내역의 연도별·월별 집계.
 *
 * <p>사용자 요청 2026-09-10: 배당 화면에는 월별 막대 차트만 있어 "그 해에 얼마 받았나" 를 숫자로 읽을 수 없었다. 묶는 기준은 지급일이며, 존은 화면이 쓰는
 * 것과 같아야 한다 - 지급일이 UTC 로 2026-01-01T00:00Z 면 KST 로는 2026-01-01 이지만, 2025-12-31T15:00Z 도 KST 로는
 * 2026-01-01 이라 존을 잘못 쓰면 해가 통째로 어긋난다.
 */
class DividendPeriodBreakdownTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static DividendView dividend(String payDate, String gross, String tax, String net) {
    return new DividendView(
        null,
        null,
        null,
        null,
        "종목",
        1,
        null,
        gross == null ? null : new BigDecimal(gross),
        tax == null ? null : new BigDecimal(tax),
        null,
        null,
        net == null ? null : new BigDecimal(net),
        null,
        payDate == null ? null : Instant.parse(payDate),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void 연도별로_묶고_최근이_위에_온다() {
    List<DividendView> dividends =
        List.of(
            dividend("2025-03-10T00:00:00Z", "1000", "154", "846"),
            dividend("2025-09-10T00:00:00Z", "2000", "308", "1692"),
            dividend("2026-01-10T00:00:00Z", "3000", "462", "2538"));

    List<DividendPeriodBreakdown.Row> rows = DividendPeriodBreakdown.byYear(dividends, KST);

    assertThat(rows).extracting(DividendPeriodBreakdown.Row::label).containsExactly("2026", "2025");
    assertThat(rows.get(1).count()).isEqualTo(2);
    assertThat(rows.get(1).grossAmount()).isEqualByComparingTo("3000");
    assertThat(rows.get(1).tax()).isEqualByComparingTo("462");
    assertThat(rows.get(1).netAmount()).isEqualByComparingTo("2538");
  }

  @Test
  void 월별은_yyyy_MM_이름으로_묶는다() {
    List<DividendView> dividends =
        List.of(
            dividend("2026-08-10T00:00:00Z", "100", "15", "85"),
            dividend("2026-08-25T00:00:00Z", "200", "30", "170"),
            dividend("2026-09-10T00:00:00Z", "300", "46", "254"));

    List<DividendPeriodBreakdown.Row> rows = DividendPeriodBreakdown.byMonth(dividends, KST);

    assertThat(rows)
        .extracting(DividendPeriodBreakdown.Row::label)
        .containsExactly("2026-09", "2026-08");
    assertThat(rows.get(1).count()).isEqualTo(2);
    assertThat(rows.get(1).netAmount()).isEqualByComparingTo("255");
  }

  @Test
  void 존을_화면과_같게_써야_해가_어긋나지_않는다() {
    List<DividendView> newYearEveInUtc =
        List.of(dividend("2025-12-31T15:00:00Z", "100", "0", "100"));

    assertThat(DividendPeriodBreakdown.byYear(newYearEveInUtc, KST))
        .extracting(DividendPeriodBreakdown.Row::label)
        .containsExactly("2026");
    assertThat(DividendPeriodBreakdown.byYear(newYearEveInUtc, ZoneId.of("UTC")))
        .extracting(DividendPeriodBreakdown.Row::label)
        .containsExactly("2025");
  }

  @Test
  void 지급일이_없으면_기준일로_묶고_둘_다_없으면_뺀다() {
    DividendView recordDateOnly =
        new DividendView(
            null,
            null,
            null,
            null,
            "종목",
            1,
            null,
            new BigDecimal("500"),
            BigDecimal.ZERO,
            null,
            null,
            new BigDecimal("500"),
            Instant.parse("2026-07-01T00:00:00Z"),
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    DividendView noDates =
        new DividendView(
            null,
            null,
            null,
            null,
            "종목",
            1,
            null,
            new BigDecimal("900"),
            null,
            null,
            null,
            new BigDecimal("900"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    List<DividendPeriodBreakdown.Row> rows =
        DividendPeriodBreakdown.byMonth(
            java.util.Arrays.asList(recordDateOnly, noDates, null), KST);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).label()).isEqualTo("2026-07");
    assertThat(rows.get(0).netAmount()).isEqualByComparingTo("500");
  }

  @Test
  void 빈_목록과_null_금액을_견딘다() {
    assertThat(DividendPeriodBreakdown.byYear(List.of(), KST)).isEmpty();
    assertThat(DividendPeriodBreakdown.byMonth(null, KST)).isEmpty();

    List<DividendPeriodBreakdown.Row> rows =
        DividendPeriodBreakdown.byYear(
            List.of(dividend("2026-05-01T00:00:00Z", null, null, null)), KST);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).grossAmount()).isEqualByComparingTo("0");
    assertThat(rows.get(0).count()).isEqualTo(1);
  }

  @Test
  void 합계는_행을_그대로_더한_값이다() {
    List<DividendView> dividends =
        List.of(
            dividend("2025-03-10T00:00:00Z", "1000", "154", "846"),
            dividend("2026-01-10T00:00:00Z", "3000", "462", "2538"));
    List<DividendPeriodBreakdown.Row> rows = DividendPeriodBreakdown.byYear(dividends, KST);

    DividendPeriodBreakdown.Row total = DividendPeriodBreakdown.total(rows, "합계");

    assertThat(total.count()).isEqualTo(2);
    assertThat(total.grossAmount()).isEqualByComparingTo("4000");
    assertThat(total.tax()).isEqualByComparingTo("616");
    assertThat(total.netAmount()).isEqualByComparingTo("3384");
    assertThat(DividendPeriodBreakdown.total(List.of(), "합계").count()).isZero();
  }

  /**
   * 실측 2026-09-10: 3개월을 고르면 연도별 표가 한 줄뿐이라 아무것도 말해 주지 않는다(자산 성장이 같은 이유로 달 단위를 먼저 보여 준다). 화면이 그럴 때
   * 월별을 기본 탭으로 알려 준다 - 동작은 panelTabDefault.test.mjs 가, 조건은 여기서 지킨다.
   */
  @Test
  void 연도가_한_줄이면_월별_탭을_기본으로_알려_준다() throws java.io.IOException {
    String fragment =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/jte/stock/htmx/fragments/dividend/dividendPeriodBreakdown.jte"),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(fragment)
        .as("짧은 구간에서 연도별 한 줄만 보여 주면 표가 아무것도 말해 주지 않는다")
        .contains(
            "data-panel-tab-default=\"${yearlyRows != null && yearlyRows.size() > 1 ? \"year\" : \"month\"}\"");
  }

  /**
   * 실측 2026-09-10('3년' 프리셋): 첫 해 2023 은 9월부터만 들어와 화면에 1건 2,197,046원으로 보이는데 그 해 전체는 4건 7,612,046원이다.
   * 구간이 그 해를 온전히 덮지 않으면 표시한다(자산 성장 연도별 성과와 같은 표기).
   */
  @Test
  void 구간이_그_해를_다_덮지_않으면_부분_연도로_표시한다() {
    List<DividendView> dividends =
        List.of(
            dividend("2023-09-20T00:00:00Z", "100", "0", "100"),
            dividend("2024-05-10T00:00:00Z", "200", "0", "200"),
            dividend("2026-02-10T00:00:00Z", "300", "0", "300"));

    List<DividendPeriodBreakdown.Row> rows =
        DividendPeriodBreakdown.byYear(
            dividends,
            KST,
            java.time.LocalDate.of(2023, 9, 11),
            java.time.LocalDate.of(2026, 9, 10));

    assertThat(rows)
        .extracting(DividendPeriodBreakdown.Row::label)
        .containsExactly("2026", "2024", "2023");
    assertThat(rows.get(0).partial()).as("2026 은 9/10 까지만이라 부분").isTrue();
    assertThat(rows.get(1).partial()).as("2024 는 통째로 들어왔다").isFalse();
    assertThat(rows.get(2).partial()).as("2023 은 9/11 부터라 부분").isTrue();
    // 어느 구간만 담겼는지도 밝힌다(자산 성장 연도별 성과와 같은 표기).
    assertThat(rows.get(2).coveredFrom()).isEqualTo(java.time.LocalDate.of(2023, 9, 11));
    assertThat(rows.get(2).coveredTo()).isEqualTo(java.time.LocalDate.of(2023, 12, 31));
    assertThat(rows.get(0).coveredFrom()).isEqualTo(java.time.LocalDate.of(2026, 1, 1));
    assertThat(rows.get(0).coveredTo()).isEqualTo(java.time.LocalDate.of(2026, 9, 10));
    assertThat(rows.get(1).coveredFrom()).as("온전한 해는 구간을 적지 않는다").isNull();
  }

  @Test
  void 구간을_주지_않으면_부분_표시는_하지_않는다() {
    List<DividendPeriodBreakdown.Row> rows =
        DividendPeriodBreakdown.byYear(
            List.of(dividend("2026-02-10T00:00:00Z", "300", "0", "300")), KST);
    assertThat(rows.get(0).partial()).isFalse();
    assertThat(
            DividendPeriodBreakdown.byMonth(
                    List.of(dividend("2026-02-10T00:00:00Z", "300", "0", "300")), KST)
                .get(0)
                .partial())
        .as("월별은 부분 표시를 쓰지 않는다")
        .isFalse();
  }

  /**
   * 실측 2026-09-10(qa/dividend-chart-vs-table.cjs): 같은 화면의 월별 막대 차트는 3년 구간의 37달을 모두 그리는데 표는 23줄이었다
   * (2023-11 다음이 2024-04). 구간을 알면 빈 달도 0 으로 채워 차트와 같은 달을 보여 준다.
   */
  @Test
  void 구간을_주면_배당이_없던_달도_0으로_채운다() {
    List<DividendView> dividends =
        List.of(
            dividend("2026-01-10T00:00:00Z", "100", "0", "100"),
            dividend("2026-04-10T00:00:00Z", "300", "0", "300"));

    List<DividendPeriodBreakdown.Row> rows =
        DividendPeriodBreakdown.byMonth(
            dividends,
            KST,
            java.time.LocalDate.of(2026, 1, 1),
            java.time.LocalDate.of(2026, 4, 30));

    assertThat(rows)
        .extracting(DividendPeriodBreakdown.Row::label)
        .containsExactly("2026-04", "2026-03", "2026-02", "2026-01");
    assertThat(rows.get(1).count()).as("2026-03 은 배당이 없다").isZero();
    assertThat(rows.get(1).netAmount()).isEqualByComparingTo("0");
    assertThat(rows.get(3).netAmount()).isEqualByComparingTo("100");
    assertThat(DividendPeriodBreakdown.total(rows, "합계").netAmount())
        .as("0 을 채워도 합계는 그대로다")
        .isEqualByComparingTo("400");
  }

  @Test
  void 구간을_주지_않으면_있는_달만_보여_준다() {
    List<DividendView> dividends =
        List.of(
            dividend("2026-01-10T00:00:00Z", "100", "0", "100"),
            dividend("2026-04-10T00:00:00Z", "300", "0", "300"));
    assertThat(DividendPeriodBreakdown.byMonth(dividends, KST))
        .extracting(DividendPeriodBreakdown.Row::label)
        .containsExactly("2026-04", "2026-01");
  }

  @Test
  void 구간_밖의_달도_빠뜨리지_않는다() {
    List<DividendView> dividends =
        List.of(
            dividend("2025-12-10T00:00:00Z", "500", "0", "500"),
            dividend("2026-01-10T00:00:00Z", "100", "0", "100"));

    List<DividendPeriodBreakdown.Row> rows =
        DividendPeriodBreakdown.byMonth(
            dividends,
            KST,
            java.time.LocalDate.of(2026, 1, 1),
            java.time.LocalDate.of(2026, 2, 28));

    assertThat(rows).extracting(DividendPeriodBreakdown.Row::label).contains("2025-12");
    assertThat(DividendPeriodBreakdown.total(rows, "합계").netAmount()).isEqualByComparingTo("600");
  }
}
