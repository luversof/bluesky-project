package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 자산 현황 표의 <b>파생 수치</b>(수익률 · 비중 · 원금 회수 · 합계).
 *
 * <p>이 계산은 {@code assetStatus.jte} 안에 인라인 Java 로 들어 있었다 &mdash; 계좌 행 8 개 · 계좌 합계 7 개 · 종목 행 2 개 ·
 * 종목 합계 1 개. 템플릿의 계산은 렌더 검사로만 닿을 수 있어, 예컨대 "원금이 0 이면 수익률을 0 으로 둔다" 같은 규칙이 지켜지는지 값으로 못 박을 곳이 없었다. 그
 * 템플릿은 이 모듈에서 가장 크다(1,337 줄, 인라인 블록 25 개).
 *
 * <p>계산 규칙은 템플릿에 있던 그대로다. 옮기면서 값이 바뀌지 않았음은 리팩터 전후의 렌더 HTML 을 통째로 견주어 확인했다(2026-09-08, 계좌 15 · 종목 14
 * 행). 눈에 띄는 비대칭 둘은 <b>일부러 그대로</b> 두었다 &mdash; 계좌의 수익률은 double 나눗셈이고 종목의 수익률은 소수 4 자리 반올림이며, 계좌 합계의
 * 원금은 원금 맵에 있는 것만 더한다(행은 원금이 없으면 매수금액으로 대신하지만 합계는 그러지 않는다).
 */
public final class StockAssetStatusUtil {

  private StockAssetStatusUtil() {}

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static boolean positive(BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) > 0;
  }

  /** 분모가 0 이하이면 0. 비율을 double 로 낸다(계좌 행·합계의 규칙). */
  private static double ratePct(BigDecimal numerator, BigDecimal denominator) {
    return positive(denominator) ? numerator.doubleValue() / denominator.doubleValue() * 100 : 0.0;
  }

  /** 분모가 0 이하이거나 분자가 없으면 0. 소수 4 자리 반올림 뒤 100 배(종목 행의 규칙). */
  private static BigDecimal ratePctScaled(BigDecimal numerator, BigDecimal denominator) {
    if (!positive(denominator) || numerator == null) {
      return BigDecimal.ZERO;
    }
    return numerator.divide(denominator, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
  }

  /**
   * 계좌 한 줄.
   *
   * @param principal 원금 &mdash; 계좌 설정의 수동 원금이 있으면 그것, 없으면 매수금액
   * @param principalReturn 평가액 − 원금
   */
  public record AccountRow(
      BigDecimal buyAmount,
      BigDecimal evaluationAmount,
      BigDecimal weightPct,
      BigDecimal evaluationProfit,
      double evaluationProfitRate,
      BigDecimal principal,
      BigDecimal principalReturn,
      double principalReturnRate) {}

  /**
   * @param profitBasis 원금 맵의 값. 없으면(null) 매수금액이 원금이다.
   * @param totalEvaluationAmount 전 계좌 평가액 &mdash; 비중의 분모. 0 이하이면 비중 0.
   */
  public static AccountRow accountRow(
      TradeProfit account, BigDecimal profitBasis, BigDecimal totalEvaluationAmount) {
    BigDecimal buyAmount = nz(account.totalBuyCost());
    BigDecimal evaluationAmount = nz(account.evaluationAmount());
    BigDecimal weightPct = ratePctScaled(evaluationAmount, totalEvaluationAmount);
    BigDecimal evaluationProfit = nz(account.evaluationProfit());
    BigDecimal principal = profitBasis != null ? profitBasis : buyAmount;
    BigDecimal principalReturn = evaluationAmount.subtract(principal);
    return new AccountRow(
        buyAmount,
        evaluationAmount,
        weightPct,
        evaluationProfit,
        ratePct(evaluationProfit, buyAmount),
        principal,
        principalReturn,
        ratePct(principalReturn, principal));
  }

  /** 계좌 표의 합계 줄. */
  public record AccountTotals(
      BigDecimal buyAmount,
      BigDecimal evaluationAmount,
      BigDecimal principal,
      BigDecimal evaluationProfit,
      double evaluationProfitRate,
      BigDecimal principalReturn,
      double principalReturnRate) {}

  public static AccountTotals accountTotals(
      Map<UUID, TradeProfit> accountTotalMap, Map<UUID, BigDecimal> accountProfitBasisMap) {
    BigDecimal buyAmount = BigDecimal.ZERO;
    BigDecimal evaluationAmount = BigDecimal.ZERO;
    BigDecimal evaluationProfit = BigDecimal.ZERO;
    if (accountTotalMap != null) {
      for (TradeProfit account : accountTotalMap.values()) {
        buyAmount = buyAmount.add(nz(account.totalBuyCost()));
        evaluationAmount = evaluationAmount.add(nz(account.evaluationAmount()));
        evaluationProfit = evaluationProfit.add(nz(account.evaluationProfit()));
      }
    }
    BigDecimal principal = BigDecimal.ZERO;
    if (accountProfitBasisMap != null) {
      for (BigDecimal value : accountProfitBasisMap.values()) {
        principal = principal.add(nz(value));
      }
    }
    BigDecimal principalReturn = evaluationAmount.subtract(principal);
    return new AccountTotals(
        buyAmount,
        evaluationAmount,
        principal,
        evaluationProfit,
        ratePct(evaluationProfit, buyAmount),
        principalReturn,
        ratePct(principalReturn, principal));
  }

  /** 종목 한 줄의 비율 둘. 둘 다 소수 4 자리 반올림 뒤 100 배(화면은 소수 1 자리로 보여 준다). */
  public record StockRow(BigDecimal evaluationProfitRatePct, BigDecimal totalWeightPct) {}

  public static StockRow stockRow(TradeProfit item, BigDecimal totalEvaluationAmount) {
    return new StockRow(
        ratePctScaled(item.evaluationProfit(), item.totalBuyCost()),
        ratePctScaled(item.evaluationAmount(), totalEvaluationAmount));
  }

  /** 종목 표 합계 줄의 매수금액. */
  public static BigDecimal totalBuyCost(List<TradeProfit> stockAggregated) {
    BigDecimal total = BigDecimal.ZERO;
    if (stockAggregated != null) {
      for (TradeProfit item : stockAggregated) {
        total = total.add(nz(item.totalBuyCost()));
      }
    }
    return total;
  }
}
