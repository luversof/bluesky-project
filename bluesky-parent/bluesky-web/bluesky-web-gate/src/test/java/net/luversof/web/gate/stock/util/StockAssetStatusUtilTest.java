package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 자산 현황 표의 파생 수치.
 *
 * <p>이 계산은 템플릿 안에 인라인 Java 로 있어서 값으로 못 박을 곳이 없었다. 여기 있는 규칙은 템플릿에 있던 그대로이고, 옮기면서 값이 바뀌지 않았음은 리팩터 전후의
 * 렌더 HTML 을 통째로 견주어 확인했다(2026-09-08).
 */
class StockAssetStatusUtilTest {

  private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private static TradeProfit account(String evalAmount, String evalProfit, String buyCost) {
    return TradeProfit.ofAccountStatus(
        "계좌", bd(evalAmount), bd(evalProfit), BigDecimal.ZERO, bd(buyCost), BigDecimal.ZERO);
  }

  private static TradeProfit stock(String evalAmount, String evalProfit, String buyCost) {
    return TradeProfit.ofStockStatus(
        A, "종목", null, 0, null, bd(evalAmount), bd(evalProfit), null, bd(buyCost));
  }

  // ---------------------------------------------------------------- 계좌 행

  /** 평가 1,200 / 매수 1,000 → 평가손익 200 (20%). 원금이 따로 없으면 매수금액이 원금이라 회수도 200 (20%). */
  @Test
  void 계좌_행은_수익률과_원금_회수를_낸다() {
    var row = StockAssetStatusUtil.accountRow(account("1200", "200", "1000"), null, bd("4800"));

    assertThat(row.buyAmount()).isEqualByComparingTo("1000");
    assertThat(row.evaluationProfitRate()).isCloseTo(20.0, within(1e-9));
    assertThat(row.principal()).as("원금 맵에 없으면 매수금액이 원금이다").isEqualByComparingTo("1000");
    assertThat(row.principalReturn()).isEqualByComparingTo("200");
    assertThat(row.principalReturnRate()).isCloseTo(20.0, within(1e-9));
    assertThat(row.weightPct()).as("1,200 / 4,800 = 25%").isEqualByComparingTo("25.00");
  }

  /** 수동 원금이 있으면 회수는 그것 기준이다. 원금 800 이면 1,200 − 800 = 400 (50%). 평가손익 쪽은 매수금액 기준 그대로다. */
  @Test
  void 수동_원금이_있으면_회수는_그_원금_기준이다() {
    var row =
        StockAssetStatusUtil.accountRow(account("1200", "200", "1000"), bd("800"), bd("4800"));

    assertThat(row.principal()).isEqualByComparingTo("800");
    assertThat(row.principalReturn()).isEqualByComparingTo("400");
    assertThat(row.principalReturnRate()).isCloseTo(50.0, within(1e-9));
    assertThat(row.evaluationProfitRate())
        .as("평가손익률은 원금이 아니라 매수금액 기준")
        .isCloseTo(20.0, within(1e-9));
  }

  /** 분모가 0 이면 비율은 0 이다. 0 으로 나누면 렌더가 통째로 죽는다. */
  @Test
  void 매수금액과_전체_평가액이_0_이면_비율은_0_이다() {
    var row = StockAssetStatusUtil.accountRow(account("0", "0", "0"), null, BigDecimal.ZERO);

    assertThat(row.evaluationProfitRate()).isZero();
    assertThat(row.principalReturnRate()).isZero();
    assertThat(row.weightPct()).isEqualByComparingTo("0");
  }

  /** 계좌 행의 값이 null 이어도 0 으로 본다. 집계에 빈 계좌가 섞여 온다. */
  @Test
  void 계좌_행의_null_값은_0_이다() {
    var row =
        StockAssetStatusUtil.accountRow(
            TradeProfit.ofAccountStatus("빈", null, null, null, null, null), null, bd("100"));

    assertThat(row.buyAmount()).isEqualByComparingTo("0");
    assertThat(row.evaluationAmount()).isEqualByComparingTo("0");
    assertThat(row.principalReturn()).isEqualByComparingTo("0");
  }

  // ---------------------------------------------------------------- 계좌 합계

  @Test
  void 계좌_합계는_행을_더하고_합계로_비율을_낸다() {
    Map<UUID, TradeProfit> accounts = new LinkedHashMap<>();
    accounts.put(A, account("1200", "200", "1000"));
    accounts.put(B, account("3600", "-400", "4000"));
    Map<UUID, BigDecimal> principals = new LinkedHashMap<>();
    principals.put(A, bd("800"));
    principals.put(B, bd("4000"));

    var totals = StockAssetStatusUtil.accountTotals(accounts, principals);

    assertThat(totals.buyAmount()).isEqualByComparingTo("5000");
    assertThat(totals.evaluationAmount()).isEqualByComparingTo("4800");
    assertThat(totals.evaluationProfit()).isEqualByComparingTo("-200");
    assertThat(totals.evaluationProfitRate()).as("-200 / 5,000").isCloseTo(-4.0, within(1e-9));
    assertThat(totals.principal()).isEqualByComparingTo("4800");
    assertThat(totals.principalReturn()).isEqualByComparingTo("0");
    assertThat(totals.principalReturnRate()).isZero();
  }

  /**
   * 합계의 원금은 <b>원금 맵에 있는 것만</b> 더한다 &mdash; 행처럼 매수금액으로 대신하지 않는다.
   *
   * <p>템플릿에 있던 규칙 그대로다. 컨트롤러는 보유가 있는 계좌마다 원금을 넣으므로 실제로는 어긋나지 않지만, 규칙 자체는 여기 못 박아 둔다.
   */
  @Test
  void 합계의_원금은_원금_맵만_더한다() {
    Map<UUID, TradeProfit> accounts = new LinkedHashMap<>();
    accounts.put(A, account("1200", "200", "1000"));
    accounts.put(B, account("3600", "-400", "4000"));

    var totals = StockAssetStatusUtil.accountTotals(accounts, Map.of(A, bd("800")));

    assertThat(totals.principal()).isEqualByComparingTo("800");
    assertThat(totals.principalReturn()).as("4,800 − 800").isEqualByComparingTo("4000");
  }

  @Test
  void 계좌가_없으면_합계는_전부_0_이다() {
    var totals = StockAssetStatusUtil.accountTotals(null, null);

    assertThat(totals.buyAmount()).isEqualByComparingTo("0");
    assertThat(totals.evaluationProfitRate()).isZero();
    assertThat(totals.principalReturnRate()).isZero();
  }

  // ---------------------------------------------------------------- 종목 행

  /** 종목의 비율은 소수 4 자리로 반올림한 뒤 100 배다 &mdash; 1/3 은 33.33 이지 33.333… 이 아니다. */
  @Test
  void 종목_행의_비율은_소수_4자리_반올림이다() {
    var row = StockAssetStatusUtil.stockRow(stock("1000", "1000", "3000"), bd("3000"));

    assertThat(row.evaluationProfitRatePct()).isEqualByComparingTo("33.33");
    assertThat(row.totalWeightPct()).isEqualByComparingTo("33.33");
  }

  @Test
  void 종목_행의_분모가_0_이거나_분자가_없으면_0_이다() {
    assertThat(StockAssetStatusUtil.stockRow(stock("1000", "1000", "0"), bd("0")))
        .isEqualTo(new StockAssetStatusUtil.StockRow(BigDecimal.ZERO, BigDecimal.ZERO));
    var noProfit = TradeProfit.ofStockStatus(A, "종목", null, 0, null, null, null, null, bd("3000"));
    assertThat(StockAssetStatusUtil.stockRow(noProfit, bd("3000")))
        .isEqualTo(new StockAssetStatusUtil.StockRow(BigDecimal.ZERO, BigDecimal.ZERO));
  }

  // ---------------------------------------------------------------- 종목 합계

  @Test
  void 종목_합계의_매수금액은_null_을_0_으로_보고_더한다() {
    var items =
        List.of(
            stock("1", "1", "1000"),
            TradeProfit.ofStockStatus(B, "없음", null, 0, null, null, null, null, null),
            stock("1", "1", "2500"));

    assertThat(StockAssetStatusUtil.totalBuyCost(items)).isEqualByComparingTo("3500");
    assertThat(StockAssetStatusUtil.totalBuyCost(null)).isEqualByComparingTo("0");
  }
}
