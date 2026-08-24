package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 매도 원가가 세 곳에서 같은 식을 쓰는지 본다.
 *
 * <p>매도 원가 = 매도금액 &minus; 증권거래세 &minus; 기록된 실현손익. 기록된 실현손익은 증권사가 세금까지 뺀 뒤의 값이라, 세금을 빼지 않으면 원가가 그만큼
 * 부풀어 오른다.
 *
 * <p>실측 2026-08-22: 매매 화면만 이 식에서 세금을 빠뜨리고 있었다. 계좌별 표의 매도 원가가 한국투자증권 위탁 776,247,350 &rarr;
 * 774,422,253(1,825,097 과대), ISA 80,889,768 &rarr; 80,828,898(60,870 과대)로 합 1,885,967 원 부풀어 있었다.
 * 종목별 표도 같은 식이었고, 그 값으로 나누는 선택 합계의 수익률(net/soldCost)까지 낮게 나왔다.
 *
 * <p>api-stock 의 {@code costOfGoodsSold} 와 배당 화면은 이미 올바른 식을 쓴다. 세 곳이 갈라지지 않도록 고정한다.
 */
class SoldCostFormulaTest {

  private static final Path TRADE_CONTROLLER =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockTradeHtmxController.java");

  private static final Path TRADE_SECTIONS =
      Path.of("src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte");

  private static final Path DIVIDEND_CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 계좌별_매도원가가_세금을_뺀다() throws IOException {
    String source = read(TRADE_CONTROLLER);
    assertThat(source)
        .as("매도 원가에서 증권거래세를 빼지 않으면 원가가 부풀고 수익률이 낮게 나온다")
        .contains("sellAmount.subtract(sellTax).subtract(realized)");
    assertThat(source).as("세금을 빼지 않는 옛 식이 남아 있다").doesNotContain("sellAmount.subtract(realized)");
    assertThat(source).contains("s.totalSellTax()");
  }

  @Test
  void 종목별_매도원가가_세금을_뺀다() throws IOException {
    String source = read(TRADE_SECTIONS);
    assertThat(source)
        .as("종목별 표가 계좌별 표와 다른 식을 쓴다")
        .contains("item.totalSellAmount().subtract(itemSellTax).subtract(itemNet)");
    assertThat(source)
        .as("세금을 빼지 않는 옛 식이 남아 있다")
        .doesNotContain("item.totalSellAmount().subtract(itemNet)");
  }

  /** 배당 화면은 이미 올바른 식을 쓴다. 셋이 갈라지지 않도록 함께 본다. */
  @Test
  void 배당_화면의_원가_식도_그대로다() throws IOException {
    assertThat(read(DIVIDEND_CONTROLLER))
        .contains("amount.subtract(nz(trade.tax())).subtract(nz(trade.realizedProfit()))");
  }

  /** 세 곳 모두 '매도금액에서 세금과 실현손익을 뺀다'는 같은 모양이어야 한다. */
  @Test
  void 세_곳의_식이_같은_모양이다() throws IOException {
    for (Path path : List.of(TRADE_CONTROLLER, TRADE_SECTIONS, DIVIDEND_CONTROLLER)) {
      String source = read(path);
      long subtractions =
          source
              .lines()
              .filter(
                  line ->
                      line.contains("subtract") && line.contains("Tax")
                          || line.contains("subtract(nz(trade.tax()))")
                          || line.contains("subtract(sellTax)")
                          || line.contains("subtract(itemSellTax)"))
              .count();
      assertThat(subtractions).as(path + " 에 세금을 빼는 원가 계산이 없다").isGreaterThan(0);
    }
  }
}
