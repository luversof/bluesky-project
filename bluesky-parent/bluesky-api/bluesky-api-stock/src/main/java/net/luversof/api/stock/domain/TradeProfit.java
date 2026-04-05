package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class TradeProfit {

  private UUID stockItemId;
  private UUID accountId;

  // 매수 관련 정보 (총액/평단 - 수수료 제외: 기존 호환 유지)
  private BigDecimal totalBuyAmount;
  private BigDecimal averageBuyPrice;

  // 매도 관련 정보 (실현 손익 - 수수료/거래세 제외: 기존 호환 유지)
  private int totalSellQuantity;
  private BigDecimal averageSellPrice;
  private BigDecimal totalSellAmount;
  private BigDecimal realizedProfit;

  // 보유 관련 정보 (미실현 손익 - 수수료 제외: 기존 호환 유지)
  private int holdingQuantity;
  private BigDecimal currentPrice;
  private BigDecimal evaluationAmount;
  private BigDecimal evaluationProfit;

  // 총 손익 (실현 + 미실현) - 수수료 제외: 기존 호환 유지
  private BigDecimal totalProfit;

  // -----------------------------
  // 수수료/거래세 포함한 실제 손익 계산을 위한 추가 필드
  // -----------------------------
  // 합계 수수료/거래세
  private BigDecimal totalBuyFee; // BUY 거래 수수료 합계
  private BigDecimal totalSellFee; // SELL 거래 수수료 합계
  private BigDecimal totalSellTax; // SELL 거래세 합계

  // 순수 매수/매도 금액 (수수료/세금 반영)
  // 매수 총원가 = 총매수액 + 매수수수료
  private BigDecimal totalBuyCost;
  // 매도 총수령액 = 총매도액 - 매도수수료 - 거래세
  private BigDecimal totalSellProceeds;

  // 수수료/세금 반영 평단
  private BigDecimal averageBuyPriceNet; // (총매수액+수수료)/수량
  private BigDecimal averageSellPriceNet; // (총매도액-수수료-세금)/수량

  // 실현/미실현/총 손익 (수수료/세금 반영)
  private BigDecimal realizedProfitNet; // 총수령액 - 총원가 (간이 방식)
  private BigDecimal evaluationProfitNet; // 평가액 - (평단NET*보유수량)
  private BigDecimal totalProfitNet; // realizedNet + evaluationNet

  public UUID getStockItemId() {
    return stockItemId;
  }

  public void setStockItemId(UUID stockItemId) {
    this.stockItemId = stockItemId;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public BigDecimal getTotalBuyAmount() {
    return totalBuyAmount;
  }

  public void setTotalBuyAmount(BigDecimal totalBuyAmount) {
    this.totalBuyAmount = totalBuyAmount;
  }

  public BigDecimal getAverageBuyPrice() {
    return averageBuyPrice;
  }

  public void setAverageBuyPrice(BigDecimal averageBuyPrice) {
    this.averageBuyPrice = averageBuyPrice;
  }

  public int getTotalSellQuantity() {
    return totalSellQuantity;
  }

  public void setTotalSellQuantity(int totalSellQuantity) {
    this.totalSellQuantity = totalSellQuantity;
  }

  public BigDecimal getAverageSellPrice() {
    return averageSellPrice;
  }

  public void setAverageSellPrice(BigDecimal averageSellPrice) {
    this.averageSellPrice = averageSellPrice;
  }

  public BigDecimal getTotalSellAmount() {
    return totalSellAmount;
  }

  public void setTotalSellAmount(BigDecimal totalSellAmount) {
    this.totalSellAmount = totalSellAmount;
  }

  public BigDecimal getRealizedProfit() {
    return realizedProfit;
  }

  public void setRealizedProfit(BigDecimal realizedProfit) {
    this.realizedProfit = realizedProfit;
  }

  public int getHoldingQuantity() {
    return holdingQuantity;
  }

  public void setHoldingQuantity(int holdingQuantity) {
    this.holdingQuantity = holdingQuantity;
  }

  public BigDecimal getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(BigDecimal currentPrice) {
    this.currentPrice = currentPrice;
  }

  public BigDecimal getEvaluationAmount() {
    return evaluationAmount;
  }

  public void setEvaluationAmount(BigDecimal evaluationAmount) {
    this.evaluationAmount = evaluationAmount;
  }

  public BigDecimal getEvaluationProfit() {
    return evaluationProfit;
  }

  public void setEvaluationProfit(BigDecimal evaluationProfit) {
    this.evaluationProfit = evaluationProfit;
  }

  public BigDecimal getTotalProfit() {
    return totalProfit;
  }

  public void setTotalProfit(BigDecimal totalProfit) {
    this.totalProfit = totalProfit;
  }

  public BigDecimal getTotalBuyFee() {
    return totalBuyFee;
  }

  public void setTotalBuyFee(BigDecimal totalBuyFee) {
    this.totalBuyFee = totalBuyFee;
  }

  public BigDecimal getTotalSellFee() {
    return totalSellFee;
  }

  public void setTotalSellFee(BigDecimal totalSellFee) {
    this.totalSellFee = totalSellFee;
  }

  public BigDecimal getTotalSellTax() {
    return totalSellTax;
  }

  public void setTotalSellTax(BigDecimal totalSellTax) {
    this.totalSellTax = totalSellTax;
  }

  public BigDecimal getTotalBuyCost() {
    return totalBuyCost;
  }

  public void setTotalBuyCost(BigDecimal totalBuyCost) {
    this.totalBuyCost = totalBuyCost;
  }

  public BigDecimal getTotalSellProceeds() {
    return totalSellProceeds;
  }

  public void setTotalSellProceeds(BigDecimal totalSellProceeds) {
    this.totalSellProceeds = totalSellProceeds;
  }

  public BigDecimal getAverageBuyPriceNet() {
    return averageBuyPriceNet;
  }

  public void setAverageBuyPriceNet(BigDecimal averageBuyPriceNet) {
    this.averageBuyPriceNet = averageBuyPriceNet;
  }

  public BigDecimal getAverageSellPriceNet() {
    return averageSellPriceNet;
  }

  public void setAverageSellPriceNet(BigDecimal averageSellPriceNet) {
    this.averageSellPriceNet = averageSellPriceNet;
  }

  public BigDecimal getRealizedProfitNet() {
    return realizedProfitNet;
  }

  public void setRealizedProfitNet(BigDecimal realizedProfitNet) {
    this.realizedProfitNet = realizedProfitNet;
  }

  public BigDecimal getEvaluationProfitNet() {
    return evaluationProfitNet;
  }

  public void setEvaluationProfitNet(BigDecimal evaluationProfitNet) {
    this.evaluationProfitNet = evaluationProfitNet;
  }

  public BigDecimal getTotalProfitNet() {
    return totalProfitNet;
  }

  public void setTotalProfitNet(BigDecimal totalProfitNet) {
    this.totalProfitNet = totalProfitNet;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TradeProfit that = (TradeProfit) o;
    return totalSellQuantity == that.totalSellQuantity
        && holdingQuantity == that.holdingQuantity
        && Objects.equals(stockItemId, that.stockItemId)
        && Objects.equals(accountId, that.accountId)
        && Objects.equals(totalBuyAmount, that.totalBuyAmount)
        && Objects.equals(averageBuyPrice, that.averageBuyPrice)
        && Objects.equals(averageSellPrice, that.averageSellPrice)
        && Objects.equals(totalSellAmount, that.totalSellAmount)
        && Objects.equals(realizedProfit, that.realizedProfit)
        && Objects.equals(currentPrice, that.currentPrice)
        && Objects.equals(evaluationAmount, that.evaluationAmount)
        && Objects.equals(evaluationProfit, that.evaluationProfit)
        && Objects.equals(totalProfit, that.totalProfit)
        && Objects.equals(totalBuyFee, that.totalBuyFee)
        && Objects.equals(totalSellFee, that.totalSellFee)
        && Objects.equals(totalSellTax, that.totalSellTax)
        && Objects.equals(totalBuyCost, that.totalBuyCost)
        && Objects.equals(totalSellProceeds, that.totalSellProceeds)
        && Objects.equals(averageBuyPriceNet, that.averageBuyPriceNet)
        && Objects.equals(averageSellPriceNet, that.averageSellPriceNet)
        && Objects.equals(realizedProfitNet, that.realizedProfitNet)
        && Objects.equals(evaluationProfitNet, that.evaluationProfitNet)
        && Objects.equals(totalProfitNet, that.totalProfitNet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        stockItemId,
        accountId,
        totalBuyAmount,
        averageBuyPrice,
        totalSellQuantity,
        averageSellPrice,
        totalSellAmount,
        realizedProfit,
        holdingQuantity,
        currentPrice,
        evaluationAmount,
        evaluationProfit,
        totalProfit,
        totalBuyFee,
        totalSellFee,
        totalSellTax,
        totalBuyCost,
        totalSellProceeds,
        averageBuyPriceNet,
        averageSellPriceNet,
        realizedProfitNet,
        evaluationProfitNet,
        totalProfitNet);
  }

  @Override
  public String toString() {
    return "TradeProfit{"
        + "stockItemId="
        + stockItemId
        + ", accountId="
        + accountId
        + ", totalBuyAmount="
        + totalBuyAmount
        + ", averageBuyPrice="
        + averageBuyPrice
        + ", totalSellQuantity="
        + totalSellQuantity
        + ", averageSellPrice="
        + averageSellPrice
        + ", totalSellAmount="
        + totalSellAmount
        + ", realizedProfit="
        + realizedProfit
        + ", holdingQuantity="
        + holdingQuantity
        + ", currentPrice="
        + currentPrice
        + ", evaluationAmount="
        + evaluationAmount
        + ", evaluationProfit="
        + evaluationProfit
        + ", totalProfit="
        + totalProfit
        + ", totalBuyFee="
        + totalBuyFee
        + ", totalSellFee="
        + totalSellFee
        + ", totalSellTax="
        + totalSellTax
        + ", totalBuyCost="
        + totalBuyCost
        + ", totalSellProceeds="
        + totalSellProceeds
        + ", averageBuyPriceNet="
        + averageBuyPriceNet
        + ", averageSellPriceNet="
        + averageSellPriceNet
        + ", realizedProfitNet="
        + realizedProfitNet
        + ", evaluationProfitNet="
        + evaluationProfitNet
        + ", totalProfitNet="
        + totalProfitNet
        + '}';
  }
}
