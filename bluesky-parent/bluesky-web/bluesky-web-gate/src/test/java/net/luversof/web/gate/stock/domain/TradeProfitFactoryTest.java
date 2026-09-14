package net.luversof.web.gate.stock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * 자산 현황의 행을 짓는 팩토리들. 스물다섯 자리짜리 <b>위치 기반</b> 생성자를 채운다.
 *
 * <p>자리 하나가 밀리면 화면의 숫자가 통째로 다른 뜻이 되는데, 타입이 거의 다 {@code BigDecimal} 이라 컴파일러는 아무 말도 하지 않는다. 이 팩토리들에는
 * 테스트가 없었다(실측 2026-09-12: 주식 패키지에서 테스트가 한 번도 부르지 않는 public 메서드 76 개 중 셋).
 *
 * <p>값은 자리마다 다르게 준다 - 같은 값을 쓰면 두 자리가 바뀌어도 테스트가 통과한다.
 */
class TradeProfitFactoryTest {

  private BigDecimal n(int value) {
    return BigDecimal.valueOf(value);
  }

  /** 계좌 행: 종목 자리는 비고, 평단·현재가처럼 계좌 단위에서 뜻이 없는 자리는 0 이다. */
  @Test
  void 계좌_행은_자리마다_제_값을_넣는다() {
    TradeProfit row =
        TradeProfit.ofPortfolioAccount(
            "한국투자증권 위탁",
            n(101), // totalBuyAmount
            7, // totalSellQuantity
            n(102), // totalSellAmount
            n(103), // realizedProfit
            9, // holdingQuantity
            n(104), // evaluationAmount
            n(105), // evaluationProfit
            n(106), // totalProfit
            n(107), // totalBuyFee
            n(108), // totalSellFee
            n(109), // totalSellTax
            n(110), // totalBuyCost
            n(111), // totalSellProceeds
            n(112), // realizedProfitNet
            n(113), // evaluationProfitNet
            n(114)); // totalProfitNet

    assertThat(row.accountName()).isEqualTo("한국투자증권 위탁");
    assertThat(row.stockItemId()).isNull();
    assertThat(row.stockItemName()).isNull();
    assertThat(row.accountId()).isNull();
    assertThat(row.totalBuyAmount()).isEqualByComparingTo(n(101));
    assertThat(row.totalSellQuantity()).isEqualTo(7);
    assertThat(row.totalSellAmount()).isEqualByComparingTo(n(102));
    assertThat(row.realizedProfit()).isEqualByComparingTo(n(103));
    assertThat(row.holdingQuantity()).isEqualTo(9);
    assertThat(row.evaluationAmount()).isEqualByComparingTo(n(104));
    assertThat(row.evaluationProfit()).isEqualByComparingTo(n(105));
    assertThat(row.totalProfit()).isEqualByComparingTo(n(106));
    assertThat(row.totalBuyFee()).isEqualByComparingTo(n(107));
    assertThat(row.totalSellFee()).isEqualByComparingTo(n(108));
    assertThat(row.totalSellTax()).isEqualByComparingTo(n(109));
    assertThat(row.totalBuyCost()).isEqualByComparingTo(n(110));
    assertThat(row.totalSellProceeds()).isEqualByComparingTo(n(111));
    assertThat(row.realizedProfitNet()).isEqualByComparingTo(n(112));
    assertThat(row.evaluationProfitNet()).isEqualByComparingTo(n(113));
    assertThat(row.totalProfitNet()).isEqualByComparingTo(n(114));
    // 계좌 단위에서 뜻이 없는 자리
    assertThat(row.averageBuyPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.averageSellPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.currentPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.averageBuyPriceNet()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.averageSellPriceNet()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.currentPriceDate()).isNull();
  }

  /** 종목 행: 계좌 자리에는 이름을 박지 않고 메시지를 쓴다(영어 화면에 한글이 나가지 않게). */
  @Test
  void 종목_행은_자리마다_제_값을_넣는다() {
    UUID stockId = UUID.randomUUID();
    TradeProfit row =
        TradeProfit.ofPortfolioStock(
            stockId, "삼성전자", n(201), // totalBuyAmount
            n(202), // averageBuyPrice
            3, // totalSellQuantity
            n(203), // averageSellPrice
            n(204), // totalSellAmount
            n(205), // realizedProfit
            4, // holdingQuantity
            n(206), // currentPrice
            n(207), // evaluationAmount
            n(208), // evaluationProfit
            n(209), // totalProfit
            n(210), // totalBuyFee
            n(211), // totalSellFee
            n(212), // totalSellTax
            n(213), // totalBuyCost
            n(214), // totalSellProceeds
            n(215), // averageBuyPriceNet
            n(216), // averageSellPriceNet
            n(217), // realizedProfitNet
            n(218), // evaluationProfitNet
            n(219)); // totalProfitNet

    assertThat(row.stockItemId()).isEqualTo(stockId);
    assertThat(row.stockItemName()).isEqualTo("삼성전자");
    assertThat(row.accountId()).isNull();
    // 메시지 소스가 없는 단위 테스트에서는 빈 문자열이 온다(운영에서는 "전체"). 여기서 고정할 것은
    // "계좌 자리에 종목 이름이나 널이 들어가지 않는다" 는 것이다.
    assertThat(row.accountName()).as("계좌 자리는 메시지에서 온다").isNotNull().isNotEqualTo("삼성전자");
    assertThat(row.totalBuyAmount()).isEqualByComparingTo(n(201));
    assertThat(row.averageBuyPrice()).isEqualByComparingTo(n(202));
    assertThat(row.totalSellQuantity()).isEqualTo(3);
    assertThat(row.averageSellPrice()).isEqualByComparingTo(n(203));
    assertThat(row.totalSellAmount()).isEqualByComparingTo(n(204));
    assertThat(row.realizedProfit()).isEqualByComparingTo(n(205));
    assertThat(row.holdingQuantity()).isEqualTo(4);
    assertThat(row.currentPrice()).isEqualByComparingTo(n(206));
    assertThat(row.evaluationAmount()).isEqualByComparingTo(n(207));
    assertThat(row.evaluationProfit()).isEqualByComparingTo(n(208));
    assertThat(row.totalProfit()).isEqualByComparingTo(n(209));
    assertThat(row.totalBuyFee()).isEqualByComparingTo(n(210));
    assertThat(row.totalSellFee()).isEqualByComparingTo(n(211));
    assertThat(row.totalSellTax()).isEqualByComparingTo(n(212));
    assertThat(row.totalBuyCost()).isEqualByComparingTo(n(213));
    assertThat(row.totalSellProceeds()).isEqualByComparingTo(n(214));
    assertThat(row.averageBuyPriceNet()).isEqualByComparingTo(n(215));
    assertThat(row.averageSellPriceNet()).isEqualByComparingTo(n(216));
    assertThat(row.realizedProfitNet()).isEqualByComparingTo(n(217));
    assertThat(row.evaluationProfitNet()).isEqualByComparingTo(n(218));
    assertThat(row.totalProfitNet()).isEqualByComparingTo(n(219));
  }

  /** 이름만 갈아 끼우고 나머지 스물세 자리는 그대로여야 한다. */
  @Test
  void 이름_주입은_나머지를_건드리지_않는다() {
    UUID stockId = UUID.randomUUID();
    TradeProfit source =
        TradeProfit.ofPortfolioStock(
            stockId, "옛 이름", n(201), n(202), 3, n(203), n(204), n(205), 4, n(206), n(207), n(208),
            n(209), n(210), n(211), n(212), n(213), n(214), n(215), n(216), n(217), n(218), n(219));

    TradeProfit renamed = TradeProfit.withNames(source, "새 이름", "새 계좌");

    assertThat(renamed.stockItemName()).isEqualTo("새 이름");
    assertThat(renamed.accountName()).isEqualTo("새 계좌");
    assertThat(renamed.stockItemId()).isEqualTo(source.stockItemId());
    assertThat(renamed.accountId()).isEqualTo(source.accountId());
    assertThat(renamed.totalBuyAmount()).isEqualByComparingTo(source.totalBuyAmount());
    assertThat(renamed.averageBuyPrice()).isEqualByComparingTo(source.averageBuyPrice());
    assertThat(renamed.totalSellQuantity()).isEqualTo(source.totalSellQuantity());
    assertThat(renamed.averageSellPrice()).isEqualByComparingTo(source.averageSellPrice());
    assertThat(renamed.totalSellAmount()).isEqualByComparingTo(source.totalSellAmount());
    assertThat(renamed.realizedProfit()).isEqualByComparingTo(source.realizedProfit());
    assertThat(renamed.holdingQuantity()).isEqualTo(source.holdingQuantity());
    assertThat(renamed.currentPrice()).isEqualByComparingTo(source.currentPrice());
    assertThat(renamed.evaluationAmount()).isEqualByComparingTo(source.evaluationAmount());
    assertThat(renamed.evaluationProfit()).isEqualByComparingTo(source.evaluationProfit());
    assertThat(renamed.totalProfit()).isEqualByComparingTo(source.totalProfit());
    assertThat(renamed.totalBuyFee()).isEqualByComparingTo(source.totalBuyFee());
    assertThat(renamed.totalSellFee()).isEqualByComparingTo(source.totalSellFee());
    assertThat(renamed.totalSellTax()).isEqualByComparingTo(source.totalSellTax());
    assertThat(renamed.totalBuyCost()).isEqualByComparingTo(source.totalBuyCost());
    assertThat(renamed.totalSellProceeds()).isEqualByComparingTo(source.totalSellProceeds());
    assertThat(renamed.averageBuyPriceNet()).isEqualByComparingTo(source.averageBuyPriceNet());
    assertThat(renamed.averageSellPriceNet()).isEqualByComparingTo(source.averageSellPriceNet());
    assertThat(renamed.realizedProfitNet()).isEqualByComparingTo(source.realizedProfitNet());
    assertThat(renamed.evaluationProfitNet()).isEqualByComparingTo(source.evaluationProfitNet());
    assertThat(renamed.totalProfitNet()).isEqualByComparingTo(source.totalProfitNet());
    assertThat(renamed.currentPriceDate()).isEqualTo(source.currentPriceDate());
  }
}
