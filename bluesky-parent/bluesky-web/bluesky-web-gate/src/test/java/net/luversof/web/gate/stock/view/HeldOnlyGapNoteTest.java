package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 자산 현황의 '종목별 현황' 합계는 보유 종목 몫이다 - 얼마나 작은지까지 적는다.
 *
 * <p>실측 2026-09-11: 이 표의 실현손익 합계는 <b>140,350,295</b> 인데 대시보드 · 매매 · 자산 성장은 <b>225,630,135</b> 를
 * 적는다(차이 85,279,840 = 37.8%). 누적 배당도 65,652,134 대 59,067,537 로 6,584,597 이 빠진다. 표에 보이는 종목은 9 개다.
 *
 * <p>빠진 종목은 두 갈래다: 다 판 34 종목과, <b>거래 행 없이 배당만 있는 1 종목</b>(하나금융지주 2,100). 뒤엣것을 빼고 세면 59,067,537 +
 * 6,582,497 = 65,650,034 로 전체 배당에 2,100 모자라 사용자가 합을 맞출 수 없다. 둘을 합쳐 35 종목으로 적으면 실현 140,350,295 +
 * 85,279,840 = 225,630,135, 배당 59,067,537 + 6,584,597 = 65,652,134 로 정확히 맞는다.
 *
 * <p>"보유 중인 종목만 나옵니다" 라는 안내는 2026-09-03 부터 있었지만 <b>얼마나</b> 작은지는 없었다. 같은 이름의 값이 화면마다 다른 이유를 사용자가 직접
 * 맞춰 볼 수 있어야 한다.
 */
class HeldOnlyGapNoteTest {

  private static final Path TEMPLATE = Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte");
  private static final Path CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockPortfolioHtmxController.java");

  @Test
  void 판_종목_몫을_걸러내기_전에_센다() throws IOException {
    String controller = Files.readString(CONTROLLER, StandardCharsets.UTF_8);

    int countedAt = controller.indexOf("List<TradeProfit> soldOutList");
    int removedAt =
        controller.indexOf("stockGroupedList.removeIf(tp -> tp.holdingQuantity() == 0)");
    assertThat(countedAt).as("판 종목 목록을 만드는 자리").isPositive();
    assertThat(removedAt).as("보유 0 을 걸러내는 자리").isPositive();
    assertThat(countedAt).as("걸러낸 뒤에 세면 0 이 된다").isLessThan(removedAt);
    assertThat(controller).contains("model.addAttribute(\"soldOutRealizedProfit\"");
    assertThat(controller).contains("model.addAttribute(\"soldOutDividend\"");
    assertThat(controller).contains("model.addAttribute(\"soldOutStockCount\"");
  }

  /** 거래 행 없이 배당만 있는 종목도 '빠진 종목' 이다 - 빼면 합이 맞지 않는다. */
  @Test
  void 배당만_있는_종목도_빠진_몫에_넣는다() throws IOException {
    String controller = Files.readString(CONTROLLER, StandardCharsets.UTF_8);

    assertThat(controller).contains("notListedStockItemIds");
    assertThat(controller)
        .as("보유 종목이 아니면서 배당이 있는 종목을 더한다")
        .contains("if (stockItemId != null && !heldStockItemIds.contains(stockItemId))");
    int soldOutAt = controller.indexOf("notListedStockItemIds =");
    int dividendAt = controller.indexOf("dividendByStockItem.forEach", soldOutAt);
    assertThat(dividendAt).as("다 판 종목 집합에서 출발해 배당 종목을 더해야 한다").isGreaterThan(soldOutAt);
  }

  @Test
  void 판_종목이_있으면_숫자를_적는다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("stock.asset.status.combined.held.only.detail");
    assertThat(template).as("판 종목이 없으면 종전 문구 그대로").contains("soldOutStockCount > 0 ?");
    assertThat(template).contains("soldOutRealizedProfit").contains("soldOutDividend");
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      assertThat(text).as(bundle).contains("stock.asset.status.combined.held.only.detail");
      // {0} 종목 수 · {1} 실현손익 · {2} 배당
      int at = text.indexOf("stock.asset.status.combined.held.only.detail");
      String line = text.substring(at, text.indexOf((char) 10, at));
      assertThat(line).as(bundle + " 자리표시자").contains("{0}").contains("{1}").contains("{2}");
    }
  }
}
