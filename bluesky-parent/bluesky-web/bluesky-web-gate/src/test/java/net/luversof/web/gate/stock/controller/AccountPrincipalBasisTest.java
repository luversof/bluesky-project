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
 * <p>실측 2026-08-23: 이 사용자의 6 개 계좌는 직접 입력값이 모두 비어 있어 합계가 632,223,826 원(전부 폴백)이다. ISA·연금저축1·연금저축2 에
 * 각각 60,000,000 / 12,000,000 / 18,000,000 을 넣으면 621,595,903 원이 되어 10,627,923 원 줄고, 그만큼 수익률이 올라간다.
 */
class AccountPrincipalBasisTest {

  /** 실측값(평가액, 평가손익, 직접 입력 원금 또는 null). */
  private record AccountRow(String name, String evaluation, String profit, String manual) {}

  private static final List<AccountRow> ACCOUNTS =
      List.of(
          new AccountRow("동양증권", "0", "0", null),
          new AccountRow("한국투자증권 위탁", "1286956465", "882200896", null),
          new AccountRow("한국투자증권 ISA", "60101700", "-6049643", "60000000"),
          new AccountRow("한국투자증권 연금저축1", "13656180", "-1492431", "12000000"),
          new AccountRow("한국투자증권 연금저축2", "17318980", "-2008989", "18000000"),
          new AccountRow("KB증권 위탁", "115248510", "-11591824", null));

  private BigDecimal holdingCost(AccountRow row) {
    return new BigDecimal(row.evaluation()).subtract(new BigDecimal(row.profit()));
  }

  @Test
  void 직접_입력한_원금이_있으면_그것을_없으면_보유원가를_쓴다() {
    assertThat(
            StockSummaryHtmxController.accountPrincipal(
                new BigDecimal("60000000"), new BigDecimal("66151343")))
        .isEqualTo(new BigDecimal("60000000"));
    assertThat(StockSummaryHtmxController.accountPrincipal(null, new BigDecimal("66151343")))
        .isEqualTo(new BigDecimal("66151343"));
    assertThat(StockSummaryHtmxController.accountPrincipal(null, null)).isEqualTo(BigDecimal.ZERO);
  }

  @Test
  void 직접_입력한_원금이_있으면_평가손익도_그_원금_대비로_낸다() {
    // 평가액 60,101,700 · 직접 입력 원금 60,000,000 -> 손익 101,700(기본값 -6,049,643 이 아니다)
    assertThat(
            StockSummaryHtmxController.accountEvaluationProfit(
                new BigDecimal("60000000"), new BigDecimal("60101700"), new BigDecimal("-6049643")))
        .isEqualTo(new BigDecimal("101700"));
    assertThat(
            StockSummaryHtmxController.accountEvaluationProfit(
                null, new BigDecimal("60101700"), new BigDecimal("-6049643")))
        .isEqualTo(new BigDecimal("-6049643"));
  }

  @Test
  void 실측_계좌로_합계가_두_기준에서_각각_맞는다() {
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

    assertThat(allFallback).as("직접 입력값이 모두 비어 있는 현재 상태").isEqualTo(new BigDecimal("632223826"));
    assertThat(withManual).as("세 계좌의 직접 입력 원금을 복구한 상태").isEqualTo(new BigDecimal("621595903"));
    assertThat(allFallback.subtract(withManual))
        .as("복구하면 원금이 이만큼 줄고 수익률이 그만큼 올라간다")
        .isEqualTo(new BigDecimal("10627923"));
  }
}
