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
 * 이동평균(WMA) 원가와 실현손익의 기준을 고정한다.
 *
 * <p>이 계산기는 두 가지 실현손익을 함께 낸다 — 매도 거래에 기록된 값(증권사 기준, 그대로 합산)과 앱이 평균단가로 계산한 값(수수료·세금 반영). 화면은 기록값을 쓰고
 * 원가/평균단가는 이 계산 결과를 쓰므로, 둘의 규칙이 흔들리면 화면 숫자가 서로 어긋난다(실측 사례: 매매 화면 헤드라인과 계좌별 표가 253,554원 차이).
 *
 * <p><b>두 값은 원가 기준 자체가 다르다.</b> 실측 2026-08-23 으로 매도 54 건의 기록값을 원장에서 재계산해 맞춰 봤다 &mdash; 계좌x종목 단위
 * 원가로는 38/54(70%)만 재현되는데 <b>종목 단위(계좌 합산)</b> 원가로는 50/54(92%)가 재현된다. 예: TIGER 코리아배당다우존스는 4 개 계좌를 합친
 * 평단 11,767.3 으로 네 건이 모두 정확히 맞는다(538,600 / 476,709 / 9,641 / 6,470).
 *
 * <p>그래서 계좌별 표의 매도원가(= 매도금액 - 거래세 - 기록 실현손익)는 그 계좌의 매수와 맞지 않을 수 있다. 연금저축1 의 PLUS 고배당주는 403 주를
 * 14,805 원에 사서 19,895 원에 팔았는데 기록 실현손익 405,412 원에서 역산하면 매도원가가 주당 18,888 원이다(차이 1,645,270 원). 같은 화면에
 * 이 계산기가 낸 평단 14,805 원과 나란히 나온다. 이 계산기를 고칠 때 그 차이를 "버그" 로 오해하지 않도록 적어 둔다.
 */
class AverageCostProfitCalculatorBasisTest {

  private static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final UUID STOCK_ITEM_ID = UUID.randomUUID();

  private final AverageCostProfitCalculator calculator = new AverageCostProfitCalculator();

  private Trade trade(
      String date,
      TradeType type,
      int quantity,
      String price,
      String fee,
      String tax,
      String recordedProfit) {
    Trade trade = new Trade();
    trade.setId(UUID.randomUUID());
    trade.setAccountId(ACCOUNT_ID);
    trade.setStockItemId(STOCK_ITEM_ID);
    trade.setType(type);
    trade.setQuantity(quantity);
    trade.setPrice(new BigDecimal(price));
    trade.setFee(new BigDecimal(fee));
    trade.setTax(new BigDecimal(tax));
    trade.setTradeDate(Instant.parse(date + "T00:00:00Z"));
    if (recordedProfit != null) {
      trade.setRealizedProfit(new BigDecimal(recordedProfit));
    }
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

  /** 매수 100@1,000(수수료 100) + 100@1,200(수수료 120) -> 평균 1,100 / 수수료 포함 1,101.10 */
  private List<Trade> twoBuys() {
    return List.of(
        trade("2020-02-03", TradeType.BUY, 100, "1000", "100", "0", null),
        trade("2020-03-03", TradeType.BUY, 100, "1200", "120", "0", null));
  }

  @Test
  void 평균단가는_수수료_제외와_포함_두_가지로_나온다() {
    var profit = calculator.calculate(twoBuys(), request(), null, null);

    assertEquals(200, profit.getHoldingQuantity());
    assertEquals(0, new BigDecimal("1100.00").compareTo(profit.getAverageBuyPrice()));
    assertEquals(0, new BigDecimal("1101.10").compareTo(profit.getAverageBuyPriceNet()));
    assertEquals(0, new BigDecimal("220000").compareTo(profit.getTotalBuyAmount()));
    assertEquals(0, new BigDecimal("220").compareTo(profit.getTotalBuyFee()));
  }

  @Test
  void 부분_매도_뒤에도_남은_평균단가는_그대로다() {
    var trades = new java.util.ArrayList<>(twoBuys());
    // 50주 매도: 대금 75,000 - 수수료 75 - 세금 150 = 74,775, 원가(net) 1,101.10 x 50 = 55,055
    trades.add(trade("2020-04-01", TradeType.SELL, 50, "1500", "75", "150", "20000"));

    var profit = calculator.calculate(trades, request(), null, null);

    assertEquals(150, profit.getHoldingQuantity());
    assertEquals(0, new BigDecimal("1100.00").compareTo(profit.getAverageBuyPrice()));
    assertEquals(0, new BigDecimal("1101.10").compareTo(profit.getAverageBuyPriceNet()));
    assertEquals(0, new BigDecimal("19720").compareTo(profit.getRealizedProfitNet()));
    // 기록된 실현손익은 계산값과 별개로 그대로 합산된다.
    assertEquals(0, new BigDecimal("20000").compareTo(profit.getRealizedProfit()));
    assertEquals(0, new BigDecimal("74775").compareTo(profit.getTotalSellProceeds()));
  }

  @Test
  void 전량_매도하면_원가가_0으로_정리되고_재매수는_새로_쌓인다() {
    var trades = new java.util.ArrayList<>(twoBuys());
    trades.add(trade("2020-04-01", TradeType.SELL, 50, "1500", "75", "150", "20000"));
    trades.add(trade("2020-05-06", TradeType.SELL, 150, "1000", "150", "300", "-10000"));

    var closed = calculator.calculate(trades, request(), null, null);
    assertEquals(0, closed.getHoldingQuantity());
    assertEquals(0, BigDecimal.ZERO.compareTo(closed.getAverageBuyPrice()));
    // 전량 매도면 (총 매도 실수령 - 총 매수 원가(수수료 포함)) 과 같아야 한다: 224,325 - 220,220
    assertEquals(0, new BigDecimal("4105").compareTo(closed.getRealizedProfitNet()));
    assertEquals(0, new BigDecimal("10000").compareTo(closed.getRealizedProfit()));
    assertEquals(200, closed.getTotalSellQuantity());

    trades.add(trade("2020-06-10", TradeType.BUY, 10, "2000", "0", "0", null));
    var reentered = calculator.calculate(trades, request(), null, null);
    assertEquals(10, reentered.getHoldingQuantity());
    assertEquals(0, new BigDecimal("2000.00").compareTo(reentered.getAverageBuyPrice()));
  }

  @Test
  void 보유수량보다_많이_팔면_원가는_음수로_남지_않는다() {
    var trades = new java.util.ArrayList<>(twoBuys());
    trades.add(trade("2020-04-01", TradeType.SELL, 250, "1500", "375", "750", "100000"));

    var profit = calculator.calculate(trades, request(), null, null);

    assertEquals(0, profit.getHoldingQuantity());
    assertEquals(0, BigDecimal.ZERO.compareTo(profit.getAverageBuyPrice()));
    assertEquals(0, BigDecimal.ZERO.compareTo(profit.getAverageBuyPriceNet()));
  }

  /**
   * 같은 날 매수와 매도가 함께 있으면 <b>매수를 먼저</b> 처리한다.
   *
   * <p>원장에 실제로 있다 &mdash; 실측 2026-08-24: 같은 날 매수와 매도가 함께 있는 (계좌, 종목, 날짜) 가 6 건이다 (2020-01-29 /
   * 01-30 / 02-05 / 03-03 두 건 / 03-04, 모두 같은 계좌에서 같은 수량을 되판 형태). 조회가 돌려주는 순서는 <b>매도가 먼저</b>다
   * &mdash; SQL 에 ORDER BY 가 없다.
   *
   * <p>정렬 키는 {@code tradeDate -> 매수 우선 -> id} 다. 가운데 키를 빼면 남는 것은 id 뿐이라 <b>id 가 어떻게 생겼느냐에 따라</b>
   * 결과가 달라진다 &mdash; 매도가 먼저 처리되면 그 시점 보유가 0 이라 원가가 0 으로 잡히고 실현손익이 매도대금 전액이 된다.
   *
   * <p>실데이터로는 잡히지 않는다 &mdash; 실측 2026-08-24: 위 6 건 모두 id 오름차순이 우연히 매수를 먼저 놓아, 가운데 키를 빼고 배포해도 {@code
   * calculateProfit} 의 화면 숫자가 하나도 바뀌지 않았다(61 행 x 7 필드 전부 동일). 검사 스크립트 48 개도 전부 통과했다. 그래서 이 검사는 매도의
   * id 가 매수보다 <b>작게</b> 오도록 못박아 그 상황을 직접 만든다.
   */
  @Test
  void 같은_날이면_id_와_무관하게_매수를_먼저_처리한다() {
    Trade sell = trade("2020-01-29", TradeType.SELL, 100, "1405", "0", "0", "11000");
    sell.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    Trade buy = trade("2020-01-29", TradeType.BUY, 100, "1295", "0", "0", null);
    buy.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    var profit = calculator.calculate(List.of(sell, buy), request(), null, null);

    assertEquals(0, profit.getHoldingQuantity());
    // 매수를 먼저 처리하면 원가 1,295 x 100 = 129,500, 매도대금 140,500 -> 11,000
    assertEquals(
        0,
        new BigDecimal("11000").compareTo(profit.getRealizedProfitNet()),
        "매도가 먼저 처리되면 원가 0 으로 잡혀 140,500 이 된다");
    assertEquals(0, new BigDecimal("129500").compareTo(profit.getTotalBuyAmount()));
  }

  @Test
  void 단가_0_매수는_무상주로_보아_평균단가를_낮춘다() {
    // 실데이터: 한국투자증권 위탁 계좌의 원티드랩 2021-10-28 매수 4주 단가 0(상장 배정분).
    var trades = new java.util.ArrayList<>(twoBuys());
    trades.add(trade("2020-04-01", TradeType.BUY, 100, "0", "0", "0", null));

    var profit = calculator.calculate(trades, request(), null, null);

    // 원가는 그대로 220,000, 수량만 300 -> 평균 733.33
    assertEquals(300, profit.getHoldingQuantity());
    assertEquals(0, new BigDecimal("220000").compareTo(profit.getTotalBuyAmount()));
    assertEquals(0, new BigDecimal("733.33").compareTo(profit.getAverageBuyPrice()));
    // 수수료 포함 원가도 220,220 그대로다.
    assertEquals(0, new BigDecimal("734.07").compareTo(profit.getAverageBuyPriceNet()));
  }
}
