package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DividendView;
import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;
import net.luversof.web.gate.stock.util.StockYieldUtil;

/**
 * 연 수익률(들고 있던 날 기준) &mdash; 사용자 요청 2026-09-28: "배당에 대한 수익률을 좀 더 이해하기 쉽게. 다양한 수익률과 투입원금이 잘 눈에 안
 * 들어온다".
 *
 * <p>예전 기본 지표(기간 일평균 투입원금 수익률)는 들고 있지 않은 날도 0 원으로 세어 기간 전체(2,354 일)로 나눠, 약 115~134 일 들고 있던 종목이
 * 284.68% 로 보였다. 연 수익률 = 세후 배당 x 365 / (날마다 들고 있던 원금의 합) 은 같은 종목을 연 44.14% 로 본다.
 */
class DividendYieldAnnualizedTest {

  private static final UUID ACCOUNT = UUID.randomUUID();

  private static DividendView dividend(String net, String principalCost) {
    return new DividendView(
        UUID.randomUUID(),
        ACCOUNT,
        "계좌",
        UUID.randomUUID(),
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
        null,
        null,
        null);
  }

  private static BitSet days(int from, int toExclusive) {
    BitSet bits = new BitSet();
    bits.set(from, toExclusive);
    return bits;
  }

  /** 기간 1,000 일 중 100 일만 100 만 원을 들고 세후 3 만 원: 예전 기준 30% · 연 수익률 10.95%. */
  @Test
  void 들고_있던_날로_1년_기준을_낸다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("종목", 1000);
    accumulator.accept(dividend("30000", "1000000"));
    accumulator.acceptDailyPrincipalCostSum(new BigDecimal("100000000")); // 100 만 x 100 일
    accumulator.acceptHeldDays(days(900, 1000));

    DividendYieldGroupView view = accumulator.toView();

    assertThat(view.yieldOnDailyAverageCostPct())
        .as("예전 기준: 기간 일평균 10 만 원으로 나눠 부풀었다")
        .isEqualByComparingTo("30.0000");
    assertThat(view.heldDayCount()).isEqualTo(100);
    assertThat(view.heldAverageDailyPrincipalCost()).isEqualByComparingTo("1000000.00");
    // 30,000 / 1,000,000 x 365 / 100 = 10.95%
    assertThat(view.annualizedYieldPct()).isEqualByComparingTo("10.9500");
    assertThat(view.principalCostDaySum()).isEqualByComparingTo("100000000");
    assertThat(view.shortHeld()).isFalse();
    assertThat(view.periodDayCount()).isEqualTo(1000);
  }

  private static net.luversof.web.gate.stock.dto.response.TradeResponse trade(
      net.luversof.web.gate.stock.constant.TradeType type,
      int quantity,
      String price,
      String realizedProfit,
      String date) {
    return new net.luversof.web.gate.stock.dto.response.TradeResponse(
        UUID.randomUUID(),
        ACCOUNT,
        UUID.randomUUID(),
        "종목",
        type,
        quantity,
        new BigDecimal(price),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        new BigDecimal(price).multiply(BigDecimal.valueOf(quantity)),
        realizedProfit == null ? null : new BigDecimal(realizedProfit),
        Instant.parse(date + "T03:00:00Z"));
  }

  /**
   * 날마다의 원금을 거래로 되짚어 '들고 있던 날' 을 켠다 - 1 월 11 일 매수, 1 월 21 일 전량 매도면 11~20 일 10 일. 매도한 날부터는 원금이 0 이라
   * 꺼진다.
   */
  @Test
  void 거래로_들고_있던_날을_켠다() {
    var zone = java.time.ZoneId.of("Asia/Seoul");
    var summary =
        StockDividendHtmxController.summarizePeriodPrincipalCosts(
            List.of(
                trade(
                    net.luversof.web.gate.stock.constant.TradeType.BUY,
                    10,
                    "1000",
                    null,
                    "2026-01-11"),
                trade(
                    net.luversof.web.gate.stock.constant.TradeType.SELL,
                    10,
                    "1000",
                    "0",
                    "2026-01-21")),
            java.time.LocalDate.of(2026, 1, 1),
            java.time.LocalDate.of(2026, 1, 31),
            zone);

    assertThat(summary.heldDays().cardinality()).isEqualTo(10);
    assertThat(summary.heldDays().nextSetBit(0)).as("1 월 11 일 = 칸 10").isEqualTo(10);
    assertThat(summary.heldDays().previousSetBit(30)).as("1 월 20 일 = 칸 19").isEqualTo(19);
    assertThat(summary.principalCostSum()).as("1 만 원 x 10 일").isEqualByComparingTo("100000");
  }

  /** 머리 숫자의 문장 "평균 A 원을 넣어 두고 B 원을 받았다" 는 B / A = 머리 숫자여야 한다. B 는 분자(기준일 원금이 있는 배당)라 세후 전체와 다르다. */
  @Test
  void 머리_문장의_두_금액으로_머리_숫자가_되짚힌다() {
    var ttm = new StockDividendHtmxController.YieldAccumulator("portfolio", 365);
    ttm.accept(dividend("50000", "1000000"));
    // 매도 뒤 지급 - 원금이 없어 분자에서 빠진다.
    ttm.accept(dividend("7000", null));
    ttm.acceptDailyPrincipalCostSum(new BigDecimal("365000000")); // 100 만 x 365 일
    ttm.acceptHeldDays(days(0, 365));

    var attributes = StockDividendHtmxController.ttmHeadline(ttm.toView());

    BigDecimal yieldPct = attributes.yieldPct();
    BigDecimal average = attributes.averageDailyPrincipalCost();
    BigDecimal used = attributes.netWithPrincipalCost();
    assertThat(yieldPct).isEqualByComparingTo("5.0000");
    assertThat(used).as("분자 - 세후 전체 57,000 이 아니다").isEqualByComparingTo("50000");
    assertThat(
            used.multiply(BigDecimal.valueOf(100))
                .divide(average, 4, java.math.RoundingMode.HALF_UP))
        .isEqualByComparingTo(yieldPct);
    assertThat(attributes.netAmount()).as("월 평균은 받은 돈 전체로").isEqualByComparingTo("57000");
    assertThat(StockDividendHtmxController.ttmHeadline(null).yieldPct()).isNull();
  }

  /** 순위는 연 수익률 순이되, 보유가 짧아 흔들리는 행은 연 수익률이 높아도 뒤로 간다. */
  @Test
  void 순위는_연_수익률_순이고_짧게_든_행은_뒤다() {
    var shortHigh = new StockDividendHtmxController.YieldAccumulator("짧게 높음", 1000);
    shortHigh.accept(dividend("10000", "1000000"));
    shortHigh.acceptDailyPrincipalCostSum(new BigDecimal("30000000")); // 100 만 x 30 일 -> 연 12.17%
    shortHigh.acceptHeldDays(days(0, 30));
    var longMid = new StockDividendHtmxController.YieldAccumulator("오래 중간", 1000);
    longMid.accept(dividend("20000", "1000000"));
    longMid.acceptDailyPrincipalCostSum(new BigDecimal("300000000")); // 100 만 x 300 일 -> 연 2.43%
    longMid.acceptHeldDays(days(0, 300));
    var longHigh = new StockDividendHtmxController.YieldAccumulator("오래 높음", 1000);
    longHigh.accept(dividend("50000", "1000000"));
    longHigh.acceptDailyPrincipalCostSum(new BigDecimal("300000000")); // -> 연 6.08%
    longHigh.acceptHeldDays(days(0, 300));

    List<DividendYieldGroupView> rows =
        StockDividendHtmxController.sortYieldRows(List.of(shortHigh, longMid, longHigh));

    assertThat(rows)
        .extracting(DividendYieldGroupView::label)
        .containsExactly("오래 높음", "오래 중간", "짧게 높음");
    assertThat(rows.get(2).shortHeld()).isTrue();
  }

  /** 계좌 · 종목을 합친 행은 겹친 날을 한 번만 센다 - 두 포지션이 같은 날 들고 있어도 그날은 하루다. */
  @Test
  void 겹친_날은_한_번만_센다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("계좌", 365);
    accumulator.accept(dividend("1000", "100000"));
    accumulator.acceptDailyPrincipalCostSum(new BigDecimal("20000000"));
    accumulator.acceptHeldDays(days(0, 200));
    accumulator.acceptHeldDays(days(100, 300));

    DividendYieldGroupView view = accumulator.toView();

    assertThat(view.heldDayCount()).as("0~299 = 300 일(겹친 100 일은 한 번)").isEqualTo(300);
    assertThat(view.heldAverageDailyPrincipalCost()).isEqualByComparingTo("66666.67");
  }

  /** 연도 행은 기간 전체의 보유 날 중 그 해 칸만 센다. */
  @Test
  void 연도_행은_그_해_칸만_센다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("2026", 271);
    accumulator.accept(dividend("1000", "100000"));
    accumulator.acceptDailyPrincipalCostSum(new BigDecimal("27100000"));
    accumulator.acceptHeldDays(days(0, 1000));
    accumulator.restrictHeldDays(729, 1000);

    assertThat(accumulator.toView().heldDayCount()).isEqualTo(271);
  }

  /** 들고 있던 날이 짧으면 1 년으로 늘린 값이 크게 흔들린다 - 90 일 문턱, 기간이 더 짧으면 기간 전체가 문턱. */
  @Test
  void 보유_짧음_문턱() {
    assertThat(DividendYieldGroupView.isShortHeld(89, 1000)).isTrue();
    assertThat(DividendYieldGroupView.isShortHeld(90, 1000)).isFalse();
    assertThat(DividendYieldGroupView.isShortHeld(28, 28)).as("한 달 기간을 다 들고 있었다").isFalse();
    assertThat(DividendYieldGroupView.isShortHeld(10, 28)).isTrue();
    assertThat(DividendYieldGroupView.isShortHeld(0, 1000))
        .as("원금 기록이 없는 행은 짧음이 아니라 값 없음")
        .isFalse();
  }

  @Test
  void 원금_기록이_없으면_연_수익률은_없다() {
    var accumulator = new StockDividendHtmxController.YieldAccumulator("종목", 365);
    accumulator.accept(dividend("1000", null));

    DividendYieldGroupView view = accumulator.toView();

    assertThat(view.annualizedYieldPct()).isNull();
    assertThat(view.heldAverageDailyPrincipalCost()).isNull();
    assertThat(view.principalCostDaySum()).isNull();
    assertThat(view.heldDayCount()).isZero();
  }

  @Test
  void 유틸_계산() {
    assertThat(
            StockYieldUtil.annualizedPctOnHeldDays(
                new BigDecimal("30000"), new BigDecimal("100000000")))
        .isEqualByComparingTo("10.9500");
    assertThat(StockYieldUtil.annualizedPctOnHeldDays(BigDecimal.ONE, BigDecimal.ZERO)).isNull();
    assertThat(StockYieldUtil.annualizedPctOnHeldDays(null, BigDecimal.TEN)).isNull();
    // 기간 전체(365 일)를 평균 100 만 원 들고 세후 10 만 원이면 기간 수익률 10% - 예전 연 환산(10% x 365 / 365)과 같다.
    assertThat(
            StockYieldUtil.annualizedPctOnHeldDays(
                new BigDecimal("100000"), new BigDecimal("365000000")))
        .isEqualByComparingTo(StockYieldUtil.annualizedPct(new BigDecimal("10"), 365));
  }

  private static DividendYieldGroupView row(String label, String annualized, boolean shortHeld) {
    return new DividendYieldGroupView(
        UUID.randomUUID(),
        label,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        1L,
        null,
        BigDecimal.ONE,
        shortHeld ? 30L : 300L,
        BigDecimal.ONE,
        annualized == null ? null : new BigDecimal(annualized),
        shortHeld,
        365L);
  }

  /** 최고 효율 카드는 순위 첫 행이되, 보유가 짧아 흔들리는 행은 건너뛴다. 모두 짧으면 첫 행. */
  @Test
  void 최고_효율은_보유가_짧은_행을_건너뛴다() {
    var shortTop = row("짧게 든 것", "80", true);
    var longSecond = row("오래 든 것", "12", false);

    assertThat(StockDividendHtmxController.bestYieldRow(List.of(shortTop, longSecond)))
        .isSameAs(longSecond);
    assertThat(StockDividendHtmxController.bestYieldRow(List.of(shortTop))).isSameAs(shortTop);
    assertThat(StockDividendHtmxController.bestYieldRow(List.of())).isNull();
  }

  private static net.luversof.web.gate.stock.domain.TradeProfit holding(
      UUID account, UUID stock, int quantity, String evaluation) {
    return new net.luversof.web.gate.stock.domain.TradeProfit(
        stock,
        "종목",
        account,
        "계좌",
        null,
        null,
        0,
        null,
        null,
        null,
        quantity,
        null,
        evaluation == null ? null : new BigDecimal(evaluation),
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
        null);
  }

  /** 현재 평가금액은 지금 들고 있는 것만 더한다 - 다 판 것(수량 0)과 평가가 없는 것은 빼서 표가 "지금 보유하지 않음" 으로 적게 한다. */
  @Test
  void 현재_평가금액은_들고_있는_것만_종목별_계좌별로_더한다() {
    UUID a1 = UUID.randomUUID();
    UUID a2 = UUID.randomUUID();
    UUID s1 = UUID.randomUUID();
    UUID s2 = UUID.randomUUID();
    var holdings =
        List.of(
            holding(a1, s1, 10, "1000"),
            holding(a2, s1, 5, "500"),
            holding(a1, s2, 0, "700"), // 다 팔았다
            holding(a2, s2, 3, null)); // 평가 없음

    var byStock =
        StockDividendHtmxController.sumCurrentValue(
            holdings, net.luversof.web.gate.stock.domain.TradeProfit::stockItemId);
    var byAccount =
        StockDividendHtmxController.sumCurrentValue(
            holdings, net.luversof.web.gate.stock.domain.TradeProfit::accountId);

    assertThat(byStock).containsOnlyKeys(s1);
    assertThat(byStock.get(s1)).isEqualByComparingTo("1500");
    assertThat(byAccount.get(a1)).isEqualByComparingTo("1000");
    assertThat(byAccount.get(a2)).isEqualByComparingTo("500");
    assertThat(StockDividendHtmxController.sumCurrentValue(null, x -> null)).isEmpty();
  }

  private static net.luversof.web.gate.stock.dto.response.TradeResponse trade(
      UUID account,
      UUID stock,
      net.luversof.web.gate.stock.constant.TradeType type,
      int quantity,
      String realizedProfit,
      String date) {
    return new net.luversof.web.gate.stock.dto.response.TradeResponse(
        UUID.randomUUID(),
        account,
        stock,
        "종목",
        type,
        quantity,
        new BigDecimal("1000"),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        new BigDecimal("1000").multiply(BigDecimal.valueOf(quantity)),
        realizedProfit == null ? null : new BigDecimal(realizedProfit),
        Instant.parse(date + "T03:00:00Z"));
  }

  /**
   * 자산현황의 누적 배당 연 수익률(사용자 요청 2026-09-28): 종목의 "날마다 들고 있던 원금의 합" 을 계좌마다 첫 거래일부터 세어 종목으로 더하고, 누적 배당 x
   * 365 / 그 합. 원금 기록이 없는 종목(배당만 있음)은 값을 내지 않는다.
   */
  @Test
  void 자산현황_누적_배당_연_수익률() {
    var zone = java.time.ZoneId.of("Asia/Seoul");
    UUID acc1 = UUID.randomUUID();
    UUID acc2 = UUID.randomUUID();
    UUID stock = UUID.randomUUID();
    UUID dividendOnly = UUID.randomUUID();
    var buy = net.luversof.web.gate.stock.constant.TradeType.BUY;
    var sell = net.luversof.web.gate.stock.constant.TradeType.SELL;
    var trades =
        List.of(
            // 계좌 1: 1/1 10 주 산 뒤 1/11 전량 매도 -> 1/1~1/10 10 일 x 10,000
            trade(acc1, stock, buy, 10, null, "2026-01-01"),
            trade(acc1, stock, sell, 10, "0", "2026-01-11"),
            // 계좌 2: 1/6 5 주 -> 오늘(1/10)까지 5 일 x 5,000
            trade(acc2, stock, buy, 5, null, "2026-01-06"));
    java.time.LocalDate today = java.time.LocalDate.of(2026, 1, 10);

    var daySums = StockDividendHtmxController.principalCostDaySumByStock(trades, today, zone);
    assertThat(daySums.get(stock)).as("100,000 + 25,000").isEqualByComparingTo("125000");

    var yields =
        StockPortfolioHtmxController.dividendAnnualizedByStockItem(
            java.util.Map.of(stock, new BigDecimal("1000"), dividendOnly, new BigDecimal("500")),
            trades,
            today,
            zone);
    // 1,000 x 365 / 125,000 = 292%
    assertThat(yields.get(stock)).isEqualByComparingTo("292.0000");
    assertThat(yields).as("원금 기록이 없는 종목은 값을 내지 않는다").doesNotContainKey(dividendOnly);
    assertThat(
            StockPortfolioHtmxController.dividendAnnualizedByStockItem(
                java.util.Map.of(), trades, today, zone))
        .isEmpty();
  }
}
