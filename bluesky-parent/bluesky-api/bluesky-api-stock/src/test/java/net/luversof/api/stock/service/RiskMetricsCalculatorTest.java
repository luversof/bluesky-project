package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.service.RiskMetricsCalculator.RiskMetrics;

/** 최대낙폭 · 변동성(사용자 결정 2026-09-22: 1 년 기준, 표에 보여 주기만 한다). */
class RiskMetricsCalculatorTest {

  private static final UUID ITEM = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 22);

  private static StockDailyClosePrice price(String date, String close) {
    return new StockDailyClosePrice(ITEM, LocalDate.parse(date), new BigDecimal(close));
  }

  /** 오늘부터 거꾸로 하루씩 짚어 가며 종가를 붙인다(오름차순으로 돌려준다). */
  private static List<StockDailyClosePrice> series(String... closes) {
    List<StockDailyClosePrice> rows = new ArrayList<>();
    LocalDate date = TODAY.minusDays(closes.length - 1L);
    for (String close : closes) {
      rows.add(price(date.toString(), close));
      date = date.plusDays(1);
    }
    return rows;
  }

  @Test
  void 전고점_대비_가장_많이_빠진_폭을_낸다() {
    // 100 -> 120 까지 올랐다가 90 으로. 전고점 120 대비 -25% 가 최대 낙폭이다.
    // 시작가 100 대비로 세면 -10% 가 되어 실제로 겪은 손실을 작게 말한다.
    RiskMetrics metrics =
        RiskMetricsCalculator.compute(series("100", "110", "120", "90", "100"), TODAY, 12);

    assertThat(metrics).isNotNull();
    assertThat(metrics.maxDrawdownPct()).isEqualByComparingTo("-25.00");
    assertThat(metrics.tradingDays()).isEqualTo(5);
  }

  @Test
  void 오르기만_했으면_낙폭은_0이다() {
    RiskMetrics metrics = RiskMetricsCalculator.compute(series("100", "110", "120"), TODAY, 12);

    assertThat(metrics).isNotNull();
    assertThat(metrics.maxDrawdownPct()).isEqualByComparingTo("0");
  }

  @Test
  void 회복해도_한때_빠진_폭은_남는다() {
    // 반등했다고 해서 그 사이 겪은 낙폭이 없던 일이 되지 않는다.
    RiskMetrics metrics =
        RiskMetricsCalculator.compute(series("100", "50", "100", "200"), TODAY, 12);

    assertThat(metrics.maxDrawdownPct()).isEqualByComparingTo("-50.00");
  }

  @Test
  void 표본이_모자라면_변동성을_안_낸다() {
    // 며칠치로 낸 변동성은 숫자일 뿐이다 - 0 이나 아무 값이나 내면 사실처럼 읽힌다.
    RiskMetrics few = RiskMetricsCalculator.compute(series("100", "110", "120"), TODAY, 12);

    assertThat(few).isNotNull();
    assertThat(few.volatilityPct()).isNull();
    assertThat(few.maxDrawdownPct()).as("낙폭은 이틀로도 낼 수 있다").isNotNull();
  }

  @Test
  void 움직이지_않으면_변동성은_0이다() {
    String[] flat = new String[40];
    java.util.Arrays.fill(flat, "100");

    RiskMetrics metrics = RiskMetricsCalculator.compute(series(flat), TODAY, 12);

    assertThat(metrics.volatilityPct()).isEqualByComparingTo("0.00");
    assertThat(metrics.maxDrawdownPct()).isEqualByComparingTo("0");
  }

  @Test
  void 더_출렁인_쪽이_더_큰_변동성을_낸다() {
    String[] calm = new String[40];
    String[] wild = new String[40];
    for (int index = 0; index < 40; index++) {
      calm[index] = index % 2 == 0 ? "100" : "101";
      wild[index] = index % 2 == 0 ? "100" : "110";
    }

    BigDecimal calmVol = RiskMetricsCalculator.compute(series(calm), TODAY, 12).volatilityPct();
    BigDecimal wildVol = RiskMetricsCalculator.compute(series(wild), TODAY, 12).volatilityPct();

    assertThat(wildVol).isGreaterThan(calmVol);
    // 연환산이 빠지면 숫자가 16 분의 1 쯤으로 작아져 "안 출렁인다" 로 읽힌다.
    assertThat(wildVol).as("연환산(sqrt 252)이 들어가야 한다").isGreaterThan(new BigDecimal("50"));
  }

  /**
   * 손으로 세어 둔 값 하나를 박아 둔다 - 100 과 101 을 번갈아 21 점이면 표본표준편차 x sqrt(252) x 100 = 16.21 이다(독립 계산
   * 2026-09-22).
   *
   * <p>연환산을 빼면 1.02, 365 일로 내면 19.50, 모표준편차로 내면 15.80 이 나온다 - 세 가지가 서로 다른 수라 어느 것이 틀었는지까지 가른다.
   */
  @Test
  void 변동성은_표본표준편차를_252일로_연환산한_값이다() {
    String[] closes = new String[21];
    for (int index = 0; index < closes.length; index++) {
      closes[index] = index % 2 == 0 ? "100" : "101";
    }

    RiskMetrics metrics = RiskMetricsCalculator.compute(series(closes), TODAY, 12);

    assertThat(metrics.volatilityPct())
        .as("수익률 20 개 - 최소 표본 수에 딱 맞는다")
        .isEqualByComparingTo("16.21");
    assertThat(metrics.maxDrawdownPct())
        .as("전고점 101 에서 100 으로 - -0.99%")
        .isEqualByComparingTo("-0.99");
    assertThat(metrics.tradingDays()).isEqualTo(21);
  }

  @Test
  void 기간_밖은_세지_않는다() {
    List<StockDailyClosePrice> rows =
        List.of(
            price("2024-01-02", "1000"),
            price("2026-09-01", "100"),
            price("2026-09-10", "110"),
            price("2026-09-22", "105"));

    RiskMetrics metrics = RiskMetricsCalculator.compute(rows, TODAY, 1);

    assertThat(metrics.tradingDays()).as("한 달 안의 세 날만 센다").isEqualTo(3);
    assertThat(metrics.fromDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    // 2024 년의 1000 원을 전고점으로 잡으면 낙폭이 -89% 가 된다.
    assertThat(metrics.maxDrawdownPct()).isEqualByComparingTo("-4.55");
  }

  @Test
  void 언제부터_센_것인지_알린다() {
    // 이력이 기간을 못 덮으면 있는 만큼으로 내되, "1 년 기준" 이라 적으면 거짓이 되므로 시작일을 준다.
    RiskMetrics metrics =
        RiskMetricsCalculator.compute(
            List.of(price("2026-08-03", "100"), price("2026-09-22", "90")), TODAY, 12);

    assertThat(metrics.fromDate()).isEqualTo(LocalDate.of(2026, 8, 3));
    assertThat(metrics.tradingDays()).isEqualTo(2);
  }

  @Test
  void 셀_것이_없으면_아무_값도_안_낸다() {
    assertThat(RiskMetricsCalculator.compute(null, TODAY, 12)).isNull();
    assertThat(RiskMetricsCalculator.compute(List.of(), TODAY, 12)).isNull();
    assertThat(RiskMetricsCalculator.compute(series("100"), TODAY, 12))
        .as("하루치로는 낙폭도 못 낸다")
        .isNull();
    assertThat(RiskMetricsCalculator.compute(series("100", "110"), TODAY, 0)).isNull();
  }
}
