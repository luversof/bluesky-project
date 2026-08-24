package net.luversof.api.stock.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;

/**
 * 같은 시각(tradeDate 가 동일한) 매수/매도가 섞여 있을 때 결과가 입력 순서에 흔들리지 않아야 한다.
 *
 * <p>실제 데이터에 당일 매매(같은 종목을 같은 timestamp 로 매수 + 매도)가 6 건 있다. 정렬 키가 tradeDate 뿐이면 타이 순서는 DB 가 행을 돌려준
 * 순서에 좌우되고, 매도가 먼저 오면 보유수량 0 상태로 원가 0 이 잡혀 매도대금 전액이 실현손익이 된다.
 */
class AverageCostProfitCalculatorOrderTest {

  private static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final UUID STOCK_ITEM_ID = UUID.randomUUID();
  private static final Instant SAME_INSTANT = Instant.parse("2020-01-29T00:00:00Z");

  private Trade trade(TradeType type, int quantity, String price, String fee, String tax) {
    Trade trade = new Trade();
    trade.setId(UUID.randomUUID());
    trade.setAccountId(ACCOUNT_ID);
    trade.setStockItemId(STOCK_ITEM_ID);
    trade.setType(type);
    trade.setQuantity(quantity);
    trade.setPrice(new BigDecimal(price));
    trade.setFee(new BigDecimal(fee));
    trade.setTax(new BigDecimal(tax));
    trade.setTradeDate(SAME_INSTANT);
    trade.setRealizedProfit(BigDecimal.ZERO);
    return trade;
  }

  private TradeProfitRequest request() {
    TradeProfitRequest request = new TradeProfitRequest();
    request.setAccountIdList(List.of(ACCOUNT_ID));
    // 기간을 주면 현재가 조회 경로를 타지 않아 StockPriceService 없이 계산할 수 있다.
    request.setStartDate(Instant.parse("2020-01-01T00:00:00Z"));
    request.setEndDate(Instant.parse("2020-12-31T00:00:00Z"));
    return request;
  }

  @Test
  void sameInstantBuyAndSellIsOrderIndependent() {
    // 실데이터: 쌍방울 2020-01-29 당일 매매
    Trade buy = trade(TradeType.BUY, 17939, "1295", "3484", "0");
    Trade sell = trade(TradeType.SELL, 17939, "1405", "3780", "56710");

    var calculator = new AverageCostProfitCalculator();
    var buyFirst = calculator.calculate(List.of(buy, sell), request(), null, null);
    var sellFirst = calculator.calculate(List.of(sell, buy), request(), null, null);

    assertEquals(buyFirst.getRealizedProfitNet(), sellFirst.getRealizedProfitNet());
    assertEquals(buyFirst.getHoldingQuantity(), sellFirst.getHoldingQuantity());
    assertEquals(buyFirst.getAverageBuyPrice(), sellFirst.getAverageBuyPrice());
  }
}
