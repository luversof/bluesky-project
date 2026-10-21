package net.luversof.web.gate.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

/**
 * 목표 월배당까지 &mdash; 지금 월배당에서 목표까지 얼마를 더 사야 하고, 매달 적립하며 분배금을 다시 사면 몇 달 걸리는지(2026-10-20 자율 개선).
 *
 * <p>시뮬레이터에는 복리(연 납입 · 연 수익률) · 배당 생활(지출 · 소진)은 있었지만 "월 얼마를 받으려면" 을 거꾸로 푸는 곳이 없었다.
 *
 * <p>기준은 등록된 월배당 <b>전 종목</b>(표 거르기와 무관 - 목표는 포트폴리오 전체의 것)이고, 금액은 화면 머리와 같은 <b>최근 주당 분배금</b> x
 * <b>원장의 지금 수량</b>(원장을 못 읽은 종목은 스냅샷 수량)이다. 분배율 = 월배당 x 12 &divide; 평가액. 가정: 가격 · 분배율 그대로, 분배금은 세전
 * 그대로 다시 산다 &mdash; 화면이 그렇게 적는다.
 */
public final class MonthlyDividendGoalCalculator {

  /** 이만큼 걸려도 못 닿으면 '닿지 않음' 으로 본다(100 년). */
  public static final int MAX_MONTHS = 1200;

  private MonthlyDividendGoalCalculator() {}

  /**
   * 지금 포트폴리오의 월배당과 평가액.
   *
   * @param monthlyDividend 최근 주당 분배금 x 지금 수량의 합(세전)
   * @param marketValue 현재가 x 지금 수량의 합
   * @param rowCount 셈에 넣은 종목 수(주당 분배금 · 현재가 · 수량이 다 있는 것)
   */
  public record Basis(BigDecimal monthlyDividend, BigDecimal marketValue, int rowCount) {

    /** 연 분배율(소수, 0.12 = 12%). 평가액이 0 이면 0. */
    public BigDecimal annualYield() {
      if (marketValue == null || marketValue.signum() <= 0 || monthlyDividend == null) {
        return BigDecimal.ZERO;
      }
      return monthlyDividend
          .multiply(BigDecimal.valueOf(12))
          .divide(marketValue, 10, RoundingMode.HALF_UP);
    }

    /** 연 분배율(%), 소수 둘째 자리. */
    public BigDecimal annualYieldPct() {
      return annualYield().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
  }

  /**
   * 목표 계산 결과.
   *
   * @param basis 지금 포트폴리오
   * @param goalMonthly 목표 월배당(세전)
   * @param monthlyContribution 매달 더 사는 금액
   * @param shortfall 목표 - 지금(이미 넘었으면 0)
   * @param additionalCapital 같은 분배율로 한 번에 더 사야 할 금액(원 올림). 분배율이 0 이면 null
   * @param monthsToGoal 매달 적립 + 분배금 재투자로 닿는 달 수(0 = 이미). 분배율이 0 이거나 {@value #MAX_MONTHS} 달 안에 못 닿으면
   *     null
   * @param capitalAtGoal 닿는 달의 평가액(원 반올림), 못 닿으면 null
   * @param contributedAtGoal 그때까지 넣은 적립금 합
   */
  public record Goal(
      Basis basis,
      BigDecimal goalMonthly,
      BigDecimal monthlyContribution,
      BigDecimal shortfall,
      BigDecimal additionalCapital,
      Integer monthsToGoal,
      BigDecimal capitalAtGoal,
      BigDecimal contributedAtGoal) {

    public boolean reached() {
      return shortfall.signum() == 0;
    }

    /** 닿는 데 걸리는 해(달 수 / 12). */
    public int years() {
      return monthsToGoal == null ? 0 : monthsToGoal / 12;
    }

    /** 해를 뺀 나머지 달. */
    public int remainderMonths() {
      return monthsToGoal == null ? 0 : monthsToGoal % 12;
    }
  }

  /** 전 종목의 지금 월배당 · 평가액. 수량은 원장 지금 수량, 없으면 스냅샷 수량. */
  public static Basis basisOf(
      List<MonthlyDividendSnapshotResponse> rows, Map<UUID, Integer> currentQuantities) {
    BigDecimal monthly = BigDecimal.ZERO;
    BigDecimal value = BigDecimal.ZERO;
    int count = 0;
    if (rows != null) {
      for (MonthlyDividendSnapshotResponse row : rows) {
        if (row == null
            || row.latestMonthlyDividendPerShare() == null
            || row.currentPrice() == null
            || row.currentPrice().signum() <= 0) {
          continue;
        }
        Integer quantity =
            currentQuantities != null && row.stockItemId() != null
                ? currentQuantities.getOrDefault(row.stockItemId(), row.heldQuantity())
                : row.heldQuantity();
        if (quantity == null || quantity <= 0) {
          continue;
        }
        BigDecimal shares = BigDecimal.valueOf(quantity);
        monthly = monthly.add(row.latestMonthlyDividendPerShare().multiply(shares));
        value = value.add(row.currentPrice().multiply(shares));
        count++;
      }
    }
    return new Basis(monthly, value, count);
  }

  /**
   * 목표까지의 금액과 기간.
   *
   * <p>기간은 달마다 평가액 V 에 대해 {@code 이번 달 분배금 = V x 분배율 / 12} 가 목표 이상이 되는 첫 달이다. 닿지 않았으면 {@code V += 적립
   * + 분배금} 하고 다음 달로 간다(분배율 · 가격은 그대로).
   */
  public static Goal compute(Basis basis, BigDecimal goalMonthly, BigDecimal monthlyContribution) {
    BigDecimal goal = goalMonthly == null ? BigDecimal.ZERO : goalMonthly.max(BigDecimal.ZERO);
    BigDecimal contribution =
        monthlyContribution == null ? BigDecimal.ZERO : monthlyContribution.max(BigDecimal.ZERO);
    BigDecimal shortfall = goal.subtract(basis.monthlyDividend()).max(BigDecimal.ZERO);
    BigDecimal yield = basis.annualYield();
    if (yield.signum() <= 0) {
      return new Goal(basis, goal, contribution, shortfall, null, null, null, null);
    }
    BigDecimal additional =
        shortfall.multiply(BigDecimal.valueOf(12)).divide(yield, 0, RoundingMode.CEILING);
    if (shortfall.signum() == 0) {
      // 이미 넘었다 - 분배율을 소수로 끊은 반올림 탓에 0 달째 비교가 한 끗 모자라 1 달로 나오지 않게 바로 끝낸다.
      return new Goal(
          basis,
          goal,
          contribution,
          shortfall,
          additional,
          0,
          basis.marketValue().setScale(0, RoundingMode.HALF_UP),
          BigDecimal.ZERO);
    }
    BigDecimal monthlyRate = yield.divide(BigDecimal.valueOf(12), 12, RoundingMode.HALF_UP);
    BigDecimal value = basis.marketValue();
    BigDecimal contributed = BigDecimal.ZERO;
    for (int month = 0; month <= MAX_MONTHS; month++) {
      BigDecimal dividend = value.multiply(monthlyRate);
      if (dividend.compareTo(goal) >= 0) {
        return new Goal(
            basis,
            goal,
            contribution,
            shortfall,
            additional,
            month,
            value.setScale(0, RoundingMode.HALF_UP),
            contributed);
      }
      value = value.add(contribution).add(dividend);
      contributed = contributed.add(contribution);
    }
    return new Goal(basis, goal, contribution, shortfall, additional, null, null, null);
  }
}
