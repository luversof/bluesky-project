package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 요약 화면의 "원금"이 계좌마다 어떤 기준을 쓰는지 고정한다.
 *
 * <p>직접 입력한 원금이 있으면 그것을, 없으면 보유원가(평가액 - 평가손익)를 쓴다. 즉 <b>한 화면의 합계에 두 기준이 섞일 수 있다</b>. 화면은 그 차이를 "수동
 * 원금 반영" 줄로 밝히는데, 그 줄을 만드는 계산 자체는 지금까지 렌더 테스트가 값을 넣어 주는 방식이라 검증되지 않았다.
 *
 * <p>실측 2026-08-23 의 계좌 구성(6 개 계좌 · 비과세 3 계좌만 직접 입력 원금 대상)을 그대로 본뜬다. 금액은 실제 값이 아니라 같은 모양의 표본이다
 * &mdash; 폴백만 쓰던 상태에서 세 계좌의 직접 입력 원금을 넣으면 합계가 줄고 그만큼 수익률이 올라간다는 관계가 이 검사의 대상이다.
 */
class AccountPrincipalBasisTest {

  /** 표본(평가액, 평가손익, 직접 입력 원금 또는 null). 금액은 실제 값이 아니라 같은 모양으로 만든 것이다. */
  private record AccountRow(String name, String evaluation, String profit, String manual) {}

  private static final List<AccountRow> ACCOUNTS =
      List.of(
          new AccountRow("동양증권", "0", "0", null),
          new AccountRow("한국투자증권 위탁", "1200000000", "800000000", null),
          // 직접 입력 원금이 보유원가보다 작은 경우
          new AccountRow("한국투자증권 ISA", "60000000", "-6000000", "50000000"),
          new AccountRow("한국투자증권 연금저축1", "13000000", "-1000000", "10000000"),
          // 반대로 직접 입력 원금이 보유원가보다 큰 경우
          new AccountRow("한국투자증권 연금저축2", "17000000", "-2000000", "20000000"),
          new AccountRow("KB증권 위탁", "115000000", "-11000000", null));

  private BigDecimal holdingCost(AccountRow row) {
    return new BigDecimal(row.evaluation()).subtract(new BigDecimal(row.profit()));
  }

  @Test
  void 직접_입력한_원금이_있으면_그것을_없으면_보유원가를_쓴다() {
    assertThat(
            StockSummaryHtmxController.accountPrincipal(
                new BigDecimal("50000000"), new BigDecimal("66000000")))
        .isEqualTo(new BigDecimal("50000000"));
    assertThat(StockSummaryHtmxController.accountPrincipal(null, new BigDecimal("66000000")))
        .isEqualTo(new BigDecimal("66000000"));
    assertThat(StockSummaryHtmxController.accountPrincipal(null, null)).isEqualTo(BigDecimal.ZERO);
  }

  @Test
  void 직접_입력한_원금이_있으면_평가손익도_그_원금_대비로_낸다() {
    // 평가액 60,000,000 · 직접 입력 원금 50,000,000 -> 손익 10,000,000(기본값 -6,000,000 이 아니다)
    assertThat(
            StockSummaryHtmxController.accountEvaluationProfit(
                new BigDecimal("50000000"), new BigDecimal("60000000"), new BigDecimal("-6000000")))
        .isEqualTo(new BigDecimal("10000000"));
    assertThat(
            StockSummaryHtmxController.accountEvaluationProfit(
                null, new BigDecimal("60000000"), new BigDecimal("-6000000")))
        .isEqualTo(new BigDecimal("-6000000"));
  }

  @Test
  void 여섯_계좌_구성으로_합계가_두_기준에서_각각_맞는다() {
    BigDecimal allFallback = BigDecimal.ZERO;
    BigDecimal withManual = BigDecimal.ZERO;
    for (AccountRow row : ACCOUNTS) {
      allFallback =
          allFallback.add(StockSummaryHtmxController.accountPrincipal(null, holdingCost(row)));
      withManual =
          withManual.add(
              StockSummaryHtmxController.accountPrincipal(
                  row.manual() == null ? null : new BigDecimal(row.manual()), holdingCost(row)));
    }

    assertThat(allFallback).as("직접 입력값이 모두 비어 있는 상태").isEqualTo(new BigDecimal("625000000"));
    assertThat(withManual).as("세 계좌의 직접 입력 원금을 넣은 상태").isEqualTo(new BigDecimal("606000000"));
    assertThat(allFallback.subtract(withManual))
        .as("넣으면 원금이 이만큼 줄고 수익률이 그만큼 올라간다")
        .isEqualTo(new BigDecimal("19000000"));
  }
}
