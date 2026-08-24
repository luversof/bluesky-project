package net.luversof.api.stock.service.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.StockPriceService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequestGroup;

@Component
public class AverageCostProfitCalculator implements ProfitCalculator {

  private static final Logger log = LoggerFactory.getLogger(AverageCostProfitCalculator.class);

  @Override
  public TradeProfit calculate(
      List<Trade> trades, TradeProfitRequest request, StockPriceService stockPriceService) {
    return calculate(trades, request, stockPriceService, null);
  }

  @Override
  public TradeProfit calculate(
      List<Trade> trades,
      TradeProfitRequest request,
      StockPriceService stockPriceService,
      java.util.Map<java.util.UUID, net.luversof.api.stock.domain.StockDailyClosePrice>
          latestPrices) {
    if (trades == null || trades.isEmpty()) {
      return null;
    }

    // 1. Identify Context (Stock/Account)
    Trade first = trades.get(0);
    UUID stockItemId = first.getStockItemId();
    UUID accountId =
        (request.getGroupBy() == TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM)
            ? first.getAccountId()
            : null;

    // 2. Sort by date ascending
    // 같은 시각이면 BUY 를 먼저 처리한다. 당일 매매(같은 timestamp 의 매수+매도)에서 매도가 먼저 오면
    // 보유수량 0 이라 단위원가가 0 으로 잡혀 매도대금 전액이 실현손익이 되고, 뒤의 매수는 유령 보유로 남는다.
    // 정렬 키가 tradeDate 뿐이면 그 순서는 DB 가 행을 돌려준 순서(쿼리에 ORDER BY 가 없다)에 좌우된다.
    // 마지막 id 비교는 남은 동률까지 없애 결과를 입력 순서와 무관하게 만든다.
    // 시계열 경로(TradeProfitService)도 같은 규칙으로 정렬한다.
    List<Trade> sortedTrades = new ArrayList<>(trades);
    sortedTrades.sort(
        Comparator.comparing(Trade::getTradeDate)
            .thenComparing(trade -> trade.getType() == TradeType.BUY ? 0 : 1)
            .thenComparing(Trade::getId, Comparator.nullsLast(Comparator.naturalOrder())));

    // 3. Period accumulators
    BigDecimal periodTotalBuyAmount = BigDecimal.ZERO;
    BigDecimal periodTotalBuyFee = BigDecimal.ZERO;

    int periodTotalSellQuantity = 0;
    BigDecimal periodTotalSellAmount = BigDecimal.ZERO;
    BigDecimal periodTotalSellFee = BigDecimal.ZERO;
    BigDecimal periodTotalSellTax = BigDecimal.ZERO;

    BigDecimal periodRealizedProfit = BigDecimal.ZERO; // Gross (from DB)
    BigDecimal periodRealizedProfitNet = BigDecimal.ZERO; // Net (Calculated)

    // Date filter helpers
    Instant start = request.getStartDate();
    Instant end = request.getEndDate();

    // 4. WMA State for Holdings
    long currentQuantity = 0;
    BigDecimal currentTotalCost = BigDecimal.ZERO; // Gross cost (price * qty)
    BigDecimal currentTotalCostNet = BigDecimal.ZERO; // Net cost (price * qty + fee)

    for (Trade trade : sortedTrades) {
      if (trade.getType() == null) continue;

      Instant tradeDate = trade.getTradeDate();
      boolean inPeriod = true;
      if (start != null && tradeDate.isBefore(start)) inPeriod = false;
      if (end != null && tradeDate.isAfter(end)) inPeriod = false;

      BigDecimal fee = nz(trade.getFee());
      BigDecimal tax = nz(trade.getTax());
      int q = trade.getQuantity();
      BigDecimal price = trade.getPrice();

      // Amount: Price * Quantity
      BigDecimal amount = price.multiply(BigDecimal.valueOf(q));

      if (trade.getType() == TradeType.BUY) {
        if (q > 0) {
          currentQuantity += q;
          currentTotalCost = currentTotalCost.add(amount);
          // Cost Net includes Fee
          currentTotalCostNet = currentTotalCostNet.add(amount).add(fee);
        }

        if (inPeriod) {
          periodTotalBuyAmount = periodTotalBuyAmount.add(amount);
          periodTotalBuyFee = periodTotalBuyFee.add(fee);
        }
      } else if (trade.getType() == TradeType.SELL) {
        BigDecimal tradeSellAmount = amount;
        BigDecimal dbRealizedProfit = nz(trade.getRealizedProfit());

        // Calculate Net Realized Profit for this trade
        // Proceeds = SellAmount - Fee - Tax
        BigDecimal sellProceeds = tradeSellAmount.subtract(fee).subtract(tax);

        // Cost Basis for this sale?
        // Based on WMA, per-share unit cost net
        BigDecimal unitCostNet = BigDecimal.ZERO;
        BigDecimal unitCostGross = BigDecimal.ZERO;
        if (currentQuantity > 0) {
          unitCostNet =
              currentTotalCostNet.divide(
                  BigDecimal.valueOf(currentQuantity), 10, RoundingMode.HALF_UP);
          unitCostGross =
              currentTotalCost.divide(
                  BigDecimal.valueOf(currentQuantity), 10, RoundingMode.HALF_UP);
        }

        BigDecimal costOfGoodsSoldNet = unitCostNet.multiply(BigDecimal.valueOf(q));
        BigDecimal costOfGoodsSoldGross = unitCostGross.multiply(BigDecimal.valueOf(q));

        BigDecimal myRealizedProfitNet = sellProceeds.subtract(costOfGoodsSoldNet);

        // Update State
        if (currentQuantity >= q) {
          currentQuantity -= q;
          currentTotalCost = currentTotalCost.subtract(costOfGoodsSoldGross);
          currentTotalCostNet = currentTotalCostNet.subtract(costOfGoodsSoldNet);
        } else {
          currentQuantity = 0;
          currentTotalCost = BigDecimal.ZERO;
          currentTotalCostNet = BigDecimal.ZERO;
        }

        // Floating Point cleanup
        if (currentQuantity == 0) {
          currentTotalCost = BigDecimal.ZERO;
          currentTotalCostNet = BigDecimal.ZERO;
        }

        if (inPeriod) {
          periodTotalSellQuantity += q;
          periodTotalSellAmount = periodTotalSellAmount.add(tradeSellAmount);
          periodTotalSellFee = periodTotalSellFee.add(fee);
          periodTotalSellTax = periodTotalSellTax.add(tax);

          // Use DB Value for Gross, Calculated for Net
          periodRealizedProfit = periodRealizedProfit.add(dbRealizedProfit);
          periodRealizedProfitNet = periodRealizedProfitNet.add(myRealizedProfitNet);
        }
      }
    }

    // 5. Calculate Holdings (Snapshot)
    int holdingQuantity = (int) currentQuantity;

    BigDecimal averageBuyPrice =
        holdingQuantity > 0
            ? currentTotalCost.divide(BigDecimal.valueOf(holdingQuantity), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    BigDecimal averageBuyPriceNet =
        holdingQuantity > 0
            ? currentTotalCostNet.divide(
                BigDecimal.valueOf(holdingQuantity), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    // 6. Current Evaluation
    BigDecimal currentPrice = BigDecimal.ZERO;
    BigDecimal evaluationAmount = BigDecimal.ZERO;
    BigDecimal evaluationProfit = BigDecimal.ZERO; // Gross
    BigDecimal evaluationProfitNet = BigDecimal.ZERO; // Net

    java.time.LocalDate currentPriceDate = null;
    if (!request.hasDateRange()) {
      // 가격과 그 가격의 거래일을 함께 받는다. 상위에서 일괄 조회해 넘겨준 맵이 있으면 그것을 쓰고,
      // 없을 때만 개별 조회로 떨어진다(종목마다 조회하면 종목 수만큼 DB 왕복이 생긴다).
      var preloaded = latestPrices != null ? latestPrices.get(stockItemId) : null;
      if (preloaded != null) {
        currentPrice = preloaded.closePrice() != null ? preloaded.closePrice() : BigDecimal.ZERO;
        currentPriceDate = preloaded.tradeDate();
      } else {
        var latest = stockPriceService.getCurrentPriceHistory(stockItemId);
        currentPrice = latest.map(h -> h.getClosePrice()).orElse(BigDecimal.ZERO);
        currentPriceDate = latest.map(h -> h.getTradeDate()).orElse(null);
      }
      evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(holdingQuantity));

      evaluationProfit = evaluationAmount.subtract(currentTotalCost);

      // Evaluation Net: Value - Cost Basis Net (which includes buy fees)
      // Selling fees (future) are not deduced in "Current Evaluation" usually,
      // but sometimes "Liquidation Value" does.
      // Standard Accounting: Unrealized G/L = Market Value - Cost Basis.
      evaluationProfitNet = evaluationAmount.subtract(currentTotalCostNet);
    }

    // 7. Period Averages & Derived Stats
    BigDecimal averageSellPrice =
        periodTotalSellQuantity > 0
            ? periodTotalSellAmount.divide(
                BigDecimal.valueOf(periodTotalSellQuantity), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    // Net Sell Price (Proceeds / Qty)
    BigDecimal periodTotalSellProceeds =
        periodTotalSellAmount.subtract(periodTotalSellFee).subtract(periodTotalSellTax);
    BigDecimal averageSellPriceNet =
        periodTotalSellQuantity > 0
            ? periodTotalSellProceeds.divide(
                BigDecimal.valueOf(periodTotalSellQuantity), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    BigDecimal periodTotalBuyCost = periodTotalBuyAmount.add(periodTotalBuyFee);

    BigDecimal totalProfit = periodRealizedProfit.add(evaluationProfit);
    BigDecimal totalProfitNet = periodRealizedProfitNet.add(evaluationProfitNet);

    // 8. Populate Result
    TradeProfit profit = new TradeProfit();
    profit.setStockItemId(stockItemId);
    profit.setAccountId(accountId);

    // Base Fields
    profit.setTotalBuyAmount(periodTotalBuyAmount);
    profit.setAverageBuyPrice(averageBuyPrice);
    profit.setTotalSellQuantity(periodTotalSellQuantity);
    profit.setAverageSellPrice(averageSellPrice);
    profit.setTotalSellAmount(periodTotalSellAmount);
    profit.setRealizedProfit(periodRealizedProfit);
    profit.setHoldingQuantity(holdingQuantity);
    profit.setCurrentPrice(currentPrice);
    profit.setCurrentPriceDate(currentPriceDate);
    profit.setEvaluationAmount(evaluationAmount);
    profit.setEvaluationProfit(evaluationProfit);
    profit.setTotalProfit(totalProfit);

    // Net Fields
    profit.setTotalBuyFee(periodTotalBuyFee);
    profit.setTotalSellFee(periodTotalSellFee);
    profit.setTotalSellTax(periodTotalSellTax);
    profit.setTotalBuyCost(periodTotalBuyCost);
    profit.setTotalSellProceeds(periodTotalSellProceeds);

    profit.setAverageBuyPriceNet(averageBuyPriceNet);
    profit.setAverageSellPriceNet(averageSellPriceNet);

    profit.setRealizedProfitNet(periodRealizedProfitNet);
    profit.setEvaluationProfitNet(evaluationProfitNet);
    profit.setTotalProfitNet(totalProfitNet);

    log.debug(
        "[ProfitCalculator] Stock: {}, Realized: {}, RealizedNet: {}",
        stockItemId,
        periodRealizedProfit,
        periodRealizedProfitNet);

    return profit;
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
