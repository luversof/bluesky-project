package net.luversof.web.gate.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeProfit(
    UUID stockItemId,
    String stockItemName,
    UUID accountId,
    String accountName,

    // 매수 관련 정보 (수수료 제외: 기존 호환 유지)
    BigDecimal totalBuyAmount,
    BigDecimal averageBuyPrice,

    // 매도 관련 정보 (실현 손익 - 수수료/거래세 제외: 기존 호환 유지)
    int totalSellQuantity,
    BigDecimal averageSellPrice,
    BigDecimal totalSellAmount,
    BigDecimal realizedProfit,

    // 보유 관련 정보 (미실현 손익 - 수수료 제외: 기존 호환 유지)
    int holdingQuantity,
    BigDecimal currentPrice,
    BigDecimal evaluationAmount,
    BigDecimal evaluationProfit,

    // 총 손익 (실현 + 미실현) - 수수료 제외: 기존 호환 유지
    BigDecimal totalProfit,

    // -----------------------------
    // 수수료/거래세 포함한 실제 손익 계산을 위한 추가 필드
    // -----------------------------
    // 합계 수수료/거래세
    BigDecimal totalBuyFee, // BUY 거래 수수료 합계
    BigDecimal totalSellFee, // SELL 거래 수수료 합계
    BigDecimal totalSellTax, // SELL 거래세 합계

    // 순수 매수/매도 금액 (수수료/세금 반영)
    BigDecimal totalBuyCost, // 매수 총원가 = 총매수액 + 매수수수료
    BigDecimal totalSellProceeds, // 매도 총수령액 = 총매도액 - 매도수수료 - 거래세

    // 수수료/세금 반영 평단
    BigDecimal averageBuyPriceNet, // (총매수액+수수료)/수량
    BigDecimal averageSellPriceNet, // (총매도액-수수료-세금)/수량

    // 실현/미실현/총 손익 (수수료/세금 반영)
    BigDecimal realizedProfitNet, // 총수령액 - 총원가
    BigDecimal evaluationProfitNet, // 평가액 - (평단NET*보유수량)
    BigDecimal totalProfitNet, // realizedNet + evaluationNet

    /** currentPrice 가 어느 거래일 종가인지. 오늘 시세가 아직 없으면 과거 일자가 온다. */
    java.time.LocalDate currentPriceDate) {

  // --- 팩토리 메서드 ---

  /** 자산 현황 - 계좌별 집계 행 생성 */
  public static TradeProfit ofAccountStatus(
      String accountName,
      BigDecimal evalAmt,
      BigDecimal evalProfit,
      BigDecimal realizedProfit,
      BigDecimal totalBuyCost,
      BigDecimal totalSellProceeds) {
    return new TradeProfit(
        null,
        null,
        null,
        accountName,
        null,
        null,
        0,
        null,
        null,
        realizedProfit,
        0,
        null,
        evalAmt,
        evalProfit,
        null,
        null,
        null,
        null,
        totalBuyCost,
        totalSellProceeds,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  /** 자산 현황 - 종목별 집계 행 생성 */
  public static TradeProfit ofStockStatus(
      UUID stockItemId,
      String stockItemName,
      BigDecimal avgBuyPrice,
      int holdingQty,
      BigDecimal currentPrice,
      BigDecimal evalAmt,
      BigDecimal evalProfit,
      BigDecimal realizedProfit,
      BigDecimal totalBuyCost) {
    return new TradeProfit(
        stockItemId,
        stockItemName,
        null,
        null,
        null,
        avgBuyPrice,
        0,
        null,
        null,
        realizedProfit,
        holdingQty,
        currentPrice,
        evalAmt,
        evalProfit,
        null,
        null,
        null,
        null,
        totalBuyCost,
        null,
        avgBuyPrice,
        null,
        null,
        null,
        null,
        null);
  }

  /** 실현손익 현황 - 종목별 집계 행 생성 (수수료/세금 반영 포함) */
  public static TradeProfit ofStockRealized(
      UUID stockItemId,
      String stockItemName,
      int holdingQty,
      int totalSellQty,
      BigDecimal totalBuyAmount,
      BigDecimal totalSellAmount,
      BigDecimal evalAmt,
      BigDecimal evalProfit,
      BigDecimal realizedProfit,
      BigDecimal realizedProfitNet,
      BigDecimal totalBuyCost,
      BigDecimal totalSellProceeds,
      BigDecimal totalBuyFee,
      BigDecimal totalSellFee,
      BigDecimal totalSellTax) {
    return new TradeProfit(
        stockItemId,
        stockItemName,
        null,
        null,
        totalBuyAmount,
        null,
        totalSellQty,
        null,
        totalSellAmount,
        // 화면(종목별 실현손익 표)은 거래에 기록된 실현손익을 쓴다. 예전에는 이 자리에도 net 값을
        // 넣어, 같은 화면의 헤드라인·거래목록(기록값)과 표 합계가 어긋났다(실측 0.11% 차이).
        realizedProfit,
        holdingQty,
        null,
        evalAmt,
        evalProfit,
        null,
        totalBuyFee,
        totalSellFee,
        totalSellTax,
        totalBuyCost,
        totalSellProceeds,
        null,
        null,
        realizedProfitNet,
        null,
        null,
        null);
  }

  /** 포트폴리오 - 종목 집계 행 생성 */
  public static TradeProfit ofPortfolioStock(
      UUID stockId,
      String stockName,
      BigDecimal totalBuyAmount,
      BigDecimal avgBuyPrice,
      int totalSellQty,
      BigDecimal avgSellPrice,
      BigDecimal totalSellAmount,
      BigDecimal realizedProfit,
      int holdingQty,
      BigDecimal currentPrice,
      BigDecimal evaluationAmount,
      BigDecimal evaluationProfit,
      BigDecimal totalProfit,
      BigDecimal totalBuyFee,
      BigDecimal totalSellFee,
      BigDecimal totalSellTax,
      BigDecimal totalBuyCost,
      BigDecimal totalSellProceeds,
      BigDecimal avgBuyPriceNet,
      BigDecimal avgSellPriceNet,
      BigDecimal realizedProfitNet,
      BigDecimal evaluationProfitNet,
      BigDecimal totalProfitNet) {
    return new TradeProfit(
        stockId,
        stockName,
        null,
        // 종목 단위로 합친 행의 계좌 이름 자리. 문구를 그대로 박으면 영어 화면에도 한글이 나간다.
        io.github.luversof.boot.context.support.MessageUtil.getMessage("common.label.all"),
        totalBuyAmount,
        avgBuyPrice,
        totalSellQty,
        avgSellPrice,
        totalSellAmount,
        realizedProfit,
        holdingQty,
        currentPrice,
        evaluationAmount,
        evaluationProfit,
        totalProfit,
        totalBuyFee,
        totalSellFee,
        totalSellTax,
        totalBuyCost,
        totalSellProceeds,
        avgBuyPriceNet,
        avgSellPriceNet,
        realizedProfitNet,
        evaluationProfitNet,
        totalProfitNet,
        null);
  }

  /** 포트폴리오 - 계좌 소계 행 생성 */
  public static TradeProfit ofPortfolioAccount(
      String accountName,
      BigDecimal totalBuyAmount,
      int totalSellQty,
      BigDecimal totalSellAmount,
      BigDecimal realizedProfit,
      int holdingQty,
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
      BigDecimal totalProfitNet) {
    return new TradeProfit(
        null,
        null,
        null,
        accountName,
        totalBuyAmount,
        BigDecimal.ZERO,
        totalSellQty,
        BigDecimal.ZERO,
        totalSellAmount,
        realizedProfit,
        holdingQty,
        BigDecimal.ZERO,
        evaluationAmount,
        evaluationProfit,
        totalProfit,
        totalBuyFee,
        totalSellFee,
        totalSellTax,
        totalBuyCost,
        totalSellProceeds,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        realizedProfitNet,
        evaluationProfitNet,
        totalProfitNet,
        null);
  }

  /** 이름 정보를 주입하여 새 TradeProfit 생성 */
  public static TradeProfit withNames(
      TradeProfit source, String stockItemName, String accountName) {
    return new TradeProfit(
        source.stockItemId(),
        stockItemName,
        source.accountId(),
        accountName,
        source.totalBuyAmount(),
        source.averageBuyPrice(),
        source.totalSellQuantity(),
        source.averageSellPrice(),
        source.totalSellAmount(),
        source.realizedProfit(),
        source.holdingQuantity(),
        source.currentPrice(),
        source.evaluationAmount(),
        source.evaluationProfit(),
        source.totalProfit(),
        source.totalBuyFee(),
        source.totalSellFee(),
        source.totalSellTax(),
        source.totalBuyCost(),
        source.totalSellProceeds(),
        source.averageBuyPriceNet(),
        source.averageSellPriceNet(),
        source.realizedProfitNet(),
        source.evaluationProfitNet(),
        source.totalProfitNet(),
        source.currentPriceDate());
  }
}
