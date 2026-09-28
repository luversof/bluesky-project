package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;

/**
 * 배당 수익률 랭킹 표의 손익 &mdash; 평가 손익(a) + 실현 손익(b) + 배당금(c) = 합산 손익(d).
 *
 * <p>사용자 요청 2026-09-28: "배당의 평가금액이 원금에 대해 수익인지 손해인지 표시를 해달라", "합산 수익률도 보여달라", "실현 손익도 있으면 보여줘야 할까?"
 * &rarr; 권장안 승인.
 *
 * <ul>
 *   <li>평가 손익 = 지금 들고 있는 것의 평가 손익(api-stock 정의, 자산현황과 같은 값). 원금 = 현재 평가금액 - 평가 손익, 평가 손익률 = 평가 손익 /
 *       원금. 지금 들고 있지 않으면 없다(null).
 *   <li>실현 손익 = 판 것에서 난 손익({@code TradeProfit.realizedProfit} - 자산현황 합산 손익과 같은 값). 판 적이 없으면 null.
 *   <li>합산 손익 = 평가 + 실현 + 이 행의 세후 배당. 다 판 종목도 값이 있다(실현 + 배당).
 *   <li>합산 수익률 = 합산 손익 / 지금까지 산 총액({@code totalBuyCost} - 매수 수수료 포함). 지금 원금으로 나누면 예전에 판 분량의 수익까지 지금
 *       원금에 얹혀 부풀고(삼성전자 +306.90% &rarr; 약 +345%), 다 판 종목은 원금이 없어 낼 수 없다.
 * </ul>
 */
public final class DividendHoldingProfitUtil {

  private DividendHoldingProfitUtil() {}

  /** 한 행(또는 합계)의 값. 금액은 원, 비율은 %(소수 둘째). 없는 값은 null. */
  public record HoldingProfit(
      BigDecimal currentValue,
      BigDecimal principal,
      BigDecimal evaluationProfit,
      BigDecimal evaluationProfitPct,
      BigDecimal realizedProfit,
      BigDecimal dividend,
      BigDecimal totalBuyCost,
      BigDecimal combinedProfit,
      BigDecimal combinedProfitPct) {

    /** 지금 들고 있는가(평가 손익이 있는가). */
    public boolean held() {
      return currentValue != null;
    }
  }

  /**
   * 한 행.
   *
   * @param currentValue 현재 평가금액(지금 안 들었으면 null 또는 0)
   * @param evaluationProfit 평가 손익
   * @param realizedProfit 실현 손익(판 적이 없으면 null 또는 0)
   * @param netDividend 이 행의 세후 배당
   * @param totalBuyCost 지금까지 산 총액(매수 기록이 없으면 null)
   */
  public static HoldingProfit of(
      BigDecimal currentValue,
      BigDecimal evaluationProfit,
      BigDecimal realizedProfit,
      BigDecimal netDividend,
      BigDecimal totalBuyCost) {
    boolean held = currentValue != null && currentValue.signum() > 0;
    BigDecimal evaluation = held ? nz(evaluationProfit) : null;
    BigDecimal principal = held ? currentValue.subtract(evaluation) : null;
    BigDecimal realized =
        realizedProfit != null && realizedProfit.signum() != 0 ? realizedProfit : null;
    BigDecimal dividend = nz(netDividend);
    BigDecimal buy = totalBuyCost != null && totalBuyCost.signum() > 0 ? totalBuyCost : null;
    BigDecimal combined = nz(evaluation).add(nz(realized)).add(dividend);
    return new HoldingProfit(
        held ? currentValue : null,
        principal,
        evaluation,
        held ? pct(evaluation, principal) : null,
        realized,
        dividend,
        buy,
        combined,
        buy != null ? pct(combined, buy) : null);
  }

  /** 행의 값. 지도에 없는 값은 없는 것으로 본다. */
  public static HoldingProfit ofRow(
      DividendYieldGroupView row,
      Map<UUID, BigDecimal> currentValues,
      Map<UUID, BigDecimal> evaluationProfits,
      Map<UUID, BigDecimal> realizedProfits,
      Map<UUID, BigDecimal> totalBuyCosts) {
    if (row == null || row.groupId() == null) {
      return null;
    }
    UUID id = row.groupId();
    return of(
        get(currentValues, id),
        get(evaluationProfits, id),
        get(realizedProfits, id),
        row.totalNetAmount(),
        get(totalBuyCosts, id));
  }

  /**
   * 합계행 = 행들의 합. 평가금액 · 평가 손익 · 원금은 지금 들고 있는 행만, 실현 · 배당 · 산 총액은 모든 행. 합계 줄에서도 "평가 + 실현 + 배당 = 합산"
   * 이 화면 그대로 맞는다(사용자 2026-09-28 "a + b = c 로 받아들여지지"). 행이 없으면 null.
   */
  public static HoldingProfit total(
      List<DividendYieldGroupView> rows,
      Map<UUID, BigDecimal> currentValues,
      Map<UUID, BigDecimal> evaluationProfits,
      Map<UUID, BigDecimal> realizedProfits,
      Map<UUID, BigDecimal> totalBuyCosts) {
    if (rows == null || rows.isEmpty()) {
      return null;
    }
    BigDecimal value = BigDecimal.ZERO;
    BigDecimal evaluation = BigDecimal.ZERO;
    BigDecimal realized = BigDecimal.ZERO;
    BigDecimal dividend = BigDecimal.ZERO;
    BigDecimal buy = BigDecimal.ZERO;
    for (DividendYieldGroupView row : rows) {
      HoldingProfit one =
          ofRow(row, currentValues, evaluationProfits, realizedProfits, totalBuyCosts);
      if (one == null) {
        continue;
      }
      if (one.held()) {
        value = value.add(one.currentValue());
        evaluation = evaluation.add(one.evaluationProfit());
      }
      realized = realized.add(nz(one.realizedProfit()));
      dividend = dividend.add(one.dividend());
      buy = buy.add(nz(one.totalBuyCost()));
    }
    return of(
        value.signum() > 0 ? value : null,
        value.signum() > 0 ? evaluation : null,
        realized,
        dividend,
        buy);
  }

  private static BigDecimal get(Map<UUID, BigDecimal> map, UUID id) {
    return map == null ? null : map.get(id);
  }

  private static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static BigDecimal pct(BigDecimal amount, BigDecimal base) {
    if (base == null || base.signum() <= 0) {
      return null;
    }
    return amount.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
  }
}
