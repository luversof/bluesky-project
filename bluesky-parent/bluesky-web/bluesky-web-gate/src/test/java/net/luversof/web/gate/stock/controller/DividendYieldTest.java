package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DividendView;

/**
 * 배당수익률 계산을 고정한다. 화면의 핵심 수치인데 검증이 하나도 없었다.
 *
 * <p>핵심 규칙은 <b>분자와 분모가 같은 모집단이어야 한다</b>는 것이다. 지급일에 이미 전량 매도한 배당은 기준일 원금이 없어 분모에 기여하지 못하는데, 분자에 그
 * 배당금을 넣으면 수익률이 부풀어 오른다(실측: 193 건 중 5 건·세후 144,360 원이 그렇게 들어가 7.12% 가 7.14% 로 보였다. 매도한 포지션의 비중이 큰
 * 사용자에게는 더 커진다).
 *
 * <p>또 하나. 평균 원금은 <b>포지션(계좌x종목)별로 평균을 낸 뒤 합산</b>한다. 전체를 한 번에 평균 내면 배당 횟수가 많은 포지션이 분모를 끌어내린다.
 */
class DividendYieldTest {

  private static final UUID ACCOUNT = UUID.randomUUID();
  private static final UUID STOCK_A = UUID.randomUUID();
  private static final UUID STOCK_B = UUID.randomUUID();

  private static DividendView dividend(
      UUID stockItemId, String net, String principalCost, String principalMarket) {
    return new DividendView(
        UUID.randomUUID(),
        ACCOUNT,
        "계좌",
        stockItemId,
        "종목",
        10,
        BigDecimal.ONE,
        new BigDecimal(net),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        new BigDecimal(net),
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-15T00:00:00Z"),
        null,
        null,
        principalCost == null ? null : new BigDecimal(principalCost),
        principalMarket == null ? null : new BigDecimal(principalMarket),
        null,
        null);
  }

  @Test
  void 원금이_없는_배당은_분자에도_넣지_않는다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("전체", 365);
    accumulator.accept(dividend(STOCK_A, "1000", "100000", "100000"));
    // 지급일에 이미 전량 매도 -> 기준일 원금 없음
    accumulator.accept(dividend(STOCK_A, "500", null, null));

    var view = accumulator.toView();

    // 세후 총액은 둘 다 센다(표에 보이는 합계).
    assertThat(view.totalNetAmount()).isEqualByComparingTo("1500");
    // 수익률 분자는 분모에 기여한 1,000 만. 100,000 대비 1.0000%
    assertThat(view.yieldOnCostPct()).isEqualByComparingTo("1.0000");
    assertThat(view.averagePrincipalCost()).isEqualByComparingTo("100000.00");
  }

  /** 분자에 전액을 넣었다면 1.5% 가 되어 실제보다 높게 보인다. */
  @Test
  void 전액을_분자에_넣으면_과대계상된다는_것을_보인다() {
    BigDecimal inflated =
        StockDividendHtmxController.percentage(new BigDecimal("1500"), new BigDecimal("100000"));
    assertThat(inflated).isEqualByComparingTo("1.5000");
  }

  @Test
  void 평균원금은_포지션별로_평균낸_뒤_합산한다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("전체", 365);
    // A 종목: 배당 2회, 원금 100,000 / 200,000 -> 평균 150,000
    accumulator.accept(dividend(STOCK_A, "1000", "100000", "100000"));
    accumulator.accept(dividend(STOCK_A, "1000", "200000", "200000"));
    // B 종목: 배당 1회, 원금 50,000 -> 평균 50,000
    accumulator.accept(dividend(STOCK_B, "1000", "50000", "50000"));

    var view = accumulator.toView();

    // 150,000 + 50,000 = 200,000 (전체를 한 번에 평균내면 116,666.67 이 되어 수익률이 부풀어 오른다)
    assertThat(view.averagePrincipalCost()).isEqualByComparingTo("200000.00");
    assertThat(view.yieldOnCostPct()).isEqualByComparingTo("1.5000"); // 3,000 / 200,000
  }

  @Test
  void 원금이_하나도_없으면_수익률은_null이다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("전체", 365);
    accumulator.accept(dividend(STOCK_A, "1000", null, null));

    var view = accumulator.toView();

    assertThat(view.totalNetAmount()).isEqualByComparingTo("1000");
    assertThat(view.averagePrincipalCost()).isNull();
    assertThat(view.yieldOnCostPct()).as("분모가 없으면 비율을 만들어내지 않는다").isNull();
    assertThat(view.yieldOnMarketPct()).isNull();
  }

  /** 일평균 원금은 기간 일수로 나눈다. 일수가 0 이면 비율을 내지 않는다. */
  @Test
  void 일평균_원금은_기간_일수로_나눈다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("전체", 10);
    accumulator.accept(dividend(STOCK_A, "1000", "100000", "100000"));
    accumulator.acceptDailyPrincipalCostSum(new BigDecimal("1000000")); // 10일치 합

    var view = accumulator.toView();

    assertThat(view.averageDailyPrincipalCost()).isEqualByComparingTo("100000.00");
    assertThat(view.yieldOnDailyAverageCostPct()).isEqualByComparingTo("1.0000");
  }

  @Test
  void 기간_일수가_0이면_일평균을_내지_않는다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("전체", 0);
    accumulator.accept(dividend(STOCK_A, "1000", "100000", "100000"));
    accumulator.acceptDailyPrincipalCostSum(new BigDecimal("1000000"));

    var view = accumulator.toView();

    assertThat(view.averageDailyPrincipalCost()).isNull();
    assertThat(view.yieldOnDailyAverageCostPct()).isNull();
  }

  @Test
  void 분모가_0이하면_비율을_내지_않는다() {
    assertThat(StockDividendHtmxController.percentage(new BigDecimal("100"), BigDecimal.ZERO))
        .isNull();
    assertThat(StockDividendHtmxController.percentage(new BigDecimal("100"), new BigDecimal("-1")))
        .isNull();
    assertThat(StockDividendHtmxController.percentage(null, new BigDecimal("100"))).isNull();
    assertThat(StockDividendHtmxController.percentage(new BigDecimal("100"), null)).isNull();
  }

  /** 마지막 배당일은 지급일 기준, 지급일이 없으면 기준일로 본다. */
  @Test
  void 마지막_배당일은_가장_늦은_지급일이다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("전체", 365);
    accumulator.accept(dividend(STOCK_A, "1000", "100000", "100000"));
    var later =
        new DividendView(
            UUID.randomUUID(),
            ACCOUNT,
            "계좌",
            STOCK_A,
            "종목",
            10,
            BigDecimal.ONE,
            new BigDecimal("1000"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            new BigDecimal("1000"),
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-15T00:00:00Z"),
            null,
            null,
            new BigDecimal("100000"),
            new BigDecimal("100000"),
            null,
            null);
    accumulator.accept(later);

    assertThat(accumulator.toView().lastDividendDate())
        .isEqualTo(Instant.parse("2026-03-15T00:00:00Z"));
  }

  /**
   * 일수가중 수익률도 다른 두 수익률과 같은 분자를 쓴다.
   *
   * <p>기준일 원금이 있는 배당은 그 날 그 종목을 들고 있었다는 뜻이라 일수 합계에도 반드시 기여한다. 반대로 기준일 원금이 없는 배당은 기간에 따라 일수 합계에 전혀
   * 기여하지 않을 수 있다 &mdash; NAVER 는 2021-01-18 에 전량 매도했는데 배당이 2021-04-08 로 기록돼 있어, 4월만 보는 기간에서는 원금이
   * 하루도 없는데 배당만 분자에 들어간다. 예전에는 이 수익률만 전액(totalNetAmount)을 분자로 써서 그런 기간에 과대 계상됐다.
   */
  @Test
  void 일수가중_수익률도_원금이_있는_배당만_분자에_넣는다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("전체", 10);
    accumulator.accept(dividend(STOCK_A, "1000", "100000", "100000"));
    // 기간 안에 원금이 하루도 없던 종목의 배당(지급 시점엔 이미 전량 매도)
    accumulator.accept(dividend(STOCK_A, "500", null, null));
    accumulator.acceptDailyPrincipalCostSum(new BigDecimal("1000000")); // 10일치 합 -> 일평균 100,000

    var view = accumulator.toView();

    assertThat(view.totalNetAmount()).as("표의 합계는 둘 다 센다").isEqualByComparingTo("1500");
    assertThat(view.averageDailyPrincipalCost()).isEqualByComparingTo("100000.00");
    assertThat(view.yieldOnDailyAverageCostPct())
        .as("전액 1,500 을 분자에 넣으면 1.5% 로 과대 계상된다")
        .isEqualByComparingTo("1.0000");
  }

  /** 세 수익률이 같은 분자를 쓰는지. 하나만 규칙이 다르면 같은 화면에서 서로 어긋난다. */
  @Test
  void 세_수익률이_같은_분자를_쓴다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("전체", 10);
    accumulator.accept(dividend(STOCK_A, "1000", "100000", "100000"));
    accumulator.accept(dividend(STOCK_A, "500", null, null));
    accumulator.acceptDailyPrincipalCostSum(new BigDecimal("1000000"));

    var view = accumulator.toView();

    assertThat(view.yieldOnCostPct()).isEqualByComparingTo("1.0000");
    assertThat(view.yieldOnMarketPct()).isEqualByComparingTo("1.0000");
    assertThat(view.yieldOnDailyAverageCostPct()).isEqualByComparingTo("1.0000");
  }
}
