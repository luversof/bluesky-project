package net.luversof.web.gate.stock.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * List&lt;TradeProfit&gt;의 필드별 합계를 한 번에 계산하는 유틸리티. 컨트롤러 곳곳에 흩어진 동일한
 * stream-reduce 패턴을 제거한다.
 */
public final class TradeProfitAggregator {

  private TradeProfitAggregator() {
  }

  /** 집계 결과를 담는 값 객체. 합산 필드 외에 평균 단가 계산 편의 메서드를 제공한다. */
  public record Sums(
      int holdingQuantity,
      int totalSellQuantity,
      BigDecimal totalBuyAmount,
      BigDecimal totalSellAmount,
      BigDecimal realizedProfit,
      BigDecimal evaluationAmount,
      BigDecimal evaluationProfit,
      BigDecimal totalProfit,
      BigDecimal totalBuyFee,
      BigDecimal totalSellFee,
      BigDecimal totalSellTax,
      BigDecimal totalBuyCost,
      BigDecimal totalSellProceeds,
      BigDecimal realizedProfitNet,
      BigDecimal evaluationProfitNet,
      BigDecimal totalProfitNet,
      BigDecimal currentHoldingBuyAmount,
      BigDecimal currentHoldingBuyCost) {

    /** 현재 보유분 기준 가중 평균 매수 단가 (수수료 제외) */
    public BigDecimal avgBuyPrice() {
      return holdingQuantity > 0
          ? currentHoldingBuyAmount.divide(
              BigDecimal.valueOf(holdingQuantity), 2, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;
    }

    /** 매도 평균 단가 (수수료 제외) */
    public BigDecimal avgSellPrice() {
      return totalSellQuantity > 0
          ? totalSellAmount.divide(BigDecimal.valueOf(totalSellQuantity), 0, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;
    }

    /** 현재 보유분 기준 가중 평균 매수 단가 (수수료/세금 반영) */
    public BigDecimal avgBuyPriceNet() {
      return holdingQuantity > 0
          ? currentHoldingBuyCost.divide(
              BigDecimal.valueOf(holdingQuantity), 2, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;
    }

    /** 매도 평균 단가 (수수료/세금 반영) */
    public BigDecimal avgSellPriceNet() {
      return totalSellQuantity > 0
          ? totalSellProceeds.divide(BigDecimal.valueOf(totalSellQuantity), 0, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;
    }
  }

  /** List&lt;TradeProfit&gt;의 모든 집계 필드를 합산한다. */
  public static Sums aggregate(List<TradeProfit> list) {
    return new Sums(
        list.stream().mapToInt(TradeProfit::holdingQuantity).sum(),
        list.stream().mapToInt(TradeProfit::totalSellQuantity).sum(),
        sum(list, TradeProfit::totalBuyAmount),
        sum(list, TradeProfit::totalSellAmount),
        sum(list, TradeProfit::realizedProfit),
        sum(list, TradeProfit::evaluationAmount),
        sum(list, TradeProfit::evaluationProfit),
        sum(list, TradeProfit::totalProfit),
        sum(list, TradeProfit::totalBuyFee),
        sum(list, TradeProfit::totalSellFee),
        sum(list, TradeProfit::totalSellTax),
        sum(list, TradeProfit::totalBuyCost),
        sum(list, TradeProfit::totalSellProceeds),
        sum(list, TradeProfit::realizedProfitNet),
        sum(list, TradeProfit::evaluationProfitNet),
        sum(list, TradeProfit::totalProfitNet),
        sumHoldingBasis(list, TradeProfit::averageBuyPrice, TradeProfit::totalBuyAmount),
        sumHoldingBasis(list, TradeProfit::averageBuyPriceNet, TradeProfit::totalBuyCost));
  }

  private static BigDecimal sumHoldingBasis(
      List<TradeProfit> list,
      Function<TradeProfit, BigDecimal> averageGetter,
      Function<TradeProfit, BigDecimal> fallbackAmountGetter) {
    return list.stream()
        .filter(Objects::nonNull)
        .map(
            tradeProfit -> {
              if (tradeProfit.holdingQuantity() <= 0) {
                return BigDecimal.ZERO;
              }

              BigDecimal average = averageGetter.apply(tradeProfit);
              if (average != null) {
                return average.multiply(BigDecimal.valueOf(tradeProfit.holdingQuantity()));
              }

              BigDecimal fallbackAmount = fallbackAmountGetter.apply(tradeProfit);
              return fallbackAmount != null ? fallbackAmount : BigDecimal.ZERO;
            })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal sum(List<TradeProfit> list, Function<TradeProfit, BigDecimal> getter) {
    return list.stream()
        .map(getter)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
